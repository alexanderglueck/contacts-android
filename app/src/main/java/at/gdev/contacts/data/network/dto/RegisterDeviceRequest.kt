package at.gdev.contacts.data.network.dto

import kotlinx.serialization.Serializable

/** Invisible device registration: upsert-by-token on the backend. */
@Serializable
data class RegisterDeviceRequest(
    val name: String,
    val token: String,
    /**
     * Firebase installation ID. FCM's `token` field is deprecated in favor of
     * `fid`, so we send both and let the backend pick which one it addresses
     * pushes with. Null when Firebase can't resolve an installation.
     */
    val fid: String? = null,
)
