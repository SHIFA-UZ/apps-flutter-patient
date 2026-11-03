package shifa.uz.data.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** docs/posthog/development.md §3: the catalog's names and keys, so the usual privacy slips fail before review. */
class AnalyticsEventTest {
    // Every event class, as one instance. Add a line per new event in AnalyticsEvent.kt.
    private val catalog =
        listOf(
            AnalyticsEvent.AnalyticsOptedOut,
            AnalyticsEvent.AnalyticsOptedIn,
            AnalyticsEvent.MarkedInternal,
        )

    // privacy.md §3: identity, free text, exact amounts and record ids never travel.
    private val blockedKeys =
        setOf(
            "name", "first_name", "last_name", "phone", "email", "text", "comment", "message", "query", "notes",
            "value", "answer", "title", "url", "amount", "address", "birth_date",
        )

    @Test
    fun `event names are snake_case`() {
        catalog.forEach { assertThat(it.name).matches("[a-z][a-z0-9]*(_[a-z0-9]+)*") }
    }

    @Test
    fun `no property key is personal data or a record id`() {
        catalog.forEach { event ->
            val keys = event.properties.keys + event.userProperties.keys + event.userPropertiesSetOnce.keys
            keys.forEach { key ->
                assertThat(key).matches("[a-z][a-z0-9]*(_[a-z0-9]+)*")
                assertThat(key).isNotIn(blockedKeys)
                assertThat(key).doesNotMatch(".*_id$")
            }
        }
    }

    @Test
    fun `null property values are left out instead of sent as null`() {
        assertThat(listOf("a" to 1, "b" to null, "c" to "x").clean()).containsExactly("a", 1, "c", "x")
    }
}

/** For ViewModel tests: the events a ViewModel sent, in order. */
class RecordingTracker : AnalyticsTracker {
    val events = mutableListOf<AnalyticsEvent>()

    override fun track(event: AnalyticsEvent) {
        events += event
    }
}
