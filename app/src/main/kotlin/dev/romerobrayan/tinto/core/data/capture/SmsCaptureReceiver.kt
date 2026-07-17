package dev.romerobrayan.tinto.core.data.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.romerobrayan.tinto.core.data.capture.parser.TintoIssuerRules
import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.RawCapture
import dev.romerobrayan.tinto.core.domain.repository.CaptureSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

/**
 * Live SMS capture (RECEIVE_SMS). Manifest-registered so bank messages are
 * caught even while the app isn't running, but everything is gated on the
 * explicit opt-in: without it the broadcast returns before a body is read.
 *
 * Dependencies come through an explicit Hilt entry point — system-instantiated
 * receivers can't be constructor-injected.
 */
class SmsCaptureReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CaptureEntryPoint {
        fun capturePipeline(): CapturePipeline
        fun captureSettings(): CaptureSettingsRepository
        fun applicationScope(): CoroutineScope
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CaptureEntryPoint::class.java,
        )
        if (!entryPoint.captureSettings().smsCaptureEnabled.value) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // A long SMS arrives as several PDUs of one logical message — regroup
        // per sender and concatenate the parts in order.
        val captures = messages.filterNotNull()
            .groupBy { it.originatingAddress.orEmpty() }
            .filterKeys(TintoIssuerRules::isAllowlisted)
            .map { (sender, parts) ->
                RawCapture(
                    sender = sender,
                    body = parts.joinToString(separator = "") { it.messageBody.orEmpty() },
                    receivedAt = Instant.fromEpochMilliseconds(parts.first().timestampMillis),
                    channel = CaptureChannel.SMS,
                )
            }
        if (captures.isEmpty()) return

        // Staging hits Room; goAsync keeps the receiver alive for the write.
        val pendingResult = goAsync()
        entryPoint.applicationScope().launch {
            try {
                captures.forEach { entryPoint.capturePipeline().submit(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
