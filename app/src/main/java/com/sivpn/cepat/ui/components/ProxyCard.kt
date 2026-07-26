package com.sivpn.cepat.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Router
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ProxyCard(
    proxyFullInput: String,
    onProxyClick: () -> Unit
) {
    VpnItemCard(
        icon = Icons.Default.Router,
        iconColor = Color(0xFF10B981),
        title = "Remote Squid Proxy",
        subtitle = if (proxyFullInput.isEmpty()) "Kosong (Menggunakan IP SSH)" else proxyFullInput,
        onClick = onProxyClick
    )
}
