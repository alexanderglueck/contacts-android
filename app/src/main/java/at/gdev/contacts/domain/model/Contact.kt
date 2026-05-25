package at.gdev.contacts.domain.model

import java.time.LocalDate

data class ContactSummary(
    val id: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val titleBefore: String? = null,
    val titleAfter: String? = null,
    val nickname: String? = null,
    val salutation: String? = null,
    val company: String? = null,
    val imageUrl: String? = null,
) {
    val displayName: String
        get() = composeDisplayName(titleBefore, firstName, lastName, titleAfter, nickname, fallback = fullName)
}

data class Contact(
    val id: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val nickname: String? = null,
    val titleBefore: String? = null,
    val titleAfter: String? = null,
    val salutation: String? = null,
    val imageUrl: String? = null,
    val active: Boolean = true,

    val company: String? = null,
    val job: String? = null,
    val department: String? = null,
    val customId: String? = null,
    val iban: String? = null,
    val vatin: String? = null,

    val dateOfBirth: LocalDate? = null,
    val diedAt: LocalDate? = null,
    val diedFrom: String? = null,
    val firstMet: String? = null,
    val note: String? = null,

    val gender: NamedRef? = null,
    val nationality: NamedRef? = null,
    val contactGroups: List<NamedRef> = emptyList(),

    val numbers: List<ContactNumber> = emptyList(),
    val emails: List<ContactEmail> = emptyList(),
    val urls: List<ContactUrl> = emptyList(),
    val notes: List<ContactNote> = emptyList(),
    val addresses: List<ContactAddress> = emptyList(),
    val dates: List<ContactDate> = emptyList(),
    val calls: List<ContactCall> = emptyList(),
    val giftIdeas: List<ContactGiftIdea> = emptyList(),
) {
    val displayName: String
        get() = composeDisplayName(titleBefore, firstName, lastName, titleAfter, nickname, fallback = fullName)
}

data class NamedRef(val id: Int, val name: String)

data class ContactNumber(val id: String, val name: String, val number: String)
data class ContactEmail(val id: String, val name: String, val email: String)
data class ContactUrl(val id: String, val name: String, val url: String)
data class ContactNote(val id: String, val name: String, val text: String)
data class ContactAddress(
    val id: String,
    val name: String,
    val street: String,
    val zip: String,
    val city: String,
    val state: String,
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
data class ContactDate(
    val id: String,
    val name: String,
    val date: LocalDate?,
    val skipYear: Boolean,
)
data class ContactCall(
    val id: String,
    val calledAt: String?,
    val note: String?,
)
data class ContactGiftIdea(
    val id: String,
    val name: String,
    val description: String?,
    val url: String?,
    val dueAt: LocalDate?,
)
