package dev.romerobrayan.tinto.core.data.capture

import android.content.Context
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romerobrayan.tinto.core.common.TintoDispatchers
import dev.romerobrayan.tinto.core.data.capture.parser.IssuerRules
import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.RawCapture
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * One-time bounded READ_SMS backfill: existing inbox messages from the
 * allow-listed senders inside the last [BACKFILL_WINDOW_DAYS] days, so the
 * inbox isn't flooded on day one. Staging dedups on the message itself, so
 * a re-run (or overlap with live receive) never doubles an item.
 */
@Singleton
class SmsBackfill @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processor: CaptureProcessor,
    private val settings: CaptureSettings,
    private val dispatchers: TintoDispatchers,
) {

    suspend fun runOnce() {
        if (settings.backfillDone) return
        withContext(dispatchers.io) {
            val cutoffMs = (Clock.System.now() - BACKFILL_WINDOW_DAYS.days).toEpochMilliseconds()
            val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
            )
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(cutoffMs.toString()),
                "${Telephony.Sms.DATE} ASC",
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressIndex).orEmpty().filter(Char::isDigit)
                    if (sender !in IssuerRules.allSenders) continue
                    val body = cursor.getString(bodyIndex).orEmpty()
                    if (body.isBlank()) continue
                    processor.process(
                        RawCapture(
                            sender = sender,
                            body = body,
                            receivedAt = Instant.fromEpochMilliseconds(cursor.getLong(dateIndex)),
                            channel = CaptureChannel.SMS,
                        ),
                    )
                }
            }
            settings.backfillDone = true
        }
    }

    private companion object {
        const val BACKFILL_WINDOW_DAYS = 90
    }
}
