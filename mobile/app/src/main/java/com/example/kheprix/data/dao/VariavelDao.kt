package com.example.kheprix.data.dao

import androidx.room.*
import com.example.kheprix.data.entity.SyncStatus
import com.example.kheprix.data.entity.Variavel
import kotlinx.coroutines.flow.Flow

@Dao
interface VariavelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(variavel: Variavel): Long

    @Update
    suspend fun update(variavel: Variavel)

    @Delete
    suspend fun delete(variavel: Variavel)

    @Query("SELECT * FROM variavel WHERE estudo_id = :estudoId ORDER BY nome ASC")
    fun getByEstudo(estudoId: Long): Flow<List<Variavel>>

    @Query("SELECT * FROM variavel WHERE id = :id")
    suspend fun getById(id: Long): Variavel?

    @Query("SELECT * FROM variavel WHERE sincronizado = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<Variavel>

    @Query("UPDATE variavel SET sincronizado = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)
}
