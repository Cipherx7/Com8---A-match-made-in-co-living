package com.example.data.repository

import android.util.Log
import com.example.data.model.AiCompatibilityReport
import com.example.data.model.FlatmateListing
import com.example.data.model.FoodHabit
import com.example.data.model.GuestPolicy
import com.example.data.model.RoleType
import com.example.data.model.SleepSchedule
import com.example.data.model.UserLifestyleProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiRepo"
private const val AI_TIMEOUT_MS = 60_000L
private const val MODEL_NAME = "gemini-3.8-flash"

class GeminiAiRepository {

    private val compatibilitySchema = Schema.obj(
        properties = mapOf(
            "matchScore" to Schema.integer(description = "Compatibility percentage from 0 to 100"),
            "headline" to Schema.string(description = "Punchy 1-sentence Gen-Z verdict on living together"),
            "strengths" to Schema.array(
                items = Schema.string(),
                description = "2 to 3 concrete lifestyle strengths based on habits and budget"
            ),
            "dealBreakers" to Schema.array(
                items = Schema.string(),
                description = "1 to 2 honest friction points or things to align on before move-in"
            ),
            "icebreakerPrompt" to Schema.string(
                description = "Friendly, specific first message the user can send to this flatmate"
            )
        )
    )

    private val compatibilityModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = compatibilitySchema
            }
        )
    }

    private val advisorModel by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_NAME)
    }

    suspend fun generateCompatibilityReport(
        profile: UserLifestyleProfile?,
        candidate: FlatmateListing,
        baselineScore: Int
    ): Result<AiCompatibilityReport> {
        val userSummary = if (profile != null) {
            """
            Seeker Profile:
            - Name: ${profile.displayName}
            - Target City: ${profile.city}, Role: ${RoleType.fromCode(profile.roleType).label}
            - Monthly Budget: INR ${profile.monthlyBudget}
            - Sleep: ${SleepSchedule.fromCode(profile.sleepSchedule).label}
            - Cleanliness (1-5): ${profile.cleanlinessLevel}
            - Guests: ${GuestPolicy.fromCode(profile.guestPolicy).label}
            - Food: ${FoodHabit.fromCode(profile.foodHabit).label}
            - Bio: ${profile.bio}
            """.trimIndent()
        } else {
            "Seeker Profile: Young professional in ${candidate.city}, baseline budget INR ${candidate.monthlyRent}."
        }

        val prompt = """
            You are CoM8's people-first flatmate compatibility engine for Indian metro cities.
            Compare the seeker and candidate below and produce a candid, practical compatibility report
            so deal-breakers surface before move-in. Keep tone young, sharp, warm, and trustworthy.
            Anchor matchScore near $baselineScore (+/- 8 points based on nuance).

            $userSummary

            Candidate Card:
            - Name: ${candidate.name} (${candidate.age}), ${candidate.occupation}
            - Role: ${RoleType.fromCode(candidate.roleType).label}
            - Location: ${candidate.locality}, ${candidate.city}
            - Rent Split: INR ${candidate.monthlyRent}/mo (${candidate.moveInTimeline})
            - Sleep: ${SleepSchedule.fromCode(candidate.sleepSchedule).label}
            - Cleanliness (1-5): ${candidate.cleanlinessLevel}
            - Guests: ${GuestPolicy.fromCode(candidate.guestPolicy).label}
            - Food: ${FoodHabit.fromCode(candidate.foodHabit).label}
            - Trust Score: ${candidate.trustScore}/100 (Verified: ${candidate.verified})
            - Bio: ${candidate.bio}
        """.trimIndent()

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                compatibilityModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("Compatibility engine returned an empty response.")
            val json = JSONObject(jsonText)
            val strengthsArray = json.optJSONArray("strengths")
            val strengths = buildList {
                if (strengthsArray != null) {
                    for (i in 0 until strengthsArray.length()) {
                        add(strengthsArray.getString(i))
                    }
                }
            }
            val dealsArray = json.optJSONArray("dealBreakers")
            val dealBreakers = buildList {
                if (dealsArray != null) {
                    for (i in 0 until dealsArray.length()) {
                        add(dealsArray.getString(i))
                    }
                }
            }
            Result.success(
                AiCompatibilityReport(
                    matchScore = json.optInt("matchScore", baselineScore).coerceIn(30, 99),
                    headline = json.optString("headline", "Solid roommate alignment worth a vibe check."),
                    strengths = strengths.ifEmpty { listOf("Similar neighborhood & rent expectations") },
                    dealBreakers = dealBreakers.ifEmpty { listOf("Confirm maid/cook split & guest notice rules") },
                    icebreakerPrompt = json.optString(
                        "icebreakerPrompt",
                        "Hey ${candidate.name.substringBefore(' ')}! Loved your ${candidate.locality} vibe—are you free for a quick flatmate chat this week?"
                    )
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Compatibility report timed out", e)
            Result.failure(Exception("Compatibility check timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Compatibility report failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Unable to generate AI compatibility report."))
        }
    }

    fun askCoM8BuddyStream(
        question: String,
        city: String,
        monthlyBudget: Long
    ): Flow<String> = flow {
        val prompt = """
            You are CoM8 Buddy, a street-smart Indian metro flatmate & PG advisor (Pune, Mumbai, Bengaluru, Hyderabad, Delhi NCR).
            User context: Target city = $city, monthly budget = INR $monthlyBudget.
            Answer concisely (3-5 crisp bullet points or 1 short paragraph, max 130 words) with realistic local rent insights, deposit norms, and scam/red-flag checks.
            User question: $question
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                advisorModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("CoM8 Buddy returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "CoM8 Buddy timed out", e)
            throw Exception("CoM8 Buddy timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "CoM8 Buddy failed", e)
            throw Exception(e.localizedMessage ?: "Failed to reach CoM8 Buddy.")
        }
    }
}
