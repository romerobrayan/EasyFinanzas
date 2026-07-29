package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.data.firebase.listenAsList
import dev.romerobrayan.tinto.core.data.firebase.toFirestoreMap
import dev.romerobrayan.tinto.core.data.firebase.toRecurringRule
import dev.romerobrayan.tinto.core.data.firebase.userCollection
import dev.romerobrayan.tinto.core.domain.model.RecurringRule
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import dev.romerobrayan.tinto.core.domain.repository.RecurringRuleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Automation rules routed by session: signed-in reads/writes
 * `users/{uid}/recurring_rules`, demo mode uses the in-memory list. Writes are
 * fire-and-forget (offline-friendly), upsert by id. Signed-out → empty, so the
 * generator materializes nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SyncedRecurringRuleRepository @Inject constructor(
    private val auth: AuthRepository,
    private val demo: InMemoryRecurringRuleRepository,
    private val analytics: TintoAnalytics,
) : RecurringRuleRepository {

    override fun observeRules(): Flow<List<RecurringRule>> =
        auth.session.flatMapLatest { session ->
            when (session) {
                is UserSession.SignedIn ->
                    userCollection(session.user.uid, "recurring_rules")
                        .listenAsList(analytics)
                        .map { docs -> docs.mapNotNull { it.toRecurringRule() } }

                UserSession.Demo -> demo.observeRules()
                else -> flowOf(emptyList())
            }
        }

    override suspend fun upsertRule(rule: RecurringRule) {
        when (val session = auth.session.value) {
            is UserSession.SignedIn ->
                userCollection(session.user.uid, "recurring_rules")
                    .document(rule.id)
                    .set(rule.toFirestoreMap())

            UserSession.Demo -> demo.upsertRule(rule)
            else -> Unit
        }
    }

    override suspend fun deleteRule(ruleId: String) {
        when (val session = auth.session.value) {
            is UserSession.SignedIn ->
                userCollection(session.user.uid, "recurring_rules")
                    .document(ruleId)
                    .delete()

            UserSession.Demo -> demo.deleteRule(ruleId)
            else -> Unit
        }
    }
}
