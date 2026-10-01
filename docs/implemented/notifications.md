# Notifications

## Inbox (`ui/notifications/NotificationsScreen.kt`, NOTIF-01)

- Figma: `1703:15201`. A page above the tabs, opened by the header bell on Home, Bookings and Documents. It gets fresh ViewModels each time it opens.
- Every open loads N1; pull to refresh reloads it. A failed first load shows a Retry button under the error banner. A failed reload keeps the list with the error. A 401 logs out.
- N1 items without `id`, `title`, `message`, `type` or a parseable `createdAt` are skipped (13 quirk 9). The linked appointment, document, task and doctor come camelCase, snake_case or under `data`.
- Chips: All / Appointments / Tasks / Documents / Payments. They match on the type: `APPOINTMENT`, `TASK`, `DOCUMENT` or `SIGNATURE`, and `PAYMENT` or `INSTALLMENT`. Payments isn't in Figma; payment cards keep the folder icon, as Figma has no payment one. The empty texts differ for All and for a filter. Newest first, grouped under Today / Yesterday / "Sep 20" (local dates). Figma's "Last Week" is placeholder copy.
- Cards: `ShifaNotification` with the type's icon (appointment, task, chat; everything else gets Figma's folder card). Title and message use the 13 §6.3 localized texts; SIGNATURE_REQUESTED fills in the doctor, DOCUMENT_ACCESS_REQUEST fills in who, file and patient. Unknown types show the backend's text. Time: "Just now", "N minutes ago", HH:mm today, "Yesterday HH:mm", else "Sep 20 • HH:mm".
- The header check (N4) marks everything read, shows "All notifications marked as read" and refreshes the bell count.
- Unread cards have a brand (primary) title; Figma has no unread look.
- Not built: the ⋮ menu (no Figma for it), and the title count.

## Details tray (NOTIF-01, NOTIF-04 in part)

- Figma: `1703:15698` (appointment), `1703:16143` (document), `1703:16483` (task). A tap opens the tray and marks the item read (N3, best effort, quirk 11). It updates on screen right away, and the bell refreshes when N3 goes through.
- The tray shows the message, the linked card and a button. The cards are Home's appointment card, the Documents list card and Home's task card. Inside a tray they use Figma's look: canvas fill, no shadow, no chevron (`LocalCardOnSurface`).
- There is no get-by-id endpoint, so the item is looked up in B2, F1 or T1. When it isn't found, or the list call fails, the button opens the list instead: the Bookings tab, the Documents tab or My Tasks. TASK_COMPLETED opens My Tasks. PROPHYLAXIS_REMINDER opens the Bookings tab. The button closes the page first, then opens the target.
- Button labels come from 13 §6.3 ("View appointment", "View document", "Open task", "Open bookings"), not Figma's placeholder copy.
- Not built:
  - Buttons for payment, installment, treatment plan, sign form and visit summary. Those screens don't exist yet, so the tray shows only the message. Chat items open the chat (see messages.md).
  - Approve and reject for access requests (NOTIF-02).
  - The 280 max-height scroll for long messages.
- Not tried on a device: mark read and mark all read (production data).

## Push (NOTIF-03, NOTIF-04)

- Android: `ShifaMessagingService` (Firebase Messaging, BOM in the catalog). iOS: `AppDelegate.swift` with the `FirebaseMessaging` Swift package, `iosApp.entitlements` (`aps-environment`), and the `remote-notification` background mode. They hand the token, taps and arrivals to the shared `Push` object (Android directly, iOS through `PushBridge.kt`).
- `PushViewModel` lives with the signed-in shell. It sends P7 with the token, and again on each refresh; logout sends `""` (AuthRepository). A refresh while the app is closed is sent on the next open.
- `PushSetup()` runs once signed in (13 §11.3): Android 13+ asks for `POST_NOTIFICATIONS` and creates `shifa_patient_channel` with its localized name; iOS asks for alert, badge and sound. The token is registered either way, so a later grant works.
- Background and closed: the OS draws the backend's English text (13 §3.2). On Android the manifest sets the channel, the "S" icon and the brand color. Foreground: Android posts a notification on the channel (tray id = server id), and iOS shows the banner. Both use the backend's text and refresh the bell.
- Tap: the data (`AppNotification.fromPush`, with `entityId` mapped by type, 13 §6.2) is marked read with N3 when the id is a number. Then it routes like the tray's button, and everything open is closed first, like Flutter's `go`. Types without a route open the inbox. Taps that cold-start the app wait for sign-in. Android reads the data from the launch intent's extras (`singleTop`, so a tap on a running app arrives in `onNewIntent`).
- Not built:
  - Localized foreground text (Flutter had it); localized background text needs the backend ([backend-improvements.md](../backend-improvements.md) #1).
  - Cancelling the tray notification on mark read (13 §11.8).
  - The read-id dedupe set (13 §8.2).
  - Marking read for string ids.
  - The reminder channel (no scheduled reminders yet).
- Not tried on a device. P7 replaces the account's Flutter token on the server, so that account's Flutter app stops getting pushes. A Firebase console test message to this device's token is safe.
