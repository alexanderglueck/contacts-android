package at.gdev.contacts.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import at.gdev.contacts.domain.repository.ContactsRepository
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
