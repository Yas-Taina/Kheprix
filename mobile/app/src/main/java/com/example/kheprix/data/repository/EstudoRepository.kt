package com.example.kheprix.data.repository

import com.example.kheprix.data.dao.EstudoDao
import com.example.kheprix.data.entity.Estudo
import com.example.kheprix.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

class EstudoRepository(private val dao: EstudoDao) {
    fun getAll(): Flow<List<Estudo>> = dao.getAll()
    suspend fun getById(id: Long): Estudo? = dao.getById(id)
    suspend fun insert(estudo: Estudo): Long = dao.insert(estudo)
    suspend fun update(estudo: Estudo) = dao.update(estudo)
    suspend fun delete(estudo: Estudo) = dao.delete(estudo)
    suspend fun getPendingSync(): List<Estudo> = dao.getBySyncStatus(SyncStatus.ASYNC)
    suspend fun markAsSynced(id: Long) = dao.updateSyncStatus(id, SyncStatus.SYNC)
}
