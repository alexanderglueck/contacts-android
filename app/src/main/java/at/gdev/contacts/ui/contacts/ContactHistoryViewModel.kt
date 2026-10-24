package at.gdev.contacts.ui.contacts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.gdev.contacts.data.network.ForbiddenException
import at.gdev.contacts.domain.model.ContactActivity
import at.gdev.contacts.domain.repository.ContactsRepository
import at.gdev.contacts.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactHistoryUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val items: List<ContactActivity> = emptyList(),
    val hasMore: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ContactHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ContactsRepository,
) : ViewModel() {

    private val contactId: String = checkNotNull(savedStateHandle[Routes.ARG_CONTACT_ID])

    private val _state = MutableStateFlow(ContactHistoryUiState())
    val state: StateFlow<ContactHistoryUiState> = _state.asStateFlow()

    private var page = 1

    init {
        loadFirstPage(pulled = false)
    }

    fun pullToRefresh() {
        if (_state.value.refreshing) return
        loadFirstPage(pulled = true)
    }

    fun loadMore() {
        val s = _state.value
        if (!s.hasMore || s.loadingMore || s.loading || s.refreshing) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            repository.listActivities(contactId, page + 1)
                .onSuccess { result ->
                    page += 1
                    _state.update {
                        it.copy(loadingMore = false, items = it.items + result.items, hasMore = result.hasMore)
                    }
                }
                .onFailure { e -> _state.update { it.copy(loadingMore = false, error = e.toMessage()) } }
        }
    }

    private fun loadFirstPage(pulled: Boolean) {
        _state.update { it.copy(loading = !pulled && it.items.isEmpty(), refreshing = pulled, error = null) }
        viewModelScope.launch {
            repository.listActivities(contactId, 1)
                .onSuccess { result ->
                    page = 1
                    _state.update {
                        it.copy(loading = false, refreshing = false, items = result.items, hasMore = result.hasMore)
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, refreshing = false, error = e.toMessage()) }
                }
        }
    }

    private fun Throwable.toMessage(): String = when (this) {
        is ForbiddenException -> "You don't have permission to view the history."
        else -> message ?: "Could not load the history"
    }
}
