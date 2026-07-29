package dev.romerobrayan.tinto.feature.voiceentry

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.component.tintoTextFieldColors
import dev.romerobrayan.tinto.core.designsystem.theme.ButtonShape
import dev.romerobrayan.tinto.core.designsystem.theme.CardShape
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.domain.model.SpeechModelState

/**
 * Dictate a movement: hold the button, speak, get an editable transcript.
 *
 * Transcription only — the text is handed back to the add-transaction form
 * verbatim. Turning "gasté veinte mil en almuerzo" into an amount and a category
 * is a later sprint.
 */
@Composable
fun VoiceEntryScreen(
    onClose: () -> Unit,
    onUseTranscript: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Platform-call exception, same as the SMS opt-in in ProfileScreen: the
    // runtime prompt needs the Activity, so the screen owns the launcher — but
    // the outcome goes straight into ViewModel state rather than branching here.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionResult(
            granted = granted,
            canAskAgain = granted || context.canAskForMicrophoneAgain(),
        )
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onPermissionChecked(context.hasMicrophonePermission())
        onPauseOrDispose { }
    }

    VoiceEntryContent(
        state = state,
        onClose = onClose,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onOpenSettings = { context.openAppSettings() },
        onDownloadModel = viewModel::onDownloadModel,
        onRecordStart = viewModel::onRecordStart,
        onRecordStop = viewModel::onRecordStop,
        onTranscriptChanged = viewModel::onTranscriptChanged,
        onUseTranscript = { onUseTranscript(state.transcript) },
        modifier = modifier,
    )
}

@Composable
private fun VoiceEntryContent(
    state: VoiceEntryUiState,
    onClose: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDownloadModel: () -> Unit,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
    onTranscriptChanged: (String) -> Unit,
    onUseTranscript: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.stt_title),
                style = type.screenTitle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = tinto.muted,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.stt_hint),
            style = type.body,
            color = tinto.muted,
        )

        Spacer(Modifier.height(20.dp))

        when {
            state.permission == MicPermission.PERMANENTLY_DENIED ->
                BlockingPanel(
                    title = stringResource(R.string.stt_permission_denied),
                    body = stringResource(R.string.stt_permission_settings),
                    actionLabel = stringResource(R.string.stt_permission_open_settings),
                    onAction = onOpenSettings,
                )

            state.permission != MicPermission.GRANTED ->
                BlockingPanel(
                    title = stringResource(R.string.stt_permission_title),
                    body = stringResource(R.string.stt_permission_body),
                    actionLabel = stringResource(R.string.stt_permission_grant),
                    onAction = onRequestPermission,
                )

            state.model !is SpeechModelState.Ready ->
                ModelPanel(state = state, onDownload = onDownloadModel)

            else -> RecordPanel(
                state = state,
                onRecordStart = onRecordStart,
                onRecordStop = onRecordStop,
            )
        }

        state.errorRes?.let { errorRes ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(errorRes),
                style = type.caption,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (state.transcript.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.transcript,
                onValueChange = onTranscriptChanged,
                label = { Text(stringResource(R.string.stt_result_label)) },
                modifier = Modifier.fillMaxWidth(),
                colors = tintoTextFieldColors(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onUseTranscript,
                shape = ButtonShape,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(stringResource(R.string.stt_use))
            }
        }
    }
}

@Composable
private fun BlockingPanel(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, CardShape)
            .padding(16.dp),
    ) {
        Text(title, style = type.sectionTitle, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(body, style = type.body, color = tinto.muted)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun ModelPanel(state: VoiceEntryUiState, onDownload: () -> Unit) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current

    when (val model = state.model) {
        is SpeechModelState.Downloading -> Column(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    R.string.stt_model_downloading,
                    (model.fraction * 100).toInt(),
                ),
                style = type.body,
                color = tinto.muted,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { model.fraction },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        SpeechModelState.Verifying -> Text(
            text = stringResource(R.string.stt_model_verifying),
            style = type.body,
            color = tinto.muted,
        )

        is SpeechModelState.Failed -> BlockingPanel(
            title = stringResource(R.string.stt_model_title),
            body = stringResource(R.string.stt_model_body, state.modelSizeMb),
            actionLabel = stringResource(R.string.stt_model_retry),
            onAction = onDownload,
        )

        else -> BlockingPanel(
            title = stringResource(R.string.stt_model_title),
            body = stringResource(R.string.stt_model_body, state.modelSizeMb),
            actionLabel = stringResource(R.string.stt_model_download),
            onAction = onDownload,
        )
    }
}

@Composable
private fun RecordPanel(
    state: VoiceEntryUiState,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current
    val phase = state.phase
    val recordDescription = stringResource(R.string.cd_stt_record)

    // The ring tracks the microphone's RMS — a live signal that the app is
    // actually hearing something, which a spinner cannot give.
    val amplitude = (phase as? VoicePhase.Recording)?.amplitude ?: 0f
    val ringScale by animateFloatAsState(
        targetValue = 1f + (amplitude * 0.35f),
        label = "sttRing",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (phase is VoicePhase.Recording) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                )
            }
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .semantics { contentDescription = recordDescription }
                    .pointerInput(state.canRecord) {
                        if (!state.canRecord) return@pointerInput
                        detectTapGestures(
                            onPress = {
                                onRecordStart()
                                tryAwaitRelease()
                                onRecordStop()
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (phase is VoicePhase.Transcribing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = when (phase) {
                is VoicePhase.Recording -> stringResource(R.string.stt_recording)
                VoicePhase.Transcribing -> stringResource(R.string.stt_transcribing)
                else -> stringResource(R.string.stt_hold_to_record)
            },
            style = type.body,
            color = tinto.muted,
            textAlign = TextAlign.Center,
        )

        if (phase is VoicePhase.Recording) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.stt_timer, (phase.elapsedMillis / 1000L).toInt()),
                style = type.moneyRow,
                color = tinto.gold,
            )
        }
    }
}

private fun Context.hasMicrophonePermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

/**
 * False once the system stops showing the prompt, which is the difference
 * between "ask again" and "send them to settings".
 */
private fun Context.canAskForMicrophoneAgain(): Boolean {
    val activity = findActivity() ?: return true
    return activity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
}

/** Compose hands out a ContextWrapper, so the Activity needs unwrapping. */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
