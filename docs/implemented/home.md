# App shell & Home

## Tab shell (`ui/navigation/MainScreen.kt`, SHELL-01/03)

- The Figma bar (`ShifaNavBar`): Home, Bookings, Shifa AI, Documents, Messages. The labels are localized. The plan's 4-tab list (Home, Bookings, Documents, Doctors) is older than the design; this follows Figma.
- Only Home is built. Every other tab and every page Home links to (Notifications, Tasks, Doctors, Appointment) is a "Coming soon" placeholder. The avatar opens Profile ([profile.md](profile.md)).
- Android back: a page closes, then a tab other than Home goes to Home, then the app closes (02 §10).
- Tabs keep their state (`rememberSaveableStateHolder`). ViewModels live in a store scoped to the signed-in session, so none outlive a logout. This is hand-rolled until the nav library (deferred) gives per-destination scoping. SHELL-02 (routes, redirect guard) is not built.
- Header (SHELL-03): `ShifaMainHeader` with the avatar (Profile) and the bell (Notifications). The bell shows the N2 unread count, capped at 9+. Figma has no badge, so it uses the Material one. The chat icon and its badge are not in the Figma header.

## Home (`ui/home/`, HOME-01..04)

- Figma: `1647:10075`. It shows the header ("Hello, {first name}", from P1), then Upcoming appointments, My Tasks and My Doctors. Each section shows up to 3 cards (10 §3.4). Pull to refresh reloads everything.
- Loading: P1, N2, B2 and T1 run in parallel, then D2 runs for the doctors. A list shows a spinner until it first loads and its empty text when empty. A failed load shows one error banner. A failed reload keeps the previous lists. A section whose first load failed is hidden. Failed P1 and N2 calls only cost the name and the badge. A 401 on P1 logs out. The app-wide 401 handler (CORE-05) is not built.
- Upcoming (HOME-01): one unfiltered B2 call, instead of Flutter's `status=CONFIRMED&startDate=today`. It keeps confirmed visits (a null status counts as confirmed) until their join window closes (start − 5 min to end + 15 min, 06). Flutter dropped a visit once it started. During the window the card says "Now", and video visits show Join. The list is sorted soonest first. The section arrow shows only with more than 3 visits. Figma hides it; it replaces Flutter's "all bookings" button and goes to the Bookings tab. Tapping a card or Join opens the Appointment placeholder.
- My Tasks (HOME-02): T1, open tasks only (not completed or cancelled). Flutter showed a static card. Each card shows the category tag and "Starts: dd.MM.yyyy" for a future start, else "Started: …" (12 §3).
- My Doctors (HOME-03): Figma replaces "Doctors near me" with "My Doctors". The list is the doctors of the 3 most recent bookings, taken from the same B2 response and loaded with D2. There's no location flow and no D1 call. A doctor that fails to load is left out. The card shows the rating as "x.x(N)", verified when there are certificates, and the next free slot as "Today · HH:mm" / "Tomorrow · HH:mm" / "dd.MM · HH:mm".
- HOME-04: Home has its own ViewModel and state, so it can't overwrite other tabs' lists.
- Professions show as the API sends them. The translated catalog comes with DOC-01.
- `sanitizeError` now applies the auth-only 401 "Invalid credentials" and 404 "No account found" texts to `/auth/*` calls only (01 §8). Elsewhere, a 404 falls back to the call's default message.
