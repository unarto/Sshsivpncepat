package com.sivpn.cepat.vpn

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QrCodeGenerator {
    /**
     * Generates a QR Code Bitmap from the provided text.
     * Adaptive colors conforming to scan rules (high contrast is required for scan reliability).
     */
    fun generateQr(text: String, size: Int = 512, darkColor: Int = Color.BLACK, lightColor: Int = Color.WHITE): Bitmap? {
        if (text.isBlank() || size <= 0) return null
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) darkColor else lightColor)
                }
            }
            bitmap
        } catch (e: Exception) {
            LogManager.addLog("Gagal membuat QR Code: ${e.message}")
            null
        }
    }
}
