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
import androidx.compose.runtime.mutableStateOf
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

    // Set when launched from a birthday notification; consumed by the nav host.
    private var openCalendar by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition {
            sessionViewModel.state.value is SessionState.Loading
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openCalendar = intent?.getBooleanExtra(EXTRA_OPEN_CALENDAR, false) == true
        setContent {
            ContactsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ContactsNavHost(
                        sessionViewModel = sessionViewModel,
                        openCalendar = openCalendar,
                        onCalendarOpened = { openCalendar = false },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_CALENDAR, false)) {
            openCalendar = true
        }
    }

    companion object {
        const val EXTRA_OPEN_CALENDAR = "at.gdev.contacts.OPEN_CALENDAR"
    }
}
