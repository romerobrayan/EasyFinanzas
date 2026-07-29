package dev.romerobrayan.tinto.core.domain.repository

import dev.romerobrayan.tinto.core.domain.model.MonthSummary

/**
 * Turns a [MonthSummary] into the sentence the synthesizer reads.
 *
 * This is a seam, not an abstraction for its own sake: the domain may not read
 * `strings.xml`, and the project forbids hardcoded Spanish in Kotlin, so the
 * wording lives in a data-layer implementation with resource access. It is also
 * where an LLM-generated narration would later plug in — independently of which
 * [SpeechSynthesizer] is bound.
 */
interface MonthSummaryNarrator {

    fun narrate(summary: MonthSummary): String
}
