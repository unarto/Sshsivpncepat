package com.sivpn.cepat.vpn

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ConnectionManager {

    suspend fun extractAssets(context: Context) = withContext(Dispatchers.IO) {
        // Extraction removed: hev-socks5-tunnel is now loaded natively via JNI (libhev-socks5-tunnel.so).
    }
}
