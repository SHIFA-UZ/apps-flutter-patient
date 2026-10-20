# 16 — Implementation Plan (tickets, in user-journey order)

The tickets follow the order a patient moves through the app: **Splash → Login/Register → Shell → Home → Doctors → Booking → Visits → Documents → Tasks → Chat → Notifications → Shifa AI → Profile**. They start with a short foundation milestone, because every screen needs it.

> **No onboarding screen exists** in the Flutter app. The first screen is the Splash (`/`), which goes to `/login` or `/home`. If you want an onboarding carousel in the new app, add `ONB-01` (below) as a *new* feature. There is nothing to port.

## How to read a ticket

```
### ID · Title                                         [size S/M/L]
Route · Spec: doc §section · API: endpoint IDs from 15-api-reference.md §2
Build:     what to implement
Done when: acceptance checks
Depends:   ticket IDs that must be merged first
Fix:       known Flutter bug to NOT copy (see 00-README "Verified bugs")
```

Size: **S** ≈ ½–1 day, **M** ≈ 2–3 days, **L** ≈ 4–6 days for one developer working with AI assistance.

### Definition of done (every ticket)

- Works on **Android and iOS**.
- Every string comes from `composeResources`, in all 4 locales (`en`, `uz`, `ru`, `de`).
- Loading, error (with retry) and empty states are present wherever the spec lists them.
- Repository code has unit tests using Ktor `MockEngine` with the JSON examples from `15-api-reference.md` §3.
- If the implementation differs from the spec, fix the spec doc in the same PR.

### Prompt template for handing a ticket to Claude

```
Implement ticket AUTH-02 from docs/spec/16-implementation-plan.md.
Read the spec sections it lists (plus 15-api-reference.md for the endpoints,
04-design-system-and-ui-kit.md for UI). Plan first, then build data layer →
ViewModel → screen. Respect the "Fix" line. Where the spec says UNVERIFIED,
check the Flutter repo (added via --add-dir) before deciding.
```

---

## Milestone overview

| # | Milestone | Tickets | Result you can demo |
|---|---|---|---|
| M0 | Foundation | CORE-01…07 | Empty app with theme, strings, HTTP client, storage, navigation skeleton |
| M1 | Launch & Auth | AUTH-01…08, LOCK-01 | Register, log in, reset password, and a restored session |
| M2 | App shell | SHELL-01…03 | Bottom tabs, redirect guard, header with badge slots |
| M3 | Home | HOME-01…04 | Home shows upcoming visits, tasks, nearby doctors |
| M4 | Doctors | DOC-01…03 | Search, filter and open a doctor profile |
| M5 | Booking & payment | BOOK-01…07 | Book, pay (Click.uz), list, cancel, reschedule |
| M6 | Visit & after-visit | BOOK-08…13 | Video call, AI summary, e-sign, treatment plan, review, reminders |
| M7 | Documents | DOCS-01…03 | Upload, view and delete medical documents |
| M8 | Tasks | TASK-01…02 | Remote-care tasks and check-ins |
| M9 | Chat | CHAT-01…06 | Doctor chat with text, image, voice and document |
| M10 | Notifications | NOTIF-01…04 | Inbox, push, and tap-to-screen routing |
| M11 | Shifa AI | AI-01…03 | Streaming assistant, voice input, AI booking |
| M12 | Profile & account | PROF-01…07, LOCK-02…03 | Profile, edit, location, language, app lock, delete account, logout |
| M13 | Release | REL-01…04 | Tests, crash reporting, store migration, release builds |

Do **CORE-07 (risk spikes)** early, in parallel with M1. Video, push, audio and WebView are the parts most likely to force architecture changes.

---

## M0 — Foundation

### CORE-01 · Project setup & CI  [S]
Spec: 01 §15–17
Build:
- KMP project (Android + iOS, shared Compose UI), with the module/package layout from 01 §16.
- Package IDs: see REL-03 (decide same or new IDs *now*).
- CI pipeline: build both platforms, lint, unit tests.

Done when: the app launches on both platforms and CI is green.

### CORE-02 · Design system  [M]
Spec: 04 §2–5
Build:
- `ShifaTheme`, mapping the color/typography/spacing/shape tokens to `MaterialTheme`.
- The dimension constants.
- The assets from 04 §7.

Done when: a debug "catalog" screen shows the colors, text styles and buttons, and they match the Flutter app side by side.
Fix: pick **one** teal. The token is `#00BBB0`, but `#17C3B2` is hardcoded in 19 files (04 quirks).

### CORE-03 · Shared UI kit  [M]
Spec: 04 §6
Build: `PrimaryButton`, `SecondaryButton`, `BaseCard`, `EmptyState`, `SectionTitle`, `UserAvatar`, `SegmentedControl`, `AppVersionFooter`, `LanguageMiniToggle`. The header and phone input come later, in SHELL-03 and AUTH-03.
Done when: every widget appears in the catalog screen, with all its states.
Depends: CORE-02

### CORE-04 · Strings & localization  [S]
Spec: 01 §10, `strings/README.md`
Build:
- Convert `strings/*.json` to `composeResources/values{,-uz,-ru,-de}/strings.xml`, and convert the placeholders.
- In-app language override with persistence (pref `app_language`).

Done when: switching language at runtime re-renders the catalog screen. A test fails if any key is missing from a locale.
Fix: fill the missing `de`/`en` keys and `priceNotSet`, or explicitly accept the English fallback.

### CORE-05 · HTTP client & error model  [M]
Spec: 01 §4, §8–9 · 15 §1
Build:
- A Ktor client with JSON, 30 s timeouts, and base-URL resolution.
- A Bearer-token plugin that **skips the 16 public `/auth/*` paths**.
- 401 → logout, with these exceptions: `/auth/*` or `/public/*` paths, requests sent without a token, the 10 s grace period, and the 5 s rate limit. A 403 never logs out.
- Error-body parsing (`message` / `error`), the sanitizer, and the error → localized-message mapper.
- A photo/file URL resolver.

Done when: there are unit tests for every 401/403 branch and for the sanitizer patterns.
Fix: resolve relative photo URLs against the **real backend origin**, not `localhost:8090`.

### CORE-06 · Storage & DI  [S]
Spec: 01 §6
Build:
- Secure storage via `expect`/`actual`: iOS Keychain; on Android, `EncryptedSharedPreferences` or DataStore with Tink.
- `multiplatform-settings` for non-secret prefs.
- The first-launch token wipe (`has_launched_before`).
- Koin modules.

Done when: a token survives an app restart, and a reinstall on iOS wipes it.

### CORE-07 · Native risk spikes (throwaway)  [L]
Spec: 06 §3.6 & §10, 13 §3.1–3.2, 08 §6.3, 06 §3.2
Build: minimal proofs on **iOS and Android**:
- a Daily call joining a test room
- FCM/APNs token + push received
- recording and playback of AAC `.m4a` (32 kbps / 16 kHz mono)
- a WebView that intercepts a URL

Done when: each works, and the chosen library or `expect`/`actual` approach is written down in the new CLAUDE.md.

### ONB-01 · Onboarding carousel (optional, new)  [S]
**Not in the Flutter app.** Only do this if product wants it: show it once before `/login`, controlled by a new pref flag.

---

## M1 — Launch & Auth

### AUTH-01 · Splash & session restore  [S]
Route `/` · Spec: 05 §2.1, §3.1 · 02 §8 · API: P1
Build:
- The splash animation: 1600 ms, with a 5 s hard fallback.
- Token check → `GET /patients/me/profile` → decide between `/home` and `/login`.
- The auth state (`isAuthReady`, `isAuthenticated`, `forcePasswordReset`).

Done when:
- A valid token goes to Home.
- A missing or invalid token goes to Login.
- Airplane mode goes to Login, and does not hang.

Depends: CORE-05, CORE-06
Decide: `forcePasswordReset` is not persisted across restarts (05 quirk Q4). Persist it or not.

### AUTH-02 · Login  [M]
Route `/login` · Spec: 05 §2.2, §3.2, §6.2 · API: A1
Build:
- Identifier input: an email is sent as-is; a phone is normalized.
- Password, with the sign-in button disabled while loading or locked.
- Lockout: persisted counters (`login_failed_attempts`, `login_lockout_until_epoch_ms`), the "attempts remaining" banner, the locked banner with a 30 s countdown refresh.
- Links to Forgot password and Create account.
- Language toggle and version footer.

Done when: every lockout rule in 05 §6.2 is covered by a unit test. Wrong credentials show the sanitized message.
Depends: AUTH-01, CORE-03

### AUTH-03 · Create account + phone input  [L]
Route `/create-account` · Spec: 05 §2.4, §3.3 · 14 §3.4 · API: A2, A4, A5
Build:
- The form, with the password policy (05 §6.3) and a live checklist.
- `PhoneInputField` with a country picker.
- `check-identifier` handling:
  - patient → "already exists" dialog
  - doctor → go to AUTH-05
  - none → continue
- OTP channel choice: email vs SMS, with the email fallback on 503/429 or after 3 SMS sends.

Done when: all branches of the 05 §2.4 flowchart work.
Depends: AUTH-02
Fix: `PhoneInputField` misparses `+998…` numbers, and switching country corrupts the number (04, 14 §3.4).

### LOCK-01 · App-lock core service  [S]
Spec: 03 §2, §5 · 01 §6
Build:
- PIN rules (4–6 digits).
- Storage keys: enabled, biometric, inactivity seconds.
- Cooldown per failed attempt, and the max attempts.
- A temporary-disable API, for pickers, camera and video.

Done when: unit tests cover the PIN rules and the cooldown schedule.
Fix: **store a salted hash**, not the PIN itself, and compare in constant time.

### AUTH-04 · Register OTP verify + PIN setup prompt  [M]
Route `/register-otp-verify` · Spec: 05 §2.4 steps 8–9, §3.4 · API: A4/A5 (resend), A7
Build:
- A 6-digit code screen with a masked destination.
- A 300 s resend cooldown shown as `m:ss`.
- `register-patient` → save the token.
- The optional "Set up PIN" dialog pair, which calls LOCK-01 → Home.

Done when: a new account can be registered end to end against the test backend.
Depends: AUTH-03, LOCK-01

### AUTH-05 · Existing doctor → patient account  [M]
Routes `/confirm-doctor-to-patient`, `/doctor-otp-verify` · Spec: 05 §2.5, §3.5–3.6 · API: A4, A6
Build: the confirm screen, then OTP verify, then `create-patient-for-doctor`, then the same PIN prompt as AUTH-04.
Done when: a doctor identity can create a patient account and land on Home.
Depends: AUTH-04
Decide: a doctor matched by phone only dead-ends, because email is required (05 quirk Q7).

### AUTH-06 · Forgot password (3 screens)  [M]
Routes `/forgot-password`, `/forgot-password-otp`, `/forgot-password-new-password` · Spec: 05 §2.6, §3.7–3.9 · API: A9, A10
Build: identifier → OTP → new password → auto-login.
Done when: the password can be reset via email and via phone.
Depends: AUTH-02

### AUTH-07 · Forced password reset  [S]
Route `/reset-password` · Spec: 05 §2.3, §3.10 · API: A8 (authenticated)
Build: a screen with no back button, the policy checklist, and `go('/home')` on success.
Done when: a user flagged `forcePasswordReset` cannot reach any other screen until the password is reset.
Depends: AUTH-02, SHELL-02

### AUTH-08 · Logout & session expiry  [S]
Spec: 05 §2.8–2.9 · 14 §3.1.2 · API: P7 (`fcmToken: ""`)
Build:
- Logout steps: clear the FCM token on the backend, clear the PIN and disable the lock, wipe the token, reset the auth state.
- The ApiClient 401 callback leads to the same path.

Done when: after logout, the next user sees no previous-user data. Test this by logging in as a second account.
Depends: AUTH-01
Fix:
- **Clear copilot memory (`copilot_*_v1`) and the profile state** on logout.
- **Await** the logout before navigating.

---

## M2 — App shell

### SHELL-01 · Tab shell & bottom bar  [M]
Spec: 02 §5–7 · 04 (bar spec)
Build:
- 4 persistent tabs: Home, Bookings, Documents, Doctors. Each has its own back stack and keeps its state when switching.
- A plain shell for Account, Chat, Notifications, Tasks and Shifa AI: the same bar, with no highlighted tab.
- The center Shifa AI button (hidden until AI-01).
- Bottom content padding: 93 + system inset.

Done when: switching tabs keeps scroll position and back stack; Android back behaves as in 02 §10.
Depends: CORE-03

### SHELL-02 · Redirect guard & route table  [S]
Spec: 02 §2–3 · 05 §5.3
Build:
- All routes from 02 §3 as type-safe destinations (placeholders for screens not built yet).
- The auth whitelist and the redirect rules.

Done when:
- A deep link to a protected route while logged out goes to Login.
- A logged-in user who opens `/login` is sent to Home.

Depends: SHELL-01, AUTH-01
Skip: the `/link` and `/__/auth` Firebase sinks, since Firebase Auth isn't used.

### SHELL-03 · App header  [S]
Spec: 04 (AppHeader) · 10 §3.3
Build: the header with the chat and notification icon buttons. Badge counts are wired later in CHAT-06 and NOTIF-01.
Done when: the header renders on Home, with tappable icons that go to `/chat` and `/notifications`.
Depends: SHELL-01

---

## M3 — Home

Home shows data from three features, so the data layers for **appointments, tasks and doctors are built here**. Their full screens come later.

### HOME-01 · Home layout + Upcoming appointments  [M]
Route `/home` · Spec: 10 §2–3.4 · 06 §5.9 (Appointment model) · API: B2 (`status=CONFIRMED&startDate=`), P1
Build:
- The Home scaffold, header and pull-to-refresh.
- `Appointment` model and repository.
- `AppointmentCard` (06 §4.7).
- The upcoming-appointments section.

Done when: it shows confirmed future appointments, or the empty state; tapping a card goes to `/bookings/{id}` (placeholder until BOOK-06).
Depends: SHELL-03

### HOME-02 · My Tasks section  [S]
Spec: 10 §3.5 · 12 §5 · API: T1
Build: the `RemoteCareTask` model and repository, and the section using `RemoteCareTaskCard`.
Done when: it shows the tasks list, or empty; tapping a task goes to `/tasks` (placeholder until TASK-01).
Depends: HOME-01

### HOME-03 · Doctors near me section  [M]
Spec: 10 §2.2, §3.6 · 07 §4.1 · API: D1 (`pageSize=20`)
Build:
- The `Doctor` model and repository.
- A "use my location" permission flow.
- The client-side 50 km filter.

Done when: nearby doctors show once location is granted, with a sensible state when it is denied; tapping a doctor goes to `/doctors/{id}`.
Depends: HOME-01

### HOME-04 · Shared-state check  [S]
Spec: 10 §5
Build: make sure Home uses its **own** state and does not overwrite the Doctors/Bookings tab lists. In Flutter they share one provider and "last loader wins", which is a bug.
Done when: loading Home doesn't reset the Doctors tab's filters or page.
Depends: HOME-03, DOC-01

---

## M4 — Doctors

### DOC-01 · Doctors list: search, filters, pagination  [L]
Route `/doctors` · Spec: 07 §1.3, §2.1–2.7, §5 · Appendices A–C · API: D1, B2 (Recent), B1 (availability enrichment)
Build:
- Debounced search.
- Quick filter chips.
- The filters sheet, using the full profession/region catalogs.
- The pagination bar.
- The discovery and doctor cards, and the availability enrichment (07 §3.8).

Done when: every filter serializes to exactly the query params in 07 §2.2, and paging works.
Depends: HOME-03

### DOC-02 · Doctor profile + reviews  [L]
Route `/doctors/:id` · Spec: 07 §1.4, §2.8, §4 · API: D2, D3, D4
Build: all profile sections in order; the reviews list; the "Book" CTA → `/bookings/flow/{doctorId}`.
Done when: every section in 07 §2.8 renders, including the price, experience and language formatting (07 §4.3).
Depends: DOC-01

### DOC-03 · Certificate viewer  [S]
Spec: 07 §2.9, §3.9
Build: the image/PDF certificate viewer.
Done when: it opens from the doctor profile.
Depends: DOC-02, DOCS-03 (shares the PDF renderer; a stub is fine until then)

---

## M5 — Booking & payment

### BOOK-01 · Booking flow: location, date, slots, video/service, reason  [L]
Route `/bookings/flow/:doctorId` · Spec: 06 §3.1 steps 1–8, §4.1, §7.1 · API: D2, D3, B1
Build:
- The location step, required only for in-person bookings with more than one location.
- The "next available date" search, going day by day up to 30 days ahead.
- The date picker, up to 90 days ahead.
- Slot auto-select.
- The video toggle and service dropdown.
- The reason field.
- Times in UTC, shown in local time.

Done when: the slots shown match what the backend returns for the chosen day, location and type.
Depends: DOC-02

### BOOK-02 · Booking attachments  [S]
Spec: 06 §3.1 step 9 · 11 §7.1 · API: F2 (`category=PRE_APPOINTMENT`), F3
Build:
- A PDF picker (max 5), with the app lock temporarily disabled while it's open.
- Upload immediately; delete best-effort when a chip is removed.

Done when: attachments appear on the created appointment.
Depends: BOOK-01, LOCK-01

### BOOK-03 · Confirm & create booking  [M]
Spec: 06 §3.1 steps 10–11, §5.1 · API: B3
Build:
- The video-service checks.
- The double-booking guard.
- `bookAppointment`, including reschedule via `rescheduleId`.
- The success snackbar, then go to `/bookings`.
- If `paymentStatus == PENDING`, go to BOOK-04.

Done when: both free and paid bookings are created, and the list refreshes.
Depends: BOOK-01
Skip: `/bookings/confirm` (`ConfirmBookingScreen`) is dead in Flutter, because `/bookings/:id` shadows it.

### BOOK-04 · Consultation payment (Click.uz + Stripe fallback)  [L]
Routes `/bookings/:id/pay` and the in-flow checkout · Spec: 06 §3.2–3.3, §4.4, §5.2, §7.2 · API: $1, $3
Build:
- Create the checkout.
- An in-app **WebView** that intercepts `/api/payments/checkout/success|cancel`.
- Confirmation **only** when `GET /payments/{id}` returns `PAID`, polled every 3 s.
- The pending screen.
- A Stripe fallback if the Click page fails to load.

Done when: in the payment sandbox, success, cancel, and closing the app mid-payment each end in the correct state.
Depends: BOOK-03, CORE-07

### BOOK-05 · Bookings list  [M]
Route `/bookings` · Spec: 06 §3.10, §4.2, §4.7
Build: tabs and filters per 06 §4.2, the cards, "pay later" from a card, and refresh.
Done when: each appointment status shows the label, color and actions listed in 06 §5.9.
Depends: HOME-01, BOOK-04

### BOOK-06 · Appointment details, cancel, reschedule  [M]
Route `/bookings/:id` · Spec: 06 §3.5, §4.3 · API: B4, B5
Build:
- The details screen.
- Actions allowed per status.
- Cancel: a hard `DELETE`, with a confirmation.
- Reschedule: opens the flow with `?rescheduleId=`.

Done when: cancel and reschedule update the list and Home.
Depends: BOOK-05

### BOOK-07 · Create booking (doctor picker)  [S]
Route `/bookings/create` · Spec: 06 §3.11
Build: doctor search/pick → booking flow.
Done when: reachable from the Bookings tab, and it does not disturb the Doctors tab state.
Depends: BOOK-01

---

## M6 — Visit & after-visit

### BOOK-08 · Waiting room + video call  [L]
Routes `/bookings/:id/waiting`, `/bookings/:id/video` (full screen) · Spec: 06 §3.6, §5.3, §10 · API: V1
Build:
- The waiting room.
- Get the Daily token; the backend refuses it for unpaid consultations.
- Camera and mic permissions and toggles.
- Leave and end-call handling.
- Temporarily disable the app lock during the call.

Done when: a patient and a doctor (using the doctor app) complete a call on both platforms.
Depends: BOOK-06, CORE-07, LOCK-01

### BOOK-09 · AI visit summary  [M]
Route `/bookings/:id/visit-summary` · Spec: 06 §3.7, §5.4 · API: B8, B9, B10
Build: generate, then fetch; render the summary; the "ask a question" box.
Done when: it works for a completed appointment, including the "still generating" state.
Depends: BOOK-06

### BOOK-10 · E-signing (appointment + medical form 025-2)  [M]
Routes `/bookings/:id/sign`, `/bookings/sign-form/:formId` · Spec: 06 §3.8, §5.6 · API: B4, B6, B11, B12
Build: the summary to sign, a signature canvas → PNG, and submit.
Done when: the signed state is reflected on the details screen.
Depends: BOOK-06

### BOOK-11 · Treatment plan summary + payment  [M]
Routes `/bookings/treatment-plan/:planId[/pay?installmentItemId=]` · Spec: 06 §3.4, §4.5, §5.5 · API: B13, $2, $3
Build: the plan lines and installments; pay the full plan or one installment (online payment is UZS only); reuse the BOOK-04 WebView.
Done when: paying an installment updates the plan state.
Depends: BOOK-04

### BOOK-12 · Submit review  [S]
Route `/bookings/:id/review` · Spec: 06 §3.9, §5.7 · 07 §3.4 · API: B7, D5
Build: rating and comment; if a review already exists (from `B7`), show it instead.
Done when: the review appears on the doctor profile.
Depends: BOOK-06, DOC-02

### BOOK-13 · Local appointment reminders  [S]
Spec: 06 §8 · 13 §8.1, §8.3
Build: schedule and cancel local reminders on the `appointments_channel`, with a route in the payload.
Done when: a reminder fires at the right local time, and tapping it opens the appointment.
Depends: BOOK-06, NOTIF-03 (the channel setup; stub it until then)

---

## M7 — Documents

### DOCS-01 · Documents list & categories  [M]
Route `/documents` · Spec: 11 §2.1, §3.1, §5 · API: F1
Build: the list, the category filter (all values in 11 §5.2, plus `PRE_APPOINTMENT`), and `DocumentCard`.
Done when: filtering matches 11 §5.2, and the empty state shows.
Depends: SHELL-01

### DOCS-02 · Upload document  [M]
Spec: 11 §2.2, §4.2 · API: F2
Build: pick a file or take a photo (app lock temporarily disabled); choose a category; multipart fields as listed in 11 §4.2.
Done when: the uploaded file appears in the list with the right category.
Depends: DOCS-01, LOCK-01

### DOCS-03 · Document details, viewer, delete  [M]
Routes `/documents/:id`, `/documents/viewer` · Spec: 11 §2.3, §3.2–3.3, §4.3–4.4 · API: F3, F4
Build:
- A PDF renderer (PdfRenderer on Android, PDFKit on iOS) and an image viewer.
- The details screen.
- Delete, with a confirmation.

Done when: PDFs and images open in the app; deleting removes the document from the list.
Fix: send the Bearer token **only to the backend origin**, and don't log the user out on a 401 from a file host (11 §8).

---

## M8 — Tasks

### TASK-01 · Tasks list  [S]
Route `/tasks` · Spec: 12 §2.1, §3.1, §5 · API: T1
Build: the list with status and due display.
Done when: every task type and status in 12 §5.1 renders correctly.
Depends: HOME-02

### TASK-02 · Task check-in  [M]
Route `/tasks/:id/check-in` · Spec: 12 §2.2, §3.2, §4.2–4.4 · API: T2, T3, T4
Build: the check-in form for each input type, validation, and submit.
Done when: submitting a check-in updates the task's progress.
Depends: TASK-01

---

## M9 — Chat

### CHAT-01 · Conversation list + start a chat  [M]
Route `/chat` · Spec: 08 §5.3, §3.1, §6.4 · API: C1, C5
Build: the conversation list, and searching doctors to start a new conversation.
Done when: the list shows the last message and unread count per conversation.
Depends: SHELL-03

### CHAT-02 · Conversation: text, polling, read  [M]
Spec: 08 §5.4–5.5, §6.1, §7.1 · API: C2 (5 s poll while open), C3, C4
Build: text bubbles, optimistic send with reconciliation, the 5 s poll that stops when you leave the screen, mark read, and link detection.
Done when: messages from the doctor app appear within about 5 s; a failed send shows as failed.
Depends: CHAT-01

### CHAT-03 · Image messages  [S]
Spec: 08 §6.2, §7.2 · API: C7, C3
Build: pick and upload an image (app lock temporarily disabled), the image bubble with caching, and a full-screen viewer.
Depends: CHAT-02

### CHAT-04 · Voice messages  [M]
Spec: 08 §6.3, §7.3 · API: C7, C3
Build: the recording dialog (AAC-LC `.m4a`, 32 kbps, 16 kHz, mono), the voice bubble with playback, and the transcription display.
Depends: CHAT-02, CORE-07

### CHAT-05 · Document bubbles  [S]
Spec: 08 §5.5
Build: a document bubble that opens the file with the DOCS-03 viewer.
Depends: CHAT-02, DOCS-03

### CHAT-06 · Chat unread badge  [S]
Spec: 08 §5.2 · API: C6 (poll every 10 s)
Build: the badge on the header chat icon; stop polling when logged out.
Depends: CHAT-01, SHELL-03

---

## M10 — Notifications

### NOTIF-01 · Inbox, details sheet, badge  [M]
Route `/notifications` · Spec: 13 §3.4, §4.1–4.3, §6.1, §6.3–6.4 · API: N1, N2, N3, N4
Build:
- The inbox with localized title/body templates per type (13 §6.3).
- The details sheet.
- Mark one read and mark all read.
- The bell badge.

Done when: every notification type renders with the right text and style.
Depends: SHELL-03

### NOTIF-02 · Document access approve / reject  [S]
Spec: 13 §3.5 · API: N5, N6
Build: approve and reject actions on access-request notifications.
Depends: NOTIF-01

### NOTIF-03 · FCM token & push display  [L]
Spec: 13 §3.1–3.2, §8.1–8.2, §10 · API: P7
Build:
- Permission request timing.
- Register the token after login and on refresh; clear it on logout (AUTH-08).
- Notification channels.
- In the foreground, show pushes as local notifications.

Done when: push arrives in the foreground, the background and the terminated state, on both platforms.
Depends: CORE-07, AUTH-08

### NOTIF-04 · Tap routing (push, local, inbox)  [M]
Spec: 13 §3.3, §6.2 · 02 §9 · 06 §2.1
Build: payload → route for every type in 02 §9.2, including taps that cold-start the app (route after auth is ready).
Done when: every type in the table opens the right screen from a cold start.
Depends: NOTIF-03, all target screens
Fix: a chat notification should open **the conversation**. Flutter ignores `chatId`.

---

## M11 — Shifa AI

### AI-01 · Gating, entry button, streaming chat  [L]
Route `/shifa-ai` · Spec: 09 §0–1, §2.1, §3–5.1, §5.8–5.9, §6–7 · API: X1 (SSE)
Build:
- **One** gating rule: the `PATIENT_SHIFA_AI` feature, falling back to the tier. Use it for the bar, the router and the screen.
- The center button.
- The SSE stream rendering.
- Local memory.
- Clear chat.
- Rate-limit and error handling.

Done when: gated users can't reach the screen by any path; replies stream token by token.
Depends: SHELL-01, PROF-01 (profile features; the profile data layer from AUTH-01 is enough)
Fix: use the **shared Ktor client** (timeouts and 401 handling). Memory must be cleared on logout (AUTH-08).

### AI-02 · Voice input + transcription feedback  [M]
Spec: 09 §2.5–2.6, §5.7 · API: X2, X3
Build: reuse the CHAT-04 recorder; transcribe; offer "report incorrect text" when `public/config` enables it.
Depends: AI-01, CHAT-04

### AI-03 · Doctor suggestions & AI booking  [M]
Spec: 09 §2.2–2.4, §5.2–5.6 · API: X7, X5, X4
Build: suggestion cards, booking preview and confirm, quick follow-up chips, and manual booking via the booking flow.
Done when: a booking made from the AI appears in the Bookings list.
Depends: AI-01, BOOK-03
Skip: X6 `suggest-doctors`, which is dead.

---

## M12 — Profile & account (end of the journey)

### PROF-01 · Profile screen + language dialog  [M]
Route `/account` · Spec: 14 §3.1, §3.1.1, §6.1–6.2 · API: P1, P2 (`language`)
Build:
- The profile card.
- The Edit button.
- The Settings section: Language, App lock, Change password, Privacy.
- The Account section: Delete, Log out.
- The version footer.
- The language dialog, which updates both the app and the backend.

Done when: changing the language updates the UI and `PATCH`es the profile.
Depends: SHELL-01
Fix: Flutter's Privacy item does nothing when tapped. Link it to the real privacy policy, or hide it.

### PROF-02 · Avatar upload  [S]
Spec: 14 §2.3 · API: P3 (fallback P4, **UNVERIFIED**)
Build: pick or take a photo (app lock temporarily disabled), upload it, and refresh the avatar with a cache-buster.
Depends: PROF-01, LOCK-01

### PROF-03 · Edit profile  [M]
Route `/account/edit` · Spec: 14 §3.2, §7 · API: P2
Build: the fields and validation from 14 §7, and a "discard changes" prompt.
Depends: PROF-01, AUTH-03 (phone input)

### PROF-04 · Location picker + geocoding  [M]
Spec: 14 §2.4, §3.3, §4.7–4.8
Build:
- A map picker (MapLibre or Google Maps Compose).
- Current location via geolocation.
- Forward and reverse geocoding.

Done when: a picked location is saved to the profile on both platforms.
Fix: ship a real Maps/geocoding key, or add a fallback geocoder. In Flutter store builds a picked location is silently lost.

### PROF-05 · Change password  [S]
Route `/account/change-password` · Spec: 05 §2.7, §3.11 · API: A8
Build: current/new/confirm fields with the policy checklist.
Depends: PROF-01

### LOCK-02 · App-lock settings  [M]
Route `/account/app-lock` · Spec: 03 §7
Build: enable/disable, set or change the PIN, the biometric toggle, and the inactivity timeout.
Depends: LOCK-01, PROF-01

### LOCK-03 · Lock screen, lifecycle triggers, biometrics  [L]
Spec: 03 §4–6, §8, §11 · 10 §3.7
Build:
- A root gate and a resume trigger based on inactivity time.
- The lock screen with cooldown and max attempts (reaching the max forces a re-login).
- Biometrics: `androidx.biometric` on Android, `LocalAuthentication` on iOS.
- Respect temporary-disable windows.
- The Home "enable biometrics?" prompt.

Done when: every transition in the 03 §5 state machine is tested, and nothing freezes (03 §11).
Depends: LOCK-02
Fix: **verify on submit, or at the stored PIN's length**. Flutter checks at 4 digits, so a 5–6 digit PIN can never unlock.

### PROF-06 · Delete account (email OTP)  [M]
Route `/account/delete-account-verify` · Spec: 14 §2.2, §3.1.3, §3.5, §4.4–4.6 · API: P5, A4 (`purpose: ACCOUNT_DELETION`), P6
Build: a confirm dialog → request with a 10 s timeout → the OTP screen → confirm → logout.
Done when: a test account can be deleted end to end.
Depends: PROF-01, AUTH-08
Decide: phone-only accounts cannot delete their account in Flutter, because email is required. Ask the backend team for a phone path.

### PROF-07 · Logout from profile  [S]
Spec: 14 §3.1.2
Build: a confirm dialog → AUTH-08 (awaited) → `/login`.
Depends: PROF-01, AUTH-08

---

## M13 — Release

### REL-01 · Port behavioral tests  [M]
Spec: 01 §18
Build: port the contracts the Flutter tests cover: connectivity, error localization, localization completeness, notification tap handler, waiting room, and the smoke test.

### REL-02 · Crash reporting  [S]
Spec: 01 §2
Build: Crashlytics (it was a dependency in Flutter but never initialized), or another crash reporter.

### REL-03 · App identity & migration  [M]
Spec: 01 §14 · 00-README
Decide:
- Keep `com.shifa.patientapp` / `uz.shifa.patient` (a store update), or use new IDs.
- If you keep them, either read the legacy Flutter secure storage once at first launch, or accept that existing users log in again and set their PIN again.
- New IDs need new Firebase app registrations.

### REL-04 · Release builds & store listing  [M]
Build: signing, versioning, TestFlight and Play internal track, and the permission usage strings from 01 §14.

---

## Not ported (dead in Flutter — only build if product asks)

| Item | Where |
|---|---|
| Firebase Phone Auth, `/link` and `/__/auth` routes | 05 §1.1, 02 |
| `/account-info` screen (orphaned) | 05 §3.12 |
| `/bookings/confirm` screen (shadowed, never used) | 02 §3.1, 06 |
| `POST /auth/check-existing-doctor` | 15 §2.1 A3 |
| `POST /patients/me/copilot/suggest-doctors` | 15 §2.11 X6 |
| Notification polling service, in-app "new notification" dialog | 13 §4.4, §9 |
| `ImageCompressionService`, `TextCleaner` (unused) | 08 §8.2, §8.6 |

## Critical path

```
CORE-01 → CORE-05/06 → AUTH-01 → AUTH-02 → AUTH-03 → AUTH-04 → SHELL-01/02 → HOME-01
  → DOC-01 → DOC-02 → BOOK-01 → BOOK-03 → BOOK-04 → BOOK-06 → BOOK-08
(CORE-07 spikes run in parallel from week 1. Documents, Tasks, Chat, Notifications
 and Profile can run in parallel once M2 is done, if you have more than one developer.)
```
