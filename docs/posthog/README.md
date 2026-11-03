# PostHog: product analytics for the patient app

How we learn what every patient does in the app, from the first launch to the end of a visit, with
[PostHog](https://posthog.com). Nothing is built yet. These docs are the plan, the setup guide and the rules for
every feature that comes after.

Rewritten on 2026-11-03 against the app as it is on `main` (v1.2.4). The first version (2026-10-19) predates
booking payments, voice and file messages, the Shifa AI redesign, the Development backend switch and the removal of
Near me and App Lock. See [What changed since the first plan](#what-changed-since-the-first-plan).

| File | What's in it |
|---|---|
| [setup.md](setup.md) | **Start here if PostHog doesn't exist yet.** Creating the account, organization and projects, the settings, the keys, billing, then wiring the SDKs into this KMP app and verifying it, step by step |
| [events.md](events.md) | The event catalog: every screen, every user action and every outcome, feature by feature, with a coverage matrix so nothing a patient can do goes untracked |
| [personalisation.md](personalisation.md) | How the tracked data becomes personalisation later: person properties, cohorts, feature flags with payloads, experiments and surveys, with the concrete candidates for Shifa |
| [privacy.md](privacy.md) | What may and may never be sent. Health data rules, consent, Uzbekistan's personal data law, session replay |
| [dashboards.md](dashboards.md) | The funnels, retention charts and alerts to build in PostHog, and the questions they answer |
| [development.md](development.md) | How to add tracking to a new feature: code pattern, tests, review checklist |

## Goals

1. **Know 100 % of behaviour.** Every screen a patient sees, every action they take, and whether it worked. The
   rule in events.md: if a patient can tap it, there is a row for it. Install → sign up → first booking → payment →
   the video visit → signing → tasks → coming back.
2. **Measure the core flows.** Booking and payment conversion, video join success, check-in adherence, Shifa AI
   usage, how patients respond to notifications.
3. **Catch problems early.** Failed bookings and payments, failed video joins, API errors, crashes and slow AI
   replies, by platform and app version.
4. **Be ready to personalise.** The data model (identity, person properties, cohorts) and the feature-flag
   plumbing are designed now, so personalisation later is configuration, not a rewrite. See personalisation.md.
5. **Never leak health data.** Analytics records *what the patient did in the app*, never *what is wrong with
   them*. See [privacy.md](privacy.md). This rule beats every other goal.

## Decisions

Dated 2026-11-03. D1 and D7 were signed off by the user on 2026-11-03 (legal review done: EU hosting and opt-out are fine).

| # | Decision | Recommendation | Why |
|---|---|---|---|
| D1 | Hosting | PostHog Cloud **EU** (`https://eu.i.posthog.com`), *signed off 2026-11-03* | Uzbekistan's personal data law has a localization rule (privacy.md §5). The EU cloud has GDPR terms and a DPA. |
| D2 | SDK | PostHog's **native** SDKs (`posthog-android` 3.75.1, `posthog-ios` 3.93.3, both released 2026-11-02) behind our own small `Analytics` facade in `commonMain` | The official `posthog-kmp` is still 0.x (0.10.0, 2026-10-27) and its iOS linking needs Kotlin 2.4.0+; we're on Kotlin 2.3.0. The facade follows the Daily pattern (`NativeVideoCall`). Switching to `posthog-kmp` after the Kotlin upgrade only changes the two platform files. |
| D3 | Identity | `distinct_id` = `patient_<P1 id>`, identified after login. Anonymous before that | Joins a patient's events across reinstalls and devices without sending a name, phone or email |
| D4 | Projects and builds | One PostHog organization **Shifa** with one project, **Shifa**, fed by **release builds only**. Debug builds carry no key and never send anything (the user's decision, 2026-11-03: no development data is needed). The doctor app shares the project, told apart by `app = doctor` and `doctor_<id>` distinct ids, until a paid plan makes a second project worth it (setup.md §1.7) | One project fits the free plan; nothing from emulators, test accounts or the development backend ever lands in the numbers. Events are checked in debug builds through the Debug menu's event list, and in PostHog from a TestFlight / internal-track build on an account listed in the internal-users filter; see setup.md §1.4, §1.6-1.7 and §7 |
| D5 | Session replay | **Off** at launch | Compose screens hold health data, and iOS Compose can't be masked reliably (privacy.md §6) |
| D6 | Autocapture | Lifecycle events **on**, automatic screen views **off**, element autocapture **off** | One Activity and one `UIViewController`, with our own overlay navigation: automatic screen views would all say `MainActivity`. Screens are sent by hand (events.md §2). |
| D7 | Consent | On by default, with an opt-out switch in Profile → Settings, plus a line in the privacy policy. *Signed off 2026-11-03*: opt-out, not opt-in | privacy.md §4 |
| D8 | Demo data | Analytics is **off** while Debug → Demo data is on: the facade is disabled **and** the SDK is opted out, so its automatic lifecycle and exception events stop too. (Belt and braces: demo data exists only in debug builds, which have no key anyway, D4) | Fake bookings and visits must never reach PostHog |
| D9 | Development backend | Nothing to do: only debug builds can switch to `api-dev.shifa.uz`, and debug builds never send (D4) | No `backend` property is needed |
| D10 | Feature flags | Plumbing built in ANL-02 (`Flags` object, flags preloaded at launch, safe defaults). No flag used until personalisation.md says so | Flags are how personalisation and experiments ship later without a release per change |
| D11 | Error tracking | PostHog exception autocapture **on** (ANL-06), replacing the never-wired Crashlytics | A crash then sits in the same timeline as the taps before it. `apiCall` already sanitizes error messages; privacy.md §3 applies to anything thrown. |
| D12 | Surveys | Off until the first personalisation round (ANL-08) | App questions only, never health questions (privacy.md §7) |

## What changed since the first plan

The 2026-10-19 plan was written before these shipped. events.md covers them now.

| Shipped | When | Analytics impact |
|---|---|---|
| Booking payments (BOOK-04): payment methods, Click / Stripe, the hosted checkout in a WebView with a hold countdown, B4 polling, release on close | 2026-10-20, fixed 2026-10-30 | New §5 Payments: `payment_*` events, `payment_kind` and `payment_provider` on bookings, a Payments dashboard |
| Voice messages played in chats; voice and PDF sending in chats (CHAT-03/04) | 2026-10-20 | `chat_message_sent.type` gains `voice` / `document`; `chat_voice_played` |
| Shifa AI redesign: Copy / Regenerate / Share on replies; the transcript is sent as the question at once | 2026-10-24 | `ai_reply_action`; `ai_message_sent.input` is `typed` / `voice` (no `voice_edited`) |
| Doctor services with descriptions; Select Clinic and Select Service on the booking page; the slot scan starts on the doctor's next available day | 2026-10-24, 2026-10-30 | `doctor_service_expanded`, `booking_location_selected`, `booking_service_selected`, `booking_slots_loaded.scan_start` |
| Document viewer: From Appointment card, tap to expand | 2026-10-24 | `document_visit_opened`, `document_expanded` |
| Near me removed; App Lock dropped; the AI visit-summary page dropped (summaries open the Documents tab) | 2026-10-19 to 2026-10-24 | `near_me_result`, the `near_me` chip and `coming_soon_tapped.feature = app_lock` are gone; `push_opened.routed_to` gains `documents_tab` and `live_appointment` |
| Notification inbox paged (`/notifications/page`, N1 fallback), Payments filter chip | 2026-09-28, 2026-10-30 | `notifications_page_loaded`; `notification_filter_selected.filter` gains `payments` |
| Debug menu: Development backend switch, also on Login | 2026-10-30 | D9: nothing to track, debug builds never send |
| Task notification's Submit Check in opens today's PENDING check-in only | 2026-11-01 | `checkin_nothing_due` fires more precisely |
| PostHog itself: error tracking and surveys matured on the native SDKs; `posthog-kmp` reached 0.10.0 (still 0.x) | 2026 | D11, D12, D2 versions |

## Rollout plan

Ticket IDs continue the style in `docs/kmp-spec/16-implementation-plan.md`.

| Ticket | Scope | Depends on |
|---|---|---|
| ANL-01 | PostHog account, organization, the project, settings, the key, the CI secret, privacy and legal sign-off (setup.md §1-3, privacy.md) | - |
| ANL-02 | The SDKs and the facade: an Android `Application` class, the iOS Swift bridge, identify / reset, super properties, opt-out, the demo-mode off switch, the `Flags` object (setup.md §4-6) | ANL-01 |
| ANL-03 | Screen tracking for every screen and overlay (events.md §2) | ANL-02 |
| ANL-04 | Core journey events: auth, doctors and booking, payments, the live visit and video, signing (events.md §3-6) | ANL-02 |
| ANL-05 | Engagement events: tasks, documents, messages, Shifa AI, notifications and push, profile (events.md §7-12) | ANL-02 |
| ANL-06 | Reliability: `api_error`, `api_slow`, exception autocapture (events.md §13) | ANL-02 |
| ANL-07 | Dashboards, funnels, cohorts and alerts in PostHog (dashboards.md) | ANL-04, ANL-05, data for 1-2 weeks |
| ANL-08 | Personalisation round 1: behavioural person properties, the first flags and one experiment, the first survey (personalisation.md §6) | ANL-07, data for 4 weeks |

ANL-02 needs an Android `Application` class. That's also the deferred move of `initSecureSettings` out of
`MainActivity`, so do both together.

## Status

Not started. When ANL tickets are built, add them to `docs/implemented/README.md` (status rows and a log line) and add
an `analytics.md` area file there, as for every other feature.
