package shifa.uz.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

// Figma: semantic/space/*. Inset = padding, gap = between siblings in a row, stack = between siblings in a column.
object ShifaSpacing {
    object Inset {
        val Xs = 8.dp
        val Sm = 12.dp
        val Md = 16.dp
        val Lg = 24.dp
    }

    object Gap {
        val Xs = 4.dp
        val Sm = 8.dp
        val Md = 12.dp
        val Lg = 24.dp
    }

    object Stack {
        val Xs = 4.dp
        val Sm = 8.dp
        val Md = 16.dp
    }
}

// Figma: shifa/size/*. Component sizes and strokes bound to these variables.
object ShifaSize {
    val S2 = 2.dp
    val S4 = 4.dp
    val S6 = 6.dp
    val S8 = 8.dp
    val S12 = 12.dp
    val S14 = 14.dp
    val S16 = 16.dp
    val S20 = 20.dp
    val S22 = 22.dp
    val S24 = 24.dp
    val S26 = 26.dp
    val S28 = 28.dp
    val S30 = 30.dp
    val S36 = 36.dp
    val S64 = 64.dp
    val S100 = 100.dp
    val S120 = 120.dp
}

// Sizes Figma sets as raw values (no variable), named <component><Part>. One section per component.
object ShifaDimens {
    // buttons
    val TertiaryButtonPaddingStart = 10.dp
    val TertiaryButtonPaddingEnd = 15.dp
    val TertiaryButtonPaddingVertical = 10.dp
    val IconButtonPadding = 10.dp

    // page-header
    val PageHeaderAvatar = 40.dp

    // cards
    val AppointmentAvatar = 42.dp
    val AppointmentActionHeight = 38.dp
    val DoctorAvatar = 70.dp
    val DoctorAvatarCompact = 44.dp
    val DoctorAvatarIn = 56.dp
    val DoctorCardInWidth = 250.dp
    val DocumentThumbWidth = 55.dp
    val DocumentThumbHeight = 48.dp
    val TaskIcon = 48.dp
    val ProgressBarGap = 10.dp
    val CheckInCardGap = 9.dp

    // interactive-chip
    val InteractiveChipBorder = 1.dp
    val InteractiveChipIconGap = 5.dp

    // dropdown
    val DropdownMenuPaddingBottom = 13.dp

    // switch
    val SwitchWidth = 40.dp
    val SwitchKnob = 15.dp

    // notification
    val NotificationAvatar = 50.dp

    // image-container
    val ImageContainerSize = 302.dp

    // chat-bubble
    val ChatBubbleMaxWidth = 360.dp

    // section
    val SectionBorder = 1.dp

    // nav-bar
    val NavBarTileWidth = 65.dp

    // splash (1614:12212 → 1614:12237). Blob centres are offsets from the screen centre.
    val SplashTile = 178.dp
    val SplashTileRadius = 37.dp
    val SplashBlobWidth = 891.563.dp
    val SplashBlobHeight = 609.879.dp
    val SplashBlobBlur = 25.dp
    val SplashBlobAStart = DpOffset((-99.61).dp, 139.6.dp)
    val SplashBlobAEnd = DpOffset((-81.16).dp, 388.84.dp)
    val SplashBlobBStart = DpOffset((-10.16).dp, (-233.16).dp)
    val SplashBlobBEnd = DpOffset(107.84.dp, (-376.16).dp)

    // login (1614:12247)
    val LoginHeroHeight = 358.dp
    val LoginHeroTile = 111.dp
    val LoginLogo = DpSize(59.dp, 43.dp)
    val LoginFooterHeight = 66.dp

    // create account (1631:10131, 1631:10296, 1631:10449)
    val RegisterAvatar = 280.dp
}

// Figma: shifa/radius/* and semantic/radius/control-*. Full (999) = CircleShape.
object ShifaRadius {
    val Sm = RoundedCornerShape(8.dp)
    val ControlMd = RoundedCornerShape(10.dp) // buttons, inputs, cards
    val ControlLg = RoundedCornerShape(16.dp) // sections, images
    val Xl = RoundedCornerShape(24.dp) // trays, pop-ups
    val Full = CircleShape // tags, action/icon buttons, avatars
}

internal val ShifaShapes =
    Shapes(
        extraSmall = ShifaRadius.Sm,
        small = ShifaRadius.ControlMd,
        medium = ShifaRadius.ControlLg,
        large = ShifaRadius.Xl,
        extraLarge = ShifaRadius.Xl,
    )

// Figma effect styles: DROP_SHADOW, spread 0. Use: Modifier.dropShadow(ShifaRadius.ControlMd, ShifaShadow.Card)
object ShifaShadow {
    val Default = Shadow(radius = 16.dp, color = Color(0x26000000)) // buttons
    val Card = Shadow(radius = 14.dp, color = Color(0x26000000))
    val Tray = Shadow(radius = 26.dp, color = Color(0x66000000)) // trays, modals
    val Dropdown = Shadow(radius = 6.dp, color = Color(0x26000000)) // raw in Figma: expanded dropdown
    val NavBar = Shadow(radius = 41.4.dp, color = Color(0x3B000000), offset = DpOffset(2.dp, 2.dp))
    val ChatTray =
        Shadow(
            radius = 30.dp,
            color = Color(0x14000000),
            offset = DpOffset(2.dp, 2.dp),
        ) // tray shifa-chat row, no style
}
