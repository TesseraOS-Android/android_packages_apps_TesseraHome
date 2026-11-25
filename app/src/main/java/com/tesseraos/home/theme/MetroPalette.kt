package com.tesseraos.home.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.tesseraos.home.R

data class MetroAccent(
    val name: String,
    val hex: Long
) {
    val color: Color get() = Color(hex)
}

object MetroPalette {
    val PureBlack = Color(0xFF000000)
    val SurfaceDark = Color(0xFF1F1F1F)
    val SurfaceSubtle = Color(0xFF1A1A1A)
    val White = Color(0xFFFFFFFF)
    val MutedLabel = Color(0xFF999999)
    val HelperGray = Color(0xFF8A8A8A)

    val Accents: List<MetroAccent> = listOf(
        MetroAccent("Lime", 0xFFA4C400),
        MetroAccent("Green", 0xFF60A917),
        MetroAccent("Emerald", 0xFF008A00),
        MetroAccent("Teal", 0xFF00ABA9),
        MetroAccent("Cyan", 0xFF1BA1E2),
        MetroAccent("Cobalt", 0xFF0050EF),
        MetroAccent("Indigo", 0xFF6A00FF),
        MetroAccent("Violet", 0xFFAA00FF),
        MetroAccent("Pink", 0xFFF472D0),
        MetroAccent("Magenta", 0xFFD80073),
        MetroAccent("Crimson", 0xFFA20025),
        MetroAccent("Red", 0xFFE51400),
        MetroAccent("Orange", 0xFFFA6800),
        MetroAccent("Amber", 0xFFF0A30A),
        MetroAccent("Yellow", 0xFFE3C800),
        MetroAccent("Brown", 0xFF825A2C),
        MetroAccent("Olive", 0xFF6D8764),
        MetroAccent("Steel", 0xFF647687),
        MetroAccent("Mauve", 0xFF76608A),
        MetroAccent("Taupe", 0xFF87794E)
    )

    val DefaultAccentHex: Long = 0xFF0050EF

    val Selawik: FontFamily = FontFamily(
        Font(R.font.selawik_light, FontWeight.Light),
        Font(R.font.selawik_semilight, FontWeight.ExtraLight),
        Font(R.font.selawik_regular, FontWeight.Normal),
        Font(R.font.selawik_semibold, FontWeight.SemiBold),
        Font(R.font.selawik_bold, FontWeight.Bold)
    )
}
