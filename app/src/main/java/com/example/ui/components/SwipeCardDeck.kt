package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoM8ThemeTokens
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.viewmodel.ScoredCandidate
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PROFILE_CARDS_PER_AD = 3

@Composable
fun SwipeCardDeck(
    candidates: List<ScoredCandidate>,
    totalAvailableCount: Int = candidates.size,
    onSwipe: (ScoredCandidate, Boolean) -> Unit,
    onInspectCandidate: (ScoredCandidate) -> Unit,
    onResetPassed: () -> Unit,
    onOpenFilters: () -> Unit = {},
    onCreatePost: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = CoM8ThemeTokens.colors
    val topCandidate = candidates.firstOrNull()
    val nextCandidate = candidates.getOrNull(1)
    var swipesSinceAd by remember { mutableIntStateOf(0) }
    val nativeAdController = rememberSwipeDeckNativeAdController()
    val activeNativeAd = nativeAdController.currentAd
    val showingAdCard = topCandidate != null && swipesSinceAd >= PROFILE_CARDS_PER_AD && activeNativeAd != null
    val cardKey = if (showingAdCard) "native_ad_${activeNativeAd.hashCode()}" else topCandidate?.listing?.id

    val scope = rememberCoroutineScope()
    val offsetX = remember(cardKey) { Animatable(0f) }
    val offsetY = remember(cardKey) { Animatable(0f) }
    val currentOnSwipe by rememberUpdatedState(onSwipe)

    LaunchedEffect(cardKey) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
    }

    fun dismissAdCard() {
        swipesSinceAd = 0
        nativeAdController.consumeAndPreloadNext()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val photoHeight = (maxHeight * 0.62f).coerceIn(240.dp, 410.dp)
            if (topCandidate == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyDeckCard(
                        hasAnyUsersInPool = totalAvailableCount > 0,
                        onResetPassed = onResetPassed,
                        onOpenFilters = onOpenFilters,
                        onCreatePost = onCreatePost
                    )
                }
            } else if (showingAdCard && activeNativeAd != null) {
                val progress = (abs(offsetX.value) / 280f).coerceIn(0f, 1f)
                // Peeking profile card behind the Native Ad card
                FlatmateDeckCard(
                    candidate = topCandidate,
                    photoHeight = photoHeight,
                    swipeOffsetX = 0f,
                    onCardClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .offset(y = 14.dp)
                        .graphicsLayer {
                            val scale = 0.96f + (0.04f * progress)
                            scaleX = scale
                            scaleY = scale
                            alpha = 0.85f + (0.15f * progress)
                        }
                )

                NativeAdDeckCard(
                    nativeAd = activeNativeAd,
                    mediaHeight = (photoHeight * 0.72f).coerceIn(220.dp, 300.dp),
                    onDismissAd = { dismissAdCard() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .offset {
                            IntOffset(
                                offsetX.value.roundToInt(),
                                offsetY.value.roundToInt()
                            )
                        }
                        .graphicsLayer {
                            rotationZ = (offsetX.value / 42f).coerceIn(-11f, 11f)
                        }
                        .pointerInput(cardKey) {
                            detectDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (abs(offsetX.value) > 150f) {
                                            val targetX = if (offsetX.value > 0f) 1250f else -1250f
                                            offsetX.animateTo(
                                                targetValue = targetX,
                                                animationSpec = tween(durationMillis = 210)
                                            )
                                            dismissAdCard()
                                        } else {
                                            launch {
                                                offsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                            launch {
                                                offsetY.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        launch { offsetX.animateTo(0f, spring()) }
                                        launch { offsetY.animateTo(0f, spring()) }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                        offsetY.snapTo((offsetY.value + dragAmount.y * 0.25f).coerceIn(-65f, 65f))
                                    }
                                }
                            )
                        }
                )
            } else {
                val backdropColor = if (tokens.isDark) tokens.heroCardBack else Color(0xFFDDE6E4)
                if (nextCandidate != null) {
                    val progress = (abs(offsetX.value) / 280f).coerceIn(0f, 1f)
                    FlatmateDeckCard(
                        candidate = nextCandidate,
                        photoHeight = photoHeight,
                        swipeOffsetX = 0f,
                        onCardClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .offset(y = 14.dp)
                            .graphicsLayer {
                                val scale = 0.96f + (0.04f * progress)
                                scaleX = scale
                                scaleY = scale
                                alpha = 0.85f + (0.15f * progress)
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(start = 14.dp, end = 14.dp, top = 14.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(backdropColor)
                    )
                }

                FlatmateDeckCard(
                    candidate = topCandidate,
                    photoHeight = photoHeight,
                    swipeOffsetX = offsetX.value,
                    onCardClick = { onInspectCandidate(topCandidate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .offset {
                            IntOffset(
                                offsetX.value.roundToInt(),
                                offsetY.value.roundToInt()
                            )
                        }
                        .graphicsLayer {
                            rotationZ = (offsetX.value / 42f).coerceIn(-11f, 11f)
                        }
                        .pointerInput(topCandidate.listing.id) {
                            detectDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (abs(offsetX.value) > 150f) {
                                            val liked = offsetX.value > 0f
                                            val targetX = if (liked) 1250f else -1250f
                                            offsetX.animateTo(
                                                targetValue = targetX,
                                                animationSpec = tween(durationMillis = 210)
                                            )
                                            swipesSinceAd += 1
                                            if (swipesSinceAd >= PROFILE_CARDS_PER_AD && nativeAdController.currentAd == null) {
                                                nativeAdController.consumeAndPreloadNext()
                                            }
                                            currentOnSwipe(topCandidate, liked)
                                        } else {
                                            launch {
                                                offsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                            launch {
                                                offsetY.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        launch { offsetX.animateTo(0f, spring()) }
                                        launch { offsetY.animateTo(0f, spring()) }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                        offsetY.snapTo((offsetY.value + dragAmount.y * 0.25f).coerceIn(-65f, 65f))
                                    }
                                }
                            )
                        }
                        .testTag("top_swipe_card")
                )
            }
        }

        if (topCandidate != null) {
            Text(
                text = if (showingAdCard) {
                    "Swipe left or right to skip ad · tap button to open"
                } else {
                    "Swipe right to like · swipe left to pass"
                },
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = tokens.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp, bottom = 14.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlatmateDeckCard(
    candidate: ScoredCandidate,
    photoHeight: Dp,
    swipeOffsetX: Float,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = CoM8ThemeTokens.colors
    val l = candidate.listing
    val pastelBg = if (tokens.isDark) tokens.pastelMint else Color(0xFFCFE3DE)
    val initialFg = if (tokens.isDark) tokens.accentMint else tokens.primary

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(24.dp),
        color = tokens.surface,
        border = BorderStroke(1.dp, tokens.border),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(photoHeight)
                    .background(pastelBg)
                    .padding(14.dp)
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart),
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.accentAmber
                ) {
                    Text(
                        text = "${candidate.matchScore}% match",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = tokens.primaryDark
                    )
                }

                if (l.verified) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = RoundedCornerShape(16.dp),
                        color = tokens.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = "Verified",
                                tint = tokens.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Verified",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = tokens.primary
                            )
                        }
                    }
                }

                val avatarSize = (photoHeight * 0.36f).coerceIn(96.dp, 148.dp)
                val avatarFont = if (avatarSize >= 136.dp) 64.sp else 48.sp
                Surface(
                    modifier = Modifier
                        .size(avatarSize)
                        .align(Alignment.Center),
                    shape = CircleShape,
                    color = tokens.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        InitialAvatarCircle(
                            initial = l.avatarInitials.ifBlank { l.name },
                            size = avatarSize,
                            fontSize = avatarFont,
                            backgroundColor = tokens.surface,
                            textColor = initialFg,
                            photoUri = l.photoUri
                        )
                    }
                }

                Surface(
                    modifier = Modifier.align(Alignment.BottomStart),
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.primaryDark
                ) {
                    Text(
                        text = l.roleSummaryBadge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (abs(swipeOffsetX) > 45f) {
                    val isLike = swipeOffsetX > 0f
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 136.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isLike) tokens.primary else tokens.dangerFg
                    ) {
                        Text(
                            text = if (isLike) "LIKE" else "PASS",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.surface)
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${l.name.substringBefore(' ')}, ${l.age}",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = tokens.onSurface,
                        maxLines = 1
                    )
                    val roleSubtitle = buildString {
                        if (l.gender.isNotBlank()) append("${l.gender} · ")
                        append(l.occupation.ifBlank { "Roommate" })
                        if (l.organizationName.isNotBlank()) append(" · ${l.organizationName}")
                    }
                    Text(
                        text = roleSubtitle,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = tokens.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = tokens.onSurfaceMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${l.locality}, ${l.city} · ${l.rentRangeLabel}",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = tokens.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    l.traitPills().forEachIndexed { idx, tag ->
                        val isMintPill = idx < 3
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isMintPill) tokens.successBg else tokens.neutralChipBg
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isMintPill) tokens.successFg else tokens.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDeckCard(
    hasAnyUsersInPool: Boolean,
    onResetPassed: () -> Unit,
    onOpenFilters: () -> Unit,
    onCreatePost: () -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(26.dp),
        color = tokens.surface,
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                tint = tokens.successFg,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = if (hasAnyUsersInPool) "You’ve seen everyone here" else "No flatmates found yet",
                style = MaterialTheme.typography.titleLarge,
                color = tokens.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (hasAnyUsersInPool) {
                    "Adjust your filters or bring back profiles you passed on earlier."
                } else {
                    "Verified roommates who sign up or post in your area appear here automatically."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.onSurfaceMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (hasAnyUsersInPool) {
                OutlinedButton(
                    onClick = onResetPassed,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.2.dp, tokens.primary)
                ) {
                    Text("Review passed profiles", color = tokens.successFg)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onOpenFilters,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.2.dp, tokens.primary)
                    ) {
                        Text("Adjust filters", color = tokens.successFg)
                    }
                    OutlinedButton(
                        onClick = onCreatePost,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.2.dp, tokens.border)
                    ) {
                        Text("Post search", color = tokens.onSurface)
                    }
                }
            }
        }
    }
}
