package com.example.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HinglishTransliteratorTest {

    @Test
    fun `transliterateWord maps shayari lexicon accurately`() {
        assertEquals("दिल", HinglishTransliterator.transliterateWord("dil"))
        assertEquals("मोहब्बत", HinglishTransliterator.transliterateWord("mohabbat"))
        assertEquals("ज़िंदगी", HinglishTransliterator.transliterateWord("zindagi"))
        assertEquals("इश्क़", HinglishTransliterator.transliterateWord("ishq"))
        assertEquals("ख़ामोशी", HinglishTransliterator.transliterateWord("khamoshi"))
    }

    @Test
    fun `transliterateText converts Hinglish sentences while preserving newlines and Hindi`() {
        val converted = HinglishTransliterator.transliterateText("dil ki baat\naur पुरानी यादें")
        assertEquals("दिल की बात\nऔर पुरानी यादें", converted)
    }

    @Test
    fun `applySpaceOrNewlineAutoConvert converts trailing Latin word on space`() {
        val prev = TextFieldValue("mera dil", TextRange(8))
        val incoming = TextFieldValue("mera dil ", TextRange(9))
        val result = HinglishTransliterator.applySpaceOrNewlineAutoConvert(prev, incoming)
        assertEquals("mera दिल ", result.text)
        assertEquals(TextRange("mera दिल ".length), result.selection)
    }

    @Test
    fun `suggestCandidates provides dictionary prefix and exact matches`() {
        val suggestions = HinglishTransliterator.suggestCandidates("mohab")
        assertTrue(suggestions.contains("मोहब्बत"))

        val dilSuggestions = HinglishTransliterator.suggestCandidates("dil")
        assertEquals("दिल", dilSuggestions.first())
    }

    @Test
    fun `replaceActiveWord and insertAtCursor preserve surrounding text`() {
        val field = TextFieldValue("आज दिल ", TextRange(7))
        val withWord = HinglishTransliterator.replaceActiveWord(
            TextFieldValue("आज dil", TextRange(6)),
            "दिल",
            appendSpace = true
        )
        assertEquals("आज दिल ", withWord.text)

        val withNewline = HinglishTransliterator.insertAtCursor(field, "\n", padWithSpace = false)
        assertEquals("आज दिल \n", withNewline.text)
        assertFalse(HinglishTransliterator.hasConvertibleHinglish("आज दिल"))
        assertTrue(HinglishTransliterator.hasConvertibleHinglish("dil"))
    }
}
