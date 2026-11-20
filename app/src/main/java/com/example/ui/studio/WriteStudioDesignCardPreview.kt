package com.example.ui.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ShayariTagMatcher
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
    val formattedHashTags = ShayariTagMatcher.parseTags(tag).take(4).joinToString("  ") { "#$it" }
    val topLabel = listOfNotNull(
        title.trim().takeIf { it.isNotEmpty() },
        formattedHashTags.takeIf { it.isNotEmpty() }
    ).joinToString(" • ")

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
            if (design.cardTemplate == StudioCardTemplate.POLAROID_FRAME) {
                // Polaroid Gallery Frame layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFFDF9))
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        StudioWallpaperBackground(design = design)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = horizontalAlignment,
                            verticalArrangement = verticalArrangement
                        ) {
                            if (topLabel.isNotEmpty()) {
                                Text(
                                    text = topLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = subTextColor,
                                    textAlign = design.textAlign.composeAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
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
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF8C7B6B),
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        if (signature.isNotBlank()) {
                            Text(
                                text = "— $signature",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = design.fontStyle.fontFamily,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF2B2220),
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    StudioWallpaperBackground(design = design)

                    // Template decorative overlays
                    when (design.cardTemplate) {
                        StudioCardTemplate.EDITORIAL_NOTEBOOK -> {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val lineSpacing = 28.dp.toPx()
                                var y = 38.dp.toPx()
                                while (y < size.height - 16.dp.toPx()) {
                                    drawLine(
                                        color = design.textColor.copy(alpha = 0.10f),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    y += lineSpacing
                                }
                                val marginX = 28.dp.toPx()
                                drawLine(
                                    color = design.accentColor.copy(alpha = 0.36f),
                                    start = Offset(marginX, 0f),
                                    end = Offset(marginX, size.height),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                            }
                        }

                        StudioCardTemplate.ROYAL_GOLD_FRAME -> {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val outer = 12.dp.toPx()
                                val inner = 18.dp.toPx()
                                drawRoundRect(
                                    color = design.accentColor.copy(alpha = 0.65f),
                                    topLeft = Offset(outer, outer),
                                    size = Size(size.width - outer * 2, size.height - outer * 2),
                                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                                    style = Stroke(width = 1.4.dp.toPx())
                                )
                                drawRoundRect(
                                    color = design.accentColor.copy(alpha = 0.30f),
                                    topLeft = Offset(inner, inner),
                                    size = Size(size.width - inner * 2, size.height - inner * 2),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                    style = Stroke(width = 0.8.dp.toPx())
                                )
                            }
                        }

                        StudioCardTemplate.MAGAZINE_STRIPE -> {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(8.dp)
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(design.accentColor, design.accentColor.copy(alpha = 0.55f))
                                        )
                                    )
                            )
                            Text(
                                text = "”",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 90.sp,
                                    lineHeight = 90.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = design.accentColor.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 18.dp, top = 4.dp)
                            )
                        }

                        StudioCardTemplate.VINTAGE_CORNERS -> {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val inset = 14.dp.toPx()
                                val len = 22.dp.toPx()
                                val stroke = 1.8.dp.toPx()
                                val c = design.accentColor.copy(alpha = 0.65f)
                                drawLine(c, Offset(inset, inset), Offset(inset + len, inset), stroke)
                                drawLine(c, Offset(inset, inset), Offset(inset, inset + len), stroke)
                                drawLine(c, Offset(size.width - inset, inset), Offset(size.width - inset - len, inset), stroke)
                                drawLine(c, Offset(size.width - inset, inset), Offset(size.width - inset, inset + len), stroke)
                                drawLine(c, Offset(inset, size.height - inset), Offset(inset + len, size.height - inset), stroke)
                                drawLine(c, Offset(inset, size.height - inset), Offset(inset, size.height - inset - len), stroke)
                                drawLine(c, Offset(size.width - inset, size.height - inset), Offset(size.width - inset - len, size.height - inset), stroke)
                                drawLine(c, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - len), stroke)
                            }
                        }

                        StudioCardTemplate.GLASS_QUOTE -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(16.dp)
                            ) {}
                        }

                        else -> Unit
                    }

                    val startPad = when (design.cardTemplate) {
                        StudioCardTemplate.EDITORIAL_NOTEBOOK -> 40.dp
                        StudioCardTemplate.MAGAZINE_STRIPE -> 28.dp
                        StudioCardTemplate.GLASS_QUOTE -> 30.dp
                        else -> 24.dp
                    }
                    val endPad = if (design.cardTemplate == StudioCardTemplate.GLASS_QUOTE) 30.dp else 24.dp

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = startPad, end = endPad, top = 24.dp, bottom = 26.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = horizontalAlignment,
                        verticalArrangement = verticalArrangement
                    ) {
                        if (design.cardTemplate == StudioCardTemplate.EDITORIAL_NOTEBOOK ||
                            design.cardTemplate == StudioCardTemplate.ROYAL_GOLD_FRAME
                        ) {
                            Text(
                                text = if (design.cardTemplate == StudioCardTemplate.ROYAL_GOLD_FRAME) "✦  ❝  ✦" else "❝",
                                style = MaterialTheme.typography.headlineMedium,
                                color = design.accentColor.copy(alpha = 0.85f),
                                textAlign = design.textAlign.composeAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (topLabel.isNotEmpty()) {
                            Text(
                                text = topLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.4.sp
                                ),
                                color = design.accentColor.copy(alpha = 0.92f),
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
}

@Composable
private fun StudioWallpaperBackground(design: ShayariDesignState) {
    when (val wp = design.wallpaper) {
        is StudioWallpaper.SolidOrGradient -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(wp.startColor, wp.endColor)))
            )
            if (design.scrimDimness > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = design.scrimDimness.coerceIn(0f, 0.85f)))
            )
        }

        is StudioWallpaper.GalleryPhoto -> {
            AsyncImage(
                model = wp.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = design.scrimDimness.coerceIn(0f, 0.85f)))
            )
        }
    }
}
