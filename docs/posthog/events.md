# Event catalog

Every screen and event the app sends, in journey order. This file and `data/analytics/AnalyticsEvent.kt` must
always match: an event that isn't here isn't sent. The privacy rules in [privacy.md](privacy.md) apply to every row.

## 1. Conventions

### 1.1 Names

- Events: `snake_case`, `object_action`, past tense for outcomes: `booking_completed`, `checkin_submitted`.
  Use `_started` for the user beginning a flow, `_failed` for an outcome that didn't happen, `_tapped` only for
  taps whose outcome isn't an event of its own.
- Properties: `snake_case`. Booleans start with `is_` / `has_` or are plain adjectives (`granted`).
- Values are fixed enums in `snake_case` (`reason = "slot_taken"`), never backend or localized text.
- Fire outcome events from the ViewModel **after** the result: `booking_completed` in B3's `onSuccess`. A tap that
  starts a request isn't an outcome.
- PostHog's own events (`Application Opened`, `$screen`, `$identify`…) keep their names.

### 1.2 Identity and person properties

- Before login: PostHog's anonymous id. After P1 loads: `identify("patient_<P1 id>")`. PostHog merges the
  anonymous history into the person, so signup and login events join the user.
- `reset()` on logout, account deletion and a session 401.
- Person properties (`identify`'s `userProperties`, updated on every identify):

| Property | Value | Source |
|---|---|---|
| `language` | `en` / `ru` / `uz` / `de` | the app language |
| `subscription_tier` | P1 `subscriptionTier` as sent (`FREE`, `PRO`…) | P1 |
| `shifa_ai_enabled` | bool | `canUseShifaAi` |
| `has_photo` | bool | P1 `photoUrl != null` |
| `has_email` / `has_phone` | bool | P1 |
| `country` | ISO country code only | P1 `locationCountry`, if set |
| `internal` | `true` for the team's accounts | Debug menu (development.md §5) |

- Set once (`userPropertiesSetOnce`): `first_platform` (`android` / `ios`), `signup_method` (`email` / `sms` /
  `doctor_link`, from the signup flow when it ran on this device).
- Never person properties: name, phone, email, birth date or age, gender, address, photo URL (privacy.md §3).

### 1.3 Super properties (on every event)

PostHog adds `$os`, `$os_version`, `$app_version`, `$app_build`, `$device_type`, `$lib`, `$session_id` itself.
We add:

| Property | Value |
|---|---|
| `app_language` | the app language now (it can differ from the person's at signup) |

## 2. Screens (`$screen`)

Sent by `TrackScreen` when the screen appears (setup.md §6). The name is the `AnalyticsScreen` id.

| Screen id | Screen (file) | Extra properties |
|---|---|---|
| `splash` | `SplashScreen` | - |
| `login` | `LoginScreen` | - |
| `register` | `RegisterScreen` | `step`: `account` / `info` / `photo` / `code` / `doctor_code` (sent on each step) |
| `forgot_password` | `ForgotPasswordScreen` | `step`: `identifier` / `code` / `new_password` |
| `home` | Home tab | - |
| `bookings` | Bookings tab | - |
| `shifa_ai` | Shifa AI tab (chats) | - |
| `documents` | Documents tab | - |
| `messages` | Messages tab | - |
| `bookings_list` | `BookingsListScreen` | `list`: `recommended` / `my_doctors` / `past` |
| `upcoming` | `UpcomingScreen` | - |
| `my_tasks` | `TasksScreen` | - |
| `task` | `TaskCheckInScreen` | `category` |
| `appointment_details` | `AppointmentDetailsScreen` | `state`: `upcoming` / `under_48h` / `past` / `cancelled` |
| `reschedule` | `RescheduleScreen` | `is_video` |
| `live_appointment` | `LiveAppointmentScreen` | `is_video`, `signature_due` |
| `video_call` | `VideoCallScreen` (full screen only, not the small window) | - |
| `doctor_profile` | `DoctorProfileScreen` | `source` (§4) |
| `create_booking` | `CreateBookingScreen` | - |
| `document_viewer` | `DocumentViewerScreen` | `file_type`: `pdf` / `image` / `other`, `is_certificate` |
| `ai_chat` | `ShifaAiChatScreen` | `is_new` |
| `conversation` | `ConversationScreen` | `is_new` |
| `notifications` | `NotificationsScreen` | - |
| `profile` | `ProfileScreen` | - |
| `edit_profile` | `EditProfileScreen` | - |
| `change_password` | `ChangePasswordScreen` | - |
| `delete_account` | `DeleteAccountScreen` | - |

Trays and dialogs aren't screens; their events below cover them.

## 3. Launch and auth

| Event | When | Properties | Where |
|---|---|---|---|
| `Application Installed` / `Updated` / `Opened` / `Backgrounded` | automatic (SDK lifecycle) | `version`, `build`, `from_background` (SDK) | - |
| `session_restored` | the splash's session check ends | `result`: `signed_in` / `no_token` / `expired` (401) / `offline` / `timeout` | `SplashScreen` / `AuthRepository.restoreSession` |
| `login_succeeded` | A1 succeeds | `identifier_type`: `email` / `phone` | `LoginViewModel` |
| `login_failed` | A1 fails, or the form is refused | `reason`: `empty_fields` / `invalid_credentials` / `locked_out` / `network` / `server` | `LoginViewModel` |
| `login_locked_out` | the 5th failure locks sign in | - | `LoginViewModel` |
| `signup_started` | Register opens (from Login's Create account) | - | `RegisterViewModel` |
| `signup_step_completed` | Next succeeds on a step | `step`: `account` / `info` / `photo`, `has_email`, `has_photo` (photo step), `filled_optional` (info step: count of the 3 optional fields) | `RegisterViewModel` |
| `signup_identifier_conflict` | A2 finds an account | `kind`: `patient_exists` / `doctor_exists` | `RegisterViewModel` |
| `signup_code_sent` | A4 / A5 succeeds | `channel`: `email` / `sms`, `is_resend` | `RegisterViewModel` |
| `signup_sms_unavailable` | the "SMS unavailable" dialog opens | `cause`: `503` / `429` / `channel_email` / `limit` | `RegisterViewModel` |
| `signup_completed` | A7 (or A6 for the doctor link) succeeds | `channel`: `email` / `sms`, `is_doctor_link`, `has_photo` | `RegisterViewModel` |
| `signup_failed` | a step's call fails | `step`, `reason`: `invalid_code` / `network` / `server` / `validation` | `RegisterViewModel` |
| `password_reset_requested` | A9 succeeds | `channel`: `email` / `sms` | `ForgotPasswordViewModel` |
| `password_reset_completed` | A10 succeeds (auto-login) | - | `ForgotPasswordViewModel` |
| `password_reset_failed` | A9 / A10 fails | `step`, `reason` | `ForgotPasswordViewModel` |
| `logged_out` | logout finishes | `source`: `profile` / `session_expired` / `account_deleted` | `AuthRepository`, then `reset()` |

## 4. Home, doctors and booking

`source` for doctor and booking events: `home_my_doctors` / `bookings_recommended` / `bookings_my_doctors` /
`bookings_chip` / `bookings_list` / `appointment_details` / `live_appointment` / `ai`.

| Event | When | Properties | Where |
|---|---|---|---|
| `home_refreshed` | pull to refresh | - | `HomeViewModel` |
| `home_section_tapped` | a Home card or arrow | `section`: `live` / `upcoming` / `tasks` / `my_doctors`, `target`: `card` / `arrow` / `join` / `view` | `HomeScreen` callbacks |
| `doctor_search_performed` | D1 returns for a typed search (after the debounce) | `query_length`, `results_count`, `chip` | `BookingsViewModel` |
| `doctor_filter_selected` | a chip is picked | `chip`: `all` / `remote` / `today` / `emergency` / `near_me` | `BookingsViewModel` |
| `near_me_result` | the Near me location flow ends | `result`: `located` / `denied` / `services_off` / `no_fix` | `BookingsViewModel` |
| `doctor_profile_viewed` | the profile's D2 loads | `source`, `has_reviews`, `has_online_services` | `DoctorProfileViewModel` |
| `doctor_profile_tab_viewed` | a tab is picked | `tab`: `about` / `clinic` / `reviews` / `services` / `certificates` | `DoctorProfileScreen` |
| `doctor_contact_tapped` | a contact link | `channel`: `phone` / `email` / `telegram` / `instagram` | `DoctorProfileScreen` |
| `booking_started` | Create Booking opens | `source` | `CreateBookingViewModel` init |
| `booking_slots_loaded` | the first slot scan ends | `days_to_first_slot` (0-30, or none), `is_video` | `CreateBookingViewModel` |
| `booking_visit_type_changed` | the video switch | `is_video` | `CreateBookingViewModel` |
| `booking_date_picked` | the date picker | `days_ahead` | `CreateBookingViewModel` |
| `booking_attachment_added` | an attachment's F2 succeeds | `count` (attachments now) | `CreateBookingViewModel` |
| `booking_attachment_failed` | an attachment's F2 fails, or it's empty | `reason` | `CreateBookingViewModel` |
| `booking_completed` | B3 succeeds | `is_video`, `days_ahead`, `payment_status` (`paid` / `pending` / `none`), `has_reason`, `attachments` | `CreateBookingViewModel` |
| `booking_failed` | Complete Booking doesn't book | `reason`: `needs_service` / `double_booking` / `slot_taken` / `no_price` / `network` / `server` | `CreateBookingViewModel` |
| `appointment_details_viewed` | the details open | `state` (as the screen), `is_video` | `AppointmentDetailsViewModel` |
| `appointment_cancelled` | B5 succeeds | `hours_ahead`, `is_video` | `AppointmentDetailsViewModel` |
| `appointment_cancel_failed` | B5 fails | `reason` | `AppointmentDetailsViewModel` |
| `appointment_contact_tapped` | the under-48 h warning | `channel`: `phone` / `email` | `AppointmentDetailsScreen` |
| `reschedule_started` | Reschedule opens | `is_video`, `hours_ahead` | `CreateBookingViewModel` (reschedule mode) |
| `appointment_rescheduled` | B3 with `rescheduleAppointmentId` succeeds | `days_moved` (new start − old start, in days) | `CreateBookingViewModel` |
| `reschedule_failed` | as `booking_failed` | `reason` | `CreateBookingViewModel` |
| `review_started` | Leave Review opens | - | `AppointmentDetailsViewModel` |
| `review_submitted` | D5 succeeds | `rating` (1-5), `has_comment`, `days_after_visit` | `AppointmentDetailsViewModel` |
| `review_failed` | D5 fails | `reason` | `AppointmentDetailsViewModel` |

## 5. The live visit and video call

| Event | When | Properties | Where |
|---|---|---|---|
| `live_appointment_viewed` | the Live Appointment page opens | `minutes_to_start` (negative once started), `is_video`, `signature_due` | `LiveAppointmentViewModel` |
| `video_call_opened` | the call screen opens | `minutes_to_start`, `source`: `live_appointment` / `home_join` / `upcoming_join` | `VideoCallViewModel` |
| `video_permissions_result` | the camera / mic prompt answers | `granted` | `VideoCallViewModel.onPermissions` |
| `video_join_tapped` | Join Now | - | `VideoCallViewModel.join` |
| `video_joined` | Daily reports Joined | `join_ms` (tap → joined) | `VideoCallViewModel` |
| `video_join_failed` | V1 or Daily fails | `reason`: `not_in_window` / `ended` / `payment_required` / `not_your_visit` / `daily_failed` / `network` | `VideoCallViewModel` |
| `video_doctor_joined` | `doctorJoined` turns true | `waited_s` (joined → doctor) | `VideoCallViewModel` |
| `video_call_minimized` / `video_call_expanded` | Back during the call / a tap on the window | - | `VideoCallScreen` callbacks (via the ViewModel) |
| `video_control_toggled` | camera, mic or flip | `control`: `camera` / `mic` / `flip`, `is_on` | `VideoCallViewModel` |
| `video_call_ended` | the call leaves | `ended_by`: `patient` / `doctor` / `error`, `duration_s` (joined → left), `doctor_joined`, `was_minimized` | `VideoCallViewModel` |
| `signature_started` | Sign now opens the tray | `during_call` | `LiveAppointmentViewModel` |
| `signature_submitted` | B6 succeeds | `during_call`, `strokes` (count) | `LiveAppointmentViewModel` |
| `signature_failed` | B6 fails, or the pad is empty | `reason`: `empty` / `network` / `server` | `LiveAppointmentViewModel` |
| `appointment_end_tapped` | End Appointment | `was_in_call` | `LiveAppointmentScreen` |

## 6. Tasks and check-ins

| Event | When | Properties | Where |
|---|---|---|---|
| `tasks_viewed` | My Tasks opens or switches tab | `tab`: `open` / `completed`, `open_count` | `TasksViewModel` |
| `task_opened` | the task page opens | `source`: `home` / `my_tasks` / `notification` / `push`, `category`, `input_type` | `TaskCheckInViewModel` |
| `checkin_day_opened` | a calendar day opens its tray | `state`: `due` / `not_yet_due` / `submitted` | `TaskCheckInViewModel.openDay` |
| `checkin_submitted` | T4 succeeds | `input_type`, `category`, `has_notes`, `source`: `task_page` / `notification`, `minutes_from_scheduled` | `TaskCheckInViewModel.submit` |
| `checkin_validation_failed` | Submit Entry refused by the form | `field`: `value` / `notes`, `rule`: `required` / `not_a_number` / `yes_no` | `TaskCheckInViewModel.submit` |
| `checkin_failed` | T4 fails | `reason` | `TaskCheckInViewModel.submit` |
| `checkin_nothing_due` | a notification's Submit Check in found nothing due | - | `TaskCheckInViewModel` (`openDue`) |

Never the value, the notes, the task name or its description (privacy.md §3).

## 7. Documents

| Event | When | Properties | Where |
|---|---|---|---|
| `documents_searched` | the search settles | `query_length`, `results_count` | `DocumentsViewModel` |
| `documents_filtered` | a category chip | `category` | `DocumentsViewModel` |
| `document_upload_started` | a pick in the Upload tray | `source`: `camera` / `photo` / `file` | `DocumentsViewModel` |
| `document_uploaded` | F2 succeeds | `source`, `file_type`, `category` (or none), `size_kb_bucket`: `<500` / `500-2000` / `2000-10000` / `>10000` | `DocumentsViewModel` |
| `document_upload_failed` | F2 fails, or the title is empty | `reason` | `DocumentsViewModel` |
| `document_viewed` | F4 loads the file | `source`: `list` / `notification` / `push` / `certificate`, `file_type`, `uploaded_by`: `self` / `doctor` | `DocumentViewerViewModel` |
| `document_view_failed` | F4 fails, or the type is unsupported | `reason` | `DocumentViewerViewModel` |
| `document_downloaded` | the save succeeds | `file_type` | `DocumentViewerViewModel` |
| `document_deleted` | F3 succeeds | `via`: `swipe` / `viewer` | `DocumentsViewModel` / `DocumentViewerViewModel` |

`category` is F2's code (`BLOOD_TEST`…), a coarse type, never the title.

## 8. Messages (doctor chat)

| Event | When | Properties | Where |
|---|---|---|---|
| `chat_opened` | a chat opens | `source`: `list` / `search` / `notification` / `push`, `is_new`, `unread_count` | `ConversationViewModel` |
| `chat_doctor_searched` | C5 returns | `query_length`, `results_count` | `MessagesViewModel` |
| `chat_message_sent` | C3 succeeds | `type`: `text` / `image`, `is_first_in_chat` | `ConversationViewModel` |
| `chat_message_failed` | C3 / C7 fails | `type`, `reason` | `ConversationViewModel` |
| `chat_attachment_opened` | a received image, document or voice is opened | `type` | `ConversationScreen` |

## 9. Shifa AI

| Event | When | Properties | Where |
|---|---|---|---|
| `ai_chat_started` | New Chat, or a saved chat opened | `is_new`, `saved_chats` (count) | `ShifaAiViewModel.open` |
| `ai_message_sent` | Send | `turn` (user messages so far), `input`: `typed` / `voice` / `voice_edited` | `ShifaAiViewModel.send` |
| `ai_reply_received` | X1 completes | `first_token_ms`, `total_ms`, `reply_chars` | `ShifaAiViewModel.send` |
| `ai_reply_failed` | X1 fails | `reason`: `rate_limit` / `network` / `server` / `empty` | `ShifaAiViewModel.send` |
| `ai_reply_abandoned` | the chat is left while streaming | `elapsed_ms` | `ShifaAiViewModel.close` |
| `ai_voice_recorded` | the mic's second tap ends a recording | `duration_s` | `ShifaAiChatScreen` → ViewModel |
| `ai_voice_transcribed` | X2 succeeds | `transcribe_ms`, `transcript_chars` | `ShifaAiViewModel.transcribe` |
| `ai_voice_failed` | the mic is denied, recording fails, or X2 fails | `reason`: `permission_denied` / `record_failed` / `rate_limit` / `network` / `server` | `ShifaAiViewModel` |
| `ai_chat_deleted` | the confirm deletes | `messages` (count) | `ShifaAiViewModel.delete` |
| `ai_coming_soon_tapped` | Add to chat's Camera / Photo / File | `feature` | `ShifaAiChatScreen` |
| `ai_searched` | the chats search settles | `results_count` | `ShifaAiViewModel.search` |

Never the question, the reply or the transcript (privacy.md §3).

## 10. Notifications and push

| Event | When | Properties | Where |
|---|---|---|---|
| `push_permission_result` | the OS prompt answers (`PushSetup`) | `granted` | `PushSetup` (Android, iOS) |
| `push_received` | a push arrives in the foreground (Android service, iOS `willPresent`) | `type` (13's notification type) | `ShifaMessagingService`, `AppDelegate` → bridge |
| `push_opened` | a push is tapped | `type`, `cold_start`, `routed_to`: `appointment` / `document` / `task` / `chat` / `list` / `inbox` | `MainScreen` (`push.open`) |
| `notifications_viewed` | the inbox's N1 loads | `count`, `unread_count` | `NotificationsViewModel` |
| `notification_filter_selected` | a chip | `filter` | `NotificationsScreen` |
| `notification_opened` | a card opens its tray | `type`, `was_unread`, `age_hours` | `NotificationsViewModel.open` |
| `notification_action_tapped` | the tray's button | `type`, `action`: `view` / `submit_checkin` / `open_list` / `open_chat` | `NotificationsScreen` |
| `notifications_all_read` | N4 succeeds | `unread_count` (before) | `NotificationsViewModel` |

`type` is the backend's type code (`TASK_REMINDER`…), never the title or message.

## 11. Profile and settings

| Event | When | Properties | Where |
|---|---|---|---|
| `language_changed` | a new language is saved | `from`, `to`, `source`: `profile` / `edit_profile` | `ProfileViewModel` / `EditProfileViewModel` |
| `profile_updated` | Edit profile's P2 succeeds | `fields`: list of changed field *names* (`first_name`, `phone`…) | `EditProfileViewModel` |
| `profile_photo_updated` | P3 + P2 succeed | `source`: `profile` / `edit_profile` | `ProfileViewModel` / `EditProfileViewModel` |
| `password_changed` | A8 succeeds | - | `ChangePasswordViewModel` |
| `password_change_failed` | A8 fails | `reason`: `wrong_current` / `policy` / `network` / `server` | `ChangePasswordViewModel` |
| `account_deletion_started` | the delete confirm | - | `DeleteAccountViewModel` |
| `account_deleted` | P6 succeeds (sent before `reset()`) | - | `DeleteAccountViewModel` |
| `privacy_policy_opened` | Privacy | - | `ProfileScreen` |
| `analytics_opted_out` | the "Share usage data" switch goes off (the last event sent) | - | `ProfileViewModel` |
| `coming_soon_tapped` | any "Coming soon" placeholder | `feature`: `app_lock` / `map` / … | the screen |

## 12. Reliability

| Event | When | Properties | Where |
|---|---|---|---|
| `api_error` | any `apiCall` failure | `endpoint`: the path with ids replaced (`patients/me/appointments/{id}`), `method`, `status` (0 = no response), `error`: `ApiException` class name | `ApiHelper.apiCall`, next to `httpLogger` |
| `api_slow` | a call over 5 s that succeeded | `endpoint`, `method`, `ms` | `ApiHelper.apiCall` |

`api_error` never has the backend's message, the body or a query string (they can hold personal data).
`endpoint` is built from `ApiConfig.Endpoints` templates, not the request URL.

## 13. Where the journey stitches together

These are the hand-offs between features that the funnels in [dashboards.md](dashboards.md) rely on. Keep their
`source` values in sync when routes change:

- notification / push → task page / check-in tray → `checkin_submitted.source = notification`
- Home's Live Appointment → `live_appointment_viewed` → `video_call_opened` → `signature_submitted.during_call`
- doctor profile (any `source`) → `booking_started.source` → `booking_completed`
- past `appointment_details_viewed` → `review_submitted`
