# Project rules

This repo is a template. API and UI come from new specs / Figma — the tech stack and architecture below are fixed. Follow them for every new feature.

## Sources of truth

- `docs/kmp-spec/` is the source of truth for API and features; `docs/kmp-spec/15-api-reference.md` wins for request and response shapes.
- Do not reproduce bugs listed in `docs/kmp-spec/00-README.md`.
- UI comes from Figma via the Figma MCP. Map design tokens into `ui/theme/`, reusable components into `ui/widgets/`.
- `docs/implemented/` records what's already built. Read `docs/implemented/README.md` before starting a task, and update it (status, area file, log line) at the end of every job, in the same commit.

## Git

- Remote: `SHIFA-UZ/apps-flutter-patient`. The KMP app replaced the Flutter app on `main` (v1.2.0, `shifa2.0` merged).
- New features go on branch `shifa3.0`. Never push to `main` directly or merge into it unless the user asks; changes reach `main` through a pull request.
- Never commit `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`: the IDE rewrites it locally and drops the Archive action TestFlight builds need.
- Never commit or push on your own. Show the user the finished work (what changed, screenshots, test results), and only commit and push once they approve.
- Commits are authored by the user only, with the repo's git identity (`git config user.name` / `user.email`). Never add `Co-Authored-By`, "Generated with" or any other Claude or agent attribution to commit messages or PR descriptions, and never set Claude or an agent as the author or committer.

## Tech stack (do not swap without asking)

- Kotlin Multiplatform (Android + iOS: `iosArm64`, `iosSimulatorArm64`), single module `composeApp`
- Compose Multiplatform + Material 3 for all UI (shared in `commonMain`)
- Ktor Client (OkHttp on Android, Darwin on iOS) + ContentNegotiation, Logging, HttpTimeout; the Bearer token is added by an `HttpSend` interceptor (no Auth plugin, no refresh token)
- kotlinx.serialization (JSON), kotlinx.coroutines, kotlinx.datetime
- Multiplatform Settings (`no-arg`) for local key/value; the token uses `secureSettings` (`KeychainSettings` on iOS, EncryptedSharedPreferences from `androidx.security:security-crypto` on Android)
- Kamel for async image loading
- JetBrains `lifecycle-viewmodel-compose` for ViewModels
- Firebase (google-services, App Distribution for debug builds)
- Daily for video calls: `co.daily:client` on Android, Daily's Swift package on iOS behind `iosApp/iosApp/DailyVideoCall.swift` (it implements the Kotlin `NativeVideoCall`)
- Tests (`androidUnitTest`): JUnit4, Mockito-Kotlin, Turbine, Truth, kotlinx-coroutines-test, Roborazzi + Robolectric for screenshots
- ktlint (`./gradlew ktlintFormat`)
- All versions live in `gradle/libs.versions.toml`; add deps there, never inline

## Architecture: MVVM + Repository

```
composeApp/src/
├── commonMain/kotlin/<pkg>/
│   ├── App.kt              # root: theme + top-level screen switching
│   ├── data/
│   │   ├── api/            # HttpClient.kt, ApiHelper.kt, ApiConfig.kt, ErrorMessages.kt
│   │   ├── model/          # @Serializable DTOs, one file per domain (XxxModels.kt), ApiError.kt
│   │   ├── storage/        # TokenStorage, SecureSettings (expect/actual secure store)
│   │   └── <feature>/      # XxxRepository.kt
│   ├── ui/
│   │   ├── theme/          # Color.kt, Theme.kt, Typography.kt (tokens from Figma)
│   │   ├── widgets/        # reusable design-system components (<App>Button, <App>TextField…)
│   │   ├── navigation/     # bottom bar / tabs
│   │   └── <feature>/      # XxxScreen.kt, XxxViewModel.kt, XxxUiState.kt
│   └── utils/              # expect/actual platform helpers
├── androidMain/            # actuals, MainActivity, @Preview files (ui/<feature>/XxxPreviews.kt)
├── iosMain/                # actuals, MainViewController
└── androidUnitTest/        # ViewModel tests + widget screenshot tests
iosApp/                     # Xcode host app
```

### Data layer
- `ApiConfig` holds `BASE_URL`, `TIMEOUT_MS`, and `Endpoints` (consts, or functions for path params).
- `createHttpClient(tokenStorage, engine)` builds the Ktor client (engine defaults to `expect fun createHttpEngine()`); the app uses the shared `httpClient`. The token goes only to the backend host and never on `ApiConfig.PUBLIC_AUTH_PATHS`.
- Calls go through `apiCall<T>(defaultMessage) { client.get(…) }` from `ApiHelper.kt`: returns `Result<T>`, non-2xx becomes an `ApiException` whose message is sanitized by `sanitizeError` (`ErrorMessages.kt`), no response becomes `NetworkError`. There is no refresh token; a 401 means logout.
- Repositories take `HttpClient` (and `TokenStorage` if needed) via constructor and return `Result<T>`. Map HTTP errors to the sealed `ApiException` (ValidationError, Unauthorized, NotFound, Conflict, RateLimit, ServerError, NetworkError…).
- Debug builds have Profile → Debug menu: demo data (`data/debug/DemoBackend.kt`, a `MockEngine` behind `httpClient`) and fake pushes. A new endpoint gets a `DemoBackend` handler too; without one, demo GETs read production and other calls are refused.
- Logging: errors only, never successes. Every failed backend call is logged through `httpLogger` (Logcat tag `HttpClient` on Android, the Xcode console on iOS). `apiCall` already does it: `FAILED <status> <method> <path>: <backend reason>`, `FAILED <message>, no response: <exception>`, or an unreadable body. Network code outside `apiCall` (e.g. the Shifa AI SSE stream) logs its failures the same way, with `logFailure(response, reason)` or `httpLogger.log("FAILED …")`. Debug builds only (release logs nothing); never log tokens, passwords, headers or request bodies.

### UI layer
- One `XxxUiState` `@Stable data class` per screen with defaults; derived flags as properties (`isInputValid`).
- ViewModel: constructor-injected deps with default values (no DI framework), `private val _state = MutableStateFlow(XxxUiState())`, `val state: StateFlow<…> = _state.asStateFlow()`, updates via `_state.value = _state.value.copy(...)`, async work in `viewModelScope.launch`, `result.fold(onSuccess, onFailure)` mapping the failure to a localized `StringResource` with `e.toUserMessage()`.
- Screen split in two:
  - `XxxScreen(viewModel = viewModel { XxxViewModel() }, onNavigate… callbacks)` — collects state, wires callbacks.
  - `internal fun XxxScreenContent(state, onXxx: () -> Unit, …)` — stateless, used by previews and screenshot tests.
- Navigation: simple state (`rememberSaveable`) in `App.kt` / `MainScreen` + `Scaffold` bottom bar. No nav library.
- No raw hex/dp/sp in screens or widgets. Tokens live in `ui/theme/` and are named 1:1 after Figma variables: `ShifaColors.<Text|Icon|Bg|Border|Interactive|Alpha>.X` (`semantic/color/*`), `MaterialTheme.typography.*` + `ShifaTextStyles`, `ShifaSpacing.<Inset|Gap|Stack>.X` (`semantic/space/*`), `ShifaRadius.*`, `ShifaShadow.*` (via `Modifier.dropShadow(shape, shadow)`). Shared components come from `ui/widgets`.
- Figma is light-mode only; `ShifaTheme` has no dark scheme.

### Tests
- Every ViewModel gets `XxxViewModelTest`: `StandardTestDispatcher` + `Dispatchers.setMain`, mocked repositories, `runTest` + `advanceUntilIdle`, assert with Turbine `state.test { awaitItem() }` + Truth. Class under test named `classToTest`, backtick test names.
- Every widget gets `XxxScreenshotTest` (`@RunWith(RobolectricTestRunner)`, `@GraphicsMode(NATIVE)`, `@Config(sdk = [33])`, `captureRoboImage("screenshots/Xxx_state.png")`).
- Commands: `./gradlew :composeApp:testDebugUnitTest`, `verifyRoborazziDebug`, `recordRoborazziDebug`.

### CI/CD (GitHub Actions, ported from `apps-flutter-patient`; deploys are manual `workflow_dispatch`)
- `ci.yml`: runs on every pull request (and by hand): ktlint, unit tests, Roborazzi verify. The deploy workflows don't run tests.
- `google_play_deploy.yml`: signed `bundleRelease` → Play production (`com.shifa.patientapp`). Signing via root `keystore.properties` written by CI (gitignored); versionCode = `VERSION_CODE_OFFSET + run_number`.
- `ios_testflight.yml`: fastlane `beta` lane in `iosApp/fastlane` → TestFlight (`uz.shifa.patient`, team `NH97PGA9PT`). Build number = `BUILD_NUMBER_OFFSET + run_number`; version from `MARKETING_VERSION` in `iosApp/Configuration/Config.xcconfig`.
- Version name lives in one place: `MARKETING_VERSION` in `iosApp/Configuration/Config.xcconfig`; `composeApp/build.gradle.kts` reads it for Android's `versionName`.
- Secrets (same as Flutter repo): `FIREBASE_ANDROID_CONFIG_BASE64`, `FIREBASE_IOS_CONFIG_BASE64`, `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `GOOGLE_PLAY_SERVICE_ACCOUNT_BASE64`, `IOS_CERTIFICATE_P12_BASE64`, `IOS_CERTIFICATE_PASSWORD`, `APP_STORE_CONNECT_KEY_ID`, `APP_STORE_CONNECT_ISSUER_ID`, `APP_STORE_CONNECT_API_KEY_P8`.
