package com.sivpn.cepat.model

data class DialogUiState(
    val showJniDownloader: Boolean = false,
    val showProfileDialog: Boolean = false,
    val showPayloadDialog: Boolean = false,
    val showProxyDialog: Boolean = false,
    val showSshDialog: Boolean = false,
    val showAddProfileDialog: Boolean = false,
    val showLogDialog: Boolean = false,
    val showLimitDialog: Boolean = false,
    val showSplitTunnelingDialog: Boolean = false,
    val showKillSwitchDialog: Boolean = false,
    val showTetherDialog: Boolean = false,
    val showMenu: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showPingIntervalDialog: Boolean = false,
    val showForcingTlsDialog: Boolean = false,
    val showDnsDropdown: Boolean = false
)
