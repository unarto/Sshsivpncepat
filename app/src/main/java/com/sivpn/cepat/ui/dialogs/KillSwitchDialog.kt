package com.sivpn.cepat.ui.dialogs
import androidx.compose.material3.MaterialTheme

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun KillSwitchDialog(
    initialEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(initialEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = "Kill Switch", tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kill Switch (Anti Bocor)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Aktifkan Kill Switch", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { 
                            enabled = it 
                            onSave(it)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Kill Switch mengunci lalu lintas internet agar tidak bocor keluar dari VPN saat koneksi sedang terputus atau mencoba menyambung kembali.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Tips", tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Rekomendasi Sistem:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            Text(
                                "Untuk perlindungan tingkat OS, buka Pengaturan Android VPN > Aktifkan 'VPN Selalu Aktif' dan 'Blokir koneksi tanpa VPN'.",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("TUTUP", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("BUKA PENGATURAN")
                    }
                }
            }
        }
    }
}
