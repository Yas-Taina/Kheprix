package com.example.kheprix.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unidade_amostral",
    foreignKeys = [
        ForeignKey(
            entity = Estudo::class,
            parentColumns = ["id"],
            childColumns = ["estudo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CampanhaColeta::class,
            parentColumns = ["id"],
            childColumns = ["campanha_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("estudo_id"), Index("campanha_id")]
)
data class UnidadeAmostral(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "estudo_id") val estudoId: Long,
    @ColumnInfo(name = "campanha_id") val campanhaId: Long,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
    @ColumnInfo(name = "raio") val raio: Double?,
    @ColumnInfo(name = "metodo_coleta") val metodoColeta: String?,
    @ColumnInfo(name = "esforco_amostral") val esforcoAmostral: String?,
    @ColumnInfo(name = "sincronizado") val sincronizado: SyncStatus = SyncStatus.ASYNC
)

