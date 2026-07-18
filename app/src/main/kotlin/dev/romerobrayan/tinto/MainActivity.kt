package dev.romerobrayan.tinto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.romerobrayan.tinto.core.data.notifications.ReminderNotifications
import dev.romerobrayan.tinto.core.designsystem.theme.TintoTheme
import dev.romerobrayan.tinto.navigation.TintoRoot

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A reminder-notification tap asks for the Recordatorios tab.
        val openRemindersTab =
            intent?.getStringExtra(ReminderNotifications.EXTRA_OPEN_TAB) ==
                ReminderNotifications.TAB_REMINDERS
        setContent {
            TintoTheme {
                TintoRoot(openRemindersOnLaunch = openRemindersTab)
            }
        }
    }
}
