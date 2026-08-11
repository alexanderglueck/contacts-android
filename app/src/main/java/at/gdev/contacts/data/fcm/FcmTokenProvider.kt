package at.gdev.contacts.data.fcm

import at.gdev.contacts.data.auth.TokenStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the installation ID that push delivery is addressed by, plus -- for one
 * registration only -- the registration token this device used before the cutover.
 */
@Singleton
class FcmTokenProvider @Inject constructor(
    private val tokenStore: TokenStore,
) {
    /**
     * The registration token this device last held, read only from [TokenStore] and never
     * from the SDK: `getToken()` throws now that registration goes through installation IDs.
     *
     * Sent alongside the FID on the first registration after the cutover, purely so the
     * backend can match this device by its old token and adopt the new FID onto the existing
     * row instead of orphaning it. Cleared once that has happened, after which registration
     * is by FID alone. Null on installs that never held a token.
     */
    suspend fun preCutoverToken(): String? =
        tokenStore.currentFcmToken()?.takeIf { it.isNotBlank() }

    /**
     * The installation ID FCM has registered as a messaging target, as reported by
     * [at.gdev.contacts.data.fcm.ContactsMessagingService.onRegistered].
     *
     * Deliberately not FirebaseInstallations.getId(): that hands back an installation
     * identifier, which is *not* addressable on its own. Sending to one was verified
     * against production FCM and answered 404 UNREGISTERED, both in the `token` field
     * and in the native `fid` field. Only the value the registration callback reports
     * is a valid target.
     *
     * Null until FCM has registered this installation. register() is nudged here so the
     * callback fires; it resolves asynchronously, so a null return simply means "not yet"
     * and registration is deferred until `onRegistered` arrives.
     */
    suspend fun installationId(): String? {
        tokenStore.currentFid()?.let { return it }
        // Links this installation to FCM as a message target. Auto-init fires this on startup
        // too, so the nudge only matters when we need an ID before that has happened.
        runCatching { FirebaseMessaging.getInstance().register().await() }
        return null
    }
}
