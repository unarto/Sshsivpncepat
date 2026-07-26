package com.sivpn.cepat.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ProfileCard(
    currentProfile: String,
    onProfileClick: () -> Unit
) {
    VpnItemCard(
        icon = Icons.Default.AccountCircle,
        iconColor = Color(0xFF8B5CF6),
        title = "Profile Server",
        subtitle = if (currentProfile.isEmpty()) "Pilih Profile..." else currentProfile,
        onClick = onProfileClick
    )
}
