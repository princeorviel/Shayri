package com.example.ai

import com.example.data.model.ShayariCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShayarVoiceEngineTest {

    @Test
    fun heartbreakPromptUsesHumanDirectorNotesAndEmotionalTagsWithoutChoppingLines() {
        val couplet = "तेरे जाने के बाद ख़ामोशी बढ़ गई है\nदिल के हर कोने में तन्हाई भर गई है"
        val prompt = ShayarVoiceEngine.buildPoeticRecitationPrompt(
            couplet = couplet,
            persona = ShayarVoicePersona.DARD_SHAYARA,
            categoryId = ShayariCategory.DARD.id
        )

        assertTrue(prompt.contains("# AUDIO PROFILE"))
        assertTrue(prompt.contains("# DIRECTOR'S NOTES"))
        assertTrue(prompt.contains("# TRANSCRIPT"))
        assertTrue(prompt.contains("[softly, heartbroken]"))
        assertTrue(prompt.contains("[sigh] [sadly, voice trembling]"))
        assertFalse("Short/medium lines must not be artificially chopped with '...'", prompt.contains("..."))
    }

    @Test
    fun romanticAttitudeAndWittyPromptsProduceDistinctExpressiveCues() {
        val romanticPrompt = ShayarVoiceEngine.buildPoeticRecitationPrompt(
            couplet = "तुम पास हो तो शामें गुलज़ार लगती हैं\nतुम्हारी एक मुस्कान ही प्यार लगती है",
            persona = ShayarVoicePersona.ROMANTIC_SHAYARA,
            categoryId = ShayariCategory.MOHABBAT.id
        )
        assertTrue(romanticPrompt.contains("[warmly, intimately]"))
        assertTrue(romanticPrompt.contains("[whispering, tenderly]"))

        val attitudePrompt = ShayarVoiceEngine.buildPoeticRecitationPrompt(
            couplet = "हमारा अंदाज़ ही हमारी पहचान है\nझुकना हमारी फ़ितरत में नहीं जनाब",
            persona = ShayarVoicePersona.JOSH_SHAYAR,
            categoryId = ShayariCategory.ATTITUDE.id
        )
        assertTrue(attitudePrompt.contains("[calmly, confident]"))
        assertTrue(attitudePrompt.contains("[firm, commanding]"))

        val wittyPrompt = ShayarVoiceEngine.buildPoeticRecitationPrompt(
            couplet = "दोस्त वो जो उधार लेकर भूल जाए\nऔर पार्टी में सबसे आगे लाइन लगाए",
            persona = ShayarVoicePersona.SUREELI_SHAYARA,
            categoryId = ShayariCategory.DOSTI.id
        )
        assertTrue(wittyPrompt.contains("[chuckle]"))
    }

    @Test
    fun adjacentReelsPickContrastingVoicePersonas() {
        val first = ShayarVoicePersona.recommendedForReel(
            categoryId = ShayariCategory.DARD.id,
            couplet = "दिल में दर्द है बहुत",
            pageIndex = 0,
            stableKey = "s1"
        )
        val second = ShayarVoicePersona.recommendedForReel(
            categoryId = ShayariCategory.DARD.id,
            couplet = "आँसू थमते नहीं आज",
            pageIndex = 1,
            stableKey = "s2"
        )
        assertNotEquals(first, second)
    }
}
