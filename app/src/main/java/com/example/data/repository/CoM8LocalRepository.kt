package com.example.data.repository

import android.content.Context
import com.example.data.local.AppThemeMode
import com.example.data.local.CoM8Dao
import com.example.data.local.CoM8Database
import com.example.data.local.LocalListingEntity
import com.example.data.local.MatchMessageEntity
import com.example.data.local.SessionDataStore
import com.example.data.local.SwipeEntity
import com.example.data.local.UserProfileEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.model.FlatmateListing
import com.example.data.model.MatchMessage
import com.example.data.model.SwipeRecord
import com.example.data.model.UserLifestyleProfile
import com.example.data.model.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class CoM8LocalRepository(
    private val dao: CoM8Dao,
    private val sessionStore: SessionDataStore,
    private val firestoreRepository: CoM8FirestoreRepository? = null,
    private val db: CoM8Database? = null,
    private val appContext: Context? = null
) {
    constructor(context: Context) : this(
        dao = CoM8Database.getInstance(context).coM8Dao(),
        sessionStore = SessionDataStore(context.applicationContext),
        firestoreRepository = kotlin.runCatching { CoM8FirestoreRepository(context.applicationContext) }.getOrNull(),
        db = CoM8Database.getInstance(context),
        appContext = context.applicationContext
    )

    val activePhoneFlow: Flow<String?> = sessionStore.userIdFlow
    val themeModeFlow: Flow<AppThemeMode> = sessionStore.themeModeFlow
    val permissionsPromptedFlow: Flow<Boolean> = sessionStore.permissionsPromptedFlow

    fun currentAuthUid(): String? = firestoreRepository?.currentAuthUid()

    suspend fun setThemeMode(mode: AppThemeMode) {
        sessionStore.setThemeMode(mode)
    }

    suspend fun setPermissionsPrompted(prompted: Boolean) {
        sessionStore.setPermissionsPrompted(prompted)
    }

    suspend fun finalizeFirebasePhoneAuth(uid: String, rawPhoneNumber: String?): Result<String> {
        val userId = "phone_$uid"
        val cleanPhone = rawPhoneNumber?.trim().orEmpty()
        var existing = dao.getUserProfileOnce(userId)
        if (existing == null && firestoreRepository?.isSignedIn() == true) {
            val remote = firestoreRepository.getOwnProfile().getOrNull()
            if (remote != null && remote.quizCompleted) {
                val hydrated = remote.copy(phoneNumber = userId, authProvider = "PHONE")
                dao.upsertUserProfile(UserProfileEntity.fromDomain(hydrated))
                dao.upsertUserSettings(UserSettingsEntity(userId = userId))
                existing = dao.getUserProfileOnce(userId)
            }
        }
        if (existing == null) {
            val now = System.currentTimeMillis()
            val initialProfile = UserLifestyleProfile(
                phoneNumber = userId,
                authProvider = "PHONE",
                displayName = "",
                quizCompleted = false,
                verifiedBadge = true,
                trustScore = 90L,
                createdAt = now,
                updatedAt = now
            )
            dao.upsertUserProfile(UserProfileEntity.fromDomain(initialProfile))
            dao.upsertUserSettings(UserSettingsEntity(userId = userId))
            if (firestoreRepository?.isSignedIn() == true) {
                firestoreRepository.syncUserProfile(
                    initialProfile.copy(
                        phoneNumber = cleanPhone.ifBlank { userId },
                        displayName = "CoM8 Member"
                    )
                )
            }
        }
        if (appContext != null && firestoreRepository?.isSignedIn() == true) {
            firestoreRepository.recordAppInstallInfo(appContext, "PHONE")
        }
        sessionStore.saveUserId(userId)
        return Result.success(userId)
    }

    suspend fun finalizeMobileAuth(rawMobile: String): Result<String> {
        val userId = normalizeMobileId(rawMobile)
            ?: return Result.failure(
                IllegalArgumentException("Enter a valid mobile number (at least 10 digits).")
            )
        val existing = dao.getUserProfileOnce(userId)
        if (existing == null) {
            val now = System.currentTimeMillis()
            val initialProfile = UserLifestyleProfile(
                phoneNumber = userId,
                authProvider = "PHONE",
                displayName = "",
                quizCompleted = false,
                verifiedBadge = true,
                trustScore = 90L,
                createdAt = now,
                updatedAt = now
            )
            dao.upsertUserProfile(UserProfileEntity.fromDomain(initialProfile))
            dao.upsertUserSettings(UserSettingsEntity(userId = userId))
        }
        sessionStore.saveUserId(userId)
        return Result.success(userId)
    }

    suspend fun finalizeGoogleAuth(uid: String, email: String?, displayName: String?): Result<String> {
        val userId = "google_$uid"
        var existing = dao.getUserProfileOnce(userId)
        if (existing == null && firestoreRepository?.isSignedIn() == true) {
            val remote = firestoreRepository.getOwnProfile().getOrNull()
            if (remote != null && remote.quizCompleted) {
                val hydrated = remote.copy(
                    phoneNumber = userId,
                    email = email.orEmpty(),
                    authProvider = "GOOGLE"
                )
                dao.upsertUserProfile(UserProfileEntity.fromDomain(hydrated))
                dao.upsertUserSettings(UserSettingsEntity(userId = userId))
                existing = dao.getUserProfileOnce(userId)
            }
        }
        if (existing == null) {
            val now = System.currentTimeMillis()
            val firstName = displayName?.trim()?.substringBefore(' ').orEmpty()
            val initialProfile = UserLifestyleProfile(
                phoneNumber = userId,
                email = email.orEmpty(),
                authProvider = "GOOGLE",
                displayName = firstName,
                quizCompleted = false,
                verifiedBadge = true,
                trustScore = 90L,
                createdAt = now,
                updatedAt = now
            )
            dao.upsertUserProfile(UserProfileEntity.fromDomain(initialProfile))
            dao.upsertUserSettings(UserSettingsEntity(userId = userId))
            if (firestoreRepository?.isSignedIn() == true) {
                firestoreRepository.syncUserProfile(
                    initialProfile.copy(displayName = firstName.ifBlank { "CoM8 Member" })
                )
            }
        }
        if (appContext != null && firestoreRepository?.isSignedIn() == true) {
            firestoreRepository.recordAppInstallInfo(appContext, "GOOGLE")
        }
        sessionStore.saveUserId(userId)
        return Result.success(userId)
    }

    suspend fun signInWithMobile(rawDigits: String): Result<String> {
        val normalized = normalizeIndianMobile(rawDigits)
            ?: return Result.failure(IllegalArgumentException("Enter a valid 10-digit mobile number."))
        val existing = dao.getUserProfileOnce(normalized)
            ?: return Result.failure(
                IllegalStateException("Number not registered yet. Switch to Sign Up for instant entry.")
            )
        sessionStore.saveUserId(existing.phoneNumber)
        return Result.success(existing.phoneNumber)
    }

    suspend fun signUpWithMobile(rawDigits: String): Result<String> {
        val normalized = normalizeIndianMobile(rawDigits)
            ?: return Result.failure(IllegalArgumentException("Enter a valid 10-digit mobile number."))
        val existing = dao.getUserProfileOnce(normalized)
        if (existing != null) {
            return Result.failure(
                IllegalStateException("This mobile number is already registered. Switch to Sign In.")
            )
        }
        val now = System.currentTimeMillis()
        val initialProfile = UserLifestyleProfile(
            phoneNumber = normalized,
            authProvider = "PHONE",
            displayName = "CoM8 ${normalized.takeLast(4)}",
            quizCompleted = false,
            verifiedBadge = true,
            trustScore = 90L,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertUserProfile(UserProfileEntity.fromDomain(initialProfile))
        dao.upsertUserSettings(UserSettingsEntity(userId = normalized))
        sessionStore.saveUserId(normalized)
        return Result.success(normalized)
    }

    suspend fun signOut() {
        kotlin.runCatching { Firebase.auth.signOut() }
        withContext(Dispatchers.IO) {
            kotlin.runCatching { db?.clearAllTables() }
            appContext?.cacheDir?.let { deleteDirectoryContents(it) }
            appContext?.codeCacheDir?.let { deleteDirectoryContents(it) }
        }
        sessionStore.clearAll()
    }

    fun observeUserProfile(phoneNumber: String): Flow<UserLifestyleProfile?> =
        dao.observeUserProfile(phoneNumber).map { it?.toDomain() }

    suspend fun saveUserProfile(phoneNumber: String, profile: UserLifestyleProfile): Result<Unit> {
        return try {
            val existing = dao.getUserProfileOnce(phoneNumber)
            val now = System.currentTimeMillis()
            val toSave = profile.copy(
                phoneNumber = phoneNumber,
                email = profile.email.ifBlank { existing?.email.orEmpty() },
                authProvider = profile.authProvider.ifBlank { existing?.authProvider ?: "PHONE" },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            dao.upsertUserProfile(UserProfileEntity.fromDomain(toSave))
            if (firestoreRepository?.isSignedIn() == true) {
                firestoreRepository.syncUserProfile(toSave)
                if (appContext != null) {
                    firestoreRepository.recordAppInstallInfo(appContext, toSave.authProvider)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeLocalListings(): Flow<List<FlatmateListing>> {
        val localFlow = dao.observeLocalListings().map { list -> list.map { it.toDomain() } }
        val remoteRepo = firestoreRepository
        if (remoteRepo == null || !remoteRepo.isSignedIn()) {
            return localFlow
        }
        val remoteListingsFlow = remoteRepo.observePublicListings()
            .catch { error ->
                val isFirestoreError = generateSequence(error) { it.cause }
                    .any { it is FirebaseFirestoreException }
                if (!isFirestoreError && error is CancellationException) throw error
                emit(emptyList())
            }
        val remoteProfilesFlow = remoteRepo.observePublicProfiles()
            .catch { error ->
                val isFirestoreError = generateSequence(error) { it.cause }
                    .any { it is FirebaseFirestoreException }
                if (!isFirestoreError && error is CancellationException) throw error
                emit(emptyList())
            }
        return combine(localFlow, remoteListingsFlow, remoteProfilesFlow) { local, remoteListings, remoteProfiles ->
            val postOwners = (local + remoteListings).map { it.ownerPhone }.toSet()
            val dedupedProfiles = remoteProfiles.filter { it.ownerPhone !in postOwners }
            (local + remoteListings + dedupedProfiles).distinctBy { it.id }
        }
    }

    fun observeOwnListing(userId: String): Flow<FlatmateListing?> {
        val localFlow = dao.observeOwnListing(userId).map { it?.toDomain() }
        val remoteRepo = firestoreRepository
        val authUid = remoteRepo?.currentAuthUid()
        if (remoteRepo == null || !remoteRepo.isSignedIn() || authUid == null) {
            return localFlow
        }
        val remoteOwnFlow = remoteRepo.observePublicListings()
            .catch { error ->
                val isFirestoreError = generateSequence(error) { it.cause }
                    .any { it is FirebaseFirestoreException }
                if (!isFirestoreError && error is CancellationException) throw error
                emit(emptyList())
            }
            .map { listings -> listings.firstOrNull { it.ownerPhone == authUid } }
        return combine(localFlow, remoteOwnFlow) { local, remote ->
            local ?: remote
        }
    }

    suspend fun publishListing(phoneNumber: String, listing: FlatmateListing): Result<String> {
        return try {
            val id = listing.id.ifBlank { "local_${UUID.randomUUID().toString().take(8)}" }
            val entity = LocalListingEntity.fromDomain(
                listing.copy(id = id, ownerPhone = phoneNumber)
            )
            dao.upsertListing(entity)
            val existingSettings = dao.getUserSettingsOnce(phoneNumber) ?: UserSettingsEntity(userId = phoneNumber)
            dao.upsertUserSettings(
                existingSettings.copy(
                    postIsLive = listing.isLive,
                    postVisibility = listing.visibility,
                    hideExactAddress = listing.hideExactAddress,
                    messagesOnlyAfterMatching = listing.messagesOnlyAfterMatch,
                    showPhoneNumber = listing.showPhoneNumber,
                    pauseAfter30Days = listing.pauseAfter30Days
                )
            )
            if (firestoreRepository?.isSignedIn() == true) {
                firestoreRepository.publishListing(listing.copy(id = id))
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOwnListing(userId: String) {
        val own = dao.observeOwnListing(userId).first()
        dao.deleteOwnListing(userId)
        if (firestoreRepository?.isSignedIn() == true && own != null) {
            firestoreRepository.deleteListing(own.id)
        }
    }

    fun observeSettings(userId: String): Flow<UserSettings> =
        dao.observeUserSettings(userId).map { it?.toDomain() ?: UserSettings(userId = userId) }

    suspend fun saveSettings(userId: String, settings: UserSettings) {
        dao.upsertUserSettings(UserSettingsEntity.fromDomain(settings.copy(userId = userId)))
    }

    fun observeSwipes(phoneNumber: String): Flow<List<SwipeRecord>> {
        val localSwipes = dao.observeSwipes(phoneNumber).map { list -> list.map { it.toDomain() } }
        val remoteRepo = firestoreRepository
        if (remoteRepo == null || !remoteRepo.isSignedIn()) {
            return localSwipes
        }
        val remoteSwipes = remoteRepo.observeSwipes()
            .catch { error ->
                val isFirestoreError = generateSequence(error) { it.cause }
                    .any { it is FirebaseFirestoreException }
                if (!isFirestoreError && error is CancellationException) throw error
                emit(emptyList())
            }
        return combine(localSwipes, remoteSwipes) { local, remote ->
            (local + remote)
                .sortedByDescending { it.updatedAt }
                .distinctBy { it.listingId }
        }
    }

    suspend fun recordSwipe(
        phoneNumber: String,
        listing: FlatmateListing,
        decision: String,
        status: String,
        compatibilityScore: Int
    ): Result<Unit> {
        return try {
            val existing = dao.getSwipeOnce(phoneNumber, listing.id)
            val preview = existing?.lastMessagePreview?.takeIf { it.isNotBlank() }
                ?: "You matched. Say hi and share your move-in date."
            dao.upsertSwipe(
                SwipeEntity(
                    phoneNumber = phoneNumber,
                    listingId = listing.id,
                    decision = decision,
                    status = status,
                    targetName = listing.name,
                    targetCity = listing.city,
                    targetLocality = listing.locality,
                    targetRent = listing.monthlyRent,
                    compatibilityScore = compatibilityScore.toLong(),
                    unreadCount = existing?.unreadCount ?: 0,
                    lastMessagePreview = preview,
                    updatedAt = System.currentTimeMillis()
                )
            )
            if (firestoreRepository?.isSignedIn() == true) {
                firestoreRepository.saveSwipe(listing, decision, status, compatibilityScore)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearUnread(phoneNumber: String, listingId: String) {
        dao.clearUnread(phoneNumber, listingId)
    }

    suspend fun deleteSwipe(phoneNumber: String, listingId: String): Result<Unit> {
        return try {
            dao.deleteSwipe(phoneNumber, listingId)
            if (firestoreRepository?.isSignedIn() == true) {
                firestoreRepository.deleteSwipe(listingId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassedSwipes(phoneNumber: String) {
        val passed = dao.observeSwipes(phoneNumber).first().filter { it.decision == "PASSED" }
        dao.deletePassedSwipes(phoneNumber)
        if (firestoreRepository?.isSignedIn() == true) {
            passed.forEach { swipe ->
                firestoreRepository.deleteSwipe(swipe.listingId)
            }
        }
    }

    fun observeSharedChats(): Flow<List<CoM8FirestoreRepository.SharedChatThread>> {
        val remoteRepo = firestoreRepository
        if (remoteRepo == null || !remoteRepo.isSignedIn()) {
            return flowOf(emptyList())
        }
        return remoteRepo.observeSharedChats()
            .catch { error ->
                val isFirestoreError = generateSequence(error) { it.cause }
                    .any { it is FirebaseFirestoreException }
                if (!isFirestoreError && error is CancellationException) throw error
                emit(emptyList())
            }
    }

    fun observeMatchMessages(
        phoneNumber: String,
        listingId: String,
        peerUid: String? = null,
        chatIdHint: String? = null
    ): Flow<List<MatchMessage>> {
        val localMsgs = dao.observeMessages(phoneNumber, listingId).map { list -> list.map { it.toDomain() } }
        val remoteRepo = firestoreRepository
        if (remoteRepo == null || !remoteRepo.isSignedIn()) {
            return localMsgs
        }
        val myUid = remoteRepo.currentAuthUid()
        val cleanPeerUid = peerUid?.takeIf { it.isNotBlank() && it != myUid }
            ?: listingId.takeIf { it.startsWith("profile_") }?.removePrefix("profile_")
                ?.takeIf { it.isNotBlank() && it != myUid }
        val resolvedChatId = chatIdHint?.takeIf { it.isNotBlank() }
            ?: listingId.takeIf { it.startsWith("chat_") }?.removePrefix("chat_")?.takeIf { it.isNotBlank() }
            ?: if (myUid != null && !cleanPeerUid.isNullOrBlank()) {
                CoM8FirestoreRepository.sharedChatIdFor(myUid, cleanPeerUid)
            } else {
                null
            }
        val remoteMsgs = if (resolvedChatId != null) {
            remoteRepo.observeSharedChatMessages(resolvedChatId, listingId)
        } else {
            remoteRepo.observeMatchMessages(listingId)
        }.catch { error ->
            val isFirestoreError = generateSequence(error) { it.cause }
                .any { it is FirebaseFirestoreException }
            if (!isFirestoreError && error is CancellationException) throw error
            emit(emptyList())
        }
        return combine(localMsgs, remoteMsgs) { local, remote ->
            if (remote.isEmpty()) {
                local
            } else {
                val pendingLocal = local.filter { l ->
                    remote.none { r ->
                        r.senderRole == l.senderRole &&
                            r.text.trim() == l.text.trim() &&
                            kotlin.math.abs(r.createdAt - l.createdAt) < 30_000L
                    }
                }
                (remote + pendingLocal).sortedBy { it.createdAt }
            }
        }
    }

    suspend fun sendMatchMessage(
        phoneNumber: String,
        listingId: String,
        senderRole: String,
        text: String,
        listing: FlatmateListing? = null,
        myDisplayName: String = "CoM8 Member",
        compatibilityScore: Int = 90,
        peerUidOverride: String? = null,
        peerNameOverride: String? = null,
        chatIdOverride: String? = null
    ): Result<Long> {
        return try {
            val now = System.currentTimeMillis()
            val cleanText = text.trim()
            if (cleanText.isEmpty()) {
                return Result.failure(IllegalArgumentException("Message text cannot be blank"))
            }
            val id = dao.insertMessage(
                MatchMessageEntity(
                    phoneNumber = phoneNumber,
                    listingId = listingId,
                    senderRole = senderRole,
                    text = cleanText,
                    createdAt = now
                )
            )
            val existing = dao.getSwipeOnce(phoneNumber, listingId)
            if (existing != null) {
                dao.upsertSwipe(
                    existing.copy(
                        status = "INTRO_SENT",
                        lastMessagePreview = cleanText,
                        unreadCount = 0,
                        updatedAt = now
                    )
                )
            }
            val remoteRepo = firestoreRepository
            if (remoteRepo?.isSignedIn() == true) {
                val myUid = remoteRepo.currentAuthUid()
                val rawChatId = chatIdOverride?.takeIf { it.isNotBlank() }
                    ?: listingId.takeIf { it.startsWith("chat_") }?.removePrefix("chat_")
                val cleanPeerUid = peerUidOverride?.takeIf { it.isNotBlank() && it != myUid }
                    ?: listing?.ownerPhone?.takeIf {
                        it.isNotBlank() &&
                            it != myUid &&
                            !it.startsWith("phone_") &&
                            !it.startsWith("google_") &&
                            !it.startsWith("+")
                    }
                    ?: listingId.takeIf { it.startsWith("profile_") }?.removePrefix("profile_")
                        ?.takeIf { it.isNotBlank() && it != myUid }
                    ?: rawChatId?.split("__")?.firstOrNull { it.isNotBlank() && it != myUid }
                val resolvedChatId = rawChatId
                    ?: if (myUid != null && !cleanPeerUid.isNullOrBlank()) {
                        CoM8FirestoreRepository.sharedChatIdFor(myUid, cleanPeerUid)
                    } else {
                        null
                    }
                if (resolvedChatId != null && !cleanPeerUid.isNullOrBlank() && myUid != null) {
                    val remoteResult = remoteRepo.sendSharedChatMessage(
                        chatId = resolvedChatId,
                        peerUid = cleanPeerUid,
                        peerName = peerNameOverride?.takeIf { it.isNotBlank() }
                            ?: listing?.name?.takeIf { it.isNotBlank() }
                            ?: existing?.targetName?.takeIf { it.isNotBlank() }
                            ?: "CoM8 Mate",
                        myName = myDisplayName,
                        listingId = listingId,
                        targetCity = listing?.city ?: existing?.targetCity ?: "Pune",
                        targetLocality = listing?.locality ?: existing?.targetLocality ?: "Baner",
                        targetRent = listing?.monthlyRent ?: existing?.targetRent ?: 12000L,
                        compatibilityScore = compatibilityScore,
                        text = cleanText
                    )
                    if (remoteResult.isFailure) {
                        return Result.failure(
                            remoteResult.exceptionOrNull()
                                ?: IllegalStateException("Failed to send message")
                        )
                    }
                } else {
                    val remoteResult = remoteRepo.sendMatchMessage(listingId, senderRole, cleanText)
                    if (remoteResult.isFailure) {
                        return Result.failure(
                            remoteResult.exceptionOrNull()
                                ?: IllegalStateException("Failed to send message")
                        )
                    }
                }
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportUser(
        listingId: String,
        reportedName: String,
        reason: String,
        details: String,
        reportedUserId: String? = null,
        chatId: String? = null
    ): Result<String> {
        val remoteRepo = firestoreRepository
        if (remoteRepo == null || !remoteRepo.isSignedIn()) {
            return Result.failure(IllegalStateException("Please sign in to submit a report."))
        }
        val myUid = remoteRepo.currentAuthUid()
        val rawChatId = chatId?.takeIf { it.isNotBlank() }
            ?: listingId.takeIf { it.startsWith("chat_") }?.removePrefix("chat_")
            ?: ""
        val resolvedPeerUid = reportedUserId?.takeIf { it.isNotBlank() && it != myUid }
            ?: listingId.takeIf { it.startsWith("profile_") }?.removePrefix("profile_")
                ?.takeIf { it.isNotBlank() && it != myUid }
            ?: rawChatId.split("__").firstOrNull { it.isNotBlank() && it != myUid }
            ?: ""
        val resolvedChatId = rawChatId.ifBlank {
            if (myUid != null && resolvedPeerUid.isNotBlank()) {
                CoM8FirestoreRepository.sharedChatIdFor(myUid, resolvedPeerUid)
            } else {
                ""
            }
        }
        return remoteRepo.submitUserReport(
            reportedUserId = resolvedPeerUid,
            reportedName = reportedName,
            listingId = listingId,
            chatId = resolvedChatId,
            reason = reason,
            details = details
        )
    }

    private fun deleteDirectoryContents(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                deleteDirectoryContents(child)
            }
            child.delete()
        }
    }

    companion object {
        fun normalizeMobileId(raw: String): String? {
            val trimmed = raw.trim()
            val hasPlus = trimmed.startsWith("+")
            val digits = trimmed.filter { it.isDigit() }
            if (digits.length !in 10..15) return null
            return when {
                hasPlus -> "+$digits"
                digits.length == 10 -> "+91$digits"
                else -> "+$digits"
            }
        }

        fun normalizeIndianMobile(raw: String): String? {
            val digits = raw.filter { it.isDigit() }
            val tenDigits = when {
                digits.length == 10 -> digits
                digits.length == 12 && digits.startsWith("91") -> digits.drop(2)
                else -> return null
            }
            return if (tenDigits.length == 10 && tenDigits.first() in '6'..'9') {
                "+91$tenDigits"
            } else {
                null
            }
        }
    }
}
