package dev.romerobrayan.tinto.core.data.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import dev.romerobrayan.tinto.core.domain.model.CaptureChannel
import dev.romerobrayan.tinto.core.domain.model.RawCapture
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Live SMS capture: SMS_RECEIVED → RawCapture → CaptureProcessor. Gated on
 * the explicit opt-in; with capture disabled the receiver returns without
 * touching the message. Unknown senders die in the parser (allowlist).
 */
@AndroidEntryPoint
class SmsCaptureReceiver : BroadcastReceiver() {

    @Inject lateinit var processor: CaptureProcessor

    @Inject lateinit var settings: CaptureSettings

    @Inject lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!settings.smsCaptureEnabled.value) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // A long SMS arrives as several PDUs of one logical message — reassemble per sender.
        val bodiesBySender = messages
            .filterNotNull()
            .groupBy { it.displayOriginatingAddress.orEmpty() }
            .mapValues { (_, parts) -> parts.joinToString("") { it.messageBody.orEmpty() } }

        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                bodiesBySender.forEach { (sender, body) ->
                    if (body.isBlank()) return@forEach
                    processor.process(
                        RawCapture(
                            sender = sender,
                            body = body,
                            receivedAt = Clock.System.now(),
                            channel = CaptureChannel.SMS,
                        ),
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
