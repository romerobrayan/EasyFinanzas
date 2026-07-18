package dev.romerobrayan.tinto.feature.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.component.MoneyText
import dev.romerobrayan.tinto.core.designsystem.component.TintoConfirmDialog
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape
import dev.romerobrayan.tinto.core.designsystem.theme.TileShape

@Composable
fun RemindersScreen(
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showNotificationExplainer by rememberSaveable { mutableStateOf(false) }
    var notificationAskDone by rememberSaveable { mutableStateOf(false) }

    // Runtime-permission platform call — the screen owns the launcher (same
    // exception as the login credential picker). Declining degrades
    // gracefully: reminders keep working, they just don't alert.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    // In-context ask: the first time a reminder is saved, not at app launch.
    LaunchedEffect(viewModel) {
        viewModel.saved.collect {
            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission && !notificationAskDone) {
                showNotificationExplainer = true
            }
        }
    }

    RemindersContent(
        state = state,
        onAddClick = viewModel::onAddClick,
        onReminderClick = viewModel::onReminderClick,
        modifier = modifier,
    )

    if (showNotificationExplainer) {
        TintoConfirmDialog(
            title = stringResource(R.string.reminder_permission_title),
            message = stringResource(R.string.reminder_permission_message),
            confirmLabel = stringResource(R.string.reminder_permission_allow),
            onConfirm = {
                showNotificationExplainer = false
                notificationAskDone = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = {
                showNotificationExplainer = false
                notificationAskDone = true
            },
            destructive = false,
        )
    }

    state.form?.let { form ->
        ReminderFormSheet(
            form = form,
            onTitleChanged = viewModel::onTitleChanged,
            onAmountChanged = viewModel::onAmountChanged,
            onDueDateChanged = viewModel::onDueDateChanged,
            onDueTimeChanged = viewModel::onDueTimeChanged,
            onRecurrenceChanged = viewModel::onRecurrenceChanged,
            onSubmit = viewModel::onSubmit,
            onMarkPaid = viewModel::onMarkPaid,
            onDelete = viewModel::onDelete,
            onDismiss = viewModel::onFormDismiss,
        )
    }
}

@Composable
private fun RemindersContent(
    state: RemindersUiState,
    onAddClick: () -> Unit,
    onReminderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.reminders_title),
                style = type.screenTitle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAddClick) {
                Text(
                    text = stringResource(R.string.reminders_add),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                    color = tinto.gold,
                )
            }
        }

        if (state.upcoming.isEmpty() && state.paid.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.reminders_empty),
                    style = type.caption,
                    color = tinto.muted,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (state.upcoming.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.reminders_upcoming),
                style = type.sectionTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            ReminderList(state.upcoming, onReminderClick)
        }

        if (state.paid.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.reminders_paid),
                style = type.sectionTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            ReminderList(state.paid, onReminderClick)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReminderList(
    reminders: List<ReminderUi>,
    onReminderClick: (String) -> Unit,
) {
    reminders.forEachIndexed { index, reminder ->
        ReminderRow(reminder, onClick = { onReminderClick(reminder.id) })
        if (index != reminders.lastIndex) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: ReminderUi,
    onClick: () -> Unit,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
