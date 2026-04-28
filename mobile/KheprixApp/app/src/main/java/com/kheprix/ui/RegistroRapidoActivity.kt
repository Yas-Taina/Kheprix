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
import com.kheprix.db.CampanhaDao
import com.kheprix.db.EstudoDao
import com.kheprix.db.EventoDao
import com.kheprix.db.UnidadeDao
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
                // Tenta carregar da API
                val resp = RetrofitClient.apiService.getEstudos(SessionManager.getAuthHeader())
                if (resp.isSuccessful) {
                    estudos.clear()
                    estudos.addAll(resp.body() ?: emptyList())
                }
            } catch (_: Exception) { }

            // Merge com SQLite: adiciona offline-only
            val offlineDao = EstudoDao(this@RegistroRapidoActivity)
            val offlineEstudos = offlineDao.listarTodos()
            val remoteIds = estudos.mapNotNull { it.id }.toSet()

            offlineEstudos.forEach { off ->
                if (off.remoteId == null || !remoteIds.contains(off.remoteId)) {
                    // Criar pseudo-EstudoResponse para offline-only (remoteId=-1 como marcador)
                    estudos.add(EstudoResponse(
                        id = off.remoteId ?: -1,
                        nome = off.nome,
                        observacoes = off.observacoes,
                        perfil = off.perfil,
                        createdAt = off.createdAt ?: "",
                        updatedAt = off.updatedAt ?: ""
                    ))
                }
            }

            popularSpinner(binding.spinnerEstudo, estudos.map { it.nome })
        }
    }

    private fun carregarCampanhas(estudoId: Int) {
        campanhas.clear()
        popularSpinner(binding.spinnerCampanha, emptyList())
        unidades.clear()
        popularSpinner(binding.spinnerUnidade, emptyList())
        eventos.clear()
        popularSpinner(binding.spinnerEvento, emptyList())

        // Se estudo_id == -1, é offline-only: buscar local_id do DAO
        if (estudoId == -1) {
            val estudoSel = estudoSelecionado()
            if (estudoSel != null) {
                val estudo = EstudoDao(this).listarTodos().firstOrNull { it.nome == estudoSel.nome }
                if (estudo != null) {
                    carregarCampanhasOffline(estudo.localId)
                }
            }
            return
        }

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getCampanhas(SessionManager.getAuthHeader(), estudoId)
                if (resp.isSuccessful) {
                    campanhas.addAll(resp.body() ?: emptyList())
                    popularSpinner(binding.spinnerCampanha, campanhas.map { it.nome })
                }
            } catch (_: Exception) { }

            // Merge com SQLite
            val offlineDao = CampanhaDao(this@RegistroRapidoActivity)
            val estudoLocalId = EstudoDao(this@RegistroRapidoActivity).buscarPorRemoteId(estudoId)?.localId ?: return@launch
            val offline = offlineDao.listarPorEstudoLocal(estudoLocalId)
            val remoteIds = campanhas.mapNotNull { it.id }.toSet()

            offline.forEach { off ->
                if (off.remoteId == null || !remoteIds.contains(off.remoteId)) {
                    campanhas.add(CampanhaResponse(
                        id = off.remoteId ?: -1,
                        nome = off.nome,
                        dataInicio = off.dataInicio,
                        dataFim = off.dataFim,
                        descricao = off.descricao,
                        createdAt = off.createdAt ?: "",
                        updatedAt = off.updatedAt ?: "",
                        valoresVariaveis = null
                    ))
                }
            }
            popularSpinner(binding.spinnerCampanha, campanhas.map { it.nome })
        }
    }

    private fun carregarCampanhasOffline(estudoLocalId: Long) {
        val offlineDao = CampanhaDao(this)
        campanhas.clear()
        campanhas.addAll(offlineDao.listarPorEstudoLocal(estudoLocalId).map { off ->
            CampanhaResponse(
                id = off.remoteId ?: -1,
                nome = off.nome,
                dataInicio = off.dataInicio,
                dataFim = off.dataFim,
                descricao = off.descricao,
                createdAt = off.createdAt ?: "",
                updatedAt = off.updatedAt ?: "",
                valoresVariaveis = null
            )
        })
        popularSpinner(binding.spinnerCampanha, campanhas.map { it.nome })
    }

    private fun carregarUnidades(estudoId: Int, campanhaId: Int) {
        unidades.clear()
        popularSpinner(binding.spinnerUnidade, emptyList())
        eventos.clear()
        popularSpinner(binding.spinnerEvento, emptyList())

        if (campanhaId == -1) {
            val campanhaLoc = CampanhaDao(this).listarTodos().firstOrNull { it.remoteId == -1 }
            if (campanhaLoc != null) {
                carregarUnidadesOffline(campanhaLoc.localId)
            }
            return
        }

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getUnidades(SessionManager.getAuthHeader(), estudoId, campanhaId)
                if (resp.isSuccessful) {
                    unidades.addAll(resp.body() ?: emptyList())
                    popularSpinner(binding.spinnerUnidade, unidades.map { it.nome })
                }
            } catch (_: Exception) { }

            // Merge com SQLite
            val unidadeDao = UnidadeDao(this@RegistroRapidoActivity)
            val campanhaLocalId = CampanhaDao(this@RegistroRapidoActivity).buscarPorRemoteIdEscopo(campanhaId, EstudoDao(this@RegistroRapidoActivity).buscarPorRemoteId(estudoId)?.localId ?: return@launch)?.localId ?: return@launch
            val offline = unidadeDao.listarPorCampanhaLocal(campanhaLocalId)
            val remoteIds = unidades.mapNotNull { it.id }.toSet()

            offline.forEach { off ->
                if (off.remoteId == null || !remoteIds.contains(off.remoteId)) {
                    unidades.add(UnidadeResponse(
                        id = off.remoteId ?: -1,
                        campanhaId = off.campanhaLocalId.toInt(),
                        nome = off.nome,
                        latitude = off.latitude,
                        longitude = off.longitude,
                        raio = off.raio,
                        metodoColeta = off.metodoColeta,
                        esforcoAmostral = off.esforcoAmostral,
                        createdAt = off.createdAt ?: "",
                        updatedAt = off.updatedAt ?: ""
                    ))
                }
            }
            popularSpinner(binding.spinnerUnidade, unidades.map { it.nome })
        }
    }

    private fun carregarUnidadesOffline(campanhaLocalId: Long) {
        val unidadeDao = UnidadeDao(this)
        unidades.clear()
        unidades.addAll(unidadeDao.listarPorCampanhaLocal(campanhaLocalId).map { off ->
            UnidadeResponse(
                id = off.remoteId ?: -1,
                campanhaId = off.campanhaLocalId.toInt(),
                nome = off.nome,
                latitude = off.latitude,
                longitude = off.longitude,
                raio = off.raio,
                metodoColeta = off.metodoColeta,
                esforcoAmostral = off.esforcoAmostral,
                createdAt = off.createdAt ?: "",
                updatedAt = off.updatedAt ?: ""
            )
        })
        popularSpinner(binding.spinnerUnidade, unidades.map { it.nome })
    }

    private fun carregarEventos(estudoId: Int, campanhaId: Int, unidadeId: Int) {
        eventos.clear()
        popularSpinner(binding.spinnerEvento, emptyList())

        if (unidadeId == -1) {
            val unidadeLoc = UnidadeDao(this).listarPorCampanhaLocal(CampanhaDao(this).listarTodos().first { it.remoteId == campanhaId }.localId).firstOrNull { it.remoteId == -1 }
            if (unidadeLoc != null) {
                carregarEventosOffline(unidadeLoc.localId)
            }
            return
        }

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.getEventos(SessionManager.getAuthHeader(), estudoId, campanhaId, unidadeId)
                if (resp.isSuccessful) {
                    eventos.addAll(resp.body() ?: emptyList())
                    popularSpinner(binding.spinnerEvento, eventos.map { it.horarioInicio })
                }
            } catch (_: Exception) { }

            // Merge com SQLite
            val eventoDao = EventoDao(this@RegistroRapidoActivity)
            val unidadeLocalId = UnidadeDao(this@RegistroRapidoActivity).buscarPorRemoteIdEscopo(unidadeId, CampanhaDao(this@RegistroRapidoActivity).buscarPorRemoteIdEscopo(campanhaId, EstudoDao(this@RegistroRapidoActivity).buscarPorRemoteId(estudoId)?.localId ?: return@launch)?.localId ?: return@launch)?.localId ?: return@launch
            val offline = eventoDao.listarPorUnidadeLocal(unidadeLocalId)
            val remoteIds = eventos.mapNotNull { it.id }.toSet()

            offline.forEach { off ->
                if (off.remoteId == null || !remoteIds.contains(off.remoteId)) {
                    eventos.add(EventoResponse(
                        id = off.remoteId ?: -off.localId.toInt(),
                        unidadeAmostralId = off.unidadeLocalId.toInt(),
                        horarioInicio = off.horarioInicio,
                        horarioFim = off.horarioFim,
                        esforcoReal = off.esforcoReal,
                        createdAt = off.createdAt ?: ""
                    ))
                }
            }
            popularSpinner(binding.spinnerEvento, eventos.map { it.horarioInicio })
        }
    }

    private fun carregarEventosOffline(unidadeLocalId: Long) {
        val eventoDao = EventoDao(this)
        eventos.clear()
        eventos.addAll(eventoDao.listarPorUnidadeLocal(unidadeLocalId).map { off ->
            EventoResponse(
                id = off.remoteId ?: -off.localId.toInt(),
                unidadeAmostralId = off.unidadeLocalId.toInt(),
                horarioInicio = off.horarioInicio,
                horarioFim = off.horarioFim,
                esforcoReal = off.esforcoReal,
                createdAt = off.createdAt ?: ""
            )
        })
        popularSpinner(binding.spinnerEvento, eventos.map { it.horarioInicio })
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
