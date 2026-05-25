package at.gdev.contacts.data.network

import at.gdev.contacts.data.network.dto.CalendarEventsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CalendarApi {
    /** Today + next N days, pre-sorted by date. Single round trip for the list view. */
    @GET("calendar/upcoming")
    suspend fun upcoming(): CalendarEventsResponse

    /** Window query for a future month/agenda view. Both params are ISO dates (YYYY-MM-DD). */
    @GET("calendar/events")
    suspend fun events(
        @Query("from") from: String,
        @Query("to") to: String,
    ): CalendarEventsResponse
}
