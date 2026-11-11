package com.example.ntpclock.domain.model

import com.example.ntpclock.sync.SyncStatus

enum class SyncTriggerSource {
    MANUAL,
    PERIODIC_WORK,
    BOOT,
    NETWORK_RECONNECT,
}

data class SyncHistoryRecord(
    val id: Long = 0L,
    val runAtEpochMillis: Long,
    val triggerSource: SyncTriggerSource,
    val ntpServer: String,
    val ntpPort: Int,
    val rttMillis: Long?,
    val offsetMillis: Long?,
    val stratum: Int?,
    val leapIndicator: Int?,
    val beforeEpochMillis: Long,
    val targetEpochMillis: Long?,
    val afterEpochMillis: Long?,
    val postSyncDiffMillis: Long?,
    val apiSuccess: Boolean,
    val status: SyncStatus,
    val errorMessage: String?,
    val samplesAttempted: Int,
    val samplesValid: Int,
)

