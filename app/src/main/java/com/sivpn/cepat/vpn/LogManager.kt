package com.sivpn.cepat.vpn

import com.sivpn.cepat.config.SettingsManager

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

object LogManager {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()
    
    var maxLogLines: Int = 1000

    fun addLog(line: String) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val timestamp = sdf.format(java.util.Date())
        val threadId = Thread.currentThread().id
        val formattedLine = "[$timestamp] [Thread-$threadId] $line"

        _logs.update { current ->
            val updated = current.toMutableList()
            updated.add(formattedLine)
            if (updated.size > maxLogLines) {
                while (updated.size > maxLogLines && updated.isNotEmpty()) {
                    updated.removeAt(0)
                }
            }
            updated
        }
    }

    fun clearLogs() {
        _logs.update { emptyList() }
    }

    fun clearPhysicalLogFile(context: Context) {
        val logFileSetting = SettingsManager(context).getHevLogFile()
        if (logFileSetting != "stderr" && logFileSetting.isNotBlank()) {
            try {
                val file = if (logFileSetting.startsWith("/")) {
                    File(logFileSetting)
                } else {
                    File(context.filesDir, logFileSetting)
                }
                if (file.exists() && file.isFile) {
                    file.writeText("") // Truncate the file
                    addLog("--- Berkas log fisik '${file.name}' berhasil dibersihkan otomatis ---")
                }
            } catch (e: Exception) {
                addLog("Gagal membersihkan berkas log fisik: ${e.message}")
            }
        }
    }
}
