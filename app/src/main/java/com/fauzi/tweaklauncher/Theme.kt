package com.fauzi.tweaklauncher

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Warna semantik yang tidak ikut berubah dengan tema. */
val Green = Color(0xFF4ADE80)
val Orange = Color(0xFFFF9F5A)
val Blue = Color(0xFF7DB8FF)
val Red = Color(0xFFFF6B6B)

data class Palette(
    val bg: Color,
    val card: Color,
    val inset: Color,
    val line: Color,
    val text: Color,
    val soft: Color,
    val sub: Color,
    val accent: Color,
    val onAccent: Color,
    val radius: Dp,
    val dark: Boolean,
    val wallpaper: Boolean
)

fun parseColor(hex: String, fallback: Color = Color(0xFFC6F432)): Color = try {
    Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
} catch (e: Exception) {
    fallback
}

fun buildPalette(theme: String, accentHex: String, radius: Int, cardAlpha: Int, wallpaper: Boolean): Palette {
    val a = parseColor(accentHex)
    val onA = if (a.luminance() > 0.5f) Color(0xFF0A0B0F) else Color.White
    val alpha = cardAlpha.coerceIn(20, 100) / 100f
    val r = radius.coerceIn(6, 44).dp
    return when (theme) {
        "light" -> Palette(
            bg = Color(0xFFF4F5F8), card = Color.White.copy(alpha = alpha), inset = Color(0xFFEDEFF3),
            line = Color(0xFFDCE0E8), text = Color(0xFF12141A), soft = Color(0xFF3A4150), sub = Color(0xFF5C6577),
            accent = a, onAccent = onA, radius = r, dark = false, wallpaper = wallpaper
        )
        "dark" -> Palette(
            bg = Color(0xFF14161B), card = Color(0xFF1F222A).copy(alpha = alpha), inset = Color(0xFF171A20),
            line = Color(0xFF2E323C), text = Color(0xFFF2F4F8), soft = Color(0xFFC7CDD8), sub = Color(0xFF9AA3B2),
            accent = a, onAccent = onA, radius = r, dark = true, wallpaper = wallpaper
        )
        else -> Palette(
            bg = Color(0xFF000000), card = Color(0xFF111318).copy(alpha = alpha), inset = Color(0xFF08090C),
            line = Color(0xFF22262F), text = Color(0xFFF2F4F8), soft = Color(0xFFC7CDD8), sub = Color(0xFF9AA3B2),
            accent = a, onAccent = onA, radius = r, dark = true, wallpaper = wallpaper
        )
    }
}

/** Palet layar LCD PDA (abu-hijau) dengan bar judul biru tua ala Pocket PC. */
fun pdaPalette() = Palette(
    bg = Color(0xFFC5CFA8), card = Color(0xFFB8C49A), inset = Color(0xFFABB88C),
    line = Color(0xFF3A4528), text = Color(0xFF1A2112), soft = Color(0xFF263019), sub = Color(0xFF47552F),
    accent = Color(0xFF1F2F6B), onAccent = Color(0xFFE6ECCB), radius = 3.dp, dark = false, wallpaper = false
)

/** Palet Windows 95: desktop teal, jendela abu-abu, bar judul biru tua. */
fun win95Palette() = Palette(
    bg = Color(0xFF008080), card = Color(0xFFC0C0C0), inset = Color(0xFFFFFFFF),
    line = Color(0xFF808080), text = Color(0xFF000000), soft = Color(0xFF1A1A1A), sub = Color(0xFF404040),
    accent = Color(0xFF000080), onAccent = Color(0xFFFFFFFF), radius = 0.dp, dark = false, wallpaper = false
)

fun iconShape(name: String): Shape = when (name) {
    "circle" -> CircleShape
    "rounded" -> RoundedCornerShape(percent = 24)
    "square" -> RoundedCornerShape(percent = 8)
    "teardrop" -> RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 50, bottomStartPercent = 10)
    else -> RoundedCornerShape(percent = 36)
}

val LocalPal = staticCompositionLocalOf { buildPalette("amoled", "#C6F432", 28, 100, false) }
val LocalCfg = staticCompositionLocalOf<Cfg> { error("Cfg belum disediakan") }

/** Akses warna tema dengan singkat: Pal.accent, Pal.card, dst. */
object Pal {
    val bg: Color @androidx.compose.runtime.Composable get() = LocalPal.current.bg
    val card: Color @androidx.compose.runtime.Composable get() = LocalPal.current.card
    val inset: Color @androidx.compose.runtime.Composable get() = LocalPal.current.inset
    val line: Color @androidx.compose.runtime.Composable get() = LocalPal.current.line
    val text: Color @androidx.compose.runtime.Composable get() = LocalPal.current.text
    val soft: Color @androidx.compose.runtime.Composable get() = LocalPal.current.soft
    val sub: Color @androidx.compose.runtime.Composable get() = LocalPal.current.sub
    val accent: Color @androidx.compose.runtime.Composable get() = LocalPal.current.accent
    val onAccent: Color @androidx.compose.runtime.Composable get() = LocalPal.current.onAccent
    val dark: Boolean @androidx.compose.runtime.Composable get() = LocalPal.current.dark
    val wallpaper: Boolean @androidx.compose.runtime.Composable get() = LocalPal.current.wallpaper
    val shape: Shape @androidx.compose.runtime.Composable get() = RoundedCornerShape(LocalPal.current.radius)
    val pill: Shape @androidx.compose.runtime.Composable get() = when (LocalCfg.current.uiMode) {
        "pda" -> RoundedCornerShape(3.dp)
        "win95" -> androidx.compose.ui.graphics.RectangleShape
        else -> CircleShape
    }
}

@Composable
fun TweakTheme(cfg: Cfg, content: @Composable () -> Unit) {
    val pda = cfg.uiMode == "pda"
    val pal = if (pda) pdaPalette() else if (cfg.uiMode == "win95") win95Palette() else buildPalette(cfg.theme, cfg.accent, cfg.radius, cfg.cardAlpha, cfg.background == "wallpaper")
    val ff = if (pda) FontFamily.Monospace else if (cfg.uiMode == "win95") FontFamily.SansSerif else when (cfg.font) {
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }
    val scheme = if (pal.dark) {
        darkColorScheme(primary = pal.accent, background = pal.bg, surface = pal.card, onSurface = pal.text)
    } else {
        lightColorScheme(primary = pal.accent, background = pal.bg, surface = pal.card, onSurface = pal.text)
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalPal provides pal,
            LocalCfg provides cfg,
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = ff, color = pal.text)
        ) {
            content()
        }
    }
}
