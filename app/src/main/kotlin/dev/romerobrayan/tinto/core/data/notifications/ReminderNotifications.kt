package dev.romerobrayan.tinto.core.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.romerobrayan.tinto.R
import dev.romerobrayan.tinto.core.common.MoneyFormat
import dev.romerobrayan.tinto.core.domain.model.Money

/**
 * The one place reminder notifications are built: channel creation at app
 * start, and the notification itself (title + formatted amount, tap opens
 * the Recordatorios tab). On-device only — amounts may appear here, but
 * never in analytics.
 */
object ReminderNotifications {

    const val CHANNEL_ID = "reminders"

    /** Intent extra MainActivity reads to land on a specific tab. */
    const val EXTRA_OPEN_TAB = "dev.romerobrayan.tinto.extra.OPEN_TAB"
    const val TAB_REMINDERS = "reminders"

    /** Safe to call repeatedly — creating an existing channel is a no-op. */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_reminders),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_reminders_desc)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, reminderId: String, title: String, amount: Money?) {
        // Declining POST_NOTIFICATIONS degrades gracefully: no alert, no crash.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val tapIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_OPEN_TAB, TAB_REMINDERS)
            }
        val contentIntent = tapIntent?.let {
            PendingIntent.getActivity(
                context,
                reminderId.hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val text = amount
            ?.let { context.getString(R.string.reminder_notif_text_amount, MoneyFormat.format(it)) }
            ?: context.getString(R.string.reminder_notif_text)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            // VtPrimary — the wine accent from DESIGN_SYSTEM.md.
            .setColor(NOTIFICATION_ACCENT)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
    }

    private val NOTIFICATION_ACCENT = 0xFFB23A5E.toInt()
}
