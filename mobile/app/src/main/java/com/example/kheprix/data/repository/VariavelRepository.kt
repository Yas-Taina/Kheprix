package com.example.kheprix.data.repository

import com.example.kheprix.data.dao.VariavelDao
import com.example.kheprix.data.entity.SyncStatus
import com.example.kheprix.data.entity.Variavel
import kotlinx.coroutines.flow.Flow

class VariavelRepository(private val dao: VariavelDao) {
    fun getByEstudo(estudoId: Long): Flow<List<Variavel>> = dao.getByEstudo(estudoId)
    suspend fun getById(id: Long): Variavel? = dao.getById(id)
    suspend fun insert(variavel: Variavel): Long = dao.insert(variavel)
    suspend fun update(variavel: Variavel) = dao.update(variavel)
    suspend fun delete(variavel: Variavel) = dao.delete(variavel)
    suspend fun getPendingSync(): List<Variavel> = dao.getBySyncStatus(SyncStatus.ASYNC)
    suspend fun markAsSynced(id: Long) = dao.updateSyncStatus(id, SyncStatus.SYNC)
}
