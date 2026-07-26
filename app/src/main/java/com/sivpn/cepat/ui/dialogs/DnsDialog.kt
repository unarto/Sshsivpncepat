package com.sivpn.cepat.ui.dialogs
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun DnsDialog(
    initialDns: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var dnsInput by remember { mutableStateOf(initialDns) }

    val presets = listOf(
        Pair("Google DNS", "8.8.8.8:8.8.4.4"),
        Pair("Cloudflare DNS", "1.1.1.1:1.0.0.1"),
        Pair("AdGuard DNS (Anti-Iklan)", "94.140.14.14:94.140.15.15"),
        Pair("Quad9 DNS", "9.9.9.9:149.112.112.112"),
        Pair("OpenDNS", "208.67.222.222:208.67.220.220")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Pengaturan Custom DNS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = dnsInput,
                    onValueChange = { dnsInput = it },
                    label = { Text("Primary:Secondary DNS") },
                    placeholder = { Text("8.8.8.8:8.8.4.4") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Pilihan Preset DNS:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                    items(presets) { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dnsInput = preset.second }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = preset.first, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = preset.second, fontSize = 12.sp, color = Color(0xFF06B6D4))
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(dnsInput)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
