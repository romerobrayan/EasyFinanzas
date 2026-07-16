package dev.romerobrayan.tinto.feature.login

import androidx.annotation.StringRes

data class LoginUiState(
    val isSigningIn: Boolean = false,
    @param:StringRes val errorRes: Int? = null,
)
