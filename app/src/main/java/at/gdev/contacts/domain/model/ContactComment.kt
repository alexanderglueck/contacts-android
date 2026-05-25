package at.gdev.contacts.domain.model

data class ContactComment(
    val id: String,
    val parentId: String? = null,
    val text: String,
    val tombstoned: Boolean = false,
    val owner: NamedRef? = null,
    val createdAt: String,
    val updatedAt: String,
)
