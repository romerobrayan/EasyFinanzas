package dev.romerobrayan.tinto.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.Recurrence
import dev.romerobrayan.tinto.core.domain.model.Reminder
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
import dev.romerobrayan.tinto.core.domain.usecase.markAsPaid
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val analytics: TintoAnalytics,
) : ViewModel() {

    private val timeZone = TimeZone.currentSystemDefault()

    private data class ReminderForm(
        val editingReminderId: String? = null,
        val title: String = "",
        val amountDigits: String = "",
        val dueDate: LocalDate,
        val recurrence: Recurrence = Recurrence.NONE,
        /** Paid state of the reminder being edited; preserved on save. */
        val editingIsPaid: Boolean = false,
        val submitAttempted: Boolean = false,
    )

    /** Non-null while the reminder bottom sheet is open. */
    private val form = MutableStateFlow<ReminderForm?>(null)

    val uiState: StateFlow<RemindersUiState> = combine(
        reminderRepository.observeReminders(),
        form,
    ) { reminders, currentForm ->
        val (paid, upcoming) = reminders
            .sortedBy { it.dueDate }
            .partition { it.isPaid }
        RemindersUiState(
            upcoming = upcoming.map { it.toUi() },
            paid = paid.map { it.toUi() },
            form = currentForm?.let {
                ReminderFormUiState(
                    editingReminderId = it.editingReminderId,
                    title = it.title,
                    amountDigits = it.amountDigits,
                    dueDate = it.dueDate,
                    recurrence = it.recurrence,
                    canMarkPaid = it.editingReminderId != null && !it.editingIsPaid,
                    errors = if (it.submitAttempted) validate(it) else emptySet(),
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    fun onAddClick() {
        form.value = ReminderForm(dueDate = Clock.System.todayIn(timeZone))
    }

    fun onReminderClick(reminderId: String) {
        viewModelScope.launch {
            val reminder = reminderRepository.observeReminders().first()
                .firstOrNull { it.id == reminderId } ?: return@launch
            form.value = ReminderForm(
                editingReminderId = reminder.id,
                title = reminder.title,
                amountDigits = reminder.amount?.let { (it.cents / CENTS_PER_PESO).toString() }.orEmpty(),
                dueDate = reminder.dueDate,
                recurrence = reminder.recurrence,
                editingIsPaid = reminder.isPaid,
            )
        }
    }

    fun onFormDismiss() {
        form.value = null
    }

    fun onTitleChanged(value: String) {
        form.update { it?.copy(title = value) }
    }

    fun onAmountChanged(raw: String) {
        form.update {
            it?.copy(
                amountDigits = raw.filter(Char::isDigit).trimStart('0').take(MAX_AMOUNT_DIGITS),
            )
        }
    }

    fun onDueDateChanged(date: LocalDate) {
        form.update { it?.copy(dueDate = date) }
    }

    fun onRecurrenceChanged(recurrence: Recurrence) {
        form.update { it?.copy(recurrence = recurrence) }
    }

    fun onSubmit() {
        val currentForm = form.value ?: return
        if (validate(currentForm).isNotEmpty()) {
            form.update { it?.copy(submitAttempted = true) }
            return
        }
        viewModelScope.launch {
            val reminder = Reminder(
                id = currentForm.editingReminderId ?: UUID.randomUUID().toString(),
                title = currentForm.title.trim(),
                amount = currentForm.amountDigits.toLongOrNull()
                    ?.takeIf { it > 0 }
                    ?.let(Money::ofPesos),
                dueDate = currentForm.dueDate,
                recurrence = currentForm.recurrence,
                isPaid = currentForm.editingReminderId != null && currentForm.editingIsPaid,
            )
            if (currentForm.editingReminderId == null) {
                reminderRepository.addReminder(reminder)
                analytics.logAddReminder(reminder.recurrence.name)
            } else {
                reminderRepository.updateReminder(reminder)
            }
            form.value = null
        }
    }

    /** Deletes the reminder being edited (behind the confirm dialog). */
    fun onDelete() {
        val reminderId = form.value?.editingReminderId ?: return
        viewModelScope.launch {
            reminderRepository.deleteReminder(reminderId)
            form.value = null
        }
    }

    /**
     * Marks the reminder being edited as paid, applying the recurrence
     * rollover from core/domain: one-off completes, recurring reschedules.
     * Acts on the stored reminder — unsaved form edits are discarded.
     */
    fun onMarkPaid() {
        val reminderId = form.value?.editingReminderId ?: return
        viewModelScope.launch {
            val reminder = reminderRepository.observeReminders().first()
                .firstOrNull { it.id == reminderId } ?: return@launch
            reminderRepository.updateReminder(reminder.markAsPaid())
            analytics.logReminderPaid(reminder.recurrence.name)
            form.value = null
        }
    }

    private fun validate(currentForm: ReminderForm) =
        ReminderFormValidator.validate(title = currentForm.title)

    private fun Reminder.toUi() = ReminderUi(
        id = id,
        title = title,
        amount = amount,
        dueDateLabel = Dates.dayOfMonthName(dueDate),
        recurrence = recurrence,
        isPaid = isPaid,
    )

    private companion object {
        const val MAX_AMOUNT_DIGITS = 10
        const val CENTS_PER_PESO = 100L
    }
}
