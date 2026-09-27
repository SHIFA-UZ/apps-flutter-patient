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
  - lockout banners: red when locked, orange for the attempts countdown. They are styled like `ShifaNotification`; there is no Figma component.
- "Continue with Google" is hidden until the backend supports it.
- `LoginViewModel.signIn()` checks both fields are non-empty, then calls `AuthRepository.login` (`POST /auth/login?app=patient`). On success the token is saved to secure storage and `signedIn` fires once, which goes to Home.
- Identifier normalization (`loginUsername`): anything with `@` or a letter is sent as typed; phones become `+<digits>`, and a 9-digit local number gets `+998`. This fixes quirk Q8, where the Flutter app sent `901234567` as `+901234567`.
- Lockout (`data/auth/LoginAttempts.kt`, 05 §6.2): 5 failures lock sign in for 5 minutes. The counters are kept in plain Settings (`login_failed_attempts`, `login_lockout_until_epoch_ms`), so a restart doesn't reset them. While locked, the screen refreshes the minutes every 30 s. A network error doesn't count as a failure (fixes quirk Q11).
- Errors show as a red inline banner above the fields until the next edit. The Flutter app uses a SnackBar. Backend messages go through `sanitizeError` (01 §8): only known-safe strings pass, the rest fall back to a status default (401 "Invalid credentials", 404, 429) or "Login failed".
- `forcePasswordReset` is parsed but not acted on until AUTH-07 exists.
- Forgot password and Create account are no-op callbacks in `App.kt`.
- The language toggle and version footer are not built.

## Home placeholder (`ui/home/`)

- `HomeScreen` shows "Home" and a Log out button until HOME-01 is built. Log out clears the token and goes back to Login; there is no logout endpoint (05 §2.8). The FCM token reset and the Copilot memory wipe come with their features.
