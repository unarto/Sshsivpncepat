package com.sivpn.cepat.viewmodel

import com.sivpn.cepat.config.DefaultValues
import com.sivpn.cepat.model.MainUiState
import com.sivpn.cepat.parser.ProxyParser
import com.sivpn.cepat.config.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainProxyController(
    private val settingsManager: SettingsManager,
    private val _uiState: MutableStateFlow<MainUiState>
) {

    private companion object {
        const val DEFAULT_PROXY_PORT = DefaultValues.DEFAULT_PROXY_PORT
    }

    private fun updateState(transform: (MainUiState) -> MainUiState) {
        _uiState.update(transform)
    }

    fun loadSettings() {
        val proxyHost = settingsManager.getProxyHost()
        val proxyPort = settingsManager.getProxyPort().toString()
        val proxyFullInput = if (proxyHost.isEmpty()) "" else "$proxyHost:$proxyPort"
        val sni = settingsManager.getSni()
        val dns = settingsManager.getDns()
        val udpgw = settingsManager.getUdpgw()

        updateState { state ->
            state.copy(
                proxyHost = proxyHost,
                proxyPort = proxyPort,
                proxyFullInput = proxyFullInput,
                sni = sni,
                dns = dns,
                udpgw = udpgw
            )
        }
    }

    fun updateProxyFullInput(input: String) {
        val proxy = ProxyParser.parseFullInput(input, _uiState.value.proxyPort)
        val portInt = proxy.port.toIntOrNull() ?: DEFAULT_PROXY_PORT

        updateState {
            it.copy(
                proxyFullInput = input,
                proxyHost = proxy.host,
                proxyPort = proxy.port
            )
        }

        settingsManager.setProxyHost(proxy.host)
        settingsManager.setProxyPort(portInt)
    }

    fun updateSni(sni: String) {
        updateState { it.copy(sni = sni) }
        settingsManager.setSni(sni)
    }

    fun updateDns(dns: String) {
        updateState { it.copy(dns = dns) }
        settingsManager.setDns(dns)
    }

    fun updateUdpgw(udpgw: String) {
        updateState { it.copy(udpgw = udpgw) }
        settingsManager.setUdpgw(udpgw)
    }
}
