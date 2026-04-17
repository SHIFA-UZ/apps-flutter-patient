# App Lock PIN Freeze – Root Cause and Fix

## 1. Root Cause

### What was happening

- User logs in, sets PIN, uses app, puts app in background, reopens app.
- Lock screen **appeared** (correct) but was **frozen**: no button reacted, touch events did nothing.
- After force-close, app sometimes did **not** ask for PIN (bypass).

### Why the screen froze

The lock UI was implemented as a **Stack overlay** on top of the main app:

```dart
return Stack(
  children: [
    widget.child,           // MaterialApp.router (full app)
    Positioned.fill(
      child: ColoredBox(
        color: Colors.white,
        child: AppLockScreen(...),
      ),
    ),
  ],
);
```

So when locked:

1. The **entire app** (MaterialApp.router, including Navigator and its Overlay) was still built and kept in the tree as the first child.
2. The PIN screen was the **second** child, drawn and hit-tested on top.

In Flutter, hit-testing for a Stack runs in **reverse child order** (last child first), so the overlay *should* get taps first. The freeze was still possible because:

- **Navigator/Overlay interaction**: The router’s Navigator has its own Overlay for routes and dialogs. Depending on timing (e.g. right after resume), a route or overlay entry could be updated or drawn in a way that affected hit-testing or focus.
- **Build/context timing**: `_maybeShowLockOnResumed()` is async (it awaits `isLockEnabled()` and `hasPin()` from secure storage). When `setState` ran, the overlay was built in a frame where the rest of the tree (including Navigator state) was also updating. On some devices/situations this could lead to the overlay being present but not correctly receiving pointer events (e.g. wrong hit-test target or focus).
- **No isolation**: Any widget inside the first child (e.g. a full-screen route, a dialog, or a transparent overlay) could in principle sit in the same Overlay stack or affect which widget received events. Relying on “we’re the second child so we’re on top” is fragile when the first child is a full app with its own overlay stack.

So the **root cause** was: **using an overlay (Stack) for the lock screen while the full app stayed in the tree**, which left the lock screen exposed to hit-test/overlay ordering and lifecycle timing, and could make it appear but not receive touches.

### Why PIN was sometimes bypassed

- **Resume race**: If `_maybeShowLockOnResumed` ran late or not at all (e.g. `mounted` false, or callback lost), the lock screen might never show.
- **Double scheduling**: Multiple `addPostFrameCallback` for resume could lead to overlapping async runs and inconsistent `needsUnlock` / `_showLockOverlay` state.
- **Storage delay**: If `isLockEnabled()` or `hasPin()` hung (e.g. secure storage slow on resume), the code could never reach `setState(() => _showLockOverlay = true)` or could be in an inconsistent state.

---

## 2. Code Before and After

### Before (overlay – frozen PIN possible)

**File:** `lib/core/widgets/app_lock_lifecycle_layer.dart`

- Lock shown by building a **Stack**: first child = full app, second child = full-screen overlay with PIN.
- No timeout on storage calls; no guard against double resume handling.
- No lifecycle logging.

```dart
@override
Widget build(BuildContext context) {
  if (!_showLockOverlay) return widget.child;

  return Stack(
    children: [
      widget.child,
      Positioned.fill(
        child: PopScope(
          canPop: false,
          child: ColoredBox(
            color: Colors.white,
            child: AppLockScreen(
              onUnlock: _onUnlock,
              onForceLogin: _onForceLogin,
            ),
          ),
        ),
      ),
    ],
  );
}
```

### After (root-level gate – PIN always interactive)

**File:** `lib/core/widgets/app_lock_lifecycle_layer.dart`

- When lock is required, the **whole app is replaced** by a dedicated `MaterialApp` whose only content is the PIN screen. No Stack, no overlay, no Navigator under it.
- Lock state is a single boolean `_showLockScreen`. When true, the widget tree is only: `MaterialApp → PopScope → AppLockScreen`. So nothing can be drawn or hit-tested above the PIN screen.
- Storage calls are wrapped in `Future.wait(...).timeout(_storageTimeout)` (5 seconds); on timeout we still show the lock (fail-secure).
- Resume handling is guarded with `_resumeCheckScheduled` so we don’t run multiple resume checks in parallel.
- In debug builds, lifecycle and lock decisions are logged with `[AppLockLifecycle]` for tracing.

```dart
@override
Widget build(BuildContext context) {
  if (!_showLockScreen) return widget.child;

  // Root-level gate: REPLACE entire app with PIN screen.
  final languageState = ref.watch(languageProvider);
  return MaterialApp(
    theme: AppTheme.lightTheme,
    locale: languageState.locale,
    // ... supportedLocales, localizationsDelegates
    home: PopScope(
      canPop: false,
      child: AppLockScreen(
        onUnlock: _onUnlock,
        onForceLogin: _onForceLogin,
      ),
    ),
  );
}
```

---

## 3. Exact Lines Changed

- **`lib/core/widgets/app_lock_lifecycle_layer.dart`**  
  - Replaced the entire implementation:
    - `_showLockOverlay` → `_showLockScreen` (semantic rename).
    - Added `_resumeCheckScheduled`, `_storageTimeout`, `_shouldShowLock()` with timeout.
    - Replaced overlay `Stack` + `Positioned.fill` + `ColoredBox` + `AppLockScreen` with a **full replacement** `MaterialApp(..., home: PopScope(child: AppLockScreen(...)))`.
    - Added `import 'package:flutter/foundation.dart'` for `kDebugMode`.
    - Added `import 'package:flutter_localizations/flutter_localizations.dart'` and `language_provider` / `AppTheme` / `AppLocalizations` for the gate’s `MaterialApp`.
    - In `didChangeAppLifecycleState`: set `_resumeCheckScheduled = true` before scheduling, clear in `whenComplete`.
    - In `_maybeShowLockOnColdStart` and `_maybeShowLockOnResumed`: use `_shouldShowLock()` (with timeout) instead of inline `isLockEnabled()`/`hasPin()`.
    - Wrapped all debug prints in `kDebugMode`.

---

## 4. Why the Freeze Happened

- The lock screen was **drawn** on top of the app (Stack overlay) but **event delivery** could still go to the wrong place because:
  - The full app (including Navigator/Overlay) remained in the tree and was updated on resume.
  - Hit-testing and focus can be affected by overlay order and timing when two “layers” (app + overlay) are both active.
- So the overlay was visible but, in some resume/lifecycle situations, **not the widget that received touch events**, which looked like a frozen PIN screen.

---

## 5. Why the New Architecture Prevents Freeze

1. **Single widget tree when locked**  
   When `_showLockScreen` is true, the only thing built is the gate `MaterialApp` → PIN screen. The main app (`widget.child`) is **not** in the tree. So there is no second layer, no Navigator, no Overlay under the PIN screen. There is nothing that can be drawn or hit-tested above it.

2. **No overlay at all**  
   We do not use Stack or any overlay for the lock. We **replace** the app with the PIN screen. So overlay ordering and hit-test quirks are eliminated by design.

3. **Defensive behavior**  
   - **Timeout**: Storage is not allowed to block forever; after 5 seconds we still show the lock (fail-secure).
   - **Single resume handling**: `_resumeCheckScheduled` prevents multiple concurrent resume flows and keeps `needsUnlock` / `_showLockScreen` consistent.
   - **Logging**: In debug, every lifecycle transition and “show/hide PIN” decision is logged so we can verify behavior and catch regressions.

4. **Consistent PIN after kill**  
   Cold start still runs `_maybeShowLockOnColdStart()` after the first frame and uses the same `_shouldShowLock()` (with timeout). So when the app is killed and reopened, PIN is required whenever lock is enabled and PIN is set, with no dependency on overlay or resume timing.

---

## 6. Expected Behavior After Fix

- On login → set PIN once (unchanged).
- When app goes to background → `needsUnlock` is set.
- When app returns to foreground → PIN screen is **always** shown as a **full-screen replacement** (no overlay).
- PIN screen is **fully interactive** (no freeze).
- No bypass: if lock is enabled and PIN is set, resume and cold start always show the PIN screen until the user unlocks.
- If app is killed and reopened → PIN is required again.
- Lock state is still stored securely (existing app lock service); no change to persistence.
- Navigator/stack of the main app is not touched for lock; we only swap the root content, so no stack corruption.

---

## 7. Safeguards Added

- **Timeout** on `isLockEnabled()` and `hasPin()` (5 s); on timeout we still show lock.
- **Single resume handler** via `_resumeCheckScheduled` to avoid double listeners and races.
- **Lifecycle logging** under `kDebugMode` for AppState and lock show/hide.
- **Observer cleanup** in `dispose()` (unchanged but documented).
- **Full replacement gate** so the PIN screen is the only content when locked (no overlay to block or be blocked).
