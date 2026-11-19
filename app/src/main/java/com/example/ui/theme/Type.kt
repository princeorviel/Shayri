package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.FontScaleOption
import com.example.data.model.ShayariFontStyle

val RozhaDisplayFontFamily = FontFamily(
    Font(R.font.rozha_one, FontWeight.Normal)
)

val DevanagariSerifFontFamily = FontFamily(
    Font(R.font.noto_serif_devanagari, FontWeight.Normal)
)

val PoppinsUiFontFamily = FontFamily(
    Font(R.font.poppins, FontWeight.Normal)
)

val ShayariTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RozhaDisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RozhaDisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RozhaDisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DevanagariSerifFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DevanagariSerifFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DevanagariSerifFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PoppinsUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PoppinsUiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PoppinsUiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PoppinsUiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)

@Immutable
data class ShayariReadingStyle(
    val fontScale: FontScaleOption = FontScaleOption.MEDIUM,
    val fontStyle: ShayariFontStyle = ShayariFontStyle.CLASSIC_SERIF
) {
    fun cardCoupletTextStyle(base: TextStyle): TextStyle = base.copy(
        fontFamily = fontStyle.fontFamily,
        fontSize = fontScale.cardSp.sp,
        lineHeight = (fontScale.cardSp * fontStyle.lineHeightFactor).sp,
        letterSpacing = fontStyle.letterSpacingSp.sp
    )

    fun heroCoupletTextStyle(base: TextStyle): TextStyle = base.copy(
        fontFamily = fontStyle.fontFamily,
        fontSize = fontScale.heroSp.sp,
        lineHeight = (fontScale.heroSp * fontStyle.lineHeightFactor).sp,
        letterSpacing = fontStyle.letterSpacingSp.sp
    )

    fun readerCoupletTextStyle(base: TextStyle): TextStyle = base.copy(
        fontFamily = fontStyle.fontFamily,
        fontSize = fontScale.readerSp.sp,
        lineHeight = (fontScale.readerSp * fontStyle.lineHeightFactor).sp,
        letterSpacing = fontStyle.letterSpacingSp.sp
    )
}

val LocalShayariReadingStyle = staticCompositionLocalOf { ShayariReadingStyle() }
