# From tracking to personalisation

What the data in [events.md](events.md) lets us do later, and what has to be in place now so that "later" is a
configuration change in PostHog, not a release. Nothing here is built; the plumbing is ANL-02, the first use is
ANL-08.

## 1. The idea, and the boundary

Personalisation here means **the app adapts to how a patient uses it**: which section Home shows first, which visit
type is pre-selected, which payment provider is pre-picked, when a reminder arrives, whether a first-week tip is
shown. It never means adapting to **what is wrong with the patient**. The analytics data holds no diagnoses,
measurements, task names or document titles (privacy.md §3), so health-based personalisation isn't possible from
PostHog, by design. If the product ever wants it (for example "diabetes patients see a glucose shortcut"), it
belongs in the backend, behind its access control and legal basis, not in analytics.

Four PostHog building blocks do the work:

| Block | What it is | We use it for |
|---|---|---|
| Person properties | Key-values on the patient (`has_booked`, `last_visit_type`) | Targeting flags, filtering insights |
| Cohorts | Saved groups built from events and properties ("checked in ≥ 3 times in 7 days") | Targeting flags, retention analysis, re-engagement lists |
| Feature flags | On/off or multivariate switches evaluated per patient, optionally with a JSON payload | Turning a personalisation on for a cohort; shipping it gradually; A/B tests |
| Surveys | In-app questions targeted like flags | Asking "why" after a flow, app questions only |

## 2. Behavioural person properties

Set by events (`userProperties` / `userPropertiesSetOnce` on the capture call, see setup.md §4), so they're always
current without a separate call. They're the vocabulary flags and cohorts target. All of them are allowed by
privacy.md §1 (usage facts, not health or identity).

| Property | Type | Set by | Meaning |
|---|---|---|---|
| `signed_up_at` | ISO date, once | `signup_completed` | Age of the account (first-week logic) |
| `last_login_at` | ISO date | `login_succeeded` | |
| `has_booked` | bool | `booking_completed`, `payment_completed` | Activated |
| `first_booking_at` | ISO date, once | same | Time to first booking |
| `last_visit_type` | `video` / `in_person` | same | Default for the video switch |
| `bookings_count` | int | same | Device-kept counter, see the note below |
| `has_paid` | bool | `payment_completed` | |
| `first_payment_at` | ISO date, once | `payment_completed` | |
| `last_payment_provider` | `click` / `stripe` | `payment_completed` | Pre-pick the provider |
| `payments_count` | int | `payment_completed` | |
| `has_video_visit` | bool | `video_joined` | Has actually been in a call |
| `first_video_visit_at` | ISO date, once | `video_joined` | |
| `camera_mic_granted` | bool | `video_permissions_result` | Show the permission explainer before the next call |
| `has_checked_in` | bool | `checkin_submitted` | Uses tasks |
| `first_checkin_at` | ISO date, once | `checkin_submitted` | |
| `checkins_count` | int | `checkin_submitted` | |
| `has_uploaded` | bool | `document_uploaded` | |
| `first_upload_at` | ISO date, once | `document_uploaded` | |
| `has_messaged` | bool | `chat_message_sent` | |
| `first_message_at` | ISO date, once | `chat_message_sent` | |
| `has_used_ai` | bool | `ai_message_sent` | |
| `first_ai_message_at` | ISO date, once | `ai_message_sent` | |
| `ai_messages_count` | int | `ai_message_sent` | |
| `has_reviewed` | bool | `review_submitted` | |
| `push_granted` | bool | `push_permission_result` | Whether push personalisation can reach them |
| `language`, `subscription_tier`, `shifa_ai_enabled`, `has_photo`, `has_email`, `has_phone`, `internal` | profile | `identify` (events.md §1.2) | |

**Counters:** PostHog has no "increment" for person properties. The `*_count` values are kept by the app in plain
`Settings` on the device and sent as the new total on each event, so they're right on one device and approximate
across devices. For exact counts, use a cohort ("performed `booking_completed` ≥ 3 times"); cohorts are computed
server-side from the events. Keep the counters only where a flag needs a cheap number without a cohort.

## 3. Cohorts

Built in PostHog (Data management → Cohorts), from events and the properties above, never from backend health data
(privacy.md §7). Starting set:

| Cohort | Definition | Used for |
|---|---|---|
| New this week | `signed_up_at` in the last 7 days | First-week tips, onboarding experiments |
| Activated | `has_booked = true` | Baseline for everything after activation |
| Booked, never visited | `has_booked = true` and no `video_joined` and no past `appointment_details_viewed` in 30 days | Pre-visit reminders, "how a video visit works" tip |
| Paying | `has_paid = true` | Payment UX changes |
| Payment drop-offs | `payment_checkout_opened` in 7 days and no `payment_completed` | Survey "what stopped you?", retry nudge |
| Task-active | `checkin_submitted` ≥ 3 times in 7 days | Tasks-first Home layout |
| Task-lapsing | `task_opened` in 14 days, no `checkin_submitted` in 7 days | Reminder timing experiment |
| AI users | `ai_message_sent` in 14 days | AI-first layout, AI shortcuts |
| AI-eligible, never tried | `shifa_ai_enabled = true` and `has_used_ai` **is not set** (the catalog only ever sets it to `true`; use PostHog's "is not set" operator, not `= false`) and `signed_up_at` > 7 days ago | AI intro tip |
| Chat users | `chat_message_sent` in 30 days | Messages tab prominence |
| Lapsed | no `Application Opened` in 30 days, but one in the 30 days before | Re-engagement (backend push campaign) |
| Video-preferring | `last_visit_type = video` | Video switch default on |
| Push-reachable | `push_granted = true` | Any push-based personalisation |
| Android / iOS | `$os` | Platform-specific rollouts |

## 4. Feature flags: the plumbing (ANL-02)

Flags are read through one object, with a typed accessor and a safe default per flag, so a missing network, a
deleted flag or a payload typo falls back to today's behaviour:

```kotlin
// data/analytics/Flags.kt
object Flags {
    /** Which Home section comes first. Payload: {"order": ["tasks","upcoming","my_doctors"]}. */
    val homeOrder: List<String>
        get() = Analytics.sink?.featureFlagPayloadJson("home-order")?.let(::parseOrder) ?: DEFAULT_HOME_ORDER

    /** Pre-select video on Create Booking for patients whose last visit was video. */
    val defaultVideoFromHistory: Boolean
        get() = Analytics.sink?.isFeatureEnabled("booking-default-video-from-history") ?: false
}
```

Rules:

- **Read flags in ViewModels**, never in composables, so previews and tests see the defaults and a flag can be
  faked in a ViewModel test.
- **Respect opt-out:** `Flags` reads through `Analytics.sink`, which is null while the patient has opted out or demo
  data is on (setup.md §4), so an opted-out patient gets the defaults, never a personalised variant (privacy.md §4).
- **Preload at launch** (`preloadFeatureFlags = true`) and **reload after `identify`**, because most targeting uses
  person properties and cohorts that only exist for the identified patient. Until the reload lands, the default
  applies; a flag that changes a screen the patient is already on must not flip it mid-view. Read it once when the
  ViewModel loads.
- **Names:** `kebab-case`, `<area>-<what>`: `home-order`, `booking-default-video-from-history`,
  `payment-preselect-last-provider`. Multivariate flags for experiments end in `-test`.
- **Payloads** are small JSON with a schema comment on the accessor. Parse defensively.
- **Lifecycle:** a rollout flag is removed from code and PostHog once it's at 100 % for two releases. Flags aren't
  config. An experiment flag is removed when the experiment ends, with the winning branch kept.
- **Targeting** uses only person properties from §2, cohorts from §3, `$os`, `$app_version` and the rollout
  percentage. Never a cohort uploaded from the backend (privacy.md §7). PostHog can't evaluate a flag against a
  cohort with an event-based condition (Task-active, AI users, Payment drop-offs, …), only against property-based
  ones, so a flag that needs "did X" targets the matching person property from §2 (`has_checked_in`,
  `has_used_ai`) or a counter.
- **Cost:** flag evaluations count against the 1,000,000 free requests a month (setup.md §1.6). One evaluation per
  launch and one per identify is about 2-4 per session, far below the limit.

### 4.1 Flags the backend can read too

Some personalisation lives on the backend (when a reminder push is sent, which notification types are sent). The
backend can evaluate the same flags for the same patient: PostHog's feature-flag API takes a `distinct_id`
(`patient_<id>`, which the backend knows) and returns the flags, and there is a Java SDK (`com.posthog.java:posthog`)
that caches definitions locally. That's a backend ticket when the first backend-side personalisation is chosen;
until then, every flag is app-side.

## 5. Experiments and surveys

- **Experiments** are multivariate flags with a primary metric from events.md, decided before the test starts, run
  for at least two full weeks (visits are weekly), on a cohort big enough for the effect size PostHog estimates.
  One experiment per screen at a time.
- **Surveys** (README D12): app questions only, never health questions (privacy.md §7), shown after a finished flow
  (`payment_abandoned`, `video_call_ended`, `review_submitted`), never during a visit, never more than once a month
  per patient. The native SDKs render them; `posthog-kmp` doesn't wrap them yet, which is another reason for D2.
  **Fixed choices only, no free-text fields** (privacy.md §7): "What stopped you from paying?" with four options and
  "Other", never an open text box, because wording can't stop a patient from typing medical details.

## 6. Round 1 candidates (ANL-08)

What the data supports first, after about four weeks of Prod events. Each one is a flag plus the data it reads;
none needs new tracking beyond events.md.

| # | Personalisation | Reads | Mechanism | Primary metric |
|---|---|---|---|---|
| P1 | **Video switch default** on Create Booking follows the patient's last visit type | `last_visit_type` | flag `booking-default-video-from-history` targeting cohort Video-preferring | `booking_visit_type_changed` rate (lower = better default), `booking_done` (dashboards.md: `booking_completed` or `payment_completed`) |
| P2 | **Payment provider pre-selected** to the one used last time | `last_payment_provider` | flag `payment-preselect-last-provider` targeting Paying | seconds from `payment_methods_loaded` to `payment_checkout_opened`; `payment_completed` ÷ `payment_checkout_opened` |
| P3 | **Home order**: Task-active patients see My Tasks first, AI users see an AI shortcut card, everyone else keeps today's order | cohorts Task-active, AI users | flag `home-order` with a payload per cohort | `home_section_tapped` on the first section; `checkin_submitted` per Task-active patient |
| P4 | **First-week tips**: a one-line card on Home for New this week ("Book your first visit", then "Turn on reminders") | cohort New this week, `has_booked`, `push_granted` | flag `home-first-week-tips`, payload = which tip | activation funnel (dashboards.md §1) |
| P5 | **Camera / mic explainer** before Join for patients who denied once | `camera_mic_granted = false` | flag `video-permission-explainer` | `video_permissions_result.granted` on the retry |
| P6 | **AI intro tip** for AI-eligible, never tried | cohort AI-eligible never tried | flag `ai-intro-tip` | `ai_message_sent` first-time rate |
| P7 | **Payment drop-off survey**: one fixed-choice question after `payment_abandoned` | cohort Payment drop-offs | survey, shown once | response count; then fixes |
| P8 | **Experiment: slot scan default day** (today vs the doctor's next available day as the pre-selected date) | - | flag `booking-scan-start-test` (2 variants) | `booking_done` ÷ `booking_started` |

## 7. Later rounds (need backend work or more data)

- **Reminder timing** per patient: `checkin_submitted.minutes_from_scheduled` and `hour_of_day` show when each
  cohort actually checks in; the backend's reminder job reads a flag payload (`reminder-offset-minutes`) per cohort
  (§4.1).
- **Re-engagement pushes** for Lapsed and Booked-never-visited: PostHog has the cohort; the backend sends the push.
  Needs a cohort export (PostHog API) or a flag the backend evaluates per patient.
- **Language default at signup** from the device locale, if `language_changed` right after signup is common.
- **Doctor list order** by what converts for similar patients (`booking_started.source`, chips used): only after
  volume, and only with usage signals, never with specialties the patient has visited (that's health data).
- **Review prompt timing**: a survey or nudge after `video_call_ended` with `duration_s > 60`, if
  `review_submitted` lags `appointment_details_viewed(past)`.

## 8. What we don't personalise from analytics

- Anything keyed on the doctor's specialty, the task's category or a document category as a proxy for a condition.
  `category` is allowed in events for product counts (how many VITAL tasks exist), not for targeting a patient.
- Prices or discounts per patient (`amount_bucket` is for funnels, not pricing).
- Content of Shifa AI replies (the backend owns the model and the prompt; analytics only times it).
