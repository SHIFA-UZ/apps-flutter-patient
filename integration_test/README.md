# Integration tests (`integration_test`)

## What runs in default CI

The main GitHub Actions workflow runs **`flutter test`** (the `test/` directory only).  
Tests in **`integration_test/`** are **not** run automatically on every PR (they require a device or web driver and often Firebase).

## Smoke test file

- `patient_app_smoke_test.dart` — mounts `ShifaPatientApp` under `IntegrationTestWidgetsFlutterBinding` (minimal golden-path sanity check).

## Running locally

**Android (device or emulator):**

```bash
flutter test integration_test/patient_app_smoke_test.dart
```

**Chrome (Linux/macOS CI-friendly):**

```bash
flutter test integration_test/patient_app_smoke_test.dart -d chrome
```

## Full E2E (login → book → document → video → chat)

Requires:

- Configured Firebase (`flutterfire configure`) and valid `google-services.json` / `GoogleService-Info.plist`
- Running backend matching `AppConfig` / `--dart-define=API_BASE_URL=...`
- Test accounts and permissions (camera, mic)

Consider [Patrol](https://patrol.leancode.co/) or a dedicated staging job with secrets for stable E2E.

## Coverage threshold (e.g. 70%)

After `flutter test --coverage`, use `lcov` / [very_good_coverage](https://pub.dev/packages/very_good_coverage) on `coverage/lcov.info`.  
Enforce a minimum only once the suite consistently clears it; the launch checklist references this.
