package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.AiCompatibilityReport
import com.example.data.model.ComparisonStatus
import com.example.data.model.CompatibilityCalculator
import com.example.data.model.UserLifestyleProfile
import com.example.ui.components.CircularBackButton
import com.example.ui.components.InitialAvatarCircle
import com.example.ui.components.LeafletMapView
import com.example.ui.components.MapPinData
import com.example.ui.theme.CoM8ThemeTokens
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.viewmodel.ScoredCandidate
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CandidateCompatibilityPane(
    candidate: ScoredCandidate,
    userProfile: UserLifestyleProfile?,
    aiReportState: UiState<AiCompatibilityReport>,
    onRequestAiReport: (ScoredCandidate) -> Unit,
    onBack: () -> Unit,
    onPass: (() -> Unit)? = null,
    onLikeAndChat: (ScoredCandidate) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = CoM8ThemeTokens.colors
    val l = candidate.listing
    var showFullMapModal by remember { mutableStateOf(false) }
    var previewPhotoIndex by remember { mutableStateOf<Int?>(null) }

    val fallbackSummary = remember(userProfile, l) {
        CompatibilityCalculator.computeWhyFitSummary(userProfile, l)
    }
    val comparisons = remember(userProfile, l) {
        CompatibilityCalculator.computeTraitComparisons(userProfile, l)
    }

    LaunchedEffect(l.id) {
        if (aiReportState is UiState.Idle) {
            onRequestAiReport(candidate)
        }
    }

    val whyFitText = when (aiReportState) {
        is UiState.Success -> "${aiReportState.data.headline} ${aiReportState.data.strengths.firstOrNull().orEmpty()}".trim()
        else -> fallbackSummary
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = tokens.background,
        contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = tokens.surface,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = tokens.border, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onPass != null) {
                            OutlinedButton(
                                onClick = onPass,
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("detail_pass_button"),
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.dp, tokens.border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = tokens.surface,
                                    contentColor = tokens.onSurfaceMuted
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Pass",
                                    tint = tokens.onSurfaceMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Button(
                            onClick = { onLikeAndChat(candidate) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("detail_vibe_button"),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Like ${l.name.substringBefore(' ')}",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // Top Pastel Banner with Back Button and Large Avatar Circle
            val bannerColor = if (tokens.isDark) tokens.pastelMint else Color(0xFFCFE3DE)
            val avatarTextColor = if (tokens.isDark) tokens.accentMint else tokens.primary

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bannerColor)
                    .statusBarsPadding()
                    .height(170.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                CircularBackButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.Center),
                    shape = CircleShape,
                    color = tokens.background
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        InitialAvatarCircle(
                            initial = l.avatarInitials.ifBlank { l.name },
                            size = 96.dp,
                            fontSize = 42.sp,
                            backgroundColor = tokens.background,
                            textColor = avatarTextColor,
                            photoUri = l.photoUri
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${l.name.substringBefore(' ')}, ${l.age}",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = tokens.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = tokens.accentAmber
                    ) {
                        Text(
                            text = "${candidate.matchScore}%",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = tokens.primaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val subtitleParts = buildList {
                    if (l.gender.isNotBlank()) add(l.gender)
                    add(
                        if (l.organizationName.isNotBlank()) "${l.occupation} · ${l.organizationName}"
                        else l.occupation
                    )
                    add("${l.locality}, ${l.city}")
                }
                Text(
                    text = subtitleParts.joinToString(" · "),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = tokens.onSurfaceMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        l.flatTypeLabel,
                        l.roomsFreeLabel,
                        "₹${formatAmount(l.monthlyRent.toInt())} per month",
                        l.moveInTimeline
                    ).filter { it.isNotBlank() }.forEach { pill ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (tokens.isDark) tokens.neutralChipBg else tokens.primaryDark
                        ) {
                            Text(
                                text = pill,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (tokens.isDark) tokens.onSurface else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Mint "Why you fit" Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.successBg
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Why you fit",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = tokens.successFg
                            )
                            if (aiReportState is UiState.Error) {
                                Text(
                                    text = "Retry AI",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = tokens.successFg,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onRequestAiReport(candidate) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = whyFitText,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = tokens.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                val itemsList = l.flatPhotoLabels.ifEmpty { listOf("Living room", "Your room", "Kitchen") }
                val photoCount = if (l.flatPhotoUris.isNotEmpty()) l.flatPhotoUris.size else itemsList.size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Space & vibe",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = tokens.onSurface
                    )
                    Text(
                        text = "$photoCount photos",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = tokens.onSurfaceMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Full-bleed horizontal photo gallery (236x160dp tiles, 10dp gap)
            val itemsList = l.flatPhotoLabels.ifEmpty { listOf("Living room", "Your room", "Kitchen") }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                itemsIndexed(itemsList) { idx, label ->
                    val bgTint = when (idx % 3) {
                        0 -> if (tokens.isDark) tokens.pastelMint else Color(0xFFCFE3DE)
                        1 -> if (tokens.isDark) tokens.pastelAmber else Color(0xFFF6DDB0)
                        else -> if (tokens.isDark) tokens.pastelLavender else Color(0xFFDDE3F5)
                    }
                    val fgTint = when (idx % 3) {
                        0 -> if (tokens.isDark) tokens.accentMint else Color(0xFF0A5F54)
                        1 -> if (tokens.isDark) tokens.accentAmber else Color(0xFF7A5200)
                        else -> if (tokens.isDark) tokens.onSurface else Color(0xFF2F4A8A)
                    }
                    val roomIcon = when (idx % 3) {
                        0 -> Icons.Outlined.Chair
                        1 -> Icons.Outlined.Bed
                        else -> Icons.Outlined.Kitchen
                    }
                    val photoUri = l.flatPhotoUris.getOrNull(idx)

                    Box(
                        modifier = Modifier
                            .size(width = 236.dp, height = 160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgTint)
                            .clickable { previewPhotoIndex = idx }
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = roomIcon,
                                contentDescription = label,
                                tint = fgTint,
                                modifier = Modifier
                                    .size(38.dp)
                                    .align(Alignment.Center)
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = tokens.surface
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = tokens.onSurface
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Location card with Approximate Area Map + "Open map"
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Location",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = tokens.onSurface
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = tokens.surface,
                        border = BorderStroke(1.dp, tokens.border)
                    ) {
                        Column {
                            ApproximateLocationMapBox(
                                badgeText = "${l.locality}, ${l.city}",
                                onClick = { showFullMapModal = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 10.dp)
                                ) {
                                    Text(
                                        text = "Approximate area",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = tokens.onSurface
                                    )
                                    Text(
                                        text = "exact address shared after match",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = tokens.onSurfaceMuted
                                    )
                                }
                                OutlinedButton(
                                    onClick = { showFullMapModal = true },
                                    shape = RoundedCornerShape(22.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    border = BorderStroke(1.dp, tokens.primary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.primary),
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .testTag("open_map_button")
                                ) {
                                    Text(
                                        text = "Open map",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // How you compare grouped card
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "How you compare",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = tokens.onSurface
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = tokens.surface,
                        border = BorderStroke(1.dp, tokens.border)
                    ) {
                        Column {
                            comparisons.forEachIndexed { index, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = row.trait,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = tokens.onSurface
                                    )
                                    val statusColor = when (row.status) {
                                        ComparisonStatus.MATCH -> tokens.successFg
                                        ComparisonStatus.CLOSE -> tokens.warnFg
                                        ComparisonStatus.DIFFERS -> tokens.dangerFg
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        when (row.status) {
                                            ComparisonStatus.MATCH -> Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = statusColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            ComparisonStatus.CLOSE -> Text(
                                                text = "—",
                                                color = statusColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                            ComparisonStatus.DIFFERS -> Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = null,
                                                tint = statusColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = row.label,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = statusColor
                                        )
                                    }
                                }
                                if (index < comparisons.lastIndex) {
                                    HorizontalDivider(color = tokens.border, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFullMapModal) {
        Dialog(
            onDismissRequest = { showFullMapModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = tokens.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${l.locality}, ${l.city}",
                                style = MaterialTheme.typography.titleLarge,
                                color = tokens.onSurface
                            )
                            Text(
                                text = "Approximate neighborhood radius",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.onSurfaceMuted
                            )
                        }
                        CircularBackButton(onClick = { showFullMapModal = false })
                    }
                    LeafletMapView(
                        pins = listOf(MapPinData(l, candidate.matchScore)),
                        selectedListingId = l.id,
                        onMarkerClick = {},
                        zoomLevel = 14,
                        interactive = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }

    val selectedIdx = previewPhotoIndex
    if (selectedIdx != null) {
        val itemsList = l.flatPhotoLabels.ifEmpty { listOf("Living room", "Your room", "Kitchen") }
        val label = itemsList.getOrElse(selectedIdx) { "Flat photo" }
        val uri = l.flatPhotoUris.getOrNull(selectedIdx)
        Dialog(
            onDismissRequest = { previewPhotoIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(24.dp),
                color = tokens.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.onSurface
                        )
                        CircularBackButton(onClick = { previewPhotoIndex = null })
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(tokens.pastelMint),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!uri.isNullOrBlank()) {
                            AsyncImage(
                                model = uri,
                                contentDescription = label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = when (selectedIdx % 3) {
                                    0 -> Icons.Outlined.Chair
                                    1 -> Icons.Outlined.Bed
                                    else -> Icons.Outlined.Kitchen
                                },
                                contentDescription = label,
                                tint = tokens.successFg,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApproximateLocationMapBox(
    badgeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionButtonLabel: String? = null
) {
    val tokens = CoM8ThemeTokens.colors
    val landColor = tokens.mapLand
    val parkColor = tokens.mapPark
    val waterColor = tokens.mapWater
    val roadColor = tokens.mapRoad
    val pinPrimary = tokens.primary
    val pinDotCore = tokens.surface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(landColor)
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRoundRect(
                color = parkColor,
                topLeft = Offset(w * 0.6f, h * 0.08f),
                size = Size(w * 0.34f, h * 0.36f),
                cornerRadius = CornerRadius(18f, 18f)
            )
            drawRoundRect(
                color = waterColor,
                topLeft = Offset(w * 0.08f, h * 0.56f),
                size = Size(w * 0.24f, h * 0.26f),
                cornerRadius = CornerRadius(14f, 14f)
            )

            val crispRoad = roadColor.copy(alpha = 0.96f)
            drawLine(crispRoad, Offset(0f, h * 0.34f), Offset(w, h * 0.26f), strokeWidth = 14f, cap = StrokeCap.Round)
            drawLine(crispRoad, Offset(0f, h * 0.78f), Offset(w, h * 0.58f), strokeWidth = 12f, cap = StrokeCap.Round)
            drawLine(crispRoad, Offset(w * 0.28f, 0f), Offset(w * 0.19f, h), strokeWidth = 14f, cap = StrokeCap.Round)
            drawLine(crispRoad, Offset(w * 0.72f, 0f), Offset(w * 0.66f, h), strokeWidth = 10f, cap = StrokeCap.Round)

            val curve = Path().apply {
                moveTo(w * 0.32f, 0f)
                quadraticTo(w * 0.45f, h * 0.5f, w * 0.48f, h)
            }
            drawPath(curve, color = crispRoad, style = Stroke(width = 10f))

            val pinCenter = Offset(w * 0.5f, h * 0.52f)
            drawCircle(
                color = pinPrimary.copy(alpha = 0.16f),
                radius = h * 0.28f,
                center = pinCenter
            )
            drawCircle(
                color = pinDotCore,
                radius = 12.dp.toPx(),
                center = pinCenter
            )
            drawCircle(
                color = pinPrimary,
                radius = 9.dp.toPx(),
                center = pinCenter
            )
        }

        if (badgeText.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(14.dp),
                color = tokens.surface
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = tokens.onSurface
                )
            }
        }

        if (!actionButtonLabel.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(999.dp),
                color = tokens.surface,
                border = BorderStroke(1.dp, tokens.border)
            ) {
                Text(
                    text = actionButtonLabel,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.onSurface
                )
            }
        }
    }
}

private fun formatAmount(amount: Int): String {
    val s = amount.toString()
    return if (s.length > 3) "${s.dropLast(3)},${s.takeLast(3)}" else s
}
