package com.boosthub.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgLight = Color(0xFFF3F5F8)
val SurfaceWhite = Color(0xFFFFFFFF)
val BorderGray = Color(0xFFE2E5EA)
val TextBlack = Color(0xFF111111)
val TextSoft = Color(0xFF555B66)

val BrandBlue = Color(0xFF1877F2)
val BrandPurple = Color(0xFF7B2FF7)
val BrandPink = Color(0xFFE91E63)
val BrandGold = Color(0xFFFFC83D)
val BrandGreen = Color(0xFF1FA463)
val BrandOrange = Color(0xFFF57C00)
val BrandRed = Color(0xFFD93025)

private val Scheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandPurple,
    background = BgLight,
    onBackground = TextBlack,
    surface = SurfaceWhite,
    onSurface = TextBlack,
    surfaceVariant = BgLight,
    onSurfaceVariant = TextSoft,
    outline = BorderGray
)

@Composable
fun BoostHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
