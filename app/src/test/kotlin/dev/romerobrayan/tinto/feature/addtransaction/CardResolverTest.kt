package dev.romerobrayan.tinto.feature.addtransaction

import dev.romerobrayan.tinto.core.domain.model.Card
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardResolverTest {

    private val bancolombia = Card(id = "card-banco", bank = "Bancolombia", last4 = "3092", label = "Débito")
    private val nu = Card(id = "card-nu", bank = "Nu", last4 = "2481", label = "Crédito")

    /** Same four digits at two banks — the regression this resolver exists for. */
    private val globalTwin = Card(id = "card-global", bank = "Global66", last4 = "3092", label = null)

    private val cards = listOf(bancolombia, nu, globalTwin)

    @Test
    fun `picked card wins over a sibling sharing the same last4`() {
        assertEquals(
            globalTwin,
            CardResolver.resolve(cards, cardId = "card-global", last4 = "3092"),
        )
        assertEquals(
            bancolombia,
            CardResolver.resolve(cards, cardId = "card-banco", last4 = "3092"),
        )
    }

    @Test
    fun `falls back to digits when no card was picked`() {
        // A hand-typed last4 has no id; first match by digits is the best guess.
        assertEquals(bancolombia, CardResolver.resolve(cards, cardId = null, last4 = "3092"))
        assertEquals(nu, CardResolver.resolve(cards, cardId = null, last4 = "2481"))
    }

    @Test
    fun `falls back to digits when the picked card no longer exists`() {
        assertEquals(
            nu,
            CardResolver.resolve(cards, cardId = "card-deleted", last4 = "2481"),
        )
    }

    @Test
    fun `returns null when nothing matches`() {
        assertNull(CardResolver.resolve(cards, cardId = null, last4 = "9999"))
        assertNull(CardResolver.resolve(cards, cardId = "card-deleted", last4 = null))
        assertNull(CardResolver.resolve(cards, cardId = null, last4 = ""))
        assertNull(CardResolver.resolve(emptyList(), cardId = "card-banco", last4 = "3092"))
    }
}
