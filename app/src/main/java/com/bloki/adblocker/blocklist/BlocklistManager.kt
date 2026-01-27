package com.bloki.adblocker.blocklist

import android.content.Context
import com.bloki.adblocker.BlokiApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Downloads, caches, and loads blocklists into the BlocklistEngine.
 */
class BlocklistManager(
    private val context: Context,
    private val engine: BlocklistEngine
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val cacheDir = File(context.filesDir, "blocklists").also { it.mkdirs() }

    /**
     * Initialize default sources in DB if empty, then load all enabled lists.
     */
    suspend fun initialize() {
        val dao = BlokiApp.instance.database.blocklistSourceDao()
        // Insert defaults if not present
        BlocklistSources.defaults.forEach { dao.insert(it) }

        // Load whitelist
        val whitelistDomains = BlokiApp.instance.database.whitelistDao().getAllDomains()
        engine.setWhitelist(whitelistDomains)

        // Load cached blocklists
        loadCachedLists()

        // If no domains loaded (first run), download lists automatically
        if (engine.blockedCount == 0) {
            updateLists()
        }
    }

    /**
     * Download all enabled lists and reload the engine.
     */
    suspend fun updateLists() {
        val dao = BlokiApp.instance.database.blocklistSourceDao()
        val sources = dao.getEnabled()

        for (source in sources) {
            try {
                val domains = downloadList(source.url)
                saveToDisk(source.url, domains)
                dao.update(source.copy(
                    domainCount = domains.size,
                    lastUpdated = System.currentTimeMillis()
                ))
            } catch (_: Exception) {
                // Keep cached version
            }
        }

        loadCachedLists()
    }

    private suspend fun loadCachedLists() {
        engine.clearBlockedDomains()
        val dao = BlokiApp.instance.database.blocklistSourceDao()
        val sources = dao.getEnabled()

        for (source in sources) {
            val file = cacheFile(source.url)
            if (file.exists()) {
                val domains = file.readLines().filter { it.isNotBlank() }
                engine.addBlockedDomains(domains)
            }
        }
    }

    private suspend fun downloadList(url: String): List<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()

        parseHostsFile(body)
    }

    private fun parseHostsFile(content: String): List<String> {
        return content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith('#') }
            .mapNotNull { line ->
                // Hosts format: "0.0.0.0 domain.com" or "127.0.0.1 domain.com" or just "domain.com"
                val parts = line.split(Regex("\\s+"))
                when {
                    parts.size >= 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1") -> {
                        val domain = parts[1].lowercase().trimEnd('.')
                        if (domain != "localhost" && domain.contains('.')) domain else null
                    }
                    parts.size == 1 && parts[0].contains('.') && !parts[0].startsWith('#') -> {
                        parts[0].lowercase().trimEnd('.')
                    }
                    else -> null
                }
            }
            .distinct()
            .toList()
    }

    private fun saveToDisk(url: String, domains: List<String>) {
        cacheFile(url).writeText(domains.joinToString("\n"))
    }

    private fun cacheFile(url: String): File {
        val filename = url.hashCode().toString(16) + ".txt"
        return File(cacheDir, filename)
    }
}
