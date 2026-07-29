package dev.romerobrayan.tinto.feature.voiceentry

import androidx.annotation.StringRes
import dev.romerobrayan.tinto.core.domain.model.SpeechModelState

data class VoiceEntryUiState(
    val permission: MicPermission = MicPermission.UNKNOWN,
    val model: SpeechModelState = SpeechModelState.NotDownloaded,
    /** Rounded download size, for the "descarga única" copy. */
    val modelSizeMb: Int = 0,
    val phase: VoicePhase = VoicePhase.Idle,
    /** The transcript, editable — the user gets the last word on what was said. */
    val transcript: String = "",
    @StringRes val errorRes: Int? = null,
) {
    /** Holding the button only does something when all three preconditions hold. */
    val canRecord: Boolean
        get() = permission == MicPermission.GRANTED &&
            model is SpeechModelState.Ready &&
            phase !is VoicePhase.Transcribing

    val isBusy: Boolean
        get() = phase is VoicePhase.Transcribing || model is SpeechModelState.Downloading
}

/**
 * `RECORD_AUDIO` as state rather than as a thrown exception.
 *
 * [PERMANENTLY_DENIED] is separate from [DENIED] because the two need different
 * copy and a different button: one re-asks, the other can only send the user to
 * system settings. Collapsing them produces a dialog that does nothing.
 */
enum class MicPermission {
    /** Not checked yet — the screen has not resumed. */
    UNKNOWN,
    GRANTED,
    /** Declined, but asking again is still allowed. */
    DENIED,
    /** Declined such that the system will no longer show the prompt. */
    PERMANENTLY_DENIED,
}

sealed interface VoicePhase {

    data object Idle : VoicePhase

    data class Recording(
        val elapsedMillis: Long,
        val amplitude: Float,
    ) : VoicePhase

    data object Transcribing : VoicePhase

    /** A transcript is in hand; the field is editable and can be used. */
    data object Transcribed : VoicePhase
}
