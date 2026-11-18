package com.example.ntpclock.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.ntpclock.NtpClockApplication
import com.example.ntpclock.domain.model.SyncTriggerSource
import com.example.ntpclock.notification.NtpNotificationManager
import com.example.ntpclock.sync.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class NtpForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "NtpForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.i(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_STOP -> {
                stopForegroundServiceInternal()
                return START_NOT_STICKY
            }

            ACTION_SYNC_NOW -> {
                triggerImmediateSync()
            }

            ACTION_START -> {
                startForegroundInternal()
                ensureSyncLoopRunning()
            }
        }

        return START_STICKY
    }

    private fun startForegroundInternal() {
        val initialNotification = NtpNotificationManager.buildNotification(
            context = this,
            statusTitle = "Đang hoạt động ngầm",
            statusDetail = "Chuẩn bị đồng bộ...",
            ntpServer = "Đang tải...",
            lastSyncText = "Chưa đồng bộ",
            nextSyncText = "Đang tính...",
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NtpNotificationManager.NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NtpNotificationManager.NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed starting foreground service with type, fallback to default", e)
            try {
                startForeground(NtpNotificationManager.NOTIFICATION_ID, initialNotification)
            } catch (e2: Exception) {
                Log.e(TAG, "Could not start foreground notification", e2)
            }
        }
    }

    private fun ensureSyncLoopRunning() {
        if (syncJob?.isActive == true) return

        syncJob = serviceScope.launch {
            val app = applicationContext as? NtpClockApplication ?: return@launch

            while (isActive) {
                val settings = app.container.settingsRepository.getCurrentSettings()
                if (!settings.autoSyncEnabled) {
                    Log.i(TAG, "Auto sync disabled in settings, stopping foreground service")
                    stopSelf()
                    break
                }

                performSyncExecution(app, SyncTriggerSource.PERIODIC_WORK)

                val intervalDuration = settings.syncIntervalMinutes.toDuration(DurationUnit.MINUTES)
                    .coerceAtLeast(15.toDuration(DurationUnit.MINUTES))
                delay(intervalDuration)
            }
        }
    }

    private fun triggerImmediateSync() {
        serviceScope.launch {
            val app = applicationContext as? NtpClockApplication ?: return@launch
            performSyncExecution(app, SyncTriggerSource.MANUAL)
        }
    }

    private suspend fun performSyncExecution(app: NtpClockApplication, triggerSource: SyncTriggerSource) {
        val settings = app.container.settingsRepository.getCurrentSettings()
        val ntpServerText = "${settings.host}:${settings.port}"

        // Acquire WakeLock during NTP query to guarantee zero network delay/drift on Doze mode
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NTPClock:ForegroundServiceWakeLock"
        )
        wakeLock?.acquire(15_000L)

        try {
            NtpNotificationManager.updateNotification(
                context = this,
                statusTitle = "Đang đồng bộ NTP...",
                statusDetail = "Đang gửi gói tin NTP đến $ntpServerText",
                ntpServer = ntpServerText,
                lastSyncText = "Đang kết nối...",
                nextSyncText = "Đang xử lý...",
            )

            val result = app.container.syncCoordinator.syncAndApplyClock(triggerSource)

            val lastTimeFormatted = app.container.timeZoneManager.formatTime(result.executedAtEpochMillis)
            val nextSyncEpoch = result.executedAtEpochMillis + (settings.syncIntervalMinutes * 60 * 1000L)
            val nextTimeFormatted = app.container.timeZoneManager.formatTime(nextSyncEpoch)

            val (statusTitle, statusDetail) = when (result.status) {
                SyncStatus.CLOCK_SYNC_SUCCESS -> Pair("Đã đồng bộ thành công", "Đã đặt giờ hệ thống (${result.postSyncDiffMillis?.let { "Lệch $it ms" } ?: "Khớp"})")
                SyncStatus.NTP_SUCCESS -> Pair("NTP kết nối tốt", "Offset: ${result.sample?.offsetMillis ?: 0} ms")
                SyncStatus.NTP_FAILED -> Pair("Lỗi kết nối NTP", result.message)
                SyncStatus.CLOCK_SYNC_DENIED -> Pair("Từ chối chỉnh giờ", "Chưa có quyền Device Owner")
                SyncStatus.CLOCK_SYNC_VERIFY_FAILED -> Pair("Xác minh thất bại", result.message)
            }

            NtpNotificationManager.updateNotification(
                context = this,
                statusTitle = statusTitle,
                statusDetail = statusDetail,
                ntpServer = ntpServerText,
                lastSyncText = lastTimeFormatted,
                nextSyncText = nextTimeFormatted,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing background sync in service", e)
        } finally {
            if (wakeLock?.isHeld == true) {
                try {
                    wakeLock.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun stopForegroundServiceInternal() {
        Log.i(TAG, "Stopping foreground service")
        syncJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "NtpForegroundService destroyed")
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "NtpForegroundService"

        const val ACTION_START = "com.example.ntpclock.action.START_FOREGROUND_SERVICE"
        const val ACTION_STOP = "com.example.ntpclock.action.STOP_FOREGROUND_SERVICE"
        const val ACTION_SYNC_NOW = "com.example.ntpclock.action.SYNC_NOW_FOREGROUND_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, NtpForegroundService::class.java).apply {
                action = ACTION_START
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed starting NtpForegroundService", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NtpForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed stopping NtpForegroundService", e)
            }
        }

        fun triggerSyncNow(context: Context) {
            val intent = Intent(context, NtpForegroundService::class.java).apply {
                action = ACTION_SYNC_NOW
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed triggering sync in NtpForegroundService", e)
            }
        }
    }
}
