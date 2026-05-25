package at.gdev.contacts.data.repository

import at.gdev.contacts.data.network.ReferenceApi
import at.gdev.contacts.data.network.dto.ReferenceItemDto
import at.gdev.contacts.domain.model.NamedRef
import at.gdev.contacts.domain.repository.ReferenceRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultReferenceRepository @Inject constructor(
    private val api: ReferenceApi,
) : ReferenceRepository {

    private val gendersMutex = Mutex()
    private val countriesMutex = Mutex()
    private val groupsMutex = Mutex()

    private var gendersCache: List<NamedRef>? = null
    private var countriesCache: List<NamedRef>? = null
    private var groupsCache: List<NamedRef>? = null

    override suspend fun genders(): List<NamedRef> = gendersMutex.withLock {
        gendersCache ?: api.genders().data.map { it.toDomain() }.also { gendersCache = it }
    }

    override suspend fun countries(): List<NamedRef> = countriesMutex.withLock {
        countriesCache ?: api.countries().data.map { it.toDomain() }.also { countriesCache = it }
    }

    override suspend fun contactGroups(): List<NamedRef> = groupsMutex.withLock {
        groupsCache ?: api.contactGroups().data.map { it.toDomain() }.also { groupsCache = it }
    }

    private fun ReferenceItemDto.toDomain() = NamedRef(id = id, name = name)
}
