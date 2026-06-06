package at.gdev.contacts.data.network.dto

import kotlinx.serialization.Serializable

/** Invisible device registration: upsert-by-token on the backend. */
@Serializable
data class RegisterDeviceRequest(
    val name: String,
    val token: String,
)
