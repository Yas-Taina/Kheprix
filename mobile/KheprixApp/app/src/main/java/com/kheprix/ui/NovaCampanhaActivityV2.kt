package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityNovaCampanhaV2Binding
import com.kheprix.models.CampanhaRequest
import com.kheprix.models.ValorVariavelRequest
import com.kheprix.models.VariavelResponse
import kotlinx.coroutines.launch

/**
 * Cadastro e edição de Campanha — com campos dinâmicos de variáveis.
 *
 * As variáveis de nível "campanha" cadastradas no estudo são carregadas
 * automaticamente via GET /estudos/:id/variaveis?nivel_aplicacao=campanha
 * e exibidas como campos de texto com a métrica ao lado (ex: °C).
 *
 * O formulário inclui:
 *  - Data de início (com DatePicker)
 *  - Campos dinâmicos de variáveis nível campanha
 *  - Descrição (multiline)
 *
 * Extras recebidos:
 *   estudo_remote_id → Int
 *   estudo_nome      → String
 *   campanha_id      → Int (-1 para criação)
 *   campanha_nome    → String (pré-preenchimento em edição)
 */
class NovaCampanhaActivityV2 : AppCompatActivity() {

    private lateinit var binding: ActivityNovaCampanhaV2Binding

    private var estudoRemoteId = -1
    private var campanhaId     = -1
    private var modoEdicao     = false

    /** Variáveis de nível campanha carregadas da API */
    private val variaveis = mutableListOf<VariavelResponse>()

    /** Views dos campos de variáveis, mapeadas por variavel.id */
    private val camposVariavel = mutableMapOf<Int, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovaCampanhaV2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        modoEdicao     = campanhaId != -1

        binding.tvTitulo.text = if (modoEdicao) "Editar Campanha" else "Nova Campanha"

        if (modoEdicao) {
            binding.etNome.setText(intent.getStringExtra("campanha_nome") ?: "")
            intent.getStringExtra("campanha_data_inicio")?.let { iso ->
                binding.etDataInicio.setText(isoParaBr(iso))
            }
        }

        // Campo e ícone calendário: abrem DatePickerDialog
        binding.ivCalendario.setOnClickListener { abrirDatePicker() }
        binding.etDataInicio.setOnClickListener { abrirDatePicker() }

        binding.btnConfirmar.setOnClickListener {
            if (modoEdicao) editarCampanha() else criarCampanha()
        }

        binding.ivBack.setOnClickListener { finish() }
        binding.ivMenuLateral.setOnClickListener { onBackPressed() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarVariaveis()
    }

    // ── Variáveis dinâmicas ───────────────────────────────────────────────

    private fun carregarVariaveis() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getVariaveis(
                    SessionManager.getAuthHeader(), estudoRemoteId,
                    nivelAplicacao = "campanha"
                )
                if (resp.isSuccessful) {
                    variaveis.clear()
                    variaveis.addAll(resp.body() ?: emptyList())
                    renderizarCamposVariaveis()
                    if (modoEdicao) preencherValoresVariaveis()
                } else {
                    Toast.makeText(
                        this@NovaCampanhaActivityV2,
                        "Erro ao carregar variáveis: ${resp.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    renderizarCamposVariaveis()
                }
            } catch (_: Exception) {
                renderizarCamposVariaveis()
            }
        }
    }

    private fun preencherValoresVariaveis() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getCampanha(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId
                )
                if (resp.isSuccessful) {
                    resp.body()?.valoresVariaveis?.forEach { vv ->
                        camposVariavel[vv.variavelId]?.setText(vv.valor)
                    }
                }
            } catch (_: Exception) { /* offline: valores não pré-preenchidos */ }
        }
    }

    private fun renderizarCamposVariaveis() {
        binding.layoutVariaveis.removeAllViews()
        camposVariavel.clear()

        binding.tvVariaveisTitle.visibility = View.VISIBLE

        if (variaveis.isEmpty()) {
            val vazio = TextView(this).apply {
                text = "Sem variáveis cadastradas para o nível campanha."
                textSize = 13f
                setTextColor(0xFF6B7A5E.toInt())
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(0, 4, 0, 0)
            }
            binding.layoutVariaveis.addView(vazio)
            return
        }

        variaveis.forEachIndexed { index, variavel ->
            // Label: "Variável N:"
            val label = TextView(this).apply {
                text = "${variavel.nome}:"
                textSize = 14f
                setTextColor(0xFF6B7A5E.toInt())
                setPadding(0, if (index > 0) 16 else 0, 0, 0)
                typeface = android.graphics.Typeface.MONOSPACE
            }
            binding.layoutVariaveis.addView(label)

            // Linha: campo de texto + unidade (métrica)
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 4 }
            }

            val campo = EditText(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, 52.dpToPx(), 1f
                )
                background = ContextCompat.getDrawable(this@NovaCampanhaActivityV2, R.drawable.bg_field_green)
                setPadding(24, 0, 24, 0)
                setTextColor(0xFF4A5240.toInt())
                hint = "Placeholder"
                inputType = when (variavel.tipoDado) {
                    "number" -> android.text.InputType.TYPE_CLASS_NUMBER or
                            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                    else -> android.text.InputType.TYPE_CLASS_TEXT
                }
            }
            camposVariavel[variavel.id] = campo
            linha.addView(campo)

            // Unidade/métrica ao lado
            if (!variavel.metrica.isNullOrEmpty()) {
                val unidade = TextView(this).apply {
                    text = variavel.metrica
                    textSize = 15f
                    setTextColor(0xFF4A5240.toInt())
                    setPadding(12, 0, 0, 0)
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                linha.addView(unidade)
            }

            binding.layoutVariaveis.addView(linha)
        }
    }

    private fun Int.dpToPx() =
        (this * resources.displayMetrics.density).toInt()


    // ── DatePicker ────────────────────────────────────────────────────────

    private fun abrirDatePicker() {
        val cal = java.util.Calendar.getInstance()
        // Pré-seleciona data já preenchida, se houver
        val atual = binding.etDataInicio.text.toString().trim()
        if (atual.isNotEmpty()) {
            val partes = atual.split("/")
            if (partes.size == 3) runCatching {
                cal.set(partes[2].toInt(), partes[1].toInt() - 1, partes[0].toInt())
            }
        }
        android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                binding.etDataInicio.setText(
                    "%02d/%02d/%04d".format(day, month + 1, year)
                )
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    /** Converte "dd/MM/yyyy" em "yyyy-MM-dd"; retorna null se inválido. */
    private fun brParaIso(br: String): String? {
        val partes = br.split("/")
        if (partes.size != 3) return null
        return runCatching {
            "%04d-%02d-%02d".format(partes[2].toInt(), partes[1].toInt(), partes[0].toInt())
        }.getOrNull()
    }

    /** Converte "yyyy-MM-dd" em "dd/MM/yyyy"; retorna a string original se não casar. */
    private fun isoParaBr(iso: String): String {
        val base = iso.take(10)
        val partes = base.split("-")
        if (partes.size != 3) return iso
        return runCatching {
            "%02d/%02d/%04d".format(partes[2].toInt(), partes[1].toInt(), partes[0].toInt())
        }.getOrDefault(iso)
    }

    // ── Montar valores das variáveis ──────────────────────────────────────

    private fun coletarValoresVariaveis(): List<ValorVariavelRequest> {
        return camposVariavel.entries
            .mapNotNull { (varId, campo) ->
                val valor = campo.text.toString().trim()
                if (valor.isNotEmpty()) ValorVariavelRequest(variavelId = varId, valor = valor)
                else null
            }
    }

    // ── API ───────────────────────────────────────────────────────────────

    private fun criarCampanha() {
        val nome   = binding.etNome.text.toString().trim()
        val inicio = binding.etDataInicio.text.toString().trim()
        val descricao = binding.etDescricao.text.toString().trim().ifEmpty { null }

        if (nome.isEmpty() || inicio.isEmpty()) {
            Toast.makeText(this, "Preencha nome e data de início", Toast.LENGTH_SHORT).show()
            return
        }

        val inicioIso = brParaIso(inicio) ?: run {
            Toast.makeText(this, "Data inválida", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.postCampanha(
                    SessionManager.getAuthHeader(), estudoRemoteId,
                    CampanhaRequest(
                        nome = nome,
                        dataInicio = inicioIso,
                        descricao = descricao,
                        valoresVariaveis = coletarValoresVariaveis()
                    )
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@NovaCampanhaActivityV2, "Campanha criada!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NovaCampanhaActivityV2, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@NovaCampanhaActivityV2, "Sem conexão", Toast.LENGTH_SHORT).show()
            } finally { setLoading(false) }
        }
    }

    private fun editarCampanha() {
        val nome   = binding.etNome.text.toString().trim()
        val inicio = binding.etDataInicio.text.toString().trim()
        val descricao = binding.etDescricao.text.toString().trim().ifEmpty { null }

        if (nome.isEmpty() || inicio.isEmpty()) {
            Toast.makeText(this, "Preencha nome e data de início", Toast.LENGTH_SHORT).show()
            return
        }

        val inicioIso = brParaIso(inicio) ?: run {
            Toast.makeText(this, "Data inválida", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.patchCampanha(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId,
                    CampanhaRequest(
                        nome = nome,
                        dataInicio = inicioIso,
                        descricao = descricao,
                        valoresVariaveis = coletarValoresVariaveis()
                    )
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@NovaCampanhaActivityV2, "Campanha atualizada!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NovaCampanhaActivityV2, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@NovaCampanhaActivityV2, "Sem conexão", Toast.LENGTH_SHORT).show()
            } finally { setLoading(false) }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }
}

// Alias para retrocompatibilidade — CampanhasActivity ainda referencia NovaCampanhaActivity
