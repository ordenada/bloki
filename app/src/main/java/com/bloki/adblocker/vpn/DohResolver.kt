package com.bloki.adblocker.vpn

import android.util.Log
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class DohResolver {

    private val client = OkHttpClient.Builder()
        .dns(StaticDns)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val dnsMessageType = "application/dns-message".toMediaType()

    fun resolve(dnsQuery: ByteArray): ByteArray? {
        val request = Request.Builder()
            .url(DOH_URL)
            .post(dnsQuery.toRequestBody(dnsMessageType))
            .header("Accept", "application/dns-message")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    Log.w(TAG, "DoH HTTP ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DoH request failed", e)
            null
        }
    }

    private object StaticDns : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return listOf(InetAddress.getByName(hostname))
        }
    }

    companion object {
        private const val TAG = "BlokiDoH"
        private const val DOH_URL = "https://1.1.1.1/dns-query"
    }
}
