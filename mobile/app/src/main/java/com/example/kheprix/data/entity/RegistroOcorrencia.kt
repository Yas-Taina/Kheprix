package com.example.kheprix.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registro_ocorrencia",
    foreignKeys = [
        ForeignKey(
            entity = EventoAmostragem::class,
            parentColumns = ["id"],
            childColumns = ["evento_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Estudo::class,
            parentColumns = ["id"],
            childColumns = ["estudo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Especie::class,
            parentColumns = ["id"],
            childColumns = ["especie_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("evento_id"), Index("estudo_id"), Index("especie_id")]
)
data class RegistroOcorrencia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "evento_id") val eventoId: Long,
    @ColumnInfo(name = "estudo_id") val estudoId: Long,
    @ColumnInfo(name = "especie_id") val especieId: Long?,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
    @ColumnInfo(name = "foto") val foto: ByteArray?,
    @ColumnInfo(name = "quantidade_individuos") val quantidadeIndividuos: Int?,
    @ColumnInfo(name = "data_hora") val dataHora: Long,
    @ColumnInfo(name = "ausencia_especie") val ausenciaEspecie: Boolean = false,
    @ColumnInfo(name = "sincronizado") val sincronizado: SyncStatus = SyncStatus.ASYNC
)

