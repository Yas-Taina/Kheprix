package com.example.kheprix.data.database

import androidx.room.TypeConverter
import com.example.kheprix.data.entity.NivelEstudo
import com.example.kheprix.data.entity.SyncStatus
import com.example.kheprix.data.entity.TipoDado

class Converters {

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromNivelEstudo(value: NivelEstudo): String = value.name

    @TypeConverter
    fun toNivelEstudo(value: String): NivelEstudo = NivelEstudo.valueOf(value)

    @TypeConverter
    fun fromTipoDado(value: TipoDado): String = value.name

    @TypeConverter
    fun toTipoDado(value: String): TipoDado = TipoDado.valueOf(value)
}
