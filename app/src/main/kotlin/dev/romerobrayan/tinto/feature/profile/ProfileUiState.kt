package dev.romerobrayan.tinto.feature.profile

import android.net.Uri
import dev.romerobrayan.tinto.core.data.export.ImportSummary
import dev.romerobrayan.tinto.core.domain.model.Card

/** The card bottom-sheet form; non-null while the sheet is open. */
data class CardFormUiState(
    /** null = adding a new card; non-null = editing that card. */
    val editingCardId: String? = null,
    val bank: String = "",
    val last4: String = "",
    val label: String = "",
    /** Only populated after a submit attempt, so the form starts clean. */
    val errors: Set<CardFormValidator.Error> = emptySet(),
)

/** One-shot result of an export attempt, surfaced once as a toast (+ a share prompt on success). */
sealed interface ExportOutcome {
    data class Success(val uri: Uri) : ExportOutcome
    data object Failure : ExportOutcome
}

/** One-shot result of an import attempt, surfaced once as a toast. */
sealed interface ImportOutcome {
    data class Success(val summary: ImportSummary) : ImportOutcome
    data object Failure : ImportOutcome
}

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val cards: List<Card> = emptyList(),
    /** True while exploring with sample data (no cloud persistence). */
    val isDemo: Boolean = false,
    /** True for a no-account profile: the ledger lives only on this device. */
    val isLocal: Boolean = false,
    /** Whether the user opted in to SMS capture (Sprint 3). */
    val smsCaptureEnabled: Boolean = false,
    /** Whether the user opted in to Nu notification capture (Sprint 4). */
    val notificationCaptureEnabled: Boolean = false,
    /** Whether system-level notification access is currently granted. */
    val notificationAccessGranted: Boolean = false,
    val cardForm: CardFormUiState? = null,
)
