package com.example.ntpclock.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.ntpclock.NtpClockApplication
import com.example.ntpclock.domain.model.SyncTriggerSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val app = context.applicationContext as? NtpClockApplication ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = app.container.settingsRepository.getCurrentSettings()
                if (settings.autoSyncEnabled) {
                    app.container.syncWorkScheduler.schedulePeriodicSync(settings.syncIntervalMinutes)
                }
                if (settings.syncOnBootEnabled) {
                    app.container.syncWorkScheduler.enqueueOneTimeSync(SyncTriggerSource.BOOT)
                }
                Log.i(TAG, "Handled $action: autoSync=${settings.autoSyncEnabled}, syncOnBoot=${settings.syncOnBootEnabled}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed handling $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

