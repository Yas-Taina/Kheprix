package com.example.kheprix.data.repository

import com.example.kheprix.data.dao.RegistroOcorrenciaDao
import com.example.kheprix.data.entity.RegistroOcorrencia
import com.example.kheprix.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

class RegistroOcorrenciaRepository(private val dao: RegistroOcorrenciaDao) {
    fun getByEvento(eventoId: Long): Flow<List<RegistroOcorrencia>> = dao.getByEvento(eventoId)
    fun getByEstudo(estudoId: Long): Flow<List<RegistroOcorrencia>> = dao.getByEstudo(estudoId)
    fun getByEspecie(especieId: Long): Flow<List<RegistroOcorrencia>> = dao.getByEspecie(especieId)
    suspend fun getById(id: Long): RegistroOcorrencia? = dao.getById(id)
    suspend fun insert(registro: RegistroOcorrencia): Long = dao.insert(registro)
    suspend fun update(registro: RegistroOcorrencia) = dao.update(registro)
    suspend fun delete(registro: RegistroOcorrencia) = dao.delete(registro)
    suspend fun getPendingSync(): List<RegistroOcorrencia> = dao.getBySyncStatus(SyncStatus.ASYNC)
    suspend fun markAsSynced(id: Long) = dao.updateSyncStatus(id, SyncStatus.SYNC)
}
