package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CardStyle
import com.example.data.model.FontScaleOption
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariFontStyle
import com.example.ui.theme.LocalShayariReadingStyle

@Composable
fun ShayariArtCard(
    shayari: ShayariEntity,
    cardStyle: CardStyle,
    fontScale: FontScaleOption,
    fontStyle: ShayariFontStyle = LocalShayariReadingStyle.current.fontStyle,
    modifier: Modifier = Modifier
) {
    val category = ShayariCategory.fromId(shayari.categoryId)

    Surface(
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(cardStyle.startColor, cardStyle.endColor)
                    )
                )
        ) {
            if (cardStyle.usesArtBackdrop) {
                OptimizedDrawableImage(
                    resId = category.backgroundFor(shayari),
                    contentDescription = stringResource(R.string.cd_card_backdrop),
                    widthPx = 720,
                    heightPx = 900,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.12f)
                        .clip(RoundedCornerShape(26.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88100A18))
                )
            }

            // Double ornamental frame drawn smoothly via Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outerInset = 18.dp.toPx()
                val innerInset = 25.dp.toPx()
                drawRoundRect(
                    color = cardStyle.accentColor.copy(alpha = 0.55f),
                    topLeft = Offset(outerInset, outerInset),
                    size = Size(size.width - outerInset * 2, size.height - outerInset * 2),
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                    style = Stroke(width = 1.4.dp.toPx())
                )
                drawRoundRect(
                    color = cardStyle.accentColor.copy(alpha = 0.25f),
                    topLeft = Offset(innerInset, innerInset),
                    size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top category ornament
                Text(
                    text = "✦  ${category.titleHi}  ✦",
                    style = MaterialTheme.typography.labelLarge,
                    color = cardStyle.accentColor,
                    textAlign = TextAlign.Center
                )

                // Center couplet
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "❝",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp),
                        color = cardStyle.accentColor.copy(alpha = 0.72f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = shayari.couplet,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = fontStyle.fontFamily,
                            fontSize = fontScale.readerSp.sp,
                            lineHeight = (fontScale.readerSp * fontStyle.lineHeightFactor).sp,
                            letterSpacing = fontStyle.letterSpacingSp.sp
                        ),
                        color = cardStyle.textColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(1.5.dp)
                                .background(cardStyle.accentColor.copy(alpha = 0.65f))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = shayari.poet,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = fontStyle.fontFamily
                            ),
                            color = cardStyle.accentColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(1.5.dp)
                                .background(cardStyle.accentColor.copy(alpha = 0.65f))
                        )
                    }
                }

                // Bottom brand flourish
                Text(
                    text = "शायरी • दिल की बात",
                    style = MaterialTheme.typography.labelSmall,
                    color = cardStyle.textColor.copy(alpha = 0.58f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
