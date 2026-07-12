package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {

    fun observeReminders(): Flow<List<Reminder>>
}
