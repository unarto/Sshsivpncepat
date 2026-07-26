package com.sivpn.cepat.vpn.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.sivpn.cepat.vpn.LogManager
import com.sivpn.cepat.config.SettingsManager

object VpnInterfaceConfigurator {
    fun configure(builder: VpnService.Builder, context: Context) {
        builder.setBlocking(false)
        
        val settings = SettingsManager(context)
        val ipv4 = settings.getHevIpv4().takeIf { it.isNotBlank() } ?: com.sivpn.cepat.config.DefaultValues.DEFAULT_TUN_IPV4
        builder.addAddress(ipv4, 24)
        builder.addRoute("0.0.0.0", 0)
        
        val ipv6 = settings.getHevIpv6().takeIf { it.isNotBlank() } ?: com.sivpn.cepat.config.DefaultValues.DEFAULT_TUN_IPV6
        if (ipv6.isNotBlank()) {
            try {
                builder.addAddress(ipv6, 128)
                builder.addRoute("::", 0)
                LogManager.addLog("IPv6 route configured: $ipv6")
            } catch (e: Exception) {
                LogManager.addLog("Failed to configure IPv6: ${e.message}")
            }
        }

        val dnsString = settings.getDns()
        dnsString.split(Regex("[:;,\\s]+")).forEach { dnsIp ->
            if (dnsIp.isNotBlank()) {
                try {
                    builder.addDnsServer(dnsIp.trim())
                } catch (e: Exception) {
                    LogManager.addLog("Failed to add DNS $dnsIp: ${e.message}")
                }
            }
        }

        builder.setMtu(1500)
        builder.setSession("SiVPN")

        try {
            val configIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                Intent(context, Class.forName("com.sivpn.cepat.ui.MainActivity")),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            builder.setConfigureIntent(configIntent)
        } catch (e: Exception) {
            LogManager.addLog("Gagal menetapkan Configure Intent: ${e.message}")
        }

        if (SettingsManager(context).getKillSwitchEnabled()) {
            LogManager.addLog("Kill Switch aktif: Mengunci lalu lintas internet selama rekoneksi.")
        } else {
            LogManager.addLog("Kill Switch nonaktif.")
        }

        if (SettingsManager(context).getSplitTunnelingEnabled()) {
            val bypassApps = SettingsManager(context).getBypassApps()
            val filterMode = SettingsManager(context).getAppsFilterMode()
            if (filterMode == "filter") {
                LogManager.addLog("Apps Filter (Only Tunnel) aktif. Memproses ${bypassApps.size} aplikasi...")
                for (packageName in bypassApps) {
                    if (packageName.isNotBlank()) {
                        if (packageName == context.packageName) {
                            LogManager.addLog("Abaikan aplikasi sendiri (SIVPN) dari mode Only Tunnel untuk mencegah routing loop.")
                            continue
                        }
                        try {
                            builder.addAllowedApplication(packageName)
                            LogManager.addLog("Hanya rute VPN untuk: $packageName")
                        } catch (e: Exception) {
                            LogManager.addLog("Gagal menambahkan rute $packageName: ${e.message}")
                        }
                    }
                }
            } else {
                LogManager.addLog("Bypass Aplikasi aktif. Memproses ${bypassApps.size} aplikasi bypass...")
                for (packageName in bypassApps) {
                    if (packageName.isNotBlank()) {
                        try {
                            builder.addDisallowedApplication(packageName)
                            LogManager.addLog("Aplikasi bypass: $packageName")
                        } catch (e: Exception) {
                            LogManager.addLog("Gagal menambahkan bypass $packageName: ${e.message}")
                        }
                    }
                }
            }
        }

        try {
            val filterMode = SettingsManager(context).getAppsFilterMode()
            val splitTunneling = SettingsManager(context).getSplitTunnelingEnabled()
            if (!splitTunneling || filterMode != "filter") {
                builder.addDisallowedApplication(context.packageName)
                LogManager.addLog("Aplikasi sendiri (SIVPN) dibypass untuk menghindari routing loop.")
            }
        } catch (e: Exception) {
            LogManager.addLog("Gagal menambahkan self-bypass: ${e.message}")
        }
    }
}
