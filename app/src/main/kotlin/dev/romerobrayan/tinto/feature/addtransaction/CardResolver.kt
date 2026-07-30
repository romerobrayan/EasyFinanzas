package dev.romerobrayan.tinto.feature.addtransaction

import dev.romerobrayan.tinto.core.domain.model.Card

/**
 * Pure card lookup for the manual form.
 *
 * An explicitly picked [cardId] always wins over [last4]: two registered cards
 * can end in the same four digits, and matching on digits alone would attach
 * whichever one happens to come first in the list. The digits stay as a
 * fallback for a hand-typed last4 — and for a pick whose card was since
 * deleted.
 */
object CardResolver {

    fun resolve(cards: List<Card>, cardId: String?, last4: String?): Card? {
        cardId?.let { id -> cards.firstOrNull { it.id == id } }?.let { return it }
        if (last4.isNullOrBlank()) return null
        return cards.firstOrNull { it.last4 == last4 }
    }
}
