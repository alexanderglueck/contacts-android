package at.gdev.contacts.domain.model

data class ContactLookup(
    val contactId: String,
    val fullName: String,
    val matchedNumber: String,
    val matchedLabel: String,
    val imageUrl: String? = null,
)
