package com.sivpn.cepat.ssh
import com.sivpn.cepat.vpn.LogManager

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

object StreamForwarder {
    const val BUFFER_SIZE = 32 * 1024

    fun forwardStream(input: InputStream, output: OutputStream, buffer: ByteArray) {
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            synchronized(output) {
                output.write(buffer, 0, bytesRead)
                output.flush()
            }
        }
    }

    fun gracefulClose(socket: Socket?) {
        if (socket == null || socket.isClosed) return
        try {
            if (socket.isConnected && !socket.isInputShutdown) {
                socket.shutdownInput()
            }
        } catch (e: Exception) {
            LogManager.addLog("StreamForwarder shutdownInput error: ${e.message}")
        }
        try {
            if (socket.isConnected && !socket.isOutputShutdown) {
                socket.shutdownOutput()
            }
        } catch (e: Exception) {
            LogManager.addLog("StreamForwarder shutdownOutput error: ${e.message}")
        }
        try {
            socket.close()
        } catch (e: Exception) {
            LogManager.addLog("StreamForwarder close error: ${e.message}")
        }
    }
}
