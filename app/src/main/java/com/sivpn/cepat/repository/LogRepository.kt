package com.sivpn.cepat.repository

import com.sivpn.cepat.vpn.LogManager
import kotlinx.coroutines.flow.StateFlow

class LogRepository {

    val logs: StateFlow<List<String>> = LogManager.logs

    fun addLog(log: String) {
        LogManager.addLog(log)
    }

    fun clearLogs() {
        LogManager.clearLogs()
    }

    fun setMaxLogLines(maxLines: Int) {
        LogManager.maxLogLines = maxLines
    }
}
