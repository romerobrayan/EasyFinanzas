package dev.romerobrayan.tinto.feature.recurring

import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.TransactionFrequency
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate

/** One automation rule as rendered in the management list. */
data class RecurringRuleRowUi(
    val id: String,
    val title: String,
    val amount: Money,
    val type: TransactionType,
    val frequency: TransactionFrequency,
    val categoryName: String,
    val categoryIconKey: String,
    val categoryColorHex: String,
    val nextOccurrence: LocalDate,
    val isActive: Boolean,
)

data class RecurringRulesUiState(
    val rules: List<RecurringRuleRowUi> = emptyList(),
)
