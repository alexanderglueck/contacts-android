package at.gdev.contacts.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
    val terms: Boolean,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class AuthResponse(
    val user: UserDto,
    val token: String,
)

/**
 * `POST /auth/login` returns either an [AuthResponse] (no 2FA) or this challenge envelope.
 * Both branches are decoded into the same DTO; check [twoFactorRequired] to discriminate.
 */
@Serializable
data class LoginResponse(
    val user: UserDto? = null,
    val token: String? = null,
    @SerialName("two_factor_required") val twoFactorRequired: Boolean? = null,
    @SerialName("challenge_token") val challengeToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null,
)

@Serializable
data class TwoFactorChallengeRequest(
    @SerialName("challenge_token") val challengeToken: String,
    val code: String? = null,
    @SerialName("recovery_code") val recoveryCode: String? = null,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class MeResponse(
    val user: UserDto,
)

@Serializable
data class LogoutResponse(
    val ok: Boolean,
)

@Serializable
data class UserDto(
    val ulid: String,
    val name: String,
    val email: String,
    @SerialName("current_team") val currentTeam: TeamSummaryDto? = null,
)

@Serializable
data class TeamSummaryDto(
    val uuid: String,
    val name: String,
)

@Serializable
data class ValidationErrorBody(
    val message: String,
    val errors: Map<String, List<String>> = emptyMap(),
)

@Serializable
data class GenericErrorBody(
    val message: String,
)
