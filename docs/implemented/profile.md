# Profile & account

## Profile (`ui/profile/ProfileScreen.kt`, PROF-01/02/07)

- Figma: `1703:16707`. Opened from the header avatar (the Account page). It shows the profile card (avatar with a camera badge, name, phone, email), Edit Profile, Settings (Language, App Lock, Change Password, Privacy) and Account (Log Out, Delete my Account). Pull to refresh reloads P1. A 401 on P1 logs out.
- No version footer: Figma has none.
- App Lock (LOCK-02) and Privacy are "Coming soon" placeholders. Privacy did nothing in Flutter; it needs the real policy URL.
- The dialogs have no Figma frame and use the pop-up widget. Language is a radio list, Log out and Delete my account are confirms.
- Language (CORE-04): the choice is saved locally (`app_language`, `LanguageStorage`), then P2 `{language}` tells the backend (errors ignored). The saved choice wins, then the system language, then English. The server's language never overrides the local one, which differs from Flutter (14 §3.1). `ProvideAppLocale` switches the resources at runtime: `Locale.setDefault` + configuration on Android, `AppleLanguages` on iOS. The iOS switch hasn't been tried on a device.
- Avatar (PROF-02): the picker uploads P3 and saves the URL with P2, from the profile card or Edit profile. Home's header refreshes after the change. No cache-buster, and the app lock isn't suspended (no LOCK-01 yet).
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
