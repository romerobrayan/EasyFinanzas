package dev.romerobrayan.tinto.core.data.export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romerobrayan.tinto.core.common.TintoDispatchers
import dev.romerobrayan.tinto.core.domain.repository.CardRepository
import dev.romerobrayan.tinto.core.domain.repository.CategoryRepository
import dev.romerobrayan.tinto.core.domain.repository.ReminderRepository
import dev.romerobrayan.tinto.core.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * Writes a one-shot snapshot of the user's data (demo or signed-in — the
 * repositories already route by session) as versioned JSON to a
 * user-picked [Uri] from the Storage Access Framework.
 */
@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository,
    private val dispatchers: TintoDispatchers,
) {

    // encodeDefaults = true: schema_version and currency must always be present
    // in the wire format, even while their values equal the Kotlin defaults.
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val document = ExportDocument(
                exportedAt = Clock.System.now().toString(),
                appVersion = appVersionName(),
                cards = cardRepository.observeCards().first().map { it.toExportDto() },
                categories = categoryRepository.observeCategories().first().map { it.toExportDto() },
                transactions = transactionRepository.observeTransactions().first().map { it.toExportDto() },
                reminders = reminderRepository.observeReminders().first().map { it.toExportDto() },
            )
            val bytes = json.encodeToString(document).toByteArray()
            val stream = context.contentResolver.openOutputStream(uri)
                ?: error("openOutputStream returned null for $uri")
            stream.use { it.write(bytes) }
        }
    }

    private fun appVersionName(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
}
