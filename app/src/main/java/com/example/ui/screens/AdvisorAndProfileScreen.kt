package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.BuildConfig
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppThemeMode
import com.example.data.model.FlatmateListing
import com.example.data.model.UserLifestyleProfile
import com.example.data.model.UserSettings
import com.example.ui.auth.PhoneAuthManager
import com.example.ui.auth.PhoneAuthState
import com.example.ui.components.CircularBackButton
import com.example.ui.components.InitialAvatarCircle
import com.example.ui.theme.CoM8ThemeTokens
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserLifestyleProfile?,
    ownListing: FlatmateListing?,
    userSettings: UserSettings,
    isPhoneVerified: Boolean = true,
    onPhoneLinked: (String) -> Unit = {},
    onEditQuiz: () -> Unit,
    onNavigateToPost: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = CoM8ThemeTokens.colors
    val name = userProfile?.displayName?.ifBlank { "CoM8 Member" } ?: "CoM8 Member"
    val age = userProfile?.age ?: 23
    val targetCity = userProfile?.preferredCities?.firstOrNull() ?: userProfile?.city ?: "Pune"
    val fromCity = userProfile?.currentResidence?.ifBlank { "Nashik" } ?: "Nashik"
    val strength = userProfile?.profileStrengthPercent() ?: 80
    var showVerifyPhoneDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
        ) {
            // Header row: Profile + Circular Settings icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = tokens.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenSettings)
                        .testTag("open_settings_button"),
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
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = tokens.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Identity card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = tokens.surface,
                border = BorderStroke(1.dp, tokens.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatarBg = if (tokens.isDark) tokens.pastelMint else Color(0xFFCFE3DE)
                    val avatarFg = if (tokens.isDark) tokens.accentMint else tokens.primary
                    InitialAvatarCircle(
                        initial = name,
                        size = 72.dp,
                        fontSize = 30.sp,
                        backgroundColor = avatarBg,
                        textColor = avatarFg,
                        photoUri = userProfile?.photoUri.orEmpty()
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${name.substringBefore(' ')}, $age",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = tokens.onSurface
                        )
                        val subtitle = buildString {
                            append("Moving to $targetCity")
                            val org = userProfile?.organizationName?.trim().orEmpty()
                            if (org.isNotBlank()) {
                                append(" · $org")
                            } else {
                                append(" · from $fromCity")
                            }
                        }
                        Text(
                            text = subtitle,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = tokens.onSurfaceMuted
                        )
                        if (isPhoneVerified) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = "Phone verified",
                                    tint = tokens.successFg,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Phone verified",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = tokens.successFg
                                )
                            }
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PhoneAndroid,
                                        contentDescription = "Phone unverified",
                                        tint = tokens.warnFg,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Phone unverified",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = tokens.warnFg
                                    )
                                }
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { showVerifyPhoneDialog = true }
                                        .testTag("verify_phone_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = tokens.primary
                                ) {
                                    Text(
                                        text = "Verify",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dark "Post yourself" promo card (Horizontal Row: text + Create post CTA)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (tokens.isDark) tokens.surface else tokens.primaryDark,
                border = if (tokens.isDark) BorderStroke(1.dp, tokens.border) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (ownListing == null) "Post yourself" else "Your search is ${if (ownListing.isLive) "live" else "paused"}",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color(0xFFF5F7F6)
                        )
                        Text(
                            text = if (ownListing == null) {
                                "Let compatible flatmates find you first."
                            } else {
                                "${ownListing.locality}, ${ownListing.city} · ₹${formatProfileAmount(ownListing.monthlyRent.toInt())}/mo"
                            },
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = Color(0xFFB9C7C4)
                        )
                    }
                    Button(
                        onClick = onNavigateToPost,
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.accentMint,
                            contentColor = tokens.primaryDark
                        ),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("profile_create_post_button")
                    ) {
                        Text(
                            text = if (ownListing == null) "Create post" else "Edit post",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile strength card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = tokens.surface,
                border = BorderStroke(1.dp, tokens.border)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile strength",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = tokens.onSurface
                        )
                        Text(
                            text = "$strength%",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = tokens.successFg
                        )
                    }
                    LinearProgressIndicator(
                        progress = { strength / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = tokens.primary,
                        trackColor = tokens.border,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = if (strength >= 95 && isPhoneVerified) {
                            "Your profile is verified and ready for high-trust matches."
                        } else if (!isPhoneVerified) {
                            "Verify your phone number to unlock sign-in by phone and boost trust."
                        } else {
                            "Add a short bio and preferences to get stronger matches."
                        },
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = tokens.onSurfaceMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // My living style + Edit link
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My living style",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = tokens.onSurface
                    )
                    Text(
                        text = "Edit",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = tokens.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onEditQuiz)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("retake_quiz_button")
                    )
                }

                val tags = userProfile?.livingStyleTags() ?: listOf(
                    "Non-smoker",
                    "Doesn't drink",
                    "Early riser",
                    "Tidy",
                    targetCity
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEachIndexed { idx, tag ->
                        val isNeutral = idx == tags.lastIndex || idx == tags.lastIndex - 1
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isNeutral) tokens.neutralChipBg else tokens.successBg
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isNeutral) tokens.onSurface else tokens.successFg
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details card (3 rows)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = tokens.surface,
                border = BorderStroke(1.dp, tokens.border)
            ) {
                val budgetMin = ((userProfile?.budgetMin ?: 8000L) / 1000).coerceAtLeast(3)
                val budgetMax = ((userProfile?.budgetMax ?: 12000L) / 1000).coerceAtLeast(budgetMin + 1)
                Column {
                    ProfileDetailRow(
                        label = "Budget",
                        value = "₹${budgetMin}k to ₹${budgetMax}k per month"
                    )
                    HorizontalDivider(color = tokens.border)
                    ProfileDetailRow(
                        label = "Dealbreakers",
                        value = if (userProfile?.smokingDrinkingDealbreaker != false) {
                            "Smoking, drinking"
                        } else {
                            "Flexible"
                        }
                    )
                    HorizontalDivider(color = tokens.border)
                    ProfileDetailRow(
                        label = "Visibility",
                        value = userSettings.postVisibility
                    )
                }
            }
        }
    }

    if (showVerifyPhoneDialog) {
        VerifyAndLinkPhoneDialog(
            onDismiss = { showVerifyPhoneDialog = false },
            onLinked = { linkedNumber ->
                showVerifyPhoneDialog = false
                onPhoneLinked(linkedNumber)
            }
        )
    }
}

@Composable
internal fun VerifyAndLinkPhoneDialog(
    onDismiss: () -> Unit,
    onLinked: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = CoM8ThemeTokens.colors
    val phoneAuthManager = remember(scope) { PhoneAuthManager(scope) }
    val phoneAuthState by phoneAuthManager.uiState.collectAsStateWithLifecycle()

    var mobileDigits by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }

    val isLoading = phoneAuthState is PhoneAuthState.SendingCode ||
        phoneAuthState is PhoneAuthState.Verifying
    val errorMessage = (phoneAuthState as? PhoneAuthState.Error)?.message ?: localErrorMessage

    LaunchedEffect(phoneAuthState) {
        when (val state = phoneAuthState) {
            is PhoneAuthState.CodeSent -> {
                codeSent = true
                otpCode = ""
            }
            is PhoneAuthState.Verified -> {
                onLinked(state.phoneNumber.ifBlank { "+91$mobileDigits" })
            }
            else -> Unit
        }
    }

    fun sendCode() {
        val activity = context as? Activity
        if (activity == null) {
            localErrorMessage = "Unable to start SMS verification from this context."
            return
        }
        localErrorMessage = null
        phoneAuthManager.verifyPhoneNumber(
            activity = activity,
            phoneNumber = "+91$mobileDigits",
            linkToCurrentUser = true
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = tokens.surface,
        title = {
            Text(
                text = if (!codeSent) "Verify your phone number" else "Enter 6-digit code",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                color = tokens.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (!codeSent) {
                        "Link your +91 mobile number so you can also sign in using Phone OTP and access the exact same account."
                    } else {
                        "Enter the SMS code sent to +91 $mobileDigits."
                    },
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = tokens.onSurfaceMuted
                )

                if (!codeSent) {
                    OutlinedTextField(
                        value = mobileDigits,
                        onValueChange = { raw ->
                            mobileDigits = raw.filter { it.isDigit() }.take(10)
                            localErrorMessage = null
                            phoneAuthManager.clearError()
                        },
                        singleLine = true,
                        prefix = { Text("+91 ") },
                        placeholder = { Text("98765 43210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.primary,
                            unfocusedBorderColor = tokens.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("link_phone_input")
                    )
                } else {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { raw ->
                            otpCode = raw.filter { it.isDigit() }.take(6)
                            localErrorMessage = null
                            phoneAuthManager.clearError()
                        },
                        singleLine = true,
                        placeholder = { Text("6-digit code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.primary,
                            unfocusedBorderColor = tokens.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("link_otp_input")
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = tokens.dangerFg
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!codeSent) {
                        sendCode()
                    } else {
                        localErrorMessage = null
                        phoneAuthManager.linkWithCredential(otpCode)
                    }
                },
                enabled = !isLoading && (!codeSent && mobileDigits.length == 10 || codeSent && otpCode.length == 6),
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primary, contentColor = Color.White),
                shape = RoundedCornerShape(22.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (!codeSent) "Send code" else "Verify & link")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = tokens.onSurfaceMuted)
            }
        }
    )
}

@Composable
fun SettingsScreen(
    accountIdentifier: String,
    hasOwnListing: Boolean,
    settings: UserSettings,
    themeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onUpdateSettings: (UserSettings) -> Unit,
    onEditPost: () -> Unit,
    onDeletePost: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val tokens = CoM8ThemeTokens.colors
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CircularBackButton(onClick = onBack)
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = tokens.onSurface
                )
            }

            SettingsSection(title = "APPEARANCE") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val selected = themeMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { onSelectThemeMode(mode) }
                                .testTag("theme_mode_${mode.code.lowercase()}"),
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) tokens.primary else tokens.neutralChipBg
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color.White else tokens.onSurface
                                )
                            }
                        }
                    }
                }
            }

            SettingsSection(title = "MY POST") {
                SettingsSwitchRow(
                    title = "Post is live",
                    checked = settings.postIsLive,
                    onCheckedChange = { onUpdateSettings(settings.copy(postIsLive = it)) }
                )
                HorizontalDivider(color = tokens.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val options = listOf("Verified people", "Only matches", "Hidden")
                            val next = options[(options.indexOf(settings.postVisibility) + 1) % options.size]
                            onUpdateSettings(settings.copy(postVisibility = next))
                        }
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Who can see my post",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.onSurface
                    )
                    Text(
                        text = settings.postVisibility,
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.successFg
                    )
                }
                HorizontalDivider(color = tokens.border)
                SettingsSwitchRow(
                    title = "Hide exact address",
                    checked = settings.hideExactAddress,
                    onCheckedChange = { onUpdateSettings(settings.copy(hideExactAddress = it)) }
                )
                HorizontalDivider(color = tokens.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEditPost)
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit post",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.onSurface
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Edit post",
                        tint = tokens.onSurfaceMuted
                    )
                }
                if (hasOwnListing) {
                    HorizontalDivider(color = tokens.border)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteConfirm = true }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Delete post",
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.dangerFg,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            SettingsSection(title = "PRIVACY") {
                SettingsSwitchRow(
                    title = "Messages only after matching",
                    checked = settings.messagesOnlyAfterMatching,
                    onCheckedChange = { onUpdateSettings(settings.copy(messagesOnlyAfterMatching = it)) }
                )
                HorizontalDivider(color = tokens.border)
                SettingsSwitchRow(
                    title = "Show my phone number",
                    checked = settings.showPhoneNumber,
                    onCheckedChange = { onUpdateSettings(settings.copy(showPhoneNumber = it)) }
                )
                HorizontalDivider(color = tokens.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blocked people",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.onSurface
                    )
                    Text(
                        text = "${settings.blockedPeople.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.onSurfaceMuted
                    )
                }
            }

            SettingsSection(title = "NOTIFICATIONS") {
                SettingsSwitchRow(
                    title = "New matches",
                    checked = settings.notifyNewMatches,
                    onCheckedChange = { onUpdateSettings(settings.copy(notifyNewMatches = it)) }
                )
                HorizontalDivider(color = tokens.border)
                SettingsSwitchRow(
                    title = "Messages",
                    checked = settings.notifyMessages,
                    onCheckedChange = { onUpdateSettings(settings.copy(notifyMessages = it)) }
                )
            }

            SettingsSection(title = "ACCOUNT") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (accountIdentifier.contains("@")) "Google account" else "Phone number",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.onSurface
                    )
                    Text(
                        text = accountIdentifier,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.onSurfaceMuted
                    )
                }
                HorizontalDivider(color = tokens.border)
                var showSignOutConfirm by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSignOutConfirm = true }
                        .padding(horizontal = 16.dp, vertical = 15.dp)
                        .testTag("sign_out_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sign out",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.dangerFg,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (showSignOutConfirm) {
                    AlertDialog(
                        onDismissRequest = { showSignOutConfirm = false },
                        containerColor = tokens.surface,
                        title = { Text("Sign out of CoM8?", color = tokens.onSurface) },
                        text = {
                            Text(
                                "Signing out clears local app data and cache on this device. Your profile, matches, and chats stay synced in your account.",
                                color = tokens.onSurfaceMuted
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showSignOutConfirm = false
                                    onSignOut()
                                },
                                modifier = Modifier.testTag("confirm_sign_out_button")
                            ) {
                                Text("Sign out", color = tokens.dangerFg, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSignOutConfirm = false }) {
                                Text("Cancel", color = tokens.onSurface)
                            }
                        }
                    )
                }
            }

            val settingsContext = LocalContext.current

            SettingsSection(title = "ABOUT & LEGAL") {
                SettingsExternalLinkRow(
                    title = "Terms & Conditions",
                    subtitle = "com8.com/terms",
                    testTag = "settings_terms_link",
                    onClick = { openExternalCom8Page(settingsContext, "https://com8.com/terms") }
                )
                HorizontalDivider(color = tokens.border)
                SettingsExternalLinkRow(
                    title = "Privacy Policy",
                    subtitle = "com8.com/privacy",
                    testTag = "settings_privacy_link",
                    onClick = { openExternalCom8Page(settingsContext, "https://com8.com/privacy") }
                )
                HorizontalDivider(color = tokens.border)
                SettingsExternalLinkRow(
                    title = "Disclaimer",
                    subtitle = "com8.com/disclaimer",
                    testTag = "settings_disclaimer_link",
                    onClick = { openExternalCom8Page(settingsContext, "https://com8.com/disclaimer") }
                )
                HorizontalDivider(color = tokens.border)
                SettingsExternalLinkRow(
                    title = "Usage & Guidelines",
                    subtitle = "com8.com/usage",
                    testTag = "settings_usage_link",
                    onClick = { openExternalCom8Page(settingsContext, "https://com8.com/usage") }
                )
                HorizontalDivider(color = tokens.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App version",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.onSurface
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.onSurfaceMuted
                    )
                }
                HorizontalDivider(color = tokens.border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last updated",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.onSurface
                    )
                    Text(
                        text = BuildConfig.LAST_UPDATED_DATE,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.onSurfaceMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = tokens.surface,
            title = { Text("Delete your post?", color = tokens.onSurface) },
            text = {
                Text(
                    "Your roommate post will no longer appear in Discover until you publish a new one.",
                    color = tokens.onSurfaceMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeletePost()
                    }
                ) {
                    Text("Delete", color = tokens.dangerFg, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = tokens.onSurface)
                }
            }
        )
    }
}

@Composable
private fun ProfileDetailRow(
    label: String,
    value: String,
    valueColor: Color = CoM8ThemeTokens.colors.onSurfaceMuted
) {
    val tokens = CoM8ThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = tokens.onSurface
        )
        Text(
            text = value,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = valueColor
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.onSurfaceMuted,
            letterSpacing = 0.8.sp
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = tokens.surface,
            border = BorderStroke(1.dp, tokens.border)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
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
            text = title,
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

@Composable
private fun SettingsExternalLinkRow(
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.onSurfaceMuted
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = "Open $title in browser",
            tint = tokens.onSurfaceMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun openExternalCom8Page(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val launched = kotlin.runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
    if (!launched) {
        Toast.makeText(context, "Open $url in your browser", Toast.LENGTH_SHORT).show()
    }
}

private fun formatProfileAmount(amount: Int): String {
    val s = amount.toString()
    return if (s.length > 3) "${s.dropLast(3)},${s.takeLast(3)}" else s
}
