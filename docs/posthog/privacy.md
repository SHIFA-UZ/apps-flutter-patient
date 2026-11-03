# Privacy rules for analytics

Shifa is a medical app. Analytics may describe **how the app is used**. It must never describe **the patient's
health or identity**. When a rule here and a dashboard or personalisation idea disagree, the rule wins: get the
number from the backend's database instead, where access control and the legal basis already exist.

## 1. The test for every property

Before a property is added, answer three questions. Any "yes" means it doesn't go to PostHog:

1. Could it say something about the patient's health? (A symptom, a diagnosis, a doctor's specialty, a
   measurement, a medication, the name of a document or task, the exact price of a service.)
2. Could it identify the patient or another person on its own? (A name, phone, email, address, birth date, photo,
   free text the user typed.)
3. Is it a record id other than the patient's own `distinct_id`? (Appointment, doctor, document, task, chat,
   message, notification or payment ids, checkout URLs.) Ids let anyone with backend access join analytics back to
   medical records.

## 2. Allowed

- Enum values we define (`reason`, `source`, `state`, `type`, `channel`, `provider`, `payment_kind`…).
- Counts, durations, lengths and buckets (`query_length`, `results_count`, `duration_s`, `size_kb_bucket`,
  `amount_bucket`, `hour_of_day`).
- Coarse categories the product needs, reviewed one by one and listed in events.md: a task's `category` and
  `input_type`, a document's F2 `category`, a notification's `type` code, `is_video`, `currency`.
- The person properties in events.md §1.2 and personalisation.md §2: usage facts (`has_booked`, `last_visit_type`),
  never health facts.
- Exception types and stack traces from our own code (README D11), because `apiCall` strips backend text before it
  reaches an exception.

## 3. Never sent (anywhere: event, person, super property, screen property, exception message, survey answer)

| Never | Examples |
|---|---|
| Identity | first / last / full name, phone, email, birth date or age, gender, address, photo URL, the P1 `id` except as `patient_<id>` in `distinct_id` |
| Free text | doctor and document searches (send `query_length`), booking reason, review comment, chat messages and captions, Shifa AI questions, replies and transcripts, check-in notes and text values, signature images, profile fields' values, document titles |
| Health data | check-in values and answers, task names and descriptions, doctor names / professions / specialties, diagnoses, visit summaries, document titles and file names, Shifa AI content, the exact price of a service (it can reveal the service) |
| Other people | doctor ids and names, clinic names, the requester in a document access request |
| Record ids | appointment, document, task, check-in, chat, message, notification, payment and checkout ids |
| Secrets and transport | tokens, OTP codes, passwords, headers, request or response bodies, backend error messages, full URLs, query strings, checkout URLs |
| Location | coordinates, IP (discarded in project settings, setup.md §1.3) |

The review is easy because every event is a typed class in `AnalyticsEvent.kt` (setup.md §4). A PR that adds or
changes one gets the checklist in development.md §4.

## 4. Consent and opt-out

- **Default (proposed, README D7):** analytics on, pseudonymous, with an opt-out. Profile → Settings → "Share usage
  data" (a `ShifaSwitch`), saved in plain Settings (`analytics_opt_out`). Turning it off sends `analytics_opted_out`,
  then calls the SDK's `optOut()` **and** sets `Analytics.enabled = false`, so the facade returns no sink; nothing
  more is sent until it's turned back on (`analytics_opted_in` is then the first event). The facade guard is what
  stops feature flags too: the SDK's `optOut()` alone still answers flag reads, so without it personalisation would
  keep applying to a patient who opted out. With the guard, `Flags` sees no sink and the defaults apply.
- The privacy policy (`https://shifa.uz/#privacy`, opened from Profile) must say what's collected, by whom (PostHog,
  the EU region), why (improving the app, personalising it, fixing errors), and how to opt out. Update the policy
  before the key ships in a release build.
- **Decided 2026-11-03 (legal review done):** opt-out stays; no explicit opt-in consent is required (see §5). Had it
  gone the other way, the default would flip to off with a one-time consent prompt after login.
- iOS App Tracking Transparency: not needed. We don't track across other companies' apps or share data with data
  brokers, and PostHog doesn't use the IDFA. In the App Store privacy labels declare "Product Interaction" and
  "Crash Data" as **linked to the user** (Apple counts data tied to an account id, which `patient_<id>` is) and
  **not used for tracking** (Apple's "tracking" means cross-company). The same in Google Play's Data safety form:
  "App interactions", "Crash logs", "Diagnostics", collected, not shared, for analytics.
- Account deletion: the team deletes the person in PostHog (Persons → the `patient_<id>` → Delete, which also
  deletes their events and exceptions). Add this step to the account-deletion runbook; PostHog's API can automate
  it later from the backend's P6 handler.

## 5. Where the data lives: Uzbekistan's personal data law

- The Law of the Republic of Uzbekistan "On Personal Data" (ZRU-547, 2019), with its 2021 amendment
  (Article 27¹), requires personal data of Uzbekistan's citizens, when processed with information technology, to be
  stored on servers physically in Uzbekistan and registered in the state register of personal data bases. **This
  must be confirmed by a lawyer before any decision is final.** Confirmed 2026-11-03: the user's legal review cleared
  PostHog Cloud EU with pseudonymous ids, so the options below are kept only as the record of what was weighed.
- What this means for us:
  - Our events carry a pseudonymous `distinct_id` and no direct identifiers (§3). Legal must decide whether that is
    still personal data under the law (it can be linked back through our backend, so it probably is).
  - If it is, the options are: (a) **self-host PostHog in Uzbekistan** (the `distinct_id` and events stay in the
    country; check PostHog's current self-hosting guidance, since the free "hobby" deploy is meant for small
    volumes and has no support), or (b) send **no person identity at all**: skip `identify`, keep only anonymous
    ids, and lose cross-device joining, person properties and most of personalisation.md.
  - If legal clears cross-border storage of pseudonymous data, use PostHog Cloud EU and sign PostHog's DPA
    (Settings → Organization → Data processing agreement).
- Health data is a special category in most laws. §3 keeps it out entirely, so the question above is only about the
  pseudonymous id.

## 6. Session replay

Off (README D5). Reasons:

- iOS: Compose Multiplatform draws every screen into one Metal view, so PostHog's iOS replay can't see the text and
  images to mask them. It would need screenshot mode, which records the screen as is: health data in pictures.
- Android: replay sees Compose and masks text and images by default (`maskAllTextInputs`, `maskAllImages`), but a
  missed mask on any screen leaks health data, and every new screen would need a privacy check.

If product needs replay later: Android only, wireframe mode (not screenshot), all masking on, sampled, and limited
to the auth and booking screens that show no health data, with a legal review first.

## 7. Feature flags, cohorts and surveys

- Flags and cohorts may target the person properties in personalisation.md §2, the cohorts in §3, `$os` and
  `$app_version` only. Never upload cohorts built from backend health data, and never target by a task or document
  `category` (personalisation.md §8).
- Surveys must not ask health questions and must use **fixed choices only** (single or multiple choice, ratings).
  No free-text question type: §3 bans free text, and no wording can stop a patient from typing medical details
  into an open box.

## 8. Error tracking

- Exception autocapture (README D11) sends the exception type, message and stack trace. Our exceptions' messages
  are display-safe already (`sanitizeError`); a new exception class that wraps backend text or a URL must strip it
  first.
- Never `captureException` with a message built from user input, a request body or a response body.
- Breadcrumbs are our own events; PostHog shows the events before the crash. Nothing extra is sent.

## 9. Access and retention

- PostHog members: product, engineering leads, and support leads only. Remove leavers the day they leave.
- Exports: no CSV export of persons. Share insight links within the organization only.
- Retention: the plan's 1 year (setup.md §1.3). Review this file once a year, and whenever a new kind of data is
  proposed.
- Debug builds never send (README D4), so emulator and test-account activity never becomes data to protect.
