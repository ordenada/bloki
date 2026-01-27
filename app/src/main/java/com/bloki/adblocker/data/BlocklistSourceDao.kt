package com.bloki.adblocker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BlocklistSourceDao {

    @Query("SELECT * FROM blocklist_source ORDER BY name ASC")
    fun getAll(): Flow<List<BlocklistSource>>

    @Query("SELECT * FROM blocklist_source WHERE enabled = 1")
    suspend fun getEnabled(): List<BlocklistSource>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: BlocklistSource)

    @Update
    suspend fun update(source: BlocklistSource)

    @Query("DELETE FROM blocklist_source WHERE url = :url")
    suspend fun delete(url: String)
}
