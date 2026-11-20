package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ExportActionBar
import com.example.ui.components.GlassCard
import com.example.ui.components.PlainEmptyState
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

/**
 * Step 11: Expense Report Screen
 * Live expenses for selected period with category breakdown, detailed table,
 * and 4 export actions (PDF, Print, Excel/CSV, Share).
 */
@Composable
fun ExpenseReportScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by storeViewModel.businessFilteredExpenses.collectAsState()
    val period by storeViewModel.businessPeriod.collectAsState()
    val customRange by storeViewModel.businessCustomDateRange.collectAsState()
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val store by storeViewModel.store.collectAsState()

    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    val totalExpenses = remember(expenses) {
        expenses.sumOf { it.amount }
    }

    val byCategory = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

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

    fun buildExportData(): ReportTableData =
        com.example.util.ReportDataBuilders.buildExpenseReportTable(
            expenses = expenses,
            store = store,
            periodLabel = period.labelEn,
            lang = currentLanguage
        )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar (scrolls with content)
        item(key = "expense_report_top_bar") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .displayCutoutPadding(),
                color = Color(0xF20F0F0F),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { storeViewModel.navigateBackInBusiness() },
                        modifier = Modifier.testTag("expense_report_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "খরচ রিপোর্ট" else "Expense Report",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (currentLanguage == AppLanguage.BN)
                                "${period.labelBn} • ${ShopLocale.formatNumber(expenses.size, currentLanguage)}টি এন্ট্রি"
                            else
                                "${period.labelEn} • ${ShopLocale.formatNumber(expenses.size, currentLanguage)} entries",
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Period Tabs
        item {
            HistoryPeriodTabs(
                currentPeriod = period,
                customRange = customRange,
                currentLanguage = currentLanguage,
                onSelectPeriod = { selected ->
                    if (selected == HistoryPeriod.CUSTOM) {
                        showDatePicker = true
                    } else {
                        storeViewModel.setBusinessPeriod(selected)
                    }
                }
            )
        }

        // Summary Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("expense_report_summary_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x26FF5252)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = AlertRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "মোট ব্যয় / খরচ" else "Total Expenses",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = ShopLocale.formatMoney(totalExpenses, currentLanguage),
                        color = AlertRed,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Export Actions
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ExportActionBar(
                    onDownloadPdf = {
                        val file = ReportExportUtil.generatePdfReport(context, buildExportData(), "ExpenseReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                    },
                    onPrint = {
                        val file = ReportExportUtil.generatePdfReport(context, buildExportData(), "ExpenseReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.printReportFile(context, file, "ExpenseReport", currentLanguage)
                    },
                    onExportCsv = {
                        val file = ReportExportUtil.generateCsvReport(context, buildExportData(), "ExpenseReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                    },
                    onShare = {
                        val file = ReportExportUtil.generatePdfReport(context, buildExportData(), "ExpenseReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.shareReportFile(context, file, "application/pdf", "Expense Report - ${store?.name}", currentLanguage)
                    }
                )
            }
        }

        // Breakdown by Category
        if (byCategory.isNotEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "ক্যাটেগরি অনুযায়ী" else "Category Breakdown",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        byCategory.forEach { (catName, catTotal) ->
                            val pct = if (totalExpenses > 0) (catTotal / totalExpenses * 100) else 0.0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = ShopLocale.displayCategory(catName, currentLanguage), color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                    Text(text = ShopLocale.format("pct_of_total", currentLanguage, ShopLocale.formatPercent(pct, 1, currentLanguage)), color = TextMuted, fontSize = 11.sp)
                                }
                                Text(
                                    text = ShopLocale.formatMoney(catTotal, currentLanguage),
                                    color = AlertRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Entries List
        if (expenses.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlainEmptyState(
                        message = if (currentLanguage == AppLanguage.BN) "এই সময়ে কোনো খরচ নেই" else "No expenses recorded in this period"
                    )
                }
            }
        } else {
            item {
                Text(
                    text = if (currentLanguage == AppLanguage.BN)
                        "লেনদেন (${ShopLocale.formatNumber(expenses.size, currentLanguage)})"
                    else
                        "Transactions (${ShopLocale.formatNumber(expenses.size, currentLanguage)})",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(expenses, key = { it.id }) { exp ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0x33FF5252)
                                ) {
                                    Text(
                                        text = ShopLocale.displayCategory(exp.category, currentLanguage),
                                        color = AlertRed,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ShopLocale.displayAccountName(exp.accountName, exp.accountId, currentLanguage),
                                    color = TextMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                            if (exp.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = exp.note,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ShopLocale.formatDateShort(exp.date, currentLanguage),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "-${ShopLocale.formatMoney(exp.amount, currentLanguage)}",
                            color = AlertRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 11: Other Income Report Screen
 * Live income for selected period with category breakdown, detailed table,
 * and 4 export actions (PDF, Print, Excel/CSV, Share).
 */
@Composable
fun IncomeReportScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val incomes by storeViewModel.businessFilteredOtherIncomes.collectAsState()
    val period by storeViewModel.businessPeriod.collectAsState()
    val customRange by storeViewModel.businessCustomDateRange.collectAsState()
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val store by storeViewModel.store.collectAsState()

    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    val totalIncome = remember(incomes) {
        incomes.sumOf { it.amount }
    }

    val byCategory = remember(incomes) {
        incomes.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

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

    fun buildExportData(): ReportTableData =
        com.example.util.ReportDataBuilders.buildOtherIncomeReportTable(
            incomes = incomes,
            store = store,
            periodLabel = period.labelEn,
            lang = currentLanguage
        )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar (scrolls with content)
        item(key = "income_report_top_bar") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .displayCutoutPadding(),
                color = Color(0xF20F0F0F),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { storeViewModel.navigateBackInBusiness() },
                        modifier = Modifier.testTag("income_report_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "অন্যান্য আয় রিপোর্ট" else "Other Income Report",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (currentLanguage == AppLanguage.BN)
                                "${period.labelBn} • ${ShopLocale.formatNumber(incomes.size, currentLanguage)}টি এন্ট্রি"
                            else
                                "${period.labelEn} • ${ShopLocale.formatNumber(incomes.size, currentLanguage)} entries",
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Period Tabs
        item {
            HistoryPeriodTabs(
                currentPeriod = period,
                customRange = customRange,
                currentLanguage = currentLanguage,
                onSelectPeriod = { selected ->
                    if (selected == HistoryPeriod.CUSTOM) {
                        showDatePicker = true
                    } else {
                        storeViewModel.setBusinessPeriod(selected)
                    }
                }
            )
        }

        // Summary Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("income_report_summary_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x265DD62C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = VividGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "মোট অন্যান্য আয়" else "Total Other Income",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = ShopLocale.formatMoney(totalIncome, currentLanguage),
                        color = VividGreen,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Export Actions
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ExportActionBar(
                    onDownloadPdf = {
                        val file = ReportExportUtil.generatePdfReport(context, buildExportData(), "IncomeReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                    },
                    onPrint = {
                        val file = ReportExportUtil.generatePdfReport(context, buildExportData(), "IncomeReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.printReportFile(context, file, "IncomeReport", currentLanguage)
                    },
                    onExportCsv = {
                        val file = ReportExportUtil.generateCsvReport(context, buildExportData(), "IncomeReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                    },
                    onShare = {
                        val file = ReportExportUtil.generatePdfReport(context, buildExportData(), "IncomeReport_${System.currentTimeMillis()}", currentLanguage)
                        ReportExportUtil.shareReportFile(context, file, "application/pdf", "Income Report - ${store?.name}", currentLanguage)
                    }
                )
            }
        }

        // Breakdown by Category
        if (byCategory.isNotEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "ক্যাটেগরি অনুযায়ী" else "Category Breakdown",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        byCategory.forEach { (catName, catTotal) ->
                            val pct = if (totalIncome > 0) (catTotal / totalIncome * 100) else 0.0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = ShopLocale.displayCategory(catName, currentLanguage), color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                    Text(text = ShopLocale.format("pct_of_total", currentLanguage, ShopLocale.formatPercent(pct, 1, currentLanguage)), color = TextMuted, fontSize = 11.sp)
                                }
                                Text(
                                    text = "+${ShopLocale.formatMoney(catTotal, currentLanguage)}",
                                    color = VividGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Entries List
        if (incomes.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlainEmptyState(
                        message = if (currentLanguage == AppLanguage.BN) "এই সময়ে কোনো অন্যান্য আয় নেই" else "No other income recorded in this period"
                    )
                }
            }
        } else {
            item {
                Text(
                    text = if (currentLanguage == AppLanguage.BN)
                        "লেনদেন (${ShopLocale.formatNumber(incomes.size, currentLanguage)})"
                    else
                        "Transactions (${ShopLocale.formatNumber(incomes.size, currentLanguage)})",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(incomes, key = { it.id }) { inc ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0x335DD62C)
                                ) {
                                    Text(
                                        text = ShopLocale.displayCategory(inc.category, currentLanguage),
                                        color = VividGreen,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ShopLocale.displayAccountName(inc.accountName, inc.accountId, currentLanguage),
                                    color = TextMuted,
                                    fontSize = 11.5.sp
                                )
                            }
                            if (inc.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = inc.note,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ShopLocale.formatDateShort(inc.date, currentLanguage),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "+${ShopLocale.formatMoney(inc.amount, currentLanguage)}",
                            color = VividGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
