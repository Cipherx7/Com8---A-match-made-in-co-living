package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Mode Design Tokens
val BackgroundLight = Color(0xFFF5F7F6)
val SurfaceWhite = Color(0xFFFFFFFF)
val NeutralChipBgLight = Color(0xFFEEF2F1)
val OutlineSubtle = Color(0xFFDCE3E1)
val InkPrimary = Color(0xFF10201F)
val InkSecondary = Color(0xFF4F5F5D)
val InkMuted = Color(0xFF7B8B88)

val EmeraldPrimary = Color(0xFF0B7A6B)
val EmeraldPressed = Color(0xFF085C50)
val ForestDark = Color(0xFF10201F)
val ForestCardBack = Color(0xFF1A2B29)
val MintAccent = Color(0xFF3CC9B0)
val MintTint = Color(0xFFDDF1EC)
val SuccessFgLight = Color(0xFF0A5F54)
val SageSurface = Color(0xFFDDF1EC)

val AmberMatch = Color(0xFFF5C04A)
val AmberMatchText = Color(0xFF2B2205)
val AmberCardTint = Color(0xFFF3DFB3)
val AmberCardInk = Color(0xFF7A5200)
val LavenderCardTint = Color(0xFFDCE2FA)
val LavenderCardInk = Color(0xFF2D438B)

val DestructiveRed = Color(0xFFB42318)
val DangerWarm = Color(0xFF9A3412)
val DestructiveBg = Color(0xFFFEECEB)
val WarningAmber = Color(0xFF7A5200)

// Dark Mode Design Tokens
val CanvasDark = Color(0xFF10201F)
val SurfaceDark = Color(0xFF1A2B29)
val SurfaceVariantDark = Color(0xFF233735)
val InkDark = Color(0xFFF5F7F6)
val MutedDark = Color(0xFFA9B8B5)
val OutlineDark = Color(0xFF2C403D)
val SuccessBgDark = Color(0xFF143D36)
val SuccessFgDark = Color(0xFF3CC9B0)
val DangerDark = Color(0xFFF87171)
val DangerBgDark = Color(0xFF3A1D1C)
val WarningDark = Color(0xFFF5C04A)

// Backwards-compatible alias for light canvas
val CanvasLight = BackgroundLight
val SurfaceSubtle = NeutralChipBgLight

@Immutable
data class CoM8Colors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val border: Color,
    val primary: Color,
    val primaryDark: Color,
    val accentMint: Color,
    val accentAmber: Color,
    val accentAmberText: Color,
    val successBg: Color,
    val successFg: Color,
    val warnFg: Color,
    val dangerFg: Color,
    val dangerBg: Color,
    val neutralChipBg: Color,
    val heroCardBack: Color,
    val pastelMint: Color,
    val pastelAmber: Color,
    val pastelLavender: Color,
    val mapLand: Color,
    val mapPark: Color,
    val mapWater: Color,
    val mapRoad: Color
)

val LightCoM8Colors = CoM8Colors(
    isDark = false,
    background = BackgroundLight,
    surface = SurfaceWhite,
    onSurface = InkPrimary,
    onSurfaceMuted = InkSecondary,
    border = OutlineSubtle,
    primary = EmeraldPrimary,
    primaryDark = ForestDark,
    accentMint = MintAccent,
    accentAmber = AmberMatch,
    accentAmberText = AmberMatchText,
    successBg = MintTint,
    successFg = SuccessFgLight,
    warnFg = WarningAmber,
    dangerFg = DestructiveRed,
    dangerBg = DestructiveBg,
    neutralChipBg = NeutralChipBgLight,
    heroCardBack = ForestCardBack,
    pastelMint = Color(0xFFDDF1EC),
    pastelAmber = AmberCardTint,
    pastelLavender = LavenderCardTint,
    mapLand = Color(0xFFE3EEEA),
    mapPark = Color(0xFFD2E5DA),
    mapWater = Color(0xFFD7E2EC),
    mapRoad = Color(0xFFFFFFFF)
)

val DarkCoM8Colors = CoM8Colors(
    isDark = true,
    background = CanvasDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    onSurfaceMuted = MutedDark,
    border = OutlineDark,
    primary = EmeraldPrimary,
    primaryDark = ForestDark,
    accentMint = MintAccent,
    accentAmber = AmberMatch,
    accentAmberText = AmberMatchText,
    successBg = SuccessBgDark,
    successFg = SuccessFgDark,
    warnFg = WarningDark,
    dangerFg = DangerDark,
    dangerBg = DangerBgDark,
    neutralChipBg = SurfaceVariantDark,
    heroCardBack = SurfaceDark,
    pastelMint = Color(0xFF1B3A35),
    pastelAmber = Color(0xFF3E3119),
    pastelLavender = Color(0xFF28304A),
    mapLand = Color(0xFF182A27),
    mapPark = Color(0xFF1D3B33),
    mapWater = Color(0xFF1C303D),
    mapRoad = Color(0xFF2D4541)
)

val LocalCoM8Colors = staticCompositionLocalOf { LightCoM8Colors }
