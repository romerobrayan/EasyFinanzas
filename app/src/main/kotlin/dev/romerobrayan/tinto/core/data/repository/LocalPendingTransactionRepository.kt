package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.data.local.CaptureSeenEntity
import dev.romerobrayan.tinto.core.data.local.PendingTransactionDao
import dev.romerobrayan.tinto.core.data.local.toDomain
import dev.romerobrayan.tinto.core.data.local.toEntity
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.repository.PendingTransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed staging store. Deliberately outside the `Synced*`/`InMemory*`
 * session split: captures are device-local and pre-user-data until confirmed
 * (TASK_SPRINT_3_CAPTURE.md), so demo and signed-in sessions share it.
 */
@Singleton
class LocalPendingTransactionRepository @Inject constructor(
    private val dao: PendingTransactionDao,
) : PendingTransactionRepository {

    override fun observePending(): Flow<List<PendingTransaction>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun stage(pending: PendingTransaction, rawKey: String): Boolean =
        dao.stageIfUnseen(
            entity = pending.toEntity(),
            seen = CaptureSeenEntity(rawKey = rawKey, seenAt = pending.capturedAt.toEpochMilliseconds()),
        )

    override suspend fun remove(pendingId: String) = dao.deleteById(pendingId)
}
