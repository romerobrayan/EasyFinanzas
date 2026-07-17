package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.theme.CardShape
import kotlinx.datetime.LocalTime

/**
 * Material time picker wrapped for Tinto's LocalTime-based forms, mirroring
 * [TintoDatePickerDialog]. The 12/24-hour format follows the device setting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TintoTimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CardShape,
        containerColor = MaterialTheme.colorScheme.surface,
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(LocalTime(timePickerState.hour, timePickerState.minute)) },
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
