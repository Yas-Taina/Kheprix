package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kheprix.R
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityMainBinding

/**
 * Tela inicial do app (splash/welcome).
 * Se o usuário já estiver logado, redireciona direto para a HomeActivity (a criar).
 *
 * PLACEHOLDER – substituir depois:
 *  - Logo: troque R.drawable.ic_placeholder_beetle pelo seu drawable real (ex: R.drawable.kheprix_beetle)
 *  - Fonte: veja res/font/ e aplique via textAppearance ou TypefaceCompat
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se já logado, pula direto para home
        if (SessionManager.isLoggedIn()) {
            goToHome()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnCadastro.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
