package com.example.kheprix.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "campanha_coleta",
    foreignKeys = [ForeignKey(
        entity = Estudo::class,
        parentColumns = ["id"],
        childColumns = ["estudo_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("estudo_id")]
)
data class CampanhaColeta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "estudo_id") val estudoId: Long,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "data_inicio") val dataInicio: Long,
    @ColumnInfo(name = "data_alteracao") val dataAlteracao: Long,
    @ColumnInfo(name = "descricao") val descricao: String?,
    @ColumnInfo(name = "sincronizado") val sincronizado: SyncStatus = SyncStatus.ASYNC
)

