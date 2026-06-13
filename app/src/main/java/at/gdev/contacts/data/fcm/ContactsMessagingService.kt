package at.gdev.contacts.data.fcm

import android.app.Notification
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
        val manager = getSystemService<NotificationManager>() ?: return
        val channelId = getString(R.string.default_notification_channel_id)

        val id = Random.nextInt()
        manager.notify(id, buildReminder(channelId, title, body, id))
        // (Re)post the group summary. When two or more reminders are pending the system
        // collapses them into a group, and the OS-generated summary wouldn't carry our
        // tap-intent — so tapping the collapsed stack would land on the default screen
        // instead of the calendar. Owning the summary keeps that tap deep-linking too.
        manager.notify(SUMMARY_ID, buildSummary(channelId))
    }

    private fun buildReminder(channelId: String, title: String, body: String, id: Int): Notification =
        baseBuilder(channelId, id)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

    private fun buildSummary(channelId: String): Notification =
        baseBuilder(channelId, SUMMARY_ID)
            .setGroupSummary(true)
            .build()

    /** Shared notification setup; every reminder and the summary open the calendar on tap. */
    private fun baseBuilder(channelId: String, requestCode: Int): NotificationCompat.Builder {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_CALENDAR, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.notification_accent))
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setContentIntent(pendingIntent)
    }

    private companion object {
        const val TAG = "ContactsFcm"

        /** Groups birthday reminders so multiple pending ones collapse into one stack. */
        const val GROUP_KEY = "at.gdev.contacts.birthday_reminders"

        /** Stable id for the group-summary notification (children use random ids). */
        const val SUMMARY_ID = 1
    }
}
