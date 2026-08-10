package at.gdev.contacts.data.fcm

import at.gdev.contacts.data.auth.TokenStore
import com.google.firebase.installations.FirebaseInstallations
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
     * The Firebase installation ID, which FCM's non-deprecated registration flow
     * uses in place of a registration token. Best-effort: a failure here must not
     * block registration, since the token is what push delivery still runs on.
     */
    suspend fun installationId(): String? =
        runCatching { FirebaseInstallations.getInstance().id.await() }.getOrNull()
}
