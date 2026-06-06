package at.gdev.contacts.data.network.dto

import kotlinx.serialization.Serializable

/** Standard `{ "data": ... }` response wrapper used by the device endpoints. */
@Serializable
data class DataEnvelope<T>(val data: T)
