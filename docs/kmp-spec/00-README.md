# Shifa Bemor Patient App — Rebuild Spec for Kotlin Multiplatform + Compose Multiplatform

This folder describes the Flutter app `shifa_patient_app_v1` (v1.1.9+8). It is written so a team with **no access to the Flutter code** can rebuild the same product in KMP/CMP.

Each doc cites the Flutter sources as `lib/...:line` for traceability. Anything the code could not confirm is marked **UNVERIFIED**. Secrets are written as `<REDACTED>`.

## How to use this in the KMP project

1. Copy the whole `kmp-spec/` folder into the new repo, for example as `docs/spec/`.
2. Read this file first, then read `01`, `02` and `15`. They set the global rules that every feature relies on.
3. Implement one feature at a time. Give the implementer (human or AI) that feature's doc, plus `15-api-reference.md` and `04-design-system-and-ui-kit.md`.
4. For **API contract questions**, `15` is the source of truth. For **UI and behavior**, the feature doc is the source of truth. If they disagree, trust `15` for payloads and flag the disagreement.
5. Each doc ends with **"Quirks / tech debt"**. For each item, decide whether to replicate it (backend compatibility) or fix it (bugs). Don't port bugs blindly.

## Index

| # | File | Covers |
|---|------|--------|
| 01 | [01-architecture-and-core.md](01-architecture-and-core.md) | Bootstrap order, base URL and environments, full ApiClient behavior (401/403, grace period), **every storage key**, logger redaction, error sanitizing and localization, the localization system, connectivity, subscription gating, platform permissions, dependency → KMP library table, recommended module layout, tests to port |
| 02 | [02-navigation-and-shell.md](02-navigation-and-shell.md) | Every route (path, params, extras, shell), auth redirect guard, persistent vs rebuilt tabs, bottom bar, Shifa AI floating button, splash, deep-link and notification routing, back behavior |
| 03 | [03-app-lock.md](03-app-lock.md) | PIN and biometric lock: storage, cooldowns, max attempts, inactivity timeout, lifecycle gates, temporary disable around pickers/video, settings screen, state machine |
| 04 | [04-design-system-and-ui-kit.md](04-design-system-and-ui-kit.md) | Color, typography and spacing tokens, the Material 3 theme, and a spec for every shared widget and asset |
| 05 | [05-auth.md](05-auth.md) | Login, registration with OTP, existing doctor → patient account, forgot/forced/change password, session restore, logout, login lockout |
| 06 | [06-bookings-payments-video.md](06-bookings-payments-video.md) | Bookings list, booking flow (doctor → location → date → slot → reason → attachments → pay), cancel/reschedule, Click.uz and Stripe payments, waiting room, Daily video call, visit summary, treatment plans and installments, e-sign forms, reviews |
| 07 | [07-doctors.md](07-doctors.md) | Doctor discovery (search, filters, pagination), doctor profile, reviews, certificates, plus the full profession, region and country catalogs |
| 08 | [08-chat.md](08-chat.md) | Doctor–patient chat (REST polling), text/image/voice/document messages, upload, optimistic send, unread counts, voice transcription |
| 09 | [09-shifa-ai-copilot.md](09-shifa-ai-copilot.md) | Shifa AI assistant: SSE streaming, voice transcription, doctor suggestions, AI booking, local memory, gating |
| 10 | [10-home.md](10-home.md) | Home dashboard sections and their data sources |
| 11 | [11-documents.md](11-documents.md) | Medical documents: categories, upload, view (PDF/image), delete, doctor access requests |
| 12 | [12-tasks.md](12-tasks.md) | Remote care tasks and the check-in form |
| 13 | [13-notifications.md](13-notifications.md) | Notification inbox, FCM token lifecycle, push handling per app state, local reminder channels, each notification type → tap route |
| 14 | [14-profile-and-account.md](14-profile-and-account.md) | Profile, edit profile, avatar, location picker and geocoding, language switch, account deletion with OTP, logout |
| 15 | [15-api-reference.md](15-api-reference.md) | **Consolidated HTTP contract**: 67 backend endpoints plus third-party calls, reconstructed JSON schemas, tolerant-parsing rules, dead endpoints |
| — | [strings/](strings/README.md) | All UI strings exported losslessly: `en.json`, `uz.json`, `ru.json`, `de.json` (834 keys), with placeholder conventions and a guide to converting them to `composeResources/values*/strings.xml` |

## Global facts every feature depends on

These are summarized from 01, 05 and 15. See those docs for details.

- **Backend:** Spring Boot REST at `https://shifa-doc-backend-mvp-production.up.railway.app/api`. Client paths are written *without* `/api`.
  - The base URL can be overridden at build time; every build without an override uses production.
  - JSON only, 30 s connect/receive timeouts.
- **Auth:** a JWT sent as `Authorization: Bearer <token>`.
  - A fixed list of 16 `/auth/*` paths is always sent **without** a token (01 §4, 05 §4).
  - `/auth/reset-password` needs the token.
- **401 handling:** a 401 means clear the token and log out. It is ignored when any of these hold:
  - the path starts with `/auth/` or `/public/`
  - the request carried no `Authorization` header
  - the token was saved less than 10 s ago
  - a logout already happened less than 5 s ago
- **403 handling:** a 403 never logs the user out.
- **Secure storage** (Keychain / EncryptedSharedPreferences):
  - `auth_token`, `auth_token_saved_at` and `user_id` live in the `shifa_auth_secure` file.
  - App-lock keys live in the `shifa_app_lock_prefs` file.
  - Auth tokens are wiped on the first launch after a reinstall, because the iOS Keychain survives reinstalls.
- **Plain preferences:** `has_launched_before`, `app_language`, `login_failed_attempts`, `login_lockout_until_epoch_ms`, `shown_notification_ids`, `copilot_*_v1`.
- **Times:** sent as UTC ISO-8601 and shown in device-local time. The device time zone is sent with `PATCH /patients/me/profile {timeZone}` after login and on every app resume.
- **Push:** the FCM token is sent with `PUT /patients/me/fcm-token {"fcmToken": "..."}` after login. Logout sends `""`.
- **Real-time:** there are no websockets. Everything is REST polling:
  - the open chat every 5 s
  - the chat unread count every 10 s
  - payment status every 3 s during checkout
- **Shifa AI gating:** the `PATIENT_SHIFA_AI` entry in profile `features`, falling back to `subscriptionTier`. The router and the widgets check this differently (09 §7).
- **Firebase:** only FCM is needed.
  - Firebase Auth code exists but is never wired up; OTP goes through the backend.
  - Crashlytics is a dependency but is never initialized.
- **Locales:** `en`, `uz`, `ru`, `de`. Lookup falls back to English, then to the raw key.

## Verified bugs — do NOT replicate

I checked these directly in the source while compiling this index.

1. **A 5–6 digit PIN can never unlock the app.** The lock screen checks the PIN as soon as 4 digits are entered (`app_lock_screen.dart:83`). A failed check clears the input and counts an attempt. Enough failures force a re-login. Check only on submit, or at the stored PIN's length.
2. **Relative photo/file URLs resolve to `localhost:8090`** in every build (`ApiClient.publicBaseUrl`, `image_utils.dart:15,26`). Resolve them against the real backend origin.
3. **The app-lock PIN is stored unhashed** (inside encrypted storage). Store a salted hash instead; the PIN is only ever compared.
4. **Copilot memory (`copilot_*_v1`) survives logout.** On a shared device, the next user sees the previous user's AI history. Clear it on logout.
5. **Copilot calls bypass the main HTTP client.** They get no timeout and no 401 logout. Route them through the shared Ktor client.
6. **Store builds ship without a Google Maps key.** A location picked on the map is silently lost (14 §10).
7. **The Bearer token is attached to arbitrary `fileUrl`s.** A 401 from a file host logs the user out (11 §8). Attach the token only for the backend origin.
8. **Translation gaps** (`de` falls back to English):
   - `de` is missing 345 keys.
   - `en` is missing `discardChanges`, `noTasksFound`, `profileUpdatedSuccessfully` and `saveChanges`.
   - `priceNotSet` is missing in every language (07).
   - Missing keys render as the raw key. The full per-locale gap report is in `strings/README.md`.
9. **`PhoneInputField` misparses `+998…` numbers**, and changing the country corrupts the number (04).
10. **Notification taps to `/chat` pass a `chatId` that the screen ignores** (02, 08).

Each feature doc lists more quirks, including ones that exist for backend compatibility. Review each one.

## Where the code differs from the repo's CLAUDE.md / README

CLAUDE.md and the README are out of date. The spec follows the code:

- `--dart-define=FLAVOR` has no effect. Every build hits production unless `API_BASE_URL` is set.
- Firebase Phone Auth is not used, and Crashlytics is not initialized.
- The docs don't mention several features that are in the code: Shifa AI copilot, subscriptions, Click.uz/Stripe payments, treatment plans, e-sign forms and document access requests.
- The production app uses the Firebase project `shifa-doctor-staging` (01 §14).

## Suggested KMP stack

The full table and module layout are in 01 §15–16.

| Concern | Suggested library |
|---|---|
| Networking | Ktor client: auth plugin, SSE, multipart |
| JSON | kotlinx.serialization, with lenient decoders for the tolerant parsing in 15 §6 |
| Dependency injection | Koin |
| State | ViewModel + StateFlow |
| Navigation | Navigation Compose (type-safe routes, nested graphs per tab) |
| Preferences | multiplatform-settings |
| Secure storage | Keychain / EncryptedSharedPreferences via `expect`/`actual` |
| Images | Coil 3 |
| Dates and time zones | kotlinx-datetime |
| Push | GitLive Firebase Messaging, or native FCM/APNs |
| Native-only (`expect`/`actual`) | Daily video SDK, biometrics, audio record/play, PDF rendering, file and image pickers, maps, geolocation, WebView (Click.uz) |
