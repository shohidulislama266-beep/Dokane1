package com.example.domain

import com.example.data.model.BusinessStrategyDefaults
import com.example.data.model.BusinessStrategyFacts
import com.example.data.model.BusinessStrategyThresholds
import com.example.data.model.Customer
import com.example.data.model.Payment
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SavedBusinessStrategy
import com.example.data.model.StrategyActionAdvice
import com.example.data.model.StrategyActionTarget
import com.example.data.model.StrategyLowMarginFact
import com.example.data.model.StrategyOverdueBakiFact
import com.example.data.model.StrategySlowItemFact
import com.example.data.model.StrategyTopSellerFact
import com.example.data.model.StrategyWeeklyComparison
import com.example.util.ShopLocale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong
import org.json.JSONObject

object BusinessStrategyEngine {

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun calculateBusinessStrategyFacts(
        products: List<Product>,
        sales: List<Sale>,
        customers: List<Customer>,
        payments: List<Payment>,
        nowMs: Long = System.currentTimeMillis(),
        thresholds: BusinessStrategyThresholds = BusinessStrategyDefaults.THRESHOLDS
    ): BusinessStrategyFacts {
        val productById = products.associateBy { it.id }
        val productByLowerName = products.associateBy { it.name.trim().lowercase(Locale.ROOT) }

        val slowCutoffMs = nowMs - (thresholds.slowMovingDays.coerceAtLeast(1) * DAY_MS)
        val overdueCutoffMs = nowMs - (thresholds.overdueBakiDays.coerceAtLeast(1) * DAY_MS)

        // Track last sold time per product & 30-day top sellers
        val lastSoldByProductId = mutableMapOf<String, Long>()
        val lastSoldByName = mutableMapOf<String, Long>()

        data class SellerAgg(
            val key: String,
            val productName: String,
            val unit: String,
            var qtySold: Double = 0.0,
            var salesAmount: Double = 0.0,
            val stockLeft: Double = 0.0
        )

        val recentSales = sales.filter { (it.createdAt?.toDate()?.time ?: 0L) >= slowCutoffMs }
        val sellerSource = if (recentSales.isNotEmpty()) recentSales else sales
        val sellerMap = mutableMapOf<String, SellerAgg>()

        for (sale in sales) {
            val saleTs = sale.createdAt?.toDate()?.time ?: 0L
            for (item in sale.items) {
                val pId = item.productId.trim()
                val pNameKey = item.name.trim().lowercase(Locale.ROOT)
                if (pId.isNotBlank() && saleTs > (lastSoldByProductId[pId] ?: 0L)) {
                    lastSoldByProductId[pId] = saleTs
                }
                if (pNameKey.isNotBlank() && saleTs > (lastSoldByName[pNameKey] ?: 0L)) {
                    lastSoldByName[pNameKey] = saleTs
                }
            }
        }

        for (sale in sellerSource) {
            for (item in sale.items) {
                val matchedProd = if (item.productId.isNotBlank()) {
                    productById[item.productId]
                } else {
                    productByLowerName[item.name.trim().lowercase(Locale.ROOT)]
                }
                val aggKey = matchedProd?.id?.takeIf { it.isNotBlank() }
                    ?: item.productId.ifBlank { item.name.trim().lowercase(Locale.ROOT) }
                if (aggKey.isBlank()) continue
                val agg = sellerMap.getOrPut(aggKey) {
                    SellerAgg(
                        key = aggKey,
                        productName = matchedProd?.name?.ifBlank { item.name } ?: item.name,
                        unit = matchedProd?.let { CalculationEngine.effectiveStockUnit(it) } ?: item.unit.ifBlank { "pcs" },
                        stockLeft = matchedProd?.stock ?: 0.0
                    )
                }
                agg.qtySold += CalculationEngine.saleItemStockQty(item)
                agg.salesAmount += item.lineTotal.coerceAtLeast(0.0)
            }
        }

        val topSellers = sellerMap.values
            .sortedWith(compareByDescending<SellerAgg> { it.salesAmount }.thenByDescending { it.qtySold })
            .take(thresholds.topSellersLimit)
            .map {
                StrategyTopSellerFact(
                    productName = it.productName,
                    unit = it.unit,
                    qtySold = CalculationEngine.roundStock(it.qtySold),
                    stockLeft = CalculationEngine.roundStock(it.stockLeft),
                    salesAmount = CalculationEngine.roundMoney(it.salesAmount)
                )
            }

        // 2. Slow items: stocked products with no sale in 30 days
        val allSlowItems = mutableListOf<StrategySlowItemFact>()
        for (p in products) {
            if (p.stock <= 0.0001) continue
            val lastSaleId = lastSoldByProductId[p.id] ?: 0L
            val lastSaleName = lastSoldByName[p.name.trim().lowercase(Locale.ROOT)] ?: 0L
            val lastSaleTs = maxOf(lastSaleId, lastSaleName)
            if (lastSaleTs == 0L || lastSaleTs <= slowCutoffMs) {
                val stockVal = CalculationEngine.roundMoney(p.stock * p.buyPrice.coerceAtLeast(0.0))
                val daysSince = if (lastSaleTs > 0L) ((nowMs - lastSaleTs) / DAY_MS).toInt().coerceAtLeast(thresholds.slowMovingDays) else null
                allSlowItems.add(
                    StrategySlowItemFact(
                        productName = p.name,
                        unit = CalculationEngine.effectiveStockUnit(p),
                        stockQty = CalculationEngine.roundStock(p.stock),
                        stockValueAtCost = stockVal,
                        daysSinceLastSale = daysSince
                    )
                )
            }
        }
        val slowItemsSorted = allSlowItems.sortedByDescending { it.stockValueAtCost }
        val slowItemsTotalValue = CalculationEngine.roundMoney(slowItemsSorted.sumOf { it.stockValueAtCost })
        val slowItems = slowItemsSorted.take(thresholds.slowItemsLimit)

        // 3. Overdue customers owing over 60 days (STRICTLY anonymized - NO names or phones)
        val bakiOverview = CalculationEngine.calculateBakiOverview(customers, sales, payments)
        val allOverdueDebtors = mutableListOf<Pair<Double, Int>>() // (balance, daysOverdue)
        for (summary in bakiOverview.debtors) {
            if (summary.currentBalance <= 0.001) continue
            val oldestUnpaidTs = computeOldestUnpaidBakiTimestamp(summary.customer, sales, payments)
            if (oldestUnpaidTs in 1L..overdueCutoffMs) {
                val daysOverdue = ((nowMs - oldestUnpaidTs) / DAY_MS).toInt().coerceAtLeast(thresholds.overdueBakiDays)
                allOverdueDebtors.add(CalculationEngine.roundMoney(summary.currentBalance) to daysOverdue)
            }
        }
        val sortedOverdue = allOverdueDebtors.sortedByDescending { it.first }
        val overdueTotal = CalculationEngine.roundMoney(sortedOverdue.sumOf { it.first })
        val overdueEntries = sortedOverdue.take(thresholds.overdueDebtorsLimit).mapIndexed { index, pair ->
            StrategyOverdueBakiFact(
                anonymizedRef = index + 1,
                amountOwed = pair.first,
                daysOverdue = pair.second
            )
        }

        // 4. Low margin (< 10%) or loss products
        val allLowMargin = mutableListOf<StrategyLowMarginFact>()
        for (p in products) {
            if (p.buyPrice <= 0.0 && p.sellPrice <= 0.0) continue
            val isLoss = p.sellPrice < p.buyPrice
            val marginPct = if (p.sellPrice > 0.0) {
                CalculationEngine.calculateMarginPercent(p.sellPrice, p.buyPrice)
            } else if (p.buyPrice > 0.0) {
                -100.0
            } else {
                100.0
            }
            if (isLoss || (p.buyPrice > 0.0 && marginPct < thresholds.lowMarginPercent)) {
                allLowMargin.add(
                    StrategyLowMarginFact(
                        productName = p.name,
                        buyPrice = CalculationEngine.roundMoney(p.buyPrice),
                        sellPrice = CalculationEngine.roundMoney(p.sellPrice),
                        marginPercent = CalculationEngine.roundMoney(marginPct),
                        isLoss = isLoss
                    )
                )
            }
        }
        val lowMarginSorted = allLowMargin
            .sortedWith(compareByDescending<StrategyLowMarginFact> { it.isLoss }.thenBy { it.marginPercent })
        val lowMarginItems = lowMarginSorted.take(thresholds.lowMarginLimit)
        val lossCount = lowMarginSorted.count { it.isLoss }

        // 5. Weekly comparison: last 7 days ending today vs preceding 7 days
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }
        val thisWeekStart = cal.timeInMillis
        val lastWeekStart = thisWeekStart - (7L * DAY_MS)
        val lastWeekEnd = thisWeekStart - 1L

        val thisWeekSalesList = CalculationEngine.filterSalesByDateRange(sales, thisWeekStart, nowMs)
        val lastWeekSalesList = CalculationEngine.filterSalesByDateRange(sales, lastWeekStart, lastWeekEnd)
        val thisWeekTotal = CalculationEngine.roundMoney(thisWeekSalesList.sumOf { it.grandTotal })
        val lastWeekTotal = CalculationEngine.roundMoney(lastWeekSalesList.sumOf { it.grandTotal })
        val pctChange = if (lastWeekTotal > 0.0) {
            CalculationEngine.roundMoney(((thisWeekTotal - lastWeekTotal) / lastWeekTotal) * 100.0)
        } else {
            null
        }

        val stockSummary = CalculationEngine.calculateStockSummary(products)

        return BusinessStrategyFacts(
            topSellers = topSellers,
            slowItems = slowItems,
            slowItemsTotalCount = slowItemsSorted.size,
            slowItemsTotalStockValue = slowItemsTotalValue,
            overdueBakiCount = sortedOverdue.size,
            overdueBakiTotalAmount = overdueTotal,
            overdueBakiEntries = overdueEntries,
            lowMarginItems = lowMarginItems,
            lossItemsCount = lossCount,
            weeklyComparison = StrategyWeeklyComparison(
                thisWeekSales = thisWeekTotal,
                lastWeekSales = lastWeekTotal,
                thisWeekOrders = thisWeekSalesList.size.toLong(),
                lastWeekOrders = lastWeekSalesList.size.toLong(),
                changePercent = pctChange
            ),
            totalProductsCount = products.size,
            lowStockCount = (stockSummary.lowStockCount + stockSummary.outOfStockCount).toInt(),
            totalStockValueAtCost = CalculationEngine.roundMoney(stockSummary.stockValueAtCost),
            computedAtMs = nowMs
        )
    }

    /**
     * FIFO aging: subtracts total payments against chronological baki sales to find
     * the oldest still-unpaid baki timestamp.
     */
    private fun computeOldestUnpaidBakiTimestamp(
        customer: Customer,
        allSales: List<Sale>,
        allPayments: List<Payment>
    ): Long {
        val bakiSales = allSales
            .filter { CalculationEngine.doesSaleMatchCustomer(it, customer) && it.paymentType == "baki" }
            .sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
        var paymentsRemaining = allPayments
            .filter { CalculationEngine.doesPaymentMatchCustomer(it, customer) }
            .sumOf { it.amount.coerceAtLeast(0.0) }

        for (sale in bakiSales) {
            val amt = sale.grandTotal.coerceAtLeast(0.0)
            if (paymentsRemaining >= amt - 0.001) {
                paymentsRemaining = (paymentsRemaining - amt).coerceAtLeast(0.0)
            } else {
                val ts = sale.createdAt?.toDate()?.time ?: 0L
                if (ts > 0L) return ts
            }
        }
        return customer.createdAt?.toDate()?.time ?: 0L
    }
}

object BusinessStrategyBanglaBuilder {

    fun formatDateKey(nowMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))
    }

    fun formatBanglaTime(timeMs: Long): String {
        if (timeMs <= 0L) return "এখন"
        val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val period = when (hour24) {
            in 4..11 -> "সকাল"
            in 12..15 -> "দুপুর"
            in 16..18 -> "বিকাল"
            in 19..23 -> "রাত"
            else -> "রাত"
        }
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val hStr = ShopLocale.toBengaliDigits(hour12.toString())
        val mStr = ShopLocale.toBengaliDigits(String.format(Locale.US, "%02d", minute))
        return "$period $hStr:$mStr"
    }

    fun formatTakaBn(amount: Double): String {
        val rounded = CalculationEngine.roundMoney(amount)
        val numStr = if (abs(rounded - rounded.roundToLong()) < 0.01) {
            rounded.roundToLong().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded)
        }
        return "৳${ShopLocale.toBengaliDigits(numStr)}"
    }

    fun formatNumBn(value: Double): String {
        val clean = if (abs(value - value.roundToLong()) < 0.01) {
            value.roundToLong().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
        return ShopLocale.toBengaliDigits(clean)
    }

    fun formatIntBn(value: Int): String = ShopLocale.toBengaliDigits(value.toString())

    fun actionButtonLabel(target: StrategyActionTarget): String = when (target) {
        StrategyActionTarget.SELL -> "বিক্রি করুন"
        StrategyActionTarget.STOCK -> "মাল দেখুন"
        StrategyActionTarget.LOW_STOCK -> "কম মাল দেখুন"
        StrategyActionTarget.BAKI -> "বাকি দেখুন"
        StrategyActionTarget.REPORTS -> "হিসাব দেখুন"
    }

    /**
     * Always produces clear, simple Bangla advice using ONLY numbers calculated by CalculationEngine.
     * Contains no English business words and never returns blank.
     */
    fun buildFixedStrategy(
        storeId: String,
        facts: BusinessStrategyFacts,
        generatedAtMs: Long = System.currentTimeMillis(),
        refreshCountToday: Int = 0,
        updatedBy: String = "",
        thresholds: BusinessStrategyThresholds = BusinessStrategyDefaults.THRESHOLDS
    ): SavedBusinessStrategy {
        val weekly = facts.weeklyComparison
        val diagnosis = when {
            weekly.thisWeekSales <= 0.0 && weekly.lastWeekSales <= 0.0 -> {
                if (facts.totalProductsCount > 0) {
                    "এই সপ্তাহে এখনো বিক্রি শুরু হয়নি, দোকানে ${formatTakaBn(facts.totalStockValueAtCost)} টাকার মাল আছে।"
                } else {
                    "দোকানে এখনো মাল ও বিক্রির হিসাব যোগ হয়নি, আগে পণ্য ও বিক্রি যোগ করুন।"
                }
            }
            weekly.lastWeekSales <= 0.0 -> {
                "এই সপ্তাহে ${formatTakaBn(weekly.thisWeekSales)} বিক্রি হয়েছে ও ${formatIntBn(weekly.thisWeekOrders.toInt())}টি বিল হয়েছে।"
            }
            weekly.changePercent != null && weekly.changePercent >= 5.0 -> {
                val pct = formatNumBn(abs(weekly.changePercent))
                "এই সপ্তাহে বিক্রি ${formatTakaBn(weekly.thisWeekSales)}, যা গত সপ্তাহের ${formatTakaBn(weekly.lastWeekSales)} থেকে $pct% বেশি।"
            }
            weekly.changePercent != null && weekly.changePercent <= -5.0 -> {
                val pct = formatNumBn(abs(weekly.changePercent))
                "এই সপ্তাহে বিক্রি ${formatTakaBn(weekly.thisWeekSales)}, যা গত সপ্তাহের ${formatTakaBn(weekly.lastWeekSales)} থেকে $pct% কম।"
            }
            else -> {
                "এই সপ্তাহে বিক্রি ${formatTakaBn(weekly.thisWeekSales)} এবং গত সপ্তাহে ছিল ${formatTakaBn(weekly.lastWeekSales)}।"
            }
        }

        val focusLine = when {
            facts.lossItemsCount > 0 -> {
                val firstLoss = facts.lowMarginItems.first()
                "${firstLoss.productName} কেনা দামের চেয়ে কমে বিক্রি হচ্ছে, আগে এর দাম ঠিক করা দরকার।"
            }
            facts.overdueBakiCount > 0 -> {
                "${formatIntBn(facts.overdueBakiCount)} জন ক্রেতার কাছে ${formatIntBn(thresholds.overdueBakiDays)} দিনের বেশি ${formatTakaBn(facts.overdueBakiTotalAmount)} বাকি পড়ে আছে।"
            }
            facts.lowMarginItems.isNotEmpty() -> {
                val low = facts.lowMarginItems.first()
                "${low.productName}-এ লাভ ${formatNumBn(low.marginPercent)}% থাকায় আয় কম হচ্ছে।"
            }
            facts.slowItemsTotalCount > 0 -> {
                "${formatIntBn(facts.slowItemsTotalCount)}টি মালে ${formatIntBn(thresholds.slowMovingDays)} দিন বিক্রি নেই, ${formatTakaBn(facts.slowItemsTotalStockValue)} টাকা আটকে আছে।"
            }
            facts.lowStockCount > 0 -> {
                "দোকানের ${formatIntBn(facts.lowStockCount)}টি মাল প্রায় শেষ হয়ে গেছে, দ্রুত তোলা দরকার।"
            }
            facts.topSellers.isNotEmpty() -> {
                val top = facts.topSellers.first()
                "সবচেয়ে বেশি চলছে ${top.productName} (বিক্রি ${formatTakaBn(top.salesAmount)}), এখন বাকি আছে ${formatNumBn(top.stockLeft)}টি।"
            }
            else -> {
                "দোকানের বিক্রি ও মালের হিসাব নিয়মিত রাখলে লাভ ভালো থাকবে।"
            }
        }

        val actions = mutableListOf<StrategyActionAdvice>()

        // Candidate 1: Loss or <10% margin products
        val lossOrLowMargin = facts.lowMarginItems.firstOrNull()
        if (lossOrLowMargin != null) {
            val text = if (lossOrLowMargin.isLoss) {
                "${lossOrLowMargin.productName} এর বিক্রির দাম বাড়ান, এখন ${formatTakaBn(lossOrLowMargin.buyPrice)} কেনায় ${formatTakaBn(lossOrLowMargin.sellPrice)} বিক্রি হচ্ছে।"
            } else {
                "${lossOrLowMargin.productName} এর দাম দেখুন, কারণ এতে লাভ মাত্র ${formatNumBn(lossOrLowMargin.marginPercent)}% থাকছে।"
            }
            actions.add(
                StrategyActionAdvice(
                    text = text,
                    target = StrategyActionTarget.STOCK.wireKey,
                    buttonLabel = actionButtonLabel(StrategyActionTarget.STOCK)
                )
            )
        }

        // Candidate 2: Overdue Baki > 60 days
        if (facts.overdueBakiCount > 0) {
            actions.add(
                StrategyActionAdvice(
                    text = "${formatIntBn(thresholds.overdueBakiDays)} দিনের পুরনো ${formatTakaBn(facts.overdueBakiTotalAmount)} বাকি তুলুন, ${formatIntBn(facts.overdueBakiCount)} জন ক্রেতার টাকা আটকে আছে।",
                    target = StrategyActionTarget.BAKI.wireKey,
                    buttonLabel = actionButtonLabel(StrategyActionTarget.BAKI)
                )
            )
        }

        // Candidate 3: Slow items no sale in 30 days
        val firstSlow = facts.slowItems.firstOrNull()
        if (firstSlow != null && actions.size < 3) {
            actions.add(
                StrategyActionAdvice(
                    text = "${firstSlow.productName} সহ ${formatIntBn(facts.slowItemsTotalCount)}টি অচল মালে ছাড় দিয়ে ছাড়ুন, নইলে ${formatTakaBn(facts.slowItemsTotalStockValue)} টাকা আটকে থাকবে।",
                    target = StrategyActionTarget.STOCK.wireKey,
                    buttonLabel = actionButtonLabel(StrategyActionTarget.STOCK)
                )
            )
        }

        // Candidate 4: Top sellers & stock left / low stock
        val topSeller = facts.topSellers.firstOrNull()
        if (actions.size < 3 && (facts.lowStockCount > 0 || topSeller != null)) {
            if (facts.lowStockCount > 0) {
                actions.add(
                    StrategyActionAdvice(
                        text = "${formatIntBn(facts.lowStockCount)}টি দরকারি মাল কম আছে, ক্রেতা ফিরে যাওয়ার আগেই দোকানে তুলুন।",
                        target = StrategyActionTarget.LOW_STOCK.wireKey,
                        buttonLabel = actionButtonLabel(StrategyActionTarget.LOW_STOCK)
                    )
                )
            } else if (topSeller != null) {
                actions.add(
                    StrategyActionAdvice(
                        text = "${topSeller.productName} ভালো চলছে, হাতে থাকা ${formatNumBn(topSeller.stockLeft)}টি শেষ হওয়ার আগে আরও আনুন।",
                        target = StrategyActionTarget.STOCK.wireKey,
                        buttonLabel = actionButtonLabel(StrategyActionTarget.STOCK)
                    )
                )
            }
        }

        // Candidate 5: Weekly sales push or report check so we always have 3 useful actions
        if (actions.size < 3) {
            actions.add(
                StrategyActionAdvice(
                    text = "এই সপ্তাহের ${formatTakaBn(weekly.thisWeekSales)} বিক্রির সাথে লাভ-ক্ষতি মিলিয়ে দেখুন, কোথায় খরচ বা কম দাম হচ্ছে বোঝা যাবে।",
                    target = StrategyActionTarget.REPORTS.wireKey,
                    buttonLabel = actionButtonLabel(StrategyActionTarget.REPORTS)
                )
            )
        }
        if (actions.size < 3) {
            actions.add(
                StrategyActionAdvice(
                    text = "নতুন বিক্রি দ্রুত তুলে ধরুন ও নগদ লেনদেন বাড়ান, এতে দোকানের টাকার চলাচল বাড়বে।",
                    target = StrategyActionTarget.SELL.wireKey,
                    buttonLabel = actionButtonLabel(StrategyActionTarget.SELL)
                )
            )
        }
        if (actions.size < 3) {
            actions.add(
                StrategyActionAdvice(
                    text = "মালের তালিকায় দাম ও সংখ্যা যাচাই করে নিন, যাতে ভুল দামে বিক্রি না হয়।",
                    target = StrategyActionTarget.STOCK.wireKey,
                    buttonLabel = actionButtonLabel(StrategyActionTarget.STOCK)
                )
            )
        }

        return SavedBusinessStrategy(
            id = "today",
            storeId = storeId,
            dateKey = formatDateKey(generatedAtMs),
            diagnosis = diagnosis,
            focusLine = focusLine,
            actions = actions.take(3),
            generatedAtMs = generatedAtMs,
            refreshCountToday = refreshCountToday,
            source = "fixed",
            updatedBy = updatedBy
        )
    }

    /**
     * Strictly anonymized prompt facts: NO customer names and NO customer phone numbers.
     */
    fun buildPromptFactText(
        facts: BusinessStrategyFacts,
        thresholds: BusinessStrategyThresholds = BusinessStrategyDefaults.THRESHOLDS
    ): String {
        val sb = StringBuilder()
        sb.appendLine("সাপ্তাহিক বিক্রি:")
        sb.appendLine("- এই সপ্তাহ (৭ দিন): ${formatTakaBn(facts.weeklyComparison.thisWeekSales)} (${formatIntBn(facts.weeklyComparison.thisWeekOrders.toInt())} বিল)")
        sb.appendLine("- গত সপ্তাহ: ${formatTakaBn(facts.weeklyComparison.lastWeekSales)} (${formatIntBn(facts.weeklyComparison.lastWeekOrders.toInt())} বিল)")
        facts.weeklyComparison.changePercent?.let {
            val dir = if (it >= 0.0) "বেশি" else "কম"
            sb.appendLine("- পরিবর্তন: ${formatNumBn(abs(it))}% $dir")
        }

        sb.appendLine("\nসবচেয়ে বেশি চলা মাল ও হাতে থাকা সংখ্যা:")
        if (facts.topSellers.isEmpty()) {
            sb.appendLine("- কোনো তথ্য নেই")
        } else {
            facts.topSellers.forEach { t ->
                sb.appendLine("- ${t.productName}: বিক্রি ${formatNumBn(t.qtySold)} (${formatTakaBn(t.salesAmount)}), হাতে আছে ${formatNumBn(t.stockLeft)}")
            }
        }

        sb.appendLine("\n${formatIntBn(thresholds.slowMovingDays)} দিন বিক্রি না হওয়া মাল (মোট আটকা মূল্য ${formatTakaBn(facts.slowItemsTotalStockValue)}):")
        if (facts.slowItems.isEmpty()) {
            sb.appendLine("- নেই")
        } else {
            facts.slowItems.forEach { s ->
                sb.appendLine("- ${s.productName}: সংখ্যা ${formatNumBn(s.stockQty)}, আটকা মূল্য ${formatTakaBn(s.stockValueAtCost)}")
            }
        }

        sb.appendLine("\n${formatIntBn(thresholds.overdueBakiDays)} দিনের বেশি পুরনো বাকি (মোট ${formatIntBn(facts.overdueBakiCount)} জন ক্রেতা, মোট ${formatTakaBn(facts.overdueBakiTotalAmount)}):")
        if (facts.overdueBakiEntries.isEmpty()) {
            sb.appendLine("- নেই")
        } else {
            facts.overdueBakiEntries.forEach { b ->
                sb.appendLine("- ক্রেতা ${formatIntBn(b.anonymizedRef)}: বাকি ${formatTakaBn(b.amountOwed)} (${formatIntBn(b.daysOverdue)} দিন)")
            }
        }

        sb.appendLine("\n${formatNumBn(thresholds.lowMarginPercent)}% এর নিচে লাভ বা লোকসানে বিক্রি হওয়া মাল:")
        if (facts.lowMarginItems.isEmpty()) {
            sb.appendLine("- নেই")
        } else {
            facts.lowMarginItems.forEach { m ->
                val stateLabel = if (m.isLoss) "লোকসান" else "কম লাভ (${formatNumBn(m.marginPercent)}%)"
                sb.appendLine("- ${m.productName}: কেনা ${formatTakaBn(m.buyPrice)}, বিক্রি ${formatTakaBn(m.sellPrice)} - $stateLabel")
            }
        }

        sb.appendLine("\nকম হওয়া মাল সংখ্যা: ${formatIntBn(facts.lowStockCount)}টি")
        return sb.toString().trim()
    }

    fun buildGeminiSystemPrompt(): String {
        return """
আপনি একজন বাংলাদেশি ছোট দোকানের সহজ-সরল ব্যবসার পরামর্শদাতা।
নিয়মগুলো অবশ্যই মানতে হবে:
১. সব লেখা কেবল সহজ, ছোট বাংলা বাক্যে লিখবেন, যাতে কম পড়াশোনা জানা দোকানদারও সহজে বুঝতে পারে।
২. কোনো ইংরেজি ব্যবসার শব্দ বা ইংরেজি হরফ লিখবেন না। যেমন "মাল" লিখবেন ("inventory" বা "stock" নয়), "লাভ" লিখবেন ("margin" বা "profit" নয়), "বাকি" লিখবেন ("receivable" বা "due" নয়), "ক্রেতা" লিখবেন ("customer" নয়), "বিক্রি" লিখবেন ("sales" নয়), "দাম" লিখবেন ("price" নয়), "হিসাব" লিখবেন ("report" নয়)।
৩. সব সংখ্যা বাংলা অঙ্কে (০, ১, ২, ৩, ৪, ৫, ৬, ৭, ৮, ৯) ও টাকার চিহ্ন ৳ দিয়ে লিখবেন (যেমন ৳৫০০)।
৪. কখনোই নিজে কোনো সংখ্যা বানাবেন না; শুধু প্রদত্ত তথ্যের সংখ্যাই ব্যবহার করবেন।
৫. কেবল নিচের JSON ফরম্যাটে উত্তর দেবেন:
{
  "diagnosis": "১টি ছোট বাংলা বাক্যে দোকানের বর্তমান অবস্থা",
  "focusLine": "১টি ছোট বাংলা বাক্যে আজকের সবচেয়ে জরুরি বিষয়",
  "actions": [
    { "text": "১ লাইনে কী করবেন ও কেন করবেন", "target": "STOCK" },
    { "text": "১ লাইনে কী করবেন ও কেন করবেন", "target": "BAKI" },
    { "text": "১ লাইনে কী করবেন ও কেন করবেন", "target": "LOW_STOCK" }
  ]
}
"target" শুধু এই ৫টির একটি হতে পারবে: "SELL", "STOCK", "LOW_STOCK", "BAKI", "REPORTS"।
"actions" এ ঠিক ৩টি করণীয় থাকতে হবে, প্রতিটা মাত্র ১ লাইনের।
        """.trimIndent()
    }

    /**
     * Parses AI JSON strictly; falls back cleanly to fixed Bangla strategy if invalid or contains forbidden English jargon.
     */
    fun parseAiJsonToStrategy(
        rawJsonText: String,
        storeId: String,
        facts: BusinessStrategyFacts,
        generatedAtMs: Long,
        refreshCountToday: Int,
        updatedBy: String,
        thresholds: BusinessStrategyThresholds = BusinessStrategyDefaults.THRESHOLDS
    ): SavedBusinessStrategy {
        val fallback = buildFixedStrategy(
            storeId = storeId,
            facts = facts,
            generatedAtMs = generatedAtMs,
            refreshCountToday = refreshCountToday,
            updatedBy = updatedBy,
            thresholds = thresholds
        )
        return try {
            val cleaned = rawJsonText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val json = JSONObject(cleaned)
            val rawDiag = sanitizeBanglaSentence(json.optString("diagnosis", ""))
            val rawFocus = sanitizeBanglaSentence(json.optString("focusLine", ""))
            val arr = json.optJSONArray("actions")
            if (rawDiag.isBlank() || arr == null || arr.length() == 0) {
                return fallback
            }

            val parsedActions = mutableListOf<StrategyActionAdvice>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val txt = sanitizeBanglaSentence(obj.optString("text", ""))
                if (txt.isBlank()) continue
                val target = StrategyActionTarget.fromWireKey(obj.optString("target", "STOCK"))
                parsedActions.add(
                    StrategyActionAdvice(
                        text = txt,
                        target = target.wireKey,
                        buttonLabel = actionButtonLabel(target)
                    )
                )
                if (parsedActions.size == 3) break
            }

            while (parsedActions.size < 3 && fallback.actions.size > parsedActions.size) {
                parsedActions.add(fallback.actions[parsedActions.size])
            }
            if (parsedActions.isEmpty()) return fallback

            SavedBusinessStrategy(
                id = "today",
                storeId = storeId,
                dateKey = formatDateKey(generatedAtMs),
                diagnosis = rawDiag,
                focusLine = rawFocus.ifBlank { fallback.focusLine },
                actions = parsedActions.take(3),
                generatedAtMs = generatedAtMs,
                refreshCountToday = refreshCountToday,
                source = "ai",
                updatedBy = updatedBy
            )
        } catch (_: Exception) {
            fallback
        }
    }

    private val forbiddenEnglishBusinessTerms = listOf(
        "inventory" to "মাল",
        "stock" to "মাল",
        "margin" to "লাভ",
        "profit" to "লাভ",
        "receivable" to "বাকি",
        "credit" to "বাকি",
        "customer" to "ক্রেতা",
        "client" to "ক্রেতা",
        "revenue" to "বিক্রি",
        "sales" to "বিক্রি",
        "sale" to "বিক্রি",
        "report" to "হিসাব",
        "price" to "দাম",
        "strategy" to "পরামর্শ",
        "shop" to "দোকান",
        "store" to "দোকান"
    )

    internal fun sanitizeBanglaSentence(raw: String): String {
        var text = raw.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        for ((eng, bn) in forbiddenEnglishBusinessTerms) {
            text = text.replace(Regex("\\b$eng\\b", RegexOption.IGNORE_CASE), bn)
        }
        return ShopLocale.toBengaliDigits(text)
    }
}
