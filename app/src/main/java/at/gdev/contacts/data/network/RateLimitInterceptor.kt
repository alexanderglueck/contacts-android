package at.gdev.contacts.data.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Retries 429 responses with backoff. Honors `Retry-After` (seconds) when the server
 * sends one; otherwise falls back to exponential backoff capped at [MAX_BACKOFF_MS].
 * Body is closed between attempts so the connection can be reused.
 */
class RateLimitInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var response = chain.proceed(chain.request())
        var attempt = 0
        while (response.code == 429 && attempt < MAX_RETRIES) {
            val retryAfterMs = response.header("Retry-After")
                ?.toLongOrNull()
                ?.times(1000L)
            val backoffMs = (retryAfterMs ?: (BASE_BACKOFF_MS shl attempt))
                .coerceAtMost(MAX_BACKOFF_MS)
            response.close()
            try {
                Thread.sleep(backoffMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw e
            }
            response = chain.proceed(chain.request())
            attempt += 1
        }
        return response
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val BASE_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_MS = 30_000L
    }
}
