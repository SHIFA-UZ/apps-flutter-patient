# Analytics (PostHog)

The plan: [../posthog/README.md](../posthog/README.md). This file records what of it is built.

## The PostHog project (ANL-01, 2026-11-03)

- Organization **SHIFA** on PostHog Cloud EU, project **Shifa** (id 332380). Settings applied through the API:
  timezone `Asia/Tashkent`, client IPs discarded, session replay off, autocapture / heatmaps / web vitals / console
  logs / performance off, surveys off, exception autocapture off (ANL-06 turns it on), the internal-users filter on
  by default for new insights, weeks start on Monday. Person profiles "identified only" is set in the SDK.
- `POSTHOG_API_KEY_PROD` is in GitHub (2026-11-03) and legal signed off on EU hosting and opt-out (README D1, D7).
  Still to do: the team's `patient_<id>`s in the internal-users filter (setup.md §1.4), the privacy policy text, the
  store privacy labels (setup.md §8).

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
## Journey events (ANL-04)

- events.md §3-6 as `AnalyticsEvent` data classes, fired from the ViewModels after the outcome: session restore
  and logout (`AuthRepository`, with `logout(source)`), login, the signup wizard (steps, conflicts, codes, the SMS
  fallback, completion, abandonment), password reset, Home (refresh, section taps), the Bookings tab (chips, typed
  searches, list arrows), the doctor profile (view, tabs, services, contacts, certificates), Create Booking and
  Reschedule (every choice, the slot scan, the outcome, Back as an abandonment with the step reached), payments
  (methods, provider, checkout, check, redirects, completion by `via`, failure, abandonment, Book again), the
  details page (view, toggle, cancel, contact, review), the live page (view, toggle, signing) and the video call
  (open, permissions, join, doctor joined, controls, small window, end with who ended it).
- Person properties set by the outcomes: `has_booked`, `last_visit_type`, `first_booking_at`, `has_paid`,
  `last_payment_provider`, `first_payment_at`, `has_reviewed`, `camera_mic_granted`, `has_video_visit`,
  `first_video_visit_at`, `signup_method`, `signed_up_at`. The `*_count` counters from personalisation.md are not
  kept (cohorts count events exactly).
- `PaymentReports` keeps one `payment_completed` per booking across the payment page's poll and the booking page's
  close-time check, whichever sees PAID first.
- Helpers in `AnalyticsEvent.kt`: `analyticsReason()` (an `ApiException` as a fixed reason), `amountBucket`,
  `hoursAhead`, `daysAhead`, `hourOfDay`, `paymentFailureReason`; `CreateBookingUiState.paymentKind`.
- Left out, with the reason: `source` on `doctor_profile_viewed` / `booking_started` / `appointment_details_viewed`
  / `video_call_opened` (the ViewModels don't know the route; the `$screen` sequence shows it), `seconds_on_page`
  and `signup_abandoned.seconds_spent`, `during_call` and `strokes` on signing, `via` on `payment_abandoned`,
  `results_count` on `doctor_filter_selected`, `slots_count` on `booking_date_picked`, `bookings_refreshed`,
  `has_price` / `payment_kind` on `doctor_service_expanded`, `is_reschedule` on the booking choice events (the
  `reschedule` screen says it), `scan_days`, `booking_reason_entered`, `session_restored = timeout` (the splash's
  cap cancels the call), Home's `target`: the task card and arrow share one callback (both `card`) and an Upcoming
  card's Join opens the details (`card`),
  `appointment_end_tapped` (no such control reaches the ViewModel), `login_locked_out` fires on the failure that
  locks, `video_join_failed.reason` is the API reason or `daily_failed` (no `not_in_window` / `payment_required`
  mapping; the backend's message isn't sent).
## Engagement events (ANL-05)

- events.md §7-12 from the ViewModels and the pure-UI taps in the screens: My Tasks and the task page (view, refresh,
  month, day, a check-in's validation, outcome and abandonment, nothing due), Documents (refresh, chip, the upload
  tray's source through `pick(..., source)`, upload outcome, viewer open / expand / visit / download / delete),
  Messages (refresh, doctor search, chat opened, sends by type with duration and size buckets, upload and send
  failures, recording, voice played, attachment opened), Shifa AI (chat started, question by input and length,
  reply timing, failures and the rate limit, abandonment, Copy / Regenerate / Share, voice, delete, the gated tab),
  push (permission, token, foreground arrival with its `type`, tap with `routed_to`) and the inbox (view, pages,
  refresh, chips, card, tray button, mark all), Profile (refresh, rows, language, photo, edit outcome with the
  changed field names, password, account deletion steps, privacy, Coming soon, Retry).
- `Push.arrived` now carries the push's `type` (Android service, iOS `onPushArrived(type:)`), analytics only.
- Person properties set: `has_checked_in` / `first_checkin_at`, `has_uploaded` / `first_upload_at`,
  `has_messaged` / `first_message_at`, `has_used_ai` / `first_ai_message_at`, `push_granted`, `has_photo`.
- Left out, with the reason: `documents_searched` and `ai_searched` (both filter locally on every keystroke; the
  Documents and Shifa AI screens show the result), `chat_opened.source` (the ViewModel doesn't know the route),
  `chat_voice_played.is_own` (the player gets a URL), `coming_soon_tapped` on the Shifa AI tray is `ai_attachment`
  (one callback for camera, photo and file), `notification_action_tapped` has `checkin_tray` for the check-in
  button, `notification_link_missing`, `push_opened.cold_start` / `was_signed_out`, `task_opened.progress_pct`
  when the task has no progress, `retry_tapped` on Messages and Notifications (their Retry is the refresh),
  `source` on `tasks_viewed` / `task_opened` / `checkin_submitted` / `document_viewed` / `notifications_viewed` (the
  ViewModels don't know the route), `chat_opened.unread_count`, `chat_voice_played.duration_s_bucket`,
  `notification_opened.has_link` and `notification_action_tapped.action` (`routed_to` says where it went),
  `reason` on `document_download_failed` and `chat_recording_failed` (the platform reports no cause).
  `notifications_all_read.unread_count` counts the pages loaded so far, not the whole inbox (N4 clears it all).
  `document_viewed` waits for both the file and the visit lookup, so `has_visit` is final; an unsupported file is a
  `document_view_failed` with `unsupported`.
## Reliability (ANL-06)

- `api_error` from both failure branches of `apiCall` (and an unreadable body), with the path as a template
  (`endpointTemplate`: numeric, UUID-like and long segments become `{id}`), the method, the status, the
  `ApiException` kind and the backend's `code`; never the message. A call with no response has no endpoint (there
  is no request object in that branch), only the exception's class. `api_slow` for a successful call over 5 s.
- `ai_stream_error` from `CopilotRepository.stream`: `connect` with the status for a non-200, `stream` for a
  dropped stream. `webview_error` from the payment WebView's main-frame load failure on both platforms, with the
  provider and the platform's error code (iOS reports the provisional-navigation failure only).
- Exception autocapture is on in both SDK configs (README D11) and in the PostHog project. No "Crash now" row:
  release builds have no Debug menu and debug builds never send, so it couldn't verify anything.
- Not built: the `Flags` accessors (first flag in ANL-08; the sink methods exist).

## Dashboards, cohorts and alerts (ANL-07, 2026-11-03)

Created in the Shifa project through the API, empty until the first release build sends events:

- **Nine dashboards**, one per dashboards.md section, 122 insights in all: Patient · Journey (activation funnel,
  login vs signup, first-visit funnel, weekly retention, stickiness, feature reach, path to booking, platform /
  version / language splits), Booking, Payments, Video visit, Tasks, Shifa AI, Notifications chat and documents,
  Reliability, and Personalisation (one insight per flag, broken down by `$feature/<flag>`, empty until a flag is
  on). Insight names are `<dashboard> · <title>`. Two actions stand in for event groups: **Booking done**
  (`booking_completed` or `payment_completed`) ends every booking funnel, **Came back** (booking, payment, check-in,
  Shifa AI, chat, document viewed) is the retention return event. Ratios use formulas (`A/B`), timings use median
  and p90 of the event's `*_ms` / `*_s` property. Plan rows that need a property the app doesn't send (`during_call`,
  `cold_start`, `chat_opened.source`, `turn` averages, crash-free sessions) are left out, as analytics.md records.
- **Sixteen cohorts**: the fourteen from personalisation.md §3 plus Android and iOS. "Lapsed" uses PostHog's "stopped
  performing" condition (opened in the 30 days before, not in the last 30).
- **Five alerts**, the free plan's limit, with fixed daily bounds (dashboards.md §10), mailed to the project owner.
- A local release build is the way to test before a store release (setup.md §7.1); `composeApp/build.gradle.kts`
  reads `posthog.apiKey.prod` from `local.properties` when CI's property is absent and debug-signs the release
  type when there is no `keystore.properties`.

## Flags (ANL-08, started 2026-11-03)

- The seven round-1 flags from personalisation.md §6 exist in PostHog, **inactive at 0 %**, targeting the §3
  cohorts or person properties: `booking-default-video-from-history` (Video-preferring),
  `payment-preselect-last-provider` (Paying), `home-order` (multivariate `tasks_first` / `ai_card` on
  `has_checked_in` / `has_used_ai`, with an `{"order": [...]}` payload per variant, because flags can't target
  event-based cohorts), `home-first-week-tips` (New this week, `{"tip": ...}` payload), `video-permission-explainer`
  (`camera_mic_granted = false`), `ai-intro-tip` (AI-eligible never tried), `booking-scan-start-test` (variants
  `control` / `next_available`). P7 is a survey, off until surveys are decided (README D12).
- Not built yet: the `Flags` accessors and the ViewModel reads. They come one personalisation at a time, each on its
  own branch, once the user picks which to wire; until then the flags change nothing.
