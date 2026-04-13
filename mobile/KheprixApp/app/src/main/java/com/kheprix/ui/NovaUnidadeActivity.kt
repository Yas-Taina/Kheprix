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
import com.kheprix.models.UnidadeRequest
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
class NovaUnidadeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNovaUnidadeBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var estudoRemoteId = -1
    private var campanhaId     = -1
    private var unidadeId      = -1
    private var modoEdicao     = false

    private var latDecimal: Double? = null
    private var lonDecimal: Double? = null

    private val variaveis = mutableListOf<VariavelResponse>()
    private val camposVariavel = mutableMapOf<Int, EditText>()

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
        campanhaId     = intent.getIntExtra("campanha_id", -1)
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
        binding.ivMenuLateral.setOnClickListener { onBackPressed() }
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
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getVariaveis(
                    SessionManager.getAuthHeader(), estudoRemoteId, nivelAplicacao = "unidade"
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
            val campo = EditText(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 48.dpToPx(), 1f)
                background = ContextCompat.getDrawable(this@NovaUnidadeActivity, R.drawable.bg_field_green)
                setPadding(20, 0, 20, 0)
                setTextColor(0xFF4A5240.toInt()); hint = "Placeholder"
                inputType = if (v.tipoDado == "numerico")
                    android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                else android.text.InputType.TYPE_CLASS_TEXT
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

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()

    // ── Edição ────────────────────────────────────────────────────────────

    private fun preencherDadosEdicao() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getUnidade(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId
                )
                resp.body()?.let { u ->
                    latDecimal = u.latitude; lonDecimal = u.longitude
                    binding.etLatitude.setText(decimalToDms(u.latitude))
                    binding.etLongitude.setText(decimalToDms(u.longitude))
                    binding.etRaio.setText(u.raio?.toString() ?: "")
                    binding.etMetodoColeta.setText(u.metodoColeta ?: "")
                    binding.etEsforcoAmostral.setText(u.esforcoAmostral ?: "")
                }
            } catch (_: Exception) {}
        }
    }

    // ── API ───────────────────────────────────────────────────────────────

    private fun criarUnidade() {
        val req = coletarFormulario() ?: return
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
            esforcoAmostral = binding.etEsforcoAmostral.text.toString().trim().ifEmpty { null }
        )
    }

    private fun salvarOffline(req: UnidadeRequest) {
        try {
            val db = com.kheprix.db.DatabaseHelper(this).writableDatabase
            // Busca campanha_local_id pelo remote_id
            val cur = db.rawQuery(
                "SELECT local_id FROM campanhas WHERE remote_id = ?",
                arrayOf(campanhaId.toString())
            )
            val campanhaLocalId = cur.use { if (it.moveToFirst()) it.getLong(0) else -1L }
            if (campanhaLocalId != -1L) {
                val cv = android.content.ContentValues().apply {
                    put("sincronizado", 0)
                    put("campanha_local_id", campanhaLocalId)
                    put("nome", req.nome)
                    put("latitude", req.latitude)
                    put("longitude", req.longitude)
                    req.raio?.let { put("raio", it) }
                    req.metodoColeta?.let { put("metodo_coleta", it) }
                    req.esforcoAmostral?.let { put("esforco_amostral", it) }
                    put("created_at", java.time.Instant.now().toString())
                }
                db.insert("unidades_amostrais", null, cv)
                Toast.makeText(this, "Salvo offline. Sincronize quando tiver conexão.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Sem conexão e sem dados locais para esta campanha.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Sem conexão — não foi possível salvar offline.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }
}
