package com.example.ntpclock.domain.model

import com.example.ntpclock.core.time.TimeZoneManager
import com.example.ntpclock.ntp.NtpQueryConfig

/**
 * Persistent application configuration aligned with spec-project.md Section 10.2.
 *
 * @property maxOffsetBeforeSetMs Maximum allowed absolute offset before applying setTime (0 = unlimited).
 * @property settingsLockedByPolicy Prevents accidental edits to NTP host/port in Settings unless admin mode is unlocked.
 */
data class NtpSettings(
    val host: String = NtpQueryConfig.DEFAULT_HOST,
    val port: Int = NtpQueryConfig.DEFAULT_PORT,
    val timeZoneId: String = TimeZoneManager.TARGET_TIMEZONE_ID,
    val syncIntervalMinutes: Long = DEFAULT_SYNC_INTERVAL_MINUTES,
    val sampleCount: Int = DEFAULT_SAMPLE_COUNT,
    val sampleIntervalMs: Long = DEFAULT_SAMPLE_INTERVAL_MS,
    val timeoutMs: Int = NtpQueryConfig.DEFAULT_TIMEOUT_MS,
    val maxAcceptableRttMs: Long = NtpQueryConfig.DEFAULT_MAX_RTT_MS,
    val maxOffsetBeforeSetMs: Long = DEFAULT_MAX_OFFSET_BEFORE_SET_MS,
    val autoSyncEnabled: Boolean = true,
    val syncOnBootEnabled: Boolean = true,
    val settingsLockedByPolicy: Boolean = true,
) {
    fun toQueryConfig(): NtpQueryConfig = NtpQueryConfig(
        host = host,
        port = port,
        timeoutMs = timeoutMs,
        maxRttMs = maxAcceptableRttMs,
    )

    companion object {
        const val DEFAULT_SYNC_INTERVAL_MINUTES = 60L
        const val MIN_SYNC_INTERVAL_MINUTES = 15L
        const val DEFAULT_SAMPLE_COUNT = 5
        const val DEFAULT_SAMPLE_INTERVAL_MS = 250L
        const val DEFAULT_MAX_OFFSET_BEFORE_SET_MS = 0L
    }
}

