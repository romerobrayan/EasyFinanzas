package dev.romerobrayan.tinto.core.domain.model

import kotlinx.datetime.LocalDate

/**
 * One bar of the spend chart: total expenses inside [start, endExclusive).
 * Label formatting is a presentation concern and happens in the UI layer.
 */
data class ChartBucket(
    val start: LocalDate,
    val endExclusive: LocalDate,
    val total: Money,
)
