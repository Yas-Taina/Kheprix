package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.kheprix.R
import com.kheprix.api.SessionManager

/**
 * SplashActivity — primeira tela exibida ao abrir o app.
 *
 * Exibe o logo Kheprix por 2 segundos e depois redireciona:
 *  - Para HomeActivity se já existe sessão salva
 *  - Para MainActivity (tela de boas-vindas) se não há sessão
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val next = if (SessionManager.isLoggedIn()) {
                Intent(this, HomeActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java)
            }
            startActivity(next)
            finish()
        }, 2000)
    }
}
