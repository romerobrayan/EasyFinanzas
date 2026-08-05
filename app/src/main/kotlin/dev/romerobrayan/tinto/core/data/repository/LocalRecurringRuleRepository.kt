package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.data.local.RecurringRuleDao
import dev.romerobrayan.tinto.core.data.local.toDomain
import dev.romerobrayan.tinto.core.data.local.toEntity
import dev.romerobrayan.tinto.core.domain.model.RecurringRule
import dev.romerobrayan.tinto.core.domain.repository.RecurringRuleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Automation rules of a no-account user; device-local. The catch-up generator
 * observes the bound repository, so a rule keeps materializing movements after
 * a reboot the same way it does for a signed-in account.
 */
@Singleton
class LocalRecurringRuleRepository @Inject constructor(
    private val dao: RecurringRuleDao,
) : RecurringRuleRepository {

    override fun observeRules(): Flow<List<RecurringRule>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun upsertRule(rule: RecurringRule) {
        dao.upsert(rule.toEntity())
    }

    override suspend fun deleteRule(ruleId: String) {
        dao.delete(ruleId)
    }
}
