package com.sivpn.cepat.ssh
import com.sivpn.cepat.vpn.LogManager

import android.util.Base64
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

object WebSocketEngine {
    private val secureRandom = SecureRandom()

    fun extractWsKey(payload: String): String? {
        val lines = payload.split("\r\n", "\n")
        for (line in lines) {
            if (line.lowercase(Locale.ENGLISH).startsWith("sec-websocket-key:")) {
                val key = line.substringAfter(":").trim()
                if (key.isNotEmpty()) return key
            }
        }
        return null
    }

    fun generateWsKey(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun calculateWsAccept(key: String): String {
        val concat = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(concat.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    fun prepareWsPayloadChunks(chunks: List<PayloadChunk>): Pair<List<PayloadChunk>, String> {
        val fullPayload = chunks.joinToString("") { it.content }
        var secWsKey = extractWsKey(fullPayload)
        
        if (secWsKey == null) {
            secWsKey = generateWsKey()
        }
        
        val newChunks = chunks.toMutableList()
        val headersToInject = java.lang.StringBuilder()
        
        if (!fullPayload.contains("Upgrade:", ignoreCase = true)) headersToInject.append("Upgrade: websocket\r\n")
        if (!fullPayload.contains("Connection:", ignoreCase = true)) headersToInject.append("Connection: Upgrade\r\n")
        if (!fullPayload.contains("Sec-WebSocket-Version:", ignoreCase = true)) headersToInject.append("Sec-WebSocket-Version: 13\r\n")
        if (!fullPayload.contains("Sec-WebSocket-Key:", ignoreCase = true)) headersToInject.append("Sec-WebSocket-Key: $secWsKey\r\n")

        if (headersToInject.isNotEmpty() && newChunks.isNotEmpty()) {
            val lastIdx = newChunks.lastIndex
            var lastContent = newChunks[lastIdx].content
            
            while (lastContent.endsWith("\r\n")) {
                lastContent = lastContent.substring(0, lastContent.length - 2)
            }
            while (lastContent.endsWith("\n")) {
                lastContent = lastContent.substring(0, lastContent.length - 1)
            }
            
            lastContent += "\r\n" + headersToInject.toString() + "\r\n"
            newChunks[lastIdx] = newChunks[lastIdx].copy(content = lastContent)
        }
        return Pair(newChunks, secWsKey)
    }

    fun prepareWsPayload(parsedPayload: String): Pair<String, String> {
        var secWsKey = extractWsKey(parsedPayload)
        if (secWsKey == null) {
            secWsKey = generateWsKey()
        }
        
        var finalPayload = parsedPayload
        val headersToInject = java.lang.StringBuilder()
        
        if (!finalPayload.contains("Upgrade:", ignoreCase = true)) headersToInject.append("Upgrade: websocket\r\n")
        if (!finalPayload.contains("Connection:", ignoreCase = true)) headersToInject.append("Connection: Upgrade\r\n")
        if (!finalPayload.contains("Sec-WebSocket-Version:", ignoreCase = true)) headersToInject.append("Sec-WebSocket-Version: 13\r\n")
        if (!finalPayload.contains("Sec-WebSocket-Key:", ignoreCase = true)) headersToInject.append("Sec-WebSocket-Key: $secWsKey\r\n")

        if (headersToInject.isNotEmpty()) {
            var cleanPayload = finalPayload
            while (cleanPayload.endsWith("\r\n")) {
                cleanPayload = cleanPayload.substring(0, cleanPayload.length - 2)
            }
            while (cleanPayload.endsWith("\n")) {
                cleanPayload = cleanPayload.substring(0, cleanPayload.length - 1)
            }
            finalPayload = cleanPayload + "\r\n" + headersToInject.toString() + "\r\n"
        }
        
        return Pair(finalPayload, secWsKey)
    }

    fun readWsHandshakeResponse(input: InputStream, expectedKey: String): Boolean {
        LogManager.addLog("WebSocket Handshake Start")
        val headerBytes = ByteArrayOutputStream()
        var last4 = 0
        var last2 = 0
        while (true) {
            val b = try {
                input.read()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                LogManager.addLog("Handshake Failed: Error reading input - ${e.message}")
                return false
            }
            if (b == -1) {
                LogManager.addLog("Handshake Failed: EOF while reading headers")
                return false
            }
            headerBytes.write(b)
            last4 = ((last4 shl 8) or b) and 0xFFFFFFFF.toInt()
            last2 = ((last2 shl 8) or b) and 0xFFFF
            if (last4 == 0x0D0A0D0A || last2 == 0x0A0A) {
                break
            }
            if (headerBytes.size() > 16 * 1024) {
                LogManager.addLog("Handshake Failed: Headers too large (> 16KB)")
                return false
            }
        }
        val headers = headerBytes.toString("UTF-8")
        val lines = headers.split(Regex("\r?\n")).map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty() || !lines[0].contains("101")) {
            LogManager.addLog("Handshake Failed: Missing or invalid 101 status")
            return false
        }
        LogManager.addLog("WebSocket HTTP 101 Switching Protocols received")
        
        var hasUpgrade = false
        var hasConnection = false
        var acceptValid = false
        val expectedAccept = if (expectedKey.isNotBlank()) calculateWsAccept(expectedKey) else ""
        
        for (line in lines) {
            val lower = line.lowercase(Locale.ENGLISH)
            if (lower.startsWith("upgrade:") && lower.contains("websocket")) hasUpgrade = true
            if (lower.startsWith("connection:") && lower.contains("upgrade")) hasConnection = true
            if (lower.startsWith("sec-websocket-accept:")) {
                val acceptVal = line.substringAfter(":").trim()
                if (expectedAccept.isNotEmpty()) {
                    if (acceptVal == expectedAccept) {
                        acceptValid = true
                    }
                } else {
                    if (acceptVal.isNotEmpty()) {
                        acceptValid = true
                    }
                }
            }
        }
        if (hasUpgrade && hasConnection && acceptValid) {
            LogManager.addLog("Sec-WebSocket-Accept Valid")
            LogManager.addLog("WebSocket Handshake Success")
            return true
        } else {
            LogManager.addLog("Handshake Failed: Missing/invalid Upgrade, Connection, or Accept header")
            return false
        }
    }

    fun startAutoPing(output: OutputStream, scope: CoroutineScope): Job {
        return scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(30_000)
                try {
                    val mKey = ByteArray(4)
                    secureRandom.nextBytes(mKey)
                    val pingHeader = byteArrayOf(0x89.toByte(), 0x80.toByte(), mKey[0], mKey[1], mKey[2], mKey[3])
                    synchronized(output) {
                        output.write(pingHeader)
                        output.flush()
                    }
                    LogManager.addLog("Ping Sent")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LogManager.addLog("Connection Closed (Auto Ping Failed)")
                    break
                }
            }
        }
    }

    suspend fun forwardWsEncode(input: InputStream, output: OutputStream) {
        WebSocketFramer.forwardWsEncode(input, output)
    }

    suspend fun forwardWsDecode(input: InputStream, clientOutput: OutputStream, remoteOutput: OutputStream) {
        WebSocketFramer.forwardWsDecode(input, clientOutput, remoteOutput)
    }
}
