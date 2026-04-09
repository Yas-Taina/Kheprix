package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kheprix.api.RetrofitClient
import com.kheprix.databinding.ActivityCadastroBinding
import com.kheprix.models.AutocadastroRequest
import kotlinx.coroutines.launch

/**
 * Tela de Cadastro de novo usuário.
 *
 * Fluxo:
 *  1. Usuário preenche nome, e-mail, senha e confirmação de senha
 *  2. Valida se as senhas coincidem
 *  3. POST /usuarios/autocadastro
 *  4. Em caso de sucesso, redireciona para LoginActivity
 */
class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Link "aqui" → voltar para login
        binding.tvLinkLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnConfirmar.setOnClickListener {
            val nome  = binding.etNome.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val senha = binding.etSenha.text.toString()
            val confirmacao = binding.etConfirmarSenha.text.toString()

            if (!validarCampos(nome, email, senha, confirmacao)) return@setOnClickListener

            realizarCadastro(nome, email, senha)
        }
    }

    private fun validarCampos(nome: String, email: String, senha: String, confirmacao: String): Boolean {
        if (nome.isEmpty()) {
            binding.etNome.error = "Informe o nome"
            return false
        }
        if (email.isEmpty()) {
            binding.etEmail.error = "Informe o e-mail"
            return false
        }
        if (senha.isEmpty()) {
            binding.etSenha.error = "Informe a senha"
            return false
        }
        if (senha.length < 6) {
            binding.etSenha.error = "A senha deve ter ao menos 6 caracteres"
            return false
        }
        if (senha != confirmacao) {
            binding.etConfirmarSenha.error = "As senhas não coincidem"
            return false
        }
        return true
    }

    private fun realizarCadastro(nome: String, email: String, senha: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.autocadastro(
                    AutocadastroRequest(nome = nome, email = email, senha = senha)
                )

                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        // Salva dados para pré-preenchimento após login
                        SessionManager.saveUser(user.id, user.nome, user.email)
                    }
                    Toast.makeText(
                        this@CadastroActivity,
                        "Cadastro realizado! Faça seu login.",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(this@CadastroActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                } else {
                    when (response.code()) {
                        409 -> mostrarErro("E-mail já cadastrado")
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

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }

    private fun mostrarErro(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
