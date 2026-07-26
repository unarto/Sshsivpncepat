package com.sivpn.cepat.hevsocks

data class HevConfig(
    val mtu: Int,
    val multiQueue: Boolean,
    val ipv4: String,
    val ipv6: String,
    
    val dnsPort: Int,
    val dnsAddress: String,
    
    val socks5Port: Int,
    val socks5Address: String,
    val socks5Udp: String,
    val socks5UdpAddress: String,
    val socks5Pipeline: Boolean,
    val socks5Username: String,
    val socks5Password: String,
    val socks5Mark: Int,
    
    val taskStackSize: Int,
    val tcpBufferSize: Int,
    val udpRecvBufferSize: Int,
    val udpCopyBufferNums: Int,
    val maxSessionCount: Int,
    val connectTimeout: Int,
    val tcpReadWriteTimeout: Int,
    val udpReadWriteTimeout: Int,
    
    val logFile: String,
    val logLevel: String
)
