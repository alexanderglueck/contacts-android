package at.gdev.contacts.data.repository

import at.gdev.contacts.data.network.CalendarApi
import at.gdev.contacts.data.network.dto.CalendarEventDto
import at.gdev.contacts.data.network.toDomainError
import at.gdev.contacts.domain.model.CalendarEvent
import at.gdev.contacts.domain.repository.CalendarRepository
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCalendarRepository @Inject constructor(
    private val api: CalendarApi,
    private val json: Json,
) : CalendarRepository {

    override suspend fun upcoming(): Result<List<CalendarEvent>> = runCatching {
        api.upcoming().data.mapNotNull { it.toDomain() }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(it.toDomainError(json)) },
    )

    override suspend fun events(from: LocalDate, to: LocalDate): Result<List<CalendarEvent>> = runCatching {
        api.events(from.format(ISO), to.format(ISO)).data.mapNotNull { it.toDomain() }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(it.toDomainError(json)) },
    )

    private fun CalendarEventDto.toDomain(): CalendarEvent? {
        val parsedDate = date?.let { runCatching { LocalDate.parse(it.take(10), ISO) }.getOrNull() } ?: return null
        return CalendarEvent(
            type = type ?: "event",
            date = parsedDate,
            title = title ?: name,
            years = years,
            contactId = contact?.ulid,
            contactName = contact?.fullname,
            contactImageUrl = contact?.imageUrl,
        )
    }

    private companion object {
        val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
