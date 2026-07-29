package dev.romerobrayan.tinto.core.data.recurring

import dev.romerobrayan.tinto.core.domain.model.RecurringRule
import dev.romerobrayan.tinto.core.domain.repository.RecurringRuleRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import dev.romerobrayan.tinto.core.domain.usecase.generateDueOccurrences
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Materializes automation rules into the ledger. Observes the (session-routed)
 * [RecurringRuleRepository] from app start: on every emission it runs the
 * catch-up generator for each active rule, writes the due movements through
 * [TransactionRepository], and persists the advanced rule.
 *
 * Observing the repository (rather than hooking rule CRUD) makes demo and
 * signed-in identical, and covers multi-device edits arriving via Firestore
 * sync and signed-out (empty emission → nothing to do) for free — mirrors the
 * reminder coordinator.
 *
 * No infinite loop: the generator advances a rule past `today` in a single
 * in-memory pass before any write, so the one re-emission caused by persisting
 * the advanced rule finds nothing due. Writes are idempotent by deterministic
 * occurrence id, so overlapping emissions can't double the ledger either.
 */
@Singleton
class RecurringTransactionCoordinator @Inject constructor(
    private val recurringRuleRepository: RecurringRuleRepository,
    private val transactionRepository: TransactionRepository,
    private val applicationScope: CoroutineScope,
) {

    private val started = AtomicBoolean(false)

    @Volatile
    private var latestRules: List<RecurringRule> = emptyList()

    /** Called once from Application.onCreate; extra calls are no-ops. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        applicationScope.launch {
            recurringRuleRepository.observeRules().collect { rules ->
                latestRules = rules
                materialize(rules)
            }
        }
    }

    /** Re-runs the catch-up against the cached list (after boot/clock changes). */
    fun reconcileNow() {
        applicationScope.launch { materialize(latestRules) }
    }

    private suspend fun materialize(rules: List<RecurringRule>) {
        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(timeZone).date
        rules.asSequence().filter { it.isActive }.forEach { rule ->
            val due = generateDueOccurrences(rule, today, timeZone, now)
            if (due.transactions.isEmpty()) return@forEach
            due.transactions.forEach { transactionRepository.addTransaction(it) }
            recurringRuleRepository.upsertRule(due.advancedRule.copy(updatedAt = now))
        }
    }
}
