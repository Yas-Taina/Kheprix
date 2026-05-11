package com.kheprix.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityCadastroEspecieBinding
import com.kheprix.databinding.ActivityEspecieDetalheBinding
import com.kheprix.databinding.ActivityEspeciesBinding
import com.kheprix.db.OfflineRepository
import com.kheprix.models.EspecieRequest
import com.kheprix.models.EspeciePatchRequest
import com.kheprix.models.EspecieResponse
import com.kheprix.util.ImagemLoader
import com.kheprix.util.PhotoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EspeciesActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityEspeciesBinding
    private var estudoRemoteId = -1
    private var estudoLocalId  = -1L
    private var estudoNome = ""
    private val especies = mutableListOf<EspecieResponse>()
    private val especiesExibidas = mutableListOf<EspecieResponse>()
    private var filtro: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEspeciesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId  = intent.getLongExtra("estudo_local_id", -1L)
        estudoNome     = intent.getStringExtra("estudo_nome") ?: ""

        binding.tvEstudoNome.text = estudoNome

        binding.rvEspecies.layoutManager = LinearLayoutManager(this)
        binding.rvEspecies.adapter = EspecieAdapter(especiesExibidas, lifecycleScope,
            onItemClick = { especie ->
                val i = Intent(this, EspecieDetalheActivity::class.java)
                i.putExtra("estudo_remote_id", estudoRemoteId)
                i.putExtra("estudo_local_id", estudoLocalId)
                i.putExtra("especie_id", especie.id)
                i.putExtra("estudo_nome", estudoNome)
                startActivity(i)
            }
        )

        binding.btnAdicionarEspecie.setOnClickListener {
            val i = Intent(this, CadastroEspecieActivity::class.java)
            i.putExtra("estudo_remote_id", estudoRemoteId)
            i.putExtra("estudo_local_id", estudoLocalId)
            startActivity(i)
        }

        binding.btnFiltrar.setOnClickListener { abrirDialogoFiltro() }

        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarEspecies()
    }

    override fun onResume() {
        super.onResume()
        carregarEspecies()
    }

    private fun carregarEspecies() {
        lifecycleScope.launch {
            val especiesOnline = mutableListOf<EspecieResponse>()
            if (estudoRemoteId > 0) {
                try {
                    val resp = RetrofitClient.apiService.getEspecies(
                        SessionManager.getAuthHeader(), estudoRemoteId
                    )
                    if (resp.isSuccessful) {
                        especiesOnline.addAll(resp.body() ?: emptyList())
                    }
                } catch (_: Exception) { }
            }

            val repo = OfflineRepository(this@EspeciesActivity)
            var estudoLocalId = if (this@EspeciesActivity.estudoLocalId > 0) this@EspeciesActivity.estudoLocalId
                else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
                else null
            if (estudoLocalId == null && especiesOnline.isNotEmpty()) {
                try {
                    val estudo = RetrofitClient.apiService.getEstudos(SessionManager.getAuthHeader())
                        .body()?.firstOrNull { it.id == estudoRemoteId }
                    if (estudo != null) estudoLocalId = repo.cacheEstudo(estudo)
                } catch (_: Exception) { }
            }

            estudoLocalId?.let { id ->
                especiesOnline.forEach { e ->
                    try { repo.cacheEspecie(id, e) } catch (_: Exception) { }
                }
            }

            especies.clear()
            especies.addAll(especiesOnline)

            if (estudoLocalId != null) {
                val remoteIds = especiesOnline.map { it.id }.toSet()
                val offline = repo.listarEspeciesPorEstudoLocal(estudoLocalId!!)
                offline.forEach { off ->
                    if (off.id < 0 || !remoteIds.contains(off.id)) {
                        especies.add(off)
                    }
                }
            }

            aplicarFiltro()
        }
    }

    private fun abrirDialogoFiltro() {
        val edit = EditText(this).apply {
            hint = "Nome popular, científico, classe..."
            setText(filtro)
            setSingleLine(true)
        }
        val container = FrameLayout(this).apply {
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(edit)
        }
        AlertDialog.Builder(this)
            .setTitle("Filtrar espécies")
            .setView(container)
            .setPositiveButton("Filtrar") { _, _ ->
                filtro = edit.text.toString().trim()
                aplicarFiltro()
            }
            .setNegativeButton("Limpar") { _, _ ->
                filtro = ""
                aplicarFiltro()
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun aplicarFiltro() {
        especiesExibidas.clear()
        if (filtro.isEmpty()) {
            especiesExibidas.addAll(especies)
        } else {
            val q = filtro.lowercase()
            especiesExibidas.addAll(especies.filter { e ->
                (e.nomePopular?.lowercase()?.contains(q) == true) ||
                e.genero.lowercase().contains(q) ||
                e.especie.lowercase().contains(q) ||
                e.classe.lowercase().contains(q) ||
                e.ordem.lowercase().contains(q) ||
                e.familia.lowercase().contains(q)
            })
        }
        binding.rvEspecies.adapter?.notifyDataSetChanged()
    }
}

class EspecieAdapter(
    private val items: List<EspecieResponse>,
    private val scope: CoroutineScope,
    private val onItemClick: (EspecieResponse) -> Unit
) : RecyclerView.Adapter<EspecieAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNomeCientifico: TextView = view.findViewById(R.id.tvNomeCientifico)
        val tvNomePopular: TextView    = view.findViewById(R.id.tvNomePopular)
        val ivFoto: ImageView          = view.findViewById(R.id.ivEspecieFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_especie, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNomeCientifico.text = "${item.genero} ${item.especie}"
        holder.tvNomePopular.text    = item.nomePopular ?: "—"

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

class CadastroEspecieActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityCadastroEspecieBinding

    private var estudoRemoteId = -1
    private var estudoLocalId  = -1L
    private var especieId      = -1
    private var fotoBase64: String? = null
    private var cameraImageUri: Uri? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) abrirCamera()
            else Toast.makeText(this, "Permissão de câmera necessária", Toast.LENGTH_SHORT).show()
        }

    companion object {
        private const val REQ_GALERIA = 101
        private const val REQ_CAMERA  = 102

        val STATUS_CONSERVACAO = listOf(
            "Não avaliada", "Dados insuficientes", "Menos preocupante",
            "Quase ameaçada", "Vulnerável", "Ameaçada",
            "Em perigo crítico", "Extinta na natureza", "Extinta"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroEspecieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId  = intent.getLongExtra("estudo_local_id", -1L)
        especieId      = intent.getIntExtra("especie_id", -1)

        val modoEdicao = especieId != -1
        binding.tvTitulo.text = if (modoEdicao) "Editar Espécie" else "Cadastro de Espécies"

        if (modoEdicao) carregarEspecieParaEdicao()

        setupSpinnerStatus()
        setupFotoListeners()

        binding.btnConfirmar.setOnClickListener {
            if (modoEdicao) editarEspecie() else criarEspecie()
        }

        binding.ivBack.setOnClickListener { finish() }
        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    private fun setupFotoListeners() {
        binding.ivSelecionarGaleria.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQ_GALERIA)
        }

        binding.ivAbrirCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                abrirCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun abrirCamera() {
        val arquivo = File.createTempFile("foto_", ".jpg", cacheDir)
        cameraImageUri = FileProvider.getUriForFile(
            this, "${packageName}.provider", arquivo
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        startActivityForResult(intent, REQ_CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        val uri: Uri? = when (requestCode) {
            REQ_GALERIA -> data?.data
            REQ_CAMERA  -> cameraImageUri
            else        -> null
        }

        uri?.let { u ->
            lifecycleScope.launch {
                val base64 = withContext(Dispatchers.IO) { PhotoUtils.uriToBase64(this@CadastroEspecieActivity, u) }
                fotoBase64 = base64
                binding.tvNomeFoto.text = u.lastPathSegment ?: "foto.jpg"
                if (base64 != null) {
                    val bmp = withContext(Dispatchers.IO) { PhotoUtils.base64ToBitmap(base64) }
                    if (bmp != null) {
                        binding.ivPreviewFoto.setImageBitmap(bmp)
                        binding.ivPreviewFoto.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(this@CadastroEspecieActivity, "Não foi possível carregar a foto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSpinnerStatus() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, STATUS_CONSERVACAO)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = adapter
    }

    private fun carregarEspecieParaEdicao() {
        if (estudoRemoteId > 0 && especieId > 0) {
            lifecycleScope.launch {
                try {
                    val resp = RetrofitClient.apiService.getEspecie(
                        SessionManager.getAuthHeader(), estudoRemoteId, especieId
                    )
                    val e = resp.body()
                    if (e != null) preencherCampos(e) else preencherCamposOffline()
                } catch (_: Exception) { preencherCamposOffline() }
            }
        } else {
            preencherCamposOffline()
        }
    }

    private fun preencherCamposOffline() {
        val repo = com.kheprix.db.OfflineRepository(this)
        val resolvedEstudoLocal = if (estudoLocalId > 0) estudoLocalId
            else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
            else null
        if (resolvedEstudoLocal == null) return
        val match = repo.listarEspeciesPorEstudoLocal(resolvedEstudoLocal)
            .firstOrNull { it.id == especieId }
            ?: return
        preencherCampos(match)
    }

    private fun preencherCampos(e: EspecieResponse) {
        binding.etClasse.setText(e.classe)
        binding.etOrdem.setText(e.ordem)
        binding.etFamilia.setText(e.familia)
        binding.etGenero.setText(e.genero)
        binding.etEspecie.setText(e.especie)
        binding.etNomePopular.setText(e.nomePopular ?: "")
        binding.checkEndemismo.isChecked = e.endemismo

        if (!e.foto.isNullOrBlank()) {
            binding.tvNomeFoto.text = "foto_atual.jpg"
            binding.ivPreviewFoto.visibility = View.VISIBLE
            ImagemLoader.load(lifecycleScope, binding.ivPreviewFoto, e.foto, R.drawable.ic_placeholder_beetle)
        }

        val idx = STATUS_CONSERVACAO.indexOfFirst {
            it.equals(e.statusConservacao, ignoreCase = true)
        }
        if (idx >= 0) binding.spinnerStatus.setSelection(idx)
    }

    private fun criarEspecie() {
        val req = coletarFormulario() ?: return
        val especieReq = EspecieRequest(
            classe = req.classe, ordem = req.ordem, familia = req.familia,
            genero = req.genero, especie = req.especie, endemismo = req.endemismo,
            foto = req.foto, nomePopular = req.nomePopular,
            statusConservacao = req.statusConservacao
        )
        if (estudoRemoteId <= 0) {
            salvarEspecieOffline(especieReq)
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.postEspecie(
                    SessionManager.getAuthHeader(), estudoRemoteId, especieReq
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@CadastroEspecieActivity, "Espécie cadastrada!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CadastroEspecieActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                salvarEspecieOffline(especieReq)
            } finally { setLoading(false) }
        }
    }

    private fun salvarEspecieOffline(req: EspecieRequest) {
        val repo = OfflineRepository(this)
        val resolved = when {
            estudoLocalId > 0 -> estudoLocalId
            estudoRemoteId > 0 -> repo.estudoLocalIdFromRemote(estudoRemoteId)
            else -> null
        }
        if (resolved == null) {
            Toast.makeText(this, "Estudo não está salvo offline.", Toast.LENGTH_LONG).show()
            return
        }
        val estudoLocalId = resolved
        try {
            repo.criarEspecieOffline(estudoLocalId, req)
            Toast.makeText(
                this,
                "Sem conexão — espécie salva offline.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Erro ao salvar offline: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun editarEspecie() {
        val req = coletarFormulario() ?: return
        val patchReq = EspeciePatchRequest(
            classe = req.classe, ordem = req.ordem, familia = req.familia,
            genero = req.genero, especie = req.especie, endemismo = req.endemismo,
            foto = req.foto, nomePopular = req.nomePopular,
            statusConservacao = req.statusConservacao
        )
        if (estudoRemoteId <= 0 || especieId <= 0) {
            salvarEdicaoOffline(patchReq, "Espécie atualizada offline.")
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.patchEspecie(
                    SessionManager.getAuthHeader(), estudoRemoteId, especieId, patchReq
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@CadastroEspecieActivity, "Espécie atualizada!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CadastroEspecieActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                salvarEdicaoOffline(patchReq, "Sem conexão — alterações salvas offline.")
            } finally { setLoading(false) }
        }
    }


    private fun salvarEdicaoOffline(req: EspeciePatchRequest, msg: String) {
        val repo = OfflineRepository(this)
        val localId = if (especieId < 0) (-especieId).toLong()
            else {
                val resolvedEstudoLocal = if (estudoLocalId > 0) estudoLocalId
                    else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
                    else null
                if (resolvedEstudoLocal == null || especieId <= 0) null
                else repo.especieLocalIdFromRemote(resolvedEstudoLocal, especieId)
            }
        if (localId == null) {
            Toast.makeText(this, "Espécie não encontrada offline.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            repo.editarEspecieOffline(localId, req)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao salvar offline: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private data class FormData(
        val classe: String, val ordem: String, val familia: String,
        val genero: String, val especie: String, val endemismo: Boolean,
        val foto: String?, val nomePopular: String?, val statusConservacao: String?
    )

    private fun coletarFormulario(): FormData? {
        val classe = binding.etClasse.text.toString().trim()
        val ordem  = binding.etOrdem.text.toString().trim()
        val familia = binding.etFamilia.text.toString().trim()
        val genero = binding.etGenero.text.toString().trim()
        val especie = binding.etEspecie.text.toString().trim()

        if (classe.isEmpty() || ordem.isEmpty() || familia.isEmpty() ||
            genero.isEmpty() || especie.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return null
        }

        return FormData(
            classe = classe, ordem = ordem, familia = familia,
            genero = genero, especie = especie,
            endemismo = binding.checkEndemismo.isChecked,
            foto = fotoBase64,
            nomePopular = binding.etNomePopular.text.toString().trim().ifEmpty { null },
            statusConservacao = binding.spinnerStatus.selectedItem?.toString()
        )
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }
}

class EspecieDetalheActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityEspecieDetalheBinding
    private var estudoRemoteId = -1
    private var estudoLocalId  = -1L
    private var especieId      = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEspecieDetalheBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoLocalId  = intent.getLongExtra("estudo_local_id", -1L)
        especieId      = intent.getIntExtra("especie_id", -1)

        carregarEspecie()

        binding.ivEditar.setOnClickListener {
            val i = Intent(this, CadastroEspecieActivity::class.java)
            i.putExtra("estudo_remote_id", estudoRemoteId)
            i.putExtra("estudo_local_id", estudoLocalId)
            i.putExtra("especie_id", especieId)
            startActivity(i)
        }

        binding.ivDeletar.setOnClickListener { confirmarDelete() }
        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        carregarEspecie()
    }

    private fun carregarEspecie() {
        if (especieId <= 0 || estudoRemoteId <= 0) {
            carregarEspecieOffline()
            return
        }
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEspecie(
                    SessionManager.getAuthHeader(), estudoRemoteId, especieId
                )
                val e = resp.body()
                if (e != null) preencher(e) else carregarEspecieOffline()
            } catch (_: Exception) { carregarEspecieOffline() }
        }
    }

    private fun carregarEspecieOffline() {
        val repo = com.kheprix.db.OfflineRepository(this)
        val resolvedEstudoLocal = if (estudoLocalId > 0) estudoLocalId
            else if (estudoRemoteId > 0) repo.estudoLocalIdFromRemote(estudoRemoteId)
            else null
        if (resolvedEstudoLocal == null) return
        val match = repo.listarEspeciesPorEstudoLocal(resolvedEstudoLocal)
            .firstOrNull { it.id == especieId }
            ?: return
        preencher(match)
    }

    private fun preencher(e: EspecieResponse) {
        binding.tvTitulo.text = "${e.genero} ${e.especie}"
        binding.tvNomeCientifico.text = "${e.genero} ${e.especie}"
        binding.tvNomePopular.text = e.nomePopular ?: "—"
        binding.tvClasse.text = e.classe
        binding.tvOrdem.text  = e.ordem
        binding.tvFamilia.text = e.familia
        binding.tvStatus.text = e.statusConservacao ?: "—"
        binding.tvEndemismo.text = if (e.endemismo) "A espécie é nativa da região do estudo" else ""
        binding.tvEndemismo.visibility = if (e.endemismo) View.VISIBLE else View.GONE

        ImagemLoader.load(
            scope = lifecycleScope,
            target = binding.ivFoto,
            url = e.foto,
            placeholder = R.drawable.ic_placeholder_beetle
        )
    }

    private fun confirmarDelete() {
        AlertDialog.Builder(this)
            .setTitle("Deletar espécie")
            .setMessage("Tem certeza?")
            .setPositiveButton("Deletar") { _, _ ->
                if (especieId < 0 || estudoRemoteId <= 0) {
                    val localId = if (especieId < 0) (-especieId).toLong() else null
                    if (localId != null) {
                        com.kheprix.db.DatabaseHelper(this).writableDatabase
                            .delete("especies", "local_id = ?", arrayOf(localId.toString()))
                    }
                    finish()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        RetrofitClient.apiService.deleteEspecie(
                            SessionManager.getAuthHeader(), estudoRemoteId, especieId
                        )
                        finish()
                    } catch (_: Exception) { }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }
}
