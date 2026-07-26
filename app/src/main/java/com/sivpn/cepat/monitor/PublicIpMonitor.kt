package com.sivpn.cepat.monitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

class PublicIpMonitor {

    fun monitorPublicIp(
        connectionState: String,
        sshHost: String,
        autoRefreshEnabled: Boolean,
        intervalSeconds: Int,
        manualRefreshTrigger: Int
    ): Flow<String> = flow {
        if (connectionState == "CONNECTED") {
            emit("Memeriksa IP...")
            delay(1500) // Wait for VPN routing tables

            val resolvedIp = fetchPublicIpWithRetry(sshHost)
            emit(resolvedIp)

            while (autoRefreshEnabled) {
                delay(intervalSeconds * 1000L)
                val refreshedIp = fetchPublicIpWithRetry(sshHost)
                emit(refreshedIp)
            }
        } else {
            // Disconnected: resolve domain of server
            val hostIp = try {
                if (sshHost.isNotEmpty()) InetAddress.getByName(sshHost).hostAddress ?: sshHost else ""
            } catch (e: Exception) {
                sshHost
            }
            emit(hostIp)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetchPublicIpWithRetry(sshHost: String): String {
        var resolvedIp = ""
        for (retry in 1..3) {
            try {
                val url = URL("https://api.ipify.org")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.useCaches = false
                
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP Error: ${conn.responseCode}")
                }
                
                val ip = conn.inputStream.bufferedReader().use { it.readText().trim() }
                if (ip.isNotEmpty() && ip.contains(".")) {
                    resolvedIp = ip
                    break
                }
            } catch (e: Exception) {
                try {
                    val url2 = URL("https://ifconfig.me/ip")
                    val conn2 = url2.openConnection() as HttpURLConnection
                    conn2.connectTimeout = 6000
                    conn2.readTimeout = 6000
                    conn2.useCaches = false
                    
                    if (conn2.responseCode != HttpURLConnection.HTTP_OK) {
                        throw Exception("HTTP Error: ${conn2.responseCode}")
                    }
                    
                    val ip2 = conn2.inputStream.bufferedReader().use { it.readText().trim() }
                    if (ip2.isNotEmpty()) {
                        resolvedIp = ip2
                        break
                    }
                } catch (ex: Exception) {
                    try {
                        val url3 = URL("https://ipv4.icanhazip.com")
                        val conn3 = url3.openConnection() as HttpURLConnection
                        conn3.connectTimeout = 6000
                        conn3.readTimeout = 6000
                        conn3.useCaches = false
                        
                        if (conn3.responseCode != HttpURLConnection.HTTP_OK) {
                            throw Exception("HTTP Error: ${conn3.responseCode}")
                        }
                        
                        val ip3 = conn3.inputStream.bufferedReader().use { it.readText().trim() }
                        if (ip3.isNotEmpty()) {
                            resolvedIp = ip3
                            break
                        }
                    } catch (ey: Exception) {
                        delay(2000)
                    }
                }
            }
        }

        if (resolvedIp.isEmpty() && sshHost.isNotEmpty()) {
            resolvedIp = try {
                InetAddress.getByName(sshHost).hostAddress ?: sshHost
            } catch (e: Exception) {
                sshHost
            }
        }
        return resolvedIp
    }
}
