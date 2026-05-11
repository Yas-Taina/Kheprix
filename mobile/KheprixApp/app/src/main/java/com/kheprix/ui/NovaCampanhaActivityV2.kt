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
import com.kheprix.db.CampanhaDao
import com.kheprix.db.OfflineRepository
import com.kheprix.models.CampanhaRequest
import com.kheprix.models.ValorVariavelRequest
import com.kheprix.models.VariavelResponse
import kotlinx.coroutines.launch


class NovaCampanhaActivityV2 : BaseDrawerActivity() {

    private lateinit var binding: ActivityNovaCampanhaV2Binding

    private var estudoRemoteId = -1
    private var estudoLocalId  = -1L
    private var campanhaId     = -1
    private var modoEdicao     = false

    private val variaveis = mutableListOf<VariavelResponse>()
    private val camposVariavel = mutableMapOf<Int, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovaCampanhaV2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId  = intent.getLongExtra("estudo_local_id", -1L)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        modoEdicao     = campanhaId != -1

        binding.tvTitulo.text = if (modoEdicao) "Editar Campanha" else "Nova Campanha"

        if (modoEdicao) {
            binding.etNome.setText(intent.getStringExtra("campanha_nome") ?: "")
        }

        binding.ivCalendario.setOnClickListener { abrirDatePicker() }
        binding.etDataInicio.setOnClickListener { abrirDatePicker() }

        binding.btnConfirmar.setOnClickListener {
            if (modoEdicao) editarCampanha() else criarCampanha()
        }

        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarVariaveis()
    }

    private fun carregarVariaveis() {
        if (estudoRemoteId <= 0) {
            carregarVariaveisOffline()
            return
        }
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
                carregarVariaveisOffline()
            }
        }
    }

    private fun carregarVariaveisOffline() {
        if (estudoLocalId <= 0) { renderizarCamposVariaveis(); return }
        val db = com.kheprix.db.DatabaseHelper(this).readableDatabase
        variaveis.clear()
        db.rawQuery(
            "SELECT remote_id, nome, nivel_aplicacao, tipo_dado, metrica, created_at, updated_at, local_id FROM variaveis WHERE estudo_local_id = ? AND nivel_aplicacao = 'campanha'",
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
        renderizarCamposVariaveis()
        if (modoEdicao) preencherValoresVariaveisOffline()
    }

    private fun preencherValoresVariaveis() {
        lifecycleScope.launch {
            if (estudoRemoteId > 0 && campanhaId > 0) {
                try {
                    val resp = RetrofitClient.apiService.getCampanha(
                        SessionManager.getAuthHeader(), estudoRemoteId, campanhaId
                    )
                    resp.body()?.let { c ->
                        binding.etNome.setText(c.nome)
                        binding.etDataInicio.setText(isoParaBr(c.dataInicio))
                        binding.etDescricao.setText(c.descricao ?: "")
                        c.valoresVariaveis?.forEach { vv -> aplicarValorNoCampo(vv.variavelId, vv.valor) }
                        return@launch
                    }
                } catch (_: Exception) { }
            }
            preencherValoresVariaveisOffline()
        }
    }

    private fun preencherValoresVariaveisOffline() {
        val campanhaLocalId: Long = when {
            campanhaId < 0 -> (-campanhaId).toLong()
            campanhaId > 0 -> {
                val repo = OfflineRepository(this)
                val eL = if (estudoLocalId > 0) estudoLocalId
                         else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
                         else null
                eL?.let { CampanhaDao(this).buscarPorRemoteIdEscopo(campanhaId, it)?.localId } ?: return
            }
            else -> return
        }
        val c = CampanhaDao(this).buscarPorLocalId(campanhaLocalId) ?: return
        binding.etNome.setText(c.nome)
        binding.etDataInicio.setText(isoParaBr(c.dataInicio))
        binding.etDescricao.setText(c.descricao ?: "")
        val db = com.kheprix.db.DatabaseHelper(this).readableDatabase
        db.rawQuery(
            "SELECT variavel_remote_id, variavel_local_id, valor FROM valores_variaveis WHERE campanha_local_id = ?",
            arrayOf(campanhaLocalId.toString())
        ).use { cur ->
            while (cur.moveToNext()) {
                val varRemoteId = if (cur.isNull(0)) null else cur.getInt(0)
                val varLocalId  = cur.getLong(1)
                val valor       = cur.getString(2) ?: continue
                val varId = if (varRemoteId != null && varRemoteId > 0) varRemoteId
                            else -varLocalId.toInt()
                aplicarValorNoCampo(varId, valor)
            }
        }
    }

    private fun aplicarValorNoCampo(variavelId: Int, valor: String) {
        when (val view = camposVariavel[variavelId]) {
            is Spinner -> {
                val idx = when (valor.trim().lowercase()) {
                    "true", "verdadeiro" -> 1
                    "false", "falso"     -> 2
                    else                 -> 0
                }
                view.setSelection(idx)
            }
            is EditText -> view.setText(valor)
            else -> {}
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
            val label = TextView(this).apply {
                text = "${variavel.nome}:"
                textSize = 14f
                setTextColor(0xFF6B7A5E.toInt())
                setPadding(0, if (index > 0) 16 else 0, 0, 0)
                typeface = android.graphics.Typeface.MONOSPACE
            }
            binding.layoutVariaveis.addView(label)

            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 4 }
            }

            val campo: View = criarCampoVariavel(variavel.tipoDado)
            camposVariavel[variavel.id] = campo
            linha.addView(campo)

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

    private fun criarCampoVariavel(tipoDado: String): View {
        val lp = LinearLayout.LayoutParams(0, 52.dpToPx(), 1f)
        val bg = ContextCompat.getDrawable(this, R.drawable.bg_field_green)
        return when (tipoDado) {
            "boolean" -> Spinner(this).apply {
                layoutParams = lp
                background = bg
                setPadding(24, 0, 24, 0)
                adapter = ArrayAdapter(
                    this@NovaCampanhaActivityV2,
                    android.R.layout.simple_spinner_item,
                    listOf("—", "Verdadeiro", "Falso")
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
            else -> EditText(this).apply {
                layoutParams = lp
                background = bg
                setPadding(24, 0, 24, 0)
                setTextColor(0xFF4A5240.toInt())
                hint = "Placeholder"
                inputType = when (tipoDado) {
                    "number" -> android.text.InputType.TYPE_CLASS_NUMBER or
                            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                            android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                    else -> android.text.InputType.TYPE_CLASS_TEXT
                }
            }
        }
    }

    private fun abrirDatePicker() {
        val cal = java.util.Calendar.getInstance()
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

    private fun brParaIso(br: String): String? {
        val partes = br.split("/")
        if (partes.size != 3) return null
        return runCatching {
            "%04d-%02d-%02d".format(partes[2].toInt(), partes[1].toInt(), partes[0].toInt())
        }.getOrNull()
    }

    private fun isoParaBr(iso: String): String {
        val base = iso.take(10)
        val partes = base.split("-")
        if (partes.size != 3) return iso
        return runCatching {
            "%02d/%02d/%04d".format(partes[2].toInt(), partes[1].toInt(), partes[0].toInt())
        }.getOrDefault(iso)
    }

    private fun coletarValoresVariaveis(): List<ValorVariavelRequest> {
        return camposVariavel.entries.mapNotNull { (varId, view) ->
            val valor = when (view) {
                is Spinner -> when (view.selectedItem?.toString()) {
                    "Verdadeiro" -> "true"
                    "Falso"      -> "false"
                    else         -> null
                }
                is EditText -> view.text.toString().trim().ifEmpty { null }
                else -> null
            } ?: return@mapNotNull null
            ValorVariavelRequest(variavelId = varId, valor = valor)
        }
    }

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

        val req = CampanhaRequest(
            nome = nome,
            dataInicio = inicioIso,
            descricao = descricao,
            valoresVariaveis = coletarValoresVariaveis()
        )

        if (estudoRemoteId <= 0) {
            salvarCampanhaOffline(req)
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.postCampanha(
                    SessionManager.getAuthHeader(), estudoRemoteId, req
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@NovaCampanhaActivityV2, "Campanha criada!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NovaCampanhaActivityV2, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                salvarCampanhaOffline(req)
            } finally { setLoading(false) }
        }
    }


    private fun salvarCampanhaOffline(req: CampanhaRequest) {
        val repo = OfflineRepository(this)
        val resolvedLocalId = when {
            estudoLocalId > 0 -> estudoLocalId
            estudoRemoteId > 0 -> repo.estudoLocalIdFromRemote(estudoRemoteId)
            else -> null
        }
        if (resolvedLocalId == null) {
            Toast.makeText(
                this,
                "Estudo não está salvo offline. Salve o estudo primeiro.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        try {
            repo.criarCampanhaOffline(resolvedLocalId, req)
            Toast.makeText(this, "Campanha salva offline.", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Erro ao salvar offline: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
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

        val req = CampanhaRequest(
            nome = nome,
            dataInicio = inicioIso,
            descricao = descricao,
            valoresVariaveis = coletarValoresVariaveis()
        )

        if (campanhaId < 0 || estudoRemoteId <= 0) {
            editarCampanhaOffline(req)
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.patchCampanha(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, req
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@NovaCampanhaActivityV2, "Campanha atualizada!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NovaCampanhaActivityV2, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                editarCampanhaOffline(req)
            } finally { setLoading(false) }
        }
    }

    private fun editarCampanhaOffline(req: CampanhaRequest) {
        val repo = OfflineRepository(this)
        val campanhaLocalId: Long = when {
            campanhaId < 0 -> (-campanhaId).toLong()
            campanhaId > 0 -> {
                val eL = if (estudoLocalId > 0) estudoLocalId
                         else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
                         else null
                eL?.let { CampanhaDao(this).buscarPorRemoteIdEscopo(campanhaId, it)?.localId } ?: run {
                    Toast.makeText(this, "Campanha não encontrada offline", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            else -> return
        }
        try {
            repo.editarCampanhaOffline(campanhaLocalId, req)
            Toast.makeText(this, "Campanha salva offline.", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar offline: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }
}
