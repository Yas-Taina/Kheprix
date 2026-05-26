package com.kheprix.db

import android.content.Context
import com.kheprix.db.DatabaseHelper.Companion.COL_LOCAL_ID
import com.kheprix.db.DatabaseHelper.Companion.COL_REMOTE_ID
import com.kheprix.db.DatabaseHelper.Companion.TABLE_REGISTROS

class RegistroDao(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    data class RegistroOffline(
        val localId: Long,
        val remoteId: Int?,
        val eventoLocalId: Long,
        val especieLocalId: Long,
        val especieRemoteId: Int,
        val data: String,
        val hora: String,
        val latitude: Double,
        val longitude: Double,
        val qtdeIndividuos: Int?,
        val foto: String?,
        val ausenciaEspecie: Boolean?,
        val sincronizado: Int,
        val createdAt: String?
    )

    fun buscarPorLocalId(localId: Long): RegistroOffline? {
        val db = dbHelper.readableDatabase
        return db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, evento_local_id, especie_local_id, especie_remote_id, data, hora, latitude, longitude, qtde_individuos, foto, ausencia_especie, sincronizado, created_at FROM $TABLE_REGISTROS WHERE $COL_LOCAL_ID = ?",
            arrayOf(localId.toString())
        ).use { c ->
            if (c.moveToFirst()) RegistroOffline(
                localId = c.getLong(0),
                remoteId = if (c.isNull(1)) null else c.getInt(1),
                eventoLocalId = c.getLong(2),
                especieLocalId = c.getLong(3),
                especieRemoteId = c.getInt(4),
                data = c.getString(5),
                hora = c.getString(6),
                latitude = c.getDouble(7),
                longitude = c.getDouble(8),
                qtdeIndividuos = if (c.isNull(9)) null else c.getInt(9),
                foto = c.getString(10),
                ausenciaEspecie = if (c.isNull(11)) null else c.getInt(11) == 1,
                sincronizado = c.getInt(12),
                createdAt = c.getString(13)
            ) else null
        }
    }

    fun buscarPorRemoteIdEscopo(remoteId: Int, eventoLocalId: Long): RegistroOffline? {
        val db = dbHelper.readableDatabase
        return db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, evento_local_id, especie_local_id, especie_remote_id, data, hora, latitude, longitude, qtde_individuos, foto, ausencia_especie, sincronizado, created_at FROM $TABLE_REGISTROS WHERE $COL_REMOTE_ID = ? AND evento_local_id = ?",
            arrayOf(remoteId.toString(), eventoLocalId.toString())
        ).use { c ->
            if (c.moveToFirst()) RegistroOffline(
                localId = c.getLong(0),
                remoteId = if (c.isNull(1)) null else c.getInt(1),
                eventoLocalId = c.getLong(2),
                especieLocalId = c.getLong(3),
                especieRemoteId = c.getInt(4),
                data = c.getString(5),
                hora = c.getString(6),
                latitude = c.getDouble(7),
                longitude = c.getDouble(8),
                qtdeIndividuos = if (c.isNull(9)) null else c.getInt(9),
                foto = c.getString(10),
                ausenciaEspecie = if (c.isNull(11)) null else c.getInt(11) == 1,
                sincronizado = c.getInt(12),
                createdAt = c.getString(13)
            ) else null
        }
    }

    fun listarPorEventoLocal(eventoLocalId: Long): List<RegistroOffline> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, evento_local_id, especie_local_id, especie_remote_id, data, hora, latitude, longitude, qtde_individuos, foto, ausencia_especie, sincronizado, created_at FROM $TABLE_REGISTROS WHERE evento_local_id = ? ORDER BY created_at DESC",
            arrayOf(eventoLocalId.toString())
        )
        return cursor.use { c ->
            val list = mutableListOf<RegistroOffline>()
            while (c.moveToNext()) {
                list.add(
                    RegistroOffline(
                        localId = c.getLong(0),
                        remoteId = if (c.isNull(1)) null else c.getInt(1),
                        eventoLocalId = c.getLong(2),
                        especieLocalId = c.getLong(3),
                        especieRemoteId = c.getInt(4),
                        data = c.getString(5),
                        hora = c.getString(6),
                        latitude = c.getDouble(7),
                        longitude = c.getDouble(8),
                        qtdeIndividuos = if (c.isNull(9)) null else c.getInt(9),
                        foto = c.getString(10),
                        ausenciaEspecie = if (c.isNull(11)) null else c.getInt(11) == 1,
                        sincronizado = c.getInt(12),
                        createdAt = c.getString(13)
                    )
                )
            }
            list
        }
    }
}
