package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CoM8ThemeTokens
import com.example.ui.theme.SpaceGroteskFontFamily

fun Modifier.pressScaleClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(),
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun CoM8Wordmark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onDarkBackground: Boolean = false
) {
    val tokens = CoM8ThemeTokens.colors
    val iconShape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = painterResource(id = com.example.R.drawable.com8_brand_launcher_icon_1794810825878),
            contentDescription = "CoM8 logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(if (compact) 32.dp else 36.dp)
                .clip(iconShape)
                .then(
                    if (onDarkBackground) {
                        Modifier.border(
                            width = 1.dp,
                            color = Color(0xFF3CC9B0).copy(alpha = 0.35f),
                            shape = iconShape
                        )
                    } else {
                        Modifier
                    }
                )
        )
        Text(
            text = "CoM8",
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 19.sp else 22.sp,
            color = if (onDarkBackground) Color(0xFFF5F7F6) else tokens.onSurface
        )
    }
}

@Composable
fun CircularBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Back"
) {
    val tokens = CoM8ThemeTokens.colors
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .pressScaleClickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = tokens.surface,
            border = BorderStroke(1.dp, tokens.border)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = contentDescription,
                    tint = tokens.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun MatchScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
    showMatchWord: Boolean = true
) {
    val tokens = CoM8ThemeTokens.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = tokens.accentAmber
    ) {
        Text(
            text = if (showMatchWord) "$score% match" else "$score%",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.accentAmberText
        )
    }
}

@Composable
fun VerifiedBadgePill(
    modifier: Modifier = Modifier,
    label: String = "Verified"
) {
    val tokens = CoM8ThemeTokens.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = tokens.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = label,
                tint = tokens.successFg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tokens.successFg
            )
        }
    }
}

@Composable
fun InitialAvatarCircle(
    initial: String,
    size: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = CoM8ThemeTokens.colors.successBg,
    textColor: Color = CoM8ThemeTokens.colors.successFg,
    photoUri: String = "",
    photoResId: Int = 0,
    ringColor: Color? = null
) {
    val cleanInitial = initial.trim().take(1).uppercase().ifEmpty { "C" }
    val ringModifier = if (ringColor != null) {
        Modifier
            .border(2.dp, ringColor, CircleShape)
            .padding(3.dp)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(ringModifier)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        val presetResId = photoUri.removePrefix("preset:").toIntOrNull()
        when {
            photoUri.isNotBlank() && !photoUri.startsWith("preset:") -> {
                AsyncImage(
                    model = photoUri,
                    contentDescription = cleanInitial,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            presetResId != null && presetResId != 0 -> {
                Image(
                    painter = painterResource(id = presetResId),
                    contentDescription = cleanInitial,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            photoResId != 0 -> {
                Image(
                    painter = painterResource(id = photoResId),
                    contentDescription = cleanInitial,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Text(
                    text = cleanInitial,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = textColor
                )
            }
        }
    }
}
