package com.example.ntpclock.data.repository

import com.example.ntpclock.data.preferences.NtpSettingsDataStore
import com.example.ntpclock.domain.model.NtpSettings
import com.example.ntpclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val dataStore: NtpSettingsDataStore,
) : SettingsRepository {
    override val settings: Flow<NtpSettings> = dataStore.settingsFlow

    override suspend fun getCurrentSettings(): NtpSettings = dataStore.getCurrent()

    override suspend fun updateSettings(transform: (NtpSettings) -> NtpSettings) {
        dataStore.update(transform)
    }

    override suspend fun resetDefaults() {
        dataStore.resetDefaults()
    }
}

