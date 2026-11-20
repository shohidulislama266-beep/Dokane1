package com.example.data.model

import androidx.annotation.FontRes
import com.example.R

data class BusinessCardPalette(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val hexColors: List<String>
)

object BusinessCardPalettes {
    val ALL: List<BusinessCardPalette> = listOf(
        BusinessCardPalette(
            id = "forest_cream",
            nameEn = "Forest Cream",
            nameBn = "ফরেস্ট ক্রিম",
            hexColors = listOf("#223631", "#7D946D", "#F0EEE9", "#DC793A")
        ),
        BusinessCardPalette(
            id = "deep_navy",
            nameEn = "Deep Navy",
            nameBn = "ডিপ নেভি",
            hexColors = listOf("#131936", "#00656E", "#F0EEE9", "#DC793A")
        ),
        BusinessCardPalette(
            id = "midnight_teal",
            nameEn = "Midnight Teal",
            nameBn = "মিডনাইট টিল",
            hexColors = listOf("#1F2C43", "#00656E", "#F8F0E5", "#951528")
        ),
        BusinessCardPalette(
            id = "bordo_classic",
            nameEn = "Bordo Classic",
            nameBn = "বোর্দো ক্লাসিক",
            hexColors = listOf("#6C151E", "#0F3D3A", "#F5DABF", "#DC793A")
        ),
        BusinessCardPalette(
            id = "old_money",
            nameEn = "Old Money",
            nameBn = "ওল্ড মানি",
            hexColors = listOf("#000000", "#6C151E", "#F8F0E5", "#5A5753")
        ),
        BusinessCardPalette(
            id = "red_black",
            nameEn = "Red Black",
            nameBn = "রেড ব্ল্যাক",
            hexColors = listOf("#000000", "#DF2531", "#FFFFFF", "#6C151E")
        ),
        BusinessCardPalette(
            id = "moss_gold",
            nameEn = "Moss Gold",
            nameBn = "মস গোল্ড",
            hexColors = listOf("#2E3D30", "#7D946D", "#F8F0E5", "#DC793A")
        ),
        BusinessCardPalette(
            id = "etoile",
            nameEn = "Etoile",
            nameBn = "এতোয়াল",
            hexColors = listOf("#1C352D", "#F8F0E5", "#7D946D", "#F5DABF")
        ),
        BusinessCardPalette(
            id = "navy_red",
            nameEn = "Navy Red",
            nameBn = "নেভি রেড",
            hexColors = listOf("#131936", "#1F2C43", "#F0EEE9", "#951528")
        ),
        BusinessCardPalette(
            id = "teal_peach",
            nameEn = "Teal Peach",
            nameBn = "টিল পিচ",
            hexColors = listOf("#0F3D3A", "#00656E", "#F5DABF", "#DC793A")
        ),
        BusinessCardPalette(
            id = "burgundy_green",
            nameEn = "Burgundy Green",
            nameBn = "বার্গান্ডি গ্রিন",
            hexColors = listOf("#6C151E", "#223631", "#F0EEE9", "#7D946D")
        ),
        BusinessCardPalette(
            id = "black_orange",
            nameEn = "Black Orange",
            nameBn = "ব্ল্যাক অরেঞ্জ",
            hexColors = listOf("#000000", "#1F2C43", "#F0EEE9", "#DC793A")
        )
    )

    fun byId(id: String): BusinessCardPalette =
        ALL.firstOrNull { it.id == id } ?: ALL.first()
}

data class PaletteColorRoles(
    val backgroundArgb: Int,
    val mainTextArgb: Int,
    val highlightTextArgb: Int,
    val accentGraphicsArgb: Int,
    val splitPanelArgb: Int,
    val darkestArgb: Int,
    val lightestArgb: Int,
    val isLightCard: Boolean,
    val mainContrastRatio: Double,
    val highlightContrastRatio: Double
)

enum class BusinessCardLayout(val labelEn: String, val labelBn: String) {
    LEFT_ALIGNED("Left-Aligned", "বামে সারিবদ্ধ"),
    CENTERED("Centered", "মাঝে সারিবদ্ধ"),
    SPLIT_PANEL("Split Panel", "বিভক্ত প্যানেল")
}

enum class BusinessCardSide(val labelEn: String, val labelBn: String) {
    FRONT("Front", "সামনে"),
    BACK("Back", "পিছনে")
}

enum class BusinessCardFontPair(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    @param:FontRes val banglaFontRes: Int,
    @param:FontRes val latinFontRes: Int
) {
    SILIGURI_JAKARTA(
        id = "siliguri_jakarta",
        labelEn = "Siliguri · Jakarta",
        labelBn = "সিলিগুড়ি · জাকার্তা",
        banglaFontRes = R.font.hind_siliguri,
        latinFontRes = R.font.plus_jakarta_sans
    ),
    SERIF_PLAYFAIR(
        id = "serif_playfair",
        labelEn = "Serif · Playfair",
        labelBn = "সেরিফ · প্লেফেয়ার",
        banglaFontRes = R.font.noto_serif_bengali,
        latinFontRes = R.font.playfair_display
    ),
    NOTO_GROTESK(
        id = "noto_grotesk",
        labelEn = "Noto · Grotesk",
        labelBn = "নোটো · গ্রোটেস্ক",
        banglaFontRes = R.font.noto_sans_bengali,
        latinFontRes = R.font.space_grotesk
    ),
    TIRO_PLAYFAIR(
        id = "tiro_playfair",
        labelEn = "Tiro · Playfair",
        labelBn = "তিরো · প্লেফেয়ার",
        banglaFontRes = R.font.tiro_bangla,
        latinFontRes = R.font.playfair_display
    );

    companion object {
        fun byId(id: String): BusinessCardFontPair =
            entries.firstOrNull { it.id == id } ?: SILIGURI_JAKARTA
    }
}

enum class BusinessCardFontSize(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val scale: Float
) {
    SMALL("small", "Small", "ছোট", 0.88f),
    MEDIUM("medium", "Medium", "মাঝারি", 1.0f),
    LARGE("large", "Large", "বড়", 1.14f);

    companion object {
        fun byId(id: String): BusinessCardFontSize =
            entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}

enum class BusinessCardTextAlign(
    val id: String,
    val labelEn: String,
    val labelBn: String
) {
    AUTO("auto", "Auto", "অটো"),
    LEFT("left", "Left", "বামে"),
    CENTER("center", "Center", "মাঝে"),
    RIGHT("right", "Right", "ডানে");

    companion object {
        fun byId(id: String): BusinessCardTextAlign =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

data class BusinessCardProfileSeed(
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val email: String = "",
    val logoUri: String = ""
)

data class BusinessCardDraft(
    val cardId: String = "",
    val paletteId: String = BusinessCardPalettes.ALL.first().id,
    val isLightCard: Boolean = false,
    val layout: BusinessCardLayout = BusinessCardLayout.LEFT_ALIGNED,
    val side: BusinessCardSide = BusinessCardSide.FRONT,
    val fontPair: BusinessCardFontPair = BusinessCardFontPair.SILIGURI_JAKARTA,
    val fontSize: BusinessCardFontSize = BusinessCardFontSize.MEDIUM,
    val textAlign: BusinessCardTextAlign = BusinessCardTextAlign.AUTO,
    val shopName: String = "",
    val ownerName: String = "",
    val title: String = "",
    val tagline: String = "",
    val phone: String = "",
    val address: String = "",
    val email: String = "",
    val logoUri: String = "",
    val backgroundImageUri: String = "",
    val backgroundOverlayAlpha: Float = 0.45f
) {
    fun appliedProfile(seed: BusinessCardProfileSeed): BusinessCardDraft = copy(
        shopName = seed.shopName,
        ownerName = seed.ownerName,
        title = "",
        tagline = "",
        phone = seed.phone,
        address = seed.address,
        email = seed.email,
        logoUri = seed.logoUri
    )
}

data class SavedBusinessCard(
    val id: String,
    val savedAtMs: Long,
    val draft: BusinessCardDraft
)
