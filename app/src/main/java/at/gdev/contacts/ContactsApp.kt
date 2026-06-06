package at.gdev.contacts

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.provider.ContactsContract
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import at.gdev.contacts.data.sync.ContactSyncScheduler
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class ContactsApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: ContactSyncScheduler
    @Inject lateinit var okHttpClient: OkHttpClient

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Hand Coil the same authenticated OkHttp client Retrofit uses, so any
     * image URL that's auth-gated picks up the Bearer token automatically.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(okHttpClient)
        .crossfade(true)
        .build()

    override fun onCreate() {
        super.onCreate()
        syncScheduler.schedulePeriodic()
        notifyDirectoryAvailable()
        createBirthdayNotificationChannel()
    }

    /** Channel that birthday push notifications are posted to. */
    private fun createBirthdayNotificationChannel() {
        val manager = getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            getString(R.string.default_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.default_notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Tells the system to drop any cached list of contact directories. Without
     * this nudge the dialer may not pick up our [calls.ContactsDirectoryProvider]
     * until after the user reboots or the system happens to rescan.
     */
    private fun notifyDirectoryAvailable() {
        runCatching { ContactsContract.Directory.notifyDirectoryChange(contentResolver) }
    }
}
