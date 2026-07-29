package dev.romerobrayan.tinto.feature.voiceentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.domain.model.ModelFailure
import dev.romerobrayan.tinto.core.domain.model.RecognitionFailure
import dev.romerobrayan.tinto.core.domain.model.RecognitionState
import dev.romerobrayan.tinto.core.domain.model.SpeechModelState
import dev.romerobrayan.tinto.core.domain.repository.SpeechRecognizer
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the voice-entry screen.
 *
 * Two independent lifecycles are folded into one UI state here: the model
 * download (minutes, once) and a recognition (seconds, repeatable). The screen
 * renders whichever is blocking.
 */
@HiltViewModel
class VoiceEntryViewModel @Inject constructor(
    private val recognizer: SpeechRecognizer,
) : ViewModel() {

    private val state = MutableStateFlow(
        VoiceEntryUiState(modelSizeMb = (recognizer.model.sizeBytes / BYTES_PER_MB).toInt()),
    )
    val uiState: StateFlow<VoiceEntryUiState> = state.asStateFlow()

    private var recognitionJob: Job? = null
    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            recognizer.modelState.collect { modelState ->
                state.update {
                    it.copy(
                        model = modelState,
                        errorRes = modelState.errorRes() ?: it.errorRes,
                    )
                }
            }
        }
        viewModelScope.launch { recognizer.refreshModelState() }
    }

    /**
     * The screen reports the permission result here rather than branching on it
     * itself — the launcher needs an Activity, but the decision does not.
     */
    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        state.update {
            it.copy(
                permission = when {
                    granted -> MicPermission.GRANTED
                    canAskAgain -> MicPermission.DENIED
                    else -> MicPermission.PERMANENTLY_DENIED
                },
                errorRes = null,
            )
        }
    }

    /**
     * Called on resume with the current grant, so revoking the permission in
     * system settings lands without a restart. A still-[MicPermission.UNKNOWN]
     * state stays unknown: showing "denied" before we ever asked would be a lie.
     */
    fun onPermissionChecked(granted: Boolean) {
        state.update {
            when {
                granted -> it.copy(permission = MicPermission.GRANTED)
                it.permission == MicPermission.GRANTED -> it.copy(permission = MicPermission.DENIED)
                else -> it
            }
        }
    }

    fun onDownloadModel() {
        if (downloadJob?.isActive == true) return
        state.update { it.copy(errorRes = null) }
        downloadJob = viewModelScope.launch { recognizer.prepareModel() }
    }

    /** Button pressed. */
    fun onRecordStart() {
        if (!state.value.canRecord || recognitionJob?.isActive == true) return
        state.update { it.copy(errorRes = null, transcript = "", phase = VoicePhase.Idle) }
        // recognize() is called HERE, synchronously, not inside the launch: its
        // factory resets the stop flag, and a fast tap can deliver onRecordStop
        // before the coroutine below ever runs. Built eagerly, press strictly
        // precedes release; built lazily, a quick tap recorded 15 s of silence.
        val recognitionFlow = recognizer.recognize()
        recognitionJob = viewModelScope.launch {
            recognitionFlow.collect { recognition ->
                state.update { current ->
                    when (recognition) {
                        is RecognitionState.Idle ->
                            current.copy(phase = VoicePhase.Idle)
                        is RecognitionState.Recording ->
                            current.copy(
                                phase = VoicePhase.Recording(
                                    recognition.elapsedMillis,
                                    recognition.amplitude,
                                ),
                            )
                        is RecognitionState.Transcribing ->
                            current.copy(phase = VoicePhase.Transcribing)
                        is RecognitionState.Success ->
                            current.copy(
                                phase = VoicePhase.Transcribed,
                                transcript = recognition.text,
                            )
                        is RecognitionState.Error ->
                            current.copy(
                                phase = VoicePhase.Idle,
                                errorRes = recognition.reason.messageRes(),
                            )
                    }
                }
            }
        }
    }

    /** Button released: commit the utterance and transcribe it. */
    fun onRecordStop() {
        recognizer.stopRecording()
    }

    /** Backing out of a recording entirely — throws the audio away. */
    fun onRecordCancelled() {
        recognitionJob?.cancel()
        recognitionJob = null
        state.update { it.copy(phase = VoicePhase.Idle) }
    }

    fun onTranscriptChanged(value: String) {
        state.update { it.copy(transcript = value) }
    }

    override fun onCleared() {
        recognitionJob?.cancel()
        recognizer.release()
        super.onCleared()
    }

    private fun SpeechModelState.errorRes(): Int? = when (this) {
        is SpeechModelState.Failed -> when (reason) {
            ModelFailure.NETWORK -> R.string.stt_error_model_network
            ModelFailure.CHECKSUM_MISMATCH -> R.string.stt_error_model_checksum
            ModelFailure.CHECKSUM_UNKNOWN -> R.string.stt_error_model_checksum_unknown
            ModelFailure.STORAGE -> R.string.stt_error_model_storage
        }
        else -> null
    }

    private fun RecognitionFailure.messageRes(): Int = when (this) {
        RecognitionFailure.MODEL_UNAVAILABLE -> R.string.stt_error_model_unavailable
        RecognitionFailure.MICROPHONE_UNAVAILABLE -> R.string.stt_error_microphone_unavailable
        RecognitionFailure.RECORDING_FAILED -> R.string.stt_error_recording_failed
        RecognitionFailure.TRANSCRIPTION_FAILED -> R.string.stt_error_transcription_failed
        RecognitionFailure.NO_SPEECH_DETECTED -> R.string.stt_error_no_speech
    }

    private companion object {
        const val BYTES_PER_MB = 1_000_000L
    }
}
