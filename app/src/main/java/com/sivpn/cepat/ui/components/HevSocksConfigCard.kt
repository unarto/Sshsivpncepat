package com.sivpn.cepat.ui.components
import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sivpn.cepat.model.MainUiState
import com.sivpn.cepat.viewmodel.MainHevSocksController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HevSocksConfigCard(
    uiState: MainUiState,
    controller: MainHevSocksController
) {
    var showAdvanced by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { controller.updateHevSocksExpanded(!uiState.isHevSocksExpanded) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = Icons.Default.SettingsApplications, contentDescription = "HevSocks Config", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "HevSocks Config", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "Pengaturan dari hev-socks5-tunnel", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(imageVector = if (uiState.isHevSocksExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Expand", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedVisibility(visible = uiState.isHevSocksExpanded) {
                Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.isVpnActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF7ED), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFEA580C), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Perubahan akan diterapkan saat VPN dijalankan kembali.", fontSize = 12.sp, color = Color(0xFFEA580C))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sesuai Parameter Native", fontSize = 12.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Reset Default", fontSize = 12.sp, color = Color(0xFFEF4444), modifier = Modifier.clickable {
                                controller.resetHevSocksDefaults()
                            })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SegmentedButton(
                            selected = !showAdvanced,
                            onClick = { showAdvanced = false },
                            label = "Basic Mode"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SegmentedButton(
                            selected = showAdvanced,
                            onClick = { showAdvanced = true },
                            label = "Advanced Mode"
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- TUNNEL CONFIGURATION ---
                    SectionTitle("TUNNEL CONFIGURATION")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.hevMtu.toString(),
                            onValueChange = { controller.updateHevMtu(it) },
                            label = { Text("MTU (1200-9000)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Multi-Queue", fontSize = 13.sp)
                            Text("Experimental. Beberapa kernel Android mungkin tidak mendukung fitur ini.", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(checked = uiState.hevMultiQueue, onCheckedChange = { controller.updateHevMultiQueue(it) })
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = uiState.hevIpv4, onValueChange = { controller.updateHevIpv4(it) }, label = { Text("IPv4 Address") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = uiState.hevIpv6, onValueChange = { controller.updateHevIpv6(it) }, label = { Text("IPv6 Address") }, modifier = Modifier.weight(1f), singleLine = true)
                    }

                    // --- DNS ---
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle("DNS")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = uiState.hevDnsAddress, onValueChange = { controller.updateHevDnsAddress(it) }, label = { Text("DNS Address") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = uiState.hevDnsPort.toString(), onValueChange = { controller.updateHevDnsPort(it) }, label = { Text("DNS Port") }, modifier = Modifier.weight(0.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }

                    // --- SOCKS5 SETTINGS ---
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle("SOCKS5 SETTINGS")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = uiState.hevSocks5Address, onValueChange = { controller.updateHevSocks5Address(it) }, label = { Text("Socks5 Host") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = uiState.hevSocks5Port.toString(), onValueChange = { controller.updateHevSocks5Port(it) }, label = { Text("Socks5 Port") }, modifier = Modifier.weight(0.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("UDP Relay Mode", fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("udp", "udpgw", "none").forEach { mode ->
                            FilterChip(
                                selected = uiState.hevSocks5Udp == mode,
                                onClick = { controller.updateHevSocks5Udp(mode) },
                                label = { Text(mode) }
                            )
                        }
                    }

                    if (uiState.hevSocks5Udp == "udpgw") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.hevSocks5UdpAddress,
                            onValueChange = { controller.updateHevSocks5UdpAddress(it) },
                            label = { Text("UDPGW Address (IP:Port)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (showAdvanced) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Pipeline", fontSize = 13.sp)
                            Switch(checked = uiState.hevSocks5Pipeline, onCheckedChange = { controller.updateHevSocks5Pipeline(it) })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = uiState.hevSocks5Username, onValueChange = { controller.updateHevSocks5Username(it) }, label = { Text("Username") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = uiState.hevSocks5Password, onValueChange = { controller.updateHevSocks5Password(it) }, label = { Text("Password") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.hevSocks5Mark.toString(),
                            onValueChange = { controller.updateHevSocks5Mark(it) },
                            label = { Text("Mark (Advanced)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    // --- PERFORMANCE ---
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle("PERFORMANCE")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = uiState.hevConnectTimeout.toString(), onValueChange = { controller.updateHevConnectTimeout(it) }, label = { Text("Connect Timeout") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        OutlinedTextField(value = uiState.hevTcpReadWriteTimeout.toString(), onValueChange = { controller.updateHevTcpReadWriteTimeout(it) }, label = { Text("TCP Timeout") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = uiState.hevUdpReadWriteTimeout.toString(), onValueChange = { controller.updateHevUdpReadWriteTimeout(it) }, label = { Text("UDP Timeout") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }

                    if (showAdvanced) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = uiState.hevTaskStackSize.toString(), onValueChange = { controller.updateHevTaskStackSize(it) }, label = { Text("Task Stack Size") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                            OutlinedTextField(value = uiState.hevTcpBufferSize.toString(), onValueChange = { controller.updateHevTcpBufferSize(it) }, label = { Text("TCP Buffer Size") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = uiState.hevUdpRecvBufferSize.toString(), onValueChange = { controller.updateHevUdpRecvBufferSize(it) }, label = { Text("UDP Recv Buffer") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                            OutlinedTextField(value = uiState.hevUdpCopyBufferNums.toString(), onValueChange = { controller.updateHevUdpCopyBufferNums(it) }, label = { Text("UDP Copy Bufs") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = uiState.hevMaxSessionCount.toString(), onValueChange = { controller.updateHevMaxSessionCount(it) }, label = { Text("Max Session Count") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }

                    // --- LOGGING ---
                    if (showAdvanced) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionTitle("LOGGING")
                        OutlinedTextField(value = uiState.hevLogFile, onValueChange = { controller.updateHevLogFile(it) }, label = { Text("Log File (e.g. stderr)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Log Level", fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("debug", "info", "warn", "error").forEach { level ->
                                FilterChip(
                                    selected = uiState.hevLogLevel == level,
                                    onClick = { controller.updateHevLogLevel(level) },
                                    label = { Text(level) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Box(
        modifier = Modifier
            .background(if (selected) Color(0xFF3B82F6) else Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (selected) Color.White else Color(0xFF475569),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
