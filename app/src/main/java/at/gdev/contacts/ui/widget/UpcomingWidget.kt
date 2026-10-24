package at.gdev.contacts.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import at.gdev.contacts.data.network.UnauthenticatedException
import at.gdev.contacts.domain.model.CalendarEvent
import at.gdev.contacts.domain.repository.CalendarRepository
import at.gdev.contacts.ui.MainActivity
import at.gdev.contacts.ui.calendar.displayTitle
import at.gdev.contacts.ui.calendar.whenText
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate

/**
 * Home screen widget with the next birthdays and important dates. It reads the
 * same calendar range as the app's "Upcoming" view; tapping it opens that view.
 * It refreshes on the system's widget update period and after the daily sync.
 */
class UpcomingWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun calendarRepository(): CalendarRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .calendarRepository()
        val today = LocalDate.now()
        val result = repository.events(today, today.plusDays(WINDOW_DAYS))
        val events = result.getOrNull()?.sortedBy { it.date }?.take(MAX_EVENTS)
        val message = when {
            result.exceptionOrNull() is UnauthenticatedException -> "Sign in to see upcoming dates"
            events == null -> "Could not load. Tap to open the app."
            events.isEmpty() -> "Nothing in the next $WINDOW_DAYS days"
            else -> null
        }

        provideContent {
            GlanceTheme {
                WidgetContent(context, today, events.orEmpty(), message)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, today: LocalDate, events: List<CalendarEvent>, message: String?) {
        val openCalendar = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_CALENDAR, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(openCalendar)),
        ) {
            Text(
                "Upcoming",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                modifier = GlanceModifier.padding(bottom = 6.dp),
            )
            if (message != null) {
                Text(message, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
            } else {
                LazyColumn {
                    items(events) { event ->
                        Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(
                                displayTitle(event),
                                maxLines = 1,
                                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
                            )
                            Text(
                                whenText(context, event.date, today),
                                maxLines = 1,
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                            )
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val WINDOW_DAYS = 30L
        const val MAX_EVENTS = 8
    }
}

class UpcomingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingWidget()
}
