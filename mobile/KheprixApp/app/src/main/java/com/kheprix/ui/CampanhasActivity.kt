package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityCampanhasBinding
import com.kheprix.db.CampanhaDao
import com.kheprix.db.EstudoDao
import com.kheprix.db.OfflineRepository
import com.kheprix.models.CampanhaResponse
import kotlinx.coroutines.launch


class CampanhasActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityCampanhasBinding
    private var estudoRemoteId = -1
    private var estudoLocalId  = -1L
    private var estudoNome     = ""
    private val campanhas = mutableListOf<CampanhaResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCampanhasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId  = intent.getLongExtra("estudo_local_id", -1L)
        estudoNome     = intent.getStringExtra("estudo_nome") ?: ""

        binding.tvEstudoNome.text = estudoNome

        setupRecyclerView()
        setupListeners()
        carregarCampanhas()
    }

    override fun onResume() {
        super.onResume()
        carregarCampanhas()
    }

    private fun setupRecyclerView() {
        binding.rvCampanhas.layoutManager = LinearLayoutManager(this)
        binding.rvCampanhas.adapter = CampanhaAdapter(campanhas,
            onItemClick = { campanha ->
                val intent = Intent(this, UnidadesActivity::class.java)
                intent.putExtra("estudo_remote_id", estudoRemoteId)
                intent.putExtra("estudo_local_id", estudoLocalId)
                intent.putExtra("estudo_nome", estudoNome)
                intent.putExtra("campanha_id", campanha.id)
                intent.putExtra("campanha_local_id", resolveCampanhaLocalId(campanha.id))
                intent.putExtra("campanha_nome", campanha.nome)
                startActivity(intent)
            },
            onDeleteClick = { campanha -> confirmarDelete(campanha) }
        )
    }


    private fun resolveCampanhaLocalId(campanhaId: Int): Long {
        if (campanhaId < 0) return (-campanhaId).toLong()
        if (estudoLocalId <= 0) return -1L
        return CampanhaDao(this).buscarPorRemoteIdEscopo(campanhaId, estudoLocalId)?.localId ?: -1L
    }

    private fun setupListeners() {
        binding.btnAdicionarCampanha.setOnClickListener {
            val intent = Intent(this, NovaCampanhaActivityV2::class.java)
            intent.putExtra("estudo_remote_id", estudoRemoteId)
            intent.putExtra("estudo_local_id", estudoLocalId)
            intent.putExtra("estudo_nome", estudoNome)
            startActivity(intent)
        }
        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    private fun carregarCampanhas() {
        if (estudoRemoteId <= 0) {
            val estudoLocal = if (estudoLocalId > 0) estudoLocalId
                else EstudoDao(this).listarTodos().firstOrNull { it.nome == estudoNome }?.localId
            if (estudoLocal != null) {
                estudoLocalId = estudoLocal
                carregarCampanhasOffline(estudoLocal)
            }
            return
        }

        lifecycleScope.launch {
            val campanhasOnline = mutableListOf<CampanhaResponse>()

            try {
                val resp = RetrofitClient.apiService.getCampanhas(
                    SessionManager.getAuthHeader(), estudoRemoteId
                )
                if (resp.isSuccessful) {
                    campanhasOnline.addAll(resp.body() ?: emptyList())
                }
            } catch (_: Exception) { }

            val campanhaDao = CampanhaDao(this@CampanhasActivity)
            val repo = OfflineRepository(this@CampanhasActivity)
            var resolvedEstudoLocalId = EstudoDao(this@CampanhasActivity).buscarPorRemoteId(estudoRemoteId)?.localId
            if (resolvedEstudoLocalId == null && campanhasOnline.isNotEmpty()) {
                try {
                    val estudoResp = RetrofitClient.apiService.getEstudos(SessionManager.getAuthHeader()).body()
                    val estudo = estudoResp?.firstOrNull { it.id == estudoRemoteId }
                    if (estudo != null) resolvedEstudoLocalId = repo.cacheEstudo(estudo)
                } catch (_: Exception) { }
            }
            if (resolvedEstudoLocalId == null) return@launch
            this@CampanhasActivity.estudoLocalId = resolvedEstudoLocalId

            campanhasOnline.forEach { c ->
                try { repo.cacheCampanha(resolvedEstudoLocalId, c) } catch (_: Exception) { }
            }

            val offline = campanhaDao.listarPorEstudoLocal(resolvedEstudoLocalId)
            val remoteIds = campanhasOnline.mapNotNull { it.id }.toSet()

            campanhas.clear()
            campanhas.addAll(campanhasOnline)

            offline.forEach { off ->
                if (off.remoteId == null || !remoteIds.contains(off.remoteId)) {
                    campanhas.add(CampanhaResponse(
                        id = off.remoteId ?: -off.localId.toInt(),
                        nome = off.nome,
                        dataInicio = off.dataInicio,
                        dataFim = off.dataFim,
                        descricao = off.descricao,
                        createdAt = off.createdAt ?: "",
                        updatedAt = off.updatedAt ?: "",
                        valoresVariaveis = null
                    ))
                }
            }

            binding.rvCampanhas.adapter?.notifyDataSetChanged()
        }
    }

    private fun carregarCampanhasOffline(estudoLocalId: Long) {
        val campanhaDao = CampanhaDao(this)
        campanhas.clear()
        campanhas.addAll(campanhaDao.listarPorEstudoLocal(estudoLocalId).map { off ->
            CampanhaResponse(
                id = off.remoteId ?: -off.localId.toInt(),
                nome = off.nome,
                dataInicio = off.dataInicio,
                dataFim = off.dataFim,
                descricao = off.descricao,
                createdAt = off.createdAt ?: "",
                updatedAt = off.updatedAt ?: "",
                valoresVariaveis = null
            )
        })
        binding.rvCampanhas.adapter?.notifyDataSetChanged()
    }

    private fun confirmarDelete(campanha: CampanhaResponse) {
        AlertDialog.Builder(this)
            .setTitle("Deletar campanha")
            .setMessage("Deletar \"${campanha.nome}\"?")
            .setPositiveButton("Deletar") { _, _ ->
                if (campanha.id < 0 || estudoRemoteId <= 0) {
                    val localId = if (campanha.id < 0) (-campanha.id).toLong()
                        else CampanhaDao(this).listarTodos().firstOrNull { it.remoteId == campanha.id }?.localId
                    if (localId != null) {
                        com.kheprix.db.DatabaseHelper(this).writableDatabase
                            .delete("campanhas", "local_id = ?", arrayOf(localId.toString()))
                        carregarCampanhas()
                    }
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        RetrofitClient.apiService.deleteCampanha(
                            SessionManager.getAuthHeader(), estudoRemoteId, campanha.id
                        )
                        CampanhaDao(this@CampanhasActivity).listarTodos()
                            .firstOrNull { it.remoteId == campanha.id }?.localId?.let { lid ->
                                com.kheprix.db.DatabaseHelper(this@CampanhasActivity).writableDatabase
                                    .delete("campanhas", "local_id = ?", arrayOf(lid.toString()))
                            }
                        carregarCampanhas()
                    } catch (_: Exception) { }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }
}

class CampanhaAdapter(
    private val items: List<CampanhaResponse>,
    private val onItemClick: (CampanhaResponse) -> Unit,
    private val onDeleteClick: (CampanhaResponse) -> Unit
) : RecyclerView.Adapter<CampanhaAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView    = view.findViewById(R.id.tvCampanhaNome)
        val tvData: TextView    = view.findViewById(R.id.tvCampanhaData)
        val ivDelete: ImageView = view.findViewById(R.id.ivDeleteCampanha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_campanha, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNome.text = item.nome
        holder.tvData.text = formatarData(item.createdAt)
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.ivDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size

    private fun formatarData(iso: String) = try {
        val p = iso.substring(0, 10).split("-"); "${p[2]}/${p[1]}/${p[0]}"
    } catch (_: Exception) { iso }
}
