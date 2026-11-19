package com.example.ui.components

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision

/**
 * Loads a bundled photographic drawable asynchronously via Coil at an explicit bounded
 * target pixel size with RGB_565 (50% lower bitmap RAM than ARGB_8888) and shared memory caching.
 */
@Composable
fun OptimizedDrawableImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    widthPx: Int,
    heightPx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val request = remember(context, resId, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(resId)
            .size(widthPx, heightPx)
            .precision(Precision.INEXACT)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}
