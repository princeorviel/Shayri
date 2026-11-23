package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ShayariRepository
import com.example.data.local.ShayariSeedData
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.CardStyle
import com.example.data.model.CommunityShayari
import com.example.data.model.FontScaleOption
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariFontStyle
import com.example.data.model.ShayariTagMatcher
import com.example.data.model.ThemeModeOption
import com.example.util.HinglishTransliterator
import java.util.Calendar
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShayariUiState(
    val isLoading: Boolean = true,
    val allShayari: List<ShayariEntity> = emptyList(),
    val filteredShayari: List<ShayariEntity> = emptyList(),
    val favoriteShayari: List<ShayariEntity> = emptyList(),
    val featuredShayari: ShayariEntity? = null,
    val popularShayari: List<ShayariEntity> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val preferences: UserPreferences = UserPreferences()
)

class ShayariViewModel(
    private val repository: ShayariRepository,
    private val preferencesRepository: UserPreferencesRepository,
    autoSeed: Boolean = true
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCategoryFlow = MutableStateFlow<String?>(null)
    private val refreshSeedFlow = MutableStateFlow(0L)
    private val onlineCommunityPostsFlow = MutableStateFlow<List<ShayariEntity>>(emptyList())

    init {
        if (autoSeed) {
            viewModelScope.launch {
                repository.ensureSeeded()
            }
        }
    }

    private val initialPreferences: UserPreferences = preferencesRepository.readCachedPreferencesSync()

    private val allShayariSharedFlow = combine(
        repository.allShayari.onStart {
            // Emit bundled catalog instantly on cold start before Room finishes first query/seeding
            emit(ShayariSeedData.allShayari)
        },
        onlineCommunityPostsFlow
    ) { localCatalog, onlinePosts ->
        // If Room hasn't finished seeding yet on fresh install, keep showing bundled catalog immediately
        val effectiveLocal = if (localCatalog.isEmpty()) ShayariSeedData.allShayari else localCatalog
        if (onlinePosts.isEmpty()) {
            effectiveLocal
        } else {
            val onlineKeys = HashSet<String>(onlinePosts.size * 2)
            for (post in onlinePosts) {
                onlineKeys.add(dedupeKey(post.couplet, post.poet))
            }
            val merged = ArrayList<ShayariEntity>(onlinePosts.size + effectiveLocal.size)
            merged.addAll(onlinePosts)
            for (item in effectiveLocal) {
                // Skip local user-post copy if the exact same post is already live from Firestore
                if (item.id >= ShayariRepository.USER_POST_ID_BASE &&
                    dedupeKey(item.couplet, item.poet) in onlineKeys
                ) {
                    continue
                }
                merged.add(item)
            }
            merged
        }
    }
        .flowOn(Dispatchers.Default)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    private data class IndexedShayari(
        val entity: ShayariEntity,
        val normCouplet: String,
        val normPoet: String,
        val normMoodTag: String,
        val multiCategoryIds: Set<String>?
    )

    @Volatile
    private var cachedIndexSource: List<ShayariEntity>? = null
    @Volatile
    private var cachedIndexList: List<IndexedShayari> = emptyList()

    private suspend fun getOrBuildSearchIndex(source: List<ShayariEntity>): List<IndexedShayari> {
        if (cachedIndexSource === source && cachedIndexList.size == source.size) {
            return cachedIndexList
        }
        val ctx = currentCoroutineContext()
        val built = ArrayList<IndexedShayari>(source.size)
        for (i in source.indices) {
            if ((i and 511) == 0) ctx.ensureActive()
            val item = source[i]
            val multiCats = if (ShayariTagMatcher.hasMultiTagMetadata(item)) {
                ShayariTagMatcher.matchingCategoryIds(item.categoryId, item.moodTag)
            } else {
                null
            }
            built.add(
                IndexedShayari(
                    entity = item,
                    normCouplet = ShayariTagMatcher.normalizeForSearch(item.couplet),
                    normPoet = ShayariTagMatcher.normalizeForSearch(item.poet),
                    normMoodTag = ShayariTagMatcher.normalizeForSearch(item.moodTag),
                    multiCategoryIds = multiCats
                )
            )
        }
        cachedIndexSource = source
        cachedIndexList = built
        return built
    }

    private data class CatalogDerivedState(
        val isLoaded: Boolean = false,
        val allShayari: List<ShayariEntity> = emptyList(),
        val categoryCounts: Map<String, Int> = emptyMap(),
        val popularShayari: List<ShayariEntity> = emptyList(),
        val featuredShayari: ShayariEntity? = null
    )

    private data class FilterResult(
        val filtered: List<ShayariEntity> = emptyList(),
        val selectedCategoryId: String? = null,
        val searchQuery: String = ""
    )

    private val catalogDerivedFlow: StateFlow<CatalogDerivedState> = combine(
        allShayariSharedFlow,
        refreshSeedFlow
    ) { all, refreshSeed ->
        val counts = HashMap<String, Int>(ShayariCategory.entries.size * 2)
        val featuredPool = ArrayList<ShayariEntity>(64)
        val renownedPoetPool = ArrayList<ShayariEntity>(256)

        for (item in all) {
            if (ShayariTagMatcher.hasMultiTagMetadata(item)) {
                val matchedCats = ShayariTagMatcher.matchingCategoryIds(item.categoryId, item.moodTag)
                for (catId in matchedCats) {
                    counts[catId] = (counts[catId] ?: 0) + 1
                }
            } else {
                counts[item.categoryId] = (counts[item.categoryId] ?: 0) + 1
            }
            if (item.isFeatured) {
                featuredPool.add(item)
            } else if (item.poet in POPULAR_POETS) {
                renownedPoetPool.add(item)
            }
        }

        val combinedPopular = when {
            featuredPool.isNotEmpty() || renownedPoetPool.isNotEmpty() -> {
                ArrayList<ShayariEntity>(featuredPool.size + renownedPoetPool.size).apply {
                    addAll(featuredPool)
                    addAll(renownedPoetPool)
                }
            }
            else -> all
        }
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val rotationSeed = if (refreshSeed > 0L) refreshSeed else dayOfYear.toLong()
        val popularList = if (combinedPopular.size > 1) {
            combinedPopular.shuffled(Random(rotationSeed)).take(60)
        } else {
            combinedPopular
        }

        CatalogDerivedState(
            isLoaded = all.isNotEmpty(),
            allShayari = all,
            categoryCounts = counts,
            popularShayari = popularList,
            featuredShayari = popularList.firstOrNull()
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CatalogDerivedState()
        )

    private val filterInputsFlow = combine(
        searchQueryFlow,
        selectedCategoryFlow,
        refreshSeedFlow
    ) { query, selectedCat, refreshSeed ->
        Triple(query, selectedCat, refreshSeed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredFlow: StateFlow<FilterResult> = combine(
        allShayariSharedFlow,
        filterInputsFlow
    ) { allItems, inputs ->
        allItems to inputs
    }
        .mapLatest { (allItems, inputs) ->
            val (query, selectedCat, refreshSeed) = inputs
            val trimmed = query.trim()
            val baseFiltered = when {
                // Fast-path: Home screen & category filter NEVER wait for heavy search normalization!
                trimmed.isEmpty() && selectedCat == null -> allItems
                trimmed.isEmpty() -> allItems.filter { item ->
                    item.categoryId == selectedCat ||
                        (ShayariTagMatcher.hasMultiTagMetadata(item) &&
                            selectedCat in ShayariTagMatcher.matchingCategoryIds(item.categoryId, item.moodTag))
                }
                else -> {
                    val spec = ShayariTagMatcher.buildSearchSpec(trimmed)
                    if (spec.matchAllFallback && selectedCat == null) {
                        allItems
                    } else {
                        val indexedAll = getOrBuildSearchIndex(allItems)
                        val scored = ArrayList<Pair<Int, ShayariEntity>>()
                        var firstScore = -1
                        var hasDistinctScores = false
                        val ctx = currentCoroutineContext()
                        for (idx in indexedAll.indices) {
                            if ((idx and 511) == 0) {
                                ctx.ensureActive()
                            }
                            val entry = indexedAll[idx]
                            val item = entry.entity
                            if (selectedCat != null) {
                                val inSelectedCat = item.categoryId == selectedCat ||
                                    (entry.multiCategoryIds != null && selectedCat in entry.multiCategoryIds)
                                if (!inSelectedCat) continue
                            }
                            val score = ShayariTagMatcher.scoreMatch(
                                spec = spec,
                                primaryCategoryId = item.categoryId,
                                multiCategoryIds = entry.multiCategoryIds,
                                normCouplet = entry.normCouplet,
                                normPoet = entry.normPoet,
                                normMoodTag = entry.normMoodTag
                            )
                            if (score > 0) {
                                if (firstScore < 0) {
                                    firstScore = score
                                } else if (score != firstScore) {
                                    hasDistinctScores = true
                                }
                                scored.add(score to item)
                            }
                        }
                        if (hasDistinctScores && scored.size > 1) {
                            ctx.ensureActive()
                            scored.sortByDescending { it.first }
                        }
                        scored.map { it.second }
                    }
                }
            }
            val filtered = if (refreshSeed > 0L && trimmed.isEmpty()) {
                baseFiltered.shuffled(Random(refreshSeed))
            } else {
                baseFiltered
            }
            FilterResult(
                filtered = filtered,
                selectedCategoryId = selectedCat,
                searchQuery = query
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FilterResult()
        )

    private val favoritesFlow: StateFlow<List<ShayariEntity>> = repository.favoriteShayari.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val preferencesFlow: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initialPreferences
    )

    val uiState: StateFlow<ShayariUiState> = combine(
        catalogDerivedFlow,
        filteredFlow,
        favoritesFlow,
        preferencesFlow,
        searchQueryFlow
    ) { catalog, filterResult, favorites, prefs, liveSearchQuery ->
        ShayariUiState(
            isLoading = !catalog.isLoaded,
            allShayari = catalog.allShayari,
            filteredShayari = filterResult.filtered,
            favoriteShayari = favorites,
            featuredShayari = catalog.featuredShayari,
            popularShayari = catalog.popularShayari,
            categoryCounts = catalog.categoryCounts,
            selectedCategoryId = filterResult.selectedCategoryId,
            searchQuery = liveSearchQuery,
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ShayariUiState(preferences = initialPreferences)
    )

    fun refreshFeed() {
        searchQueryFlow.value = ""
        selectedCategoryFlow.value = null
        refreshSeedFlow.value = System.currentTimeMillis()
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun onSearchQueryChanged(query: String) {
        if (query.isNotBlank() && selectedCategoryFlow.value != null) {
            selectedCategoryFlow.value = null
        }
        searchQueryFlow.value = query
    }

    fun onCategorySelected(categoryId: String?) {
        selectedCategoryFlow.value = categoryId
    }

    fun toggleFavorite(shayari: ShayariEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(shayari.id, shayari.isFavorite)
        }
    }

    fun cycleThemeMode(currentlyDarkSystem: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.preferences.themeMode
            val next = when (current) {
                ThemeModeOption.SYSTEM -> if (currentlyDarkSystem) ThemeModeOption.LIGHT else ThemeModeOption.DARK
                ThemeModeOption.LIGHT -> ThemeModeOption.DARK
                ThemeModeOption.DARK -> ThemeModeOption.LIGHT
            }
            preferencesRepository.setThemeMode(next)
        }
    }

    fun setThemeMode(mode: ThemeModeOption) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setFontScale(scale: FontScaleOption) {
        viewModelScope.launch {
            preferencesRepository.setFontScale(scale)
        }
    }

    fun setFontStyle(style: ShayariFontStyle) {
        viewModelScope.launch {
            preferencesRepository.setFontStyle(style)
        }
    }

    fun resetReadingTypography() {
        viewModelScope.launch {
            preferencesRepository.setFontScale(FontScaleOption.MEDIUM)
            preferencesRepository.setFontStyle(ShayariFontStyle.CLASSIC_SERIF)
        }
    }

    fun setCardStyle(style: CardStyle) {
        viewModelScope.launch {
            preferencesRepository.setCardStyle(style)
        }
    }

    fun setAuthorPenName(name: String) {
        viewModelScope.launch {
            preferencesRepository.setAuthorPenName(name)
        }
    }

    fun toggleOwnerStatsEnabled(onToggled: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val enabled = preferencesRepository.toggleOwnerStatsEnabled()
            onToggled(enabled)
        }
    }

    fun registerLocalAccount(
        fullName: String,
        username: String,
        contact: String,
        password: String,
        bio: String,
        socialLinks: com.example.data.model.SocialLinks,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            preferencesRepository.registerLocalAccount(
                fullName = fullName,
                username = username,
                contact = contact,
                password = password,
                bio = bio,
                socialLinks = socialLinks
            )
            onComplete()
        }
    }

    fun loginLocalAccount(
        identifier: String,
        password: String,
        onResult: (com.example.data.local.LoginLocalResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = preferencesRepository.loginLocalAccount(identifier, password)
            onResult(result)
        }
    }

    fun resetLocalPasswordAndLogin(
        newPassword: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val ok = preferencesRepository.resetLocalPasswordAndLogin(newPassword)
            onResult(ok)
        }
    }

    fun logoutLocalAccount(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            preferencesRepository.logoutLocalAccount()
            onComplete()
        }
    }

    fun updateLocalProfileAndSocialLinks(
        penName: String,
        bio: String,
        socialLinks: com.example.data.model.SocialLinks,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            preferencesRepository.updateLocalProfileAndSocialLinks(
                penName = penName,
                bio = bio,
                socialLinks = socialLinks
            )
            onComplete()
        }
    }

    fun updateEasyProfile(
        penName: String,
        bio: String,
        profilePhotoUri: String,
        socialLinks: com.example.data.model.SocialLinks? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            preferencesRepository.updateEasyProfile(
                penName = penName,
                bio = bio,
                profilePhotoUri = profilePhotoUri,
                socialLinks = socialLinks
            )
            onComplete()
        }
    }

    fun quickCreateNameAccount(
        fullName: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            preferencesRepository.quickCreateNameAccount(fullName)
            onComplete()
        }
    }

    fun updateOnlineCommunityPosts(posts: List<CommunityShayari>) {
        val mapped = posts.mapNotNull { post ->
            val cleanCouplet = post.couplet.trim()
            if (cleanCouplet.isEmpty()) return@mapNotNull null
            val cat = ShayariCategory.fromId(post.categoryId)
            val displayMood = post.promptTitle.trim().ifBlank { cat.titleHi }
            ShayariEntity(
                id = onlineCommunityCatalogId(post.id),
                couplet = cleanCouplet,
                poet = post.penName.trim().ifBlank { "शायर दोस्त" },
                categoryId = cat.id,
                moodTag = displayMood,
                isFavorite = false,
                isFeatured = false
            )
        }
        onlineCommunityPostsFlow.value = mapped
    }

    fun upsertUserPostToCatalog(
        personalId: String,
        couplet: String,
        authorName: String,
        title: String = "",
        themeTag: String = "",
        primaryCategoryId: String? = null
    ) {
        viewModelScope.launch {
            repository.upsertUserPostToCatalog(
                personalId = personalId,
                couplet = couplet,
                authorName = authorName,
                title = title,
                themeTag = themeTag,
                primaryCategoryId = primaryCategoryId
            )
        }
    }

    fun removeUserPostFromCatalog(personalId: String) {
        viewModelScope.launch {
            repository.removeUserPostFromCatalog(personalId)
        }
    }

    fun setDailyNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyNotificationSettings(enabled, hour, minute)
        }
    }

    companion object {
        const val ONLINE_COMMUNITY_ID_BASE = 2_000_000

        fun onlineCommunityCatalogId(communityDocId: String): Int {
            val positiveHash = (communityDocId.hashCode() and 0x3FFFFFFF) % 900_000_000
            return ONLINE_COMMUNITY_ID_BASE + positiveHash
        }

        private fun dedupeKey(couplet: String, poet: String): String {
            return "${couplet.trim().replace(Regex("\\s+"), " ").lowercase()}|${poet.trim().lowercase()}"
        }

        private val POPULAR_POETS = setOf(
            "मिर्ज़ा ग़ालिब",
            "गुलज़ार",
            "अल्लामा इक़बाल",
            "अहमद फ़राज़",
            "दुष्यंत कुमार",
            "बशीर बद्र",
            "निदा फ़ाज़ली",
            "फ़ैज़ अहमद फ़ैज़",
            "मुनव्वर राना",
            "बिस्मिल अज़ीमाबादी",
            "राहत इंदौरी",
            "जौन एलिया",
            "परवीन शाकिर",
            "कुमार विश्वास",
            "वसीम बरेलवी",
            "मीर तक़ी मीर"
        )
    }

    class Factory(
        private val repository: ShayariRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShayariViewModel(repository, preferencesRepository) as T
        }
    }
}
