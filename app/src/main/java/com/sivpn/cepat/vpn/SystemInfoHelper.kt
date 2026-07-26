package com.sivpn.cepat.vpn

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

object SystemInfoHelper {

    suspend fun logSystemInfo(context: Context) = withContext(Dispatchers.IO) {
        try {
            val model = Build.MODEL
            val sdkInt = Build.VERSION.SDK_INT
            
            LogManager.addLog("=== DETAIL PERANGKAT ===")
            LogManager.addLog("Model Perangkat : $model")
            LogManager.addLog("Versi Android    : SDK $sdkInt")

            // SIM Card / Carrier Info
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simInfo = if (telephonyManager != null) {
                val operatorName = runCatching {
                    telephonyManager.simOperatorName.ifEmpty {
                        telephonyManager.networkOperatorName
                    }
                }.getOrDefault("")
                
                val simState = runCatching { telephonyManager.simState }
                    .getOrDefault(TelephonyManager.SIM_STATE_UNKNOWN)
                val stateText = when (simState) {
                    TelephonyManager.SIM_STATE_READY -> "Ready / Aktif"
                    TelephonyManager.SIM_STATE_ABSENT -> "Tidak Ada / Tidak Terdeteksi"
                    TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Terkunci Jaringan"
                    TelephonyManager.SIM_STATE_PIN_REQUIRED -> "Perlu PIN"
                    TelephonyManager.SIM_STATE_PUK_REQUIRED -> "Perlu PUK"
                    TelephonyManager.SIM_STATE_UNKNOWN -> "Tidak Diketahui"
                    else -> "Lainnya"
                }

                if (operatorName.isNotEmpty()) {
                    "$operatorName (Status SIM: $stateText)"
                } else {
                    "Status SIM: $stateText"
                }
            } else {
                "TelephonyManager tidak didukung"
            }
            LogManager.addLog("Informasi SIM   : $simInfo")

            // Local Network Interfaces and IP Addresses
            LogManager.addLog("Daftar IP Lokal:")
            var ipFound = false
            val ipList = mutableListOf<String>()
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                if (interfaces != null) {
                    while (interfaces.hasMoreElements()) {
                        val intf = interfaces.nextElement()
                        if (intf.isLoopback || !intf.isUp) continue
                        
                        val addrs = intf.inetAddresses
                        while (addrs.hasMoreElements()) {
                            val addr = addrs.nextElement()
                            if (addr is Inet4Address) {
                                val ip = addr.hostAddress ?: ""
                                if (ip.isNotEmpty()) {
                                    val interfaceFriendlyName = when {
                                        intf.name.contains("wlan", ignoreCase = true) -> "WiFi (${intf.name})"
                                        intf.name.contains("rmnet", ignoreCase = true) -> "Data Seluler (${intf.name})"
                                        intf.name.contains("ap", ignoreCase = true) || intf.name.contains("softap", ignoreCase = true) -> "Hotspot (${intf.name})"
                                        intf.name.contains("rndis", ignoreCase = true) -> "USB Tethering (${intf.name})"
                                        else -> intf.name
                                    }
                                    ipList.add("   - $interfaceFriendlyName: $ip")
                                    ipFound = true
                                }
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                // Ignore empty or restricted interface logs
            }

            if (ipFound) {
                ipList.forEach { LogManager.addLog(it) }
            } else {
                LogManager.addLog("   - Gagal mendeteksi IP lokal aktif.")
            }
            LogManager.addLog("========================")
        } catch (e: Exception) {
            LogManager.addLog("Gagal membaca detail perangkat: ${e.message}")
        }
    }
}
