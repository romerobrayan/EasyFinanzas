package dev.romerobrayan.tinto.core.domain.model

/**
 * A downloadable Whisper model, as **data rather than a constant**.
 *
 * `docs/stt-whisper-model-choice.md` picks `base-q5_1` on 64-bit ARM and
 * `tiny-q5_1` on 32-bit, but that decision was made without ever running the
 * models on a real device — it is explicitly provisional. Keeping the identity
 * in a value object means revisiting it is editing this file, not rewriting a
 * layer. Same instinct as `IssuerRules` in the capture parser.
 *
 * [sha256] is not optional. A truncated or tampered 57 MB download that still
 * loads would produce silently wrong transcripts, and this app writes to a
 * ledger.
 */
data class SpeechModel(
    /** Stable id, also the on-disk filename. */
    val id: String,
    /** Where to fetch it. */
    val url: String,
    /** Lowercase hex SHA-256 of the complete file. */
    val sha256: String,
    /** Expected size, used for download progress before the server answers. */
    val sizeBytes: Long,
) {
    /**
     * True when [sha256] is still the unfilled placeholder. The download refuses
     * to hand back a model it cannot verify — see `SpeechModelStore`.
     */
    val hasKnownChecksum: Boolean
        get() = sha256.length == SHA256_HEX_LENGTH && sha256 != UNKNOWN_CHECKSUM

    companion object {
        const val SHA256_HEX_LENGTH: Int = 64

        /**
         * Placeholder for a checksum we do not have yet.
         *
         * The build environment that introduced this feature could not reach
         * `huggingface.co`, so the real digests could not be computed. Fill them
         * in from the model files (`sha256sum ggml-base-q5_1.bin`, or the SHA-256
         * shown on the HuggingFace file page) before shipping. Until then the
         * store fails with [ModelFailure.CHECKSUM_UNKNOWN] rather than trusting
         * an unverified download.
         */
        const val UNKNOWN_CHECKSUM: String = ""

        // TODO(sprint-N): fill both digests from the real files, then delete
        //  UNKNOWN_CHECKSUM and the CHECKSUM_UNKNOWN failure path.
        val BASE_Q5_1: SpeechModel = SpeechModel(
            id = "ggml-base-q5_1.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin",
            sha256 = UNKNOWN_CHECKSUM,
            sizeBytes = 57_000_000L,
        )

        val TINY_Q5_1: SpeechModel = SpeechModel(
            id = "ggml-tiny-q5_1.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin",
            sha256 = UNKNOWN_CHECKSUM,
            sizeBytes = 31_000_000L,
        )
    }
}

/** Availability of the on-device model. Orthogonal to [RecognitionState]. */
sealed interface SpeechModelState {

    /** Never fetched, or a previous attempt was discarded. */
    data object NotDownloaded : SpeechModelState

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : SpeechModelState {
        /** 0f..1f, or 0f when the total is not yet known. */
        val fraction: Float
            get() = if (totalBytes <= 0L) 0f else (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    }

    /** Bytes are down; hashing before the file is accepted. */
    data object Verifying : SpeechModelState

    /** Downloaded, verified, usable offline. */
    data object Ready : SpeechModelState

    data class Failed(val reason: ModelFailure) : SpeechModelState
}

enum class ModelFailure {
    /** Could not reach the host, or the transfer broke. Retry resumes. */
    NETWORK,

    /** The file downloaded but hashed to something else. It was deleted. */
    CHECKSUM_MISMATCH,

    /** We have no expected digest to compare against — see [SpeechModel.UNKNOWN_CHECKSUM]. */
    CHECKSUM_UNKNOWN,

    /** No room, or the file could not be written. */
    STORAGE,
}
