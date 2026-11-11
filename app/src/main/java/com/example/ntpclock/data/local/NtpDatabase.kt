package com.example.ntpclock.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SyncHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NtpDatabase : RoomDatabase() {
    abstract fun syncHistoryDao(): SyncHistoryDao

    companion object {
        fun create(context: Context): NtpDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NtpDatabase::class.java,
                "ntp_clock.db",
            ).build()
    }
}

