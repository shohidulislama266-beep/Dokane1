package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.StaffPerformanceRow
import com.example.ui.components.ExportActionBar
import com.example.ui.components.GlassCard
import com.example.ui.components.PlainEmptyState
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

@Composable
fun StaffReportScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val period by storeViewModel.historyPeriod.collectAsState()
    val customRange by storeViewModel.historyCustomDateRange.collectAsState()
    val staffSummary by storeViewModel.staffReport.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()

    val nowFormatted = remember(currentLanguage) { ShopLocale.formatDateLong(System.currentTimeMillis(), currentLanguage) }

    var showDatePicker by remember { mutableStateOf(false) }

    BackHandler {
        storeViewModel.navigateBackInBusiness()
    }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            initialRange = customRange,
            currentLanguage = currentLanguage,
            onDismiss = { showDatePicker = false },
            onConfirm = { s, e ->
                storeViewModel.setHistoryCustomDateRange(s, e)
                showDatePicker = false
            }
        )
    }

    fun buildReportData(): ReportTableData {
        val isBn = currentLanguage == AppLanguage.BN
        val headers = if (canSeeProfit) {
            if (isBn) listOf("কর্মী", "পদ", "অর্ডার", "মাল বিক্রি", "মোট বিক্রি", "লাভ", "বাকি আদায়")
            else listOf("Staff Member", "Role", "Orders", "Items Sold", "Sales Total", "Profit", "Payments In")
        } else {
            if (isBn) listOf("কর্মী", "পদ", "অর্ডার", "মাল বিক্রি", "মোট বিক্রি", "বাকি আদায়")
            else listOf("Staff Member", "Role", "Orders", "Items Sold", "Sales Total", "Payments In")
        }

        val rows = staffSummary.rows.map { row ->
            if (canSeeProfit) {
                listOf(
                    row.memberName,
                    ShopLocale.displayStaffRole(row.roleLabel, currentLanguage),
                    ShopLocale.formatNumber(row.salesCount, currentLanguage),
                    ShopLocale.formatNumber(row.itemsSold, currentLanguage),
                    ShopLocale.formatMoney(row.totalSales, currentLanguage),
                    ShopLocale.formatMoney(row.profit, currentLanguage),
                    ShopLocale.formatMoney(row.paymentsReceived, currentLanguage)
                )
            } else {
                listOf(
                    row.memberName,
                    ShopLocale.displayStaffRole(row.roleLabel, currentLanguage),
                    ShopLocale.formatNumber(row.salesCount, currentLanguage),
                    ShopLocale.formatNumber(row.itemsSold, currentLanguage),
                    ShopLocale.formatMoney(row.totalSales, currentLanguage),
                    ShopLocale.formatMoney(row.paymentsReceived, currentLanguage)
                )
            }
        }

        val rawRows: List<List<Double?>> = staffSummary.rows.map { row ->
            if (canSeeProfit) {
                listOf(null, null, row.salesCount.toDouble(), row.itemsSold, row.totalSales, row.profit, row.paymentsReceived)
            } else {
                listOf(null, null, row.salesCount.toDouble(), row.itemsSold, row.totalSales, row.paymentsReceived)
            }
        }

        val totalLabel = if (isBn)
            "মোট (${ShopLocale.formatNumber(staffSummary.rows.size, currentLanguage)} জন)"
        else
            "TOTAL (${ShopLocale.formatNumber(staffSummary.rows.size, currentLanguage)} staff)"
        val totals = if (canSeeProfit) {
            listOf(
                totalLabel,
                "",
                ShopLocale.formatNumber(staffSummary.totalSalesCount, currentLanguage),
                ShopLocale.formatNumber(staffSummary.totalItemsSold, currentLanguage),
                ShopLocale.formatMoney(staffSummary.totalSalesAmount, currentLanguage),
                ShopLocale.formatMoney(staffSummary.totalProfit, currentLanguage),
                ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, currentLanguage)
            )
        } else {
            listOf(
                totalLabel,
                "",
                ShopLocale.formatNumber(staffSummary.totalSalesCount, currentLanguage),
                ShopLocale.formatNumber(staffSummary.totalItemsSold, currentLanguage),
                ShopLocale.formatMoney(staffSummary.totalSalesAmount, currentLanguage),
                ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, currentLanguage)
            )
        }

        val rawTotals: List<Double?> = if (canSeeProfit) {
            listOf(null, null, staffSummary.totalSalesCount.toDouble(), staffSummary.totalItemsSold, staffSummary.totalSalesAmount, staffSummary.totalProfit, staffSummary.totalPaymentsReceived)
        } else {
            listOf(null, null, staffSummary.totalSalesCount.toDouble(), staffSummary.totalItemsSold, staffSummary.totalSalesAmount, staffSummary.totalPaymentsReceived)
        }

        return ReportTableData(
            title = if (isBn) "স্টাফ রিপোর্ট" else "Staff Performance Report",
            storeName = store?.name ?: "StoreOS Dokan",
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = if (isBn) period.labelBn else period.labelEn,
            generatedAt = nowFormatted,
            headers = headers,
            rows = rows,
            numericColumns = if (canSeeProfit) setOf(2, 3, 4, 5, 6) else setOf(2, 3, 4, 5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("staff_report_list"),
        contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "staff_report_top_bar") {
            ReportTopBar(
                title = if (currentLanguage == AppLanguage.BN) "স্টাফ পারফর্মেন্স রিপোর্ট" else "Staff Report",
                onBack = { storeViewModel.navigateBackInBusiness() }
            )
        }

        item(key = "staff_report_period_tabs") {
            HistoryPeriodTabs(
                currentPeriod = period,
                customRange = customRange,
                currentLanguage = currentLanguage,
                onSelectPeriod = {
                    if (it == HistoryPeriod.CUSTOM) showDatePicker = true
                    else storeViewModel.setHistoryPeriod(it)
                }
            )
        }

        // 4 Export buttons strictly on ONE line
        item(key = "staff_report_export_bar") {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ExportActionBar(
                    onDownloadPdf = {
                        val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "StaffReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                    },
                    onPrint = {
                        val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "StaffReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.printReportFile(context, file, "StaffReport", currentLanguage)
                    },
                    onExportCsv = {
                        val file = ReportExportUtil.generateCsvReport(context, buildReportData(), "StaffReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                    },
                    onShare = {
                        val file = ReportExportUtil.generatePdfReport(context, buildReportData(), "StaffReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.shareReportFile(context, file, "application/pdf", "Staff Performance Report", currentLanguage)
                    }
                )
            }
        }

        // KPI Overview Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiStatCard(
                    title = if (currentLanguage == AppLanguage.BN) "মোট বিক্রয়" else "Total Sales",
                    value = ShopLocale.formatMoney(staffSummary.totalSalesAmount, currentLanguage),
                    icon = Icons.Default.TrendingUp,
                    iconTint = VividGreen,
                    modifier = Modifier.weight(1f)
                )
                KpiStatCard(
                    title = if (currentLanguage == AppLanguage.BN) "অর্ডার" else "Orders",
                    value = ShopLocale.formatNumber(staffSummary.totalSalesCount, currentLanguage),
                    icon = Icons.Default.Receipt,
                    iconTint = Color(0xFFFFB300),
                    modifier = Modifier.weight(1f)
                )
                if (canSeeProfit) {
                    KpiStatCard(
                        title = if (currentLanguage == AppLanguage.BN) "মোট লাভ" else "Profit",
                        value = ShopLocale.formatMoney(staffSummary.totalProfit, currentLanguage),
                        icon = Icons.Default.ShoppingCart,
                        iconTint = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (staffSummary.rows.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(30.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlainEmptyState(
                        message = if (currentLanguage == AppLanguage.BN) "এই সময়ের মধ্যে কোনো বিক্রয় বা কালেকশন রেকর্ড নেই" else "No sales or collections recorded by staff in this period"
                    )
                }
            }
        } else {
            // Table Title Header
            item {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "স্টাফ পারফর্মেন্স তালিকা" else "Staff Performance Breakdown",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                )
            }

            // Table Horizontal Scroll View
            item {
                val hScroll = rememberScrollState()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    color = GlassSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(modifier = Modifier.horizontalScroll(hScroll)) {
                        // Header Row
                        val isBn = currentLanguage == AppLanguage.BN
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF1E2638))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isBn) "কর্মী" else "Staff Member", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp))
                            Text(text = if (isBn) "পদ" else "Role", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                            Text(text = if (isBn) "অর্ডার" else "Orders", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(60.dp))
                            Text(text = if (isBn) "মাল বিক্রি" else "Items Sold", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(80.dp))
                            Text(text = if (isBn) "মোট বিক্রি" else "Sales Total", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
                            if (canSeeProfit) {
                                Text(text = ShopLocale.get("col_profit", currentLanguage), color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(100.dp))
                            }
                            Text(text = if (isBn) "বাকি আদায়" else "Payments In", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
                        }

                        HorizontalDivider(color = GlassBorder, thickness = 1.dp)

                        // Rows
                        staffSummary.rows.forEach { row ->
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = row.memberName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(130.dp))
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x225DD62C))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = ShopLocale.displayStaffRole(row.roleLabel, currentLanguage),
                                        color = VividGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(text = ShopLocale.formatNumber(row.salesCount, currentLanguage), color = TextPrimary, fontSize = 13.sp, textAlign = TextAlign.End, maxLines = 1, softWrap = false, modifier = Modifier.width(60.dp))
                                Text(text = ShopLocale.formatNumber(row.itemsSold, currentLanguage), color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.End, maxLines = 1, softWrap = false, modifier = Modifier.width(80.dp))
                                Text(text = ShopLocale.formatMoney(row.totalSales, currentLanguage), color = VividGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
                                if (canSeeProfit) {
                                    Text(text = ShopLocale.formatMoney(row.profit, currentLanguage), color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.width(100.dp))
                                }
                                Text(text = ShopLocale.formatMoney(row.paymentsReceived, currentLanguage), color = TextPrimary, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
                            }
                            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
                        }

                        // Totals Row
                        Row(
                            modifier = Modifier
                                .background(Color(0x2600E5FF))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isBn) "মোট" else "TOTAL", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(130.dp))
                            Text(text = if (isBn) "${ShopLocale.formatNumber(staffSummary.rows.size, currentLanguage)} জন" else "${ShopLocale.formatNumber(staffSummary.rows.size, currentLanguage)} staff", color = TextMuted, fontSize = 11.sp, modifier = Modifier.width(90.dp))
                            Text(text = ShopLocale.formatNumber(staffSummary.totalSalesCount, currentLanguage), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1, softWrap = false, modifier = Modifier.width(60.dp))
                            Text(text = ShopLocale.formatNumber(staffSummary.totalItemsSold, currentLanguage), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1, softWrap = false, modifier = Modifier.width(80.dp))
                            Text(text = ShopLocale.formatMoney(staffSummary.totalSalesAmount, currentLanguage), color = VividGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
                            if (canSeeProfit) {
                                Text(text = ShopLocale.formatMoney(staffSummary.totalProfit, currentLanguage), color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End, modifier = Modifier.width(100.dp))
                            }
                            Text(text = ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, currentLanguage), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(110.dp))
                        }
                    }
                }
            }

            // Individual Card Breakdown
            items(staffSummary.rows) { row ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("staff_card_${row.memberId}")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = VividGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = row.memberName,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x225DD62C))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = ShopLocale.displayStaffRole(row.roleLabel, currentLanguage),
                                    color = VividGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val isBnCard = currentLanguage == AppLanguage.BN
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = if (isBnCard) "বিক্রি" else "Sales Recorded", color = TextMuted, fontSize = 11.sp)
                                Text(text = ShopLocale.formatMoney(row.totalSales, currentLanguage), color = VividGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = if (isBnCard) "অর্ডার / মাল" else "Orders / Items", color = TextMuted, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                Text(
                                    text = "${ShopLocale.formatNumber(row.salesCount, currentLanguage)} / ${ShopLocale.formatNumber(row.itemsSold, currentLanguage)}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            if (canSeeProfit) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = if (isBnCard) "লাভ" else "Profit Generated", color = TextMuted, fontSize = 11.sp)
                                    Text(text = ShopLocale.formatMoney(row.profit, currentLanguage), color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (row.paymentsReceived > 0.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x14FFFFFF))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Payment, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = if (isBnCard) "ক্রেতার বাকি আদায়" else "Customer Payments Received", color = TextSecondary, fontSize = 11.sp)
                                }
                                Text(text = ShopLocale.formatMoney(row.paymentsReceived, currentLanguage), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
