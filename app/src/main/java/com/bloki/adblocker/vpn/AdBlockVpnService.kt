package com.bloki.adblocker.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bloki.adblocker.BlokiApp
import com.bloki.adblocker.MainActivity
import com.bloki.adblocker.R
import com.bloki.adblocker.data.DnsQueryLog
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream

class AdBlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    @Volatile private var running = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dohResolver = DohResolver()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun start() {
        if (running) return
        running = true
        isRunning = true

        startForegroundNotification()

        val builder = Builder()
            .setSession("Bloki Ad Blocker")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("1.1.1.1")
            .addDnsServer("1.0.0.1")
            .addRoute("1.1.1.1", 32)
            .addRoute("1.0.0.1", 32)
            .setMtu(1500)
            .setBlocking(true)
            .addDisallowedApplication(packageName)

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setConfigureIntent(pendingIntent)

        vpnInterface = builder.establish()
        if (vpnInterface == null) {
            Log.e(TAG, "Failed to establish VPN interface")
            running = false
            isRunning = false
            return
        }

        Log.i(TAG, "VPN interface established, fd=${vpnInterface!!.fd}")
        scope.launch { runPacketLoop() }
    }

    private fun stop() {
        running = false
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
        scope.cancel()
        stopForeground(true)
        stopSelf()
    }

    private suspend fun runPacketLoop() {
        val fd = vpnInterface ?: return
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(1500)
        val app = BlokiApp.instance
        val statsDao = app.database.statsDao()

        Log.i(TAG, "Packet loop started")

        try {
            while (running) {
                val length = input.read(buffer)
                if (length <= 0) {
                    Log.d(TAG, "Read returned $length, VPN likely closed")
                    break
                }

                val packet = buffer.copyOf(length)
                val query = DnsPacketParser.parse(packet, length)
                if (query == null) {
                    Log.d(TAG, "Non-DNS packet received, len=$length, proto=${if (length > 9) (packet[9].toInt() and 0xFF) else -1}")
                    continue
                }

                val domain = query.questionDomain
                Log.d(TAG, "DNS query: $domain (type=${query.questionType})")

                if (app.blocklistEngine.isBlocked(domain)) {
                    Log.d(TAG, "Blocked: $domain")
                    val response = DnsPacketParser.buildNxDomainResponse(query)
                    output.write(response)
                    scope.launch { statsDao.insert(DnsQueryLog(domain = domain, blocked = true)) }
                } else {
                    Log.d(TAG, "Forwarding: $domain")
                    try {
                        val dnsResponse = dohResolver.resolve(query.dnsPayload)
                        if (dnsResponse != null) {
                            val responsePacket = DnsPacketParser.wrapInUdpIp(query, dnsResponse)
                            output.write(responsePacket)
                            Log.d(TAG, "Response written for $domain (${dnsResponse.size} bytes)")
                        } else {
                            Log.w(TAG, "DoH returned null for $domain")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "DoH error for $domain", e)
                    }
                    scope.launch { statsDao.insert(DnsQueryLog(domain = domain, blocked = false)) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Packet loop error", e)
        } finally {
            Log.i(TAG, "Packet loop ended")
            input.close()
            output.close()
        }
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, BlokiApp.CHANNEL_VPN)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BlokiVPN"
        const val ACTION_START = "com.bloki.adblocker.START"
        const val ACTION_STOP = "com.bloki.adblocker.STOP"
        @Volatile var isRunning = false
            private set
    }
}
