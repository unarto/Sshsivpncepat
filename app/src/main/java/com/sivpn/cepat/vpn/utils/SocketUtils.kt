package com.sivpn.cepat.vpn.utils

import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import java.net.Socket
import android.util.Log

object SocketUtils {
    private var isTfoSupported: Boolean? = null

    fun enableTcpFastOpen(socket: Socket) {
        if (isTfoSupported == false) return // Fast exit if known unsupported
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            isTfoSupported = false
            return
        }

        var pfd: ParcelFileDescriptor? = null
        try {
            pfd = ParcelFileDescriptor.fromSocket(socket)
            val fd = pfd.fileDescriptor
            // 30 is TCP_FASTOPEN_CONNECT on Linux/Android
            Os.setsockoptInt(fd, OsConstants.IPPROTO_TCP, 30, 1)
            isTfoSupported = true
        } catch (e: Exception) {
            if (isTfoSupported == null) {
                Log.w("SocketUtils", "TCP Fast Open not supported: ${e.message}")
            }
            isTfoSupported = false
        } finally {
            try { pfd?.close() } catch (e: Exception) {
                Log.w("SocketUtils", "Failed to close PFD: ${e.message}")
            }
        }
    }
}
