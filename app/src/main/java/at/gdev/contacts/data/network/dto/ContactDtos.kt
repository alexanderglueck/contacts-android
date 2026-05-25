package at.gdev.contacts.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

@Serializable
data class ContactsListResponse(
    /** Spec types this as `string | array` (Laravel paginator quirk), so we hold raw JSON. */
    val data: JsonElement,
    val meta: PaginationMetaDto,
    val q: String = "",
) {
    fun contacts(json: Json): List<ContactSummaryDto> =
        if (data is JsonArray) {
            json.decodeFromJsonElement(ListSerializer(ContactSummaryDto.serializer()), data)
        } else {
            emptyList()
        }
}

@Serializable
data class ContactSummaryDto(
    val ulid: String,
    val fullname: String,
    val firstname: String,
    val lastname: String,
    val nickname: String? = null,
    val title: String? = null,
    @SerialName("title_after") val titleAfter: String? = null,
    val salutation: String? = null,
    val company: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val numbers: List<ContactSummaryNumberDto> = emptyList(),
)

@Serializable
data class ContactSummaryNumberDto(
    val ulid: String,
    val name: String,
    val number: String,
    val e164: String? = null,
)

@Serializable
data class PaginationMetaDto(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("last_page") val lastPage: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
    val from: Int? = null,
    val to: Int? = null,
)
