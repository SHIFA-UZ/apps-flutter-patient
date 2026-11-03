package shifa.uz.data.analytics

import android.app.Application
import com.posthog.PersonProfiles
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import org.json.JSONArray
import org.json.JSONObject
import shifa.uz.BuildConfig
import shifa.uz.data.debug.DemoMode

/**
 * docs/posthog/setup.md §5. Release builds only (README D4: debug builds never track), and no key (CI without the
 * secret) means analytics stays off: nothing is set up and [Analytics.platform] stays null.
 */
fun setUpAnalytics(app: Application) {
    if (BuildConfig.DEBUG) return
    val key = BuildConfig.POSTHOG_API_KEY
    if (key.isBlank()) return
    val optedOut = AnalyticsSettings().optedOut
    val demo = DemoMode.enabled.value
    val config =
        PostHogAndroidConfig(projectToken = key, host = POSTHOG_HOST).apply {
            captureApplicationLifecycleEvents = true // Application Installed / Updated / Opened / Backgrounded
            captureScreenViews = false // one Activity; screens are sent by hand (README D6)
            captureDeepLinks = false
            captureElementInteractions = false
            // Our own push events (events.md §11); the SDK's would carry the push payload.
            capturePushNotificationSubscriptions = false
            capturePushNotificationOpened = false
            sessionReplay = false // README D5
            personProfiles = PersonProfiles.IDENTIFIED_ONLY
            errorTrackingConfig.autoCapture = true // README D11: crashes next to the taps before them
            // README D10; not while off, or setup would still ask PostHog for flags with the distinct id. (The
            // SDK's reset() reloads them regardless; that one anonymous request on logout is accepted, since the
            // identity must be dropped even while opted out.)
            preloadFeatureFlags = !(optedOut || demo)
            this.optOut = optedOut || demo // privacy.md §4, README D8
        }
    PostHogAndroid.setup(app, config)
    Analytics.platform = PostHogSink()
    Analytics.start(optedOut, demo)
}

internal class PostHogSink : AnalyticsSink {
    override fun capture(
        event: String,
        properties: Map<String, Any>,
        userProperties: Map<String, Any>,
        userPropertiesSetOnce: Map<String, Any>,
    ) = PostHog.capture(
        event = event,
        properties = properties,
        userProperties = userProperties.ifEmpty { null },
        userPropertiesSetOnce = userPropertiesSetOnce.ifEmpty { null },
    )

    override fun screen(
        name: String,
        properties: Map<String, Any>,
    ) = PostHog.screen(name, properties)

    override fun identify(
        distinctId: String,
        userProperties: Map<String, Any>,
        userPropertiesSetOnce: Map<String, Any>,
    ) = PostHog.identify(distinctId, userProperties, userPropertiesSetOnce.ifEmpty { null })

    override fun register(
        key: String,
        value: Any,
    ) = PostHog.register(key, value)

    override fun reset() = PostHog.reset()

    override fun optOut(out: Boolean) = if (out) PostHog.optOut() else PostHog.optIn()

    override fun isFeatureEnabled(key: String) = PostHog.isFeatureEnabled(key)

    override fun featureFlagPayloadJson(key: String) = PostHog.getFeatureFlagResult(key)?.payload?.let(::toJson)

    override fun reloadFeatureFlags() = PostHog.reloadFeatureFlags()
}

// ponytail: org.json covers the shapes PostHog hands back (objects, arrays, scalars); no serializer needed.
private fun toJson(payload: Any): String =
    when (payload) {
        is Map<*, *> -> JSONObject(payload).toString()
        is Collection<*> -> JSONArray(payload).toString()
        is String -> JSONObject.quote(payload)
        else -> payload.toString()
    }
