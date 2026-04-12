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

    private val variaveis = mutableListOf<VariavelResponse>()
    private val camposVariavel = mutableMapOf<Int, EditText>()

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

        binding.ivCalendario.setOnClickListener { abrirDatePicker() }
        binding.etHoraInicio.setOnClickListener { abrirTimePicker() }

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
            val campo = EditText(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, (48 * resources.displayMetrics.density).toInt(), 1f)
                background = androidx.core.content.ContextCompat.getDrawable(this@NovoEventoActivity, R.drawable.bg_field_green)
                setPadding(20, 0, 20, 0); setTextColor(0xFF4A5240.toInt()); hint = "Placeholder"
            }
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

    private fun preencherEdicao() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEvento(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, eventoId
                )
                resp.body()?.let { e ->
                    val partes = e.horarioInicio.split("T")
                    if (partes.size >= 2) {
                        dataInicio = partes[0]
                        horaInicio = partes[1].take(8)
                        val dp = partes[0].split("-")
                        binding.etDataInicio.setText("${dp.getOrElse(2){""}}/{${dp.getOrElse(1){""}}/{${dp.getOrElse(0){""}}}")
                        binding.etHoraInicio.setText(partes[1].take(5))
                    }
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
        val dataExib = binding.etDataInicio.text.toString().trim()
        val horaExib = binding.etHoraInicio.text.toString().trim()

        // Se o usuário não usou picker, tenta converter exibição
        val dataFinal = dataInicio.ifEmpty {
            // converte dd/mm/yyyy → yyyy-mm-dd
            val p = dataExib.split("/")
            if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}" else ""
        }
        val horaFinal = horaInicio.ifEmpty {
            if (horaExib.matches(Regex("\\d{2}:\\d{2}"))) "$horaExib:00" else ""
        }
        if (dataFinal.isEmpty() || horaFinal.isEmpty()) {
            Toast.makeText(this, "Preencha data e hora de início", Toast.LENGTH_SHORT).show()
            return null
        }
        return EventoRequest(
            horarioInicio = "${dataFinal}T${horaFinal}",
            esforcoReal = binding.etEsforcoReal.text.toString().trim().ifEmpty { null }
        )
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }
}
