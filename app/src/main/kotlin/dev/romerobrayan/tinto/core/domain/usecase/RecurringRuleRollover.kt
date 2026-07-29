package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.RecurringRule
import dev.romerobrayan.tinto.core.domain.model.TransactionFrequency
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Advances a rule's [RecurringRule.nextOccurrence] by exactly one period. Pure
 * and total; month/year math clamps the day-of-month for free via
 * kotlinx-datetime (Jan 31 + 1 month = Feb 28/29). Pinned by
 * RecurringRuleRolloverTest.
 */
fun RecurringRule.advanced(): RecurringRule =
    copy(nextOccurrence = nextOccurrence.plusFrequency(frequency))

/**
 * The next occurrence date one [frequency] after this one.
 *
 * SEMIMONTHLY ("quincenal") lands on fixed calendar dates — the 15th and the
 * last day of each month — so it routes through [nextHalfMonth] rather than a
 * fixed day count. For a date already snapped to a half-month slot (the 15th
 * or the last day) this yields the alternating series 15 → last → 15(next).
 */
fun LocalDate.plusFrequency(frequency: TransactionFrequency): LocalDate = when (frequency) {
    TransactionFrequency.DAILY -> plus(1, DateTimeUnit.DAY)
    TransactionFrequency.WEEKLY -> plus(1, DateTimeUnit.WEEK)
    TransactionFrequency.MONTHLY -> plus(1, DateTimeUnit.MONTH)
    TransactionFrequency.SEMIMONTHLY -> nextHalfMonth()
}

/**
 * The next half-month slot strictly after this date. Slots are the 15th and
 * the last day of each month:
 *  - day < 15 → the 15th of the same month;
 *  - 15 ≤ day < last day → the last day of the same month;
 *  - day == last day → the 15th of the next month.
 *
 * Used both to snap a rule's first [RecurringRule.nextOccurrence] from an
 * arbitrary anchor and to advance an already-snapped occurrence.
 */
fun LocalDate.nextHalfMonth(): LocalDate {
    val lastDay = lastDayOfMonth()
    return when {
        dayOfMonth < 15 -> LocalDate(year, monthNumber, 15)
        dayOfMonth < lastDay -> LocalDate(year, monthNumber, lastDay)
        else -> fifteenthOfNextMonth()
    }
}

/** Last day-of-month (28–31), computed with month clamping. */
private fun LocalDate.lastDayOfMonth(): Int =
    LocalDate(year, monthNumber, 1)
        .plus(1, DateTimeUnit.MONTH)
        .minus(1, DateTimeUnit.DAY)
        .dayOfMonth

private fun LocalDate.fifteenthOfNextMonth(): LocalDate {
    val firstOfNext = LocalDate(year, monthNumber, 1).plus(1, DateTimeUnit.MONTH)
    return LocalDate(firstOfNext.year, firstOfNext.monthNumber, 15)
}
