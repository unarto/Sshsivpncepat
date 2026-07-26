package com.sivpn.cepat.ui.components
import androidx.compose.material3.MaterialTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun KillSwitchCard(
    enabled: Boolean,
    onClick: () -> Unit
) {
    VpnItemCard(
        icon = Icons.Default.Security,
        iconColor = if (enabled) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
        title = "Kill Switch (Keamanan)",
        subtitle = if (enabled) "Aktif (Blokir Internet Saat Terputus)" else "Nonaktif",
        onClick = onClick
    )
}
