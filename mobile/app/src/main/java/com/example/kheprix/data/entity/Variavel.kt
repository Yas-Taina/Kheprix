package com.example.kheprix.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class NivelEstudo {
    CAMPANHA,
    EVENTO
}

enum class TipoDado {
    TEXTO,
    DATA,
    NUMERICO
}

@Entity(
    tableName = "variavel",
    foreignKeys = [ForeignKey(
        entity = Estudo::class,
        parentColumns = ["id"],
        childColumns = ["estudo_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("estudo_id")]
)
data class Variavel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "estudo_id") val estudoId: Long,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "metrica") val metrica: String?,
    @ColumnInfo(name = "nivel_estudo") val nivelEstudo: NivelEstudo,
    @ColumnInfo(name = "tipo_dado") val tipoDado: TipoDado,
    @ColumnInfo(name = "sincronizado") val sincronizado: SyncStatus = SyncStatus.ASYNC
)
