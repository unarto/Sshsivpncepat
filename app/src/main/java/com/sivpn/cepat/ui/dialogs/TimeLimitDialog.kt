package com.sivpn.cepat.ui.dialogs
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun TimeLimitDialog(
    initialLimitMinutes: Int,
    initialEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, Boolean) -> Unit
) {
    var limitEnabled by remember { mutableStateOf(initialEnabled) }
    var minutesText by remember { mutableStateOf(if (initialLimitMinutes > 0) initialLimitMinutes.toString() else "60") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Pengaturan Waktu & Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Batasi Durasi Koneksi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Putuskan VPN otomatis bila durasi tercapai", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = limitEnabled,
                        onCheckedChange = { limitEnabled = it }
                    )
                }

                if (limitEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it },
                        label = { Text("Durasi (Menit)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val mins = minutesText.toIntOrNull() ?: 60
                            onSave(mins, limitEnabled)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
