# What's implemented

Read this before starting a task. Update it at the end of every job, in the same commit:
the status table, the file for the area you touched, and one line in the log.

Ticket IDs are from `docs/kmp-spec/16-implementation-plan.md`.

| Area | File |
|---|---|
| Design tokens and widget kit | [design-system.md](design-system.md) |
| Splash and login | [auth.md](auth.md) |

## Status

| Ticket | Status | Notes |
|---|---|---|
| CORE-01 Project setup & CI | Partly done | KMP project, Firebase configs and the CI/CD workflows (see CLAUDE.md) are in place. CI has never run. `App()` switches Splash → Login → Home. Package IDs are still `shifa.uz` (REL-03 decision pending). |
| CORE-02 Design system | Done | Tokens come from Figma, not Flutter. No in-app catalog screen: `@Preview`s and Roborazzi screenshots replace it. |
| CORE-03 Shared UI kit | Done (Figma kit) | Every Figma component is built. Not built, because Figma has no component for them: `EmptyState`, `SegmentedControl`, `AppVersionFooter`, `LanguageMiniToggle`. |
| CORE-04 Strings & localization | Done, except the language override | `composeResources/values{,-uz,-ru,-de}/strings.xml` (en/uz/ru 837 keys, de 492 falls back to en). `StringsTest` checks keys and placeholders. Added the missing en/ru keys plus `priceNotSet`, `showPassword`, `hidePassword`. Renamed `ent specialist` / `general practitioner` to snake_case. The in-app language override (`app_language`) is not built. Widget texts ("See all", "Clear", …) are still hardcoded English. Escaping: `strings/README.md` says to write `\'`, `\"` and `%%`, but CMP 1.9.3 doesn't unescape those, so they're written literally. Only `&`, `<`, `>` are XML-escaped. |
| CORE-05 HTTP client & error model | Partly done | Production `BASE_URL`, 30 s connect/receive timeouts, and a Bearer token on backend requests only, never on the 16 public `/auth/*` paths or other hosts. The old refresh-token flow and the Ktor Auth plugin are removed, because the backend has none. `apiCall` maps failures to `ApiException`; `ErrorMessages.kt` sanitizes backend messages (01 §8) and localizes them. Not built: the global 401 → logout interceptor (grace period and rate limit) and the photo URL resolver. |
| CORE-06 Storage & DI | Partly done | The token lives in `shifa_auth_secure`: Keychain on iOS, EncryptedSharedPreferences on Android. Other keys use plain Settings. No Koin: CLAUDE.md says no DI framework. Not built: `auth_token_saved_at`, `user_id`, and the Android backup exclusions. |
| CORE-07 and everything from M1 onwards | Not started | Except the two below. |
| AUTH-01 Splash & session restore | Done | The token is checked against `/patients/me/profile` while the splash plays, with a 5 s cap. Only a 401 signs out; offline keeps the session (05 §2.1). The first launch after a reinstall wipes the token. |
| AUTH-02 Login | Mostly done | Real login, identifier normalization, the persisted 5-attempt lockout and localized errors. Goes to a placeholder Home. Not built: routing on `forcePasswordReset` (needs AUTH-07), the language toggle and the version footer. |

## Log

- 2026-09-26: Reset the old KMP app to a template. Copied Firebase configs and CI/CD from `apps-flutter-patient`. Built design tokens and the full Figma widget kit with screenshot tests.
- 2026-09-27: Set up git on branch `shifa2.0` of `SHIFA-UZ/apps-flutter-patient`. Added this folder.
- 2026-09-27: Built the graphify knowledge graph in `graphify-out/` (read `GRAPH_REPORT.md`, query with `graphify query "…"`, refresh with `/graphify . --update`).
- 2026-09-27: Converted strings to CMP resources (CORE-04). Built the splash animation and the Login screen UI (AUTH-01/02, no backend). Native launch screens now match the splash's first frame.
- 2026-09-27: Connected Login to the backend (AUTH-01/02, CORE-05/06 in part): spec HTTP client, secure token storage, error sanitizer, lockout, session restore, and a placeholder Home with Log out.
