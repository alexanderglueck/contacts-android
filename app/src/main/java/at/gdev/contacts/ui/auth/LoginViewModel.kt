package at.gdev.contacts.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.gdev.contacts.data.network.ValidationException
import at.gdev.contacts.domain.model.LoginResult
import at.gdev.contacts.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoginStep { Credentials, TwoFactor }

data class LoginUiState(
    val step: LoginStep = LoginStep.Credentials,
    val email: String = "",
    val password: String = "",
    val twoFactorCode: String = "",
    val useRecoveryCode: Boolean = false,
    val challengeToken: String? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun setEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun setPassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun setTwoFactorCode(value: String) = _state.update { it.copy(twoFactorCode = value, error = null) }

    fun toggleRecoveryCode() = _state.update {
        it.copy(useRecoveryCode = !it.useRecoveryCode, twoFactorCode = "", error = null)
    }

    fun cancelTwoFactor() = _state.update {
        it.copy(
            step = LoginStep.Credentials,
            twoFactorCode = "",
            useRecoveryCode = false,
            challengeToken = null,
            error = null,
        )
    }

    fun submit() {
        val current = _state.value
        if (current.submitting) return
        when (current.step) {
            LoginStep.Credentials -> submitCredentials(current)
            LoginStep.TwoFactor -> submitTwoFactor(current)
        }
    }

    private fun submitCredentials(current: LoginUiState) {
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.login(current.email.trim(), current.password)
            _state.update { previous ->
                result.fold(
                    onSuccess = { outcome ->
                        when (outcome) {
                            is LoginResult.Authenticated -> previous.copy(submitting = false, success = true)
                            is LoginResult.TwoFactorRequired -> previous.copy(
                                submitting = false,
                                step = LoginStep.TwoFactor,
                                challengeToken = outcome.challengeToken,
                                twoFactorCode = "",
                                useRecoveryCode = false,
                            )
                        }
                    },
                    onFailure = { err -> previous.copy(submitting = false, error = err.toUserMessage("Login failed")) },
                )
            }
        }
    }

    private fun submitTwoFactor(current: LoginUiState) {
        val challenge = current.challengeToken ?: return
        val code = current.twoFactorCode.trim()
        if (code.isBlank()) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.submitTwoFactor(
                challengeToken = challenge,
                code = code.takeUnless { current.useRecoveryCode },
                recoveryCode = code.takeIf { current.useRecoveryCode },
            )
            _state.update { previous ->
                result.fold(
                    onSuccess = { previous.copy(submitting = false, success = true) },
                    onFailure = { err -> previous.copy(submitting = false, error = err.toUserMessage("Verification failed")) },
                )
            }
        }
    }

    private fun Throwable.toUserMessage(fallback: String): String =
        (this as? ValidationException)?.firstError() ?: message ?: fallback
}
