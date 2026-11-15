package com.example.ui.auth

import android.app.Activity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhoneAuthManagerTest {

    private class FakePhoneAuthGateway : PhoneAuthGateway {
        var lastPhoneNumber: String? = null
        var lastTimeoutSeconds: Long? = null
        var lastResendToken: ResendTokenHandle? = null
        var lastCallbacks: PhoneAuthGateway.Callbacks? = null
        var verifyInvocationCount = 0

        var signInResult: Result<Pair<String, String?>> =
            Result.success("uid_123" to "+919822544582")
        var linkResult: Result<Pair<String, String?>> =
            Result.success("google_uid_123" to "+919822544582")

        override fun verifyPhoneNumber(
            activity: Activity,
            phoneNumber: String,
            timeoutSeconds: Long,
            resendToken: ResendTokenHandle?,
            callbacks: PhoneAuthGateway.Callbacks
        ) {
            verifyInvocationCount++
            lastPhoneNumber = phoneNumber
            lastTimeoutSeconds = timeoutSeconds
            lastResendToken = resendToken
            lastCallbacks = callbacks
        }

        override fun getCredential(verificationId: String, otpCode: String): PhoneCredentialHandle {
            return PhoneCredentialHandle.Test(verificationId, otpCode)
        }

        override suspend fun signInWithCredential(
            credential: PhoneCredentialHandle
        ): Result<Pair<String, String?>> = signInResult

        override suspend fun linkWithCredential(
            credential: PhoneCredentialHandle
        ): Result<Pair<String, String?>> = linkResult
    }

    private fun testActivity(): Activity = Robolectric.buildActivity(Activity::class.java).get()

    @Test
    fun verifyPhoneNumber_and_signInWithCredential_transitionsToVerified() = runTest {
        val gateway = FakePhoneAuthGateway()
        val manager = PhoneAuthManager(scope = this, gateway = gateway)
        val activity = testActivity()

        assertEquals(PhoneAuthState.Idle, manager.uiState.value)

        manager.verifyPhoneNumber(activity, "+919822544582")
        assertEquals(PhoneAuthState.SendingCode, manager.uiState.value)
        assertEquals("+919822544582", gateway.lastPhoneNumber)
        assertEquals(60L, gateway.lastTimeoutSeconds)
        assertNull(gateway.lastResendToken)

        gateway.lastCallbacks?.onCodeSent("ver_id_1", ResendTokenHandle.Test)
        assertEquals(
            PhoneAuthState.CodeSent(verificationId = "ver_id_1", isResend = false),
            manager.uiState.value
        )

        manager.signInWithCredential("123456")
        advanceUntilIdle()
        assertEquals(
            PhoneAuthState.Verified(uid = "uid_123", phoneNumber = "+919822544582"),
            manager.uiState.value
        )
    }

    @Test
    fun resend_reusesStoredResendToken_and_preservesVerificationSessionOnFailure() = runTest {
        val gateway = FakePhoneAuthGateway()
        val manager = PhoneAuthManager(scope = this, gateway = gateway)
        val activity = testActivity()

        manager.verifyPhoneNumber(activity, "9822544582", isResend = false)
        gateway.lastCallbacks?.onCodeSent("ver_id_1", ResendTokenHandle.Test)

        manager.verifyPhoneNumber(activity, "9822544582", isResend = true)
        assertNotNull(gateway.lastResendToken)
        gateway.lastCallbacks?.onCodeSent("ver_id_2", ResendTokenHandle.Test)
        assertEquals(
            PhoneAuthState.CodeSent(verificationId = "ver_id_2", isResend = true),
            manager.uiState.value
        )

        gateway.signInResult = Result.failure(IllegalStateException("Invalid OTP code"))
        manager.signInWithCredential("000000")
        advanceUntilIdle()

        val error = manager.uiState.value as PhoneAuthState.Error
        assertEquals("Invalid OTP code", error.message)
        assertTrue(error.hasVerificationId)

        manager.clearError()
        assertTrue(manager.uiState.value is PhoneAuthState.CodeSent)
    }

    @Test
    fun autoVerification_and_linkWithCredential_updateStateCleanly() = runTest {
        val gateway = FakePhoneAuthGateway()
        val manager = PhoneAuthManager(scope = this, gateway = gateway)
        val activity = testActivity()

        manager.verifyPhoneNumber(activity, "+919822544582", linkToCurrentUser = true)
        gateway.lastCallbacks?.onVerificationCompleted(
            PhoneCredentialHandle.Test("auto_ver", "654321")
        )
        advanceUntilIdle()
        assertEquals(
            PhoneAuthState.Verified(uid = "google_uid_123", phoneNumber = "+919822544582"),
            manager.uiState.value
        )

        manager.reset()
        assertEquals(PhoneAuthState.Idle, manager.uiState.value)

        // Short phone number triggers immediate validation error without hitting gateway
        manager.verifyPhoneNumber(activity, "12345")
        val validationError = manager.uiState.value as PhoneAuthState.Error
        assertFalse(validationError.hasVerificationId)
        assertEquals(1, gateway.verifyInvocationCount)
    }
}
