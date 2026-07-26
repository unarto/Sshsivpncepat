package com.sivpn.cepat.monitor

import android.content.Context
import android.net.TrafficStats
import com.sivpn.cepat.model.SpeedInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SpeedMonitor {

    fun monitorSpeed(
        context: Context,
        isVpnActive: Boolean,
        connectionState: String,
        speedometerEnabled: Boolean
    ): Flow<SpeedInfo> = flow {
        var lastRxBytes = 0L
        var lastTxBytes = 0L
        var lastTime = System.currentTimeMillis()

        while (true) {
            if (isVpnActive && connectionState == "CONNECTED" && speedometerEnabled) {
                val uid = context.applicationInfo.uid
                val currentRx = TrafficStats.getUidRxBytes(uid)
                val currentTx = TrafficStats.getUidTxBytes(uid)
                val currentTime = System.currentTimeMillis()

                val timeDeltaSec = (currentTime - lastTime) / 1000.0
                var rxSpeed = 0L
                var txSpeed = 0L

                if (timeDeltaSec > 0.1) {
                    if (lastRxBytes > 0L && currentRx >= lastRxBytes) {
                        rxSpeed = ((currentRx - lastRxBytes) / timeDeltaSec).toLong()
                    }
                    if (lastTxBytes > 0L && currentTx >= lastTxBytes) {
                        txSpeed = ((currentTx - lastTxBytes) / timeDeltaSec).toLong()
                    }
                }

                lastRxBytes = if (currentRx != TrafficStats.UNSUPPORTED.toLong()) currentRx else 0L
                lastTxBytes = if (currentTx != TrafficStats.UNSUPPORTED.toLong()) currentTx else 0L
                lastTime = currentTime

                emit(SpeedInfo(rxBytesPerSec = rxSpeed, txBytesPerSec = txSpeed))
            } else {
                lastRxBytes = 0L
                lastTxBytes = 0L
                lastTime = System.currentTimeMillis()
                emit(SpeedInfo(0L, 0L))
            }
            delay(500)
        }
    }.flowOn(Dispatchers.IO)
}
