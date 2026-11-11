package com.example.ntpclock.presentation.poc

import com.example.ntpclock.core.common.DeviceProfile
import com.example.ntpclock.ntp.NtpQueryConfig
import com.example.ntpclock.sync.SyncExecutionResult

data class PocUiState(
    val currentSystemTimeText: String = "--:--:--.---",
    val currentDateText: String = "--/--/----",
    val currentTimezoneId: String = "",
    val isTargetTimezone: Boolean = false,
    val deviceProfile: DeviceProfile = DeviceProfile(
        model = "",
        manufacturer = "",
        device = "",
        androidVersion = "",
        apiLevel = 0,
        securityPatch = "",
        buildFingerprint = "",
    ),
    val packageName: String = "",
    val receiverComponentName: String = "",
    val isLanConnected: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val isAdminActive: Boolean = false,
    val isAutoTimeEnabled: Boolean = true,
    val isAutoTimeZoneEnabled: Boolean = true,
    val ntpHost: String = NtpQueryConfig.DEFAULT_HOST,
    val ntpPort: String = NtpQueryConfig.DEFAULT_PORT.toString(),
    val isBusy: Boolean = false,
    val lastSyncResult: SyncExecutionResult? = null,
    val logs: List<String> = emptyList(),
)

