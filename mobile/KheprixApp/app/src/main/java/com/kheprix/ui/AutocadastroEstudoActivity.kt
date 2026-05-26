package com.kheprix.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.zxing.integration.android.IntentIntegrator
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityAutocadastroEstudoBinding
import com.kheprix.models.IngressarRequest
import kotlinx.coroutines.launch

class AutocadastroEstudoActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityAutocadastroEstudoBinding

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) iniciarScanQr()
            else Toast.makeText(this, "Permissão de câmera necessária para QR", Toast.LENGTH_SHORT)
                .show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAutocadastroEstudoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivScanQr.setOnClickListener { verificarPermissaoCamera() }

        binding.btnConfirmar.setOnClickListener {
            val codigo = binding.etCodigo.text.toString().trim()
            val senha = binding.etSenha.text.toString()

            if (codigo.isEmpty()) {
                binding.etCodigo.error = "Informe o código"; return@setOnClickListener
            }
            if (senha.isEmpty()) {
                binding.etSenha.error = "Informe a senha"; return@setOnClickListener
            }

            ingressarEstudo(codigo, senha)
        }

        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    private fun verificarPermissaoCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            iniciarScanQr()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun iniciarScanQr() {
        IntentIntegrator(this)
            .setPrompt("Aponte para o QR Code do estudo")
            .setBeepEnabled(true)
            .initiateScan()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: android.content.Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            binding.etCodigo.setText(result.contents)
        }
    }

    private fun ingressarEstudo(codigo: String, senha: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.ingressarEstudo(
                    SessionManager.getAuthHeader(),
                    IngressarRequest(codigo = codigo, senhaAutocadastro = senha)
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    Toast.makeText(
                        this@AutocadastroEstudoActivity,
                        "Ingressou em \"${body?.nomeEstudo}\" como ${body?.perfil}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    when (resp.code()) {
                        401 -> Toast.makeText(
                            this@AutocadastroEstudoActivity,
                            "Código ou senha incorretos",
                            Toast.LENGTH_SHORT
                        ).show()

                        404 -> Toast.makeText(
                            this@AutocadastroEstudoActivity,
                            "Estudo não encontrado",
                            Toast.LENGTH_SHORT
                        ).show()

                        else -> Toast.makeText(
                            this@AutocadastroEstudoActivity,
                            "Erro: ${resp.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(this@AutocadastroEstudoActivity, "Sem conexão", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }
}
