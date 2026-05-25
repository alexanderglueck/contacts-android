package at.gdev.contacts.domain.repository

import at.gdev.contacts.domain.model.NamedRef

interface ReferenceRepository {
    suspend fun genders(): List<NamedRef>
    suspend fun countries(): List<NamedRef>
    suspend fun contactGroups(): List<NamedRef>
}
