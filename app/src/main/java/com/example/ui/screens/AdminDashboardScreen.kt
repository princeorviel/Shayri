package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.rememberGoogleSignInLauncher
import com.example.data.model.ShayariCategory
import com.example.ui.viewmodel.AdminCategorySortOrder
import com.example.ui.viewmodel.AdminCategoryStatItem
import com.example.ui.viewmodel.AdminDashboardUiState
import com.example.ui.viewmodel.AdminStatsLoadState
import com.example.ui.viewmodel.buildAdminCategoryStatRows
import kotlin.math.roundToInt

@Composable
fun AdminDashboardScreen(
    isFirebaseSignedIn: Boolean,
    uiState: AdminDashboardUiState,
    catalogCategoryCounts: Map<String, Int> = emptyMap(),
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (AdminCategorySortOrder) -> Unit,
    onToggleOnlyPopulated: () -> Unit,
    onRefresh: () -> Unit,
    onCategoryClick: (ShayariCategory) -> Unit = {},
    onOpenKaviManch: () -> Unit = {},
    onAuthSuccess: () -> Unit = {},
    onAuthError: (String) -> Unit = {},
    onSignInClickOverride: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isCloudConfigured: Boolean = true
) {
    BackHandler(onBack = onBack)

    val googleSignIn = rememberGoogleSignInLauncher(
        onAuthSuccess = onAuthSuccess,
        onAuthError = onAuthError,
        onClickOverride = onSignInClickOverride
    )

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val totalCatalogCount = remember(catalogCategoryCounts) { catalogCategoryCounts.values.sum() }
    val activeCatalogCategories = remember(catalogCategoryCounts) {
        catalogCategoryCounts.count { it.value > 0 }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("admin_dashboard_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("admin_dashboard_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.admin_dashboard_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isCloudConfigured) {
                            stringResource(R.string.admin_dashboard_subtitle)
                        } else {
                            stringResource(R.string.admin_dashboard_catalog_subtitle)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isCloudConfigured && isFirebaseSignedIn) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !uiState.isRefreshing,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("admin_dashboard_refresh_button")
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.admin_dashboard_refresh_cd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (!isCloudConfigured) {
                AdminCatalogDashboardContent(
                    uiState = uiState,
                    catalogCategoryCounts = catalogCategoryCounts,
                    totalCatalogCount = totalCatalogCount,
                    activeCatalogCategories = activeCatalogCategories,
                    isDarkTheme = isDarkTheme,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onSortOrderChanged = onSortOrderChanged,
                    onToggleOnlyPopulated = onToggleOnlyPopulated,
                    onCategoryClick = onCategoryClick
                )
            } else if (!isFirebaseSignedIn) {
                AdminCatalogDashboardContent(
                    uiState = uiState,
                    catalogCategoryCounts = catalogCategoryCounts,
                    totalCatalogCount = totalCatalogCount,
                    activeCatalogCategories = activeCatalogCategories,
                    isDarkTheme = isDarkTheme,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onSortOrderChanged = onSortOrderChanged,
                    onToggleOnlyPopulated = onToggleOnlyPopulated,
                    onCategoryClick = onCategoryClick,
                    authGateContent = {
                        AdminAuthRequiredCard(
                            isLoading = googleSignIn.isLoading,
                            onSignInClick = { googleSignIn.launch() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            } else {
                when (val state = uiState.loadState) {
                    AdminStatsLoadState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .testTag("admin_dashboard_loading"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = stringResource(R.string.admin_dashboard_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is AdminStatsLoadState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .testTag("admin_dashboard_error"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 460.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = onRefresh,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("admin_dashboard_retry_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.ai_retry))
                                    }
                                }
                            }
                        }
                    }

                    is AdminStatsLoadState.Success -> {
                        val snapshot = state.snapshot
                        val rows = remember(
                            snapshot.countsByCategory,
                            catalogCategoryCounts,
                            uiState.searchQuery,
                            uiState.sortOrder,
                            uiState.showOnlyPopulated
                        ) {
                            buildAdminCategoryStatRows(
                                firestoreCounts = snapshot.countsByCategory,
                                catalogCounts = catalogCategoryCounts,
                                searchQuery = uiState.searchQuery,
                                sortOrder = uiState.sortOrder,
                                showOnlyPopulated = uiState.showOnlyPopulated
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("admin_category_stats_list"),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 6.dp,
                                bottom = 28.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "summary_hero") {
                                AdminSummaryHeroCard(
                                    totalFirestoreCount = snapshot.totalShayariCount,
                                    activeCategories = snapshot.activeCategoryCount,
                                    totalCategories = ShayariCategory.entries.size,
                                    totalWahCount = snapshot.totalWahCount,
                                    totalCatalogCount = totalCatalogCount
                                )
                            }

                            if (snapshot.totalShayariCount == 0) {
                                item(key = "empty_firestore_callout") {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_empty_firestore_banner")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Groups,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Firestore में अभी 0 कम्युनिटी शायरी हैं",
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                    text = "कवि मंच से नई शायरी पोस्ट होते ही यहाँ लाइव संख्या अपडेट होगी।",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                                )
                                            }
                                            FilledTonalButton(
                                                onClick = onOpenKaviManch,
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                                            ) {
                                                Text(
                                                    text = "कवि मंच",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item(key = "search_and_filters") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = onSearchQueryChanged,
                                        placeholder = { Text(stringResource(R.string.admin_dashboard_search_hint)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = null
                                            )
                                        },
                                        trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                                            {
                                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = stringResource(R.string.action_cancel)
                                                    )
                                                }
                                            }
                                        } else null,
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_dashboard_search_input")
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AdminCategorySortOrder.entries.forEach { order ->
                                            FilterChip(
                                                selected = uiState.sortOrder == order,
                                                onClick = { onSortOrderChanged(order) },
                                                label = { Text(order.label) },
                                                modifier = Modifier.testTag("admin_sort_${order.name.lowercase()}")
                                            )
                                        }

                                        FilterChip(
                                            selected = uiState.showOnlyPopulated,
                                            onClick = onToggleOnlyPopulated,
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.FilterList,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text("केवल > 0") },
                                            modifier = Modifier.testTag("admin_filter_populated")
                                        )
                                    }
                                }
                            }

                            item(key = "list_section_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "कैटेगरी के अनुसार Firestore शायरी",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${rows.size} कैटेगरी",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (rows.isEmpty()) {
                                item(key = "no_matching_categories") {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "इस फ़िल्टर से कोई कैटेगरी नहीं मिली।",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp)
                                        )
                                    }
                                }
                            } else {
                                items(
                                    items = rows,
                                    key = { it.category.id }
                                ) { statItem ->
                                    AdminCategoryStatRow(
                                        item = statItem,
                                        isDarkTheme = isDarkTheme,
                                        onClick = { onCategoryClick(statItem.category) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSummaryHeroCard(
    totalFirestoreCount: Int,
    activeCategories: Int,
    totalCategories: Int,
    totalWahCount: Long,
    totalCatalogCount: Int
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f)
        )
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_summary_hero_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "लाइव Firestore आँकड़े",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                ) {
                    Text(
                        text = "$activeCategories / $totalCategories सक्रिय",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminMetricTile(
                    value = "$totalFirestoreCount",
                    label = "Firestore शायरी",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_total_firestore_count")
                )
                AdminMetricTile(
                    value = "$totalWahCount",
                    label = "कुल वाह! लाइक",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_total_wah_count")
                )
                if (totalCatalogCount > 0) {
                    AdminMetricTile(
                        value = "$totalCatalogCount",
                        label = "ऐप कैटलॉग",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_total_catalog_count")
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminMetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminCatalogDashboardContent(
    uiState: AdminDashboardUiState,
    catalogCategoryCounts: Map<String, Int>,
    totalCatalogCount: Int,
    activeCatalogCategories: Int,
    isDarkTheme: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (AdminCategorySortOrder) -> Unit,
    onToggleOnlyPopulated: () -> Unit,
    onCategoryClick: (ShayariCategory) -> Unit,
    authGateContent: (@Composable () -> Unit)? = null
) {
    val rows = remember(
        catalogCategoryCounts,
        uiState.searchQuery,
        uiState.sortOrder,
        uiState.showOnlyPopulated
    ) {
        buildAdminCategoryStatRows(
            firestoreCounts = emptyMap(),
            catalogCounts = catalogCategoryCounts,
            searchQuery = uiState.searchQuery,
            sortOrder = uiState.sortOrder,
            showOnlyPopulated = uiState.showOnlyPopulated,
            useCatalogAsPrimary = true
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_category_stats_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 6.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (authGateContent != null) {
            item(key = "auth_gate_card") {
                authGateContent()
            }
        }

        if (authGateContent == null || catalogCategoryCounts.isNotEmpty()) {
            item(key = "catalog_summary_hero") {
                AdminCatalogSummaryHeroCard(
                    totalCatalogCount = totalCatalogCount,
                    activeCategories = activeCatalogCategories,
                    totalCategories = ShayariCategory.entries.size
                )
            }

            if (authGateContent == null) {
                item(key = "offline_cloud_notice") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_offline_cloud_notice")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.admin_dashboard_offline_notice),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item(key = "catalog_search_and_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text(stringResource(R.string.admin_dashboard_search_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.action_cancel)
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_dashboard_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AdminCategorySortOrder.entries.forEach { order ->
                            FilterChip(
                                selected = uiState.sortOrder == order,
                                onClick = { onSortOrderChanged(order) },
                                label = { Text(order.label) },
                                modifier = Modifier.testTag("admin_sort_${order.name.lowercase()}")
                            )
                        }

                        FilterChip(
                            selected = uiState.showOnlyPopulated,
                            onClick = onToggleOnlyPopulated,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("केवल > 0") },
                            modifier = Modifier.testTag("admin_filter_populated")
                        )
                    }
                }
            }

            item(key = "catalog_list_section_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "कैटेगरी के अनुसार कुल शायरी",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${rows.size} कैटेगरी",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (rows.isEmpty()) {
                item(key = "catalog_no_matching_categories") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "इस फ़िल्टर से कोई कैटेगरी नहीं मिली।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }
            } else {
                items(
                    items = rows,
                    key = { it.category.id }
                ) { statItem ->
                    AdminCategoryStatRow(
                        item = statItem,
                        isDarkTheme = isDarkTheme,
                        useCatalogAsPrimary = true,
                        onClick = { onCategoryClick(statItem.category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminCatalogSummaryHeroCard(
    totalCatalogCount: Int,
    activeCategories: Int,
    totalCategories: Int
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f)
        )
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_summary_hero_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ऐप कैटलॉग आँकड़े",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                ) {
                    Text(
                        text = "$activeCategories / $totalCategories सक्रिय",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminMetricTile(
                    value = "$totalCatalogCount",
                    label = "कुल शायरी",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_total_catalog_count")
                )
                AdminMetricTile(
                    value = "$totalCategories",
                    label = "कुल कैटेगरी",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_total_categories_count")
                )
            }
        }
    }
}

@Composable
private fun AdminCategoryStatRow(
    item: AdminCategoryStatItem,
    isDarkTheme: Boolean,
    useCatalogAsPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val accent = if (isDarkTheme) item.category.darkAccent else item.category.lightAccent
    val primaryCount = if (useCatalogAsPrimary) item.catalogCount else item.firestoreCount
    val percentLabel = "${(item.shareFraction * 100f).roundToInt()}%"

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("admin_category_row_${item.category.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.category.emoji,
                            fontSize = 18.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.category.titleHi,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (!useCatalogAsPrimary && item.catalogCount > 0) {
                            "${item.category.labelEn} • कैटलॉग: ${item.catalogCount}"
                        } else {
                            item.category.labelEn
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (primaryCount > 0) {
                        accent.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = BorderStroke(
                        1.dp,
                        if (primaryCount > 0) accent.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.testTag("admin_category_count_${item.category.id}")
                ) {
                    Text(
                        text = "$primaryCount शायरी",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (primaryCount > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { item.shareFraction },
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
                Text(
                    text = percentLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AdminAuthRequiredCard(
    isLoading: Boolean,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = modifier.testTag("admin_auth_gate")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(
                text = "Firestore Admin Dashboard के लिए लॉगिन करें",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Google अकाउंट से लॉगिन करके आप Firestore में हर कैटेगरी की कुल शायरी संख्या और लाइव आँकड़े देख सकते हैं।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onSignInClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_dashboard_google_signin")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sign_in_with_google),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
