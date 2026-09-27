# App template (Kotlin Multiplatform)

Android + iOS app skeleton. Tech stack and architecture rules: see [CLAUDE.md](CLAUDE.md).

## Build

```bash
./gradlew :composeApp:assembleDebug        # Android
./gradlew :composeApp:testDebugUnitTest    # unit tests
./gradlew :composeApp:verifyRoborazziDebug # screenshot tests
./gradlew ktlintFormat
```

iOS: open `iosApp/iosApp.xcodeproj` in Xcode.
