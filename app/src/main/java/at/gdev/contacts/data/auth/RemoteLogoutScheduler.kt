package at.gdev.contacts.data.auth

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues the server-side half of logout (de-register this device + revoke the
 * token) as retrying, connectivity-gated work, so a failed or offline sign-out
 * doesn't leave a stale device row on the backend.
 */
@Singleton
class RemoteLogoutScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val workManager get() = WorkManager.getInstance(context)

    /**
     * Schedules cleanup using the [token] and [deviceId] captured at logout time,
     * since the local session is wiped immediately.
     */
    fun schedule(token: String, deviceId: String?) {
        val request = OneTimeWorkRequestBuilder<RemoteLogoutWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(
                Data.Builder()
                    .putString(RemoteLogoutWorker.KEY_TOKEN, token)
                    .putString(RemoteLogoutWorker.KEY_DEVICE_ID, deviceId)
                    .build(),
            )
            .build()
        // APPEND_OR_REPLACE so signing out of one account never cancels a still-pending
        // cleanup for a previous one; each captured token gets its own run.
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "remote-logout"
    }
}
