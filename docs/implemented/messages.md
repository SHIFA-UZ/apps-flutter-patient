# Messages

## Chats (`ui/messages/MessagesScreen.kt`, CHAT-01)

- Figma: `1772:16842`. The Messages tab. The header is Home's (avatar → Profile, bell → Notifications with the badge).
- C1, newest first, grouped by day ("Today", "Yesterday", "Sep 20"; Figma's "Last Week" isn't used). Each row shows the doctor's photo, the last message ("No messages" when empty, the backend's "photo" localized), its time and the unread count.
- Loads on the first open, on pull to refresh, when a chat closes and when a push arrives.
- Search finds doctors (C5, 400 ms after typing stops; Flutter searched on every key). Picking one refetches C1 for their chat (08 §5.3 `_startConversation`), or opens a new chat. Figma's placeholder is "Search".
- No initials when there's no photo (08 §5.6): the kit's avatar has none.

## Chat (`ConversationScreen.kt`, CHAT-02, partly CHAT-03/05)

- Figma: `1772:17206`. An overlay above the tabs; the doctor's name and photo in the header.
- C2 on open and every 5 s while the chat is shown (the screen runs the poll, so it stops on leave). C4 on open and when a new received message comes in; its errors are ignored.
- Opens at the newest message (Flutter opened at the oldest).
- Send (C3) clears the composer once it goes. A failed send keeps the text and shows the error. A sent message stays on screen until a fetch brings it.
- A new chat is addressed by `recipientDoctorId`, then switches to the `chatId` of its first message (Flutter never switched, 08 §6.4).
- The + tray: Camera and Photo upload (C7, the picker's file name) and send as an image, with the typed text as the caption. File and the mic show "Coming soon".
- Bubbles (08 §5.5), in both message formats (15 §5.9):
  - text;
  - image (280 × 280 crop, with its caption; Flutter never showed the caption);
  - document (its file name);
  - voice ("Voice Message 0:42");
  - system (centred grey text).
- Tapping an image, document or voice message opens it in the system viewer.
- Notification taps open the chat when the push or inbox item has a `chatId` (fixes 00 bug 10), else the Messages tab. The inbox tray's button is "Open chat".
- Not built:
  - Figma's ⋮ (no API for it);
  - times and read ticks, link detection;
  - the voice recorder and player (CHAT-04);
  - sending files;
  - the in-app image viewer;
  - the unread badge (CHAT-06).
- Not tried against the backend: sending, uploads and mark read (production data).
