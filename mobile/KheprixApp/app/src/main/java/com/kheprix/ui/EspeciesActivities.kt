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
import kotlinx.coroutines.launch
import java.io.File

// ════════════════════════════════════════════════════════════════════════════
// LISTA DE ESPÉCIES
// ════════════════════════════════════════════════════════════════════════════

/**
 * Lista de espécies de um estudo.
 * Extras: estudo_remote_id, estudo_nome
 */
class EspeciesActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityEspeciesBinding
    private var estudoRemoteId = -1
    private var estudoNome = ""
    private val especies = mutableListOf<EspecieResponse>()
    private val especiesExibidas = mutableListOf<EspecieResponse>()
    private var filtro: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEspeciesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        estudoNome     = intent.getStringExtra("estudo_nome") ?: ""

        binding.tvEstudoNome.text = estudoNome

        binding.rvEspecies.layoutManager = LinearLayoutManager(this)
        binding.rvEspecies.adapter = EspecieAdapter(especiesExibidas, lifecycleScope,
            onItemClick = { especie ->
                val i = Intent(this, EspecieDetalheActivity::class.java)
                i.putExtra("estudo_remote_id", estudoRemoteId)
                i.putExtra("especie_id", especie.id)
                i.putExtra("estudo_nome", estudoNome)
                startActivity(i)
            }
        )

        binding.btnAdicionarEspecie.setOnClickListener {
            val i = Intent(this, CadastroEspecieActivity::class.java)
            i.putExtra("estudo_remote_id", estudoRemoteId)
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
            try {
                val resp = RetrofitClient.apiService.getEspecies(
                    SessionManager.getAuthHeader(), estudoRemoteId
                )
                if (resp.isSuccessful) {
                    especiesOnline.addAll(resp.body() ?: emptyList())
                }
            } catch (_: Exception) { }

            val repo = OfflineRepository(this@EspeciesActivity)
            // Resolve estudo localId, cacheando da API se necessário.
            var estudoLocalId = repo.estudoLocalIdFromRemote(estudoRemoteId)
            if (estudoLocalId == null && especiesOnline.isNotEmpty()) {
                try {
                    val estudo = RetrofitClient.apiService.getEstudos(SessionManager.getAuthHeader())
                        .body()?.firstOrNull { it.id == estudoRemoteId }
                    if (estudo != null) estudoLocalId = repo.cacheEstudo(estudo)
                } catch (_: Exception) { }
            }

            // Espelha espécies online no SQLite para acesso offline posterior.
            estudoLocalId?.let { id ->
                especiesOnline.forEach { e ->
                    try { repo.cacheEspecie(id, e) } catch (_: Exception) { }
                }
            }

            especies.clear()
            especies.addAll(especiesOnline)

            // Mescla com espécies offline (criadas localmente, ainda não sincronizadas).
            if (estudoLocalId != null) {
                val remoteIds = especiesOnline.map { it.id }.toSet()
                val offline = repo.listarEspeciesPorEstudoLocal(estudoLocalId!!)
                offline.forEach { off ->
                    // id < 0 ⇒ offline-only (codificado como -localId); senão
                    // é remoteId e só adiciona se não veio da API.
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

// Adapter da lista
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
        // Nome científico = genero + especie
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

// ════════════════════════════════════════════════════════════════════════════
// CADASTRO / EDIÇÃO DE ESPÉCIE
// ════════════════════════════════════════════════════════════════════════════

/**
 * Cadastro e edição de espécie.
 *
 * Foto: o usuário pode tirar foto (câmera) ou selecionar da galeria.
 * A foto é convertida para Base64 antes do envio.
 *
 * Extras recebidos:
 *   estudo_remote_id → Int  (obrigatório)
 *   especie_id       → Int  (se edição; -1 para criação)
 */
class CadastroEspecieActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityCadastroEspecieBinding

    private var estudoRemoteId = -1
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

    // ── Foto ──────────────────────────────────────────────────────────────

    private fun setupFotoListeners() {
        // Ícone pasta → galeria
        binding.ivSelecionarGaleria.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQ_GALERIA)
        }

        // Ícone câmera → câmera
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

        uri?.let {
            fotoBase64 = PhotoUtils.uriToBase64(this, it)
            binding.tvNomeFoto.text = it.lastPathSegment ?: "foto.jpg"
            // Preview: decode e exibe miniatura no campo de foto
            val bmp = PhotoUtils.base64ToBitmap(fotoBase64 ?: "")
            // Preview disponível: adicione um ImageView com id ivPreviewFoto ao layout para exibir miniaturaal: adicionar ImageView de preview
        }
    }

    // ── Spinner Status Conservação ────────────────────────────────────────

    private fun setupSpinnerStatus() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, STATUS_CONSERVACAO)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = adapter
    }

    // ── Preencher campos no modo edição ───────────────────────────────────

    private fun carregarEspecieParaEdicao() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEspecie(
                    SessionManager.getAuthHeader(), estudoRemoteId, especieId
                )
                resp.body()?.let { e ->
                    binding.etClasse.setText(e.classe)
                    binding.etOrdem.setText(e.ordem)
                    binding.etFamilia.setText(e.familia)
                    binding.etGenero.setText(e.genero)
                    binding.etEspecie.setText(e.especie)
                    binding.etNomePopular.setText(e.nomePopular ?: "")
                    binding.checkEndemismo.isChecked = e.endemismo
                    // fotoBase64 permanece null: se o usuário não escolher nova
                    // foto, o patch omite o campo e o backend preserva a atual.

                    // Selecionar status no spinner
                    val idx = STATUS_CONSERVACAO.indexOfFirst {
                        it.equals(e.statusConservacao, ignoreCase = true)
                    }
                    if (idx >= 0) binding.spinnerStatus.setSelection(idx)
                }
            } catch (_: Exception) { }
        }
    }

    // ── API calls ─────────────────────────────────────────────────────────

    private fun criarEspecie() {
        val req = coletarFormulario() ?: return
        val especieReq = EspecieRequest(
            classe = req.classe, ordem = req.ordem, familia = req.familia,
            genero = req.genero, especie = req.especie, endemismo = req.endemismo,
            foto = req.foto, nomePopular = req.nomePopular,
            statusConservacao = req.statusConservacao
        )
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
        val estudoLocalId = repo.estudoLocalIdFromRemote(estudoRemoteId)
        if (estudoLocalId == null) {
            Toast.makeText(
                this,
                "Sem conexão — este estudo não está salvo offline.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
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
        setLoading(true)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.patchEspecie(
                    SessionManager.getAuthHeader(), estudoRemoteId, especieId,
                    EspeciePatchRequest(
                        classe = req.classe, ordem = req.ordem, familia = req.familia,
                        genero = req.genero, especie = req.especie, endemismo = req.endemismo,
                        foto = req.foto, nomePopular = req.nomePopular,
                        statusConservacao = req.statusConservacao
                    )
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@CadastroEspecieActivity, "Espécie atualizada!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CadastroEspecieActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@CadastroEspecieActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            } finally { setLoading(false) }
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

// ════════════════════════════════════════════════════════════════════════════
// DETALHE DE ESPÉCIE
// ════════════════════════════════════════════════════════════════════════════

/**
 * Exibe os detalhes de uma espécie com opção de editar (ícone caneta) e deletar (lixeira).
 *
 * Extras: estudo_remote_id, especie_id, estudo_nome
 */
class EspecieDetalheActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityEspecieDetalheBinding
    private var estudoRemoteId = -1
    private var especieId      = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEspecieDetalheBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        especieId      = intent.getIntExtra("especie_id", -1)

        carregarEspecie()

        binding.ivEditar.setOnClickListener {
            val i = Intent(this, CadastroEspecieActivity::class.java)
            i.putExtra("estudo_remote_id", estudoRemoteId)
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
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEspecie(
                    SessionManager.getAuthHeader(), estudoRemoteId, especieId
                )
                resp.body()?.let { e ->
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
            } catch (_: Exception) { }
        }
    }

    private fun confirmarDelete() {
        AlertDialog.Builder(this)
            .setTitle("Deletar espécie")
            .setMessage("Tem certeza?")
            .setPositiveButton("Deletar") { _, _ ->
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
