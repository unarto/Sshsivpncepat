package com.sivpn.cepat.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

object PingUtility {

    /**
     * Measures TCP socket connection latency to the specified host and port (or default 80 if invalid).
     * This is extremely reliable, doesn't require ICMP permissions, and simulates real connection overhead.
     */
    suspend fun measureLatency(host: String, port: Int, timeoutMs: Int = 2500): Long = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext -1L
        
        val cleanHost = host.trim()
        val cleanPort = if (port in 1..65535) port else 80

        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(cleanHost, cleanPort), timeoutMs)
            val duration = System.currentTimeMillis() - startTime
            duration
        } catch (e: IOException) {
            // Fallback to simpler ICMP Ping process
            measureIcmpPing(cleanHost, timeoutMs)
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Fallback ICMP ping using system process execution.
     */
    private suspend fun measureIcmpPing(host: String, timeoutMs: Int): Long = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val timeoutSec = maxOf(1, timeoutMs / 1000)
            val process = ProcessBuilder("ping", "-c", "1", "-w", timeoutSec.toString(), host)
                .redirectErrorStream(true)
                .start()
            
            val exitCode = kotlinx.coroutines.withTimeoutOrNull((timeoutSec + 1) * 1000L) {
                process.waitFor()
            }
            
            if (exitCode == null) {
                process.destroy()
                return@withContext -1L
            }
            
            if (exitCode == 0) {
                System.currentTimeMillis() - startTime
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }
}
