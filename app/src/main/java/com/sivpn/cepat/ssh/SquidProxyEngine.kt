package com.sivpn.cepat.ssh
import com.sivpn.cepat.vpn.LogManager

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.net.Socket
import java.util.Locale

object SquidProxyEngine {
    fun connect(
        socket: Socket,
        host: String,
        port: Int,
        username: String? = null,
        password: String? = null
    ): Socket {
        val out = socket.outputStream
        val input = socket.inputStream

        val connectReq = StringBuilder()
        connectReq.append("CONNECT $host:$port HTTP/1.1\r\n")
        connectReq.append("Host: $host:$port\r\n")
        connectReq.append("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)\r\n")
        connectReq.append("Proxy-Connection: Keep-Alive\r\n")

        if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
            val auth = "$username:$password"
            val encodedAuth = Base64.encodeToString(auth.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            connectReq.append("Proxy-Authorization: Basic $encodedAuth\r\n")
        }
        connectReq.append("\r\n")

        synchronized(out) {
            out.write(connectReq.toString().toByteArray(Charsets.UTF_8))
            out.flush()
        }

        val headerBytes = ByteArrayOutputStream()
        var last4 = 0
        while (true) {
            val b = input.read()
            if (b == -1) throw Exception("Squid Proxy: Unexpected EOF while reading CONNECT response")
            headerBytes.write(b)
            last4 = ((last4 shl 8) or b) and 0xFFFFFFFF.toInt()
            if (last4 == 0x0D0A0D0A) {
                break
            }
            if (headerBytes.size() > 16 * 1024) {
                throw Exception("Squid Proxy: Response header too large")
            }
        }

        val responseHeaders = headerBytes.toString("UTF-8")
        val lines = responseHeaders.split(Regex("\r?\n")).map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty() || (!lines[0].contains("200") && !lines[0].lowercase(Locale.ENGLISH).contains("established"))) {
            throw Exception("Squid Proxy CONNECT failed: ${lines.firstOrNull() ?: "Empty Response"}")
        }

        return socket
    }
}
