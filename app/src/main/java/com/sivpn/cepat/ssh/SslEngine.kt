package com.sivpn.cepat.ssh
import com.sivpn.cepat.vpn.LogManager

import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object SslEngine {
    fun connect(
        socket: Socket,
        remoteHost: String,
        remotePort: Int,
        sni: String,
        tlsVersion: String
    ): SSLSocket {
        LogManager.addLog("Handshake Start (SNI: $sni)...")
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val sslSocket = factory.createSocket(socket, remoteHost, remotePort, true) as SSLSocket
        
        val params = sslSocket.sslParameters
        if (sni.isNotBlank()) {
            params.serverNames = listOf(SNIHostName(sni))
        }
        
        if (tlsVersion != "Auto" && tlsVersion.isNotBlank()) {
            val supported = sslSocket.supportedProtocols
            if (supported.contains(tlsVersion)) {
                sslSocket.enabledProtocols = arrayOf(tlsVersion)
            }
        }
        sslSocket.sslParameters = params
        sslSocket.startHandshake()
        
        val session = sslSocket.session
        if (session != null) {
            LogManager.addLog("TLS Protocol: ${session.protocol}, CipherSuite: ${session.cipherSuite}")
        }
        LogManager.addLog("Handshake Success (SSL/TLS)")
        return sslSocket
    }
}
