package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.data.local.ReminderDao
import dev.romerobrayan.tinto.core.data.local.toDomain
import dev.romerobrayan.tinto.core.data.local.toEntity
import dev.romerobrayan.tinto.core.domain.model.Reminder
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Payment reminders of a no-account user; device-local. The alarm coordinator
 * observes the bound repository, so notifications work here exactly as they do
 * for a signed-in account.
 */
@Singleton
class LocalReminderRepository @Inject constructor(
    private val dao: ReminderDao,
) : ReminderRepository {

    override fun observeReminders(): Flow<List<Reminder>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun addReminder(reminder: Reminder) {
        dao.upsert(reminder.toEntity())
    }

    override suspend fun updateReminder(reminder: Reminder) {
        dao.upsert(reminder.toEntity())
    }

    override suspend fun deleteReminder(reminderId: String) {
        dao.delete(reminderId)
    }
}
