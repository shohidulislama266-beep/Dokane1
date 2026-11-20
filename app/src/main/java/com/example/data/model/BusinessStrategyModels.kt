package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class BusinessStrategyThresholds(
    val slowMovingDays: Int = 30,
    val overdueBakiDays: Int = 60,
    val lowMarginPercent: Double = 10.0,
    val topSellersLimit: Int = 3,
    val slowItemsLimit: Int = 5,
    val overdueDebtorsLimit: Int = 5,
    val lowMarginLimit: Int = 5,
    val maxAiRefreshesPerDay: Int = 5
)

object BusinessStrategyDefaults {
    val THRESHOLDS = BusinessStrategyThresholds()
}

data class StrategyTopSellerFact(
    val productName: String,
    val unit: String,
    val qtySold: Double,
    val stockLeft: Double,
    val salesAmount: Double
)

data class StrategySlowItemFact(
    val productName: String,
    val unit: String,
    val stockQty: Double,
    val stockValueAtCost: Double,
    val daysSinceLastSale: Int?
)

/**
 * Strictly anonymized overdue debtor entry. Never stores customer name, phone, or ID.
 */
data class StrategyOverdueBakiFact(
    val anonymizedRef: Int,
    val amountOwed: Double,
    val daysOverdue: Int
)

data class StrategyLowMarginFact(
    val productName: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val marginPercent: Double,
    val isLoss: Boolean
)

data class StrategyWeeklyComparison(
    val thisWeekSales: Double,
    val lastWeekSales: Double,
    val thisWeekOrders: Long,
    val lastWeekOrders: Long,
    val changePercent: Double?
)

data class BusinessStrategyFacts(
    val topSellers: List<StrategyTopSellerFact> = emptyList(),
    val slowItems: List<StrategySlowItemFact> = emptyList(),
    val slowItemsTotalCount: Int = 0,
    val slowItemsTotalStockValue: Double = 0.0,
    val overdueBakiCount: Int = 0,
    val overdueBakiTotalAmount: Double = 0.0,
    val overdueBakiEntries: List<StrategyOverdueBakiFact> = emptyList(),
    val lowMarginItems: List<StrategyLowMarginFact> = emptyList(),
    val lossItemsCount: Int = 0,
    val weeklyComparison: StrategyWeeklyComparison = StrategyWeeklyComparison(0.0, 0.0, 0L, 0L, null),
    val totalProductsCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalStockValueAtCost: Double = 0.0,
    val computedAtMs: Long = 0L
)

enum class StrategyActionTarget(val wireKey: String) {
    SELL("SELL"),
    STOCK("STOCK"),
    LOW_STOCK("LOW_STOCK"),
    BAKI("BAKI"),
    REPORTS("REPORTS");

    companion object {
        fun fromWireKey(raw: String): StrategyActionTarget =
            entries.firstOrNull { it.wireKey.equals(raw.trim(), ignoreCase = true) } ?: STOCK
    }
}

@IgnoreExtraProperties
data class StrategyActionAdvice(
    val text: String = "",
    val target: String = StrategyActionTarget.STOCK.wireKey,
    val buttonLabel: String = "দেখুন"
)

@IgnoreExtraProperties
data class SavedBusinessStrategy(
    @DocumentId val id: String = "today",
    val storeId: String = "",
    val dateKey: String = "",
    val diagnosis: String = "",
    val focusLine: String = "",
    val actions: List<StrategyActionAdvice> = emptyList(),
    val generatedAtMs: Long = 0L,
    val refreshCountToday: Int = 0,
    val source: String = "fixed", // "ai" or "fixed"
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PrivateAiKeyDoc(
    @DocumentId val id: String = "aiKey",
    val storeId: String = "",
    val apiKey: String = "",
    val keyLast4: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AiKeyUiSummary(
    val hasSavedKey: Boolean = false,
    val keyLast4: String = ""
) {
    val maskedDisplay: String
        get() = if (hasSavedKey && keyLast4.isNotBlank()) "••••$keyLast4" else ""
}
