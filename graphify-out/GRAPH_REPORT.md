# Graph Report - shifa 2  (2026-09-27)

## Corpus Check
- 147 files · ~180,069 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 87 file(s) not represented in the graph (top: .xml 65, (none) 5, .ttf 4)

## Summary
- 1233 nodes · 2537 edges · 97 communities (69 shown, 28 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 216 edges (avg confidence: 0.86)
- Token cost: 626,059 input · 0 output

## Community Hubs (Navigation)
- Widget Screenshots & Cards
- API Helper & Token Refresh
- Document & Image Pickers
- Auth Spec (Repository & Flows)
- App Entry & Theme
- Roborazzi Screenshot Tests
- Dropdown/Switch/Image Widgets
- Compose Preview Files
- Remote Care Tasks
- NavBar, Tray & PopUp
- Booking Slots & Appointments
- Spec Index & Core Architecture
- Payments & Visit Summary
- Spacing & Size Tokens
- Notification Widget
- Release CI/CD & Firebase
- Shifa AI Copilot Chat
- Android File Saver
- Project Setup & Localization Tickets
- Booking & Payment API
- Location Picking & Public Config
- Profile Screens & Store
- iOS Host App
- Color Tokens & Page Element
- Chat Bubble & Bar Widgets
- Auth Endpoints & Phone Input
- Doctors Discovery
- Input & Search Fields
- Documents, Video & 401 Rules
- Documents & Attachments
- Widget Kit Status (Implemented)
- App Lock State Machine
- Chat Conversation & Voice
- Notifications, FCM & Logout
- Profile Model & HTTP API
- Bottom NavBar
- Switch, PopUp & Gallery
- Tag Widget
- Project Rules & Fixed Stack
- Backend & Base Cards
- Verified Bugs & Error Localization
- Flutter Design Tokens (Spec)
- Home Screen & Bookings List
- Notifications Inbox & Badges
- MVVM + Repository Pattern
- Splash, Lock Screen & Header
- Shifa AI Gating & Redirect Guard
- Chat API & Tickets
- Section Widget
- Storage & DI Decision
- Figma Token Pipeline
- Time Provider
- Form Field State Styling
- Platform expect/actual
- Shell Routes & Deep Links
- Chat Polling & Contacts
- Doctors API & Home Nearby
- Push & Local Notifications
- Chips & Carousel
- Lint, Tests & CI Workflow
- Account Deletion OTP
- Pager Widget
- Chip Widget Tests
- Image Container Widget
- FileSaver (common)
- Design System Tickets
- Chat Message Models
- Subscription Tier Gate
- Chat Screenshot Tests
- Gradle Wrapper
- composeApp Build Script
- Android HTTP Engine
- ApiConfig
- iOS HTTP Engine
- iOS App Icon & Launch Logo
- Play Store Icon
- Launcher Icon hdpi
- Round Icon hdpi
- Launcher Icon mdpi
- Round Icon mdpi
- Launcher Icon xhdpi
- Round Icon xhdpi
- Launcher Icon xxhdpi
- Round Icon xxhdpi
- Launcher Icon xxxhdpi
- Round Icon xxxhdpi
- Date Utils
- Password Validation
- App Version Footer
- Unused Chat Helpers
- Unused Suggest-Doctors
- Web Push (unused)
- Critical Path
- Onboarding Carousel

## God Nodes (most connected - your core abstractions)
1. `ShifaColors` - 28 edges
2. `06 Bookings, Payments, Video, Visit Summaries, Treatment Plans, Reviews, E-Signing spec` - 24 edges
3. `Text` - 21 edges
4. `Icon` - 19 edges
5. `ShifaSpacing` - 19 edges
6. `Widget kit (ui/widgets, ui/navigation)` - 18 edges
7. `ShifaRadius` - 17 edges
8. `05 Authentication & Account Lifecycle spec` - 17 edges
9. `Shifa Bemor KMP Rebuild Spec Index` - 16 edges
10. `AppointmentBookingFlowScreen (book / reschedule flow)` - 16 edges

## Surprising Connections (you probably didn't know these)
- `Build commands (assembleDebug, testDebugUnitTest, verifyRoborazziDebug, ktlintFormat)` --semantically_similar_to--> `lint-test job (ktlintCheck, testDebugUnitTest, verifyRoborazziDebug)`  [INFERRED] [semantically similar]
  README.md → .github/workflows/ci.yml
- `ShifaButtonsScreenshotTest` --references--> `ShifaButtons screenshot: primary/secondary/tertiary/action/icon buttons in default, disabled, danger, loading states`  [INFERRED]
  composeApp/src/androidUnitTest/kotlin/shifa/uz/ui/widgets/ShifaButtonsScreenshotTest.kt → composeApp/screenshots/ShifaButtons_all_states.png
- `ShifaCardsScreenshotTest` --references--> `ShifaCards screenshot: appointment (upcoming/past with status badges), doctor, document, certificate, service, check-in, task and settings cards`  [INFERRED]
  composeApp/src/androidUnitTest/kotlin/shifa/uz/ui/widgets/ShifaCardsScreenshotTest.kt → composeApp/screenshots/ShifaCards_all_cards.png
- `ShifaChatScreenshotTest` --references--> `ShifaChat bar screenshot: idle (mic), typed (clear X, teal border), recording (waveform)`  [INFERRED]
  composeApp/src/androidUnitTest/kotlin/shifa/uz/ui/widgets/ShifaChatScreenshotTest.kt → composeApp/screenshots/ShifaChat_chat_bar.png
- `ShifaChatScreenshotTest` --references--> `ShifaChat bubble screenshot: teal, gray and borderless bubbles, image-placeholder bubble, multi-line max-width bubble`  [INFERRED]
  composeApp/src/androidUnitTest/kotlin/shifa/uz/ui/widgets/ShifaChatScreenshotTest.kt → composeApp/screenshots/ShifaChat_chat_bubble.png

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Shifa AI Subscription Gating Points** — docs_kmp_spec_01_architecture_and_core_subscription_gating, docs_kmp_spec_02_navigation_and_shell_redirect_guard, docs_kmp_spec_02_navigation_and_shell_persistent_bottom_bar, docs_kmp_spec_02_navigation_and_shell_shifa_ai_screen [EXTRACTED 1.00]
- **Three App Lock Gates (root, resume, shell)** — docs_kmp_spec_03_app_lock_app_lock_lifecycle_layer, docs_kmp_spec_03_app_lock_lock_manager, docs_kmp_spec_03_app_lock_app_lock_wrapper [EXTRACTED 1.00]
- **BaseCard-derived Cards** — docs_kmp_spec_04_design_system_and_ui_kit_base_card, docs_kmp_spec_04_design_system_and_ui_kit_appointment_card, docs_kmp_spec_04_design_system_and_ui_kit_doctor_card, docs_kmp_spec_04_design_system_and_ui_kit_document_card, docs_kmp_spec_04_design_system_and_ui_kit_remote_care_task_card [EXTRACTED 1.00]
- **State holders whose copyWith silently resets error** — docs_kmp_spec_05_auth_authstate, docs_kmp_spec_06_bookings_payments_video_bookingsprovider, docs_kmp_spec_07_doctors_doctorsprovider, docs_kmp_spec_07_doctors_reviewsprovider [INFERRED 0.85]
- **Consultation payment flow (Click.uz / Stripe)** — docs_kmp_spec_06_bookings_payments_video_consultation_checkout, docs_kmp_spec_06_bookings_payments_video_checkout_webview, docs_kmp_spec_06_bookings_payments_video_payment_status_polling, docs_kmp_spec_06_bookings_payments_video_click_to_stripe_fallback, docs_kmp_spec_06_bookings_payments_video_payment_pending_screen [EXTRACTED 1.00]
- **Patient registration OTP flow** — docs_kmp_spec_05_auth_post_auth_check_identifier, docs_kmp_spec_05_auth_post_auth_send_sms_otp, docs_kmp_spec_05_auth_post_auth_send_email_otp, docs_kmp_spec_05_auth_sms_email_otp_fallback, docs_kmp_spec_05_auth_post_auth_register_patient [EXTRACTED 1.00]
- **Copilot send pipeline: stream, suggest doctors, resolve booking preview, confirm** — docs_kmp_spec_09_shifa_ai_copilot_copilotchatnotifier, docs_kmp_spec_09_shifa_ai_copilot_copilot_stream, docs_kmp_spec_09_shifa_ai_copilot_suggest_doctors_chat, docs_kmp_spec_09_shifa_ai_copilot_resolve_booking, docs_kmp_spec_09_shifa_ai_copilot_book_appointment [EXTRACTED 1.00]
- **AppHeader unread badge system (notifications + chat)** — docs_kmp_spec_10_home_appheader, docs_kmp_spec_13_notifications_notificationiconbutton, docs_kmp_spec_08_chat_chaticonbutton, docs_kmp_spec_13_notifications_unreadnotificationscountprovider, docs_kmp_spec_08_chat_unreadcountprovider [EXTRACTED 1.00]
- **Notification tap deep-link routing targets** — docs_kmp_spec_13_notifications_notificationtaphandler, docs_kmp_spec_13_notifications_gettargetlocation, docs_kmp_spec_11_documents_documentdetailsscreen, docs_kmp_spec_12_tasks_taskcheckinscreen, docs_kmp_spec_08_chat_chatscreen [EXTRACTED 1.00]
- **Account Deletion Email OTP Flow** — docs_kmp_spec_14_profile_and_account_profilescreen, docs_kmp_spec_14_profile_and_account_accountdeletionrepository, docs_kmp_spec_14_profile_and_account_deleteaccountotpverifyscreen, docs_kmp_spec_15_api_reference_p5_delete_account_request, docs_kmp_spec_15_api_reference_a4_send_email_otp, docs_kmp_spec_15_api_reference_p6_delete_account_confirm, docs_kmp_spec_14_profile_and_account_logout_routine [EXTRACTED 1.00]
- **Profile Location Picking Pipeline** — docs_kmp_spec_14_profile_and_account_locationpickersection, docs_kmp_spec_14_profile_and_account_mappickerscreen, docs_kmp_spec_14_profile_and_account_googlegeocodingservice, docs_kmp_spec_15_api_reference_google_geocoding_api, docs_kmp_spec_15_api_reference_openstreetmap_tiles, docs_kmp_spec_15_api_reference_g1_public_config, docs_kmp_spec_15_api_reference_p2_patch_profile [EXTRACTED 1.00]
- **Implementation Critical Path** — docs_kmp_spec_16_implementation_plan_core_01, docs_kmp_spec_16_implementation_plan_core_05, docs_kmp_spec_16_implementation_plan_core_06, docs_kmp_spec_16_implementation_plan_auth_01, docs_kmp_spec_16_implementation_plan_auth_02, docs_kmp_spec_16_implementation_plan_auth_03, docs_kmp_spec_16_implementation_plan_auth_04, docs_kmp_spec_16_implementation_plan_shell_01, docs_kmp_spec_16_implementation_plan_shell_02, docs_kmp_spec_16_implementation_plan_home_01, docs_kmp_spec_16_implementation_plan_doc_01, docs_kmp_spec_16_implementation_plan_doc_02, docs_kmp_spec_16_implementation_plan_book_01, docs_kmp_spec_16_implementation_plan_book_03, docs_kmp_spec_16_implementation_plan_book_04, docs_kmp_spec_16_implementation_plan_book_06, docs_kmp_spec_16_implementation_plan_book_08, docs_kmp_spec_16_implementation_plan_critical_path [EXTRACTED 1.00]
- **Manual workflow_dispatch CI/CD pipelines ported from apps-flutter-patient** — github_workflows_ci_ci_workflow, github_workflows_google_play_deploy_workflow, github_workflows_ios_testflight_workflow, claude_github_secrets, claude_apps_flutter_patient [EXTRACTED 1.00]
- **Data layer HTTP + auth token flow** — claude_apiconfig, claude_create_http_client, claude_with_refresh_helpers, claude_api_exception, claude_token_storage [EXTRACTED 1.00]
- **Figma variables mapped 1:1 into ui/theme tokens** — claude_design_tokens_rule, docs_implemented_design_system_figma_file, docs_implemented_design_system_color_kt, docs_implemented_design_system_typography_kt, docs_implemented_design_system_dimens_kt [INFERRED 0.95]
- **Form fields sharing default/filled/error/disabled state chrome** — composeapp_src_commonmain_kotlin_shifa_uz_ui_widgets_shifainputfield_shifainputfield, composeapp_src_commonmain_kotlin_shifa_uz_ui_widgets_shifadropdown_shifadropdown, composeapp_src_commonmain_kotlin_shifa_uz_ui_widgets_shifachat_shifachatbar, composeapp_screenshots_shifa_semantic_state_palette [INFERRED 0.75]
- **Appointment card family with status badges (confirmed/pending/cancelled)** — composeapp_src_commonmain_kotlin_shifa_uz_ui_widgets_shifacards_shifaupcomingappointmentcard, composeapp_src_commonmain_kotlin_shifa_uz_ui_widgets_shifacards_shifapastappointmentcard, composeapp_src_commonmain_kotlin_shifa_uz_ui_widgets_shifacards_shifaappointmentstatus [INFERRED 0.85]
- **Chat UI components (input bar + bubble types + image bubble)** — composeapp_src_commonmain_kotlin_shifa_uz_ui_widgets_shifachat_shifachatbar, composeapp_src_commonmain_kotlin_shifa_uz_ui_widgets_shifachat_shifachatbubble, composeapp_src_commonmain_kotlin_shifa_uz_ui_widgets_shifachat_shifachatbubbletype, composeapp_src_commonmain_kotlin_shifa_uz_ui_widgets_shifachat_shifachatimagebubble [INFERRED 0.85]
- **Shifa brand app/launcher icon set (Android densities, Play Store, iOS icon + launch logo)** — composeapp_src_androidmain_ic_launcher_playstore, composeapp_src_androidmain_res_mipmap_hdpi_ic_launcher, composeapp_src_androidmain_res_mipmap_hdpi_ic_launcher_round, composeapp_src_androidmain_res_mipmap_mdpi_ic_launcher, composeapp_src_androidmain_res_mipmap_mdpi_ic_launcher_round, composeapp_src_androidmain_res_mipmap_xhdpi_ic_launcher, composeapp_src_androidmain_res_mipmap_xhdpi_ic_launcher_round, composeapp_src_androidmain_res_mipmap_xxhdpi_ic_launcher, composeapp_src_androidmain_res_mipmap_xxhdpi_ic_launcher_round, composeapp_src_androidmain_res_mipmap_xxxhdpi_ic_launcher, composeapp_src_androidmain_res_mipmap_xxxhdpi_ic_launcher_round, iosapp_iosapp_assets_xcassets_appicon_appiconset_app_icon_1024, iosapp_iosapp_assets_xcassets_launchlogo_imageset_logo [INFERRED 0.95]
- **Roborazzi screenshot renders of Shifa design-system widgets** — composeapp_screenshots_shifapageelement_all_types, composeapp_screenshots_shifapageheader_all_variants, composeapp_screenshots_shifapager_all_pages, composeapp_screenshots_shifapopup_default, composeapp_screenshots_shifasearchfield_all_states, composeapp_screenshots_shifasection_all_types, composeapp_screenshots_shifaswitch_all_states, composeapp_screenshots_shifatag_all_types, composeapp_screenshots_shifatray_all_types [INFERRED 0.95]

## Communities (97 total, 28 thin omitted)

### Community 0 - "Widget Screenshots & Cards"
Cohesion: 0.05
Nodes (88): ShifaButtons screenshot: primary/secondary/tertiary/action/icon buttons in default, disabled, danger, loading states, ShifaCards screenshot: appointment (upcoming/past with status badges), doctor, document, certificate, service, check-in, task and settings cards, ShifaPageHeader screenshot: large 'Home' and 'Page Name' main headers with bell + avatar, centered sub header with back, check and more actions, ShifaPopUp screenshot: rounded white shadowed dialog with centered header and circular close button, ShifaSearchField screenshot: empty, filled with clear, disabled grey, and error (red border/text) pill search fields, ShifaTray screenshot: chat tray (suggested message chips, add button, 'Ask Anything' input with mic, send), 'Add to chat' tray with Camera/Photo/File outlined buttons, empty tray with header + close, SectionPreview(), Icon (+80 more)

### Community 1 - "API Helper & Token Refresh"
Cohesion: 0.06
Nodes (45): body, bodyastext, deleteWithRefresh(), getWithRefresh(), handleResponseWithRetry(), Result, T, patchWithRefresh() (+37 more)

### Community 2 - "Document & Image Pickers"
Cohesion: 0.06
Nodes (29): activityresultcontracts, addressof, betainteropapi, rememberDocumentPickerLauncher(), rememberImagePickerLauncher(), DocumentPickerResult, ImagePickerResult, rememberDocumentPickerLauncher() (+21 more)

### Community 3 - "Auth Spec (Repository & Flows)"
Cohesion: 0.06
Nodes (40): AccountInfoScreen (orphaned), Global 401 handling (ApiClient), AuthErrorSanitizer, AuthNotifier, AuthRepository, AuthState, Change password, Existing doctor to patient conversion (+32 more)

### Community 4 - "App Entry & Theme"
Cohesion: 0.09
Nodes (26): Bundle, ComponentActivity, AppAndroidPreview(), MainActivity, App(), ShifaTheme(), manrope(), manropeFontFamily() (+18 more)

### Community 5 - "Roborazzi Screenshot Tests"
Cohesion: 0.13
Nodes (16): captureroboimage, ShifaButtonsScreenshotTest, ShifaCardsScreenshotTest, ShifaNotificationScreenshotTest, ShifaPageElementScreenshotTest, ShifaPageHeaderScreenshotTest, ShifaPagerScreenshotTest, ShifaSearchFieldScreenshotTest (+8 more)

### Community 6 - "Dropdown/Switch/Image Widgets"
Cohesion: 0.09
Nodes (26): animatecolorasstate, animatefloatasstate, asyncpainterresource, biasalignment, box, clip, ShifaDimens, Modifier (+18 more)

### Community 7 - "Compose Preview Files"
Cohesion: 0.11
Nodes (20): composable, Placeholder(), compositionlocalprovider, dp, fillmaxwidth, height, ic_camera_22, ic_file_22 (+12 more)

### Community 8 - "Remote Care Tasks"
Cohesion: 0.12
Nodes (26): RemoteCareTaskCard, translate() falls back to English, then the raw key (never null), 12 Remote care tasks spec, Check-in eligibility window (isWithinAllowedCheckInWindow: scheduledAt <= now + 10 min), CheckInRequest (explicitNulls = false), Open vs Completed tab classification rules, RemoteCareTask, TaskCategory / TaskStatus / TaskInputType (NUMERIC, TEXT, BOOLEAN) / CheckInStatus (+18 more)

### Community 9 - "NavBar, Tray & PopUp"
Cohesion: 0.09
Nodes (22): columnscope, ShifaRadius, Modifier, dropshadow, horizontalscroll, ic_arrow_up_24, ic_nav_bookings, ic_nav_bookings_selected (+14 more)

### Community 10 - "Booking Slots & Appointments"
Cohesion: 0.11
Nodes (25): Appointment model, AppointmentBookingFlowScreen (book / reschedule flow), AppointmentStatus enum, AvailableSlot, 48 h cancel / reschedule rule, CreateBookingScreen (doctor picker), Client-side double-booking guard, GET /patients/me/schedule/doctors/{doctorId}/available (+17 more)

### Community 11 - "Spec Index & Core Architecture"
Cohesion: 0.14
Nodes (24): Shifa Bemor KMP Rebuild Spec Index, Suggested KMP Stack (Ktor, Koin, Navigation Compose, Coil 3), ApiClient (Dio wrapper), AppLogger and Redaction, AuthNotifier (_checkAuth, offline-tolerant), Bootstrap Sequence (main()), Copilot Memory (copilot_*_v1 keys), Crashlytics Declared but Never Initialized (+16 more)

### Community 12 - "Payments & Visit Summary"
Cohesion: 0.14
Nodes (22): AI visit summary (VisitSummaryScreen), AppointmentDetailsScreen, Checkout WebView (success/cancel URL interception), Click.uz to Stripe fallback, POST /payments/consultation/checkout, ConsultationPaymentScreen (/bookings/:id/pay), Local notifications (video join reminder, care-plan checklist), Money in minor units (+14 more)

### Community 13 - "Spacing & Size Tokens"
Cohesion: 0.10
Nodes (19): animatefloat, circleshape, Gap, Inset, ShifaShadow, ShifaSize, ShifaSpacing, Stack (+11 more)

### Community 14 - "Notification Widget"
Cohesion: 0.16
Nodes (20): ShifaNotification screenshot: task, document, appointment, chat, feedback (highlighted amber border) and message-with-avatar notifications, Color, Modifier, NotificationBody(), notificationChrome(), ShifaMessageNotification(), ShifaNotification(), ShifaNotificationType (+12 more)

### Community 15 - "Release CI/CD & Firebase"
Cohesion: 0.19
Nodes (20): CI/CD (manual workflow_dispatch GitHub Actions), Firebase (google-services, App Distribution), CI secrets (Firebase, keystore, Play service account, iOS cert, App Store Connect), ShifaTheme (light only, no dark scheme), Version name in build.gradle.kts and Config.xcconfig MARKETING_VERSION, CORE-01 Project setup & CI (partly done), REL-03 package ID decision (pending), bundleRelease AAB build (-PversionBuild) (+12 more)

### Community 16 - "Shifa AI Copilot Chat"
Cohesion: 0.11
Nodes (20): POST book-appointment (confirm), POST /patients/me/copilot/stream (SSE), copilotBackendLanguage (EN/RU/UZ), CopilotChatNotifier, CopilotChatState, sealed CopilotError (port), CopilotMemoryStore (SharedPreferences local memory), CopilotMessage (+12 more)

### Community 17 - "Android File Saver"
Cohesion: 0.18
Nodes (11): build, FileSaver, ByteArray, Context, Result, rememberFileSaver(), contentvalues, environment (+3 more)

### Community 18 - "Project Setup & Localization Tickets"
Cohesion: 0.13
Nodes (19): Shifa Bemor Patient App (shifa_patient_app_v1 v1.1.9+8), 01 Architecture, App Core and Cross-Cutting Infrastructure, AppLanguage enum, Q4 Free-text language field, CORE-01 Project setup & CI, CORE-04 Strings & localization, CORE-06 Storage & DI, Definition of Done (every ticket) (+11 more)

### Community 19 - "Booking & Payment API"
Cohesion: 0.24
Nodes (19): 06 Bookings, Payments, Video, Visit Summaries, Treatment Plans, Reviews, E-Signing spec, Appointment Schema, Checkout WebView Protocol (Click-to-Stripe fallback), Date and Time Formats, Money in Integer Minor Units, Click.uz and Stripe Gateways, Payment Endpoints ($1-$3), Scheduling & Appointment Endpoints (B1-B13) (+11 more)

### Community 20 - "Location Picking & Public Config"
Cohesion: 0.19
Nodes (19): 09 Shifa AI copilot spec, GoogleGeocodingService, Location Picking Flow, LocationPickerSection, Q5 Map selection silently lost without Google key, API Contract Inconsistencies, Shifa AI Copilot Endpoints (X1-X7), Dead or Unused API Code (+11 more)

### Community 21 - "Profile Screens & Store"
Cohesion: 0.20
Nodes (19): Avatar Upload Flow, EditProfileScreen (/account/edit), Profile Language Dialog, LanguageMiniToggle, languageProvider, profileProvider (ProfileNotifier), ProfileRepository, ProfileScreen (/account) (+11 more)

### Community 22 - "iOS Host App"
Cohesion: 0.14
Nodes (14): App, ComposeApp, ComposeView, ContentView, .body, Context, iOSApp, .body (+6 more)

### Community 23 - "Color Tokens & Page Element"
Cohesion: 0.14
Nodes (16): column, ShifaPageElement screenshot: header with back button, flex header, body, action buttons (filled + outlined), footer CTA, Alpha, Bg, Border, Interactive, ShifaColors, Modifier (+8 more)

### Community 24 - "Chat Bubble & Bar Widgets"
Cohesion: 0.21
Nodes (16): ShifaChat bubble screenshot: teal, gray and borderless bubbles, image-placeholder bubble, multi-line max-width bubble, BarIcon(), Bubble(), DrawableResource, Modifier, ShifaChatBar(), ShifaChatBubble(), ShifaChatBubbleType (+8 more)

### Community 25 - "Auth Endpoints & Phone Input"
Cohesion: 0.25
Nodes (17): 05 Authentication & Account Lifecycle spec, AppVersionFooter, parseFullPhone 4-digit dial code bug (Q10), PhoneInputField, A1 POST /auth/login?app=patient, A4 POST /auth/send-email-otp, A8 POST /auth/reset-password (authenticated), Auth Endpoints (A1-A10) (+9 more)

### Community 26 - "Doctors Discovery"
Cohesion: 0.20
Nodes (17): 07 Doctors: discovery, profile, reviews, certificates, catalogs spec, CertificateViewerScreen, Filter composition (_composeFilters), Client-side country filter (country_catalog), DoctorCard, DoctorProfileScreen (/doctors/:id), DoctorQuickFilterChips, DoctorsPaginationBar (+9 more)

### Community 27 - "Input & Search Fields"
Cohesion: 0.20
Nodes (14): basictextfield, clickable, collectisfocusedasstate, TextStyle, lineBox(), Modifier, ShifaInputField(), ic_search_16 (+6 more)

### Community 28 - "Documents, Video & 401 Rules"
Cohesion: 0.21
Nodes (16): 03 App Lock (PIN + biometrics), 11 Documents spec, 401 Auto-Logout Rules, Daily.co Video, Document Schema, Documents Endpoints (F1-F4), V1 POST /video/token, BOOK-02 Booking attachments (+8 more)

### Community 29 - "Documents & Attachments"
Cohesion: 0.15
Nodes (16): POST /messages/upload-attachment, App lock suspended while file/camera pickers are open, appLockTemporaryDisableProvider, Bearer token sent to arbitrary file hosts, DocumentCard, DocumentCategory (22 codes + PRE_APPOINTMENT), DocumentDetailsScreen, DocumentModel (+8 more)

### Community 30 - "Widget Kit Status (Implemented)"
Cohesion: 0.17
Nodes (15): Known differences from Figma, Open design questions (ImageContainer Fit/Crop, Dropdown inline/popup, search icon colour), ShifaButtons (Primary/Secondary/Tertiary/Action/Icon), ShifaCards (appointment, doctor, document, certificate, check-in, task, settings), ShifaChat (ChatBar, ChatBubble, ChatImageBubble), ShifaChips (ShifaChip, ShifaChipGrid, ShifaCarousel), ShifaInputField, ShifaNotification / ShifaMessageNotification (+7 more)

### Community 31 - "App Lock State Machine"
Cohesion: 0.25
Nodes (15): Platform Permissions Table, AppLockController (KMP single gate), AppLockLifecycleLayer (root gate), AppLockService, AppLockSettingsScreen (/account/app-lock), App Lock State Machine, appLockStateProvider, Temporary Disable (appLockTemporaryDisableProvider) (+7 more)

### Community 32 - "Chat Conversation & Voice"
Cohesion: 0.17
Nodes (15): ChatConversationScreen, Text / Image / Voice / Document message bubbles, normalizePhotoUrl / withCacheBuster, Optimistic message send reconciled by id, optimisticMessagesProvider, PublicBackendConfig (publicBaseUrl hard-coded localhost:8090), VoiceRecorder / VoicePlayer expect/actual (port), VoiceRecordingDialog (+7 more)

### Community 33 - "Notifications, FCM & Logout"
Cohesion: 0.23
Nodes (15): 13 Notifications (inbox, FCM push, local reminders) spec, Stale backend snapshot bin/main/com/shifa/**, Logout Routine, FCM Data Payload Contract, Firebase / FCM, Notification Schema, Notifications & Document Access Endpoints (N1-N6), P7 PUT /patients/me/fcm-token (+7 more)

### Community 34 - "Profile Model & HTTP API"
Cohesion: 0.18
Nodes (15): 14 Profile & Account Spec, normalizePhotoUrl, PatientProfileModel, UserAvatar widget, withCacheBuster, 15 HTTP API Reference, Dio ApiClient, AuthErrorSanitizer (+7 more)

### Community 35 - "Bottom NavBar"
Cohesion: 0.17
Nodes (11): ShifaNavBar screenshot: floating pill bottom bar, 5 tabs (Home, Bookings, Shifa AI, Documents, Messages) each shown selected in teal, NavBarPreview(), ShifaNavBarScreenshotTest, Modifier, ShifaNavBar(), ShifaTab, Bookings, Documents (+3 more)

### Community 36 - "Switch, PopUp & Gallery"
Cohesion: 0.15
Nodes (9): ShifaSwitch screenshot: off (grey track) and on (teal track) toggles, PopUpPreview(), SwitchPreview(), Gallery(), Color, ShifaPopUpScreenshotTest, ShifaSwitchScreenshotTest, Modifier (+1 more)

### Community 37 - "Tag Widget"
Cohesion: 0.21
Nodes (10): background, ShifaTag screenshot: teal, blue and amber pill tags, Modifier, ShifaTag(), ShifaTagType, Primary, Secondary, Tertiary (+2 more)

### Community 38 - "Project Rules & Fixed Stack"
Cohesion: 0.17
Nodes (12): docs/kmp-spec/15-api-reference.md, Compose Multiplatform + Material 3, docs/kmp-spec (source of truth for API and features), Figma MCP (UI source), Kotlin Multiplatform (composeApp, Android + iOS), Ktor Client (OkHttp/Darwin, Auth bearer, HttpTimeout), gradle/libs.versions.toml (version catalog), Project rules (CLAUDE.md) (+4 more)

### Community 39 - "Backend & Base Cards"
Cohesion: 0.18
Nodes (12): Shifa Spring Boot REST Backend, API Base URL Resolution (API_BASE_URL / production), CI/CD Workflows (Flutter), publicBaseUrl / normalizePhotoUrl (localhost:8090), Public Config Endpoint (GET /api/public/config), AppointmentCard, BaseCard, DoctorCard (+4 more)

### Community 40 - "Verified Bugs & Error Localization"
Cohesion: 0.18
Nodes (12): Verified Bugs - Do Not Replicate, AppLanguage / LanguageNotifier, AuthErrorSanitizer, Connectivity Provider (unused by UI), AppLocalizations (en/uz/ru/de string map), Placeholder Syntaxes ({{x}}, {x}, ${...}, %s), Tests as Behavioral Contracts, translateError (error localization) (+4 more)

### Community 41 - "Flutter Design Tokens (Spec)"
Cohesion: 0.23
Nodes (12): AppDesignSystem Tokens, AppTheme.lightTheme (Material 3), Color Tokens (primary #00BBB0 etc.), EmptyState, SectionTitle, SegmentedControl, ShifaActionButton (unused), ShifaPrimaryButton (+4 more)

### Community 42 - "Home Screen & Bookings List"
Cohesion: 0.17
Nodes (12): bookingsProvider / BookingsState, BookingsScreen (upcoming / past list), GET /patients/me/appointments, AppHeader (greeting + notification/chat badges), AppointmentCard, Doctors near me (Haversine distance, 50 km radius, top 3), EmptyState, Time-of-day greeting with patient name (+4 more)

### Community 43 - "Notifications Inbox & Badges"
Cohesion: 0.21
Nodes (12): unreadCountProvider, Document access request approve / reject flow, documentAccessRequestActedIdsProvider, PUT /patients/me/fcm-token, NotificationDetailsSheet, NotificationIconButton (unread badge), NotificationsController, notificationsProvider (+4 more)

### Community 44 - "MVVM + Repository Pattern"
Cohesion: 0.31
Nodes (11): Sealed ApiException (ValidationError, Unauthorized, NotFound, ...), ApiConfig (BASE_URL, TIMEOUT_MS, Endpoints), createHttpClient(tokenProvider) / expect createHttpEngine, MVVM + Repository architecture, XxxScreen / stateless XxxScreenContent split, State-based navigation (rememberSaveable + Scaffold bottom bar, no nav library), XxxUiState @Stable data class pattern, ViewModel pattern (MutableStateFlow, viewModelScope, result.fold) (+3 more)

### Community 45 - "Splash, Lock Screen & Header"
Cohesion: 0.24
Nodes (11): REST Polling (no websockets), Back-button Behaviour, SplashScreen, AppLockScreen, Bug: 5-6 Digit PIN Can Never Unlock (auto-verify at 4 digits), AppHeader, Brand Assets (shifa_logo.png, Shifa AI Icon.svg), ChatIconButton (+3 more)

### Community 46 - "Shifa AI Gating & Redirect Guard"
Cohesion: 0.31
Nodes (11): parsePatientTier (fail-open to premium), patientFeatureProvider (features list first), Subscription / Feature Gating (PatientTier, PatientFeature), Allowed-route Whitelist, PersistentBottomBar, Q4 Tab Tap Resets Branch Stack, Redirect Guard, ShifaAiNavFab (centre FAB) (+3 more)

### Community 47 - "Chat API & Tickets"
Cohesion: 0.36
Nodes (11): 08 Chat (doctor-patient messaging) spec, Chat Endpoints (C1-C7), ChatContact Schema, ChatMessage Schema (new and legacy formats), CHAT-01 Conversation list + start a chat, CHAT-02 Conversation: text, polling, read, CHAT-03 Image messages, CHAT-04 Voice messages (+3 more)

### Community 48 - "Section Widget"
Cohesion: 0.27
Nodes (9): alignment, ShifaSection screenshot: white, teal-tinted and transparent 'Upcoming Appointment' sections with arrow and doctor card, plus 'My Tasks' section with chevron row, Modifier, ShifaSection(), ShifaSectionType, CardOnColor, CardOnWhite, CardTransparent (+1 more)

### Community 49 - "Storage & DI Decision"
Cohesion: 0.24
Nodes (10): apps-flutter-patient (Flutter source repo), Multiplatform Settings (no-arg), No DI framework (constructor defaults), TokenStorage (Settings wrapper), CORE-03 Shared UI kit (done, Figma kit), CORE-04 Strings & localization (not started), CORE-06 Storage & DI (template only), CORE-07 and M1+ (not started) (+2 more)

### Community 50 - "Figma Token Pipeline"
Cohesion: 0.31
Nodes (10): No raw hex/dp/sp; tokens named 1:1 after Figma variables, Color.kt / ShifaColors, Design system doc, Dimens.kt (ShifaSpacing, ShifaSize, ShifaRadius, ShifaShadow), Figma file o0MqTfUEfhBX49VljFhp70 (tokens 1572:13099, components 1572:11699), Figma-exported icons (composeResources/drawable ic_<name>_<size>), Modifier.lineBox(style), ShifaDimens (raw Figma values without variable) (+2 more)

### Community 51 - "Time Provider"
Cohesion: 0.20
Nodes (4): clock, instant, nsdate, timeintervalsince1970

### Community 52 - "Form Field State Styling"
Cohesion: 0.22
Nodes (6): Shifa semantic state palette (teal interactive, red danger/error, gray disabled), ShifaChat bar screenshot: idle (mic), typed (clear X, teal border), recording (waveform), ShifaDropdown screenshot: closed, expanded, expanded with checked option, filled, error (red border), ShifaInputField screenshot: empty, filled (floating label), error (red border + red helper), disabled, ShifaDropdownScreenshotTest, ShifaInputFieldScreenshotTest

### Community 53 - "Platform expect/actual"
Cohesion: 0.31
Nodes (7): AndroidPlatform, getPlatform(), getPlatform(), Platform, getPlatform(), IOSPlatform, uidevice

### Community 54 - "Shell Routes & Deep Links"
Cohesion: 0.22
Nodes (10): MainShell, NotificationTapHandler (payload to path), pendingNotificationTapProvider, Plain Shell (ShellRoute, non-persistent), Q2 /bookings/confirm Shadowed by /bookings/:id, Q9 Chat Deep Link chatId Ignored, Route Table (42 routes), shellTabIndexProvider (+2 more)

### Community 55 - "Chat Polling & Contacts"
Cohesion: 0.22
Nodes (10): Chat REST endpoints (/messages: conversations, conversation/{id}, send, read, search, unread-count), REST polling (conversation 5 s, unread count 10 s), ChatIconButton (unread badge), chatRepositoryProvider (ApiClient chat calls), ChatScreen (conversation list + user search), conversationProvider, conversationsProvider, userSearchProvider (+2 more)

### Community 56 - "Doctors API & Home Nearby"
Cohesion: 0.33
Nodes (10): 10 Home screen spec, Doctor Schema, Doctors Public Directory Endpoints (D1-D5), Pagination (GET /public/doctors only), PublicDoctorLocation Schema, Review Schema, DOC-01 Doctors list: search, filters, pagination, DOC-02 Doctor profile + reviews (+2 more)

### Community 57 - "Push & Local Notifications"
Cohesion: 0.24
Nodes (10): FCM data payload, getTargetLocation, getTargetPathFromPayload, LocalNotificationService, Notification channels shifa_patient_channel / appointments_channel, NotificationModel (fromFcmData), NotificationTapHandler, PushNotificationService (FCM) (+2 more)

### Community 58 - "Chips & Carousel"
Cohesion: 0.31
Nodes (8): border, Dp, Modifier, T, ShifaCarousel(), ShifaChipGrid(), ic_check_16, spacer

### Community 59 - "Lint, Tests & CI Workflow"
Cohesion: 0.31
Nodes (9): ktlint, XxxScreenshotTest pattern (Roborazzi + Robolectric), XxxViewModelTest pattern (StandardTestDispatcher, Turbine, Truth), Gallery preview container, Per-widget *ScreenshotTest (composeApp/screenshots/), CI workflow (ci.yml), lint-test job (ktlintCheck, testDebugUnitTest, verifyRoborazziDebug), Screenshot diff artifacts (*_compare.png on failure) (+1 more)

### Community 60 - "Account Deletion OTP"
Cohesion: 0.31
Nodes (9): AccountDeletionRepository, Delete Account Email OTP Flow, DeleteAccountOtpVerifyScreen, DeleteAccountRequestResult, Q16 Account deletion requires email, ID Types (numeric Long IDs), P5 POST /patients/me/delete-account/request, P6 POST /patients/me/delete-account/confirm (+1 more)

### Community 61 - "Pager Widget"
Cohesion: 0.29
Nodes (7): arrangement, ShifaPager screenshot: 4-dot page indicator, active teal dot at each of 4 positions, Modifier, ShifaPager(), contentdescription, semantics, size

### Community 62 - "Chip Widget Tests"
Cohesion: 0.29
Nodes (6): ShifaChips screenshot: small/large filter chips (selected with check), time-slot grid, scrollable specialty carousel, ShifaChipsScreenshotTest, ShifaChip(), ShifaChipType, Filter, Slot

### Community 63 - "Image Container Widget"
Cohesion: 0.33
Nodes (4): ShifaImageContainer screenshot: empty white square placeholder on gray background, ShifaImageContainerScreenshotTest, Modifier, ShifaImageContainer()

### Community 64 - "FileSaver (common)"
Cohesion: 0.40
Nodes (4): FileSaver, ByteArray, Result, rememberFileSaver()

### Community 65 - "Design System Tickets"
Cohesion: 0.60
Nodes (6): 02 Navigation and App Shell, 04 Design System and UI Kit, CORE-02 Design system, CORE-03 Shared UI kit, SHELL-01 Tab shell & bottom bar, SHELL-03 App header

### Community 66 - "Chat Message Models"
Cohesion: 0.40
Nodes (5): ChatContact, ChatMessage (new vs legacy wire format), ConversationWithMessages, MessageType / MessageStatus / SenderRole, MessageContent

### Community 67 - "Subscription Tier Gate"
Cohesion: 0.50
Nodes (5): canUseShifaAi (port), parsePatientTier (fail-open), PatientTier / PatientFeature PATIENT_SHIFA_AI, Shifa AI PREMIUM gating inconsistency, Shifa AI Subscription Tier Gate

### Community 69 - "Gradle Wrapper"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Ambiguous Edges - Review These
- `A4 POST /auth/send-email-otp` → `P6 POST /patients/me/delete-account/confirm`  [AMBIGUOUS]
  docs/kmp-spec/15-api-reference.md · relation: shares_data_with
- `Upload to Google Play production (r0adkll/upload-google-play, com.shifa.patientapp)` → `REL-03 package ID decision (pending)`  [AMBIGUOUS]
  docs/implemented/README.md · relation: conceptually_related_to

## Knowledge Gaps
- **166 isolated node(s):** `Endpoints`, `ValidationError`, `Unauthorized`, `TokenExpired`, `TokenInvalid` (+161 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 342 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **28 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `A4 POST /auth/send-email-otp` and `P6 POST /patients/me/delete-account/confirm`?**
  _Edge tagged AMBIGUOUS (relation: shares_data_with) - confidence is low._
- **What is the exact relationship between `Upload to Google Play production (r0adkll/upload-google-play, com.shifa.patientapp)` and `REL-03 package ID decision (pending)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Shifa Bemor KMP Rebuild Spec Index` connect `Spec Index & Core Architecture` to `Design System Tickets`, `Profile Model & HTTP API`, `Verified Bugs & Error Localization`, `Shifa AI Gating & Redirect Guard`, `Project Setup & Localization Tickets`, `Documents, Video & 401 Rules`?**
  _High betweenness centrality (0.039) - this node is a cross-community bridge._
- **Why does `05 Authentication & Account Lifecycle spec` connect `Auth Endpoints & Phone Input` to `Notifications, FCM & Logout`, `Auth Spec (Repository & Flows)`?**
  _High betweenness centrality (0.033) - this node is a cross-community bridge._
- **Why does `06 Bookings, Payments, Video, Visit Summaries, Treatment Plans, Reviews, E-Signing spec` connect `Booking & Payment API` to `Notifications, FCM & Logout`, `Auth Spec (Repository & Flows)`, `Booking Slots & Appointments`, `Home Screen & Bookings List`, `Payments & Visit Summary`, `Chat API & Tickets`, `Doctors Discovery`, `Documents, Video & 401 Rules`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Are the 20 inferred relationships involving `Text` (e.g. with `Placeholder()` and `ShifaNavBar()`) actually correct?**
  _`Text` has 20 INFERRED edges - model-reasoned connections that need verification._
- **Are the 18 inferred relationships involving `Icon` (e.g. with `ShifaNavBar()` and `ButtonContent()`) actually correct?**
  _`Icon` has 18 INFERRED edges - model-reasoned connections that need verification._