package at.gdev.contacts.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import at.gdev.contacts.domain.model.CalendarEvent
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onContactClick: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val today = remember { LocalDate.now() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ModeToggle(
                mode = state.mode,
                onSelect = viewModel::setMode,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when (state.mode) {
                CalendarMode.Upcoming -> UpcomingView(
                    state = state,
                    today = today,
                    onRetry = viewModel::refresh,
                    onContactClick = onContactClick,
                )

                CalendarMode.Month -> MonthView(
                    state = state,
                    today = today,
                    onMonthChange = viewModel::setMonth,
                    onSelectDay = viewModel::selectDay,
                    onContactClick = onContactClick,
                    onRetry = viewModel::refresh,
                )
            }
        }
    }
}

// ----- Mode toggle -----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeToggle(
    mode: CalendarMode,
    onSelect: (CalendarMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        val options = listOf(CalendarMode.Upcoming to "Upcoming", CalendarMode.Month to "Month")
        options.forEachIndexed { index, (m, label) ->
            SegmentedButton(
                selected = mode == m,
                onClick = { onSelect(m) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) { Text(label) }
        }
    }
}

// ----- Upcoming list -----

@Composable
private fun UpcomingView(
    state: CalendarUiState,
    today: LocalDate,
    onRetry: () -> Unit,
    onContactClick: (String) -> Unit,
) {
    when {
        state.upcomingLoading && state.upcoming.isEmpty() -> Centered { CircularProgressIndicator() }
        state.upcomingError != null && state.upcoming.isEmpty() -> ErrorBlock(state.upcomingError, onRetry)
        state.upcoming.isEmpty() -> Centered { Text("Nothing coming up.", style = MaterialTheme.typography.bodyMedium) }
        else -> {
            val grouped = state.upcoming.groupBy { bucketFor(it.date, today) }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (bucket, items) ->
                    item(key = "h-$bucket") { BucketHeader(bucket) }
                    items(items, key = { e -> "${e.contactId}-${e.type}-${e.date}-${e.title}" }) { event ->
                        EventRow(event, today) { event.contactId?.let(onContactClick) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// ----- Month grid -----

@Composable
private fun MonthView(
    state: CalendarUiState,
    today: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onContactClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val startMonth = remember { YearMonth.now().minusYears(2) }
    val endMonth = remember { YearMonth.now().plusYears(2) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = state.month,
        firstDayOfWeek = firstDayOfWeek,
    )

    // Sync grid scroll → VM state. The grid's "first visible month" is the source of truth while the user pages.
    LaunchedEffect(calendarState.firstVisibleMonth.yearMonth) {
        onMonthChange(calendarState.firstVisibleMonth.yearMonth)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MonthHeader(
            month = state.month,
            onPrev = {
                state.month.minusMonths(1).let { onMonthChange(it) }
            },
            onNext = {
                state.month.plusMonths(1).let { onMonthChange(it) }
            },
            onJumpToday = {
                onMonthChange(YearMonth.from(today))
                onSelectDay(today)
            },
        )
        WeekdayRow(firstDayOfWeek)

        if (state.monthLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                DayCell(
                    day = day,
                    today = today,
                    selected = state.selectedDay == day.date,
                    hasEvents = state.monthEvents[day.date]?.isNotEmpty() == true,
                    onClick = {
                        if (day.position == DayPosition.MonthDate) onSelectDay(day.date)
                    },
                )
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SelectedDayList(
            selected = state.selectedDay,
            events = state.monthEvents[state.selectedDay].orEmpty(),
            today = today,
            error = state.monthError,
            onRetry = onRetry,
            onContactClick = onContactClick,
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onJumpToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
        }
        Text(
            text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onJumpToday) {
            Icon(Icons.Filled.Today, contentDescription = "Today")
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun WeekdayRow(firstDayOfWeek: DayOfWeek) {
    val days = remember(firstDayOfWeek) { daysOfWeek(firstDayOfWeek = firstDayOfWeek) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        days.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    today: LocalDate,
    selected: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit,
) {
    val inMonth = day.position == DayPosition.MonthDate
    val isToday = day.date == today
    val baseColor = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        !inMonth -> MaterialTheme.colorScheme.outline
        selected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(baseColor)
            .clickable(enabled = inMonth, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (inMonth && hasEvents) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        ),
                )
            }
        }
    }
}

@Composable
private fun SelectedDayList(
    selected: LocalDate,
    events: List<CalendarEvent>,
    today: LocalDate,
    error: String?,
    onRetry: () -> Unit,
    onContactClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = whenText(selected, today),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        when {
            error != null && events.isEmpty() -> ErrorBlock(error, onRetry)
            events.isEmpty() -> Text(
                "Nothing on this day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(events, key = { e -> "${e.contactId}-${e.type}-${e.date}-${e.title}" }) { event ->
                    EventRow(event, today) { event.contactId?.let(onContactClick) }
                    HorizontalDivider()
                }
            }
        }
    }
}

// ----- Shared bits -----

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun BucketHeader(label: String) {
    Text(
        label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun EventRow(event: CalendarEvent, today: LocalDate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = event.contactId != null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EventIcon(event.type)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                displayTitle(event),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                whenText(event.date, today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun EventIcon(type: String) {
    val icon = if (type.equals("birthday", ignoreCase = true)) Icons.Filled.Cake else Icons.Filled.Event
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(40.dp).clip(CircleShape),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

// ----- Helpers -----

private fun bucketFor(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days <= 0L -> "Today"
        days == 1L -> "Tomorrow"
        days in 2..7 -> "This week"
        days in 8..31 -> "This month"
        else -> "Later"
    }
}

private fun whenText(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val pretty = date.format(DATE_FORMAT)
    return when {
        days == 0L -> "Today · $pretty"
        days == 1L -> "Tomorrow · $pretty"
        days in 2..6 -> "$weekday · $pretty"
        else -> pretty
    }
}

private fun displayTitle(event: CalendarEvent): String {
    val name = event.contactName ?: "Contact"
    return if (event.type.equals("birthday", ignoreCase = true)) {
        val years = event.years
        if (years != null && years > 0) "$name's ${ordinal(years)} birthday" else "$name's birthday"
    } else {
        val label = event.title?.takeIf { it.isNotBlank() } ?: event.type
        "$name · $label"
    }
}

private fun ordinal(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    val suffix = when {
        mod100 in 11..13 -> "th"
        mod10 == 1 -> "st"
        mod10 == 2 -> "nd"
        mod10 == 3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
