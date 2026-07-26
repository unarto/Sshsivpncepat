package com.sivpn.cepat.ssh
import com.sivpn.cepat.vpn.LogManager

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale

data class HttpResponse(
    val statusCode: Int,
    val statusLine: String,
    val headers: Map<String, String>,
    val rawResponse: String
)

object HttpPayloadEngine {

    val VALID_STATUS_CODES = setOf(200, 101, 301, 302, 307, 400, 403, 404, 407, 500, 502, 503, 504)

    fun formatPayloadChunks(payload: String, sshHost: String, sshPort: Int): List<PayloadChunk> {
        return buildPayloadChunks(payload, sshHost, sshPort)
    }

    fun buildPayloadChunks(
        payload: String,
        sshHost: String,
        sshPort: Int,
        proxyUsername: String? = null,
        proxyPassword: String? = null
    ): List<PayloadChunk> {
        if (payload.isBlank()) {
            val connectReq = StringBuilder()
            connectReq.append("CONNECT $sshHost:$sshPort HTTP/1.1\r\n")
            connectReq.append("Host: $sshHost:$sshPort\r\n")
            connectReq.append("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)\r\n")
            connectReq.append("Proxy-Connection: Keep-Alive\r\n")
            if (!proxyUsername.isNullOrEmpty() && !proxyPassword.isNullOrEmpty()) {
                val auth = "$proxyUsername:$proxyPassword"
                val encodedAuth = Base64.encodeToString(auth.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                connectReq.append("Proxy-Authorization: Basic $encodedAuth\r\n")
            }
            connectReq.append("\r\n")
            return listOf(PayloadChunk(connectReq.toString(), SplitType.NORMAL_SPLIT))
        }

        val chunks = PayloadFormatter.parsePayloadChunkDetails(payload, sshHost, sshPort).toMutableList()

        if (!proxyUsername.isNullOrEmpty() && !proxyPassword.isNullOrEmpty() && chunks.isNotEmpty()) {
            val lastIdx = chunks.lastIndex
            var lastContent = chunks[lastIdx].content
            if (!lastContent.contains("Proxy-Authorization:", ignoreCase = true)) {
                val auth = "$proxyUsername:$proxyPassword"
                val encodedAuth = Base64.encodeToString(auth.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                val authHeader = "Proxy-Authorization: Basic $encodedAuth\r\n"
                
                if (lastContent.endsWith("\r\n\r\n")) {
                    lastContent = lastContent.substring(0, lastContent.length - 2) + authHeader + "\r\n"
                } else if (lastContent.endsWith("\r\n")) {
                    lastContent += authHeader
                } else {
                    lastContent += "\r\n" + authHeader
                }
                chunks[lastIdx] = chunks[lastIdx].copy(content = lastContent)
            }
        }
        return chunks
    }

    fun injectPayloadChunks(chunks: List<PayloadChunk>, output: OutputStream) {
        sendPayloadChunks(chunks, output)
    }

    fun sendPayloadChunks(chunks: List<PayloadChunk>, output: OutputStream) {
        for (chunk in chunks) {
            if (chunk.content.isNotEmpty()) {
                val firstLine = chunk.content.lines().firstOrNull() ?: ""
                LogManager.addLog("Sending HTTP Payload Chunk: $firstLine")
                synchronized(output) {
                    output.write(chunk.content.toByteArray(Charsets.UTF_8))
                    output.flush()
                }
            }
            when (chunk.splitType) {
                SplitType.DELAY_SPLIT -> Thread.sleep(1000)
                SplitType.NORMAL_SPLIT -> Thread.sleep(200)
                SplitType.INSTANT_SPLIT -> { /* No delay */ }
            }
        }
    }

    fun readResponse(input: InputStream): String {
        val headerBytes = ByteArrayOutputStream()
        var last4 = 0
        while (true) {
            val b = input.read()
            if (b == -1) break
            headerBytes.write(b)
            last4 = ((last4 shl 8) or b) and 0xFFFFFFFF.toInt()
            if (last4 == 0x0D0A0D0A || (last4 and 0xFFFF) == 0x0A0A) {
                break
            }
            if (headerBytes.size() > 16 * 1024) {
                break
            }
        }
        return headerBytes.toString("UTF-8")
    }

    fun parseResponse(rawResponse: String): HttpResponse {
        val lines = rawResponse.split(Regex("\r?\n")).map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return HttpResponse(0, "", emptyMap(), rawResponse)
        }

        val statusLine = lines[0]
        val statusParts = statusLine.split(" ")
        val statusCode = if (statusParts.size >= 2) {
            statusParts[1].toIntOrNull() ?: 0
        } else {
            0
        }

        val headers = mutableMapOf<String, String>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            val colonIdx = line.indexOf(':')
            if (colonIdx != -1) {
                val key = line.substring(0, colonIdx).trim()
                val value = line.substring(colonIdx + 1).trim()
                headers[key.lowercase(Locale.ENGLISH)] = value
            }
        }

        return HttpResponse(statusCode, statusLine, headers, rawResponse)
    }

    fun validateResponse(response: HttpResponse): Boolean {
        LogManager.addLog("HTTP Response: ${response.statusLine} (Code: ${response.statusCode})")
        return response.statusCode in VALID_STATUS_CODES
    }
}
