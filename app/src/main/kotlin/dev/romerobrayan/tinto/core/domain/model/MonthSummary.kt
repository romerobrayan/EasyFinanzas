package dev.romerobrayan.tinto.core.domain.model

import kotlinx.datetime.LocalDate

/**
 * What one month of spending amounts to, as pure numbers — no strings, no
 * formatting, no locale. Turning this into a sentence is the narrator's job, so
 * the domain never reaches for Android resources.
 */
data class MonthSummary(
    /** First day of the month being summarized. */
    val month: LocalDate,
    /** Total EXPENSE for the month. */
    val total: Money,
    /** Null when the previous month has no spend to compare against. */
    val comparison: MonthComparison?,
    /** Null when the month has no expenses. */
    val topCategory: CategorySpend?,
)

/** Change vs the previous month, as a whole percentage of that month's total. */
data class MonthComparison(
    val previousMonth: LocalDate,
    val percent: Int,
    val isDecrease: Boolean,
)

/** The category the user spent most on, already resolved to its display name. */
data class CategorySpend(
    val categoryName: String,
    val total: Money,
)
