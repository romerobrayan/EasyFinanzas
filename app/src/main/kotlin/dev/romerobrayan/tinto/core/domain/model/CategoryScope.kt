package dev.romerobrayan.tinto.core.domain.model

/**
 * Whether a category applies to expenses or to incomes. Expenses and incomes
 * draw from disjoint category sets in the add/edit form (Sprint 5). Old rows
 * without a stored scope default to [EXPENSE].
 */
enum class CategoryScope { EXPENSE, INCOME }

/** The category set a movement of this [TransactionType] draws from. */
fun TransactionType.toCategoryScope(): CategoryScope = when (this) {
    TransactionType.EXPENSE -> CategoryScope.EXPENSE
    TransactionType.INCOME -> CategoryScope.INCOME
}
