package at.gdev.contacts.domain.model

import java.time.LocalDate

/**
 * Partial-update payload for a contact's base fields. Any null is left unchanged
 * server-side; pass an empty list to clear contact_groups.
 */
data class ContactPatch(
    val salutation: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val titleBefore: String? = null,
    val titleAfter: String? = null,
    val nickname: String? = null,
    val genderId: Int? = null,
    val company: String? = null,
    val vatin: String? = null,
    val department: String? = null,
    val job: String? = null,
    val customId: String? = null,
    val iban: String? = null,
    val dateOfBirth: LocalDate? = null,
    val diedAt: LocalDate? = null,
    val diedFrom: String? = null,
    val nationalityId: Int? = null,
    val firstMet: String? = null,
    val note: String? = null,
    val active: Boolean? = null,
    val contactGroupIds: List<Int>? = null,
)
