package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.domain.model.Reminder
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Demo-mode sample reminders; served by [SyncedReminderRepository] during the demo session. */
@Singleton
class InMemoryReminderRepository @Inject constructor() : ReminderRepository {

    private val reminders = MutableStateFlow(MockData.reminders)

    override fun observeReminders(): Flow<List<Reminder>> = reminders.asStateFlow()

    override suspend fun addReminder(reminder: Reminder) {
        reminders.update { it + reminder }
    }

    override suspend fun updateReminder(reminder: Reminder) {
        reminders.update { list -> list.map { if (it.id == reminder.id) reminder else it } }
    }

    override suspend fun deleteReminder(reminderId: String) {
        reminders.update { list -> list.filterNot { it.id == reminderId } }
    }
}
