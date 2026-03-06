package com.example.kheprix.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "especie",
    foreignKeys = [ForeignKey(
        entity = Estudo::class,
        parentColumns = ["id"],
        childColumns = ["estudo_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("estudo_id")]
)
data class Especie(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "estudo_id") val estudoId: Long,
    @ColumnInfo(name = "foto") val foto: ByteArray?,
    @ColumnInfo(name = "classe") val classe: String?,
    @ColumnInfo(name = "ordem") val ordem: String?,
    @ColumnInfo(name = "familia") val familia: String?,
    @ColumnInfo(name = "genero") val genero: String?,
    @ColumnInfo(name = "especie") val especie: String?,
    @ColumnInfo(name = "nome_popular") val nomePopular: String?,
    @ColumnInfo(name = "status_conservacao") val statusConservacao: String?,
    @ColumnInfo(name = "nativa_regiao") val nativaRegiao: Boolean = false,
    @ColumnInfo(name = "sincronizado") val sincronizado: SyncStatus = SyncStatus.ASYNC
)