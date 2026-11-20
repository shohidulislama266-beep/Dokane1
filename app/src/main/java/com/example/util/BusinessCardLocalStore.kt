package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.model.BusinessCardDraft
import com.example.data.model.BusinessCardFontPair
import com.example.data.model.BusinessCardFontSize
import com.example.data.model.BusinessCardLayout
import com.example.data.model.BusinessCardSide
import com.example.data.model.BusinessCardTextAlign
import com.example.data.model.SavedBusinessCard
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

object BusinessCardLocalStore {
    const val CARD_ASPECT_RATIO = 3.5f / 2.0f
    const val EXPORT_PNG_WIDTH = 1050
    const val EXPORT_PNG_HEIGHT = 600

    private const val CARDS_JSON_FILE = "saved_cards.json"

    fun getRootDir(context: Context): File =
        File(context.filesDir, "business_cards").apply { mkdirs() }

    fun getExportsDir(context: Context): File =
        File(context.cacheDir, "business_cards/exports").apply { mkdirs() }

    private fun getMediaDir(context: Context): File =
        File(getRootDir(context), "media").apply { mkdirs() }

    fun decodeLocalBitmap(uriString: String, maxDimensionPx: Int = 1200): Bitmap? {
        val clean = uriString.trim()
        if (clean.isEmpty()) return null
        val path = when {
            clean.startsWith("file://") -> Uri.parse(clean).path
            clean.startsWith("/") -> clean
            else -> null
        } ?: return null
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var inSample = 1
        while (bounds.outWidth / inSample > maxDimensionPx * 2 ||
            bounds.outHeight / inSample > maxDimensionPx * 2
        ) {
            inSample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = inSample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    fun importAndCropBackground(context: Context, uri: Uri): String {
        val raw = decodeBitmapFromUri(context, uri, 2400)
            ?: throw IllegalStateException("Could not decode background image")
        try {
            val targetAspect = EXPORT_PNG_WIDTH.toFloat() / EXPORT_PNG_HEIGHT.toFloat() // 1.75f
            val srcW = raw.width
            val srcH = raw.height
            val srcAspect = srcW.toFloat() / srcH.toFloat()

            val cropW: Int
            val cropH: Int
            if (srcAspect > targetAspect) {
                cropH = srcH
                cropW = (srcH * targetAspect).roundToInt().coerceIn(1, srcW)
            } else {
                cropW = srcW
                cropH = (srcW / targetAspect).roundToInt().coerceIn(1, srcH)
            }
            val startX = ((srcW - cropW) / 2).coerceAtLeast(0)
            val startY = ((srcH - cropH) / 2).coerceAtLeast(0)

            val cropped = Bitmap.createBitmap(raw, startX, startY, cropW, cropH)
            val scaled = Bitmap.createScaledBitmap(cropped, EXPORT_PNG_WIDTH, EXPORT_PNG_HEIGHT, true)
            if (cropped !== raw) cropped.recycle()

            val outFile = File(getMediaDir(context), "bg_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            if (scaled !== cropped && scaled !== raw) scaled.recycle()
            return Uri.fromFile(outFile).toString()
        } finally {
            raw.recycle()
        }
    }

    fun importLogoImage(context: Context, uri: Uri): String {
        val raw = decodeBitmapFromUri(context, uri, 512)
            ?: throw IllegalStateException("Could not decode logo image")
        try {
            val maxSide = maxOf(raw.width, raw.height)
            val scaled = if (maxSide > 512) {
                val ratio = 512f / maxSide.toFloat()
                Bitmap.createScaledBitmap(
                    raw,
                    (raw.width * ratio).roundToInt().coerceAtLeast(1),
                    (raw.height * ratio).roundToInt().coerceAtLeast(1),
                    true
                )
            } else {
                raw
            }
            val outFile = File(getMediaDir(context), "logo_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            if (scaled !== raw) scaled.recycle()
            return Uri.fromFile(outFile).toString()
        } finally {
            raw.recycle()
        }
    }

    fun saveCard(context: Context, draft: BusinessCardDraft): SavedBusinessCard {
        val existing = loadSavedCards(context).toMutableList()
        val now = System.currentTimeMillis()
        val saved = SavedBusinessCard(
            id = "card_${UUID.randomUUID().toString().take(8)}_$now",
            savedAtMs = now,
            draft = draft
        )
        existing.add(0, saved)
        persistCards(context, existing)
        return saved
    }

    fun loadSavedCards(context: Context): List<SavedBusinessCard> {
        val file = File(getRootDir(context), CARDS_JSON_FILE)
        if (!file.exists()) return emptyList()
        val raw = runCatching { file.readText() }.getOrNull() ?: return emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { idx ->
            val o = arr.optJSONObject(idx) ?: return@mapNotNull null
            val d = o.optJSONObject("draft") ?: return@mapNotNull null
            val savedMs = o.optLong("savedAtMs", o.optLong("updatedAtMillis", 0L))
            SavedBusinessCard(
                id = o.optString("id", ""),
                savedAtMs = savedMs,
                draft = BusinessCardDraft(
                    paletteId = d.optString("paletteId", "forest_cream"),
                    isLightCard = d.optBoolean("isLightCard", false),
                    layout = BusinessCardLayout.entries.firstOrNull {
                        it.name == d.optString("layout", BusinessCardLayout.LEFT_ALIGNED.name)
                    } ?: BusinessCardLayout.LEFT_ALIGNED,
                    side = BusinessCardSide.entries.firstOrNull {
                        it.name == d.optString("side", BusinessCardSide.FRONT.name)
                    } ?: BusinessCardSide.FRONT,
                    fontPair = BusinessCardFontPair.entries.firstOrNull {
                        it.name == d.optString("fontPair", BusinessCardFontPair.SILIGURI_JAKARTA.name)
                    } ?: BusinessCardFontPair.SILIGURI_JAKARTA,
                    fontSize = BusinessCardFontSize.entries.firstOrNull {
                        it.name == d.optString("fontSize", BusinessCardFontSize.MEDIUM.name)
                    } ?: BusinessCardFontSize.MEDIUM,
                    textAlign = BusinessCardTextAlign.entries.firstOrNull {
                        it.name == d.optString("textAlign", BusinessCardTextAlign.AUTO.name)
                    } ?: BusinessCardTextAlign.AUTO,
                    shopName = d.optString("shopName", ""),
                    ownerName = d.optString("ownerName", ""),
                    title = d.optString("title", ""),
                    tagline = d.optString("tagline", ""),
                    phone = d.optString("phone", ""),
                    email = d.optString("email", ""),
                    address = d.optString("address", ""),
                    logoUri = d.optString("logoUri", ""),
                    backgroundImageUri = d.optString("backgroundImageUri", ""),
                    backgroundOverlayAlpha = d.optDouble("backgroundOverlayAlpha", 0.45).toFloat()
                )
            )
        }.sortedByDescending { it.savedAtMs }
    }

    fun deleteCard(context: Context, id: String): List<SavedBusinessCard> {
        val remaining = loadSavedCards(context).filterNot { it.id == id }
        persistCards(context, remaining)
        return remaining
    }

    private fun persistCards(context: Context, cards: List<SavedBusinessCard>) {
        val arr = JSONArray()
        cards.forEach { card ->
            val d = card.draft
            arr.put(JSONObject().apply {
                put("id", card.id)
                put("savedAtMs", card.savedAtMs)
                put("draft", JSONObject().apply {
                    put("paletteId", d.paletteId)
                    put("isLightCard", d.isLightCard)
                    put("layout", d.layout.name)
                    put("side", d.side.name)
                    put("fontPair", d.fontPair.name)
                    put("fontSize", d.fontSize.name)
                    put("textAlign", d.textAlign.name)
                    put("shopName", d.shopName)
                    put("ownerName", d.ownerName)
                    put("title", d.title)
                    put("tagline", d.tagline)
                    put("phone", d.phone)
                    put("email", d.email)
                    put("address", d.address)
                    put("logoUri", d.logoUri)
                    put("backgroundImageUri", d.backgroundImageUri)
                    put("backgroundOverlayAlpha", d.backgroundOverlayAlpha.toDouble())
                })
            })
        }
        val file = File(getRootDir(context), CARDS_JSON_FILE)
        file.writeText(arr.toString())
    }

    private fun decodeBitmapFromUri(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val bytes = when (uri.scheme) {
            "file", null -> uri.path?.let { File(it).readBytes() }
            else -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var inSample = 1
        while (bounds.outWidth / inSample > maxDim * 2 || bounds.outHeight / inSample > maxDim * 2) {
            inSample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = inSample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}
