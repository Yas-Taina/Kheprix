package com.kheprix.ui

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityNovoRegistroBinding
import com.kheprix.databinding.ActivityRegistrosBinding
import com.kheprix.databinding.ActivityRegistrosDetalheBinding
import com.kheprix.models.*
import com.kheprix.util.ImagemLoader
import com.kheprix.util.PhotoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

// ════════════════════════════════════════════════════════════════════════════
// LISTA DE REGISTROS
// ════════════════════════════════════════════════════════════════════════════

/**
 * Lista de Registros de Ocorrência de um Evento de Amostragem.
 *
 * Recursos:
 *  - Filtro por espécie (ícone funil)
 *  - "Visualizar Detalhes" → card inline com dados do evento + botão editar
 *  - Clique num registro → NovoRegistroActivity (edição)
 *  - Botão "Adicionar Registro" → NovoRegistroActivity (criação)
 *
 * Extras: estudo_remote_id, campanha_id, unidade_id, evento_id, evento_nome
 */
class RegistrosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrosBinding
    private var estudoRemoteId = -1
    private var campanhaId     = -1
    private var unidadeId      = -1
    private var eventoId       = -1
    private var eventoNome     = ""

    private val registros     = mutableListOf<RegistroResponse>()
    private val registrosFiltrados = mutableListOf<RegistroResponse>()
    private val especies      = mutableListOf<EspecieResponse>()
    private lateinit var adapter: RegistroAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        unidadeId      = intent.getIntExtra("unidade_id", -1)
        eventoId       = intent.getIntExtra("evento_id", -1)
        eventoNome     = intent.getStringExtra("evento_nome") ?: ""

        binding.tvEventoNome.text = eventoNome

        adapter = RegistroAdapter(registrosFiltrados, lifecycleScope,
            onItemClick = { r -> abrirDetalhe(r) },
            buscarEspecie = { id -> especies.firstOrNull { it.id == id } }
        )
        binding.rvRegistros.layoutManager = LinearLayoutManager(this)
        binding.rvRegistros.adapter = adapter

        binding.btnAdicionarRegistro.setOnClickListener {
            startActivity(Intent(this, NovoRegistroActivity::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("campanha_id", campanhaId)
                putExtra("unidade_id", unidadeId)
                putExtra("evento_id", eventoId)
            })
        }

        binding.btnVisualizarDetalhes.setOnClickListener {
            val vis = binding.cardDetalhesEvento.visibility == View.VISIBLE
            binding.cardDetalhesEvento.visibility = if (vis) View.GONE else View.VISIBLE
        }

        binding.btnEditarEvento.setOnClickListener {
            startActivity(Intent(this, NovoEventoActivity::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("campanha_id", campanhaId)
                putExtra("unidade_id", unidadeId)
                putExtra("evento_id", eventoId)
            })
        }

        // Filtro por espécie
        binding.btnFiltrar.setOnClickListener { mostrarFiltroEspecie() }

        binding.ivMenuLateral.setOnClickListener { onBackPressed() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarEspecies()
        carregarDetalhes()
        carregarRegistros()
    }

    override fun onResume() { super.onResume(); carregarRegistros() }

    private fun carregarEspecies() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEspecies(SessionManager.getAuthHeader(), estudoRemoteId)
                if (resp.isSuccessful) { especies.clear(); especies.addAll(resp.body() ?: emptyList()) }
            } catch (_: Exception) {}
        }
    }

    private fun carregarDetalhes() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEvento(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, eventoId
                )
                resp.body()?.let { e ->
                    binding.tvDetalheInicio.text  = e.horarioInicio.replace("T", " ").take(16)
                    binding.tvDetalheFim.text     = e.horarioFim?.replace("T", " ")?.take(16) ?: "—"
                    binding.tvDetalheEsforco.text = e.esforcoReal ?: "—"
                }
            } catch (_: Exception) {}
        }
    }

    private fun carregarRegistros() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getRegistros(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, eventoId
                )
                if (resp.isSuccessful) {
                    registros.clear()
                    registros.addAll(resp.body() ?: emptyList())
                    aplicarFiltro(especieIdFiltro)
                }
            } catch (_: Exception) {
                Toast.makeText(this@RegistrosActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Filtro ────────────────────────────────────────────────────────────

    private var especieIdFiltro: Int? = null

    private fun mostrarFiltroEspecie() {
        val nomes = listOf("Todas as espécies") + especies.map { "${it.genero} ${it.especie}" }
        AlertDialog.Builder(this)
            .setTitle("Filtrar por espécie")
            .setItems(nomes.toTypedArray()) { _, idx ->
                especieIdFiltro = if (idx == 0) null else especies.getOrNull(idx - 1)?.id
                aplicarFiltro(especieIdFiltro)
            }.show()
    }

    private fun aplicarFiltro(especieId: Int?) {
        registrosFiltrados.clear()
        registrosFiltrados.addAll(
            if (especieId == null) registros else registros.filter { it.especieId == especieId }
        )
        adapter.notifyDataSetChanged()
    }

    private fun abrirDetalhe(r: RegistroResponse) {
        startActivity(Intent(this, RegistroDetalheActivity::class.java).apply {
            putExtra("estudo_remote_id", estudoRemoteId)
            putExtra("campanha_id", campanhaId)
            putExtra("unidade_id", unidadeId)
            putExtra("evento_id", eventoId)
            putExtra("registro_id", r.id)
        })
    }
}

class RegistroAdapter(
    private val items: List<RegistroResponse>,
    private val scope: CoroutineScope,
    private val onItemClick: (RegistroResponse) -> Unit,
    private val buscarEspecie: (Int) -> EspecieResponse?
) : RecyclerView.Adapter<RegistroAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvId: TextView     = view.findViewById(R.id.tvRegistroId)
        val tvData: TextView   = view.findViewById(R.id.tvRegistroData)
        val ivFoto: ImageView  = view.findViewById(R.id.ivRegistroFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_registro, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val especie = buscarEspecie(item.especieId)
        holder.tvId.text   = if (especie != null) "${especie.genero} ${especie.especie}" else "Registro #${item.id}"
        holder.tvData.text = item.data.split("-").let { p ->
            if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else item.data
        }
        ImagemLoader.load(
            scope = scope,
            target = holder.ivFoto,
            url = item.foto,
            placeholder = R.drawable.ic_placeholder_beetle
        )
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}

// ════════════════════════════════════════════════════════════════════════════
// NOVO REGISTRO
// ════════════════════════════════════════════════════════════════════════════

/**
 * Cadastro e edição de Registro de Ocorrência.
 *
 * Campos:
 *  - Foto (galeria ou câmera) → Base64
 *  - Data (DatePicker) + Hora (TimePicker)
 *  - Latitude / Longitude com ícone GPS
 *  - Espécie (Spinner carregado da API)
 *  - Quantidade de indivíduos
 *  - Variáveis de nível "registro" do estudo
 *
 * Detecta offline e exibe OfflineWarningDialog antes de salvar localmente.
 *
 * Extras: estudo_remote_id, campanha_id, unidade_id, evento_id, registro_id (-1 novo)
 */
class NovoRegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNovoRegistroBinding

    private var estudoRemoteId = -1
    private var campanhaId     = -1
    private var unidadeId      = -1
    private var eventoId       = -1
    private var registroId     = -1
    private var modoEdicao     = false

    private var fotoBase64: String? = null
    private var cameraUri: Uri?     = null
    private var dataStr = ""
    private var horaStr = ""
    private var latDecimal: Double? = null
    private var lonDecimal: Double? = null

    private val especies = mutableListOf<EspecieResponse>()
    private val variaveis = mutableListOf<VariavelResponse>()
    /** Para tipo "boolean" é Spinner; demais tipos é EditText. */
    private val camposVariavel = mutableMapOf<Int, View>()

    private val locationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) obterLocalizacao()
            else Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
        }

    private val galeria = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) processarImagem(r.data?.data)
    }

    private val camera = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) processarImagem(cameraUri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovoRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        unidadeId      = intent.getIntExtra("unidade_id", -1)
        eventoId       = intent.getIntExtra("evento_id", -1)
        registroId     = intent.getIntExtra("registro_id", -1)
        modoEdicao     = registroId != -1

        binding.tvTitulo.text = if (modoEdicao) "Editar Registro:" else "Novo Registro de Ocorrência:"

        binding.ivGaleria.setOnClickListener {
            galeria.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
        }
        binding.ivCamera.setOnClickListener { abrirCamera() }
        binding.ivCalendario.setOnClickListener { abrirDatePicker() }
        binding.etHora.setOnClickListener { abrirTimePicker() }
        binding.ivGpsLat.setOnClickListener { verificarPermissaoGps() }
        binding.ivGpsLon.setOnClickListener { verificarPermissaoGps() }

        binding.btnConfirmar.setOnClickListener {
            if (modoEdicao) editarRegistro() else criarRegistro()
        }
        binding.ivBack.setOnClickListener { finish() }
        binding.ivMenuLateral.setOnClickListener { onBackPressed() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarEspecies()
        carregarVariaveis()
        if (modoEdicao) preencherEdicao()
    }

    // ── Foto ──────────────────────────────────────────────────────────────

    private fun processarImagem(uri: Uri?) {
        uri ?: return
        fotoBase64 = PhotoUtils.uriToBase64(this, uri)
        binding.tvNomeFoto.text = uri.lastPathSegment ?: "foto.jpg"
    }

    private fun abrirCamera() {
        val arquivo = File.createTempFile("reg_", ".jpg", cacheDir)
        cameraUri = FileProvider.getUriForFile(this, "${packageName}.provider", arquivo)
        camera.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
        })
    }

    // ── GPS ───────────────────────────────────────────────────────────────

    private fun verificarPermissaoGps() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED)
            obterLocalizacao()
        else locationPermLauncher.launch(arrayOf(fine, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @Suppress("MissingPermission")
    private fun obterLocalizacao() {
        LocationServices.getFusedLocationProviderClient(this).lastLocation
            .addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    latDecimal = loc.latitude; lonDecimal = loc.longitude
                    binding.etLatitude.setText(decimalToDms(loc.latitude))
                    binding.etLongitude.setText(decimalToDms(loc.longitude))
                } else {
                    Toast.makeText(this, "GPS indisponível. Ative a localização.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun decimalToDms(dec: Double): String {
        val neg = dec < 0; val abs = Math.abs(dec)
        val deg = abs.toInt(); val minD = (abs - deg) * 60
        val min = minD.toInt(); val sec = ((minD - min) * 60).toInt()
        return "${if (neg) "-" else ""}${deg}°${min}'${sec}\""
    }

    private fun parseCoordenada(texto: String): Double? {
        texto.trim().toDoubleOrNull()?.let { return it }
        val r = Regex("""(-?\d+)°(\d+)'(\d+)""").find(texto.trim()) ?: return null
        val (d, m, s) = r.destructured
        val dv = d.toInt(); val abs = Math.abs(dv) + m.toInt() / 60.0 + s.toInt() / 3600.0
        return if (dv < 0) -abs else abs
    }

    // ── DatePicker / TimePicker ───────────────────────────────────────────

    private fun abrirDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            dataStr = "%04d-%02d-%02d".format(y, m + 1, d)
            binding.etData.setText("%02d/%02d/%04d".format(d, m + 1, y))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun abrirTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            horaStr = "%02d:%02d:00".format(h, m)
            binding.etHora.setText("%02d:%02d".format(h, m))
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    // ── Espécies (spinner) ────────────────────────────────────────────────

    private fun carregarEspecies() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEspecies(SessionManager.getAuthHeader(), estudoRemoteId)
                if (resp.isSuccessful) {
                    especies.clear(); especies.addAll(resp.body() ?: emptyList())
                    val nomes = listOf("Selecione...") + especies.map { "${it.genero} ${it.especie}" }
                    binding.spinnerEspecie.adapter = ArrayAdapter(this@NovoRegistroActivity,
                        android.R.layout.simple_spinner_item, nomes).also {
                        it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ── Variáveis nível registro ──────────────────────────────────────────

    private fun carregarVariaveis() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getVariaveis(
                    SessionManager.getAuthHeader(), estudoRemoteId, nivelAplicacao = "registro"
                )
                if (resp.isSuccessful) {
                    variaveis.clear(); variaveis.addAll(resp.body() ?: emptyList())
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
                text = "${v.nome}:"; textSize = 13f; setTextColor(0xFF6B7A5E.toInt())
                setPadding(0, if (i > 0) 12 else 0, 0, 0); typeface = android.graphics.Typeface.MONOSPACE
            }
            binding.layoutVariaveis.addView(label)
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).also { it.topMargin = 4 }
            }
            val campo: View = criarCampoVariavel(v.tipoDado)
            camposVariavel[v.id] = campo; linha.addView(campo)
            if (!v.metrica.isNullOrEmpty()) {
                linha.addView(TextView(this).apply {
                    text = v.metrica; textSize = 14f; setTextColor(0xFF4A5240.toInt())
                    setPadding(10, 0, 0, 0); typeface = android.graphics.Typeface.MONOSPACE
                })
            }
            binding.layoutVariaveis.addView(linha)
        }
    }

    /** Cria a view de entrada adequada ao tipoDado da variável. */
    private fun criarCampoVariavel(tipoDado: String): View {
        val lp = LinearLayout.LayoutParams(0, (48 * resources.displayMetrics.density).toInt(), 1f)
        val bg = ContextCompat.getDrawable(this, R.drawable.bg_field_green)
        return when (tipoDado) {
            "boolean" -> Spinner(this).apply {
                layoutParams = lp
                background = bg
                setPadding(20, 0, 20, 0)
                adapter = ArrayAdapter(
                    this@NovoRegistroActivity,
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

    // ── Preencher edição ──────────────────────────────────────────────────

    private fun preencherEdicao() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getRegistro(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, eventoId, registroId
                )
                resp.body()?.let { r ->
                    val dp = r.data.split("-")
                    dataStr = r.data
                    binding.etData.setText(if (dp.size == 3) "${dp[2]}/${dp[1]}/${dp[0]}" else r.data)
                    horaStr = r.hora
                    binding.etHora.setText(r.hora.take(5))
                    latDecimal = r.latitude; lonDecimal = r.longitude
                    binding.etLatitude.setText(decimalToDms(r.latitude))
                    binding.etLongitude.setText(decimalToDms(r.longitude))
                    binding.etQtde.setText(r.qtdeIndividuos?.toString() ?: "")
                    // fotoBase64 permanece null: se o usuário não escolher nova
                    // foto, o patch omite o campo e o backend preserva a atual.
                    if (r.foto != null) binding.tvNomeFoto.text = "foto_atual.jpg"
                    // Selecionar espécie no spinner
                    val idx = especies.indexOfFirst { it.id == r.especieId }
                    if (idx >= 0) binding.spinnerEspecie.setSelection(idx + 1)
                }
            } catch (_: Exception) {}
        }
    }

    // ── API ───────────────────────────────────────────────────────────────

    private fun criarRegistro() {
        val req = coletarFormulario() ?: return
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.postRegistro(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, eventoId, req
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@NovoRegistroActivity, "Registro criado!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NovoRegistroActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                // Offline: mostra dialog e salva localmente
                mostrarDialogOffline(req)
            } finally { setLoading(false) }
        }
    }

    private fun editarRegistro() {
        val form = coletarFormulario() ?: return
        setLoading(true)
        lifecycleScope.launch {
            try {
                val req = RegistroPatchRequest(
                    especieId = form.especieId, data = form.data, hora = form.hora,
                    latitude = form.latitude, longitude = form.longitude,
                    qtdeIndividuos = form.qtdeIndividuos, foto = form.foto
                )
                val resp = RetrofitClient.apiService.patchRegistro(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, eventoId, registroId, req
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@NovoRegistroActivity, "Registro atualizado!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NovoRegistroActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                mostrarDialogOffline(null)
            } finally { setLoading(false) }
        }
    }

    private fun coletarFormulario(): RegistroRequest? {
        val espPos = binding.spinnerEspecie.selectedItemPosition
        if (espPos == 0) { Toast.makeText(this, "Selecione a espécie", Toast.LENGTH_SHORT).show(); return null }
        val especie = especies.getOrNull(espPos - 1) ?: return null

        val dataFinal = dataStr.ifEmpty {
            val p = binding.etData.text.toString().split("/")
            if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}" else ""
        }
        val horaFinal = horaStr.ifEmpty {
            val h = binding.etHora.text.toString()
            if (h.matches(Regex("\\d{2}:\\d{2}"))) "$h:00" else ""
        }
        if (dataFinal.isEmpty() || horaFinal.isEmpty()) {
            Toast.makeText(this, "Preencha data e hora", Toast.LENGTH_SHORT).show(); return null
        }
        val lat = latDecimal ?: parseCoordenada(binding.etLatitude.text.toString())
        val lon = lonDecimal ?: parseCoordenada(binding.etLongitude.text.toString())
        if (lat == null || lon == null) {
            Toast.makeText(this, "Preencha coordenadas válidas", Toast.LENGTH_SHORT).show(); return null
        }
        return RegistroRequest(
            especieId = especie.id, data = dataFinal, hora = horaFinal,
            latitude = lat, longitude = lon,
            qtdeIndividuos = binding.etQtde.text.toString().trim().toIntOrNull(),
            foto = fotoBase64
        )
    }

    private fun mostrarDialogOffline(req: RegistroRequest?) {
        // Persiste no SQLite quando offline
        if (req != null) {
            try {
                val db = com.kheprix.db.DatabaseHelper(this).writableDatabase
                // Busca local_id do evento pelo remote_id
                val curEvento = db.rawQuery(
                    "SELECT local_id FROM eventos_amostragem WHERE remote_id = ?",
                    arrayOf(eventoId.toString())
                )
                val eventoLocalId = curEvento.use { if (it.moveToFirst()) it.getLong(0) else -1L }

                // Busca local_id da espécie pelo remote_id
                val curEspecie = db.rawQuery(
                    "SELECT local_id FROM especies WHERE remote_id = ?",
                    arrayOf(req.especieId.toString())
                )
                val especieLocalId = curEspecie.use { if (it.moveToFirst()) it.getLong(0) else -1L }

                if (eventoLocalId != -1L && especieLocalId != -1L) {
                    val cv = android.content.ContentValues().apply {
                        put("sincronizado", 0)
                        put("evento_local_id", eventoLocalId)
                        put("especie_local_id", especieLocalId)
                        put("especie_remote_id", req.especieId)
                        put("data", req.data)
                        put("hora", req.hora)
                        put("latitude", req.latitude)
                        put("longitude", req.longitude)
                        put("qtde_individuos", req.qtdeIndividuos)
                        put("foto", req.foto)
                        req.ausenciaEspecie?.let { put("ausencia_especie", if (it) 1 else 0) }
                        put("created_at", java.time.Instant.now().toString())
                    }
                    db.insert("registros_ocorrencia", null, cv)
                }
            } catch (_: Exception) { /* silencioso */ }
        }
        startActivity(Intent(this, OfflineWarningActivity::class.java))
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }
}

// ════════════════════════════════════════════════════════════════════════════
// DETALHE DE REGISTRO
// ════════════════════════════════════════════════════════════════════════════

/**
 * Exibe os detalhes de um Registro de Ocorrência com opção de editar e deletar.
 *
 * Campos: foto, nome científico/popular (da espécie), qtde indivíduos,
 * latitude/longitude, data, hora e variáveis de nível "registro" do estudo.
 *
 * Extras: estudo_remote_id, campanha_id, unidade_id, evento_id, registro_id
 */
class RegistroDetalheActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrosDetalheBinding
    private var estudoRemoteId = -1
    private var campanhaId     = -1
    private var unidadeId      = -1
    private var eventoId       = -1
    private var registroId     = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrosDetalheBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        campanhaId     = intent.getIntExtra("campanha_id", -1)
        unidadeId      = intent.getIntExtra("unidade_id", -1)
        eventoId       = intent.getIntExtra("evento_id", -1)
        registroId     = intent.getIntExtra("registro_id", -1)

        binding.ivEditar.setOnClickListener {
            startActivity(Intent(this, NovoRegistroActivity::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("campanha_id", campanhaId)
                putExtra("unidade_id", unidadeId)
                putExtra("evento_id", eventoId)
                putExtra("registro_id", registroId)
            })
        }
        binding.ivDeletar.setOnClickListener { confirmarDelete() }
        binding.ivMenuLateral.setOnClickListener { onBackPressed() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarVariaveis()
    }

    override fun onResume() {
        super.onResume()
        carregarRegistro()
    }

    private fun carregarRegistro() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getRegistro(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, eventoId, registroId
                )
                resp.body()?.let { r ->
                    binding.tvQtde.text      = r.qtdeIndividuos?.toString() ?: "—"
                    binding.tvLatitude.text  = r.latitude.toString()
                    binding.tvLongitude.text = r.longitude.toString()
                    binding.tvData.text      = r.data.split("-").let { p ->
                        if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else r.data
                    }
                    binding.tvHora.text = r.hora.take(5)

                    ImagemLoader.load(
                        scope = lifecycleScope,
                        target = binding.ivFoto,
                        url = r.foto,
                        placeholder = R.drawable.ic_placeholder_beetle
                    )

                    carregarEspecie(r.especieId)
                }
            } catch (_: Exception) {
                Toast.makeText(this@RegistroDetalheActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun carregarEspecie(especieId: Int) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEspecie(
                    SessionManager.getAuthHeader(), estudoRemoteId, especieId
                )
                resp.body()?.let { e ->
                    binding.tvTitulo.text         = "${e.genero} ${e.especie}"
                    binding.tvNomeCientifico.text = "${e.genero} ${e.especie}"
                    binding.tvNomePopular.text    = e.nomePopular ?: "—"
                }
            } catch (_: Exception) { }
        }
    }

    private fun carregarVariaveis() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getVariaveis(
                    SessionManager.getAuthHeader(), estudoRemoteId, nivelAplicacao = "registro"
                )
                if (resp.isSuccessful) renderizarVariaveis(resp.body() ?: emptyList())
            } catch (_: Exception) { }
        }
    }

    private fun renderizarVariaveis(vars: List<VariavelResponse>) {
        binding.layoutVariaveis.removeAllViews()
        if (vars.isEmpty()) {
            binding.tvVariaveisTitle.visibility = View.GONE
            return
        }
        binding.tvVariaveisTitle.visibility = View.VISIBLE
        vars.forEachIndexed { i, v ->
            val nome = TextView(this).apply {
                text = "${v.nome}${if (v.metrica.isNullOrBlank()) "" else " (${v.metrica})"}:"
                textSize = 14f
                setTextColor(0xFF6B7A5E.toInt())
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(0, if (i > 0) 10 else 0, 0, 0)
            }
            val valor = TextView(this).apply {
                text = "—"
                textSize = 15f
                setTextColor(0xFF4A5240.toInt())
                typeface = android.graphics.Typeface.MONOSPACE
            }
            binding.layoutVariaveis.addView(nome)
            binding.layoutVariaveis.addView(valor)
        }
    }

    private fun confirmarDelete() {
        AlertDialog.Builder(this)
            .setTitle("Deletar registro")
            .setMessage("Tem certeza?")
            .setPositiveButton("Deletar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        RetrofitClient.apiService.deleteRegistro(
                            SessionManager.getAuthHeader(),
                            estudoRemoteId, campanhaId, unidadeId, eventoId, registroId
                        )
                        finish()
                    } catch (_: Exception) {
                        Toast.makeText(this@RegistroDetalheActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }
}

// ════════════════════════════════════════════════════════════════════════════
// AVISO OFFLINE
// ════════════════════════════════════════════════════════════════════════════

/**
 * Tela/Dialog de aviso de modo offline.
 * Exibe um card centralizado informando que os registros serão salvos localmente
 * e que a sincronização poderá ser feita quando houver conexão.
 * Botão "Continuar" fecha a tela.
 */
class OfflineWarningActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_warning)

        // Fundo transparente para parecer um dialog sobreposto
        window.setBackgroundDrawableResource(android.R.color.transparent)

        findViewById<Button>(R.id.btnContinuar).setOnClickListener {
            finish()
        }
    }
}
