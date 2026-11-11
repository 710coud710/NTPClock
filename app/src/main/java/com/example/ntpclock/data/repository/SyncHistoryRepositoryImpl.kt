package com.example.ntpclock.data.repository

import com.example.ntpclock.data.local.SyncHistoryDao
import com.example.ntpclock.data.local.SyncHistoryEntity
import com.example.ntpclock.domain.model.SyncHistoryRecord
import com.example.ntpclock.domain.repository.SyncHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SyncHistoryRepositoryImpl(
    private val dao: SyncHistoryDao,
) : SyncHistoryRepository {
    override fun observeHistory(limit: Int): Flow<List<SyncHistoryRecord>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun observeLatestSuccessfulSync(): Flow<SyncHistoryRecord?> =
        dao.observeLatestSuccessful().map { it?.toDomain() }

    override suspend fun insertRecord(record: SyncHistoryRecord) {
        dao.insert(SyncHistoryEntity.fromDomain(record))
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}

