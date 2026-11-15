package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppThemeMode
import com.example.data.model.AiCompatibilityReport
import com.example.data.model.CoM8Catalog
import com.example.data.model.CompatibilityCalculator
import com.example.data.model.FlatmateListing
import com.example.data.model.MatchMessage
import com.example.data.model.SwipeRecord
import com.example.data.model.UserLifestyleProfile
import com.example.data.model.UserSettings
import com.example.data.repository.CoM8FirestoreRepository
import com.example.data.repository.CoM8LocalRepository
import com.example.data.repository.GeminiAiRepository
import com.example.util.CoM8LocationHelper
import com.example.util.CoM8NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Streaming(val text: String) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ScoredCandidate(
    val listing: FlatmateListing,
    val matchScore: Int
)

data class DeckFilterState(
    val selectedCity: String = "All Cities",
    val selectedRole: String? = null,
    val selectedGender: String? = null,
    val maxRent: Long = 40000L,
    val strictDealbreakers: Boolean? = null
)

class CoM8ViewModel(
    private val localRepository: CoM8LocalRepository,
    private val geminiAiRepository: GeminiAiRepository,
    val activePhoneNumber: String
) : ViewModel() {

    private val _filterState = MutableStateFlow(DeckFilterState())
    val filterState: StateFlow<DeckFilterState> = _filterState.asStateFlow()

    private val _selectedCandidateId = MutableStateFlow<String?>(null)
    val selectedCandidateId: StateFlow<String?> = _selectedCandidateId.asStateFlow()

    private val _aiReportState = MutableStateFlow<UiState<AiCompatibilityReport>>(UiState.Idle)
    val aiReportState: StateFlow<UiState<AiCompatibilityReport>> = _aiReportState.asStateFlow()

    private val _buddyState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val buddyState: StateFlow<UiState<String>> = _buddyState.asStateFlow()

    private val _matchBanner = MutableStateFlow<ScoredCandidate?>(null)
    val matchBanner: StateFlow<ScoredCandidate?> = _matchBanner.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _activeChatListingId = MutableStateFlow<String?>(null)
    val activeChatListingId: StateFlow<String?> = _activeChatListingId.asStateFlow()

    val userProfileState: StateFlow<UiState<UserLifestyleProfile?>> =
        localRepository.observeUserProfile(activePhoneNumber)
            .map<UserLifestyleProfile?, UiState<UserLifestyleProfile?>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(UiState.Error(error.localizedMessage ?: "Failed to load profile"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val themeModeState: StateFlow<AppThemeMode> =
        localRepository.themeModeFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppThemeMode.SYSTEM)

    val permissionsPromptedState: StateFlow<Boolean?> =
        localRepository.permissionsPromptedFlow
            .map<Boolean, Boolean?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val userSettingsState: StateFlow<UserSettings> =
        localRepository.observeSettings(activePhoneNumber)
            .catch { error ->
                if (error is CancellationException) throw error
                emit(UserSettings(userId = activePhoneNumber))
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                UserSettings(userId = activePhoneNumber)
            )

    val ownListingState: StateFlow<FlatmateListing?> =
        localRepository.observeOwnListing(activePhoneNumber)
            .catch { error ->
                if (error is CancellationException) throw error
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val localListingsFlow: StateFlow<List<FlatmateListing>> =
        localRepository.observeLocalListings()
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val swipesState: StateFlow<UiState<List<SwipeRecord>>> =
        localRepository.observeSwipes(activePhoneNumber)
            .map<List<SwipeRecord>, UiState<List<SwipeRecord>>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(UiState.Error(error.localizedMessage ?: "Failed to load saved matches"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val allMergedListings: StateFlow<List<ScoredCandidate>> = combine(
        userProfileState,
        localListingsFlow
    ) { profileState, savedListings ->
        val profile = (profileState as? UiState.Success)?.data
        val myUid = localRepository.currentAuthUid()
        val merged = savedListings
            .distinctBy { it.id }
            .filter {
                it.ownerPhone != activePhoneNumber &&
                    (myUid == null || it.ownerPhone != myUid)
            }
        merged.map { candidate ->
            ScoredCandidate(
                listing = candidate,
                matchScore = CompatibilityCalculator.computeScore(profile, candidate)
            )
        }.sortedByDescending { it.matchScore }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val deckCandidates: StateFlow<List<ScoredCandidate>> = combine(
        allMergedListings,
        userProfileState,
        swipesState,
        _filterState
    ) { all, profileUi, swipesUi, filter ->
        val profile = (profileUi as? UiState.Success)?.data
        val preferredCities = profile?.preferredCities
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val swipedIds = (swipesUi as? UiState.Success)?.data?.map { it.listingId }?.toSet() ?: emptySet()
        val enforceDealbreakers = filter.strictDealbreakers ?: (profile?.smokingDrinkingDealbreaker == true)

        all.filter { item ->
            val l = item.listing
            val matchesCity = if (filter.selectedCity != "All Cities") {
                l.city.equals(filter.selectedCity, ignoreCase = true)
            } else if (preferredCities.isNotEmpty()) {
                preferredCities.any { pref -> pref.equals(l.city, ignoreCase = true) }
            } else {
                true
            }

            val matchesDealbreakers = if (!enforceDealbreakers || profile == null) {
                true
            } else {
                (profile.smoking != "Never" || l.smoking == "Never") &&
                    (profile.drinking != "Never" || l.drinking == "Never")
            }

            l.id !in swipedIds &&
                matchesCity &&
                matchesDealbreakers &&
                l.monthlyRent <= filter.maxRent &&
                (filter.selectedRole == null || l.roleType == filter.selectedRole) &&
                (filter.selectedGender == null || l.gender.equals(filter.selectedGender, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val sharedChatsFlow: StateFlow<List<CoM8FirestoreRepository.SharedChatThread>> =
        localRepository.observeSharedChats()
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val likedMatches: StateFlow<List<SwipeRecord>> = combine(
        swipesState,
        sharedChatsFlow,
        allMergedListings,
        _activeChatListingId
    ) { state, chats, candidates, openChatListingId ->
        val myUid = localRepository.currentAuthUid()
        val likedSwipes = (state as? UiState.Success)?.data
            ?.filter { it.decision == "LIKED" }
            ?.toMutableList()
            ?: mutableListOf()

        chats.forEach { chat ->
            val matchingCandidate = candidates.firstOrNull {
                it.listing.id == chat.listingId || it.listing.ownerPhone == chat.peerUid
            }
            val candidateListingId = matchingCandidate?.listing?.id
            val existingIdx = likedSwipes.indexOfFirst { swipe ->
                swipe.listingId == chat.listingId ||
                    (candidateListingId != null && swipe.listingId == candidateListingId) ||
                    swipe.listingId == "chat_${chat.chatId}" ||
                    swipe.listingId == "profile_${chat.peerUid}"
            }
            val resolvedListingId = if (existingIdx >= 0) {
                likedSwipes[existingIdx].listingId
            } else {
                candidateListingId ?: "chat_${chat.chatId}"
            }
            val isFromPeer = myUid != null && chat.lastSenderId.isNotBlank() && chat.lastSenderId != myUid
            val isCurrentlyOpen = openChatListingId == resolvedListingId
            if (existingIdx >= 0) {
                val existing = likedSwipes[existingIdx]
                val latestTime = maxOf(existing.updatedAt, chat.updatedAt)
                val latestPreview = if (chat.updatedAt >= existing.updatedAt && chat.lastMessageText.isNotBlank()) {
                    chat.lastMessageText
                } else {
                    existing.lastMessagePreview
                }
                likedSwipes[existingIdx] = existing.copy(
                    status = "INTRO_SENT",
                    lastMessagePreview = latestPreview,
                    unreadCount = if (isFromPeer && !isCurrentlyOpen && chat.updatedAt > existing.updatedAt) {
                        maxOf(existing.unreadCount, 1)
                    } else if (isCurrentlyOpen) {
                        0
                    } else {
                        existing.unreadCount
                    },
                    updatedAt = latestTime
                )
            } else {
                likedSwipes.add(
                    SwipeRecord(
                        phoneNumber = activePhoneNumber,
                        listingId = resolvedListingId,
                        decision = "LIKED",
                        status = "INTRO_SENT",
                        targetName = matchingCandidate?.listing?.name ?: chat.peerName,
                        targetCity = matchingCandidate?.listing?.city ?: chat.targetCity,
                        targetLocality = matchingCandidate?.listing?.locality ?: chat.targetLocality,
                        targetRent = matchingCandidate?.listing?.monthlyRent ?: chat.targetRent,
                        compatibilityScore = matchingCandidate?.matchScore?.toLong() ?: chat.compatibilityScore,
                        unreadCount = if (isFromPeer && !isCurrentlyOpen) 1 else 0,
                        lastMessagePreview = chat.lastMessageText,
                        updatedAt = chat.updatedAt
                    )
                )
            }
        }
        likedSwipes.sortedByDescending { it.updatedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChatMessages: StateFlow<UiState<List<MatchMessage>>> = combine(
        _activeChatListingId,
        allMergedListings,
        sharedChatsFlow
    ) { listingId, candidates, chats ->
        if (listingId.isNullOrBlank()) {
            Triple<String?, String?, String?>(null, null, null)
        } else {
            val cand = candidates.firstOrNull { it.listing.id == listingId }
            val matchingChat = chats.firstOrNull { chat ->
                "chat_${chat.chatId}" == listingId ||
                    chat.listingId == listingId ||
                    (cand != null && chat.peerUid == cand.listing.ownerPhone) ||
                    "profile_${chat.peerUid}" == listingId
            }
            val peerUid = cand?.listing?.ownerPhone?.takeIf { it.isNotBlank() }
                ?: matchingChat?.peerUid
                ?: listingId.takeIf { it.startsWith("profile_") }?.removePrefix("profile_")
            val chatIdHint = matchingChat?.chatId
                ?: listingId.takeIf { it.startsWith("chat_") }?.removePrefix("chat_")
            Triple(listingId, peerUid, chatIdHint)
        }
    }
        .distinctUntilChanged()
        .flatMapLatest { (listingId, peerUid, chatIdHint) ->
            if (listingId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                localRepository.observeMatchMessages(
                    phoneNumber = activePhoneNumber,
                    listingId = listingId,
                    peerUid = peerUid,
                    chatIdHint = chatIdHint
                )
                    .map<List<MatchMessage>, UiState<List<MatchMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        emit(UiState.Error(error.localizedMessage ?: "Failed to load chat"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun updateCityFilter(city: String) {
        _filterState.value = _filterState.value.copy(selectedCity = city)
    }

    fun updateRoleFilter(roleCode: String?) {
        _filterState.value = _filterState.value.copy(selectedRole = roleCode)
    }

    fun updateGenderFilter(gender: String?) {
        _filterState.value = _filterState.value.copy(selectedGender = gender)
    }

    fun updateMaxRentFilter(maxRent: Long) {
        _filterState.value = _filterState.value.copy(maxRent = maxRent)
    }

    fun updateDealbreakerFilter(strict: Boolean?) {
        _filterState.value = _filterState.value.copy(strictDealbreakers = strict)
    }

    fun selectCandidate(listingId: String?) {
        _selectedCandidateId.value = listingId
        _aiReportState.value = UiState.Idle
    }

    fun dismissMatchBanner() {
        _matchBanner.value = null
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun swipeCandidate(
        candidate: ScoredCandidate,
        liked: Boolean,
        onLikedSaved: ((ScoredCandidate) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val decision = if (liked) "LIKED" else "PASSED"
            val status = if (liked) "SAVED" else "PASSED"
            val result = localRepository.recordSwipe(
                phoneNumber = activePhoneNumber,
                listing = candidate.listing,
                decision = decision,
                status = status,
                compatibilityScore = candidate.matchScore
            )
            if (result.isSuccess) {
                if (liked) {
                    onLikedSaved?.invoke(candidate)
                }
            } else {
                _actionError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save swipe"
            }
        }
    }

    fun resetPassedSwipes() {
        viewModelScope.launch {
            localRepository.resetPassedSwipes(activePhoneNumber)
        }
    }

    fun saveLifestyleQuiz(profile: UserLifestyleProfile, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val cities = profile.preferredCities.filter { it.isNotBlank() }.ifEmpty { listOf("Pune") }
            val existing = (userProfileState.value as? UiState.Success)?.data
            val enriched = profile.copy(
                phoneNumber = activePhoneNumber,
                email = profile.email.ifBlank { existing?.email.orEmpty() },
                authProvider = profile.authProvider.ifBlank { existing?.authProvider ?: "PHONE" },
                displayName = profile.displayName.ifBlank { "CoM8 ${activePhoneNumber.takeLast(4)}" },
                preferredCities = cities,
                city = cities.first(),
                quizCompleted = true,
                verifiedBadge = true,
                trustScore = 92L
            )
            val result = localRepository.saveUserProfile(activePhoneNumber, enriched)
            if (result.isSuccess) {
                _filterState.value = _filterState.value.copy(selectedCity = "All Cities")
                onComplete()
            } else {
                _actionError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun markPhoneVerified(linkedPhone: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val existing = (userProfileState.value as? UiState.Success)?.data
            if (existing != null) {
                val updated = existing.copy(
                    verifiedBadge = true,
                    trustScore = maxOf(existing.trustScore, 95L)
                )
                val result = localRepository.saveUserProfile(activePhoneNumber, updated)
                if (result.isSuccess) {
                    onComplete()
                } else {
                    _actionError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update verification status"
                }
            } else {
                onComplete()
            }
        }
    }

    fun publishOwnListing(listing: FlatmateListing, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val profile = (userProfileState.value as? UiState.Success)?.data
            val existingOwn = ownListingState.value
            val result = localRepository.publishListing(
                activePhoneNumber,
                listing.copy(
                    id = existingOwn?.id ?: "own_$activePhoneNumber",
                    name = profile?.displayName?.ifBlank { "CoM8 Member" } ?: "CoM8 Member",
                    age = (profile?.age ?: 24).toLong(),
                    gender = profile?.gender ?: "He",
                    occupation = profile?.occupationCategory ?: "Working professional",
                    organizationName = profile?.organizationName.orEmpty(),
                    interests = profile?.interests.orEmpty(),
                    verified = true,
                    trustScore = 92L,
                    photoUri = profile?.photoUri.orEmpty()
                )
            )
            if (result.isSuccess) {
                onSuccess()
            } else {
                _actionError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to publish listing"
            }
        }
    }

    fun deleteOwnListing(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            localRepository.deleteOwnListing(activePhoneNumber)
            onSuccess()
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            localRepository.setThemeMode(mode)
        }
    }

    fun completePermissionOnboarding(
        context: Context,
        notificationsGranted: Boolean,
        coarseLocationGranted: Boolean,
        onComplete: (detectedCity: String?) -> Unit = {}
    ) {
        viewModelScope.launch {
            localRepository.setPermissionsPrompted(true)
            if (notificationsGranted) {
                CoM8NotificationHelper.ensureChannel(context.applicationContext)
            } else {
                val currentSettings = userSettingsState.value
                if (currentSettings.notifyNewMatches || currentSettings.notifyMessages) {
                    localRepository.saveSettings(
                        activePhoneNumber,
                        currentSettings.copy(
                            notifyNewMatches = false,
                            notifyMessages = false
                        )
                    )
                }
            }
            val detectedCity = if (coarseLocationGranted) {
                CoM8LocationHelper.detectApproximateCity(
                    context.applicationContext,
                    CoM8Catalog.selectableCities
                )
            } else {
                null
            }
            if (!detectedCity.isNullOrBlank()) {
                val existing = (userProfileState.value as? UiState.Success)?.data
                if (existing != null && existing.currentResidence.isBlank()) {
                    localRepository.saveUserProfile(
                        activePhoneNumber,
                        existing.copy(currentResidence = detectedCity)
                    )
                }
            }
            onComplete(detectedCity)
        }
    }

    fun notifyLikedMatch(context: Context, candidate: ScoredCandidate) {
        val settings = userSettingsState.value
        CoM8NotificationHelper.postMatchOrMessageNotification(
            context = context.applicationContext,
            title = "Liked ${candidate.listing.name}",
            body = "You're connected with ${candidate.listing.name} (${candidate.matchScore}% match) in ${candidate.listing.locality}.",
            notificationId = candidate.listing.id.hashCode(),
            userSettingEnabled = settings.notifyNewMatches
        )
    }

    fun saveUserSettings(settings: UserSettings) {
        viewModelScope.launch {
            localRepository.saveSettings(activePhoneNumber, settings)
            val own = ownListingState.value
            if (own != null) {
                localRepository.publishListing(
                    activePhoneNumber,
                    own.copy(
                        isLive = settings.postIsLive,
                        visibility = settings.postVisibility,
                        hideExactAddress = settings.hideExactAddress,
                        messagesOnlyAfterMatch = settings.messagesOnlyAfterMatching,
                        showPhoneNumber = settings.showPhoneNumber,
                        pauseAfter30Days = settings.pauseAfter30Days
                    )
                )
            }
        }
    }

    fun requestAiCompatibilityReport(candidate: ScoredCandidate) {
        viewModelScope.launch {
            _aiReportState.value = UiState.Loading
            val profile = (userProfileState.value as? UiState.Success)?.data
            val res = geminiAiRepository.generateCompatibilityReport(
                profile = profile,
                candidate = candidate.listing,
                baselineScore = candidate.matchScore
            )
            _aiReportState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "AI analysis failed") }
            )
        }
    }

    fun askCoM8Buddy(question: String) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val city = profile?.preferredCities?.firstOrNull() ?: profile?.city ?: "Pune"
        val budget = profile?.monthlyBudget ?: 18000L
        viewModelScope.launch {
            _buddyState.value = UiState.Loading
            try {
                var latest = ""
                geminiAiRepository.askCoM8BuddyStream(question, city, budget).collect { chunk ->
                    latest = chunk
                    _buddyState.value = UiState.Streaming(chunk)
                }
                _buddyState.value = if (latest.isNotBlank()) {
                    UiState.Success(latest)
                } else {
                    UiState.Error("No response from CoM8 Buddy.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _buddyState.value = UiState.Error(e.localizedMessage ?: "CoM8 Buddy failed.")
            }
        }
    }

    fun openMatchChat(listingId: String) {
        _activeChatListingId.value = listingId
        viewModelScope.launch {
            localRepository.clearUnread(activePhoneNumber, listingId)
        }
    }

    fun closeMatchChat() {
        _activeChatListingId.value = null
    }

    fun sendChatMessage(listingId: String, text: String) {
        if (text.isBlank()) return
        val cand = allMergedListings.value.firstOrNull { it.listing.id == listingId }
        val matchRecord = likedMatches.value.firstOrNull { it.listingId == listingId }
        val matchingChat = sharedChatsFlow.value.firstOrNull { chat ->
            "chat_${chat.chatId}" == listingId ||
                chat.listingId == listingId ||
                (cand != null && chat.peerUid == cand.listing.ownerPhone) ||
                "profile_${chat.peerUid}" == listingId
        }
        val myName = (userProfileState.value as? UiState.Success)?.data?.displayName
            ?.takeIf { it.isNotBlank() } ?: "CoM8 Member"
        val peerUid = cand?.listing?.ownerPhone?.takeIf { it.isNotBlank() } ?: matchingChat?.peerUid
        val peerName = cand?.listing?.name?.takeIf { it.isNotBlank() }
            ?: matchRecord?.targetName?.takeIf { it.isNotBlank() }
            ?: matchingChat?.peerName
        val score = cand?.matchScore ?: matchRecord?.compatibilityScore?.toInt() ?: 90
        viewModelScope.launch {
            val res = localRepository.sendMatchMessage(
                phoneNumber = activePhoneNumber,
                listingId = listingId,
                senderRole = "ME",
                text = text,
                listing = cand?.listing,
                myDisplayName = myName,
                compatibilityScore = score,
                peerUidOverride = peerUid,
                peerNameOverride = peerName,
                chatIdOverride = matchingChat?.chatId
            )
            if (res.isFailure) {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to send message"
            }
        }
    }

    fun reportChatUser(
        listingId: String,
        reportedName: String,
        reason: String,
        details: String,
        onSuccess: () -> Unit = {}
    ) {
        val cand = allMergedListings.value.firstOrNull { it.listing.id == listingId }
        val matchingChat = sharedChatsFlow.value.firstOrNull { chat ->
            "chat_${chat.chatId}" == listingId ||
                chat.listingId == listingId ||
                (cand != null && chat.peerUid == cand.listing.ownerPhone) ||
                "profile_${chat.peerUid}" == listingId
        }
        val peerUid = cand?.listing?.ownerPhone?.takeIf { it.isNotBlank() } ?: matchingChat?.peerUid
        viewModelScope.launch {
            val res = localRepository.reportUser(
                listingId = listingId,
                reportedName = reportedName,
                reason = reason,
                details = details,
                reportedUserId = peerUid,
                chatId = matchingChat?.chatId
            )
            if (res.isSuccess) {
                onSuccess()
            } else {
                _actionError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to submit report"
            }
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            localRepository.signOut()
            onComplete()
        }
    }
}
