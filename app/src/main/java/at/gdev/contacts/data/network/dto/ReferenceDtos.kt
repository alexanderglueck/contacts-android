package at.gdev.contacts.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReferenceListResponse(
    val data: List<ReferenceItemDto>,
)

@Serializable
data class ReferenceItemDto(
    val id: Int,
    val name: String,
)
