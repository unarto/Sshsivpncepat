package com.sivpn.cepat.ssh
import com.sivpn.cepat.vpn.LogManager

import kotlinx.coroutines.*
import java.io.EOFException
import java.net.ConnectException
import com.sivpn.cepat.vpn.utils.SocketUtils
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.security.GeneralSecurityException
import java.security.cert.CertificateException
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

enum class TunnelMode {
    TCP,
    SSL,
    WS,
    WSS
}

object PayloadInjector {
    private var serverSocket: ServerSocket? = null
    @Volatile var isRunning = false
    var localPort = 0
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val clientJobs = mutableListOf<Job>()
    private val activeSockets = mutableSetOf<Socket>()
    private var acceptJob: Job? = null

    private const val BUFFER_SIZE = StreamForwarder.BUFFER_SIZE
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val HANDSHAKE_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 0 // Infinite for long-lived tunnel

    private var isTfoEnabled = false
    private var isAutoPingEnabled = false

    fun start(proxyHost: String, proxyPort: Int, sshHost: String, sshPort: Int, payload: String, sni: String, tlsVersion: String, tfoEnabled: Boolean = false, autoPingEnabled: Boolean = false) {
        val targetHost = if (proxyHost.isNotEmpty() && proxyPort > 0) proxyHost else sshHost
        val targetPort = if (proxyHost.isNotEmpty() && proxyPort > 0) proxyPort else sshPort

        if (targetHost.isBlank() || targetPort !in 1..65535) {
            LogManager.addLog("Local Proxy tidak dijalankan: target host/port tidak valid.")
            isRunning = false
            localPort = 0
            return
        }

        if (isRunning) {
            stop()
        }

        val bindLatch = CountDownLatch(1)
        localPort = 0
        isRunning = true
        isTfoEnabled = tfoEnabled
        isAutoPingEnabled = autoPingEnabled

        acceptJob = scope.launch {
            try {
                serverSocket = ServerSocket(0)
                serverSocket?.reuseAddress = true
                serverSocket?.soTimeout = 1000
                localPort = serverSocket?.localPort ?: 0
                LogManager.addLog("Local Proxy started on port $localPort")
                bindLatch.countDown()
                
                while (isRunning) {
                    var clientSocket: Socket? = null
                    try {
                        clientSocket = serverSocket?.accept()
                    } catch (e: SocketTimeoutException) {
                        continue
                    } catch (e: SocketException) {
                        break
                    } catch (e: Exception) {
                        break
                    }

                    if (clientSocket != null) {
                        registerSocket(clientSocket)
                        val job = launch {
                            handleClient(clientSocket, targetHost, targetPort, sshHost, sshPort, payload, sni, tlsVersion, isAutoPingEnabled)
                        }
                        synchronized(clientJobs) {
                            clientJobs.add(job)
                        }
                        job.invokeOnCompletion {
                            synchronized(clientJobs) {
                                clientJobs.remove(job)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    LogManager.addLog("Local Proxy error: ${e.message}")
                }
                isRunning = false
                localPort = 0
                bindLatch.countDown()
            } finally {
                isRunning = false
            }
        }

        if (!bindLatch.await(2, TimeUnit.SECONDS) || localPort == 0) {
            LogManager.addLog("Local Proxy gagal bind port tepat waktu.")
            stop()
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            LogManager.addLog("PayloadInjector serverSocket close error: ${e.message}")
        }
        serverSocket = null
        
        val aJob = acceptJob
        acceptJob = null

        val socketsToClose = synchronized(activeSockets) {
            val list = activeSockets.toList()
            activeSockets.clear()
            list
        }
        socketsToClose.forEach {
            StreamForwarder.gracefulClose(it)
        }
        
        val jobsToCancel = synchronized(clientJobs) {
            val list = clientJobs.toList()
            clientJobs.clear()
            list
        }
        
        scope.launch {
            aJob?.cancelAndJoin()
            jobsToCancel.forEach { it.cancel() }
            jobsToCancel.forEach { it.join() }
        }
    }

    private fun registerSocket(socket: Socket) {
        synchronized(activeSockets) {
            activeSockets.add(socket)
        }
    }

    private fun unregisterSocket(socket: Socket?) {
        if (socket == null) return
        synchronized(activeSockets) {
            activeSockets.remove(socket)
        }
    }

    private fun determineMode(payload: String, sni: String): TunnelMode {
        val isSsl = sni.isNotBlank()
        val lowerPayload = payload.lowercase(Locale.ENGLISH)

        val hasUpgradeHeader = lowerPayload.contains("upgrade:") && lowerPayload.contains("websocket")
        val hasConnectionHeader = lowerPayload.contains("connection:") && lowerPayload.contains("upgrade")
        val hasWsKey = lowerPayload.contains("sec-websocket-key") || lowerPayload.contains("[websocket_key]")
        val hasWsTags = lowerPayload.contains("[websocket_version]") || lowerPayload.contains("[websocket_extensions]")

        val isWs = (hasUpgradeHeader && hasConnectionHeader) || hasWsKey || hasWsTags

        return when {
            isSsl && isWs -> TunnelMode.WSS
            isSsl -> TunnelMode.SSL
            isWs -> TunnelMode.WS
            else -> TunnelMode.TCP
        }
    }

    private suspend fun handleClient(
        clientSocket: Socket,
        remoteHost: String,
        remotePort: Int,
        sshHost: String,
        sshPort: Int,
        payload: String,
        sni: String,
        tlsVersion: String,
        autoPingEnabled: Boolean
    ) = coroutineScope {
        var remoteSocket: Socket? = null
        var autoPingJob: Job? = null
        try {
            val mode = determineMode(payload, sni)
            LogManager.addLog("Injector Mode: $mode")

            clientSocket.tcpNoDelay = true
            clientSocket.keepAlive = true
            clientSocket.soTimeout = READ_TIMEOUT_MS
            clientSocket.receiveBufferSize = BUFFER_SIZE
            clientSocket.sendBufferSize = BUFFER_SIZE

            var connected = false
            var retryCount = 0
            val maxRetries = 3
            var currentDelay = 1000L

            while (!connected && retryCount < maxRetries && isActive) {
                try {
                    val sock = Socket()
                    sock.tcpNoDelay = true
                    sock.keepAlive = true
                    sock.soTimeout = HANDSHAKE_TIMEOUT_MS
                    sock.receiveBufferSize = BUFFER_SIZE
                    sock.sendBufferSize = BUFFER_SIZE
                    if (isTfoEnabled && (payload.isNotEmpty() || sni.isNotEmpty())) {
                        SocketUtils.enableTcpFastOpen(sock)
                    }
                    sock.connect(InetSocketAddress(remoteHost, remotePort), CONNECT_TIMEOUT_MS)

                    LogManager.addLog("TCP Connected to $remoteHost:$remotePort")

                    if (mode == TunnelMode.SSL || mode == TunnelMode.WSS) {
                        val sslSocket = SslEngine.connect(sock, remoteHost, remotePort, sni, tlsVersion)
                        sslSocket.soTimeout = READ_TIMEOUT_MS
                        remoteSocket = sslSocket
                        registerSocket(sslSocket)
                    } else {
                        sock.soTimeout = READ_TIMEOUT_MS
                        remoteSocket = sock
                        registerSocket(sock)
                    }
                    connected = true
                } catch (e: Exception) {
                    try { remoteSocket?.close() } catch (ex: Exception) {}
                    if (remoteSocket != null) {
                        unregisterSocket(remoteSocket)
                        remoteSocket = null
                    }

                    val isSslError = e is SSLException ||
                            e is SSLHandshakeException ||
                            e is CertificateException ||
                            e is GeneralSecurityException

                    val isRetryable = !isSslError && (
                            e is SocketTimeoutException ||
                            e is ConnectException ||
                            e is SocketException ||
                            e is EOFException
                    )

                    if (!isRetryable) {
                        throw e
                    }

                    retryCount++
                    if (retryCount >= maxRetries) {
                        LogManager.addLog("Handshake Failed setelah $maxRetries percobaan.")
                        throw Exception("Koneksi gagal: ${e.message}", e)
                    }
                    LogManager.addLog("Retry ($retryCount/$maxRetries) karena: ${e.message}")
                    delay(currentDelay)
                    currentDelay *= 2
                }
            }

            val connectedSocket = remoteSocket ?: throw IllegalStateException("Remote socket belum siap")
            val remoteOut = connectedSocket.outputStream
            val remoteIn = connectedSocket.inputStream

            val isDirectConnection = (remoteHost == sshHost && remotePort == sshPort)
            val shouldSendPayload = payload.isNotEmpty() || (!isDirectConnection)

            if (shouldSendPayload) {
                val formattedPayloadChunks = HttpPayloadEngine.formatPayloadChunks(payload, sshHost, sshPort)

                if (mode == TunnelMode.WS || mode == TunnelMode.WSS) {
                    val (finalPayloadChunks, secWsKey) = WebSocketEngine.prepareWsPayloadChunks(formattedPayloadChunks)
                    
                    HttpPayloadEngine.injectPayloadChunks(finalPayloadChunks, remoteOut)
                    
                    connectedSocket.soTimeout = HANDSHAKE_TIMEOUT_MS
                    val handshakeOk = WebSocketEngine.readWsHandshakeResponse(remoteIn, secWsKey)
                    connectedSocket.soTimeout = READ_TIMEOUT_MS

                    if (!handshakeOk) {
                        throw Exception("Invalid WebSocket Handshake Response (Gagal verifikasi RFC 6455)")
                    }
                    LogManager.addLog("WS Connected (Handshake 101 sukses)")
                    
                    if (autoPingEnabled) {
                        autoPingJob = WebSocketEngine.startAutoPing(remoteOut, this)
                    }
                } else {
                    HttpPayloadEngine.injectPayloadChunks(formattedPayloadChunks, remoteOut)
                    
                    connectedSocket.soTimeout = HANDSHAKE_TIMEOUT_MS
                    val rawResponse = HttpPayloadEngine.readResponse(remoteIn)
                    connectedSocket.soTimeout = READ_TIMEOUT_MS

                    if (rawResponse.isNotEmpty()) {
                        val parsedResponse = HttpPayloadEngine.parseResponse(rawResponse)
                        if (!HttpPayloadEngine.validateResponse(parsedResponse)) {
                            throw Exception("Proxy Handshake Failed: ${parsedResponse.statusLine}")
                        }
                        LogManager.addLog("Proxy Connected (Handshake ${parsedResponse.statusCode} sukses)")
                    } else {
                        LogManager.addLog("Warning: No response from proxy, continuing anyway.")
                    }
                }
            }

            if (mode == TunnelMode.WS || mode == TunnelMode.WSS) {
                val clientToRemote = launch { 
                    try {
                        WebSocketEngine.forwardWsEncode(clientSocket.inputStream, remoteOut)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // ignore IO break
                    }
                }
                val remoteToClient = launch { 
                    try {
                        WebSocketEngine.forwardWsDecode(remoteIn, clientSocket.outputStream, remoteOut)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // ignore IO break
                    }
                }
                
                clientToRemote.invokeOnCompletion { 
                    remoteToClient.cancel()
                    StreamForwarder.gracefulClose(clientSocket)
                    StreamForwarder.gracefulClose(connectedSocket)
                }
                remoteToClient.invokeOnCompletion { 
                    clientToRemote.cancel()
                    StreamForwarder.gracefulClose(clientSocket)
                    StreamForwarder.gracefulClose(connectedSocket)
                }
                
                joinAll(clientToRemote, remoteToClient)
                LogManager.addLog("WS Closed")
            } else {
                val buffer1 = ByteArray(BUFFER_SIZE)
                val buffer2 = ByteArray(BUFFER_SIZE)

                val clientToRemote = launch { 
                    try {
                        StreamForwarder.forwardStream(clientSocket.inputStream, remoteOut, buffer1)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // ignore IO break
                    }
                }
                val remoteToClient = launch { 
                    try {
                        StreamForwarder.forwardStream(remoteIn, clientSocket.outputStream, buffer2)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // ignore IO break
                    }
                }
                
                clientToRemote.invokeOnCompletion { 
                    remoteToClient.cancel()
                    StreamForwarder.gracefulClose(clientSocket)
                    StreamForwarder.gracefulClose(connectedSocket)
                }
                remoteToClient.invokeOnCompletion { 
                    clientToRemote.cancel()
                    StreamForwarder.gracefulClose(clientSocket)
                    StreamForwarder.gracefulClose(connectedSocket)
                }
                
                joinAll(clientToRemote, remoteToClient)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogManager.addLog("PayloadInjector error: ${e.message}")
        } finally {
            autoPingJob?.cancel()
            unregisterSocket(clientSocket)
            unregisterSocket(remoteSocket)
            StreamForwarder.gracefulClose(clientSocket)
            StreamForwarder.gracefulClose(remoteSocket)
        }
    }
}
