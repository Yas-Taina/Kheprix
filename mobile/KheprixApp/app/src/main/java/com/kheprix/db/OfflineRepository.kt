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
import com.kheprix.models.EspeciePatchRequest
import com.kheprix.models.EspecieResponse
import com.kheprix.models.EstudoRequest
import com.kheprix.models.EstudoResponse
import com.kheprix.models.EventoRequest
import com.kheprix.models.EventoResponse
import com.kheprix.models.RegistroPatchRequest
import com.kheprix.models.RegistroRequest
import com.kheprix.models.RegistroResponse
import com.kheprix.models.UnidadeRequest
import com.kheprix.models.UnidadeResponse
import com.kheprix.models.ValorVariavelResponse
import com.kheprix.models.VariavelResponse


class OfflineRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    class OfflineIntegrityException(message: String) : IllegalStateException(message)

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

    fun editarCampanhaOffline(localId: Long, req: CampanhaRequest) {
        val db = dbHelper.writableDatabase
        db.transactionTo {
            val cv = ContentValues().apply {
                put("nome", req.nome)
                put("data_inicio", req.dataInicio)
                put("data_fim", req.dataFim)
                put("descricao", req.descricao)
                put(COL_SINCRONIZADO, 0)
                put(COL_UPDATED_AT, nowIso())
            }
            db.update(TABLE_CAMPANHAS, cv, "$COL_LOCAL_ID = ?", arrayOf(localId.toString()))
            substituirValores(db, "campanha_local_id", localId,
                req.valoresVariaveis?.map { it.variavelId to it.valor })
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
                val variavelLocalId: Long?
                val variavelRemoteId: Int?
                if (vv.variavelId < 0) {
                    variavelLocalId = (-vv.variavelId).toLong()
                    variavelRemoteId = null
                } else {
                    variavelLocalId = lookupLocalId(
                        TABLE_VARIAVEIS, vv.variavelId, "estudo_local_id", estudoLocalId
                    )
                    variavelRemoteId = vv.variavelId
                }
                if (variavelLocalId != null) {
                    db.insertOrThrow(TABLE_VALORES_VARIAVEIS, null, ContentValues().apply {
                        put(COL_SINCRONIZADO, 0)
                        put("campanha_local_id", campanhaId)
                        put("variavel_local_id", variavelLocalId)
                        if (variavelRemoteId != null) put("variavel_remote_id", variavelRemoteId)
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
        return db.transactionTo {
            val id = db.insertOrThrow(TABLE_UNIDADES, null, ContentValues().apply {
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
            salvarValores(db, "unidade_local_id", id,
                req.valoresVariaveis?.map { it.variavelId to it.valor })
            id
        }
    }

    fun editarUnidadeOffline(localId: Long, req: UnidadeRequest) {
        val db = dbHelper.writableDatabase
        db.transactionTo {
            val cv = ContentValues().apply {
                put("nome", req.nome)
                put("latitude", req.latitude)
                put("longitude", req.longitude)
                put("raio", req.raio)
                put("metodo_coleta", req.metodoColeta)
                put("esforco_amostral", req.esforcoAmostral)
                put(COL_SINCRONIZADO, 0)
                put(COL_UPDATED_AT, nowIso())
            }
            db.update(TABLE_UNIDADES, cv, "$COL_LOCAL_ID = ?", arrayOf(localId.toString()))
            substituirValores(db, "unidade_local_id", localId,
                req.valoresVariaveis?.map { it.variavelId to it.valor })
        }
    }

    fun criarEventoOffline(unidadeLocalId: Long, req: EventoRequest): Long {
        requireUnidadeExists(unidadeLocalId)
        val db = dbHelper.writableDatabase
        return db.transactionTo {
            val id = db.insertOrThrow(TABLE_EVENTOS, null, ContentValues().apply {
                put(COL_SINCRONIZADO, 0)
                put("unidade_local_id", unidadeLocalId)
                put("horario_inicio", req.horarioInicio)
                put("horario_fim", req.horarioFim)
                put("esforco_real", req.esforcoReal)
                put(COL_CREATED_AT, nowIso())
            })
            salvarValores(db, "evento_local_id", id,
                req.valoresVariaveis?.map { it.variavelId to it.valor })
            id
        }
    }

    fun editarEventoOffline(localId: Long, req: EventoRequest) {
        val db = dbHelper.writableDatabase
        db.transactionTo {
            val cv = ContentValues().apply {
                put("horario_inicio", req.horarioInicio)
                put("horario_fim", req.horarioFim)
                put("esforco_real", req.esforcoReal)
                put(COL_SINCRONIZADO, 0)
            }
            db.update(TABLE_EVENTOS, cv, "$COL_LOCAL_ID = ?", arrayOf(localId.toString()))
            substituirValores(db, "evento_local_id", localId,
                req.valoresVariaveis?.map { it.variavelId to it.valor })
        }
    }

    fun editarRegistroOffline(localId: Long, req: RegistroPatchRequest, especieLocalId: Long? = null) {
        val db = dbHelper.writableDatabase
        db.transactionTo {
            val cv = ContentValues().apply {
                req.especieId?.let { put("especie_remote_id", it) }
                especieLocalId?.let { put("especie_local_id", it) }
                req.data?.let { put("data", it) }
                req.hora?.let { put("hora", it) }
                req.latitude?.let { put("latitude", it) }
                req.longitude?.let { put("longitude", it) }
                req.qtdeIndividuos?.let { put("qtde_individuos", it) }
                req.ausenciaEspecie?.let { put("ausencia_especie", if (it) 1 else 0) }
                if (!req.foto.isNullOrBlank()) put("foto", req.foto)
                put(COL_SINCRONIZADO, 0)
            }
            db.update(TABLE_REGISTROS, cv, "$COL_LOCAL_ID = ?", arrayOf(localId.toString()))
            substituirValores(db, "registro_local_id", localId,
                req.valoresVariaveis?.map { it.variavelId to it.valor })
        }
    }

    fun cacheRegistro(eventoLocalId: Long, especieLocalId: Long, r: RegistroResponse): Long {
        val db = dbHelper.writableDatabase
        val existing = findLocalIdByRemote(db, TABLE_REGISTROS, r.id, "evento_local_id", eventoLocalId)
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
            put(COL_CREATED_AT, r.createdAt)
        }
        return db.transactionTo {
            val localId = if (existing != null) {
                db.update(TABLE_REGISTROS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
                existing
            } else {
                db.insertOrThrow(TABLE_REGISTROS, null, cv)
            }
            substituirValores(db, "registro_local_id", localId,
                r.valoresVariaveis?.map { it.variavelId to it.valor }, sincronizado = 1)
            localId
        }
    }

    fun criarRegistroOffline(
        eventoLocalId: Long,
        especieLocalId: Long,
        req: RegistroRequest
    ): Long {
        requireEventoExists(eventoLocalId)
        requireEspecieExists(especieLocalId)
        val db = dbHelper.writableDatabase
        return db.transactionTo {
            val id = db.insertOrThrow(TABLE_REGISTROS, null, ContentValues().apply {
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
            salvarValores(db, "registro_local_id", id,
                req.valoresVariaveis?.map { it.variavelId to it.valor })
            id
        }
    }

    fun editarEspecieOffline(localId: Long, req: EspeciePatchRequest) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            req.classe?.let { put("classe", it) }
            req.ordem?.let { put("ordem", it) }
            req.familia?.let { put("familia", it) }
            req.genero?.let { put("genero", it) }
            req.especie?.let { put("especie", it) }
            req.endemismo?.let { put("endemismo", if (it) 1 else 0) }
            req.nomePopular?.let { put("nome_popular", it) }
            req.statusConservacao?.let { put("status_conservacao", it) }
            if (!req.foto.isNullOrBlank()) put("foto", req.foto)
            put(COL_SINCRONIZADO, 0)
        }
        db.update(TABLE_ESPECIES, cv, "$COL_LOCAL_ID = ?", arrayOf(localId.toString()))
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

    fun cacheVariaveis(estudoLocalId: Long, variaveis: List<VariavelResponse>) {
        val db = dbHelper.writableDatabase
        db.transactionTo {
            variaveis.forEach { v ->
                val existing = db.rawQuery(
                    "SELECT $COL_LOCAL_ID FROM $TABLE_VARIAVEIS WHERE $COL_REMOTE_ID = ? AND estudo_local_id = ? LIMIT 1",
                    arrayOf(v.id.toString(), estudoLocalId.toString())
                ).use { if (it.moveToFirst()) it.getLong(0) else null }
                val cv = ContentValues().apply {
                    put(COL_REMOTE_ID, v.id)
                    put(COL_SINCRONIZADO, 1)
                    put("estudo_local_id", estudoLocalId)
                    put("nome", v.nome)
                    put("nivel_aplicacao", v.nivelAplicacao)
                    put("tipo_dado", v.tipoDado)
                    put("metrica", v.metrica)
                    put(COL_CREATED_AT, v.createdAt)
                    put(COL_UPDATED_AT, v.updatedAt)
                }
                if (existing != null) {
                    db.update(TABLE_VARIAVEIS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
                } else {
                    db.insertOrThrow(TABLE_VARIAVEIS, null, cv)
                }
            }
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
        return db.transactionTo {
            val localId = if (existing != null) {
                db.update(TABLE_CAMPANHAS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
                existing
            } else {
                db.insertOrThrow(TABLE_CAMPANHAS, null, cv)
            }
            if (!c.valoresVariaveis.isNullOrEmpty()) {
                substituirValores(db, "campanha_local_id", localId,
                    c.valoresVariaveis.map { it.variavelId to it.valor }, sincronizado = 1)
            }
            localId
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
        return db.transactionTo {
            val localId = if (existing != null) {
                db.update(TABLE_UNIDADES, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
                existing
            } else {
                db.insertOrThrow(TABLE_UNIDADES, null, cv)
            }
            substituirValores(db, "unidade_local_id", localId,
                u.valoresVariaveis?.map { it.variavelId to it.valor }, sincronizado = 1)
            localId
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
        return db.transactionTo {
            val localId = if (existing != null) {
                db.update(TABLE_EVENTOS, cv, "$COL_LOCAL_ID = ?", arrayOf(existing.toString()))
                existing
            } else {
                db.insertOrThrow(TABLE_EVENTOS, null, cv)
            }
            substituirValores(db, "evento_local_id", localId,
                e.valoresVariaveis?.map { it.variavelId to it.valor }, sincronizado = 1)
            localId
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

    fun listarValoresPorCampanha(campanhaLocalId: Long): List<ValorVariavelResponse> =
        listarValores("campanha_local_id", campanhaLocalId)

    fun listarValoresPorUnidade(unidadeLocalId: Long): List<ValorVariavelResponse> =
        listarValores("unidade_local_id", unidadeLocalId)

    fun listarValoresPorEvento(eventoLocalId: Long): List<ValorVariavelResponse> =
        listarValores("evento_local_id", eventoLocalId)

    fun listarValoresPorRegistro(registroLocalId: Long): List<ValorVariavelResponse> =
        listarValores("registro_local_id", registroLocalId)

    private fun listarValores(parentCol: String, parentLocalId: Long): List<ValorVariavelResponse> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<ValorVariavelResponse>()
        db.rawQuery(
            "SELECT variavel_remote_id, variavel_local_id, valor FROM $TABLE_VALORES_VARIAVEIS WHERE $parentCol = ?",
            arrayOf(parentLocalId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val remoteId = if (c.isNull(0)) null else c.getInt(0)
                val localId  = c.getLong(1)
                list.add(ValorVariavelResponse(
                    variavelId = remoteId ?: -localId.toInt(),
                    valor = c.getString(2)
                ))
            }
        }
        return list
    }

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

    private fun salvarValores(
        db: SQLiteDatabase,
        parentCol: String,
        parentLocalId: Long,
        pares: List<Pair<Int, String>>?,
        sincronizado: Int = 0
    ) {
        pares?.forEach { (variavelId, valor) ->
            // variavelId < 0 means it is an offline-only variable whose local_id is -variavelId
            val variavelLocalId: Long?
            val variavelRemoteId: Int?
            if (variavelId < 0) {
                variavelLocalId = (-variavelId).toLong()
                variavelRemoteId = null
            } else {
                variavelLocalId = db.rawQuery(
                    "SELECT $COL_LOCAL_ID FROM $TABLE_VARIAVEIS WHERE $COL_REMOTE_ID = ? LIMIT 1",
                    arrayOf(variavelId.toString())
                ).use { if (it.moveToFirst()) it.getLong(0) else null }
                variavelRemoteId = variavelId
            }
            if (variavelLocalId != null) {
                db.insertOrThrow(TABLE_VALORES_VARIAVEIS, null, ContentValues().apply {
                    put(COL_SINCRONIZADO, sincronizado)
                    put(parentCol, parentLocalId)
                    put("variavel_local_id", variavelLocalId)
                    if (variavelRemoteId != null) put("variavel_remote_id", variavelRemoteId)
                    put("valor", valor)
                })
            }
        }
    }

    private fun substituirValores(
        db: SQLiteDatabase,
        parentCol: String,
        parentLocalId: Long,
        pares: List<Pair<Int, String>>?,
        sincronizado: Int = 0
    ) {
        db.delete(TABLE_VALORES_VARIAVEIS, "$parentCol = ?", arrayOf(parentLocalId.toString()))
        salvarValores(db, parentCol, parentLocalId, pares, sincronizado)
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
