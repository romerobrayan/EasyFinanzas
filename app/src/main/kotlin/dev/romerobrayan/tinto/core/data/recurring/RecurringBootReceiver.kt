package dev.romerobrayan.tinto.core.data.recurring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Materializes anything the automation rules owe after the device rebooted or
 * the clock/timezone changed. Receiving any of these starts the process, which
 * runs Application.onCreate → the coordinator's repository collection;
 * reconcileNow covers the cached list immediately (mirrors the reminder boot
 * receiver).
 *
 * TODO(sprint-5): an optional daily setAndAllowWhileIdle alarm would keep
 * generation fresh without the user opening the app; boot/clock coverage is
 * enough for now.
 */
@AndroidEntryPoint
class RecurringBootReceiver : BroadcastReceiver() {

    @Inject lateinit var coordinator: RecurringTransactionCoordinator

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> coordinator.reconcileNow()
        }
    }
}
