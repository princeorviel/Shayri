package com.example.util

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PersonalShayari
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.toShayariEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShayariShareHelperTest {

    @Test
    fun `buildShareTextChooserIntent wraps ACTION_SEND text_plain with chooser metadata`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shayari = ShayariEntity(
            id = 1,
            couplet = "कुछ बातें अनकही ही अच्छी लगती हैं,\nकुछ यादें दिल में ही सच्ची लगती हैं।",
            poet = "गुलज़ार",
            categoryId = ShayariCategory.YAADEIN.id,
            moodTag = "मीठी यादें"
        )

        val formatted = ShayariShareHelper.formatShareText(shayari)
        val previewTitle = "${shayari.poet} • ${ShayariCategory.YAADEIN.titleHi}"
        val chooserIntent = ShayariShareHelper.buildShareTextChooserIntent(
            context = context,
            text = formatted,
            previewTitle = previewTitle
        )

        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.action)
        assertEquals(previewTitle, chooserIntent.getStringExtra(Intent.EXTRA_TITLE))

        @Suppress("DEPRECATION")
        val targetIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(targetIntent)
        assertEquals(Intent.ACTION_SEND, targetIntent?.action)
        assertEquals("text/plain", targetIntent?.type)
        val sharedText = targetIntent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(sharedText.contains(shayari.couplet))
        assertTrue(sharedText.contains(shayari.poet))
        assertTrue(sharedText.contains("#शायरी"))
    }

    @Test
    fun `formatPersonalShareText preserves title, custom theme tag, and user author signature`() {
        val item = PersonalShayari(
            id = "p1",
            userId = "u1",
            title = "शाम की तन्हाई",
            themeTag = "मीठा सुकून",
            couplet = "चाँदनी रात में हवाओं ने पैग़ाम दिया,\nदिल के सन्नाटे को एक नया नाम दिया।"
        )

        val text = ShayariShareHelper.formatPersonalShareText(item, "Prince Orviel")
        assertTrue(text.contains("✦ शाम की तन्हाई"))
        assertTrue(text.contains(item.couplet))
        assertTrue(text.contains("— Prince Orviel"))
        assertTrue(text.contains("#मीठा_सुकून"))
        assertTrue(text.contains("#शायरी"))
        org.junit.Assert.assertFalse(text.contains("मेरी कलम"))

        val entity = item.toShayariEntity(authorName = "Prince Orviel")
        assertEquals("Prince Orviel", entity.poet)

        val untitledGuest = item.copy(title = "").toShayariEntity(authorName = "")
        assertEquals("", untitledGuest.poet)
    }

    @Test
    fun `saveCardToGallery writes png image via MediaStore`() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shayari = ShayariEntity(
            id = 42,
            couplet = "इश्क़ ने 'ग़ालिब' निकम्मा कर दिया,\nवरना हम भी आदमी थे काम के।",
            poet = "मिर्ज़ा ग़ालिब",
            categoryId = ShayariCategory.MOHABBAT.id,
            moodTag = "प्यार"
        )

        val result = ShayariShareHelper.saveCardToGallery(
            context = context,
            shayari = shayari,
            style = com.example.data.model.CardStyle.NIGHT_MUSHAIRA,
            fontStyle = com.example.data.model.ShayariFontStyle.CLASSIC_SERIF
        )

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `buildShareImageChooserIntent wraps ACTION_SEND image_png with ClipData and chooser grant flags`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = android.net.Uri.parse("content://com.example.fileprovider/shared_cards/card.png")
        val previewTitle = "मिर्ज़ा ग़ालिब • मोहब्बत"

        val chooserIntent = ShayariShareHelper.buildShareImageChooserIntent(
            context = context,
            imageUri = uri,
            text = "इश्क़ ने 'ग़ालिब' निकम्मा कर दिया",
            previewTitle = previewTitle
        )

        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.action)
        assertEquals(previewTitle, chooserIntent.getStringExtra(Intent.EXTRA_TITLE))
        assertTrue((chooserIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)

        @Suppress("DEPRECATION")
        val targetIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(targetIntent)
        assertEquals(Intent.ACTION_SEND, targetIntent?.action)
        assertEquals("image/png", targetIntent?.type)
        assertEquals(uri, @Suppress("DEPRECATION") targetIntent?.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM))
        assertNotNull(targetIntent?.clipData)
        assertTrue(((targetIntent?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    }

    @Test
    fun `createShareImageUri writes PNG into shared_cards cache dir and returns FileProvider URI`() =
        kotlinx.coroutines.test.runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val shayari = ShayariEntity(
                id = -12345,
                couplet = "सफ़र में धूप तो होगी जो चल सको तो चलो,\nसभी हैं भीड़ में तुम भी निकल सको तो चलो।",
                poet = "निदा फ़ाज़ली",
                categoryId = ShayariCategory.ZINDAGI.id,
                moodTag = "सुकून"
            )

            val uri = ShayariShareHelper.createShareImageUri(
                context = context,
                shayari = shayari,
                style = com.example.data.model.CardStyle.NIGHT_MUSHAIRA,
                fontStyle = com.example.data.model.ShayariFontStyle.CLASSIC_SERIF,
                customBadgeLabel = "सुकून"
            )

            assertEquals("content", uri.scheme)
            assertTrue(uri.toString().contains("shared_cards"))
            val cacheDir = java.io.File(context.cacheDir, "shared_cards")
            val generatedFiles = cacheDir.listFiles().orEmpty()
            assertTrue(generatedFiles.any { it.name.endsWith(".png") && it.length() > 0L })
        }

    @Test
    fun `buildShareAppChooserIntent wraps ACTION_SEND text_plain with Play Store link`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val chooserIntent = ShayariShareHelper.buildShareAppChooserIntent(context)

        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.action)

        @Suppress("DEPRECATION")
        val targetIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(targetIntent)
        assertEquals(Intent.ACTION_SEND, targetIntent?.action)
        assertEquals("text/plain", targetIntent?.type)
        val sharedText = targetIntent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(sharedText.contains("https://play.google.com/store/apps/details?id=${context.packageName}"))
        org.junit.Assert.assertFalse(sharedText.contains("30,000"))
        org.junit.Assert.assertFalse(sharedText.contains("10,000"))
    }

    @Test
    fun `renderDesignedCardBitmap creates expected dimensions for solid and art studio wallpapers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val minimalSpec = com.example.ui.studio.ShayariDesignState(
            wallpaper = com.example.ui.studio.StudioWallpapers.minimalPapers.first(),
            scrimDimness = 0f,
            verticalAlign = com.example.ui.studio.StudioVerticalAlign.TOP
        )
        val bitmap = ShayariShareHelper.renderDesignedCardBitmap(
            context = context,
            couplet = "कुछ ख्वाब आँखों में ही रहने दो,\nहर बात ज़ुबां से कहनी ज़रूरी नहीं।",
            authorName = "Prince Orviel",
            title = "",
            tag = "ज़िंदगी",
            design = minimalSpec
        )
        assertEquals(1080, bitmap.width)
        assertEquals(1134, bitmap.height)
        bitmap.recycle()
    }

    @Test
    fun `appendAppPromoFooter attaches Play Store download link once without duplicating`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val base = "इश्क़ की बारिश में भीगते रहे हम\n\n— मिर्ज़ा ग़ालिब\n#शायरी"
        val withPromo = ShayariShareHelper.appendAppPromoFooter(context, base)
        val expectedUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"

        assertTrue(withPromo.contains(base))
        assertTrue(withPromo.contains(expectedUrl))
        // Calling twice should be idempotent
        assertEquals(withPromo, ShayariShareHelper.appendAppPromoFooter(context, withPromo))
    }

    @Test
    fun `parseDeepLink resolves read, category, and home deep links cleanly`() {
        val readTarget = ShayariShareHelper.parseDeepLink(android.net.Uri.parse("shayari://read/105"))
        assertEquals(ShayariShareHelper.DeepLinkTarget.ReadShayari(105), readTarget)

        val catTarget = ShayariShareHelper.parseDeepLink(android.net.Uri.parse("shayari://category/mohabbat"))
        assertEquals(ShayariShareHelper.DeepLinkTarget.OpenCategory(ShayariCategory.MOHABBAT.id), catTarget)

        val englishLabelTarget = ShayariShareHelper.parseDeepLink(android.net.Uri.parse("shayari://category/Hausla"))
        assertEquals(ShayariShareHelper.DeepLinkTarget.OpenCategory(ShayariCategory.HAUSLA.id), englishLabelTarget)

        val homeTarget = ShayariShareHelper.parseDeepLink(android.net.Uri.parse("shayari://home"))
        assertEquals(ShayariShareHelper.DeepLinkTarget.OpenHome, homeTarget)
    }
}
