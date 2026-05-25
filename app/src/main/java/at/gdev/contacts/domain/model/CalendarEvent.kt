package at.gdev.contacts.domain.model

import java.time.LocalDate

data class CalendarEvent(
    /** "birthday", "anniversary", or any custom date label the backend exposes. */
    val type: String,
    val date: LocalDate,
    val title: String?,
    val years: Int?,
    val contactId: String?,
    val contactName: String?,
    val contactImageUrl: String?,
)
