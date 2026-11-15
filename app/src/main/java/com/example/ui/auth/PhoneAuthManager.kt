package com.example.ui.auth

import android.app.Activity
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

sealed interface PhoneAuthState {
    data object Idle : PhoneAuthState
    data object SendingCode : PhoneAuthState
    data class CodeSent(
        val verificationId: String,
        val isResend: Boolean = false
    ) : PhoneAuthState
    data object Verifying : PhoneAuthState
    data class Verified(
        val uid: String,
        val phoneNumber: String
    ) : PhoneAuthState
    data class Error(
        val message: String,
        val hasVerificationId: Boolean = false
    ) : PhoneAuthState
}

internal sealed interface PhoneCredentialHandle {
    data class Firebase(val credential: PhoneAuthCredential) : PhoneCredentialHandle
    data class Test(val verificationId: String, val otpCode: String) : PhoneCredentialHandle
}

internal sealed interface ResendTokenHandle {
    data class Firebase(val token: PhoneAuthProvider.ForceResendingToken) : ResendTokenHandle
    data object Test : ResendTokenHandle
}

internal interface PhoneAuthGateway {
    interface Callbacks {
        fun onVerificationCompleted(credential: PhoneCredentialHandle)
        fun onVerificationFailed(message: String)
        fun onCodeSent(verificationId: String, token: ResendTokenHandle)
    }

    fun verifyPhoneNumber(
        activity: Activity,
        phoneNumber: String,
        timeoutSeconds: Long,
        resendToken: ResendTokenHandle?,
        callbacks: Callbacks
    )

    fun getCredential(verificationId: String, otpCode: String): PhoneCredentialHandle

    suspend fun signInWithCredential(credential: PhoneCredentialHandle): Result<Pair<String, String?>>

    suspend fun linkWithCredential(credential: PhoneCredentialHandle): Result<Pair<String, String?>>
}

internal class FirebasePhoneAuthGateway : PhoneAuthGateway {
    override fun verifyPhoneNumber(
        activity: Activity,
        phoneNumber: String,
        timeoutSeconds: Long,
        resendToken: ResendTokenHandle?,
        callbacks: PhoneAuthGateway.Callbacks
    ) {
        val sdkCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                callbacks.onVerificationCompleted(PhoneCredentialHandle.Firebase(credential))
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("PhoneAuthManager", "Phone verification failed", e)
                callbacks.onVerificationFailed(
                    e.localizedMessage ?: "Unable to send verification code. Please try again."
                )
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                callbacks.onCodeSent(verificationId, ResendTokenHandle.Firebase(token))
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(sdkCallbacks)
        val firebaseToken = (resendToken as? ResendTokenHandle.Firebase)?.token
        if (firebaseToken != null) {
            optionsBuilder.setForceResendingToken(firebaseToken)
        }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    override fun getCredential(verificationId: String, otpCode: String): PhoneCredentialHandle {
        return PhoneCredentialHandle.Firebase(
            PhoneAuthProvider.getCredential(verificationId, otpCode)
        )
    }

    override suspend fun signInWithCredential(
        credential: PhoneCredentialHandle
    ): Result<Pair<String, String?>> {
        val firebaseCred = (credential as? PhoneCredentialHandle.Firebase)?.credential
            ?: return Result.failure(IllegalArgumentException("Expected Firebase PhoneAuthCredential."))
        return try {
            val result = Firebase.auth.signInWithCredential(firebaseCred).await()
            val user = result.user
            if (user != null) {
                Result.success(user.uid to user.phoneNumber)
            } else {
                Result.failure(IllegalStateException("Phone verification failed to return a user."))
            }
        } catch (e: Exception) {
            Log.e("PhoneAuthManager", "Phone credential sign-in failed", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Invalid verification code. Please check and retry.",
                    e
                )
            )
        }
    }

    override suspend fun linkWithCredential(
        credential: PhoneCredentialHandle
    ): Result<Pair<String, String?>> {
        val firebaseCred = (credential as? PhoneCredentialHandle.Firebase)?.credential
            ?: return Result.failure(IllegalArgumentException("Expected Firebase PhoneAuthCredential."))
        val user = Firebase.auth.currentUser
            ?: return Result.failure(IllegalStateException("No active signed-in user found."))
        return try {
            val result = user.linkWithCredential(firebaseCred).await()
            val linkedUser = result.user ?: user
            Result.success(linkedUser.uid to linkedUser.phoneNumber)
        } catch (e: FirebaseAuthUserCollisionException) {
            Log.w("PhoneAuthManager", "Phone already linked to another account", e)
            Result.failure(
                IllegalStateException(
                    "This phone number is already linked to a different CoM8 account.",
                    e
                )
            )
        } catch (e: Exception) {
            Log.e("PhoneAuthManager", "Phone linking failed", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Verification failed. Please check the code and retry.",
                    e
                )
            )
        }
    }
}

class PhoneAuthManager internal constructor(
    private val scope: CoroutineScope,
    private val gateway: PhoneAuthGateway = FirebasePhoneAuthGateway(),
    private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS
) {
    constructor(scope: CoroutineScope) : this(
        scope = scope,
        gateway = FirebasePhoneAuthGateway(),
        timeoutSeconds = DEFAULT_TIMEOUT_SECONDS
    )

    private val _uiState = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Idle)
    val uiState: StateFlow<PhoneAuthState> = _uiState.asStateFlow()

    private var storedVerificationId: String? = null
    private var storedResendToken: ResendTokenHandle? = null
    private var lastPhoneNumber: String = ""

    fun verifyPhoneNumber(
        activity: Activity,
        phoneNumber: String,
        isResend: Boolean = false,
        linkToCurrentUser: Boolean = false
    ) {
        val current = _uiState.value
        if (current is PhoneAuthState.SendingCode || current is PhoneAuthState.Verifying) {
            return
        }
        val cleanPhone = phoneNumber.trim()
        val digits = cleanPhone.filter { it.isDigit() }
        if (digits.length < 10) {
            _uiState.value = PhoneAuthState.Error(
                message = "Enter a valid 10-digit mobile number",
                hasVerificationId = !storedVerificationId.isNullOrBlank()
            )
            return
        }
        val e164Phone = if (cleanPhone.startsWith("+")) cleanPhone else "+91$digits"
        lastPhoneNumber = e164Phone
        _uiState.value = PhoneAuthState.SendingCode

        val callbacks = object : PhoneAuthGateway.Callbacks {
            override fun onVerificationCompleted(credential: PhoneCredentialHandle) {
                if (linkToCurrentUser) {
                    linkWithCredentialHandle(credential, e164Phone)
                } else {
                    signInWithCredentialHandle(credential, e164Phone)
                }
            }

            override fun onVerificationFailed(message: String) {
                _uiState.value = PhoneAuthState.Error(
                    message = message,
                    hasVerificationId = !storedVerificationId.isNullOrBlank()
                )
            }

            override fun onCodeSent(verificationId: String, token: ResendTokenHandle) {
                storedVerificationId = verificationId
                storedResendToken = token
                _uiState.value = PhoneAuthState.CodeSent(
                    verificationId = verificationId,
                    isResend = isResend
                )
            }
        }

        gateway.verifyPhoneNumber(
            activity = activity,
            phoneNumber = e164Phone,
            timeoutSeconds = timeoutSeconds,
            resendToken = if (isResend) storedResendToken else null,
            callbacks = callbacks
        )
    }

    fun signInWithCredential(otpCode: String) {
        val current = _uiState.value
        if (current is PhoneAuthState.SendingCode || current is PhoneAuthState.Verifying) {
            return
        }
        val cleanCode = otpCode.filter { it.isDigit() }
        if (cleanCode.length != 6) {
            _uiState.value = PhoneAuthState.Error(
                message = "Enter the 6-digit verification code",
                hasVerificationId = !storedVerificationId.isNullOrBlank()
            )
            return
        }
        val verificationId = storedVerificationId
        if (verificationId.isNullOrBlank()) {
            _uiState.value = PhoneAuthState.Error(
                message = "Verification session expired. Request a new code.",
                hasVerificationId = false
            )
            return
        }
        val credential = gateway.getCredential(verificationId, cleanCode)
        signInWithCredentialHandle(credential, lastPhoneNumber)
    }

    fun signInWithCredential(
        credential: PhoneAuthCredential,
        fallbackPhone: String = lastPhoneNumber
    ) {
        signInWithCredentialHandle(PhoneCredentialHandle.Firebase(credential), fallbackPhone)
    }

    fun linkWithCredential(otpCode: String) {
        val current = _uiState.value
        if (current is PhoneAuthState.SendingCode || current is PhoneAuthState.Verifying) {
            return
        }
        val cleanCode = otpCode.filter { it.isDigit() }
        if (cleanCode.length != 6) {
            _uiState.value = PhoneAuthState.Error(
                message = "Enter the 6-digit code",
                hasVerificationId = !storedVerificationId.isNullOrBlank()
            )
            return
        }
        val verificationId = storedVerificationId
        if (verificationId.isNullOrBlank()) {
            _uiState.value = PhoneAuthState.Error(
                message = "Verification session expired. Request a new code.",
                hasVerificationId = false
            )
            return
        }
        val credential = gateway.getCredential(verificationId, cleanCode)
        linkWithCredentialHandle(credential, lastPhoneNumber)
    }

    fun linkWithCredential(
        credential: PhoneAuthCredential,
        fallbackPhone: String = lastPhoneNumber
    ) {
        linkWithCredentialHandle(PhoneCredentialHandle.Firebase(credential), fallbackPhone)
    }

    fun clearError() {
        val current = _uiState.value
        if (current is PhoneAuthState.Error) {
            val verId = storedVerificationId
            _uiState.value = if (!verId.isNullOrBlank()) {
                PhoneAuthState.CodeSent(verificationId = verId, isResend = true)
            } else {
                PhoneAuthState.Idle
            }
        }
    }

    fun reset() {
        storedVerificationId = null
        storedResendToken = null
        lastPhoneNumber = ""
        _uiState.value = PhoneAuthState.Idle
    }

    private fun signInWithCredentialHandle(
        credential: PhoneCredentialHandle,
        fallbackPhone: String
    ) {
        scope.launch {
            _uiState.value = PhoneAuthState.Verifying
            val res = gateway.signInWithCredential(credential)
            _uiState.value = res.fold(
                onSuccess = { (uid, phone) ->
                    PhoneAuthState.Verified(
                        uid = uid,
                        phoneNumber = phone?.takeIf { it.isNotBlank() } ?: fallbackPhone
                    )
                },
                onFailure = { error ->
                    PhoneAuthState.Error(
                        message = error.localizedMessage
                            ?: "Invalid verification code. Please check and retry.",
                        hasVerificationId = !storedVerificationId.isNullOrBlank()
                    )
                }
            )
        }
    }

    private fun linkWithCredentialHandle(
        credential: PhoneCredentialHandle,
        fallbackPhone: String
    ) {
        scope.launch {
            _uiState.value = PhoneAuthState.Verifying
            val res = gateway.linkWithCredential(credential)
            _uiState.value = res.fold(
                onSuccess = { (uid, phone) ->
                    PhoneAuthState.Verified(
                        uid = uid,
                        phoneNumber = phone?.takeIf { it.isNotBlank() } ?: fallbackPhone
                    )
                },
                onFailure = { error ->
                    PhoneAuthState.Error(
                        message = error.localizedMessage
                            ?: "Verification failed. Please check the code and retry.",
                        hasVerificationId = !storedVerificationId.isNullOrBlank()
                    )
                }
            )
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 60L
    }
}
