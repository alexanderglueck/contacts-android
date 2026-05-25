package at.gdev.contacts.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.gdev.contacts.domain.model.ContactSummary
import at.gdev.contacts.domain.repository.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsListUiState(
    val query: String = "",
    val contacts: List<ContactSummary> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ContactsListViewModel @Inject constructor(
    private val repository: ContactsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<ContactsListUiState> = combine(
        repository.summaries,
        query,
        loading,
        error,
    ) { contacts, q, isLoading, err ->
        ContactsListUiState(query = q, contacts = contacts, loading = isLoading, error = err)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ContactsListUiState())

    init {
        refresh(query.value)
        query
            .debounce(300)
            .distinctUntilChanged()
            .onEach { refresh(it) }
            .launchIn(viewModelScope)
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun retry() = refresh(query.value)

    private fun refresh(q: String) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            val result = repository.refresh(q)
            loading.value = false
            result.onFailure { error.value = it.message ?: "Failed to load contacts" }
        }
    }
}
