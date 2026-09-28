# Tasks

## My Tasks (`ui/tasks/TasksScreen.kt`, TASK-01)

- Figma: `1655:17107` (Open), `1662:18016` (Completed). An overlay above the tabs, opened from Home's My Tasks cards and arrow. The ViewModel lives in the tab shell, so the check-in page can report back to it.
- Every open loads T1; pull to refresh reloads it, also on an empty tab (12 quirk 2). A failed reload keeps the list with an error banner. A 401 logs out.
- Open Tasks: everything but COMPLETED and CANCELLED, so EXPIRED stays. Completed: COMPLETED, plus EXPIRED with nothing pending and at least one check-in. Such a task is in both tabs (12 quirk 1, kept). API order, no client sort.
- Cards: Home's task card (category icon and tag, "Starts:" / "Started: dd.MM.yyyy"). Open cards add the progress bar "completed/total". Completed cards have no bar (Figma). Figma's "Due today" placeholder isn't shown. Any card opens the check-in.
- Not built: the ⋮ menu (no Figma for it) and a reload on app resume (pull to refresh covers it).

## Task check-in (`TaskCheckInScreen.kt`, TASK-02)

- Figma: `1662:18317`. An overlay above My Tasks. It shows the list's task card with its progress (no T2 refetch), then "Select check-in" with the T3 slots.
- Slots: PENDING ones (an unknown status counts as PENDING) due within 10 minutes, on the device's clock (the server's zone is unverified, as in Flutter). A slot whose date doesn't parse is left out. Below the due ones, the next 3 PENDING slots not due yet show greyed and can't be ticked (not in Figma), so the patient sees when the next one opens. With none due: "No check-ins due yet…" under the greyed ones (or alone if none parse); with none PENDING: "No pending check-ins". Worked out on load and pull to refresh, with no ticker (12 quirk 10). A reload keeps the ticked slot while it's still listed.
- Ticking a slot opens its form in the card (Figma check-if-fill) and starts it empty. Ticking another one moves the form. The form has:
  - NUMERIC: a decimal field; a comma works as the decimal point. TEXT, and any other type: a text field. The label is "Enter {inputLabel}", else the generic one. BOOLEAN: Yes / No chips.
  - Notes, when the task requires them or has a notes label. Required notes can't be blank. Without a label they read "Additional notes" (Flutter said "optional", but a field with no label is always required).
  - Submit.
- Values and notes are trimmed, so whitespace isn't a value. T4 sends only the set fields. Success closes the page, reloads My Tasks with "Check-in submitted successfully", and refreshes Home. A failure keeps the form with the error; the three T4 validation messages are localized. A 401 logs out.
- Not built: multi-line fields, the example-value hint (quirk 6), and T2 with the notification deep links (NOTIF). Not submitted on a device (production).
