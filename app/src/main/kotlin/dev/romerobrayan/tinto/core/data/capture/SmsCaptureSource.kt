package dev.romerobrayan.tinto.core.data.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romerobrayan.tinto.core.common.TintoDispatchers
import dev.romerobrayan.tinto.core.data.capture.parser.TintoIssuerRules
import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.RawCapture
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * The READ_SMS side of SMS capture: a bounded backfill (last
 * [BACKFILL_WINDOW_DAYS] days) over `content://sms/inbox`, filtered to the
 * allow-listed bank senders before a body is ever read into the pipeline.
 * Runs only after the explicit opt-in — [CaptureSettingsRepository] triggers
 * it — and silently no-ops without the permission. Live messages arrive via
 * [SmsCaptureReceiver]; both feed the same [CapturePipeline].
 */
@Singleton
class SmsCaptureSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pipeline: CapturePipeline,
    private val dispatchers: TintoDispatchers,
) : CaptureSource {

    override val channel: CaptureChannel = CaptureChannel.SMS

    override suspend fun backfill() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return

        withContext(dispatchers.io) {
            val since = Clock.System.now().minus(BACKFILL_WINDOW_DAYS.days).toEpochMilliseconds()
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(since.toString()),
                "${Telephony.Sms.DATE} ASC",
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressIndex) ?: continue
                    if (!TintoIssuerRules.isAllowlisted(sender)) continue
                    val body = cursor.getString(bodyIndex) ?: continue
                    pipeline.submit(
                        RawCapture(
                            sender = sender,
                            body = body,
                            receivedAt = Instant.fromEpochMilliseconds(cursor.getLong(dateIndex)),
                            channel = CaptureChannel.SMS,
                        ),
                    )
                }
            }
        }
    }

    private companion object {
        /** Bounded so the inbox isn't flooded on day one (open question #2: 90 days). */
        const val BACKFILL_WINDOW_DAYS = 90
    }
}
