package com.sivpn.cepat.model

data class MainUiState(
    // Theme
    val themeMode: Int = 0,
    val tcpFastOpenEnabled: Boolean = false,

    // Profiles & Credentials
    val currentProfile: String = "Default",
    val profileList: List<String> = listOf("Default"),
    val sshHost: String = "",
    val sshPort: String = "22",
    val sshUsername: String = "",
    val sshPassword: String = "",
    val sshFullInput: String = "",
    val payload: String = "",
    val proxyHost: String = "",
    val proxyPort: String = "8080",
    val proxyFullInput: String = "",
    val sni: String = "",
    val dns: String = "8.8.8.8",
    val udpgw: String = "127.0.0.1:7300",
    val autoPing: Boolean = true,
    val pingAddress: String = "1.1.1.1",

    // VPN Feature States
    val splitTunnelingEnabled: Boolean = false,
    val appsFilterMode: String = "bypass",
    val bypassApps: Set<String> = emptySet(),
    val killSwitchEnabled: Boolean = false,
    val forcingTls: String = "Auto",
    val speedometerEnabled: Boolean = true,
    val autoReconnectEnabled: Boolean = true,
    val ipAutoRefreshEnabled: Boolean = true,
    val ipAutoRefreshInterval: Int = 10,
    val hotshareEnabled: Boolean = false,
    val hotspotRootEnabled: Boolean = false,
    val hotshareWakeLockEnabled: Boolean = false,
    val vpnWakeLockEnabled: Boolean = false,
    val keepAliveInterval: Int = 30,

    // Log Settings
    val autoCleanLogsEnabled: Boolean = false,
    val autoCleanInterval: Int = 60,
    val maxLogLines: Int = 500,

    // JNI State
    val isNativeSshLoadedState: Boolean = true,
    val isHevLoadedState: Boolean = true,

    // Dynamic Connection & Telemetry State
    val isVpnActive: Boolean = false,
    val connectionState: String = "DISCONNECTED",
    val connectionStartTime: Long = 0L,
    val connectionLimitMinutes: Int = 0,
    val connectionLimitEnabled: Boolean = false,
    val statusCardVisible: Boolean = true,
    val elapsedSeconds: Long = 0L,
    val rxSpeedBytesSec: Long = 0L,
    val txSpeedBytesSec: Long = 0L,
    val isBatteryOptimized: Boolean = false,
    val currentPublicIp: String = "",
    val currentPingMs: Long = -1L,
    val isPinging: Boolean = false,

    // HevSocks Parameters
    val hevMtu: Int = 8500,
    val hevMultiQueue: Boolean = false,
    val hevIpv4: String = "198.18.0.1",
    val hevIpv6: String = "fc00::1",
    val hevDnsPort: Int = 53,
    val hevDnsAddress: String = "94.140.14.14",
    val hevSocks5Port: Int = 1080,
    val hevSocks5Address: String = "127.0.0.1",
    val hevSocks5Udp: String = "udp",
    val hevSocks5UdpAddress: String = "127.0.0.1:7300",
    val hevSocks5Pipeline: Boolean = false,
    val hevSocks5Username: String = "",
    val hevSocks5Password: String = "",
    val hevSocks5Mark: Int = 0,
    val hevTaskStackSize: Int = 86016,
    val hevTcpBufferSize: Int = 65536,
    val hevUdpRecvBufferSize: Int = 524288,
    val hevUdpCopyBufferNums: Int = 10,
    val hevMaxSessionCount: Int = 0,
    val hevConnectTimeout: Int = 10000,
    val hevTcpReadWriteTimeout: Int = 300000,
    val hevUdpReadWriteTimeout: Int = 60000,
    val hevLogFile: String = "stderr",
    val hevLogLevel: String = "warn",
    val isHevSocksExpanded: Boolean = false
)
