package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.CoM8Catalog
import com.example.data.model.RoleType
import com.example.data.repository.CoM8LocalRepository
import com.example.data.repository.GeminiAiRepository
import com.example.ui.auth.MobileAuthScreen
import com.example.ui.components.CoM8Wordmark
import com.example.ui.components.SwipeCardDeck
import com.example.ui.screens.CandidateCompatibilityPane
import com.example.ui.screens.LifestyleQuizForm
import com.example.ui.screens.MatchesAndChatScreen
import com.example.ui.screens.PermissionsPrePromptScreen
import com.example.ui.screens.PostYourselfScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CoM8ThemeTokens
import com.example.ui.viewmodel.CoM8ViewModel
import com.example.ui.viewmodel.ScoredCandidate
import com.example.ui.viewmodel.UiState
import com.example.util.CoM8PermissionHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CoM8Tab(
    val label: String,
    val icon: ImageVector,
    val tag: String
) {
    DISCOVER("Discover", Icons.Outlined.Layers, "nav_discover"),
    MATCHES("Matches", Icons.Outlined.FavoriteBorder, "nav_matches"),
    POST("Post", Icons.Outlined.AddCircleOutline, "nav_post"),
    PROFILE("Profile", Icons.Outlined.PersonOutline, "nav_profile")
}

private sealed interface SessionCheckState {
    data object Loading : SessionCheckState
    data class Ready(val phone: String?) : SessionCheckState
}

@Composable
fun CoM8AppNavigation() {
    val tokens = CoM8ThemeTokens.colors
    val context = LocalContext.current
    val repository = remember(context.applicationContext) {
        CoM8LocalRepository(context.applicationContext)
    }
    val sessionState by remember(repository) {
        kotlinx.coroutines.flow.flow {
            repository.activePhoneFlow.collect { phone ->
                emit(SessionCheckState.Ready(phone))
            }
        }
    }.collectAsStateWithLifecycle(initialValue = SessionCheckState.Loading)

    when (val state = sessionState) {
        SessionCheckState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tokens.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = tokens.primary)
            }
        }
        is SessionCheckState.Ready -> {
            val phone = state.phone
            if (phone.isNullOrBlank()) {
                MobileAuthScreen(repository = repository)
            } else {
                key(phone) {
                    CoM8AuthenticatedRoot(activePhoneNumber = phone)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoM8AuthenticatedRoot(
    activePhoneNumber: String
) {
    val tokens = CoM8ThemeTokens.colors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel: CoM8ViewModel = viewModel(
        key = activePhoneNumber,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                CoM8ViewModel(
                    localRepository = CoM8LocalRepository(app),
                    geminiAiRepository = GeminiAiRepository(),
                    activePhoneNumber = activePhoneNumber
                )
            }
        }
    )

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeModeState.collectAsStateWithLifecycle()
    val permissionsPrompted by viewModel.permissionsPromptedState.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettingsState.collectAsStateWithLifecycle()
    val ownListing by viewModel.ownListingState.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val deckCandidates by viewModel.deckCandidates.collectAsStateWithLifecycle()
    val allMergedListings by viewModel.allMergedListings.collectAsStateWithLifecycle()
    val likedMatches by viewModel.likedMatches.collectAsStateWithLifecycle()
    val selectedCandidateId by viewModel.selectedCandidateId.collectAsStateWithLifecycle()
    val aiReportState by viewModel.aiReportState.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val activeChatListingId by viewModel.activeChatListingId.collectAsStateWithLifecycle()
    val activeChatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(CoM8Tab.DISCOVER) }
    var editingQuiz by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDiscoverFilterSheet by remember { mutableStateOf(false) }
    var sheetCandidate by remember { mutableStateOf<ScoredCandidate?>(null) }
    var phoneLinkedInSession by remember(activePhoneNumber) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionError) {
        val err = actionError
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(err)
            viewModel.clearActionError()
        }
    }

    val performFullSignOut: () -> Unit = {
        coroutineScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    CredentialManager.create(context)
                        .clearCredentialState(ClearCredentialStateRequest())
                }
                runCatching { Firebase.auth.signOut() }
            }
            viewModel.signOut()
        }
    }

    val userProfile = (userProfileState as? UiState.Success)?.data
    val currentFirebasePhone = Firebase.auth.currentUser?.phoneNumber
    val isPhoneVerified = phoneLinkedInSession ||
        !currentFirebasePhone.isNullOrBlank() ||
        userProfile?.authProvider == "PHONE"
    val needsInitialQuiz = userProfileState is UiState.Success && (userProfile == null || !userProfile.quizCompleted)

    if (needsInitialQuiz || editingQuiz) {
        if (editingQuiz) {
            BackHandler { editingQuiz = false }
        }
        Scaffold(
            containerColor = tokens.background,
            contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(tokens.background),
                contentAlignment = Alignment.TopCenter
            ) {
                LifestyleQuizForm(
                    initialProfile = userProfile,
                    defaultName = userProfile?.displayName?.ifBlank { "Abhishek" } ?: "Abhishek",
                    phoneNumberDisplay = currentFirebasePhone?.takeIf { it.isNotBlank() }
                        ?: activePhoneNumber.takeIf { it.startsWith("+") }
                        ?: userProfile?.email.orEmpty(),
                    isPhoneVerified = isPhoneVerified,
                    onPhoneLinked = { linkedNumber ->
                        phoneLinkedInSession = true
                        viewModel.markPhoneVerified(linkedNumber) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Phone verified and linked to your account.")
                            }
                        }
                    },
                    onCancel = if (editingQuiz) { { editingQuiz = false } } else null,
                    onSaveQuiz = { saved ->
                        viewModel.saveLifestyleQuiz(saved) {
                            editingQuiz = false
                        }
                    }
                )
            }
        }
        return
    }

    if (userProfileState is UiState.Loading || permissionsPrompted == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tokens.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoM8Wordmark()
                CircularProgressIndicator(color = tokens.primary)
            }
        }
        return
    }

    if (permissionsPrompted == false) {
        val missingPermissions = CoM8PermissionHelper.missingPostOnboardingPermissions(context)
        if (missingPermissions.isEmpty()) {
            LaunchedEffect(Unit) {
                viewModel.completePermissionOnboarding(
                    context = context,
                    notificationsGranted = CoM8PermissionHelper.hasNotificationPermission(context),
                    coarseLocationGranted = CoM8PermissionHelper.hasCoarseLocationPermission(context)
                )
            }
        } else {
            Scaffold(
                containerColor = tokens.background,
                contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(tokens.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                    PermissionsPrePromptScreen(
                        onFinished = { notifGranted, locGranted ->
                            viewModel.completePermissionOnboarding(
                                context = context,
                                notificationsGranted = notifGranted,
                                coarseLocationGranted = locGranted
                            ) { detectedCity ->
                                if (!detectedCity.isNullOrBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Location enabled around $detectedCity.")
                                    }
                                }
                            }
                        }
                    )
                }
            }
            return
        }
    }

    val hideBottomNav = (currentTab == CoM8Tab.MATCHES && activeChatListingId != null) ||
        (currentTab == CoM8Tab.PROFILE && showSettings)

    val unreadMatchesCount = likedMatches.count { it.unreadCount > 0 }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLargeScreen = maxWidth >= 760.dp

            Scaffold(
                containerColor = tokens.background,
                contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    AnimatedVisibility(
                        visible = !isLargeScreen && !hideBottomNav,
                        enter = expandVertically(
                            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Top
                        ) + fadeIn(tween(durationMillis = 200)),
                        exit = shrinkVertically(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(tween(durationMillis = 160))
                    ) {
                        Column(modifier = Modifier.background(tokens.surface)) {
                            HorizontalDivider(color = tokens.border, thickness = 1.dp)
                            NavigationBar(
                                containerColor = tokens.surface,
                                tonalElevation = 0.dp
                            ) {
                                CoM8Tab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            showSettings = false
                                            if (tab != CoM8Tab.MATCHES) {
                                                viewModel.closeMatchChat()
                                            }
                                            currentTab = tab
                                        },
                                        modifier = Modifier.testTag(tab.tag),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = tokens.successFg,
                                            selectedTextColor = tokens.successFg,
                                            unselectedIconColor = tokens.onSurfaceMuted,
                                            unselectedTextColor = tokens.onSurfaceMuted,
                                            indicatorColor = Color.Transparent
                                        ),
                                        icon = {
                                            if (tab == CoM8Tab.MATCHES && unreadMatchesCount > 0) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge(containerColor = tokens.primary) {
                                                            Text("$unreadMatchesCount", color = Color.White)
                                                        }
                                                    }
                                                ) {
                                                    Icon(tab.icon, contentDescription = tab.label)
                                                }
                                            } else {
                                                Icon(tab.icon, contentDescription = tab.label)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(tokens.background)
                ) {
                    if (isLargeScreen) {
                        NavigationRail(
                            containerColor = tokens.surface,
                            modifier = Modifier
                                .fillMaxHeight()
                                .border(1.dp, tokens.border, RoundedCornerShape(0.dp)),
                            header = {
                                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                                    CoM8Wordmark(compact = true)
                                }
                            }
                        ) {
                            CoM8Tab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        showSettings = false
                                        if (tab != CoM8Tab.MATCHES) viewModel.closeMatchChat()
                                        currentTab = tab
                                    },
                                    modifier = Modifier.testTag(tab.tag),
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = tokens.successFg,
                                        selectedTextColor = tokens.successFg,
                                        unselectedIconColor = tokens.onSurfaceMuted,
                                        unselectedTextColor = tokens.onSurfaceMuted,
                                        indicatorColor = tokens.successBg
                                    ),
                                    icon = {
                                        if (tab == CoM8Tab.MATCHES && unreadMatchesCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = tokens.primary) {
                                                        Text("$unreadMatchesCount", color = Color.White)
                                                    }
                                                }
                                            ) {
                                                Icon(tab.icon, contentDescription = tab.label)
                                            }
                                        } else {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = currentTab,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        transitionSpec = {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(170))
                        },
                        label = "tab_content_transition"
                    ) { activeTab ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            when (activeTab) {
                                CoM8Tab.DISCOVER -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 0.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Discover",
                                            fontFamily = com.example.ui.theme.SpaceGroteskFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            color = tokens.onSurface
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .clickable { showDiscoverFilterSheet = true }
                                                .testTag("discover_filter_button"),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(44.dp),
                                                shape = CircleShape,
                                                color = tokens.surface,
                                                border = BorderStroke(1.dp, tokens.border)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Tune,
                                                        contentDescription = "Filter flatmates",
                                                        tint = tokens.onSurface,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val focusedCandidate = deckCandidates.firstOrNull { it.listing.id == selectedCandidateId }
                                        ?: deckCandidates.firstOrNull()

                                    if (isLargeScreen && focusedCandidate != null) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .padding(end = 16.dp, bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            SwipeCardDeck(
                                                candidates = deckCandidates,
                                                totalAvailableCount = allMergedListings.size,
                                                onSwipe = { cand, liked ->
                                                    viewModel.swipeCandidate(cand, liked) {
                                                        viewModel.notifyLikedMatch(context, cand)
                                                        viewModel.openMatchChat(cand.listing.id)
                                                        currentTab = CoM8Tab.MATCHES
                                                    }
                                                },
                                                onInspectCandidate = { cand ->
                                                    viewModel.selectCandidate(cand.listing.id)
                                                    viewModel.requestAiCompatibilityReport(cand)
                                                },
                                                onResetPassed = { viewModel.resetPassedSwipes() },
                                                onOpenFilters = { showDiscoverFilterSheet = true },
                                                onCreatePost = { currentTab = CoM8Tab.POST },
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1.05f)
                                                    .fillMaxHeight(),
                                                shape = RoundedCornerShape(24.dp),
                                                color = tokens.surface,
                                                border = BorderStroke(1.dp, tokens.border)
                                            ) {
                                                CandidateCompatibilityPane(
                                                    candidate = focusedCandidate,
                                                    userProfile = userProfile,
                                                    aiReportState = aiReportState,
                                                    onRequestAiReport = { viewModel.requestAiCompatibilityReport(it) },
                                                    onBack = { viewModel.selectCandidate(null) },
                                                    onPass = { viewModel.swipeCandidate(focusedCandidate, false) },
                                                    onLikeAndChat = { cand ->
                                                        viewModel.swipeCandidate(cand, true) {
                                                            viewModel.notifyLikedMatch(context, cand)
                                                            viewModel.openMatchChat(cand.listing.id)
                                                            currentTab = CoM8Tab.MATCHES
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        SwipeCardDeck(
                                            candidates = deckCandidates,
                                            totalAvailableCount = allMergedListings.size,
                                            onSwipe = { cand, liked ->
                                                viewModel.swipeCandidate(cand, liked) {
                                                    viewModel.notifyLikedMatch(context, cand)
                                                }
                                            },
                                            onInspectCandidate = { cand ->
                                                viewModel.selectCandidate(cand.listing.id)
                                                viewModel.requestAiCompatibilityReport(cand)
                                                sheetCandidate = cand
                                            },
                                            onResetPassed = { viewModel.resetPassedSwipes() },
                                            onOpenFilters = { showDiscoverFilterSheet = true },
                                            onCreatePost = { currentTab = CoM8Tab.POST },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                CoM8Tab.MATCHES -> {
                                    MatchesAndChatScreen(
                                        matches = likedMatches,
                                        allCandidates = allMergedListings,
                                        userProfile = userProfile,
                                        activeChatListingId = activeChatListingId,
                                        chatMessagesState = activeChatMessages,
                                        onOpenChat = { viewModel.openMatchChat(it) },
                                        onCloseChat = { viewModel.closeMatchChat() },
                                        onSendMessage = { id, text -> viewModel.sendChatMessage(id, text) },
                                        onReportUser = { listingId, targetName, reason, details ->
                                            viewModel.reportChatUser(
                                                listingId = listingId,
                                                reportedName = targetName,
                                                reason = reason,
                                                details = details
                                            ) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Report submitted. Thank you for keeping CoM8 safe.")
                                                }
                                            }
                                        },
                                        onInspectCandidate = { cand ->
                                            viewModel.selectCandidate(cand.listing.id)
                                            viewModel.requestAiCompatibilityReport(cand)
                                            sheetCandidate = cand
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                CoM8Tab.POST -> {
                                    PostYourselfScreen(
                                        userProfile = userProfile,
                                        ownListing = ownListing,
                                        userSettings = userSettings,
                                        onUpdateSettings = { viewModel.saveUserSettings(it) },
                                        onPublishListing = { listing ->
                                            viewModel.publishOwnListing(listing) {
                                                currentTab = CoM8Tab.PROFILE
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                CoM8Tab.PROFILE -> {
                                    AnimatedContent(
                                        targetState = showSettings,
                                        modifier = Modifier.weight(1f),
                                        transitionSpec = {
                                            fadeIn(tween(220)) togetherWith fadeOut(tween(170))
                                        },
                                        label = "profile_settings_transition"
                                    ) { isSettings ->
                                        if (isSettings) {
                                            SettingsScreen(
                                                accountIdentifier = userProfile?.email?.ifBlank { activePhoneNumber } ?: activePhoneNumber,
                                                hasOwnListing = ownListing != null,
                                                settings = userSettings,
                                                themeMode = themeMode,
                                                onSelectThemeMode = { viewModel.setThemeMode(it) },
                                                onUpdateSettings = { viewModel.saveUserSettings(it) },
                                                onEditPost = {
                                                    showSettings = false
                                                    currentTab = CoM8Tab.POST
                                                },
                                                onDeletePost = {
                                                    viewModel.deleteOwnListing()
                                                },
                                                onSignOut = performFullSignOut,
                                                onBack = { showSettings = false },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            val currentFirebasePhone = Firebase.auth.currentUser?.phoneNumber
                                            val isPhoneVerified = phoneLinkedInSession ||
                                                !currentFirebasePhone.isNullOrBlank() ||
                                                userProfile?.authProvider == "PHONE"
                                            ProfileScreen(
                                                userProfile = userProfile,
                                                ownListing = ownListing,
                                                userSettings = userSettings,
                                                isPhoneVerified = isPhoneVerified,
                                                onPhoneLinked = { linkedNumber ->
                                                    phoneLinkedInSession = true
                                                    viewModel.markPhoneVerified(linkedNumber) {
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar("Phone verified and linked to your account.")
                                                        }
                                                    }
                                                },
                                                onEditQuiz = { editingQuiz = true },
                                                onNavigateToPost = { currentTab = CoM8Tab.POST },
                                                onOpenSettings = { showSettings = true },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full-screen in-window Match Detail overlay (preserves edge-to-edge WindowInsets cleanly)
        AnimatedVisibility(
            visible = sheetCandidate != null,
            enter = fadeIn() + slideInVertically { it / 10 },
            exit = fadeOut() + slideOutVertically { it / 10 }
        ) {
            val activeSheetItem = sheetCandidate
            if (activeSheetItem != null) {
                BackHandler { sheetCandidate = null }
                CandidateCompatibilityPane(
                    candidate = activeSheetItem,
                    userProfile = userProfile,
                    aiReportState = aiReportState,
                    onRequestAiReport = { viewModel.requestAiCompatibilityReport(it) },
                    onBack = { sheetCandidate = null },
                    onPass = {
                        sheetCandidate = null
                        viewModel.swipeCandidate(activeSheetItem, false)
                    },
                    onLikeAndChat = { cand ->
                        sheetCandidate = null
                        viewModel.swipeCandidate(cand, true) {
                            viewModel.notifyLikedMatch(context, cand)
                            viewModel.openMatchChat(cand.listing.id)
                            currentTab = CoM8Tab.MATCHES
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showDiscoverFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDiscoverFilterSheet = false },
            sheetState = sheetState,
            containerColor = tokens.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Discover filters",
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "City",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoM8Catalog.supportedCities.forEach { city ->
                            FilterChip(
                                selected = filterState.selectedCity == city,
                                onClick = { viewModel.updateCityFilter(city) },
                                label = { Text(city) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tokens.successBg,
                                    selectedLabelColor = tokens.successFg
                                )
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Who to show",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterState.selectedRole == null,
                            onClick = { viewModel.updateRoleFilter(null) },
                            label = { Text("Everyone") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tokens.successBg,
                                selectedLabelColor = tokens.successFg
                            )
                        )
                        listOf(RoleType.SEEKING_FLAT, RoleType.HAS_FLAT).forEach { role ->
                            FilterChip(
                                selected = filterState.selectedRole == role.code,
                                onClick = { viewModel.updateRoleFilter(role.code) },
                                label = { Text(role.badge) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tokens.successBg,
                                    selectedLabelColor = tokens.successFg
                                )
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Gender",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterState.selectedGender == null,
                            onClick = { viewModel.updateGenderFilter(null) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tokens.successBg,
                                selectedLabelColor = tokens.successFg
                            )
                        )
                        CoM8Catalog.genderOptions.forEach { gender ->
                            FilterChip(
                                selected = filterState.selectedGender == gender,
                                onClick = { viewModel.updateGenderFilter(gender) },
                                label = { Text(gender) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tokens.successBg,
                                    selectedLabelColor = tokens.successFg
                                )
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Max rent",
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.onSurface
                        )
                        Text(
                            text = "₹${filterState.maxRent / 1000}k / month",
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.successFg
                        )
                    }
                    Slider(
                        value = filterState.maxRent.toFloat(),
                        onValueChange = { viewModel.updateMaxRentFilter(it.toLong()) },
                        valueRange = 5000f..50000f,
                        colors = SliderDefaults.colors(
                            thumbColor = tokens.primary,
                            activeTrackColor = tokens.primary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enforce smoking & drinking dealbreakers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    )
                    Switch(
                        checked = filterState.strictDealbreakers
                            ?: (userProfile?.smokingDrinkingDealbreaker != false),
                        onCheckedChange = { viewModel.updateDealbreakerFilter(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = tokens.primary
                        )
                    )
                }

                Button(
                    onClick = { showDiscoverFilterSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Apply filters", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
