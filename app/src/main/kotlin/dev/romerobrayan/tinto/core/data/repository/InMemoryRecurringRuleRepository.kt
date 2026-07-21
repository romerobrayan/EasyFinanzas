package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.domain.model.RecurringRule
import dev.romerobrayan.tinto.core.domain.repository.RecurringRuleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Demo-mode automation rules; served by [SyncedRecurringRuleRepository] during
 * the demo session. Starts empty — the demo persona has no rules until the
 * user creates one. Nothing survives a process restart, by design.
 */
@Singleton
class InMemoryRecurringRuleRepository @Inject constructor() : RecurringRuleRepository {

    private val rules = MutableStateFlow<List<RecurringRule>>(emptyList())

    override fun observeRules(): Flow<List<RecurringRule>> = rules.asStateFlow()

    override suspend fun upsertRule(rule: RecurringRule) {
        rules.update { list ->
            if (list.any { it.id == rule.id }) {
                list.map { if (it.id == rule.id) rule else it }
            } else {
                list + rule
            }
        }
    }

    override suspend fun deleteRule(ruleId: String) {
        rules.update { list -> list.filterNot { it.id == ruleId } }
    }
}
