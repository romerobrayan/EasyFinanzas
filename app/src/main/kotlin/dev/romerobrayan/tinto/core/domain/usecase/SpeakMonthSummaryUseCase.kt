package dev.romerobrayan.tinto.core.domain.usecase

import dev.romerobrayan.tinto.core.domain.model.MonthSummary
import dev.romerobrayan.tinto.core.domain.model.SpeechOutcome
import dev.romerobrayan.tinto.core.domain.repository.MonthSummaryNarrator
import dev.romerobrayan.tinto.core.domain.repository.SpeechSynthesizer
import javax.inject.Inject

/**
 * Speaks a month summary: narrator turns the figures into a sentence, synthesizer
 * reads it. Keeping the two behind their interfaces is what lets the wording and
 * the speech engine be replaced independently — see `docs/tts.md`.
 *
 * Suspends until the utterance finishes; cancelling the caller stops playback.
 */
class SpeakMonthSummaryUseCase @Inject constructor(
    private val narrator: MonthSummaryNarrator,
    private val synthesizer: SpeechSynthesizer,
) {

    suspend operator fun invoke(
        summary: MonthSummary,
        onStarted: () -> Unit = {},
    ): SpeechOutcome = synthesizer.speak(narrator.narrate(summary), onStarted)

    fun stop() = synthesizer.stop()

    fun release() = synthesizer.shutdown()
}
