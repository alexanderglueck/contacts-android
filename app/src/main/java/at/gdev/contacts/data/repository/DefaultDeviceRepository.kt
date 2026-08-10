package at.gdev.contacts.data.repository

import android.os.Build
import android.util.Log
import at.gdev.contacts.data.auth.TokenStore
import at.gdev.contacts.data.fcm.FcmTokenProvider
import at.gdev.contacts.data.network.DevicesApi
import at.gdev.contacts.data.network.dto.RegisterDeviceRequest
import at.gdev.contacts.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDeviceRepository @Inject constructor(
    private val api: DevicesApi,
    private val tokenStore: TokenStore,
    private val fcmTokenProvider: FcmTokenProvider,
) : DeviceRepository {

    override suspend fun registerCurrentDevice() {
        runCatching {
            val token = fcmTokenProvider.current()
            // De-dupe: skip if this exact token was already registered.
            if (token == tokenStore.registeredFcmToken()) return
            register(token)
        }.onFailure { Log.w(TAG, "Device registration failed", it) }
    }

    override suspend fun onFcmTokenRefreshed(token: String) {
        tokenStore.saveFcmToken(token)
        // Only register if signed in; otherwise the next sign-in will register it.
        if (tokenStore.token.first() == null) return
        runCatching { register(token) }
            .onFailure { Log.w(TAG, "Re-registration after token refresh failed", it) }
    }

    override suspend fun onFcmRegistrationChanged() {
        // Only the FID moved, so the token-based de-dupe in registerCurrentDevice
        // would skip this; re-POST instead, which the backend upserts by token.
        if (tokenStore.token.first() == null) return
        runCatching { register(fcmTokenProvider.current()) }
            .onFailure { Log.w(TAG, "Re-registration after installation-ID change failed", it) }
    }

    /** POSTs the device and remembers the token + returned ULID for de-dupe / de-registration. */
    private suspend fun register(token: String) {
        val request = RegisterDeviceRequest(
            name = deviceName(),
            token = token,
            fid = fcmTokenProvider.installationId(),
        )
        val device = api.register(request).data
        tokenStore.setRegisteredFcmToken(token)
        tokenStore.setRegisteredDeviceId(device.id)
    }

    private fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    private companion object {
        const val TAG = "DeviceRepository"
    }
}
