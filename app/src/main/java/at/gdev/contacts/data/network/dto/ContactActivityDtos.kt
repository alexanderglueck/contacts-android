package at.gdev.contacts.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactActivitiesResponse(
    val data: List<ContactActivityDto>,
    val meta: PaginationMetaDto,
)

@Serializable
data class ContactActivityDto(
    val id: Long,
    /** "created", "updated" or "deleted". */
    val event: String,
    /** "contact", "number", "email", "url", "address" or "date". */
    val subject: String = "contact",
    /** Translated one-liner such as "Telefonnummer hinzugefügt". */
    val summary: String? = null,
    val user: ContactActivityUserDto? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val changes: List<ContactActivityChangeDto> = emptyList(),
)

@Serializable
data class ContactActivityUserDto(val name: String)

/** `label` is the field name translated server-side in the user's locale. */
@Serializable
data class ContactActivityChangeDto(
    val field: String,
    val label: String,
    val old: String? = null,
    val new: String? = null,
)
