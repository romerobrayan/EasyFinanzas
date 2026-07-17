package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import kotlinx.coroutines.flow.Flow

/**
 * The capture staging store. Device-local and pre-user-data by design: it
 * does NOT follow the `Synced*`/`InMemory*` session split, and nothing in it
 * is ever written to Firestore. Confirming an item promotes it through the
 * session-routed [TransactionRepository]; this store only ever holds parses
 * awaiting review.
 */
interface PendingTransactionRepository {

    /** All staged captures, newest occurrence first. */
    fun observePending(): Flow<List<PendingTransaction>>

    /**
     * Stages a parsed capture unless a capture with the same [rawKey] was
     * already staged once — even if it has since been confirmed or discarded.
     * Returns true when newly staged. This is what makes the SMS backfill and
     * re-delivered broadcasts idempotent.
     */
    suspend fun stage(pending: PendingTransaction, rawKey: String): Boolean

    /** Removes a staged item (after confirm or discard). Never touches the ledger. */
    suspend fun remove(pendingId: String)
}
