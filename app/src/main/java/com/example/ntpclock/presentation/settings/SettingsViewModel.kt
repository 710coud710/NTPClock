package com.example.ntpclock.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ntpclock.AppContainer
import com.example.ntpclock.domain.model.NtpSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val host: String = "",
    val port: String = "",
    val timeZoneId: String = "",
    val syncIntervalMinutes: String = "",
    val sampleCount: String = "",
    val timeoutMs: String = "",
    val maxAcceptableRttMs: String = "",
    val maxOffsetBeforeSetMs: String = "",
    val autoSyncEnabled: Boolean = true,
    val syncOnBootEnabled: Boolean = true,
    val settingsLockedByPolicy: Boolean = true,
    val isAdminUnlocked: Boolean = false,
    val statusMessage: String? = null,
)

class SettingsViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { s ->
                _uiState.update { state ->
                    state.copy(
                        host = s.host,
                        port = s.port.toString(),
                        timeZoneId = s.timeZoneId,
                        syncIntervalMinutes = s.syncIntervalMinutes.toString(),
                        sampleCount = s.sampleCount.toString(),
                        timeoutMs = s.timeoutMs.toString(),
                        maxAcceptableRttMs = s.maxAcceptableRttMs.toString(),
                        maxOffsetBeforeSetMs = s.maxOffsetBeforeSetMs.toString(),
                        autoSyncEnabled = s.autoSyncEnabled,
                        syncOnBootEnabled = s.syncOnBootEnabled,
                        settingsLockedByPolicy = s.settingsLockedByPolicy,
                    )
                }
            }
        }
    }

    fun onHostChange(v: String) = _uiState.update { it.copy(host = v, statusMessage = null) }
    fun onPortChange(v: String) = _uiState.update { it.copy(port = v.filter(Char::isDigit), statusMessage = null) }
    fun onTimeZoneChange(v: String) = _uiState.update { it.copy(timeZoneId = v, statusMessage = null) }
    fun onSyncIntervalChange(v: String) = _uiState.update { it.copy(syncIntervalMinutes = v.filter(Char::isDigit), statusMessage = null) }
    fun onSampleCountChange(v: String) = _uiState.update { it.copy(sampleCount = v.filter(Char::isDigit), statusMessage = null) }
    fun onTimeoutChange(v: String) = _uiState.update { it.copy(timeoutMs = v.filter(Char::isDigit), statusMessage = null) }
    fun onMaxRttChange(v: String) = _uiState.update { it.copy(maxAcceptableRttMs = v.filter(Char::isDigit), statusMessage = null) }
    fun onMaxOffsetChange(v: String) = _uiState.update { it.copy(maxOffsetBeforeSetMs = v.filter(Char::isDigit), statusMessage = null) }
    fun onAutoSyncToggle(enabled: Boolean) = _uiState.update { it.copy(autoSyncEnabled = enabled, statusMessage = null) }
    fun onSyncOnBootToggle(enabled: Boolean) = _uiState.update { it.copy(syncOnBootEnabled = enabled, statusMessage = null) }

    fun toggleAdminUnlock() {
        _uiState.update { it.copy(isAdminUnlocked = !it.isAdminUnlocked, statusMessage = null) }
    }

    fun saveSettings() {
        val s = _uiState.value
        val port = s.port.toIntOrNull()?.takeIf { it in 1..65535 }
        val interval = s.syncIntervalMinutes.toLongOrNull()?.coerceAtLeast(NtpSettings.MIN_SYNC_INTERVAL_MINUTES)
        val samples = s.sampleCount.toIntOrNull()?.coerceIn(1, 10)
        val timeout = s.timeoutMs.toIntOrNull()?.coerceIn(300, 15_000)
        val maxRtt = s.maxAcceptableRttMs.toLongOrNull()?.coerceIn(50L, 15_000L)
        val maxOffset = s.maxOffsetBeforeSetMs.toLongOrNull()?.coerceAtLeast(0L)

        if (s.host.isBlank() || port == null || interval == null || samples == null ||
            timeout == null || maxRtt == null || maxOffset == null || s.timeZoneId.isBlank()
        ) {
            _uiState.update { it.copy(statusMessage = "Giá trị cấu hình không hợp lệ, vui lòng kiểm tra lại.") }
            return
        }

        viewModelScope.launch {
            container.settingsRepository.updateSettings { current ->
                current.copy(
                    host = s.host.trim(),
                    port = port,
                    timeZoneId = s.timeZoneId.trim(),
                    syncIntervalMinutes = interval,
                    sampleCount = samples,
                    timeoutMs = timeout,
                    maxAcceptableRttMs = maxRtt,
                    maxOffsetBeforeSetMs = maxOffset,
                    autoSyncEnabled = s.autoSyncEnabled,
                    syncOnBootEnabled = s.syncOnBootEnabled,
                )
            }
            _uiState.update { it.copy(statusMessage = "Đã lưu cấu hình thành công.") }
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            container.settingsRepository.resetDefaults()
            _uiState.update { it.copy(statusMessage = "Đã khôi phục cấu hình mặc định.") }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(container) as T
            }
    }
}

