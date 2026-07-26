package com.sivpn.cepat.ui.components
import androidx.compose.material3.MaterialTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SplitTunnelingCard(
    enabled: Boolean,
    bypassCount: Int,
    onClick: () -> Unit
) {
    VpnItemCard(
        icon = Icons.Default.Apps,
        iconColor = if (enabled) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
        title = "Split Tunneling (Bypass Apps)",
        subtitle = if (enabled) "$bypassCount Aplikasi Dikecualikan" else "Nonaktif (Seluruh Trafik Lewat VPN)",
        onClick = onClick
    )
}
