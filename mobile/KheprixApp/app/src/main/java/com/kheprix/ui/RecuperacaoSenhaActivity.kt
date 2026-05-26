package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kheprix.api.RetrofitClient
import com.kheprix.databinding.ActivityRecuperacaoSenhaBinding
import com.kheprix.models.EmailRequest
import com.kheprix.models.RedefinirSenhaRequest
import com.kheprix.models.TokenRequest
import kotlinx.coroutines.launch

class RecuperacaoSenhaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecuperacaoSenhaBinding

    private var tokenValidado: String = ""

    companion object {
        private const val STEP_EMAIL = 0
        private const val STEP_TOKEN = 1
        private const val STEP_NOVA_SENHA = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecuperacaoSenhaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mostrarEtapa(STEP_EMAIL)

        binding.btnEnviarToken.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.etEmail.error = "Informe o e-mail"
                return@setOnClickListener
            }
            solicitarToken(email)
        }

        binding.btnConfirmarToken.setOnClickListener {
            val token = binding.etToken.text.toString().trim()
            if (token.isEmpty()) {
                binding.etToken.error = "Informe o token recebido"
                return@setOnClickListener
            }
            validarToken(token)
        }

        binding.btnRedefinirSenha.setOnClickListener {
            val novaSenha = binding.etNovaSenha.text.toString()
            val confirmacao = binding.etConfirmarNovaSenha.text.toString()

            if (novaSenha.isEmpty()) {
                binding.etNovaSenha.error = "Informe a nova senha"
                return@setOnClickListener
            }
            if (novaSenha.length < 6) {
                binding.etNovaSenha.error = "Mínimo 6 caracteres"
                return@setOnClickListener
            }
            if (novaSenha != confirmacao) {
                binding.etConfirmarNovaSenha.error = "As senhas não coincidem"
                return@setOnClickListener
            }
            redefinirSenha(tokenValidado, novaSenha)
        }
    }

    private fun solicitarToken(email: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.solicitarRedefinicao(EmailRequest(email))
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@RecuperacaoSenhaActivity,
                        "Token enviado! Verifique seu e-mail.",
                        Toast.LENGTH_LONG
                    ).show()
                    mostrarEtapa(STEP_TOKEN)
                } else {
                    mostrarErro("E-mail não encontrado ou erro no servidor")
                }
            } catch (e: Exception) {
                mostrarErro("Falha na conexão. Verifique sua internet")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun validarToken(token: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response =
                    RetrofitClient.apiService.validarTokenRedefinicao(TokenRequest(token))
                if (response.isSuccessful && response.body()?.valido == true) {
                    tokenValidado = token
                    mostrarEtapa(STEP_NOVA_SENHA)
                } else {
                    mostrarErro("Token inválido ou expirado")
                }
            } catch (e: Exception) {
                mostrarErro("Falha na conexão. Verifique sua internet")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun redefinirSenha(token: String, novaSenha: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.redefinirSenha(
                    RedefinirSenhaRequest(token = token, novaSenha = novaSenha)
                )
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@RecuperacaoSenhaActivity,
                        "Senha redefinida com sucesso! Faça login.",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(this@RecuperacaoSenhaActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                } else {
                    mostrarErro("Não foi possível redefinir a senha. Tente novamente")
                }
            } catch (e: Exception) {
                mostrarErro("Falha na conexão. Verifique sua internet")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun mostrarEtapa(etapa: Int) {

        val showEtapa1 = etapa == STEP_EMAIL
        binding.etEmail.visibility = if (showEtapa1) View.VISIBLE else View.GONE
        binding.tvLabelEmail.visibility = if (showEtapa1) View.VISIBLE else View.GONE
        binding.btnEnviarToken.visibility = if (showEtapa1) View.VISIBLE else View.GONE

        val showEtapa2 = etapa == STEP_TOKEN
        binding.groupToken.visibility = if (showEtapa2) View.VISIBLE else View.GONE
        binding.btnConfirmarToken.visibility = if (showEtapa2) View.VISIBLE else View.GONE

        val showEtapa3 = etapa == STEP_NOVA_SENHA
        binding.groupNovaSenha.visibility = if (showEtapa3) View.VISIBLE else View.GONE
        binding.btnRedefinirSenha.visibility = if (showEtapa3) View.VISIBLE else View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnEnviarToken.isEnabled = !loading
        binding.btnConfirmarToken.isEnabled = !loading
        binding.btnRedefinirSenha.isEnabled = !loading
    }

    private fun mostrarErro(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
