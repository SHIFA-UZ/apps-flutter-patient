package shifa.uz.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import shifa.uz.data.analytics.Analytics
import shifa.uz.data.analytics.AnalyticsScreen

/**
 * Sends the `$screen` event once when [screen] appears, with the [properties] as they are at that moment (later
 * changes, say a chat's first message, don't re-send it), and again whenever [step] changes (a wizard's step).
 * Goes in `XxxScreen`, never in `XxxScreenContent`, so previews and screenshot tests send nothing
 * (docs/posthog/setup.md §6).
 */
@Composable
fun TrackScreen(
    screen: AnalyticsScreen,
    vararg properties: Pair<String, Any?>,
    step: Any? = null,
) {
    // The block that runs is the one passed when the keys last changed, so the properties are a snapshot.
    LaunchedEffect(screen, step) { Analytics.screen(screen, properties.toList()) }
}
