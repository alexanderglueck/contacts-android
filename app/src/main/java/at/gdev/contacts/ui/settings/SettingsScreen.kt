package at.gdev.contacts.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import at.gdev.contacts.domain.model.Team
import at.gdev.contacts.ui.calls.CallerIdSetupSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var sheetVisible by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshCallerIdRole()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AccountSection(state)
            HorizontalDivider()
            TeamsSection(
                state = state,
                onSelect = viewModel::switchTeam,
                onRetry = viewModel::refreshTeams,
            )
            HorizontalDivider()
            CallerIdSection(
                state = state,
                onOpenSetup = { sheetVisible = true },
                onSimulationNumberChange = viewModel::setSimulationNumber,
                onSimulate = viewModel::simulateIncomingCall,
            )
            HorizontalDivider()
            OutlinedButton(
                onClick = { viewModel.logout(onLoggedOut) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign out")
            }
        }
    }

    CallerIdSetupSheet(
        visible = sheetVisible,
        roleAvailable = state.callerIdRoleAvailable,
        roleHeld = state.callerIdRoleHeld,
        onDismiss = { sheetVisible = false },
        onRefresh = viewModel::refreshCallerIdRole,
        createRoleRequestIntent = viewModel::createCallerIdRoleRequestIntent,
    )
}

@Composable
private fun AccountSection(state: SettingsUiState) {
    Column {
        SectionHeader("Account")
        Spacer(Modifier.height(8.dp))
        val user = state.user
        if (user == null) {
            Text("Not signed in", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(user.email, style = MaterialTheme.typography.bodyMedium)
            user.currentTeam?.let {
                Spacer(Modifier.height(2.dp))
                Text("Current team: ${it.name}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TeamsSection(
    state: SettingsUiState,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Column {
        SectionHeader("Team")
        Spacer(Modifier.height(8.dp))

        when {
            state.loadingTeams && state.teams.isEmpty() -> Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }

            state.error != null && state.teams.isEmpty() -> Column {
                Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRetry) { Text("Retry") }
            }

            state.teams.isEmpty() -> Text(
                "You are not a member of any team.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )

            else -> Column {
                state.teams.forEach { team ->
                    TeamRow(
                        team = team,
                        switching = state.switchingTo == team.uuid,
                        onSelect = { onSelect(team.uuid) },
                    )
                }
                if (state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TeamRow(team: Team, switching: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !team.isCurrent && !switching, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = team.isCurrent,
            onClick = { if (!team.isCurrent) onSelect() },
            enabled = !switching,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(team.name, style = MaterialTheme.typography.bodyLarge)
            if (team.isOwner) {
                Text("Owner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        if (switching) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun CallerIdSection(
    state: SettingsUiState,
    onOpenSetup: () -> Unit,
    onSimulationNumberChange: (String) -> Unit,
    onSimulate: () -> Unit,
) {
    Column {
        SectionHeader("Caller ID")
        Spacer(Modifier.height(8.dp))

        Text(
            "Synced contacts are exposed as a system directory, so your dialer shows " +
                    "their name on incoming calls — no permissions needed. Some dialers " +
                    "(non-AOSP / non-Google) ignore directory providers; for those, the " +
                    "optional screening fallback below posts a heads-up notification instead.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "Notification fallback",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        val status = when {
            !state.callerIdRoleAvailable -> "Not supported on this device."
            state.callerIdRoleHeld -> "Active — heads-up notifications will appear on incoming calls."
            else -> "Off. Enable if your dialer doesn't pick up the directory natively."
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

        if (state.callerIdRoleAvailable) {
            Spacer(Modifier.height(8.dp))
            if (state.callerIdRoleHeld) {
                OutlinedButton(onClick = onOpenSetup, modifier = Modifier.fillMaxWidth()) {
                    Text("Manage notification fallback")
                }
            } else {
                OutlinedButton(onClick = onOpenSetup, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable notification fallback")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Simulate incoming call",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Runs the same lookup the screening service uses and posts the notification " +
                    "directly. Doesn't require the screening role to be granted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.simulationNumber,
            onValueChange = onSimulationNumberChange,
            label = { Text("Phone number") },
            placeholder = { Text("+43 660 1234567") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSimulate,
            enabled = state.simulationNumber.isNotBlank() && !state.simulating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.simulating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Simulate")
            }
        }
        state.simulationResult?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}
