package at.gdev.contacts.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactDetailResponse(
    val data: ContactDetailDto,
)

@Serializable
data class ContactDetailDto(
    val ulid: String,
    val fullname: String,
    val firstname: String,
    val lastname: String,
    val nickname: String? = null,
    val title: String? = null,
    @SerialName("title_after") val titleAfter: String? = null,
    val salutation: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val active: Boolean = true,

    val company: String? = null,
    val job: String? = null,
    val department: String? = null,
    @SerialName("custom_id") val customId: String? = null,
    val iban: String? = null,
    val vatin: String? = null,

    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("died_at") val diedAt: String? = null,
    @SerialName("died_from") val diedFrom: String? = null,
    @SerialName("first_met") val firstMet: String? = null,
    val note: String? = null,

    val gender: NamedRefDto? = null,
    val nationality: NamedRefDto? = null,
    @SerialName("contact_groups") val contactGroups: List<NamedRefDto> = emptyList(),

    val numbers: List<ContactNumberDto> = emptyList(),
    val emails: List<ContactEmailDto> = emptyList(),
    val urls: List<ContactUrlDto> = emptyList(),
    val notes: List<ContactNoteDto> = emptyList(),
    val addresses: List<ContactAddressDto> = emptyList(),
    val dates: List<ContactDateDto> = emptyList(),
    val calls: List<ContactCallDto> = emptyList(),
    @SerialName("gift_ideas") val giftIdeas: List<ContactGiftIdeaDto> = emptyList(),
)

@Serializable
data class NamedRefDto(val id: Int, val name: String)

@Serializable
data class ContactNumberDto(val ulid: String, val name: String, val number: String)

@Serializable
data class ContactEmailDto(val ulid: String, val name: String, val email: String)

@Serializable
data class ContactUrlDto(val ulid: String, val name: String, val url: String)

@Serializable
data class ContactNoteDto(val ulid: String, val name: String, val note: String)

@Serializable
data class ContactAddressDto(
    val ulid: String,
    val name: String,
    val street: String,
    val zip: String,
    val city: String,
    val state: String,
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class ContactDateDto(
    val ulid: String,
    val name: String,
    val date: String? = null,
    @SerialName("skip_year") val skipYear: Boolean = false,
)

@Serializable
data class ContactCallDto(
    val ulid: String,
    @SerialName("called_at") val calledAt: String? = null,
    val note: String? = null,
)

@Serializable
data class ContactGiftIdeaDto(
    val ulid: String,
    val name: String,
    val description: String? = null,
    val url: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
)
