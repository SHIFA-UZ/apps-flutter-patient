# PostHog: product analytics for the patient app

How we track the patient's experience from the first launch to the end of a visit, with
[PostHog](https://posthog.com). Nothing here is built yet. These docs are the plan, the setup guide and the rules
for every feature that comes after.

| File | What's in it |
|---|---|
| [setup.md](setup.md) | Creating the PostHog projects, the keys, CI, and wiring the SDKs into this KMP app, step by step |
| [events.md](events.md) | The event catalog: every screen and event across the user journey, with properties and where each fires |
| [privacy.md](privacy.md) | What may and may never be sent. Health data rules, consent, Uzbekistan's personal data law, session replay |
| [dashboards.md](dashboards.md) | The funnels, retention charts and alerts to build in PostHog, and the questions they answer |
| [development.md](development.md) | How to add tracking to a new feature: code pattern, tests, review checklist, flags and experiments |

## Goals

1. **See the whole journey.** Install → sign up → first booking → the video visit → signing → tasks → coming back.
   Know where patients drop off and why.
2. **Measure the core flows.** Booking conversion, video join success, check-in adherence, Shifa AI usage, how
   patients respond to notifications.
3. **Catch problems early.** Failed bookings, failed video joins, API errors and slow AI replies, by platform and
   app version.
4. **Never leak health data.** Analytics records *what the patient did in the app*, never *what is wrong with them*.
   See [privacy.md](privacy.md). This rule beats every other goal.

## Decisions (proposed, need sign-off)

| # | Decision | Recommendation | Why |
|---|---|---|---|
| D1 | Hosting | PostHog Cloud **EU** (`https://eu.i.posthog.com`), *if legal clears it*; otherwise self-hosted in Uzbekistan | Uzbekistan's personal data law has a localization rule (privacy.md §5). The EU cloud has GDPR terms and a DPA. |
| D2 | SDK | PostHog's **native** SDKs (`posthog-android`, `posthog-ios`) behind our own small `Analytics` facade in `commonMain` | The official `posthog-kmp` SDK is 0.x, and its iOS linking needs Kotlin 2.4 (we're on 2.3.0). The facade follows the same pattern as Daily (`NativeVideoCall`). Switching to `posthog-kmp` later only changes the two platform files. |
| D3 | Identity | `distinct_id` = `patient_<P1 id>`, identified after login. Anonymous before that | Joins a user's events across reinstalls and devices without sending a name, phone or email |
| D4 | Projects | Two PostHog projects: **Shifa Patient (Dev)** and **Shifa Patient (Prod)** | Debug builds never pollute production numbers |
| D5 | Session replay | **Off** at launch | Compose screens hold health data, and iOS Compose can't be masked reliably (privacy.md §6) |
| D6 | Autocapture | Lifecycle events **on**, automatic screen views **off**, element autocapture **off** | We have one Activity and our own overlay navigation, so automatic screen views would all say "MainActivity". Screens are sent by hand. |
| D7 | Consent | On by default, with an opt-out switch in Profile → Settings, plus a line in the privacy policy | Legal decides whether it must be opt-in instead (privacy.md §4) |
| D8 | Demo data | Analytics is **off** while Debug → Demo data is on | Fake bookings and visits must never reach PostHog |

## Rollout plan

Ticket IDs continue the style in `docs/kmp-spec/16-implementation-plan.md`.

| Ticket | Scope | Depends on |
|---|---|---|
| ANL-01 | PostHog projects, keys, CI secrets, privacy and legal sign-off (setup.md §1-3, privacy.md) | - |
| ANL-02 | The SDKs and the facade: an Android `Application` class, the iOS Swift bridge, identify / reset, super properties, opt-out, demo-mode off switch (setup.md §4-6) | ANL-01 |
| ANL-03 | Screen tracking for every screen and overlay (events.md §2) | ANL-02 |
| ANL-04 | Core journey events: auth, booking, video visit, signing (events.md §3-6) | ANL-02 |
| ANL-05 | Engagement events: tasks, documents, messages, Shifa AI, notifications and push, profile (events.md §7-12) | ANL-02 |
| ANL-06 | Reliability events: `api_error`, failure reasons (events.md §13) | ANL-02 |
| ANL-07 | Dashboards, funnels and alerts in PostHog (dashboards.md) | ANL-04, data for 1-2 weeks |
| ANL-08 | Later: feature flags and experiments, surveys, a review of session replay (development.md §6) | ANL-07 |

ANL-02 needs an Android `Application` class. That's also the deferred move of `initSecureSettings` out of
`MainActivity`, so do both together.

## Status

Not started. When ANL tickets are built, add them to `docs/implemented/README.md` (status rows and a log line) and add
an `analytics.md` area file there, as for every other feature.
