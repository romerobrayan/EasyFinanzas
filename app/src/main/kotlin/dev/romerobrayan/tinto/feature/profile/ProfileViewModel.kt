package dev.romerobrayan.tinto.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.MockData
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analytics: TintoAnalytics,
    cardRepository: CardRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.session,
        cardRepository.observeCards(),
    ) { session, cards ->
        when (session) {
            is UserSession.SignedIn -> ProfileUiState(
                userName = session.user.displayName ?: session.user.email.orEmpty(),
                userEmail = session.user.email.orEmpty(),
                cards = cards,
                isDemo = false,
            )

            // Demo persona; also what Loading/SignedOut briefly render on the
            // way out of this screen.
            else -> ProfileUiState(
                userName = MockData.USER_NAME,
                userEmail = MockData.USER_EMAIL,
                cards = cards,
                isDemo = true,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    /** Ends the session (or leaves the demo); TintoRoot falls back to the login gate. */
    fun onSignOut() {
        analytics.logSignOut()
        authRepository.signOut()
    }
}
