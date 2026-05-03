package com.kheprix.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityNovaUnidadeBinding
import com.kheprix.db.OfflineRepository
import com.kheprix.models.UnidadeRequest
import com.kheprix.models.UnidadeResponse
import com.kheprix.models.ValorVariavelRequest
import com.kheprix.models.VariavelResponse
import kotlinx.coroutines.launch

/**
 * Cadastro e edição de Unidade Amostral.
 *
 * Campos:
 *  - Latitude / Longitude (com ícone GPS que preenche automaticamente)
 *  - Raio (em metros, opcional)
 *  - Método de Coleta (opcional)
 *  - Esforço Amostral (opcional)
 *  - Variáveis de nível "unidade" do estudo (dinâmicas)
 *
 * Coordenadas são exibidas em formato DMS mas enviadas como decimal (Double).
 * O ícone de localização usa FusedLocationProviderClient para GPS.
 *
 * Extras recebidos:
 *   estudo_remote_id → Int
 *   campanha_id      → Int
 *   unidade_id       → Int (-1 para criação)
 *   (dados para pré-preenchimento em modo edição)
 */
class NovaUnidadeActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityNovaUnidadeBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var estudoRemoteId = -1
    private var estudoLocalId  = -1L
    private var campanhaId     = -1
    private var campanhaLocalId = -1L
    private var unidadeId      = -1
    private var modoEdicao     = false

    private var latDecimal: Double? = null
    private var lonDecimal: Double? = null

    private val variaveis = mutableListOf<VariavelResponse>()
    /** Para tipo "boolean" é Spinner; demais tipos é EditText. */
    private val camposVariavel = mutableMapOf<Int, View>()

    /** Unidade carregada em modo edição — usada para preencher campos e
     *  recuperar os ids dos valores_variaveis existentes ao salvar. */
    private var unidadeCarregada: UnidadeResponse? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                obterLocalizacao()
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovaUnidadeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId  = intent.getLongExtra("estudo_local_id", -1L)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        campanhaLocalId = intent.getLongExtra("campanha_local_id", -1L)
        if (campanhaId < 0 && campanhaLocalId <= 0) campanhaLocalId = (-campanhaId).toLong()
        unidadeId      = intent.getIntExtra("unidade_id", -1)
        modoEdicao     = unidadeId != -1

        binding.tvTitulo.text = if (modoEdicao) "Editar Unidade Amostral" else "Nova Unidade Amostral"

        // Ícones GPS: preenchem lat/lon com localização do dispositivo
        binding.ivGpsLat.setOnClickListener { verificarPermissaoGps() }
        binding.ivGpsLon.setOnClickListener { verificarPermissaoGps() }

        binding.btnConfirmar.setOnClickListener {
            if (modoEdicao) editarUnidade() else criarUnidade()
        }

        binding.ivBack.setOnClickListener { finish() }
        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        if (modoEdicao) preencherDadosEdicao()
        carregarVariaveis()
    }

    // ── GPS ───────────────────────────────────────────────────────────────

    private fun verificarPermissaoGps() {
        val fine   = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED) {
            obterLocalizacao()
        } else {
            locationPermissionLauncher.launch(arrayOf(fine, coarse))
        }
    }

    @Suppress("MissingPermission")
    private fun obterLocalizacao() {
        fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                latDecimal = loc.latitude
                lonDecimal = loc.longitude
                binding.etLatitude.setText(decimalToDms(loc.latitude))
                binding.etLongitude.setText(decimalToDms(loc.longitude))
            } else {
                Toast.makeText(this, "Localização não disponível. Ative o GPS.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao obter localização", Toast.LENGTH_SHORT).show()
        }
    }

    /** Converte decimal → DMS string para exibição */
    private fun decimalToDms(dec: Double): String {
        val neg = dec < 0; val abs = Math.abs(dec)
        val deg = abs.toInt(); val minD = (abs - deg) * 60
        val min = minD.toInt(); val sec = ((minD - min) * 60).toInt()
        return "${if (neg) "-" else ""}${deg}°${min}'${sec}\""
    }

    /** Tenta fazer parse de DMS ou decimal puro digitado pelo usuário */
    private fun parseCoordenada(texto: String): Double? {
        val trimmed = texto.trim()
        // Tenta decimal direto
        trimmed.toDoubleOrNull()?.let { return it }
        // Tenta formato DMS: -25°25'48"
        val regex = Regex("""(-?\d+)°(\d+)'(\d+)""")
        val match = regex.find(trimmed) ?: return null
        val (d, m, s) = match.destructured
        val v = d.toInt().let { dv ->
            val abs = Math.abs(dv) + m.toInt() / 60.0 + s.toInt() / 3600.0
            if (dv < 0) -abs else abs
        }
        return v
    }

    // ── Variáveis dinâmicas (nível unidade) ───────────────────────────────

    private fun carregarVariaveis() {
        if (estudoRemoteId <= 0) { carregarVariaveisOffline(); return }
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getVariaveis(
                    SessionManager.getAuthHeader(), estudoRemoteId, nivelAplicacao = "unidade"
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
            "SELECT remote_id, nome, nivel_aplicacao, tipo_dado, metrica, created_at, updated_at, local_id FROM variaveis WHERE estudo_local_id = ? AND nivel_aplicacao = 'unidade'",
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
        binding.layoutVariaveis.removeAllViews()
        camposVariavel.clear()
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
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
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

        // Em modo edição, se a unidade já chegou, preencher imediatamente.
        aplicarValoresVariaveis()
    }

    /** Aplica os valores das variáveis da unidade carregada nos campos
     *  renderizados. Idempotente — chamado tanto após renderizar quanto
     *  após carregar a unidade, pois os fluxos são assíncronos. */
    private fun aplicarValoresVariaveis() {
        val u = unidadeCarregada ?: return
        if (camposVariavel.isEmpty()) return
        u.valoresVariaveis?.forEach { vv ->
            when (val view = camposVariavel[vv.variavelId]) {
                is Spinner -> {
                    val idx = when (vv.valor.trim().lowercase()) {
                        "true", "verdadeiro" -> 1
                        "false", "falso"     -> 2
                        else                 -> 0
                    }
                    view.setSelection(idx)
                }
                is EditText -> view.setText(vv.valor)
                else -> { /* tipo desconhecido: ignora */ }
            }
        }
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()

    /** Cria a view de entrada adequada ao tipoDado da variável. */
    private fun criarCampoVariavel(tipoDado: String): View {
        val lp = LinearLayout.LayoutParams(0, 48.dpToPx(), 1f)
        val bg = ContextCompat.getDrawable(this, R.drawable.bg_field_green)
        return when (tipoDado) {
            "boolean" -> Spinner(this).apply {
                layoutParams = lp
                background = bg
                setPadding(20, 0, 20, 0)
                adapter = ArrayAdapter(
                    this@NovaUnidadeActivity,
                    android.R.layout.simple_spinner_item,
                    listOf("—", "Verdadeiro", "Falso")
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            }
            else -> EditText(this).apply {
                layoutParams = lp
                background = bg
                setPadding(20, 0, 20, 0)
                setTextColor(0xFF4A5240.toInt()); hint = "Placeholder"
                inputType = when (tipoDado) {
                    "number" -> android.text.InputType.TYPE_CLASS_NUMBER or
                            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                            android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                    else -> android.text.InputType.TYPE_CLASS_TEXT
                }
            }
        }
    }

    // ── Edição ────────────────────────────────────────────────────────────

    private fun preencherDadosEdicao() {
        if (estudoRemoteId <= 0 || campanhaId <= 0 || unidadeId <= 0) return
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getUnidade(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId
                )
                resp.body()?.let { u ->
                    unidadeCarregada = u
                    binding.etNomeUnidade.setText(u.nome)
                    latDecimal = u.latitude; lonDecimal = u.longitude
                    binding.etLatitude.setText(decimalToDms(u.latitude))
                    binding.etLongitude.setText(decimalToDms(u.longitude))
                    binding.etRaio.setText(u.raio?.toString() ?: "")
                    binding.etMetodoColeta.setText(u.metodoColeta ?: "")
                    binding.etEsforcoAmostral.setText(u.esforcoAmostral ?: "")
                    aplicarValoresVariaveis()
                }
            } catch (_: Exception) {}
        }
    }

    // ── API ───────────────────────────────────────────────────────────────

    private fun criarUnidade() {
        val req = coletarFormulario() ?: return
        // Pais offline-only: salva direto no SQLite (sem chamar API).
        if (estudoRemoteId <= 0 || campanhaId <= 0) {
            salvarOffline(req)
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.postUnidade(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, req
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@NovaUnidadeActivity, "Unidade criada!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NovaUnidadeActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                salvarOffline(req)
            } finally { setLoading(false) }
        }
    }

    private fun editarUnidade() {
        val req = coletarFormulario() ?: return
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.patchUnidade(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, req
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@NovaUnidadeActivity, "Unidade atualizada!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NovaUnidadeActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@NovaUnidadeActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            } finally { setLoading(false) }
        }
    }

    private fun coletarFormulario(): UnidadeRequest? {
        val latTexto = binding.etLatitude.text.toString().trim()
        val lonTexto = binding.etLongitude.text.toString().trim()
        val lat = parseCoordenada(latTexto)
        val lon = parseCoordenada(lonTexto)
        val nomeUnidade = binding.etNomeUnidade.text.toString().trim()
        if (lat == null || lon == null) {
            Toast.makeText(this, "Preencha coordenadas válidas", Toast.LENGTH_SHORT).show()
            return null
        }
        // Usa decimal atualizado pelo GPS se disponível, senão parse do texto
        val finalLat = latDecimal ?: lat
        val finalLon = lonDecimal ?: lon
        return UnidadeRequest(
            nome = binding.etNomeUnidade.text.toString().trim().let {
                if (it.isEmpty()) "Unidade ${System.currentTimeMillis()}" else it
            },
            latitude = finalLat, longitude = finalLon,
            raio = binding.etRaio.text.toString().trim().toDoubleOrNull(),
            metodoColeta = binding.etMetodoColeta.text.toString().trim().ifEmpty { null },
            esforcoAmostral = binding.etEsforcoAmostral.text.toString().trim().ifEmpty { null },
            valoresVariaveis = coletarValoresVariaveis()
        )
    }

    /** Monta os valores_variaveis a enviar.
     *
     * Em criação: cada item carrega `variavel_id` + `valor`.
     * Em edição: o backend (`sincronizar_valores_variaveis`) só atualiza
     * registros existentes, identificados por `id`. Para variáveis sem
     * valor prévio salvo na unidade, ainda enviamos `variavel_id` (que o
     * backend ignora no PATCH); itens com valor preenchido e id existente
     * são atualizados; itens omitidos são removidos via soft delete.
     */
    private fun coletarValoresVariaveis(): List<ValorVariavelRequest>? {
        if (camposVariavel.isEmpty()) return null
        val idPorVariavel: Map<Int, Int> =
            unidadeCarregada?.valoresVariaveis
                ?.mapNotNull { vv -> vv.id?.let { vv.variavelId to it } }
                ?.toMap()
                ?: emptyMap()

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
                val existenteId = idPorVariavel[varId]
                if (existenteId != null)
                    ValorVariavelRequest(id = existenteId, variavelId = varId, valor = valor)
                else
                    ValorVariavelRequest(variavelId = varId, valor = valor)
            } else {
                ValorVariavelRequest(variavelId = varId, valor = valor)
            }
        }
        return itens.ifEmpty { null }
    }

    /**
     * Persiste a unidade no SQLite. Requer que o estudo e a campanha pai
     * estejam salvos offline — se não estiverem, aborta e NÃO grava órfão.
     */
    private fun salvarOffline(req: UnidadeRequest) {
        val repo = OfflineRepository(this)
        val campanhaResolved = when {
            campanhaLocalId > 0 -> campanhaLocalId
            estudoRemoteId > 0 && campanhaId > 0 ->
                repo.estudoLocalIdFromRemote(estudoRemoteId)?.let {
                    repo.campanhaLocalIdFromRemote(it, campanhaId)
                }
            else -> null
        }
        if (campanhaResolved == null) {
            Toast.makeText(this, "Campanha não está salva offline.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            repo.criarUnidadeOffline(campanhaResolved, req)
            Toast.makeText(this, "Unidade salva offline.", Toast.LENGTH_SHORT).show()
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
