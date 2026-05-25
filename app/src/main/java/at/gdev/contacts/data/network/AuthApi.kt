package at.gdev.contacts.data.network

import at.gdev.contacts.data.network.dto.AuthResponse
import at.gdev.contacts.data.network.dto.LoginRequest
import at.gdev.contacts.data.network.dto.LoginResponse
import at.gdev.contacts.data.network.dto.LogoutResponse
import at.gdev.contacts.data.network.dto.MeResponse
import at.gdev.contacts.data.network.dto.RegisterRequest
import at.gdev.contacts.data.network.dto.TwoFactorChallengeRequest
import retrofit2.http.Body
import retrofit2.http.GET
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

    @GET("auth/me")
    suspend fun me(): MeResponse
}
