package at.gdev.contacts.data.auth

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import at.gdev.contacts.data.network.AuthApi
import at.gdev.contacts.data.network.DevicesApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.Response

/**
 * Performs the server-side half of logout — de-registering this device and
 * revoking the access token — after the local session has already been cleared.
 *
 * The token and device ULID arrive via [inputData] (captured at logout time)
 * because [TokenStore] is wiped immediately so the UI can sign out even when
 * offline. The worker therefore attaches the captured token itself instead of
 * relying on the auth interceptor, whose live token read now returns null. It
 * runs the DELETE before the revoke so both calls authenticate with a token that
 * is still valid, and retries transient failures with backoff.
 */
@HiltWorker
class RemoteLogoutWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val devicesApi: DevicesApi,
    private val authApi: AuthApi,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN) ?: return Result.success()
        val authorization = "Bearer $token"
        val deviceId = inputData.getString(KEY_DEVICE_ID)

        return runCatching {
            if (!deviceId.isNullOrBlank()) {
                devicesApi.deregister(authorization, deviceId).failIfRetryable()
            }
            authApi.logout(authorization).failIfRetryable()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Log.w(TAG, "Giving up remote logout cleanup after $runAttemptCount attempts", it)
                    Result.failure()
                }
            },
        )
    }

    /**
     * 2xx (done), 401/403 (token already invalid) and 404 (device row already
     * gone) are terminal — nothing a retry would fix. Anything else (5xx, 429, …)
     * throws so the run is retried; a network failure throws before returning a
     * response and is handled the same way.
     */
    private fun Response<Unit>.failIfRetryable() {
        if (isSuccessful || code() in TERMINAL_CODES) return
        error("Unexpected HTTP ${code()}")
    }

    companion object {
        const val KEY_TOKEN = "token"
        const val KEY_DEVICE_ID = "device_id"

        private const val TAG = "RemoteLogoutWorker"
        private const val MAX_RETRIES = 5
        private val TERMINAL_CODES = setOf(401, 403, 404)
    }
}
