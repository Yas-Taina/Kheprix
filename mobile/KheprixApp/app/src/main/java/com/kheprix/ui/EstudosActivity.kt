package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityEstudosBinding
import com.kheprix.db.EstudoOfflineManager
import com.kheprix.ui.adapters.EstudoAdapter
import com.kheprix.ui.adapters.EstudoItem
import kotlinx.coroutines.launch

/**
 * Tela principal: lista de Estudos do usuário.
 *
 * Comportamento dos botões por estudo:
 *  - Sem dados offline → exibe "Salvar Offline"
 *  - Com dados offline → exibe "Limpar Armazenamento" + "Atualizar Dados"
 *  - Ícone lixeira → deletar estudo no servidor (com confirmação)
 *  - Ícone usuário+ → colaboradores (somente se perfil == "proprietario")
 *  - Botão filtrar → abre FilterDialog (a implementar)
 *
 * Navegação:
 *  - "Adicionar Estudo" → NovoEstudoActivity
 *  - Clique no item → EstudoDetalheActivity
 *  - Ícone usuário+ → ColaboradoresActivity
 */
class EstudosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEstudosBinding
    private lateinit var offlineManager: EstudoOfflineManager
    private val estudos = mutableListOf<EstudoItem>()
    private lateinit var adapter: EstudoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstudosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        offlineManager = EstudoOfflineManager(this)

        setupRecyclerView()
        setupListeners()
        carregarEstudos()
    }

    override fun onResume() {
        super.onResume()
        carregarEstudos()
    }

    private fun setupRecyclerView() {
        adapter = EstudoAdapter(
            estudos = estudos,
            onItemClick = { item ->
                val intent = Intent(this, EstudoDetalheActivity::class.java)
                intent.putExtra(EstudoDetalheActivity.EXTRA_ESTUDO_REMOTE_ID, item.remoteId)
                intent.putExtra(EstudoDetalheActivity.EXTRA_ESTUDO_LOCAL_ID, item.localId)
                intent.putExtra(EstudoDetalheActivity.EXTRA_ESTUDO_NOME, item.nome)
                intent.putExtra(EstudoDetalheActivity.EXTRA_PERFIL, item.perfil)
                startActivity(intent)
            },
            onDeleteClick = { item -> confirmarDeleteEstudo(item) },
            onColaboradoresClick = { item ->
                val intent = Intent(this, ColaboradoresActivity::class.java)
                intent.putExtra("estudo_remote_id", item.remoteId)
                intent.putExtra("estudo_nome", item.nome)
                startActivity(intent)
            },
            onSalvarOfflineClick = { item -> salvarOffline(item) },
            onLimparOfflineClick = { item -> confirmarLimparOffline(item) },
            onAtualizarDadosClick = { item -> atualizarDados(item) }
        )

        binding.rvEstudos.layoutManager = LinearLayoutManager(this)
        binding.rvEstudos.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAdicionarEstudo.setOnClickListener {
            startActivity(Intent(this, NovoEstudoActivity::class.java))
        }

        binding.btnFiltrar.setOnClickListener { mostrarFiltroDialog() }

        // Header: menu lateral e perfil
        binding.ivMenuLateral.setOnClickListener {
        }
        binding.ivPerfil.setOnClickListener {
            // TODO: abrir PerfilActivity
        }
    }

    private fun carregarEstudos(nome: String = "") {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val token = SessionManager.getAuthHeader()
                val response = RetrofitClient.apiService.getEstudos(token, nome = nome.ifEmpty { null })

                if (response.isSuccessful) {
                    val lista = response.body() ?: emptyList()
                    estudos.clear()
                    lista.forEach { estudo ->
                        // Verifica se já existe salvo offline pelo remote_id
                        val localId = buscarLocalIdPorRemoteId(estudo.id)
                        val offline = localId != null
                        val offlineCount = if (offline && localId != null)
                            offlineManager.contarRegistrosOffline(localId) else 0

                        estudos.add(
                            EstudoItem(
                                remoteId = estudo.id,
                                localId = localId,
                                nome = estudo.nome,
                                createdAt = estudo.createdAt,
                                perfil = estudo.perfil ?: "",
                                salvosOffline = offline,
                                registrosOffline = offlineCount
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@EstudosActivity, "Erro ao carregar estudos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EstudosActivity, "Sem conexão. Dados locais exibidos.", Toast.LENGTH_SHORT).show()
                carregarEstudosLocais()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun carregarEstudosLocais() {
        // Carrega estudos do SQLite em modo offline
        val db = com.kheprix.db.DatabaseHelper(this).readableDatabase
        val cursor = db.rawQuery(
            "SELECT local_id, remote_id, nome, perfil, created_at, updated_at FROM estudos ORDER BY updated_at DESC",
            null
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val localId  = c.getLong(0)
                val remoteId = if (c.isNull(1)) -1 else c.getInt(1)
                val offlineCount = offlineManager.contarRegistrosOffline(localId)
                estudos.add(EstudoItem(
                    remoteId    = remoteId,
                    localId     = localId,
                    nome        = c.getString(2) ?: "—",
                    createdAt   = c.getString(4) ?: "",
                    perfil      = c.getString(3) ?: "",
                    salvosOffline = true,
                    registrosOffline = offlineCount
                ))
            }
        }
        adapter.notifyDataSetChanged()
    }

    /** Retorna o local_id de um estudo pelo remote_id, ou null se não estiver salvo. */
    private fun buscarLocalIdPorRemoteId(remoteId: Int): Long? {
        val db = com.kheprix.db.DatabaseHelper(this).readableDatabase
        val cursor = db.rawQuery(
            "SELECT local_id FROM estudos WHERE remote_id = ?",
            arrayOf(remoteId.toString())
        )
        return cursor.use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    private fun salvarOffline(item: EstudoItem) {
        setLoading(true)
        lifecycleScope.launch {
            offlineManager.salvarEstudoOffline(item.remoteId)
                .onSuccess {
                    Toast.makeText(this@EstudosActivity, "Estudo salvo offline!", Toast.LENGTH_SHORT).show()
                    carregarEstudos()
                }
                .onFailure {
                    Toast.makeText(this@EstudosActivity, "Erro ao salvar offline: ${it.message}", Toast.LENGTH_LONG).show()
                }
            setLoading(false)
        }
    }

    private fun atualizarDados(item: EstudoItem) {
        val localId = item.localId ?: return
        setLoading(true)
        lifecycleScope.launch {
            offlineManager.atualizarDadosEstudo(localId)
                .onSuccess {
                    Toast.makeText(this@EstudosActivity, "Dados atualizados!", Toast.LENGTH_SHORT).show()
                    carregarEstudos()
                }
                .onFailure {
                    Toast.makeText(this@EstudosActivity, "Erro: ${it.message}", Toast.LENGTH_LONG).show()
                }
            setLoading(false)
        }
    }

    private fun confirmarLimparOffline(item: EstudoItem) {
        AlertDialog.Builder(this)
            .setTitle("Limpar armazenamento")
            .setMessage("Isso vai sincronizar e remover os dados offline de \"${item.nome}\". Continuar?")
            .setPositiveButton("Sim") { _, _ ->
                val localId = item.localId ?: return@setPositiveButton
                lifecycleScope.launch {
                    offlineManager.deletarDadosEstudo(localId)
                        .onSuccess { carregarEstudos() }
                        .onFailure { Toast.makeText(this@EstudosActivity, "Erro: ${it.message}", Toast.LENGTH_LONG).show() }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarDeleteEstudo(item: EstudoItem) {
        AlertDialog.Builder(this)
            .setTitle("Deletar estudo")
            .setMessage("Tem certeza que deseja deletar \"${item.nome}\"? Esta ação não pode ser desfeita.")
            .setPositiveButton("Deletar") { _, _ -> deletarEstudo(item) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletarEstudo(item: EstudoItem) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteEstudo(
                    SessionManager.getAuthHeader(), item.remoteId
                )
                if (response.isSuccessful || response.code() == 204 || response.code() == 200) {
                    item.localId?.let { offlineManager.deletarDadosEstudoLocal(it) }
                    Toast.makeText(this@EstudosActivity, "Estudo removido", Toast.LENGTH_SHORT).show()
                    carregarEstudos()
                } else {
                    Toast.makeText(this@EstudosActivity, "Erro ao deletar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EstudosActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
    private var filtroNome: String = ""

    private fun mostrarFiltroDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "Nome do estudo"; setPadding(48, 24, 48, 24)
        }
        editText.setText(filtroNome)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filtrar estudos")
            .setView(editText)
            .setPositiveButton("Filtrar") { _, _ ->
                filtroNome = editText.text.toString().trim()
                carregarEstudos(filtroNome)
            }
            .setNeutralButton("Limpar") { _, _ ->
                filtroNome = ""
                carregarEstudos()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


}
