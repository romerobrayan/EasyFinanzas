package dev.romerobrayan.tinto.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.common.MovementUi
import dev.romerobrayan.tinto.core.designsystem.theme.ButtonShape
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.SheetShape
import dev.romerobrayan.tinto.core.domain.model.TransactionSource

/**
 * Movement detail bottom sheet, opened by tapping a [StatementRow] on the
 * dashboard preview or the statement. Signed amount as hero, the movement's
 * facts as label/value rows, and the two actions of Sprint 2: Editar and
 * Eliminar (with confirmation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementDetailSheet(
    item: MovementUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
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
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MoneyText(amount = item.amount, type = item.type, style = type.moneyHero)
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.title,
                style = type.body.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(20.dp))
            DetailRow(label = stringResource(R.string.movement_detail_category)) {
                CategoryIcon(
                    iconKey = item.categoryIconKey,
                    colorHex = item.categoryColorHex,
                    size = 24.dp,
                )
                Spacer(Modifier.width(8.dp))
                DetailValue(item.categoryName)
            }
            DetailDivider()
            DetailRow(label = stringResource(R.string.movement_detail_method)) {
                DetailValue(
                    when {
                        item.isCash -> stringResource(R.string.method_cash)
                        item.cardLast4 != null -> stringResource(R.string.card_mask, item.cardLast4)
                        else -> stringResource(R.string.add_method_card)
                    },
                )
            }
            DetailDivider()
            DetailRow(label = stringResource(R.string.movement_detail_date)) {
                DetailValue(Dates.dayOfMonthName(item.date))
            }
            item.merchant?.let { merchant ->
                DetailDivider()
                DetailRow(label = stringResource(R.string.movement_detail_merchant)) {
                    DetailValue(merchant)
                }
            }
            DetailDivider()
            DetailRow(label = stringResource(R.string.movement_detail_source)) {
                DetailValue(sourceLabel(item.source))
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onEdit,
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
                    text = stringResource(R.string.action_edit),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                )
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { showDeleteConfirm = true }) {
                Text(
                    text = stringResource(R.string.action_delete),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                    color = tinto.expense,
                )
            }
        }
    }

    if (showDeleteConfirm) {
        TintoConfirmDialog(
            title = stringResource(R.string.movement_delete_confirm_title),
            message = stringResource(R.string.movement_delete_confirm_message),
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
private fun DetailRow(
    label: String,
    value: @Composable () -> Unit,
) {
    val type = LocalTintoTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = type.caption,
            color = LocalTintoColors.current.muted,
            modifier = Modifier.weight(1f),
        )
        value()
    }
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun DetailValue(text: String) {
    Text(
        text = text,
        style = LocalTintoTypography.current.body,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun sourceLabel(source: TransactionSource): String = stringResource(
    when (source) {
        TransactionSource.MANUAL -> R.string.source_manual
        TransactionSource.NOTIFICATION -> R.string.source_notification
        TransactionSource.EMAIL -> R.string.source_email
        TransactionSource.SMS -> R.string.source_sms
    },
)
