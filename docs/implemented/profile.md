# Profile & account

## Profile (`ui/profile/ProfileScreen.kt`, PROF-01/02/07)

- Figma: `1703:16707`. Opened from the header avatar (the Account page). It shows the profile card (avatar with a camera badge, name, phone, email), Edit Profile, Settings (Language, App Lock, Change Password, Privacy) and Account (Log Out, Delete my Account). Pull to refresh reloads P1. A 401 on P1 logs out.
- No version footer: Figma has none.
- App Lock (LOCK-02) is a "Coming soon" placeholder. Privacy opens `https://shifa.uz/#privacy` in an in-app browser (`rememberInAppBrowser`: a Custom Tab on Android, `SFSafariViewController` on iOS); it did nothing in Flutter.
- The dialogs have no Figma frame and use the pop-up widget. Language is a radio list, Log out and Delete my account are confirms.
- Language (CORE-04): the choice is saved locally (`app_language`, `LanguageStorage`), then P2 `{language}` tells the backend (errors ignored). The saved choice wins, then the system language, then English. The server's language never overrides the local one, which differs from Flutter (14 §3.1). `ProvideAppLocale` switches the resources at runtime: `Locale.setDefault` + configuration on Android, `AppleLanguages` on iOS. The iOS switch hasn't been tried on a device.
- Avatar (PROF-02): the picker uploads P3 and saves the URL with P2, from the profile card or Edit profile. Home's header takes the saved profile (P2's reply) after this or an Edit profile save. No cache-buster, and the app lock isn't suspended (no LOCK-01 yet).
- Log out (PROF-07, AUTH-08): confirm → P7 with `""` → the token is wiped → Login. The logout is awaited (Flutter didn't wait). Every screen's ViewModel lives in the signed-in session's store, so no profile state survives. No PIN to clear yet (LOCK-01). The global 401 path (CORE-05) is still not built.

## Edit profile (`EditProfileScreen.kt`, PROF-03)

- Figma: `1703:17376`. Starts from the loaded profile, with no fetch. The name splits on the first space, the birth date is parsed from ISO, and an unknown gender is no selection and isn't sent (14 Q15).
- Validation (14 §7): first and last name are required (the backend only renames when both are sent). The phone is optional, else it needs at least 10 digits.
- Save sends P2 with the trimmed fields and blanks as null, without `photoUrl` (14 Q1, Q3). The language applies once the save succeeds. Discard goes back with no prompt (14 §3.2), and it is disabled while saving.
- Differences from Figma:
  - The header says "Edit Profile", where Figma says "Profile".
  - The phone is one field with the dial code; Figma's flag and code boxes need a country picker.
  - Language is a dropdown of the four app languages; Figma shows a text field with the code.
  - "Select on Map" opens a placeholder (PROF-04), so the street address is typed.

## Change password (`ChangePasswordScreen.kt`, PROF-05)

- A8 with current/new/confirm fields, sent as typed. The new password follows the registration policy and checklist, which is the same as the backend's `PasswordPolicy`. A wrong current password is a 400 "Current password is incorrect", shown as such (Flutter showed the generic "Failed to change password"), and the session stays. Success goes back to Profile with a banner. No Figma frame, so it reuses the registration page layout.

## Delete account (`DeleteAccountScreen.kt`, PROF-06)

- Confirm dialog → P5 (10 s timeout) → the code page → P6 → awaited logout → Login, which shows "account deleted".
- The code page sends A4 (`purpose: ACCOUNT_DELETION`) when it opens and on resend. The resend cooldown is the challenge's `expiresInSeconds`, not Flutter's fixed 5 minutes (14 Q17).
- Phone-only accounts can't delete: without an email the dialog stops with "email required" (14 Q16). The backend team needs to add a phone path.
- The confirm button is the primary style, not destructive, because the page layout has no destructive option. Not tried against the backend.

## Debug menu (`ui/debug/DebugScreen.kt`, debug builds only)

- Profile → Settings → "Debug menu", shown only when `isDebugBuild` (`BuildConfig.DEBUG`, `Platform.isDebugBinary`). English only; release builds have no row and ignore the saved demo flag.
- **Demo data** (`data/debug/DemoBackend.kt`): the shared `httpClient` switches to a Ktor `MockEngine` with in-memory data seeded around now:
  - a video visit whose join window is open, a REQUESTED one, an unpaid one, a cancellable one, past ones;
  - two demo doctors (a free video service, an online-priced one, a clinic-only one that B3 rejects for video, as production does), with slots 09:00 to 16:30;
  - check-ins due now for three tasks, five notifications (four unread), two chats (the doctor answers each message 4 s later), two documents, and a canned Shifa AI reply.
  - Booking, cancel, check-ins, mark read, chat, uploads and profile edits change only that data. A GET it doesn't fake goes to the real backend, and so does `auth/`; any other write gets 501 "Not available in demo mode". The data is reseeded on launch and by "Reset demo data".
  - The live video visit (9101) asks for a signature: B4 and B6 are faked; V1 stays refused, so a call stops at Join Now.
  - Switching it re-keys `App`, so every screen restarts from the splash on the other backend. The flag is kept across launches.
- **Fake pushes**: a local notification with a push's data (type, title, message and the linked id; never `notificationId`, so the tap marks nothing read), for appointment confirmed and reminder, payment due, task check-in, new document, new chat message and treatment plan. The ids are the current backend's next open visit, open task, newest document and chat. Tapping one goes through `Push.opened` like a real push. Android shares `showPush` with `ShifaMessagingService`; iOS posts a `UNNotificationRequest`, which AppDelegate shows and routes.
- ponytail: one fixed data set, and the upload's title and category are read from the multipart text. Not built: time travel and forced failures (offered, not picked).

