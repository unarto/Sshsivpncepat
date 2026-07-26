package com.sivpn.cepat.config

object DefaultValues {
    // Profile
    const val DEFAULT_PROFILE = "Default"

    // SSH
    const val DEFAULT_SSH_HOST = ""
    const val DEFAULT_SSH_PORT = 22
    const val DEFAULT_SSH_USERNAME = ""
    const val DEFAULT_SSH_PASSWORD = ""
    const val DEFAULT_PAYLOAD = ""

    // Proxy
    const val DEFAULT_PROXY_HOST = ""
    const val DEFAULT_PROXY_PORT = 8080
    const val DEFAULT_SNI = ""
    const val DEFAULT_DNS = "94.140.14.14:94.140.15.15"
    const val DEFAULT_UDPGW = "127.0.0.1:7300"

    // HEV / SOCKS / TUN
    const val DEFAULT_SOCKS_HOST = "127.0.0.1"
    const val DEFAULT_SOCKS_PORT = 1080
    const val DEFAULT_UDP_MODE = "udp"
    const val DEFAULT_UDP_ADDRESS = "127.0.0.1:7300"
    const val DEFAULT_SOCKS_PIPELINE = false
    const val DEFAULT_SOCKS_USERNAME = ""
    const val DEFAULT_SOCKS_PASSWORD = ""
    const val DEFAULT_SOCKS_MARK = 0
    const val DEFAULT_TUN_IPV4 = "198.18.0.1"
    const val DEFAULT_TUN_IPV6 = "fc00::1"
    const val DEFAULT_TUN_MTU = 8500
    const val DEFAULT_TUN_DNS = "94.140.14.14"
    const val DEFAULT_LOG_LEVEL = "warn"
    const val DEFAULT_READ_WRITE_TIMEOUT = 300000
    const val DEFAULT_CONNECT_TIMEOUT = 60000
    const val DEFAULT_BUFFER_SIZE = 65536
    const val DEFAULT_MARK = 438
}
