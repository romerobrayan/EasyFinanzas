package dev.romerobrayan.tinto.feature.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analytics: TintoAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** The account picker is opening; disable the buttons and clear old errors. */
    fun onSignInStarted() {
        _uiState.value = LoginUiState(isSigningIn = true)
    }

    /** Google handed us an ID token; exchange it for a Firebase session. */
    fun onGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            try {
                authRepository.signInWithGoogle(idToken)
                analytics.logLogin("google")
                // The session flow flips to SignedIn and TintoRoot swaps
                // screens; the spinner stays on until this screen leaves,
                // and onScreenLeft clears it for the next visit.
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                analytics.recordError(error)
                _uiState.value = LoginUiState(errorRes = R.string.login_error_generic)
            }
        }
    }

    fun onSignInCancelled() {
        _uiState.value = LoginUiState()
    }

    /**
     * The login screen left the composition (sign-in landed, demo entered, or
     * the activity is being torn down). This view model outlives the screen —
     * TintoRoot swaps composables under one activity, so the same instance is
     * reused when the user signs out and comes back — and a retained
     * `isSigningIn = true` would leave the Google button spinning forever.
     * The error is kept so it survives configuration changes.
     */
    fun onScreenLeft() {
        // Still signed out means the screen is only being recomposed (a
        // configuration change) — keep what the user was typing. Anything else
        // means the gate was passed, so the nickname prompt must not be
        // waiting when they come back after a sign-out.
        val stillAtTheGate = authRepository.session.value == UserSession.SignedOut
        _uiState.update { state ->
            state.copy(
                isSigningIn = false,
                localNameDraft = state.localNameDraft.takeIf { stillAtTheGate },
            )
        }
    }

    fun onSignInFailed(@StringRes messageRes: Int) {
        _uiState.value = LoginUiState(errorRes = messageRes)
    }

    fun onDemoClick() {
        analytics.logDemoMode()
        authRepository.enterDemoMode()
    }

    /** Opens the nickname prompt, prefilled with the last local profile if any. */
    fun onContinueWithoutAccountClick() {
        _uiState.value = LoginUiState(
            localNameDraft = authRepository.localProfileName.value.orEmpty(),
        )
    }

    fun onLocalNameChanged(value: String) {
        // A nickname, not a legal name: cap it where the profile header still
        // reads well, and keep newlines out of a single-line field.
        _uiState.update { it.copy(localNameDraft = value.replace("\n", "").take(MAX_LOCAL_NAME_LENGTH)) }
    }

    fun onLocalNameDismissed() {
        _uiState.update { it.copy(localNameDraft = null) }
    }

    /**
     * Starts the device-local session. Nothing is logged here on purpose: the
     * mode promises that no trace of it reaches Google, and the repository
     * turns collection off before the session flips.
     */
    fun onLocalNameConfirmed() {
        val name = _uiState.value.localNameDraft?.trim().orEmpty()
        if (name.isEmpty()) return
        authRepository.continueLocally(name)
    }

    private companion object {
        const val MAX_LOCAL_NAME_LENGTH = 24
    }
}
