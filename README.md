# Shifa Patient App

A Flutter mobile application for patients to interact with the Shifa healthcare platform — book appointments, chat with doctors, manage medical documents, and receive remote care tasks.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Build Flavors](#build-flavors)
- [Project Structure](#project-structure)
- [Localization](#localization)
- [Testing](#testing)
- [CI/CD](#cicd)
- [Platform Requirements](#platform-requirements)
- [Permissions](#permissions)

## Features

- **Authentication** — Login (phone/email + password), registration with phone OTP verification, password reset, forced password reset on first login
- **Appointment Booking** — Browse doctors, select date/time, video consultation toggle, reason for visit, confirmation flow
- **Video Consultations** — Video calling via Daily.co with waiting room and post-call visit summary
- **Chat** — Real-time messaging with doctors, text/image/voice/document message types, typing indicators
- **Medical Documents** — Upload, view, download, and delete documents with in-app PDF viewer
- **Doctor Discovery** — Search and filter doctors, view profiles with specializations and patient reviews
- **Remote Care Tasks** — View assigned tasks and submit check-ins
- **Notifications** — Push notifications via Firebase Cloud Messaging with in-app notification center
- **Profile Management** — Edit personal info, location picker with map, account deletion with OTP verification
- **App Lock** — PIN code and biometric authentication (Face ID / fingerprint)
- **Multi-language** — English, Uzbek, German, Russian
- **Crash Reporting** — Firebase Crashlytics integration

## Tech Stack

| Category | Technology |
|----------|------------|
| Framework | Flutter (SDK ^3.9.2) |
| State Management | Riverpod (`flutter_riverpod: ^2.5.1`) |
| Navigation | GoRouter (`go_router: ^14.0.0`) |
| HTTP Client | Dio (`dio: ^5.4.0`) with JWT auth interceptor |
| Backend | Spring Boot REST API (Railway) |
| Auth | Firebase Auth (Phone OTP), JWT tokens |
| Push Notifications | Firebase Cloud Messaging + Local Notifications |
| Crash Reporting | Firebase Crashlytics |
| Video Calls | Daily Flutter (`daily_flutter: ^0.37.0`) |
| Maps | Flutter Map + Geocoding + Geolocator |
| PDF Viewer | pdfx |
| Audio | record (recording), audioplayers (playback) |
| Secure Storage | FlutterSecureStorage (Keychain/Keystore) |
| Local Storage | SharedPreferences |
| Biometrics | local_auth |

## Architecture

The app follows a **modified Clean Architecture** with a feature-based structure. Each feature module has three layers:

```
feature/
├── data/              # Repositories (API calls via ApiClient)
├── providers/         # Riverpod providers (StateNotifier, FutureProvider)
└── presentation/      # Screens and feature-specific widgets
```

There is no `domain/usecases` layer — business logic lives in providers and repositories.

**State management** uses `StateNotifier` with manual loading/error state handling via `copyWith` pattern. The codebase does not use `AsyncNotifier`, code generation (`@riverpod`), or `autoDispose`.

**Design system** is centralized in `AppDesignSystem` with color tokens, typography, spacing constants, and layout values. Primary color: `#00BBB0` (teal).

## Prerequisites

- Flutter SDK ^3.9.2
- Xcode (for iOS builds)
- Android Studio or Android SDK (for Android builds)
- Firebase project configured (see `FIREBASE.md`)
- Backend server running (or use production URL)

## Getting Started

### 1. Clone and install dependencies

```bash
git clone <repository-url>
cd apps-flutter-patient
flutter pub get
```

### 2. Firebase setup

The project requires Firebase for phone authentication, push notifications, and crash reporting.

```bash
# Regenerate Firebase config if needed
dart run flutterfire configure

# Get Android SHA fingerprints for Firebase Console
cd android && ./gradlew signingReport
```

Ensure `google-services.json` (Android) and `GoogleService-Info.plist` (iOS) are in place.

### 3. Run the app

```bash
# Run with debug flavor (connects to localhost:8080)
flutter run

# Run with production backend
flutter run --dart-define=FLAVOR=production

# Run with custom backend URL
flutter run --dart-define=API_BASE_URL=https://your-backend.com
```

## Build Flavors

The app uses `--dart-define` flags to configure the backend URL at build time. Configuration lives in `lib/core/config/app_config.dart`.

| Flavor | API Base URL | Usage |
|--------|-------------|-------|
| `debug` | `http://localhost:8080/api` | Local development (default) |
| `qa` | `http://localhost:9090/api` | QA testing |
| `production` | `https://shifa-doc-backend-mvp-production.up.railway.app/api` | Production / release builds |

```bash
# Debug (default)
flutter run

# QA
flutter run --dart-define=FLAVOR=qa

# Production
flutter run --dart-define=FLAVOR=production

# Custom URL (overrides flavor)
flutter run --dart-define=API_BASE_URL=https://staging.example.com
```

Android emulator automatically converts `localhost` to `10.0.2.2`. Release builds default to production if no flavor is specified.

### Building for release

```bash
# Android APK
flutter build apk --release --dart-define=FLAVOR=production

# Android App Bundle (Play Store)
flutter build appbundle --release --dart-define=FLAVOR=production

# iOS
flutter build ios --release --dart-define=FLAVOR=production
```

## Project Structure

```
lib/
├── main.dart                       # Entry point, Firebase init
├── app/
│   ├── app.dart                   # MaterialApp, lifecycle management
│   ├── router.dart                # GoRouter config, auth guards
│   ├── main_shell.dart            # Shell routing wrapper
│   └── persistent_bottom_bar.dart # Bottom navigation bar
├── core/
│   ├── config/                    # Environment config (API URLs, flags)
│   ├── constants/                 # Asset paths
│   ├── localization/              # AppLocalizations (4 languages)
│   ├── models/                    # Shared data models (Equatable, JSON)
│   ├── network/                   # ApiClient (Dio + JWT auth)
│   ├── providers/                 # Global providers (language, connectivity)
│   ├── services/                  # Push notifications, video, app lock, geocoding
│   ├── theme/                     # AppTheme + AppDesignSystem
│   ├── utils/                     # Logger, storage, validation, date helpers
│   └── widgets/                   # Reusable UI components
└── features/
    ├── auth/                      # Login, registration, OTP, password reset
    ├── bookings/                  # Appointment booking, details, video call
    ├── chat/                      # Messaging with doctors
    ├── doctors/                   # Doctor listing, profiles, reviews
    ├── documents/                 # Medical document management
    ├── home/                      # Home screen
    ├── notifications/             # Notification center
    ├── profile/                   # User profile, edit, account deletion
    ├── settings/                  # App lock settings
    └── tasks/                     # Remote care tasks
```

## Localization

The app supports 4 languages:

| Language | Code |
|----------|------|
| English | `en` |
| Uzbek | `uz` |
| German | `de` |
| Russian | `ru` |

Translations are managed in `lib/core/localization/app_localizations.dart` using a custom `LocalizationsDelegate`. All user-facing strings must have entries in all 4 languages.

The app auto-detects the system language on first launch and persists the user's selection via SharedPreferences.

## Testing

```bash
# Run all tests
flutter test

# Run a specific test
flutter test test/widget_test.dart

# Run with coverage
flutter test --coverage

# Static analysis
flutter analyze
```

Tests use `flutter_test` and `mocktail` for mocking. Test files are in `test/` and integration tests in `integration_test/`.

## CI/CD

### Pull Request Checks (`flutter_ci.yml`)

Runs on push/PR to `main`:
1. Format check (test files only)
2. `flutter analyze` (fails on errors, allows warnings/infos)
3. `flutter test --coverage`

### iOS TestFlight (`ios_testflight.yml`)

Manual workflow dispatch that builds and deploys to TestFlight via Xcode 26.0 and Flutter 3.35.5.

## Platform Requirements

| Platform | Minimum Version | Notes |
|----------|----------------|-------|
| Android | API 24 (Android 7.0) | Required by Daily.co video SDK |
| iOS | 13.0 | Deployment target |

**App Identifiers:**
- Android: `com.shifa.patientapp`
- iOS Display Name: "Shifa Bemor"

## Permissions

### Android

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | API communication |
| `CAMERA` | Video consultations |
| `RECORD_AUDIO` | Video consultations, voice messages |
| `MODIFY_AUDIO_SETTINGS` | Audio routing during calls |
| `ACCESS_FINE_LOCATION` | Doctor discovery, profile address |
| `ACCESS_COARSE_LOCATION` | Doctor discovery, profile address |
| `POST_NOTIFICATIONS` | Push notifications (Android 13+) |

### iOS

| Key | Description |
|-----|-------------|
| `NSCameraUsageDescription` | Video consultations |
| `NSMicrophoneUsageDescription` | Video consultations |
| `NSLocationWhenInUseUsageDescription` | Doctor discovery, profile address |
| `NSLocationAlwaysAndWhenInUseUsageDescription` | Doctor discovery, profile address |
| `NSPhotoLibraryUsageDescription` | Profile picture |
