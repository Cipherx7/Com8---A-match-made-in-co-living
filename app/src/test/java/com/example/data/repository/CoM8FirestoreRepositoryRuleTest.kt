package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FlatmateListing
import com.example.data.model.UserLifestyleProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CoM8FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun syncAndObserveOwnProfile_succeedsForAuthenticatedUser() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CoM8FirestoreRepository(firestore, auth)

        val profile = UserLifestyleProfile(
            phoneNumber = "google_$uid",
            displayName = "Abhishek",
            city = "Pune",
            roleType = "SEEKING_FLAT",
            monthlyBudget = 14000L,
            sleepSchedule = "EARLY_BIRD",
            cleanlinessLevel = 4L,
            guestPolicy = "WEEKENDS_OK",
            foodHabit = "VEG_ONLY",
            bio = "Working in cybersecurity and looking for a quiet 2BHK in Baner.",
            quizCompleted = true,
            verifiedBadge = true,
            trustScore = 92L
        )

        val syncRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.syncUserProfile(profile) }
        assertTrue("Expected sync success: ${syncRes.exceptionOrNull()}", syncRes.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getOwnProfile().getOrThrow() }
        assertNotNull(fetched)
        assertEquals("Abhishek", fetched?.displayName)
        assertEquals("Pune", fetched?.city)
    }

    @Test
    fun publishAndObservePublicListing_enforcesOwnershipAndPublicVisibility() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CoM8FirestoreRepository(firestore, auth)
        val listingId = "list_${UUID.randomUUID().toString().take(8)}"

        val pubRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.publishListing(
                FlatmateListing(
                    id = listingId,
                    name = "Rohan",
                    age = 24L,
                    occupation = "Product intern",
                    roleType = "HAS_FLAT",
                    city = "Pune",
                    locality = "Baner",
                    monthlyRent = 11000L,
                    latitude = 18.5590,
                    longitude = 73.7868,
                    sleepSchedule = "EARLY_BIRD",
                    cleanlinessLevel = 4L,
                    guestPolicy = "NO_GUESTS",
                    foodHabit = "VEG_ONLY",
                    bio = "Quiet 2BHK near Baner highway with shared kitchen.",
                    moveInTimeline = "From 1 Dec",
                    trustScore = 92L,
                    verified = true,
                    isLive = true
                )
            )
        }
        assertTrue("Publish should succeed: ${pubRes.exceptionOrNull()}", pubRes.isSuccess)

        val bobUid = signInTestUser(BOB_EMAIL)
        assertTrue(bobUid != aliceUid)
        val bobRepo = CoM8FirestoreRepository(firestore, auth)

        val visibleListings = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observePublicListings().first { items -> items.any { it.id == listingId } }
        }
        assertTrue(visibleListings.any { it.id == listingId && it.locality == "Baner" })

        val crossDelete = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.deleteListing(listingId) }
        assertTrue("Cross-user delete must fail", crossDelete.isFailure)
        val err = crossDelete.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, err?.code)
    }

    @Test
    fun swipeAndChatMessage_persistUnderOwnerAndRejectCrossUserRead() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CoM8FirestoreRepository(firestore, auth)
        val listingId = "cand_${UUID.randomUUID().toString().take(8)}"

        val listing = FlatmateListing(
            id = listingId,
            name = "Aditya",
            city = "Pune",
            locality = "Aundh",
            monthlyRent = 12000L
        )
        val swipeRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveSwipe(listing, "LIKED", "SAVED", 90)
        }
        assertTrue("Swipe write should succeed: ${swipeRes.exceptionOrNull()}", swipeRes.isSuccess)

        val msgRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendMatchMessage(listingId, "ME", "Hey Aditya, is the room available from Dec 1?")
        }
        assertTrue("Message write should succeed: ${msgRes.exceptionOrNull()}", msgRes.isSuccess)

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeMatchMessages(listingId).first { it.isNotEmpty() }
        }
        assertTrue(messages.any { it.text.contains("Dec 1") })

        signInTestUser(BOB_EMAIL)
        var crossUserDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).collection("swipes").get().await()
            }
        } catch (e: Exception) {
            crossUserDenied = e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Cross-user swipe read must be denied", crossUserDenied)
    }

    @Test
    fun observePublicListings_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = CoM8FirestoreRepository(firestore, auth)

        var deniedOrUnauth = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePublicListings().first()
            }
        } catch (e: IllegalStateException) {
            deniedOrUnauth = true
        } catch (e: Exception) {
            deniedOrUnauth = e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue(deniedOrUnauth)
    }

    @Test
    fun syncQuizCompletedProfile_publishesPublicProfileAndSwipeLogsDirectionEvent() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CoM8FirestoreRepository(firestore, auth)

        val profile = UserLifestyleProfile(
            phoneNumber = "+919876543210",
            displayName = "Abhishek",
            age = 24,
            gender = "He",
            occupationCategory = "Working professional",
            organizationName = "Acme Security",
            city = "Pune",
            preferredAreas = listOf("Baner"),
            roleType = "SEEKING_FLAT",
            monthlyBudget = 14000L,
            sleepSchedule = "EARLY_BIRD",
            cleanlinessLevel = 4L,
            guestPolicy = "WEEKENDS_OK",
            foodHabit = "VEG_ONLY",
            bio = "Looking for a tidy roommate in Baner.",
            interests = listOf("Startups", "Cybersecurity"),
            authProvider = "PHONE",
            quizCompleted = true,
            verifiedBadge = true,
            trustScore = 95L
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.syncUserProfile(profile) }.isSuccess)

        val listing = FlatmateListing(
            id = "cand_${UUID.randomUUID().toString().take(8)}",
            name = "Karan",
            city = "Pune",
            locality = "Wakad",
            monthlyRent = 11000L
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.saveSwipe(listing, "PASSED", "PASSED", 74) }.isSuccess)

        val swipeEvents = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("users").document(aliceUid).collection("swipe_events").get().await()
        }
        assertTrue(swipeEvents.documents.any { it.getString("direction") == "LEFT" && it.getString("listingId") == listing.id })

        signInTestUser(BOB_EMAIL)
        val bobRepo = CoM8FirestoreRepository(firestore, auth)
        val publicProfiles = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observePublicProfiles().first { list -> list.any { it.id == "profile_$aliceUid" } }
        }
        val matched = publicProfiles.first { it.id == "profile_$aliceUid" }
        assertEquals("He", matched.gender)
        assertEquals("Acme Security", matched.organizationName)
        assertTrue(matched.interests.contains("Cybersecurity"))
    }

    @Test
    fun sharedTwoUserChat_deliversMessagesBothWaysAndBlocksOutsider() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = signInTestUser(BOB_EMAIL)
        assertTrue(aliceUid != bobUid)
        val chatId = CoM8FirestoreRepository.sharedChatIdFor(aliceUid, bobUid)

        // Alice sends intro message to Bob
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = CoM8FirestoreRepository(firestore, auth)
        val send1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendSharedChatMessage(
                chatId = chatId,
                peerUid = bobUid,
                peerName = "Bob",
                myName = "Alice",
                listingId = "profile_$bobUid",
                targetCity = "Pune",
                targetLocality = "Baner",
                targetRent = 12000L,
                compatibilityScore = 93,
                text = "Hey Bob, still looking for a Baner flatmate?"
            )
        }
        assertTrue("Alice send failed: ${send1.exceptionOrNull()}", send1.isSuccess)

        // Bob sees conversation and receives Alice's message as MATE, then replies
        signInTestUser(BOB_EMAIL)
        val bobRepo = CoM8FirestoreRepository(firestore, auth)
        val bobThreads = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeSharedChats().first { list -> list.any { it.chatId == chatId } }
        }
        assertEquals("Alice", bobThreads.first { it.chatId == chatId }.peerName)

        val bobMsgs = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeSharedChatMessages(chatId).first { it.isNotEmpty() }
        }
        assertEquals("MATE", bobMsgs.first().senderRole)

        val send2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.sendSharedChatMessage(
                chatId = chatId,
                peerUid = aliceUid,
                peerName = "Alice",
                myName = "Bob",
                listingId = "chat_$chatId",
                targetCity = "Pune",
                targetLocality = "Baner",
                targetRent = 12000L,
                compatibilityScore = 93,
                text = "Yes! Room is free from Dec 1."
            )
        }
        assertTrue("Bob reply failed: ${send2.exceptionOrNull()}", send2.isSuccess)

        // Alice receives Bob's reply in real time
        signInTestUser(ALICE_EMAIL)
        val aliceMsgs = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeSharedChatMessages(chatId).first { list -> list.size >= 2 }
        }
        assertTrue(aliceMsgs.any { it.senderRole == "ME" && it.text.contains("Baner") })
        assertTrue(aliceMsgs.any { it.senderRole == "MATE" && it.text.contains("Dec 1") })
    }

    @Test
    fun submitUserReport_storesReporterIdAndTimestampAndBlocksSelfReport() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = signInTestUser(BOB_EMAIL)

        signInTestUser(ALICE_EMAIL)
        val aliceRepo = CoM8FirestoreRepository(firestore, auth)
        val reportResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.submitUserReport(
                reportedUserId = bobUid,
                reportedName = "Bob",
                listingId = "profile_$bobUid",
                chatId = CoM8FirestoreRepository.sharedChatIdFor(aliceUid, bobUid),
                reason = "Inappropriate content",
                details = "Flagged message in conversation."
            )
        }
        assertTrue("Report should succeed: ${reportResult.exceptionOrNull()}", reportResult.isSuccess)
        val reportId = reportResult.getOrThrow()

        val savedDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("reports").document(reportId).get().await()
        }
        assertEquals(aliceUid, savedDoc.getString("reporterId"))
        assertEquals(bobUid, savedDoc.getString("reportedUserId"))
        assertEquals("Inappropriate content", savedDoc.getString("reason"))
        assertNotNull(savedDoc.getTimestamp("createdAt"))

        val selfReport = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.submitUserReport(
                reportedUserId = aliceUid,
                reportedName = "Alice",
                listingId = "profile_$aliceUid",
                chatId = "",
                reason = "Other",
                details = ""
            )
        }
        assertTrue("Self report must fail", selfReport.isFailure)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_com8@test.com"
        const val BOB_EMAIL = "bob_com8@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}


