package com.kheprix.util

import android.graphics.BitmapFactory
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

/**
 * Carrega uma imagem de uma URL em um [ImageView], usando cache local em SQLite.
 *
 * Fluxo:
 *   1. Se a URL estiver em [ImagemCacheDao], decodifica os bytes e exibe.
 *   2. Caso contrário, baixa via OkHttp, grava no cache e exibe.
 *   3. Em qualquer falha (URL nula, rede ou decodificação), mostra [placeholder].
 */
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

        val resolvido = resolverUrl(url)
        val context = target.context.applicationContext
        val dao = ImagemCacheDao(context)
        target.setImageResource(placeholder)

        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                dao.buscar(resolvido) ?: baixar(resolvido)?.also { dao.salvar(resolvido, it) }
            }
            val bmp = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            if (bmp != null) target.setImageBitmap(bmp)
        }
    }

    /**
     * Reescreve a URL para usar a [RetrofitClient.BASE_URL] quando:
     *   - a URL é relativa (começa com "/"); ou
     *   - é absoluta mas aponta para localhost/127.0.0.1 (caso comum quando
     *     BACKEND_URL não está setado no servidor).
     * Caso contrário, devolve a URL original.
     */
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
