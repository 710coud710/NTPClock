package com.example.ntpclock.sync

import com.example.ntpclock.domain.model.SyncTriggerSource
import com.example.ntpclock.ntp.NtpSample

enum class SyncStatus {
    NTP_SUCCESS,
    CLOCK_SYNC_SUCCESS,
    CLOCK_SYNC_DENIED,
    NTP_FAILED,
    CLOCK_SYNC_VERIFY_FAILED,
}

data class SyncExecutionResult(
    val status: SyncStatus,
    val message: String,
    val triggerSource: SyncTriggerSource = SyncTriggerSource.MANUAL,
    val sample: NtpSample? = null,
    val samplesAttempted: Int = 1,
    val samplesValid: Int = if (sample != null) 1 else 0,
    val beforeEpochMillis: Long,
    val targetEpochMillis: Long? = null,
    val afterEpochMillis: Long? = null,
    val postSyncDiffMillis: Long? = null,
    val executedAtEpochMillis: Long,
)
