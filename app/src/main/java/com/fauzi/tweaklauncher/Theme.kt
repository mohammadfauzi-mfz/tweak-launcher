package com.fauzi.tweaklauncher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF07080B)
val CardBg = Color(0xFF12141A)
val Inset = Color(0xFF0B0C10)
val Line = Color(0xFF232733)
val TextC = Color(0xFFF2F4F8)
val TextSoft = Color(0xFFC7CDD8)
val Sub = Color(0xFF9AA3B2)
val Lime = Color(0xFFC6F432)
val Ink = Color(0xFF0A0B0F)
val Green = Color(0xFF4ADE80)
val Orange = Color(0xFFFF9F5A)
val Blue = Color(0xFF7DB8FF)

@Composable
fun TweakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lime,
            background = Bg,
            surface = CardBg,
            onSurface = TextC
        ),
        content = content
    )
}
