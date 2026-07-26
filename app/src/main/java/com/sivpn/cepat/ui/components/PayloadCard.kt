package com.sivpn.cepat.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun PayloadCard(
    payload: String,
    onPayloadClick: () -> Unit
) {
    VpnItemCard(
        icon = Icons.Default.Code,
        iconColor = Color(0xFFF59E0B),
        title = "Payload / Bug Host",
        subtitle = if (payload.isEmpty()) "Kosong (Direct Connection)" else payload,
        onClick = onPayloadClick
    )
}
