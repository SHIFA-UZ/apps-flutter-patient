# Design system

Figma file `B7f95gNrRt8HYYzlGZdk9N` (a copy of `o0MqTfUEfhBX49VljFhp70` with the same node IDs; newer designs are only in the copy): tokens page `1572:13099`, Components page `1572:11699`. Light mode only. The 2026-10-24 design update came from a later copy, `vcvTRthZBesjFI0wu4uzJO` (same node IDs).

Page backgrounds: tab pages are surface; every page outside the tab bar is canvas under a surface header (`ShifaPageElementType.SubBody` for a padded body), except the two chats (doctor chat, Shifa AI chat), which stay surface.

## Tokens (`ui/theme/`)

- `Color.kt`: `ShifaColors.Text|Icon|Bg|Border|Interactive|Alpha.*`, named after Figma `semantic/color/*`. Colours Figma sets as raw values (no variable) go in `ShifaColors.Raw.<Component><Part>`.
- `Typography.kt`: Manrope. `MaterialTheme.typography` slots are annotated with the Figma component that uses each one. Styles with no M3 slot are in `ShifaTextStyles`.
- `Dimens.kt`:
  - `ShifaSpacing.Inset|Gap|Stack` (`semantic/space/*`), `ShifaSize.S*` (`shifa/size/*`), `ShifaRadius`, `ShifaShadow` (applied with `Modifier.dropShadow`).
  - `ShifaDimens.<Component><Part>` holds raw Figma values that have no variable.
- `Modifier.lineBox(style)` sizes text to Figma's line boxes. Compose pads line heights smaller than the font's own height, so use it wherever the height must match Figma.

## Widgets (`ui/widgets/`, nav bar in `ui/navigation/`)

| File | Composables |
|---|---|
| ShifaButtons | `ShifaPrimaryButton`, `ShifaSecondaryButton`, `ShifaTertiaryButton`, `ShifaActionButton` (pill), `ShifaIconButton` (`1572:13829`: 42 round, an 18 icon in 12 padding; secondary is canvas with a dark, grey or red icon; `shadow = false` in the chat tray) |
| ShifaInputField | `ShifaInputField` (`clearable` hides the X; `trailingIcon` replaces it, e.g. the password eye) |
| ShifaSearchField, ShifaDropdown, ShifaSwitch | `ShifaSearchField`, `ShifaDropdown` (opens inline; `onSelect` gets the index, so labels may repeat), `ShifaSwitch` |
| ShifaChips | `ShifaChip` (filter / slot, padding 16 × 8 on both, `1585:12641`), `ShifaChipGrid` (4 columns), `ShifaCarousel` |
| ShifaCalendarCard | `ShifaCalendarCard` (`1662:18317`): calendar-card, a month toggle and date-bubbles (non-month / default / disabled / filled); a null arrow callback hides it |
| ShifaRating | `ShifaRating`: five stars, the rating filled amber; tappable with `onRate` (review tray, `1887:19946`) |
| ShifaReviewCard | `ShifaReviewCard` (`1655:16298`): doctor-card, review type: avatar, name, date, stars, comment |
| ShifaSignaturePad | `ShifaSignaturePad` (`1919:23186`): the input-field box as a pad; the caller keeps the strokes, `signaturePng` makes the PNG |
| ShifaTag | `ShifaTag` (primary / secondary / tertiary, `1585:12637`): Bold 12/12, padding 12 × 8 |
| ShifaCards | upcoming (`1572:13984`) and past (`1573:12114`) appointment cards, `ShifaDoctorCard` full / compact / in (`1590:9994`; avatars 84 / 48 / 68, name Bold 16, the in card hugs its content), document, document with an action button (`ShifaDocumentActionCard`, Request to sign, `1918:22602`), certificate (`1592:9994`), doctor service (`1670:13024` / `1921:24533`: name Bold 16, a chevron that opens the description; no description, no chevron), check-in (`1662:18576`, the ticked card takes its form as a slot), task (optional progress bar), `ShifaSettingsItem` (optional icon, destructive red for Delete my Account). Under `LocalCardOnSurface` the cards take the on-white look of the notification trays: canvas fill, no shadow, no chevron. Every avatar (`ShifaAvatar`: headers, profile, doctors, chats, reviews) without a photo, or whose photo fails to load, shows the Shifa AI header picture (`img_shifa_ai_header`), cropped round |
| ShifaSection | `ShifaSection`: header (Bold 20/22) with an optional arrow; background type card-on-color / on-white (padding 16) / transparent |
| ShifaPageHeader | `ShifaMainHeader` (32 title on every tab; the bell's unread count is a `ShifaNotificationBubble`), `ShifaSubHeader` (24 title) |
| ShifaNavBar | `ShifaNavBar`, `ShifaTab` (`1572:13574`) |
| ShifaNotification | `ShifaNotification` (5 types, `1598:10299`: surface, title Bold 16, body 12/14, a 22 high title bar; unread: a bold brand time and the bubble, an unread chat on canvas), `ShifaMessageNotification` (photo, unread count with a brand time, optional tap), `ShifaNotificationBubble` (`1785:18917`: the unread count, white Bold 10 on brand, 99+ above 99) |
| ShifaBanner | `ShifaBanner` error / warning / success: an inline message box for screens (login lockout, register errors). No Figma component; styled like `ShifaNotification`. |
| ShifaPager, ShifaPageElement, ShifaImageContainer | pager dots, page-element slots (the background is painted under insets the caller adds, e.g. `statusBarsPadding`), 302dp image box |
| ShifaSkeleton | Not in Figma: `ShifaSkeleton` (a pulsing grey block, still in previews), `ShifaCardSkeleton` (avatar + text bars in the card chrome), `ShifaListSkeleton`, `ShifaChatSkeleton`, `ShifaChipsSkeleton`. Every first load shows these instead of a spinner (`HomeSection` for Home, Bookings, the full lists, Tasks, Upcoming; Messages, the chats, Notifications, Documents, the doctor profile and its reviews, the booking's time slots). The document viewer keeps its spinner. |
| ShifaChat | `ShifaChatBar` (`1673:14877`; recording: an animated waveform and a stop button, Figma's is static), `ShifaChatBubble` and `ShifaChatImageBubble` (`1673:14821`; sent or received, optional caption; not in Figma: an optional sent `time` under the bubble, caption 10/14 tertiary, used by doctor chats), `ShifaChatReplyBubble` (Shifa AI's reply, `1673:15033`: no fill, an optional time, then Copy / Regenerate / Share action-buttons), `ShifaChatVoiceBubble` (not in Figma: play / pause, progress and m:ss, Flutter's look), `ShifaChatThinkingBubble` (three pulsing dots, unfilled) |
| ShifaTray, ShifaPopUp | `ShifaTray`, `ShifaChatTray` (`1681:13188`, row as `1772:17215`: no shadows), `ShifaPopUp` (`1702:14263`). They only draw: host them in `ModalBottomSheet` / `Dialog` (see `ShifaTray.kt`). |

Icons are Figma exports in `composeResources/drawable` (`ic_<name>_<size>`, `img_*`). `img_logo` is the white Shifa mark; tint it. `ic_eye_20` and `ic_eye_off_20` are Feather icons, because Figma has no eye icon. `ic_upload_22` and `ic_calendar_20` come from the create-account frames.

## Previews and tests

- Previews live in `androidMain/.../ui/widgets/*Previews.kt` and use `Gallery`. Screen previews use `PreviewTheme`. Both force inspection mode so Compose resources load under Robolectric.
- Every widget file has a `*ScreenshotTest` that captures its preview into `composeApp/screenshots/`.

## Known differences from Figma / open questions

- Section, tray and pop-up titles are raw black in Figma; they use `Text.Primary` (#272727) here.
- Certificate illustration has no soft shadow (vector drawables can't blur). The doctor card "in" badge sits about 2px low.
- Chat send button has no disabled state; the recording waveform moves (Figma's is static) and has a stop button on the right.
- Transparent sections keep 12dp of vertical padding that Figma's don't have, so the gap between Home's sections is a little larger than Figma's (12).
- Open questions:
  - `ShifaImageContainer`: Fit or Crop? Currently Fit.
  - Dropdown: inline or popup? Currently inline.
  - Search icon colour when focused.
