package com.example.ntpclock.presentation.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ntpclock.AppContainer
import com.example.ntpclock.admin.PolicyActionResult
import com.example.ntpclock.core.common.DeviceProfile
import com.example.ntpclock.domain.model.SyncTriggerSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiagnosticsUiState(
    val deviceProfile: DeviceProfile = DeviceProfile.current(),
    val packageName: String = "",
    val receiverComponentName: String = "",
    val isLanConnected: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val isAdminActive: Boolean = false,
    val isAutoTimeEnabled: Boolean = true,
    val isAutoTimeZoneEnabled: Boolean = true,
    val currentTimezoneId: String = "",
    val targetTimezoneId: String = "",
    val periodicWorkState: String = "UNKNOWN",
    val logs: List<String> = emptyList(),
)

class DiagnosticsViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        refreshStatus(logEvent = true)
        viewModelScope.launch {
            container.syncWorkScheduler.observePeriodicWorkInfo().collect { info ->
                _uiState.update {
                    it.copy(periodicWorkState = info?.state?.name ?: "NOT_SCHEDULED")
                }
            }
        }
    }

    fun refreshStatus(logEvent: Boolean = false) {
        val admin = container.deviceOwnerManager.getStatus()
        val profile = DeviceProfile.current()
        val lan = container.networkMonitor.isLocalNetworkConnected()
        val autoTime = container.systemClockController.isAutoTimeEnabled()
        val autoTz = container.systemClockController.isAutoTimeZoneEnabled()
        val tz = container.timeZoneManager.currentTimeZoneId()

        _uiState.update {
            it.copy(
                deviceProfile = profile,
                packageName = admin.packageName,
                receiverComponentName = admin.receiverComponentName,
                isLanConnected = lan,
                isDeviceOwner = admin.isDeviceOwner,
                isAdminActive = admin.isAdminActive,
                isAutoTimeEnabled = autoTime,
                isAutoTimeZoneEnabled = autoTz,
                currentTimezoneId = tz,
            )
        }
        if (logEvent) {
            appendLog("Status: DO=${admin.isDeviceOwner}, AutoTime=$autoTime, AutoTZ=$autoTz, LAN=$lan, TZ=$tz")
        }
    }

    fun disableAutoTime() {
        val result = container.systemClockController.disableAutoTime()
        logPolicyResult("AUTO_TIME", result)
        refreshStatus()
    }

    fun applyTargetTimezone() {
        viewModelScope.launch {
            val targetTz = container.settingsRepository.getCurrentSettings().timeZoneId
            val result = container.systemClockController.setTimeZone(targetTz)
            logPolicyResult("TIMEZONE($targetTz)", result)
            refreshStatus()
        }
    }

    fun enqueueImmediateWorkerSync() {
        container.syncWorkScheduler.enqueueOneTimeSync(SyncTriggerSource.MANUAL)
        appendLog("WorkManager: Đã gửi OneTimeWorkRequest đồng bộ nền")
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    private fun logPolicyResult(tag: String, result: PolicyActionResult) {
        when (result) {
            is PolicyActionResult.Success -> appendLog("$tag OK: ${result.detail}")
            is PolicyActionResult.Denied -> appendLog("$tag DENIED: ${result.reason}")
            is PolicyActionResult.Unsupported -> appendLog("$tag UNSUPPORTED: ${result.reason}")
            is PolicyActionResult.Failed -> appendLog("$tag FAILED: ${result.reason}")
        }
    }

    private fun appendLog(message: String) {
        val time = container.timeZoneManager.formatTime(container.clockProvider.currentTimeMillis())
        _uiState.update { state ->
            state.copy(logs = (listOf("[$time] $message") + state.logs).take(50))
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DiagnosticsViewModel(container) as T
            }
    }
}

