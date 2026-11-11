package com.example.ntpclock.sync

import com.example.ntpclock.admin.DeviceOwnerManager
import com.example.ntpclock.admin.SetTimeOutcome
import com.example.ntpclock.admin.SystemClockController
import com.example.ntpclock.core.network.NetworkMonitor
import com.example.ntpclock.core.time.ClockProvider
import com.example.ntpclock.ntp.NtpClient
import com.example.ntpclock.ntp.NtpQueryConfig
import com.example.ntpclock.ntp.NtpQueryOutcome
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/**
 * Coordinates pre-flight policy checks, NTP query, System Clock update, and post-sync verification.
 */
class NtpSyncCoordinator(
    private val networkMonitor: NetworkMonitor,
    private val deviceOwnerManager: DeviceOwnerManager,
    private val systemClockController: SystemClockController,
    private val ntpClient: NtpClient,
    private val clockProvider: ClockProvider,
    private val verifyToleranceMillis: Long = DEFAULT_VERIFY_TOLERANCE_MS,
) {
    private val syncMutex = Mutex()

    suspend fun queryNtpOnly(config: NtpQueryConfig): SyncExecutionResult = syncMutex.withLock {
        val startWall = clockProvider.currentTimeMillis()
        if (!networkMonitor.isLocalNetworkConnected()) {
            return@withLock SyncExecutionResult(
                status = SyncStatus.NTP_FAILED,
                message = "Không có kết nối Wi-Fi/LAN nội bộ",
                beforeEpochMillis = startWall,
                executedAtEpochMillis = startWall,
            )
        }

        return@withLock when (val outcome = ntpClient.query(config)) {
            is NtpQueryOutcome.Failure -> SyncExecutionResult(
                status = SyncStatus.NTP_FAILED,
                message = outcome.reason,
                beforeEpochMillis = startWall,
                executedAtEpochMillis = clockProvider.currentTimeMillis(),
            )

            is NtpQueryOutcome.Success -> {
                val sample = outcome.sample
                SyncExecutionResult(
                    status = SyncStatus.NTP_SUCCESS,
                    message = "Lấy giờ NTP thành công (offset=${sample.offsetMillis}ms, RTT=${sample.roundTripDelayMillis}ms, stratum=${sample.stratum})",
                    sample = sample,
                    beforeEpochMillis = startWall,
                    targetEpochMillis = sample.estimatedServerTimeAtT4Millis,
                    executedAtEpochMillis = clockProvider.currentTimeMillis(),
                )
            }
        }
    }

    suspend fun syncAndApplyClock(config: NtpQueryConfig): SyncExecutionResult = syncMutex.withLock {
        val startWall = clockProvider.currentTimeMillis()

        if (!networkMonitor.isLocalNetworkConnected()) {
            return@withLock SyncExecutionResult(
                status = SyncStatus.NTP_FAILED,
                message = "Không có kết nối Wi-Fi/LAN nội bộ. Không thay đổi giờ hệ thống.",
                beforeEpochMillis = startWall,
                executedAtEpochMillis = startWall,
            )
        }

        if (!deviceOwnerManager.isDeviceOwner()) {
            return@withLock SyncExecutionResult(
                status = SyncStatus.CLOCK_SYNC_DENIED,
                message = "Ứng dụng chưa được provision Device Owner",
                beforeEpochMillis = startWall,
                executedAtEpochMillis = startWall,
            )
        }

        if (systemClockController.isAutoTimeEnabled()) {
            return@withLock SyncExecutionResult(
                status = SyncStatus.CLOCK_SYNC_DENIED,
                message = "Automatic Date & Time đang bật (AUTO_TIME != 0). Cần tắt trước khi đặt giờ.",
                beforeEpochMillis = startWall,
                executedAtEpochMillis = startWall,
            )
        }

        val sample = when (val outcome = ntpClient.query(config)) {
            is NtpQueryOutcome.Failure -> {
                return@withLock SyncExecutionResult(
                    status = SyncStatus.NTP_FAILED,
                    message = outcome.reason,
                    beforeEpochMillis = startWall,
                    executedAtEpochMillis = clockProvider.currentTimeMillis(),
                )
            }

            is NtpQueryOutcome.Success -> outcome.sample
        }

        val elapsedSinceT4Millis =
            ((clockProvider.elapsedRealtimeNanos() - sample.t4ElapsedNanos).coerceAtLeast(0L)) / 1_000_000L
        val beforeEpochMillis = clockProvider.currentTimeMillis()
        val targetEpochMillis = sample.estimatedServerTimeAtT4Millis + elapsedSinceT4Millis

        when (val setTimeOutcome = systemClockController.setTime(targetEpochMillis)) {
            is SetTimeOutcome.Denied -> {
                return@withLock SyncExecutionResult(
                    status = SyncStatus.CLOCK_SYNC_DENIED,
                    message = setTimeOutcome.reason,
                    sample = sample,
                    beforeEpochMillis = beforeEpochMillis,
                    targetEpochMillis = targetEpochMillis,
                    executedAtEpochMillis = clockProvider.currentTimeMillis(),
                )
            }

            is SetTimeOutcome.Failed -> {
                return@withLock SyncExecutionResult(
                    status = SyncStatus.CLOCK_SYNC_VERIFY_FAILED,
                    message = setTimeOutcome.reason,
                    sample = sample,
                    beforeEpochMillis = beforeEpochMillis,
                    targetEpochMillis = targetEpochMillis,
                    executedAtEpochMillis = clockProvider.currentTimeMillis(),
                )
            }

            SetTimeOutcome.Success -> {
                val afterElapsedNanos = clockProvider.elapsedRealtimeNanos()
                val afterEpochMillis = clockProvider.currentTimeMillis()
                val totalElapsedFromT4Millis =
                    ((afterElapsedNanos - sample.t4ElapsedNanos).coerceAtLeast(0L)) / 1_000_000L
                val expectedNowMillis = sample.estimatedServerTimeAtT4Millis + totalElapsedFromT4Millis
                val postSyncDiffMillis = afterEpochMillis - expectedNowMillis

                val verified = abs(postSyncDiffMillis) <= verifyToleranceMillis
                val status = if (verified) {
                    SyncStatus.CLOCK_SYNC_SUCCESS
                } else {
                    SyncStatus.CLOCK_SYNC_VERIFY_FAILED
                }
                val message = if (verified) {
                    "Đồng bộ System Clock thành công (sai số hậu kiểm=${postSyncDiffMillis}ms)"
                } else {
                    "setTime() trả về true nhưng hậu kiểm vượt ngưỡng (sai số=${postSyncDiffMillis}ms)"
                }

                return@withLock SyncExecutionResult(
                    status = status,
                    message = message,
                    sample = sample,
                    beforeEpochMillis = beforeEpochMillis,
                    targetEpochMillis = targetEpochMillis,
                    afterEpochMillis = afterEpochMillis,
                    postSyncDiffMillis = postSyncDiffMillis,
                    executedAtEpochMillis = afterEpochMillis,
                )
            }
        }
    }

    companion object {
        const val DEFAULT_VERIFY_TOLERANCE_MS = 2_000L
    }
}

