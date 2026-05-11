package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityEstudoDetalheBinding
import com.kheprix.db.EstudoOfflineManager
import kotlinx.coroutines.launch


class EstudoDetalheActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityEstudoDetalheBinding
    private lateinit var offlineManager: EstudoOfflineManager

    private var estudoRemoteId = -1
    private var estudoLocalId = -1L
    private var estudoNome = ""
    private var perfil = ""

    companion object {
        const val EXTRA_ESTUDO_REMOTE_ID = "estudo_remote_id"
        const val EXTRA_ESTUDO_LOCAL_ID = "estudo_local_id"
        const val EXTRA_ESTUDO_NOME = "estudo_nome"
        const val EXTRA_PERFIL = "perfil"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstudoDetalheBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra(EXTRA_ESTUDO_REMOTE_ID, -1)
        estudoLocalId = intent.getLongExtra(EXTRA_ESTUDO_LOCAL_ID, -1L)
        estudoNome = intent.getStringExtra(EXTRA_ESTUDO_NOME) ?: ""
        perfil = intent.getStringExtra(EXTRA_PERFIL) ?: ""

        offlineManager = EstudoOfflineManager(this)

        binding.tvEstudoNome.text = estudoNome

        atualizarContadorOffline()
        carregarDetalhes()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSincronizar.setOnClickListener {
            if (estudoLocalId == -1L) {
                Toast.makeText(this, "Estudo não salvo offline", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setLoading(true)
            lifecycleScope.launch {
                offlineManager.sincronizarDadosEstudo(estudoLocalId)
                    .onSuccess {
                        atualizarContadorOffline()
                        val pendentes =
                            offlineManager.listarRegistrosOfflinePendentes(estudoLocalId)
                        if (pendentes.isEmpty()) {
                            Toast.makeText(
                                this@EstudoDetalheActivity,
                                "Sincronizado!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            mostrarPendentes(pendentes)
                        }
                    }
                    .onFailure {
                        Toast.makeText(
                            this@EstudoDetalheActivity,
                            "Erro: ${it.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                setLoading(false)
            }
        }

        binding.btnVisualizarDetalhes.setOnClickListener {
            val cardVisivel = binding.cardDetalhes.visibility == View.VISIBLE
            binding.cardDetalhes.visibility = if (cardVisivel) View.GONE else View.VISIBLE
        }

        binding.cardCampanhas.setOnClickListener {
            val intent = Intent(this, CampanhasActivity::class.java)
            intent.putExtra("estudo_remote_id", estudoRemoteId)
            intent.putExtra("estudo_local_id", estudoLocalId)
            intent.putExtra("estudo_nome", estudoNome)
            startActivity(intent)
        }

        binding.cardEspecies.setOnClickListener {
            val intent = Intent(this, EspeciesActivity::class.java)
            intent.putExtra("estudo_remote_id", estudoRemoteId)
            intent.putExtra("estudo_local_id", estudoLocalId)
            intent.putExtra("estudo_nome", estudoNome)
            startActivity(intent)
        }

        binding.cardCadastrarEspecie.setOnClickListener {
            val intent = Intent(this, CadastroEspecieActivity::class.java)
            intent.putExtra("estudo_remote_id", estudoRemoteId)
            intent.putExtra("estudo_local_id", estudoLocalId)
            startActivity(intent)
        }

        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(
                android.content.Intent(
                    this,
                    PerfilActivity::class.java
                )
            )
        }
    }

    private fun atualizarContadorOffline() {
        val count = if (estudoLocalId != -1L)
            offlineManager.contarRegistrosOffline(estudoLocalId) else 0
        binding.tvRegistrosOffline.text = "$count Registros Offline"
        binding.tvRegistrosOffline.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    private fun carregarDetalhes() {
        lifecycleScope.launch {
            if (estudoRemoteId > 0) {
                try {
                    val token = SessionManager.getAuthHeader()
                    val response = RetrofitClient.apiService.getEstudos(token)
                    val estudo = response.body()?.firstOrNull { it.id == estudoRemoteId }
                    if (estudo != null) {
                        preencherDetalhes(
                            estudo.nome,
                            estudo.observacoes,
                            estudo.perfil,
                            estudo.createdAt,
                            estudo.updatedAt
                        )
                        return@launch
                    }
                } catch (_: Exception) {
                }
            }
            preencherDetalhesOffline()
        }
    }

    private fun preencherDetalhesOffline() {
        if (estudoLocalId <= 0) return
        val db = com.kheprix.db.DatabaseHelper(this).readableDatabase
        db.rawQuery(
            "SELECT nome, observacoes, perfil, created_at, updated_at FROM estudos WHERE local_id = ?",
            arrayOf(estudoLocalId.toString())
        ).use { c ->
            if (c.moveToFirst()) {
                preencherDetalhes(
                    c.getString(0), c.getString(1), c.getString(2),
                    c.getString(3) ?: "", c.getString(4) ?: ""
                )
            }
        }
    }

    private fun preencherDetalhes(
        nome: String,
        obs: String?,
        perfil: String?,
        createdAt: String,
        updatedAt: String
    ) {
        binding.tvDetalheNome.text = nome
        binding.tvDetalheObs.text = obs ?: "—"
        binding.tvDetalhePerfil.text = perfil ?: "—"
        binding.tvDetalheCriado.text = formatarData(createdAt)
        binding.tvDetalheAtualizado.text = formatarData(updatedAt)
    }

    private fun formatarData(iso: String): String {
        return try {
            val parts = iso.substring(0, 10).split("-")
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } catch (e: Exception) {
            iso
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun mostrarPendentes(pendentes: List<String>) {
        val msg = "${pendentes.size} item(ns) ainda não sincronizados:\n\n" +
                pendentes.joinToString("\n") { "• $it" }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Pendências offline")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }
}
