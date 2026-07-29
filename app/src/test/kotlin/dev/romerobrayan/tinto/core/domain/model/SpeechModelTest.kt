package dev.romerobrayan.tinto.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the fail-closed rule around model checksums: a model without a
 * full-length digest is unusable rather than unverified-but-accepted, and the
 * shipped models must always carry real digests.
 */
class SpeechModelTest {

    @Test
    fun `a model with no digest is not considered verifiable`() {
        val model = SpeechModel(
            id = "x.bin",
            url = "https://example.invalid/x.bin",
            sha256 = "",
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
    fun `the shipped models carry real, distinct digests`() {
        assertTrue(SpeechModel.BASE_Q5_1.hasKnownChecksum)
        assertTrue(SpeechModel.TINY_Q5_1.hasKnownChecksum)
        // Two different files can never share a digest; equal ones would mean a
        // copy-paste slip that verification could not catch.
        assertTrue(SpeechModel.BASE_Q5_1.sha256 != SpeechModel.TINY_Q5_1.sha256)
    }

    @Test
    fun `download progress is a clamped fraction`() {
        assertEquals(0.5f, SpeechModelState.Downloading(50L, 100L).fraction, 0.0001f)
        // Total unknown until the server answers — report no progress, not NaN.
        assertEquals(0f, SpeechModelState.Downloading(10L, 0L).fraction, 0.0001f)
        assertEquals(1f, SpeechModelState.Downloading(200L, 100L).fraction, 0.0001f)
    }
}
