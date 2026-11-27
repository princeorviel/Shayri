package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Smooth, GPU-accelerated fade-in and subtle upward settle for Shayari text
 * whenever a feed card is loaded, scrolled into view, or restyled.
 */
fun Modifier.shayariTextFadeIn(
    revealKey: Any,
    durationMillis: Int = 420,
    delayMillis: Int = 0,
    slideUpDp: Dp = 10.dp
): Modifier = composed {
    val progress = remember(revealKey) { Animatable(0f) }
    val slideUpPx = with(LocalDensity.current) { slideUpDp.toPx() }

    LaunchedEffect(revealKey) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        )
    }

    this.graphicsLayer {
        val value = progress.value.coerceIn(0f, 1f)
        alpha = value
        translationY = (1f - value) * slideUpPx
    }
}

/**
 * Tracks a smooth 0f -> 1f reveal whenever a pager item becomes the active page
 * (on initial load, vertical swipe, or feed refresh), and resets cleanly once off-screen.
 */
@Composable
fun rememberPagerRevealProgress(
    isCurrentPage: Boolean,
    contentKey: Any,
    durationMillis: Int = 460,
    delayMillis: Int = 40
): Float {
    val progress = remember(contentKey) { Animatable(0f) }

    LaunchedEffect(isCurrentPage, contentKey) {
        if (isCurrentPage) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    delayMillis = delayMillis,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            progress.snapTo(0f)
        }
    }

    return progress.value.coerceIn(0f, 1f)
}
