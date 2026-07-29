package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.RecurringRule
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

/** The movements a rule owes as of [today], plus the rule advanced past them. */
data class DueOccurrences(
    val transactions: List<Transaction>,
    val advancedRule: RecurringRule,
)

/**
 * Catch-up generator (pure): every occurrence with `nextOccurrence <= today`
 * becomes a [Transaction], all in one in-memory pass, returning the rule
 * advanced so its `nextOccurrence` is strictly after [today]. Doing the whole
 * catch-up before any write avoids a re-emission loop in the coordinator.
 *
 * Each occurrence's id is deterministic — `auto-{ruleId}-{yyyyMMdd}` — so
 * running this twice on the same day upserts the same rows instead of
 * duplicating them (idempotent by construction). An inactive rule yields
 * nothing and is returned unchanged.
 */
fun generateDueOccurrences(
    rule: RecurringRule,
    today: LocalDate,
    timeZone: TimeZone,
    now: Instant,
): DueOccurrences {
    if (!rule.isActive) return DueOccurrences(emptyList(), rule)

    val transactions = mutableListOf<Transaction>()
    var current = rule
    var guard = 0
    while (current.nextOccurrence <= today && guard < MAX_CATCH_UP) {
        val date = current.nextOccurrence
        transactions += Transaction(
            id = occurrenceId(rule.id, date),
            type = rule.type,
            amount = rule.amount,
            method = rule.method,
            cardId = rule.cardId,
            bank = rule.bank,
            categoryId = rule.categoryId,
            merchant = rule.merchant,
            occurredAt = date.atTime(12, 0).toInstant(timeZone),
            source = TransactionSource.RECURRING,
            createdAt = now,
            updatedAt = now,
        )
        current = current.advanced()
        guard++
    }
    return DueOccurrences(transactions, current)
}

/** Deterministic per-occurrence id: stable across runs → upsert-safe. */
fun occurrenceId(ruleId: String, date: LocalDate): String =
    "auto-$ruleId-%04d%02d%02d".format(date.year, date.monthNumber, date.dayOfMonth)

/** Safety bound so a bad rule can never spin forever (≈13 years daily). */
private const val MAX_CATCH_UP = 5_000
