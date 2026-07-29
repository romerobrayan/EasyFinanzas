package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.CategorySpend
import dev.romerobrayan.tinto.core.domain.model.MonthComparison
import dev.romerobrayan.tinto.core.domain.model.MonthSummary
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Condenses one month of expenses into the figures a spoken summary needs: the
 * month's total, how it moved against the previous month, and the biggest
 * category.
 *
 * Sibling to [AggregateSpendUseCase] rather than a replacement — that one buckets
 * spend for the chart, this one summarizes a single month. Pure date math here,
 * not SQL, so it stays testable and locale-safe.
 */
class SummarizeMonthUseCase @Inject constructor() {

    operator fun invoke(
        transactions: List<Transaction>,
        categories: List<Category>,
        month: LocalDate,
        timeZone: TimeZone,
    ): MonthSummary {
        val monthStart = month.startOfMonth()
        val previousMonthStart = monthStart.minus(1, DateTimeUnit.MONTH)

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val thisMonth = expenses.inMonth(monthStart, timeZone)
        val total = thisMonth.fold(Money.Zero) { acc, transaction -> acc + transaction.amount }

        val previousTotal = expenses.inMonth(previousMonthStart, timeZone)
            .fold(Money.Zero) { acc, transaction -> acc + transaction.amount }

        // No previous spend means no percentage to speak of — same rule the
        // dashboard comparison chip uses, and it avoids dividing by zero.
        val comparison = previousTotal.takeIf { it.cents > 0 }?.let { previous ->
            val delta = total.cents - previous.cents
            MonthComparison(
                previousMonth = previousMonthStart,
                percent = (abs(delta) * 100.0 / previous.cents).roundToInt(),
                isDecrease = delta < 0,
            )
        }

        val categoryNamesById = categories.associate { it.id to it.name }
        val topCategory = thisMonth
            .groupingBy { it.categoryId }
            .fold(Money.Zero) { acc, transaction -> acc + transaction.amount }
            .maxByOrNull { (_, categoryTotal) -> categoryTotal.cents }
            ?.let { (categoryId, categoryTotal) ->
                CategorySpend(
                    categoryName = categoryNamesById[categoryId] ?: return@let null,
                    total = categoryTotal,
                )
            }

        return MonthSummary(
            month = monthStart,
            total = total,
            comparison = comparison,
            topCategory = topCategory,
        )
    }

    private fun List<Transaction>.inMonth(
        monthStart: LocalDate,
        timeZone: TimeZone,
    ): List<Transaction> {
        val nextMonthStart = monthStart.plus(1, DateTimeUnit.MONTH)
        return filter {
            val date = it.occurredAt.toLocalDateTime(timeZone).date
            date >= monthStart && date < nextMonthStart
        }
    }
}
