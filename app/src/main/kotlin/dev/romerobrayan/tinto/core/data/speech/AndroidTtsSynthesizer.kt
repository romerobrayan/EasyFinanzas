package dev.romerobrayan.tinto.core.data.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romerobrayan.tinto.core.common.TintoDispatchers
import dev.romerobrayan.tinto.core.domain.model.SpeechFailure
import dev.romerobrayan.tinto.core.domain.model.SpeechOutcome
import dev.romerobrayan.tinto.core.domain.repository.SpeechSynthesizer
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The on-device [SpeechSynthesizer], wrapping `android.speech.tts.TextToSpeech`.
 *
 * Two things this type exists to hide: initialization is asynchronous and can
 * fail, and progress arrives on a listener rather than a return value. Both are
 * bridged into coroutines so callers just suspend. See `docs/tts.md`.
 *
 * The engine is built on first use and kept warm; [shutdown] releases it and a
 * later [speak] re-initializes transparently.
 */
@Singleton
class AndroidTtsSynthesizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: TintoDispatchers,
) : SpeechSynthesizer {

    /** Serializes initialization so concurrent speaks await one engine, not two. */
    private val initLock = Mutex()

    @Volatile
    private var engine: TextToSpeech? = null

    private val currentUtterance = AtomicReference<Utterance?>(null)
    private val utteranceCounter = AtomicLong()

    override suspend fun speak(text: String, onStarted: () -> Unit): SpeechOutcome =
        withContext(dispatchers.default) {
            when (val engineState = obtainEngine()) {
                is EngineState.Unavailable -> SpeechOutcome.Failed(engineState.reason)
                is EngineState.Ready -> awaitUtterance(engineState.engine, text, onStarted)
            }
        }

    override fun stop() {
        engine?.stop()
        // stop() does not reliably deliver onStop, so settle the continuation here
        // too; finish() is idempotent, whichever path gets there first.
        finish(utteranceId = null, outcome = SpeechOutcome.Cancelled)
    }

    override fun shutdown() {
        finish(utteranceId = null, outcome = SpeechOutcome.Cancelled)
        val released = engine
        engine = null
        released?.stop()
        released?.shutdown()
    }

    private suspend fun obtainEngine(): EngineState = initLock.withLock {
        engine?.let { return@withLock EngineState.Ready(it) }

        val initStatus = CompletableDeferred<Int>()
        val tts = TextToSpeech(context) { status -> initStatus.complete(status) }

        val status = try {
            initStatus.await()
        } catch (cancellation: CancellationException) {
            // Cancelled mid-init: release the half-built engine rather than leak it.
            tts.shutdown()
            throw cancellation
        }

        if (status != TextToSpeech.SUCCESS) {
            tts.shutdown()
            // Deliberately not cached — the user may install an engine and retry.
            return@withLock EngineState.Unavailable(SpeechFailure.ENGINE_UNAVAILABLE)
        }

        selectSpanish(tts)?.let { failure ->
            tts.shutdown()
            return@withLock EngineState.Unavailable(failure)
        }

        tts.setOnUtteranceProgressListener(progressListener)
        engine = tts
        EngineState.Ready(tts)
    }

    /** Returns null once a Spanish voice is selected, or why none could be. */
    private fun selectSpanish(tts: TextToSpeech): SpeechFailure? {
        var sawMissingData = false
        for (locale in SPANISH_LOCALES) {
            when (tts.setLanguage(locale)) {
                TextToSpeech.LANG_MISSING_DATA -> sawMissingData = true
                TextToSpeech.LANG_NOT_SUPPORTED -> Unit
                else -> return null
            }
        }
        // Missing data is the more actionable of the two — the user can download it.
        return if (sawMissingData) {
            SpeechFailure.MISSING_VOICE_DATA
        } else {
            SpeechFailure.LANGUAGE_NOT_SUPPORTED
        }
    }

    private suspend fun awaitUtterance(
        tts: TextToSpeech,
        text: String,
        onStarted: () -> Unit,
    ): SpeechOutcome = suspendCancellableCoroutine { continuation ->
        val utteranceId = "tinto-${utteranceCounter.incrementAndGet()}"
        currentUtterance.set(Utterance(utteranceId, onStarted, continuation))

        continuation.invokeOnCancellation {
            currentUtterance.set(null)
            tts.stop()
        }

        val queued = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (queued != TextToSpeech.SUCCESS) {
            finish(utteranceId, SpeechOutcome.Failed(SpeechFailure.SYNTHESIS_FAILED))
        }
    }

    /**
     * Settles the in-flight utterance exactly once. A null [utteranceId] settles
     * whatever is current (the stop/shutdown path); a non-null one only settles
     * its own, so a late callback from a flushed utterance is ignored.
     */
    private fun finish(utteranceId: String?, outcome: SpeechOutcome) {
        val utterance = currentUtterance.get() ?: return
        if (utteranceId != null && utterance.id != utteranceId) return
        if (!currentUtterance.compareAndSet(utterance, null)) return
        if (utterance.continuation.isActive) utterance.continuation.resume(outcome)
    }

    private val progressListener = object : UtteranceProgressListener() {

        override fun onStart(utteranceId: String?) {
            val utterance = currentUtterance.get() ?: return
            if (utterance.id == utteranceId) utterance.onStarted()
        }

        override fun onDone(utteranceId: String?) {
            finish(utteranceId, SpeechOutcome.Completed)
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            finish(utteranceId, SpeechOutcome.Cancelled)
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onError(utteranceId: String?) {
            finish(utteranceId, SpeechOutcome.Failed(SpeechFailure.SYNTHESIS_FAILED))
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            finish(utteranceId, SpeechOutcome.Failed(SpeechFailure.SYNTHESIS_FAILED))
        }
    }

    private class Utterance(
        val id: String,
        val onStarted: () -> Unit,
        val continuation: CancellableContinuation<SpeechOutcome>,
    )

    private sealed interface EngineState {
        class Ready(val engine: TextToSpeech) : EngineState
        class Unavailable(val reason: SpeechFailure) : EngineState
    }

    private companion object {
        /** Colombian Spanish first, then plain Spanish before giving up. */
        val SPANISH_LOCALES = listOf(
            Locale.forLanguageTag("es-CO"),
            Locale.forLanguageTag("es"),
        )
    }
}
