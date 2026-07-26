package com.sivpn.cepat.ui.components
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
@Composable
fun UdpgwCard(udpgw: String, onUdpgwChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            OutlinedTextField(
                value = udpgw,
                onValueChange = onUdpgwChange,
                label = { Text("UDPGW address:port") },
                placeholder = { Text("127.0.0.1:7300") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(imageVector = Icons.Default.AccountTree, contentDescription = "UDPGW", tint = Color(0xFFEAB308)) }
            )
        }
    }
}
