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
 * On-palette confirmation dialog. Destructive confirmations (delete movement /
 * card / reminder — the default) render the action in the expense red;
 * affirmative ones (e.g. enabling capture) render it in gold. Dismissal is
 * always "Cancelar".
 */
@Composable
fun TintoConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
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
                Text(
                    text = confirmLabel,
                    style = type.body,
                    color = if (destructive) tinto.expense else tinto.gold,
                )
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
