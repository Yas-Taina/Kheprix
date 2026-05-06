package com.kheprix.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityEventosBinding
import com.kheprix.databinding.ActivityNovoEventoBinding
import com.kheprix.db.CampanhaDao
import com.kheprix.db.EstudoDao
import com.kheprix.db.EventoDao
import com.kheprix.db.OfflineRepository
import com.kheprix.db.UnidadeDao
import com.kheprix.models.EventoRequest
import com.kheprix.models.EventoResponse
import com.kheprix.models.ValorVariavelRequest
import com.kheprix.models.VariavelResponse
import kotlinx.coroutines.launch
import java.util.Calendar

// ════════════════════════════════════════════════════════════════════════════
// LISTA DE EVENTOS
// ════════════════════════════════════════════════════════════════════════════

/**
 * Lista de Eventos de Amostragem de uma Unidade Amostral.
 *
 * "Visualizar Detalhes" → card inline com dados da unidade + botão editar.
 * Clique no evento → RegistrosActivity.
 *
 * Extras: estudo_remote_id, campanha_id, unidade_id, unidade_nome
 */
class EventosActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityEventosBinding
    private var estudoRemoteId = -1
    private var estudoLocalId  = -1L
    private var campanhaId     = -1
    private var campanhaLocalId = -1L
    private var unidadeId      = -1
    private var unidadeLocalId = -1L
    private var unidadeNome    = ""
    private val eventos = mutableListOf<EventoResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId  = intent.getLongExtra("estudo_local_id", -1L)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        campanhaLocalId = intent.getLongExtra("campanha_local_id", -1L)
        if (campanhaId < 0 && campanhaLocalId <= 0) campanhaLocalId = (-campanhaId).toLong()
        unidadeId      = intent.getIntExtra("unidade_id", -1)
        unidadeLocalId = intent.getLongExtra("unidade_local_id", -1L)
        if (unidadeId < 0 && unidadeLocalId <= 0) unidadeLocalId = (-unidadeId).toLong()
        unidadeNome    = intent.getStringExtra("unidade_nome") ?: ""

        binding.tvUnidadeNome.text = unidadeNome

        val adapter = EventoAdapter(eventos,
            onItemClick   = { e -> abrirRegistros(e) },
            onDeleteClick = { e -> confirmarDelete(e) }
        )
        binding.rvEventos.layoutManager = LinearLayoutManager(this)
        binding.rvEventos.adapter = adapter

        binding.btnAdicionarEvento.setOnClickListener {
            startActivity(Intent(this, NovoEventoActivity::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("estudo_local_id", estudoLocalId)
                putExtra("campanha_id", campanhaId)
                putExtra("campanha_local_id", campanhaLocalId)
                putExtra("unidade_id", unidadeId)
                putExtra("unidade_local_id", unidadeLocalId)
            })
        }

        binding.btnVisualizarDetalhes.setOnClickListener {
            val vis = binding.cardDetalhesUnidade.visibility == View.VISIBLE
            binding.cardDetalhesUnidade.visibility = if (vis) View.GONE else View.VISIBLE
        }

        binding.btnEditarUnidade.setOnClickListener {
            startActivity(Intent(this, NovaUnidadeActivity::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("estudo_local_id", estudoLocalId)
                putExtra("campanha_id", campanhaId)
                putExtra("campanha_local_id", campanhaLocalId)
                putExtra("unidade_id", unidadeId)
                putExtra("unidade_local_id", unidadeLocalId)
            })
        }

        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarDetalhes()
        carregarEventos()
    }

    override fun onResume() {
        super.onResume()
        carregarDetalhes()
        carregarEventos()
    }

    private fun carregarDetalhes() {
        lifecycleScope.launch {
            // Tenta API quando temos remote id da unidade.
            if (unidadeId > 0 && estudoRemoteId > 0 && campanhaId > 0) {
                try {
                    val resp = RetrofitClient.apiService.getUnidade(
                        SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId
                    )
                    resp.body()?.let { u ->
                        unidadeNome = u.nome
                        binding.tvUnidadeNome.text = u.nome
                        preencherCardUnidade(u.nome, u.latitude, u.longitude, u.raio, u.metodoColeta, u.esforcoAmostral, u.updatedAt)
                        return@launch
                    }
                } catch (_: Exception) { }
            }
            // Fallback offline: SQLite.
            preencherDetalhesOffline()
        }
    }

    private fun preencherCardUnidade(
        nome: String, lat: Double, lon: Double,
        raio: Double?, metodo: String?, esforco: String?, updatedAt: String? = null
    ) {
        binding.tvDetalheNome.text      = nome
        binding.tvDetalheLat.text       = decimalToDms(lat)
        binding.tvDetalheLon.text       = decimalToDms(lon)
        binding.tvDetalheRaio.text      = raio?.let { "${it}m" } ?: "—"
        binding.tvDetalheMetodo.text    = metodo ?: "—"
        binding.tvDetalheEsforco.text   = esforco ?: "—"
        binding.tvDetalheUpdatedAt.text = updatedAt?.let { formatarDataHora(it) } ?: "—"
    }

    private fun preencherDetalhesOffline() {
        val unidadeDao = UnidadeDao(this)
        val campanhaDao = CampanhaDao(this)
        val estudoDao = EstudoDao(this)

        val campanhaLocalId = if (campanhaId > 0) {
            estudoDao.buscarPorRemoteId(estudoRemoteId)?.localId?.let { estudoLocal ->
                campanhaDao.buscarPorRemoteIdEscopo(campanhaId, estudoLocal)?.localId
            }
        } else null

        val unidade = when {
            unidadeId > 0 && campanhaLocalId != null ->
                unidadeDao.buscarPorRemoteIdEscopo(unidadeId, campanhaLocalId)
            campanhaLocalId != null ->
                unidadeDao.listarPorCampanhaLocal(campanhaLocalId).firstOrNull { it.nome == unidadeNome }
            else ->
                unidadeDao.listarTodos().firstOrNull { it.nome == unidadeNome }
        } ?: return

        preencherCardUnidade(
            unidade.nome, unidade.latitude, unidade.longitude,
            unidade.raio, unidade.metodoColeta, unidade.esforcoAmostral, unidade.updatedAt
        )
    }

    private fun carregarEventos() {
        // Unidade offline-only: lista do SQLite.
        if (unidadeId <= 0) {
            val localId = if (unidadeLocalId > 0) unidadeLocalId
                else {
                    val campanha = CampanhaDao(this).listarTodos()
                        .firstOrNull { it.remoteId == campanhaId || (campanhaId <= 0) }
                    campanha?.let {
                        UnidadeDao(this).listarPorCampanhaLocal(it.localId).firstOrNull { u -> u.nome == unidadeNome }?.localId
                    }
                }
            if (localId != null) {
                unidadeLocalId = localId
                carregarEventosOffline(localId)
            }
            return
        }

        lifecycleScope.launch {
            val eventosOnline = mutableListOf<EventoResponse>()

            // Tenta carregar da API (se online)
            try {
                val resp = RetrofitClient.apiService.getEventos(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId
                )
                if (resp.isSuccessful) {
                    eventosOnline.addAll(resp.body() ?: emptyList())
                }
            } catch (_: Exception) { }

            eventos.clear()
            eventos.addAll(eventosOnline)

            // Tenta mesclar eventos offline-only. Se qualquer pai não estiver
            // no SQLite, pula o merge mas mantém os eventos online visíveis.
            val estudoLocalId = EstudoDao(this@EventosActivity).buscarPorRemoteId(estudoRemoteId)?.localId
            val campanhaLocalId = estudoLocalId?.let {
                CampanhaDao(this@EventosActivity).buscarPorRemoteIdEscopo(campanhaId, it)?.localId
            }
            val unidadeLocalId = campanhaLocalId?.let {
                UnidadeDao(this@EventosActivity).buscarPorRemoteIdEscopo(unidadeId, it)?.localId
            }

            if (unidadeLocalId != null) {
                val eventoDao = EventoDao(this@EventosActivity)
                val offline = eventoDao.listarPorUnidadeLocal(unidadeLocalId)
                val remoteIds = eventosOnline.mapNotNull { it.id }.toSet()
                offline.forEach { off ->
                    if (off.remoteId == null || !remoteIds.contains(off.remoteId)) {
                        eventos.add(EventoResponse(
                            // Eventos offline-only ficam com id = -localId para
                            // serem rastreáveis pelo SQLite nas telas seguintes.
                            id = off.remoteId ?: -off.localId.toInt(),
                            unidadeAmostralId = off.unidadeLocalId.toInt(),
                            horarioInicio = off.horarioInicio,
                            horarioFim = off.horarioFim,
                            esforcoReal = off.esforcoReal,
                            createdAt = off.createdAt ?: ""
                        ))
                    }
                }
            }

            binding.rvEventos.adapter?.notifyDataSetChanged()
        }
    }

    private fun carregarEventosOffline(unidadeLocalId: Long) {
        val eventoDao = EventoDao(this)
        eventos.clear()
        eventos.addAll(eventoDao.listarPorUnidadeLocal(unidadeLocalId).map { off ->
            EventoResponse(
                id = off.remoteId ?: -off.localId.toInt(),
                unidadeAmostralId = off.unidadeLocalId.toInt(),
                horarioInicio = off.horarioInicio,
                horarioFim = off.horarioFim,
                esforcoReal = off.esforcoReal,
                createdAt = off.createdAt ?: ""
            )
        })
        binding.rvEventos.adapter?.notifyDataSetChanged()
    }

    private fun abrirRegistros(e: EventoResponse) {
        val eventoLocalId = if (e.id < 0) (-e.id).toLong()
            else if (unidadeLocalId > 0) EventoDao(this).buscarPorRemoteIdEscopo(e.id, unidadeLocalId)?.localId ?: -1L
            else -1L
        startActivity(Intent(this, RegistrosActivity::class.java).apply {
            putExtra("estudo_remote_id", estudoRemoteId)
            putExtra("estudo_local_id", estudoLocalId)
            putExtra("campanha_id", campanhaId)
            putExtra("campanha_local_id", campanhaLocalId)
            putExtra("unidade_id", unidadeId)
            putExtra("unidade_local_id", unidadeLocalId)
            putExtra("evento_id", e.id)
            putExtra("evento_local_id", eventoLocalId)
            putExtra("evento_nome", e.horarioInicio)
        })
    }

    private fun confirmarDelete(e: EventoResponse) {
        AlertDialog.Builder(this)
            .setTitle("Deletar evento")
            .setMessage("Deletar evento iniciado em ${e.horarioInicio}?")
            .setPositiveButton("Deletar") { _, _ ->
                if (e.id < 0 || unidadeId <= 0 || campanhaId <= 0 || estudoRemoteId <= 0) {
                    val localId = if (e.id < 0) (-e.id).toLong()
                        else if (unidadeLocalId > 0) EventoDao(this).buscarPorRemoteIdEscopo(e.id, unidadeLocalId)?.localId
                        else null
                    if (localId != null) {
                        com.kheprix.db.DatabaseHelper(this).writableDatabase
                            .delete("eventos_amostragem", "local_id = ?", arrayOf(localId.toString()))
                        carregarEventos()
                    }
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        RetrofitClient.apiService.deleteEvento(
                            SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, e.id
                        )
                        carregarEventos()
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun decimalToDms(dec: Double): String {
        val neg = dec < 0; val abs = Math.abs(dec)
        val deg = abs.toInt(); val minD = (abs - deg) * 60
        val min = minD.toInt(); val sec = ((minD - min) * 60).toInt()
        return "${if (neg) "-" else ""}${deg}°${min}'${sec}\""
    }

    private fun formatarDataHora(iso: String): String = try {
        val s = iso.replace("T", " ")
        val partes = s.split(" ")
        val d = partes[0].split("-")
        val h = if (partes.size > 1) partes[1].take(5) else ""
        val data = if (d.size == 3) "${d[2]}/${d[1]}/${d[0]}" else partes[0]
        if (h.isNotEmpty()) "$data, ${h}h" else data
    } catch (_: Exception) { iso }
}

class EventoAdapter(
    private val items: List<EventoResponse>,
    private val onItemClick: (EventoResponse) -> Unit,
    private val onDeleteClick: (EventoResponse) -> Unit
) : RecyclerView.Adapter<EventoAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView    = view.findViewById(R.id.tvEventoNome)
        val tvData: TextView    = view.findViewById(R.id.tvEventoData)
        val ivDelete: ImageView = view.findViewById(R.id.ivDeleteEvento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNome.text = "Evento de amostragem"
        holder.tvData.text = formatarDataHora(item.horarioInicio)
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.ivDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size

    private fun formatarDataHora(iso: String): String = try {
        val s = iso.replace("T", " ")
        val partes = s.split(" ")
        val d = partes[0].split("-")
        val h = if (partes.size > 1) partes[1].take(5) else ""
        val data = if (d.size == 3) "${d[2]}/${d[1]}/${d[0]}" else partes[0]
        if (h.isNotEmpty()) "$data, ${h}h" else data
    } catch (_: Exception) { iso }
}

// ════════════════════════════════════════════════════════════════════════════
// NOVO EVENTO
// ════════════════════════════════════════════════════════════════════════════

/**
 * Cadastro e edição de Evento de Amostragem.
 *
 * Campos:
 *  - Data de Início (DatePicker) + Hora de Início (TimePicker)
 *  - Esforço Real
 *  - Variáveis de nível "evento" do estudo (dinâmicas)
 *
 * Extras: estudo_remote_id, campanha_id, unidade_id, evento_id (-1 novo)
 */
class NovoEventoActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityNovoEventoBinding
    private var estudoRemoteId = -1
    private var estudoLocalId  = -1L
    private var campanhaId     = -1
    private var campanhaLocalId = -1L
    private var unidadeId      = -1
    private var unidadeLocalId = -1L
    private var eventoId       = -1
    private var modoEdicao     = false

    private var dataInicio = ""
    private var horaInicio = ""

    private val variaveis = mutableListOf<VariavelResponse>()
    /** Para tipo "boolean" é Spinner; demais tipos é EditText. */
    private val camposVariavel = mutableMapOf<Int, View>()

    private var eventoCarregado: EventoResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovoEventoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId  = intent.getLongExtra("estudo_local_id", -1L)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        campanhaLocalId = intent.getLongExtra("campanha_local_id", -1L)
        if (campanhaId < 0 && campanhaLocalId <= 0) campanhaLocalId = (-campanhaId).toLong()
        unidadeId      = intent.getIntExtra("unidade_id", -1)
        unidadeLocalId = intent.getLongExtra("unidade_local_id", -1L)
        if (unidadeId < 0 && unidadeLocalId <= 0) unidadeLocalId = (-unidadeId).toLong()
        eventoId       = intent.getIntExtra("evento_id", -1)
        modoEdicao     = eventoId != -1

        binding.tvTitulo.text = if (modoEdicao) "Editar Evento" else "Novo Evento de Amostragem"

        binding.ivCalendario.setOnClickListener { abrirDatePicker() }
        binding.etDataInicio.setOnClickListener { abrirDatePicker() }
        binding.etHoraInicio.setOnClickListener { abrirTimePicker() }

        binding.btnConfirmar.setOnClickListener {
            if (modoEdicao) editarEvento() else criarEvento()
        }
        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        if (modoEdicao) preencherEdicao()
        carregarVariaveis()
    }

    private fun abrirDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            dataInicio = "%04d-%02d-%02d".format(y, m + 1, d)
            binding.etDataInicio.setText("%02d/%02d/%04d".format(d, m + 1, y))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun abrirTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            horaInicio = "%02d:%02d:00".format(h, m)
            binding.etHoraInicio.setText("%02d:%02d".format(h, m))
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun carregarVariaveis() {
        if (estudoRemoteId <= 0) { carregarVariaveisOffline(); return }
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getVariaveis(
                    SessionManager.getAuthHeader(), estudoRemoteId, nivelAplicacao = "evento"
                )
                if (resp.isSuccessful) {
                    variaveis.clear()
                    variaveis.addAll(resp.body() ?: emptyList())
                    renderizarVariaveis()
                } else carregarVariaveisOffline()
            } catch (_: Exception) { carregarVariaveisOffline() }
        }
    }

    private fun carregarVariaveisOffline() {
        if (estudoLocalId <= 0) { renderizarVariaveis(); return }
        val db = com.kheprix.db.DatabaseHelper(this).readableDatabase
        variaveis.clear()
        db.rawQuery(
            "SELECT remote_id, nome, nivel_aplicacao, tipo_dado, metrica, created_at, updated_at, local_id FROM variaveis WHERE estudo_local_id = ? AND nivel_aplicacao = 'evento'",
            arrayOf(estudoLocalId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val rid = if (c.isNull(0)) -c.getLong(7).toInt() else c.getInt(0)
                variaveis.add(VariavelResponse(
                    id = rid,
                    nome = c.getString(1),
                    nivelAplicacao = c.getString(2),
                    tipoDado = c.getString(3),
                    metrica = c.getString(4),
                    createdAt = c.getString(5) ?: "",
                    updatedAt = c.getString(6) ?: ""
                ))
            }
        }
        renderizarVariaveis()
    }

    private fun renderizarVariaveis() {
        binding.layoutVariaveis.removeAllViews(); camposVariavel.clear()
        if (variaveis.isEmpty()) { binding.tvVariaveisTitle.visibility = View.GONE; return }
        binding.tvVariaveisTitle.visibility = View.VISIBLE

        variaveis.forEachIndexed { i, v ->
            val label = TextView(this).apply {
                text = "${v.nome}:"; textSize = 13f
                setTextColor(0xFF6B7A5E.toInt())
                setPadding(0, if (i > 0) 12 else 0, 0, 0)
                typeface = android.graphics.Typeface.MONOSPACE
            }
            binding.layoutVariaveis.addView(label)
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 4 }
            }
            val campo: View = criarCampoVariavel(v.tipoDado)
            camposVariavel[v.id] = campo; linha.addView(campo)
            if (!v.metrica.isNullOrEmpty()) {
                linha.addView(TextView(this).apply {
                    text = v.metrica; textSize = 14f
                    setTextColor(0xFF4A5240.toInt()); setPadding(10, 0, 0, 0)
                    typeface = android.graphics.Typeface.MONOSPACE
                })
            }
            binding.layoutVariaveis.addView(linha)
        }

        aplicarValoresVariaveis()
    }

    private fun aplicarValoresVariaveis() {
        val e = eventoCarregado ?: return
        if (camposVariavel.isEmpty()) return
        e.valoresVariaveis?.forEach { vv ->
            when (val view = camposVariavel[vv.variavelId]) {
                is Spinner -> view.setSelection(when (vv.valor.trim().lowercase()) {
                    "true", "verdadeiro" -> 1
                    "false", "falso"     -> 2
                    else                 -> 0
                })
                is EditText -> view.setText(vv.valor)
                else -> {}
            }
        }
    }

    /** Cria a view de entrada adequada ao tipoDado da variável. */
    private fun criarCampoVariavel(tipoDado: String): View {
        val lp = LinearLayout.LayoutParams(0, (48 * resources.displayMetrics.density).toInt(), 1f)
        val bg = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_field_green)
        return when (tipoDado) {
            "boolean" -> Spinner(this).apply {
                layoutParams = lp
                background = bg
                setPadding(20, 0, 20, 0)
                adapter = ArrayAdapter(
                    this@NovoEventoActivity,
                    android.R.layout.simple_spinner_item,
                    listOf("—", "Verdadeiro", "Falso")
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
            else -> EditText(this).apply {
                layoutParams = lp
                background = bg
                setPadding(20, 0, 20, 0); setTextColor(0xFF4A5240.toInt()); hint = "Placeholder"
                inputType = when (tipoDado) {
                    "number" -> android.text.InputType.TYPE_CLASS_NUMBER or
                            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                            android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                    else -> android.text.InputType.TYPE_CLASS_TEXT
                }
            }
        }
    }

    private fun preencherEdicao() {
        if (estudoRemoteId <= 0 || campanhaId <= 0 || unidadeId <= 0 || eventoId <= 0) return
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEvento(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, eventoId
                )
                resp.body()?.let { e ->
                    eventoCarregado = e
                    preencherDataHora(e.horarioInicio)
                    binding.etEsforcoReal.setText(e.esforcoReal ?: "")
                    aplicarValoresVariaveis()
                }
            } catch (_: Exception) {}
        }
    }

    private fun criarEvento() {
        val req = coletarFormulario() ?: return
        // Pais offline-only: salva direto no SQLite.
        if (estudoRemoteId <= 0 || campanhaId <= 0 || unidadeId <= 0) {
            salvarEventoOffline(req)
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.postEvento(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, req
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@NovoEventoActivity, "Evento criado!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NovoEventoActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                salvarEventoOffline(req)
            } finally { setLoading(false) }
        }
    }

    /**
     * Persiste o evento no SQLite. Requer que estudo→campanha→unidade estejam
     * salvos offline — se algum faltar, aborta e NÃO grava órfão.
     */
    private fun salvarEventoOffline(req: EventoRequest) {
        val repo = OfflineRepository(this)
        val unidadeResolved = when {
            unidadeLocalId > 0 -> unidadeLocalId
            estudoRemoteId > 0 && campanhaId > 0 && unidadeId > 0 ->
                repo.estudoLocalIdFromRemote(estudoRemoteId)?.let { eL ->
                    repo.campanhaLocalIdFromRemote(eL, campanhaId)?.let { cL ->
                        repo.unidadeLocalIdFromRemote(cL, unidadeId)
                    }
                }
            else -> null
        }
        if (unidadeResolved == null) {
            Toast.makeText(this, "Unidade não está salva offline.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            repo.criarEventoOffline(unidadeResolved, req)
            Toast.makeText(this, "Evento salvo offline.", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar offline: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun editarEvento() {
        val req = coletarFormulario() ?: return
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.patchEvento(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, eventoId, req
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@NovoEventoActivity, "Evento atualizado!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NovoEventoActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@NovoEventoActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            } finally { setLoading(false) }
        }
    }

    private fun coletarFormulario(): EventoRequest? {
        val iniIso = montarIso(dataInicio, horaInicio, binding.etDataInicio.text.toString().trim(), binding.etHoraInicio.text.toString().trim())
        if (iniIso == null) {
            Toast.makeText(this, "Preencha data e hora de início", Toast.LENGTH_SHORT).show()
            return null
        }
        return EventoRequest(
            horarioInicio = iniIso,
            horarioFim = null,
            esforcoReal = binding.etEsforcoReal.text.toString().trim().ifEmpty { null },
            valoresVariaveis = coletarValoresVariaveis()
        )
    }

    private fun coletarValoresVariaveis(): List<ValorVariavelRequest>? {
        if (camposVariavel.isEmpty()) return null
        val idPorVariavel: Map<Int, Int> =
            eventoCarregado?.valoresVariaveis
                ?.mapNotNull { vv -> vv.id?.let { vv.variavelId to it } }
                ?.toMap() ?: emptyMap()

        val itens = camposVariavel.entries.mapNotNull { (varId, view) ->
            val valor = when (view) {
                is Spinner -> when (view.selectedItem?.toString()) {
                    "Verdadeiro" -> "true"
                    "Falso"      -> "false"
                    else         -> null
                }
                is EditText -> view.text.toString().trim().ifEmpty { null }
                else -> null
            } ?: return@mapNotNull null

            if (modoEdicao) {
                val existingId = idPorVariavel[varId]
                if (existingId != null)
                    ValorVariavelRequest(id = existingId, variavelId = varId, valor = valor)
                else
                    ValorVariavelRequest(variavelId = varId, valor = valor)
            } else {
                ValorVariavelRequest(variavelId = varId, valor = valor)
            }
        }
        return itens.ifEmpty { null }
    }

    /** Combina data/hora em ISO "yyyy-MM-ddTHH:mm:ss"; retorna null se faltar algum. */
    private fun montarIso(dataIso: String, horaIso: String, dataExib: String, horaExib: String): String? {
        val dataFinal = dataIso.ifEmpty {
            val p = dataExib.split("/")
            if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}" else ""
        }
        val horaFinal = horaIso.ifEmpty {
            if (horaExib.matches(Regex("\\d{2}:\\d{2}"))) "$horaExib:00" else ""
        }
        if (dataFinal.isEmpty() || horaFinal.isEmpty()) return null
        return "${dataFinal}T${horaFinal}"
    }

    /** Pré-preenche data/hora de início a partir de string ISO do backend. */
    private fun preencherDataHora(iso: String) {
        val partes = iso.split("T")
        if (partes.size < 2) return
        val dp = partes[0].split("-")
        if (dp.size != 3) return
        dataInicio = partes[0]; horaInicio = partes[1].take(8)
        binding.etDataInicio.setText("${dp[2]}/${dp[1]}/${dp[0]}")
        binding.etHoraInicio.setText(partes[1].take(5))
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }
}
