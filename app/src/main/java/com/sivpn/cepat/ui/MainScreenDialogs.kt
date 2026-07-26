package com.sivpn.cepat.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import com.sivpn.cepat.model.DialogUiState
import com.sivpn.cepat.model.MainUiState
import com.sivpn.cepat.ui.dialogs.*
import com.sivpn.cepat.viewmodel.DialogViewModel
import com.sivpn.cepat.viewmodel.MainViewModel

@Composable
fun MainScreenDialogs(
    uiState: MainUiState,
    dialogState: DialogUiState,
    viewModel: MainViewModel,
    dialogViewModel: DialogViewModel,
    logs: List<String>
) {
    if (dialogState.showPayloadDialog) {
        PayloadDialog(
            initialPayload = uiState.payload,
            onDismiss = { dialogViewModel.setShowPayloadDialog(false) },
            onSave = { newPayload -> viewModel.updatePayload(newPayload) }
        )
    }

    if (dialogState.showProfileDialog) {
        ProfileDialog(
            currentProfile = uiState.currentProfile,
            profileList = uiState.profileList,
            onDismiss = { dialogViewModel.setShowProfileDialog(false) },
            onSelectProfile = { profile -> viewModel.selectProfile(profile) },
            onAddProfileClick = { dialogViewModel.setShowAddProfileDialog(true) },
            onDeleteProfile = { profile -> viewModel.deleteProfile(profile) }
        )
    }

    if (dialogState.showAddProfileDialog) {
        AddProfileDialog(
            onDismiss = { dialogViewModel.setShowAddProfileDialog(false) },
            onSave = { name -> viewModel.addProfile(name) }
        )
    }

    if (dialogState.showSshDialog) {
        SshDialog(
            initialSshFullInput = uiState.sshFullInput,
            onDismiss = { dialogViewModel.setShowSshDialog(false) },
            onSave = { input -> viewModel.updateSshFullInput(input) }
        )
    }

    if (dialogState.showProxyDialog) {
        ProxyDialog(
            initialProxyFullInput = uiState.proxyFullInput,
            onDismiss = { dialogViewModel.setShowProxyDialog(false) },
            onSave = { input -> viewModel.updateProxyFullInput(input) }
        )
    }

    if (dialogState.showLogDialog) {
        LogDialog(
            logs = logs,
            onDismiss = { dialogViewModel.setShowLogDialog(false) },
            onSettingsClick = {
                dialogViewModel.setShowLogDialog(false)
                dialogViewModel.setShowSettingsDialog(true)
            }
        )
    }

    if (dialogState.showSplitTunnelingDialog) {
        val context = LocalContext.current
        SplitTunnelDialog(
            initialEnabled = uiState.splitTunnelingEnabled,
            initialFilterMode = uiState.appsFilterMode,
            initialBypassApps = uiState.bypassApps,
            onDismiss = { dialogViewModel.setShowSplitTunnelingDialog(false) },
            onSave = { enabled, mode, apps -> 
                viewModel.updateSplitTunneling(enabled, mode, apps)
                if (com.sivpn.cepat.vpn.SiVpnService.connectionState == "CONNECTED") {
                    android.widget.Toast.makeText(
                        context, 
                        "VPN perlu dihubungkan ulang agar pengaturan rute aplikasi yang baru dapat diterapkan.", 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    if (dialogState.showKillSwitchDialog) {
        KillSwitchDialog(
            initialEnabled = uiState.killSwitchEnabled,
            onDismiss = { dialogViewModel.setShowKillSwitchDialog(false) },
            onSave = { enabled ->
                viewModel.updateKillSwitch(enabled)
            }
        )
    }

    if (dialogState.showLimitDialog) {
        TimeLimitDialog(
            initialLimitMinutes = uiState.connectionLimitMinutes,
            initialEnabled = uiState.connectionLimitEnabled,
            onDismiss = { dialogViewModel.setShowLimitDialog(false) },
            onSave = { minutes, enabled ->
                viewModel.updateTimeLimit(enabled, minutes)
            }
        )
    }

    if (dialogState.showSettingsDialog) {
        LogCleanDialog(
            initialEnabled = uiState.autoCleanLogsEnabled,
            initialMaxLines = uiState.maxLogLines,
            onDismiss = { dialogViewModel.setShowSettingsDialog(false) },
            onSave = { enabled, maxLines ->
                viewModel.updateAutoCleanLogs(enabled, uiState.autoCleanInterval, maxLines)
                dialogViewModel.setShowSettingsDialog(false)
                dialogViewModel.setShowLogDialog(true)
            }
        )
    }

    if (dialogState.showPingIntervalDialog) {
        PingIntervalDialog(
            initialInterval = uiState.keepAliveInterval,
            onDismiss = { dialogViewModel.setShowPingIntervalDialog(false) },
            onSave = { interval ->
                viewModel.updateKeepAliveInterval(interval)
            }
        )
    }

    if (dialogState.showForcingTlsDialog) {
        // Build a simple alert dialog for Forcing TLS
        AlertDialog(
            onDismissRequest = { dialogViewModel.setShowForcingTlsDialog(false) },
            title = { Text("Forcing TLS") },
            text = {
                Column {
                    listOf("Auto", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3").forEach { tls ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateForcingTls(tls)
                                    dialogViewModel.setShowForcingTlsDialog(false)
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = (uiState.forcingTls == tls) || (tls == "Auto" && uiState.forcingTls.isEmpty()),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tls)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogViewModel.setShowForcingTlsDialog(false) }) {
                    Text("BATAL")
                }
            }
        )
    }

    if (dialogState.showDnsDropdown) {
        DnsDialog(
            initialDns = uiState.dns,
            onDismiss = { dialogViewModel.setShowDnsDropdown(false) },
            onSave = { dns -> viewModel.updateDns(dns) }
        )
    }

    if (dialogState.showJniDownloader) {
        JniDownloaderDialog(onDismiss = { dialogViewModel.setShowJniDownloader(false) })
    }
}
