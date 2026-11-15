package com.example.ui.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.repository.CoM8LocalRepository
import com.example.ui.components.CircularBackButton
import com.example.ui.components.CoM8Wordmark
import com.example.ui.theme.CoM8ThemeTokens
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class AuthStep {
    WELCOME,
    PHONE_INPUT,
    OTP_VERIFY
}

@Composable
fun MobileAuthScreen(
    repository: CoM8LocalRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val phoneAuthManager = remember(scope) { PhoneAuthManager(scope) }
    val phoneAuthState by phoneAuthManager.uiState.collectAsStateWithLifecycle()

    var authStep by remember { mutableStateOf(AuthStep.WELCOME) }
    var mobileDigits by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isPhoneLoading = phoneAuthState is PhoneAuthState.SendingCode ||
        phoneAuthState is PhoneAuthState.Verifying
    val displayedError = (phoneAuthState as? PhoneAuthState.Error)?.message ?: errorMessage

    LaunchedEffect(phoneAuthState) {
        when (val state = phoneAuthState) {
            is PhoneAuthState.CodeSent -> {
                if (!state.isResend && authStep != AuthStep.OTP_VERIFY) {
                    otpCode = ""
                    authStep = AuthStep.OTP_VERIFY
                }
            }
            is PhoneAuthState.Verified -> {
                repository.finalizeFirebasePhoneAuth(
                    uid = state.uid,
                    rawPhoneNumber = state.phoneNumber.ifBlank { "+91$mobileDigits" }
                )
            }
            else -> Unit
        }
    }

    if (authStep == AuthStep.OTP_VERIFY) {
        BackHandler {
            errorMessage = null
            phoneAuthManager.reset()
            authStep = AuthStep.PHONE_INPUT
        }
    } else if (authStep == AuthStep.PHONE_INPUT) {
        BackHandler {
            errorMessage = null
            phoneAuthManager.reset()
            authStep = AuthStep.WELCOME
        }
    }

    fun startPhoneVerification(isResend: Boolean = false) {
        val activity = context as? Activity
        if (activity == null) {
            errorMessage = "Unable to start phone verification from this context."
            return
        }
        errorMessage = null
        phoneAuthManager.verifyPhoneNumber(
            activity = activity,
            phoneNumber = "+91$mobileDigits",
            isResend = isResend
        )
    }

    fun launchGoogleSignIn() {
        if (isGoogleLoading || isPhoneLoading) return
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            errorMessage = "Google Sign-In is not configured yet."
            return
        }
        val activity = context as? Activity
        if (activity == null) {
            errorMessage = "Unable to launch Google Sign-In from this context."
            return
        }

        isGoogleLoading = true
        errorMessage = null
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        repository.finalizeGoogleAuth(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email,
                            displayName = firebaseUser.displayName
                        )
                    } else {
                        errorMessage = "Google Sign-In failed to return a user."
                    }
                } else {
                    errorMessage = "Unexpected Google credential type."
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                errorMessage = e.localizedMessage ?: "Google Sign-In failed. Try phone number."
            } finally {
                isGoogleLoading = false
            }
        }
    }

    AnimatedContent(
        targetState = authStep,
        transitionSpec = {
            if (targetState.ordinal >= initialState.ordinal) {
                (slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(180)))
            } else {
                (slideInHorizontally(tween(280)) { -it / 3 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(180)))
            }
        },
        label = "auth_step_transition"
    ) { step ->
        when (step) {
            AuthStep.WELCOME -> WelcomeHeroScreen(
                isGoogleLoading = isGoogleLoading,
                errorMessage = displayedError,
                onPhoneClick = {
                    errorMessage = null
                    phoneAuthManager.clearError()
                    authStep = AuthStep.PHONE_INPUT
                },
                onGoogleClick = ::launchGoogleSignIn
            )
            AuthStep.PHONE_INPUT -> PhoneNumberEntryScreen(
                mobileDigits = mobileDigits,
                isLoading = isPhoneLoading,
                errorMessage = displayedError,
                onDigitsChanged = { raw ->
                    mobileDigits = raw.filter { it.isDigit() }.take(10)
                    errorMessage = null
                    phoneAuthManager.clearError()
                },
                onBack = {
                    errorMessage = null
                    phoneAuthManager.reset()
                    authStep = AuthStep.WELCOME
                },
                onSendCode = { startPhoneVerification(isResend = false) }
            )
            AuthStep.OTP_VERIFY -> OtpVerificationScreen(
                mobileDigits = mobileDigits,
                otpCode = otpCode,
                isLoading = isPhoneLoading,
                errorMessage = displayedError,
                onOtpChanged = { raw ->
                    otpCode = raw.filter { it.isDigit() }.take(6)
                    errorMessage = null
                    phoneAuthManager.clearError()
                },
                onBack = {
                    errorMessage = null
                    phoneAuthManager.reset()
                    authStep = AuthStep.PHONE_INPUT
                },
                onResend = { startPhoneVerification(isResend = true) },
                onVerify = {
                    errorMessage = null
                    phoneAuthManager.signInWithCredential(otpCode)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeHeroScreen(
    isGoogleLoading: Boolean,
    errorMessage: String?,
    onPhoneClick: () -> Unit,
    onGoogleClick: () -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    val welcomeBg = Color(0xFF10201F)
    val welcomeInk = Color(0xFFF5F7F6)
    val welcomeMuted = Color(0xFFA9B8B5)
    val welcomeCardInkMuted = Color(0xFF4F5F5D)
    val welcomeBackdrop = Color(0xFF1D3532)
    val welcomeAvatarBg = Color(0xFFCFE3DE)
    val welcomeMint = Color(0xFF3CC9B0)
    val welcomeAmber = Color(0xFFF5C04A)
    val welcomeBorder = Color(0xFFDCE3E1)
    val welcomeGoogleBorder = Color(0xFF3C5552)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(welcomeBg)
            .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .heightIn(min = maxHeight)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 28.dp)
        ) {
            CoM8Wordmark(onDarkBackground = true)

            Spacer(modifier = Modifier.height(28.dp))

            // Tilted backdrop + overlapping white preview card
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(190.dp)
                        .rotate(-4f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(welcomeBackdrop)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = welcomeInk,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(welcomeAvatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "R",
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = welcomeBg
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Rohan, Baner",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = welcomeBg
                                    )
                                    Text(
                                        text = "Looking for a 2BHK flatmate",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = welcomeCardInkMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = welcomeAmber
                            ) {
                                Text(
                                    text = "92% match",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = welcomeBg
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Non-smoker", "Doesn't drink", "Early riser").forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, welcomeBorder)
                                ) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = welcomeBg
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Live with people\nwho fit your life.",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.5).sp,
                color = welcomeInk
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Swipe through verified flatmates matched on smoking, sleep, cleanliness, budget and more.",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = welcomeMuted
            )

            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = tokens.dangerBg,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.dangerFg
                        )
                    }
                }

                Button(
                    onClick = onPhoneClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("continue_phone_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = welcomeMint,
                        contentColor = welcomeBg
                    )
                ) {
                    Text(
                        text = "Continue with phone number",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onGoogleClick,
                    enabled = !isGoogleLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("continue_google_button"),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, welcomeGoogleBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = welcomeInk
                    )
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = welcomeInk,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Continue with Google",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "By continuing you agree to the Terms and Privacy Policy.",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = welcomeMuted.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PhoneNumberEntryScreen(
    mobileDigits: String,
    isLoading: Boolean,
    errorMessage: String?,
    onDigitsChanged: (String) -> Unit,
    onBack: () -> Unit,
    onSendCode: () -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    val focusRequester = remember { FocusRequester() }
    var isPhoneFocused by remember { mutableStateOf(false) }
    val formatted = remember(mobileDigits) { formatIndianPhone(mobileDigits) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime))
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
        ) {
            CircularBackButton(onClick = onBack, modifier = Modifier.testTag("phone_back_button"))

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "What's your phone\nnumber?",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 34.5.sp,
                letterSpacing = (-0.3).sp,
                color = tokens.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "We'll send a 6-digit SMS code via Firebase Auth to confirm it's you.",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.5.sp,
                color = tokens.onSurfaceMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Mobile number",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = tokens.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.surface,
                    border = BorderStroke(1.dp, tokens.border)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+91",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = tokens.onSurface
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable { focusRequester.requestFocus() },
                    shape = RoundedCornerShape(16.dp),
                    color = tokens.surface,
                    border = BorderStroke(
                        width = if (isPhoneFocused) 2.dp else 1.dp,
                        color = if (isPhoneFocused) tokens.primary else tokens.border
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = mobileDigits,
                            onValueChange = onDigitsChanged,
                            singleLine = true,
                            cursorBrush = SolidColor(tokens.primary),
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { onSendCode() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { isPhoneFocused = it.isFocused }
                                .testTag("mobile_number_input")
                        )
                        Text(
                            text = if (mobileDigits.isEmpty()) "98765 43210" else formatted,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            letterSpacing = 0.5.sp,
                            color = if (mobileDigits.isEmpty()) {
                                tokens.onSurfaceMuted.copy(alpha = 0.65f)
                            } else {
                                tokens.onSurface
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = tokens.successBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = tokens.successFg,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = "Your number is never shown on your profile. Others only see it if you choose to share it.",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = tokens.successFg
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tokens.dangerBg
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.dangerFg
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onSendCode,
                enabled = !isLoading && mobileDigits.length >= 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("send_otp_button"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.primary,
                    contentColor = Color.White,
                    disabledContainerColor = tokens.primary.copy(alpha = 0.45f),
                    disabledContentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Send code",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
            Text(
                text = "By continuing you agree to the Terms and Privacy Policy.",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = tokens.onSurfaceMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OtpVerificationScreen(
    mobileDigits: String,
    otpCode: String,
    isLoading: Boolean,
    errorMessage: String?,
    onOtpChanged: (String) -> Unit,
    onBack: () -> Unit,
    onResend: () -> Unit,
    onVerify: () -> Unit
) {
    val tokens = CoM8ThemeTokens.colors
    val focusRequester = remember { FocusRequester() }
    var isOtpFocused by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(24) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000L)
            countdown -= 1
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime))
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
        ) {
            CircularBackButton(onClick = onBack, modifier = Modifier.testTag("otp_back_button"))

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Enter the 6-digit code",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 34.5.sp,
                letterSpacing = (-0.3).sp,
                color = tokens.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Sent to +91 ${formatIndianPhone(mobileDigits)}.",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = tokens.onSurfaceMuted
                )
                Text(
                    text = "Change number",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = tokens.primary,
                    modifier = Modifier.clickable(onClick = onBack)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { focusRequester.requestFocus() }
            ) {
                BasicTextField(
                    value = otpCode,
                    onValueChange = onOtpChanged,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onVerify() }),
                    cursorBrush = SolidColor(Color.Transparent),
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = Color.Transparent),
                    modifier = Modifier
                        .matchParentSize()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isOtpFocused = it.isFocused }
                        .testTag("otp_code_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(6) { index ->
                        val char = otpCode.getOrNull(index)?.toString().orEmpty()
                        val isActive = isOtpFocused && index == otpCode.length.coerceAtMost(5)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = tokens.surface,
                            border = BorderStroke(
                                width = if (isActive) 2.dp else 1.dp,
                                color = if (isActive) tokens.primary else tokens.border
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = char,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center,
                                    color = tokens.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (countdown > 0) "Resend code in 0:${countdown.toString().padStart(2, '0')}" else "Didn't get a code?",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = tokens.onSurfaceMuted
                )
                Text(
                    text = "Resend",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (countdown == 0) tokens.primary else Color(0xFF7B8A88),
                    modifier = Modifier.clickable(enabled = countdown == 0) {
                        countdown = 24
                        onResend()
                    }
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tokens.dangerBg
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.dangerFg
                    )
                }
            }
        }

        Button(
            onClick = onVerify,
            enabled = !isLoading && otpCode.length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(top = 12.dp)
                .height(56.dp)
                .testTag("mobile_auth_submit_button"),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = tokens.primary,
                contentColor = Color.White,
                disabledContainerColor = tokens.primary.copy(alpha = 0.45f),
                disabledContentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Verify and continue",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

private fun formatIndianPhone(digits: String): String {
    val clean = digits.filter { it.isDigit() }.take(10)
    return if (clean.length > 5) "${clean.take(5)} ${clean.drop(5)}" else clean
}
