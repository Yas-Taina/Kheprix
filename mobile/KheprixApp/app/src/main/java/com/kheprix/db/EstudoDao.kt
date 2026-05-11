package com.kheprix.db

import android.content.Context
import com.kheprix.db.DatabaseHelper.Companion.COL_LOCAL_ID
import com.kheprix.db.DatabaseHelper.Companion.COL_REMOTE_ID
import com.kheprix.db.DatabaseHelper.Companion.TABLE_ESTUDOS

class EstudoDao(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    data class EstudoOffline(
        val localId: Long,
        val remoteId: Int?,
        val nome: String,
        val observacoes: String?,
        val perfil: String?,
        val sincronizado: Int,
        val createdAt: String?,
        val updatedAt: String?
    )

    fun listarTodos(): List<EstudoOffline> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, nome, observacoes, perfil, sincronizado, created_at, updated_at FROM $TABLE_ESTUDOS ORDER BY created_at DESC",
            null
        )
        return cursor.use { c ->
            val list = mutableListOf<EstudoOffline>()
            while (c.moveToNext()) {
                list.add(EstudoOffline(
                    localId = c.getLong(0),
                    remoteId = if (c.isNull(1)) null else c.getInt(1),
                    nome = c.getString(2),
                    observacoes = c.getString(3),
                    perfil = c.getString(4),
                    sincronizado = c.getInt(5),
                    createdAt = c.getString(6),
                    updatedAt = c.getString(7)
                ))
            }
            list
        }
    }

    fun buscarPorRemoteId(remoteId: Int): EstudoOffline? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, nome, observacoes, perfil, sincronizado, created_at, updated_at FROM $TABLE_ESTUDOS WHERE $COL_REMOTE_ID = ? LIMIT 1",
            arrayOf(remoteId.toString())
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                EstudoOffline(
                    localId = c.getLong(0),
                    remoteId = if (c.isNull(1)) null else c.getInt(1),
                    nome = c.getString(2),
                    observacoes = c.getString(3),
                    perfil = c.getString(4),
                    sincronizado = c.getInt(5),
                    createdAt = c.getString(6),
                    updatedAt = c.getString(7)
                )
            } else null
        }
    }

    fun buscarPorLocalId(localId: Long): EstudoOffline? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, nome, observacoes, perfil, sincronizado, created_at, updated_at FROM $TABLE_ESTUDOS WHERE $COL_LOCAL_ID = ? LIMIT 1",
            arrayOf(localId.toString())
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                EstudoOffline(
                    localId = c.getLong(0),
                    remoteId = if (c.isNull(1)) null else c.getInt(1),
                    nome = c.getString(2),
                    observacoes = c.getString(3),
                    perfil = c.getString(4),
                    sincronizado = c.getInt(5),
                    createdAt = c.getString(6),
                    updatedAt = c.getString(7)
                )
            } else null
        }
    }
}
