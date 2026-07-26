package com.sivpn.cepat.model

data class SpeedInfo(
    val rxBytesPerSec: Long = 0L,
    val txBytesPerSec: Long = 0L
) {
    fun formatRxSpeed(): String {
        return formatSpeed(rxBytesPerSec)
    }

    fun formatTxSpeed(): String {
        return formatSpeed(txBytesPerSec)
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }
}
