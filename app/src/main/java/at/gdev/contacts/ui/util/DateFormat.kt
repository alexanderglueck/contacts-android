package at.gdev.contacts.ui.util

import android.content.Context
import android.text.format.DateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * Date/time formatting that follows the user's device settings — locale ordering
 * and the 12/24-hour preference — via [android.text.format.DateFormat], so the
 * app reads natively for whoever uses it. Timezone/parsing lives in
 * `data.util.DateTimes`; these take already-local values and only format.
 */

private fun LocalDateTime.toJavaDate(): Date = Date.from(atZone(ZoneId.systemDefault()).toInstant())
private fun LocalDate.toJavaDate(): Date = Date.from(atStartOfDay(ZoneId.systemDefault()).toInstant())

/** Short date in the device's locale/format, e.g. "08.06.2026" (AT) or "6/8/26" (US). "—" if null. */
fun Context.formatDate(date: LocalDate?): String =
    date?.let { DateFormat.getDateFormat(this).format(it.toJavaDate()) } ?: "—"

/** Time honoring the device 12/24-hour setting, e.g. "20:00" or "8:00 PM". "—" if null. */
fun Context.formatTime(time: LocalTime?): String =
    time?.let { DateFormat.getTimeFormat(this).format(it.atDate(LocalDate.now()).toJavaDate()) } ?: "—"

/** Date + time in the device's locale and 12/24-hour setting. "—" if null. */
fun Context.formatDateTime(dateTime: LocalDateTime?): String = dateTime?.let {
    val d = it.toJavaDate()
    "${DateFormat.getDateFormat(this).format(d)} ${DateFormat.getTimeFormat(this).format(d)}"
} ?: "—"

/** Localized month + day without a year (for recurring/skip-year dates), e.g. "8. Juni" / "June 8". */
fun Context.formatMonthDay(date: LocalDate?): String {
    if (date == null) return "—"
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, "MMMMd")
    return java.text.SimpleDateFormat(pattern, locale).format(date.toJavaDate())
}
