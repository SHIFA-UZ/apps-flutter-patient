# Push Notifications – Deep Linking & Mark as Read

## Architecture

- **Single entry**: All FCM and local notification entry points funnel to one flow.
  - **Foreground**: `FirebaseMessaging.onMessage` → show local notification (skipped if already in read cache).
  - **Background**: `FirebaseMessaging.onMessageOpenedApp` → `handleNotificationNavigation(message)`.
  - **Terminated**: `getInitialMessage()` stored → `processPendingInitialMessage()` → `handleNotificationNavigation(message)`.
  - **Local notification tap**: `onDidReceiveNotificationResponse` → `handleNotificationNavigationFromData(payload)`.

- **App layer** (MainShell): When the service delivers a payload, the callback:
  1. Marks the notification as read (PUT `/notifications/:notificationId/read`) via `NotificationsController`.
  2. Invalidates notification list and unread count (so list and badge update immediately).
  3. Navigates by type (see below). Navigation is not blocked if the mark-as-read API fails.

- **Duplicate prevention**: Service keeps a set of `notificationId`s already handled. Foreground messages with an id in that set do not show a local notification again.

## Payload (backend)

Every push **data** payload must include:

| Key              | Required | Description                                      |
|------------------|----------|--------------------------------------------------|
| `type`           | Yes      | e.g. `appointment_reminder`, `new_document`, `new_message` |
| `entityId`       | Yes      | Target entity id (appointment uuid, document id, chat id) |
| `notificationId` | Yes      | Id to mark as read (used for PUT `/notifications/:id/read`) |
| `route`          | No       | Optional fallback path if type is not mapped    |

Legacy keys (`appointmentId`, `documentId`, `chatId`, `id`) are still supported.

## Type → route mapping

| type                     | Route                    |
|--------------------------|--------------------------|
| appointment_reminder, appointment_* | `/bookings/:entityId`    |
| new_document, document   | `/documents/:entityId`   |
| new_message, chat_message| `/chat` (extra: chatId)  |
| task_*                   | `/tasks` or `/tasks/:id/check-in` |
| default                  | `/notifications`         |

## Test cases (must pass)

1. **App killed** → receive notification → tap → opens correct page.
2. **App background** → tap notification → opens correct page.
3. **App foreground** → tap local notification → opens correct page.
4. **After tap** → notification is marked read (no reappearance).
5. **Reopen app** → no duplicate popup for the same notification.
6. **Unread badge** → decreases correctly after tap.
