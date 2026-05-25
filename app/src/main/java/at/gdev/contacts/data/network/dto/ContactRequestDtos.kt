package at.gdev.contacts.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactNumberRequest(
    val name: String,
    val number: String,
)

@Serializable
data class ContactEmailRequest(
    val name: String,
    val email: String,
)

@Serializable
data class ContactUrlRequest(
    val name: String,
    val url: String,
)

@Serializable
data class ContactNoteRequest(
    val name: String,
    val note: String,
)

@Serializable
data class ContactDateRequest(
    val name: String,
    val date: String,
    @SerialName("skip_year") val skipYear: Boolean = false,
)

@Serializable
data class ContactAddressRequest(
    val name: String,
    val street: String,
    val zip: String,
    val city: String,
    val state: String? = null,
    @SerialName("country_id") val countryId: Int,
)

@Serializable
data class ContactCallRequest(
    @SerialName("called_at") val calledAt: String,
    val note: String? = null,
)

@Serializable
data class ContactGiftIdeaRequest(
    val name: String,
    val description: String? = null,
    val url: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
)

/** Server treats all fields as optional for partial update; `explicitNulls = false` keeps payloads tight. */
@Serializable
data class ContactUpdateRequest(
    val salutation: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val title: String? = null,
    @SerialName("title_after") val titleAfter: String? = null,
    val nickname: String? = null,
    @SerialName("gender_id") val genderId: Int? = null,
    val company: String? = null,
    val vatin: String? = null,
    val department: String? = null,
    val job: String? = null,
    @SerialName("custom_id") val customId: String? = null,
    val iban: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("died_at") val diedAt: String? = null,
    @SerialName("died_from") val diedFrom: String? = null,
    @SerialName("nationality_id") val nationalityId: Int? = null,
    @SerialName("first_met") val firstMet: String? = null,
    val note: String? = null,
    val active: Boolean? = null,
    @SerialName("contact_groups") val contactGroups: List<Int>? = null,
)
