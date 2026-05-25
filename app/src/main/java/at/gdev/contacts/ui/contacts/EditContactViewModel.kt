package at.gdev.contacts.ui.contacts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.gdev.contacts.data.network.ValidationException
import at.gdev.contacts.domain.model.Contact
import at.gdev.contacts.domain.model.ContactPatch
import at.gdev.contacts.domain.model.NamedRef
import at.gdev.contacts.domain.repository.ContactsRepository
import at.gdev.contacts.domain.repository.ReferenceRepository
import at.gdev.contacts.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class EditContactUiState(
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),

    val genders: List<NamedRef> = emptyList(),
    val nationalities: List<NamedRef> = emptyList(),
    val contactGroups: List<NamedRef> = emptyList(),

    val salutation: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val titleBefore: String = "",
    val titleAfter: String = "",
    val nickname: String = "",
    val genderId: Int? = null,
    val nationalityId: Int? = null,
    val selectedGroupIds: Set<Int> = emptySet(),
    val company: String = "",
    val job: String = "",
    val department: String = "",
    val customId: String = "",
    val iban: String = "",
    val vatin: String = "",
    val dateOfBirth: LocalDate? = null,
    val firstMet: String = "",
    val diedAt: LocalDate? = null,
    val diedFrom: String = "",
    val note: String = "",
    val active: Boolean = true,
) {
    val canSave: Boolean
        get() = !loading && !submitting &&
                salutation.isNotBlank() &&
                firstName.isNotBlank() &&
                lastName.isNotBlank()
}

@HiltViewModel
class EditContactViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contactsRepository: ContactsRepository,
    private val referenceRepository: ReferenceRepository,
) : ViewModel() {

    private val contactId: String = checkNotNull(savedStateHandle[Routes.ARG_CONTACT_ID])

    private val _state = MutableStateFlow(EditContactUiState())
    val state: StateFlow<EditContactUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val contact = contactsRepository.getContact(contactId)
            val genders = runCatching { referenceRepository.genders() }.getOrDefault(emptyList())
            val countries = runCatching { referenceRepository.countries() }.getOrDefault(emptyList())
            val groups = runCatching { referenceRepository.contactGroups() }.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    loading = false,
                    genders = genders,
                    nationalities = countries,
                    contactGroups = groups,
                ).populateFrom(contact)
            }
        }
    }

    private fun EditContactUiState.populateFrom(contact: Contact?): EditContactUiState {
        if (contact == null) return this.copy(error = "Contact not found")
        return copy(
            salutation = contact.salutation.orEmpty(),
            firstName = contact.firstName,
            lastName = contact.lastName,
            titleBefore = contact.titleBefore.orEmpty(),
            titleAfter = contact.titleAfter.orEmpty(),
            nickname = contact.nickname.orEmpty(),
            genderId = contact.gender?.id,
            nationalityId = contact.nationality?.id,
            selectedGroupIds = contact.contactGroups.map { it.id }.toSet(),
            company = contact.company.orEmpty(),
            job = contact.job.orEmpty(),
            department = contact.department.orEmpty(),
            customId = contact.customId.orEmpty(),
            iban = contact.iban.orEmpty(),
            vatin = contact.vatin.orEmpty(),
            dateOfBirth = contact.dateOfBirth,
            firstMet = contact.firstMet.orEmpty(),
            diedAt = contact.diedAt,
            diedFrom = contact.diedFrom.orEmpty(),
            note = contact.note.orEmpty(),
            active = contact.active,
        )
    }

    fun setSalutation(v: String) = _state.update { it.copy(salutation = v).clearErrors() }
    fun setFirstName(v: String) = _state.update { it.copy(firstName = v).clearErrors() }
    fun setLastName(v: String) = _state.update { it.copy(lastName = v).clearErrors() }
    fun setTitleBefore(v: String) = _state.update { it.copy(titleBefore = v).clearErrors() }
    fun setTitleAfter(v: String) = _state.update { it.copy(titleAfter = v).clearErrors() }
    fun setNickname(v: String) = _state.update { it.copy(nickname = v).clearErrors() }
    fun setGender(id: Int?) = _state.update { it.copy(genderId = id).clearErrors() }
    fun setNationality(id: Int?) = _state.update { it.copy(nationalityId = id).clearErrors() }
    fun toggleGroup(id: Int) = _state.update {
        val next = it.selectedGroupIds.toMutableSet().apply { if (!add(id)) remove(id) }
        it.copy(selectedGroupIds = next).clearErrors()
    }
    fun setCompany(v: String) = _state.update { it.copy(company = v).clearErrors() }
    fun setJob(v: String) = _state.update { it.copy(job = v).clearErrors() }
    fun setDepartment(v: String) = _state.update { it.copy(department = v).clearErrors() }
    fun setCustomId(v: String) = _state.update { it.copy(customId = v).clearErrors() }
    fun setIban(v: String) = _state.update { it.copy(iban = v).clearErrors() }
    fun setVatin(v: String) = _state.update { it.copy(vatin = v).clearErrors() }
    fun setDateOfBirth(v: LocalDate?) = _state.update { it.copy(dateOfBirth = v).clearErrors() }
    fun setFirstMet(v: String) = _state.update { it.copy(firstMet = v).clearErrors() }
    fun setDiedAt(v: LocalDate?) = _state.update { it.copy(diedAt = v).clearErrors() }
    fun setDiedFrom(v: String) = _state.update { it.copy(diedFrom = v).clearErrors() }
    fun setNote(v: String) = _state.update { it.copy(note = v).clearErrors() }
    fun setActive(v: Boolean) = _state.update { it.copy(active = v).clearErrors() }

    private fun EditContactUiState.clearErrors(): EditContactUiState =
        if (error == null && fieldErrors.isEmpty()) this
        else copy(error = null, fieldErrors = emptyMap())

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        _state.update { it.copy(submitting = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val patch = ContactPatch(
                salutation = s.salutation.trim(),
                firstName = s.firstName.trim(),
                lastName = s.lastName.trim(),
                titleBefore = s.titleBefore.trim().ifBlank { null },
                titleAfter = s.titleAfter.trim().ifBlank { null },
                nickname = s.nickname.trim().ifBlank { null },
                genderId = s.genderId,
                company = s.company.trim().ifBlank { null },
                vatin = s.vatin.trim().ifBlank { null },
                department = s.department.trim().ifBlank { null },
                job = s.job.trim().ifBlank { null },
                customId = s.customId.trim().ifBlank { null },
                iban = s.iban.trim().ifBlank { null },
                dateOfBirth = s.dateOfBirth,
                diedAt = s.diedAt,
                diedFrom = s.diedFrom.trim().ifBlank { null },
                nationalityId = s.nationalityId,
                firstMet = s.firstMet.trim().ifBlank { null },
                note = s.note.trim().ifBlank { null },
                active = s.active,
                contactGroupIds = s.selectedGroupIds.toList(),
            )
            val result = contactsRepository.updateContact(contactId, patch)
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(submitting = false, saved = true) },
                    onFailure = { err ->
                        val fieldErrors = (err as? ValidationException)?.errors
                            ?.mapValues { it.value.joinToString(" ") }
                            ?: emptyMap()
                        current.copy(
                            submitting = false,
                            error = err.message ?: "Failed to update contact",
                            fieldErrors = fieldErrors,
                        )
                    },
                )
            }
        }
    }
}
