package com.example.ntpclock.worker

import android.content.Context
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ntpclock.NtpClockApplication
import com.example.ntpclock.domain.model.SyncTriggerSource
import com.example.ntpclock.sync.SyncStatus

class NtpSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NtpClockApplication ?: return Result.failure()
        val triggerName = inputData.getString(KEY_TRIGGER_SOURCE)
        val triggerSource = triggerName
            ?.let { runCatching { SyncTriggerSource.valueOf(it) }.getOrNull() }
            ?: SyncTriggerSource.PERIODIC_WORK

        // Acquire a partial WakeLock to keep CPU awake during NTP network exchange on Doze/vendor power-saving modes
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NTPClock:SyncWorkerWakeLock"
        )
        wakeLock?.acquire(WAKELOCK_TIMEOUT_MS)

        try {
            val executionResult = app.container.syncCoordinator.syncAndApplyClock(
                triggerSource = triggerSource,
            )

            return when (executionResult.status) {
                SyncStatus.CLOCK_SYNC_SUCCESS,
                SyncStatus.NTP_SUCCESS -> Result.success()

                SyncStatus.CLOCK_SYNC_DENIED -> {
                    // Policy/Device Owner missing is non-transient; keep periodic schedule without tight retry loop.
                    if (triggerSource == SyncTriggerSource.PERIODIC_WORK) Result.success() else Result.failure()
                }

                SyncStatus.NTP_FAILED,
                SyncStatus.CLOCK_SYNC_VERIFY_FAILED -> {
                    if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
                }
            }
        } finally {
            if (wakeLock?.isHeld == true) {
                try {
                    wakeLock.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    companion object {
        const val KEY_TRIGGER_SOURCE = "trigger_source"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val WAKELOCK_TIMEOUT_MS = 15_000L
    }
}

