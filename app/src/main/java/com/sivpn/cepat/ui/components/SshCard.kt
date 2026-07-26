package com.sivpn.cepat.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SshCard(
    sshHost: String,
    sshPort: String,
    sshUsername: String,
    onSshClick: () -> Unit
) {
    val subtitleText = if (sshHost.isEmpty()) {
        "Pengaturan Server & Akun SSH"
    } else {
        "$sshHost:$sshPort ($sshUsername)"
    }

    VpnItemCard(
        icon = Icons.Default.Dns,
        iconColor = Color(0xFF3B82F6),
        title = "Akun & Server SSH",
        subtitle = subtitleText,
        onClick = onSshClick
    )
}
