package com.example.kheprix.data.repository

import com.example.kheprix.data.dao.EventoAmostragemDao
import com.example.kheprix.data.entity.EventoAmostragem
import com.example.kheprix.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

class EventoAmostragemRepository(private val dao: EventoAmostragemDao) {
    fun getByUnidade(unidadeId: Long): Flow<List<EventoAmostragem>> = dao.getByUnidade(unidadeId)
    fun getByEstudo(estudoId: Long): Flow<List<EventoAmostragem>> = dao.getByEstudo(estudoId)
    suspend fun getById(id: Long): EventoAmostragem? = dao.getById(id)
    suspend fun insert(evento: EventoAmostragem): Long = dao.insert(evento)
    suspend fun update(evento: EventoAmostragem) = dao.update(evento)
    suspend fun delete(evento: EventoAmostragem) = dao.delete(evento)
    suspend fun getPendingSync(): List<EventoAmostragem> = dao.getBySyncStatus(SyncStatus.ASYNC)
    suspend fun markAsSynced(id: Long) = dao.updateSyncStatus(id, SyncStatus.SYNC)
}
