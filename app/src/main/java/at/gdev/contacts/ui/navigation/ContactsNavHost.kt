package at.gdev.contacts.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import at.gdev.contacts.ui.auth.LoginScreen
import at.gdev.contacts.ui.auth.RegisterScreen
import at.gdev.contacts.ui.auth.SessionState
import at.gdev.contacts.ui.auth.SessionViewModel
import at.gdev.contacts.ui.calendar.CalendarScreen
import at.gdev.contacts.ui.contacts.ContactDetailScreen
import at.gdev.contacts.ui.contacts.ContactsListScreen
import at.gdev.contacts.ui.contacts.EditContactScreen
import at.gdev.contacts.ui.settings.SettingsScreen

@Composable
fun ContactsNavHost(
    sessionViewModel: SessionViewModel = hiltViewModel(),
    openCalendar: Boolean = false,
    onCalendarOpened: () -> Unit = {},
) {
    val sessionState by sessionViewModel.state.collectAsState()

    // Lock in a start destination only after the persisted session resolves.
    // The system splash (set up in MainActivity) stays on top until then,
    // so the user never sees the login screen flash for one frame.
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(sessionState) {
        if (startDestination == null) {
            startDestination = when (sessionState) {
                SessionState.Loading -> null
                is SessionState.LoggedIn -> Routes.CONTACTS_LIST
                SessionState.LoggedOut -> Routes.LOGIN
            }
        }
    }
    val resolved = startDestination ?: return

    val navController = rememberNavController()

    // Deep link from a birthday notification: jump to the calendar (which defaults
    // to today) once the session has resolved to a logged-in user.
    LaunchedEffect(openCalendar, sessionState) {
        if (openCalendar && sessionState is SessionState.LoggedIn) {
            navController.navigate(Routes.CALENDAR) {
                launchSingleTop = true
            }
            onCalendarOpened()
        }
    }

    NavHost(navController = navController, startDestination = resolved) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.CONTACTS_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onCreateAccount = { navController.navigate(Routes.REGISTER) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Routes.CONTACTS_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() },
            )
        }
        composable(Routes.CONTACTS_LIST) {
            ContactsListScreen(
                onContactClick = { contactId ->
                    navController.navigate(Routes.contactDetail(contactId))
                },
                onAddContact = { navController.navigate(Routes.CONTACT_NEW) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
            )
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(
                onBack = { navController.popBackStack() },
                onContactClick = { id -> navController.navigate(Routes.contactDetail(id)) },
            )
        }
        composable(
            route = Routes.CONTACT_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_CONTACT_ID) { type = NavType.StringType }),
        ) {
            ContactDetailScreen(
                onBack = { navController.popBackStack() },
                onEditBase = { id -> navController.navigate(Routes.contactEdit(id)) },
            )
        }
        composable(Routes.CONTACT_NEW) {
            EditContactScreen(
                onBack = { navController.popBackStack() },
                onSaved = { newId ->
                    navController.navigate(Routes.contactDetail(newId)) {
                        popUpTo(Routes.CONTACTS_LIST)
                    }
                },
            )
        }
        composable(
            route = Routes.CONTACT_EDIT,
            arguments = listOf(navArgument(Routes.ARG_CONTACT_ID) { type = NavType.StringType }),
        ) {
            EditContactScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
