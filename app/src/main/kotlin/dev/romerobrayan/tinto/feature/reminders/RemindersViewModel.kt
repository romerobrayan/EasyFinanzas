package dev.romerobrayan.tinto.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.domain.model.Reminder
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RemindersViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
) : ViewModel() {

    val uiState: StateFlow<RemindersUiState> = reminderRepository.observeReminders()
        .map { reminders ->
            val (paid, upcoming) = reminders
                .sortedBy { it.dueDate }
                .partition { it.isPaid }
            RemindersUiState(
                upcoming = upcoming.map { it.toUi() },
                paid = paid.map { it.toUi() },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    private fun Reminder.toUi() = ReminderUi(
        id = id,
        title = title,
        amount = amount,
        dueDateLabel = Dates.dayOfMonthName(dueDate),
        recurrence = recurrence,
        isPaid = isPaid,
    )
}
