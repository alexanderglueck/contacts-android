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
            val fid = fcmTokenProvider.installationId()
            if (token == null && fid == null) {
                // Registering by installation ID and FCM hasn't reported one yet; onRegistered
                // will call back and register us then.
                Log.d(TAG, "No push identifier available yet, deferring registration")
                return
            }
            // De-dupe on both identifiers: either one changing is worth a re-POST.
            if (token == tokenStore.registeredFcmToken() && fid == tokenStore.registeredFid()) return
            register(token, fid)
        }.onFailure { Log.w(TAG, "Device registration failed", it) }
    }

    override suspend fun onFcmTokenRefreshed(token: String) {
        tokenStore.saveFcmToken(token)
        // Only register if signed in; otherwise the next sign-in will register it.
        if (tokenStore.token.first() == null) return
        runCatching { register(token, fcmTokenProvider.installationId()) }
            .onFailure { Log.w(TAG, "Re-registration after token refresh failed", it) }
    }

    override suspend fun onFcmRegistrationChanged(installationId: String) {
        tokenStore.saveFid(installationId)
        // Only the FID moved, so the de-dupe in registerCurrentDevice may skip this;
        // re-POST instead, which the backend upserts onto the existing row.
        if (tokenStore.token.first() == null) return
        runCatching { register(fcmTokenProvider.current(), installationId) }
            .onFailure { Log.w(TAG, "Re-registration after installation-ID change failed", it) }
    }

    /**
     * POSTs the device and remembers both identifiers plus the returned ULID, for de-dupe and
     * de-registration. At least one of [token]/[fid] must be non-null; the backend requires a
     * name plus one identifier.
     */
    private suspend fun register(token: String?, fid: String?) {
        val request = RegisterDeviceRequest(name = deviceName(), token = token, fid = fid)
        val device = api.register(request).data
        tokenStore.setRegisteredFcmToken(token)
        tokenStore.setRegisteredFid(fid)
        tokenStore.setRegisteredDeviceId(device.id)
    }

    private fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    private companion object {
        const val TAG = "DeviceRepository"
    }
}
