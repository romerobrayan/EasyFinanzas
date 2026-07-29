package dev.romerobrayan.tinto.core.data.speech

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.romerobrayan.tinto.core.common.TintoDispatchers
import dev.romerobrayan.tinto.core.domain.model.ModelFailure
import dev.romerobrayan.tinto.core.domain.model.SpeechModel
import dev.romerobrayan.tinto.core.domain.model.SpeechModelState
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The download path is the one thing a JVM fake cannot vouch for: it needs a
 * real filesystem, a real `HttpURLConnection`, and a server that can refuse or
 * honour a `Range` header.
 *
 * The properties under test are the integrity ones. A file at the final path
 * must always be verified, a corrupt download must never survive to be resumed
 * forever, and a model we cannot check must not be used at all.
 *
 * Uses a hand-rolled socket server rather than MockWebServer — a few dozen lines
 * against a new test dependency, per the guardrail in `CLAUDE.md`.
 */
@RunWith(AndroidJUnit4::class)
class SpeechModelStoreTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dispatchers = TintoDispatchers(io = Dispatchers.IO, default = Dispatchers.Default)

    private lateinit var server: FakeModelServer
    private lateinit var store: SpeechModelStore
    private lateinit var payload: ByteArray

    private val modelsDir: File get() = File(context.filesDir, "models")

    @Before
    fun setUp() {
        modelsDir.deleteRecursively()
        payload = ByteArray(PAYLOAD_BYTES) { index -> (index % 251).toByte() }
        server = FakeModelServer(payload).apply { start() }
        store = SpeechModelStore(context, dispatchers)
    }

    @After
    fun tearDown() {
        server.stop()
        modelsDir.deleteRecursively()
    }

    @Test
    fun downloadsVerifiesAndCachesTheModel() = runTest {
        val model = modelWith(sha256 = payload.sha256Hex())

        val file = store.ensure(model)

        assertNotNull(file)
        assertEquals(SpeechModelState.Ready, store.modelState.value)
        assertTrue(file!!.isFile)
        assertEquals(payload.size.toLong(), file.length())
        // The partial file must not be left behind.
        assertFalse(File(modelsDir, model.id + ".part").exists())
    }

    @Test
    fun aCachedModelIsReturnedWithoutHittingTheNetwork() = runTest {
        val model = modelWith(sha256 = payload.sha256Hex())
        store.ensure(model)
        val requestsAfterFirst = server.requestCount.get()

        val second = store.ensure(model)

        assertNotNull(second)
        assertEquals(requestsAfterFirst, server.requestCount.get())
    }

    @Test
    fun aCorruptDownloadIsDeletedRatherThanCached() = runTest {
        // The bytes are fine; the expectation is not. Same observable situation
        // as a tampered or truncated file.
        val model = modelWith(sha256 = "0".repeat(SpeechModel.SHA256_HEX_LENGTH))

        val file = store.ensure(model)

        assertNull(file)
        assertEquals(
            SpeechModelState.Failed(ModelFailure.CHECKSUM_MISMATCH),
            store.modelState.value,
        )
        assertFalse(File(modelsDir, model.id).exists())
        // Critically: the partial file is gone too, so a retry starts clean
        // instead of resuming into the same bad digest forever.
        assertFalse(File(modelsDir, model.id + ".part").exists())
    }

    @Test
    fun aModelWithNoKnownChecksumIsRefusedWithoutDownloading() = runTest {
        val model = modelWith(sha256 = SpeechModel.UNKNOWN_CHECKSUM)

        val file = store.ensure(model)

        assertNull(file)
        assertEquals(
            SpeechModelState.Failed(ModelFailure.CHECKSUM_UNKNOWN),
            store.modelState.value,
        )
        assertEquals(0, server.requestCount.get())
    }

    @Test
    fun anInterruptedDownloadResumesFromWhereItStopped() = runTest {
        val model = modelWith(sha256 = payload.sha256Hex())
        // Simulate a transfer that died partway: bytes on disk, no final file.
        modelsDir.mkdirs()
        val part = File(modelsDir, model.id + ".part")
        part.writeBytes(payload.copyOf(PAYLOAD_BYTES / 3))

        val file = store.ensure(model)

        assertNotNull(file)
        assertEquals(payload.size.toLong(), file!!.length())
        assertTrue(file.readBytes().contentEquals(payload))
        // The server was asked to continue, not to start over.
        assertEquals(PAYLOAD_BYTES / 3, server.lastRangeStart.get())
    }

    @Test
    fun aCorruptPartialFileIsCaughtAtTheEndAndTheNextRetryStartsClean() = runTest {
        val model = modelWith(sha256 = payload.sha256Hex())
        modelsDir.mkdirs()
        // Garbage where a real prefix should be. Resume cannot detect this
        // mid-flight — the digest only covers the whole file — so the check lands
        // at the end. What matters is that it lands at all, and that the bad
        // bytes are then thrown away instead of being resumed into forever.
        File(modelsDir, model.id + ".part").writeBytes(ByteArray(PAYLOAD_BYTES / 3) { 7 })

        val firstAttempt = store.ensure(model)

        assertNull(firstAttempt)
        assertEquals(
            SpeechModelState.Failed(ModelFailure.CHECKSUM_MISMATCH),
            store.modelState.value,
        )
        assertFalse(File(modelsDir, model.id + ".part").exists())

        // The retry has nothing to resume from, so it succeeds.
        val secondAttempt = store.ensure(model)
        assertNotNull(secondAttempt)
        assertTrue(secondAttempt!!.readBytes().contentEquals(payload))
    }

    @Test
    fun refreshReportsWhatIsActuallyOnDisk() = runTest {
        val model = modelWith(sha256 = payload.sha256Hex())

        store.refresh(model)
        assertEquals(SpeechModelState.NotDownloaded, store.modelState.value)

        store.ensure(model)
        store.refresh(model)
        assertEquals(SpeechModelState.Ready, store.modelState.value)

        store.evict(model)
        store.refresh(model)
        assertEquals(SpeechModelState.NotDownloaded, store.modelState.value)
    }

    @Test
    fun downloadProgressIsReportedWhileBytesArrive() = runTest {
        val model = modelWith(sha256 = payload.sha256Hex())
        val seen = mutableListOf<SpeechModelState>()
        backgroundScope.launch(Dispatchers.Default) {
            store.modelState.collect { seen += it }
        }

        store.ensure(model)

        assertTrue(
            "expected at least one Downloading state, saw $seen",
            seen.any { it is SpeechModelState.Downloading },
        )
        assertTrue(seen.contains(SpeechModelState.Ready))
    }

    private fun modelWith(sha256: String) = SpeechModel(
        id = "test-model.bin",
        url = server.url(),
        sha256 = sha256,
        sizeBytes = PAYLOAD_BYTES.toLong(),
    )

    private companion object {
        // Big enough to cross the progress-report interval more than once.
        const val PAYLOAD_BYTES = 3 * 1024 * 1024
    }
}

private fun ByteArray.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString(separator = "") { "%02x".format(it) }

/**
 * A single-file HTTP server that understands `Range`, so the resume path is
 * exercised against something that behaves like the real host.
 */
private class FakeModelServer(private val payload: ByteArray) {

    private val socket = ServerSocket(0)
    private var running = true

    val requestCount = AtomicInteger(0)
    val lastRangeStart = AtomicInteger(-1)

    fun url(): String = "http://127.0.0.1:${socket.localPort}/model.bin"

    fun start() {
        thread(isDaemon = true) {
            while (running) {
                val client = try {
                    socket.accept()
                } catch (closed: Exception) {
                    return@thread
                }
                thread(isDaemon = true) { serve(client) }
            }
        }
    }

    private fun serve(client: Socket) = client.use {
        requestCount.incrementAndGet()
        val reader = client.getInputStream().bufferedReader()
        var rangeStart = 0

        // Read the request head; blank line terminates it.
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Range:", ignoreCase = true)) {
                rangeStart = line.substringAfter("bytes=").substringBefore('-').trim().toIntOrNull() ?: 0
            }
        }
        lastRangeStart.set(rangeStart)

        val body = payload.copyOfRange(rangeStart, payload.size)
        val status = if (rangeStart > 0) "206 Partial Content" else "200 OK"
        val head = buildString {
            append("HTTP/1.1 $status\r\n")
            append("Content-Length: ${body.size}\r\n")
            if (rangeStart > 0) {
                append("Content-Range: bytes $rangeStart-${payload.size - 1}/${payload.size}\r\n")
            }
            append("Connection: close\r\n\r\n")
        }
        client.getOutputStream().apply {
            write(head.toByteArray())
            write(body)
            flush()
        }
    }

    fun stop() {
        running = false
        runCatching { socket.close() }
    }
}
