package com.example.kheprix.data.repository

import com.example.kheprix.data.dao.EspecieDao
import com.example.kheprix.data.entity.Especie
import com.example.kheprix.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

class EspecieRepository(private val dao: EspecieDao) {
    fun getByEstudo(estudoId: Long): Flow<List<Especie>> = dao.getByEstudo(estudoId)
    suspend fun getById(id: Long): Especie? = dao.getById(id)
    suspend fun insert(especie: Especie): Long = dao.insert(especie)
    suspend fun update(especie: Especie) = dao.update(especie)
    suspend fun delete(especie: Especie) = dao.delete(especie)
    suspend fun getPendingSync(): List<Especie> = dao.getBySyncStatus(SyncStatus.ASYNC)
    suspend fun markAsSynced(id: Long) = dao.updateSyncStatus(id, SyncStatus.SYNC)
}
