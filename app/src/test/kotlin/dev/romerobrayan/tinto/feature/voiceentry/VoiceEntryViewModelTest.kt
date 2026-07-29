package dev.romerobrayan.tinto.feature.voiceentry

import androidx.lifecycle.ViewModel
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.domain.model.ModelFailure
import dev.romerobrayan.tinto.core.domain.model.RecognitionFailure
import dev.romerobrayan.tinto.core.domain.model.RecognitionState
import dev.romerobrayan.tinto.core.domain.model.SpeechModel
import dev.romerobrayan.tinto.core.domain.model.SpeechModelState
import dev.romerobrayan.tinto.core.domain.repository.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins the voice-entry state machine.
 *
 * The cases that matter are the ones a screen-level bug would hide: recording
 * cannot start before the model and the permission are both in hand, releasing
 * the button must commit the utterance rather than cancel it, and every failure
 * has to reach the user as its own message instead of a generic one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceEntryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var recognizer: FakeSpeechRecognizer
    private lateinit var viewModel: VoiceEntryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        recognizer = FakeSpeechRecognizer()
        viewModel = VoiceEntryViewModel(recognizer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `recording is blocked until both the permission and the model are ready`() =
        runTest(dispatcher) {
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.canRecord)

            viewModel.onPermissionResult(granted = true, canAskAgain = true)
            advanceUntilIdle()
            // Permission alone is not enough — the model is still missing.
            assertFalse(viewModel.uiState.value.canRecord)

            recognizer.modelStateFlow.value = SpeechModelState.Ready
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.canRecord)
        }

    @Test
    fun `a denial that can be retried is distinct from a permanent one`() = runTest(dispatcher) {
        viewModel.onPermissionResult(granted = false, canAskAgain = true)
        advanceUntilIdle()
        assertEquals(MicPermission.DENIED, viewModel.uiState.value.permission)

        viewModel.onPermissionResult(granted = false, canAskAgain = false)
        advanceUntilIdle()
        assertEquals(MicPermission.PERMANENTLY_DENIED, viewModel.uiState.value.permission)
    }

    @Test
    fun `revoking the permission from settings is noticed on resume`() = runTest(dispatcher) {
        viewModel.onPermissionResult(granted = true, canAskAgain = true)
        advanceUntilIdle()
        assertEquals(MicPermission.GRANTED, viewModel.uiState.value.permission)

        viewModel.onPermissionChecked(granted = false)
        advanceUntilIdle()
        assertEquals(MicPermission.DENIED, viewModel.uiState.value.permission)
    }

    @Test
    fun `a never-asked permission stays unknown rather than showing as denied`() =
        runTest(dispatcher) {
            viewModel.onPermissionChecked(granted = false)
            advanceUntilIdle()
            assertEquals(MicPermission.UNKNOWN, viewModel.uiState.value.permission)
        }

    @Test
    fun `the full happy path walks recording to transcribing to an editable transcript`() =
        runTest(dispatcher) {
            grantEverything()

            viewModel.onRecordStart()
            recognizer.emit(RecognitionState.Recording(elapsedMillis = 1_200, amplitude = 0.4f))
            advanceUntilIdle()
            val recording = viewModel.uiState.value.phase
            assertTrue(recording is VoicePhase.Recording)
            assertEquals(1_200L, (recording as VoicePhase.Recording).elapsedMillis)

            recognizer.emit(RecognitionState.Transcribing)
            advanceUntilIdle()
            assertEquals(VoicePhase.Transcribing, viewModel.uiState.value.phase)

            recognizer.emit(RecognitionState.Success("gasté veinte mil pesos en almuerzo"))
            advanceUntilIdle()
            assertEquals(VoicePhase.Transcribed, viewModel.uiState.value.phase)
            assertEquals(
                "gasté veinte mil pesos en almuerzo",
                viewModel.uiState.value.transcript,
            )
        }

    @Test
    fun `releasing the button commits the utterance instead of cancelling it`() =
        runTest(dispatcher) {
            grantEverything()
            viewModel.onRecordStart()
            recognizer.emit(RecognitionState.Recording(500, 0.2f))
            advanceUntilIdle()

            viewModel.onRecordStop()
            advanceUntilIdle()

            assertTrue(recognizer.stopRecordingCalls == 1)
            // The flow is still live, so the transcript can still arrive.
            recognizer.emit(RecognitionState.Success("almuerzo"))
            advanceUntilIdle()
            assertEquals("almuerzo", viewModel.uiState.value.transcript)
        }

    @Test
    fun `cancelling a recording drops the audio and returns to idle`() = runTest(dispatcher) {
        grantEverything()
        viewModel.onRecordStart()
        recognizer.emit(RecognitionState.Recording(500, 0.2f))
        advanceUntilIdle()

        viewModel.onRecordCancelled()
        advanceUntilIdle()

        assertEquals(VoicePhase.Idle, viewModel.uiState.value.phase)
        assertEquals("", viewModel.uiState.value.transcript)
    }

    @Test
    fun `the transcript stays editable after it arrives`() = runTest(dispatcher) {
        grantEverything()
        viewModel.onRecordStart()
        recognizer.emit(RecognitionState.Success("veinte mil"))
        advanceUntilIdle()

        viewModel.onTranscriptChanged("veinte mil pesos")
        assertEquals("veinte mil pesos", viewModel.uiState.value.transcript)
    }

    @Test
    fun `every recognition failure maps to its own message`() = runTest(dispatcher) {
        val expected = mapOf(
            RecognitionFailure.MODEL_UNAVAILABLE to R.string.stt_error_model_unavailable,
            RecognitionFailure.MICROPHONE_UNAVAILABLE to R.string.stt_error_microphone_unavailable,
            RecognitionFailure.RECORDING_FAILED to R.string.stt_error_recording_failed,
            RecognitionFailure.TRANSCRIPTION_FAILED to R.string.stt_error_transcription_failed,
            RecognitionFailure.NO_SPEECH_DETECTED to R.string.stt_error_no_speech,
        )
        // Exhaustive by construction: a new enum case fails this assertion.
        assertEquals(RecognitionFailure.entries.size, expected.size)

        grantEverything()
        expected.forEach { (failure, messageRes) ->
            viewModel.onRecordStart()
            recognizer.emit(RecognitionState.Error(failure))
            advanceUntilIdle()
            assertEquals(messageRes, viewModel.uiState.value.errorRes)
            assertEquals(VoicePhase.Idle, viewModel.uiState.value.phase)
            viewModel.onRecordCancelled()
            advanceUntilIdle()
        }
    }

    @Test
    fun `every model failure maps to its own message`() = runTest(dispatcher) {
        val expected = mapOf(
            ModelFailure.NETWORK to R.string.stt_error_model_network,
            ModelFailure.CHECKSUM_MISMATCH to R.string.stt_error_model_checksum,
            ModelFailure.CHECKSUM_UNKNOWN to R.string.stt_error_model_checksum_unknown,
            ModelFailure.STORAGE to R.string.stt_error_model_storage,
        )
        assertEquals(ModelFailure.entries.size, expected.size)

        expected.forEach { (failure, messageRes) ->
            recognizer.modelStateFlow.value = SpeechModelState.Failed(failure)
            advanceUntilIdle()
            assertEquals(messageRes, viewModel.uiState.value.errorRes)
        }
    }

    @Test
    fun `an unverifiable model is refused rather than used`() = runTest(dispatcher) {
        viewModel.onPermissionResult(granted = true, canAskAgain = true)
        recognizer.modelStateFlow.value =
            SpeechModelState.Failed(ModelFailure.CHECKSUM_UNKNOWN)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canRecord)
        assertEquals(
            R.string.stt_error_model_checksum_unknown,
            viewModel.uiState.value.errorRes,
        )
    }

    @Test
    fun `download progress reaches the ui state`() = runTest(dispatcher) {
        recognizer.modelStateFlow.value = SpeechModelState.Downloading(25_000_000, 50_000_000)
        advanceUntilIdle()

        val model = viewModel.uiState.value.model
        assertTrue(model is SpeechModelState.Downloading)
        assertEquals(0.5f, (model as SpeechModelState.Downloading).fraction, 0.001f)
        assertTrue(viewModel.uiState.value.isBusy)
    }

    @Test
    fun `starting a new recording clears the previous error and transcript`() =
        runTest(dispatcher) {
            grantEverything()
            viewModel.onRecordStart()
            recognizer.emit(RecognitionState.Error(RecognitionFailure.NO_SPEECH_DETECTED))
            advanceUntilIdle()
            assertEquals(R.string.stt_error_no_speech, viewModel.uiState.value.errorRes)
            viewModel.onRecordCancelled()
            advanceUntilIdle()

            viewModel.onRecordStart()
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.errorRes)
            assertEquals("", viewModel.uiState.value.transcript)
        }

    @Test
    fun `clearing the view model releases the native context`() = runTest(dispatcher) {
        grantEverything()
        viewModel.onRecordStart()
        advanceUntilIdle()

        viewModel.clearForTest()
        advanceUntilIdle()

        assertTrue(recognizer.released)
    }

    /**
     * Must advance: the model state reaches the UI through a `viewModelScope`
     * collector, which under `StandardTestDispatcher` has not run yet. Without
     * this the preconditions look unmet and `onRecordStart` silently no-ops.
     */
    private fun TestScope.grantEverything() {
        viewModel.onPermissionResult(granted = true, canAskAgain = true)
        recognizer.modelStateFlow.value = SpeechModelState.Ready
        advanceUntilIdle()
    }
}

/**
 * `onCleared` is protected on [ViewModel]; reflection keeps the production API
 * unchanged rather than widening it just so a test can reach it. Same helper
 * shape as `DashboardSpeechTest`.
 */
private fun VoiceEntryViewModel.clearForTest() {
    ViewModel::class.java.getDeclaredMethod("onCleared")
        .apply { isAccessible = true }
        .invoke(this)
}

/**
 * A recognizer the test drives by hand. Recognition states are pushed through a
 * channel so the test controls exactly when each one lands, which is what makes
 * the intermediate phases observable.
 */
private class FakeSpeechRecognizer : SpeechRecognizer {

    val modelStateFlow = MutableStateFlow<SpeechModelState>(SpeechModelState.NotDownloaded)

    /**
     * One buffered channel for the fake's whole life, not one per [recognize].
     * Under `StandardTestDispatcher` the collector does not start until
     * `advanceUntilIdle`, so a per-call channel would drop everything the test
     * emitted between `onRecordStart()` and that point.
     */
    private val channel = Channel<RecognitionState>(Channel.UNLIMITED)

    var stopRecordingCalls = 0
        private set
    var released = false
        private set
    var prepareCalls = 0
        private set

    override val model: SpeechModel = SpeechModel(
        id = "fake.bin",
        url = "https://example.invalid/fake.bin",
        sha256 = "a".repeat(SpeechModel.SHA256_HEX_LENGTH),
        sizeBytes = 50_000_000L,
    )

    override val modelState: StateFlow<SpeechModelState> = modelStateFlow

    override suspend fun refreshModelState() = Unit

    override suspend fun prepareModel() {
        prepareCalls++
    }

    override fun recognize(): Flow<RecognitionState> = channel.receiveAsFlow()

    override fun stopRecording() {
        stopRecordingCalls++
    }

    override fun release() {
        released = true
    }

    fun emit(state: RecognitionState) {
        channel.trySend(state)
    }
}
