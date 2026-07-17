package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.Transaction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Flags pending captures that likely show money the ledger (or another
 * pending capture) already knows about, so the review inbox can warn before
 * the user confirms a double.
 *
 * Rule: same [dev.romerobrayan.tinto.core.domain.model.TransactionType],
 * identical amount, and `occurredAt` within [DEFAULT_WINDOW]. The card mask is
 * deliberately NOT required to match: the prime duplicate cases are the same
 * money reported by two different issuers — a 1CERO1 card-bill `Pago` and its
 * Bancolombia `Pagaste` cash leg carry different masks by definition.
 *
 * Surface-only: likely duplicates are flagged for the user to reconcile,
 * never auto-merged (ARCHITECTURE.md dedup rule).
 */
object DetectPendingDuplicates {

    /** Start at ±10 min on identical amount; tuned against the real pairs. */
    val DEFAULT_WINDOW: Duration = 10.minutes

    fun duplicateIds(
        pending: List<PendingTransaction>,
        committed: List<Transaction>,
        window: Duration = DEFAULT_WINDOW,
    ): Set<String> {
        fun PendingTransaction.matchesCommitted(transaction: Transaction): Boolean =
            transaction.type == type &&
                transaction.amount == amount &&
                (transaction.occurredAt - occurredAt).absoluteValue <= window

        fun PendingTransaction.matchesPending(other: PendingTransaction): Boolean =
            other.id != id &&
                other.type == type &&
                other.amount == amount &&
                (other.occurredAt - occurredAt).absoluteValue <= window

        return pending
            .filter { candidate ->
                committed.any(candidate::matchesCommitted) || pending.any(candidate::matchesPending)
            }
            .map { it.id }
            .toSet()
    }
}
