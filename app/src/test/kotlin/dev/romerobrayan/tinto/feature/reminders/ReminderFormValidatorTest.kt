package dev.romerobrayan.tinto.feature.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderFormValidatorTest {

    @Test
    fun `non-blank title is valid`() {
        assertTrue(ReminderFormValidator.validate("Arriendo").isEmpty())
    }

    @Test
    fun `blank title is rejected`() {
        assertEquals(
            setOf(ReminderFormValidator.Error.TITLE_REQUIRED),
            ReminderFormValidator.validate(""),
        )
        assertEquals(
            setOf(ReminderFormValidator.Error.TITLE_REQUIRED),
            ReminderFormValidator.validate("   "),
        )
    }
}
