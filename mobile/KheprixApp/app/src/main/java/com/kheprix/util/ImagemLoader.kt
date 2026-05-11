package com.kheprix.util

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.kheprix.api.RetrofitClient
import com.kheprix.db.ImagemCacheDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

object ImagemLoader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun load(
        scope: CoroutineScope,
        target: ImageView,
        url: String?,
        @DrawableRes placeholder: Int
    ) {
        if (url.isNullOrBlank()) {
            target.setImageResource(placeholder)
            return
        }

        target.setImageResource(placeholder)

        val ehBase64 = url.startsWith("data:") ||
            (!url.startsWith("http://") && !url.startsWith("https://") && url.length > 500)
        val ehUrl = !ehBase64

        if (!ehUrl) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { decodeBase64(url) }
                if (bmp != null) target.setImageBitmap(bmp)
            }
            return
        }

        val resolvido = resolverUrl(url)
        val context = target.context.applicationContext
        val dao = ImagemCacheDao(context)

        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                dao.buscar(resolvido) ?: baixar(resolvido)?.also { dao.salvar(resolvido, it) }
            }
            val bmp = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            if (bmp != null) target.setImageBitmap(bmp)
        }
    }

    private fun decodeBase64(raw: String): android.graphics.Bitmap? {
        val payload = if (raw.startsWith("data:")) raw.substringAfter(",", "") else raw
        if (payload.isEmpty()) return null
        return try {
            val bytes = Base64.decode(payload, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun resolverUrl(url: String): String {
        val base = RetrofitClient.BASE_URL.trimEnd('/')
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            val path = if (url.startsWith("/")) url else "/$url"
            return "$base$path"
        }
        return try {
            val original = URI(url)
            if (original.host == "localhost" || original.host == "127.0.0.1") {
                val b = URI(base)
                URI(b.scheme, null, b.host, b.port, original.path, original.query, original.fragment).toString()
            } else url
        } catch (_: Exception) {
            url
        }
    }

    private fun baixar(url: String): ByteArray? {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ImagemLoader", "HTTP ${response.code} ao baixar $url")
                    return null
                }
                response.body?.bytes()
            }
        } catch (e: Exception) {
            Log.w("ImagemLoader", "Falha ao baixar $url", e)
            null
        }
    }
}
