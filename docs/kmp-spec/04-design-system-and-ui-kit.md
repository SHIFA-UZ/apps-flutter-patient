# 04 — Design System and UI Kit

Scope: `lib/core/theme/*`, `lib/core/layout/*`, `lib/core/widgets/*` (non-lock widgets), `lib/core/constants/assets.dart`, `assets/`, fonts.
App-lock widgets (`app_lock_screen.dart`, `app_lock_wrapper.dart`, `app_lock_lifecycle_layer.dart`) are specified in **03-app-lock.md**. The bottom bar, Shifa AI FAB and splash are specified in **02-navigation-and-shell.md**.

All sizes are Flutter logical pixels, which map 1:1 to Compose `dp`/`sp`. Colors are ARGB hex.

---

## 1. Sources of truth

| File | Lines | Role |
|---|---|---|
| `lib/core/theme/app_design_system.dart` | 73 | Tokens: colors, shadow, text styles, spacing, scroll physics |
| `lib/core/theme/app_theme.dart` | 99 | Material 3 `ThemeData` (`AppTheme.lightTheme`). This is the only theme. There is no dark theme |
| `lib/core/layout/responsive_layout.dart` | 35 | Breakpoints and centred content width |
| `lib/core/widgets/*.dart` | — | Reusable UI kit (section 6) |
| `lib/core/constants/assets.dart` | 7 | Asset path constants |

There are two parallel styling systems:
1. **`AppDesignSystem` tokens.** Newer; used by the UI kit and most screens.
2. **The legacy hardcoded teal `Color(0xFF17C3B2)`.** It still appears **50 times across 19 files** under `lib/` (for example `app_lock_screen.dart:164,195`, `app_lock_wrapper.dart:73`, `language_mini_toggle.dart:16`). In the KMP build, replace every occurrence with `primary #00BBB0` unless product explicitly wants the older shade (**UNVERIFIED** whether it is intentional).

---

## 2. Color tokens (`app_design_system.dart:26-43`)

| Token | Hex | Meaning / use |
|---|---|---|
| `primary` | `#00BBB0` | Brand teal: headers, primary buttons, selected segment, icons |
| `primaryLight` | `#59C2BC` | Primary button pressed/hover background |
| `secondaryLight` | `#CCF1EF` | Secondary (outline) button pressed/hover background |
| `primaryDark` | `#129B8A` | `ColorScheme.secondary` |
| `primaryAi` | `#26BAA4` | Shifa AI accents (FAB; see 02) |
| `destructiveRed` | `#DC2F2F` | Destructive button background/outline |
| `destructiveLight` | `#EB5454` | Destructive primary pressed/hover |
| `destructiveSecondaryLight` | `#F5C1C1` | Destructive secondary pressed/hover |
| `disabledGrey` | `#C6C6C6` | Disabled button background/outline |
| `white` | `#FFFFFF` | |
| `textPrimary` | `#1A1A1A` | Default text |
| `textSecondary` | `#6B7280` | Captions and secondary text |
| `textTertiary` | `#9CA3AF` | Hints, chevrons, cancelled text, empty-state icons |
| `border` | `#E5E7EB` | Card border (1 px) |
| `background` | `#FFFFFF` | Card default and scaffold |
| `backgroundSecondary` | `#F9FAFB` | Light section backgrounds |
| `backgroundTertiary` | `#F3F4F6` | Segmented-control track, cancelled card background |

**Shadow `cardShadow`** (`:45`): a single `BoxShadow(color: black @ 4% alpha (0x0A000000), blurRadius: 8, offset: (0, 2))`.
- Compose: `Modifier.shadow(elevation ≈ 2.dp, shape, ambientColor/spotColor = Black.copy(alpha = .04f))`, or a custom `drawBehind` blur for a closer match.

**Material/other literal colors used by UI-kit widgets** (not tokens, hardcoded):

| Color | Where |
|---|---|
| Material `Colors.blue` `#2196F3` | Appointment status confirmed |
| `Colors.orange` `#FF9800`, `orange.shade800` `#EF6C00` | Status pending; payment-pending badge |
| `Colors.red` `#F44336` | Status cancelled |
| `Colors.green` `#4CAF50` | Status completed |
| `Colors.amber.shade700` `#FFA000` / `shade800` `#FF8F00` | Rating star / rating text |
| `Colors.redAccent` `#FF5252` | Unread badges (notification, chat) |
| `#E0E0E0` / `#607D8B` | Avatar fallback background / initials |
| `Colors.grey[50]` `#FAFAFA`, `grey[300]` `#E0E0E0` | Input fill / input border |
| `Colors.grey.shade600` `#757575` | Version footer |
| `Colors.black12` `#1F000000`, `Colors.black87` `#DD000000`, `0x14000000` | Language toggle |

---

## 3. Typography (`app_design_system.dart:49-59`)

**No custom font.** `pubspec.yaml` has no `fonts:` section and the theme sets no `fontFamily`, so the platform default is used (Roboto on Android, San Francisco on iOS). The KMP build should use `FontFamily.Default`.

| Token | Size | Weight | Line height (multiplier) | Color |
|---|---|---|---|---|
| `h1` | 24 | w600 | 1.25 (= 30) | textPrimary |
| `h2` | 18 | w600 | 1.3 (≈ 23.4) | textPrimary |
| `body1` | 16 | w400 | 1.4 (= 22.4) | textPrimary |
| `body2` | 14 | w400 | 1.4 (= 19.6) | textPrimary |
| `caption` | 12 | w400 | 1.35 (= 16.2) | textSecondary |

Other recurring literal styles:
- Button label: 16 bold (w700). Action-button label: 12 bold.
- AppBar title: 20 bold white.
- Rating: 11 w600.
- Badge counter: 10 bold white.

**Compose mapping (recommended)**

```kotlin
val ShifaTypography = Typography(
  headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),   // h1
  titleMedium   = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.4.sp), // h2
  bodyLarge     = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal,   lineHeight = 22.4.sp), // body1
  bodyMedium    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal,   lineHeight = 19.6.sp), // body2
  bodySmall     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal,   lineHeight = 16.2.sp), // caption (color textSecondary applied at call site)
  labelLarge    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),                           // buttons
  titleLarge    = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),                           // top app bar
)
```

Also expose the tokens by their original names (`ShifaType.h1` … `caption`) so screen specs from other docs can be ported literally. Set `LineHeightStyle(Alignment.Center, Trim.None)` and `includeFontPadding = false` on Android so that Flutter's `height` multiplier behaviour matches.

---

## 4. Spacing, shapes, layout constants (`app_design_system.dart:61-72`)

| Token | Value | Use |
|---|---|---|
| `screenPaddingH` | 16 | Horizontal page gutter |
| `sectionTitleSpacing` | 8 | Below a section title |
| `sectionToSectionSpacing` | 24 | Between sections |
| `cardSpacing` | 12 | Between cards in lists |
| `cardRadius` | 16 | BaseCard corner |
| `cardPadding` | 16 | BaseCard inner padding |
| `headerHeight` | 140 | AppHeader height (excluding status bar) |
| `headerRadius` | 24 | AppHeader bottom corners |
| `headerPadding` | 16 | AppHeader padding |
| `bottomNavHeight` | 93 | Height reserved for the floating bottom bar and FAB (see 02) |
| `fabSafeBottom` | 93 | **Unused** anywhere |

Helpers:
- `safeBottomWithNavBar(context)` = `93 + MediaQuery.viewPadding.bottom`. Use it as the bottom content padding of scrollables inside the tab shell so the last item clears the bar.
- `listScrollPhysics` (`:7`) = `AlwaysScrollableScrollPhysics(parent: BouncingScrollPhysics())` on **all** platforms. Its comment claims it clamps on Android, which is wrong: it bounces on Android too. It is always scrollable so pull-to-refresh works on short lists.
- `horizontalScrollPhysics` = `BouncingScrollPhysics()`.

Radii in use:

| Radius | Where |
|---|---|
| 4 | Category chip |
| 6 | Header logo |
| 8 | Buttons; FAB theme |
| 10 | Segment pill; payment and unread badges |
| 12 | Inputs; Material Card theme; icon tiles; segmented track |
| 14 | Lock-screen logo |
| 16 | BaseCard |
| 20 | Splash logo |
| 24 | Header bottom corners |
| 999 | Language pill |

Compose `Shapes(extraSmall = 4.dp, small = 8.dp, medium = 12.dp, large = 16.dp, extraLarge = 24.dp)`.

### Responsive layout (`lib/core/layout/responsive_layout.dart`)

- `contentMaxWidth = 840`, `wideBreakpoint = 720`.
- `isWide(context)` = screen width ≥ 720.
- `horizontalInset(context)`: 16 if width ≤ 840, else `max(16, (width − 840) / 2)`. This centres content in an 840-wide column.
- `symmetricPagePadding(context)` = horizontal `horizontalInset`.
- Used only in the doctors feature: `doctors_screen`, `doctor_filters_sheet`, `doctor_filter_chips`. Every other screen uses a fixed 16 px gutter.
- Compose: `BoxWithConstraints { val inset = if (maxWidth <= 840.dp) 16.dp else max(16.dp, (maxWidth - 840.dp) / 2) }`.

---

## 5. Material theme (`lib/core/theme/app_theme.dart`)

`useMaterial3: true`, `primaryColor: #00BBB0`, `scaffoldBackgroundColor: white`.
- `colorScheme: ColorScheme.fromSeed(seedColor: #00BBB0, primary: #00BBB0, secondary: #129B8A)`. Every other role is generated from the seed by the M3 tonal algorithm. Exact generated tones are **UNVERIFIED**; they surface in dialogs, switches, progress indicators, date pickers and similar.

| Component | Overrides (line) |
|---|---|
| AppBar | `:16-26` background primary; foreground white; elevation 0; centerTitle true; title white 20 bold; icons white |
| Card | `:27` elevation 2; radius **12**; white |
| ElevatedButton | `:35` background primary; foreground white; disabled background `#C6C6C6` and foreground white; padding all 10; radius 8; elevation 0; text 16 bold |
| OutlinedButton | `:50` foreground primary; side 2 px primary; padding 10; radius 8; text 16 bold |
| TextButton | `:62` foreground primary; text 16 bold |
| InputDecoration | `:68-84` filled `#FAFAFA`; border and enabledBorder radius 12, `#E0E0E0` 1 px; focusedBorder primary 2 px; contentPadding h16/v16 |
| BottomNavigationBar | `:85` white; selected primary; unselected grey; fixed; elevation 8. **Unused** (custom bar, see 02) |
| FloatingActionButton | `:92` background primary; foreground white; radius 8 |

**Compose mapping**

```kotlin
val ShifaLightColors = lightColorScheme(
  primary = Color(0xFF00BBB0), onPrimary = Color.White,
  secondary = Color(0xFF129B8A), onSecondary = Color.White,
  error = Color(0xFFDC2F2F), onError = Color.White,
  background = Color.White, onBackground = Color(0xFF1A1A1A),
  surface = Color.White, onSurface = Color(0xFF1A1A1A),
  surfaceVariant = Color(0xFFF3F4F6), onSurfaceVariant = Color(0xFF6B7280),
  outline = Color(0xFFE5E7EB),
)
// Tokens without an M3 role:
@Immutable data class ShifaColors(
  val primaryLight: Color = Color(0xFF59C2BC), val secondaryLight: Color = Color(0xFFCCF1EF),
  val primaryAi: Color = Color(0xFF26BAA4), val destructiveLight: Color = Color(0xFFEB5454),
  val destructiveSecondaryLight: Color = Color(0xFFF5C1C1), val disabledGrey: Color = Color(0xFFC6C6C6),
  val textTertiary: Color = Color(0xFF9CA3AF), val backgroundSecondary: Color = Color(0xFFF9FAFB),
  val inputFill: Color = Color(0xFFFAFAFA), val inputBorder: Color = Color(0xFFE0E0E0),
  val badgeRed: Color = Color(0xFFFF5252),
)
val LocalShifaColors = staticCompositionLocalOf { ShifaColors() }
```

To match the Flutter look exactly, use `lightColorScheme` with explicit roles. Do not use a dynamic or seed-generated scheme on Android 12+, because dynamic color would override the brand.
- `TopAppBar`: `centerAlignedTopAppBarColors(containerColor = primary, titleContentColor = White, navigationIconContentColor = White, actionIconContentColor = White)`.
- `OutlinedTextField`: `colors(unfocusedContainerColor = #FAFAFA, focusedContainerColor = #FAFAFA, unfocusedBorderColor = #E0E0E0, focusedBorderColor = primary)` with `shape = RoundedCornerShape(12.dp)`. Compose's focused border is 2 dp by default, which matches.

---

## 6. UI kit — per-widget spec

In the "Used in" column, "screen" means `lib/features/**/presentation/*`.

### 6.1 Buttons — `lib/core/widgets/shifa_button.dart` (372 lines)

`enum ButtonWidth { hug, fill }` (`:5`).
- `hug` wraps the content.
- `fill` means `width = infinity` (the button wraps itself in `SizedBox(width: double.infinity)`) and `minimumSize (∞, 40)`.

#### ShifaPrimaryButton (`:19`)

Params: `label: String` (required), `onPressed: VoidCallback?`, `icon: IconData?`, `destructive = false`, `width = ButtonWidth.fill`, `isLoading = false`.

Named constructor `ShifaPrimaryButton.icon(icon, onPressed, destructive)` (`:30`) produces an icon-only button with width hug.

State and visuals:
- `enabled = onPressed != null && !isLoading`.
- Background:
  - disabled: `#C6C6C6`
  - pressed or hovered: `destructiveLight #EB5454` or `primaryLight #59C2BC`
  - otherwise: `destructiveRed` or `primary`
- Foreground is always white.
- Padding all 10, radius 8. Elevation is 2 on hover, otherwise 0.
- `tapTargetSize: shrinkWrap`, so there is no 48 px min touch target.
- Minimum size:
  - icon-only: 40×40
  - fill: (∞, 40)
  - hug: 0
- Content:
  - `[Icon 16] + 8 px gap + Text(label, 16 bold)`. The Row uses `mainAxisSize.min` for hug and `.max` for fill, and is centred.
  - **Loading** replaces the content with a `CircularProgressIndicator` sized 16×16, strokeWidth 2, white. The button is disabled while loading, but the background stays **disabled grey**.
  - Icon-only renders as `IconButton` with the same style, icon 16.

Used in about 40 screens (login, OTP, create account, booking flow, payments, profile, documents, tasks and more).

Compose: `Button(colors = …, contentPadding = PaddingValues(10.dp), shape = RoundedCornerShape(8.dp), elevation = null)`. Use `MutableInteractionSource.collectIsPressedAsState()` or `collectIsHoveredAsState()` for the pressed and hover background, and `Modifier.defaultMinSize(minHeight = 40.dp)`. Disable the minimum interactive component size with `LocalMinimumInteractiveComponentSize provides Dp.Unspecified` to match shrinkWrap, or keep 48 dp for accessibility. The latter is recommended; it is a deliberate deviation.

#### ShifaSecondaryButton (`:128`)

The params are the same as the primary button's, plus a `.icon` constructor. **The default width is `hug`** (`:115`), whereas the primary defaults to fill.
- Background:
  - normal: white
  - pressed or hovered: `secondaryLight #CCF1EF` or `destructiveSecondaryLight #F5C1C1`
  - disabled: white
- Foreground and 2 px border: `primary` or `destructiveRed`. When disabled, both are `#C6C6C6`.
- Other metrics match the primary button: padding 10, radius 8, min 40 / (∞, 40), icon 16, gap 8, label 16 bold. The spinner uses the accent colour.

Used in `sign_appointment`, `appointment_details`, `profile`, `edit_profile`, `doctor_filters_sheet`, `doctor_discovery_card` and `notifications` screens.

Compose: `OutlinedButton(border = BorderStroke(2.dp, accent))`.

#### ShifaActionButton (`:234`) — **unused in the codebase**

A compact variant with a `secondary` flag (false = filled, true = outlined).
- Padding 8, radius 8. Icon-only minimum is 32×32, otherwise 0.
- Icon 12, gap 4, label 12 bold, spinner 12. The default width is hug.

Port it only if later screens need it.

### 6.2 AppHeader — `lib/core/widgets/app_header.dart` (141 lines, class `:9`)

This is the teal rounded hero header at the top of the four persistent tabs (Home, Bookings, Documents, Doctors).

| Param | Type / default |
|---|---|
| `title` | String, required |
| `subtitle` | String? |
| `showProfile` | bool = true |
| `showNotification` | bool = true |
| `showBack` | bool = false |
| `onLogoTap`, `onProfileTap`, `onNotificationTap`, `onChatTap`, `onBackTap` | VoidCallback? |
| `profilePhotoUrl` | String? |

Layout:
- A container of height `140 + statusBarTop`, colour `primary`, with bottom-left and bottom-right radius 24.
- Inside it: `SafeArea(bottom: false)` then padding `LTRB(16, 8, 16, 16)` then `Column(crossAxis start, mainAxis spaceBetween)`.

1. **Top row** (spaceBetween):
   - **Left**:
     - If `showBack`: IconButton `arrow_back_ios_new` (white, 22, zero padding, min 40×40). It calls `onBackTap`, otherwise `Navigator.maybePop`.
     - Else: the logo, `assets/branding/shifa_logo.png` at 28×28, clipped with radius 6, wrapped in a GestureDetector(`onLogoTap`).
   - **Right** (Row, min):
     - `NotificationIconButton` (white), only if `showNotification`.
     - A 4 px gap. It is always present, even when the notification button is hidden.
     - `ChatIconButton` (white). It is **always shown**; there is no flag.
     - If `showProfile`: a 4 px gap, then an IconButton containing a `CircleAvatar(radius 16)`.
       - With `profilePhotoUrl`: a `NetworkImage` background.
       - Otherwise: background white @20% with `Icons.person_outline` white 18.
       - Calls `onProfileTap`.
2. **Bottom**:
   - `title` in h1 style, coloured white.
   - If `subtitle` is set: a 2 px gap, then the subtitle in white @90%, 14, normal.

Compose: a `Box(Modifier.fillMaxWidth().background(primary, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).windowInsetsPadding(WindowInsets.statusBars).height(140.dp).padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp))`. Load the avatar with Coil `AsyncImage`.

### 6.3 BaseCard — `lib/core/widgets/base_card.dart` (45 lines, `:5`)

Params: `child` (required), `onTap: VoidCallback?`, `padding = EdgeInsets.all(16)`, `color = background (white)`, `elevated = true`.

Visuals:
- A container with radius 16 and a 1 px `#E5E7EB` border.
- The `cardShadow` is applied only if `elevated`.
- Inside it: a transparent `Material`, then an `InkWell(borderRadius 16, onTap)` that shows a ripple only when `onTap` is non-null, then the padding, then the child.

This is the base for AppointmentCard, DoctorCard, DocumentCard and RemoteCareTaskCard, and is also used directly by many screens.

Compose: `Surface(shape = RoundedCornerShape(16.dp), color, border = BorderStroke(1.dp, outline), shadowElevation = if (elevated) 2.dp else 0.dp, onClick = …)`. Use the non-clickable `Surface` overload when `onTap == null`.

### 6.4 AppointmentCard — `lib/core/widgets/appointment_card.dart` (201 lines, `:14`)

Params:
- `appointment` is **typed `dynamic`** (duck-typed). The fields it reads are:
  - `status` (enum or String)
  - `paymentStatus`
  - `doctor` (`name`, `photoUrl`, `profession`, `clinic`, `rating`, `reviewCount`)
  - `appointmentDate` / date-time
  - `reason`
- `onTap: VoidCallback?`
- `onPayNow: VoidCallback?`

Model mapping in KMP: pass the `Appointment` model from the bookings spec.

Logic:
- `status`: if `appointment.status` is already an `AppointmentStatus`, it is used as-is. Otherwise `AppointmentStatus.fromString(status ?? 'PENDING')` is used, and any exception falls back to `pending` (`:55-62`).
- `isCancelled = status == cancelled` (`:63`). When cancelled:
  - tap is disabled (`onTap: null`);
  - the card background is `backgroundTertiary #F3F4F6`;
  - name, time and secondary texts all become `textTertiary`.
- `paymentPending = paymentStatus.toString().toUpperCase() == 'PENDING'` (`:64`).
- Date formats:
  - `DateFormat('dd MMM')` and `DateFormat('HH:mm')` (`:10-11`). **No locale is passed**, so month abbreviations follow the Intl default locale (**UNVERIFIED**; they are likely English unless `Intl.defaultLocale` is set).
  - The rendered string is `"HH:mm, dd MMM"`, for example `14:30, 05 Aug`.

Layout (`BaseCard` with a Row, crossAxis start):
- **Left column**:
  - `UserAvatar(radius 24)`.
  - If `rating != null && reviewCount > 0`: a 4 px gap, then a Row of `Icons.star` 12 amber.shade700, a 2 px gap, `rating.toStringAsFixed(1)` in 11 w600 amber.shade800, and `" (n)"` in caption.
- A 12 px gap.
- **Expanded column**:
  1. A Row:
     - Doctor name in body2 w600, 1 line, ellipsis. The fallback is l10n `unknownDoctor`, then `'Unknown'`.
     - A **status badge**: padding h6/v2, background `statusColor @12%`, radius 8, containing only an icon (size 16, statusColor).

       | Status | Color | Icon |
       |---|---|---|
       | confirmed | blue | `check_circle` |
       | pending | orange | `schedule` |
       | cancelled | red | `cancel` |
       | completed | green | `check_circle_outline` |

  2. If `paymentPending`: a 6 px gap, then a Row of:
     - the badge `paymentPendingBadge` (padding h8/v3, orange @12%, radius 10, caption orange.shade800 w700);
     - an 8 px gap;
     - a `TextButton(payNow)` that calls `onPayNow`.
  3. `translateProfession(doctor.profession)`, plus `" · {clinic}"` if the clinic is non-empty. Caption style, 1 line.
  4. A 6 px gap, then a Row of `Icons.access_time` 14 textSecondary, a 4 px gap, and `"HH:mm, dd MMM"` in caption colour textPrimary.
  5. If `reason` is non-empty: top padding 2, then the reason in caption at size 11, 1 line, ellipsis.

Used in the Home (upcoming) and Bookings lists.

### 6.5 DoctorCard — `lib/core/widgets/doctor_card.dart` (85 lines, `:9`)

Params: `doctor: DoctorModel` (required), `onTap`, `distanceText: String?`.

A `BaseCard` containing a Row:
- **Left column**: the same avatar (r24) and rating block as AppointmentCard (`:36-47`).
- A 12 px gap.
- **Expanded column**:
  - name in body2 w600, 1 line;
  - `translateProfession(profession)` plus `" · clinic"` in caption, 1 line;
  - if `distanceText` is set: top padding 2, then the distance in caption textTertiary.
- `Icons.chevron_right` in textTertiary.

Used in Home (nearby/recent doctors) and in Doctors lists. The richer discovery card lives in the doctors feature (`doctor_discovery_card`); see the doctors spec.

### 6.6 DocumentCard — `lib/core/widgets/document_card.dart` (141 lines, `:10`)

Params: `document: DocumentModel`, `formattedDate: String`, `uploaderLabel: String` (all three required), `onView`, `onDelete` (both optional).

- **Icon** (`:29-31`), with `t = type.toLowerCase()` and `url = fileUrl.toLowerCase()`:
  - `picture_as_pdf` if `t` contains "pdf" or `url` ends with ".pdf";
  - `image` if `t` contains "image", or `url` contains "image" or ends with .jpg/.jpeg/.png;
  - `description` otherwise.
- **Title**: `document.title`. If it is empty: l10n `document`, then `'Document'`.
- **Tap**: calls `onView` only if `fileUrl` is non-empty (`:43`).

Layout (BaseCard with a Row):
- An icon tile 48×48 (primary @10%, radius 12) with the icon at 26 in primary.
- A 12 px gap.
- **Expanded column**:
  - the title in body2 w600, max 2 lines;
  - a 4 px gap;
  - a `Wrap(spacing 6, runSpacing ~4)` containing:
    - `formattedDate` in caption;
    - if `findPatientCategory(document.category)` is non-null (`lib/features/documents/domain/document_category.dart:57`), a **category chip**: padding h6/v2, primary @12%, radius 4, `[category.icon 10 primary] + 4 px gap + label` in caption primary at size 10.
  - `uploaderLabel`: top padding 2, caption textTertiary, 1 line.
- An 8 px gap.
- If `onDelete` is set: an IconButton `delete_outline` (tooltip l10n `delete`, textTertiary, 22).
- A `TextButton(view)` in primary with padding h12/v8, calling `onView`.

Category label (`document_category.dart:22`): l10n key `documentCategory_<CODE>`. If it is missing, the code is humanised: `BLOOD_TEST` becomes "Blood Test".

| Code | Icon |
|---|---|
| BLOOD_TEST | bloodtype |
| URINE_TEST | science |
| STOOL_TEST | science |
| LAB_RESULT | biotech |
| MRI | medical_services |
| CT_SCAN | medical_services |
| XRAY | medical_information |
| ULTRASOUND | monitor_heart |
| MAMMOGRAPHY | medical_services |
| ECG | monitor_heart |
| EEG | psychology |
| ENDOSCOPY | medical_information |
| BIOPSY | biotech |
| PATHOLOGY | biotech |
| IMAGING_OTHER | image |
| PRESCRIPTION | medication |
| VACCINATION_RECORD | vaccines |
| DISCHARGE_SUMMARY | local_hospital |
| REFERRAL | assignment |
| HOSPITAL_REPORT | local_hospital |
| ALLERGY_REPORT | warning_amber |
| OTHER_MEDICAL | folder_shared |

(`document_category.dart:33-54`)

Used in the Documents screen.

### 6.7 UserAvatar — `lib/core/widgets/user_avatar.dart` (74 lines, `:4`)

Params: `name: String?` (required, nullable), `photoUrl: String?` (required, nullable), `radius = 20`.

- **Initials** (`:18`): `name.trim().split(/\s+/)`.
  - One part: its first character.
  - Two or more parts: the first character of the first part plus the first character of the last part.
  - The result is uppercased. An empty name gives no initials, so the person icon is shown.
- **URL**: `normalizePhotoUrl(photoUrl)` (`lib/core/utils/image_utils.dart:4`):
  - null or blank gives null;
  - an absolute URL on Android whose host is `localhost` or `127.0.0.1` has its scheme, host and port rewritten to `ApiClient.publicBaseUrl`;
  - a relative path becomes `"$publicBaseUrl/$path"`.
- **Render**:
  - `SizedBox(2r × 2r)`, then `ClipOval`, then `Image.network(fit: cover, cacheWidth/Height = 2r)`.
  - The loading and error builders both show the fallback.
  - **Fallback**: a circle in `#E0E0E0`. It shows the initials (fontSize `r × 0.75`, w600, `#607D8B`), or else `Icons.person` (size r, `#607D8B`).
- It uses `Image.network`, not CachedNetworkImage, so there is no disk cache.

Used in AppointmentCard, DoctorCard and doctor, booking, chat and profile screens.

Compose: Coil 3 `SubcomposeAsyncImage` with `loading`/`error` slots set to the fallback composable and `Modifier.size(radius*2).clip(CircleShape)`.

### 6.8 SegmentedControl — `lib/core/widgets/segmented_control.dart` (65 lines, `:5`)

Params: `selectedIndex: int`, `labels: List<String>`, `onSelected: ValueChanged<int>`.

- **Track**: height 44, `backgroundTertiary #F3F4F6`, radius 12, padding 4.
- **Items**: a Row of equal `Expanded` items. Each is an `InkWell(radius 10)` wrapping an `AnimatedContainer(200 ms, radius 10, color = selected ? primary : transparent)`, then a centred label.
- **Label**: body2 w600, 1 line, ellipsis. White when selected, textSecondary otherwise.
- The colour animates over 200 ms. The pill does not slide.

Used in the Bookings tabs (upcoming/past) and the Doctors tabs.

Compose: a `Row` in a `Box`, with `animateColorAsState(tween(200))` for each item.

### 6.9 EmptyState — `lib/core/widgets/empty_state.dart` (50 lines, `:5`)

Params: `icon = Icons.inbox_outlined`, `message: String` (required), `action: Widget?`.

Layout:
- Padding v32/h16, then a `Center` containing a `Column(mainAxisSize.min)`.
- The icon at 56 in textTertiary, then a 16 px gap.
- The message in body2 textSecondary, centred.
- If `action` is set: a 20 px gap, then the action (typically a ShifaPrimaryButton or ShifaSecondaryButton).

Used in Home, Bookings, Doctors and Documents.

### 6.10 SectionTitle — `lib/core/widgets/section_title.dart` (31 lines, `:5`)

Params: `title: String`, `trailing: Widget?`.

- Bottom padding 8 (`sectionTitleSpacing`).
- Without `trailing`: `Text(title, h2)`.
- With `trailing`: a `Row(spaceBetween)` of `[Flexible(Text h2), trailing]`, where `trailing` is typically a "see all" TextButton.

Used in Home.

### 6.11 NotificationIconButton / ChatIconButton

Files: `lib/core/widgets/notification_icon_button.dart` (`:5`) and `chat_icon_button.dart` (`:5`), each 53 lines.

Params: `onPressed: VoidCallback?`, `iconColor = Colors.white`.

- **Icon**: `notifications_outlined` / `chat_bubble_outline`.
- **Count source**:
  - notification: `unreadNotificationsCountProvider` (see the notifications spec);
  - chat: `unreadCountProvider`, a StreamProvider that **polls every 10 s** (see the chat spec).
  - While loading or on error, the count is treated as 0.
- **Layout**: a `Stack(clipBehavior: none)` containing an IconButton. If `count > 0`, a badge is added at `Positioned(right: 6, top: 6)`:
  - `redAccent #FF5252`, padding h5/v2, radius 10, `minWidth 16`;
  - text white, 10 bold, centred;
  - `count > 9` displays **"9+"**.

Used only inside AppHeader. Other screens build their own badges where needed.

### 6.12 PhoneInputField — `lib/core/widgets/phone_input_field.dart` (205 lines)

This is a phone number field with a country-code picker. It emits an E.164-style string.

| Param | Default |
|---|---|
| `initialFullPhone: String?` | — (e.g. `+998901234567`) |
| `labelText: String?` | — |
| `hintText: String?` | `'901234567'` |
| `validator: String? Function(String?)?` | — |
| `onChanged: void Function(String fullPhone)?` | — |
| `enabled` | true |
| `autofocus` | false |

Public state `PhoneInputFieldState` (`:31`), reached via a `GlobalKey`, exposes `fullPhone` (getter) = `dialCode + digitsOnly(nationalText)`. The dial code defaults to `+998`.

- **Parsing** (`parseFullPhone`, static):
  - null or empty gives `('+998', '')`.
  - Otherwise the input is stripped to digits. For `len` in `[4, 3, 2, 1]`, the first `len` digits are taken as the dial code if they start with 1–9.
  - **Quirk**: because 4 is tried first, `+998901234567` parses as dial `+9989` and number `01234567`. The `+9989` lookup misses the map and falls back to `UZ`, so the picker shows 🇺🇿 +998, but the text field shows **`01234567`** (the leading `9` is missing). `fullPhone` still concatenates back to `+998901234567` while `_countryCode.dialCode` stays `+9989`. Once the user re-selects a country in the picker, the dial code becomes `+998` and the result is the broken `+99801234567`. In KMP, match the longest *known* dial code instead (**UNVERIFIED** at runtime; derived from reading the code).
- **Country map**: `_dialCodeToCountryCode` maps about 200 dial codes to ISO codes and defaults to `UZ`.
- **Layout**: `Row(crossAxis start)` of:
  - `CountryCodePicker` (package `country_code_picker`): showFlag, `initialSelection = iso`, padding 0, dialog 90% screen width × 70% height, showing dial code and flag;
  - an 8 px gap;
  - `Expanded(TextFormField(keyboardType: phone, prefixIcon: Icons.phone, label/hint))`, which uses the global InputDecoration theme.
- **Validator wrapper**: it passes `null` when the national part is empty, otherwise the full phone.
- **onChanged** fires on every text change and on every country change.
- `didUpdateWidget` re-parses when `initialFullPhone` changes.
- **Top-level helper** `normalizePhoneForSms(String?)` (`:200`):
  - returns `''` if the input is empty or has no digits;
  - returns the trimmed input if it starts with `+`;
  - otherwise returns `'+' + digits`.

Used in `create_account` and `edit_profile`. `normalizePhoneForSms` is also used in `auth_repository`, `login` and `forgot_password`.

Compose: an `ExposedDropdownMenuBox` or a bottom-sheet country list, with a `libphonenumber` port for parsing (for example `io.michaelrocks:libphonenumber-android` on Android and `PhoneNumberKit` on iOS, bridged through `expect`/`actual`), or keep the table.

### 6.13 RemoteCareTaskCard — `lib/core/widgets/remote_care_task_card.dart` (73 lines, `:7`)

This is a highlighted (teal-tinted) card for a remote-care task.

Params: `title`, `subtitle` (both required), `trailingLabel` (default l10n `view`, then `'View'`), `onTap`.

A `BaseCard(color: primary @8%, elevated)` containing a Row of:
- an icon tile (padding 10, primary, radius 12) holding `Icons.task_alt` white 22;
- a 12 px gap;
- an Expanded column: title in body2 w600, max 2 lines; a 2 px gap; subtitle in caption textSecondary, max 2 lines;
- the trailing text in caption w600 primary;
- a 4 px gap;
- `chevron_right` 20 textTertiary.

Used in Home (the tasks section; see the tasks spec).

### 6.14 AppVersionFooter — `lib/core/widgets/app_version_footer.dart` (63 lines, `:6`)

Param: `padding = EdgeInsets.only(top: 24, bottom: 8)`.

- It loads `PackageInfo.fromPlatform()`.
  - Label: `"{AppConfig.appDisplayName} {version} ({AppConfig.releaseDateLabel})"`, for example `Shifa Bemor 1.1.7 (5 Aug 2026)`. `appDisplayName = 'Shifa Bemor'`; `releaseDateLabel` is a hardcoded string in AppConfig (see the core/config spec).
  - If the version is blank it shows `—`. On exception the label is `"{name} ({date})"`.
- While loading, a 14 px-high empty box is shown to avoid layout jump.
- Style: `textTheme.bodySmall` with colour grey.shade600 `#757575`, size 12, centred.

Used in the Login and Profile screens. KMP: `BuildKonfig`/`expect fun appVersion()`.

### 6.15 LanguageMiniToggle — `lib/core/widgets/language_mini_toggle.dart` (89 lines, `:7`)

This is a small pill dropdown that switches the app language immediately. It has no params.

- **Pill**:
  - white, radius 999, 1 px `black12` border;
  - shadow `0x14000000` (black 8%), blur 8, offset (0, 2);
  - padding h10/v6.
- **Content**: a Row of:
  - the current `languageProvider` code, uppercased (`EN`, `DE`, `UZ`, `RU`), at 12 w700 black87 with letterSpacing 0.3;
  - a 6 px gap;
  - `keyboard_arrow_down` 18 in legacy teal `#17C3B2` (`:16`).
- **Tap**:
  - It calls `showMenu` anchored directly under the pill: `RelativeRect(x, y + h, x + w, y + h + 200)`.
  - The menu items are `AppLanguage.values` with their uppercased codes, in enum order (see the localization spec).
  - Selecting one calls `languageProvider.notifier.setLanguage(lang)`, which persists `app_language` in SharedPreferences.

Used on the Login screen (top-right). Compose: `Box { Surface(pill) ; DropdownMenu(expanded) }`.

### 6.16 App-lock widgets

`AppLockScreen`, `AppLockWrapper` and `AppLockLifecycleLayer` are covered in **03-app-lock.md**. Visual summary:
- white background;
- logo 64 with radius 14;
- PIN dots filled in `#17C3B2`;
- error text red;
- a numeric keypad.

---

## 7. Assets and fonts

`pubspec.yaml` bundles `assets/images/` and `assets/branding/`, with `uses-material-design: true` (Material Icons font). There are **no custom fonts**.

| File | Details | Used by |
|---|---|---|
| `assets/branding/shifa_logo.png` | 12 697 B raster logo | AppHeader (28, r6); AppLockScreen (64, r14); `login_screen.dart:142`; Splash (96, r20); launcher icon via `flutter_launcher_icons` (Android minSdk 21) |
| `assets/branding/shifa_logo.svg` | 32×23 viewBox, white path | `Assets.shifaLogo`. **Unused** |
| `assets/branding/Shifa AI Icon.svg` | 46×32 viewBox, fill `#272727` (filename contains spaces) | `Assets.shifaAiIcon`, used in the centre FAB and tinted white via `ColorFilter` (see 02) |
| `assets/images/default_avatar.svg` | `#E0E0E0` circle, `#90A4AE` figure | **Unused** (UserAvatar draws its own fallback) |
| `assets/l10n/en.json`, `ru.json`, `uz.json` | Legacy string files | **Not bundled** (not listed in pubspec) and unused. Strings live in `app_localizations.dart` maps |
| `assets/.DS_Store` | macOS junk | — |

Asset constants (`lib/core/constants/assets.dart`): `shifaLogo` (svg), `shifaLogoPng`, `shifaAiIcon`.

Icons are Material Icons, both outlined and filled, as named above. In CMP, use `material-icons-extended`; for example `Icons.Outlined.Notifications`, `Icons.Outlined.ChatBubbleOutline`, `Icons.AutoMirrored.Filled.ArrowBackIos` (Flutter's `arrow_back_ios_new`) and `Icons.Filled.TaskAlt`.

---

## 8. Localization keys used by this area

| Key | Where |
|---|---|
| `unknownDoctor` | AppointmentCard name fallback |
| `paymentPendingBadge` | AppointmentCard payment badge |
| `payNow` | AppointmentCard button |
| `document` | DocumentCard title fallback |
| `delete` | DocumentCard delete tooltip |
| `view` | DocumentCard button; RemoteCareTaskCard trailing default |
| `documentCategory_<CODE>` | Category chip; 22 codes listed in section 6.6 |
| Profession names | via `AppLocalizations.translateProfession(profession)` (`app_localizations.dart:3621`), which delegates to `ProfessionData.translate(profession, languageCode)`. This is table-driven, not a key lookup; see the doctors/core spec for the profession table |

Other visible strings are passed in by callers (titles, messages, labels). The language codes (`EN`/`DE`/`UZ`/`RU`) and the version footer are not localized.

---

## KMP/CMP porting notes

1. **Theme entry point**:
   ```kotlin
   @Composable fun ShifaTheme(content: @Composable () -> Unit) =
     CompositionLocalProvider(LocalShifaColors provides ShifaColors(), LocalShifaDimens provides ShifaDimens()) {
       MaterialTheme(colorScheme = ShifaLightColors, typography = ShifaTypography, shapes = ShifaShapes, content = content)
     }
   ```
   Put the design tokens (section 2–4) in a `ShifaDimens` data class (`screenPaddingH = 16.dp`, `cardRadius = 16.dp`, `headerHeight = 140.dp`, `bottomNavHeight = 93.dp`, …) and expose them as a `CompositionLocal`, so screens reference tokens and never literals.
2. **Light only.** Flutter defines no dark theme and forces light. Either set `darkTheme = false` in KMP or ship the light scheme for both.
3. **Status bar.** Headers draw teal under the status bar. Use `enableEdgeToEdge()` on Android and use light status-bar icons on header screens. On iOS, `UIStatusBarStyle.lightContent` is needed on teal headers (**UNVERIFIED** what Flutter currently sets; it defaults per AppBar and system overlay).
4. **Images.** Use Coil 3 (`io.coil-kt.coil3:coil-compose` plus `coil-network-ktor3`) for avatars and document thumbnails. Port `normalizePhotoUrl` into shared code: rewrite localhost to the API host on Android, and prefix relative paths with the public base URL.
5. **Vectors.** Convert `Shifa AI Icon.svg` (and the optional `shifa_logo.svg`) to Compose `ImageVector` or a composeResources `drawable/*.xml`, and rename the file without spaces. Keep `shifa_logo.png` as a raster resource. Regenerate launcher icons natively (Android adaptive icon, iOS AppIcon set).
6. **Buttons.** Implement `ShifaPrimaryButton`/`ShifaSecondaryButton` as a single `ShifaButton(style = Primary|Secondary, destructive, width = Hug|Fill, loading, icon)` composable with the exact state colours in section 6.1. Remember the default-width asymmetry: primary defaults to fill, secondary to hug.
7. **Legacy teal.** Normalise `#17C3B2` to `primary #00BBB0` everywhere, including the lock screen, app-lock wrapper spinner and language toggle, unless design says otherwise.
8. **Status and badge colours.** Move the Material literals (blue, orange, red, green, amber, redAccent) into `ShifaColors` (`statusConfirmed`, `statusPending`, …) rather than hardcoding them again.
9. **Phone input.** Replace the prefix-length heuristic with longest-known-dial-code matching (see the 6.12 quirk). Keep the default country UZ/+998 and E.164 output.
10. **Date formatting in cards.** Use `kotlinx-datetime` plus a locale-aware formatter. Decide explicitly whether month abbreviations follow the app language; Flutter does not pass a locale (see 6.4).
11. **Touch targets.** Flutter buttons shrink-wrap to 40 px. Compose enforces 48 dp by default. Keep 48 dp for accessibility unless pixel parity is required.
12. **Responsive.** Only the doctors feature centres content at 840 max width. Consider applying `horizontalInset` globally for tablets and desktop.

---

## Observed quirks / tech debt

1. **Two teals.** The legacy `#17C3B2` is hardcoded 50× in 19 files, while the token `primary` is `#00BBB0`. The visible shade therefore differs between screens (lock screen, language toggle and many feature screens).
2. **`listScrollPhysics` comment is wrong** (`app_design_system.dart:7`). It says "clamping on Android" but always uses Bouncing.
3. **`fabSafeBottom` (93) is unused.** `bottomNavHeight` is also 93, and stays 93 even when the FAB is hidden (see 02).
4. **`ShifaActionButton` is unused.** The `BottomNavigationBarTheme` is unused. The `Assets.shifaLogo` svg and `default_avatar.svg` are unused.
5. **Radius mismatch.** The Material `CardTheme` radius is 12 and elevation 2, but `BaseCard` uses radius 16 with a 1 px border and a custom shadow. Raw `Card` widgets in screens look different from BaseCard.
6. **`UserAvatar` uses `Image.network`** without disk caching, while the chat feature uses `CachedNetworkImage`. The two caching behaviours are inconsistent. AppHeader's avatar uses `NetworkImage` via `CircleAvatar`, which has no fallback on error (it shows an empty circle).
7. **`AppointmentCard.appointment` is `dynamic`.** It is duck-typed field access with a try/catch around status parsing; type-safety is lost.
8. **Hardcoded Material colours** for statuses, rating and badges bypass `AppDesignSystem`, and are not themable.
9. **AppHeader always shows the chat icon** (there is no `showChat` flag). A stray 4 px gap remains when `showNotification` is false.
10. **SegmentedControl** has a redundant outer `AnimatedContainer`, and the selected pill fades rather than slides.
11. **Date formats ignore the app locale.** `DateFormat('dd MMM')` is created at file level without a locale (**UNVERIFIED**; the month language depends on `Intl.defaultLocale`).
12. **`PhoneInputField.parseFullPhone`** tries 4-digit dial codes first, so a pre-filled `+998901234567` displays as `+998` / `01234567`. The number only round-trips because of string concatenation; changing the country afterwards corrupts it (see 6.12).
13. **`withOpacity`** is used throughout (deprecated in recent Flutter in favour of `withValues(alpha:)`). This has no visual impact.
14. **No custom font or dark theme.** The typography relies on the platform fonts, so iOS and Android render with different metrics.
15. **`assets/l10n/*.json`** are stale, unbundled string files, and `assets/.DS_Store` is committed. Both are junk.
16. **Unread badges** are duplicated code (notification and chat widgets are near-identical, 53 lines each).
17. **`LanguageMiniToggle` menu** positions itself manually via `RenderBox`, and its item labels are just the uppercased codes, not language names.
