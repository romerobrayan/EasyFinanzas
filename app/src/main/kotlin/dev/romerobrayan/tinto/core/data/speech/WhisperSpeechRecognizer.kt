package dev.romerobrayan.tinto.core.data.speech

import android.os.Build
import dev.romerobrayan.tinto.core.common.TintoDispatchers
import dev.romerobrayan.tinto.core.domain.model.RecognitionFailure
import dev.romerobrayan.tinto.core.domain.model.RecognitionState
import dev.romerobrayan.tinto.core.domain.model.SpeechModel
import dev.romerobrayan.tinto.core.domain.model.SpeechModelState
import dev.romerobrayan.tinto.core.domain.repository.SpeechRecognizer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The on-device [SpeechRecognizer], wrapping whisper.cpp over JNI.
 *
 * Mirrors `AndroidTtsSynthesizer` in shape: a `@Singleton` that hides an
 * awkward, stateful native resource behind coroutines. See `docs/stt-whisper.md`.
 *
 * The concurrency rule that matters: **`whisper_context` is not thread-safe**,
 * so every native call is confined to [nativeDispatcher], a single-threaded
 * view of IO. That dispatcher *is* the lock — there is no separate mutex to
 * forget to take, and [release] runs on it too, so freeing cannot race an
 * inference already in flight.
 */
@Singleton
class WhisperSpeechRecognizer @Inject constructor(
    private val store: SpeechModelStore,
    private val recorder: AudioRecorder,
    private val dispatchers: TintoDispatchers,
) : SpeechRecognizer {

    override val model: SpeechModel = selectModel()

    override val modelState: StateFlow<SpeechModelState> = store.modelState

    /** Single-threaded: serializes native calls without an explicit lock. */
    private val nativeDispatcher = dispatchers.io.limitedParallelism(1, "whisper")

    /** Runs [release] on the same thread as inference, so the free queues behind it. */
    private val nativeScope = CoroutineScope(SupervisorJob() + nativeDispatcher)

    /** Only ever touched from [nativeDispatcher]. */
    private var handle: Long = 0L

    /** Set from the UI thread, read by the capture loop. */
    @Volatile
    private var stopRequested: Boolean = false

    override suspend fun refreshModelState() = store.refresh(model)

    override suspend fun prepareModel() {
        store.ensure(model)
    }

    override fun recognize(): Flow<RecognitionState> = channelFlow {
        val modelFile = store.resolve(model)
        if (modelFile == null || !WhisperNative.isAvailable) {
            send(RecognitionState.Error(RecognitionFailure.MODEL_UNAVAILABLE))
            return@channelFlow
        }

        stopRequested = false
        val audio = try {
            recorder.record(
                maxMillis = MAX_RECORDING_MILLIS,
                shouldStop = { stopRequested },
            ) { elapsedMillis, amplitude ->
                // Progress is droppable: a missed waveform frame is invisible,
                // and back-pressuring the mic read loop would drop audio instead.
                trySend(RecognitionState.Recording(elapsedMillis, amplitude))
            }
        } catch (capture: AudioCaptureException) {
            send(RecognitionState.Error(capture.reason))
            return@channelFlow
        }

        send(RecognitionState.Transcribing)

        val transcript = withContext(nativeDispatcher) { transcribe(modelFile, audio) }

        send(
            when {
                transcript == null ->
                    RecognitionState.Error(RecognitionFailure.TRANSCRIPTION_FAILED)
                transcript.isBlank() ->
                    RecognitionState.Error(RecognitionFailure.NO_SPEECH_DETECTED)
                else -> RecognitionState.Success(transcript)
            },
        )
        // Returning here closes the channel and completes the flow. The terminal
        // state has been sent, so there is nothing left to wait for.
    }.flowOn(dispatchers.io)

    override fun stopRecording() {
        stopRequested = true
    }

    override fun release() {
        nativeScope.launch {
            if (handle != 0L) {
                WhisperNative.freeContext(handle)
                handle = 0L
            }
        }
    }

    /** Must run on [nativeDispatcher]. Loads the model once and keeps it warm. */
    private fun transcribe(modelFile: File, audio: FloatArray): String? {
        if (handle == 0L) {
            handle = WhisperNative.initContext(modelFile.absolutePath)
            if (handle == 0L) return null
        }
        val raw = WhisperNative.transcribe(handle, audio, threadCount()) ?: return null
        return raw.cleanTranscript()
    }

    private companion object {

        /**
         * The recording cap. Whisper encodes a padded 30 s window regardless, so
         * a longer cap costs almost nothing in inference time — the limit exists
         * to bound memory and to stop a stuck button recording forever.
         */
        const val MAX_RECORDING_MILLIS = 15_000L

        /**
         * More threads than physical cores makes whisper slower, not faster, and
         * four is where the gains flatten on phone-class hardware.
         */
        fun threadCount(): Int =
            Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

        /**
         * 64-bit gets the more accurate model; 32-bit devices are old enough that
         * `base` would take longer than the user will wait. See
         * `docs/stt-whisper-model-choice.md`.
         */
        fun selectModel(): SpeechModel =
            if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) {
                SpeechModel.BASE_Q5_1
            } else {
                SpeechModel.TINY_Q5_1
            }

        /**
         * Whisper marks non-speech audio with bracketed tags like `[BLANK_AUDIO]`
         * or `(música)`. Surfacing those verbatim in an editable field would read
         * as a transcription bug; stripping them lets silence fall through to
         * `NO_SPEECH_DETECTED`.
         */
        val NON_SPEECH = Regex("""[\[(][^\])]*[\])]""")

        fun String.cleanTranscript(): String =
            replace(NON_SPEECH, " ").replace(Regex("""\s+"""), " ").trim()
    }
}
