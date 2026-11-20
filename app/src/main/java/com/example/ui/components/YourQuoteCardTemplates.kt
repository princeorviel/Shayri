package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariTagMatcher
import com.example.ui.theme.LocalShayariReadingStyle

/**
 * User-selectable YourQuote.in-inspired feed card modes on Home.
 */
enum class FeedCardStyleMode(
    val labelHi: String,
    val emoji: String
) {
    MIX_YOURQUOTE("मिक्स कार्ड", "✨"),
    PAPER_DIARY("डायरी पेपर", "📄"),
    POLAROID("पोलरॉइड", "🖼️"),
    ROYAL_DARK("रॉयल गोल्ड", "👑"),
    PASTEL_ART("पेस्टल आर्ट", "🎨"),
    PHOTO_CINEMA("फ़ोटो कार्ड", "🌄")
}

enum class YourQuoteFeedLook {
    EDITORIAL_PAPER,
    POLAROID_GALLERY,
    VELVET_GOLD,
    PASTEL_WATERCOLOR,
    MODERN_MAGAZINE,
    VINTAGE_DIARY,
    AURORA_GLASS,
    CINEMATIC_PHOTO
}

private data class PastelPalette(
    val start: Color,
    val end: Color,
    val orbA: Color,
    val orbB: Color,
    val ink: Color,
    val accent: Color
)

private val PASTEL_PALETTES = listOf(
    PastelPalette(
        start = Color(0xFFFFF1F4),
        end = Color(0xFFF9DCE7),
        orbA = Color(0x44E11D48),
        orbB = Color(0x33F59E0B),
        ink = Color(0xFF2B1220),
        accent = Color(0xFFB81D4D)
    ),
    PastelPalette(
        start = Color(0xFFF3EEFF),
        end = Color(0xFFE2D9FB),
        orbA = Color(0x407C3AED),
        orbB = Color(0x33EC4899),
        ink = Color(0xFF1F1638),
        accent = Color(0xFF6D28D9)
    ),
    PastelPalette(
        start = Color(0xFFEEF9F6),
        end = Color(0xFFD6F0EA),
        orbA = Color(0x3B0D9488),
        orbB = Color(0x2E38BDF8),
        ink = Color(0xFF102A28),
        accent = Color(0xFF0F766E)
    ),
    PastelPalette(
        start = Color(0xFFFFF6EB),
        end = Color(0xFFFBE4CA),
        orbA = Color(0x3DF97316),
        orbB = Color(0x33E11D48),
        ink = Color(0xFF2D1A10),
        accent = Color(0xFFC2410C)
    )
)

private val ROYAL_DARK_GRADIENTS = listOf(
    listOf(Color(0xFF1B1028), Color(0xFF0D0816)),
    listOf(Color(0xFF3E1020), Color(0xFF19060E)),
    listOf(Color(0xFF0E242B), Color(0xFF061116)),
    listOf(Color(0xFF181822), Color(0xFF0B0B10))
)

private val AURORA_GRADIENTS = listOf(
    listOf(Color(0xFF2E1065), Color(0xFF701A75), Color(0xFF9D174D)),
    listOf(Color(0xFF0F172A), Color(0xFF1E3A5F), Color(0xFF0F766E)),
    listOf(Color(0xFF3B0764), Color(0xFF4C1D95), Color(0xFF1E1B4B)),
    listOf(Color(0xFF450A0A), Color(0xFF7C2D12), Color(0xFF1C1917))
)

private val MAGAZINE_ACCENTS = listOf(
    Color(0xFFE11D48),
    Color(0xFF7C3AED),
    Color(0xFF0F766E),
    Color(0xFFD97706),
    Color(0xFF2563EB)
)

private val DIVERSE_PHOTO_POOL = intArrayOf(
    R.drawable.img_bg_candle_lake,
    R.drawable.img_bg_romantic,
    R.drawable.img_hero_mehfil,
    R.drawable.img_card_mushaira,
    R.drawable.img_bg_bewafai,
    R.drawable.img_bg_aansu,
    R.drawable.img_bg_musafir,
    R.drawable.img_bg_zindagi,
    R.drawable.img_bg_prakriti_rain,
    R.drawable.img_bg_khuda,
    R.drawable.img_bg_aaina,
    R.drawable.img_bg_waqt,
    R.drawable.img_bg_dosti,
    R.drawable.img_bg_motivation,
    R.drawable.img_bg_tareef,
    R.drawable.img_bg_sharabi,
    R.drawable.img_bg_secret_love,
    R.drawable.img_bg_genz,
    R.drawable.img_bg_festive,
    R.drawable.img_bg_maut,
    R.drawable.img_bg_attitude,
    R.drawable.img_bg_nibba_nibbi
)

private val PHOTO_CARD_SCRIM = Brush.verticalGradient(
    colors = listOf(
        Color(0x2E0A0812),
        Color(0x7A0C0915),
        Color(0xBF080510)
    )
)

private val DARK_TEXT_SHADOW = Shadow(
    color = Color.Black.copy(alpha = 0.78f),
    offset = Offset(0f, 1.5f),
    blurRadius = 4f
)

private val CANVAS_SHAPE = RoundedCornerShape(14.dp)
private val INNER_FRAME_SHAPE = RoundedCornerShape(10.dp)
private val TAG_PILL_SHAPE = RoundedCornerShape(50)

fun resolveYourQuoteFeedLook(
    shayariId: Int,
    mode: FeedCardStyleMode
): YourQuoteFeedLook {
    val hash = kotlin.math.abs(shayariId * 2654435761L).toInt().let { kotlin.math.abs(it) }
    return when (mode) {
        FeedCardStyleMode.MIX_YOURQUOTE -> {
            val allLooks = YourQuoteFeedLook.entries
            allLooks[hash % allLooks.size]
        }
        FeedCardStyleMode.PAPER_DIARY -> when (hash % 3) {
            0 -> YourQuoteFeedLook.EDITORIAL_PAPER
            1 -> YourQuoteFeedLook.VINTAGE_DIARY
            else -> YourQuoteFeedLook.MODERN_MAGAZINE
        }
        FeedCardStyleMode.POLAROID -> YourQuoteFeedLook.POLAROID_GALLERY
        FeedCardStyleMode.ROYAL_DARK -> if (hash % 2 == 0) {
            YourQuoteFeedLook.VELVET_GOLD
        } else {
            YourQuoteFeedLook.AURORA_GLASS
        }
        FeedCardStyleMode.PASTEL_ART -> YourQuoteFeedLook.PASTEL_WATERCOLOR
        FeedCardStyleMode.PHOTO_CINEMA -> YourQuoteFeedLook.CINEMATIC_PHOTO
    }
}

@DrawableRes
fun resolveDiversePhotoForCard(shayari: ShayariEntity, category: ShayariCategory): Int {
    if (category.isJokeCategory) return category.backgroundResId
    val hash = kotlin.math.abs((shayari.id * 31 + shayari.couplet.hashCode()) and 0x7FFFFFFF)
    // Mix category's native art with the full diverse gallery so no two cards in a category look identical
    return if (hash % 4 == 0) {
        category.backgroundFor(shayari)
    } else {
        DIVERSE_PHOTO_POOL[hash % DIVERSE_PHOTO_POOL.size]
    }
}

private fun cleanPoetSignature(raw: String): String =
    raw.trim().trimStart('_', '-', '—', '–', '•', ' ').trim()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YourQuoteCardCanvas(
    shayari: ShayariEntity,
    category: ShayariCategory,
    styleMode: FeedCardStyleMode,
    isOnlinePost: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cleanShayari = remember(shayari) {
        val cleaned = cleanPoetSignature(shayari.poet)
        if (cleaned == shayari.poet) shayari else shayari.copy(poet = cleaned)
    }
    val readingStyle = LocalShayariReadingStyle.current
    val look = remember(cleanShayari.id, styleMode) {
        resolveYourQuoteFeedLook(cleanShayari.id, styleMode)
    }
    val hashSeed = remember(cleanShayari.id) {
        kotlin.math.abs((cleanShayari.id * 37) xor (cleanShayari.couplet.hashCode() and 0xFFFF))
    }
    val artworkResId = remember(cleanShayari.id, cleanShayari.categoryId, cleanShayari.moodTag) {
        resolveDiversePhotoForCard(cleanShayari, category)
    }
    val hashTags = remember(cleanShayari.moodTag, cleanShayari.categoryId) {
        ShayariTagMatcher.displayHashTags(cleanShayari.moodTag, cleanShayari.categoryId, maxTags = 3)
    }

    when (look) {
        YourQuoteFeedLook.EDITORIAL_PAPER -> {
            EditorialPaperCardCanvas(
                shayari = cleanShayari,
                hashTags = hashTags,
                isOnlinePost = isOnlinePost,
                accentColor = MAGAZINE_ACCENTS[hashSeed % MAGAZINE_ACCENTS.size],
                modifier = modifier
            )
        }

        YourQuoteFeedLook.POLAROID_GALLERY -> {
            PolaroidGalleryCardCanvas(
                shayari = cleanShayari,
                artworkResId = artworkResId,
                hashTags = hashTags,
                isOnlinePost = isOnlinePost,
                modifier = modifier
            )
        }

        YourQuoteFeedLook.VELVET_GOLD -> {
            VelvetGoldCardCanvas(
                shayari = cleanShayari,
                gradient = ROYAL_DARK_GRADIENTS[hashSeed % ROYAL_DARK_GRADIENTS.size],
                hashTags = hashTags,
                isOnlinePost = isOnlinePost,
                modifier = modifier
            )
        }

        YourQuoteFeedLook.PASTEL_WATERCOLOR -> {
            PastelWatercolorCardCanvas(
                shayari = cleanShayari,
                palette = PASTEL_PALETTES[hashSeed % PASTEL_PALETTES.size],
                hashTags = hashTags,
                isOnlinePost = isOnlinePost,
                modifier = modifier
            )
        }

        YourQuoteFeedLook.MODERN_MAGAZINE -> {
            ModernMagazineCardCanvas(
                shayari = cleanShayari,
                accent = MAGAZINE_ACCENTS[hashSeed % MAGAZINE_ACCENTS.size],
                hashTags = hashTags,
                isOnlinePost = isOnlinePost,
                modifier = modifier
            )
        }

        YourQuoteFeedLook.VINTAGE_DIARY -> {
            VintageDiaryCardCanvas(
                shayari = cleanShayari,
                hashTags = hashTags,
                isOnlinePost = isOnlinePost,
                modifier = modifier
            )
        }

        YourQuoteFeedLook.AURORA_GLASS -> {
            AuroraGlassCardCanvas(
                shayari = cleanShayari,
                gradient = AURORA_GRADIENTS[hashSeed % AURORA_GRADIENTS.size],
                hashTags = hashTags,
                isOnlinePost = isOnlinePost,
                modifier = modifier
            )
        }

        YourQuoteFeedLook.CINEMATIC_PHOTO -> {
            CinematicPhotoCardCanvas(
                shayari = cleanShayari,
                artworkResId = artworkResId,
                hashTags = hashTags,
                isOnlinePost = isOnlinePost,
                readingStyle = readingStyle,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorialPaperCardCanvas(
    shayari: ShayariEntity,
    hashTags: List<String>,
    isOnlinePost: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val readingStyle = LocalShayariReadingStyle.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .heightIn(min = 232.dp)
            .clip(CANVAS_SHAPE)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFCF6), Color(0xFFF6EFE1))))
            .border(1.dp, Color(0xFFE6DAC6), CANVAS_SHAPE)
            .testTag("shayari_bg_image_${shayari.id}")
    ) {
        // Classic YourQuote ruled diary lines + vertical margin line
        Canvas(modifier = Modifier.matchParentSize()) {
            val lineSpacing = 28.dp.toPx()
            var y = 36.dp.toPx()
            while (y < size.height - 12.dp.toPx()) {
                drawLine(
                    color = Color(0x208C7458),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += lineSpacing
            }
            val marginX = 28.dp.toPx()
            drawLine(
                color = accentColor.copy(alpha = 0.28f),
                start = Offset(marginX, 0f),
                end = Offset(marginX, size.height),
                strokeWidth = 1.4.dp.toPx()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, end = 22.dp, top = 20.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❝",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, lineHeight = 32.sp),
                    color = accentColor.copy(alpha = 0.8f)
                )
                if (isOnlinePost) {
                    OnlineBadgePill(darkSurface = false)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = shayari.couplet,
                style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge).copy(
                    fontSize = (readingStyle.fontScale.cardSp + 1f).sp,
                    lineHeight = ((readingStyle.fontScale.cardSp + 1f) * readingStyle.fontStyle.lineHeightFactor).sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF1F1916),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(2.dp)
                        .background(accentColor.copy(alpha = 0.65f))
                )
                if (shayari.poet.isNotBlank()) {
                    Text(
                        text = "— ${shayari.poet}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = readingStyle.fontStyle.fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Italic
                        ),
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (hashTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                CardHashTagRow(tags = hashTags, darkSurface = false, accentColor = accentColor)
            }
        }
    }
}

@Composable
private fun PolaroidGalleryCardCanvas(
    shayari: ShayariEntity,
    @DrawableRes artworkResId: Int,
    hashTags: List<String>,
    isOnlinePost: Boolean,
    modifier: Modifier = Modifier
) {
    val readingStyle = LocalShayariReadingStyle.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .clip(CANVAS_SHAPE)
            .background(Color(0xFFFFFDF9))
            .border(1.dp, Color(0xFFE6E0D6), CANVAS_SHAPE)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 14.dp)
            .testTag("shayari_bg_image_${shayari.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 178.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            OptimizedDrawableImage(
                resId = artworkResId,
                contentDescription = null,
                widthPx = 720,
                heightPx = 420,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(PHOTO_CARD_SCRIM)
            )
            if (isOnlinePost) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    OnlineBadgePill(darkSurface = true)
                }
            }
            Text(
                text = shayari.couplet,
                style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge).copy(
                    fontSize = (readingStyle.fontScale.cardSp + 0.5f).sp,
                    lineHeight = ((readingStyle.fontScale.cardSp + 0.5f) * readingStyle.fontStyle.lineHeightFactor).sp,
                    fontWeight = FontWeight.Medium,
                    shadow = DARK_TEXT_SHADOW
                ),
                color = Color(0xFFFFFBF4),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hashTags.isNotEmpty()) {
                CardHashTagRow(
                    tags = hashTags.take(2),
                    darkSurface = false,
                    accentColor = Color(0xFF9E2A43),
                    modifier = Modifier.weight(1f, fill = false)
                )
            } else {
                Text(
                    text = "✦ शायरी",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8C7B6B)
                )
            }
            if (shayari.poet.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "— ${shayari.poet}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = readingStyle.fontStyle.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic
                    ),
                    color = Color(0xFF2B2220),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VelvetGoldCardCanvas(
    shayari: ShayariEntity,
    gradient: List<Color>,
    hashTags: List<String>,
    isOnlinePost: Boolean,
    modifier: Modifier = Modifier
) {
    val readingStyle = LocalShayariReadingStyle.current
    val gold = Color(0xFFEAC784)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .heightIn(min = 234.dp)
            .clip(CANVAS_SHAPE)
            .background(Brush.linearGradient(gradient))
            .testTag("shayari_bg_image_${shayari.id}"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val outer = 10.dp.toPx()
            val inner = 15.dp.toPx()
            drawRoundRect(
                color = gold.copy(alpha = 0.52f),
                topLeft = Offset(outer, outer),
                size = Size(size.width - outer * 2, size.height - outer * 2),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawRoundRect(
                color = gold.copy(alpha = 0.24f),
                topLeft = Offset(inner, inner),
                size = Size(size.width - inner * 2, size.height - inner * 2),
                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                style = Stroke(width = 0.8.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isOnlinePost) {
                OnlineBadgePill(darkSurface = true)
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Text(
                    text = "✦   ❝   ✦",
                    style = MaterialTheme.typography.labelLarge,
                    color = gold.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = shayari.couplet,
                style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge).copy(
                    fontSize = (readingStyle.fontScale.cardSp + 1f).sp,
                    lineHeight = ((readingStyle.fontScale.cardSp + 1f) * readingStyle.fontStyle.lineHeightFactor).sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFFFFF8EE),
                textAlign = TextAlign.Center
            )

            if (shayari.poet.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(1.dp)
                            .background(gold.copy(alpha = 0.65f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = shayari.poet,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = readingStyle.fontStyle.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(1.dp)
                            .background(gold.copy(alpha = 0.65f))
                    )
                }
            }

            if (hashTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                CardHashTagRow(tags = hashTags, darkSurface = true, accentColor = gold)
            }
        }
    }
}

@Composable
private fun PastelWatercolorCardCanvas(
    shayari: ShayariEntity,
    palette: PastelPalette,
    hashTags: List<String>,
    isOnlinePost: Boolean,
    modifier: Modifier = Modifier
) {
    val readingStyle = LocalShayariReadingStyle.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .heightIn(min = 232.dp)
            .clip(CANVAS_SHAPE)
            .background(Brush.linearGradient(listOf(palette.start, palette.end)))
            .border(1.dp, palette.accent.copy(alpha = 0.18f), CANVAS_SHAPE)
            .testTag("shayari_bg_image_${shayari.id}"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.orbA, Color.Transparent),
                    center = Offset(size.width * 0.14f, size.height * 0.18f),
                    radius = size.minDimension * 0.62f
                ),
                center = Offset(size.width * 0.14f, size.height * 0.18f),
                radius = size.minDimension * 0.62f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.orbB, Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.84f),
                    radius = size.minDimension * 0.68f
                ),
                center = Offset(size.width * 0.88f, size.height * 0.84f),
                radius = size.minDimension * 0.68f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isOnlinePost) {
                OnlineBadgePill(darkSurface = false)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (hashTags.isNotEmpty()) {
                CardHashTagRow(tags = hashTags, darkSurface = false, accentColor = palette.accent)
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = shayari.couplet,
                style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge).copy(
                    fontSize = (readingStyle.fontScale.cardSp + 1f).sp,
                    lineHeight = ((readingStyle.fontScale.cardSp + 1f) * readingStyle.fontStyle.lineHeightFactor).sp,
                    fontWeight = FontWeight.Medium
                ),
                color = palette.ink,
                textAlign = TextAlign.Center
            )

            if (shayari.poet.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "— ${shayari.poet}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = readingStyle.fontStyle.fontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ModernMagazineCardCanvas(
    shayari: ShayariEntity,
    accent: Color,
    hashTags: List<String>,
    isOnlinePost: Boolean,
    modifier: Modifier = Modifier
) {
    val readingStyle = LocalShayariReadingStyle.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .heightIn(min = 228.dp)
            .clip(CANVAS_SHAPE)
            .background(Color(0xFFFCFBFA))
            .border(1.dp, accent.copy(alpha = 0.22f), CANVAS_SHAPE)
            .testTag("shayari_bg_image_${shayari.id}")
    ) {
        // Bold left accent stripe
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(7.dp)
                .matchParentSize()
                .background(
                    Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.55f)))
                )
        )

        // Subtle oversized quote watermark in top-right
        Text(
            text = "”",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 86.sp,
                lineHeight = 86.sp,
                fontWeight = FontWeight.Black
            ),
            color = accent.copy(alpha = 0.10f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 22.dp, top = 22.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (isOnlinePost) {
                OnlineBadgePill(darkSurface = false)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = shayari.couplet,
                style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge).copy(
                    fontSize = (readingStyle.fontScale.cardSp + 1f).sp,
                    lineHeight = ((readingStyle.fontScale.cardSp + 1f) * readingStyle.fontStyle.lineHeightFactor).sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF18181F),
                textAlign = TextAlign.Start
            )

            if (shayari.poet.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "— ${shayari.poet}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = readingStyle.fontStyle.fontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hashTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                CardHashTagRow(tags = hashTags, darkSurface = false, accentColor = accent)
            }
        }
    }
}

@Composable
private fun VintageDiaryCardCanvas(
    shayari: ShayariEntity,
    hashTags: List<String>,
    isOnlinePost: Boolean,
    modifier: Modifier = Modifier
) {
    val readingStyle = LocalShayariReadingStyle.current
    val sepiaInk = Color(0xFF3A271B)
    val sepiaAccent = Color(0xFF8A562C)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .heightIn(min = 232.dp)
            .clip(CANVAS_SHAPE)
            .background(Brush.verticalGradient(listOf(Color(0xFFF6EBD7), Color(0xFFEAD7B7))))
            .border(1.dp, Color(0xFFD2B48C), CANVAS_SHAPE)
            .testTag("shayari_bg_image_${shayari.id}"),
        contentAlignment = Alignment.Center
    ) {
        // Antique corner brackets drawn via Canvas
        Canvas(modifier = Modifier.matchParentSize()) {
            val inset = 12.dp.toPx()
            val len = 18.dp.toPx()
            val stroke = 1.6.dp.toPx()
            val c = sepiaAccent.copy(alpha = 0.55f)
            // Top-left
            drawLine(c, Offset(inset, inset), Offset(inset + len, inset), stroke)
            drawLine(c, Offset(inset, inset), Offset(inset, inset + len), stroke)
            // Top-right
            drawLine(c, Offset(size.width - inset, inset), Offset(size.width - inset - len, inset), stroke)
            drawLine(c, Offset(size.width - inset, inset), Offset(size.width - inset, inset + len), stroke)
            // Bottom-left
            drawLine(c, Offset(inset, size.height - inset), Offset(inset + len, size.height - inset), stroke)
            drawLine(c, Offset(inset, size.height - inset), Offset(inset, size.height - inset - len), stroke)
            // Bottom-right
            drawLine(c, Offset(size.width - inset, size.height - inset), Offset(size.width - inset - len, size.height - inset), stroke)
            drawLine(c, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - len), stroke)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isOnlinePost) {
                OnlineBadgePill(darkSurface = false)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = shayari.couplet,
                style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge).copy(
                    fontSize = (readingStyle.fontScale.cardSp + 1f).sp,
                    lineHeight = ((readingStyle.fontScale.cardSp + 1f) * readingStyle.fontStyle.lineHeightFactor).sp,
                    fontWeight = FontWeight.Medium
                ),
                color = sepiaInk,
                textAlign = TextAlign.Center
            )

            if (shayari.poet.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "~ ${shayari.poet} ~",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = readingStyle.fontStyle.fontFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = sepiaAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hashTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                CardHashTagRow(tags = hashTags, darkSurface = false, accentColor = sepiaAccent)
            }
        }
    }
}

@Composable
private fun AuroraGlassCardCanvas(
    shayari: ShayariEntity,
    gradient: List<Color>,
    hashTags: List<String>,
    isOnlinePost: Boolean,
    modifier: Modifier = Modifier
) {
    val readingStyle = LocalShayariReadingStyle.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .heightIn(min = 234.dp)
            .clip(CANVAS_SHAPE)
            .background(Brush.linearGradient(gradient))
            .padding(14.dp)
            .testTag("shayari_bg_image_${shayari.id}"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.13f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isOnlinePost) {
                    OnlineBadgePill(darkSurface = true)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = shayari.couplet,
                    style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge).copy(
                        fontSize = (readingStyle.fontScale.cardSp + 1f).sp,
                        lineHeight = ((readingStyle.fontScale.cardSp + 1f) * readingStyle.fontStyle.lineHeightFactor).sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                if (shayari.poet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "— ${shayari.poet}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = readingStyle.fontStyle.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFFFDE68A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (hashTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    CardHashTagRow(tags = hashTags, darkSurface = true, accentColor = Color(0xFFFDE68A))
                }
            }
        }
    }
}

@Composable
private fun CinematicPhotoCardCanvas(
    shayari: ShayariEntity,
    @DrawableRes artworkResId: Int,
    hashTags: List<String>,
    isOnlinePost: Boolean,
    readingStyle: com.example.ui.theme.ShayariReadingStyle,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .heightIn(min = 232.dp)
            .clip(CANVAS_SHAPE),
        contentAlignment = Alignment.Center
    ) {
        OptimizedDrawableImage(
            resId = artworkResId,
            contentDescription = null,
            widthPx = 720,
            heightPx = 480,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .testTag("shayari_bg_image_${shayari.id}")
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(PHOTO_CARD_SCRIM)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(7.dp)
                .border(
                    width = 0.9.dp,
                    color = Color.White.copy(alpha = 0.26f),
                    shape = INNER_FRAME_SHAPE
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isOnlinePost) {
                OnlineBadgePill(darkSurface = true)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (hashTags.isNotEmpty()) {
                CardHashTagRow(tags = hashTags, darkSurface = true, accentColor = Color(0xFFF5DEB0))
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = shayari.couplet,
                style = readingStyle.cardCoupletTextStyle(MaterialTheme.typography.bodyLarge).copy(
                    fontSize = (readingStyle.fontScale.cardSp + 1f).sp,
                    lineHeight = ((readingStyle.fontScale.cardSp + 1f) * readingStyle.fontStyle.lineHeightFactor).sp,
                    fontWeight = FontWeight.Medium,
                    shadow = DARK_TEXT_SHADOW
                ),
                color = Color(0xFFFDF8F2),
                textAlign = TextAlign.Center
            )

            if (shayari.poet.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "— ${shayari.poet}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = readingStyle.fontStyle.fontFamily,
                        shadow = DARK_TEXT_SHADOW
                    ),
                    color = Color(0xFFF3D59B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun OnlineBadgePill(darkSurface: Boolean) {
    Surface(
        shape = TAG_PILL_SHAPE,
        color = if (darkSurface) Color(0x4410B981) else Color(0x1E059669),
        border = BorderStroke(
            0.8.dp,
            if (darkSurface) Color(0x9934D399) else Color(0x66059669)
        )
    ) {
        Text(
            text = "🌐 ऑनलाइन",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (darkSurface) Color(0xFFA7F3D0) else Color(0xFF065F46),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardHashTagRow(
    tags: List<String>,
    darkSurface: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = TAG_PILL_SHAPE,
                color = if (darkSurface) Color.Black.copy(alpha = 0.30f) else accentColor.copy(alpha = 0.10f),
                border = BorderStroke(
                    0.8.dp,
                    if (darkSurface) Color.White.copy(alpha = 0.25f) else accentColor.copy(alpha = 0.28f)
                )
            ) {
                Text(
                    text = "#$tag",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (darkSurface) accentColor else accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
