package com.example.data.model

import androidx.annotation.FontRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R

enum class CardStyle(
    val id: String,
    val labelHi: String,
    val startColor: Color,
    val endColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val usesArtBackdrop: Boolean = false
) {
    NIGHT_MUSHAIRA(
        id = "night_mushaira",
        labelHi = "शब-ए-ग़ज़ल",
        startColor = Color(0xFF1A1228),
        endColor = Color(0xFF0E0916),
        textColor = Color(0xFFFDF6EA),
        accentColor = Color(0xFFE6BE78),
        usesArtBackdrop = true
    ),
    ROYAL_VELVET(
        id = "royal_velvet",
        labelHi = "शाही मख़मल",
        startColor = Color(0xFF6B2031),
        endColor = Color(0xFF2B0A13),
        textColor = Color(0xFFFFF5EE),
        accentColor = Color(0xFFF0C782)
    ),
    IVORY_PARCHMENT(
        id = "ivory_parchment",
        labelHi = "सुनहरा काग़ज़",
        startColor = Color(0xFFFCF6EA),
        endColor = Color(0xFFEDE0C8),
        textColor = Color(0xFF241914),
        accentColor = Color(0xFF8C5A1B)
    ),
    EMERALD_MOONLIGHT(
        id = "emerald_moonlight",
        labelHi = "चाँदनी रात",
        startColor = Color(0xFF143536),
        endColor = Color(0xFF09191B),
        textColor = Color(0xFFF1FAF8),
        accentColor = Color(0xFF9BE0D8)
    );

    companion object {
        fun fromId(id: String): CardStyle =
            entries.find { it.id == id } ?: NIGHT_MUSHAIRA
    }
}

enum class FontScaleOption(
    val id: String,
    val labelHi: String,
    val cardSp: Float,
    val heroSp: Float,
    val readerSp: Float
) {
    SMALL("small", "छोटा", cardSp = 15.5f, heroSp = 17.5f, readerSp = 19f),
    MEDIUM("medium", "मीडियम", cardSp = 18f, heroSp = 20f, readerSp = 22f),
    LARGE("large", "बड़ा", cardSp = 20.5f, heroSp = 22.5f, readerSp = 26f),
    EXTRA_LARGE("extra_large", "बहुत बड़ा", cardSp = 23f, heroSp = 25f, readerSp = 30f);

    companion object {
        fun fromId(id: String): FontScaleOption =
            entries.find { it.id == id } ?: MEDIUM
    }
}

enum class ShayariFontStyle(
    val id: String,
    val labelHi: String,
    val subtitleHi: String,
    @param:FontRes val fontRes: Int,
    val lineHeightFactor: Float = 1.64f,
    val letterSpacingSp: Float = 0.2f
) {
    CLASSIC_SERIF(
        id = "classic_serif",
        labelHi = "क्लासिक",
        subtitleHi = "साफ़ किताब जैसा",
        fontRes = R.font.noto_serif_devanagari,
        lineHeightFactor = 1.65f,
        letterSpacingSp = 0.2f
    ),
    TRADITIONAL_PRINT(
        id = "traditional_print",
        labelHi = "देसी प्रिंट",
        subtitleHi = "किताबी स्टाइल",
        fontRes = R.font.tiro_devanagari_hindi,
        lineHeightFactor = 1.68f,
        letterSpacingSp = 0.15f
    ),
    MODERN_SANS(
        id = "modern_sans",
        labelHi = "मॉडर्न",
        subtitleHi = "सिंपल और साफ़",
        fontRes = R.font.noto_sans_devanagari,
        lineHeightFactor = 1.60f,
        letterSpacingSp = 0.1f
    ),
    HANDWRITTEN(
        id = "handwritten",
        labelHi = "हाथ की लिखावट",
        subtitleHi = "पेन से लिखा हुआ",
        fontRes = R.font.kalam,
        lineHeightFactor = 1.58f,
        letterSpacingSp = 0.25f
    ),
    ROYAL_DISPLAY(
        id = "royal_display",
        labelHi = "बोल्ड स्टाइल",
        subtitleHi = "गहरा और मोटा",
        fontRes = R.font.rozha_one,
        lineHeightFactor = 1.55f,
        letterSpacingSp = 0.15f
    );

    val fontFamily: FontFamily by lazy {
        FontFamily(Font(fontRes, FontWeight.Normal))
    }

    companion object {
        fun fromId(id: String): ShayariFontStyle =
            entries.find { it.id == id } ?: CLASSIC_SERIF
    }
}

enum class ThemeModeOption(val id: String, val labelHi: String) {
    SYSTEM("system", "ऑटो"),
    LIGHT("light", "लाइट"),
    DARK("dark", "डार्क");

    companion object {
        fun fromId(id: String): ThemeModeOption =
            entries.find { it.id == id } ?: SYSTEM
    }
}
