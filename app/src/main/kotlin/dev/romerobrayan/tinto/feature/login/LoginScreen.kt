package dev.romerobrayan.tinto.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.designsystem.component.TintoWordmark
import dev.romerobrayan.tinto.core.designsystem.theme.ButtonShape
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoColors
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import kotlinx.coroutines.launch

/**
 * Entry gate: Google sign-in (cloud-synced ledger) or the sample-data demo.
 * The Credential Manager call happens here — it needs this Activity context.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Read at composition time (Compose lint: resource queries through
    // LocalContext.current don't refresh on configuration changes).
    val webClientId = stringResource(R.string.default_web_client_id)

    LoginContent(
        state = state,
        onGoogleClick = {
            if (webClientId.startsWith("REPLACE")) {
                // Placeholder google-services.json still in the build.
                viewModel.onSignInFailed(R.string.login_error_not_configured)
            } else {
                viewModel.onSignInStarted()
                scope.launch {
                    try {
                        viewModel.onGoogleIdToken(fetchGoogleIdToken(context, webClientId))
                    } catch (cancelled: GetCredentialCancellationException) {
                        viewModel.onSignInCancelled()
                    } catch (noAccount: NoCredentialException) {
                        viewModel.onSignInFailed(R.string.login_error_no_google_account)
                    } catch (credentialError: GetCredentialException) {
                        viewModel.onSignInFailed(R.string.login_error_generic)
                    } catch (unexpectedCredential: IllegalStateException) {
                        viewModel.onSignInFailed(R.string.login_error_generic)
                    }
                }
            }
        },
        onDemoClick = viewModel::onDemoClick,
        modifier = modifier,
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onGoogleClick: () -> Unit,
    onDemoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = LocalTintoTypography.current
    val tinto = LocalTintoColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1.2f))
        TintoWordmark()
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.login_tagline),
            style = type.body,
            color = tinto.muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))

        state.errorRes?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                style = type.caption,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
        }

        Button(
            onClick = onGoogleClick,
            enabled = !state.isSigningIn,
            shape = ButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = tinto.muted,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (state.isSigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = tinto.gold,
                )
            } else {
                Text(
                    text = stringResource(R.string.login_google),
                    style = type.body.copy(fontWeight = FontWeight.Medium),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onDemoClick, enabled = !state.isSigningIn) {
            Text(
                text = stringResource(R.string.login_demo),
                style = type.body,
                color = tinto.gold,
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.login_demo_hint),
            style = type.caption,
            color = tinto.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
    }
}
