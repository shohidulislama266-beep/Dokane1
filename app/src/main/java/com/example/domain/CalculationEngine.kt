package com.example.domain

import com.example.data.model.Account
import com.example.data.model.AccountAdjustment
import com.example.data.model.AccountStatementEntry
import com.example.data.model.AccountStatementReport
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.OtherIncome
import com.example.data.model.Payment
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseItem
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.Supplier
import com.example.data.model.SupplierPayment
import com.example.ui.viewmodel.HistoryPeriod
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class StoreFinancials(
    val todaySales: Double,
    val todayProfit: Double,
    val todayOrdersCount: Long,
    val todayItemsSold: Double,
    val stockValueAtCost: Double,
    val cashBalance: Double
)

data class StockSummary(
    val totalItems: Long,
    val lowStockCount: Long,
    val outOfStockCount: Long,
    val stockValueAtCost: Double
)

data class PeriodFinancialSummary(
    val totalSold: Double,
    val totalBought: Double,
    val totalProfit: Double,
    val totalOrders: Long,
    val totalItemsSold: Double
)

data class ProductSoldStat(
    val productId: String,
    val productName: String,
    val unit: String,
    val quantitySold: Double,
    val totalSalesAmount: Double,
    val totalProfit: Double
)

data class StaffPerformanceRow(
    val memberId: String,
    val memberName: String,
    val roleLabel: String,
    val salesCount: Long,
    val totalSales: Double,
    val itemsSold: Double,
    val profit: Double,
    val paymentsReceived: Double
)

data class StaffReportSummary(
    val rows: List<StaffPerformanceRow>,
    val totalSalesCount: Long,
    val totalSalesAmount: Double,
    val totalItemsSold: Double,
    val totalProfit: Double,
    val totalPaymentsReceived: Double
)

enum class LedgerEntryType {
    BAKI_SALE,
    PAYMENT
}

data class CustomerLedgerEntry(
    val id: String,
    val type: LedgerEntryType,
    val referenceNo: String,
    val timestamp: Long,
    val amount: Double,
    val note: String,
    val runningBalance: Double,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0
)

data class CustomerBalanceSummary(
    val customer: Customer,
    val totalBakiSales: Double,
    val totalPayments: Double,
    val currentBalance: Double,
    val totalPurchases: Double,
    val lastPurchaseDate: Long?
)

data class BakiOverview(
    val totalBakiOwed: Double,
    val debtorCount: Long,
    val debtors: List<CustomerBalanceSummary>
)

data class DailySalesPoint(
    val label: String,
    val timestamp: Long,
    val salesAmount: Double,
    val profitAmount: Double,
    val orderCount: Long
)

data class CategorySalesStat(
    val category: String,
    val salesAmount: Double,
    val itemsCount: Double,
    val percentage: Double
)

data class MoneyNowSummary(
    val stockValueAtCost: Double,
    val bakiCustomersOwe: Double,
    val debtToSuppliers: Double,
    val debtorsCount: Long,
    val suppliersCount: Long,
    val totalCashAndBankBalance: Double = 0.0
)

data class ProfitAndLossStatement(
    val netSales: Double,
    val costOfGoodsSold: Double,
    val grossProfit: Double,
    val expenses: Double,
    val otherIncome: Double = 0.0,
    val netProfit: Double,
    val isStockBased: Boolean,
    val expensesByCategory: Map<String, Double> = emptyMap()
)

data class DayBookEntry(
    val id: String,
    val timestamp: Long,
    val type: String, // "SALE_CASH", "SALE_BAKI", "PURCHASE_PAID", "PURCHASE_CREDIT", "PAYMENT_IN", "PURCHASE_RETURN", "PAYMENT_OUT", "EXPENSE", "OTHER_INCOME", "ADJUSTMENT"
    val referenceNo: String,
    val partyName: String,
    val amount: Double,
    val isIncome: Boolean,
    val note: String,
    val accountName: String = ""
)

data class CategoryExpenseStat(
    val category: String,
    val amount: Double,
    val count: Long,
    val percentage: Double
)

data class ExpenseReportData(
    val period: HistoryPeriod,
    val startDate: Long,
    val endDate: Long,
    val totalExpenses: Double,
    val categoryStats: List<CategoryExpenseStat>,
    val expenses: List<Expense>
)

data class IncomeReportData(
    val period: HistoryPeriod,
    val startDate: Long,
    val endDate: Long,
    val totalIncome: Double,
    val categoryStats: List<CategoryExpenseStat>,
    val incomes: List<OtherIncome>
)

data class CustomerStatementReport(
    val customer: Customer,
    val period: HistoryPeriod,
    val startDate: Long,
    val endDate: Long,
    val openingBalance: Double,
    val entries: List<CustomerLedgerEntry>,
    val totalBakiSales: Double,
    val totalPayments: Double,
    val closingBalance: Double
)

data class SupplierStatementEntry(
    val id: String,
    val timestamp: Long,
    val type: String, // "CREDIT_PURCHASE", "PAYMENT_OUT", "PURCHASE_RETURN"
    val referenceNo: String,
    val amount: Double,
    val note: String,
    val runningBalance: Double
)

data class SupplierStatementReport(
    val supplier: Supplier,
    val period: HistoryPeriod,
    val startDate: Long,
    val endDate: Long,
    val openingBalance: Double,
    val entries: List<SupplierStatementEntry>,
    val totalPurchases: Double,
    val totalPayments: Double,
    val totalReturns: Double,
    val closingBalance: Double
)

data class MonthlyCustomerStat(
    val customerName: String,
    val customerPhone: String,
    val totalBought: Double,
    val paidAmount: Double,
    val bakiAmount: Double
)

data class MonthlyDailySalesRow(
    val dateLabel: String,
    val timestamp: Long,
    val orders: Long,
    val itemsSold: Double,
    val salesAmount: Double,
    val profitAmount: Double
)

data class MonthlyCategorySalesRow(
    val category: String,
    val itemsCount: Double,
    val salesAmount: Double,
    val profitAmount: Double,
    val percentage: Double
)

data class MonthlyDebtorRow(
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val amountOwed: Double,
    val daysOverdue: Long,
    val ageingBucket: String = "0-30"
)

data class MonthlyBakiAgeing(
    val bucket0To30: Double = 0.0,
    val bucket31To60: Double = 0.0,
    val bucket60Plus: Double = 0.0,
    val bucket61To90: Double = 0.0,
    val bucket90Plus: Double = 0.0
)

data class MonthlySupplierOwedRow(
    val supplierId: String,
    val supplierName: String,
    val supplierPhone: String,
    val amountOwed: Double
)

data class MonthlySupplierRow(
    val supplierId: String,
    val supplierName: String,
    val supplierPhone: String,
    val opening: Double,
    val creditPurchases: Double,
    val totalPurchases: Double,
    val paidAtPurchase: Double,
    val paymentsOut: Double,
    val returns: Double,
    val closing: Double
)

data class MonthlyPurchaseRow(
    val purchaseId: String,
    val purchaseNo: String,
    val dateMillis: Long,
    val supplierName: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val creditAmount: Double,
    val paymentType: String
)

data class MonthlyReturnRow(
    val returnId: String,
    val returnNo: String,
    val dateMillis: Long,
    val supplierName: String,
    val amount: Double,
    val reason: String
)

data class MonthlyLowStockRow(
    val productId: String,
    val productName: String,
    val stock: Double,
    val lowStockLimit: Double,
    val unit: String,
    val stockValueAtCost: Double
)

data class MonthlySlowStockRow(
    val productId: String,
    val productName: String,
    val stock: Double,
    val unit: String,
    val moneyStuck: Double
)

data class MonthlyExpenseCategoryRow(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val count: Int = 0
)

data class MonthlyIncomeCategoryRow(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val count: Int = 0
)

data class MonthlyAccountCashRow(
    val accountId: String,
    val accountName: String,
    val accountType: String,
    val opening: Double,
    val moneyIn: Double,
    val moneyOut: Double,
    val closing: Double
)

data class MonthlyDebtorAgeingGroup(
    val bucketKey: String, // "0-30", "31-60", "61-90", "90+"
    val debtors: List<MonthlyDebtorRow> = emptyList(),
    val count: Long = 0L,
    val totalAmount: Double = 0.0
)

data class MonthlyReconciliationCheck(
    val key: String, // "summary", "summary_no_profit", "sales", "baki", "suppliers", "stock", "purchases", "expenses_income", "cash"
    val opening: Double = 0.0,
    val totalIn: Double = 0.0,
    val totalOut: Double = 0.0,
    val actualClosing: Double = 0.0,
    val expectedClosing: Double = 0.0,
    val difference: Double = 0.0,
    val matches: Boolean = true,
    val isAvailable: Boolean = true,
    val note: String = ""
)

data class MonthlyReportData(
    val year: Int,
    val month: Int,
    val monthLabel: String,
    val totalSales: Double,
    val totalProfit: Double,
    val totalOrders: Long,
    val totalItemsSold: Double,
    val stockValueAtCost: Double,
    val bakiCustomersOwe: Double,
    val debtToSuppliers: Double,
    val customerStats: List<MonthlyCustomerStat>,
    val itemsSold: List<ProductSoldStat>,
    // 1 & 2. Sales & P&L
    val grossSales: Double = totalSales,
    val totalDiscounts: Double = 0.0,
    val netSales: Double = totalSales,
    val averageOrderValue: Double = 0.0,
    val cashReceivedSales: Double = 0.0,
    val bakiCreditSales: Double = 0.0,
    val topProductsByAmount: List<ProductSoldStat> = itemsSold.sortedByDescending { it.totalSalesAmount }.take(5),
    val topCustomersByAmount: List<MonthlyCustomerStat> = emptyList(),
    val dailySalesRows: List<MonthlyDailySalesRow> = emptyList(),
    val salesByCategoryRows: List<MonthlyCategorySalesRow> = emptyList(),
    val salesByCategoryTotalItems: Double = 0.0,
    val salesByCategoryTotalSales: Double = 0.0,
    val salesByCategoryTotalProfit: Double = 0.0,
    val costOfGoodsSold: Double = 0.0,
    val grossProfit: Double = totalProfit,
    val totalExpenses: Double = 0.0,
    val otherIncome: Double = 0.0,
    val netProfit: Double = totalProfit,
    // 3. Baki / Credit
    val bakiOpeningReceivable: Double = 0.0,
    val bakiNewCreditGiven: Double = 0.0,
    val bakiCollectedThisMonth: Double = 0.0,
    val bakiClosingReceivable: Double = bakiCustomersOwe,
    val debtorRows: List<MonthlyDebtorRow> = emptyList(),
    val debtorRowsGrouped: List<MonthlyDebtorRow> = debtorRows,
    val debtorCount: Long = debtorRows.size.toLong(),
    val bakiAgeingGroups: List<MonthlyDebtorAgeingGroup> = emptyList(),
    val bakiAgeing: MonthlyBakiAgeing = MonthlyBakiAgeing(),
    val bakiHasActivityOrDue: Boolean = false,
    // 4. Suppliers
    val supplierOpeningPayable: Double = 0.0,
    val supplierPurchasesThisMonth: Double = 0.0,
    val supplierCreditPurchasesThisMonth: Double = 0.0,
    val supplierPaidAtPurchaseThisMonth: Double = 0.0,
    val supplierPaidThisMonth: Double = 0.0,
    val supplierPaymentsOutThisMonth: Double = 0.0,
    val supplierReturnsThisMonth: Double = 0.0,
    val supplierClosingPayable: Double = debtToSuppliers,
    val supplierOwedRows: List<MonthlySupplierOwedRow> = emptyList(),
    val supplierRows: List<MonthlySupplierRow> = emptyList(),
    val supplierRowCount: Long = supplierRows.size.toLong(),
    val supplierHasActivityOrDue: Boolean = false,
    // 5. Stock
    val openingStockValue: Double = stockValueAtCost,
    val openingStockValueComputed: Double? = null,
    val openingStockAvailable: Boolean = false,
    val purchasedStockValue: Double = 0.0,
    val soldStockAtCost: Double = 0.0,
    val closingStockValue: Double = stockValueAtCost,
    val stockItemCount: Long = 0L,
    val totalStockUnits: Double = 0.0,
    val lowStockItems: List<MonthlyLowStockRow> = emptyList(),
    val lowStockCount: Long = lowStockItems.size.toLong(),
    val slowStock30DayItems: List<MonthlySlowStockRow> = emptyList(),
    val slowStockCount: Long = slowStock30DayItems.size.toLong(),
    val totalSlowStockMoneyStuck: Double = 0.0,
    // 6. Purchases
    val totalBought: Double = supplierPurchasesThisMonth,
    val purchaseReturnsTotal: Double = supplierReturnsThisMonth,
    val netPurchases: Double = CalculationEngine.roundMoney(supplierPurchasesThisMonth - supplierReturnsThisMonth),
    val purchaseRows: List<MonthlyPurchaseRow> = emptyList(),
    val returnRows: List<MonthlyReturnRow> = emptyList(),
    val purchaseHasActivity: Boolean = false,
    // 7. Expenses & Other Income
    val expensesByCategory: List<MonthlyExpenseCategoryRow> = emptyList(),
    val totalExpenseCount: Long = 0L,
    val incomesByCategory: List<MonthlyIncomeCategoryRow> = emptyList(),
    val totalOtherIncomeCount: Long = 0L,
    val netOtherIncomeMinusExpenses: Double = 0.0,
    // 8. Cash & Bank
    val accountCashRows: List<MonthlyAccountCashRow> = emptyList(),
    val cashOpeningTotal: Double = 0.0,
    val cashInTotal: Double = 0.0,
    val cashOutTotal: Double = 0.0,
    val cashClosingBalance: Double = 0.0,
    // Checks (every section)
    val summaryCheck: MonthlyReconciliationCheck = MonthlyReconciliationCheck("summary"),
    val summaryCheckNoProfit: MonthlyReconciliationCheck = MonthlyReconciliationCheck("summary_no_profit"),
    val salesCheck: MonthlyReconciliationCheck = MonthlyReconciliationCheck("sales"),
    val cashCheck: MonthlyReconciliationCheck = MonthlyReconciliationCheck("cash"),
    val bakiCheck: MonthlyReconciliationCheck = MonthlyReconciliationCheck("baki"),
    val supplierCheck: MonthlyReconciliationCheck = MonthlyReconciliationCheck("suppliers"),
    val stockCheck: MonthlyReconciliationCheck = MonthlyReconciliationCheck("stock", isAvailable = false),
    val purchasesCheck: MonthlyReconciliationCheck = MonthlyReconciliationCheck("purchases"),
    val expensesIncomeCheck: MonthlyReconciliationCheck = MonthlyReconciliationCheck("expenses_income"),
    val isMonthEmpty: Boolean = false
)

enum class HealthProblemType {
    NEGATIVE_STOCK,
    SELL_PRICE_LOWER_THAN_BUY_PRICE,
    DUPLICATE_PRODUCT_BARCODE_OR_NAME,
    DUPLICATE_PHONE_NUMBER,
    ORPHAN_TRANSACTION,
    CUSTOMER_BALANCE_MISMATCH,
    SUPPLIER_BALANCE_MISMATCH,
    PRODUCT_NO_CATEGORY,
    WRONG_SALE_TOTAL,
    MISSING_ZERO_PRICE,
    ORPHAN_REFERENCE,
    DUPLICATE_NUMBER,
    TRANSACTION_INVALID_ACCOUNT,
    NEGATIVE_ACCOUNT_BALANCE,
    ACCOUNT_STATEMENT_MISMATCH
}

data class HealthProblem(
    val id: String,
    val type: HealthProblemType,
    val title: String,
    val description: String,
    val recordRef: String,
    val entityCollection: String,
    val entityId: String,
    val fixDescription: String,
    val fixActionPayload: Map<String, Any?> = emptyMap()
)

data class DailyCashflowPoint(
    val dayLabel: String,
    val timestamp: Long,
    val cashIn: Double,
    val cashOut: Double
)

data class SevenDayCashflowSummary(
    val points: List<DailyCashflowPoint>,
    val totalIn: Double,
    val totalOut: Double
)

data class HomeInsights(
    val yesterdaySales: Double,
    val salesChangePercent: Double?,
    val todaySparklinePoints: List<Float>
)

object CalculationEngine {

    const val STOCK_EPSILON = 1e-6

    val SUPPORTED_UNITS: List<String> = listOf(
        "pcs", "dozen", "box", "pack", "bag", "carton",
        "kg", "gram", "litre", "ml", "meter", "feet"
    )

    fun roundStock(value: Double): Double {
        if (!value.isFinite()) return 0.0
        val clean = if (kotlin.math.abs(value) < 1e-9) 0.0 else value
        return BigDecimal.valueOf(clean).setScale(6, RoundingMode.HALF_UP).toDouble()
    }

    fun roundMoney(value: Double): Double {
        if (!value.isFinite()) return 0.0
        val clean = if (kotlin.math.abs(value) < 1e-9) 0.0 else value
        return BigDecimal.valueOf(clean).setScale(4, RoundingMode.HALF_UP).toDouble()
    }

    fun effectiveStockUnit(product: Product): String {
        return product.unit.trim().ifBlank { "pcs" }
    }

    fun effectiveBuyUnit(product: Product): String {
        return product.buyUnit.trim().ifBlank { effectiveStockUnit(product) }
    }

    fun effectiveBuyConversion(product: Product): Double {
        val stockUnit = effectiveStockUnit(product)
        val buyUnit = effectiveBuyUnit(product)
        if (buyUnit.equals(stockUnit, ignoreCase = true) || product.buyConversion <= 0.0) {
            return 1.0
        }
        return roundStock(product.buyConversion)
    }

    fun defaultSubUnitConversion(stockUnit: String, sellUnit: String): Double? {
        return when (stockUnit.trim().lowercase() to sellUnit.trim().lowercase()) {
            "kg" to "gram", "kg" to "gm" -> 1000.0
            "litre" to "ml", "ltr" to "ml" -> 1000.0
            "dozen" to "pcs", "doz" to "pcs" -> 12.0
            "meter" to "feet" -> 3.2808
            else -> null
        }
    }

    fun availableSellUnits(product: Product): List<Pair<String, Double>> {
        val stockUnit = effectiveStockUnit(product)
        val result = mutableListOf(stockUnit to 1.0)
        for ((unitName, factor) in product.sellUnits) {
            val clean = unitName.trim()
            if (clean.isNotBlank() &&
                !clean.equals(stockUnit, ignoreCase = true) &&
                factor > 0.0
            ) {
                result.add(clean to roundStock(factor))
            }
        }
        return result
    }

    fun sellFactorForUnit(product: Product, sellUnit: String): Double {
        val stockUnit = effectiveStockUnit(product)
        val trimmed = sellUnit.trim()
        if (trimmed.isBlank() || trimmed.equals(stockUnit, ignoreCase = true)) {
            return 1.0
        }
        val matched = product.sellUnits.entries.firstOrNull {
            it.key.trim().equals(trimmed, ignoreCase = true)
        }?.value
        return if (matched != null && matched > 0.0) roundStock(matched) else 1.0
    }

    fun buyQtyToStockQty(buyQty: Double, conversion: Double): Double {
        val safeQty = buyQty.coerceAtLeast(0.0)
        val safeConv = if (conversion > 0.0) conversion else 1.0
        return roundStock(safeQty * safeConv)
    }

    fun costPerStockUnit(buyUnitPrice: Double, conversion: Double): Double {
        val safePrice = buyUnitPrice.coerceAtLeast(0.0)
        val safeConv = if (conversion > 0.0) conversion else 1.0
        return roundMoney(safePrice / safeConv)
    }

    fun sellPricePerUnit(stockSellPrice: Double, sellFactor: Double): Double {
        val safePrice = stockSellPrice.coerceAtLeast(0.0)
        val safeFactor = if (sellFactor > 0.0) sellFactor else 1.0
        return roundMoney(safePrice / safeFactor)
    }

    fun buyPricePerUnit(stockBuyPrice: Double, sellFactor: Double): Double {
        val safePrice = stockBuyPrice.coerceAtLeast(0.0)
        val safeFactor = if (sellFactor > 0.0) sellFactor else 1.0
        return roundMoney(safePrice / safeFactor)
    }

    fun buyPriceInBuyUnit(stockBuyPrice: Double, conversion: Double): Double {
        val safePrice = stockBuyPrice.coerceAtLeast(0.0)
        val safeConv = if (conversion > 0.0) conversion else 1.0
        return roundMoney(safePrice * safeConv)
    }

    fun sellQtyToStockQty(sellQty: Double, sellFactor: Double): Double {
        val safeQty = sellQty.coerceAtLeast(0.0)
        val safeFactor = if (sellFactor > 0.0) sellFactor else 1.0
        return roundStock(safeQty / safeFactor)
    }

    fun maxSellableQtyInUnit(availableStock: Double, sellFactor: Double): Double {
        val safeStock = availableStock.coerceAtLeast(0.0)
        val safeFactor = if (sellFactor > 0.0) sellFactor else 1.0
        return roundStock(safeStock * safeFactor)
    }

    fun saleItemStockQty(item: SaleItem): Double {
        return if (item.stockQty > 0.0) roundStock(item.stockQty) else roundStock(item.qty.coerceAtLeast(0.0))
    }

    fun purchaseItemStockQty(item: PurchaseItem): Double {
        return if (item.stockQty > 0.0) roundStock(item.stockQty) else roundStock(item.qty.coerceAtLeast(0.0))
    }

    fun isStockSufficient(currentStock: Double, requestedStockQty: Double): Boolean {
        return requestedStockQty > 0.0 && (currentStock - requestedStockQty) >= -STOCK_EPSILON
    }

    /**
     * Profit per piece = sell price - buy price
     */
    fun calculateProfitPerPiece(sellPrice: Double, buyPrice: Double): Double {
        return roundMoney(sellPrice - buyPrice)
    }

    /**
     * Profit margin % = ((sell price - buy price) / sell price) * 100
     */
    fun calculateMarginPercent(sellPrice: Double, buyPrice: Double): Double {
        if (sellPrice <= 0.0) return 0.0
        val profit = sellPrice - buyPrice
        return ((profit / sellPrice) * 100.0).coerceIn(-100.0, 1000.0)
    }

    /**
     * Rule: profit = sale price x qty - buy price x qty - discount
     */
    fun calculateLineProfit(salePrice: Double, buyPrice: Double, qty: Double, discount: Double): Double {
        val safeQty = qty.coerceAtLeast(0.0)
        val boundedDiscount = calculateBoundedDiscount(salePrice, safeQty, discount)
        return roundMoney((salePrice * safeQty) - (buyPrice * safeQty) - boundedDiscount)
    }

    /**
     * Line total before discount: sale price x qty
     */
    fun calculateLineGrossTotal(salePrice: Double, qty: Double): Double {
        return roundMoney(salePrice * qty.coerceAtLeast(0.0))
    }

    /**
     * Rule: a discount can never be more than the line total
     */
    fun calculateBoundedDiscount(salePrice: Double, qty: Double, discount: Double): Double {
        val gross = calculateLineGrossTotal(salePrice, qty)
        return roundMoney(discount.coerceIn(0.0, gross))
    }

    /**
     * Net line total: gross - bounded discount
     */
    fun calculateLineNetTotal(salePrice: Double, qty: Double, discount: Double): Double {
        val gross = calculateLineGrossTotal(salePrice, qty)
        val bounded = calculateBoundedDiscount(salePrice, qty, discount)
        return roundMoney((gross - bounded).coerceAtLeast(0.0))
    }

    /**
     * Rule: Total sale discount (per-item + invoice) can never exceed pre-discount profit
     * (and never exceed gross subtotal). If pre-discount profit <= 0, max discount is 0.0.
     */
    fun calculateSaleMaxProfitDiscount(items: List<SaleItem>): Double {
        val subtotal = roundMoney(items.sumOf { calculateLineGrossTotal(it.sellPrice, it.qty) })
        val grossPreDiscountProfit = roundMoney(
            items.sumOf { (it.sellPrice - it.buyPrice) * it.qty.coerceAtLeast(0.0) }
        )
        return roundMoney(grossPreDiscountProfit.coerceIn(0.0, subtotal))
    }

    /**
     * Maximum allowed additional invoice-level discount after accounting for per-item discounts,
     * capped by net pre-invoice profit and remaining net subtotal.
     */
    fun calculateMaxInvoiceDiscount(items: List<SaleItem>): Double {
        val subtotal = roundMoney(items.sumOf { calculateLineGrossTotal(it.sellPrice, it.qty) })
        val itemDiscounts = roundMoney(items.sumOf { calculateBoundedDiscount(it.sellPrice, it.qty, it.discount) })
        val netPreInvoiceProfit = roundMoney(
            items.sumOf { calculateLineProfit(it.sellPrice, it.buyPrice, it.qty, it.discount) }
        )
        val remainingSubtotal = roundMoney((subtotal - itemDiscounts).coerceAtLeast(0.0))
        return roundMoney(netPreInvoiceProfit.coerceIn(0.0, remainingSubtotal))
    }

    /**
     * Returns true when any non-zero discount exceeds the sale's available profit.
     * Selling at 0 discount remains allowed even when buyPrice >= sellPrice.
     */
    fun isSaleDiscountOverProfit(items: List<SaleItem>, invoiceDiscount: Double): Boolean {
        val safeInvoice = invoiceDiscount.coerceAtLeast(0.0)
        val itemDiscounts = roundMoney(items.sumOf { calculateBoundedDiscount(it.sellPrice, it.qty, it.discount) })
        val requestedTotalDiscount = roundMoney(itemDiscounts + safeInvoice)
        if (requestedTotalDiscount <= 0.001) return false
        val maxProfitDiscount = calculateSaleMaxProfitDiscount(items)
        val maxInvoiceDiscount = calculateMaxInvoiceDiscount(items)
        return safeInvoice > maxInvoiceDiscount + 0.001 || requestedTotalDiscount > maxProfitDiscount + 0.001
    }

    /**
     * Rule: stock can never go below 0
     */
    fun validateStockDeduction(currentStock: Double, qtyToDeduct: Double): Double {
        val remaining = roundStock(currentStock - qtyToDeduct)
        require(qtyToDeduct > 0.0 && remaining >= -STOCK_EPSILON) {
            "Stock cannot fall below zero. Current stock: $currentStock, Requested: $qtyToDeduct"
        }
        return remaining.coerceAtLeast(0.0)
    }

    /**
     * Rule: invoice numbers are sequential with one prefix (INV-0001, never doubled)
     */
    fun formatInvoiceNumber(sequence: Long): String {
        return String.format(java.util.Locale.US, "INV-%04d", sequence.coerceAtLeast(1L))
    }

    /**
     * Calculate summary metrics from raw sales and products collections.
     * Centralized: Nothing may calculate its own totals!
     */
    fun calculateStoreFinancials(
        products: List<Product>,
        sales: List<Sale>,
        startingCashFloat: Double = 0.0
    ): StoreFinancials {
        val todaySalesList = filterSalesByPeriod(sales, HistoryPeriod.TODAY, null)
        val todayPeriodSummary = calculatePeriodSummary(todaySalesList, purchasesTotal = 0.0)
        val allTimeSales = sales.sumOf { it.grandTotal }

        val todaySales = todayPeriodSummary.totalSold
        val todayProfit = todayPeriodSummary.totalProfit
        val todayOrdersCount = todayPeriodSummary.totalOrders
        val todayItemsSold = todayPeriodSummary.totalItemsSold

        // Stock value at cost = sum of (buyPrice * stock)
        var stockValueAtCost = 0.0
        for (prod in products) {
            val validStock = prod.stock.coerceAtLeast(0.0)
            stockValueAtCost += (prod.buyPrice * validStock)
        }

        // Cash balance = starting float + all recorded sales
        val cashBalance = startingCashFloat + allTimeSales

        return StoreFinancials(
            todaySales = todaySales,
            todayProfit = todayProfit,
            todayOrdersCount = todayOrdersCount,
            todayItemsSold = todayItemsSold,
            stockValueAtCost = roundMoney(stockValueAtCost),
            cashBalance = cashBalance
        )
    }

    /**
     * Calculate stock metrics: item count, low stock count, stock value
     */
    fun calculateStockSummary(products: List<Product>): StockSummary {
        var lowStockCount = 0L
        var outOfStockCount = 0L
        var stockValueAtCost = 0.0

        for (prod in products) {
            if (prod.stock <= 0.0) {
                outOfStockCount += 1L
            } else if (prod.stock <= prod.lowStockLimit) {
                lowStockCount += 1L
            }
            val validStock = prod.stock.coerceAtLeast(0.0)
            stockValueAtCost += (prod.buyPrice * validStock)
        }

        return StockSummary(
            totalItems = products.size.toLong(),
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            stockValueAtCost = roundMoney(stockValueAtCost)
        )
    }

    /**
     * Build a SaleItem with guaranteed calculation consistency and unit conversion
     */
    fun buildSaleItem(
        product: Product,
        qty: Double,
        unit: String = product.unit,
        requestedDiscount: Double = 0.0
    ): SaleItem {
        val chosenUnit = unit.trim().ifBlank { effectiveStockUnit(product) }
        val safeQty = roundStock(qty.coerceAtLeast(0.0001))
        val factor = sellFactorForUnit(product, chosenUnit)
        val unitBuyPrice = buyPricePerUnit(product.buyPrice, factor)
        val unitSellPrice = sellPricePerUnit(product.sellPrice, factor)
        val stockQty = sellQtyToStockQty(safeQty, factor)
        val boundedDiscount = calculateBoundedDiscount(unitSellPrice, safeQty, requestedDiscount)
        val lineTotal = calculateLineNetTotal(unitSellPrice, safeQty, boundedDiscount)
        val profit = calculateLineProfit(unitSellPrice, unitBuyPrice, safeQty, boundedDiscount)

        return SaleItem(
            productId = product.id,
            name = product.name,
            qty = safeQty,
            unit = chosenUnit,
            stockQty = stockQty,
            buyPrice = unitBuyPrice,
            sellPrice = unitSellPrice,
            discount = boundedDiscount,
            lineTotal = lineTotal,
            profit = profit
        )
    }

    /**
     * Build a PurchaseItem in the buy unit with automatic stock-unit conversion and cost per stock unit
     */
    fun buildPurchaseItem(
        product: Product,
        buyQty: Double,
        buyUnitPrice: Double,
        buyUnit: String = effectiveBuyUnit(product),
        conversion: Double = effectiveBuyConversion(product)
    ): PurchaseItem {
        val chosenBuyUnit = buyUnit.trim().ifBlank { effectiveBuyUnit(product) }
        val stockUnit = effectiveStockUnit(product)
        val safeConversion = if (chosenBuyUnit.equals(stockUnit, ignoreCase = true) || conversion <= 0.0) {
            1.0
        } else {
            roundStock(conversion)
        }
        val safeBuyQty = roundStock(buyQty.coerceAtLeast(0.0))
        val safeBuyPrice = roundMoney(buyUnitPrice.coerceAtLeast(0.0))
        val stockQty = buyQtyToStockQty(safeBuyQty, safeConversion)
        val unitCostPerStock = costPerStockUnit(safeBuyPrice, safeConversion)
        val lineTotal = roundMoney(safeBuyQty * safeBuyPrice)

        return PurchaseItem(
            productId = product.id,
            name = product.name,
            qty = safeBuyQty,
            unit = chosenBuyUnit,
            stockQty = stockQty,
            conversionFactor = safeConversion,
            costPerStockUnit = unitCostPerStock,
            buyPrice = safeBuyPrice,
            lineTotal = lineTotal
        )
    }

    /**
     * Calculate summary metrics for a given list of sales and purchases total.
     * Centralized in CalculationEngine per Rule 4.
     */
    fun calculatePeriodSummary(
        sales: List<Sale>,
        purchasesTotal: Double = 0.0
    ): PeriodFinancialSummary {
        var totalSold = 0.0
        var totalProfit = 0.0
        var totalItemsSold = 0.0

        for (sale in sales) {
            totalSold += sale.grandTotal
            totalProfit += sale.profit
            totalItemsSold += sale.totalItems
        }

        return PeriodFinancialSummary(
            totalSold = roundMoney(totalSold),
            totalBought = purchasesTotal,
            totalProfit = roundMoney(totalProfit),
            totalOrders = sales.size.toLong(),
            totalItemsSold = roundStock(totalItemsSold)
        )
    }

    /**
     * Calculate product-wise sold statistics sorted by quantity sold descending.
     * Keyed by (product + unit) so mixed-unit sales display each unit accurately.
     */
    fun calculateProductsSoldStats(sales: List<Sale>): List<ProductSoldStat> {
        val accumulators = mutableMapOf<String, ProductSoldAccumulator>()

        for (sale in sales) {
            for (item in sale.items) {
                val unitKey = item.unit.ifBlank { "pcs" }.trim()
                val key = "${item.productId.ifBlank { item.name }}__${unitKey.lowercase()}"
                val acc = accumulators.getOrPut(key) {
                    ProductSoldAccumulator(
                        productId = item.productId,
                        productName = item.name,
                        unit = unitKey
                    )
                }
                acc.qty += item.qty
                acc.salesAmount += item.lineTotal
                acc.profit += item.profit
            }
        }

        return accumulators.values.map {
            ProductSoldStat(
                productId = it.productId,
                productName = it.productName,
                unit = it.unit,
                quantitySold = roundStock(it.qty),
                totalSalesAmount = roundMoney(it.salesAmount),
                totalProfit = roundMoney(it.profit)
            )
        }.sortedByDescending { it.totalSalesAmount }
    }

    /**
     * Date filtering logic for sales.
     */
    fun isSaleInDateRange(sale: Sale, startMillis: Long, endMillis: Long): Boolean {
        val saleTime = sale.createdAt?.toDate()?.time ?: return false
        return saleTime in startMillis..endMillis
    }

    fun filterSalesByDateRange(sales: List<Sale>, startMillis: Long, endMillis: Long): List<Sale> {
        return sales.filter { isSaleInDateRange(it, startMillis, endMillis) }
    }

    fun formatPaymentNumber(sequence: Long): String {
        return String.format(Locale.US, "PAY-%04d", sequence.coerceAtLeast(1L))
    }

    /**
     * Checks if a sale belongs to a customer.
     * Matches by customerId if present, or by mobile, or by customerName (excluding Cash Sale).
     * This guarantees that old Baki sales from before this update appear correctly.
     */
    fun doesSaleMatchCustomer(sale: Sale, customer: Customer): Boolean {
        if (sale.customerName == "Cash Sale" && sale.customerId.isBlank()) return false
        if (customer.id.isNotBlank() && sale.customerId.isNotBlank()) {
            return sale.customerId == customer.id
        }
        if (customer.mobile.isNotBlank() && sale.customerPhone.isNotBlank()) {
            return sale.customerPhone.trim() == customer.mobile.trim()
        }
        if (customer.name.isNotBlank() && sale.customerName.isNotBlank()) {
            return sale.customerName.trim().equals(customer.name.trim(), ignoreCase = true)
        }
        return false
    }

    /**
     * Checks if a payment belongs to a customer.
     */
    fun doesPaymentMatchCustomer(payment: Payment, customer: Customer): Boolean {
        if (customer.id.isNotBlank() && payment.customerId.isNotBlank()) {
            return payment.customerId == customer.id
        }
        if (customer.mobile.isNotBlank() && payment.customerMobile.isNotBlank()) {
            return payment.customerMobile.trim() == customer.mobile.trim()
        }
        if (customer.name.isNotBlank() && payment.customerName.isNotBlank()) {
            return payment.customerName.trim().equals(customer.name.trim(), ignoreCase = true)
        }
        return false
    }

    /**
     * Calculates customer balance summary:
     * - Total Baki sales
     * - Total payments
     * - Balance = Baki sales - payments (coerced at least 0.0)
     * - Total purchases (all sales, cash + baki)
     * - Last purchase date
     */
    /**
     * Returns the amount paid at checkout for a sale.
     * Backward-compatible with existing sales that do not store paidAmount/dueAmount.
     */
    fun salePaidAmount(sale: Sale): Double {
        val total = sale.grandTotal.coerceAtLeast(0.0)
        if (sale.paidAmount > 0.0001 || sale.dueAmount > 0.0001) {
            return roundMoney(sale.paidAmount.coerceIn(0.0, total))
        }
        return if (sale.paymentType == "baki") 0.0 else roundMoney(total)
    }

    /**
     * Returns the remaining unpaid (baki) amount for a sale.
     * Backward-compatible with existing sales that do not store paidAmount/dueAmount.
     */
    fun saleDueAmount(sale: Sale): Double {
        val total = sale.grandTotal.coerceAtLeast(0.0)
        if (sale.paidAmount > 0.0001 || sale.dueAmount > 0.0001) {
            return roundMoney(sale.dueAmount.coerceIn(0.0, total))
        }
        return if (sale.paymentType == "baki") roundMoney(total) else 0.0
    }

    fun isPartPaidSale(sale: Sale): Boolean =
        salePaidAmount(sale) > 0.001 && saleDueAmount(sale) > 0.001

    fun calculateCustomerSummary(
        customer: Customer,
        allSales: List<Sale>,
        allPayments: List<Payment>
    ): CustomerBalanceSummary {
        val customerSales = allSales.filter { doesSaleMatchCustomer(it, customer) }
        val customerPayments = allPayments.filter { doesPaymentMatchCustomer(it, customer) }

        val bakiSales = customerSales.filter { saleDueAmount(it) > 0.001 }
        val totalBaki = roundMoney(bakiSales.sumOf { saleDueAmount(it) })
        val totalPaid = roundMoney(customerPayments.sumOf { it.amount })
        val balance = roundMoney((totalBaki - totalPaid).coerceAtLeast(0.0))

        val totalPurchases = customerSales.sumOf { it.grandTotal }
        val lastPurchaseDate = customerSales.mapNotNull { it.createdAt?.toDate()?.time }.maxOrNull()

        return CustomerBalanceSummary(
            customer = customer.copy(currentBalance = balance),
            totalBakiSales = totalBaki,
            totalPayments = totalPaid,
            currentBalance = balance,
            totalPurchases = totalPurchases,
            lastPurchaseDate = lastPurchaseDate
        )
    }

    /**
     * Calculates the full customer ledger with running balance after each line (newest first).
     * Events are sorted chronologically (oldest first) to accurately track the running balance,
     * then returned in reverse chronological order (newest first) for presentation.
     */
    fun calculateCustomerLedger(
        customer: Customer,
        allSales: List<Sale>,
        allPayments: List<Payment>
    ): List<CustomerLedgerEntry> {
        val customerSales = allSales.filter { doesSaleMatchCustomer(it, customer) && saleDueAmount(it) > 0.001 }
        val customerPayments = allPayments.filter { doesPaymentMatchCustomer(it, customer) }

        data class RawEvent(
            val id: String,
            val type: LedgerEntryType,
            val referenceNo: String,
            val timestamp: Long,
            val amount: Double,
            val note: String,
            val totalAmount: Double = 0.0,
            val paidAmount: Double = 0.0
        )

        val rawEvents = mutableListOf<RawEvent>()

        for (sale in customerSales) {
            val ts = sale.createdAt?.toDate()?.time ?: 0L
            rawEvents.add(
                RawEvent(
                    id = sale.id,
                    type = LedgerEntryType.BAKI_SALE,
                    referenceNo = sale.invoiceNo,
                    timestamp = ts,
                    amount = saleDueAmount(sale),
                    note = sale.notes,
                    totalAmount = sale.grandTotal,
                    paidAmount = salePaidAmount(sale)
                )
            )
        }

        for (payment in customerPayments) {
            val ts = if (payment.paymentDate > 0L) payment.paymentDate else (payment.createdAt?.toDate()?.time ?: 0L)
            rawEvents.add(
                RawEvent(
                    id = payment.id,
                    type = LedgerEntryType.PAYMENT,
                    referenceNo = payment.paymentNo,
                    timestamp = ts,
                    amount = payment.amount,
                    note = payment.note
                )
            )
        }

        // Sort chronologically (oldest first) to compute running balance correctly
        rawEvents.sortBy { it.timestamp }

        var running = 0.0
        val entries = mutableListOf<CustomerLedgerEntry>()

        for (ev in rawEvents) {
            if (ev.type == LedgerEntryType.BAKI_SALE) {
                running = roundMoney(running + ev.amount)
            } else {
                running = roundMoney((running - ev.amount).coerceAtLeast(0.0))
            }
            entries.add(
                CustomerLedgerEntry(
                    id = ev.id,
                    type = ev.type,
                    referenceNo = ev.referenceNo,
                    timestamp = ev.timestamp,
                    amount = ev.amount,
                    note = ev.note,
                    runningBalance = running,
                    totalAmount = ev.totalAmount,
                    paidAmount = ev.paidAmount
                )
            )
        }

        // Return newest first per requirement
        return entries.reversed()
    }

    /**
     * Aggregates all customers, merges any old baki sales from customers not yet in customer table,
     * computes balances, and returns the Baki overview.
     */
    fun calculateBakiOverview(
        storedCustomers: List<Customer>,
        allSales: List<Sale>,
        allPayments: List<Payment>
    ): BakiOverview {
        val customerList = storedCustomers.toMutableList()

        // Identify any customer from old Baki sales who might not have an explicit Customer document yet
        for (sale in allSales) {
            if (sale.paymentType == "baki" && sale.customerName != "Cash Sale" && sale.customerName.isNotBlank()) {
                val exists = customerList.any { doesSaleMatchCustomer(sale, it) }
                if (!exists) {
                    customerList.add(
                        Customer(
                            id = sale.customerId.ifBlank { "legacy_${sale.customerName.trim().lowercase().replace(' ', '_')}" },
                            name = sale.customerName.trim(),
                            mobile = sale.customerPhone.trim(),
                            notes = "Previous Baki Sale"
                        )
                    )
                }
            }
        }

        val summaries = customerList.map { calculateCustomerSummary(it, allSales, allPayments) }
        val debtors = summaries.filter { it.currentBalance > 0.001 }.sortedByDescending { it.currentBalance }
        val totalBaki = debtors.sumOf { it.currentBalance }

        return BakiOverview(
            totalBakiOwed = totalBaki,
            debtorCount = debtors.size.toLong(),
            debtors = debtors
        )
    }

    /**
     * Determines whether a customer can be deleted:
     * Only if balance is zero and they have no sales.
     */
    fun canDeleteCustomer(summary: CustomerBalanceSummary): Boolean {
        return summary.currentBalance <= 0.001 && summary.totalPurchases <= 0.001
    }

    /**
     * Standard date range bounds for all reports and tabs.
     */
    fun getPeriodBounds(period: HistoryPeriod, customRange: Pair<Long, Long>?): Pair<Long, Long> {
        return when (period) {
            HistoryPeriod.TODAY -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                start to end
            }
            HistoryPeriod.WEEK -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val endCal = Calendar.getInstance()
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                start to endCal.timeInMillis
            }
            HistoryPeriod.MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                start to end
            }
            HistoryPeriod.YEAR -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                start to end
            }
            HistoryPeriod.ALL -> {
                0L to Long.MAX_VALUE
            }
            HistoryPeriod.CUSTOM -> {
                if (customRange != null) {
                    val sCal = Calendar.getInstance().apply {
                        timeInMillis = customRange.first
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val eCal = Calendar.getInstance().apply {
                        timeInMillis = customRange.second
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    sCal.timeInMillis to eCal.timeInMillis
                } else {
                    0L to Long.MAX_VALUE
                }
            }
        }
    }

    fun isTimestampInPeriod(timestamp: Long, period: HistoryPeriod, customRange: Pair<Long, Long>?): Boolean {
        val (start, end) = getPeriodBounds(period, customRange)
        return timestamp in start..end
    }

    fun filterSalesByPeriod(sales: List<Sale>, period: HistoryPeriod, customRange: Pair<Long, Long>?): List<Sale> {
        val (start, end) = getPeriodBounds(period, customRange)
        return sales.filter { (it.createdAt?.toDate()?.time ?: 0L) in start..end }
    }

    fun filterPurchasesByPeriod(purchases: List<Purchase>, period: HistoryPeriod, customRange: Pair<Long, Long>?): List<Purchase> {
        val (start, end) = getPeriodBounds(period, customRange)
        return purchases.filter { (it.createdAt?.toDate()?.time ?: 0L) in start..end }
    }

    fun filterReturnsByPeriod(returns: List<PurchaseReturn>, period: HistoryPeriod, customRange: Pair<Long, Long>?): List<PurchaseReturn> {
        val (start, end) = getPeriodBounds(period, customRange)
        return returns.filter { (it.createdAt?.toDate()?.time ?: 0L) in start..end }
    }

    fun filterPaymentsByPeriod(payments: List<Payment>, period: HistoryPeriod, customRange: Pair<Long, Long>?): List<Payment> {
        val (start, end) = getPeriodBounds(period, customRange)
        return payments.filter {
            val ts = if (it.paymentDate > 0L) it.paymentDate else (it.createdAt?.toDate()?.time ?: 0L)
            ts in start..end
        }
    }

    fun filterExpensesByPeriod(expenses: List<Expense>, period: HistoryPeriod, customRange: Pair<Long, Long>?): List<Expense> {
        val (start, end) = getPeriodBounds(period, customRange)
        return expenses.filter {
            val ts = if (it.date > 0L) it.date else (it.createdAt?.toDate()?.time ?: 0L)
            ts in start..end
        }
    }

    fun filterOtherIncomesByPeriod(incomes: List<OtherIncome>, period: HistoryPeriod, customRange: Pair<Long, Long>?): List<OtherIncome> {
        val (start, end) = getPeriodBounds(period, customRange)
        return incomes.filter {
            val ts = if (it.date > 0L) it.date else (it.createdAt?.toDate()?.time ?: 0L)
            ts in start..end
        }
    }

    fun filterSupplierPaymentsByPeriod(payments: List<SupplierPayment>, period: HistoryPeriod, customRange: Pair<Long, Long>?): List<SupplierPayment> {
        val (start, end) = getPeriodBounds(period, customRange)
        return payments.filter {
            val ts = if (it.paymentDate > 0L) it.paymentDate else (it.createdAt?.toDate()?.time ?: 0L)
            ts in start..end
        }
    }

    fun filterAdjustmentsByPeriod(adjustments: List<AccountAdjustment>, period: HistoryPeriod, customRange: Pair<Long, Long>?): List<AccountAdjustment> {
        val (start, end) = getPeriodBounds(period, customRange)
        return adjustments.filter {
            val ts = it.createdAt?.toDate()?.time ?: 0L
            ts in start..end
        }
    }

    /**
     * Unified calculation of Money Now metrics:
     * - stockValueAtCost: sum of (stock * buyPrice)
     * - bakiCustomersOwe: sum of currentBalance from customers
     * - debtToSuppliers: sum of balance from suppliers
     */
    fun calculateMoneyNow(
        products: List<Product>,
        customers: List<Customer>,
        suppliers: List<Supplier>,
        allSales: List<Sale> = emptyList(),
        allPayments: List<Payment> = emptyList(),
        totalCashAndBankBalance: Double = 0.0
    ): MoneyNowSummary {
        val stockVal = roundMoney(products.sumOf { it.stock.coerceAtLeast(0.0) * it.buyPrice })
        val bakiOverview = calculateBakiOverview(customers, allSales, allPayments)
        val debtSuppliers = suppliers.sumOf { it.balance.coerceAtLeast(0.0) }

        return MoneyNowSummary(
            stockValueAtCost = stockVal,
            bakiCustomersOwe = bakiOverview.totalBakiOwed,
            debtToSuppliers = debtSuppliers,
            debtorsCount = bakiOverview.debtorCount,
            suppliersCount = suppliers.count { it.balance > 0.001 }.toLong(),
            totalCashAndBankBalance = totalCashAndBankBalance
        )
    }

    /**
     * Calculates data points for sales chart (bars/lines).
     * Grouped by Day, or by Month if period is YEAR or ALL.
     */
    fun calculateDailySalesChart(
        sales: List<Sale>,
        period: HistoryPeriod,
        customRange: Pair<Long, Long>?,
        lang: AppLanguage = AppLanguage.EN
    ): List<DailySalesPoint> {
        val isMonthly = period == HistoryPeriod.YEAR || period == HistoryPeriod.ALL

        val grouped = mutableMapOf<String, DailySalesPoint>()

        for (sale in sales) {
            val ts = sale.createdAt?.toDate()?.time ?: 0L
            val key = if (isMonthly) {
                ShopLocale.formatMonthShortYear(ts, lang)
            } else {
                ShopLocale.formatDayMonth(ts, lang)
            }
            val existing = grouped[key]
            if (existing == null) {
                grouped[key] = DailySalesPoint(
                    label = key,
                    timestamp = ts,
                    salesAmount = sale.grandTotal,
                    profitAmount = sale.profit,
                    orderCount = 1L
                )
            } else {
                grouped[key] = existing.copy(
                    salesAmount = existing.salesAmount + sale.grandTotal,
                    profitAmount = existing.profitAmount + sale.profit,
                    orderCount = existing.orderCount + 1L
                )
            }
        }

        return grouped.values.sortedBy { it.timestamp }
    }

    /**
     * Best-selling products (top N by quantity sold, with sales amount).
     */
    fun calculateTopSellingProducts(sales: List<Sale>, limit: Int = 5): List<ProductSoldStat> {
        return calculateProductsSoldStats(sales).take(limit)
    }

    /**
     * Sales grouped by category for Donut Chart.
     */
    fun calculateSalesByCategory(
        sales: List<Sale>,
        products: List<Product>
    ): List<CategorySalesStat> {
        val productCategoryMap = products.associate { it.id to it.category.ifBlank { "General" } }
        val categoryTotals = mutableMapOf<String, Pair<Double, Double>>()

        for (sale in sales) {
            for (item in sale.items) {
                val cat = productCategoryMap[item.productId] ?: "General"
                val cur = categoryTotals[cat] ?: (0.0 to 0.0)
                categoryTotals[cat] = (cur.first + item.lineTotal) to (cur.second + saleItemStockQty(item))
            }
        }

        val grandTotal = categoryTotals.values.sumOf { it.first }

        return categoryTotals.map { (cat, pair) ->
            val pct = if (grandTotal > 0.0) (pair.first / grandTotal) * 100.0 else 0.0
            CategorySalesStat(
                category = cat,
                salesAmount = pair.first,
                itemsCount = roundStock(pair.second),
                percentage = pct
            )
        }.sortedByDescending { it.salesAmount }
    }

    /**
     * Slow-selling products: products with stock > 0 but no sales in the period.
     */
    fun calculateSlowSellingProducts(products: List<Product>, sales: List<Sale>): List<Product> {
        val soldProductIds = sales.flatMap { it.items }.map { it.productId }.toSet()
        val soldProductNames = sales.flatMap { it.items }.map { it.name.trim().lowercase() }.toSet()

        return products.filter { prod ->
            prod.stock > 0.0 &&
                    !soldProductIds.contains(prod.id) &&
                    !soldProductNames.contains(prod.name.trim().lowercase())
        }.sortedByDescending { it.stock }
    }

    /**
     * Helper to associate historical records without accountId with default "Cash in Hand".
     * Rule 6: Zero rewriting of old records.
     */
    fun isRecordForAccount(recordAccountId: String?, account: Account): Boolean {
        if (account.isDefault || account.id == "cash_in_hand") {
            return recordAccountId.isNullOrBlank() || recordAccountId == "cash_in_hand" || recordAccountId == account.id
        }
        return recordAccountId == account.id
    }

    /**
     * Account balance = opening balance + money in - money out:
     * Money in: Paid in Full sales, Payments In, Other Income, upward adjustments.
     * Money out: Paid purchases, Payments Out, Expenses, downward adjustments.
     * Baki sales & Credit purchases do not move money until payment recorded.
     * Purchase returns do not move account money in this version.
     */
    fun calculateAccountBalances(
        accounts: List<Account>,
        sales: List<Sale>,
        paymentsIn: List<Payment>,
        purchases: List<Purchase>,
        supplierPayments: List<SupplierPayment>,
        expenses: List<Expense>,
        otherIncomes: List<OtherIncome>,
        adjustments: List<AccountAdjustment>
    ): Map<String, Double> {
        val balanceMap = mutableMapOf<String, Double>()

        for (acc in accounts) {
            var moneyIn = 0.0
            var moneyOut = 0.0

            // 1. Cash & part-paid sales (Money In)
            for (sale in sales) {
                val paid = salePaidAmount(sale)
                if (paid > 0.001 && isRecordForAccount(sale.accountId, acc)) {
                    moneyIn += paid
                }
            }

            // 2. Payments In from customers (Money In)
            for (pay in paymentsIn) {
                if (isRecordForAccount(pay.accountId, acc)) {
                    moneyIn += pay.amount
                }
            }

            // 3. Other Income (Money In)
            for (inc in otherIncomes) {
                if (!inc.isDeleted && isRecordForAccount(inc.accountId, acc)) {
                    moneyIn += inc.amount
                }
            }

            // 4. Paid purchases (Money Out)
            for (pur in purchases) {
                if (pur.paidAmount > 0.0 && isRecordForAccount(pur.accountId, acc)) {
                    moneyOut += pur.paidAmount
                }
            }

            // 5. Supplier Payments / Payments Out (Money Out)
            for (sp in supplierPayments) {
                if (isRecordForAccount(sp.accountId, acc)) {
                    moneyOut += sp.amount
                }
            }

            // 6. Expenses (Money Out)
            for (exp in expenses) {
                if (!exp.isDeleted && isRecordForAccount(exp.accountId, acc)) {
                    moneyOut += exp.amount
                }
            }

            // 7. Adjustments
            for (adj in adjustments) {
                if (adj.accountId == acc.id) {
                    if (adj.difference > 0.0) {
                        moneyIn += adj.difference
                    } else if (adj.difference < 0.0) {
                        moneyOut += kotlin.math.abs(adj.difference)
                    }
                }
            }

            val bal = acc.openingBalance + moneyIn - moneyOut
            balanceMap[acc.id] = bal
        }

        return balanceMap
    }

    /**
     * Overall total balance across all active (non-archived) accounts.
     */
    fun calculateTotalBalance(accounts: List<Account>, balances: Map<String, Double>): Double {
        return accounts.filter { !it.isArchived }.sumOf { balances[it.id] ?: it.openingBalance }
    }

    /**
     * Account statement for a given account and period.
     */
    fun calculateAccountStatement(
        account: Account,
        allSales: List<Sale>,
        allPaymentsIn: List<Payment>,
        allPurchases: List<Purchase>,
        allSupplierPayments: List<SupplierPayment>,
        allExpenses: List<Expense>,
        allOtherIncomes: List<OtherIncome>,
        allAdjustments: List<AccountAdjustment>,
        period: HistoryPeriod,
        customRange: Pair<Long, Long>?
    ): AccountStatementReport {
        val (start, end) = getPeriodBounds(period, customRange)

        data class RawAccountEvent(
            val id: String,
            val timestamp: Long,
            val type: String,
            val referenceNo: String,
            val partyOrCategory: String,
            val moneyIn: Double,
            val moneyOut: Double,
            val note: String
        )

        val rawEvents = mutableListOf<RawAccountEvent>()
        var priorMoneyIn = 0.0
        var priorMoneyOut = 0.0

        // Sales (Cash & part-paid)
        for (sale in allSales) {
            val paid = salePaidAmount(sale)
            if (paid > 0.001 && isRecordForAccount(sale.accountId, account)) {
                val ts = sale.createdAt?.toDate()?.time ?: 0L
                if (ts < start) {
                    priorMoneyIn += paid
                } else if (ts in start..end) {
                    rawEvents.add(
                        RawAccountEvent(
                            id = sale.id,
                            timestamp = ts,
                            type = "Sale",
                            referenceNo = sale.invoiceNo,
                            partyOrCategory = sale.customerName,
                            moneyIn = paid,
                            moneyOut = 0.0,
                            note = sale.notes
                        )
                    )
                }
            }
        }

        // Payments In
        for (pay in allPaymentsIn) {
            if (isRecordForAccount(pay.accountId, account)) {
                val ts = if (pay.paymentDate > 0L) pay.paymentDate else (pay.createdAt?.toDate()?.time ?: 0L)
                if (ts < start) {
                    priorMoneyIn += pay.amount
                } else if (ts in start..end) {
                    rawEvents.add(
                        RawAccountEvent(
                            id = pay.id,
                            timestamp = ts,
                            type = "Payment In",
                            referenceNo = pay.paymentNo,
                            partyOrCategory = pay.customerName,
                            moneyIn = pay.amount,
                            moneyOut = 0.0,
                            note = pay.note
                        )
                    )
                }
            }
        }

        // Purchases (Paid Amount)
        for (pur in allPurchases) {
            if (pur.paidAmount > 0.0 && isRecordForAccount(pur.accountId, account)) {
                val ts = pur.createdAt?.toDate()?.time ?: 0L
                if (ts < start) {
                    priorMoneyOut += pur.paidAmount
                } else if (ts in start..end) {
                    rawEvents.add(
                        RawAccountEvent(
                            id = pur.id,
                            timestamp = ts,
                            type = "Purchase",
                            referenceNo = pur.purchaseNo,
                            partyOrCategory = pur.supplierName,
                            moneyIn = 0.0,
                            moneyOut = pur.paidAmount,
                            note = pur.note
                        )
                    )
                }
            }
        }

        // Supplier Payments (Payment Out)
        for (sp in allSupplierPayments) {
            if (isRecordForAccount(sp.accountId, account)) {
                val ts = if (sp.paymentDate > 0L) sp.paymentDate else (sp.createdAt?.toDate()?.time ?: 0L)
                if (ts < start) {
                    priorMoneyOut += sp.amount
                } else if (ts in start..end) {
                    rawEvents.add(
                        RawAccountEvent(
                            id = sp.id,
                            timestamp = ts,
                            type = "Payment Out",
                            referenceNo = sp.paymentNo,
                            partyOrCategory = sp.supplierName,
                            moneyIn = 0.0,
                            moneyOut = sp.amount,
                            note = sp.note
                        )
                    )
                }
            }
        }

        // Expenses
        for (exp in allExpenses) {
            if (!exp.isDeleted && isRecordForAccount(exp.accountId, account)) {
                val ts = if (exp.date > 0L) exp.date else (exp.createdAt?.toDate()?.time ?: 0L)
                if (ts < start) {
                    priorMoneyOut += exp.amount
                } else if (ts in start..end) {
                    rawEvents.add(
                        RawAccountEvent(
                            id = exp.id,
                            timestamp = ts,
                            type = "Expense",
                            referenceNo = "EXP",
                            partyOrCategory = exp.category,
                            moneyIn = 0.0,
                            moneyOut = exp.amount,
                            note = exp.note
                        )
                    )
                }
            }
        }

        // Other Income
        for (inc in allOtherIncomes) {
            if (!inc.isDeleted && isRecordForAccount(inc.accountId, account)) {
                val ts = if (inc.date > 0L) inc.date else (inc.createdAt?.toDate()?.time ?: 0L)
                if (ts < start) {
                    priorMoneyIn += inc.amount
                } else if (ts in start..end) {
                    rawEvents.add(
                        RawAccountEvent(
                            id = inc.id,
                            timestamp = ts,
                            type = "Other Income",
                            referenceNo = "INC",
                            partyOrCategory = inc.category,
                            moneyIn = inc.amount,
                            moneyOut = 0.0,
                            note = inc.note
                        )
                    )
                }
            }
        }

        // Adjustments
        for (adj in allAdjustments) {
            if (adj.accountId == account.id) {
                val ts = adj.createdAt?.toDate()?.time ?: 0L
                val isUpward = adj.difference > 0.0
                val inAmt = if (isUpward) adj.difference else 0.0
                val outAmt = if (!isUpward) kotlin.math.abs(adj.difference) else 0.0
                if (ts < start) {
                    priorMoneyIn += inAmt
                    priorMoneyOut += outAmt
                } else if (ts in start..end) {
                    rawEvents.add(
                        RawAccountEvent(
                            id = adj.id,
                            timestamp = ts,
                            type = "Adjustment",
                            referenceNo = "ADJ",
                            partyOrCategory = "Balance Adjustment",
                            moneyIn = inAmt,
                            moneyOut = outAmt,
                            note = "${adj.note} (by ${adj.adjustedBy})"
                        )
                    )
                }
            }
        }

        val openingBalance = account.openingBalance + priorMoneyIn - priorMoneyOut

        // Sort chronologically (oldest first) to accurately build running balances
        rawEvents.sortBy { it.timestamp }

        var running = openingBalance
        val entries = mutableListOf<AccountStatementEntry>()
        var periodTotalIn = 0.0
        var periodTotalOut = 0.0

        for (ev in rawEvents) {
            running = running + ev.moneyIn - ev.moneyOut
            periodTotalIn += ev.moneyIn
            periodTotalOut += ev.moneyOut

            entries.add(
                AccountStatementEntry(
                    id = ev.id,
                    timestamp = ev.timestamp,
                    type = ev.type,
                    referenceNo = ev.referenceNo,
                    partyOrCategory = ev.partyOrCategory,
                    moneyIn = ev.moneyIn,
                    moneyOut = ev.moneyOut,
                    runningBalance = running,
                    note = ev.note
                )
            )
        }

        // Return newest first
        return AccountStatementReport(
            account = account,
            period = period,
            startDate = start,
            endDate = end,
            openingBalance = openingBalance,
            entries = entries.reversed(),
            totalMoneyIn = periodTotalIn,
            totalMoneyOut = periodTotalOut,
            closingBalance = running
        )
    }

    /**
     * Profit and Loss statement calculation with real expenses and other income.
     * Rule: Gross Profit = Net Sales - COGS
     *       Net Profit = Gross Profit + Other Income - Expenses
     */
    fun calculateProfitAndLoss(
        sales: List<Sale>,
        purchases: List<Purchase>,
        returns: List<PurchaseReturn>,
        expenses: List<Expense> = emptyList(),
        otherIncomes: List<OtherIncome> = emptyList(),
        isStockBased: Boolean
    ): ProfitAndLossStatement {
        val netSales = sales.sumOf { it.grandTotal }
        val cogs = if (isStockBased) {
            sales.flatMap { it.items }.sumOf { it.buyPrice * it.qty }
        } else {
            val totalPurchased = purchases.sumOf { it.totalAmount }
            val totalReturns = returns.sumOf { it.amount }
            (totalPurchased - totalReturns).coerceAtLeast(0.0)
        }
        val grossProfit = netSales - cogs

        val activeExpenses = expenses.filter { !it.isDeleted }
        val totalExpenses = activeExpenses.sumOf { it.amount }
        val categoryBreakdown = activeExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val activeOtherIncomes = otherIncomes.filter { !it.isDeleted }
        val totalOtherIncome = activeOtherIncomes.sumOf { it.amount }

        val netProfit = grossProfit + totalOtherIncome - totalExpenses

        return ProfitAndLossStatement(
            netSales = netSales,
            costOfGoodsSold = cogs,
            grossProfit = grossProfit,
            expenses = totalExpenses,
            otherIncome = totalOtherIncome,
            netProfit = netProfit,
            isStockBased = isStockBased,
            expensesByCategory = categoryBreakdown
        )
    }

    /**
     * Day Book chronological timeline containing ALL transactions:
     * Sales (Cash & Baki), Purchases (Paid & Credit), Payments In, Purchase Returns,
     * Payments Out (Supplier Payments), Expenses, Other Incomes, and Balance Adjustments.
     */
    fun calculateDayBook(
        sales: List<Sale>,
        purchases: List<Purchase>,
        paymentsIn: List<Payment>,
        returns: List<PurchaseReturn>,
        supplierPayments: List<SupplierPayment> = emptyList(),
        expenses: List<Expense> = emptyList(),
        otherIncomes: List<OtherIncome> = emptyList(),
        adjustments: List<AccountAdjustment> = emptyList()
    ): List<DayBookEntry> {
        val entries = mutableListOf<DayBookEntry>()

        for (sale in sales) {
            val isBaki = saleDueAmount(sale) > 0.001
            val paid = salePaidAmount(sale)
            entries.add(
                DayBookEntry(
                    id = sale.id,
                    timestamp = sale.createdAt?.toDate()?.time ?: 0L,
                    type = if (isBaki) "SALE_BAKI" else "SALE_CASH",
                    referenceNo = sale.invoiceNo,
                    partyName = sale.customerName,
                    amount = sale.grandTotal,
                    isIncome = true,
                    note = sale.notes,
                    accountName = if (paid > 0.001) sale.accountName else ""
                )
            )
        }

        for (p in purchases) {
            val isCredit = p.paymentType == "credit"
            entries.add(
                DayBookEntry(
                    id = p.id,
                    timestamp = p.createdAt?.toDate()?.time ?: 0L,
                    type = if (isCredit) "PURCHASE_CREDIT" else "PURCHASE_PAID",
                    referenceNo = p.purchaseNo,
                    partyName = p.supplierName,
                    amount = p.totalAmount,
                    isIncome = false,
                    note = p.note,
                    accountName = if (isCredit) "" else p.accountName
                )
            )
        }

        for (pay in paymentsIn) {
            val ts = if (pay.paymentDate > 0L) pay.paymentDate else (pay.createdAt?.toDate()?.time ?: 0L)
            entries.add(
                DayBookEntry(
                    id = pay.id,
                    timestamp = ts,
                    type = "PAYMENT_IN",
                    referenceNo = pay.paymentNo,
                    partyName = pay.customerName,
                    amount = pay.amount,
                    isIncome = true,
                    note = pay.note,
                    accountName = pay.accountName
                )
            )
        }

        for (ret in returns) {
            entries.add(
                DayBookEntry(
                    id = ret.id,
                    timestamp = ret.createdAt?.toDate()?.time ?: 0L,
                    type = "PURCHASE_RETURN",
                    referenceNo = ret.returnNo,
                    partyName = ret.supplierName,
                    amount = ret.amount,
                    isIncome = true,
                    note = ret.reason
                )
            )
        }

        for (sp in supplierPayments) {
            val ts = if (sp.paymentDate > 0L) sp.paymentDate else (sp.createdAt?.toDate()?.time ?: 0L)
            entries.add(
                DayBookEntry(
                    id = sp.id,
                    timestamp = ts,
                    type = "PAYMENT_OUT",
                    referenceNo = sp.paymentNo,
                    partyName = sp.supplierName,
                    amount = sp.amount,
                    isIncome = false,
                    note = sp.note,
                    accountName = sp.accountName
                )
            )
        }

        for (exp in expenses) {
            if (!exp.isDeleted) {
                val ts = if (exp.date > 0L) exp.date else (exp.createdAt?.toDate()?.time ?: 0L)
                entries.add(
                    DayBookEntry(
                        id = exp.id,
                        timestamp = ts,
                        type = "EXPENSE",
                        referenceNo = "EXP",
                        partyName = exp.category,
                        amount = exp.amount,
                        isIncome = false,
                        note = exp.note,
                        accountName = exp.accountName
                    )
                )
            }
        }

        for (inc in otherIncomes) {
            if (!inc.isDeleted) {
                val ts = if (inc.date > 0L) inc.date else (inc.createdAt?.toDate()?.time ?: 0L)
                entries.add(
                    DayBookEntry(
                        id = inc.id,
                        timestamp = ts,
                        type = "OTHER_INCOME",
                        referenceNo = "INC",
                        partyName = inc.category,
                        amount = inc.amount,
                        isIncome = true,
                        note = inc.note,
                        accountName = inc.accountName
                    )
                )
            }
        }

        for (adj in adjustments) {
            val ts = adj.createdAt?.toDate()?.time ?: 0L
            val isUpward = adj.difference > 0.0
            entries.add(
                DayBookEntry(
                    id = adj.id,
                    timestamp = ts,
                    type = "ADJUSTMENT",
                    referenceNo = "ADJ",
                    partyName = adj.accountName,
                    amount = kotlin.math.abs(adj.difference),
                    isIncome = isUpward,
                    note = "${adj.note} (${if (isUpward) "+৳" else "-৳"}${kotlin.math.abs(adj.difference).toInt()})",
                    accountName = adj.accountName
                )
            )
        }

        return entries.sortedByDescending { it.timestamp }
    }

    /**
     * Expense Report calculation.
     */
    fun calculateExpenseReport(
        expenses: List<Expense>,
        period: HistoryPeriod,
        customRange: Pair<Long, Long>?
    ): ExpenseReportData {
        val (start, end) = getPeriodBounds(period, customRange)
        val filtered = expenses.filter {
            !it.isDeleted && ((if (it.date > 0L) it.date else (it.createdAt?.toDate()?.time ?: 0L)) in start..end)
        }

        val totalExpenses = filtered.sumOf { it.amount }
        val categoryStats = filtered.groupBy { it.category }.map { (cat, list) ->
            val catTotal = list.sumOf { it.amount }
            val pct = if (totalExpenses > 0.0) (catTotal / totalExpenses) * 100.0 else 0.0
            CategoryExpenseStat(
                category = cat,
                amount = catTotal,
                count = list.size.toLong(),
                percentage = pct
            )
        }.sortedByDescending { it.amount }

        return ExpenseReportData(
            period = period,
            startDate = start,
            endDate = end,
            totalExpenses = totalExpenses,
            categoryStats = categoryStats,
            expenses = filtered.sortedByDescending { if (it.date > 0L) it.date else (it.createdAt?.toDate()?.time ?: 0L) }
        )
    }

    /**
     * Income Report calculation.
     */
    fun calculateIncomeReport(
        incomes: List<OtherIncome>,
        period: HistoryPeriod,
        customRange: Pair<Long, Long>?
    ): IncomeReportData {
        val (start, end) = getPeriodBounds(period, customRange)
        val filtered = incomes.filter {
            !it.isDeleted && ((if (it.date > 0L) it.date else (it.createdAt?.toDate()?.time ?: 0L)) in start..end)
        }

        val totalIncome = filtered.sumOf { it.amount }
        val categoryStats = filtered.groupBy { it.category }.map { (cat, list) ->
            val catTotal = list.sumOf { it.amount }
            val pct = if (totalIncome > 0.0) (catTotal / totalIncome) * 100.0 else 0.0
            CategoryExpenseStat(
                category = cat,
                amount = catTotal,
                count = list.size.toLong(),
                percentage = pct
            )
        }.sortedByDescending { it.amount }

        return IncomeReportData(
            period = period,
            startDate = start,
            endDate = end,
            totalIncome = totalIncome,
            categoryStats = categoryStats,
            incomes = filtered.sortedByDescending { if (it.date > 0L) it.date else (it.createdAt?.toDate()?.time ?: 0L) }
        )
    }

    /**
     * Customer Statement report for chosen period.
     */
    fun calculateCustomerStatement(
        customer: Customer,
        allCustomerSales: List<Sale>,
        allCustomerPayments: List<Payment>,
        period: HistoryPeriod,
        customRange: Pair<Long, Long>?
    ): CustomerStatementReport {
        val (start, end) = getPeriodBounds(period, customRange)
        val customerSales = allCustomerSales.filter { doesSaleMatchCustomer(it, customer) }
        val customerPayments = allCustomerPayments.filter { doesPaymentMatchCustomer(it, customer) }

        // Opening balance before start date
        val priorBaki = customerSales.filter {
            saleDueAmount(it) > 0.001 && (it.createdAt?.toDate()?.time ?: 0L) < start
        }.sumOf { saleDueAmount(it) }

        val priorPaid = customerPayments.filter {
            val ts = if (it.paymentDate > 0L) it.paymentDate else (it.createdAt?.toDate()?.time ?: 0L)
            ts < start
        }.sumOf { it.amount }

        val openingBalance = roundMoney((priorBaki - priorPaid).coerceAtLeast(0.0))

        // Events inside period
        val periodSales = customerSales.filter {
            saleDueAmount(it) > 0.001 && (it.createdAt?.toDate()?.time ?: 0L) in start..end
        }
        val periodPayments = customerPayments.filter {
            val ts = if (it.paymentDate > 0L) it.paymentDate else (it.createdAt?.toDate()?.time ?: 0L)
            ts in start..end
        }

        val ledger = calculateCustomerLedger(customer, periodSales, periodPayments)
        val totalBaki = roundMoney(periodSales.sumOf { saleDueAmount(it) })
        val totalPaid = roundMoney(periodPayments.sumOf { it.amount })
        val closingBalance = roundMoney((openingBalance + totalBaki - totalPaid).coerceAtLeast(0.0))

        return CustomerStatementReport(
            customer = customer,
            period = period,
            startDate = start,
            endDate = end,
            openingBalance = openingBalance,
            entries = ledger,
            totalBakiSales = totalBaki,
            totalPayments = totalPaid,
            closingBalance = closingBalance
        )
    }

    /**
     * Supplier Statement report for chosen period.
     */
    fun calculateSupplierStatement(
        supplier: Supplier,
        allPurchases: List<Purchase>,
        allReturns: List<PurchaseReturn>,
        period: HistoryPeriod,
        customRange: Pair<Long, Long>?
    ): SupplierStatementReport {
        val (start, end) = getPeriodBounds(period, customRange)
        val supplierPurchases = allPurchases.filter { it.supplierId == supplier.id || it.supplierName.equals(supplier.name, ignoreCase = true) }
        val supplierReturns = allReturns.filter { it.supplierId == supplier.id || it.supplierName.equals(supplier.name, ignoreCase = true) }

        val priorCredit = supplierPurchases.filter {
            (it.createdAt?.toDate()?.time ?: 0L) < start
        }.sumOf { it.dueAmount }

        val priorReturns = supplierReturns.filter {
            (it.createdAt?.toDate()?.time ?: 0L) < start
        }.sumOf { it.amount }

        val openingBalance = (priorCredit - priorReturns).coerceAtLeast(0.0)

        val periodPurchases = supplierPurchases.filter { (it.createdAt?.toDate()?.time ?: 0L) in start..end }
        val periodReturns = supplierReturns.filter { (it.createdAt?.toDate()?.time ?: 0L) in start..end }

        val entries = mutableListOf<SupplierStatementEntry>()
        var running = openingBalance

        // Chronological order to build running balance
        val rawEvents = mutableListOf<Triple<Long, String, SupplierStatementEntry>>()
        for (p in periodPurchases) {
            val ts = p.createdAt?.toDate()?.time ?: 0L
            val entry = SupplierStatementEntry(
                id = p.id,
                timestamp = ts,
                type = if (p.paymentType == "credit") "CREDIT_PURCHASE" else "PAID_PURCHASE",
                referenceNo = p.purchaseNo,
                amount = p.totalAmount,
                note = p.note,
                runningBalance = 0.0
            )
            rawEvents.add(Triple(ts, "P", entry))
        }

        for (r in periodReturns) {
            val ts = r.createdAt?.toDate()?.time ?: 0L
            val entry = SupplierStatementEntry(
                id = r.id,
                timestamp = ts,
                type = "PURCHASE_RETURN",
                referenceNo = r.returnNo,
                amount = r.amount,
                note = r.reason,
                runningBalance = 0.0
            )
            rawEvents.add(Triple(ts, "R", entry))
        }

        rawEvents.sortBy { it.first }

        for ((_, type, entry) in rawEvents) {
            if (type == "P") {
                running += entry.amount
            } else {
                running = (running - entry.amount).coerceAtLeast(0.0)
            }
            entries.add(entry.copy(runningBalance = running))
        }

        val totalPurchases = periodPurchases.sumOf { it.totalAmount }
        val totalReturns = periodReturns.sumOf { it.amount }
        val totalPayments = periodPurchases.sumOf { it.paidAmount }

        return SupplierStatementReport(
            supplier = supplier,
            period = period,
            startDate = start,
            endDate = end,
            openingBalance = openingBalance,
            entries = entries.reversed(),
            totalPurchases = totalPurchases,
            totalPayments = totalPayments,
            totalReturns = totalReturns,
            closingBalance = running
        )
    }

    /**
     * Complete month-end Monthly Report calculation reusing existing data and formulas.
     */
    fun calculateMonthlyReportData(
        year: Int,
        month: Int, // 0-based Calendar month (0 = Jan, 11 = Dec)
        allSales: List<Sale>,
        products: List<Product>,
        customers: List<Customer>,
        suppliers: List<Supplier>,
        allPayments: List<Payment>,
        lang: AppLanguage = AppLanguage.EN,
        allPurchases: List<Purchase> = emptyList(),
        allPurchaseReturns: List<PurchaseReturn> = emptyList(),
        allSupplierPayments: List<SupplierPayment> = emptyList(),
        allExpenses: List<Expense> = emptyList(),
        allOtherIncomes: List<OtherIncome> = emptyList(),
        accounts: List<Account> = emptyList(),
        allAdjustments: List<AccountAdjustment> = emptyList()
    ): MonthlyReportData {
        val calStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = calStart.timeInMillis

        val calEnd = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endMillis = calEnd.timeInMillis
        val nowMillis = System.currentTimeMillis()
        val asOfMillis = minOf(nowMillis, endMillis)
        val dayMs = 86_400_000L

        val monthLabel = ShopLocale.formatMonthYear(year, month, lang)

        fun saleTs(s: Sale): Long = s.createdAt?.toDate()?.time ?: 0L
        fun paymentTs(p: Payment): Long = if (p.paymentDate > 0L) p.paymentDate else (p.createdAt?.toDate()?.time ?: 0L)
        fun purchaseTs(p: Purchase): Long = p.createdAt?.toDate()?.time ?: 0L
        fun returnTs(r: PurchaseReturn): Long = r.createdAt?.toDate()?.time ?: 0L
        fun supPaymentTs(sp: SupplierPayment): Long = if (sp.paymentDate > 0L) sp.paymentDate else (sp.createdAt?.toDate()?.time ?: 0L)
        fun expenseTs(e: Expense): Long = if (e.date > 0L) e.date else (e.createdAt?.toDate()?.time ?: 0L)
        fun incomeTs(i: OtherIncome): Long = if (i.date > 0L) i.date else (i.createdAt?.toDate()?.time ?: 0L)

        // -------------------------------------------------------------
        // 1 & 2. SALES
        // -------------------------------------------------------------
        val monthSales = allSales.filter { saleTs(it) in startMillis..endMillis }
        val grossSales = roundMoney(
            monthSales.sumOf { s -> if (s.subtotal > 0.0) s.subtotal else (s.grandTotal + s.discount) }
        )
        val totalDiscounts = roundMoney(monthSales.sumOf { it.discount })
        val netSales = roundMoney(monthSales.sumOf { it.grandTotal })
        val totalOrders = monthSales.size.toLong()
        val totalItemsSold = roundStock(monthSales.sumOf { it.totalItems })
        val averageOrderValue = if (totalOrders > 0L) roundMoney(netSales / totalOrders.toDouble()) else 0.0
        val cashReceivedSales = roundMoney(monthSales.sumOf { salePaidAmount(it) })
        val bakiCreditSales = roundMoney(monthSales.sumOf { saleDueAmount(it) })

        val customerStatsMap = mutableMapOf<String, MonthlyCustomerStat>()
        var cashSaleBought = 0.0
        for (sale in monthSales) {
            val isWalkInCash = saleDueAmount(sale) <= 0.001 && (sale.customerName == "Cash Sale" || sale.customerId.isBlank())
            if (isWalkInCash) {
                cashSaleBought += sale.grandTotal
            } else {
                val key = sale.customerId.ifBlank { sale.customerName.trim().lowercase() }
                val current = customerStatsMap[key]
                val paid = salePaidAmount(sale)
                val baki = saleDueAmount(sale)
                if (current == null) {
                    customerStatsMap[key] = MonthlyCustomerStat(
                        customerName = sale.customerName,
                        customerPhone = sale.customerPhone,
                        totalBought = sale.grandTotal,
                        paidAmount = paid,
                        bakiAmount = baki
                    )
                } else {
                    customerStatsMap[key] = current.copy(
                        totalBought = current.totalBought + sale.grandTotal,
                        paidAmount = current.paidAmount + paid,
                        bakiAmount = current.bakiAmount + baki
                    )
                }
            }
        }
        val customerStatsList = mutableListOf<MonthlyCustomerStat>()
        if (cashSaleBought > 0.0) {
            customerStatsList.add(
                MonthlyCustomerStat(
                    customerName = "Cash Sale",
                    customerPhone = "",
                    totalBought = roundMoney(cashSaleBought),
                    paidAmount = roundMoney(cashSaleBought),
                    bakiAmount = 0.0
                )
            )
        }
        val namedCustomerStatsSorted = customerStatsMap.values.sortedByDescending { it.totalBought }
        customerStatsList.addAll(namedCustomerStatsSorted)

        val itemsSold = calculateProductsSoldStats(monthSales).map { stat ->
            if (lang == AppLanguage.BN) {
                val prod = products.firstOrNull { it.id == stat.productId }
                val bnName = prod?.nameBn?.takeIf { it.isNotBlank() } ?: stat.productName
                stat.copy(productName = bnName)
            } else stat
        }
        val topProductsByAmount = itemsSold.sortedByDescending { it.totalSalesAmount }.take(5)
        val topCustomersByAmount = namedCustomerStatsSorted.take(5)

        // Daily Sales breakdown (sorted chronologically by day in month)
        val dailyBucketMap = mutableMapOf<Int, MonthlyDailySalesRow>()
        val tempCal = Calendar.getInstance()
        for (sale in monthSales) {
            val ts = saleTs(sale).takeIf { it > 0L } ?: startMillis
            tempCal.timeInMillis = ts
            val dom = tempCal.get(Calendar.DAY_OF_MONTH)
            val existing = dailyBucketMap[dom]
            if (existing == null) {
                dailyBucketMap[dom] = MonthlyDailySalesRow(
                    dateLabel = ShopLocale.formatDayMonth(ts, lang),
                    timestamp = ts,
                    orders = 1L,
                    itemsSold = roundStock(sale.totalItems),
                    salesAmount = roundMoney(sale.grandTotal),
                    profitAmount = roundMoney(sale.profit)
                )
            } else {
                dailyBucketMap[dom] = existing.copy(
                    orders = existing.orders + 1L,
                    itemsSold = roundStock(existing.itemsSold + sale.totalItems),
                    salesAmount = roundMoney(existing.salesAmount + sale.grandTotal),
                    profitAmount = roundMoney(existing.profitAmount + sale.profit)
                )
            }
        }
        val dailySalesRows = dailyBucketMap.entries
            .sortedBy { it.key }
            .map { it.value }

        // Sales by Category (matching calculateSalesByCategory + profit attribution)
        val productCategoryMap = products.associate { it.id to it.category.ifBlank { "General" } }
        val categorySalesAccum = mutableMapOf<String, Triple<Double, Double, Double>>() // sales, items, profit
        for (sale in monthSales) {
            for (item in sale.items) {
                val cat = productCategoryMap[item.productId] ?: "General"
                val cur = categorySalesAccum[cat] ?: Triple(0.0, 0.0, 0.0)
                categorySalesAccum[cat] = Triple(
                    cur.first + item.lineTotal,
                    cur.second + saleItemStockQty(item),
                    cur.third + item.profit
                )
            }
        }
        val categoryGrandSales = categorySalesAccum.values.sumOf { it.first }
        val salesByCategoryRows = categorySalesAccum.map { (cat, triple) ->
            val pct = if (categoryGrandSales > 0.0) (triple.first / categoryGrandSales) * 100.0 else 0.0
            MonthlyCategorySalesRow(
                category = cat,
                itemsCount = roundStock(triple.second),
                salesAmount = roundMoney(triple.first),
                profitAmount = roundMoney(triple.third),
                percentage = pct
            )
        }.sortedByDescending { it.salesAmount }

        val salesByCategoryTotalItems = roundStock(salesByCategoryRows.sumOf { it.itemsCount })
        val salesByCategoryTotalSales = roundMoney(salesByCategoryRows.sumOf { it.salesAmount })
        val salesByCategoryTotalProfit = roundMoney(salesByCategoryRows.sumOf { it.profitAmount })

        val dailySalesSum = roundMoney(dailySalesRows.sumOf { it.salesAmount })
        val paidPlusBaki = roundMoney(cashReceivedSales + bakiCreditSales)
        val salesExpectedNet = roundMoney(grossSales - totalDiscounts)
        val salesDiff = roundMoney(netSales - salesExpectedNet)
        val salesSplitDiff = roundMoney(netSales - paidPlusBaki)
        val salesDailyDiff = roundMoney(netSales - dailySalesSum)
        val salesAllMatch = kotlin.math.abs(salesDiff) <= 0.05 &&
            kotlin.math.abs(salesSplitDiff) <= 0.05 &&
            kotlin.math.abs(salesDailyDiff) <= 0.05
        val salesCheck = when {
            kotlin.math.abs(salesDiff) > 0.05 -> MonthlyReconciliationCheck(
                key = "sales",
                opening = grossSales,
                totalIn = 0.0,
                totalOut = totalDiscounts,
                actualClosing = netSales,
                expectedClosing = salesExpectedNet,
                difference = salesDiff,
                matches = false
            )
            kotlin.math.abs(salesSplitDiff) > 0.05 -> MonthlyReconciliationCheck(
                key = "sales",
                opening = cashReceivedSales,
                totalIn = bakiCreditSales,
                totalOut = 0.0,
                actualClosing = netSales,
                expectedClosing = paidPlusBaki,
                difference = salesSplitDiff,
                matches = false
            )
            kotlin.math.abs(salesDailyDiff) > 0.05 -> MonthlyReconciliationCheck(
                key = "sales",
                opening = 0.0,
                totalIn = dailySalesSum,
                totalOut = 0.0,
                actualClosing = netSales,
                expectedClosing = dailySalesSum,
                difference = salesDailyDiff,
                matches = false
            )
            else -> MonthlyReconciliationCheck(
                key = "sales",
                opening = grossSales,
                totalIn = 0.0,
                totalOut = totalDiscounts,
                actualClosing = netSales,
                expectedClosing = salesExpectedNet,
                difference = 0.0,
                matches = salesAllMatch
            )
        }

        // -------------------------------------------------------------
        // 6 & 7. PURCHASES, EXPENSES & OTHER INCOME
        // -------------------------------------------------------------
        val activeExpenses = allExpenses.filter { !it.isDeleted }
        val activeOtherIncomes = allOtherIncomes.filter { !it.isDeleted }
        val monthExpenses = activeExpenses.filter { expenseTs(it) in startMillis..endMillis }
        val monthOtherIncomes = activeOtherIncomes.filter { incomeTs(it) in startMillis..endMillis }

        val monthPurchases = allPurchases.filter { purchaseTs(it) in startMillis..endMillis }
        val monthReturns = allPurchaseReturns.filter { returnTs(it) in startMillis..endMillis }
        val monthSupplierPayments = allSupplierPayments.filter { supPaymentTs(it) in startMillis..endMillis }

        // -------------------------------------------------------------
        // 8. PROFIT & LOSS (reusing calculateProfitAndLoss)
        // -------------------------------------------------------------
        val pnl = calculateProfitAndLoss(
            sales = monthSales,
            purchases = monthPurchases,
            returns = monthReturns,
            expenses = monthExpenses,
            otherIncomes = monthOtherIncomes,
            isStockBased = true
        )
        val costOfGoodsSold = roundMoney(pnl.costOfGoodsSold)
        val grossProfit = roundMoney(pnl.grossProfit)
        val totalExpenses = roundMoney(pnl.expenses)
        val otherIncomeTotal = roundMoney(pnl.otherIncome)
        val netProfit = roundMoney(pnl.netProfit)

        val summaryExpectedNet = roundMoney(grossProfit + otherIncomeTotal - totalExpenses)
        val summaryDiff = roundMoney(netProfit - summaryExpectedNet)
        val summaryCheck = MonthlyReconciliationCheck(
            key = "summary",
            opening = grossProfit,
            totalIn = otherIncomeTotal,
            totalOut = totalExpenses,
            actualClosing = netProfit,
            expectedClosing = summaryExpectedNet,
            difference = summaryDiff,
            matches = kotlin.math.abs(summaryDiff) <= 0.05
        )

        val expCountByCat = monthExpenses.groupingBy { it.category.ifBlank { "Other" } }.eachCount()
        val expensesByCategory = pnl.expensesByCategory.entries
            .sortedByDescending { it.value }
            .map { (cat, amt) ->
                val cleanCat = cat.ifBlank { "Other" }
                val pct = if (totalExpenses > 0.0) (amt / totalExpenses) * 100.0 else 0.0
                MonthlyExpenseCategoryRow(
                    category = cleanCat,
                    amount = roundMoney(amt),
                    percentage = pct,
                    count = expCountByCat[cleanCat] ?: 0
                )
            }

        val incGroup = monthOtherIncomes.groupBy { it.category.ifBlank { "Other" } }
        val incomesByCategory = incGroup.entries
            .map { (cat, list) ->
                val amt = list.sumOf { it.amount }
                val pct = if (otherIncomeTotal > 0.0) (amt / otherIncomeTotal) * 100.0 else 0.0
                MonthlyIncomeCategoryRow(
                    category = cat,
                    amount = roundMoney(amt),
                    percentage = pct,
                    count = list.size
                )
            }
            .sortedByDescending { it.amount }

        val totalExpenseCount = monthExpenses.size.toLong()
        val totalOtherIncomeCount = monthOtherIncomes.size.toLong()
        val expCatSum = roundMoney(expensesByCategory.sumOf { it.amount })
        val incCatSum = roundMoney(incomesByCategory.sumOf { it.amount })
        val netExpIncExpected = roundMoney(incCatSum - expCatSum)
        val netExpIncActual = roundMoney(otherIncomeTotal - totalExpenses)
        val expIncDiff = roundMoney(netExpIncActual - netExpIncExpected)
        val expensesIncomeCheck = MonthlyReconciliationCheck(
            key = "expenses_income",
            opening = 0.0,
            totalIn = incCatSum,
            totalOut = expCatSum,
            actualClosing = netExpIncActual,
            expectedClosing = netExpIncExpected,
            difference = expIncDiff,
            matches = kotlin.math.abs(expIncDiff) <= 0.05 &&
                kotlin.math.abs(expCatSum - totalExpenses) <= 0.05 &&
                kotlin.math.abs(incCatSum - otherIncomeTotal) <= 0.05
        )

        // -------------------------------------------------------------
        // 8. CASH & BANK (per account using calculateAccountStatement)
        // -------------------------------------------------------------
        val activeAccounts = accounts.filter { !it.isArchived }
        val accountCashRows = activeAccounts.map { acc ->
            val stmt = calculateAccountStatement(
                account = acc,
                allSales = allSales,
                allPaymentsIn = allPayments,
                allPurchases = allPurchases,
                allSupplierPayments = allSupplierPayments,
                allExpenses = activeExpenses,
                allOtherIncomes = activeOtherIncomes,
                allAdjustments = allAdjustments,
                period = HistoryPeriod.CUSTOM,
                customRange = startMillis to endMillis
            )
            MonthlyAccountCashRow(
                accountId = acc.id,
                accountName = acc.name,
                accountType = acc.type,
                opening = roundMoney(stmt.openingBalance),
                moneyIn = roundMoney(stmt.totalMoneyIn),
                moneyOut = roundMoney(stmt.totalMoneyOut),
                closing = roundMoney(stmt.closingBalance)
            )
        }
        val cashOpeningTotal = roundMoney(accountCashRows.sumOf { it.opening })
        val cashInTotal = roundMoney(accountCashRows.sumOf { it.moneyIn })
        val cashOutTotal = roundMoney(accountCashRows.sumOf { it.moneyOut })
        val cashClosingTotal = roundMoney(accountCashRows.sumOf { it.closing })
        val cashExpectedClosing = roundMoney(cashOpeningTotal + cashInTotal - cashOutTotal)
        val cashDiff = roundMoney(cashClosingTotal - cashExpectedClosing)
        val cashCheck = MonthlyReconciliationCheck(
            key = "cash",
            opening = cashOpeningTotal,
            totalIn = cashInTotal,
            totalOut = cashOutTotal,
            actualClosing = cashClosingTotal,
            expectedClosing = cashExpectedClosing,
            difference = cashDiff,
            matches = kotlin.math.abs(cashDiff) <= 0.05
        )

        val summaryCheckNoProfit = cashCheck.copy(key = "summary_no_profit")

        // -------------------------------------------------------------
        // 3. BAKI / CREDIT (opening, new credit, collected, closing, overdue & ageing 0-30, 31-60, 61-90, 90+)
        // -------------------------------------------------------------
        val allBakiCustomers = customers.toMutableList()
        for (sale in allSales) {
            if ((sale.paymentType == "baki" || saleDueAmount(sale) > 0.001) && sale.customerName != "Cash Sale" && sale.customerName.isNotBlank()) {
                if (allBakiCustomers.none { doesSaleMatchCustomer(sale, it) }) {
                    allBakiCustomers.add(
                        Customer(
                            id = sale.customerId.ifBlank { "legacy_${sale.customerName.trim().lowercase().replace(' ', '_')}" },
                            name = sale.customerName.trim(),
                            mobile = sale.customerPhone.trim(),
                            notes = "Previous Baki Sale"
                        )
                    )
                }
            }
        }

        var bakiOpeningSum = 0.0
        var bakiNewCreditSum = 0.0
        var bakiCollectedSum = 0.0
        var bakiClosingSum = 0.0
        var bucket0To30 = 0.0
        var bucket31To60 = 0.0
        var bucket61To90 = 0.0
        var bucket90Plus = 0.0
        val debtorRows = mutableListOf<MonthlyDebtorRow>()

        for (cust in allBakiCustomers) {
            val custBakiSalesAll = allSales
                .filter { doesSaleMatchCustomer(it, cust) && saleDueAmount(it) > 0.001 }
                .sortedBy { saleTs(it) }
            val custPaymentsAll = allPayments.filter { doesPaymentMatchCustomer(it, cust) }

            val priorBaki = custBakiSalesAll.filter { saleTs(it) < startMillis }.sumOf { saleDueAmount(it) }
            val priorPaid = custPaymentsAll.filter { paymentTs(it) < startMillis }.sumOf { it.amount }
            val opening = (priorBaki - priorPaid).coerceAtLeast(0.0)

            val monthBaki = custBakiSalesAll.filter { saleTs(it) in startMillis..endMillis }.sumOf { saleDueAmount(it) }
            val monthPaid = custPaymentsAll.filter { paymentTs(it) in startMillis..endMillis }.sumOf { it.amount }
            val closing = (opening + monthBaki - monthPaid).coerceAtLeast(0.0)

            bakiOpeningSum += opening
            bakiNewCreditSum += monthBaki
            bakiCollectedSum += monthPaid
            bakiClosingSum += closing

            if (closing > 0.001) {
                // FIFO matching against baki invoices up to endMillis to determine overdue age & ageing buckets
                val bakiUpToEnd = custBakiSalesAll.filter { saleTs(it) <= endMillis }
                var remainingPaid = custPaymentsAll.filter { paymentTs(it) <= endMillis }.sumOf { it.amount }
                var oldestUnpaidTs: Long? = null
                var assignedToBuckets = 0.0

                for (bSale in bakiUpToEnd) {
                    val amount = saleDueAmount(bSale).coerceAtLeast(0.0)
                    val covered = minOf(amount, remainingPaid)
                    remainingPaid -= covered
                    val unpaid = amount - covered
                    if (unpaid > 0.001) {
                        val sTs = saleTs(bSale).takeIf { it > 0L } ?: endMillis
                        if (oldestUnpaidTs == null || sTs < oldestUnpaidTs) {
                            oldestUnpaidTs = sTs
                        }
                        val ageDays = ((asOfMillis - sTs).coerceAtLeast(0L)) / dayMs
                        when {
                            ageDays <= 30L -> bucket0To30 += unpaid
                            ageDays <= 60L -> bucket31To60 += unpaid
                            ageDays <= 90L -> bucket61To90 += unpaid
                            else -> bucket90Plus += unpaid
                        }
                        assignedToBuckets += unpaid
                    }
                }
                if (closing - assignedToBuckets > 0.001) {
                    bucket0To30 += (closing - assignedToBuckets)
                }
                val daysOverdue = oldestUnpaidTs?.let { ((asOfMillis - it).coerceAtLeast(0L)) / dayMs } ?: 0L
                val bucketTag = when {
                    daysOverdue <= 30L -> "0-30"
                    daysOverdue <= 60L -> "31-60"
                    daysOverdue <= 90L -> "61-90"
                    else -> "90+"
                }
                debtorRows.add(
                    MonthlyDebtorRow(
                        customerId = cust.id,
                        customerName = cust.name,
                        customerPhone = cust.mobile,
                        amountOwed = roundMoney(closing),
                        daysOverdue = daysOverdue,
                        ageingBucket = bucketTag
                    )
                )
            }
        }
        // Include any orphan customer payments in the month that didn't match a customer record
        val matchedPaymentIds = allBakiCustomers
            .flatMap { cust -> allPayments.filter { doesPaymentMatchCustomer(it, cust) } }
            .map { it.id }
            .toSet()
        val orphanMonthPayments = allPayments
            .filter { it.id !in matchedPaymentIds && paymentTs(it) in startMillis..endMillis }
            .sumOf { it.amount }
        bakiCollectedSum += orphanMonthPayments

        bakiOpeningSum = roundMoney(bakiOpeningSum)
        bakiNewCreditSum = roundMoney(bakiNewCreditSum)
        bakiCollectedSum = roundMoney(bakiCollectedSum)
        bakiClosingSum = roundMoney(bakiClosingSum)

        val bakiExpectedClosing = roundMoney(bakiOpeningSum + bakiNewCreditSum - bakiCollectedSum)
        val bakiDiff = roundMoney(bakiClosingSum - bakiExpectedClosing)
        val bakiCheck = MonthlyReconciliationCheck(
            key = "baki",
            opening = bakiOpeningSum,
            totalIn = bakiNewCreditSum,
            totalOut = bakiCollectedSum,
            actualClosing = bakiClosingSum,
            expectedClosing = bakiExpectedClosing,
            difference = bakiDiff,
            matches = kotlin.math.abs(bakiDiff) <= 0.05
        )
        val bakiHasActivityOrDue = bakiOpeningSum > 0.001 ||
            bakiNewCreditSum > 0.001 ||
            bakiCollectedSum > 0.001 ||
            bakiClosingSum > 0.001 ||
            debtorRows.isNotEmpty()
        val sortedDebtorRows = debtorRows.sortedByDescending { it.amountOwed }
        val orderedBucketKeys = listOf("0-30", "31-60", "61-90", "90+")
        val bakiAgeingGroups = orderedBucketKeys.map { bucketKey ->
            val groupDebtors = sortedDebtorRows.filter { it.ageingBucket == bucketKey }
            MonthlyDebtorAgeingGroup(
                bucketKey = bucketKey,
                debtors = groupDebtors,
                count = groupDebtors.size.toLong(),
                totalAmount = roundMoney(groupDebtors.sumOf { it.amountOwed })
            )
        }
        val debtorRowsGrouped = bakiAgeingGroups.flatMap { it.debtors }
        val debtorCount = sortedDebtorRows.size.toLong()

        // -------------------------------------------------------------
        // 4 & 6. SUPPLIERS & PURCHASES
        // -------------------------------------------------------------
        fun purchaseCreditAmount(p: Purchase): Double {
            if (p.dueAmount > 0.001) return p.dueAmount
            if (p.paymentType == "credit" && p.paidAmount <= 0.001) return p.totalAmount
            return (p.totalAmount - p.paidAmount).coerceAtLeast(0.0).takeIf { p.paymentType == "credit" } ?: 0.0
        }

        fun purchasePaidAmountEffective(p: Purchase): Double {
            if (p.paidAmount > 0.001) return p.paidAmount
            if (p.paymentType != "credit" && p.dueAmount <= 0.001) return p.totalAmount
            return (p.totalAmount - purchaseCreditAmount(p)).coerceAtLeast(0.0)
        }

        fun purchaseMatchesSup(p: Purchase, s: Supplier): Boolean =
            if (p.supplierId.isNotBlank()) p.supplierId == s.id
            else s.name.isNotBlank() && p.supplierName.trim().equals(s.name.trim(), ignoreCase = true)

        fun spMatchesSup(sp: SupplierPayment, s: Supplier): Boolean =
            if (sp.supplierId.isNotBlank()) sp.supplierId == s.id
            else s.name.isNotBlank() && sp.supplierName.trim().equals(s.name.trim(), ignoreCase = true)

        fun returnMatchesSup(r: PurchaseReturn, s: Supplier): Boolean =
            if (r.supplierId.isNotBlank()) r.supplierId == s.id
            else s.name.isNotBlank() && r.supplierName.trim().equals(s.name.trim(), ignoreCase = true)

        val supplierPurchasesTotal = roundMoney(monthPurchases.sumOf { it.totalAmount })
        val supplierCreditPurchasesTotal = roundMoney(monthPurchases.sumOf { purchaseCreditAmount(it) })
        val supplierPaidAtPurchase = roundMoney(monthPurchases.sumOf { purchasePaidAmountEffective(it) })
        val supplierPayOutTotal = roundMoney(monthSupplierPayments.sumOf { it.amount })
        val supplierPaidTotal = roundMoney(supplierPaidAtPurchase + supplierPayOutTotal)
        val supplierReturnsTotal = roundMoney(monthReturns.sumOf { it.amount })

        var supplierOpeningSum = 0.0
        var supplierClosingSum = 0.0
        val supplierOwedRows = mutableListOf<MonthlySupplierOwedRow>()
        val supplierRows = mutableListOf<MonthlySupplierRow>()

        for (sup in suppliers) {
            val supPurchases = allPurchases.filter { purchaseMatchesSup(it, sup) }
            val supPayments = allSupplierPayments.filter { spMatchesSup(it, sup) }
            val supReturns = allPurchaseReturns.filter { returnMatchesSup(it, sup) }

            val priorCredit = supPurchases.filter { purchaseTs(it) < startMillis }.sumOf { purchaseCreditAmount(it) }
            val priorPayOut = supPayments.filter { supPaymentTs(it) < startMillis }.sumOf { it.amount }
            val priorRet = supReturns.filter { returnTs(it) < startMillis }.sumOf { it.amount }
            val supOpening = (priorCredit - priorPayOut - priorRet).coerceAtLeast(0.0)

            val monthSupPurchases = supPurchases.filter { purchaseTs(it) in startMillis..endMillis }
            val monthSupCredit = monthSupPurchases.sumOf { purchaseCreditAmount(it) }
            val monthSupTotal = monthSupPurchases.sumOf { it.totalAmount }
            val monthSupPaidAtPurchase = monthSupPurchases.sumOf { purchasePaidAmountEffective(it) }
            val monthSupPayOut = supPayments.filter { supPaymentTs(it) in startMillis..endMillis }.sumOf { it.amount }
            val monthSupRet = supReturns.filter { returnTs(it) in startMillis..endMillis }.sumOf { it.amount }

            // Back out post-endMillis movements from current stored Supplier.balance
            val afterCredit = supPurchases.filter { purchaseTs(it) > endMillis }.sumOf { purchaseCreditAmount(it) }
            val afterPayOut = supPayments.filter { supPaymentTs(it) > endMillis }.sumOf { it.amount }
            val afterRet = supReturns.filter { returnTs(it) > endMillis }.sumOf { it.amount }
            val supClosing = (sup.balance - afterCredit + afterPayOut + afterRet).coerceAtLeast(0.0)

            supplierOpeningSum += supOpening
            supplierClosingSum += supClosing
            if (supClosing > 0.001) {
                supplierOwedRows.add(
                    MonthlySupplierOwedRow(
                        supplierId = sup.id,
                        supplierName = sup.name,
                        supplierPhone = sup.mobile,
                        amountOwed = roundMoney(supClosing)
                    )
                )
            }
            val supHasActivity = supOpening > 0.001 ||
                monthSupTotal > 0.001 ||
                monthSupCredit > 0.001 ||
                monthSupPayOut > 0.001 ||
                monthSupRet > 0.001 ||
                supClosing > 0.001
            if (supHasActivity) {
                supplierRows.add(
                    MonthlySupplierRow(
                        supplierId = sup.id,
                        supplierName = sup.name,
                        supplierPhone = sup.mobile,
                        opening = roundMoney(supOpening),
                        creditPurchases = roundMoney(monthSupCredit),
                        totalPurchases = roundMoney(monthSupTotal),
                        paidAtPurchase = roundMoney(monthSupPaidAtPurchase),
                        paymentsOut = roundMoney(monthSupPayOut),
                        returns = roundMoney(monthSupRet),
                        closing = roundMoney(supClosing)
                    )
                )
            }
        }
        supplierOpeningSum = roundMoney(supplierOpeningSum)
        supplierClosingSum = roundMoney(supplierClosingSum)

        // Reconciliation formula for Supplier Payable balance:
        // Opening Payable + Credit Purchases - Payments Out - Purchase Returns = Closing Payable
        val supplierOutCombined = roundMoney(supplierPayOutTotal + supplierReturnsTotal)
        val supplierExpectedClosing = roundMoney(supplierOpeningSum + supplierCreditPurchasesTotal - supplierOutCombined)
        val supplierDiff = roundMoney(supplierClosingSum - supplierExpectedClosing)
        val supplierCheck = MonthlyReconciliationCheck(
            key = "suppliers",
            opening = supplierOpeningSum,
            totalIn = supplierCreditPurchasesTotal,
            totalOut = supplierOutCombined,
            actualClosing = supplierClosingSum,
            expectedClosing = supplierExpectedClosing,
            difference = supplierDiff,
            matches = kotlin.math.abs(supplierDiff) <= 0.05
        )
        val supplierHasActivityOrDue = supplierOpeningSum > 0.001 ||
            supplierPurchasesTotal > 0.001 ||
            supplierCreditPurchasesTotal > 0.001 ||
            supplierPaidTotal > 0.001 ||
            supplierReturnsTotal > 0.001 ||
            supplierClosingSum > 0.001 ||
            supplierRows.isNotEmpty()

        // 6. PURCHASES SECTION
        val purchaseRows = monthPurchases
            .sortedByDescending { purchaseTs(it) }
            .map { p ->
                MonthlyPurchaseRow(
                    purchaseId = p.id,
                    purchaseNo = p.purchaseNo.ifBlank { p.id.takeLast(6).uppercase() },
                    dateMillis = purchaseTs(p),
                    supplierName = p.supplierName.ifBlank { "Supplier" },
                    totalAmount = roundMoney(p.totalAmount),
                    paidAmount = roundMoney(purchasePaidAmountEffective(p)),
                    creditAmount = roundMoney(purchaseCreditAmount(p)),
                    paymentType = p.paymentType
                )
            }
        val returnRows = monthReturns
            .sortedByDescending { returnTs(it) }
            .map { r ->
                MonthlyReturnRow(
                    returnId = r.id,
                    returnNo = r.returnNo.ifBlank { r.id.takeLast(6).uppercase() },
                    dateMillis = returnTs(r),
                    supplierName = r.supplierName.ifBlank { "Supplier" },
                    amount = roundMoney(r.amount),
                    reason = r.reason
                )
            }
        val totalBought = supplierPurchasesTotal
        val purchaseReturnsTotal = supplierReturnsTotal
        val netPurchases = roundMoney(totalBought - purchaseReturnsTotal)
        val purchaseRowsSum = roundMoney(purchaseRows.sumOf { it.totalAmount })
        val returnRowsSum = roundMoney(returnRows.sumOf { it.amount })
        val purchasesExpectedNet = roundMoney(purchaseRowsSum - returnRowsSum)
        val purchasesDiff = roundMoney(netPurchases - purchasesExpectedNet)
        val purchasesSplitExpected = roundMoney(supplierPaidAtPurchase + supplierCreditPurchasesTotal)
        val purchasesSplitDiff = roundMoney(totalBought - purchasesSplitExpected)
        val purchasesCheck = if (kotlin.math.abs(purchasesDiff) > 0.05) {
            MonthlyReconciliationCheck(
                key = "purchases",
                opening = purchaseRowsSum,
                totalIn = 0.0,
                totalOut = returnRowsSum,
                actualClosing = netPurchases,
                expectedClosing = purchasesExpectedNet,
                difference = purchasesDiff,
                matches = false
            )
        } else if (kotlin.math.abs(purchasesSplitDiff) > 0.05) {
            MonthlyReconciliationCheck(
                key = "purchases",
                opening = supplierPaidAtPurchase,
                totalIn = supplierCreditPurchasesTotal,
                totalOut = 0.0,
                actualClosing = totalBought,
                expectedClosing = purchasesSplitExpected,
                difference = purchasesSplitDiff,
                matches = false
            )
        } else {
            MonthlyReconciliationCheck(
                key = "purchases",
                opening = totalBought,
                totalIn = 0.0,
                totalOut = purchaseReturnsTotal,
                actualClosing = netPurchases,
                expectedClosing = purchasesExpectedNet,
                difference = 0.0,
                matches = true
            )
        }
        val purchaseHasActivity = monthPurchases.isNotEmpty() || monthReturns.isNotEmpty()

        // -------------------------------------------------------------
        // 5. STOCK (stock value at cost, item count, low stock, 30-day slow stock)
        // Opening stock is ONLY reported if it can be computed cleanly without guessing.
        // Because StoreOS mutates Product.stock/buyPrice directly (manual edits, restocks,
        // deletes) without a historical cost-layer ledger, we do NOT guess opening stock.
        // -------------------------------------------------------------
        val activeProducts = products
        val stockItemCount = activeProducts.size.toLong()
        val totalStockUnits = roundStock(activeProducts.sumOf { it.stock.coerceAtLeast(0.0) })
        val currentStockValueAtCost = roundMoney(activeProducts.sumOf { it.stock.coerceAtLeast(0.0) * it.buyPrice })
        val purchasedAfterEndCost = roundMoney(
            allPurchases.filter { purchaseTs(it) > endMillis }.sumOf { it.totalAmount }
        )
        val returnsAfterEndCost = roundMoney(
            allPurchaseReturns.filter { returnTs(it) > endMillis }.sumOf { it.amount }
        )
        val soldAfterEndAtCost = roundMoney(
            allSales.filter { saleTs(it) > endMillis }
                .flatMap { it.items }
                .sumOf { it.buyPrice * it.qty }
        )
        val closingStockValue = if (endMillis >= nowMillis) {
            currentStockValueAtCost
        } else {
            roundMoney((currentStockValueAtCost - purchasedAfterEndCost + returnsAfterEndCost + soldAfterEndAtCost).coerceAtLeast(0.0))
        }
        val purchasedStockValue = supplierPurchasesTotal
        val soldStockAtCost = costOfGoodsSold
        val openingStockAvailable = false
        val openingStockValueComputed: Double? = null
        val openingStockValue = 0.0

        val stockCheck = MonthlyReconciliationCheck(
            key = "stock",
            opening = 0.0,
            totalIn = purchasedStockValue,
            totalOut = soldStockAtCost,
            actualClosing = closingStockValue,
            expectedClosing = closingStockValue,
            difference = 0.0,
            matches = true,
            isAvailable = false,
            note = "Opening stock is not tracked"
        )

        val lowStockItems = activeProducts
            .filter { it.stock <= it.lowStockLimit }
            .sortedBy { it.stock }
            .map { prod ->
                MonthlyLowStockRow(
                    productId = prod.id,
                    productName = if (lang == AppLanguage.BN && prod.nameBn.isNotBlank()) prod.nameBn else prod.name,
                    stock = prod.stock,
                    lowStockLimit = prod.lowStockLimit,
                    unit = effectiveStockUnit(prod),
                    stockValueAtCost = roundMoney(prod.stock.coerceAtLeast(0.0) * prod.buyPrice)
                )
            }

        val thirtyDaysStart = asOfMillis - (30L * dayMs)
        val salesInLast30Days = allSales.filter { tsSale ->
            val ts = saleTs(tsSale)
            ts in thirtyDaysStart..asOfMillis
        }
        val slowProducts = calculateSlowSellingProducts(activeProducts, salesInLast30Days)
        val slowStock30DayItems = slowProducts
            .map { prod ->
                MonthlySlowStockRow(
                    productId = prod.id,
                    productName = if (lang == AppLanguage.BN && prod.nameBn.isNotBlank()) prod.nameBn else prod.name,
                    stock = prod.stock,
                    unit = effectiveStockUnit(prod),
                    moneyStuck = roundMoney(prod.stock.coerceAtLeast(0.0) * prod.buyPrice)
                )
            }
            .sortedByDescending { it.moneyStuck }
        val totalSlowStockMoneyStuck = roundMoney(slowStock30DayItems.sumOf { it.moneyStuck })

        val isMonthEmpty = monthSales.isEmpty() &&
            monthPurchases.isEmpty() &&
            monthReturns.isEmpty() &&
            monthExpenses.isEmpty() &&
            monthOtherIncomes.isEmpty() &&
            monthSupplierPayments.isEmpty() &&
            bakiNewCreditSum <= 0.001 &&
            bakiCollectedSum <= 0.001 &&
            cashInTotal <= 0.001 &&
            cashOutTotal <= 0.001

        return MonthlyReportData(
            year = year,
            month = month,
            monthLabel = monthLabel,
            totalSales = netSales,
            totalProfit = grossProfit,
            totalOrders = totalOrders,
            totalItemsSold = totalItemsSold,
            stockValueAtCost = closingStockValue,
            bakiCustomersOwe = bakiClosingSum,
            debtToSuppliers = supplierClosingSum,
            customerStats = customerStatsList,
            itemsSold = itemsSold,
            grossSales = grossSales,
            totalDiscounts = totalDiscounts,
            netSales = netSales,
            averageOrderValue = averageOrderValue,
            cashReceivedSales = cashReceivedSales,
            bakiCreditSales = bakiCreditSales,
            topProductsByAmount = topProductsByAmount,
            topCustomersByAmount = topCustomersByAmount,
            dailySalesRows = dailySalesRows,
            salesByCategoryRows = salesByCategoryRows,
            salesByCategoryTotalItems = salesByCategoryTotalItems,
            salesByCategoryTotalSales = salesByCategoryTotalSales,
            salesByCategoryTotalProfit = salesByCategoryTotalProfit,
            costOfGoodsSold = costOfGoodsSold,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            otherIncome = otherIncomeTotal,
            netProfit = netProfit,
            bakiOpeningReceivable = bakiOpeningSum,
            bakiNewCreditGiven = bakiNewCreditSum,
            bakiCollectedThisMonth = bakiCollectedSum,
            bakiClosingReceivable = bakiClosingSum,
            debtorRows = sortedDebtorRows,
            debtorRowsGrouped = debtorRowsGrouped,
            debtorCount = debtorCount,
            bakiAgeingGroups = bakiAgeingGroups,
            bakiAgeing = MonthlyBakiAgeing(
                bucket0To30 = roundMoney(bucket0To30),
                bucket31To60 = roundMoney(bucket31To60),
                bucket60Plus = roundMoney(bucket61To90 + bucket90Plus),
                bucket61To90 = roundMoney(bucket61To90),
                bucket90Plus = roundMoney(bucket90Plus)
            ),
            bakiHasActivityOrDue = bakiHasActivityOrDue,
            supplierOpeningPayable = supplierOpeningSum,
            supplierPurchasesThisMonth = supplierPurchasesTotal,
            supplierCreditPurchasesThisMonth = supplierCreditPurchasesTotal,
            supplierPaidAtPurchaseThisMonth = supplierPaidAtPurchase,
            supplierPaidThisMonth = supplierPaidTotal,
            supplierPaymentsOutThisMonth = supplierPayOutTotal,
            supplierReturnsThisMonth = supplierReturnsTotal,
            supplierClosingPayable = supplierClosingSum,
            supplierOwedRows = supplierOwedRows.sortedByDescending { it.amountOwed },
            supplierRows = supplierRows.sortedByDescending { maxOf(it.closing, it.totalPurchases) },
            supplierRowCount = supplierRows.size.toLong(),
            supplierHasActivityOrDue = supplierHasActivityOrDue,
            openingStockValue = openingStockValue,
            openingStockValueComputed = openingStockValueComputed,
            openingStockAvailable = openingStockAvailable,
            purchasedStockValue = purchasedStockValue,
            soldStockAtCost = soldStockAtCost,
            closingStockValue = closingStockValue,
            stockItemCount = stockItemCount,
            totalStockUnits = totalStockUnits,
            lowStockItems = lowStockItems,
            lowStockCount = lowStockItems.size.toLong(),
            slowStock30DayItems = slowStock30DayItems,
            slowStockCount = slowStock30DayItems.size.toLong(),
            totalSlowStockMoneyStuck = totalSlowStockMoneyStuck,
            totalBought = totalBought,
            purchaseReturnsTotal = purchaseReturnsTotal,
            netPurchases = netPurchases,
            purchaseRows = purchaseRows,
            returnRows = returnRows,
            purchaseHasActivity = purchaseHasActivity,
            expensesByCategory = expensesByCategory,
            totalExpenseCount = totalExpenseCount,
            incomesByCategory = incomesByCategory,
            totalOtherIncomeCount = totalOtherIncomeCount,
            netOtherIncomeMinusExpenses = netExpIncActual,
            accountCashRows = accountCashRows,
            cashOpeningTotal = cashOpeningTotal,
            cashInTotal = cashInTotal,
            cashOutTotal = cashOutTotal,
            cashClosingBalance = cashClosingTotal,
            summaryCheck = summaryCheck,
            summaryCheckNoProfit = summaryCheckNoProfit,
            salesCheck = salesCheck,
            cashCheck = cashCheck,
            bakiCheck = bakiCheck,
            supplierCheck = supplierCheck,
            stockCheck = stockCheck,
            purchasesCheck = purchasesCheck,
            expensesIncomeCheck = expensesIncomeCheck,
            isMonthEmpty = isMonthEmpty
        )
    }

    /**
     * Data Health Check scanner per Step 6.
     * Finds the 7 problem types from prompt:
     * 1. Negative stock
     * 2. Sale price lower than buying price
     * 3. Duplicate product barcodes or exact duplicate product names
     * 4. Duplicate phone numbers across customers or suppliers
     * 5. Orphan transactions (sales/purchases pointing to customer/supplier that no longer exists)
     * 6. Customer or supplier balances that do not match their transactions sum
     * 7. Products with no category
     */
    fun scanDataHealth(
        store: com.example.data.model.Store?,
        products: List<Product>,
        sales: List<Sale>,
        purchases: List<Purchase>,
        customers: List<Customer>,
        suppliers: List<Supplier>,
        payments: List<Payment>,
        accounts: List<Account> = emptyList(),
        accountBalances: Map<String, Double> = emptyMap(),
        expenses: List<Expense> = emptyList(),
        otherIncomes: List<OtherIncome> = emptyList(),
        supplierPayments: List<SupplierPayment> = emptyList(),
        adjustments: List<AccountAdjustment> = emptyList(),
        lang: AppLanguage = AppLanguage.EN
    ): List<HealthProblem> {
        val problems = mutableListOf<HealthProblem>()
        val customerMap = customers.associateBy { it.id }
        val supplierMap = suppliers.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }

        // 1. Negative stock
        for (prod in products) {
            if (prod.stock < 0.0) {
                val unitDisp = ShopLocale.displayUnit(prod.unit, lang)
                problems.add(
                    HealthProblem(
                        id = "health_neg_stock_${prod.id}",
                        type = HealthProblemType.NEGATIVE_STOCK,
                        title = ShopLocale.format("hp_title_neg_stock", lang, prod.name),
                        description = ShopLocale.format("hp_desc_neg_stock", lang, prod.stock, unitDisp),
                        recordRef = prod.name,
                        entityCollection = "products",
                        entityId = prod.id,
                        fixDescription = ShopLocale.format("hp_fix_neg_stock", lang, prod.name, unitDisp),
                        fixActionPayload = mapOf("stock" to 0.0)
                    )
                )
            }
        }

        // 2. Sale price lower than buying price
        for (prod in products) {
            if (prod.sellPrice < prod.buyPrice && prod.buyPrice > 0.0) {
                val unitDisp = ShopLocale.displayUnit(prod.unit, lang)
                val sellStr = ShopLocale.formatMoneyFixed2(prod.sellPrice, lang)
                val buyStr = ShopLocale.formatMoneyFixed2(prod.buyPrice, lang)
                val lossStr = ShopLocale.formatMoneyFixed2(prod.buyPrice - prod.sellPrice, lang)
                problems.add(
                    HealthProblem(
                        id = "health_low_sell_${prod.id}",
                        type = HealthProblemType.SELL_PRICE_LOWER_THAN_BUY_PRICE,
                        title = ShopLocale.format("hp_title_sell_below", lang, prod.name),
                        description = ShopLocale.format("hp_desc_sell_below", lang, sellStr, buyStr, lossStr, unitDisp),
                        recordRef = prod.name,
                        entityCollection = "products",
                        entityId = prod.id,
                        fixDescription = ShopLocale.format("hp_fix_sell_below", lang, buyStr),
                        fixActionPayload = mapOf("sellPrice" to prod.buyPrice)
                    )
                )
            }
        }

        // 3. Duplicate product barcodes or exact duplicate product names
        val barcodeGroups = products.filter { it.barcode.isNotBlank() }.groupBy { it.barcode.trim() }
        for ((code, list) in barcodeGroups) {
            if (list.size > 1) {
                for (i in 1 until list.size) {
                    val p = list[i]
                    problems.add(
                        HealthProblem(
                            id = "health_dup_barcode_${p.id}",
                            type = HealthProblemType.DUPLICATE_PRODUCT_BARCODE_OR_NAME,
                            title = ShopLocale.format("hp_title_dup_barcode", lang, code, p.name),
                            description = ShopLocale.format("hp_desc_dup_barcode", lang, p.name, code, list[0].name),
                            recordRef = p.name,
                            entityCollection = "products",
                            entityId = p.id,
                            fixDescription = ShopLocale.format("hp_fix_dup_barcode", lang, p.name),
                            fixActionPayload = mapOf("barcode" to "")
                        )
                    )
                }
            }
        }
        val nameGroups = products.groupBy { it.name.trim().lowercase() }
        for ((_, list) in nameGroups) {
            if (list.size > 1) {
                for (i in 1 until list.size) {
                    val p = list[i]
                    val uniqueName = "${p.name.trim()} (${i + 1})"
                    problems.add(
                        HealthProblem(
                            id = "health_dup_name_${p.id}",
                            type = HealthProblemType.DUPLICATE_PRODUCT_BARCODE_OR_NAME,
                            title = ShopLocale.format("hp_title_dup_name", lang, p.name),
                            description = ShopLocale.format("hp_desc_dup_name", lang, p.name),
                            recordRef = p.name,
                            entityCollection = "products",
                            entityId = p.id,
                            fixDescription = ShopLocale.format("hp_fix_dup_name", lang, uniqueName),
                            fixActionPayload = mapOf("name" to uniqueName)
                        )
                    )
                }
            }
        }

        // 4. Duplicate phone numbers across customers or suppliers
        val allContacts = mutableListOf<Triple<String, String, String>>() // (phone, type, id)
        for (c in customers) {
            val phone = c.mobile.trim()
            if (phone.isNotBlank()) allContacts.add(Triple(phone, "Customer", c.id))
        }
        for (s in suppliers) {
            val phone = s.mobile.trim()
            if (phone.isNotBlank()) allContacts.add(Triple(phone, "Supplier", s.id))
        }
        val phoneGroups = allContacts.groupBy { it.first }
        for ((phone, entries) in phoneGroups) {
            if (entries.size > 1) {
                val duplicate = entries[1]
                val first = entries[0]
                val firstType = if (first.second == "Customer") ShopLocale.get("party_customer", lang) else ShopLocale.get("party_supplier", lang)
                val dupType = if (duplicate.second == "Customer") ShopLocale.get("party_customer", lang) else ShopLocale.get("party_supplier", lang)
                problems.add(
                    HealthProblem(
                        id = "health_dup_phone_${duplicate.third}",
                        type = HealthProblemType.DUPLICATE_PHONE_NUMBER,
                        title = ShopLocale.format("hp_title_dup_phone", lang, phone),
                        description = ShopLocale.format("hp_desc_dup_phone", lang, phone, firstType, dupType),
                        recordRef = phone,
                        entityCollection = if (duplicate.second == "Customer") "customers" else "suppliers",
                        entityId = duplicate.third,
                        fixDescription = ShopLocale.format("hp_fix_dup_phone", lang, dupType),
                        fixActionPayload = mapOf("mobile" to "")
                    )
                )
            }
        }

        // 5. Orphan transactions (sales/purchases pointing to customer/supplier that no longer exists)
        for (sale in sales) {
            if (sale.paymentType == "baki" && sale.customerId.isNotBlank() && !customerMap.containsKey(sale.customerId)) {
                val custDisp = sale.customerName.ifBlank { ShopLocale.get("archived_customer_fallback", lang) }
                problems.add(
                    HealthProblem(
                        id = "health_orphan_sale_${sale.id}",
                        type = HealthProblemType.ORPHAN_TRANSACTION,
                        title = ShopLocale.format("hp_title_orphan_sale", lang, sale.invoiceNo),
                        description = ShopLocale.format("hp_desc_orphan_sale", lang, sale.customerId),
                        recordRef = sale.invoiceNo,
                        entityCollection = "sales",
                        entityId = sale.id,
                        fixDescription = ShopLocale.format("hp_fix_orphan_sale", lang, custDisp),
                        fixActionPayload = mapOf("customerId" to "", "customerName" to sale.customerName.ifBlank { "Archived Customer" })
                    )
                )
            }
        }
        for (purchase in purchases) {
            if (purchase.supplierId.isNotBlank() && !supplierMap.containsKey(purchase.supplierId)) {
                problems.add(
                    HealthProblem(
                        id = "health_orphan_purch_${purchase.id}",
                        type = HealthProblemType.ORPHAN_TRANSACTION,
                        title = ShopLocale.format("hp_title_orphan_pur", lang, purchase.purchaseNo),
                        description = ShopLocale.format("hp_desc_orphan_pur", lang, purchase.supplierId),
                        recordRef = purchase.purchaseNo,
                        entityCollection = "purchases",
                        entityId = purchase.id,
                        fixDescription = ShopLocale.format("hp_fix_orphan_pur", lang, purchase.supplierName),
                        fixActionPayload = mapOf("supplierId" to "")
                    )
                )
            }
        }

        // 6. Customer or supplier balances that do not match their transactions sum
        for (cust in customers) {
            val summary = calculateCustomerSummary(cust, sales, payments)
            if (kotlin.math.abs(cust.currentBalance - summary.currentBalance) > 0.05) {
                val profBalStr = ShopLocale.formatMoneyFixed2(cust.currentBalance, lang)
                val calcBalStr = ShopLocale.formatMoneyFixed2(summary.currentBalance, lang)
                problems.add(
                    HealthProblem(
                        id = "health_cust_bal_${cust.id}",
                        type = HealthProblemType.CUSTOMER_BALANCE_MISMATCH,
                        title = ShopLocale.format("hp_title_cust_bal", lang, cust.name),
                        description = ShopLocale.format("hp_desc_cust_bal", lang, profBalStr, calcBalStr),
                        recordRef = cust.name,
                        entityCollection = "customers",
                        entityId = cust.id,
                        fixDescription = ShopLocale.format("hp_fix_cust_bal", lang, calcBalStr),
                        fixActionPayload = mapOf("currentBalance" to summary.currentBalance)
                    )
                )
            }
        }
        for (sup in suppliers) {
            val supPurchases = purchases.filter { it.supplierId == sup.id }
            val calcDue = supPurchases.sumOf { it.dueAmount }
            if (kotlin.math.abs(sup.balance - calcDue) > 0.05) {
                val supBalStr = ShopLocale.formatMoneyFixed2(sup.balance, lang)
                val calcDueStr = ShopLocale.formatMoneyFixed2(calcDue, lang)
                problems.add(
                    HealthProblem(
                        id = "health_sup_bal_${sup.id}",
                        type = HealthProblemType.SUPPLIER_BALANCE_MISMATCH,
                        title = ShopLocale.format("hp_title_sup_bal", lang, sup.name),
                        description = ShopLocale.format("hp_desc_sup_bal", lang, supBalStr, calcDueStr),
                        recordRef = sup.name,
                        entityCollection = "suppliers",
                        entityId = sup.id,
                        fixDescription = ShopLocale.format("hp_fix_sup_bal", lang, calcDueStr),
                        fixActionPayload = mapOf("balance" to calcDue)
                    )
                )
            }
        }

        // 7. Products with no category
        for (prod in products) {
            val cat = prod.category.trim()
            if (cat.isBlank() || cat.equals("General", ignoreCase = true) || cat.equals("None", ignoreCase = true)) {
                problems.add(
                    HealthProblem(
                        id = "health_no_cat_${prod.id}",
                        type = HealthProblemType.PRODUCT_NO_CATEGORY,
                        title = ShopLocale.format("hp_title_no_cat", lang, prod.name),
                        description = ShopLocale.format("hp_desc_no_cat", lang, prod.name),
                        recordRef = prod.name,
                        entityCollection = "products",
                        entityId = prod.id,
                        fixDescription = ShopLocale.format("hp_fix_no_cat", lang, prod.name),
                        fixActionPayload = mapOf("category" to "General")
                    )
                )
            }
        }

        // 8. Negative account balance check (Step 12)
        for (acc in accounts) {
            val bal = accountBalances[acc.id] ?: acc.openingBalance
            if (bal < -0.01 && !acc.isArchived) {
                val accDisp = ShopLocale.displayAccountName(acc.name, acc.id, lang)
                val balStr = ShopLocale.formatMoneyFixed2(bal, lang)
                problems.add(
                    HealthProblem(
                        id = "health_acc_neg_${acc.id}",
                        type = HealthProblemType.NEGATIVE_ACCOUNT_BALANCE,
                        title = ShopLocale.format("hp_title_neg_acc", lang, accDisp),
                        description = ShopLocale.format("hp_desc_neg_acc", lang, accDisp, balStr),
                        recordRef = accDisp,
                        entityCollection = "accounts",
                        entityId = acc.id,
                        fixDescription = ShopLocale.format("hp_fix_neg_acc", lang, accDisp),
                        fixActionPayload = mapOf("newBalance" to 0.0)
                    )
                )
            }
        }

        // 9. Transactions pointing to invalid or archived accounts (Step 12)
        for (sale in sales) {
            if (sale.paymentType != "baki" && sale.accountId.isNotBlank() && sale.accountId != "cash_in_hand") {
                val acc = accountMap[sale.accountId]
                if (acc == null || acc.isArchived) {
                    problems.add(
                        HealthProblem(
                            id = "health_orphan_acc_sale_${sale.id}",
                            type = HealthProblemType.TRANSACTION_INVALID_ACCOUNT,
                            title = ShopLocale.format("hp_title_inv_acc_sale", lang, sale.invoiceNo),
                            description = ShopLocale.format("hp_desc_inv_acc_sale", lang, sale.invoiceNo, sale.accountId),
                            recordRef = sale.invoiceNo,
                            entityCollection = "sales",
                            entityId = sale.id,
                            fixDescription = ShopLocale.get("hp_fix_reassign_cash", lang),
                            fixActionPayload = mapOf("accountId" to "cash_in_hand", "accountName" to "Cash in Hand")
                        )
                    )
                }
            }
        }
        for (exp in expenses) {
            if (!exp.isDeleted && exp.accountId.isNotBlank() && exp.accountId != "cash_in_hand") {
                val acc = accountMap[exp.accountId]
                if (acc == null || acc.isArchived) {
                    val catDisp = ShopLocale.displayCategory(exp.category, lang)
                    val amtStr = ShopLocale.formatMoney(exp.amount, lang)
                    problems.add(
                        HealthProblem(
                            id = "health_orphan_acc_exp_${exp.id}",
                            type = HealthProblemType.TRANSACTION_INVALID_ACCOUNT,
                            title = ShopLocale.format("hp_title_inv_acc_exp", lang, catDisp),
                            description = ShopLocale.format("hp_desc_inv_acc_exp", lang, amtStr, exp.accountId),
                            recordRef = catDisp,
                            entityCollection = "expenses",
                            entityId = exp.id,
                            fixDescription = ShopLocale.get("hp_fix_reassign_cash", lang),
                            fixActionPayload = mapOf("accountId" to "cash_in_hand", "accountName" to "Cash in Hand")
                        )
                    )
                }
            }
        }

        for (pay in payments) {
            if (pay.accountId.isNotBlank() && pay.accountId != "cash_in_hand") {
                val acc = accountMap[pay.accountId]
                if (acc == null || acc.isArchived) {
                    problems.add(
                        HealthProblem(
                            id = "health_orphan_acc_pay_${pay.id}",
                            type = HealthProblemType.TRANSACTION_INVALID_ACCOUNT,
                            title = ShopLocale.format("hp_title_inv_acc_pay", lang, pay.paymentNo),
                            description = ShopLocale.format("hp_desc_inv_acc_pay", lang, pay.paymentNo, pay.accountId),
                            recordRef = pay.paymentNo,
                            entityCollection = "payments",
                            entityId = pay.id,
                            fixDescription = ShopLocale.get("hp_fix_reassign_cash", lang),
                            fixActionPayload = mapOf("accountId" to "cash_in_hand", "accountName" to "Cash in Hand")
                        )
                    )
                }
            }
        }
        for (pur in purchases) {
            if (pur.paidAmount > 0.0 && pur.accountId.isNotBlank() && pur.accountId != "cash_in_hand") {
                val acc = accountMap[pur.accountId]
                if (acc == null || acc.isArchived) {
                    problems.add(
                        HealthProblem(
                            id = "health_orphan_acc_pur_${pur.id}",
                            type = HealthProblemType.TRANSACTION_INVALID_ACCOUNT,
                            title = ShopLocale.format("hp_title_inv_acc_pur", lang, pur.purchaseNo),
                            description = ShopLocale.format("hp_desc_inv_acc_pur", lang, pur.purchaseNo, pur.accountId),
                            recordRef = pur.purchaseNo,
                            entityCollection = "purchases",
                            entityId = pur.id,
                            fixDescription = ShopLocale.get("hp_fix_reassign_cash", lang),
                            fixActionPayload = mapOf("accountId" to "cash_in_hand", "accountName" to "Cash in Hand")
                        )
                    )
                }
            }
        }
        for (sp in supplierPayments) {
            if (sp.accountId.isNotBlank() && sp.accountId != "cash_in_hand") {
                val acc = accountMap[sp.accountId]
                if (acc == null || acc.isArchived) {
                    problems.add(
                        HealthProblem(
                            id = "health_orphan_acc_sp_${sp.id}",
                            type = HealthProblemType.TRANSACTION_INVALID_ACCOUNT,
                            title = ShopLocale.format("hp_title_inv_acc_sp", lang, sp.paymentNo),
                            description = ShopLocale.format("hp_desc_inv_acc_sp", lang, sp.accountId),
                            recordRef = sp.paymentNo,
                            entityCollection = "supplierPayments",
                            entityId = sp.id,
                            fixDescription = ShopLocale.get("hp_fix_reassign_cash", lang),
                            fixActionPayload = mapOf("accountId" to "cash_in_hand", "accountName" to "Cash in Hand")
                        )
                    )
                }
            }
        }

        // 10. Account statement vs calculated balance mismatch
        for (acc in accounts) {
            val calcBal = accountBalances[acc.id] ?: acc.openingBalance
            val statement = calculateAccountStatement(
                account = acc,
                allSales = sales,
                allPaymentsIn = payments,
                allPurchases = purchases,
                allSupplierPayments = supplierPayments,
                allExpenses = expenses,
                allOtherIncomes = otherIncomes,
                allAdjustments = adjustments,
                period = HistoryPeriod.ALL,
                customRange = null
            )
            if (kotlin.math.abs(calcBal - statement.closingBalance) > 0.01) {
                val accDisp = ShopLocale.displayAccountName(acc.name, acc.id, lang)
                val calcStr = ShopLocale.formatMoneyFixed2(calcBal, lang)
                val closeStr = ShopLocale.formatMoneyFixed2(statement.closingBalance, lang)
                problems.add(
                    HealthProblem(
                        id = "health_acc_stmt_mismatch_${acc.id}",
                        type = HealthProblemType.ACCOUNT_STATEMENT_MISMATCH,
                        title = ShopLocale.format("hp_title_stmt_mismatch", lang, accDisp),
                        description = ShopLocale.format("hp_desc_stmt_mismatch", lang, calcStr, closeStr),
                        recordRef = accDisp,
                        entityCollection = "accounts",
                        entityId = acc.id,
                        fixDescription = ShopLocale.get("hp_fix_stmt_mismatch", lang)
                    )
                )
            }
        }

        return problems
    }

    fun calculateStaffReport(
        members: List<com.example.data.model.StoreMember>,
        ownerUid: String,
        sales: List<Sale>,
        paymentsIn: List<Payment>,
        period: HistoryPeriod,
        customRange: Pair<Long, Long>?,
        lang: AppLanguage = AppLanguage.EN
    ): StaffReportSummary {
        val (start, end) = getPeriodBounds(period, customRange)
        val periodSales = sales.filter { sale ->
            val ts = sale.createdAt?.toDate()?.time ?: 0L
            ts in start..end
        }
        val periodPayments = paymentsIn.filter { pay ->
            val ts = if (pay.paymentDate > 0L) pay.paymentDate else (pay.createdAt?.toDate()?.time ?: 0L)
            ts in start..end
        }

        val ownerLabel = ShopLocale.get("role_owner", lang)
        val allMembers = members.toMutableList()
        if (allMembers.none { it.userId == ownerUid }) {
            allMembers.add(
                0,
                com.example.data.model.StoreMember(
                    userId = ownerUid,
                    displayName = ownerLabel,
                    roleLabel = "Owner",
                    status = "Active"
                )
            )
        }

        val rows = allMembers.map { member ->
            val memberSales = periodSales.filter { sale ->
                if (sale.soldBy.isNotBlank()) {
                    sale.soldBy == member.userId
                } else {
                    member.userId == ownerUid
                }
            }
            val memberPayments = periodPayments.filter { pay ->
                if (pay.receivedBy.isNotBlank()) {
                    pay.receivedBy == member.userId
                } else {
                    member.userId == ownerUid
                }
            }

            StaffPerformanceRow(
                memberId = member.userId,
                memberName = member.displayName.ifBlank {
                    if (member.userId == ownerUid) ownerLabel
                    else ShopLocale.format("staff_fallback_id", lang, member.userId.take(5))
                },
                roleLabel = member.roleLabel,
                salesCount = memberSales.size.toLong(),
                totalSales = memberSales.sumOf { it.grandTotal },
                itemsSold = memberSales.sumOf { it.totalItems },
                profit = memberSales.sumOf { it.profit },
                paymentsReceived = memberPayments.sumOf { it.amount }
            )
        }

        return StaffReportSummary(
            rows = rows,
            totalSalesCount = rows.sumOf { it.salesCount },
            totalSalesAmount = rows.sumOf { it.totalSales },
            totalItemsSold = rows.sumOf { it.itemsSold },
            totalProfit = rows.sumOf { it.profit },
            totalPaymentsReceived = rows.sumOf { it.paymentsReceived }
        )
    }

    /**
     * Computes real Home dashboard insights:
     * - yesterdaySales: real sales recorded yesterday
     * - salesChangePercent: computed ONLY when yesterdaySales > 0.0, else null (never fabricated)
     * - todaySparklinePoints: normalized curve built strictly from today's recorded sales timestamps
     */
    fun calculateHomeInsights(sales: List<Sale>): HomeInsights {
        val todayList = filterSalesByPeriod(sales, HistoryPeriod.TODAY, null)
        val todayTotal = todayList.sumOf { it.grandTotal }

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val yStart = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val yEnd = cal.timeInMillis

        val yesterdayList = filterSalesByDateRange(sales, yStart, yEnd)
        val yesterdayTotal = yesterdayList.sumOf { it.grandTotal }

        val pctChange = if (yesterdayTotal > 0.0) {
            ((todayTotal - yesterdayTotal) / yesterdayTotal) * 100.0
        } else {
            null
        }

        // Build real sparkline points across 8 three-hour buckets from today's actual sales
        val sparkline = if (todayList.isEmpty()) {
            emptyList()
        } else {
            val buckets = DoubleArray(8)
            val calBucket = Calendar.getInstance()
            for (sale in todayList) {
                val ts = sale.createdAt?.toDate()?.time ?: continue
                calBucket.timeInMillis = ts
                val hour = calBucket.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                val idx = (hour / 3).coerceIn(0, 7)
                buckets[idx] += sale.grandTotal.coerceAtLeast(0.0)
            }
            val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            val activeMaxBucket = (nowHour / 3).coerceIn(0, 7)
            val cumulative = DoubleArray(activeMaxBucket + 1)
            var running = 0.0
            for (i in 0..activeMaxBucket) {
                running += buckets[i]
                cumulative[i] = running
            }
            val maxVal = cumulative.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            val pts = mutableListOf(0f)
            for (v in cumulative) {
                pts.add((v / maxVal).toFloat().coerceIn(0f, 1f))
            }
            pts
        }

        return HomeInsights(
            yesterdaySales = yesterdayTotal,
            salesChangePercent = pctChange,
            todaySparklinePoints = sparkline
        )
    }

    /**
     * Computes real 7-day cashflow (Cash In vs Cash Out) across the last 7 calendar days ending today.
     * Cash In: Paid-in-full sales, customer payments received, non-deleted other incomes.
     * Cash Out: Purchase paid amounts, supplier payments out, non-deleted expenses.
     */
    fun calculateSevenDayCashflow(
        sales: List<Sale>,
        paymentsIn: List<Payment>,
        otherIncomes: List<OtherIncome>,
        purchases: List<Purchase>,
        supplierPayments: List<SupplierPayment>,
        expenses: List<Expense>,
        lang: AppLanguage = AppLanguage.EN
    ): SevenDayCashflowSummary {
        val points = mutableListOf<DailyCashflowPoint>()

        val cal = Calendar.getInstance()
        for (offset in -6..0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val dayEnd = cal.timeInMillis

            var cashIn = 0.0
            for (sale in sales) {
                val paid = salePaidAmount(sale)
                if (paid > 0.001) {
                    val ts = sale.createdAt?.toDate()?.time ?: 0L
                    if (ts in dayStart..dayEnd) cashIn += paid
                }
            }
            for (pay in paymentsIn) {
                val ts = if (pay.paymentDate > 0L) pay.paymentDate else (pay.createdAt?.toDate()?.time ?: 0L)
                if (ts in dayStart..dayEnd) cashIn += pay.amount
            }
            for (inc in otherIncomes) {
                if (!inc.isDeleted) {
                    val ts = if (inc.date > 0L) inc.date else (inc.createdAt?.toDate()?.time ?: 0L)
                    if (ts in dayStart..dayEnd) cashIn += inc.amount
                }
            }

            var cashOut = 0.0
            for (pur in purchases) {
                if (pur.paidAmount > 0.0) {
                    val ts = pur.createdAt?.toDate()?.time ?: 0L
                    if (ts in dayStart..dayEnd) cashOut += pur.paidAmount
                }
            }
            for (sp in supplierPayments) {
                val ts = if (sp.paymentDate > 0L) sp.paymentDate else (sp.createdAt?.toDate()?.time ?: 0L)
                if (ts in dayStart..dayEnd) cashOut += sp.amount
            }
            for (exp in expenses) {
                if (!exp.isDeleted) {
                    val ts = if (exp.date > 0L) exp.date else (exp.createdAt?.toDate()?.time ?: 0L)
                    if (ts in dayStart..dayEnd) cashOut += exp.amount
                }
            }

            points.add(
                DailyCashflowPoint(
                    dayLabel = ShopLocale.formatWeekdaySingle(dayStart, lang),
                    timestamp = dayStart,
                    cashIn = cashIn,
                    cashOut = cashOut
                )
            )
        }

        return SevenDayCashflowSummary(
            points = points,
            totalIn = points.sumOf { it.cashIn },
            totalOut = points.sumOf { it.cashOut }
        )
    }

    fun extractSequenceNumber(referenceNo: String, expectedPrefix: String): Long {
        val trimmed = referenceNo.trim()
        if (!trimmed.startsWith(expectedPrefix, ignoreCase = true)) return 0L
        val suffix = trimmed.substring(expectedPrefix.length).trim()
        return suffix.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    }

    /**
     * Recomputes line totals, subtotal, bounded discounts, grand total, profit, and totalItems
     * for an imported Sale using CalculationEngine rules while keeping its invoiceNo unchanged.
     */
    fun recomputeSaleForImport(
        sale: Sale,
        productsById: Map<String, Product> = emptyMap()
    ): Sale {
        val recomputedItems = sale.items.map { rawItem ->
            val safeQty = roundStock(rawItem.qty.coerceAtLeast(0.0))
            val matchedProduct = productsById[rawItem.productId]
            val factor = if (matchedProduct != null) {
                sellFactorForUnit(matchedProduct, rawItem.unit)
            } else {
                1.0
            }
            val safeBuyPrice = roundMoney(rawItem.buyPrice.coerceAtLeast(0.0))
            val safeSellPrice = roundMoney(rawItem.sellPrice.coerceAtLeast(0.0))
            val computedStockQty = when {
                rawItem.stockQty > 0.0 -> roundStock(rawItem.stockQty)
                matchedProduct != null -> sellQtyToStockQty(safeQty, factor)
                else -> safeQty
            }
            val boundDiscount = calculateBoundedDiscount(safeSellPrice, safeQty, rawItem.discount)
            val lineTotal = calculateLineNetTotal(safeSellPrice, safeQty, boundDiscount)
            val lineProfit = calculateLineProfit(safeSellPrice, safeBuyPrice, safeQty, boundDiscount)
            rawItem.copy(
                qty = safeQty,
                unit = rawItem.unit.trim().ifBlank { matchedProduct?.unit ?: "pcs" },
                stockQty = computedStockQty,
                buyPrice = safeBuyPrice,
                sellPrice = safeSellPrice,
                discount = boundDiscount,
                lineTotal = lineTotal,
                profit = lineProfit
            )
        }

        val totalItems = roundStock(recomputedItems.sumOf { saleItemStockQty(it) })
        val subtotal = roundMoney(recomputedItems.sumOf { calculateLineGrossTotal(it.sellPrice, it.qty) })
        val itemDiscounts = roundMoney(recomputedItems.sumOf { it.discount })
        val invoiceDiscount = (sale.discount - itemDiscounts)
            .coerceIn(0.0, (subtotal - itemDiscounts).coerceAtLeast(0.0))
        val totalDiscount = roundMoney((itemDiscounts + invoiceDiscount).coerceIn(0.0, subtotal))
        val grandTotal = roundMoney((subtotal - totalDiscount).coerceAtLeast(0.0))
        val itemProfitSum = recomputedItems.sumOf { it.profit }
        val totalProfit = roundMoney(itemProfitSum - invoiceDiscount)

        return sale.copy(
            items = recomputedItems,
            totalItems = totalItems,
            subtotal = subtotal,
            discount = totalDiscount,
            grandTotal = grandTotal,
            profit = totalProfit
        )
    }

    /**
     * Recomputes line totals, totalItems, totalAmount, bounded paidAmount, dueAmount,
     * and paymentType for an imported Purchase while keeping its purchaseNo unchanged.
     */
    fun recomputePurchaseForImport(purchase: Purchase): Purchase {
        val recomputedItems = purchase.items.map { rawItem ->
            val safeQty = roundStock(rawItem.qty.coerceAtLeast(0.0))
            val safeBuyPrice = roundMoney(rawItem.buyPrice.coerceAtLeast(0.0))
            val safeConv = if (rawItem.conversionFactor > 0.0) roundStock(rawItem.conversionFactor) else 1.0
            val stockQty = if (rawItem.stockQty > 0.0) {
                roundStock(rawItem.stockQty)
            } else {
                buyQtyToStockQty(safeQty, safeConv)
            }
            val unitCost = costPerStockUnit(safeBuyPrice, safeConv)
            val lineTotal = roundMoney(safeQty * safeBuyPrice)
            rawItem.copy(
                qty = safeQty,
                unit = rawItem.unit.trim().ifBlank { "pcs" },
                stockQty = stockQty,
                conversionFactor = safeConv,
                costPerStockUnit = unitCost,
                buyPrice = safeBuyPrice,
                lineTotal = lineTotal
            )
        }

        val totalItems = roundStock(recomputedItems.sumOf { purchaseItemStockQty(it) })
        val totalAmount = roundMoney(recomputedItems.sumOf { it.lineTotal })
        val boundedPaid = roundMoney(purchase.paidAmount.coerceIn(0.0, totalAmount))
        val dueAmount = roundMoney((totalAmount - boundedPaid).coerceAtLeast(0.0))
        val paymentType = if (dueAmount > 0.001) "credit" else "paid"

        return purchase.copy(
            items = recomputedItems,
            totalItems = totalItems,
            totalAmount = totalAmount,
            paidAmount = boundedPaid,
            dueAmount = dueAmount,
            paymentType = paymentType
        )
    }

    private class ProductSoldAccumulator(
        val productId: String,
        val productName: String,
        val unit: String,
        var qty: Double = 0.0,
        var salesAmount: Double = 0.0,
        var profit: Double = 0.0
    )
}
