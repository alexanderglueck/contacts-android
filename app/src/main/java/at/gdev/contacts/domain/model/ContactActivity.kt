package at.gdev.contacts.domain.model

/** One entry in a contact's edit history. */
data class ContactActivity(
    val id: Long,
    val event: String,
    val summary: String? = null,
    val userName: String? = null,
    val createdAt: String? = null,
    val changes: List<ContactActivityChange> = emptyList(),
)

data class ContactActivityChange(
    val field: String,
    val label: String,
    val old: String? = null,
    val new: String? = null,
)

data class ContactActivityPage(
    val items: List<ContactActivity>,
    val hasMore: Boolean,
)
