package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.RecognitionState
import dev.romerobrayan.tinto.core.domain.model.SpeechModel
import dev.romerobrayan.tinto.core.domain.model.SpeechModelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain contract for turning speech into text, so features never touch
 * `android.media` or JNI. The mirror image of [SpeechSynthesizer] — see
 * `docs/stt-whisper.md`.
 *
 * Two lifecycles live here and they are deliberately separate:
 *
 * - **The model** ([modelState], [prepareModel]) is a ~57 MB file fetched once
 *   and kept. It outlives any single recording.
 * - **A recognition** ([recognize]) is one press-and-hold, seconds long.
 *
 * Folding the download into the recognition flow would mean a first-run
 * recording that silently blocks for a minute behind a "listening" indicator.
 * Keeping them apart lets the UI show download progress as its own thing.
 */
interface SpeechRecognizer {

    /** Which model this recognizer is configured to use. */
    val model: SpeechModel

    /** Availability of [model] on disk. Cold until [prepareModel] runs. */
    val modelState: StateFlow<SpeechModelState>

    /**
     * Reports whether [model] is already on disk, without downloading it.
     * Call on screen entry so the UI can distinguish "not yet downloaded" from
     * "downloading" before the user does anything.
     */
    suspend fun refreshModelState()

    /**
     * Downloads and verifies [model] if it is not already usable, publishing
     * progress through [modelState]. Suspends until it settles. Safe to call
     * repeatedly and safe to call when already `Ready` — it returns immediately.
     *
     * Cancelling keeps the partial download for a later resume.
     */
    suspend fun prepareModel()

    /**
     * One recording session.
     *
     * Collecting starts capture; the flow emits [RecognitionState.Recording]
     * while audio comes in, then [RecognitionState.Transcribing], then exactly
     * one terminal [RecognitionState.Success] or [RecognitionState.Error].
     *
     * Capture ends when [stopRecording] is called or the internal cap is
     * reached, whichever comes first.
     *
     * The caller must hold `RECORD_AUDIO`; without it this fails with
     * [dev.romerobrayan.tinto.core.domain.model.RecognitionFailure.MICROPHONE_UNAVAILABLE]
     * rather than throwing.
     */
    fun recognize(): Flow<RecognitionState>

    /**
     * Ends capture and proceeds to transcription, keeping the audio recorded so
     * far. This is the button-release path.
     *
     * Deliberately **not** the same as cancelling the collection. Cancelling
     * throws the utterance away; this one commits it. Conflating them would mean
     * either "release to transcribe" or "abandon" had to be expressed as the
     * other, and one of the two would end up surprising. No-op when idle.
     */
    fun stopRecording()

    /**
     * Releases the native context. Idempotent — a later [recognize] reloads the
     * model transparently, so calling this early costs one reload, never a crash.
     */
    fun release()
}
