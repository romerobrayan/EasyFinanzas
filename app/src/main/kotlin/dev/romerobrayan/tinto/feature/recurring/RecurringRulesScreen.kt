package dev.romerobrayan.tinto.feature.recurring

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.Dates
import dev.romerobrayan.tinto.core.designsystem.component.CategoryIcon
import dev.romerobrayan.tinto.core.designsystem.component.MoneyText
import dev.romerobrayan.tinto.core.designsystem.component.TintoConfirmDialog
import dev.romerobrayan.tinto.core.designsystem.component.frequencyLabelRes
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography

/**
 * "Movimientos automáticos": the lightweight manager for automation rules —
 * pause/resume (switch) or delete each. Deliberately minimal; creation happens
 * in the add-transaction form.
 */
@Composable
fun RecurringRulesScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringRulesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecurringRulesContent(
        state = state,
        onClose = onClose,
        onToggleActive = viewModel::onToggleActive,
        onDelete = viewModel::onDelete,
        modifier = modifier,
    )
}

@Composable
private fun RecurringRulesContent(
    state: RecurringRulesUiState,
    onClose: () -> Unit,
    onToggleActive: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    var deleteTarget by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.recurring_rules_title),
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
        if (state.rules.isEmpty()) {
            Text(
                text = stringResource(R.string.recurring_rules_empty),
                style = type.body,
                color = tinto.muted,
            )
        } else {
            state.rules.forEachIndexed { index, rule ->
                RuleRow(
                    rule = rule,
                    onToggleActive = { onToggleActive(rule.id) },
                    onDelete = { deleteTarget = rule.id },
                )
                if (index != state.rules.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    deleteTarget?.let { ruleId ->
        TintoConfirmDialog(
            title = stringResource(R.string.recurring_delete_confirm_title),
            message = stringResource(R.string.recurring_delete_confirm_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                deleteTarget = null
                onDelete(ruleId)
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun RuleRow(
    rule: RecurringRuleRowUi,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(iconKey = rule.categoryIconKey, colorHex = rule.categoryColorHex)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = rule.title,
                style = type.body,
                color = if (rule.isActive) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    tinto.muted
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.recurring_row_subtitle,
                    stringResource(frequencyLabelRes(rule.frequency)),
                    Dates.dayOfMonthName(rule.nextOccurrence),
                ),
                style = type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            MoneyText(amount = rule.amount, type = rule.type)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = rule.isActive,
            onCheckedChange = { onToggleActive() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = tinto.muted,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.action_delete),
                tint = tinto.expense,
            )
        }
    }
}
