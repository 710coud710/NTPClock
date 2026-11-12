package com.example.ntpclock.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ntpclock.AppContainer
import com.example.ntpclock.admin.PolicyActionResult
import com.example.ntpclock.core.time.TimeZoneManager
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
    val showSaveConfirmDialog: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val isAutoTimeEnabled: Boolean = true,
    val isAutoTimeZoneEnabled: Boolean = true,
    val currentTimezoneId: String = "",
    val receiverComponentName: String = "",
    val policyStatusMessage: String? = null,
    val statusMessage: String? = null,
)

class SettingsViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshPolicyStatus()
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
                    )
                }
            }
        }
    }

    fun refreshPolicyStatus() {
        val admin = container.deviceOwnerManager.getStatus()
        _uiState.update { state ->
            state.copy(
                isDeviceOwner = admin.isDeviceOwner,
                receiverComponentName = admin.receiverComponentName,
                isAutoTimeEnabled = container.systemClockController.isAutoTimeEnabled(),
                isAutoTimeZoneEnabled = container.systemClockController.isAutoTimeZoneEnabled(),
                currentTimezoneId = container.timeZoneManager.currentTimeZoneId(),
            )
        }
    }

    fun disableAutoTime() {
        val result = container.systemClockController.disableAutoTime()
        _uiState.update { it.copy(policyStatusMessage = formatPolicyResult("Auto Time", result)) }
        refreshPolicyStatus()
    }

    fun applyTargetTimezone() {
        val targetTz = _uiState.value.timeZoneId.trim().ifEmpty { TimeZoneManager.TARGET_TIMEZONE_ID }
        val result = container.systemClockController.setTimeZone(targetTz)
        _uiState.update { it.copy(policyStatusMessage = formatPolicyResult("Timezone ($targetTz)", result)) }
        refreshPolicyStatus()
    }

    private fun formatPolicyResult(label: String, result: PolicyActionResult): String = when (result) {
        is PolicyActionResult.Success -> "$label: ${result.detail}"
        is PolicyActionResult.Denied -> "$label bị từ chối: ${result.reason}"
        is PolicyActionResult.Unsupported -> "$label chưa hỗ trợ API: ${result.reason}"
        is PolicyActionResult.Failed -> "$label thất bại: ${result.reason}"
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

    fun requestSaveConfirmation() {
        if (parseValidatedInputs(_uiState.value) == null) {
            _uiState.update {
                it.copy(statusMessage = "Giá trị cấu hình không hợp lệ, vui lòng kiểm tra lại.")
            }
            return
        }
        _uiState.update { it.copy(showSaveConfirmDialog = true, statusMessage = null) }
    }

    fun dismissSaveConfirmation() {
        _uiState.update { it.copy(showSaveConfirmDialog = false) }
    }

    fun confirmSaveSettings() {
        val parsed = parseValidatedInputs(_uiState.value)
        if (parsed == null) {
            _uiState.update {
                it.copy(
                    showSaveConfirmDialog = false,
                    statusMessage = "Giá trị cấu hình không hợp lệ, vui lòng kiểm tra lại.",
                )
            }
            return
        }

        _uiState.update { it.copy(showSaveConfirmDialog = false) }
        viewModelScope.launch {
            container.settingsRepository.updateSettings { current ->
                current.copy(
                    host = parsed.host,
                    port = parsed.port,
                    timeZoneId = parsed.timeZoneId,
                    syncIntervalMinutes = parsed.syncIntervalMinutes,
                    sampleCount = parsed.sampleCount,
                    timeoutMs = parsed.timeoutMs,
                    maxAcceptableRttMs = parsed.maxAcceptableRttMs,
                    maxOffsetBeforeSetMs = parsed.maxOffsetBeforeSetMs,
                    autoSyncEnabled = parsed.autoSyncEnabled,
                    syncOnBootEnabled = parsed.syncOnBootEnabled,
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

    private fun parseValidatedInputs(s: SettingsUiState): ValidatedSettings? {
        val port = s.port.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        val interval = s.syncIntervalMinutes.toLongOrNull()
            ?.coerceAtLeast(NtpSettings.MIN_SYNC_INTERVAL_MINUTES) ?: return null
        val samples = s.sampleCount.toIntOrNull()?.coerceIn(1, 10) ?: return null
        val timeout = s.timeoutMs.toIntOrNull()?.coerceIn(300, 15_000) ?: return null
        val maxRtt = s.maxAcceptableRttMs.toLongOrNull()?.coerceIn(50L, 15_000L) ?: return null
        val maxOffset = s.maxOffsetBeforeSetMs.toLongOrNull()?.coerceAtLeast(0L) ?: return null
        if (s.host.isBlank() || s.timeZoneId.isBlank()) return null

        return ValidatedSettings(
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

    private data class ValidatedSettings(
        val host: String,
        val port: Int,
        val timeZoneId: String,
        val syncIntervalMinutes: Long,
        val sampleCount: Int,
        val timeoutMs: Int,
        val maxAcceptableRttMs: Long,
        val maxOffsetBeforeSetMs: Long,
        val autoSyncEnabled: Boolean,
        val syncOnBootEnabled: Boolean,
    )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(container) as T
            }
    }
}
