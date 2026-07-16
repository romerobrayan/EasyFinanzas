package dev.romerobrayan.tinto.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.component.TintoConfirmDialog
import dev.romerobrayan.tinto.core.designsystem.component.tintoTextFieldColors
import dev.romerobrayan.tinto.core.designsystem.theme.ButtonShape
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.SheetShape

/**
 * Bottom-sheet form for adding or editing a registered card: banco (required),
 * últimos 4 dígitos (exactly four), etiqueta opcional. Editing adds an
 * "Eliminar tarjeta" action behind a confirmation dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardFormSheet(
    form: CardFormUiState,
    onBankChanged: (String) -> Unit,
    onLast4Changed: (String) -> Unit,
    onLabelChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    val isEditing = form.editingCardId != null
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
                    if (isEditing) R.string.card_form_title_edit else R.string.card_form_title_new,
                ),
                style = type.screenTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(16.dp))
            TextField(
                value = form.bank,
                onValueChange = onBankChanged,
                label = { Text(stringResource(R.string.card_form_bank_label)) },
                singleLine = true,
                colors = tintoTextFieldColors(),
                shape = ButtonShape,
                modifier = Modifier.fillMaxWidth(),
            )
            if (CardFormValidator.Error.BANK_REQUIRED in form.errors) {
                Spacer(Modifier.height(4.dp))
                ErrorText(stringResource(R.string.card_form_error_bank))
            }

            Spacer(Modifier.height(12.dp))
            TextField(
                value = form.last4,
                onValueChange = onLast4Changed,
                label = { Text(stringResource(R.string.card_form_last4_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = tintoTextFieldColors(),
                shape = ButtonShape,
                modifier = Modifier.fillMaxWidth(),
            )
            if (CardFormValidator.Error.LAST4_INVALID in form.errors) {
                Spacer(Modifier.height(4.dp))
                ErrorText(stringResource(R.string.card_form_error_last4))
            }

            Spacer(Modifier.height(12.dp))
            TextField(
                value = form.label,
                onValueChange = onLabelChanged,
                label = { Text(stringResource(R.string.card_form_label_label)) },
                singleLine = true,
                colors = tintoTextFieldColors(),
                shape = ButtonShape,
                modifier = Modifier.fillMaxWidth(),
            )

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
                    text = stringResource(R.string.card_form_save),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                )
            }
            if (isEditing) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = stringResource(R.string.card_form_delete),
                        style = type.body.copy(fontWeight = FontWeight.Medium),
                        color = tinto.expense,
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        TintoConfirmDialog(
            title = stringResource(R.string.card_delete_confirm_title),
            message = stringResource(R.string.card_delete_confirm_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        style = LocalTintoTypography.current.caption,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Start,
    )
}
