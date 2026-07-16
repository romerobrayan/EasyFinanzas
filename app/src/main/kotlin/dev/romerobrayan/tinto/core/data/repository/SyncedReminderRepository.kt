package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.data.firebase.listenAsList
import dev.romerobrayan.tinto.core.data.firebase.toFirestoreMap
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
 * Payment reminders routed by session: signed-in reads/writes
 * `users/{uid}/reminders`, demo mode uses the in-memory samples. Writes are
 * fire-and-forget so reminders keep working offline.
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

    override suspend fun addReminder(reminder: Reminder) {
        when (val session = auth.session.value) {
            is UserSession.SignedIn ->
                userCollection(session.user.uid, "reminders")
                    .document(reminder.id)
                    .set(reminder.toFirestoreMap())

            UserSession.Demo -> demo.addReminder(reminder)
            else -> Unit
        }
    }

    override suspend fun updateReminder(reminder: Reminder) {
        when (val session = auth.session.value) {
            is UserSession.SignedIn ->
                userCollection(session.user.uid, "reminders")
                    .document(reminder.id)
                    .set(reminder.toFirestoreMap())

            UserSession.Demo -> demo.updateReminder(reminder)
            else -> Unit
        }
    }

    override suspend fun deleteReminder(reminderId: String) {
        when (val session = auth.session.value) {
            is UserSession.SignedIn ->
                userCollection(session.user.uid, "reminders")
                    .document(reminderId)
                    .delete()

            UserSession.Demo -> demo.deleteReminder(reminderId)
            else -> Unit
        }
    }
}
