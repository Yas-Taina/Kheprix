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
import android.widget.LinearLayout
import com.kheprix.models.UnidadeResponse
import com.kheprix.models.ValorVariavelResponse
import kotlinx.coroutines.launch

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

        binding.btnVisualizarDetalhes.setOnClickListener {
            val visivel = binding.cardDetalhesCampanha.visibility == View.VISIBLE
            binding.cardDetalhesCampanha.visibility = if (visivel) View.GONE else View.VISIBLE
        }

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
                    binding.tvDetalheUpdatedAt.text = c.updatedAt?.let { formatarDataHora(it) } ?: "—"
                    renderizarValoresCard(c.valoresVariaveis, fetchVarNomesApi())
                } ?: preencherDetalhesOffline()
            } catch (_: Exception) { preencherDetalhesOffline() }
        }
    }

    private suspend fun fetchVarNomesApi(): Map<Int, Pair<String, String?>> {
        if (estudoRemoteId <= 0) return emptyMap()
        return try {
            RetrofitClient.apiService.getVariaveis(SessionManager.getAuthHeader(), estudoRemoteId)
                .body()?.associate { v -> v.id to (v.nome to v.metrica) } ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
    }

    private fun preencherDetalhesOffline() {
        if (campanhaLocalId <= 0) return
        val c = CampanhaDao(this).listarTodos().firstOrNull { it.localId == campanhaLocalId } ?: return
        binding.tvDetalheNome.text      = c.nome
        binding.tvDetalheInicio.text    = formatarData(c.dataInicio)
        binding.tvDetalheDescricao.text = c.descricao ?: "—"
        binding.tvDetalheUpdatedAt.text = c.updatedAt?.let { formatarDataHora(it) } ?: "—"
        val valoresOffline = mutableListOf<ValorVariavelResponse>()
        com.kheprix.db.DatabaseHelper(this).readableDatabase.rawQuery(
            "SELECT variavel_remote_id, variavel_local_id, valor FROM valores_variaveis WHERE campanha_local_id = ?",
            arrayOf(campanhaLocalId.toString())
        ).use { cur ->
            while (cur.moveToNext()) {
                val rid = if (cur.isNull(0)) null else cur.getInt(0)
                val lid = cur.getLong(1)
                val valor = cur.getString(2) ?: return@use
                val varId = if (rid != null && rid > 0) rid else -lid.toInt()
                valoresOffline.add(ValorVariavelResponse(variavelId = varId, valor = valor))
            }
        }
        renderizarValoresCard(valoresOffline.ifEmpty { null })
    }

    private fun renderizarValoresCard(
        valores: List<ValorVariavelResponse>?,
        apiNomes: Map<Int, Pair<String, String?>> = emptyMap()
    ) {
        binding.layoutVariaveisDetalhe.removeAllViews()
        if (valores.isNullOrEmpty()) return
        val varMap = mutableMapOf<Int, Pair<String, String?>>()
        varMap.putAll(apiNomes)
        com.kheprix.db.DatabaseHelper(this).readableDatabase.rawQuery(
            "SELECT remote_id, local_id, nome, metrica FROM variaveis", null
        ).use { c ->
            while (c.moveToNext()) {
                val rid = if (c.isNull(0)) null else c.getInt(0)
                val lid = c.getLong(1)
                val nome = c.getString(2)
                val metrica = if (c.isNull(3)) null else c.getString(3)
                if (rid != null) varMap[rid] = nome to metrica
                varMap[-lid.toInt()] = nome to metrica
            }
        }
        val dp = resources.displayMetrics.density
        binding.layoutVariaveisDetalhe.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                .also { it.topMargin = (6 * dp).toInt(); it.bottomMargin = (6 * dp).toInt() }
            setBackgroundColor(0xFFD0CCB8.toInt())
        })
        binding.layoutVariaveisDetalhe.addView(TextView(this).apply {
            text = "Variáveis:"; textSize = 12f; setTextColor(0xFF6B7A5E.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 0, 0, (2 * dp).toInt())
        })
        valores.forEach { vv ->
            val (nome, metrica) = varMap[vv.variavelId] ?: ("Variável ${vv.variavelId}" to null)
            binding.layoutVariaveisDetalhe.addView(TextView(this).apply {
                text = "$nome:"; textSize = 12f; setTextColor(0xFF6B7A5E.toInt())
                typeface = android.graphics.Typeface.MONOSPACE
            })
            binding.layoutVariaveisDetalhe.addView(TextView(this).apply {
                text = "${vv.valor}${if (!metrica.isNullOrBlank()) " $metrica" else ""}"
                textSize = 14f; setTextColor(0xFF4A5240.toInt())
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(0, 0, 0, (6 * dp).toInt())
            })
        }
    }

    private fun carregarUnidades() {
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

            try {
                val resp = RetrofitClient.apiService.getUnidades(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId
                )
                if (resp.isSuccessful) {
                    unidadesOnline.addAll(resp.body() ?: emptyList())
                }
            } catch (_: Exception) { }

            val unidadeDao = UnidadeDao(this@UnidadesActivity)
            val repo = OfflineRepository(this@UnidadesActivity)
            var estudoLocalId = EstudoDao(this@UnidadesActivity).buscarPorRemoteId(estudoRemoteId)?.localId
            if (estudoLocalId == null && unidadesOnline.isNotEmpty()) {
                try {
                    val estudo = RetrofitClient.apiService.getEstudos(SessionManager.getAuthHeader())
                        .body()?.firstOrNull { it.id == estudoRemoteId }
                    if (estudo != null) estudoLocalId = repo.cacheEstudo(estudo)
                } catch (_: Exception) { }
            }
            if (estudoLocalId == null) return@launch

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

            unidadesOnline.forEach { u ->
                try { repo.cacheUnidade(campanhaLocalId!!, u) } catch (_: Exception) { }
            }

            val offline = unidadeDao.listarPorCampanhaLocal(campanhaLocalId!!)
            val remoteIds = unidadesOnline.mapNotNull { it.id }.toSet()

            unidades.clear()
            unidades.addAll(unidadesOnline)

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
                        if (campanhaLocalId > 0) {
                            UnidadeDao(this@UnidadesActivity).buscarPorRemoteIdEscopo(u.id, campanhaLocalId)?.localId?.let { lid ->
                                com.kheprix.db.DatabaseHelper(this@UnidadesActivity).writableDatabase
                                    .delete("unidades_amostrais", "local_id = ?", arrayOf(lid.toString()))
                            }
                        }
                        carregarUnidades()
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun formatarData(iso: String) = try {
        val p = iso.substring(0, 10).split("-"); "${p[2]}/${p[1]}/${p[0]}"
    } catch (_: Exception) { iso }

    private fun formatarDataHora(iso: String): String = try {
        val s = iso.replace("T", " ")
        val partes = s.split(" ")
        val d = partes[0].split("-")
        val h = if (partes.size > 1) partes[1].take(5) else ""
        val data = if (d.size == 3) "${d[2]}/${d[1]}/${d[0]}" else partes[0]
        if (h.isNotEmpty()) "$data, ${h}h" else data
    } catch (_: Exception) { iso }
}

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
