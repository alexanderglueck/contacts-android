package at.gdev.contacts.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.gdev.contacts.domain.model.AuthSession
import at.gdev.contacts.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SessionState {
    /** Waiting for the persisted session to resolve from DataStore. */
    data object Loading : SessionState
    data class LoggedIn(val session: AuthSession) : SessionState
    data object LoggedOut : SessionState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<SessionState> = authRepository.session
        .map<AuthSession?, SessionState> { session ->
            if (session != null) SessionState.LoggedIn(session) else SessionState.LoggedOut
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SessionState.Loading,
        )
}
