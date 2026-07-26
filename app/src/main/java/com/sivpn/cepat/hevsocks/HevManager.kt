package com.sivpn.cepat.hevsocks

import android.content.Context
import com.sivpn.cepat.TProxyService
import com.sivpn.cepat.config.DefaultValues
import com.sivpn.cepat.config.SettingsManager
import com.sivpn.cepat.vpn.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HevManager {
    fun isRunning(): Boolean = TProxyService.isServiceRunning()
    val isLibraryLoaded: Boolean get() = TProxyService.isLibraryLoaded

    suspend fun start(context: Context, fd: Int) = withContext(Dispatchers.IO) {
        val settings = SettingsManager(context)
        
        val config = HevConfig(
            mtu = settings.getHevMtu().coerceAtLeast(576),
            multiQueue = settings.getHevMultiQueue(),
            ipv4 = settings.getHevIpv4().takeIf { it.isNotBlank() } ?: DefaultValues.DEFAULT_TUN_IPV4,
            ipv6 = settings.getHevIpv6().takeIf { it.isNotBlank() } ?: DefaultValues.DEFAULT_TUN_IPV6,
            
            dnsPort = settings.getHevDnsPort().coerceIn(1, 65535),
            dnsAddress = settings.getHevDnsAddress().takeIf { it.isNotBlank() } ?: DefaultValues.DEFAULT_TUN_DNS,
            
            socks5Port = settings.getHevSocks5Port().coerceIn(1, 65535),
            socks5Address = settings.getHevSocks5Address().takeIf { it.isNotBlank() } ?: DefaultValues.DEFAULT_SOCKS_HOST,
            socks5Udp = settings.getHevSocks5Udp(),
            socks5UdpAddress = settings.getHevSocks5UdpAddress(),
            socks5Pipeline = settings.getHevSocks5Pipeline(),
            socks5Username = settings.getHevSocks5Username(),
            socks5Password = settings.getHevSocks5Password(),
            socks5Mark = settings.getHevSocks5Mark(),
            
            taskStackSize = settings.getHevTaskStackSize().coerceAtLeast(8192),
            tcpBufferSize = settings.getHevTcpBufferSize().coerceAtLeast(4096),
            udpRecvBufferSize = settings.getHevUdpRecvBufferSize().coerceAtLeast(4096),
            udpCopyBufferNums = settings.getHevUdpCopyBufferNums().coerceAtLeast(1),
            maxSessionCount = settings.getHevMaxSessionCount().coerceAtLeast(0),
            connectTimeout = settings.getHevConnectTimeout().coerceAtLeast(1000),
            tcpReadWriteTimeout = settings.getHevTcpReadWriteTimeout().coerceAtLeast(1000),
            udpReadWriteTimeout = settings.getHevUdpReadWriteTimeout().coerceAtLeast(1000),
            
            logFile = settings.getHevLogFile().takeIf { it.isNotBlank() } ?: "stderr",
            logLevel = settings.getHevLogLevel().takeIf { it.isNotBlank() } ?: DefaultValues.DEFAULT_LOG_LEVEL
        )
        
        val configPath = HevConfigWriter.writeConfig(context, config)
        
        LogManager.addLog("Starting HevSocks5Tunnel natively via JNI...")
        try {
            TProxyService.startServiceSafe(configPath, fd)
        } catch (e: UnsatisfiedLinkError) {
            LogManager.addLog("Native library hev-socks5-tunnel not found! Make sure .so files are in jniLibs.")
            throw e
        } catch (e: Exception) {
            LogManager.addLog("Execution Hev tunnel failed: ${e.message}")
            throw e
        }
    }
    
    fun stop() {
        LogManager.addLog("Stopping HevSocks5Tunnel...")
        TProxyService.stopServiceSafe()
    }
}
