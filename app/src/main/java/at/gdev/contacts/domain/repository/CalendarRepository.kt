package at.gdev.contacts.domain.repository

import at.gdev.contacts.domain.model.CalendarEvent
import java.time.LocalDate

interface CalendarRepository {
    suspend fun upcoming(): Result<List<CalendarEvent>>
    suspend fun events(from: LocalDate, to: LocalDate): Result<List<CalendarEvent>>
}
