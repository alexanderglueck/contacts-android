package at.gdev.contacts.data.network

import at.gdev.contacts.data.network.dto.CalendarEventsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CalendarApi {
    /** Window query for the agenda/month views. Both params are ISO dates (YYYY-MM-DD). */
    @GET("calendar/events")
    suspend fun events(
        @Query("from") from: String,
        @Query("to") to: String,
    ): CalendarEventsResponse
}
