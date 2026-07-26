package com.sivpn.cepat.ui.components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
@Composable
fun PingIntervalCard(interval: Int, onClick: () -> Unit) {
    VpnItemCard(
        icon = Icons.Default.NetworkPing,
        iconColor = Color(0xFF06B6D4),
        title = "Interval Keep-Alive (Ping)",
        subtitle = "$interval detik",
        onClick = onClick
    )
}
