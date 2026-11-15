package com.example.data.model

import com.example.R
import kotlin.math.abs

enum class RoleType(val code: String, val label: String, val badge: String) {
    SEEKING_FLAT("SEEKING_FLAT", "Looking for a roommate", "Looking for a roommate"),
    HAS_FLAT("HAS_FLAT", "Team up for a place", "Team up for a place"),
    PG_MATE("PG_MATE", "PG / Co-Live", "PG / Co-Live");

    companion object {
        fun fromCode(code: String): RoleType = entries.firstOrNull { it.code == code } ?: SEEKING_FLAT
    }
}

enum class SleepSchedule(val code: String, val label: String, val shortLabel: String) {
    EARLY_BIRD("EARLY_BIRD", "Early riser (6am–10pm)", "Early riser"),
    BALANCED("BALANCED", "Flexible schedule", "Flexible"),
    NIGHT_OWL("NIGHT_OWL", "Night owl (11am–2am)", "Night owl");

    companion object {
        fun fromCode(code: String): SleepSchedule = entries.firstOrNull { it.code == code } ?: EARLY_BIRD
    }
}

enum class GuestPolicy(val code: String, val label: String) {
    NO_GUESTS("NO_GUESTS", "Quiet / No overnight"),
    WEEKENDS_OK("WEEKENDS_OK", "Weekends cool"),
    CHILL_ANYTIME("CHILL_ANYTIME", "Friends anytime");

    companion object {
        fun fromCode(code: String): GuestPolicy = entries.firstOrNull { it.code == code } ?: WEEKENDS_OK
    }
}

enum class FoodHabit(val code: String, val label: String) {
    VEG_ONLY("VEG_ONLY", "Pure Veg"),
    EGGETARIAN("EGGETARIAN", "Eggetarian"),
    NON_VEG_OK("NON_VEG_OK", "All Foods OK");

    companion object {
        fun fromCode(code: String): FoodHabit = entries.firstOrNull { it.code == code } ?: NON_VEG_OK
    }
}

enum class ComparisonStatus {
    MATCH,
    CLOSE,
    DIFFERS
}

data class TraitComparison(
    val trait: String,
    val status: ComparisonStatus,
    val label: String
)

data class UserLifestyleProfile(
    val phoneNumber: String = "",
    val email: String = "",
    val authProvider: String = "PHONE",
    val displayName: String = "",
    val age: Int = 23,
    val gender: String = "He",
    val occupationCategory: String = "Working professional",
    val organizationName: String = "",
    val photoUri: String = "",
    val currentResidence: String = "Nashik",
    val preferredCities: List<String> = listOf("Pune"),
    val city: String = "Pune",
    val preferredAreas: List<String> = listOf("Baner", "Aundh"),
    val roleType: String = RoleType.SEEKING_FLAT.code,
    val monthlyBudget: Long = 12000L,
    val budgetMin: Long = 8000L,
    val budgetMax: Long = 12000L,
    val smoking: String = "Never",
    val drinking: String = "Never",
    val smokingDrinkingDealbreaker: Boolean = true,
    val sleepSchedule: String = SleepSchedule.EARLY_BIRD.code,
    val cleanlinessLevel: Long = 4L,
    val cleanlinessTag: String = "Tidy",
    val guestPolicy: String = GuestPolicy.WEEKENDS_OK.code,
    val foodHabit: String = FoodHabit.VEG_ONLY.code,
    val moveInTimeline: String = "Within 2 weeks",
    val roomType: String = "Private room",
    val bio: String = "",
    val interests: List<String> = listOf("Startups", "Fitness"),
    val quizCompleted: Boolean = false,
    val verifiedBadge: Boolean = true,
    val selfieVerified: Boolean = false,
    val idVerified: Boolean = false,
    val trustScore: Long = 90L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun profileStrengthPercent(): Int {
        var strength = 40
        if (displayName.isNotBlank() && city.isNotBlank()) strength += 15
        if (preferredAreas.isNotEmpty()) strength += 10
        if (bio.trim().length >= 20) strength += 10
        if (photoUri.isNotBlank()) strength += 10
        if (selfieVerified) strength += 5
        if (idVerified) strength += 10
        return strength.coerceIn(40, 100)
    }

    fun livingStyleTags(): List<String> = buildList {
        add(if (smoking == "Never") "Non-smoker" else "Smokes ${smoking.lowercase()}")
        add(if (drinking == "Never") "Doesn't drink" else "Drinks ${drinking.lowercase()}")
        add(SleepSchedule.fromCode(sleepSchedule).shortLabel)
        add(cleanlinessTag)
        if (city.isNotBlank()) add(city)
    }
}

data class FlatmateListing(
    val id: String = "",
    val ownerPhone: String = "",
    val name: String = "",
    val age: Long = 24L,
    val gender: String = "He",
    val occupation: String = "",
    val organizationName: String = "",
    val roleType: String = RoleType.SEEKING_FLAT.code,
    val roleSummaryBadge: String = "Looking for a roommate",
    val flatTypeLabel: String = "2BHK",
    val roomsFreeLabel: String = "1 room free",
    val city: String = "Pune",
    val locality: String = "",
    val monthlyRent: Long = 11000L,
    val rentRangeLabel: String = "₹9k to ₹12k per month",
    val latitude: Double = 18.5590,
    val longitude: Double = 73.7868,
    val smoking: String = "Never",
    val drinking: String = "Never",
    val sleepSchedule: String = SleepSchedule.EARLY_BIRD.code,
    val cleanlinessLevel: Long = 4L,
    val cleanlinessTag: String = "Tidy",
    val guestPolicy: String = GuestPolicy.NO_GUESTS.code,
    val foodHabit: String = FoodHabit.VEG_ONLY.code,
    val bio: String = "",
    val moveInTimeline: String = "From 1 Dec",
    val trustScore: Long = 92L,
    val verified: Boolean = true,
    val prompts: List<Pair<String, String>> = emptyList(),
    val avatarInitials: String = "",
    val accentColorHex: Long = 0xFFD7EAE4,
    val avatarTextHex: Long = 0xFF0C7A63,
    val photoResId: Int = 0,
    val photoUri: String = "",
    val flatPhotoLabels: List<String> = listOf("Living room", "Your room", "Kitchen"),
    val flatPhotoUris: List<String> = emptyList(),
    val visibility: String = "Verified people",
    val hideExactAddress: Boolean = true,
    val messagesOnlyAfterMatch: Boolean = true,
    val showPhoneNumber: Boolean = false,
    val pauseAfter30Days: Boolean = true,
    val isLive: Boolean = true,
    val interests: List<String> = emptyList()
) {
    fun traitPills(): List<String> = buildList {
        add(if (smoking == "Never") "Non-smoker" else "Smokes ${smoking.lowercase()}")
        add(if (drinking == "Never") "Doesn't drink" else "Drinks ${drinking.lowercase()}")
        add(SleepSchedule.fromCode(sleepSchedule).shortLabel)
        add(cleanlinessTag)
    }
}

data class SwipeRecord(
    val phoneNumber: String = "",
    val listingId: String = "",
    val decision: String = "LIKED",
    val status: String = "SAVED",
    val targetName: String = "",
    val targetCity: String = "",
    val targetLocality: String = "",
    val targetRent: Long = 0L,
    val compatibilityScore: Long = 88L,
    val unreadCount: Int = 0,
    val lastMessagePreview: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class MatchMessage(
    val id: Long = 0L,
    val phoneNumber: String = "",
    val listingId: String = "",
    val senderRole: String = "ME",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class UserSettings(
    val userId: String = "",
    val postIsLive: Boolean = true,
    val postVisibility: String = "Verified people",
    val hideExactAddress: Boolean = true,
    val messagesOnlyAfterMatching: Boolean = true,
    val showPhoneNumber: Boolean = false,
    val pauseAfter30Days: Boolean = true,
    val notifyNewMatches: Boolean = true,
    val notifyMessages: Boolean = true,
    val blockedPeople: List<String> = emptyList()
)

data class AiCompatibilityReport(
    val matchScore: Int,
    val headline: String,
    val strengths: List<String>,
    val dealBreakers: List<String>,
    val icebreakerPrompt: String
)

data class LocalityPreset(
    val city: String,
    val locality: String,
    val latitude: Double,
    val longitude: Double,
    val typicalRent: Long
)

object CompatibilityCalculator {
    fun computeScore(profile: UserLifestyleProfile?, candidate: FlatmateListing): Int {
        if (profile == null) return 92
        var score = 46
        score += if (profile.smoking == candidate.smoking) 12 else if (profile.smoking == "Socially" || candidate.smoking == "Socially") 6 else 0
        score += if (profile.drinking == candidate.drinking) 10 else if (profile.drinking == "Socially" || candidate.drinking == "Socially") 5 else 0
        score += when {
            profile.sleepSchedule == candidate.sleepSchedule -> 12
            profile.sleepSchedule == SleepSchedule.BALANCED.code || candidate.sleepSchedule == SleepSchedule.BALANCED.code -> 8
            else -> 3
        }
        val cleanDiff = abs(profile.cleanlinessLevel - candidate.cleanlinessLevel).toInt()
        score += (10 - cleanDiff * 3).coerceAtLeast(2)
        val inBudget = candidate.monthlyRent in (profile.budgetMin - 1000L)..(profile.budgetMax + 1500L)
        score += if (inBudget) 10 else 4
        return score.coerceIn(52, 96)
    }

    fun computeWhyFitSummary(profile: UserLifestyleProfile?, candidate: FlatmateListing): String {
        val smokeDrinkMatch = profile == null || (profile.smoking == candidate.smoking && profile.drinking == candidate.drinking)
        val sleepMatch = profile == null || profile.sleepSchedule == candidate.sleepSchedule
        val guestMatch = profile != null && profile.guestPolicy == candidate.guestPolicy
        return buildString {
            if (smokeDrinkMatch && sleepMatch) {
                append("You both want a smoke-free, alcohol-free home and ${SleepSchedule.fromCode(candidate.sleepSchedule).shortLabel.lowercase()} routine.")
            } else {
                append("Strong budget and locality fit in ${candidate.locality}, ${candidate.city}.")
            }
            if (!guestMatch) {
                append(" You differ on guests, so it's worth a chat.")
            }
        }
    }

    fun computeTraitComparisons(
        profile: UserLifestyleProfile?,
        candidate: FlatmateListing
    ): List<TraitComparison> {
        val userSmoking = profile?.smoking ?: "Never"
        val userDrinking = profile?.drinking ?: "Never"
        val userSleep = profile?.sleepSchedule ?: SleepSchedule.EARLY_BIRD.code
        val userClean = profile?.cleanlinessLevel ?: 4L
        val userGuests = profile?.guestPolicy ?: GuestPolicy.WEEKENDS_OK.code

        return listOf(
            TraitComparison(
                trait = "Smoking",
                status = if (userSmoking == candidate.smoking) ComparisonStatus.MATCH else ComparisonStatus.DIFFERS,
                label = if (userSmoking == candidate.smoking) "Match" else "Differs"
            ),
            TraitComparison(
                trait = "Drinking",
                status = if (userDrinking == candidate.drinking) ComparisonStatus.MATCH else ComparisonStatus.DIFFERS,
                label = if (userDrinking == candidate.drinking) "Match" else "Differs"
            ),
            TraitComparison(
                trait = "Sleep schedule",
                status = when {
                    userSleep == candidate.sleepSchedule -> ComparisonStatus.MATCH
                    userSleep == SleepSchedule.BALANCED.code || candidate.sleepSchedule == SleepSchedule.BALANCED.code -> ComparisonStatus.CLOSE
                    else -> ComparisonStatus.DIFFERS
                },
                label = when {
                    userSleep == candidate.sleepSchedule -> "Match"
                    userSleep == SleepSchedule.BALANCED.code || candidate.sleepSchedule == SleepSchedule.BALANCED.code -> "Close"
                    else -> "Differs"
                }
            ),
            TraitComparison(
                trait = "Cleanliness",
                status = when {
                    userClean == candidate.cleanlinessLevel -> ComparisonStatus.MATCH
                    abs(userClean - candidate.cleanlinessLevel) <= 1L -> ComparisonStatus.CLOSE
                    else -> ComparisonStatus.DIFFERS
                },
                label = when {
                    userClean == candidate.cleanlinessLevel -> "Match"
                    abs(userClean - candidate.cleanlinessLevel) <= 1L -> "Close"
                    else -> "Differs"
                }
            ),
            TraitComparison(
                trait = "Guests",
                status = if (userGuests == candidate.guestPolicy) ComparisonStatus.MATCH else ComparisonStatus.DIFFERS,
                label = if (userGuests == candidate.guestPolicy) "Match" else "Differs"
            )
        )
    }

    fun computeSharedTraits(profile: UserLifestyleProfile?, candidate: FlatmateListing): List<String> = buildList {
        if (candidate.smoking == "Never" && (profile == null || profile.smoking == "Never")) {
            add("Smoke-free home")
        }
        if (profile == null || profile.sleepSchedule == candidate.sleepSchedule) {
            add(SleepSchedule.fromCode(candidate.sleepSchedule).shortLabel)
        }
        if (profile == null || candidate.monthlyRent <= profile.budgetMax + 1500L) {
            add("Budget overlap")
        }
        if (isEmpty()) add("${candidate.city} locality fit")
    }

    fun computeWhyMatched(profile: UserLifestyleProfile?, candidate: FlatmateListing): List<String> {
        val reasons = mutableListOf<String>()
        val targetCities = profile?.preferredCities?.filter { it.isNotBlank() }.orEmpty()
        if (targetCities.any { it.equals(candidate.city, ignoreCase = true) }) {
            reasons.add("Located in ${candidate.locality}, ${candidate.city} — right in your preferred city list.")
        } else {
            reasons.add("Prime neighborhood in ${candidate.locality}, ${candidate.city} (${candidate.moveInTimeline}).")
        }
        if (profile != null) {
            reasons.add("Monthly rent of ₹${candidate.monthlyRent} aligns with your ₹${profile.budgetMin}–₹${profile.budgetMax} range.")
            if (profile.sleepSchedule == candidate.sleepSchedule) {
                reasons.add("You both follow the ${SleepSchedule.fromCode(candidate.sleepSchedule).shortLabel} routine.")
            }
            if (abs(profile.cleanlinessLevel - candidate.cleanlinessLevel) <= 1L) {
                reasons.add("Aligned cleanliness expectations (${candidate.cleanlinessTag}).")
            }
        }
        return reasons
    }
}

object CoM8Catalog {
    val supportedCities = listOf("All Cities", "Pune", "Mumbai", "Bengaluru", "Hyderabad", "Delhi NCR")
    val selectableCities = listOf("Pune", "Mumbai", "Bengaluru", "Hyderabad", "Delhi NCR")
    val genderOptions = listOf("He", "She", "Other")
    val interestOptions = listOf("Startups", "Fitness", "Music", "Cooking", "Reading", "Travel")

    val cityAreasMap: Map<String, List<String>> = mapOf(
        "Pune" to listOf("Baner", "Aundh", "Wakad", "Hinjewadi", "Kothrud", "Viman Nagar", "Hadapsar"),
        "Mumbai" to listOf("Bandra West", "Powai", "Andheri West", "Lower Parel", "Goregaon"),
        "Bengaluru" to listOf("Koramangala", "Indiranagar", "HSR Layout", "Whitefield", "Bellandur"),
        "Hyderabad" to listOf("Gachibowli", "Jubilee Hills", "Madhapur", "Kondapur"),
        "Delhi NCR" to listOf("Hauz Khas", "Gurugram Phase 3", "Saket", "Noida Sec 62")
    )

    fun areasForCity(city: String): List<String> {
        val clean = city.substringBefore(',').trim()
        return cityAreasMap.entries.firstOrNull {
            it.key.equals(clean, ignoreCase = true) || clean.contains(it.key, ignoreCase = true)
        }?.value ?: cityAreasMap.getValue("Pune")
    }

    val presetAvatars = listOf(
        R.drawable.img_flatmate_ananya_1794592209987,
        R.drawable.img_flatmate_rohan_1794592210309,
        R.drawable.img_flatmate_kavya_1794592210341,
        R.drawable.img_flatmate_siddharth_1794592244629
    )

    val localityPresets = listOf(
        LocalityPreset("Pune", "Baner", 18.5590, 73.7868, 11000L),
        LocalityPreset("Pune", "Aundh", 18.5580, 73.8075, 11500L),
        LocalityPreset("Pune", "Kothrud", 18.5074, 73.8077, 10500L),
        LocalityPreset("Pune", "Viman Nagar", 18.5679, 73.9143, 14500L),
        LocalityPreset("Mumbai", "Bandra West", 19.0596, 72.8295, 24000L),
        LocalityPreset("Mumbai", "Powai", 19.1176, 72.9060, 21000L),
        LocalityPreset("Bengaluru", "Koramangala", 12.9352, 77.6245, 16500L),
        LocalityPreset("Bengaluru", "Indiranagar", 12.9784, 77.6408, 18500L),
        LocalityPreset("Bengaluru", "HSR Layout", 12.9121, 77.6446, 15000L),
        LocalityPreset("Hyderabad", "Gachibowli", 17.4401, 78.3489, 13500L),
        LocalityPreset("Delhi NCR", "Hauz Khas", 28.5494, 77.2001, 17000L)
    )

    val curatedCandidates: List<FlatmateListing> = listOf(
        FlatmateListing(
            id = "com8_pune_rohan",
            ownerPhone = "curated_host_0",
            name = "Rohan",
            age = 24,
            occupation = "Product intern",
            roleType = RoleType.HAS_FLAT.code,
            roleSummaryBadge = "Has a flat · 1 room free",
            flatTypeLabel = "2BHK",
            roomsFreeLabel = "1 room free",
            city = "Pune",
            locality = "Baner",
            monthlyRent = 11000,
            rentRangeLabel = "₹9k to ₹12k per month",
            latitude = 18.5590,
            longitude = 73.7868,
            smoking = "Never",
            drinking = "Never",
            sleepSchedule = SleepSchedule.EARLY_BIRD.code,
            cleanlinessLevel = 5,
            cleanlinessTag = "Tidy",
            guestPolicy = GuestPolicy.NO_GUESTS.code,
            foodHabit = FoodHabit.VEG_ONLY.code,
            bio = "Calm 2BHK off Baner High Street. Early-riser weekday routine, zero smoking or alcohol indoors.",
            moveInTimeline = "From 1 Dec",
            trustScore = 92,
            verified = true,
            avatarInitials = "R",
            accentColorHex = 0xFFD7EAE4,
            avatarTextHex = 0xFF0C7A63,
            flatPhotoLabels = listOf("Living room", "Your room", "Kitchen")
        ),
        FlatmateListing(
            id = "com8_pune_aditya",
            ownerPhone = "curated_host_1",
            name = "Aditya",
            age = 25,
            occupation = "Backend engineer",
            roleType = RoleType.HAS_FLAT.code,
            roleSummaryBadge = "Has a flat · 1 room free",
            flatTypeLabel = "2BHK",
            roomsFreeLabel = "1 room free",
            city = "Pune",
            locality = "Aundh",
            monthlyRent = 11500,
            rentRangeLabel = "₹10k to ₹13k per month",
            latitude = 18.5580,
            longitude = 73.8075,
            smoking = "Never",
            drinking = "Never",
            sleepSchedule = SleepSchedule.EARLY_BIRD.code,
            cleanlinessLevel = 4,
            cleanlinessTag = "Tidy",
            guestPolicy = GuestPolicy.WEEKENDS_OK.code,
            foodHabit = FoodHabit.VEG_ONLY.code,
            bio = "Quiet lane near Parihar Chowk. High-speed fiber, cook in the morning, split utilities equally.",
            moveInTimeline = "Immediate",
            trustScore = 91,
            verified = true,
            avatarInitials = "A",
            accentColorHex = 0xFFF3DFB3,
            avatarTextHex = 0xFF7D530C,
            flatPhotoLabels = listOf("Living room", "Bedroom", "Balcony")
        ),
        FlatmateListing(
            id = "com8_pune_karan",
            ownerPhone = "curated_host_2",
            name = "Karan",
            age = 23,
            occupation = "Design associate",
            roleType = RoleType.SEEKING_FLAT.code,
            roleSummaryBadge = "Needs a flat · Buddy up",
            flatTypeLabel = "2BHK",
            roomsFreeLabel = "Looking together",
            city = "Pune",
            locality = "Kothrud",
            monthlyRent = 10500,
            rentRangeLabel = "₹8k to ₹12k per month",
            latitude = 18.5074,
            longitude = 73.8077,
            smoking = "Never",
            drinking = "Never",
            sleepSchedule = SleepSchedule.EARLY_BIRD.code,
            cleanlinessLevel = 4,
            cleanlinessTag = "Spotless",
            guestPolicy = GuestPolicy.WEEKENDS_OK.code,
            foodHabit = FoodHabit.EGGETARIAN.code,
            bio = "Shortlisted two sunlit flats near Karve Statue. Looking for one chill flatmate to sign directly.",
            moveInTimeline = "Within 2 weeks",
            trustScore = 89,
            verified = true,
            avatarInitials = "K",
            accentColorHex = 0xFFDCE2FA,
            avatarTextHex = 0xFF2D438B,
            flatPhotoLabels = listOf("Living room", "Your room", "Workspace")
        ),
        FlatmateListing(
            id = "com8_pune_ananya",
            ownerPhone = "curated_host_3",
            name = "Ananya",
            age = 24,
            occupation = "UI/UX designer",
            roleType = RoleType.HAS_FLAT.code,
            roleSummaryBadge = "Has a flat · 1 room free",
            flatTypeLabel = "2BHK",
            roomsFreeLabel = "1 room free",
            city = "Pune",
            locality = "Viman Nagar",
            monthlyRent = 13500,
            rentRangeLabel = "₹12k to ₹15k per month",
            latitude = 18.5679,
            longitude = 73.9143,
            smoking = "Never",
            drinking = "Socially",
            sleepSchedule = SleepSchedule.BALANCED.code,
            cleanlinessLevel = 5,
            cleanlinessTag = "Spotless",
            guestPolicy = GuestPolicy.WEEKENDS_OK.code,
            foodHabit = FoodHabit.VEG_ONLY.code,
            bio = "Sunlit flat 5 mins from Phoenix Mall with maid & cook setup already running.",
            moveInTimeline = "From 1 Dec",
            trustScore = 94,
            verified = true,
            avatarInitials = "A",
            accentColorHex = 0xFFD7EAE4,
            avatarTextHex = 0xFF0C7A63,
            photoResId = R.drawable.img_flatmate_ananya_1794592209987
        ),
        FlatmateListing(
            id = "com8_mumbai_rohan_m",
            ownerPhone = "curated_host_4",
            name = "Siddharth",
            age = 26,
            occupation = "Product analyst",
            roleType = RoleType.HAS_FLAT.code,
            roleSummaryBadge = "Has a flat · 1 room free",
            flatTypeLabel = "2BHK",
            roomsFreeLabel = "1 room free",
            city = "Mumbai",
            locality = "Powai",
            monthlyRent = 21000,
            rentRangeLabel = "₹19k to ₹23k per month",
            latitude = 19.1176,
            longitude = 72.9060,
            smoking = "Never",
            drinking = "Never",
            sleepSchedule = SleepSchedule.EARLY_BIRD.code,
            cleanlinessLevel = 4,
            cleanlinessTag = "Tidy",
            guestPolicy = GuestPolicy.WEEKENDS_OK.code,
            foodHabit = FoodHabit.NON_VEG_OK.code,
            bio = "Lake-facing 2BHK in Powai with gym access, fiber Wi-Fi, and zero brokerage.",
            moveInTimeline = "Immediate",
            trustScore = 92,
            verified = true,
            avatarInitials = "S",
            accentColorHex = 0xFFF3DFB3,
            avatarTextHex = 0xFF7D530C,
            photoResId = R.drawable.img_flatmate_rohan_1794592210309
        ),
        FlatmateListing(
            id = "com8_blr_kavya",
            ownerPhone = "curated_host_5",
            name = "Kavya",
            age = 23,
            occupation = "SDE-1 · SaaS",
            roleType = RoleType.HAS_FLAT.code,
            roleSummaryBadge = "Has a flat · 1 room free",
            flatTypeLabel = "2BHK",
            roomsFreeLabel = "1 room free",
            city = "Bengaluru",
            locality = "Koramangala",
            monthlyRent = 16500,
            rentRangeLabel = "₹15k to ₹18k per month",
            latitude = 12.9352,
            longitude = 77.6245,
            smoking = "Never",
            drinking = "Never",
            sleepSchedule = SleepSchedule.BALANCED.code,
            cleanlinessLevel = 4,
            cleanlinessTag = "Tidy",
            guestPolicy = GuestPolicy.NO_GUESTS.code,
            foodHabit = FoodHabit.EGGETARIAN.code,
            bio = "Quiet 2BHK near 80ft Road. Great for deep work and peaceful weeknights.",
            moveInTimeline = "Immediate",
            trustScore = 95,
            verified = true,
            avatarInitials = "K",
            accentColorHex = 0xFFDCE2FA,
            avatarTextHex = 0xFF2D438B,
            photoResId = R.drawable.img_flatmate_kavya_1794592210341
        ),
        FlatmateListing(
            id = "com8_hyd_meera",
            ownerPhone = "curated_host_6",
            name = "Meera",
            age = 25,
            occupation = "Consultant",
            roleType = RoleType.HAS_FLAT.code,
            roleSummaryBadge = "Has a flat · 1 room free",
            flatTypeLabel = "3BHK",
            roomsFreeLabel = "1 room free",
            city = "Hyderabad",
            locality = "Gachibowli",
            monthlyRent = 13500,
            rentRangeLabel = "₹12k to ₹15k per month",
            latitude = 17.4401,
            longitude = 78.3489,
            smoking = "Never",
            drinking = "Socially",
            sleepSchedule = SleepSchedule.BALANCED.code,
            cleanlinessLevel = 5,
            cleanlinessTag = "Spotless",
            guestPolicy = GuestPolicy.WEEKENDS_OK.code,
            foodHabit = FoodHabit.NON_VEG_OK.code,
            bio = "Gated 3BHK near Financial District with pool and morning cook.",
            moveInTimeline = "Immediate",
            trustScore = 93,
            verified = true,
            avatarInitials = "M",
            accentColorHex = 0xFFD7EAE4,
            avatarTextHex = 0xFF0C7A63,
            photoResId = R.drawable.img_flatmate_ananya_1794592209987
        ),
        FlatmateListing(
            id = "com8_delhi_arjun",
            ownerPhone = "curated_host_7",
            name = "Arjun",
            age = 27,
            occupation = "Growth lead",
            roleType = RoleType.HAS_FLAT.code,
            roleSummaryBadge = "Has a flat · 1 room free",
            flatTypeLabel = "2BHK",
            roomsFreeLabel = "1 room free",
            city = "Delhi NCR",
            locality = "Hauz Khas",
            monthlyRent = 17000,
            rentRangeLabel = "₹15k to ₹19k per month",
            latitude = 28.5494,
            longitude = 77.2001,
            smoking = "Never",
            drinking = "Never",
            sleepSchedule = SleepSchedule.EARLY_BIRD.code,
            cleanlinessLevel = 4,
            cleanlinessTag = "Tidy",
            guestPolicy = GuestPolicy.WEEKENDS_OK.code,
            foodHabit = FoodHabit.NON_VEG_OK.code,
            bio = "2 mins from Hauz Khas Metro, terrace access, and calm weekday schedule.",
            moveInTimeline = "Next month",
            trustScore = 90,
            verified = true,
            avatarInitials = "A",
            accentColorHex = 0xFFF3DFB3,
            avatarTextHex = 0xFF7D530C,
            photoResId = R.drawable.img_flatmate_siddharth_1794592244629
        )
    )
}
