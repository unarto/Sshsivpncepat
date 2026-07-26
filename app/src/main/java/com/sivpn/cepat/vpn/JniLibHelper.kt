package com.sivpn.cepat.vpn

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object JniLibHelper {
    private const val TAG = "JniLibHelper"

    fun getAbiDirName(): String {
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        // Standardize naming
        return when {
            primaryAbi.startsWith("arm64") -> "arm64-v8a"
            primaryAbi.startsWith("armeabi-v7") -> "armeabi-v7a"
            primaryAbi.startsWith("x86_64") -> "x86_64"
            primaryAbi.startsWith("x86") -> "x86"
            else -> primaryAbi
        }
    }

    fun getJniLibsDir(context: Context): File {
        val dir = File(context.filesDir, "jniLibs/${getAbiDirName()}")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun loadDownloadedLibs(context: Context): Boolean {
        // Coba memuat library yang sudah dibundel di dalam APK (otomatis dimuat sistem ke nativeLibraryDir)
        var someLoaded = false
        
        try {
            if (!NativeSshTunnel.isLibraryLoaded) {
                System.loadLibrary("ssh")
                NativeSshTunnel.isLibraryLoaded = true
                someLoaded = true
                Log.d(TAG, "Berhasil memuat libssh.so secara statis dari APK.")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Gagal memuat libssh.so secara statis, mencoba metode dinamis...", e)
        }

        try {
            if (!com.sivpn.cepat.TProxyService.isLibraryLoaded) {
                System.loadLibrary("hev-socks5-tunnel")
                com.sivpn.cepat.TProxyService.isLibraryLoaded = true
                someLoaded = true
                Log.d(TAG, "Berhasil memuat libhev-socks5-tunnel.so secara statis dari APK.")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Gagal memuat libhev-socks5-tunnel.so secara statis, mencoba metode dinamis...", e)
        }

        // Jika statis gagal, coba metode dinamis (hasil unduhan)
        val jniLibsDir = getJniLibsDir(context)

        val sshFile = File(jniLibsDir, "libssh.so")
        if (sshFile.exists() && !NativeSshTunnel.isLibraryLoaded) {
            Log.d(TAG, "Mencoba memuat libssh.so hasil unduhan dari: ${sshFile.absolutePath}")
            val success = NativeSshTunnel.loadFromPath(sshFile.absolutePath)
            if (success) {
                someLoaded = true
                Log.d(TAG, "Berhasil memuat libssh.so secara dinamis.")
            }
        }

        val hevFile = File(jniLibsDir, "libhev-socks5-tunnel.so")
        if (hevFile.exists() && !com.sivpn.cepat.TProxyService.isLibraryLoaded) {
            Log.d(TAG, "Mencoba memuat libhev-socks5-tunnel.so hasil unduhan dari: ${hevFile.absolutePath}")
            val success = com.sivpn.cepat.TProxyService.loadFromPath(hevFile.absolutePath)
            if (success) {
                someLoaded = true
                Log.d(TAG, "Berhasil memuat libhev-socks5-tunnel.so secara dinamis.")
            }
        }
        return someLoaded
    }

    fun cleanDownloadedLibs(context: Context) {
        val jniLibsDir = getJniLibsDir(context)
        if (jniLibsDir.exists()) {
            jniLibsDir.listFiles()?.forEach { file ->
                file.delete()
            }
        }
    }

    suspend fun downloadNativeLibrary(
        context: Context,
        urlStr: String,
        fileName: String,
        onProgress: (progress: Float, byteCount: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 20000
        connection.readTimeout = 20000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Gagal mengunduh $fileName\nStatus HTTP: ${connection.responseCode} ${connection.responseMessage}")
        }

        val fileLength = connection.contentLength.toLong()
        val jniLibsDir = getJniLibsDir(context)
        val tempFile = File(jniLibsDir, "$fileName.tmp")
        val finalFile = File(jniLibsDir, fileName)

        if (tempFile.exists()) {
            tempFile.delete()
        }

        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(tempFile).use { output ->
                val data = ByteArray(1024 * 16)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    output.write(data, 0, count)
                    if (fileLength > 0) {
                        onProgress(total.toFloat() / fileLength, total, fileLength)
                    } else {
                        onProgress(-1f, total, -1)
                    }
                }
                output.flush()
            }
        }

        if (finalFile.exists()) {
            finalFile.delete()
        }
        if (!tempFile.renameTo(finalFile)) {
            throw Exception("Gagal merubah nama file temporer ke $fileName")
        }
        finalFile
    }
}
