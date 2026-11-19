package com.example.util

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SocialSharePlatform(
    val id: String,
    val labelRes: Int,
    val packageNames: List<String>,
    val prefersImageCard: Boolean = false
) {
    WHATSAPP(
        id = "whatsapp",
        labelRes = R.string.share_platform_whatsapp,
        packageNames = listOf("com.whatsapp", "com.whatsapp.w4b")
    ),
    INSTAGRAM(
        id = "instagram",
        labelRes = R.string.share_platform_instagram,
        packageNames = listOf("com.instagram.android"),
        prefersImageCard = true
    ),
    FACEBOOK(
        id = "facebook",
        labelRes = R.string.share_platform_facebook,
        packageNames = listOf("com.facebook.katana", "com.facebook.lite")
    ),
    TWITTER(
        id = "twitter",
        labelRes = R.string.share_platform_twitter,
        packageNames = listOf("com.twitter.android")
    ),
    THREADS(
        id = "threads",
        labelRes = R.string.share_platform_threads,
        packageNames = listOf("com.instagram.barcelona")
    ),
    TELEGRAM(
        id = "telegram",
        labelRes = R.string.share_platform_telegram,
        packageNames = listOf("org.telegram.messenger", "org.telegram.messenger.web")
    ),
    SIGNAL(
        id = "signal",
        labelRes = R.string.share_platform_signal,
        packageNames = listOf("org.thoughtcrime.securesms")
    )
}

enum class SocialShareResult {
    LAUNCHED_NATIVE_APP,
    LAUNCHED_WEB_FALLBACK,
    LAUNCHED_CHOOSER_FALLBACK,
    FAILED
}

object SocialShareLauncher {

    const val LIVE_WEBSITE_URL = "https://princeorviel.github.io/Shayri/"

    fun buildTargetedAppIntent(
        context: Context,
        packageName: String,
        text: String,
        previewTitle: String,
        imageUri: Uri? = null
    ): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            setPackage(packageName)
            if (imageUri != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, previewTitle)
                clipData = ClipData.newUri(context.contentResolver, previewTitle, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, previewTitle)
            }
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun buildWebShareUrl(
        platform: SocialSharePlatform,
        text: String,
        shareUrl: String = LIVE_WEBSITE_URL
    ): String? {
        val encodedText = Uri.encode(text)
        val encodedUrl = Uri.encode(shareUrl)
        return when (platform) {
            SocialSharePlatform.WHATSAPP -> "https://api.whatsapp.com/send?text=$encodedText"
            SocialSharePlatform.FACEBOOK -> "https://www.facebook.com/sharer/sharer.php?u=$encodedUrl&quote=$encodedText"
            SocialSharePlatform.TWITTER -> "https://twitter.com/intent/tweet?text=$encodedText"
            SocialSharePlatform.THREADS -> "https://www.threads.net/intent/post?text=$encodedText"
            SocialSharePlatform.TELEGRAM -> "https://t.me/share/url?url=$encodedUrl&text=$encodedText"
            SocialSharePlatform.INSTAGRAM -> null
            SocialSharePlatform.SIGNAL -> null
        }
    }

    fun shareToPlatform(
        context: Context,
        platform: SocialSharePlatform,
        text: String,
        previewTitle: String,
        imageUri: Uri? = null,
        shareUrl: String = LIVE_WEBSITE_URL
    ): SocialShareResult {
        val pm = context.packageManager
        for (pkg in platform.packageNames) {
            val intent = buildTargetedAppIntent(
                context = context,
                packageName = pkg,
                text = text,
                previewTitle = previewTitle,
                imageUri = imageUri
            )
            if (intent.resolveActivity(pm) != null) {
                if (imageUri != null) {
                    runCatching {
                        context.grantUriPermission(
                            pkg,
                            imageUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }
                val launched = runCatching {
                    context.startActivity(intent)
                    true
                }.getOrDefault(false)
                if (launched) return SocialShareResult.LAUNCHED_NATIVE_APP
            }
        }

        val webUrl = buildWebShareUrl(platform, text, shareUrl)
        if (webUrl != null) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            val webLaunched = runCatching {
                context.startActivity(webIntent)
                true
            }.getOrDefault(false)
            if (webLaunched) return SocialShareResult.LAUNCHED_WEB_FALLBACK
        }

        // Copy text so user can paste cleanly in apps like Instagram/Signal if not directly resolved
        ShayariShareHelper.copyPlainText(context, text)
        val fallbackLaunched = if (imageUri != null) {
            runCatching {
                context.startActivity(
                    ShayariShareHelper.buildShareImageChooserIntent(
                        context = context,
                        imageUri = imageUri,
                        text = text,
                        previewTitle = previewTitle
                    )
                )
                true
            }.getOrDefault(false)
        } else {
            ShayariShareHelper.sharePlainText(
                context = context,
                text = text,
                previewTitle = previewTitle
            )
        }
        return if (fallbackLaunched) {
            SocialShareResult.LAUNCHED_CHOOSER_FALLBACK
        } else {
            SocialShareResult.FAILED
        }
    }

    suspend fun createAppPromoImageUri(context: Context): Uri = withContext(Dispatchers.IO) {
        val bitmap = renderAppPromoCardBitmap(context)
        try {
            val cardsDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
            val outFile = File(cardsDir, "shayari_app_promo_card.png")
            FileOutputStream(outFile).use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    "Failed to compress app promo bitmap"
                }
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun saveAppPromoCardToGallery(context: Context): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = renderAppPromoCardBitmap(context)
            val fileName = "shayari_app_card_${System.currentTimeMillis()}.png"
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/Shayari"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: error("Unable to create MediaStore entry for app promo card")
                    try {
                        resolver.openOutputStream(uri)?.use { out ->
                            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                                "Failed to compress app promo bitmap"
                            }
                        } ?: error("Unable to open MediaStore output stream")
                        val complete = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        resolver.update(uri, complete, null, null)
                        uri
                    } catch (t: Throwable) {
                        resolver.delete(uri, null, null)
                        throw t
                    }
                } else {
                    val dir = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                            ?: File(context.filesDir, "shayari_images"),
                        "Shayari"
                    ).apply { mkdirs() }
                    val outFile = File(dir, fileName)
                    FileOutputStream(outFile).use { out ->
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                            "Failed to compress app promo bitmap"
                        }
                    }
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(outFile.absolutePath),
                        arrayOf("image/png"),
                        null
                    )
                    Uri.fromFile(outFile)
                }
            } finally {
                bitmap.recycle()
            }
        }
    }

    fun renderAppPromoCardBitmap(context: Context): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                Color.parseColor("#2A1025"),
                Color.parseColor("#0E0814"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val gold = Color.parseColor("#F0C97D")
        val cream = Color.parseColor("#FFF8F0")
        val muted = Color.parseColor("#D8C6D6")

        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            alpha = 150
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawRoundRect(RectF(56f, 56f, width - 56f, height - 56f), 48f, 48f, framePaint)

        val displayTypeface = ResourcesCompat.getFont(context, R.font.rozha_one) ?: Typeface.SERIF
        val serifTypeface = ResourcesCompat.getFont(context, R.font.noto_serif_devanagari) ?: Typeface.SERIF
        val uiTypeface = ResourcesCompat.getFont(context, R.font.poppins) ?: Typeface.SANS_SERIF

        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            textSize = 36f
            typeface = uiTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✦  हिंदी व उर्दू शायरी ऐप  ✦", width / 2f, 220f, badgePaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            textSize = 144f
            typeface = displayTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.app_name), width / 2f, 440f, titlePaint)

        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cream
            textSize = 48f
            typeface = serifTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.app_subtitle), width / 2f, 535f, subtitlePaint)

        val desc = "मोहब्बत • दर्द • ज़िंदगी • दोस्ती • हौसला\n\nReels स्टाइल स्वाइप करें, अपनी शायरी लिखें और सुंदर फ़ोटो कार्ड बनाएँ।"
        val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 40f
            typeface = serifTypeface
        }
        val descLayout = StaticLayout.Builder
            .obtain(desc, 0, desc.length, descPaint, width - 220)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(18f, 1.2f)
            .build()
        canvas.save()
        canvas.translate(110f, 670f)
        descLayout.draw(canvas)
        canvas.restore()

        val ctaBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
        }
        val pillRect = RectF(160f, 1040f, width - 160f, 1140f)
        canvas.drawRoundRect(pillRect, 50f, 50f, ctaBgPaint)

        val ctaTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#201208")
            textSize = 34f
            typeface = Typeface.create(uiTypeface, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("princeorviel.github.io/Shayri", width / 2f, 1102f, ctaTextPaint)

        return bitmap
    }
}
