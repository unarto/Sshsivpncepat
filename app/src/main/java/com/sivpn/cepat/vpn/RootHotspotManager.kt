package com.sivpn.cepat.vpn

import com.topjohnwu.superuser.Shell

object RootHotspotManager {

    fun isRootAvailable(): Boolean {
        // Request root access, returns true if granted
        return Shell.getShell().isRoot
    }

    fun startHotspotRouting(): Boolean {
        if (!isRootAvailable()) {
            LogManager.addLog("Akses Root tidak tersedia. Root Hotspot gagal diaktifkan.")
            return false
        }

        LogManager.addLog("Mengaktifkan routing Hotspot Root (via libsu)...")
        val cmds = listOf(
            "sysctl -w net.ipv4.ip_forward=1",
            "iptables -I FORWARD -j ACCEPT",
            "iptables -t nat -I POSTROUTING -o tun0 -j MASQUERADE"
        )
        
        val result = Shell.cmd(*cmds.toTypedArray()).exec()
        return if (result.isSuccess) {
            LogManager.addLog("Routing Hotspot Root berhasil diterapkan.")
            true
        } else {
            LogManager.addLog("Gagal menerapkan routing Hotspot Root.")
            false
        }
    }

    fun stopHotspotRouting() {
        if (!isRootAvailable()) return

        LogManager.addLog("Menonaktifkan routing Hotspot Root...")
        val cmds = listOf(
            "iptables -D FORWARD -j ACCEPT 2>/dev/null",
            "iptables -t nat -D POSTROUTING -o tun0 -j MASQUERADE 2>/dev/null",
            "sysctl -w net.ipv4.ip_forward=0"
        )
        Shell.cmd(*cmds.toTypedArray()).exec()
        LogManager.addLog("Routing Hotspot Root dihapus.")
    }
}
