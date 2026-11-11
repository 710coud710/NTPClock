package com.example.ntpclock.core.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * Reads device timezone metadata and formats epoch timestamps consistently.
 */
class TimeZoneManager {

    companion object {
        const val TARGET_TIMEZONE_ID = "Asia/Ho_Chi_Minh"
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }

    fun currentTimeZoneId(): String = TimeZone.getDefault().id

    fun isTargetTimeZone(targetId: String = TARGET_TIMEZONE_ID): Boolean =
        currentTimeZoneId() == targetId

    fun formatTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(TIME_FORMATTER)

    fun formatDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(DATE_FORMATTER)

    fun formatDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(DATE_TIME_FORMATTER)
}

