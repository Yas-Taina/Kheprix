package com.example.kheprix.data.repository

import com.example.kheprix.data.dao.UnidadeAmostralDao
import com.example.kheprix.data.entity.SyncStatus
import com.example.kheprix.data.entity.UnidadeAmostral
import kotlinx.coroutines.flow.Flow

class UnidadeAmostralRepository(private val dao: UnidadeAmostralDao) {
    fun getByEstudo(estudoId: Long): Flow<List<UnidadeAmostral>> = dao.getByEstudo(estudoId)
    fun getByCampanha(campanhaId: Long): Flow<List<UnidadeAmostral>> = dao.getByCampanha(campanhaId)
    suspend fun getById(id: Long): UnidadeAmostral? = dao.getById(id)
    suspend fun insert(unidade: UnidadeAmostral): Long = dao.insert(unidade)
    suspend fun update(unidade: UnidadeAmostral) = dao.update(unidade)
    suspend fun delete(unidade: UnidadeAmostral) = dao.delete(unidade)
    suspend fun getPendingSync(): List<UnidadeAmostral> = dao.getBySyncStatus(SyncStatus.ASYNC)
    suspend fun markAsSynced(id: Long) = dao.updateSyncStatus(id, SyncStatus.SYNC)
}
