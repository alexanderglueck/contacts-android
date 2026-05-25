package at.gdev.contacts.domain.model

data class AuthSession(
    val accessToken: String,
    val user: AuthUser,
)

data class AuthUser(
    val ulid: String,
    val name: String,
    val email: String,
    val currentTeam: TeamSummary? = null,
)

data class TeamSummary(
    val uuid: String,
    val name: String,
)

sealed interface LoginResult {
    data class Authenticated(val session: AuthSession) : LoginResult
    data class TwoFactorRequired(val challengeToken: String, val expiresInSeconds: Int) : LoginResult
}
