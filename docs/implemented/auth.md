# Launch & auth

## Splash (`ui/splash/`, AUTH-01)

- Figma: Mobile 1 `1614:12212` animates into Mobile 2 `1614:12237`. `SplashScreenContent(progress)` draws any frame in between. 0 is Mobile 1 and 1 is Mobile 2.
- The animation runs 1600 ms with `EaseInOutCubic`:
  - the two blobs move from their start to their end offset (`ShifaDimens.SplashBlob*`)
  - the tile border fades in
  - the logo tint goes from white to `Raw.SplashLogo`
- `SplashScreen` always goes to Login when it finishes. Session restore (token check → `/patients/me/profile`, 5 s fallback) is not built yet.
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
- `LoginViewModel` only validates: the identifier must not be blank, and the password must not be empty. What's not built yet:
  - identifier normalization
  - the `AuthRepository.login` call
  - persisted lockout counters and the 30 s refresh
- `loginAttemptsRemaining` and `loginLockedUntilMinutes` exist in the UI state so the banners can be previewed. Nothing sets them yet.
- Forgot password and Create account are no-op callbacks in `App.kt`.
- The language toggle and version footer are not built.
