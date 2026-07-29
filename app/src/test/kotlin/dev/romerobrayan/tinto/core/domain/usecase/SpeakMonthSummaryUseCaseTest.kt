package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.MonthSummary
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.SpeechFailure
import dev.romerobrayan.tinto.core.domain.model.SpeechOutcome
import dev.romerobrayan.tinto.core.domain.repository.MonthSummaryNarrator
import dev.romerobrayan.tinto.core.domain.repository.SpeechSynthesizer
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The use case is deliberately thin, so what is worth pinning is that it does not
 * mangle anything on the way through: the narrator's text reaches the synthesizer
 * verbatim, and every outcome comes back unchanged.
 */
class SpeakMonthSummaryUseCaseTest {

    private val summary = MonthSummary(
        month = LocalDate(2026, 7, 1),
        total = Money.ofPesos(80_000),
        comparison = null,
        topCategory = null,
    )

    @Test
    fun `the narrated text is what gets spoken`() = runTest {
        val synthesizer = FakeSpeechSynthesizer()
        val useCase = SpeakMonthSummaryUseCase(FixedNarrator("En julio gastaste 80000 pesos."), synthesizer)

        useCase(summary)

        assertEquals("En julio gastaste 80000 pesos.", synthesizer.spokenText)
    }

    @Test
    fun `completion is passed straight through`() = runTest {
        val synthesizer = FakeSpeechSynthesizer(outcome = SpeechOutcome.Completed)
        val useCase = SpeakMonthSummaryUseCase(FixedNarrator("hola"), synthesizer)

        assertEquals(SpeechOutcome.Completed, useCase(summary))
    }

    @Test
    fun `every failure reason survives the round trip`() = runTest {
        for (reason in SpeechFailure.entries) {
            val synthesizer = FakeSpeechSynthesizer(outcome = SpeechOutcome.Failed(reason))
            val useCase = SpeakMonthSummaryUseCase(FixedNarrator("hola"), synthesizer)

            assertEquals(SpeechOutcome.Failed(reason), useCase(summary))
        }
    }

    @Test
    fun `onStarted is handed to the synthesizer rather than invoked eagerly`() = runTest {
        val synthesizer = FakeSpeechSynthesizer()
        val useCase = SpeakMonthSummaryUseCase(FixedNarrator("hola"), synthesizer)
        var started = false

        useCase(summary) { started = true }

        assertTrue(started)
        assertEquals(1, synthesizer.speakCount)
    }

    @Test
    fun `release shuts the engine down`() {
        val synthesizer = FakeSpeechSynthesizer()
        val useCase = SpeakMonthSummaryUseCase(FixedNarrator("hola"), synthesizer)

        useCase.stop()
        useCase.release()

        assertEquals(1, synthesizer.stopCount)
        assertEquals(1, synthesizer.shutdownCount)
    }
}

private class FixedNarrator(private val text: String) : MonthSummaryNarrator {
    override fun narrate(summary: MonthSummary): String = text
}

private class FakeSpeechSynthesizer(
    private val outcome: SpeechOutcome = SpeechOutcome.Completed,
) : SpeechSynthesizer {

    var spokenText: String? = null
    var speakCount = 0
    var stopCount = 0
    var shutdownCount = 0

    override suspend fun speak(text: String, onStarted: () -> Unit): SpeechOutcome {
        spokenText = text
        speakCount++
        onStarted()
        return outcome
    }

    override fun stop() {
        stopCount++
    }

    override fun shutdown() {
        shutdownCount++
    }
}
