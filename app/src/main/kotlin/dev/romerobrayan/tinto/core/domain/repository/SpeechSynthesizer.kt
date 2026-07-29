package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.SpeechOutcome

/**
 * Domain contract for speaking text out loud, so features never touch
 * `android.speech.tts`. A cloud provider replaces the on-device engine with a
 * single `@Binds` change — see `docs/tts.md`.
 *
 * [speak] suspends until the utterance finishes, which makes the caller's job the
 * utterance's lifetime: cancelling the coroutine stops playback, and there is no
 * separate "still speaking?" flag to drift out of sync with the engine.
 */
interface SpeechSynthesizer {

    /**
     * Speaks [text], suspending until it finishes, is stopped, or fails.
     *
     * [onStarted] fires when audio actually begins — engine init and voice lookup
     * happen before the first sound, and on a cold start that gap is noticeable.
     * Cancelling the calling coroutine stops playback and returns via cancellation.
     */
    suspend fun speak(text: String, onStarted: () -> Unit = {}): SpeechOutcome

    /** Stops the current utterance, if any. Safe to call when idle. */
    fun stop()

    /**
     * Releases the underlying engine. Idempotent — a later [speak] transparently
     * re-initializes, so calling this early degrades to an extra init, never a
     * crash. Call it when the owning scope dies.
     */
    fun shutdown()
}
