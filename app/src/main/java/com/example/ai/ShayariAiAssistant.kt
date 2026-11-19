package com.example.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

enum class ShayariAssistMode {
    RHYMES,
    COMPLETE_LINES
}

data class ShayariAiSuggestions(
    val hint: String,
    val rhymes: List<String>,
    val completions: List<String>
)

sealed interface ShayariAiUiState {
    data object Idle : ShayariAiUiState
    data class Loading(val mode: ShayariAssistMode) : ShayariAiUiState
    data class Success(
        val mode: ShayariAssistMode,
        val suggestions: ShayariAiSuggestions
    ) : ShayariAiUiState

    data class Error(
        val mode: ShayariAssistMode,
        val message: String
    ) : ShayariAiUiState
}

interface ShayariAiClient {
    suspend fun suggest(
        mode: ShayariAssistMode,
        draft: String,
        themeOrTitle: String
    ): ShayariAiSuggestions
}

class FirebaseShayariAiClient : ShayariAiClient {

    private val suggestionsSchema = Schema.obj(
        properties = mapOf(
            "hint" to Schema.string(
                description = "A concise 1-sentence Hindi tip about the Qafiya rhyme sound or poetic mood."
            ),
            "rhymes" to Schema.array(
                items = Schema.string(),
                description = "6 to 8 natural Hindi or Hindustani rhyming words (Qafiya) in Devanagari script."
            ),
            "completions" to Schema.array(
                items = Schema.string(),
                description = "3 poetic Hindi Shayari line completions or next lines in Devanagari script."
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.85f
                responseMimeType = "application/json"
                responseSchema = suggestionsSchema
            },
            systemInstruction = content {
                text(
                    """
                    You are a friendly Hindi and Hindustani Shayari buddy.
                    Help users write relatable, heartfelt Hindi Shayari in Devanagari script.
                    Use simple, natural everyday spoken Hindi and Urdu words (avoid heavy, bookish Sanskritized words).
                    Pay careful attention to Qafiya (rhyming words), Radif, rhythm, and emotion.
                    Keep all output words, hints, and lines strictly in simple Devanagari Hindi.
                    Avoid English explanations or transliteration unless the user explicitly wrote in English.
                    """.trimIndent()
                )
            }
        )
    }

    override suspend fun suggest(
        mode: ShayariAssistMode,
        draft: String,
        themeOrTitle: String
    ): ShayariAiSuggestions {
        val prompt = buildShayariPrompt(mode = mode, draft = draft, themeOrTitle = themeOrTitle)
        val response = withTimeout(60_000L) {
            model.generateContent(prompt)
        }
        val jsonText = response.text
            ?: throw IllegalStateException("AI से कोई जवाब नहीं मिला।")
        return parseShayariSuggestionsJson(jsonText)
    }
}

internal fun buildShayariPrompt(
    mode: ShayariAssistMode,
    draft: String,
    themeOrTitle: String
): String {
    val cleanDraft = draft.trim()
    val cleanTheme = themeOrTitle.trim().ifBlank { "प्यार और एहसास" }
    return when (mode) {
        ShayariAssistMode.RHYMES -> """
            यूज़र अपनी शायरी लिख रहा है।
            टॉपिक / टाइटल: "$cleanTheme"
            अभी लिखी लाइनें / शब्द:
            "${cleanDraft.ifBlank { cleanTheme }}"

            कृपया आम बोलचाल की आसान हिंदी में जवाब दें:
            1) "hint": यूज़र के आखिरी शब्दों या टॉपिक के हिसाब से तुकबंदी (क़ाफ़िया) का 1 छोटा आसान सुझाव दें।
            2) "rhymes": 8 आसान और खूबसूरत हिंदी/हिंदुस्तानी तुकबंदी शब्द दें जो आखिरी शब्द या इस शायरी से मेल खाते हों।
            3) "completions": उन शब्दों का इस्तेमाल करके 2 छोटी, असरदार अगली पंक्तियाँ (लाइनें) दें।
        """.trimIndent()

        ShayariAssistMode.COMPLETE_LINES -> """
            यूज़र अपनी शायरी लिख रहा है और उसे आगे की लाइन पूरी करनी है।
            टॉपिक / टाइटल: "$cleanTheme"
            अभी का ड्राफ़्ट:
            "${cleanDraft.ifBlank { "इस टॉपिक पर एक शुरुआती शेर" }}"

            कृपया आम बोलचाल की आसान हिंदी में जवाब दें:
            1) "hint": शेर की लय और भाव पर 1 छोटा आसान सुझाव दें।
            2) "completions": 3 अलग-अलग आसान और दिल को छूने वाली अगली पंक्तियाँ (लाइनें) दें जो यूज़र की शायरी को आगे बढ़ाएँ।
            3) "rhymes": उनसे जुड़े 4 आसान तुकबंदी शब्द भी दें।
        """.trimIndent()
    }
}

internal fun parseShayariSuggestionsJson(rawJson: String): ShayariAiSuggestions {
    val json = JSONObject(rawJson)
    val hint = json.optString("hint").trim()
    val rhymesArray = json.optJSONArray("rhymes")
    val completionsArray = json.optJSONArray("completions")

    val rhymes = buildList {
        if (rhymesArray != null) {
            for (i in 0 until rhymesArray.length()) {
                val item = rhymesArray.optString(i).trim()
                if (item.isNotEmpty() && !contains(item)) add(item)
            }
        }
    }

    val completions = buildList {
        if (completionsArray != null) {
            for (i in 0 until completionsArray.length()) {
                val item = completionsArray.optString(i).trim()
                if (item.isNotEmpty() && !contains(item)) add(item)
            }
        }
    }

    if (rhymes.isEmpty() && completions.isEmpty()) {
        throw IllegalStateException("AI से कोई काम का सुझाव नहीं मिल पाया। फिर से कोशिश करें।")
    }

    return ShayariAiSuggestions(
        hint = hint,
        rhymes = rhymes,
        completions = completions
    )
}

object ShayariDraftEditorHelper {
    fun appendRhymingWord(currentDraft: String, word: String, maxLength: Int): String {
        val cleanWord = word.trim()
        if (cleanWord.isEmpty()) return currentDraft
        val updated = when {
            currentDraft.isBlank() -> cleanWord
            currentDraft.last().isWhitespace() -> currentDraft + cleanWord
            else -> "$currentDraft $cleanWord"
        }
        return updated.take(maxLength)
    }

    fun appendLine(currentDraft: String, line: String, maxLength: Int): String {
        val cleanLine = line.trim()
        if (cleanLine.isEmpty()) return currentDraft
        val updated = when {
            currentDraft.isBlank() -> cleanLine
            currentDraft.endsWith("\n") -> currentDraft + cleanLine
            else -> "$currentDraft\n$cleanLine"
        }
        return updated.take(maxLength)
    }
}
