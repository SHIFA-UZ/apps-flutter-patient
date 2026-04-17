# Patient App — Full Localization Audit Report

**Goal:** 100% localization coverage for production release.  
**Scope:** All user-facing text in `lib/` (screens, widgets, dialogs, SnackBars, notifications, errors).

**Completed in this pass:**
- App title uses `l10n.appName`; init error screen uses `appInitializationError` / `pleaseRestartApp` without fallbacks.
- Router error screen uses `errorUnknownError` for null error.
- Confirm booking screen uses `date`/`time` from props, `l10n.doctor`, and `l10n.checkUp` (no hardcoded placeholders).
- New keys added: `notificationChannelName`, `notificationChannelDescription`, `notificationChannelAppointmentsName`, `notificationChannelAppointmentsDescription` (en/de/uz/ru) for future notification channel localization.
- Reference JSON files added: `assets/l10n/en.json`, `uz.json`, `ru.json` (subset; app still uses in-code Maps in `app_localizations.dart`).

---

## 1. Full Report of Untranslated / Hardcoded Strings

### 1.1 App-level

| File | Line | Original text | Suggested key | Notes |
|------|------|----------------|---------------|--------|
| `lib/app/app.dart` | 131 | `'Shifa Patient'` | `appName` | **Key exists.** Use `l10n.appName`. |
| `lib/app/app.dart` | 163 | `'App Initialization Error'` | `appInitializationError` | **Key exists.** Remove `??` fallback. |
| `lib/app/app.dart` | 168 | `'Please restart the app'` | `pleaseRestartApp` | **Key exists.** Remove `??` fallback. |

### 1.2 Placeholder / demo UI (confirm booking)

| File | Line | Original text | Suggested key | Notes |
|------|------|----------------|---------------|--------|
| `lib/features/bookings/presentation/screens/confirm_booking_screen.dart` | 51 | `'Dr John Markram'` | — | Replace with real `doctorName` from booking; do not localize placeholder. |
| `lib/features/bookings/presentation/screens/confirm_booking_screen.dart` | 65 | `'11:25 am, May 18 2025'` | — | Replace with formatted `dateTime` from booking. |
| `lib/features/bookings/presentation/screens/confirm_booking_screen.dart` | 66 | `'Check Up'` | `checkUp` | **Key exists.** Remove `?? 'Check Up'`. |

### 1.3 App Lock / PIN (fallbacks to remove)

All listed keys **already exist** in `app_localizations.dart`. Remove the `?? '...'` fallbacks and use getters/translate only.

| File | Line | Fallback text | Key to use |
|------|------|----------------|------------|
| `lib/features/settings/presentation/screens/app_lock_settings_screen.dart` | 52–61, 86–87, 96, 103, 120–121, 132, 141–142, 148–149, 157, 164, 171, 187, 197–198, 207, 221, 237, 245, 272, 275–276, 298, 310–311, 319–320, 327, 330, 504 | Various PIN/App Lock strings | `setUpPin`, `setUpPinRequired`, `setUp`, `confirmPin`, `reEnterPin`, `pinSetSuccessfully`, `pinsDoNotMatch`, `enterCurrentPin`, `incorrectPin`, `enterNewPin`, `confirmNewPin`, `pinChangedSuccessfully`, `pinLengthRequirement`, `enterPinCode`, `clearPin`, `clearPinConfirmation`, `clear`, `pinCleared`, `appLock`, `enableAppLock`, `appLockEnabled`, `appLockDisabled`, `pinCode`, `changePin`, `changePinDescription`, `clearPinDescription`, `confirm` |

### 1.4 Auth (doctor/register OTP + PIN)

| File | Line | Fallback text | Key to use |
|------|------|----------------|------------|
| `lib/features/auth/presentation/screens/doctor_otp_verify_screen.dart` | 196–197, 205, 215–216, 224–225, 234 | Set Up PIN, Enter your PIN code, Confirm PIN, Re-enter, PIN set successfully | Same as app_lock section |
| `lib/features/auth/presentation/screens/register_otp_verify_screen.dart` | 204–207, 215–216, 224–225, 243 | Same | Same |

### 1.5 Profile

| File | Line | Original / fallback | Key to use |
|------|------|----------------------|------------|
| `lib/features/profile/presentation/screens/profile_screen.dart` | 136 | `'App Lock'` | `appLock` (exists) |
| `lib/features/profile/presentation/screens/profile_screen.dart` | 449 | `'Uploading photo...'` | `uploading` (exists) |
| `lib/features/profile/presentation/screens/profile_screen.dart` | 516 | `'Photo upload endpoint not available...'` | `photoUploadEndpointNotAvailable` (exists) |
| `lib/features/profile/presentation/screens/profile_screen.dart` | 540 | `'Profile photo updated successfully'` | `profilePhotoUpdatedSuccessfully` (exists) |
| `lib/features/profile/presentation/screens/profile_screen.dart` | 554 | `'Failed to get photo URL from server'` | `failedToGetPhotoUrl` (exists) |
| `lib/features/profile/presentation/screens/profile_screen.dart` | 566 | `'Failed to upload photo'` | `failedToUploadPhoto` (exists) |
| `lib/features/profile/presentation/screens/profile_screen.dart` | 581 | `'Are you sure you want to delete your account?...'` | `deleteAccountConfirmation` (exists) |

### 1.6 Location picker

| File | Line | Original / fallback | Key to use |
|------|------|----------------------|------------|
| `lib/features/profile/presentation/location_picker_widget.dart` | 544 | `'Select location on map'` | `selectLocationOnMap` (exists) |
| `lib/features/profile/presentation/location_picker_widget.dart` | 623–624 | `'Latitude'`, `'Longitude'` | `latitude`, `longitude` (exist) |

### 1.7 Documents

| File | Line | Original / fallback | Key to use |
|------|------|----------------------|------------|
| `lib/features/documents/presentation/screens/documents_screen.dart` | 459 | `'Could not read file bytes'` | `couldNotReadFileBytes` (exists) |

### 1.8 Chat

| File | Line | Fallback | Key to use |
|------|------|----------|------------|
| `lib/features/chat/presentation/chat_conversation_screen.dart` | 147 | `'Take Photo'` | `takePhoto` (exists) |
| `lib/features/chat/presentation/chat_conversation_screen.dart` | 152 | `'Choose from Gallery'` | `chooseFromGallery` (exists) |

### 1.9 Sign appointment

| File | Line | Fallback | Key to use |
|------|------|----------|------------|
| `lib/features/bookings/presentation/screens/sign_appointment_screen.dart` | 282 | `'Confirm'` | `confirm` (exists) |

### 1.10 Router (error screen)

| File | Line | Original | Key to use |
|------|------|----------|------------|
| `lib/app/router.dart` | 192 | `'Unknown'` (for null `state.error`) | `errorUnknownError` (exists) — **fixed** |

### 1.11 Notifications (channel names — not yet localizable)

| File | Line | Original text | Suggested key | Notes |
|------|------|----------------|---------------|--------|
| `lib/core/services/push_notification_service.dart` | 167 | `'Shifa Patient Notifications'` | `notificationChannelName` | **New key.** Channel name shown in system settings. |
| `lib/core/services/push_notification_service.dart` | 168 | `'Notifications for messages, appointments, and tasks'` | `notificationChannelDescription` | **New key.** |
| `lib/core/services/local_notification_service.dart` | 86, 145 | `'Appointment Notifications'` | `notificationChannelAppointmentsName` | **New key.** |
| `lib/core/services/local_notification_service.dart` | 89, 146 | `'Notifications for appointment updates'` | `notificationChannelAppointmentsDescription` | **New key.** |

**Note:** Android notification channel names/descriptions are set at creation time. To localize them, the app must create channels (or pass names) in a context where `AppLocalizations` is available (e.g. after app start with locale). Currently they are `const` and cannot be localized without refactor.

### 1.12 Exception messages (thrown and later shown via translateError/userFriendlyError)

These are not shown raw to users if the app uses `userFriendlyError` / `translateError` everywhere. Listed for completeness; no UI change if error handling is consistent.

| File | Line | Text in Exception |
|------|------|-------------------|
| `lib/features/auth/data/auth_repository.dart` | 67, 70, 84, 149, 168, 223, 281 | No token received, Invalid response, Login failed, etc. |
| `lib/features/chat/presentation/chat_conversation_screen.dart` | 207, 211, 286, 290 | Upload failed, No URL in upload response |
| `lib/features/chat/services/file_upload_service.dart` | 65, 67, 119 | Upload failed, Error uploading file |

---

## 2. Suggested Localization Keys (feature-based)

Existing keys in the app already follow a flat structure (`setUpPin`, `appLock`, `failedToUploadPhoto`). For **new** keys only:

| Key | English | Use |
|-----|---------|-----|
| `notificationChannelName` | Shifa Patient Notifications | FCM/local channel name |
| `notificationChannelDescription` | Notifications for messages, appointments, and tasks | FCM channel description |
| `notificationChannelAppointmentsName` | Appointment Notifications | Local notification channel name |
| `notificationChannelAppointmentsDescription` | Notifications for appointment updates | Local notification channel description |
| `errorUnknownError` | Unknown error | Router/fallback when error is null (already exists) |

All other strings in this report already have keys; the fix is to **remove fallbacks** and use existing getters/translate.

---

## 3. Localization File Format (reference)

The app currently uses **in-code Maps** in `lib/core/localization/app_localizations.dart` (en, de, uz, ru), not JSON/ARB. For a future migration to JSON/ARB, below is an equivalent structure for **new and critical** keys only.

### en.json (excerpt — new keys only)

```json
{
  "appName": "Shifa Patient",
  "appInitializationError": "App Initialization Error",
  "pleaseRestartApp": "Please restart the app",
  "notificationChannelName": "Shifa Patient Notifications",
  "notificationChannelDescription": "Notifications for messages, appointments, and tasks",
  "notificationChannelAppointmentsName": "Appointment Notifications",
  "notificationChannelAppointmentsDescription": "Notifications for appointment updates",
  "errorUnknownError": "Unknown error"
}
```

**Note:** The app currently uses in-code Maps in `lib/core/localization/app_localizations.dart` (en, de, uz, ru). Reference JSON files with a subset of keys are in `assets/l10n/en.json`, `uz.json`, `ru.json`. For a full migration to JSON/ARB, export all keys from `_localizedValues`.

### uz.json (excerpt)

```json
{
  "appName": "Shifa Patient",
  "appInitializationError": "Ilova ishga tushirishda xato",
  "pleaseRestartApp": "Iltimos, ilovani qayta ishga tushiring",
  "notificationChannelName": "Shifa Patient bildirishnomalari",
  "notificationChannelDescription": "Xabarlar, uchrashuvlar va vazifalar haqida bildirishnomalar",
  "notificationChannelAppointmentsName": "Uchrashuv bildirishnomalari",
  "notificationChannelAppointmentsDescription": "Uchrashuv yangilanishlari haqida bildirishnomalar",
  "errorUnknownError": "Noma'lum xato"
}
```

### ru.json (excerpt)

```json
{
  "appName": "Shifa Patient",
  "appInitializationError": "Ошибка инициализации приложения",
  "pleaseRestartApp": "Пожалуйста, перезапустите приложение",
  "notificationChannelName": "Уведомления Shifa Patient",
  "notificationChannelDescription": "Уведомления о сообщениях, приёмах и задачах",
  "notificationChannelAppointmentsName": "Уведомления о приёмах",
  "notificationChannelAppointmentsDescription": "Уведомления об изменениях приёмов",
  "errorUnknownError": "Неизвестная ошибка"
}
```

---

## 4. Refactoring Suggestions (Flutter widgets)

### 4.1 Use existing keys without fallbacks

**Pattern to remove:** `l10n.someKey ?? 'Fallback'`  
**Replace with:** `l10n.someKey` or `l10n.translate('someKey')` (and ensure key exists in all locales).

### 4.2 App title

**FROM:**  
`title: 'Shifa Patient',`  

**TO:**  
`title: AppLocalizations.of(context)!.appName,`  
(or pass `appName` from a parent that has context).

### 4.3 App init error screen

**FROM:**  
`AppLocalizations.of(context)?.translate('appInitializationError') ?? 'App Initialization Error'`  

**TO:**  
`(AppLocalizations.of(context) ?? AppLocalizations(const Locale('en'))).translate('appInitializationError')`  

Same for `pleaseRestartApp`.

### 4.4 Confirm booking screen

**FROM:**  
`const Text('Dr John Markram', ...)` and `const Text('11:25 am, May 18 2025', ...)`  

**TO:**  
Use real booking data: `Text(doctorName)` and `Text(formatDateTime(dateTime))`. If the screen is still demo-only, use placeholder keys, e.g. `l10n.translate('bookingDemoDoctor')` / `l10n.translate('bookingDemoDateTime')` and add those keys only if you keep demo data.

### 4.5 Notification channels

- **Option A:** Add keys `notificationChannelName`, `notificationChannelDescription`, etc. Resolve them when initializing notifications (e.g. in a method that has access to `BuildContext` or `Locale`) and pass the resolved strings into the service.
- **Option B:** Keep channel names in English for system settings (many apps do) and document that in the audit.

---

## 5. Notifications (Firebase / local / push)

- **In-app notification list and dialogs:** Already use `NotificationLocalization.getTitle` / `getMessage(l10n)` — localized.
- **FCM foreground:** Title/body come from backend or from the same localization when showing local notifications.
- **Channel names/descriptions:** Currently hardcoded in `push_notification_service.dart` and `local_notification_service.dart`; see §1.11 and §4.5.
- **Payload parsing:** No user-facing literal strings in payload parsing; navigation uses route/IDs. No change needed for localization.

---

## 6. Architecture / Improvements

1. **Single source of truth:** Keep using `AppLocalizations` and `translate()`; avoid duplicate fallbacks so missing keys are visible in development.
2. **Remove `?? '...'` fallbacks** where the key exists in all locales so that missing translations are caught.
3. **Notification channels:** If you need localized channel names, initialize channels (or set their names) after app start with current `Locale` and pass localized strings into the notification service.
4. **Tests:** Add a test that loads all keys for `en` and checks that no key is empty or equals the key name (catches missing translations).
5. **Optional migration:** If you later move to ARB/JSON, use the same key names as in `app_localizations.dart` to keep refactors minimal.

---

## 7. Summary Checklist

- [x] App title → use `appName`
- [x] App init error → use `appInitializationError`, `pleaseRestartApp` (no fallbacks)
- [ ] All app lock/PIN screens → remove `?? '...'` fallbacks (keys exist)
- [ ] Profile screen → remove fallbacks for uploading, photo, delete account (keys exist)
- [ ] Location picker, documents, chat, sign appointment → use existing keys without fallbacks
- [x] Confirm booking → use `date`/`time` from props, `l10n.doctor`, `l10n.checkUp`
- [x] Router error → use `errorUnknownError` for null error
- [ ] Notification channel names/descriptions → add keys and optional runtime localization

**Total:** Most strings are already localized; the main work is removing fallbacks and fixing the few remaining hardcoded spots (app title, confirm booking placeholders, notification channels, router error).
