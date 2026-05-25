package at.gdev.contacts.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * The endpoint may return either:
 *   {"data": [{contact_ulid, fullname, matched_number: {name, number}}, ...]}
 *   {"data": []}
 *
 * Both shapes have `data` as an array — we hold raw JSON and decode permissively.
 */
@Serializable
data class ContactByNumberResponse(
    val data: JsonElement,
) {
    fun matches(json: Json): List<ContactByNumberMatch> {
        if (data !is JsonArray) return emptyList()
        return data.mapNotNull { element ->
            (element as? JsonObject)?.let {
                runCatching {
                    json.decodeFromJsonElement(ContactByNumberMatch.serializer(), it)
                }.getOrNull()
            }
        }
    }
}

@Serializable
data class ContactByNumberMatch(
    @SerialName("contact_ulid") val contactUlid: String,
    val fullname: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("matched_number") val matchedNumber: MatchedNumberDto,
)

@Serializable
data class MatchedNumberDto(
    val name: String,
    val number: String,
    /** E.164-normalized form supplied by the server; not currently surfaced in the UI. */
    val e164: String? = null,
)
