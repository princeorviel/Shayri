package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import kotlinx.coroutines.launch

@Composable
fun FloatingScrollButtons(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val canScrollUp by remember(listState) {
        derivedStateOf { listState.canScrollBackward }
    }
    val canScrollDown by remember(listState) {
        derivedStateOf { listState.canScrollForward }
    }
    val upAlpha by animateFloatAsState(
        targetValue = if (canScrollUp) 1f else 0.55f,
        animationSpec = tween(180),
        label = "scrollUpAlpha"
    )
    val downAlpha by animateFloatAsState(
        targetValue = if (canScrollDown) 1f else 0.55f,
        animationSpec = tween(180),
        label = "scrollDownAlpha"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = {
                if (canScrollUp) {
                    coroutineScope.launch {
                        if (listState.firstVisibleItemIndex > 60) {
                            listState.scrollToItem(12)
                        }
                        listState.animateScrollToItem(0)
                    }
                }
            },
            enabled = canScrollUp,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(48.dp)
                .minimumInteractiveComponentSize()
                .alpha(upAlpha)
                .testTag("scroll_up_button")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = stringResource(R.string.action_scroll_up),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Surface(
            onClick = {
                if (canScrollDown) {
                    coroutineScope.launch {
                        val totalItems = listState.layoutInfo.totalItemsCount
                        if (totalItems > 0) {
                            val target = totalItems - 1
                            if (target - listState.firstVisibleItemIndex > 60) {
                                listState.scrollToItem((target - 12).coerceAtLeast(0))
                            }
                            listState.animateScrollToItem(
                                index = target,
                                scrollOffset = 50_000
                            )
                        }
                    }
                }
            },
            enabled = canScrollDown,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(48.dp)
                .minimumInteractiveComponentSize()
                .alpha(downAlpha)
                .testTag("scroll_down_button")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = stringResource(R.string.action_scroll_down),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
