package at.gdev.contacts.data.repository

import android.os.Build
import at.gdev.contacts.data.auth.TokenStore
import at.gdev.contacts.data.local.CallEventStore
import at.gdev.contacts.data.local.ContactsDao
import at.gdev.contacts.data.network.AuthApi
import at.gdev.contacts.data.network.dto.AuthResponse
import at.gdev.contacts.data.network.dto.LoginRequest
import at.gdev.contacts.data.network.dto.RegisterRequest
import at.gdev.contacts.data.network.dto.TwoFactorChallengeRequest
import at.gdev.contacts.data.network.dto.UserDto
import at.gdev.contacts.data.network.toDomainError
import at.gdev.contacts.data.sync.ContactSyncScheduler
import at.gdev.contacts.domain.model.AuthSession
import at.gdev.contacts.domain.model.AuthUser
import at.gdev.contacts.domain.model.LoginResult
import at.gdev.contacts.domain.model.TeamSummary
import at.gdev.contacts.domain.repository.AuthRepository
import at.gdev.contacts.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val json: Json,
    private val syncScheduler: ContactSyncScheduler,
    private val contactsDao: ContactsDao,
    private val callEventStore: CallEventStore,
    private val deviceRepository: DeviceRepository,
) : AuthRepository {

    override val session: Flow<AuthSession?> = tokenStore.session

    override suspend fun login(email: String, password: String): Result<LoginResult> = runCatching {
        val response = api.login(
            LoginRequest(email = email, password = password, deviceName = deviceName())
        )
        if (response.twoFactorRequired == true && response.challengeToken != null) {
            LoginResult.TwoFactorRequired(
                challengeToken = response.challengeToken,
                expiresInSeconds = response.expiresIn ?: 300,
            )
        } else {
            val user = response.user ?: error("Login response missing user")
            val token = response.token ?: error("Login response missing token")
            LoginResult.Authenticated(AuthSession(accessToken = token, user = user.toDomain()))
        }
    }.onSuccess { result ->
        if (result is LoginResult.Authenticated) {
            tokenStore.save(result.session)
            deviceRepository.registerCurrentDevice()
            syncScheduler.syncNow()
        }
    }.mapError()

    override suspend fun submitTwoFactor(
        challengeToken: String,
        code: String?,
        recoveryCode: String?,
    ): Result<AuthSession> = runCatching {
        api.twoFactorChallenge(
            TwoFactorChallengeRequest(
                challengeToken = challengeToken,
                code = code,
                recoveryCode = recoveryCode,
                deviceName = deviceName(),
            )
        ).toSession()
    }.onSuccess {
        tokenStore.save(it)
        deviceRepository.registerCurrentDevice()
        syncScheduler.syncNow()
    }.mapError()

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String,
        termsAccepted: Boolean,
    ): Result<AuthSession> = runCatching {
        api.register(
            RegisterRequest(
                name = name,
                email = email,
                password = password,
                passwordConfirmation = passwordConfirmation,
                terms = termsAccepted,
                deviceName = deviceName(),
            )
        ).toSession()
    }.onSuccess {
        tokenStore.save(it)
        deviceRepository.registerCurrentDevice()
        syncScheduler.syncNow()
    }.mapError()

    override suspend fun logout() {
        runCatching { api.logout() }
        deviceRepository.deregisterCurrentDevice()
        tokenStore.clear()
        syncScheduler.cancelAll()
        contactsDao.clearAll()
        callEventStore.clear()
    }

    private fun AuthResponse.toSession(): AuthSession =
        AuthSession(accessToken = token, user = user.toDomain())

    private fun UserDto.toDomain(): AuthUser = AuthUser(
        ulid = ulid,
        name = name,
        email = email,
        currentTeam = currentTeam?.let { TeamSummary(it.uuid, it.name) },
    )

    private fun <T> Result<T>.mapError(): Result<T> =
        fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(it.toDomainError(json)) })

    private fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
}
