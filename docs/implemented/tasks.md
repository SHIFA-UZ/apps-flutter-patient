# Tasks

## My Tasks (`ui/tasks/TasksScreen.kt`, TASK-01)

- Figma: `1655:17107` (Open), `1662:18016` (Completed). An overlay above the tabs, opened from Home's My Tasks cards and arrow. The ViewModel lives in the tab shell, so the check-in page can report back to it.
- Every open loads T1; pull to refresh reloads it, also on an empty tab (12 quirk 2). A failed reload keeps the list with an error banner. A 401 logs out.
- Open Tasks: everything but COMPLETED and CANCELLED, so EXPIRED stays. Completed: COMPLETED, plus EXPIRED with nothing pending and at least one check-in. Such a task is in both tabs (12 quirk 1, kept). API order, no client sort.
- Figma's canvas background under the header and tabs.
- Cards: Home's task card (category icon and tag, "Starts" / "Started dd.MM.yyyy"; no description line when it's blank). Open cards add the progress bar "completed/total". Completed cards have no bar (Figma). Figma's "Due today" placeholder isn't shown. Any card opens the task page.
- Not built: the ⋮ menu (no Figma for it) and a reload on app resume (pull to refresh covers it).

## Task page (`TaskCheckInScreen.kt`, TASK-02)

- Figma: `1662:18317` (calendar), `1891:21385` (submit tray), `1891:21727` (submitted tray). An overlay above My Tasks, titled "Task". It shows the list's task card, with its progress worked out from T3 once loaded (no T2 refetch), then the month calendar.
- Calendar (`ShifaCalendarCard`): Monday-first weeks, the days around the month greyed. A day is filled when all its check-ins are COMPLETED, default (tappable) when one is PENDING (an unknown status counts as PENDING), else disabled (none, or missed). It opens on this month, moved into the task's months on the first load; the arrows stop at the first and last month with a check-in. A check-in whose date doesn't parse isn't on it. Pull to refresh reloads T3.
- Tapping a default day opens the tray on its first PENDING check-in (by time): the task card with "Due HH:mm", then:
  - NUMERIC: a decimal field; a comma works as the decimal point. TEXT, and any other type: a text field. The label is "Enter {inputLabel}", else the generic one. BOOLEAN: Yes / No chips.
  - Comment (the notes label, else "Comment"), always shown, multi-line. Required notes can't be blank.
  - Submit Entry, enabled from 10 minutes before the check-in's time, on the device's clock (the server's zone is unverified, as in Flutter). Before that the 10-minute rule is spelled out above it. A past PENDING one can still be sent.
- Values and notes are trimmed, so whitespace isn't a value. T4 sends only the set fields. Success closes the tray with "Check-in submitted successfully", reloads T3 (the day turns filled, or opens its next check-in), and reloads My Tasks and Home; the page stays open. A failure keeps the form with the error; the three T4 validation messages are localized. A 401 logs out.
- Tapping a filled day shows "Check in already submitted", then each check-in's card, value (Yes / No for BOOLEAN; with its time when the day has several) and comment, read only (T3's `numericValue` / `textValue` / `booleanValue` / `notes`).
- The same tray opens alone over the inbox from a task notification's Submit Check in (see notifications.md).
- Not built: From Appointment (T1 has no link to the visit) and Delete Entry (no endpoint to delete or edit a check-in), see backend-improvements §14. Also T2 with the notification deep links (NOTIF), a ticker for the 10-minute window (12 quirk 10), and the example-value hint (quirk 6). Not submitted on a device (production).
