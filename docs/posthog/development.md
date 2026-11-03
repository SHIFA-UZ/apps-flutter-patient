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
- [ ] Seen arriving in the PostHog **Dev** project (Activity view), with the expected properties
- [ ] dashboards.md updated if there's a new flow to watch

## 5. Testing with the Dev project

- Debug builds use the Dev key (setup.md §2) and log every event (`debug = true`).
- Demo data turns analytics off (README D8). To test analytics, use the Development backend
  (Debug → Development backend, or Login's debug button) with a seed account; the super property
  `backend = development` separates those events in Dev charts (D9).
- Debug menu additions (ANL-02, ANL-06):
  - "Mark me as internal": sets the person property `internal = true`, so Prod filters exclude you if a debug build
    ever points at Prod.
  - "Analytics: show events": a list of the last 50 events sent this session, with their properties, to check
    without opening PostHog (and the only way to check on the free plan's single project, setup.md §1.6).
  - "Analytics: reload flags" and the current values of every `Flags` accessor.
  - "Crash now": throws, to verify exception autocapture.
- Never point a debug build at the Prod key.

## 6. Renaming and removing events

- Don't rename an event or a property value: charts break silently. Add the new event, send both for one release,
  move the insights, then remove the old one.
- Mark a removed event in events.md as "Removed in vX.Y" instead of deleting the row for one release, so people
  reading old charts know why they stopped.
- `$app_version` is on every event, so a change can always be told apart by version.
- A flag that's removed from PostHog while a release still reads it returns the default; that's why every accessor
  has one.
