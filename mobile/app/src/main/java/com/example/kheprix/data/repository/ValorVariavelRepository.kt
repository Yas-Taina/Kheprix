package com.example.kheprix.data.repository

import com.example.kheprix.data.dao.ValorVariavelDao
import com.example.kheprix.data.entity.SyncStatus
import com.example.kheprix.data.entity.ValorVariavel
import kotlinx.coroutines.flow.Flow

class ValorVariavelRepository(private val dao: ValorVariavelDao) {
    fun getByVariavel(variavelId: Long): Flow<List<ValorVariavel>> = dao.getByVariavel(variavelId)
    suspend fun getById(id: Long): ValorVariavel? = dao.getById(id)
    suspend fun insert(valor: ValorVariavel): Long = dao.insert(valor)
    suspend fun update(valor: ValorVariavel) = dao.update(valor)
    suspend fun delete(valor: ValorVariavel) = dao.delete(valor)
    suspend fun getPendingSync(): List<ValorVariavel> = dao.getBySyncStatus(SyncStatus.ASYNC)
    suspend fun markAsSynced(id: Long) = dao.updateSyncStatus(id, SyncStatus.SYNC)
}
