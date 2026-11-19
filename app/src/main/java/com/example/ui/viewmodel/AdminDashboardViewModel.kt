package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ShayariCategory
import com.example.data.remote.AdminCategoryStatsSnapshot
import com.example.data.remote.AdminStatsDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AdminCategorySortOrder(val label: String) {
    HIGHEST_COUNT("ज़्यादा से कम"),
    LOWEST_COUNT("कम से ज़्यादा"),
    ALPHABETICAL("A–Z")
}

data class AdminCategoryStatItem(
    val category: ShayariCategory,
    val firestoreCount: Int,
    val catalogCount: Int = 0,
    val shareFraction: Float = 0f
)

sealed interface AdminStatsLoadState {
    data object Loading : AdminStatsLoadState
    data class Success(val snapshot: AdminCategoryStatsSnapshot) : AdminStatsLoadState
    data class Error(val message: String) : AdminStatsLoadState
}

data class AdminDashboardUiState(
    val loadState: AdminStatsLoadState = AdminStatsLoadState.Loading,
    val searchQuery: String = "",
    val sortOrder: AdminCategorySortOrder = AdminCategorySortOrder.HIGHEST_COUNT,
    val showOnlyPopulated: Boolean = false,
    val isRefreshing: Boolean = false
)

class AdminDashboardViewModel(
    private val repository: AdminStatsDataSource
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(AdminCategorySortOrder.HIGHEST_COUNT)
    private val showOnlyPopulated = MutableStateFlow(false)
    private val isRefreshing = MutableStateFlow(false)
    private val manualOverride = MutableStateFlow<AdminStatsLoadState?>(null)

    private val streamState: StateFlow<AdminStatsLoadState> = repository.observeCategoryStats()
        .map<AdminCategoryStatsSnapshot, AdminStatsLoadState> { snapshot ->
            manualOverride.value = null
            AdminStatsLoadState.Success(snapshot)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(
                AdminStatsLoadState.Error(
                    rootError.message?.takeIf { it.isNotBlank() }
                        ?: "Firestore से शायरी आँकड़े लोड नहीं हो सके।"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AdminStatsLoadState.Loading
        )

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        streamState,
        manualOverride,
        searchQuery,
        sortOrder,
        showOnlyPopulated,
        isRefreshing
    ) { values ->
        val stream = values[0] as AdminStatsLoadState
        val override = values[1] as AdminStatsLoadState?
        AdminDashboardUiState(
            loadState = override ?: stream,
            searchQuery = values[2] as String,
            sortOrder = values[3] as AdminCategorySortOrder,
            showOnlyPopulated = values[4] as Boolean,
            isRefreshing = values[5] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AdminDashboardUiState()
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query.take(60)
    }

    fun onSortOrderChanged(order: AdminCategorySortOrder) {
        sortOrder.value = order
    }

    fun onToggleOnlyPopulated() {
        showOnlyPopulated.value = !showOnlyPopulated.value
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            val result = repository.fetchCategoryStats()
            result.onSuccess { snapshot ->
                manualOverride.value = AdminStatsLoadState.Success(snapshot)
            }.onFailure { error ->
                manualOverride.value = AdminStatsLoadState.Error(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "डेटा रिफ्रेश नहीं हो सका, कृपया फिर कोशिश करें।"
                )
            }
            isRefreshing.value = false
        }
    }

    class Factory(
        private val repository: AdminStatsDataSource
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminDashboardViewModel(repository) as T
        }
    }
}

fun buildAdminCategoryStatRows(
    firestoreCounts: Map<String, Int>,
    catalogCounts: Map<String, Int>,
    searchQuery: String,
    sortOrder: AdminCategorySortOrder,
    showOnlyPopulated: Boolean
): List<AdminCategoryStatItem> {
    val totalFirestore = firestoreCounts.values.sum().coerceAtLeast(1)
    val query = searchQuery.trim().lowercase()

    val items = ShayariCategory.entries
        .map { category ->
            val fsCount = firestoreCounts[category.id] ?: 0
            AdminCategoryStatItem(
                category = category,
                firestoreCount = fsCount,
                catalogCount = catalogCounts[category.id] ?: 0,
                shareFraction = if (firestoreCounts.values.sum() == 0) {
                    0f
                } else {
                    (fsCount.toFloat() / totalFirestore.toFloat()).coerceIn(0f, 1f)
                }
            )
        }
        .filter { item ->
            (!showOnlyPopulated || item.firestoreCount > 0) &&
                (query.isBlank() ||
                    item.category.titleHi.lowercase().contains(query) ||
                    item.category.labelEn.lowercase().contains(query) ||
                    item.category.id.lowercase().contains(query))
        }

    return when (sortOrder) {
        AdminCategorySortOrder.HIGHEST_COUNT -> items.sortedWith(
            compareByDescending<AdminCategoryStatItem> { it.firestoreCount }
                .thenBy { it.category.ordinal }
        )
        AdminCategorySortOrder.LOWEST_COUNT -> items.sortedWith(
            compareBy<AdminCategoryStatItem> { it.firestoreCount }
                .thenBy { it.category.ordinal }
        )
        AdminCategorySortOrder.ALPHABETICAL -> items.sortedBy { it.category.labelEn.lowercase() }
    }
}
