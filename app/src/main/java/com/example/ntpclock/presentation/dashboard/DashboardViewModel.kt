package com.example.ntpclock.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ntpclock.AppContainer
import com.example.ntpclock.domain.model.SyncTriggerSource
import com.example.ntpclock.sync.SyncStatus
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
        autoCheckNtpOnLaunch()
    }

    private fun autoCheckNtpOnLaunch() {
        viewModelScope.launch {
            // Tự động kiểm tra NTP đọc ghi nhận giờ chuẩn ngay khi mở ứng dụng
            testNtpReadOnly()
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                val now = container.clockProvider.currentTimeMillis()
                val full = container.timeZoneManager.formatTime(now)
                val dotIndex = full.indexOf('.')
                val timePart = if (dotIndex > 0) full.substring(0, dotIndex) else full
                val millisPart = if (dotIndex > 0) full.substring(dotIndex) else ""

                val lastResult = _uiState.value.lastSyncResult
                val activeOffset: Long? = run {
                    if (lastResult != null) {
                        when (lastResult.status) {
                            // Khi đã đồng bộ thành công vào System Clock, giờ thiết bị ĐÃ bằng giờ NTP.
                            // Do đó độ lệch thời gian thực tế lúc này chỉ còn là chênh lệch hậu kiểm (~0ms),
                            // không cộng đồn độ lệch trước khi đồng bộ nữa.
                            SyncStatus.CLOCK_SYNC_SUCCESS -> lastResult.postSyncDiffMillis ?: 0L
                            SyncStatus.NTP_SUCCESS -> lastResult.sample?.offsetMillis
                            else -> null
                        }
                    } else {
                        _uiState.value.latestSuccessfulSync?.let { record ->
                            when {
                                record.status == SyncStatus.CLOCK_SYNC_SUCCESS || record.afterEpochMillis != null -> {
                                    record.postSyncDiffMillis ?: 0L
                                }
                                record.status == SyncStatus.NTP_SUCCESS -> record.offsetMillis
                                else -> null
                            }
                        }
                    }
                }

                val (ntpTimePart, ntpMillisPart) = if (activeOffset != null) {
                    val ntpNow = now + activeOffset
                    val ntpFull = container.timeZoneManager.formatTime(ntpNow)
                    val dotIndexNtp = ntpFull.indexOf('.')
                    val timeNtp = if (dotIndexNtp > 0) ntpFull.substring(0, dotIndexNtp) else ntpFull
                    val millisNtp = if (dotIndexNtp > 0) ntpFull.substring(dotIndexNtp) else ""
                    Pair(timeNtp, millisNtp)
                } else {
                    Pair(null, null)
                }

                _uiState.update { state ->
                    state.copy(
                        currentSystemTimeText = timePart,
                        currentMillisText = millisPart,
                        currentNtpTimeText = ntpTimePart,
                        currentNtpMillisText = ntpMillisPart,
                        ntpOffsetMillis = activeOffset,
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
