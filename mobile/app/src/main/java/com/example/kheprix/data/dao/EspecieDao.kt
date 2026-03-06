package com.example.kheprix.data.dao

import androidx.room.*
import com.example.kheprix.data.entity.Especie
import com.example.kheprix.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EspecieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(especie: Especie): Long

    @Update
    suspend fun update(especie: Especie)

    @Delete
    suspend fun delete(especie: Especie)

    @Query("SELECT * FROM especie WHERE estudo_id = :estudoId ORDER BY especie ASC")
    fun getByEstudo(estudoId: Long): Flow<List<Especie>>

    @Query("SELECT * FROM especie WHERE id = :id")
    suspend fun getById(id: Long): Especie?

    @Query("SELECT * FROM especie WHERE sincronizado = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<Especie>

    @Query("UPDATE especie SET sincronizado = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)
}
