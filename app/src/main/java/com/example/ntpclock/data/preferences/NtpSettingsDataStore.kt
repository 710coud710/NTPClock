package com.example.ntpclock.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ntpclock.core.time.TimeZoneManager
import com.example.ntpclock.domain.model.NtpSettings
import com.example.ntpclock.ntp.NtpQueryConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.ntpSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ntp_settings"
)

class NtpSettingsDataStore(context: Context) {

    private val dataStore = context.applicationContext.ntpSettingsDataStore

    val settingsFlow: Flow<NtpSettings> = dataStore.data.map { prefs ->
        NtpSettings(
            host = prefs[KEY_HOST] ?: NtpQueryConfig.DEFAULT_HOST,
            port = prefs[KEY_PORT] ?: NtpQueryConfig.DEFAULT_PORT,
            timeZoneId = prefs[KEY_TIMEZONE_ID] ?: TimeZoneManager.TARGET_TIMEZONE_ID,
            syncIntervalMinutes = prefs[KEY_SYNC_INTERVAL_MINUTES]
                ?: NtpSettings.DEFAULT_SYNC_INTERVAL_MINUTES,
            sampleCount = prefs[KEY_SAMPLE_COUNT] ?: NtpSettings.DEFAULT_SAMPLE_COUNT,
            sampleIntervalMs = prefs[KEY_SAMPLE_INTERVAL_MS]
                ?: NtpSettings.DEFAULT_SAMPLE_INTERVAL_MS,
            timeoutMs = prefs[KEY_TIMEOUT_MS] ?: NtpQueryConfig.DEFAULT_TIMEOUT_MS,
            maxAcceptableRttMs = prefs[KEY_MAX_RTT_MS] ?: NtpQueryConfig.DEFAULT_MAX_RTT_MS,
            maxOffsetBeforeSetMs = prefs[KEY_MAX_OFFSET_BEFORE_SET_MS]
                ?: NtpSettings.DEFAULT_MAX_OFFSET_BEFORE_SET_MS,
            autoSyncEnabled = prefs[KEY_AUTO_SYNC_ENABLED] ?: true,
            syncOnBootEnabled = prefs[KEY_SYNC_ON_BOOT_ENABLED] ?: true,
            settingsLockedByPolicy = prefs[KEY_SETTINGS_LOCKED] ?: true,
        )
    }

    suspend fun getCurrent(): NtpSettings = settingsFlow.first()

    suspend fun update(transform: (NtpSettings) -> NtpSettings) {
        dataStore.edit { prefs ->
            val current = NtpSettings(
                host = prefs[KEY_HOST] ?: NtpQueryConfig.DEFAULT_HOST,
                port = prefs[KEY_PORT] ?: NtpQueryConfig.DEFAULT_PORT,
                timeZoneId = prefs[KEY_TIMEZONE_ID] ?: TimeZoneManager.TARGET_TIMEZONE_ID,
                syncIntervalMinutes = prefs[KEY_SYNC_INTERVAL_MINUTES]
                    ?: NtpSettings.DEFAULT_SYNC_INTERVAL_MINUTES,
                sampleCount = prefs[KEY_SAMPLE_COUNT] ?: NtpSettings.DEFAULT_SAMPLE_COUNT,
                sampleIntervalMs = prefs[KEY_SAMPLE_INTERVAL_MS]
                    ?: NtpSettings.DEFAULT_SAMPLE_INTERVAL_MS,
                timeoutMs = prefs[KEY_TIMEOUT_MS] ?: NtpQueryConfig.DEFAULT_TIMEOUT_MS,
                maxAcceptableRttMs = prefs[KEY_MAX_RTT_MS] ?: NtpQueryConfig.DEFAULT_MAX_RTT_MS,
                maxOffsetBeforeSetMs = prefs[KEY_MAX_OFFSET_BEFORE_SET_MS]
                    ?: NtpSettings.DEFAULT_MAX_OFFSET_BEFORE_SET_MS,
                autoSyncEnabled = prefs[KEY_AUTO_SYNC_ENABLED] ?: true,
                syncOnBootEnabled = prefs[KEY_SYNC_ON_BOOT_ENABLED] ?: true,
                settingsLockedByPolicy = prefs[KEY_SETTINGS_LOCKED] ?: true,
            )
            val updated = transform(current)
            prefs[KEY_HOST] = updated.host
            prefs[KEY_PORT] = updated.port
            prefs[KEY_TIMEZONE_ID] = updated.timeZoneId
            prefs[KEY_SYNC_INTERVAL_MINUTES] = updated.syncIntervalMinutes
            prefs[KEY_SAMPLE_COUNT] = updated.sampleCount
            prefs[KEY_SAMPLE_INTERVAL_MS] = updated.sampleIntervalMs
            prefs[KEY_TIMEOUT_MS] = updated.timeoutMs
            prefs[KEY_MAX_RTT_MS] = updated.maxAcceptableRttMs
            prefs[KEY_MAX_OFFSET_BEFORE_SET_MS] = updated.maxOffsetBeforeSetMs
            prefs[KEY_AUTO_SYNC_ENABLED] = updated.autoSyncEnabled
            prefs[KEY_SYNC_ON_BOOT_ENABLED] = updated.syncOnBootEnabled
            prefs[KEY_SETTINGS_LOCKED] = updated.settingsLockedByPolicy
        }
    }

    suspend fun resetDefaults() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_HOST = stringPreferencesKey("ntp_host")
        val KEY_PORT = intPreferencesKey("ntp_port")
        val KEY_TIMEZONE_ID = stringPreferencesKey("timezone_id")
        val KEY_SYNC_INTERVAL_MINUTES = longPreferencesKey("sync_interval_minutes")
        val KEY_SAMPLE_COUNT = intPreferencesKey("sample_count")
        val KEY_SAMPLE_INTERVAL_MS = longPreferencesKey("sample_interval_ms")
        val KEY_TIMEOUT_MS = intPreferencesKey("timeout_ms")
        val KEY_MAX_RTT_MS = longPreferencesKey("max_rtt_ms")
        val KEY_MAX_OFFSET_BEFORE_SET_MS = longPreferencesKey("max_offset_before_set_ms")
        val KEY_AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val KEY_SYNC_ON_BOOT_ENABLED = booleanPreferencesKey("sync_on_boot_enabled")
        val KEY_SETTINGS_LOCKED = booleanPreferencesKey("settings_locked_by_policy")
    }
}

