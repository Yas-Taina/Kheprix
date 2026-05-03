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

/**
 * Detalhes de um Estudo.
 *
 * Exibe:
 *  - Nome do estudo + contagem de registros offline
 *  - Botão "Sincronizar Dados" (sincroniza local → servidor)
 *  - Botão "Visualizar Detalhes" → expande/colapsa um card inline com dados cadastrados
 *  - Cards de navegação: Campanhas, Espécies, Cadastrar Espécie
 *
 * Extras recebidos:
 *   EXTRA_ESTUDO_REMOTE_ID → Int
 *   EXTRA_ESTUDO_LOCAL_ID  → Long (-1 se não salvo offline)
 *   EXTRA_ESTUDO_NOME      → String
 *   EXTRA_PERFIL           → String
 */
class EstudoDetalheActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityEstudoDetalheBinding
    private lateinit var offlineManager: EstudoOfflineManager

    private var estudoRemoteId = -1
    private var estudoLocalId  = -1L
    private var estudoNome     = ""
    private var perfil         = ""

    companion object {
        const val EXTRA_ESTUDO_REMOTE_ID = "estudo_remote_id"
        const val EXTRA_ESTUDO_LOCAL_ID  = "estudo_local_id"
        const val EXTRA_ESTUDO_NOME      = "estudo_nome"
        const val EXTRA_PERFIL           = "perfil"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstudoDetalheBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra(EXTRA_ESTUDO_REMOTE_ID, -1)
        estudoLocalId  = intent.getLongExtra(EXTRA_ESTUDO_LOCAL_ID, -1L)
        estudoNome     = intent.getStringExtra(EXTRA_ESTUDO_NOME) ?: ""
        perfil         = intent.getStringExtra(EXTRA_PERFIL) ?: ""

        offlineManager = EstudoOfflineManager(this)

        binding.tvEstudoNome.text = estudoNome

        atualizarContadorOffline()
        carregarDetalhes()
        setupListeners()
    }

    private fun setupListeners() {
        // "Sincronizar Dados" → push local → servidor
        binding.btnSincronizar.setOnClickListener {
            if (estudoLocalId == -1L) {
                Toast.makeText(this, "Estudo não salvo offline", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setLoading(true)
            lifecycleScope.launch {
                offlineManager.sincronizarDadosEstudo(estudoLocalId)
                    .onSuccess {
                        Toast.makeText(this@EstudoDetalheActivity, "Sincronizado!", Toast.LENGTH_SHORT).show()
                        atualizarContadorOffline()
                    }
                    .onFailure {
                        Toast.makeText(this@EstudoDetalheActivity, "Erro: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                setLoading(false)
            }
        }

        // "Visualizar Detalhes" → toggle do card inline
        binding.btnVisualizarDetalhes.setOnClickListener {
            val cardVisivel = binding.cardDetalhes.visibility == View.VISIBLE
            binding.cardDetalhes.visibility = if (cardVisivel) View.GONE else View.VISIBLE
        }

        // Navegação para Campanhas
        binding.cardCampanhas.setOnClickListener {
            val intent = Intent(this, CampanhasActivity::class.java)
            intent.putExtra("estudo_remote_id", estudoRemoteId)
            intent.putExtra("estudo_local_id", estudoLocalId)
            intent.putExtra("estudo_nome", estudoNome)
            startActivity(intent)
        }

        // Navegação para lista de Espécies
        binding.cardEspecies.setOnClickListener {
            val intent = Intent(this, EspeciesActivity::class.java)
            intent.putExtra("estudo_remote_id", estudoRemoteId)
            intent.putExtra("estudo_local_id", estudoLocalId)
            intent.putExtra("estudo_nome", estudoNome)
            startActivity(intent)
        }

        // Navegação para Cadastro de Espécie
        binding.cardCadastrarEspecie.setOnClickListener {
            val intent = Intent(this, CadastroEspecieActivity::class.java)
            intent.putExtra("estudo_remote_id", estudoRemoteId)
            intent.putExtra("estudo_local_id", estudoLocalId)
            startActivity(intent)
        }

        // Header
        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener { startActivity(android.content.Intent(this, PerfilActivity::class.java)) }
    }

    private fun atualizarContadorOffline() {
        val count = if (estudoLocalId != -1L)
            offlineManager.contarRegistrosOffline(estudoLocalId) else 0
        binding.tvRegistrosOffline.text = "$count Registros Offline"
        binding.tvRegistrosOffline.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    /** Carrega os dados do estudo do servidor para preencher o card de detalhes */
    private fun carregarDetalhes() {
        lifecycleScope.launch {
            try {
                val token = SessionManager.getAuthHeader()
                val response = RetrofitClient.apiService.getEstudos(token)
                val estudo = response.body()?.firstOrNull { it.id == estudoRemoteId }

                estudo?.let {
                    binding.tvDetalheNome.text = it.nome
                    binding.tvDetalheObs.text = it.observacoes ?: "—"
                    binding.tvDetalhePerfil.text = it.perfil ?: "—"
                    binding.tvDetalheCriado.text = formatarData(it.createdAt)
                    binding.tvDetalheAtualizado.text = formatarData(it.updatedAt)
                }
            } catch (_: Exception) {
                // Modo offline: detalhes podem não estar disponíveis
            }
        }
    }

    private fun formatarData(iso: String): String {
        return try {
            val parts = iso.substring(0, 10).split("-")
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } catch (e: Exception) { iso }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
