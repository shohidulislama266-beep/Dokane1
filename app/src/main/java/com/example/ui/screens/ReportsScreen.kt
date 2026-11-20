package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.HealthProblem
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.BusinessNavDestination
import com.example.ui.viewmodel.HistoryPeriod
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ReportDataBuilders
import com.example.util.ReportExportUtil
import com.example.util.ReportTableData
import com.example.util.ShopLocale
import java.util.Locale

private data class ReportTileItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String,
    val destination: BusinessNavDestination,
    val exportSlug: String,
    val shareTitle: String,
    val requiresCustomerSelection: Boolean = false,
    val requiresSupplierSelection: Boolean = false
)

@Composable
fun FinancialReportsHubScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val isOwner by storeViewModel.isOwner.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val canBaki by storeViewModel.canAccessCustomersAndBakiFlow.collectAsState()
    val canSuppliers by storeViewModel.canAccessSuppliersAndPurchasesFlow.collectAsState()
    val canStock by storeViewModel.canViewStockFlow.collectAsState()
    val canHistory by storeViewModel.canSeeHistoryFlow.collectAsState()
    val canExpenses by storeViewModel.canAccessExpensesAndIncomeFlow.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val isDemoMode = storeViewModel.isDemoMode
    val showProfitInSimpleMode by storeViewModel.showProfitInSimpleMode.collectAsState()
    val isRestrictedMonthly = isSimpleMode || isFatherMode
    val showMonthlyProfit = canSeeProfit && (!isRestrictedMonthly || showProfitInSimpleMode)

    val period by storeViewModel.businessPeriod.collectAsState()
    val customRange by storeViewModel.businessCustomDateRange.collectAsState()
    val store by storeViewModel.store.collectAsState()

    val monthYear by storeViewModel.monthlyReportYear.collectAsState()
    val monthIndex by storeViewModel.monthlyReportMonth.collectAsState()
    val monthlyReportData by storeViewModel.monthlyReportData.collectAsState()

    val filteredSales by storeViewModel.businessFilteredSales.collectAsState()
    val filteredPurchases by storeViewModel.businessFilteredPurchases.collectAsState()
    val filteredReturns by storeViewModel.businessFilteredReturns.collectAsState()
    val dayBookEntries by storeViewModel.dayBookEntries.collectAsState()
    val pnlStatement by storeViewModel.pnlStatement.collectAsState()
    val isPnlStockBased by storeViewModel.isPnlStockBased.collectAsState()
    val filteredExpenses by storeViewModel.businessFilteredExpenses.collectAsState()
    val filteredIncomes by storeViewModel.businessFilteredOtherIncomes.collectAsState()
    val products by storeViewModel.products.collectAsState()
    val customerStatementReport by storeViewModel.customerStatementReport.collectAsState()
    val supplierStatementReport by storeViewModel.supplierStatementReport.collectAsState()
    val staffReport by storeViewModel.staffReport.collectAsState()
    val staffPeriod by storeViewModel.historyPeriod.collectAsState()

    val healthProblems by storeViewModel.healthProblems.collectAsState()
    val lastHealthScanMillis by storeViewModel.lastHealthScanMillis.collectAsState()
    val isScanningHealth by storeViewModel.isScanningHealth.collectAsState()

    var exportLanguage by remember(currentLanguage) { mutableStateOf(currentLanguage) }
    var showDatePicker by remember { mutableStateOf(false) }

    BackHandler {
        storeViewModel.navigateBackInBusiness()
    }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            initialRange = customRange,
            currentLanguage = currentLanguage,
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                storeViewModel.setBusinessCustomDateRange(start, end)
                showDatePicker = false
            }
        )
    }

    val effectivePeriod = if (isSimpleMode && period == HistoryPeriod.CUSTOM) HistoryPeriod.TODAY else period
    val currentMonthLabel = ShopLocale.formatMonthYear(monthYear, monthIndex, currentLanguage)
    val safeStoreName = store?.name?.ifBlank { "StoreOS" } ?: "StoreOS"

    fun buildReportForTile(tile: ReportTileItem): ReportTableData? {
        return when (tile.id) {
            "sales" -> ReportDataBuilders.buildSalesReportTable(
                sales = filteredSales,
                store = store,
                periodLabel = if (currentLanguage == AppLanguage.BN) effectivePeriod.labelBn else effectivePeriod.labelEn,
                canSeeProfit = canSeeProfit,
                lang = currentLanguage
            )
            "daybook" -> ReportDataBuilders.buildDayBookReportTable(
                entries = dayBookEntries,
                store = store,
                periodLabel = if (currentLanguage == AppLanguage.BN) effectivePeriod.labelBn else effectivePeriod.labelEn,
                lang = currentLanguage
            )
            "pnl" -> ReportDataBuilders.buildProfitLossReportTable(
                pnl = pnlStatement,
                isStockBased = if (isFatherMode) true else isPnlStockBased,
                store = store,
                periodLabel = if (currentLanguage == AppLanguage.BN) effectivePeriod.labelBn else effectivePeriod.labelEn,
                lang = currentLanguage,
                isFatherMode = isFatherMode
            )
            "cust_statement" -> {
                val rep = customerStatementReport
                if (rep == null) {
                    val msg = if (currentLanguage == AppLanguage.BN) {
                        "বিবরণী দেখতে একজন ক্রেতা নির্বাচন করুন"
                    } else {
                        "Select a customer to view statement"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    storeViewModel.navigateInBusiness(BusinessNavDestination.REPORT_CUSTOMER_STATEMENT)
                    null
                } else {
                    ReportDataBuilders.buildCustomerStatementReportTable(
                        rep = rep,
                        store = store,
                        periodLabel = if (currentLanguage == AppLanguage.BN) effectivePeriod.labelBn else effectivePeriod.labelEn,
                        lang = currentLanguage
                    )
                }
            }
            "sup_statement" -> {
                val rep = supplierStatementReport
                if (rep == null) {
                    val msg = if (currentLanguage == AppLanguage.BN) {
                        "বিবরণী দেখতে একজন মহাজন নির্বাচন করুন"
                    } else {
                        "Select a supplier to view statement"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    storeViewModel.navigateInBusiness(BusinessNavDestination.REPORT_SUPPLIER_STATEMENT)
                    null
                } else {
                    ReportDataBuilders.buildSupplierStatementReportTable(
                        rep = rep,
                        store = store,
                        periodLabel = if (currentLanguage == AppLanguage.BN) effectivePeriod.labelBn else effectivePeriod.labelEn,
                        lang = currentLanguage
                    )
                }
            }
            "purchases" -> ReportDataBuilders.buildPurchaseReportTable(
                purchases = filteredPurchases,
                returns = filteredReturns,
                store = store,
                periodLabel = if (currentLanguage == AppLanguage.BN) effectivePeriod.labelBn else effectivePeriod.labelEn,
                lang = currentLanguage
            )
            "expenses" -> ReportDataBuilders.buildExpenseReportTable(
                expenses = filteredExpenses,
                store = store,
                periodLabel = if (currentLanguage == AppLanguage.BN) effectivePeriod.labelBn else effectivePeriod.labelEn,
                lang = currentLanguage
            )
            "incomes" -> ReportDataBuilders.buildOtherIncomeReportTable(
                incomes = filteredIncomes,
                store = store,
                periodLabel = if (currentLanguage == AppLanguage.BN) effectivePeriod.labelBn else effectivePeriod.labelEn,
                lang = currentLanguage
            )
            "inventory" -> ReportDataBuilders.buildInventoryReportTable(
                products = products,
                store = store,
                canSeeProfit = canSeeProfit,
                lang = currentLanguage
            )
            "staff" -> ReportDataBuilders.buildStaffReportTable(
                staffSummary = staffReport,
                store = store,
                periodLabel = if (currentLanguage == AppLanguage.BN) staffPeriod.labelBn else staffPeriod.labelEn,
                canSeeProfit = canSeeProfit,
                lang = currentLanguage
            )
            else -> null
        }
    }

    val isBn = currentLanguage == AppLanguage.BN
    val visibleReports = remember(
        canHistory,
        isSimpleMode,
        isFatherMode,
        isDemoMode,
        canSeeProfit,
        canBaki,
        canSuppliers,
        canExpenses,
        canStock,
        currentLanguage
    ) {
        buildList {
            if (canHistory) {
                add(
                    ReportTileItem(
                        id = "sales",
                        title = if (isBn) "বিক্রয় রিপোর্ট" else "Sales Report",
                        subtitle = if (isBn) "প্রতিটি বিক্রয়" else "every sale",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        testTag = "report_link_sales",
                        destination = BusinessNavDestination.REPORT_SALES,
                        exportSlug = "SalesReport",
                        shareTitle = if (isBn) "বিক্রয় রিপোর্ট" else "Sales Report"
                    )
                )
            }
            if (!isSimpleMode && !isFatherMode) {
                add(
                    ReportTileItem(
                        id = "daybook",
                        title = if (isBn) "দৈনিক খাতা" else "Day Book",
                        subtitle = if (isBn) "প্রতিদিনের হিসাব" else "daily entries",
                        icon = Icons.Default.Book,
                        testTag = "report_link_daybook",
                        destination = BusinessNavDestination.REPORT_DAY_BOOK,
                        exportSlug = "DayBook",
                        shareTitle = if (isBn) "দৈনিক খাতা" else "Day Book"
                    )
                )
            }
            if (canSeeProfit && (!isSimpleMode || isFatherMode)) {
                add(
                    ReportTileItem(
                        id = "pnl",
                        title = if (isBn) "লাভ-ক্ষতি" else "Profit & Loss",
                        subtitle = if (isBn) "ক্রয়মূল্য ও লাভ" else "buying cost & profit",
                        icon = Icons.Default.PieChart,
                        testTag = "report_link_pnl",
                        destination = BusinessNavDestination.REPORT_PROFIT_LOSS,
                        exportSlug = "ProfitLoss",
                        shareTitle = if (isBn) "লাভ ও ক্ষতি" else "Profit and Loss"
                    )
                )
            }
            if (canBaki && !isSimpleMode && !isFatherMode) {
                add(
                    ReportTileItem(
                        id = "cust_statement",
                        title = if (isBn) "ক্রেতা বিবরণী" else "Customer Statement",
                        subtitle = if (isBn) "ক্রেতা বাছুন" else "pick a customer",
                        icon = Icons.Default.People,
                        testTag = "report_link_cust_statement",
                        destination = BusinessNavDestination.REPORT_CUSTOMER_STATEMENT,
                        exportSlug = "CustomerStatement",
                        shareTitle = if (isBn) "ক্রেতা বিবরণী" else "Customer Statement",
                        requiresCustomerSelection = true
                    )
                )
            }
            if (canSuppliers && !isSimpleMode && !isFatherMode) {
                add(
                    ReportTileItem(
                        id = "sup_statement",
                        title = if (isBn) "মহাজন বিবরণী" else "Supplier Statement",
                        subtitle = if (isBn) "মহাজন বাছুন" else "pick a supplier",
                        icon = Icons.Default.LocalShipping,
                        testTag = "report_link_sup_statement",
                        destination = BusinessNavDestination.REPORT_SUPPLIER_STATEMENT,
                        exportSlug = "SupplierStatement",
                        shareTitle = if (isBn) "মহাজন বিবরণী" else "Supplier Statement",
                        requiresSupplierSelection = true
                    )
                )
            }
            if (canSuppliers && !isFatherMode) {
                add(
                    ReportTileItem(
                        id = "purchases",
                        title = if (isBn) "ক্রয় রিপোর্ট" else "Purchase Report",
                        subtitle = if (isBn) "অর্ডার ও দেনা" else "orders & dues",
                        icon = Icons.Default.ShoppingBag,
                        testTag = "report_link_purchases",
                        destination = BusinessNavDestination.REPORT_PURCHASES,
                        exportSlug = "PurchaseReport",
                        shareTitle = if (isBn) "ক্রয় রিপোর্ট" else "Purchase Report"
                    )
                )
            }
            if (canExpenses && !isSimpleMode && !isFatherMode) {
                add(
                    ReportTileItem(
                        id = "expenses",
                        title = if (isBn) "খরচ রিপোর্ট" else "Expense Report",
                        subtitle = if (isBn) "দোকানের খরচ" else "operating costs",
                        icon = Icons.Default.Receipt,
                        testTag = "report_link_expense_report",
                        destination = BusinessNavDestination.REPORT_EXPENSES,
                        exportSlug = "ExpenseReport",
                        shareTitle = if (isBn) "খরচ রিপোর্ট" else "Expense Report"
                    )
                )
                add(
                    ReportTileItem(
                        id = "incomes",
                        title = if (isBn) "অন্যান্য আয়" else "Other Income",
                        subtitle = if (isBn) "কমিশন ও আয়" else "commissions",
                        icon = Icons.Default.AttachMoney,
                        testTag = "report_link_income_report",
                        destination = BusinessNavDestination.REPORT_INCOMES,
                        exportSlug = "OtherIncomeReport",
                        shareTitle = if (isBn) "অন্যান্য আয়" else "Other Income Report"
                    )
                )
            }
            if (canStock) {
                add(
                    ReportTileItem(
                        id = "inventory",
                        title = if (isBn) "মজুদ মূল্য" else "Inventory Valuation",
                        subtitle = if (canSeeProfit) {
                            if (isBn) "ক্রয়মূল্যে মজুদ" else "stock at cost"
                        } else {
                            if (isBn) "মজুদ ও দাম" else "stock & price"
                        },
                        icon = Icons.Default.Inventory2,
                        testTag = "report_link_inventory",
                        destination = BusinessNavDestination.REPORT_INVENTORY,
                        exportSlug = "InventoryReport",
                        shareTitle = if (isBn) "মজুদ রিপোর্ট" else "Inventory Report"
                    )
                )
            }
            if (!isFatherMode && !isSimpleMode && !isDemoMode) {
                add(
                    ReportTileItem(
                        id = "staff",
                        title = if (isBn) "কর্মী রিপোর্ট" else "Staff Report",
                        subtitle = if (isBn) "কে কত বিক্রি করেছে" else "who sold what",
                        icon = Icons.Default.Badge,
                        testTag = "report_link_staff",
                        destination = BusinessNavDestination.REPORT_STAFF,
                        exportSlug = "StaffReport",
                        shareTitle = if (isBn) "কর্মী রিপোর্ট" else "Staff Report"
                    )
                )
            }
        }
    }

    val reportRows = remember(visibleReports) { visibleReports.chunked(2) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070907))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.25f, size.height * 0.08f),
                        radius = size.width * 0.85f
                    )
                )
            }
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .testTag("reports_hub_list"),
        contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header: Back Arrow + "Reports"
        item(key = "financial_reports_hub_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("financial_reports_hub_screen"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { storeViewModel.navigateBackInBusiness() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("financial_reports_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isBn) "পিছনে" else "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isBn) "আর্থিক রিপোর্ট" else "Reports",
                    color = TextPrimary,
                    fontSize = if (isFatherMode) 22.sp else 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
            // 1. Glowing Monthly / Business Report Hero Card
            if (canSeeProfit || isRestrictedMonthly) {
                item {
                    val resolvedMonthlyLang = if (isRestrictedMonthly) AppLanguage.BN else exportLanguage
                    MonthlyReportHeroCard(
                        monthLabel = currentMonthLabel,
                        currentLanguage = currentLanguage,
                        exportLanguage = resolvedMonthlyLang,
                        isFatherMode = isRestrictedMonthly,
                        onSelectLanguage = { exportLanguage = it },
                        onView = {
                            storeViewModel.navigateInBusiness(BusinessNavDestination.MONTHLY_REPORT)
                        },
                        onPdf = {
                            try {
                                val table = ReportDataBuilders.buildMonthlyReportTable(
                                    reportData = monthlyReportData,
                                    year = monthYear,
                                    month = monthIndex,
                                    store = store,
                                    canSeeProfit = showMonthlyProfit,
                                    lang = resolvedMonthlyLang,
                                    includeSupplierDebt = !isRestrictedMonthly,
                                    isRestrictedMode = isRestrictedMonthly
                                )
                                val suffix = if (resolvedMonthlyLang == AppLanguage.BN) "BN" else "EN"
                                val file = ReportExportUtil.generatePdfReport(
                                    context,
                                    table,
                                    "MonthlyReport_${monthYear}_${monthIndex + 1}_$suffix",
                                    resolvedMonthlyLang
                                )
                                ReportExportUtil.downloadReportFile(context, file, resolvedMonthlyLang)
                            } catch (e: Exception) {
                                val err = if (isBn) "পিডিএফ এক্সপোর্ট করা যায়নি" else (e.localizedMessage ?: "PDF export unavailable")
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onExcel = {
                            try {
                                val table = ReportDataBuilders.buildMonthlyReportTable(
                                    reportData = monthlyReportData,
                                    year = monthYear,
                                    month = monthIndex,
                                    store = store,
                                    canSeeProfit = showMonthlyProfit,
                                    lang = resolvedMonthlyLang,
                                    includeSupplierDebt = !isRestrictedMonthly,
                                    isRestrictedMode = isRestrictedMonthly
                                )
                                val suffix = if (resolvedMonthlyLang == AppLanguage.BN) "BN" else "EN"
                                val file = ReportExportUtil.generateCsvReport(
                                    context,
                                    table,
                                    "MonthlyReport_${monthYear}_${monthIndex + 1}_$suffix",
                                    resolvedMonthlyLang
                                )
                                ReportExportUtil.downloadReportFile(context, file, resolvedMonthlyLang)
                            } catch (e: Exception) {
                                val err = if (isBn) "হিসাব ফাইল এক্সপোর্ট করা যায়নি" else (e.localizedMessage ?: "Excel export unavailable")
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShare = {
                            try {
                                val table = ReportDataBuilders.buildMonthlyReportTable(
                                    reportData = monthlyReportData,
                                    year = monthYear,
                                    month = monthIndex,
                                    store = store,
                                    canSeeProfit = showMonthlyProfit,
                                    lang = resolvedMonthlyLang,
                                    includeSupplierDebt = !isRestrictedMonthly,
                                    isRestrictedMode = isRestrictedMonthly
                                )
                                val suffix = if (resolvedMonthlyLang == AppLanguage.BN) "BN" else "EN"
                                val file = ReportExportUtil.generatePdfReport(
                                    context,
                                    table,
                                    "MonthlyReport_${monthYear}_${monthIndex + 1}_$suffix",
                                    resolvedMonthlyLang
                                )
                                val reportPrefix = if (resolvedMonthlyLang == AppLanguage.BN) "ব্যবসার রিপোর্ট" else "Business Report"
                                val cleanStoreName = if (isRestrictedMonthly) {
                                    ShopLocale.plainBanglaText(safeStoreName, "দোকান")
                                } else safeStoreName
                                ReportExportUtil.shareReportFile(
                                    context,
                                    file,
                                    "application/pdf",
                                    "$reportPrefix - $cleanStoreName",
                                    resolvedMonthlyLang
                                )
                            } catch (e: Exception) {
                                val err = if (isBn) "শেয়ার করা যায়নি" else (e.localizedMessage ?: "Share unavailable")
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // 2. Period Capsule Bar
            item {
                HistoryPeriodTabs(
                    currentPeriod = effectivePeriod,
                    customRange = customRange,
                    currentLanguage = currentLanguage,
                    includeCustomDate = !isSimpleMode && !isFatherMode,
                    onSelectPeriod = { selected ->
                        if (selected == HistoryPeriod.CUSTOM) {
                            showDatePicker = true
                        } else {
                            storeViewModel.setBusinessPeriod(selected)
                        }
                    }
                )
            }

            // 3. 2-Column Grid of Real Existing Reports
            items(reportRows) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        ReportGridTile(
                            item = item,
                            currentLanguage = currentLanguage,
                            isFatherMode = isFatherMode,
                            onOpen = { storeViewModel.navigateInBusiness(item.destination) },
                            onDownload = {
                                try {
                                    val data = buildReportForTile(item) ?: return@ReportGridTile
                                    val file = ReportExportUtil.generatePdfReport(
                                        context,
                                        data,
                                        "${item.exportSlug}_${System.currentTimeMillis()}",
                                        currentLanguage
                                    )
                                    ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                                } catch (e: Exception) {
                                    val err = if (isBn) "ডাউনলোড ব্যর্থ হয়েছে" else (e.localizedMessage ?: "Download failed")
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShare = {
                                try {
                                    val data = buildReportForTile(item) ?: return@ReportGridTile
                                    val file = ReportExportUtil.generatePdfReport(
                                        context,
                                        data,
                                        "${item.exportSlug}_${System.currentTimeMillis()}",
                                        currentLanguage
                                    )
                                    ReportExportUtil.shareReportFile(
                                        context,
                                        file,
                                        "application/pdf",
                                        "${item.shareTitle} - $safeStoreName",
                                        currentLanguage
                                    )
                                } catch (e: Exception) {
                                    val err = if (isBn) "শেয়ার করা যায়নি" else (e.localizedMessage ?: "Share unavailable")
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 4. Bottom Data Health Check Bar (Owner & Detailed Mode only, hidden in Father mode)
            if (isOwner && !isSimpleMode && !isFatherMode) {
                item {
                    DataHealthCheckBar(
                        problems = healthProblems,
                        isScanning = isScanningHealth,
                        lastScanMillis = lastHealthScanMillis,
                        currentLanguage = currentLanguage,
                        onOpenDetails = {
                            storeViewModel.navigateInBusiness(BusinessNavDestination.DATA_HEALTH_CHECK)
                        },
                        onScan = {
                            storeViewModel.scanDataHealth()
                        }
                    )
                }
        }
    }
}

@Composable
private fun MonthlyReportHeroCard(
    monthLabel: String,
    currentLanguage: AppLanguage,
    exportLanguage: AppLanguage,
    isFatherMode: Boolean = false,
    onSelectLanguage: (AppLanguage) -> Unit,
    onView: () -> Unit,
    onPdf: () -> Unit,
    onExcel: () -> Unit,
    onShare: () -> Unit
) {
    val isBn = currentLanguage == AppLanguage.BN
    val heroShape = RoundedCornerShape(20.dp)
    Surface(
        shape = heroShape,
        color = Color.Transparent,
        border = BorderStroke(1.4.dp, VividGreen.copy(alpha = 0.78f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 16.dp,
                shape = heroShape,
                spotColor = VividGreen.copy(alpha = 0.42f),
                ambientColor = VividGreen.copy(alpha = 0.22f)
            )
            .clip(heroShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF234715), Color(0xFF0E160B))
                )
            )
            .testTag("reports_monthly_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isFatherMode) 42.dp else 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VividGreen.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = VividGreen,
                        modifier = Modifier.size(if (isFatherMode) 22.dp else 18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) "মাসিক রিপোর্ট · $monthLabel" else "Monthly Report · $monthLabel",
                        color = TextPrimary,
                        fontSize = if (isFatherMode) 16.sp else 14.5.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isBn) "এক ট্যাপে দেখুন, PDF বা Excel নিন" else "One tap: view in app, PDF or Excel",
                        color = Color(0xFFD6E2D2),
                        fontSize = if (isFatherMode) 13.sp else 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroActionPill(
                        label = if (isBn) "দেখুন" else "View",
                        testTag = "reports_monthly_view_btn",
                        onClick = onView,
                        isFatherMode = isFatherMode
                    )
                    HeroActionPill(
                        label = "PDF",
                        testTag = "reports_monthly_pdf_btn",
                        onClick = onPdf,
                        isFatherMode = isFatherMode
                    )
                    HeroActionPill(
                        label = "Excel",
                        testTag = "reports_monthly_excel_btn",
                        onClick = onExcel,
                        isFatherMode = isFatherMode
                    )
                    HeroActionPill(
                        label = if (isBn) "শেয়ার" else "Share",
                        testTag = "reports_monthly_share_btn",
                        onClick = onShare,
                        isFatherMode = isFatherMode
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // EN | BN Toggle
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xCC121612),
                        border = BorderStroke(1.dp, Color(0x30FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val enSelected = exportLanguage == AppLanguage.EN
                            val bnSelected = exportLanguage == AppLanguage.BN
                            Surface(
                                onClick = { onSelectLanguage(AppLanguage.EN) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (enSelected) Color(0xFF283226) else Color.Transparent,
                                modifier = Modifier.testTag("reports_lang_en")
                            ) {
                                Text(
                                    text = "EN",
                                    color = if (enSelected) VividGreen else TextSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (enSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                onClick = { onSelectLanguage(AppLanguage.BN) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (bnSelected) Color(0xFF283226) else Color.Transparent,
                                modifier = Modifier.testTag("reports_lang_bn")
                            ) {
                                Text(
                                    text = "BN",
                                    color = if (bnSelected) VividGreen else TextSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (bnSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroActionPill(
    label: String,
    testTag: String,
    onClick: () -> Unit,
    isFatherMode: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xCC151B14),
        border = BorderStroke(1.dp, Color(0x40FFFFFF)),
        modifier = Modifier
            .defaultMinSize(minHeight = if (isFatherMode) 44.dp else 34.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = if (isFatherMode) 14.dp else 11.dp,
                vertical = if (isFatherMode) 10.dp else 7.dp
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = TextPrimary,
                fontSize = if (isFatherMode) 13.5.sp else 11.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ReportGridTile(
    item: ReportTileItem,
    currentLanguage: AppLanguage,
    isFatherMode: Boolean = false,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn = currentLanguage == AppLanguage.BN
    val tileShape = RoundedCornerShape(18.dp)
    val bgBrush = Brush.verticalGradient(
        colors = listOf(Color(0xEB222622), Color(0xEE121412))
    )
    val borderBrush = Brush.verticalGradient(
        colors = listOf(Color(0x3DFFFFFF), Color(0x14FFFFFF))
    )

    Surface(
        onClick = onOpen,
        shape = tileShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderBrush),
        modifier = modifier
            .defaultMinSize(minHeight = if (isFatherMode) 128.dp else 114.dp)
            .clip(tileShape)
            .background(bgBrush)
            .testTag(item.testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Glowing Green Circle Icon + Download & Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = VividGreen.copy(alpha = 0.14f),
                    border = BorderStroke(1.2.dp, VividGreen.copy(alpha = 0.72f)),
                    modifier = Modifier.size(if (isFatherMode) 36.dp else 32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = VividGreen,
                            modifier = Modifier.size(if (isFatherMode) 18.dp else 15.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDownload)
                            .testTag("${item.testTag}_download"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = if (isBn) "${item.title} ডাউনলোড" else "Download ${item.title}",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onShare)
                            .testTag("${item.testTag}_share"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = if (isBn) "${item.title} শেয়ার" else "Share ${item.title}",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title & Bottom Subtitle + Chevron
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = if (isFatherMode) 14.5.sp else 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.subtitle,
                        color = TextMuted,
                        fontSize = if (isFatherMode) 11.5.sp else 10.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DataHealthCheckBar(
    problems: List<HealthProblem>?,
    isScanning: Boolean,
    lastScanMillis: Long?,
    currentLanguage: AppLanguage,
    onOpenDetails: () -> Unit,
    onScan: () -> Unit
) {
    val isBn = currentLanguage == AppLanguage.BN
    val hasScanned = lastScanMillis != null && lastScanMillis > 0L
    val barShape = RoundedCornerShape(18.dp)
    val hasIssues = hasScanned && problems != null && problems.isNotEmpty()
    val isHealthy = hasScanned && problems != null && problems.isEmpty()
    val amberTint = Color(0xFFFFB300)
    val accentColor = if (hasIssues) amberTint else VividGreen

    val statusText = when {
        isScanning -> if (isBn) "হিসাব যাচাই চলছে..." else "Scanning records..."
        !hasScanned -> if (isBn) "হিসাব যাচাই করতে স্ক্যান করুন" else "Tap Scan to verify records"
        isHealthy -> {
            val ago = ShopLocale.formatTimeAgo(lastScanMillis, currentLanguage)
            if (isBn) "সব ঠিক আছে · $ago" else "All checks passed · $ago"
        }
        else -> {
            val count = ShopLocale.formatNumber((problems?.size ?: 0).toLong(), currentLanguage)
            if (isBn) "$count টি অসঙ্গতি পাওয়া গেছে" else "$count issue${if ((problems?.size ?: 0) == 1) "" else "s"} found"
        }
    }

    Surface(
        shape = barShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(barShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF172714), Color(0xFF101610))
                )
            )
            .testTag("reports_data_health_bar")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onOpenDetails)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = when {
                                hasIssues -> Icons.Default.Warning
                                isHealthy -> Icons.Default.Check
                                else -> Icons.Default.HealthAndSafety
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) "ডেটা হেলথ চেক" else "Data Health Check",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusText,
                        color = if (hasIssues) amberTint else Color(0xFFBBD0B7),
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = onScan,
                    enabled = !isScanning,
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xCC1E241E),
                    border = BorderStroke(1.dp, Color(0x38FFFFFF)),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .testTag("reports_health_scan_btn")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isScanning) "..." else if (isBn) "স্ক্যান" else "Scan",
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
