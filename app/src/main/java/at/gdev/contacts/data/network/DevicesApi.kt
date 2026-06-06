package at.gdev.contacts.data.network

import at.gdev.contacts.data.network.dto.DataEnvelope
import at.gdev.contacts.data.network.dto.DeviceDto
import at.gdev.contacts.data.network.dto.RegisterDeviceRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

interface DevicesApi {
    /** Registers the current device (name + FCM token) for the signed-in user. Returns 201. */
    @POST("devices")
    suspend fun register(@Body body: RegisterDeviceRequest): DataEnvelope<DeviceDto>

    /** Removes one of the user's devices by its ULID (on logout). Returns 204. */
    @DELETE("devices/{device}")
    suspend fun deregister(@Path("device") deviceId: String): Response<Unit>
}
