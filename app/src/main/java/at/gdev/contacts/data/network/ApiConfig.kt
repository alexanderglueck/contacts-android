package at.gdev.contacts.data.network

object ApiConfig {
    const val ORIGIN = "https://contacts.alexanderglueck.at"
    const val BASE_URL = "$ORIGIN/api/v1/"

    const val FORGOT_PASSWORD_URL = "$ORIGIN/forgot-password"

    /**
     * Backend often emits image URLs against its own configured APP_URL, which
     * during local dev is `http://localhost:8080/...` — from inside the emulator
     * that resolves to the device itself, not the dev machine. Swap the host so
     * Coil can reach it. Also absolutize relative paths.
     */
    fun normalizeImageUrl(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val absolute = if (raw.startsWith("/")) "$ORIGIN$raw" else raw
        return absolute
            .replace("http://localhost:8080", ORIGIN)
            .replace("http://127.0.0.1:8080", ORIGIN)
    }
}
