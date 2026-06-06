package at.gdev.contacts.domain.repository

/**
 * Registers this device's FCM token with the backend so it can receive push
 * notifications (today's birthdays). Registration is invisible to the user and
 * de-duped by token.
 */
interface DeviceRepository {
    /** Registers the current device if its FCM token hasn't been registered yet. */
    suspend fun registerCurrentDevice()

    /** Persists a refreshed FCM token and re-registers when a session exists. */
    suspend fun onFcmTokenRefreshed(token: String)

    /** Best-effort removal of this device's token on logout. */
    suspend fun deregisterCurrentDevice()
}
