package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.CoM8Catalog
import com.example.data.model.FlatmateListing
import com.example.data.model.FoodHabit
import com.example.data.model.GuestPolicy
import com.example.data.model.LocalityPreset
import com.example.data.model.RoleType
import com.example.data.model.SleepSchedule
import com.example.data.model.UserLifestyleProfile
import com.example.data.model.UserSettings
import com.example.ui.components.CircularBackButton
import com.example.ui.components.LeafletMapView
import com.example.ui.components.MapPinData
import com.example.ui.components.pressScaleClickable
import com.example.ui.theme.AmberCardInk
import com.example.ui.theme.CoM8ThemeTokens
import com.example.ui.theme.LavenderCardInk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PostYourselfScreen(
    userProfile: UserLifestyleProfile?,
    ownListing: FlatmateListing?,
    userSettings: UserSettings,
    onUpdateSettings: (UserSettings) -> Unit,
    onPublishListing: (FlatmateListing) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = CoM8ThemeTokens.colors
    val initialPreset = remember(ownListing, userProfile) {
        val targetLocality = ownListing?.locality
            ?: userProfile?.preferredAreas?.firstOrNull()
            ?: "Baner"
        val targetCity = ownListing?.city
            ?: userProfile?.preferredCities?.firstOrNull()
            ?: "Pune"
        CoM8Catalog.localityPresets.firstOrNull {
            it.locality.equals(targetLocality, ignoreCase = true)
        } ?: CoM8Catalog.localityPresets.firstOrNull {
            it.city.equals(targetCity, ignoreCase = true)
        } ?: CoM8Catalog.localityPresets.first()
    }

    var roleType by remember(ownListing) {
        mutableStateOf(ownListing?.roleType ?: RoleType.SEEKING_FLAT.code)
    }
    var selectedPreset by remember(initialPreset) { mutableStateOf(initialPreset) }
    var rentText by remember(ownListing) {
        mutableStateOf((ownListing?.monthlyRent ?: initialPreset.typicalRent).toString())
    }
    var roomsFreeText by remember(ownListing) {
        mutableStateOf(ownListing?.roomsFreeLabel ?: (userProfile?.roomType ?: "Private room"))
    }
    var availableFrom by remember(ownListing) {
        mutableStateOf(ownListing?.moveInTimeline?.removePrefix("From ") ?: "Now")
    }
    var bio by remember(ownListing, userProfile) {
        mutableStateOf(
            ownListing?.bio?.ifBlank { null }
                ?: "Looking for a considerate, tidy roommate with a similar weekday routine to share a calm home."
        )
    }
    var photoUris by remember(ownListing) {
        mutableStateOf(ownListing?.flatPhotoUris ?: emptyList())
    }
    var showLocalityModal by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var visibility by remember(userSettings.postVisibility) {
        mutableStateOf(userSettings.postVisibility)
    }

    LaunchedEffect(userSettings.postVisibility) {
        visibility = userSettings.postVisibility
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
    ) { uris ->
        if (uris.isNotEmpty()) {
            photoUris = (photoUris + uris.map { it.toString() }).distinct().take(8)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = tokens.primary,
        unfocusedBorderColor = tokens.border,
        focusedContainerColor = tokens.surface,
        unfocusedContainerColor = tokens.surface,
        focusedTextColor = tokens.onSurface,
        unfocusedTextColor = tokens.onSurface
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Find a flatmate",
                    style = MaterialTheme.typography.headlineMedium,
                    color = tokens.onSurface
                )
                Text(
                    text = "Share what kind of roommate you're looking for. Verified people who fit your lifestyle will see you in Discover.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.onSurfaceMuted
                )
            }

            // Segmented Toggle: Looking for a roommate | Team up for a place
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                color = tokens.surface,
                border = BorderStroke(1.dp, tokens.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    listOf(
                        RoleType.SEEKING_FLAT.code to "Looking for a roommate",
                        RoleType.HAS_FLAT.code to "Team up for a place"
                    ).forEach { (code, label) ->
                        val selected = roleType == code
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .pressScaleClickable { roleType = code },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) tokens.primary else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color.White else tokens.onSurfaceMuted
                                )
                            }
                        }
                    }
                }
            }

            // Space & vibe photos (Up to 8)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Space & vibe photos (optional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.onSurface
                    )
                    Text(
                        text = "Up to 8",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.onSurfaceMuted
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    if (photoUris.isNotEmpty()) {
                        itemsIndexed(photoUris) { idx, uri ->
                            Box(
                                modifier = Modifier
                                    .size(width = 104.dp, height = 104.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(tokens.pastelMint)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Flat photo ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (idx == 0) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp),
                                        shape = RoundedCornerShape(999.dp),
                                        color = tokens.surface
                                    ) {
                                        Text(
                                            text = "Cover",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.onSurface
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            photoUris = photoUris.filterIndexed { index, _ -> index != idx }
                                        },
                                    shape = CircleShape,
                                    color = tokens.primaryDark.copy(alpha = 0.78f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Remove photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val defaultSlots = listOf(
                            Triple("Cover", tokens.pastelMint, Icons.Outlined.Chair),
                            Triple("Room", tokens.pastelAmber, Icons.Outlined.Bed),
                            Triple("Kitchen", tokens.pastelLavender, Icons.Outlined.Kitchen)
                        )
                        itemsIndexed(defaultSlots) { idx, (label, bg, icon) ->
                            val iconTint = when (idx) {
                                0 -> tokens.successFg
                                1 -> if (tokens.isDark) tokens.accentAmber else AmberCardInk
                                else -> if (tokens.isDark) tokens.onSurface else LavenderCardInk
                            }
                            Box(
                                modifier = Modifier
                                    .size(width = 104.dp, height = 104.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bg)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = iconTint,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.Center)
                                )
                                if (idx == 0) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp),
                                        shape = RoundedCornerShape(999.dp),
                                        color = tokens.surface
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (photoUris.size < 8) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .size(width = 104.dp, height = 104.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("add_flat_photos_button"),
                                shape = RoundedCornerShape(16.dp),
                                color = tokens.surface,
                                border = BorderStroke(1.2.dp, tokens.border)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "Add flat photo",
                                        tint = tokens.onSurfaceMuted
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = tokens.onSurfaceMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Target neighborhood + Map Box with Move pin
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Target neighborhood",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.onSurface
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showLocalityModal = true },
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.surface,
                    border = BorderStroke(1.dp, tokens.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = tokens.successFg,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${selectedPreset.locality}, ${selectedPreset.city}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = tokens.onSurface
                            )
                        }
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelLarge,
                            color = tokens.successFg
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = tokens.surface,
                    border = BorderStroke(1.dp, tokens.border)
                ) {
                    ApproximateLocationMapBox(
                        badgeText = "${selectedPreset.locality}, ${selectedPreset.city}",
                        actionButtonLabel = "Move pin",
                        onClick = { showLocalityModal = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(142.dp)
                    )
                }
            }

            // Rent per month + Rooms free
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Budget share / month",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.onSurface
                    )
                    OutlinedTextField(
                        value = rentText,
                        onValueChange = { rentText = it.filter { ch -> ch.isDigit() }.take(6) },
                        prefix = { Text("₹", color = tokens.onSurfaceMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_rent_input")
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Room preference",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.onSurface
                    )
                    OutlinedTextField(
                        value = roomsFreeText,
                        onValueChange = { roomsFreeText = it.take(24) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_rooms_input")
                    )
                }
            }

            // Available from chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Target move-in",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presetDates = listOf("Now", "1 Dec")
                    presetDates.forEach { label ->
                        val selected = availableFrom == label
                        PostChip(
                            label = label,
                            selected = selected,
                            onClick = { availableFrom = label }
                        )
                    }
                    val isCustomDate = availableFrom != "Now" && availableFrom != "1 Dec"
                    PostChip(
                        label = if (isCustomDate) availableFrom else "Pick a date",
                        selected = isCustomDate,
                        onClick = { showDatePicker = true }
                    )
                }
            }

            // Roommate preference bio
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "What kind of roommate are you looking for?",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.onSurface
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(360) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_bio_input")
                )
            }

            // Post settings card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Post settings",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.onSurface
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = tokens.surface,
                    border = BorderStroke(1.dp, tokens.border)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val options = listOf("Verified people", "Only matches", "Hidden")
                                    val next = options[(options.indexOf(visibility) + 1) % options.size]
                                    visibility = next
                                    onUpdateSettings(userSettings.copy(postVisibility = next))
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Who can see my post",
                                style = MaterialTheme.typography.bodyLarge,
                                color = tokens.onSurface
                            )
                            Text(
                                text = visibility,
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.successFg
                            )
                        }
                        HorizontalDivider(color = tokens.border)
                        PostSettingSwitchRow(
                            label = "Hide exact address",
                            checked = userSettings.hideExactAddress,
                            onCheckedChange = {
                                onUpdateSettings(userSettings.copy(hideExactAddress = it))
                            }
                        )
                        HorizontalDivider(color = tokens.border)
                        PostSettingSwitchRow(
                            label = "Messages only after matching",
                            checked = userSettings.messagesOnlyAfterMatching,
                            onCheckedChange = {
                                onUpdateSettings(userSettings.copy(messagesOnlyAfterMatching = it))
                            }
                        )
                        HorizontalDivider(color = tokens.border)
                        PostSettingSwitchRow(
                            label = "Pause after 30 days",
                            checked = userSettings.pauseAfter30Days,
                            onCheckedChange = {
                                onUpdateSettings(userSettings.copy(pauseAfter30Days = it))
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val parsedRent = (rentText.toLongOrNull() ?: selectedPreset.typicalRent)
                        .coerceIn(2500L, 150000L)
                    val roleBadge = if (roleType == RoleType.HAS_FLAT.code) {
                        "Team up · ${selectedPreset.locality}"
                    } else {
                        "Looking for a roommate · ${selectedPreset.locality}"
                    }
                    onPublishListing(
                        FlatmateListing(
                            name = userProfile?.displayName?.ifBlank { "CoM8 Member" } ?: "CoM8 Member",
                            age = (userProfile?.age ?: 24).toLong(),
                            gender = userProfile?.gender ?: "He",
                            occupation = userProfile?.occupationCategory ?: "Working professional",
                            organizationName = userProfile?.organizationName.orEmpty(),
                            roleType = roleType,
                            roleSummaryBadge = roleBadge,
                            flatTypeLabel = roomsFreeText.ifBlank { userProfile?.roomType ?: "Private room" },
                            roomsFreeLabel = if (availableFrom == "Now") "Immediate" else "From $availableFrom",
                            city = selectedPreset.city,
                            locality = selectedPreset.locality,
                            monthlyRent = parsedRent,
                            rentRangeLabel = "₹${(parsedRent / 1000).coerceAtLeast(3)}k per month",
                            latitude = selectedPreset.latitude,
                            longitude = selectedPreset.longitude,
                            smoking = userProfile?.smoking ?: "Never",
                            drinking = userProfile?.drinking ?: "Never",
                            sleepSchedule = userProfile?.sleepSchedule ?: SleepSchedule.EARLY_BIRD.code,
                            cleanlinessLevel = userProfile?.cleanlinessLevel ?: 4L,
                            cleanlinessTag = userProfile?.cleanlinessTag ?: "Tidy",
                            guestPolicy = userProfile?.guestPolicy ?: GuestPolicy.WEEKENDS_OK.code,
                            foodHabit = userProfile?.foodHabit ?: FoodHabit.VEG_ONLY.code,
                            bio = bio.trim().ifBlank { "Looking for a tidy, easygoing flatmate." },
                            moveInTimeline = if (availableFrom == "Now") "Immediate" else "From $availableFrom",
                            flatPhotoUris = photoUris,
                            visibility = visibility,
                            hideExactAddress = userSettings.hideExactAddress,
                            messagesOnlyAfterMatch = userSettings.messagesOnlyAfterMatching,
                            showPhoneNumber = userSettings.showPhoneNumber,
                            pauseAfter30Days = userSettings.pauseAfter30Days,
                            isLive = true
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("publish_listing_submit"),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (ownListing != null) "Update post" else "Publish post",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLocalityModal) {
        LocalityPinPickerDialog(
            currentPreset = selectedPreset,
            onSelectPreset = { preset ->
                selectedPreset = preset
                if (ownListing == null) {
                    rentText = preset.typicalRent.toString()
                }
                showLocalityModal = false
            },
            onDismiss = { showLocalityModal = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            availableFrom = SimpleDateFormat("d MMM", Locale.getDefault())
                                .format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Set date", color = tokens.successFg)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = tokens.onSurfaceMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun PostChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    Surface(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .pressScaleClickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) tokens.primary else tokens.surface,
        border = if (selected) null else BorderStroke(1.dp, tokens.border)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else tokens.onSurface
            )
        }
    }
}

@Composable
private fun PostSettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = tokens.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = tokens.border
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalityPinPickerDialog(
    currentPreset: LocalityPreset,
    onSelectPreset: (LocalityPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    var picked by remember(currentPreset) { mutableStateOf(currentPreset) }
    val previewListing = remember(picked) {
        FlatmateListing(
            id = "pin_preview_${picked.locality}",
            name = "Your flat",
            roleSummaryBadge = "Selected area",
            city = picked.city,
            locality = picked.locality,
            monthlyRent = picked.typicalRent,
            latitude = picked.latitude,
            longitude = picked.longitude
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = tokens.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Move locality pin",
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.onSurface
                        )
                        Text(
                            text = "${picked.locality}, ${picked.city}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.successFg
                        )
                    }
                    CircularBackButton(onClick = onDismiss)
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoM8Catalog.localityPresets.forEach { preset ->
                        val active = preset == picked
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { picked = preset },
                            shape = RoundedCornerShape(999.dp),
                            color = if (active) tokens.successBg else tokens.surface,
                            border = BorderStroke(
                                1.dp,
                                if (active) tokens.primary else tokens.border
                            )
                        ) {
                            Text(
                                text = "${preset.locality} · ${preset.city}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) tokens.successFg else tokens.onSurface
                            )
                        }
                    }
                }

                LeafletMapView(
                    pins = listOf(MapPinData(previewListing, 92)),
                    selectedListingId = previewListing.id,
                    onMarkerClick = {},
                    zoomLevel = 14,
                    interactive = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Button(
                    onClick = { onSelectPreset(picked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Use ${picked.locality}, ${picked.city}",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
