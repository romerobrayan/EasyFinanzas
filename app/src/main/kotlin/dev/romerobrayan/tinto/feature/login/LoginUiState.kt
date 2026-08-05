package dev.romerobrayan.tinto.feature.login

import androidx.annotation.StringRes

data class LoginUiState(
    val isSigningIn: Boolean = false,
    @param:StringRes val errorRes: Int? = null,
    /**
     * The nickname being typed for a device-local profile; non-null exactly
     * while that prompt is open (empty string = prompt open, nothing typed).
     */
    val localNameDraft: String? = null,
) {
    val isNamingLocalProfile: Boolean get() = localNameDraft != null

    val canConfirmLocalName: Boolean get() = !localNameDraft.isNullOrBlank()
}
