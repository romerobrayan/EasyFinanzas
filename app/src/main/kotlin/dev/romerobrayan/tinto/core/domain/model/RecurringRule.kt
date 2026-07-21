package dev.romerobrayan.tinto.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * A user-created automation: it materializes movements (expense or income) on
 * a cadence, straight into the ledger — no review. This is not a parsed
 * capture, so the "never auto-commit" rule (which guards *parses*) doesn't
 * apply; the user built this rule on purpose.
 *
 * [nextOccurrence] is the next date to generate (already snapped for
 * SEMIMONTHLY to the 15th / last day). Each generated movement uses a
 * deterministic id derived from this rule + the occurrence date, so a
 * catch-up that runs twice on the same day never doubles the ledger.
 */
data class RecurringRule(
    val id: String,
    val type: TransactionType,
    val amount: Money,
    val method: PaymentMethod,
    /** Registered card this rule bills to; null when cash/transfer. */
    val cardId: String?,
    val bank: String?,
    val categoryId: String,
    /** Free-text description carried onto every generated movement. */
    val merchant: String?,
    val frequency: TransactionFrequency,
    /** The first occurrence's date — the movement the user entered. */
    val anchorDate: LocalDate,
    /** The next date pending generation. */
    val nextOccurrence: LocalDate,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
