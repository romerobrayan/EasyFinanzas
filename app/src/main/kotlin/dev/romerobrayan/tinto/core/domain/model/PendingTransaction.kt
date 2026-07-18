package dev.romerobrayan.tinto.core.domain.model

import kotlinx.datetime.Instant

/**
 * A parsed capture staged for user review. Device-local only: it never
 * reaches Firestore — on confirm it is promoted to a [Transaction] through
 * the session-routed repository, on discard it is dropped without a ledger
 * write. Never auto-committed.
 */
data class PendingTransaction(
    val id: String,
    val channel: CaptureChannel,
    /** Issuer that sent the message: "Bancolombia" / "1CERO1". */
    val issuer: String,
    /** Original message, kept for the review sheet + rule tuning. Device-local only. */
    val rawBody: String,
    val type: TransactionType,
    val amount: Money,
    /** Digits parsed from the message's account/card mask; may not match a registered card. */
    val last4: String?,
    /** Registered card the mask matched, when it did. */
    val cardId: String?,
    val bank: String?,
    val merchant: String?,
    val occurredAt: Instant,
    val capturedAt: Instant,
)
