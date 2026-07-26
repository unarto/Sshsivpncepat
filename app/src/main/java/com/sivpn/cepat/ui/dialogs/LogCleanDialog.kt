package com.sivpn.cepat.ui.dialogs
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sivpn.cepat.vpn.LogManager

@Composable
fun LogCleanDialog(
    initialEnabled: Boolean,
    initialMaxLines: Int,
    onDismiss: () -> Unit,
    onSave: (Boolean, Int) -> Unit
) {
    var autoCleanEnabled by remember { mutableStateOf(initialEnabled) }
    var maxLogLines by remember { mutableStateOf(initialMaxLines) }
    
    val lineOptions = listOf(100, 300, 500, 1000)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pengaturan Bersih Log",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pembersihan Otomatis (Auto Clean)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Cegah aplikasi lag karena log terlalu penuh", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = autoCleanEnabled,
                        onCheckedChange = { autoCleanEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Batas Baris Log Maksimal (Buffer RAM)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lineOptions.forEach { lines ->
                        FilterChip(
                            selected = maxLogLines == lines,
                            onClick = { maxLogLines = lines },
                            label = { Text("$lines", fontSize = 12.sp) },
                            enabled = autoCleanEnabled
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        LogManager.clearLogs() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BERSIHKAN UNTUK SEMUA LOG SEKARANG", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            onSave(autoCleanEnabled, maxLogLines)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("KEMBALI KE CONSOLE")
                    }
                }
            }
        }
    }
}
