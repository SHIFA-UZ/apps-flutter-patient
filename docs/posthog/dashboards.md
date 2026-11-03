# Dashboards, funnels, cohorts and alerts

What to build in PostHog (ANL-07) once the events in [events.md](events.md) have collected 1-2 weeks of data. Build
everything in **Prod**, with the "Filter out internal and test users" filter on. Name insights with the dashboard
prefix (`Journey · Activation funnel`), so search finds them.

`booking_done` below means "`booking_completed` OR `payment_completed`": a pay-now booking's outcome is the payment
(events.md §5). Use PostHog's event grouping (an action named `Booking done`) for it.

## 1. Dashboard: Journey (the whole experience)

The main dashboard: from install to coming back.

| Insight | Type | Definition | Answers |
|---|---|---|---|
| Activation funnel | Funnel, 7-day window | `Application Installed` → `signup_started` → `signup_code_sent` → `signup_completed` → `booking_started` → `booking_done` | Where new users drop off before their first booking |
| Login vs signup | Trends | `login_succeeded`, `signup_completed`, `password_reset_completed` per day | Growth and how often people get stuck signing in |
| Signup abandonment | Trends | `signup_abandoned` by `step` | Which step loses people |
| First-visit funnel | Funnel, 30-day window | `booking_done` → `live_appointment_viewed` → `video_joined` → `video_call_ended` (duration_s > 60) | Do bookings turn into real visits? |
| Weekly retention | Retention | Start: `signup_completed`; return: any of `booking_done`, `checkin_submitted`, `ai_message_sent`, `chat_message_sent`, `document_viewed` | Do patients come back after signing up? |
| Stickiness | Stickiness | `Application Opened`, days per week | How habitual the app is |
| Feature reach | Trends, unique users, weekly | The first event of each area: `booking_started`, `payment_checkout_opened`, `video_joined`, `checkin_submitted`, `ai_message_sent`, `chat_message_sent`, `document_uploaded` | Which features people actually use |
| Path to booking | User paths | Starting at `home` screen, ending at `booking_done` | The real routes patients take |
| Home usage | Trends | `home_section_tapped` by `section`, `target` | What Home is for |
| Platform / version split | Trends, broken down by `$os`, `$app_version` | Key outcome events | Regressions after a release |
| Language | Trends, unique users | any event, by `app_language` | Which languages to invest in |

## 2. Dashboard: Booking

| Insight | Type | Definition |
|---|---|---|
| Booking funnel | Funnel, 1-hour window | `doctor_profile_viewed` → `booking_started` → `booking_slots_loaded` → `booking_done`. Not `booking_service_selected` or `booking_slot_selected`: the first slot is pre-selected and legacy doctors list no services, so requiring them would count real bookings as drop-offs |
| By source | Funnel above, broken down by `booking_started.source` | Which entry point converts |
| By payment kind | Trends | `booking_done` by `payment_kind` (both outcome events carry it), next to `booking_abandoned` by `step_reached` for `payment_methods` / `checkout`: does paying up front lose bookings? |
| Abandonment | Trends | `booking_abandoned` by `step_reached` |
| Failure reasons | Trends | `booking_failed` by `reason` |
| No slots | Trends | `booking_slots_loaded` where `days_to_first_slot = none`, by `is_video`, `scan_start` |
| Video vs in person | Trends | `booking_done` by `is_video` |
| Lead time | Trends, median | `booking_completed.days_ahead`; `booking_slot_selected.hour_of_day` histogram |
| Search quality | Trends | `doctor_search_performed` where `results_count = 0`, as a share of all searches |
| Chips | Trends | `doctor_filter_selected` by `chip` |
| Doctor profile depth | Trends | `doctor_profile_tab_viewed` by `tab`; `doctor_service_expanded`; `doctor_contact_tapped` by `channel` |
| Cancellations and reschedules | Trends | `appointment_cancelled` (median `hours_ahead`), `appointment_cancel_failed` by `reason`, `appointment_rescheduled` |
| Reviews | Trends | `review_submitted` by `rating`; funnel past `appointment_details_viewed` → `review_started` → `review_submitted` |

## 3. Dashboard: Payments

| Insight | Type | Definition |
|---|---|---|
| Payment funnel | Funnel, 30-min window | `payment_methods_loaded` (filter `count > 0`: an empty list books without payment and isn't a drop-off) → `payment_completed`. Two steps only: `payment_provider_selected` is skipped when one provider is pre-selected, and `payment_checkout_opened` is skipped when B3 answers PAID at once (`via = immediate`) |
| Checkout reached | Trends, formula | `payment_checkout_opened` ÷ `payment_methods_loaded` (how many open the hosted page at all) |
| By provider | Funnel above, broken down by `provider` | Click vs Stripe conversion |
| By platform | Funnel above, broken down by `$os` | WebView problems on one platform |
| Time to pay | Trends, median and p90 | `payment_completed.seconds_to_pay` |
| How payments confirm | Trends | `payment_completed` by `via` (poll / check / redirect / close_check / immediate) |
| Failures | Trends | `payment_failed` by `reason`; `payment_checkout_failed` by `reason`; `payment_methods_failed` by `reason` |
| Abandonment | Trends | `payment_abandoned` by `via`, `released`; median `seconds_open` |
| Hold length | Trends | `payment_checkout_opened.hold_seconds` vs `payment_completed.seconds_to_pay` (is the hold long enough?) |
| Refund cases | Trends | `payment_failed` where `reason = expired_paid_refund` (every one is a manual clinic refund) |
| Amounts | Trends | `payment_completed` by `amount_bucket` |
| WebView errors | Trends | `webview_error` by `provider`, `$os` |

## 4. Dashboard: Video visit and signing

| Insight | Type | Definition |
|---|---|---|
| Join funnel | Funnel, 1-hour window | `video_call_opened` → `video_permissions_result` (granted = true) → `video_join_tapped` → `video_joined` → `video_doctor_joined` |
| Join failures | Trends | `video_join_failed` by `reason`, broken down by `$os` |
| Permissions | Trends | `video_permissions_result` by `granted`, `$os` |
| Waiting for the doctor | Trends, median and p90 | `video_doctor_joined.waited_s` |
| Join speed | Trends, p90 | `video_joined.join_ms` |
| Call length | Trends, median | `video_call_ended.duration_s` where `doctor_joined = true` |
| Who ends calls | Trends | `video_call_ended` by `ended_by` |
| Small window use | Trends | `video_call_minimized`; share of calls with `was_minimized` |
| Signing funnel | Funnel | `live_appointment_viewed` (signature_due = true) → `signature_started` → `signature_submitted` |
| Signing during calls | Trends | `signature_submitted` by `during_call`; `signature_abandoned` |

## 5. Dashboard: Tasks and adherence

| Insight | Type | Definition |
|---|---|---|
| Check-ins per active patient | Trends, weekly | `checkin_submitted` count ÷ unique users with `task_opened` |
| Reminder → check-in | Funnel, 2-hour window | `push_opened` (type = `TASK_REMINDER`) → `checkin_submitted` |
| Inbox → check-in | Funnel | `notification_action_tapped` (action = `submit_checkin`) → `checkin_submitted` (source = `notification`) |
| Nothing due | Trends | `checkin_nothing_due` (the reminder arrived after the day was done) |
| On time | Trends, median | `checkin_submitted.minutes_from_scheduled`; `hour_of_day` histogram (feeds personalisation.md §7) |
| By task type | Trends | `checkin_submitted` by `category`, `input_type` |
| Form friction | Trends | `checkin_validation_failed` by `field`, `rule`; `checkin_abandoned` |
| Calendar use | Trends | `task_month_changed` by `direction`; `checkin_day_opened` by `state` |

## 6. Dashboard: Shifa AI

| Insight | Type | Definition |
|---|---|---|
| Usage | Trends, unique users | `ai_message_sent` per day; average `turn` per chat |
| Reach among eligible | Formula | unique `ai_message_sent` ÷ unique users with `shifa_ai_enabled = true`; `ai_gated_seen` count |
| Reply speed | Trends, median and p90 | `ai_reply_received.first_token_ms`, `total_ms` |
| Failures | Trends | `ai_reply_failed` by `reason`; `ai_reply_abandoned`; `ai_stream_error` |
| Reply actions | Trends | `ai_reply_action` by `action` (regenerate rate = reply quality signal) |
| Voice | Trends | `ai_message_sent` by `input`; `ai_voice_failed` by `reason`; median `ai_voice_transcribed.transcribe_ms` |
| Rate limit | Trends | `ai_rate_limited`, unique users, by `subscription_tier` |
| AI → booking | Funnel, 1-day window | `ai_message_sent` → `booking_done` |

## 7. Dashboard: Notifications, chat and documents

| Insight | Type | Definition |
|---|---|---|
| Push taps | Trends | `push_opened` by `type`, `cold_start`, `was_signed_out`. There is no true open rate: `push_received` only sees foreground arrivals (the OS shows background pushes without waking the app), so the delivery count has to come from the backend's send log |
| Foreground pushes | Trends | `push_received` by `type` (how often a push lands while the patient is already in the app) |
| Push permission | Trends | `push_permission_result` by `granted`, by `$os` |
| Push routing | Trends | `push_opened` by `routed_to`; `notification_link_missing` by `type` |
| Inbox engagement | Funnel | `notifications_viewed` → `notification_opened` → `notification_action_tapped` |
| Inbox depth | Trends | `notifications_page_loaded` by `page`; `notification_filter_selected` by `filter` |
| Chat activity | Trends | `chat_message_sent` by `type`; `chat_opened` by `source`; `chat_voice_played` |
| Chat failures | Trends | `chat_message_failed` by `type`, `stage`, `reason` |
| Uploads | Trends | `document_uploaded` by `source`, `category`; `document_upload_failed` by `reason`; `document_upload_cancelled` |
| Viewer | Trends | `document_viewed` by `source`, `uploaded_by`; `document_view_failed` by `reason`; `document_visit_opened` |

## 8. Dashboard: Reliability

| Insight | Type | Definition |
|---|---|---|
| API errors | Trends | `api_error` by `endpoint`, `status`, `code` |
| Errors per user | Trends, formula | `api_error` ÷ unique users, by `$app_version` |
| Slow calls | Trends | `api_slow` by `endpoint`, p90 `ms` |
| Crashes | Error tracking | `$exception` issues by `$app_version`, `$os`; crash-free sessions |
| Session restore | Trends | `session_restored` by `result` (spikes in `expired` or `timeout` mean an auth or backend issue) |
| Login trouble | Trends | `login_failed` by `reason` |

## 9. Dashboard: Personalisation (ANL-08)

One insight per flag in personalisation.md §6, each broken down by the flag's variant (PostHog adds
`$feature/<flag>` to events while a flag is active):

| Insight | Definition |
|---|---|
| P1 video default | `booking_visit_type_changed` ÷ `booking_started`, by `$feature/booking-default-video-from-history` |
| P2 provider pre-pick | median seconds `payment_methods_loaded` → `payment_checkout_opened`, by `$feature/payment-preselect-last-provider` |
| P3 home order | `home_section_tapped` on the first section ÷ `home` screens, by `$feature/home-order` |
| P4 first-week tips | activation funnel, by `$feature/home-first-week-tips` |
| P8 scan start | `booking_done` ÷ `booking_started`, by `$feature/booking-scan-start-test` (the experiment's own page shows significance) |

## 10. Alerts

Set on the insights above (PostHog → insight → Alerts), to the team's channel:

| Alert | Condition |
|---|---|
| Booking failures | `booking_failed` daily count > 2× its 7-day average |
| Payments failing | `payment_failed` + `payment_checkout_failed` ÷ `payment_checkout_opened` > 30 % in a day |
| Video joins failing | `video_join_failed` ÷ `video_join_tapped` > 20 % in an hour (when volume allows) |
| API errors after a release | `api_error` per user for the newest `$app_version` > 2× the previous version's |
| New crash | a new `$exception` issue with > 5 users (Error tracking's own alert) |
| Shifa AI down | `ai_reply_failed` (reason ≠ `rate_limit`) > 10 in an hour |
| Login trouble | `login_failed` (reason = `server` or `network`) > 2× its average |

## 11. Cohorts

The saved cohorts are defined in [personalisation.md §3](personalisation.md#3-cohorts); build them in ANL-07 so
retention and funnel insights can be filtered by them from the start.
