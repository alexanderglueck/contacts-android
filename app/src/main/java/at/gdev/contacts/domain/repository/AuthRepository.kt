package at.gdev.contacts.domain.repository

import at.gdev.contacts.domain.model.AuthSession
import at.gdev.contacts.domain.model.LoginResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<AuthSession?>
    suspend fun login(email: String, password: String): Result<LoginResult>
    suspend fun submitTwoFactor(
        challengeToken: String,
        code: String?,
        recoveryCode: String?,
    ): Result<AuthSession>
    suspend fun register(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String,
        termsAccepted: Boolean,
    ): Result<AuthSession>
    suspend fun logout()
}
