package com.example.util

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.CompletedSaleReceipt
import com.example.domain.CalculationEngine
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Offscreen PNG receipt renderer and exporter for StoreOS.
 *
 * - Renders at a fixed logical width of 720 px, dynamic height growing with items,
 *   and exports at 2x resolution (1440 px wide ARGB_8888 PNG) for crisp readability.
 * - Uses bundled Bangla (Noto Sans Bengali) & Latin (Plus Jakarta Sans) TTF fonts so
 *   Bangla glyphs are sharp without any network calls at share time.
 * - Performs zero totals math internally; displays precomputed values from
 *   CompletedSaleReceipt / CalculationEngine formatted to 2 decimals with one digit style.
 */
object ReceiptImageRenderer {

    const val LOGICAL_WIDTH_PX = 720
    const val SCALE_FACTOR = 2
    const val EXPORT_WIDTH_PX = LOGICAL_WIDTH_PX * SCALE_FACTOR // 1440 px

    private const val OUTER_MARGIN = 24f
    private const val CARD_CORNER_RADIUS = 20f
    private const val CARD_INNER_PAD = 28f
    private const val CARD_WIDTH = LOGICAL_WIDTH_PX - (OUTER_MARGIN * 2f) // 672f
    private const val CONTENT_LEFT = OUTER_MARGIN + CARD_INNER_PAD // 52f
    private const val CONTENT_WIDTH = CARD_WIDTH - (CARD_INNER_PAD * 2f) // 616f

    // Item table column widths (sum == CONTENT_WIDTH == 616f)
    private const val COL_ITEM_W = 248f
    private const val COL_QTY_W = 112f
    private const val COL_RATE_W = 124f
    private const val COL_AMOUNT_W = 132f

    private data class ReceiptPalette(
        val background: Int,
        val card: Int,
        val cardBorder: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val textMuted: Int,
        val accentGreen: Int,
        val divider: Int,
        val paidBadgeBg: Int,
        val paidBadgeText: Int,
        val bakiBadgeBg: Int,
        val bakiBadgeText: Int,
        val discountText: Int
    )

    private val lightPalette = ReceiptPalette(
        background = Color.parseColor("#FFFFFF"),
        card = Color.parseColor("#FFFFFF"),
        cardBorder = Color.parseColor("#E5E7EB"),
        textPrimary = Color.parseColor("#111827"),
        textSecondary = Color.parseColor("#374151"),
        textMuted = Color.parseColor("#6B7280"),
        accentGreen = Color.parseColor("#337418"),
        divider = Color.parseColor("#E5E7EB"),
        paidBadgeBg = Color.parseColor("#E8F5E2"),
        paidBadgeText = Color.parseColor("#337418"),
        bakiBadgeBg = Color.parseColor("#FEF3C7"),
        bakiBadgeText = Color.parseColor("#B45309"),
        discountText = Color.parseColor("#DC2626")
    )

    private val darkPalette = ReceiptPalette(
        background = Color.parseColor("#0F0F0F"),
        card = Color.parseColor("#202020"),
        cardBorder = Color.parseColor("#303030"),
        textPrimary = Color.parseColor("#F8F8F8"),
        textSecondary = Color.parseColor("#D1D5DB"),
        textMuted = Color.parseColor("#9CA3AF"),
        accentGreen = Color.parseColor("#5DD62C"),
        divider = Color.parseColor("#333333"),
        paidBadgeBg = Color.parseColor("#1D3814"),
        paidBadgeText = Color.parseColor("#5DD62C"),
        bakiBadgeBg = Color.parseColor("#3D2B08"),
        bakiBadgeText = Color.parseColor("#FBBF24"),
        discountText = Color.parseColor("#FF6B6B")
    )

    private data class FontBundle(
        val bangla: Typeface,
        val latin: Typeface
    )

    private fun loadBundledFonts(context: Context): FontBundle {
        val bangla = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
        }.getOrNull() ?: Typeface.DEFAULT
        val latin = runCatching {
            ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
        }.getOrNull() ?: Typeface.SANS_SERIF
        return FontBundle(bangla = bangla, latin = latin)
    }

    private fun isBanglaChar(ch: Char): Boolean {
        val code = ch.code
        return code in 0x0980..0x09FF || code == 0x0964 || code == 0x0965
    }

    private fun splitScriptRuns(text: String): List<Pair<String, Boolean>> {
        if (text.isEmpty()) return emptyList()
        val runs = mutableListOf<Pair<String, Boolean>>()
        val current = StringBuilder()
        var currentIsBangla = isBanglaChar(text[0])

        for (ch in text) {
            val isBangla = if (
                ch.isWhitespace() ||
                ch == '\u200C' ||
                ch == '\u200D' ||
                ch in ".,:-•/|()[]!?@#&+'\"·৳%+"
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

    private class ScriptTypefaceSpan(
        private val customTypeface: Typeface,
        private val isBold: Boolean
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

    private fun buildSpanned(
        text: String,
        fonts: FontBundle,
        isBold: Boolean
    ): CharSequence {
        val runs = splitScriptRuns(text)
        if (runs.isEmpty()) return ""
        val ssb = SpannableStringBuilder()
        runs.forEach { (segment, isBangla) ->
            val start = ssb.length
            ssb.append(segment)
            val tf = if (isBangla) fonts.bangla else fonts.latin
            ssb.setSpan(
                ScriptTypefaceSpan(tf, isBold = isBold),
                start,
                ssb.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return ssb
    }

    private fun makeStaticLayout(
        text: String,
        widthPx: Int,
        textSizePx: Float,
        color: Int,
        fonts: FontBundle,
        isBold: Boolean = false,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        lineSpacingMult: Float = 1.15f
    ): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSizePx
            this.color = color
            this.isSubpixelText = true
        }
        val spanned = buildSpanned(text, fonts, isBold)
        return StaticLayout.Builder
            .obtain(spanned, 0, spanned.length, paint, widthPx.coerceAtLeast(1))
            .setAlignment(align)
            .setLineSpacing(0f, lineSpacingMult)
            .setIncludePad(true)
            .build()
    }

    /**
     * Formats each unit rate cleanly as stored (no extra rounding/truncation of decimals)
     * using single-digit-style EN or BN numerals and ৳ symbol.
     */
    internal fun formatStoredRate(rate: Double, lang: AppLanguage): String {
        val safe = if (kotlin.math.abs(rate) < 1e-9) 0.0 else rate
        val bd = BigDecimal.valueOf(safe).stripTrailingZeros()
        val rawNum = bd.toPlainString()
        val localizedNum = ShopLocale.digits(rawNum, lang)
        return if (lang == AppLanguage.BN) {
            "$localizedNum ${ShopLocale.CURRENCY}"
        } else {
            "${ShopLocale.CURRENCY} $localizedNum"
        }
    }

    internal fun buildSafeInvoiceFileName(invoiceNo: String, timestamp: Long): String {
        val clean = invoiceNo.trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
        val base = clean.ifBlank { "receipt_$timestamp" }
        return "$base.png"
    }

    private fun decodeLogoBitmap(context: Context, rawUri: String): Bitmap? {
        val clean = rawUri.trim()
        if (clean.isEmpty()) return null
        // 1. Data URI (data:image/...;base64,...)
        if (ProductImageUtil.isSyncDataUri(clean)) {
            val bytes = ProductImageUtil.decodeDataUriBytes(clean) ?: return null
            return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        }
        // 2. Local file path or file://
        BusinessCardLocalStore.decodeLocalBitmap(clean, maxDimensionPx = 512)?.let { return it }
        // 3. content:// URI
        if (clean.startsWith("content://")) {
            return runCatching {
                context.contentResolver.openInputStream(Uri.parse(clean))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        }
        return null
    }

    fun renderReceiptBitmap(
        context: Context,
        receipt: CompletedSaleReceipt,
        style: ReceiptImageStyle = ReceiptImageStyle.LIGHT,
        lang: AppLanguage = AppLanguage.EN
    ): Bitmap {
        val fonts = loadBundledFonts(context)
        val palette = if (style == ReceiptImageStyle.DARK) darkPalette else lightPalette
        val logoBitmap = decodeLogoBitmap(context, receipt.storeLogoUri)

        try {
            // Pass 1: Measure exact logical height for width = 720 px
            val logicalHeight = ceil(
                drawOrMeasure(
                    canvas = null,
                    receipt = receipt,
                    palette = palette,
                    fonts = fonts,
                    logoBitmap = logoBitmap,
                    lang = lang
                )
            ).toInt().coerceAtLeast(360)

            // Pass 2: Allocate crisp 2x bitmap (1440 px wide) and draw
            val outBitmap = Bitmap.createBitmap(
                LOGICAL_WIDTH_PX * SCALE_FACTOR,
                logicalHeight * SCALE_FACTOR,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(outBitmap)
            canvas.scale(SCALE_FACTOR.toFloat(), SCALE_FACTOR.toFloat())
            drawOrMeasure(
                canvas = canvas,
                receipt = receipt,
                palette = palette,
                fonts = fonts,
                logoBitmap = logoBitmap,
                lang = lang,
                measuredTotalHeight = logicalHeight.toFloat()
            )
            return outBitmap
        } finally {
            logoBitmap?.recycle()
        }
    }

    /**
     * Shared two-pass layout function:
     * When canvas == null -> computes exact logical height without drawing.
     * When canvas != null -> paints every section onto canvas with identical geometry.
     */
    private fun drawOrMeasure(
        canvas: Canvas?,
        receipt: CompletedSaleReceipt,
        palette: ReceiptPalette,
        fonts: FontBundle,
        logoBitmap: Bitmap?,
        lang: AppLanguage,
        measuredTotalHeight: Float = 0f
    ): Float {
        if (canvas != null) {
            canvas.drawColor(palette.background)
            val cardRect = RectF(
                OUTER_MARGIN,
                OUTER_MARGIN,
                LOGICAL_WIDTH_PX - OUTER_MARGIN,
                measuredTotalHeight - OUTER_MARGIN
            )
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.card
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.cardBorder
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRoundRect(cardRect, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, cardPaint)
            canvas.drawRoundRect(cardRect, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, borderPaint)
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.divider
            strokeWidth = 1.25f
        }
        val rowSepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.divider
            alpha = 150
            strokeWidth = 1f
        }

        var y = OUTER_MARGIN + CARD_INNER_PAD
        val contentIntW = CONTENT_WIDTH.toInt()

        // 1. Optional Shop Logo
        if (logoBitmap != null && logoBitmap.width > 0 && logoBitmap.height > 0) {
            val boxSize = 88f
            if (canvas != null) {
                val scale = min(boxSize / logoBitmap.width, boxSize / logoBitmap.height)
                val drawW = logoBitmap.width * scale
                val drawH = logoBitmap.height * scale
                val left = (LOGICAL_WIDTH_PX - drawW) / 2f
                val destRect = RectF(left, y, left + drawW, y + drawH)
                val srcRect = Rect(0, 0, logoBitmap.width, logoBitmap.height)
                val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(logoBitmap, srcRect, destRect, imgPaint)
            }
            y += boxSize + 12f
        }

        // 2. Shop Name (skip if blank)
        val cleanShopName = receipt.storeName.trim()
        if (cleanShopName.isNotEmpty()) {
            val shopLayout = makeStaticLayout(
                text = cleanShopName,
                widthPx = contentIntW,
                textSizePx = 26f,
                color = palette.textPrimary,
                fonts = fonts,
                isBold = true,
                align = Layout.Alignment.ALIGN_CENTER
            )
            if (canvas != null) {
                canvas.save()
                canvas.translate(CONTENT_LEFT, y)
                shopLayout.draw(canvas)
                canvas.restore()
            }
            y += shopLayout.height + 4f
        }

        // 3. Shop Phone (skip if blank)
        val cleanPhone = receipt.storePhone.trim()
        if (cleanPhone.isNotEmpty()) {
            val localizedPhoneLine = ShopLocale.format(
                "phone_prefix",
                lang,
                ShopLocale.digits(cleanPhone, lang)
            )
            val phoneLayout = makeStaticLayout(
                text = localizedPhoneLine,
                widthPx = contentIntW,
                textSizePx = 16.5f,
                color = palette.textSecondary,
                fonts = fonts,
                isBold = false,
                align = Layout.Alignment.ALIGN_CENTER
            )
            if (canvas != null) {
                canvas.save()
                canvas.translate(CONTENT_LEFT, y)
                phoneLayout.draw(canvas)
                canvas.restore()
            }
            y += phoneLayout.height + 3f
        }

        // 4. Shop Address (skip if blank)
        val cleanAddress = receipt.storeAddress.trim()
        if (cleanAddress.isNotEmpty()) {
            val addrLayout = makeStaticLayout(
                text = ShopLocale.digits(cleanAddress, lang),
                widthPx = contentIntW,
                textSizePx = 15.5f,
                color = palette.textMuted,
                fonts = fonts,
                isBold = false,
                align = Layout.Alignment.ALIGN_CENTER
            )
            if (canvas != null) {
                canvas.save()
                canvas.translate(CONTENT_LEFT, y)
                addrLayout.draw(canvas)
                canvas.restore()
            }
            y += addrLayout.height + 4f
        }

        if (cleanShopName.isNotEmpty() || cleanPhone.isNotEmpty() || cleanAddress.isNotEmpty() || logoBitmap != null) {
            y += 10f
            if (canvas != null) {
                canvas.drawLine(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y, dividerPaint)
            }
            y += 16f
        }

        // 5. Invoice Number (exact as stored, no '#'), Date & Time, and PAID / BAKI (DUE) badge
        val isBaki = receipt.paymentType.equals("baki", ignoreCase = true)
        val badgeText = ShopLocale.get(
            if (isBaki) "receipt_status_baki" else "receipt_status_paid",
            lang
        )
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            color = if (isBaki) palette.bakiBadgeText else palette.paidBadgeText
        }
        val badgeSpanned = buildSpanned(badgeText, fonts, isBold = true)
        val badgeTextWidth = StaticLayout.getDesiredWidth(badgeSpanned, badgeTextPaint)
        val badgePadH = 14f
        val badgePadV = 8f
        val badgeW = badgeTextWidth + badgePadH * 2f
        val badgeLayout = makeStaticLayout(
            text = badgeText,
            widthPx = ceil(badgeTextWidth + 4f).toInt(),
            textSizePx = 14f,
            color = if (isBaki) palette.bakiBadgeText else palette.paidBadgeText,
            fonts = fonts,
            isBold = true,
            align = Layout.Alignment.ALIGN_CENTER
        )
        val badgeH = badgeLayout.height + badgePadV * 2f

        val metaLeftW = (CONTENT_WIDTH - badgeW - 14f).toInt().coerceAtLeast(240)
        val invoiceDisplay = if (receipt.invoiceNo.isNotBlank()) {
            "${ShopLocale.get("invoice", lang)}: ${ShopLocale.digits(receipt.invoiceNo.trim(), lang)}"
        } else {
            ShopLocale.get("invoice", lang)
        }
        val invoiceLayout = makeStaticLayout(
            text = invoiceDisplay,
            widthPx = metaLeftW,
            textSizePx = 20f,
            color = palette.accentGreen,
            fonts = fonts,
            isBold = true
        )
        val dateText = ShopLocale.formatDateLong(receipt.timestamp, lang)
        val dateLayout = makeStaticLayout(
            text = dateText,
            widthPx = metaLeftW,
            textSizePx = 15f,
            color = palette.textMuted,
            fonts = fonts,
            isBold = false
        )

        if (canvas != null) {
            // Left: Invoice + Date/Time
            canvas.save()
            canvas.translate(CONTENT_LEFT, y)
            invoiceLayout.draw(canvas)
            canvas.restore()

            canvas.save()
            canvas.translate(CONTENT_LEFT, y + invoiceLayout.height + 2f)
            dateLayout.draw(canvas)
            canvas.restore()

            // Right: Status Badge
            val badgeLeft = CONTENT_LEFT + CONTENT_WIDTH - badgeW
            val badgeRect = RectF(badgeLeft, y + 2f, badgeLeft + badgeW, y + 2f + badgeH)
            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isBaki) palette.bakiBadgeBg else palette.paidBadgeBg
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(badgeRect, 10f, 10f, badgeBgPaint)
            canvas.save()
            canvas.translate(badgeLeft + badgePadH - 2f, y + 2f + badgePadV)
            badgeLayout.draw(canvas)
            canvas.restore()
        }
        y += max(invoiceLayout.height + 2f + dateLayout.height, badgeH + 2f) + 8f

        // 6. Customer Name & Phone (only when customer is attached)
        val rawCustomer = receipt.customerName.trim()
        val hasCustomer = rawCustomer.isNotEmpty() &&
            !rawCustomer.equals("Cash Sale", ignoreCase = true) &&
            !rawCustomer.equals("Walk-in", ignoreCase = true)
        if (hasCustomer) {
            val custLine = buildString {
                append(ShopLocale.format("customer_prefix", lang, rawCustomer))
                val custPhone = receipt.customerPhone.trim()
                if (custPhone.isNotEmpty()) {
                    append("  •  ")
                    append(ShopLocale.digits(custPhone, lang))
                }
            }
            val custLayout = makeStaticLayout(
                text = custLine,
                widthPx = contentIntW,
                textSizePx = 16f,
                color = palette.textSecondary,
                fonts = fonts,
                isBold = true
            )
            if (canvas != null) {
                canvas.save()
                canvas.translate(CONTENT_LEFT, y)
                custLayout.draw(canvas)
                canvas.restore()
            }
            y += custLayout.height + 8f
        }

        y += 8f
        if (canvas != null) {
            canvas.drawLine(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y, dividerPaint)
        }
        y += 14f

        // 7. Item Table Header: Item | Qty | Rate | Amount
        val hdrItem = makeStaticLayout(
            text = ShopLocale.get("receipt_col_item", lang),
            widthPx = COL_ITEM_W.toInt(),
            textSizePx = 14f,
            color = palette.textMuted,
            fonts = fonts,
            isBold = true,
            align = Layout.Alignment.ALIGN_NORMAL
        )
        val hdrQty = makeStaticLayout(
            text = ShopLocale.get("receipt_col_qty", lang),
            widthPx = COL_QTY_W.toInt(),
            textSizePx = 14f,
            color = palette.textMuted,
            fonts = fonts,
            isBold = true,
            align = Layout.Alignment.ALIGN_CENTER
        )
        val hdrRate = makeStaticLayout(
            text = ShopLocale.get("receipt_col_rate", lang),
            widthPx = COL_RATE_W.toInt(),
            textSizePx = 14f,
            color = palette.textMuted,
            fonts = fonts,
            isBold = true,
            align = Layout.Alignment.ALIGN_OPPOSITE
        )
        val hdrAmount = makeStaticLayout(
            text = ShopLocale.get("receipt_col_amount", lang),
            widthPx = COL_AMOUNT_W.toInt(),
            textSizePx = 14f,
            color = palette.textMuted,
            fonts = fonts,
            isBold = true,
            align = Layout.Alignment.ALIGN_OPPOSITE
        )
        val hdrHeight = maxOf(hdrItem.height, hdrQty.height, hdrRate.height, hdrAmount.height).toFloat()
        if (canvas != null) {
            drawTableRowLayouts(canvas, y, hdrItem, hdrQty, hdrRate, hdrAmount)
        }
        y += hdrHeight + 8f
        if (canvas != null) {
            canvas.drawLine(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y, dividerPaint)
        }
        y += 8f

        // 8. Item Rows (full wrapped item names, exact stored rate, 2-decimal rounded lineTotal)
        receipt.items.forEachIndexed { idx, item ->
            val itemLayout = makeStaticLayout(
                text = item.name,
                widthPx = (COL_ITEM_W - 8f).toInt(),
                textSizePx = 16.5f,
                color = palette.textPrimary,
                fonts = fonts,
                isBold = false,
                align = Layout.Alignment.ALIGN_NORMAL
            )
            val qtyStr = "${ShopLocale.formatNumber(item.qty, lang)} ${ShopLocale.displayUnit(item.unit, lang)}"
            val qtyLayout = makeStaticLayout(
                text = qtyStr,
                widthPx = COL_QTY_W.toInt(),
                textSizePx = 15.5f,
                color = palette.textSecondary,
                fonts = fonts,
                isBold = false,
                align = Layout.Alignment.ALIGN_CENTER
            )
            val rateStr = formatStoredRate(item.sellPrice, lang)
            val rateLayout = makeStaticLayout(
                text = rateStr,
                widthPx = COL_RATE_W.toInt(),
                textSizePx = 15.5f,
                color = palette.textSecondary,
                fonts = fonts,
                isBold = false,
                align = Layout.Alignment.ALIGN_OPPOSITE
            )
            val amountStr = ShopLocale.formatMoneyFixed2(item.lineTotal, lang)
            val amountLayout = makeStaticLayout(
                text = amountStr,
                widthPx = COL_AMOUNT_W.toInt(),
                textSizePx = 16f,
                color = palette.textPrimary,
                fonts = fonts,
                isBold = true,
                align = Layout.Alignment.ALIGN_OPPOSITE
            )

            val rowH = maxOf(itemLayout.height, qtyLayout.height, rateLayout.height, amountLayout.height).toFloat()
            if (canvas != null) {
                drawTableRowLayouts(canvas, y, itemLayout, qtyLayout, rateLayout, amountLayout)
            }
            y += rowH + 10f
            if (idx < receipt.items.lastIndex) {
                if (canvas != null) {
                    canvas.drawLine(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y, rowSepPaint)
                }
                y += 10f
            }
        }

        y += 4f
        if (canvas != null) {
            canvas.drawLine(CONTENT_LEFT, y, CONTENT_LEFT + CONTENT_WIDTH, y, dividerPaint)
        }
        y += 14f

        // 9. Summary Totals (uses values directly from receipt/CalculationEngine, formatted to 2 decimals)
        y = drawSummaryLine(
            canvas = canvas,
            y = y,
            label = ShopLocale.get("subtotal", lang),
            value = ShopLocale.formatMoneyFixed2(receipt.subtotal, lang),
            labelColor = palette.textSecondary,
            valueColor = palette.textPrimary,
            textSize = 17f,
            isBold = false,
            fonts = fonts
        )

        if (receipt.discount > 0.0) {
            y = drawSummaryLine(
                canvas = canvas,
                y = y,
                label = ShopLocale.get("total_discount", lang),
                value = "-${ShopLocale.formatMoneyFixed2(receipt.discount, lang)}",
                labelColor = palette.discountText,
                valueColor = palette.discountText,
                textSize = 17f,
                isBold = false,
                fonts = fonts
            )
        }

        if (isBaki) {
            val paidAmt = CalculationEngine.roundMoney(receipt.paidAmount.coerceAtLeast(0.0))
            val dueAmt = CalculationEngine.roundMoney(
                if (receipt.dueAmount > 0.0 || receipt.paidAmount > 0.0) {
                    receipt.dueAmount.coerceAtLeast(0.0)
                } else {
                    (receipt.grandTotal - paidAmt).coerceAtLeast(0.0)
                }
            )
            y = drawSummaryLine(
                canvas = canvas,
                y = y,
                label = ShopLocale.get("paid_amount_label", lang),
                value = ShopLocale.formatMoneyFixed2(paidAmt, lang),
                labelColor = palette.textSecondary,
                valueColor = palette.textPrimary,
                textSize = 17f,
                isBold = false,
                fonts = fonts
            )
            y = drawSummaryLine(
                canvas = canvas,
                y = y,
                label = ShopLocale.get("due_amount_label", lang),
                value = ShopLocale.formatMoneyFixed2(dueAmt, lang),
                labelColor = palette.bakiBadgeText,
                valueColor = palette.bakiBadgeText,
                textSize = 17f,
                isBold = true,
                fonts = fonts
            )
        }

        y += 6f
        y = drawSummaryLine(
            canvas = canvas,
            y = y,
            label = ShopLocale.get("grand_total", lang).uppercase(),
            value = ShopLocale.formatMoneyFixed2(receipt.grandTotal, lang),
            labelColor = palette.textPrimary,
            valueColor = palette.accentGreen,
            textSize = 22f,
            isBold = true,
            fonts = fonts
        )

        // 10. Optional Notes
        val cleanNotes = receipt.notes.trim()
        if (cleanNotes.isNotEmpty()) {
            y += 8f
            val noteLayout = makeStaticLayout(
                text = ShopLocale.format("note_prefix", lang, ShopLocale.digits(cleanNotes, lang)),
                widthPx = contentIntW,
                textSizePx = 14.5f,
                color = palette.textMuted,
                fonts = fonts,
                isBold = false
            )
            if (canvas != null) {
                canvas.save()
                canvas.translate(CONTENT_LEFT, y)
                noteLayout.draw(canvas)
                canvas.restore()
            }
            y += noteLayout.height
        }

        // 11. Thank-you footer
        y += 18f
        val thanksLayout = makeStaticLayout(
            text = ShopLocale.get("receipt_image_thank_you", lang),
            widthPx = contentIntW,
            textSizePx = 15.5f,
            color = palette.textMuted,
            fonts = fonts,
            isBold = false,
            align = Layout.Alignment.ALIGN_CENTER
        )
        if (canvas != null) {
            canvas.save()
            canvas.translate(CONTENT_LEFT, y)
            thanksLayout.draw(canvas)
            canvas.restore()
        }
        y += thanksLayout.height

        return y + CARD_INNER_PAD + OUTER_MARGIN
    }

    private fun drawTableRowLayouts(
        canvas: Canvas,
        y: Float,
        colItem: StaticLayout,
        colQty: StaticLayout,
        colRate: StaticLayout,
        colAmount: StaticLayout
    ) {
        var x = CONTENT_LEFT
        canvas.save()
        canvas.translate(x, y)
        colItem.draw(canvas)
        canvas.restore()

        x += COL_ITEM_W
        canvas.save()
        canvas.translate(x, y)
        colQty.draw(canvas)
        canvas.restore()

        x += COL_QTY_W
        canvas.save()
        canvas.translate(x, y)
        colRate.draw(canvas)
        canvas.restore()

        x += COL_RATE_W
        canvas.save()
        canvas.translate(x, y)
        colAmount.draw(canvas)
        canvas.restore()
    }

    private fun drawSummaryLine(
        canvas: Canvas?,
        y: Float,
        label: String,
        value: String,
        labelColor: Int,
        valueColor: Int,
        textSize: Float,
        isBold: Boolean,
        fonts: FontBundle
    ): Float {
        val leftW = (CONTENT_WIDTH * 0.52f).roundToInt()
        val rightW = (CONTENT_WIDTH - leftW).roundToInt()
        val labelLayout = makeStaticLayout(
            text = label,
            widthPx = leftW,
            textSizePx = textSize,
            color = labelColor,
            fonts = fonts,
            isBold = isBold,
            align = Layout.Alignment.ALIGN_NORMAL
        )
        val valueLayout = makeStaticLayout(
            text = value,
            widthPx = rightW,
            textSizePx = textSize,
            color = valueColor,
            fonts = fonts,
            isBold = isBold,
            align = Layout.Alignment.ALIGN_OPPOSITE
        )
        if (canvas != null) {
            canvas.save()
            canvas.translate(CONTENT_LEFT, y)
            labelLayout.draw(canvas)
            canvas.restore()

            canvas.save()
            canvas.translate(CONTENT_LEFT + leftW, y)
            valueLayout.draw(canvas)
            canvas.restore()
        }
        return y + max(labelLayout.height, valueLayout.height) + 6f
    }

    fun exportReceiptPng(
        context: Context,
        receipt: CompletedSaleReceipt,
        style: ReceiptImageStyle = ReceiptImageStyle.LIGHT,
        lang: AppLanguage = AppLanguage.EN
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = buildSafeInvoiceFileName(receipt.invoiceNo, receipt.timestamp)
        val outFile = File(reportsDir, fileName)
        val bitmap = renderReceiptBitmap(context, receipt, style, lang)
        try {
            FileOutputStream(outFile).use { fos ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                    throw IllegalStateException("PNG compression failed")
                }
            }
        } finally {
            bitmap.recycle()
        }
        if (!outFile.exists() || outFile.length() <= 0L) {
            throw IllegalStateException("Exported PNG file is empty")
        }
        return outFile
    }

    fun shareExistingReceiptPng(
        context: Context,
        pngFile: File,
        receipt: CompletedSaleReceipt,
        lang: AppLanguage = AppLanguage.EN
    ): Result<File> = runCatching {
        if (!pngFile.exists() || pngFile.length() <= 0L) {
            throw IllegalStateException("Exported PNG file is missing")
        }
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, pngFile)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, receipt.invoiceNo.ifBlank { pngFile.nameWithoutExtension })
            clipData = ClipData.newRawUri(pngFile.name, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(
            sendIntent,
            ShopLocale.get("share_receipt_via", lang)
        ).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
        pngFile
    }

    fun shareReceiptPng(
        context: Context,
        receipt: CompletedSaleReceipt,
        style: ReceiptImageStyle = ReceiptImageStyle.LIGHT,
        lang: AppLanguage = AppLanguage.EN
    ): Result<File> = runCatching {
        val pngFile = exportReceiptPng(context, receipt, style, lang)
        shareExistingReceiptPng(context, pngFile, receipt, lang).getOrThrow()
    }

    fun saveReceiptPngToGallery(
        context: Context,
        receipt: CompletedSaleReceipt,
        style: ReceiptImageStyle = ReceiptImageStyle.LIGHT,
        lang: AppLanguage = AppLanguage.EN
    ): Result<String> = runCatching {
        val fileName = buildSafeInvoiceFileName(receipt.invoiceNo, receipt.timestamp)
        val bitmap = renderReceiptBitmap(context, receipt, style, lang)
        try {
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/StoreOS")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Unable to create gallery entry")
                var writeSuccess = false
                try {
                    resolver.openOutputStream(uri)?.use { os ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)) {
                            throw IllegalStateException("PNG encoding failed")
                        }
                    } ?: throw IllegalStateException("Cannot open gallery output stream")
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    writeSuccess = true
                } finally {
                    if (!writeSuccess) {
                        runCatching { resolver.delete(uri, null, null) }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "StoreOS"
                )
                val targetDir = if (picturesDir.exists() || picturesDir.mkdirs()) {
                    picturesDir
                } else {
                    File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "StoreOS")
                        .apply { mkdirs() }
                }
                val outFile = File(targetDir, fileName)
                FileOutputStream(outFile).use { fos ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                        throw IllegalStateException("PNG encoding failed")
                    }
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outFile.absolutePath),
                    arrayOf("image/png"),
                    null
                )
            }
            fileName
        } finally {
            bitmap.recycle()
        }
    }
}
