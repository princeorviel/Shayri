package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun ShayariBottomBar(
    isHomeSelected: Boolean,
    isFavoritesSelected: Boolean,
    isAccountSelected: Boolean,
    currentUserPhotoUrl: Uri?,
    currentUserDisplayName: String?,
    isSignedIn: Boolean,
    onHomeClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onCreateClick: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val homeLabel = stringResource(R.string.nav_mehfil)
    val favoritesLabel = stringResource(R.string.nav_favorites)
    val createLabel = stringResource(R.string.mehfil_write_invite_cta)
    val accountLabel = stringResource(R.string.nav_account)

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .defaultMinSize(minHeight = 62.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                BottomBarSlot(
                    label = homeLabel,
                    showLabel = isHomeSelected,
                    selected = isHomeSelected,
                    onClick = onHomeClick,
                    testTag = "nav_tab_mehfil",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isHomeSelected) Icons.Rounded.Home else Icons.Outlined.Home,
                        contentDescription = null,
                        tint = if (isHomeSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(25.dp)
                    )
                }

                // 2. Favorites
                BottomBarSlot(
                    label = favoritesLabel,
                    showLabel = isFavoritesSelected,
                    selected = isFavoritesSelected,
                    onClick = onFavoritesClick,
                    testTag = "nav_tab_favorites",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isFavoritesSelected) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = null,
                        tint = if (isFavoritesSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 3. Create (+) Circular Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onCreateClick)
                        .semantics { contentDescription = createLabel }
                        .testTag("nav_tab_create")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // 4. Account (Icon or logged-in user photo)
                BottomBarSlot(
                    label = accountLabel,
                    showLabel = isAccountSelected,
                    selected = isAccountSelected,
                    onClick = onAccountClick,
                    testTag = "nav_tab_account",
                    modifier = Modifier.weight(1f)
                ) {
                    AccountAvatar(
                        photoUrl = currentUserPhotoUrl,
                        displayName = currentUserDisplayName,
                        isSignedIn = isSignedIn,
                        isSelected = isAccountSelected,
                        size = 25.dp,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarSlot(
    label: String,
    showLabel: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 4.dp)
            .semantics { contentDescription = label }
            .testTag(testTag)
    ) {
        iconContent()
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
