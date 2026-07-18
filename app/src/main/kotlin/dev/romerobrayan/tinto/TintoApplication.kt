package dev.romerobrayan.tinto

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.romerobrayan.tinto.core.data.notifications.ReminderNotificationCoordinator
import javax.inject.Inject

@HiltAndroidApp
class TintoApplication : Application() {

    @Inject lateinit var reminderNotificationCoordinator: ReminderNotificationCoordinator

    override fun onCreate() {
        super.onCreate()
        // Creates the notification channel and starts observing reminders so
        // alarms stay reconciled in demo and signed-in mode alike.
        reminderNotificationCoordinator.start()
    }
}
