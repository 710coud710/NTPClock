package com.example.ntpclock.sync

import android.content.ComponentName
import com.example.ntpclock.admin.AdminStatus
import com.example.ntpclock.admin.DeviceOwnerManager
import com.example.ntpclock.admin.PolicyActionResult
import com.example.ntpclock.admin.SetTimeOutcome
import com.example.ntpclock.admin.SystemClockController
import com.example.ntpclock.core.network.NetworkMonitor
import com.example.ntpclock.core.time.ClockProvider
import com.example.ntpclock.ntp.NtpClient
import com.example.ntpclock.ntp.NtpQueryConfig
import com.example.ntpclock.ntp.NtpQueryOutcome
import com.example.ntpclock.ntp.NtpSample
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NtpSyncCoordinatorTest {

    @Test
    fun `syncAndApplyClock returns CLOCK_SYNC_DENIED when not device owner`() = runTest {
        val coordinator = createCoordinator(isDeviceOwner = false)
        val result = coordinator.syncAndApplyClock(NtpQueryConfig())
        assertEquals(SyncStatus.CLOCK_SYNC_DENIED, result.status)
    }

    @Test
    fun `syncAndApplyClock returns CLOCK_SYNC_DENIED when auto time is enabled`() = runTest {
        val coordinator = createCoordinator(isDeviceOwner = true, autoTimeEnabled = true)
        val result = coordinator.syncAndApplyClock(NtpQueryConfig())
        assertEquals(SyncStatus.CLOCK_SYNC_DENIED, result.status)
    }

    @Test
    fun `syncAndApplyClock returns NTP_FAILED when network is disconnected`() = runTest {
        val coordinator = createCoordinator(lanConnected = false)
        val result = coordinator.syncAndApplyClock(NtpQueryConfig())
        assertEquals(SyncStatus.NTP_FAILED, result.status)
    }

    @Test
    fun `syncAndApplyClock applies server time plus monotonic processing delta and verifies`() = runTest {
        val clock = FakeClockProvider(
            wallMillis = 1_794_380_000_000L,
            elapsedNanos = 1_000_000_000L,
        )
        val controller = FakeSystemClockController(clock = clock, autoTimeEnabled = false)
        val sample = NtpSample(
            serverHost = "192.168.4.2",
            serverPort = 123,
            version = 4,
            stratum = 2,
            leapIndicator = 0,
            t1EpochMillis = 1_794_380_000_000L,
            t2EpochMillis = 1_794_380_300_005L,
            t3EpochMillis = 1_794_380_300_006L,
            t4EpochMillis = 1_794_380_000_011L,
            t4ElapsedNanos = 1_011_000_000L,
            offsetMillis = 300_000L,
            roundTripDelayMillis = 10L,
            estimatedServerTimeAtT4Millis = 1_794_380_300_011L,
        )
        // Advance monotonic clock by 25ms after T4 before setTime() is invoked
        clock.elapsedNanos = 1_036_000_000L

        val coordinator = NtpSyncCoordinator(
            networkMonitor = FakeNetworkMonitor(true),
            deviceOwnerManager = FakeDeviceOwnerManager(true),
            systemClockController = controller,
            ntpClient = FakeNtpClient(NtpQueryOutcome.Success(sample)),
            clockProvider = clock,
        )

        val result = coordinator.syncAndApplyClock(NtpQueryConfig())
        assertEquals(SyncStatus.CLOCK_SYNC_SUCCESS, result.status)
        assertEquals(1_794_380_300_036L, controller.lastSetTimeMillis)
        assertEquals(0L, result.postSyncDiffMillis)
    }

    @Test
    fun `syncAndApplyClock returns CLOCK_SYNC_VERIFY_FAILED when clock does not move`() = runTest {
        val clock = FakeClockProvider(
            wallMillis = 1_794_380_000_000L,
            elapsedNanos = 1_000_000_000L,
        )
        val controller = FakeSystemClockController(
            clock = clock,
            autoTimeEnabled = false,
            applyClockChange = false,
        )
        val sample = NtpSample(
            serverHost = "192.168.4.2",
            serverPort = 123,
            version = 4,
            stratum = 2,
            leapIndicator = 0,
            t1EpochMillis = 1_794_380_000_000L,
            t2EpochMillis = 1_794_380_300_000L,
            t3EpochMillis = 1_794_380_300_000L,
            t4EpochMillis = 1_794_380_000_010L,
            t4ElapsedNanos = 1_000_000_000L,
            offsetMillis = 300_000L,
            roundTripDelayMillis = 10L,
            estimatedServerTimeAtT4Millis = 1_794_380_300_010L,
        )

        val coordinator = NtpSyncCoordinator(
            networkMonitor = FakeNetworkMonitor(true),
            deviceOwnerManager = FakeDeviceOwnerManager(true),
            systemClockController = controller,
            ntpClient = FakeNtpClient(NtpQueryOutcome.Success(sample)),
            clockProvider = clock,
        )

        val result = coordinator.syncAndApplyClock(NtpQueryConfig())
        assertEquals(SyncStatus.CLOCK_SYNC_VERIFY_FAILED, result.status)
    }

    private fun createCoordinator(
        lanConnected: Boolean = true,
        isDeviceOwner: Boolean = true,
        autoTimeEnabled: Boolean = false,
    ): NtpSyncCoordinator {
        val clock = FakeClockProvider(1_794_380_000_000L, 1_000_000_000L)
        return NtpSyncCoordinator(
            networkMonitor = FakeNetworkMonitor(lanConnected),
            deviceOwnerManager = FakeDeviceOwnerManager(isDeviceOwner),
            systemClockController = FakeSystemClockController(clock, autoTimeEnabled),
            ntpClient = FakeNtpClient(NtpQueryOutcome.Failure("unused")),
            clockProvider = clock,
        )
    }

    private class FakeClockProvider(
        var wallMillis: Long,
        var elapsedNanos: Long,
    ) : ClockProvider {
        override fun currentTimeMillis(): Long = wallMillis
        override fun elapsedRealtimeNanos(): Long = elapsedNanos
    }

    private class FakeNetworkMonitor(private val connected: Boolean) : NetworkMonitor {
        override fun isLocalNetworkConnected(): Boolean = connected
    }

    private class FakeDeviceOwnerManager(private val deviceOwner: Boolean) : DeviceOwnerManager {
        override val adminComponent: ComponentName
            get() = throw UnsupportedOperationException("Not used in JVM unit test")

        override fun isDeviceOwner(): Boolean = deviceOwner
        override fun isAdminActive(): Boolean = deviceOwner
        override fun getStatus(): AdminStatus = AdminStatus(
            isDeviceOwner = deviceOwner,
            isAdminActive = deviceOwner,
            packageName = "com.example.ntpclock",
            receiverComponentName = "com.example.ntpclock/.admin.NtpDeviceAdminReceiver",
        )
    }

    private class FakeSystemClockController(
        private val clock: FakeClockProvider,
        private val autoTimeEnabled: Boolean,
        private val applyClockChange: Boolean = true,
    ) : SystemClockController {
        var lastSetTimeMillis: Long? = null

        override fun isAutoTimeEnabled(): Boolean = autoTimeEnabled
        override fun isAutoTimeZoneEnabled(): Boolean = false
        override fun disableAutoTime(): PolicyActionResult = PolicyActionResult.Success("ok")
        override fun setTime(epochMillis: Long): SetTimeOutcome {
            lastSetTimeMillis = epochMillis
            if (applyClockChange) {
                clock.wallMillis = epochMillis
            }
            return SetTimeOutcome.Success
        }

        override fun setTimeZone(timeZoneId: String): PolicyActionResult = PolicyActionResult.Success("ok")
    }

    private class FakeNtpClient(private val outcome: NtpQueryOutcome) : NtpClient {
        override suspend fun query(config: NtpQueryConfig): NtpQueryOutcome = outcome
    }
}

