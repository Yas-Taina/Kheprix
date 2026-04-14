package com.kheprix.api

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton that provides the configured Retrofit [ApiService].
 *
 * The base URL points to the local development server (localhost:3000).
 * On a physical device, replace 10.0.2.2 with your machine's LAN IP.
 */
object RetrofitClient {

    // 10.0.2.2 is the Android emulator alias for the host machine's localhost.
    // For a physical device on the same Wi-Fi, use your machine's IP, e.g. 192.168.1.X
    private const val BASE_URL = "http://192.168.192.122:3000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

/**
 * Manages the JWT token and user session using SharedPreferences.
 *
 * Usage:
 *   SessionManager.init(context)         // call once in Application.onCreate()
 *   SessionManager.saveToken("...")
 *   SessionManager.getAuthHeader()       // returns "Bearer <token>"
 *   SessionManager.logout()
 */
object SessionManager {

    private const val PREFS_NAME = "session_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── Token ──────────────────────────────────────────────────────────────

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    /**
     * Returns the Bearer header value ready to be passed to Retrofit, e.g.
     * "Bearer eyJhbGciOiJIUzI1Ni..."
     * Returns an empty string if no token is stored (unauthenticated state).
     */
    fun getAuthHeader(): String {
        val token = getToken() ?: return ""
        return "Bearer $token"
    }

    fun isLoggedIn(): Boolean = getToken() != null

    // ── User info ──────────────────────────────────────────────────────────

    fun saveUser(id: Int, name: String, email: String) {
        prefs.edit()
            .putInt(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    // ── Logout ─────────────────────────────────────────────────────────────

    /**
     * Clears all session data from SharedPreferences.
     * Call this on logout; then navigate the user to the login screen.
     *
     * Note: this does NOT delete local offline study data from SQLite.
     * Call [com.kheprix.db.EstudoOfflineManager.deletarDadosEstudo] separately
     * if you also want to wipe offline data.
     */
    fun logout() {
        prefs.edit().clear().apply()
    }
}
