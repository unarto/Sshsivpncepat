package com.sivpn.cepat.ui.components
import androidx.compose.material3.MaterialTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsCard(
    onClick: () -> Unit
) {
    VpnItemCard(
        icon = Icons.Default.Settings,
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = "Pengaturan Lanjutan",
        subtitle = "HevSocks, Timeout, Log Clean, KeepAlive",
        onClick = onClick
    )
}
