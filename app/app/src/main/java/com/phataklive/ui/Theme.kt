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

/**
 * Brand: night-navy rail + signal amber; gate states use traffic-signal colours tuned for both themes.
 * Content sits on frosted glass panels over a soft ambient light field (the station lamps at dusk: navy sky,
 * a warm amber glow, a little signal green).
 */
@Immutable
data class PhatakColors(
    val dark: Boolean,
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
    /** glass panel fill (cards) and the denser fill for fields and menus */
    val card: Color,
    val glassStrong: Color,
    /** light catching the top edge of a glass panel, fading to the bottom edge */
    val edgeTop: Color,
    val edgeBottom: Color,
    /** hairlines inside panels (dividers, inactive timeline dots) */
    val cardBorder: Color,
    /** ambient light field behind the glass */
    val bgBase: Color,
    val glowCool: Color,
    val glowWarm: Color,
    val glowSignal: Color,
    /** frosted bottom bar: tint over the blur, and the tint used where blur is unavailable (Android 10-11) */
    val chrome: Color,
    val chromeFallback: Color,
)

private val Light = PhatakColors(
    dark = false,
    headerTop = Color(0xFF0B1B3A), headerBottom = Color(0xFF1C3A72),
    onHeader = Color.White, onHeaderMuted = Color(0xFFB8C4DC),
    amber = Color(0xFFFFB020),
    closed = Color(0xFFC81E33), closedBg = Color(0xFFFFEEF0),
    soon = Color(0xFFB85F00), soonBg = Color(0xFFFFF4E2),
    open = Color(0xFF0F7A47), openBg = Color(0xFFE6F6EE),
    live = Color(0xFF2155D6),
    muted = Color(0xFF55627A),
    card = Color.White.copy(alpha = 0.64f), glassStrong = Color.White.copy(alpha = 0.84f),
    edgeTop = Color.White.copy(alpha = 0.95f), edgeBottom = Color(0xFF7D90B8).copy(alpha = 0.22f),
    cardBorder = Color(0xFF9AA9C7).copy(alpha = 0.30f),
    bgBase = Color(0xFFEEF2FA),
    glowCool = Color(0xFFBFD0FF), glowWarm = Color(0xFFFFD58A).copy(alpha = 0.70f), glowSignal = Color(0xFFBDEBD9).copy(alpha = 0.75f),
    chrome = Color.White.copy(alpha = 0.55f), chromeFallback = Color.White.copy(alpha = 0.90f),
)

private val Dark = PhatakColors(
    dark = true,
    headerTop = Color(0xFF050B18), headerBottom = Color(0xFF13274D),
    onHeader = Color.White, onHeaderMuted = Color(0xFF9AA8C4),
    amber = Color(0xFFFFC14D),
    closed = Color(0xFFFF6676), closedBg = Color(0xFF3A1419),
    soon = Color(0xFFFFA94A), soonBg = Color(0xFF3A2510),
    open = Color(0xFF45DFA0), openBg = Color(0xFF0F3324),
    live = Color(0xFF7DB2FF),
    muted = Color(0xFFA3ADBF),
    card = Color(0xFF17223A).copy(alpha = 0.60f), glassStrong = Color(0xFF17223A).copy(alpha = 0.86f),
    edgeTop = Color.White.copy(alpha = 0.17f), edgeBottom = Color.White.copy(alpha = 0.04f),
    cardBorder = Color.White.copy(alpha = 0.10f),
    bgBase = Color(0xFF060B17),
    glowCool = Color(0xFF1F4288).copy(alpha = 0.60f), glowWarm = Color(0xFFFFB020).copy(alpha = 0.12f), glowSignal = Color(0xFF14A578).copy(alpha = 0.12f),
    chrome = Color(0xFF0E1628).copy(alpha = 0.55f), chromeFallback = Color(0xFF0E1628).copy(alpha = 0.94f),
)

val LocalPhatak = staticCompositionLocalOf { Light }

object Phatak {
    val c: PhatakColors @Composable get() = LocalPhatak.current
}

/** Theme choice in the app: "auto" follows the phone, "light" / "dark" force one. */
object ThemeMode {
    const val AUTO = "auto"
    const val LIGHT = "light"
    const val DARK = "dark"
    fun next(mode: String) = when (mode) { AUTO -> LIGHT; LIGHT -> DARK; else -> AUTO }
}

@Composable
fun isDark(mode: String): Boolean = when (mode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    else -> isSystemInDarkTheme()
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
fun PhatakTheme(mode: String = ThemeMode.AUTO, content: @Composable () -> Unit) {
    val dark = isDark(mode)
    val p = if (dark) Dark else Light
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFF8FB2FF), onPrimary = Color(0xFF0B1B3A), secondary = p.amber,
        background = p.bgBase, surface = Color(0xFF0F1727), surfaceVariant = Color(0xFF1A2438),
        onBackground = Color(0xFFE7ECF5), onSurface = Color(0xFFE7ECF5), onSurfaceVariant = Color(0xFFB9C3D4),
        outline = Color(0xFF34425E), surfaceContainer = Color(0xFF131C2E),
    ) else lightColorScheme(
        primary = Color(0xFF1C3A72), onPrimary = Color.White, secondary = p.amber,
        background = p.bgBase, surface = Color.White, surfaceVariant = Color(0xFFE8EDF6),
        onBackground = Color(0xFF101828), onSurface = Color(0xFF101828), onSurfaceVariant = Color(0xFF414C60),
        outline = Color(0xFFC5CDDB), surfaceContainer = Color.White,
    )
    CompositionLocalProvider(LocalPhatak provides p) {
        MaterialTheme(colorScheme = scheme, typography = AppType, content = content)
    }
}
