package com.sivpn.cepat.viewmodel

import com.sivpn.cepat.config.DefaultValues
import com.sivpn.cepat.model.MainUiState
import com.sivpn.cepat.config.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainHevSocksController(
    private val settingsManager: SettingsManager,
    private val _uiState: MutableStateFlow<MainUiState>
) {

    private companion object {
        const val DEFAULT_HEV_MTU = DefaultValues.DEFAULT_TUN_MTU
        const val DEFAULT_HEV_DNS_PORT = 53
        const val DEFAULT_HEV_SOCKS5_PORT = DefaultValues.DEFAULT_SOCKS_PORT
    }

    private fun updateState(transform: (MainUiState) -> MainUiState) {
        _uiState.update(transform)
    }

    fun loadSettings() {
        updateState { state ->
            state
                .let(::loadNetworkConfig)
                .let(::loadBufferConfig)
                .let(::loadTimeoutConfig)
                .let(::loadLogConfig)
        }
    }

    private fun loadNetworkConfig(state: MainUiState): MainUiState {
        return state.copy(
            hevMtu = settingsManager.getHevMtu(),
            hevMultiQueue = settingsManager.getHevMultiQueue(),
            hevIpv4 = settingsManager.getHevIpv4(),
            hevIpv6 = settingsManager.getHevIpv6(),
            hevDnsPort = settingsManager.getHevDnsPort(),
            hevDnsAddress = settingsManager.getHevDnsAddress(),
            hevSocks5Port = settingsManager.getHevSocks5Port(),
            hevSocks5Address = settingsManager.getHevSocks5Address(),
            hevSocks5Udp = settingsManager.getHevSocks5Udp(),
            hevSocks5UdpAddress = settingsManager.getHevSocks5UdpAddress(),
            hevSocks5Pipeline = settingsManager.getHevSocks5Pipeline(),
            hevSocks5Username = settingsManager.getHevSocks5Username(),
            hevSocks5Password = settingsManager.getHevSocks5Password(),
            hevSocks5Mark = settingsManager.getHevSocks5Mark()
        )
    }

    private fun loadBufferConfig(state: MainUiState): MainUiState {
        return state.copy(
            hevTaskStackSize = settingsManager.getHevTaskStackSize(),
            hevTcpBufferSize = settingsManager.getHevTcpBufferSize(),
            hevUdpRecvBufferSize = settingsManager.getHevUdpRecvBufferSize(),
            hevUdpCopyBufferNums = settingsManager.getHevUdpCopyBufferNums()
        )
    }

    private fun loadTimeoutConfig(state: MainUiState): MainUiState {
        return state.copy(
            hevMaxSessionCount = settingsManager.getHevMaxSessionCount(),
            hevConnectTimeout = settingsManager.getHevConnectTimeout(),
            hevTcpReadWriteTimeout = settingsManager.getHevTcpReadWriteTimeout(),
            hevUdpReadWriteTimeout = settingsManager.getHevUdpReadWriteTimeout()
        )
    }

    private fun loadLogConfig(state: MainUiState): MainUiState {
        return state.copy(
            hevLogFile = settingsManager.getHevLogFile(),
            hevLogLevel = settingsManager.getHevLogLevel()
        )
    }

    fun updateHevSocksExpanded(expanded: Boolean) {
        updateState { it.copy(isHevSocksExpanded = expanded) }
    }

    fun updateHevMtu(mtu: String) {
        val parsed = mtu.toIntOrNull() ?: DEFAULT_HEV_MTU
        updateState { it.copy(hevMtu = parsed) }
        settingsManager.setHevMtu(parsed)
    }

    fun updateHevMultiQueue(mq: Boolean) {
        updateState { it.copy(hevMultiQueue = mq) }
        settingsManager.setHevMultiQueue(mq)
    }

    fun updateHevIpv4(ip: String) {
        updateState { it.copy(hevIpv4 = ip) }
        settingsManager.setHevIpv4(ip)
    }

    fun updateHevIpv6(ip: String) {
        updateState { it.copy(hevIpv6 = ip) }
        settingsManager.setHevIpv6(ip)
    }

    fun updateHevDnsAddress(addr: String) {
        updateState { it.copy(hevDnsAddress = addr) }
        settingsManager.setHevDnsAddress(addr)
    }

    fun updateHevDnsPort(portStr: String) {
        val port = portStr.toIntOrNull() ?: DEFAULT_HEV_DNS_PORT
        updateState { it.copy(hevDnsPort = port) }
        settingsManager.setHevDnsPort(port)
    }

    fun updateHevSocks5Address(addr: String) {
        updateState { it.copy(hevSocks5Address = addr) }
        settingsManager.setHevSocks5Address(addr)
    }

    fun updateHevSocks5Port(portStr: String) {
        val port = portStr.toIntOrNull() ?: DEFAULT_HEV_SOCKS5_PORT
        updateState { it.copy(hevSocks5Port = port) }
        settingsManager.setHevSocks5Port(port)
    }

    fun updateHevSocks5Udp(udp: String) {
        updateState { it.copy(hevSocks5Udp = udp) }
        settingsManager.setHevSocks5Udp(udp)
    }

    fun updateHevSocks5UdpAddress(addr: String) {
        updateState { it.copy(hevSocks5UdpAddress = addr) }
        settingsManager.setHevSocks5UdpAddress(addr)
    }

    fun updateHevSocks5Pipeline(pipeline: Boolean) {
        updateState { it.copy(hevSocks5Pipeline = pipeline) }
        settingsManager.setHevSocks5Pipeline(pipeline)
    }

    fun updateHevSocks5Username(username: String) {
        updateState { it.copy(hevSocks5Username = username) }
        settingsManager.setHevSocks5Username(username)
    }

    fun updateHevSocks5Password(password: String) {
        updateState { it.copy(hevSocks5Password = password) }
        settingsManager.setHevSocks5Password(password)
    }

    fun updateHevSocks5Mark(markStr: String) {
        val mark = markStr.toIntOrNull() ?: 0
        updateState { it.copy(hevSocks5Mark = mark) }
        settingsManager.setHevSocks5Mark(mark)
    }

    fun updateHevTaskStackSize(size: String) {
        val parsed = size.toIntOrNull() ?: 86016
        updateState { it.copy(hevTaskStackSize = parsed) }
        settingsManager.setHevTaskStackSize(parsed)
    }

    fun updateHevTcpBufferSize(size: String) {
        val parsed = size.toIntOrNull() ?: DefaultValues.DEFAULT_BUFFER_SIZE
        updateState { it.copy(hevTcpBufferSize = parsed) }
        settingsManager.setHevTcpBufferSize(parsed)
    }

    fun updateHevUdpRecvBufferSize(size: String) {
        val parsed = size.toIntOrNull() ?: 524288
        updateState { it.copy(hevUdpRecvBufferSize = parsed) }
        settingsManager.setHevUdpRecvBufferSize(parsed)
    }

    fun updateHevUdpCopyBufferNums(nums: String) {
        val parsed = nums.toIntOrNull() ?: 10
        updateState { it.copy(hevUdpCopyBufferNums = parsed) }
        settingsManager.setHevUdpCopyBufferNums(parsed)
    }

    fun updateHevMaxSessionCount(count: String) {
        val parsed = count.toIntOrNull() ?: 0
        updateState { it.copy(hevMaxSessionCount = parsed) }
        settingsManager.setHevMaxSessionCount(parsed)
    }

    fun updateHevConnectTimeout(timeout: String) {
        val parsed = timeout.toIntOrNull() ?: 10000
        updateState { it.copy(hevConnectTimeout = parsed) }
        settingsManager.setHevConnectTimeout(parsed)
    }

    fun updateHevTcpReadWriteTimeout(timeout: String) {
        val parsed = timeout.toIntOrNull() ?: DefaultValues.DEFAULT_READ_WRITE_TIMEOUT
        updateState { it.copy(hevTcpReadWriteTimeout = parsed) }
        settingsManager.setHevTcpReadWriteTimeout(parsed)
    }

    fun updateHevUdpReadWriteTimeout(timeout: String) {
        val parsed = timeout.toIntOrNull() ?: DefaultValues.DEFAULT_CONNECT_TIMEOUT
        updateState { it.copy(hevUdpReadWriteTimeout = parsed) }
        settingsManager.setHevUdpReadWriteTimeout(parsed)
    }

    fun updateHevLogFile(file: String) {
        updateState { it.copy(hevLogFile = file) }
        settingsManager.setHevLogFile(file)
    }

    fun updateHevLogLevel(level: String) {
        updateState { it.copy(hevLogLevel = level) }
        settingsManager.setHevLogLevel(level)
    }

    fun resetHevSocksDefaults() {
        settingsManager.resetHevDefaults()
        loadSettings()
    }
}
