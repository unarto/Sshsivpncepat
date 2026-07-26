package com.sivpn.cepat.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sivpn.cepat.model.MainUiState
import com.sivpn.cepat.monitor.PingMonitor
import com.sivpn.cepat.monitor.PublicIpMonitor
import com.sivpn.cepat.monitor.SpeedMonitor
import com.sivpn.cepat.config.ProfileManager
import com.sivpn.cepat.repository.LogRepository
import com.sivpn.cepat.config.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel(
    private val settingsManager: SettingsManager,
    private val logRepository: LogRepository,
    private val profileManager: ProfileManager,
    publicIpMonitor: PublicIpMonitor = PublicIpMonitor(),
    pingMonitor: PingMonitor = PingMonitor(),
    speedMonitor: SpeedMonitor = SpeedMonitor()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Domain Controllers
    val hevSocksController = MainHevSocksController(settingsManager, _uiState)
    val sshController = MainSshController(settingsManager, _uiState)
    val proxyController = MainProxyController(settingsManager, _uiState)
    val profileController = MainProfileController(settingsManager, logRepository, _uiState) { loadSettings() }
    val monitorController = MainMonitorController(publicIpMonitor, pingMonitor, speedMonitor, _uiState, viewModelScope)

    init {
        loadSettings()
        monitorController.startConnectionStatusPolling()
        observeLogs()
    }

    fun loadSettings() {
        _uiState.update { state ->
            state.copy(
                themeMode = settingsManager.getThemeMode(),
                tcpFastOpenEnabled = settingsManager.getTcpFastOpenEnabled(),
                autoPing = settingsManager.getAutoPing(),
                pingAddress = settingsManager.getPingAddress(),
                splitTunnelingEnabled = settingsManager.getSplitTunnelingEnabled(),
                appsFilterMode = settingsManager.getAppsFilterMode(),
                bypassApps = settingsManager.getBypassApps(),
                killSwitchEnabled = settingsManager.getKillSwitchEnabled(),
                forcingTls = settingsManager.getForcingTls(),
                speedometerEnabled = settingsManager.getSpeedometerEnabled(),
                autoReconnectEnabled = settingsManager.getAutoReconnectEnabled(),
                ipAutoRefreshEnabled = settingsManager.getIpAutoRefreshEnabled(),
                ipAutoRefreshInterval = settingsManager.getIpAutoRefreshInterval(),
                hotshareWakeLockEnabled = settingsManager.getHotshareWakeLockEnabled(),
                vpnWakeLockEnabled = settingsManager.getVpnWakeLockEnabled(),
                keepAliveInterval = settingsManager.getKeepAliveInterval(),
                autoCleanLogsEnabled = settingsManager.getAutoCleanLogsEnabled(),
                autoCleanInterval = settingsManager.getAutoCleanInterval(),
                maxLogLines = settingsManager.getMaxLogLines(),
                isNativeSshLoadedState = com.sivpn.cepat.ssh.LibSsh2Client.isLibraryLoaded,
                isHevLoadedState = com.sivpn.cepat.hevsocks.HevManager.isLibraryLoaded,
                connectionLimitMinutes = settingsManager.getConnectionLimitMinutes(),
                connectionLimitEnabled = settingsManager.getConnectionLimitEnabled(),
                statusCardVisible = settingsManager.getStatusCardVisible()
            )
        }

        profileController.loadSettings()
        sshController.loadSettings()
        proxyController.loadSettings()
        hevSocksController.loadSettings()

        restartMonitors()
    }

    private fun observeLogs() {
        logRepository.setMaxLogLines(_uiState.value.maxLogLines)
    }

    // Monitor Delegations
    fun restartMonitors() = monitorController.restartMonitors()
    fun restartPublicIpMonitor() = monitorController.restartPublicIpMonitor()
    fun restartPingMonitor() = monitorController.restartPingMonitor()
    fun startSpeedMonitor(context: Context) = monitorController.startSpeedMonitor(context)

    // Profile Delegations
    fun selectProfile(profile: String) = profileController.selectProfile(profile)
    fun addProfile(name: String) = profileController.addProfile(name)
    fun deleteProfile(profile: String): Boolean = profileController.deleteProfile(profile)
    fun deleteCurrentProfile(): Boolean = profileController.deleteCurrentProfile()

    // SSH Delegations
    fun updateSshFullInput(input: String) = sshController.updateSshFullInput(input)
    fun updatePayload(payload: String) = sshController.updatePayload(payload)

    // Proxy Delegations
    fun updateProxyFullInput(input: String) = proxyController.updateProxyFullInput(input)
    fun updateSni(sni: String) = proxyController.updateSni(sni)
    fun updateDns(dns: String) = proxyController.updateDns(dns)
    fun updateUdpgw(udpgw: String) = proxyController.updateUdpgw(udpgw)

    // HEV Delegations
    fun updateHevSocksExpanded(expanded: Boolean) = hevSocksController.updateHevSocksExpanded(expanded)
    fun updateHevMtu(mtu: String) = hevSocksController.updateHevMtu(mtu)
    fun updateHevMultiQueue(mq: Boolean) = hevSocksController.updateHevMultiQueue(mq)
    fun updateHevIpv4(ip: String) = hevSocksController.updateHevIpv4(ip)
    fun updateHevIpv6(ip: String) = hevSocksController.updateHevIpv6(ip)
    fun updateHevDnsAddress(addr: String) = hevSocksController.updateHevDnsAddress(addr)
    fun updateHevDnsPort(portStr: String) = hevSocksController.updateHevDnsPort(portStr)
    fun updateHevSocks5Address(addr: String) = hevSocksController.updateHevSocks5Address(addr)
    fun updateHevSocks5Port(portStr: String) = hevSocksController.updateHevSocks5Port(portStr)
    fun updateHevSocks5Udp(udp: String) = hevSocksController.updateHevSocks5Udp(udp)
    fun resetHevSocksDefaults() = hevSocksController.resetHevSocksDefaults()

    fun toggleHotshare(context: Context, enabled: Boolean) {
        _uiState.update { it.copy(hotshareEnabled = enabled) }
        if (enabled) {
            val socksPort = settingsManager.getHotshareSocksPort()
            val httpPort = settingsManager.getHotshareHttpPort()
            com.sivpn.cepat.vpn.HttpProxyServer.start(context, localBindPort = httpPort, socksPort = 1080)
        } else {
            com.sivpn.cepat.vpn.HttpProxyServer.stop()
        }
    }

    fun toggleHotspotRoot(enabled: Boolean) {
        if (enabled) {
            val success = com.sivpn.cepat.vpn.RootHotspotManager.startHotspotRouting()
            _uiState.update { it.copy(hotspotRootEnabled = success) }
        } else {
            com.sivpn.cepat.vpn.RootHotspotManager.stopHotspotRouting()
            _uiState.update { it.copy(hotspotRootEnabled = false) }
        }
    }

    // Settings & Config Actions
    fun updateThemeMode(mode: Int) {
        _uiState.update { it.copy(themeMode = mode) }
        settingsManager.setThemeMode(mode)
    }

    fun updateTcpFastOpenEnabled(enabled: Boolean) {
        _uiState.update { it.copy(tcpFastOpenEnabled = enabled) }
        settingsManager.setTcpFastOpenEnabled(enabled)
        logRepository.addLog("TCP Fast Open: $enabled")
    }

    fun updateSpeedometerEnabled(enabled: Boolean) {
        _uiState.update { it.copy(speedometerEnabled = enabled) }
        settingsManager.setSpeedometerEnabled(enabled)
        logRepository.addLog("Speedometer Real-time: $enabled")
    }

    fun updateSplitTunneling(enabled: Boolean, mode: String, apps: Set<String>) {
        _uiState.update {
            it.copy(
                splitTunnelingEnabled = enabled,
                appsFilterMode = mode,
                bypassApps = apps
            )
        }
        settingsManager.setSplitTunnelingEnabled(enabled)
        settingsManager.setAppsFilterMode(mode)
        settingsManager.setBypassApps(apps)
    }

    fun updateKillSwitch(enabled: Boolean) {
        _uiState.update { it.copy(killSwitchEnabled = enabled) }
        settingsManager.setKillSwitchEnabled(enabled)
    }

    fun updateVpnWakeLock(enabled: Boolean) {
        _uiState.update { it.copy(vpnWakeLockEnabled = enabled) }
        settingsManager.setVpnWakeLockEnabled(enabled)
    }

    fun updateAutoReconnect(enabled: Boolean) {
        _uiState.update { it.copy(autoReconnectEnabled = enabled) }
        settingsManager.setAutoReconnectEnabled(enabled)
    }

    fun updateIpAutoRefresh(enabled: Boolean) {
        _uiState.update { it.copy(ipAutoRefreshEnabled = enabled) }
        settingsManager.setIpAutoRefreshEnabled(enabled)
    }

    fun updateIpAutoRefreshInterval(interval: Int) {
        _uiState.update { it.copy(ipAutoRefreshInterval = interval) }
        settingsManager.setIpAutoRefreshInterval(interval)
    }

    fun updateAutoPing(enabled: Boolean) {
        _uiState.update { it.copy(autoPing = enabled) }
        settingsManager.setAutoPing(enabled)
    }

    fun updatePingAddress(address: String) {
        _uiState.update { it.copy(pingAddress = address) }
        settingsManager.setPingAddress(address)
    }

    fun updateForcingTls(tls: String) {
        _uiState.update { it.copy(forcingTls = tls) }
        settingsManager.setForcingTls(tls)
    }

    fun updateKeepAliveInterval(interval: Int) {
        _uiState.update { it.copy(keepAliveInterval = interval) }
        settingsManager.setKeepAliveInterval(interval)
    }

    fun updateTimeLimit(enabled: Boolean, minutes: Int) {
        _uiState.update { it.copy(connectionLimitEnabled = enabled, connectionLimitMinutes = minutes) }
        settingsManager.setConnectionLimitEnabled(enabled)
        settingsManager.setConnectionLimitMinutes(minutes)
    }

    fun updateAutoCleanLogs(enabled: Boolean, interval: Int, maxLines: Int) {
        _uiState.update { it.copy(autoCleanLogsEnabled = enabled, autoCleanInterval = interval, maxLogLines = maxLines) }
        settingsManager.setAutoCleanLogsEnabled(enabled)
        settingsManager.setAutoCleanInterval(interval)
        settingsManager.setMaxLogLines(maxLines)
    }

    // Clipboard & File import/export wrappers
    fun importFromClipboard(): Boolean {
        val content = profileManager.readFromClipboard()
        if (content.isNotEmpty()) {
            val success = profileManager.importConfigFromJson(content)
            if (success) {
                loadSettings()
                logRepository.addLog("Berhasil mengimpor dari clipboard.")
                return true
            } else {
                logRepository.addLog("Gagal mengimpor dari clipboard. Format tidak valid.")
            }
        }
        return false
    }

    fun exportToClipboard(): Boolean {
        val json = profileManager.exportConfigAsJson()
        return profileManager.copyToClipboard("SIVPN Config", json)
    }

    fun importConfigContent(content: String): Boolean {
        val success = profileManager.importConfigFromJson(content)
        if (success) {
            loadSettings()
            logRepository.addLog("Berhasil mengimpor konfigurasi dari berkas.")
        } else {
            logRepository.addLog("Gagal mengimpor: Berkas tidak valid.")
        }
        return success
    }

    fun exportConfigContent(): String {
        return profileManager.exportConfigAsJson()
    }

    override fun onCleared() {
        super.onCleared()
        monitorController.cancelAllJobs()
    }
}
