package com.sivpn.cepat

import android.util.Log
import com.sivpn.cepat.vpn.LogManager
import java.io.File

/**
 * Service wrapper untuk antarmuka JNI Native Library hev-socks5-tunnel.
 * Menyediakan manajemen pemuatan library native dan pemanggilan fungsi SOCKS5 tunnel secara aman.
 */
object TProxyService {
    const val ACTION_DISCONNECT = "com.sivpn.cepat.vpn.DISCONNECT"
    private const val TAG = "TProxyService"

    /**
     * Status yang menunjukkan apakah native library `hev-socks5-tunnel` telah berhasil dimuat.
     */
    @JvmStatic
    var isLibraryLoaded: Boolean = false
        internal set

    init {
        try {
            System.loadLibrary("hev-socks5-tunnel")
            isLibraryLoaded = true
            Log.i(TAG, "Berhasil memuat library hev-socks5-tunnel dari system loadLibrary.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Gagal memuat libhev-socks5-tunnel.so via System.loadLibrary", e)
            isLibraryLoaded = false
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException saat memuat libhev-socks5-tunnel.so", e)
            isLibraryLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Exception tidak terduga saat memuat libhev-socks5-tunnel.so", e)
            isLibraryLoaded = false
        }
    }

    /**
     * Memuat native library `hev-socks5-tunnel` dari lokasi file eksternal (path).
     */
    @JvmStatic
    fun loadFromPath(path: String): Boolean {
        if (isLibraryLoaded) {
            Log.d(TAG, "Library hev-socks5-tunnel sudah dimuat sebelumnya.")
            return true
        }

        if (path.isBlank()) {
            val msg = "Gagal memuat library: Path kosong atau null."
            Log.e(TAG, msg)
            LogManager.addLog("TProxyService: $msg")
            return false
        }

        return try {
            System.load(path)
            isLibraryLoaded = true
            Log.i(TAG, "Berhasil memuat libhev-socks5-tunnel dari path: \$path")
            LogManager.addLog("TProxyService: Berhasil memuat library dari path: \$path")
            true
        } catch (e: UnsatisfiedLinkError) {
            val msg = "UnsatisfiedLinkError dari path \$path: \${e.message}"
            Log.e(TAG, msg, e)
            LogManager.addLog("TProxyService: \$msg")
            false
        } catch (e: SecurityException) {
            val msg = "SecurityException dari path \$path: \${e.message}"
            Log.e(TAG, msg, e)
            LogManager.addLog("TProxyService: \$msg")
            false
        } catch (e: Exception) {
            val msg = "Exception saat memuat library dari path \$path: \${e.message}"
            Log.e(TAG, msg, e)
            LogManager.addLog("TProxyService: \$msg")
            false
        }
    }

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyStartService(configPath: String, fd: Int): Boolean

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyStopService(): Boolean

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyIsRunning(): Boolean

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyGetStats(): LongArray?

    @JvmStatic
    fun isServiceRunning(): Boolean {
        return try { if (isLibraryLoaded) TProxyIsRunning() else false } catch(e: Exception) { false }
    }

    @JvmStatic
    @Synchronized
    fun startServiceSafe(configPath: String, fd: Int) {
        if (!isLibraryLoaded) {
            val msg = "Gagal menjalankan StartService, libhev-socks5-tunnel belum dimuat!"
            LogManager.addLog("TProxyService: \$msg")
            Log.e(TAG, msg)
            return
        }

        if (isServiceRunning()) {
            val msg = "TProxyService sudah berjalan. Mengabaikan permintaan start."
            LogManager.addLog("TProxyService: \$msg")
            Log.i(TAG, msg)
            return
        }

        if (configPath.isBlank()) {
            val msg = "Gagal menjalankan StartService, configPath tidak boleh kosong!"
            LogManager.addLog("TProxyService: \$msg")
            Log.e(TAG, msg)
            return
        }

        val configFile = File(configPath)
        if (!configFile.exists() || !configFile.canRead()) {
            val msg = "Gagal menjalankan StartService, configPath tidak ditemukan atau tidak dapat dibaca: \$configPath"
            LogManager.addLog("TProxyService: \$msg")
            Log.e(TAG, msg)
            return
        }

        if (fd < 0) {
            val msg = "Gagal menjalankan StartService, fd tidak valid (\$fd < 0)!"
            LogManager.addLog("TProxyService: \$msg")
            Log.e(TAG, msg)
            return
        }

        try {
            LogManager.addLog("TProxyService: Memulai TProxyStartService dengan fd: \$fd")
            Log.i(TAG, "Memulai TProxyStartService dengan config: \$configPath, fd: \$fd")
            val success = TProxyStartService(configPath, fd)
            if (!success) LogManager.addLog("TProxyStartService returned false")
        } catch (e: UnsatisfiedLinkError) {
            val msg = "UnsatisfiedLinkError saat StartService: \${e.message}"
            LogManager.addLog("TProxyService: \$msg")
            Log.e(TAG, msg, e)
        } catch (e: SecurityException) {
            val msg = "SecurityException saat StartService: \${e.message}"
            LogManager.addLog("TProxyService: \$msg")
            Log.e(TAG, msg, e)
        } catch (e: Exception) {
            val msg = "Error saat StartService: \${e.message}"
            LogManager.addLog("TProxyService: \$msg")
            Log.e(TAG, msg, e)
        }
    }

    @JvmStatic
    @Synchronized
    fun stopServiceSafe() {
        if (!isLibraryLoaded) {
            val msg = "Gagal menjalankan StopService, libhev-socks5-tunnel belum dimuat!"
            LogManager.addLog("TProxyService: \$msg")
            Log.e(TAG, msg)
            return
        }

        if (!isServiceRunning()) {
            val msg = "TProxyService belum berjalan. Mengabaikan permintaan stop."
            Log.d(TAG, msg)
            return
        }

        try {
            LogManager.addLog("TProxyService: Menghentikan TProxyStopService")
            Log.i(TAG, "Menghentikan TProxyStopService")
            val success = TProxyStopService()
            if (!success) LogManager.addLog("TProxyStopService returned false")
        } catch (e: UnsatisfiedLinkError) {
            val msg = "UnsatisfiedLinkError saat StopService: \${e.message}"
            LogManager.addLog("TProxyService: \$msg")
            Log.e(TAG, msg, e)
        } catch (e: SecurityException) {
            val msg = "SecurityException saat StopService: \${e.message}"
            LogManager.addLog("TProxyService: \$msg")
            Log.e(TAG, msg, e)
        } catch (e: Exception) {
            val msg = "Error saat StopService: \${e.message}"
            LogManager.addLog("TProxyService: \$msg")
            Log.e(TAG, msg, e)
        }
    }

    @JvmStatic
    fun getStatsSafe(): LongArray? {
        if (!isLibraryLoaded) {
            Log.w(TAG, "TProxyGetStats dipanggil tetapi library belum dimuat.")
            return null
        }

        if (!isServiceRunning()) {
            return null
        }

        return try {
            TProxyGetStats()
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "UnsatisfiedLinkError saat GetStats: \${e.message}", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException saat GetStats: \${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error saat GetStats: \${e.message}", e)
            null
        }
    }
}
