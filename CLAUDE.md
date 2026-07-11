# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Shifa Patient App** — a Flutter mobile application for patients to interact with the Shifa healthcare platform. Uses a modified Clean Architecture (data/providers/presentation — no domain/usecase layer), Riverpod for state management, and GoRouter for navigation.

## Tech Stack

- **Flutter/Dart** (SDK: ^3.9.2)
- **State Management**: Riverpod (`flutter_riverpod: ^2.5.1`) — `StateNotifier` for complex state, `FutureProvider`/`StreamProvider` for async data
- **Navigation**: GoRouter (`go_router: ^14.0.0`) with `StatefulShellRoute.indexedStack` for persistent bottom nav
- **HTTP Client**: Dio (`dio: ^5.4.0`) with JWT auth interceptor, 30s timeout
- **Backend**: Spring Boot REST API (Railway production)
- **Firebase**: Auth (`firebase_auth: ^5.3.3`), FCM (`firebase_messaging: ^15.1.3`), Crashlytics (`firebase_crashlytics: ^4.3.10`)
- **Video Calls**: Daily Flutter (`daily_flutter: ^0.37.0`)
- **Maps**: Flutter Map (`flutter_map: ^7.0.2`) + geocoding
- **Localization**: 4 languages (en, uz, de, ru) via custom `AppLocalizations` delegate
- **Storage**: `SharedPreferences` for preferences, `FlutterSecureStorage` for tokens (Keychain/Keystore)
- **Biometrics**: `local_auth` for app lock (PIN + biometric)
- **Media**: `record: ^6.1.2` (audio recording), `audioplayers: ^6.0.0` (playback), `flutter_image_compress: ^2.1.0`
- **Documents**: `pdfx: ^2.9.2` (PDF viewer), `file_picker: ^8.0.0`

## Architecture

### Directory Structure

```
lib/
├── main.dart                   # Entry point, Firebase init, first-launch keychain clear
├── app/
│   ├── app.dart               # MaterialApp, lifecycle observer, auth callbacks
│   ├── router.dart            # GoRouter config (549 lines), auth guards
│   ├── main_shell.dart        # Shell routing wrapper
│   └── persistent_bottom_bar.dart
├── core/
│   ├── config/                # AppConfig (env URLs, build flags)
│   ├── constants/             # Assets paths only
│   ├── localization/          # AppLocalizations (custom delegate, 4 locales)
│   ├── models/                # 13 shared models (Equatable, fromJson/toJson)
│   ├── network/               # ApiClient (Dio wrapper), api_providers.dart
│   ├── providers/             # language_provider, connectivity_provider
│   ├── services/              # 10 services (push, local notif, daily video, app lock, geocoding)
│   ├── theme/                 # AppTheme + AppDesignSystem (Material 3, teal primary #00BBB0)
│   ├── utils/                 # 9 utilities (logger, storage, password validation, date)
│   └── widgets/               # 18 reusable widgets (buttons, cards, avatars, app lock)
└── features/
    ├── auth/                  # 2 repos, 4 providers, 13 screens
    ├── bookings/              # 2 repos, 2 providers, 10 screens (incl. video call, waiting room)
    ├── chat/                  # 1 repo, domain models, 2 screens, 6 message widgets, 2 services
    ├── doctors/               # 2 repos, 2 providers, 2 screens
    ├── documents/             # 1 repo, 1 provider, 3 screens (incl. PDF viewer)
    ├── home/                  # presentation only (1 screen, no data/providers)
    ├── notifications/         # 1 repo, 1 provider, 1 service, notification localization
    ├── profile/               # 2 repos, 1 provider, 3 screens, location picker
    ├── settings/              # presentation only (app lock settings screen)
    └── tasks/                 # 1 repo, 1 provider, 2 screens
```

### Feature Structure Pattern

Each feature follows this pattern (no domain/usecase layer):

```
feature/
├── data/              # Repositories (API calls via ApiClient)
├── providers/         # Riverpod providers (StateNotifier, FutureProvider)
└── presentation/      # Screens and feature-specific widgets
```

**Exceptions**: `home/` and `settings/` have presentation only. `chat/` uniquely has a `domain/` folder with `chat_models.dart`.

### Riverpod Patterns Actually Used

| Pattern | Usage | Example |
|---------|-------|---------|
| `StateNotifierProvider` | Complex state with loading/error | `authStateProvider`, `bookingsProvider` |
| `FutureProvider` | One-shot async data | `conversationsProvider` |
| `FutureProvider.family` | Parameterized async | `conversationProvider(id)` |
| `StreamProvider` | Continuous data (polling) | `unreadCountProvider` (10s poll) |
| `Provider` | Singletons, repositories | `apiClientProvider`, all `*RepositoryProvider` |
| `StateProvider` | Simple mutable state | `optimisticMessagesProvider` |

**Important**: This codebase uses `StateNotifier` exclusively — NOT `AsyncNotifier` or `Notifier`. All async state (loading, error, data) is managed manually in state classes with `copyWith`.

### State Class Pattern

```dart
class FeatureState {
  final List<Model> items;
  final bool isLoading;
  final String? error;
  // copyWith pattern
}

class FeatureNotifier extends StateNotifier<FeatureState> {
  Future<void> loadData() async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final data = await _repository.getData();
      state = state.copyWith(items: data, isLoading: false);
    } catch (e) {
      state = state.copyWith(error: e.toString(), isLoading: false);
    }
  }
}
```

### Repository Injection Pattern

```dart
final featureRepositoryProvider = Provider<FeatureRepository>((ref) {
  return FeatureRepository(ref.watch(apiClientProvider));
});

final featureProvider = StateNotifierProvider<FeatureNotifier, FeatureState>((ref) {
  return FeatureNotifier(ref.watch(featureRepositoryProvider));
});
```

### Navigation Architecture

- **Router**: `lib/app/router.dart` — GoRouter with auth redirect guard
- **Persistent tabs** (StatefulShellRoute.indexedStack, preserves state): Home, Bookings, Documents, Doctors
- **Non-persistent tabs** (ShellRoute): Account, Chat, Notifications, Tasks
- **Auth guard**: redirects unauthenticated users to login, handles force-password-reset, swallows Firebase reCAPTCHA callbacks
- **Complex objects between routes**: use `state.extra` (not path/query params)

### API Client Behavior

- Adds `Authorization: Bearer <token>` header automatically
- **401**: clears token, triggers logout (but NOT on `/public/*` or `/auth/*` endpoints)
- **403**: does NOT logout (valid token, wrong role)
- **Grace period**: 10 seconds after token save before 401 triggers logout
- **Rate limit**: max 1 logout per 5 seconds
- **Timeout**: 30 seconds (connect + receive)

### Model Pattern

Most models in `core/models/` use:
- `Equatable` for value equality (except `ProfessionModel` and task models)
- `factory Model.fromJson(Map<String, dynamic>)`
- `Map<String, dynamic> toJson()`
- `@override List<Object?> get props`
- Null-safe field defaults

## Build Flavors

| Flavor | API URL | Command |
|--------|---------|---------|
| **production** | `https://shifa-doc-backend-mvp-production.up.railway.app/api` | `--dart-define=FLAVOR=production` |
| **debug** | `http://localhost:8080/api` | Default (no flag) |
| **qa** | `http://localhost:9090/api` | `--dart-define=FLAVOR=qa` |

Config in `lib/core/config/app_config.dart`. Android emulator auto-converts `localhost` → `10.0.2.2`. Override with `--dart-define=API_BASE_URL=https://custom.com`.

## Common Commands

```bash
flutter pub get                                    # Install dependencies
flutter run -d ios                                 # Run (debug flavor, localhost:8080)
flutter run --dart-define=FLAVOR=production         # Run with production backend
flutter test                                       # Run all tests
flutter test test/specific_test.dart               # Run specific test
flutter analyze                                    # Lint/analyze
flutter clean                                      # Clean build artifacts
flutter build apk --release --dart-define=FLAVOR=production  # Build Android APK
flutter build ios --dart-define=FLAVOR=production   # Build iOS
```

## CI/CD

- **flutter_ci.yml**: Runs on push/PR to main — format check (test/ only), `flutter analyze` (errors only), `flutter test --coverage`
- **ios_testflight.yml**: Manual dispatch — builds and deploys to TestFlight (Xcode 26.0, Flutter 3.35.5)

## Testing

9 test files in `test/` using `flutter_test` + `mocktail`. Coverage is minimal. When writing tests:
- Widget tests for critical UI flows
- Unit tests for providers and repositories
- Mock `ApiClient` and repositories with `mocktail`
- Integration tests go in `integration_test/`

## Common Gotchas

1. **Android emulator localhost**: Use `10.0.2.2` — handled automatically by `ApiClient.apiBaseUrl`
2. **401 vs 403**: ApiClient logs out on 401 (invalid token) but NOT on 403 (wrong role)
3. **Tab state preservation**: Only Home, Bookings, Documents, Doctors preserve state (indexedStack). Other tabs rebuild on navigation
4. **Translations**: Every key MUST exist in all 4 language maps (en, uz, de, ru) or you get null errors
5. **Firebase Phone Auth**: Emulators may fail — use test phone numbers or physical device
6. **JWT grace period**: 10-second window after login before 401 can trigger logout (prevents race conditions)
7. **First launch keychain clear**: `StorageService` clears auth tokens on first launch after reinstall (iOS Keychain persists across reinstalls)
8. **Router Firebase callbacks**: Router swallows `/link` and `/__/auth` paths (iOS Safari reCAPTCHA)

---

## Commit Permission Rule

**NEVER run `git commit` (or `git push`) without explicit permission from the user for that specific change.** Make and stage the edits, summarize what changed, and wait for the user to say to commit. Do not commit as part of completing a task unless the user asked you to commit. This applies even when the user approved a plan or the code changes — approval to write code is NOT approval to commit. When in doubt, stop and ask.

## Git Branching Rule

**NEVER commit and push directly to `main`.** Before committing and pushing changes, check the current branch. If on `main`:

1. Create a new branch named after the feature/fix being developed (e.g., `feature/add-appointment-reminders`, `fix/login-401-handling`, `refactor/chat-providers`).
2. Stage and commit the changes on the new branch.
3. Push the new branch to the remote with `-u` to set upstream tracking.

This applies to all changes — features, bug fixes, refactors, docs, etc. The `main` branch should only receive changes via merged pull requests.

## Commit Message Convention

Follow the **Conventional Commits** format:

```
<type>(<scope>): <short summary>

<optional body — explain WHY, not WHAT>
```

### Types

| Type | When to use |
|------|-------------|
| `feat` | New feature or capability |
| `fix` | Bug fix |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `style` | Formatting, missing semicolons, etc. (no logic change) |
| `docs` | Documentation only |
| `test` | Adding or updating tests |
| `chore` | Build, config, dependencies, CI changes |
| `perf` | Performance improvement |

### Scope

Use the feature module or area affected: `auth`, `bookings`, `chat`, `doctors`, `documents`, `home`, `notifications`, `profile`, `settings`, `tasks`, `core`, `router`, `l10n`, `theme`.

### Rules

1. **Summary line**: imperative mood, lowercase, no period, max 72 characters (e.g., `feat(bookings): add appointment reminder notifications`).
2. **Body** (optional): explain the motivation or context behind the change. Wrap at 72 characters.
3. **Breaking changes**: add `!` after type/scope (e.g., `feat(auth)!: switch to OAuth2 flow`) and explain in the body.
4. **Multiple scopes**: if a change spans many areas, use the most relevant scope or omit it (e.g., `refactor: migrate providers to AsyncNotifier`).

### Examples

```
feat(doctors): add search by specialty filter

fix(auth): handle expired refresh token without logout loop

refactor(bookings): extract time slot selection into separate widget

docs: update CLAUDE.md with commit message convention

chore: bump flutter_riverpod to 2.6.0
```

---

## Best Practices

These rules apply every time code is written or modified in this project.

### Dart & Flutter

1. **Use `const` constructors** wherever possible — widgets, default values, static instances. Dart's `const` enables compile-time constants and widget tree optimization.
2. **Prefer `final`** for all local variables and fields that are assigned once. Never use `var` when `final` works.
3. **Use named parameters** for functions with more than 2 parameters. Always mark required params with `required`.
4. **Avoid `dynamic`**. Always specify types explicitly. If a type is truly unknown, prefer `Object?` over `dynamic`.
5. **Null safety**: never use `!` (bang operator) without verifying the value is non-null. Prefer null-aware operators (`?.`, `??`, `?..`) and early returns.
6. **String interpolation**: use `'$variable'` and `'${expression}'` instead of concatenation.
7. **Collections**: use collection literals (`[]`, `{}`, `<Type>[]`) instead of constructors. Use `...` spread and `if`/`for` inside collection literals.
8. **Imports**: use **package imports** everywhere in `lib/` (e.g., `import 'package:shifa_patient_app_v1/core/models/user_model.dart'`). Do NOT use relative imports — the entire codebase uses package imports consistently.
9. **File naming**: `snake_case.dart` for all files. One public class per file. File name matches the primary class.
10. **No code generation**: this project does NOT use `build_runner`, `freezed`, `json_serializable`, or `@riverpod` annotations. Do not introduce `.g.dart` or `.freezed.dart` files.

### Riverpod (Project-Specific)

1. **Follow existing pattern**: use `StateNotifier` + `StateNotifierProvider` for complex state with loading/error. Do NOT introduce `AsyncNotifier`, `Notifier`, or code-gen (`@riverpod`) — the codebase doesn't use them.
2. **No `autoDispose`**: this codebase does not use `autoDispose` on any providers. Do not introduce it without explicit request.
3. **Provider location**: feature-specific providers go in `features/<feature>/providers/`. Global providers go in `core/providers/`.
4. **Repository injection**: always inject repositories via `Provider`, then pass to notifiers via `ref.watch(repositoryProvider)`.
5. **Use `ref.watch`** in `build()` methods to reactively rebuild. Use `ref.read` in callbacks and event handlers (never `ref.watch` outside `build`).
6. **Use `ref.listen`** for side effects (showing snackbars, navigation) — never trigger side effects in `build()`.
7. **FutureProvider.family** for parameterized one-shot async data (e.g., fetching a single entity by ID).
8. **StreamProvider** for continuous data (polling, real-time updates). Use manual `while(true)` + `yield` + `Future.delayed` for polling.
9. **State classes** must have `copyWith` method. Include `isLoading`, `error`, and the data fields. Initialize with `isLoading: false, error: null`.
10. **Cleanup**: use traditional `dispose()` in `ConsumerStatefulWidget` for cancelling timers, disposing controllers, and closing streams. The codebase does NOT use `ref.onDispose` — all cleanup is in widget `dispose()` methods.

### GoRouter Navigation

1. **Route paths**: lowercase, hyphen-separated (e.g., `/create-account`, `/forgot-password-otp`). Use path params for IDs (`/doctors/:id`, `/bookings/:id`).
2. **`context.go`** for replacing the current route (tab navigation). **`context.push`** for stacking routes (detail screens, flows).
3. **Complex objects**: pass via `state.extra`, never serialize to query params.
4. **Auth guard**: all new routes must be added to the allowed routes list in `router.dart`'s `redirect` function, or they will redirect to home/login.
5. **Shell routes**: if the new screen needs the bottom nav bar, nest it under the appropriate shell route in `router.dart`.
6. **Router from providers**: use `ref.read(routerProvider)` to access the router from providers (not `GoRouter.of(context)`).

### API & Networking

1. **Always use `ApiClient`** from `core/network/api_client.dart`. Never create raw Dio instances.
2. **Repository pattern**: all API calls live in `features/<feature>/data/<feature>_repository.dart`. Never call `ApiClient` directly from widgets or providers.
3. **Error handling in repositories**: catch generic `Exception`, check if it's `DioException` to extract `e.response?.data?['message']`, then re-throw `Exception` with a user-friendly message. Let the provider's `copyWith(error: e.toString())` handle state updates.
4. **Response parsing**: always check response type before casting. Use `response.data is List` or `response.data is Map` guards.
5. **File uploads**: use `apiClient.uploadFile()` method — handles multipart form data.
6. **Endpoint paths**: start with `/` but do NOT include `/api` prefix (ApiClient adds it). E.g., use `/patients/me/profile`, not `/api/patients/me/profile`.

### UI & Widgets

1. **Use `AppDesignSystem`** for all colors, spacing, and radii. Never hardcode color values — reference `AppDesignSystem.primary` (#00BBB0), `AppDesignSystem.destructiveRed`, `AppDesignSystem.textPrimary`, etc. Use `AppDesignSystem.h1`, `AppDesignSystem.body1` for typography tokens.
2. **Use `AppDesignSystem` layout constants**: `screenPaddingH` (16), `cardRadius` (16), `cardPadding` (16), `cardSpacing` (12), `sectionToSectionSpacing` (24), `headerHeight` (140), `bottomNavHeight` (80). Never hardcode these values.
3. **`Theme.of(context)`** only for Material-level overrides (e.g., `appBarTheme.titleTextStyle`). For everything else, use `AppDesignSystem` directly.
4. **Reuse `core/widgets/`**: check existing widgets before creating new ones — `ShifaPrimaryButton`, `ShifaSecondaryButton`, `BaseCard`, `EmptyState`, `SectionTitle`, `UserAvatar`, `PhoneInputField`, `SegmentedControl`, `NotificationIconButton`, `ChatIconButton`, `LanguageMiniToggle`, `AppHeader`.
5. **ConsumerWidget** for widgets that read providers. **ConsumerStatefulWidget** only when lifecycle methods (`initState`, `dispose`) are needed.
6. **Responsive design**: use `MediaQuery.of(context).size` and `LayoutBuilder` instead of hardcoded dimensions. Use `Expanded` and `Flexible` in `Row`/`Column`.
7. **Loading states**: show loading indicator while `state.isLoading` is true. Show error with retry button when `state.error` is non-null. Show `EmptyState` widget when data list is empty.
8. **Scrolling**: use `ListView.builder` or `ListView.separated` for lists (lazy loading), not `Column` with `SingleChildScrollView` for dynamic lists.
9. **Images**: use `CachedNetworkImage` for network images (currently used in chat). Provide `placeholder` and `errorWidget` parameters.

### Localization

1. **All user-facing strings** must go through `AppLocalizations`. Never hardcode text in widgets.
2. **Add translations to ALL 4 languages** (en, uz, de, ru) when adding new keys. Missing keys cause null errors at runtime.
3. **Access pattern**: `final l10n = AppLocalizations.of(context)!;` then `l10n.keyName`.
4. **Key naming**: camelCase, descriptive (e.g., `appointmentCancelled`, `noUpcomingBookings`). See `HOW_TO_ADD_TRANSLATIONS.md`.

### Models

1. **Extend `Equatable`** for value equality. Override `props` with all fields. (Note: `ProfessionModel` and task models currently don't — new models should.)
2. **Implement `fromJson` factory** and `toJson` method for serialization. No code generation — write these manually.
3. **Null-safe defaults**: provide default values for optional fields in `fromJson` (e.g., `json['name'] ?? ''`).
4. **Shared models** go in `core/models/`. Feature-specific models that aren't shared can live in the feature's `data/` or `domain/` folder (e.g., `chat/domain/chat_models.dart`).

### Security

1. **Never log tokens, passwords, or PII**. Use `AppLogger` for structured logging — it auto-redacts fields containing: `token`, `password`, `secret`, `authorization`, `cookie`, `apiKey`, `phone`, `email`, `name`, `address`.
2. **Secure storage**: auth tokens go in `FlutterSecureStorage` (via `StorageService`), never `SharedPreferences`. StorageService keys: `auth_token`, `auth_token_saved_at`, `user_id`.
3. **SharedPreferences** is only for non-sensitive data: `has_launched_before`, `app_language`, `shown_notification_ids`, `login_failed_attempts`, `login_lockout_until_epoch_ms`.
4. **Input validation**: validate all user inputs before sending to API. Use `password_validation.dart` for password rules (min 8 chars, max 128, requires uppercase + lowercase + digit + special char).
5. **Sanitize error messages**: use `auth_error_sanitizer.dart` to convert backend errors to user-friendly messages. It filters SQL keywords, stack traces, Java class names, and raw JSON. Never show raw backend errors to users.

### Testing

1. **Mock with `mocktail`**: create mock classes for repositories and ApiClient. Don't mock StateNotifier directly — test through the provider.
2. **Widget tests**: use `ProviderScope(overrides: [...])` to inject mock providers.
3. **Naming**: test files mirror source files — `lib/features/auth/providers/auth_provider.dart` → `test/auth_provider_test.dart`.
4. **Test structure**: `group()` by method/behavior, `test()` with descriptive names starting with a verb (e.g., `'loads appointments on init'`).
5. **Run `flutter analyze` before committing** — CI fails on analyzer errors.

### Performance

1. **`const` widgets**: mark widget constructors `const` when all fields are final/compile-time constants. This skips rebuild.
2. **Selective rebuilds**: watch only the specific provider/state you need. Avoid watching a parent provider when you only need one field — use `ref.watch(provider.select((s) => s.field))`.
3. **`ListView.builder`** or **`ListView.separated`** for dynamic lists. Never use `Column` + `map()` for scrollable lists.
4. **Image compression**: use `ImageCompressionService` before uploading images in chat/documents.
5. **Dispose resources**: cancel timers, close streams, dispose controllers in `dispose()`. All cleanup in this codebase uses traditional `StatefulWidget.dispose()`, not `ref.onDispose`.

### When Making Changes

1. **Follow existing architecture**: place code in the appropriate feature module and layer (data/presentation/providers).
2. **Look at similar features first**: before implementing, check how existing features do it (e.g., check `bookings_repository.dart` before writing a new repository).
3. **Update translations**: any new UI text needs entries in all 4 languages in `app_localizations.dart`.
4. **Test on both platforms**: platform-specific behavior differs, especially Firebase and biometrics.
5. **Backend coordination**: if feature requires backend changes, document them in `BACKEND_INTEGRATION.md`.
6. **No breaking API contracts**: don't change request/response shapes without coordinating with backend team.
7. **Run checks**: `flutter analyze` and `flutter test` must pass before committing.

## Additional Documentation

- `BACKEND_INTEGRATION.md` — Backend endpoints and integration status
- `FIREBASE.md` — Firebase setup and troubleshooting (Phone Auth, FCM)
- `BUILD_IOS.md` — iOS build and distribution
- `PLAY_STORE_PUBLISH.md` — Android Play Store publishing
- `HOW_TO_ADD_TRANSLATIONS.md` — Guide for adding new translations
- `CREATE_TEST_USER.md` — Guide for creating test users in backend
