package dev.romerobrayan.tinto.core.domain.model

/**
 * An amount of money in minor units (centavos). Money is NEVER represented as
 * Double/Float anywhere in the app — integer minor units by design.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun minus(other: Money): Money = Money(cents - other.cents)

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    fun abs(): Money = if (cents < 0) Money(-cents) else this

    val isZero: Boolean get() = cents == 0L

    companion object {
        val Zero: Money = Money(0)

        /** Convenience for whole-peso amounts (COP displays no decimals). */
        fun ofPesos(pesos: Long): Money = Money(pesos * 100)
    }
}
