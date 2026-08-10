package at.gdev.contacts.data.network

import at.gdev.contacts.data.network.dto.AuthResponse
import at.gdev.contacts.data.network.dto.LoginRequest
import at.gdev.contacts.data.network.dto.LoginResponse
import at.gdev.contacts.data.network.dto.LogoutResponse
import at.gdev.contacts.data.network.dto.MeResponse
import at.gdev.contacts.data.network.dto.RegisterRequest
import at.gdev.contacts.data.network.dto.TwoFactorChallengeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/two-factor/challenge")
    suspend fun twoFactorChallenge(@Body body: TwoFactorChallengeRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(): LogoutResponse

    /**
     * Revokes an explicitly supplied token. Used by the post-logout cleanup
     * worker, which runs after the local session (and its token) is cleared, so
     * the auth interceptor can no longer attach it.
     */
    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String): Response<Unit>

    @GET("auth/me")
    suspend fun me(): MeResponse
}
