package com.sivpn.cepat.ui.components
import androidx.compose.material3.MaterialTheme

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sivpn.cepat.model.MainUiState
import com.sivpn.cepat.model.DialogUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    uiState: MainUiState,
    dialogState: DialogUiState,
    onRefreshIp: () -> Unit,
    onSplitTunnelingClick: () -> Unit,
    onKillSwitchClick: () -> Unit,
    onLogClick: () -> Unit,
    onMenuToggle: (Boolean) -> Unit,
    onAddProfileClick: () -> Unit,
    onDeleteProfileClick: () -> Unit,
    onThemeModeChange: (Int) -> Unit,
    onSpeedometerToggle: (Boolean) -> Unit,
    onHotshareToggle: (Boolean) -> Unit,
    onHotspotRootToggle: (Boolean) -> Unit,
    onImportFileClick: () -> Unit,
    onImportClipboardClick: () -> Unit,
    onExportFileClick: () -> Unit,
    onExportClipboardClick: () -> Unit
) {
    val context = LocalContext.current
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "SIVPN Cepat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val ipLabel = if (uiState.currentPublicIp.isNotEmpty()) uiState.currentPublicIp else uiState.sshHost
                Row(
                    modifier = Modifier.clickable {
                        onRefreshIp()
                        Toast.makeText(context, "Memperbarui IP Publik...", Toast.LENGTH_SHORT).show()
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isVpnActive) {
                            if (uiState.connectionState == "CONNECTED") "Connected (IP: $ipLabel)" else "Connecting... (IP: $ipLabel)"
                        } else {
                            ipLabel
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh IP Publik",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSplitTunnelingClick) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Bypass Aplikasi",
                    tint = if (uiState.splitTunnelingEnabled) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onKillSwitchClick) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Kill Switch",
                    tint = if (uiState.killSwitchEnabled) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onLogClick) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Terminal Logs",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { onMenuToggle(true) }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            DropdownMenu(
                expanded = dialogState.showMenu,
                onDismissRequest = { onMenuToggle(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("Add Profile") },
                    onClick = {
                        onMenuToggle(false)
                        onAddProfileClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Current Profile") },
                    onClick = {
                        onMenuToggle(false)
                        onDeleteProfileClick()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Hotshare (Proxy)", fontSize = 14.sp)
                            Switch(
                                checked = uiState.hotshareEnabled,
                                onCheckedChange = { active ->
                                    onMenuToggle(false)
                                    onHotshareToggle(active)
                                }
                            )
                        }
                    },
                    onClick = {
                        val nextState = !uiState.hotshareEnabled
                        onHotshareToggle(nextState)
                        onMenuToggle(false)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Hotspot Root", fontSize = 14.sp)
                            Switch(
                                checked = uiState.hotspotRootEnabled,
                                onCheckedChange = { active ->
                                    onMenuToggle(false)
                                    onHotspotRootToggle(active)
                                }
                            )
                        }
                    },
                    onClick = {
                        val nextState = !uiState.hotspotRootEnabled
                        onHotspotRootToggle(nextState)
                        onMenuToggle(false)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                DropdownMenuItem(
                    text = {
                        val themeText = when (uiState.themeMode) {
                            1 -> "Mode Tema: Terang"
                            2 -> "Mode Tema: Gelap"
                            else -> "Mode Tema: Sistem"
                        }
                        Text(themeText)
                    },
                    onClick = {
                        val nextMode = (uiState.themeMode + 1) % 3
                        onThemeModeChange(nextMode)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Speedometer", fontSize = 14.sp)
                            Switch(
                                checked = uiState.speedometerEnabled,
                                onCheckedChange = { active ->
                                    onMenuToggle(false)
                                    onSpeedometerToggle(active)
                                }
                            )
                        }
                    },
                    onClick = {
                        val nextState = !uiState.speedometerEnabled
                        onSpeedometerToggle(nextState)
                        onMenuToggle(false)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                DropdownMenuItem(
                    text = { Text("Import Config (.sivpn)") },
                    onClick = {
                        onMenuToggle(false)
                        onImportFileClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Import dari Clipboard") },
                    onClick = {
                        onMenuToggle(false)
                        onImportClipboardClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export Config (.sivpn)") },
                    onClick = {
                        onMenuToggle(false)
                        onExportFileClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export ke Clipboard") },
                    onClick = {
                        onMenuToggle(false)
                        onExportClipboardClick()
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
