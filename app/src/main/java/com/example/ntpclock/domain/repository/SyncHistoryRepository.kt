package com.example.ntpclock.domain.repository

import com.example.ntpclock.domain.model.SyncHistoryRecord
import kotlinx.coroutines.flow.Flow

interface SyncHistoryRepository {
    fun observeHistory(limit: Int = 100): Flow<List<SyncHistoryRecord>>
    fun observeLatestSuccessfulSync(): Flow<SyncHistoryRecord?>
    suspend fun insertRecord(record: SyncHistoryRecord)
    suspend fun clearAll()
}

