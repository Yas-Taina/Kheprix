package com.kheprix.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object PhotoUtils {
    fun uriToBase64(context: Context, uri: Uri, maxWidth: Int = 1024): String? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return null

            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val scaled = scaleBitmap(original, maxWidth)
            "data:image/jpeg;base64,${bitmapToBase64(scaled)}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val payload = if (base64.startsWith("data:")) base64.substringAfter(",", "") else base64
            if (payload.isEmpty()) return null
            val bytes = Base64.decode(payload, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun rotarBase64(base64: String, graus: Float = 90f): String? {
        val bmp = base64ToBitmap(base64) ?: return null
        val matrix = android.graphics.Matrix().apply { postRotate(graus) }
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        return "data:image/jpeg;base64,${bitmapToBase64(rotated)}"
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width.toFloat()
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }
}
