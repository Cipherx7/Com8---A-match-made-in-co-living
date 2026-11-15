package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CoM8Catalog
import com.example.data.model.FoodHabit
import com.example.data.model.GuestPolicy
import com.example.data.model.RoleType
import com.example.data.model.SleepSchedule
import com.example.data.model.UserLifestyleProfile
import com.example.ui.components.CircularBackButton
import com.example.ui.components.pressScaleClickable
import com.example.ui.theme.CoM8ThemeTokens
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LifestyleQuizForm(
    initialProfile: UserLifestyleProfile?,
    defaultName: String,
    phoneNumberDisplay: String = "+91 98765 43210",
    isPhoneVerified: Boolean = true,
    onPhoneLinked: ((String) -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onSaveQuiz: (UserLifestyleProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }

    // Step 1: Basics
    var displayName by remember(initialProfile) {
        val current = initialProfile?.displayName.orEmpty()
        mutableStateOf(if (current.startsWith("CoM8 ")) "" else current)
    }
    var age by remember(initialProfile) {
        mutableIntStateOf(initialProfile?.age ?: 23)
    }
    var gender by remember(initialProfile) {
        mutableStateOf(initialProfile?.gender?.ifBlank { "He" } ?: "He")
    }
    var occupationCategory by remember(initialProfile) {
        mutableStateOf(
            initialProfile?.occupationCategory?.ifBlank { "Working professional" }
                ?: "Working professional"
        )
    }
    var organizationName by remember(initialProfile) {
        mutableStateOf(initialProfile?.organizationName.orEmpty())
    }
    var targetCity by remember(initialProfile) {
        mutableStateOf(initialProfile?.city?.ifBlank { "Pune, Maharashtra" } ?: "Pune, Maharashtra")
    }

    // Step 2: Living Style
    var smoking by remember(initialProfile) { mutableStateOf(initialProfile?.smoking ?: "Never") }
    var drinking by remember(initialProfile) { mutableStateOf(initialProfile?.drinking ?: "Never") }
    var sleepSchedule by remember(initialProfile) {
        mutableStateOf(initialProfile?.sleepSchedule ?: SleepSchedule.EARLY_BIRD.code)
    }
    var cleanlinessTag by remember(initialProfile) {
        mutableStateOf(initialProfile?.cleanlinessTag ?: "Tidy")
    }
    var dealbreakerToggle by remember(initialProfile) {
        mutableStateOf(initialProfile?.smokingDrinkingDealbreaker ?: true)
    }

    // Step 3: Budget & Area
    var budgetRange by remember(initialProfile) {
        val min = (initialProfile?.budgetMin ?: 8000L).toFloat().coerceIn(5000f, 38000f)
        val max = (initialProfile?.budgetMax ?: 12000L).toFloat().coerceIn(min + 2000f, 40000f)
        mutableStateOf(min..max)
    }
    var preferredAreas by remember(initialProfile) {
        mutableStateOf(
            initialProfile?.preferredAreas?.takeIf { it.isNotEmpty() }
                ?: listOf("Baner", "Aundh")
        )
    }
    var moveInTimeline by remember(initialProfile) {
        mutableStateOf(initialProfile?.moveInTimeline ?: "Within 2 weeks")
    }
    var roomType by remember(initialProfile) {
        mutableStateOf(initialProfile?.roomType ?: "Private room")
    }

    // Step 4: About You
    var photoUri by remember(initialProfile) { mutableStateOf(initialProfile?.photoUri.orEmpty()) }
    var bio by remember(initialProfile) { mutableStateOf(initialProfile?.bio.orEmpty()) }
    var interests by remember(initialProfile) {
        mutableStateOf(
            initialProfile?.interests?.takeIf { it.isNotEmpty() }
                ?: listOf("Startups", "Fitness")
        )
    }

    // Step 5: Verify
    var selfieVerified by remember(initialProfile) { mutableStateOf(initialProfile?.selfieVerified ?: false) }
    var idVerified by remember(initialProfile) { mutableStateOf(initialProfile?.idVerified ?: false) }
    var showSelfieDialog by remember { mutableStateOf(false) }
    var showPhoneLinkDialog by remember { mutableStateOf(false) }

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    val idPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            idVerified = true
        }
    }

    if (step > 1) {
        BackHandler { step -= 1 }
    } else if (onCancel != null) {
        BackHandler { onCancel() }
    }

    fun submitProfile() {
        val cleanCity = targetCity.substringBefore(',').trim().ifBlank { "Pune" }
        val cleanlinessNumeric = when (cleanlinessTag) {
            "Spotless" -> 5L
            "Tidy" -> 4L
            else -> 3L
        }
        onSaveQuiz(
            UserLifestyleProfile(
                displayName = displayName.trim().ifBlank { defaultName },
                age = age,
                gender = gender,
                occupationCategory = occupationCategory,
                organizationName = organizationName.trim(),
                photoUri = photoUri,
                currentResidence = initialProfile?.currentResidence?.ifBlank { "Nashik" } ?: "Nashik",
                preferredCities = listOf(cleanCity),
                city = cleanCity,
                preferredAreas = preferredAreas,
                roleType = initialProfile?.roleType ?: RoleType.SEEKING_FLAT.code,
                monthlyBudget = budgetRange.endInclusive.roundToInt().toLong(),
                budgetMin = budgetRange.start.roundToInt().toLong(),
                budgetMax = budgetRange.endInclusive.roundToInt().toLong(),
                smoking = smoking,
                drinking = drinking,
                smokingDrinkingDealbreaker = dealbreakerToggle,
                sleepSchedule = sleepSchedule,
                cleanlinessLevel = cleanlinessNumeric,
                cleanlinessTag = cleanlinessTag,
                guestPolicy = initialProfile?.guestPolicy ?: GuestPolicy.WEEKENDS_OK.code,
                foodHabit = initialProfile?.foodHabit ?: FoodHabit.VEG_ONLY.code,
                moveInTimeline = moveInTimeline,
                roomType = roomType,
                bio = bio.trim(),
                interests = interests,
                quizCompleted = true,
                verifiedBadge = isPhoneVerified || selfieVerified || idVerified,
                selfieVerified = selfieVerified,
                idVerified = idVerified,
                trustScore = when {
                    isPhoneVerified && idVerified -> 98L
                    isPhoneVerified || idVerified -> 95L
                    else -> 85L
                }
            )
        )
    }

    val tokens = CoM8ThemeTokens.colors
    val animatedProgress by animateFloatAsState(
        targetValue = step / 5f,
        animationSpec = tween(280),
        label = "setup_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Progress Bar Row (inline back + 6dp progress bar + "N of 5")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CircularBackButton(
                    onClick = {
                        if (step > 1) step -= 1 else onCancel?.invoke()
                    },
                    modifier = Modifier.testTag("setup_back_button")
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(tokens.border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(tokens.primary)
                    )
                }
                Text(
                    text = "$step of 5",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = tokens.onSurfaceMuted
                )
            }

            // Step Content (scrolls cleanly above keyboard without shifting header/bottom button)
            AnimatedContent(
                targetState = step,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                transitionSpec = {
                    if (targetState >= initialState) {
                        (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(tween(200)) { -it / 4 } + fadeOut(tween(150)))
                    } else {
                        (slideInHorizontally(tween(260)) { -it / 4 } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(tween(200)) { it / 4 } + fadeOut(tween(150)))
                    }
                },
                label = "setup_step_body"
            ) { activeStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(top = 24.dp, bottom = 16.dp)
                ) {
                    when (activeStep) {
                        1 -> SetupStepBasics(
                            displayName = displayName,
                            onNameChange = { displayName = it },
                            age = age,
                            onAgeChange = { age = it.coerceIn(18, 60) },
                            gender = gender,
                            onGenderChange = { gender = it },
                            occupationCategory = occupationCategory,
                            onOccupationChange = { occupationCategory = it },
                            organizationName = organizationName,
                            onOrganizationChange = { organizationName = it.take(60) },
                            targetCity = targetCity,
                            onCityChange = { newCity ->
                                targetCity = newCity
                                val defaultAreas = CoM8Catalog.areasForCity(newCity).take(2)
                                preferredAreas = defaultAreas
                            }
                        )
                        2 -> SetupStepLivingStyle(
                            smoking = smoking,
                            onSmokingChange = { smoking = it },
                            drinking = drinking,
                            onDrinkingChange = { drinking = it },
                            sleepSchedule = sleepSchedule,
                            onSleepChange = { sleepSchedule = it },
                            cleanlinessTag = cleanlinessTag,
                            onCleanlinessChange = { cleanlinessTag = it },
                            dealbreakerToggle = dealbreakerToggle,
                            onDealbreakerChange = { dealbreakerToggle = it }
                        )
                        3 -> SetupStepBudgetAndArea(
                            targetCity = targetCity,
                            budgetRange = budgetRange,
                            onBudgetChange = { budgetRange = it },
                            preferredAreas = preferredAreas,
                            onToggleArea = { area ->
                                preferredAreas = if (area in preferredAreas && preferredAreas.size > 1) {
                                    preferredAreas - area
                                } else if (area !in preferredAreas) {
                                    preferredAreas + area
                                } else preferredAreas
                            },
                            moveInTimeline = moveInTimeline,
                            onMoveInChange = { moveInTimeline = it },
                            roomType = roomType,
                            onRoomTypeChange = { roomType = it }
                        )
                        4 -> SetupStepAboutYou(
                            photoUri = photoUri,
                            onPickPhoto = {
                                profilePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            bio = bio,
                            onBioChange = { bio = it.take(200) },
                            interests = interests,
                            onToggleInterest = { item ->
                                interests = if (item in interests) interests - item else interests + item
                            },
                            onAddCustomInterest = { custom ->
                                val clean = custom.trim().take(28)
                                if (clean.isNotEmpty() && interests.none { it.equals(clean, ignoreCase = true) }) {
                                    interests = interests + clean
                                }
                            }
                        )
                        5 -> SetupStepVerify(
                            phoneDisplay = phoneNumberDisplay,
                            isPhoneVerified = isPhoneVerified,
                            selfieVerified = selfieVerified,
                            idVerified = idVerified,
                            onVerifyPhone = { showPhoneLinkDialog = true },
                            onStartSelfie = { showSelfieDialog = true },
                            onAddId = {
                                idPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                }
            }

            // Bottom Pinned Action Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val canProceed = when (step) {
                    1 -> displayName.trim().isNotEmpty()
                    else -> true
                }
                Button(
                    onClick = {
                        if (step < 5) step += 1 else submitProfile()
                    },
                    enabled = canProceed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag(if (step < 5) "onboarding_next_button" else "save_quiz_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.primary,
                        contentColor = Color.White,
                        disabledContainerColor = tokens.primary.copy(alpha = 0.45f),
                        disabledContentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (step < 5) "Continue" else "Finish setup",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                if (step == 5) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { submitProfile() }
                            .padding(horizontal = 20.dp)
                            .testTag("skip_verify_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Skip for now",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = tokens.onSurfaceMuted
                        )
                    }
                }
            }
        }
    }

    if (showSelfieDialog) {
        AlertDialog(
            onDismissRequest = { showSelfieDialog = false },
            containerColor = tokens.surface,
            title = {
                Text(
                    text = "Quick Selfie Check",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = tokens.onSurface
                )
            },
            text = {
                Text(
                    text = "Confirm your selfie check to display a Verified badge on your CoM8 profile.",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = tokens.onSurfaceMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selfieVerified = true
                        showSelfieDialog = false
                    },
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary)
                ) {
                    Text("Complete Check", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelfieDialog = false }) {
                    Text("Cancel", fontFamily = InterFontFamily, color = tokens.onSurfaceMuted)
                }
            }
        )
    }

    if (showPhoneLinkDialog) {
        VerifyAndLinkPhoneDialog(
            onDismiss = { showPhoneLinkDialog = false },
            onLinked = { linkedNumber ->
                showPhoneLinkDialog = false
                onPhoneLinked?.invoke(linkedNumber)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupStepBasics(
    displayName: String,
    onNameChange: (String) -> Unit,
    age: Int,
    onAgeChange: (Int) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    occupationCategory: String,
    onOccupationChange: (String) -> Unit,
    organizationName: String,
    onOrganizationChange: (String) -> Unit,
    targetCity: String,
    onCityChange: (String) -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    SetupHeader(
        title = "Tell us the basics",
        subtitle = "This is what future flatmates see first."
    )

    Spacer(modifier = Modifier.height(22.dp))

    FieldBlock(label = "First name") {
        RoundedTextBox(
            value = displayName,
            onValueChange = onNameChange,
            placeholder = "Abhishek",
            modifier = Modifier.testTag("quiz_name_input")
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    FieldBlock(label = "Age") {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            color = tokens.surface,
            border = BorderStroke(1.dp, tokens.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = age.toString(),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = tokens.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tokens.neutralChipBg
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = "Increase age",
                            tint = tokens.onSurfaceMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onAgeChange(age + 1) }
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Decrease age",
                            tint = tokens.onSurfaceMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onAgeChange(age - 1) }
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    FieldBlock(label = "Gender") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CoM8Catalog.genderOptions.forEach { option ->
                PillSelectChip(
                    text = option,
                    selected = gender == option,
                    onClick = { onGenderChange(option) },
                    modifier = Modifier.testTag("gender_chip_${option.lowercase()}")
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    FieldBlock(label = "I am a") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Working professional", "Student", "Other").forEach { option ->
                    PillSelectChip(
                        text = option,
                        selected = occupationCategory == option,
                        onClick = { onOccupationChange(option) }
                    )
                }
            }

            AnimatedVisibility(
                visible = occupationCategory == "Working professional" || occupationCategory == "Student",
                enter = expandVertically(tween(220)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(150))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val placeholder = if (occupationCategory == "Working professional") {
                        "Company name (optional)"
                    } else {
                        "College name (optional)"
                    }
                    RoundedTextBox(
                        value = organizationName,
                        onValueChange = onOrganizationChange,
                        placeholder = placeholder,
                        modifier = Modifier.testTag("organization_name_input")
                    )
                    Text(
                        text = "This will help match your peers better.",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = tokens.onSurfaceMuted
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    FieldBlock(label = "City I'm moving to") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundedTextBox(
                value = targetCity,
                onValueChange = onCityChange,
                placeholder = "Pune, Maharashtra",
                modifier = Modifier.testTag("quiz_city_input")
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoM8Catalog.selectableCities.forEach { city ->
                    PillSelectChip(
                        text = city,
                        selected = targetCity.startsWith(city, ignoreCase = true),
                        onClick = { onCityChange(city) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupStepLivingStyle(
    smoking: String,
    onSmokingChange: (String) -> Unit,
    drinking: String,
    onDrinkingChange: (String) -> Unit,
    sleepSchedule: String,
    onSleepChange: (String) -> Unit,
    cleanlinessTag: String,
    onCleanlinessChange: (String) -> Unit,
    dealbreakerToggle: Boolean,
    onDealbreakerChange: (Boolean) -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    SetupHeader(
        title = "How do you live day to day?",
        subtitle = "We only show people whose answers fit yours."
    )

    Spacer(modifier = Modifier.height(20.dp))

    FieldBlock(label = "Smoking") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Never", "Socially", "Regularly").forEach { opt ->
                PillSelectChip(text = opt, selected = smoking == opt, onClick = { onSmokingChange(opt) })
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    FieldBlock(label = "Drinking") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Never", "Socially", "Regularly").forEach { opt ->
                PillSelectChip(text = opt, selected = drinking == opt, onClick = { onDrinkingChange(opt) })
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    FieldBlock(label = "Sleep schedule") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                SleepSchedule.EARLY_BIRD.code to "Early riser",
                SleepSchedule.NIGHT_OWL.code to "Night owl",
                SleepSchedule.BALANCED.code to "Flexible"
            ).forEach { (code, label) ->
                PillSelectChip(text = label, selected = sleepSchedule == code, onClick = { onSleepChange(code) })
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    FieldBlock(label = "Cleanliness") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Spotless", "Tidy", "Relaxed").forEach { opt ->
                PillSelectChip(text = opt, selected = cleanlinessTag == opt, onClick = { onCleanlinessChange(opt) })
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Dealbreaker toggle card (padding 14x16, radius 16, switch track 48x28, knob 22x22)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                role = Role.Switch,
                onClick = { onDealbreakerChange(!dealbreakerToggle) }
            ),
        shape = RoundedCornerShape(16.dp),
        color = tokens.surface,
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Treat smoking and drinking as dealbreakers",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = tokens.onSurface
                )
                Text(
                    text = "Hide anyone who answers differently.",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = tokens.onSurfaceMuted
                )
            }
            CoM8PillSwitch(
                checked = dealbreakerToggle,
                onCheckedChange = onDealbreakerChange
            )
        }
    }
}

@Composable
private fun CoM8PillSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    val trackColor by animateColorAsState(
        targetValue = if (checked) tokens.primary else tokens.border,
        animationSpec = tween(200),
        label = "switch_track"
    )
    // Track 48x28, knob 22x22, 3dp offset from edges => 3dp..23dp
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 23.dp else 3.dp,
        animationSpec = tween(200),
        label = "switch_knob"
    )
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(trackColor),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupStepBudgetAndArea(
    targetCity: String,
    budgetRange: ClosedFloatingPointRange<Float>,
    onBudgetChange: (ClosedFloatingPointRange<Float>) -> Unit,
    preferredAreas: List<String>,
    onToggleArea: (String) -> Unit,
    moveInTimeline: String,
    onMoveInChange: (String) -> Unit,
    roomType: String,
    onRoomTypeChange: (String) -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    val cleanCity = targetCity.substringBefore(',').trim().ifBlank { "Pune" }
    val areaOptions = CoM8Catalog.areasForCity(cleanCity)

    SetupHeader(
        title = "Where and how much?",
        subtitle = "Budget and area keep your matches realistic."
    )

    Spacer(modifier = Modifier.height(22.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Rent budget per month",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = tokens.onSurface
        )
        Text(
            text = "₹${formatRupees(budgetRange.start.roundToInt())} to ₹${formatRupees(budgetRange.endInclusive.roundToInt())}",
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = tokens.onSurface
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    CoM8BudgetRangeSlider(
        value = budgetRange,
        onValueChange = onBudgetChange,
        minLimit = 5000f,
        maxLimit = 40000f,
        step = 500f,
        minGap = 2000f
    )

    Spacer(modifier = Modifier.height(22.dp))

    FieldBlock(label = "Preferred areas in $cleanCity") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            areaOptions.forEach { area ->
                PillSelectChip(
                    text = area,
                    selected = area in preferredAreas,
                    onClick = { onToggleArea(area) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    FieldBlock(label = "Move-in") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Within 2 weeks", "Next month", "Flexible").forEach { opt ->
                PillSelectChip(text = opt, selected = moveInTimeline == opt, onClick = { onMoveInChange(opt) })
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    FieldBlock(label = "Room type") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Private room", "Shared room").forEach { opt ->
                PillSelectChip(text = opt, selected = roomType == opt, onClick = { onRoomTypeChange(opt) })
            }
        }
    }
}

@Composable
private fun CoM8BudgetRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    minLimit: Float,
    maxLimit: Float,
    step: Float,
    minGap: Float
) {
    val tokens = CoM8ThemeTokens.colors
    val density = LocalDensity.current
    val knobSize = 28.dp
    val hitSize = 48.dp
    val totalSpan = (maxLimit - minLimit).coerceAtLeast(1f)

    fun snap(raw: Float): Float {
        val steps = ((raw - minLimit) / step).roundToInt()
        return (minLimit + steps * step).coerceIn(minLimit, maxLimit)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val knobPx = with(density) { knobSize.toPx() }
        val hitPx = with(density) { hitSize.toPx() }
        val usableWidthPx = (constraints.maxWidth.toFloat() - knobPx).coerceAtLeast(1f)

        val startFrac = ((value.start - minLimit) / totalSpan).coerceIn(0f, 1f)
        val endFrac = ((value.endInclusive - minLimit) / totalSpan).coerceIn(startFrac, 1f)
        val startX = startFrac * usableWidthPx
        val endX = endFrac * usableWidthPx

        // Track (6dp height, 3dp radius)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = knobSize / 2)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(tokens.border)
                .pointerInput(value) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / usableWidthPx).coerceIn(0f, 1f)
                        val tappedVal = snap(minLimit + frac * totalSpan)
                        val distStart = abs(tappedVal - value.start)
                        val distEnd = abs(tappedVal - value.endInclusive)
                        if (distStart <= distEnd) {
                            onValueChange(tappedVal.coerceAtMost(value.endInclusive - minGap)..value.endInclusive)
                        } else {
                            onValueChange(value.start..tappedVal.coerceAtLeast(value.start + minGap))
                        }
                    }
                }
        ) {
            // Active segment
            Box(
                modifier = Modifier
                    .offset { IntOffset(startX.roundToInt(), 0) }
                    .width(with(density) { (endX - startX).coerceAtLeast(0f).toDp() })
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(tokens.primary)
            )
        }

        var dragStartVal by remember { mutableFloatStateOf(value.start) }
        var dragEndVal by remember { mutableFloatStateOf(value.endInclusive) }

        // Start knob (28x28 visible circle inside 48dp hit target)
        Box(
            modifier = Modifier
                .offset { IntOffset((startX + knobPx / 2f - hitPx / 2f).roundToInt(), 0) }
                .size(hitSize)
                .pointerInput(value.endInclusive) {
                    detectDragGestures(
                        onDragStart = { dragStartVal = value.start },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val deltaVal = (dragAmount.x / usableWidthPx) * totalSpan
                            dragStartVal = (dragStartVal + deltaVal)
                                .coerceIn(minLimit, value.endInclusive - minGap)
                            onValueChange(snap(dragStartVal).coerceAtMost(value.endInclusive - minGap)..value.endInclusive)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(knobSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, tokens.primary, CircleShape)
            )
        }

        // End knob (28x28 visible circle inside 48dp hit target)
        Box(
            modifier = Modifier
                .offset { IntOffset((endX + knobPx / 2f - hitPx / 2f).roundToInt(), 0) }
                .size(hitSize)
                .pointerInput(value.start) {
                    detectDragGestures(
                        onDragStart = { dragEndVal = value.endInclusive },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val deltaVal = (dragAmount.x / usableWidthPx) * totalSpan
                            dragEndVal = (dragEndVal + deltaVal)
                                .coerceIn(value.start + minGap, maxLimit)
                            onValueChange(value.start..snap(dragEndVal).coerceAtLeast(value.start + minGap))
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(knobSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, tokens.primary, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupStepAboutYou(
    photoUri: String,
    onPickPhoto: () -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    interests: List<String>,
    onToggleInterest: (String) -> Unit,
    onAddCustomInterest: (String) -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    var showAddInterestDialog by remember { mutableStateOf(false) }
    var customInterestInput by remember { mutableStateOf("") }
    var bioFocused by remember { mutableStateOf(false) }

    SetupHeader(
        title = "Show your side",
        subtitle = "A photo and a few lines help people say yes."
    )

    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPickPhoto)
            .testTag("profile_photo_picker"),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photoUri.isNotBlank()) {
                ProfileAvatarImage(
                    photoUri = photoUri,
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val dashColor = Color(0xFF7B8A88)
                val density = LocalDensity.current
                val strokePx = with(density) { 2.dp.toPx() }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = dashColor,
                        radius = (size.minDimension - strokePx) / 2f,
                        style = Stroke(
                            width = strokePx,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add a profile photo",
                    tint = tokens.onSurfaceMuted,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Add a profile photo",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = tokens.onSurface
            )
            Text(
                text = "A clear face photo. Only matches see it in full.",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = tokens.onSurfaceMuted
            )
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    FieldBlock(label = "About me") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                shape = RoundedCornerShape(16.dp),
                color = tokens.surface,
                border = BorderStroke(
                    width = if (bioFocused) 2.dp else 1.dp,
                    color = if (bioFocused) tokens.primary else tokens.border
                )
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    BasicTextField(
                        value = bio,
                        onValueChange = onBioChange,
                        cursorBrush = SolidColor(tokens.primary),
                        textStyle = TextStyle(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            lineHeight = 22.5.sp,
                            color = tokens.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 92.dp)
                            .onFocusChanged { bioFocused = it.isFocused }
                            .testTag("setup_bio_input")
                    )
                    if (bio.isEmpty()) {
                        Text(
                            text = "Tell flatmates about your routine, work, and home vibe...",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            lineHeight = 22.5.sp,
                            color = tokens.onSurfaceMuted.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Text(
                text = "${bio.length} / 200",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = tokens.onSurfaceMuted,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    FieldBlock(label = "Interests") {
        val displayedInterests = remember(interests) {
            (CoM8Catalog.interestOptions + interests).distinct()
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            displayedInterests.forEach { item ->
                PillSelectChip(
                    text = item,
                    selected = item in interests,
                    onClick = { onToggleInterest(item) }
                )
            }
            Surface(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .pressScaleClickable {
                        customInterestInput = ""
                        showAddInterestDialog = true
                    }
                    .testTag("add_custom_interest_chip"),
                shape = RoundedCornerShape(22.dp),
                color = tokens.successBg,
                border = BorderStroke(1.dp, tokens.primary.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add custom interest",
                        tint = tokens.successFg,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Add",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = tokens.successFg
                    )
                }
            }
        }
    }

    if (showAddInterestDialog) {
        AlertDialog(
            onDismissRequest = { showAddInterestDialog = false },
            containerColor = tokens.surface,
            title = {
                Text(
                    text = "Add custom interest",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = tokens.onSurface
                )
            },
            text = {
                RoundedTextBox(
                    value = customInterestInput,
                    onValueChange = { customInterestInput = it.take(28) },
                    placeholder = "e.g. Badminton, Photography, Chess",
                    modifier = Modifier.testTag("custom_interest_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customInterestInput.isNotBlank()) {
                            onAddCustomInterest(customInterestInput)
                        }
                        showAddInterestDialog = false
                    },
                    enabled = customInterestInput.isNotBlank(),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary)
                ) {
                    Text("Add interest", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddInterestDialog = false }) {
                    Text("Cancel", fontFamily = InterFontFamily, color = tokens.onSurfaceMuted)
                }
            }
        )
    }
}

@Composable
private fun SetupStepVerify(
    phoneDisplay: String,
    isPhoneVerified: Boolean,
    selfieVerified: Boolean,
    idVerified: Boolean,
    onVerifyPhone: () -> Unit,
    onStartSelfie: () -> Unit,
    onAddId: () -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    SetupHeader(
        title = "Get verified",
        subtitle = "Living with a stranger needs trust. Verified profiles are shown first and carry a badge."
    )

    Spacer(modifier = Modifier.height(22.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VerifyActionCard(
            icon = Icons.Outlined.PhoneAndroid,
            iconTint = if (isPhoneVerified) tokens.successFg else tokens.onSurface,
            iconBg = if (isPhoneVerified) tokens.successBg else tokens.neutralChipBg,
            title = "Phone number",
            subtitle = if (isPhoneVerified) {
                phoneDisplay.ifBlank { "+91 verified" }
            } else {
                "Verify to also sign in with Phone"
            },
            done = isPhoneVerified,
            actionLabel = "Verify",
            onAction = onVerifyPhone
        )

        VerifyActionCard(
            icon = Icons.Outlined.PersonOutline,
            iconTint = if (selfieVerified) tokens.successFg else tokens.onSurface,
            iconBg = if (selfieVerified) tokens.successBg else tokens.neutralChipBg,
            title = "Selfie check",
            subtitle = "Takes about 30 seconds",
            done = selfieVerified,
            actionLabel = "Start",
            onAction = onStartSelfie
        )

        VerifyActionCard(
            icon = Icons.Outlined.Badge,
            iconTint = if (idVerified) tokens.successFg else tokens.onSurface,
            iconBg = if (idVerified) tokens.successBg else tokens.neutralChipBg,
            title = "Government ID",
            subtitle = "Optional. Never shown to others.",
            done = idVerified,
            actionLabel = "Add",
            onAction = onAddId
        )
    }
}

@Composable
private fun VerifyActionCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = tokens.surface,
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = tokens.onSurface
                )
                Text(
                    text = subtitle,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = tokens.onSurfaceMuted
                )
            }
            if (done) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Done",
                        tint = tokens.successFg,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Done",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = tokens.successFg
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.heightIn(min = 44.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, tokens.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = tokens.surface,
                        contentColor = tokens.primary
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupHeader(title: String, subtitle: String) {
    val tokens = CoM8ThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.3).sp,
            color = tokens.onSurface
        )
        Text(
            text = subtitle,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.5.sp,
            color = tokens.onSurfaceMuted
        )
    }
}

@Composable
private fun FieldBlock(
    label: String,
    content: @Composable () -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = tokens.onSurface
        )
        content()
    }
}

@Composable
private fun RoundedTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val tokens = CoM8ThemeTokens.colors
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = tokens.surface,
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = if (focused) tokens.primary else tokens.border
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(tokens.primary),
                textStyle = TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = tokens.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = tokens.onSurfaceMuted.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun PillSelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = CoM8ThemeTokens.colors
    Surface(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .pressScaleClickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) tokens.primary else tokens.surface,
        border = BorderStroke(1.dp, if (selected) tokens.primary else tokens.border)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = InterFontFamily,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                fontSize = 14.sp,
                color = if (selected) Color.White else tokens.onSurface
            )
        }
    }
}

@Composable
fun ProfileAvatarImage(
    photoUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val presetResId = photoUri.removePrefix("preset:").toIntOrNull()
    if (presetResId != null && presetResId != 0) {
        Image(
            painter = painterResource(id = presetResId),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else if (photoUri.isNotBlank()) {
        AsyncImage(
            model = photoUri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = CoM8Catalog.presetAvatars.first()),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

private fun formatRupees(amount: Int): String {
    val s = amount.toString()
    return if (s.length > 3) {
        val last3 = s.takeLast(3)
        val rest = s.dropLast(3)
        "$rest,$last3"
    } else s
}
