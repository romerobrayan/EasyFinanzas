package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.PendingTransaction
import dev.romerobrayan.tinto.core.domain.model.RawCapture

/**
 * Outcome of parsing one [RawCapture]. Only [Recognized] stages anything;
 * everything else is dropped silently — a false "detected movement" is worse
 * than a missed one.
 */
sealed interface ParseResult {

    data class Recognized(val pending: PendingTransaction) : ParseResult

    /** Known noise (PIN changes, payment requests…), deliberately dropped. */
    data class Ignored(val reason: String) : ParseResult

    /** No rule matched — dropped, debug-logged for rule tuning. */
    data object Unrecognized : ParseResult
}

/** Turns raw bank messages into staged parses using per-issuer rule sets. */
interface TransactionParser {

    fun parse(raw: RawCapture): ParseResult
}
