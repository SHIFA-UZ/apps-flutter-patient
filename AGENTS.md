# Agent instructions

Read `CLAUDE.md` in this folder: it is the guide for this repository and applies to every agent and every person,
whatever the tool. The workspace guide one level up (`../CLAUDE.md`, with `../AGENTS.md` as its summary) adds the
cross-repository rules and wins where the two differ.

In short: Kotlin Multiplatform with MVVM and repositories under `composeApp/src/commonMain/kotlin/shifa/uz/`
(`ui/<feature>`, `data/<feature>`, `data/api`, `data/model`, `ui/widgets`, `ui/theme`, `utils`), no raw hex, dp or sp
values, ktlint clean; one branch per change from a fresh `main`, commits as short imperative sentences, the matching
`docs/implemented/` entry in the same commit, never commit `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`;
no AI attribution, no push or pull request before the owner has seen the work.
