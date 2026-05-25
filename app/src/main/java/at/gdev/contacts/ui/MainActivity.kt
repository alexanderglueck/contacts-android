package at.gdev.contacts.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition {
            sessionViewModel.state.value is SessionState.Loading
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ContactsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ContactsNavHost(sessionViewModel = sessionViewModel)
                }
            }
        }
    }
}
