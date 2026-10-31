# Booking & Payment — Mobile Integration Guide

How the patient app (and the doctor app's service editor) should implement the new booking payment flow.

**TL;DR:** a priced service is now either **pay now** (online, Click/Stripe) or **pay later** (at the clinic). For pay-now, the app picks a provider *before* booking, sends it with the booking, gets a `checkoutUrl` back, and the slot is held for **15 minutes** while the patient pays. Payment success is only ever confirmed by the backend (webhook) — the app polls, it never trusts the browser redirect.

The old flow (book first, pay later from the appointments list) still works, so the current app version does not break.

---

## 1. Service payment types

Services come from `GET /api/public/doctors/{id}` → `serviceItems[]`:

```json
{
  "id": 42,
  "title": "Online consultation",
  "isFreeConsultation": false,
  "paymentType": "PAY_NOW",
  "videoAllowed": true,
  "prices": [{ "amountMinor": 30000000, "currency": "UZS", "locationId": null }]
}
```

| Kind | How to detect | Booking result |
|---|---|---|
| **Free** | `isFreeConsultation == true` (ignore `paymentType`) | `CONFIRMED`, `paymentStatus = NOT_REQUIRED` |
| **Pay later** | `paymentType == "PAY_LATER"` | `CONFIRMED`, `paymentStatus = PAY_IN_CLINIC` |
| **Pay now** | `paymentType == "PAY_NOW"` | `PENDING_PAYMENT` → pay → `CONFIRMED` + `PAID` |

**Video rule:** video is only allowed for free or pay-now services. Use `videoAllowed` to hide/disable the video option. Booking video on a pay-later service returns `400 Video consultation is not allowed for this service`.

**Amounts:** every `amountMinor` is in hundredths of the currency, UZS included (tiyin): the doctor app stores a typed price × 100; Click is charged `amountMinor / 100` so'm and Stripe receives `amountMinor` as its unit amount. `30000000` → show `300 000 UZS`. Online payment is UZS-only. (Corrected 2026-10-31; this line used to say UZS had no subunit, and the patient app showed UZS prices ×100 too large.) Stripe refuses amounts under about 50 US cents (`amount_too_small`), roughly 6 000 so'm.

---

## 2. Pay-now flow (new app)

```
Select service + slot
   │
   ▼
GET /api/payments/methods?serviceId=&locationId=      ← which providers to offer
   │  1 item → single "Pay with X" sheet
   │  2 items → choice sheet (Click / Stripe)
   ▼
POST /api/patients/me/appointments  { ..., paymentProvider: "CLICK" }
   │  → status PENDING_PAYMENT, payment.checkoutUrl, holdExpiresAt
   ▼
Open checkoutUrl in in-app browser (SFSafariViewController / Chrome Custom Tabs)
   │  show countdown to holdExpiresAt
   ▼
Browser closed / app resumed
   ▼
Poll GET /api/patients/me/appointments/{id}  until status leaves PENDING_PAYMENT
   ├─ CONFIRMED + PAID      → success screen
   ├─ PAYMENT_FAILED        → "payment failed", offer to book again
   └─ EXPIRED               → "time ran out, slot released", offer to book again
```

### 2.1 Get payment methods

`GET /api/payments/methods?serviceId={id}&locationId={id?}` — patient auth.

```json
[
  { "provider": "CLICK",  "amountMinor": 30000000, "currency": "UZS" },
  { "provider": "STRIPE", "amountMinor": 30000000, "currency": "UZS" }
]
```

- **Empty list** → service is free or pay-later. Skip the payment sheet.
- Always send `locationId` for in-clinic bookings — price can differ per location.
- Always send `paymentProvider` on booking, **even when only one method is listed**. Omitting it falls back to the legacy flow (section 4).

### 2.2 Book

`POST /api/patients/me/appointments`

```json
{
  "doctorId": 7,
  "startAt": "2026-10-25T09:00:00Z",
  "slotMinutes": 30,
  "reason": "Headache",
  "isVideo": true,
  "serviceId": 42,
  "locationId": null,
  "documentIds": [],
  "rescheduleAppointmentId": null,
  "paymentProvider": "CLICK"
}
```

Response (pay-now):

```json
{
  "id": 1201,
  "status": "PENDING_PAYMENT",
  "paymentStatus": "PENDING",
  "paymentType": "PAY_NOW",
  "paymentAmountMinor": 30000000,
  "paymentCurrency": "UZS",
  "holdExpiresAt": "2026-10-20T10:15:00Z",
  "isVideoConsultation": true,
  "payment": {
    "id": 880,
    "provider": "CLICK",
    "checkoutUrl": "https://my.click.uz/services/pay?...",
    "expiresAt": "2026-10-20T10:15:00Z"
  },
  "...": "other AppointmentDto fields (section 5)"
}
```

If this request fails (e.g. provider error), **nothing is booked** — the whole request is rolled back. Safe to show the error and let the user retry.

### 2.3 Pay

- Open `payment.checkoutUrl` in an **in-app browser**, not a WebView you control the redirect of, and not the external browser.
- After payment, the provider redirects to a plain backend HTML page ("Payment successful / cancelled — return to the app"). There is **no deep link**. Detect the browser being dismissed / app returning to foreground and start polling.
- Show a countdown to `holdExpiresAt`. The slot is held for 15 minutes (server-configurable — always use `holdExpiresAt`, never hardcode).

### 2.4 Confirm (poll)

Poll `GET /api/patients/me/appointments/{id}` every 2–3 s for up to ~60 s after returning, then fall back to pull-to-refresh.

| `status` | `paymentStatus` | Meaning | UI |
|---|---|---|---|
| `PENDING_PAYMENT` | `PENDING` | Waiting for payment / webhook | Spinner + countdown + "Continue payment" |
| `CONFIRMED` | `PAID` | Paid, booked | Success |
| `PAYMENT_FAILED` | `FAILED` | Provider declined | Failed, slot released |
| `EXPIRED` | `FAILED` | Hold ran out or checkout session expired | Expired, slot released |

You can also poll `GET /api/payments/{paymentId}` (`status`: `PENDING` / `PAID` / `FAILED` / `CANCELLED`), but the **appointment** is the source of truth for what to show.

**Never mark the booking as paid based on the redirect URL** — only on `paymentStatus == "PAID"` from the API.

### 2.5 Continue an unfinished payment

If the patient closed the browser while still `PENDING_PAYMENT` and before `holdExpiresAt`:

- Reopen the same `checkoutUrl` (from the booking response, or `GET /api/payments/{paymentId}` → `checkoutUrl`).
- Avoid calling `POST /api/payments/consultation/checkout` again for the same appointment unless the URL is lost — it creates a new payment session.

After `EXPIRED` / `PAYMENT_FAILED` the appointment is dead (`400 This appointment is no longer payable`). The patient must book again.

---

## 3. Pay-later & free flows

No payment step: `/payments/methods` returns `[]`, so skip the payment sheet. Don't send `paymentProvider` (the backend ignores it for these kinds anyway).

- Booking returns `CONFIRMED` immediately.
- Pay later: show a **"Pay at clinic"** badge (`paymentBadge == "PAY_IN_CLINIC"`) with the amount.

---

## 4. Legacy flow (current app, no `paymentProvider`)

Still supported so older app versions keep working:

1. `POST /api/patients/me/appointments` without `paymentProvider` → `status = REQUESTED`, `paymentStatus = PENDING`, no `payment` object, **no hold timer**.
2. Later: `POST /api/payments/consultation/checkout` `{ "appointmentId": 1201, "gateway": "CLICK" }` → `{ paymentId, externalRef, checkoutUrl, status, gateway }`.
3. Webhook confirms → `CONFIRMED` + `PAID`.

New app builds should use section 2 instead.

---

## 5. Appointment DTO — new fields

Returned by booking, `GET /api/patients/me/appointments` and `GET /api/patients/me/appointments/{id}`.

| Field | Type | Notes |
|---|---|---|
| `status` | string | New values: `PENDING_PAYMENT`, `EXPIRED`, `PAYMENT_FAILED` |
| `paymentStatus` | string | New value: `PAY_IN_CLINIC` |
| `paymentType` | `PAY_NOW` \| `PAY_LATER` \| null | Snapshot at booking. Null for free / old bookings |
| `paymentBadge` | `PAID` \| `PAY_IN_CLINIC` \| null | Ready-to-render badge |
| `holdExpiresAt` | ISO instant \| null | Only while `PENDING_PAYMENT` |
| `isVideoConsultation` | bool | Use instead of parsing `location` |
| `serviceId`, `serviceTitle` | | |
| `isLocked24h` | bool | Visit starts in < 24 h |
| `canCancel`, `canReschedule` | bool | **Drive the buttons from these** |
| `refundRequired` | bool | Paid but visit not kept — clinic refunds manually |
| `payment` | object \| null | Only on the booking response for pay-now: `{ id, provider, checkoutUrl, expiresAt }` |
| `documentsCount` | int | Detail endpoint only |
| `doctorClinic` | string | Now the selected location's clinic when set |

**List filter:** `GET /api/patients/me/appointments` hides `EXPIRED` and `PAYMENT_FAILED` by default. Pass `?status=EXPIRED` or `?status=PAYMENT_FAILED` to fetch them explicitly.

---

## 6. Cancel & reschedule — 24 h lock

- Patients can no longer cancel or reschedule a visit that starts in **less than 24 hours**.
  - `DELETE /api/patients/me/appointments/{id}` → `409` with `message: "APPOINTMENT_LOCKED_24H"`
  - Booking with `rescheduleAppointmentId` pointing at a locked visit → same `409`
- Hide/disable the buttons using `canCancel` / `canReschedule`; show a "Contact the clinic to change this visit" hint when `isLocked24h` is true.
- Cancelling a **paid** visit is allowed (outside 24 h) and sets `refundRequired = true`. Show "Refund will be processed by the clinic".
- Cancelling a `PENDING_PAYMENT` visit releases the slot. If the patient then pays anyway on the open checkout page, the backend flags `refundRequired = true`. Since 2026-10-30 an **unpaid** `PENDING_PAYMENT` visit can be cancelled inside the 24 h window too (`canCancel` is true for it): closing the checkout must not leave a booking behind. The lock keeps protecting confirmed and paid visits.

---

## 7. Edge cases the app should handle

| Case | Backend behaviour | App |
|---|---|---|
| Hold expires while patient is on checkout page | Slot released, `EXPIRED` | Show expired state on return |
| Patient pays **after** expiry, slot still free | Re-confirmed: `CONFIRMED` + `PAID` | Just render what polling returns |
| Patient pays after expiry, slot taken | Stays `EXPIRED`, `paymentStatus = PAID`, `refundRequired = true` | Show "Payment received, slot no longer available — refund in progress" |
| Booking a past slot | `400 Time slot is in the past` | Refresh slots |
| Slot taken in the meantime | `409` overlap message | Refresh slots |

---

## 8. Error reference

All errors use `{ "error": <code>, "message": "<text>", "status": <code> }`.

| HTTP | `message` | Where |
|---|---|---|
| 400 | `Provider not enabled` | Booking — provider not in `/methods` list |
| 400 | `Video consultation is not allowed for this service` | Booking — video + pay-later |
| 400 | `serviceId is required for video consultation` | Booking |
| 400 | `Online payment is available in UZS only (currency=…)` | Booking / `/methods` |
| 400 | `Selected service has no price configured` | `/methods` |
| 400 | `Click payments unavailable (…)` / `Could not start Click checkout: …` | Booking / checkout |
| 400 | `Time slot is in the past` | Booking |
| 400 | `This appointment is no longer payable` | Checkout on `EXPIRED` / `PAYMENT_FAILED` / `CANCELLED` |
| 400 | `This appointment does not require online payment` | Checkout on free / pay-later |
| 409 | `APPOINTMENT_LOCKED_24H` | Cancel / reschedule within 24 h |
| 409 | overlap message | Slot already taken |

---

## 9. Doctor app — service editor

`/api/doctors/me/services` (create/update) and its responses now include `paymentType`:

```json
{ "title": "Consultation", "isFreeConsultation": false, "paymentType": "PAY_LATER", "prices": [...] }
```

- Values: `PAY_NOW` (default) or `PAY_LATER`.
- On update, omitting `paymentType` keeps the current value.
- `isFreeConsultation: true` + `paymentType: "PAY_LATER"` → `400 A free consultation cannot be pay-later`. Hide the selector when "Free" is on.
- Hint in UI: pay-later services cannot be booked as video.
