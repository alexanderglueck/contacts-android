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
    @Suppress("DEPRECATION") // See installationId(): the backend still addresses pushes by token.
    suspend fun current(): String {
        val cached = tokenStore.currentFcmToken()
        if (!cached.isNullOrBlank()) return cached
        val fresh = FirebaseMessaging.getInstance().token.await()
        tokenStore.saveFcmToken(fresh)
        return fresh
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
        runCatching { FirebaseMessaging.getInstance().register().await() }
        return null
    }
}
