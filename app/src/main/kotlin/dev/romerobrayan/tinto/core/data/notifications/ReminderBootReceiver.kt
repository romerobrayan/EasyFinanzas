package dev.romerobrayan.tinto.core.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Alarms die on reboot; this re-registers them. Also reacts to timezone and
 * clock changes so triggers stay anchored to local wall time. Receiving any
 * of these starts the process, which runs Application.onCreate → the
 * coordinator's repository collection; reconcileNow covers the cached list
 * immediately.
 */
@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject lateinit var coordinator: ReminderNotificationCoordinator

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> coordinator.reconcileNow()
        }
    }
}
