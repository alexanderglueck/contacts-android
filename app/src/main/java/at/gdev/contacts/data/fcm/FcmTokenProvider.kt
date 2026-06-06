package at.gdev.contacts.data.fcm

import at.gdev.contacts.data.auth.TokenStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the device's current FCM registration token, caching it in [TokenStore]
 * so we don't hit Firebase on every sign-in.
 */
@Singleton
class FcmTokenProvider @Inject constructor(
    private val tokenStore: TokenStore,
) {
    suspend fun current(): String {
        val cached = tokenStore.currentFcmToken()
        if (!cached.isNullOrBlank()) return cached
        val fresh = FirebaseMessaging.getInstance().token.await()
        tokenStore.saveFcmToken(fresh)
        return fresh
    }
}
