package com.sivpn.cepat.ssh
import com.sivpn.cepat.vpn.LogManager

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom

object WebSocketFramer {

    private const val BUFFER_SIZE = 32 * 1024

    fun forwardWsEncode(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        val encodedBuffer = ByteArray(BUFFER_SIZE + 16)
        val random = SecureRandom()
        val maskKey = ByteArray(4)
        
        while (true) {
            val read = input.read(buffer)
            if (read == -1) {
                random.nextBytes(maskKey)
                val closeFrame = byteArrayOf(0x88.toByte(), 0x80.toByte(), maskKey[0], maskKey[1], maskKey[2], maskKey[3])
                synchronized(output) {
                    try { 
                        output.write(closeFrame)
                        output.flush() 
                    } catch (e: Exception) {
                        LogManager.addLog("WS Encode close error: ${e.message}")
                    }
                }
                break
            }
            
            random.nextBytes(maskKey)
            
            var headerLen = 0
            encodedBuffer[headerLen++] = 0x82.toByte() // Binary frame
            
            if (read <= 125) {
                encodedBuffer[headerLen++] = (read or 0x80).toByte()
            } else {
                encodedBuffer[headerLen++] = (126 or 0x80).toByte()
                encodedBuffer[headerLen++] = ((read shr 8) and 0xFF).toByte()
                encodedBuffer[headerLen++] = (read and 0xFF).toByte()
            }
            
            encodedBuffer[headerLen++] = maskKey[0]
            encodedBuffer[headerLen++] = maskKey[1]
            encodedBuffer[headerLen++] = maskKey[2]
            encodedBuffer[headerLen++] = maskKey[3]
            
            for (i in 0 until read) {
                encodedBuffer[headerLen + i] = (buffer[i].toInt() xor maskKey[i % 4].toInt()).toByte()
            }
            
            synchronized(output) {
                output.write(encodedBuffer, 0, headerLen + read)
                output.flush()
            }
        }
    }

    fun forwardWsDecode(input: InputStream, outputToClient: OutputStream, outputToServer: OutputStream) {
        val dataInputStream = java.io.DataInputStream(input)
        val maskKey = ByteArray(4)
        
        while (true) {
            val b1 = dataInputStream.read()
            if (b1 == -1) break
            val b2 = dataInputStream.readUnsignedByte()
            
            val opcode = b1 and 0x0F
            val isMasked = (b2 and 0x80) != 0
            var payloadLength = (b2 and 0x7F).toLong()
            
            if (payloadLength == 126L) {
                payloadLength = dataInputStream.readUnsignedShort().toLong()
            } else if (payloadLength == 127L) {
                payloadLength = dataInputStream.readLong()
            }
            
            if (payloadLength > 10 * 1024 * 1024) throw IOException("WS Payload too large")
            
            if (isMasked) {
                dataInputStream.readFully(maskKey)
            }
            
            if (opcode == 0x0 || opcode == 0x1 || opcode == 0x2) {
                val chunkBuf = ByteArray(BUFFER_SIZE)
                var remaining = payloadLength
                var maskIndex = 0
                
                while (remaining > 0) {
                    val toRead = if (remaining > BUFFER_SIZE) BUFFER_SIZE else remaining.toInt()
                    dataInputStream.readFully(chunkBuf, 0, toRead)
                    
                    if (isMasked) {
                        for (i in 0 until toRead) {
                            chunkBuf[i] = (chunkBuf[i].toInt() xor maskKey[maskIndex % 4].toInt()).toByte()
                            maskIndex++
                        }
                    }
                    
                    outputToClient.write(chunkBuf, 0, toRead)
                    outputToClient.flush()
                    remaining -= toRead
                }
            } else {
                val payload = ByteArray(payloadLength.toInt())
                dataInputStream.readFully(payload)
                
                if (isMasked) {
                    for (i in payload.indices) {
                        payload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
                    }
                }
                
                when (opcode) {
                    0x8 -> {
                        val closeMKey = ByteArray(4)
                        SecureRandom().nextBytes(closeMKey)
                        val closeFrame = byteArrayOf(0x88.toByte(), 0x80.toByte(), closeMKey[0], closeMKey[1], closeMKey[2], closeMKey[3])
                        synchronized(outputToServer) {
                            try { 
                                outputToServer.write(closeFrame)
                                outputToServer.flush() 
                            } catch (e: Exception) {
                                LogManager.addLog("WS Decode close error: ${e.message}")
                            }
                        }
                        break
                    }
                    0x9 -> {
                        var headerLen = 0
                        val pongMKey = ByteArray(4)
                        SecureRandom().nextBytes(pongMKey)
                        
                        val pongBuf = ByteArray(16 + payload.size)
                        pongBuf[headerLen++] = 0x8A.toByte()
                        
                        if (payload.size <= 125) {
                            pongBuf[headerLen++] = (payload.size or 0x80).toByte()
                        } else if (payload.size <= 65535) {
                            pongBuf[headerLen++] = (126 or 0x80).toByte()
                            pongBuf[headerLen++] = ((payload.size shr 8) and 0xFF).toByte()
                            pongBuf[headerLen++] = (payload.size and 0xFF).toByte()
                        } else {
                            continue
                        }
                        
                        pongBuf[headerLen++] = pongMKey[0]
                        pongBuf[headerLen++] = pongMKey[1]
                        pongBuf[headerLen++] = pongMKey[2]
                        pongBuf[headerLen++] = pongMKey[3]
                        
                        for (i in payload.indices) {
                            pongBuf[headerLen + i] = (payload[i].toInt() xor pongMKey[i % 4].toInt()).toByte()
                        }
                        
                        synchronized(outputToServer) {
                            outputToServer.write(pongBuf, 0, headerLen + payload.size)
                            outputToServer.flush()
                        }
                    }
                }
            }
        }
    }
}
