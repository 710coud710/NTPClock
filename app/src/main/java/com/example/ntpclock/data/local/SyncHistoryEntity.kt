package com.example.ntpclock.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ntpclock.domain.model.SyncHistoryRecord
import com.example.ntpclock.domain.model.SyncTriggerSource
import com.example.ntpclock.sync.SyncStatus

@Entity(tableName = "sync_history")
data class SyncHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val runAtEpochMillis: Long,
    val triggerSource: String,
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
    val status: String,
    val errorMessage: String?,
    val samplesAttempted: Int,
    val samplesValid: Int,
) {
    fun toDomain(): SyncHistoryRecord = SyncHistoryRecord(
        id = id,
        runAtEpochMillis = runAtEpochMillis,
        triggerSource = runCatching { SyncTriggerSource.valueOf(triggerSource) }
            .getOrDefault(SyncTriggerSource.MANUAL),
        ntpServer = ntpServer,
        ntpPort = ntpPort,
        rttMillis = rttMillis,
        offsetMillis = offsetMillis,
        stratum = stratum,
        leapIndicator = leapIndicator,
        beforeEpochMillis = beforeEpochMillis,
        targetEpochMillis = targetEpochMillis,
        afterEpochMillis = afterEpochMillis,
        postSyncDiffMillis = postSyncDiffMillis,
        apiSuccess = apiSuccess,
        status = runCatching { SyncStatus.valueOf(status) }.getOrDefault(SyncStatus.NTP_FAILED),
        errorMessage = errorMessage,
        samplesAttempted = samplesAttempted,
        samplesValid = samplesValid,
    )

    companion object {
        fun fromDomain(record: SyncHistoryRecord): SyncHistoryEntity = SyncHistoryEntity(
            id = record.id,
            runAtEpochMillis = record.runAtEpochMillis,
            triggerSource = record.triggerSource.name,
            ntpServer = record.ntpServer,
            ntpPort = record.ntpPort,
            rttMillis = record.rttMillis,
            offsetMillis = record.offsetMillis,
            stratum = record.stratum,
            leapIndicator = record.leapIndicator,
            beforeEpochMillis = record.beforeEpochMillis,
            targetEpochMillis = record.targetEpochMillis,
            afterEpochMillis = record.afterEpochMillis,
            postSyncDiffMillis = record.postSyncDiffMillis,
            apiSuccess = record.apiSuccess,
            status = record.status.name,
            errorMessage = record.errorMessage,
            samplesAttempted = record.samplesAttempted,
            samplesValid = record.samplesValid,
        )
    }
}

