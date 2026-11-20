package com.example.data.model

/**
 * Multi-tag parsing, formatting, and cross-category discovery helper.
 * Allows a single shayari with 1 primary category and multiple tags
 * (e.g. "दर्द, बेवफ़ाई, जख्मी दिल") to appear in all closely related categories.
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

    private val TAG_SPLIT_REGEX = Regex("[,•|#/\n]+")

    private val KEYWORD_TO_CATEGORY_IDS: List<Pair<List<String>, List<String>>> = listOf(
        listOf("जख्मी दिल", "ज़ख़्मी दिल", "टूटा दिल", "दिल टूटा", "दिल का दर्द", "zakhmi", "broken", "heartbreak") to
            listOf(ShayariCategory.DARD.id, ShayariCategory.BEWAFAI.id),
        listOf("बेवफ़ाई", "बेवफाई", "बेवफ़ा", "बेवफा", "धोखा", "जुदाई", "छल", "bewafai", "bewafa", "dhoka") to
            listOf(ShayariCategory.BEWAFAI.id, ShayariCategory.DARD.id),
        listOf("दर्द", "ग़म", "गम", "दुख", "पीड़ा", "dard", "gam", "sad", "pain") to
            listOf(ShayariCategory.DARD.id, ShayariCategory.AANSU.id),
        listOf("आंसू", "आँसू", "अश्क", "रोना", "नमी", "aansu", "tears") to
            listOf(ShayariCategory.AANSU.id, ShayariCategory.DARD.id),
        listOf("तन्हाई", "तन्हा", "तनहा", "अकेला", "अकेलापन", "ख़ामोशी", "खामोशी", "सन्नाटा", "tanhai", "alone") to
            listOf(ShayariCategory.TANHAI.id, ShayariCategory.YAADEIN.id),
        listOf("यादें", "याद", "इंतज़ार", "इंतजार", "yaadein", "yaad", "memories") to
            listOf(ShayariCategory.YAADEIN.id, ShayariCategory.TANHAI.id),
        listOf("मोहब्बत", "इश्क़", "इश्क", "प्यार", "चाहत", "दिल की बात", "mohabbat", "ishq", "pyar", "love") to
            listOf(ShayariCategory.MOHABBAT.id, ShayariCategory.ROMANTIC.id),
        listOf("रोमांटिक", "रोमांस", "नज़दीकियाँ", "romantic", "romance") to
            listOf(ShayariCategory.ROMANTIC.id, ShayariCategory.MOHABBAT.id),
        listOf("शराबी", "शराब", "मैख़ाना", "मयख़ाना", "जाम", "साकी", "साक़ी", "sharabi", "maikhana") to
            listOf(ShayariCategory.SHARABI.id, ShayariCategory.DARD.id),
        listOf("ख़ुदा", "खुदा", "रब", "दुआ", "इबादत", "सजदा", "khuda", "rab", "dua") to
            listOf(ShayariCategory.KHUDA_RAB.id),
        listOf("मौत", "जनाज़ा", "जनाजा", "कफ़न", "कफन", "maut", "janaza") to
            listOf(ShayariCategory.MAUT_JANAZA.id, ShayariCategory.DARD.id),
        listOf("दोस्ती", "दोस्त", "यारी", "यार", "dosti", "yaari", "friend") to
            listOf(ShayariCategory.DOSTI.id),
        listOf("हौसला", "मोटिवेशन", "उम्मीद", "जीत", "कामयाबी", "hausla", "motivation") to
            listOf(ShayariCategory.HAUSLA.id, ShayariCategory.ZINDAGI.id),
        listOf("ज़िंदगी", "जिंदगी", "जीवन", "सुकून", "zindagi", "life") to
            listOf(ShayariCategory.ZINDAGI.id),
        listOf("मुसाफ़िर", "मुसाफिर", "सफ़र", "सफर", "मंज़िल", "रास्ते", "musafir", "safar") to
            listOf(ShayariCategory.MUSAFIR.id, ShayariCategory.ZINDAGI.id),
        listOf("वक़्त", "वक्त", "समय", "सब्र", "waqt", "time") to
            listOf(ShayariCategory.WAQT.id, ShayariCategory.ZINDAGI.id),
        listOf("अंदाज़", "तेवर", "रॉयल", "स्वाभिमान", "attitude", "royal") to
            listOf(ShayariCategory.ATTITUDE.id),
        listOf("तारीफ़", "तारीफ", "खूबसूरती", "हुस्न", "चाँद", "tareef", "beauty") to
            listOf(ShayariCategory.TAREEF.id, ShayariCategory.ROMANTIC.id),
        listOf("आईना", "आइना", "अक्स", "aaina", "mirror") to
            listOf(ShayariCategory.AAINA.id),
        listOf("प्रकृति", "बारिश", "सावन", "चाँदनी", "prakriti", "rain", "nature") to
            listOf(ShayariCategory.PRAKRITI.id),
        listOf("खुशी", "मुस्कान", "हँसी", "khushi", "happy", "smile") to
            listOf(ShayariCategory.KHUSHI.id),
        listOf("परिवार", "माँ", "पापा", "घर", "parivar", "family") to
            listOf(ShayariCategory.PARIVAR.id),
        listOf("देशप्रेम", "वतन", "भारत", "तिरंगा", "deshprem", "india") to
            listOf(ShayariCategory.DESHPREM.id)
    )

    fun parseTags(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val seen = LinkedHashSet<String>()
        raw.split(TAG_SPLIT_REGEX).forEach { part ->
            val clean = part.trim().removePrefix("#").trim()
            if (clean.isNotEmpty() && clean != "मेरी शायरी") {
                seen.add(clean.take(24))
            }
        }
        return seen.toList()
    }

    fun formatTagsStorage(tags: List<String>, maxLen: Int = 100): String {
        val unique = tags.mapNotNull { t ->
            t.trim().removePrefix("#").trim().take(24).takeIf { it.isNotEmpty() }
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

    fun toggleTagInString(currentRaw: String, tagToToggle: String, maxLen: Int = 100): String {
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

    fun addTagToString(currentRaw: String, tagToAdd: String, maxLen: Int = 100): String {
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
            val lower = tag.lowercase()
            for ((keywords, catIds) in KEYWORD_TO_CATEGORY_IDS) {
                if (keywords.any { kw -> lower.contains(kw) || kw.contains(lower) }) {
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
            val lower = tag.lowercase()
            ShayariCategory.entries.forEach { cat ->
                if (cat.titleHi.equals(tag, ignoreCase = true) ||
                    cat.id.equals(tag, ignoreCase = true) ||
                    cat.labelEn.equals(tag, ignoreCase = true)
                ) {
                    result.add(cat.id)
                }
            }
            for ((keywords, catIds) in KEYWORD_TO_CATEGORY_IDS) {
                if (keywords.any { kw -> lower.contains(kw) || (lower.length >= 3 && kw.contains(lower)) }) {
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
