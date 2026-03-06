package com.example.kheprix.data.dao

import androidx.room.*
import com.example.kheprix.data.entity.Estudo
import com.example.kheprix.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EstudoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(estudo: Estudo): Long

    @Update
    suspend fun update(estudo: Estudo)

    @Delete
    suspend fun delete(estudo: Estudo)

    @Query("SELECT * FROM estudo ORDER BY nome ASC")
    fun getAll(): Flow<List<Estudo>>

    @Query("SELECT * FROM estudo WHERE id = :id")
    suspend fun getById(id: Long): Estudo?

    @Query("SELECT * FROM estudo WHERE sincronizado = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<Estudo>

    @Query("UPDATE estudo SET sincronizado = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)
}
