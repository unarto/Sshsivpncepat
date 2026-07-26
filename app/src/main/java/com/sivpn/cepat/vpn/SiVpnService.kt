package com.sivpn.cepat.vpn

import com.sivpn.cepat.config.SettingsManager

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.widget.Toast
import com.sivpn.cepat.vpn.service.VpnInterfaceConfigurator
import com.sivpn.cepat.vpn.service.VpnMonitors
import com.sivpn.cepat.vpn.service.VpnNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

class SiVpnService : VpnService() {

    companion object {
        const val ACTION_STOP = "com.sivpn.cepat.vpn.STOP"
        
        private val _isRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
        val isRunningFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isRunning.asStateFlow()
        
        private val _connectionState = kotlinx.coroutines.flow.MutableStateFlow("DISCONNECTED")
        val connectionStateFlow: kotlinx.coroutines.flow.StateFlow<String> = _connectionState.asStateFlow()
        
        private val _connectionStartTime = kotlinx.coroutines.flow.MutableStateFlow(0L)
        val connectionStartTimeFlow: kotlinx.coroutines.flow.StateFlow<Long> = _connectionStartTime.asStateFlow()
        
        // Helper properties for backward compatibility
        var isRunning: Boolean
            get() = _isRunning.value
            set(value) { _isRunning.value = value }
            
        var connectionState: String
            get() = _connectionState.value
            set(value) { _connectionState.value = value }
            
        var connectionStartTime: Long
            get() = _connectionStartTime.value
            set(value) { _connectionStartTime.value = value }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private var logCleanupJob: Job? = null
    private var notificationJob: Job? = null
    private var sshJob: Job? = null
    private var hevJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    @Volatile private var isHevRunning = false

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("SiVpnService", "awal onStartCommand: action = ${intent?.action}")
        if (intent?.action == ACTION_STOP || intent?.action == com.sivpn.cepat.TProxyService.ACTION_DISCONNECT) {
            android.util.Log.d("SiVpnService", "Menerima action stop, mematikan VPN")
            isRunning = false
            connectionState = "DISCONNECTED"
            
            runBlocking {
                withTimeoutOrNull(3000) {
                    cleanupProcesses()
                }
            }
            
            SiVpnQsTileService.requestUpdate(this)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }
        
        if (isRunning && vpnJob?.isActive == true) {
            LogManager.addLog("VPN service sudah berjalan, mengabaikan start request ganda.")
            VpnNotificationManager.updateForegroundNotification(this, "Koneksi aman sedang berjalan")
            return START_STICKY
        }

        acquireWakeLock()

        VpnNotificationManager.showForegroundNotification(this)
        isRunning = true
        connectionState = "CONNECTING"
        connectionStartTime = 0L
        SiVpnQsTileService.requestUpdate(this)
        startVpnScope()
        startPeriodicLogCleanup()
        return START_STICKY
    }

    private fun startPeriodicLogCleanup() {
        if (logCleanupJob?.isActive == true) return
        logCleanupJob = serviceScope.launch {
            VpnMonitors.runLogCleanupMonitor(this@SiVpnService)
        }
    }

    private fun startVpnScope() {
        if (vpnJob?.isActive == true) return
        vpnJob = serviceScope.launch {
            val model = android.os.Build.MODEL
            val sdkInt = android.os.Build.VERSION.SDK_INT
            val arch = if (android.os.Build.SUPPORTED_ABIS.isNotEmpty()) android.os.Build.SUPPORTED_ABIS[0] else "unknown"

            LogManager.addLog("--- Informasi Sistem ---")
            LogManager.addLog("Model Perangkat: $model")
            LogManager.addLog("Versi API Android: $sdkInt")
            LogManager.addLog("Arsitektur CPU: $arch")
            LogManager.addLog("-------------------------")

            LogManager.addLog("Memvalidasi ketersediaan library native JNI...")
            JniLibHelper.loadDownloadedLibs(this@SiVpnService)
            if (!com.sivpn.cepat.ssh.LibSsh2Client.isLibraryLoaded || !com.sivpn.cepat.hevsocks.HevManager.isLibraryLoaded) {
                val errMsg = "Koneksi Dibatalkan: File library JNI (.so) tidak lengkap atau gagal dimuat!\n" +
                        "libssh.so: " + (if (com.sivpn.cepat.ssh.LibSsh2Client.isLibraryLoaded) "Ditemukan" else "TIDAK DITEMUKAN") + ", " +
                        "libhev-socks5-tunnel.so: " + (if (com.sivpn.cepat.hevsocks.HevManager.isLibraryLoaded) "Ditemukan" else "TIDAK DITEMUKAN") + ".\n" +
                        "Pastikan file-file tersebut terpasang di folder jniLibs."
                LogManager.addLog(errMsg)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SiVpnService, "Gagal memulai VPN: Library native (.so) tidak ditemukan!", Toast.LENGTH_LONG).show()
                }
                connectionState = "DISCONNECTED"
                stopSelf()
                return@launch
            }

            LogManager.addLog("Extracting assets...")
            ConnectionManager.extractAssets(this@SiVpnService)

            launch(Dispatchers.IO) {
                VpnMonitors.runDurationMonitor(this@SiVpnService) { stopSelf() }
            }

            launch(Dispatchers.IO) {
                VpnMonitors.runKeepAliveMonitor(this@SiVpnService)
            }

            launch(Dispatchers.IO) {
                VpnMonitors.runSpeedometerMonitor(this@SiVpnService)
            }

            var reconnectDelayMs = 2000L
            val minDelayMs = 2000L
            val maxDelayMs = 32000L

            while (isRunning) {
                connectionState = "CONNECTING"
                cleanupProcesses()

                try {
                    setupVpnInterface()
                } catch (se: SecurityException) {
                    LogManager.addLog("ERROR KEAMANAN VPN: Gagal melakukan establish VPN.")
                    LogManager.addLog("- Kemungkinan 1: Izin VPN ditolak, dicabut, atau diblokir.")
                    LogManager.addLog("- Kemungkinan 2: Aplikasi VPN lain mengaktifkan 'Always-on VPN'.")
                    LogManager.addLog("- Detail: ${se.message}")
                    connectionState = "DISCONNECTED"
                    stopSelf()
                    return@launch
                } catch (e: Exception) {
                    LogManager.addLog("Gagal setup VPN Interface: ${e.message}. Retrying in ${reconnectDelayMs / 1000}s...")
                    delay(reconnectDelayMs)
                    reconnectDelayMs = minOf(reconnectDelayMs * 2, maxDelayMs)
                    continue
                }

                val socksReady = com.sivpn.cepat.ssh.TunnelManager.startTunnel(this@SiVpnService, serviceScope)


                if (!isRunning) break

                if (!socksReady || !com.sivpn.cepat.ssh.TunnelManager.isSshRunning) {
                    if (!com.sivpn.cepat.ssh.TunnelManager.isSshRunning) {
                        LogManager.addLog("SSH terputus saat menunggu SOCKS5 siap.")
                    } else {
                        LogManager.addLog("Error: SOCKS5 port 1080 tidak merespon dalam 5 detik.")
                        com.sivpn.cepat.ssh.TunnelManager.stopTunnel()
                    }

                    if (!SettingsManager(this@SiVpnService).getAutoReconnectEnabled()) {
                        LogManager.addLog("Koneksi gagal. Auto-Reconnect dimatikan. Menghentikan VPN.")
                        connectionState = "DISCONNECTED"
                        stopSelf()
                        break
                    }
                    LogManager.addLog("Retrying in ${reconnectDelayMs / 1000}s...")
                    delay(reconnectDelayMs)
                    reconnectDelayMs = minOf(reconnectDelayMs * 2, maxDelayMs)
                    continue
                }

                LogManager.addLog("SOCKS5 port 1080 siap. Starting hev-socks5-tunnel natively...")
                isHevRunning = true
                hevJob = launch(Dispatchers.IO) {
                    try {
                        val fd = vpnInterface?.fd
                        if (fd != null) {
                            com.sivpn.cepat.hevsocks.HevManager.start(this@SiVpnService, fd)
                        }
                    } catch (t: Throwable) {
                        LogManager.addLog("Error HEV Native: ${t.javaClass.simpleName} - ${t.message}")
                    } finally {
                        isHevRunning = false
                    }
                }

                var hevReady = false
                withContext(Dispatchers.IO) {
                    for (i in 1..30) { // Timeout 3 detik
                        if (!isRunning || !isHevRunning) break
                        if (com.sivpn.cepat.hevsocks.HevManager.isRunning()) {
                            hevReady = true
                            break
                        }
                        delay(100)
                    }
                }

                if (!isRunning) break

                if (hevReady && com.sivpn.cepat.ssh.TunnelManager.isSshRunning && isHevRunning) {
                    LogManager.addLog("VPN Running! Koneksi berhasil terjalin.")
                    connectionState = "CONNECTED"
                    if (connectionStartTime == 0L) {
                        connectionStartTime = System.currentTimeMillis()
                    }
                    reconnectDelayMs = minDelayMs
                } else {
                    if (!hevReady) {
                        LogManager.addLog("Error: HEV Tunnel gagal berjalan dalam waktu 3 detik.")
                    }
                    if (!SettingsManager(this@SiVpnService).getAutoReconnectEnabled()) {
                        LogManager.addLog("Failed to establish tunnel. Auto-Reconnect dimatikan. Menghentikan VPN.")
                        connectionState = "DISCONNECTED"
                        stopSelf()
                        break
                    }
                    LogManager.addLog("Failed to establish tunnel. Retrying in ${reconnectDelayMs / 1000}s...")
                    delay(reconnectDelayMs)
                    reconnectDelayMs = minOf(reconnectDelayMs * 2, maxDelayMs)
                    continue
                }

                while (isRunning && com.sivpn.cepat.ssh.TunnelManager.isSshRunning && isHevRunning) {
                    delay(1000)
                }

                if (isRunning) {
                    val reason = when {
                        !com.sivpn.cepat.ssh.TunnelManager.isSshRunning -> "Library SSH (libssh) terputus"
                        !isHevRunning -> "Library HEV Tunnel terputus"
                        else -> "Koneksi tidak stabil"
                    }
                    
                    if (!SettingsManager(this@SiVpnService).getAutoReconnectEnabled()) {
                        LogManager.addLog("Koneksi terputus: $reason. Auto-Reconnect dimatikan. Menghentikan VPN.")
                        connectionState = "DISCONNECTED"
                        stopSelf()
                        break
                    }
                    
                    LogManager.addLog("Auto-Reconnect: $reason! Memulai rekoneksi otomatis...")
                    connectionState = "CONNECTING"
                    cleanupProcesses()
                    
                    delay(reconnectDelayMs)
                    reconnectDelayMs = minOf(reconnectDelayMs * 2, maxDelayMs)
                }
            }
        }
    }

    private suspend fun cleanupProcesses() {
        // 1. stop HEV
        try {
            com.sivpn.cepat.hevsocks.HevManager.stop()
        } catch (e: Throwable) {
            LogManager.addLog("Failed to quit HevSocks5Tunnel JNI: ${e.message}")
        }
        hevJob?.cancelAndJoin()
        hevJob = null
        isHevRunning = false

        // 2. stop SSH
        try {
            com.sivpn.cepat.ssh.TunnelManager.stopTunnel()
        } catch (e: Exception) {
            LogManager.addLog("Failed to interrupt SSH JNI: ${e.message}")
        }
        
        // Wait for native SSH to actually stop
        var retryCount = 0
        while (com.sivpn.cepat.ssh.TunnelManager.isSshRunning && retryCount < 30) {
            kotlinx.coroutines.delay(100)
            retryCount++
        }
        if (com.sivpn.cepat.ssh.TunnelManager.isSshRunning) {
            LogManager.addLog("Warning: SSH Tunnel did not stop gracefully in time.")
            com.sivpn.cepat.ssh.TunnelManager.isSshRunning = false // Force reset as last resort
        }

        // 3. stop PayloadInjector
        try {
            
        } catch (e: Exception) {
            LogManager.addLog("Failed to stop PayloadInjector: ${e.message}")
        }

        // 4. stop LocalPortForwarder
        try {
            LocalPortForwarder.stop()
        } catch (e: Exception) {
            LogManager.addLog("Failed to stop LocalPortForwarder: ${e.message}")
        }

        // 5. stop HttpProxyServer
        try {
            HttpProxyServer.stop()
        } catch (e: Exception) {
            LogManager.addLog("Failed to stop HttpProxyServer: ${e.message}")
        }

        // 6. tutup VPN Interface
        if (!isRunning) {
            try {
                vpnInterface?.close()
                LogManager.addLog("VPN Interface ditutup (shutdown).")
            } catch (e: Exception) {
                LogManager.addLog("Error closing VPN Interface: ${e.message}")
            }
            vpnInterface = null
            
            // Hapus file konfigurasi HEV
            try {
                java.io.File(filesDir, "hev_config.yml").delete()
                LogManager.addLog("File konfigurasi hev_config.yml dihapus.")
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    @Throws(SecurityException::class, Exception::class)
    private fun setupVpnInterface() {
        LogManager.addLog("Configuring VPN Interface...")
        val builder = Builder()
        VpnInterfaceConfigurator.configure(builder, this)
        
        // Menutup antarmuka lama sebelum establish yang baru untuk mencegah FD leak
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            LogManager.addLog("Error closing old VPN Interface: ${e.message}")
        }
        vpnInterface = null
        
        LogManager.addLog("Membangun ulang antarmuka VPN (establish)...")
        val newInterface = builder.establish()
        if (newInterface == null) {
            if (prepare(this) != null) {
                throw SecurityException("Izin VPN dicabut oleh sistem (prepare() mengembalikan Intent).")
            } else {
                throw Exception("builder.establish() mengembalikan null, tetapi prepare() sukses.")
            }
        }
        
        vpnInterface = newInterface
        LogManager.addLog("VPN Interface established successfully with MTU 1500")
    }

    private fun acquireWakeLock() {
        if (!SettingsManager(this).getVpnWakeLockEnabled()) {
            LogManager.addLog("VPN WakeLock is disabled in settings. Skipping acquire to save battery (CPU can sleep).")
            return
        }
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SiVPN::WakeLock")
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
            LogManager.addLog("WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            LogManager.addLog("WakeLock released")
        }
    }

    override fun onRevoke() {
        LogManager.addLog("Izin VPN dicabut oleh sistem (onRevoke).")
        connectionState = "DISCONNECTED"
        stopSelf()
        super.onRevoke()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        LogManager.addLog("Aplikasi dihapus dari Recent Tasks (onTaskRemoved). Menghentikan VPN.")
        connectionState = "DISCONNECTED"
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        android.util.Log.d("SiVpnService", "awal stopService() / onDestroy")
        super.onDestroy()
        isRunning = false
        connectionState = "DISCONNECTED"
        connectionStartTime = 0L
        vpnJob?.cancel()
        vpnJob = null
        logCleanupJob?.cancel()
        logCleanupJob = null
        notificationJob?.cancel()
        notificationJob = null

        runBlocking {
            withTimeoutOrNull(3000) {
                cleanupProcesses()
            }
        }

        serviceJob.cancel()

        releaseWakeLock()
        wakeLock = null
        
        SiVpnQsTileService.requestUpdate(this)
        
        LogManager.addLog("SiVPN Service Stopped")
    }
}
