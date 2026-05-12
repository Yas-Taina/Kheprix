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
import com.kheprix.db.EstudoDao
import com.kheprix.db.EventoDao
import com.kheprix.db.OfflineRepository
import com.kheprix.db.RegistroDao
import com.kheprix.models.*
import com.kheprix.util.ImagemLoader
import com.kheprix.util.PhotoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar


class RegistrosActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityRegistrosBinding
    private var estudoRemoteId = -1
    private var estudoLocalId = -1L
    private var campanhaId = -1
    private var campanhaLocalId = -1L
    private var unidadeId = -1
    private var unidadeLocalId = -1L
    private var eventoId = -1
    private var eventoLocalId = -1L
    private var eventoNome = ""

    private val registros = mutableListOf<RegistroResponse>()
    private val registrosFiltrados = mutableListOf<RegistroResponse>()
    private val especies = mutableListOf<EspecieResponse>()
    private lateinit var adapter: RegistroAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId = intent.getLongExtra("estudo_local_id", -1L)
        campanhaId = intent.getIntExtra("campanha_id", -1)
        campanhaLocalId = intent.getLongExtra("campanha_local_id", -1L)
        if (campanhaId < 0 && campanhaLocalId <= 0) campanhaLocalId = (-campanhaId).toLong()
        unidadeId = intent.getIntExtra("unidade_id", -1)
        unidadeLocalId = intent.getLongExtra("unidade_local_id", -1L)
        if (unidadeId < 0 && unidadeLocalId <= 0) unidadeLocalId = (-unidadeId).toLong()
        eventoId = intent.getIntExtra("evento_id", -1)
        eventoLocalId = intent.getLongExtra("evento_local_id", -1L)
        if (eventoId < 0 && eventoLocalId <= 0) eventoLocalId = (-eventoId).toLong()
        eventoNome = intent.getStringExtra("evento_nome") ?: ""

        binding.tvEventoNome.text = formatarDataHora(eventoNome)

        adapter = RegistroAdapter(
            registrosFiltrados, lifecycleScope,
            onItemClick = { r -> abrirDetalhe(r) },
            buscarEspecie = { id -> especies.firstOrNull { it.id == id } }
        )
        binding.rvRegistros.layoutManager = LinearLayoutManager(this)
        binding.rvRegistros.adapter = adapter

        binding.btnAdicionarRegistro.setOnClickListener {
            startActivity(Intent(this, NovoRegistroActivity::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("estudo_local_id", estudoLocalId)
                putExtra("campanha_id", campanhaId)
                putExtra("campanha_local_id", campanhaLocalId)
                putExtra("unidade_id", unidadeId)
                putExtra("unidade_local_id", unidadeLocalId)
                putExtra("evento_id", eventoId)
                putExtra("evento_local_id", eventoLocalId)
            })
        }

        binding.btnVisualizarDetalhes.setOnClickListener {
            val vis = binding.cardDetalhesEvento.visibility == View.VISIBLE
            binding.cardDetalhesEvento.visibility = if (vis) View.GONE else View.VISIBLE
        }

        binding.btnEditarEvento.setOnClickListener {
            startActivity(Intent(this, NovoEventoActivity::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("estudo_local_id", estudoLocalId)
                putExtra("campanha_id", campanhaId)
                putExtra("campanha_local_id", campanhaLocalId)
                putExtra("unidade_id", unidadeId)
                putExtra("unidade_local_id", unidadeLocalId)
                putExtra("evento_id", eventoId)
                putExtra("evento_local_id", eventoLocalId)
            })
        }

        binding.btnFiltrar.setOnClickListener { mostrarFiltroEspecie() }

        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarEspecies()
        carregarDetalhes()
        carregarRegistros()
    }

    override fun onResume() {
        super.onResume()
        carregarEspecies()
        carregarDetalhes()
        carregarRegistros()
    }

    private fun carregarEspecies() {
        lifecycleScope.launch {
            val online = mutableListOf<EspecieResponse>()
            if (estudoRemoteId > 0) {
                try {
                    val resp = RetrofitClient.apiService.getEspecies(
                        SessionManager.getAuthHeader(),
                        estudoRemoteId
                    )
                    if (resp.isSuccessful) online.addAll(resp.body() ?: emptyList())
                } catch (_: Exception) {
                }
            }

            val repo = OfflineRepository(this@RegistrosActivity)
            val resolvedEstudoLocal = if (estudoLocalId > 0) estudoLocalId
            else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
            else null
            resolvedEstudoLocal?.let { id ->
                online.forEach { e ->
                    try {
                        repo.cacheEspecie(id, e)
                    } catch (_: Exception) {
                    }
                }
            }

            especies.clear()
            especies.addAll(online)
            if (resolvedEstudoLocal != null) {
                val remoteIds = online.map { it.id }.toSet()
                repo.listarEspeciesPorEstudoLocal(resolvedEstudoLocal).forEach { off ->
                    if (off.id < 0 || !remoteIds.contains(off.id)) especies.add(off)
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun carregarDetalhes() {
        lifecycleScope.launch {
            if (eventoId > 0 && estudoRemoteId > 0 && campanhaId > 0 && unidadeId > 0) {
                try {
                    val resp = RetrofitClient.apiService.getEvento(
                        SessionManager.getAuthHeader(),
                        estudoRemoteId,
                        campanhaId,
                        unidadeId,
                        eventoId
                    )
                    resp.body()?.let { e ->
                        val repo = OfflineRepository(this@RegistrosActivity)
                        val uLocal = if (unidadeLocalId > 0) unidadeLocalId
                                     else repo.estudoLocalIdFromRemote(estudoRemoteId)?.let { eL ->
                                         repo.campanhaLocalIdFromRemote(eL, campanhaId)?.let { cL ->
                                             repo.unidadeLocalIdFromRemote(cL, unidadeId)
                                         }
                                     }
                        if (uLocal != null) try { repo.cacheEvento(uLocal, e) } catch (_: Exception) { }
                        preencherCardEvento(e.horarioInicio, e.esforcoReal, e.updatedAt)
                        renderizarValoresCard(e.valoresVariaveis, fetchVarNomesApi())
                        return@launch
                    }
                } catch (_: Exception) {
                }
            }
            preencherDetalhesEventoOffline()
        }
    }

    private suspend fun fetchVarNomesApi(): Map<Int, Pair<String, String?>> {
        if (estudoRemoteId <= 0) return emptyMap()
        return try {
            RetrofitClient.apiService.getVariaveis(SessionManager.getAuthHeader(), estudoRemoteId)
                .body()?.associate { v -> v.id to (v.nome to v.metrica) } ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun preencherCardEvento(inicio: String, esforco: String?, updatedAt: String? = null) {
        binding.tvDetalheInicio.text = formatarDataHora(inicio)
        binding.tvDetalheEsforco.text = esforco ?: "—"
        binding.tvDetalheUpdatedAt.text = updatedAt?.let { formatarDataHora(it) } ?: "—"
    }

    private fun formatarDataHora(iso: String): String = try {
        val s = iso.replace("T", " ")
        val partes = s.split(" ")
        val d = partes[0].split("-")
        val h = if (partes.size > 1) partes[1].take(5) else ""
        val data = if (d.size == 3) "${d[2]}/${d[1]}/${d[0]}" else partes[0]
        if (h.isNotEmpty()) "$data, ${h}h" else data
    } catch (_: Exception) {
        iso
    }

    private fun preencherDetalhesEventoOffline() {
        val eventoDao = EventoDao(this)
        val repo = OfflineRepository(this)
        val evento = when {
            eventoLocalId > 0 -> eventoDao.listarTodos().firstOrNull { it.localId == eventoLocalId }
            eventoId < 0 -> eventoDao.listarTodos()
                .firstOrNull { it.localId == (-eventoId).toLong() }

            eventoId > 0 -> eventoDao.listarTodos().firstOrNull { it.remoteId == eventoId }
            else -> eventoDao.listarTodos().firstOrNull { it.horarioInicio == eventoNome }
        } ?: return
        preencherCardEvento(evento.horarioInicio, evento.esforcoReal)
        renderizarValoresCard(repo.listarValoresPorEvento(evento.localId))
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

    private fun carregarRegistros() {
        if (eventoId <= 0) {
            val localId = if (eventoLocalId > 0) eventoLocalId
            else if (eventoId < 0) (-eventoId).toLong()
            else EventoDao(this).listarTodos()
                .firstOrNull { it.horarioInicio == eventoNome }?.localId
            if (localId != null) {
                eventoLocalId = localId
                carregarRegistrosOffline(localId)
            }
            return
        }
        if (estudoRemoteId <= 0 || campanhaId <= 0 || unidadeId <= 0) {
            if (eventoLocalId > 0) carregarRegistrosOffline(eventoLocalId)
            return
        }

        lifecycleScope.launch {
            val registrosOnline = mutableListOf<RegistroResponse>()

            try {
                val resp = RetrofitClient.apiService.getRegistros(
                    SessionManager.getAuthHeader(), estudoRemoteId, campanhaId, unidadeId, eventoId
                )
                if (resp.isSuccessful) {
                    registrosOnline.addAll(resp.body() ?: emptyList())
                }
            } catch (_: Exception) {
            }

            registros.clear()
            registros.addAll(registrosOnline)

            val resolvedEstudoLocalId =
                EstudoDao(this@RegistrosActivity).buscarPorRemoteId(estudoRemoteId)?.localId
            if (resolvedEstudoLocalId != null) this@RegistrosActivity.estudoLocalId = resolvedEstudoLocalId
            val resolvedCampanhaLocalId = resolvedEstudoLocalId?.let {
                com.kheprix.db.CampanhaDao(this@RegistrosActivity)
                    .buscarPorRemoteIdEscopo(campanhaId, it)?.localId
            }
            if (resolvedCampanhaLocalId != null) this@RegistrosActivity.campanhaLocalId = resolvedCampanhaLocalId
            val resolvedUnidadeLocalId = resolvedCampanhaLocalId?.let {
                com.kheprix.db.UnidadeDao(this@RegistrosActivity)
                    .buscarPorRemoteIdEscopo(unidadeId, it)?.localId
            }
            if (resolvedUnidadeLocalId != null) this@RegistrosActivity.unidadeLocalId = resolvedUnidadeLocalId
            val resolvedEventoLocalId = resolvedUnidadeLocalId?.let {
                EventoDao(this@RegistrosActivity).buscarPorRemoteIdEscopo(eventoId, it)?.localId
            }
            if (resolvedEventoLocalId != null) this@RegistrosActivity.eventoLocalId = resolvedEventoLocalId

            if (resolvedEventoLocalId != null) {
                val registroDao = RegistroDao(this@RegistrosActivity)
                val offline = registroDao.listarPorEventoLocal(resolvedEventoLocalId)
                val remoteIds = registrosOnline.mapNotNull { it.id }.toSet()
                offline.forEach { off ->
                    if (off.remoteId == null || !remoteIds.contains(off.remoteId)) {
                        registros.add(
                            RegistroResponse(
                                id = off.remoteId ?: -off.localId.toInt(),
                                especieId = off.especieRemoteId,
                                eventoAmostragemId = off.eventoLocalId.toInt(),
                                data = off.data,
                                hora = off.hora,
                                latitude = off.latitude,
                                longitude = off.longitude,
                                qtdeIndividuos = off.qtdeIndividuos,
                                foto = off.foto,
                                ausenciaEspecie = off.ausenciaEspecie,
                                createdAt = off.createdAt ?: ""
                            )
                        )
                    }
                }
            }

            aplicarFiltro(especieIdFiltro)
        }
    }

    private fun carregarRegistrosOffline(eventoLocalId: Long) {
        val registroDao = RegistroDao(this)
        registros.clear()
        registros.addAll(registroDao.listarPorEventoLocal(eventoLocalId).map { off ->
            RegistroResponse(
                id = off.remoteId ?: -off.localId.toInt(),
                especieId = off.especieRemoteId,
                eventoAmostragemId = off.eventoLocalId.toInt(),
                data = off.data,
                hora = off.hora,
                latitude = off.latitude,
                longitude = off.longitude,
                qtdeIndividuos = off.qtdeIndividuos,
                foto = off.foto,
                ausenciaEspecie = off.ausenciaEspecie,
                createdAt = off.createdAt ?: ""
            )
        })
        aplicarFiltro(especieIdFiltro)
    }

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
            putExtra("estudo_local_id", estudoLocalId)
            putExtra("campanha_id", campanhaId)
            putExtra("campanha_local_id", campanhaLocalId)
            putExtra("unidade_id", unidadeId)
            putExtra("unidade_local_id", unidadeLocalId)
            putExtra("evento_id", eventoId)
            putExtra("evento_local_id", eventoLocalId)
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
        val tvId: TextView = view.findViewById(R.id.tvRegistroId)
        val tvData: TextView = view.findViewById(R.id.tvRegistroData)
        val ivFoto: ImageView = view.findViewById(R.id.ivRegistroFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_registro, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val especie = buscarEspecie(item.especieId)
        holder.tvId.text =
            if (especie != null) "${especie.genero} ${especie.especie}" else "Registro #${item.id}"
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

class NovoRegistroActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityNovoRegistroBinding

    private var estudoRemoteId = -1
    private var estudoLocalId = -1L
    private var campanhaId = -1
    private var campanhaLocalId = -1L
    private var unidadeId = -1
    private var unidadeLocalId = -1L
    private var eventoId = -1
    private var eventoLocalId = -1L
    private var registroId = -1
    private var modoEdicao = false

    private var fotoBase64: String? = null
    private var cameraUri: Uri? = null
    private var dataStr = ""
    private var horaStr = ""
    private var latDecimal: Double? = null
    private var lonDecimal: Double? = null

    private val especies = mutableListOf<EspecieResponse>()
    private val variaveis = mutableListOf<VariavelResponse>()
    private val camposVariavel = mutableMapOf<Int, View>()

    private var registroCarregado: RegistroResponse? = null

    private val locationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) obterLocalizacao()
            else Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
        }

    private val galeria =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            if (r.resultCode == Activity.RESULT_OK) processarImagem(r.data?.data)
        }

    private val camera =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            if (r.resultCode == Activity.RESULT_OK) processarImagem(cameraUri)
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) abrirCamera()
            else Toast.makeText(this, "Permissão de câmera necessária", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovoRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId = intent.getLongExtra("estudo_local_id", -1L)
        campanhaId = intent.getIntExtra("campanha_id", -1)
        campanhaLocalId = intent.getLongExtra("campanha_local_id", -1L)
        if (campanhaId < 0 && campanhaLocalId <= 0) campanhaLocalId = (-campanhaId).toLong()
        unidadeId = intent.getIntExtra("unidade_id", -1)
        unidadeLocalId = intent.getLongExtra("unidade_local_id", -1L)
        if (unidadeId < 0 && unidadeLocalId <= 0) unidadeLocalId = (-unidadeId).toLong()
        eventoId = intent.getIntExtra("evento_id", -1)
        eventoLocalId = intent.getLongExtra("evento_local_id", -1L)
        if (eventoId < 0 && eventoLocalId <= 0) eventoLocalId = (-eventoId).toLong()
        registroId = intent.getIntExtra("registro_id", -1)
        modoEdicao = registroId != -1

        binding.tvTitulo.text =
            if (modoEdicao) "Editar Registro:" else "Novo Registro de Ocorrência:"

        binding.ivGaleria.setOnClickListener {
            galeria.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
        }
        binding.ivCamera.setOnClickListener { verificarPermissaoCamera() }
        binding.ivCalendario.setOnClickListener { abrirDatePicker() }
        binding.etHora.setOnClickListener { abrirTimePicker() }
        binding.ivGpsLat.setOnClickListener { verificarPermissaoGps() }
        binding.ivGpsLon.setOnClickListener { verificarPermissaoGps() }

        binding.btnConfirmar.setOnClickListener {
            if (modoEdicao) editarRegistro() else criarRegistro()
        }
        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        aplicarMascaraDms(binding.etLatitude)
        aplicarMascaraDms(binding.etLongitude)

        carregarEspecies()
        carregarVariaveis()
        if (modoEdicao) preencherEdicao()
    }

    private fun processarImagem(uri: Uri?) {
        uri ?: return
        lifecycleScope.launch {
            val base64 = withContext(Dispatchers.IO) {
                PhotoUtils.uriToBase64(
                    this@NovoRegistroActivity,
                    uri
                )
            }
            fotoBase64 = base64
            binding.tvNomeFoto.text = uri.lastPathSegment ?: "foto.jpg"
            if (base64 != null) {
                val bmp = withContext(Dispatchers.IO) { PhotoUtils.base64ToBitmap(base64) }
                if (bmp != null) {
                    binding.ivPreviewFoto.setImageBitmap(bmp)
                    binding.ivPreviewFoto.visibility = View.VISIBLE
                }
            } else {
                Toast.makeText(
                    this@NovoRegistroActivity,
                    "Não foi possível carregar a foto",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun verificarPermissaoCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            abrirCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamera() {
        val arquivo = File.createTempFile("reg_", ".jpg", cacheDir)
        cameraUri = FileProvider.getUriForFile(this, "${packageName}.provider", arquivo)
        camera.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
        })
    }

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
                    Toast.makeText(
                        this,
                        "GPS indisponível. Ative a localização.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun decimalToDms(dec: Double): String {
        val neg = dec < 0;
        val abs = Math.abs(dec)
        val deg = abs.toInt();
        val minD = (abs - deg) * 60
        val min = minD.toInt();
        val sec = ((minD - min) * 60).toInt()
        return "${if (neg) "-" else ""}%02d°%02d'%02d\"".format(deg, min, sec)
    }

    private fun aplicarMascaraDms(et: android.widget.EditText) {
        et.addTextChangedListener(object : android.text.TextWatcher {
            private var editing = false
            private var prevLen = 0
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (editing || s == null) return
                editing = true
                val deleting = s.length < prevLen
                prevLen = s.length
                if (!deleting) {
                    val raw = s.toString()
                    val neg = raw.startsWith("-")
                    val digits = raw.filter { it.isDigit() }.take(6)
                    val sb = StringBuilder()
                    if (neg) sb.append("-")
                    digits.forEachIndexed { i, c ->
                        sb.append(c)
                        if (i == 1 && digits.length > 1) sb.append("°")
                        else if (i == 3 && digits.length > 3) sb.append("'")
                        else if (i == 5) sb.append("\"")
                    }
                    val result = sb.toString()
                    if (result != raw) {
                        s.replace(0, s.length, result)
                        prevLen = result.length
                        try {
                            et.setSelection(result.length)
                        } catch (_: Exception) {
                        }
                    }
                }
                editing = false
            }
        })
    }

    private fun parseCoordenada(texto: String): Double? {
        texto.trim().toDoubleOrNull()?.let { return it }
        val r = Regex("""(-?\d+)°(\d+)'(\d+)""").find(texto.trim()) ?: return null
        val (d, m, s) = r.destructured
        val dv = d.toInt();
        val abs = Math.abs(dv) + m.toInt() / 60.0 + s.toInt() / 3600.0
        return if (dv < 0) -abs else abs
    }

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

    private fun carregarEspecies() {
        lifecycleScope.launch {
            val online = mutableListOf<EspecieResponse>()
            if (estudoRemoteId > 0) {
                try {
                    val resp = RetrofitClient.apiService.getEspecies(
                        SessionManager.getAuthHeader(),
                        estudoRemoteId
                    )
                    if (resp.isSuccessful) online.addAll(resp.body() ?: emptyList())
                } catch (_: Exception) {
                }
            }

            val repo = OfflineRepository(this@NovoRegistroActivity)
            val resolvedEstudoLocal = if (estudoLocalId > 0) estudoLocalId
            else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
            else null
            resolvedEstudoLocal?.let { id ->
                online.forEach { e ->
                    try {
                        repo.cacheEspecie(id, e)
                    } catch (_: Exception) {
                    }
                }
            }

            especies.clear()
            especies.addAll(online)
            if (resolvedEstudoLocal != null) {
                val remoteIds = online.map { it.id }.toSet()
                repo.listarEspeciesPorEstudoLocal(resolvedEstudoLocal).forEach { off ->
                    if (off.id < 0 || !remoteIds.contains(off.id)) especies.add(off)
                }
            }

            val nomes = listOf("Selecione...") + especies.map { "${it.genero} ${it.especie}" }
            binding.spinnerEspecie.adapter = ArrayAdapter(
                this@NovoRegistroActivity, android.R.layout.simple_spinner_item, nomes
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
    }

    private fun carregarVariaveis() {
        if (estudoRemoteId <= 0) {
            carregarVariaveisOffline(); return
        }
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getVariaveis(
                    SessionManager.getAuthHeader(), estudoRemoteId, nivelAplicacao = "registro"
                )
                if (resp.isSuccessful) {
                    variaveis.clear(); variaveis.addAll(resp.body() ?: emptyList())
                    val repo = OfflineRepository(this@NovoRegistroActivity)
                    val eLocal = if (estudoLocalId > 0) estudoLocalId
                                 else repo.estudoLocalIdFromRemote(estudoRemoteId)
                    if (eLocal != null) try { repo.cacheVariaveis(eLocal, variaveis) } catch (_: Exception) { }
                    renderizarVariaveis()
                } else carregarVariaveisOffline()
            } catch (_: Exception) {
                carregarVariaveisOffline()
            }
        }
    }

    private fun carregarVariaveisOffline() {
        if (estudoLocalId <= 0) {
            renderizarVariaveis(); return
        }
        val db = com.kheprix.db.DatabaseHelper(this).readableDatabase
        variaveis.clear()
        db.rawQuery(
            "SELECT remote_id, nome, nivel_aplicacao, tipo_dado, metrica, created_at, updated_at, local_id FROM variaveis WHERE estudo_local_id = ? AND nivel_aplicacao = 'registro'",
            arrayOf(estudoLocalId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val rid = if (c.isNull(0)) -c.getLong(7).toInt() else c.getInt(0)
                variaveis.add(
                    VariavelResponse(
                        id = rid,
                        nome = c.getString(1),
                        nivelAplicacao = c.getString(2),
                        tipoDado = c.getString(3),
                        metrica = c.getString(4),
                        createdAt = c.getString(5) ?: "",
                        updatedAt = c.getString(6) ?: ""
                    )
                )
            }
        }
        renderizarVariaveis()
    }

    private fun renderizarVariaveis() {
        binding.layoutVariaveis.removeAllViews(); camposVariavel.clear()
        if (variaveis.isEmpty()) {
            binding.tvVariaveisTitle.visibility = View.GONE; return
        }
        binding.tvVariaveisTitle.visibility = View.VISIBLE
        variaveis.forEachIndexed { i, v ->
            val label = TextView(this).apply {
                text = "${v.nome}:"; textSize = 13f; setTextColor(0xFF6B7A5E.toInt())
                setPadding(0, if (i > 0) 12 else 0, 0, 0); typeface =
                android.graphics.Typeface.MONOSPACE
            }
            binding.layoutVariaveis.addView(label)
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity =
                android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 4 }
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

        aplicarValoresVariaveis()
    }

    private fun aplicarValoresVariaveis() {
        val r = registroCarregado ?: return
        if (camposVariavel.isEmpty()) return
        r.valoresVariaveis?.forEach { vv ->
            when (val view = camposVariavel[vv.variavelId]) {
                is Spinner -> view.setSelection(
                    when (vv.valor.trim().lowercase()) {
                        "true", "verdadeiro" -> 1
                        "false", "falso" -> 2
                        else -> 0
                    }
                )

                is EditText -> view.setText(vv.valor)
                else -> {}
            }
        }
    }

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

    private fun preencherEdicao() {
        if (estudoRemoteId > 0 && campanhaId > 0 && unidadeId > 0 && eventoId > 0 && registroId > 0) {
            lifecycleScope.launch {
                try {
                    val resp = RetrofitClient.apiService.getRegistro(
                        SessionManager.getAuthHeader(),
                        estudoRemoteId,
                        campanhaId,
                        unidadeId,
                        eventoId,
                        registroId
                    )
                    val r = resp.body()
                    if (r != null) {
                        val repo = OfflineRepository(this@NovoRegistroActivity)
                        val evtLocal = if (eventoLocalId > 0) eventoLocalId
                                       else repo.estudoLocalIdFromRemote(estudoRemoteId)?.let { eL ->
                                           repo.campanhaLocalIdFromRemote(eL, campanhaId)?.let { cL ->
                                               repo.unidadeLocalIdFromRemote(cL, unidadeId)?.let { uL ->
                                                   repo.eventoLocalIdFromRemote(uL, eventoId)
                                               }
                                           }
                                       }
                        val resolvedEstudo = if (estudoLocalId > 0) estudoLocalId
                                             else repo.estudoLocalIdFromRemote(estudoRemoteId)
                        val especieLocal = resolvedEstudo?.let { repo.resolveEspecieLocalId(it, r.especieId) }
                        if (evtLocal != null && especieLocal != null) {
                            try { repo.cacheRegistro(evtLocal, especieLocal, r) } catch (_: Exception) { }
                        }
                        preencherForm(r)
                    } else preencherFormOffline()
                } catch (_: Exception) {
                    preencherFormOffline()
                }
            }
        } else {
            preencherFormOffline()
        }
    }

    private fun resolveRegistroLocalId(): Long? {
        if (registroId < 0) return (-registroId).toLong()
        val evtLocal = if (eventoLocalId > 0) eventoLocalId
        else {
            val repo = OfflineRepository(this)
            val eL = if (estudoLocalId > 0) estudoLocalId
            else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
            else null
            val cL = eL?.let { repo.campanhaLocalIdFromRemote(it, campanhaId) }
            val uL = cL?.let { repo.unidadeLocalIdFromRemote(it, unidadeId) }
            uL?.let { repo.eventoLocalIdFromRemote(it, eventoId) }
        } ?: return null
        return RegistroDao(this).buscarPorRemoteIdEscopo(registroId, evtLocal)?.localId
    }

    private fun preencherFormOffline() {
        val localId = resolveRegistroLocalId()
        if (localId == null) return
        val off = RegistroDao(this).buscarPorLocalId(localId) ?: return
        val valoresOffline = OfflineRepository(this).listarValoresPorRegistro(localId)
        val r = RegistroResponse(
            id = off.remoteId ?: -off.localId.toInt(),
            eventoAmostragemId = off.eventoLocalId.toInt(),
            especieId = off.especieRemoteId,
            data = off.data,
            hora = off.hora,
            latitude = off.latitude,
            longitude = off.longitude,
            qtdeIndividuos = off.qtdeIndividuos,
            foto = off.foto,
            ausenciaEspecie = off.ausenciaEspecie,
            createdAt = off.createdAt ?: "",
            valoresVariaveis = valoresOffline.ifEmpty { null }
        )
        preencherForm(r)
    }

    private fun preencherForm(r: RegistroResponse) {
        registroCarregado = r
        val dp = r.data.split("-")
        dataStr = r.data
        binding.etData.setText(if (dp.size == 3) "${dp[2]}/${dp[1]}/${dp[0]}" else r.data)
        horaStr = r.hora
        binding.etHora.setText(r.hora.take(5))
        latDecimal = r.latitude; lonDecimal = r.longitude
        binding.etLatitude.setText(decimalToDms(r.latitude))
        binding.etLongitude.setText(decimalToDms(r.longitude))
        binding.etQtde.setText(r.qtdeIndividuos?.toString() ?: "")
        if (!r.foto.isNullOrBlank()) {
            binding.tvNomeFoto.text = "foto_atual.jpg"
            binding.ivPreviewFoto.visibility = View.VISIBLE
            ImagemLoader.load(
                lifecycleScope,
                binding.ivPreviewFoto,
                r.foto,
                R.drawable.ic_placeholder_beetle
            )
        }
        val idx = especies.indexOfFirst { it.id == r.especieId }
        if (idx >= 0) binding.spinnerEspecie.setSelection(idx + 1)
        aplicarValoresVariaveis()
    }

    private fun criarRegistro() {
        val req = coletarFormulario() ?: return
        if (estudoRemoteId <= 0 || campanhaId <= 0 || unidadeId <= 0 || eventoId <= 0) {
            mostrarDialogOffline(req)
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.postRegistro(
                    SessionManager.getAuthHeader(),
                    estudoRemoteId,
                    campanhaId,
                    unidadeId,
                    eventoId,
                    req
                )
                if (resp.isSuccessful) {
                    Toast.makeText(
                        this@NovoRegistroActivity,
                        "Registro criado!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@NovoRegistroActivity,
                        "Erro: ${resp.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                mostrarDialogOffline(req)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun editarRegistro() {
        val form = coletarFormulario() ?: return
        val patchReq = RegistroPatchRequest(
            especieId = form.especieId, data = form.data, hora = form.hora,
            latitude = form.latitude, longitude = form.longitude,
            qtdeIndividuos = form.qtdeIndividuos, foto = form.foto,
            valoresVariaveis = form.valoresVariaveis
        )
        if (estudoRemoteId <= 0 || campanhaId <= 0 || unidadeId <= 0 || eventoId <= 0 || registroId <= 0) {
            salvarEdicaoOffline(patchReq, "Registro atualizado offline.")
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.patchRegistro(
                    SessionManager.getAuthHeader(),
                    estudoRemoteId,
                    campanhaId,
                    unidadeId,
                    eventoId,
                    registroId,
                    patchReq
                )
                if (resp.isSuccessful) {
                    Toast.makeText(
                        this@NovoRegistroActivity,
                        "Registro atualizado!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@NovoRegistroActivity,
                        "Erro: ${resp.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                salvarEdicaoOffline(patchReq, "Sem conexão — alterações salvas offline.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun salvarEdicaoOffline(req: RegistroPatchRequest, msg: String) {
        val repo = OfflineRepository(this)
        val localId = resolveRegistroLocalId()
        if (localId == null) {
            Toast.makeText(this, "Registro não encontrado offline.", Toast.LENGTH_LONG).show()
            return
        }
        val resolvedEstudoLocal = if (estudoLocalId > 0) estudoLocalId
        else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
        else null
        val novoEspecieLocalId = req.especieId?.let { eid ->
            resolvedEstudoLocal?.let { repo.resolveEspecieLocalId(it, eid) }
        }
        try {
            repo.editarRegistroOffline(localId, req, novoEspecieLocalId)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar offline: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun coletarFormulario(): RegistroRequest? {
        val espPos = binding.spinnerEspecie.selectedItemPosition
        if (espPos == 0) {
            Toast.makeText(this, "Selecione a espécie", Toast.LENGTH_SHORT).show(); return null
        }
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
            Toast.makeText(this, "Preencha coordenadas válidas", Toast.LENGTH_SHORT)
                .show(); return null
        }
        return RegistroRequest(
            especieId = especie.id, data = dataFinal, hora = horaFinal,
            latitude = lat, longitude = lon,
            qtdeIndividuos = binding.etQtde.text.toString().trim().toIntOrNull(),
            foto = fotoBase64,
            valoresVariaveis = coletarValoresVariaveis()
        )
    }

    private fun coletarValoresVariaveis(): List<ValorVariavelRequest>? {
        if (camposVariavel.isEmpty()) return null
        val idPorVariavel: Map<Int, Int> =
            registroCarregado?.valoresVariaveis
                ?.mapNotNull { vv -> vv.id?.let { vv.variavelId to it } }
                ?.toMap() ?: emptyMap()

        val itens = camposVariavel.entries.mapNotNull { (varId, view) ->
            val valor = when (view) {
                is Spinner -> when (view.selectedItem?.toString()) {
                    "Verdadeiro" -> "true"
                    "Falso" -> "false"
                    else -> null
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

    private fun mostrarDialogOffline(req: RegistroRequest?) {
        if (req != null) {
            val salvo = salvarRegistroOffline(req)
            if (!salvo) return
        }
        startActivity(Intent(this, OfflineWarningActivity::class.java))
        finish()
    }

    private fun salvarRegistroOffline(req: RegistroRequest): Boolean {
        val repo = OfflineRepository(this)
        val eventoResolved = when {
            eventoLocalId > 0 -> eventoLocalId
            estudoRemoteId > 0 && campanhaId > 0 && unidadeId > 0 && eventoId > 0 ->
                repo.estudoLocalIdFromRemote(estudoRemoteId)?.let { eL ->
                    repo.campanhaLocalIdFromRemote(eL, campanhaId)?.let { cL ->
                        repo.unidadeLocalIdFromRemote(cL, unidadeId)?.let { uL ->
                            repo.resolveEventoLocalId(uL, eventoId)
                        }
                    }
                }

            else -> null
        }
        if (eventoResolved == null) {
            Toast.makeText(this, "Evento não está salvo offline.", Toast.LENGTH_LONG)
                .show(); return false
        }
        val resolvedEstudoLocal = if (estudoLocalId > 0) estudoLocalId
        else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
        else null
        if (resolvedEstudoLocal == null) {
            Toast.makeText(this, "Estudo não está salvo offline.", Toast.LENGTH_LONG)
                .show(); return false
        }
        val especieLocalId = repo.resolveEspecieLocalId(resolvedEstudoLocal, req.especieId) ?: run {
            Toast.makeText(this, "Espécie não está salva offline.", Toast.LENGTH_LONG)
                .show(); return false
        }
        return try {
            repo.criarRegistroOffline(eventoResolved, especieLocalId, req)
            true
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar offline: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }
}

class RegistroDetalheActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityRegistrosDetalheBinding
    private var estudoRemoteId = -1
    private var estudoLocalId = -1L
    private var campanhaId = -1
    private var campanhaLocalId = -1L
    private var unidadeId = -1
    private var unidadeLocalId = -1L
    private var eventoId = -1
    private var eventoLocalId = -1L
    private var registroId = -1

    private var registroCarregado: RegistroResponse? = null
    private val variaveis = mutableListOf<VariavelResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrosDetalheBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId = intent.getLongExtra("estudo_local_id", -1L)
        campanhaId = intent.getIntExtra("campanha_id", -1)
        campanhaLocalId = intent.getLongExtra("campanha_local_id", -1L)
        if (campanhaId < 0 && campanhaLocalId <= 0) campanhaLocalId = (-campanhaId).toLong()
        unidadeId = intent.getIntExtra("unidade_id", -1)
        unidadeLocalId = intent.getLongExtra("unidade_local_id", -1L)
        if (unidadeId < 0 && unidadeLocalId <= 0) unidadeLocalId = (-unidadeId).toLong()
        eventoId = intent.getIntExtra("evento_id", -1)
        eventoLocalId = intent.getLongExtra("evento_local_id", -1L)
        if (eventoId < 0 && eventoLocalId <= 0) eventoLocalId = (-eventoId).toLong()
        registroId = intent.getIntExtra("registro_id", -1)

        binding.ivEditar.setOnClickListener {
            startActivity(Intent(this, NovoRegistroActivity::class.java).apply {
                putExtra("estudo_remote_id", estudoRemoteId)
                putExtra("estudo_local_id", estudoLocalId)
                putExtra("campanha_id", campanhaId)
                putExtra("campanha_local_id", campanhaLocalId)
                putExtra("unidade_id", unidadeId)
                putExtra("unidade_local_id", unidadeLocalId)
                putExtra("evento_id", eventoId)
                putExtra("evento_local_id", eventoLocalId)
                putExtra("registro_id", registroId)
            })
        }
        binding.ivDeletar.setOnClickListener { confirmarDelete() }
        binding.ivMenuLateral.setOnClickListener { openDrawer() }
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
        if (registroId <= 0 || estudoRemoteId <= 0 || campanhaId <= 0 || unidadeId <= 0 || eventoId <= 0) {
            carregarRegistroOffline()
            return
        }
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getRegistro(
                    SessionManager.getAuthHeader(),
                    estudoRemoteId,
                    campanhaId,
                    unidadeId,
                    eventoId,
                    registroId
                )
                val r = resp.body()
                if (r != null) {
                    val repo = OfflineRepository(this@RegistroDetalheActivity)
                    val evtLocal = if (eventoLocalId > 0) eventoLocalId
                                   else repo.estudoLocalIdFromRemote(estudoRemoteId)?.let { eL ->
                                       repo.campanhaLocalIdFromRemote(eL, campanhaId)?.let { cL ->
                                           repo.unidadeLocalIdFromRemote(cL, unidadeId)?.let { uL ->
                                               repo.eventoLocalIdFromRemote(uL, eventoId)
                                           }
                                       }
                                   }
                    val resolvedEstudo = if (estudoLocalId > 0) estudoLocalId
                                         else repo.estudoLocalIdFromRemote(estudoRemoteId)
                    val especieLocal = resolvedEstudo?.let { repo.resolveEspecieLocalId(it, r.especieId) }
                    if (evtLocal != null && especieLocal != null) {
                        try { repo.cacheRegistro(evtLocal, especieLocal, r) } catch (_: Exception) { }
                    }
                    preencherRegistro(r)
                } else carregarRegistroOffline()
            } catch (_: Exception) {
                carregarRegistroOffline()
            }
        }
    }

    private fun resolveRegistroLocalId(): Long? {
        if (registroId < 0) return (-registroId).toLong()
        val evtLocal = if (eventoLocalId > 0) eventoLocalId
        else {
            val repo = OfflineRepository(this)
            val eL = if (estudoLocalId > 0) estudoLocalId
            else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
            else null
            val cL = eL?.let { repo.campanhaLocalIdFromRemote(it, campanhaId) }
            val uL = cL?.let { repo.unidadeLocalIdFromRemote(it, unidadeId) }
            uL?.let { repo.eventoLocalIdFromRemote(it, eventoId) }
        } ?: return null
        return RegistroDao(this).buscarPorRemoteIdEscopo(registroId, evtLocal)?.localId
    }

    private fun carregarRegistroOffline() {
        val localId = resolveRegistroLocalId()
        if (localId == null) return
        val off = RegistroDao(this).buscarPorLocalId(localId) ?: return
        val valoresOffline = OfflineRepository(this).listarValoresPorRegistro(localId)
        val r = RegistroResponse(
            id = off.remoteId ?: -off.localId.toInt(),
            eventoAmostragemId = off.eventoLocalId.toInt(),
            especieId = off.especieRemoteId,
            data = off.data,
            hora = off.hora,
            latitude = off.latitude,
            longitude = off.longitude,
            qtdeIndividuos = off.qtdeIndividuos,
            foto = off.foto,
            ausenciaEspecie = off.ausenciaEspecie,
            createdAt = off.createdAt ?: "",
            valoresVariaveis = valoresOffline.ifEmpty { null }
        )
        preencherRegistro(r)
    }

    private fun preencherRegistro(r: RegistroResponse) {
        registroCarregado = r
        binding.tvQtde.text = r.qtdeIndividuos?.toString() ?: "—"
        binding.tvLatitude.text = r.latitude.toString()
        binding.tvLongitude.text = r.longitude.toString()
        binding.tvData.text = r.data.split("-").let { p ->
            if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else r.data
        }
        binding.tvHora.text = "${r.hora.take(5)}h"

        ImagemLoader.load(
            scope = lifecycleScope,
            target = binding.ivFoto,
            url = r.foto,
            placeholder = R.drawable.ic_placeholder_beetle
        )

        carregarEspecie(r.especieId)
        renderizarVariaveis()
    }

    private fun carregarEspecie(especieId: Int) {
        if (estudoRemoteId > 0 && especieId > 0) {
            lifecycleScope.launch {
                try {
                    val resp = RetrofitClient.apiService.getEspecie(
                        SessionManager.getAuthHeader(), estudoRemoteId, especieId
                    )
                    val e = resp.body()
                    if (e != null) preencherEspecie(e) else carregarEspecieOffline(especieId)
                } catch (_: Exception) {
                    carregarEspecieOffline(especieId)
                }
            }
        } else {
            carregarEspecieOffline(especieId)
        }
    }

    private fun carregarEspecieOffline(especieId: Int) {
        val repo = OfflineRepository(this)
        val resolvedEstudoLocal = if (estudoLocalId > 0) estudoLocalId
        else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
        else null
        if (resolvedEstudoLocal == null) return
        val match = repo.listarEspeciesPorEstudoLocal(resolvedEstudoLocal)
            .firstOrNull { it.id == especieId }
            ?: return
        preencherEspecie(match)
    }

    private fun preencherEspecie(e: com.kheprix.models.EspecieResponse) {
        binding.tvTitulo.text = "${e.genero} ${e.especie}"
        binding.tvNomeCientifico.text = "${e.genero} ${e.especie}"
        binding.tvNomePopular.text = e.nomePopular ?: "—"
    }

    private fun carregarVariaveis() {
        if (estudoRemoteId <= 0) {
            carregarVariaveisOffline(); return
        }
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getVariaveis(
                    SessionManager.getAuthHeader(), estudoRemoteId, nivelAplicacao = "registro"
                )
                if (resp.isSuccessful) {
                    variaveis.clear()
                    variaveis.addAll(resp.body() ?: emptyList())
                    val repo = OfflineRepository(this@RegistroDetalheActivity)
                    val eLocal = if (estudoLocalId > 0) estudoLocalId
                                 else repo.estudoLocalIdFromRemote(estudoRemoteId)
                    if (eLocal != null) try { repo.cacheVariaveis(eLocal, variaveis) } catch (_: Exception) { }
                    renderizarVariaveis()
                } else carregarVariaveisOffline()
            } catch (_: Exception) {
                carregarVariaveisOffline()
            }
        }
    }

    private fun carregarVariaveisOffline() {
        if (estudoLocalId <= 0) {
            renderizarVariaveis(); return
        }
        val db = com.kheprix.db.DatabaseHelper(this).readableDatabase
        variaveis.clear()
        db.rawQuery(
            "SELECT remote_id, nome, nivel_aplicacao, tipo_dado, metrica, created_at, updated_at, local_id FROM variaveis WHERE estudo_local_id = ? AND nivel_aplicacao = 'registro'",
            arrayOf(estudoLocalId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val rid = if (c.isNull(0)) -c.getLong(7).toInt() else c.getInt(0)
                variaveis.add(
                    VariavelResponse(
                        id = rid,
                        nome = c.getString(1),
                        nivelAplicacao = c.getString(2),
                        tipoDado = c.getString(3),
                        metrica = c.getString(4),
                        createdAt = c.getString(5) ?: "",
                        updatedAt = c.getString(6) ?: ""
                    )
                )
            }
        }
        renderizarVariaveis()
    }

    private fun renderizarVariaveis() {
        binding.layoutVariaveis.removeAllViews()
        if (variaveis.isEmpty()) {
            binding.tvVariaveisTitle.visibility = View.GONE
            return
        }
        binding.tvVariaveisTitle.visibility = View.VISIBLE

        val valoresPorVariavel: Map<Int, String> =
            registroCarregado?.valoresVariaveis
                ?.associate { it.variavelId to it.valor }
                ?: emptyMap()

        variaveis.forEachIndexed { i, v ->
            val nome = TextView(this).apply {
                text = "${v.nome}${if (v.metrica.isNullOrBlank()) "" else " (${v.metrica})"}:"
                textSize = 14f
                setTextColor(0xFF6B7A5E.toInt())
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(0, if (i > 0) 10 else 0, 0, 0)
            }
            val valor = TextView(this).apply {
                text = formatarValor(valoresPorVariavel[v.id], v.tipoDado)
                textSize = 15f
                setTextColor(0xFF4A5240.toInt())
                typeface = android.graphics.Typeface.MONOSPACE
            }
            binding.layoutVariaveis.addView(nome)
            binding.layoutVariaveis.addView(valor)
        }
    }

    private fun formatarValor(valor: String?, tipoDado: String): String {
        if (valor.isNullOrBlank()) return "—"
        return if (tipoDado == "boolean") {
            when (valor.trim().lowercase()) {
                "true", "verdadeiro" -> "Verdadeiro"
                "false", "falso" -> "Falso"
                else -> valor
            }
        } else valor
    }

    private fun confirmarDelete() {
        AlertDialog.Builder(this)
            .setTitle("Deletar registro")
            .setMessage("Tem certeza?")
            .setPositiveButton("Deletar") { _, _ ->
                if (registroId < 0 || estudoRemoteId <= 0 || campanhaId <= 0 || unidadeId <= 0 || eventoId <= 0) {
                    val localId = resolveRegistroLocalId()
                    if (localId != null) {
                        com.kheprix.db.DatabaseHelper(this).writableDatabase
                            .delete(
                                "registros_ocorrencia",
                                "local_id = ?",
                                arrayOf(localId.toString())
                            )
                    }
                    finish()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        RetrofitClient.apiService.deleteRegistro(
                            SessionManager.getAuthHeader(),
                            estudoRemoteId, campanhaId, unidadeId, eventoId, registroId
                        )
                        resolveRegistroLocalId()?.let { lid ->
                            com.kheprix.db.DatabaseHelper(this@RegistroDetalheActivity).writableDatabase
                                .delete(
                                    "registros_ocorrencia",
                                    "local_id = ?",
                                    arrayOf(lid.toString())
                                )
                        }
                        finish()
                    } catch (_: Exception) {
                        Toast.makeText(
                            this@RegistroDetalheActivity,
                            "Sem conexão",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }
}

class OfflineWarningActivity : BaseDrawerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_warning)

        window.setBackgroundDrawableResource(android.R.color.transparent)

        findViewById<Button>(R.id.btnContinuar).setOnClickListener {
            finish()
        }
    }
}
