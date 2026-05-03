package com.kheprix.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.kheprix.api.ApiService
import com.kheprix.api.RetrofitClient
import com.kheprix.api.SessionManager
import com.kheprix.db.DatabaseHelper.Companion.COL_LOCAL_ID
import com.kheprix.db.DatabaseHelper.Companion.COL_REMOTE_ID
import com.kheprix.db.DatabaseHelper.Companion.COL_SINCRONIZADO
import com.kheprix.db.DatabaseHelper.Companion.TABLE_CAMPANHAS
import com.kheprix.db.DatabaseHelper.Companion.TABLE_ESPECIES
import com.kheprix.db.DatabaseHelper.Companion.TABLE_ESTUDOS
import com.kheprix.db.DatabaseHelper.Companion.TABLE_EVENTOS
import com.kheprix.db.DatabaseHelper.Companion.TABLE_REGISTROS
import com.kheprix.db.DatabaseHelper.Companion.TABLE_UNIDADES
import com.kheprix.db.DatabaseHelper.Companion.TABLE_VALORES_VARIAVEIS
import com.kheprix.db.DatabaseHelper.Companion.TABLE_VARIAVEIS
import com.kheprix.models.*

/**
 * Manages offline storage and synchronisation of study data.
 *
 * All public functions are suspending and should be called from a coroutine
 * (e.g. viewModelScope.launch { … }).
 *
 * The five main operations are:
 *   1. [salvarEstudoOffline]    – download study from server and persist locally
 *   2. [sincronizarDadosEstudo] – push unsynced local records to the server
 *   3. [atualizarDadosEstudo]   – sync up, then refresh the local copy from server
 *   4. [deletarDadosEstudo]     – sync up, then wipe the local copy
 *   5. [contarRegistrosOffline] – count rows not yet synced per study
 */
class EstudoOfflineManager(context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val api: ApiService = RetrofitClient.apiService

    // ════════════════════════════════════════════════════════════════════════
    // 1. SALVAR ESTUDO OFFLINE
    //    Downloads the complete study tree from the server and stores it in
    //    SQLite with sincronizado = 1 (already in sync).
    // ════════════════════════════════════════════════════════════════════════

    suspend fun salvarEstudoOffline(estudoRemoteId: Int): Result<Unit> = runCatching {
        val token = SessionManager.getAuthHeader()
        val db = dbHelper.writableDatabase

        // Fetch study root
        val estudosResp = api.getEstudos(token).body()
            ?: error("Falha ao buscar estudos")
        val estudo = estudosResp.firstOrNull { it.id == estudoRemoteId }
            ?: error("Estudo $estudoRemoteId não encontrado")

        // Upsert study row
        val estudoLocalId = upsertEstudo(db, estudo)

        // Variables
        val variaveis = api.getVariaveis(token, estudoRemoteId).body() ?: emptyList()
        val varLocalIds = mutableMapOf<Int, Long>() // remoteId -> localId
        variaveis.forEach { v ->
            val localId = upsertVariavel(db, v, estudoLocalId)
            varLocalIds[v.id] = localId
        }

        // Species
        val especies = api.getEspecies(token, estudoRemoteId).body() ?: emptyList()
        val espLocalIds = mutableMapOf<Int, Long>()
        especies.forEach { e ->
            val localId = upsertEspecie(db, e, estudoLocalId)
            espLocalIds[e.id] = localId
        }

        // Campaigns
        val campanhas = api.getCampanhas(token, estudoRemoteId).body() ?: emptyList()
        campanhas.forEach { campanha ->
            val campanhaLocalId = upsertCampanha(db, campanha, estudoLocalId)

            // Sampling units
            val unidades = api.getUnidades(token, estudoRemoteId, campanha.id).body() ?: emptyList()
            unidades.forEach { unidade ->
                val unidadeLocalId = upsertUnidade(db, unidade, campanhaLocalId)

                // Sampling events
                val eventos = api.getEventos(token, estudoRemoteId, campanha.id, unidade.id).body() ?: emptyList()
                eventos.forEach { evento ->
                    val eventoLocalId = upsertEvento(db, evento, unidadeLocalId)

                    // Occurrence records
                    val registros = api.getRegistros(token, estudoRemoteId, campanha.id, unidade.id, evento.id).body() ?: emptyList()
                    registros.forEach { registro ->
                        val espLocalId = espLocalIds[registro.especieId]
                            ?: error("Espécie remota ${registro.especieId} não encontrada localmente")
                        upsertRegistro(db, registro, eventoLocalId, espLocalId)
                    }
                }
            }
        }

        Log.d("OfflineManager", "Estudo $estudoRemoteId salvo offline com sucesso.")
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. SINCRONIZAR DADOS DO ESTUDO
    //    Pushes all rows with sincronizado = 0 to the server.
    //    Local IDs are NOT sent; the server generates its own IDs.
    // ════════════════════════════════════════════════════════════════════════

    suspend fun sincronizarDadosEstudo(estudoLocalId: Long): Result<Unit> = runCatching {
        val token = SessionManager.getAuthHeader()
        val db = dbHelper.writableDatabase

        // ── Study itself ─────────────────────────────────────────────────────
        val estudo = queryEstudoByLocalId(db, estudoLocalId)
        var estudoRemoteId = estudo.remoteId

        // Load local variables up front — the backend requires them in the
        // study creation payload (POST /estudos rejects empty `variaveis`).
        val variaveisLocais = queryUnsyncedVariaveis(db, estudoLocalId)

        if (estudo.sincronizado == 0) {
            if (variaveisLocais.isEmpty()) {
                error("Estudo não possui variáveis para sincronizar")
            }
            val req = EstudoRequest(
                nome = estudo.nome,
                observacoes = estudo.observacoes,
                variaveis = variaveisLocais.map { v ->
                    VariavelRequest(
                        nome = v.nome,
                        nivelAplicacao = v.nivelAplicacao,
                        tipoDado = v.tipoDado,
                        metrica = v.metrica
                    )
                }
            )
            val resp = api.postEstudo(token, req).body()
                ?: error("Erro ao criar estudo no servidor")
            estudoRemoteId = resp.id
            markSynced(db, TABLE_ESTUDOS, estudoLocalId, estudoRemoteId)
        }

        val remoteEstudoId = estudoRemoteId ?: error("Estudo remoto não disponível")

        // ── Variables ────────────────────────────────────────────────────────
        // After the study POST the server has created the variables; fetch them
        // back and match by (nome, nivel_aplicacao, tipo_dado) to stamp remote
        // IDs on the local rows.
        val varLocalToRemote = mutableMapOf<Long, Int>()
        if (variaveisLocais.isNotEmpty()) {
            val remotas = api.getVariaveis(token, remoteEstudoId).body() ?: emptyList()
            variaveisLocais.forEach { v ->
                val match = remotas.firstOrNull {
                    it.nome == v.nome &&
                    it.nivelAplicacao == v.nivelAplicacao &&
                    it.tipoDado == v.tipoDado
                }
                if (match != null) {
                    varLocalToRemote[v.localId] = match.id
                    markSynced(db, TABLE_VARIAVEIS, v.localId, match.id)
                }
            }
        }

        // ── Species ──────────────────────────────────────────────────────────
        val especies = queryUnsyncedEspecies(db, estudoLocalId)
        val espLocalToRemote = mutableMapOf<Long, Int>()
        especies.forEach { e ->
            val req = EspecieRequest(
                classe = e.classe,
                ordem = e.ordem,
                familia = e.familia,
                genero = e.genero,
                especie = e.especie,
                endemismo = e.endemismo,
                foto = e.foto,
                nomePopular = e.nomePopular,
                statusConservacao = e.statusConservacao
            )
            val resp = api.postEspecie(token, remoteEstudoId, req).body()
                ?: error("Erro ao criar espécie")
            espLocalToRemote[e.localId] = resp.id
            markSynced(db, TABLE_ESPECIES, e.localId, resp.id)
        }

        // Also load already-synced species for use in records
        queryAllEspecies(db, estudoLocalId).forEach { e ->
            if (e.remoteId != null) espLocalToRemote[e.localId] = e.remoteId
        }

        // ── Campaigns ────────────────────────────────────────────────────────
        val campanhas = queryAllCampanhas(db, estudoLocalId)
        campanhas.forEach { campanha ->
            var campanhaRemoteId = campanha.remoteId

            if (campanha.sincronizado == 0) {
                // Coleta valores_variaveis da campanha; resolve variavel_id remoto
                // a partir do varLocalToRemote (variáveis recém-sincronizadas) ou
                // do próprio variavel_remote_id armazenado.
                val valoresLocais = queryUnsyncedValoresVariaveisDeCampanha(db, campanha.localId)
                val valoresReq = valoresLocais.mapNotNull { vv ->
                    val varRemoteId = varLocalToRemote[vv.variavelLocalId]
                        ?: vv.variavelRemoteId
                        ?: queryVariavelRemoteId(db, vv.variavelLocalId)
                    if (varRemoteId != null && varRemoteId > 0)
                        ValorVariavelRequest(variavelId = varRemoteId, valor = vv.valor)
                    else null
                }

                val req = CampanhaRequest(
                    nome = campanha.nome,
                    dataInicio = campanha.dataInicio,
                    dataFim = campanha.dataFim,
                    descricao = campanha.descricao,
                    valoresVariaveis = valoresReq.ifEmpty { null }
                )
                val resp = api.postCampanha(token, remoteEstudoId, req).body()
                    ?: error("Erro ao criar campanha")
                campanhaRemoteId = resp.id
                markSynced(db, TABLE_CAMPANHAS, campanha.localId, resp.id)
                markValoresVariaveisCampanhaSynced(db, campanha.localId)
            } else if (campanhaRemoteId != null && campanhaRemoteId > 0) {
                // Campanha já sincronizada, mas pode ter valores_variaveis órfãos
                // (criados offline em sync anterior antes do fix). Faz PATCH.
                val orfaos = queryUnsyncedValoresVariaveisDeCampanha(db, campanha.localId)
                if (orfaos.isNotEmpty()) {
                    val valoresReq = orfaos.mapNotNull { vv ->
                        val varRemoteId = varLocalToRemote[vv.variavelLocalId]
                            ?: vv.variavelRemoteId
                            ?: queryVariavelRemoteId(db, vv.variavelLocalId)
                        if (varRemoteId != null && varRemoteId > 0)
                            ValorVariavelRequest(variavelId = varRemoteId, valor = vv.valor)
                        else null
                    }
                    if (valoresReq.isNotEmpty()) {
                        val patchReq = CampanhaRequest(
                            nome = campanha.nome,
                            dataInicio = campanha.dataInicio,
                            dataFim = campanha.dataFim,
                            descricao = campanha.descricao,
                            valoresVariaveis = valoresReq
                        )
                        val patchResp = api.patchCampanha(token, remoteEstudoId, campanhaRemoteId, patchReq)
                        if (patchResp.isSuccessful) {
                            markValoresVariaveisCampanhaSynced(db, campanha.localId)
                        } else {
                            error("Erro ao enviar valores_variaveis pendentes da campanha (HTTP ${patchResp.code()})")
                        }
                    }
                }
            }

            val remoteCampanhaId = campanhaRemoteId ?: error("Campanha remota não disponível")

            // ── Sampling units ──────────────────────────────────────────────
            val unidades = queryAllUnidades(db, campanha.localId)
            unidades.forEach { unidade ->
                var unidadeRemoteId = unidade.remoteId

                if (unidade.sincronizado == 0) {
                    val req = UnidadeRequest(
                        nome = unidade.nome,
                        latitude = unidade.latitude,
                        longitude = unidade.longitude,
                        raio = unidade.raio,
                        metodoColeta = unidade.metodoColeta,
                        esforcoAmostral = unidade.esforcoAmostral
                    )
                    val resp = api.postUnidade(token, remoteEstudoId, remoteCampanhaId, req).body()
                        ?: error("Erro ao criar unidade amostral")
                    unidadeRemoteId = resp.id
                    markSynced(db, TABLE_UNIDADES, unidade.localId, resp.id)
                }

                val remoteUnidadeId = unidadeRemoteId ?: error("Unidade remota não disponível")

                // ── Sampling events ─────────────────────────────────────────
                val eventos = queryAllEventos(db, unidade.localId)
                eventos.forEach { evento ->
                    var eventoRemoteId = evento.remoteId

                    if (evento.sincronizado == 0) {
                        val req = EventoRequest(
                            horarioInicio = evento.horarioInicio,
                            horarioFim = evento.horarioFim,
                            esforcoReal = evento.esforcoReal
                        )
                        val resp = api.postEvento(token, remoteEstudoId, remoteCampanhaId, remoteUnidadeId, req).body()
                            ?: error("Erro ao criar evento de amostragem")
                        eventoRemoteId = resp.id
                        markSynced(db, TABLE_EVENTOS, evento.localId, resp.id)
                    }

                    val remoteEventoId = eventoRemoteId ?: error("Evento remoto não disponível")

                    // ── Occurrence records ───────────────────────────────────
                    val registros = queryAllRegistros(db, evento.localId)
                    registros.forEach { registro ->
                        if (registro.sincronizado == 0) {
                            val especieRemoteId = espLocalToRemote[registro.especieLocalId]
                                ?: error("Espécie remota para registro ${registro.localId} não encontrada")
                            val req = RegistroRequest(
                                especieId = especieRemoteId,
                                data = registro.data,
                                hora = registro.hora,
                                latitude = registro.latitude,
                                longitude = registro.longitude,
                                qtdeIndividuos = registro.qtdeIndividuos,
                                foto = registro.foto,
                                ausenciaEspecie = registro.ausenciaEspecie
                            )
                            val resp = api.postRegistro(
                                token, remoteEstudoId, remoteCampanhaId,
                                remoteUnidadeId, remoteEventoId, req
                            ).body() ?: error("Erro ao criar registro de ocorrência")
                            markSynced(db, TABLE_REGISTROS, registro.localId, resp.id)
                        }
                    }
                }
            }
        }

        Log.d("OfflineManager", "Estudo localId=$estudoLocalId sincronizado com sucesso.")
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. ATUALIZAR DADOS DO ESTUDO
    //    Sync up → wipe local → download fresh copy from server.
    // ════════════════════════════════════════════════════════════════════════

    suspend fun atualizarDadosEstudo(estudoLocalId: Long): Result<Unit> = runCatching {
        // Push any local-only data first so nothing is lost
        sincronizarDadosEstudo(estudoLocalId).getOrThrow()

        // Retrieve the remote id before wiping
        val db = dbHelper.readableDatabase
        val estudo = queryEstudoByLocalId(db, estudoLocalId)
        val remoteId = estudo.remoteId ?: error("Estudo ainda não possui ID remoto após sincronização")

        // Delete local copy
        deletarDadosEstudoLocal(estudoLocalId)

        // Re-download fresh copy
        salvarEstudoOffline(remoteId).getOrThrow()

        Log.d("OfflineManager", "Estudo localId=$estudoLocalId atualizado com sucesso.")
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. DELETAR DADOS DO ESTUDO
    //    Sync up → wipe all local rows for this study tree.
    // ════════════════════════════════════════════════════════════════════════

    suspend fun deletarDadosEstudo(estudoLocalId: Long): Result<Unit> = runCatching {
        sincronizarDadosEstudo(estudoLocalId).getOrThrow()
        deletarDadosEstudoLocal(estudoLocalId)
        Log.d("OfflineManager", "Dados locais do estudo localId=$estudoLocalId deletados.")
    }

    /** Wipes all local rows for the study tree without syncing first. */
    fun deletarDadosEstudoLocal(estudoLocalId: Long) {
        val db = dbHelper.writableDatabase
        // Cascade order: deepest first
        val campanhaLocalIds = queryCampanhaLocalIds(db, estudoLocalId)
        campanhaLocalIds.forEach { campanhaLocalId ->
            val unidadeLocalIds = queryUnidadeLocalIds(db, campanhaLocalId)
            unidadeLocalIds.forEach { unidadeLocalId ->
                val eventoLocalIds = queryEventoLocalIds(db, unidadeLocalId)
                eventoLocalIds.forEach { eventoLocalId ->
                    db.delete(TABLE_REGISTROS, "evento_local_id = ?", arrayOf(eventoLocalId.toString()))
                }
                db.delete(TABLE_EVENTOS, "unidade_local_id = ?", arrayOf(unidadeLocalId.toString()))
            }
            db.delete(TABLE_UNIDADES, "campanha_local_id = ?", arrayOf(campanhaLocalId.toString()))
            db.delete(TABLE_VALORES_VARIAVEIS, "campanha_local_id = ?", arrayOf(campanhaLocalId.toString()))
        }
        db.delete(TABLE_CAMPANHAS, "estudo_local_id = ?", arrayOf(estudoLocalId.toString()))
        db.delete(TABLE_ESPECIES,  "estudo_local_id = ?", arrayOf(estudoLocalId.toString()))
        db.delete(TABLE_VARIAVEIS, "estudo_local_id = ?", arrayOf(estudoLocalId.toString()))
        db.delete(TABLE_ESTUDOS,   COL_LOCAL_ID + " = ?", arrayOf(estudoLocalId.toString()))
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. CONTAR REGISTROS OFFLINE
    //    Returns the total count of rows with sincronizado = 0 for the
    //    entire study tree (study + all child levels).
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Indica se o estudo foi explicitamente salvo offline pelo usuário
     * (vs. apenas espelhado via `cacheEstudo` durante navegação online).
     *
     * Heurística: presença de variáveis locais. `salvarEstudoOffline` e
     * `criarEstudoOffline` inserem variáveis; `cacheEstudo` não.
     */
    fun isExplicitamenteSalvoOffline(estudoLocalId: Long): Boolean {
        val db = dbHelper.readableDatabase
        return db.rawQuery(
            "SELECT 1 FROM $TABLE_VARIAVEIS WHERE estudo_local_id = ? LIMIT 1",
            arrayOf(estudoLocalId.toString())
        ).use { it.moveToFirst() }
    }

    /**
     * Diagnóstico: lista descrições legíveis de cada linha com sincronizado=0
     * dentro do estudo. Útil para mostrar ao usuário "quem" ainda está pendente
     * após uma tentativa de sincronização que reportou sucesso mas deixou contagem > 0.
     */
    fun listarRegistrosOfflinePendentes(estudoLocalId: Long): List<String> {
        val db = dbHelper.readableDatabase
        val items = mutableListOf<String>()

        db.rawQuery(
            "SELECT nome FROM $TABLE_ESTUDOS WHERE $COL_LOCAL_ID = ? AND $COL_SINCRONIZADO = 0",
            arrayOf(estudoLocalId.toString())
        ).use { if (it.moveToFirst()) items.add("Estudo: ${it.getString(0)}") }

        db.rawQuery(
            "SELECT nome FROM $TABLE_VARIAVEIS WHERE estudo_local_id = ? AND $COL_SINCRONIZADO = 0",
            arrayOf(estudoLocalId.toString())
        ).use { while (it.moveToNext()) items.add("Variável: ${it.getString(0)}") }

        db.rawQuery(
            "SELECT genero, especie FROM $TABLE_ESPECIES WHERE estudo_local_id = ? AND $COL_SINCRONIZADO = 0",
            arrayOf(estudoLocalId.toString())
        ).use { while (it.moveToNext()) items.add("Espécie: ${it.getString(0)} ${it.getString(1)}") }

        val campanhas = mutableListOf<Triple<Long, String, Int>>()
        db.rawQuery(
            "SELECT $COL_LOCAL_ID, nome, $COL_SINCRONIZADO FROM $TABLE_CAMPANHAS WHERE estudo_local_id = ?",
            arrayOf(estudoLocalId.toString())
        ).use {
            while (it.moveToNext()) {
                campanhas.add(Triple(it.getLong(0), it.getString(1), it.getInt(2)))
            }
        }

        campanhas.forEach { (campanhaLocalId, nomeCamp, sincCamp) ->
            if (sincCamp == 0) items.add("Campanha: $nomeCamp")

            db.rawQuery(
                "SELECT v.nome, vv.valor FROM $TABLE_VALORES_VARIAVEIS vv " +
                        "JOIN $TABLE_VARIAVEIS v ON v.$COL_LOCAL_ID = vv.variavel_local_id " +
                        "WHERE vv.campanha_local_id = ? AND vv.$COL_SINCRONIZADO = 0",
                arrayOf(campanhaLocalId.toString())
            ).use {
                while (it.moveToNext()) {
                    items.add("Valor de variável da campanha '$nomeCamp' → '${it.getString(0)}' = ${it.getString(1)}")
                }
            }

            val unidades = mutableListOf<Triple<Long, String, Int>>()
            db.rawQuery(
                "SELECT $COL_LOCAL_ID, nome, $COL_SINCRONIZADO FROM $TABLE_UNIDADES WHERE campanha_local_id = ?",
                arrayOf(campanhaLocalId.toString())
            ).use {
                while (it.moveToNext()) {
                    unidades.add(Triple(it.getLong(0), it.getString(1), it.getInt(2)))
                }
            }

            unidades.forEach { (unidadeLocalId, nomeU, sincU) ->
                if (sincU == 0) items.add("Unidade: $nomeU (campanha: $nomeCamp)")

                val eventos = mutableListOf<Triple<Long, String, Int>>()
                db.rawQuery(
                    "SELECT $COL_LOCAL_ID, horario_inicio, $COL_SINCRONIZADO FROM $TABLE_EVENTOS WHERE unidade_local_id = ?",
                    arrayOf(unidadeLocalId.toString())
                ).use {
                    while (it.moveToNext()) {
                        eventos.add(Triple(it.getLong(0), it.getString(1), it.getInt(2)))
                    }
                }

                eventos.forEach { (eventoLocalId, horario, sincE) ->
                    if (sincE == 0) items.add("Evento: $horario (unidade: $nomeU)")
                    db.rawQuery(
                        "SELECT data, hora FROM $TABLE_REGISTROS WHERE evento_local_id = ? AND $COL_SINCRONIZADO = 0",
                        arrayOf(eventoLocalId.toString())
                    ).use {
                        while (it.moveToNext()) {
                            items.add("Registro: ${it.getString(0)} ${it.getString(1)} (evento: $horario)")
                        }
                    }
                }
            }
        }

        return items
    }

    fun contarRegistrosOffline(estudoLocalId: Long): Int {
        val db = dbHelper.readableDatabase
        var total = 0

        // Study itself
        total += countUnsyncedRows(db, TABLE_ESTUDOS, "local_id = ? AND $COL_SINCRONIZADO = 0", estudoLocalId.toString())

        // Variables & Species
        total += countUnsyncedRows(db, TABLE_VARIAVEIS, "estudo_local_id = ? AND $COL_SINCRONIZADO = 0", estudoLocalId.toString())
        total += countUnsyncedRows(db, TABLE_ESPECIES,  "estudo_local_id = ? AND $COL_SINCRONIZADO = 0", estudoLocalId.toString())

        // Campaigns
        val campanhaLocalIds = queryCampanhaLocalIds(db, estudoLocalId)
        total += countUnsyncedRows(db, TABLE_CAMPANHAS, "estudo_local_id = ? AND $COL_SINCRONIZADO = 0", estudoLocalId.toString())

        campanhaLocalIds.forEach { campanhaLocalId ->
            // Campaign variable values
            total += countUnsyncedRows(db, TABLE_VALORES_VARIAVEIS, "campanha_local_id = ? AND $COL_SINCRONIZADO = 0", campanhaLocalId.toString())

            val unidadeLocalIds = queryUnidadeLocalIds(db, campanhaLocalId)
            total += countUnsyncedRows(db, TABLE_UNIDADES, "campanha_local_id = ? AND $COL_SINCRONIZADO = 0", campanhaLocalId.toString())

            unidadeLocalIds.forEach { unidadeLocalId ->
                val eventoLocalIds = queryEventoLocalIds(db, unidadeLocalId)
                total += countUnsyncedRows(db, TABLE_EVENTOS, "unidade_local_id = ? AND $COL_SINCRONIZADO = 0", unidadeLocalId.toString())

                eventoLocalIds.forEach { eventoLocalId ->
                    total += countUnsyncedRows(db, TABLE_REGISTROS, "evento_local_id = ? AND $COL_SINCRONIZADO = 0", eventoLocalId.toString())
                }
            }
        }

        return total
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS – UPSERT
    // ════════════════════════════════════════════════════════════════════════

    private fun upsertEstudo(db: android.database.sqlite.SQLiteDatabase, e: EstudoResponse): Long {
        val existing = findByRemoteId(db, TABLE_ESTUDOS, e.id)
        val cv = ContentValues().apply {
            put(COL_REMOTE_ID, e.id)
            put(COL_SINCRONIZADO, 1)
            put("nome", e.nome)
            put("observacoes", e.observacoes)
            put("perfil", e.perfil)
            put("created_at", e.createdAt)
            put("updated_at", e.updatedAt)
        }
        return if (existing != null) {
            db.update(TABLE_ESTUDOS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insert(TABLE_ESTUDOS, null, cv)
        }
    }

    private fun upsertVariavel(db: android.database.sqlite.SQLiteDatabase, v: VariavelResponse, estudoLocalId: Long): Long {
        val existing = findByRemoteIdAndParent(db, TABLE_VARIAVEIS, v.id, "estudo_local_id", estudoLocalId)
        val cv = ContentValues().apply {
            put(COL_REMOTE_ID, v.id)
            put(COL_SINCRONIZADO, 1)
            put("estudo_local_id", estudoLocalId)
            put("nome", v.nome)
            put("nivel_aplicacao", v.nivelAplicacao)
            put("tipo_dado", v.tipoDado)
            put("metrica", v.metrica)
            put("created_at", v.createdAt)
            put("updated_at", v.updatedAt)
        }
        return if (existing != null) {
            db.update(TABLE_VARIAVEIS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insert(TABLE_VARIAVEIS, null, cv)
        }
    }

    private fun upsertEspecie(db: android.database.sqlite.SQLiteDatabase, e: EspecieResponse, estudoLocalId: Long): Long {
        val existing = findByRemoteIdAndParent(db, TABLE_ESPECIES, e.id, "estudo_local_id", estudoLocalId)
        val cv = ContentValues().apply {
            put(COL_REMOTE_ID, e.id)
            put(COL_SINCRONIZADO, 1)
            put("estudo_local_id", estudoLocalId)
            put("foto", e.foto)
            put("classe", e.classe)
            put("ordem", e.ordem)
            put("familia", e.familia)
            put("genero", e.genero)
            put("especie", e.especie)
            put("nome_popular", e.nomePopular)
            put("status_conservacao", e.statusConservacao)
            put("endemismo", if (e.endemismo) 1 else 0)
            put("created_at", e.createdAt)
        }
        return if (existing != null) {
            db.update(TABLE_ESPECIES, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insert(TABLE_ESPECIES, null, cv)
        }
    }

    private fun upsertCampanha(db: android.database.sqlite.SQLiteDatabase, c: CampanhaResponse, estudoLocalId: Long): Long {
        val existing = findByRemoteIdAndParent(db, TABLE_CAMPANHAS, c.id, "estudo_local_id", estudoLocalId)
        val cv = ContentValues().apply {
            put(COL_REMOTE_ID, c.id)
            put(COL_SINCRONIZADO, 1)
            put("estudo_local_id", estudoLocalId)
            put("nome", c.nome)
            put("data_inicio", c.dataInicio)
            put("data_fim", c.dataFim)
            put("descricao", c.descricao)
            put("created_at", c.createdAt)
            put("updated_at", c.updatedAt)
        }
        return if (existing != null) {
            db.update(TABLE_CAMPANHAS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insert(TABLE_CAMPANHAS, null, cv)
        }
    }

    private fun upsertUnidade(db: android.database.sqlite.SQLiteDatabase, u: UnidadeResponse, campanhaLocalId: Long): Long {
        val existing = findByRemoteIdAndParent(db, TABLE_UNIDADES, u.id, "campanha_local_id", campanhaLocalId)
        val cv = ContentValues().apply {
            put(COL_REMOTE_ID, u.id)
            put(COL_SINCRONIZADO, 1)
            put("campanha_local_id", campanhaLocalId)
            put("nome", u.nome)
            put("latitude", u.latitude)
            put("longitude", u.longitude)
            put("raio", u.raio)
            put("metodo_coleta", u.metodoColeta)
            put("esforco_amostral", u.esforcoAmostral)
            put("created_at", u.createdAt)
            put("updated_at", u.updatedAt)
        }
        return if (existing != null) {
            db.update(TABLE_UNIDADES, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insert(TABLE_UNIDADES, null, cv)
        }
    }

    private fun upsertEvento(db: android.database.sqlite.SQLiteDatabase, e: EventoResponse, unidadeLocalId: Long): Long {
        val existing = findByRemoteIdAndParent(db, TABLE_EVENTOS, e.id, "unidade_local_id", unidadeLocalId)
        val cv = ContentValues().apply {
            put(COL_REMOTE_ID, e.id)
            put(COL_SINCRONIZADO, 1)
            put("unidade_local_id", unidadeLocalId)
            put("horario_inicio", e.horarioInicio)
            put("horario_fim", e.horarioFim)
            put("esforco_real", e.esforcoReal)
            put("created_at", e.createdAt)
        }
        return if (existing != null) {
            db.update(TABLE_EVENTOS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insert(TABLE_EVENTOS, null, cv)
        }
    }

    private fun upsertRegistro(db: android.database.sqlite.SQLiteDatabase, r: RegistroResponse, eventoLocalId: Long, especieLocalId: Long): Long {
        val existing = findByRemoteIdAndParent(db, TABLE_REGISTROS, r.id, "evento_local_id", eventoLocalId)
        val cv = ContentValues().apply {
            put(COL_REMOTE_ID, r.id)
            put(COL_SINCRONIZADO, 1)
            put("evento_local_id", eventoLocalId)
            put("especie_local_id", especieLocalId)
            put("especie_remote_id", r.especieId)
            put("data", r.data)
            put("hora", r.hora)
            put("latitude", r.latitude)
            put("longitude", r.longitude)
            put("qtde_individuos", r.qtdeIndividuos)
            put("foto", r.foto)
            put("ausencia_especie", r.ausenciaEspecie?.let { if (it) 1 else 0 })
            put("created_at", r.createdAt)
        }
        return if (existing != null) {
            db.update(TABLE_REGISTROS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insert(TABLE_REGISTROS, null, cv)
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS – QUERIES
    // ════════════════════════════════════════════════════════════════════════

    private fun markSynced(
        db: android.database.sqlite.SQLiteDatabase,
        table: String, localId: Long, remoteId: Int
    ) {
        val cv = ContentValues().apply {
            put(COL_SINCRONIZADO, 1)
            put(COL_REMOTE_ID, remoteId)
        }
        db.update(table, cv, "$COL_LOCAL_ID = ?", arrayOf(localId.toString()))
    }

    private fun findByRemoteId(db: android.database.sqlite.SQLiteDatabase, table: String, remoteId: Int): Long? {
        val cursor = db.rawQuery("SELECT $COL_LOCAL_ID FROM $table WHERE $COL_REMOTE_ID = ?", arrayOf(remoteId.toString()))
        return cursor.use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    private fun findByRemoteIdAndParent(
        db: android.database.sqlite.SQLiteDatabase,
        table: String, remoteId: Int,
        parentCol: String, parentLocalId: Long
    ): Long? {
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID FROM $table WHERE $COL_REMOTE_ID = ? AND $parentCol = ?",
            arrayOf(remoteId.toString(), parentLocalId.toString())
        )
        return cursor.use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    private fun countUnsyncedRows(
        db: android.database.sqlite.SQLiteDatabase,
        table: String, where: String, vararg args: String
    ): Int {
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $table WHERE $where", args)
        return cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    private fun queryCampanhaLocalIds(db: android.database.sqlite.SQLiteDatabase, estudoLocalId: Long): List<Long> {
        val cursor = db.rawQuery("SELECT $COL_LOCAL_ID FROM $TABLE_CAMPANHAS WHERE estudo_local_id = ?", arrayOf(estudoLocalId.toString()))
        return cursor.use { buildLongList(it) }
    }

    private fun queryUnidadeLocalIds(db: android.database.sqlite.SQLiteDatabase, campanhaLocalId: Long): List<Long> {
        val cursor = db.rawQuery("SELECT $COL_LOCAL_ID FROM $TABLE_UNIDADES WHERE campanha_local_id = ?", arrayOf(campanhaLocalId.toString()))
        return cursor.use { buildLongList(it) }
    }

    private fun queryEventoLocalIds(db: android.database.sqlite.SQLiteDatabase, unidadeLocalId: Long): List<Long> {
        val cursor = db.rawQuery("SELECT $COL_LOCAL_ID FROM $TABLE_EVENTOS WHERE unidade_local_id = ?", arrayOf(unidadeLocalId.toString()))
        return cursor.use { buildLongList(it) }
    }

    private fun buildLongList(cursor: Cursor): List<Long> {
        val list = mutableListOf<Long>()
        while (cursor.moveToNext()) list.add(cursor.getLong(0))
        return list
    }

    // ── Local-only data helpers (rows with sincronizado = 0) ─────────────────

    private fun queryUnsyncedVariaveis(db: android.database.sqlite.SQLiteDatabase, estudoLocalId: Long): List<LocalVariavel> {
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, nome, nivel_aplicacao, tipo_dado, metrica FROM $TABLE_VARIAVEIS WHERE estudo_local_id = ? AND $COL_SINCRONIZADO = 0",
            arrayOf(estudoLocalId.toString())
        )
        val list = mutableListOf<LocalVariavel>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(LocalVariavel(
                    localId = it.getLong(0),
                    remoteId = if (it.isNull(1)) null else it.getInt(1),
                    nome = it.getString(2),
                    nivelAplicacao = it.getString(3),
                    tipoDado = it.getString(4),
                    metrica = it.getString(5)
                ))
            }
        }
        return list
    }

    private fun queryUnsyncedEspecies(db: android.database.sqlite.SQLiteDatabase, estudoLocalId: Long): List<LocalEspecie> {
        return queryEspecies(db, estudoLocalId, syncFilter = "AND $COL_SINCRONIZADO = 0")
    }

    private fun queryAllEspecies(db: android.database.sqlite.SQLiteDatabase, estudoLocalId: Long): List<LocalEspecie> {
        return queryEspecies(db, estudoLocalId, syncFilter = "")
    }

    private fun queryEspecies(db: android.database.sqlite.SQLiteDatabase, estudoLocalId: Long, syncFilter: String): List<LocalEspecie> {
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, foto, classe, ordem, familia, genero, especie, nome_popular, status_conservacao, endemismo FROM $TABLE_ESPECIES WHERE estudo_local_id = ? $syncFilter",
            arrayOf(estudoLocalId.toString())
        )
        val list = mutableListOf<LocalEspecie>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(LocalEspecie(
                    localId = it.getLong(0),
                    remoteId = if (it.isNull(1)) null else it.getInt(1),
                    foto = it.getString(2),
                    classe = it.getString(3),
                    ordem = it.getString(4),
                    familia = it.getString(5),
                    genero = it.getString(6),
                    especie = it.getString(7),
                    nomePopular = it.getString(8),
                    statusConservacao = it.getString(9),
                    endemismo = it.getInt(10) == 1
                ))
            }
        }
        return list
    }

    private fun queryAllCampanhas(db: android.database.sqlite.SQLiteDatabase, estudoLocalId: Long): List<LocalCampanha> {
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, $COL_SINCRONIZADO, nome, data_inicio, data_fim, descricao FROM $TABLE_CAMPANHAS WHERE estudo_local_id = ?",
            arrayOf(estudoLocalId.toString())
        )
        val list = mutableListOf<LocalCampanha>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(LocalCampanha(
                    localId = it.getLong(0),
                    remoteId = if (it.isNull(1)) null else it.getInt(1),
                    sincronizado = it.getInt(2),
                    nome = it.getString(3),
                    dataInicio = it.getString(4),
                    dataFim = it.getString(5),
                    descricao = it.getString(6)
                ))
            }
        }
        return list
    }

    private fun queryAllUnidades(db: android.database.sqlite.SQLiteDatabase, campanhaLocalId: Long): List<LocalUnidade> {
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, $COL_SINCRONIZADO, nome, latitude, longitude, raio, metodo_coleta, esforco_amostral FROM $TABLE_UNIDADES WHERE campanha_local_id = ?",
            arrayOf(campanhaLocalId.toString())
        )
        val list = mutableListOf<LocalUnidade>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(LocalUnidade(
                    localId = it.getLong(0),
                    remoteId = if (it.isNull(1)) null else it.getInt(1),
                    sincronizado = it.getInt(2),
                    nome = it.getString(3),
                    latitude = it.getDouble(4),
                    longitude = it.getDouble(5),
                    raio = if (it.isNull(6)) null else it.getDouble(6),
                    metodoColeta = it.getString(7),
                    esforcoAmostral = it.getString(8)
                ))
            }
        }
        return list
    }

    private fun queryAllEventos(db: android.database.sqlite.SQLiteDatabase, unidadeLocalId: Long): List<LocalEvento> {
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, $COL_SINCRONIZADO, horario_inicio, horario_fim, esforco_real FROM $TABLE_EVENTOS WHERE unidade_local_id = ?",
            arrayOf(unidadeLocalId.toString())
        )
        val list = mutableListOf<LocalEvento>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(LocalEvento(
                    localId = it.getLong(0),
                    remoteId = if (it.isNull(1)) null else it.getInt(1),
                    sincronizado = it.getInt(2),
                    horarioInicio = it.getString(3),
                    horarioFim = it.getString(4),
                    esforcoReal = it.getString(5)
                ))
            }
        }
        return list
    }

    private fun queryAllRegistros(db: android.database.sqlite.SQLiteDatabase, eventoLocalId: Long): List<LocalRegistro> {
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, $COL_SINCRONIZADO, especie_local_id, data, hora, latitude, longitude, qtde_individuos, foto, ausencia_especie FROM $TABLE_REGISTROS WHERE evento_local_id = ?",
            arrayOf(eventoLocalId.toString())
        )
        val list = mutableListOf<LocalRegistro>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(LocalRegistro(
                    localId = it.getLong(0),
                    remoteId = if (it.isNull(1)) null else it.getInt(1),
                    sincronizado = it.getInt(2),
                    especieLocalId = it.getLong(3),
                    data = it.getString(4),
                    hora = it.getString(5),
                    latitude = it.getDouble(6),
                    longitude = it.getDouble(7),
                    qtdeIndividuos = if (it.isNull(8)) null else it.getInt(8),
                    foto = it.getString(9),
                    ausenciaEspecie = if (it.isNull(10)) null else it.getInt(10) == 1
                ))
            }
        }
        return list
    }

    private fun queryUnsyncedValoresVariaveisDeCampanha(
        db: android.database.sqlite.SQLiteDatabase, campanhaLocalId: Long
    ): List<LocalValorVariavel> {
        val list = mutableListOf<LocalValorVariavel>()
        db.rawQuery(
            "SELECT $COL_LOCAL_ID, variavel_local_id, variavel_remote_id, valor FROM $TABLE_VALORES_VARIAVEIS WHERE campanha_local_id = ? AND $COL_SINCRONIZADO = 0",
            arrayOf(campanhaLocalId.toString())
        ).use {
            while (it.moveToNext()) {
                list.add(LocalValorVariavel(
                    localId = it.getLong(0),
                    variavelLocalId = it.getLong(1),
                    variavelRemoteId = if (it.isNull(2)) null else it.getInt(2),
                    valor = it.getString(3)
                ))
            }
        }
        return list
    }

    private fun queryVariavelRemoteId(
        db: android.database.sqlite.SQLiteDatabase, variavelLocalId: Long
    ): Int? {
        return db.rawQuery(
            "SELECT $COL_REMOTE_ID FROM $TABLE_VARIAVEIS WHERE $COL_LOCAL_ID = ?",
            arrayOf(variavelLocalId.toString())
        ).use { if (it.moveToFirst() && !it.isNull(0)) it.getInt(0) else null }
    }

    private fun markValoresVariaveisCampanhaSynced(
        db: android.database.sqlite.SQLiteDatabase, campanhaLocalId: Long
    ) {
        val cv = ContentValues().apply { put(COL_SINCRONIZADO, 1) }
        db.update(
            TABLE_VALORES_VARIAVEIS, cv,
            "campanha_local_id = ? AND $COL_SINCRONIZADO = 0",
            arrayOf(campanhaLocalId.toString())
        )
    }

    private fun queryEstudoByLocalId(db: android.database.sqlite.SQLiteDatabase, localId: Long): LocalEstudo {
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, $COL_SINCRONIZADO, nome, observacoes FROM $TABLE_ESTUDOS WHERE $COL_LOCAL_ID = ?",
            arrayOf(localId.toString())
        )
        return cursor.use {
            if (!it.moveToFirst()) error("Estudo localId=$localId não encontrado")
            LocalEstudo(
                localId = it.getLong(0),
                remoteId = if (it.isNull(1)) null else it.getInt(1),
                sincronizado = it.getInt(2),
                nome = it.getString(3),
                observacoes = it.getString(4)
            )
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE DATA CLASSES – local row representations
    // ════════════════════════════════════════════════════════════════════════

    private data class LocalEstudo(val localId: Long, val remoteId: Int?, val sincronizado: Int, val nome: String, val observacoes: String?)
    private data class LocalVariavel(val localId: Long, val remoteId: Int?, val nome: String, val nivelAplicacao: String, val tipoDado: String, val metrica: String?)
    private data class LocalEspecie(val localId: Long, val remoteId: Int?, val foto: String?, val classe: String, val ordem: String, val familia: String, val genero: String, val especie: String, val nomePopular: String?, val statusConservacao: String?, val endemismo: Boolean)
    private data class LocalCampanha(val localId: Long, val remoteId: Int?, val sincronizado: Int, val nome: String, val dataInicio: String, val dataFim: String?, val descricao: String?)
    private data class LocalUnidade(val localId: Long, val remoteId: Int?, val sincronizado: Int, val nome: String, val latitude: Double, val longitude: Double, val raio: Double?, val metodoColeta: String?, val esforcoAmostral: String?)
    private data class LocalEvento(val localId: Long, val remoteId: Int?, val sincronizado: Int, val horarioInicio: String, val horarioFim: String?, val esforcoReal: String?)
    private data class LocalRegistro(val localId: Long, val remoteId: Int?, val sincronizado: Int, val especieLocalId: Long, val data: String, val hora: String, val latitude: Double, val longitude: Double, val qtdeIndividuos: Int?, val foto: String?, val ausenciaEspecie: Boolean?)
    private data class LocalValorVariavel(val localId: Long, val variavelLocalId: Long, val variavelRemoteId: Int?, val valor: String)
}
