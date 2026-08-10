package at.gdev.contacts.domain.repository

/**
 * Registers this device with the backend so it can receive push notifications
 * (today's birthdays). Registration is invisible to the user and de-duped by
 * token.
 *
 * Each registration carries two identifiers: the legacy FCM registration token,
 * which is what push delivery currently uses, and the Firebase installation ID
 * (FID), which is the identifier FCM's non-deprecated registration flow hands
 * out. FCM co-supports both, addressed by the v1 send API's `token` and `fid`
 * fields respectively, so we upload both and the backend can switch fields
 * without needing a second client release.
 */
interface DeviceRepository {
    /** Registers the current device if its FCM token hasn't been registered yet. */
    suspend fun registerCurrentDevice()

    /** Persists a refreshed FCM token and re-registers when a session exists. */
    suspend fun onFcmTokenRefreshed(token: String)

    /**
     * Persists the installation ID FCM reports through `onRegistered` and re-registers,
     * so the backend sees the current FID even when the legacy token is unchanged.
     *
     * This callback is the only source of an addressable FID; an ID read from
     * FirebaseInstallations is not a messaging target and FCM rejects it.
     */
    suspend fun onFcmRegistrationChanged(installationId: String)
}
