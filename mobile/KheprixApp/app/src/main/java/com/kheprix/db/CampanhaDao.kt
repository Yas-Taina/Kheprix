package com.kheprix.db

import android.content.Context
import com.kheprix.db.DatabaseHelper.Companion.COL_LOCAL_ID
import com.kheprix.db.DatabaseHelper.Companion.COL_REMOTE_ID
import com.kheprix.db.DatabaseHelper.Companion.TABLE_CAMPANHAS

class CampanhaDao(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    data class CampanhaOffline(
        val localId: Long,
        val remoteId: Int?,
        val estudoLocalId: Long,
        val nome: String,
        val dataInicio: String,
        val dataFim: String?,
        val descricao: String?,
        val sincronizado: Int,
        val createdAt: String?,
        val updatedAt: String?
    )

    fun listarPorEstudoLocal(estudoLocalId: Long): List<CampanhaOffline> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, estudo_local_id, nome, data_inicio, data_fim, descricao, sincronizado, created_at, updated_at FROM $TABLE_CAMPANHAS WHERE estudo_local_id = ? ORDER BY created_at DESC",
            arrayOf(estudoLocalId.toString())
        )
        return cursor.use { c ->
            val list = mutableListOf<CampanhaOffline>()
            while (c.moveToNext()) {
                list.add(CampanhaOffline(
                    localId = c.getLong(0),
                    remoteId = if (c.isNull(1)) null else c.getInt(1),
                    estudoLocalId = c.getLong(2),
                    nome = c.getString(3),
                    dataInicio = c.getString(4),
                    dataFim = c.getString(5),
                    descricao = c.getString(6),
                    sincronizado = c.getInt(7),
                    createdAt = c.getString(8),
                    updatedAt = c.getString(9)
                ))
            }
            list
        }
    }

    fun listarTodos(): List<CampanhaOffline> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, estudo_local_id, nome, data_inicio, data_fim, descricao, sincronizado, created_at, updated_at FROM $TABLE_CAMPANHAS ORDER BY created_at DESC",
            null
        )
        return cursor.use { c ->
            val list = mutableListOf<CampanhaOffline>()
            while (c.moveToNext()) {
                list.add(CampanhaOffline(
                    localId = c.getLong(0),
                    remoteId = if (c.isNull(1)) null else c.getInt(1),
                    estudoLocalId = c.getLong(2),
                    nome = c.getString(3),
                    dataInicio = c.getString(4),
                    dataFim = c.getString(5),
                    descricao = c.getString(6),
                    sincronizado = c.getInt(7),
                    createdAt = c.getString(8),
                    updatedAt = c.getString(9)
                ))
            }
            list
        }
    }

    fun buscarPorRemoteIdEscopo(remoteId: Int, estudoLocalId: Long): CampanhaOffline? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, estudo_local_id, nome, data_inicio, data_fim, descricao, sincronizado, created_at, updated_at FROM $TABLE_CAMPANHAS WHERE $COL_REMOTE_ID = ? AND estudo_local_id = ? LIMIT 1",
            arrayOf(remoteId.toString(), estudoLocalId.toString())
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                CampanhaOffline(
                    localId = c.getLong(0),
                    remoteId = if (c.isNull(1)) null else c.getInt(1),
                    estudoLocalId = c.getLong(2),
                    nome = c.getString(3),
                    dataInicio = c.getString(4),
                    dataFim = c.getString(5),
                    descricao = c.getString(6),
                    sincronizado = c.getInt(7),
                    createdAt = c.getString(8),
                    updatedAt = c.getString(9)
                )
            } else null
        }
    }

    fun buscarPorLocalId(localId: Long): CampanhaOffline? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, estudo_local_id, nome, data_inicio, data_fim, descricao, sincronizado, created_at, updated_at FROM $TABLE_CAMPANHAS WHERE $COL_LOCAL_ID = ? LIMIT 1",
            arrayOf(localId.toString())
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                CampanhaOffline(
                    localId = c.getLong(0),
                    remoteId = if (c.isNull(1)) null else c.getInt(1),
                    estudoLocalId = c.getLong(2),
                    nome = c.getString(3),
                    dataInicio = c.getString(4),
                    dataFim = c.getString(5),
                    descricao = c.getString(6),
                    sincronizado = c.getInt(7),
                    createdAt = c.getString(8),
                    updatedAt = c.getString(9)
                )
            } else null
        }
    }
}
