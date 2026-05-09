package com.kheprix.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Utility functions for converting images to/from Base64 strings.
 *
 * The API transmits photos as Base64-encoded strings. Use these helpers
 * whenever you need to prepare an image for upload or render a received one.
 */
object PhotoUtils {

    /**
     * Converts a [Uri] (e.g. from a gallery picker or camera) to a Base64 string.
     *
     * @param context  Application or Activity context.
     * @param uri      Content URI of the image.
     * @param maxWidth Optional max width to scale the bitmap before encoding (saves bandwidth).
     * @return Base64 string, or null if the conversion fails.
     */
    fun uriToBase64(context: Context, uri: Uri, maxWidth: Int = 1024): String? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return null

            // Decode full bitmap then scale it
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val scaled = scaleBitmap(original, maxWidth)
            // Prefix with data URI scheme so the string is unambiguously Base64
            // and never confused with a relative URL path (raw JPEG Base64 starts
            // with "/9j/" which would otherwise be misidentified as a URL path).
            "data:image/jpeg;base64,${bitmapToBase64(scaled)}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a [Bitmap] to a Base64-encoded JPEG string (no data URI prefix).
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Decodes a Base64 string back to a [Bitmap] for display in an ImageView.
     * Accepts both raw Base64 and data URI format ("data:image/...;base64,...").
     *
     * @return Decoded [Bitmap], or null if the string is invalid.
     */
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

    /**
     * Scales a bitmap so that its width does not exceed [maxWidth],
     * maintaining the original aspect ratio.
     */
    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width.toFloat()
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }
}
