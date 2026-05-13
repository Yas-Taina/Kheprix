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
import com.kheprix.db.EstudoDao
import com.kheprix.db.EstudoOfflineManager
import com.kheprix.db.OfflineRepository
import com.kheprix.ui.adapters.EstudoAdapter
import com.kheprix.ui.adapters.EstudoItem
import kotlinx.coroutines.launch


class EstudosActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityEstudosBinding
    private lateinit var offlineManager: EstudoOfflineManager
    private val estudos = mutableListOf<EstudoItem>()
    private lateinit var adapter: EstudoAdapter
    private var filtroNome: String = ""

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

        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    private fun carregarEstudos() {
        setLoading(true)
        val filtro = filtroNome
        lifecycleScope.launch {
            val estudosOnline = mutableListOf<EstudoItem>()

            try {
                val token = SessionManager.getAuthHeader()
                val response =
                    RetrofitClient.apiService.getEstudos(token, nome = filtro.ifEmpty { null })

                if (response.isSuccessful) {
                    val repo = OfflineRepository(this@EstudosActivity)
                    response.body()?.forEach { estudo ->
                        val localId = try {
                            repo.cacheEstudo(estudo)
                        } catch (_: Exception) {
                            buscarLocalIdPorRemoteId(estudo.id)
                        }
                        val explicito = localId?.let {
                            offlineManager.isExplicitamenteSalvoOffline(it)
                        } ?: false
                        val offlineCount = localId?.let {
                            offlineManager.contarRegistrosOffline(it)
                        } ?: 0

                        estudosOnline.add(
                            EstudoItem(
                                remoteId = estudo.id,
                                localId = localId,
                                nome = estudo.nome,
                                createdAt = estudo.createdAt,
                                perfil = estudo.perfil ?: "",
                                salvosOffline = explicito,
                                registrosOffline = offlineCount
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }

            val estudosOffline = EstudoDao(this@EstudosActivity).listarTodos()
            val remoteIds = estudosOnline.mapNotNull { it.remoteId }.toSet()

            estudos.clear()
            estudos.addAll(estudosOnline)

            estudosOffline.forEach { off ->
                val isOfflineOnly = off.remoteId == null || !remoteIds.contains(off.remoteId)
                if (!isOfflineOnly) return@forEach
                if (!offlineManager.isExplicitamenteSalvoOffline(off.localId)) return@forEach
                if (filtro.isNotEmpty() && !off.nome.contains(
                        filtro,
                        ignoreCase = true
                    )
                ) return@forEach

                val offlineCount = offlineManager.contarRegistrosOffline(off.localId)
                estudos.add(
                    EstudoItem(
                        remoteId = off.remoteId ?: -off.localId.toInt(),
                        localId = off.localId,
                        nome = off.nome,
                        createdAt = off.createdAt ?: "",
                        perfil = off.perfil ?: "",
                        salvosOffline = true,
                        registrosOffline = offlineCount
                    )
                )
            }

            adapter.notifyDataSetChanged()
            setLoading(false)
        }
    }

    private fun carregarEstudosLocais() {
        val db = com.kheprix.db.DatabaseHelper(this).readableDatabase
        val cursor = db.rawQuery(
            "SELECT local_id, remote_id, nome, perfil, created_at, updated_at FROM estudos ORDER BY updated_at DESC",
            null
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val localId = c.getLong(0)
                val remoteId = if (c.isNull(1)) -1 else c.getInt(1)
                val offlineCount = offlineManager.contarRegistrosOffline(localId)
                estudos.add(
                    EstudoItem(
                        remoteId = remoteId,
                        localId = localId,
                        nome = c.getString(2) ?: "—",
                        createdAt = c.getString(4) ?: "",
                        perfil = c.getString(3) ?: "",
                        salvosOffline = true,
                        registrosOffline = offlineCount
                    )
                )
            }
        }
        adapter.notifyDataSetChanged()
    }

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
                    Toast.makeText(
                        this@EstudosActivity,
                        "Estudo salvo offline!",
                        Toast.LENGTH_SHORT
                    ).show()
                    carregarEstudos()
                }
                .onFailure {
                    Toast.makeText(
                        this@EstudosActivity,
                        "Erro ao salvar offline: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
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
                    Toast.makeText(this@EstudosActivity, "Dados atualizados!", Toast.LENGTH_SHORT)
                        .show()
                    carregarEstudos()
                }
                .onFailure {
                    Toast.makeText(this@EstudosActivity, "Erro: ${it.message}", Toast.LENGTH_LONG)
                        .show()
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
                    setLoading(true)
                    offlineManager.deletarDadosEstudo(localId)
                        .onSuccess { carregarEstudos() }
                        .onFailure { confirmarLimparSemSincronizar(item) }
                    setLoading(false)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarLimparSemSincronizar(item: EstudoItem) {
        AlertDialog.Builder(this)
            .setTitle("Sem conexão")
            .setMessage("Não foi possível sincronizar \"${item.nome}\" com o servidor. Deseja limpar os dados offline mesmo assim?\n\nAtenção: dados não sincronizados serão perdidos permanentemente.")
            .setPositiveButton("Limpar mesmo assim") { _, _ ->
                val localId = item.localId ?: return@setPositiveButton
                offlineManager.deletarDadosEstudoLocal(localId)
                carregarEstudos()
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
                    Toast.makeText(this@EstudosActivity, "Estudo removido", Toast.LENGTH_SHORT)
                        .show()
                    carregarEstudos()
                } else {
                    Toast.makeText(this@EstudosActivity, "Erro ao deletar", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EstudosActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

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
                carregarEstudos()
            }
            .setNeutralButton("Limpar") { _, _ ->
                filtroNome = ""
                carregarEstudos()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
