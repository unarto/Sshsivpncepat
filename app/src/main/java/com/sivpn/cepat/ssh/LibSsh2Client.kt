package com.sivpn.cepat.ssh
import com.sivpn.cepat.vpn.LogManager

import com.sivpn.cepat.vpn.NativeSshTunnel

object LibSsh2Client {
    fun loadFromPath(path: String): Boolean {
        return NativeSshTunnel.loadFromPath(path)
    }

    suspend fun startTunnel(
        host: String,
        port: Int,
        username: String,
        password: String,
        socksPort: Int
    ): Int {
        return NativeSshTunnel.startSshTunnelSafe(host, port, username, password, socksPort)
    }

    fun stopTunnel() {
        NativeSshTunnel.stopSshTunnelSafe()
    }
    
    fun isRunning(): Boolean {
        return NativeSshTunnel.isServiceRunning()
    }
    
    val isLibraryLoaded: Boolean
        get() = NativeSshTunnel.isLibraryLoaded
}
