package com.example.ui.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.OptimizedDrawableImage

@Composable
fun WriteStudioDesignCardPreview(
    couplet: String,
    authorName: String,
    title: String,
    tag: String,
    design: ShayariDesignState,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(18.dp)
    val signature = authorName.trim()
    val horizontalAlignment = when (design.textAlign) {
        StudioTextAlign.START -> Alignment.Start
        StudioTextAlign.CENTER -> Alignment.CenterHorizontally
        StudioTextAlign.END -> Alignment.End
    }
    val verticalArrangement = when (design.verticalAlign) {
        StudioVerticalAlign.TOP -> Arrangement.Top
        StudioVerticalAlign.CENTER -> Arrangement.Center
        StudioVerticalAlign.BOTTOM -> Arrangement.Bottom
    }
    val subTextColor = design.textColor.copy(alpha = 0.78f)
    val watermarkColor = design.textColor.copy(alpha = 0.55f)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val ratio = 4f / 4.2f
        val availableWidth = maxWidth.coerceAtMost(420.dp).coerceAtLeast(0.dp)
        val (cardWidth, cardHeight) = if (constraints.hasBoundedHeight && availableWidth / ratio > maxHeight) {
            val boundedHeight = maxHeight.coerceAtLeast(0.dp)
            (boundedHeight * ratio) to boundedHeight
        } else {
            availableWidth to (availableWidth / ratio)
        }

        Surface(
            shape = cardShape,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            modifier = Modifier
                .size(width = cardWidth, height = cardHeight)
                .clip(cardShape)
                .testTag("studio_live_card_preview")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            when (val wp = design.wallpaper) {
                is StudioWallpaper.SolidOrGradient -> {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Brush.verticalGradient(listOf(wp.startColor, wp.endColor)))
                    )
                    if (design.scrimDimness > 0.01f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = (design.scrimDimness * 0.7f).coerceIn(0f, 0.75f)))
                        )
                    }
                }

                is StudioWallpaper.DrawableArt -> {
                    OptimizedDrawableImage(
                        resId = wp.resId,
                        contentDescription = null,
                        widthPx = 720,
                        heightPx = 720,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = design.scrimDimness.coerceIn(0f, 0.85f)))
                    )
                }

                is StudioWallpaper.GalleryPhoto -> {
                    AsyncImage(
                        model = wp.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = design.scrimDimness.coerceIn(0f, 0.85f)))
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = verticalArrangement
            ) {
                val topLabel = listOfNotNull(
                    title.trim().takeIf { it.isNotEmpty() },
                    tag.trim().takeIf { it.isNotEmpty() }?.let { "#$it" }
                ).joinToString(" • ")

                if (topLabel.isNotEmpty()) {
                    Text(
                        text = topLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = subTextColor,
                        textAlign = design.textAlign.composeAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = couplet.ifBlank { "यहाँ आपकी शायरी दिखेगी…" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = design.fontStyle.fontFamily,
                        fontSize = design.fontSizeSp.sp,
                        lineHeight = (design.fontSizeSp * design.fontStyle.lineHeightFactor).sp,
                        letterSpacing = design.fontStyle.letterSpacingSp.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = design.textColor,
                    textAlign = design.textAlign.composeAlign,
                    modifier = Modifier.fillMaxWidth()
                )

                if (signature.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "— $signature",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = design.fontStyle.fontFamily,
                            fontSize = (design.fontSizeSp * 0.72f).coerceIn(13f, 18f).sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = subTextColor,
                        textAlign = design.textAlign.composeAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
            }

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                ),
                color = watermarkColor,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            }
        }
    }
}
