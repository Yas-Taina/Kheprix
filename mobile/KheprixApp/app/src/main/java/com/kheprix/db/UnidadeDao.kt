package com.kheprix.db

import android.content.Context
import com.kheprix.db.DatabaseHelper.Companion.COL_LOCAL_ID
import com.kheprix.db.DatabaseHelper.Companion.COL_REMOTE_ID
import com.kheprix.db.DatabaseHelper.Companion.TABLE_UNIDADES

class UnidadeDao(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    data class UnidadeOffline(
        val localId: Long,
        val remoteId: Int?,
        val campanhaLocalId: Long,
        val nome: String,
        val latitude: Double,
        val longitude: Double,
        val raio: Double?,
        val metodoColeta: String?,
        val esforcoAmostral: String?,
        val sincronizado: Int,
        val createdAt: String?,
        val updatedAt: String?
    )

    fun listarPorCampanhaLocal(campanhaLocalId: Long): List<UnidadeOffline> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, campanha_local_id, nome, latitude, longitude, raio, metodo_coleta, esforco_amostral, sincronizado, created_at, updated_at FROM ${DatabaseHelper.TABLE_UNIDADES} WHERE campanha_local_id = ? ORDER BY created_at DESC",
            arrayOf(campanhaLocalId.toString())
        )
        return cursor.use { c ->
            val list = mutableListOf<UnidadeOffline>()
            while (c.moveToNext()) {
                list.add(
                    UnidadeOffline(
                        localId = c.getLong(0),
                        remoteId = if (c.isNull(1)) null else c.getInt(1),
                        campanhaLocalId = c.getLong(2),
                        nome = c.getString(3),
                        latitude = c.getDouble(4),
                        longitude = c.getDouble(5),
                        raio = if (c.isNull(6)) null else c.getDouble(6),
                        metodoColeta = c.getString(7),
                        esforcoAmostral = c.getString(8),
                        sincronizado = c.getInt(9),
                        createdAt = c.getString(10),
                        updatedAt = c.getString(11)
                    )
                )
            }
            list
        }
    }

    fun listarTodos(): List<UnidadeOffline> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, campanha_local_id, nome, latitude, longitude, raio, metodo_coleta, esforco_amostral, sincronizado, created_at, updated_at FROM ${DatabaseHelper.TABLE_UNIDADES} ORDER BY created_at DESC",
            null
        )
        return cursor.use { c ->
            val list = mutableListOf<UnidadeOffline>()
            while (c.moveToNext()) {
                list.add(
                    UnidadeOffline(
                        localId = c.getLong(0),
                        remoteId = if (c.isNull(1)) null else c.getInt(1),
                        campanhaLocalId = c.getLong(2),
                        nome = c.getString(3),
                        latitude = c.getDouble(4),
                        longitude = c.getDouble(5),
                        raio = if (c.isNull(6)) null else c.getDouble(6),
                        metodoColeta = c.getString(7),
                        esforcoAmostral = c.getString(8),
                        sincronizado = c.getInt(9),
                        createdAt = c.getString(10),
                        updatedAt = c.getString(11)
                    )
                )
            }
            list
        }
    }

    fun buscarPorRemoteIdEscopo(remoteId: Int, campanhaLocalId: Long): UnidadeOffline? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_LOCAL_ID, $COL_REMOTE_ID, campanha_local_id, nome, latitude, longitude, raio, metodo_coleta, esforco_amostral, sincronizado, created_at, updated_at FROM ${DatabaseHelper.TABLE_UNIDADES} WHERE $COL_REMOTE_ID = ? AND campanha_local_id = ? LIMIT 1",
            arrayOf(remoteId.toString(), campanhaLocalId.toString())
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                UnidadeOffline(
                    localId = c.getLong(0),
                    remoteId = if (c.isNull(1)) null else c.getInt(1),
                    campanhaLocalId = c.getLong(2),
                    nome = c.getString(3),
                    latitude = c.getDouble(4),
                    longitude = c.getDouble(5),
                    raio = if (c.isNull(6)) null else c.getDouble(6),
                    metodoColeta = c.getString(7),
                    esforcoAmostral = c.getString(8),
                    sincronizado = c.getInt(9),
                    createdAt = c.getString(10),
                    updatedAt = c.getString(11)
                )
            } else null
        }
    }
}
