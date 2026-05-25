package at.gdev.contacts.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.gdev.contacts.data.network.ValidationException
import at.gdev.contacts.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val termsAccepted: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val success: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !submitting &&
                name.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                passwordConfirmation.isNotBlank() &&
                termsAccepted
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun setName(value: String) = clearErrors { it.copy(name = value) }
    fun setEmail(value: String) = clearErrors { it.copy(email = value) }
    fun setPassword(value: String) = clearErrors { it.copy(password = value) }
    fun setPasswordConfirmation(value: String) = clearErrors { it.copy(passwordConfirmation = value) }
    fun setTermsAccepted(value: Boolean) = clearErrors { it.copy(termsAccepted = value) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(submitting = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val result = authRepository.register(
                name = current.name.trim(),
                email = current.email.trim(),
                password = current.password,
                passwordConfirmation = current.passwordConfirmation,
                termsAccepted = current.termsAccepted,
            )
            _state.update { previous ->
                result.fold(
                    onSuccess = { previous.copy(submitting = false, success = true) },
                    onFailure = { err ->
                        val fieldErrors = (err as? ValidationException)?.errors
                            ?.mapValues { it.value.joinToString(" ") }
                            ?: emptyMap()
                        previous.copy(
                            submitting = false,
                            error = err.message ?: "Registration failed",
                            fieldErrors = fieldErrors,
                        )
                    },
                )
            }
        }
    }

    private inline fun clearErrors(transform: (RegisterUiState) -> RegisterUiState) {
        _state.update { transform(it).copy(error = null, fieldErrors = emptyMap()) }
    }
}
