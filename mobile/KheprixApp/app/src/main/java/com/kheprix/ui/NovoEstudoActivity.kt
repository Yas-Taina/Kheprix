package com.kheprix.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kheprix.R
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityNovoEstudoBinding
import com.kheprix.db.OfflineRepository
import com.kheprix.models.EstudoRequest
import com.kheprix.models.VariavelRequest
import kotlinx.coroutines.launch

/**
 * Tela de criação de estudo (também usada para edição de nome/observações).
 *
 * Regras:
 *  - Variáveis podem ser adicionadas dinamicamente clicando em ⊕
 *  - Variáveis podem ser removidas clicando no ✕ vermelho
 *  - Variáveis NÃO podem ser editadas após criação (somente excluídas + recriadas)
 *  - Em modo edição (EXTRA_ESTUDO_REMOTE_ID presente), a seção de variáveis fica oculta
 *
 * Extras recebidos (modo edição):
 *   EXTRA_ESTUDO_REMOTE_ID → Int   (id no servidor)
 *   EXTRA_ESTUDO_NOME      → String
 *   EXTRA_ESTUDO_OBS       → String
 */
class NovoEstudoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNovoEstudoBinding

    /** Container onde as linhas de variável são adicionadas dinamicamente */
    private val variavelViews = mutableListOf<View>()

    private var modoEdicao = false
    private var estudoRemoteId: Int = -1

    companion object {
        const val EXTRA_ESTUDO_REMOTE_ID = "estudo_remote_id"
        const val EXTRA_ESTUDO_NOME      = "estudo_nome"
        const val EXTRA_ESTUDO_OBS       = "estudo_obs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovoEstudoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Detectar modo edição
        estudoRemoteId = intent.getIntExtra(EXTRA_ESTUDO_REMOTE_ID, -1)
        modoEdicao = estudoRemoteId != -1

        if (modoEdicao) {
            binding.tvTitulo.text = "Editar Estudo"
            binding.etNomeEstudo.setText(intent.getStringExtra(EXTRA_ESTUDO_NOME) ?: "")
            binding.etObservacoes.setText(intent.getStringExtra(EXTRA_ESTUDO_OBS) ?: "")
            // Variáveis não editáveis: ocultar seção inteira
            binding.layoutVariaveis.visibility = View.GONE
            binding.tvVariaveisTitle.visibility = View.GONE
            binding.btnAdicionarVariavel.visibility = View.GONE
        } else {
            // Adicionar primeira linha de variável automaticamente
            adicionarLinhaVariavel()
        }

        binding.btnAdicionarVariavel.setOnClickListener { adicionarLinhaVariavel() }

        binding.btnConfirmar.setOnClickListener {
            if (modoEdicao) editarEstudo() else criarEstudo()
        }

        binding.ivBack.setOnClickListener { finish() }
    }

    // ── Variáveis dinâmicas ───────────────────────────────────────────────

    private fun adicionarLinhaVariavel() {
        val inflater = LayoutInflater.from(this)
        val linha = inflater.inflate(R.layout.item_variavel_form, binding.layoutVariaveis, false)

        // Spinner "Nível de Estudo"
        val spinnerNivel = linha.findViewById<Spinner>(R.id.spinnerNivelAplicacao)
        ArrayAdapter.createFromResource(
            this,
            R.array.niveis_aplicacao,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerNivel.adapter = adapter
        }

        // Spinner "Tipo de Dado"
        val spinnerTipo = linha.findViewById<Spinner>(R.id.spinnerTipoDado)
        ArrayAdapter.createFromResource(
            this,
            R.array.tipos_dado,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTipo.adapter = adapter
        }

        // Botão ✕ remover linha
        linha.findViewById<ImageView>(R.id.ivRemoverVariavel).setOnClickListener {
            binding.layoutVariaveis.removeView(linha)
            variavelViews.remove(linha)
        }

        binding.layoutVariaveis.addView(linha)
        variavelViews.add(linha)
    }

    private fun coletarVariaveis(): List<VariavelRequest>? {
        val lista = mutableListOf<VariavelRequest>()
        for (view in variavelViews) {
            val nome  = view.findViewById<EditText>(R.id.etNomeVariavel).text.toString().trim()
            val nivel = view.findViewById<Spinner>(R.id.spinnerNivelAplicacao).selectedItem?.toString() ?: ""
            val tipo  = view.findViewById<Spinner>(R.id.spinnerTipoDado).selectedItem?.toString() ?: ""
            val metrica = view.findViewById<EditText>(R.id.etMetrica).text.toString().trim()

            if (nome.isEmpty()) {
                Toast.makeText(this, "Preencha o nome de todas as variáveis", Toast.LENGTH_SHORT).show()
                return null
            }
            lista.add(VariavelRequest(
                nome = nome,
                nivelAplicacao = nivelParaApi(nivel),
                tipoDado = tipoParaApi(tipo),
                metrica = metrica.ifEmpty { null }
            ))
        }
        return lista
    }

    // ── API calls ─────────────────────────────────────────────────────────

    private fun criarEstudo() {
        val nome = binding.etNomeEstudo.text.toString().trim()
        val obs  = binding.etObservacoes.text.toString().trim()

        if (nome.isEmpty()) {
            binding.etNomeEstudo.error = "Informe o nome do estudo"
            return
        }
        val variaveis = coletarVariaveis() ?: return

        val req = EstudoRequest(
            nome = nome,
            observacoes = obs.ifEmpty { null },
            variaveis = variaveis
        )

        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.postEstudo(
                    SessionManager.getAuthHeader(), req
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@NovoEstudoActivity, "Estudo criado!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@NovoEstudoActivity, "Erro: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                salvarEstudoOffline(req)
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Persiste o estudo no SQLite com sincronizado=0. Será enviado quando a
     * conexão voltar (via sincronizarDadosEstudo).
     */
    private fun salvarEstudoOffline(req: EstudoRequest) {
        try {
            OfflineRepository(this).criarEstudoOffline(req)
            Toast.makeText(
                this,
                "Sem conexão — estudo salvo offline. Sincronize quando estiver online.",
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

    private fun editarEstudo() {
        // A API não possui PATCH /estudos/:id no spec atual.
        // Quando disponível, chame aqui o endpoint de atualização.
        Toast.makeText(this, "Funcionalidade em implementação", Toast.LENGTH_SHORT).show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Converte label PT-BR → valor aceito pela API */
    private fun nivelParaApi(label: String) = when (label) {
        "Campanha" -> "campanha"
        "Unidade"  -> "unidade"
        "Evento"   -> "evento"
        "Registro" -> "registro"
        else       -> label.lowercase()
    }

    private fun tipoParaApi(label: String) = when (label) {
        "Numérico" -> "number"
        "Texto"    -> "string"
        "Booleano" -> "boolean"
        "Data"     -> "date"
        else       -> label.lowercase()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnConfirmar.isEnabled = !loading
    }
}
