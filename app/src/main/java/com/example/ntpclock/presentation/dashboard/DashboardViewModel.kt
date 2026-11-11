package com.example.ntpclock.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ntpclock.AppContainer
import com.example.ntpclock.domain.model.SyncTriggerSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshPolicyStatus()
        startClockTicker()
        observeFlows()
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                val now = container.clockProvider.currentTimeMillis()
                val full = container.timeZoneManager.formatTime(now)
                val dotIndex = full.indexOf('.')
                val timePart = if (dotIndex > 0) full.substring(0, dotIndex) else full
                val millisPart = if (dotIndex > 0) full.substring(dotIndex) else ""

                _uiState.update { state ->
                    state.copy(
                        currentSystemTimeText = timePart,
                        currentMillisText = millisPart,
                        currentDateText = container.timeZoneManager.formatDate(now),
                        currentTimezoneId = container.timeZoneManager.currentTimeZoneId(),
                        isTargetTimezone = container.timeZoneManager.isTargetTimeZone(),
                    )
                }
                delay(250L)
            }
        }
    }

    private fun observeFlows() {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            container.networkMonitor.observeLocalNetworkConnected().collect { connected ->
                _uiState.update { it.copy(isLanConnected = connected) }
            }
        }
        viewModelScope.launch {
            container.syncHistoryRepository.observeLatestSuccessfulSync().collect { latest ->
                _uiState.update { it.copy(latestSuccessfulSync = latest) }
            }
        }
        viewModelScope.launch {
            container.syncWorkScheduler.observePeriodicWorkInfo().collect { info ->
                val text = info?.state?.name ?: "Chưa lập lịch"
                _uiState.update { it.copy(periodicWorkStateText = text) }
            }
        }
    }

    fun refreshPolicyStatus() {
        val adminStatus = container.deviceOwnerManager.getStatus()
        _uiState.update { state ->
            state.copy(
                isLanConnected = container.networkMonitor.isLocalNetworkConnected(),
                isDeviceOwner = adminStatus.isDeviceOwner,
                isAutoTimeEnabled = container.systemClockController.isAutoTimeEnabled(),
                receiverComponentName = adminStatus.receiverComponentName,
                currentTimezoneId = container.timeZoneManager.currentTimeZoneId(),
                isTargetTimezone = container.timeZoneManager.isTargetTimeZone(),
            )
        }
    }

    fun syncNow() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val result = container.syncCoordinator.syncAndApplyClock(SyncTriggerSource.MANUAL)
                _uiState.update { it.copy(lastSyncResult = result) }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
                refreshPolicyStatus()
            }
        }
    }

    fun testNtpReadOnly() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val result = container.syncCoordinator.queryNtpOnly()
                _uiState.update { it.copy(lastSyncResult = result) }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
                refreshPolicyStatus()
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DashboardViewModel(container) as T
            }
    }
}

