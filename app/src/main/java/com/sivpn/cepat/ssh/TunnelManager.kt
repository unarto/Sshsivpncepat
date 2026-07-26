package com.sivpn.cepat.ssh

import android.content.Context
import com.sivpn.cepat.config.SettingsManager
import com.sivpn.cepat.vpn.LogManager
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket

object TunnelManager {
    private var sshJob: Job? = null
    @Volatile var isSshRunning = false
        

    suspend fun startTunnel(context: Context, scope: CoroutineScope): Boolean {
        val settings = SettingsManager(context)
        
        val proxyHost = settings.getProxyHost()
        val proxyPort = settings.getProxyPort()
        val payload = settings.getPayload()
        val sni = settings.getSni()
        val tlsVersion = settings.getForcingTls()
        
        var actualSshHost = settings.getSshHost()
        var actualSshPort = settings.getSshPort()
        val tfoEnabled = settings.getTcpFastOpenEnabled()
        val autoPingEnabled = settings.getAutoPing()

        if (payload.isNotEmpty() || sni.isNotEmpty() || (proxyHost.isNotEmpty() && proxyPort > 0)) {
            LogManager.addLog("Starting Payload/SNI Injector Mode...")
            PayloadInjector.start(proxyHost, proxyPort, actualSshHost, actualSshPort, payload, sni, tlsVersion, tfoEnabled, autoPingEnabled)
            
            if (PayloadInjector.isRunning) {
                actualSshHost = "127.0.0.1"
                actualSshPort = PayloadInjector.localPort
                LogManager.addLog("Routing SSH connection through local proxy: $actualSshHost:$actualSshPort")
            } else {
                LogManager.addLog("Gagal memulai Payload Injector. Melanjutkan dengan Direct connect.")
            }
        }

        LogManager.addLog("Starting SSH natively...")
        isSshRunning = true
        sshJob = scope.launch(Dispatchers.IO) {
            try {
                val username = settings.getSshUsername()
                val password = settings.getSshPassword()
                
                val result = LibSsh2Client.startTunnel(actualSshHost, actualSshPort, username, password, 1080)
                
                when (result) {
                    0 -> LogManager.addLog("SSH Tunnel berhenti secara normal.")
                    -1 -> LogManager.addLog("SSH Error (-1): Gagal menginisialisasi libssh2.")
                    -3 -> LogManager.addLog("SSH Error (-3): Gagal terhubung ke remote SSH server ($actualSshHost:$actualSshPort).")
                    -4 -> LogManager.addLog("SSH Error (-4): Gagal membuat sesi SSH.")
                    -5 -> LogManager.addLog("SSH Error (-5): Handshake SSH gagal.")
                    -6 -> LogManager.addLog("SSH Error (-6): Autentikasi SSH gagal. Periksa Username/Password.")
                    -7 -> LogManager.addLog("SSH Error (-7): Gagal membuat local server socket SOCKS5.")
                    -8 -> LogManager.addLog("SSH Error (-8): Gagal melakukan bind pada local SOCKS5 port (1080).")
                    -9 -> LogManager.addLog("SSH Error (-9): Gagal listen pada local SOCKS5 port.")
                    -10 -> LogManager.addLog("SSH Error (-10): Argumen JNI tidak valid.")
                    -11 -> LogManager.addLog("SSH Error (-11): Gagal konversi string JNI.")
                    else -> LogManager.addLog("SSH Tunnel berhenti dengan kode: $result")
                }
            } catch (t: Throwable) {
                LogManager.addLog("Error SSH Native: ${t.javaClass.simpleName} - ${t.message}")
            } finally {
                isSshRunning = false
                PayloadInjector.stop()
            }
        }

        // Wait until SOCKS5 is ready
        var socksReady = false
        withContext(Dispatchers.IO) {
            for (i in 1..150) { // 15 second timeout
                if (!isSshRunning) break
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress("127.0.0.1", 1080), 100)
                    }
                    socksReady = true
                    break
                } catch (e: Exception) {
                    delay(100)
                }
            }
        }
        
        return socksReady
    }

    fun stopTunnel() {
        if (!isSshRunning) return
        LogManager.addLog("Stopping SSH tunnel...")
        LibSsh2Client.stopTunnel()
        PayloadInjector.stop()
        
        sshJob?.cancel()
        sshJob = null
    }
}
