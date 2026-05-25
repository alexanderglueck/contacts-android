package at.gdev.contacts.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The spec doesn't pin the event shape (Scramble couldn't introspect the
 * controller's dynamic payload), so we model each field as nullable and let
 * `ignoreUnknownKeys = true` swallow anything new.
 */
@Serializable
data class CalendarEventsResponse(
    val data: List<CalendarEventDto> = emptyList(),
)

@Serializable
data class CalendarEventDto(
    val type: String? = null,
    val date: String? = null,
    val title: String? = null,
    val name: String? = null,
    val years: Int? = null,
    val contact: CalendarEventContactDto? = null,
)

@Serializable
data class CalendarEventContactDto(
    val ulid: String? = null,
    val fullname: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
)
