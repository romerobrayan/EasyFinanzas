package dev.romerobrayan.tinto.core.domain.model

import kotlinx.datetime.Instant

/**
 * A parsed capture staged for user review. Device-local until the user
 * confirms it into the ledger — a parse is never a [Transaction] on its own
 * (CLAUDE.md guardrail: never auto-commit).
 *
 * Deliberately has no category: the parser never guesses one; the user
 * assigns it in the review sheet.
 */
data class PendingTransaction(
    val id: String,
    val channel: CaptureChannel,
    /** Issuing bank per the matched rule set: "Bancolombia", "1CERO1". */
    val issuer: String,
    /** Original message, kept for the review sheet + rule tuning; device-local only. */
    val rawBody: String,
    val type: TransactionType,
    val amount: Money,
    /** Parsed account/card mask digits; may not match a registered card. */
    val last4: String?,
    /** Set when [last4] matched a registered [Card] at parse time. */
    val cardId: String?,
    val bank: String?,
    val merchant: String?,
    val occurredAt: Instant,
    val capturedAt: Instant,
)
