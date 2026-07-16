package dev.romerobrayan.tinto.feature.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.theme.ButtonShape
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape
import dev.romerobrayan.tinto.core.designsystem.theme.TileShape

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(state = state, onSignOut = viewModel::onSignOut, modifier = modifier)
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    onSignOut: () -> Unit,
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
        Text(
            text = stringResource(R.string.profile_title),
            style = type.screenTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialsOf(state.userName),
                    style = type.sectionTitle,
                    color = tinto.gold,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.userName,
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = state.userEmail,
                    style = type.caption,
                    color = tinto.muted,
                )
            }
            if (state.isDemo) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_demo_badge),
                        style = type.meta,
                        color = tinto.gold,
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.profile_cards_section),
            style = type.sectionTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        state.cards.forEachIndexed { index, card ->
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
                        imageVector = Icons.Outlined.CreditCard,
                        contentDescription = null,
                        tint = tinto.gold,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = card.bank,
                        style = type.body,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    card.label?.let { label ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = type.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.card_mask, card.last4),
                    style = type.moneyRow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (index != state.cards.lastIndex) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.profile_data_section),
            style = type.sectionTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ButtonShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    // TODO(sprint-2): real JSON export via the Storage Access Framework.
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.FileDownload,
                contentDescription = null,
                tint = tinto.gold,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.profile_export),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.profile_export_hint),
                    style = type.caption,
                    color = tinto.muted,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.profile_permissions_section),
            style = type.sectionTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        // Placeholder for the Sprint-3 capture onboarding (permissions live there).
        PermissionRow(Icons.Outlined.NotificationsNone, stringResource(R.string.profile_perm_notifications))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
        PermissionRow(Icons.Outlined.Sms, stringResource(R.string.profile_perm_sms))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
        PermissionRow(Icons.Outlined.Email, stringResource(R.string.profile_perm_gmail))

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.profile_session_section),
            style = type.sectionTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ButtonShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onSignOut)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = tinto.expense,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(
                        if (state.isDemo) R.string.profile_exit_demo else R.string.profile_sign_out,
                    ),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (!state.isDemo) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.profile_sign_out_hint),
                        style = type.caption,
                        color = tinto.muted,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, label: String) {
    val type = LocalTintoTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalTintoColors.current.muted,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = type.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_perm_soon),
                style = type.meta,
                color = LocalTintoColors.current.gold,
            )
        }
    }
}

private fun initialsOf(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
