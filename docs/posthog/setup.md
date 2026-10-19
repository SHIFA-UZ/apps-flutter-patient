# Setting up PostHog

Part 1 is done once in PostHog's web app (ANL-01). Part 2 is the code (ANL-02). Check [privacy.md](privacy.md) before
anything is sent from a real device.

## 1. PostHog organization and projects

1. Create the organization **Shifa** at <https://eu.posthog.com> (EU cloud, decision D1). If legal requires data to
   stay in Uzbekistan, stop here and plan a self-hosted instance instead (privacy.md §5).
2. Turn on 2FA for every member, and keep members to the people who need the data (Project → Access control).
3. Create two projects:
   - **Shifa Patient (Dev)**: debug builds, emulators, local testing.
   - **Shifa Patient (Prod)**: Play Store and TestFlight / App Store release builds.
4. Apply these settings to **both** projects:

| Setting | Value | Why |
|---|---|---|
| Timezone | `Asia/Tashkent` | Days and weeks in charts match the patients' days |
| Discard client IP data | **On** | IP is personal data, and we don't need geo-IP |
| Person profiles | Identified only (also set in the SDK) | Anonymous pre-login traffic stays cheap and profile-free |
| Record user sessions (session replay) | **Off** | Decision D5, privacy.md §6 |
| Autocapture / heatmaps / web vitals | Off | Web-only features; nothing to capture in the app |
| Exception autocapture | Off for now | Crashes go to Crashlytics when it's wired (spec 01 §2.1). Revisit in ANL-08. |
| Data retention | 1 year (Prod), 30 days (Dev), if the plan allows | Keep only what we analyse |
| Surveys | Off until ANL-08 | |

5. In Prod, add the property filters "Filter out internal and test users". Mark the team's accounts with the person
   property `internal = true` (set from the Debug menu, development.md §5).

## 2. Keys

Each project has a **Project API key** (`phc_…`). It's a write-only key built into the app (PostHog designs it to be
public), but keep it out of git anyway, so forks and local runs don't write to Prod.

| Where | Dev key | Prod key |
|---|---|---|
| Local Android | `local.properties`: `posthog.apiKey.dev=phc_…` | - |
| Local iOS | `iosApp/Configuration/Secrets.xcconfig` (gitignored): `POSTHOG_API_KEY = phc_…` | - |
| GitHub Actions | secret `POSTHOG_API_KEY_DEV` (for CI builds of debug, if any) | secret `POSTHOG_API_KEY_PROD` |

Host for both: `https://eu.i.posthog.com` (an `ApiConfig`-style constant, not a secret).

A build with no key runs with analytics off, so a fresh clone works without any setup.

### CI changes

- `google_play_deploy.yml`, in the build step: pass `-Pposthog.apiKey.prod=${{ secrets.POSTHOG_API_KEY_PROD }}` to
  Gradle.
- `ios_testflight.yml`, before the fastlane step: write the key into the xcconfig that release builds read:
  `echo "POSTHOG_API_KEY = $POSTHOG_API_KEY_PROD" > iosApp/Configuration/Secrets.xcconfig`, with
  `POSTHOG_API_KEY_PROD: ${{ secrets.POSTHOG_API_KEY_PROD }}` in that step's `env`.
- Add both secrets to the secrets list in `CLAUDE.md` (CI/CD section).

## 3. Versions

- Add `posthog-android` to `gradle/libs.versions.toml`, pinned to the latest 3.x on Maven Central
  (`com.posthog:posthog-android`). Never use `3.+`: builds must be reproducible.
- iOS: add the Swift package `https://github.com/PostHog/posthog-ios.git` to the Xcode project ("Up to Next Major",
  from the latest 3.x), product `PostHog`, added the same way as Daily's package.
- Upgrade path: once `com.posthog:posthog-kmp` reaches 1.0 and we're on Kotlin 2.4+, it can replace the two platform
  files below. The facade and every call site stay the same.

## 4. Code: the shared facade (`commonMain`)

New package `data/analytics/`. Nothing in `ui/` touches a PostHog class directly.

```kotlin
// data/analytics/Analytics.kt

/** What the platform SDK does. Android: PostHog's SDK; iOS: Swift's PostHogAnalytics through the bridge. */
interface AnalyticsSink {
    fun capture(event: String, properties: Map<String, Any>)
    fun screen(name: String, properties: Map<String, Any>)
    fun identify(distinctId: String, userProperties: Map<String, Any>, userPropertiesSetOnce: Map<String, Any>)
    fun register(key: String, value: Any)
    fun reset()
    fun optOut(out: Boolean)
}

/** For ViewModels: constructor-injected with `Analytics` as the default, a recording fake in tests. */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

/**
 * The app's one way into analytics (docs/posthog). [sink] is set at launch; it stays null in tests, previews,
 * builds without a key, and while demo data is on, so nothing is sent from those.
 */
object Analytics : AnalyticsTracker {
    var sink: AnalyticsSink? = null

    override fun track(event: AnalyticsEvent) {
        sink?.capture(event.name, event.properties)
    }

    fun screen(screen: AnalyticsScreen, vararg properties: Pair<String, Any?>) {
        sink?.screen(screen.id, properties.clean())
    }

    /** After P1 loads (Home), and again after Edit profile saves. */
    fun identify(profile: PatientProfile) { /* events.md §1.2 */ }

    /** Logout, account deletion and a session 401. */
    fun reset() {
        sink?.reset()
    }
}
```

```kotlin
// data/analytics/AnalyticsEvent.kt: the catalog in code. One class per event in events.md.

sealed class AnalyticsEvent(
    val name: String,
    vararg props: Pair<String, Any?>,
) {
    /** Nulls dropped, so an unknown value is left out instead of sent as "null". */
    val properties: Map<String, Any> = props.clean()

    data class BookingCompleted(val isVideo: Boolean, val paymentStatus: String?, val daysAhead: Int) :
        AnalyticsEvent(
            "booking_completed",
            "is_video" to isVideo,
            "payment_status" to paymentStatus,
            "days_ahead" to daysAhead,
        )
    // … every other event in events.md
}

enum class AnalyticsScreen(val id: String) { Home("home"), Bookings("bookings") /* … events.md §2 */ }
```

Why typed events and not `track("some_name", mapOf(...))`: every event and property is in one file, compiled and
reviewable. A typo can't create a new event, and a privacy review reads one file (privacy.md §3).

## 5. Code: platforms

### Android

1. **Application class** (the deferred `Application` init): `shifa/uz/ShifaApp.kt`, registered with
   `android:name=".ShifaApp"` in `AndroidManifest.xml`. Move `initSecureSettings(this)` there from `MainActivity`.
2. Gradle (`composeApp/build.gradle.kts`): `implementation(libs.posthog.android)` in `androidMain`, and per build type
   a `buildConfigField("String", "POSTHOG_API_KEY", …)`: debug reads `posthog.apiKey.dev` from `local.properties`,
   release reads the `posthog.apiKey.prod` Gradle property. Missing → `""`.
3. `androidMain/…/data/analytics/PostHogSink.kt`: implements `AnalyticsSink` with `PostHog.capture`, `PostHog.screen`,
   `PostHog.identify`, `PostHog.register`, `PostHog.reset`, `PostHog.optOut()` / `optIn()`.
4. In `ShifaApp.onCreate`, when the key isn't blank:

```kotlin
val config =
    PostHogAndroidConfig(apiKey = BuildConfig.POSTHOG_API_KEY, host = POSTHOG_HOST).apply {
        captureApplicationLifecycleEvents = true // Application Installed / Updated / Opened / Backgrounded
        captureScreenViews = false // one Activity; screens are sent by hand (D6)
        captureDeepLinks = false
        sessionReplay = false // D5
        personProfiles = PersonProfiles.IDENTIFIED_ONLY
        optOut = AnalyticsSettings.optedOut || DemoBackend.enabled // privacy.md §4, D8
        debug = BuildConfig.DEBUG
    }
PostHogAndroid.setup(this, config)
Analytics.sink = PostHogSink()
```

### iOS

Same pattern as Daily: a Kotlin interface that Swift implements and registers at launch.

1. `iosMain/…/data/analytics/AnalyticsBridge.kt`: `interface NativeAnalytics` (the `AnalyticsSink` methods with
   `Map<String, Any>`), and `fun registerAnalytics(native: NativeAnalytics)`, which sets `Analytics.sink` unless
   demo data is on.
2. `iosApp/iosApp/PostHogAnalytics.swift`: `class PostHogAnalytics: NativeAnalytics`, forwarding to
   `PostHogSDK.shared.capture(_:properties:)`, `.screen(_:properties:)`, `.identify(_:userProperties:userPropertiesSetOnce:)`,
   `.register(_:)`, `.reset()` and `.optOut()` / `.optIn()`.
3. `Info.plist`: `PostHogApiKey = $(POSTHOG_API_KEY)`. `Config.xcconfig` gets `#include? "Secrets.xcconfig"`, so a
   missing file just means no key.
4. In `AppDelegate.application(_:didFinishLaunchingWithOptions:)`, after Firebase:

```swift
if let key = Bundle.main.object(forInfoDictionaryKey: "PostHogApiKey") as? String, !key.isEmpty {
    let config = PostHogConfig(apiKey: key, host: "https://eu.i.posthog.com") // `projectToken:` in newer SDKs
    config.captureApplicationLifecycleEvents = true
    config.captureScreenViews = false // UIKit only sees the one Compose controller
    config.sessionReplay = false
    config.personProfiles = .identifiedOnly
    PostHogSDK.shared.setup(config)
    AnalyticsBridgeKt.registerAnalytics(native: PostHogAnalytics())
}
```

Check the opt-out and demo flags before `setup`, as on Android.

## 6. Hooks into the app

| What | Where | Notes |
|---|---|---|
| Screens | A `TrackScreen(AnalyticsScreen.X)` composable (`LaunchedEffect(screen) { Analytics.screen(screen) }`) in each `XxxScreen`, never in `XxxContent`, so previews and screenshot tests send nothing. Tabs: in `MainScreen` when `tab` changes. | events.md §2 has every screen |
| Identify | `HomeViewModel` after P1 succeeds, and after Edit profile saves | events.md §1.2 |
| Reset | `AuthRepository.logout()`, after account deletion, and on the session 401 path | Before the token is wiped, so the logout event still belongs to the user |
| Super properties | On launch and when they change: `app_language` (LanguageStorage) | events.md §1.3 |
| Events | ViewModels, through an `analytics: AnalyticsTracker = Analytics` constructor param (CLAUDE.md DI style) | Only after the outcome is known: `booking_completed` in B3's `onSuccess`, not on the tap |
| Push | `ShifaMessagingService` / `AppDelegate.willPresent` (received), `Push.opened` handling (opened) | events.md §12 |
| API errors | `apiCall` failure branch, next to `httpLogger` | events.md §13, with the path's ids replaced |
| Opt-out | Profile → Settings → "Share usage data" switch → `AnalyticsSettings` (plain Settings) → `Analytics.sink?.optOut(...)` | privacy.md §4 |
| Demo data | Switching demo data on re-keys `App`; the sink is detached while it's on | D8 |

## 7. Checking it works

1. Debug build with the Dev key, `debug = true`: Logcat (tag `PostHog`) / the Xcode console print each event.
2. PostHog Dev → **Activity** (live events): open the app, log in, open a few screens. Check that:
   - `Application Opened` arrives, then `$screen` with our screen names;
   - after login, events carry `distinct_id = patient_<id>` and the person has only the allowed properties
     (privacy.md §3);
   - no event has a name, phone, email, free text, doctor or document names, or health values.
3. Log out: the next events are anonymous again (new `distinct_id`).
4. Turn on demo data: nothing arrives. Turn on the opt-out switch: nothing arrives.
5. Kill the network, use the app, bring it back: the queued events arrive (the SDKs batch and retry).
6. Only then add the Prod key to CI.
