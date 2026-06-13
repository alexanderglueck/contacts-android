package at.gdev.contacts.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import at.gdev.contacts.ui.auth.SessionState
import at.gdev.contacts.ui.auth.SessionViewModel
import at.gdev.contacts.ui.navigation.ContactsNavHost
import at.gdev.contacts.ui.theme.ContactsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    // Bumped each time we're launched/re-launched from a birthday notification.
    // A monotonic counter (rather than a boolean) means every tap is a distinct
    // event, so a second notification still navigates even while a prior one is
    // mid-handling. Consumed by the nav host.
    private var calendarRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition {
            sessionViewModel.state.value is SessionState.Loading
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only act on a genuine fresh launch. A config-change/process-death recreation
        // re-delivers the same launch intent, and re-handling it would yank the user
        // back to the calendar; a real notification tap always has a null bundle.
        if (savedInstanceState == null && intent?.opensCalendar() == true) {
            calendarRequest++
        }
        setContent {
            ContactsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ContactsNavHost(
                        sessionViewModel = sessionViewModel,
                        calendarRequest = calendarRequest,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.opensCalendar()) {
            calendarRequest++
        }
    }

    companion object {
        /**
         * Set by the PendingIntent we build in [ContactsMessagingService] when our own
         * code posts the notification — i.e. the foreground / data-only push path.
         */
        const val EXTRA_OPEN_CALENDAR = "at.gdev.contacts.OPEN_CALENDAR"

        /**
         * Key of the FCM `data` field carrying the push type. For a notification+data
         * message tapped from the system tray (the backgrounded/killed case, where
         * onMessageReceived never runs), FCM copies the `data` payload onto the launcher
         * intent's extras — so this is how we recover the routing on a cold start.
         */
        private const val EXTRA_FCM_TYPE = "type"

        /** Push types that are birthday-reminder digests and should open the calendar. */
        private val CALENDAR_PUSH_TYPES = setOf("daily", "weekly")

        /**
         * Whether this launch intent should deep-link to the calendar — either from our
         * own PendingIntent or from a system-tray tap of a backend birthday reminder.
         */
        private fun Intent.opensCalendar(): Boolean =
            getBooleanExtra(EXTRA_OPEN_CALENDAR, false) ||
                getStringExtra(EXTRA_FCM_TYPE) in CALENDAR_PUSH_TYPES
    }
}
