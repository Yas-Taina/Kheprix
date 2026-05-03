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
import com.kheprix.databinding.ActivityUnidadesBinding
import com.kheprix.db.CampanhaDao
import com.kheprix.db.EstudoDao
import com.kheprix.db.OfflineRepository
import com.kheprix.db.UnidadeDao
import com.kheprix.models.UnidadeResponse
import kotlinx.coroutines.launch

/**
 * Lista de Unidades Amostrais de uma Campanha.
 *
 * Exibe:
 *  - Botão "Adicionar Unidade" → NovaUnidadeActivity
 *  - Botão "Visualizar Detalhes" → toggle card inline com dados da campanha + botão editar
 *  - Lista: nome da unidade + coordenadas formatadas em DMS, lixeira
 *
 * Extras recebidos:
 *   estudo_remote_id  → Int
 *   campanha_id       → Int
 *   campanha_nome     → String
 *   estudo_nome       → String
 */
class UnidadesActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityUnidadesBinding
    private var estudoRemoteId = -1
    private var estudoLocalId  = -1L
    private var campanhaId     = -1
    private var campanhaLocalId = -1L
    private var campanhaNome   = ""
    private var estudoNome     = ""
    private val unidades = mutableListOf<UnidadeResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnidadesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId  = intent.getLongExtra("estudo_local_id", -1L)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        campanhaLocalId = intent.getLongExtra("campanha_local_id", -1L)
        campanhaNome   = intent.getStringExtra("campanha_nome") ?: ""
        estudoNome     = intent.getStringExtra("estudo_nome") ?: ""

        // Decodifica negativo: campanhaId codificado como -localId.
        if (campanhaId < 0 && campanhaLocalId <= 0) campanhaLocalId = (-campanhaId).toLong()

        binding.tvCampanhaNome.text = campanhaNome

        val adapter = UnidadeAdapter(unidades,
            onItemClick = { u -> abrirEventos(u) },
            onDeleteClick = { u -> confirmarDelete(u) }
        )
        binding.rvUnidades.layoutManager = LinearLayoutManager(this)
        binding.rvUnidades.adapter = adapter

        binding.btnAdicionarUnidade.setOnClickListener {
            startActivity(Intent(this, NovaUnidadeActivity::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("estudo_local_id", estudoLocalId)
                putExtra("campanha_id", campanhaId)
                putExtra("campanha_local_id", campanhaLocalId)
            })
        }

        // "Visualizar Detalhes" → toggle card da campanha
        binding.btnVisualizarDetalhes.setOnClickListener {
            val visivel = binding.cardDetalhesCampanha.visibility == View.VISIBLE
            binding.cardDetalhesCampanha.visibility = if (visivel) View.GONE else View.VISIBLE
        }

        // Botão editar dentro do card de detalhes
        binding.btnEditarCampanha.setOnClickListener {
            startActivity(Intent(this, NovaCampanhaActivityV2::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("estudo_local_id", estudoLocalId)
                putExtra("campanha_id", campanhaId)
                putExtra("campanha_local_id", campanhaLocalId)
                putExtra("campanha_nome", campanhaNome)
            })
        }

        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarDetalhes()
        carregarUnidades()
    }

    override fun onResume() {
        super.onResume()
        carregarDetalhes()
        carregarUnidades()
    }

    private fun carregarDetalhes() {
        // Campanha offline-only: lê do SQLite.
        if (estudoRemoteId <= 0 || campanhaId <= 0) {
            preencherDetalhesOffline()
            return
        }
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getCampanha(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId
                )
                resp.body()?.let { c ->
                    binding.tvDetalheNome.text      = c.nome
                    binding.tvDetalheInicio.text    = formatarData(c.dataInicio)
                    binding.tvDetalheDescricao.text = c.descricao ?: "—"
                } ?: preencherDetalhesOffline()
            } catch (_: Exception) { preencherDetalhesOffline() }
        }
    }

    private fun preencherDetalhesOffline() {
        if (campanhaLocalId <= 0) return
        val c = CampanhaDao(this).listarTodos().firstOrNull { it.localId == campanhaLocalId } ?: return
        binding.tvDetalheNome.text      = c.nome
        binding.tvDetalheInicio.text    = formatarData(c.dataInicio)
        binding.tvDetalheDescricao.text = c.descricao ?: "—"
    }

    private fun carregarUnidades() {
        // Campanha offline-only: lista do SQLite.
        if (campanhaId <= 0) {
            val localId = if (campanhaLocalId > 0) campanhaLocalId
                else CampanhaDao(this).listarTodos().firstOrNull { it.nome == campanhaNome }?.localId
            if (localId != null) {
                campanhaLocalId = localId
                carregarUnidadesOffline(localId)
            }
            return
        }

        lifecycleScope.launch {
            val unidadesOnline = mutableListOf<UnidadeResponse>()

            // Tenta carregar da API (se online)
            try {
                val resp = RetrofitClient.apiService.getUnidades(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId
                )
                if (resp.isSuccessful) {
                    unidadesOnline.addAll(resp.body() ?: emptyList())
                }
            } catch (_: Exception) { }

            // Merge com SQLite
            val unidadeDao = UnidadeDao(this@UnidadesActivity)
            val repo = OfflineRepository(this@UnidadesActivity)
            // Resolve estudo localId; se não existir, tenta espelhar da API
            // para permitir cache das unidades carregadas online.
            var estudoLocalId = EstudoDao(this@UnidadesActivity).buscarPorRemoteId(estudoRemoteId)?.localId
            if (estudoLocalId == null && unidadesOnline.isNotEmpty()) {
                try {
                    val estudo = RetrofitClient.apiService.getEstudos(SessionManager.getAuthHeader())
                        .body()?.firstOrNull { it.id == estudoRemoteId }
                    if (estudo != null) estudoLocalId = repo.cacheEstudo(estudo)
                } catch (_: Exception) { }
            }
            if (estudoLocalId == null) return@launch

            // Espelha a campanha no SQLite (caso ainda não exista), usando
            // dados da API; sem isso, as unidades online não podem ser associadas.
            var campanhaLocalId = CampanhaDao(this@UnidadesActivity)
                .buscarPorRemoteIdEscopo(campanhaId, estudoLocalId!!)?.localId
            if (campanhaLocalId == null && unidadesOnline.isNotEmpty()) {
                try {
                    val camp = RetrofitClient.apiService.getCampanha(
                        SessionManager.getAuthHeader(), estudoRemoteId, campanhaId
                    ).body()
                    if (camp != null) campanhaLocalId = repo.cacheCampanha(estudoLocalId!!, camp)
                } catch (_: Exception) { }
            }
            if (campanhaLocalId == null) return@launch

            // Espelha as unidades carregadas online.
            unidadesOnline.forEach { u ->
                try { repo.cacheUnidade(campanhaLocalId!!, u) } catch (_: Exception) { }
            }

            val offline = unidadeDao.listarPorCampanhaLocal(campanhaLocalId!!)
            val remoteIds = unidadesOnline.mapNotNull { it.id }.toSet()

            unidades.clear()
            unidades.addAll(unidadesOnline)

            // Adicionar offline-only (não presentes na API)
            offline.forEach { off ->
                if (off.remoteId == null || !remoteIds.contains(off.remoteId)) {
                    unidades.add(UnidadeResponse(
                        id = off.remoteId ?: -off.localId.toInt(),
                        campanhaId = off.campanhaLocalId.toInt(),
                        nome = off.nome,
                        latitude = off.latitude,
                        longitude = off.longitude,
                        raio = off.raio,
                        metodoColeta = off.metodoColeta,
                        esforcoAmostral = off.esforcoAmostral,
                        createdAt = off.createdAt ?: "",
                        updatedAt = off.updatedAt ?: ""
                    ))
                }
            }

            binding.rvUnidades.adapter?.notifyDataSetChanged()
        }
    }

    private fun carregarUnidadesOffline(campanhaLocalId: Long) {
        val unidadeDao = UnidadeDao(this)
        unidades.clear()
        unidades.addAll(unidadeDao.listarPorCampanhaLocal(campanhaLocalId).map { off ->
            UnidadeResponse(
                id = off.remoteId ?: -off.localId.toInt(),
                campanhaId = off.campanhaLocalId.toInt(),
                nome = off.nome,
                latitude = off.latitude,
                longitude = off.longitude,
                raio = off.raio,
                metodoColeta = off.metodoColeta,
                esforcoAmostral = off.esforcoAmostral,
                createdAt = off.createdAt ?: "",
                updatedAt = off.updatedAt ?: ""
            )
        })
        binding.rvUnidades.adapter?.notifyDataSetChanged()
    }

    private fun abrirEventos(u: UnidadeResponse) {
        val unidadeLocalId = if (u.id < 0) (-u.id).toLong()
            else if (campanhaLocalId > 0) UnidadeDao(this).buscarPorRemoteIdEscopo(u.id, campanhaLocalId)?.localId ?: -1L
            else -1L
        startActivity(Intent(this, EventosActivity::class.java).apply {
            putExtra("estudo_remote_id", estudoRemoteId)
            putExtra("estudo_local_id", estudoLocalId)
            putExtra("campanha_id", campanhaId)
            putExtra("campanha_local_id", campanhaLocalId)
            putExtra("unidade_id", u.id)
            putExtra("unidade_local_id", unidadeLocalId)
            putExtra("unidade_nome", u.nome)
        })
    }

    private fun confirmarDelete(u: UnidadeResponse) {
        AlertDialog.Builder(this)
            .setTitle("Deletar unidade")
            .setMessage("Deletar \"${u.nome}\"?")
            .setPositiveButton("Deletar") { _, _ ->
                if (u.id < 0 || campanhaId <= 0 || estudoRemoteId <= 0) {
                    val localId = if (u.id < 0) (-u.id).toLong()
                        else if (campanhaLocalId > 0) UnidadeDao(this).buscarPorRemoteIdEscopo(u.id, campanhaLocalId)?.localId
                        else null
                    if (localId != null) {
                        com.kheprix.db.DatabaseHelper(this).writableDatabase
                            .delete("unidades_amostrais", "local_id = ?", arrayOf(localId.toString()))
                        carregarUnidades()
                    }
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        RetrofitClient.apiService.deleteUnidade(
                            SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, u.id
                        )
                        carregarUnidades()
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun formatarData(iso: String) = try {
        val p = iso.substring(0, 10).split("-"); "${p[2]}/${p[1]}/${p[0]}"
    } catch (_: Exception) { iso }
}

// ── Adapter ──────────────────────────────────────────────────────────────────

class UnidadeAdapter(
    private val items: List<UnidadeResponse>,
    private val onItemClick: (UnidadeResponse) -> Unit,
    private val onDeleteClick: (UnidadeResponse) -> Unit
) : RecyclerView.Adapter<UnidadeAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView    = view.findViewById(R.id.tvUnidadeNome)
        val tvCoord: TextView   = view.findViewById(R.id.tvUnidadeCoord)
        val ivDelete: ImageView = view.findViewById(R.id.ivDeleteUnidade)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_unidade, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNome.text  = item.nome
        holder.tvCoord.text = "${decimalToDms(item.latitude)}, ${decimalToDms(item.longitude)}"
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.ivDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size

    /** Converte decimal (−25.43) para DMS "−25°25'48\"" */
    private fun decimalToDms(dec: Double): String {
        val neg  = dec < 0
        val abs  = Math.abs(dec)
        val deg  = abs.toInt()
        val minD = (abs - deg) * 60
        val min  = minD.toInt()
        val sec  = ((minD - min) * 60).toInt()
        return "${if (neg) "-" else ""}${deg}°${min}'${sec}\""
    }
}
