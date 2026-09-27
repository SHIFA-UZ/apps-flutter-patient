# 01 — Architecture, App Core and Cross-Cutting Infrastructure

Spec for rebuilding the **Shifa Bemor** patient app (Flutter, package `shifa_patient_app_v1`, version `1.1.9+8`) in Kotlin Multiplatform + Compose Multiplatform. The source is the Flutter code at the commit this doc was written against. Citations use `path:line`. **UNVERIFIED** marks anything the code does not prove. Secrets are shown as `<REDACTED>`.

Other docs in this folder cover the features. This one covers what they all share: bootstrap, networking, storage, logging, error handling, localization, subscription gating, platform configuration and dependencies.

---

## 1. App overview

| Item | Value | Source |
|---|---|---|
| Product name (launcher / display) | `Shifa Bemor` ("bemor" = patient in Uzbek) | `lib/core/config/app_config.dart:42`, `android/app/src/main/AndroidManifest.xml` `android:label`, `ios/Runner/Info.plist` `CFBundleDisplayName` |
| Localized in-app name | l10n key `appName` | `lib/app/app.dart:130` |
| Release date label (About screen) | `5 Aug 2026` | `lib/core/config/app_config.dart:46` |
| Android applicationId / namespace | `com.shifa.patientapp` | `android/app/build.gradle.kts` |
| iOS bundle id | `uz.shifa.patient` (team `NH97PGA9PT`) | `ios/Runner.xcodeproj/project.pbxproj`, `lib/firebase_options.dart:59` |
| Web | Firebase Hosting target `patient`, site `shifa-bemor` | `firebase.json`, `.firebaserc` |
| Backend | Spring Boot REST, `https://shifa-doc-backend-mvp-production.up.railway.app/api` | `lib/core/config/app_config.dart:9-10` |
| Firebase project | `shifa-doctor-staging` (sender `55173524197`). **Despite the name, production uses it.** | `lib/firebase_options.dart:35-60` |
| Locales | en (default/fallback), de, uz, ru | `lib/app/app.dart:135-140` |
| Theme | Light only, Material 3, primary teal `#00BBB0` | `lib/app/app.dart:132` (see design-system doc) |

Feature modules under `lib/features/`:
- auth
- bookings (includes payments and video)
- chat
- copilot ("Shifa AI")
- doctors
- documents
- home
- notifications
- profile
- settings (app lock)
- tasks

Shared code lives in `lib/core/`, `lib/app/` (router, shell, bottom bar) and `lib/state/subscription/`.

Architecture:
- Each feature has `data/` (repositories), `providers/` (Riverpod `StateNotifier`) and `presentation/` (screens).
- There is no domain/use-case layer.
- State is held in immutable state classes with `copyWith`, and holds `isLoading`, `error` and the data.
- A KMP port maps each `StateNotifier` to a ViewModel (or state holder) exposing `StateFlow<State>`.

---

## 2. Bootstrap sequence

### 2.1 `main()` (`lib/main.dart:23-69`)

1. `WidgetsFlutterBinding.ensureInitialized()` (`:24`).
2. Make the status bar transparent. The navigation bar keeps the system default (`:27-29`).
3. **First-launch keychain wipe** (`:32-42`). This exists because the iOS Keychain survives uninstall.
   - If the SharedPreferences bool `has_launched_before` is not `true`:
     1. Call `StorageService().clearAuthToken()` with a **3 s timeout**. On timeout it silently continues.
     2. Set `has_launched_before = true`.
   - Any exception is logged and swallowed.
   - Only the auth keys are cleared. App-lock keys in the Keychain are **not** cleared, but they are ignored while logged out (§12).
4. **Firebase init**: `Firebase.initializeApp(DefaultFirebaseOptions.currentPlatform)` with a **10 s timeout** (`:45-47`).
   - On non-web, also register the background FCM handler (`:48-50`).
   - Failure is logged and the app continues without Firebase. Push and phone-auth then won't work; the code checks `Firebase.apps.isNotEmpty` before using FCM.
5. `runApp(ProviderScope(child: ShifaPatientApp()))` (`:55-59`).
6. **Deferred, not awaited**: on non-web, `PushNotificationService().initialize()` (`:62-68`), so Android leaves the native splash quickly. Errors are logged.
   - It requests notification permission (alert/badge/sound, not provisional).
   - If permission is not granted it stops early and fetches no token.
   - Details are in the notifications doc.

Background FCM handler (`lib/main.dart:17-21`): a top-level function annotated `@pragma('vm:entry-point')`. It re-initializes Firebase and logs `messageId`. It does nothing else; the OS shows notification payloads.

**Crashlytics is NOT wired.**
- `firebase_crashlytics` is in `pubspec.yaml:104`.
- The Gradle plugin `com.google.firebase.crashlytics` is applied (`android/settings.gradle.kts:25`, `android/app/build.gradle.kts:18`).
- But no Dart code calls it: no `FlutterError.onError`, no `recordError`, no `PlatformDispatcher.onError`.
- In KMP, decide deliberately. Recommendation: add Crashlytics via the GitLive SDK, or native wiring per platform, and hook the uncaught-exception handlers.

### 2.2 Root widget (`lib/app/app.dart`)

`ShifaPatientApp` is a `ConsumerStatefulWidget` and a `WidgetsBindingObserver`.

State flags:
- `_callbackSetup`
- `_fcmTokenUploadSetup`
- `_clearedNotificationsOnLogout`
- `_timezoneSyncOnAuthDone`

On every build (`:56-128`):

1. It watches `routerProvider`, `languageProvider` and `authStateProvider`.
2. **Once, post-frame**: it registers `apiClient.setOnAuthError(...)` (`:63-80`). The callback calls `authNotifier.logout()`, but only if `authState.isAuthenticated` is still true, which prevents loops.
3. **Timezone sync**: if authenticated and not done yet, then once post-frame it calls `profileProvider.notifier.syncTimeZoneFromDevice()` (`:85-93`). The flag resets when logged out.
   - It reads `FlutterTimezone.getLocalTimezone()` (an IANA id such as `Asia/Tashkent`).
   - If the value is non-empty and differs from `profile.timeZone`, it sends `PATCH /patients/me/profile` with body `{"timeZone": "<iana>"}`.
   - Errors are ignored.
   - Sources: `lib/features/profile/providers/profile_provider.dart:130-141`, `lib/features/profile/data/profile_repository.dart:67-70`.
4. **FCM token upload**: runs when authenticated, `_fcmTokenUploadSetup` is false and `Firebase.apps.isNotEmpty` (`:95-116`).
   - It registers `PushNotificationService.setOnFcmTokenReady(token → updateFcmToken(token))`. Empty tokens are ignored.
   - It also immediately uploads the current `getFcmToken()` if that is non-empty.
   - Endpoint: `PUT /patients/me/fcm-token`, body `{"fcmToken": "<token>"}` (`lib/features/notifications/data/notifications_repository.dart:81-87`).
5. **When logged out** (`:117-124`): reset the FCM flag, and once per logout call `notificationPollingServiceProvider.clearShownNotifications()`. That removes the SharedPreferences key `shown_notification_ids`.
6. Notification **polling is disabled**; the app relies on FCM (comment at `:82-83`).
7. It returns `AppLockLifecycleLayer(child: MaterialApp.router(...))` (`:129-149`):
   - title = `AppLocalizations(locale).appName`
   - `debugShowCheckedModeBanner: false`
   - `theme: AppTheme.lightTheme`
   - `routerConfig: router`
   - `locale: languageState.locale`
   - supportedLocales en, de, uz, ru
   - delegates: AppLocalizations plus the Global Material/Widgets/Cupertino delegates
8. If build throws, it shows a fallback error screen (`:150-176`):
   - A red `error_outline` icon.
   - The text `translate('appInitializationError') + ': ' + userFriendlyError(e)`.
   - The text `translate('pleaseRestartApp')`.

### 2.3 Lifecycle hooks

| Event | Action | Source |
|---|---|---|
| `resumed` while authenticated | `syncTimeZoneFromDevice()`, which PATCHes only if the zone changed | `lib/app/app.dart:46-53` |
| Cold start, authenticated, lock enabled, PIN set | The PIN screen **replaces the whole app** until unlocked. Reading the lock state has a 5 s timeout; timeout or error → **show the lock** (fail closed). | `lib/core/widgets/app_lock_lifecycle_layer.dart` (settings/app-lock doc) |
| `paused` / `detached` → `resumed` after ≥ 5 s | Re-lock. `inactive` and `hidden` do not count as background. | same |
| Lock screen "forgot / force login" | `authNotifier.logout()` | same |
| Logout | See §6.4 | `lib/features/auth/providers/auth_provider.dart:198-218` |

### 2.4 Splash and auth bootstrap (summary; full detail in the auth and navigation docs)

- The router's `initialLocation` is the splash route.
- The router swallows Firebase reCAPTCHA return paths `/link` and `/__/auth` (`lib/app/router.dart:194-220, 293`).
- Splash timing: a minimum animation of **1600 ms**, an auth-wait timeout of **4 s**, and a hard fallback of **5 s**.
- Splash destination:
  - not authenticated → login
  - `forcePasswordReset` → reset-password
  - otherwise → home
- `AuthNotifier` runs `_checkAuth` in a microtask on creation:
  - If a token exists, it validates it with `GET /patients/me/profile`.
  - **Only a 401 makes the token invalid.** Network or other errors keep the user logged in (offline-tolerant).
  - An exception in the check → unauthenticated.
  - Then it sets `isAuthReady = true`.
- `PROMO_CAPTURE` (a bool dart-define, `lib/features/auth/providers/auth_provider.dart:22`) fakes an authenticated state with token `promo-capture`. It exists only for marketing screenshots (`tool/capture_promo_screens.dart`, `integration_test/promo_screens_capture_test.dart`). A port can drop it.

---

## 3. Environment, flavors and base-URL resolution

### 3.1 Build-time variables (`--dart-define`) — `lib/core/config/app_config.dart`

| Define | Default | Use | Source |
|---|---|---|---|
| `API_BASE_URL` | `''` | Overrides the backend host. May or may not end in `/api`. | `:16-19` |
| `ENVIRONMENT` | `'development'` | `isProduction` (`== 'production'`), `isDevelopment`, `isStaging`. `enableDebugLogging = !isProduction`. | `:24-39` |
| `GOOGLE_MAPS_API_KEY` | `''` | Reverse geocoding. When empty, the key is fetched from the public config (§5). | `:52-55` |
| `PROMO_CAPTURE` | `false` | Screenshot mode (§2.4) | `lib/features/auth/providers/auth_provider.dart:22` |

Constants:
- `productionApiBaseUrl = 'https://shifa-doc-backend-mvp-production.up.railway.app'` (no `/api`) (`:9-10`)
- `appDisplayName = 'Shifa Bemor'`
- `releaseDateLabel = '5 Aug 2026'`

**There is no `FLAVOR` variable in the code.** CI passes `--dart-define=FLAVOR=production`, but nothing reads it. The project `CLAUDE.md` table ("debug = localhost:8080, qa = localhost:9090") is **stale**. There are no Android productFlavors or iOS schemes per flavor.

### 3.2 API base URL (`lib/core/network/api_client.dart:13-40`)

```
if API_BASE_URL != '':        base = API_BASE_URL with regex /\/api\/?$/ removed;  apiBaseUrl = base + "/api"
else if release build:        apiBaseUrl = "https://shifa-doc-backend-mvp-production.up.railway.app/api"
else (debug, any platform):   apiBaseUrl = "https://shifa-doc-backend-mvp-production.up.railway.app/api"
```

- **Debug builds also hit production.** The debug branch has web/android/other cases, but all three are the production URL. Its comment about localhost / 10.0.2.2 is stale.
- Store builds (Play, TestFlight) pass `ENVIRONMENT=production` and **no** `API_BASE_URL`, so they use the release branch.
- Web deploys pass `API_BASE_URL` from a secret (`.github/workflows/web_deploy.yml`).
- Requests go to `apiBaseUrl + path`, where every repository path starts with `/` and has no `/api`. Example: `/patients/me/profile` → `https://…/api/patients/me/profile`.
- `ApiClient.baseUrl` (instance getter, `:203-206`) is `apiBaseUrl` with `/api` stripped. Code that builds multipart URLs itself uses it as `${baseUrl}/api/...`.

### 3.3 `publicBaseUrl` (`lib/core/network/api_client.dart:42-50`) — dev-only static-file host

| Platform | Value |
|---|---|
| web | `http://localhost:8090` |
| Android | `http://10.0.2.2:8090` |
| other (iOS) | `http://localhost:8090` |

`normalizePhotoUrl` is its only user (`lib/core/utils/image_utils.dart:4-29`):

- null, empty or blank → `null`.
- Absolute `http(s)://` URL:
  - web: returned as is.
  - Android with host `localhost` or `127.0.0.1`: scheme, host and port are rewritten to `http://10.0.2.2:8090`.
  - otherwise: returned as is.
- Relative URL → `publicBaseUrl + '/' + path` (leading `/` stripped).

**Bug:** this happens in production too, so a relative photo URL would point to localhost. **UNVERIFIED** whether the production backend ever returns relative URLs; it appears to return absolute URLs. **KMP:** resolve relative paths against the API host, or treat them as invalid.

`withCacheBuster(url, cacheKey)` (`:31-`) normalizes the URL, then, if `cacheKey` is non-null and non-zero, adds the query parameter `v=<cacheKey>` (keeping existing query parameters).

### 3.4 Platform network rules

- Android `usesCleartextTraffic="false"` and `networkSecurityConfig=@xml/network_security_config`:
  - The main config has no cleartext and trusts system and **user** CAs.
  - A `debug-overrides` / domain config allows cleartext only for `localhost`, `127.0.0.1`, `10.0.2.2` and `::1`.
- iOS has no `NSAppTransportSecurity` exceptions, so ATS defaults apply (HTTPS only).

---

## 4. ApiClient (full behavior) — `lib/core/network/api_client.dart`

A single instance: `apiClientProvider = Provider((ref) => ApiClient())` (`lib/core/network/api_providers.dart:4`). Nothing else constructs it. It wraps Dio.

### 4.1 Configuration (`:81-93`)

- `baseUrl = apiBaseUrl` (§3.2)
- `connectTimeout = 30 s`, `receiveTimeout = 30 s`. **No send timeout**, so uploads are unbounded.
- Default headers: `Content-Type: application/json` and `Accept: application/json`.
- **No `Accept-Language` header.** The backend learns the language only through the profile `language` field (§10.3).
- Status handling uses the Dio default: **any non-2xx throws** `DioException`. Repositories catch it and read `e.response?.data['message']` (see each feature doc).

### 4.2 Request interceptor (`:96-112`)

- If `isUnauthenticatedAuthPath(options.path)` is **false**:
  - Read `auth_token` from secure storage.
  - If it is non-null, set `Authorization: Bearer <token>`.
  - **Any storage exception is swallowed** and the request goes out without a token. This is deliberate: a slow or broken Android Keystore must not block requests.
- For unauthenticated auth paths, secure storage is **not touched at all**.

`isUnauthenticatedAuthPath(path)` (`:54-77`):
- If the path contains `://`, take its URI path. Ensure it has a leading `/`.
- The result is `true` if the path **equals or ends with** any of these (so `/api/auth/login` matches too):

```
/auth/login
/auth/register
/auth/register-patient
/auth/register-clinic-staff
/auth/verify-key
/auth/check-existing-patient
/auth/check-existing-doctor
/auth/check-identifier
/auth/create-patient-for-doctor
/auth/send-email-otp
/auth/send-sms-otp
/auth/verify
/auth/verify-email-otp
/auth/send-login-otp
/auth/send-forgot-password-otp
/auth/forgot-password-reset
```

Note: `/auth/reset-password` (forced reset for a logged-in user) and `/auth/change-password` are **not** listed, so they carry the token.

### 4.3 Error interceptor (`:113-192`)

1. If there is a response, call `AppLogger.apiError(status, data, path)` (§7).
2. **Only 401** can log out. **403 never logs out** (valid token, wrong role).
3. For 401, logout is skipped when:
   - the path `startsWith('/public/')` or `startsWith('/auth/')` (prefix check, unlike the suffix check in §4.2), or
   - `_isLoggingOut` is true.
4. Otherwise these checks run in order. The first that matches passes the error on **without** logging out:
   1. The request had **no** `Authorization` header (the token read failed or there was no token).
   2. `_lastLogoutAttempt` was **< 5 s** ago.
   3. Reading the token **throws**, or the token is **null**.
   4. **Grace period**: read `auth_token_saved_at` (ISO-8601). If `now − savedAt < 10 s`, skip. This avoids a race right after login. Read or parse errors mean no grace period.
5. If none match, log out:
   1. `_isLoggingOut = true`, `_lastLogoutAttempt = now`.
   2. `await clearAuthToken()` (deletes all 3 auth keys).
   3. `Future.microtask(onAuthError)`. That runs `AuthNotifier.logout()` if the user is still authenticated (§2.2).
   4. After 5 s, `_isLoggingOut = false`.
6. With no response (timeout or DNS), log `API Error (no response):` plus the message.
7. The error is **always** passed on (`handler.next(error)`). Callers still receive the exception.

### 4.4 Methods (`:198-247`)

| Method | Notes |
|---|---|
| `setOnAuthError(cb)` | Set by the root widget |
| `baseUrl` | apiBaseUrl without `/api` |
| `getAuthToken()` | Reads secure storage directly; for code that bypasses Dio |
| `dio` | The raw Dio. Used directly in `chat_conversation_screen.dart:201,282`, `profile_screen.dart:515,535` and `documents_repository.dart:17` (a GET with a bytes response) and `:78` |
| `get(path, {queryParameters})`, `post(path, {data, queryParameters})`, `put(path, {data})`, `patch(path, {data})`, `delete(path)` | Thin wrappers |
| `uploadFile(path, filePath, {fieldName='file', additionalFields})` | A multipart POST. **Nothing calls it.** |

Requests that **bypass** ApiClient, and therefore its 401 logic and timeouts:
- Chat attachments use `http.MultipartRequest` to `${apiClient.baseUrl}/api/messages/upload-attachment` with a manually added `Authorization: Bearer` header (`lib/features/chat/services/file_upload_mobile.dart:16-41`).
- The Shifa AI API (`copilot_api`) uses the `http` package with `StorageService().getAuthToken()`.
- The public config and Google geocoding use `http` with no auth (§5).
- The certificate viewer downloads with `http` (see the documents doc).

**KMP recommendation:**
- Use one Ktor `HttpClient` with `defaultRequest { url(apiBaseUrl) }`, `HttpTimeout(connect = 30 s, socket/request = 30 s)` and `ContentNegotiation(json)`.
- Add the bearer token in a custom plugin, or in `Auth { bearer { loadTokens } }`. Note that Ktor's `Auth` plugin only sends the header after a 401 unless `sendWithoutRequest { true }` is set, so a custom plugin matches the current behavior more simply.
- Use `HttpResponseValidator` for the 401 rules above.
- Route multipart and AI calls through the same client, with the logout logic scoped to protected paths. That removes the bypasses.

---

## 5. Public config endpoint — `GET /api/public/config`

`lib/core/network/public_backend_config.dart`:

- Called with the `http` package, **no auth**, **5 s timeout**.
- The URL is `${AppConfig.apiBaseUrl}/api/public/config`. `AppConfig.apiBaseUrl` is the **raw `API_BASE_URL` define**, not `ApiClient.apiBaseUrl`.
- **If `API_BASE_URL` is empty, nothing is fetched and the result is `{}`** (`:28-31`). **Play Store and TestFlight builds never call this endpoint.** Only web and custom builds do.
- Quirk: if `API_BASE_URL` already ends in `/api`, the URL becomes `/api/api/public/config`.
- Memoization:
  - A static `_cache`.
  - If a fetch is in flight, concurrent callers poll 20 × 80 ms, then fall through (`:19-26`).
  - HTTP 200 with a JSON object → cached.
  - Anything else, or an exception → `{}` is cached, so there is no retry until `clearCache()`.
  - `clearCache()` is at `:14-16`.

Keys read from the response:

| Key | Type | Use | Source |
|---|---|---|---|
| `transcriptionFeedbackEnabled` | bool, or the string `"true"` | Shows the "Report incorrect text" action for voice transcription in Shifa AI | `public_backend_config.dart` `transcriptionFeedbackEnabled()`, `lib/features/copilot/presentation/shifa_ai_screen.dart:284` |
| `googleMapsApiKey` | string | Google reverse-geocoding key, used when the build-time `GOOGLE_MAPS_API_KEY` is empty. Same `API_BASE_URL`-non-empty guard and 5 s timeout. | `lib/core/services/google_geocoding_service.dart:20-57` |

Geocoding request: `GET https://maps.googleapis.com/maps/api/geocode/json?latlng=<lat>,<lng>&key=<REDACTED>&language=en`.

On store builds without the define, both the key and the config are empty. What geocoding then falls back to is covered in the profile/location doc.

Map tiles come from OpenStreetMap: `https://tile.openstreetmap.org/{z}/{x}/{y}.png` (`lib/features/profile/presentation/location_picker_widget.dart:554-555`).

---

## 6. Storage — every persisted key in `lib/`

### 6.1 Secure storage: auth (`lib/core/utils/storage_service.dart`)

- Backend: `FlutterSecureStorage`. On Android: EncryptedSharedPreferences / Keystore, file `sharedPreferencesName: 'shifa_auth_secure'`, `resetOnError: true` (an undecryptable store is wiped, which forces a re-login). On iOS: the Keychain with default accessibility.

| Key | Value | Written | Deleted |
|---|---|---|---|
| `auth_token` | The JWT string | `saveAuthToken`, called from `lib/features/auth/data/auth_repository.dart:67, 277, 321, 389, 439, 469` | `clearAuthToken()` |
| `auth_token_saved_at` | `DateTime.now().toIso8601String()` (local time, no zone suffix), written right after the token inside a try/catch | `saveAuthToken` | `clearAuthToken()` |
| `user_id` | A string | `saveUserId`, which **nothing calls**, so the key is never written | `clearAuthToken()` |

Other methods: `getAuthToken`, `getAuthTokenSavedAt`, `getUserId`, and `isAuthenticated` (token non-null and non-empty).

`clearAuthToken()` deletes all three keys. It is called by:
- the first-launch wipe (§2.1)
- the 401 logout (§4.3)
- `AuthRepository.logout`

### 6.2 Secure storage: app lock (`lib/core/services/app_lock_service.dart:9-42`)

Android file `sharedPreferencesName: 'shifa_app_lock_prefs'`, `resetOnError: false`.

| Key | Value |
|---|---|
| `app_lock_pin` | The PIN in **plaintext** (trimmed). 4–6 characters, must match `^\d+$`. |
| `biometric_enabled` | `'true'` / `'false'` |
| `app_lock_enabled` | `'true'` / `'false'` |
| `app_lock_inactivity_seconds` | An integer string. Default `30`. |
| `app_lock_seen_biometric_prompt` | `'true'` once the prompt has been shown |

Rules:
- `maxPinAttempts = 5`.
- Cooldown: 0 s for attempts 1–3, then 30 s from attempt 4.
- Full behavior is in the settings/app-lock doc.
- **KMP:** store a salted hash (for example PBKDF2) instead of the plaintext PIN. This is a behavior-compatible improvement, because the PIN is only ever compared.

### 6.3 Non-secure: SharedPreferences (Android `FlutterSharedPreferences`, iOS `NSUserDefaults`)

| Key | Type | Meaning | Source |
|---|---|---|---|
| `has_launched_before` | bool | First-launch detection for the keychain wipe | `lib/main.dart:34,38` |
| `app_language` | string `en`/`de`/`uz`/`ru` | UI language. Also read by the push service to localize notifications. | `lib/core/providers/language_provider.dart:76,94,109`, `lib/core/services/push_notification_service.dart:145` |
| `login_failed_attempts` | int | Failed login counter | `lib/core/services/login_attempt_service.dart:11` |
| `login_lockout_until_epoch_ms` | int (epoch ms) | Login lockout end | `lib/core/services/login_attempt_service.dart:12` |
| `shown_notification_ids` | string (JSON array of ints) | Notification IDs already shown as local notifications. Removed on logout. | `lib/core/services/notification_polling_service.dart:17` |
| `copilot_messages_v1` | string (JSON list of `{role, content}`) | Shifa AI chat memory | `lib/features/copilot/data/copilot_memory_store.dart:6` |
| `copilot_summary_v1` | string | Shifa AI rolling summary | same, `:7` |
| `copilot_structured_state_v1` | string (JSON object) | Shifa AI structured state | same, `:8` |

Login lockout (`login_attempt_service.dart:8-9`):
- `maxAttempts = 5`, `lockoutDurationMinutes = 5`.
- Each failure increments the counter. At ≥ 5 failures, set `until = now + 5 min`.
- Once `until` has passed, both keys are removed.
- A successful login calls `clearAttempts()`, which resets them (`lib/features/auth/providers/auth_provider.dart:92`).

**Privacy quirk:** the three `copilot_*` keys are cleared only by "clear conversation" (`lib/features/copilot/providers/copilot_chat_provider.dart:281`), **not on logout**. The next account on the same device sees the previous user's AI chat. **KMP:** clear them on logout.

### 6.4 Logout sequence (`lib/features/auth/providers/auth_provider.dart:198-218`)

This runs only if currently authenticated:

1. `notificationsRepository.updateFcmToken('')` (best effort, detaches the device from push)
2. `appLockService.clearPin()`
3. `appLockService.setLockEnabled(false)`
4. `appLockStateProvider.setLockEnabled(false)`
5. `authRepository.logout()`, which calls `clearAuthToken()`
6. `state = AuthState.initial`

Then:
- The router redirect sends the user to login.
- The root widget clears `shown_notification_ids` (§2.2).

When the logout was triggered by a 401, the token is already gone. Step 1 then goes out with no Authorization header, and the resulting 401 is ignored (§4.3 rule 4.1).

### 6.5 Backup exclusion (Android)

- `android/app/src/main/res/xml/backup_rules.xml` (`fullBackupContent`) and `data_extraction_rules.xml` (cloud backup and device transfer) exclude `sharedpref/shifa_app_lock_prefs.xml` and `sharedpref/shifa_auth_secure.xml`.
- `FlutterSharedPreferences.xml` (language, copilot memory, lockout) **is** backed up.
- iOS: the Keychain items survive reinstall, which is handled by §2.1.

### 6.6 Other persistent OS state

Android notification channels:

| Channel | Name / importance | Source |
|---|---|---|
| `shifa_patient_channel` | Push, `Importance.high` | `lib/core/services/push_notification_service.dart:160` |
| `appointments_channel` | "Appointment Notifications" (local reminders) | `lib/core/services/local_notification_service.dart:96` |

- Notification small icon: `@mipmap/ic_launcher`.
- The push service keeps an in-memory cache of read notification IDs, max 128 entries.

**KMP storage mapping:**
- Non-secure keys: `multiplatform-settings` (SharedPreferences / NSUserDefaults) with the **same key names**, so an in-place upgrade over the Flutter app keeps the language.
  - Note: Flutter's SharedPreferences stores keys with the prefix `flutter.` in the `FlutterSharedPreferences` file on Android and in NSUserDefaults on iOS.
  - To migrate, read `flutter.app_language` etc. once.
- Secure keys: Android `EncryptedSharedPreferences` (or DataStore + Tink) using the same file names; iOS Keychain.
  - flutter_secure_storage's on-disk format is its own. **UNVERIFIED** whether a native reader can decode it. The safe plan is to accept a forced re-login after migration.

---

## 7. Logger and redaction — `lib/core/utils/app_logger.dart`

- **Enabled only when** `AppConfig.enableDebugLogging && kDebugMode` (`:31`), which means a debug build with a non-production `ENVIRONMENT`. Release builds log nothing.
- `debug(message, [detail])`, `error(message, [error, stackTrace])`, and `apiError(statusCode, body, [path])` (`:34-73`).
- Redaction in `apiError` (`:10-29`, `:75-95`):
  - Map keys are lower-cased. A key is redacted to `***` if it **contains** any entry of the sensitive set:
    `token`, `authToken`, `accessToken`, `refreshToken`, `password`, `secret`, `authorization`, `cookie`, `apiKey`, `api_key`, `phone`, `email`, `name`, `firstname`, `lastname`, `surname`, `address`.
  - Quirk: the camelCase entries (`authToken`, `apiKey`, …) never match a lower-cased key. `authToken`/`accessToken`/`refreshToken` are still covered by `token`, but a key such as `apiKey` → `apikey` is **not** redacted; only `api_key` is. **KMP:** lower-case the sensitive set too.
  - Redaction recurses into nested maps.
  - Lists become `[N items]`.
  - The final string is truncated to **300** characters plus `…[truncated]`.
- `safeApiFailureMessage(statusCode, body)` (`:97-`):
  - debug → `Upload failed: <code> <body truncated to 200 chars…[truncated]>`
  - release → `Upload failed: <code>`
- Leak outside the logger: `push_notification_service.dart:82` prints the FCM token when `kDebugMode`. It only happens in debug builds.

**KMP:** use Kermit or Napier with a release no-op writer. Port `_redact` as-is (with the lower-case fix).

---

## 8. Error sanitizer (auth flows) — `lib/core/utils/auth_error_sanitizer.dart`

`AuthErrorSanitizer.sanitize({int? statusCode, dynamic data, required String defaultMessage}) → String`. The auth repositories use it to turn backend responses into safe English messages. `translateError` (§9) then localizes them.

Raw message:
- If `data` is a Map: `data['message'] ?? data['error']`.
- If `data` is a String: the string itself.

Algorithm:
1. If the raw message is **exactly** in the known-safe list → return it.
2. If it starts with `Login error: ` and the remainder is known-safe → return the remainder.
3. Otherwise decide by status:

   | Status | Result |
   |---|---|
   | 401 | `Invalid credentials` |
   | 403 | `defaultMessage` |
   | 404 | `No account found` |
   | 429 | `Too many requests. Please try again later.` |
   | 500, 502, 503 | `defaultMessage` |

4. Otherwise (unknown or technical) → `defaultMessage`.

Known-safe list (`:7-47`, exact strings):

```
No account found
No doctor account found
No patient account found
No admin account found
Doctor profile not found
Email OTP required when email is provided
Email verification code required
Invalid or expired email verification code
Invalid or expired verification code
Invalid phone number
Account is disabled
Account is disabled.
Not a doctor account
Missing Bearer token
Firebase verification not configured
Invalid or expired token
Access restricted to doctors.
Your account has been blocked.
Invalid key
Key already used
Username is required
Password is required
Invalid credentials
Account is locked
Invalid credentials - password mismatch
You need to create a patient account first. Use Create Account in the patient app and link your doctor account.
Phone verification not configured
Invalid or expired phone verification
Phone number not found in verification
Invalid phone in verification
Phone number does not match verification
Email already registered
Phone already registered
Patient with this phone number already exists.
Phone number not found
User not found
Too many requests. Please try again later.
Too many verification requests. Please try again later.
SMS could not be sent. Please use email verification or contact support.
```

"Technical message" detector (`:50-59`, all case-insensitive except the JSON one). A message is technical if it is **longer than 500 characters** or matches any of:

```
\b(SELECT|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER)\b
\b(ORA-\d+|SQLSTATE|syntax error)\b
\bat\s+\w+\.\w+\s*\([^)]*\)
\bCaused by:\s
\bException\s*:
\bNullPointerException|IllegalArgumentException|BadCredentialsException\b
^\s*\{.*\}\s*$
java\.|kotlin\.|org\.spring|org\.hibernate
```

---

## 9. Error localization — `lib/core/localization/error_localizations.dart`

### 9.1 `translateError(AppLocalizations l10n, String raw) → String`

Steps:
1. Trim. Empty → `errorSomethingWentWrong`.
2. Unwrap: repeatedly strip the leading prefixes `Exception:` and `Failed to book appointment:`, up to **8** times, trimming each time.
3. If it starts with `Login error: ` and the inner text is technical → `errorLoginFailed`.
4. **Exact map**: if the message matches a map entry exactly → that key (table §9.2).
   - Rule for steps 4–7: a match is used only if its key has a translation (`translate(key) != key`). Otherwise processing falls through to the next step.
   - Exception: step 5 rule 10 always returns.
5. **Fuzzy rules** on the lower-cased text, in order (first match wins):

   | # | Condition | Key |
   |---|---|---|
   | 1 | contains `sms could not be sent` | `errorSmsCouldNotBeSent` |
   | 2 | contains `too many verification requests` | `errorTooManyVerificationRequests` |
   | 3 | contains `connection timed out` or `check your internet and try again` | `errorConnectionTimedOut` |
   | 4 | starts with `network error` and contains `internet` | `errorNetworkConnection` |
   | 5 | contains `timeoutexception` and (`join failed` or `future not completed`) | `videoCallConnectionTimeout` |
   | 6 | contains `payment is required` and (`video` or `consultation`) | `errorVideoCallPaymentRequired` |
   | 7 | contains `not yet available` and `5 minutes before` | `errorVideoCallNotYetAvailable` |
   | 8 | contains `video call has ended`, or (`join window` and `15`) | `errorVideoCallHasEnded` |
   | 9 | contains `call error occurred` | `callErrorOccurred` |
   | 10 | starts with `Access denied: This app requires ` and ends with ` role` (case-sensitive) | `errorAccessDeniedThisAppRequiresRole`, with the literal `${requiredRole.name}` in the string replaced by `X` |

6. **Prefix map** (`:101-110`): if the text starts with the prefix, the remainder fills the placeholder.

   | Prefix | Key | Placeholder |
   |---|---|---|
   | `Invalid response from server: ` | `errorInvalidResponseFromServer` | `{{code}}` |
   | `Network error: ` | `errorNetworkError` | `{{type}}` |
   | `Appointment not found: ` | `errorAppointmentNotFound` | `{{id}}` |
   | `Failed to retrieve patient profile: ` | `errorFailedToRetrievePatientProfile` | `{{detail}}` |
   | `Failed to get or create room: ` | `errorFailedToGetOrCreateRoom` | `{{detail}}` |
   | `Failed to generate token: ` | `errorFailedToGenerateTokenDetail` | `{{detail}}` |
   | `Failed to generate video token: ` | `errorFailedToGenerateVideoToken` | `{{detail}}` |
   | `Error ` (only when the remainder is all digits) | `errorStatusCode` | `{{code}}` |

   The trimmed remainder is inserted **without** a technical-content check (test: `Network error: timeout` → the placeholder is filled with `timeout`).
7. `Patient profile not found for user <X>.` → `errorPatientProfileNotFoundForUser`. The id is the text before the first `.`. It replaces both `{{id}}` and the literal `${user.id}` in the translation.
8. A `Login error: …` prefix → `errorLoginFailed`. A `Registration error: …` prefix → `errorRegistrationFailed`.
9. Technical detector (narrower than §8, `:113-127`, case-insensitive). Match, or length > 500 → `errorSomethingWentWrong`:

   ```
   \bSELECT\b[\s\S]*\bFROM\b
   \bINSERT\s+INTO\b
   \bUPDATE\b[\s\S]*\bSET\b
   \bDELETE\s+FROM\b
   \b(DROP|ALTER|CREATE)\s+(TABLE|INDEX|DATABASE|SCHEMA)\b
   \b(ORA-\d+|SQLSTATE|syntax error)\b
   \bat\s+\w+\.\w+\s*\([^)]*\)
   \bCaused by:\s
   \bNullPointerException|IllegalArgumentException|BadCredentialsException\b
   java\.|kotlin\.|org\.spring|org\.hibernate
   ```

10. Final fallback, all → `errorSomethingWentWrong`:
    - text that looks like JSON (`{…}`)
    - text matching `\b\w*Exception\b|\bError\s*:` (case-insensitive)
    - text containing ` at `
    - text longer than **120** characters

    Otherwise **return the trimmed raw message untranslated**. Backend messages that are short and human-readable are shown verbatim, usually in English.

### 9.2 Exact message → key map (`:7-97`)

| Backend / app message (exact) | l10n key |
|---|---|
| Could not send verification SMS. Please try again later. | errorFailedToSendSmsCode |
| Could not send verification email. Please try again later. | errorFailedToSendEmailCode |
| Could not send verification code | errorFailedToSendVerificationCode |
| No email address on file. Please contact support. | errorNoEmailOnFileContactSupport |
| No account found | errorNoAccountFound |
| No doctor account found | errorNoDoctorAccountFound |
| No patient account found | errorNoPatientAccountFound |
| No admin account found | errorNoAdminAccountFound |
| Doctor profile not found | errorDoctorProfileNotFound |
| Email OTP required when email is provided | errorEmailOtpRequiredWhenEmailProvided |
| Email verification code required | errorEmailVerificationCodeRequired |
| Invalid or expired email verification code | errorInvalidOrExpiredEmailVerificationCode |
| Invalid or expired verification code | invalidVerificationCode |
| Invalid phone number | errorInvalidPhoneNumber |
| Account is disabled / Account is disabled. | errorAccountIsDisabled |
| Not a doctor account | errorNotADoctorAccount |
| Missing Bearer token | errorMissingBearerToken |
| Firebase verification not configured | errorFirebaseVerificationNotConfigured |
| Invalid or expired token | errorInvalidOrExpiredToken |
| Access restricted to doctors. | errorAccessRestrictedToDoctors |
| Your account has been blocked. | errorYourAccountHasBeenBlocked |
| Invalid key | errorInvalidKey |
| Key already used | errorKeyAlreadyUsed |
| Username is required | errorUsernameRequired |
| Password is required | errorPasswordRequired |
| Invalid credentials | errorInvalidCredentials |
| Account is locked | errorAccountLocked |
| Invalid credentials - password mismatch | errorInvalidCredentialsPasswordMismatch |
| You need to create a patient account first. Use Create Account in the patient app and link your doctor account. | errorCreatePatientAccountFirst |
| Phone verification not configured | errorPhoneVerificationNotConfigured |
| Invalid or expired phone verification | errorInvalidOrExpiredPhoneVerification |
| Phone number not found in verification | errorPhoneNumberNotFoundInVerification |
| Invalid phone in verification | errorInvalidPhoneInVerification |
| Phone number does not match verification | errorPhoneNumberDoesNotMatchVerification |
| Email already registered | errorEmailAlreadyRegistered |
| Phone already registered | errorPhoneAlreadyRegistered |
| Patient with this phone number already exists. | errorPatientWithPhoneAlreadyExists |
| Phone number not found | errorPhoneNumberNotFound |
| User not found | errorUserNotFound |
| Session invalid | errorSessionInvalid |
| Session expired or signed out | errorSessionExpiredOrSignedOut |
| Invalid token | errorInvalidToken |
| Too many requests. Please try again later. | errorTooManyRequests |
| Too many verification requests. Please try again later. | errorTooManyVerificationRequests |
| SMS could not be sent. Please use email verification or contact support. (`kSmsOtpUnavailableMessage`) | errorSmsCouldNotBeSent |
| Authentication required | errorAuthenticationRequired |
| Video call is not yet available. You can join 5 minutes before the appointment start. | errorVideoCallNotYetAvailable |
| Video call has ended. The join window closes 15 minutes after the appointment end. | errorVideoCallHasEnded |
| Payment is required before joining this video consultation. | errorVideoCallPaymentRequired |
| Camera and microphone access are required for video consultations. You can enable them in your device Settings. | errorVideoCallMediaPermissionRequired |
| Video call is not available. It may have ended, or the join window has closed (usually 15 minutes after the appointment end). | errorVideoCallHasEnded |
| Appointment does not belong to this doctor | errorAppointmentDoesNotBelongToDoctor |
| Appointment does not have a patient assigned. Please ensure the appointment is linked to your profile. | errorAppointmentDoesNotHavePatientAssigned |
| Appointment does not belong to this patient | errorAppointmentDoesNotBelongToPatient |
| Unable to determine user identity | errorUnableToDetermineUserIdentity |
| Unable to determine user name | errorUnableToDetermineUserName |
| User name cannot be blank | errorUserNameCannotBeBlank |
| Failed to generate token | errorFailedToGenerateToken |
| Daily.co API key is not configured. Please set DAILY_API_KEY environment variable in Railway. | errorDailyApiKeyNotConfigured |
| No token received from server | errorNoTokenReceivedFromServer |
| Login failed | errorLoginFailed |
| Failed to send email code | errorFailedToSendEmailCode |
| Failed to send SMS code | errorFailedToSendSmsCode |
| SMS verification is only available for Uzbek phone numbers. Please use email. | errorSmsOnlyForUzbekPhone |
| Could not verify account details | errorCouldNotVerifyAccountDetails |
| Connection timed out. Check your internet and try again. | errorConnectionTimedOut |
| Network error. Check your internet connection and try again. | errorNetworkConnection |
| No token received | errorNoTokenReceived |
| Failed to create patient account | errorFailedToCreatePatientAccount |
| Registration failed | errorRegistrationFailed |
| Failed to reset password | errorFailedToResetPassword |
| Failed to change password | errorFailedToChangePassword |
| Unknown error | errorUnknownError |
| Something went wrong | errorSomethingWentWrong |
| Session expired. Please start again. | errorSessionExpiredPleaseStartAgain |
| Boolean value required | checkInPleaseSelectYesOrNo |
| Numeric value required | checkInValueRequired |
| Text value required | checkInValueRequired |
| serviceId is required for video consultation | bookingSelectServiceForVideo |
| Please select a service for video consultation. | bookingSelectServiceForVideo |

### 9.3 `userFriendlyError(l10n, Object error, {String? logContext})`

1. Log the error (debug builds only).
2. Map the error type:
   - `SmsOtpUnavailableException` → `errorSmsCouldNotBeSent`
   - `OtpRateLimitException` → `errorTooManyVerificationRequests`
   - both types are defined in `lib/features/auth/data/auth_exceptions.dart`
3. Otherwise → `translateError(l10n, error.toString())`.

This is **the** function screens use to show any error. In KMP, provide `fun Throwable.toUserMessage(strings): String`, with repository exceptions carrying the backend `message` string so the same exact-match table works.

Dart `Exception('x').toString()` produces `Exception: x`. That is why the unwrap step exists. In Kotlin, use a `ShifaException(message)` and pass `message` directly.

---

## 10. Localization system

### 10.1 Runtime (`lib/core/localization/app_localizations.dart`, 4132 lines)

- The strings live in a single Dart map, `_localizedValues: Map<String, Map<String, String>>` (`:18-3608`). Sub-maps:
  - `en` at `:19-1002`
  - `de` at `:1003-1621`
  - `uz` at `:1622-2632`
  - `ru` at `:2633-3607`
- **They are extracted to `docs/kmp-spec/strings/{en,de,uz,ru}.json`** (flat, sorted, UTF-8). See `strings/README.md`.
- Lookup: `translate(key) = map[locale.languageCode][key] ?? map['en'][key] ?? key`. A missing key renders **the raw key**.
- `AppLocalizations.of(context)`, and `AppLocalizations.forLanguageCode(code)` for use outside the widget tree (the push service).
- Delegate: `isSupported` for `en`, `de`, `uz`, `ru`. `load` is effectively synchronous. `shouldReload = false`.
- Many typed getters (`l10n.appName`, `l10n.login`, …) are thin wrappers over `translate`, and some are typed `String?`.
- **No plural support** anywhere. Counts are interpolated into fixed strings.
- `languageChanged(lang)` = `translate('languageChanged') + ' ' + lang`.

Helper methods that fill placeholders (all use `{{…}}`):

| Helper | Placeholders |
|---|---|
| treatmentPlanLoadFailedWithCode | `{{code}}` |
| treatmentPlanUnexpectedError | `{{error}}` |
| treatmentPlanStatusLine, treatmentPlanPaymentStatusLineLong | `{{status}}` |
| treatmentPlanItemDue | `{{date}}` |
| treatmentPlanQuantityShort | `{{n}}` |
| treatmentPlanDiscountLine | `{{amount}}` |
| treatmentPlanProgressLine | `{{done}}`, `{{total}}` |
| doctorsCount | `{{count}}` |
| doctorsPageSummary | `{{page}}`, `{{pages}}`, `{{count}}` |
| fromPrice | `{{price}}` |

### 10.2 Placeholder syntaxes in the strings (all four coexist)

| Syntax | Keys | Filled by |
|---|---|---|
| `{{name}}` (most common) | accountLockedTryAgainIn `{{minutes}}`; codeExpiresIn, resendCodeIn `{{time}}`; currentPaymentStatus `{{status}}`; doctorsCount; doctorsPageSummary; errorAppointmentNotFound `{{id}}`; error*Detail / errorFailedTo… `{{detail}}`; errorInvalidResponseFromServer, errorStatusCode, treatmentPlanLoadFailedWithCode `{{code}}`; errorNetworkError `{{type}}`; existingDoctorMessage `{{name}}`; fromPrice `{{price}}`; loginAttemptsRemaining `{{count}}`; paymentCouldNotStartWithError, treatmentPlanUnexpectedError `{{error}}`; treatmentPlan* (see above); tryAgainInSeconds `{{seconds}}` | `String.replaceAll('{{x}}', v)` at the call sites |
| `{name}` (single brace) | checkInEnterLabeledValue `{label}`; documentAccessRequestMessage `{requesterName}` `{fileName}` `{patientName}`; maxBookingDocuments `{count}`; otpSentToEmail `{email}`; otpSentToPhone `{phone}`; signatureRequestedMessage `{doctorName}`; timeYesterday `{time}` | `replaceAll('{x}', v)` |
| Literal Dart-looking `${…}` | errorAccessDeniedThisAppRequiresRole `${requiredRole.name}`; errorPatientProfileNotFoundForUser `${user.id}` | `translateError` (§9.1) |
| `%s` | minutesAgo, hoursAgo, daysAgo | `replaceAll('%s', n)` in `lib/features/notifications/presentation/notification_ui_helpers.dart` |

A literal `$` appears in some values, for example `priceUnder20` = `Under $20` and `passwordRequirementSpecialChar` = `(!@#$%^&* etc.)`. These are correct and are not placeholders.

### 10.3 Language selection — `lib/core/providers/language_provider.dart`

`enum AppLanguage` (`:7-38`):

| Enum | code | displayName |
|---|---|---|
| english | `en` | English |
| german | `de` | Deutsch |
| uzbek | `uz` | O'zbek |
| russian | `ru` | Русский |

- `fromCode` returns english for unknown codes.
- `fromSystemLocale` maps the `de`, `uz` and `ru` language codes, and returns english for anything else.
- `LanguageNotifier` starts with english and asynchronously runs `_loadLanguage()` (`:73-104`):
  - If prefs `app_language` exists → use it.
  - Else detect the OS locale, **save it** to `app_language`, then call `PushNotificationService().refreshLocalizationCache()`.
  - Errors → english.
- `setLanguage(lang, {updateBackend = false})` (`:106-123`): saves the pref, updates the state and refreshes the push l10n cache. The `updateBackend` flag **does nothing**.
- Backend sync lives in the profile screen:
  - The language dialog (`lib/features/profile/presentation/profile_screen.dart:405-440`) calls `setLanguage`, then `profileProvider.updateProfile(language: code)`. That sends `PATCH /patients/me/profile` with `language`.
  - A `ref.listen` (`profile_screen.dart:41-52`) applies `profile.language` to the app **whenever the profile's language changes**, so the backend value wins over the local one.
- The login screen has a `LanguageMiniToggle` (`lib/core/widgets/`) that uses a local teal `0xFF17C3B2` (not `#00BBB0`).
- The backend uses the profile `language` to localize server-side push texts (**UNVERIFIED**: backend behavior).

### 10.4 Localization outside the string map (must also be ported)

| What | Where | Notes |
|---|---|---|
| `formatNextAvailableSlot(dt)` | `app_localizations.dart` | today → `"<availabilityToday> · HH:mm"`; tomorrow → `"<availabilityTomorrow> · HH:mm"`; otherwise `"<weekday> HH:mm"` using **hardcoded** weekday labels: uz `dush, sesh, chor, pay, jum, shan, yak`; ru `пн, вт, ср, чт, пт, сб, вс`; de `Mo, Di, Mi, Do, Fr, Sa, So`; en `Mon, Tue, Wed, Thu, Fri, Sat, Sun` (Mon-first) |
| Profession names | `lib/core/models/profession_model.dart` (`ProfessionData.translate`) | **English + Uzbek only**: `uz` / `uz_UZ` → Uzbek, all others → English. Handles comma-separated compound values (e.g. `Dentist, Pediatric` → `Pediatric Dentist`) through compound and part alias tables; dedupes and joins with `, `. Full table in the doctors doc. |
| Regions | `lib/core/models/region_catalog.dart` (`UzbekistanRegions.displayLabel`) | Each region has canonical, Uzbek, English and alias forms. `uz` → Uzbek; ru and others → English. **Canonical values are sent to the API.** |
| Notification titles and messages by type | `lib/features/notifications/utils/notification_localization.dart` | Maps notification `type` to keys (`notificationMessageCancelled`, …) and fills `{doctorName}` etc. See the notifications doc. |
| Relative time | `lib/features/notifications/presentation/notification_ui_helpers.dart` | < 1 min `justNow`; 1 min `minuteAgo`; < 60 min `minutesAgo` (`%s`); today `HH:mm`; yesterday `timeYesterday` (`{time}`, falls back to `yesterday HH:mm`); otherwise `DateFormat.MMM(locale) + " d • HH:mm"` |
| Tier label | `lib/core/subscription/patient_subscription.dart:62` | Hardcoded `Pro` / `Premium`. Nothing uses it. |
| Language display names | `AppLanguage.displayName` | Endonyms, not translated |
| iOS permission prompts | `ios/Runner/Info.plist` | **English only**. There are no `InfoPlist.strings` per locale. |
| Dates | `intl` `DateFormat(pattern, localeCode)` throughout | In KMP use kotlinx-datetime plus platform formatters (expect/actual), or a small formatter per locale. |

---

## 11. Connectivity — `lib/core/providers/connectivity_provider.dart`

- `connectivityListOnline(results)` = any result `!= none` (`:4-6`).
- `connectivityListProvider` (a StreamProvider) first yields `checkConnectivity()`, then every `onConnectivityChanged` event (`:9-17`).
- `isOnlineProvider` = `maybeWhen(data: online, orElse: true)`, so loading or error counts as **online** (`:19-23`).
- **Nothing in `lib/` watches these providers.** There is no offline banner or offline cache. Offline shows up only as request errors ("Connection timed out…", "Network error…").
- Test: `test/connectivity_logic_test.dart`.
- **KMP:** optional. If kept, use `ConnectivityManager.NetworkCallback` / `NWPathMonitor` via expect/actual, exposed as a `StateFlow<Boolean>` that defaults to `true`.

---

## 12. Subscription / feature gating

`lib/core/subscription/patient_subscription.dart`:
- `enum PatientTier { pro, premium }`
- `enum PatientFeature { shifaAi }`
- Backend feature name: `shifaAi` → `"PATIENT_SHIFA_AI"`. `featureBackendName` falls back to `name.toUpperCase()`.
- Minimum tier: `shifaAi` → `premium`. `tierAllows(tier, feature)` compares tier order `pro < premium`.
- `parsePatientTier(raw, {fallback = premium})`: trim and upper-case; `PRO` → pro, `PREMIUM` → premium, anything else (including null) → **premium**. A missing tier never downgrades the user.

`lib/state/subscription/patient_subscription_provider.dart`:
- `patientTierProvider` = `parsePatientTier(profile?.subscriptionTier)` (`:10-13`).
- `patientFeatureProvider(feature)` (`:17-28`):
  - If `profile.features` is **non-empty**, the result is `features.contains("PATIENT_SHIFA_AI")`.
  - Otherwise it is tier-based.

Data source: `GET /patients/me/profile` → JSON `subscriptionTier: String?` and `features: List<String>?` (`lib/core/models/patient_profile_model.dart:76-80`).

Gating points:

| Where | Logic | Source |
|---|---|---|
| Router redirect for `/shifa-ai` and `/shifa-ai/*` | Only when the profile is loaded. **Tier only**, ignores `features`. Not allowed → redirect to home. | `lib/app/router.dart:267-281` |
| Bottom bar center Shifa AI FAB | Hidden for PRO | `lib/app/persistent_bottom_bar.dart:113` |
| `ShifaAiScreen` | Defensive redirect when not allowed | `lib/features/copilot/presentation/shifa_ai_screen.dart:510-517` |

- **Quirk:** the router and the provider use different rules. A PRO user who is granted `PATIENT_SHIFA_AI` in `features` would pass the provider but be redirected by the router. **KMP:** use one `canUse(feature)` function everywhere, preferring the `features` list.
- The backend enforces the rules too (comment at `patient_subscription_provider.dart:6-9`).

---

## 13. Layout and small utilities

| Utility | Behavior | Source |
|---|---|---|
| `ResponsiveLayout` | `contentMaxWidth = 840`, `wideBreakpoint = 720`. `isWide(ctx, {breakpoint})` = width ≥ breakpoint. `horizontalInset(ctx)` = 16 (`screenPaddingH`) when width ≤ 840, else `max(16, (w − 840)/2)`. `symmetricPagePadding` wraps it. Used by the doctors screen, filter chips and filter sheet (breakpoint **640**). | `lib/core/layout/responsive_layout.dart:7-35` |
| Assets | `assets/branding/shifa_logo.svg`, `assets/branding/shifa_logo.png`, `assets/branding/Shifa AI Icon.svg` (the name contains spaces). Asset folders: `assets/images/`, `assets/branding/`. | `lib/core/constants/assets.dart`, `pubspec.yaml` |
| `parseAppointmentDateTime(s)` | Throws `FormatException` on an empty string. Trims, strips a trailing `[Zone/Id]` (regex `\[[^\]]*\]$`), parses ISO-8601 and converts to **device local** time. | `lib/core/utils/date_utils.dart:4-9` |
| `isWithinVideoJoinWindow(startAt, endAt)` | `nowUtc ∈ [start − 5 min, end + 15 min]`, inclusive | `lib/core/utils/date_utils.dart:14-21` |
| `distanceKm(lat1, lon1, lat2, lon2)` | Haversine with `p = 0.017453292519943295`, `12742 * asin(sqrt(a))`. Returns null if any argument is null. Constants: `nearbyDoctorsMaxRadiusKm = 50.0`, `nearbyDoctorsCloseRadiusKm = 15.0`. | `lib/core/utils/location_utils.dart:4-20` |
| `PasswordValidation` | min 8, max 128. Regexes: upper `[A-Z]`, lower `[a-z]`, digit `[0-9]`, special `[^A-Za-z0-9\s]`. See the next rows. | `lib/core/utils/password_validation.dart:3-53` |
| `PasswordValidation.validate` | Returns the first failing l10n key in this order: `passwordRequired` (null or empty), `passwordTooShort`, `passwordTooLong`, `passwordRequirementUppercase`, `passwordRequirementLowercase`, `passwordRequirementDigit`, `passwordRequirementSpecialChar`. Null when valid. | same |
| `PasswordValidation` checklist | Rows in order: MinLength, Uppercase, Lowercase, Digit, SpecialChar, MaxLength. Keys `passwordRequirement*`. | same |
| `showPermissionRationale({context, rationaleKey, title})` | Pre-permission explainer (Apple guideline 5.1.1(iv)). **Skipped if the translation equals the key.** A non-dismissible dialog titled `permissionNeeded` with a single `continueButton`. | `lib/core/utils/permission_rationale.dart:6-` |
| Rationale keys used | `permissionRationaleCamera` (chat camera `chat_conversation_screen.dart:167`, documents scan `documents_screen.dart:316`), `permissionRationaleMicrophone` (voice note `chat_conversation_screen.dart:241`), `permissionRationaleLocation` (`location_picker_widget.dart:453`) | |
| `TextCleaner.cleanText` (AI text) | In order: (1) join spaced-out letters: regex `\b(?:\w\s)+\w\b` with the spaces removed; (2) add a space after punctuation: `([.!?,;:])(?=\w)` → `$1 `; (3) collapse `\s+` to a single space; (4) trim | `lib/core/utils/text_cleaner.dart:11-` |
| `file_delete*.dart`, `voice_file_exists*.dart` | Conditional imports. IO: delete the file if it exists / `File.exists`. Web stub: no-op / `true`. | `lib/core/utils/` |

---

## 14. Platform configuration

### 14.1 Android (`android/`)

| Item | Value |
|---|---|
| applicationId / namespace | `com.shifa.patientapp` |
| minSdk | **24** (required by Daily) |
| compileSdk / targetSdk | Flutter defaults (`flutter.compileSdkVersion`, `flutter.targetSdkVersion`); **UNVERIFIED** exact numbers (Flutter 3.35 → 36 / 36) |
| NDK | `27.3.13750724` |
| Java / Kotlin JVM target | 11; `coreLibraryDesugaring` `com.android.tools:desugar_jdk_libs:2.0.4` |
| Extra dependency | `com.google.android.play:integrity:1.6.0` (Firebase Phone Auth / Play Integrity) |
| Gradle plugins | `com.google.gms.google-services`, `com.google.firebase.crashlytics` |
| Signing | Release reads `android/key.properties` (`<REDACTED>`); a release assemble/bundle **fails** without it. The CI-verified **upload** key SHA-1 is `E3:33:88:4E:68:DF:39:0F:F5:12:90:4C:48:A1:20:33:2C:AF:CF:D3` (`.github/workflows/google_play_deploy.yml:155`). Firebase Phone Auth also needs the Play app-signing SHA registered (**UNVERIFIED**, lives in the Firebase console). |
| MainActivity | Plain `FlutterActivity` |
| Activity | `singleTop`, `windowSoftInputMode=adjustResize`, LaunchTheme → NormalTheme |
| Application | label `Shifa Bemor`, `usesCleartextTraffic=false`, `networkSecurityConfig`, `fullBackupContent=@xml/backup_rules`, `dataExtractionRules=@xml/data_extraction_rules` |
| `<queries>` | `PROCESS_TEXT`, `VIEW geo:`, `VIEW https` (for `url_launcher`) |
| Deep links / App Links | **None** (no intent filters beyond MAIN/LAUNCHER). Notification taps are routed in-app (notifications doc). |
| google-services.json | Project `shifa-doctor-staging`, number `55173524197`, appId `1:55173524197:android:ddadc878f20da52792420c`, API key `<REDACTED>`. In CI it is decoded from a secret. |

### 14.2 iOS (`ios/`)

| Item | Value |
|---|---|
| Bundle id / team | `uz.shifa.patient` / `NH97PGA9PT` |
| Deployment target | iOS **13.0** |
| Display name / bundle name | `Shifa Bemor` / `shifa_patient_app_v1` |
| Orientations | iPhone: portrait, landscape left, landscape right. iPad: all four. |
| URL schemes (`CFBundleURLTypes`) | `com.googleusercontent.apps.55173524197-<client-suffix>` (REVERSED_CLIENT_ID) and `app-1-55173524197-ios-91a17909836d42e292420c` (encoded app id). Both are for Firebase Phone Auth reCAPTCHA return. No other deep links. |
| `UIBackgroundModes` | `fetch`, `remote-notification` |
| `ITSAppUsesNonExemptEncryption` | `false` |
| Entitlements | `aps-environment = development` (**quirk**: App Store builds re-sign to production via the provisioning profile, **UNVERIFIED**; set `production` for release in KMP) |
| Podfile | `use_frameworks!`. `post_install` sets `GCC_PREPROCESSOR_DEFINITIONS` `PERMISSION_CAMERA=1` and `PERMISSION_MICROPHONE=1` (permission_handler) and disables pod code signing. |
| AppDelegate | Standard `FlutterAppDelegate` + `GeneratedPluginRegistrant` |
| Firebase iOS appId | `1:55173524197:ios:91a17909836d42e292420c` (GoogleService-Info.plist, API key `<REDACTED>`) |

### 14.3 Web

- Firebase web appId `1:55173524197:web:964e0be5d1a5700892420c`, authDomain `shifa-doctor-staging.firebaseapp.com`, storageBucket `shifa-doctor-staging.firebasestorage.app`, API key `<REDACTED>` (`lib/firebase_options.dart:35-43`).
- `web/firebase-messaging-sw.js` is the service worker for web push.
- Compose Multiplatform wasm is optional; the Flutter web target exists but is secondary. Many services are guarded by `kIsWeb`: push init, background handler, and file stubs.

### 14.4 Permissions table

| Permission | Android manifest | iOS Info.plist key / text (English only) | Requested at runtime by | Used for |
|---|---|---|---|---|
| Internet | `INTERNET` | – | – | everything |
| Notifications | `POST_NOTIFICATIONS` (API 33+) | (APNs via entitlement) | `FirebaseMessaging.requestPermission(alert, badge, sound)` at startup (`push_notification_service.dart`); local notifications request alert/badge/sound on iOS | push, appointment reminders |
| Camera | `CAMERA` | `NSCameraUsageDescription` — "We need camera access for video consultations" | `permission_handler` camera + microphone before a video call (`lib/features/bookings/presentation/video_call_screen_mobile.dart:82-85`); `image_picker` camera in chat and document scan (after `permissionRationaleCamera`) | video calls, photos |
| Microphone | `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS` | `NSMicrophoneUsageDescription` — "We need microphone access for video consultations" | video call (above); `record` voice notes in chat and Shifa AI voice input (after `permissionRationaleMicrophone`) | video, voice messages, AI dictation |
| Location | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | `NSLocationWhenInUseUsageDescription` and `NSLocationAlwaysAndWhenInUseUsageDescription` — "We use your location to show doctors near you and for your profile address" | `Geolocator.requestPermission()` at `home_screen.dart:157`, `doctors_screen.dart:178`, `location_picker_widget.dart:459` (after `permissionRationaleLocation`) | nearby doctors, profile address |
| Photo library | (none; Android photo picker / SAF) | `NSPhotoLibraryUsageDescription` — "We need photo library access to set your profile picture" | `image_picker` gallery, `file_picker` | avatar, chat attachments, documents |
| Biometrics | (`local_auth` merges `USE_BIOMETRIC` via its own manifest, **UNVERIFIED**) | **`NSFaceIDUsageDescription` is MISSING**. Face ID crashes or falls back without it; add it in KMP. | `local_auth` in app lock | app unlock |

---

## 15. Dependencies → KMP equivalents (`pubspec.yaml`)

| Flutter package (version) | Used for (where) | KMP / Compose equivalent |
|---|---|---|
| flutter_riverpod ^2.5.1 | All state (StateNotifier, FutureProvider, StreamProvider) | `androidx.lifecycle` ViewModel (KMP) + `StateFlow`; DI with **Koin** |
| go_router ^14.0.0 | Routing, shell tabs, redirects (about 40 files) | `org.jetbrains.androidx.navigation:navigation-compose` (type-safe routes) or Voyager/Decompose; the redirect logic becomes a central `NavGuard` |
| dio ^5.4.0 | REST client (ApiClient; 12 files) | **Ktor client** (OkHttp engine on Android, Darwin on iOS) + kotlinx.serialization |
| http ^1.2.0 | Public config, geocoding, chat multipart upload, copilot API, certificate download | Same Ktor client |
| shared_preferences ^2.3.2 | Non-secure prefs (§6.3; 6 files) | `com.russhwolf:multiplatform-settings` |
| flutter_secure_storage ^9.2.2 | JWT and app-lock storage | Android EncryptedSharedPreferences / Keystore; iOS Keychain (expect/actual), or `KVault` |
| firebase_core ^3.6.0, firebase_messaging ^15.1.3 | FCM push | GitLive `firebase-kotlin-sdk` (`firebase-messaging`) or native FirebaseMessaging (Android) / Firebase iOS SDK via expect/actual; KMPNotifier is an option |
| firebase_auth ^5.3.3 (+ platform_interface ^7.7.3) | Phone OTP (SMS) verification (auth doc) | GitLive `firebase-auth` (PhoneAuthProvider) or native per platform |
| firebase_crashlytics ^4.3.10 | **Declared, never initialized** | GitLive `firebase-crashlytics` or native; actually wire it |
| flutter_local_notifications ^17.2.2 | Foreground push display, appointment reminders (channels §6.6) | Android `NotificationManagerCompat` + `AlarmManager`/WorkManager; iOS `UNUserNotificationCenter` (expect/actual); or KMPNotifier |
| flutter_timezone ^3.0.1 | Device IANA zone (profile sync, reminders) | `kotlinx-datetime` `TimeZone.currentSystemDefault().id` |
| intl ^0.20.2 | Date and number formatting (17 files) | kotlinx-datetime + platform `DateTimeFormatter`/`NSDateFormatter` (expect/actual) |
| flutter_localizations | Material/Cupertino l10n | Compose Resources `stringResource` + `values-xx` |
| equatable ^2.0.5 | Model equality | Kotlin `data class` |
| local_auth ^2.3.0 | Biometric unlock | Android `androidx.biometric` BiometricPrompt; iOS `LAContext` (expect/actual) |
| daily_flutter ^0.37.0 | Video consultations (`video_call_screen_mobile.dart`) | Daily native SDKs: Android `co.daily:client`, iOS `Daily` (SPM) via expect/actual + platform views (`AndroidView` / `UIKitView`) |
| permission_handler ^11.4.0 | Camera + mic before a call | `moko-permissions` or expect/actual |
| geolocator ^12.0.0 | Current location (home, doctors, picker) | Android FusedLocationProvider; iOS CLLocationManager (expect/actual), or `compass`/`moko-geo` |
| flutter_map ^7.0.2, latlong2 ^0.9.1 | OSM map in the location picker | Android `osmdroid` or MapLibre; iOS MapKit/MapLibre via expect/actual (MapLibre Compose is an option) |
| geocoding ^3.0.0 | **Unused** (Google HTTP geocoding is used instead) | drop |
| image_picker ^1.0.7 | Camera/gallery (chat, profile, edit profile) | Android `ActivityResultContracts.PickVisualMedia`/`TakePicture`; iOS `PHPickerViewController`/`UIImagePickerController`, or `Peekaboo` / FileKit |
| file_picker ^8.0.0 | Documents, booking attachments | FileKit (`io.github.vinceglb:filekit`) |
| flutter_image_compress ^2.1.0 | Compress before chat/doc upload | Android `Bitmap.compress`; iOS `UIImage.jpegData` (expect/actual) |
| cached_network_image ^3.3.1 | Image bubbles, doctor photos | **Coil 3** (multiplatform) |
| flutter_svg ^2.0.9 | Shifa AI nav FAB icon | Compose Resources vector drawable (convert SVG → XML) or Coil SVG |
| url_launcher ^6.2.5 | External links, phone, maps (chat, appointment details, doctor profile) | `LocalUriHandler` / Android Intent / `UIApplication.open` |
| webview_flutter ^4.10.0 | Payment pages (Click/Payme etc.) and certificate viewer | Android `WebView` (AndroidView); iOS `WKWebView` (UIKitView), or `compose-webview-multiplatform` |
| pdfx ^2.9.2 | PDF viewer (documents, certificates) | Android `PdfRenderer`; iOS `PDFKit` (expect/actual) |
| flutter_linkify ^6.0.0 | Clickable links in chat | `AnnotatedString` + `LinkAnnotation` with a URL regex |
| audioplayers ^6.0.0 | Voice-message playback | Android Media3 ExoPlayer; iOS AVPlayer (expect/actual) |
| record ^6.1.2, path_provider ^2.1.2 | Voice recording to a temp file | Android `MediaRecorder`; iOS `AVAudioRecorder`; `okio`/FileKit for paths |
| country_code_picker ^3.4.1 | Phone input country selector | Custom Compose dropdown with a static country list, or `libphonenumber` on Android |
| connectivity_plus ^7.0.0 | Online state (**unused by the UI**) | `ConnectivityManager` / `NWPathMonitor` (optional) |
| package_info_plus ^9.0.1 | Version footer | `BuildConfig.VERSION_NAME` / `NSBundle` (expect/actual) |
| cupertino_icons ^1.0.8 | Unused | drop |
| dev: mocktail ^1.0.4, flutter_test, integration_test | Tests | `kotlin.test`, MockK (Android/JVM) or fakes, Turbine for flows, Compose UI tests |
| dev: flutter_launcher_icons ^0.13.1 | Icons from `assets/branding/shifa_logo.png` (`min_sdk_android: 21`) | Android adaptive icon + iOS AppIcon asset catalog |
| dev: flutter_lints ^5.0.0 | Lints | detekt + ktlint |

---

## 16. Recommended KMP module layout

```
:composeApp                  // Android + iOS entry points, Koin start, NavHost, root lifecycle observer (§2.2)
:core:config                 // BuildKonfig: API_BASE_URL, ENVIRONMENT, GOOGLE_MAPS_API_KEY; base-URL rules (§3.2)
:core:network                // Ktor client, auth plugin, 401 policy (§4), public config (§5), ShifaException
:core:storage                // SecureStore (expect/actual), Settings (multiplatform-settings), StorageKeys object (§6)
:core:l10n                   // composeResources strings, AppLanguage, LanguageRepository (§10), error translation (§9)
:core:designsystem           // AppDesignSystem colors/typography/spacing, shared widgets
:core:common                 // date utils, password validation, distance, text cleaner, logger (§7, §13)
:core:subscription           // PatientTier/Feature + a single canUse() (§12)
:core:platform               // expect/actual: push, local notifications, biometrics, location, timezone, pdf, webview,
                             //   image picker/compress, audio record/play, video (Daily), permissions, connectivity
:feature:auth
:feature:home
:feature:bookings            // + payments, video, waiting room
:feature:doctors
:feature:documents
:feature:chat
:feature:copilot             // Shifa AI
:feature:notifications
:feature:profile
:feature:tasks
:feature:settings            // app lock
```

Each `:feature:*` has `data/` (repository, DTOs with `@Serializable`), `presentation/` (ViewModel + screens) and `di/` (Koin module). Keep the absence of a use-case layer; it mirrors the source.

Keep a single `StorageKeys` object containing **exactly** the key strings in §6. That keeps upgrades from the Flutter build compatible (with the prefix caveat in §6.6).

---

## 17. CI/CD (`.github/workflows/`, all `workflow_dispatch` / manual)

| Workflow | What it does |
|---|---|
| `flutter_ci.yml` | `dart format` check on `test/` and `integration_test/` only; `flutter analyze --no-fatal-infos --no-fatal-warnings`; `flutter test --coverage`. The project `CLAUDE.md` says it runs on push/PR; the file is manual-only. |
| `google_play_deploy.yml` | Flutter 3.35.5, Java 17. Decodes `google-services.json` and the keystore from secrets and verifies the keystore SHA-1. Build number = `19 + run_number`. `flutter build appbundle --release --dart-define=ENVIRONMENT=production --dart-define=FLAVOR=production` (**no API_BASE_URL**). Uploads `com.shifa.patientapp` to the **production** track. |
| `ios_testflight.yml` | Xcode 26.0, Flutter 3.35.5. Build number = `19 + run_number`. `flutter build ios --release --no-codesign` with the same defines, then Fastlane signs and uploads to TestFlight. |
| `web_deploy.yml` | `flutter build web --dart-define=API_BASE_URL=<secret> --dart-define=ENVIRONMENT=production`; `firebase deploy --only hosting:patient` to project `shifa-doctor-staging`. |

KMP: keep versionCode/CFBundleVersion **> 19 + last run number** so store uploads keep increasing.

---

## 18. Tests as behavioral contracts (`test/`, `integration_test/`)

| Test | Contract |
|---|---|
| `test/connectivity_logic_test.dart` | Online if any result ≠ none; `[none]` → offline |
| `test/core/layout/responsive_layout_test.dart` | Content is centered on wide screens; 16 px gutter on narrow ones |
| `test/core/models/profession_model_test.dart` | Uzbek translation; compound aliases (e.g. `Dentist, Pediatric`); English unchanged; sort order |
| `test/document_model_test.dart` | `fromJson`; `createdAt` is used when `date` is missing |
| `test/documents_notifier_test.dart` | Load success/error; delete removes the item |
| `test/empty_state_widget_test.dart` | EmptyState renders its texts |
| `test/error_localizations_test.dart` | A known message is translated (uz); empty → generic; SQL text never leaks (`SELECT` absent); `Network error: timeout` fills the placeholder; `userFriendlyError` works |
| `test/features/doctors/doctor_discovery_widgets_test.dart` | Uzbek weekday slot label; pagination bar |
| `test/features/doctors/doctors_filter_catalog_test.dart` | Regions; country filter: **Germany excludes** legacy Uzbek doctors with no country, **Uzbekistan includes** them; UzbekistanRegions labels and aliases |
| `test/localization_completeness_test.dart` | Every `en` key exists in `uz` and `ru` (**de is not checked**); a RU value identical to EN and longer than 24 characters is flagged, except `appName`, `shifaAiTitle`, `documentCategory_EEG` |
| `test/notification_tap_handler_test.dart` | Notification type → route: e.g. `APPOINTMENT_REMINDER` without `appointmentId` → null; `DOCUMENT_ACCESS_REQUEST` → null; with `documentId` → document route; `NEW_MESSAGE` + `chatId` → chat; `treatmentPlanId` → treatment plan; `PROPHYLAXIS_REMINDER` → bookings (notifications doc) |
| `test/notifications_screen_appbar_test.dart`, `test/waiting_room_screen_test.dart`, `test/widget_test.dart` | UI smoke tests |
| `integration_test/patient_app_smoke_test.dart` | The app mounts |
| `integration_test/promo_screens_capture_test.dart` | Promo screenshots (`PROMO_CAPTURE`) |

Note: `localization_completeness_test` claims every en key is in uz/ru. In fact **en itself lacks 4 keys** that the UI uses (see `strings/README.md`), so the test cannot catch them.

---

## 19. Porting notes and quirks (decide consciously)

1. **Debug builds hit the production backend** (§3.2). The KMP port should add a real dev/staging switch through BuildKonfig.
2. **`FLAVOR` is dead**, and the `CLAUDE.md` flavor table is stale (§3.1).
3. **The public config is never fetched on store builds**, because the fetch is guarded by `API_BASE_URL` being non-empty (§5). So the AI transcription-feedback button and the Google geocoding key are effectively off in the mobile store builds. The KMP port should decide whether to fetch the config from the resolved API base instead. That is a behavior change, so check with the backend team.
4. **`publicBaseUrl` is a localhost dev host** used for relative photo URLs in all builds (§3.3).
5. **Crashlytics is declared but never initialized** (§2.1).
6. Unused code: the `geocoding` package, `ApiClient.uploadFile`, `StorageService.saveUserId`/`user_id`, `isOnlineProvider`, `patientTierLabel`, `cupertino_icons`, and the `updateBackend` flag.
7. **The app-lock PIN is stored in plaintext** in secure storage (§6.2).
8. **Copilot memory survives logout**, a cross-account leak (§6.3).
9. **Missing English strings**: `discardChanges`, `noTasksFound`, `profileUpdatedSuccessfully`, `saveChanges` render as raw keys in en (and in de, which falls back to en). `ru` also lacks `noTasksFound`. The keys are used at `edit_profile_screen.dart:182, 379, 386` and `tasks_screen.dart:155`. Add English values in KMP.
10. **German covers only 489 of 834 keys.** The rest falls back to English.
11. **Subscription gating is inconsistent**: the router uses the tier and the provider uses `features` (§12).
12. **Logger quirks**: the camelCase sensitive keys never match (§7), and the FCM token is printed in debug builds.
13. **Two different "technical message" detectors** (§8 vs §9.1). The sanitizer's is broader: it matches any single SQL keyword, `Exception:` and JSON.
14. **401 handling** uses a *suffix* list to decide whether to attach the token, but a *prefix* (`/auth/`, `/public/`) to decide whether to skip logout. Keep both.
15. **Requests outside Dio** (chat upload, copilot, config, geocoding, certificate download) skip the 30 s timeouts and the 401 logout. Unify them in KMP, but keep the upload endpoints and payloads identical.
16. **No `Accept-Language` header.** The server language comes only from `PATCH /patients/me/profile {language}`, and the profile's language overrides the local choice once the profile loads (§10.3).
17. **`NSFaceIDUsageDescription` is missing**, iOS permission texts are English only, and `aps-environment = development` (§14.2, §14.4).
18. **The Firebase project is named `shifa-doctor-staging`** but serves production for all three platforms.
19. `auth_token_saved_at` is local time without an offset. Compare it as local time, or store epoch milliseconds in KMP. It only feeds the 10 s grace period.
20. The iOS Keychain survives reinstall. Keep the `has_launched_before` wipe (§2.1), and consider also wiping the app-lock keys on first launch.
21. The splash and auth check are **offline-tolerant**: only a 401 on `GET /patients/me/profile` logs the user out.
