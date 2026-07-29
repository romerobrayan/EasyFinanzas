package dev.romerobrayan.tinto.core.domain.model

/**
 * How one spoken utterance ended. Failures are returned rather than thrown so the
 * caller maps them to a message in an exhaustive `when` — adding a failure mode
 * later is a compile error, not a silent fallthrough to a generic string.
 */
sealed interface SpeechOutcome {

    /** The engine finished reading the whole text. */
    data object Completed : SpeechOutcome

    /** Stopped on purpose (the user tapped stop, or the scope was cancelled). */
    data object Cancelled : SpeechOutcome

    data class Failed(val reason: SpeechFailure) : SpeechOutcome
}

/** The speech failures worth telling the user apart; see `docs/tts.md`. */
enum class SpeechFailure {
    /** No engine installed, or initialization reported an error. */
    ENGINE_UNAVAILABLE,

    /** The engine has no Spanish voice at all. */
    LANGUAGE_NOT_SUPPORTED,

    /** Spanish is known to the engine but its voice data is not downloaded. */
    MISSING_VOICE_DATA,

    /** The engine accepted the utterance and then errored while speaking. */
    SYNTHESIS_FAILED,
}
