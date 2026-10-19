package shifa.uz.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.manrope_bold
import shifa.composeapp.generated.resources.manrope_medium
import shifa.composeapp.generated.resources.manrope_regular
import shifa.composeapp.generated.resources.manrope_semibold

@Composable
fun manropeFontFamily() =
    FontFamily(
        Font(Res.font.manrope_regular, weight = FontWeight.Normal),
        Font(Res.font.manrope_medium, weight = FontWeight.Medium),
        Font(Res.font.manrope_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.manrope_bold, weight = FontWeight.Bold),
    )

private fun manrope(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Int = 0,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    // Figma line boxes: glyphs centered, no trim. See [lineBox] for line heights below the font's.
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

// Styles as used by the Figma components (Patient App v2 → Components).
@Composable
fun shifaTypography(): Typography {
    val family = manropeFontFamily()
    val base = Typography()
    return Typography(
        // Not used in Figma: M3 metrics, Manrope family.
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        // page-header, main tab title
        headlineLarge = manrope(family, FontWeight.Bold, 32, 40, -1),
        // page-header, other / sub page title
        headlineSmall = manrope(family, FontWeight.Bold, 24, 28, -1),
        // section header ("Upcoming Appointment", "My Tasks")
        titleMedium = manrope(family, FontWeight.Bold, 16, 22, -1),
        // card title (doctor name, task name)
        titleSmall = manrope(family, FontWeight.Bold, 14, 16),
        // input-field value / placeholder
        bodyLarge = manrope(family, FontWeight.Medium, 14, 16),
        // card body ("Dentist - Clinic", time)
        bodyMedium = manrope(family, FontWeight.Medium, 12, 12),
        // card meta ("Due today"), usually Text.Tertiary
        bodySmall = manrope(family, FontWeight.Normal, 12, 12),
        // primary / secondary / tertiary button
        labelLarge = manrope(family, FontWeight.Bold, 16, 22),
        // action-button (pill)
        labelMedium = manrope(family, FontWeight.Bold, 12, 16),
        // notification time, attachment count
        labelSmall = manrope(family, FontWeight.Bold, 10, 12),
    )
}

// Figma styles with no M3 slot.
object ShifaTextStyles {
    // input-field label, suggestive text; nav-bar tile label uses Medium (Bold when selected)
    @Composable
    fun caption() = manrope(manropeFontFamily(), FontWeight.Normal, 10, 14)

    // Figma text style "Input text"
    @Composable
    fun inputText() = manrope(manropeFontFamily(), FontWeight.SemiBold, 16, 16)

    // chat-bar and search-field input / placeholder
    @Composable
    fun chatInput() = manrope(manropeFontFamily(), FontWeight.SemiBold, 14, 16)

    // profile name (Bold "Input text"), doctor-card name
    @Composable
    fun profileName() = manrope(manropeFontFamily(), FontWeight.Bold, 16, 16)

    // tag
    @Composable
    fun tag() = manrope(manropeFontFamily(), FontWeight.Bold, 12, 12)

    // interactive-chip, Type=filter / Type=slot (Bold when active)
    @Composable
    fun chipFilter() = manrope(manropeFontFamily(), FontWeight.Medium, 12, 18)

    @Composable
    fun chipSlot() = manrope(manropeFontFamily(), FontWeight.Medium, 14, 18)

    // doctor's notes and bio
    @Composable
    fun chatMessage() = manrope(manropeFontFamily(), FontWeight.Normal, 12, 14)

    // chat-bubble message (semantic/type/input, Regular, line height 18)
    @Composable
    fun chatBubble() = manrope(manropeFontFamily(), FontWeight.Normal, 16, 18)

    // video call: waiting for the doctor (Bold 18, line height 28)
    @Composable
    fun callStatus() = manrope(manropeFontFamily(), FontWeight.Bold, 18, 28, -1)

    // notification body (Medium 12, line height 14)
    @Composable
    fun notificationBody() = manrope(manropeFontFamily(), FontWeight.Medium, 12, 14)
}

/**
 * Sizes text to its Figma line boxes (lines × line height). Compose pads a line height smaller than the
 * font's own height (e.g. 12/12) back up to it; here the glyphs overflow the box instead, as in Figma.
 */
fun Modifier.lineBox(style: TextStyle) =
    layout { measurable, constraints ->
        val text = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val line = style.lineHeight.roundToPx()
        // The padding is less than a line, so rounding down gives the line count.
        val lines = (text.height / line).coerceAtLeast(1)
        val height = (lines * line).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(text.width, height) { text.place(0, (height - text.height) / 2) }
    }
