package com.example.kheprix.data.dao

import androidx.room.*
import com.example.kheprix.data.entity.CampanhaColeta
import com.example.kheprix.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CampanhaColetaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(campanha: CampanhaColeta): Long

    @Update
    suspend fun update(campanha: CampanhaColeta)

    @Delete
    suspend fun delete(campanha: CampanhaColeta)

    @Query("SELECT * FROM campanha_coleta WHERE estudo_id = :estudoId ORDER BY data_inicio DESC")
    fun getByEstudo(estudoId: Long): Flow<List<CampanhaColeta>>

    @Query("SELECT * FROM campanha_coleta WHERE id = :id")
    suspend fun getById(id: Long): CampanhaColeta?

    @Query("SELECT * FROM campanha_coleta WHERE sincronizado = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<CampanhaColeta>

    @Query("UPDATE campanha_coleta SET sincronizado = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)
}
