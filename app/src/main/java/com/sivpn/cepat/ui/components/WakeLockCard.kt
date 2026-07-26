package com.sivpn.cepat.ui.components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
@Composable
fun WakeLockCard(enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    VpnItemCard(
        icon = Icons.Default.LockClock,
        iconColor = Color(0xFF8B5CF6),
        title = "SIVPN CPU WakeLock",
        subtitle = if (enabled) "Aktif (Koneksi Lebih Stabil)" else "Tidak Aktif",
        trailingContent = { Switch(checked = enabled, onCheckedChange = onCheckedChange) }
    )
}
