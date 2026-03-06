package com.example.kheprix.data.dao

import androidx.room.*
import com.example.kheprix.data.entity.SyncStatus
import com.example.kheprix.data.entity.UnidadeAmostral
import kotlinx.coroutines.flow.Flow

@Dao
interface UnidadeAmostralDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unidade: UnidadeAmostral): Long

    @Update
    suspend fun update(unidade: UnidadeAmostral)

    @Delete
    suspend fun delete(unidade: UnidadeAmostral)

    @Query("SELECT * FROM unidade_amostral WHERE estudo_id = :estudoId ORDER BY nome ASC")
    fun getByEstudo(estudoId: Long): Flow<List<UnidadeAmostral>>

    @Query("SELECT * FROM unidade_amostral WHERE campanha_id = :campanhaId ORDER BY nome ASC")
    fun getByCampanha(campanhaId: Long): Flow<List<UnidadeAmostral>>

    @Query("SELECT * FROM unidade_amostral WHERE id = :id")
    suspend fun getById(id: Long): UnidadeAmostral?

    @Query("SELECT * FROM unidade_amostral WHERE sincronizado = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<UnidadeAmostral>

    @Query("UPDATE unidade_amostral SET sincronizado = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)
}
