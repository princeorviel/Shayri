package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ShayariRepository
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.CardStyle
import com.example.data.model.FontScaleOption
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariFontStyle
import com.example.data.model.ThemeModeOption
import com.example.util.HinglishTransliterator
import java.util.Calendar
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
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

    init {
        if (autoSeed) {
            viewModelScope.launch {
                repository.ensureSeeded()
            }
        }
    }

    private data class CatalogDerivedState(
        val allShayari: List<ShayariEntity>,
        val categoryCounts: Map<String, Int>,
        val popularShayari: List<ShayariEntity>,
        val featuredShayari: ShayariEntity?
    )

    private data class FilterResult(
        val filtered: List<ShayariEntity>,
        val selectedCategoryId: String?,
        val searchQuery: String
    )

    private val catalogDerivedFlow = combine(
        repository.allShayari,
        refreshSeedFlow
    ) { all, refreshSeed ->
        val counts = HashMap<String, Int>(ShayariCategory.entries.size * 2)
        val featuredPool = ArrayList<ShayariEntity>(64)
        val renownedPoetPool = ArrayList<ShayariEntity>(256)

        for (item in all) {
            counts[item.categoryId] = (counts[item.categoryId] ?: 0) + 1
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
            allShayari = all,
            categoryCounts = counts,
            popularShayari = popularList,
            featuredShayari = popularList.firstOrNull()
        )
    }.flowOn(Dispatchers.Default)

    private val filterInputsFlow = combine(
        searchQueryFlow,
        selectedCategoryFlow,
        refreshSeedFlow
    ) { query, selectedCat, refreshSeed ->
        Triple(query, selectedCat, refreshSeed)
    }

    private val filteredFlow = combine(
        repository.allShayari,
        filterInputsFlow
    ) { all, (query, selectedCat, refreshSeed) ->
        val trimmed = query.trim()
        val baseFiltered = when {
            trimmed.isEmpty() && selectedCat == null -> all
            trimmed.isEmpty() -> all.filter { it.categoryId == selectedCat }
            else -> {
                val hindiQuery = if (HinglishTransliterator.hasConvertibleHinglish(trimmed)) {
                    HinglishTransliterator.transliterateText(trimmed).trim()
                } else {
                    ""
                }
                val matchingCategoryIds = ShayariCategory.entries
                    .filter { cat ->
                        cat.titleHi.contains(trimmed, ignoreCase = true) ||
                            cat.labelEn.contains(trimmed, ignoreCase = true) ||
                            (hindiQuery.isNotEmpty() && cat.titleHi.contains(hindiQuery, ignoreCase = true))
                    }
                    .mapTo(HashSet()) { it.id }

                all.filter { item ->
                    if (selectedCat != null && item.categoryId != selectedCat) return@filter false
                    val matchesRaw = item.categoryId in matchingCategoryIds ||
                        item.couplet.contains(trimmed, ignoreCase = true) ||
                        item.poet.contains(trimmed, ignoreCase = true) ||
                        item.moodTag.contains(trimmed, ignoreCase = true)
                    if (matchesRaw) return@filter true
                    hindiQuery.isNotEmpty() && (
                        item.couplet.contains(hindiQuery, ignoreCase = true) ||
                            item.poet.contains(hindiQuery, ignoreCase = true) ||
                            item.moodTag.contains(hindiQuery, ignoreCase = true)
                        )
                }
            }
        }
        val filtered = if (refreshSeed > 0L) {
            baseFiltered.shuffled(Random(refreshSeed))
        } else {
            baseFiltered
        }
        FilterResult(
            filtered = filtered,
            selectedCategoryId = selectedCat,
            searchQuery = query
        )
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<ShayariUiState> = combine(
        catalogDerivedFlow,
        filteredFlow,
        repository.favoriteShayari,
        preferencesRepository.preferencesFlow
    ) { catalog, filterResult, favorites, prefs ->
        ShayariUiState(
            isLoading = false,
            allShayari = catalog.allShayari,
            filteredShayari = filterResult.filtered,
            favoriteShayari = favorites,
            featuredShayari = catalog.featuredShayari,
            popularShayari = catalog.popularShayari,
            categoryCounts = catalog.categoryCounts,
            selectedCategoryId = filterResult.selectedCategoryId,
            searchQuery = filterResult.searchQuery,
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShayariUiState()
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

    fun setDailyNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyNotificationSettings(enabled, hour, minute)
        }
    }

    private companion object {
        val POPULAR_POETS = setOf(
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
