package dev.romerobrayan.tinto.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.RecurringRuleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * The "Movimientos automáticos" manager: lists the user's automation rules and
 * lets them pause/resume (isActive) or delete one. Deleting a rule stops future
 * generation; already-created movements stay in the ledger.
 */
@HiltViewModel
class RecurringRulesViewModel @Inject constructor(
    private val recurringRuleRepository: RecurringRuleRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    val uiState: StateFlow<RecurringRulesUiState> = combine(
        recurringRuleRepository.observeRules(),
        categoryRepository.observeCategories(),
    ) { rules, categories ->
        val categoriesById = categories.associateBy { it.id }
        RecurringRulesUiState(
            rules = rules
                // Active first, newest first within each group (stable sorts).
                .sortedByDescending { it.createdAt }
                .sortedByDescending { it.isActive }
                .map { rule ->
                    val category = categoriesById[rule.categoryId]
                    RecurringRuleRowUi(
                        id = rule.id,
                        title = rule.merchant ?: category?.name.orEmpty(),
                        amount = rule.amount,
                        type = rule.type,
                        frequency = rule.frequency,
                        categoryName = category?.name.orEmpty(),
                        categoryIconKey = category?.iconKey ?: "dots",
                        categoryColorHex = category?.colorHex ?: "#B99CA6",
                        nextOccurrence = rule.nextOccurrence,
                        isActive = rule.isActive,
                    )
                },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringRulesUiState())

    fun onToggleActive(ruleId: String) {
        viewModelScope.launch {
            val rule = recurringRuleRepository.observeRules().first()
                .firstOrNull { it.id == ruleId } ?: return@launch
            recurringRuleRepository.upsertRule(
                rule.copy(isActive = !rule.isActive, updatedAt = Clock.System.now()),
            )
        }
    }

    fun onDelete(ruleId: String) {
        viewModelScope.launch { recurringRuleRepository.deleteRule(ruleId) }
    }
}
