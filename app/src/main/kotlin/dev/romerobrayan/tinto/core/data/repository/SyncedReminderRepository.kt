package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.data.firebase.listenAsList
import dev.romerobrayan.tinto.core.data.firebase.toReminder
import dev.romerobrayan.tinto.core.data.firebase.userCollection
import dev.romerobrayan.tinto.core.domain.model.Reminder
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Payment reminders routed by session. Real accounts start empty —
 * reminder creation UI is a later sprint.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SyncedReminderRepository @Inject constructor(
    private val auth: AuthRepository,
    private val demo: InMemoryReminderRepository,
    private val analytics: TintoAnalytics,
) : ReminderRepository {

    override fun observeReminders(): Flow<List<Reminder>> =
        auth.session.flatMapLatest { session ->
            when (session) {
                is UserSession.SignedIn ->
                    userCollection(session.user.uid, "reminders")
                        .orderBy("dueDate")
                        .listenAsList(analytics)
                        .map { docs -> docs.mapNotNull { it.toReminder() } }

                UserSession.Demo -> demo.observeReminders()
                else -> flowOf(emptyList())
            }
        }
}
