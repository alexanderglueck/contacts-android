package at.gdev.contacts.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import at.gdev.contacts.domain.repository.ContactsRepository
import at.gdev.contacts.ui.widget.UpcomingWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ContactSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: ContactsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val outcome = repository.syncAll()
        // Rides on the daily sync so the widget's "Today" stays current.
        runCatching { UpcomingWidget().updateAll(applicationContext) }
        return outcome.fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            },
        )
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}
