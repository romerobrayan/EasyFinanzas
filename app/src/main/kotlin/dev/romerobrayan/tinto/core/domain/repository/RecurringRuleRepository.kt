package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.RecurringRule
import kotlinx.coroutines.flow.Flow

interface RecurringRuleRepository {

    fun observeRules(): Flow<List<RecurringRule>>

    /** Create or replace by id (upsert). */
    suspend fun upsertRule(rule: RecurringRule)

    suspend fun deleteRule(ruleId: String)
}
