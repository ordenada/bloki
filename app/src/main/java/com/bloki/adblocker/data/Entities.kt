package com.bloki.adblocker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dns_query_log")
data class DnsQueryLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val blocked: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "whitelist")
data class WhitelistEntry(
    @PrimaryKey val domain: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocklist_source")
data class BlocklistSource(
    @PrimaryKey val url: String,
    val name: String,
    val enabled: Boolean = true,
    val domainCount: Int = 0,
    val lastUpdated: Long = 0
)
