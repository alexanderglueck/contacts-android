package at.gdev.contacts.data.repository

import at.gdev.contacts.data.auth.TokenStore
import at.gdev.contacts.data.network.TeamsApi
import at.gdev.contacts.data.network.toDomainError
import at.gdev.contacts.domain.model.Team
import at.gdev.contacts.domain.model.TeamSummary
import at.gdev.contacts.domain.repository.TeamsRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTeamsRepository @Inject constructor(
    private val api: TeamsApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) : TeamsRepository {

    override suspend fun list(): Result<List<Team>> = runCatching {
        api.list().teams.map { Team(it.uuid, it.name, it.isOwner, it.isCurrent) }
    }.mapApiError()

    override suspend fun switchTo(teamUuid: String): Result<TeamSummary> = runCatching {
        val response = api.switchTo(teamUuid)
        val team = TeamSummary(response.currentTeam.uuid, response.currentTeam.name)
        tokenStore.updateCurrentTeam(team)
        team
    }.mapApiError()

    private fun <T> Result<T>.mapApiError(): Result<T> =
        fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(it.toDomainError(json)) })
}
