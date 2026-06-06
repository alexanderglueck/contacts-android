package at.gdev.contacts.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    val id: String,
    val name: String,
    @SerialName("can_push") val canPush: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
)
