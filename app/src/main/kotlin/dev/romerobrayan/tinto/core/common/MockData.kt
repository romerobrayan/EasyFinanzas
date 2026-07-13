package dev.romerobrayan.tinto.core.common

import dev.romerobrayan.tinto.core.domain.model.Card
import dev.romerobrayan.tinto.core.domain.model.Category
import dev.romerobrayan.tinto.core.domain.model.Money
import dev.romerobrayan.tinto.core.domain.model.PaymentMethod
import dev.romerobrayan.tinto.core.domain.model.Recurrence
import dev.romerobrayan.tinto.core.domain.model.Reminder
import dev.romerobrayan.tinto.core.domain.model.Transaction
import dev.romerobrayan.tinto.core.domain.model.TransactionSource
import dev.romerobrayan.tinto.core.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Sprint-1 in-memory dataset: realistic COP movements across May–July 2026
 * (plus December 2025 history for the year chart), the seeded category set,
 * and two registered cards. Everything flows through the same domain models
 * the Room-backed repositories will return in Sprint 2, so swapping
 * mock → Room is a Hilt binding change only.
 */
object MockData {

    val timeZone: TimeZone = TimeZone.of("America/Bogota")

    /** TODO(sprint-2+): replaced by real recurring-charge detection. */
    val recurringMerchants: Set<String> = setOf("Google One", "YouTube Premium")

    const val USER_NAME = "Brayan Romero"
    const val USER_EMAIL = "brayan@tinto.app"

    val categories: List<Category> = listOf(
        Category("cat-comida", "Comida", "tools-kitchen-2", "#E08AA3", isSystem = true),
        Category("cat-entretenimiento", "Entretenimiento", "device-tv", "#C9A961", isSystem = true),
        Category("cat-hormiga", "Gasto hormiga", "ant", "#D8567A", isSystem = true),
        Category("cat-salud", "Salud", "heartbeat", "#5FB894", isSystem = true),
        Category("cat-transporte", "Pasajes / Transporte", "bus", "#7F9BD1", isSystem = true),
        Category("cat-servicios", "Servicios / Suscripciones", "repeat", "#C79A6B", isSystem = true),
        Category("cat-mercado", "Mercado", "shopping-cart", "#9DC97E", isSystem = true),
        Category("cat-otros", "Otros", "dots", "#B99CA6", isSystem = true),
    )

    val cards: List<Card> = listOf(
        Card(id = "card-bancolombia", bank = "Bancolombia", last4 = "3092", label = "Débito"),
        Card(id = "card-nu", bank = "Nu", last4 = "2481", label = "Crédito"),
    )

    private var transactionSequence = 0

    private fun transaction(
        year: Int,
        month: Int,
        day: Int,
        pesos: Long,
        categoryId: String,
        merchant: String,
        cardId: String? = null,
        type: TransactionType = TransactionType.EXPENSE,
        hour: Int = 12,
        minute: Int = 0,
    ): Transaction {
        val occurredAt = LocalDateTime(year, month, day, hour, minute).toInstant(timeZone)
        val card = cards.firstOrNull { it.id == cardId }
        return Transaction(
            id = "tx-%03d".format(transactionSequence++),
            type = type,
            amount = Money.ofPesos(pesos),
            method = if (card != null) PaymentMethod.CARD else PaymentMethod.CASH,
            cardId = card?.id,
            bank = card?.bank,
            categoryId = categoryId,
            merchant = merchant,
            occurredAt = occurredAt,
            source = TransactionSource.MANUAL,
            createdAt = occurredAt,
            updatedAt = occurredAt,
        )
    }

    val transactions: List<Transaction> = listOf(
        // Julio 2026
        transaction(2026, 7, 11, 18_000, "cat-comida", "Almuerzo corrientazo", hour = 13, minute = 24),
        transaction(2026, 7, 11, 6_500, "cat-hormiga", "Café y pandebono", hour = 9, minute = 40),
        transaction(2026, 7, 10, 86_400, "cat-mercado", "Mercado D1", cardId = "card-bancolombia", hour = 19),
        transaction(2026, 7, 10, 14_200, "cat-transporte", "Uber", cardId = "card-nu", hour = 8),
        transaction(2026, 7, 9, 26_900, "cat-servicios", "YouTube Premium", cardId = "card-nu", hour = 6),
        transaction(2026, 7, 8, 32_800, "cat-salud", "Farmacia Cruz Verde", cardId = "card-bancolombia", hour = 16),
        transaction(2026, 7, 7, 42_000, "cat-entretenimiento", "Cine Colombia", cardId = "card-nu", hour = 20),
        transaction(2026, 7, 6, 23_000, "cat-transporte", "Recarga TransMilenio"),
        transaction(2026, 7, 5, 12_900, "cat-servicios", "Google One", cardId = "card-nu", hour = 7),
        transaction(2026, 7, 4, 38_500, "cat-comida", "Rappi — Sushi Green", cardId = "card-nu", hour = 21),
        transaction(2026, 7, 3, 5_800, "cat-hormiga", "Empanadas de la esquina", hour = 10),
        transaction(2026, 7, 2, 950_000, "cat-otros", "Arriendo apartamento", cardId = "card-bancolombia", hour = 18),
        transaction(2026, 7, 1, 2_850_000, "cat-otros", "Salario", cardId = "card-bancolombia", type = TransactionType.INCOME, hour = 9),
        // Junio 2026 — deliberately heavier than julio so the MoM chip reads a drop
        transaction(2026, 6, 28, 52_300, "cat-comida", "Crepes & Waffles", cardId = "card-nu", hour = 13),
        transaction(2026, 6, 25, 118_700, "cat-mercado", "Éxito mercado", cardId = "card-bancolombia", hour = 19),
        transaction(2026, 6, 21, 89_900, "cat-servicios", "Internet Claro", cardId = "card-bancolombia", hour = 16),
        transaction(2026, 6, 18, 45_000, "cat-salud", "Copago consulta EPS", hour = 11),
        transaction(2026, 6, 15, 68_000, "cat-entretenimiento", "Concierto La Macarena", cardId = "card-nu", hour = 20),
        transaction(2026, 6, 12, 20_000, "cat-transporte", "Recarga TransMilenio", hour = 8),
        transaction(2026, 6, 9, 26_900, "cat-servicios", "YouTube Premium", cardId = "card-nu", hour = 6),
        transaction(2026, 6, 7, 96_500, "cat-mercado", "Mercado D1", cardId = "card-bancolombia", hour = 14),
        transaction(2026, 6, 5, 12_900, "cat-servicios", "Google One", cardId = "card-nu", hour = 7),
        transaction(2026, 6, 4, 22_500, "cat-comida", "Almuerzo ejecutivo", hour = 12, minute = 45),
        transaction(2026, 6, 3, 7_200, "cat-hormiga", "Café Juan Valdez", cardId = "card-nu", hour = 15),
        transaction(2026, 6, 2, 950_000, "cat-otros", "Arriendo apartamento", cardId = "card-bancolombia", hour = 18),
        transaction(2026, 6, 1, 2_850_000, "cat-otros", "Salario", cardId = "card-bancolombia", type = TransactionType.INCOME, hour = 9),
        // Mayo 2026
        transaction(2026, 5, 30, 35_000, "cat-comida", "Domicilio Rappi", cardId = "card-nu", hour = 13),
        transaction(2026, 5, 24, 74_800, "cat-mercado", "Mercado Ara", hour = 10),
        transaction(2026, 5, 19, 28_000, "cat-entretenimiento", "Cine Colombia", cardId = "card-nu", hour = 18),
        transaction(2026, 5, 12, 15_000, "cat-transporte", "Taxi aeropuerto", hour = 9),
        transaction(2026, 5, 9, 26_900, "cat-servicios", "YouTube Premium", cardId = "card-nu", hour = 6),
        transaction(2026, 5, 5, 12_900, "cat-servicios", "Google One", cardId = "card-nu", hour = 7),
        transaction(2026, 5, 2, 950_000, "cat-otros", "Arriendo apartamento", cardId = "card-bancolombia", hour = 18),
        transaction(2026, 5, 1, 2_850_000, "cat-otros", "Salario", cardId = "card-bancolombia", type = TransactionType.INCOME, hour = 9),
        // Diciembre 2025 — historia para la vista de Año
        transaction(2025, 12, 24, 185_000, "cat-otros", "Regalos navideños", cardId = "card-nu", hour = 17),
        transaction(2025, 12, 20, 96_000, "cat-mercado", "Cena de Navidad", cardId = "card-bancolombia", hour = 20),
    )

    val reminders: List<Reminder> = listOf(
        Reminder(
            id = "rem-arriendo",
            title = "Arriendo apartamento",
            amount = Money.ofPesos(950_000),
            dueDate = LocalDate(2026, 8, 1),
            recurrence = Recurrence.MONTHLY,
            isPaid = false,
        ),
        Reminder(
            id = "rem-nu",
            title = "Pago tarjeta Nu",
            amount = Money.ofPesos(412_000),
            dueDate = LocalDate(2026, 7, 15),
            recurrence = Recurrence.MONTHLY,
            isPaid = false,
        ),
        Reminder(
            id = "rem-claro",
            title = "Internet Claro",
            amount = Money.ofPesos(89_900),
            dueDate = LocalDate(2026, 7, 21),
            recurrence = Recurrence.MONTHLY,
            isPaid = false,
        ),
        Reminder(
            id = "rem-tigo",
            title = "Plan celular Tigo",
            amount = Money.ofPesos(45_000),
            dueDate = LocalDate(2026, 7, 5),
            recurrence = Recurrence.MONTHLY,
            isPaid = true,
        ),
    )
}
