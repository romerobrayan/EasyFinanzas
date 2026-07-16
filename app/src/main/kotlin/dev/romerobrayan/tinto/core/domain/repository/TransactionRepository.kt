package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    fun observeTransactions(): Flow<List<Transaction>>

    suspend fun addTransaction(transaction: Transaction)

    suspend fun updateTransaction(transaction: Transaction)

    suspend fun deleteTransaction(transactionId: String)
}
