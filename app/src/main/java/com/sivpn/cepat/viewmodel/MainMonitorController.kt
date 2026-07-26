package com.sivpn.cepat.viewmodel

import android.content.Context
import com.sivpn.cepat.model.MainUiState
import com.sivpn.cepat.monitor.PingMonitor
import com.sivpn.cepat.monitor.PublicIpMonitor
import com.sivpn.cepat.monitor.SpeedMonitor
import com.sivpn.cepat.vpn.SiVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainMonitorController(
    private val publicIpMonitor: PublicIpMonitor = PublicIpMonitor(),
    private val pingMonitor: PingMonitor = PingMonitor(),
    private val speedMonitor: SpeedMonitor = SpeedMonitor(),
    private val _uiState: MutableStateFlow<MainUiState>,
    private val coroutineScope: CoroutineScope
) {

    private companion object {
        const val STATE_CONNECTED = "CONNECTED"
        const val TIMER_DELAY_MS = 1000L
    }

    private var connectionJob: Job? = null
    private var elapsedTimerJob: Job? = null
    private var ipJob: Job? = null
    private var pingJob: Job? = null
    private var speedJob: Job? = null

    private fun updateState(transform: (MainUiState) -> MainUiState) {
        _uiState.update(transform)
    }

    fun startConnectionStatusPolling() {
        connectionJob?.cancel()
        connectionJob = coroutineScope.launch(Dispatchers.IO) {
            combine(
                SiVpnService.isRunningFlow,
                SiVpnService.connectionStateFlow,
                SiVpnService.connectionStartTimeFlow
            ) { isActive, connState, startTime ->
                val elapsed = if (isActive && connState == STATE_CONNECTED && startTime > 0L) {
                    (System.currentTimeMillis() - startTime) / 1000
                } else {
                    0L
                }

                updateState {
                    it.copy(
                        isVpnActive = isActive,
                        connectionState = connState,
                        connectionStartTime = startTime,
                        elapsedSeconds = elapsed
                    )
                }
            }.collect {}
        }

        elapsedTimerJob?.cancel()
        elapsedTimerJob = coroutineScope.launch(Dispatchers.IO) {
            while (currentCoroutineContext().isActive) {
                val state = _uiState.value
                if (state.isVpnActive && state.connectionState == STATE_CONNECTED && state.connectionStartTime > 0L) {
                    updateState {
                        it.copy(elapsedSeconds = (System.currentTimeMillis() - it.connectionStartTime) / 1000)
                    }
                }
                delay(TIMER_DELAY_MS)
            }
        }
    }

    fun restartMonitors() {
        restartPublicIpMonitor()
        restartPingMonitor()
    }

    fun restartPublicIpMonitor() {
        ipJob?.cancel()
        ipJob = coroutineScope.launch {
            _uiState
                .map { "${it.connectionState}|${it.sshHost}|${it.ipAutoRefreshEnabled}|${it.ipAutoRefreshInterval}" }
                .distinctUntilChanged()
                .flatMapLatest {
                    val state = _uiState.value
                    publicIpMonitor.monitorPublicIp(
                        connectionState = state.connectionState,
                        sshHost = state.sshHost,
                        autoRefreshEnabled = state.ipAutoRefreshEnabled,
                        intervalSeconds = state.ipAutoRefreshInterval,
                        manualRefreshTrigger = 0
                    )
                }
                .collect { ip ->
                    updateState { it.copy(currentPublicIp = ip) }
                }
        }
    }

    fun restartPingMonitor() {
        pingJob?.cancel()
        pingJob = coroutineScope.launch {
            _uiState
                .map { "${it.connectionState}|${it.sshHost}|${it.sshPort}|${it.pingAddress}" }
                .distinctUntilChanged()
                .flatMapLatest {
                    val state = _uiState.value
                    pingMonitor.monitorPing(
                        connectionState = state.connectionState,
                        sshHost = state.sshHost,
                        sshPort = state.sshPort,
                        pingAddress = state.pingAddress
                    )
                }
                .collect { latency ->
                    updateState { it.copy(currentPingMs = latency) }
                }
        }
    }

    fun startSpeedMonitor(context: Context) {
        val appContext = context.applicationContext
        speedJob?.cancel()
        speedJob = coroutineScope.launch {
            _uiState
                .map { "${it.isVpnActive}|${it.connectionState}|${it.speedometerEnabled}" }
                .distinctUntilChanged()
                .flatMapLatest {
                    val state = _uiState.value
                    speedMonitor.monitorSpeed(
                        context = appContext,
                        isVpnActive = state.isVpnActive,
                        connectionState = state.connectionState,
                        speedometerEnabled = state.speedometerEnabled
                    )
                }
                .collect { info ->
                    updateState {
                        it.copy(
                            rxSpeedBytesSec = info.rxBytesPerSec,
                            txSpeedBytesSec = info.txBytesPerSec
                        )
                    }
                }
        }
    }

    fun cancelAllJobs() {
        connectionJob?.cancel()
        elapsedTimerJob?.cancel()
        ipJob?.cancel()
        pingJob?.cancel()
        speedJob?.cancel()
    }
}
