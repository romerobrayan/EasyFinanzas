package dev.romerobrayan.tinto.core.data.speech

/**
 * The entire JNI surface, kept to four calls so the native side stays a thin
 * translation layer rather than a place logic can hide.
 *
 * **Nothing here is thread-safe.** `whisper_context` cannot be used from two
 * threads at once, and this object does not defend against that — the
 * serialization lives in `WhisperSpeechRecognizer`, which confines every call
 * below to a single-threaded dispatcher. Do not call these from anywhere else.
 */
internal object WhisperNative {

    /**
     * False when the shared library is missing for this ABI, which turns an
     * `UnsatisfiedLinkError` at an arbitrary call site into a state the UI can
     * render.
     */
    val isAvailable: Boolean by lazy {
        try {
            System.loadLibrary("tinto_whisper")
            true
        } catch (missing: UnsatisfiedLinkError) {
            false
        }
    }

    /**
     * Loads a ggml model with `whisper_init_from_file` (mmap — the 57 MB is not
     * copied onto the heap).
     *
     * @return an opaque context handle, or 0 on failure.
     */
    external fun initContext(modelPath: String): Long

    /** Releases a handle from [initContext]. Safe with 0. */
    external fun freeContext(handle: Long)

    /**
     * Runs inference over normalized 16 kHz mono samples.
     *
     * Blocking and potentially several seconds long — see
     * `docs/stt-whisper-model-choice.md`.
     *
     * @return the transcript, or null if inference failed.
     */
    external fun transcribe(handle: Long, audio: FloatArray, threads: Int): String?
}
