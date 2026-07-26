package com.sivpn.cepat.vpn

import com.sivpn.cepat.config.SettingsManager

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import com.sivpn.cepat.vpn.utils.SocketUtils
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object HttpProxyServer {
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private val clientJobs = mutableListOf<Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private const val BUFFER_SIZE = 32 * 1024
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val IDLE_TIMEOUT_MS = 5 * 60 * 1000 // 5 minutes

    private val connectionSemaphore = Semaphore(100)
    private val activeConnections = AtomicInteger(0)

    @Volatile
    var isRunning = false
        private set

    @Volatile
    var activePort = 8080
        private set

    private var isTfoEnabled = false

    fun start(context: android.content.Context, localBindPort: Int = 8080, socksHost: String = "127.0.0.1", socksPort: Int = 1080) {
        if (socksHost.isBlank() || socksPort !in 1..65535) {
            LogManager.addLog("[Hotshare HTTP] SOCKS target tidak valid, server tidak dijalankan.")
            isRunning = false
            return
        }

        if (isRunning) {
            stop()
        }

        isTfoEnabled = com.sivpn.cepat.config.SettingsManager(context).getTcpFastOpenEnabled()

        isRunning = true
        acceptJob = scope.launch {
            var portToTry = localBindPort.coerceIn(1, 65535)
            var success = false

            val maxPort = (portToTry + 10).coerceAtMost(65535)
            while (portToTry <= maxPort && !success && isRunning) {
                try {
                    serverSocket = ServerSocket()
                    serverSocket?.reuseAddress = true
                    serverSocket?.bind(InetSocketAddress("0.0.0.0", portToTry))
                    serverSocket?.soTimeout = 1000 // Timeout untuk stop responsif
                    activePort = portToTry
                    success = true
                    LogManager.addLog("[Hotshare HTTP] Server aktif di 0.0.0.0:$activePort -> SOCKS5 $socksHost:$socksPort")
                    HotshareWakeLockManager.acquire(context)
                } catch (e: Exception) {
                    LogManager.addLog("[Hotshare HTTP] Gagal menggunakan port $portToTry: ${e.message}. Mencoba port berikutnya...")
                    portToTry++
                }
            }

            if (!success) {
                LogManager.addLog("[Hotshare HTTP] Gagal memulai server HTTP Proxy pada rentang port $localBindPort-$maxPort.")
                isRunning = false
                return@launch
            }

            try {
                while (isRunning) {
                    var clientSocket: Socket? = null
                    try {
                        clientSocket = serverSocket?.accept()
                    } catch (e: SocketTimeoutException) {
                        continue
                    } catch (e: SocketException) {
                        break
                    } catch (e: IOException) {
                        if (isRunning) {
                            LogManager.addLog("[Hotshare HTTP] Accept error: ${e.message}")
                        }
                        break
                    } catch (e: Exception) {
                        break
                    }

                    if (clientSocket != null) {
                        if (connectionSemaphore.tryAcquire()) {
                            val job = launch {
                                try {
                                    handleClient(clientSocket, socksHost, socksPort)
                                } finally {
                                    connectionSemaphore.release()
                                }
                            }
                            synchronized(clientJobs) {
                                clientJobs.add(job)
                            }
                            job.invokeOnCompletion {
                                synchronized(clientJobs) {
                                    clientJobs.remove(job)
                                }
                            }
                        } else {
                            LogManager.addLog("[Hotshare HTTP] Batas maksimum 100 koneksi tercapai. Menolak klien baru.")
                            gracefulClose(clientSocket)
                        }
                    }
                }
            } finally {
                isRunning = false
                closeServerSocket()
                if (!LocalPortForwarder.isRunning) {
                    HotshareWakeLockManager.release()
                }
            }
        }
    }

    private suspend fun handleClient(clientSocket: Socket, socksHost: String, socksPort: Int) = coroutineScope {
        val startTime = System.currentTimeMillis()
        var socksSocket: Socket? = null
        val clientIp = clientSocket.inetAddress?.hostAddress ?: "unknown"
        val count = activeConnections.incrementAndGet()
        
        try {
            HotshareClientManager.registerClientActivity(clientIp)

            clientSocket.tcpNoDelay = true
            clientSocket.keepAlive = true
            clientSocket.soTimeout = IDLE_TIMEOUT_MS
            clientSocket.receiveBufferSize = BUFFER_SIZE
            clientSocket.sendBufferSize = BUFFER_SIZE

            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()

            // Read headers byte-by-byte until \r\n\r\n
            val headerBuffer = ByteArrayOutputStream()
            var state = 0
            while (true) {
                val b = clientIn.read()
                if (b == -1) break
                headerBuffer.write(b)
                if (state == 0 && b == '\r'.code) state = 1
                else if (state == 1 && b == '\n'.code) state = 2
                else if (state == 2 && b == '\r'.code) state = 3
                else if (state == 3 && b == '\n'.code) {
                    break
                } else {
                    state = 0
                }
                if (headerBuffer.size() > 8192) break
            }

            val headersStr = headerBuffer.toString("ISO-8859-1")
            val lines = headersStr.split("\r\n")
            if (lines.isEmpty() || lines[0].isEmpty()) return@coroutineScope

            val firstLine = lines[0]
            val tokens = StringTokenizer(firstLine)
            if (tokens.countTokens() < 2) return@coroutineScope

            val method = tokens.nextToken()
            val url = tokens.nextToken()

            val isConnect = method.equals("CONNECT", ignoreCase = true)
            val (targetHost, targetPort) = if (isConnect) {
                parseHostAndPort(url, 443)
            } else {
                val tempUrl = when {
                    url.startsWith("http://", ignoreCase = true) -> url.substring(7)
                    url.startsWith("https://", ignoreCase = true) -> url.substring(8)
                    else -> {
                        val hostHeader = lines.firstOrNull { it.startsWith("Host:", ignoreCase = true) }
                            ?.substringAfter(':')
                            ?.trim()
                        hostHeader ?: ""
                    }
                }
                val slashIdx = tempUrl.indexOf('/')
                val hostPortPart = if (slashIdx != -1) tempUrl.substring(0, slashIdx) else tempUrl
                parseHostAndPort(hostPortPart, 80)
            }

            if (targetHost.isBlank()) {
                clientOut.write("HTTP/1.1 400 Bad Request\r\n\r\n".toByteArray())
                clientOut.flush()
                return@coroutineScope
            }

            try {
                socksSocket = connectViaSocks5(socksHost, socksPort, targetHost, targetPort)
            } catch (e: Exception) {
                LogManager.addLog("[Hotshare HTTP] Gagal koneksi SOCKS5 ke $targetHost:$targetPort - ${e.message}")
                if (isConnect) {
                    try {
                        clientOut.write("HTTP/1.1 502 Bad Gateway\r\n\r\n".toByteArray())
                        clientOut.flush()
                    } catch (ex: Exception) {
                        LogManager.addLog("[Hotshare HTTP] Failed to send 502 to client: ${ex.message}")
                    }
                }
                return@coroutineScope
            }

            socksSocket.tcpNoDelay = true
            socksSocket.keepAlive = true
            socksSocket.soTimeout = IDLE_TIMEOUT_MS
            socksSocket.receiveBufferSize = BUFFER_SIZE
            socksSocket.sendBufferSize = BUFFER_SIZE

            val socksOut = socksSocket.getOutputStream()
            val socksIn = socksSocket.getInputStream()

            if (isConnect) {
                clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                clientOut.flush()
            } else {
                socksOut.write(rewriteRequestHeaderToOriginForm(headersStr, url))
                socksOut.flush()
            }

            val bufferPool1 = ByteBuffer.allocateDirect(BUFFER_SIZE)
            val bufferPool2 = ByteBuffer.allocateDirect(BUFFER_SIZE)

            val clientToSocks = launch { 
                try {
                    pipeStream(clientIn, socksOut, bufferPool1)
                } catch (e: EOFException) {
                } catch (e: SocketException) {
                } catch (e: SocketTimeoutException) {
                    LogManager.addLog("[Hotshare HTTP] Client -> Socks Timeout: ${e.message}")
                } catch (e: IOException) {
                } catch (e: CancellationException) {
                }
            }
            val socksToClient = launch { 
                try {
                    pipeStream(socksIn, clientOut, bufferPool2)
                } catch (e: EOFException) {
                } catch (e: SocketException) {
                } catch (e: SocketTimeoutException) {
                    LogManager.addLog("[Hotshare HTTP] Socks -> Client Timeout: ${e.message}")
                } catch (e: IOException) {
                } catch (e: CancellationException) {
                }
            }

            clientToSocks.invokeOnCompletion { socksToClient.cancel() }
            socksToClient.invokeOnCompletion { clientToSocks.cancel() }

            joinAll(clientToSocks, socksToClient)

        } catch (e: CancellationException) {
            // Ignored
        } catch (e: SocketTimeoutException) {
            LogManager.addLog("[Hotshare HTTP] Koneksi Timeout (Idle): $clientIp")
        } catch (e: SocketException) {
            // LogManager.addLog("[Hotshare HTTP] Koneksi Socket terputus: $clientIp")
        } catch (e: EOFException) {
            // LogManager.addLog("[Hotshare HTTP] Koneksi EOF: $clientIp")
        } catch (e: Exception) {
            LogManager.addLog("[Hotshare HTTP] Error dari $clientIp: ${e.message}")
        } finally {
            gracefulClose(clientSocket)
            gracefulClose(socksSocket)
            activeConnections.decrementAndGet()
        }
    }

    private fun parseHostAndPort(hostPort: String, defaultPort: Int): Pair<String, Int> {
        val trimmed = hostPort.trim()
        if (trimmed.startsWith("[")) {
            val endBracket = trimmed.indexOf(']')
            if (endBracket > 0) {
                val host = trimmed.substring(1, endBracket)
                val port = trimmed.substring(endBracket + 1).removePrefix(":").toIntOrNull() ?: defaultPort
                return host to port.coerceIn(1, 65535)
            }
        }

        val colonIndex = trimmed.lastIndexOf(':')
        val hasSingleColon = colonIndex > 0 && trimmed.indexOf(':') == colonIndex
        if (hasSingleColon) {
            val host = trimmed.substring(0, colonIndex)
            val port = trimmed.substring(colonIndex + 1).toIntOrNull() ?: defaultPort
            return host to port.coerceIn(1, 65535)
        }

        return trimmed to defaultPort
    }

    private fun rewriteRequestHeaderToOriginForm(headers: String, originalUrl: String): ByteArray {
        if (!originalUrl.startsWith("http://", ignoreCase = true) && !originalUrl.startsWith("https://", ignoreCase = true)) {
            return headers.toByteArray(Charsets.ISO_8859_1)
        }

        val lines = headers.split("\r\n").toMutableList()
        if (lines.isNotEmpty()) {
            val firstLineTokens = StringTokenizer(lines[0])
            if (firstLineTokens.countTokens() >= 3) {
                val method = firstLineTokens.nextToken()
                val absoluteUrl = firstLineTokens.nextToken()
                val protocol = firstLineTokens.nextToken()
                val schemeOffset = absoluteUrl.indexOf("://")
                val pathStart = if (schemeOffset >= 0) absoluteUrl.indexOf('/', schemeOffset + 3) else -1
                val originPath = if (pathStart >= 0) absoluteUrl.substring(pathStart) else "/"
                lines[0] = "$method $originPath $protocol"
            }
        }

        return lines.joinToString("\r\n").toByteArray(Charsets.ISO_8859_1)
    }

    private fun connectViaSocks5(socksHost: String, socksPort: Int, targetHost: String, targetPort: Int): Socket {
        val socket = Socket()
        if (isTfoEnabled) {
            SocketUtils.enableTcpFastOpen(socket)
        }
        socket.connect(InetSocketAddress(socksHost, socksPort), CONNECT_TIMEOUT_MS)
        val out = DataOutputStream(socket.getOutputStream())
        val inStream = DataInputStream(socket.getInputStream())

        // SOCKS5 Handshake
        out.writeByte(0x05)
        out.writeByte(0x01)
        out.writeByte(0x00)
        out.flush()

        val ver = inStream.readByte().toInt()
        val method = inStream.readByte().toInt()
        if (ver != 5 || method != 0) {
            socket.close()
            throw IOException("SOCKS5 Shake failed")
        }

        // Connect Command
        out.writeByte(0x05)
        out.writeByte(0x01)
        out.writeByte(0x00)
        out.writeByte(0x03) // Domain name type
        val hostBytes = targetHost.toByteArray(Charsets.UTF_8)
        out.writeByte(hostBytes.size)
        out.write(hostBytes)
        out.writeShort(targetPort)
        out.flush()

        val rVer = inStream.readByte().toInt()
        val rRep = inStream.readByte().toInt()
        inStream.readByte()
        val rAtyp = inStream.readByte().toInt()

        if (rVer != 5 || rRep != 0) {
            socket.close()
            throw IOException("SOCKS5 Target Connect failed: $rRep")
        }

        when (rAtyp) {
            0x01 -> {
                val dummy = ByteArray(6)
                inStream.readFully(dummy)
            }
            0x03 -> {
                val len = inStream.readByte().toInt() and 0xFF
                val dummy = ByteArray(len + 2)
                inStream.readFully(dummy)
            }
            0x04 -> {
                val dummy = ByteArray(18)
                inStream.readFully(dummy)
            }
        }

        return socket
    }

    private fun pipeStream(input: InputStream, output: OutputStream, buffer: ByteBuffer) {
        var inputChannel: java.nio.channels.ReadableByteChannel? = null
        var outputChannel: java.nio.channels.WritableByteChannel? = null
        try {
            inputChannel = Channels.newChannel(input)
            outputChannel = Channels.newChannel(output)
            
            while (inputChannel.read(buffer) != -1) {
                buffer.flip()
                while (buffer.hasRemaining()) {
                    outputChannel.write(buffer)
                }
                buffer.clear()
            }
        } finally {
            // Do not close socket streams here; handled by gracefulClose in handleClient
        }
    }

    private fun gracefulClose(socket: Socket?) {
        if (socket == null || socket.isClosed) return
        try {
            if (!socket.isInputShutdown) socket.shutdownInput()
        } catch (e: Exception) {
            LogManager.addLog("Hotshare HTTP socket shutdownInput error: ${e.message}")
        }
        try {
            if (!socket.isOutputShutdown) socket.shutdownOutput()
        } catch (e: Exception) {
            LogManager.addLog("Hotshare HTTP socket shutdownOutput error: ${e.message}")
        }
        try {
            socket.close()
        } catch (e: Exception) {
            LogManager.addLog("Hotshare HTTP socket close error: ${e.message}")
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        closeServerSocket()
        
        val aJob = acceptJob
        acceptJob = null
        
        val jobsToCancel = synchronized(clientJobs) {
            clientJobs.toList()
        }
        
        scope.launch {
            aJob?.cancelAndJoin()
            jobsToCancel.forEach { it.cancel() }
            jobsToCancel.forEach { it.join() }
            
            LogManager.addLog("[Hotshare HTTP] Server dihentikan.")
            if (!LocalPortForwarder.isRunning) {
                HotshareWakeLockManager.release()
            }
        }
    }

    private fun closeServerSocket() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        } finally {
            serverSocket = null
        }
    }
}
