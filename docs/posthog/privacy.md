# Privacy rules for analytics

Shifa is a medical app. Analytics may describe **how the app is used**. It must never describe **the patient's
health or identity**. When a rule here and a dashboard idea disagree, the rule wins: get the number from the backend's
database instead, where access control and the legal basis already exist.

## 1. The test for every property

Before a property is added, answer three questions. Any "yes" means it doesn't go to PostHog:

1. Could it say something about the patient's health? (A symptom, a diagnosis, a doctor's specialty, a
   measurement, a medication, the name of a document or task.)
2. Could it identify the patient or another person on its own? (A name, phone, email, address, birth date, photo,
   free text the user typed.)
3. Is it a record id other than the patient's own `distinct_id`? (Appointment, doctor, document, task, chat,
   message or notification ids.) Ids let anyone with backend access join analytics back to medical records.

## 2. Allowed

- Enum values we define (`reason`, `source`, `state`, `type`, `channel`…).
- Counts, durations, lengths and buckets (`query_length`, `results_count`, `duration_s`, `size_kb_bucket`).
- Coarse categories the product needs, reviewed one by one and listed in events.md: a task's `category` and
  `input_type`, a document's F2 `category`, a notification's `type` code, `is_video`.
- The person properties in events.md §1.2. Nothing else.

## 3. Never sent (anywhere: event, person, super property, screen property)

| Never | Examples |
|---|---|
| Identity | first / last / full name, phone, email, birth date or age, gender, address, photo URL, the P1 `id` except as `patient_<id>` in `distinct_id` |
| Free text | doctor and document searches (send `query_length`), booking reason, review comment, chat messages, Shifa AI questions, replies and transcripts, check-in notes and text values, signature images, profile fields' values |
| Health data | check-in values, task names and descriptions, doctor names / professions / specialties, diagnoses, visit summaries, document titles and file names, Shifa AI content |
| Other people | doctor ids and names, clinic names, the requester in a document access request |
| Record ids | appointment, document, task, check-in, chat, message, notification, payment ids |
| Secrets and transport | tokens, OTP codes, passwords, headers, request or response bodies, backend error messages, full URLs or query strings |
| Location | coordinates (the Near me fix), IP (discarded in project settings, setup.md §1) |

The review is easy because every event is a typed class in `AnalyticsEvent.kt` (setup.md §4). A PR that adds or
changes one gets the checklist in development.md §4.

## 4. Consent and opt-out

- **Default (proposed):** analytics on, pseudonymous, with an opt-out. Profile → Settings → "Share usage data"
  (a `ShifaSwitch`), saved in plain Settings (`analytics_opt_out`). Turning it off sends `analytics_opted_out`, then
  calls the SDK's `optOut()`; nothing more is sent until it's turned back on.
- The privacy policy (`https://shifa.uz/#privacy`, opened from Profile) must say what's collected, by whom (PostHog,
  the region), why, and how to opt out. Update the policy before the Prod key ships.
- **Legal decides** whether Uzbekistan's law requires explicit opt-in consent instead (see §5). If it does, the
  default flips to off, and a one-time consent prompt after login turns it on. The SDK starts opted out until then.
- iOS App Tracking Transparency: not needed. We don't track across other companies' apps or share data with data
  brokers, and PostHog doesn't use the IDFA. Declare "Product Interaction" / "Usage Data" (not linked to tracking)
  in the App Store privacy labels, and the same in Google Play's Data safety form ("App interactions", "Diagnostics").
- Account deletion: the team deletes the person in PostHog (Persons → the `patient_<id>` → Delete, which also
  deletes their events). Add this step to the account-deletion runbook; PostHog's API can automate it later.

## 5. Where the data lives: Uzbekistan's personal data law

- The Law of the Republic of Uzbekistan "On Personal Data" (ZRU-547, 2019), with its 2021 amendment
  (Article 27¹), requires personal data of Uzbekistan's citizens, when processed with information technology, to be
  stored on servers physically in Uzbekistan and registered in the state register of personal data bases. **This
  must be confirmed by a lawyer before any decision is final.**
- What this means for us:
  - Our events carry a pseudonymous `distinct_id` and no direct identifiers (§3). Legal must decide whether that is
    still personal data under the law (it can be linked back through our backend, so it probably is).
  - If it is, the options are: (a) **self-host PostHog in Uzbekistan** (the `distinct_id` and events stay in the
    country; check PostHog's current self-hosting guidance, since the free "hobby" deploy is meant for small
    volumes and has no support), or (b) send **no person identity at all**: skip `identify`, keep only anonymous
    ids, and lose cross-device and post-login joining.
  - If legal clears cross-border storage of pseudonymous data, use PostHog Cloud EU and sign PostHog's DPA.
- Health data is a special category in most laws. §3 keeps it out entirely, so the question above is only about the
  pseudonymous id.

## 6. Session replay

Off (decision D5). Reasons:

- iOS: Compose Multiplatform draws every screen into one Metal view, so PostHog's iOS replay can't see the text and
  images to mask them. It would need screenshot mode, which records the screen as is: health data in pictures.
- Android: replay sees Compose and masks text and images by default (`maskAllTextInputs`, `maskAllImages`), but a
  missed mask on any screen leaks health data, and every new screen would need a privacy check.

If product needs replay later (ANL-08): Android only, wireframe mode (not screenshot), all masking on, sampled, and
limited to the auth and booking screens that show no health data, with a legal review first.

## 7. Feature flags and surveys

- Flags may target person properties from events.md §1.2 only. Never upload cohorts built from backend health data.
- Surveys (ANL-08) must not ask health questions. Their answers are free text in PostHog, so ask about the app only.

## 8. Access and retention

- PostHog members: product, engineering leads, and support leads only. Remove leavers the day they leave.
- Exports: no CSV export of persons. Share insight links within the organization only.
- Retention: setup.md §1. Review this file once a year, and whenever a new kind of data is proposed.
