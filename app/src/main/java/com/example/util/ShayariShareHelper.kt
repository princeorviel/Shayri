package com.example.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
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
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.CardStyle
import com.example.data.model.PersonalShayari
import com.example.data.model.ShayariCategory
import com.example.data.model.ShayariEntity
import com.example.data.model.ShayariFontStyle
import com.example.data.model.ShayariTagMatcher
import com.example.ui.studio.ShayariDesignState
import com.example.ui.studio.StudioCardTemplate
import com.example.ui.studio.StudioTextAlign
import com.example.ui.studio.StudioVerticalAlign
import com.example.ui.studio.StudioWallpaper
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShayariShareHelper {

    fun formatShareText(shayari: ShayariEntity): String {
        val category = ShayariCategory.fromId(shayari.categoryId)
        return "${shayari.couplet}\n\n— ${shayari.poet} (${category.titleHi})\n#शायरी"
    }

    fun formatPersonalShareText(item: PersonalShayari, authorName: String? = null): String {
        val header = item.title.trim().takeIf { it.isNotEmpty() }
        val cleanAuthor = authorName?.trim()?.takeIf { it.isNotEmpty() }
        val tag = item.themeTag.trim().takeIf { it.isNotEmpty() }
            ?.replace(Regex("\\s+"), "_")
        return buildString {
            if (header != null) {
                append("✦ ").append(header).append("\n\n")
            }
            append(item.couplet.trim())
            if (cleanAuthor != null) {
                append("\n\n— ").append(cleanAuthor)
            }
            append("\n\n")
            if (tag != null) {
                append("#").append(tag).append(" ")
            }
            append("#शायरी")
        }
    }

    fun buildShareTextChooserIntent(
        context: Context,
        text: String,
        previewTitle: String = context.getString(R.string.share_chooser_title)
    ): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, previewTitle)
        }
        return Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title)).apply {
            putExtra(Intent.EXTRA_TITLE, previewTitle)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun copyToClipboard(context: Context, shayari: ShayariEntity) {
        copyPlainText(context, formatShareText(shayari))
    }

    fun copyPlainText(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Shayari", text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareText(context: Context, shayari: ShayariEntity): Boolean {
        val category = ShayariCategory.fromId(shayari.categoryId)
        val previewTitle = "${shayari.poet} • ${category.titleHi}"
        return sharePlainText(
            context = context,
            text = formatShareText(shayari),
            previewTitle = previewTitle
        )
    }

    fun sharePlainText(
        context: Context,
        text: String,
        previewTitle: String = context.getString(R.string.share_chooser_title)
    ): Boolean {
        return runCatching {
            val chooser = buildShareTextChooserIntent(context, text, previewTitle)
            context.startActivity(chooser)
            true
        }.getOrElse { false }
    }

    fun playStoreUrl(context: Context): String =
        "https://play.google.com/store/apps/details?id=${context.packageName}"

    fun appendAppPromoFooter(context: Context, baseText: String): String {
        val clean = baseText.trimEnd()
        val storeUrl = playStoreUrl(context)
        if (clean.contains(storeUrl)) return clean
        val appName = context.getString(R.string.app_name)
        val footer = context.getString(R.string.share_viral_footer, appName, storeUrl)
        return "$clean\n\n$footer"
    }

    fun formatAppShareText(context: Context): String {
        val appName = context.getString(R.string.app_name)
        val storeUrl = playStoreUrl(context)
        return context.getString(R.string.share_app_message, appName, storeUrl)
    }

    fun buildShareAppChooserIntent(context: Context): Intent {
        val appName = context.getString(R.string.app_name)
        val subject = context.getString(R.string.share_app_subject, appName)
        val chooserTitle = context.getString(R.string.share_app_chooser_title)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, formatAppShareText(context))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        return Intent.createChooser(sendIntent, chooserTitle).apply {
            putExtra(Intent.EXTRA_TITLE, subject)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun shareApp(context: Context): Boolean {
        return runCatching {
            context.startActivity(buildShareAppChooserIntent(context))
            true
        }.getOrElse { false }
    }

    fun openPlayStoreForRating(context: Context): Boolean {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=${context.packageName}")
        ).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(playStoreUrl(context))
        ).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(marketIntent)
            true
        }.getOrElse {
            runCatching {
                context.startActivity(webIntent)
                true
            }.getOrElse { false }
        }
    }

    sealed class DeepLinkTarget {
        data class ReadShayari(val shayariId: Int) : DeepLinkTarget()
        data class OpenCategory(val categoryId: String) : DeepLinkTarget()
        data object OpenHome : DeepLinkTarget()
    }

    fun parseDeepLink(uri: Uri?): DeepLinkTarget? {
        if (uri == null || !uri.scheme.equals("shayari", ignoreCase = true)) return null
        val host = uri.host?.lowercase().orEmpty()
        val firstSegment = uri.pathSegments.firstOrNull()?.trim().orEmpty()
        return when (host) {
            "read" -> {
                val id = firstSegment.toIntOrNull()
                    ?: uri.getQueryParameter("id")?.toIntOrNull()
                if (id != null && id > 0) DeepLinkTarget.ReadShayari(id) else DeepLinkTarget.OpenHome
            }
            "category" -> {
                val catKey = firstSegment.ifBlank { uri.getQueryParameter("id").orEmpty() }.lowercase()
                val matched = ShayariCategory.entries.firstOrNull {
                    it.id.equals(catKey, ignoreCase = true) ||
                        it.labelEn.equals(catKey, ignoreCase = true)
                }
                if (matched != null) DeepLinkTarget.OpenCategory(matched.id) else DeepLinkTarget.OpenHome
            }
            "home" -> DeepLinkTarget.OpenHome
            else -> null
        }
    }

    fun buildShareImageChooserIntent(
        context: Context,
        imageUri: Uri,
        text: String,
        previewTitle: String = context.getString(R.string.share_chooser_title)
    ): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, previewTitle)
            clipData = ClipData.newUri(context.contentResolver, previewTitle, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title)).apply {
            putExtra(Intent.EXTRA_TITLE, previewTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    suspend fun createShareImageUri(
        context: Context,
        shayari: ShayariEntity,
        style: CardStyle,
        fontStyle: ShayariFontStyle = ShayariFontStyle.CLASSIC_SERIF,
        customBadgeLabel: String? = null
    ): Uri = withContext(Dispatchers.IO) {
        val bitmap = renderCardBitmap(context, shayari, style, fontStyle, customBadgeLabel)
        try {
            val cardsDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
            val safeId = shayari.id.toString().replace('-', 'n')
            val outFile = File(cardsDir, "shayari_card_${safeId}_${style.id}_${fontStyle.id}.png")
            FileOutputStream(outFile).use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    "Failed to compress shared card bitmap"
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

    suspend fun shareAsImageCard(
        context: Context,
        shayari: ShayariEntity,
        style: CardStyle,
        fontStyle: ShayariFontStyle = ShayariFontStyle.CLASSIC_SERIF,
        customText: String? = null,
        customBadgeLabel: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val uri = createShareImageUri(
                context = context,
                shayari = shayari,
                style = style,
                fontStyle = fontStyle,
                customBadgeLabel = customBadgeLabel
            )
            val category = ShayariCategory.fromId(shayari.categoryId)
            val previewTitle = listOfNotNull(
                shayari.poet.takeIf { it.isNotBlank() },
                customBadgeLabel?.takeIf { it.isNotBlank() } ?: category.titleHi
            ).joinToString(" • ").ifBlank { context.getString(R.string.share_chooser_title) }
            val shareText = customText ?: formatShareText(shayari)
            val chooser = buildShareImageChooserIntent(
                context = context,
                imageUri = uri,
                text = shareText,
                previewTitle = previewTitle
            )
            withContext(Dispatchers.Main) {
                context.startActivity(chooser)
            }
            true
        }.getOrElse { false }
    }

    suspend fun saveCardToGallery(
        context: Context,
        shayari: ShayariEntity,
        style: CardStyle,
        fontStyle: ShayariFontStyle = ShayariFontStyle.CLASSIC_SERIF
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = renderCardBitmap(context, shayari, style, fontStyle)
            val fileName = "shayari_${shayari.id}_${System.currentTimeMillis()}.png"
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
                        ?: error("Unable to create MediaStore image entry")
                    try {
                        resolver.openOutputStream(uri)?.use { out ->
                            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                                "Failed to compress bitmap"
                            }
                        } ?: error("Unable to open MediaStore output stream")

                        val completeValues = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        resolver.update(uri, completeValues, null, null)
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
                            "Failed to compress bitmap"
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

    private fun renderCardBitmap(
        context: Context,
        shayari: ShayariEntity,
        style: CardStyle,
        fontStyle: ShayariFontStyle,
        customBadgeLabel: String? = null
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                style.startColor.toArgb(),
                style.endColor.toArgb(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val category = ShayariCategory.fromId(shayari.categoryId)
        if (style.usesArtBackdrop) {
            val backdrop = BitmapFactory.decodeResource(context.resources, category.backgroundFor(shayari))
            if (backdrop != null) {
                val srcCrop = Rect(
                    (backdrop.width * 0.06f).toInt(),
                    (backdrop.height * 0.06f).toInt(),
                    (backdrop.width * 0.94f).toInt(),
                    (backdrop.height * 0.94f).toInt()
                )
                val dstRect = Rect(0, 0, width, height)
                canvas.drawBitmap(backdrop, srcCrop, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                backdrop.recycle()

                val scrimPaint = Paint().apply {
                    color = android.graphics.Color.argb(135, 16, 10, 24)
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
            }
        }

        val accentArgb = style.accentColor.toArgb()
        val textArgb = style.textColor.toArgb()

        // 2. Ornamental outer & inner frames
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            alpha = 140
            this.style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(
            RectF(56f, 56f, width - 56f, height - 56f),
            44f, 44f, framePaint
        )
        val innerFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            alpha = 65
            this.style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(
            RectF(74f, 74f, width - 74f, height - 74f),
            34f, 34f, innerFramePaint
        )

        val serifTypeface = ResourcesCompat.getFont(context, fontStyle.fontRes)
            ?: ResourcesCompat.getFont(context, R.font.noto_serif_devanagari)
            ?: Typeface.SERIF
        val displayTypeface = ResourcesCompat.getFont(context, R.font.rozha_one)
            ?: serifTypeface
        val uiTypeface = ResourcesCompat.getFont(context, R.font.poppins)
            ?: Typeface.SANS_SERIF

        // 3. Category pill at top
        val categoryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            textSize = 36f
            typeface = uiTypeface
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        val badgeLabel = customBadgeLabel?.takeIf { it.isNotBlank() } ?: category.titleHi
        canvas.drawText("✦  $badgeLabel  ✦", width / 2f, 165f, categoryPaint)

        // 4. Decorative quote mark
        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            alpha = 180
            textSize = 110f
            typeface = displayTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("❝", width / 2f, 310f, quotePaint)

        // 5. Couplet body centered vertically
        val coupletPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textArgb
            textSize = if (shayari.couplet.length > 130) 46f else 54f
            typeface = serifTypeface
        }
        val textWidth = width - 220
        val staticLayout = StaticLayout.Builder
            .obtain(shayari.couplet, 0, shayari.couplet.length, coupletPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(22f, 1.25f)
            .build()

        val contentCenterY = height / 2f + 10f
        val topY = contentCenterY - staticLayout.height / 2f
        canvas.save()
        canvas.translate(110f, topY)
        staticLayout.draw(canvas)
        canvas.restore()

        // 6. Divider + Poet attribution
        val dividerY = topY + staticLayout.height + 70f
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            alpha = 180
            strokeWidth = 3f
        }
        canvas.drawLine(width / 2f - 90f, dividerY, width / 2f + 90f, dividerY, dividerPaint)

        if (shayari.poet.isNotBlank()) {
            val poetPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentArgb
                textSize = 38f
                typeface = serifTypeface
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("— ${shayari.poet}", width / 2f, dividerY + 64f, poetPaint)
        }

        // 7. Subtle watermark footer
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textArgb
            alpha = 135
            textSize = 28f
            typeface = displayTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("शायरी • दिल की बात", width / 2f, height - 108f, footerPaint)

        return bitmap
    }

    suspend fun createDesignedCardImageUri(
        context: Context,
        couplet: String,
        authorName: String,
        title: String,
        tag: String,
        design: ShayariDesignState
    ): Uri = withContext(Dispatchers.IO) {
        val bitmap = renderDesignedCardBitmap(
            context = context,
            couplet = couplet,
            authorName = authorName,
            title = title,
            tag = tag,
            design = design
        )
        try {
            val cardsDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
            val outFile = File(cardsDir, "shayari_designed_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    "Failed to compress designed card bitmap"
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

    suspend fun shareDesignedCard(
        context: Context,
        couplet: String,
        authorName: String,
        title: String,
        tag: String,
        design: ShayariDesignState
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val uri = createDesignedCardImageUri(
                context = context,
                couplet = couplet,
                authorName = authorName,
                title = title,
                tag = tag,
                design = design
            )
            val signature = authorName.trim()
            val baseShareText = buildString {
                if (title.isNotBlank()) append("✦ ").append(title.trim()).append("\n\n")
                append(couplet.trim())
                if (signature.isNotBlank()) {
                    append("\n\n— ").append(signature)
                }
                if (tag.isNotBlank()) {
                    append("\n#").append(tag.trim().replace(Regex("\\s+"), "_"))
                }
                append(" #शायरी")
            }
            val shareText = appendAppPromoFooter(context, baseShareText)
            val chooser = buildShareImageChooserIntent(
                context = context,
                imageUri = uri,
                text = shareText,
                previewTitle = title.trim().ifBlank {
                    if (signature.isNotBlank()) "$signature • शायरी" else "शायरी"
                }
            )
            withContext(Dispatchers.Main) {
                context.startActivity(chooser)
            }
            true
        }.getOrElse { false }
    }

    suspend fun saveDesignedCardToGallery(
        context: Context,
        couplet: String,
        authorName: String,
        title: String,
        tag: String,
        design: ShayariDesignState
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = renderDesignedCardBitmap(
                context = context,
                couplet = couplet,
                authorName = authorName,
                title = title,
                tag = tag,
                design = design
            )
            val fileName = "shayari_studio_${System.currentTimeMillis()}.png"
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
                        ?: error("Unable to create MediaStore image entry")
                    try {
                        resolver.openOutputStream(uri)?.use { out ->
                            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                                "Failed to compress bitmap"
                            }
                        } ?: error("Unable to open MediaStore output stream")

                        val completeValues = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        resolver.update(uri, completeValues, null, null)
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
                            "Failed to compress bitmap"
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

    fun renderDesignedCardBitmap(
        context: Context,
        couplet: String,
        authorName: String,
        title: String,
        tag: String,
        design: ShayariDesignState
    ): Bitmap {
        val width = 1080
        val height = 1134
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (val wp = design.wallpaper) {
            is StudioWallpaper.SolidOrGradient -> {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        wp.startColor.toArgb(),
                        wp.endColor.toArgb(),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
                if (design.scrimDimness > 0.01f) {
                    val alpha = (design.scrimDimness * 0.7f * 255).toInt().coerceIn(0, 192)
                    canvas.drawColor(android.graphics.Color.argb(alpha, 0, 0, 0))
                }
            }

            is StudioWallpaper.DrawableArt -> {
                val decoded = BitmapFactory.decodeResource(context.resources, wp.resId)
                drawCenterCropBitmap(canvas, decoded, width, height)
                decoded?.recycle()
                val alpha = (design.scrimDimness.coerceIn(0f, 0.85f) * 255).toInt()
                canvas.drawColor(android.graphics.Color.argb(alpha, 0, 0, 0))
            }

            is StudioWallpaper.GalleryPhoto -> {
                val decoded = runCatching {
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(wp.uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, boundsOpts)
                    }
                    var sample = 1
                    while (boundsOpts.outWidth / sample > 2048 || boundsOpts.outHeight / sample > 2048) {
                        sample *= 2
                    }
                    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                    context.contentResolver.openInputStream(wp.uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                }.getOrNull()
                if (decoded != null) {
                    drawCenterCropBitmap(canvas, decoded, width, height)
                    decoded.recycle()
                } else {
                    canvas.drawColor(android.graphics.Color.rgb(24, 24, 32))
                }
                val alpha = (design.scrimDimness.coerceIn(0f, 0.85f) * 255).toInt()
                canvas.drawColor(android.graphics.Color.argb(alpha, 0, 0, 0))
            }
        }

        val accentArgb = design.accentColor.toArgb()
        when (design.cardTemplate) {
            StudioCardTemplate.CLASSIC_CLEAN -> Unit
            StudioCardTemplate.ROYAL_GOLD_FRAME -> {
                val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = accentArgb
                    alpha = 185
                    strokeWidth = 5f
                }
                val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = accentArgb
                    alpha = 95
                    strokeWidth = 2.5f
                }
                canvas.drawRoundRect(34f, 34f, width - 34f, height - 34f, 28f, 28f, outerBorder)
                canvas.drawRoundRect(52f, 52f, width - 52f, height - 52f, 20f, 20f, innerBorder)
            }
            StudioCardTemplate.EDITORIAL_NOTEBOOK -> {
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentArgb
                    alpha = 42
                    strokeWidth = 2.5f
                }
                var y = 140f
                while (y < height - 110f) {
                    canvas.drawLine(0f, y, width.toFloat(), y, rulePaint)
                    y += 82f
                }
                val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(85, 224, 96, 114)
                    strokeWidth = 4f
                }
                canvas.drawLine(96f, 0f, 96f, height.toFloat(), marginPaint)
            }
            StudioCardTemplate.MAGAZINE_STRIPE -> {
                val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentArgb
                    alpha = 230
                }
                canvas.drawRoundRect(36f, 96f, 54f, height - 96f, 9f, 9f, stripePaint)
            }
            StudioCardTemplate.GLASS_QUOTE -> {
                val glassBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(120, 12, 16, 28)
                }
                val glassStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = accentArgb
                    alpha = 140
                    strokeWidth = 3f
                }
                canvas.drawRoundRect(54f, 84f, width - 54f, height - 96f, 36f, 36f, glassBg)
                canvas.drawRoundRect(54f, 84f, width - 54f, height - 96f, 36f, 36f, glassStroke)
            }
            StudioCardTemplate.POLAROID_FRAME -> {
                val mattePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = android.graphics.Color.rgb(253, 250, 245)
                    strokeWidth = 56f
                }
                canvas.drawRect(28f, 28f, width - 28f, height - 28f, mattePaint)
            }
            StudioCardTemplate.VINTAGE_CORNERS -> {
                val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = accentArgb
                    alpha = 195
                    strokeWidth = 5f
                }
                val m = 48f
                val l = 74f
                canvas.drawLine(m, m, m + l, m, cornerPaint)
                canvas.drawLine(m, m, m, m + l, cornerPaint)
                canvas.drawLine(width - m, m, width - m - l, m, cornerPaint)
                canvas.drawLine(width - m, m, width - m, m + l, cornerPaint)
                canvas.drawLine(m, height - m, m + l, height - m, cornerPaint)
                canvas.drawLine(m, height - m, m, height - m - l, cornerPaint)
                canvas.drawLine(width - m, height - m, width - m - l, height - m, cornerPaint)
                canvas.drawLine(width - m, height - m, width - m, height - m - l, cornerPaint)
            }
        }

        val textArgb = design.textColor.toArgb()
        val typeface = ResourcesCompat.getFont(context, design.fontStyle.fontRes)
            ?: ResourcesCompat.getFont(context, R.font.noto_serif_devanagari)
            ?: Typeface.SERIF
        val uiTypeface = ResourcesCompat.getFont(context, R.font.poppins) ?: Typeface.SANS_SERIF

        val scaleFactor = width / 380f
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textArgb
            textSize = design.fontSizeSp * scaleFactor
            this.typeface = typeface
        }
        val cleanSignature = authorName.trim()
        val signatureText = if (cleanSignature.isNotBlank()) "— $cleanSignature" else ""
        val signaturePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textArgb
            alpha = 215
            textSize = (design.fontSizeSp * 0.72f).coerceIn(13f, 18f) * scaleFactor
            this.typeface = typeface
        }
        val formattedTags = ShayariTagMatcher.parseTags(tag)
            .take(3)
            .joinToString("  ") { "#$it" }
        val topLabel = listOfNotNull(
            title.trim().takeIf { it.isNotEmpty() },
            formattedTags.takeIf { it.isNotEmpty() }
        ).joinToString(" • ")
        val topLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textArgb
            alpha = 200
            textSize = 13f * scaleFactor
            this.typeface = uiTypeface
        }

        val horizontalPadding = 84f
        val textWidth = (width - horizontalPadding * 2).toInt()
        val alignment = when (design.textAlign) {
            StudioTextAlign.START -> Layout.Alignment.ALIGN_NORMAL
            StudioTextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            StudioTextAlign.END -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val cleanCouplet = couplet.ifBlank { "मेरी शायरी" }
        val bodyLayout = StaticLayout.Builder
            .obtain(cleanCouplet, 0, cleanCouplet.length, bodyPaint, textWidth)
            .setAlignment(alignment)
            .setLineSpacing(12f, design.fontStyle.lineHeightFactor / 1.45f)
            .build()

        val sigLayout = if (signatureText.isNotEmpty()) {
            StaticLayout.Builder
                .obtain(signatureText, 0, signatureText.length, signaturePaint, textWidth)
                .setAlignment(alignment)
                .build()
        } else {
            null
        }

        val topLayout = if (topLabel.isNotEmpty()) {
            StaticLayout.Builder
                .obtain(topLabel, 0, topLabel.length, topLabelPaint, textWidth)
                .setAlignment(alignment)
                .build()
        } else {
            null
        }

        val sigBlockHeight = sigLayout?.let { 40f + it.height } ?: 0f
        val totalBlockHeight = (topLayout?.height?.plus(30f) ?: 0f) + bodyLayout.height + sigBlockHeight
        val minY = 96f
        val maxY = height - 130f - totalBlockHeight
        val startY = when (design.verticalAlign) {
            StudioVerticalAlign.TOP -> minY
            StudioVerticalAlign.CENTER -> ((height - totalBlockHeight) / 2f).coerceIn(minY, maxOf(minY, maxY))
            StudioVerticalAlign.BOTTOM -> maxOf(minY, maxY)
        }

        var cursorY = startY
        if (topLayout != null) {
            canvas.save()
            canvas.translate(horizontalPadding, cursorY)
            topLayout.draw(canvas)
            canvas.restore()
            cursorY += topLayout.height + 30f
        }

        canvas.save()
        canvas.translate(horizontalPadding, cursorY)
        bodyLayout.draw(canvas)
        canvas.restore()
        cursorY += bodyLayout.height

        if (sigLayout != null) {
            cursorY += 40f
            canvas.save()
            canvas.translate(horizontalPadding, cursorY)
            sigLayout.draw(canvas)
            canvas.restore()
        }

        val watermarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textArgb
            alpha = 145
            textSize = 13f * scaleFactor
            this.typeface = uiTypeface
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(context.getString(R.string.app_name), 48f, height - 40f, watermarkPaint)

        return bitmap
    }

    private fun drawCenterCropBitmap(canvas: Canvas, src: Bitmap?, targetW: Int, targetH: Int) {
        if (src == null) return
        val srcRatio = src.width.toFloat() / src.height.toFloat()
        val dstRatio = targetW.toFloat() / targetH.toFloat()
        val srcRect = if (srcRatio > dstRatio) {
            val cropW = (src.height * dstRatio).toInt()
            val left = (src.width - cropW) / 2
            Rect(left, 0, left + cropW, src.height)
        } else {
            val cropH = (src.width / dstRatio).toInt()
            val top = (src.height - cropH) / 2
            Rect(0, top, src.width, top + cropH)
        }
        canvas.drawBitmap(src, srcRect, Rect(0, 0, targetW, targetH), Paint(Paint.FILTER_BITMAP_FLAG))
    }
}
