package dev.romerobrayan.tinto.core.common

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime

/**
 * Spanish (es-CO) date labels for the UI. Month/day names come from platform
 * locale data rather than hardcoded strings; static UI templates stay in
 * strings.xml.
 */
object Dates {

    private val locale = Locale.forLanguageTag("es-CO")

    /** "Julio 2026" */
    fun monthYearLabel(date: LocalDate): String =
        "${monthName(date).replaceFirstChar { it.titlecase(locale) }} ${date.year}"

    /** "julio" */
    fun monthName(date: LocalDate): String =
        date.toJavaLocalDate().month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .lowercase(locale)

    /** "jul" */
    fun shortMonth(date: LocalDate): String =
        date.toJavaLocalDate().month.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
            .removeSuffix(".")
            .lowercase(locale)

    /** "11 jul" */
    fun dayMonthLabel(date: LocalDate): String = "${date.dayOfMonth} ${shortMonth(date)}"

    /** "11 de julio" */
    fun dayOfMonthName(date: LocalDate): String = "${date.dayOfMonth} de ${monthName(date)}"

    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

    /** "8:00 p. m." */
    fun timeLabel(time: LocalTime): String = time.toJavaLocalTime().format(timeFormatter)
}
