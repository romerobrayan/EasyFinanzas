package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.TransactionType

/**
 * One recognizable message shape within an issuer rule set. Templates use
 * named groups the parser core reads uniformly: `amount` (required), and
 * optionally `merchant`, `last4`, `date`, `time`. Everything template-specific
 * lives in the regex — the parser core never branches per issuer.
 */
data class MessageTemplate(
    val regex: Regex,
    val type: TransactionType,
)

/** A known-noise shape that is deliberately dropped (never staged). */
data class DropPattern(
    val regex: Regex,
    val reason: String,
)

/**
 * The per-issuer rule set: which senders it owns, how it writes amounts, the
 * ordered transaction templates, and its drop patterns. Adding a bank is
 * adding one of these to [IssuerRules.all] — never a new `when` branch.
 */
data class IssuerRule(
    /** Issuer label surfaced in the inbox ("SMS · Bancolombia"). */
    val issuer: String,
    /** Bank name written into promoted movements; null when unknown. */
    val bank: String?,
    /** Senders this rule owns: SMS shortcodes (digits) or app package names. */
    val senders: Set<String>,
    val amountConvention: AmountConvention,
    /** Checked in order; the first match wins. */
    val templates: List<MessageTemplate>,
    /** Checked before templates; a hit drops the message with a reason. */
    val dropPatterns: List<DropPattern> = emptyList(),
)
