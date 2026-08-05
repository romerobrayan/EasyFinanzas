package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.data.local.TransactionDao
import dev.romerobrayan.tinto.core.data.local.toDomain
import dev.romerobrayan.tinto.core.data.local.toEntity
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The ledger of a no-account user: Room on this device, nothing else.
 * [SyncedTransactionRepository] serves it while the session is
 * [dev.romerobrayan.tinto.core.domain.model.UserSession.Local].
 */
@Singleton
class LocalTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun addTransaction(transaction: Transaction) {
        dao.upsert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        dao.upsert(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transactionId: String) {
        dao.delete(transactionId)
    }
}
