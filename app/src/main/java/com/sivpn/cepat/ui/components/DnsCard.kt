package com.sivpn.cepat.ui.components
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsCard(dns: String, onDnsChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val presets = listOf(
        Pair("Google DNS", "8.8.8.8:8.8.4.4"),
        Pair("Cloudflare (1.1.1.1)", "1.1.1.1:1.0.0.1"),
        Pair("AdGuard DNS (Blokir Iklan)", "94.140.14.14:94.140.15.15"),
        Pair("Quad9 (Malware Block)", "9.9.9.9:149.112.112.112"),
        Pair("OpenDNS", "208.67.222.222:208.67.220.220"),
        Pair("ControlD (Uncensored)", "76.76.2.0:76.76.10.0")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFD1FAE5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "DNS",
                        tint = Color(0xFF059669)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "DNS Settings (Primary : Secondary)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = dns,
                    onValueChange = onDnsChange,
                    label = { Text("DNS (Format: 94.140.14.14:94.140.15.15)") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(preset.first, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(preset.second, fontSize = 12.sp, color = Color.Gray)
                                }
                            },
                            onClick = {
                                onDnsChange(preset.second)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
