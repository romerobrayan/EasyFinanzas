package dev.romerobrayan.tinto.core.data.repository

import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.domain.model.Reminder
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Demo-mode sample reminders; served by [SyncedReminderRepository] during the demo session. */
@Singleton
class InMemoryReminderRepository @Inject constructor() : ReminderRepository {

    private val reminders = MutableStateFlow(MockData.reminders)

    override fun observeReminders(): Flow<List<Reminder>> = reminders.asStateFlow()
}
