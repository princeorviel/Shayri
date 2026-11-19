package com.example.ui.studio

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.example.R
import com.example.data.model.ShayariFontStyle

enum class StudioStep {
    WRITE,
    DESIGN
}

enum class DesignTab {
    WALLPAPER,
    TEXT
}

enum class StudioTextAlign(
    val labelHi: String,
    val composeAlign: TextAlign
) {
    START("बाएँ", TextAlign.Start),
    CENTER("बीच", TextAlign.Center),
    END("दाएँ", TextAlign.End)
}

enum class StudioVerticalAlign(val labelHi: String) {
    TOP("ऊपर"),
    CENTER("बीच"),
    BOTTOM("नीचे")
}

sealed interface StudioWallpaper {
    val id: String
    val labelHi: String
    val keywords: String

    data class SolidOrGradient(
        override val id: String,
        override val labelHi: String,
        override val keywords: String,
        val startColor: Color,
        val endColor: Color,
        val defaultTextColor: Color,
        val defaultAccentColor: Color
    ) : StudioWallpaper

    data class DrawableArt(
        override val id: String,
        override val labelHi: String,
        override val keywords: String,
        @param:DrawableRes val resId: Int,
        val isLightBackdrop: Boolean = false
    ) : StudioWallpaper

    data class GalleryPhoto(
        val uri: Uri,
        override val id: String = "gallery_${uri.toString().hashCode()}",
        override val labelHi: String = "गैलरी फ़ोटो",
        override val keywords: String = "gallery photo custom गैलरी"
    ) : StudioWallpaper
}

data class ShayariDesignState(
    val wallpaper: StudioWallpaper = StudioWallpapers.defaultWallpaper,
    val scrimDimness: Float = 0.35f,
    val fontStyle: ShayariFontStyle = ShayariFontStyle.CLASSIC_SERIF,
    val fontSizeSp: Float = 21f,
    val textColor: Color = Color(0xFFFFFFFF),
    val accentColor: Color = Color(0xFFE6BE78),
    val textAlign: StudioTextAlign = StudioTextAlign.CENTER,
    val verticalAlign: StudioVerticalAlign = StudioVerticalAlign.CENTER
)

object StudioTextColors {
    val palette: List<Pair<String, Color>> = listOf(
        "सफ़ेद" to Color(0xFFFFFFFF),
        "क्रीम" to Color(0xFFFDF6EA),
        "सुनहरा" to Color(0xFFE6BE78),
        "गुलाबी" to Color(0xFFFFA6B8),
        "आसमानी" to Color(0xFF9BE0D8),
        "काला" to Color(0xFF18181F),
        "स्याही" to Color(0xFF1E324C),
        "महरून" to Color(0xFF6B182C)
    )
}

object StudioPromptStarters {
    val starters: List<String> = listOf(
        "ख़ामोशी में भी एक बात छिपी होती है,\n",
        "ज़िंदगी के इस मोड़ पर आकर समझ आया,\n",
        "चाँदनी रात में तेरी याद का साया,\n",
        "हम मुसाफ़िर हैं तन्हाई की राहों के,\n",
        "आँखों के आँसू रब से दुआ बन गए,\n"
    )
}

object StudioWallpapers {
    val minimalPapers: List<StudioWallpaper.SolidOrGradient> = listOf(
        StudioWallpaper.SolidOrGradient(
            id = "paper_ivory",
            labelHi = "सफ़ेद काग़ज़",
            keywords = "white ivory minimal clean paper सादा सफ़ेद कागज",
            startColor = Color(0xFFFAF7F2),
            endColor = Color(0xFFF0EAE0),
            defaultTextColor = Color(0xFF1D1A18),
            defaultAccentColor = Color(0xFF8C5A1B)
        ),
        StudioWallpaper.SolidOrGradient(
            id = "paper_blush",
            labelHi = "हल्का गुलाबी",
            keywords = "pink rose soft pastel love प्यारा गुलाबी",
            startColor = Color(0xFFFDF0F3),
            endColor = Color(0xFFF6DCE3),
            defaultTextColor = Color(0xFF2D1620),
            defaultAccentColor = Color(0xFF9B2C4D)
        ),
        StudioWallpaper.SolidOrGradient(
            id = "paper_parchment",
            labelHi = "धूप क्रीम",
            keywords = "cream beige vintage warm क्रीम धूप",
            startColor = Color(0xFFFCF6EA),
            endColor = Color(0xFFEFE1C6),
            defaultTextColor = Color(0xFF241914),
            defaultAccentColor = Color(0xFF8C5A1B)
        ),
        StudioWallpaper.SolidOrGradient(
            id = "paper_sky",
            labelHi = "सुबह का आसमान",
            keywords = "blue sky pastel calm morning नीला आसमान",
            startColor = Color(0xFFEEF5FA),
            endColor = Color(0xFFDCEAF4),
            defaultTextColor = Color(0xFF162636),
            defaultAccentColor = Color(0xFF2B5B84)
        ),
        StudioWallpaper.SolidOrGradient(
            id = "paper_charcoal",
            labelHi = "गहरा काला",
            keywords = "black dark minimal night काला अंधेरा",
            startColor = Color(0xFF181820),
            endColor = Color(0xFF0D0D12),
            defaultTextColor = Color(0xFFFAF8F5),
            defaultAccentColor = Color(0xFFE6BE78)
        ),
        StudioWallpaper.SolidOrGradient(
            id = "paper_midnight",
            labelHi = "शब-ए-ग़ज़ल",
            keywords = "purple midnight dark night रात गज़ल",
            startColor = Color(0xFF1E132B),
            endColor = Color(0xFF0E0816),
            defaultTextColor = Color(0xFFFDF6EA),
            defaultAccentColor = Color(0xFFE6BE78)
        ),
        StudioWallpaper.SolidOrGradient(
            id = "paper_velvet",
            labelHi = "शाही मख़मल",
            keywords = "red maroon velvet royal प्यार मोहब्बत",
            startColor = Color(0xFF6B2031),
            endColor = Color(0xFF2B0A13),
            defaultTextColor = Color(0xFFFFF5EE),
            defaultAccentColor = Color(0xFFF0C782)
        ),
        StudioWallpaper.SolidOrGradient(
            id = "paper_emerald",
            labelHi = "चाँदनी रात",
            keywords = "green teal emerald night सुकून चाँद",
            startColor = Color(0xFF143536),
            endColor = Color(0xFF09191B),
            defaultTextColor = Color(0xFFF1FAF8),
            defaultAccentColor = Color(0xFF9BE0D8)
        )
    )

    val artBackdrops: List<StudioWallpaper.DrawableArt> = listOf(
        StudioWallpaper.DrawableArt(
            id = "art_romantic",
            labelHi = "मोहब्बत",
            keywords = "love romantic rose flowers प्यार मोहब्बत इश्क",
            resId = R.drawable.img_bg_romantic
        ),
        StudioWallpaper.DrawableArt(
            id = "art_candle",
            labelHi = "शमा और झील",
            keywords = "candle lake night light शमा रात तन्हाई",
            resId = R.drawable.img_bg_candle_lake
        ),
        StudioWallpaper.DrawableArt(
            id = "art_mehfil",
            labelHi = "महफ़िल",
            keywords = "mehfil evening mushaira शायरी महफिल",
            resId = R.drawable.img_hero_mehfil
        ),
        StudioWallpaper.DrawableArt(
            id = "art_mushaira",
            labelHi = "मुशायरा",
            keywords = "mushaira stage poetry मुशायरा",
            resId = R.drawable.img_card_mushaira
        ),
        StudioWallpaper.DrawableArt(
            id = "art_aansu",
            labelHi = "बारिश और आँसू",
            keywords = "rain tears sad pain दर्द आंसू बारिश",
            resId = R.drawable.img_bg_aansu
        ),
        StudioWallpaper.DrawableArt(
            id = "art_bewafai",
            labelHi = "बेवफ़ाई",
            keywords = "bewafai heartbreak sad जुदाई बेवफाई दर्द",
            resId = R.drawable.img_bg_bewafai
        ),
        StudioWallpaper.DrawableArt(
            id = "art_musafir",
            labelHi = "मुसाफ़िर",
            keywords = "journey road traveler सफर मुसाफिर राह",
            resId = R.drawable.img_bg_musafir
        ),
        StudioWallpaper.DrawableArt(
            id = "art_khuda",
            labelHi = "ख़ुदा और इबादत",
            keywords = "khuda rab dua prayer खुदा रब इबादत",
            resId = R.drawable.img_bg_khuda
        ),
        StudioWallpaper.DrawableArt(
            id = "art_zindagi",
            labelHi = "ज़िंदगी",
            keywords = "zindagi life sunrise hope जिंदगी सवेरा",
            resId = R.drawable.img_bg_zindagi
        ),
        StudioWallpaper.DrawableArt(
            id = "art_sharabi",
            labelHi = "मयख़ाना",
            keywords = "sharabi maikhana saqi मयखाना साकी जाम",
            resId = R.drawable.img_bg_sharabi
        ),
        StudioWallpaper.DrawableArt(
            id = "art_secret_love",
            labelHi = "ख़ामोश इश्क़",
            keywords = "secret love shadow adultery इश्क राज़",
            resId = R.drawable.img_bg_secret_love
        ),
        StudioWallpaper.DrawableArt(
            id = "art_aaina",
            labelHi = "आईना",
            keywords = "mirror reflection truth आईना सच",
            resId = R.drawable.img_bg_aaina
        ),
        StudioWallpaper.DrawableArt(
            id = "art_waqt",
            labelHi = "वक़्त",
            keywords = "time clock waqt वक्त समय",
            resId = R.drawable.img_bg_waqt
        ),
        StudioWallpaper.DrawableArt(
            id = "art_dosti",
            labelHi = "दोस्ती",
            keywords = "friendship dosti yaari दोस्ती यारी",
            resId = R.drawable.img_bg_dosti
        ),
        StudioWallpaper.DrawableArt(
            id = "art_motivation",
            labelHi = "हौसला",
            keywords = "motivation mountain success हौसला कामयाबी",
            resId = R.drawable.img_bg_motivation
        ),
        StudioWallpaper.DrawableArt(
            id = "art_attitude",
            labelHi = "अंदाज़",
            keywords = "attitude fire bold अंदाज़ जुनून",
            resId = R.drawable.img_bg_attitude
        ),
        StudioWallpaper.DrawableArt(
            id = "art_tareef",
            labelHi = "तारीफ़",
            keywords = "praise beauty tareef तारीफ खूबसूरती",
            resId = R.drawable.img_bg_tareef
        ),
        StudioWallpaper.DrawableArt(
            id = "art_maut",
            labelHi = "सन्नाटा",
            keywords = "silence night dark janaza जनाजा सन्नाटा",
            resId = R.drawable.img_bg_maut
        ),
        StudioWallpaper.DrawableArt(
            id = "art_deshprem",
            labelHi = "देशप्रेम",
            keywords = "india pride tiranga देशप्रेम वतन",
            resId = R.drawable.img_bg_deshprem
        ),
        StudioWallpaper.DrawableArt(
            id = "art_festive",
            labelHi = "त्योहार",
            keywords = "festival celebration lights दीवाली ईद होली",
            resId = R.drawable.img_bg_festive
        ),
        StudioWallpaper.DrawableArt(
            id = "art_genz",
            labelHi = "मॉडर्न वाइब",
            keywords = "modern neon urban genz वाइब",
            resId = R.drawable.img_bg_genz
        ),
        StudioWallpaper.DrawableArt(
            id = "art_comedy",
            labelHi = "मज़ेदार",
            keywords = "fun smile comedy हँसी मज़ाक",
            resId = R.drawable.img_bg_comedy
        ),
        StudioWallpaper.DrawableArt(
            id = "art_prakriti_rain",
            labelHi = "बारिश और प्रकृति",
            keywords = "prakriti rain lotus nature बारिश प्रकृति झील",
            resId = R.drawable.img_bg_prakriti_rain
        ),
        StudioWallpaper.DrawableArt(
            id = "art_mulla_joke",
            labelHi = "मुल्ला नसरुद्दीन",
            keywords = "mulla nasruddin joke funny मुल्ला चुटकुला",
            resId = R.drawable.img_joke_mulla_nasruddin
        ),
        StudioWallpaper.DrawableArt(
            id = "art_santa_banta_joke",
            labelHi = "संता-बंता",
            keywords = "santa banta chai joke funny संता बंता जोक",
            resId = R.drawable.img_joke_santa_banta
        ),
        StudioWallpaper.DrawableArt(
            id = "art_motu_patlu_joke",
            labelHi = "मोटू-पतलू",
            keywords = "motu patlu samosa joke funny मोटू पतलू",
            resId = R.drawable.img_joke_motu_patlu
        ),
        StudioWallpaper.DrawableArt(
            id = "art_jija_sali_joke",
            labelHi = "जीजा-साली",
            keywords = "jija sali wedding joke funny जीजा साली शादी",
            resId = R.drawable.img_joke_jija_sali
        ),
        StudioWallpaper.DrawableArt(
            id = "art_cute",
            labelHi = "मीठा प्यार",
            keywords = "cute sweet love मीठा प्यार",
            resId = R.drawable.img_bg_nibba_nibbi
        )
    )

    val all: List<StudioWallpaper> = minimalPapers + artBackdrops

    val defaultWallpaper: StudioWallpaper = artBackdrops.first()
}
