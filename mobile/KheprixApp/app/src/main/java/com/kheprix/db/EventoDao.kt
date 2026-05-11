package com.kheprix.db

import android.content.Context
import com.kheprix.db.DatabaseHelper.Companion.COL_LOCAL_ID
import com.kheprix.db.DatabaseHelper.Companion.COL_REMOTE_ID
import com.kheprix.db.DatabaseHelper.Companion.TABLE_EVENTOS

class EventoDao(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    data class EventoOffline(
        val localId: Long,
        val remoteId: Int?,
        val unidadeLocalId: Long,
        val horarioInicio: String,
        val horarioFim: String?,
        val esforcoReal: String?,
        val sincronizado: Int,
        val createdAt: String?
    )

    fun listarPorUnidadeLocal(unidadeLocalId: Long): List<EventoOffline> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, unidade_local_id, horario_inicio, horario_fim, esforco_real, sincronizado, created_at FROM $TABLE_EVENTOS WHERE unidade_local_id = ? ORDER BY created_at DESC",
            arrayOf(unidadeLocalId.toString())
        )
        return cursor.use { c ->
            val list = mutableListOf<EventoOffline>()
            while (c.moveToNext()) {
                list.add(
                    EventoOffline(
                        localId = c.getLong(0),
                        remoteId = if (c.isNull(1)) null else c.getInt(1),
                        unidadeLocalId = c.getLong(2),
                        horarioInicio = c.getString(3),
                        horarioFim = c.getString(4),
                        esforcoReal = c.getString(5),
                        sincronizado = c.getInt(6),
                        createdAt = c.getString(7)
                    )
                )
            }
            list
        }
    }

    fun listarTodos(): List<EventoOffline> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, unidade_local_id, horario_inicio, horario_fim, esforco_real, sincronizado, created_at FROM $TABLE_EVENTOS ORDER BY created_at DESC",
            null
        )
        return cursor.use { c ->
            val list = mutableListOf<EventoOffline>()
            while (c.moveToNext()) {
                list.add(
                    EventoOffline(
                        localId = c.getLong(0),
                        remoteId = if (c.isNull(1)) null else c.getInt(1),
                        unidadeLocalId = c.getLong(2),
                        horarioInicio = c.getString(3),
                        horarioFim = c.getString(4),
                        esforcoReal = c.getString(5),
                        sincronizado = c.getInt(6),
                        createdAt = c.getString(7)
                    )
                )
            }
            list
        }
    }

    fun buscarPorRemoteIdEscopo(remoteId: Int, unidadeLocalId: Long): EventoOffline? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, unidade_local_id, horario_inicio, horario_fim, esforco_real, sincronizado, created_at FROM $TABLE_EVENTOS WHERE $COL_REMOTE_ID = ? AND unidade_local_id = ? LIMIT 1",
            arrayOf(remoteId.toString(), unidadeLocalId.toString())
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                EventoOffline(
                    localId = c.getLong(0),
                    remoteId = if (c.isNull(1)) null else c.getInt(1),
                    unidadeLocalId = c.getLong(2),
                    horarioInicio = c.getString(3),
                    horarioFim = c.getString(4),
                    esforcoReal = c.getString(5),
                    sincronizado = c.getInt(6),
                    createdAt = c.getString(7)
                )
            } else null
        }
    }
}
