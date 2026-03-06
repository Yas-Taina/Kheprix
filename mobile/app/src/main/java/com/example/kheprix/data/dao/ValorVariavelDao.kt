package com.example.kheprix.data.dao

import androidx.room.*
import com.example.kheprix.data.entity.SyncStatus
import com.example.kheprix.data.entity.ValorVariavel
import kotlinx.coroutines.flow.Flow

@Dao
interface ValorVariavelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(valorVariavel: ValorVariavel): Long

    @Update
    suspend fun update(valorVariavel: ValorVariavel)

    @Delete
    suspend fun delete(valorVariavel: ValorVariavel)

    @Query("SELECT * FROM valor_variavel WHERE variavel_id = :variavelId")
    fun getByVariavel(variavelId: Long): Flow<List<ValorVariavel>>

    @Query("SELECT * FROM valor_variavel WHERE id = :id")
    suspend fun getById(id: Long): ValorVariavel?

    @Query("SELECT * FROM valor_variavel WHERE sincronizado = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<ValorVariavel>

    @Query("UPDATE valor_variavel SET sincronizado = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)
}
