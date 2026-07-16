package dev.romerobrayan.tinto.feature.reminders

/**
 * Pure client-side validation for the reminder form. Only the title is
 * mandatory — amount is optional and the date always has a value.
 */
object ReminderFormValidator {

    enum class Error { TITLE_REQUIRED }

    fun validate(title: String): Set<Error> =
        if (title.isBlank()) setOf(Error.TITLE_REQUIRED) else emptySet()
}
