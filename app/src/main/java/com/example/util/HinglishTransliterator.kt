package com.example.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class TrailingHinglishWord(
    val word: String,
    val startIndex: Int,
    val endIndex: Int
)

/**
 * Fast, offline Hinglish-to-Devanagari transliterator tuned for Hindi/Urdu shayari
 * and everyday mobile typing. Combines a curated shayari/conversational lexicon
 * with a rule-based phonetic syllable converter.
 */
object HinglishTransliterator {

    val quickMoodWords: List<String> = listOf(
        "दिल",
        "इश्क़",
        "ज़िंदगी",
        "ख़ामोशी",
        "यादें",
        "ख़ुदा",
        "तन्हाई",
        "सुकून",
        "आँखें",
        "वक़्त",
        "सफ़र",
        "मुस्कान",
        "दुआ",
        "चाँद"
    )

    private val exactLexicon: Map<String, String> = mapOf(
        // Core shayari & emotion words
        "pyar" to "प्यार",
        "pyaar" to "प्यार",
        "pyara" to "प्यारा",
        "pyari" to "प्यारी",
        "pyare" to "प्यारे",
        "ishq" to "इश्क़",
        "ishk" to "इश्क़",
        "mohabbat" to "मोहब्बत",
        "muhabbat" to "मोहब्बत",
        "dil" to "दिल",
        "dhadkan" to "धड़कन",
        "saans" to "साँस",
        "saansein" to "साँसें",
        "jaan" to "जान",
        "rooh" to "रूह",
        "zindagi" to "ज़िंदगी",
        "zindgi" to "ज़िंदगी",
        "jindagi" to "ज़िंदगी",
        "khuda" to "ख़ुदा",
        "rab" to "रब",
        "allah" to "अल्लाह",
        "bhagwan" to "भगवान",
        "ibadat" to "इबादत",
        "ibaadat" to "इबादत",
        "dua" to "दुआ",
        "duaa" to "दुआ",
        "kaynat" to "कायनात",
        "kayanat" to "कायनात",
        "kaaynaat" to "कायनात",
        "kainaat" to "कायनात",
        "aansu" to "आँसू",
        "aansoo" to "आँसू",
        "ansu" to "आँसू",
        "aankh" to "आँख",
        "aankhein" to "आँखें",
        "ankhein" to "आँखें",
        "ankhen" to "आँखें",
        "aankhen" to "आँखें",
        "nazar" to "नज़र",
        "nazrein" to "नज़रें",
        "palkein" to "पलकें",
        "tanhai" to "तन्हाई",
        "tanhaai" to "तन्हाई",
        "tanha" to "तन्हा",
        "akelapan" to "अकेलापन",
        "akela" to "अकेला",
        "akeli" to "अकेली",
        "akele" to "अकेले",
        "janaza" to "जनाज़ा",
        "janaaza" to "जनाज़ा",
        "saqi" to "साक़ी",
        "saki" to "साक़ी",
        "maikhana" to "मयख़ाना",
        "maykhana" to "मयख़ाना",
        "sharab" to "शराब",
        "sharaab" to "शराब",
        "sharabi" to "शराबी",
        "jaam" to "जाम",
        "talabgar" to "तलबगार",
        "talab" to "तलब",
        "raahi" to "राही",
        "rahi" to "राही",
        "musafir" to "मुसाफ़िर",
        "musaafir" to "मुसाफ़िर",
        "safar" to "सफ़र",
        "manzil" to "मंज़िल",
        "rasta" to "रास्ता",
        "raasta" to "रास्ता",
        "raah" to "राह",
        "raahein" to "राहें",
        "sanam" to "सनम",
        "kasam" to "क़सम",
        "qasam" to "क़सम",
        "gawah" to "गवाह",
        "gawahi" to "गवाही",
        "adalat" to "अदालत",
        "adaalat" to "अदालत",
        "intezar" to "इंतज़ार",
        "intizar" to "इंतज़ार",
        "intazaar" to "इंतज़ार",
        "intezaar" to "इंतज़ार",
        "khamoshi" to "ख़ामोशी",
        "khamosh" to "ख़ामोश",
        "sukoon" to "सुकून",
        "sukun" to "सुकून",
        "chain" to "चैन",
        "dard" to "दर्द",
        "gam" to "ग़म",
        "gham" to "ग़म",
        "khushi" to "ख़ुशी",
        "khush" to "ख़ुश",
        "khushiyan" to "ख़ुशियाँ",
        "muskan" to "मुस्कान",
        "muskaan" to "मुस्कान",
        "muskurahat" to "मुस्कुराहट",
        "hasi" to "हँसी",
        "hansna" to "हँसना",
        "rona" to "रोना",
        "bewafa" to "बेवफ़ा",
        "bewafai" to "बेवफ़ाई",
        "wafa" to "वफ़ा",
        "wafadar" to "वफ़ादार",
        "judai" to "जुदाई",
        "judaai" to "जुदाई",
        "juda" to "जुदा",
        "yaad" to "याद",
        "yaadein" to "यादें",
        "yadein" to "यादें",
        "yaadon" to "यादों",
        "khwab" to "ख़्वाब",
        "khwaab" to "ख़्वाब",
        "sapna" to "सपना",
        "sapne" to "सपने",
        "neend" to "नींद",
        "raat" to "रात",
        "raatein" to "रातें",
        "din" to "दिन",
        "subah" to "सुबह",
        "shaam" to "शाम",
        "chand" to "चाँद",
        "chaand" to "चाँद",
        "chandni" to "चाँदनी",
        "suraj" to "सूरज",
        "taare" to "तारे",
        "sitare" to "सितारे",
        "aasman" to "आसमान",
        "aasmaan" to "आसमान",
        "zameen" to "ज़मीन",
        "jameen" to "ज़मीन",
        "duniya" to "दुनिया",
        "shahar" to "शहर",
        "shehar" to "शहर",
        "ghar" to "घर",
        "hawa" to "हवा",
        "barish" to "बारिश",
        "baarish" to "बारिश",
        "bunde" to "बूँदें",
        "boondein" to "बूँदें",
        "samandar" to "समंदर",
        "samundar" to "समंदर",
        "nadi" to "नदी",
        "kinara" to "किनारा",
        "toofan" to "तूफ़ान",
        "aag" to "आग",
        "paani" to "पानी",
        "pani" to "पानी",
        "phool" to "फूल",
        "gulab" to "गुलाब",
        "kaante" to "काँटे",
        "shama" to "शमा",
        "parwana" to "परवाना",
        "deewana" to "दीवाना",
        "diwana" to "दीवाना",
        "deewangi" to "दीवानगी",
        "junoon" to "जुनून",
        "arzu" to "आरज़ू",
        "aarzoo" to "आरज़ू",
        "tamanna" to "तमन्ना",
        "chahat" to "चाहत",
        "chaahat" to "चाहत",
        "umeed" to "उम्मीद",
        "ummeed" to "उम्मीद",
        "bharosa" to "भरोसा",
        "vishwas" to "विश्वास",
        "sitam" to "सितम",
        "dhoka" to "धोखा",
        "dhokha" to "धोखा",
        "kismat" to "क़िस्मत",
        "qismat" to "क़िस्मत",
        "naseeb" to "नसीब",
        "taqdeer" to "तक़दीर",
        "takdeer" to "तक़दीर",
        "waqt" to "वक़्त",
        "wakt" to "वक़्त",
        "lamha" to "लम्हा",
        "lamhe" to "लम्हे",
        "pal" to "पल",
        "umr" to "उम्र",
        "umar" to "उम्र",
        "ehsaas" to "एहसास",
        "jazbaat" to "जज़्बात",
        "khayal" to "ख़याल",
        "khayaal" to "ख़याल",
        "alfaaz" to "अल्फ़ाज़",
        "alfaz" to "अल्फ़ाज़",
        "lafz" to "लफ़्ज़",
        "baat" to "बात",
        "baatein" to "बातें",
        "kalam" to "क़लम",
        "qalam" to "क़लम",
        "shayari" to "शायरी",
        "shayri" to "शायरी",
        "shayar" to "शायर",
        "ghazal" to "ग़ज़ल",
        "kavita" to "कविता",
        "geet" to "गीत",
        "nagma" to "नग़्मा",
        "awaaz" to "आवाज़",
        "awaz" to "आवाज़",
        "aaina" to "आईना",
        "chehra" to "चेहरा",
        "tasveer" to "तस्वीर",
        "saya" to "साया",
        "saaya" to "साया",
        "roshni" to "रोशनी",
        "andhera" to "अँधेरा",
        "andhere" to "अँधेरे",
        "dosti" to "दोस्ती",
        "dost" to "दोस्त",
        "yaar" to "यार",
        "yaari" to "यारी",
        "rishta" to "रिश्ता",
        "rishte" to "रिश्ते",
        "haath" to "हाथ",
        "saath" to "साथ",
        "sath" to "साथ",
        "naam" to "नाम",
        "kaam" to "काम",
        "aaram" to "आराम",
        "badnaam" to "बदनाम",
        "mohlat" to "मोहल्त",
        "mulakat" to "मुलाक़ात",
        "mulaqat" to "मुलाक़ात",
        "alvida" to "अलविदा",
        "shukriya" to "शुक्रिया",
        "mehfil" to "महफ़िल",

        // Pronouns, verbs, connectors & everyday Hindi words
        "main" to "मैं",
        "mai" to "मैं",
        "mein" to "में",
        "me" to "में",
        "hum" to "हम",
        "ham" to "हम",
        "tum" to "तुम",
        "tu" to "तू",
        "aap" to "आप",
        "woh" to "वो",
        "wo" to "वो",
        "yeh" to "ये",
        "ye" to "ये",
        "mera" to "मेरा",
        "meri" to "मेरी",
        "mere" to "मेरे",
        "tera" to "तेरा",
        "teri" to "तेरी",
        "tere" to "तेरे",
        "hamara" to "हमारा",
        "hamari" to "हमारी",
        "hamare" to "हमारे",
        "tumhara" to "तुम्हारा",
        "tumhari" to "तुम्हारी",
        "tumhare" to "तुम्हारे",
        "apna" to "अपना",
        "apni" to "अपनी",
        "apne" to "अपने",
        "uska" to "उसका",
        "uski" to "उसकी",
        "uske" to "उसके",
        "iska" to "इसका",
        "iski" to "इसकी",
        "iske" to "इसके",
        "unke" to "उनके",
        "unki" to "उनकी",
        "unka" to "उनका",
        "kisi" to "किसी",
        "koi" to "कोई",
        "kuch" to "कुछ",
        "kuchh" to "कुछ",
        "sab" to "सब",
        "sabhi" to "सभी",
        "har" to "हर",
        "ek" to "एक",
        "do" to "दो",
        "teen" to "तीन",
        "char" to "चार",
        "hai" to "है",
        "hain" to "हैं",
        "hoon" to "हूँ",
        "hu" to "हूँ",
        "ho" to "हो",
        "hota" to "होता",
        "hoti" to "होती",
        "hote" to "होते",
        "hua" to "हुआ",
        "hui" to "हुई",
        "huye" to "हुए",
        "hue" to "हुए",
        "hoga" to "होगा",
        "hogi" to "होगी",
        "honge" to "होंगे",
        "tha" to "था",
        "thi" to "थी",
        "the" to "थे",
        "nahi" to "नहीं",
        "nahin" to "नहीं",
        "na" to "ना",
        "mat" to "मत",
        "kya" to "क्या",
        "kyun" to "क्यों",
        "kyunki" to "क्योंकि",
        "kyo" to "क्यों",
        "kaise" to "कैसे",
        "kaisa" to "कैसा",
        "kaisi" to "कैसी",
        "kaun" to "कौन",
        "kahan" to "कहाँ",
        "yahan" to "यहाँ",
        "wahan" to "वहाँ",
        "jahan" to "जहाँ",
        "kab" to "कब",
        "jab" to "जब",
        "tab" to "तब",
        "ab" to "अब",
        "abhi" to "अभी",
        "kabhi" to "कभी",
        "hamesha" to "हमेशा",
        "aaj" to "आज",
        "kal" to "कल",
        "parso" to "परसों",
        "aur" to "और",
        "bhi" to "भी",
        "hi" to "ही",
        "par" to "पर",
        "lekin" to "लेकिन",
        "magar" to "मगर",
        "agar" to "अगर",
        "phir" to "फिर",
        "fir" to "फिर",
        "se" to "से",
        "ko" to "को",
        "ka" to "का",
        "ki" to "की",
        "ke" to "के",
        "pe" to "पे",
        "tak" to "तक",
        "liye" to "लिए",
        "bina" to "बिना",
        "bin" to "बिन",
        "sirf" to "सिर्फ़",
        "bas" to "बस",
        "shayad" to "शायद",
        "zaroor" to "ज़रूर",
        "bilkul" to "बिल्कुल",
        "bahut" to "बहुत",
        "bohot" to "बहुत",
        "zyada" to "ज़्यादा",
        "jyada" to "ज़्यादा",
        "kam" to "कम",
        "jaise" to "जैसे",
        "waise" to "वैसे",
        "aise" to "ऐसे",
        "aisa" to "ऐसा",
        "aisi" to "ऐसी",
        "waisa" to "वैसा",
        "sach" to "सच",
        "jhoot" to "झूठ",
        "jhooth" to "झूठ",
        "achha" to "अच्छा",
        "accha" to "अच्छा",
        "bura" to "बुरा",
        "bada" to "बड़ा",
        "chhota" to "छोटा",
        "naya" to "नया",
        "nayi" to "नयी",
        "naye" to "नये",
        "purana" to "पुराना",
        "paas" to "पास",
        "door" to "दूर",
        "dur" to "दूर",
        "andar" to "अंदर",
        "bahar" to "बाहर",
        "upar" to "ऊपर",
        "neeche" to "नीचे",
        "aage" to "आगे",
        "peeche" to "पीछे",
        "karna" to "करना",
        "karta" to "करता",
        "karti" to "करती",
        "karte" to "करते",
        "kar" to "कर",
        "kiya" to "किया",
        "kiye" to "किये",
        "karo" to "करो",
        "karunga" to "करूँगा",
        "chhod" to "छोड़",
        "chhodna" to "छोड़ना",
        "jeena" to "जीना",
        "marna" to "मरना",
        "milna" to "मिलना",
        "mila" to "मिला",
        "mili" to "मिली",
        "mile" to "मिले",
        "bichhad" to "बिछड़",
        "bhool" to "भूल",
        "bhul" to "भूल",
        "bhoolna" to "भूलना",
        "toot" to "टूट",
        "toota" to "टूटा",
        "tuta" to "टूटा",
        "bikhar" to "बिखर",
        "likhna" to "लिखना",
        "likh" to "लिख",
        "likha" to "लिखा",
        "padhna" to "पढ़ना",
        "padh" to "पढ़",
        "dekhna" to "देखना",
        "dekh" to "देख",
        "dekha" to "देखा",
        "sunna" to "सुनना",
        "sun" to "सुन",
        "suna" to "सुना",
        "bolna" to "बोलना",
        "bol" to "बोल",
        "bola" to "बोला",
        "kehna" to "कहना",
        "keh" to "कह",
        "kaha" to "कहा",
        "rehna" to "रहना",
        "reh" to "रह",
        "raha" to "रहा",
        "rahi" to "रही",
        "rahe" to "रहे",
        "jaana" to "जाना",
        "jana" to "जाना",
        "gaya" to "गया",
        "gayi" to "गयी",
        "gaye" to "गये",
        "aana" to "आना",
        "aaya" to "आया",
        "aayi" to "आयी",
        "aaye" to "आये",
        "chalna" to "चलना",
        "chal" to "चल",
        "chala" to "चला",
        "chalte" to "चलते",
        "samajhna" to "समझना",
        "samajh" to "समझ",
        "samjha" to "समझा",
        "sochna" to "सोचना",
        "soch" to "सोच",
        "socha" to "सोचा",
        "chahna" to "चाहना",
        "chahta" to "चाहता",
        "chahti" to "चाहती",
        "chahiye" to "चाहिए",
        "sakta" to "सकता",
        "sakti" to "सकती",
        "sakte" to "सकते",
        "diya" to "दिया",
        "liya" to "लिया",
        "paya" to "पाया",
        "khoya" to "खोया",
        "ban" to "बन",
        "bana" to "बना",
        "bani" to "बनी",
        "bane" to "बने",
        "lagta" to "लगता",
        "lagti" to "लगती",
        "lagte" to "लगते",
        "laga" to "लगा",
        "lagi" to "लगी",
        "lage" to "लगे",
        "milega" to "मिलेगा",
        "rahega" to "रहेगा"
    )

    private val initialVowels: List<Pair<String, String>> = listOf(
        "aa" to "आ",
        "ee" to "ई",
        "ii" to "ई",
        "oo" to "ऊ",
        "uu" to "ऊ",
        "ai" to "ऐ",
        "au" to "औ",
        "ou" to "औ",
        "ri" to "ऋ",
        "a" to "अ",
        "i" to "इ",
        "u" to "उ",
        "e" to "ए",
        "o" to "ओ"
    )

    private val medialMatras: List<Pair<String, String>> = listOf(
        "aa" to "ा",
        "ee" to "ी",
        "ii" to "ी",
        "oo" to "ू",
        "uu" to "ू",
        "ai" to "ै",
        "au" to "ौ",
        "ou" to "ौ",
        "a" to "",
        "i" to "ि",
        "u" to "ु",
        "e" to "े",
        "o" to "ो"
    )

    private val consonants: List<Pair<String, String>> = listOf(
        "ksh" to "क्ष",
        "chh" to "छ",
        "gya" to "ज्ञ",
        "gy" to "ज्ञ",
        "sh" to "श",
        "ch" to "च",
        "kh" to "ख",
        "gh" to "घ",
        "jh" to "झ",
        "th" to "थ",
        "dh" to "ध",
        "ph" to "फ",
        "bh" to "भ",
        "tr" to "त्र",
        "k" to "क",
        "g" to "ग",
        "c" to "क",
        "j" to "ज",
        "t" to "त",
        "d" to "द",
        "n" to "न",
        "p" to "प",
        "f" to "फ़",
        "b" to "ब",
        "m" to "म",
        "y" to "य",
        "r" to "र",
        "l" to "ल",
        "v" to "व",
        "w" to "व",
        "s" to "स",
        "h" to "ह",
        "z" to "ज़",
        "q" to "क़",
        "x" to "क्स"
    )

    fun isAsciiWordChar(ch: Char): Boolean = ch in 'a'..'z' || ch in 'A'..'Z'

    fun hasConvertibleHinglish(text: String): Boolean = text.any { isAsciiWordChar(it) }

    fun extractTrailingWord(textBeforeCursor: String): TrailingHinglishWord? {
        if (textBeforeCursor.isEmpty()) return null
        var start = textBeforeCursor.length
        while (start > 0 && isAsciiWordChar(textBeforeCursor[start - 1])) {
            start--
        }
        if (start == textBeforeCursor.length) return null
        return TrailingHinglishWord(
            word = textBeforeCursor.substring(start),
            startIndex = start,
            endIndex = textBeforeCursor.length
        )
    }

    fun transliterateWord(word: String): String {
        val cleaned = word.lowercase().trim()
        if (cleaned.isEmpty()) return word
        exactLexicon[cleaned]?.let { return it }
        return phoneticConvert(cleaned)
    }

    fun suggestCandidates(prefixOrWord: String, maxResults: Int = 4): List<String> {
        val cleaned = prefixOrWord.lowercase().trim()
        if (cleaned.isEmpty()) return emptyList()

        val results = linkedSetOf<String>()
        exactLexicon[cleaned]?.let { results.add(it) }

        if (cleaned.length >= 2) {
            exactLexicon.entries
                .asSequence()
                .filter { (key, _) -> key != cleaned && key.startsWith(cleaned) }
                .sortedBy { it.key.length }
                .take(maxResults)
                .forEach { (_, hindi) -> results.add(hindi) }
        }

        val phonetic = phoneticConvert(cleaned)
        if (phonetic.isNotBlank()) {
            results.add(phonetic)
        }
        return results.take(maxResults)
    }

    /**
     * Converts any Latin/Hinglish words inside [text] into Hindi Devanagari while
     * leaving existing Devanagari text, spaces, punctuation, and newlines untouched.
     */
    fun transliterateText(text: String): String {
        if (!hasConvertibleHinglish(text)) return text
        val out = StringBuilder(text.length)
        val token = StringBuilder()
        for (ch in text) {
            if (isAsciiWordChar(ch)) {
                token.append(ch)
            } else {
                if (token.isNotEmpty()) {
                    out.append(transliterateWord(token.toString()))
                    token.setLength(0)
                }
                out.append(ch)
            }
        }
        if (token.isNotEmpty()) {
            out.append(transliterateWord(token.toString()))
        }
        return out.toString()
    }

    /**
     * Handles live keyboard input when Hinglish mode is active: if the user just typed a
     * single space or newline right after a Latin word, converts that word automatically.
     */
    fun applySpaceOrNewlineAutoConvert(
        previous: TextFieldValue,
        incoming: TextFieldValue
    ): TextFieldValue {
        if (incoming.composition != null) return incoming
        if (incoming.text.length != previous.text.length + 1) return incoming
        val cursor = incoming.selection.start
        if (!incoming.selection.collapsed || cursor <= 0 || cursor > incoming.text.length) {
            return incoming
        }
        val insertedChar = incoming.text[cursor - 1]
        if (insertedChar != ' ' && insertedChar != '\n') return incoming

        val textBeforeSeparator = incoming.text.substring(0, cursor - 1)
        val trailing = extractTrailingWord(textBeforeSeparator) ?: return incoming
        val converted = transliterateWord(trailing.word)
        if (converted == trailing.word) return incoming

        val updatedText = buildString {
            append(incoming.text, 0, trailing.startIndex)
            append(converted)
            append(insertedChar)
            append(incoming.text, cursor, incoming.text.length)
        }
        val newCursor = trailing.startIndex + converted.length + 1
        return TextFieldValue(
            text = updatedText,
            selection = TextRange(newCursor)
        )
    }

    fun replaceActiveWord(
        fieldValue: TextFieldValue,
        replacement: String,
        appendSpace: Boolean = true
    ): TextFieldValue {
        val cursor = fieldValue.selection.start.coerceIn(0, fieldValue.text.length)
        val beforeCursor = fieldValue.text.substring(0, cursor)
        val afterCursor = fieldValue.text.substring(fieldValue.selection.end.coerceIn(cursor, fieldValue.text.length))
        val trailing = extractTrailingWord(beforeCursor)
        val prefix = if (trailing != null) {
            beforeCursor.substring(0, trailing.startIndex)
        } else {
            beforeCursor
        }
        val suffixSpace = if (appendSpace && !afterCursor.startsWith(" ")) " " else ""
        val updatedText = prefix + replacement + suffixSpace + afterCursor
        val newCursor = prefix.length + replacement.length + suffixSpace.length
        return TextFieldValue(
            text = updatedText,
            selection = TextRange(newCursor)
        )
    }

    fun insertAtCursor(
        fieldValue: TextFieldValue,
        snippet: String,
        padWithSpace: Boolean = false
    ): TextFieldValue {
        val start = fieldValue.selection.min.coerceIn(0, fieldValue.text.length)
        val end = fieldValue.selection.max.coerceIn(start, fieldValue.text.length)
        val before = fieldValue.text.substring(0, start)
        val after = fieldValue.text.substring(end)

        val needsLeadingSpace = padWithSpace &&
            before.isNotEmpty() &&
            !before.last().isWhitespace() &&
            !before.endsWith("—")
        val needsTrailingSpace = padWithSpace &&
            (after.isEmpty() || !after.first().isWhitespace())

        val insertion = buildString {
            if (needsLeadingSpace) append(' ')
            append(snippet)
            if (needsTrailingSpace) append(' ')
        }
        val updatedText = before + insertion + after
        return TextFieldValue(
            text = updatedText,
            selection = TextRange(before.length + insertion.length)
        )
    }

    private fun phoneticConvert(input: String): String {
        val out = StringBuilder()
        var idx = 0
        val len = input.length

        while (idx < len) {
            // 1. Handle initial vowel (start of word or right after another vowel)
            val vowelMatch = initialVowels.firstOrNull { (roman, _) ->
                input.startsWith(roman, idx)
            }
            if (vowelMatch != null) {
                out.append(vowelMatch.second)
                idx += vowelMatch.first.length
                continue
            }

            // 2. Nasalization before consonant or at end: e.g., "n" or "m" preceded by vowel
            // Let standard consonant matching handle if followed by vowel; check simple anusvara
            if ((input[idx] == 'n' || input[idx] == 'm') &&
                out.isNotEmpty() &&
                idx + 1 < len &&
                !isVowelChar(input[idx + 1]) &&
                input[idx + 1] != input[idx]
            ) {
                out.append("ं")
                idx++
                continue
            }

            // 3. Consonant cluster
            val consMatch = consonants.firstOrNull { (roman, _) ->
                input.startsWith(roman, idx)
            }
            if (consMatch != null) {
                out.append(consMatch.second)
                idx += consMatch.first.length

                if (idx < len) {
                    val matraMatch = medialMatras.firstOrNull { (roman, _) ->
                        input.startsWith(roman, idx)
                    }
                    if (matraMatch != null) {
                        out.append(matraMatch.second)
                        idx += matraMatch.first.length
                    } else if (idx < len && input[idx] != 'h') {
                        // Doubled consonant or consonant cluster without intervening vowel
                        val nextCons = consonants.firstOrNull { (roman, _) ->
                            input.startsWith(roman, idx)
                        }
                        val isNasal = (input[idx] == 'n' || input[idx] == 'm') &&
                            idx + 1 < len && !isVowelChar(input[idx + 1])
                        if (nextCons != null && !isNasal && consMatch.first != nextCons.first) {
                            // Keep readable schwa-free cluster only when clearly conjunct (r, y, l, v, t, k, etc.)
                            if (nextCons.first in CONJUNCT_FOLLOWERS) {
                                out.append("्")
                            }
                        } else if (nextCons != null && consMatch.first == nextCons.first) {
                            out.append("्")
                        }
                    }
                }
                continue
            }

            // Unknown non-vowel char fallback
            out.append(input[idx])
            idx++
        }
        return out.toString()
    }

    private fun isVowelChar(ch: Char): Boolean = ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u'

    private val CONJUNCT_FOLLOWERS = setOf("r", "y", "l", "v", "w", "k", "t", "p", "s")
}
