package com.example.util

import com.example.data.model.AccountStatementReport
import com.example.data.model.Expense
import com.example.data.model.OtherIncome
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.Store
import com.example.domain.CustomerStatementReport
import com.example.domain.DayBookEntry
import com.example.domain.LedgerEntryType
import com.example.domain.MonthlyReportData
import com.example.domain.ProfitAndLossStatement
import com.example.domain.StaffReportSummary
import com.example.domain.SupplierStatementReport

object ReportDataBuilders {

    private fun nowFormatted(lang: AppLanguage): String =
        ShopLocale.formatDateLong(System.currentTimeMillis(), lang)

    fun formatMonthlyCheckLine(
        check: com.example.domain.MonthlyReconciliationCheck,
        lang: AppLanguage,
        plainBangla: Boolean = false
    ): String {
        val raw = if (!check.isAvailable) {
            val detail = if (check.key == "stock" || check.note.contains("opening", ignoreCase = true)) {
                ShopLocale.get("mr_stock_opening_not_tracked", lang)
            } else {
                check.note.trim()
            }
            if (detail.isBlank()) {
                if (lang == AppLanguage.BN) "হিসাব মিল: পাওয়া যাচ্ছে না" else "Numbers check: Not available"
            } else {
                ShopLocale.format("mr_check_unavailable", lang, detail)
            }
        } else if (check.matches) {
            val eq = "${ShopLocale.formatMoney(check.opening, lang)} + ${ShopLocale.formatMoney(check.totalIn, lang)} - ${ShopLocale.formatMoney(check.totalOut, lang)} = ${ShopLocale.formatMoney(check.actualClosing, lang)}"
            ShopLocale.format("mr_check_ok", lang, eq)
        } else {
            ShopLocale.format(
                "mr_check_mismatch",
                lang,
                ShopLocale.formatSignedMoney(check.difference, lang),
                ShopLocale.formatMoney(check.expectedClosing, lang),
                ShopLocale.formatMoney(check.actualClosing, lang)
            )
        }
        return if (plainBangla) ShopLocale.plainBanglaText(raw, raw) else raw
    }

    fun buildMonthlyReportTable(
        reportData: MonthlyReportData,
        year: Int,
        month: Int,
        store: Store?,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN,
        includeSupplierDebt: Boolean = true,
        isRestrictedMode: Boolean = !includeSupplierDebt
    ): ReportTableData {
        val effectiveLang = if (isRestrictedMode) AppLanguage.BN else lang
        val isBn = effectiveLang == AppLanguage.BN
        fun txt(s: String, fallback: String = "তথ্য"): String =
            if (isRestrictedMode) ShopLocale.plainBanglaText(s, fallback) else s
        fun money(v: Double): String = txt(ShopLocale.formatMoney(v, effectiveLang))
        fun num(v: Long): String = txt(ShopLocale.formatNumber(v, effectiveLang))
        fun numD(v: Double): String = txt(ShopLocale.formatNumber(v, effectiveLang))
        fun pct(v: Double): String = txt(ShopLocale.formatPercent(v, 1, effectiveLang))

        val currentMonthLabel = txt(ShopLocale.formatMonthYear(year, month, effectiveLang))
        val emptyMonthFriendly = txt(ShopLocale.format("mr_empty_month_desc", effectiveLang, currentMonthLabel))
        val noSectionRowsLabel = if (reportData.isMonthEmpty) {
            emptyMonthFriendly
        } else {
            txt(if (isBn) "এই বিভাগে কোনো তথ্য নেই" else "No records in this section for $currentMonthLabel")
        }
        val activeSummaryCheck = if (canSeeProfit) reportData.summaryCheck else reportData.summaryCheckNoProfit

        val title = if (isRestrictedMode) {
            "মাসিক হিসাব - $currentMonthLabel"
        } else {
            ShopLocale.format("rep_monthly_title", effectiveLang, currentMonthLabel)
        }
        val storeNameStr = txt(
            store?.name?.ifBlank { ShopLocale.get("receipt_header_store_default", effectiveLang) }
                ?: ShopLocale.get("receipt_header_store_default", effectiveLang),
            "দোকান"
        )

        // Empty month: show a short friendly message, never empty cells
        if (reportData.isMonthEmpty) {
            val emptySection = ReportSectionTable(
                title = txt(ShopLocale.get("mr_empty_month_title", effectiveLang)),
                headers = emptyList(),
                rows = emptyList(),
                emptyMessage = emptyMonthFriendly
            )
            return ReportTableData(
                title = title,
                storeName = storeNameStr,
                storePhone = if (isRestrictedMode) ShopLocale.digits(store?.phone ?: "", effectiveLang) else ShopLocale.digits(store?.phone ?: "", effectiveLang),
                storeAddress = if (isRestrictedMode) txt(store?.address ?: "", "") else (store?.address ?: ""),
                periodLabel = currentMonthLabel,
                generatedAt = txt(nowFormatted(effectiveLang)),
                headers = if (isBn) listOf("মাস", "বার্তা") else listOf("Month", "Message"),
                rows = listOf(listOf(currentMonthLabel, emptyMonthFriendly)),
                numericColumns = emptySet(),
                rawNumbers = listOf(listOf(null, null)),
                totals = null,
                rawTotals = emptyList(),
                notes = null,
                summaryMetrics = emptyList(),
                sections = listOf(emptySection),
                preferLandscape = false
            )
        }

        val summaryMetrics = buildList {
            add(
                ReportSummaryMetric(
                    label = txt(if (isBn) "নিট বিক্রয়" else "Net Sales"),
                    value = money(reportData.netSales),
                    detail = txt("${ShopLocale.format("total_orders_row", effectiveLang, reportData.totalOrders)} · ${ShopLocale.get("mr_items_sold", effectiveLang)}: ${numD(reportData.totalItemsSold)}")
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = txt(if (isBn) "মোট লাভ" else "Gross Profit"),
                        value = money(reportData.grossProfit)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = txt(ShopLocale.get("mr_col_orders", effectiveLang)),
                    value = num(reportData.totalOrders)
                )
            )
            add(
                ReportSummaryMetric(
                    label = txt(ShopLocale.get("mr_items_sold", effectiveLang)),
                    value = numD(reportData.totalItemsSold)
                )
            )
            add(
                ReportSummaryMetric(
                    label = txt(if (isBn) "মোট খরচ" else "Total Expenses"),
                    value = money(reportData.totalExpenses)
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = txt(if (isBn) "নিট লাভ" else "Net Profit"),
                        value = money(reportData.netProfit)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = txt(ShopLocale.get("mr_cash_and_bank_total", effectiveLang)),
                    value = money(reportData.cashClosingBalance)
                )
            )
            if (!isRestrictedMode) {
                add(
                    ReportSummaryMetric(
                        label = txt(ShopLocale.get("mr_other_income", effectiveLang)),
                        value = money(reportData.otherIncome)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = txt(if (isBn) "ক্রেতার কাছে পাব" else "Baki to Receive"),
                    value = money(reportData.bakiClosingReceivable)
                )
            )
            if (!isRestrictedMode) {
                add(
                    ReportSummaryMetric(
                        label = txt(if (isBn) "মহাজনকে দেব" else "Supplier Payable"),
                        value = money(reportData.supplierClosingPayable)
                    )
                )
            }
        }

        val sections = mutableListOf<ReportSectionTable>()

        // -----------------------------------------------------------
        // RESTRICTED MODES (Simple / Father): ONLY Summary, Baki, Low Stock (plain Bangla)
        // -----------------------------------------------------------
        if (isRestrictedMode) {
            val summaryRows = buildList {
                add(listOf("মোট বিক্রি", num(reportData.totalOrders) + " টি অর্ডার", money(reportData.netSales)))
                add(listOf("অর্ডার", "মোট", num(reportData.totalOrders)))
                add(listOf("বিক্রিত পণ্য সংখ্যা", "মোট", numD(reportData.totalItemsSold)))
                if (canSeeProfit) {
                    add(listOf("মোট লাভ", "মোট", money(reportData.grossProfit)))
                    add(listOf("নিট লাভ", "মোট", money(reportData.netProfit)))
                }
                add(listOf("মোট খরচ", "মোট", money(reportData.totalExpenses)))
                add(listOf("মোট ক্যাশ ও ব্যাংক", "মোট", money(reportData.cashClosingBalance)))
                add(listOf("ক্রেতার কাছে পাব", "মোট", money(reportData.bakiClosingReceivable)))
            }
            val summaryRaw = buildList {
                add(listOf(null, null, reportData.netSales))
                add(listOf(null, null, reportData.totalOrders.toDouble()))
                add(listOf(null, null, reportData.totalItemsSold))
                if (canSeeProfit) {
                    add(listOf(null, null, reportData.grossProfit))
                    add(listOf(null, null, reportData.netProfit))
                }
                add(listOf(null, null, reportData.totalExpenses))
                add(listOf(null, null, reportData.cashClosingBalance))
                add(listOf(null, null, reportData.bakiClosingReceivable))
            }
            sections.add(
                ReportSectionTable(
                    title = "১. সারসংক্ষেপ",
                    headers = listOf("হিসাবের খাত", "বিবরণ", "পরিমাণ"),
                    rows = summaryRows,
                    numericColumns = setOf(2),
                    rawNumbers = summaryRaw,
                    checkLine = formatMonthlyCheckLine(activeSummaryCheck, effectiveLang, plainBangla = true),
                    checkMatches = activeSummaryCheck.matches,
                    checkAvailable = activeSummaryCheck.isAvailable,
                    emptyMessage = noSectionRowsLabel
                )
            )

            // Baki (Customers Owe) grouped 0-30, 31-60, 61-90, 90+
            val bakiMetrics = listOf(
                ReportSummaryMetric("শুরুতে বাকি", money(reportData.bakiOpeningReceivable)),
                ReportSummaryMetric("নতুন বাকি", money(reportData.bakiNewCreditGiven)),
                ReportSummaryMetric("এই মাসে আদায়", money(reportData.bakiCollectedThisMonth)),
                ReportSummaryMetric("শেষে বাকি আছে", money(reportData.bakiClosingReceivable)),
                ReportSummaryMetric("০-৩০ দিন", money(reportData.bakiAgeing.bucket0To30)),
                ReportSummaryMetric("৩১-৬০ দিন", money(reportData.bakiAgeing.bucket31To60)),
                ReportSummaryMetric("৬১-৯০ দিন", money(reportData.bakiAgeing.bucket61To90)),
                ReportSummaryMetric("৯০+ দিন", money(reportData.bakiAgeing.bucket90Plus))
            )
            val bakiRows = reportData.debtorRowsGrouped.map { d ->
                val bucketLabel = when (d.ageingBucket) {
                    "0-30" -> "০-৩০ দিন"
                    "31-60" -> "৩১-৬০ দিন"
                    "61-90" -> "৬১-৯০ দিন"
                    else -> "৯০+ দিন"
                }
                listOf(txt(d.customerName, "ক্রেতা"), bucketLabel, num(d.daysOverdue) + " দিন", money(d.amountOwed))
            }
            val bakiRaw = reportData.debtorRowsGrouped.map { d -> listOf(null, null, d.daysOverdue.toDouble(), d.amountOwed) }
            sections.add(
                ReportSectionTable(
                    title = "২. কার কাছে পাব",
                    summaryMetrics = bakiMetrics,
                    headers = listOf("ক্রেতা", "মেয়াদ", "বকেয়ার দিন", "বাকি টাকা"),
                    rows = bakiRows,
                    numericColumns = setOf(2, 3),
                    rawNumbers = bakiRaw,
                    totals = if (bakiRows.isNotEmpty()) listOf("মোট (${num(reportData.debtorCount)} জন)", "—", "—", money(reportData.bakiClosingReceivable)) else null,
                    rawTotals = if (bakiRows.isNotEmpty()) listOf(null, null, null, reportData.bakiClosingReceivable) else emptyList(),
                    checkLine = formatMonthlyCheckLine(reportData.bakiCheck, effectiveLang, plainBangla = true),
                    checkMatches = reportData.bakiCheck.matches,
                    checkAvailable = reportData.bakiCheck.isAvailable,
                    emptyMessage = "মাস শেষে কোনো ক্রেতার বাকি নেই"
                )
            )

            // Low Stock
            val lowRows = reportData.lowStockItems.map { item ->
                val unit = txt(ShopLocale.displayUnit(item.unit, AppLanguage.BN))
                val base = listOf(txt(item.productName, "পণ্য"), "${numD(item.stock)} $unit", "${numD(item.lowStockLimit)} $unit")
                if (canSeeProfit) base + money(item.stockValueAtCost) else base
            }
            val lowRaw = reportData.lowStockItems.map { item ->
                if (canSeeProfit) listOf(null, item.stock, item.lowStockLimit, item.stockValueAtCost)
                else listOf(null, item.stock, item.lowStockLimit)
            }
            sections.add(
                ReportSectionTable(
                    title = "৩. কী মাল কমে গেছে",
                    summaryMetrics = buildList {
                        add(ReportSummaryMetric("কম স্টক পণ্য", num(reportData.lowStockCount) + " টি"))
                        add(ReportSummaryMetric("মোট পণ্যের ধরন", num(reportData.stockItemCount) + " টি"))
                        if (canSeeProfit) {
                            add(ReportSummaryMetric("বর্তমান স্টক মূল্য", money(reportData.closingStockValue)))
                        }
                    },
                    headers = if (canSeeProfit) listOf("পণ্য", "মজুদ", "সীমা", "মূল্য") else listOf("পণ্য", "মজুদ", "সীমা"),
                    rows = lowRows,
                    numericColumns = if (canSeeProfit) setOf(3) else emptySet(),
                    rawNumbers = lowRaw,
                    checkLine = formatMonthlyCheckLine(reportData.stockCheck, effectiveLang, plainBangla = true),
                    checkMatches = reportData.stockCheck.matches,
                    checkAvailable = reportData.stockCheck.isAvailable,
                    emptyMessage = "সব মাল পর্যাপ্ত আছে"
                )
            )
        } else {
            // -----------------------------------------------------------
            // DETAILED MODE: ALL 8 SECTIONS + FULL TABLES + CHECKS
            // -----------------------------------------------------------
            // 1. Summary
            val summaryRows = buildList {
                add(listOf(if (isBn) "নিট বিক্রয়" else "Net Sales", ShopLocale.format("total_orders_row", effectiveLang, reportData.totalOrders), money(reportData.netSales)))
                add(listOf(ShopLocale.get("mr_col_orders", effectiveLang), "—", num(reportData.totalOrders)))
                add(listOf(ShopLocale.get("mr_items_sold", effectiveLang), "—", numD(reportData.totalItemsSold)))
                if (canSeeProfit) {
                    add(listOf(if (isBn) "বিক্রিত পণ্যের ক্রয়মূল্য" else "Cost of Goods Sold", "—", money(reportData.costOfGoodsSold)))
                    add(listOf(if (isBn) "মোট লাভ" else "Gross Profit", "—", money(reportData.grossProfit)))
                }
                add(listOf(ShopLocale.get("mr_other_income", effectiveLang), "—", money(reportData.otherIncome)))
                add(listOf(if (isBn) "মোট খরচ" else "Total Expenses", "—", money(reportData.totalExpenses)))
                if (canSeeProfit) {
                    add(listOf(if (isBn) "নিট লাভ" else "Net Profit", "—", money(reportData.netProfit)))
                }
                add(listOf(ShopLocale.get("mr_cash_and_bank_total", effectiveLang), "—", money(reportData.cashClosingBalance)))
                add(listOf(if (isBn) "ক্রেতার কাছে পাব" else "Total Baki to Receive", "—", money(reportData.bakiClosingReceivable)))
                add(listOf(if (isBn) "মহাজনকে দেব" else "Total Payable to Suppliers", "—", money(reportData.supplierClosingPayable)))
            }
            val summaryRaw = buildList {
                add(listOf(null, null, reportData.netSales))
                add(listOf(null, null, reportData.totalOrders.toDouble()))
                add(listOf(null, null, reportData.totalItemsSold))
                if (canSeeProfit) {
                    add(listOf(null, null, reportData.costOfGoodsSold))
                    add(listOf(null, null, reportData.grossProfit))
                }
                add(listOf(null, null, reportData.otherIncome))
                add(listOf(null, null, reportData.totalExpenses))
                if (canSeeProfit) {
                    add(listOf(null, null, reportData.netProfit))
                }
                add(listOf(null, null, reportData.cashClosingBalance))
                add(listOf(null, null, reportData.bakiClosingReceivable))
                add(listOf(null, null, reportData.supplierClosingPayable))
            }
            sections.add(
                ReportSectionTable(
                    title = ShopLocale.get("mr_sec_summary", effectiveLang),
                    headers = listOf(if (isBn) "বিবরণ" else "Metric", if (isBn) "তথ্য" else "Detail", if (isBn) "পরিমাণ" else "Amount"),
                    rows = summaryRows,
                    numericColumns = setOf(2),
                    rawNumbers = summaryRaw,
                    checkLine = formatMonthlyCheckLine(activeSummaryCheck, effectiveLang),
                    checkMatches = activeSummaryCheck.matches,
                    checkAvailable = activeSummaryCheck.isAvailable,
                    emptyMessage = noSectionRowsLabel
                )
            )

            // 2. Sales (Paid vs Baki, By Day, Top 5 Products, By Category, Top 5 Customers)
            val salesMetrics = listOf(
                ReportSummaryMetric(if (isBn) "মোট বিক্রয় (ছাড়ের আগে)" else "Gross Sales", money(reportData.grossSales)),
                ReportSummaryMetric(if (isBn) "ছাড়" else "Discounts", money(reportData.totalDiscounts)),
                ReportSummaryMetric(if (isBn) "নগদ প্রাপ্ত বিক্রয়" else "Paid Sales", money(reportData.cashReceivedSales)),
                ReportSummaryMetric(if (isBn) "বাকিতে বিক্রয়" else "Baki Sales", money(reportData.bakiCreditSales))
            )
            val dailyRows = reportData.dailySalesRows.map { d ->
                val base = listOf(
                    ShopLocale.digits(d.dateLabel, effectiveLang),
                    num(d.orders),
                    numD(d.itemsSold),
                    money(d.salesAmount)
                )
                if (canSeeProfit) base + money(d.profitAmount) else base
            }
            val dailyRaw = reportData.dailySalesRows.map { d ->
                if (canSeeProfit) listOf(null, d.orders.toDouble(), d.itemsSold, d.salesAmount, d.profitAmount)
                else listOf(null, d.orders.toDouble(), d.itemsSold, d.salesAmount)
            }
            sections.add(
                ReportSectionTable(
                    title = "${ShopLocale.get("mr_sec_sales", effectiveLang)} — ${ShopLocale.get("mr_sales_by_day", effectiveLang)}",
                    summaryMetrics = salesMetrics,
                    headers = if (canSeeProfit) {
                        listOf(
                            ShopLocale.get("mr_col_day", effectiveLang),
                            ShopLocale.get("mr_col_orders", effectiveLang),
                            ShopLocale.get("mr_col_items", effectiveLang),
                            ShopLocale.get("mr_col_sales", effectiveLang),
                            ShopLocale.get("col_profit", effectiveLang)
                        )
                    } else {
                        listOf(
                            ShopLocale.get("mr_col_day", effectiveLang),
                            ShopLocale.get("mr_col_orders", effectiveLang),
                            ShopLocale.get("mr_col_items", effectiveLang),
                            ShopLocale.get("mr_col_sales", effectiveLang)
                        )
                    },
                    rows = dailyRows,
                    numericColumns = if (canSeeProfit) setOf(1, 2, 3, 4) else setOf(1, 2, 3),
                    rawNumbers = dailyRaw,
                    totals = if (dailyRows.isNotEmpty()) {
                        val baseTot = listOf(
                            if (isBn) "মোট" else "Total",
                            num(reportData.totalOrders),
                            numD(reportData.totalItemsSold),
                            money(reportData.netSales)
                        )
                        if (canSeeProfit) baseTot + money(reportData.totalProfit) else baseTot
                    } else null,
                    rawTotals = if (dailyRows.isNotEmpty()) {
                        if (canSeeProfit) {
                            listOf(null, reportData.totalOrders.toDouble(), reportData.totalItemsSold, reportData.netSales, reportData.totalProfit)
                        } else {
                            listOf(null, reportData.totalOrders.toDouble(), reportData.totalItemsSold, reportData.netSales)
                        }
                    } else emptyList(),
                    checkLine = formatMonthlyCheckLine(reportData.salesCheck, effectiveLang),
                    checkMatches = reportData.salesCheck.matches,
                    checkAvailable = reportData.salesCheck.isAvailable,
                    emptyMessage = noSectionRowsLabel
                )
            )
            if (reportData.topProductsByAmount.isNotEmpty()) {
                val topProdRows = reportData.topProductsByAmount.mapIndexed { idx, p ->
                    val qty = "${numD(p.quantitySold)} ${ShopLocale.displayUnit(p.unit, effectiveLang)}"
                    val base = listOf(num(idx + 1L), p.productName, qty, money(p.totalSalesAmount))
                    if (canSeeProfit) base + money(p.totalProfit) else base
                }
                val topProdRaw = reportData.topProductsByAmount.map { p ->
                    if (canSeeProfit) listOf(null, null, p.quantitySold, p.totalSalesAmount, p.totalProfit)
                    else listOf(null, null, p.quantitySold, p.totalSalesAmount)
                }
                sections.add(
                    ReportSectionTable(
                        title = "${ShopLocale.get("mr_sec_sales", effectiveLang)} — ${ShopLocale.get("mr_top_products", effectiveLang)}",
                        headers = if (canSeeProfit) {
                            listOf("#", ShopLocale.get("col_product", effectiveLang), ShopLocale.get("col_qty_sold", effectiveLang), ShopLocale.get("mr_col_sales", effectiveLang), ShopLocale.get("col_profit", effectiveLang))
                        } else {
                            listOf("#", ShopLocale.get("col_product", effectiveLang), ShopLocale.get("col_qty_sold", effectiveLang), ShopLocale.get("mr_col_sales", effectiveLang))
                        },
                        rows = topProdRows,
                        numericColumns = if (canSeeProfit) setOf(3, 4) else setOf(3),
                        rawNumbers = topProdRaw
                    )
                )
            }
            if (reportData.salesByCategoryRows.isNotEmpty()) {
                val catRows = reportData.salesByCategoryRows.map { c ->
                    val base = listOf(
                        ShopLocale.displayCategory(c.category, effectiveLang),
                        numD(c.itemsCount),
                        money(c.salesAmount),
                        pct(c.percentage)
                    )
                    if (canSeeProfit) base + money(c.profitAmount) else base
                }
                val catRaw = reportData.salesByCategoryRows.map { c ->
                    if (canSeeProfit) listOf(null, c.itemsCount, c.salesAmount, c.percentage, c.profitAmount)
                    else listOf(null, c.itemsCount, c.salesAmount, c.percentage)
                }
                sections.add(
                    ReportSectionTable(
                        title = "${ShopLocale.get("mr_sec_sales", effectiveLang)} — ${ShopLocale.get("mr_sales_by_category", effectiveLang)}",
                        headers = if (canSeeProfit) {
                            listOf(
                                ShopLocale.get("col_category", effectiveLang),
                                ShopLocale.get("mr_col_items", effectiveLang),
                                ShopLocale.get("mr_col_sales", effectiveLang),
                                ShopLocale.get("mr_col_share", effectiveLang),
                                ShopLocale.get("col_profit", effectiveLang)
                            )
                        } else {
                            listOf(
                                ShopLocale.get("col_category", effectiveLang),
                                ShopLocale.get("mr_col_items", effectiveLang),
                                ShopLocale.get("mr_col_sales", effectiveLang),
                                ShopLocale.get("mr_col_share", effectiveLang)
                            )
                        },
                        rows = catRows,
                        numericColumns = if (canSeeProfit) setOf(1, 2, 3, 4) else setOf(1, 2, 3),
                        rawNumbers = catRaw,
                        totals = run {
                            val baseTot = listOf(
                                if (isBn) "মোট" else "Total",
                                numD(reportData.salesByCategoryTotalItems),
                                money(reportData.salesByCategoryTotalSales),
                                pct(100.0)
                            )
                            if (canSeeProfit) baseTot + money(reportData.salesByCategoryTotalProfit) else baseTot
                        },
                        rawTotals = if (canSeeProfit) {
                            listOf(
                                null,
                                reportData.salesByCategoryTotalItems,
                                reportData.salesByCategoryTotalSales,
                                100.0,
                                reportData.salesByCategoryTotalProfit
                            )
                        } else {
                            listOf(
                                null,
                                reportData.salesByCategoryTotalItems,
                                reportData.salesByCategoryTotalSales,
                                100.0
                            )
                        }
                    )
                )
            }
            if (reportData.topCustomersByAmount.isNotEmpty()) {
                val topCustRows = reportData.topCustomersByAmount.mapIndexed { idx, cust ->
                    listOf(
                        num(idx + 1L),
                        ShopLocale.displayCustomerName(cust.customerName, effectiveLang),
                        money(cust.paidAmount),
                        money(cust.bakiAmount),
                        money(cust.totalBought)
                    )
                }
                val topCustRaw = reportData.topCustomersByAmount.map { cust ->
                    listOf(null, null, cust.paidAmount, cust.bakiAmount, cust.totalBought)
                }
                sections.add(
                    ReportSectionTable(
                        title = "${ShopLocale.get("mr_sec_sales", effectiveLang)} — ${if (isBn) "শীর্ষ ৫ ক্রেতা" else "Top 5 Customers"}",
                        headers = listOf(
                            "#",
                            ShopLocale.get("col_customer", effectiveLang),
                            ShopLocale.get("col_paid", effectiveLang),
                            ShopLocale.get("col_due", effectiveLang),
                            ShopLocale.get("col_total", effectiveLang)
                        ),
                        rows = topCustRows,
                        numericColumns = setOf(2, 3, 4),
                        rawNumbers = topCustRaw
                    )
                )
            }

            // 3. Baki (customers owe) grouped 0-30, 31-60, 61-90, 90+
            val bakiMetrics = listOf(
                ReportSummaryMetric(if (isBn) "শুরুতে বাকি" else "Opening Receivable", money(reportData.bakiOpeningReceivable)),
                ReportSummaryMetric(if (isBn) "নতুন বাকি" else "New Baki", money(reportData.bakiNewCreditGiven)),
                ReportSummaryMetric(if (isBn) "আদায়" else "Payments Received", money(reportData.bakiCollectedThisMonth)),
                ReportSummaryMetric(if (isBn) "শেষে বাকি" else "Closing Receivable", money(reportData.bakiClosingReceivable)),
                ReportSummaryMetric(ShopLocale.get("mr_bucket_0_30", effectiveLang), money(reportData.bakiAgeing.bucket0To30)),
                ReportSummaryMetric(ShopLocale.get("mr_bucket_31_60", effectiveLang), money(reportData.bakiAgeing.bucket31To60)),
                ReportSummaryMetric(ShopLocale.get("mr_bucket_61_90", effectiveLang), money(reportData.bakiAgeing.bucket61To90)),
                ReportSummaryMetric(ShopLocale.get("mr_bucket_90_plus", effectiveLang), money(reportData.bakiAgeing.bucket90Plus))
            )
            val bakiRows = reportData.debtorRowsGrouped.map { d ->
                val bucketLbl = when (d.ageingBucket) {
                    "0-30" -> ShopLocale.get("mr_bucket_0_30", effectiveLang)
                    "31-60" -> ShopLocale.get("mr_bucket_31_60", effectiveLang)
                    "61-90" -> ShopLocale.get("mr_bucket_61_90", effectiveLang)
                    else -> ShopLocale.get("mr_bucket_90_plus", effectiveLang)
                }
                listOf(
                    d.customerName,
                    ShopLocale.digits(d.customerPhone.ifBlank { "—" }, effectiveLang),
                    bucketLbl,
                    num(d.daysOverdue),
                    money(d.amountOwed)
                )
            }
            val bakiRaw = reportData.debtorRowsGrouped.map { d -> listOf(null, null, null, d.daysOverdue.toDouble(), d.amountOwed) }
            sections.add(
                ReportSectionTable(
                    title = ShopLocale.get("mr_sec_baki", effectiveLang),
                    summaryMetrics = bakiMetrics,
                    headers = listOf(
                        ShopLocale.get("col_customer", effectiveLang),
                        if (isBn) "ফোন" else "Phone",
                        ShopLocale.get("mr_col_age_group", effectiveLang),
                        ShopLocale.get("mr_col_days_overdue", effectiveLang),
                        ShopLocale.get("col_amount", effectiveLang)
                    ),
                    rows = bakiRows,
                    numericColumns = setOf(3, 4),
                    rawNumbers = bakiRaw,
                    totals = if (bakiRows.isNotEmpty()) listOf(
                        if (isBn) "মোট (${num(reportData.debtorCount)} জন)" else "Total (${num(reportData.debtorCount)})",
                        "—",
                        "—",
                        "—",
                        money(reportData.bakiClosingReceivable)
                    ) else null,
                    rawTotals = if (bakiRows.isNotEmpty()) listOf(null, null, null, null, reportData.bakiClosingReceivable) else emptyList(),
                    checkLine = formatMonthlyCheckLine(reportData.bakiCheck, effectiveLang),
                    checkMatches = reportData.bakiCheck.matches,
                    checkAvailable = reportData.bakiCheck.isAvailable,
                    emptyMessage = noSectionRowsLabel
                )
            )

            // 4. Suppliers
            val supMetrics = listOf(
                ReportSummaryMetric(if (isBn) "শুরুতে দেনা" else "Opening Payable", money(reportData.supplierOpeningPayable)),
                ReportSummaryMetric(ShopLocale.get("mr_col_credit_purchases", effectiveLang), money(reportData.supplierCreditPurchasesThisMonth)),
                ReportSummaryMetric(ShopLocale.get("mr_col_payments_out", effectiveLang), money(reportData.supplierPaymentsOutThisMonth)),
                ReportSummaryMetric(ShopLocale.get("mr_col_returns", effectiveLang), money(reportData.supplierReturnsThisMonth)),
                ReportSummaryMetric(if (isBn) "শেষে দেনা" else "Closing Payable", money(reportData.supplierClosingPayable))
            )
            val supRows = reportData.supplierRows.map { s ->
                listOf(
                    s.supplierName,
                    money(s.opening),
                    money(s.creditPurchases),
                    money(s.paymentsOut),
                    money(s.returns),
                    money(s.closing)
                )
            }
            val supRaw = reportData.supplierRows.map { s ->
                listOf(null, s.opening, s.creditPurchases, s.paymentsOut, s.returns, s.closing)
            }
            sections.add(
                ReportSectionTable(
                    title = ShopLocale.get("mr_sec_suppliers", effectiveLang),
                    summaryMetrics = supMetrics,
                    headers = listOf(
                        ShopLocale.get("col_supplier", effectiveLang),
                        ShopLocale.get("mr_col_opening", effectiveLang),
                        ShopLocale.get("mr_col_credit_purchases", effectiveLang),
                        ShopLocale.get("mr_col_payments_out", effectiveLang),
                        ShopLocale.get("mr_col_returns", effectiveLang),
                        ShopLocale.get("mr_col_closing", effectiveLang)
                    ),
                    rows = supRows,
                    numericColumns = setOf(1, 2, 3, 4, 5),
                    rawNumbers = supRaw,
                    totals = if (supRows.isNotEmpty()) listOf(
                        if (isBn) "মোট (${num(reportData.supplierRowCount)})" else "Total (${num(reportData.supplierRowCount)})",
                        money(reportData.supplierOpeningPayable),
                        money(reportData.supplierCreditPurchasesThisMonth),
                        money(reportData.supplierPaymentsOutThisMonth),
                        money(reportData.supplierReturnsThisMonth),
                        money(reportData.supplierClosingPayable)
                    ) else null,
                    rawTotals = if (supRows.isNotEmpty()) listOf(
                        null,
                        reportData.supplierOpeningPayable,
                        reportData.supplierCreditPurchasesThisMonth,
                        reportData.supplierPaymentsOutThisMonth,
                        reportData.supplierReturnsThisMonth,
                        reportData.supplierClosingPayable
                    ) else emptyList(),
                    checkLine = formatMonthlyCheckLine(reportData.supplierCheck, effectiveLang),
                    checkMatches = reportData.supplierCheck.matches,
                    checkAvailable = reportData.supplierCheck.isAvailable,
                    emptyMessage = noSectionRowsLabel
                )
            )

            // 5. Stock (Opening Not Available unless computable, Low Stock, Slow Sellers 30d)
            val openingStockStr = reportData.openingStockValueComputed?.let { money(it) }
                ?: ShopLocale.get("mr_not_available", effectiveLang)
            val stockMetrics = buildList {
                add(ReportSummaryMetric(if (isBn) "প্রারম্ভিক স্টক" else "Opening Stock Value", openingStockStr))
                if (canSeeProfit) {
                    add(ReportSummaryMetric(if (isBn) "বর্তমান স্টক মূল্য" else "Stock Value (at Cost)", money(reportData.closingStockValue)))
                }
                add(ReportSummaryMetric(ShopLocale.get("mr_stock_item_count", effectiveLang), num(reportData.stockItemCount)))
                add(ReportSummaryMetric(ShopLocale.get("mr_total_units", effectiveLang), numD(reportData.totalStockUnits)))
            }
            val lowRows = reportData.lowStockItems.map { item ->
                val unit = ShopLocale.displayUnit(item.unit, effectiveLang)
                val base = listOf(item.productName, "${numD(item.stock)} $unit", "${numD(item.lowStockLimit)} $unit")
                if (canSeeProfit) base + money(item.stockValueAtCost) else base
            }
            val lowRaw = reportData.lowStockItems.map { item ->
                if (canSeeProfit) listOf(null, item.stock, item.lowStockLimit, item.stockValueAtCost)
                else listOf(null, item.stock, item.lowStockLimit)
            }
            sections.add(
                ReportSectionTable(
                    title = "${ShopLocale.get("mr_sec_stock", effectiveLang)} — ${ShopLocale.get("mr_low_stock_short", effectiveLang)} (${num(reportData.lowStockCount)})",
                    summaryMetrics = stockMetrics,
                    headers = if (canSeeProfit) {
                        listOf(
                            ShopLocale.get("col_product", effectiveLang),
                            ShopLocale.get("mr_col_stock", effectiveLang),
                            ShopLocale.get("mr_col_limit", effectiveLang),
                            ShopLocale.get("mr_col_value_cost", effectiveLang)
                        )
                    } else {
                        listOf(
                            ShopLocale.get("col_product", effectiveLang),
                            ShopLocale.get("mr_col_stock", effectiveLang),
                            ShopLocale.get("mr_col_limit", effectiveLang)
                        )
                    },
                    rows = lowRows,
                    numericColumns = if (canSeeProfit) setOf(3) else emptySet(),
                    rawNumbers = lowRaw,
                    checkLine = formatMonthlyCheckLine(reportData.stockCheck, effectiveLang),
                    checkMatches = reportData.stockCheck.matches,
                    checkAvailable = reportData.stockCheck.isAvailable,
                    emptyMessage = if (isBn) "সব পণ্যের স্টক স্বাভাবিক" else "No low-stock items"
                )
            )
            if (reportData.slowStock30DayItems.isNotEmpty()) {
                val slowRows = reportData.slowStock30DayItems.map { item ->
                    val unit = ShopLocale.displayUnit(item.unit, effectiveLang)
                    val base = listOf(item.productName, "${numD(item.stock)} $unit", unit)
                    if (canSeeProfit) base + money(item.moneyStuck) else base
                }
                val slowRaw = reportData.slowStock30DayItems.map { item ->
                    if (canSeeProfit) listOf(null, item.stock, null, item.moneyStuck)
                    else listOf(null, item.stock, null)
                }
                sections.add(
                    ReportSectionTable(
                        title = "${ShopLocale.get("mr_sec_stock", effectiveLang)} — ${if (isBn) "৩০ দিনে বিক্রি হয়নি" else "Slow Sellers (30+ Days)"} (${num(reportData.slowStockCount)})",
                        headers = if (canSeeProfit) {
                            listOf(
                                ShopLocale.get("col_product", effectiveLang),
                                ShopLocale.get("mr_col_stock", effectiveLang),
                                if (isBn) "একক" else "Unit",
                                ShopLocale.get("mr_col_value_cost", effectiveLang)
                            )
                        } else {
                            listOf(
                                ShopLocale.get("col_product", effectiveLang),
                                ShopLocale.get("mr_col_stock", effectiveLang),
                                if (isBn) "একক" else "Unit"
                            )
                        },
                        rows = slowRows,
                        numericColumns = if (canSeeProfit) setOf(3) else emptySet(),
                        rawNumbers = slowRaw,
                        totals = if (canSeeProfit) listOf(
                            if (isBn) "মোট আটকে আছে" else "Total Stuck",
                            "—",
                            "—",
                            money(reportData.totalSlowStockMoneyStuck)
                        ) else null,
                        rawTotals = if (canSeeProfit) listOf(null, null, null, reportData.totalSlowStockMoneyStuck) else emptyList()
                    )
                )
            }

            // 6. Purchases (Bought, Returns, Net)
            val purchaseMetrics = listOf(
                ReportSummaryMetric(ShopLocale.get("mr_total_bought", effectiveLang), money(reportData.totalBought)),
                ReportSummaryMetric(ShopLocale.get("mr_purchase_returns", effectiveLang), money(reportData.purchaseReturnsTotal)),
                ReportSummaryMetric(ShopLocale.get("mr_net_purchases", effectiveLang), money(reportData.netPurchases))
            )
            val purRows = buildList {
                reportData.purchaseRows.forEach { p ->
                    add(
                        listOf(
                            ShopLocale.get("entry_purchase", effectiveLang),
                            p.supplierName,
                            ShopLocale.formatDateOnly(p.dateMillis, effectiveLang),
                            money(p.totalAmount),
                            money(p.paidAmount),
                            money(p.creditAmount)
                        )
                    )
                }
                reportData.returnRows.forEach { r ->
                    add(
                        listOf(
                            ShopLocale.get("entry_purchase_return", effectiveLang),
                            r.supplierName,
                            ShopLocale.formatDateOnly(r.dateMillis, effectiveLang),
                            money(-r.amount),
                            "—",
                            "—"
                        )
                    )
                }
            }
            val purRaw = buildList {
                reportData.purchaseRows.forEach { p ->
                    add(listOf(null, null, null, p.totalAmount, p.paidAmount, p.creditAmount))
                }
                reportData.returnRows.forEach { r ->
                    add(listOf(null, null, null, -r.amount, null, null))
                }
            }
            sections.add(
                ReportSectionTable(
                    title = ShopLocale.get("mr_sec_purchases", effectiveLang),
                    summaryMetrics = purchaseMetrics,
                    headers = listOf(
                        ShopLocale.get("col_type", effectiveLang),
                        ShopLocale.get("col_supplier", effectiveLang),
                        ShopLocale.get("col_date", effectiveLang),
                        ShopLocale.get("col_total", effectiveLang),
                        ShopLocale.get("col_paid", effectiveLang),
                        ShopLocale.get("col_due", effectiveLang)
                    ),
                    rows = purRows,
                    numericColumns = setOf(3, 4, 5),
                    rawNumbers = purRaw,
                    totals = if (purRows.isNotEmpty()) listOf(
                        if (isBn) "নিট ক্রয়" else "Net Purchases",
                        "—",
                        "—",
                        money(reportData.netPurchases),
                        money(reportData.supplierPaidAtPurchaseThisMonth),
                        money(reportData.supplierCreditPurchasesThisMonth)
                    ) else null,
                    rawTotals = if (purRows.isNotEmpty()) listOf(
                        null,
                        null,
                        null,
                        reportData.netPurchases,
                        reportData.supplierPaidAtPurchaseThisMonth,
                        reportData.supplierCreditPurchasesThisMonth
                    ) else emptyList(),
                    checkLine = formatMonthlyCheckLine(reportData.purchasesCheck, effectiveLang),
                    checkMatches = reportData.purchasesCheck.matches,
                    checkAvailable = reportData.purchasesCheck.isAvailable,
                    emptyMessage = noSectionRowsLabel
                )
            )

            // 7. Expenses & Other Income by category
            val eiMetrics = listOf(
                ReportSummaryMetric(if (isBn) "মোট খরচ" else "Total Expenses", money(reportData.totalExpenses)),
                ReportSummaryMetric(ShopLocale.get("mr_other_income", effectiveLang), money(reportData.otherIncome)),
                ReportSummaryMetric(ShopLocale.get("mr_net_income_expense", effectiveLang), money(reportData.netOtherIncomeMinusExpenses))
            )
            val expRows = reportData.expensesByCategory.map { c ->
                listOf(
                    ShopLocale.displayCategory(c.category, effectiveLang),
                    num(c.count.toLong()),
                    pct(c.percentage),
                    money(c.amount)
                )
            }
            val expRaw = reportData.expensesByCategory.map { c ->
                listOf(null, c.count.toDouble(), c.percentage, c.amount)
            }
            sections.add(
                ReportSectionTable(
                    title = "${ShopLocale.get("mr_sec_expenses_income", effectiveLang)} — ${ShopLocale.get("mr_expenses_by_cat", effectiveLang)}",
                    summaryMetrics = eiMetrics,
                    headers = listOf(
                        ShopLocale.get("col_category", effectiveLang),
                        ShopLocale.get("mr_col_count", effectiveLang),
                        ShopLocale.get("mr_col_share", effectiveLang),
                        ShopLocale.get("col_amount", effectiveLang)
                    ),
                    rows = expRows,
                    numericColumns = setOf(1, 2, 3),
                    rawNumbers = expRaw,
                    totals = if (expRows.isNotEmpty()) listOf(
                        if (isBn) "মোট খরচ" else "Total Expenses",
                        num(reportData.totalExpenseCount),
                        pct(100.0),
                        money(reportData.totalExpenses)
                    ) else null,
                    rawTotals = if (expRows.isNotEmpty()) listOf(
                        null,
                        reportData.totalExpenseCount.toDouble(),
                        100.0,
                        reportData.totalExpenses
                    ) else emptyList(),
                    emptyMessage = noSectionRowsLabel
                )
            )
            val incRows = reportData.incomesByCategory.map { c ->
                listOf(
                    ShopLocale.displayCategory(c.category, effectiveLang),
                    num(c.count.toLong()),
                    pct(c.percentage),
                    money(c.amount)
                )
            }
            val incRaw = reportData.incomesByCategory.map { c ->
                listOf(null, c.count.toDouble(), c.percentage, c.amount)
            }
            sections.add(
                ReportSectionTable(
                    title = "${ShopLocale.get("mr_sec_expenses_income", effectiveLang)} — ${ShopLocale.get("mr_income_by_cat", effectiveLang)}",
                    headers = listOf(
                        ShopLocale.get("col_category", effectiveLang),
                        ShopLocale.get("mr_col_count", effectiveLang),
                        ShopLocale.get("mr_col_share", effectiveLang),
                        ShopLocale.get("col_amount", effectiveLang)
                    ),
                    rows = incRows,
                    numericColumns = setOf(1, 2, 3),
                    rawNumbers = incRaw,
                    totals = if (incRows.isNotEmpty()) listOf(
                        ShopLocale.get("mr_other_income", effectiveLang),
                        num(reportData.totalOtherIncomeCount),
                        pct(100.0),
                        money(reportData.otherIncome)
                    ) else null,
                    rawTotals = if (incRows.isNotEmpty()) listOf(
                        null,
                        reportData.totalOtherIncomeCount.toDouble(),
                        100.0,
                        reportData.otherIncome
                    ) else emptyList(),
                    checkLine = formatMonthlyCheckLine(reportData.expensesIncomeCheck, effectiveLang),
                    checkMatches = reportData.expensesIncomeCheck.matches,
                    checkAvailable = reportData.expensesIncomeCheck.isAvailable,
                    emptyMessage = noSectionRowsLabel
                )
            )

            // 8. Cash & Bank
            val cashMetrics = listOf(
                ReportSummaryMetric(ShopLocale.get("mr_col_opening", effectiveLang), money(reportData.cashOpeningTotal)),
                ReportSummaryMetric(ShopLocale.get("money_in_label", effectiveLang), money(reportData.cashInTotal)),
                ReportSummaryMetric(ShopLocale.get("money_out_label", effectiveLang), money(reportData.cashOutTotal)),
                ReportSummaryMetric(ShopLocale.get("mr_col_closing", effectiveLang), money(reportData.cashClosingBalance))
            )
            val cashRows = reportData.accountCashRows.map { acc ->
                listOf(
                    ShopLocale.displayAccountName(acc.accountName, acc.accountId, effectiveLang),
                    money(acc.opening),
                    money(acc.moneyIn),
                    money(acc.moneyOut),
                    money(acc.closing)
                )
            }
            val cashRaw = reportData.accountCashRows.map { acc ->
                listOf(null, acc.opening, acc.moneyIn, acc.moneyOut, acc.closing)
            }
            sections.add(
                ReportSectionTable(
                    title = ShopLocale.get("mr_sec_cash", effectiveLang),
                    summaryMetrics = cashMetrics,
                    headers = listOf(
                        ShopLocale.get("col_account", effectiveLang),
                        ShopLocale.get("mr_col_opening", effectiveLang),
                        ShopLocale.get("col_money_in", effectiveLang),
                        ShopLocale.get("col_money_out", effectiveLang),
                        ShopLocale.get("mr_col_closing", effectiveLang)
                    ),
                    rows = cashRows,
                    numericColumns = setOf(1, 2, 3, 4),
                    rawNumbers = cashRaw,
                    totals = if (cashRows.isNotEmpty()) listOf(
                        if (isBn) "মোট" else "Total",
                        money(reportData.cashOpeningTotal),
                        money(reportData.cashInTotal),
                        money(reportData.cashOutTotal),
                        money(reportData.cashClosingBalance)
                    ) else null,
                    rawTotals = if (cashRows.isNotEmpty()) listOf(
                        null,
                        reportData.cashOpeningTotal,
                        reportData.cashInTotal,
                        reportData.cashOutTotal,
                        reportData.cashClosingBalance
                    ) else emptyList(),
                    checkLine = formatMonthlyCheckLine(reportData.cashCheck, effectiveLang),
                    checkMatches = reportData.cashCheck.matches,
                    checkAvailable = reportData.cashCheck.isAvailable,
                    emptyMessage = noSectionRowsLabel
                )
            )
        }

        // Flat backward-compatible table mirror
        val headers = if (isBn) {
            listOf("বিভাগ", "বিবরণ / আইটেম", "বিস্তারিত", "পরিমাণ")
        } else {
            listOf("Section", "Metric / Item", "Detail", "Amount")
        }
        val flatRows = mutableListOf<List<String>>()
        val flatRaw = mutableListOf<List<Double?>>()
        sections.forEach { sec ->
            if (sec.rows.isEmpty() && !sec.emptyMessage.isNullOrBlank()) {
                flatRows.add(listOf(sec.title, sec.emptyMessage, "—", "—"))
                flatRaw.add(listOf(null, null, null, null))
            } else {
                sec.rows.forEachIndexed { rIdx, row ->
                    val label = row.firstOrNull().orEmpty()
                    val amtIdx = (sec.headers.size - 1).coerceAtLeast(0)
                    val amtStr = row.getOrNull(amtIdx).orEmpty()
                    val detail = if (row.size > 2) row.subList(1, row.size - 1).joinToString(" • ") else "—"
                    val rawVal = sec.rawNumbers.getOrNull(rIdx)?.getOrNull(amtIdx)
                    flatRows.add(listOf(sec.title, label, detail, amtStr))
                    flatRaw.add(listOf(null, null, null, rawVal))
                }
            }
        }

        return ReportTableData(
            title = title,
            storeName = storeNameStr,
            storePhone = ShopLocale.digits(store?.phone ?: "", effectiveLang),
            storeAddress = if (isRestrictedMode) txt(store?.address ?: "", "") else (store?.address ?: ""),
            periodLabel = currentMonthLabel,
            generatedAt = txt(nowFormatted(effectiveLang)),
            headers = headers,
            rows = flatRows,
            numericColumns = setOf(3),
            rawNumbers = flatRaw,
            totals = null,
            rawTotals = emptyList(),
            notes = if (isRestrictedMode) null else formatMonthlyCheckLine(reportData.cashCheck, effectiveLang),
            summaryMetrics = summaryMetrics,
            sections = sections,
            preferLandscape = !isRestrictedMode
        )
    }

    fun buildSalesReportTable(
        sales: List<Sale>,
        store: Store?,
        periodLabel: String,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalAmount = sales.sumOf { it.grandTotal }
        val totalDiscount = sales.sumOf { it.discount }
        val totalProfit = sales.sumOf { it.profit }

        val rows = sales.map { s ->
            val dateStr = s.createdAt?.toDate()?.time?.let { ShopLocale.formatDateShort(it, lang) } ?: "—"
            val status = if (s.paymentType == "baki") ShopLocale.get("status_baki", lang) else ShopLocale.get("status_paid", lang)
            val custName = ShopLocale.displayCustomerName(s.customerName, lang)
            if (canSeeProfit) {
                listOf(
                    s.invoiceNo,
                    dateStr,
                    custName,
                    ShopLocale.formatMoney(s.grandTotal, lang),
                    ShopLocale.formatMoney(s.discount, lang),
                    ShopLocale.formatMoney(s.profit, lang),
                    status
                )
            } else {
                listOf(
                    s.invoiceNo,
                    dateStr,
                    custName,
                    ShopLocale.formatMoney(s.grandTotal, lang),
                    ShopLocale.formatMoney(s.discount, lang),
                    status
                )
            }
        }
        val rawRows = sales.map { s ->
            if (canSeeProfit) {
                listOf(null, null, null, s.grandTotal, s.discount, s.profit, null)
            } else {
                listOf(null, null, null, s.grandTotal, s.discount, null)
            }
        }
        val totalLabel = ShopLocale.format("total_orders_row", lang, sales.size)
        val totals = if (canSeeProfit) {
            listOf(
                totalLabel,
                "",
                "",
                ShopLocale.formatMoney(totalAmount, lang),
                ShopLocale.formatMoney(totalDiscount, lang),
                ShopLocale.formatMoney(totalProfit, lang),
                ""
            )
        } else {
            listOf(
                totalLabel,
                "",
                "",
                ShopLocale.formatMoney(totalAmount, lang),
                ShopLocale.formatMoney(totalDiscount, lang),
                ""
            )
        }
        val rawTotals = if (canSeeProfit) {
            listOf(null, null, null, totalAmount, totalDiscount, totalProfit, null)
        } else {
            listOf(null, null, null, totalAmount, totalDiscount, null)
        }

        val headers = if (canSeeProfit) {
            listOf(
                ShopLocale.get("col_invoice", lang),
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_customer", lang),
                ShopLocale.get("col_total", lang),
                ShopLocale.get("col_discount", lang),
                ShopLocale.get("col_profit", lang),
                ShopLocale.get("col_status", lang)
            )
        } else {
            listOf(
                ShopLocale.get("col_invoice", lang),
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_customer", lang),
                ShopLocale.get("col_total", lang),
                ShopLocale.get("col_discount", lang),
                ShopLocale.get("col_status", lang)
            )
        }

        val paidTotal = sales.filter { it.paymentType != "baki" }.sumOf { it.grandTotal }
        val bakiTotal = sales.filter { it.paymentType == "baki" }.sumOf { it.grandTotal }
        val salesSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট বিক্রয়" else "Total Sales",
                    value = ShopLocale.formatMoney(totalAmount, lang),
                    detail = totalLabel
                )
            )
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "নগদ আদায়" else "Paid Sales",
                    value = ShopLocale.formatMoney(paidTotal, lang)
                )
            )
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "বাকি বিক্রয়" else "Baki (Due)",
                    value = ShopLocale.formatMoney(bakiTotal, lang)
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "মোট লাভ" else "Total Profit",
                        value = ShopLocale.formatMoney(totalProfit, lang),
                        detail = if (lang == AppLanguage.BN) {
                            "ছাড়: ${ShopLocale.formatMoney(totalDiscount, lang)}"
                        } else {
                            "Discount: ${ShopLocale.formatMoney(totalDiscount, lang)}"
                        }
                    )
                )
            }
        }

        return ReportTableData(
            title = ShopLocale.get("rep_sales_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = headers,
            rows = rows,
            numericColumns = if (canSeeProfit) setOf(3, 4, 5) else setOf(3, 4),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (lang == AppLanguage.BN) {
                "মোট অর্ডার: ${ShopLocale.formatNumber(sales.size, lang)}টি • নগদ: ${ShopLocale.formatMoney(paidTotal, lang)} • বাকি: ${ShopLocale.formatMoney(bakiTotal, lang)} • মোট ছাড়: ${ShopLocale.formatMoney(totalDiscount, lang)}"
            } else {
                "Orders: ${sales.size} • Paid: ${ShopLocale.formatMoney(paidTotal, lang)} • Baki: ${ShopLocale.formatMoney(bakiTotal, lang)} • Total Discount: ${ShopLocale.formatMoney(totalDiscount, lang)}"
            },
            summaryMetrics = salesSummary
        )
    }

    fun buildPurchaseReportTable(
        purchases: List<Purchase>,
        returns: List<PurchaseReturn>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalPurchases = purchases.sumOf { it.totalAmount }
        val totalPaid = purchases.sumOf { it.paidAmount }
        val totalDue = purchases.sumOf { it.dueAmount }

        val rows = purchases.map { p ->
            val dateStr = p.createdAt?.toDate()?.time?.let { ShopLocale.formatDateShort(it, lang) } ?: "—"
            val status = if (p.paymentType == "credit") ShopLocale.get("status_credit_due", lang) else ShopLocale.get("status_paid", lang)
            listOf(
                p.purchaseNo,
                dateStr,
                p.supplierName,
                ShopLocale.formatMoney(p.totalAmount, lang),
                ShopLocale.formatMoney(p.paidAmount, lang),
                ShopLocale.formatMoney(p.dueAmount, lang),
                status
            )
        }
        val rawRows = purchases.map { p ->
            listOf(null, null, null, p.totalAmount, p.paidAmount, p.dueAmount, null)
        }
        val totals = listOf(
            ShopLocale.format("total_orders_row", lang, purchases.size),
            "",
            "",
            ShopLocale.formatMoney(totalPurchases, lang),
            ShopLocale.formatMoney(totalPaid, lang),
            ShopLocale.formatMoney(totalDue, lang),
            ""
        )
        val rawTotals = listOf<Double?>(null, null, null, totalPurchases, totalPaid, totalDue, null)
        val totalReturns = returns.sumOf { it.amount }
        val purchaseSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট ক্রয়" else "Total Purchases",
                value = ShopLocale.formatMoney(totalPurchases, lang),
                detail = ShopLocale.format("total_orders_row", lang, purchases.size)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পরিশোধিত" else "Total Paid",
                value = ShopLocale.formatMoney(totalPaid, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "সরবরাহকারী বকেয়া" else "Total Due",
                value = ShopLocale.formatMoney(totalDue, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "ক্রয় ফেরত" else "Purchase Returns",
                value = ShopLocale.formatMoney(totalReturns, lang),
                detail = if (lang == AppLanguage.BN) "${returns.size}টি ফেরত" else "${returns.size} returns"
            )
        )
        return ReportTableData(
            title = ShopLocale.get("rep_purchase_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_order_no", lang),
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_supplier", lang),
                ShopLocale.get("col_total", lang),
                ShopLocale.get("col_paid", lang),
                ShopLocale.get("col_due", lang),
                ShopLocale.get("col_status", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4, 5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (lang == AppLanguage.BN) {
                "মোট ক্রয়: ${ShopLocale.formatMoney(totalPurchases, lang)} | পরিশোধিত: ${ShopLocale.formatMoney(totalPaid, lang)} | বকেয়া: ${ShopLocale.formatMoney(totalDue, lang)} | ফেরত: ${ShopLocale.formatMoney(totalReturns, lang)}"
            } else {
                "Total Purchases: ${ShopLocale.formatMoney(totalPurchases, lang)} | Paid: ${ShopLocale.formatMoney(totalPaid, lang)} | Due: ${ShopLocale.formatMoney(totalDue, lang)} | Returns: ${ShopLocale.formatMoney(totalReturns, lang)}"
            },
            summaryMetrics = purchaseSummary
        )
    }

    fun buildDayBookReportTable(
        entries: List<DayBookEntry>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalIn = entries.filter { it.isIncome }.sumOf { it.amount }
        val totalOut = entries.filter { !it.isIncome }.sumOf { it.amount }

        val rows = entries.map { e ->
            val dateStr = ShopLocale.formatDateShort(e.timestamp, lang)
            val direction = if (e.isIncome) ShopLocale.get("flow_in", lang) else ShopLocale.get("flow_out", lang)
            val typeStr = ShopLocale.displayEntryType(e.type, lang)
            val partyStr = ShopLocale.displayCustomerName(e.partyName, lang)
            listOf(
                dateStr,
                typeStr,
                e.referenceNo,
                partyStr,
                direction,
                ShopLocale.formatMoney(e.amount, lang)
            )
        }
        val rawRows = entries.map { e ->
            listOf(null, null, null, null, null, if (e.isIncome) e.amount else -e.amount)
        }
        val totals = listOf(
            ShopLocale.get("net_flow_row", lang),
            "",
            "",
            ShopLocale.format(
                "daybook_totals_note",
                lang,
                ShopLocale.formatMoney(totalIn, lang),
                ShopLocale.formatMoney(totalOut, lang)
            ),
            "",
            ShopLocale.formatMoney(totalIn - totalOut, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, null, null, totalIn - totalOut)
        val netFlow = totalIn - totalOut
        val dayBookSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "টাকা এসেছে" else "Total Cash In",
                value = ShopLocale.formatMoney(totalIn, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "টাকা গেছে" else "Total Cash Out",
                value = ShopLocale.formatMoney(totalOut, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "নিট প্রবাহ" else "Net Cash Flow",
                value = ShopLocale.formatMoney(netFlow, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট লেনদেন" else "Transactions",
                value = ShopLocale.formatNumber(entries.size, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.get("rep_day_book_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_datetime", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_party", lang),
                ShopLocale.get("col_flow", lang),
                ShopLocale.get("col_amount", lang)
            ),
            rows = rows,
            numericColumns = setOf(5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = totals[3],
            summaryMetrics = dayBookSummary
        )
    }

    fun buildProfitLossReportTable(
        pnl: ProfitAndLossStatement,
        isStockBased: Boolean,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN,
        isFatherMode: Boolean = false
    ): ReportTableData {
        val effectiveStockBased = if (isFatherMode) true else isStockBased
        val cogsMethod = if (effectiveStockBased) ShopLocale.get("pnl_stock_based", lang) else ShopLocale.get("pnl_purchases_based", lang)
        val rows = mutableListOf(
            listOf(ShopLocale.get("pnl_net_sales", lang), ShopLocale.formatMoney(pnl.netSales, lang)),
            listOf(
                ShopLocale.format("pnl_cogs_label", lang, cogsMethod),
                "-${ShopLocale.formatMoney(pnl.costOfGoodsSold, lang)}"
            )
        )
        val rawRows = mutableListOf(
            listOf<Double?>(null, pnl.netSales),
            listOf<Double?>(null, -pnl.costOfGoodsSold)
        )
        if (isFatherMode) {
            rows.add(listOf(ShopLocale.get("col_profit", lang), ShopLocale.formatMoney(pnl.grossProfit, lang)))
            rawRows.add(listOf<Double?>(null, pnl.grossProfit))
        } else {
            rows.add(listOf(ShopLocale.get("pnl_gross_profit", lang), ShopLocale.formatMoney(pnl.grossProfit, lang)))
            rows.add(listOf(ShopLocale.get("pnl_other_income", lang), "+${ShopLocale.formatMoney(pnl.otherIncome, lang)}"))
            rows.add(listOf(ShopLocale.get("pnl_operating_expenses", lang), "-${ShopLocale.formatMoney(pnl.expenses, lang)}"))
            rawRows.add(listOf<Double?>(null, pnl.grossProfit))
            rawRows.add(listOf<Double?>(null, pnl.otherIncome))
            rawRows.add(listOf<Double?>(null, -pnl.expenses))
            pnl.expensesByCategory.forEach { (cat, amt) ->
                rows.add(listOf("  • ${ShopLocale.displayCategory(cat, lang)}", "-${ShopLocale.formatMoney(amt, lang)}"))
                rawRows.add(listOf<Double?>(null, -amt))
            }
            rows.add(listOf(ShopLocale.get("pnl_net_profit", lang), ShopLocale.formatMoney(pnl.netProfit, lang)))
            rawRows.add(listOf<Double?>(null, pnl.netProfit))
        }

        val totalAmount = if (isFatherMode) pnl.grossProfit else pnl.netProfit
        val totalLabel = if (isFatherMode) ShopLocale.get("col_profit", lang).uppercase() else ShopLocale.get("pnl_net_profit", lang).uppercase()

        val grossMarginPct = if (pnl.netSales > 0.0) (pnl.grossProfit / pnl.netSales) * 100.0 else 0.0
        val netMarginPct = if (pnl.netSales > 0.0) (pnl.netProfit / pnl.netSales) * 100.0 else 0.0
        val pnlSummary = if (isFatherMode) {
            listOf(
                ReportSummaryMetric(ShopLocale.get("pnl_net_sales", lang), ShopLocale.formatMoney(pnl.netSales, lang)),
                ReportSummaryMetric(ShopLocale.get("col_profit", lang), ShopLocale.formatMoney(pnl.grossProfit, lang))
            )
        } else {
            listOf(
                ReportSummaryMetric(ShopLocale.get("pnl_net_sales", lang), ShopLocale.formatMoney(pnl.netSales, lang)),
                ReportSummaryMetric(
                    ShopLocale.get("pnl_gross_profit", lang),
                    ShopLocale.formatMoney(pnl.grossProfit, lang),
                    ShopLocale.formatPercent(grossMarginPct, 1, lang)
                ),
                ReportSummaryMetric(ShopLocale.get("pnl_operating_expenses", lang), ShopLocale.formatMoney(pnl.expenses, lang)),
                ReportSummaryMetric(
                    ShopLocale.get("pnl_net_profit", lang),
                    ShopLocale.formatMoney(pnl.netProfit, lang),
                    ShopLocale.formatPercent(netMarginPct, 1, lang)
                )
            )
        }

        return ReportTableData(
            title = if (isFatherMode) ShopLocale.get("nav_business", lang) else ShopLocale.get("rep_pnl_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(ShopLocale.get("col_line_item", lang), ShopLocale.get("col_amount", lang)),
            rows = rows,
            numericColumns = setOf(1),
            rawNumbers = rawRows,
            totals = listOf(totalLabel, ShopLocale.formatMoney(totalAmount, lang)),
            rawTotals = listOf(null, totalAmount),
            notes = if (effectiveStockBased) ShopLocale.get("pnl_note_stock", lang) else ShopLocale.get("pnl_note_purchase", lang),
            summaryMetrics = pnlSummary
        )
    }

    fun buildExpenseReportTable(
        expenses: List<Expense>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalExpenses = expenses.sumOf { it.amount }
        val rows = expenses.map { exp ->
            listOf(
                ShopLocale.formatDateLongFullMonth(exp.date, lang),
                ShopLocale.displayCategory(exp.category, lang),
                ShopLocale.displayAccountName(exp.accountName, exp.accountId, lang),
                exp.note,
                ShopLocale.formatMoneyFixed2(exp.amount, lang)
            )
        }
        val rawNumbers = expenses.map { exp ->
            listOf<Double?>(null, null, null, null, exp.amount)
        }
        val totalFormatted = ShopLocale.formatMoneyFixed2(totalExpenses, lang)
        val totals = listOf(
            ShopLocale.get("total_expenses_row", lang),
            "",
            "",
            ShopLocale.format("entries_count", lang, expenses.size),
            totalFormatted
        )
        val rawTotals = listOf<Double?>(null, null, null, null, totalExpenses)
        val byCat = expenses.groupBy { ShopLocale.displayCategory(it.category, lang) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        val topCat = byCat.firstOrNull()
        val avgExp = if (expenses.isNotEmpty()) totalExpenses / expenses.size else 0.0
        val expenseSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট খরচ" else "Total Expenses",
                    value = totalFormatted,
                    detail = ShopLocale.format("entries_count", lang, expenses.size)
                )
            )
            if (topCat != null) {
                val pct = if (totalExpenses > 0.0) (topCat.second / totalExpenses) * 100.0 else 0.0
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "সর্বোচ্চ খাত" else "Top Category",
                        value = topCat.first,
                        detail = "${ShopLocale.formatMoneyFixed2(topCat.second, lang)} (${ShopLocale.formatPercent(pct, 1, lang)})"
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "গড় খরচ" else "Average Entry",
                    value = ShopLocale.formatMoneyFixed2(avgExp, lang)
                )
            )
        }
        val catBreakdownNote = if (byCat.isNotEmpty()) {
            val topText = byCat.take(4).joinToString(" • ") { "${it.first}: ${ShopLocale.formatMoneyFixed2(it.second, lang)}" }
            "${ShopLocale.get("total_expenses_row", lang)}: $totalFormatted | $topText"
        } else {
            "${ShopLocale.get("total_expenses_row", lang)}: $totalFormatted"
        }

        return ReportTableData(
            title = ShopLocale.get("rep_expense_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_category", lang),
                ShopLocale.get("col_account", lang),
                ShopLocale.get("col_note", lang),
                ShopLocale.get("col_amount", lang)
            ),
            rows = rows,
            numericColumns = setOf(4),
            rawNumbers = rawNumbers,
            totals = totals,
            rawTotals = rawTotals,
            notes = catBreakdownNote,
            summaryMetrics = expenseSummary
        )
    }

    fun buildOtherIncomeReportTable(
        incomes: List<OtherIncome>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalIncome = incomes.sumOf { it.amount }
        val rows = incomes.map { inc ->
            listOf(
                ShopLocale.formatDateLongFullMonth(inc.date, lang),
                ShopLocale.displayCategory(inc.category, lang),
                ShopLocale.displayAccountName(inc.accountName, inc.accountId, lang),
                inc.note,
                ShopLocale.formatMoneyFixed2(inc.amount, lang)
            )
        }
        val rawNumbers = incomes.map { inc ->
            listOf<Double?>(null, null, null, null, inc.amount)
        }
        val totalFormatted = "+${ShopLocale.formatMoneyFixed2(totalIncome, lang)}"
        val totals = listOf(
            ShopLocale.get("total_other_income_row", lang),
            "",
            "",
            ShopLocale.format("entries_count", lang, incomes.size),
            totalFormatted
        )
        val rawTotals = listOf<Double?>(null, null, null, null, totalIncome)
        val byCat = incomes.groupBy { ShopLocale.displayCategory(it.category, lang) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        val topCat = byCat.firstOrNull()
        val avgInc = if (incomes.isNotEmpty()) totalIncome / incomes.size else 0.0
        val incomeSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট অন্যান্য আয়" else "Total Other Income",
                    value = ShopLocale.formatMoneyFixed2(totalIncome, lang),
                    detail = ShopLocale.format("entries_count", lang, incomes.size)
                )
            )
            if (topCat != null) {
                val pct = if (totalIncome > 0.0) (topCat.second / totalIncome) * 100.0 else 0.0
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "প্রধান উৎস" else "Top Source",
                        value = topCat.first,
                        detail = "${ShopLocale.formatMoneyFixed2(topCat.second, lang)} (${ShopLocale.formatPercent(pct, 1, lang)})"
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "গড় আয়" else "Average Entry",
                    value = ShopLocale.formatMoneyFixed2(avgInc, lang)
                )
            )
        }
        val catBreakdownNote = if (byCat.isNotEmpty()) {
            val topText = byCat.take(4).joinToString(" • ") { "${it.first}: ${ShopLocale.formatMoneyFixed2(it.second, lang)}" }
            "${ShopLocale.get("total_other_income_row", lang)}: ${ShopLocale.formatMoneyFixed2(totalIncome, lang)} | $topText"
        } else {
            "${ShopLocale.get("total_other_income_row", lang)}: ${ShopLocale.formatMoneyFixed2(totalIncome, lang)}"
        }

        return ReportTableData(
            title = ShopLocale.get("rep_other_income_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_category", lang),
                ShopLocale.get("col_account", lang),
                ShopLocale.get("col_note", lang),
                ShopLocale.get("col_amount", lang)
            ),
            rows = rows,
            numericColumns = setOf(4),
            rawNumbers = rawNumbers,
            totals = totals,
            rawTotals = rawTotals,
            notes = catBreakdownNote,
            summaryMetrics = incomeSummary
        )
    }

    fun buildInventoryReportTable(
        products: List<Product>,
        store: Store?,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val now = nowFormatted(lang)
        val totalItemsCount = products.sumOf { it.stock }
        val totalCostValue = products.sumOf { it.stock * it.buyPrice }
        val totalSaleValue = products.sumOf { it.stock * it.sellPrice }

        val rows = products.map { p ->
            val stockText = "${ShopLocale.formatNumber(p.stock, lang)} ${ShopLocale.displayUnit(p.unit, lang)}"
            if (canSeeProfit) {
                listOf(
                    p.name,
                    stockText,
                    ShopLocale.formatMoney(p.buyPrice, lang),
                    ShopLocale.formatMoney(p.sellPrice, lang),
                    ShopLocale.formatMoney(p.stock * p.buyPrice, lang)
                )
            } else {
                listOf(
                    p.name,
                    stockText,
                    ShopLocale.formatMoney(p.sellPrice, lang),
                    ShopLocale.formatMoney(p.stock * p.sellPrice, lang)
                )
            }
        }
        val rawRows = products.map { p ->
            if (canSeeProfit) {
                listOf(null, p.stock.toDouble(), p.buyPrice, p.sellPrice, p.stock * p.buyPrice)
            } else {
                listOf(null, p.stock.toDouble(), p.sellPrice, p.stock * p.sellPrice)
            }
        }
        val totalProductsLabel = ShopLocale.format("total_products_row", lang, products.size)
        val totalItemsLabel = ShopLocale.format("items_count_label", lang, totalItemsCount)
        val totals = if (canSeeProfit) {
            listOf(
                totalProductsLabel,
                totalItemsLabel,
                "",
                "",
                ShopLocale.formatMoney(totalCostValue, lang)
            )
        } else {
            listOf(
                totalProductsLabel,
                totalItemsLabel,
                "",
                ShopLocale.formatMoney(totalSaleValue, lang)
            )
        }
        val rawTotals = if (canSeeProfit) {
            listOf(null, totalItemsCount.toDouble(), null, null, totalCostValue)
        } else {
            listOf(null, totalItemsCount.toDouble(), null, totalSaleValue)
        }
        val headers = if (canSeeProfit) {
            listOf(
                ShopLocale.get("col_product", lang),
                ShopLocale.get("col_stock", lang),
                ShopLocale.get("col_buy_price", lang),
                ShopLocale.get("col_sell_price", lang),
                ShopLocale.get("col_cost_value", lang)
            )
        } else {
            listOf(
                ShopLocale.get("col_product", lang),
                ShopLocale.get("col_stock", lang),
                ShopLocale.get("col_sell_price", lang),
                ShopLocale.get("col_sale_value", lang)
            )
        }
        val invSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট পণ্য" else "Total Products",
                    value = ShopLocale.formatNumber(products.size, lang),
                    detail = totalItemsLabel
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "ক্রয় মূল্য (মজুদ)" else "Total Cost Value",
                        value = ShopLocale.formatMoney(totalCostValue, lang)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "বিক্রয় মূল্য (সম্ভাব্য)" else "Total Sale Value",
                    value = ShopLocale.formatMoney(totalSaleValue, lang)
                )
            )
        }
        return ReportTableData(
            title = ShopLocale.get("rep_inventory_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = ShopLocale.format("as_of_date", lang, now),
            generatedAt = now,
            headers = headers,
            rows = rows,
            numericColumns = if (canSeeProfit) setOf(1, 2, 3, 4) else setOf(1, 2, 3),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (canSeeProfit) {
                if (lang == AppLanguage.BN) {
                    "সম্ভাব্য বিক্রয় মূল্য: ${ShopLocale.formatMoney(totalSaleValue, lang)} • সম্ভাব্য লাভ: ${ShopLocale.formatMoney(totalSaleValue - totalCostValue, lang)}"
                } else {
                    "Potential Retail Value: ${ShopLocale.formatMoney(totalSaleValue, lang)} • Potential Gross Profit: ${ShopLocale.formatMoney(totalSaleValue - totalCostValue, lang)}"
                }
            } else null,
            summaryMetrics = invSummary
        )
    }

    fun buildCustomerStatementReportTable(
        rep: CustomerStatementReport,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val rows = rep.entries.map { e ->
            val dStr = ShopLocale.formatDateShort(e.timestamp, lang)
            val isBakiSale = e.type == LedgerEntryType.BAKI_SALE
            val typeStr = if (isBakiSale) ShopLocale.get("entry_baki_sale", lang) else ShopLocale.get("entry_payment_in", lang)
            val signedAmt = if (isBakiSale) "+${ShopLocale.formatMoney(e.amount, lang)}" else "-${ShopLocale.formatMoney(e.amount, lang)}"
            listOf(
                dStr,
                typeStr,
                e.referenceNo,
                signedAmt,
                ShopLocale.formatMoney(e.runningBalance, lang)
            )
        }
        val rawRows = rep.entries.map { e ->
            val isBakiSale = e.type == LedgerEntryType.BAKI_SALE
            listOf<Double?>(null, null, null, if (isBakiSale) e.amount else -e.amount, e.runningBalance)
        }
        val totals = listOf(
            ShopLocale.get("closing_balance_row", lang),
            "",
            "",
            ShopLocale.format(
                "cust_stmt_totals_note",
                lang,
                ShopLocale.formatMoney(rep.totalBakiSales, lang),
                ShopLocale.formatMoney(rep.totalPayments, lang)
            ),
            ShopLocale.formatMoney(rep.closingBalance, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, null, rep.closingBalance)
        val custSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পূর্বের জের" else "Opening Balance",
                value = ShopLocale.formatMoney(rep.openingBalance, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট বাকি বিক্রয়" else "Baki Sales",
                value = ShopLocale.formatMoney(rep.totalBakiSales, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট আদায়" else "Payments Received",
                value = ShopLocale.formatMoney(rep.totalPayments, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "শেষে বাকি আছে" else "Closing Balance",
                value = ShopLocale.formatMoney(rep.closingBalance, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.format("rep_customer_stmt_title", lang, rep.customer.name),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_amount", lang),
                ShopLocale.get("col_balance", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = "${ShopLocale.format("opening_balance_note", lang, ShopLocale.formatMoney(rep.openingBalance, lang))} • ${ShopLocale.get("closing_balance_row", lang)}: ${ShopLocale.formatMoney(rep.closingBalance, lang)}",
            summaryMetrics = custSummary
        )
    }

    fun buildSupplierStatementReportTable(
        rep: SupplierStatementReport,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val rows = rep.entries.map { e ->
            val dStr = ShopLocale.formatDateShort(e.timestamp, lang)
            listOf(
                dStr,
                ShopLocale.displayEntryType(e.type, lang),
                e.referenceNo,
                ShopLocale.formatMoney(e.amount, lang),
                ShopLocale.formatMoney(e.runningBalance, lang)
            )
        }
        val rawRows = rep.entries.map { e ->
            listOf<Double?>(null, null, null, e.amount, e.runningBalance)
        }
        val totals = listOf(
            ShopLocale.get("closing_balance_row", lang),
            "",
            "",
            ShopLocale.format(
                "sup_stmt_totals_note",
                lang,
                ShopLocale.formatMoney(rep.totalPurchases, lang),
                ShopLocale.formatMoney(rep.totalReturns, lang)
            ),
            ShopLocale.formatMoney(rep.closingBalance, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, null, rep.closingBalance)
        val supSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পূর্বের জের" else "Opening Balance",
                value = ShopLocale.formatMoney(rep.openingBalance, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট ক্রয়" else "Total Purchases",
                value = ShopLocale.formatMoney(rep.totalPurchases, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "ক্রয় ফেরত" else "Total Returns",
                value = ShopLocale.formatMoney(rep.totalReturns, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "শেষে দেব" else "Closing Balance",
                value = ShopLocale.formatMoney(rep.closingBalance, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.format("rep_supplier_stmt_title", lang, rep.supplier.name),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_amount", lang),
                ShopLocale.get("col_balance", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = "${ShopLocale.format("opening_balance_note", lang, ShopLocale.formatMoney(rep.openingBalance, lang))} • ${ShopLocale.get("closing_balance_row", lang)}: ${ShopLocale.formatMoney(rep.closingBalance, lang)}",
            summaryMetrics = supSummary
        )
    }

    fun buildAccountStatementReportTable(
        rep: AccountStatementReport,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val rows = rep.entries.map { entry ->
            listOf(
                ShopLocale.formatDateShort(entry.timestamp, lang),
                ShopLocale.displayEntryType(entry.type, lang),
                entry.referenceNo,
                if (entry.moneyIn > 0.0) ShopLocale.formatMoneyFixed2(entry.moneyIn, lang) else "—",
                if (entry.moneyOut > 0.0) ShopLocale.formatMoneyFixed2(entry.moneyOut, lang) else "—",
                ShopLocale.formatMoneyFixed2(entry.runningBalance, lang)
            )
        }
        val rawRows = rep.entries.map { entry ->
            listOf(
                null,
                null,
                null,
                if (entry.moneyIn > 0.0) entry.moneyIn else null,
                if (entry.moneyOut > 0.0) entry.moneyOut else null,
                entry.runningBalance
            )
        }
        val totals = listOf(
            ShopLocale.get("closing_balance_row", lang),
            "",
            "",
            ShopLocale.formatMoneyFixed2(rep.totalMoneyIn, lang),
            ShopLocale.formatMoneyFixed2(rep.totalMoneyOut, lang),
            ShopLocale.formatMoneyFixed2(rep.closingBalance, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, rep.totalMoneyIn, rep.totalMoneyOut, rep.closingBalance)
        val accDisplay = ShopLocale.displayAccountName(rep.account.name, rep.account.id, lang)
        val acctSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পূর্বের জের" else "Opening Balance",
                value = ShopLocale.formatMoneyFixed2(rep.openingBalance, lang)
            ),
            ReportSummaryMetric(
                label = ShopLocale.get("col_money_in", lang),
                value = ShopLocale.formatMoneyFixed2(rep.totalMoneyIn, lang)
            ),
            ReportSummaryMetric(
                label = ShopLocale.get("col_money_out", lang),
                value = ShopLocale.formatMoneyFixed2(rep.totalMoneyOut, lang)
            ),
            ReportSummaryMetric(
                label = ShopLocale.get("closing_balance_row", lang),
                value = ShopLocale.formatMoneyFixed2(rep.closingBalance, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.format("rep_account_stmt_title", lang, accDisplay),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_money_in", lang),
                ShopLocale.get("col_money_out", lang),
                ShopLocale.get("col_running_balance", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4, 5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = "${ShopLocale.format("opening_balance_note", lang, ShopLocale.formatMoneyFixed2(rep.openingBalance, lang))} • ${ShopLocale.get("closing_balance_row", lang)}: ${ShopLocale.formatMoneyFixed2(rep.closingBalance, lang)}",
            summaryMetrics = acctSummary
        )
    }

    fun buildStaffReportTable(
        staffSummary: StaffReportSummary,
        store: Store?,
        periodLabel: String,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val headers = if (canSeeProfit) {
            listOf(
                ShopLocale.get("col_staff_member", lang),
                ShopLocale.get("col_role", lang),
                ShopLocale.get("col_orders", lang),
                ShopLocale.get("col_qty_sold", lang),
                ShopLocale.get("col_sales_total", lang),
                ShopLocale.get("col_profit", lang),
                ShopLocale.get("col_payments_in", lang)
            )
        } else {
            listOf(
                ShopLocale.get("col_staff_member", lang),
                ShopLocale.get("col_role", lang),
                ShopLocale.get("col_orders", lang),
                ShopLocale.get("col_qty_sold", lang),
                ShopLocale.get("col_sales_total", lang),
                ShopLocale.get("col_payments_in", lang)
            )
        }
        val rows = staffSummary.rows.map { row ->
            val roleDisplay = ShopLocale.displayStaffRole(row.roleLabel, lang)
            val displayMember = if (row.memberName == "Owner") ShopLocale.get("role_owner", lang) else row.memberName
            if (canSeeProfit) {
                listOf(
                    displayMember,
                    roleDisplay,
                    ShopLocale.formatNumber(row.salesCount, lang),
                    ShopLocale.formatNumber(row.itemsSold, lang),
                    ShopLocale.formatMoney(row.totalSales, lang),
                    ShopLocale.formatMoney(row.profit, lang),
                    ShopLocale.formatMoney(row.paymentsReceived, lang)
                )
            } else {
                listOf(
                    displayMember,
                    roleDisplay,
                    ShopLocale.formatNumber(row.salesCount, lang),
                    ShopLocale.formatNumber(row.itemsSold, lang),
                    ShopLocale.formatMoney(row.totalSales, lang),
                    ShopLocale.formatMoney(row.paymentsReceived, lang)
                )
            }
        }
        val rawRows: List<List<Double?>> = staffSummary.rows.map { row ->
            if (canSeeProfit) {
                listOf(null, null, row.salesCount.toDouble(), row.itemsSold.toDouble(), row.totalSales, row.profit, row.paymentsReceived)
            } else {
                listOf(null, null, row.salesCount.toDouble(), row.itemsSold.toDouble(), row.totalSales, row.paymentsReceived)
            }
        }
        val totalStaffLabel = ShopLocale.format("total_staff_row", lang, staffSummary.rows.size)
        val totals = if (canSeeProfit) {
            listOf(
                totalStaffLabel,
                "",
                ShopLocale.formatNumber(staffSummary.totalSalesCount, lang),
                ShopLocale.formatNumber(staffSummary.totalItemsSold, lang),
                ShopLocale.formatMoney(staffSummary.totalSalesAmount, lang),
                ShopLocale.formatMoney(staffSummary.totalProfit, lang),
                ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)
            )
        } else {
            listOf(
                totalStaffLabel,
                "",
                ShopLocale.formatNumber(staffSummary.totalSalesCount, lang),
                ShopLocale.formatNumber(staffSummary.totalItemsSold, lang),
                ShopLocale.formatMoney(staffSummary.totalSalesAmount, lang),
                ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)
            )
        }
        val rawTotals = if (canSeeProfit) {
            listOf(null, null, staffSummary.totalSalesCount.toDouble(), staffSummary.totalItemsSold.toDouble(), staffSummary.totalSalesAmount, staffSummary.totalProfit, staffSummary.totalPaymentsReceived)
        } else {
            listOf(null, null, staffSummary.totalSalesCount.toDouble(), staffSummary.totalItemsSold.toDouble(), staffSummary.totalSalesAmount, staffSummary.totalPaymentsReceived)
        }
        val staffSummaryMetrics = buildList {
            add(
                ReportSummaryMetric(
                    label = ShopLocale.get("sales_recorded_lbl", lang),
                    value = ShopLocale.formatMoney(staffSummary.totalSalesAmount, lang),
                    detail = ShopLocale.format(
                        "staff_orders_items_val",
                        lang,
                        ShopLocale.formatNumber(staffSummary.totalSalesCount, lang),
                        ShopLocale.formatNumber(staffSummary.totalItemsSold, lang)
                    )
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = ShopLocale.get("profit_generated_lbl", lang),
                        value = ShopLocale.formatMoney(staffSummary.totalProfit, lang)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = ShopLocale.get("cust_payments_received_lbl", lang),
                    value = ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)
                )
            )
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট স্টাফ" else "Team Members",
                    value = ShopLocale.formatNumber(staffSummary.rows.size, lang)
                )
            )
        }
        return ReportTableData(
            title = ShopLocale.get("rep_staff_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = headers,
            rows = rows,
            numericColumns = if (canSeeProfit) setOf(2, 3, 4, 5, 6) else setOf(2, 3, 4, 5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (lang == AppLanguage.BN) {
                "মোট ${ShopLocale.formatNumber(staffSummary.rows.size, lang)} জন স্টাফ • মোট অর্ডার: ${ShopLocale.formatNumber(staffSummary.totalSalesCount, lang)} • ক্রেতার বাকি আদায়: ${ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)}"
            } else {
                "Active Team: ${staffSummary.rows.size} staff • Total Orders: ${staffSummary.totalSalesCount} • Customer Payments Collected: ${ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)}"
            },
            summaryMetrics = staffSummaryMetrics
        )
    }
}
