package at.gdev.contacts.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val workManager get() = WorkManager.getInstance(context)
    private val onlineOnly = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Daily background refresh. Idempotent — calling more than once just keeps the schedule. */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<ContactSyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(onlineOnly)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Run-now sync. Used after a fresh login to warm the local cache. */
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<ContactSyncWorker>()
            .setConstraints(onlineOnly)
            .build()
        workManager.enqueueUniqueWork(
            ONE_SHOT_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(ONE_SHOT_WORK_NAME)
    }

    private companion object {
        const val PERIODIC_WORK_NAME = "contact-sync-periodic"
        const val ONE_SHOT_WORK_NAME = "contact-sync-now"
    }
}
