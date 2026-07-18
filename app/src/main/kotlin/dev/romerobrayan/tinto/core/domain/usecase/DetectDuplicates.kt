package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.Transaction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Default window for "the same money seen twice": identical amount within
 * this many minutes. Tuned against the real pairs — the 1CERO1 `Pago` and
 * its Bancolombia `Pagaste` counterpart arrive under a minute apart.
 */
val DUPLICATE_WINDOW: Duration = 10.minutes

/**
 * Flags pending captures that look like duplicates: an identical amount
 * occurring within [window] of an already-committed movement **or** of
 * another pending item. Cross-issuer on purpose — the prime duplicate case
 * is the same payment reported by two banks (1CERO1 `Pago` ↔ Bancolombia
 * `Pagaste`), where the masks differ, so last4 is deliberately not part of
 * the match. Surfaces only; never merges (the user is the gate).
 *
 * @return ids of the pending items to badge as "Posible duplicado". When two
 * pending items mirror each other, both are flagged — the user picks which
 * one to keep.
 */
fun detectDuplicates(
    pending: List<PendingTransaction>,
    committed: List<Transaction>,
    window: Duration = DUPLICATE_WINDOW,
): Set<String> {
    val flagged = mutableSetOf<String>()
    pending.forEach { item ->
        val matchesCommitted = committed.any { transaction ->
            transaction.amount == item.amount &&
                (transaction.occurredAt - item.occurredAt).absoluteValue <= window
        }
        val matchesOtherPending = pending.any { other ->
            other.id != item.id &&
                other.amount == item.amount &&
                (other.occurredAt - item.occurredAt).absoluteValue <= window
        }
        if (matchesCommitted || matchesOtherPending) flagged += item.id
    }
    return flagged
}
