package at.gdev.contacts.data.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Datetime handling against the API. Time-bearing datetimes are UTC on the wire:
 * the client localizes them for display/editing and sends UTC back. This covers
 * both Laravel-managed timestamps (comment `created_at`/`updated_at`, returned as
 * `2026-06-08T17:49:49+00:00`) and `called_at`.
 *
 * Date-only values (birthdays, important dates, all-day events) are timezone-
 * agnostic `yyyy-MM-dd` and don't go through here.
 */
object DateTimes {

    private val API_OUT_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    /** Parse a UTC/offset API datetime (e.g. `...Z` or `...+00:00`) to local
     *  wall-clock time; null if absent or unparseable. */
    fun instantToLocal(value: String?): LocalDateTime? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val zone = ZoneId.systemDefault()
        runCatching { return Instant.parse(raw).atZone(zone).toLocalDateTime() }
        runCatching { return OffsetDateTime.parse(raw).atZoneSameInstant(zone).toLocalDateTime() }
        return null
    }

    /** Convert a local datetime to the UTC string the API expects ("yyyy-MM-ddTHH:mm:ssZ"). */
    fun localToApiUtc(local: LocalDateTime): String =
        local.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).format(API_OUT_UTC)
}
