# Dashboards, funnels and alerts

What to build in PostHog (ANL-07) once the events in [events.md](events.md) have collected 1-2 weeks of data. Build
everything in **Prod**, with the "Filter out internal and test users" filter on. Name insights with the dashboard
prefix (`Journey · Activation funnel`), so search finds them.

## 1. Dashboard: Journey (the whole experience)

The main dashboard: from install to coming back.

| Insight | Type | Definition | Answers |
|---|---|---|---|
| Activation funnel | Funnel, 7-day window | `Application Installed` → `signup_started` → `signup_code_sent` → `signup_completed` → `booking_completed` | Where new users drop off before their first booking |
| Login vs signup | Trends | `login_succeeded`, `signup_completed`, `password_reset_completed` per day | Growth and how often people get stuck signing in |
| First-visit funnel | Funnel, 30-day window | `booking_completed` → `live_appointment_viewed` → `video_joined` → `video_call_ended` (duration_s > 60) | Do bookings turn into real visits? |
| Weekly retention | Retention | Start: `signup_completed`; return: any of `booking_completed`, `checkin_submitted`, `ai_message_sent`, `chat_message_sent`, `document_viewed` | Do patients come back after signing up? |
| Stickiness | Stickiness | `Application Opened`, days per week | How habitual the app is |
| Feature reach | Trends, unique users, weekly | The first event of each area: `booking_started`, `video_joined`, `checkin_submitted`, `ai_message_sent`, `chat_message_sent`, `document_uploaded` | Which features people actually use |
| Path to booking | User paths | Starting at `home` screen, ending at `booking_completed` | The real routes patients take |
| Platform / version split | Trends, broken down by `$os`, `$app_version` | Key outcome events | Regressions after a release |

## 2. Dashboard: Booking

| Insight | Type | Definition |
|---|---|---|
| Booking funnel | Funnel, 1-hour window | `doctor_profile_viewed` → `booking_started` → `booking_slots_loaded` → `booking_completed` |
| By source | Funnel above, broken down by `booking_started.source` | Which entry point converts |
| Failure reasons | Trends | `booking_failed` by `reason` |
| No slots | Trends | `booking_slots_loaded` where `days_to_first_slot` is none, by `is_video` |
| Video vs in person | Trends | `booking_completed` by `is_video` |
| Lead time | Trends, median | `booking_completed.days_ahead` |
| Search quality | Trends | `doctor_search_performed` where `results_count = 0`, as a share of all searches |
| Near me | Trends | `near_me_result` by `result` |
| Cancellations and reschedules | Trends | `appointment_cancelled` (median `hours_ahead`), `appointment_rescheduled` |
| Reviews | Trends | `review_submitted` by `rating`; funnel past `appointment_details_viewed` → `review_started` → `review_submitted` |

## 3. Dashboard: Video visit and signing

| Insight | Type | Definition |
|---|---|---|
| Join funnel | Funnel, 1-hour window | `video_call_opened` → `video_permissions_result` (granted = true) → `video_join_tapped` → `video_joined` → `video_doctor_joined` |
| Join failures | Trends | `video_join_failed` by `reason`, broken down by `$os` |
| Waiting for the doctor | Trends, median and p90 | `video_doctor_joined.waited_s` |
| Join speed | Trends, p90 | `video_joined.join_ms` |
| Call length | Trends, median | `video_call_ended.duration_s` where `doctor_joined = true` |
| Who ends calls | Trends | `video_call_ended` by `ended_by` |
| Small window use | Trends | `video_call_minimized`; share of calls with `was_minimized` |
| Signing funnel | Funnel | `live_appointment_viewed` (signature_due = true) → `signature_started` → `signature_submitted` |
| Signing during calls | Trends | `signature_submitted` by `during_call` |

## 4. Dashboard: Tasks and adherence

| Insight | Type | Definition |
|---|---|---|
| Check-ins per active patient | Trends, weekly | `checkin_submitted` count ÷ unique users with `task_opened` |
| Reminder → check-in | Funnel, 2-hour window | `push_opened` (type = `TASK_REMINDER`) → `checkin_submitted` |
| Inbox → check-in | Funnel | `notification_action_tapped` (action = `submit_checkin`) → `checkin_submitted` (source = `notification`) |
| On time | Trends, median | `checkin_submitted.minutes_from_scheduled` |
| By task type | Trends | `checkin_submitted` by `category`, `input_type` |
| Form friction | Trends | `checkin_validation_failed` by `field`, `rule` |

## 5. Dashboard: Shifa AI

| Insight | Type | Definition |
|---|---|---|
| Usage | Trends, unique users | `ai_message_sent` per day; average messages per chat |
| Reply speed | Trends, median and p90 | `ai_reply_received.first_token_ms`, `total_ms` |
| Failures | Trends | `ai_reply_failed` by `reason`; `ai_reply_abandoned` |
| Voice | Trends | `ai_message_sent` by `input`; `ai_voice_failed` by `reason`; median `ai_voice_transcribed.transcribe_ms` |
| Rate limit | Trends | `ai_reply_failed` (reason = `rate_limit`), unique users, by `subscription_tier` |
| Demand for attachments | Trends | `ai_coming_soon_tapped` by `feature` |
| AI → booking | Funnel, 1-day window | `ai_message_sent` → `booking_completed` (when AI booking, AI-03, exists) |

## 6. Dashboard: Notifications, chat and documents

| Insight | Type | Definition |
|---|---|---|
| Push open rate | Trends, formula | `push_opened` ÷ `push_received`, by `type` |
| Push permission | Trends | `push_permission_result` by `granted`, by `$os` |
| Inbox engagement | Funnel | `notifications_viewed` → `notification_opened` → `notification_action_tapped` |
| Chat activity | Trends | `chat_message_sent` by `type`; `chat_opened` by `source` |
| Uploads | Trends | `document_uploaded` by `source`, `category`; `document_upload_failed` by `reason` |
| Viewer | Trends | `document_viewed` by `source`, `uploaded_by`; `document_view_failed` by `reason` |

## 7. Dashboard: Reliability

| Insight | Type | Definition |
|---|---|---|
| API errors | Trends | `api_error` by `endpoint`, `status` |
| Errors per user | Trends, formula | `api_error` ÷ unique users, by `$app_version` |
| Slow calls | Trends | `api_slow` by `endpoint`, p90 `ms` |
| Session restore | Trends | `session_restored` by `result` (spikes in `expired` or `timeout` mean an auth or backend issue) |

## 8. Alerts

Set on the insights above (PostHog → insight → Alerts), to the team's channel:

| Alert | Condition |
|---|---|
| Booking failures | `booking_failed` daily count > 2× its 7-day average |
| Video joins failing | `video_join_failed` ÷ `video_join_tapped` > 20 % in an hour (when volume allows) |
| API errors after a release | `api_error` per user for the newest `$app_version` > 2× the previous version's |
| Shifa AI down | `ai_reply_failed` (reason ≠ `rate_limit`) > 10 in an hour |
| Login trouble | `login_failed` (reason = `server` or `network`) > 2× its average |

## 9. Cohorts

Useful saved cohorts (built from events, never from backend health data, privacy.md §7):

- **New this week**: `signup_completed` in the last 7 days.
- **Booked, never visited**: `booking_completed` but no `video_joined` in 30 days.
- **Active on tasks**: `checkin_submitted` in the last 7 days.
- **Lapsed**: no `Application Opened` in 30 days, but one in the 30 days before.
- **AI users**: `ai_message_sent` in the last 14 days.
