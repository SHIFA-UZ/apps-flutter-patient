package shifa.uz.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private fun Int.twoDigits() = toString().padStart(2, '0')

private fun LocalDateTime.hhmm() = "${hour.twoDigits()}:${minute.twoDigits()}"

/** 15 §1.7: ISO-8601, with an optional trailing `[Zone/Id]`. No offset is read as local time, like Dart. */
fun parseInstant(value: String?): Instant? {
    val iso = value?.substringBefore('[')?.trim()?.ifEmpty { null } ?: return null
    return runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(iso).toInstant(TimeZone.currentSystemDefault()) }.getOrNull()
}

/** A `yyyy-MM-dd` or an instant as a local date; null when it's neither. */
fun parseDay(
    value: String,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): LocalDate? =
    runCatching { LocalDate.parse(value.trim()) }.getOrNull() ?: parseInstant(value)?.toLocalDateTime(zone)?.date

/** dd.MM.yyyy */
fun formatDate(date: LocalDate) = "${date.dayOfMonth.twoDigits()}.${date.monthNumber.twoDigits()}.${date.year}"

/** A `yyyy-MM-dd` or an instant (in local time, 11 quirk 15) as dd.MM.yyyy. Anything else is shown as sent. */
fun formatDay(
    value: String,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val date = parseDay(value, zone) ?: return value
    return formatDate(date)
}

/**
 * 07 next-slot text: "{today} · HH:mm", "{tomorrow} · HH:mm", else "dd.MM · HH:mm". Flutter showed only a
 * weekday there, which can't tell this week from next (07 quirk 18).
 */
fun formatSlot(
    at: Instant,
    now: Instant,
    today: String,
    tomorrow: String,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val local = at.toLocalDateTime(zone)
    val days = now.toLocalDateTime(zone).date.daysUntil(local.date)
    val day =
        when (days) {
            0 -> today
            1 -> tomorrow
            else -> "${local.dayOfMonth.twoDigits()}.${local.monthNumber.twoDigits()}"
        }
    return "$day · ${local.hhmm()}"
}

/** Figma past-appointment card: "HH:mm, d MMM". [months] are the 12 short names. */
fun formatTimeDay(
    at: Instant,
    months: List<String>,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val local = at.toLocalDateTime(zone)
    return "${local.hhmm()}, ${local.dayOfMonth} ${months[local.monthNumber - 1]}"
}

/** 06 §4.3: "EEEE HH:mm - HH:mm, dd MMMM yyyy" in local time; without an end, just the start time. */
fun formatVisit(
    start: Instant,
    end: Instant?,
    weekdays: List<String>,
    months: List<String>,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val local = start.toLocalDateTime(zone)
    val until = end?.let { " - ${it.toLocalDateTime(zone).hhmm()}" }.orEmpty()
    return "${weekdays[local.dayOfWeek.isoDayNumber - 1]} ${local.hhmm()}$until, " +
        "${local.dayOfMonth.twoDigits()} ${months[local.monthNumber - 1]} ${local.year}"
}

/** A chat message's sent time: HH:mm today, else "HH:mm, d MMM". [months] are the 12 short names. */
fun formatSentTime(
    at: Instant,
    now: Instant,
    months: List<String>,
    zone: TimeZone = TimeZone.currentSystemDefault(),
) = if (at.toLocalDateTime(zone).date == now.toLocalDateTime(zone).date) {
    formatTime(at, zone)
} else {
    formatTimeDay(at, months, zone)
}

/** HH:mm in local time. */
fun formatTime(
    at: Instant,
    zone: TimeZone = TimeZone.currentSystemDefault(),
) = at.toLocalDateTime(zone).hhmm()

/** Figma date button: "Aug 20, 2026". [months] are the 12 short names. */
fun formatShortDate(
    date: LocalDate,
    months: List<String>,
) = "${months[date.monthNumber - 1]} ${date.dayOfMonth}, ${date.year}"
