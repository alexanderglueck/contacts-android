package at.gdev.contacts.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamsListResponse(
    val teams: List<TeamDto>,
)

@Serializable
data class TeamDto(
    val uuid: String,
    val name: String,
    @SerialName("is_owner") val isOwner: Boolean,
    @SerialName("is_current") val isCurrent: Boolean,
)

@Serializable
data class SwitchTeamResponse(
    @SerialName("current_team") val currentTeam: TeamSummaryDto,
)
