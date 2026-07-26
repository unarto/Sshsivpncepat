package com.sivpn.cepat.ui.components
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sivpn.cepat.model.MainUiState
import com.sivpn.cepat.model.SpeedInfo

@Composable
fun StatusCard(
    uiState: MainUiState,
    onSetTimeLimitClick: () -> Unit
) {
    if (!uiState.statusCardVisible) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (uiState.connectionState) {
                "CONNECTED" -> Color(0xFFECFDF5)
                "CONNECTING" -> Color(0xFFFFFBEB)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when (uiState.connectionState) {
                "CONNECTED" -> Color(0xFF10B981)
                "CONNECTING" -> Color(0xFFF59E0B)
                else -> Color(0xFFE2E8F0)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (uiState.connectionState != "DISCONNECTED") 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when (uiState.connectionState) {
                                    "CONNECTED" -> Color(0xFF10B981)
                                    "CONNECTING" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF94A3B8)
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STATUS KONEKSI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                color = when (uiState.connectionState) {
                                    "CONNECTED" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    "CONNECTING" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else -> Color(0xFFF1F5F9)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (uiState.connectionState) {
                                "CONNECTED" -> "TERHUBUNG"
                                "CONNECTING" -> "MENYAMBUNGKAN"
                                else -> "TERPUTUS"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (uiState.connectionState) {
                                "CONNECTED" -> Color(0xFF047857)
                                "CONNECTING" -> Color(0xFFB45309)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val hours = uiState.elapsedSeconds / 3600
                    val minutes = (uiState.elapsedSeconds % 3600) / 60
                    val seconds = uiState.elapsedSeconds % 60
                    val timeStr = if (hours > 0) {
                        String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format("%02d:%02d", minutes, seconds)
                    }
                    Text(
                        text = timeStr,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Ping section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Ping",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val pingStr = if (uiState.connectionState == "CONNECTED") {
                        if (uiState.currentPingMs >= 0) "${uiState.currentPingMs} ms" else "Pinging..."
                    } else {
                        "- ms"
                    }
                    Text(
                        text = pingStr,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (uiState.speedometerEnabled && uiState.connectionState == "CONNECTED") {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                val speedInfo = SpeedInfo(uiState.rxSpeedBytesSec, uiState.txSpeedBytesSec)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DL: ${speedInfo.formatRxSpeed()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0284C7)
                    )
                    Text(
                        text = "UL: ${speedInfo.formatTxSpeed()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF16A34A)
                    )
                }
            }
        }
    }
}
