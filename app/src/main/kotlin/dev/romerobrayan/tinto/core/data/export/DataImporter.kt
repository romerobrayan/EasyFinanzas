package dev.romerobrayan.tinto.core.data.export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romerobrayan.tinto.core.common.TintoDispatchers
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class ImportSummary(
    val cardsAdded: Int,
    val transactionsAdded: Int,
    val remindersAdded: Int,
) {
    val totalAdded: Int get() = cardsAdded + transactionsAdded + remindersAdded
}

/**
 * Restores a previously exported JSON snapshot. Only rows whose id isn't
 * already present get added — re-importing the same file, or an older
 * backup over newer data, never duplicates or overwrites anything.
 * Categories aren't imported: they're a fixed system set with no
 * `CategoryRepository` write methods.
 */
@Singleton
class DataImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository,
    private val reminderRepository: ReminderRepository,
    private val dispatchers: TintoDispatchers,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importFrom(uri: Uri): Result<ImportSummary> = withContext(dispatchers.io) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: error("openInputStream returned null for $uri")
            val document = json.decodeFromString<ExportDocument>(text)
            check(document.schemaVersion == 1) { "Unsupported schema_version ${document.schemaVersion}" }

            val existingCardIds = cardRepository.observeCards().first().map { it.id }.toSet()
            val existingTransactionIds = transactionRepository.observeTransactions().first().map { it.id }.toSet()
            val existingReminderIds = reminderRepository.observeReminders().first().map { it.id }.toSet()

            var cardsAdded = 0
            document.cards.filter { it.id !in existingCardIds }.forEach { dto ->
                cardRepository.addCard(dto.toDomain())
                cardsAdded++
            }

            var transactionsAdded = 0
            document.transactions.filter { it.id !in existingTransactionIds }
                .mapNotNull { it.toDomain() }
                .forEach { transactionRepository.addTransaction(it); transactionsAdded++ }

            var remindersAdded = 0
            document.reminders.filter { it.id !in existingReminderIds }
                .mapNotNull { it.toDomain() }
                .forEach { reminderRepository.addReminder(it); remindersAdded++ }

            ImportSummary(cardsAdded, transactionsAdded, remindersAdded)
        }
    }
}
