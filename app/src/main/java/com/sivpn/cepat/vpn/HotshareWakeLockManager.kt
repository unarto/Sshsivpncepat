package com.sivpn.cepat.vpn

import com.sivpn.cepat.config.SettingsManager

import android.content.Context
import android.os.PowerManager

object HotshareWakeLockManager {
    private var wakeLock: PowerManager.WakeLock? = null

    @Synchronized
    fun acquire(context: Context) {
        if (!SettingsManager(context).getHotshareWakeLockEnabled()) {
            LogManager.addLog("[Hotshare WakeLock] Fitur WakeLock dinonaktifkan di setelan.")
            return
        }

        if (wakeLock == null) {
            try {
                val appContext = context.applicationContext
                val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SiVPN::HotshareWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire()
                }
                LogManager.addLog("[Hotshare WakeLock] WakeLock khusus Hotshare berhasil diaktifkan.")
            } catch (e: Exception) {
                LogManager.addLog("[Hotshare WakeLock] Gagal mengaktifkan WakeLock: ${e.message}")
            }
        }
    }

    @Synchronized
    fun release() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                LogManager.addLog("[Hotshare WakeLock] WakeLock khusus Hotshare dilepas secara aman.")
            }
        } catch (e: Exception) {
            LogManager.addLog("[Hotshare WakeLock] Gagal melepas WakeLock: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    @Synchronized
    fun isHeld(): Boolean {
        return wakeLock?.isHeld == true
    }
}
