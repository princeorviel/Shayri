package com.example.ai

import com.example.data.model.ShayariCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShayarVoiceEngineTest {

    @Test
    fun bewafaiCategoryAndTagTriggerBetrayedShayarReaction() {
        val couplet = "तुमने वादे किए थे उम्र भर साथ चलने के\nऔर रास्तों में ही हाथ छोड़ दिया हमारा"
        val emotionByCategory = ShayarVoiceEngine.detectCoupletEmotion(
            couplet = couplet,
            categoryId = ShayariCategory.BEWAFAI.id
        )
        assertEquals(CoupletEmotion.BEWAFAI_BETRAYAL, emotionByCategory)

        // Even if category is generic/empty, an explicit #बेवफाई tag must take highest priority
        val emotionByTag = ShayarVoiceEngine.detectCoupletEmotion(
            couplet = couplet,
            categoryId = ShayariCategory.MOHABBAT.id,
            tags = listOf("बेवफाई", "धोखा")
        )
        assertEquals(CoupletEmotion.BEWAFAI_BETRAYAL, emotionByTag)

        val prompt = ShayarVoiceEngine.buildPoeticRecitationPrompt(
            couplet = couplet,
            persona = ShayarVoicePersona.DARD_SHAYAR,
            categoryId = ShayariCategory.BEWAFAI.id,
            tags = listOf("बेवफाई")
        )

        assertFalse("Prompt must not use Markdown '#' headers", prompt.contains("# AUDIO PROFILE"))
        assertFalse("Prompt must not use Markdown '#' headers", prompt.contains("# TRANSCRIPT"))
        assertTrue(prompt.contains("bewafai"))
        assertTrue(prompt.contains("[bitterly hurt, wounded]"))
        assertTrue(prompt.contains("[heavy sigh] [deeply betrayed, voice breaking with hurt]"))
        assertFalse("Short/medium lines must not be artificially chopped with '...'", prompt.contains("..."))
    }

    @Test
    fun dardCategoryAndTagTriggerTearfulPainReaction() {
        val couplet = "तेरे जाने के बाद ख़ामोशी बढ़ गई है\nदिल के हर कोने में तन्हाई भर गई है"
        val emotion = ShayarVoiceEngine.detectCoupletEmotion(
            couplet = couplet,
            categoryId = ShayariCategory.DARD.id
        )
        assertEquals(CoupletEmotion.DARD_TEARFUL, emotion)

        val prompt = ShayarVoiceEngine.buildPoeticRecitationPrompt(
            couplet = couplet,
            persona = ShayarVoicePersona.DARD_SHAYARA,
            categoryId = ShayariCategory.DARD.id,
            tags = listOf("दर्द")
        )

        assertTrue(prompt.contains("[softly, aching with pain]"))
        assertTrue(prompt.contains("[sigh] [crying softly, voice trembling]"))
        assertFalse("Short/medium lines must not be artificially chopped with '...'", prompt.contains("..."))
    }

    @Test
    fun romanticSharabiAttitudeAndDostiPromptsProduceDistinctExpressiveCues() {
        val romanticPrompt = ShayarVoiceEngine.buildPoeticRecitationPrompt(
            couplet = "तुम पास हो तो शामें गुलज़ार लगती हैं\nतुम्हारी एक मुस्कान ही प्यार लगती है",
            persona = ShayarVoicePersona.ROMANTIC_SHAYARA,
            categoryId = ShayariCategory.MOHABBAT.id
        )
        assertTrue(romanticPrompt.contains("[warmly, deeply in love]"))
        assertTrue(romanticPrompt.contains("[soft smile] [whispering, adoringly]"))

        val sharabiPrompt = ShayarVoiceEngine.buildPoeticRecitationPrompt(
            couplet = "आज पैमाना भर दे साक़ी ग़म भुलाना है\nमैख़ाने में ही आज रात गुज़ारना है",
            persona = ShayarVoicePersona.MEHFIL_SHAYAR,
            categoryId = ShayariCategory.SHARABI.id
        )
        assertTrue(sharabiPrompt.contains("[husky, slightly tipsy, wry smile]"))
        assertTrue(sharabiPrompt.contains("[soft laugh] [sigh] [raspy, painfully honest]"))

        val attitudePrompt = ShayarVoiceEngine.buildPoeticRecitationPrompt(
            couplet = "हमारा अंदाज़ ही हमारी पहचान है\nझुकना हमारी फ़ितरत में नहीं जनाब",
            persona = ShayarVoicePersona.JOSH_SHAYAR,
            categoryId = ShayariCategory.ATTITUDE.id
        )
        assertTrue(attitudePrompt.contains("[calmly, arrogant smirk]"))
        assertTrue(attitudePrompt.contains("[firm, fierce commanding authority]"))

        val dostiPrompt = ShayarVoiceEngine.buildPoeticRecitationPrompt(
            couplet = "दोस्त वो जो उधार लेकर भूल जाए\nऔर पार्टी में सबसे आगे लाइन लगाए",
            persona = ShayarVoicePersona.SUREELI_SHAYARA,
            categoryId = ShayariCategory.DOSTI.id
        )
        assertTrue(dostiPrompt.contains("[warm chuckle]"))

        val comedyPrompt = ShayarVoiceEngine.buildPoeticRecitationPrompt(
            couplet = "मोटू बोला समोसे के बिना दिमाग़ नहीं चलता\nपतलू बोला फिर तो तेरा दिमाग़ रोज़ हलवाई के यहाँ चलता है",
            persona = ShayarVoicePersona.TADAP_AASHIQ,
            categoryId = ShayariCategory.MOTU_PATLU.id
        )
        assertTrue(comedyPrompt.contains("[chuckle] [laughing playfully]"))
    }

    @Test
    fun adjacentReelsStrictlyAlternateGenderAndContrastingVoicePersonas() {
        val categories = listOf(
            ShayariCategory.BEWAFAI.id,
            ShayariCategory.DARD.id,
            ShayariCategory.ATTITUDE.id,
            ShayariCategory.MOHABBAT.id
        )
        val personas = categories.mapIndexed { idx, catId ->
            ShayarVoicePersona.recommendedForReel(
                categoryId = catId,
                couplet = "शायरी लाइन $idx",
                pageIndex = idx,
                stableKey = "reel_$idx"
            )
        }

        for (i in 0 until personas.lastIndex) {
            assertNotEquals("Consecutive Reels must never repeat the same persona", personas[i], personas[i + 1])
            assertNotEquals(
                "Consecutive Reels must alternate Female <-> Male for unmistakable voice variety",
                personas[i].isFemale,
                personas[i + 1].isFemale
            )
            assertNotEquals(
                "Consecutive Reels must use different Gemini voice models",
                personas[i].geminiVoiceName,
                personas[i + 1].geminiVoiceName
            )
        }
    }
}
