# Event catalog

Every screen and event the app sends, in journey order, against the app on `main` (v1.2.4, 2026-11-03). This file
and `data/analytics/AnalyticsEvent.kt` must always match: an event that isn't here isn't sent. The privacy rules
in [privacy.md](privacy.md) apply to every row.

The coverage rule: **if a patient can see it or tap it, there's a row for it.** §14 is the proof: every screen's
controls, with the event each one produces. A new control without a row is a review failure (development.md §4).

## 1. Conventions

### 1.1 Names

- Events: `snake_case`, `object_action`, past tense for outcomes: `booking_completed`, `checkin_submitted`.
  Use `_started` for the user beginning a flow, `_failed` for an outcome that didn't happen, `_tapped` only for
  taps whose outcome isn't an event of its own.
- Properties: `snake_case`. Booleans start with `is_` / `has_` or are plain adjectives (`granted`).
- Values are fixed enums in `snake_case` (`reason = "slot_taken"`), never backend or localized text. Backend
  codes we pass through unchanged are written as the backend sends them (`category = "VITAL"`, `type = "TASK_REMINDER"`).
- Fire outcome events from the ViewModel **after** the result: `booking_completed` in B3's `onSuccess`. A tap that
  starts a request isn't an outcome.
- PostHog's own events (`Application Opened`, `$screen`, `$identify`, `$exception`…) keep their names.

### 1.2 Identity and person properties

- Before login: PostHog's anonymous id. After P1 loads: `identify("patient_<P1 id>")`. PostHog merges the
  anonymous history into the person, so signup and login events join the user.
- `reset()` on logout, account deletion and a session 401.
- **Profile person properties** (`identify`'s `userProperties`, updated on every identify):

| Property | Value | Source |
|---|---|---|
| `app` | `patient` | constant; the doctor app sets `doctor` (setup.md §1.7) |
| `language` | `en` / `ru` / `uz` / `de` | the app language |
| `subscription_tier` | P1 `subscriptionTier` as sent (`BASIC`, `PRO`, `PREMIUM`…) | P1 |
| `shifa_ai_enabled` | bool | `canUseShifaAi` |
| `has_photo` | bool | P1 `photoUrl != null` |
| `has_email` / `has_phone` | bool | P1 |

- **Set once** (`userPropertiesSetOnce`): `first_platform` (`android` / `ios`), `signup_method` (`email` / `sms` /
  `doctor_link`, when the signup ran on this device).
- **Behavioural person properties** are set by events, not by identify. They're listed in
  [personalisation.md §2](personalisation.md#2-behavioural-person-properties); each event's row below names the
  ones it sets under "Sets".
- Never person properties: name, phone, email, birth date or age, gender, address, photo URL (privacy.md §3).

### 1.3 Super properties (on every event)

PostHog adds `$os`, `$os_version`, `$app_version`, `$app_build`, `$device_type`, `$lib`, `$session_id` itself.
We add:

| Property | Value | When |
|---|---|---|
| `app` | `patient` (the doctor app sends `doctor`; setup.md §1.7) | always |
| `app_language` | the app language now (it can differ from the person's at signup) | always |

Both are registered by `Analytics.start`, right after the SDK's setup. The SDK sends `Application Installed` /
`Updated` during that setup, so those two events carry neither property; everything from `Application Opened` on does.

Debug builds send nothing at all (README D4), so there is no `backend` or `debug` property to filter on.

## 2. Screens (`$screen`)

Sent by `TrackScreen` when the screen appears (setup.md §6). The name is the `AnalyticsScreen` id. Tabs are sent
from `MainScreen` when `tab` changes; a tab shown again after an overlay closes isn't re-sent.

| Screen id | Screen (file) | Extra properties |
|---|---|---|
| `splash` | `SplashScreen` | - |
| `login` | `LoginScreen` | - |
| `register` | `RegisterScreen` | `step`: `account` / `info` / `photo` / `otp` (sent on each `RegisterStep`), `is_doctor_link` |
| `forgot_password` | `ForgotPasswordScreen` | `step`: `identifier` / `code` / `new_password` (`ForgotPasswordStep`) |
| `home` | Home tab (`HomeScreen`) | - |
| `bookings` | Bookings tab (`BookingsScreen`) | - |
| `shifa_ai` | Shifa AI tab (`ShifaAiScreen`, the chats list) | - |
| `documents` | Documents tab (`DocumentsScreen`) | - |
| `messages` | Messages tab (`MessagesScreen`) | - |
| `bookings_list` | `BookingsListScreen` | `list`: `recommended` / `my_doctors` / `past` (`BookingsList`) |
| `upcoming` | `UpcomingScreen` | - |
| `my_tasks` | `TasksScreen` | - |
| `task` | `TaskCheckInScreen` | `category` (`VITAL` / `EXERCISE` / `MEDICATION` / `OTHER`), `input_type` (`NUMERIC` / `TEXT` / `BOOLEAN`) |
| `appointment_details` | `AppointmentDetailsScreen` | `state`: `upcoming` / `past` / `cancelled`, `is_video`, `payment_badge`: `paid` / `pay_in_clinic` / `pending` / `refund` / `none` |
| `reschedule` | `RescheduleScreen` | `is_video` |
| `live_appointment` | `LiveAppointmentScreen` | `is_video`, `signature_due` |
| `video_call` | `VideoCallScreen` (full screen only, not the small window) | - |
| `doctor_profile` | `DoctorProfileScreen` | `source` (§4) |
| `create_booking` | `CreateBookingScreen` | `source` (§4), `is_reschedule` |
| `payment` | `PaymentScreen` (the hosted checkout WebView) | `provider`: `click` / `stripe` |
| `document_viewer` | `DocumentViewerScreen` | `file_type`: `pdf` / `image` / `other`, `is_certificate`, `has_visit` |
| `ai_chat` | `ShifaAiChatScreen` | `is_new` |
| `conversation` | `ConversationScreen` | `is_new` |
| `notifications` | `NotificationsScreen` | - |
| `profile` | `ProfileScreen` | - |
| `edit_profile` | `EditProfileScreen` | - |
| `change_password` | `ChangePasswordScreen` | - |
| `delete_account` | `DeleteAccountScreen` | - |

Trays and dialogs aren't screens; their events below cover them (the Confirm Payment tray, the check-in tray, the
review tray, the signature pad, the upload pop-up, the language dialog, the cancel / logout / delete confirms).

## 3. Launch and auth

| Event | When | Properties | Sets | Where |
|---|---|---|---|---|
| `Application Installed` / `Updated` / `Opened` / `Backgrounded` | automatic (SDK lifecycle) | `version`, `build`, `from_background` (SDK) | - | - |
| `session_restored` | the splash's session check ends | `result`: `signed_in` / `no_token` / `expired` (401) / `offline` / `timeout` (the 5 s cap) / `reinstall_wipe` | - | `SplashScreen` → `AuthRepository.restoreSession` |
| `login_succeeded` | A1 succeeds | `identifier_type`: `email` / `phone` | `last_login_at` | `LoginViewModel.signIn` |
| `login_failed` | A1 fails, or the form is refused | `reason`: `empty_fields` / `invalid_credentials` / `locked_out` / `network` / `server` | - | `LoginViewModel.signIn` |
| `login_locked_out` | the 5th failure locks sign in | - | - | `LoginViewModel` |
| `signup_started` | Register opens (Login's Create account) | - | - | `RegisterViewModel` init |
| `signup_step_completed` | Next succeeds on a step | `step`: `account` / `info` / `photo`, `has_email`, `has_phone`, `has_photo` (photo step), `filled_optional` (info step: how many of birth date, country, address were filled) | - | `RegisterViewModel.next` |
| `signup_identifier_conflict` | A2 finds an account | `kind`: `patient_exists` / `doctor_exists` | - | `RegisterViewModel.checkAccount` |
| `signup_code_sent` | A4 / A5 succeeds | `channel`: `email` / `sms`, `is_resend` | - | `RegisterViewModel.sendOtp` / `resend` |
| `signup_sms_unavailable` | the "SMS unavailable" dialog opens | `cause`: `503` / `429` / `limit` | - | `RegisterViewModel` |
| `signup_fallback_email` | Continue with email after SMS failed | - | - | `RegisterViewModel.onContinueWithEmail` |
| `signup_completed` | A7 (or A6 for the doctor link) succeeds | `channel`: `email` / `sms`, `is_doctor_link`, `has_photo` | set once `signup_method`, `signed_up_at` | `RegisterViewModel.verify` |
| `signup_failed` | a step's call fails | `step`, `reason`: `invalid_code` / `network` / `server` / `validation` | - | `RegisterViewModel` |
| `signup_abandoned` | Register closes before completion | `step`, `seconds_spent` | - | `RegisterScreen.onClose` → ViewModel |
| `password_reset_requested` | A9 succeeds | `channel`: `email` / `sms`, `is_resend` | - | `ForgotPasswordViewModel.next` / `resend` |
| `password_reset_completed` | A10 succeeds (auto-login) | - | - | `ForgotPasswordViewModel.resetPassword` (private, from `next`; `reset()` only clears the form) |
| `password_reset_failed` | A9 / A10 fails | `step`, `reason` | - | `ForgotPasswordViewModel` |
| `logged_out` | logout finishes | `source`: `profile` / `session_expired` / `account_deleted` | - | `AuthRepository.logout`, then `reset()` |

## 4. Home, doctors and booking

`source` for doctor and booking events, by where the patient came from: `home_my_doctors` / `home_upcoming` /
`bookings_recommended` / `bookings_my_doctors` / `bookings_chip` / `bookings_search` / `bookings_list` /
`appointment_details` / `live_appointment` / `chat`.

| Event | When | Properties | Sets | Where |
|---|---|---|---|---|
| `home_refreshed` | pull to refresh | - | - | `HomeViewModel.refresh` |
| `home_section_tapped` | a Home card or arrow | `section`: `live` / `upcoming` / `tasks` / `my_doctors`, `target`: `card` / `arrow` / `join` / `view`. Note the routes as built: an Upcoming card and its Join both open the appointment details; a task card and the tasks arrow both open My Tasks; Live's View opens the Live Appointment page | - | `HomeScreen` callbacks |
| `bookings_refreshed` | pull to refresh on the Bookings tab | - | - | `BookingsViewModel.load` |
| `doctor_search_performed` | D1 returns for a typed search (after the debounce) | `query_length`, `results_count`, `chip` | - | `BookingsViewModel.search` |
| `doctor_filter_selected` | a chip is picked | `chip`: `all` / `remote` / `today` / `emergency` (`DoctorFilter`), `results_count` | - | `BookingsViewModel.filterBy` |
| `bookings_list_opened` | a See all arrow opens the full page | `list`: `recommended` / `my_doctors` / `past`, `count` | - | `BookingsScreen` / `HomeScreen` callbacks |
| `doctor_profile_viewed` | the profile's D2 loads | `source`, `has_reviews`, `supports_online`, `services_count`, `locations_count` | - | `DoctorProfileViewModel.load` |
| `doctor_profile_tab_viewed` | a tab is picked | `tab`: `about` / `clinic` / `reviews` / `services` / `certificates` (`DoctorTab`) | - | `DoctorProfileViewModel.selectTab` |
| `doctor_service_expanded` | a service's chevron opens its description | `has_price`, `payment_kind` (§5.0) | - | `DoctorProfileScreen` callback |
| `doctor_contact_tapped` | a contact link | `channel`: `phone` / `email` / `telegram` / `instagram` | - | `DoctorProfileScreen` |
| `doctor_certificate_opened` | a certificate opens in the viewer | - | - | `DoctorProfileScreen` |
| `booking_started` | Create Booking opens | `source`, `is_reschedule` | - | `CreateBookingViewModel` init |
| `booking_location_selected` | Select Clinic | `locations_count` | - | `CreateBookingViewModel.onLocationChange` |
| `booking_service_selected` | Select Service | `payment_kind` (§5.0), `has_price`, `allows_video`, `services_count` | - | `CreateBookingViewModel.onServiceChange` |
| `booking_visit_type_changed` | the video switch | `is_video` | - | `CreateBookingViewModel.onVideoChange` |
| `booking_slots_loaded` | the slot scan ends | `has_slots`, `days_to_first_slot` (days from today to the first free slot, left out when `has_slots = false`), `is_video`, `scan_start`: `next_available` / `today` / `picked`, `scan_days` | - | `CreateBookingViewModel.loadSlots` |
| `booking_date_picked` | the date picker | `days_ahead`, `slots_count` | - | `CreateBookingViewModel.onDateChange` |
| `booking_slot_selected` | a time chip is tapped (the first free slot is pre-selected after a scan, so a booking can complete without this event; funnels must not require it) | `hours_ahead`, `hour_of_day` (local, 0-23) | - | `CreateBookingViewModel.onSlotSelect` |
| `booking_reason_entered` | the reason field loses focus with text | `length` | - | `CreateBookingScreen` → ViewModel |
| `booking_attachment_added` | an attachment's F2 succeeds | `count` (attachments now) | - | `CreateBookingViewModel.attach` |
| `booking_attachment_failed` | an attachment's F2 fails, or it's empty | `reason` | - | `CreateBookingViewModel.attach` |
| `booking_completed` | B3 succeeds and no payment is owed (free, pay in clinic, or a reschedule) | `is_video`, `days_ahead`, `hour_of_day`, `payment_kind`, `has_reason`, `attachments`, `is_reschedule`, `source`, `seconds_on_page` | `has_booked = true`, `last_visit_type`, `bookings_count` (+1, see personalisation.md §2), set once `first_booking_at` | `CreateBookingViewModel.submit` |
| `booking_failed` | Complete Booking doesn't book | `reason`: `needs_service` / `needs_video_service` / `double_booking` / `slot_taken` / `slot_past` / `no_price` / `provider_disabled` / `network` / `server` | - | `CreateBookingViewModel.book` / `submit` |
| `booking_abandoned` | Create Booking closes without booking | `step_reached`: `opened` / `service` / `date` / `slot` / `payment_methods` / `checkout`, `seconds_on_page`, `is_reschedule` | - | `CreateBookingScreen.onBack` → ViewModel |
| `appointment_details_viewed` | the details open | `state`, `is_video`, `payment_badge`, `can_cancel`, `can_reschedule`, `locked_24h`, `source`: `home` / `upcoming` / `bookings_past` / `notification` / `push` / `document_viewer` | - | `AppointmentDetailsViewModel` init |
| `appointment_details_toggled` | the collapsible details card | `open`, `screen`: `details` / `reschedule` / `live_appointment` | - | `AppointmentDetailsViewModel.toggleDetails`, `RescheduleScreen`, `LiveAppointmentViewModel.toggleDetails` |
| `retry_tapped` | a Retry button after a failed load | `screen`: `doctor_profile` / `document_viewer` / `messages` / `notifications` | - | the screen's `onRetry` |
| `appointment_cancel_started` | Cancel opens its confirm | `hours_ahead` | - | `AppointmentDetailsViewModel.askCancel` |
| `appointment_cancelled` | B5 succeeds | `hours_ahead`, `is_video`, `was_paid` | - | `AppointmentDetailsViewModel.cancel` |
| `appointment_cancel_failed` | B5 fails | `reason`: `locked_24h` / `network` / `server` | - | `AppointmentDetailsViewModel.cancel` |
| `appointment_contact_tapped` | the under-24 h contact row | `channel`: `phone` / `email` | - | `AppointmentDetailsScreen` |
| `reschedule_started` | Reschedule opens. The page's Select Service, date picker and slot chips fire the `booking_*` rows above with `is_reschedule = true` | `is_video`, `hours_ahead` | - | `CreateBookingViewModel` (reschedule mode) |
| `appointment_rescheduled` | B3 with `rescheduleAppointmentId` succeeds | `days_moved` (new start − old start, in days), `is_video` | - | `CreateBookingViewModel.submit` |
| `reschedule_failed` | as `booking_failed` | `reason` | - | `CreateBookingViewModel` |
| `review_started` | Leave Review opens | `days_after_visit` | - | `AppointmentDetailsViewModel.openReview` |
| `review_submitted` | D5 succeeds | `rating` (1-5), `has_comment`, `days_after_visit` | `has_reviewed = true` | `AppointmentDetailsViewModel.submitReview` |
| `review_failed` | D5 fails | `reason` | - | `AppointmentDetailsViewModel.submitReview` |
| `review_abandoned` | the tray closes without sending | `had_rating` | - | `AppointmentDetailsViewModel.closeReview` |

## 5. Payments (BOOK-04)

### 5.0 `payment_kind`

The service decides the flow (MOBILE_BOOKING_PAYMENT §1). Every booking and payment event carries it:

| `payment_kind` | Service | Outcome |
|---|---|---|
| `free` | `isFreeConsultation` | books at once, `NOT_REQUIRED` |
| `pay_in_clinic` | `paymentType = PAY_LATER` | books at once, `PAY_IN_CLINIC` |
| `pay_now` | any other priced service | `PENDING_PAYMENT` → the checkout → `PAID` |
| `none` | the doctor lists no services | legacy B3 |

`provider` is `click` / `stripe`, from the backend's `payments/methods`. Amounts go only as `amount_bucket`:
`<50k` / `50k-150k` / `150k-500k` / `>500k` (UZS so'm; other currencies `foreign`), plus `currency`. Never the exact
amount (privacy.md §2).

| Event | When | Properties | Sets | Where |
|---|---|---|---|---|
| `payment_methods_loaded` | Complete Payment's `payments/methods` returns | `count`, `providers` (list), `amount_bucket`, `currency` | - | `CreateBookingViewModel.book` |
| `payment_methods_failed` | `payments/methods` fails | `reason`: `no_price` / `currency` / `network` / `server` | - | `CreateBookingViewModel.book` |
| `payment_provider_selected` | a card in the Confirm Payment tray is tapped (with one provider the card isn't tappable and that provider is pre-selected, so funnels must not require this event) | `provider`, `providers_count` | - | `CreateBookingViewModel.onProviderSelect` |
| `payment_methods_closed` | the tray closes without Pay | `had_provider` | - | `CreateBookingViewModel.closeMethods` |
| `payment_checkout_opened` | B3 returns `PENDING_PAYMENT` with a `checkoutUrl`; the payment page opens | `provider`, `hold_seconds` (to `holdExpiresAt`), `amount_bucket`, `currency`, `is_video` | - | `CreateBookingViewModel.submit` |
| `payment_checkout_failed` | Pay's B3 fails (nothing booked, the tray stays) | `provider`, `reason`: `provider_disabled` / `provider_unavailable` / `slot_taken` / `slot_past` / `currency` / `network` / `server` | - | `CreateBookingViewModel.submit` |
| `payment_check_tapped` | the page's Complete Payment button asks B4 | `result`: `paid` / `pending` / `failed`, `seconds_open` | - | `PaymentViewModel.check` |
| `payment_redirect_seen` | the WebView lands on `/payments/checkout/success` or `/cancel` | `page`: `success` / `cancel`, `provider` | - | `PaymentScreen` → ViewModel |
| `payment_completed` | B4 (poll, check, redirect or the close-time check) says PAID, or B3 itself returned PAID with no checkout (`via = immediate`) | `provider`, `seconds_to_pay` (checkout opened → PAID; 0 for `immediate`), `via`: `poll` / `check` / `redirect` / `close_check` / `immediate`, `amount_bucket`, `currency`, `payment_kind` (always `pay_now`, so `booking_done` can break down by it), `is_video`, `days_ahead`, `hour_of_day`, `source` | everything `booking_completed` sets (`has_booked = true`, `last_visit_type`, `bookings_count` +1, set once `first_booking_at`) plus `has_paid = true`, `last_payment_provider`, `payments_count` (+1), set once `first_payment_at` | `PaymentViewModel.refresh` / `CreateBookingViewModel.submit` / `onPaymentClosed` |
| `payment_book_again_tapped` | Book again on the failed / expired payment page | `reason` (as `payment_failed`) | - | `PaymentScreen` → `CreateBookingViewModel.onPaymentDone` |
| `payment_failed` | the booking died | `reason`: `declined` (`PAYMENT_FAILED`) / `expired` (hold ran out) / `expired_paid_refund` (paid too late, slot taken), `provider`, `seconds_open` | - | `PaymentViewModel.refresh` |
| `payment_abandoned` | the page closed without PAID | `via`: `back` / `provider_cancel`, `seconds_open`, `released`: `true` (B5 freed the slot) / `false` (B5 refused or B4 unknown; the hold expires by itself) | - | `CreateBookingViewModel.onPaymentClosed` |
| `payment_still_pending_shown` | "Payment is still pending…" notice | - | - | `PaymentViewModel.check` |

`booking_completed` is **not** sent for pay-now bookings; `payment_completed` is their outcome, which is why it
carries and sets everything `booking_completed` does. The booking funnel in dashboards.md §2 unions the two.

## 6. The live visit, video call and signing

| Event | When | Properties | Sets | Where |
|---|---|---|---|---|
| `live_appointment_viewed` | the Live Appointment page opens | `minutes_to_start` (negative once started), `is_video`, `signature_due`, `source`: `home` / `notification` / `push` | - | `LiveAppointmentViewModel` init |
| `video_call_opened` | the call screen opens (only from the Live Appointment page's Join Video Call; Home's and Upcoming's Join open the details) | `minutes_to_start`, `is_video` | - | `VideoCallViewModel` init |
| `video_permissions_result` | the camera / mic prompt answers | `granted` | `camera_mic_granted` | `VideoCallViewModel.onPermissions` |
| `video_join_tapped` | Join Now | - | - | `VideoCallViewModel.join` |
| `video_joined` | Daily reports Joined | `join_ms` (tap → joined) | `has_video_visit = true`, set once `first_video_visit_at` | `VideoCallViewModel` |
| `video_join_failed` | V1 or Daily fails | `reason`: `not_in_window` / `ended` / `payment_required` / `not_your_visit` / `daily_failed` / `network` / `server` | - | `VideoCallViewModel.join` |
| `video_doctor_joined` | `doctorJoined` turns true | `waited_s` (joined → doctor) | - | `VideoCallViewModel` |
| `video_call_minimized` / `video_call_expanded` | Back during the call / a tap on the window | - | - | `VideoCallScreen` callbacks (via the ViewModel) |
| `video_control_toggled` | camera, mic or flip | `control`: `camera` / `mic` / `flip`, `is_on` | - | `VideoCallViewModel.toggleCamera` / `toggleMic` / `flipCamera` |
| `video_call_ended` | the call leaves | `ended_by`: `patient` / `doctor` / `error`, `duration_s` (joined → left), `doctor_joined`, `was_minimized` | - | `VideoCallViewModel.leave` / listener |
| `signature_started` | Request to sign opens the pad | `during_call` | - | `LiveAppointmentViewModel.openSign` |
| `signature_submitted` | B6 succeeds | `during_call`, `strokes` (count) | - | `LiveAppointmentViewModel.sign` |
| `signature_failed` | B6 fails, or the pad is empty | `reason`: `empty` / `network` / `server` | - | `LiveAppointmentViewModel.sign` |
| `signature_abandoned` | the pad closes without sending | `had_strokes` | - | `LiveAppointmentViewModel.closeSign` |
| `appointment_end_tapped` | End Appointment | `was_in_call` | - | `LiveAppointmentScreen` |

## 7. Tasks and check-ins

| Event | When | Properties | Sets | Where |
|---|---|---|---|---|
| `tasks_viewed` | My Tasks opens or switches tab | `tab`: `open` / `completed` (`TasksTab`), `open_count`, `completed_count`, `source`: `home_card` / `home_arrow` / `notification` / `push` | - | `TasksViewModel` |
| `tasks_refreshed` | pull to refresh | - | - | `TasksViewModel` |
| `task_opened` | the task page opens (Home's task cards open My Tasks, not a task, so there is no `home` source) | `source`: `my_tasks` / `notification` / `push`, `category`, `input_type`, `status` (`ACTIVE` / `COMPLETED` / `EXPIRED`), `progress_pct` (0-100, in tens) | - | `TaskCheckInViewModel.load` |
| `task_refreshed` | pull to refresh on the task page | - | - | `TaskCheckInViewModel.load` |
| `task_month_changed` | the calendar's arrows | `direction`: `prev` / `next`, `months_from_now` | - | `TaskCheckInViewModel.prevMonth` / `nextMonth` |
| `checkin_day_opened` | a calendar day opens its tray | `state`: `due` / `not_yet_due` / `submitted` / `missed`, `days_from_today` | - | `TaskCheckInViewModel.openDay` |
| `checkin_submitted` | T4 succeeds | `input_type`, `category`, `has_notes`, `source`: `task_page` / `notification`, `minutes_from_scheduled` (negative = early), `hour_of_day` | `has_checked_in = true`, `checkins_count` (+1), set once `first_checkin_at` | `TaskCheckInViewModel.submit` |
| `checkin_validation_failed` | Submit Entry refused by the form | `field`: `value` / `notes`, `rule`: `required` / `not_a_number` / `yes_no` | - | `TaskCheckInViewModel.submit` |
| `checkin_failed` | T4 fails | `reason` | - | `TaskCheckInViewModel.submit` |
| `checkin_nothing_due` | a notification's Submit Check in found nothing due today (opens the task page) | - | - | `TaskCheckInViewModel.openDueDay` |
| `checkin_abandoned` | the tray closes with the form touched but not sent | `input_type` | - | `TaskCheckInViewModel.closeDay` |

Never the value, the answer, the notes, the task name or its description (privacy.md §3).

## 8. Documents

| Event | When | Properties | Sets | Where |
|---|---|---|---|---|
| `documents_refreshed` | pull to refresh | - | - | `DocumentsViewModel.load` |
| `documents_searched` | the search settles | `query_length`, `results_count` | - | `DocumentsViewModel.search` |
| `documents_filtered` | a category chip | `category` (F2's code, or `all`), `results_count` | - | `DocumentsViewModel.filterBy` |
| `document_upload_started` | a pick in the Upload tray | `source`: `camera` / `photo` / `file` | - | `DocumentsViewModel.pick` |
| `document_upload_changed` | "Change" replaces the picked file | - | - | `DocumentsViewModel.pick` |
| `document_uploaded` | F2 succeeds | `source`, `file_type`, `category` (or `none`), `size_kb_bucket`: `<500` / `500-2000` / `2000-10000` / `>10000`, `title_edited` | `has_uploaded = true`, set once `first_upload_at` | `DocumentsViewModel.upload` |
| `document_upload_failed` | F2 fails, or the title is empty | `reason`: `title_missing` / `too_large` / `network` / `server` | - | `DocumentsViewModel.upload` |
| `document_upload_cancelled` | the pop-up's Cancel | - | - | `DocumentsViewModel.cancelUpload` |
| `document_viewed` | F4 loads the file | `source`: `list` / `notification` / `push` / `certificate` / `chat` / `booking_attachment`, `file_type`, `uploaded_by`: `self` / `doctor`, `has_visit` | - | `DocumentViewerViewModel.load` |
| `document_view_failed` | F4 fails, or the type is unsupported | `reason` | - | `DocumentViewerViewModel.load` |
| `document_expanded` | tap to fill the page / Back to shrink | `expanded` | - | `DocumentViewerScreen` |
| `document_visit_opened` | the From Appointment card opens the visit | - | - | `DocumentViewerScreen` callback |
| `document_downloaded` | the save succeeds | `file_type` | - | `DocumentViewerViewModel.download` |
| `document_download_failed` | the save fails | `reason` | - | `DocumentViewerViewModel.download` |
| `document_deleted` | F3 succeeds | `via`: `swipe` / `viewer` | - | `DocumentsViewModel.delete` / `DocumentViewerViewModel.delete` |
| `document_delete_failed` | F3 fails | `via`, `reason` | - | same |

`category` is F2's code (`BLOOD_TEST`…), a coarse type, never the title.

## 9. Messages (doctor chat)

| Event | When | Properties | Sets | Where |
|---|---|---|---|---|
| `chats_refreshed` | pull to refresh | - | - | `MessagesViewModel.load` |
| `chat_doctor_searched` | C5 returns | `query_length`, `results_count` | - | `MessagesViewModel.search` |
| `chat_opened` | a chat opens | `source`: `list` / `search` / `notification` / `push`, `is_new`, `unread_count` | - | `ConversationViewModel` init / `MessagesViewModel.start` |
| `chat_message_sent` | C3 succeeds | `type`: `text` / `image` / `voice` / `document` (`MessageType`), `is_first_in_chat`, `has_caption` (attachments), `duration_s_bucket` (voice: `<10` / `10-30` / `30-60` / `>60`), `size_kb_bucket` (files) | `has_messaged = true`, set once `first_message_at` | `ConversationViewModel.deliver` |
| `chat_message_failed` | C3 or C7 fails | `type`, `stage`: `upload` / `send`, `reason` | - | `ConversationViewModel.fail` |
| `chat_recording_started` | the mic starts a recording | - | - | `ConversationViewModel.onRecording` |
| `chat_recording_failed` | the recording produced no audio | - | - | `ConversationViewModel.sendVoice` |
| `chat_attachment_opened` | a received image, document or file opens in the viewer | `type` | - | `ConversationScreen` |
| `chat_voice_played` | a voice bubble's play | `is_own`, `duration_s_bucket` | - | `ConversationScreen` (AudioPlayer) |

Never the text, the caption, the doctor's name or id, or the chat id (privacy.md §3).

## 10. Shifa AI

| Event | When | Properties | Sets | Where |
|---|---|---|---|---|
| `ai_chat_started` | New Chat, or a saved chat opened | `is_new`, `saved_chats` (count) | - | `ShifaAiViewModel.open` |
| `ai_message_sent` | Send (typed) or the transcript sent after X2 | `turn` (user messages so far in the chat), `input`: `typed` / `voice`, `length_bucket`: `<50` / `50-200` / `>200` chars | `has_used_ai = true`, `ai_messages_count` (+1), set once `first_ai_message_at` | `ShifaAiViewModel.send` |
| `ai_reply_received` | X1 completes | `first_token_ms`, `total_ms`, `reply_chars_bucket`: `<300` / `300-1000` / `>1000`, `is_regenerate` | - | `ShifaAiViewModel.send` / `regenerate` |
| `ai_reply_failed` | X1 fails | `reason`: `rate_limit` / `network` / `server` / `empty` / `sse_error`, `is_regenerate` | - | `ShifaAiViewModel.send` |
| `ai_rate_limited` | the rate-limit latch sets | `turn` | - | `ShifaAiViewModel.send` |
| `ai_reply_abandoned` | the chat is left while streaming | `elapsed_ms` | - | `ShifaAiViewModel.close` |
| `ai_reply_action` | Copy, Regenerate or Share on a reply | `action`: `copy` / `regenerate` / `share`, `turn` | - | `ShifaAiChatScreen` callbacks → ViewModel |
| `ai_voice_recorded` | the recording bar's stop ends a recording | `duration_s` | - | `ShifaAiViewModel.transcribe` |
| `ai_voice_transcribed` | X2 succeeds (the transcript is sent as the question at once) | `transcribe_ms`, `transcript_chars_bucket` | - | `ShifaAiViewModel.transcribe` |
| `ai_voice_failed` | the mic is denied, recording fails, or X2 fails | `reason`: `permission_denied` / `record_failed` / `rate_limit` / `network` / `server` | - | `ShifaAiViewModel` |
| `ai_chat_deleted` | the confirm deletes | `messages` (count) | - | `ShifaAiViewModel.delete` |
| `ai_searched` | the chats search settles | `query_length`, `results_count` | - | `ShifaAiViewModel.search` |
| `ai_gated_seen` | a gated profile (tab hidden) would have seen the tab: sent once per session when Home loads with `canUseShifaAi = false` | `subscription_tier` | - | `HomeViewModel.show` |

Never the question, the reply or the transcript (privacy.md §3).

## 11. Notifications and push

`type` is the backend's type code (`TASK_REMINDER`, `APPOINTMENT_REMINDER`, `CONSULTATION_PAYMENT_DUE_24H`,
`NEW_MESSAGE`, `NEW_DOCUMENT`, `SIGNATURE_REQUESTED`, `AI_VISIT_SUMMARY_READY`…), never the title or message.
`routed_to` is where the tap landed: `appointment` / `live_appointment` / `document` / `documents_tab` / `task` /
`tasks_list` / `chat` / `messages_tab` / `bookings_tab` / `inbox` (no link). A tap while signed out is kept in
`Push.opened` and routed after login; it's sent then with `was_signed_out = true`.

| Event | When | Properties | Sets | Where |
|---|---|---|---|---|
| `push_permission_result` | the OS prompt answers (`PushSetup`) | `granted` | `push_granted` | `PushSetup` (Android, iOS) |
| `push_token_registered` | P7 succeeds after login | - | - | `PushViewModel` |
| `push_received` | a push arrives **in the foreground** (background deliveries are shown by the OS and never reach app code, so this is not a delivery count) | `type` | - | `ShifaMessagingService`, `AppDelegate.willPresent` → `Push.arrived` |
| `push_opened` | a push is tapped | `type`, `cold_start`, `was_signed_out`, `routed_to` | - | `PushViewModel` (`Push.opened`) → `MainScreen` |
| `notifications_viewed` | the inbox's first page loads | `count`, `unread_count`, `source`: `bell` / `push_fallback` | - | `NotificationsViewModel.load` |
| `notifications_page_loaded` | the next page loads (scroll) | `page` (2, 3…) | - | `NotificationsViewModel.loadMore` |
| `notifications_refreshed` | pull to refresh | - | - | `NotificationsViewModel.load` |
| `notification_filter_selected` | a chip | `filter`: `all` / `appointments` / `tasks` / `documents` / `payments` (`NotificationFilter`) | - | `NotificationsScreen` |
| `notification_opened` | a card opens its tray (N3 marks it read) | `type`, `was_unread`, `age_hours_bucket`: `<1` / `1-24` / `1-7d` / `>7d`, `has_link` | - | `NotificationsViewModel.open` |
| `notification_action_tapped` | the tray's button | `type`, `action`: `view` / `submit_checkin` / `open_list` / `open_chat` / `sign_now` / `open_documents`, `routed_to` | - | `NotificationsScreen` → `MainScreen` |
| `notification_link_missing` | the tray's target isn't found (falls back to a list) | `type`, `routed_to` | - | `NotificationsViewModel.of` |
| `notifications_all_read` | N4 succeeds | `unread_count` (before) | - | `NotificationsViewModel.markAllRead` |

## 12. Profile and settings

| Event | When | Properties | Sets | Where |
|---|---|---|---|---|
| `profile_refreshed` | pull to refresh | - | - | `ProfileViewModel.load` |
| `profile_row_tapped` | a settings or account row | `row`: `edit_profile` / `change_password` / `language` / `privacy` / `debug` / `delete_account` / `logout` | - | `ProfileScreen` |
| `language_changed` | a new language is saved | `from`, `to`, `source`: `profile` / `edit_profile` | `language` (profile property) | `ProfileViewModel.selectLanguage` / `EditProfileViewModel.save` |
| `profile_updated` | Edit profile's P2 succeeds | `fields`: list of changed field *names* (`first_name`, `last_name`, `phone`, `birth_date`, `gender`, `address`, `street_address`, `language`) | profile properties re-identified | `EditProfileViewModel.save` |
| `profile_update_failed` | P2 fails or validation refuses | `reason`: `validation` / `network` / `server`, `field` (validation) | - | `EditProfileViewModel.save` |
| `profile_photo_updated` | P3 + P2 succeed | `source`: `profile` / `edit_profile` | `has_photo = true` | `ProfileViewModel.uploadPhoto` / `EditProfileViewModel.uploadPhoto` |
| `profile_photo_failed` | P3 or P2 fails | `source`, `reason` | - | same |
| `password_changed` | A8 succeeds | - | - | `ChangePasswordViewModel.save` |
| `password_change_failed` | A8 fails | `reason`: `wrong_current` / `policy` / `mismatch` / `network` / `server` | - | `ChangePasswordViewModel.save` |
| `account_deletion_started` | the delete confirm → P5 succeeds (code sent) | - | - | `ProfileViewModel.requestDeletion` |
| `account_deletion_code_resent` | Resend | - | - | `DeleteAccountViewModel.resend` |
| `account_deletion_failed` | P5 or P6 fails | `step`: `request` / `confirm`, `reason`: `email_required` / `invalid_code` / `network` / `server` | - | `ProfileViewModel` / `DeleteAccountViewModel` |
| `account_deleted` | P6 succeeds (sent before `reset()`) | - | - | `DeleteAccountViewModel.confirm` |
| `privacy_policy_opened` | Privacy (in-app browser) | - | - | `ProfileScreen` |
| `analytics_opted_out` | the "Share usage data" switch goes off (the last event sent) | - | - | `ProfileViewModel` |
| `analytics_opted_in` | the switch goes back on (the first event sent) | - | - | `ProfileViewModel` |
| `coming_soon_tapped` | a "Coming soon" placeholder | `feature`: `map` (Edit profile → Select on Map) / `ai_camera` / `ai_photo` / `ai_file` (the Shifa AI attachment tray) | - | the screen |

## 13. Reliability

| Event | When | Properties | Where |
|---|---|---|---|
| `api_error` | any `apiCall` failure | `endpoint`: the path with ids replaced (`patients/me/appointments/{id}`), `method`, `status` (0 = no response), `error`: `ApiException` class name, `code`: the backend's error code when sent | `ApiHelper.apiCall`, both failure branches |
| `api_slow` | a call over 5 s that succeeded | `endpoint`, `method`, `ms` | `ApiHelper.apiCall` |
| `ai_stream_error` | the X1 SSE stream fails outside `apiCall` | `stage`: `connect` / `stream`, `status` | the Shifa AI repository |
| `$exception` | automatic: an unhandled exception (README D11) | SDK: type, message, stack | SDK |
| `webview_error` | the payment WebView fails to load the checkout | `provider`, `error_code` (platform code, no URL) | `PaymentScreen` → ViewModel |

`api_error` never has the backend's message, the body or a query string (they can hold personal data). `endpoint`
is built from `ApiConfig.Endpoints` templates, not the request URL. `$exception` messages come from our own
exception classes; `apiCall` sanitizes backend text before it reaches an `ApiException`, so a stack trace holds no
patient data. Review any new exception type against privacy.md §3.

## 14. Coverage matrix: every control, one row

How to read it: each screen lists what a patient can do there, and the event that records it. "screen" means the
`$screen` row in §2 is enough (a pure navigation tap whose destination sends its own screen). When a screen gains a
control, this table gets a row in the same PR.

| Screen | Control | Event |
|---|---|---|
| Splash | (none; waits) | `session_restored` |
| Login | Sign in | `login_succeeded` / `login_failed` / `login_locked_out` |
| | Show / hide password | (not tracked: no decision value) |
| | Forgot password? | screen `forgot_password` |
| | Create account | `signup_started` + screen `register` |
| | Development backend (debug) | nothing: debug builds never send |
| Register | Next / Create account / Continue with email / Resend / code entry | `signup_step_completed`, `signup_identifier_conflict`, `signup_code_sent`, `signup_sms_unavailable`, `signup_fallback_email`, `signup_completed`, `signup_failed` |
| | Photo pick | `signup_step_completed.has_photo` |
| | Back / Close | `signup_abandoned` |
| Forgot password | Next / Resend / Reset | `password_reset_requested`, `password_reset_completed`, `password_reset_failed` |
| | Back / Close | screen (the Login screen re-sends) |
| Home | Pull to refresh | `home_refreshed` |
| | Avatar → Profile, bell → Notifications | screens `profile`, `notifications` |
| | Live Appointment View | `home_section_tapped(live, view)` + `live_appointment_viewed` |
| | Upcoming card / its Join / arrow | `home_section_tapped` + `appointment_details_viewed` (both the card and Join open the details) / screen `upcoming` |
| | Task card / arrow | `home_section_tapped` + `tasks_viewed(home_card / home_arrow)` (both open My Tasks) |
| | My Doctors card / arrow | `home_section_tapped` + `doctor_profile_viewed` / `bookings_list_opened` |
| Bookings tab | Pull to refresh | `bookings_refreshed` |
| | Search | `doctor_search_performed` |
| | Chips | `doctor_filter_selected` |
| | Doctor card | `doctor_profile_viewed` |
| | Section arrows | `bookings_list_opened` |
| | Past appointment card | `appointment_details_viewed` |
| Bookings list page | Card | `doctor_profile_viewed` / `appointment_details_viewed` |
| Upcoming page | Card / Join | `appointment_details_viewed` (both) |
| Doctor profile | Retry after a failed load | `retry_tapped(doctor_profile)` |
| | Tabs | `doctor_profile_tab_viewed` |
| | Service chevron | `doctor_service_expanded` |
| | Phone / email / Telegram / Instagram | `doctor_contact_tapped` |
| | Certificate | `doctor_certificate_opened` + `document_viewed(certificate)` |
| | Create Booking | `booking_started` |
| Create Booking | Select Clinic / Select Service / video switch / date / slot / reason / attach | `booking_location_selected`, `booking_service_selected`, `booking_visit_type_changed`, `booking_date_picked`, `booking_slot_selected`, `booking_reason_entered`, `booking_attachment_added` / `_failed`; `booking_slots_loaded` on each scan |
| | Complete Booking (free / pay in clinic / reschedule) | `booking_completed` / `appointment_rescheduled` / `booking_failed` / `reschedule_failed` |
| | Complete Payment (pay now) | `payment_methods_loaded` / `payment_methods_failed` |
| | Confirm Payment tray: provider card / Pay / close | `payment_provider_selected`, `payment_checkout_opened` / `payment_checkout_failed`, `payment_methods_closed` |
| | Back | `booking_abandoned` |
| Payment page | (hosted checkout) | `payment_redirect_seen`, `payment_completed`, `payment_failed`, `webview_error` |
| | Complete Payment | `payment_check_tapped`, `payment_still_pending_shown` |
| | Book again (after a failure) | `payment_book_again_tapped` |
| | Back / provider cancel | `payment_abandoned` |
| Appointment details | Doctor header card | `doctor_profile_viewed(appointment_details)` |
| | Details card toggle | `appointment_details_toggled(details)` |
| | Reschedule | `reschedule_started` |
| | Cancel → confirm | `appointment_cancel_started`, `appointment_cancelled` / `appointment_cancel_failed` |
| | Phone / email (24 h) | `appointment_contact_tapped` |
| | Leave Review → stars / comment / Submit / close | `review_started`, `review_submitted` / `review_failed` / `review_abandoned` |
| Reschedule page | Details toggle | `appointment_details_toggled(reschedule)` |
| | Select Service / date / slot | `booking_service_selected`, `booking_date_picked`, `booking_slot_selected` (all `is_reschedule = true`); `booking_slots_loaded` on each scan |
| | Confirm | `appointment_rescheduled` / `reschedule_failed` |
| | Back | `booking_abandoned(is_reschedule = true)` |
| Live Appointment | Doctor header card | `doctor_profile_viewed(live_appointment)` |
| | Details toggle | `appointment_details_toggled(live_appointment)` |
| | Request to sign → pad → Submit / close | `signature_started`, `signature_submitted` / `signature_failed` / `signature_abandoned` |
| | Join Video Call | `video_call_opened` |
| | End Appointment | `appointment_end_tapped` |
| Video call | Permissions prompt | `video_permissions_result` |
| | Join Now | `video_join_tapped`, `video_joined` / `video_join_failed`, `video_doctor_joined` |
| | Camera / mic / flip | `video_control_toggled` |
| | Back (shrink) / tap window | `video_call_minimized` / `video_call_expanded` |
| | Hang up / doctor leaves / error | `video_call_ended` |
| My Tasks | Open / Completed tabs, pull to refresh, card | `tasks_viewed`, `tasks_refreshed`, `task_opened` |
| Task page | Pull to refresh, month arrows, day tap | `task_refreshed`, `task_month_changed`, `checkin_day_opened` |
| | Tray: value / Yes-No / notes / Submit Entry / close | `checkin_submitted` / `checkin_validation_failed` / `checkin_failed`, `checkin_abandoned` |
| Documents tab | Search, chips, pull to refresh | `documents_searched`, `documents_filtered`, `documents_refreshed` |
| | Upload tray: camera / photo / file; pop-up: title, type, Change, Upload, Cancel | `document_upload_started`, `document_upload_changed`, `document_uploaded` / `document_upload_failed`, `document_upload_cancelled` |
| | Card | `document_viewed(list)` |
| | Swipe delete | `document_deleted(swipe)` / `document_delete_failed` |
| Document viewer | Retry after a failed load | `retry_tapped(document_viewer)` |
| | Tap to expand / Back | `document_expanded` |
| | From Appointment card | `document_visit_opened` |
| | Download | `document_downloaded` / `document_download_failed` |
| | Delete → confirm | `document_deleted(viewer)` / `document_delete_failed` |
| Shifa AI tab | Search, New Chat, chat row | `ai_searched`, `ai_chat_started` |
| AI chat | Send / mic / stop / delete / Copy / Regenerate / Share / Back | `ai_message_sent`, `ai_reply_received` / `ai_reply_failed` / `ai_rate_limited`, `ai_voice_recorded`, `ai_voice_transcribed` / `ai_voice_failed`, `ai_chat_deleted`, `ai_reply_action`, `ai_reply_abandoned` |
| | Add to chat tray: Camera / Photo / File (placeholders) | `coming_soon_tapped(ai_camera / ai_photo / ai_file)` |
| Messages tab | Search, pull to refresh, chat row, search result, Retry | `chat_doctor_searched`, `chats_refreshed`, `chat_opened`, `retry_tapped(messages)` |
| Conversation | Send / attach photo / PDF / mic / stop / tap attachment / play voice | `chat_message_sent` / `chat_message_failed`, `chat_recording_started` / `chat_recording_failed`, `chat_attachment_opened`, `chat_voice_played` |
| Notifications | Chips, pull to refresh, Retry, scroll (paging), card, tray button, Mark all read | `notification_filter_selected`, `notifications_refreshed`, `retry_tapped(notifications)`, `notifications_page_loaded`, `notification_opened`, `notification_action_tapped` / `notification_link_missing`, `notifications_all_read` |
| Push (OS) | Permission prompt, arrival, tap | `push_permission_result`, `push_received`, `push_opened` |
| Profile | Pull to refresh, every row, avatar camera | `profile_refreshed`, `profile_row_tapped`, `profile_photo_updated` / `profile_photo_failed` |
| | Language dialog | `language_changed` |
| | Log out → confirm | `logged_out(profile)` |
| | Share usage data switch | `analytics_opted_out` / `analytics_opted_in` |
| Edit profile | Fields / Save / photo | `profile_updated` / `profile_update_failed`, `profile_photo_updated` |
| | Select on Map | `coming_soon_tapped(map)` |
| Change password | Save | `password_changed` / `password_change_failed` |
| Delete account | Code / Resend / Confirm | `account_deletion_code_resent`, `account_deleted` / `account_deletion_failed` |
| Debug (debug builds) | Demo data, Development backend, fake pushes, Analytics (your id, the session's events), Crash now | debug builds never send |

## 15. Where the journey stitches together

These are the hand-offs between features that the funnels in [dashboards.md](dashboards.md) rely on. Keep their
`source` values in sync when routes change:

- notification / push → task page / check-in tray → `checkin_submitted.source = notification`
- Home's Live Appointment → `live_appointment_viewed` → `video_call_opened` → `signature_submitted.during_call`
- doctor profile (any `source`) → `booking_started.source` → `booking_completed` **or** `payment_checkout_opened`
  → `payment_completed`
- `CONSULTATION_PAYMENT_*` push → `push_opened.routed_to = appointment` → `appointment_details_viewed`
- past `appointment_details_viewed` → `review_started` → `review_submitted`
- `NEW_DOCUMENT` push → `document_viewed.source = push` → `document_visit_opened` → `appointment_details_viewed.source = document_viewer`
