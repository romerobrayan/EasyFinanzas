package dev.romerobrayan.tinto.feature.reminders

import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.Recurrence
import kotlinx.datetime.LocalDate

data class ReminderUi(
    val id: String,
    val title: String,
    val amount: Money?,
    /** Locale-formatted due date core, e.g. "15 de julio". */
    val dueDateLabel: String,
    val recurrence: Recurrence,
    val isPaid: Boolean,
)

/** The reminder bottom-sheet form; non-null while the sheet is open. */
data class ReminderFormUiState(
    /** null = adding a new reminder; non-null = editing that reminder. */
    val editingReminderId: String? = null,
    val title: String = "",
    /** Raw peso digits as typed; formatting happens in the amount field. */
    val amountDigits: String = "",
    val dueDate: LocalDate,
    val recurrence: Recurrence = Recurrence.NONE,
    /** "Marcar como pagado" shows only for existing, still-unpaid reminders. */
    val canMarkPaid: Boolean = false,
    /** Only populated after a submit attempt, so the form starts clean. */
    val errors: Set<ReminderFormValidator.Error> = emptySet(),
)

data class RemindersUiState(
    val upcoming: List<ReminderUi> = emptyList(),
    val paid: List<ReminderUi> = emptyList(),
    val form: ReminderFormUiState? = null,
)
