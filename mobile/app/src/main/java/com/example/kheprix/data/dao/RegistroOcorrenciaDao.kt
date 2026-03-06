package com.example.kheprix.data.dao

import androidx.room.*
import com.example.kheprix.data.entity.RegistroOcorrencia
import com.example.kheprix.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroOcorrenciaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(registro: RegistroOcorrencia): Long

    @Update
    suspend fun update(registro: RegistroOcorrencia)

    @Delete
    suspend fun delete(registro: RegistroOcorrencia)

    @Query("SELECT * FROM registro_ocorrencia WHERE evento_id = :eventoId ORDER BY data_hora DESC")
    fun getByEvento(eventoId: Long): Flow<List<RegistroOcorrencia>>

    @Query("SELECT * FROM registro_ocorrencia WHERE estudo_id = :estudoId ORDER BY data_hora DESC")
    fun getByEstudo(estudoId: Long): Flow<List<RegistroOcorrencia>>

    @Query("SELECT * FROM registro_ocorrencia WHERE especie_id = :especieId ORDER BY data_hora DESC")
    fun getByEspecie(especieId: Long): Flow<List<RegistroOcorrencia>>

    @Query("SELECT * FROM registro_ocorrencia WHERE id = :id")
    suspend fun getById(id: Long): RegistroOcorrencia?

    @Query("SELECT * FROM registro_ocorrencia WHERE sincronizado = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<RegistroOcorrencia>

    @Query("UPDATE registro_ocorrencia SET sincronizado = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)
}
