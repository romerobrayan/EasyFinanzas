package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.data.local.PendingTransactionDao
import dev.romerobrayan.tinto.core.data.local.PendingTransactionEntity
import dev.romerobrayan.tinto.core.data.local.toDomain
import dev.romerobrayan.tinto.core.data.local.toEntity
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.repository.PendingTransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed staging store. Not session-routed on purpose: captures are
 * device-local until the user confirms them into the (session-routed) ledger.
 */
@Singleton
class RoomPendingTransactionRepository @Inject constructor(
    private val dao: PendingTransactionDao,
) : PendingTransactionRepository {

    override fun observePending(): Flow<List<PendingTransaction>> =
        dao.observePending().map { entities -> entities.mapNotNull { it.toDomain() } }

    override suspend fun stage(pending: PendingTransaction) {
        dao.insert(pending.toEntity())
    }

    override suspend fun markConfirmed(pendingId: String) {
        dao.updateStatus(pendingId, PendingTransactionEntity.STATUS_CONFIRMED)
    }

    override suspend fun markDiscarded(pendingId: String) {
        dao.updateStatus(pendingId, PendingTransactionEntity.STATUS_DISCARDED)
    }
}
