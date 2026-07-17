package dev.romerobrayan.tinto.core.data.capture.parser

import dev.romerobrayan.tinto.core.domain.model.TransactionType

/**
 * Everything the parser knows about one issuer, as data. Adding a bank is
 * adding one of these plus its senders — never a new `when` branch in the
 * parser core (ARCHITECTURE.md rule).
 */
class IssuerRule(
    val issuer: String,
    /** SMS shortcodes (later sprints: notification package names) this rule owns. */
    val senders: Set<String>,
    val decimalConvention: DecimalConvention,
    /** Checked before templates: known noise → [dev.romerobrayan.tinto.core.domain.repository.ParseResult.Ignored]. */
    val dropPatterns: List<Regex>,
    /** Ordered message templates; the first whose pattern matches wins. */
    val templates: List<MessageTemplate>,
    /** Date layouts this issuer writes; first match in the body wins. */
    val dateLayouts: List<DateLayout>,
    /** How this issuer masks the user's account/card. Group 1 = digits; the last 4 are kept. */
    val accountMaskPatterns: List<Regex>,
) {

    /** Matches raw sender addresses, tolerating a `+57` country prefix. */
    fun matchesSender(sender: String): Boolean {
        if (sender in senders) return true
        val digits = sender.filter(Char::isDigit)
        return digits in senders || digits.removePrefix(COLOMBIA_COUNTRY_CODE) in senders
    }

    private companion object {
        const val COLOMBIA_COUNTRY_CODE = "57"
    }
}

/**
 * One recognizable message shape: the regex that identifies it, the direction
 * it implies, and the builder that pulls the counterparty out of the match.
 */
class MessageTemplate(
    val id: String,
    val pattern: Regex,
    val type: TransactionType,
    /** Counterparty for `merchant`; defaults to none (e.g. a card-bill payment). */
    val merchant: (MatchResult) -> String? = { null },
)
