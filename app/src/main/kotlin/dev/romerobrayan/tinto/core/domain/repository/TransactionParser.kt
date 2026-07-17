package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.RawCapture

/**
 * Outcome of parsing one [RawCapture]. Only [Recognized] ever stages — a
 * capture must yield both an amount and a direction; everything else is
 * dropped, never surfaced (a false "detected movement" is worse than a
 * missed one).
 */
sealed interface ParseResult {

    data class Recognized(val pending: PendingTransaction) : ParseResult

    /** Known noise (PIN changes, payment requests, marketing) — dropped by design. */
    data class Ignored(val reason: String) : ParseResult

    /** No rule matched — dropped, debug-logged for rule tuning. */
    data object Unrecognized : ParseResult
}

interface TransactionParser {

    /**
     * Pure parse of one captured message. [registeredCards] lets a recognized
     * capture auto-match its parsed mask to a registered card's last 4 digits.
     */
    fun parse(raw: RawCapture, registeredCards: List<Card>): ParseResult
}
