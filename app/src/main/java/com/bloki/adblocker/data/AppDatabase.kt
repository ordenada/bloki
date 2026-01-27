package com.bloki.adblocker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DnsQueryLog::class, WhitelistEntry::class, BlocklistSource::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun blocklistSourceDao(): BlocklistSourceDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bloki.db"
            ).build()
        }
    }
}
