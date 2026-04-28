package com.kheprix.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.kheprix.db.DatabaseHelper.Companion.COL_CREATED_AT
import com.kheprix.db.DatabaseHelper.Companion.COL_LOCAL_ID
import com.kheprix.db.DatabaseHelper.Companion.COL_REMOTE_ID
import com.kheprix.db.DatabaseHelper.Companion.COL_SINCRONIZADO
import com.kheprix.db.DatabaseHelper.Companion.COL_UPDATED_AT
import com.kheprix.db.DatabaseHelper.Companion.TABLE_CAMPANHAS
import com.kheprix.db.DatabaseHelper.Companion.TABLE_ESPECIES
import com.kheprix.db.DatabaseHelper.Companion.TABLE_ESTUDOS
import com.kheprix.db.DatabaseHelper.Companion.TABLE_EVENTOS
import com.kheprix.db.DatabaseHelper.Companion.TABLE_REGISTROS
import com.kheprix.db.DatabaseHelper.Companion.TABLE_UNIDADES
import com.kheprix.db.DatabaseHelper.Companion.TABLE_VALORES_VARIAVEIS
import com.kheprix.db.DatabaseHelper.Companion.TABLE_VARIAVEIS
import com.kheprix.models.CampanhaRequest
import com.kheprix.models.CampanhaResponse
import com.kheprix.models.EspecieRequest
import com.kheprix.models.EspecieResponse
import com.kheprix.models.EstudoRequest
import com.kheprix.models.EstudoResponse
import com.kheprix.models.EventoRequest
import com.kheprix.models.EventoResponse
import com.kheprix.models.RegistroRequest
import com.kheprix.models.UnidadeRequest
import com.kheprix.models.UnidadeResponse

/**
 * Fonte única de persistência offline com validação de integridade.
 *
 * Invariantes:
 *  - Nada é inserido sem que o pai exista localmente (FK lógica obrigatória).
 *  - Linhas criadas offline têm remote_id = NULL e sincronizado = 0.
 *  - Unicidade de remote_id é garantida no schema via índice UNIQUE parcial
 *    (ver DatabaseHelper.criarIndicesUnicidade).
 *
 * Todas as funções criarXOffline lançam [OfflineIntegrityException] se o pai
 * informado não existir no SQLite — impede rastros órfãos.
 */
class OfflineRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    class OfflineIntegrityException(message: String) : IllegalStateException(message)

    // ════════════════════════════════════════════════════════════════════════
    // RESOLUÇÃO remote_id → local_id
    // ════════════════════════════════════════════════════════════════════════

    fun estudoLocalIdFromRemote(remoteId: Int): Long? =
        lookupLocalId(TABLE_ESTUDOS, remoteId, parentCol = null, parentLocalId = null)

    fun campanhaLocalIdFromRemote(estudoLocalId: Long, remoteId: Int): Long? =
        lookupLocalId(TABLE_CAMPANHAS, remoteId, "estudo_local_id", estudoLocalId)

    fun unidadeLocalIdFromRemote(campanhaLocalId: Long, remoteId: Int): Long? =
        lookupLocalId(TABLE_UNIDADES, remoteId, "campanha_local_id", campanhaLocalId)

    fun eventoLocalIdFromRemote(unidadeLocalId: Long, remoteId: Int): Long? =
        lookupLocalId(TABLE_EVENTOS, remoteId, "unidade_local_id", unidadeLocalId)

    fun especieLocalIdFromRemote(estudoLocalId: Long, remoteId: Int): Long? =
        lookupLocalId(TABLE_ESPECIES, remoteId, "estudo_local_id", estudoLocalId)

    // ════════════════════════════════════════════════════════════════════════
    // VALIDAÇÃO DE EXISTÊNCIA (lança OfflineIntegrityException se faltar)
    // ════════════════════════════════════════════════════════════════════════

    fun requireEstudoExists(localId: Long) =
        requireRowExists(TABLE_ESTUDOS, localId, "Estudo")

    fun requireCampanhaExists(localId: Long) =
        requireRowExists(TABLE_CAMPANHAS, localId, "Campanha")

    fun requireUnidadeExists(localId: Long) =
        requireRowExists(TABLE_UNIDADES, localId, "Unidade amostral")

    fun requireEventoExists(localId: Long) =
        requireRowExists(TABLE_EVENTOS, localId, "Evento de amostragem")

    fun requireEspecieExists(localId: Long) =
        requireRowExists(TABLE_ESPECIES, localId, "Espécie")

    // ════════════════════════════════════════════════════════════════════════
    // CRIAÇÃO OFFLINE — remote_id = NULL, sincronizado = 0
    // ════════════════════════════════════════════════════════════════════════

    fun criarEstudoOffline(req: EstudoRequest): Long {
        val db = dbHelper.writableDatabase
        val now = nowIso()
        return db.transactionTo {
            val estudoId = db.insertOrThrow(TABLE_ESTUDOS, null, ContentValues().apply {
                put(COL_SINCRONIZADO, 0)
                put("nome", req.nome)
                put("observacoes", req.observacoes)
                put(COL_CREATED_AT, now)
                put(COL_UPDATED_AT, now)
            })
            req.variaveis.forEach { v ->
                db.insertOrThrow(TABLE_VARIAVEIS, null, ContentValues().apply {
                    put(COL_SINCRONIZADO, 0)
                    put("estudo_local_id", estudoId)
                    put("nome", v.nome)
                    put("nivel_aplicacao", v.nivelAplicacao)
                    put("tipo_dado", v.tipoDado)
                    put("metrica", v.metrica)
                    put(COL_CREATED_AT, now)
                })
            }
            estudoId
        }
    }

    fun criarCampanhaOffline(estudoLocalId: Long, req: CampanhaRequest): Long {
        requireEstudoExists(estudoLocalId)
        val db = dbHelper.writableDatabase
        val now = nowIso()
        return db.transactionTo {
            val campanhaId = db.insertOrThrow(TABLE_CAMPANHAS, null, ContentValues().apply {
                put(COL_SINCRONIZADO, 0)
                put("estudo_local_id", estudoLocalId)
                put("nome", req.nome)
                put("data_inicio", req.dataInicio)
                put("data_fim", req.dataFim)
                put("descricao", req.descricao)
                put(COL_CREATED_AT, now)
                put(COL_UPDATED_AT, now)
            })
            req.valoresVariaveis?.forEach { vv ->
                val variavelLocalId = lookupLocalId(
                    TABLE_VARIAVEIS, vv.variavelId,
                    "estudo_local_id", estudoLocalId
                )
                if (variavelLocalId != null) {
                    db.insertOrThrow(TABLE_VALORES_VARIAVEIS, null, ContentValues().apply {
                        put(COL_SINCRONIZADO, 0)
                        put("campanha_local_id", campanhaId)
                        put("variavel_local_id", variavelLocalId)
                        put("variavel_remote_id", vv.variavelId)
                        put("valor", vv.valor)
                    })
                }
            }
            campanhaId
        }
    }

    fun criarUnidadeOffline(campanhaLocalId: Long, req: UnidadeRequest): Long {
        requireCampanhaExists(campanhaLocalId)
        val db = dbHelper.writableDatabase
        val now = nowIso()
        return db.insertOrThrow(TABLE_UNIDADES, null, ContentValues().apply {
            put(COL_SINCRONIZADO, 0)
            put("campanha_local_id", campanhaLocalId)
            put("nome", req.nome)
            put("latitude", req.latitude)
            put("longitude", req.longitude)
            put("raio", req.raio)
            put("metodo_coleta", req.metodoColeta)
            put("esforco_amostral", req.esforcoAmostral)
            put(COL_CREATED_AT, now)
            put(COL_UPDATED_AT, now)
        })
    }

    fun criarEventoOffline(unidadeLocalId: Long, req: EventoRequest): Long {
        requireUnidadeExists(unidadeLocalId)
        val db = dbHelper.writableDatabase
        return db.insertOrThrow(TABLE_EVENTOS, null, ContentValues().apply {
            put(COL_SINCRONIZADO, 0)
            put("unidade_local_id", unidadeLocalId)
            put("horario_inicio", req.horarioInicio)
            put("horario_fim", req.horarioFim)
            put("esforco_real", req.esforcoReal)
            put(COL_CREATED_AT, nowIso())
        })
    }

    fun criarRegistroOffline(
        eventoLocalId: Long,
        especieLocalId: Long,
        req: RegistroRequest
    ): Long {
        requireEventoExists(eventoLocalId)
        requireEspecieExists(especieLocalId)
        val db = dbHelper.writableDatabase
        return db.insertOrThrow(TABLE_REGISTROS, null, ContentValues().apply {
            put(COL_SINCRONIZADO, 0)
            put("evento_local_id", eventoLocalId)
            put("especie_local_id", especieLocalId)
            put("especie_remote_id", req.especieId)
            put("data", req.data)
            put("hora", req.hora)
            put("latitude", req.latitude)
            put("longitude", req.longitude)
            put("qtde_individuos", req.qtdeIndividuos)
            put("foto", req.foto)
            put("ausencia_especie", req.ausenciaEspecie?.let { if (it) 1 else 0 })
            put(COL_CREATED_AT, nowIso())
        })
    }

    fun criarEspecieOffline(estudoLocalId: Long, req: EspecieRequest): Long {
        requireEstudoExists(estudoLocalId)
        val db = dbHelper.writableDatabase
        return db.insertOrThrow(TABLE_ESPECIES, null, ContentValues().apply {
            put(COL_SINCRONIZADO, 0)
            put("estudo_local_id", estudoLocalId)
            put("classe", req.classe)
            put("ordem", req.ordem)
            put("familia", req.familia)
            put("genero", req.genero)
            put("especie", req.especie)
            put("nome_popular", req.nomePopular)
            put("status_conservacao", req.statusConservacao)
            put("endemismo", if (req.endemismo) 1 else 0)
            put("foto", req.foto)
            put(COL_CREATED_AT, nowIso())
        })
    }

    // ════════════════════════════════════════════════════════════════════════
    // CACHE DE DADOS ONLINE — upsert com sincronizado = 1
    // Usado para espelhar dados recém-carregados da API no SQLite, permitindo
    // acesso offline sem exigir o fluxo explícito de "Salvar Offline".
    // ════════════════════════════════════════════════════════════════════════

    fun cacheEstudo(e: EstudoResponse): Long {
        val db = dbHelper.writableDatabase
        val existing = findLocalIdByRemote(db, TABLE_ESTUDOS, e.id, null, null)
        val cv = ContentValues().apply {
            put(COL_REMOTE_ID, e.id)
            put(COL_SINCRONIZADO, 1)
            put("nome", e.nome)
            put("observacoes", e.observacoes)
            put("perfil", e.perfil)
            put(COL_CREATED_AT, e.createdAt)
            put(COL_UPDATED_AT, e.updatedAt)
        }
        return if (existing != null) {
            db.update(TABLE_ESTUDOS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insertOrThrow(TABLE_ESTUDOS, null, cv)
        }
    }

    fun cacheCampanha(estudoLocalId: Long, c: CampanhaResponse): Long {
        val db = dbHelper.writableDatabase
        val existing = findLocalIdByRemote(db, TABLE_CAMPANHAS, c.id, "estudo_local_id", estudoLocalId)
        val cv = ContentValues().apply {
            put(COL_REMOTE_ID, c.id)
            put(COL_SINCRONIZADO, 1)
            put("estudo_local_id", estudoLocalId)
            put("nome", c.nome)
            put("data_inicio", c.dataInicio)
            put("data_fim", c.dataFim)
            put("descricao", c.descricao)
            put(COL_CREATED_AT, c.createdAt)
            put(COL_UPDATED_AT, c.updatedAt)
        }
        return if (existing != null) {
            db.update(TABLE_CAMPANHAS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insertOrThrow(TABLE_CAMPANHAS, null, cv)
        }
    }

    fun cacheUnidade(campanhaLocalId: Long, u: UnidadeResponse): Long {
        val db = dbHelper.writableDatabase
        val existing = findLocalIdByRemote(db, TABLE_UNIDADES, u.id, "campanha_local_id", campanhaLocalId)
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
            put(COL_CREATED_AT, u.createdAt)
            put(COL_UPDATED_AT, u.updatedAt)
        }
        return if (existing != null) {
            db.update(TABLE_UNIDADES, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insertOrThrow(TABLE_UNIDADES, null, cv)
        }
    }

    fun cacheEvento(unidadeLocalId: Long, e: EventoResponse): Long {
        val db = dbHelper.writableDatabase
        val existing = findLocalIdByRemote(db, TABLE_EVENTOS, e.id, "unidade_local_id", unidadeLocalId)
        val cv = ContentValues().apply {
            put(COL_REMOTE_ID, e.id)
            put(COL_SINCRONIZADO, 1)
            put("unidade_local_id", unidadeLocalId)
            put("horario_inicio", e.horarioInicio)
            put("horario_fim", e.horarioFim)
            put("esforco_real", e.esforcoReal)
            put(COL_CREATED_AT, e.createdAt)
        }
        return if (existing != null) {
            db.update(TABLE_EVENTOS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insertOrThrow(TABLE_EVENTOS, null, cv)
        }
    }

    fun cacheEspecie(estudoLocalId: Long, e: EspecieResponse): Long {
        val db = dbHelper.writableDatabase
        val existing = findLocalIdByRemote(db, TABLE_ESPECIES, e.id, "estudo_local_id", estudoLocalId)
        val cv = ContentValues().apply {
            put(COL_REMOTE_ID, e.id)
            put(COL_SINCRONIZADO, 1)
            put("estudo_local_id", estudoLocalId)
            put("classe", e.classe)
            put("ordem", e.ordem)
            put("familia", e.familia)
            put("genero", e.genero)
            put("especie", e.especie)
            put("nome_popular", e.nomePopular)
            put("status_conservacao", e.statusConservacao)
            put("endemismo", if (e.endemismo) 1 else 0)
            put("foto", e.foto)
            put(COL_CREATED_AT, e.createdAt)
        }
        return if (existing != null) {
            db.update(TABLE_ESPECIES, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
            existing
        } else {
            db.insertOrThrow(TABLE_ESPECIES, null, cv)
        }
    }

    /**
     * Lista espécies do SQLite como EspecieResponse. Útil para UIs que
     * precisam mostrar espécies offline (cacheadas ou criadas localmente).
     *
     * Para distinguir espécies criadas offline (ainda não sincronizadas),
     * o campo `id` é preenchido com `-localId` (negativo). Espécies com
     * remote_id têm o próprio remote_id como `id` (positivo).
     */
    fun listarEspeciesPorEstudoLocal(estudoLocalId: Long): List<EspecieResponse> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<EspecieResponse>()
        db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, estudo_local_id, foto, classe, ordem, familia, genero, especie, nome_popular, status_conservacao, endemismo, $COL_CREATED_AT FROM $TABLE_ESPECIES WHERE estudo_local_id = ? ORDER BY $COL_CREATED_AT DESC",
            arrayOf(estudoLocalId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val localId = c.getLong(0)
                val remoteId = if (c.isNull(1)) null else c.getInt(1)
                list.add(EspecieResponse(
                    id = remoteId ?: -localId.toInt(),
                    estudoId = c.getLong(2).toInt(),
                    foto = c.getString(3),
                    classe = c.getString(4),
                    ordem = c.getString(5),
                    familia = c.getString(6),
                    genero = c.getString(7),
                    especie = c.getString(8),
                    nomePopular = c.getString(9),
                    statusConservacao = c.getString(10),
                    endemismo = c.getInt(11) == 1,
                    createdAt = c.getString(12) ?: ""
                ))
            }
        }
        return list
    }

    /**
     * Resolve o local_id de uma espécie a partir do id usado na UI:
     * - id > 0: é o remote_id → busca normal.
     * - id < 0: é `-localId` (espécie criada offline) → retorna abs(id).
     */
    fun resolveEspecieLocalId(estudoLocalId: Long, id: Int): Long? {
        if (id < 0) {
            val localId = (-id).toLong()
            val db = dbHelper.readableDatabase
            val existe = db.rawQuery(
                "SELECT 1 FROM $TABLE_ESPECIES WHERE $COL_LOCAL_ID = ? AND estudo_local_id = ? LIMIT 1",
                arrayOf(localId.toString(), estudoLocalId.toString())
            ).use { it.moveToFirst() }
            return if (existe) localId else null
        }
        return especieLocalIdFromRemote(estudoLocalId, id)
    }

    /**
     * Resolve o local_id de um evento a partir do id usado na UI:
     * - id > 0: é o remote_id → busca normal.
     * - id < 0: é `-localId` (evento criado offline) → retorna abs(id).
     */
    fun resolveEventoLocalId(unidadeLocalId: Long, id: Int): Long? {
        if (id < 0) {
            val localId = (-id).toLong()
            val db = dbHelper.readableDatabase
            val existe = db.rawQuery(
                "SELECT 1 FROM $TABLE_EVENTOS WHERE $COL_LOCAL_ID = ? AND unidade_local_id = ? LIMIT 1",
                arrayOf(localId.toString(), unidadeLocalId.toString())
            ).use { it.moveToFirst() }
            return if (existe) localId else null
        }
        return eventoLocalIdFromRemote(unidadeLocalId, id)
    }

    private fun findLocalIdByRemote(
        db: SQLiteDatabase, table: String, remoteId: Int,
        parentCol: String?, parentLocalId: Long?
    ): Long? {
        val (where, args) = if (parentCol != null && parentLocalId != null) {
            "$COL_REMOTE_ID = ? AND $parentCol = ?" to
                    arrayOf(remoteId.toString(), parentLocalId.toString())
        } else {
            "$COL_REMOTE_ID = ?" to arrayOf(remoteId.toString())
        }
        return db.rawQuery(
            "SELECT $COL_LOCAL_ID FROM $table WHERE $where LIMIT 1",
            args
        ).use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ════════════════════════════════════════════════════════════════════════

    private fun lookupLocalId(
        table: String,
        remoteId: Int,
        parentCol: String?,
        parentLocalId: Long?
    ): Long? {
        val db = dbHelper.readableDatabase
        val (where, args) = if (parentCol != null && parentLocalId != null) {
            "$COL_REMOTE_ID = ? AND $parentCol = ?" to
                    arrayOf(remoteId.toString(), parentLocalId.toString())
        } else {
            "$COL_REMOTE_ID = ?" to arrayOf(remoteId.toString())
        }
        return db.rawQuery(
            "SELECT $COL_LOCAL_ID FROM $table WHERE $where LIMIT 1",
            args
        ).use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    private fun requireRowExists(table: String, localId: Long, rotulo: String) {
        val db = dbHelper.readableDatabase
        val existe = db.rawQuery(
            "SELECT 1 FROM $table WHERE $COL_LOCAL_ID = ? LIMIT 1",
            arrayOf(localId.toString())
        ).use { it.moveToFirst() }
        if (!existe) {
            throw OfflineIntegrityException(
                "$rotulo (localId=$localId) não está salvo offline."
            )
        }
    }

    private fun nowIso(): String = java.time.Instant.now().toString()

    private inline fun <T> SQLiteDatabase.transactionTo(block: () -> T): T {
        beginTransaction()
        return try {
            val result = block()
            setTransactionSuccessful()
            result
        } finally {
            endTransaction()
        }
    }
}
