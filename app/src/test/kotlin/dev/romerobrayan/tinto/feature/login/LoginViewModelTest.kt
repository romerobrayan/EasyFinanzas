package dev.romerobrayan.tinto.feature.login

import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.AuthUser
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins the sign-in button state machine. The view model is activity-scoped and
 * outlives the login screen (TintoRoot swaps composables without a NavHost),
 * so a successful sign-in keeps the spinner on while the screen swaps out and
 * onScreenLeft must clear it — otherwise the login screen shown after a
 * sign-out reuses the retained state and spins forever.
 */
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authRepository = FakeAuthRepository()
        viewModel = LoginViewModel(authRepository, NoOpAnalytics())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful sign-in keeps the spinner while the screen swaps out`() = runTest(dispatcher) {
        viewModel.onSignInStarted()
        viewModel.onGoogleIdToken("token")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSigningIn)
        assertNull(viewModel.uiState.value.errorRes)
    }

    @Test
    fun `onScreenLeft clears the spinner so the next visit starts idle`() = runTest(dispatcher) {
        viewModel.onSignInStarted()
        viewModel.onGoogleIdToken("token")
        advanceUntilIdle()

        viewModel.onScreenLeft()

        assertFalse(viewModel.uiState.value.isSigningIn)
        assertNull(viewModel.uiState.value.errorRes)
    }

    @Test
    fun `onScreenLeft keeps an error visible across configuration changes`() {
        viewModel.onSignInFailed(R.string.login_error_generic)

        viewModel.onScreenLeft()

        assertFalse(viewModel.uiState.value.isSigningIn)
        assertEquals(R.string.login_error_generic, viewModel.uiState.value.errorRes)
    }

    @Test
    fun `failed token exchange stops the spinner and surfaces the error`() = runTest(dispatcher) {
        authRepository.signInError = IllegalStateException("credential rejected")
        viewModel.onSignInStarted()
        viewModel.onGoogleIdToken("token")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSigningIn)
        assertEquals(R.string.login_error_generic, viewModel.uiState.value.errorRes)
    }
}

private class FakeAuthRepository : AuthRepository {

    var signInError: Exception? = null

    override val session: StateFlow<UserSession> = MutableStateFlow(UserSession.SignedOut)

    override suspend fun signInWithGoogle(idToken: String): AuthUser {
        signInError?.let { throw it }
        return AuthUser(uid = "uid-1", displayName = "Test", email = "test@example.com")
    }

    override fun enterDemoMode() = Unit

    override fun signOut() = Unit
}

private class NoOpAnalytics : TintoAnalytics {
    override fun setUser(userId: String?) = Unit
    override fun logScreenView(screenName: String) = Unit
    override fun logLogin(method: String) = Unit
    override fun logDemoMode() = Unit
    override fun logSignOut() = Unit
    override fun logAddTransaction(type: String, method: String) = Unit
    override fun logEditTransaction(type: String, method: String) = Unit
    override fun logDeleteTransaction(type: String, method: String) = Unit
    override fun logAddCard() = Unit
    override fun logDeleteCard() = Unit
    override fun logAddReminder(recurrence: String) = Unit
    override fun logReminderPaid(recurrence: String) = Unit
    override fun logReminderNotificationShown(recurrence: String) = Unit
    override fun logCapturePermissionGranted(channel: String) = Unit
    override fun logCaptureDetected(channel: String, issuer: String) = Unit
    override fun logPendingConfirmed(count: Int) = Unit
    override fun logPendingDiscarded(count: Int) = Unit
    override fun logPendingDuplicateShown() = Unit
    override fun recordError(error: Throwable) = Unit
}
