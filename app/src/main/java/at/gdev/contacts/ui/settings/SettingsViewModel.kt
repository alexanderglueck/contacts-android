package at.gdev.contacts.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.gdev.contacts.calls.CallerIdNotifier
import at.gdev.contacts.calls.CallerIdRoleHelper
import at.gdev.contacts.domain.model.AuthUser
import at.gdev.contacts.domain.model.Team
import at.gdev.contacts.domain.repository.AuthRepository
import at.gdev.contacts.domain.repository.ContactsRepository
import at.gdev.contacts.domain.repository.TeamsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: AuthUser? = null,
    val teams: List<Team> = emptyList(),
    val loadingTeams: Boolean = false,
    val switchingTo: String? = null,
    val error: String? = null,
    val callerIdRoleAvailable: Boolean = true,
    val callerIdRoleHeld: Boolean = false,
    val simulationNumber: String = "",
    val simulating: Boolean = false,
    val simulationResult: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val teamsRepository: TeamsRepository,
    private val contactsRepository: ContactsRepository,
    private val callerIdRoleHelper: CallerIdRoleHelper,
    private val callerIdNotifier: CallerIdNotifier,
) : ViewModel() {

    private val teams = MutableStateFlow<List<Team>>(emptyList())
    private val loadingTeams = MutableStateFlow(false)
    private val switchingTo = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val callerIdRoleHeld = MutableStateFlow(callerIdRoleHelper.isRoleHeld())
    private val simulationNumber = MutableStateFlow("")
    private val simulating = MutableStateFlow(false)
    private val simulationResult = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        authRepository.session.map { it?.user },
        teams,
        loadingTeams,
        switchingTo,
        error,
        callerIdRoleHeld,
        simulationNumber,
        simulating,
        simulationResult,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            user = values[0] as AuthUser?,
            teams = values[1] as List<Team>,
            loadingTeams = values[2] as Boolean,
            switchingTo = values[3] as String?,
            error = values[4] as String?,
            callerIdRoleAvailable = callerIdRoleHelper.isRoleAvailable(),
            callerIdRoleHeld = values[5] as Boolean,
            simulationNumber = values[6] as String,
            simulating = values[7] as Boolean,
            simulationResult = values[8] as String?,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    init {
        refreshTeams()
    }

    fun refreshCallerIdRole() {
        callerIdRoleHeld.value = callerIdRoleHelper.isRoleHeld()
    }

    fun createCallerIdRoleRequestIntent() = callerIdRoleHelper.createRequestIntent()

    fun setSimulationNumber(value: String) {
        simulationNumber.value = value
        simulationResult.value = null
    }

    fun simulateIncomingCall() {
        val number = simulationNumber.value.trim()
        if (number.isBlank() || simulating.value) return
        viewModelScope.launch {
            simulating.value = true
            simulationResult.value = null
            val matches = runCatching { contactsRepository.lookupByNumber(number) }
                .getOrDefault(emptyList())
            val first = matches.firstOrNull()
            if (first == null) {
                simulationResult.value = "No match for $number"
            } else {
                callerIdNotifier.showMatch(number, first)
                simulationResult.value = "Matched ${first.fullName} — notification posted"
            }
            simulating.value = false
        }
    }

    fun refreshTeams() {
        viewModelScope.launch {
            loadingTeams.value = true
            error.value = null
            teamsRepository.list()
                .onSuccess { teams.value = it }
                .onFailure { error.value = it.message ?: "Failed to load teams" }
            loadingTeams.value = false
        }
    }

    fun switchTeam(uuid: String) {
        val current = teams.value.firstOrNull { it.uuid == uuid } ?: return
        if (current.isCurrent || switchingTo.value != null) return
        viewModelScope.launch {
            switchingTo.value = uuid
            error.value = null
            teamsRepository.switchTo(uuid)
                .onSuccess {
                    // Reflect new current_team locally; refresh contacts for the new scope.
                    teams.value = teams.value.map { it.copy(isCurrent = it.uuid == uuid) }
                    contactsRepository.refresh(null)
                }
                .onFailure { error.value = it.message ?: "Failed to switch team" }
            switchingTo.value = null
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
