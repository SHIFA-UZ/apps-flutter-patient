# Building the Shifa Patient App for iOS

The patient app **already supports iOS**. The `ios/` folder contains the full Xcode project. To build and run the iOS version you need a **Mac with Xcode** (iOS builds cannot be done on Windows).

## Prerequisites

- **macOS** (Monterey 12+ recommended)
- **Xcode** (from Mac App Store, latest stable)
- **Flutter** installed and `flutter doctor` passing for iOS
- **CocoaPods**: `sudo gem install cocoapods` (often already installed with Xcode)
- **Apple Developer account** (for running on device / App Store; simulator works without)

## One-time setup on Mac

```bash
cd /path/to/shifa_patient_app_v1
flutter pub get
cd ios && pod install && cd ..
flutter doctor
```

Fix any issues reported by `flutter doctor` (e.g. Xcode license, command-line tools).

## Run on iOS Simulator

```bash
cd /path/to/shifa_patient_app_v1
flutter run -d ios \
  --dart-define=API_BASE_URL=https://shifa-doc-backend-mvp-production.up.railway.app/api \
  --dart-define=ENVIRONMENT=production
```

Or pick a device with `flutter devices` then `flutter run -d <device_id>`.

## Build release for device / App Store (on Mac)

Use the same `--dart-define` values as your Android build:

```bash
cd /path/to/shifa_patient_app_v1
flutter build ios --release \
  --dart-define=API_BASE_URL=https://shifa-doc-backend-mvp-production.up.railway.app/api \
  --dart-define=ENVIRONMENT=production
```

Then:

1. Open **Xcode**: `open ios/Runner.xcworkspace`
2. Select the **Runner** scheme and your **team** (Signing & Capabilities).
3. Choose **Product → Archive**.
4. In Organizer, use **Distribute App** to upload to App Store Connect or export an IPA.

## Build from Windows

You **cannot** build an iOS app directly on Windows. Options:

1. **Use a Mac** (your own or a colleague’s) and run the commands above.
2. **CI/CD on macOS**: e.g. **Codemagic**, **GitHub Actions** (macos-latest), **Bitrise** — add a workflow that runs `flutter build ios` and optionally uploads to TestFlight/App Store.
3. **Rent a Mac in the cloud** (MacStadium, AWS EC2 Mac, etc.) and run the same commands via SSH.

## Configuration (same as Android)

- **API base URL** and **environment** are set via `--dart-define` (see examples above).
- **Google Maps API key** (if you use it on iOS): set in Xcode or via `--dart-define=GOOGLE_MAPS_API_KEY=...` and ensure the key has iOS restrictions in Google Cloud Console.

## Summary

| Item              | Status |
|-------------------|--------|
| iOS project       | ✅ Present in `ios/` |
| Camera / mic      | ✅ In Info.plist |
| Location / photos | ✅ In Info.plist |
| Build on Windows  | ❌ Not possible; use Mac or cloud Mac |
| Build on Mac      | ✅ `flutter build ios` + Xcode |
