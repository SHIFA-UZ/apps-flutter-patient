package shifa.uz.data.analytics

/**
 * The event catalog in code: one class per row of docs/posthog/events.md, and nothing else is ever sent. Names
 * are `snake_case`, values are our own enums, counts and buckets; no identity, health data, record ids or free text
 * (privacy.md §3). `AnalyticsEventTest` checks the names and property keys.
 */
sealed class AnalyticsEvent(
    val name: String,
    vararg props: Pair<String, Any?>,
    /** Person properties set with the event (personalisation.md §2): `has_booked = true`, … */
    val userProperties: Map<String, Any> = emptyMap(),
    /** Person properties set only the first time: `first_booking_at`, … */
    val userPropertiesSetOnce: Map<String, Any> = emptyMap(),
) {
    val properties: Map<String, Any> = props.toList().clean()

    /** For the Debug menu's list. (Not toString: data objects generate their own.) */
    val label: String get() = if (properties.isEmpty()) name else "$name $properties"

    // events.md §12: the "Share usage data" switch. Opted out is the last event sent, opted in the first.
    data object AnalyticsOptedOut : AnalyticsEvent("analytics_opted_out")

    data object AnalyticsOptedIn : AnalyticsEvent("analytics_opted_in")

    /** Debug menu: marks the team's account, so Prod's "internal users" filter excludes it (development.md §5). */
    data object MarkedInternal : AnalyticsEvent("debug_marked_internal", userProperties = mapOf("internal" to true))
}
