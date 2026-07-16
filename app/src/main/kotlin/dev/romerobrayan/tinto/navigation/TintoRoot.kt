package dev.romerobrayan.tinto.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.romerobrayan.tinto.core.designsystem.component.TintoWordmark
import dev.romerobrayan.tinto.core.domain.model.UserSession
import dev.romerobrayan.tinto.feature.login.LoginScreen

/**
 * Root of the UI: routes on the auth session. Loading shows the wordmark for
 * the instant Firebase restores the persisted user; SignedOut gates on login;
 * Demo and SignedIn get the full app shell.
 */
@Composable
fun TintoRoot(viewModel: SessionViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (session) {
            UserSession.Loading -> SplashContent()
            UserSession.SignedOut -> LoginScreen()
            UserSession.Demo, is UserSession.SignedIn -> TintoApp(onScreenView = viewModel::onScreenView)
        }
    }
}

@Composable
private fun SplashContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TintoWordmark()
    }
}
