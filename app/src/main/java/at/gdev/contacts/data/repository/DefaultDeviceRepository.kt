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

    override suspend fun deregisterCurrentDevice() {
        runCatching {
            val deviceId = tokenStore.registeredDeviceId()
            if (!deviceId.isNullOrBlank()) {
                api.deregister(deviceId)
            }
        }.onFailure { Log.w(TAG, "Device de-registration failed", it) }
        tokenStore.setRegisteredFcmToken(null)
        tokenStore.setRegisteredDeviceId(null)
    }

    /** POSTs the device and remembers the token + returned ULID for de-dupe / de-registration. */
    private suspend fun register(token: String) {
        val device = api.register(RegisterDeviceRequest(name = deviceName(), token = token)).data
        tokenStore.setRegisteredFcmToken(token)
        tokenStore.setRegisteredDeviceId(device.id)
    }

    private fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    private companion object {
        const val TAG = "DeviceRepository"
    }
}
