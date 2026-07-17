package dev.romerobrayan.tinto.feature.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate

@Composable
fun AddTransactionScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { onClose() }
    }

    AddTransactionContent(
        state = state,
        onClose = onClose,
        onAmountChanged = viewModel::onAmountChanged,
        onTypeChanged = viewModel::onTypeChanged,
        onMethodChanged = viewModel::onMethodChanged,
        onLast4Changed = viewModel::onLast4Changed,
        onCategorySelected = viewModel::onCategorySelected,
        onDateChanged = viewModel::onDateChanged,
        onMerchantChanged = viewModel::onMerchantChanged,
        onSubmit = viewModel::onSubmit,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionContent(
    state: AddTransactionUiState,
    onClose: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onTypeChanged: (TransactionType) -> Unit,
    onMethodChanged: (PaymentMethod) -> Unit,
    onLast4Changed: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onMerchantChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    if (state.isEditing) R.string.add_title_edit else R.string.add_title,
                ),
                style = type.screenTitle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    tint = tinto.muted,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TintoSelectorPill(
                label = stringResource(R.string.add_type_expense),
                selected = state.type == TransactionType.EXPENSE,
                onClick = { onTypeChanged(TransactionType.EXPENSE) },
            )
            TintoSelectorPill(
                label = stringResource(R.string.add_type_income),
                selected = state.type == TransactionType.INCOME,
                onClick = { onTypeChanged(TransactionType.INCOME) },
            )
        }

        Spacer(Modifier.height(24.dp))
        TintoAmountField(
            amountDigits = state.amountDigits,
            onAmountChanged = onAmountChanged,
        )
        if (AddTransactionValidator.Error.AMOUNT_REQUIRED in state.errors) {
            Spacer(Modifier.height(4.dp))
            ErrorText(stringResource(R.string.add_error_amount), centered = true)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.add_method_label),
            style = type.sectionTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TintoSelectorPill(
                label = stringResource(R.string.add_method_cash),
                selected = state.method == PaymentMethod.CASH,
                onClick = { onMethodChanged(PaymentMethod.CASH) },
            )
            TintoSelectorPill(
                label = stringResource(R.string.add_method_card),
                selected = state.method == PaymentMethod.CARD,
                onClick = { onMethodChanged(PaymentMethod.CARD) },
            )
        }

        if (state.method == PaymentMethod.CARD) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.cards.forEach { card ->
                    TintoSelectorPill(
                        label = "${card.bank} ${stringResource(R.string.card_mask, card.last4)}",
                        selected = state.last4 == card.last4,
                        onClick = { onLast4Changed(card.last4) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            TextField(
                value = state.last4,
                onValueChange = onLast4Changed,
                label = { Text(stringResource(R.string.add_last4_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = tintoTextFieldColors(),
                shape = ButtonShape,
                modifier = Modifier.fillMaxWidth(),
            )
            if (AddTransactionValidator.Error.LAST4_INVALID in state.errors) {
                Spacer(Modifier.height(4.dp))
                ErrorText(stringResource(R.string.add_error_last4))
            }
        }

        Spacer(Modifier.height(24.dp))
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
            state.categories.forEach { category ->
                TintoCategoryChip(
                    name = category.name,
                    iconKey = category.iconKey,
                    colorHex = category.colorHex,
                    selected = state.categoryId == category.id,
                    onClick = { onCategorySelected(category.id) },
                )
            }
        }
        if (AddTransactionValidator.Error.CATEGORY_REQUIRED in state.errors) {
            Spacer(Modifier.height(4.dp))
            ErrorText(stringResource(R.string.add_error_category))
        }

        Spacer(Modifier.height(24.dp))
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
                text = if (state.isDateToday) {
                    stringResource(R.string.date_today)
                } else {
                    Dates.dayOfMonthName(state.date)
                },
                style = type.body.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(24.dp))
        TextField(
            value = state.merchant,
            onValueChange = onMerchantChanged,
            label = { Text(stringResource(R.string.add_merchant_label)) },
            singleLine = true,
            colors = tintoTextFieldColors(),
            shape = ButtonShape,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))
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
                text = stringResource(
                    when {
                        state.isEditing -> R.string.add_save_changes
                        state.type == TransactionType.EXPENSE -> R.string.add_save_expense
                        else -> R.string.add_save_income
                    },
                ),
                style = type.body.copy(fontWeight = FontWeight.Medium),
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        TintoDatePickerDialog(
            initialDate = state.date,
            onConfirm = { date ->
                onDateChanged(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun ErrorText(message: String, centered: Boolean = false) {
    Text(
        text = message,
        style = LocalTintoTypography.current.caption,
        color = MaterialTheme.colorScheme.error,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
    )
}
