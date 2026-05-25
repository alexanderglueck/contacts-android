package at.gdev.contacts.data.network

import at.gdev.contacts.data.network.dto.SwitchTeamResponse
import at.gdev.contacts.data.network.dto.TeamsListResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TeamsApi {
    @GET("teams")
    suspend fun list(): TeamsListResponse

    @POST("teams/{team}/switch")
    suspend fun switchTo(@Path("team") teamUuid: String): SwitchTeamResponse
}
