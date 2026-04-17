# Shifa Patient App — launch readiness checklist

Use this before store submission or major production cutover. Check items when verified; adjust for your legal jurisdiction (HIPAA / GDPR / local health rules).

---

## Technical

- [ ] **API & environment**: Release builds use safe URLs (`ENVIRONMENT`, `API_BASE_URL` via `--dart-define`); no localhost in production (`AppConfig`).
- [ ] **Crashlytics**: Enabled for release; no PHI in logs or Crashlytics payloads; debug collection off as intended.
- [ ] **Video (Daily)**: Join/leave/lifecycle and background behavior validated on real devices.
- [ ] **Offline**: Chat send retry / banners; document upload errors; connectivity handling smoke-tested.
- [ ] **Static analysis**: `flutter analyze` clean of **errors**; warnings budget agreed (CI uses `--no-fatal-infos`).
- [ ] **Tests**: `flutter test` green in CI; coverage artifact reviewed; plan for raising minimum coverage over time.
- [ ] **Flutter & deps**: SDK `^3.9.2` satisfied; `pubspec.lock` committed; run `flutter pub outdated` for security patches.
- [ ] **Integration / E2E**: Staging golden path executed (see `integration_test/README.md`).

---

## UX

- [ ] **Placeholders**: No production placeholder image URLs; local assets for fallbacks where needed.
- [ ] **States**: Loading / empty / error patterns consistent on main flows (auth, bookings, documents, chat, video).
- [ ] **Accessibility**: TalkBack / VoiceOver pass on primary actions; tooltips / semantics on critical controls.
- [ ] **Text scaling**: Large system fonts do not break main layouts (`MediaQuery.withClampedTextScaling` + scroll where needed).

---

## Security

- [ ] **JWT**: Stored via secure storage; cleared on logout; 401 handling does not leak tokens.
- [ ] **Transport**: Production API over HTTPS only.
- [ ] **Logging**: No patient identifiers or clinical content in debug prints shipped to analytics.
- [ ] **Secrets**: No API keys or private keys in repo; use CI secrets / `--dart-define` for build-time config only as appropriate.

---

## Compliance & store readiness

- [ ] **Privacy policy**: Linked in store listing and in-app where required.
- [ ] **Consent**: Cookie / analytics / marketing consent if applicable.
- [ ] **Medical disclaimer**: App positioning as tool, not a substitute for emergency or professional care (legal review).
- [ ] **HIPAA / GDPR / regional**: Data processing agreements, DPA, retention, and breach process documented.
- [ ] **Versioning**: `pubspec.yaml` `version:` + Android `versionCode` / iOS `CFBundleVersion` aligned with release notes.

---

## Final validation (manual)

- [ ] Smoke on **physical** Android and iOS: login → booking → (optional) video → chat → documents.
- [ ] Crashlytics receives a **test** non-fatal error in a staging build.
- [ ] CI pipeline **green** on default branch after merge.

---

## CI commands (reference)

```bash
dart format --set-exit-if-changed lib test integration_test
flutter analyze --no-fatal-infos --no-fatal-warnings
flutter test --coverage
# Optional integration smoke (device / chrome):
# flutter test integration_test/patient_app_smoke_test.dart -d chrome
```

---

## Coverage gate (optional)

To enforce a minimum line coverage (e.g. 70%), add a step after `flutter test --coverage` using `lcov` or `very_good_coverage` on `coverage/lcov.info` once the suite reliably meets the threshold.
