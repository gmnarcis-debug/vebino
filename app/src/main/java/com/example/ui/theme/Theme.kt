package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.domain.model.AppFontChoice
import com.example.domain.model.AppThemeMode

@Immutable
data class WebinoExtendedColors(
    val isDark: Boolean,
    val canvasTop: Color,
    val canvasMid: Color,
    val canvasBottom: Color,
    val ambientVioletOrb: Color,
    val ambientCyanOrb: Color,
    val ambientMintOrb: Color,
    val glassContainer: Color,
    val glassContainerStrong: Color,
    val glassStrokeTop: Color,
    val glassStrokeBottom: Color,
    val cardShadowTint: Color,
    val navPillContainer: Color,
    val navPillBorder: Color,
    val navSelectedChip: Color,
    val terminalContainer: Color,
    val terminalBorder: Color,
    val subtleSurface: Color,
)

private val LightExtendedColors = WebinoExtendedColors(
    isDark = false,
    canvasTop = LavenderCanvasTop,
    canvasMid = LavenderCanvasMid,
    canvasBottom = PeachCanvasBottom,
    ambientVioletOrb = Color(0x55A78BFA),
    ambientCyanOrb = Color(0x55FF6AA8),
    ambientMintOrb = Color(0x44FF9F43),
    glassContainer = LightGlassFill,
    glassContainerStrong = LightGlassStrong,
    glassStrokeTop = LightGlassStrokeTop,
    glassStrokeBottom = LightGlassStrokeBottom,
    cardShadowTint = Color(0x338B5CF6),
    navPillContainer = Color(0xC7FFFFFF),
    navPillBorder = Color(0xFFD9E3F4),
    navSelectedChip = Color(0x228B5CF6),
    terminalContainer = Color(0xFF0F172A),
    terminalBorder = Color(0xFF1E293B),
    subtleSurface = Color(0xFFF6EDFB),
)

private val DarkExtendedColors = WebinoExtendedColors(
    isDark = true,
    canvasTop = NavyVoid,
    canvasMid = NavyDeep,
    canvasBottom = Color(0xFF0F1634),
    ambientVioletOrb = Color(0x307C3AED),
    ambientCyanOrb = Color(0x2406B6D4),
    ambientMintOrb = Color(0x1A10B981),
    glassContainer = DarkGlassFill,
    glassContainerStrong = DarkGlassStrong,
    glassStrokeTop = DarkGlassStrokeTop,
    glassStrokeBottom = DarkGlassStrokeBottom,
    cardShadowTint = Color(0x4D02040D),
    navPillContainer = Color(0xEB141C3A),
    navPillBorder = Color(0xFF2D3C70),
    navSelectedChip = Color(0x297C3AED),
    terminalContainer = NavyVoid,
    terminalBorder = NavyBorder,
    subtleSurface = Color(0xFF151E3F),
)

val LocalWebinoColors = staticCompositionLocalOf { LightExtendedColors }

object WebinoThemeTokens {
    val colors: WebinoExtendedColors
        @Composable get() = LocalWebinoColors.current
}

private val WebinoDarkScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = PureWhite,
    primaryContainer = ElectricVioletDark,
    onPrimaryContainer = SnowWhite,
    secondary = CyanAccent,
    onSecondary = NavyVoid,
    secondaryContainer = NavyCard,
    onSecondaryContainer = CyanLight,
    tertiary = ElectricVioletLight,
    onTertiary = NavyVoid,
    background = NavyDeep,
    onBackground = SnowWhite,
    surface = NavySurface,
    onSurface = SnowWhite,
    surfaceVariant = NavyCard,
    onSurfaceVariant = SoftGray,
    outline = NavyBorder,
    outlineVariant = NavyBorder,
    error = ErrorCoral,
    onError = PureWhite,
)

private val WebinoLightScheme = lightColorScheme(
    primary = ElectricViolet,
    onPrimary = PureWhite,
    primaryContainer = ElectricVioletSoft,
    onPrimaryContainer = ElectricVioletDark,
    secondary = SkyBlue,
    onSecondary = PureWhite,
    secondaryContainer = SkyBlueSoft,
    onSecondaryContainer = LightText,
    tertiary = MintGreen,
    onTertiary = PureWhite,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightGlassStrokeBottom,
    error = ErrorCoral,
    onError = PureWhite,
)

val WebinoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun WebinoTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    fontChoice: AppFontChoice = AppFontChoice.VAZIRMATN,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        AppThemeMode.NAVY_DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) WebinoDarkScheme else WebinoLightScheme
    val extendedColors = if (isDark) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity != null) {
                runCatching {
                    val controller = WindowCompat.getInsetsController(activity.window, view)
                    controller.isAppearanceLightStatusBars = !isDark
                    controller.isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    CompositionLocalProvider(LocalWebinoColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildWebinoTypography(fontChoice),
            shapes = WebinoShapes,
            content = content,
        )
    }
}
