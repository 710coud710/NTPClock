package com.example.ntpclock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncHistoryDao {
    @Query("SELECT * FROM sync_history ORDER BY runAtEpochMillis DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SyncHistoryEntity>>

    @Query(
        "SELECT * FROM sync_history WHERE status IN ('CLOCK_SYNC_SUCCESS', 'NTP_SUCCESS') " +
            "ORDER BY runAtEpochMillis DESC, id DESC LIMIT 1"
    )
    fun observeLatestSuccessful(): Flow<SyncHistoryEntity?>

    @Insert
    suspend fun insert(entity: SyncHistoryEntity)

    @Query("DELETE FROM sync_history")
    suspend fun clearAll()
}

