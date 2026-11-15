package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CoM8Database
import com.example.data.local.SessionDataStore
import com.example.data.model.FlatmateListing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CoM8LocalRepositoryTest {

    private lateinit var db: CoM8Database
    private lateinit var repository: CoM8LocalRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CoM8Database::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CoM8LocalRepository(
            dao = db.coM8Dao(),
            sessionStore = SessionDataStore(context)
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `normalizeIndianMobile validates 10-digit Indian numbers`() {
        assertEquals("+919876543210", CoM8LocalRepository.normalizeIndianMobile("9876543210"))
        assertEquals("+919876543210", CoM8LocalRepository.normalizeIndianMobile("+91 98765-43210"))
        assertNull(CoM8LocalRepository.normalizeIndianMobile("1234567890"))
        assertNull(CoM8LocalRepository.normalizeIndianMobile("98765"))
    }

    @Test
    fun `mobile signup and signin persist profile without OTP`() = runTest {
        val unregistered = repository.signInWithMobile("9876543210")
        assertTrue(unregistered.isFailure)

        val signedUp = repository.signUpWithMobile("9876543210")
        assertTrue(signedUp.isSuccess)
        assertEquals("+919876543210", signedUp.getOrNull())
        assertEquals("+919876543210", repository.activePhoneFlow.first())

        val duplicate = repository.signUpWithMobile("9876543210")
        assertTrue(duplicate.isFailure)

        repository.signOut()
        assertNull(repository.activePhoneFlow.first())

        val signedIn = repository.signInWithMobile("9876543210")
        assertTrue(signedIn.isSuccess)
        assertEquals("+919876543210", repository.activePhoneFlow.first())
    }

    @Test
    fun `swipes undo and messages persist per phone number`() = runTest {
        repository.signUpWithMobile("9876543211")
        val phone = "+919876543211"
        val listing = FlatmateListing(
            id = "listing_1",
            name = "Aarav",
            city = "Pune",
            locality = "Baner",
            monthlyRent = 15000L
        )

        repository.recordSwipe(phone, listing, "LIKED", "SAVED", 91)
        assertEquals(1, repository.observeSwipes(phone).first().size)

        repository.sendMatchMessage(phone, listing.id, "ME", "Hey Aarav!")
        val msgs = repository.observeMatchMessages(phone, listing.id).first()
        assertEquals(1, msgs.size)
        assertEquals("Hey Aarav!", msgs.first().text)

        repository.deleteSwipe(phone, listing.id)
        assertTrue(repository.observeSwipes(phone).first().isEmpty())
    }

    @Test
    fun `finalizeMobileAuth saves mobile number string to DataStore as user ID`() = runTest {
        val invalid = repository.finalizeMobileAuth("12345")
        assertTrue(invalid.isFailure)

        val firstEntry = repository.finalizeMobileAuth("+1 555-234-5678")
        assertTrue(firstEntry.isSuccess)
        assertEquals("+15552345678", firstEntry.getOrNull())
        assertEquals("+15552345678", repository.activePhoneFlow.first())

        repository.signOut()
        assertNull(repository.activePhoneFlow.first())

        val repeatEntry = repository.finalizeMobileAuth("5552345678")
        assertTrue(repeatEntry.isSuccess)
        assertEquals("+915552345678", repository.activePhoneFlow.first())
    }

    @Test
    fun `profile stores photo currentResidence preferredCities and computes why matched`() = runTest {
        val phone = "+919876543219"
        repository.finalizeMobileAuth(phone)
        val savedProfile = com.example.data.model.UserLifestyleProfile(
            phoneNumber = phone,
            displayName = "Aarav Sharma",
            photoUri = "preset:1",
            currentResidence = "Kothrud, Pune",
            preferredCities = listOf("Pune", "Bengaluru"),
            monthlyBudget = 17000L,
            quizCompleted = true
        )
        repository.saveUserProfile(phone, savedProfile)

        val loaded = repository.observeUserProfile(phone).first()
        assertEquals("Aarav Sharma", loaded?.displayName)
        assertEquals("Kothrud, Pune", loaded?.currentResidence)
        assertEquals(listOf("Pune", "Bengaluru"), loaded?.preferredCities)

        val candidate = FlatmateListing(
            id = "test_cand",
            name = "Ananya Kulkarni",
            city = "Pune",
            locality = "Baner",
            monthlyRent = 15500L
        )
        val reasons = com.example.data.model.CompatibilityCalculator.computeWhyMatched(loaded, candidate)
        assertTrue(reasons.isNotEmpty())
        assertTrue(reasons.first().contains("Pune"))
    }
}
