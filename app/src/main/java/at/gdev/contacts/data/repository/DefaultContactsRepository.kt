package at.gdev.contacts.data.repository

import at.gdev.contacts.data.local.ContactEntity
import at.gdev.contacts.data.local.ContactNumberEntity
import at.gdev.contacts.data.local.ContactsDao
import at.gdev.contacts.data.network.ApiConfig
import at.gdev.contacts.data.network.ContactsApi
import at.gdev.contacts.data.network.dto.ContactAddressDto
import at.gdev.contacts.data.network.dto.ContactAddressRequest
import at.gdev.contacts.data.network.dto.ContactByNumberMatch
import at.gdev.contacts.data.network.dto.ContactCallDto
import at.gdev.contacts.data.network.dto.ContactCallRequest
import at.gdev.contacts.data.network.dto.ContactCommentDto
import at.gdev.contacts.data.network.dto.ContactCommentStoreRequest
import at.gdev.contacts.data.network.dto.ContactCommentUpdateRequest
import at.gdev.contacts.data.network.dto.ContactDateDto
import at.gdev.contacts.data.network.dto.ContactDateRequest
import at.gdev.contacts.data.network.dto.ContactDetailDto
import at.gdev.contacts.data.network.dto.ContactEmailDto
import at.gdev.contacts.data.network.dto.ContactEmailRequest
import at.gdev.contacts.data.network.dto.ContactGiftIdeaDto
import at.gdev.contacts.data.network.dto.ContactGiftIdeaRequest
import at.gdev.contacts.data.network.dto.ContactNoteDto
import at.gdev.contacts.data.network.dto.ContactNoteRequest
import at.gdev.contacts.data.network.dto.ContactNumberDto
import at.gdev.contacts.data.network.dto.ContactNumberRequest
import at.gdev.contacts.data.network.dto.ContactSummaryDto
import at.gdev.contacts.data.network.dto.ContactSummaryNumberDto
import at.gdev.contacts.data.network.dto.ContactUpdateRequest
import at.gdev.contacts.data.network.dto.ContactsStoreRequest
import at.gdev.contacts.data.network.dto.ContactUrlDto
import at.gdev.contacts.data.network.dto.ContactUrlRequest
import at.gdev.contacts.data.network.dto.NamedRefDto
import at.gdev.contacts.data.network.toDomainError
import at.gdev.contacts.domain.model.Contact
import at.gdev.contacts.domain.model.ContactAddress
import at.gdev.contacts.domain.model.ContactCall
import at.gdev.contacts.domain.model.ContactComment
import at.gdev.contacts.domain.model.ContactDate
import at.gdev.contacts.domain.model.ContactEmail
import at.gdev.contacts.domain.model.ContactGiftIdea
import at.gdev.contacts.domain.model.ContactLookup
import at.gdev.contacts.domain.model.ContactNote
import at.gdev.contacts.domain.model.ContactNumber
import at.gdev.contacts.domain.model.ContactPatch
import at.gdev.contacts.domain.model.ContactSummary
import at.gdev.contacts.domain.model.ContactUrl
import at.gdev.contacts.domain.model.NamedRef
import at.gdev.contacts.domain.model.composeDisplayName
import at.gdev.contacts.domain.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultContactsRepository @Inject constructor(
    private val api: ContactsApi,
    private val dao: ContactsDao,
    private val json: Json,
) : ContactsRepository {

    private val cache = MutableStateFlow<List<ContactSummary>>(emptyList())

    /**
     * Remembered so post-mutation refreshes (create/update/delete) preserve any
     * active search filter the list screen is showing.
     */
    private var lastQuery: String? = null

    override val summaries: Flow<List<ContactSummary>> = cache.asStateFlow()

    override suspend fun refresh(query: String?): Result<Unit> = runCatching {
        val q = query?.takeIf { it.isNotBlank() }
        val all = fetchAllPages(q)
        cache.value = all.map { it.toDomain() }
        lastQuery = q
        // Only an unfiltered refresh is safe to treat as authoritative for Room —
        // a query-filtered result can't be used to prune contacts that don't match.
        if (q == null) persistSummariesToRoom(all)
    }.fold(
        onSuccess = { Result.success(Unit) },
        onFailure = { Result.failure(it.toDomainError(json)) },
    )

    private suspend fun refreshCacheSilently() {
        runCatching {
            val items = fetchAllPages(lastQuery)
            cache.value = items.map { it.toDomain() }
        }
    }

    private suspend fun persistSummariesToRoom(summaries: List<ContactSummaryDto>) {
        val now = System.currentTimeMillis()
        summaries.forEach { summary ->
            val contact = summary.toContactEntity(now)
            val numbers = summary.numbers.map { it.toNumberEntity(summary.ulid) }
            dao.replaceContactWithNumbers(contact, numbers)
        }
        dao.pruneContactsNotIn(summaries.map { it.ulid })
    }

    private suspend fun fetchAllPages(query: String?): List<ContactSummaryDto> {
        val q = query?.takeIf { it.isNotBlank() }
        val all = mutableListOf<ContactSummaryDto>()
        var page = 1
        while (true) {
            val response = api.list(query = q, page = page, perPage = SYNC_PAGE_SIZE)
            val items = response.contacts(json)
            all += items
            if (items.isEmpty() || page >= response.meta.lastPage) break
            page += 1
        }
        return all
    }

    override suspend fun getContact(id: String): Contact? = runCatching {
        api.show(id).data.toDomain()
    }.getOrNull()

    override suspend fun lookupByNumber(rawNumber: String): List<ContactLookup> {
        val digits = rawNumber.filter { it.isDigit() }
        if (digits.length < SUFFIX_MIN) return emptyList()
        val suffix = digits.takeLast(SUFFIX_LEN)

        val local = dao.lookupByDigitSuffix(suffix)
        if (local.isNotEmpty()) {
            return local.map {
                ContactLookup(
                    contactId = it.contactUlid,
                    fullName = it.contactFullName,
                    matchedNumber = it.number,
                    matchedLabel = it.name,
                    imageUrl = it.contactImageUrl,
                )
            }
        }

        return runCatching {
            val matches = api.byNumber(rawNumber).matches(json)
            if (matches.isNotEmpty()) cacheMatches(matches)
            matches.map {
                ContactLookup(
                    contactId = it.contactUlid,
                    fullName = it.fullname,
                    matchedNumber = it.matchedNumber.number,
                    matchedLabel = it.matchedNumber.name,
                    imageUrl = ApiConfig.normalizeImageUrl(it.imageUrl),
                )
            }
        }.getOrDefault(emptyList())
    }

    private suspend fun cacheMatches(matches: List<ContactByNumberMatch>) {
        val now = System.currentTimeMillis()
        matches.forEach { match ->
            val contact = ContactEntity(
                ulid = match.contactUlid,
                fullName = match.fullname,
                firstName = match.fullname.substringBefore(' '),
                lastName = match.fullname.substringAfter(' ', ""),
                company = null,
                imageUrl = ApiConfig.normalizeImageUrl(match.imageUrl),
                syncedAt = now,
            )
            val number = ContactNumberEntity(
                ulid = "match-${match.contactUlid}-${match.matchedNumber.number.hashCode()}",
                contactUlid = match.contactUlid,
                name = match.matchedNumber.name,
                number = match.matchedNumber.number,
                digits = match.matchedNumber.number.filter { it.isDigit() },
            )
            dao.upsertContacts(listOf(contact))
            dao.upsertNumbers(listOf(number))
        }
    }

    override suspend fun syncAll(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val summaries = fetchAllPages(query = null)
            persistSummariesToRoom(summaries)
            dao.contactCount()
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it.toDomainError(json)) },
        )
    }

    // ----- Contact base mutations -----

    override suspend fun createContact(patch: ContactPatch): Result<String> = runCatching {
        api.create(patch.toStoreRequest()).data.ulid
    }.fold(
        onSuccess = { ulid ->
            refreshCacheSilently()
            Result.success(ulid)
        },
        onFailure = { Result.failure(it.toDomainError(json)) },
    )

    override suspend fun updateContact(id: String, patch: ContactPatch): Result<Unit> {
        val result = mutate { api.update(id, patch.toRequest()) }
        if (result.isSuccess) refreshCacheSilently()
        return result
    }

    override suspend fun deleteContact(id: String): Result<Unit> {
        val result = mutate { ack(api.delete(id)) }
        if (result.isSuccess) refreshCacheSilently()
        return result
    }

    // ----- Numbers -----

    override suspend fun addNumber(contactId: String, name: String, number: String): Result<Unit> = mutate {
        api.createNumber(contactId, ContactNumberRequest(name, number))
    }

    override suspend fun updateNumber(contactId: String, numberId: String, name: String, number: String): Result<Unit> = mutate {
        api.updateNumber(contactId, numberId, ContactNumberRequest(name, number))
    }

    override suspend fun deleteNumber(contactId: String, numberId: String): Result<Unit> = mutate {
        ack(api.deleteNumber(contactId, numberId))
    }

    // ----- Emails -----

    override suspend fun addEmail(contactId: String, name: String, email: String): Result<Unit> = mutate {
        api.createEmail(contactId, ContactEmailRequest(name, email))
    }

    override suspend fun updateEmail(contactId: String, emailId: String, name: String, email: String): Result<Unit> = mutate {
        api.updateEmail(contactId, emailId, ContactEmailRequest(name, email))
    }

    override suspend fun deleteEmail(contactId: String, emailId: String): Result<Unit> = mutate {
        ack(api.deleteEmail(contactId, emailId))
    }

    // ----- URLs -----

    override suspend fun addUrl(contactId: String, name: String, url: String): Result<Unit> = mutate {
        api.createUrl(contactId, ContactUrlRequest(name, url))
    }

    override suspend fun updateUrl(contactId: String, urlId: String, name: String, url: String): Result<Unit> = mutate {
        api.updateUrl(contactId, urlId, ContactUrlRequest(name, url))
    }

    override suspend fun deleteUrl(contactId: String, urlId: String): Result<Unit> = mutate {
        ack(api.deleteUrl(contactId, urlId))
    }

    // ----- Notes -----

    override suspend fun addNote(contactId: String, name: String, note: String): Result<Unit> = mutate {
        api.createNote(contactId, ContactNoteRequest(name, note))
    }

    override suspend fun updateNote(contactId: String, noteId: String, name: String, note: String): Result<Unit> = mutate {
        api.updateNote(contactId, noteId, ContactNoteRequest(name, note))
    }

    override suspend fun deleteNote(contactId: String, noteId: String): Result<Unit> = mutate {
        ack(api.deleteNote(contactId, noteId))
    }

    // ----- Dates -----

    override suspend fun addDate(contactId: String, name: String, date: LocalDate, skipYear: Boolean): Result<Unit> = mutate {
        api.createDate(contactId, ContactDateRequest(name, date.formatIso(), skipYear))
    }

    override suspend fun updateDate(
        contactId: String,
        dateId: String,
        name: String,
        date: LocalDate,
        skipYear: Boolean,
    ): Result<Unit> = mutate {
        api.updateDate(contactId, dateId, ContactDateRequest(name, date.formatIso(), skipYear))
    }

    override suspend fun deleteDate(contactId: String, dateId: String): Result<Unit> = mutate {
        ack(api.deleteDate(contactId, dateId))
    }

    // ----- Addresses -----

    override suspend fun addAddress(
        contactId: String,
        name: String,
        street: String,
        zip: String,
        city: String,
        state: String?,
        countryId: Int,
    ): Result<Unit> = mutate {
        api.createAddress(contactId, ContactAddressRequest(name, street, zip, city, state, countryId))
    }

    override suspend fun updateAddress(
        contactId: String,
        addressId: String,
        name: String,
        street: String,
        zip: String,
        city: String,
        state: String?,
        countryId: Int,
    ): Result<Unit> = mutate {
        api.updateAddress(contactId, addressId, ContactAddressRequest(name, street, zip, city, state, countryId))
    }

    override suspend fun deleteAddress(contactId: String, addressId: String): Result<Unit> = mutate {
        ack(api.deleteAddress(contactId, addressId))
    }

    // ----- Calls -----

    override suspend fun addCall(contactId: String, calledAt: String, note: String?): Result<Unit> = mutate {
        api.createCall(contactId, ContactCallRequest(calledAt, note))
    }

    override suspend fun updateCall(contactId: String, callId: String, calledAt: String, note: String?): Result<Unit> = mutate {
        api.updateCall(contactId, callId, ContactCallRequest(calledAt, note))
    }

    override suspend fun deleteCall(contactId: String, callId: String): Result<Unit> = mutate {
        ack(api.deleteCall(contactId, callId))
    }

    // ----- Comments -----

    override suspend fun listComments(contactId: String): Result<List<ContactComment>> = runCatching {
        api.listComments(contactId).data.map { it.toDomain() }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(it.toDomainError(json)) },
    )

    override suspend fun addComment(contactId: String, text: String, parentId: String?): Result<Unit> = mutate {
        api.createComment(contactId, ContactCommentStoreRequest(comment = text, parentUlid = parentId))
    }

    override suspend fun updateComment(contactId: String, commentId: String, text: String): Result<Unit> = mutate {
        api.updateComment(contactId, commentId, ContactCommentUpdateRequest(comment = text))
    }

    override suspend fun deleteComment(contactId: String, commentId: String): Result<Unit> = mutate {
        ack(api.deleteComment(contactId, commentId))
    }

    private fun ContactCommentDto.toDomain(): ContactComment = ContactComment(
        id = ulid,
        parentId = parentUlid,
        text = comment,
        tombstoned = tombstoned,
        owner = owner?.let { NamedRef(it.id, it.name) },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ----- Gift ideas -----

    override suspend fun addGiftIdea(
        contactId: String,
        name: String,
        description: String?,
        url: String?,
        dueAt: LocalDate?,
    ): Result<Unit> = mutate {
        api.createGiftIdea(contactId, ContactGiftIdeaRequest(name, description, url, dueAt?.formatIso()))
    }

    override suspend fun updateGiftIdea(
        contactId: String,
        giftId: String,
        name: String,
        description: String?,
        url: String?,
        dueAt: LocalDate?,
    ): Result<Unit> = mutate {
        api.updateGiftIdea(contactId, giftId, ContactGiftIdeaRequest(name, description, url, dueAt?.formatIso()))
    }

    override suspend fun deleteGiftIdea(contactId: String, giftId: String): Result<Unit> = mutate {
        ack(api.deleteGiftIdea(contactId, giftId))
    }

    // ----- Helpers -----

    private suspend fun mutate(block: suspend () -> Unit): Result<Unit> =
        runCatching { block() }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it.toDomainError(json)) },
        )

    private fun ack(response: Response<Unit>) {
        if (!response.isSuccessful) throw HttpException(response)
    }

    private fun LocalDate.formatIso(): String = format(DateTimeFormatter.ISO_LOCAL_DATE)

    private fun ContactPatch.toStoreRequest(): ContactsStoreRequest = ContactsStoreRequest(
        salutation = salutation.orEmpty(),
        firstname = firstName.orEmpty(),
        lastname = lastName.orEmpty(),
        title = titleBefore,
        titleAfter = titleAfter,
        nickname = nickname,
        genderId = genderId,
        company = company,
        vatin = vatin,
        department = department,
        job = job,
        customId = customId,
        iban = iban,
        dateOfBirth = dateOfBirth?.formatIso(),
        diedAt = diedAt?.formatIso(),
        diedFrom = diedFrom,
        nationalityId = nationalityId,
        firstMet = firstMet,
        note = note,
        active = active,
        contactGroups = contactGroupIds,
    )

    private fun ContactPatch.toRequest(): ContactUpdateRequest = ContactUpdateRequest(
        salutation = salutation,
        firstname = firstName,
        lastname = lastName,
        title = titleBefore,
        titleAfter = titleAfter,
        nickname = nickname,
        genderId = genderId,
        company = company,
        vatin = vatin,
        department = department,
        job = job,
        customId = customId,
        iban = iban,
        dateOfBirth = dateOfBirth?.formatIso(),
        diedAt = diedAt?.formatIso(),
        diedFrom = diedFrom,
        nationalityId = nationalityId,
        firstMet = firstMet,
        note = note,
        active = active,
        contactGroups = contactGroupIds,
    )

    // ----- DTO -> domain mappers -----

    private fun ContactSummaryDto.toDomain(): ContactSummary = ContactSummary(
        id = ulid,
        firstName = firstname,
        lastName = lastname,
        fullName = fullname,
        titleBefore = title,
        titleAfter = titleAfter,
        nickname = nickname,
        salutation = salutation,
        company = company,
        imageUrl = ApiConfig.normalizeImageUrl(imageUrl),
    )

    private fun ContactDetailDto.toContactEntity(now: Long): ContactEntity = ContactEntity(
        ulid = ulid,
        fullName = composeDisplayName(title, firstname, lastname, titleAfter, nickname, fallback = fullname),
        firstName = firstname,
        lastName = lastname,
        company = company,
        imageUrl = ApiConfig.normalizeImageUrl(imageUrl),
        syncedAt = now,
    )

    private fun ContactSummaryDto.toContactEntity(now: Long): ContactEntity = ContactEntity(
        ulid = ulid,
        fullName = composeDisplayName(title, firstname, lastname, titleAfter, nickname, fallback = fullname),
        firstName = firstname,
        lastName = lastname,
        company = company,
        imageUrl = ApiConfig.normalizeImageUrl(imageUrl),
        syncedAt = now,
    )

    private fun ContactNumberDto.toNumberEntity(contactUlid: String): ContactNumberEntity =
        ContactNumberEntity(
            ulid = ulid,
            contactUlid = contactUlid,
            name = name,
            number = number,
            digits = number.filter { it.isDigit() },
        )

    private fun ContactSummaryNumberDto.toNumberEntity(contactUlid: String): ContactNumberEntity =
        ContactNumberEntity(
            ulid = ulid,
            contactUlid = contactUlid,
            name = name,
            number = number,
            digits = (e164 ?: number).filter { it.isDigit() },
        )

    private fun ContactDetailDto.toDomain(): Contact = Contact(
        id = ulid,
        firstName = firstname,
        lastName = lastname,
        fullName = fullname,
        nickname = nickname,
        titleBefore = title,
        titleAfter = titleAfter,
        salutation = salutation,
        imageUrl = ApiConfig.normalizeImageUrl(imageUrl),
        active = active,
        company = company,
        job = job,
        department = department,
        customId = customId,
        iban = iban,
        vatin = vatin,
        dateOfBirth = parseDate(dateOfBirth),
        diedAt = parseDate(diedAt),
        diedFrom = diedFrom,
        firstMet = firstMet,
        note = note,
        gender = gender?.toDomain(),
        nationality = nationality?.toDomain(),
        contactGroups = contactGroups.map { it.toDomain() },
        numbers = numbers.map { it.toDomain() },
        emails = emails.map { it.toDomain() },
        urls = urls.map { it.toDomain() },
        notes = notes.map { it.toDomain() },
        addresses = addresses.map { it.toDomain() },
        dates = dates.map { it.toDomain() },
        calls = calls.map { it.toDomain() },
        giftIdeas = giftIdeas.map { it.toDomain() },
    )

    private fun NamedRefDto.toDomain() = NamedRef(id = id, name = name)
    private fun ContactNumberDto.toDomain() = ContactNumber(ulid, name, number)
    private fun ContactEmailDto.toDomain() = ContactEmail(ulid, name, email)
    private fun ContactUrlDto.toDomain() = ContactUrl(ulid, name, url)
    private fun ContactNoteDto.toDomain() = ContactNote(ulid, name, note)
    private fun ContactAddressDto.toDomain() = ContactAddress(
        ulid, name, street, zip, city, state, country, latitude, longitude,
    )
    private fun ContactDateDto.toDomain() = ContactDate(ulid, name, parseDate(date), skipYear)
    private fun ContactCallDto.toDomain() = ContactCall(ulid, calledAt, note)
    private fun ContactGiftIdeaDto.toDomain() = ContactGiftIdea(
        id = ulid, name = name, description = description, url = url, dueAt = parseDate(dueAt),
    )

    private fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        val head = raw.substringBefore('T').substringBefore(' ').take(10)
        return try {
            LocalDate.parse(head, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val SYNC_PAGE_SIZE = 100
        const val SUFFIX_LEN = 9
        const val SUFFIX_MIN = 7
    }
}
