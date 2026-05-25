package at.gdev.contacts.data.network

object ApiConfig {
    // Android emulator maps 10.0.2.2 to the host machine's localhost.
    // For a physical device, change this to your host's LAN IP (and ensure the backend listens on it).
    const val ORIGIN = "http://10.0.2.2:8080"
    const val BASE_URL = "$ORIGIN/api/v1/"

    // TODO: replace with real password-reset URL once available.
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
