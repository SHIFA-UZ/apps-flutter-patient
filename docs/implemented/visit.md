# Live visit and video call

## Home's Live Appointment section (`ui/home/HomeScreen.kt`)

- Figma `1647:10075`. A visit inside its join window (start − 5 min to end + 15 min, 06) leaves Upcoming for its own section (card-on-white, cards without a shadow, "Now", a **View** button). Upcoming below is now card-transparent with shadowed cards, as in the new Figma, and hides when only live visits are left.
- View (or the card) opens the Live Appointment page. Upcoming's page still lists every visit, live ones with Join.

## Live Appointment (`ui/visit/LiveAppointmentScreen.kt`, BOOK-10 in part)

- Figma `1918:22602`. The doctor's header card (D2, else the visit's copy), then **Appointment Details**, collapsible with the chevron: Clinic, Video Consultation yes / no, Date and Time, Location (`locationAddress`, `locationLabel`, `location`, the doctor's address; "Video consultation" for video), Additional Notes (`reason`).
- The page reloads the visit with B4, because B2 has no `signatureRequested` / `alreadySigned` (seen 2026-10-19). While a signature is due, **Documents** shows the Request to sign card (`ShifaDocumentActionCard`) with Sign now.
- Sign now opens the tray (Figma `1919:23186`): the notes box, the signature pad (`ShifaSignaturePad`), Complete Sign off. An empty pad says "Please sign in the box above". Strokes stay inside the box: the pad clips them, and a finger past the edge draws along it. The strokes go to B6 as a PNG on white, plain base64. On success the tray closes, a banner says so and B4 reloads, so the card goes. A failure stays in the tray. The tray can't be dragged shut (a stroke would drag it).
- Video visits have **Join Video Call** above **End Appointment**, at any time (the user's call: signing doesn't block joining). End Appointment leaves the page; the backend has no end-visit call.
- Not built: Service and Documents attached rows (no such fields in B2 or B4), the 025-2 form signing (B11 / B12, reached from `SIGNATURE_REQUESTED` pushes with a `patientFormId`). The tray's "Doctor's notes" shows the visit summary, as B6 signs the summary and has no notes.

## Video call (`ui/visit/VideoCallScreen.kt`, BOOK-08)

- Figma `1791:19370` (Join Now) and `1793:15925` (waiting room). Full screen over everything.
- Opening it asks for the camera and microphone (Android runtime permissions; iOS `AVCaptureDevice`) and starts the preview. A denial shows the media-permission text.
- Join Now: V1 for the Daily token and room, then Daily's join. V1's refusals show their texts (not yet in the window, ended, payment required, not your visit); others show "Failed to start video call".
- Joined: "Waiting for doctor to start appointment" with a turning loader while the doctor isn't in, the patient's camera in the tile. When the doctor joins, their camera fills the tile and the patient's goes to a small picture (not in Figma). The pill: camera and microphone on / off (dimmed when off, not in Figma), switch camera, hang up. Hang up leaves the call; so does the doctor ending it. Back before joining leaves; Back during the call (the header arrow or the system Back) shrinks it to a 120×160 window over the app (not in Figma): the doctor's video (else the patient's), draggable within the screen, a tap makes it full screen again. The app works underneath, so the patient can open the Live Appointment page and sign. Join Video Call on that page also brings it back. A push tap shrinks the call instead of ending it. Trays (the sign tray) are their own windows, so they dim the small window while open.
- The call: `utils/VideoCall.kt`. Android uses Daily's SDK (`co.daily:client`); iOS uses Daily's Swift package through `iosApp/iosApp/DailyVideoCall.swift`, which implements the Kotlin `NativeVideoCall` and is registered in `AppDelegate`. Loudspeaker on join (06 §3.6).
- Not built: the join-timeout retry, the iOS black-camera workaround (06 §3.6). Tried on the Android emulator with demo data up to Join Now (permissions, the live camera preview, V1's refusal). Not joined in a real call yet: that needs a video visit in its window on the real backend.
- Android declares CAMERA now, so the photo camera (documents, chat, profile) asks for it before TakePicture.

## Debug menu

- Demo data: the live video visit (9101) asks for a signature; B4 and B6 are faked. V1 is refused (a fake backend can't open a Daily room), so the call stops at Join Now.
