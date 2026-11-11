package com.example.ntpclock.sync

import com.example.ntpclock.admin.DeviceOwnerManager
import com.example.ntpclock.admin.SetTimeOutcome
import com.example.ntpclock.admin.SystemClockController
import com.example.ntpclock.core.network.NetworkMonitor
import com.example.ntpclock.core.time.ClockProvider
import com.example.ntpclock.domain.model.NtpSettings
import com.example.ntpclock.domain.model.SyncHistoryRecord
import com.example.ntpclock.domain.model.SyncTriggerSource
import com.example.ntpclock.domain.repository.SettingsRepository
import com.example.ntpclock.domain.repository.SyncHistoryRepository
import com.example.ntpclock.domain.usecase.QueryNtpTimeUseCase
import com.example.ntpclock.domain.usecase.VerifyClockSyncUseCase
import com.example.ntpclock.ntp.NtpMultiSampleOutcome
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/**
 * Coordinates pre-flight policy checks, multi-sample NTP query, System Clock update,
 * post-sync verification, and Room history persistence.
 */
class NtpSyncCoordinator(
    private val networkMonitor: NetworkMonitor,
    private val deviceOwnerManager: DeviceOwnerManager,
    private val systemClockController: SystemClockController,
    private val queryNtpTimeUseCase: QueryNtpTimeUseCase,
    private val verifyClockSyncUseCase: VerifyClockSyncUseCase,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: SyncHistoryRepository,
    private val clockProvider: ClockProvider,
) {
    private val syncMutex = Mutex()

    suspend fun queryNtpOnly(
        overrideSettings: NtpSettings? = null,
        persistHistory: Boolean = true,
    ): SyncExecutionResult = syncMutex.withLock {
        val settings = overrideSettings ?: settingsRepository.getCurrentSettings()
        val startWall = clockProvider.currentTimeMillis()
        if (!networkMonitor.isLocalNetworkConnected()) {
            return@withLock finish(
                persist = persistHistory,
                settings = settings,
                apiSuccess = false,
                result = SyncExecutionResult(
                    status = SyncStatus.NTP_FAILED,
                    message = "Không có kết nối Wi-Fi/LAN nội bộ",
                    triggerSource = SyncTriggerSource.MANUAL,
                    samplesAttempted = settings.sampleCount,
                    samplesValid = 0,
                    beforeEpochMillis = startWall,
                    executedAtEpochMillis = startWall,
                ),
            )
        }

        return@withLock when (val outcome = queryNtpTimeUseCase(settings)) {
            is NtpMultiSampleOutcome.Failure -> finish(
                persist = persistHistory,
                settings = settings,
                apiSuccess = false,
                result = SyncExecutionResult(
                    status = SyncStatus.NTP_FAILED,
                    message = outcome.reason,
                    triggerSource = SyncTriggerSource.MANUAL,
                    samplesAttempted = outcome.samplesAttempted,
                    samplesValid = outcome.samplesValid,
                    beforeEpochMillis = startWall,
                    executedAtEpochMillis = clockProvider.currentTimeMillis(),
                ),
            )

            is NtpMultiSampleOutcome.Success -> {
                val summary = outcome.summary
                val sample = summary.selectedSample
                finish(
                    persist = persistHistory,
                    settings = settings,
                    apiSuccess = false,
                    result = SyncExecutionResult(
                        status = SyncStatus.NTP_SUCCESS,
                        message = "Lấy giờ NTP thành công (${summary.samplesValid}/${summary.samplesAttempted} mẫu, median offset=${summary.medianOffsetMillis}ms, RTT=${summary.minRttMillis}..${summary.maxRttMillis}ms)",
                        triggerSource = SyncTriggerSource.MANUAL,
                        sample = sample,
                        samplesAttempted = summary.samplesAttempted,
                        samplesValid = summary.samplesValid,
                        beforeEpochMillis = startWall,
                        targetEpochMillis = sample.estimatedServerTimeAtT4Millis,
                        executedAtEpochMillis = clockProvider.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    suspend fun syncAndApplyClock(
        triggerSource: SyncTriggerSource = SyncTriggerSource.MANUAL,
        overrideSettings: NtpSettings? = null,
    ): SyncExecutionResult = syncMutex.withLock {
        val settings = overrideSettings ?: settingsRepository.getCurrentSettings()
        val startWall = clockProvider.currentTimeMillis()

        if (!networkMonitor.isLocalNetworkConnected()) {
            return@withLock finish(
                persist = true,
                settings = settings,
                apiSuccess = false,
                result = SyncExecutionResult(
                    status = SyncStatus.NTP_FAILED,
                    message = "Không có kết nối Wi-Fi/LAN nội bộ. Không thay đổi giờ hệ thống.",
                    triggerSource = triggerSource,
                    samplesAttempted = settings.sampleCount,
                    samplesValid = 0,
                    beforeEpochMillis = startWall,
                    executedAtEpochMillis = startWall,
                ),
            )
        }

        if (!deviceOwnerManager.isDeviceOwner()) {
            return@withLock finish(
                persist = true,
                settings = settings,
                apiSuccess = false,
                result = SyncExecutionResult(
                    status = SyncStatus.CLOCK_SYNC_DENIED,
                    message = "Ứng dụng chưa được provision Device Owner",
                    triggerSource = triggerSource,
                    samplesAttempted = 0,
                    samplesValid = 0,
                    beforeEpochMillis = startWall,
                    executedAtEpochMillis = startWall,
                ),
            )
        }

        if (systemClockController.isAutoTimeEnabled()) {
            return@withLock finish(
                persist = true,
                settings = settings,
                apiSuccess = false,
                result = SyncExecutionResult(
                    status = SyncStatus.CLOCK_SYNC_DENIED,
                    message = "Automatic Date & Time đang bật (AUTO_TIME != 0). Cần tắt trước khi đặt giờ.",
                    triggerSource = triggerSource,
                    samplesAttempted = 0,
                    samplesValid = 0,
                    beforeEpochMillis = startWall,
                    executedAtEpochMillis = startWall,
                ),
            )
        }

        val summary = when (val outcome = queryNtpTimeUseCase(settings)) {
            is NtpMultiSampleOutcome.Failure -> {
                return@withLock finish(
                    persist = true,
                    settings = settings,
                    apiSuccess = false,
                    result = SyncExecutionResult(
                        status = SyncStatus.NTP_FAILED,
                        message = outcome.reason,
                        triggerSource = triggerSource,
                        samplesAttempted = outcome.samplesAttempted,
                        samplesValid = outcome.samplesValid,
                        beforeEpochMillis = startWall,
                        executedAtEpochMillis = clockProvider.currentTimeMillis(),
                    ),
                )
            }

            is NtpMultiSampleOutcome.Success -> outcome.summary
        }

        val sample = summary.selectedSample
        if (settings.maxOffsetBeforeSetMs > 0L && abs(sample.offsetMillis) > settings.maxOffsetBeforeSetMs) {
            return@withLock finish(
                persist = true,
                settings = settings,
                apiSuccess = false,
                result = SyncExecutionResult(
                    status = SyncStatus.CLOCK_SYNC_DENIED,
                    message = "Độ lệch (${sample.offsetMillis}ms) vượt ngưỡng maxOffsetBeforeSet (${settings.maxOffsetBeforeSetMs}ms)",
                    triggerSource = triggerSource,
                    sample = sample,
                    samplesAttempted = summary.samplesAttempted,
                    samplesValid = summary.samplesValid,
                    beforeEpochMillis = startWall,
                    executedAtEpochMillis = clockProvider.currentTimeMillis(),
                ),
            )
        }

        val elapsedSinceT4Millis =
            ((clockProvider.elapsedRealtimeNanos() - sample.t4ElapsedNanos).coerceAtLeast(0L)) / 1_000_000L
        val beforeEpochMillis = clockProvider.currentTimeMillis()
        val targetEpochMillis = sample.estimatedServerTimeAtT4Millis + elapsedSinceT4Millis

        when (val setTimeOutcome = systemClockController.setTime(targetEpochMillis)) {
            is SetTimeOutcome.Denied -> {
                return@withLock finish(
                    persist = true,
                    settings = settings,
                    apiSuccess = false,
                    result = SyncExecutionResult(
                        status = SyncStatus.CLOCK_SYNC_DENIED,
                        message = setTimeOutcome.reason,
                        triggerSource = triggerSource,
                        sample = sample,
                        samplesAttempted = summary.samplesAttempted,
                        samplesValid = summary.samplesValid,
                        beforeEpochMillis = beforeEpochMillis,
                        targetEpochMillis = targetEpochMillis,
                        executedAtEpochMillis = clockProvider.currentTimeMillis(),
                    ),
                )
            }

            is SetTimeOutcome.Failed -> {
                return@withLock finish(
                    persist = true,
                    settings = settings,
                    apiSuccess = false,
                    result = SyncExecutionResult(
                        status = SyncStatus.CLOCK_SYNC_VERIFY_FAILED,
                        message = setTimeOutcome.reason,
                        triggerSource = triggerSource,
                        sample = sample,
                        samplesAttempted = summary.samplesAttempted,
                        samplesValid = summary.samplesValid,
                        beforeEpochMillis = beforeEpochMillis,
                        targetEpochMillis = targetEpochMillis,
                        executedAtEpochMillis = clockProvider.currentTimeMillis(),
                    ),
                )
            }

            SetTimeOutcome.Success -> {
                val verification = verifyClockSyncUseCase(sample)
                val status = if (verification.isWithinTolerance) {
                    SyncStatus.CLOCK_SYNC_SUCCESS
                } else {
                    SyncStatus.CLOCK_SYNC_VERIFY_FAILED
                }
                val message = if (verification.isWithinTolerance) {
                    "Đồng bộ System Clock thành công (${summary.samplesValid}/${summary.samplesAttempted} mẫu, offset=${sample.offsetMillis}ms, sai số hậu kiểm=${verification.diffMillis}ms)"
                } else {
                    "setTime() trả về true nhưng hậu kiểm vượt ngưỡng (sai số=${verification.diffMillis}ms)"
                }

                return@withLock finish(
                    persist = true,
                    settings = settings,
                    apiSuccess = true,
                    result = SyncExecutionResult(
                        status = status,
                        message = message,
                        triggerSource = triggerSource,
                        sample = sample,
                        samplesAttempted = summary.samplesAttempted,
                        samplesValid = summary.samplesValid,
                        beforeEpochMillis = beforeEpochMillis,
                        targetEpochMillis = targetEpochMillis,
                        afterEpochMillis = verification.afterEpochMillis,
                        postSyncDiffMillis = verification.diffMillis,
                        executedAtEpochMillis = verification.afterEpochMillis,
                    ),
                )
            }
        }
    }

    private suspend fun finish(
        persist: Boolean,
        settings: NtpSettings,
        apiSuccess: Boolean,
        result: SyncExecutionResult,
    ): SyncExecutionResult {
        if (persist) {
            val errorMsg = when (result.status) {
                SyncStatus.CLOCK_SYNC_SUCCESS, SyncStatus.NTP_SUCCESS -> null
                else -> result.message
            }
            historyRepository.insertRecord(
                SyncHistoryRecord(
                    runAtEpochMillis = result.executedAtEpochMillis,
                    triggerSource = result.triggerSource,
                    ntpServer = settings.host,
                    ntpPort = settings.port,
                    rttMillis = result.sample?.roundTripDelayMillis,
                    offsetMillis = result.sample?.offsetMillis,
                    stratum = result.sample?.stratum,
                    leapIndicator = result.sample?.leapIndicator,
                    beforeEpochMillis = result.beforeEpochMillis,
                    targetEpochMillis = result.targetEpochMillis,
                    afterEpochMillis = result.afterEpochMillis,
                    postSyncDiffMillis = result.postSyncDiffMillis,
                    apiSuccess = apiSuccess,
                    status = result.status,
                    errorMessage = errorMsg,
                    samplesAttempted = result.samplesAttempted,
                    samplesValid = result.samplesValid,
                )
            )
        }
        return result
    }
}
