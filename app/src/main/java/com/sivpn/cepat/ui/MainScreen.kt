package com.sivpn.cepat.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sivpn.cepat.repository.LogRepository
import com.sivpn.cepat.ui.components.*
import com.sivpn.cepat.ui.dialogs.*
import com.sivpn.cepat.viewmodel.MainViewModel
import com.sivpn.cepat.viewmodel.DialogViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    dialogViewModel: DialogViewModel,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialogState by dialogViewModel.dialogState.collectAsStateWithLifecycle()
    val logs by com.sivpn.cepat.vpn.LogManager.logs.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            if (uri != null) {
                val json = viewModel.exportConfigContent()
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Konfigurasi berhasil diekspor!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal mengekspor konfigurasi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.use { isStream ->
                        String(isStream.readBytes(), Charsets.UTF_8)
                    }
                    if (content != null && viewModel.importConfigContent(content)) {
                        Toast.makeText(context, "Konfigurasi berhasil diimpor!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal mengimpor: Berkas tidak valid", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membaca berkas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val vpnGradientBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFFFFFFFF),
            0.5f to Color(0xFFFFFFFF),
            1.0f to Color(0xFFE0F2FE),
            1.0f to Color(0xFF87CEEB)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(vpnGradientBrush)
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    uiState = uiState,
                    dialogState = dialogState,
                    onRefreshIp = { viewModel.restartPublicIpMonitor() },
                    onSplitTunnelingClick = { dialogViewModel.setShowSplitTunnelingDialog(true) },
                    onKillSwitchClick = { dialogViewModel.setShowKillSwitchDialog(true) },
                    onLogClick = { dialogViewModel.setShowLogDialog(true) },
                    onMenuToggle = { show -> dialogViewModel.setShowMenu(show) },
                    onAddProfileClick = { dialogViewModel.setShowAddProfileDialog(true) },
                    onDeleteProfileClick = {
                        if (!viewModel.deleteCurrentProfile()) {
                            Toast.makeText(context, "Tidak dapat menghapus profile terakhir!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onThemeModeChange = { mode -> viewModel.updateThemeMode(mode) },
                    onSpeedometerToggle = { active -> viewModel.updateSpeedometerEnabled(active) },
                    onHotshareToggle = { active -> viewModel.toggleHotshare(context, active) },
                    onHotspotRootToggle = { active -> viewModel.toggleHotspotRoot(active) },
                    onImportFileClick = { importLauncher.launch(arrayOf("*/*")) },
                    onImportClipboardClick = {
                        if (viewModel.importFromClipboard()) {
                            Toast.makeText(context, "Konfigurasi berhasil diimpor!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Gagal mengimpor dari clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onExportFileClick = { exportLauncher.launch("${uiState.currentProfile}.sivpn") },
                    onExportClipboardClick = {
                        if (viewModel.exportToClipboard()) {
                            Toast.makeText(context, "Konfigurasi disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    JniWarningCard(
                        isNativeSshLoadedState = uiState.isNativeSshLoadedState,
                        isHevLoadedState = uiState.isHevLoadedState,
                        onDownloadClick = { dialogViewModel.setShowJniDownloader(true) }
                    )

                    SectionHeader("CONTROLLER")
                    ConnectionCard(
                        isVpnActive = uiState.isVpnActive,
                        onConnect = onConnect,
                        onDisconnect = onDisconnect
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusCard(
                        uiState = uiState,
                        onSetTimeLimitClick = { dialogViewModel.setShowLimitDialog(true) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileCard(
                        currentProfile = uiState.currentProfile,
                        onProfileClick = { dialogViewModel.setShowProfileDialog(true) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeLimitCard(
                        connectionLimitEnabled = uiState.connectionLimitEnabled,
                        connectionLimitMinutes = uiState.connectionLimitMinutes,
                        onClick = { dialogViewModel.setShowLimitDialog(true) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader("CONNECTION")

                    SshCard(
                        sshHost = uiState.sshHost,
                        sshPort = uiState.sshPort,
                        sshUsername = uiState.sshUsername,
                        onSshClick = { dialogViewModel.setShowSshDialog(true) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PayloadCard(
                        payload = uiState.payload,
                        onPayloadClick = { dialogViewModel.setShowPayloadDialog(true) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ProxyCard(
                        proxyFullInput = uiState.proxyFullInput,
                        onProxyClick = { dialogViewModel.setShowProxyDialog(true) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SniCard(
                        sni = uiState.sni,
                        onSniChange = { viewModel.updateSni(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    DnsCard(
                        dns = uiState.dns,
                        onDnsChange = { viewModel.updateDns(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader("SETTINGS")

                    BatteryOptimizationCard(
                        isBatteryOptimized = uiState.isBatteryOptimized,
                        onBypassClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Tidak dapat membuka pengaturan baterai", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WakeLockCard(
                        enabled = uiState.vpnWakeLockEnabled,
                        onCheckedChange = { viewModel.updateVpnWakeLock(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PingIntervalCard(
                        interval = uiState.keepAliveInterval,
                        onClick = { dialogViewModel.setShowPingIntervalDialog(true) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AutoReconnectCard(
                        enabled = uiState.autoReconnectEnabled,
                        onCheckedChange = { viewModel.updateAutoReconnect(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AutoRefreshIpCard(
                        enabled = uiState.ipAutoRefreshEnabled,
                        interval = uiState.ipAutoRefreshInterval,
                        onCheckedChange = { viewModel.updateIpAutoRefresh(it) },
                        onIntervalChange = { viewModel.updateIpAutoRefreshInterval(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    UdpgwCard(
                        udpgw = uiState.udpgw,
                        onUdpgwChange = { viewModel.updateUdpgw(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    HevSocksConfigCard(
                        uiState = uiState,
                        controller = viewModel.hevSocksController
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AutoPingCard(
                        enabled = uiState.autoPing,
                        onCheckedChange = { viewModel.updateAutoPing(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    CustomPingCard(
                        pingAddress = uiState.pingAddress,
                        onPingAddressChange = { viewModel.updatePingAddress(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ForcingTlsCard(
                        tlsVersion = uiState.forcingTls,
                        onClick = { dialogViewModel.setShowForcingTlsDialog(true) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    KillSwitchCard(
                        enabled = uiState.killSwitchEnabled,
                        onClick = { dialogViewModel.setShowKillSwitchDialog(true) }
                    )
                }
            }
        }

        MainScreenDialogs(
            uiState = uiState,
            dialogState = dialogState,
            viewModel = viewModel,
            dialogViewModel = dialogViewModel,
            logs = logs
        )
    }
}
