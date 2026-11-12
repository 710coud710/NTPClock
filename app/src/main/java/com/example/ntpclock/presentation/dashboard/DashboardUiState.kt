package com.example.ntpclock.presentation.dashboard

import com.example.ntpclock.domain.model.NtpSettings
import com.example.ntpclock.domain.model.SyncHistoryRecord
import com.example.ntpclock.sync.SyncExecutionResult

data class DashboardUiState(
    val currentSystemTimeText: String = "--:--:--",
    val currentMillisText: String = ".---",
    val currentNtpTimeText: String? = null,
    val currentNtpMillisText: String? = null,
    val ntpOffsetMillis: Long? = null,
    val currentDateText: String = "--/--/----",
    val currentTimezoneId: String = "",
    val isTargetTimezone: Boolean = false,
    val settings: NtpSettings = NtpSettings(),
    val isLanConnected: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val isAutoTimeEnabled: Boolean = true,
    val receiverComponentName: String = "",
    val latestSuccessfulSync: SyncHistoryRecord? = null,
    val lastSyncResult: SyncExecutionResult? = null,
    val periodicWorkStateText: String = "Chưa lập lịch",
    val isBusy: Boolean = false,
)
