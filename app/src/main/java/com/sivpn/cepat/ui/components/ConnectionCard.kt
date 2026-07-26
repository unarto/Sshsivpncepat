package com.sivpn.cepat.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ConnectionCard(
    isVpnActive: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    VpnItemCard(
        icon = Icons.Default.PowerSettingsNew,
        iconColor = if (isVpnActive) Color(0xFF10B981) else Color(0xFF3B82F6),
        title = "Mulai Koneksi",
        subtitle = if (isVpnActive) "VPN terhubung" else "VPN terputus",
        onClick = {
            android.util.Log.d("ConnectionCard", "Tombol UI ditekan. isVpnActive = $isVpnActive")
            if (isVpnActive) onDisconnect() else onConnect()
        }
    ) {
        Switch(
            checked = isVpnActive,
            onCheckedChange = { active ->
                android.util.Log.d("ConnectionCard", "Switch UI ditekan. active = $active")
                if (active) onConnect() else onDisconnect()
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF3B82F6),
                checkedTrackColor = Color(0xFF93C5FD)
            )
        )
    }
}
