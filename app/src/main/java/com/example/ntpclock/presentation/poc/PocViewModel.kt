package com.example.ntpclock.presentation.poc

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ntpclock.admin.AndroidDeviceOwnerManager
import com.example.ntpclock.admin.AndroidSystemClockController
import com.example.ntpclock.admin.DeviceOwnerManager
import com.example.ntpclock.admin.PolicyActionResult
import com.example.ntpclock.admin.SystemClockController
import com.example.ntpclock.core.common.DeviceProfile
import com.example.ntpclock.core.network.AndroidNetworkMonitor
import com.example.ntpclock.core.network.NetworkMonitor
import com.example.ntpclock.core.time.ClockProvider
import com.example.ntpclock.core.time.SystemClockProvider
import com.example.ntpclock.core.time.TimeZoneManager
import com.example.ntpclock.ntp.NtpQueryConfig
import com.example.ntpclock.ntp.UdpNtpClient
import com.example.ntpclock.sync.NtpSyncCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PocViewModel(
    private val clockProvider: ClockProvider,
    private val timeZoneManager: TimeZoneManager,
    private val networkMonitor: NetworkMonitor,
    private val deviceOwnerManager: DeviceOwnerManager,
    private val systemClockController: SystemClockController,
    private val syncCoordinator: NtpSyncCoordinator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PocUiState())
    val uiState: StateFlow<PocUiState> = _uiState.asStateFlow()

    init {
        refreshAllStatus(logEvent = true)
        startClockTicker()
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                val now = clockProvider.currentTimeMillis()
                _uiState.update { state ->
                    state.copy(
                        currentSystemTimeText = timeZoneManager.formatTime(now),
                        currentDateText = timeZoneManager.formatDate(now),
                        currentTimezoneId = timeZoneManager.currentTimeZoneId(),
                        isTargetTimezone = timeZoneManager.isTargetTimeZone(),
                    )
                }
                delay(500L)
            }
        }
    }

    fun onNtpHostChanged(host: String) {
        _uiState.update { it.copy(ntpHost = host) }
    }

    fun onNtpPortChanged(port: String) {
        if (port.all { it.isDigit() } && port.length <= 5) {
            _uiState.update { it.copy(ntpPort = port) }
        }
    }

    fun refreshAllStatus(logEvent: Boolean = false) {
        val adminStatus = deviceOwnerManager.getStatus()
        val profile = DeviceProfile.current()
        val lanConnected = networkMonitor.isLocalNetworkConnected()
        val autoTime = systemClockController.isAutoTimeEnabled()
        val autoTz = systemClockController.isAutoTimeZoneEnabled()
        val now = clockProvider.currentTimeMillis()

        _uiState.update { state ->
            state.copy(
                currentSystemTimeText = timeZoneManager.formatTime(now),
                currentDateText = timeZoneManager.formatDate(now),
                currentTimezoneId = timeZoneManager.currentTimeZoneId(),
                isTargetTimezone = timeZoneManager.isTargetTimeZone(),
                deviceProfile = profile,
                packageName = adminStatus.packageName,
                receiverComponentName = adminStatus.receiverComponentName,
                isLanConnected = lanConnected,
                isDeviceOwner = adminStatus.isDeviceOwner,
                isAdminActive = adminStatus.isAdminActive,
                isAutoTimeEnabled = autoTime,
                isAutoTimeZoneEnabled = autoTz,
            )
        }

        if (logEvent) {
            appendLog(
                "Status: DO=${adminStatus.isDeviceOwner}, AutoTime=$autoTime, LAN=$lanConnected, TZ=${timeZoneManager.currentTimeZoneId()}, API=${profile.apiLevel}"
            )
        }
    }

    fun disableAutoTime() {
        if (_uiState.value.isBusy) return
        val result = systemClockController.disableAutoTime()
        when (result) {
            is PolicyActionResult.Success -> appendLog("AUTO_TIME: ${result.detail}")
            is PolicyActionResult.Denied -> appendLog("AUTO_TIME DENIED: ${result.reason}")
            is PolicyActionResult.Unsupported -> appendLog("AUTO_TIME UNSUPPORTED: ${result.reason}")
            is PolicyActionResult.Failed -> appendLog("AUTO_TIME FAILED: ${result.reason}")
        }
        refreshAllStatus()
    }

    fun applyVietnamTimezone() {
        if (_uiState.value.isBusy) return
        val result = systemClockController.setTimeZone(TimeZoneManager.TARGET_TIMEZONE_ID)
        when (result) {
            is PolicyActionResult.Success -> appendLog("TIMEZONE: ${result.detail}")
            is PolicyActionResult.Denied -> appendLog("TIMEZONE DENIED: ${result.reason}")
            is PolicyActionResult.Unsupported -> appendLog("TIMEZONE UNSUPPORTED: ${result.reason}")
            is PolicyActionResult.Failed -> appendLog("TIMEZONE FAILED: ${result.reason}")
        }
        refreshAllStatus()
    }

    fun testNtpOnly() {
        val config = buildConfigOrLogError() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                appendLog("Query NTP -> ${config.host}:${config.port} ...")
                val result = syncCoordinator.queryNtpOnly(config)
                _uiState.update { it.copy(lastSyncResult = result) }
                appendLog("${result.status}: ${result.message}")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
                refreshAllStatus()
            }
        }
    }

    fun syncAndApplyClock() {
        val config = buildConfigOrLogError() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                appendLog("Sync & Set System Clock -> ${config.host}:${config.port} ...")
                val result = syncCoordinator.syncAndApplyClock(config)
                _uiState.update { it.copy(lastSyncResult = result) }
                appendLog("${result.status}: ${result.message}")
            } finally {
                _uiState.update { it.copy(isBusy = false) }
                refreshAllStatus()
            }
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    private fun buildConfigOrLogError(): NtpQueryConfig? {
        if (_uiState.value.isBusy) return null
        val host = _uiState.value.ntpHost.trim()
        val port = _uiState.value.ntpPort.toIntOrNull()
        if (host.isEmpty() || port == null || port !in 1..65535) {
            appendLog("Cấu hình NTP không hợp lệ (host='$host', port='${_uiState.value.ntpPort}')")
            return null
        }
        return NtpQueryConfig(host = host, port = port)
    }

    private fun appendLog(message: String) {
        val timestamp = timeZoneManager.formatTime(clockProvider.currentTimeMillis())
        _uiState.update { state ->
            val updated = listOf("[$timestamp] $message") + state.logs
            state.copy(logs = updated.take(MAX_LOG_LINES))
        }
    }

    companion object {
        private const val MAX_LOG_LINES = 50

        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val clockProvider = SystemClockProvider()
                    val timeZoneManager = TimeZoneManager()
                    val networkMonitor = AndroidNetworkMonitor(appContext)
                    val deviceOwnerManager = AndroidDeviceOwnerManager(appContext)
                    val clockController = AndroidSystemClockController(appContext, deviceOwnerManager)
                    val ntpClient = UdpNtpClient(clockProvider)
                    val coordinator = NtpSyncCoordinator(
                        networkMonitor = networkMonitor,
                        deviceOwnerManager = deviceOwnerManager,
                        systemClockController = clockController,
                        ntpClient = ntpClient,
                        clockProvider = clockProvider,
                    )
                    return PocViewModel(
                        clockProvider = clockProvider,
                        timeZoneManager = timeZoneManager,
                        networkMonitor = networkMonitor,
                        deviceOwnerManager = deviceOwnerManager,
                        systemClockController = clockController,
                        syncCoordinator = coordinator,
                    ) as T
                }
            }
    }
}

