package com.bloki.adblocker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Insert
    suspend fun insert(log: DnsQueryLog)

    @Query("SELECT COUNT(*) FROM dns_query_log WHERE blocked = 1 AND timestamp > :since")
    fun blockedCountSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM dns_query_log WHERE timestamp > :since")
    fun totalCountSince(since: Long): Flow<Int>

    @Query("""
        SELECT domain, COUNT(*) as cnt
        FROM dns_query_log
        WHERE blocked = 1 AND timestamp > :since
        GROUP BY domain
        ORDER BY cnt DESC
        LIMIT :limit
    """)
    fun topBlockedDomains(since: Long, limit: Int = 10): Flow<List<DomainCount>>

    @Query("DELETE FROM dns_query_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}

data class DomainCount(val domain: String, val cnt: Int)
