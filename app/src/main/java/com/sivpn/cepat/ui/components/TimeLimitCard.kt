package com.sivpn.cepat.ui.components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
@Composable
fun TimeLimitCard(connectionLimitEnabled: Boolean, connectionLimitMinutes: Int, onClick: () -> Unit) {
    VpnItemCard(
        icon = Icons.Default.Timer,
        iconColor = Color(0xFFF59E0B),
        title = "Pengaturan Waktu",
        subtitle = if (connectionLimitEnabled) "Batas waktu: $connectionLimitMinutes menit" else "Tidak ada batas waktu",
        onClick = onClick
    )
}
