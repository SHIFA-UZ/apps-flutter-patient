package shifa.uz.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private fun Int.twoDigits() = toString().padStart(2, '0')

/** 15 §1.7: ISO-8601, with an optional trailing `[Zone/Id]`. No offset is read as local time, like Dart. */
fun parseInstant(value: String?): Instant? {
    val iso = value?.substringBefore('[')?.trim()?.ifEmpty { null } ?: return null
    return runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(iso).toInstant(TimeZone.currentSystemDefault()) }.getOrNull()
}

/** dd.MM.yyyy */
fun formatDate(date: LocalDate) = "${date.dayOfMonth.twoDigits()}.${date.monthNumber.twoDigits()}.${date.year}"

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
    return "$day · ${local.hour.twoDigits()}:${local.minute.twoDigits()}"
}
