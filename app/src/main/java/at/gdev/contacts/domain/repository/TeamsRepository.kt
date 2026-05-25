package at.gdev.contacts.domain.repository

import at.gdev.contacts.domain.model.Team
import at.gdev.contacts.domain.model.TeamSummary

interface TeamsRepository {
    suspend fun list(): Result<List<Team>>
    suspend fun switchTo(teamUuid: String): Result<TeamSummary>
}
