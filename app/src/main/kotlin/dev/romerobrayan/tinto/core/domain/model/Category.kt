package dev.romerobrayan.tinto.core.domain.model

data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val isSystem: Boolean,
    /** Expense vs income category set; defaults to expense for legacy callers. */
    val scope: CategoryScope = CategoryScope.EXPENSE,
)
