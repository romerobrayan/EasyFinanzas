package dev.romerobrayan.tinto.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the fail-closed rule around model checksums.
 *
 * The digests shipped today are placeholders — the environment that added this
 * feature could not reach the model host to compute them. That is only safe
 * because an unfilled checksum makes the model unusable rather than
 * unverified-but-accepted, and this test is what keeps that true if someone
 * later "tidies up" the placeholder.
 */
class SpeechModelTest {

    @Test
    fun `a model with no digest is not considered verifiable`() {
        val model = SpeechModel(
            id = "x.bin",
            url = "https://example.invalid/x.bin",
            sha256 = SpeechModel.UNKNOWN_CHECKSUM,
            sizeBytes = 1L,
        )
        assertFalse(model.hasKnownChecksum)
    }

    @Test
    fun `a truncated digest is not accepted as a digest`() {
        val model = SpeechModel(
            id = "x.bin",
            url = "https://example.invalid/x.bin",
            sha256 = "abc123",
            sizeBytes = 1L,
        )
        assertFalse(model.hasKnownChecksum)
    }

    @Test
    fun `a full length digest is verifiable`() {
        val model = SpeechModel(
            id = "x.bin",
            url = "https://example.invalid/x.bin",
            sha256 = "0".repeat(SpeechModel.SHA256_HEX_LENGTH),
            sizeBytes = 1L,
        )
        assertTrue(model.hasKnownChecksum)
    }

    @Test
    fun `the shipped models still carry placeholder digests`() {
        // Deliberately asserts the current, known-incomplete state. When the real
        // digests land this test fails, which is the reminder to delete it and
        // the UNKNOWN_CHECKSUM path along with it.
        assertFalse(SpeechModel.BASE_Q5_1.hasKnownChecksum)
        assertFalse(SpeechModel.TINY_Q5_1.hasKnownChecksum)
    }

    @Test
    fun `download progress is a clamped fraction`() {
        assertEquals(0.5f, SpeechModelState.Downloading(50L, 100L).fraction, 0.0001f)
        // Total unknown until the server answers — report no progress, not NaN.
        assertEquals(0f, SpeechModelState.Downloading(10L, 0L).fraction, 0.0001f)
        assertEquals(1f, SpeechModelState.Downloading(200L, 100L).fraction, 0.0001f)
    }
}
