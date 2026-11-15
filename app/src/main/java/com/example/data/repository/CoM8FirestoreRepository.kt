package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.CoM8Catalog
import com.example.data.model.FlatmateListing
import com.example.data.model.MatchMessage
import com.example.data.model.RoleType
import com.example.data.model.SwipeRecord
import com.example.data.model.UserLifestyleProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

class CoM8FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    data class SharedChatThread(
        val chatId: String,
        val peerUid: String,
        val peerName: String,
        val listingId: String,
        val targetCity: String,
        val targetLocality: String,
        val targetRent: Long,
        val compatibilityScore: Long,
        val lastMessageText: String,
        val lastSenderId: String,
        val updatedAt: Long
    )
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    fun currentAuthUid(): String? = auth.currentUser?.uid

    fun isSignedIn(): Boolean = auth.currentUser != null

    suspend fun syncUserProfile(profile: UserLifestyleProfile): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val existing = try {
                docRef.get().await()
            } catch (_: FirebaseFirestoreException) {
                null
            }
            val cleanCity = profile.city.trim().ifBlank { "Pune" }.take(40)
            val cleanGender = if (profile.gender in CoM8Catalog.genderOptions) profile.gender else "He"
            val cleanOrg = profile.organizationName.trim().take(80)
            val cleanAreas = profile.preferredAreas.filter { it.isNotBlank() }.joinToString(",").take(200)
            val cleanInterests = profile.interests.filter { it.isNotBlank() }.joinToString(",").take(300)
            val cleanProvider = if (profile.authProvider == "GOOGLE") "GOOGLE" else "PHONE"

            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "displayName" to profile.displayName.trim().ifBlank { "CoM8 Member" }.take(80),
                "age" to profile.age.coerceIn(18, 65),
                "gender" to cleanGender,
                "occupationCategory" to profile.occupationCategory.trim().ifBlank { "Working professional" }.take(80),
                "organizationName" to cleanOrg,
                "city" to cleanCity,
                "preferredAreasCsv" to cleanAreas,
                "roleType" to profile.roleType,
                "monthlyBudget" to profile.monthlyBudget.coerceIn(2000L, 200000L),
                "smoking" to profile.smoking.take(30),
                "drinking" to profile.drinking.take(30),
                "sleepSchedule" to profile.sleepSchedule,
                "cleanlinessLevel" to profile.cleanlinessLevel.coerceIn(1L, 5L),
                "guestPolicy" to profile.guestPolicy,
                "foodHabit" to profile.foodHabit,
                "moveInTimeline" to profile.moveInTimeline.trim().ifBlank { "Within 2 weeks" }.take(60),
                "bio" to profile.bio.take(400),
                "interestsCsv" to cleanInterests,
                "authProvider" to cleanProvider,
                "phoneNumber" to profile.phoneNumber.take(128),
                "quizCompleted" to profile.quizCompleted,
                "verifiedBadge" to profile.verifiedBadge,
                "trustScore" to profile.trustScore.coerceIn(0L, 100L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existing == null || !existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                val createdAt = existing.getTimestamp("createdAt")
                if (createdAt != null) {
                    payload["createdAt"] = createdAt
                }
                docRef.update(
                    mapOf(
                        "displayName" to payload["displayName"],
                        "age" to payload["age"],
                        "gender" to payload["gender"],
                        "occupationCategory" to payload["occupationCategory"],
                        "organizationName" to payload["organizationName"],
                        "city" to payload["city"],
                        "preferredAreasCsv" to payload["preferredAreasCsv"],
                        "roleType" to payload["roleType"],
                        "monthlyBudget" to payload["monthlyBudget"],
                        "smoking" to payload["smoking"],
                        "drinking" to payload["drinking"],
                        "sleepSchedule" to payload["sleepSchedule"],
                        "cleanlinessLevel" to payload["cleanlinessLevel"],
                        "guestPolicy" to payload["guestPolicy"],
                        "foodHabit" to payload["foodHabit"],
                        "moveInTimeline" to payload["moveInTimeline"],
                        "bio" to payload["bio"],
                        "interestsCsv" to payload["interestsCsv"],
                        "authProvider" to payload["authProvider"],
                        "phoneNumber" to payload["phoneNumber"],
                        "quizCompleted" to payload["quizCompleted"],
                        "verifiedBadge" to payload["verifiedBadge"],
                        "trustScore" to payload["trustScore"],
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (profile.quizCompleted) {
                val existingPublic = try {
                    publicRef.get().await()
                } catch (_: FirebaseFirestoreException) {
                    null
                }
                val locality = profile.preferredAreas.firstOrNull()?.takeIf { it.isNotBlank() } ?: cleanCity
                val publicPayload = hashMapOf<String, Any>(
                    "ownerId" to uid,
                    "displayName" to payload["displayName"]!!,
                    "age" to payload["age"]!!,
                    "gender" to cleanGender,
                    "occupation" to payload["occupationCategory"]!!,
                    "organizationName" to cleanOrg,
                    "roleType" to profile.roleType,
                    "city" to cleanCity,
                    "locality" to locality.take(80),
                    "monthlyBudget" to payload["monthlyBudget"]!!,
                    "smoking" to payload["smoking"]!!,
                    "drinking" to payload["drinking"]!!,
                    "sleepSchedule" to profile.sleepSchedule,
                    "cleanlinessLevel" to payload["cleanlinessLevel"]!!,
                    "guestPolicy" to profile.guestPolicy,
                    "foodHabit" to profile.foodHabit,
                    "moveInTimeline" to payload["moveInTimeline"]!!,
                    "bio" to profile.bio.take(400),
                    "interestsCsv" to cleanInterests,
                    "trustScore" to payload["trustScore"]!!,
                    "verified" to profile.verifiedBadge,
                    "isDiscoverable" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (existingPublic == null || !existingPublic.exists()) {
                    publicPayload["createdAt"] = FieldValue.serverTimestamp()
                    publicRef.set(publicPayload).await()
                } else {
                    publicRef.update(
                        publicPayload.filterKeys { it != "ownerId" }
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordAppInstallInfo(context: Context, authProvider: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val pkgName = context.packageName.ifBlank { "com.example" }.take(128)
        val installDocId = sanitizeId("android_${pkgName}")
        val docRef = db.collection("users").document(uid).collection("installs").document(installDocId)
        return try {
            val pkgInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        pkgName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(pkgName, 0)
                }
            } catch (_: Exception) {
                null
            }
            val versionName = pkgInfo?.versionName?.ifBlank { "1.0" }?.take(40) ?: "1.0"
            val versionCode = if (pkgInfo != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkgInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkgInfo.versionCode.toLong()
                }
            } else 1L
            val firstInstallTime = pkgInfo?.firstInstallTime ?: System.currentTimeMillis()
            val lastUpdateTime = pkgInfo?.lastUpdateTime ?: System.currentTimeMillis()
            val cleanProvider = if (authProvider == "GOOGLE") "GOOGLE" else "PHONE"
            val locale = Locale.getDefault().toLanguageTag().take(32)

            val existing = try {
                docRef.get().await()
            } catch (_: FirebaseFirestoreException) {
                null
            }

            if (existing == null || !existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "packageName" to pkgName,
                    "versionName" to versionName,
                    "versionCode" to versionCode.coerceAtLeast(1L),
                    "androidSdk" to Build.VERSION.SDK_INT.coerceIn(21, 100),
                    "deviceBrand" to (Build.BRAND ?: "Android").take(60),
                    "deviceModel" to (Build.MODEL ?: "Device").take(80),
                    "authProvider" to cleanProvider,
                    "locale" to locale,
                    "firstInstallTime" to firstInstallTime.coerceAtLeast(0L),
                    "lastUpdateTime" to lastUpdateTime.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "versionName" to versionName,
                        "versionCode" to versionCode.coerceAtLeast(1L),
                        "androidSdk" to Build.VERSION.SDK_INT.coerceIn(21, 100),
                        "authProvider" to cleanProvider,
                        "locale" to locale,
                        "lastUpdateTime" to lastUpdateTime.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(): Flow<UserLifestyleProfile?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap -> snap.toUserLifestyleProfile() }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun getOwnProfile(): Result<UserLifestyleProfile?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            Result.success(snap.toUserLifestyleProfile())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun publishListing(listing: FlatmateListing): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanDocId = sanitizeId(listing.id.ifBlank { "listing_$uid" })
        val docRef = db.collection("listings").document(cleanDocId)
        return try {
            val existing = try {
                docRef.get().await()
            } catch (_: FirebaseFirestoreException) {
                null
            }
            val cleanGender = if (listing.gender in CoM8Catalog.genderOptions) listing.gender else "He"
            val payload = hashMapOf<String, Any>(
                "ownerId" to uid,
                "name" to listing.name.trim().ifBlank { "CoM8 Host" }.take(80),
                "age" to listing.age.coerceIn(18L, 65L),
                "gender" to cleanGender,
                "occupation" to listing.occupation.trim().ifBlank { "Working professional" }.take(80),
                "organizationName" to listing.organizationName.trim().take(80),
                "roleType" to listing.roleType,
                "roleSummaryBadge" to listing.roleSummaryBadge.take(80),
                "city" to listing.city.trim().ifBlank { "Pune" }.take(40),
                "locality" to listing.locality.trim().ifBlank { "Baner" }.take(80),
                "monthlyRent" to listing.monthlyRent.coerceIn(2000L, 200000L),
                "latitude" to listing.latitude.coerceIn(-90.0, 90.0),
                "longitude" to listing.longitude.coerceIn(-180.0, 180.0),
                "smoking" to listing.smoking.take(30),
                "drinking" to listing.drinking.take(30),
                "sleepSchedule" to listing.sleepSchedule,
                "cleanlinessLevel" to listing.cleanlinessLevel.coerceIn(1L, 5L),
                "guestPolicy" to listing.guestPolicy,
                "foodHabit" to listing.foodHabit,
                "bio" to listing.bio.trim().ifBlank { "Looking for a compatible roommate." }.take(500),
                "moveInTimeline" to listing.moveInTimeline.trim().ifBlank { "Immediate" }.take(60),
                "interestsCsv" to listing.interests.filter { it.isNotBlank() }.joinToString(",").take(300),
                "trustScore" to listing.trustScore.coerceIn(0L, 100L),
                "verified" to listing.verified,
                "isPublic" to listing.isLive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existing == null || !existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to payload["name"],
                        "age" to payload["age"],
                        "gender" to payload["gender"],
                        "occupation" to payload["occupation"],
                        "organizationName" to payload["organizationName"],
                        "roleType" to payload["roleType"],
                        "roleSummaryBadge" to payload["roleSummaryBadge"],
                        "city" to payload["city"],
                        "locality" to payload["locality"],
                        "monthlyRent" to payload["monthlyRent"],
                        "latitude" to payload["latitude"],
                        "longitude" to payload["longitude"],
                        "smoking" to payload["smoking"],
                        "drinking" to payload["drinking"],
                        "sleepSchedule" to payload["sleepSchedule"],
                        "cleanlinessLevel" to payload["cleanlinessLevel"],
                        "guestPolicy" to payload["guestPolicy"],
                        "foodHabit" to payload["foodHabit"],
                        "bio" to payload["bio"],
                        "moveInTimeline" to payload["moveInTimeline"],
                        "interestsCsv" to payload["interestsCsv"],
                        "isPublic" to payload["isPublic"],
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(cleanDocId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanId = sanitizeId(listingId.ifBlank { "listing_$uid" })
        val docRef = db.collection("listings").document(cleanId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observePublicListings(): Flow<List<FlatmateListing>> = flow {
        val uid = requireUserId()
        val query = db.collection("listings").whereEqualTo("isPublic", true)
        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toFlatmateListing() } }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, "listings")
                    }
                    throw error
                }
        )
    }

    fun observePublicProfiles(): Flow<List<FlatmateListing>> = flow {
        val uid = requireUserId()
        val query = db.collection("public_profiles").whereEqualTo("isDiscoverable", true)
        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toPublicProfileListing() } }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, "public_profiles")
                    }
                    throw error
                }
        )
    }

    suspend fun saveSwipe(
        listing: FlatmateListing,
        decision: String,
        status: String,
        compatibilityScore: Int
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanListingId = sanitizeId(listing.id)
        val direction = if (decision == "LIKED") "RIGHT" else "LEFT"
        val docRef = db.collection("users").document(uid).collection("swipes").document(cleanListingId)
        val eventId = "evt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val eventRef = db.collection("users").document(uid).collection("swipe_events").document(eventId)
        return try {
            val existing = try {
                docRef.get().await()
            } catch (_: FirebaseFirestoreException) {
                null
            }
            if (existing == null || !existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "listingId" to cleanListingId,
                    "decision" to decision,
                    "status" to status,
                    "direction" to direction,
                    "targetName" to listing.name.trim().ifBlank { "CoM8 Mate" }.take(80),
                    "targetCity" to listing.city.trim().ifBlank { "Pune" }.take(40),
                    "targetLocality" to listing.locality.trim().ifBlank { "Baner" }.take(80),
                    "targetRent" to listing.monthlyRent.coerceIn(0L, 200000L),
                    "compatibilityScore" to compatibilityScore.toLong().coerceIn(0L, 100L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "decision" to decision,
                        "status" to status,
                        "direction" to direction,
                        "compatibilityScore" to compatibilityScore.toLong().coerceIn(0L, 100L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Record every swipe action + direction in swipe_events audit collection
            val eventPayload = mapOf(
                "userId" to uid,
                "listingId" to cleanListingId,
                "direction" to direction,
                "decision" to decision,
                "targetName" to listing.name.trim().ifBlank { "CoM8 Mate" }.take(80),
                "compatibilityScore" to compatibilityScore.toLong().coerceIn(0L, 100L),
                "createdAt" to FieldValue.serverTimestamp()
            )
            eventRef.set(eventPayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSwipe(listingId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanListingId = sanitizeId(listingId)
        val docRef = db.collection("users").document(uid).collection("swipes").document(cleanListingId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeSwipes(): Flow<List<SwipeRecord>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("swipes")
        emitAll(
            colRef.snapshots()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toSwipeRecord() } }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendMatchMessage(
        listingId: String,
        senderRole: String,
        text: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanListingId = sanitizeId(listingId)
        val cleanMsgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val swipeRef = db.collection("users").document(uid).collection("swipes").document(cleanListingId)
        val msgRef = swipeRef.collection("messages").document(cleanMsgId)
        return try {
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty()) { "Message text cannot be blank" }
            val payload = mapOf(
                "userId" to uid,
                "senderRole" to senderRole,
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            msgRef.set(payload).await()
            Result.success(cleanMsgId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    fun observeMatchMessages(listingId: String): Flow<List<MatchMessage>> = flow {
        val uid = requireUserId()
        val cleanListingId = sanitizeId(listingId)
        val colRef = db.collection("users").document(uid)
            .collection("swipes").document(cleanListingId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        val text = doc.getString("text") ?: return@mapNotNull null
                        MatchMessage(
                            id = doc.id.hashCode().toLong(),
                            phoneNumber = uid,
                            listingId = cleanListingId,
                            senderRole = doc.getString("senderRole") ?: "ME",
                            text = text,
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(
                            toLog,
                            OperationType.LIST,
                            "users/$uid/swipes/$cleanListingId/messages"
                        )
                    }
                    throw error
                }
        )
    }

    suspend fun sendSharedChatMessage(
        chatId: String,
        peerUid: String,
        peerName: String,
        myName: String,
        listingId: String,
        targetCity: String,
        targetLocality: String,
        targetRent: Long,
        compatibilityScore: Int,
        text: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanPeerUid = peerUid.trim()
        if (cleanPeerUid.isBlank() || cleanPeerUid == uid) {
            return Result.failure(IllegalArgumentException("Cannot start shared chat without a peer user ID."))
        }
        val cleanChatId = sanitizeId(chatId)
        val cleanListingId = sanitizeId(listingId.ifBlank { "chat_$cleanChatId" })
        val cleanMsgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val chatRef = db.collection("chats").document(cleanChatId)
        val msgRef = chatRef.collection("messages").document(cleanMsgId)
        return try {
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty()) { "Message text cannot be blank" }
            val participantIds = listOf(uid, cleanPeerUid).sorted()
            val cleanMyName = myName.trim().ifBlank { "CoM8 Member" }.take(80)
            val cleanPeerName = peerName.trim().ifBlank { "CoM8 Mate" }.take(80)
            val existingChat = try {
                chatRef.get().await()
            } catch (_: FirebaseFirestoreException) {
                null
            }
            val batch = db.batch()
            if (existingChat == null || !existingChat.exists()) {
                val threadPayload = mapOf(
                    "chatId" to cleanChatId,
                    "participantIds" to participantIds,
                    "initiatorId" to uid,
                    "initiatorName" to cleanMyName,
                    "listingId" to cleanListingId,
                    "targetUserId" to cleanPeerUid,
                    "targetName" to cleanPeerName,
                    "targetCity" to targetCity.trim().ifBlank { "Pune" }.take(40),
                    "targetLocality" to targetLocality.trim().ifBlank { "Baner" }.take(80),
                    "targetRent" to targetRent.coerceIn(0L, 200000L),
                    "compatibilityScore" to compatibilityScore.toLong().coerceIn(0L, 100L),
                    "lastMessageText" to cleanText,
                    "lastSenderId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(chatRef, threadPayload)
            } else {
                batch.update(
                    chatRef,
                    mapOf(
                        "lastMessageText" to cleanText,
                        "lastSenderId" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val msgPayload = mapOf(
                "messageId" to cleanMsgId,
                "chatId" to cleanChatId,
                "participantIds" to participantIds,
                "senderId" to uid,
                "senderName" to cleanMyName,
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            batch.set(msgRef, msgPayload)
            batch.commit().await()
            Result.success(cleanMsgId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    fun observeSharedChats(): Flow<List<SharedChatThread>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("chats").whereArrayContains("participantIds", uid)
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        @Suppress("UNCHECKED_CAST")
                        val participants = (doc.get("participantIds") as? List<*>)
                            ?.mapNotNull { it as? String }
                            .orEmpty()
                        val peerUid = participants.firstOrNull { it != uid } ?: return@mapNotNull null
                        val initiatorId = doc.getString("initiatorId").orEmpty()
                        val peerName = if (initiatorId == uid) {
                            doc.getString("targetName").orEmpty().ifBlank { "CoM8 Mate" }
                        } else {
                            doc.getString("initiatorName").orEmpty().ifBlank { "CoM8 Mate" }
                        }
                        SharedChatThread(
                            chatId = doc.getString("chatId") ?: doc.id,
                            peerUid = peerUid,
                            peerName = peerName,
                            listingId = doc.getString("listingId") ?: "chat_${doc.id}",
                            targetCity = doc.getString("targetCity") ?: "Pune",
                            targetLocality = doc.getString("targetLocality") ?: "Baner",
                            targetRent = doc.getLong("targetRent") ?: 12000L,
                            compatibilityScore = doc.getLong("compatibilityScore") ?: 90L,
                            lastMessageText = doc.getString("lastMessageText").orEmpty(),
                            lastSenderId = doc.getString("lastSenderId").orEmpty(),
                            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                                ?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }.sortedByDescending { it.updatedAt }
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, "chats")
                    }
                    throw error
                }
        )
    }

    fun observeSharedChatMessages(chatId: String, listingIdForUi: String = "chat_$chatId"): Flow<List<MatchMessage>> = flow {
        val uid = requireUserId()
        val cleanChatId = sanitizeId(chatId)
        val colRef = db.collection("chats").document(cleanChatId)
            .collection("messages")
            .whereArrayContains("participantIds", uid)
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        val text = doc.getString("text") ?: return@mapNotNull null
                        val senderId = doc.getString("senderId").orEmpty()
                        MatchMessage(
                            id = doc.id.hashCode().toLong(),
                            phoneNumber = uid,
                            listingId = listingIdForUi,
                            senderRole = if (senderId == uid) "ME" else "MATE",
                            text = text,
                            createdAt = doc.getTimestamp(
                                "createdAt",
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }.sortedBy { it.createdAt }
                }
                .catch { error ->
                    val firestoreError = unwrapFirestoreException(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, "chats/$cleanChatId/messages")
                    }
                    throw error
                }
        )
    }

    suspend fun submitUserReport(
        reportedUserId: String,
        reportedName: String,
        listingId: String,
        chatId: String,
        reason: String,
        details: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanReportedUid = reportedUserId.trim().take(128)
        if (cleanReportedUid == uid) {
            return Result.failure(IllegalArgumentException("Cannot report your own account."))
        }
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("reports").document(reportId)
        return try {
            val payload = mapOf(
                "reportId" to reportId,
                "reporterId" to uid,
                "reportedUserId" to cleanReportedUid,
                "reportedName" to reportedName.trim().ifBlank { "CoM8 Mate" }.take(80),
                "listingId" to sanitizeId(listingId),
                "chatId" to chatId.trim().take(128),
                "reason" to reason.trim().ifBlank { "Inappropriate content" }.take(80),
                "details" to details.trim().take(500),
                "status" to "OPEN",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(reportId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private fun DocumentSnapshot.toUserLifestyleProfile(): UserLifestyleProfile? {
        if (!exists()) return null
        val uid = getString("userId") ?: id
        val areas = getString("preferredAreasCsv").orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val interests = getString("interestsCsv").orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val city = getString("city") ?: "Pune"
        val budget = getLong("monthlyBudget") ?: 12000L
        return UserLifestyleProfile(
            phoneNumber = getString("phoneNumber")?.takeIf { it.isNotBlank() } ?: uid,
            authProvider = getString("authProvider") ?: "GOOGLE",
            displayName = getString("displayName").orEmpty(),
            age = (getLong("age") ?: 23L).toInt(),
            gender = getString("gender") ?: "He",
            occupationCategory = getString("occupationCategory") ?: "Working professional",
            organizationName = getString("organizationName").orEmpty(),
            city = city,
            preferredCities = listOf(city),
            preferredAreas = areas.ifEmpty { CoM8Catalog.areasForCity(city).take(2) },
            roleType = getString("roleType") ?: "SEEKING_FLAT",
            monthlyBudget = budget,
            budgetMin = (budget - 4000L).coerceAtLeast(5000L),
            budgetMax = budget,
            smoking = getString("smoking") ?: "Never",
            drinking = getString("drinking") ?: "Never",
            sleepSchedule = getString("sleepSchedule") ?: "EARLY_BIRD",
            cleanlinessLevel = getLong("cleanlinessLevel") ?: 4L,
            cleanlinessTag = when ((getLong("cleanlinessLevel") ?: 4L).toInt()) {
                5 -> "Spotless"
                4 -> "Tidy"
                else -> "Relaxed"
            },
            guestPolicy = getString("guestPolicy") ?: "WEEKENDS_OK",
            foodHabit = getString("foodHabit") ?: "VEG_ONLY",
            moveInTimeline = getString("moveInTimeline") ?: "Within 2 weeks",
            bio = getString("bio").orEmpty(),
            interests = interests.ifEmpty { listOf("Startups", "Fitness") },
            quizCompleted = getBoolean("quizCompleted") ?: false,
            verifiedBadge = getBoolean("verifiedBadge") ?: true,
            trustScore = getLong("trustScore") ?: 90L,
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
            updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toFlatmateListing(): FlatmateListing? {
        if (!exists()) return null
        val ownerId = getString("ownerId").orEmpty()
        val roleCode = getString("roleType") ?: RoleType.SEEKING_FLAT.code
        val rent = getLong("monthlyRent") ?: 11000L
        val locality = getString("locality").orEmpty()
        val interests = getString("interestsCsv").orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return FlatmateListing(
            id = id,
            ownerPhone = ownerId,
            name = getString("name").orEmpty(),
            age = getLong("age") ?: 24L,
            gender = getString("gender") ?: "He",
            occupation = getString("occupation").orEmpty(),
            organizationName = getString("organizationName").orEmpty(),
            roleType = roleCode,
            roleSummaryBadge = getString("roleSummaryBadge")?.takeIf { it.isNotBlank() }
                ?: "${RoleType.fromCode(roleCode).badge}${if (locality.isNotBlank()) " · $locality" else ""}",
            city = getString("city") ?: "Pune",
            locality = locality,
            monthlyRent = rent,
            rentRangeLabel = "₹${(rent / 1000).coerceAtLeast(3)}k per month",
            latitude = getDouble("latitude") ?: 18.5590,
            longitude = getDouble("longitude") ?: 73.7868,
            smoking = getString("smoking") ?: "Never",
            drinking = getString("drinking") ?: "Never",
            sleepSchedule = getString("sleepSchedule") ?: "EARLY_BIRD",
            cleanlinessLevel = getLong("cleanlinessLevel") ?: 4L,
            cleanlinessTag = when ((getLong("cleanlinessLevel") ?: 4L).toInt()) {
                5 -> "Spotless"
                4 -> "Tidy"
                else -> "Relaxed"
            },
            guestPolicy = getString("guestPolicy") ?: "WEEKENDS_OK",
            foodHabit = getString("foodHabit") ?: "VEG_ONLY",
            bio = getString("bio").orEmpty(),
            moveInTimeline = getString("moveInTimeline") ?: "Immediate",
            trustScore = getLong("trustScore") ?: 90L,
            verified = getBoolean("verified") ?: true,
            avatarInitials = getString("name").orEmpty().trim().take(1).uppercase().ifEmpty { "C" },
            isLive = getBoolean("isPublic") ?: true,
            interests = interests
        )
    }

    private fun DocumentSnapshot.toPublicProfileListing(): FlatmateListing? {
        if (!exists()) return null
        val ownerId = getString("ownerId") ?: id
        val name = getString("displayName").orEmpty().ifBlank { return null }
        val city = getString("city") ?: "Pune"
        val locality = getString("locality").orEmpty().ifBlank { city }
        val rent = getLong("monthlyBudget") ?: 12000L
        val preset = CoM8Catalog.localityPresets.firstOrNull {
            it.locality.equals(locality, ignoreCase = true) || it.city.equals(city, ignoreCase = true)
        }
        val roleCode = getString("roleType") ?: RoleType.SEEKING_FLAT.code
        val interests = getString("interestsCsv").orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return FlatmateListing(
            id = "profile_$ownerId",
            ownerPhone = ownerId,
            name = name,
            age = getLong("age") ?: 23L,
            gender = getString("gender") ?: "He",
            occupation = getString("occupation") ?: "Working professional",
            organizationName = getString("organizationName").orEmpty(),
            roleType = roleCode,
            roleSummaryBadge = "${RoleType.fromCode(roleCode).badge} · $locality",
            flatTypeLabel = "Roommate",
            roomsFreeLabel = getString("moveInTimeline") ?: "Flexible",
            city = city,
            locality = locality,
            monthlyRent = rent,
            rentRangeLabel = "₹${(rent / 1000).coerceAtLeast(3)}k per month",
            latitude = preset?.latitude ?: 18.5590,
            longitude = preset?.longitude ?: 73.7868,
            smoking = getString("smoking") ?: "Never",
            drinking = getString("drinking") ?: "Never",
            sleepSchedule = getString("sleepSchedule") ?: "EARLY_BIRD",
            cleanlinessLevel = getLong("cleanlinessLevel") ?: 4L,
            cleanlinessTag = when ((getLong("cleanlinessLevel") ?: 4L).toInt()) {
                5 -> "Spotless"
                4 -> "Tidy"
                else -> "Relaxed"
            },
            guestPolicy = getString("guestPolicy") ?: "WEEKENDS_OK",
            foodHabit = getString("foodHabit") ?: "VEG_ONLY",
            bio = getString("bio").orEmpty().ifBlank { "Looking for a compatible roommate in $locality, $city." },
            moveInTimeline = getString("moveInTimeline") ?: "Within 2 weeks",
            trustScore = getLong("trustScore") ?: 92L,
            verified = getBoolean("verified") ?: true,
            avatarInitials = name.trim().take(1).uppercase().ifEmpty { "C" },
            isLive = getBoolean("isDiscoverable") ?: true,
            interests = interests
        )
    }

    private fun DocumentSnapshot.toSwipeRecord(): SwipeRecord? {
        if (!exists()) return null
        val uid = getString("userId").orEmpty()
        return SwipeRecord(
            phoneNumber = uid,
            listingId = getString("listingId") ?: id,
            decision = getString("decision") ?: "LIKED",
            status = getString("status") ?: "SAVED",
            targetName = getString("targetName").orEmpty(),
            targetCity = getString("targetCity").orEmpty(),
            targetLocality = getString("targetLocality").orEmpty(),
            targetRent = getLong("targetRent") ?: 0L,
            compatibilityScore = getLong("compatibilityScore") ?: 88L,
            updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    companion object {
        fun sanitizeId(raw: String): String {
            val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            return cleaned.take(128).ifBlank { "id_${UUID.randomUUID().toString().take(8)}" }
        }

        fun sharedChatIdFor(uidA: String, uidB: String): String {
            val pair = listOf(sanitizeId(uidA), sanitizeId(uidB)).sorted()
            return "${pair[0]}__${pair[1]}".take(128)
        }
    }
}
