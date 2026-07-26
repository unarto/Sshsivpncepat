package com.sivpn.cepat.ui.components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
@Composable
fun ForcingTlsCard(tlsVersion: String, onClick: () -> Unit) {
    VpnItemCard(
        icon = Icons.Default.Security,
        iconColor = Color(0xFF8B5CF6),
        title = "Forcing TLS",
        subtitle = if (tlsVersion.isEmpty()) "Auto" else tlsVersion,
        onClick = onClick
    )
}
