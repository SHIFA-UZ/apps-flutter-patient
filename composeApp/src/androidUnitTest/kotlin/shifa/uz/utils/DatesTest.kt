package shifa.uz.utils

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test

class DatesTest {
    private val tashkent = TimeZone.of("Asia/Tashkent") // UTC+5

    private fun slot(at: String) =
        formatSlot(Instant.parse(at), Instant.parse("2026-09-27T20:00:00Z"), "Today", "Tomorrow", tashkent)

    @Test
    fun `slots are today, tomorrow or a date, in local time`() {
        // Now is 28.09 01:00 in Tashkent.
        assertThat(slot("2026-09-28T05:00:00Z")).isEqualTo("Today · 10:00")
        assertThat(slot("2026-09-28T20:00:00Z")).isEqualTo("Tomorrow · 01:00")
        assertThat(slot("2026-10-05T04:30:00Z")).isEqualTo("05.10 · 09:30")
    }

    @Test
    fun `instants drop a zone suffix, read no offset as local, and reject the rest`() {
        assertThat(parseInstant("2026-09-27T10:00:00Z[UTC]")).isEqualTo(Instant.parse("2026-09-27T10:00:00Z"))
        assertThat(parseInstant("2026-09-27T10:00:00"))
            .isEqualTo(LocalDateTime(2026, 9, 27, 10, 0).toInstant(TimeZone.currentSystemDefault()))
        listOf(null, "", "tomorrow").forEach { assertThat(parseInstant(it)).isNull() }
    }

    @Test
    fun `dates are dd MM yyyy`() {
        assertThat(formatDate(LocalDate(2026, 1, 5))).isEqualTo("05.01.2026")
    }
}
