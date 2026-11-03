# Analytics (PostHog)

The plan: [../posthog/README.md](../posthog/README.md). This file records what of it is built.

## Foundation (`data/analytics/`, ANL-02)

- `Analytics` is the one way in: `track(event)`, `identify(profile)`, `reset()`, `register(key, value)`,
  `setOptedOut(out, profile)`, `setDemo(on)`, and `start(optedOut, demo)` for the platform's launch (both guards
  at once). Super properties are kept and re-registered after `reset`, which the SDKs would otherwise drop. Its `platform` is the SDK (`PostHogSink` on Android,
  `PostHogAnalytics.swift` on iOS, registered through `AnalyticsBridge.kt`); `sink` is null while opted out or in demo
  mode, so events and flag reads stop, while `reset` and `register` go through regardless (setup.md §4).
- `AnalyticsEvent` is the catalog in code (events.md). Built so far: `analytics_opted_out`, `analytics_opted_in`,
  `debug_marked_internal`. `AnalyticsEventTest` checks names and keys against privacy.md §3.
- Keys: debug reads `posthog.apiKey.dev` from `local.properties`; release reads the Gradle property
  `posthog.apiKey.prod` (Play workflow) or `POSTHOG_API_KEY_PROD` in `iosApp/Configuration/Secrets.xcconfig` (written by
  the TestFlight workflow; a local `POSTHOG_API_KEY_DEV` there feeds Debug builds only), both from the
  `POSTHOG_API_KEY_PROD` secret. No key: `platform` stays null, nothing runs.
- SDK config (setup.md §5): lifecycle events on, screen views / deep links / element interactions / push capture off,
  session replay off, identified-only person profiles, flags preloaded unless starting opted out or in demo mode (the
  SDK would otherwise still ask PostHog for flags), `debug` in debug builds. Known: the SDK's `reset()` reloads flags
  regardless, so a logout while opted out makes one anonymous flags request. Error tracking
  autocapture is ANL-06.
- Hooks: `HomeViewModel.show` identifies (`patient_<id>` with the events.md §1.2 person properties);
  `AuthRepository.logout`, the session-restore 401 and the reinstall wipe reset; `App` registers `app_language`
  (and `backend` in debug builds) and tells `Analytics` about demo mode; Profile → Settings → "Share usage data"
  (`ProfileViewModel.setShareUsageData`, saved as `analytics_opt_out` in plain Settings); the Profile language dialog
  re-identifies so the person's `language` follows.
- Debug menu → Analytics: whether a key is built in, "Mark me as internal" (`internal = true`), the session's last 50
  events (`Analytics.recent`, debug builds only).
- Not built: screens (`TrackScreen`, ANL-03), journey and engagement events (ANL-04/05), reliability events and
  exception autocapture (ANL-06), the `Flags` accessors (first flag in ANL-08; the sink methods exist).
