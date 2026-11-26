package com.example.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoSingerDetectorTest {

    @Test
    fun `detects Kishore Kumar and other playback singers from Hindi and English text`() {
        assertEquals(
            "किशोर कुमार",
            VideoSingerDetector.extractKnownSingerFromText("पल पल दिल के पास - Kishore Kumar evergreen song")
        )
        assertEquals(
            "किशोर कुमार",
            VideoSingerDetector.extractKnownSingerFromText("किशोर कुमार का सदाबहार गाना #kishorekumar")
        )
        assertEquals(
            "अरिजीत सिंह",
            VideoSingerDetector.extractKnownSingerFromText("Tum Hi Ho by Arijit Singh")
        )
        assertEquals(
            "लता मंगेशकर",
            VideoSingerDetector.extractKnownSingerFromText("लता मंगेशकर की सुरीली आवाज़")
        )
    }

    @Test
    fun `extracts explicit singer label and blocks uploader name like Prince`() {
        assertEquals(
            "जगजीत सिंह",
            VideoSingerDetector.extractKnownSingerFromText("Singer: Jagjit Singh | Gazal", uploaderName = "Prince")
        )
        assertEquals(
            "रफ़ी साहब",
            VideoSingerDetector.extractKnownSingerFromText("गायक: रफ़ी साहब", uploaderName = "Prince")
        )
        assertNull(
            VideoSingerDetector.sanitizeSingerName("Prince", uploaderName = "Prince")
        )
        assertNull(
            VideoSingerDetector.sanitizeSingerName("Prince Orviel", uploaderName = "Prince Orviel")
        )
        assertNull(
            VideoSingerDetector.sanitizeSingerName("Original Audio", uploaderName = "Prince")
        )
    }
}
