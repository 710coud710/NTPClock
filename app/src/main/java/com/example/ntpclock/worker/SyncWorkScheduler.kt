package com.example.ntpclock.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.ntpclock.domain.model.NtpSettings
import com.example.ntpclock.domain.model.SyncTriggerSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class SyncWorkScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun schedulePeriodicSync(intervalMinutes: Long = NtpSettings.DEFAULT_SYNC_INTERVAL_MINUTES) {
        val safeMinutes = intervalMinutes.coerceAtLeast(NtpSettings.MIN_SYNC_INTERVAL_MINUTES)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<NtpSyncWorker>(safeMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(workDataOf(NtpSyncWorker.KEY_TRIGGER_SOURCE to SyncTriggerSource.PERIODIC_WORK.name))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun enqueueOneTimeSync(triggerSource: SyncTriggerSource) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<NtpSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(NtpSyncWorker.KEY_TRIGGER_SOURCE to triggerSource.name))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
    }

    fun observePeriodicWorkInfo(): Flow<WorkInfo?> =
        workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_PERIODIC_WORK_NAME)
            .map { infos -> infos.firstOrNull() }

    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME = "ntp_periodic_sync"
        const val UNIQUE_ONE_TIME_WORK_NAME = "ntp_one_time_sync"
    }
}

