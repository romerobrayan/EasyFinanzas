package dev.romerobrayan.tinto.feature.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.component.MoneyText
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape
import dev.romerobrayan.tinto.core.designsystem.theme.TileShape
import dev.romerobrayan.tinto.core.domain.model.Recurrence

@Composable
fun RemindersScreen(
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RemindersContent(state = state, modifier = modifier)
}

@Composable
private fun RemindersContent(
    state: RemindersUiState,
    modifier: Modifier = Modifier,
) {
    val type = LocalTintoTypography.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.reminders_title),
            style = type.screenTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (state.upcoming.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.reminders_upcoming),
                style = type.sectionTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            ReminderList(state.upcoming)
        }

        if (state.paid.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.reminders_paid),
                style = type.sectionTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            ReminderList(state.paid)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReminderList(reminders: List<ReminderUi>) {
    reminders.forEachIndexed { index, reminder ->
        ReminderRow(reminder)
        if (index != reminders.lastIndex) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ReminderRow(reminder: ReminderUi) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(TileShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = null,
                tint = if (reminder.isPaid) tinto.muted else tinto.gold,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = type.body,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.reminder_due, reminder.dueDateLabel) +
                    " · " + recurrenceLabel(reminder.recurrence),
                style = type.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            reminder.amount?.let { amount ->
                MoneyText(
                    amount = amount,
                    neutralColor = if (reminder.isPaid) {
                        tinto.muted
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                )
            }
            if (reminder.isPaid) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reminder_paid_badge),
                        style = type.meta,
                        color = tinto.income,
                    )
                }
            }
        }
    }
}

@Composable
private fun recurrenceLabel(recurrence: Recurrence): String = stringResource(
    when (recurrence) {
        Recurrence.NONE -> R.string.recurrence_none
        Recurrence.WEEKLY -> R.string.recurrence_weekly
        Recurrence.MONTHLY -> R.string.recurrence_monthly
        Recurrence.YEARLY -> R.string.recurrence_yearly
    },
)
