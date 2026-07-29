package dev.romerobrayan.tinto.core.data.speech

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romerobrayan.tinto.core.common.TintoDispatchers
import dev.romerobrayan.tinto.core.domain.model.ModelFailure
import dev.romerobrayan.tinto.core.domain.model.SpeechModel
import dev.romerobrayan.tinto.core.domain.model.SpeechModelState
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Downloads, verifies and caches Whisper model files.
 *
 * Deliberately plain `HttpURLConnection`: the app has no HTTP client and one
 * large GET does not justify adding one (see the dependency guardrail in
 * `CLAUDE.md`).
 *
 * The integrity rules are the interesting part, and they are strict on purpose —
 * a truncated model that still loads yields silently wrong transcripts, and this
 * app writes to a ledger:
 *
 * - Bytes land in `<id>.part` and are hashed **as they arrive**, so verification
 *   costs no second pass over 57 MB on the happy path.
 * - The file is renamed into place only after the digest matches. A file at the
 *   final path is therefore always verified — "exists" means "trustworthy".
 * - A mismatch deletes the partial file. A corrupt download must never become a
 *   permanent bad cache that retries keep resuming.
 * - No expected digest means no model. We fail closed.
 *
 * Files live in `filesDir`, not `cacheDir`: the OS must not evict something the
 * user waited minutes on mobile data for.
 */
@Singleton
class SpeechModelStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: TintoDispatchers,
) {

    private val state = MutableStateFlow<SpeechModelState>(SpeechModelState.NotDownloaded)
    val modelState: StateFlow<SpeechModelState> = state.asStateFlow()

    /** Serializes downloads so two callers await one transfer, not two. */
    private val downloadLock = Mutex()

    private fun modelsDir(): File = File(context.filesDir, MODELS_DIR).apply { mkdirs() }

    /** The verified file, or null when it is not on disk. */
    fun resolve(model: SpeechModel): File? = File(modelsDir(), model.id).takeIf { it.isFile }

    /** Updates [modelState] from disk without touching the network. */
    suspend fun refresh(model: SpeechModel) = withContext(dispatchers.io) {
        state.value = if (resolve(model) != null) {
            SpeechModelState.Ready
        } else {
            SpeechModelState.NotDownloaded
        }
    }

    /**
     * Returns the verified model file, downloading it first if needed.
     *
     * Returns null on failure, with the reason published to [modelState] —
     * failures are a state the UI renders, not an exception it catches.
     * Cancellation keeps the `.part` file so a retry resumes.
     */
    suspend fun ensure(model: SpeechModel): File? = withContext(dispatchers.io) {
        downloadLock.withLock {
            resolve(model)?.let {
                state.value = SpeechModelState.Ready
                return@withLock it
            }
            if (!model.hasKnownChecksum) {
                // Fail closed. See SpeechModel.UNKNOWN_CHECKSUM.
                state.value = SpeechModelState.Failed(ModelFailure.CHECKSUM_UNKNOWN)
                return@withLock null
            }
            try {
                download(model)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (io: IOException) {
                state.value = SpeechModelState.Failed(ModelFailure.NETWORK)
                null
            } catch (security: SecurityException) {
                state.value = SpeechModelState.Failed(ModelFailure.STORAGE)
                null
            }
        }
    }

    /** Drops any cached copy, verified or partial. */
    suspend fun evict(model: SpeechModel) = withContext(dispatchers.io) {
        File(modelsDir(), model.id).delete()
        File(modelsDir(), model.id + PART_SUFFIX).delete()
        state.value = SpeechModelState.NotDownloaded
    }

    private suspend fun download(model: SpeechModel): File? {
        val target = File(modelsDir(), model.id)
        val part = File(modelsDir(), model.id + PART_SUFFIX)
        val digest = MessageDigest.getInstance("SHA-256")

        // Resume only if the server honours it. The digest has to cover the whole
        // file, so bytes already on disk are re-read through it first — a cost
        // paid only on the retry path.
        var offset = if (part.isFile) part.length() else 0L
        if (offset > 0L) {
            if (!hashExisting(part, digest)) {
                part.delete()
                digest.reset()
                offset = 0L
            }
        }

        val connection = (URL(model.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = true
            if (offset > 0L) setRequestProperty("Range", "bytes=$offset-")
        }

        try {
            val responseCode = connection.responseCode
            // A server that ignores Range answers 200 with the whole file; start over.
            if (offset > 0L && responseCode == HttpURLConnection.HTTP_OK) {
                part.delete()
                digest.reset()
                offset = 0L
            }
            if (responseCode != HttpURLConnection.HTTP_OK &&
                responseCode != HttpURLConnection.HTTP_PARTIAL
            ) {
                state.value = SpeechModelState.Failed(ModelFailure.NETWORK)
                return null
            }

            val remaining = connection.contentLengthLong.takeIf { it > 0L } ?: 0L
            val total = if (remaining > 0L) offset + remaining else model.sizeBytes
            var downloaded = offset
            state.value = SpeechModelState.Downloading(downloaded, total)

            connection.inputStream.use { source ->
                FileOutputStream(part, /* append = */ offset > 0L).use { sink ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    var sinceReport = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = source.read(buffer)
                        if (read <= 0) break
                        sink.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        sinceReport += read
                        if (sinceReport >= PROGRESS_INTERVAL_BYTES) {
                            sinceReport = 0L
                            state.value = SpeechModelState.Downloading(downloaded, total)
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        state.value = SpeechModelState.Verifying
        val actual = digest.digest().toHexString()
        if (!actual.equals(model.sha256, ignoreCase = true)) {
            part.delete()
            state.value = SpeechModelState.Failed(ModelFailure.CHECKSUM_MISMATCH)
            return null
        }

        if (!part.renameTo(target)) {
            part.delete()
            state.value = SpeechModelState.Failed(ModelFailure.STORAGE)
            return null
        }
        state.value = SpeechModelState.Ready
        return target
    }

    /**
     * Feeds an existing partial file through [digest]. Returns false when it
     * could not be read, in which case the caller restarts from zero.
     */
    private suspend fun hashExisting(part: File, digest: MessageDigest): Boolean = try {
        part.inputStream().use { source ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = source.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (io: IOException) {
        false
    }

    private companion object {
        const val MODELS_DIR = "models"
        const val PART_SUFFIX = ".part"
        const val BUFFER_BYTES = 64 * 1024
        const val PROGRESS_INTERVAL_BYTES = 256L * 1024L
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
    }
}

/** Lowercase hex, the form checksums are published in. */
internal fun ByteArray.toHexString(): String =
    joinToString(separator = "") { byte -> "%02x".format(byte) }
