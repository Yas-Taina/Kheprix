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
import com.kheprix.db.EstudoOfflineManager
import com.kheprix.db.EventoDao
import com.kheprix.db.UnidadeDao
import com.kheprix.models.CampanhaResponse
import com.kheprix.models.EstudoResponse
import com.kheprix.models.EventoResponse
import com.kheprix.models.UnidadeResponse
import kotlinx.coroutines.launch

class RegistroRapidoActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityRegistroRapidoBinding

    private val estudos = mutableListOf<EstudoResponse>()
    private val campanhas = mutableListOf<CampanhaResponse>()
    private val unidades = mutableListOf<UnidadeResponse>()
    private val eventos = mutableListOf<EventoResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroRapidoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()

        binding.btnProsseguir.setOnClickListener { prosseguir() }
        binding.ivMenuLateral.setOnClickListener { openDrawer() }
        binding.ivPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        carregarEstudos()
    }

    private fun setupSpinners() {
        binding.spinnerEstudo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos == 0) return
                val estudo = estudos.getOrNull(pos - 1) ?: return
                carregarCampanhas(estudo.id)
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.spinnerCampanha.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    if (pos == 0) return
                    val campanha = campanhas.getOrNull(pos - 1) ?: return
                    val estudoId = estudoSelecionado()?.id ?: return
                    carregarUnidades(estudoId, campanha.id)
                }

                override fun onNothingSelected(p: AdapterView<*>?) {}
            }

        binding.spinnerUnidade.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    if (pos == 0) return
                    val unidade = unidades.getOrNull(pos - 1) ?: return
                    val estudoId = estudoSelecionado()?.id ?: return
                    val campanhaId = campanhaSelecionada()?.id ?: return
                    carregarEventos(estudoId, campanhaId, unidade.id)
                }

                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
    }

    private fun carregarEstudos() {
        lifecycleScope.launch {
            estudos.clear()
            try {
                val resp = RetrofitClient.apiService.getEstudos(SessionManager.getAuthHeader())
                if (resp.isSuccessful) estudos.addAll(resp.body() ?: emptyList())
            } catch (_: Exception) {
            }

            val offlineMgr = EstudoOfflineManager(this@RegistroRapidoActivity)
            val offlineEstudos = EstudoDao(this@RegistroRapidoActivity).listarTodos()
            val remoteIds = estudos.mapNotNull { it.id }.toSet()

            offlineEstudos.forEach { off ->
                val isOfflineReal = offlineMgr.isExplicitamenteSalvoOffline(off.localId)
                if (!isOfflineReal) return@forEach
                if (off.remoteId != null && remoteIds.contains(off.remoteId)) return@forEach
                estudos.add(
                    EstudoResponse(
                        id = off.remoteId ?: -off.localId.toInt(),
                        nome = off.nome,
                        observacoes = off.observacoes,
                        perfil = off.perfil,
                        createdAt = off.createdAt ?: "",
                        updatedAt = off.updatedAt ?: ""
                    )
                )
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

        if (estudoId <= 0) {
            val localId = if (estudoId < 0) (-estudoId).toLong()
            else estudoSelecionado()?.let { sel ->
                EstudoDao(this).listarTodos().firstOrNull { it.nome == sel.nome }?.localId
            }
            if (localId != null) carregarCampanhasOffline(localId)
            return
        }

        lifecycleScope.launch {
            try {
                val resp =
                    RetrofitClient.apiService.getCampanhas(SessionManager.getAuthHeader(), estudoId)
                if (resp.isSuccessful) campanhas.addAll(resp.body() ?: emptyList())
            } catch (_: Exception) {
            }

            val estudoLocalId =
                EstudoDao(this@RegistroRapidoActivity).buscarPorRemoteId(estudoId)?.localId
            if (estudoLocalId != null) {
                val offline =
                    CampanhaDao(this@RegistroRapidoActivity).listarPorEstudoLocal(estudoLocalId)
                val remoteIds = campanhas.mapNotNull { it.id }.toSet()
                offline.forEach { off ->
                    if (off.remoteId == null || !remoteIds.contains(off.remoteId)) {
                        campanhas.add(
                            CampanhaResponse(
                                id = off.remoteId ?: -off.localId.toInt(),
                                nome = off.nome,
                                dataInicio = off.dataInicio,
                                dataFim = off.dataFim,
                                descricao = off.descricao,
                                createdAt = off.createdAt ?: "",
                                updatedAt = off.updatedAt ?: "",
                                valoresVariaveis = null
                            )
                        )
                    }
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
                id = off.remoteId ?: -off.localId.toInt(),
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

        if (campanhaId <= 0) {
            val localId = if (campanhaId < 0) (-campanhaId).toLong() else null
            if (localId != null) carregarUnidadesOffline(localId)
            return
        }

        lifecycleScope.launch {
            if (estudoId > 0) {
                try {
                    val resp = RetrofitClient.apiService.getUnidades(
                        SessionManager.getAuthHeader(),
                        estudoId,
                        campanhaId
                    )
                    if (resp.isSuccessful) unidades.addAll(resp.body() ?: emptyList())
                } catch (_: Exception) {
                }
            }

            val estudoLocalId = if (estudoId > 0)
                EstudoDao(this@RegistroRapidoActivity).buscarPorRemoteId(estudoId)?.localId else null
            val campanhaLocalId = estudoLocalId?.let {
                CampanhaDao(this@RegistroRapidoActivity).buscarPorRemoteIdEscopo(
                    campanhaId,
                    it
                )?.localId
            }
            if (campanhaLocalId != null) {
                val offline =
                    UnidadeDao(this@RegistroRapidoActivity).listarPorCampanhaLocal(campanhaLocalId)
                val remoteIds = unidades.mapNotNull { it.id }.toSet()
                offline.forEach { off ->
                    if (off.remoteId == null || !remoteIds.contains(off.remoteId)) {
                        unidades.add(
                            UnidadeResponse(
                                id = off.remoteId ?: -off.localId.toInt(),
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
                        )
                    }
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
                id = off.remoteId ?: -off.localId.toInt(),
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

        if (unidadeId <= 0) {
            val localId = if (unidadeId < 0) (-unidadeId).toLong() else null
            if (localId != null) carregarEventosOffline(localId)
            return
        }

        lifecycleScope.launch {
            if (estudoId > 0 && campanhaId > 0) {
                try {
                    val resp = RetrofitClient.apiService.getEventos(
                        SessionManager.getAuthHeader(),
                        estudoId,
                        campanhaId,
                        unidadeId
                    )
                    if (resp.isSuccessful) eventos.addAll(resp.body() ?: emptyList())
                } catch (_: Exception) {
                }
            }

            val estudoLocalId = if (estudoId > 0)
                EstudoDao(this@RegistroRapidoActivity).buscarPorRemoteId(estudoId)?.localId else null
            val campanhaLocalId = estudoLocalId?.let {
                if (campanhaId > 0) CampanhaDao(this@RegistroRapidoActivity).buscarPorRemoteIdEscopo(
                    campanhaId,
                    it
                )?.localId
                else null
            }
            val unidadeLocalId = campanhaLocalId?.let {
                UnidadeDao(this@RegistroRapidoActivity).buscarPorRemoteIdEscopo(
                    unidadeId,
                    it
                )?.localId
            }
            if (unidadeLocalId != null) {
                val offline =
                    EventoDao(this@RegistroRapidoActivity).listarPorUnidadeLocal(unidadeLocalId)
                val remoteIds = eventos.mapNotNull { it.id }.toSet()
                offline.forEach { off ->
                    if (off.remoteId == null || !remoteIds.contains(off.remoteId)) {
                        eventos.add(
                            EventoResponse(
                                id = off.remoteId ?: -off.localId.toInt(),
                                unidadeAmostralId = off.unidadeLocalId.toInt(),
                                horarioInicio = off.horarioInicio,
                                horarioFim = off.horarioFim,
                                esforcoReal = off.esforcoReal,
                                createdAt = off.createdAt ?: ""
                            )
                        )
                    }
                }
            }
            popularSpinner(
                binding.spinnerEvento,
                eventos.map { formatarDataHora(it.horarioInicio) })
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
        popularSpinner(binding.spinnerEvento, eventos.map { formatarDataHora(it.horarioInicio) })
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

    private fun popularSpinner(spinner: Spinner, itens: List<String>) {
        val lista = mutableListOf("Selecione...") + itens
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lista)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun prosseguir() {
        val estudo = estudoSelecionado()
        val campanha = campanhaSelecionada()
        val unidade = unidadeSelecionada()
        val evento = eventoSelecionado()

        if (estudo == null || campanha == null || unidade == null || evento == null) {
            Toast.makeText(this, "Selecione todos os níveis", Toast.LENGTH_SHORT).show()
            return
        }

        val estudoLocalId = if (estudo.id < 0) (-estudo.id).toLong()
        else EstudoDao(this).buscarPorRemoteId(estudo.id)?.localId ?: -1L
        val campanhaLocalId = if (campanha.id < 0) (-campanha.id).toLong()
        else if (estudoLocalId > 0) CampanhaDao(this).buscarPorRemoteIdEscopo(
            campanha.id,
            estudoLocalId
        )?.localId ?: -1L
        else -1L
        val unidadeLocalId = if (unidade.id < 0) (-unidade.id).toLong()
        else if (campanhaLocalId > 0) UnidadeDao(this).buscarPorRemoteIdEscopo(
            unidade.id,
            campanhaLocalId
        )?.localId ?: -1L
        else -1L
        val eventoLocalId = if (evento.id < 0) (-evento.id).toLong()
        else if (unidadeLocalId > 0) EventoDao(this).buscarPorRemoteIdEscopo(
            evento.id,
            unidadeLocalId
        )?.localId ?: -1L
        else -1L

        val intent = Intent(this, NovoRegistroActivity::class.java).apply {
            putExtra("estudo_remote_id", estudo.id)
            putExtra("estudo_local_id", estudoLocalId)
            putExtra("campanha_id", campanha.id)
            putExtra("campanha_local_id", campanhaLocalId)
            putExtra("unidade_id", unidade.id)
            putExtra("unidade_local_id", unidadeLocalId)
            putExtra("evento_id", evento.id)
            putExtra("evento_local_id", eventoLocalId)
        }
        startActivity(intent)
    }

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
