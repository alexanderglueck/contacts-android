package at.gdev.contacts.data.fcm

import at.gdev.contacts.data.auth.TokenStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the identifiers push delivery is addressed by: the FCM registration
 * token, cached in [TokenStore] so we don't hit Firebase on every sign-in, and
 * the Firebase installation ID that FCM is migrating to.
 */
@Singleton
class FcmTokenProvider @Inject constructor(
    private val tokenStore: TokenStore,
) {
    /**
     * The FCM registration token, or null when this app instance registers by installation
     * ID instead: `getToken()` throws once firebase_messaging_installation_id_enabled is
     * set, and the two modes are mutually exclusive. Null is a normal state, not an error --
     * the caller registers with whichever identifier it has.
     */
    @Suppress("DEPRECATION") // Deprecated in favor of register(); see installationId().
    suspend fun current(): String? {
        tokenStore.currentFcmToken()?.takeIf { it.isNotBlank() }?.let { return it }
        val fresh = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        return fresh?.also { tokenStore.saveFcmToken(it) }
    }

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
     * Null until FCM has registered this installation. Registration is nudged here so
     * the callback fires, but it lands asynchronously -- the token remains what push
     * delivery runs on in the meantime.
     */
    suspend fun installationId(): String? {
        tokenStore.currentFid()?.let { return it }
        // register() is the call that links this installation to FCM as a message target,
        // and it is gated on the firebase_messaging_installation_id_enabled manifest flag --
        // which inversely *disables* getToken() when set, so the two registration modes
        // cannot run side by side. This build does not set the flag, so this throws
        // IllegalStateException and we stay on the token path. Kept, and deliberately
        // swallowed, because it is the correct call the moment the flag is enabled.
        runCatching { FirebaseMessaging.getInstance().register().await() }
        return null
    }
}
