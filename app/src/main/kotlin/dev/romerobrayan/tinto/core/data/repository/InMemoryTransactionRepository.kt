package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Demo-mode ledger seeded from [MockData]; [SyncedTransactionRepository]
 * serves it while the session is [dev.romerobrayan.tinto.core.domain.model.UserSession.Demo].
 * Nothing here survives a process restart — by design.
 */
@Singleton
class InMemoryTransactionRepository @Inject constructor() : TransactionRepository {

    private val transactions = MutableStateFlow(MockData.transactions)

    override fun observeTransactions(): Flow<List<Transaction>> = transactions.asStateFlow()

    override suspend fun addTransaction(transaction: Transaction) {
        transactions.update { it + transaction }
    }
}
