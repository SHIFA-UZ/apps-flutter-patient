# Analytics (PostHog)

The plan: [../posthog/README.md](../posthog/README.md). This file records what of it is built.

## The PostHog project (ANL-01, 2026-11-03)

- Organization **SHIFA** on PostHog Cloud EU, project **Shifa** (id 332380). Settings applied through the API:
  timezone `Asia/Tashkent`, client IPs discarded, session replay off, autocapture / heatmaps / web vitals / console
  logs / performance off, surveys off, exception autocapture off (ANL-06 turns it on), the internal-users filter on
  by default for new insights, weeks start on Monday. Person profiles "identified only" is set in the SDK.
- Still to do: the team's `patient_<id>`s in the internal-users filter (setup.md §1.4), `POSTHOG_API_KEY_PROD` in
  GitHub (setup.md §2.1), legal sign-off (privacy.md §4-5), the privacy policy text.

## Foundation (`data/analytics/`, ANL-02)

- `Analytics` is the one way in: `track(event)`, `identify(profile)`, `reset()`, `register(key, value)`,
  `setOptedOut(out, profile)`, `setDemo(on)`, and `start(optedOut, demo)` for the platform's launch (both guards
  at once). Super properties are kept and re-registered after `reset`, which the SDKs would otherwise drop. Its `platform` is the SDK (`PostHogSink` on Android,
  `PostHogAnalytics.swift` on iOS, registered through `AnalyticsBridge.kt`); `sink` is null while opted out or in demo
  mode, so events and flag reads stop, while `reset` and `register` go through regardless (setup.md §4).
- `AnalyticsEvent` is the catalog in code (events.md). Built so far: `analytics_opted_out`, `analytics_opted_in`. `AnalyticsEventTest` checks names and keys against privacy.md §3.
- Key: release builds only (README D4: debug builds never track; the Gradle debug type and the Xcode Debug
  configuration hard-wire an empty key, and both platform setups return early in debug). Release reads the Gradle
  property `posthog.apiKey.prod` (Play workflow) or `POSTHOG_API_KEY_PROD` in `iosApp/Configuration/Secrets.xcconfig`
  (written by the TestFlight workflow), both from the `POSTHOG_API_KEY_PROD` secret. No key: `platform` stays null.
- SDK config (setup.md §5): lifecycle events on, screen views / deep links / element interactions / push capture off,
  session replay off, identified-only person profiles, flags preloaded unless starting opted out or in demo mode (the
  SDK would otherwise still ask PostHog for flags). Known: the SDK's `reset()` reloads flags
  regardless, so a logout while opted out makes one anonymous flags request. Error tracking
  autocapture is ANL-06.
- Hooks: `HomeViewModel.show` identifies (`patient_<id>` with the events.md §1.2 person properties);
  `AuthRepository.logout`, the session-restore 401 and the reinstall wipe reset; `App` registers `app = patient` and `app_language` (the person gets `app` too) and tells `Analytics` about demo mode; Profile → Settings → "Share usage data"
  (`ProfileViewModel.setShareUsageData`, saved as `analytics_opt_out` in plain Settings); the Profile language dialog
  re-identifies so the person's `language` follows.
- Debug menu → Analytics: your analytics id (`patient_<id>`, to add to PostHog's internal-users filter, since a
  debug build can't send a marker) and the session's last 50 events (`Analytics.recent`, debug builds only): the
  way to check events, since debug builds never send.
## Screens (ANL-03)

- `AnalyticsScreen` lists every `$screen` id (events.md §2); `TrackScreen(screen, properties…, step)` in each
  `XxxScreen` (never in a `Content`) sends it once when the screen appears, with the properties as they are then,
  and again only when `step` changes (Register, Forgot password). A tab shown again after an overlay closes isn't
  re-sent (events.md §2). The tabs are sent from `MainScreen` through `ShifaTab.analyticsScreen`; the video call
  only as the full screen.
- Properties as in events.md §2, computed by pure helpers in `AnalyticsScreen.kt` (`analyticsName`,
  `paymentBadge`, `appointmentState`, `fileType`), each with a unit test. Not sent: `source` on `doctor_profile`
  and `create_booking` (the screens don't know where they were opened from; the ANL-04 events carry it) and
  `is_certificate` / `has_visit` on `document_viewer` (a certificate and a chat file look the same to the viewer,
  and the visit loads after the screen appears; the `document_viewed` event will say).
- Not built: journey and engagement events (ANL-04/05), reliability events and exception autocapture (ANL-06), the
  `Flags` accessors (first flag in ANL-08; the sink methods exist).
