package com.example.domain

import com.example.data.model.BusinessCardPalette
import com.example.data.model.PaletteColorRoles
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Pure Kotlin WCAG 2.1 luminance & contrast engine for Business Card palettes.
 *
 * Role assignment:
 * - Darkest color = Card background
 * - Lightest color = Main text
 * - Other two colors = Name/shop highlight and lines/icons
 * - Enforces text contrast >= 4.5:1; if highlight/text falls below 4.5:1,
 *   falls back to the lightest color (or highest-contrast endpoint in Light card mode).
 * - Light card mode swaps card background and main text while preserving >= 4.5:1 text readability.
 */
object BusinessCardPaletteEngine {

    const val MIN_TEXT_CONTRAST = 4.5
    private const val ARGB_WHITE = 0xFFFFFFFF.toInt()
    private const val ARGB_BLACK = 0xFF000000.toInt()

    fun parseHexColor(hex: String): Int {
        val cleaned = hex.trim().removePrefix("#")
        val value = cleaned.toLong(16)
        return when (cleaned.length) {
            6 -> (0xFF000000L or value).toInt()
            8 -> value.toInt()
            else -> ARGB_BLACK
        }
    }

    fun relativeLuminance(argb: Int): Double {
        val r = linearize(((argb shr 16) and 0xFF) / 255.0)
        val g = linearize(((argb shr 8) and 0xFF) / 255.0)
        val b = linearize((argb and 0xFF) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linearize(channel: Double): Double {
        return if (channel <= 0.03928) {
            channel / 12.92
        } else {
            ((channel + 0.055) / 1.055).pow(2.4)
        }
    }

    fun contrastRatio(foregroundArgb: Int, backgroundArgb: Int): Double {
        val l1 = relativeLuminance(foregroundArgb)
        val l2 = relativeLuminance(backgroundArgb)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun resolveRoles(
        palette: BusinessCardPalette,
        isLightCard: Boolean = false
    ): PaletteColorRoles {
        val argbs = palette.hexColors.map(::parseHexColor).ifEmpty {
            listOf(ARGB_BLACK, 0xFF1F2C43.toInt(), 0xFFF0EEE9.toInt(), 0xFFDC793A.toInt())
        }
        val sortedByLum = argbs.sortedBy(::relativeLuminance)
        val darkest = sortedByLum.first()
        val lightest = sortedByLum.last()
        val middle = if (sortedByLum.size >= 4) {
            listOf(sortedByLum[1], sortedByLum[2])
        } else {
            sortedByLum
        }

        val baseBackground = if (isLightCard) lightest else darkest
        val baseMainText = if (isLightCard) darkest else lightest

        // Ensure mainText meets 4.5:1 against background; if not, choose lightest/highest contrast
        val safeMainText = if (contrastRatio(baseMainText, baseBackground) >= MIN_TEXT_CONTRAST) {
            baseMainText
        } else {
            val lightestContrast = contrastRatio(lightest, baseBackground)
            val whiteContrast = contrastRatio(ARGB_WHITE, baseBackground)
            val blackContrast = contrastRatio(ARGB_BLACK, baseBackground)
            when {
                lightestContrast >= MIN_TEXT_CONTRAST -> lightest
                whiteContrast >= blackContrast -> ARGB_WHITE
                else -> ARGB_BLACK
            }
        }

        // Assign the remaining two colors to highlight text and lines/icons
        val midRanked = middle.sortedByDescending { contrastRatio(it, baseBackground) }
        val highlightCandidate = midRanked.firstOrNull() ?: safeMainText
        val accentCandidate = midRanked.lastOrNull() ?: highlightCandidate

        val safeHighlight = if (contrastRatio(highlightCandidate, baseBackground) >= MIN_TEXT_CONTRAST) {
            highlightCandidate
        } else if (!isLightCard && contrastRatio(lightest, baseBackground) >= MIN_TEXT_CONTRAST) {
            // Explicitly use lightest color when contrast < 4.5:1
            lightest
        } else {
            safeMainText
        }

        // Split panel background keeps strong visual contrast against its opposite side
        val splitPanelColor = if (!isLightCard) {
            middle.firstOrNull { it != safeHighlight } ?: accentCandidate
        } else {
            middle.lastOrNull() ?: accentCandidate
        }

        return PaletteColorRoles(
            backgroundArgb = baseBackground,
            mainTextArgb = safeMainText,
            highlightTextArgb = safeHighlight,
            accentGraphicsArgb = accentCandidate,
            splitPanelArgb = splitPanelColor,
            darkestArgb = darkest,
            lightestArgb = lightest,
            isLightCard = isLightCard,
            mainContrastRatio = contrastRatio(safeMainText, baseBackground),
            highlightContrastRatio = contrastRatio(safeHighlight, baseBackground)
        )
    }

    /**
     * When a photo background is active with dark overlay, text renders against the darkened image,
     * so we enforce readable light text using the palette's lightest color (or white if needed).
     */
    fun effectiveTextRolesForCanvas(
        roles: PaletteColorRoles,
        hasBackgroundPhoto: Boolean
    ): Pair<Int, Int> {
        if (!hasBackgroundPhoto) {
            return roles.mainTextArgb to roles.highlightTextArgb
        }
        val lightText = if (relativeLuminance(roles.lightestArgb) >= 0.65) roles.lightestArgb else ARGB_WHITE
        val hl = if (contrastRatio(roles.highlightTextArgb, ARGB_BLACK) >= MIN_TEXT_CONTRAST &&
            relativeLuminance(roles.highlightTextArgb) >= 0.25
        ) {
            roles.highlightTextArgb
        } else {
            lightText
        }
        return lightText to hl
    }

    /**
     * Resolves high-contrast (>= 4.5:1) text color for the Split Panel accent surface,
     * preferring palette lightest/darkest and falling back to whichever of White/Black achieves >= 4.5:1.
     */
    fun resolveSplitPanelTextColor(
        panelBgArgb: Int,
        roles: PaletteColorRoles
    ): Int {
        if (contrastRatio(roles.lightestArgb, panelBgArgb) >= MIN_TEXT_CONTRAST) {
            return roles.lightestArgb
        }
        if (contrastRatio(roles.darkestArgb, panelBgArgb) >= MIN_TEXT_CONTRAST) {
            return roles.darkestArgb
        }
        val whiteContrast = contrastRatio(ARGB_WHITE, panelBgArgb)
        val blackContrast = contrastRatio(ARGB_BLACK, panelBgArgb)
        return if (whiteContrast >= blackContrast) ARGB_WHITE else ARGB_BLACK
    }
}
