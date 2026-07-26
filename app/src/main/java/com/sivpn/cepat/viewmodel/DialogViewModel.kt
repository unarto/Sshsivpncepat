package com.sivpn.cepat.viewmodel

import androidx.lifecycle.ViewModel
import com.sivpn.cepat.model.DialogUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DialogViewModel : ViewModel() {
    private val _dialogState = MutableStateFlow(DialogUiState())
    val dialogState: StateFlow<DialogUiState> = _dialogState.asStateFlow()

    fun setShowJniDownloader(show: Boolean) = _dialogState.update { it.copy(showJniDownloader = show) }
    fun setShowProfileDialog(show: Boolean) = _dialogState.update { it.copy(showProfileDialog = show) }
    fun setShowPayloadDialog(show: Boolean) = _dialogState.update { it.copy(showPayloadDialog = show) }
    fun setShowProxyDialog(show: Boolean) = _dialogState.update { it.copy(showProxyDialog = show) }
    fun setShowSshDialog(show: Boolean) = _dialogState.update { it.copy(showSshDialog = show) }
    fun setShowAddProfileDialog(show: Boolean) = _dialogState.update { it.copy(showAddProfileDialog = show) }
    fun setShowLogDialog(show: Boolean) = _dialogState.update { it.copy(showLogDialog = show) }
    fun setShowLimitDialog(show: Boolean) = _dialogState.update { it.copy(showLimitDialog = show) }
    fun setShowSplitTunnelingDialog(show: Boolean) = _dialogState.update { it.copy(showSplitTunnelingDialog = show) }
    fun setShowKillSwitchDialog(show: Boolean) = _dialogState.update { it.copy(showKillSwitchDialog = show) }
    fun setShowTetherDialog(show: Boolean) = _dialogState.update { it.copy(showTetherDialog = show) }
    fun setShowMenu(show: Boolean) = _dialogState.update { it.copy(showMenu = show) }
    fun setShowDnsDropdown(show: Boolean) = _dialogState.update { it.copy(showDnsDropdown = show) }
    fun setShowSettingsDialog(show: Boolean) = _dialogState.update { it.copy(showSettingsDialog = show) }
    fun setShowPingIntervalDialog(show: Boolean) = _dialogState.update { it.copy(showPingIntervalDialog = show) }
    fun setShowForcingTlsDialog(show: Boolean) = _dialogState.update { it.copy(showForcingTlsDialog = show) }
}
