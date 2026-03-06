package com.example.kheprix.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "valor_variavel",
    foreignKeys = [ForeignKey(
        entity = Variavel::class,
        parentColumns = ["id"],
        childColumns = ["variavel_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("variavel_id")]
)
data class ValorVariavel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "variavel_id") val variavelId: Long,
    @ColumnInfo(name = "valor") val valor: String,
    @ColumnInfo(name = "sincronizado") val sincronizado: SyncStatus = SyncStatus.ASYNC
)
