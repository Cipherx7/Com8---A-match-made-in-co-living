package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FlatmateListing
import com.example.data.model.MatchMessage
import com.example.data.model.SwipeRecord
import com.example.data.model.UserLifestyleProfile
import com.example.data.model.UserSettings

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val phoneNumber: String,
    val email: String = "",
    val authProvider: String = "PHONE",
    val displayName: String,
    val age: Int = 23,
    val gender: String = "He",
    val occupationCategory: String = "Working professional",
    val organizationName: String = "",
    val photoUri: String,
    val currentResidence: String,
    val preferredCitiesCsv: String,
    val city: String,
    val preferredAreasCsv: String = "",
    val roleType: String,
    val monthlyBudget: Long,
    val budgetMin: Long = 8000L,
    val budgetMax: Long = 12000L,
    val smoking: String = "Never",
    val drinking: String = "Never",
    val smokingDrinkingDealbreaker: Boolean = true,
    val sleepSchedule: String,
    val cleanlinessLevel: Long,
    val cleanlinessTag: String = "Tidy",
    val guestPolicy: String,
    val foodHabit: String,
    val moveInTimeline: String = "Within 2 weeks",
    val roomType: String = "Private room",
    val bio: String,
    val interestsCsv: String = "Startups,Fitness",
    val quizCompleted: Boolean,
    val verifiedBadge: Boolean,
    val selfieVerified: Boolean = false,
    val idVerified: Boolean = false,
    val trustScore: Long,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): UserLifestyleProfile {
        val parsedCities = preferredCitiesCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(city.ifBlank { "Pune" }) }
        val parsedAreas = preferredAreasCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val parsedInterests = interestsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return UserLifestyleProfile(
            phoneNumber = phoneNumber,
            email = email,
            authProvider = authProvider,
            displayName = displayName,
            age = age,
            gender = gender,
            occupationCategory = occupationCategory,
            organizationName = organizationName,
            photoUri = photoUri,
            currentResidence = currentResidence,
            preferredCities = parsedCities,
            city = parsedCities.firstOrNull() ?: city.ifBlank { "Pune" },
            preferredAreas = parsedAreas,
            roleType = roleType,
            monthlyBudget = monthlyBudget,
            budgetMin = budgetMin,
            budgetMax = budgetMax,
            smoking = smoking,
            drinking = drinking,
            smokingDrinkingDealbreaker = smokingDrinkingDealbreaker,
            sleepSchedule = sleepSchedule,
            cleanlinessLevel = cleanlinessLevel,
            cleanlinessTag = cleanlinessTag,
            guestPolicy = guestPolicy,
            foodHabit = foodHabit,
            moveInTimeline = moveInTimeline,
            roomType = roomType,
            bio = bio,
            interests = parsedInterests,
            quizCompleted = quizCompleted,
            verifiedBadge = verifiedBadge,
            selfieVerified = selfieVerified,
            idVerified = idVerified,
            trustScore = trustScore,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(profile: UserLifestyleProfile): UserProfileEntity {
            val cities = profile.preferredCities
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOf(profile.city.ifBlank { "Pune" }) }
            return UserProfileEntity(
                phoneNumber = profile.phoneNumber,
                email = profile.email,
                authProvider = profile.authProvider,
                displayName = profile.displayName,
                age = profile.age,
                gender = profile.gender,
                occupationCategory = profile.occupationCategory,
                organizationName = profile.organizationName,
                photoUri = profile.photoUri,
                currentResidence = profile.currentResidence,
                preferredCitiesCsv = cities.joinToString(","),
                city = cities.first(),
                preferredAreasCsv = profile.preferredAreas.joinToString(","),
                roleType = profile.roleType,
                monthlyBudget = profile.monthlyBudget,
                budgetMin = profile.budgetMin,
                budgetMax = profile.budgetMax,
                smoking = profile.smoking,
                drinking = profile.drinking,
                smokingDrinkingDealbreaker = profile.smokingDrinkingDealbreaker,
                sleepSchedule = profile.sleepSchedule,
                cleanlinessLevel = profile.cleanlinessLevel,
                cleanlinessTag = profile.cleanlinessTag,
                guestPolicy = profile.guestPolicy,
                foodHabit = profile.foodHabit,
                moveInTimeline = profile.moveInTimeline,
                roomType = profile.roomType,
                bio = profile.bio,
                interestsCsv = profile.interests.joinToString(","),
                quizCompleted = profile.quizCompleted,
                verifiedBadge = profile.verifiedBadge,
                selfieVerified = profile.selfieVerified,
                idVerified = profile.idVerified,
                trustScore = profile.trustScore,
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt
            )
        }
    }
}

@Entity(tableName = "local_listings")
data class LocalListingEntity(
    @PrimaryKey val id: String,
    val ownerPhone: String,
    val name: String,
    val age: Long,
    val gender: String = "He",
    val occupation: String,
    val organizationName: String = "",
    val roleType: String,
    val roleSummaryBadge: String = "Looking for a roommate",
    val flatTypeLabel: String = "2BHK",
    val roomsFreeLabel: String = "1 of 2",
    val city: String,
    val locality: String,
    val monthlyRent: Long,
    val latitude: Double,
    val longitude: Double,
    val smoking: String = "Never",
    val drinking: String = "Never",
    val sleepSchedule: String,
    val cleanlinessLevel: Long,
    val cleanlinessTag: String = "Tidy",
    val guestPolicy: String,
    val foodHabit: String,
    val bio: String,
    val moveInTimeline: String,
    val trustScore: Long,
    val verified: Boolean,
    val photoUri: String = "",
    val flatPhotoUrisCsv: String = "",
    val visibility: String = "Verified people",
    val hideExactAddress: Boolean = true,
    val messagesOnlyAfterMatch: Boolean = true,
    val showPhoneNumber: Boolean = false,
    val pauseAfter30Days: Boolean = true,
    val isLive: Boolean = true,
    val interestsCsv: String = "",
    val createdAt: Long
) {
    fun toDomain(): FlatmateListing = FlatmateListing(
        id = id,
        ownerPhone = ownerPhone,
        name = name,
        age = age,
        gender = gender,
        occupation = occupation,
        organizationName = organizationName,
        roleType = roleType,
        roleSummaryBadge = roleSummaryBadge,
        flatTypeLabel = flatTypeLabel,
        roomsFreeLabel = roomsFreeLabel,
        city = city,
        locality = locality,
        monthlyRent = monthlyRent,
        rentRangeLabel = "₹${monthlyRent / 1000}k per month",
        latitude = latitude,
        longitude = longitude,
        smoking = smoking,
        drinking = drinking,
        sleepSchedule = sleepSchedule,
        cleanlinessLevel = cleanlinessLevel,
        cleanlinessTag = cleanlinessTag,
        guestPolicy = guestPolicy,
        foodHabit = foodHabit,
        bio = bio,
        moveInTimeline = moveInTimeline,
        trustScore = trustScore,
        verified = verified,
        avatarInitials = name.trim().take(1).uppercase().ifEmpty { "C" },
        photoUri = photoUri,
        flatPhotoUris = flatPhotoUrisCsv.split("|").filter { it.isNotBlank() },
        visibility = visibility,
        hideExactAddress = hideExactAddress,
        messagesOnlyAfterMatch = messagesOnlyAfterMatch,
        showPhoneNumber = showPhoneNumber,
        pauseAfter30Days = pauseAfter30Days,
        isLive = isLive,
        interests = interestsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
    )

    companion object {
        fun fromDomain(listing: FlatmateListing): LocalListingEntity = LocalListingEntity(
            id = listing.id,
            ownerPhone = listing.ownerPhone,
            name = listing.name,
            age = listing.age,
            gender = listing.gender,
            occupation = listing.occupation,
            organizationName = listing.organizationName,
            roleType = listing.roleType,
            roleSummaryBadge = listing.roleSummaryBadge,
            flatTypeLabel = listing.flatTypeLabel,
            roomsFreeLabel = listing.roomsFreeLabel,
            city = listing.city,
            locality = listing.locality,
            monthlyRent = listing.monthlyRent,
            latitude = listing.latitude,
            longitude = listing.longitude,
            smoking = listing.smoking,
            drinking = listing.drinking,
            sleepSchedule = listing.sleepSchedule,
            cleanlinessLevel = listing.cleanlinessLevel,
            cleanlinessTag = listing.cleanlinessTag,
            guestPolicy = listing.guestPolicy,
            foodHabit = listing.foodHabit,
            bio = listing.bio,
            moveInTimeline = listing.moveInTimeline,
            trustScore = listing.trustScore,
            verified = listing.verified,
            photoUri = listing.photoUri,
            flatPhotoUrisCsv = listing.flatPhotoUris.joinToString("|"),
            visibility = listing.visibility,
            hideExactAddress = listing.hideExactAddress,
            messagesOnlyAfterMatch = listing.messagesOnlyAfterMatch,
            showPhoneNumber = listing.showPhoneNumber,
            pauseAfter30Days = listing.pauseAfter30Days,
            isLive = listing.isLive,
            interestsCsv = listing.interests.joinToString(","),
            createdAt = System.currentTimeMillis()
        )
    }
}

@Entity(
    tableName = "swipes",
    primaryKeys = ["phoneNumber", "listingId"]
)
data class SwipeEntity(
    val phoneNumber: String,
    val listingId: String,
    val decision: String,
    val status: String,
    val targetName: String,
    val targetCity: String,
    val targetLocality: String,
    val targetRent: Long,
    val compatibilityScore: Long,
    val unreadCount: Int = 0,
    val lastMessagePreview: String = "",
    val updatedAt: Long
) {
    fun toDomain(): SwipeRecord = SwipeRecord(
        phoneNumber = phoneNumber,
        listingId = listingId,
        decision = decision,
        status = status,
        targetName = targetName,
        targetCity = targetCity,
        targetLocality = targetLocality,
        targetRent = targetRent,
        compatibilityScore = compatibilityScore,
        unreadCount = unreadCount,
        lastMessagePreview = lastMessagePreview,
        updatedAt = updatedAt
    )
}

@Entity(tableName = "match_messages")
data class MatchMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val phoneNumber: String,
    val listingId: String,
    val senderRole: String,
    val text: String,
    val createdAt: Long
) {
    fun toDomain(): MatchMessage = MatchMessage(
        id = id,
        phoneNumber = phoneNumber,
        listingId = listingId,
        senderRole = senderRole,
        text = text,
        createdAt = createdAt
    )
}

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val userId: String,
    val postIsLive: Boolean = true,
    val postVisibility: String = "Verified people",
    val hideExactAddress: Boolean = true,
    val messagesOnlyAfterMatching: Boolean = true,
    val showPhoneNumber: Boolean = false,
    val pauseAfter30Days: Boolean = true,
    val notifyNewMatches: Boolean = true,
    val notifyMessages: Boolean = true,
    val blockedPeopleCsv: String = ""
) {
    fun toDomain(): UserSettings = UserSettings(
        userId = userId,
        postIsLive = postIsLive,
        postVisibility = postVisibility,
        hideExactAddress = hideExactAddress,
        messagesOnlyAfterMatching = messagesOnlyAfterMatching,
        showPhoneNumber = showPhoneNumber,
        pauseAfter30Days = pauseAfter30Days,
        notifyNewMatches = notifyNewMatches,
        notifyMessages = notifyMessages,
        blockedPeople = blockedPeopleCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
    )

    companion object {
        fun fromDomain(settings: UserSettings): UserSettingsEntity = UserSettingsEntity(
            userId = settings.userId,
            postIsLive = settings.postIsLive,
            postVisibility = settings.postVisibility,
            hideExactAddress = settings.hideExactAddress,
            messagesOnlyAfterMatching = settings.messagesOnlyAfterMatching,
            showPhoneNumber = settings.showPhoneNumber,
            pauseAfter30Days = settings.pauseAfter30Days,
            notifyNewMatches = settings.notifyNewMatches,
            notifyMessages = settings.notifyMessages,
            blockedPeopleCsv = settings.blockedPeople.joinToString(",")
        )
    }
}
