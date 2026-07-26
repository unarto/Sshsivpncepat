package com.sivpn.cepat.monitor

import com.sivpn.cepat.vpn.PingUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class PingMonitor {

    fun monitorPing(
        connectionState: String,
        sshHost: String,
        sshPort: String,
        pingAddress: String
    ): Flow<Long> = flow {
        if (connectionState == "CONNECTED") {
            while (true) {
                val target = pingAddress.trim()
                val (host, port) = if (target.isNotEmpty()) {
                    if (target.contains(":")) {
                        val parts = target.split(":")
                        val h = parts[0].trim()
                        val p = parts.getOrNull(1)?.toIntOrNull() ?: 80
                        Pair(h, p)
                    } else {
                        Pair(target, 80)
                    }
                } else {
                    Pair(sshHost, sshPort.toIntOrNull() ?: 80)
                }

                val latency = PingUtility.measureLatency(host, port)
                emit(latency)
                delay(3000)
            }
        } else {
            emit(-1L)
        }
    }.flowOn(Dispatchers.IO)
}
