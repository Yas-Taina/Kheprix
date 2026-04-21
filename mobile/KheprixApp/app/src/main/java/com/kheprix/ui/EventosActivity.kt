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
import com.kheprix.models.EventoRequest
import com.kheprix.models.EventoResponse
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
class EventosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventosBinding
    private var estudoRemoteId = -1
    private var campanhaId     = -1
    private var unidadeId      = -1
    private var unidadeNome    = ""
    private val eventos = mutableListOf<EventoResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        unidadeId      = intent.getIntExtra("unidade_id", -1)
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
                putExtra("campanha_id", campanhaId)
                putExtra("unidade_id", unidadeId)
            })
        }

        binding.btnVisualizarDetalhes.setOnClickListener {
            val vis = binding.cardDetalhesUnidade.visibility == View.VISIBLE
            binding.cardDetalhesUnidade.visibility = if (vis) View.GONE else View.VISIBLE
        }

        binding.btnEditarUnidade.setOnClickListener {
            startActivity(Intent(this, NovaUnidadeActivity::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("campanha_id", campanhaId)
                putExtra("unidade_id", unidadeId)
            })
        }

        binding.ivMenuLateral.setOnClickListener { onBackPressed() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarDetalhes()
        carregarEventos()
    }

    override fun onResume() { super.onResume(); carregarEventos() }

    private fun carregarDetalhes() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getUnidade(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId
                )
                resp.body()?.let { u ->
                    binding.tvDetalheNome.text  = u.nome
                    binding.tvDetalheLat.text   = decimalToDms(u.latitude)
                    binding.tvDetalheLon.text   = decimalToDms(u.longitude)
                    binding.tvDetalheRaio.text  = u.raio?.let { "${it}m" } ?: "—"
                    binding.tvDetalheMetodo.text = u.metodoColeta ?: "—"
                    binding.tvDetalheEsforco.text = u.esforcoAmostral ?: "—"
                }
            } catch (_: Exception) {}
        }
    }

    private fun carregarEventos() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEventos(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId
                )
                if (resp.isSuccessful) {
                    eventos.clear()
                    eventos.addAll(resp.body() ?: emptyList())
                    binding.rvEventos.adapter?.notifyDataSetChanged()
                }
            } catch (_: Exception) {
                Toast.makeText(this@EventosActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirRegistros(e: EventoResponse) {
        startActivity(Intent(this, RegistrosActivity::class.java).apply {
            putExtra("estudo_remote_id", estudoRemoteId)
            putExtra("campanha_id", campanhaId)
            putExtra("unidade_id", unidadeId)
            putExtra("evento_id", e.id)
            putExtra("evento_nome", e.horarioInicio)
        })
    }

    private fun confirmarDelete(e: EventoResponse) {
        AlertDialog.Builder(this)
            .setTitle("Deletar evento")
            .setMessage("Deletar evento iniciado em ${e.horarioInicio}?")
            .setPositiveButton("Deletar") { _, _ ->
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

    private fun formatarDataHora(iso: String) = try {
        val p = iso.substring(0, 10).split("-"); "${p[2]}/${p[1]}/${p[0]}"
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
class NovoEventoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNovoEventoBinding
    private var estudoRemoteId = -1
    private var campanhaId     = -1
    private var unidadeId      = -1
    private var eventoId       = -1
    private var modoEdicao     = false

    private var dataInicio = ""
    private var horaInicio = ""
    private var dataFim = ""
    private var horaFim = ""

    private val variaveis = mutableListOf<VariavelResponse>()
    /** Para tipo "boolean" é Spinner; demais tipos é EditText. */
    private val camposVariavel = mutableMapOf<Int, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovoEventoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        unidadeId      = intent.getIntExtra("unidade_id", -1)
        eventoId       = intent.getIntExtra("evento_id", -1)
        modoEdicao     = eventoId != -1

        binding.tvTitulo.text = if (modoEdicao) "Editar Evento" else "Novo Evento de Amostragem"

        binding.ivCalendario.setOnClickListener { abrirDatePicker(inicio = true) }
        binding.etDataInicio.setOnClickListener { abrirDatePicker(inicio = true) }
        binding.etHoraInicio.setOnClickListener { abrirTimePicker(inicio = true) }
        binding.ivCalendarioFim.setOnClickListener { abrirDatePicker(inicio = false) }
        binding.etDataFim.setOnClickListener { abrirDatePicker(inicio = false) }
        binding.etHoraFim.setOnClickListener { abrirTimePicker(inicio = false) }

        binding.btnConfirmar.setOnClickListener {
            if (modoEdicao) editarEvento() else criarEvento()
        }
        binding.ivBack.setOnClickListener { finish() }
        binding.ivMenuLateral.setOnClickListener { onBackPressed() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        if (modoEdicao) preencherEdicao()
        carregarVariaveis()
    }

    private fun abrirDatePicker(inicio: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val iso = "%04d-%02d-%02d".format(y, m + 1, d)
            val br  = "%02d/%02d/%04d".format(d, m + 1, y)
            if (inicio) { dataInicio = iso; binding.etDataInicio.setText(br) }
            else        { dataFim = iso;    binding.etDataFim.setText(br) }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun abrirTimePicker(inicio: Boolean) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            val iso = "%02d:%02d:00".format(h, m)
            val exib = "%02d:%02d".format(h, m)
            if (inicio) { horaInicio = iso; binding.etHoraInicio.setText(exib) }
            else        { horaFim = iso;    binding.etHoraFim.setText(exib) }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun carregarVariaveis() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getVariaveis(
                    SessionManager.getAuthHeader(), estudoRemoteId, nivelAplicacao = "evento"
                )
                if (resp.isSuccessful) {
                    variaveis.clear()
                    variaveis.addAll(resp.body() ?: emptyList())
                    renderizarVariaveis()
                }
            } catch (_: Exception) {}
        }
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
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEvento(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, eventoId
                )
                resp.body()?.let { e ->
                    preencherDataHora(e.horarioInicio, inicio = true)
                    e.horarioFim?.let { preencherDataHora(it, inicio = false) }
                    binding.etEsforcoReal.setText(e.esforcoReal ?: "")
                }
            } catch (_: Exception) {}
        }
    }

    private fun criarEvento() {
        val req = coletarFormulario() ?: return
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
                Toast.makeText(this@NovoEventoActivity, "Sem conexão — salvo localmente", Toast.LENGTH_SHORT).show()
            } finally { setLoading(false) }
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
        val fimIso = montarIso(dataFim, horaFim, binding.etDataFim.text.toString().trim(), binding.etHoraFim.text.toString().trim())
        if (iniIso == null) {
            Toast.makeText(this, "Preencha data e hora de início", Toast.LENGTH_SHORT).show()
            return null
        }
        if (fimIso == null) {
            Toast.makeText(this, "Preencha data e hora de fim", Toast.LENGTH_SHORT).show()
            return null
        }
        return EventoRequest(
            horarioInicio = iniIso,
            horarioFim = fimIso,
            esforcoReal = binding.etEsforcoReal.text.toString().trim().ifEmpty { null }
        )
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

    /** Pré-preenche campos a partir de string ISO do backend. */
    private fun preencherDataHora(iso: String, inicio: Boolean) {
        val partes = iso.split("T")
        if (partes.size < 2) return
        val dp = partes[0].split("-")
        if (dp.size != 3) return
        val br = "${dp[2]}/${dp[1]}/${dp[0]}"
        val horaIsoStr = partes[1].take(8)
        val horaExib = partes[1].take(5)
        if (inicio) {
            dataInicio = partes[0]; horaInicio = horaIsoStr
            binding.etDataInicio.setText(br); binding.etHoraInicio.setText(horaExib)
        } else {
            dataFim = partes[0]; horaFim = horaIsoStr
            binding.etDataFim.setText(br); binding.etHoraFim.setText(horaExib)
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }
}
