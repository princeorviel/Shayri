package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.FirebaseShayariAiClient
import com.example.ai.ShayariAiClient
import com.example.ai.ShayariAiUiState
import com.example.ai.ShayariAssistMode
import com.example.data.model.CommunityShayari
import com.example.data.model.PoetFollower
import com.example.data.model.PoetProfile
import com.example.data.model.PostNotificationType
import com.example.data.model.UserProfile
import com.example.data.model.WritingPrompt
import com.example.data.model.WritingPromptsCatalog
import com.example.data.remote.CommunityShayariRepository
import com.example.data.remote.PostNotificationsRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CommunityFeedState {
    data object Loading : CommunityFeedState
    data class Success(val posts: List<CommunityShayari>) : CommunityFeedState
    data class Error(val message: String) : CommunityFeedState
}

data class CommunityUiState(
    val feedState: CommunityFeedState = CommunityFeedState.Loading,
    val visiblePosts: List<CommunityShayari> = emptyList(),
    val userWahIds: Set<String> = emptySet(),
    val userProfile: UserProfile? = null,
    val poetProfiles: List<PoetProfile> = emptyList(),
    val followingIds: Set<String> = emptySet(),
    val selectedCategoryId: String? = null,
    val sortByTopWah: Boolean = false,
    val showMyPostsOnly: Boolean = false,
    val showFollowingOnly: Boolean = false,
    val activePrompt: WritingPrompt = WritingPromptsCatalog.prompts.first(),
    val isSubmitting: Boolean = false,
    val aiUiState: ShayariAiUiState = ShayariAiUiState.Idle
) {
    fun profileFor(userId: String): PoetProfile? = poetProfiles.firstOrNull { it.userId == userId }
}

class CommunityViewModel(
    private val repository: CommunityShayariRepository,
    private val currentUserId: String,
    private val aiClient: ShayariAiClient = FirebaseShayariAiClient(),
    private val notificationsRepository: PostNotificationsRepository? = null
) : ViewModel() {

    private val selectedCategoryFlow = MutableStateFlow<String?>(null)
    private val sortByTopWahFlow = MutableStateFlow(false)
    private val showMyPostsOnlyFlow = MutableStateFlow(false)
    private val showFollowingOnlyFlow = MutableStateFlow(false)
    private val isSubmittingFlow = MutableStateFlow(false)
    private val aiUiStateFlow = MutableStateFlow<ShayariAiUiState>(ShayariAiUiState.Idle)
    private var aiJob: Job? = null
    private val promptIndexFlow = MutableStateFlow(
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % WritingPromptsCatalog.prompts.size
    )

    private val postsFlow: StateFlow<CommunityFeedState> = repository.observePublishedShayari()
        .map<List<CommunityShayari>, CommunityFeedState> { CommunityFeedState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing community shayari", rootError)
            emit(CommunityFeedState.Error(rootError.message ?: "डेटा लोड करने में दिक्कत आई"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CommunityFeedState.Loading
        )

    private val userWahIdsFlow: StateFlow<Set<String>> = repository.observeUserWahIds(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing user wahs", firestoreError ?: error)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet()
        )

    private val userProfileFlow: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing user profile", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    private val publicPoetsFlow: StateFlow<List<PoetProfile>> = repository.observePublicPoetProfiles()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing public poet profiles", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    private val followingIdsFlow: StateFlow<Set<String>> = repository.observeFollowingIds(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing following ids", firestoreError ?: error)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet()
        )

    private val filterControlsFlow = combine(
        selectedCategoryFlow,
        sortByTopWahFlow,
        showMyPostsOnlyFlow,
        showFollowingOnlyFlow,
        promptIndexFlow
    ) { catId, topWah, mineOnly, followingOnly, promptIdx ->
        FilterControls(catId, topWah, mineOnly, followingOnly, promptIdx, isSubmitting = false)
    }.combine(isSubmittingFlow) { controls, submitting ->
        controls.copy(isSubmitting = submitting)
    }.combine(aiUiStateFlow) { controls, aiState ->
        controls to aiState
    }

    private val socialStateFlow = combine(
        userProfileFlow,
        publicPoetsFlow,
        followingIdsFlow
    ) { userProfile, publicPoets, followingIds ->
        Triple(userProfile, publicPoets, followingIds)
    }

    val uiState: StateFlow<CommunityUiState> = combine(
        postsFlow,
        userWahIdsFlow,
        socialStateFlow,
        filterControlsFlow
    ) { feedState, wahIds, (profile, rawPoets, followingIds), (controls, aiState) ->
        val allPosts = (feedState as? CommunityFeedState.Success)?.posts ?: emptyList()
        val filtered = allPosts.filter { post ->
            val matchesCategory = controls.selectedCategoryId == null || post.categoryId == controls.selectedCategoryId
            val matchesMine = !controls.showMyPostsOnly || post.userId == currentUserId
            val matchesFollowing = !controls.showFollowingOnly || followingIds.contains(post.userId)
            matchesCategory && matchesMine && matchesFollowing
        }.let { list ->
            if (controls.sortByTopWah) {
                list.sortedByDescending { it.wahCount }
            } else {
                list
            }
        }

        // Merge stored poet profiles with any post authors who haven't synced a poet_profiles doc yet
        val postCountByUser = allPosts.groupingBy { it.userId }.eachCount()
        val poetsById = rawPoets.associateBy { it.userId }.toMutableMap()
        allPosts.forEach { post ->
            if (post.userId.isNotBlank() && !poetsById.containsKey(post.userId)) {
                poetsById[post.userId] = PoetProfile(
                    id = post.userId,
                    userId = post.userId,
                    penName = post.penName.ifBlank { "शायर दोस्त" },
                    bio = "",
                    isPublic = true,
                    followerCount = 0L,
                    followingCount = 0L,
                    postCount = (postCountByUser[post.userId] ?: 1).toLong()
                )
            }
        }
        val mergedPoets = poetsById.values.map { poet ->
            val liveCount = postCountByUser[poet.userId]?.toLong()
            if (liveCount != null && liveCount > poet.postCount) {
                poet.copy(postCount = liveCount)
            } else {
                poet
            }
        }.sortedWith(
            compareByDescending<PoetProfile> { it.userId == currentUserId }
                .thenByDescending { it.followerCount }
                .thenByDescending { it.postCount }
        )

        val prompts = WritingPromptsCatalog.prompts
        val prompt = prompts[controls.promptIndex % prompts.size]

        CommunityUiState(
            feedState = feedState,
            visiblePosts = filtered,
            userWahIds = wahIds,
            userProfile = profile,
            poetProfiles = mergedPoets,
            followingIds = followingIds,
            selectedCategoryId = controls.selectedCategoryId,
            sortByTopWah = controls.sortByTopWah,
            showMyPostsOnly = controls.showMyPostsOnly,
            showFollowingOnly = controls.showFollowingOnly,
            activePrompt = prompt,
            isSubmitting = controls.isSubmitting,
            aiUiState = aiState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CommunityUiState()
    )

    fun observeFollowersForPoet(poetUserId: String): Flow<List<PoetFollower>> {
        return repository.observePoetFollowers(poetUserId)
            .catch { emit(emptyList()) }
    }

    fun ensurePublicPoetProfile(defaultPenName: String) {
        viewModelScope.launch {
            val allPosts = (uiState.value.feedState as? CommunityFeedState.Success)?.posts.orEmpty()
            val myPostCount = allPosts.count { it.userId == currentUserId }
            val existingBio = uiState.value.userProfile?.bio.orEmpty()
            repository.ensurePublicPoetProfile(
                defaultPenName = defaultPenName,
                defaultBio = existingBio,
                postCount = myPostCount
            )
        }
    }

    fun selectCategory(categoryId: String?) {
        selectedCategoryFlow.value = categoryId
    }

    fun setSortByTopWah(topWah: Boolean) {
        sortByTopWahFlow.value = topWah
    }

    fun setShowMyPostsOnly(mineOnly: Boolean) {
        showMyPostsOnlyFlow.value = mineOnly
        if (mineOnly) showFollowingOnlyFlow.value = false
    }

    fun setShowFollowingOnly(followingOnly: Boolean) {
        showFollowingOnlyFlow.value = followingOnly
        if (followingOnly) showMyPostsOnlyFlow.value = false
    }

    fun cyclePrompt() {
        promptIndexFlow.value = (promptIndexFlow.value + 1) % WritingPromptsCatalog.prompts.size
    }

    fun requestAiAssist(
        mode: ShayariAssistMode,
        draft: String,
        categoryTitle: String,
        promptTitle: String
    ) {
        val theme = listOf(categoryTitle.trim(), promptTitle.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" • ")

        if (draft.isBlank() && theme.isBlank()) {
            aiUiStateFlow.value = ShayariAiUiState.Error(
                mode = mode,
                message = "सुझाव पाने के लिए कृपया कोई शब्द, लाइन या टॉपिक चुनें।"
            )
            return
        }

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            aiUiStateFlow.value = ShayariAiUiState.Loading(mode)
            try {
                val suggestions = aiClient.suggest(
                    mode = mode,
                    draft = draft,
                    themeOrTitle = theme
                )
                aiUiStateFlow.value = ShayariAiUiState.Success(mode, suggestions)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI suggestion timed out", e)
                aiUiStateFlow.value = ShayariAiUiState.Error(
                    mode = mode,
                    message = "जवाब आने में ज़्यादा समय लग गया (60 सेकंड)। फिर से कोशिश करें।"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI suggestion failed", e)
                aiUiStateFlow.value = ShayariAiUiState.Error(
                    mode = mode,
                    message = e.localizedMessage ?: "AI से सुझाव लाने में दिक्कत आई।"
                )
            }
        }
    }

    fun dismissAiSuggestions() {
        aiJob?.cancel()
        aiUiStateFlow.value = ShayariAiUiState.Idle
    }

    fun toggleFollow(
        targetUserId: String,
        targetPenName: String,
        actorName: String,
        onSuccess: (nowFollowing: Boolean) -> Unit = {},
        onError: (String) -> Unit
    ) {
        if (targetUserId.isBlank() || targetUserId == currentUserId) return
        val currentlyFollowing = uiState.value.followingIds.contains(targetUserId)
        viewModelScope.launch {
            val result = repository.toggleFollow(
                targetUserId = targetUserId,
                targetPenName = targetPenName,
                actorName = actorName,
                currentlyFollowing = currentlyFollowing
            )
            if (result.isSuccess) {
                if (!currentlyFollowing) {
                    notificationsRepository?.sendNotification(
                        recipientUserId = targetUserId,
                        actorName = actorName,
                        type = PostNotificationType.FOLLOW,
                        postId = "profile",
                        postCouplet = targetPenName.ifBlank { "कवि प्रोफ़ाइल" }
                    )
                }
                onSuccess(!currentlyFollowing)
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "फ़ॉलो अपडेट नहीं हो सका")
            }
        }
    }

    fun updatePoetProfile(
        penName: String,
        bio: String,
        socialLinks: com.example.data.model.SocialLinks? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isSubmittingFlow.value) return
        val cleanName = penName.trim()
        if (cleanName.length < 2) {
            onError("कवि नाम कम से कम 2 अक्षरों का लिखें")
            return
        }
        viewModelScope.launch {
            isSubmittingFlow.value = true
            val allPosts = (uiState.value.feedState as? CommunityFeedState.Success)?.posts.orEmpty()
            val myPostCount = allPosts.count { it.userId == currentUserId }
            val result = repository.upsertUserProfile(
                penName = cleanName,
                bio = bio.trim().take(160),
                postCount = myPostCount,
                socialLinks = socialLinks
            )
            isSubmittingFlow.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "प्रोफ़ाइल सेव नहीं हो सकी")
            }
        }
    }

    fun toggleWah(
        post: CommunityShayari,
        actorName: String = "शायर दोस्त",
        onError: (String) -> Unit
    ) {
        val currentlyWahed = uiState.value.userWahIds.contains(post.id)
        viewModelScope.launch {
            val result = repository.toggleWah(post.id, currentlyWahed)
            if (result.isSuccess && !currentlyWahed && post.userId != currentUserId) {
                notificationsRepository?.sendNotification(
                    recipientUserId = post.userId,
                    actorName = actorName,
                    type = PostNotificationType.LIKE,
                    postId = post.id,
                    postCouplet = post.couplet
                )
            } else if (result.isFailure) {
                onError(result.exceptionOrNull()?.localizedMessage ?: "लाइक सेव नहीं हो सका")
            }
        }
    }

    fun publishOrUpdateShayari(
        editingId: String?,
        penName: String,
        couplet: String,
        categoryId: String,
        promptTitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isSubmittingFlow.value) return
        viewModelScope.launch {
            isSubmittingFlow.value = true
            val result = try {
                if (editingId == null) {
                    repository.publishShayari(penName, couplet, categoryId, promptTitle)
                } else {
                    repository.updateShayari(editingId, penName, couplet, categoryId, promptTitle)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error publishing shayari", e)
                Result.failure(e)
            }
            if (result.isSuccess) {
                val existingPosts = (uiState.value.feedState as? CommunityFeedState.Success)?.posts.orEmpty()
                val myCount = existingPosts.count { it.userId == currentUserId } + if (editingId == null) 1 else 0
                runCatching {
                    repository.upsertUserProfile(
                        penName = penName,
                        bio = uiState.value.userProfile?.bio.orEmpty(),
                        postCount = myCount
                    )
                }
                isSubmittingFlow.value = false
                onSuccess()
            } else {
                isSubmittingFlow.value = false
                onError(result.exceptionOrNull()?.localizedMessage ?: "शायरी पोस्ट नहीं हो सकी")
            }
        }
    }

    fun deleteShayari(shayariId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteShayari(shayariId)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "शायरी हटाई नहीं जा सकी")
            }
        }
    }

    private data class FilterControls(
        val selectedCategoryId: String?,
        val sortByTopWah: Boolean,
        val showMyPostsOnly: Boolean,
        val showFollowingOnly: Boolean,
        val promptIndex: Int,
        val isSubmitting: Boolean
    )

    class Factory(
        private val repository: CommunityShayariRepository,
        private val currentUserId: String,
        private val aiClient: ShayariAiClient = FirebaseShayariAiClient(),
        private val notificationsRepository: PostNotificationsRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommunityViewModel(repository, currentUserId, aiClient, notificationsRepository) as T
        }
    }

    private companion object {
        const val TAG = "CommunityVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
