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
        if (intent?.getBooleanExtra(EXTRA_OPEN_CALENDAR, false) == true) {
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
        if (intent.getBooleanExtra(EXTRA_OPEN_CALENDAR, false)) {
            calendarRequest++
        }
    }

    companion object {
        const val EXTRA_OPEN_CALENDAR = "at.gdev.contacts.OPEN_CALENDAR"
    }
}
