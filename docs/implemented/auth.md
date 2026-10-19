# Launch & auth

## Splash (`ui/splash/`, AUTH-01)

- Figma: Mobile 1 `1614:12212` animates into Mobile 2 `1614:12237`. `SplashScreenContent(progress)` draws any frame in between. 0 is Mobile 1 and 1 is Mobile 2.
- The animation runs 1600 ms with `EaseInOutCubic`:
  - the two blobs move from their start to their end offset (`ShifaDimens.SplashBlob*`)
  - the tile border fades in
  - the logo tint goes from white to `Raw.SplashLogo`
- `SplashScreen` runs `AuthRepository.restoreSession()` during the animation, then goes to Home or Login (05 §2.1):
  - On the first launch (`has_launched_before` not set), the token is wiped. The iOS Keychain survives reinstalls.
  - No token → Login, without a request.
  - A token → `GET /patients/me/profile`. Only a 401 clears it and goes to Login. Offline or a 5xx goes to Home (quirk Q18, kept on purpose). The AUTH-01 ticket says airplane mode goes to Login, which conflicts with 05 §2.1; this follows the spec.
  - If the check takes longer than 5 s, it goes to Login.
- Native launch screens show Mobile 1's first frame, so the hand-off doesn't flash:
  - Android: `res/drawable/splash_background.xml`, a layer-list with `splash_logo.xml`.
  - iOS: `LaunchScreen.storyboard` background plus `LaunchLogo.imageset`.
  - The Android 12+ system splash (`windowSplashScreen*`) is not themed.
- Differences from Figma:
  - The grain texture is baked into the blob colour.
  - Blobs use a radial-gradient fade instead of a real blur.
  - The tile has no backdrop blur.

## Login (`ui/login/`, AUTH-02)

- Figma: get-started `1614:12247`. It adds features the design doesn't show but the spec asks for (05 §3.2):
  - an "Email or phone" label
  - a password eye (`ShifaInputField.trailingIcon`)
  - a "Forgot password?" tertiary button
  - lockout banners (`ShifaBanner`): red when locked, orange for the attempts countdown. There is no Figma component.
- "Continue with Google" is hidden until the backend supports it.
- `LoginViewModel.signIn()` checks both fields are non-empty, then calls `AuthRepository.login` (`POST /auth/login?app=patient`). On success the token is saved to secure storage and `signedIn` fires once, which goes to Home.
- Identifier normalization (`loginUsername`): anything with `@` or a letter is sent as typed; phones become `+<digits>`, and a 9-digit local number gets `+998`. This fixes quirk Q8, where the Flutter app sent `901234567` as `+901234567`.
- Lockout (`data/auth/LoginAttempts.kt`, 05 §6.2): 5 failures lock sign in for 5 minutes. The counters are kept in plain Settings (`login_failed_attempts`, `login_lockout_until_epoch_ms`), so a restart doesn't reset them. While locked, the screen refreshes the minutes every 30 s. A network error doesn't count as a failure (fixes quirk Q11).
- Errors show as a red `ShifaBanner` above the fields until the next edit. The Flutter app uses a SnackBar. Backend messages go through `sanitizeError` (01 §8): only known-safe strings pass, the rest fall back to a status default (401 "Invalid credentials", 404, 429) or "Login failed".
- `forcePasswordReset` is parsed but not acted on until AUTH-07 exists.
- "Forgot password?" opens Forgot password. Create account opens Register.
- The language toggle and version footer are not built.

## Registration (`ui/register/`, AUTH-03/04/05)

- Figma: create-account `1631:10131`, account-information `1631:10296`, profile-image `1631:10449`. The code screen has no frame; it uses the same page layout.
- One `RegisterViewModel` and `RegisterScreen` drive every step (`RegisterStep`). System back and the header arrow go one step back; from step 1 they return to Login. Leaving drops everything, the password included (05 §6.5).
- Step 1: name, surname, phone, "Email (Optional)", password and confirm, with the live password checklist (05 §6.3). Errors show after the first Next. Rules (05 §3.3):
  - Phone or email is required. A non-Uzbek phone needs an email, because SMS only goes to +998.
  - Phones are normalized like Login (`data/auth/Phone.kt`): 9 digits get `+998`.
  - Names are checked trimmed, and the email regex takes any TLD length. Both fix Q19.
  - Next calls check-identifier (A2): a patient gets the "already exists" dialog; a doctor gets the "existing doctor" dialog; none (or a 404) goes on.
- Step 2: date of birth (Material date picker, no future dates), country, address. All optional.
  - Country: every ISO country from the OS (`utils/Country.kt`, expect/actual), since there is no country endpoint (15: local catalogs). The name shows in the device language; P2 gets the English name (TR stays "Turkey", like 07 appendix C). Uzbekistan comes first. The closed dropdown opens a searchable `CountryTray` in a bottom sheet, because ~250 rows don't fit the inline menu.
- Step 3: photo from the platform picker. On iOS it's `PHPickerViewController` (no permission prompt), re-encoded as JPEG. Optional.
- Next on step 3 sends the code (05 §2.4 steps 5–6):
  - With an email → A4 email code.
  - Without one → A5 SMS. A 503 "SMS could not be sent", a 429, a server reply of `channel=email`, or a 4th SMS open the "SMS unavailable" dialog, which asks for an email. Only its buttons close it. The Flutter app waited for an email after `channel=email` even when it didn't know one; here the user is asked for it.
  - The SMS count is per registration, not persisted.
- Code screen: 6 digits only (non-digits are dropped), the masked destination, a 300 s `m:ss` resend cooldown, and "code sent again" after a resend. Verify calls A7 with `emailOtp` or `smsOtp`, saves the token, then goes to Home.
- After A7, the photo goes to P3 (`patients/me/photo`, field `file`) and step 2 goes to P2 (`PATCH patients/me/profile`, only the set keys). A7 has `birthDate`/`address` keys, but the Flutter UI never sent them, so they're unverified. Both saves are best effort: a failure is dropped silently. P4 is not used.
- Doctor link (05 §2.5): "Create patient account" skips steps 2–3 and sends an email code to the typed email, then A6. A phone-only doctor match shows "email required" (Q7). The dialog copy still says the code goes "to your phone"; that's the spec string.
- Not built: the `PhoneInputField` country picker. No PIN setup prompt: app lock is dropped (the user's call, 2026-10-19). It's a plain phone field, which avoids bug #9.
- Differences from Figma: the sub-logo glass tile is drawn as a surface tile with a faint teal border. "Continue with Google" is hidden.

## Forgot password (`ui/forgotpassword/`, AUTH-06)

- No Figma frames: the three steps (05 §3.7–3.9) reuse the register page layout, sub-logo, fields, banners, code entry and password checklist.
- Step 1: email or phone. Emails are lowercased, phones normalized like Login. Both are validated before sending (Flutter sent anything non-empty, Q8). Send calls A9 with `identifier` and `app=patient`; the reply's `channel` (default email) picks the message: SMS, "sent to <email>", or "sent to the email on your account" for a phone.
- Step 2: 6 digits, a 60 s resend cooldown, and a 10:00 `m:ss` expiry countdown that turns red in the last minute (display only). There is no verify call, so the code is only checked by A10.
- Step 3: new password with the register checklist and rules, plus confirm. The password is sent as typed (Flutter trimmed it here, Q10).
- A10 sends the identifier under its own key (`email` or `phone`) and the code under the channel's (`emailOtp` or `smsOtp`). Flutter sent a phone as `email` (Q13). The backend's handling of phone + `emailOtp` is unverified. The returned token is saved and the app goes to Home.
- A code error from A10 goes back to step 2 with the code cleared (Q12). Other errors stay on step 3.
- Back walks the steps; back from step 2 clears the code and timers; from step 1 it returns to Login. Leaving drops everything.
- Not built: the `passwordResetSuccess` SnackBar (needs a snackbar host on Home). Not yet tried against the backend.

## Home placeholder (`ui/home/`)

- `HomeScreen` shows "Home" and a Log out button until HOME-01 is built. Log out clears the token and goes back to Login; there is no logout endpoint (05 §2.8). The FCM token reset and the Copilot memory wipe come with their features.
