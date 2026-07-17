package dev.romerobrayan.tinto.feature.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.designsystem.component.TintoAmountField
import dev.romerobrayan.tinto.core.designsystem.component.TintoCategoryChip
import dev.romerobrayan.tinto.core.designsystem.component.TintoDatePickerDialog
import dev.romerobrayan.tinto.core.designsystem.component.TintoSelectorPill
import dev.romerobrayan.tinto.core.designsystem.component.tintoTextFieldColors
import dev.romerobrayan.tinto.core.designsystem.theme.ButtonShape
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape
import dev.romerobrayan.tinto.core.designsystem.theme.SheetShape
import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import dev.romerobrayan.tinto.feature.addtransaction.AddTransactionValidator
import kotlinx.datetime.LocalDate

/**
 * The editable confirm-before-commit sheet: every field pre-filled from the
 * parse, the category deliberately unset (required to confirm). On a likely
 * duplicate the prominence flips — Descartar becomes the filled action.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PendingReviewSheet(
    sheet: PendingReviewSheetUiState,
    categories: List<Category>,
    cards: List<Card>,
    onAmountChanged: (String) -> Unit,
    onTypeChanged: (TransactionType) -> Unit,
    onMethodChanged: (PaymentMethod) -> Unit,
    onLast4Changed: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onMerchantChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = SheetShape,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "${captureChannelLabel(sheet.channel)} · ${sheet.issuer}",
                style = type.caption,
                color = tinto.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            if (sheet.isDuplicate) {
                Spacer(Modifier.height(12.dp))
                DuplicateWarning()
            }

            Spacer(Modifier.height(16.dp))
            TintoAmountField(
                amountDigits = sheet.amountDigits,
                onAmountChanged = onAmountChanged,
                autoFocus = false,
            )
            if (AddTransactionValidator.Error.AMOUNT_REQUIRED in sheet.errors) {
                Spacer(Modifier.height(4.dp))
                SheetError(stringResource(R.string.add_error_amount), centered = true)
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TintoSelectorPill(
                    label = stringResource(R.string.add_type_expense),
                    selected = sheet.type == TransactionType.EXPENSE,
                    onClick = { onTypeChanged(TransactionType.EXPENSE) },
                )
                TintoSelectorPill(
                    label = stringResource(R.string.add_type_income),
                    selected = sheet.type == TransactionType.INCOME,
                    onClick = { onTypeChanged(TransactionType.INCOME) },
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.add_method_label),
                style = type.sectionTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TintoSelectorPill(
                    label = stringResource(R.string.add_method_cash),
                    selected = sheet.method == PaymentMethod.CASH,
                    onClick = { onMethodChanged(PaymentMethod.CASH) },
                )
                TintoSelectorPill(
                    label = stringResource(R.string.add_method_card),
                    selected = sheet.method == PaymentMethod.CARD,
                    onClick = { onMethodChanged(PaymentMethod.CARD) },
                )
            }
            if (sheet.method == PaymentMethod.CARD) {
                if (cards.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cards.forEach { card ->
                            TintoSelectorPill(
                                label = "${card.bank} ${stringResource(R.string.card_mask, card.last4)}",
                                selected = sheet.last4 == card.last4,
                                onClick = { onLast4Changed(card.last4) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = sheet.last4,
                    onValueChange = onLast4Changed,
                    label = { Text(stringResource(R.string.add_last4_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = tintoTextFieldColors(),
                    shape = ButtonShape,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (AddTransactionValidator.Error.LAST4_INVALID in sheet.errors) {
                    Spacer(Modifier.height(4.dp))
                    SheetError(stringResource(R.string.add_error_last4))
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.add_category_label),
                style = type.sectionTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    TintoCategoryChip(
                        name = category.name,
                        iconKey = category.iconKey,
                        colorHex = category.colorHex,
                        selected = sheet.categoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                    )
                }
            }
            if (AddTransactionValidator.Error.CATEGORY_REQUIRED in sheet.errors) {
                Spacer(Modifier.height(4.dp))
                SheetError(stringResource(R.string.add_error_category))
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.add_date_label),
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
                    text = Dates.dayOfMonthName(sheet.date),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(20.dp))
            TextField(
                value = sheet.merchant,
                onValueChange = onMerchantChanged,
                label = { Text(stringResource(R.string.add_merchant_label)) },
                singleLine = true,
                colors = tintoTextFieldColors(),
                shape = ButtonShape,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.pending_original_message),
                style = type.caption,
                color = tinto.muted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = sheet.rawBody,
                style = type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            // On a likely duplicate the safe action leads (pre-selected
            // Descartar per the brief); otherwise Confirmar is primary.
            Button(
                onClick = if (sheet.isDuplicate) onDiscard else onConfirm,
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
                    text = stringResource(
                        if (sheet.isDuplicate) R.string.action_discard else R.string.action_confirm,
                    ),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = if (sheet.isDuplicate) onConfirm else onDiscard) {
                    Text(
                        text = stringResource(
                            if (sheet.isDuplicate) R.string.action_confirm else R.string.action_discard,
                        ),
                        style = type.body.copy(fontWeight = FontWeight.Medium),
                        color = if (sheet.isDuplicate) tinto.gold else tinto.expense,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        TintoDatePickerDialog(
            initialDate = sheet.date,
            onConfirm = { date ->
                onDateChanged(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/** "Posible duplicado" pill — on-palette, expense-toned text. */
@Composable
internal fun DuplicateBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(R.string.pending_duplicate_badge),
            style = LocalTintoTypography.current.meta,
            color = LocalTintoColors.current.expense,
            maxLines = 1,
        )
    }
}

@Composable
private fun DuplicateWarning() {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ButtonShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = tinto.expense,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = stringResource(R.string.pending_duplicate_badge),
                style = type.body.copy(fontWeight = FontWeight.Medium),
                color = tinto.expense,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.pending_duplicate_hint),
                style = type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SheetError(message: String, centered: Boolean = false) {
    Text(
        text = message,
        style = LocalTintoTypography.current.caption,
        color = MaterialTheme.colorScheme.error,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
    )
}
