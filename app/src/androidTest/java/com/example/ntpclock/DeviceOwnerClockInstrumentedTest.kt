package com.example.ntpclock

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ntpclock.admin.AndroidDeviceOwnerManager
import com.example.ntpclock.admin.AndroidSystemClockController
import com.example.ntpclock.admin.PolicyActionResult
import com.example.ntpclock.admin.SetTimeOutcome
import com.example.ntpclock.data.local.NtpDatabase
import com.example.ntpclock.data.local.SyncHistoryEntity
import com.example.ntpclock.domain.model.SyncTriggerSource
import com.example.ntpclock.sync.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class DeviceOwnerClockInstrumentedTest {

    @Test
    fun roomHistoryRoundTripPersistsAndQueriesRecords() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, NtpDatabase::class.java).build()
        try {
            val dao = db.syncHistoryDao()
            val entity = SyncHistoryEntity(
                runAtEpochMillis = 1_794_380_000_000L,
                triggerSource = SyncTriggerSource.MANUAL.name,
                ntpServer = "192.168.4.2",
                ntpPort = 123,
                rttMillis = 12L,
                offsetMillis = 45L,
                stratum = 2,
                leapIndicator = 0,
                beforeEpochMillis = 1_794_379_999_955L,
                targetEpochMillis = 1_794_380_000_000L,
                afterEpochMillis = 1_794_380_000_002L,
                postSyncDiffMillis = 2L,
                apiSuccess = true,
                status = SyncStatus.CLOCK_SYNC_SUCCESS.name,
                errorMessage = null,
                samplesAttempted = 5,
                samplesValid = 5,
            )
            dao.insert(entity)

            val recent = dao.observeRecent(10).first()
            assertEquals(1, recent.size)
            assertEquals("192.168.4.2", recent.first().ntpServer)
            assertEquals(SyncStatus.CLOCK_SYNC_SUCCESS, recent.first().toDomain().status)
        } finally {
            db.close()
        }
    }

    @Test
    fun deviceOwnerCanDisableAutoTimeAndSetSystemClockAndTimezone() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val deviceOwnerManager = AndroidDeviceOwnerManager(context)
        assumeTrue("Device must be provisioned as Device Owner for this test", deviceOwnerManager.isDeviceOwner())

        val clockController = AndroidSystemClockController(context, deviceOwnerManager)
        val initialTime = System.currentTimeMillis()
        val wasAutoTimeEnabled = clockController.isAutoTimeEnabled()

        try {
            val disableResult = clockController.disableAutoTime()
            assertTrue("Expected Success disabling auto time, got $disableResult", disableResult is PolicyActionResult.Success)
            assertFalse(clockController.isAutoTimeEnabled())

            val shiftedTarget = initialTime + 60_000L
            val setOutcome = clockController.setTime(shiftedTarget)
            assertEquals(SetTimeOutcome.Success, setOutcome)

            val afterShift = System.currentTimeMillis()
            assertTrue(
                "Expected System Clock near shiftedTarget=$shiftedTarget, got $afterShift",
                abs(afterShift - shiftedTarget) < 5_000L,
            )

            val tzResult = clockController.setTimeZone("Asia/Ho_Chi_Minh")
            assertTrue("Expected Success setting timezone, got $tzResult", tzResult is PolicyActionResult.Success)
        } finally {
            // Restore approximate original clock and auto-time state on the test device.
            clockController.setTime(initialTime + 1_000L)
            if (wasAutoTimeEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val dpm = context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                dpm.setAutoTimeEnabled(deviceOwnerManager.adminComponent, true)
            }
        }
    }
}

