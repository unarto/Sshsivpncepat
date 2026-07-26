package com.sivpn.cepat.ui.components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
@Composable
fun AutoReconnectCard(enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    VpnItemCard(
        icon = Icons.Default.Autorenew,
        iconColor = Color(0xFF10B981),
        title = "Auto Reconnect",
        subtitle = "Otomatis menyambung saat terputus",
        trailingContent = { Switch(checked = enabled, onCheckedChange = onCheckedChange) }
    )
}
