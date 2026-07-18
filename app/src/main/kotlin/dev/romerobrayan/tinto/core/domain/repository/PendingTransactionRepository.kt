package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import kotlinx.coroutines.flow.Flow

/**
 * The device-local staging store for captured parses. Deliberately outside
 * the `Synced*`/`InMemory*` session split: SMS lives on the phone and can
 * arrive signed-out, and a raw parse is not user data to replicate to the
 * cloud. Confirmed/discarded items leave the inbox but their keys are kept
 * so a re-backfill never resurrects them.
 */
interface PendingTransactionRepository {

    /** Items awaiting review, newest first. */
    fun observePending(): Flow<List<PendingTransaction>>

    /** Stages a parse. A capture already seen (same message) is ignored. */
    suspend fun stage(pending: PendingTransaction)

    /** Removes the item from the inbox after it was promoted to the ledger. */
    suspend fun markConfirmed(pendingId: String)

    /** Removes the item from the inbox without a ledger write. */
    suspend fun markDiscarded(pendingId: String)
}
