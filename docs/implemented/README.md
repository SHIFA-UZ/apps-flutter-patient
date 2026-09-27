# What's implemented

Read this before starting a task. Update it at the end of every job, in the same commit:
the status table, the file for the area you touched, and one line in the log.

Ticket IDs are from `docs/kmp-spec/16-implementation-plan.md`.

| Area | File |
|---|---|
| Design tokens and widget kit | [design-system.md](design-system.md) |

## Status

| Ticket | Status | Notes |
|---|---|---|
| CORE-01 Project setup & CI | Partly done | KMP project, Firebase configs and the CI/CD workflows (see CLAUDE.md) are in place. CI has never run. `App()` is an empty `ShifaTheme`. Package IDs are still `shifa.uz` (REL-03 decision pending). |
| CORE-02 Design system | Done | Tokens come from Figma, not Flutter. No in-app catalog screen: `@Preview`s and Roborazzi screenshots replace it. |
| CORE-03 Shared UI kit | Done (Figma kit) | Every Figma component is built. Not built, because Figma has no component for them: `EmptyState`, `SegmentedControl`, `AppVersionFooter`, `LanguageMiniToggle`. |
| CORE-04 Strings & localization | Not started | Widget texts ("See all", "Clear", …) are still hardcoded English. |
| CORE-05 HTTP client & error model | Template only | `HttpClient.kt`, `ApiHelper.kt` and `ApiError.kt` come from the old app: 300 s timeout, refresh-token flow on `/api/refresh-token`, placeholder `BASE_URL`. None of it is adapted to the spec yet (30 s timeout, 401 → logout rules, public `/auth/*` paths). |
| CORE-06 Storage & DI | Template only | `TokenStorage` uses plain Multiplatform Settings, not secure storage. The spec asks for Koin, but CLAUDE.md says no DI framework; ask before adding it. |
| CORE-07 and everything from M1 onwards | Not started | |

## Log

- 2026-09-26: Reset the old KMP app to a template. Copied Firebase configs and CI/CD from `apps-flutter-patient`. Built design tokens and the full Figma widget kit with screenshot tests.
- 2026-09-27: Set up git on branch `shifa2.0` of `SHIFA-UZ/apps-flutter-patient`. Added this folder.
- 2026-09-27: Built the graphify knowledge graph in `graphify-out/` (read `GRAPH_REPORT.md`, query with `graphify query "…"`, refresh with `/graphify . --update`).
