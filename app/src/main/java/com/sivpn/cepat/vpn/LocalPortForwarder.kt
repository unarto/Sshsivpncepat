package com.sivpn.cepat.vpn

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.concurrent.atomic.AtomicInteger

object LocalPortForwarder {
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
    var activePort = 1080
        private set

    fun start(context: android.content.Context, localBindPort: Int = 1080, targetHost: String = "127.0.0.1", targetPort: Int = 1080) {
        if (targetHost.isBlank() || targetPort !in 1..65535) {
            LogManager.addLog("[Hotshare] Target SOCKS tidak valid, server tidak dijalankan.")
            isRunning = false
            return
        }

        if (isRunning) {
            stop()
        }

        isRunning = true
        acceptJob = scope.launch {
            var portToTry = localBindPort
            var success = false
            
            val startPort = localBindPort.coerceIn(1, 65535)
            val maxPort = (startPort + 10).coerceAtMost(65535)
            portToTry = startPort

            while (portToTry <= maxPort && !success && isRunning) {
                try {
                    serverSocket = ServerSocket()
                    serverSocket?.reuseAddress = true
                    serverSocket?.bind(InetSocketAddress("0.0.0.0", portToTry))
                    serverSocket?.soTimeout = 1000 // Timeout untuk memeriksa isRunning secara berkala
                    activePort = portToTry
                    success = true
                    LogManager.addLog("[Hotshare] Server SOCKS aktif di 0.0.0.0:$activePort -> meneruskan ke $targetHost:$targetPort")
                    HotshareWakeLockManager.acquire(context)
                } catch (e: Exception) {
                    LogManager.addLog("[Hotshare] Gagal menggunakan port $portToTry: ${e.message}. Mencoba port berikutnya...")
                    portToTry++
                }
            }

            if (!success) {
                LogManager.addLog("[Hotshare] Gagal memulai server Hotshare SOCKS pada rentang port $localBindPort-$maxPort.")
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
                            LogManager.addLog("[Hotshare] Accept error: ${e.message}")
                        }
                        break
                    } catch (e: Exception) {
                        break
                    }

                    if (clientSocket != null) {
                        if (connectionSemaphore.tryAcquire()) {
                            val job = launch {
                                try {
                                    handleClient(clientSocket, targetHost, targetPort)
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
                            LogManager.addLog("[Hotshare] Batas maksimum 100 koneksi tercapai. Menolak klien baru.")
                            gracefulClose(clientSocket)
                        }
                    }
                }
            } finally {
                isRunning = false
                closeServerSocket()
                if (!HttpProxyServer.isRunning) {
                    HotshareWakeLockManager.release()
                }
            }
        }
    }

    private suspend fun handleClient(clientSocket: Socket, targetHost: String, targetPort: Int) = coroutineScope {
        val startTime = System.currentTimeMillis()
        val destinationSocket = Socket()
        val clientIp = clientSocket.inetAddress?.hostAddress ?: "unknown"
        val count = activeConnections.incrementAndGet()
        
        LogManager.addLog("[Hotshare] Client Connected: $clientIp (Active: $count)")

        try {
            HotshareClientManager.registerClientActivity(clientIp)
            
            clientSocket.tcpNoDelay = true
            clientSocket.keepAlive = true
            clientSocket.soTimeout = IDLE_TIMEOUT_MS
            clientSocket.receiveBufferSize = BUFFER_SIZE
            clientSocket.sendBufferSize = BUFFER_SIZE

            destinationSocket.reuseAddress = true
            destinationSocket.connect(InetSocketAddress(targetHost, targetPort), CONNECT_TIMEOUT_MS)
            
            destinationSocket.tcpNoDelay = true
            destinationSocket.keepAlive = true
            destinationSocket.soTimeout = IDLE_TIMEOUT_MS
            destinationSocket.receiveBufferSize = BUFFER_SIZE
            destinationSocket.sendBufferSize = BUFFER_SIZE

            LogManager.addLog("[Hotshare] Destination Connected: $targetHost:$targetPort for $clientIp")

            val bufferPool1 = ByteBuffer.allocateDirect(BUFFER_SIZE)
            val bufferPool2 = ByteBuffer.allocateDirect(BUFFER_SIZE)

            val clientToDest = launch {
                try {
                    pipeStream(clientSocket.getInputStream(), destinationSocket.getOutputStream(), bufferPool1)
                } catch (e: EOFException) {
                    // Ignore EOF
                } catch (e: SocketException) {
                    // Ignore closed socket
                } catch (e: SocketTimeoutException) {
                    LogManager.addLog("[Hotshare] Client -> Dest Timeout: ${e.message}")
                } catch (e: IOException) {
                    LogManager.addLog("[Hotshare] Client -> Dest Error: ${e.message}")
                } catch (e: CancellationException) {
                    // Ignored
                }
            }

            val destToClient = launch {
                try {
                    pipeStream(destinationSocket.getInputStream(), clientSocket.getOutputStream(), bufferPool2)
                } catch (e: EOFException) {
                    // Ignore EOF
                } catch (e: SocketException) {
                    // Ignore closed socket
                } catch (e: SocketTimeoutException) {
                    LogManager.addLog("[Hotshare] Dest -> Client Timeout: ${e.message}")
                } catch (e: IOException) {
                    LogManager.addLog("[Hotshare] Dest -> Client Error: ${e.message}")
                } catch (e: CancellationException) {
                    // Ignored
                }
            }

            clientToDest.invokeOnCompletion {
                destToClient.cancel()
            }
            
            destToClient.invokeOnCompletion {
                clientToDest.cancel()
            }

            joinAll(clientToDest, destToClient)

        } catch (e: CancellationException) {
            // Ignored
        } catch (e: SocketTimeoutException) {
            LogManager.addLog("[Hotshare] Koneksi Timeout (Idle): $clientIp")
        } catch (e: SocketException) {
            LogManager.addLog("[Hotshare] Koneksi Socket terputus: $clientIp")
        } catch (e: EOFException) {
            LogManager.addLog("[Hotshare] Koneksi EOF: $clientIp")
        } catch (e: IOException) {
            LogManager.addLog("[Hotshare] I/O Error dari $clientIp: ${e.message}")
        } catch (e: Exception) {
            LogManager.addLog("[Hotshare] Error dari $clientIp: ${e.message}")
        } finally {
            gracefulClose(clientSocket)
            gracefulClose(destinationSocket)
            
            val duration = System.currentTimeMillis() - startTime
            val remaining = activeConnections.decrementAndGet()
            LogManager.addLog("[Hotshare] Client Disconnected: $clientIp (Duration: ${duration}ms, Active: $remaining)")
            LogManager.addLog("[Hotshare] Destination Closed: $targetHost:$targetPort for $clientIp")
        }
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
            // Biarkan penutupan stream/socket dilakukan di handleClient secara graceful
        }
    }

    private fun gracefulClose(socket: Socket?) {
        if (socket == null || socket.isClosed) return
        try {
            if (!socket.isInputShutdown) socket.shutdownInput()
        } catch (e: Exception) {
            LogManager.addLog("Hotshare socket shutdownInput error: ${e.message}")
        }
        try {
            if (!socket.isOutputShutdown) socket.shutdownOutput()
        } catch (e: Exception) {
            LogManager.addLog("Hotshare socket shutdownOutput error: ${e.message}")
        }
        try {
            socket.close()
        } catch (e: Exception) {
            LogManager.addLog("Hotshare socket close error: ${e.message}")
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
            
            LogManager.addLog("[Hotshare] Server dihentikan.")
            if (!HttpProxyServer.isRunning) {
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
