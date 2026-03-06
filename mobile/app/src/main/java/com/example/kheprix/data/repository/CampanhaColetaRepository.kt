package com.example.kheprix.data.repository

import com.example.kheprix.data.dao.CampanhaColetaDao
import com.example.kheprix.data.entity.CampanhaColeta
import com.example.kheprix.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

class CampanhaColetaRepository(private val dao: CampanhaColetaDao) {
    fun getByEstudo(estudoId: Long): Flow<List<CampanhaColeta>> = dao.getByEstudo(estudoId)
    suspend fun getById(id: Long): CampanhaColeta? = dao.getById(id)
    suspend fun insert(campanha: CampanhaColeta): Long = dao.insert(campanha)
    suspend fun update(campanha: CampanhaColeta) = dao.update(campanha)
    suspend fun delete(campanha: CampanhaColeta) = dao.delete(campanha)
    suspend fun getPendingSync(): List<CampanhaColeta> = dao.getBySyncStatus(SyncStatus.ASYNC)
    suspend fun markAsSynced(id: Long) = dao.updateSyncStatus(id, SyncStatus.SYNC)
}
