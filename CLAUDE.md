# Project rules

This repo is a template. API and UI come from new specs / Figma — the tech stack and architecture below are fixed. Follow them for every new feature.

## Sources of truth

- `docs/spec/` is the source of truth for API and features; `docs/spec/15-api-reference.md` wins for request and response shapes.
- Do not reproduce bugs listed in `docs/spec/00-README.md`.
- UI comes from Figma via the Figma MCP. Map design tokens into `ui/theme/`, reusable components into `ui/widgets/`.

## Git

- Remote: `SHIFA-UZ/apps-flutter-patient` (the Flutter app lives on `main`). This KMP rewrite lives on branch `shifa2.0`, branched from `main`.
- Commit all work to `shifa2.0`. Never merge, rebase or open a PR into `main`, and never push to `main`, until the user says the rewrite is done.

## Tech stack (do not swap without asking)

- Kotlin Multiplatform (Android + iOS: `iosArm64`, `iosSimulatorArm64`), single module `composeApp`
- Compose Multiplatform + Material 3 for all UI (shared in `commonMain`)
- Ktor Client (OkHttp on Android, Darwin on iOS) + ContentNegotiation, Logging, Auth (bearer), HttpTimeout
- kotlinx.serialization (JSON), kotlinx.coroutines, kotlinx.datetime
- Multiplatform Settings (`no-arg`) for local key/value + token storage
- Kamel for async image loading
- JetBrains `lifecycle-viewmodel-compose` for ViewModels
- Firebase (google-services, App Distribution for debug builds)
- Tests (`androidUnitTest`): JUnit4, Mockito-Kotlin, Turbine, Truth, kotlinx-coroutines-test, Roborazzi + Robolectric for screenshots
- ktlint (`./gradlew ktlintFormat`)
- All versions live in `gradle/libs.versions.toml`; add deps there, never inline

## Architecture: MVVM + Repository

```
composeApp/src/
├── commonMain/kotlin/<pkg>/
│   ├── App.kt              # root: theme + top-level screen switching
│   ├── data/
│   │   ├── api/            # HttpClient.kt, ApiHelper.kt, ApiConfig.kt
│   │   ├── model/          # @Serializable DTOs, one file per domain (XxxModels.kt), ApiError.kt
│   │   ├── storage/        # TokenStorage (Settings wrapper)
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
- `createHttpClient(tokenProvider)` builds the Ktor client; engine via `expect fun createHttpEngine()`.
- Authenticated calls use `getWithRefresh/postWithRefresh/putWithRefresh/patchWithRefresh/deleteWithRefresh` from `ApiHelper.kt` (auto token refresh on 401 `TOKEN_EXPIRED`, clears storage on invalid token).
- Repositories take `HttpClient` (and `TokenStorage` if needed) via constructor and return `Result<T>`. Map HTTP errors to the sealed `ApiException` (ValidationError, Unauthorized, NotFound, Conflict, RateLimit, ServerError, NetworkError…).

### UI layer
- One `XxxUiState` `@Stable data class` per screen with defaults; derived flags as properties (`isInputValid`).
- ViewModel: constructor-injected deps with default values (no DI framework), `private val _state = MutableStateFlow(XxxUiState())`, `val state: StateFlow<…> = _state.asStateFlow()`, updates via `_state.value = _state.value.copy(...)`, async work in `viewModelScope.launch`, `result.fold(onSuccess, onFailure)` mapping `ApiException` → user message.
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

### CI/CD (GitHub Actions, all manual `workflow_dispatch`, ported from `apps-flutter-patient`)
- `ci.yml`: ktlint, unit tests, Roborazzi verify.
- `google_play_deploy.yml`: signed `bundleRelease` → Play production (`com.shifa.patientapp`). Signing via root `keystore.properties` written by CI (gitignored); versionCode = `VERSION_CODE_OFFSET + run_number`.
- `ios_testflight.yml`: fastlane `beta` lane in `iosApp/fastlane` → TestFlight (`uz.shifa.patient`, team `NH97PGA9PT`). Build number = `BUILD_NUMBER_OFFSET + run_number`; version from `MARKETING_VERSION` in `iosApp/Configuration/Config.xcconfig`.
- Version name lives in two places: `versionMajor/Minor/Patch` in `composeApp/build.gradle.kts` and `MARKETING_VERSION` in `Config.xcconfig` — bump both.
- Secrets (same as Flutter repo): `FIREBASE_ANDROID_CONFIG_BASE64`, `FIREBASE_IOS_CONFIG_BASE64`, `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `GOOGLE_PLAY_SERVICE_ACCOUNT_BASE64`, `IOS_CERTIFICATE_P12_BASE64`, `IOS_CERTIFICATE_PASSWORD`, `APP_STORE_CONNECT_KEY_ID`, `APP_STORE_CONNECT_ISSUER_ID`, `APP_STORE_CONNECT_API_KEY_P8`.
