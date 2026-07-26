package com.sivpn.cepat.vpn

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object HotshareClientManager {
    
    data class ConnectedClient(
        val ip: String,
        val macAddress: String,
        val isProxyActive: Boolean,
        val lastActiveTime: Long
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeProxyClients = ConcurrentHashMap<String, Long>()
    
    private val _connectedClientsFlow = MutableStateFlow<List<ConnectedClient>>(emptyList())
    val connectedClientsFlow: StateFlow<List<ConnectedClient>> = _connectedClientsFlow.asStateFlow()

    private var refreshJob: Job? = null

    init {
        startPeriodicRefresh()
    }

    /**
     * Registers activity for a specific client IP address.
     * Called whenever a proxy client establishes or transfers data on a socket.
     */
    fun registerClientActivity(ipAddress: String?) {
        if (ipAddress.isNullOrBlank()) return
        
        // Skip loopback interfaces
        if (ipAddress == "127.0.0.1" || ipAddress == "::1" || ipAddress.lowercase() == "localhost") return
        
        scope.launch(Dispatchers.Default) {
            val now = System.currentTimeMillis()
            activeProxyClients[ipAddress] = now
            refreshList()
        }
    }

    /**
     * Start periodic refresh to prune old proxy client connections and read ARP records
     */
    fun startPeriodicRefresh() {
        if (refreshJob?.isActive == true) return
        
        refreshJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                refreshList()
                delay(4000) // Refresh every 4 seconds
            }
        }
    }

    /**
     * Stop periodic refresh
     */
    fun stopPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    /**
     * Forces a refresh and updates the downstream Flow
     */
    suspend fun refreshList() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val timeoutThreshold = 30000L // 30 seconds idle timeout for proxy activity

        // 1. Prune expired proxy clients
        val iterator = activeProxyClients.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > timeoutThreshold) {
                iterator.remove()
            }
        }

        // 2. Read ARP table for physical devices on hotspot interface
        val arpDevices = getArpClients()

        // 3. Merge ARP entries with active proxy client tracking
        val mergedList = mutableMapOf<String, ConnectedClient>()

        // Add ARP clients first
        arpDevices.forEach { (ip, mac) ->
            val lastActive = activeProxyClients[ip]
            val isProxyActive = lastActive != null && (now - lastActive < timeoutThreshold)
            mergedList[ip] = ConnectedClient(
                ip = ip,
                macAddress = mac,
                isProxyActive = isProxyActive,
                lastActiveTime = lastActive ?: 0L
            )
        }

        // Add proxy clients that might not be in ARP (or if ARP read failed/is restricted in Android 10+)
        activeProxyClients.forEach { (ip, lastActive) ->
            if (!mergedList.containsKey(ip)) {
                mergedList[ip] = ConnectedClient(
                    ip = ip,
                    macAddress = "Menghubungi...",
                    isProxyActive = true,
                    lastActiveTime = lastActive
                )
            }
        }

        val updatedList = mergedList.values.toList().sortedWith(
            compareByDescending<ConnectedClient> { it.isProxyActive }
                .thenBy { it.ip }
        )

        _connectedClientsFlow.value = updatedList
    }

    /**
     * Retrieve list of active IP to MAC entries from /proc/net/arp
     */
    private fun getArpClients(): List<Pair<String, String>> {
        val clients = mutableListOf<Pair<String, String>>()
        try {
            val file = File("/proc/net/arp")
            if (file.exists() && file.canRead()) {
                file.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val tokens = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                        // Structure: IP address, HW type, Flags, HW address, Mask, Device
                        if (tokens.size >= 4 && !line.contains("IP address", ignoreCase = true)) {
                            val ip = tokens[0]
                            val flags = tokens[2]
                            val mac = tokens[3]
                            // "0x0" is incomplete or inactive entry, "00:00:00:00:00:00" is dummy
                            if (flags != "0x0" && mac != "00:00:00:00:00:00" && mac.length == 17) {
                                clients.add(ip to mac)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Quietly ignore or log
        }
        return clients
    }

    /**
     * Clear all current clients
     */
    fun clear() {
        activeProxyClients.clear()
        _connectedClientsFlow.value = emptyList()
    }
}
