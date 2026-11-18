package com.example.ntpclock.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ntpclock.AppContainer
import com.example.ntpclock.domain.model.SyncTriggerSource
import com.example.ntpclock.service.NtpForegroundService
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
            // Tự động kiểm tra NTP đọc ghi nhận giờ chuẩn ngay khi mở ứng dụng (không hiện banner kết quả)
            testNtpReadOnly(isUserAction = false)
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
                val sample = lastResult?.sample
                val currentElapsedNanos = container.clockProvider.elapsedRealtimeNanos()

                val (ntpTimePart, ntpMillisPart, calculatedOffset) = when {
                    sample != null -> {
                        // Tính toán giờ NTP chuẩn dựa trên Monotonic Clock (elapsedRealtimeNanos).
                        // Giờ NTP này ĐỘC LẬP HOÀN TOÀN với việc giờ thiết bị (System.currentTimeMillis)
                        // bị người dùng thay đổi thủ công trong Cài đặt hệ thống.
                        val elapsedSinceT4Ms = (currentElapsedNanos - sample.t4ElapsedNanos).coerceAtLeast(0L) / 1_000_000L
                        val trueNtpTimeNow = sample.estimatedServerTimeAtT4Millis + elapsedSinceT4Ms
                        val ntpFull = container.timeZoneManager.formatTime(trueNtpTimeNow)
                        val dotIndexNtp = ntpFull.indexOf('.')
                        val timeNtp = if (dotIndexNtp > 0) ntpFull.substring(0, dotIndexNtp) else ntpFull
                        val millisNtp = if (dotIndexNtp > 0) ntpFull.substring(dotIndexNtp) else ""
                        val currentOffset = trueNtpTimeNow - now
                        Triple(timeNtp, millisNtp, currentOffset)
                    }
                    else -> {
                        val fallbackRecord = _uiState.value.latestSuccessfulSync
                        val offset = fallbackRecord?.offsetMillis
                        if (offset != null) {
                            val ntpNow = now + offset
                            val ntpFull = container.timeZoneManager.formatTime(ntpNow)
                            val dotIndexNtp = ntpFull.indexOf('.')
                            val timeNtp = if (dotIndexNtp > 0) ntpFull.substring(0, dotIndexNtp) else ntpFull
                            val millisNtp = if (dotIndexNtp > 0) ntpFull.substring(dotIndexNtp) else ""
                            Triple(timeNtp, millisNtp, offset)
                        } else {
                            Triple(null, null, null)
                        }
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        currentSystemTimeText = timePart,
                        currentMillisText = millisPart,
                        currentNtpTimeText = ntpTimePart,
                        currentNtpMillisText = ntpMillisPart,
                        ntpOffsetMillis = calculatedOffset,
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
                _uiState.update { it.copy(lastSyncResult = result, showResultBanner = true) }
                if (_uiState.value.settings.autoSyncEnabled) {
                    NtpForegroundService.triggerSyncNow(container.appContext)
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
                refreshPolicyStatus()
            }
        }
    }

    fun testNtpReadOnly(isUserAction: Boolean = true) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val result = container.syncCoordinator.queryNtpOnly()
                _uiState.update { it.copy(lastSyncResult = result, showResultBanner = isUserAction) }
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
