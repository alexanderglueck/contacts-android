package at.gdev.contacts.domain.repository

import at.gdev.contacts.domain.model.Contact
import at.gdev.contacts.domain.model.ContactLookup
import at.gdev.contacts.domain.model.ContactPatch
import at.gdev.contacts.domain.model.ContactSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** One page of picker search results plus whether more pages exist. */
data class ContactSearchPage(val contacts: List<ContactSummary>, val hasMore: Boolean)

interface ContactsRepository {
    val summaries: Flow<List<ContactSummary>>
    suspend fun refresh(query: String? = null): Result<Unit>
    suspend fun getContact(id: String): Contact?

    /** Paginated remote search for pickers (does not touch the [summaries] cache). */
    suspend fun searchContacts(query: String, page: Int = 1): ContactSearchPage

    suspend fun lookupByNumber(rawNumber: String): List<ContactLookup>
    suspend fun syncAll(): Result<Int>

    // Contact base mutations
    suspend fun createContact(patch: ContactPatch): Result<String>
    suspend fun updateContact(id: String, patch: ContactPatch): Result<Unit>
    suspend fun deleteContact(id: String): Result<Unit>

    // Avatar
    suspend fun uploadContactImage(id: String, bytes: ByteArray, mimeType: String): Result<Unit>
    suspend fun removeContactImage(id: String): Result<Unit>

    // Numbers
    suspend fun addNumber(contactId: String, name: String, number: String): Result<Unit>
    suspend fun updateNumber(contactId: String, numberId: String, name: String, number: String): Result<Unit>
    suspend fun deleteNumber(contactId: String, numberId: String): Result<Unit>

    // Emails
    suspend fun addEmail(contactId: String, name: String, email: String): Result<Unit>
    suspend fun updateEmail(contactId: String, emailId: String, name: String, email: String): Result<Unit>
    suspend fun deleteEmail(contactId: String, emailId: String): Result<Unit>

    // URLs
    suspend fun addUrl(contactId: String, name: String, url: String): Result<Unit>
    suspend fun updateUrl(contactId: String, urlId: String, name: String, url: String): Result<Unit>
    suspend fun deleteUrl(contactId: String, urlId: String): Result<Unit>

    // Notes
    suspend fun addNote(contactId: String, name: String, note: String): Result<Unit>
    suspend fun updateNote(contactId: String, noteId: String, name: String, note: String): Result<Unit>
    suspend fun deleteNote(contactId: String, noteId: String): Result<Unit>

    // Dates
    suspend fun addDate(contactId: String, name: String, date: LocalDate, skipYear: Boolean): Result<Unit>
    suspend fun updateDate(contactId: String, dateId: String, name: String, date: LocalDate, skipYear: Boolean): Result<Unit>
    suspend fun deleteDate(contactId: String, dateId: String): Result<Unit>

    // Addresses
    suspend fun addAddress(
        contactId: String,
        name: String,
        street: String,
        zip: String,
        city: String,
        state: String?,
        countryId: Int,
    ): Result<Unit>

    suspend fun updateAddress(
        contactId: String,
        addressId: String,
        name: String,
        street: String,
        zip: String,
        city: String,
        state: String?,
        countryId: Int,
    ): Result<Unit>

    suspend fun deleteAddress(contactId: String, addressId: String): Result<Unit>

    // Calls
    suspend fun addCall(contactId: String, calledAt: String, note: String?): Result<Unit>
    suspend fun updateCall(contactId: String, callId: String, calledAt: String, note: String?): Result<Unit>
    suspend fun deleteCall(contactId: String, callId: String): Result<Unit>

    // Comments — kept separate from the contact detail; fetch explicitly.
    suspend fun listComments(contactId: String): Result<List<at.gdev.contacts.domain.model.ContactComment>>
    suspend fun addComment(contactId: String, text: String, parentId: String? = null): Result<Unit>
    suspend fun updateComment(contactId: String, commentId: String, text: String): Result<Unit>
    suspend fun deleteComment(contactId: String, commentId: String): Result<Unit>

    // Relations
    suspend fun addRelation(
        contactId: String,
        relatedContactId: String,
        forwardLabel: String,
        inverseLabel: String?,
    ): Result<Unit>

    suspend fun updateRelation(
        contactId: String,
        relationId: String,
        forwardLabel: String,
        inverseLabel: String?,
    ): Result<Unit>

    suspend fun deleteRelation(contactId: String, relationId: String): Result<Unit>

    // Gift ideas
    suspend fun addGiftIdea(
        contactId: String,
        name: String,
        description: String?,
        url: String?,
        dueAt: LocalDate?,
    ): Result<Unit>

    suspend fun updateGiftIdea(
        contactId: String,
        giftId: String,
        name: String,
        description: String?,
        url: String?,
        dueAt: LocalDate?,
    ): Result<Unit>

    suspend fun deleteGiftIdea(contactId: String, giftId: String): Result<Unit>
}
