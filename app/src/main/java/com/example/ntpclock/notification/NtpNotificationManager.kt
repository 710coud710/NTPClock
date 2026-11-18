package com.example.ntpclock.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ntpclock.MainActivity
import com.example.ntpclock.R

object NtpNotificationManager {

    const val CHANNEL_ID = "ntp_foreground_service_channel"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Tác vụ đồng bộ NTP ngầm"
            val descriptionText = "Duy trì dịch vụ cập nhật giờ NTP định kỳ ngầm"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        context: Context,
        statusTitle: String = "Đang hoạt động ngầm",
        statusDetail: String = "Sẵn sàng đồng bộ giờ NTP",
        ntpServer: String = "192.168.4.2",
        lastSyncText: String = "Chưa đồng bộ",
        nextSyncText: String = "--:--",
    ): Notification {
        createNotificationChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle("NTPClock · $statusTitle")
            .bigText(
                "• Trạng thái: $statusDetail\n" +
                        "• Máy chủ NTP: $ntpServer\n" +
                        "• Đồng bộ gần nhất: $lastSyncText\n" +
                        "• Lần tới dự kiến: $nextSyncText"
            )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("NTPClock · $statusTitle")
            .setContentText("NTP Server: $ntpServer | Lần cuối: $lastSyncText")
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun updateNotification(
        context: Context,
        statusTitle: String,
        statusDetail: String,
        ntpServer: String,
        lastSyncText: String,
        nextSyncText: String,
    ) {
        val notification = buildNotification(
            context = context,
            statusTitle = statusTitle,
            statusDetail = statusDetail,
            ntpServer = ntpServer,
            lastSyncText = lastSyncText,
            nextSyncText = nextSyncText,
        )
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
