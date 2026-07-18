package dev.romerobrayan.tinto.core.data.capture.parser

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

/**
 * Normalizes the three date layouts seen in issuer messages against
 * America/Bogota:
 *
 * - `DD/MM/YY`  (Bancolombia: `07/07/26`)
 * - `DD/MM/YYYY` (Bancolombia: `16/07/2026`)
 * - `YYYY/MM/DD` (1CERO1: `2026/05/20`)
 *
 * with an optional `HH:mm` / `HH:mm:ss` time. A message without a parsable
 * absolute date falls back to the capture's received timestamp.
 */
object CaptureDateParser {

    val issuerTimeZone: TimeZone = TimeZone.of("America/Bogota")

    fun parse(dateText: String?, timeText: String?, receivedAt: Instant): Instant {
        val date = dateText?.let(::parseDate) ?: return receivedAt
        val time = timeText?.let(::parseTime) ?: LocalTime(NOON_HOUR, 0)
        return date.atTime(time).toInstant(issuerTimeZone)
    }

    private fun parseDate(text: String): LocalDate? {
        val parts = text.trim().split('/')
        if (parts.size != 3 || parts.any { it.toIntOrNull() == null }) return null
        val numbers = parts.map { it.toInt() }
        return runCatching {
            when {
                // YYYY/MM/DD — 1CERO1.
                parts[0].length == 4 -> LocalDate(numbers[0], numbers[1], numbers[2])
                // DD/MM/YYYY — Bancolombia long form.
                parts[2].length == 4 -> LocalDate(numbers[2], numbers[1], numbers[0])
                // DD/MM/YY — Bancolombia short form.
                else -> LocalDate(CENTURY + numbers[2], numbers[1], numbers[0])
            }
        }.getOrNull()
    }

    private fun parseTime(text: String): LocalTime? {
        val parts = text.trim().split(':')
        if (parts.size !in 2..3 || parts.any { it.toIntOrNull() == null }) return null
        return runCatching {
            LocalTime(parts[0].toInt(), parts[1].toInt(), parts.getOrNull(2)?.toInt() ?: 0)
        }.getOrNull()
    }

    private const val CENTURY = 2000
    private const val NOON_HOUR = 12
}
