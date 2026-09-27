package shifa.uz.ui.theme

import androidx.compose.ui.graphics.Color

// Primitives (Figma: shifa/color/*)
private val Teal50 = Color(0xFF00BBB0)
private val Charcoal = Color(0xFF272727)
private val Grey600 = Color(0xFF494D4C)
private val Grey500 = Color(0xFF7D8282)
private val Grey400 = Color(0xFF979B9B)
private val Grey200 = Color(0xFFE8E8E8)
private val Grey100 = Color(0xFFF2F3F2)
private val Red = Color(0xFFDC2F2F)
private val Amber = Color(0xFFEBA400)
private val Blue = Color(0xFF1C75CF)

// Semantic tokens, named 1:1 after Figma `semantic/color/<group>/<name>`.
object ShifaColors {
    object Text {
        val Primary = Charcoal
        val Tertiary = Grey500
        val Disabled = Grey400
        val Inverse = Color.White
        val Brand = Teal50
        val Link = Teal50
        val Error = Red
        val Warning = Amber
        val Info = Blue
    }

    object Icon {
        val Primary = Charcoal
        val Secondary = Grey600
        val Disabled = Grey400
        val Inverse = Color.White
        val Brand = Teal50
        val Error = Red
        val Warning = Amber
        val Info = Blue
    }

    object Bg {
        val Canvas = Grey100
        val Surface = Color.White
        val Disabled = Grey200
        val Brand = Teal50
        val SuccessSubtle = Color(0xFFE5F8F7)
        val WarningSubtle = Color(0xFFFCF6E8)
        val ErrorSubtle = Color(0xFFF9EBEB)
        val InfoSubtle = Color(0xFFEBF2F9)
        val Overlay = Color(0x80000000)
    }

    object Border {
        val Default = Grey200
        val Disabled = Grey400
        val Brand = Teal50
        val Focus = Teal50
        val Error = Red
        val Warning = Amber
        val Success = Teal50
    }

    object Interactive {
        val Primary = Teal50
        val OnPrimary = Color.White
        val DisabledBg = Grey200
        val Destructive = Red
    }

    // Figma: shifa/color/alpha/*
    object Alpha {
        val White70 = Color(0xB3FFFFFF)
        val White90 = Color(0xE6FFFFFF)
        val Teal75 = Color(0xBF00BBB0)
        val Teal15 = Color(0x2600BBB0)
        val Black5 = Color(0x0D000000)
    }
}
