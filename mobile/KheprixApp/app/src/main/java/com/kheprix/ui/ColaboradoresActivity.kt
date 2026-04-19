package com.kheprix.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityColaboradoresBinding
import com.kheprix.models.ColaboradorPatchRequest
import com.kheprix.models.ColaboradorResponse
import com.kheprix.models.ConviteRequest
import com.kheprix.models.ConviteListResponse
import com.kheprix.models.CodigoAcessoPatchRequest
import kotlinx.coroutines.launch

/**
 * Tela de Colaboradores do Estudo (somente proprietário).
 *
 * Seções:
 *  1. "Convidar Colaboradores" — campo e-mail + botão Confirmar
 *     POST /estudos/:id/convites
 *
 *  2. Lista de colaboradores — nome, spinner perfil, "Alterar Acesso", lixeira
 *     GET  /estudos/:id/colaboradores
 *     PATCH /estudos/:id/colaboradores/:id
 *     DELETE /estudos/:id/colaboradores/:id
 *
 *  3. "Configurar Autocadastro de Colaborador"
 *     - QR Code gerado localmente com o código do estudo
 *     - Exibe código + campo senha de autocadastro editável
 *     GET  /estudos/:id/codigo_acesso
 *     PATCH /estudos/:id/codigo_acesso  { senha_autocadastro }
 *
 * Extras recebidos:
 *   estudo_remote_id → Int
 *   estudo_nome      → String
 *
 * Dependência para QR Code (build.gradle):
 *   implementation 'com.google.zxing:core:3.5.1'
 *   (ZXing Android Embedded já inclui o core)
 */
class ColaboradoresActivity : AppCompatActivity() {

    private lateinit var binding: ActivityColaboradoresBinding
    private var estudoRemoteId = -1
    private val colaboradores = mutableListOf<ColaboradorResponse>()
    private lateinit var adapter: ColaboradorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityColaboradoresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        estudoRemoteId = intent.getIntExtra("estudo_remote_id", -1)
        binding.tvEstudoNome.text = intent.getStringExtra("estudo_nome") ?: ""

        setupAdapter()
        setupConviteSection()
        setupAutocadastroSection()
        carregarColaboradores()
        carregarCodigoAcesso()
        carregarConvitesPendentes()

        binding.ivMenuLateral.setOnClickListener { onBackPressed() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    // ── Adapter ──────────────────────────────────────────────────────────

    private fun setupAdapter() {
        adapter = ColaboradorAdapter(colaboradores,
            onAlterarAcesso = { colab, novoPerfil -> alterarAcesso(colab, novoPerfil) },
            onDeletar       = { colab -> confirmarDeletar(colab) }
        )
        binding.rvColaboradores.layoutManager = LinearLayoutManager(this)
        binding.rvColaboradores.adapter = adapter
    }

    // ── Seção: Convidar ───────────────────────────────────────────────────

    private fun setupConviteSection() {
        binding.btnAdicionarColaboradores.setOnClickListener {
            val email = binding.etEmailConvite.text.toString().trim()
            if (email.isEmpty()) {
                binding.etEmailConvite.error = "Informe o e-mail"
                return@setOnClickListener
            }
            enviarConvite(email)
        }
    }

    private fun enviarConvite(email: String) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.postConvite(
                    SessionManager.getAuthHeader(), estudoRemoteId,
                    ConviteRequest(emailConvidado = email)
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@ColaboradoresActivity, "Convite enviado para $email", Toast.LENGTH_SHORT).show()
                    binding.etEmailConvite.text?.clear()
                } else {
                    Toast.makeText(this@ColaboradoresActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@ColaboradoresActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Seção: Convites enviados (pendentes) ─────────────────────────

    private val convitesPendentes = mutableListOf<ConviteListResponse>()

    private fun carregarConvitesPendentes() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getConvites(
                    SessionManager.getAuthHeader(), estudoRemoteId
                )
                if (resp.isSuccessful) {
                    convitesPendentes.clear()
                    convitesPendentes.addAll(
                        (resp.body() ?: emptyList())
                            .filter { it.status.equals("pendente", ignoreCase = true) }
                    )
                    atualizarListaConvitesPendentes()
                }
            } catch (_: Exception) { /* silencioso */ }
        }
    }

    private fun atualizarListaConvitesPendentes() {
        val container = binding.layoutConvitesPendentes
        container.removeAllViews()
        if (convitesPendentes.isEmpty()) {
            binding.tvSemConvitesPendentes.visibility = android.view.View.VISIBLE
            return
        }
        binding.tvSemConvitesPendentes.visibility = android.view.View.GONE
        convitesPendentes.forEach { convite ->
            val item = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 12, 0, 12)
            }
            val tvEmail = android.widget.TextView(this).apply {
                text = convite.emailConvidado
                textSize = 14f
                setTextColor(0xFF4A5240.toInt())
                typeface = android.graphics.Typeface.MONOSPACE
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btnCancelar = android.widget.Button(this).apply {
                text = "Cancelar"
                textSize = 12f
                setTextColor(0xFFFFFFFF.toInt())
                backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFB88A8A.toInt())
                setOnClickListener { cancelarConvite(convite.id) }
            }
            item.addView(tvEmail)
            item.addView(btnCancelar)
            container.addView(item)

            val divider = android.view.View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(0xFFC8D4BE.toInt())
            }
            container.addView(divider)
        }
    }

    private fun cancelarConvite(conviteId: Int) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.deleteConvite(
                    SessionManager.getAuthHeader(), estudoRemoteId, conviteId
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@ColaboradoresActivity, "Convite cancelado", Toast.LENGTH_SHORT).show()
                    carregarConvitesPendentes()
                }
            } catch (_: Exception) {
                Toast.makeText(this@ColaboradoresActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Seção: Autocadastro ───────────────────────────────────────────────

    private fun setupAutocadastroSection() {
        binding.btnAlterarSenha.setOnClickListener {
            val novaSenha = binding.etSenhaAutocadastro.text.toString().trim()
            if (novaSenha.isEmpty()) {
                binding.etSenhaAutocadastro.error = "Informe a nova senha"
                return@setOnClickListener
            }
            alterarSenhaAutocadastro(novaSenha)
        }
    }

    private fun carregarCodigoAcesso() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getCodigoAcesso(
                    SessionManager.getAuthHeader(), estudoRemoteId
                )
                if (resp.isSuccessful) {
                    val body = resp.body() ?: return@launch
                    binding.tvCodigoEstudo.text = body.codigo
                    binding.etSenhaAutocadastro.setText(body.senhaAutocadastro)
                    gerarQrCode(body.codigo)
                }
            } catch (_: Exception) { /* silencioso */ }
        }
    }

    private fun alterarSenhaAutocadastro(novaSenha: String) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.patchCodigoAcesso(
                    SessionManager.getAuthHeader(), estudoRemoteId,
                    CodigoAcessoPatchRequest(senhaAutocadastro = novaSenha)
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@ColaboradoresActivity, "Senha atualizada!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ColaboradoresActivity, "Erro ao atualizar", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@ColaboradoresActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Gera um Bitmap de QR Code com o código do estudo usando ZXing core.
     *
     * ATIVAR após adicionar:
     *   implementation 'com.google.zxing:core:3.5.1'
     */
    private fun gerarQrCode(conteudo: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(conteudo, BarcodeFormat.QR_CODE, 512, 512)
            val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            binding.ivQrCode.setImageBitmap(bmp)
        } catch (e: Exception) {
            // ZXing não disponível: exibe placeholder
            binding.ivQrCode.setImageResource(R.drawable.ic_placeholder_beetle)
        }
    }

    // ── Carregar colaboradores ────────────────────────────────────────────

    private fun carregarColaboradores() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getColaboradores(
                    SessionManager.getAuthHeader(), estudoRemoteId
                )
                if (resp.isSuccessful) {
                    colaboradores.clear()
                    // Exclui o próprio proprietário da lista
                    val meuId = SessionManager.getUserId()
                    colaboradores.addAll(
                        (resp.body() ?: emptyList()).filter { it.idUsuario != meuId }
                    )
                    adapter.notifyDataSetChanged()
                }
            } catch (_: Exception) { }
        }
    }

    // ── Ações sobre colaboradores ─────────────────────────────────────────

    private fun alterarAcesso(colab: ColaboradorResponse, novoPerfil: String) {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.patchColaborador(
                    SessionManager.getAuthHeader(), estudoRemoteId, colab.idUsuario,
                    ColaboradorPatchRequest(perfil = perfilParaApi(novoPerfil))
                )
                if (resp.isSuccessful) {
                    Toast.makeText(this@ColaboradoresActivity, "Acesso atualizado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ColaboradoresActivity, "Erro: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@ColaboradoresActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmarDeletar(colab: ColaboradorResponse) {
        AlertDialog.Builder(this)
            .setTitle("Remover colaborador")
            .setMessage("Remover ${colab.nome} do estudo?")
            .setPositiveButton("Remover") { _, _ ->
                lifecycleScope.launch {
                    try {
                        RetrofitClient.apiService.deleteColaborador(
                            SessionManager.getAuthHeader(), estudoRemoteId, colab.idUsuario
                        )
                        carregarColaboradores()
                    } catch (_: Exception) { }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    /** Converte label PT-BR → valor da API */
    private fun perfilParaApi(label: String) = when (label) {
        "Proprietário" -> "proprietario"
        "Colaborador"  -> "colaborador"
        else           -> label.lowercase()
    }
}

// ── Adapter ──────────────────────────────────────────────────────────────────

class ColaboradorAdapter(
    private val items: List<ColaboradorResponse>,
    private val onAlterarAcesso: (ColaboradorResponse, String) -> Unit,
    private val onDeletar: (ColaboradorResponse) -> Unit
) : RecyclerView.Adapter<ColaboradorAdapter.VH>() {

    private val perfis = listOf("Colaborador", "Proprietário")

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView       = view.findViewById(R.id.tvColabNome)
        val spinner: Spinner       = view.findViewById(R.id.spinnerPerfil)
        val btnAlterar: Button     = view.findViewById(R.id.btnAlterarAcesso)
        val ivDeletar: ImageView   = view.findViewById(R.id.ivDeletarColab)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_colaborador, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNome.text = item.nome

        val spinnerAdapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_item,
            perfis
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        holder.spinner.adapter = spinnerAdapter

        // Pré-selecionar perfil atual
        val perfilLabel = when (item.perfil) {
            "proprietario" -> "Proprietário"
            else           -> "Colaborador"
        }
        holder.spinner.setSelection(perfis.indexOf(perfilLabel).coerceAtLeast(0))

        holder.btnAlterar.setOnClickListener {
            onAlterarAcesso(item, holder.spinner.selectedItem.toString())
        }
        holder.ivDeletar.setOnClickListener { onDeletar(item) }
    }

    override fun getItemCount() = items.size
}
