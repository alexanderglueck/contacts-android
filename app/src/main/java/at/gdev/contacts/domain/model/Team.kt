package at.gdev.contacts.domain.model

data class Team(
    val uuid: String,
    val name: String,
    val isOwner: Boolean,
    val isCurrent: Boolean,
)
