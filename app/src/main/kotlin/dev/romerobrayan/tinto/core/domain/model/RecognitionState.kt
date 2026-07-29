package dev.romerobrayan.tinto.core.domain.model

/**
 * The lifecycle of a single recognition attempt, from the moment the user starts
 * recording to a transcript or a failure.
 *
 * There is deliberately **no partial/streaming state**. Whisper is batch by
 * nature: the encoder consumes a padded 30-second window in one pass, so there
 * is no intermediate transcript to report. Faking one would mean inventing text
 * we do not have. See `docs/stt-whisper.md`.
 */
sealed interface RecognitionState {

    /** Nothing happening. The terminal state of a cancelled attempt. */
    data object Idle : RecognitionState

    /**
     * Capturing audio. [elapsedMillis] drives the timer, [amplitude] (0f..1f) the
     * waveform. Emitted repeatedly while the user holds the button.
     */
    data class Recording(
        val elapsedMillis: Long,
        val amplitude: Float,
    ) : RecognitionState

    /**
     * Audio captured, inference running. On a mid-range device this is seconds,
     * not milliseconds — which is exactly why it is a state the UI can show
     * rather than a spinner bolted onto [Recording].
     */
    data object Transcribing : RecognitionState

    /** [text] is whatever Whisper heard, trimmed. Never empty — that is [Error]. */
    data class Success(val text: String) : RecognitionState

    data class Error(val reason: RecognitionFailure) : RecognitionState
}

/**
 * Why a recognition attempt failed.
 *
 * An enum rather than free-form text so the ViewModel maps each case to its own
 * `@StringRes` in an exhaustive `when` — adding a case later is a compile error
 * instead of a silent fallthrough to a generic message. Same reasoning as
 * [SpeechFailure] on the text-to-speech side.
 */
enum class RecognitionFailure {

    /** The model is not downloaded, or the download never completed. */
    MODEL_UNAVAILABLE,

    /** `AudioRecord` could not be constructed or started — mic busy or denied. */
    MICROPHONE_UNAVAILABLE,

    /** Capture started but produced no usable audio. */
    RECORDING_FAILED,

    /** The native call failed or returned nothing. */
    TRANSCRIPTION_FAILED,

    /** Audio was captured but Whisper found no speech in it. */
    NO_SPEECH_DETECTED,
}
