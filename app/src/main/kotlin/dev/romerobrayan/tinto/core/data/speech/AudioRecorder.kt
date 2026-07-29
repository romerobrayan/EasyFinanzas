package dev.romerobrayan.tinto.core.data.speech

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import dev.romerobrayan.tinto.core.domain.model.RecognitionFailure
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Microphone capture in exactly the shape Whisper wants: 16 kHz, mono,
 * normalized float samples.
 *
 * Whisper's front end resamples anything else to 16 kHz anyway, so recording at
 * a higher rate would cost battery and memory to throw the extra data away.
 *
 * Audio is accumulated **in memory and never written to disk**. A 15-second cap
 * at 16 kHz is 240 000 samples — under a megabyte as floats. Nothing to clean
 * up, nothing to leak, no file lifecycle to get wrong with someone's voice in it.
 */
class AudioRecorder @Inject constructor() {

    /**
     * Records until [shouldStop] returns true or [maxMillis] elapses, then
     * returns everything captured.
     *
     * [shouldStop] is polled once per buffer — roughly every few tens of
     * milliseconds, which is well inside human reaction time for a button
     * release. Cancelling the coroutine is the *other* ending: it stops the
     * microphone and discards the audio.
     *
     * [onProgress] fires each buffer with elapsed time and a 0f..1f RMS
     * amplitude for the waveform.
     *
     * @throws AudioCaptureException when the mic cannot be opened or yields nothing.
     */
    suspend fun record(
        maxMillis: Long,
        shouldStop: () -> Boolean,
        onProgress: (elapsedMillis: Long, amplitude: Float) -> Unit,
    ): FloatArray {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuffer <= 0) throw AudioCaptureException(RecognitionFailure.MICROPHONE_UNAVAILABLE)

        // A generous ring buffer: a stall in our read loop must not drop audio
        // mid-sentence, and the memory is trivial next to the model.
        val bufferBytes = minBuffer * BUFFER_MULTIPLIER

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL,
                ENCODING,
                bufferBytes,
            )
        } catch (denied: SecurityException) {
            // Missing RECORD_AUDIO reaches us as a SecurityException on newer
            // releases. It is a state the UI renders, not a crash.
            throw AudioCaptureException(RecognitionFailure.MICROPHONE_UNAVAILABLE, denied)
        } catch (bad: IllegalArgumentException) {
            throw AudioCaptureException(RecognitionFailure.MICROPHONE_UNAVAILABLE, bad)
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw AudioCaptureException(RecognitionFailure.MICROPHONE_UNAVAILABLE)
        }

        val maxSamples = ((SAMPLE_RATE * maxMillis) / 1000L).toInt()
        val samples = FloatArray(maxSamples)
        var written = 0
        val chunk = ShortArray(minBuffer / 2)

        try {
            try {
                recorder.startRecording()
            } catch (denied: SecurityException) {
                throw AudioCaptureException(RecognitionFailure.MICROPHONE_UNAVAILABLE, denied)
            } catch (state: IllegalStateException) {
                throw AudioCaptureException(RecognitionFailure.MICROPHONE_UNAVAILABLE, state)
            }
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw AudioCaptureException(RecognitionFailure.MICROPHONE_UNAVAILABLE)
            }

            while (written < maxSamples && !shouldStop()) {
                currentCoroutineContext().ensureActive()
                val read = recorder.read(chunk, 0, minOf(chunk.size, maxSamples - written))
                if (read <= 0) break

                var sumOfSquares = 0.0
                for (index in 0 until read) {
                    val sample = chunk[index] / PCM16_FULL_SCALE
                    samples[written + index] = sample
                    sumOfSquares += (sample * sample).toDouble()
                }
                written += read

                val rms = kotlin.math.sqrt(sumOfSquares / read).toFloat()
                onProgress(
                    (written * 1000L) / SAMPLE_RATE,
                    (rms * AMPLITUDE_GAIN).coerceIn(0f, 1f),
                )
            }
        } finally {
            // stop() throws if the recorder never started; release() must run regardless.
            runCatching { recorder.stop() }
            recorder.release()
        }

        if (written < MIN_USEFUL_SAMPLES) {
            throw AudioCaptureException(RecognitionFailure.RECORDING_FAILED)
        }
        return if (written == samples.size) samples else samples.copyOf(written)
    }

    internal companion object {
        /** Whisper's native rate. Anything else gets resampled by its front end. */
        const val SAMPLE_RATE = 16_000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        private const val BUFFER_MULTIPLIER = 4
        private const val PCM16_FULL_SCALE = 32_768f

        /** RMS of speech sits well below 1.0; scale it so the waveform is visible. */
        private const val AMPLITUDE_GAIN = 4f

        /** Below ~0.3 s there is nothing worth sending to the model. */
        private const val MIN_USEFUL_SAMPLES = SAMPLE_RATE / 3
    }
}

internal class AudioCaptureException(
    val reason: RecognitionFailure,
    cause: Throwable? = null,
) : Exception(cause)
