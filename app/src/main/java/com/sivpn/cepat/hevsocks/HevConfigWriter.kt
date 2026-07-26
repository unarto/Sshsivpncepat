package com.sivpn.cepat.hevsocks

import android.content.Context
import java.io.File

object HevConfigWriter {
    fun writeConfig(context: Context, config: HevConfig): String {
        val socks5UdpMode = if (config.socks5Udp.equals("udp", ignoreCase = true) || config.socks5Udp.equals("udpgw", ignoreCase = true)) "udp" else "tcp"
        
        val configContent = StringBuilder().apply {
            append("tunnel:\n")
            append("  mtu: ${config.mtu}\n")
            append("  multi-queue: ${config.multiQueue}\n")
            append("  ipv4: ${config.ipv4}\n")
            append("  ipv6: \'${config.ipv6}\'\n")
            append("  icmp: \'reply\'\n\n")
            
            
            
            append("mapdns:\n")
            append("  port: ${config.dnsPort}\n")
            append("  address: ${config.dnsAddress}\n")
            append("  network: 240.0.0.0\n")
            append("  netmask: 240.0.0.0\n")
            append("  cache-size: 10000\n\n")
            
            append("socks5:\n")
            append("  port: ${config.socks5Port}\n")
            append("  address: ${config.socks5Address}\n")
            append("  udp: $socks5UdpMode\n")
            
            if (socks5UdpMode == "udp" && config.socks5UdpAddress.isNotBlank()) {
                append("  udp-address: ${config.socks5UdpAddress}\n")
            }
            if (config.socks5Pipeline) {
                append("  pipeline: true\n")
            }
            if (config.socks5Username.isNotBlank()) {
                append("  username: '${config.socks5Username}'\n")
            }
            if (config.socks5Password.isNotBlank()) {
                append("  password: '${config.socks5Password}'\n")
            }
            if (config.socks5Mark > 0) {
                append("  mark: ${config.socks5Mark}\n")
            }
            append("\n")
            
            append("misc:\n")
            append("  task-stack-size: ${config.taskStackSize}\n")
            append("  tcp-buffer-size: ${config.tcpBufferSize}\n")
            append("  udp-recv-buffer-size: ${config.udpRecvBufferSize}\n")
            append("  udp-copy-buffer-nums: ${config.udpCopyBufferNums}\n")
            
            if (config.maxSessionCount > 0) {
                append("  max-session-count: ${config.maxSessionCount}\n")
            }
            
            append("  connect-timeout: ${config.connectTimeout}\n")
            append("  tcp-read-write-timeout: ${config.tcpReadWriteTimeout}\n")
            append("  udp-read-write-timeout: ${config.udpReadWriteTimeout}\n")
            append("  log-file: ${config.logFile}\n")
            append("  log-level: ${config.logLevel}\n")
        }.toString()
        
        val filesDir = context.filesDir
        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }
        val configFile = File(filesDir, "hev_config.yml")
        val tempConfigFile = File(filesDir, "hev_config.yml.tmp")
        tempConfigFile.writeText(configContent)
        if (!tempConfigFile.renameTo(configFile)) {
            configFile.writeText(configContent)
            tempConfigFile.delete()
        }
        
        return configFile.absolutePath
    }
}
