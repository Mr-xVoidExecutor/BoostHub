package com.boosthub.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0B1020)
val SurfaceDark = Color(0xFF151B33)
val BrandBlue = Color(0xFF1877F2)
val BrandPurple = Color(0xFF7B2FF7)
val BrandPink = Color(0xFFFF4F9A)
val BrandGold = Color(0xFFFFC83D)
val BrandGreen = Color(0xFF2ED47A)
val TextSoft = Color(0xFFB4BCD6)

private val Scheme = darkColorScheme(
    primary = BrandBlue,
    secondary = BrandPurple,
    background = BgDark,
    surface = SurfaceDark,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun BoostHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
