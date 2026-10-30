# 15 — HTTP API Reference (Shifa Bemor patient app)

Scope: every network call made by the Flutter patient app, reconstructed from the client source. The backend (Spring Boot) source was **not** available, so request and response contracts are inferred from what the client sends and parses. Anything the client code does not prove is marked **UNVERIFIED**.

Conventions used in this document:
- File references are relative to `lib/` unless they start with another root. `file:line` points to the line that issues the HTTP call.
- Secrets are written as `<REDACTED>`.
- "Req" means request body and "Res" means response body. `?` marks an optional or nullable key.

---

## 1. Global conventions

### 1.1 Base URLs and environment resolution

| Item | Value | Source |
|---|---|---|
| Production host | `https://shifa-doc-backend-mvp-production.up.railway.app` | `core/config/app_config.dart:9` |
| API base (Dio `baseUrl`) | `<host>/api` | `core/network/api_client.dart` (`apiBaseUrl` getter, ~l.15-40) |
| Override | `--dart-define=API_BASE_URL=<url>`. A trailing `/api` is stripped, then `/api` is appended again. | same |
| Default when no define is set | Production `/api` on every platform and build mode, including debug, web, Android and iOS. | `api_client.dart:33-37` |
| `ENVIRONMENT` define | Defaults to `development`. It only toggles logging flags and never changes URLs. | `app_config.dart:24` |
| `GOOGLE_MAPS_API_KEY` define | Defaults to `''`. See §4.1. | `app_config.dart:52` |
| `publicBaseUrl` (media) | Hardcoded to `http://localhost:8090`, or `http://10.0.2.2:8090` on Android. See §1.12. | `api_client.dart:42-50` |

> The table above describes the Flutter app. **KMP app (2026-10-30):** `ApiConfig.BASE_URL` is the production
> `/api/` base, a constant with no build-time override; `ApiConfig.DEV_BASE_URL` is the development backend
> `https://api-dev.shifa.uz/api/` (deployed from the backend's `main`, seeded with `seed.patient1@shifa.dev` /
> `Dev12345!`). Debug builds switch between them in Profile → Debug menu → Development backend (`BackendChoice`);
> release builds always use production. `https://api.shifa.uz` is the production name now; the Railway host above
> stays alive for the store builds that call it. There is no media `publicBaseUrl`: file URLs come absolute from the API.

All paths in this document are relative to `/api`. Example: `GET /patients/me/profile` means `GET https://…railway.app/api/patients/me/profile`.

### 1.2 Transports

| Transport | Used by | Consequences |
|---|---|---|
| Dio `ApiClient` (`core/network/api_client.dart`) | Almost everything | Bearer interceptor, 401 auto-logout, 30s timeouts, JSON headers |
| Dio `apiClient.dio.post` with `FormData` | Multipart uploads: documents, chat attachments, profile photo | Same interceptor. Dio replaces `Content-Type` with `multipart/form-data; boundary=…`. This is standard Dio 5 behaviour and was not re-verified at runtime. |
| Raw `package:http` | Copilot (`features/copilot/data/copilot_api.dart`), public config (`core/network/public_backend_config.dart`), Google geocoding, certificate viewer, dead `FileUploadService` | Bypasses the interceptor. The token is added manually where noted. **No 401 auto-logout. No global timeout** except where noted. |

`ApiClient.uploadFile()` exists (multipart field `file`) but has **no callers**.

### 1.3 Default headers (Dio)

```
Content-Type: application/json
Accept: application/json
Authorization: Bearer <JWT>    (added per request, see 1.4)
```

The copilot SSE stream additionally sends `Accept: text/event-stream`.

No other custom headers are sent: no locale header, app-version header, device-id header or `X-*` headers. The UI language reaches the backend only through profile `language` and explicit `language` fields in the AI endpoints.

### 1.4 Authentication

- **Scheme**: a JWT in `Authorization: Bearer <token>`. The token is opaque to the client, which never decodes it.
- **Where the token comes from**: the `token` field returned by login, register-patient, create-patient-for-doctor and forgot-password-reset.
- **Storage**: `FlutterSecureStorage` (Keychain/Keystore) under these keys:
  - `auth_token` holds the token.
  - `auth_token_saved_at` holds an ISO-8601 local timestamp.
  - `user_id` is not written by any API flow observed.
- **Paths that never carry a token**: `ApiClient.isUnauthenticatedAuthPath` (`api_client.dart:57-76`) matches a path when it equals, or ends with, one of these:
  - `/auth/login`
  - `/auth/register`, `/auth/register-patient`, `/auth/register-clinic-staff`
  - `/auth/verify-key`, `/auth/verify`, `/auth/verify-email-otp`
  - `/auth/check-existing-patient`, `/auth/check-existing-doctor`, `/auth/check-identifier`
  - `/auth/create-patient-for-doctor`
  - `/auth/send-email-otp`, `/auth/send-sms-otp`, `/auth/send-login-otp`
  - `/auth/send-forgot-password-otp`, `/auth/forgot-password-reset`

  Several of these are never called by the app: register, register-clinic-staff, verify-key, check-existing-patient, verify, verify-email-otp and send-login-otp.
- **Paths that do carry a token**:
  - Every other path gets the token if one is stored, **including `/public/**`**.
  - **`/auth/reset-password` is not on the list**, so it is authenticated.
  - If reading the token from Keystore fails, the request is sent without a token.
- **401 handling** (`api_client.dart:111-185`). The client clears the token and fires `onAuthError` (which calls `authNotifier.logout()` if currently authenticated) only when **all** of these hold:
  1. The request path does not start with `/public/` or `/auth/`.
  2. No logout is already in progress.
  3. The request actually carried an `Authorization` header.
  4. At least 5s have passed since the last logout attempt.
  5. A stored token still exists.
  6. At least 10s have passed since `auth_token_saved_at` (post-login grace period).
- **403**: never triggers logout. It is treated as a valid token with the wrong role. Example: a doctor account calling `/auth/login?app=patient`.
- **Token validation at startup**: `GET /patients/me/profile` (§3.2). **Only a 401** marks the session invalid; network errors keep the user signed in.
- **Logout** (`features/auth/providers/auth_provider.dart:202`):
  1. `PUT /patients/me/fcm-token {"fcmToken": ""}`, with errors ignored.
  2. Clear the PIN and the token locally.

  There is **no server logout endpoint and no refresh token**.

### 1.5 Timeouts

| Call | Timeout |
|---|---|
| All Dio calls | connect 30s, receive 30s. No send timeout is set. |
| Public config (`http.get`) | 5s |
| Certificate viewer (`http.get`) | 45s |
| Account-deletion OTP send | 10s UI-level `.timeout` on top of Dio |
| Account-deletion request | UI-level `.timeout` in `profile_screen.dart:665` (value not captured, **UNVERIFIED**) |
| Copilot (raw http) | none |
| Google geocoding | none |

### 1.6 Error body format

**KMP app, backend of 2026-10-30:** every error from the backend's handlers and security filters is
`{"error": <status>, "message": "...", "status": <status>, "code": "<CODE>", "requestId": "<uuid>"}`. `code` comes
from the backend's `shared/ErrorCode.kt` (`VALIDATION`, `MALFORMED_BODY`, `BAD_REQUEST`, `UNAUTHORIZED`,
`PAYMENT_REQUIRED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `RATE_LIMITED`, `SESSION_INVALID`, `SESSION_EXPIRED`,
`TOKEN_INVALID`, `ACCOUNT_DISABLED`, `INTERNAL`, `UPSTREAM`, `UNAVAILABLE`); validation errors add `errors: {field: reason}`.
A request without a usable token on a protected route is a 401 with `UNAUTHORIZED`, `TOKEN_INVALID` or
`SESSION_EXPIRED` (the app signs out on any 401). `ApiError` reads `code`, `ApiException.code` carries it, and
`sanitizeError` falls back on a known code's text when the English `message` isn't a known one. The rest of this section describes
the Flutter app's tolerant reading, which the KMP app keeps for older backends.

The backend error envelope was **UNVERIFIED** when this was written. The client tolerates all of the following shapes:

```json
{ "message": "Human readable text" }
{ "error": "Human readable text" }
{ "code": "RATE_LIMIT", "message": "..." }      // copilot
{ "detail": "..." }                             // copilot only
{ "reason": "..." }                             // /video/token only
"plain string body"                             // documents, tasks, video, sanitizer
```

How each area extracts the message:

| Area | Extraction order |
|---|---|
| Most repositories (bookings, chat, profile, documents) | `response.data['message']` → `['error']` → `DioException.message` → a hardcoded fallback. Documents and tasks also accept a raw String body; tasks ignores messages of 300 characters or more. |
| Auth and account deletion | `core/utils/auth_error_sanitizer.dart`, described below. |
| `/video/token` | `message` → `error` → `reason` → raw body → `Server error (<status>): <statusMessage>` |
| Copilot | `message` → `error` → `detail`. 429 bodies are read as `{code, message}`. |

`AuthErrorSanitizer.sanitize(statusCode, data, defaultMessage)` works as follows:
- It extracts `message` or `error`, or uses the body directly if it is a String.
- It unwraps a `"Login error: <inner>"` prefix.
- Known-safe backend strings pass through verbatim. These are the backend's exact strings, so the KMP client should match them:
  - `No account found`
  - `Invalid credentials`
  - `Invalid credentials - password mismatch`
  - `You need to create a patient account first. Use Create Account in the patient app and link your doctor account.`
  - `Email already registered`
  - `Phone already registered`
  - `Too many requests. Please try again later.`
  - `Too many verification requests. Please try again later.`
  - `SMS could not be sent. Please use email verification or contact support.`
  - `Invalid or expired email verification code`
  - `Invalid or expired verification code`
  - `Account is disabled`
  - `Account is locked`
  - `Your account has been blocked.`
  - a few similar strings
- It filters SQL keywords, stack traces, Java class names and raw JSON out of anything else.
- If nothing usable remains, it falls back by status code:

  | Status | Message |
  |---|---|
  | 401 | `Invalid credentials` |
  | 404 | `No account found` |
  | 429 | `Too many requests…` |
  | 403 or 5xx | the caller's `defaultMessage` |

Dio uses its default `validateStatus` (2xx only), so every non-2xx response arrives as a `DioException`. Repository checks like `if (statusCode != 200)` are therefore mostly unreachable.

### 1.7 Date and time formats

| Kind | Format | Examples |
|---|---|---|
| Instants (appointments, slots, messages, notifications, payments) | ISO-8601 UTC with `Z`, e.g. `2026-02-12T13:00:00Z`. The client strips an optional trailing zone id in brackets (`2026-02-12T13:00:00Z[UTC]`) before parsing, then converts to device local time (`core/utils/date_utils.dart` `parseAppointmentDateTime`). | `startAt`, `endAt`, `createdAt`, `readAt`, `lastMessageAt`, `nextAvailableStartAt` |
| Dates (no time) | `yyyy-MM-dd` | `birthDate`, document `date`, query `day`, `startDate`, `endDate`, task `startDate`/`endDate`, installment `dueDate` |
| Local times | `HH:mm` or `HH:mm:ss` | task `morningTime`/`afternoonTime`/`eveningTime`, check-in `scheduledTime` |
| Client-sent instants | `DateTime.toUtc().toIso8601String()`, which includes milliseconds: `2026-02-12T13:00:00.000Z` | copilot `preferredStartAt` |
| Booking `startAt` | Echoes the slot's `startAt` string exactly as the server returned it | `POST /patients/me/appointments` |
| Time zone | IANA name from `flutter_timezone`, sent as profile `timeZone` on start and on resume | `PATCH /patients/me/profile` |

Scheduling days are computed on the device's local calendar. `day=yyyy-MM-dd` is the local date, and whether the server interprets it in the patient's `timeZone` is **UNVERIFIED**.

### 1.8 Pagination

`GET /public/doctors` has its own paging:
- Query: `page` (1-based, omitted when it is 1) and `pageSize` (always sent, default 20).
- Response: `{doctors, totalCount, page?, pageSize?}`.
- The client also accepts a bare array for backwards compatibility.

Since the backend's Phase 6 (2026-10-30) the lists that grow with a patient's history have a `/page` sibling with the
same filters and order, returning Spring's `Page` shape, 0-based `page` and `size` (default 20, `sort` ignored):
`{content: [...], totalElements, totalPages, number, size, first, last, empty, pageable, sort}`. Siblings the patient app
may call: `GET /notifications/page` (N1a, used), `GET /patients/me/appointments/page` (B2's filters),
`GET /patients/me/documents/page`, `GET /public/doctors/{doctorId}/reviews/page`. The original list endpoints stay
unchanged and return full JSON arrays.

### 1.9 ID types

- **In responses**, IDs are parsed with `.toString()`, so the server may return numbers or strings. The client keeps them as `String`.
- **In requests**, IDs placed in JSON bodies are converted with `int.parse` and sent as **JSON numbers**: `doctorId`, `appointmentId`, `conversationId`, `recipientDoctorId`, `treatmentPlanId`, `installmentItemId`, `checkInId`, `serviceId`, `locationId`, `documentIds[]`, `rescheduleAppointmentId` and `allowedDoctorIds[]`. The backend therefore uses numeric (Long) IDs.
- **Exceptions**:
  - `challengeId` (account deletion) is a string.
  - `paymentId` is treated as an opaque string.
  - Notification IDs are read as `int`, but `PUT /notifications/{id}/read` is also called with string IDs (UUIDs) taken from FCM payloads.

### 1.10 Money

- Amounts are **integer minor units** with an ISO currency code: `amountMinor`, `minPriceMinor`, `paymentAmountMinor`, `totalMinor`, and similar.
- Display divides by 100. The UI's default currency is `UZS`.
- Price-filter bands in doctor search are `≤2000`, `2000–5000` and `≥5000` minor units.

### 1.11 Language codes

| Where | Values |
|---|---|
| App locale, profile `language` | `en`, `de`, `uz`, `ru` (lowercase). German has no counterpart in the AI endpoints. |
| AI visit summary `language` | Locale `languageCode`: `en`, `de`, `uz` or `ru` |
| Copilot `language` | `EN`, `RU` or `UZ` (uppercase). Derived from profile `language`; `de` and anything else map to `EN` (`shifa_ai_screen.dart:24-26`). |
| Copilot transcription `languageHint` | `en`, `ru` or `uz` (lowercase) |

### 1.12 Media URL normalization

`normalizePhotoUrl()` (`core/utils/image_utils.dart`) is applied to every `photoUrl`, `doctorPhotoUrl`, `participantPhotoUrl` and `patientPhotoUrl`:
- **Absolute URLs** are kept. On Android, hosts `localhost` and `127.0.0.1` are rewritten to `10.0.2.2:8090`.
- **Relative URLs** are prefixed with `publicBaseUrl`, which is `http://localhost:8090`. **This breaks relative photo URLs in production.** The backend must return absolute URLs.

Other URL fields are used as-is, without normalization: document `fileUrl`, chat attachment URLs and certificate URLs.

---

## 2. Endpoint index

Legend: Auth ✔ means a Bearer token is sent. Auth ✘ means no token. "public+tok" means the path is under `/public` but a token is sent anyway when stored; a 401 there never logs the user out.

### 2.1 Auth (10)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| A1 | POST | `/auth/login?app=patient` | ✘ | `features/auth/data/auth_repository.dart:53` | Password login (email or phone) |
| A2 | POST | `/auth/check-identifier` | ✘ | `auth_repository.dart:103` | Classify phone/email as doctor, patient or none |
| A3 | POST | `/auth/check-existing-doctor` | ✘ | `auth_repository.dart:140` | **DEAD**: no callers |
| A4 | POST | `/auth/send-email-otp` | ✘ | `auth_repository.dart:164`, `:484` | Send 6-digit email OTP (registration, doctor link, account info, account deletion) |
| A5 | POST | `/auth/send-sms-otp` | ✘ | `auth_repository.dart:194` | Send SMS OTP with optional server-side email fallback |
| A6 | POST | `/auth/create-patient-for-doctor` | ✘ | `auth_repository.dart:267` | Create a patient account linked to an existing doctor identity |
| A7 | POST | `/auth/register-patient` | ✘ | `auth_repository.dart:318` | Register a new patient |
| A8 | POST | `/auth/reset-password` | ✔ | `auth_repository.dart:361`, `:504` | Forced reset after login, and change password |
| A9 | POST | `/auth/send-forgot-password-otp` | ✘ | `auth_repository.dart:404` | Send forgot-password OTP |
| A10 | POST | `/auth/forgot-password-reset` | ✘ | `auth_repository.dart:380`, `:428`, `:458` | Reset password with OTP and log in |

### 2.2 Profile, account, device (7)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| P1 | GET | `/patients/me/profile` | ✔ | `features/profile/data/profile_repository.dart:15`, `auth_repository.dart:347` | Load profile, and validate the token at startup |
| P2 | PATCH | `/patients/me/profile` | ✔ | `profile_repository.dart:69` | Partial profile update (including language, timeZone, photoUrl, location) |
| P3 | POST | `/patients/me/photo` | ✔ | `features/profile/presentation/screens/profile_screen.dart:515` | Upload avatar (multipart) |
| P4 | POST | `/profile/photo` | ✔ | `profile_screen.dart:535` | Fallback avatar upload, tried only if P3 fails. Existence is **UNVERIFIED**. |
| P5 | POST | `/patients/me/delete-account/request` | ✔ | `features/profile/data/account_deletion_repository.dart:36` | Start account deletion and get a challenge |
| P6 | POST | `/patients/me/delete-account/confirm` | ✔ | `account_deletion_repository.dart:64` | Confirm deletion with email OTP |
| P7 | PUT | `/patients/me/fcm-token` | ✔ | `features/notifications/data/notifications_repository.dart:83` | Register the FCM token; an empty string clears it on logout |

### 2.3 Doctors, public directory (5)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| D1 | GET | `/public/doctors` | public+tok | `features/doctors/data/doctors_repository.dart:29` | Search and list doctors (filters and pagination) |
| D2 | GET | `/public/doctors/{doctorId}` | public+tok | `doctors_repository.dart:71` | Doctor profile |
| D3 | GET | `/public/doctors/{doctorId}/locations` | public+tok | `features/doctors/data/doctor_locations_repository.dart:77` | Practice locations, used for booking |
| D4 | GET | `/public/doctors/{doctorId}/reviews` | public+tok | `features/doctors/data/reviews_repository.dart:14` | Reviews list |
| D5 | POST | `/public/doctors/{doctorId}/reviews` | public+tok | `reviews_repository.dart:35` | Submit a review. Needs the patient token; its 401 never logs out. |

### 2.4 Scheduling, appointments, forms, treatment plans, AI summary (13)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| B1 | GET | `/patients/me/schedule/doctors/{doctorId}/available` | ✔ | `features/bookings/data/schedule_repository.dart:44` | Free slots for one day |
| B2 | GET | `/patients/me/appointments` | ✔ | `features/bookings/data/bookings_repository.dart:20` | List my appointments |
| B3 | POST | `/patients/me/appointments` | ✔ | `bookings_repository.dart:80` | Book or reschedule |
| B4 | GET | `/patients/me/appointments/{id}` | ✔ | `bookings_repository.dart:43`, `:114` | Appointment detail; the raw map is also used for the signing summary |
| B5 | DELETE | `/patients/me/appointments/{id}` | ✔ | `bookings_repository.dart:105` | Cancel |
| B6 | POST | `/patients/me/appointments/{id}/submit-signature` | ✔ | `bookings_repository.dart:124` | Submit the patient's signature PNG |
| B7 | GET | `/patients/me/appointments/{id}/review` | ✔ | `bookings_repository.dart:191` | My review for this appointment, if any |
| B8 | POST | `/patients/me/appointments/{id}/ai-visit-summary:generate` | ✔ | `bookings_repository.dart:205` | Trigger AI visit-summary generation |
| B9 | GET | `/patients/me/appointments/{id}/ai-visit-summary` | ✔ | `bookings_repository.dart:215` | Fetch the AI visit summary |
| B10 | POST | `/patients/me/appointments/{id}/ai-visit-summary/ask` | ✔ | `bookings_repository.dart:229` | Ask a question about the summary |
| B11 | GET | `/patients/me/forms/{formId}/for-signing` | ✔ | `bookings_repository.dart:136` | Patient form (visit record) to sign |
| B12 | POST | `/patients/me/forms/{formId}/submit-signature` | ✔ | `bookings_repository.dart:146` | Sign the patient form |
| B13 | GET | `/patients/me/treatment-plans/{planId}` | ✔ | `features/bookings/presentation/screens/treatment_plan_summary_screen.dart:37` | Treatment plan with lines and installments. Called directly from the UI with no repository. |

### 2.5 Payments (3)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| $1 | POST | `/payments/consultation/checkout` | ✔ | `bookings_repository.dart:159` | Create a hosted checkout for an appointment (Click or Stripe) |
| $2 | POST | `/payments/treatment-plan/checkout` | ✔ | `bookings_repository.dart:173` | Create a hosted checkout for a treatment plan or installment |
| $3 | GET | `/payments/{paymentId}` | ✔ | `bookings_repository.dart:184` | Poll payment status |

The client intercepts `…/api/payments/checkout/success` and `…/api/payments/checkout/cancel` inside the WebView but never requests them (§3.5).

### 2.6 Video (1)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| V1 | POST | `/video/token` | ✔ | `core/services/daily_video_service.dart:16` | Daily.co room URL and meeting token |

### 2.7 Chat (7)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| C1 | GET | `/patients/me/messages/conversations` | ✔ | `features/chat/data/chat_repository.dart:14` | Conversation list |
| C2 | GET | `/patients/me/messages/conversations/{conversationId}` | ✔ | `chat_repository.dart:31` | Conversation and messages. Polled every 5s while open. |
| C3 | POST | `/patients/me/messages/send` | ✔ | `chat_repository.dart:68` | Send text, image or voice |
| C4 | POST | `/patients/me/messages/conversations/{conversationId}/read` | ✔ | `chat_repository.dart:84` | Mark the conversation read |
| C5 | GET | `/patients/me/messages/search?q=` | ✔ | `chat_repository.dart:99` | Search doctors to start a chat |
| C6 | GET | `/patients/me/messages/unread-count` | ✔ | `chat_repository.dart:119` | Chat badge count. Polled every 10s. |
| C7 | POST | `/messages/upload-attachment` | ✔ | `features/chat/presentation/chat_conversation_screen.dart:201`, `:282` | Upload a chat image or voice note (multipart). **Note: this path is not under `/patients/me`.** |

### 2.8 Documents (3, plus 1 dynamic)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| F1 | GET | `/patients/me/documents` | ✔ | `features/documents/data/documents_repository.dart:38` | List documents |
| F2 | POST | `/patients/me/documents` | ✔ | `documents_repository.dart:78` | Upload a document (multipart) |
| F3 | DELETE | `/patients/me/documents/{documentId}` | ✔ | `documents_repository.dart:99` | Delete an own upload |
| F4 | GET | `{document.fileUrl}` (absolute) | ✔ | `documents_repository.dart:17` | Download bytes for the in-app viewer |

### 2.9 Remote-care tasks (4)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| T1 | GET | `/tasks/my-tasks` | ✔ | `features/tasks/data/tasks_repository.dart:19` | List my tasks |
| T2 | GET | `/tasks/my-tasks/{taskId}` | ✔ | `tasks_repository.dart:54` | Task detail |
| T3 | GET | `/tasks/my-tasks/{taskId}/check-ins` | ✔ | `tasks_repository.dart:66` | Scheduled check-ins |
| T4 | POST | `/tasks/my-tasks/{taskId}/check-in` | ✔ | `tasks_repository.dart:95` | Submit a check-in value |

### 2.10 Notifications and document access (6)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| N1 | GET | `/notifications` | ✔ | `notifications_repository.dart:12` | In-app notification list |
| N1a | GET | `/notifications/page` | ✔ | `NotificationsRepository.getNotificationsPage` | One page of N1 (§1.8), newest first; the inbox uses it |
| N2 | GET | `/notifications/unread/count` | ✔ | `notifications_repository.dart:27` | Bell badge count |
| N3 | PUT | `/notifications/{id}/read` | ✔ | `notifications_repository.dart:37`, `:46` | Mark one read; the id may be an int or a string |
| N4 | PUT | `/notifications/read-all` | ✔ | `notifications_repository.dart:55` | Mark all read |
| N5 | POST | `/document-access-requests/{requestId}/approve` | ✔ | `notifications_repository.dart:64` | Approve a doctor's access to a document |
| N6 | POST | `/document-access-requests/{requestId}/reject` | ✔ | `notifications_repository.dart:73` | Reject it |

### 2.11 Shifa AI copilot (7, raw http)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| X1 | POST | `/patients/me/copilot/stream` | ✔ (manual) | `features/copilot/data/copilot_api.dart:74` | Streaming LLM chat over SSE |
| X2 | POST | `/patients/me/copilot/transcribe` | ✔ (manual) | `copilot_api.dart:157` | Speech-to-text (multipart) |
| X3 | POST | `/patients/me/copilot/transcription-feedback` | ✔ (manual) | `copilot_api.dart:181` | Report a bad transcript (multipart) |
| X4 | POST | `/patients/me/copilot/book-appointment` | ✔ (manual) | `copilot_api.dart:210` | Book the nearest slot to a preferred time |
| X5 | POST | `/patients/me/copilot/resolve-booking` | ✔ (manual) | `copilot_api.dart:245` | LLM-extracted booking from the chat, as preview or confirm |
| X6 | POST | `/patients/me/copilot/suggest-doctors` | ✔ (manual) | `copilot_api.dart:262` | **DEAD**: its provider wrappers have no callers |
| X7 | POST | `/patients/me/copilot/suggest-doctors-chat` | ✔ (manual) | `copilot_api.dart:285` | Doctor suggestions from the chat history |

### 2.12 Config (1, raw http)

| # | Method | Path | Auth | Call site | Purpose |
|---|---|---|---|---|---|
| G1 | GET | `/public/config` | ✘ | `core/network/public_backend_config.dart:33`, `core/services/google_geocoding_service.dart:36` | Feature flags and the Maps key. Effectively never called in default builds (§3.12). |

**Total: 67 distinct backend method+path combinations** (Auth 10, Profile/account 7, Doctors 5, Bookings 13, Payments 3, Video 1, Chat 7, Documents 3, Tasks 4, Notifications 6, Copilot 7, Config 1). Two of them are dead: A3 and X6. The dynamic F4 download is not counted.

---

## 3. Endpoint details

### 3.1 Auth

#### A1 `POST /auth/login?app=patient`
- **Query**: `app=patient` (constant). The backend then requires the PATIENT role; a doctor-only account gets **403**.
- **Req**:
  ```json
  { "username": "<email | +998XXXXXXXXX>", "password": "<string>" }
  ```
  `username` is either an email or a phone number normalized by `normalizePhoneForSms` to `+` followed by digits.
- **Res 200**:
  ```json
  { "token": "<JWT>", "forcePasswordReset": false }
  ```
  - `token` is required. Its absence throws `No token received from server`.
  - `forcePasswordReset` defaults to false. When true, the router forces `/reset-password` (A8).
- **Errors**: go through the sanitizer (default `Login failed`).
  - 401 → `Invalid credentials`.
  - 403 → the doctor-needs-patient-account message, if the backend sends it.
- **Client behaviour**: failed attempts are counted locally by `LoginAttemptService`, which persists `login_failed_attempts` and `login_lockout_until_epoch_ms` in SharedPreferences. The lockout is client-side only.

#### A2 `POST /auth/check-identifier`
- **Caller**: `features/auth/presentation/screens/create_account_screen.dart:394`.
- **Req**: `{ "phone"?: "<trimmed>", "email"?: "<trimmed>" }`. At least one key is present.
- **Res 200**: `{ "type": "doctor" | "patient" | "none", "firstName"?: string, "lastName"?: string }`.
  - A missing `type` is treated as `none`.
  - A null body is treated as `none`.
- **404**: treated as `{type:"none"}`. Other errors are sanitized.
- **Flow meaning**:
  - `patient`: the account already exists.
  - `doctor`: the "confirm doctor to patient" flow runs (A4 then A6).
  - `none`: normal registration (A5/A4 then A7).

#### A3 `POST /auth/check-existing-doctor` (DEAD)
- **Req**: `{ phone?, email? }`.
- **Res**: `{ firstName, lastName }`. A 404, `{exists:false}` or `{found:false}` means not found.

#### A4 `POST /auth/send-email-otp`
- **Callers**:
  - `account_info_screen.dart:192`
  - `doctor_otp_verify_screen.dart:100`
  - `register_otp_verify_screen.dart:184`, `:214`
  - `confirm_doctor_to_patient_screen.dart:44`
  - `delete_account_otp_verify_screen.dart:77` (the deletion variant)
- **Req (standard)**: `{ "email": "<trimmed>" }`.
- **Req (account deletion)**: `{ "email": "<trimmed>", "purpose": "ACCOUNT_DELETION" }`. It is called automatically when the deletion OTP screen opens and again on resend.
- **Res**: the body is ignored. The standard variant requires HTTP 200.
- **Errors**: sanitized. Timeouts and connection errors map to fixed English messages.

#### A5 `POST /auth/send-sms-otp`
- **Callers**: `create_account_screen.dart:93`, `register_otp_verify_screen.dart:228`.
- **Req**:
  ```json
  { "phone": "+998XXXXXXXXX", "fallbackEmail"?: "<lowercased>", "purpose"?: "<never sent>" }
  ```
- **Res 200**: `{ "channel": "sms" | "email" }`.
  - `email` means the server fell back to email and sent the code to `fallbackEmail`.
  - A missing or other value means `sms`.
- **Special errors**:
  - **503** with `SMS could not be sent…` throws `SmsOtpUnavailableException`. The client then falls back to A4 itself.
  - **429**, or a body containing `Too many (verification) requests`, throws `OtpRateLimitException`.
- The OTP is **not verified by a separate call**. The code is sent later in A7 or A10.

#### A6 `POST /auth/create-patient-for-doctor`
- **Caller**: `doctor_otp_verify_screen.dart:72`.
- **Req**: `{ "phone"?: string, "email"?: string, "emailOtp"?: "<6 digits>" }`. `emailOtp` is required when `email` is present.
- **Res 200**: `{ "token": "<JWT>" }`. The token is stored and the user is logged in.

#### A7 `POST /auth/register-patient`
- **Caller**: `register_otp_verify_screen.dart:102`.
- **Req**:
  ```json
  {
    "firstName": "string", "lastName": "string", "password": "string",
    "phone"?: "<trimmed, not re-normalized>", "email"?: "string",
    "emailOtp"?: "string", "smsOtp"?: "string",
    "birthDate"?: "yyyy-MM-dd", "gender"?: "...", "address"?: "...", "language"?: "..."
  }
  ```
  - Exactly one of `emailOtp` or `smsOtp` accompanies the identifier that was verified.
  - `birthDate`, `gender`, `address` and `language` are supported by the method but **never passed** by the UI.
- **Password rules** (client-side, `password_validation.dart`): 8 to 128 characters, with at least one uppercase letter, one lowercase letter, one digit and one special character.
- **Res 200**: `{ "token": "<JWT>" }`. A missing token is a hard cast error, which surfaces as `Registration failed`.

#### A8 `POST /auth/reset-password` (authenticated)
- **Forced-reset variant** (`reset_password_screen.dart:61`): `{ "newPassword": "string" }`.
- **Change-password variant** (`change_password_screen.dart:40`): `{ "currentPassword": "string", "newPassword": "string" }`.
- **Res**: the body is ignored.
- **Errors**: a 401 here is under `/auth/`, so it **does not** auto-logout.

#### A9 `POST /auth/send-forgot-password-otp`
- **Callers**: `forgot_password_screen.dart:41` (first send) and `forgot_password_otp_screen.dart:108` (resend).
- **Req**: `{ "identifier": "<email or phone, trimmed>", "app": "patient" }`.
- **Res 200**: `{ "channel": "sms" | "email" }`, default `email`.
- **Quirk**: the client stores `email = identifier` when the channel is `email`, and `phone = identifier` when it is `sms`. If a phone identifier comes back with `channel: "email"`, the phone string ends up in the `email` field of A10.

#### A10 `POST /auth/forgot-password-reset`
- **Caller**: `forgot_password_new_password_screen.dart:73`, `:79`, `:85`.
- **SMS variant**: `{ "phone", "smsOtp", "app": "patient", "newPassword" }`.
- **Email variant**: `{ "email", "emailOtp", "app": "patient", "newPassword" }`.
- **Firebase variant** (`auth_repository.dart:380`): `{ "idToken", "newPassword" }`. **DEAD**, because nothing sets a Firebase idToken any more.
- **Res 200**: `{ "token": "<JWT>" }`. The token is stored and the user is logged in. `forcePasswordReset` is set to false.
- The OTP is checked only here. The OTP screen just collects the code.

### 3.2 Profile, account, device

#### P1 `GET /patients/me/profile`
- **Callers**: `profile_provider` loadProfile (home, profile), and `auth_provider.dart:60` for startup token validation.
- **Res 200**: a PatientProfile object (§5.1).
- On 401 the session is invalid. Other errors keep the session.

#### P2 `PATCH /patients/me/profile`
- **Callers**:
  - `edit_profile_screen.dart:152` (full form)
  - `profile_screen.dart:430` (language only)
  - `profile_screen.dart:572` (photoUrl after upload)
  - `profile_provider.dart:135` (timeZone sync)
- **Req**: only non-null keys are sent.
  ```json
  {
    "firstName"?, "lastName"?, "birthDate"?: "yyyy-MM-dd",
    "gender"?: "Male" | "Female" | "Other",
    "phone"?, "address"?, "language"?: "en|de|uz|ru",
    "photoUrl"?, "latitude"?: number, "longitude"?: number,
    "locationCountry"?, "locationRegion"?, "locationDistrict"?, "locationCity"?,
    "locationPostalCode"?, "locationStreetAddress"?,
    "timeZone"?: "Asia/Tashkent"
  }
  ```
- **Res 200**: the updated PatientProfile (§5.1).
- **Asymmetry**: the client sends `firstName` and `lastName`, but the response is parsed only for `fullName`.
- Location fields come from Google reverse geocoding (§4.1). `address` is a formatted string built on the client.
- **Bug**: the edit-profile screen can PATCH a local file path as `photoUrl`.

#### P3 `POST /patients/me/photo` and P4 `POST /profile/photo` (fallback)
- **Multipart**: field `file`, containing the picked image bytes with the picked file's name.
- **Res 200**: `{ "photoUrl": "…" }`, or a bare string URL.
- **Flow**:
  1. If P3 throws for any reason, the client retries the same form data at P4.
  2. If both fail, the client shows "endpoint not available".
  3. On success it PATCHes `photoUrl` (P2), then reloads the profile.
- Whether P4 exists is **UNVERIFIED**. The fallback looks like a legacy leftover.

#### P5 `POST /patients/me/delete-account/request`
- **Caller**: `profile_screen.dart:665`.
- **Req**: no body.
- **Res 200**: `{ "challengeId": "string (required)", "maskedEmail"?: "string", "expiresInSeconds"?: 300 }`. An empty `challengeId` throws `Invalid response from server`.
- **Next**: the client navigates to the OTP screen with `{challengeId, email}`, where `email` comes from the profile, not `maskedEmail`. That screen immediately calls A4 with `purpose: ACCOUNT_DELETION`.
- Whether P5 also sends an email by itself is **UNVERIFIED**.

#### P6 `POST /patients/me/delete-account/confirm`
- **Caller**: `delete_account_otp_verify_screen.dart:111`.
- **Req**: `{ "challengeId": "string", "email": "string", "emailOtp": "string" }`.
- **Res**: the body is ignored. The client then logs out locally.

#### P7 `PUT /patients/me/fcm-token`
- **Callers**: `app/app.dart:102`, `:110` (after authentication, and on token refresh); `auth_provider.dart:202` (logout, with `""`).
- **Req**: `{ "fcmToken": "<FCM registration token>" | "" }`.
- **Res**: ignored. Errors are only logged.
- Registration is skipped when Firebase failed to initialise.

### 3.3 Doctors (public directory)

#### D1 `GET /public/doctors`
- **Callers**:
  - `doctors_screen.dart:137`, `:143` (search UI)
  - `doctors_provider.dart:189` (filter catalog: page 1, 20 unfiltered doctors)
  - `doctors_provider.dart:265`
  - `create_booking_screen.dart:40`
  - home (`loadDoctors`)
- **Query**: from `DoctorsSearchFilters.toQueryParameters()` (`features/doctors/data/doctors_search_filters.dart:106`). Keys are sent only when set.

  | Key | Type | Values seen in the UI |
  |---|---|---|
  | `search` | string | free text, 350 ms debounce |
  | `profession` | string | English profession key taken from the doctors' `profession` values |
  | `clinic` | string | (never set by UI) |
  | `region` | string | canonical Uzbek region key |
  | `country` | string | `Germany`, `Uzbekistan` |
  | `consultationType` | string | `online`, `in_person`, `both` |
  | `availableWithin` | string | `today`, `tomorrow`, `week` |
  | `minRating` | number | e.g. `4.5` ("Top rated" chip) |
  | `minPriceMinor` / `maxPriceMinor` | int | bands `—/2000`, `2000/5000`, `5000/—` |
  | `verifiedOnly` | bool | only `true` is sent |
  | `latitude`, `longitude` | number | device location ("Near me" chip) |
  | `radiusKm` | number | `50` with "Near me" |
  | `sortBy` | string | `rating`, `distance`, `reviews`, `availability` |
  | `includeNextAvailable` | bool | `true` from the search screen, so the server should fill `nextAvailableStartAt` |
  | `page` | int | only sent when > 1 |
  | `pageSize` | int | always sent, `20` |

- **Res 200**:
  ```json
  { "doctors": [Doctor…], "totalCount": 123, "page"?: 1, "pageSize"?: 20 }
  ```
  A bare `[Doctor…]` array is also accepted, with `totalCount` set to the array length.
- **Enrichment**: if none of the first 5 doctors has `nextAvailableStartAt`, the client calls B1 day by day, 2 doctors at a time, for up to 14 days per doctor. Results are cached for 5 minutes (`doctor_availability_enricher.dart`). This is expensive, so the KMP client should rely on the server field.
- **Errors**: wrapped as `Failed to search doctors: …`.

#### D2 `GET /public/doctors/{doctorId}`
- **Callers**:
  - `doctor_profile_screen.dart:55`
  - `appointment_booking_flow_screen.dart:119`
  - `doctors_provider.dart:411` ("My doctors")
  - `doctors_provider.dart:445`
- **Res 200**: a Doctor object (§5.3).
- **"My doctors" tab**: B2 with no filters, then the distinct `doctorId` values, then **N sequential D2 calls**. Individual failures are skipped.

#### D3 `GET /public/doctors/{doctorId}/locations`
- **Caller**: `appointment_booking_flow_screen.dart:80`.
- **Res 200**: `[PublicDoctorLocation…]` (§5.4).
  - Entries with `id <= 0` are dropped as "synthesized legacy placeholders".
  - A non-array response, or any error, yields `[]` and never blocks booking.

#### D4 `GET /public/doctors/{doctorId}/reviews`
- **Caller**: `reviews_provider.dart:37`.
- **Res 200**: `[Review…]` (§5.5). A non-array response yields `[]`.

#### D5 `POST /public/doctors/{doctorId}/reviews`
- **Caller**: `submit_review_screen.dart:51`.
- **Req**:
  ```json
  { "rating": 1-5, "comment": "string" | null, "appointmentId"?: number }
  ```
  `comment` is always present and may be `null`.
- **Res 200**: a Review object. After success the client reloads D4 and invalidates B7.
- **Duplicates**: the client assumes the backend rejects duplicate reviews per appointment. The local `hasReviewForAppointment` always returns false.
- **Auth**: the endpoint is under `/public`, but the patient token is sent. The backend presumably requires it (**UNVERIFIED**). A 401 does not log the user out.

### 3.4 Scheduling and appointments

#### B1 `GET /patients/me/schedule/doctors/{doctorId}/available`
- **Callers**: `schedule_provider.dart:42` (booking flow), `doctor_availability_enricher.dart:104`.
- **Query**: `day=yyyy-MM-dd` (device-local date), and `locationId?=<int>` for the chosen in-person location.
- **Res 200**:
  ```json
  [ { "startAt": "ISO-UTC", "endAt": "ISO-UTC", "slotMinutes": 30,
      "locationId"?: 12, "locationLabel"?: "Main clinic" } ]
  ```
  - `startAt`, `endAt` and `slotMinutes` (int) are required.
  - A non-array response yields `[]`.
- **Client behaviour**: to find the next available day, the booking flow scans up to 30 days forward, one call per day. The enricher scans up to 14 days.

#### B2 `GET /patients/me/appointments`
- **Callers**:
  - `bookings_provider.dart:41`, with these variants:
    - home: `status=CONFIRMED` and `startDate=<today>` (`home_screen.dart:84`)
    - Upcoming tab: `startDate=<today>` only (`bookings_screen.dart:51`)
    - Past tab: no params (`bookings_screen.dart:57`)
  - `doctors_provider.dart:381` with no params
- **Query**: `status?` (e.g. `CONFIRMED`), `startDate?` (`yyyy-MM-dd`), `endDate?` (never sent).
- **Res 200**: `[Appointment…]` (§5.6). A non-array response yields `[]`.
- A code comment says new bookings may be `REQUESTED` or `PENDING`, which is why the Upcoming tab doesn't filter by status.

#### B3 `POST /patients/me/appointments`
- **Caller**: `appointment_booking_flow_screen.dart:~897`, via `bookings_provider.dart:~91`.
- **Req**:
  ```json
  {
    "doctorId": 42,
    "startAt": "<slot.startAt verbatim, ISO UTC>",
    "slotMinutes": 30,
    "reason"?: "free text (omitted if empty)",
    "isVideo": false,
    "serviceId"?: 7,
    "locationId"?: 3,
    "documentIds"?: [101, 102],
    "rescheduleAppointmentId"?: 55
  }
  ```

  | Key | When it is sent |
  |---|---|
  | `serviceId` | Video bookings only. It is the chosen `serviceItems[].id`. |
  | `locationId` | In-person bookings only: the slot's `locationId`, or else the selected location. |
  | `documentIds` | When documents are attached: up to 5 PDFs uploaded beforehand through F2 with `category=PRE_APPOINTMENT`. |
  | `rescheduleAppointmentId` | When rescheduling, from `/booking-flow/{doctorId}?rescheduleId=`. The same endpoint is used; there is no dedicated reschedule call. |

- **Res 200**: an Appointment object (§5.6).
  - If `paymentStatus` is `PENDING` (case-insensitive), the client immediately calls $1 and opens the checkout WebView.
  - Otherwise the booking is considered complete.
- **Errors**: `message` → `error`. The backend text `serviceId is required for video consultation` is special-cased in the UI.
- Before sending, the client blocks a local double booking at the same `startAt`.

#### B4 `GET /patients/me/appointments/{id}`
- **Callers**:
  - `appointment_details_screen.dart:42`, `:55`, `:71`, `:103`
  - `sign_appointment_screen.dart` (as a raw map)
- **Res 200**: an Appointment object (§5.6).
- **Signing summary**: the signing screen reads the raw map for `doctorName`, `startAt`, `endAt`, `location`, `reason` and `alreadySigned`.

#### B5 `DELETE /patients/me/appointments/{id}`
- **Caller**: `appointment_details_screen.dart:474`.
- **Res**: ignored. The client then reloads B2.
- This is a cancel. It is a hard DELETE, not a status PATCH.

#### B6 `POST /patients/me/appointments/{id}/submit-signature`
- **Caller**: `sign_appointment_screen.dart:167`.
- **Req**: `{ "signatureImageBase64": "<base64 PNG, no data: prefix>" }`. The PNG is captured at pixelRatio 2.
- **Res**: ignored.

#### B7 `GET /patients/me/appointments/{id}/review`
- **Caller**: `bookings_provider.dart:136` (`appointmentReviewProvider`).
- **Res 200**: `{ "rating": int, "comment"?: string, "createdAt"?: string }`.
- **Any error, including 404**, is treated as no review, and the "Write review" button is shown.

#### B8 `POST /patients/me/appointments/{id}/ai-visit-summary:generate`
- The path contains a literal colon (Google-style custom method).
- **Caller**: `visit_summary_screen.dart:29`, called on every open.
- **Req**: `{ "language"?: "en|de|uz|ru", "force": false }`. `force` is always false from the UI.
- **Res**: ignored. Errors are not caught, and the future is unawaited by the UI.

#### B9 `GET /patients/me/appointments/{id}/ai-visit-summary?language=<code>`
- **Res 200**:
  ```json
  {
    "status": "ready" | "not_ready" | "...",
    "language": "en", "version": 1, "generatedAt"?: "ISO",
    "content"?: {
      "summaryPlain": "string",
      "carePlan": ["string"], "nextSteps": ["string"],
      "medicationGuidance": [ { "name", "instructions", "missedDose" } ],
      "redFlags": [ { "sign", "urgency" } ],
      "disclaimer": "string"
    }
  }
  ```
  - Defaults when keys are missing: `status` `not_ready`, `language` `en`, `version` `1`.
  - Content is rendered only when `status == "ready"` and `content` is a map. Otherwise the UI shows a "preparing" message.
  - The client does not poll; the user refreshes with pull-to-refresh.
  - Other `status` values are **UNVERIFIED**.

#### B10 `POST /patients/me/appointments/{id}/ai-visit-summary/ask`
- **Req**: `{ "language"?: "en|…", "question": "string" }`.
- **Res 200**: `{ "answer": "string", "citations": ["string"] }`. `citations` is optional and may be a list of any values; each is stringified.

#### B11 `GET /patients/me/forms/{formId}/for-signing`
- **Caller**: `sign_appointment_screen.dart:62`. The route is `/bookings/sign-form/{patientFormId}`, reached from a `SIGNATURE_REQUESTED` push.
- **Res 200**: a raw map. The keys read are:
  - `date`
  - `complaints`
  - `diagnosis`
  - `doctorName`
  - `patientSignatureImageBase64` (non-empty means already signed)

  `complaints` and `diagnosis` are joined into the displayed "reason".

#### B12 `POST /patients/me/forms/{formId}/submit-signature`
- **Req**: `{ "signatureImageBase64": "<base64 PNG>" }`.
- **Res**: ignored.

#### B13 `GET /patients/me/treatment-plans/{planId}`
- **Caller**: `treatment_plan_summary_screen.dart:37`. Opened from `TREATMENT_PLAN_*` and `INSTALLMENT_*` notifications at `/bookings/treatment-plan/{planId}`.
- **Res 200** (keys read):
  ```json
  {
    "title", "diagnosis", "notes", "status", "planPaymentStatus",
    "currency": "UZS", "totalMinor", "paidMinor", "owedMinor",
    "linesTotalCount", "linesCompletedCount",
    "lines": [ { "title", "quantity", "discountMinor", "currency", "lineTotalMinor", "status" } ],
    "installmentPlans": [ {
      "status", "totalAmountMinor", "currency",
      "items": [ { "id", "sequenceNumber", "dueDate": "yyyy-MM-dd", "amountMinor", "currency",
                   "status": "PENDING" | "OVERDUE" | "PAID" | "..." } ]
    } ]
  }
  ```
  - The "Pay online" button appears only if `owedMinor > 0` and `currency == "UZS"`.
  - Items with status `PENDING` or `OVERDUE` get a per-installment "Pay" button, which calls $2 with `installmentItemId`.
- **Errors**: a non-200 status shows `treatmentPlanLoadFailedWithCode`.

### 3.5 Payments

The payment gateways are **Click.uz**, the primary one for UZS, and **Stripe**, the fallback. The app never talks to Click or Stripe directly. It loads the backend-provided `checkoutUrl` in a WebView.

#### $1 `POST /payments/consultation/checkout`
- **Callers**:
  - `appointment_booking_flow_screen.dart:915` (right after B3)
  - `consultation_payment_screen.dart:~46` (route `/bookings/{id}/pay`, also used by payment-reminder pushes)
  - `bookings_screen.dart:66`
  - Stripe retries at `appointment_booking_flow_screen.dart:1113` and `bookings_screen.dart:336`
- **Req**: `{ "appointmentId": 42, "gateway"?: "STRIPE" }`. With no `gateway`, the server picks one (Click for UZS, **UNVERIFIED**).
- **Res 200**: `{ "paymentId": "…", "checkoutUrl": "https://…", "gateway"?: "CLICK" | "STRIPE" }`. A missing `paymentId` or `checkoutUrl` produces a "payment could not start" error.

#### $2 `POST /payments/treatment-plan/checkout`
- **Caller**: `treatment_plan_payment_screen.dart:50`.
- **Req**: `{ "treatmentPlanId": 9, "installmentItemId"?: 31 }`. Without `installmentItemId`, the client assumes the whole owed balance is charged (**UNVERIFIED**).
- **Res 200**: `{ "paymentId", "checkoutUrl" }`. There is no gateway fallback in this flow.

#### $3 `GET /payments/{paymentId}`
- **Res 200**: `{ "status": "PAID" | "PENDING" | "..." }`. The value is compared case-insensitively against `PAID` only.
- Polling errors are ignored.

#### Checkout WebView protocol (shared by all payment screens)
1. Load `checkoutUrl`. JavaScript is unrestricted.
2. Poll $3 **every 3 seconds**. When `status` is `PAID`, show "payment completed" and navigate to `/bookings/{appointmentId}`, or back to the plan.
3. Intercept navigation:
   - A URL containing `/api/payments/checkout/success` is prevented, and the client checks $3 immediately.
   - A URL containing `/api/payments/checkout/cancel` is prevented, and the screen closes (pops).
4. **Click-to-Stripe fallback**, consultation only. If `gateway == "CLICK"` and a **main-frame** WebView load error occurs:
   1. Call $1 again once with `gateway: "STRIPE"`.
   2. Switch polling to the new `paymentId`.
   3. Load the new `checkoutUrl`.

### 3.6 Video (Daily.co)

#### V1 `POST /video/token`
- **Callers**: `video_call_screen_mobile.dart:138`, `video_call_screen_web.dart:69`.
- **Req**: `{ "appointmentId": 42, "roomName"?: "string" }`. `roomName` is never passed by the UI.
- **Res 200**: `{ "token": "<Daily meeting token>", "roomUrl": "https://<sub>.daily.co/<room>", "roomName": "string" }`. The client requires status 200.
- **Joining the call**:
  - Mobile: `daily_flutter` `CallClient.join(url: roomUrl, token: token)`.
  - Web: an iframe at `${roomUrl}?t=${token}`.
- **Join window**: 5 minutes before `startAt` until 15 minutes after `endAt` (UTC). The client enforces this before offering the button; whether the server does too is **UNVERIFIED**.
- **Errors**: `message` → `error` → `reason` → raw body.

### 3.7 Chat

#### C1 `GET /patients/me/messages/conversations`
- **Callers**: `chat_providers.dart:7`, `chat_screen.dart:39`.
- **Res 200**: `[ChatContact…]` (§5.8).

#### C2 `GET /patients/me/messages/conversations/{conversationId}`
- **Caller**: `chat_providers.dart:29` (`conversationProvider`), re-fetched **every 5s** by `chat_conversation_screen.dart:60`.
- **Res 200**: `{ "conversation": ChatContact, "messages": [ChatMessage…] }`. A missing `messages` key yields `[]`.
- A new conversation has a client-side id of the form `new_…` until the first message is sent.

#### C3 `POST /patients/me/messages/send`
- **Callers**: `chat_conversation_screen.dart:~95` (text and image), `:~303` (voice).
- **Req**:
  ```json
  {
    "conversationId"?: 7,          // existing conversation
    "recipientDoctorId"?: 42,      // new conversation (instead of conversationId)
    "text"?: "string",
    "type"?: "image" | "voice",    // omitted for plain text
    "attachmentUrl"?: "<url from C7>",
    "attachmentName"?: "chat_<ms>.jpg | voice_<ms>.m4a",
    "thumbnailUrl"?: "string",     // never sent by current UI
    "fileSize"?: 12345,            // voice
    "duration"?: 12                // voice, seconds
  }
  ```
- **Res 200**: a ChatMessage (§5.9, either format).
- Document messages (`type: "document"`) are only received, never sent.

#### C4 `POST /patients/me/messages/conversations/{conversationId}/read`
- **Caller**: `chat_conversation_screen.dart:84`, when a conversation opens.
- **Req**: no body. **Res**: ignored.

#### C5 `GET /patients/me/messages/search?q=<text>`
- **Caller**: `chat_screen.dart:135` via `userSearchProvider`. It is skipped when the query is blank.
- **Res 200**: `[ { "id", "name", "photoUrl"?, "isDoctor": true, "email"?, "phone"? } ]`.

#### C6 `GET /patients/me/messages/unread-count`
- **Caller**: `chat_providers.dart:46`, a `StreamProvider` loop that runs **every 10s** for the lifetime of the app while the chat icon is mounted.
- **Res 200**: `{ "count": int }`. Any error yields `0`.

#### C7 `POST /messages/upload-attachment`
- **Callers**: `chat_conversation_screen.dart:201` (image), `:282` (voice).
- **Multipart**: field `file`.
  - Images: JPEG named `chat_<epochMs>.jpg`, compressed first.
  - Voice: AAC/M4A named `voice_<epochMs>.m4a`.
- **Res 2xx**: `{ "url": "<absolute URL>" }`. A missing or empty `url` throws `No URL in upload response`.
- A code comment explains why this endpoint exists: uploading through `/patients/me/documents` with the same title overwrote files.
- The dead `FileUploadService` (`features/chat/services/file_upload_service.dart`, `file_upload_mobile.dart`) calls the same endpoint through raw http with an extra multipart field `thumbnail` (`thumb_<name>`).

### 3.8 Documents

#### F1 `GET /patients/me/documents`
- **Caller**: `documents_provider.dart:37`.
- **Res 200**: `[Document…]` (§5.7). The body must be an array; anything else becomes a cast error, surfaced as a load failure.

#### F2 `POST /patients/me/documents` (multipart)
- **Callers**:
  - `documents_screen.dart:350`, `:415` (camera or gallery image, `${title}.jpg`)
  - `documents_screen.dart:488` (PDF via file picker, original filename)
  - `appointment_booking_flow_screen.dart:790` (pre-appointment PDFs)
- **Multipart fields**:

  | Field | Value |
  |---|---|
  | `file` | the bytes, with a filename |
  | `title` | string |
  | `date`? | `yyyy-MM-dd` |
  | `isChatAttachment` | the string `"false"` |
  | `category`? | see below |

- **Categories sent by the UI**: `BLOOD_TEST`, `URINE_TEST`, `STOOL_TEST`, `LAB_RESULT`, `MRI`, `CT_SCAN`, `XRAY`, `ULTRASOUND`, `MAMMOGRAPHY`, `ECG`, `EEG`, `ENDOSCOPY`, `BIOPSY`, `PATHOLOGY`, `IMAGING_OTHER`, `PRESCRIPTION`, `VACCINATION_RECORD`, `DISCHARGE_SUMMARY`, `REFERRAL`, `HOSPITAL_REPORT`, `ALLERGY_REPORT`, `OTHER_MEDICAL`. The booking flow uses `PRE_APPOINTMENT`.
- **Res 200**: a Document object. The booking flow needs `id` to be int-parseable so it can go into `documentIds`.
- **Errors**: `message` → `error` → raw String body → a fallback.

#### F3 `DELETE /patients/me/documents/{documentId}`
- **Callers**: `documents_screen.dart:243`, `appointment_booking_flow_screen.dart:739` (removing a pre-attached doc).
- The backend allows deleting only patient-uploaded documents. The client hides the delete button when `creatorLabel != Patient` (**UNVERIFIED**).

#### F4 `GET {fileUrl}` (absolute)
- **Caller**: `document_viewer_screen.dart:128`.
- **Request**: Dio with the Bearer token and `responseType: bytes`. The viewer sniffs the bytes to render a PDF (pdfx) or an image.
- A 401 on this call **can trigger logout**, because an absolute URL does not start with `/public/`.

### 3.9 Remote-care tasks

#### T1 `GET /tasks/my-tasks[?status=ACTIVE|COMPLETED|…]`
- **Caller**: `tasks_provider.dart:46`.
- **Query**: `status?`, the uppercased `TaskStatus` name: `DRAFT`, `ACTIVE`, `COMPLETED`, `EXPIRED` or `CANCELLED`.
- **Res 200**: `[RemoteCareTask…]`, **or** `{ "data": [RemoteCareTask…] }`. Items that fail to parse are skipped.

#### T2 `GET /tasks/my-tasks/{taskId}`
- **Caller**: `tasks_provider.dart:72`.
- **Res 200**: a RemoteCareTask (§5.11).

#### T3 `GET /tasks/my-tasks/{taskId}/check-ins`
- **Caller**: `tasks_provider.dart:82`.
- **Res 200**: `[TaskCheckIn…]`. It must be a bare array.

#### T4 `POST /tasks/my-tasks/{taskId}/check-in`
- **Caller**: `task_check_in_screen.dart:76`.
- **Req**:
  ```json
  { "checkInId": 55, "numericValue"?: 36.6, "textValue"?: "…", "booleanValue"?: true, "notes"?: "…" }
  ```
  Exactly one value key is sent, matching the task's `inputType`.
- **Res**: ignored.
- **Errors**: a short `message` (under 300 characters) or a String body is shown to the user.
- The client allows a check-in only if its scheduled time is at most 10 minutes in the future. Server enforcement is **UNVERIFIED**.

### 3.10 Notifications

#### N1 `GET /notifications`
- **Callers**: `notifications_provider.dart:57` (screen). `NotificationPollingService` would poll this every 30s, but **`startPolling` is never called**. Push (FCM) is the only real-time channel.
- **Res 200**: `[Notification…]` (§5.10). A non-array response yields `[]`.

#### N1a `GET /notifications/page`
- **Caller**: `NotificationsViewModel.load` (page 0) and `loadMore` (the next page at the end of the list).
- **Query**: `page` (0-based), `size` (20).
- **Res 200**: Spring `Page` (§1.8) of §5.10 items under `content`, newest first; `last` ends the loading. A bare array
  is read as the only page.
- **Older backend**: production (the 2026-10-20 build) has no `/page` and answers 500 `NoResourceFoundException` (a
  newer backend without it would answer 404). Page 0 then falls back to N1 as the only page; later pages are never
  asked for.

#### N2 `GET /notifications/unread/count`
- **Caller**: `notifications_provider.dart:62`, a FutureProvider that is re-fetched when invalidated after read actions. It is not polled.
- **Res 200**: `{ "count": int }`.

#### N3 `PUT /notifications/{id}/read`
- **Callers**: `notifications_screen.dart:251`, `notification_tap_handler.dart:188-197`, `:291`, and after N5/N6.
- The id is an int from N1, or a string (possibly a UUID) from the FCM `notificationId`/`id` field. The string form is sent verbatim in the path.
- **Req**: no body. **Res**: ignored.

#### N4 `PUT /notifications/read-all`
- **Caller**: `notifications_screen.dart:80`.
- **Req**: no body.

#### N5 and N6 `POST /document-access-requests/{requestId}/approve|reject`
- **Callers**: `notifications_screen.dart:481` (approve) and `:450` (reject), on `DOCUMENT_ACCESS_REQUEST` notifications.
- **Req**: `{}` (an empty JSON object).
- **Res**: ignored. After success the client calls N3 for that notification and hides the buttons locally.
- `requestId` comes from the notification's `documentAccessRequestId`.

### 3.11 Shifa AI copilot (raw `package:http`)

All copilot calls go to `${ApiClient.apiBaseUrl}/patients/me/copilot/...`. The Bearer token is read from `StorageService` and attached manually.

These calls have **no timeouts and no 401 auto-logout**. A 429 becomes `CopilotStreamException(code, message)`, with code defaulting to `RATE_LIMIT`, and the UI shows a daily-limit message. The route is `/shifa-ai` (`app/router.dart:568`).

#### X1 `POST /patients/me/copilot/stream` (SSE)
- **Caller**: `copilot_chat_provider.dart:227`.
- **Headers**: `Authorization`, `Content-Type: application/json`, `Accept: text/event-stream`.
- **Req**:
  ```json
  { "messages": [ { "role": "user" | "assistant", "content": "string" } ], "language": "EN" | "RU" | "UZ" }
  ```
  The full chat history is sent on every turn.
- **Res 200**: `text/event-stream`. The client's parser works as follows:
  - Lines are split on `\n`, and a trailing `\r` is stripped.
  - `event:<name>` sets the event name. `data:<text>` lines accumulate; the text after `data:` is kept **without trimming**, so a leading space is part of the token.
  - A blank line dispatches the event.
  - An `event: error` event has data `{ "code", "message" }` and throws `CopilotStreamException`.
  - Any other event with non-empty data is a **token**. Tokens are concatenated into the assistant reply.
  - There is no `[DONE]` sentinel; end of stream means the reply is done.
- **Non-200**: the body is read as `{code, message}` and thrown as `CopilotStreamException`. The code defaults to `UNKNOWN`.
- After each reply the UI calls X7, then X5 (preview) (`shifa_ai_screen.dart:57-59`).

#### X2 `POST /patients/me/copilot/transcribe` (multipart)
- **Caller**: `shifa_ai_screen.dart:272`.
- **Fields**: `file` (a recorded audio file path), `languageHint`? (`en`, `ru` or `uz`).
- **Res 200**: `{ "text": "transcript" }`. A comment says the backend uses Whisper (**UNVERIFIED**).

#### X3 `POST /patients/me/copilot/transcription-feedback` (multipart)
- **Caller**: `shifa_ai_screen.dart:294`. It is shown only when G1 returns `transcriptionFeedbackEnabled`, which in default builds is never (§3.12).
- **Fields**: `transcript`, `locale` (`en`, `ru` or `uz`), and `file`? (the original audio, skipped for web `blob:` paths).
- **Res**: only the 200 status is checked.

#### X4 `POST /patients/me/copilot/book-appointment`
- **Caller**: `shifa_ai_screen.dart:466`, from the "auto-book" bottom sheet on a suggested doctor.
- **Req**:
  ```json
  { "doctorId": 42, "preferredStartAt": "2026-02-12T13:00:00.000Z", "isVideo": false,
    "reason": "<localized 'Booked via Shifa AI'>", "consentConfirmed": true }
  ```
- **Res 200**: a map, which the client ignores. It shows a success message and refreshes bookings.
- **Errors**: `message` → `error` → `detail`.
- The server picks the nearest available slot. Payment handling for paid doctors is **UNVERIFIED**, because the client does not check `paymentStatus` here.

#### X5 `POST /patients/me/copilot/resolve-booking`
- **Callers**: `copilot_chat_provider.dart:338` (preview, `confirmAutoBook:false`), `:298` (confirm, `confirmAutoBook:true`).
- **Req**:
  ```json
  { "messages": [ {role, content} ], "language": "EN|RU|UZ",
    "confirmAutoBook": false, "allowedDoctorIds"?: [42, 43] }
  ```
  `allowedDoctorIds` holds the ids from the last X7 result. It is omitted if no suggestion has run yet. If the last suggestion returned no doctors, the preview is not requested at all.
- **Res 200** (keys read):

  | Key | Meaning |
  |---|---|
  | `booked`: bool | a booking was made |
  | `needsClientConfirmation`: bool | the UI should confirm first |
  | `previewMessage`: string | shown in a confirmation dialog when confirmation is needed |
  | `followUpMessage`: string | appended as an assistant bubble after booking |
  | `reasonCode`: string | why booking did not happen |
  | `message`: string | shown as an error |

  Known `reasonCode` values: `DOCTOR_MISSING`, `DOCTOR_NOT_IN_SUGGESTED_LIST`, `DOCTOR_NOT_FOUND`, `NO_SUGGESTED_DOCTORS`, `TIME_MISSING`, `INVALID_PREFERRED_TIME`, `PREFERRED_TIME_IN_PAST`, `VISIT_TYPE_MISSING`. For these the client appends a canned clarifying question in EN, RU or UZ.

#### X6 `POST /patients/me/copilot/suggest-doctors` (DEAD)
- **Req**: `{ "symptomsText": "string" }`. **Res**: `[Doctor…]`.
- The wrappers `fetchSuggestedDoctors` and `autoSuggestDoctorsFromChatText` have no callers.

#### X7 `POST /patients/me/copilot/suggest-doctors-chat`
- **Callers**: `copilot_chat_provider.dart:395`, via `shifa_ai_screen.dart:58`, `:161`.
- **Req**: `{ "messages": [ {role, content} ], "language": "EN|RU|UZ" }`.
- **Res 200**:
  ```json
  { "needsMoreInfo": false, "clarifyingQuestion"?: "string",
    "specialties": ["string"], "doctors": [Doctor…],
    "confidenceScore"?: 0.82, "uncertaintyMessage"?: "string",
    "diagnostics"?: { } }
  ```
  - When `needsMoreInfo` is true, the doctor list is cleared and `clarifyingQuestion` is appended as an assistant message.
  - A non-empty `uncertaintyMessage` is also appended.
  - `diagnostics` is opaque.
  - Doctors may carry `recommendationReason` and `triggeredBySymptoms`.

### 3.12 Config

#### G1 `GET /public/config`
- **Request**: raw http, no auth, 5s timeout. The result is cached for the life of the process (`public_backend_config.dart`).
- **Res 200** (keys read):
  - `transcriptionFeedbackEnabled`: bool, or the string `"true"`
  - `googleMapsApiKey`: string, read by the geocoding service
- **Quirk**: the URL is built from `AppConfig.apiBaseUrl`, which is the **raw `API_BASE_URL` define**, as `${define}/api/public/config`.
  - When the define is empty (the default and production case), the call is skipped. `{}` is used, and neither feature works.
  - When the define already ends in `/api`, the URL becomes `…/api/api/public/config`.

---

## 4. Third-party calls

### 4.1 Google Geocoding API (reverse geocoding for the profile location)
- **Service**: `core/services/google_geocoding_service.dart`, called from `profile/presentation/location_picker_widget.dart:166`.
- **URLs**:
  - Reverse: `GET https://maps.googleapis.com/maps/api/geocode/json?latlng={lat},{lng}&key=<REDACTED>&language=en`
  - Forward: `GET https://maps.googleapis.com/maps/api/geocode/json?address={urlencoded}&key=<REDACTED>&language=en`. The `geocode()` method exists but has no UI caller.
- **Key sources**: first the `--dart-define=GOOGLE_MAPS_API_KEY`, then `googleMapsApiKey` from G1. In default builds both are empty, so geocoding returns null and the structured location fields stay blank. This is **UNVERIFIED** for the store build pipeline, which may pass the define.
- **Response mapping**: from `results[0].address_components`:

  | Google component | Client field |
  |---|---|
  | `country` | `country` |
  | `administrative_area_level_1` | `region` (viloyat) |
  | `sublocality` | `district` (tuman) |
  | `locality` | `city` |
  | `postal_code` | `postalCode` |
  | `street_number` and `route` | `streetAddress` |

  These map to P2's `locationCountry`, `locationRegion`, `locationDistrict`, `locationCity`, `locationPostalCode` and `locationStreetAddress`.
- **Security**: the full request URL, including the key, is `debugPrint`ed.

### 4.2 Map tiles (OpenStreetMap)
- `flutter_map` `TileLayer` with `urlTemplate: https://tile.openstreetmap.org/{z}/{x}/{y}.png` and `userAgentPackageName: com.shifa.patient` (`location_picker_widget.dart:555`).
- No API key is used. OSM's tile usage policy applies.

### 4.3 External deep links (no HTTP made by the app)
- **Doctor address** (`doctor_profile_screen.dart:711-728`):
  - Android: `geo:{lat},{lng}?q={address}`, or `geo:0,0?q={address}`
  - iOS: `http://maps.apple.com/?ll=…` or `?q=…`
  - Fallback: `https://www.google.com/maps/search/?api=1&query=…`
- **Contact**:
  - `tel:` and `mailto:` (doctor profile `:845`/`:868`, appointment details `:407`/`:426`)
  - `https://t.me/{telegram}` (`:894`)
  - `https://instagram.com/{instagram}` (`:924`)
- **Placeholder image**: `https://via.placeholder.com/60` (`confirm_booking_screen.dart:45`).

### 4.4 Certificate viewer
- `GET {certificateUrl}` via raw http, **without an auth header**, with a 45s timeout (`doctor_profile_screen` → `certificate_viewer_screen.dart:128`).
- The bytes are sniffed and shown as a PDF or image. If they are neither, the URL is loaded in a WebView.
- Certificate URLs come from the Doctor `certificates[]` strings and must be publicly readable.

### 4.5 Daily.co
- There are no direct REST calls. The room URL and token come from V1.
- Mobile uses the `daily_flutter` SDK. Web uses an iframe with `?t=<token>`.

### 4.6 Click.uz and Stripe
- The app makes no direct API calls. It only loads backend-issued hosted `checkoutUrl`s in a WebView (§3.5).
- Merchant credentials live on the server.

### 4.7 LLM providers
- None are called directly. All AI features go through the backend: X1-X7 and B8-B10.

### 4.8 Firebase
- **Config files**: `lib/firebase_options.dart`, `android/app/google-services.json` and `ios/Runner/GoogleService-Info.plist`.
  - Project id: `shifa-doctor-staging`.
  - API keys and app ids: `<REDACTED>`.
  - The production app points at the Firebase project named **staging**.
- **FCM** (`core/services/push_notification_service.dart`):
  1. Calls `requestPermission`, then `getToken`. The token is uploaded with P7.
  2. `onTokenRefresh` re-uploads the token.
  3. `onMessage` (foreground) shows a local notification, localized from `data.type`.
  4. `onMessageOpenedApp` and `getInitialMessage` route through the tap handler.
- **Firebase Auth** (phone OTP): `phone_auth_repository.dart` exists but is **unused**. OTPs are backend-issued (A4, A5).
- **Crashlytics**: SDK only; no custom HTTP.

### 4.9 FCM data payload contract (server to device)

The keys read from `RemoteMessage.data` (`notification_tap_handler.dart:25-140`, `NotificationModel.fromFcmData`) are:

| Key | Notes |
|---|---|
| `type` | Uppercased, with `-` replaced by `_`. See the list below. |
| `notificationId` or `id` | Used for N3 on tap and for de-duplicating tray entries. An int or a string. |
| `entityId` | Generic target id. It is the fallback for the specific ids below. |
| `appointmentId` / `appointment_id` | |
| `documentId` / `document_id` | |
| `chatId` / `chat_id` | |
| `taskId` / `task_id` | |
| `patientFormId` / `patient_form_id` | Used for signing. |
| `treatmentPlanId` / `treatment_plan_id` | |
| `title`, `message`, `createdAt` / `created_at`, `doctorName` / `doctor_name` | Used to build a local NotificationModel. |
| `documentAccessRequestId` / `document_access_request_id` | |
| `route` | Optional explicit deep-link path, used as a last resort. |

`notification.title` and `notification.body` are only fallbacks. The client re-localizes the text from `type` into the app language.

`type` values handled: `APPOINTMENT_SCHEDULED`, `APPOINTMENT_CONFIRMED`, `APPOINTMENT_CANCELLED`, `APPOINTMENT_CHANGED`, `APPOINTMENT_REMINDER`, `APPOINTMENT_UPCOMING`, `APPOINTMENT_SOON`, `APPOINTMENT_CLOSE`, `APPOINTMENT_ASSIGNED`, `NEW_APPOINTMENT`, `APPOINTMENT_BOOKED_BY_PATIENT`, `APPOINTMENT_CANCELLED_BY_PATIENT`, `APPOINTMENT_RESCHEDULED_BY_PATIENT`, `SIGNATURE_REQUESTED`, `AI_VISIT_SUMMARY_READY`, `AI_SCRIBE_READY`, `CONSULTATION_PAYMENT_REMINDER`, `CONSULTATION_PAYMENT_DUE_24H`, `CONSULTATION_PAYMENT_DUE_6H`, `CONSULTATION_PAYMENT_DUE_1H`, `TREATMENT_PLAN_PAYMENT_REMINDER`, `TREATMENT_PLAN_UPDATED`, `INSTALLMENT_DUE_SOON`, `INSTALLMENT_DUE_TODAY`, `INSTALLMENT_OVERDUE`, `INSTALLMENT_SCHEDULE_CREATED`, `PROPHYLAXIS_REMINDER`, `NEW_DOCUMENT`, `DOCUMENT`, `DOCUMENT_ACCESS_REQUEST`, `DOCUMENT_ACCESS_APPROVED`, `DOCUMENT_ACCESS_REJECTED`, `NEW_MESSAGE`, `CHAT_MESSAGE`, `TASK_ASSIGNED`, `TASK_REMINDER`, `TASK_CANCELLED`, `TASK_COMPLETED`.

Routing summary:

| Types | Target |
|---|---|
| Appointment types | `/bookings/{id}` |
| `AI_VISIT_SUMMARY_READY` | `/bookings/{id}/visit-summary` |
| `SIGNATURE_REQUESTED` | `/bookings/sign-form/{patientFormId}`, else `/bookings/{id}` |
| Consultation payment types | `/bookings/{id}/pay` |
| Treatment plan and installment types | `/bookings/treatment-plan/{treatmentPlanId}` |
| Document types | `/documents/{id}` |
| Chat types | `/chat` |
| Task types | `/tasks/{taskId}/check-in`, else `/tasks` |

---

## 5. Reconstructed schemas

These schemas are reconstructed from the client's `fromJson` code. "req" means the client throws if the key is missing or has the wrong type. Everything else is optional or defaulted. Types are JSON types.

### 5.1 PatientProfile (`core/models/patient_profile_model.dart`), from P1 and P2
```jsonc
{
  "id"?: number|string,
  "fullName": string,            // default ""  (NO firstName/lastName read)
  "phone"?: string, "email"?: string, "address"?: string,
  "birthDate"?: "yyyy-MM-dd", "gender"?: "Male|Female|Other",
  "language"?: "en|de|uz|ru",
  "photoUrl"?: string,           // normalizePhotoUrl
  "latitude"?: number, "longitude"?: number,
  "locationCountry"?, "locationRegion"?, "locationDistrict"?, "locationCity"?,
  "locationPostalCode"?, "locationStreetAddress"?: string,
  "timeZone"?: "IANA id",
  "subscriptionTier"?: "PRO" | "PREMIUM" | null,
  "features"?: [string]          // feature flags; default []
}
```

### 5.2 UserModel (`core/models/user_model.dart`)
This model is **never parsed from any API response** and appears to be a dead model. Its keys are id, email, phone, firstName (or a split of fullName), lastName, fullName, photoUrl, birthDate, gender, address, language and role (default `PATIENT`).

### 5.3 Doctor (`core/models/doctor_model.dart`), from D1, D2, X6 and X7
```jsonc
{
  "id": number|string,                       // toString, default ""
  "firstName": string, "lastName": string,   // default ""
  "fullName"?: string,                       // default "first last"
  "profession"?: string,                     // English key, localized client-side from ProfessionData catalog
  "clinic"?: string, "address"?: string,
  "locationStreetAddress" | "street"?: string,
  "locationCity" | "city"?: string,
  "locationRegion" | "region"?: string,
  "locationCountry"?: string,
  "latitude"?: number, "longitude"?: number, "distanceKm"?: number,
  "phone"?: string, "email"?: string, "photoUrl"?: string,
  "averageRating"?: number, "reviewCount"?: int,
  "specializations"?: [string], "services"?: [string],
  "serviceItems"?: [ { "id", "title", "description"?, "isFreeConsultation": bool,
                       "prices": [ { "amountMinor": int, "currency": string } ] } ],
  "certificates"?: [string /* URL */],
  "furtherInformation"?: string, "biography"?: string,
  "telegram"?: string, "instagram"?: string,
  "nextAvailableStartAt"?: "ISO-UTC",
  "recommendationReason"?: string, "triggeredBySymptoms"?: [string],   // copilot
  "supportsOnline": bool, "supportsInPerson": bool,                    // == true
  "minPriceMinor"?: int, "minPriceCurrency"?: string,
  "onlineMinPriceMinor"?: int, "clinicMinPriceMinor"?: int
}
```

### 5.4 PublicDoctorLocation (`features/doctors/data/doctor_locations_repository.dart`), from D3
```jsonc
{ "id": number /*req, >0 kept*/, "label": string, "clinic"?, "address"?,
  "latitude"?: number, "longitude"?: number,
  "locationCountry"?, "locationRegion"?, "locationCity"?, "locationStreetAddress"?,
  "isPrimary": bool }
```

### 5.5 Review (`core/models/review_model.dart`), from D4 and D5
```jsonc
{ "id": any, "patientName": string, "patientPhotoUrl"?: string,
  "rating": int /*req*/, "comment"?: string, "createdAt": string /*req*/ }
```
B7 returns a smaller shape: `{ rating: int (req), comment?, createdAt? }`.

### 5.6 Appointment (`core/models/appointment_model.dart`), from B2, B3 and B4
```jsonc
{
  "id": any, "doctorId": any,
  "doctorName"?: string,          // split on first space into first/last
  "doctorProfession"?, "doctorClinic"?, "doctorPhotoUrl"?: string,
  "startAt": "ISO-UTC" /*req*/, "endAt": "ISO-UTC" /*req*/,
  "location": string /*req*/,     // "Video Consultation" ⇒ isVideo
  "isVideo"?: bool,
  "reason"?: string,
  "status"?: "CONFIRMED" | "PENDING" | "CANCELLED" | "COMPLETED",  // null ⇒ CONFIRMED; unknown (e.g. REQUESTED) ⇒ PENDING
  "paymentStatus"?: "NOT_REQUIRED" | "PENDING" | "PAID" | ...,     // default NOT_REQUIRED
  "paymentAmountMinor"?: int, "paymentCurrency"?: string,
  "signatureRequested"?: bool, "alreadySigned"?: bool
}
```
- **`location` is required as a String.** A null `location`, for example on a video appointment, crashes parsing. The server must always send it.
- Doctor phone and email on the details screen come from D2, not from the appointment.

### 5.7 Document (`core/models/document_model.dart`), from F1 and F2
```jsonc
{ "id": any, "patientId"?: any, "title": string,
  "date" | "createdAt"?: string,
  "fileUrl" | "url": string,
  "type"?: string /* mime/extension hint */,
  "doctorId"?: any, "doctorName"?: string,
  "creatorLabel"?: "Doctor" | "Patient" | "Unknown",
  "category"?: string /* see F2 list */,
  "isSharedWithTeam"?: bool }
```

### 5.8 ChatContact (`features/chat/domain/chat_models.dart`), from C1 and C2 `.conversation`
```jsonc
{ "id": any, "participantName": string, "participantPhotoUrl"?: string,
  "isDoctorParticipant": bool /*default true*/, "participantId": any,
  "lastMessage"?: string, "lastMessageAt"?: "ISO", "unreadCount"?: int }
```

### 5.9 ChatMessage (two accepted formats), from C2 `.messages[]` and C3
The new format is used when both `content` and `type` are present:
```jsonc
{ "id": any, "chatId": any, "senderId": any,
  "senderRole": "doctor" | "patient",                     // default doctor
  "type": "text" | "image" | "voice" | "document" | "system",
  "content": { "text"?, "fileUrl" | "url" | "attachmentUrl"?, "thumbnailUrl"?,
               "fileName"?, "fileSize"?: int, "duration"?: int },
  "status"?: "sending" | "sent" | "delivered" | "read",   // default sent
  "createdAt": "ISO" /*req*/, "isMine": bool, "isRead": bool }
```
The legacy format is flat:
```jsonc
{ "id", "conversationId"?, "senderId"?, "text"?, "attachmentUrl"?, "attachmentName"?,
  "isRead": bool, "isMine": bool, "createdAt": "ISO" /*req*/ }
```
In the legacy format the type is inferred: a `.pdf`, `.doc` or `.docx` extension means document, other attachments are images, and no attachment means text.

UserSearchResult (C5): `{ id, name, photoUrl?, isDoctor (default true), email?, phone? }`.

### 5.10 Notification (`core/models/notification_model.dart`), from N1
```jsonc
{ "id": int /*req*/, "title": string /*req*/, "message": string /*req*/,
  "type": string /*req, see §4.9*/, "createdAt": "ISO" /*req*/, "readAt"?: "ISO",
  // each optional id accepted as camelCase, snake_case, or nested under "data":
  "appointmentId"?, "patientFormId"?, "taskId"?, "treatmentPlanId"?,
  "documentId"?, "chatId"?, "doctorName"?,
  // camel or snake only (not under data):
  "documentAccessRequestId"?: int,
  "documentAccessRequestStatus"?: "pending" | "approved" | "rejected",
  "data"?: { ... } }
```
A notification is read when `readAt` is non-null. There is no `isRead` field.

### 5.11 RemoteCareTask and TaskCheckIn (`core/models/task_model.dart`), from T1-T3
```jsonc
// RemoteCareTask
{ "id": int|string, "taskName": string, "description"?: string,
  "category": "VITAL" | "EXERCISE" | "MEDICATION" | "OTHER",
  "status": "DRAFT" | "ACTIVE" | "COMPLETED" | "EXPIRED" | "CANCELLED",   // default ACTIVE
  "timesPerDay": int /*default 1*/,
  "morningTime"?: "HH:mm[:ss]", "afternoonTime"?: "...", "eveningTime"?: "...",
  "startDate": "yyyy-MM-dd" /*req*/, "endDate"?: "yyyy-MM-dd", "durationDays"?: int,
  "inputType": "NUMERIC" | "TEXT" | "BOOLEAN",                             // default TEXT
  "inputLabel"?: string, "notesRequired"?: bool, "notesLabel"?: string,
  "createdAt": "ISO" /*req*/,
  "progress"?: { "totalCheckIns", "completedCheckIns", "pendingCheckIns", "missedCheckIns" } }

// TaskCheckIn
{ "id": int, "scheduledDate": "yyyy-MM-dd", "scheduledTime": "HH:mm[:ss]",
  "status": "PENDING" | "COMPLETED" | "MISSED" /*req*/,
  "numericValue"?: number, "textValue"?: string, "booleanValue"?: bool,
  "notes"?: string, "completedAt"?: "ISO" }
```

### 5.12 Other payloads
The remaining shapes are documented inline in their endpoint sections:

| Payload | Section |
|---|---|
| AvailableSlot | B1 |
| VisitSummary / Ask | B9, B10 |
| TreatmentPlan | B13 |
| Checkout / PaymentStatus | $1-$3 |
| VideoToken | V1 |
| DeleteAccountChallenge | P5 |
| CheckIdentifier | A2 |
| Copilot results | X5, X7 |

Profession, country and region lists are **local catalogs** (`core/models/profession_model.dart`, `country_catalog.dart`, `region_catalog.dart`), not API data.

---

## 6. Inconsistencies, tolerant parsing, dead code

### 6.1 Contract inconsistencies

**Paths and auth**
1. **Chat upload path**: `POST /messages/upload-attachment` sits outside `/patients/me/**`, while every other chat call is under `/patients/me/messages/**`.
2. **Reviews on `/public`**: `POST /public/doctors/{id}/reviews` is a write on a "public" path that needs the patient identity. Because of the `/public/` prefix, a 401 there never logs the user out.
3. **`/auth/reset-password` is authenticated**, unlike the rest of `/auth/**`.
4. **Two avatar upload endpoints**: P3 `/patients/me/photo` with a blind fallback to P4 `/profile/photo`.
5. **Colon in path**: `ai-visit-summary:generate`, next to slash-style `ai-visit-summary/ask`.
6. **Singular and plural naming**: `/notifications/unread/count` vs `/patients/me/messages/unread-count`, and `/tasks/my-tasks/{id}/check-in` (POST) vs `/check-ins` (GET).

**Field shapes**
7. **Names in and out**: profile PATCH sends `firstName`/`lastName`, but GET returns only `fullName`. Appointments return a single `doctorName`, which the client splits on the first space.
8. **Multiple location key names on Doctor**: `locationCity` or `city`, `locationRegion` or `region`, `locationStreetAddress` or `street`.
9. **Document URL and date keys**: `fileUrl` or `url`, `date` or `createdAt`.
10. **Two ChatMessage formats**, new (typed `content`) and legacy (flat). The server may emit either.
11. **Notification ids**: `readAt` rather than `isRead`. Entity ids come in camelCase, snake_case or nested `data.*`. Notification ids are int in N1 but may be UUID strings in FCM, and N3 is called with both.
12. **Video detection**: `location == "Video Consultation"` **or** `isVideo`.

**Required-field hazards**
13. **`location` required**: Appointment `location` must be a non-null string, or parsing fails.
14. **Numeric-string ids**: ids are strings client-side, but `int.parse` happens before sending. A non-numeric id throws on the client before any request is made.

**Status and value vocabularies**
15. **Appointment status mapping**: unknown values such as `REQUESTED` fall into PENDING, and a missing value means CONFIRMED.
16. **Four language vocabularies**: `en/de/uz/ru` (profile and visit summary), `EN/RU/UZ` (copilot), `en/ru/uz` (transcription), and `app=patient` (auth).
17. **Payment status**: compared uppercase against `PAID` only. Other values (FAILED, CANCELLED) are not handled, and polling simply continues.

**Flow quirks**
18. **Forgot-password phone quirk**: a phone identifier with `channel:"email"` is posted as `email: "<phone>"`.
19. **`documentIds` requires numeric document ids** from F2.

**Configuration and URLs**
20. **Public config never loads** in default builds, because the URL is built from the raw define. So `transcriptionFeedbackEnabled` and the backend-provided Maps key are ineffective.
21. **Relative photo URLs** resolve to `http://localhost:8090` in production.
22. **CLAUDE.md flavors** (FLAVOR, localhost:8080/9090) do not exist in code. All builds default to production.

### 6.2 Tolerant parsing to preserve (or replace with a strict contract)

| Endpoint | Tolerance |
|---|---|
| D1 | Accepts `{doctors,totalCount,page,pageSize}` **or** a bare array |
| T1 | Accepts a bare array **or** `{data: [...]}`, and skips unparseable items |
| D3 | Drops locations with `id <= 0`, and returns `[]` on any error |
| B2, D4, N1 | A non-array response yields `[]` |
| C6 | Errors yield `0` |
| B7 | Any error yields "no review" |
| P3/P4 | Response `{photoUrl}` **or** a bare string |
| A2 | 404 yields `none` |
| A3 | 404, `{exists:false}` or `{found:false}` yields not found |
| A5 | Missing `channel` yields `sms`. A9's missing `channel` yields `email`. |
| B9 | Missing fields default (`not_ready`, `en`, `1`). `citations` may be a list of any values. |
| Dates | Trailing `[Zone/Id]` is stripped before ISO parsing |
| Errors | Accepted shapes are `message`, `error`, `detail`, `reason` and a raw string (§1.6) |

### 6.3 Dead or unused code

| Item | Location | Why it is dead |
|---|---|---|
| A3 `checkExistingDoctor` | `auth_repository.dart:132` | no callers |
| A10 Firebase `idToken` variant | `auth_repository.dart:380` | nothing produces an idToken; `PhoneAuthRepository` is unused |
| `sendSmsOtp(purpose:)` | `auth_repository.dart:188` | `purpose` is never passed |
| `register()` birthDate/gender/address/language | `auth_repository.dart:305` | never passed by UI |
| X6 `suggest-doctors` | `copilot_api.dart:261` | its wrappers are never called |
| `FileUploadService` and `file_upload_mobile.dart` | `features/chat/services/` | never referenced |
| `ApiClient.uploadFile` | `api_client.dart` | no callers |
| `NotificationPollingService` (30s N1 poll) | `core/services/notification_polling_service.dart` | `startPolling` is never called; only `clearShownNotifications` is used |
| `GoogleGeocodingService.geocode` (forward) | `google_geocoding_service.dart:106` | no callers |
| `UserModel` | `core/models/user_model.dart` | never parsed from an API response |
| `ReviewsRepository.hasReviewForAppointment` | `reviews_repository.dart:50` | always returns false |
| `DoctorsSearchFilters.clinic`, `getAppointments(endDate:)` | — | never set by UI |
| Unauthenticated-path entries for `/auth/register`, `/auth/register-clinic-staff`, `/auth/verify-key`, `/auth/check-existing-patient`, `/auth/verify`, `/auth/verify-email-otp`, `/auth/send-login-otp` | `api_client.dart:57-76` | these endpoints are never called by this app |

### 6.4 Items needing backend confirmation (UNVERIFIED)
- The error envelope, and whether it is consistent across controllers.
- Whether P4 `/profile/photo` exists.
- Whether B1 `day` is interpreted in the device zone or the profile `timeZone`.
- The set of values for `paymentStatus`, payment `status`, AI summary `status` and appointment `status` (for example `REQUESTED`).
- Whether P5 sends the deletion OTP itself, and how the A4 `purpose=ACCOUNT_DELETION` code binds to `challengeId`.
- How X4 behaves for doctors who require payment.
- Server-side enforcement of the video join window and the task check-in window.
- Whether D5 requires the token (it presumably does).
