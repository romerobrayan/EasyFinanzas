package dev.romerobrayan.tinto.feature.reminders

import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.Recurrence

data class ReminderUi(
    val id: String,
    val title: String,
    val amount: Money?,
    /** Locale-formatted due date core, e.g. "15 de julio". */
    val dueDateLabel: String,
    val recurrence: Recurrence,
    val isPaid: Boolean,
)

data class RemindersUiState(
    val upcoming: List<ReminderUi> = emptyList(),
    val paid: List<ReminderUi> = emptyList(),
)
