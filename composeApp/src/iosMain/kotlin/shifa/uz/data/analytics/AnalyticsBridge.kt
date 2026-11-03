package shifa.uz.data.analytics

import shifa.uz.data.debug.DemoMode

// docs/posthog/setup.md §5: called from iosApp/AppDelegate.swift as AnalyticsBridgeKt.

/** Read before `PostHogSDK.setup`, so the SDK starts opted out when the patient or demo mode says so. */
fun analyticsStartsOptedOut(): Boolean = AnalyticsSettings().optedOut || DemoMode.enabled.value

/** [sink] is Swift's `PostHogAnalytics`, which implements the shared [AnalyticsSink]. */
fun registerAnalytics(sink: AnalyticsSink) {
    Analytics.platform = sink
    Analytics.start(AnalyticsSettings().optedOut, DemoMode.enabled.value)
}
