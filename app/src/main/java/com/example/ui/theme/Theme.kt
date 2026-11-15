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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = MintTint,
    onPrimaryContainer = SuccessFgLight,
    secondary = MintAccent,
    onSecondary = ForestDark,
    secondaryContainer = SageSurface,
    onSecondaryContainer = SuccessFgLight,
    tertiary = AmberMatch,
    onTertiary = AmberMatchText,
    tertiaryContainer = AmberCardTint,
    onTertiaryContainer = AmberCardInk,
    error = DestructiveRed,
    onError = Color.White,
    errorContainer = DestructiveBg,
    onErrorContainer = DestructiveRed,
    background = BackgroundLight,
    onBackground = InkPrimary,
    surface = SurfaceWhite,
    onSurface = InkPrimary,
    surfaceVariant = NeutralChipBgLight,
    onSurfaceVariant = InkSecondary,
    outline = OutlineSubtle
)

private val DarkColors = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = SuccessBgDark,
    onPrimaryContainer = SuccessFgDark,
    secondary = MintAccent,
    onSecondary = ForestDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = SuccessFgDark,
    tertiary = AmberMatch,
    onTertiary = AmberMatchText,
    tertiaryContainer = Color(0xFF3E3119),
    onTertiaryContainer = WarningDark,
    error = DangerDark,
    onError = ForestDark,
    errorContainer = DangerBgDark,
    onErrorContainer = DangerDark,
    background = CanvasDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = MutedDark,
    outline = OutlineDark
)

val CoM8Shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object CoM8ThemeTokens {
    val colors: CoM8Colors
        @Composable
        @ReadOnlyComposable
        get() = LocalCoM8Colors.current
}

@Composable
fun CoM8Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val semanticColors = if (darkTheme) DarkCoM8Colors else LightCoM8Colors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(LocalCoM8Colors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = CoM8Shapes,
            content = content
        )
    }
}
