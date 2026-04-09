package com.kheprix.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.databinding.ActivityRegistroRapidoBinding
import com.kheprix.models.CampanhaResponse
import com.kheprix.models.EstudoResponse
import com.kheprix.models.EventoResponse
import com.kheprix.models.UnidadeResponse
import kotlinx.coroutines.launch

/**
 * Cadastro Rápido de Registro de Ocorrência.
 *
 * Etapa 1 (esta activity): seleção do contexto via 4 spinners em cascata:
 *   Estudo → Campanha de Coleta → Unidade Amostral → Evento de Amostragem
 *
 * Ao clicar "Prosseguir", navega para RegistroRapidoFormActivity
 * passando os IDs selecionados para preenchimento do registro em si.
 *
 * Os spinners são dependentes: ao selecionar um Estudo, carrega as Campanhas;
 * ao selecionar a Campanha, carrega as Unidades; e assim por diante.
 */
class RegistroRapidoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroRapidoBinding

    private val estudos     = mutableListOf<EstudoResponse>()
    private val campanhas   = mutableListOf<CampanhaResponse>()
    private val unidades    = mutableListOf<UnidadeResponse>()
    private val eventos     = mutableListOf<EventoResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroRapidoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()

        binding.btnProsseguir.setOnClickListener { prosseguir() }
        binding.ivMenuLateral.setOnClickListener { onBackPressed() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarEstudos()
    }

    // ── Spinners em cascata ───────────────────────────────────────────────

    private fun setupSpinners() {
        // Spinner Estudo
        binding.spinnerEstudo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos == 0) return
                val estudo = estudos.getOrNull(pos - 1) ?: return
                carregarCampanhas(estudo.id)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Spinner Campanha
        binding.spinnerCampanha.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos == 0) return
                val campanha = campanhas.getOrNull(pos - 1) ?: return
                val estudoId = estudoSelecionado()?.id ?: return
                carregarUnidades(estudoId, campanha.id)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Spinner Unidade
        binding.spinnerUnidade.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos == 0) return
                val unidade = unidades.getOrNull(pos - 1) ?: return
                val estudoId = estudoSelecionado()?.id ?: return
                val campanhaId = campanhaSelecionada()?.id ?: return
                carregarEventos(estudoId, campanhaId, unidade.id)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    // ── Carregamento em cascata ───────────────────────────────────────────

    private fun carregarEstudos() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEstudos(SessionManager.getAuthHeader())
                if (resp.isSuccessful) {
                    estudos.clear()
                    estudos.addAll(resp.body() ?: emptyList())
                    popularSpinner(binding.spinnerEstudo, estudos.map { it.nome })
                }
            } catch (_: Exception) {
                Toast.makeText(this@RegistroRapidoActivity, "Sem conexão", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun carregarCampanhas(estudoId: Int) {
        campanhas.clear()
        popularSpinner(binding.spinnerCampanha, emptyList())
        unidades.clear()
        popularSpinner(binding.spinnerUnidade, emptyList())
        eventos.clear()
        popularSpinner(binding.spinnerEvento, emptyList())

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getCampanhas(SessionManager.getAuthHeader(), estudoId)
                if (resp.isSuccessful) {
                    campanhas.addAll(resp.body() ?: emptyList())
                    popularSpinner(binding.spinnerCampanha, campanhas.map { it.nome })
                }
            } catch (_: Exception) {}
        }
    }

    private fun carregarUnidades(estudoId: Int, campanhaId: Int) {
        unidades.clear()
        popularSpinner(binding.spinnerUnidade, emptyList())
        eventos.clear()
        popularSpinner(binding.spinnerEvento, emptyList())

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getUnidades(SessionManager.getAuthHeader(), estudoId, campanhaId)
                if (resp.isSuccessful) {
                    unidades.addAll(resp.body() ?: emptyList())
                    popularSpinner(binding.spinnerUnidade, unidades.map { it.nome })
                }
            } catch (_: Exception) {}
        }
    }

    private fun carregarEventos(estudoId: Int, campanhaId: Int, unidadeId: Int) {
        eventos.clear()
        popularSpinner(binding.spinnerEvento, emptyList())

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEventos(SessionManager.getAuthHeader(), estudoId, campanhaId, unidadeId)
                if (resp.isSuccessful) {
                    eventos.addAll(resp.body() ?: emptyList())
                    popularSpinner(binding.spinnerEvento, eventos.map { it.horarioInicio })
                }
            } catch (_: Exception) {}
        }
    }

    private fun popularSpinner(spinner: Spinner, itens: List<String>) {
        val lista = mutableListOf("Selecione...") + itens
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lista)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    // ── Navegar para formulário de registro ───────────────────────────────

    private fun prosseguir() {
        val estudo   = estudoSelecionado()
        val campanha = campanhaSelecionada()
        val unidade  = unidadeSelecionada()
        val evento   = eventoSelecionado()

        if (estudo == null || campanha == null || unidade == null || evento == null) {
            Toast.makeText(this, "Selecione todos os níveis", Toast.LENGTH_SHORT).show()
            return
        }

        // Navega diretamente para NovoRegistroActivity com os IDs selecionados
        val intent = Intent(this, NovoRegistroActivity::class.java).apply {
            putExtra("estudo_remote_id", estudo.id)
            putExtra("campanha_id",      campanha.id)
            putExtra("unidade_id",       unidade.id)
            putExtra("evento_id",        evento.id)
        }
        startActivity(intent)
    }

    // ── Helpers de seleção ────────────────────────────────────────────────

    private fun estudoSelecionado(): EstudoResponse? {
        val pos = binding.spinnerEstudo.selectedItemPosition
        return if (pos > 0) estudos.getOrNull(pos - 1) else null
    }

    private fun campanhaSelecionada(): CampanhaResponse? {
        val pos = binding.spinnerCampanha.selectedItemPosition
        return if (pos > 0) campanhas.getOrNull(pos - 1) else null
    }

    private fun unidadeSelecionada(): UnidadeResponse? {
        val pos = binding.spinnerUnidade.selectedItemPosition
        return if (pos > 0) unidades.getOrNull(pos - 1) else null
    }

    private fun eventoSelecionado(): EventoResponse? {
        val pos = binding.spinnerEvento.selectedItemPosition
        return if (pos > 0) eventos.getOrNull(pos - 1) else null
    }
}
