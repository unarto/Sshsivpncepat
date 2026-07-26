package com.sivpn.cepat.config

import android.content.Context
import com.tencent.mmkv.MMKV
import com.sivpn.cepat.model.SshConfig
import com.sivpn.cepat.vpn.HevLogLevel
import com.sivpn.cepat.vpn.HevUdpMode

class SettingsManager(private val context: Context) {
    private val PREFS_NAME = "sivpn_settings"
    
    internal object Keys {
        const val MIGRATED = "migrated"
        const val THEME_MODE = "theme_mode"
        const val SSH_HOST = "ssh_host"
        const val SSH_PORT = "ssh_port"
        const val SSH_USERNAME = "ssh_username"
        const val SSH_PASSWORD = "ssh_password"
        const val PAYLOAD = "payload"
        const val PROXY_HOST = "proxy_host"
        const val PROXY_PORT = "proxy_port"
        const val AUTO_RECONNECT_ENABLED = "auto_reconnect_enabled"
        const val SNI = "sni"
        const val DNS = "dns"
        const val UDPGW = "udpgw"
        const val AUTO_PING = "auto_ping"
        const val FORCING_TLS = "forcing_tls"
        const val CURRENT_PROFILE = "current_profile"
        const val PROFILES_LIST = "profiles_list"
        const val CONNECTION_LIMIT_MINUTES = "connection_limit_minutes"
        const val CONNECTION_LIMIT_ENABLED = "connection_limit_enabled"
        const val STATUS_CARD_VISIBLE = "status_card_visible"
        const val PING_ADDRESS = "ping_address"
        const val BYPASS_APPS_LIST = "bypass_apps_list"
        const val SPLIT_TUNNELING_ENABLED = "split_tunneling_enabled"
        const val APPS_FILTER_MODE = "apps_filter_mode"
        const val KILL_SWITCH_ENABLED = "kill_switch_enabled"
        const val SPEEDOMETER_ENABLED = "speedometer_enabled"
        const val HOTSHARE_SOCKS_PORT = "hotshare_socks_port"
        const val HOTSHARE_HTTP_PORT = "hotshare_http_port"
        const val IP_AUTO_REFRESH_ENABLED = "ip_auto_refresh_enabled"
        const val IP_AUTO_REFRESH_INTERVAL = "ip_auto_refresh_interval"
        const val HOTSHARE_WAKELOCK_ENABLED = "hotshare_wakelock_enabled"
        const val VPN_WAKELOCK_ENABLED = "vpn_wakelock_enabled"
        const val KEEP_ALIVE_INTERVAL = "keep_alive_interval"
        const val AUTO_CLEAN_LOGS_ENABLED = "auto_clean_logs_enabled"
        const val AUTO_CLEAN_LOGS_INTERVAL = "auto_clean_logs_interval"
        const val MAX_LOG_LINES = "max_log_lines"
        const val TCP_FASTOPEN_ENABLED = "tcp_fastopen_enabled"
        
        const val HEV_MTU = "hev_mtu"
        const val HEV_MULTI_QUEUE = "hev_multi_queue"
        const val HEV_IPV4 = "hev_ipv4"
        const val HEV_IPV6 = "hev_ipv6"
        const val HEV_DNS_PORT = "hev_dns_port"
        const val HEV_DNS_ADDRESS = "hev_dns_address"
        const val HEV_SOCKS5_PORT = "hev_socks5_port"
        const val HEV_SOCKS5_ADDRESS = "hev_socks5_address"
        const val HEV_SOCKS5_UDP = "hev_socks5_udp"
        const val HEV_SOCKS5_UDP_ADDRESS = "hev_socks5_udp_address"
        const val HEV_SOCKS5_PIPELINE = "hev_socks5_pipeline"
        const val HEV_SOCKS5_USERNAME = "hev_socks5_username"
        const val HEV_SOCKS5_PASSWORD = "hev_socks5_password"
        const val HEV_SOCKS5_MARK = "hev_socks5_mark"
        const val HEV_TASK_STACK_SIZE = "hev_task_stack_size"
        const val HEV_TCP_BUFFER_SIZE = "hev_tcp_buffer_size"
        const val HEV_UDP_RECV_BUFFER_SIZE = "hev_udp_recv_buffer_size"
        const val HEV_UDP_COPY_BUFFER_NUMS = "hev_udp_copy_buffer_nums"
        const val HEV_MAX_SESSION_COUNT = "hev_max_session_count"
        const val HEV_CONNECT_TIMEOUT = "hev_connect_timeout"
        const val HEV_TCP_READ_WRITE_TIMEOUT = "hev_tcp_read_write_timeout"
        const val HEV_UDP_READ_WRITE_TIMEOUT = "hev_udp_read_write_timeout"
        const val HEV_LOG_FILE = "hev_log_file"
        const val HEV_LOG_LEVEL = "hev_log_level"
    }

    private val mmkv: MMKV by lazy {
        MMKV.initialize(context.applicationContext)
        val m = MMKV.mmkvWithID(PREFS_NAME, MMKV.SINGLE_PROCESS_MODE)
        if (!m.decodeBool(Keys.MIGRATED, false)) {
            listOf(PREFS_NAME, context.packageName + "_preferences").forEach { p ->
                try {
                    val prefs = context.getSharedPreferences(p, Context.MODE_PRIVATE)
                    if (prefs.all.isNotEmpty()) {
                        m.importFromSharedPreferences(prefs)
                        prefs.edit().clear().apply()
                    }
                } catch (_: Exception) {}
            }
            m.encode(Keys.MIGRATED, true)
        }
        m
    }

    private val globalKeys = setOf(
        Keys.MIGRATED, Keys.THEME_MODE, Keys.CURRENT_PROFILE, Keys.PROFILES_LIST,
        Keys.HOTSHARE_SOCKS_PORT, Keys.HOTSHARE_HTTP_PORT, Keys.IP_AUTO_REFRESH_ENABLED,
        Keys.IP_AUTO_REFRESH_INTERVAL, Keys.HOTSHARE_WAKELOCK_ENABLED, Keys.VPN_WAKELOCK_ENABLED,
        Keys.KEEP_ALIVE_INTERVAL, Keys.AUTO_CLEAN_LOGS_ENABLED, Keys.AUTO_CLEAN_LOGS_INTERVAL, Keys.MAX_LOG_LINES,
        Keys.TCP_FASTOPEN_ENABLED
    )

    private fun getProfiledKey(key: String): String {
        if (key in globalKeys) return key
        val currentProfile = mmkv.decodeString(Keys.CURRENT_PROFILE, DefaultValues.DEFAULT_PROFILE) ?: DefaultValues.DEFAULT_PROFILE
        if (currentProfile == DefaultValues.DEFAULT_PROFILE) return key
        return "${currentProfile}_$key"
    }

    private fun getStr(k: String, d: String): String = mmkv.decodeString(getProfiledKey(k), d) ?: d
    private fun setStr(k: String, v: String) = mmkv.encode(getProfiledKey(k), v)
    
    private fun getInt(k: String, d: Int): Int = try { mmkv.decodeInt(getProfiledKey(k), d) } catch(_: Exception) { d }
    private fun setInt(k: String, v: Int) = mmkv.encode(getProfiledKey(k), v)
    
    private fun getBool(k: String, d: Boolean): Boolean = mmkv.decodeBool(getProfiledKey(k), d)
    private fun setBool(k: String, v: Boolean) = mmkv.encode(getProfiledKey(k), v)
    
    private fun getSet(k: String, d: Set<String>): Set<String> = mmkv.decodeStringSet(getProfiledKey(k), d) ?: d
    private fun setSet(k: String, v: Set<String>) = mmkv.encode(getProfiledKey(k), v)

    fun getThemeMode() = getInt(Keys.THEME_MODE, 0)
    fun setThemeMode(v: Int) = setInt(Keys.THEME_MODE, v)

    fun getTcpFastOpenEnabled() = getBool(Keys.TCP_FASTOPEN_ENABLED, false)
    fun setTcpFastOpenEnabled(v: Boolean) = setBool(Keys.TCP_FASTOPEN_ENABLED, v)
    
    fun getSshHost() = getStr(Keys.SSH_HOST, "yu.xhmt.web.id")
    fun setSshHost(v: String) = setStr(Keys.SSH_HOST, v.trim())
    
    fun getSshPort() = getInt(Keys.SSH_PORT, 80).coerceIn(1, 65535)
    fun setSshPort(v: Int) = setInt(Keys.SSH_PORT, v.coerceIn(1, 65535))
    
    fun getSshUsername() = getStr(Keys.SSH_USERNAME, "80@xxxxxxxxxx")
    fun setSshUsername(v: String) = setStr(Keys.SSH_USERNAME, v)
    
    fun getSshPassword() = getStr(Keys.SSH_PASSWORD, "x")
    fun setSshPassword(v: String) = setStr(Keys.SSH_PASSWORD, v)
    
    fun getPayload() = getStr(Keys.PAYLOAD, "GET /cdn-cgi/trace HTTP/1.1[crlf]Host: open.spotify.com[crlf][crlf]")
    fun setPayload(v: String) = setStr(Keys.PAYLOAD, v)
    
    fun getProxyHost() = getStr(Keys.PROXY_HOST, "investors.spotify.com")
    fun setProxyHost(v: String) = setStr(Keys.PROXY_HOST, v.trim())
    
    fun getProxyPort() = getInt(Keys.PROXY_PORT, 80).coerceIn(1, 65535)
    fun setProxyPort(v: Int) = setInt(Keys.PROXY_PORT, v.coerceIn(1, 65535))
    
    fun getAutoReconnectEnabled() = getBool(Keys.AUTO_RECONNECT_ENABLED, true)
    fun setAutoReconnectEnabled(v: Boolean) = setBool(Keys.AUTO_RECONNECT_ENABLED, v)
    
    fun getSni() = getStr(Keys.SNI, "investors.spotify.com")
    fun setSni(v: String) = setStr(Keys.SNI, v.trim())
    
    fun getDns(): String {
        val saved = mmkv.decodeString(Keys.DNS, null)
        if (saved == null || saved == "8.8.8.8:8.8.4.4") {
            val defaultDns = DefaultValues.DEFAULT_DNS
            setStr(Keys.DNS, defaultDns)
            return defaultDns
        }
        return saved
    }
    fun setDns(v: String) = setStr(Keys.DNS, v.trim())
    
    fun getUdpgw() = getStr(Keys.UDPGW, DefaultValues.DEFAULT_UDPGW)
    fun setUdpgw(v: String) = setStr(Keys.UDPGW, v.trim())
    
    fun getAutoPing() = getBool(Keys.AUTO_PING, false)
    fun setAutoPing(v: Boolean) = setBool(Keys.AUTO_PING, v)
    
    fun getForcingTls() = getStr(Keys.FORCING_TLS, "Auto")
    fun setForcingTls(v: String) = setStr(Keys.FORCING_TLS, v)
    
    fun getCurrentProfile() = getStr(Keys.CURRENT_PROFILE, DefaultValues.DEFAULT_PROFILE)
    fun setCurrentProfile(v: String) = setStr(Keys.CURRENT_PROFILE, v)
    
    fun getProfiles(): Set<String> {
        val defaultProfiles = setOf("Default")
        val saved = getSet(Keys.PROFILES_LIST, defaultProfiles)
        return LinkedHashSet(saved)
    }
    
    fun addProfile(v: String) {
        val cur = LinkedHashSet(getProfiles())
        cur.add(v)
        setSet(Keys.PROFILES_LIST, cur)
    }
    
    fun removeProfile(v: String) {
        val cur = LinkedHashSet(getProfiles())
        if (cur.size > 1) {
            cur.remove(v)
            setSet(Keys.PROFILES_LIST, cur)
            if (getCurrentProfile() == v) {
                setCurrentProfile(cur.first())
            }
            mmkv.allKeys()?.filter { it.startsWith("${v}_") }?.forEach { mmkv.removeValueForKey(it) }
        }
    }
    
    fun getConnectionLimitMinutes() = getInt(Keys.CONNECTION_LIMIT_MINUTES, 1).coerceAtLeast(1)
    fun setConnectionLimitMinutes(v: Int) = setInt(Keys.CONNECTION_LIMIT_MINUTES, v.coerceAtLeast(1))
    
    fun getConnectionLimitEnabled() = getBool(Keys.CONNECTION_LIMIT_ENABLED, false)
    fun setConnectionLimitEnabled(v: Boolean) = setBool(Keys.CONNECTION_LIMIT_ENABLED, v)
    
    fun getStatusCardVisible() = getBool(Keys.STATUS_CARD_VISIBLE, true)
    fun setStatusCardVisible(v: Boolean) = setBool(Keys.STATUS_CARD_VISIBLE, v)
    
    fun getPingAddress() = getStr(Keys.PING_ADDRESS, "")
    fun setPingAddress(v: String) = setStr(Keys.PING_ADDRESS, v.trim())
    
    fun getBypassApps(): Set<String> = LinkedHashSet(getSet(Keys.BYPASS_APPS_LIST, emptySet()))
    fun setBypassApps(v: Set<String>) = setSet(Keys.BYPASS_APPS_LIST, LinkedHashSet(v))
    
    fun getSplitTunnelingEnabled() = getBool(Keys.SPLIT_TUNNELING_ENABLED, false)
    fun setSplitTunnelingEnabled(v: Boolean) = setBool(Keys.SPLIT_TUNNELING_ENABLED, v)
    
    fun getAppsFilterMode() = getStr(Keys.APPS_FILTER_MODE, "bypass")
    fun setAppsFilterMode(v: String) = setStr(Keys.APPS_FILTER_MODE, v)
    
    fun getKillSwitchEnabled() = getBool(Keys.KILL_SWITCH_ENABLED, false)
    fun setKillSwitchEnabled(v: Boolean) = setBool(Keys.KILL_SWITCH_ENABLED, v)
    
    fun getSpeedometerEnabled() = getBool(Keys.SPEEDOMETER_ENABLED, true)
    fun setSpeedometerEnabled(v: Boolean) = setBool(Keys.SPEEDOMETER_ENABLED, v)
    
    fun getHotshareSocksPort() = getInt(Keys.HOTSHARE_SOCKS_PORT, 1080).coerceIn(1, 65535)
    fun setHotshareSocksPort(v: Int) = setInt(Keys.HOTSHARE_SOCKS_PORT, v.coerceIn(1, 65535))
    
    fun getHotshareHttpPort() = getInt(Keys.HOTSHARE_HTTP_PORT, 8080).coerceIn(1, 65535)
    fun setHotshareHttpPort(v: Int) = setInt(Keys.HOTSHARE_HTTP_PORT, v.coerceIn(1, 65535))
    
    fun getIpAutoRefreshEnabled() = getBool(Keys.IP_AUTO_REFRESH_ENABLED, true)
    fun setIpAutoRefreshEnabled(v: Boolean) = setBool(Keys.IP_AUTO_REFRESH_ENABLED, v)
    
    fun getIpAutoRefreshInterval() = getInt(Keys.IP_AUTO_REFRESH_INTERVAL, 15).coerceAtLeast(1)
    fun setIpAutoRefreshInterval(v: Int) = setInt(Keys.IP_AUTO_REFRESH_INTERVAL, v.coerceAtLeast(1))
    
    fun getHotshareWakeLockEnabled() = getBool(Keys.HOTSHARE_WAKELOCK_ENABLED, true)
    fun setHotshareWakeLockEnabled(v: Boolean) = setBool(Keys.HOTSHARE_WAKELOCK_ENABLED, v)
    
    fun getVpnWakeLockEnabled() = getBool(Keys.VPN_WAKELOCK_ENABLED, true)
    fun setVpnWakeLockEnabled(v: Boolean) = setBool(Keys.VPN_WAKELOCK_ENABLED, v)
    
    fun getKeepAliveInterval() = getInt(Keys.KEEP_ALIVE_INTERVAL, 30).coerceAtLeast(1)
    fun setKeepAliveInterval(v: Int) = setInt(Keys.KEEP_ALIVE_INTERVAL, v.coerceAtLeast(1))
    
    fun getAutoCleanLogsEnabled() = getBool(Keys.AUTO_CLEAN_LOGS_ENABLED, false)
    fun setAutoCleanLogsEnabled(v: Boolean) = setBool(Keys.AUTO_CLEAN_LOGS_ENABLED, v)
    
    fun getAutoCleanInterval() = getInt(Keys.AUTO_CLEAN_LOGS_INTERVAL, 10).coerceAtLeast(1)
    fun setAutoCleanInterval(v: Int) = setInt(Keys.AUTO_CLEAN_LOGS_INTERVAL, v.coerceAtLeast(1))
    
    fun getMaxLogLines() = getInt(Keys.MAX_LOG_LINES, 1000).coerceAtLeast(1)
    fun setMaxLogLines(v: Int) = setInt(Keys.MAX_LOG_LINES, v.coerceAtLeast(1))
    
    fun getHevMtu() = getInt(Keys.HEV_MTU, DefaultValues.DEFAULT_TUN_MTU).coerceAtLeast(576)
    fun setHevMtu(v: Int) = setInt(Keys.HEV_MTU, v.coerceAtLeast(576))
    
    fun getHevMultiQueue() = getBool(Keys.HEV_MULTI_QUEUE, false)
    fun setHevMultiQueue(v: Boolean) = setBool(Keys.HEV_MULTI_QUEUE, v)
    
    fun getHevIpv4() = getStr(Keys.HEV_IPV4, DefaultValues.DEFAULT_TUN_IPV4)
    fun setHevIpv4(v: String) {
        val trimmed = v.trim()
        if (android.util.Patterns.IP_ADDRESS.matcher(trimmed).matches()) {
            setStr(Keys.HEV_IPV4, trimmed)
        }
    }
    
    fun getHevIpv6() = getStr(Keys.HEV_IPV6, DefaultValues.DEFAULT_TUN_IPV6)
    fun setHevIpv6(v: String) {
        val trimmed = v.trim()
        if (android.util.Patterns.IP_ADDRESS.matcher(trimmed).matches()) {
            setStr(Keys.HEV_IPV6, trimmed)
        }
    }
    
    fun getHevDnsPort() = getInt(Keys.HEV_DNS_PORT, 53).coerceIn(1, 65535)
    fun setHevDnsPort(v: Int) = setInt(Keys.HEV_DNS_PORT, v.coerceIn(1, 65535))
    
    fun getHevDnsAddress() = getStr(Keys.HEV_DNS_ADDRESS, DefaultValues.DEFAULT_TUN_DNS)
    fun setHevDnsAddress(v: String) = setStr(Keys.HEV_DNS_ADDRESS, v.trim())
    
    fun getHevSocks5Port() = getInt(Keys.HEV_SOCKS5_PORT, DefaultValues.DEFAULT_SOCKS_PORT).coerceIn(1, 65535)
    fun setHevSocks5Port(v: Int) = setInt(Keys.HEV_SOCKS5_PORT, v.coerceIn(1, 65535))
    
    fun getHevSocks5Address() = getStr(Keys.HEV_SOCKS5_ADDRESS, DefaultValues.DEFAULT_SOCKS_HOST)
    fun setHevSocks5Address(v: String) = setStr(Keys.HEV_SOCKS5_ADDRESS, v.trim())
    
    fun getHevSocks5Udp() = getStr(Keys.HEV_SOCKS5_UDP, DefaultValues.DEFAULT_UDP_MODE)
    fun setHevSocks5Udp(v: String) = setStr(Keys.HEV_SOCKS5_UDP, HevUdpMode.fromString(v).value)
    
    fun getHevSocks5UdpAddress() = getStr(Keys.HEV_SOCKS5_UDP_ADDRESS, DefaultValues.DEFAULT_UDP_ADDRESS)
    fun setHevSocks5UdpAddress(v: String) = setStr(Keys.HEV_SOCKS5_UDP_ADDRESS, v.trim())
    
    fun getHevSocks5Pipeline() = getBool(Keys.HEV_SOCKS5_PIPELINE, DefaultValues.DEFAULT_SOCKS_PIPELINE)
    fun setHevSocks5Pipeline(v: Boolean) = setBool(Keys.HEV_SOCKS5_PIPELINE, v)
    
    fun getHevSocks5Username() = getStr(Keys.HEV_SOCKS5_USERNAME, DefaultValues.DEFAULT_SOCKS_USERNAME)
    fun setHevSocks5Username(v: String) = setStr(Keys.HEV_SOCKS5_USERNAME, v.trim())
    
    fun getHevSocks5Password() = getStr(Keys.HEV_SOCKS5_PASSWORD, DefaultValues.DEFAULT_SOCKS_PASSWORD)
    fun setHevSocks5Password(v: String) = setStr(Keys.HEV_SOCKS5_PASSWORD, v.trim())
    
    fun getHevSocks5Mark() = getInt(Keys.HEV_SOCKS5_MARK, DefaultValues.DEFAULT_SOCKS_MARK)
    fun setHevSocks5Mark(v: Int) = setInt(Keys.HEV_SOCKS5_MARK, v)
    
    fun getHevTaskStackSize() = getInt(Keys.HEV_TASK_STACK_SIZE, 86016).coerceAtLeast(8192)
    fun setHevTaskStackSize(v: Int) = setInt(Keys.HEV_TASK_STACK_SIZE, v.coerceAtLeast(8192))
    
    fun getHevTcpBufferSize() = getInt(Keys.HEV_TCP_BUFFER_SIZE, DefaultValues.DEFAULT_BUFFER_SIZE).coerceAtLeast(4096)
    fun setHevTcpBufferSize(v: Int) = setInt(Keys.HEV_TCP_BUFFER_SIZE, v.coerceAtLeast(4096))
    
    fun getHevUdpRecvBufferSize() = getInt(Keys.HEV_UDP_RECV_BUFFER_SIZE, 524288).coerceAtLeast(4096)
    fun setHevUdpRecvBufferSize(v: Int) = setInt(Keys.HEV_UDP_RECV_BUFFER_SIZE, v.coerceAtLeast(4096))
    
    fun getHevUdpCopyBufferNums() = getInt(Keys.HEV_UDP_COPY_BUFFER_NUMS, 10).coerceAtLeast(1)
    fun setHevUdpCopyBufferNums(v: Int) = setInt(Keys.HEV_UDP_COPY_BUFFER_NUMS, v.coerceAtLeast(1))
    
    fun getHevMaxSessionCount() = getInt(Keys.HEV_MAX_SESSION_COUNT, 0).coerceAtLeast(0)
    fun setHevMaxSessionCount(v: Int) = setInt(Keys.HEV_MAX_SESSION_COUNT, v.coerceAtLeast(0))
    
    fun getHevConnectTimeout() = getInt(Keys.HEV_CONNECT_TIMEOUT, 10000).coerceAtLeast(1000)
    fun setHevConnectTimeout(v: Int) = setInt(Keys.HEV_CONNECT_TIMEOUT, v.coerceAtLeast(1000))
    
    fun getHevTcpReadWriteTimeout() = getInt(Keys.HEV_TCP_READ_WRITE_TIMEOUT, DefaultValues.DEFAULT_READ_WRITE_TIMEOUT).coerceAtLeast(1000)
    fun setHevTcpReadWriteTimeout(v: Int) = setInt(Keys.HEV_TCP_READ_WRITE_TIMEOUT, v.coerceAtLeast(1000))
    
    fun getHevUdpReadWriteTimeout() = getInt(Keys.HEV_UDP_READ_WRITE_TIMEOUT, DefaultValues.DEFAULT_CONNECT_TIMEOUT).coerceAtLeast(1000)
    fun setHevUdpReadWriteTimeout(v: Int) = setInt(Keys.HEV_UDP_READ_WRITE_TIMEOUT, v.coerceAtLeast(1000))
    
    fun getHevLogFile() = getStr(Keys.HEV_LOG_FILE, "stderr")
    fun setHevLogFile(v: String) = setStr(Keys.HEV_LOG_FILE, v)
    
    fun getHevLogLevel() = getStr(Keys.HEV_LOG_LEVEL, DefaultValues.DEFAULT_LOG_LEVEL)
    fun setHevLogLevel(v: String) = setStr(Keys.HEV_LOG_LEVEL, HevLogLevel.fromString(v).value)

    fun resetHevDefaults() {
        setHevMtu(DefaultValues.DEFAULT_TUN_MTU)
        setHevMultiQueue(false)
        setHevIpv4(DefaultValues.DEFAULT_TUN_IPV4)
        setHevIpv6(DefaultValues.DEFAULT_TUN_IPV6)
        setHevDnsPort(53)
        setHevDnsAddress(DefaultValues.DEFAULT_TUN_DNS)
        setHevSocks5Port(DefaultValues.DEFAULT_SOCKS_PORT)
        setHevSocks5Address(DefaultValues.DEFAULT_SOCKS_HOST)
        setHevSocks5Udp(DefaultValues.DEFAULT_UDP_MODE)
        setHevSocks5UdpAddress(DefaultValues.DEFAULT_UDP_ADDRESS)
        setHevSocks5Pipeline(DefaultValues.DEFAULT_SOCKS_PIPELINE)
        setHevSocks5Username(DefaultValues.DEFAULT_SOCKS_USERNAME)
        setHevSocks5Password(DefaultValues.DEFAULT_SOCKS_PASSWORD)
        setHevSocks5Mark(DefaultValues.DEFAULT_SOCKS_MARK)
        setHevTaskStackSize(86016)
        setHevTcpBufferSize(DefaultValues.DEFAULT_BUFFER_SIZE)
        setHevUdpRecvBufferSize(524288)
        setHevUdpCopyBufferNums(10)
        setHevMaxSessionCount(0)
        setHevConnectTimeout(10000)
        setHevTcpReadWriteTimeout(DefaultValues.DEFAULT_READ_WRITE_TIMEOUT)
        setHevUdpReadWriteTimeout(DefaultValues.DEFAULT_CONNECT_TIMEOUT)
        setHevLogFile("stderr")
        setHevLogLevel(DefaultValues.DEFAULT_LOG_LEVEL)
    }

    fun resetProxyDefaults() {
        setProxyHost(DefaultValues.DEFAULT_PROXY_HOST)
        setProxyPort(DefaultValues.DEFAULT_PROXY_PORT)
        setSni(DefaultValues.DEFAULT_SNI)
        setDns(DefaultValues.DEFAULT_DNS)
        setUdpgw(DefaultValues.DEFAULT_UDPGW)
    }

    fun resetSshDefaults() {
        setSshHost(DefaultValues.DEFAULT_SSH_HOST)
        setSshPort(DefaultValues.DEFAULT_SSH_PORT)
        setSshUsername(DefaultValues.DEFAULT_SSH_USERNAME)
        setSshPassword(DefaultValues.DEFAULT_SSH_PASSWORD)
        setPayload(DefaultValues.DEFAULT_PAYLOAD)
    }

    fun resetConnectionDefaults() {
        setAutoReconnectEnabled(true)
        setAutoPing(false)
        setForcingTls("Auto")
        setConnectionLimitMinutes(1)
        setConnectionLimitEnabled(false)
    }
    
    fun getSshConfig(): SshConfig {
        return SshConfig(
            host = getSshHost(),
            port = getSshPort(),
            username = getSshUsername(),
            password = getSshPassword(),
            payload = getPayload()
        )
    }

    fun saveSshConfig(config: SshConfig) {
        setSshHost(config.host)
        setSshPort(config.port)
        setSshUsername(config.username)
        setSshPassword(config.password)
        setPayload(config.payload)
    }
}
