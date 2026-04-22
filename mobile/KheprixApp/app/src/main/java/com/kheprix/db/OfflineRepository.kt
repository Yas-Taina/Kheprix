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
import com.kheprix.models.EstudoRequest
import com.kheprix.models.EventoRequest
import com.kheprix.models.RegistroRequest
import com.kheprix.models.UnidadeRequest

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
