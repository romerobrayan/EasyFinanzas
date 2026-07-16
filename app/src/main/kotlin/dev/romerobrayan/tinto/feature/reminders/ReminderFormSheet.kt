package dev.romerobrayan.tinto.feature.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.common.MoneyFormat
import dev.romerobrayan.tinto.core.designsystem.component.TintoConfirmDialog
import dev.romerobrayan.tinto.core.designsystem.component.TintoDatePickerDialog
import dev.romerobrayan.tinto.core.designsystem.component.TintoSelectorPill
import dev.romerobrayan.tinto.core.designsystem.component.tintoTextFieldColors
import dev.romerobrayan.tinto.core.designsystem.theme.ButtonShape
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape
import dev.romerobrayan.tinto.core.designsystem.theme.SheetShape
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.Recurrence
import kotlinx.datetime.LocalDate

/**
 * Bottom-sheet form for creating or editing a payment reminder: título
 * (required), monto opcional, fecha de vencimiento, recurrencia. Editing
 * adds "Marcar como pagado" (recurrence-aware rollover in the ViewModel)
 * and "Eliminar" behind a confirmation dialog.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ReminderFormSheet(
    form: ReminderFormUiState,
    onTitleChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onDueDateChanged: (LocalDate) -> Unit,
    onRecurrenceChanged: (Recurrence) -> Unit,
    onSubmit: () -> Unit,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    val isEditing = form.editingReminderId != null
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = SheetShape,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(
                    if (isEditing) {
                        R.string.reminder_form_title_edit
                    } else {
                        R.string.reminder_form_title_new
                    },
                ),
                style = type.screenTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(16.dp))
            TextField(
                value = form.title,
                onValueChange = onTitleChanged,
                label = { Text(stringResource(R.string.reminder_form_title_label)) },
                singleLine = true,
                colors = tintoTextFieldColors(),
                shape = ButtonShape,
                modifier = Modifier.fillMaxWidth(),
            )
            if (ReminderFormValidator.Error.TITLE_REQUIRED in form.errors) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.reminder_form_error_title),
                    style = type.caption,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(12.dp))
            AmountField(
                amountDigits = form.amountDigits,
                onAmountChanged = onAmountChanged,
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.reminder_form_due_label),
                style = type.sectionTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Event,
                    contentDescription = null,
                    tint = tinto.gold,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = Dates.dayOfMonthName(form.dueDate),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.reminder_form_recurrence_label),
                style = type.sectionTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Recurrence.entries.forEach { recurrence ->
                    TintoSelectorPill(
                        label = recurrenceLabel(recurrence),
                        selected = form.recurrence == recurrence,
                        onClick = { onRecurrenceChanged(recurrence) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSubmit,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(R.string.reminder_form_save),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                )
            }
            if (form.canMarkPaid) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onMarkPaid,
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = tinto.gold,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reminder_mark_paid),
                        style = type.body.copy(fontWeight = FontWeight.Medium),
                    )
                }
            }
            if (isEditing) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        style = type.body.copy(fontWeight = FontWeight.Medium),
                        color = tinto.expense,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        TintoDatePickerDialog(
            initialDate = form.dueDate,
            onConfirm = { date ->
                onDueDateChanged(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showDeleteConfirm) {
        TintoConfirmDialog(
            title = stringResource(R.string.reminder_delete_confirm_title),
            message = stringResource(R.string.reminder_delete_confirm_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

/** Optional amount, formatted through the app money format while typing. */
@Composable
private fun AmountField(
    amountDigits: String,
    onAmountChanged: (String) -> Unit,
) {
    val formatted = if (amountDigits.isEmpty()) {
        ""
    } else {
        MoneyFormat.format(Money.ofPesos(amountDigits.toLong()))
    }
    TextField(
        value = TextFieldValue(text = formatted, selection = TextRange(formatted.length)),
        onValueChange = { onAmountChanged(it.text) },
        label = { Text(stringResource(R.string.reminder_form_amount_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = tintoTextFieldColors(),
        shape = ButtonShape,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun recurrenceLabel(recurrence: Recurrence): String = stringResource(
    when (recurrence) {
        Recurrence.NONE -> R.string.recurrence_none
        Recurrence.WEEKLY -> R.string.recurrence_weekly
        Recurrence.MONTHLY -> R.string.recurrence_monthly
        Recurrence.YEARLY -> R.string.recurrence_yearly
    },
)
