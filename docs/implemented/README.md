# What's implemented

Read this before starting a task. Update it at the end of every job, in the same commit:
the status table, the file for the area you touched, and one line in the log.

Ticket IDs are from `docs/kmp-spec/16-implementation-plan.md`.

| Area | File |
|---|---|
| Design tokens and widget kit | [design-system.md](design-system.md) |
| Splash, login, registration and forgot password | [auth.md](auth.md) |
| Tab shell and Home | [home.md](home.md) |

## Status

| Ticket | Status | Notes |
|---|---|---|
| CORE-01 Project setup & CI | Partly done | KMP project, Firebase configs and the CI/CD workflows (see CLAUDE.md) are in place. CI has never run. `App()` switches Splash → Login ↔ Register / Forgot password → the tab shell. Package IDs are still `shifa.uz` (REL-03 decision pending). |
| CORE-02 Design system | Done | Tokens come from Figma, not Flutter. No in-app catalog screen: `@Preview`s and Roborazzi screenshots replace it. |
| CORE-03 Shared UI kit | Done (Figma kit) | Every Figma component is built. Not built, because Figma has no component for them: `EmptyState`, `SegmentedControl`, `AppVersionFooter`, `LanguageMiniToggle`. |
| CORE-04 Strings & localization | Done, except the language override | `composeResources/values{,-uz,-ru,-de}/strings.xml` (en/uz/ru 837 keys, de 492 falls back to en). `StringsTest` checks keys and placeholders. Added the missing en/ru keys plus `priceNotSet`, `showPassword`, `hidePassword`. Renamed `ent specialist` / `general practitioner` to snake_case. The in-app language override (`app_language`) is not built. Widget texts ("See all", "Clear", …) are still hardcoded English. Escaping: `strings/README.md` says to write `\'`, `\"` and `%%`, but CMP 1.9.3 doesn't unescape those, so they're written literally. Only `&`, `<`, `>` are XML-escaped. |
| CORE-05 HTTP client & error model | Partly done | Production `BASE_URL`, 30 s connect/receive timeouts, and a Bearer token on backend requests only, never on the 16 public `/auth/*` paths or other hosts. The old refresh-token flow and the Ktor Auth plugin are removed, because the backend has none. `apiCall` maps failures to `ApiException`; `ErrorMessages.kt` sanitizes backend messages (01 §8) and localizes them. Not built: the global 401 → logout interceptor (grace period and rate limit) and the photo URL resolver. |
| CORE-06 Storage & DI | Partly done | The token lives in `shifa_auth_secure`: Keychain on iOS, EncryptedSharedPreferences on Android. Other keys use plain Settings. No Koin: CLAUDE.md says no DI framework. Not built: `auth_token_saved_at`, `user_id`, and the Android backup exclusions. |
| CORE-07 and everything from M1 onwards | Not started | Except the ones below. |
| AUTH-01 Splash & session restore | Done | The token is checked against `/patients/me/profile` while the splash plays, with a 5 s cap. Only a 401 signs out; offline keeps the session (05 §2.1). The first launch after a reinstall wipes the token. |
| AUTH-02 Login | Mostly done | Real login, identifier normalization, the persisted 5-attempt lockout and localized errors. Goes to Home. Not built: routing on `forcePasswordReset` (needs AUTH-07), the language toggle and the version footer. |
| AUTH-03 Create account | Mostly done | Figma 3-step wizard (account → info → photo), password checklist, check-identifier dialogs, the email/SMS choice with the email fallback. Not built: `PhoneInputField` with a country picker (a plain phone field instead, so the 04 parsing bug can't happen). |
| AUTH-04 Register OTP verify | Mostly done | Code screen, masked destination, 300 s resend, `register-patient`, then the photo (P3) and step 2 (P2) are saved. Not built: the PIN setup prompt (needs LOCK-01). Not yet tried against the backend. |
| AUTH-05 Doctor → patient | Mostly done | The step-1 "existing doctor" dialog replaces the confirm screen, then the email code and `create-patient-for-doctor`. Phone-only matches stop with "email required" (Q7). No PIN prompt (LOCK-01). |
| SHELL-01 Tab shell & bottom bar | Partly done | Figma's 5-tab bar; only Home is built, the rest and Home's links are "Coming soon" placeholders. Back goes page → Home tab → exit. The centre Shifa AI tab is shown (Figma). |
| SHELL-03 App header | Done | Avatar → Account placeholder, bell → Notifications placeholder, with the N2 unread badge. No chat icon (not in the Figma header). |
| HOME-01 Home + Upcoming | Done | P1 greeting, pull to refresh, B2 upcoming (confirmed, until the join window closes), "Now" + Join, error banner. Cards open a placeholder until BOOK-02. |
| HOME-02 My Tasks | Done | T1, open tasks, up to 3. Opens the Tasks placeholder (TASK-01). |
| HOME-03 My Doctors | Done (per Figma) | Figma replaces "Doctors near me" with "My Doctors": the doctors of the latest bookings, via D2. No location flow. |
| HOME-04 Shared-state check | Done | Home has its own state. |
| AUTH-06 Forgot password | Mostly done | Identifier → code → new password → auto-login (A9, A10), no Figma frames. Fixes Q8, Q10, Q12, Q13. Not built: the success SnackBar. Not yet tried against the backend. |

## Log

- 2026-09-26: Reset the old KMP app to a template. Copied Firebase configs and CI/CD from `apps-flutter-patient`. Built design tokens and the full Figma widget kit with screenshot tests.
- 2026-09-27: Set up git on branch `shifa2.0` of `SHIFA-UZ/apps-flutter-patient`. Added this folder.
- 2026-09-27: Built the graphify knowledge graph in `graphify-out/` (read `GRAPH_REPORT.md`, query with `graphify query "…"`, refresh with `/graphify . --update`).
- 2026-09-27: Converted strings to CMP resources (CORE-04). Built the splash animation and the Login screen UI (AUTH-01/02, no backend). Native launch screens now match the splash's first frame.
- 2026-09-27: Connected Login to the backend (AUTH-01/02, CORE-05/06 in part): spec HTTP client, secure token storage, error sanitizer, lockout, session restore, and a placeholder Home with Log out.
- 2026-09-27: Updated CLAUDE.md to match the new data layer: no Auth plugin or `*WithRefresh` helpers, `apiCall` + `ErrorMessages.kt`, secure token storage.
- 2026-09-27: Built registration (AUTH-03/04/05): the 3 Figma steps, OTP screen, doctor link, profile photo + details save, iOS PHPicker, and `ShifaBanner` (Login now uses it).
- 2026-09-27: Register step 2 lists every country (from the OS, searchable tray) instead of 4. Built forgot password (AUTH-06) and wired Login's "Forgot password?".
- 2026-09-27: Built the tab shell and Home (SHELL-01/03, HOME-01..04): upcoming visits, my tasks, my doctors, the unread badge and pull to refresh, with placeholders for unbuilt pages. The auth-only 401/404 error texts are now limited to `/auth/*`.
