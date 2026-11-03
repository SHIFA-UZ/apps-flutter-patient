# Adding analytics to a feature

The rules for every feature from now on. Tracking is part of "done", like strings, previews and tests.

## 1. When you build or change a feature

1. **Decide the questions first.** What should the team learn from this feature? Usually: did people find it, did
   they finish it, why did it fail, what did they do instead. Two to five events are normally enough.
2. **Write the events into [events.md](events.md)** in the right journey section: name, trigger, properties, the
   person properties it sets, file. Follow the conventions in events.md §1. Reuse existing `source` / `reason`
   values where they fit.
3. **Add every new control to the coverage matrix** (events.md §14). A control without a row is the most common gap.
4. **Run the privacy test** (privacy.md §1) on every property.
5. **Add the screen** to `AnalyticsScreen` and `TrackScreen(...)` in the new `XxxScreen` (not in `XxxScreenContent`).
6. **Add the event classes** to `AnalyticsEvent.kt` and fire them from the ViewModel.
7. **If the feature reads a flag**, add the typed accessor to `Flags` with its default (personalisation.md §4).
8. **Test them** (§3).
9. **Add a funnel or insight** to [dashboards.md](dashboards.md) if the feature has a flow worth watching. Build it in
   PostHog once data arrives.
10. **Update `docs/implemented/analytics.md`** (once it exists) with the events now live, in the same commit.

## 2. Code pattern

```kotlin
class CreateBookingViewModel(
    // …existing deps
    private val analytics: AnalyticsTracker = Analytics,
) : ViewModel() {
    private suspend fun submit(request: BookingRequest) {
        bookings.book(request).fold(
            onSuccess = {
                when {
                    it.isAwaitingPayment && it.payment?.checkoutUrl != null ->
                        analytics.track(AnalyticsEvent.PaymentCheckoutOpened(provider, holdSeconds(it), bucket(it), it.isVideo))
                    request.paymentProvider != null && it.isPaid -> // B3 answered PAID with no checkout
                        analytics.track(AnalyticsEvent.PaymentCompleted(provider, secondsToPay = 0, via = "immediate", /* … */))
                    else -> analytics.track(AnalyticsEvent.BookingCompleted(isVideo, paymentKind, daysAhead))
                }
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
- Map failures to the event's `reason` enum in one private function per ViewModel (`reasonOf`). Never pass
  `e.message`.
- Fire from the ViewModel, not the composable, except pure UI taps with no ViewModel action
  (`doctor_contact_tapped`, `privacy_policy_opened`, `document_expanded`), which use `Analytics.track(...)` in the
  screen's callback.
- Never fire from `XxxScreenContent`, previews, or anything a screenshot test renders.
- A screen used in two places (the check-in tray on the task page and over the inbox) passes its `source` into the
  ViewModel's constructor.
- Durations (`seconds_on_page`, `seconds_to_pay`) come from the injected `clock: () -> Instant` the ViewModels
  already take, so tests can assert them.
- Buckets (`amount_bucket`, `size_kb_bucket`, `duration_s_bucket`) are computed by one shared function each in
  `AnalyticsEvent.kt`, so every event buckets the same way.

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
        assertThat(tracker.events).contains(AnalyticsEvent.BookingCompleted(isVideo = false, "free", daysAhead = 2))
    }
```

- One test per outcome event is enough (success and the main failure). Don't test every property permutation.
- `AnalyticsEventTest` (one test) checks the whole catalog: every name is `snake_case`, and no property key is on
  the blocked list (`name`, `phone`, `email`, `text`, `comment`, `message`, `query`, `notes`, `value`, `answer`,
  `title`, `url`, `amount`, `*_id`). It catches the most common privacy slip before review.
- A ViewModel that reads a flag takes it as a constructor parameter with the `Flags` accessor as the default, so
  the test passes the variant it wants.

## 4. Review checklist (paste into the PR description)

- [ ] events.md updated, and matches `AnalyticsEvent.kt`
- [ ] Every new control has a row in the coverage matrix (events.md §14)
- [ ] Every property passes privacy.md §1 (no health data, no identity, no record ids, no free text, no exact amounts)
- [ ] Outcome events fire after the result, from the ViewModel
- [ ] Screens use `TrackScreen` in `XxxScreen` only
- [ ] Person properties set by the event are in personalisation.md §2
- [ ] A test asserts each new outcome event
- [ ] Seen in the Debug menu's event list with the expected properties (and in PostHog's Activity view from a release build when the flow is new)
- [ ] dashboards.md updated if there's a new flow to watch

## 5. Testing without a development project

- Debug builds never send (README D4): there is no key in them, by construction. Every event still goes through
  the facade, so the Debug menu shows it.
- Debug menu → Analytics (ANL-02, ANL-06):
  - the last 50 events of the session with their properties, newest first: the way to check a feature's events
    before review, with any backend, demo data included;
  - your analytics id (`patient_<id>`): add it to PostHog's internal-users filter (setup.md §1.4) once, so the
    dashboards exclude the events your TestFlight / internal-track builds send;
  - "Analytics: reload flags" and the current values of every `Flags` accessor;
  - "Crash now": throws, to verify exception autocapture in a release build.
- Seeing events arrive in PostHog is a release-build check (setup.md §7): a TestFlight or internal-track build on
  an account listed in the internal-users filter.
- Never put a key into a debug build.

## 6. Renaming and removing events

- Don't rename an event or a property value: charts break silently. Add the new event, send both for one release,
  move the insights, then remove the old one.
- Mark a removed event in events.md as "Removed in vX.Y" instead of deleting the row for one release, so people
  reading old charts know why they stopped.
- `$app_version` is on every event, so a change can always be told apart by version.
- A flag that's removed from PostHog while a release still reads it returns the default; that's why every accessor
  has one.
