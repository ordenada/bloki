package com.bloki.adblocker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.bloki.adblocker.blocklist.BlocklistEngine
import com.bloki.adblocker.blocklist.BlocklistManager
import com.bloki.adblocker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlokiApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var blocklistEngine: BlocklistEngine
        private set
    lateinit var blocklistManager: BlocklistManager
        private set

    private val _blocklistReady = MutableStateFlow(false)
    val blocklistReady = _blocklistReady.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.create(this)
        blocklistEngine = BlocklistEngine()
        blocklistManager = BlocklistManager(this, blocklistEngine)
        createNotificationChannel()

        // Initialize blocklists at startup (downloads on first run)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            blocklistManager.initialize()
            _blocklistReady.value = true
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_VPN,
                getString(R.string.channel_vpn),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_VPN = "vpn_service"
        lateinit var instance: BlokiApp
            private set
    }
}
