package com.example.kheprix.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evento_amostragem",
    foreignKeys = [
        ForeignKey(
            entity = UnidadeAmostral::class,
            parentColumns = ["id"],
            childColumns = ["unidade_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Estudo::class,
            parentColumns = ["id"],
            childColumns = ["estudo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("unidade_id"), Index("estudo_id")]
)
data class EventoAmostragem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "unidade_id") val unidadeId: Long,
    @ColumnInfo(name = "estudo_id") val estudoId: Long,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "hora_inicio") val horaInicio: Long,
    @ColumnInfo(name = "hora_fim") val horaFim: Long?,
    @ColumnInfo(name = "esforco_real") val esforcoReal: String?,
    @ColumnInfo(name = "sincronizado") val sincronizado: SyncStatus = SyncStatus.ASYNC
)
