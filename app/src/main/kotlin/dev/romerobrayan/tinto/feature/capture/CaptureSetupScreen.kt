package dev.romerobrayan.tinto.feature.capture

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.theme.ButtonShape
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.PillShape

/**
 * The capture opt-in explainer: why we read SMS, what stays on the device,
 * and the personal-build note — nothing is read before the user enables it
 * here. Requesting the runtime permission needs the Activity, so the launcher
 * lives in the screen (the same deliberate presentation-layer exception as
 * the Credential Manager login).
 */
@Composable
fun CaptureSetupScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CaptureSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.all { it }) {
            permissionDenied = false
            viewModel.onPermissionsGranted()
        } else {
            permissionDenied = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.capture_setup_title),
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
        Text(
            text = stringResource(R.string.capture_setup_intro),
            style = type.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        ExplainerRow(Icons.Outlined.Sms, stringResource(R.string.capture_setup_bullet_detect))
        Spacer(Modifier.height(14.dp))
        ExplainerRow(Icons.Outlined.Lock, stringResource(R.string.capture_setup_bullet_local))
        Spacer(Modifier.height(14.dp))
        ExplainerRow(Icons.Outlined.Info, stringResource(R.string.capture_setup_bullet_personal))

        Spacer(Modifier.height(28.dp))
        if (state.smsEnabled) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.capture_status_on),
                        style = type.caption.copy(fontWeight = FontWeight.Medium),
                        color = tinto.gold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = viewModel::onDisable) {
                    Text(
                        text = stringResource(R.string.capture_disable),
                        style = type.body,
                        color = tinto.muted,
                    )
                }
            }
        } else {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                    )
                },
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
                    text = stringResource(R.string.capture_enable),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                )
            }
            if (permissionDenied) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.capture_permission_denied),
                    style = type.caption,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.capture_card_tip),
            style = type.caption,
            color = tinto.muted,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ExplainerRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalTintoColors.current.gold,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = LocalTintoTypography.current.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
