package com.sivpn.cepat.ui.dialogs
import androidx.compose.material3.MaterialTheme

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppItem(
    val name: String,
    val packageName: String,
    val iconDrawable: android.graphics.drawable.Drawable?
)

@Composable
fun SplitTunnelDialog(
    initialEnabled: Boolean,
    initialFilterMode: String,
    initialBypassApps: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, Set<String>) -> Unit
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(initialEnabled) }
    var filterMode by remember { mutableStateOf(initialFilterMode) }
    var selectedApps by remember { mutableStateOf(initialBypassApps) }
    
    var appList by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val items = packages.mapNotNull { appInfo ->
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    AppItem(
                        name = appInfo.loadLabel(pm).toString(),
                        packageName = appInfo.packageName,
                        iconDrawable = appInfo.loadIcon(pm)
                    )
                } else null
            }.sortedBy { it.name.lowercase() }
            appList = items
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (filterMode == "bypass") "Rute Aplikasi: Bypass" else "Rute Aplikasi: Only Tunnel",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (filterMode == "bypass") {
                        "Aplikasi yang dichecklist akan langsung melewati VPN (tanpa tunnel)."
                    } else {
                        "Hanya aplikasi yang dichecklist yang akan melewati VPN."
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Aktifkan Rute Khusus", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Mode Rute Aplikasi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val bypassColor = if (filterMode == "bypass") Color(0xFFEFF6FF) else Color.Transparent
                    val bypassTextColor = if (filterMode == "bypass") Color(0xFF3B82F6) else Color(0xFF94A3B8)
                    val bypassBorderColor = if (filterMode == "bypass") Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                    
                    val filterColor = if (filterMode == "filter") Color(0xFFEFF6FF) else Color.Transparent
                    val filterTextColor = if (filterMode == "filter") Color(0xFF3B82F6) else Color(0xFF94A3B8)
                    val filterBorderColor = if (filterMode == "filter") Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = enabled) { filterMode = "bypass" }
                            .background(bypassColor, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                            .border(1.dp, bypassBorderColor, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Bypass (Exclude)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = bypassTextColor)
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = enabled) { filterMode = "filter" }
                            .background(filterColor, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .border(1.dp, filterBorderColor, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Only Tunnel (Include)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = filterTextColor)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                var searchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari aplikasi...", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    enabled = enabled
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                    }
                } else {
                    val filteredApps = appList.filter { 
                        it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
                    }
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredApps) { app ->
                            val isChecked = selectedApps.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = enabled) {
                                        selectedApps = if (isChecked) {
                                            selectedApps - app.packageName
                                        } else {
                                            selectedApps + app.packageName
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                app.iconDrawable?.let { drawable ->
                                    Image(
                                        bitmap = drawable.toBitmap(48, 48).asImageBitmap(),
                                        contentDescription = app.name,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (enabled) MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8))
                                    Text(app.packageName, fontSize = 12.sp, color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFCBD5E1))
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedApps = if (checked) {
                                            selectedApps + app.packageName
                                        } else {
                                            selectedApps - app.packageName
                                        }
                                    },
                                    enabled = enabled
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            onSave(enabled, filterMode, selectedApps)
                            onDismiss()
                        }
                    ) {
                        Text("SELESAI", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                    }
                }
            }
        }
    }
}
