# Setting up PostHog from zero

Everything needed to go from "we have no PostHog" to "events from a real device show up in the right project".
Part 1 (§1-3) is done once, by hand, in PostHog's web app and GitHub (ticket ANL-01). Part 2 (§4-7) is the code
(ANL-02). Check [privacy.md](privacy.md) before anything is sent from a real device.

Versions checked on 2026-11-03: `posthog-android` 3.75.1, `posthog-ios` 3.93.3, `posthog-kmp` 0.10.0.

## 0. What you need before you start

- A work email for the PostHog account (one shared owner account is fine; every team member gets their own later).
- Admin access to the GitHub org `SHIFA-UZ` (to add the secrets in §2).
- The decision on hosting (README D1). The steps below assume PostHog Cloud EU. If legal requires data to stay in
  Uzbekistan, stop after §1.1 and plan a self-hosted instance instead (privacy.md §5); the code in §4-7 is the same,
  only the host URL changes.
- Nothing else. No credit card: the free plan has no card requirement.

## 1. The PostHog account, organization and projects

### 1.1 Create the account in the EU region

1. Open <https://eu.posthog.com/signup>. The `eu.` part matters: the region is chosen at signup and can't be
   changed afterwards without a migration. (`app.posthog.com` / `us.posthog.com` is the US region; don't use it.)
2. Sign up with the work email. Pick "Product analytics" when asked what you want to use.
3. Name the organization **Shifa**.
4. Turn on 2FA for your account at once (top-right avatar → Account settings → Two-factor authentication).

### 1.2 The project

PostHog creates a first project automatically. Rename it: Settings → Project → General → Project name: **Shifa**
(not "Shifa Patient": on the free plan the doctor app will share it, §1.7).

One project is enough (README D4): only release builds (Play Store, TestFlight, App Store) send to it. Debug
builds, emulators, demo data and the development backend never carry a key, so there is no development data to
keep apart. The free plan allows exactly one project, so this costs nothing (§1.6).

### 1.3 Project settings

Settings → Project:

| Setting | Value | Why |
|---|---|---|
| General → Timezone | `Asia/Tashkent` | Days and weeks in charts match the patients' days |
| General → Discard client IP data | **On** | IP is personal data, and we don't need geo-IP |
| Product analytics → Person profiles | Identified only (also set in the SDK) | Anonymous pre-login traffic stays cheap and profile-free |
| Session replay → Record user sessions | **Off** | README D5, privacy.md §6 |
| Autocapture, Heatmaps, Web vitals | Off | Web-only features; nothing to capture in a native app |
| Error tracking → Enable exception autocapture | **On** (Prod and Dev) | README D11. Also requires the SDK flag in §5. |
| Surveys | Off until ANL-08 | README D12 |
| Feature flags | Nothing to set; flags are created when needed | personalisation.md |

Data retention is fixed by the plan (1 year free, 7 years paid). We keep the free tier's 1 year; that's enough for
year-over-year retention charts.

### 1.4 Filter out the team

Settings → Project → Product analytics → "Filter out internal and test users" → add the filter
`distinct_id` is not in `patient_<id>, patient_<id>, …` with the team's own patient ids. This matters more with one
project: the team's TestFlight and internal-track builds are release builds and do send. Each team member reads
their id from Profile → Debug menu → Analytics in a debug build (development.md §5) and adds it here once; a
debug build can't send a marker itself. Turn the filter on by default for new insights.

### 1.5 Team members and access

- Settings → Organization → Members → Invite. Roles: **Admin** for the two or three people who manage settings and
  keys, **Member** for everyone who only reads dashboards.
- Keep members to the people who need the data: product, engineering leads, support leads. Remove leavers the day
  they leave (privacy.md §8).
- Every member turns on 2FA. Settings → Organization → "Enforce 2FA" once everyone has it.

### 1.6 Billing: free plan vs pay-as-you-go

PostHog's plans on 2026-11-03:

| Plan | Base price | Projects | Free monthly allowance (resets monthly) |
|---|---|---|---|
| Free | $0 | 1 | 1,000,000 analytics events, 1,000,000 feature flag requests, 100,000 exceptions, 1,500 survey responses, 5,000 session replays |
| Paid (pay-as-you-go) | $0 | 6 | The same free allowances; you pay only for usage above them |

**Start on Free.** One project is all the patient app needs (D4), and nothing is billed until usage passes the
allowances. When it does, or when the doctor app needs its own project (§1.7), switch to Paid and set a **billing
limit** (Settings → Organization → Billing → "Set a billing limit"), so a bug that loops events can't produce a
surprise bill: PostHog stops ingesting at the limit instead of charging more.

#### What it costs as the app grows

PostHog's per-event prices (2026-11-03), after each product's own first 1,000,000 free a month. An event from an
identified patient (everything after login, with person profiles) pays **both** rows:

| Monthly events | Base, per event | Identified add-on, per event |
|---|---|---|
| 1-2 M | $0.00005 | $0.000198 |
| 2-15 M | $0.0000343 | $0.0000697 |
| 15-50 M | $0.0000295 | $0.000036 |

Feature-flag requests: first 1,000,000 a month free, then $0.0001. Exceptions: first 100,000 free.

Assumptions for a patient: about 10 sessions a month (weekly visits, check-ins, chat) and about 30 events a
session (2 lifecycle, 8 screens, 15 actions and outcomes, a few errors), so about **300 events per monthly active
patient**, nearly all identified. Flags: 2 requests a session, so 200,000 a month at 10,000 patients, well inside
the free 1,000,000.

| Monthly active patients | Events a month | Base | Identified add-on | **Total a month** |
|---|---|---|---|---|
| 3,000 | 0.9 M | $0 | $0 | **$0** |
| 5,000 | 1.5 M | $25 | $99 | **$124** |
| 10,000, lean (150 events each) | 1.5 M | $25 | $99 | **$124** |
| **10,000, as assumed** | **3 M** | **$84** | **$268** | **$352** |
| 10,000, heavy (500 events each) | 5 M | $153 | $407 | **$560** |
| 30,000 | 9 M | $290 | $686 | **$976** |

How the 10,000 row is computed: 3 M events, 1 M free in each row, then 1 M at the 1-2 M price and 1 M at the
2-15 M price: base 1 M × $0.00005 + 1 M × $0.0000343 = $84; add-on 1 M × $0.000198 + 1 M × $0.0000697 = $268.
"Active" means monthly active; if 10,000 is a *daily* count, use the 30,000 row or above.

Three quarters of the bill is the identified add-on. The levers, in order: keep the catalog lean (every `$screen`
and tab change is an event; skip low-value ones such as `api_slow` and `home_refreshed` at scale), sample
high-volume events in the app (send 1 in 10 `$screen`s), and only as a last resort drop `identify` (anonymous events
are the base price only, so the 10,000 row becomes $84, but person properties, cohorts and most of
personalisation.md go with it).

### 1.7 The doctor app: how the two apps stay apart

The doctor app (`web-flutter-doctor`, moving to KMP) will get its own tracking plan. The free plan allows one
project, so the two apps **share the project "Shifa"** and are told apart by data, not by project. The patient app
already does its half (built in ANL-02):

| Mechanism | Patient app | Doctor app (its plan must mirror this) |
|---|---|---|
| Distinct id | `patient_<id>` | `doctor_<id>` (clinic staff `staff_<id>`); the admin panel is not tracked at all (the user's call, 2026-11-03). A doctor who is also a patient is two persons, which is right, since they're two roles |
| Super property on every event | `app = patient` | `app = doctor` |
| Person property | `app = patient` (set at identify) | `app = doctor` |
| Event names | events.md | its own catalog, same conventions (events.md §1.1); a name used by both apps (`appointment_cancelled`) is fine, `app` tells them apart |

How that plays out in PostHog:

- **Insights and dashboards:** every dashboard gets a dashboard-level filter `app = patient` (or `doctor`), which
  PostHog applies to all of its insights, so a forgotten filter on one insight can't mix the apps. Name dashboards
  with the app prefix ("Patient · Journey", "Doctor · Schedule"). Trends and funnels that *should* span both apps
  (a patient books → the doctor confirms) are the one thing a shared project makes possible, and the reason it's
  not a bad place to be.
- **Cohorts and feature flags:** target or filter by the person property `app`, or by `distinct_id` matching
  `^doctor_`. A flag for the doctor app never matches a patient.
- **Persons:** the Persons list mixes both; filter by `app`. Deleting a person (privacy.md §4) is per person, so
  unaffected.
- **Access:** one project means everyone with access sees both apps' data. Fine for the team today; it's the one
  thing only a separate project can fix.
- **Volume:** both apps count against the same free 1,000,000 events a month (§1.6). The doctor app is used by far
  fewer people, so it adds little.

**Later, a real split:** switch to Paid ($0 base, §1.6), create the project **Shifa Doctor**, and give the doctor
app that project's key in its next release. Events can't be moved between projects, so the doctor app's history to
that point stays in "Shifa" (filtered by `app`); decide before the doctor app's first tracked release if that
matters. The patient app changes nothing either way.

## 2. The key

The project has a **Project API key** (`phc_…`), under Settings → Project → General → Project ID & API key. It's a
write-only key built into release builds (PostHog designs it to be public), but keep it out of git anyway, so forks
and local runs can't write to the project.

| Where | Key |
|---|---|
| Debug builds (local, emulators) | **None, ever** (D4). The Gradle debug build type and the Xcode Debug configuration hard-wire an empty key |
| GitHub Actions | secret `POSTHOG_API_KEY_PROD`, passed to Gradle as `-Pposthog.apiKey.prod` by `google_play_deploy.yml` and written as `POSTHOG_API_KEY_PROD` into the gitignored `iosApp/Configuration/Secrets.xcconfig` by `ios_testflight.yml` |
| A local release build (rare) | the same Gradle property / `Secrets.xcconfig` line, by hand |

Host: `https://eu.i.posthog.com` (an `ApiConfig`-style constant, not a secret). Note the `.i.` in the ingestion
host; `eu.posthog.com` without it is the web app.

A build with no key runs with analytics off, so a fresh clone works without any setup, and a debug build can't
send even by mistake.

### 2.1 GitHub

1. `SHIFA-UZ/apps-flutter-patient` → Settings → Secrets and variables → Actions → New repository secret
   `POSTHOG_API_KEY_PROD` = the project's key.
2. `google_play_deploy.yml`, in the Gradle build step: pass `-Pposthog.apiKey.prod=${{ secrets.POSTHOG_API_KEY_PROD }}`.
3. `ios_testflight.yml`, before the fastlane step, write the key into the xcconfig that release builds read:

```yaml
- name: Write PostHog key
  env:
    POSTHOG_API_KEY_PROD: ${{ secrets.POSTHOG_API_KEY_PROD }}
  run: echo "POSTHOG_API_KEY_PROD = $POSTHOG_API_KEY_PROD" > iosApp/Configuration/Secrets.xcconfig
```

4. Add `POSTHOG_API_KEY_PROD` to the secrets list in `CLAUDE.md` (CI/CD section).

### 2.2 Rotating a key

Settings → Project → General → "Reset project API key". The old key stops working at once, so release a build with
the new key first, wait for store adoption, then reset. Store builds with the old key then send nothing (they fail
silently; the SDK drops the queue). Rotate only if a key leaks; it can't read data.

## 3. Versions and dependencies

- Android: add to `gradle/libs.versions.toml`:

```toml
[versions]
posthog = "3.75.1"

[libraries]
posthog-android = { module = "com.posthog:posthog-android", version.ref = "posthog" }
```

  Never `3.+` (PostHog's docs suggest it): builds must be reproducible. `posthog-android` needs `minSdk` 21; we're
  on 24.

- iOS: Xcode → the `iosApp` project → Package Dependencies → `+` → `https://github.com/PostHog/posthog-ios.git`,
  "Up to Next Major" from `3.93.3`, product `PostHog`, added to the `iosApp` target the same way as Daily's package.
  PostHog has announced that `posthog-ios` 4.0 will raise the minimum iOS to 15; our deployment target is 18.2,
  so the 4.0 upgrade will be a plain version bump.

- Upgrade path: `com.posthog:posthog-kmp` (0.10.0) wraps both native SDKs for `commonMain`, but its iOS linking
  needs Kotlin 2.4.0+ (we're on 2.3.0), and it's 0.x, so its API may change between minor versions. When the Kotlin
  upgrade lands and `posthog-kmp` is 1.x, it can replace the two platform files below. The facade and every call
  site stay the same.

## 4. Code: the shared facade (`commonMain`)

New package `data/analytics/`. Nothing in `ui/` touches a PostHog class directly.

```kotlin
// data/analytics/Analytics.kt

/** What the platform SDK does. Android: PostHog's SDK; iOS: Swift's PostHogAnalytics through the bridge. */
interface AnalyticsSink {
    fun capture(event: String, properties: Map<String, Any>, userProperties: Map<String, Any>, userPropertiesSetOnce: Map<String, Any>)
    fun screen(name: String, properties: Map<String, Any>)
    fun identify(distinctId: String, userProperties: Map<String, Any>, userPropertiesSetOnce: Map<String, Any>)
    fun register(key: String, value: Any)
    fun reset()
    /** The SDK's optOut() / optIn(): also stops the SDK's own lifecycle and exception capture. */
    fun optOut(out: Boolean)
    fun isFeatureEnabled(key: String): Boolean?
    /** The payload re-encoded as a JSON string by the platform (the SDKs return decoded objects); null if none. */
    fun featureFlagPayloadJson(key: String): String?
    fun reloadFeatureFlags()
}

/** For ViewModels: constructor-injected with `Analytics` as the default, a recording fake in tests. */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

/**
 * The app's one way into analytics (docs/posthog). [platform] is set at launch; it stays null in tests, previews
 * and builds without a key. [sink] is null whenever the patient opted out or demo data is on, so neither events
 * nor flag reads reach the SDK then (privacy.md §4, README D8); [setEnabled] also calls the SDK's optOut(), so
 * its automatic lifecycle and exception capture stop as well.
 */
object Analytics : AnalyticsTracker {
    var platform: AnalyticsSink? = null
    private var enabled = true

    val sink: AnalyticsSink? get() = platform.takeIf { enabled }

    /**
     * Opt-out switch and demo mode: `enabled = !optedOut && !DemoMode.enabled.value`. Turning back on re-identifies
     * the signed-in patient (the caller passes the current profile), so the next events aren't anonymous.
     */
    fun setEnabled(on: Boolean, profile: PatientProfile? = null) {
        enabled = on
        platform?.optOut(!on)
        if (on && profile != null) identify(profile)
    }

    override fun track(event: AnalyticsEvent) {
        sink?.capture(event.name, event.properties, event.userProperties, event.userPropertiesSetOnce)
    }

    fun screen(screen: AnalyticsScreen, vararg properties: Pair<String, Any?>) {
        sink?.screen(screen.id, properties.clean())
    }

    /** After P1 loads (Home), and again after Edit profile saves. events.md §1.2. */
    fun identify(profile: PatientProfile) { /* … */ }

    /**
     * Logout, account deletion and a session 401. Goes through [platform], not [sink]: the identity must be
     * dropped even while opted out, or patient A's distinct_id would be left behind for patient B's later opt-in.
     */
    fun reset() {
        platform?.reset()
    }
}
```

```kotlin
// data/analytics/AnalyticsEvent.kt: the catalog in code. One class per event in events.md.

sealed class AnalyticsEvent(
    val name: String,
    vararg props: Pair<String, Any?>,
    /** Person properties set with this event (personalisation.md §2): `has_booked = true`, … */
    val userProperties: Map<String, Any> = emptyMap(),
    /** Person properties set only the first time: `first_booking_at`, … */
    val userPropertiesSetOnce: Map<String, Any> = emptyMap(),
) {
    /** Nulls dropped, so an unknown value is left out instead of sent as "null". */
    val properties: Map<String, Any> = props.clean()

    data class BookingCompleted(val isVideo: Boolean, val paymentKind: String, val daysAhead: Int) :
        AnalyticsEvent(
            "booking_completed",
            "is_video" to isVideo,
            "payment_kind" to paymentKind,
            "days_ahead" to daysAhead,
            userProperties = mapOf("has_booked" to true, "last_visit_type" to if (isVideo) "video" else "in_person"),
            userPropertiesSetOnce = mapOf("first_booking_at" to nowIso()),
        )
    // … every other event in events.md
}

enum class AnalyticsScreen(val id: String) { Home("home"), Bookings("bookings") /* … events.md §2 */ }
```

```kotlin
// data/analytics/Flags.kt: typed feature flags with safe defaults (personalisation.md §4). Empty at first.

object Flags {
    // Example, not built yet. Payloads come as JSON strings and are decoded with kotlinx.serialization:
    // val homeOrder: List<String>
    //     get() = Analytics.sink?.featureFlagPayloadJson("home-order")?.let { decodeOrNull<HomeOrder>(it)?.order } ?: DEFAULT_HOME_ORDER
}
```

Why typed events and not `track("some_name", mapOf(...))`: every event and property is in one file, compiled and
reviewable. A typo can't create a new event, and a privacy review reads one file (privacy.md §3).

## 5. Code: platforms

### Android

1. **Application class** (the deferred `Application` init): `shifa/uz/ShifaApp.kt`, registered with
   `android:name=".ShifaApp"` in `AndroidManifest.xml`. Move `initSecureSettings(this)` there from `MainActivity`.
2. Gradle (`composeApp/build.gradle.kts`): `implementation(libs.posthog.android)` in `androidMain.dependencies`,
   and per build type a `buildConfigField("String", "POSTHOG_API_KEY", …)`: debug is always `""` (D4), release
   reads the `posthog.apiKey.prod` Gradle property, missing → `""`.
3. `androidMain/…/data/analytics/PostHogSink.kt`: implements `AnalyticsSink` with `PostHog.capture`,
   `PostHog.screen`, `PostHog.identify`, `PostHog.register`, `PostHog.reset`, `PostHog.optOut()` / `optIn()`,
   `PostHog.isFeatureEnabled`, `PostHog.getFeatureFlagResult(key)?.payload` (nullable; the payload is a decoded
   `Any`, so re-encode it with `org.json` or kotlinx.serialization to the JSON string the facade promises),
   `PostHog.reloadFeatureFlags()`.
4. In `ShifaApp.onCreate`, in release builds only and when the key isn't blank:

```kotlin
val config =
    PostHogAndroidConfig(apiKey = BuildConfig.POSTHOG_API_KEY, host = POSTHOG_HOST).apply {
        captureApplicationLifecycleEvents = true // Application Installed / Updated / Opened / Backgrounded
        captureScreenViews = false // one Activity; screens are sent by hand (D6)
        captureDeepLinks = false
        sessionReplay = false // D5
        personProfiles = PersonProfiles.IDENTIFIED_ONLY
        preloadFeatureFlags = true // D10: flags are ready by the time Home shows
        errorTrackingConfig.autoCapture = true // D11; also needs the project setting (§1.3)
        optOut = AnalyticsSettings.optedOut || DemoMode.enabled.value // privacy.md §4, D8
        debug = BuildConfig.DEBUG
    }
PostHogAndroid.setup(this, config)
Analytics.platform = PostHogSink()
Analytics.setEnabled(!AnalyticsSettings.optedOut && !DemoMode.enabled.value)
```

### iOS

Same pattern as Daily: a Kotlin interface that Swift implements and registers at launch.

1. `iosMain/…/data/analytics/AnalyticsBridge.kt`: `interface NativeAnalytics` (the `AnalyticsSink` methods with
   `Map<String, Any>`), and `fun registerAnalytics(native: NativeAnalytics)`, which sets `Analytics.platform` and
   calls `Analytics.setEnabled(...)` from the opt-out and demo flags.
2. `iosApp/iosApp/PostHogAnalytics.swift`: `class PostHogAnalytics: NativeAnalytics`, forwarding to
   `PostHogSDK.shared.capture(_:properties:userProperties:userPropertiesSetOnce:)`, `.screen(_:properties:)`,
   `.identify(_:userProperties:userPropertiesSetOnce:)`, `.register(_:)`, `.reset()`, `.optOut()` / `.optIn()`,
   `.isFeatureEnabled(_:)`, `.getFeatureFlagPayload(_:)` (returns `Any?`; re-encode with `JSONSerialization` to the
   JSON string the facade promises), `.reloadFeatureFlags()`.
3. `Info.plist`: `PostHogApiKey = $(POSTHOG_API_KEY)`. `Config.xcconfig` sets `POSTHOG_API_KEY[config=Debug]` to
   nothing and `POSTHOG_API_KEY[config=Release]` to `$(POSTHOG_API_KEY_PROD)`, with `#include? "Secrets.xcconfig"`
   (the `?` makes a missing file harmless) providing that value; `Secrets.xcconfig` is in `.gitignore`.
4. In `AppDelegate.application(_:didFinishLaunchingWithOptions:)`, after `FirebaseApp.configure()`, skipped
   entirely under `#if DEBUG`:

```swift
if let key = Bundle.main.object(forInfoDictionaryKey: "PostHogApiKey") as? String, !key.isEmpty {
    let config = PostHogConfig(projectToken: key, host: "https://eu.i.posthog.com") // `apiKey:` in older 3.x
    config.captureApplicationLifecycleEvents = true
    config.captureScreenViews = false // UIKit only sees the one Compose controller (D6)
    config.captureElementInteractions = false
    config.sessionReplay = false // D5
    config.surveys = false // D12 (the iOS default is on)
    config.personProfiles = .identifiedOnly
    config.preloadFeatureFlags = true
    config.errorTrackingConfig.autoCapture = true // D11
    #if DEBUG
    config.debug = true
    #endif
    PostHogSDK.shared.setup(config)
    AnalyticsBridgeKt.registerAnalytics(native: PostHogAnalytics())
}
```

Check the opt-out and demo flags before `setup`, as on Android. Push capture (`capturePushNotificationSubscriptions`,
`capturePushNotificationOpened`) is off on both platforms: our own push events carry no payload.

## 6. Hooks into the app

| What | Where | Notes |
|---|---|---|
| Screens | A `TrackScreen(AnalyticsScreen.X, vararg properties)` composable (`LaunchedEffect(screen, properties) { Analytics.screen(screen, *properties) }`) in each `XxxScreen`, never in `XxxScreenContent`, so previews and screenshot tests send nothing. The properties are part of the key, so Register and Forgot password re-send on each `step`. Tabs: in `MainScreen` when `tab` changes. | events.md §2 has every screen |
| Identify | `HomeViewModel` after P1 succeeds, and `EditProfileViewModel` after P2 saves | events.md §1.2 |
| Reset | `AuthRepository.logout()`, after account deletion (P6), and on the session-expired path | Send `logged_out` first, then `reset()`, so the event still belongs to the patient |
| Super properties | On launch and when they change: `app_language` (`languageStorage.language`) | events.md §1.3 |
| Events | ViewModels, through an `analytics: AnalyticsTracker = Analytics` constructor param (CLAUDE.md DI style) | Only after the outcome is known: `booking_completed` in B3's `onSuccess`, not on the tap |
| Push | `ShifaMessagingService.onMessageReceived` / `AppDelegate.willPresent` → `push_received`; `MainScreen`'s `Push.opened` handling → `push_opened` | events.md §11 |
| API errors | `apiCall`'s two failure branches, next to `httpLogger` | events.md §13, with the path's ids replaced |
| Opt-out | Profile → Settings → "Share usage data" switch → `AnalyticsSettings` (plain Settings) → `Analytics.setEnabled(false)` (facade off **and** the SDK's `optOut()`, so flags and the SDK's automatic events stop too) | privacy.md §4 |
| Demo data | `DemoMode.set` → `Analytics.setEnabled(false)`; off again → `setEnabled(!optedOut, profile)` with the signed-in profile (Home's P1), so the patient is re-identified like after an opt-in. Detaching the facade alone isn't enough: the native SDK would keep sending lifecycle events and exceptions, and `App`'s re-key doesn't rerun the native startup | D8 |
| Flags | `Flags` read in ViewModels, never in composables; `reloadFeatureFlags()` after identify | personalisation.md §4 |

## 7. Checking it works

Debug builds never send (D4), so the check has two halves: the Debug menu in a debug build shows that the right
events fire with the right properties, and a release build shows that they arrive.

1. Debug build: Profile → Debug menu → Analytics lists every event of the session, newest first, with its
   properties. Walk the flow under test and read the list.
2. Release build (TestFlight or the Play internal track, with the secret set): first add your `patient_<id>` to
   the internal-users filter (§1.4), so the dashboards leave you out. Then PostHog → **Activity** (live events):
   open the app, log in, open a few screens. Check that:
   - `Application Opened` arrives, then `$screen` with our screen names;
   - after login, events carry `distinct_id = patient_<id>` and the person has only the allowed properties
     (privacy.md §3);
   - no event has a name, phone, email, free text, doctor or document names, or health values.
3. Log out: the next events are anonymous again (new `distinct_id`).
4. Turn on demo data: nothing arrives. Turn on the opt-out switch: nothing arrives.
5. Kill the network, use the app, bring it back: the queued events arrive (the SDKs batch and retry; the queue holds
   1,000 events and drops the oldest first).
6. Force a crash from the Debug menu (add a "Crash now" row in ANL-06) in the release build: the exception appears
   under Error tracking, linked to the person (and filtered out of dashboards by the internal-users filter).
7. Create a test flag (`test-flag`, 100 % on, targeting your `distinct_id` only), restart the app: `Flags` reads it
   as on; turn it off, restart: off; delete it: the default applies.

## 8. Go-live checklist

- [ ] Legal has signed off on D1 (hosting) and D7 (consent), privacy.md §4-5.
- [ ] The privacy policy at `https://shifa.uz/#privacy` names PostHog, the region, what's collected and how to opt out.
- [ ] App Store privacy labels declare "Product interaction" and "Crash data" as linked to the user and not used
      for tracking; Play's Data safety form declares "App interactions", "Crash logs" and "Diagnostics" (privacy.md §4).
- [ ] The project has IP discarding on, replay off and the internal-user filter on (a billing limit once on Paid).
- [ ] `POSTHOG_API_KEY_PROD` is in GitHub, and both deploy workflows pass it.
- [ ] The team's `patient_<id>`s are in the internal-users filter.
- [ ] The account-deletion runbook includes deleting the person in PostHog (privacy.md §4).
