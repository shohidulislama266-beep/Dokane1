package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.Purchase
import com.example.data.model.Supplier
import com.example.domain.CalculationEngine
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.ExportActionBar
import com.example.ui.components.GlassCard
import com.example.ui.components.PlainEmptyState
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.HistoryPeriod
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ReportExportUtil
import com.example.util.ReportTableData
import com.example.util.ShopLocale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =================================================================
// 1. SALES REPORT
// =================================================================
@Composable
fun SalesReportScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val period by storeViewModel.businessPeriod.collectAsState()
    val customRange by storeViewModel.businessCustomDateRange.collectAsState()
    val sales by storeViewModel.businessFilteredSales.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()

    val totalAmount = remember(sales) { sales.sumOf { it.grandTotal } }
    val totalDiscount = remember(sales) { sales.sumOf { it.discount } }
    val totalProfit = remember(sales) { sales.sumOf { it.profit } }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            initialRange = customRange,
            currentLanguage = currentLanguage,
            onDismiss = { showDatePicker = false },
            onConfirm = { s, e ->
                storeViewModel.setBusinessCustomDateRange(s, e)
                showDatePicker = false
            }
        )
    }

    fun buildReportData(): ReportTableData =
        com.example.util.ReportDataBuilders.buildSalesReportTable(
            sales = sales,
            store = store,
            periodLabel = period.labelEn,
            canSeeProfit = canSeeProfit,
            lang = currentLanguage
        )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item(key = "sales_report_header") {
            ReportTopBar(
                title = if (currentLanguage == AppLanguage.BN) "বিক্রয় রিপোর্ট" else "Sales Report",
                onBack = { storeViewModel.navigateBackInBusiness() }
            )

            HistoryPeriodTabs(
                currentPeriod = period,
                customRange = customRange,
                currentLanguage = currentLanguage,
                onSelectPeriod = {
                    if (it == HistoryPeriod.CUSTOM) showDatePicker = true
                    else storeViewModel.setBusinessPeriod(it)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Step 4: 4 Export buttons strictly on ONE line
            ExportActionBar(
                onDownloadPdf = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "SalesReport_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                },
                onPrint = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "SalesReport_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.printReportFile(context, file, "SalesReport", currentLanguage)
                },
                onExportCsv = {
                    val file = ReportExportUtil.generateCsvReport(context, buildReportData(), "SalesReport_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                },
                onShare = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "SalesReport_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.shareReportFile(context, file, "application/pdf", "Sales Report - ${store?.name}", currentLanguage)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (sales.isEmpty()) {
            item(key = "sales_report_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    PlainEmptyState(
                        message = if (currentLanguage == AppLanguage.BN) "এই সময়ে কোনো বিক্রি নেই" else "No sales found for the chosen period"
                    )
                }
            }
        } else {
            item(key = "sales_report_table_header") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ReportTableHeaderRow(
                        headers = if (canSeeProfit) {
                            listOf(
                                ShopLocale.get("col_invoice_date", currentLanguage),
                                ShopLocale.get("col_customer", currentLanguage),
                                ShopLocale.get("col_total", currentLanguage),
                                ShopLocale.get("col_profit", currentLanguage)
                            )
                        } else {
                            listOf(
                                ShopLocale.get("col_invoice_date", currentLanguage),
                                ShopLocale.get("col_customer", currentLanguage),
                                ShopLocale.get("col_total", currentLanguage)
                            )
                        }
                    )
                }
            }

            items(sales) { s ->
                val dateStr = s.createdAt?.toDate()?.time?.let { ShopLocale.formatDateShort(it, currentLanguage) }
                    ?: if (currentLanguage == AppLanguage.BN) "এইমাত্র" else "Just now"
                val isBaki = s.paymentType == "baki"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    color = Color(0x0DFFFFFF),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.1f)) {
                            Text(
                                text = s.invoiceNo,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dateStr,
                                color = TextMuted,
                                fontSize = 10.5.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = ShopLocale.displayCustomerName(s.customerName, currentLanguage),
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isBaki) {
                                    if (currentLanguage == AppLanguage.BN) "বাকি" else "Baki (Credit)"
                                } else {
                                    if (currentLanguage == AppLanguage.BN) "নগদ" else "Cash Paid"
                                },
                                color = if (isBaki) Color(0xFFFFB300) else VividGreen,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = ShopLocale.formatMoney(s.grandTotal, currentLanguage),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                        if (canSeeProfit) {
                            Text(
                                text = ShopLocale.formatMoney(s.profit, currentLanguage),
                                color = VividGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item(key = "sales_report_totals") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        val isBn = currentLanguage == AppLanguage.BN
                        ReportTableTotalsBar(
                            label = if (isBn)
                                "মোট (${ShopLocale.formatNumber(sales.size, currentLanguage)}টি বিল)"
                            else
                                "TOTAL (${ShopLocale.formatNumber(sales.size, currentLanguage)} Orders)",
                            subValue = if (isBn)
                                "ছাড়: ${ShopLocale.formatMoney(totalDiscount, currentLanguage)}"
                            else
                                "Discounts: ${ShopLocale.formatMoney(totalDiscount, currentLanguage)}",
                            mainValue = ShopLocale.formatMoney(totalAmount, currentLanguage),
                            profitValue = if (canSeeProfit) {
                                "${ShopLocale.get("col_profit", currentLanguage)}: ${ShopLocale.formatMoney(totalProfit, currentLanguage)}"
                            } else ""
                        )
                    }
                }
            }
        }
    }
}

// =================================================================
// 2. PURCHASE REPORT (Restyled StoreOS Dark Glass Purchases Screen)
// =================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PurchaseReportScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val period by storeViewModel.businessPeriod.collectAsState()
    val customRange by storeViewModel.businessCustomDateRange.collectAsState()
    val purchases by storeViewModel.businessFilteredPurchases.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val canSuppliers by storeViewModel.canAccessSuppliersAndPurchasesFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showBuyStockDialog by remember { mutableStateOf(false) }
    var selectedPurchaseForDetails by remember { mutableStateOf<Purchase?>(null) }

    val filteredPurchases = remember(purchases, searchQuery) {
        val sorted = purchases.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) sorted
        else sorted.filter { p ->
            p.purchaseNo.lowercase().contains(q) ||
                p.supplierName.lowercase().contains(q) ||
                p.note.lowercase().contains(q) ||
                p.items.any { it.name.lowercase().contains(q) }
        }
    }

    val totalPurchases = remember(filteredPurchases) { filteredPurchases.sumOf { it.totalAmount } }
    val totalPaid = remember(filteredPurchases) { filteredPurchases.sumOf { it.paidAmount } }
    val totalDue = remember(filteredPurchases) { filteredPurchases.sumOf { it.dueAmount } }

    val groupedByDay = remember(filteredPurchases, currentLanguage) {
        filteredPurchases.groupBy { p ->
            p.createdAt?.toDate()?.time?.let { ShopLocale.formatWeekdayDate(it, currentLanguage) }
                ?: if (currentLanguage == AppLanguage.BN) "সাম্প্রতিক" else "Recent"
        }
    }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            initialRange = customRange,
            currentLanguage = currentLanguage,
            onDismiss = { showDatePicker = false },
            onConfirm = { s, e ->
                storeViewModel.setBusinessCustomDateRange(s, e)
                showDatePicker = false
            }
        )
    }

    if (showBuyStockDialog) {
        HomeRecordPurchaseDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { showBuyStockDialog = false }
        )
    }

    if (selectedPurchaseForDetails != null) {
        PurchaseDetailsDialog(
            purchase = selectedPurchaseForDetails!!,
            currentLanguage = currentLanguage,
            onDismiss = { selectedPurchaseForDetails = null }
        )
    }

    fun buildReportData(): ReportTableData =
        com.example.util.ReportDataBuilders.buildPurchaseReportTable(
            purchases = filteredPurchases,
            returns = emptyList(),
            store = store,
            periodLabel = period.labelEn,
            lang = currentLanguage
        )

    BackHandler {
        storeViewModel.navigateBackInBusiness()
    }

    val amberAccent = Color(0xFFFFB300)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070907))
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.28f, size.height * 0.12f),
                        radius = size.width * 0.78f
                    )
                )
            }
            .testTag("purchases_report_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header Row
            item(key = "purchase_report_top_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { storeViewModel.navigateBackInBusiness() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("report_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "ক্রয় হিসাব" else "Purchases",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (canSuppliers) {
                        val buyShape = RoundedCornerShape(22.dp)
                        Surface(
                            onClick = { showBuyStockDialog = true },
                            shape = buyShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color(0x88FFFFFF)),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = buyShape,
                                    spotColor = VividGreen.copy(alpha = 0.6f),
                                    ambientColor = VividGreen.copy(alpha = 0.4f)
                                )
                                .defaultMinSize(minHeight = 38.dp)
                                .testTag("purchases_buy_stock_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF7CEF4F), VividGreen)
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF0A1406),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "স্টক কিনুন" else "Buy Stock",
                                    color = Color(0xFF0A1406),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Period Filter Pills
            item {
                    HistoryPeriodTabs(
                        currentPeriod = period,
                        customRange = customRange,
                        currentLanguage = currentLanguage,
                        onSelectPeriod = {
                            if (it == HistoryPeriod.CUSTOM) showDatePicker = true
                            else storeViewModel.setBusinessPeriod(it)
                        }
                    )
                }

                // Glowing StoreOS Purchases Hero Card
                item {
                    val heroShape = RoundedCornerShape(22.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .shadow(
                                elevation = 14.dp,
                                shape = heroShape,
                                ambientColor = VividGreen.copy(alpha = 0.30f),
                                spotColor = VividGreen.copy(alpha = 0.42f)
                            )
                            .testTag("purchases_summary_card"),
                        shape = heroShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(VividGreen.copy(alpha = 0.88f), VividGreen.copy(alpha = 0.34f))
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF1F3816),
                                            Color(0xFF141D12),
                                            Color(0xFF101310)
                                        )
                                    )
                                )
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "মোট ক্রয়" else "TOTAL PURCHASES",
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.1.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = ShopLocale.formatMoney(totalPurchases, currentLanguage),
                                color = VividGreen,
                                fontSize = 31.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0x245DD62C),
                                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = "${if (currentLanguage == AppLanguage.BN) "নগদ" else "Paid"} ${ShopLocale.formatMoney(totalPaid, currentLanguage)}",
                                        color = VividGreen,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0x28453108),
                                    border = BorderStroke(1.dp, amberAccent.copy(alpha = 0.42f))
                                ) {
                                    Text(
                                        text = "${if (currentLanguage == AppLanguage.BN) "দেব" else "Due"} ${ShopLocale.formatMoney(totalDue, currentLanguage)}",
                                        color = amberAccent,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0x1CFFFFFF),
                                    border = BorderStroke(1.dp, Color(0x24FFFFFF))
                                ) {
                                    Text(
                                        text = "${ShopLocale.formatNumber(filteredPurchases.size.toLong(), currentLanguage)} ${if (filteredPurchases.size == 1) "order" else "orders"}",
                                        color = TextPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Pill Search Bar
                item {
                    val searchShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(searchShape)
                            .background(if (isSearchFocused) Color(0xD91A1E1A) else Color(0xBF151815))
                            .border(
                                width = 1.dp,
                                color = if (isSearchFocused) VividGreen.copy(alpha = 0.72f) else Color(0x28FFFFFF),
                                shape = searchShape
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = if (isSearchFocused) VividGreen else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) {
                                            "অর্ডার নং, মহাজন বা পণ্য খুঁজুন..."
                                        } else {
                                            "Search order #, supplier or item..."
                                        },
                                        color = TextMuted,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    cursorBrush = SolidColor(VividGreen),
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 13.5.sp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isSearchFocused = it.isFocused }
                                        .testTag("purchases_search_input")
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Export Bar
                item {
                    ExportActionBar(
                        onDownloadPdf = {
                            val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "PurchaseReport_${System.currentTimeMillis()}", currentLanguage)
                            ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                        },
                        onPrint = {
                            val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "PurchaseReport_${System.currentTimeMillis()}", currentLanguage)
                            ReportExportUtil.printReportFile(context, file, "PurchaseReport", currentLanguage)
                        },
                        onExportCsv = {
                            val file = ReportExportUtil.generateCsvReport(context, buildReportData(), "PurchaseReport_${System.currentTimeMillis()}", currentLanguage)
                            ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                        },
                        onShare = {
                            val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "PurchaseReport_${System.currentTimeMillis()}", currentLanguage)
                            ReportExportUtil.shareReportFile(context, file, "application/pdf", "Purchase Report - ${store?.name}", currentLanguage)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Purchases Feed
                if (filteredPurchases.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        "No matching purchases found"
                                    } else {
                                        "No purchases recorded in this period"
                                    },
                                    color = TextSecondary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    groupedByDay.forEach { (dayLabel, dayPurchases) ->
                        item(key = "purchase_day_hdr_$dayLabel") {
                            val daySum = dayPurchases.sumOf { it.totalAmount }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dayLabel,
                                    color = TextSecondary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ShopLocale.formatMoney(daySum, currentLanguage),
                                    color = amberAccent,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        items(dayPurchases, key = { it.id.ifBlank { it.purchaseNo } }) { p ->
                            val isCredit = p.paymentType == "credit" || p.dueAmount > 0.01
                            val formattedTime = remember(p.createdAt, currentLanguage) {
                                p.createdAt?.toDate()?.time?.let { ShopLocale.formatTimeOnly(it, currentLanguage) } ?: "—"
                            }
                            val displaySupplier = p.supplierName.ifBlank { p.purchaseNo }
                            val initial = displaySupplier.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "P"
                            val itemsSummary = remember(p.items, p.totalItems, formattedTime, currentLanguage) {
                                val first = p.items.firstOrNull()
                                when {
                                    first != null -> {
                                        val extra = if (p.items.size > 1) " +${ShopLocale.formatNumber(p.items.size - 1, currentLanguage)}" else ""
                                        "${first.name} ×${ShopLocale.formatNumber(first.qty, currentLanguage)} ${ShopLocale.displayUnit(first.unit, currentLanguage)}$extra · $formattedTime"
                                    }
                                    p.totalItems > 0.0 -> {
                                        val isSingle = kotlin.math.abs(p.totalItems - 1.0) < 1e-6
                                        val unitLabel = if (currentLanguage == AppLanguage.BN) "টি মাল" else if (isSingle) "item" else "items"
                                        "${ShopLocale.formatNumber(p.totalItems, currentLanguage)} $unitLabel · $formattedTime"
                                    }
                                    else -> formattedTime
                                }
                            }
                            val cardShape = RoundedCornerShape(18.dp)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(cardShape)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0xEB222522), Color(0xEC141614))
                                        )
                                    )
                                    .clickable { selectedPurchaseForDetails = p }
                                    .testTag("purchase_row_${p.purchaseNo}"),
                                shape = cardShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, Color(0x28FFFFFF))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(Color(0xFF2B2F2B), Color(0xFF191C19))
                                                )
                                            )
                                            .border(1.dp, Color(0x30FFFFFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initial,
                                            color = TextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = displaySupplier,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${p.purchaseNo} · ${if (p.accountName.isNotBlank()) p.accountName else "Purchase"}",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = itemsSummary,
                                            color = TextMuted,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = ShopLocale.formatMoney(p.totalAmount, currentLanguage),
                                            color = TextPrimary,
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        val statusColor = if (isCredit) amberAccent else VividGreen
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = statusColor.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.65f))
                                        ) {
                                            Text(
                                                text = if (isCredit) {
                                                    "Due ${ShopLocale.formatMoney(p.dueAmount, currentLanguage)}"
                                                } else {
                                                    if (currentLanguage == AppLanguage.BN) "পরিশোধিত" else "Paid"
                                                },
                                                color = statusColor,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }
    }
}

// =================================================================
// 3. DAY BOOK
// =================================================================
@Composable
fun DayBookScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val period by storeViewModel.businessPeriod.collectAsState()
    val customRange by storeViewModel.businessCustomDateRange.collectAsState()
    val entries by storeViewModel.dayBookEntries.collectAsState()
    val store by storeViewModel.store.collectAsState()

    val totalIn = remember(entries) { entries.filter { it.isIncome }.sumOf { it.amount } }
    val totalOut = remember(entries) { entries.filter { !it.isIncome }.sumOf { it.amount } }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            initialRange = customRange,
            currentLanguage = currentLanguage,
            onDismiss = { showDatePicker = false },
            onConfirm = { s, e ->
                storeViewModel.setBusinessCustomDateRange(s, e)
                showDatePicker = false
            }
        )
    }

    fun buildReportData(): ReportTableData =
        com.example.util.ReportDataBuilders.buildDayBookReportTable(
            entries = entries,
            store = store,
            periodLabel = period.labelEn,
            lang = currentLanguage
        )

    val isBn = currentLanguage == AppLanguage.BN
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item(key = "daybook_header") {
            ReportTopBar(
                title = if (isBn) "ডে বুক" else "Day Book",
                onBack = { storeViewModel.navigateBackInBusiness() }
            )

            HistoryPeriodTabs(
                currentPeriod = period,
                customRange = customRange,
                currentLanguage = currentLanguage,
                onSelectPeriod = {
                    if (it == HistoryPeriod.CUSTOM) showDatePicker = true
                    else storeViewModel.setBusinessPeriod(it)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ExportActionBar(
                onDownloadPdf = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "DayBook_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                },
                onPrint = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "DayBook_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.printReportFile(context, file, "DayBook", currentLanguage)
                },
                onExportCsv = {
                    val file = ReportExportUtil.generateCsvReport(context, buildReportData(), "DayBook_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                },
                onShare = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "DayBook_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.shareReportFile(context, file, "application/pdf", "Day Book - ${store?.name}", currentLanguage)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (entries.isEmpty()) {
            item(key = "daybook_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    PlainEmptyState(message = ShopLocale.get("no_tx_period", currentLanguage))
                }
            }
        } else {
            item(key = "daybook_table_header") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ReportTableHeaderRow(
                        headers = if (isBn) listOf("সময় / ধরন", "নাম / নম্বর", "", "টাকা")
                        else listOf("Time / Type", "Party / Ref", "", "Amount")
                    )
                }
            }

            items(entries) { e ->
                val dateStr = ShopLocale.formatDateShort(e.timestamp, currentLanguage)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    color = Color(0x0DFFFFFF),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = dateStr,
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatDayBookType(e.type, currentLanguage),
                                color = if (e.isIncome) VividGreen else Color(0xFFFF5252),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = ShopLocale.displayCustomerName(e.partyName, currentLanguage),
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${e.referenceNo}${if (e.note.isNotBlank()) " • ${e.note}" else ""}",
                                color = TextMuted,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${if (e.isIncome) "+" else "-"}${ShopLocale.formatMoney(e.amount, currentLanguage)}",
                            color = if (e.isIncome) VividGreen else Color(0xFFFF5252),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item(key = "daybook_totals") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        ReportTableTotalsBar(
                            label = if (isBn) "নিট ক্যাশ" else "NET CASH FLOW",
                            subValue = if (isBn)
                                "টাকা এসেছে: ${ShopLocale.formatMoney(totalIn, currentLanguage)}  |  টাকা গেছে: ${ShopLocale.formatMoney(totalOut, currentLanguage)}"
                            else
                                "In: ${ShopLocale.formatMoney(totalIn, currentLanguage)}  |  Out: ${ShopLocale.formatMoney(totalOut, currentLanguage)}",
                            mainValue = ShopLocale.formatSignedMoney(totalIn - totalOut, currentLanguage),
                            profitValue = if (isBn)
                                "${ShopLocale.formatNumber(entries.size, currentLanguage)}টি লেনদেন"
                            else
                                "${ShopLocale.formatNumber(entries.size, currentLanguage)} transactions"
                        )
                    }
                }
            }
        }
    }
}

private fun formatDayBookType(type: String, lang: AppLanguage = AppLanguage.EN): String {
    val isBn = lang == AppLanguage.BN
    return when (type) {
        "SALE_CASH" -> if (isBn) "নগদ বিক্রি" else "Cash Sale"
        "SALE_BAKI" -> if (isBn) "বাকি বিক্রি" else "Baki Sale"
        "PURCHASE_PAID" -> if (isBn) "নগদ কেনা" else "Cash Purchase"
        "PURCHASE_CREDIT" -> if (isBn) "বাকি কেনা" else "Credit Purchase"
        "PAYMENT_IN" -> if (isBn) "বাকি আদায়" else "Customer Payment In"
        "PURCHASE_RETURN" -> if (isBn) "মাল ফেরত" else "Purchase Return"
        else -> type
    }
}

// =================================================================
// 4. PROFIT AND LOSS
// =================================================================
@Composable
fun ProfitAndLossScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val period by storeViewModel.businessPeriod.collectAsState()
    val customRange by storeViewModel.businessCustomDateRange.collectAsState()
    val isStockBased by storeViewModel.isPnlStockBased.collectAsState()
    val pnl by storeViewModel.pnlStatement.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val canViewReports by storeViewModel.canViewReportsFlow.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()

    if (!canSeeProfit || !canViewReports) {
        if (isSimpleMode) {
            LaunchedEffect(Unit) { storeViewModel.navigateBackInBusiness() }
            return
        }
        com.example.ui.components.NoPermissionScreen(
            onBack = { storeViewModel.navigateBackInBusiness() },
            currentLanguage = currentLanguage,
            modifier = modifier
        )
        return
    }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            initialRange = customRange,
            currentLanguage = currentLanguage,
            onDismiss = { showDatePicker = false },
            onConfirm = { s, e ->
                storeViewModel.setBusinessCustomDateRange(s, e)
                showDatePicker = false
            }
        )
    }

    fun buildReportData(): ReportTableData =
        com.example.util.ReportDataBuilders.buildProfitLossReportTable(
            pnl = pnl,
            isStockBased = if (isFatherMode) true else isStockBased,
            store = store,
            periodLabel = period.labelEn,
            lang = currentLanguage,
            isFatherMode = isFatherMode
        )

    val isBnPnl = currentLanguage == AppLanguage.BN
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item(key = "pnl_header") {
            ReportTopBar(
                title = if (isFatherMode) ShopLocale.get("home_report_btn", currentLanguage)
                else if (isBnPnl) "লাভ-ক্ষতি" else "Profit & Loss",
                onBack = { storeViewModel.navigateBackInBusiness() }
            )

            HistoryPeriodTabs(
                currentPeriod = period,
                customRange = customRange,
                currentLanguage = currentLanguage,
                onSelectPeriod = {
                    if (it == HistoryPeriod.CUSTOM) showDatePicker = true
                    else storeViewModel.setBusinessPeriod(it)
                },
                includeCustomDate = !isFatherMode
            )

            Spacer(modifier = Modifier.height(10.dp))

            ExportActionBar(
                onDownloadPdf = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "ProfitLoss_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                },
                onPrint = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "ProfitLoss_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.printReportFile(context, file, "ProfitLoss", currentLanguage)
                },
                onExportCsv = {
                    val file = ReportExportUtil.generateCsvReport(context, buildReportData(), "ProfitLoss_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                },
                onShare = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "ProfitLoss_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.shareReportFile(context, file, "application/pdf", "Profit and Loss - ${store?.name}", currentLanguage)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Toggle Stock-based with plain-words explanation (hidden in Father mode)
        if (!isFatherMode) {
            item(key = "pnl_cogs_toggle") {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBnPnl) "স্টক ভিত্তিক হিসাব" else "Stock-based COGS",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isStockBased) {
                                        if (isBnPnl) "বিক্রিত মালের ক্রয়মূল্য ধরে লাভ হিসাব করে।"
                                        else "Matches buying cost directly against items sold in period."
                                    } else {
                                        if (isBnPnl) "এই সময়ে কেনা সব মালের খরচ ধরে হিসাব করে।"
                                        else "Counts all goods bought this period regardless of sales."
                                    },
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Switch(
                                checked = isStockBased,
                                onCheckedChange = { storeViewModel.togglePnlStockBased() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = VividGreen,
                                    checkedTrackColor = Color(0x335DD62C)
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // P&L Statement Line Items
        item(key = "pnl_statement_card") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    PnlRowItem(
                        label = if (isBnPnl) "মোট বিক্রি" else "Net Sales Revenue",
                        amount = ShopLocale.formatMoney(pnl.netSales, currentLanguage),
                        isPositive = true,
                        isBold = false
                    )
                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 8.dp))

                    PnlRowItem(
                        label = if (isFatherMode || isStockBased) {
                            if (isBnPnl) "বিক্রিত মালের কেনা দাম" else "Buying Cost of Sold Items"
                        } else {
                            if (isBnPnl) "মাল কেনার খরচ" else "Cost of Purchases Made"
                        },
                        amount = "-${ShopLocale.formatMoney(pnl.costOfGoodsSold, currentLanguage)}",
                        isPositive = false,
                        isBold = false
                    )
                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 8.dp))

                    if (isFatherMode) {
                        PnlRowItem(
                            label = ShopLocale.get("col_profit", currentLanguage),
                            amount = ShopLocale.formatMoney(pnl.grossProfit, currentLanguage),
                            isPositive = pnl.grossProfit >= 0,
                            isBold = true,
                            fontSize = 17.sp
                        )
                    } else {
                        PnlRowItem(
                            label = if (isBnPnl) "মোট লাভ" else "Gross Profit",
                            amount = ShopLocale.formatMoney(pnl.grossProfit, currentLanguage),
                            isPositive = pnl.grossProfit >= 0,
                            isBold = true
                        )
                        HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 8.dp))

                        if (pnl.otherIncome > 0.0) {
                            PnlRowItem(
                                label = if (isBnPnl) "অন্যান্য আয়" else "Other Income",
                                amount = "+${ShopLocale.formatMoney(pnl.otherIncome, currentLanguage)}",
                                isPositive = true,
                                isBold = false
                            )
                            HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 8.dp))
                        }

                        PnlRowItem(
                            label = if (isBnPnl) "দোকানের খরচ" else "Operating Expenses",
                            amount = "-${ShopLocale.formatMoney(pnl.expenses, currentLanguage)}",
                            isPositive = false,
                            isBold = false
                        )
                        if (pnl.expensesByCategory.isNotEmpty()) {
                            pnl.expensesByCategory.forEach { (categoryName, catAmt) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${ShopLocale.displayCategory(categoryName, currentLanguage)}",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "-${ShopLocale.formatMoney(catAmt, currentLanguage)}",
                                        color = AlertRed,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 8.dp))

                        PnlRowItem(
                            label = if (isBnPnl) "নিট লাভ" else "Net Profit",
                            amount = ShopLocale.formatMoney(pnl.netProfit, currentLanguage),
                            isPositive = pnl.netProfit >= 0,
                            isBold = true,
                            fontSize = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PnlRowItem(
    label: String,
    amount: String,
    isPositive: Boolean,
    isBold: Boolean,
    subLabel: String? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = if (isBold) TextPrimary else TextSecondary,
                fontSize = fontSize,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
            )
            if (!subLabel.isNullOrBlank()) {
                Text(
                    text = subLabel,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
        Text(
            text = amount,
            color = if (isBold && isPositive) VividGreen else if (!isPositive && isBold) Color(0xFFFF5252) else TextPrimary,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

// =================================================================
// 5. CUSTOMER STATEMENT
// =================================================================
@Composable
fun CustomerStatementScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val period by storeViewModel.businessPeriod.collectAsState()
    val customRange by storeViewModel.businessCustomDateRange.collectAsState()
    val customers by storeViewModel.customers.collectAsState()
    val selectedCustomer by storeViewModel.selectedStatementCustomer.collectAsState()
    val report by storeViewModel.customerStatementReport.collectAsState()
    val store by storeViewModel.store.collectAsState()

    val nowFormatted = remember(currentLanguage) { ShopLocale.formatDateLong(System.currentTimeMillis(), currentLanguage) }
    val isBn = currentLanguage == AppLanguage.BN

    var showPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            initialRange = customRange,
            currentLanguage = currentLanguage,
            onDismiss = { showDatePicker = false },
            onConfirm = { s, e ->
                storeViewModel.setBusinessCustomDateRange(s, e)
                showDatePicker = false
            }
        )
    }

    fun buildReportData(): ReportTableData {
        val rep = report ?: return ReportTableData("Customer Statement", store?.name ?: "", "", "", period.labelEn, nowFormatted, emptyList(), emptyList())
        return com.example.util.ReportDataBuilders.buildCustomerStatementReportTable(
            rep = rep,
            store = store,
            periodLabel = period.labelEn,
            lang = currentLanguage
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item(key = "customer_statement_header") {
            ReportTopBar(
                title = if (isBn) "ক্রেতার হিসাব" else "Customer Statement",
                onBack = { storeViewModel.navigateBackInBusiness() }
            )

            // Customer Selector Card
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPicker = true }
                        .testTag("picker_select_customer")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = ShopLocale.get("col_customer", currentLanguage),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = selectedCustomer?.name ?: if (isBn) "ক্রেতা বাছাই করুন..." else "Tap to choose a customer...",
                                color = if (selectedCustomer != null) TextPrimary else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (selectedCustomer == null) {
                                if (isBn) "বাছাই" else "Select"
                            } else {
                                ShopLocale.get("change", currentLanguage)
                            },
                            color = VividGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HistoryPeriodTabs(
                currentPeriod = period,
                customRange = customRange,
                currentLanguage = currentLanguage,
                onSelectPeriod = {
                    if (it == HistoryPeriod.CUSTOM) showDatePicker = true
                    else storeViewModel.setBusinessPeriod(it)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedCustomer != null) {
                ExportActionBar(
                    onDownloadPdf = {
                        val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "CustomerStatement_${selectedCustomer?.name}_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                    },
                    onPrint = {
                        val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "CustomerStatement_${selectedCustomer?.name}_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.printReportFile(context, file, "CustomerStatement", currentLanguage)
                    },
                    onExportCsv = {
                        val file = ReportExportUtil.generateCsvReport(context, buildReportData(), "CustomerStatement_${selectedCustomer?.name}_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                    },
                    onShare = {
                        val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "CustomerStatement_${selectedCustomer?.name}_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.shareReportFile(context, file, "application/pdf", "Statement for ${selectedCustomer?.name}", currentLanguage)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (selectedCustomer == null) {
            item(key = "customer_statement_no_party") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    PlainEmptyState(
                        message = if (isBn) "হিসাব দেখতে একজন ক্রেতা বাছাই করুন" else "Please choose a customer to generate their statement"
                    )
                }
            }
        } else if (report == null || report!!.entries.isEmpty()) {
            item(key = "customer_statement_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    PlainEmptyState(
                        message = ShopLocale.format(
                            "no_tx_party_period",
                            currentLanguage,
                            selectedCustomer!!.name,
                            ShopLocale.formatMoney(report?.openingBalance ?: 0.0, currentLanguage)
                        )
                    )
                }
            }
        } else {
            val rep = report!!
            item(key = "customer_statement_opening") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Color(0x1A00E5FF),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = ShopLocale.get("opening_balance", currentLanguage), color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text(text = ShopLocale.formatMoney(rep.openingBalance, currentLanguage), color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item(key = "customer_statement_table_header") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ReportTableHeaderRow(
                        headers = if (isBn) listOf("তারিখ / ধরন", "বিল #", "টাকা", "ব্যালেন্স")
                        else listOf("Date / Type", "Ref #", "Amount", "Balance")
                    )
                }
            }

            items(rep.entries) { e ->
                val dStr = ShopLocale.formatDateShort(e.timestamp, currentLanguage)
                val isSale = e.type == com.example.domain.LedgerEntryType.BAKI_SALE
                val isPartPaid = isSale && e.paidAmount > 0.001
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    color = Color(0x0DFFFFFF),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(text = dStr, color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = when {
                                        isPartPaid -> if (isBn) "আংশিক বাকি" else "Part Paid"
                                        isSale -> if (isBn) "বাকি বিক্রি" else "Baki Sale"
                                        else -> if (isBn) "আদায়" else "Payment In"
                                    },
                                    color = if (isSale) Color(0xFFFFB300) else VividGreen,
                                    fontSize = 10.5.sp
                                )
                            }
                            Text(text = e.referenceNo, color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text(
                                text = "${if (isSale) "+" else "-"}${ShopLocale.formatMoney(e.amount, currentLanguage)}",
                                color = if (isSale) Color(0xFFFFB300) else VividGreen,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = ShopLocale.formatMoney(e.runningBalance, currentLanguage),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (isPartPaid) {
                            val totalVal = if (e.totalAmount > 0.0) e.totalAmount else e.amount + e.paidAmount
                            Text(
                                text = if (isBn) {
                                    "মোট ${ShopLocale.formatMoney(totalVal, currentLanguage)} · দিয়েছে ${ShopLocale.formatMoney(e.paidAmount, currentLanguage)} · বাকি ${ShopLocale.formatMoney(e.amount, currentLanguage)}"
                                } else {
                                    "Total ${ShopLocale.formatMoney(totalVal, currentLanguage)} · Paid ${ShopLocale.formatMoney(e.paidAmount, currentLanguage)} · Due ${ShopLocale.formatMoney(e.amount, currentLanguage)}"
                                },
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            item(key = "customer_statement_totals") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        ReportTableTotalsBar(
                            label = if (isBn) "শেষ ব্যালেন্স" else "CLOSING BALANCE",
                            subValue = ShopLocale.format(
                                "baki_and_paid_summary",
                                currentLanguage,
                                ShopLocale.formatMoney(rep.totalBakiSales, currentLanguage),
                                ShopLocale.formatMoney(rep.totalPayments, currentLanguage)
                            ),
                            mainValue = ShopLocale.formatMoney(rep.closingBalance, currentLanguage),
                            profitValue = if (isBn) "মোট বাকি" else "Net Due"
                        )
                    }
                }
            }
        }
    }

    if (showPicker) {
        CustomerPickerModal(
            customers = customers,
            currentLanguage = currentLanguage,
            onDismiss = { showPicker = false },
            onSelect = {
                storeViewModel.selectStatementCustomer(it)
                showPicker = false
            }
        )
    }
}

// =================================================================
// 6. SUPPLIER STATEMENT
// =================================================================
@Composable
fun SupplierStatementScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val period by storeViewModel.businessPeriod.collectAsState()
    val customRange by storeViewModel.businessCustomDateRange.collectAsState()
    val suppliers by storeViewModel.suppliers.collectAsState()
    val selectedSupplier by storeViewModel.selectedStatementSupplier.collectAsState()
    val report by storeViewModel.supplierStatementReport.collectAsState()
    val store by storeViewModel.store.collectAsState()

    val nowFormatted = remember(currentLanguage) { ShopLocale.formatDateLong(System.currentTimeMillis(), currentLanguage) }
    val isBn = currentLanguage == AppLanguage.BN

    var showPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            initialRange = customRange,
            currentLanguage = currentLanguage,
            onDismiss = { showDatePicker = false },
            onConfirm = { s, e ->
                storeViewModel.setBusinessCustomDateRange(s, e)
                showDatePicker = false
            }
        )
    }

    fun buildReportData(): ReportTableData {
        val rep = report ?: return ReportTableData("Supplier Statement", store?.name ?: "", "", "", period.labelEn, nowFormatted, emptyList(), emptyList())
        return com.example.util.ReportDataBuilders.buildSupplierStatementReportTable(
            rep = rep.copy(entries = rep.entries.filter { it.type != "PURCHASE_RETURN" }),
            store = store,
            periodLabel = period.labelEn,
            lang = currentLanguage
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item(key = "supplier_statement_header") {
            ReportTopBar(
                title = if (isBn) "সাপ্লায়ার হিসাব" else "Supplier Statement",
                onBack = { storeViewModel.navigateBackInBusiness() }
            )

            // Supplier Selector Card
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPicker = true }
                        .testTag("picker_select_supplier")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isBn) "সাপ্লায়ার" else "Supplier",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = selectedSupplier?.name ?: if (isBn) "সাপ্লায়ার বাছাই করুন..." else "Tap to choose a supplier...",
                                color = if (selectedSupplier != null) TextPrimary else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (selectedSupplier == null) {
                                if (isBn) "বাছাই" else "Select"
                            } else {
                                ShopLocale.get("change", currentLanguage)
                            },
                            color = VividGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HistoryPeriodTabs(
                currentPeriod = period,
                customRange = customRange,
                currentLanguage = currentLanguage,
                onSelectPeriod = {
                    if (it == HistoryPeriod.CUSTOM) showDatePicker = true
                    else storeViewModel.setBusinessPeriod(it)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedSupplier != null) {
                ExportActionBar(
                    onDownloadPdf = {
                        val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "SupplierStatement_${selectedSupplier?.name}_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                    },
                    onPrint = {
                        val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "SupplierStatement_${selectedSupplier?.name}_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.printReportFile(context, file, "SupplierStatement", currentLanguage)
                    },
                    onExportCsv = {
                        val file = ReportExportUtil.generateCsvReport(context, buildReportData(), "SupplierStatement_${selectedSupplier?.name}_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                    },
                    onShare = {
                        val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "SupplierStatement_${selectedSupplier?.name}_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.shareReportFile(context, file, "application/pdf", "Statement for ${selectedSupplier?.name}", currentLanguage)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (selectedSupplier == null) {
            item(key = "supplier_statement_no_party") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    PlainEmptyState(
                        message = if (isBn) "হিসাব দেখতে একজন সাপ্লায়ার বাছাই করুন" else "Please choose a supplier to generate their statement"
                    )
                }
            }
        } else if (report == null || report!!.entries.isEmpty()) {
            item(key = "supplier_statement_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    PlainEmptyState(
                        message = ShopLocale.format(
                            "no_tx_party_period",
                            currentLanguage,
                            selectedSupplier!!.name,
                            ShopLocale.formatMoney(report?.openingBalance ?: 0.0, currentLanguage)
                        )
                    )
                }
            }
        } else {
            val rep = report!!
            item(key = "supplier_statement_opening") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Color(0x1AFFA000),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = ShopLocale.get("opening_balance", currentLanguage), color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text(text = ShopLocale.formatMoney(rep.openingBalance, currentLanguage), color = Color(0xFFFFB300), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item(key = "supplier_statement_table_header") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ReportTableHeaderRow(
                        headers = if (isBn) listOf("তারিখ / ধরন", "বিল #", "টাকা", "ব্যালেন্স")
                        else listOf("Date / Type", "Ref #", "Amount", "Balance")
                    )
                }
            }

            items(rep.entries.filter { it.type != "PURCHASE_RETURN" }) { e ->
                val dStr = ShopLocale.formatDateShort(e.timestamp, currentLanguage)
                val isPayment = e.type.contains("PAY", ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    color = Color(0x0DFFFFFF),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(text = dStr, color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = ShopLocale.displayEntryType(e.type, currentLanguage),
                                color = if (isPayment) VividGreen else Color(0xFFFF5252),
                                fontSize = 10.5.sp
                            )
                        }
                        Text(text = e.referenceNo, color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text(
                            text = "${if (isPayment) "-" else "+"}${ShopLocale.formatMoney(e.amount, currentLanguage)}",
                            color = if (isPayment) VividGreen else Color(0xFFFF5252),
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = ShopLocale.formatMoney(e.runningBalance, currentLanguage),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item(key = "supplier_statement_totals") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        ReportTableTotalsBar(
                            label = if (isBn) "শেষে আছে" else "CLOSING BALANCE",
                            subValue = ShopLocale.format(
                                "supplier_owed_and_paid_summary",
                                currentLanguage,
                                ShopLocale.formatMoney(rep.totalPurchases, currentLanguage),
                                ShopLocale.formatMoney(rep.totalPayments, currentLanguage)
                            ),
                            mainValue = ShopLocale.formatMoney(rep.closingBalance, currentLanguage),
                            profitValue = if (isBn) "মোট দেব" else "Total Owed"
                        )
                    }
                }
            }
        }
    }

    if (showPicker) {
        SupplierPickerModal(
            suppliers = suppliers,
            currentLanguage = currentLanguage,
            onDismiss = { showPicker = false },
            onSelect = {
                storeViewModel.selectStatementSupplier(it)
                showPicker = false
            }
        )
    }
}

// =================================================================
// 7. INVENTORY REPORT
// =================================================================
@Composable
fun InventoryReportScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val products by storeViewModel.products.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()

    var lowStockFilterOnly by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, lowStockFilterOnly) {
        if (lowStockFilterOnly) products.filter { it.stock <= it.lowStockLimit } else products
    }

    val totalItemsCount = remember(filteredProducts) { filteredProducts.sumOf { it.stock } }
    val totalCostValue = remember(filteredProducts) { filteredProducts.sumOf { it.stock * it.buyPrice } }
    val totalSaleValue = remember(filteredProducts) { filteredProducts.sumOf { it.stock * it.sellPrice } }
    val isBnInv = currentLanguage == AppLanguage.BN

    fun buildReportData(): ReportTableData {
        val base = com.example.util.ReportDataBuilders.buildInventoryReportTable(
            products = filteredProducts,
            store = store,
            canSeeProfit = canSeeProfit,
            lang = currentLanguage
        )
        return if (lowStockFilterOnly) {
            base.copy(
                title = "${base.title} (${if (isBnInv) "শুধু কম স্টক" else "Low Stock Only"})"
            )
        } else {
            base
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item(key = "inventory_report_header") {
            ReportTopBar(
                title = if (isBnInv) "স্টক রিপোর্ট" else "Inventory Report",
                onBack = { storeViewModel.navigateBackInBusiness() }
            )

            // Filter: Low stock toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = VividGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBnInv) "শুধু কম স্টক দেখান" else "Low Stock Filter Only",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = lowStockFilterOnly,
                    onCheckedChange = { lowStockFilterOnly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VividGreen,
                        checkedTrackColor = Color(0x335DD62C)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            ExportActionBar(
                onDownloadPdf = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "InventoryReport_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                },
                onPrint = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "InventoryReport_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.printReportFile(context, file, "InventoryReport", currentLanguage)
                },
                onExportCsv = {
                    val file = ReportExportUtil.generateCsvReport(context, buildReportData(), "InventoryReport_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                },
                onShare = {
                    val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "InventoryReport_${System.currentTimeMillis()}", currentLanguage)
                    ReportExportUtil.shareReportFile(context, file, "application/pdf", "Inventory Report - ${store?.name}", currentLanguage)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filteredProducts.isEmpty()) {
            item(key = "inventory_report_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    PlainEmptyState(
                        message = if (lowStockFilterOnly) {
                            if (isBnInv) "কোনো কম স্টকের মাল নেই! সব স্টক ঠিক আছে।" else "No low stock products found! All items well stocked."
                        } else {
                            if (isBnInv) "স্টকে কোনো মাল নেই।" else "No products found in inventory."
                        }
                    )
                }
            }
        } else {
            item(key = "inventory_report_table_header") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ReportTableHeaderRow(
                        headers = if (canSeeProfit) {
                            if (isBnInv) listOf("মাল", "পরিমাণ", "কেনা / বেচা", "কেনা মূল্য")
                            else listOf("Product", "Qty / Unit", "Buy / Sell", "Cost Value")
                        } else {
                            if (isBnInv) listOf("মাল", "পরিমাণ", "বিক্রয় মূল্য", "মোট মূল্য")
                            else listOf("Product", "Qty / Unit", "Sell Price", "Sale Value")
                        }
                    )
                }
            }

            items(filteredProducts) { p ->
                val isLow = p.stock <= p.lowStockLimit
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    color = Color(0x0DFFFFFF),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = p.name,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = p.category,
                                color = TextMuted,
                                fontSize = 10.5.sp
                            )
                        }
                        Text(
                            text = "${ShopLocale.formatNumber(p.stock, currentLanguage)} ${ShopLocale.displayUnit(p.unit, currentLanguage)}",
                            color = if (isLow) Color(0xFFFFB300) else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isLow) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f)
                        )
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            if (canSeeProfit) {
                                Text(
                                    text = ShopLocale.formatMoney(p.buyPrice, currentLanguage),
                                    color = TextSecondary,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = ShopLocale.formatMoney(p.sellPrice, currentLanguage),
                                color = VividGreen,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = if (canSeeProfit) {
                                ShopLocale.formatMoney(p.stock * p.buyPrice, currentLanguage)
                            } else {
                                ShopLocale.formatMoney(p.stock * p.sellPrice, currentLanguage)
                            },
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item(key = "inventory_report_totals") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        ReportTableTotalsBar(
                            label = if (canSeeProfit) {
                                if (isBnInv) "মোট কেনা মূল্য" else "TOTAL COST VALUE"
                            } else {
                                if (isBnInv) "মোট বিক্রয় মূল্য" else "TOTAL SALE VALUE"
                            },
                            subValue = if (canSeeProfit) {
                                ShopLocale.format("sale_value_summary", currentLanguage, ShopLocale.formatMoney(totalSaleValue, currentLanguage))
                            } else {
                                if (isBnInv) "${ShopLocale.formatNumber(filteredProducts.size, currentLanguage)}টি মাল"
                                else "${ShopLocale.formatNumber(filteredProducts.size, currentLanguage)} products"
                            },
                            mainValue = ShopLocale.formatMoney(if (canSeeProfit) totalCostValue else totalSaleValue, currentLanguage),
                            profitValue = if (isBnInv)
                                "মোট ${ShopLocale.formatNumber(totalItemsCount, currentLanguage)}টি"
                            else
                                "${ShopLocale.formatNumber(totalItemsCount, currentLanguage)} total items"
                        )
                    }
                }
            }
        }
    }
}

// =================================================================
// REUSABLE REPORT ROW COMPONENTS
// =================================================================

@Composable
fun ReportTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("report_back_btn")) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ReportTableHeaderRow(
    headers: List<String>
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = Color(0x1A5DD62C),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            headers.forEachIndexed { idx, h ->
                val align = if (idx >= 2) TextAlign.End else TextAlign.Start
                Text(
                    text = h,
                    color = VividGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = align,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ReportTableTotalsBar(
    label: String,
    subValue: String,
    mainValue: String,
    profitValue: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        color = Color(0x265DD62C),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D5DD62C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                if (subValue.isNotBlank()) {
                    Text(
                        text = subValue,
                        color = TextSecondary,
                        fontSize = 10.5.sp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = mainValue,
                    color = VividGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (profitValue.isNotBlank()) {
                    Text(
                        text = profitValue,
                        color = TextPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerPickerModal(
    customers: List<Customer>,
    currentLanguage: AppLanguage = AppLanguage.EN,
    onDismiss: () -> Unit,
    onSelect: (Customer) -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "ক্রেতা বাছাই করুন" else "Select Customer",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 80.dp, max = 280.dp)
                ) {
                    items(customers) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(c) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = c.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                if (c.mobile.isNotBlank()) {
                                    Text(text = ShopLocale.digits(c.mobile, currentLanguage), color = TextSecondary, fontSize = 11.5.sp)
                                }
                            }
                            Text(
                                text = ShopLocale.format("due_prefix", currentLanguage, ShopLocale.formatMoney(c.currentBalance, currentLanguage)),
                                color = if (c.currentBalance > 0) Color(0xFFFFB300) else TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        HorizontalDivider(color = Color(0x1AFFFFFF))
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierPickerModal(
    suppliers: List<Supplier>,
    currentLanguage: AppLanguage = AppLanguage.EN,
    onDismiss: () -> Unit,
    onSelect: (Supplier) -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "সাপ্লায়ার বাছাই করুন" else "Select Supplier",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 80.dp, max = 280.dp)
                ) {
                    items(suppliers) { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(s) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = s.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(text = s.company.ifBlank { ShopLocale.digits(s.mobile, currentLanguage) }, color = TextSecondary, fontSize = 11.5.sp)
                            }
                            Text(
                                text = ShopLocale.format("owed_prefix", currentLanguage, ShopLocale.formatMoney(s.balance, currentLanguage)),
                                color = if (s.balance > 0) Color(0xFFFF5252) else TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        HorizontalDivider(color = Color(0x1AFFFFFF))
                    }
                }
            }
        }
    }
}
