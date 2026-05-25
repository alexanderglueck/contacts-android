package at.gdev.contacts.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactCommentsResponse(
    val data: List<ContactCommentDto>,
)

@Serializable
data class ContactCommentDto(
    val ulid: String,
    @SerialName("parent_ulid") val parentUlid: String? = null,
    val comment: String,
    val tombstoned: Boolean = false,
    val owner: NamedRefDto? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ContactCommentStoreRequest(
    val comment: String,
    @SerialName("parent_ulid") val parentUlid: String? = null,
)

@Serializable
data class ContactCommentUpdateRequest(
    val comment: String,
)
