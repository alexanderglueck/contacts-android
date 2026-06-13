package at.gdev.contacts.calls

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import at.gdev.contacts.R
import at.gdev.contacts.domain.model.ContactLookup
import at.gdev.contacts.ui.MainActivity
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallerIdNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    init {
        val nm = context.getSystemService(NotificationManager::class.java)!!
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Incoming caller ID",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Heads-up alerts naming an incoming caller from your contacts"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    /**
     * Display the matched contact as a heads-up notification over the system
     * call screen. If the contact has an avatar URL, the image is loaded via
     * Coil and attached as the large icon; loads that fail fall back to the
     * launcher icon silently.
     */
    suspend fun showMatch(rawNumber: String, lookup: ContactLookup) {
        val largeIcon = lookup.imageUrl?.let { loadAvatarBitmap(it) }

        val openContactIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            lookup.contactId.hashCode(),
            openContactIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .apply { if (largeIcon != null) setLargeIcon(largeIcon) }
            .setContentTitle(lookup.fullName)
            .setContentText("Calling: ${lookup.matchedLabel} · ${lookup.matchedNumber}")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)!!
        nm.notify(notificationIdFor(rawNumber), notification)
    }

    fun dismiss(rawNumber: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.cancel(notificationIdFor(rawNumber))
    }

    private suspend fun loadAvatarBitmap(url: String): Bitmap? = runCatching {
        val request = ImageRequest.Builder(context)
            .data(url)
            // Notifications need a software-backed bitmap; HARDWARE bitmaps throw.
            .allowHardware(false)
            .build()
        when (val result = context.imageLoader.execute(request)) {
            is SuccessResult -> result.drawable.toBitmap()
            else -> null
        }
    }.getOrNull()

    private fun notificationIdFor(rawNumber: String): Int =
        (NOTIFICATION_ID_BASE + rawNumber.hashCode()) and Int.MAX_VALUE

    private companion object {
        const val CHANNEL_ID = "caller_id"
        const val NOTIFICATION_ID_BASE = 0x10000
    }
}
