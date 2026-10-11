# Bookings and doctors

## Bookings tab (`ui/bookings/BookingsScreen.kt`, BOOK-05 + DOC-01 in part)

- Figma: `1648:11080`. Home's header (avatar → Profile, bell → Notifications), a doctor search, filter chips, then, on All, three sections: Recommended doctors (a carousel), My Doctors and Past Appointments. Any other chip shows only its doctors, as full cards with the Clinic / Online / Nearby tags (Figma `1653:12376`). The first open loads everything; pull to refresh reloads it. A 401 logs out. The ViewModel lives in the tab shell, so the tab keeps its state across tab switches. Figma's "Find Doctors near you" row is hidden for now (the user's call).
- Figma has no upcoming list and no 06 §4.2 status tabs, so the Bookings tab shows none. Upcoming visits are on Home, including ones the doctor hasn't confirmed yet (marked Pending).
- Recommended doctors: D1 with the search (350 ms debounce, 07) and the chip's params. All: none. Remote: `consultationType=online`. Today: `availableWithin=today`. Emergency: `sortBy=availability` (the spec has no emergency filter). The card shows the next free slot. D1 reads every page (50 each, the backend's cap, up to 20 pages). Not built: the filters sheet, scroll pagination and the availability enrichment (07 §3.8).
- Near me (07 §2.1): asks for the location permission when needed, then takes one medium-accuracy fix (Android: `fused`, else network, else GPS; on the 30 s timeout the freshest last known fix; iOS: `CLLocationManager.requestLocation`). D1 gets `sortBy=distance`, `latitude`, `longitude` and `radiusKm=50`. The backend appends every doctor without coordinates after the near ones, so the app drops those. Services off, a denial, "don't ask again" or no fix go back to All with a banner. The fix is kept for the session. No fallback to the profile's coordinates: the profile has none yet (PROF-04 map picker).
- Recommended shows 10 in the carousel. With more, the arrow opens the Recommended page. With none it is hidden, unless a search is typed, so "No doctors found" still answers it.
- My Doctors: the doctors of the most recent bookings, loaded with D2, as on Home ([home.md](home.md)). With more than 3, the arrow opens the My Doctors page.
- Past Appointments: one unfiltered B2, over or cancelled, newest first. The badge is red for cancelled, green for confirmed or completed, yellow for anything else. Cancelled cards don't open. With more than 3, the arrow opens the Past page.
- Doctor cards open the doctor profile, appointment cards open the details.

## List pages (`BookingsListScreen.kt`, DOC-01 in part)

- Figma: `1655:15670` (My Doctors), `1655:15892` (Past); Recommended follows the same pattern. An overlay above the tabs with a back header and every item of the section, as flat cards (Figma). Recommended's are full cards with tags, like the chip lists. The page shares the tab's ViewModel, so Home's My Doctors arrow opens the same page and it loads B2 and D1 if the tab never did.
- My Doctors cards show "Last Visit dd.MM.yyyy": the doctor's latest confirmed or completed past visit, else no line.
- Not built: the ⋮ menu (no Figma for it), the nav bar Figma shows under Past, pull to refresh and lazy loading (a plain column).

## Appointment details (`AppointmentDetailsScreen.kt`, BOOK-06)

- Figma: `1662:17723`. An overlay above the tabs, opened from Home and the Bookings tab. It shows the list's B2 appointment (no B4 refetch). The doctor card uses D2 when the appointment has a `doctorId`; if D2 fails, it keeps B2's name, profession and clinic. The card opens the doctor profile.
- Sections: Date and Time (local, "Monday 12:00 - 12:30, 28 September 2026"), Location (video consultation, else the location, else the doctor's address, else the clinic) and Reason for visit ("check up" shows as "Check Up").
- Cancel is enabled only for an open visit (not cancelled or completed) at least 48 h away. It needs a confirm, then B5, then back to the list with "Appointment cancelled successfully", and Home refreshes. A failure keeps the screen with the error. Figma says 24 h; 06 says 48 h, which is used. Under 48 h, Figma's warning shows instead.
- Contact dials the doctor's phone, else emails them (06 quirk 13). It's disabled without either.
- Not built: reschedule, the ⋮ menu, payment, the visit summary, signing, review and Join. They come with their flows. Not tried on a device: the test account has no appointments, and cancel is a hard delete on production.

## Doctor profile (`ui/doctors/DoctorProfileScreen.kt`, DOC-02 in part, DOC-03)

- Figma: `1655:16298`. An overlay opened from any doctor card. It shows the card's copy at once, then loads D2; an error shows Retry. The header card has no shadow (Figma). Tags: Clinic when in person, Online when online (07 §4.1), Nearby within 50 km of the Near me fix (D1's `distanceKm`, else Haversine from the doctor's coordinates). No fix, no Nearby.
- Tabs: About (biography, else further information), Services (Figma service cards: Free, the first price as "150000.00 UZS", else "Price not set"; old string services as text), Certificates (cards titled with the file name, opened in the documents viewer: DOC-03) and Clinic (address, clinic, phone, email, Telegram and Instagram; the links open the dialer, mail or the browser). Each tab has an empty text.
- Create Booking opens the booking page.
- Not built: reviews (D3/D4), the service detail sheet, open in maps, the ⋮ menu, experience and languages (not in Figma).

## Create booking (`CreateBookingScreen.kt`, BOOK-01/02/03)

- Figma: `1662:18783` (empty), `1662:19457` (filled). An overlay above the doctor profile. Figma titles it "Doctor Profile" (a copy-paste), so the page says Create Booking. The header card is the profile's, refreshed with D2.
- Location: D3, ids `<= 0` dropped. The primary location is used, else the first; an error means none. There's no location step (not in Figma).
- Slots: B1 day by day for 30 days from today (7 days at a time), with `locationId` only in person. Past slots are dropped. The scan stops on the first day with free slots and selects the first one. With none, it ends on the last day with "No available time slots". The date button shows "Select" only while scanning. The picker allows today to +90 days and loads just that day.
- Video: the switch clears the service and rescans without a location. The service dropdown ("— Free" for free ones) shows only for video, and lists only services that are free or have a price with no `locationId`. A service priced only at a clinic is left out, because B3 rejects it for video ("Selected service has no price configured"). A doctor without such services shows the warning. The caption under the title is the chosen visit.
- Attachments: PDFs only, up to 5, each uploaded with F2 `category=PRE_APPOINTMENT` when picked. An empty file is skipped with a banner.
- Complete Booking checks, in order: video needs a service, then no open visit of yours at the same start (B2), then B3. Errors show above the button. On success all overlays close, the Bookings tab shows "slot booked", or the pending notice for `paymentStatus` PENDING, and Home refreshes.
- Not built: the location step, the $1 checkout WebView (BOOK-04), removing an attachment, reschedule and the ⋮ menu. A new booking shows in Home's Upcoming, marked Pending until the doctor confirms it. Not pressed on a device (production).
- The switch keeps its 48 dp touch target, so the Video card is taller than Figma's.
