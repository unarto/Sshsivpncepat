package com.sivpn.cepat.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AutoPingCard(enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    VpnItemCard(
        icon = Icons.Default.CompareArrows,
        iconColor = Color(0xFFEF4444),
        title = "Auto ping",
        subtitle = "Ping Off for keep-alive service",
        onClick = { onCheckedChange(!enabled) },
        trailingContent = {
            Checkbox(
                checked = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    )
}
