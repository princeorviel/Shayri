package com.example.data.model

import com.example.util.HinglishTransliterator

/**
 * Multi-tag parsing, formatting, category-aware tag suggestions, and universal
 * Hindi + English + Hinglish search helper.
 */
object ShayariTagMatcher {

    val POPULAR_POST_TAGS: List<String> = listOf(
        "दर्द",
        "बेवफ़ाई",
        "जख्मी दिल",
        "आंसू",
        "तन्हाई",
        "यादें",
        "मोहब्बत",
        "रोमांटिक",
        "इश्क़",
        "टूटा दिल",
        "ज़िंदगी",
        "दोस्ती",
        "हौसला",
        "वक़्त",
        "अंदाज़",
        "मुसाफ़िर",
        "ख़ुदा",
        "शराबी",
        "तारीफ़",
        "आईना",
        "ख़ामोशी",
        "इंतज़ार",
        "सुकून",
        "परिवार"
    )

    /**
     * Curated bilingual (Hindi + Hinglish + English) tags suggested for each category
     * when a poet writes/posts a shayari.
     * Note: The first 4-5 entries are used by [autoTagsForCategory] as instant starter tags.
     */
    private val CATEGORY_RELATED_POST_TAGS: Map<String, List<String>> = mapOf(
        ShayariCategory.MOHABBAT.id to listOf(
            "मोहब्बत", "romantic शायरी", "ishq shayri", "pyar", "love",
            "इश्क़", "प्यार", "चाहत", "love shayari", "दिल की बात"
        ),
        ShayariCategory.ROMANTIC.id to listOf(
            "रोमांटिक", "romantic शायरी", "love", "pyar", "ishq shayri",
            "इश्क़", "मोहब्बत", "couple shayari", "नज़दीकियाँ", "चाहत"
        ),
        ShayariCategory.BEWAFAI.id to listOf(
            "बेवफ़ाई", "bewafa shayari", "धोखा", "broken heart", "heartbreak",
            "टूटा दिल", "जुदाई", "sad shayari", "जख्मी दिल", "दर्द"
        ),
        ShayariCategory.SHARABI.id to listOf(
            "शराबी", "sharabi shayari", "मैख़ाना", "जाम", "साक़ी",
            "शराब", "nasha", "maikhana", "ग़म", "रात"
        ),
        ShayariCategory.KHUDA_RAB.id to listOf(
            "ख़ुदा", "रब", "दुआ", "khuda shayari", "ibadat",
            "भरोसा", "सजदा", "dua", "rab", "सुकून"
        ),
        ShayariCategory.MAUT_JANAZA.id to listOf(
            "मौत", "जनाज़ा", "कफ़न", "maut shayari", "alvida",
            "आख़िरी सफ़र", "दर्द", "ज़िंदगी", "तन्हाई"
        ),
        ShayariCategory.SECRET_LOVE.id to listOf(
            "बोल्ड शायरी", "hot shayari", "flirty", "bold", "secret love",
            "नटखट", "रोमांटिक", "इश्क़", "चाहत"
        ),
        ShayariCategory.ADULTERY.id to listOf(
            "राज़-ए-इश्क़", "adultery", "छुपा प्यार", "secret love", "अधूरी कसमें",
            "बेक़ाबू चाहत", "इश्क़", "मोहब्बत", "दर्द"
        ),
        ShayariCategory.GEN_Z.id to listOf(
            "Gen-Z", "vibe", "modern love", "reels shayari", "late night",
            "वाइब", "सीन-ज़ोन", "crush", "mood"
        ),
        ShayariCategory.NIBBA_NIBBI.id to listOf(
            "निब्बा-निब्बी", "babu shona", "cute love", "बाबू सोना", "funny love",
            "प्यार", "रोमांटिक", "मुस्कान"
        ),
        ShayariCategory.ATTITUDE.id to listOf(
            "Attitude", "attitude shayari", "रॉयल अंदाज़", "royal status", "तेवर",
            "swag", "दबंग", "स्वाभिमान", "अंदाज़", "killer attitude"
        ),
        ShayariCategory.TAREEF.id to listOf(
            "तारीफ़", "tareef shayari", "ख़ूबसूरती", "beauty", "हुस्न",
            "मुस्कान", "चाँद सा चेहरा", "रोमांटिक", "प्यार"
        ),
        ShayariCategory.AAINA.id to listOf(
            "आईना", "aaina shayari", "mirror", "अक्स", "चेहरा",
            "ख़ुद से", "ज़िंदगी", "सच्चाई"
        ),
        ShayariCategory.AANSU.id to listOf(
            "आंसू", "aansu shayari", "tears", "अश्क", "sad shayari",
            "रोना", "नमी", "दर्द", "ग़म"
        ),
        ShayariCategory.MUSAFIR.id to listOf(
            "मुसाफ़िर", "musafir shayari", "सफ़र", "journey", "रास्ते",
            "मंज़िल", "travel", "ज़िंदगी", "राही"
        ),
        ShayariCategory.NEW_YEAR.id to listOf(
            "New Year", "नया साल", "happy new year", "नववर्ष", "शुभकामनाएँ",
            "नई उम्मीद", "ख़ुशी", "दुआ"
        ),
        ShayariCategory.CHRISTMAS.id to listOf(
            "Christmas", "Merry Christmas", "क्रिसमस", "xmas", "festive",
            "ख़ुशी", "प्यार", "रोशनी"
        ),
        ShayariCategory.JIJA_SALI.id to listOf(
            "जीजा-साली", "jija sali", "funny shayari", "jokes", "comedy",
            "नोकझोंक", "हँसी", "मज़ाक"
        ),
        ShayariCategory.MULLA_NASRUDDIN.id to listOf(
            "मुल्ला नसरुद्दीन", "mulla nasruddin", "jokes", "funny", "comedy",
            "चुटकुले", "हाज़िरजवाबी", "हँसी"
        ),
        ShayariCategory.SANTA_BANTA.id to listOf(
            "संता-बंता", "santa banta", "jokes", "funny", "comedy",
            "चुटकुले", "ठहाके", "हँसी"
        ),
        ShayariCategory.MOTU_PATLU.id to listOf(
            "मोटू-पतलू", "motu patlu", "funny", "jokes", "comedy",
            "समोसा", "दोस्ती", "हँसी"
        ),
        ShayariCategory.DOSTI.id to listOf(
            "दोस्ती", "dosti shayari", "यारी", "friendship", "yaar",
            "जिगरी दोस्त", "best friend", "दोस्त", "वफ़ा"
        ),
        ShayariCategory.PRAKRITI.id to listOf(
            "प्रकृति", "nature shayari", "बारिश", "चाँदनी", "rain",
            "सावन", "मौसम", "कुदरत", "वादियाँ"
        ),
        ShayariCategory.HAUSLA.id to listOf(
            "हौसला", "motivational shayari", "motivation", "उम्मीद", "success",
            "कामयाबी", "संघर्ष", "जीत", "हिम्मत"
        ),
        ShayariCategory.ZINDAGI.id to listOf(
            "ज़िंदगी", "zindagi shayari", "life quotes", "life", "तजुर्बा",
            "सुकून", "सफ़र", "हक़ीक़त", "वक़्त"
        ),
        ShayariCategory.DARD.id to listOf(
            "दर्द", "dard shayari", "sad shayari", "दर्द भरी शायरी", "broken heart",
            "ग़म", "आंसू", "बेवफ़ाई", "जख्मी दिल", "टूटा दिल"
        ),
        ShayariCategory.KHUSHI.id to listOf(
            "ख़ुशी", "khushi shayari", "happy", "मुस्कान", "smile",
            "सुकून", "हँसी", "positive"
        ),
        ShayariCategory.YAADEIN.id to listOf(
            "यादें", "yaadein shayari", "memories", "miss you", "इंतज़ार",
            "याद", "गुज़रे लम्हे", "तन्हाई", "प्यार"
        ),
        ShayariCategory.TANHAI.id to listOf(
            "तन्हाई", "tanhai shayari", "alone", "अकेलापन", "ख़ामोशी",
            "lonely", "रात", "सन्नाटा", "यादें"
        ),
        ShayariCategory.WAQT.id to listOf(
            "वक़्त", "waqt shayari", "time", "सब्र", "समय",
            "हालात", "ज़िंदगी", "तजुर्बा"
        ),
        ShayariCategory.PARIVAR.id to listOf(
            "परिवार", "family shayari", "माँ", "पापा", "घर",
            "parents", "अपने", "दुआ", "रिश्ते"
        ),
        ShayariCategory.DESHPREM.id to listOf(
            "देशभक्ति", "deshbhakti shayari", "वतन", "भारत", "india",
            "तिरंगा", "patriotic", "आज़ादी", "फ़ौजी"
        )
    )

    private val TAG_SPLIT_REGEX = Regex("[,•|#/\n]+")

    private val KEYWORD_TO_CATEGORY_IDS: List<Pair<List<String>, List<String>>> = listOf(
        listOf(
            "जख्मी दिल", "ज़ख़्मी दिल", "टूटा दिल", "दिल टूटा", "दिल का दर्द",
            "zakhmi", "jakhmi", "broken", "heartbreak", "broken heart", "breakup", "break up"
        ) to listOf(ShayariCategory.DARD.id, ShayariCategory.BEWAFAI.id),
        listOf(
            "बेवफ़ाई", "बेवफाई", "बेवफ़ा", "बेवफा", "धोखा", "जुदाई", "छल", "गद्दार", "ब्रेकअप",
            "bewafai", "bewafa", "dhoka", "dhokha", "judai", "cheat", "cheating", "betray", "betrayal", "ex"
        ) to listOf(ShayariCategory.BEWAFAI.id, ShayariCategory.DARD.id),
        listOf(
            "दर्द", "ग़म", "गम", "दुख", "पीड़ा", "उदास", "उदासी", "जख्म", "ज़ख्म",
            "dard", "gam", "gham", "dukh", "sad", "sadness", "pain", "painful", "hurt", "emotional", "depressed", "udas", "udaas"
        ) to listOf(ShayariCategory.DARD.id, ShayariCategory.AANSU.id),
        listOf(
            "आंसू", "आँसू", "अश्क", "रोना", "नमी", "गीली आँखें",
            "aansu", "ansu", "aansoo", "tears", "tear", "cry", "crying", "ashk", "rona"
        ) to listOf(ShayariCategory.AANSU.id, ShayariCategory.DARD.id),
        listOf(
            "तन्हाई", "तन्हा", "तनहा", "अकेला", "अकेलापन", "ख़ामोशी", "खामोशी", "सन्नाटा", "वीरान",
            "tanhai", "tanha", "alone", "lonely", "loneliness", "solitude", "silence", "khamoshi", "akela", "akelapan"
        ) to listOf(ShayariCategory.TANHAI.id, ShayariCategory.YAADEIN.id),
        listOf(
            "यादें", "याद", "इंतज़ार", "इंतजार", "गुज़रे लम्हे",
            "yaadein", "yadein", "yaad", "memories", "memory", "miss", "missing", "intezar", "intizar", "wait", "waiting"
        ) to listOf(ShayariCategory.YAADEIN.id, ShayariCategory.TANHAI.id),
        listOf(
            "मोहब्बत", "इश्क़", "इश्क", "प्यार", "चाहत", "दिल की बात", "आशिक", "आशिकी", "महबूब", "सनम", "लव",
            "mohabbat", "muhabbat", "ishq", "ishk", "pyar", "pyaar", "love", "lover", "chahat", "aashiq", "aashiqui", "couple", "bf", "gf", "crush", "prem"
        ) to listOf(ShayariCategory.MOHABBAT.id, ShayariCategory.ROMANTIC.id),
        listOf(
            "रोमांटिक", "रोमांस", "नज़दीकियाँ", "नजदीकियां",
            "romantic", "romance", "kiss", "hug", "valentine", "sweet love", "couple"
        ) to listOf(ShayariCategory.ROMANTIC.id, ShayariCategory.MOHABBAT.id),
        listOf(
            "शराबी", "शराब", "मैख़ाना", "मयख़ाना", "जाम", "साकी", "साक़ी", "नशा", "दारू", "पैग",
            "sharabi", "sharab", "sharaab", "maikhana", "maykhana", "jaam", "saqi", "saki", "nasha", "wine", "drink", "alcohol", "daru", "peg", "bar"
        ) to listOf(ShayariCategory.SHARABI.id, ShayariCategory.DARD.id),
        listOf(
            "ख़ुदा", "खुदा", "रब", "अल्लाह", "भगवान", "ईश्वर", "दुआ", "इबादत", "सजदा",
            "khuda", "rab", "rabb", "god", "allah", "bhagwan", "ishwar", "dua", "prayer", "ibadat", "sajda", "faith", "spiritual"
        ) to listOf(ShayariCategory.KHUDA_RAB.id),
        listOf(
            "मौत", "जनाज़ा", "जनाजा", "कफ़न", "कफन", "क़ब्र", "कब्र", "अलविदा",
            "maut", "death", "janaza", "kafan", "qabr", "kabr", "grave", "funeral", "alvida"
        ) to listOf(ShayariCategory.MAUT_JANAZA.id, ShayariCategory.DARD.id),
        listOf(
            "हॉट", "बोल्ड", "फ्लर्ट", "फ्लर्टी", "नटखट", "डबल मीनिंग", "शरारती",
            "hot", "bold", "flirt", "flirty", "naughty", "adult", "spicy", "sensual", "18+", "double meaning"
        ) to listOf(ShayariCategory.SECRET_LOVE.id),
        listOf(
            "अडल्टरी", "राज़-ए-इश्क़", "छुपा प्यार", "अफ़ेयर", "अधूरी कसमें",
            "adultery", "affair", "forbidden", "secret love", "hidden love", "raaz"
        ) to listOf(ShayariCategory.ADULTERY.id, ShayariCategory.SECRET_LOVE.id),
        listOf(
            "जेन ज़ी", "वाइब", "सीन ज़ोन", "घोस्टिंग", "मॉडर्न प्यार",
            "genz", "gen-z", "gen z", "vibe", "vibes", "seenzone", "ghosting", "reels", "aesthetic", "situationship", "insta"
        ) to listOf(ShayariCategory.GEN_Z.id),
        listOf(
            "निब्बा", "निब्बी", "बाबू", "सोना", "मेला बाबू", "थाना थाया",
            "nibba", "nibbi", "babu", "shona", "sona", "mela babu", "cute"
        ) to listOf(ShayariCategory.NIBBA_NIBBI.id),
        listOf(
            "दोस्ती", "दोस्त", "यारी", "यार", "जिगरी", "मित्र", "सखा",
            "dosti", "dost", "yaari", "yaar", "jigri", "friend", "friends", "friendship", "bestie", "bff", "buddy", "bro"
        ) to listOf(ShayariCategory.DOSTI.id),
        listOf(
            "हौसला", "मोटिवेशन", "मोटिवेशनल", "उम्मीद", "जीत", "कामयाबी", "सफलता", "संघर्ष", "हिम्मत", "मेहनत", "लक्ष्य",
            "hausla", "honsla", "motivation", "motivational", "inspire", "inspiration", "success", "win", "winner", "courage", "hope", "struggle", "hardwork", "umeed", "himmat", "jeet"
        ) to listOf(ShayariCategory.HAUSLA.id, ShayariCategory.ZINDAGI.id),
        listOf(
            "ज़िंदगी", "जिंदगी", "जीवन", "सुकून", "तजुर्बा", "हक़ीक़त", "सच्चाई",
            "zindagi", "zindgi", "jindagi", "life", "reality", "experience", "lesson", "peace", "sukoon"
        ) to listOf(ShayariCategory.ZINDAGI.id),
        listOf(
            "मुसाफ़िर", "मुसाफिर", "सफ़र", "सफर", "मंज़िल", "मंजिल", "रास्ते", "राही", "बंजारा", "यात्रा",
            "musafir", "safar", "travel", "traveller", "traveler", "journey", "road", "raahi", "rahi", "manzil", "banjara", "trip"
        ) to listOf(ShayariCategory.MUSAFIR.id, ShayariCategory.ZINDAGI.id),
        listOf(
            "वक़्त", "वक्त", "समय", "सब्र", "दौर", "हालात",
            "waqt", "wakt", "time", "patience", "sabr", "samay"
        ) to listOf(ShayariCategory.WAQT.id, ShayariCategory.ZINDAGI.id),
        listOf(
            "अंदाज़", "अंदाज", "तेवर", "रॉयल", "स्वाभिमान", "दबंग", "बादशाह", "नवाब", "औकात", "एटीट्यूड",
            "attitude", "royal", "swag", "killer", "boss", "king", "queen", "ego", "pride", "savage", "dabang", "badshah", "gangster"
        ) to listOf(ShayariCategory.ATTITUDE.id),
        listOf(
            "तारीफ़", "तारीफ", "खूबसूरती", "ख़ूबसूरती", "हुस्न", "अदा", "सुंदर",
            "tareef", "tarif", "beauty", "beautiful", "pretty", "gorgeous", "praise", "compliment", "husn", "ada"
        ) to listOf(ShayariCategory.TAREEF.id, ShayariCategory.ROMANTIC.id),
        listOf(
            "आईना", "आइना", "शीशा", "दर्पण", "अक्स",
            "aaina", "aina", "mirror", "reflection", "shisha", "sheesha", "darpan"
        ) to listOf(ShayariCategory.AAINA.id),
        listOf(
            "प्रकृति", "कुदरत", "बारिश", "सावन", "चाँदनी", "चांदनी", "मौसम", "बादल", "पहाड़", "वादियाँ",
            "prakriti", "kudrat", "nature", "rain", "barish", "baarish", "monsoon", "sawan", "weather", "mountain"
        ) to listOf(ShayariCategory.PRAKRITI.id),
        listOf(
            "खुशी", "ख़ुशी", "मुस्कान", "हँसी", "हंसी", "आनंद", "खुशमिज़ाज",
            "khushi", "happy", "happiness", "smile", "muskan", "joy", "cheerful", "positive"
        ) to listOf(ShayariCategory.KHUSHI.id),
        listOf(
            "परिवार", "माँ", "मां", "पापा", "पिता", "भाई", "बहन", "घर", "माता", "अपने", "रिश्ते",
            "parivar", "pariwar", "family", "maa", "mom", "mother", "papa", "dad", "father", "brother", "bhai", "sister", "behen", "home", "ghar", "parents"
        ) to listOf(ShayariCategory.PARIVAR.id),
        listOf(
            "देशप्रेम", "देशभक्ति", "वतन", "भारत", "हिंदुस्तान", "तिरंगा", "फौजी", "सैनिक", "आज़ादी", "शहीद",
            "deshprem", "deshbhakti", "patriotic", "patriotism", "india", "bharat", "hindustan", "watan", "vatan", "army", "soldier", "fauji", "tiranga", "freedom"
        ) to listOf(ShayariCategory.DESHPREM.id),
        listOf(
            "नया साल", "नए साल", "नववर्ष", "हैप्पी न्यू ईयर",
            "new year", "newyear", "happy new year", "naya saal", "navvarsh", "2025", "2026", "2027"
        ) to listOf(ShayariCategory.NEW_YEAR.id),
        listOf(
            "क्रिसमस", "मैरी क्रिसमस",
            "christmas", "merry christmas", "xmas"
        ) to listOf(ShayariCategory.CHRISTMAS.id),
        listOf(
            "जीजा", "साली", "नोकझोंक", "छेड़छाड़",
            "jija", "sali", "jija sali"
        ) to listOf(ShayariCategory.JIJA_SALI.id),
        listOf(
            "मुल्ला", "नसरुद्दीन", "हाज़िरजवाबी",
            "mulla", "nasruddin", "nasrudin"
        ) to listOf(ShayariCategory.MULLA_NASRUDDIN.id),
        listOf(
            "संता", "बंता", "सरदार",
            "santa", "banta", "santa banta", "sardar"
        ) to listOf(ShayariCategory.SANTA_BANTA.id),
        listOf(
            "मोटू", "पतलू", "समोसा",
            "motu", "patlu", "motu patlu", "samosa"
        ) to listOf(ShayariCategory.MOTU_PATLU.id),
        listOf(
            "चुटकुले", "मज़ाक", "मजाक", "ठहाके", "कॉमेडी", "हँसी",
            "funny", "joke", "jokes", "comedy", "laugh", "humour", "humor", "chutkule", "mazaak"
        ) to listOf(
            ShayariCategory.JIJA_SALI.id,
            ShayariCategory.MULLA_NASRUDDIN.id,
            ShayariCategory.SANTA_BANTA.id,
            ShayariCategory.MOTU_PATLU.id
        )
    )

    /**
     * English & Hinglish words/poet names expanded directly to normalized Devanagari stems
     * so typing English words (like "moon", "heart", "rain", "mother", "galib") finds every
     * Hindi shayari containing those ideas or poet names.
     */
    private val ENGLISH_CONCEPT_TO_NORMALIZED_HINDI: Map<String, List<String>> = mapOf(
        // Emotions & Love
        "love" to listOf("प्यार", "मोहब्बत", "इश्क", "चाहत"),
        "lover" to listOf("आशिक", "महबूब", "सनम", "दिलबर"),
        "romantic" to listOf("रोमांटिक", "इश्क", "प्यार", "मोहब्बत", "चाहत"),
        "romance" to listOf("रोमांटिक", "प्यार", "इश्क", "नजदीक"),
        "heart" to listOf("दिल", "धडकन", "सीने"),
        "heartbreak" to listOf("टूटा दिल", "दिल टूटा", "बेवफा", "जख्मी", "दर्द"),
        "broken" to listOf("टूटा", "टूट", "जख्मी", "बिखर"),
        "breakup" to listOf("जुदाई", "बेवफा", "ब्रेकअप", "धोखा", "अलग"),
        "cheat" to listOf("धोखा", "बेवफा", "छल"),
        "betrayal" to listOf("धोखा", "बेवफा", "छल"),
        "sad" to listOf("दर्द", "गम", "उदास", "दुख", "आंसू"),
        "pain" to listOf("दर्द", "पीडा", "जख्म", "गम"),
        "cry" to listOf("रोना", "रोये", "आंसू", "अश्क"),
        "crying" to listOf("रोना", "आंसू", "अश्क"),
        "tears" to listOf("आंसू", "अश्क", "नमी", "आंख"),
        "alone" to listOf("तन्हा", "अकेला", "अकेले", "सन्नाटा", "खामोश"),
        "lonely" to listOf("तन्हाई", "अकेलापन", "अकेला", "तन्हा"),
        "silence" to listOf("खामोशी", "खामोश", "चुप", "सन्नाटा"),
        "miss" to listOf("याद", "इंतजार", "तडप"),
        "missing" to listOf("याद", "यादें", "इंतजार"),
        "memory" to listOf("याद", "यादें", "यादों"),
        "memories" to listOf("यादें", "यादों", "याद", "लम्हे"),
        "wait" to listOf("इंतजार", "राह", "सब्र"),
        "waiting" to listOf("इंतजार", "राह"),
        "smile" to listOf("मुस्कान", "मुस्कुरा", "हंसी"),
        "happy" to listOf("खुशी", "खुश", "मुस्कान", "हंसी"),
        "happiness" to listOf("खुशी", "खुशियां", "सुकून", "मुस्कान"),
        "peace" to listOf("सुकून", "चैन", "शांति"),
        // People & Relationships
        "friend" to listOf("दोस्त", "यार", "मित्र", "दोस्ती"),
        "friends" to listOf("दोस्त", "यार", "दोस्ती", "यारी"),
        "friendship" to listOf("दोस्ती", "यारी", "दोस्त", "यार"),
        "family" to listOf("परिवार", "घर", "अपने", "रिश्ते"),
        "mom" to listOf("मां", "माता", "अम्मी"),
        "mother" to listOf("मां", "माता", "अम्मी"),
        "dad" to listOf("पापा", "पिता", "बाप", "अब्बू"),
        "father" to listOf("पिता", "पापा", "बाप", "अब्बू"),
        "brother" to listOf("भाई", "भैया"),
        "sister" to listOf("बहन", "दीदी"),
        "girl" to listOf("लडकी", "हसीना", "चांद", "हुस्न"),
        "beauty" to listOf("खूबसूरती", "हुस्न", "सुंदर", "तारीफ", "अदा"),
        "beautiful" to listOf("खूबसूरत", "हसीन", "सुंदर", "प्यारी"),
        // Nature & Imagery
        "moon" to listOf("चांद", "चांदनी", "महताब"),
        "sun" to listOf("सूरज", "धूप", "आफताब"),
        "star" to listOf("तारे", "सितारे", "आसमान"),
        "stars" to listOf("तारे", "सितारे"),
        "sky" to listOf("आसमान", "गगन", "फलक"),
        "rain" to listOf("बारिश", "सावन", "बूंद", "बरसात"),
        "flower" to listOf("फूल", "गुलाब", "कली"),
        "rose" to listOf("गुलाब", "फूल", "कांटे"),
        "night" to listOf("रात", "शब", "अंधेरा", "चांदनी"),
        "morning" to listOf("सुबह", "सवेरा"),
        "evening" to listOf("शाम"),
        "wind" to listOf("हवा", "बयार"),
        "ocean" to listOf("समंदर", "सागर", "दरिया"),
        "river" to listOf("नदी", "दरिया", "किनारा"),
        "fire" to listOf("आग", "चिंगारी", "शोल"),
        "eyes" to listOf("आंख", "आंखें", "नजर", "पलक"),
        "mirror" to listOf("आईना", "आइना", "शीशा", "अक्स"),
        "dream" to listOf("ख्वाब", "सपना", "सपने"),
        "dreams" to listOf("ख्वाब", "सपने"),
        "shadow" to listOf("साया", "परछाई"),
        "light" to listOf("रोशनी", "उजाला", "चिराग", "दीया"),
        "dark" to listOf("अंधेरा", "अंधेरे", "रात"),
        // Life, Time, Spirit & Misc
        "life" to listOf("जिंदगी", "जीवन", "सफर"),
        "time" to listOf("वक्त", "समय", "दौर", "लम्हे"),
        "journey" to listOf("सफर", "रास्ते", "मुसाफिर", "मंजिल"),
        "travel" to listOf("सफर", "मुसाफिर", "रास्ते", "यात्रा"),
        "road" to listOf("रास्ता", "रास्ते", "राह"),
        "destination" to listOf("मंजिल"),
        "success" to listOf("कामयाबी", "सफलता", "जीत", "मंजिल"),
        "motivation" to listOf("हौसला", "उम्मीद", "हिम्मत", "जीत", "संघर्ष"),
        "motivational" to listOf("हौसला", "उम्मीद", "हिम्मत", "जीत"),
        "hope" to listOf("उम्मीद", "आस", "भरोसा"),
        "trust" to listOf("भरोसा", "विश्वास", "यकीन"),
        "promise" to listOf("वादा", "वादे", "कसम"),
        "god" to listOf("खुदा", "रब", "भगवान", "अल्लाह", "ईश्वर"),
        "prayer" to listOf("दुआ", "इबादत", "सजदा"),
        "death" to listOf("मौत", "जनाजा", "कफन", "कब्र"),
        "wine" to listOf("शराब", "जाम", "साकी", "मयखाना", "नशा"),
        "drink" to listOf("शराब", "जाम", "पीना", "साकी"),
        "attitude" to listOf("अंदाज", "तेवर", "औकात", "स्वाभिमान", "रॉयल"),
        "royal" to listOf("रॉयल", "बादशाह", "नवाब", "अंदाज"),
        "king" to listOf("बादशाह", "राजा", "शेर"),
        "funny" to listOf("हंसी", "मजाक", "चुटकुले", "ठहाके"),
        "joke" to listOf("चुटकुले", "मजाक", "हंसी"),
        "jokes" to listOf("चुटकुले", "मजाक", "हंसी"),
        "comedy" to listOf("चुटकुले", "मजाक", "हंसी"),
        "india" to listOf("भारत", "हिंदुस्तान", "वतन", "तिरंगा"),
        "patriotic" to listOf("देशभक्ति", "वतन", "भारत", "तिरंगा"),
        // Renowned Poets (Roman/English spellings -> normalized Hindi)
        "ghalib" to listOf("गालिब", "मिर्जा"),
        "galib" to listOf("गालिब", "मिर्जा"),
        "mirza" to listOf("मिर्जा", "गालिब"),
        "gulzar" to listOf("गुलजार"),
        "rahat" to listOf("राहत", "इंदौरी"),
        "indori" to listOf("इंदौरी", "राहत"),
        "jaun" to listOf("जौन", "एलिया"),
        "elia" to listOf("एलिया", "जौन"),
        "eliya" to listOf("एलिया", "जौन"),
        "iqbal" to listOf("इकबाल", "अल्लामा"),
        "faiz" to listOf("फैज"),
        "faraz" to listOf("फराज", "अहमद"),
        "munawwar" to listOf("मुनव्वर", "राना"),
        "munawar" to listOf("मुनव्वर", "राना"),
        "rana" to listOf("राना", "मुनव्वर"),
        "bashir" to listOf("बशीर", "बद्र"),
        "badr" to listOf("बद्र", "बशीर"),
        "kumar" to listOf("कुमार", "विश्वास"),
        "vishwas" to listOf("विश्वास", "कुमार"),
        "dushyant" to listOf("दुष्यंत"),
        "mir" to listOf("मीर"),
        "meer" to listOf("मीर"),
        "parveen" to listOf("परवीन", "शाकिर"),
        "shakir" to listOf("शाकिर", "परवीन"),
        "nida" to listOf("निदा", "फाजली"),
        "fazli" to listOf("फाजली", "निदा"),
        "waseem" to listOf("वसीम", "बरेलवी"),
        "barelvi" to listOf("बरेलवी", "वसीम"),
        "bismil" to listOf("बिस्मिल")
    )

    private val GENERIC_ALL_SHAYARI_TOKENS: Set<String> = setOf(
        "shayari", "shayri", "shairi", "shayree", "shayar", "poetry", "poem", "quote", "quotes",
        "status", "शायरी", "शायर", "कविता", "स्टेटस"
    )

    private val SEARCH_FILLER_TOKENS: Set<String> = setOf(
        "shayari", "shayri", "shairi", "shayree", "shayar", "poetry", "poem", "quote", "quotes",
        "status", "caption", "captions", "lines", "line", "post", "posts", "wali", "wala", "wale",
        "ki", "ke", "ka", "par", "pe", "in", "on", "for", "of", "the", "and", "or",
        "hindi", "english", "urdu", "hinglish", "best", "top", "new", "latest", "viral", "trending",
        "शायरी", "शायर", "स्टेटस", "कविता", "पर", "की", "के", "का", "वाली", "वाला", "वाले", "में", "हिंदी"
    )

    private val NORMALIZED_KEYWORD_TABLE: List<Pair<List<String>, List<String>>> by lazy {
        KEYWORD_TO_CATEGORY_IDS.map { (keywords, catIds) ->
            keywords.map { normalizeForSearch(it) }.distinct() to catIds
        }
    }

    private val NORMALIZED_CATEGORY_META: Map<String, List<String>> by lazy {
        ShayariCategory.entries.associate { cat ->
            cat.id to listOf(
                normalizeForSearch(cat.id.replace('_', ' ')),
                normalizeForSearch(cat.titleHi),
                normalizeForSearch(cat.labelEn),
                normalizeForSearch(cat.taglineHi)
            ).filter { it.isNotEmpty() }
        }
    }

    /**
     * Fast single-pass search normalizer:
     * - lowercases Latin chars
     * - strips Devanagari nukta (U+093C) so बेवफाई == बेवफ़ाई, जिंदगी == ज़िंदगी, इश्क == इश्क़
     * - unifies chandrabindu (ँ) -> anusvara (ं) so आंसू == आँसू, चांद == चाँद, मां == माँ
     * - strips leading/embedded `#` and collapses whitespace
     */
    fun normalizeForSearch(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length)
        var lastWasSpace = true
        for (i in 0 until input.length) {
            val ch = input[i]
            when {
                ch == '\u093C' -> continue // Devanagari nukta
                ch == '\u200C' || ch == '\u200D' -> continue // ZWNJ / ZWJ
                ch == '\u0901' -> {
                    sb.append('\u0902') // ँ -> ं
                    lastWasSpace = false
                }
                ch == '#' || ch == ',' || ch == '.' || ch == '!' || ch == '?' ||
                    ch == ';' || ch == ':' || ch == '"' || ch == '\'' || ch == '(' ||
                    ch == ')' || ch == '[' || ch == ']' || ch == '|' || ch == '•' ||
                    ch == '—' || ch == '–' || ch == '/' || ch == '\\' || ch == '_' ||
                    ch.isWhitespace() -> {
                    if (!lastWasSpace) {
                        sb.append(' ')
                        lastWasSpace = true
                    }
                }
                ch in 'A'..'Z' -> {
                    sb.append((ch.code + 32).toChar())
                    lastWasSpace = false
                }
                else -> {
                    sb.append(ch)
                    lastWasSpace = false
                }
            }
        }
        val len = sb.length
        if (len > 0 && sb[len - 1] == ' ') {
            sb.setLength(len - 1)
        }
        return sb.toString()
    }

    data class SearchQuerySpec(
        val rawNormalized: String,
        val meaningfulTokens: List<String>,
        val expandedTextTerms: List<String>,
        val matchedCategoryIds: Set<String>,
        val matchAllFallback: Boolean
    )

    fun buildSearchSpec(rawQuery: String): SearchQuerySpec {
        val rawNorm = normalizeForSearch(rawQuery)
        if (rawNorm.isEmpty()) {
            return SearchQuerySpec("", emptyList(), emptyList(), emptySet(), false)
        }

        val allTokens = rawNorm.split(' ').filter { it.isNotEmpty() }
        val filteredTokens = if (allTokens.size > 1) {
            allTokens.filter { it !in SEARCH_FILLER_TOKENS }
        } else {
            allTokens.filter { it !in GENERIC_ALL_SHAYARI_TOKENS }
        }
        val matchAllFallback = filteredTokens.isEmpty() &&
            allTokens.any { it in GENERIC_ALL_SHAYARI_TOKENS }
        val meaningfulTokens = if (filteredTokens.isNotEmpty()) filteredTokens else allTokens

        val matchedCategories = LinkedHashSet<String>()
        val expandedTerms = LinkedHashSet<String>()

        // Phrase-level matches first (e.g. "broken heart", "new year", "jija sali", "जख्मी दिल")
        val meaningfulPhrase = meaningfulTokens.joinToString(" ")
        expandedTerms.add(meaningfulPhrase)
        if (rawNorm != meaningfulPhrase) {
            expandedTerms.add(rawNorm)
        }
        if (HinglishTransliterator.hasConvertibleHinglish(meaningfulPhrase)) {
            val transliteratedPhrase = normalizeForSearch(
                HinglishTransliterator.transliterateText(meaningfulPhrase)
            )
            if (transliteratedPhrase.isNotEmpty()) {
                expandedTerms.add(transliteratedPhrase)
            }
        }

        matchCategoriesForTerm(meaningfulPhrase, matchedCategories)

        // Token-level expansion across categories, English concepts, and Hinglish transliteration
        for (token in meaningfulTokens) {
            expandedTerms.add(token)
            matchCategoriesForTerm(token, matchedCategories)

            ENGLISH_CONCEPT_TO_NORMALIZED_HINDI[token]?.forEach { hindiStem ->
                val normStem = normalizeForSearch(hindiStem)
                if (normStem.isNotEmpty()) {
                    expandedTerms.add(normStem)
                    matchCategoriesForTerm(normStem, matchedCategories)
                }
            }

            // Prefix lookup for partial English words (e.g. "moha", "roma", "bewa", "attit", "ghal")
            if (token.length >= 4 && HinglishTransliterator.hasConvertibleHinglish(token)) {
                ENGLISH_CONCEPT_TO_NORMALIZED_HINDI.entries.forEach { (engKey, hindiStems) ->
                    if (engKey.startsWith(token)) {
                        hindiStems.forEach { stem ->
                            val normStem = normalizeForSearch(stem)
                            if (normStem.isNotEmpty()) expandedTerms.add(normStem)
                        }
                    }
                }
            }

            if (HinglishTransliterator.hasConvertibleHinglish(token)) {
                val primaryHindi = normalizeForSearch(HinglishTransliterator.transliterateWord(token))
                if (primaryHindi.isNotEmpty()) {
                    expandedTerms.add(primaryHindi)
                    matchCategoriesForTerm(primaryHindi, matchedCategories)
                }
                if (token.length >= 2) {
                    HinglishTransliterator.suggestCandidates(token, maxResults = 3).forEach { cand ->
                        val normCand = normalizeForSearch(cand)
                        if (normCand.isNotEmpty()) {
                            expandedTerms.add(normCand)
                            matchCategoriesForTerm(normCand, matchedCategories)
                        }
                    }
                }
            }
        }

        return SearchQuerySpec(
            rawNormalized = rawNorm,
            meaningfulTokens = meaningfulTokens,
            expandedTextTerms = expandedTerms.filter { it.length >= 2 || meaningfulTokens.size == 1 },
            matchedCategoryIds = matchedCategories,
            matchAllFallback = matchAllFallback
        )
    }

    private fun matchCategoriesForTerm(normTerm: String, out: MutableSet<String>) {
        if (normTerm.isEmpty()) return
        for ((catId, metaTerms) in NORMALIZED_CATEGORY_META) {
            if (normTerm.length == 1) {
                if (metaTerms.any { meta -> meta.startsWith(normTerm) }) {
                    out.add(catId)
                }
            } else if (metaTerms.any { meta ->
                    meta == normTerm ||
                        meta.contains(normTerm) ||
                        (normTerm.length >= 3 && normTerm.contains(meta) && meta.length >= 3)
                }
            ) {
                out.add(catId)
            }
        }
        if (normTerm.length >= 2) {
            for ((keywords, catIds) in NORMALIZED_KEYWORD_TABLE) {
                if (keywords.any { kw ->
                        kw == normTerm ||
                            (normTerm.length >= 3 && kw.startsWith(normTerm)) ||
                            (kw.length >= 3 && normTerm.contains(kw))
                    }
                ) {
                    out.addAll(catIds)
                }
            }
        }
    }

    /**
     * Computes a relevance score (>0 means match) against pre-normalized shayari text & categories.
     */
    fun scoreMatch(
        spec: SearchQuerySpec,
        primaryCategoryId: String,
        multiCategoryIds: Set<String>?,
        normCouplet: String,
        normPoet: String,
        normMoodTag: String
    ): Int {
        if (spec.rawNormalized.isEmpty()) return 1
        if (spec.matchAllFallback) return 10

        var score = 0

        // 1. Direct exact phrase hit in couplet, poet, or tags
        val phrase = spec.meaningfulTokens.joinToString(" ")
        if (phrase.isNotEmpty()) {
            if (normCouplet.contains(phrase)) score += 120
            if (normPoet.contains(phrase)) score += 115
            if (normMoodTag.contains(phrase)) score += 110
        }

        // 2. Category match (primary or multi-tag category)
        if (primaryCategoryId in spec.matchedCategoryIds) {
            score += 75
        } else if (multiCategoryIds != null && multiCategoryIds.any { it in spec.matchedCategoryIds }) {
            score += 65
        }

        // 3. Expanded terms (English -> Hindi equivalents, Hinglish transliteration, individual tokens)
        var textHitCount = 0
        for (term in spec.expandedTextTerms) {
            val hitCouplet = normCouplet.contains(term)
            val hitPoet = normPoet.contains(term)
            val hitTag = normMoodTag.contains(term)
            if (hitCouplet || hitPoet || hitTag) {
                textHitCount++
                if (hitCouplet) score += 45
                if (hitPoet) score += 50
                if (hitTag) score += 40
            }
        }

        // Bonus when multi-word query hits multiple words in the shayari
        if (spec.meaningfulTokens.size > 1 && textHitCount >= spec.meaningfulTokens.size) {
            score += 40
        }

        return score
    }

    /**
     * Instant starter tags auto-populated when a user selects a primary category while posting
     * (e.g. mohabbat -> ["मोहब्बत", "romantic शायरी", "ishq shayri", "pyar", "love"]).
     */
    fun autoTagsForCategory(categoryId: String): List<String> {
        val related = CATEGORY_RELATED_POST_TAGS[categoryId]
        if (!related.isNullOrEmpty()) {
            return related.take(5)
        }
        val cat = ShayariCategory.fromId(categoryId)
        return listOf(cat.titleHi, "${cat.labelEn.lowercase()} shayari")
    }

    /**
     * Full ordered tag suggestions list for the Write Studio picker:
     * category-specific bilingual tags first, followed by general popular tags (deduplicated).
     */
    fun suggestedTagsForCategory(categoryId: String): List<String> {
        val ordered = LinkedHashSet<String>()
        CATEGORY_RELATED_POST_TAGS[categoryId]?.let { ordered.addAll(it) }
        ordered.addAll(POPULAR_POST_TAGS)
        return ordered.toList()
    }

    /**
     * Swaps out previously auto-added tags from [previousCategoryId] (if any) and merges
     * [newCategoryId]'s related starter tags while preserving any custom/manual tags.
     */
    fun applyCategoryAutoTags(
        currentRaw: String,
        newCategoryId: String,
        previousCategoryId: String? = null,
        maxLen: Int = 160
    ): String {
        val existing = parseTags(currentRaw)
        val previousAutoNormalized = previousCategoryId
            ?.let { autoTagsForCategory(it) }
            ?.map { it.lowercase() }
            ?.toSet()
            .orEmpty()

        val preservedManual = if (previousCategoryId != null && previousCategoryId != newCategoryId) {
            existing.filterNot { it.lowercase() in previousAutoNormalized }
        } else {
            existing
        }

        val combined = LinkedHashSet<String>()
        autoTagsForCategory(newCategoryId).forEach { combined.add(it) }
        preservedManual.forEach { tag ->
            if (combined.none { it.equals(tag, ignoreCase = true) }) {
                combined.add(tag)
            }
        }
        return formatTagsStorage(combined.toList(), maxLen = maxLen)
    }

    fun parseTags(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val seen = LinkedHashSet<String>()
        raw.split(TAG_SPLIT_REGEX).forEach { part ->
            val clean = part.trim().removePrefix("#").trim()
            if (clean.isNotEmpty() && clean != "मेरी शायरी") {
                seen.add(clean.take(28))
            }
        }
        return seen.toList()
    }

    fun formatTagsStorage(tags: List<String>, maxLen: Int = 160): String {
        val unique = tags.mapNotNull { t ->
            t.trim().removePrefix("#").trim().take(28).takeIf { it.isNotEmpty() }
        }.distinct()
        if (unique.isEmpty()) return ""
        val sb = StringBuilder()
        for (tag in unique) {
            val candidate = if (sb.isEmpty()) tag else ", $tag"
            if (sb.length + candidate.length > maxLen) break
            sb.append(candidate)
        }
        return sb.toString()
    }

    fun toggleTagInString(currentRaw: String, tagToToggle: String, maxLen: Int = 160): String {
        val cleanTarget = tagToToggle.trim().removePrefix("#").trim()
        if (cleanTarget.isEmpty()) return currentRaw
        val existing = parseTags(currentRaw).toMutableList()
        val matchIdx = existing.indexOfFirst { it.equals(cleanTarget, ignoreCase = true) }
        if (matchIdx >= 0) {
            existing.removeAt(matchIdx)
        } else {
            existing.add(cleanTarget)
        }
        return formatTagsStorage(existing, maxLen)
    }

    fun addTagToString(currentRaw: String, tagToAdd: String, maxLen: Int = 160): String {
        val cleanTarget = tagToAdd.trim().removePrefix("#").trim()
        if (cleanTarget.isEmpty()) return currentRaw
        val existing = parseTags(currentRaw).toMutableList()
        if (existing.none { it.equals(cleanTarget, ignoreCase = true) }) {
            existing.add(cleanTarget)
        }
        return formatTagsStorage(existing, maxLen)
    }

    /**
     * Trims a multi-tag string safely to <= 40 chars on a comma boundary so
     * Firestore personal_shayari rules (themeTag.size() <= 40) never fail.
     */
    fun trimTagsForCloudPersonal(raw: String, maxLen: Int = 40): String {
        val trimmed = raw.trim()
        if (trimmed.length <= maxLen) return trimmed
        val tags = parseTags(trimmed)
        if (tags.isEmpty()) return trimmed.take(maxLen)
        val formatted = formatTagsStorage(tags, maxLen)
        return formatted.ifBlank { tags.first().take(maxLen) }
    }

    fun resolvePrimaryCategory(
        rawTag: String,
        explicitCategoryId: String? = null,
        fallback: ShayariCategory = ShayariCategory.ZINDAGI
    ): ShayariCategory {
        if (!explicitCategoryId.isNullOrBlank()) {
            val found = ShayariCategory.entries.firstOrNull { it.id == explicitCategoryId }
            if (found != null) return found
        }
        val tags = parseTags(rawTag)
        for (tag in tags) {
            val exact = ShayariCategory.entries.firstOrNull { cat ->
                cat.titleHi.equals(tag, ignoreCase = true) ||
                    cat.id.equals(tag, ignoreCase = true) ||
                    cat.labelEn.equals(tag, ignoreCase = true)
            }
            if (exact != null) return exact
        }
        for (tag in tags) {
            val norm = normalizeForSearch(tag)
            for ((keywords, catIds) in NORMALIZED_KEYWORD_TABLE) {
                if (keywords.any { kw -> norm.contains(kw) || (norm.length >= 3 && kw.contains(norm)) }) {
                    val firstId = catIds.firstOrNull()
                    if (firstId != null) return ShayariCategory.fromId(firstId)
                }
            }
        }
        return fallback
    }

    /**
     * Returns ALL category IDs where a shayari should appear (primary category + related categories
     * matched from any of its tags).
     */
    fun matchingCategoryIds(primaryCategoryId: String, moodTag: String): Set<String> {
        val result = LinkedHashSet<String>()
        result.add(primaryCategoryId)
        val tags = parseTags(moodTag)
        if (tags.isEmpty()) return result

        for (tag in tags) {
            val norm = normalizeForSearch(tag)
            ShayariCategory.entries.forEach { cat ->
                if (cat.titleHi.equals(tag, ignoreCase = true) ||
                    cat.id.equals(tag, ignoreCase = true) ||
                    cat.labelEn.equals(tag, ignoreCase = true)
                ) {
                    result.add(cat.id)
                }
            }
            for ((keywords, catIds) in NORMALIZED_KEYWORD_TABLE) {
                if (keywords.any { kw -> norm.contains(kw) || (norm.length >= 3 && kw.contains(norm)) }) {
                    result.addAll(catIds)
                }
            }
        }
        return result
    }

    /**
     * Clean hashtag pills for rendering on shayari cards (max 4 tags).
     */
    fun displayHashTags(moodTag: String, primaryCategoryId: String, maxTags: Int = 4): List<String> {
        val primaryTitle = ShayariCategory.fromId(primaryCategoryId).titleHi
        val parsed = parseTags(moodTag)
        return parsed
            .filterNot { it.equals(primaryTitle, ignoreCase = true) && parsed.size == 1 }
            .take(maxTags)
    }

    /**
     * Returns true if a ShayariEntity carries multi-tag / user-post metadata that warrants
     * multi-category matching (keeps 30,000 seed items O(1) fast).
     */
    fun hasMultiTagMetadata(shayari: ShayariEntity): Boolean {
        if (shayari.id >= 1_000_000) return true
        val tag = shayari.moodTag
        return tag.contains(',') || tag.contains('#') || tag.contains('•') || tag.contains('|')
    }
}
