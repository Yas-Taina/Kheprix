package com.example.kheprix.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "estudo")
data class Estudo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "observacoes") val observacoes: String?,
    @ColumnInfo(name = "sincronizado") val sincronizado: SyncStatus = SyncStatus.ASYNC
)
