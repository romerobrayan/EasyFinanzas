package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.theme.CardShape
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography

/**
 * On-palette confirmation dialog for destructive actions (delete movement /
 * card / reminder). The confirm action renders in the expense red; dismissal
 * is always "Cancelar".
 */
@Composable
fun TintoConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CardShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = title,
                style = type.sectionTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        text = {
            Text(
                text = message,
                style = type.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel, style = type.body, color = tinto.expense)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    style = type.body,
                    color = tinto.muted,
                )
            }
        },
    )
}
