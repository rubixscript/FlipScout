package com.rubixscript.flipscout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Quiet monochrome: near-white (or near-black) canvas, soft grey cards, ink-black actions, one green for gains. */
object Palette {
    val Emerald = Color(0xFF34B27A)
    val EmeraldDeep = Color(0xFF2A9A68)
    val Teal = Color(0xFF0E7490)
    val Loss = Color(0xFFE5594F)
    val Blue = Color(0xFF5B9BF0)
    val Violet = Color(0xFF9B86F3)
    val Amber = Color(0xFFF0A93B)
    val ChatGpt = Color(0xFF10A37F)
    val Gemini = Color(0xFF4285F4)
    val Ink = Color(0xFF0B0B0B)

    /** Smoky graphite gradient for the one "feature" card on a screen. */
    val GraphiteBrush = Brush.linearGradient(listOf(Color(0xFF5A5B63), Color(0xFF2B2B30), Color(0xFF151517)))
}

/** Spacing, sizing and radius tokens so screens stay consistent without hand-picked dp values. */
object Dimens {
    val screen = 20.dp          // horizontal padding for every screen
    val cardRadius = 28.dp      // list cards and insight cards
    val fieldRadius = 16.dp     // text fields
    val buttonHeight = 56.dp    // full-width primary buttons
    val buttonRadius = 28.dp    // fully-rounded pill buttons
    val rowPadding = 16.dp      // inside a card row
    val listGap = 12.dp         // gap between cards
    val bottomBarSpace = 32.dp  // breathing room under the last list row
}

/** Type scale: big, bold, tight-tracked headlines over quiet secondary text. */
private val FlipTypography = Typography(
    headlineMedium = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = androidx.compose.ui.text.TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
    labelLarge = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF0A0A0A),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF161616),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF212121),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF2E2E2E),
    outlineVariant = Color(0xFF1F1F1F),
    error = Palette.Loss,
)

private val Light = lightColorScheme(
    primary = Palette.Ink,
    onPrimary = Color.White,
    background = Color(0xFFFDFDFD),
    onBackground = Color(0xFF0B0B0B),
    surface = Color(0xFFF6F6F6),
    onSurface = Color(0xFF0B0B0B),
    surfaceVariant = Color(0xFFEFEFEF),
    onSurfaceVariant = Color(0xFF7B7B7F),
    outline = Color(0xFFDADADA),
    outlineVariant = Color(0xFFEAEAEA),
    error = Color(0xFFD9443A),
)

@Composable
fun FlipTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = FlipTypography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(Dimens.cardRadius),
            extraLarge = RoundedCornerShape(36.dp),
        ),
        content = content,
    )
}
