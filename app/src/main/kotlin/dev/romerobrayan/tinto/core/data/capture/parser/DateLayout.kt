package dev.romerobrayan.tinto.core.data.capture.parser

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Every capture timestamp is wall-clock America/Bogotá — the issuers are
 * Colombian banks writing local time, regardless of the device zone.
 */
val CaptureTimeZone: TimeZone = TimeZone.of("America/Bogota")

/**
 * One issuer date layout as data: a regex plus the builder that turns its
 * groups into a [LocalDateTime]. Layouts are declared per issuer rule so
 * day-first and year-first formats can never cross-match each other.
 */
class DateLayout(
    private val pattern: Regex,
    private val build: (MatchResult) -> LocalDateTime?,
) {

    /** First match in [body] as a Bogotá instant, or null (caller falls back). */
    fun findIn(body: String): Instant? =
        pattern.find(body)?.let(build)?.toInstant(CaptureTimeZone)
}

object DateLayouts {

    /**
     * Bancolombia: `07/07/26 a las 14:45`, `09/07/2026 a las 08:50`,
     * `15/07/26 16:56`, `16/07/2026 10:07:26` — day first, optional "a las",
     * optional seconds, 2- or 4-digit year.
     */
    val dayMonthYear = DateLayout(
        Regex("""\b(\d{2})/(\d{2})/(\d{2,4})(?:\s+a\s+las)?\s+(\d{1,2}):(\d{2})(?::(\d{2}))?"""),
    ) { match ->
        val (day, month, year, hour, minute) = match.destructured
        localDateTimeOrNull(
            year = expandYear(year.toInt()),
            month = month.toInt(),
            day = day.toInt(),
            hour = hour.toInt(),
            minute = minute.toInt(),
            second = match.groupValues[6].toIntOrNull() ?: 0,
        )
    }

    /** 1CERO1: `2026/05/20 a las 09:11:57` — year first, always with seconds. */
    val yearMonthDay = DateLayout(
        Regex("""\b(\d{4})/(\d{2})/(\d{2})\s+a\s+las\s+(\d{1,2}):(\d{2}):(\d{2})"""),
    ) { match ->
        val (year, month, day, hour, minute, second) = match.destructured
        localDateTimeOrNull(
            year = year.toInt(),
            month = month.toInt(),
            day = day.toInt(),
            hour = hour.toInt(),
            minute = minute.toInt(),
            second = second.toInt(),
        )
    }

    private fun expandYear(year: Int): Int = if (year < 100) 2000 + year else year

    /** Out-of-range fields (a "32/13" that matched) drop to null → receivedAt fallback. */
    private fun localDateTimeOrNull(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
    ): LocalDateTime? =
        runCatching { LocalDateTime(year, month, day, hour, minute, second) }.getOrNull()
}
