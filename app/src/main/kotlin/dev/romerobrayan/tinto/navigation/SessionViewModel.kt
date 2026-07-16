package dev.romerobrayan.tinto.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.core.common.TintoAnalytics
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.core.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Session state for the root switch (splash / login / app) + screen analytics. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val analytics: TintoAnalytics,
) : ViewModel() {

    val session: StateFlow<UserSession> = authRepository.session

    fun onScreenView(screenName: String) {
        analytics.logScreenView(screenName)
    }
}
