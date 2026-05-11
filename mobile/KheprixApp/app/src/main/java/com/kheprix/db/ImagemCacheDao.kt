package com.kheprix.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

class ImagemCacheDao(context: Context) {

    private val helper = DatabaseHelper(context.applicationContext)

    fun buscar(url: String): ByteArray? {
        val db = helper.readableDatabase
        db.query(
            DatabaseHelper.TABLE_IMAGENS_CACHE,
            arrayOf("bytes"),
            "url = ?", arrayOf(url),
            null, null, null
        ).use { c ->
            if (c.moveToFirst()) return c.getBlob(0)
        }
        return null
    }

    fun salvar(url: String, bytes: ByteArray) {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put("url", url)
            put("bytes", bytes)
            put(DatabaseHelper.COL_CREATED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(
            DatabaseHelper.TABLE_IMAGENS_CACHE, null, values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}
