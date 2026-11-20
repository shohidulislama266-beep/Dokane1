package com.example.util

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.data.model.BusinessCardDraft
import com.example.data.model.BusinessCardFontPair
import com.example.data.model.BusinessCardLayout
import com.example.data.model.BusinessCardPalettes
import com.example.data.model.BusinessCardSide
import com.example.data.model.BusinessCardTextAlign
import com.example.data.model.PaletteColorRoles
import com.example.domain.BusinessCardPaletteEngine
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

data class CardExportResult(
    val file: File,
    val savedToPublicDownloads: Boolean
)

/**
 * Self-contained native Business Card renderer & exporter (no AI calls).
 *
 * - Uses bundled Google Fonts (.ttf in res/font) identically across Compose and Canvas/PdfDocument.
 * - Splits mixed Bangla + English text into Unicode script runs so Bangla letters use the
 *   Bangla-friendly font and Latin letters use the Latin display/body font seamlessly.
 * - Exports print-ready 300 DPI PNG (1050 x 600 px) and exact 3.5 x 2 inch PDF (252 x 144 pt).
 */
object BusinessCardRenderer {

    // Exact 3.5" x 2.0" print dimensions in PostScript points (72 pt/in)
    const val PDF_PAGE_WIDTH_PT = 252
    const val PDF_PAGE_HEIGHT_PT = 144

    // Exact 3.5" x 2.0" print dimensions at 300 DPI for crisp PNG export
    const val PNG_WIDTH_PX = BusinessCardLocalStore.EXPORT_PNG_WIDTH // 1050
    const val PNG_HEIGHT_PX = BusinessCardLocalStore.EXPORT_PNG_HEIGHT // 600

    private fun isBanglaChar(ch: Char): Boolean {
        val code = ch.code
        return code in 0x0980..0x09FF || code == 0x0964 || code == 0x0965
    }

    /**
     * Splits text into contiguous runs tagged with whether the run contains Bangla script.
     */
    fun splitScriptRuns(text: String): List<Pair<String, Boolean>> {
        if (text.isEmpty()) return emptyList()
        val runs = mutableListOf<Pair<String, Boolean>>()
        val current = StringBuilder()
        var currentIsBangla = isBanglaChar(text[0])

        for (ch in text) {
            val isBangla = if (
                ch.isWhitespace() ||
                ch == '\u200C' ||
                ch == '\u200D' ||
                ch in ".,:-•/|()[]!?@#&+'\"·"
            ) {
                currentIsBangla
            } else {
                isBanglaChar(ch)
            }
            if (isBangla != currentIsBangla && current.isNotEmpty()) {
                runs.add(current.toString() to currentIsBangla)
                current.clear()
                currentIsBangla = isBangla
            }
            current.append(ch)
        }
        if (current.isNotEmpty()) {
            runs.add(current.toString() to currentIsBangla)
        }
        return runs
    }

    fun composeFontFamilyForPair(pair: BusinessCardFontPair): Pair<FontFamily, FontFamily> {
        val bangla = FontFamily(
            Font(pair.banglaFontRes, FontWeight.Normal),
            Font(pair.banglaFontRes, FontWeight.Medium),
            Font(pair.banglaFontRes, FontWeight.SemiBold),
            Font(pair.banglaFontRes, FontWeight.Bold)
        )
        val latin = FontFamily(
            Font(pair.latinFontRes, FontWeight.Normal),
            Font(pair.latinFontRes, FontWeight.Medium),
            Font(pair.latinFontRes, FontWeight.SemiBold),
            Font(pair.latinFontRes, FontWeight.Bold)
        )
        return bangla to latin
    }

    fun buildComposeStyledText(
        text: String,
        fontPair: BusinessCardFontPair
    ): AnnotatedString {
        val (banglaFamily, latinFamily) = composeFontFamilyForPair(fontPair)
        val runs = splitScriptRuns(text)
        return buildAnnotatedString {
            runs.forEach { (segment, isBangla) ->
                withStyle(
                    SpanStyle(
                        fontFamily = if (isBangla) banglaFamily else latinFamily
                    )
                ) {
                    append(segment)
                }
            }
        }
    }

    private fun loadTypefaces(context: Context, pair: BusinessCardFontPair): Pair<Typeface, Typeface> {
        val bangla = runCatching {
            ResourcesCompat.getFont(context, pair.banglaFontRes)
        }.getOrNull() ?: Typeface.DEFAULT
        val latin = runCatching {
            ResourcesCompat.getFont(context, pair.latinFontRes)
        }.getOrNull() ?: Typeface.SANS_SERIF
        return bangla to latin
    }

    private class CustomTypefaceSpan(
        private val customTypeface: Typeface,
        private val isBold: Boolean = false
    ) : MetricAffectingSpan() {
        private fun apply(paint: Paint) {
            paint.typeface = if (isBold) {
                Typeface.create(customTypeface, Typeface.BOLD)
            } else {
                customTypeface
            }
            paint.isFakeBoldText = isBold && paint.typeface?.isBold != true
        }

        override fun updateDrawState(tp: TextPaint) = apply(tp)
        override fun updateMeasureState(tp: TextPaint) = apply(tp)
    }

    private fun buildSpannedText(
        text: String,
        banglaTypeface: Typeface,
        latinTypeface: Typeface,
        isBold: Boolean
    ): CharSequence {
        val runs = splitScriptRuns(text)
        if (runs.isEmpty()) return ""
        val ssb = SpannableStringBuilder()
        runs.forEach { (segment, isBangla) ->
            val start = ssb.length
            ssb.append(segment)
            val tf = if (isBangla) banglaTypeface else latinTypeface
            ssb.setSpan(
                CustomTypefaceSpan(tf, isBold = isBold),
                start,
                ssb.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return ssb
    }

    fun renderToBitmap(
        context: Context,
        draft: BusinessCardDraft,
        widthPx: Int = PNG_WIDTH_PX,
        heightPx: Int = PNG_HEIGHT_PX,
        includeSamplePlaceholders: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCard(
            context = context,
            canvas = canvas,
            targetWidth = widthPx.toFloat(),
            targetHeight = heightPx.toFloat(),
            draft = draft,
            includeSamplePlaceholders = includeSamplePlaceholders
        )
        return bitmap
    }

    fun exportPng(
        context: Context,
        draft: BusinessCardDraft,
        fileBaseName: String = buildSafeFileBaseName(draft)
    ): CardExportResult {
        val fileName = "${fileBaseName}_${System.currentTimeMillis()}.png"
        val outFile = File(BusinessCardLocalStore.getExportsDir(context), fileName)
        val bitmap = renderToBitmap(
            context = context,
            draft = draft,
            widthPx = PNG_WIDTH_PX,
            heightPx = PNG_HEIGHT_PX,
            includeSamplePlaceholders = false
        )
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        bitmap.recycle()

        val savedPublic = copyFileToMediaStoreDownloads(
            context = context,
            sourceFile = outFile,
            displayName = fileName,
            mimeType = "image/png",
            isImage = true
        )
        copyToAppExternalDownloads(context, outFile, fileName)
        return CardExportResult(file = outFile, savedToPublicDownloads = savedPublic)
    }

    /**
     * Exports an exact 3.5 x 2 inch (252 x 144 pt) vector/high-DPI PDF using PdfDocument
     * with bundled Bangla & Latin TTF typefaces so Bangla script renders sharp and selectable/printable.
     */
    fun exportPdf(
        context: Context,
        draft: BusinessCardDraft,
        fileBaseName: String = buildSafeFileBaseName(draft)
    ): CardExportResult {
        val fileName = "${fileBaseName}_${System.currentTimeMillis()}.pdf"
        val outFile = File(BusinessCardLocalStore.getExportsDir(context), fileName)

        val nativeSucceeded = runCatching {
            val doc = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PDF_PAGE_WIDTH_PT,
                    PDF_PAGE_HEIGHT_PT,
                    1
                ).create()
                val page = doc.startPage(pageInfo)
                drawCard(
                    context = context,
                    canvas = page.canvas,
                    targetWidth = PDF_PAGE_WIDTH_PT.toFloat(),
                    targetHeight = PDF_PAGE_HEIGHT_PT.toFloat(),
                    draft = draft,
                    includeSamplePlaceholders = false
                )
                doc.finishPage(page)
                FileOutputStream(outFile).use { fos ->
                    doc.writeTo(fos)
                }
            } finally {
                runCatching { doc.close() }
            }
            true
        }.getOrDefault(false)

        if (!nativeSucceeded || !outFile.exists() || outFile.length() == 0L) {
            writeFallbackCardPdf(context, draft, outFile)
        }

        val savedPublic = copyFileToMediaStoreDownloads(
            context = context,
            sourceFile = outFile,
            displayName = fileName,
            mimeType = "application/pdf",
            isImage = false
        )
        copyToAppExternalDownloads(context, outFile, fileName)
        return CardExportResult(file = outFile, savedToPublicDownloads = savedPublic)
    }

    /**
     * Valid 1-page PDF 1.4 writer (MediaBox [0 0 252 144] = 3.5 x 2 in) embedding the
     * 300-DPI rendered card JPEG. Used only when Android's native PdfDocument JNI is
     * absent (e.g., JVM/Robolectric test environments).
     */
    private fun writeFallbackCardPdf(context: Context, draft: BusinessCardDraft, outFile: File) {
        val bmp = renderToBitmap(
            context = context,
            draft = draft,
            widthPx = PNG_WIDTH_PX,
            heightPx = PNG_HEIGHT_PX,
            includeSamplePlaceholders = false
        )
        val jpegBaos = java.io.ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 95, jpegBaos)
        bmp.recycle()
        val jpegBytes = jpegBaos.toByteArray()

        val contentStream = "q\n$PDF_PAGE_WIDTH_PT 0 0 $PDF_PAGE_HEIGHT_PT 0 0 cm\n/Im1 Do\nQ\n"
            .toByteArray(Charsets.US_ASCII)

        val offsets = IntArray(6)
        val out = java.io.ByteArrayOutputStream()
        fun writeAscii(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))

        writeAscii("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")
        offsets[1] = out.size()
        writeAscii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        offsets[2] = out.size()
        writeAscii("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
        offsets[3] = out.size()
        writeAscii(
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $PDF_PAGE_WIDTH_PT $PDF_PAGE_HEIGHT_PT] " +
                "/Resources << /XObject << /Im1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n"
        )
        offsets[4] = out.size()
        writeAscii(
            "4 0 obj\n<< /Type /XObject /Subtype /Image /Width $PNG_WIDTH_PX /Height $PNG_HEIGHT_PX " +
                "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpegBytes.size} >>\nstream\n"
        )
        out.write(jpegBytes)
        writeAscii("\nendstream\nendobj\n")
        offsets[5] = out.size()
        writeAscii("5 0 obj\n<< /Length ${contentStream.size} >>\nstream\n")
        out.write(contentStream)
        writeAscii("\nendstream\nendobj\n")

        val xrefOffset = out.size()
        writeAscii("xref\n0 6\n")
        writeAscii("0000000000 65535 f \n")
        for (i in 1..5) {
            writeAscii(String.format(java.util.Locale.US, "%010d 00000 n \n", offsets[i]))
        }
        writeAscii("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

        FileOutputStream(outFile).use { fos ->
            fos.write(out.toByteArray())
        }
    }

    fun shareCardFile(
        context: Context,
        file: File,
        mimeType: String,
        subject: String,
        lang: AppLanguage
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(file.name, uri)
        }
        val chooser = Intent.createChooser(intent, ShopLocale.get("bc_share_chooser", lang)).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
    }

    private tailrec fun findActivity(context: Context): android.app.Activity? = when (context) {
        is android.app.Activity -> context
        is android.content.ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }

    fun openPdfWithChooser(
        context: Context,
        file: File,
        lang: AppLanguage
    ): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val needsNewTask = findActivity(context) == null
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri(file.name, uri)
            }
            val canView = context.packageManager
                .queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .isNotEmpty()
            val targetIntent = if (canView) {
                Intent.createChooser(viewIntent, ShopLocale.get("open_pdf_chooser", lang)).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (needsNewTask) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            } else {
                val fallbackSend = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = ClipData.newRawUri(file.name, uri)
                }
                Intent.createChooser(fallbackSend, ShopLocale.get("open_pdf_chooser", lang)).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (needsNewTask) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }
            context.startActivity(targetIntent)
            true
        }.getOrDefault(false)
    }

    fun buildSafeFileBaseName(draft: BusinessCardDraft): String {
        val raw = draft.shopName.ifBlank { "business_card" }
        val sanitized = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(28)
        return if (sanitized.isBlank()) "business_card" else "card_$sanitized"
    }

    private fun copyToAppExternalDownloads(context: Context, sourceFile: File, fileName: String) {
        runCatching {
            val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@runCatching
            if (!extDir.exists()) extDir.mkdirs()
            val target = File(extDir, fileName)
            sourceFile.copyTo(target, overwrite = true)
        }
    }

    private fun copyFileToMediaStoreDownloads(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String,
        isImage: Boolean
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val resolver = context.contentResolver
            val collection = if (isImage) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    if (isImage) "${Environment.DIRECTORY_PICTURES}/StoreOS Cards" else "${Environment.DIRECTORY_DOWNLOADS}/StoreOS Cards"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        }.getOrDefault(false)
    }

    /**
     * Core card drawing routine working in normalized 252 x 144 pt coordinate space
     * scaled cleanly to `targetWidth` x `targetHeight`.
     */
    fun drawCard(
        context: Context,
        canvas: Canvas,
        targetWidth: Float,
        targetHeight: Float,
        draft: BusinessCardDraft,
        includeSamplePlaceholders: Boolean = false
    ) {
        val baseW = PDF_PAGE_WIDTH_PT.toFloat() // 252f
        val baseH = PDF_PAGE_HEIGHT_PT.toFloat() // 144f
        val scaleX = targetWidth / baseW
        val scaleY = targetHeight / baseH

        canvas.save()
        canvas.scale(scaleX, scaleY)

        val palette = BusinessCardPalettes.byId(draft.paletteId)
        val roles = BusinessCardPaletteEngine.resolveRoles(palette, draft.isLightCard)
        val bgBitmap = BusinessCardLocalStore.decodeLocalBitmap(draft.backgroundImageUri, maxDimensionPx = 1200)
        val logoBitmap = BusinessCardLocalStore.decodeLocalBitmap(draft.logoUri, maxDimensionPx = 512)
        val hasBgPhoto = bgBitmap != null

        val (mainTextColor, highlightTextColor) = BusinessCardPaletteEngine.effectiveTextRolesForCanvas(
            roles = roles,
            hasBackgroundPhoto = hasBgPhoto
        )

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = roles.backgroundArgb
        }
        canvas.drawRect(0f, 0f, baseW, baseH, fillPaint)

        // Optional uploaded 3.5:2 background photo + dark overlay slider
        if (bgBitmap != null) {
            val destRect = RectF(0f, 0f, baseW, baseH)
            val srcRect = computeCenterCropSrcRect(bgBitmap.width, bgBitmap.height, baseW / baseH)
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bgBitmap, srcRect, destRect, bmpPaint)
            bgBitmap.recycle()

            val overlayAlphaInt = (draft.backgroundOverlayAlpha.coerceIn(0f, 0.85f) * 255f).toInt()
            if (overlayAlphaInt > 0) {
                fillPaint.color = Color.argb(overlayAlphaInt, 0, 0, 0)
                canvas.drawRect(0f, 0f, baseW, baseH, fillPaint)
            }
        }

        val (banglaTf, latinTf) = loadTypefaces(context, draft.fontPair)
        val fontScale = draft.fontSize.scale

        val shopVal = draft.shopName.trim()
        val ownerVal = draft.ownerName.trim()
        val titleVal = draft.title.trim()
        val taglineVal = draft.tagline.trim()
        val phoneVal = draft.phone.trim()
        val addressVal = draft.address.trim()
        val emailVal = draft.email.trim()

        if (draft.side == BusinessCardSide.BACK) {
            drawBackSide(
                canvas = canvas,
                baseW = baseW,
                baseH = baseH,
                roles = roles,
                mainTextColor = mainTextColor,
                highlightTextColor = highlightTextColor,
                hasBgPhoto = hasBgPhoto,
                logoBitmap = logoBitmap,
                shopName = shopVal.ifBlank { if (includeSamplePlaceholders) "Your Shop Name" else "" },
                tagline = taglineVal,
                fontScale = fontScale,
                banglaTf = banglaTf,
                latinTf = latinTf
            )
        } else {
            when (draft.layout) {
                BusinessCardLayout.LEFT_ALIGNED -> drawFrontLeftAligned(
                    canvas = canvas,
                    baseW = baseW,
                    baseH = baseH,
                    roles = roles,
                    mainTextColor = mainTextColor,
                    highlightTextColor = highlightTextColor,
                    logoBitmap = logoBitmap,
                    shopName = shopVal,
                    ownerName = ownerVal,
                    title = titleVal,
                    tagline = taglineVal,
                    phone = phoneVal,
                    address = addressVal,
                    email = emailVal,
                    textAlign = draft.textAlign,
                    fontScale = fontScale,
                    banglaTf = banglaTf,
                    latinTf = latinTf
                )
                BusinessCardLayout.CENTERED -> drawFrontCentered(
                    canvas = canvas,
                    baseW = baseW,
                    baseH = baseH,
                    roles = roles,
                    mainTextColor = mainTextColor,
                    highlightTextColor = highlightTextColor,
                    logoBitmap = logoBitmap,
                    shopName = shopVal,
                    ownerName = ownerVal,
                    title = titleVal,
                    tagline = taglineVal,
                    phone = phoneVal,
                    address = addressVal,
                    email = emailVal,
                    textAlign = draft.textAlign,
                    fontScale = fontScale,
                    banglaTf = banglaTf,
                    latinTf = latinTf
                )
                BusinessCardLayout.SPLIT_PANEL -> drawFrontSplitPanel(
                    canvas = canvas,
                    baseW = baseW,
                    baseH = baseH,
                    roles = roles,
                    mainTextColor = mainTextColor,
                    highlightTextColor = highlightTextColor,
                    hasBgPhoto = hasBgPhoto,
                    logoBitmap = logoBitmap,
                    shopName = shopVal,
                    ownerName = ownerVal,
                    title = titleVal,
                    tagline = taglineVal,
                    phone = phoneVal,
                    address = addressVal,
                    email = emailVal,
                    textAlign = draft.textAlign,
                    fontScale = fontScale,
                    banglaTf = banglaTf,
                    latinTf = latinTf
                )
            }
        }

        logoBitmap?.recycle()
        canvas.restore()
    }

    private fun drawBackSide(
        canvas: Canvas,
        baseW: Float,
        baseH: Float,
        roles: PaletteColorRoles,
        mainTextColor: Int,
        highlightTextColor: Int,
        hasBgPhoto: Boolean,
        logoBitmap: Bitmap?,
        shopName: String,
        tagline: String,
        fontScale: Float,
        banglaTf: Typeface,
        latinTf: Typeface
    ) {
        // Subtle inner frame accent
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
            color = applyAlpha(roles.accentGraphicsArgb, 0.45f)
        }
        canvas.drawRoundRect(RectF(8f, 8f, baseW - 8f, baseH - 8f), 4f, 4f, strokePaint)

        val centerY = baseH / 2f
        val logoSize = 34f
        val hasShop = shopName.isNotBlank()
        val hasTag = tagline.isNotBlank()

        var cursorY = when {
            logoBitmap != null && hasShop && hasTag -> centerY - 34f
            logoBitmap != null && hasShop -> centerY - 26f
            logoBitmap != null -> centerY - logoSize / 2f
            hasShop && hasTag -> centerY - 14f
            else -> centerY - 8f
        }

        if (logoBitmap != null) {
            drawLogoCentered(canvas, logoBitmap, baseW / 2f, cursorY + logoSize / 2f, logoSize)
            cursorY += logoSize + 7f
        } else {
            // Minimal monogram emblem accent
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
                color = roles.accentGraphicsArgb
            }
            val r = 13f
            val cy = cursorY + r - 6f
            canvas.drawCircle(baseW / 2f, cy, r, badgePaint)
            val initial = shopName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
            drawStaticText(
                canvas = canvas,
                text = initial,
                x = baseW / 2f - 14f,
                y = cy - 7.5f,
                width = 28f,
                color = highlightTextColor,
                textSizeSp = 12f * fontScale,
                isBold = true,
                align = Layout.Alignment.ALIGN_CENTER,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 1
            )
            cursorY = cy + r + 7f
        }

        if (hasShop) {
            cursorY += drawStaticText(
                canvas = canvas,
                text = shopName,
                x = 18f,
                y = cursorY,
                width = baseW - 36f,
                color = highlightTextColor,
                textSizeSp = 13f * fontScale,
                isBold = true,
                align = Layout.Alignment.ALIGN_CENTER,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 2
            )
        }

        // Accent rule
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = roles.accentGraphicsArgb
            strokeWidth = 1.2f
        }
        val ruleY = cursorY + 4f
        canvas.drawLine(baseW / 2f - 20f, ruleY, baseW / 2f + 20f, ruleY, rulePaint)

        if (hasTag) {
            drawStaticText(
                canvas = canvas,
                text = tagline,
                x = 24f,
                y = ruleY + 4f,
                width = baseW - 48f,
                color = applyAlpha(mainTextColor, if (hasBgPhoto) 0.92f else 0.80f),
                textSizeSp = 7.5f * fontScale,
                isBold = false,
                align = Layout.Alignment.ALIGN_CENTER,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 2
            )
        }
    }

    private fun drawFrontLeftAligned(
        canvas: Canvas,
        baseW: Float,
        baseH: Float,
        roles: PaletteColorRoles,
        mainTextColor: Int,
        highlightTextColor: Int,
        logoBitmap: Bitmap?,
        shopName: String,
        ownerName: String,
        title: String,
        tagline: String,
        phone: String,
        address: String,
        email: String,
        textAlign: BusinessCardTextAlign,
        fontScale: Float,
        banglaTf: Typeface,
        latinTf: Typeface
    ) {
        // Left vertical accent bar
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = roles.accentGraphicsArgb
        }
        canvas.drawRect(0f, 0f, 4.5f, baseH, accentPaint)

        val padX = 16f
        val contentW = baseW - padX * 2
        val resolvedAlign = toLayoutAlignment(textAlign, Layout.Alignment.ALIGN_NORMAL)

        var y = 14f
        val headerTextW = if (logoBitmap != null) contentW - 36f else contentW
        if (logoBitmap != null) {
            drawLogoCentered(canvas, logoBitmap, baseW - padX - 14f, 28f, 28f)
        }

        if (shopName.isNotBlank()) {
            y += drawStaticText(
                canvas = canvas,
                text = shopName,
                x = padX,
                y = y,
                width = headerTextW,
                color = highlightTextColor,
                textSizeSp = 12.5f * fontScale,
                isBold = true,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 2
            )
        }
        if (tagline.isNotBlank()) {
            y += drawStaticText(
                canvas = canvas,
                text = tagline,
                x = padX,
                y = y + 1f,
                width = headerTextW,
                color = applyAlpha(mainTextColor, 0.78f),
                textSizeSp = 6.8f * fontScale,
                isBold = false,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 1
            )
        }

        y = maxOf(y + 4f, 42f)
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = roles.accentGraphicsArgb
            strokeWidth = 0.9f
        }
        canvas.drawLine(padX, y, baseW - padX, y, dividerPaint)
        y += 6f

        if (ownerName.isNotBlank()) {
            y += drawStaticText(
                canvas = canvas,
                text = ownerName,
                x = padX,
                y = y,
                width = contentW,
                color = mainTextColor,
                textSizeSp = 9.5f * fontScale,
                isBold = true,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 1
            )
        }
        if (title.isNotBlank()) {
            y += drawStaticText(
                canvas = canvas,
                text = title,
                x = padX,
                y = y + 0.5f,
                width = contentW,
                color = applyAlpha(highlightTextColor, 0.92f),
                textSizeSp = 7.2f * fontScale,
                isBold = false,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 1
            )
        }

        // Bottom contact details pinned cleanly toward lower half
        val contactLines = listOfNotNull(
            phone.takeIf { it.isNotBlank() },
            email.takeIf { it.isNotBlank() },
            address.takeIf { it.isNotBlank() }
        )
        var contactY = maxOf(y + 6f, baseH - 12f - contactLines.size * 11f)
        contactLines.forEach { line ->
            if (contactY < baseH - 8f) {
                contactY += drawStaticText(
                    canvas = canvas,
                    text = line,
                    x = padX,
                    y = contactY,
                    width = contentW,
                    color = applyAlpha(mainTextColor, 0.90f),
                    textSizeSp = 6.9f * fontScale,
                    isBold = false,
                    align = resolvedAlign,
                    banglaTf = banglaTf,
                    latinTf = latinTf,
                    maxLines = 2
                ) + 1f
            }
        }
    }

    private fun drawFrontCentered(
        canvas: Canvas,
        baseW: Float,
        baseH: Float,
        roles: PaletteColorRoles,
        mainTextColor: Int,
        highlightTextColor: Int,
        logoBitmap: Bitmap?,
        shopName: String,
        ownerName: String,
        title: String,
        tagline: String,
        phone: String,
        address: String,
        email: String,
        textAlign: BusinessCardTextAlign,
        fontScale: Float,
        banglaTf: Typeface,
        latinTf: Typeface
    ) {
        // Delicate top & bottom accent borders
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = applyAlpha(roles.accentGraphicsArgb, 0.55f)
        }
        canvas.drawRect(7f, 7f, baseW - 7f, baseH - 7f, borderPaint)

        val padX = 18f
        val contentW = baseW - padX * 2
        val resolvedAlign = toLayoutAlignment(textAlign, Layout.Alignment.ALIGN_CENTER)

        var y = 13f
        if (logoBitmap != null) {
            drawLogoCentered(canvas, logoBitmap, baseW / 2f, y + 11f, 22f)
            y += 25f
        }
        if (shopName.isNotBlank()) {
            y += drawStaticText(
                canvas = canvas,
                text = shopName,
                x = padX,
                y = y,
                width = contentW,
                color = highlightTextColor,
                textSizeSp = 12f * fontScale,
                isBold = true,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 2
            )
        }
        if (tagline.isNotBlank()) {
            y += drawStaticText(
                canvas = canvas,
                text = tagline,
                x = padX,
                y = y + 1f,
                width = contentW,
                color = applyAlpha(mainTextColor, 0.78f),
                textSizeSp = 6.7f * fontScale,
                isBold = false,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 1
            )
        }

        val ruleY = y + 4f
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = roles.accentGraphicsArgb
            strokeWidth = 1f
        }
        canvas.drawLine(baseW / 2f - 26f, ruleY, baseW / 2f + 26f, ruleY, rulePaint)
        y = ruleY + 5f

        if (ownerName.isNotBlank()) {
            y += drawStaticText(
                canvas = canvas,
                text = ownerName,
                x = padX,
                y = y,
                width = contentW,
                color = mainTextColor,
                textSizeSp = 9.0f * fontScale,
                isBold = true,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 1
            )
        }
        if (title.isNotBlank()) {
            y += drawStaticText(
                canvas = canvas,
                text = title,
                x = padX,
                y = y + 0.5f,
                width = contentW,
                color = applyAlpha(highlightTextColor, 0.90f),
                textSizeSp = 7.0f * fontScale,
                isBold = false,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 1
            )
        }

        y += 4f
        val contactLine = listOfNotNull(
            phone.takeIf { it.isNotBlank() },
            email.takeIf { it.isNotBlank() }
        ).joinToString("  •  ")
        if (contactLine.isNotBlank()) {
            y += drawStaticText(
                canvas = canvas,
                text = contactLine,
                x = padX,
                y = y,
                width = contentW,
                color = applyAlpha(mainTextColor, 0.90f),
                textSizeSp = 6.8f * fontScale,
                isBold = false,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 2
            )
        }
        if (address.isNotBlank()) {
            drawStaticText(
                canvas = canvas,
                text = address,
                x = padX,
                y = y + 1f,
                width = contentW,
                color = applyAlpha(mainTextColor, 0.85f),
                textSizeSp = 6.7f * fontScale,
                isBold = false,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 2
            )
        }
    }

    private fun drawFrontSplitPanel(
        canvas: Canvas,
        baseW: Float,
        baseH: Float,
        roles: PaletteColorRoles,
        mainTextColor: Int,
        highlightTextColor: Int,
        hasBgPhoto: Boolean,
        logoBitmap: Bitmap?,
        shopName: String,
        ownerName: String,
        title: String,
        tagline: String,
        phone: String,
        address: String,
        email: String,
        textAlign: BusinessCardTextAlign,
        fontScale: Float,
        banglaTf: Typeface,
        latinTf: Typeface
    ) {
        val leftPanelW = baseW * 0.36f
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (hasBgPhoto) Color.argb(175, 0, 0, 0) else roles.splitPanelArgb
        }
        canvas.drawRect(0f, 0f, leftPanelW, baseH, panelPaint)

        // Ensure left panel text satisfies >= 4.5:1 against left panel color
        val panelBgColor = if (hasBgPhoto) Color.BLACK else roles.splitPanelArgb
        val panelTextColor = BusinessCardPaletteEngine.resolveSplitPanelTextColor(panelBgColor, roles)

        // Left panel: Logo + Shop Name
        var ly = 24f
        if (logoBitmap != null) {
            drawLogoCentered(canvas, logoBitmap, leftPanelW / 2f, ly + 15f, 30f)
            ly += 36f
        } else {
            ly += 12f
        }
        if (shopName.isNotBlank()) {
            ly += drawStaticText(
                canvas = canvas,
                text = shopName,
                x = 8f,
                y = ly,
                width = leftPanelW - 16f,
                color = panelTextColor,
                textSizeSp = 10f * fontScale,
                isBold = true,
                align = Layout.Alignment.ALIGN_CENTER,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 3
            )
        }
        if (tagline.isNotBlank()) {
            drawStaticText(
                canvas = canvas,
                text = tagline,
                x = 8f,
                y = ly + 3f,
                width = leftPanelW - 16f,
                color = applyAlpha(panelTextColor, 0.82f),
                textSizeSp = 6.2f * fontScale,
                isBold = false,
                align = Layout.Alignment.ALIGN_CENTER,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 2
            )
        }

        // Right panel: Owner, Title & Contact
        val rx = leftPanelW + 12f
        val rw = baseW - rx - 12f
        val resolvedAlign = toLayoutAlignment(textAlign, Layout.Alignment.ALIGN_NORMAL)
        var ry = 18f

        if (ownerName.isNotBlank()) {
            ry += drawStaticText(
                canvas = canvas,
                text = ownerName,
                x = rx,
                y = ry,
                width = rw,
                color = highlightTextColor,
                textSizeSp = 10.5f * fontScale,
                isBold = true,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 2
            )
        }
        if (title.isNotBlank()) {
            ry += drawStaticText(
                canvas = canvas,
                text = title,
                x = rx,
                y = ry + 1f,
                width = rw,
                color = applyAlpha(mainTextColor, 0.85f),
                textSizeSp = 7.2f * fontScale,
                isBold = false,
                align = resolvedAlign,
                banglaTf = banglaTf,
                latinTf = latinTf,
                maxLines = 1
            )
        }

        val ruleY = maxOf(ry + 6f, 52f)
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = roles.accentGraphicsArgb
            strokeWidth = 0.9f
        }
        canvas.drawLine(rx, ruleY, baseW - 12f, ruleY, rulePaint)

        var cy = ruleY + 8f
        listOfNotNull(
            phone.takeIf { it.isNotBlank() },
            email.takeIf { it.isNotBlank() },
            address.takeIf { it.isNotBlank() }
        ).forEach { line ->
            if (cy < baseH - 10f) {
                cy += drawStaticText(
                    canvas = canvas,
                    text = line,
                    x = rx,
                    y = cy,
                    width = rw,
                    color = mainTextColor,
                    textSizeSp = 6.9f * fontScale,
                    isBold = false,
                    align = resolvedAlign,
                    banglaTf = banglaTf,
                    latinTf = latinTf,
                    maxLines = 2
                ) + 3f
            }
        }
    }

    private fun drawLogoCentered(
        canvas: Canvas,
        logo: Bitmap,
        centerX: Float,
        centerY: Float,
        boxSize: Float
    ) {
        val ratio = min(boxSize / logo.width.toFloat(), boxSize / logo.height.toFloat())
        val drawW = logo.width * ratio
        val drawH = logo.height * ratio
        val left = centerX - drawW / 2f
        val top = centerY - drawH / 2f
        val dest = RectF(left, top, left + drawW, top + drawH)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(logo, null, dest, paint)
    }

    private fun drawStaticText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        color: Int,
        textSizeSp: Float,
        isBold: Boolean,
        align: Layout.Alignment,
        banglaTf: Typeface,
        latinTf: Typeface,
        maxLines: Int
    ): Float {
        if (text.isBlank() || width <= 4f) return 0f
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            this.color = color
            this.textSize = textSizeSp
            this.typeface = if (isBold) Typeface.create(latinTf, Typeface.BOLD) else latinTf
        }
        val spanned = buildSpannedText(text, banglaTf, latinTf, isBold)
        val layout = StaticLayout.Builder
            .obtain(spanned, 0, spanned.length, tp, width.toInt().coerceAtLeast(10))
            .setAlignment(align)
            .setMaxLines(maxLines)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    private fun toLayoutAlignment(
        textAlign: BusinessCardTextAlign,
        autoDefault: Layout.Alignment
    ): Layout.Alignment = when (textAlign) {
        BusinessCardTextAlign.AUTO -> autoDefault
        BusinessCardTextAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
        BusinessCardTextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
        BusinessCardTextAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
    }

    private fun applyAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun computeCenterCropSrcRect(srcW: Int, srcH: Int, targetAspect: Float): Rect {
        val currentAspect = srcW.toFloat() / srcH.toFloat()
        return if (currentAspect > targetAspect) {
            val cropW = (srcH * targetAspect).toInt().coerceIn(1, srcW)
            val left = (srcW - cropW) / 2
            Rect(left, 0, left + cropW, srcH)
        } else {
            val cropH = (srcW / targetAspect).toInt().coerceIn(1, srcH)
            val top = (srcH - cropH) / 2
            Rect(0, top, srcW, top + cropH)
        }
    }
}
