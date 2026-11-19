package com.example.data.model

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.SentimentSatisfiedAlt
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

enum class ShayariCategory(
    val id: String,
    val titleHi: String,
    val labelEn: String,
    val emoji: String,
    val taglineHi: String,
    val icon: ImageVector,
    val lightAccent: Color,
    val darkAccent: Color,
    @param:DrawableRes val backgroundResId: Int
) {
    MOHABBAT(
        id = "mohabbat",
        titleHi = "मोहब्बत",
        labelEn = "Mohabbat",
        emoji = "❤️",
        taglineHi = "मोहब्बत और दिल की धड़कनों के अल्फ़ाज़",
        icon = Icons.Rounded.Favorite,
        lightAccent = Color(0xFF9E2A43),
        darkAccent = Color(0xFFF09CA8),
        backgroundResId = R.drawable.img_bg_candle_lake
    ),
    ROMANTIC(
        id = "romantic",
        titleHi = "रोमांटिक",
        labelEn = "Romantic",
        emoji = "🌹",
        taglineHi = "मीठी नज़दीकियाँ, मुस्कान और रोमांस",
        icon = Icons.Rounded.LocalFlorist,
        lightAccent = Color(0xFFB52B5C),
        darkAccent = Color(0xFFFFA8C2),
        backgroundResId = R.drawable.img_bg_romantic
    ),
    BEWAFAI(
        id = "bewafai",
        titleHi = "बेवफ़ाई",
        labelEn = "Bewafai",
        emoji = "🥀",
        taglineHi = "टूटे वादे, धोखा और बदलते चेहरों का दर्द",
        icon = Icons.Rounded.HeartBroken,
        lightAccent = Color(0xFF8E2438),
        darkAccent = Color(0xFFF28B9E),
        backgroundResId = R.drawable.img_bg_bewafai
    ),
    SHARABI(
        id = "sharabi",
        titleHi = "शराबी",
        labelEn = "Sharabi",
        emoji = "🍷",
        taglineHi = "जाम, मैख़ाना और ग़म भुलाने के किस्से",
        icon = Icons.Rounded.LocalBar,
        lightAccent = Color(0xFFA65A17),
        darkAccent = Color(0xFFF7BC7C),
        backgroundResId = R.drawable.img_bg_sharabi
    ),
    KHUDA_RAB(
        id = "khuda_rab",
        titleHi = "खुदा / रब",
        labelEn = "Khuda",
        emoji = "🤲",
        taglineHi = "दुआ, भरोसा, सजदे और रब की मेहरबानी",
        icon = Icons.Rounded.WbTwilight,
        lightAccent = Color(0xFF9C721B),
        darkAccent = Color(0xFFF2CF7E),
        backgroundResId = R.drawable.img_bg_khuda
    ),
    MAUT_JANAZA(
        id = "maut_janaza",
        titleHi = "मौत / जनाज़ा",
        labelEn = "Maut",
        emoji = "🕊️",
        taglineHi = "आख़िरी सफ़र, कफ़न और ज़िंदगी की सच्चाई",
        icon = Icons.Rounded.DarkMode,
        lightAccent = Color(0xFF475569),
        darkAccent = Color(0xFFAEBED4),
        backgroundResId = R.drawable.img_bg_maut
    ),
    SECRET_LOVE(
        id = "secret_love",
        titleHi = "हॉट व बोल्ड",
        labelEn = "Bold",
        emoji = "🔞",
        taglineHi = "फ्लर्टी, नटखट और हल्की डबल-मीनिंग शायरी",
        icon = Icons.Rounded.VisibilityOff,
        lightAccent = Color(0xFF7D2958),
        darkAccent = Color(0xFFE69BC4),
        backgroundResId = R.drawable.img_bg_secret_love
    ),
    ADULTERY(
        id = "adultery",
        titleHi = "Adultery शायरी",
        labelEn = "Adultery",
        emoji = "❤️‍🔥",
        taglineHi = "राज़-ए-इश्क़, अधूरी कसमें और बेक़ाबू चाहत",
        icon = Icons.Rounded.Whatshot,
        lightAccent = Color(0xFF9F1239),
        darkAccent = Color(0xFFFB7185),
        backgroundResId = R.drawable.img_bg_secret_love
    ),
    GEN_Z(
        id = "gen_z",
        titleHi = "Gen-Z शायरी",
        labelEn = "Gen-Z",
        emoji = "🎧",
        taglineHi = "वाइब, रात के ख़याल, सीन-ज़ोन और मॉडर्न प्यार",
        icon = Icons.Rounded.Headphones,
        lightAccent = Color(0xFF53389E),
        darkAccent = Color(0xFFBBA6FF),
        backgroundResId = R.drawable.img_bg_genz
    ),
    NIBBA_NIBBI(
        id = "nibba_nibbi",
        titleHi = "निब्बा-निब्बी",
        labelEn = "Nibba-Nibbi",
        emoji = "🥰",
        taglineHi = "बाबू-सोना वाली क्यूट और तोतली आवाज़ में शायरी",
        icon = Icons.Rounded.EmojiEmotions,
        lightAccent = Color(0xFFC23E78),
        darkAccent = Color(0xFFFFAED1),
        backgroundResId = R.drawable.img_bg_nibba_nibbi
    ),
    ATTITUDE(
        id = "attitude",
        titleHi = "Attitude शायरी",
        labelEn = "Attitude",
        emoji = "😎",
        taglineHi = "रॉयल अंदाज़, तेवर और स्वाभिमान की बातें",
        icon = Icons.Rounded.Whatshot,
        lightAccent = Color(0xFFB91C1C),
        darkAccent = Color(0xFFFCA5A5),
        backgroundResId = R.drawable.img_bg_attitude
    ),
    TAREEF(
        id = "tareef",
        titleHi = "तारीफ़ शायरी",
        labelEn = "Tareef",
        emoji = "🌟",
        taglineHi = "ख़ूबसूरती, अदा और दिल से निकली तारीफ़",
        icon = Icons.Rounded.Star,
        lightAccent = Color(0xFFB45309),
        darkAccent = Color(0xFFFCD34D),
        backgroundResId = R.drawable.img_bg_tareef
    ),
    AAINA(
        id = "aaina",
        titleHi = "आईने पर शायरी",
        labelEn = "Aaina",
        emoji = "🪞",
        taglineHi = "आईना, अक्स और ख़ुद से मुलाक़ात के अल्फ़ाज़",
        icon = Icons.Rounded.AutoAwesome,
        lightAccent = Color(0xFF0E7490),
        darkAccent = Color(0xFF67E8F9),
        backgroundResId = R.drawable.img_bg_aaina
    ),
    AANSU(
        id = "aansu",
        titleHi = "आंसू पर शायरी",
        labelEn = "Aansu",
        emoji = "💧",
        taglineHi = "आँखों की नमी और अनकहे जज़्बात",
        icon = Icons.Rounded.WaterDrop,
        lightAccent = Color(0xFF2563EB),
        darkAccent = Color(0xFF93C5FD),
        backgroundResId = R.drawable.img_bg_aansu
    ),
    MUSAFIR(
        id = "musafir",
        titleHi = "मुसाफ़िर शायरी",
        labelEn = "Musafir",
        emoji = "🧳",
        taglineHi = "सफ़र, रास्ते और मंज़िल की तलाश",
        icon = Icons.Rounded.Explore,
        lightAccent = Color(0xFF0F766E),
        darkAccent = Color(0xFF5EEAD4),
        backgroundResId = R.drawable.img_bg_musafir
    ),
    NEW_YEAR(
        id = "new_year",
        titleHi = "New Year शायरी",
        labelEn = "New Year",
        emoji = "🎉",
        taglineHi = "नए साल की शुभकामनाएँ और नई उम्मीदें",
        icon = Icons.Rounded.Celebration,
        lightAccent = Color(0xFF7C3AED),
        darkAccent = Color(0xFFC4B5FD),
        backgroundResId = R.drawable.img_bg_festive
    ),
    CHRISTMAS(
        id = "christmas",
        titleHi = "Merry Christmas",
        labelEn = "Christmas",
        emoji = "🎄",
        taglineHi = "क्रिसमस की खुशियाँ, प्यार और रोशनी",
        icon = Icons.Rounded.Park,
        lightAccent = Color(0xFF15803D),
        darkAccent = Color(0xFF86EFAC),
        backgroundResId = R.drawable.img_bg_festive
    ),
    JIJA_SALI(
        id = "jija_sali",
        titleHi = "जीजा-साली",
        labelEn = "Jija-Sali",
        emoji = "😜",
        taglineHi = "मीठी नोकझोंक, छेड़छाड़ और हँसी-ठिठोली",
        icon = Icons.Rounded.EmojiEmotions,
        lightAccent = Color(0xFFDB2777),
        darkAccent = Color(0xFFF9A8D4),
        backgroundResId = R.drawable.img_joke_jija_sali
    ),
    MULLA_NASRUDDIN(
        id = "mulla_nasruddin",
        titleHi = "मुल्ला नसरुद्दीन",
        labelEn = "Mulla Nasruddin",
        emoji = "🤣",
        taglineHi = "हाज़िरजवाबी, चुटकुले और तेज़ अक़्ल की बातें",
        icon = Icons.Rounded.SentimentVerySatisfied,
        lightAccent = Color(0xFFD97706),
        darkAccent = Color(0xFFFDE68A),
        backgroundResId = R.drawable.img_joke_mulla_nasruddin
    ),
    SANTA_BANTA(
        id = "santa_banta",
        titleHi = "संता-बंता",
        labelEn = "Santa-Banta",
        emoji = "😂",
        taglineHi = "मज़ेदार चुटकुले और ठहाकों भरी शायरी",
        icon = Icons.Rounded.Mood,
        lightAccent = Color(0xFF0284C7),
        darkAccent = Color(0xFF7DD3FC),
        backgroundResId = R.drawable.img_joke_santa_banta
    ),
    MOTU_PATLU(
        id = "motu_patlu",
        titleHi = "मोटू-पतलू",
        labelEn = "Motu-Patlu",
        emoji = "🤪",
        taglineHi = "समोसा प्रेम, यारी और पेट-पकड़ हँसी",
        icon = Icons.Rounded.SentimentSatisfiedAlt,
        lightAccent = Color(0xFFEA580C),
        darkAccent = Color(0xFFFDBA74),
        backgroundResId = R.drawable.img_joke_motu_patlu
    ),
    DOSTI(
        id = "dosti",
        titleHi = "दोस्ती",
        labelEn = "Dosti",
        emoji = "🤝",
        taglineHi = "वफ़ा और यारी के अनमोल पल",
        icon = Icons.Rounded.Group,
        lightAccent = Color(0xFF216862),
        darkAccent = Color(0xFF8AD4CD),
        backgroundResId = R.drawable.img_bg_dosti
    ),
    PRAKRITI(
        id = "prakriti",
        titleHi = "प्रकृति",
        labelEn = "Prakriti",
        emoji = "🌿",
        taglineHi = "बारिश, चाँद, वादियाँ और कुदरत की ख़ूबसूरती",
        icon = Icons.Rounded.Park,
        lightAccent = Color(0xFF2E6F40),
        darkAccent = Color(0xFF8FD6A3),
        backgroundResId = R.drawable.img_bg_prakriti_rain
    ),
    HAUSLA(
        id = "hausla",
        titleHi = "हौसला",
        labelEn = "Hausla",
        emoji = "🔥",
        taglineHi = "हौसला, उम्मीद और आगे बढ़ने का जज़्बा",
        icon = Icons.Rounded.Bolt,
        lightAccent = Color(0xFFB3531E),
        darkAccent = Color(0xFFF2AA7E),
        backgroundResId = R.drawable.img_bg_motivation
    ),
    ZINDAGI(
        id = "zindagi",
        titleHi = "ज़िंदगी",
        labelEn = "Zindagi",
        emoji = "🌱",
        taglineHi = "सफ़र और तजुर्बे के आईने",
        icon = Icons.Rounded.AutoAwesome,
        lightAccent = Color(0xFF9C6B21),
        darkAccent = Color(0xFFE6BE78),
        backgroundResId = R.drawable.img_bg_zindagi
    ),
    DARD(
        id = "dard",
        titleHi = "दर्द",
        labelEn = "Dard",
        emoji = "💔",
        taglineHi = "ख़ामोश आँसुओं की जुबानी",
        icon = Icons.Rounded.WaterDrop,
        lightAccent = Color(0xFF4B558A),
        darkAccent = Color(0xFFA8B4F0),
        backgroundResId = R.drawable.img_bg_bewafai
    ),
    KHUSHI(
        id = "khushi",
        titleHi = "ख़ुशी",
        labelEn = "Khushi",
        emoji = "😊",
        taglineHi = "हँसी, सुकून और खुशमिज़ाज पल",
        icon = Icons.Rounded.SentimentSatisfiedAlt,
        lightAccent = Color(0xFFA66E14),
        darkAccent = Color(0xFFF5CA7B),
        backgroundResId = R.drawable.img_bg_motivation
    ),
    YAADEIN(
        id = "yaadein",
        titleHi = "यादें",
        labelEn = "Yaadein",
        emoji = "🌸",
        taglineHi = "गुज़रे लम्हों की मीठी ख़ुशबू",
        icon = Icons.Rounded.HistoryEdu,
        lightAccent = Color(0xFF7A4B3A),
        darkAccent = Color(0xFFE5B4A3),
        backgroundResId = R.drawable.img_card_mushaira
    ),
    TANHAI(
        id = "tanhai",
        titleHi = "तन्हाई",
        labelEn = "Tanhai",
        emoji = "🌧️",
        taglineHi = "रात और सन्नाटे से बातचीत",
        icon = Icons.Rounded.NightsStay,
        lightAccent = Color(0xFF5D407A),
        darkAccent = Color(0xFFCBB1EB),
        backgroundResId = R.drawable.img_bg_aansu
    ),
    WAQT(
        id = "waqt",
        titleHi = "वक़्त",
        labelEn = "Waqt",
        emoji = "⏳",
        taglineHi = "सब्र, ठहराव और समय की सीख",
        icon = Icons.Rounded.HourglassTop,
        lightAccent = Color(0xFF3E6478),
        darkAccent = Color(0xFF9CCCE3),
        backgroundResId = R.drawable.img_bg_waqt
    ),
    PARIVAR(
        id = "parivar",
        titleHi = "परिवार",
        labelEn = "Parivar",
        emoji = "🏡",
        taglineHi = "घर, दुआएं और अपनों की छाँव",
        icon = Icons.Rounded.Home,
        lightAccent = Color(0xFF8C4256),
        darkAccent = Color(0xFFEBA8B9),
        backgroundResId = R.drawable.img_hero_mehfil
    ),
    DESHPREM(
        id = "deshprem",
        titleHi = "देशभक्ति",
        labelEn = "Deshprem",
        emoji = "🇮🇳",
        taglineHi = "वतन की मिट्टी और गौरव के तराने",
        icon = Icons.Rounded.Flag,
        lightAccent = Color(0xFF1F5D78),
        darkAccent = Color(0xFF8AC8E3),
        backgroundResId = R.drawable.img_bg_deshprem
    );

    val isJokeCategory: Boolean
        get() = this == JIJA_SALI ||
            this == MULLA_NASRUDDIN ||
            this == SANTA_BANTA ||
            this == MOTU_PATLU

    private val backgroundPool: IntArray by lazy {
        when (this) {
            MOHABBAT -> intArrayOf(backgroundResId, R.drawable.img_bg_romantic, R.drawable.img_card_mushaira)
            ROMANTIC -> intArrayOf(backgroundResId, R.drawable.img_bg_candle_lake, R.drawable.img_bg_tareef)
            BEWAFAI -> intArrayOf(backgroundResId, R.drawable.img_bg_aansu, R.drawable.img_bg_candle_lake)
            SHARABI -> intArrayOf(backgroundResId, R.drawable.img_card_mushaira, R.drawable.img_bg_bewafai)
            KHUDA_RAB -> intArrayOf(backgroundResId, R.drawable.img_bg_candle_lake, R.drawable.img_bg_waqt)
            MAUT_JANAZA -> intArrayOf(backgroundResId, R.drawable.img_bg_waqt, R.drawable.img_bg_aansu)
            SECRET_LOVE -> intArrayOf(backgroundResId, R.drawable.img_bg_romantic, R.drawable.img_bg_candle_lake)
            ADULTERY -> intArrayOf(backgroundResId, R.drawable.img_bg_bewafai, R.drawable.img_bg_romantic)
            GEN_Z -> intArrayOf(backgroundResId, R.drawable.img_bg_nibba_nibbi, R.drawable.img_bg_attitude)
            NIBBA_NIBBI -> intArrayOf(backgroundResId, R.drawable.img_bg_romantic, R.drawable.img_bg_genz)
            ATTITUDE -> intArrayOf(backgroundResId, R.drawable.img_bg_motivation, R.drawable.img_bg_musafir)
            TAREEF -> intArrayOf(backgroundResId, R.drawable.img_bg_romantic, R.drawable.img_bg_aaina)
            AAINA -> intArrayOf(backgroundResId, R.drawable.img_bg_tareef, R.drawable.img_bg_waqt)
            AANSU -> intArrayOf(backgroundResId, R.drawable.img_bg_prakriti_rain, R.drawable.img_bg_bewafai)
            MUSAFIR -> intArrayOf(backgroundResId, R.drawable.img_bg_zindagi, R.drawable.img_bg_prakriti_rain)
            NEW_YEAR -> intArrayOf(backgroundResId, R.drawable.img_bg_motivation, R.drawable.img_bg_candle_lake)
            CHRISTMAS -> intArrayOf(backgroundResId, R.drawable.img_hero_mehfil, R.drawable.img_bg_candle_lake)
            JIJA_SALI -> intArrayOf(backgroundResId)
            MULLA_NASRUDDIN -> intArrayOf(backgroundResId)
            SANTA_BANTA -> intArrayOf(backgroundResId)
            MOTU_PATLU -> intArrayOf(backgroundResId)
            DOSTI -> intArrayOf(backgroundResId, R.drawable.img_bg_candle_lake, R.drawable.img_bg_musafir)
            PRAKRITI -> intArrayOf(backgroundResId, R.drawable.img_bg_zindagi, R.drawable.img_bg_musafir)
            HAUSLA -> intArrayOf(backgroundResId, R.drawable.img_bg_attitude, R.drawable.img_bg_musafir)
            ZINDAGI -> intArrayOf(backgroundResId, R.drawable.img_bg_prakriti_rain, R.drawable.img_bg_musafir)
            DARD -> intArrayOf(backgroundResId, R.drawable.img_bg_aansu, R.drawable.img_bg_maut)
            KHUSHI -> intArrayOf(backgroundResId, R.drawable.img_bg_prakriti_rain, R.drawable.img_bg_festive)
            YAADEIN -> intArrayOf(backgroundResId, R.drawable.img_bg_candle_lake, R.drawable.img_bg_aaina)
            TANHAI -> intArrayOf(backgroundResId, R.drawable.img_bg_prakriti_rain, R.drawable.img_bg_candle_lake)
            WAQT -> intArrayOf(backgroundResId, R.drawable.img_bg_zindagi, R.drawable.img_bg_khuda)
            PARIVAR -> intArrayOf(backgroundResId, R.drawable.img_bg_festive, R.drawable.img_bg_dosti)
            DESHPREM -> intArrayOf(backgroundResId, R.drawable.img_bg_motivation, R.drawable.img_bg_musafir)
        }
    }

    @DrawableRes
    fun backgroundFor(shayariId: Int): Int {
        val pool = backgroundPool
        if (pool.size == 1) return pool[0]
        val idx = ((shayariId % pool.size) + pool.size) % pool.size
        return pool[idx]
    }

    @DrawableRes
    fun backgroundFor(shayari: ShayariEntity): Int {
        if (isJokeCategory) return backgroundResId

        val cacheKey = ((ordinal.toLong() and 0x3FL) shl 56) xor
            ((shayari.id.toLong() and 0xFFFFFFL) shl 32) xor
            ((shayari.couplet.hashCode().toLong() * 31L + shayari.moodTag.hashCode().toLong()) and 0xFFFFFFFFL)
        ARTWORK_CACHE.get(cacheKey)?.let { return it }

        val mood = shayari.moodTag
        val couplet = shayari.couplet
        var bestDrawable = 0
        var bestScore = 0
        for (matcher in KEYWORD_MATCHERS) {
            var score = 0
            for (keyword in matcher.keywords) {
                if (mood.contains(keyword) || couplet.contains(keyword)) {
                    score++
                }
            }
            if (score > bestScore) {
                bestScore = score
                bestDrawable = matcher.drawableRes
            }
        }

        // Strongly prefer the category's native artwork when no specific multi-keyword match overrides it,
        // so visual variety stays consistent while remaining directly tied to the verse.
        val resolved = when {
            bestDrawable != 0 && bestScore >= 2 -> bestDrawable
            bestDrawable != 0 && shayari.id % 2 == 0 -> bestDrawable
            else -> backgroundFor(shayari.id)
        }
        ARTWORK_CACHE.put(cacheKey, resolved)
        return resolved
    }

    private class KeywordMatcher(
        val keywords: Array<String>,
        @param:DrawableRes val drawableRes: Int
    )

    companion object {
        val CORE_FEED_CATEGORIES: List<ShayariCategory> = listOf(
            MOHABBAT,
            DARD,
            ZINDAGI,
            DOSTI,
            HAUSLA
        )

        val FEED_ORDERED_CATEGORIES: List<ShayariCategory> =
            CORE_FEED_CATEGORIES + entries.filter { it !in CORE_FEED_CATEGORIES }

        private val BY_ID: Map<String, ShayariCategory> = entries.associateBy { it.id }

        private val ARTWORK_CACHE = android.util.LruCache<Long, Int>(1024)

        private val KEYWORD_MATCHERS: Array<KeywordMatcher> = arrayOf(
            KeywordMatcher(arrayOf("मुल्ला", "नसरुद्दीन"), R.drawable.img_joke_mulla_nasruddin),
            KeywordMatcher(arrayOf("संता", "बंता"), R.drawable.img_joke_santa_banta),
            KeywordMatcher(arrayOf("मोटू", "पतलू", "समोसा"), R.drawable.img_joke_motu_patlu),
            KeywordMatcher(arrayOf("जीजा", "साली"), R.drawable.img_joke_jija_sali),
            KeywordMatcher(arrayOf("साक़ी", "साकी", "मयख़ान", "मयखान", "जाम", "पैमाना", "शराब", "मदहोश"), R.drawable.img_bg_sharabi),
            KeywordMatcher(arrayOf("जनाज़", "जनाज", "कफ़न", "कफन", "कब्र", "श्मशान", "मौत", "क़ब्र"), R.drawable.img_bg_maut),
            KeywordMatcher(arrayOf("इबादत", "सजद", "दुआ", "ख़ुदा", "खुदा", "रब", "मस्जिद", "मंदिर"), R.drawable.img_bg_khuda),
            KeywordMatcher(arrayOf("बारिश", "सावन", "बूंद", "बादल", "झरना", "प्रकृति", "वादियाँ", "फूल"), R.drawable.img_bg_prakriti_rain),
            KeywordMatcher(arrayOf("आईना", "आइना", "अक्स", "शीश", "दरपन"), R.drawable.img_bg_aaina),
            KeywordMatcher(arrayOf("आँसू", "आंसू", "अश्क", "रोया", "रोने", "नमी"), R.drawable.img_bg_aansu),
            KeywordMatcher(arrayOf("मुसाफ़िर", "मुसाफिर", "सफ़र", "सफर", "राही", "मंज़िल", "रास्ते"), R.drawable.img_bg_musafir),
            KeywordMatcher(arrayOf("बेवफ़ा", "बेवफा", "धोखा", "जुदाई", "छल"), R.drawable.img_bg_bewafai),
            KeywordMatcher(arrayOf("अदालत", "गवाह", "तेवर", "औक़ात", "ताज", "जुनून", "रुतबा"), R.drawable.img_bg_attitude),
            KeywordMatcher(arrayOf("वतन", "तिरंग", "सरहद", "भारत", "हिंदुस्तान", "शहीद"), R.drawable.img_bg_deshprem),
            KeywordMatcher(arrayOf("यार", "दोस्त", "दोस्ती", "यारी"), R.drawable.img_bg_dosti),
            KeywordMatcher(arrayOf("वक़्त", "वक्त", "इंतज़ार", "इंतजार", "सब्र"), R.drawable.img_bg_waqt),
            KeywordMatcher(arrayOf("तारीफ़", "हुस्न", "आँखें", "काजल", "निगाह", "चाँद"), R.drawable.img_bg_tareef),
            KeywordMatcher(arrayOf("सनम", "इश्क़", "इश्क", "मोहब्बत", "प्यार", "चाहत", "दिलबर"), R.drawable.img_bg_romantic),
            KeywordMatcher(arrayOf("तन्हाई", "तनहा", "अकेला", "सन्नाटा", "रात"), R.drawable.img_bg_candle_lake),
            KeywordMatcher(arrayOf("ज़िंदगी", "जिंदगी", "हौसला", "उम्मीद", "सवेरा"), R.drawable.img_bg_zindagi)
        )

        fun fromId(id: String): ShayariCategory =
            BY_ID[id] ?: MOHABBAT
    }
}
