package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompatibilityCalculator
import com.example.data.model.MatchMessage
import com.example.data.model.SwipeRecord
import com.example.data.model.UserLifestyleProfile
import com.example.ui.components.InitialAvatarCircle
import com.example.ui.components.MatchScoreBadge
import com.example.ui.theme.AmberCardInk
import com.example.ui.theme.CoM8Colors
import com.example.ui.theme.CoM8ThemeTokens
import com.example.ui.theme.LavenderCardInk
import com.example.ui.viewmodel.ScoredCandidate
import com.example.ui.viewmodel.UiState

@Composable
fun MatchesAndChatScreen(
    matches: List<SwipeRecord>,
    allCandidates: List<ScoredCandidate>,
    userProfile: UserLifestyleProfile?,
    activeChatListingId: String?,
    chatMessagesState: UiState<List<MatchMessage>>,
    onOpenChat: (String) -> Unit,
    onCloseChat: () -> Unit,
    onSendMessage: (String, String) -> Unit,
    onReportUser: (listingId: String, targetName: String, reason: String, details: String) -> Unit = { _, _, _, _ -> },
    onInspectCandidate: (ScoredCandidate) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = CoM8ThemeTokens.colors
    val activeCandidate = allCandidates.firstOrNull { it.listing.id == activeChatListingId }
    val activeMatch = matches.firstOrNull { it.listingId == activeChatListingId }
        ?: activeCandidate?.let { cand ->
            SwipeRecord(
                listingId = cand.listing.id,
                decision = "LIKED",
                status = "SAVED",
                targetName = cand.listing.name,
                targetCity = cand.listing.city,
                targetLocality = cand.listing.locality,
                targetRent = cand.listing.monthlyRent,
                compatibilityScore = cand.matchScore.toLong()
            )
        }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
    ) {
        val isWide = maxWidth >= 740.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MatchesListPane(
                    matches = matches,
                    allCandidates = allCandidates,
                    selectedListingId = activeChatListingId,
                    onSelectMatch = onOpenChat,
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                )
                Surface(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight()
                        .border(1.dp, tokens.border, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    color = tokens.background
                ) {
                    AnimatedContent(
                        targetState = activeMatch,
                        contentKey = { it?.listingId ?: "__empty__" },
                        transitionSpec = {
                            fadeIn(tween(220, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(tween(180, easing = FastOutSlowInEasing))
                        },
                        label = "wide_chat_transition"
                    ) { targetMatch ->
                        if (targetMatch != null) {
                            val targetCandidate = allCandidates.firstOrNull { it.listing.id == targetMatch.listingId }
                            MatchChatPane(
                                match = targetMatch,
                                candidate = targetCandidate,
                                userProfile = userProfile,
                                messagesState = chatMessagesState,
                                showBackButton = false,
                                onBack = onCloseChat,
                                onSendMessage = { msg -> onSendMessage(targetMatch.listingId, msg) },
                                onReportUser = { reason, details ->
                                    onReportUser(targetMatch.listingId, targetMatch.targetName, reason, details)
                                },
                                onInspect = { targetCandidate?.let(onInspectCandidate) }
                            )
                        } else {
                            EmptyChatPlaceholder()
                        }
                    }
                }
            }
        } else {
            if (activeMatch != null) {
                BackHandler { onCloseChat() }
            }
            AnimatedContent(
                targetState = activeMatch,
                contentKey = { it?.listingId ?: "__list__" },
                transitionSpec = {
                    val enteringChat = targetState != null && initialState == null
                    if (enteringChat) {
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> fullWidth }
                        ) + fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> -(fullWidth * 0.22f).toInt() }
                                ) + fadeOut(tween(durationMillis = 200))
                            )
                    } else {
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> -(fullWidth * 0.22f).toInt() }
                        ) + fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> fullWidth }
                                ) + fadeOut(tween(durationMillis = 200))
                            )
                    }.using(SizeTransform(clip = false))
                },
                label = "mobile_match_chat_transition"
            ) { targetMatch ->
                if (targetMatch != null) {
                    val targetCandidate = allCandidates.firstOrNull { it.listing.id == targetMatch.listingId }
                    MatchChatPane(
                        match = targetMatch,
                        candidate = targetCandidate,
                        userProfile = userProfile,
                        messagesState = chatMessagesState,
                        showBackButton = true,
                        onBack = onCloseChat,
                        onSendMessage = { msg -> onSendMessage(targetMatch.listingId, msg) },
                        onReportUser = { reason, details ->
                            onReportUser(targetMatch.listingId, targetMatch.targetName, reason, details)
                        },
                        onInspect = { targetCandidate?.let(onInspectCandidate) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MatchesListPane(
                        matches = matches,
                        allCandidates = allCandidates,
                        selectedListingId = null,
                        onSelectMatch = onOpenChat,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchesListPane(
    matches: List<SwipeRecord>,
    allCandidates: List<ScoredCandidate>,
    selectedListingId: String?,
    onSelectMatch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = CoM8ThemeTokens.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Matches",
                style = MaterialTheme.typography.headlineMedium,
                color = tokens.onSurface
            )
            Text(
                text = "${matches.size} new ${if (matches.size == 1) "match" else "matches"} this week",
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.onSurfaceMuted
            )
        }

        if (matches.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = tokens.surface,
                border = BorderStroke(1.dp, tokens.border)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = tokens.successFg,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No matches yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.onSurface
                    )
                    Text(
                        text = "Swipe right on compatible flatmates in Discover to start chatting.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.onSurfaceMuted
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(matches, key = { "strip_${it.listingId}" }) { match ->
                    val (bgTint, fgTint) = avatarPaletteFor(match.targetName, tokens)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { onSelectMatch(match.listingId) }
                    ) {
                        InitialAvatarCircle(
                            initial = match.targetName,
                            size = 74.dp,
                            fontSize = 24.sp,
                            backgroundColor = bgTint,
                            textColor = fgTint,
                            ringColor = tokens.primary,
                            photoUri = allCandidates.firstOrNull { it.listing.id == match.listingId }?.listing?.photoUri.orEmpty()
                        )
                        Text(
                            text = match.targetName.substringBefore(' '),
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.onSurface
                        )
                    }
                }
            }

            Text(
                text = "Conversations",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.onSurface
            )

            LazyColumn {
                items(matches, key = { it.listingId }) { match ->
                    val (bgTint, fgTint) = avatarPaletteFor(match.targetName, tokens)
                    val isSelected = match.listingId == selectedListingId
                    val previewText = match.lastMessagePreview.ifBlank {
                        "You matched. Say hi and share your move-in date."
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) tokens.successBg.copy(alpha = 0.65f) else Color.Transparent)
                            .clickable { onSelectMatch(match.listingId) }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                            .testTag("match_item_${match.listingId}"),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InitialAvatarCircle(
                            initial = match.targetName,
                            size = 52.dp,
                            fontSize = 19.sp,
                            backgroundColor = bgTint,
                            textColor = fgTint,
                            photoUri = allCandidates.firstOrNull { it.listing.id == match.listingId }?.listing?.photoUri.orEmpty()
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = match.targetName.substringBefore(' '),
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.onSurface
                            )
                            Text(
                                text = previewText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.onSurfaceMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatRelativeTime(match.updatedAt),
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.onSurfaceMuted
                            )
                            if (match.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(tokens.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = match.unreadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(22.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = tokens.border, thickness = 1.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchChatPane(
    match: SwipeRecord,
    candidate: ScoredCandidate?,
    userProfile: UserLifestyleProfile?,
    messagesState: UiState<List<MatchMessage>>,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onReportUser: (reason: String, details: String) -> Unit,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = CoM8ThemeTokens.colors
    var messageDraft by remember(match.listingId) { mutableStateOf("") }
    var showReportDialog by remember(match.listingId) { mutableStateOf(false) }
    val (bgTint, fgTint) = avatarPaletteFor(match.targetName, tokens)
    val sharedTraits = remember(userProfile, candidate) {
        if (candidate != null) {
            CompatibilityCalculator.computeSharedTraits(userProfile, candidate.listing)
        } else {
            listOf("Smoke-free home", "Early riser", "Budget overlap")
        }
    }
    val listState = rememberLazyListState()
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(messages.size, match.listingId) {
        if (messages.isNotEmpty()) {
            val targetIdx = messages.size + 1
            listState.animateScrollToItem(targetIdx)
        }
    }

    val sendDraft = {
        val trimmed = messageDraft.trim()
        if (trimmed.isNotEmpty()) {
            onSendMessage(trimmed)
            messageDraft = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .imePadding()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = tokens.surface
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onInspect)
                    ) {
                        if (showBackButton) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("chat_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = "Back to matches",
                                    tint = tokens.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        InitialAvatarCircle(
                            initial = match.targetName,
                            size = 42.dp,
                            fontSize = 17.sp,
                            backgroundColor = bgTint,
                            textColor = fgTint,
                            photoUri = candidate?.listing?.photoUri.orEmpty()
                        )
                        Column {
                            Text(
                                text = match.targetName.substringBefore(' '),
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.onSurface
                            )
                            Text(
                                text = "Verified · ${match.targetLocality}, ${match.targetCity}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                color = tokens.onSurfaceMuted
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MatchScoreBadge(
                            score = match.compatibilityScore.toInt(),
                            showMatchWord = false
                        )
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("report_user_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = "Report user",
                                tint = tokens.dangerFg,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = tokens.border, thickness = 1.dp)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "shared_traits_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = tokens.successBg
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "What you share",
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.successFg,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sharedTraits.forEach { trait ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = tokens.surface
                                ) {
                                    Text(
                                        text = trait,
                                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = tokens.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "chat_day_header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (messages.isEmpty() && messagesState is UiState.Success) {
                            "Start the conversation with ${match.targetName.substringBefore(' ')}"
                        } else {
                            "Today"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.onSurfaceMuted
                    )
                }
            }

            if (messagesState is UiState.Loading) {
                item(key = "chat_loading_row") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = tokens.primary,
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            } else if (messagesState is UiState.Error) {
                item(key = "chat_error_row") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = tokens.dangerBg
                    ) {
                        Text(
                            text = messagesState.message,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.dangerFg
                        )
                    }
                }
            }

            itemsIndexed(
                items = messages,
                key = { idx, msg -> "${msg.id}_${msg.createdAt}_$idx" }
            ) { _, msg ->
                val isMe = msg.senderRole == "ME"
                val bubbleShape = if (isMe) {
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
                } else {
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 290.dp),
                        shape = bubbleShape,
                        color = if (isMe) tokens.primary else tokens.surface,
                        border = if (isMe) null else BorderStroke(1.dp, tokens.border)
                    ) {
                        Text(
                            text = msg.text,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isMe) Color.White else tokens.onSurface
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quickReplies = listOf(
                    "Share my move-in date",
                    "Ask about guests"
                )
                items(quickReplies) { quick ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                val text = if (quick.contains("move-in")) {
                                    "I'm planning to move in ${userProfile?.moveInTimeline?.lowercase() ?: "soon"}. Does that work for you?"
                                } else {
                                    "Quick question — how do you feel about weekend guests?"
                                }
                                onSendMessage(text)
                            },
                        shape = RoundedCornerShape(999.dp),
                        color = tokens.surface,
                        border = BorderStroke(1.dp, tokens.border)
                    ) {
                        Text(
                            text = quick,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = tokens.surface,
                    border = BorderStroke(1.dp, tokens.border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = messageDraft,
                            onValueChange = { messageDraft = it },
                            singleLine = true,
                            cursorBrush = SolidColor(tokens.primary),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = tokens.onSurface),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = { sendDraft() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_message_input")
                        )
                        if (messageDraft.isEmpty()) {
                            Text(
                                text = "Message ${match.targetName.substringBefore(' ')}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = tokens.onSurfaceMuted
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { sendDraft() }
                        .testTag("send_chat_button"),
                    shape = CircleShape,
                    color = tokens.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send message",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportUserDialog(
            targetName = match.targetName.substringBefore(' ').ifBlank { "this user" },
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                showReportDialog = false
                onReportUser(reason, details)
            }
        )
    }
}

@Composable
private fun ReportUserDialog(
    targetName: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    val reasons = remember {
        listOf(
            "Inappropriate content",
            "Harassment or abuse",
            "Spam or scam",
            "Fake profile",
            "Other"
        )
    }
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        title = {
            Text(
                text = "Report $targetName",
                style = MaterialTheme.typography.titleLarge,
                color = tokens.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Help keep CoM8 safe by letting us know why you're flagging this conversation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.onSurfaceMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                            .testTag("report_reason_${reason.lowercase().replace(' ', '_')}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = tokens.primary,
                                unselectedColor = tokens.onSurfaceMuted
                            )
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.onSurface
                        )
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 500) details = it },
                    label = { Text("Additional details (optional)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tokens.primary,
                        unfocusedBorderColor = tokens.border,
                        focusedLabelColor = tokens.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_details_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.dangerFg,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("Report User", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = tokens.onSurface)
            }
        }
    )
}

@Composable
private fun EmptyChatPlaceholder() {
    val tokens = CoM8ThemeTokens.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ChatBubbleOutline,
                contentDescription = null,
                tint = tokens.successFg,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Select a match to start chatting",
                style = MaterialTheme.typography.titleMedium,
                color = tokens.onSurface
            )
        }
    }
}

private fun avatarPaletteFor(name: String, tokens: CoM8Colors): Pair<Color, Color> {
    val first = name.trim().firstOrNull()?.uppercaseChar() ?: 'R'
    return when (first) {
        'A', 'S', 'M' -> tokens.pastelAmber to if (tokens.isDark) tokens.accentAmber else AmberCardInk
        'K', 'N', 'T' -> tokens.pastelLavender to if (tokens.isDark) tokens.onSurface else LavenderCardInk
        else -> tokens.pastelMint to tokens.successFg
    }
}

private fun formatRelativeTime(timestampMs: Long): String {
    val diffMin = (System.currentTimeMillis() - timestampMs) / 60_000L
    return when {
        diffMin < 1L -> "Now"
        diffMin < 60L -> "${diffMin}m"
        diffMin < 1440L -> "${diffMin / 60L}h"
        else -> "Yesterday"
    }
}
