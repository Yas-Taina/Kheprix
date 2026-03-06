package com.example.kheprix.data.dao

import androidx.room.*
import com.example.kheprix.data.entity.EventoAmostragem
import com.example.kheprix.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoAmostragemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evento: EventoAmostragem): Long

    @Update
    suspend fun update(evento: EventoAmostragem)

    @Delete
    suspend fun delete(evento: EventoAmostragem)

    @Query("SELECT * FROM evento_amostragem WHERE unidade_id = :unidadeId ORDER BY hora_inicio DESC")
    fun getByUnidade(unidadeId: Long): Flow<List<EventoAmostragem>>

    @Query("SELECT * FROM evento_amostragem WHERE estudo_id = :estudoId ORDER BY hora_inicio DESC")
    fun getByEstudo(estudoId: Long): Flow<List<EventoAmostragem>>

    @Query("SELECT * FROM evento_amostragem WHERE id = :id")
    suspend fun getById(id: Long): EventoAmostragem?

    @Query("SELECT * FROM evento_amostragem WHERE sincronizado = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<EventoAmostragem>

    @Query("UPDATE evento_amostragem SET sincronizado = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)
}
