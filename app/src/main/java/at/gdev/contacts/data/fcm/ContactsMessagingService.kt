package at.gdev.contacts.data.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import at.gdev.contacts.R
import at.gdev.contacts.data.auth.TokenStore
import at.gdev.contacts.domain.repository.DeviceRepository
import at.gdev.contacts.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.random.Random

/**
 * Receives birthday push notifications. The backend sends a data-only message
 * with `title`/`body`; tapping the notification opens the calendar at today.
 */
@AndroidEntryPoint
class ContactsMessagingService : FirebaseMessagingService() {

    @Inject lateinit var deviceRepository: DeviceRepository
    @Inject lateinit var tokenStore: TokenStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken: ${token.take(12)}…")
        scope.launch { deviceRepository.onFcmTokenRefreshed(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Only notify a signed-in user; a logged-out device shouldn't be receiving pushes anyway.
        val signedIn = runBlocking { tokenStore.token.first() != null }
        if (!signedIn) {
            Log.d(TAG, "Not signed in — dropping push")
            return
        }

        val title = message.data["title"]?.takeIf { it.isNotBlank() }
            ?: message.notification?.title
            ?: getString(R.string.app_name)
        val body = message.data["body"]?.takeIf { it.isNotBlank() }
            ?: message.notification?.body.orEmpty()

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_CALENDAR, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification =
            NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(this, R.color.notification_accent))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        val manager = getSystemService<NotificationManager>() ?: return
        manager.notify(Random.nextInt(), notification)
    }

    private companion object {
        const val TAG = "ContactsFcm"
    }
}
