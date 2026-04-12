package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityLoginBinding
import com.kheprix.models.LoginRequest
import kotlinx.coroutines.launch

/**
 * Tela de Login.
 *
 * Fluxo:
 *  1. Usuário preenche e-mail + senha
 *  2. POST /autenticacao/login → token salvo via SessionManager
 *  3. Redireciona para HomeActivity (a criar)
 *
 * Links:
 *  - "aqui" (cadastro) → CadastroActivity
 *  - "Esqueci minha senha" → RecuperacaoSenhaActivity
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navegar para cadastro
        binding.tvLinkCadastro.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }

        // Navegar para recuperação de senha
        binding.tvEsqueciSenha.setOnClickListener {
            startActivity(Intent(this, RecuperacaoSenhaActivity::class.java))
        }

        // Confirmar login
        binding.btnConfirmar.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val senha = binding.etSenha.text.toString()

            if (!validarCampos(email, senha)) return@setOnClickListener

            realizarLogin(email, senha)
        }
    }

    private fun validarCampos(email: String, senha: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Informe o e-mail"
            return false
        }
        if (senha.isEmpty()) {
            binding.etSenha.error = "Informe a senha"
            return false
        }
        return true
    }

    private fun realizarLogin(email: String, senha: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(email, senha))

                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token != null) {
                        SessionManager.saveToken(token)
                        // Persiste nome+email para o PerfilActivity
                        // O endpoint de login só retorna token; buscamos os dados
                        // do usuário via autocadastro response ou armazenamos o email
                        SessionManager.saveUser(
                            id    = 0,  // será sobrescrito se o app fizer GET /me
                            name  = binding.etEmail.text.toString().split("@").first(),
                            email = binding.etEmail.text.toString()
                        )
                        irParaHome()
                    } else {
                        mostrarErro("Resposta inválida do servidor")
                    }
                } else {
                    when (response.code()) {
                        401 -> mostrarErro("E-mail ou senha incorretos")
                        422 -> mostrarErro("Dados inválidos. Verifique e tente novamente")
                        else -> mostrarErro("Erro ${response.code()}. Tente novamente")
                    }
                }
            } catch (e: Exception) {
                mostrarErro("Falha na conexão. Verifique sua internet")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun irParaHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }

    private fun mostrarErro(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
