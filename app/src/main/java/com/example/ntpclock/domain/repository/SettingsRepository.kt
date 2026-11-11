package com.example.ntpclock.domain.repository

import com.example.ntpclock.domain.model.NtpSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<NtpSettings>
    suspend fun getCurrentSettings(): NtpSettings
    suspend fun updateSettings(transform: (NtpSettings) -> NtpSettings)
    suspend fun resetDefaults()
}

