package com.phataklive.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Brand: night-navy rail + signal amber; gate states use traffic-signal colours tuned for both themes. */
@Immutable
data class PhatakColors(
    val headerTop: Color,
    val headerBottom: Color,
    val onHeader: Color,
    val onHeaderMuted: Color,
    val amber: Color,
    val closed: Color, val closedBg: Color,
    val soon: Color, val soonBg: Color,
    val open: Color, val openBg: Color,
    val live: Color,
    val muted: Color,
    val card: Color,
    val cardBorder: Color,
)

private val Light = PhatakColors(
    headerTop = Color(0xFF0B1B3A), headerBottom = Color(0xFF1C3A72),
    onHeader = Color.White, onHeaderMuted = Color(0xFFB8C4DC),
    amber = Color(0xFFFFB020),
    closed = Color(0xFFD7263D), closedBg = Color(0xFFFFEEF0),
    soon = Color(0xFFE07A00), soonBg = Color(0xFFFFF4E2),
    open = Color(0xFF138A52), openBg = Color(0xFFE6F6EE),
    live = Color(0xFF2563EB),
    muted = Color(0xFF667085),
    card = Color.White, cardBorder = Color(0xFFE6E9F0),
)

private val Dark = PhatakColors(
    headerTop = Color(0xFF050B18), headerBottom = Color(0xFF13274D),
    onHeader = Color.White, onHeaderMuted = Color(0xFF9AA8C4),
    amber = Color(0xFFFFC14D),
    closed = Color(0xFFFF5C6C), closedBg = Color(0xFF3A1419),
    soon = Color(0xFFFFA43A), soonBg = Color(0xFF3A2510),
    open = Color(0xFF3DDC97), openBg = Color(0xFF0F3324),
    live = Color(0xFF6EA8FF),
    muted = Color(0xFF98A2B3),
    card = Color(0xFF131C2E), cardBorder = Color(0xFF223049),
)

val LocalPhatak = staticCompositionLocalOf { Light }

object Phatak {
    val c: PhatakColors @Composable get() = LocalPhatak.current
}

private val AppType = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.6.sp),
)

@Composable
fun PhatakTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) Dark else Light
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFF8FB2FF), onPrimary = Color(0xFF0B1B3A), secondary = p.amber,
        background = Color(0xFF0A1120), surface = Color(0xFF0F1727), surfaceVariant = Color(0xFF1A2438),
        onBackground = Color(0xFFE7ECF5), onSurface = Color(0xFFE7ECF5), onSurfaceVariant = Color(0xFFB4BED0),
        outline = Color(0xFF34425E), surfaceContainer = Color(0xFF131C2E),
    ) else lightColorScheme(
        primary = Color(0xFF1C3A72), onPrimary = Color.White, secondary = p.amber,
        background = Color(0xFFF4F6FA), surface = Color.White, surfaceVariant = Color(0xFFEEF1F7),
        onBackground = Color(0xFF101828), onSurface = Color(0xFF101828), onSurfaceVariant = Color(0xFF475467),
        outline = Color(0xFFD0D5DD), surfaceContainer = Color.White,
    )
    CompositionLocalProvider(LocalPhatak provides p) {
        MaterialTheme(colorScheme = scheme, typography = AppType, content = content)
    }
}
