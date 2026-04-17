# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **Shifa Patient App**, a Flutter mobile application for patients to interact with the Shifa healthcare platform. It uses **Clean Architecture** with a feature-based structure, **Riverpod** for state management, and **GoRouter** for navigation.

## Tech Stack

- **Flutter/Dart** (SDK: ^3.9.2)
- **State Management**: Riverpod (`flutter_riverpod`)
- **Navigation**: GoRouter with StatefulShellRoute for persistent bottom navigation
- **HTTP Client**: Dio with JWT authentication interceptor
- **Backend Communication**: REST API (Railway production, localhost dev)
- **Firebase**: Authentication (Phone OTP), Cloud Messaging (push notifications)
- **Video Calls**: Daily Flutter (`daily_flutter`)
- **Maps**: Flutter Map with geocoding
- **Localization**: 4 languages (English, Uzbek, German, Russian)
- **Local Storage**: SharedPreferences for preferences, FlutterSecureStorage for tokens
- **Biometrics**: local_auth for app lock

## Architecture

### Directory Structure

```
lib/
├── app/                    # App initialization, router, shell
├── core/                   # Shared code across features
│   ├── config/            # App configuration (API URLs, environment)
│   ├── constants/         # App-wide constants
│   ├── localization/      # Multi-language translations
│   ├── models/            # Shared data models
│   ├── network/           # ApiClient (Dio wrapper with auth)
│   ├── providers/         # Global Riverpod providers
│   ├── services/          # Core services (notifications, etc.)
│   ├── theme/             # App theme configuration
│   ├── utils/             # Utility functions
│   └── widgets/           # Reusable widgets
└── features/              # Feature modules (Clean Architecture)
    ├── auth/
    ├── bookings/
    ├── chat/
    ├── doctors/
    ├── documents/
    ├── home/
    ├── notifications/
    ├── profile/
    ├── settings/
    └── tasks/
```

### Feature Structure Pattern

Each feature follows Clean Architecture:
```
feature/
├── data/              # Repositories (API calls, data sources)
├── presentation/      # UI screens and widgets
└── providers/         # Riverpod state providers
```

**Note**: This app does NOT have a `domain/` layer with use cases. Business logic is in providers and repositories.

### Key Architectural Decisions

1. **State Management**: Riverpod providers are in `feature/providers/`. Use `StateNotifier` or `AsyncNotifier` for complex state.

2. **Navigation**: GoRouter configuration in `lib/app/router.dart`. Uses `StatefulShellRoute.indexedStack` to preserve tab state for bottom navigation (Home, Bookings, Documents, Doctors). Other tabs (Account, Chat, Notifications, Tasks) use `ShellRoute` to show persistent bottom bar.

3. **API Client**: Centralized in `lib/core/network/api_client.dart`. Automatically adds JWT token to requests. On 401 (unauthorized), triggers logout. On 403 (forbidden), does NOT logout (wrong role).

4. **Backend Configuration**:
   - **Production**: `https://shifa-doc-backend-mvp-production.up.railway.app/api` (used in release builds)
   - **Debug**: `http://localhost:8080/api` (iOS) or `http://10.0.2.2:8080/api` (Android emulator)
   - **QA**: `http://localhost:9090/api` (iOS) or `http://10.0.2.2:9090/api` (Android emulator)
   - Configuration is in `lib/core/config/app_config.dart` and works for both iOS and Android
   - Set flavor via: `--dart-define=FLAVOR=production|debug|qa`
   - Override URL via: `--dart-define=API_BASE_URL=https://your-backend.com`

5. **Localization**: All translations in `lib/core/localization/app_localizations.dart`. See `HOW_TO_ADD_TRANSLATIONS.md` for how to add translations.

6. **Firebase**: Used for Phone OTP (registration/verification) and FCM (push notifications). Configuration in `lib/firebase_options.dart`. See `FIREBASE.md` for setup details.

## Build Flavors

The app supports three build flavors that automatically configure the API base URL:

| Flavor | API URL | Use Case | Command |
|--------|---------|----------|---------|
| **production** | `https://shifa-doc-backend-mvp-production.up.railway.app/api` | Production/Release builds | `--dart-define=FLAVOR=production` |
| **debug** | `http://localhost:8080/api` | Local development | Default (no flag needed) |
| **qa** | `http://localhost:9090/api` | QA testing | `--dart-define=FLAVOR=qa` |

**How it works:**
- All configuration is in `lib/core/config/app_config.dart` (single source of truth)
- Works for both iOS and Android without platform-specific setup
- Android emulator automatically converts `localhost` to `10.0.2.2`
- Release builds default to production flavor if not specified
- You can override with `--dart-define=API_BASE_URL=https://custom-url.com`

## Common Commands

### Development

```bash
# Install dependencies
flutter pub get

# Run with debug flavor (default, uses localhost:8080)
flutter run -d ios

# Run with production flavor
flutter run --dart-define=FLAVOR=production

# Run with QA flavor (uses localhost:9090)
flutter run --dart-define=FLAVOR=qa

# Run with custom backend URL (overrides flavor)
flutter run --dart-define=API_BASE_URL=https://staging.yourdomain.com

# Run tests
flutter test

# Run specific test
flutter test test/widget_test.dart

# Lint/analyze code
flutter analyze

# Clean build artifacts
flutter clean
```

### Building

```bash
# Build Android APK with debug flavor
flutter build apk --dart-define=FLAVOR=debug

# Build Android APK with production flavor (default for release builds)
flutter build apk --release --dart-define=FLAVOR=production

# Build Android App Bundle for Play Store (uses production by default in release mode)
flutter build appbundle --release

# Build with QA flavor
flutter build apk --dart-define=FLAVOR=qa

# Build iOS (requires Xcode)
flutter build ios --dart-define=FLAVOR=production

# Build with custom backend URL (overrides flavor)
flutter build apk --release --dart-define=API_BASE_URL=https://your-backend.com
```

See `BUILD_IOS.md` for iOS build details and `PLAY_STORE_PUBLISH.md` for Android publishing.

### Firebase

```bash
# Configure Firebase (regenerate firebase_options.dart)
dart run flutterfire configure

# Get Android SHA-1/SHA-256 for Firebase Console
cd android && ./gradlew signingReport
```

See `FIREBASE.md` for complete Firebase setup (Phone Auth, FCM, SHA certificates).

## Key Files and Their Purposes

### Configuration
- `lib/core/config/app_config.dart` - Environment configuration (API URLs, build-time flags)
- `lib/firebase_options.dart` - Firebase SDK configuration (auto-generated by FlutterFire)
- `pubspec.yaml` - Dependencies and app metadata
- `analysis_options.yaml` - Dart linter rules

### Core Infrastructure
- `lib/main.dart` - App entry point, Firebase initialization
- `lib/app/app.dart` - Root widget, MaterialApp setup
- `lib/app/router.dart` - GoRouter navigation configuration
- `lib/core/network/api_client.dart` - HTTP client with authentication
- `lib/core/localization/app_localizations.dart` - Multi-language translations
- `lib/core/theme/app_theme.dart` - App theme (teal primary: #26C6DA)

### Authentication
- `lib/features/auth/data/auth_repository.dart` - Login, registration, password reset
- `lib/features/auth/data/phone_auth_repository.dart` - Firebase Phone OTP
- `lib/features/auth/providers/auth_provider.dart` - Global auth state (manages JWT token)

## Backend Integration

The app communicates with a Spring Boot backend. See `BACKEND_INTEGRATION.md` for detailed endpoint documentation.

### Test User Credentials

A test patient user exists in the backend:
- **Phone**: `+998901234567`
- **Email**: `patient@test.com`
- **Password**: `patient123`

### Key Backend Endpoints

**Authentication:**
- `POST /auth/login` - Login (phone/email + password) → JWT token
- `POST /auth/register-patient` - Register new patient
- `POST /auth/forgot-password` - Request password reset
- `POST /auth/reset-password` - Complete password reset

**Patient:**
- `GET /patients/me/profile` - Get current patient profile
- `PATCH /patients/me/profile` - Update patient profile
- `GET /patients/me/appointments` - List patient's appointments
- `POST /patients/me/appointments` - Book new appointment
- `GET /patients/me/appointments/{id}` - Get appointment details
- `DELETE /patients/me/appointments/{id}` - Cancel appointment

**Documents:**
- `GET /patients/me/documents` - List patient documents
- `POST /patients/me/documents` - Upload document
- `DELETE /patients/me/documents/{id}` - Delete document

**Doctors:**
- `GET /api/doctors` - List all doctors (public)
- `GET /api/doctors/{id}` - Get doctor profile (public)

## Important Patterns and Conventions

### State Management with Riverpod

```dart
// Define provider in feature/providers/
final myDataProvider = StateNotifierProvider<MyNotifier, MyState>((ref) {
  final apiClient = ref.read(apiClientProvider);
  return MyNotifier(apiClient);
});

// Use in widgets
class MyWidget extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(myDataProvider);
    // ... use state
  }
}
```

### API Calls

Always use `ApiClient` from `lib/core/network/api_client.dart`:

```dart
final apiClient = ref.read(apiClientProvider);

// GET request
final response = await apiClient.get('/patients/me/profile');

// POST request with body
final response = await apiClient.post('/auth/login', data: {
  'username': phone,
  'password': password,
});

// File upload
final response = await apiClient.uploadFile(
  '/patients/me/documents',
  filePath,
  fieldName: 'file',
);
```

The `ApiClient` automatically:
- Adds `Authorization: Bearer <token>` header if logged in
- Handles 401 (unauthorized) by logging out the user
- Does NOT logout on 403 (forbidden) - that's for wrong role errors

### Navigation

```dart
// Navigate to route
context.go('/bookings');

// Navigate with path parameters
context.go('/doctors/$doctorId');

// Book an appointment (path includes doctor id; optional ?rescheduleId= for reschedule)
context.push('/bookings/flow/$doctorId');

// Go back
context.pop();
```

**Important**: When passing complex objects between routes, use `state.extra` (not path/query params) to avoid serialization issues.

### Localization

```dart
// Get localization instance
final l10n = AppLocalizations.of(context)!;

// Use translations
Text(l10n.hello)  // "Hello" in English, "Salom" in Uzbek, etc.
```

To add missing translations, see `HOW_TO_ADD_TRANSLATIONS.md`.

### Theme Usage

```dart
// Access theme colors
final theme = Theme.of(context);
final primaryColor = theme.colorScheme.primary;  // Teal #26C6DA

// Pre-defined text styles
Text('Title', style: theme.textTheme.headlineMedium)
```

## Testing

Tests are in `test/` directory. Run with:

```bash
flutter test
flutter test test/specific_test.dart
```

**Note**: Currently limited test coverage. When writing tests:
- Widget tests for critical UI flows
- Unit tests for providers and repositories
- Mock `ApiClient` for repository tests

## Firebase Setup

Firebase is used for:
1. **Phone Authentication (OTP)** - During registration and phone verification
2. **Cloud Messaging (FCM)** - Push notifications

### Quick Setup Checklist

1. **Enable Phone Authentication** in Firebase Console
2. **Add Android SHA-1 and SHA-256** fingerprints to Firebase Console (get via `cd android && ./gradlew signingReport`)
3. **Add test phone numbers** in Firebase Console to avoid SMS throttling during development
4. **Re-download** `google-services.json` after adding SHA fingerprints

See `FIREBASE.md` for complete troubleshooting guide (covers "blocked requests", "app not authorized", etc.).

## Common Gotchas

1. **Android emulator can't reach localhost**: Use `10.0.2.2` instead of `localhost` for backend URL. This is handled automatically by `ApiClient.apiBaseUrl`.

2. **401 vs 403 errors**: The ApiClient logs out on 401 (token invalid/expired) but NOT on 403 (valid token, wrong permissions). This prevents patients from being logged out when accessing doctor-only endpoints.

3. **State preservation in tabs**: Home, Bookings, Documents, Doctors use `StatefulShellRoute.indexedStack` which preserves their state. Account, Chat, Notifications, Tasks use regular `ShellRoute` and do NOT preserve state.

4. **Translation keys must exist in all languages**: When adding translations, add the key to all language sections (en, uz, de, ru) to avoid null errors.

5. **Firebase Phone Auth requires real device for Play Integrity**: Emulators may fail phone verification. Use test phone numbers or test on physical device.

6. **Backend must be running**: The app requires the backend to be running. If backend is down, you'll see connection errors. Check `BACKEND_INTEGRATION.md` for backend setup.

7. **JWT token grace period**: After login, there's a 10-second grace period before 401 triggers logout. This prevents race conditions where a request is made immediately after login but before token is fully saved.

## Additional Documentation

- `README.md` - Project overview, features, and basic setup
- `BACKEND_INTEGRATION.md` - Backend endpoints and integration status
- `FIREBASE.md` - Firebase setup and troubleshooting (Phone Auth, FCM)
- `BUILD_IOS.md` - iOS build and distribution
- `PLAY_STORE_PUBLISH.md` - Android Play Store publishing
- `HOW_TO_ADD_TRANSLATIONS.md` - Guide for adding new translations
- `CREATE_TEST_USER.md` - Guide for creating test users in backend

## Current Status and Known Issues

### Completed Features
- ✅ Authentication (login, registration with phone OTP, password reset)
- ✅ Home screen with upcoming appointments
- ✅ Appointment booking flow (select doctor, date, time, confirm)
- ✅ Doctor listings and profiles
- ✅ Document management (list, upload, download, view PDF)
- ✅ Profile management (view, edit)
- ✅ Multi-language support (en, uz, de, ru)
- ✅ Firebase integration (Phone Auth, FCM)
- ✅ App lock with biometrics
- ✅ Chat functionality
- ✅ Tasks management
- ✅ Notifications

### Areas Needing Work
- Video calling UI is implemented but may need integration testing with Daily.co
- Some mock data still used for doctors/appointments (backend integration in progress)
- Test coverage is minimal
- Error handling could be more comprehensive

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

## When Making Changes

1. **Follow the existing architecture**: Place code in the appropriate feature module and layer (data/presentation/providers).

2. **Use existing patterns**: Look at similar features before implementing new ones. For example, if adding a new API call, check how `auth_repository.dart` or `bookings_repository.dart` does it.

3. **Update translations**: If adding new UI text, add translations for all languages in `app_localizations.dart`.

4. **Test on both iOS and Android**: Platform-specific behavior differs, especially for Firebase and biometrics.

5. **Consider backend integration**: If feature requires backend changes, document them in `BACKEND_INTEGRATION.md`.

6. **Maintain backward compatibility**: Don't break existing API contracts without coordinating with backend team.
