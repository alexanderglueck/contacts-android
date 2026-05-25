package at.gdev.contacts.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.gdev.contacts.data.prefs.ContactListPreferencesStore
import at.gdev.contacts.domain.model.ContactSort
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
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

/** A contiguous run of contacts that share a header letter. */
data class ContactSection(
    val letter: String,
    val contacts: List<ContactSummary>,
)

data class ContactsListUiState(
    val query: String = "",
    val sort: ContactSort = ContactSort.LastName,
    val sections: List<ContactSection> = emptyList(),
    val totalCount: Int = 0,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ContactsListViewModel @Inject constructor(
    private val repository: ContactsRepository,
    private val preferences: ContactListPreferencesStore,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(false)
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<ContactsListUiState> = combine(
        combine(repository.summaries, query, preferences.sort) { contacts, q, sort -> Triple(contacts, q, sort) },
        loading,
        refreshing,
        error,
    ) { (contacts, q, sort), isLoading, isRefreshing, err ->
        val sections = groupAndSort(contacts, sort)
        ContactsListUiState(
            query = q,
            sort = sort,
            sections = sections,
            totalCount = contacts.size,
            loading = isLoading,
            refreshing = isRefreshing,
            error = err,
        )
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

    fun setSort(value: ContactSort) {
        viewModelScope.launch { preferences.setSort(value) }
    }

    fun retry() = refresh(query.value)

    fun pullToRefresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            error.value = null
            val result = repository.refresh(query.value)
            refreshing.value = false
            result.onFailure { error.value = it.message ?: "Failed to refresh contacts" }
        }
    }

    private fun refresh(q: String) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            val result = repository.refresh(q)
            loading.value = false
            result.onFailure { error.value = it.message ?: "Failed to load contacts" }
        }
    }

    private fun groupAndSort(
        contacts: List<ContactSummary>,
        sort: ContactSort,
    ): List<ContactSection> {
        if (contacts.isEmpty()) return emptyList()
        val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
        val primary: (ContactSummary) -> String = when (sort) {
            ContactSort.FirstName -> { c -> c.firstName.ifBlank { c.fullName } }
            ContactSort.LastName -> { c -> c.lastName.ifBlank { c.fullName } }
        }
        val secondary: (ContactSummary) -> String = when (sort) {
            ContactSort.FirstName -> { c -> c.lastName }
            ContactSort.LastName -> { c -> c.firstName }
        }

        val comparator = Comparator<ContactSummary> { a, b ->
            val p = collator.compare(primary(a), primary(b))
            if (p != 0) return@Comparator p
            val s = collator.compare(secondary(a), secondary(b))
            if (s != 0) return@Comparator s
            collator.compare(a.fullName, b.fullName)
        }
        val sorted = contacts.sortedWith(comparator)

        val out = mutableListOf<ContactSection>()
        var currentLetter: String? = null
        var bucket = mutableListOf<ContactSummary>()
        for (c in sorted) {
            val letter = headerLetterFor(primary(c))
            if (letter != currentLetter) {
                if (bucket.isNotEmpty()) out += ContactSection(currentLetter!!, bucket)
                bucket = mutableListOf()
                currentLetter = letter
            }
            bucket += c
        }
        if (bucket.isNotEmpty() && currentLetter != null) out += ContactSection(currentLetter, bucket)
        return out
    }

    private fun headerLetterFor(value: String): String {
        val first = value.trim().firstOrNull() ?: return "#"
        return if (first.isLetter()) first.uppercaseChar().toString() else "#"
    }
}
