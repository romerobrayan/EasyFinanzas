package dev.romerobrayan.tinto.core.domain.model

/** The signed-in account as the domain sees it — no Firebase types above the data layer. */
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
)
