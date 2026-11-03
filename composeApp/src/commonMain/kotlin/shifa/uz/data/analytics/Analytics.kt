package shifa.uz.data.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.model.canUseShifaAi
import shifa.uz.data.storage.languageStorage
import shifa.uz.getPlatform
import shifa.uz.utils.isDebugBuild

/** PostHog Cloud EU (docs/posthog/README.md D1). Not a secret; the project key is. */
const val POSTHOG_HOST = "https://eu.i.posthog.com"

/** On every event and person: tells this app's data from the doctor app's if they ever share a project (setup.md §1.7). */
const val ANALYTICS_APP = "patient"

/** What the platform SDK does. Android: PostHog's SDK (`PostHogSink`); iOS: Swift's `PostHogAnalytics`. */
interface AnalyticsSink {
    fun capture(
        event: String,
        properties: Map<String, Any>,
        userProperties: Map<String, Any>,
        userPropertiesSetOnce: Map<String, Any>,
    )

    fun screen(
        name: String,
        properties: Map<String, Any>,
    )

    fun identify(
        distinctId: String,
        userProperties: Map<String, Any>,
        userPropertiesSetOnce: Map<String, Any>,
    )

    fun register(
        key: String,
        value: Any,
    )

    fun reset()

    /** The SDK's own opt-out: also stops its automatic lifecycle and exception events. */
    fun optOut(out: Boolean)

    fun isFeatureEnabled(key: String): Boolean

    /** The flag's payload re-encoded as JSON by the platform (the SDKs hand back decoded objects); null if none. */
    fun featureFlagPayloadJson(key: String): String?

    fun reloadFeatureFlags()
}

/** For ViewModels: constructor-injected with [Analytics] as the default, a recording fake in tests. */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

/**
 * The app's one way into analytics (docs/posthog). [platform] is set at launch by the platform code and stays null
 * in tests, previews and builds without a key. [sink] is null while the patient has opted out or demo data is on,
 * so neither events nor flag reads reach the SDK then (privacy.md §4, README D8).
 */
object Analytics : AnalyticsTracker {
    var platform: AnalyticsSink? = null

    private var optedOut = false
    private var demo = false
    private val superProperties = mutableMapOf<String, Any>()

    val sink: AnalyticsSink? get() = platform.takeIf { !optedOut && !demo }

    /** The `language` person property's source; tests replace it (the real one needs a platform `Settings`). */
    var languageProvider: () -> String = { languageStorage.language.value.code }

    private val _recent = MutableStateFlow<List<String>>(emptyList())

    /** Debug builds: the last [RECENT_MAX] events this session, for the Debug menu. Empty in release. */
    val recent: StateFlow<List<String>> get() = _recent

    /** A screen view (events.md §2); `TrackScreen` calls it. */
    fun screen(
        screen: AnalyticsScreen,
        properties: List<Pair<String, Any?>> = emptyList(),
    ) {
        if (isDebugBuild) {
            _recent.value =
                (
                    listOf(
                        "\$screen ${screen.id} ${properties.clean()}",
                    ) + _recent.value
                ).take(RECENT_MAX)
        }
        sink?.screen(screen.id, properties.clean())
    }

    override fun track(event: AnalyticsEvent) {
        if (isDebugBuild) _recent.value = (listOf(event.label) + _recent.value).take(RECENT_MAX)
        sink?.capture(event.name, event.properties, event.userProperties, event.userPropertiesSetOnce)
    }

    /**
     * After P1 loads (Home) and after a profile save. The distinct id is `patient_<id>`; the person properties are
     * the usage facts events.md §1.2 allows, never a name, phone or email.
     */
    fun identify(
        profile: PatientProfile,
        language: String = languageProvider(),
    ) {
        val id = profile.id?.trim()?.takeIf { it.isNotEmpty() } ?: return
        sink?.identify(
            "patient_$id",
            listOf(
                "app" to ANALYTICS_APP,
                "language" to language,
                "subscription_tier" to profile.subscriptionTier,
                "shifa_ai_enabled" to profile.canUseShifaAi,
                "has_photo" to (profile.photoUrl != null),
                "has_email" to !profile.email.isNullOrBlank(),
                "has_phone" to !profile.phone.isNullOrBlank(),
            ).clean(),
            mapOf("first_platform" to getPlatform().name.lowercase()),
        )
    }

    /**
     * The platform's startup: both guards at once, so a launch in demo mode never opts the SDK in for a moment
     * between the two. [platform] must already be set.
     */
    fun start(
        optedOut: Boolean,
        demo: Boolean,
    ) {
        this.optedOut = optedOut
        this.demo = demo
        apply()
        superProperties.forEach { (key, value) -> platform?.register(key, value) }
    }

    /**
     * Logout, account deletion and a session 401. Through [platform], not [sink]: the identity must go even while
     * opted out, or patient A's distinct id would be left behind for patient B's later opt-in. The SDK's reset also
     * drops the super properties, so they're put back.
     */
    fun reset() {
        platform?.reset()
        superProperties.forEach { (key, value) -> platform?.register(key, value) }
    }

    /** A super property on every event (events.md §1.3). Registered even while off, so it's there once back on. */
    fun register(
        key: String,
        value: Any,
    ) {
        superProperties[key] = value
        platform?.register(key, value)
    }

    /** Profile → "Share usage data". Turning it back on re-identifies [profile], so the next events aren't anonymous. */
    fun setOptedOut(
        out: Boolean,
        profile: PatientProfile? = null,
    ) {
        optedOut = out
        apply()
        if (!out && profile != null) identify(profile)
    }

    /** Debug → Demo data (D8). Fake bookings never reach PostHog; Home re-identifies when it's switched off. */
    fun setDemo(on: Boolean) {
        demo = on
        apply()
    }

    private fun apply() {
        platform?.optOut(optedOut || demo)
    }

    /** Tests only: the object is a singleton, so each test starts from nothing. */
    internal fun clearForTests() {
        platform = null
        optedOut = false
        demo = false
        superProperties.clear()
        _recent.value = emptyList()
    }

    private const val RECENT_MAX = 50
}

/** Nulls dropped, so an unknown value is left out instead of sent as "null". */
internal fun List<Pair<String, Any?>>.clean(): Map<String, Any> =
    buildMap { this@clean.forEach { (key, value) -> if (value != null) put(key, value) } }
