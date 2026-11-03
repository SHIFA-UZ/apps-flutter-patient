package shifa.uz.data.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import shifa.uz.data.model.PatientProfile

class AnalyticsTest {
    private val sink = RecordingSink()
    private val patient =
        PatientProfile(
            id = " 42 ",
            fullName = "Aziz Karimov",
            email = "aziz@example.com",
            photoUrl = "https://x/p.jpg",
            subscriptionTier = "PRO",
        )

    @Before
    fun setUp() {
        Analytics.clearForTests()
        Analytics.platform = sink
        Analytics.languageProvider = { "en" }
        Analytics.start(optedOut = false, demo = false)
        sink.calls.clear()
    }

    @After
    fun tearDown() {
        Analytics.clearForTests()
    }

    @Test
    fun `an event goes to the sink with its properties`() {
        Analytics.track(AnalyticsEvent.AnalyticsOptedIn)

        assertThat(sink.calls).containsExactly("capture analytics_opted_in {} {} {}")
    }

    @Test
    fun `a screen goes to the sink with its properties and nulls dropped`() {
        Analytics.screen(AnalyticsScreen.Register, listOf("step" to "otp", "is_doctor_link" to false, "x" to null))

        assertThat(sink.calls).containsExactly("screen register {step=otp, is_doctor_link=false}")
        assertThat(Analytics.recent.value.first()).isEqualTo("\$screen register {step=otp, is_doctor_link=false}")
    }

    @Test
    fun `identify uses patient_id and only the allowed person properties`() {
        Analytics.identify(patient, language = "ru")

        assertThat(sink.calls).containsExactly(
            "identify patient_42 {app=patient, language=ru, subscription_tier=PRO, shifa_ai_enabled=false, " +
                "has_photo=true, has_email=true, has_phone=false} {first_platform=android}",
        )
    }

    @Test
    fun `identify resets a different patient the SDK still holds, and not the same one`() {
        sink.distinctId = "patient_7"
        Analytics.identify(patient, language = "ru")
        Analytics.identify(patient, language = "ru")

        assertThat(sink.calls.map { it.substringBefore(" {") })
            .containsExactly(
                "reset",
                "register app=patient",
                "register app_language=en",
                "identify patient_42",
                "identify patient_42",
            )
            .inOrder()
    }

    @Test
    fun `a profile without an id is never identified`() {
        Analytics.identify(PatientProfile(fullName = "Aziz"), language = "en")

        assertThat(sink.calls).isEmpty()
    }

    @Test
    fun `opted out sends nothing, opts the SDK out, but still resets and registers`() {
        Analytics.setOptedOut(true)
        Analytics.track(AnalyticsEvent.AnalyticsOptedIn)
        Analytics.identify(patient, language = "en")
        Analytics.register("app_language", "en")
        Analytics.reset()

        assertThat(Analytics.sink).isNull()
        assertThat(sink.calls)
            .containsExactly(
                "optOut true",
                "register app_language=en",
                "reset",
                "register app=patient",
                "register app_language=en",
            ).inOrder()
    }

    @Test
    fun `opting back in re-identifies the patient`() {
        Analytics.setOptedOut(true)
        sink.calls.clear()

        Analytics.setOptedOut(false, patient)

        assertThat(sink.calls.first()).isEqualTo("optOut false")
        assertThat(sink.calls[1]).startsWith("identify patient_42 ")
    }

    @Test
    fun `demo data keeps analytics off even when the patient is opted in`() {
        Analytics.setDemo(true)
        Analytics.track(AnalyticsEvent.AnalyticsOptedIn)

        assertThat(sink.calls).containsExactly("optOut true")

        Analytics.setDemo(false)
        assertThat(sink.calls.last()).isEqualTo("optOut false")
    }

    @Test
    fun `starting in demo mode opts the SDK out at once, never in first`() {
        Analytics.start(optedOut = false, demo = true)

        assertThat(sink.calls)
            .containsExactly("optOut true", "register app=patient", "register app_language=en")
            .inOrder()
    }

    @Test
    fun `start registers app and app_language right after the opt-out state`() {
        Analytics.clearForTests()
        Analytics.platform = sink
        Analytics.languageProvider = { "ru" }

        Analytics.start(optedOut = false, demo = false)

        assertThat(sink.calls)
            .containsExactly("optOut false", "register app=patient", "register app_language=ru")
            .inOrder()
    }

    @Test
    fun `reset puts the super properties back, since the SDK's reset drops them`() {
        Analytics.register("app_language", "ru")
        Analytics.register("other", "value")
        sink.calls.clear()

        Analytics.reset()

        assertThat(sink.calls)
            .containsExactly("reset", "register app=patient", "register app_language=ru", "register other=value")
            .inOrder()
    }

    @Test
    fun `the debug list keeps the newest events first`() {
        Analytics.track(AnalyticsEvent.AnalyticsOptedOut)
        Analytics.track(AnalyticsEvent.AnalyticsOptedIn)

        assertThat(
            Analytics.recent.value.take(2),
        ).containsExactly("analytics_opted_in", "analytics_opted_out").inOrder()
    }
}

/** Records every sink call as one line, so tests assert order and arguments at once. */
class RecordingSink : AnalyticsSink {
    val calls = mutableListOf<String>()

    override fun capture(
        event: String,
        properties: Map<String, Any>,
        userProperties: Map<String, Any>,
        userPropertiesSetOnce: Map<String, Any>,
    ) {
        calls += "capture $event $properties $userProperties $userPropertiesSetOnce"
    }

    override fun screen(
        name: String,
        properties: Map<String, Any>,
    ) {
        calls += "screen $name $properties"
    }

    override fun identify(
        distinctId: String,
        userProperties: Map<String, Any>,
        userPropertiesSetOnce: Map<String, Any>,
    ) {
        calls += "identify $distinctId $userProperties $userPropertiesSetOnce"
        this.distinctId = distinctId
    }

    override fun register(
        key: String,
        value: Any,
    ) {
        calls += "register $key=$value"
    }

    override fun reset() {
        calls += "reset"
        distinctId = "anonymous"
    }

    /** What the SDK holds: an anonymous id until identified. */
    var distinctId = "anonymous"

    override fun distinctId() = distinctId

    override fun optOut(out: Boolean) {
        calls += "optOut $out"
    }

    override fun isFeatureEnabled(key: String) = false

    override fun featureFlagPayloadJson(key: String): String? = null

    override fun reloadFeatureFlags() {
        calls += "reloadFeatureFlags"
    }
}
