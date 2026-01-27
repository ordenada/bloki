package com.bloki.adblocker.blocklist

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory domain matching engine backed by a HashSet.
 * Checks the domain and all parent domains against the blocklist,
 * with whitelist taking priority.
 */
class BlocklistEngine {

    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val whitelistedDomains = ConcurrentHashMap.newKeySet<String>()

    val blockedCount: Int get() = blockedDomains.size

    fun isBlocked(domain: String): Boolean {
        val normalized = domain.lowercase().trimEnd('.')

        // Whitelist check first
        if (isWhitelisted(normalized)) return false

        // Check exact domain and all parent domains
        var current = normalized
        while (current.contains('.')) {
            if (blockedDomains.contains(current)) return true
            current = current.substringAfter('.')
        }
        return blockedDomains.contains(current)
    }

    private fun isWhitelisted(domain: String): Boolean {
        var current = domain
        while (current.contains('.')) {
            if (whitelistedDomains.contains(current)) return true
            current = current.substringAfter('.')
        }
        return whitelistedDomains.contains(current)
    }

    fun addBlockedDomains(domains: Collection<String>) {
        domains.forEach { blockedDomains.add(it.lowercase().trimEnd('.')) }
    }

    fun clearBlockedDomains() {
        blockedDomains.clear()
    }

    fun setWhitelist(domains: Collection<String>) {
        whitelistedDomains.clear()
        domains.forEach { whitelistedDomains.add(it.lowercase().trimEnd('.')) }
    }

    fun addWhitelistDomain(domain: String) {
        whitelistedDomains.add(domain.lowercase().trimEnd('.'))
    }

    fun removeWhitelistDomain(domain: String) {
        whitelistedDomains.remove(domain.lowercase().trimEnd('.'))
    }
}
