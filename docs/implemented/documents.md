# Documents

## List (`ui/documents/DocumentsScreen.kt`, DOCS-01)

- Figma: `1681:14033`. The Documents tab: Home's header (avatar → Profile, bell → Notifications), a title search, the cards and a floating Upload button above the nav bar. F1 loads on the first open; pull to refresh reloads it. A 401 on F1 logs out.
- The search is a case-insensitive title match, in memory. The list's ViewModel lives in the tab shell, so the list and the search survive tab switches.
- The backend sends the uploader's full name in `creatorLabel`, not 11's "Doctor" / "Patient" (seen on production, 2026-09-28). So the patient's own uploads are the ones whose `creatorLabel` is their P1 `fullName` (Home loads it), or "Patient". A doctor with the patient's exact name would look like theirs; F3 then fails with an error. A backend flag would fix it.
- Cards: "Added by you" for the patient's own uploads, "Added by {creatorLabel}" otherwise ("Doctor" → "Added by {doctorName}" or "Added by doctor"; blank or "Unknown" → nothing). The date is `date`, else `createdAt`, as a local `dd.MM.yyyy` (11 quirk 15). A card without a file URL doesn't open.
- Not in Figma (DOCS-01 asks, 11 quirk 1): a category tag on cards, and filter chips under the search: "All" plus only the categories the list uses, in F2's order. The filter combines with the search. Labels cover F2's categories and `PRE_APPOINTMENT` (bookings); an unknown code shows no tag and no chip.
- Not in Figma: swiping the patient's own upload to the left deletes it (F3) with no confirm. The card stays swiped away with a spinner while F3 runs. Success drops it with "Document deleted"; a failure brings it back with the error, and a 401 logs out. Doctor's documents don't swipe. Banners sit above the list, so the result shows wherever the list is scrolled.

## Upload (DOCS-02)

- Upload opens a tray (Camera, Photo, File). The Figma tray is titled "Add to chat" (a copy of the chat tray's), so it says "Upload". File picks PDFs only. The camera photo is turned upright and re-encoded as JPEG (Android: FileProvider into the cache dir; iOS: `UIImagePickerController`).
- The pick opens the Figma "Document Upload" pop-up: the file name with Change, the title (defaults to the file name without its extension) and an optional type. Change keeps a title the user typed. Picking the chosen type again clears it (11 quirk 8).
- Save sends F2 multipart: `file` with a real Content-Type and a sanitized file name (11 quirk 10), `title`, `isChatAttachment=false`, and `category` only when chosen. An empty title shows an error instead of silently doing nothing (11 quirk 8). A failure keeps the pop-up and the list (11 quirk 6). Success puts the new document first.
- Not built: the date field (not in Figma), the camera permission rationale.

## Viewer (`DocumentViewerScreen.kt`, DOCS-03)

- Opened from a card, as an overlay above the tabs. F4 loads the file; the type comes from the magic bytes first, then `type` (a MIME type or an extension) and the URL (11 quirk 2). PDFs render with `PdfRenderer` on Android (pages in a list, no pinch zoom) and PDFKit on iOS; images zoom 1–4x. Anything else shows "unsupported". Errors show with Retry.
- The token only goes to the API host, and a 401 from the file host is only an error, not a logout (11 quirk 4).
- Download saves the file as `{title}.{ext}` (a stub in Flutter, 11 quirk 3): Downloads via MediaStore on Android 10+, the app's own Downloads folder before that (no storage permission), the app's Documents folder on iOS (shown in Files under the app). Not tried on a device.
- The header's ⋮ deletes, shown only for the patient's own uploads (F3, see the list): a confirm → F3 → back to the list, which drops the document.
- From Appointment (Figma `1921:23596`): the visit the document came from, as a past-appointment card with a chevron that opens its details (the viewer closes, as the details sit under it). F1 has no link yet ([backend-improvements](../backend-improvements.md) §15), so the app reads an `appointmentId` if one comes and otherwise matches a doctor's upload (`creatorLabel` is the doctor's name) to that doctor's visit on the document's day (B2). No match, the patient's own upload, or the viewer opened from a chat: no section.
- A tap on the document fills the page with it (the From Appointment section and Download hide); another tap, Back or the header's back gives the page back. PDFs take the tap themselves (PDFKit's native view on iOS gets a tap recognizer), so scrolling and zooming still work.
- Not built: the details screen (`/documents/:id`, only opened by notification deep links, which aren't built), and a bigger receive timeout for large files (11 quirk 5). The viewer's state isn't saved across process death.
