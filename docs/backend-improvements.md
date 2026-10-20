# Backend improvements

What the patient app needs from the backend team, found while building the KMP app. Each item says what happens now, what to change, and what the app does until then.

Endpoint ids (P7, D1, B5…) are from `docs/kmp-spec/15-api-reference.md`. App languages: `en`, `ru`, `uz`, `de`. The patient's choice is saved in the profile `language` field (P2).

| # | Item | Priority | App change after |
|---|---|---|---|
| 1 | Push text in the patient's language | High | None (option A) or small (option B) |
| 2 | Push tokens per device, not per account | High | Small |
| 3 | A staging backend for testing | High | Config only |
| 4 | Error codes instead of English text | Medium | Small |
| 5 | Mark the patient's own documents | Medium | Small |
| 6 | Absolute photo and file URLs | Medium | None |
| 7 | Cancel as a status, not a delete | Low | Small |
| 8 | Delete account for phone-only patients | Low | Small |
| 9 | Doctor search cleanups | Low | None |
| 10 | Google sign-in | Later | Medium |
| 11 | Shifa AI: error codes, attachments, chat sync | Medium | Small to medium |
| 12 | Payments: statuses, idempotent checkout, return link, slot hold | High | Small to large |
| 13 | Live visit: an end-visit call, the visit's service, notes and documents | Medium | Small |

## 1. Push text in the patient's language

**Now:** every push has `notification: { title, body }` in English. When the app is closed or in the background, the phone's system shows that text as it is. The app doesn't run at that moment, so it can't translate it. A Russian or Uzbek patient gets English notifications. The Flutter app had the same problem (13 §11.5).

**Change, option A (recommended):** write `title` and `body` in the patient's profile `language`, and fall back to English. The app already has these texts in all 4 languages: `composeApp/src/commonMain/composeResources/values*/strings.xml`, mapped per type in `NOTIFICATION_TEXTS` (`ui/notifications/NotificationsUiState.kt`, from 13 §6.3). The backend can copy them. The app doesn't need any change.

**Change, option B:** send pushes without the `notification` block, only `data`. On iOS, also add `mutable-content: 1` to the APNs payload. The app then writes the text itself. This needs work in the app: an Android handler for closed-app pushes, and on iOS a Notification Service Extension (a separate small target in the Xcode project). A new app release is needed before the switch, or old installs stop showing pushes.

**Also:** keep sending `type`, `notificationId` (a number) and the linked id (`appointmentId`, `documentId`, `taskId` or `entityId`) in `data`. The app uses them to mark the push as read and to open the right screen when it's tapped. Please also confirm the full list of `type` values production sends (13 §12.17). The app knows about 20 more types than the old backend enum.

## 2. Push tokens per device, not per account

**Now:** a patient has one `fcmToken` (P7 `PUT /patients/me/fcm-token {"fcmToken": "..."}`). Each login overwrites it, and logout sends `""`. So:
- Only the device that logged in last gets pushes. A patient with a phone and a tablet gets them on one only.
- Logging out on one device stops pushes on every device.
- During the move from Flutter, signing in to the new app stops pushes in the old app on the same account.

**Change:** keep a list of tokens per patient.
- `POST /patients/me/fcm-tokens { "token": "...", "platform": "android" | "ios" }` adds a token (adding it again does nothing).
- `DELETE /patients/me/fcm-tokens/{token}` removes it on logout.
- Send each push to all of the patient's tokens. Drop a token when FCM answers `UNREGISTERED` or `INVALID_ARGUMENT`.

Keep P7 working for old app versions until they're gone.

## 3. A staging backend for testing

**Now:** the app only knows the production backend. On a real device, many flows change real data, so they're built but not tried: cancel (a hard delete, see #7), booking, task check-in, mark read, delete account, and push registration (#2).

**Change:** a staging backend with its own database, and ideally its own Firebase project. If one already exists, we need its URL. Add test patients with appointments, documents, tasks and notifications, and doctors with free slots. The app then only needs a debug build that uses the staging URL.

## 4. Error codes instead of English text

**Now:** errors look like `{ "error": 400, "message": "Current password is incorrect", "status": 400 }`. `error` is the status number. The only way to tell errors apart is the English `message`. The app matches known messages (`data/api/ErrorMessages.kt`, e.g. `serviceId is required for video consultation`), translates them, and hides the rest behind a generic text. Any change of wording on the backend breaks this without warning.

**Change:** send the same shape from every controller, with a stable code: `{ "status": 400, "code": "WRONG_CURRENT_PASSWORD", "message": "..." }`. The app translates by `code` and keeps `message` for logs. Please share the list of codes. For validation errors, a per-field list would help: `"fields": [{ "field": "email", "code": "ALREADY_USED" }]`.

## 5. Mark the patient's own documents

**Now:** the patient can only delete documents they uploaded. The spec says `creatorLabel` is `"Doctor"` or `"Patient"`, but production sends the uploader's full name (seen on 2026-09-28). The app treats a document as the patient's own when `creatorLabel` equals the patient's full name. If a doctor has the same name as the patient, the delete button shows on the doctor's file, and the delete then fails.

**Change:** add `"uploadedByPatient": true | false` (or `"creatorType": "PATIENT" | "DOCTOR"`) to each document, next to `creatorLabel`.

## 6. Absolute photo and file URLs

**Now:** the API may return relative photo and file URLs (for example `/uploads/...`). The Flutter app turned them into `http://localhost:8090/...`, so those images were broken in production (00-README bug 2, 15 §1).

**Change:** always return full `https://` URLs for photos, avatars, certificates and document files.

## 7. Cancel as a status, not a delete

**Now:** cancelling a booking is `DELETE /patients/me/appointments/{id}` (B5), and the appointment is gone. The patient loses it from their history, and an `APPOINTMENT_CANCELLED` notification points to an appointment that no longer exists.

**Change:** set the appointment's `status` to `CANCELLED` (keep the endpoint, or add `POST .../{id}/cancel`), and return cancelled appointments in the list (B2). The app will show them as cancelled.

## 8. Delete account for phone-only patients

**Now:** deleting an account confirms with an email code (P5, A4, P6). A patient who signed up with a phone number and has no email can't delete their account (14 Q16). App stores require account deletion to work for every user.

**Change:** allow an SMS code for account deletion, and accept it in P6.

## 9. Doctor search cleanups

- **Near me:** with `sortBy=distance`, `latitude`, `longitude` and `radiusKm`, D1 returns the doctors within the radius, then every doctor without coordinates. The app removes those. D1 should return only doctors within the radius.
- **Empty lists as `[]`:** D1 now sends `certificates: null`. The app handles it, but the rule should be `[]` for an empty list, never `null`.

## 10. Google sign-in

**Now:** the login screen's "Continue with Google" is hidden, because the backend has no endpoint for it.

**Change (when product wants it):** an endpoint that takes a Google ID token, checks it, and returns the same response as login (or creates the patient). The app then adds the Google sign-in SDK on both platforms.

## 11. Shifa AI: error codes, attachments, chat sync

**Now:** X1 (`POST /patients/me/copilot/stream`) streams the reply over SSE and keeps nothing. The app saves chats on the device only, so they're lost on logout, on a reinstall, and on another device. An SSE `event: error` carries a `code`, but only `RATE_LIMIT` is documented. Figma's "Add to chat" (Camera, Photo, File) has no API.

**Change:**
- Document every SSE error `code` (safety refusal, model down, too long…). The app then shows a message for each; now all but `RATE_LIMIT` show one generic error.
- Send the daily limit and when it resets (e.g. `X-RateLimit-Remaining`, `resetAt` in the error), so the app can say when to try again.
- An attachment field on X1 (an uploaded document id, or an image), if the product wants Add to chat.
- Optional: chat history on the server (list, get, delete), so chats follow the patient across devices.

## 12. Payments

The full plan is in [payment-flow.md](payment-flow.md).

**Now:** B3 creates the booking first, and a `PENDING` one is paid on a hosted Click or Stripe page ($1). The app learns the result only by polling $3 every 3 s, where only `PAID` is known. The page ends on `/api/payments/checkout/success|cancel` URLs, which the app can only catch in a WebView. There are no failed or expired states, refunds, receipts or saved cards, and nothing holds the slot while the patient pays.

**Change (in priority order):**
- Document every `paymentStatus` and payment `status`: PENDING, PROCESSING, PAID, FAILED (with a reason code), EXPIRED, REFUNDED.
- Make $1 idempotent: one open checkout per appointment, so a second call returns the same `paymentId`.
- Send a push on PAID and FAILED, and stop the payment reminders once paid.
- Redirect the hosted page back to the app (`shifa://payment/return?paymentId=…`). The app can then use Custom Tabs / Safari view instead of a WebView.
- Say whether unpaid bookings are cancelled automatically, and send a `paymentDueAt`. Better: hold the slot while the patient pays (reserve with `expiresAt`) and book only once it's paid.
- Prices before booking for every visit type (in person too), with fees or tax and the total.
- Later: refunds on cancel (with a status), fiscal receipts, a payment history endpoint, saved cards and Apple / Google Pay through Stripe, a payment that carries over on reschedule, and `paymentStatus` from X4 (AI booking).

**Update 2026-10-20:** the backend built the hold (`PENDING_PAYMENT` + `holdExpiresAt`), `paymentProvider` on B3, `payments/methods` and pay later; see [MOBILE_BOOKING_PAYMENT.md](MOBILE_BOOKING_PAYMENT.md). The app follows it (BOOK-04). Still open from this list: fees and tax, receipts, a payment history and saved cards.

**Bug (2026-10-20, production):** every Click booking fails. B3 with `paymentProvider: "CLICK"` answers `400 Could not start Click checkout: Invalid character '=' for QUERY_PARAM in "https://…/api/payments/checkout/success?ref=…"`: the backend puts its own success URL (which carries `?ref=`) into the Click checkout link's query without URL-encoding it, so its URI builder rejects it. Nothing is booked. Fixed in `backend-kotlin-shifa` on `fix/click-return-url-encoding` (`ClickGateway` URL-encodes `return_url`, with a test); it needs merging and a Railway deploy. The app shows the message in the Confirm Payment tray; Stripe is unaffected.

## 13. Live visit

**Now:** the Live Appointment page (Figma 1918:22602) shows Service, Additional Notes and Documents attached, and an End Appointment button. B2 and B4 send no service, notes or documents for a visit, and there's no call to end one. B2 also leaves out `signatureRequested` / `alreadySigned` and once sent `paymentStatus: null` where B4 said `PENDING`, so the app loads B4 for every live visit. The signature tray's "Doctor's notes" has nothing to show for B6 (the visit summary): only 025-2 forms (B11) have complaints and a diagnosis.

**Change:**
- Add the booked service (id, title), the patient's notes and the attached document ids to the appointment (B2 and B4).
- Send the same fields in B2 as in B4 (`signatureRequested`, `alreadySigned`, `paymentStatus`).
- Say what ending a visit means for a patient, and add a call for it if it's more than leaving.
- Give B6 the doctor's notes to show, or link the visit's 025-2 form, so the tray can show what's being signed.

- For the appointment details (Figma 1921:25419, 1661:17567). Checked against the backend's own OpenAPI (`/v3/api-docs`, 2026-10-19): none of this exists yet.
  - ~~**Service**~~: done (MOBILE_BOOKING_PAYMENT §5, 2026-10-20): `serviceId` and `serviceTitle` are on `AppointmentDto`, and the details page shows them.
  - **Attach Document after booking:** `POST /patients/me/appointments/{id}/documents` with `{ "documentIds": [101] }` (documents already uploaded with F2), and the attached ids as `documentIds` on `AppointmentDto`. Today documents can only be attached in B3.
  - **Documents from the visit:** `appointmentId` on `PatientDocumentDto` (F1), set when the doctor uploads during or after the visit.
  - **Tasks from the visit:** `appointmentId` (and the doctor's id and name) on `TaskDto` (T1, T2). It has no doctor today, so the app can't tell where a task came from.
- Say what happens to the old visit on a reschedule (B3 with `rescheduleAppointmentId`).

**Until then:** the rows without data are hidden, End Appointment just leaves the page, the tray shows the visit summary, and a reschedule asks for a video visit's service again.

## 14. Task entries

**Now:** the task page (Figma 1662:18317, 1891:21727) has a "From Appointment" card and a Delete Entry button on a submitted check-in. T1 / T2 have no link from a task to the visit that set it, and there's no call to delete or change a submitted check-in (T4 only creates one).

**Change:**
- Add the visit the task came from to the task (`appointmentId`, or the doctor and time).
- If patients may undo an entry: `DELETE /tasks/my-tasks/{taskId}/check-ins/{checkInId}` (or a PUT to change it), with a rule for how long after submitting it's allowed.

**Until then:** no From Appointment card and no Delete Entry; a submitted day only shows what was sent.

## Questions to confirm (15 §6.4)

- Whether the error shape is the same across all controllers (see #4).
- Whether P4 `/profile/photo` exists.
- Whether B1 `day` uses the device time zone or the profile `timeZone`.
- Which zone T3 `scheduledDate` / `scheduledTime` are in. The app reads them on the device clock, so outside Tashkent a check-in opens hours early or late.
- All values of `paymentStatus`, payment `status`, AI summary `status` and appointment `status`. Seen in B2 so far (2026-10-11): `REQUESTED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`; `paymentStatus` only `NOT_REQUIRED`.
- Who confirms a booking. B3 creates every booking as `REQUESTED`, and only some later become `CONFIRMED`, so a doctor or clinic confirms them by hand. Unconfirmed ones stay `REQUESTED` after their date. If bookings should be accepted automatically (always, or per doctor), the backend has to do it; the app shows `REQUESTED` as Pending.
- What `serviceItems[].prices[].locationId` means (not in the spec). The app reads `null` as "any place, video included" and offers a service for video only if it's free or has such a price, since B3 answers a clinic-only one with "Selected service has no price configured". Better: a per-service flag for video, or B3 taking the clinic price.
- Whether P5 sends the deletion code itself, and how the A4 `purpose=ACCOUNT_DELETION` code is tied to `challengeId`.
- Whether the server enforces the video join window and the task check-in window.
- Whether D5 (submit a review, under `/public`) needs the token.
