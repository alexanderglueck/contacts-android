package at.gdev.contacts.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.gdev.contacts.domain.model.CalendarEvent
import at.gdev.contacts.domain.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class CalendarMode { Upcoming, Month }

data class CalendarUiState(
    val mode: CalendarMode = CalendarMode.Upcoming,

    // Upcoming list
    val upcomingLoading: Boolean = true,
    val upcoming: List<CalendarEvent> = emptyList(),
    val upcomingError: String? = null,

    // Month grid
    val month: YearMonth = YearMonth.now(),
    val monthLoading: Boolean = false,
    val monthEvents: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val monthError: String? = null,
    val selectedDay: LocalDate = LocalDate.now(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        loadUpcoming()
    }

    fun setMode(mode: CalendarMode) {
        _state.update { it.copy(mode = mode) }
        if (mode == CalendarMode.Month && _state.value.monthEvents.isEmpty()) {
            loadMonth(_state.value.month)
        }
    }

    fun setMonth(month: YearMonth) {
        if (_state.value.month == month) return
        _state.update { it.copy(month = month) }
        loadMonth(month)
    }

    fun selectDay(date: LocalDate) {
        _state.update { it.copy(selectedDay = date) }
    }

    fun refresh() {
        when (_state.value.mode) {
            CalendarMode.Upcoming -> loadUpcoming()
            CalendarMode.Month -> loadMonth(_state.value.month)
        }
    }

    private fun loadUpcoming() {
        viewModelScope.launch {
            _state.update { it.copy(upcomingLoading = true, upcomingError = null) }
            repository.upcoming()
                .onSuccess { events ->
                    _state.update {
                        it.copy(upcomingLoading = false, upcoming = events.sortedBy { e -> e.date })
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            upcomingLoading = false,
                            upcomingError = err.message ?: "Failed to load",
                        )
                    }
                }
        }
    }

    private fun loadMonth(month: YearMonth) {
        viewModelScope.launch {
            _state.update { it.copy(monthLoading = true, monthError = null) }
            val from = month.atDay(1)
            val to = month.atEndOfMonth()
            repository.events(from, to)
                .onSuccess { events ->
                    _state.update {
                        it.copy(
                            monthLoading = false,
                            monthEvents = events.groupBy { e -> e.date },
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            monthLoading = false,
                            monthError = err.message ?: "Failed to load month",
                        )
                    }
                }
        }
    }
}
