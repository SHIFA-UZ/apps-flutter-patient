# Adding analytics to a feature

The rules for every feature from now on. Tracking is part of "done", like strings, previews and tests.

## 1. When you build or change a feature

1. **Decide the questions first.** What should the team learn from this feature? Usually: did people find it, did
   they finish it, why did it fail. Two to five events are normally enough.
2. **Write the events into [events.md](events.md)** in the right journey section: name, trigger, properties, file.
   Follow the conventions in events.md §1. Reuse existing `source` / `reason` values where they fit.
3. **Run the privacy test** (privacy.md §1) on every property.
4. **Add the screen** to `AnalyticsScreen` and `TrackScreen(...)` in the new `XxxScreen` (not in `XxxContent`).
5. **Add the event classes** to `AnalyticsEvent.kt` and fire them from the ViewModel.
6. **Test them** (§3).
7. **Add a funnel or insight** to [dashboards.md](dashboards.md) if the feature has a flow worth watching. Build it in
   PostHog once data arrives.
8. **Update `docs/implemented/analytics.md`** (once it exists) with the events now live, in the same commit.

## 2. Code pattern

```kotlin
class CreateBookingViewModel(
    // …existing deps
    private val analytics: AnalyticsTracker = Analytics,
) : ViewModel() {
    fun book() {
        // …
        bookings.book(request).fold(
            onSuccess = {
                analytics.track(AnalyticsEvent.BookingCompleted(isVideo, it.paymentStatus, daysAhead))
                // …
            },
            onFailure = {
                analytics.track(AnalyticsEvent.BookingFailed(reasonOf(it)))
                // …
            },
        )
    }
}
```

- The tracker is the last constructor parameter with a default, like every other dependency (CLAUDE.md), so
  existing tests don't change.
- Map failures to the event's `reason` enum in one private function per ViewModel. Never pass `e.message`.
- Fire from the ViewModel, not the composable, except pure UI taps with no ViewModel action
  (`doctor_contact_tapped`, `privacy_policy_opened`), which use `Analytics.track(...)` in the screen's callback.
- Never fire from `XxxContent`, previews, or anything a screenshot test renders.
- A screen used in two places (the check-in tray on the task page and over the inbox) passes its `source` into the
  ViewModel's constructor.

## 3. Tests

- Shared fake in `androidUnitTest` (`ui/RecordingTracker.kt`):

```kotlin
class RecordingTracker : AnalyticsTracker {
    val events = mutableListOf<AnalyticsEvent>()

    override fun track(event: AnalyticsEvent) {
        events += event
    }
}
```

- In the ViewModel test, pass it and assert the outcome event alongside the state, with backtick names as usual:

```kotlin
@Test
fun `a booking that goes through is tracked with its kind and lead time`() =
    runTest {
        val tracker = RecordingTracker()
        val classToTest = CreateBookingViewModel(doctor, bookings, /* … */ analytics = tracker)
        // …book…
        advanceUntilIdle()
        assertThat(tracker.events).contains(AnalyticsEvent.BookingCompleted(isVideo = false, "paid", daysAhead = 2))
    }
```

- One test per outcome event is enough (success and the main failure). Don't test every property permutation.
- `AnalyticsEventTest` (one test) checks the whole catalog: every name is `snake_case`, and no property key is on the
  blocked list (`name`, `phone`, `email`, `text`, `comment`, `reason_text`, `query`, `notes`, `value`, `*_id`).
  It catches the most common privacy slip before review.

## 4. Review checklist (paste into the PR description)

- [ ] events.md updated, and matches `AnalyticsEvent.kt`
- [ ] Every property passes privacy.md §1 (no health data, no identity, no record ids, no free text)
- [ ] Outcome events fire after the result, from the ViewModel
- [ ] Screens use `TrackScreen` in `XxxScreen` only
- [ ] A test asserts each new outcome event
- [ ] Seen arriving in the PostHog **Dev** project (Activity view), with the expected properties
- [ ] dashboards.md updated if there's a new flow to watch

## 5. Testing with the Dev project

- Debug builds use the Dev key (setup.md §2) and log every event (`debug = true`).
- Demo data turns analytics off (D8). To test analytics, use the real backend with a test account, or a fake
  backend you've confirmed doesn't write to production.
- Debug menu additions (ANL-02):
  - "Mark me as internal": sets the person property `internal = true`, so Prod filters exclude you if a debug build
    ever points at Prod.
  - "Analytics: show events": a list of the last 50 events sent this session, to check without opening PostHog.
- Never point a debug build at the Prod key.

## 6. Later: flags, experiments, surveys, replay (ANL-08)

- **Feature flags**: wrap a flag check in a shared `Flags` object with a typed accessor per flag and a safe default,
  so a missing network or a deleted flag falls back to current behaviour. Read flags in the ViewModel, not in
  composables. Remove the flag and its code once a rollout is done; flags aren't config.
- **Experiments**: one primary metric from events.md (for example `booking_completed` per `booking_started`), decided
  before the test starts. Run it for at least two full weeks, since the visit pattern is weekly.
- **Surveys**: the native SDKs support them; `posthog-kmp` doesn't wrap them yet. App questions only (privacy.md §7),
  shown after a finished flow (after `review_submitted` or `video_call_ended`), never during a visit.
- **Session replay**: off; see privacy.md §6 before proposing it.

## 7. Renaming and removing events

- Don't rename an event or a property value: charts break silently. Add the new event, send both for one release,
  move the insights, then remove the old one.
- Mark a removed event in events.md as "Removed in vX.Y" instead of deleting the row for one release, so people
  reading old charts know why they stopped.
- `$app_version` is on every event, so a change can always be told apart by version.
