package com.sivpn.cepat.config

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.sivpn.cepat.vpn.LogManager
import org.json.JSONArray
import org.json.JSONObject

class ProfileManager(private val context: Context, private val settingsManager: SettingsManager) {

    fun getProfiles(): Set<String> = settingsManager.getProfiles()
    fun getCurrentProfile(): String = settingsManager.getCurrentProfile()
    fun setCurrentProfile(profile: String) = settingsManager.setCurrentProfile(profile)
    fun addProfile(profile: String) = settingsManager.addProfile(profile)
    fun removeProfile(profile: String) = settingsManager.removeProfile(profile)

    fun exportConfigAsJson(): String {
        val json = JSONObject()
        json.put("ssh_host", settingsManager.getSshHost())
        json.put("ssh_port", settingsManager.getSshPort())
        json.put("ssh_username", settingsManager.getSshUsername())
        json.put("ssh_password", settingsManager.getSshPassword())
        json.put("payload", settingsManager.getPayload())
        json.put("proxy_host", settingsManager.getProxyHost())
        json.put("proxy_port", settingsManager.getProxyPort())
        json.put("sni", settingsManager.getSni())
        json.put("dns", settingsManager.getDns())
        json.put("udpgw", settingsManager.getUdpgw())
        json.put("auto_ping", settingsManager.getAutoPing())
        json.put("forcing_tls", settingsManager.getForcingTls())
        json.put("connection_limit_minutes", settingsManager.getConnectionLimitMinutes())
        json.put("connection_limit_enabled", settingsManager.getConnectionLimitEnabled())
        json.put("ping_address", settingsManager.getPingAddress())
        json.put("split_tunneling_enabled", settingsManager.getSplitTunnelingEnabled())
        json.put("apps_filter_mode", settingsManager.getAppsFilterMode())
        json.put("kill_switch_enabled", settingsManager.getKillSwitchEnabled())
        json.put("speedometer_enabled", settingsManager.getSpeedometerEnabled())
        json.put("hotshare_socks_port", settingsManager.getHotshareSocksPort())
        json.put("hotshare_http_port", settingsManager.getHotshareHttpPort())
        json.put("ip_auto_refresh_enabled", settingsManager.getIpAutoRefreshEnabled())
        json.put("ip_auto_refresh_interval", settingsManager.getIpAutoRefreshInterval())
        json.put("hotshare_wakelock_enabled", settingsManager.getHotshareWakeLockEnabled())
        json.put("vpn_wakelock_enabled", settingsManager.getVpnWakeLockEnabled())
        json.put("keep_alive_interval", settingsManager.getKeepAliveInterval())
        json.put("auto_clean_logs_enabled", settingsManager.getAutoCleanLogsEnabled())
        json.put("auto_clean_logs_interval", settingsManager.getAutoCleanInterval())
        json.put("max_log_lines", settingsManager.getMaxLogLines())

        json.put("hev_mtu", settingsManager.getHevMtu())
        json.put("hev_multi_queue", settingsManager.getHevMultiQueue())
        json.put("hev_ipv4", settingsManager.getHevIpv4())
        json.put("hev_ipv6", settingsManager.getHevIpv6())
        json.put("hev_dns_port", settingsManager.getHevDnsPort())
        json.put("hev_dns_address", settingsManager.getHevDnsAddress())
        json.put("hev_socks5_port", settingsManager.getHevSocks5Port())
        json.put("hev_socks5_address", settingsManager.getHevSocks5Address())
        json.put("hev_socks5_udp", settingsManager.getHevSocks5Udp())
        json.put("hev_task_stack_size", settingsManager.getHevTaskStackSize())
        json.put("hev_tcp_buffer_size", settingsManager.getHevTcpBufferSize())
        json.put("hev_udp_recv_buffer_size", settingsManager.getHevUdpRecvBufferSize())
        json.put("hev_udp_copy_buffer_nums", settingsManager.getHevUdpCopyBufferNums())
        json.put("hev_max_session_count", settingsManager.getHevMaxSessionCount())
        json.put("hev_connect_timeout", settingsManager.getHevConnectTimeout())
        json.put("hev_tcp_read_write_timeout", settingsManager.getHevTcpReadWriteTimeout())
        json.put("hev_udp_read_write_timeout", settingsManager.getHevUdpReadWriteTimeout())
        json.put("hev_log_file", settingsManager.getHevLogFile())
        json.put("hev_log_level", settingsManager.getHevLogLevel())

        val bypassAppsJson = JSONArray()
        settingsManager.getBypassApps().forEach { bypassAppsJson.put(it) }
        json.put("bypass_apps_list", bypassAppsJson)

        val jsonString = json.toString()
        val base64Encoded = android.util.Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        return "SIVPN://" + base64Encoded
    }

    fun importConfigFromJson(encodedString: String): Boolean {
        return try {
            val contentToDecode = if (encodedString.startsWith("SIVPN://")) {
                encodedString.substring("SIVPN://".length)
            } else {
                encodedString
            }
            val jsonString = if (contentToDecode.trim().startsWith("{")) {
                contentToDecode
            } else {
                val decodedBytes = android.util.Base64.decode(contentToDecode, android.util.Base64.DEFAULT)
                String(decodedBytes, Charsets.UTF_8)
            }
            val json = JSONObject(jsonString)
            
            if (json.has("ssh_host")) settingsManager.setSshHost(json.getString("ssh_host"))
            if (json.has("ssh_port")) settingsManager.setSshPort(json.getInt("ssh_port"))
            if (json.has("ssh_username")) settingsManager.setSshUsername(json.getString("ssh_username"))
            if (json.has("ssh_password")) settingsManager.setSshPassword(json.getString("ssh_password"))
            if (json.has("payload")) settingsManager.setPayload(json.getString("payload"))
            if (json.has("proxy_host")) settingsManager.setProxyHost(json.getString("proxy_host"))
            if (json.has("proxy_port")) settingsManager.setProxyPort(json.getInt("proxy_port"))
            if (json.has("sni")) settingsManager.setSni(json.getString("sni"))
            if (json.has("dns")) settingsManager.setDns(json.getString("dns"))
            if (json.has("udpgw")) settingsManager.setUdpgw(json.getString("udpgw"))
            if (json.has("auto_ping")) settingsManager.setAutoPing(json.getBoolean("auto_ping"))
            if (json.has("forcing_tls")) settingsManager.setForcingTls(json.getString("forcing_tls"))
            if (json.has("connection_limit_minutes")) settingsManager.setConnectionLimitMinutes(json.getInt("connection_limit_minutes"))
            if (json.has("connection_limit_enabled")) settingsManager.setConnectionLimitEnabled(json.getBoolean("connection_limit_enabled"))
            if (json.has("ping_address")) settingsManager.setPingAddress(json.getString("ping_address"))
            if (json.has("split_tunneling_enabled")) settingsManager.setSplitTunnelingEnabled(json.getBoolean("split_tunneling_enabled"))
            if (json.has("apps_filter_mode")) settingsManager.setAppsFilterMode(json.getString("apps_filter_mode"))
            if (json.has("kill_switch_enabled")) settingsManager.setKillSwitchEnabled(json.getBoolean("kill_switch_enabled"))
            if (json.has("speedometer_enabled")) settingsManager.setSpeedometerEnabled(json.getBoolean("speedometer_enabled"))
            if (json.has("hotshare_socks_port")) settingsManager.setHotshareSocksPort(json.getInt("hotshare_socks_port"))
            if (json.has("hotshare_http_port")) settingsManager.setHotshareHttpPort(json.getInt("hotshare_http_port"))
            if (json.has("ip_auto_refresh_enabled")) settingsManager.setIpAutoRefreshEnabled(json.getBoolean("ip_auto_refresh_enabled"))
            if (json.has("ip_auto_refresh_interval")) settingsManager.setIpAutoRefreshInterval(json.getInt("ip_auto_refresh_interval"))
            if (json.has("hotshare_wakelock_enabled")) settingsManager.setHotshareWakeLockEnabled(json.getBoolean("hotshare_wakelock_enabled"))
            if (json.has("vpn_wakelock_enabled")) settingsManager.setVpnWakeLockEnabled(json.getBoolean("vpn_wakelock_enabled"))
            if (json.has("keep_alive_interval")) settingsManager.setKeepAliveInterval(json.getInt("keep_alive_interval"))
            if (json.has("auto_clean_logs_enabled")) settingsManager.setAutoCleanLogsEnabled(json.getBoolean("auto_clean_logs_enabled"))
            if (json.has("auto_clean_logs_interval")) settingsManager.setAutoCleanInterval(json.getInt("auto_clean_logs_interval"))
            if (json.has("max_log_lines")) settingsManager.setMaxLogLines(json.getInt("max_log_lines"))

            if (json.has("hev_mtu")) settingsManager.setHevMtu(json.getInt("hev_mtu"))
            if (json.has("hev_multi_queue")) settingsManager.setHevMultiQueue(json.getBoolean("hev_multi_queue"))
            if (json.has("hev_ipv4")) settingsManager.setHevIpv4(json.getString("hev_ipv4"))
            if (json.has("hev_ipv6")) settingsManager.setHevIpv6(json.getString("hev_ipv6"))
            if (json.has("hev_dns_port")) settingsManager.setHevDnsPort(json.getInt("hev_dns_port"))
            if (json.has("hev_dns_address")) settingsManager.setHevDnsAddress(json.getString("hev_dns_address"))
            if (json.has("hev_socks5_port")) settingsManager.setHevSocks5Port(json.getInt("hev_socks5_port"))
            if (json.has("hev_socks5_address")) settingsManager.setHevSocks5Address(json.getString("hev_socks5_address"))
            if (json.has("hev_socks5_udp")) settingsManager.setHevSocks5Udp(json.getString("hev_socks5_udp"))
            if (json.has("hev_task_stack_size")) settingsManager.setHevTaskStackSize(json.getInt("hev_task_stack_size"))
            if (json.has("hev_tcp_buffer_size")) settingsManager.setHevTcpBufferSize(json.getInt("hev_tcp_buffer_size"))
            if (json.has("hev_udp_recv_buffer_size")) settingsManager.setHevUdpRecvBufferSize(json.getInt("hev_udp_recv_buffer_size"))
            if (json.has("hev_udp_copy_buffer_nums")) settingsManager.setHevUdpCopyBufferNums(json.getInt("hev_udp_copy_buffer_nums"))
            if (json.has("hev_max_session_count")) settingsManager.setHevMaxSessionCount(json.getInt("hev_max_session_count"))
            if (json.has("hev_connect_timeout")) settingsManager.setHevConnectTimeout(json.getInt("hev_connect_timeout"))
            if (json.has("hev_tcp_read_write_timeout")) settingsManager.setHevTcpReadWriteTimeout(json.getInt("hev_tcp_read_write_timeout"))
            if (json.has("hev_udp_read_write_timeout")) settingsManager.setHevUdpReadWriteTimeout(json.getInt("hev_udp_read_write_timeout"))
            if (json.has("hev_log_file")) settingsManager.setHevLogFile(json.getString("hev_log_file"))
            if (json.has("hev_log_level")) settingsManager.setHevLogLevel(json.getString("hev_log_level"))

            if (json.has("bypass_apps_list")) {
                val array = json.getJSONArray("bypass_apps_list")
                val bypassSet = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    bypassSet.add(array.getString(i))
                }
                settingsManager.setBypassApps(bypassSet)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("ProfileManager", "Error importing config", e)
            false
        }
    }

    fun copyToClipboard(label: String, text: String): Boolean {
        return try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboardManager.setPrimaryClip(clip)
            LogManager.addLog("Konfigurasi disalin ke clipboard.")
            true
        } catch (e: Exception) {
            LogManager.addLog("Gagal menyalin ke clipboard: ${e.message}")
            false
        }
    }

    fun readFromClipboard(): String {
        return try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
