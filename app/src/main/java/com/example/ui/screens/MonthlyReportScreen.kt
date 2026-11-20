package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.MonthlyReconciliationCheck
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.GlassCard
import com.example.ui.components.HeroGlowGlassCard
import com.example.ui.components.NoPermissionScreen
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
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ReportDataBuilders
import com.example.util.ReportExportUtil
import com.example.util.ShopLocale

private val AmberWarning = Color(0xFFFFB300)
private val CyanAccent = Color(0xFF00E5FF)

@Composable
fun MonthlyReportScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier,
    onBackOverride: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val year by storeViewModel.monthlyReportYear.collectAsState()
    val month by storeViewModel.monthlyReportMonth.collectAsState()
    val reportData by storeViewModel.monthlyReportData.collectAsState()
    val isLoading by storeViewModel.isMonthlyReportLoading.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val canViewReports by storeViewModel.canViewReportsFlow.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val showProfitInSimpleMode by storeViewModel.showProfitInSimpleMode.collectAsState()
    val isPrivacyHidden by storeViewModel.isPrivacyHidden.collectAsState()

    val isRestrictedMode = isSimpleMode || isFatherMode
    val uiLang = if (isRestrictedMode) AppLanguage.BN else currentLanguage
    val isBn = uiLang == AppLanguage.BN
    val showProfit = canSeeProfit && (!isRestrictedMode || showProfitInSimpleMode)
    val handleBack = onBackOverride ?: { storeViewModel.navigateBackInBusiness() }

    if (!isRestrictedMode && !canViewReports) {
        NoPermissionScreen(
            onBack = handleBack,
            modifier = modifier,
            currentLanguage = currentLanguage
        )
        return
    }

    BackHandler(onBack = handleBack)

    var showExportLanguageModal by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf("PDF") }

    // Sections 2-8 start collapsed by default; tap any section card to expand full tables
    val expandedSections = remember {
        mutableStateMapOf(
            "sales" to false,
            "baki" to false,
            "suppliers" to false,
            "stock" to false,
            "purchases" to false,
            "expenses_income" to false,
            "cash" to false
        )
    }

    fun txt(raw: String, fallback: String = "তথ্য"): String =
        if (isRestrictedMode) ShopLocale.plainBanglaText(raw, fallback) else raw

    val currentMonthLabel = txt(ShopLocale.formatMonthYear(year, month, uiLang))
    val emptyMonthFriendly = txt(ShopLocale.format("mr_empty_month_desc", uiLang, currentMonthLabel))
    val noSectionDataText = if (reportData.isMonthEmpty) {
        emptyMonthFriendly
    } else {
        txt(if (isBn) "এই মাসে এই বিভাগে কোনো লেনদেন নেই" else "No records in this section for $currentMonthLabel")
    }

    fun moneyText(amount: Double): String =
        if (isPrivacyHidden) "${ShopLocale.CURRENCY}••••" else txt(ShopLocale.formatMoney(amount, uiLang))

    fun numText(value: Long): String = txt(ShopLocale.formatNumber(value, uiLang))
    fun numText(value: Double): String = txt(ShopLocale.formatNumber(value, uiLang))
    fun pctText(value: Double): String = txt(ShopLocale.formatPercent(value, 1, uiLang))

    fun runExport(format: String, exportLang: AppLanguage) {
        try {
            val resolvedLang = if (isRestrictedMode) AppLanguage.BN else exportLang
            val data = ReportDataBuilders.buildMonthlyReportTable(
                reportData = reportData,
                year = year,
                month = month,
                store = store,
                canSeeProfit = showProfit,
                lang = resolvedLang,
                includeSupplierDebt = !isRestrictedMode,
                isRestrictedMode = isRestrictedMode
            )
            val fileBase = "MonthlyReport_${year}_${month + 1}_${resolvedLang.name}"
            when (format) {
                "PDF" -> {
                    val file = ReportExportUtil.generatePdfReport(context, data, fileBase, resolvedLang)
                    ReportExportUtil.downloadReportFile(context, file, resolvedLang)
                }
                "CSV" -> {
                    val file = ReportExportUtil.generateCsvReport(context, data, fileBase, resolvedLang)
                    ReportExportUtil.downloadReportFile(context, file, resolvedLang)
                }
                "PRINT" -> {
                    val file = ReportExportUtil.generatePdfReport(context, data, fileBase, resolvedLang)
                    ReportExportUtil.printReportFile(
                        context = context,
                        file = file,
                        reportName = txt(ShopLocale.get("mr_title", resolvedLang)),
                        lang = resolvedLang,
                        isLandscape = data.preferLandscape
                    )
                }
                "SHARE" -> {
                    val file = ReportExportUtil.generatePdfReport(context, data, fileBase, resolvedLang)
                    val exportMonthLabel = txt(ShopLocale.formatMonthYear(year, month, resolvedLang))
                    val subject = txt("${ShopLocale.get("mr_title", resolvedLang)}: $exportMonthLabel")
                    ReportExportUtil.shareReportFile(context, file, "application/pdf", subject, resolvedLang)
                }
            }
        } catch (e: Exception) {
            val msg = if (isBn) "ফাইল তৈরি করা যায়নি" else (e.localizedMessage ?: "Export failed")
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070907))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(VividGreen.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.25f, size.height * 0.08f),
                        radius = size.width * 0.85f
                    )
                )
            }
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .testTag("monthly_report_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp)
                .align(Alignment.TopCenter)
                .testTag("monthly_report_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "monthly_top_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = handleBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("monthly_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isBn) "পিছনে" else "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = txt(ShopLocale.get("mr_title", uiLang)),
                            color = TextPrimary,
                            fontSize = if (isRestrictedMode) 21.sp else 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            softWrap = true
                        )
                        Text(
                            text = if (isRestrictedMode) {
                                txt(ShopLocale.get("mr_subtitle_simple", uiLang))
                            } else {
                                ShopLocale.get("mr_subtitle_detailed", uiLang)
                            },
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 2,
                            softWrap = true
                        )
                    }
                    IconButton(
                        onClick = { storeViewModel.togglePrivacyHidden() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("monthly_privacy_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isPrivacyHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPrivacyHidden) {
                                if (isBn) "টাকার পরিমাণ দেখান" else "Show amounts"
                            } else {
                                if (isBn) "টাকার পরিমাণ লুকান" else "Hide amounts"
                            },
                            tint = if (isPrivacyHidden) AmberWarning else TextPrimary
                        )
                    }
                }
            }

            item(key = "monthly_month_picker") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = GlassSurface,
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                val newMonth = if (month == 0) 11 else month - 1
                                val newYear = if (month == 0) year - 1 else year
                                storeViewModel.setMonthlyReportMonth(newYear, newMonth)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = if (isBn) "আগের মাস" else "Previous Month",
                                tint = TextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = VividGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentMonthLabel,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                softWrap = true,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("monthly_current_label")
                            )
                        }

                        IconButton(
                            onClick = {
                                val newMonth = if (month == 11) 0 else month + 1
                                val newYear = if (month == 11) year + 1 else year
                                storeViewModel.setMonthlyReportMonth(newYear, newMonth)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = if (isBn) "পরের মাস" else "Next Month",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item(key = "monthly_loading") {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .testTag("monthly_loading_indicator"),
                        color = VividGreen,
                        trackColor = Color(0x22FFFFFF)
                    )
                }
            }

            item(key = "monthly_export_bar") {
                LocalizedMonthlyExportBar(
                    isBn = isBn,
                    isRestrictedMode = isRestrictedMode,
                    onDownloadPdf = {
                        if (isRestrictedMode) {
                            runExport("PDF", AppLanguage.BN)
                        } else {
                            selectedExportFormat = "PDF"
                            showExportLanguageModal = true
                        }
                    },
                    onPrint = { runExport("PRINT", uiLang) },
                    onExportCsv = {
                        if (isRestrictedMode) {
                            runExport("CSV", AppLanguage.BN)
                        } else {
                            selectedExportFormat = "CSV"
                            showExportLanguageModal = true
                        }
                    },
                    onShare = { runExport("SHARE", uiLang) }
                )
            }
                if (reportData.isMonthEmpty) {
                    item(key = "empty_month_banner") {
                        HeroGlowGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "empty_month_banner"
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 22.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AmberWarning.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f)),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = txt(ShopLocale.get("mr_empty_month_title", uiLang)),
                                    color = TextPrimary,
                                    fontSize = if (isRestrictedMode) 17.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = emptyMonthFriendly,
                                    color = TextSecondary,
                                    fontSize = if (isRestrictedMode) 14.sp else 13.sp,
                                    lineHeight = 19.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = txt(ShopLocale.get("mr_empty_month_hint", uiLang)),
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                // ------------------------------------------------------------------
                // 1. SUMMARY CARD (Always shown at top in Detailed, Simple, Father)
                // ------------------------------------------------------------------
                item(key = "section_summary") {
                    HeroGlowGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "section_summary_card"
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = txt(ShopLocale.get("mr_sec_summary", uiLang)),
                                    color = VividGreen,
                                    fontSize = if (isRestrictedMode) 17.sp else 15.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    softWrap = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VividGreen.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = txt(ShopLocale.format("total_orders_row", uiLang, reportData.totalOrders)),
                                        color = VividGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val summaryItems = buildList {
                                add(
                                    SummaryKpi(
                                        label = txt(if (isBn) "নিট বিক্রয়" else "Net Sales"),
                                        value = moneyText(reportData.netSales),
                                        color = VividGreen
                                    )
                                )
                                if (showProfit) {
                                    add(
                                        SummaryKpi(
                                            label = txt(if (isBn) "মোট লাভ" else "Gross Profit"),
                                            value = moneyText(reportData.grossProfit),
                                            color = CyanAccent
                                        )
                                    )
                                }
                                add(
                                    SummaryKpi(
                                        label = txt(ShopLocale.get("mr_col_orders", uiLang)),
                                        value = numText(reportData.totalOrders),
                                        color = TextPrimary
                                    )
                                )
                                add(
                                    SummaryKpi(
                                        label = txt(ShopLocale.get("mr_items_sold", uiLang)),
                                        value = numText(reportData.totalItemsSold),
                                        color = TextPrimary
                                    )
                                )
                                add(
                                    SummaryKpi(
                                        label = txt(if (isBn) "মোট খরচ" else "Total Expenses"),
                                        value = moneyText(reportData.totalExpenses),
                                        color = AlertRed
                                    )
                                )
                                if (showProfit) {
                                    add(
                                        SummaryKpi(
                                            label = txt(if (isBn) "নিট লাভ" else "Net Profit"),
                                            value = moneyText(reportData.netProfit),
                                            color = if (reportData.netProfit >= 0) VividGreen else AlertRed
                                        )
                                    )
                                }
                                add(
                                    SummaryKpi(
                                        label = txt(ShopLocale.get("mr_cash_and_bank_total", uiLang)),
                                        value = moneyText(reportData.cashClosingBalance),
                                        color = TextPrimary
                                    )
                                )
                                if (!isRestrictedMode) {
                                    add(
                                        SummaryKpi(
                                            label = if (isBn) "অন্যান্য আয়" else "Other Income",
                                            value = moneyText(reportData.otherIncome),
                                            color = VividGreen
                                        )
                                    )
                                    add(
                                        SummaryKpi(
                                            label = if (isBn) "মোট বাকি" else "Baki Receivable",
                                            value = moneyText(reportData.bakiClosingReceivable),
                                            color = AmberWarning
                                        )
                                    )
                                    add(
                                        SummaryKpi(
                                            label = if (isBn) "মহাজনকে দেব" else "Supplier Payable",
                                            value = moneyText(reportData.supplierClosingPayable),
                                            color = if (reportData.supplierClosingPayable > 0) AlertRed else TextPrimary
                                        )
                                    )
                                } else {
                                    add(
                                        SummaryKpi(
                                            label = "ক্রেতার কাছে পাব",
                                            value = moneyText(reportData.bakiClosingReceivable),
                                            color = AmberWarning
                                        )
                                    )
                                }
                            }

                            SummaryKpiGrid(items = summaryItems, isLargeText = isRestrictedMode)

                            Spacer(modifier = Modifier.height(10.dp))
                            val activeSummaryCheck = if (showProfit) reportData.summaryCheck else reportData.summaryCheckNoProfit
                            SectionNumbersCheckLine(
                                check = activeSummaryCheck,
                                lang = uiLang,
                                plainBangla = isRestrictedMode,
                                testTag = "check_summary_line"
                            )
                        }
                    }
                }

                // ------------------------------------------------------------------
                // RESTRICTED MODES (Simple Mode & Father Mode):
                // ONLY Summary (above), Baki, and Low Stock in plain Bangla.
                // ------------------------------------------------------------------
                if (isRestrictedMode) {
                    item(key = "section_baki") {
                        CollapsibleMonthlySection(
                            title = "২. কার কাছে পাব",
                            keyMetric = moneyText(reportData.bakiClosingReceivable),
                            keyColor = AmberWarning,
                            icon = Icons.Default.People,
                            expanded = expandedSections["baki"] == true,
                            onToggle = { expandedSections["baki"] = !(expandedSections["baki"] ?: false) },
                            isBn = true,
                            testTag = "section_baki_card"
                        ) {
                            BakiSectionContent(
                                reportData = reportData,
                                uiLang = AppLanguage.BN,
                                isBn = true,
                                plainBangla = true,
                                noSectionDataText = noSectionDataText,
                                emptyMonthFriendly = emptyMonthFriendly,
                                txt = ::txt,
                                moneyText = ::moneyText,
                                numText = ::numText
                            )
                        }
                    }

                    item(key = "section_low_stock") {
                        val lowTitle = "৩. কী মাল কমে গেছে"
                        CollapsibleMonthlySection(
                            title = lowTitle,
                            keyMetric = "${numText(reportData.lowStockCount)} টি",
                            keyColor = if (reportData.lowStockCount > 0L) AmberWarning else VividGreen,
                            icon = Icons.Default.Inventory2,
                            expanded = expandedSections["stock"] == true,
                            onToggle = { expandedSections["stock"] = !(expandedSections["stock"] ?: false) },
                            isBn = true,
                            testTag = "section_low_stock_card"
                        ) {
                            MetricRow(
                                label = "মোট পণ্যের ধরন",
                                value = "${numText(reportData.stockItemCount)} টি"
                            )
                            MetricRow(
                                label = "মোট মজুদের পরিমাণ",
                                value = numText(reportData.totalStockUnits)
                            )
                            if (showProfit) {
                                MetricRow(
                                    label = "বর্তমান স্টক মূল্য",
                                    value = moneyText(reportData.closingStockValue),
                                    valueColor = CyanAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            SectionNumbersCheckLine(
                                check = reportData.stockCheck,
                                lang = AppLanguage.BN,
                                plainBangla = true,
                                testTag = "check_stock_line"
                            )

                            SectionSubheading(
                                text = "কম স্টক পণ্য (${numText(reportData.lowStockCount)} টি)"
                            )
                            if (reportData.lowStockItems.isEmpty()) {
                                SectionEmptyMessage("সব মাল পর্যাপ্ত আছে")
                            } else {
                                val headers = if (showProfit) {
                                    listOf("পণ্য", "মজুদ", "সীমা", "মূল্য")
                                } else {
                                    listOf("পণ্য", "মজুদ", "সীমা")
                                }
                                val rows = reportData.lowStockItems.map { item ->
                                    val unitStr = txt(ShopLocale.displayUnit(item.unit, AppLanguage.BN))
                                    val base = listOf(
                                        txt(item.productName, "পণ্য"),
                                        "${numText(item.stock)} $unitStr",
                                        "${numText(item.lowStockLimit)} $unitStr"
                                    )
                                    if (showProfit) base + moneyText(item.stockValueAtCost) else base
                                }
                                MonthlyDataTable(
                                    headers = headers,
                                    rows = rows,
                                    numericColumns = if (showProfit) setOf(3) else emptySet(),
                                    minColumnWidth = 90.dp
                                )
                            }
                        }
                    }
                } else {
                    // ------------------------------------------------------------------
                    // DETAILED MODE: ALL 8 SECTIONS + FULL TABLES + PER-SECTION CHECKS
                    // ------------------------------------------------------------------
                    // 2. SALES
                    item(key = "section_sales") {
                        CollapsibleMonthlySection(
                            title = ShopLocale.get("mr_sec_sales", uiLang),
                            keyMetric = moneyText(reportData.netSales),
                            keyColor = VividGreen,
                            icon = Icons.Default.ShoppingBag,
                            expanded = expandedSections["sales"] == true,
                            onToggle = { expandedSections["sales"] = !(expandedSections["sales"] ?: false) },
                            isBn = isBn,
                            testTag = "section_sales_card"
                        ) {
                            if (reportData.totalOrders == 0L) {
                                SectionEmptyMessage(noSectionDataText)
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionNumbersCheckLine(
                                    check = reportData.salesCheck,
                                    lang = uiLang,
                                    plainBangla = false,
                                    testTag = "check_sales_line"
                                )
                            } else {
                                // Paid vs Baki split & overview metrics
                                MetricRow(
                                    label = if (isBn) "মোট বিক্রয় (ছাড়ের আগে)" else "Gross Sales",
                                    value = moneyText(reportData.grossSales)
                                )
                                MetricRow(
                                    label = if (isBn) "ছাড়" else "Discounts",
                                    value = moneyText(reportData.totalDiscounts),
                                    valueColor = if (reportData.totalDiscounts > 0) AmberWarning else TextPrimary
                                )
                                MetricRow(
                                    label = if (isBn) "নিট বিক্রয়" else "Net Sales",
                                    value = moneyText(reportData.netSales),
                                    valueColor = VividGreen,
                                    bold = true
                                )
                                MetricRow(
                                    label = if (isBn) "অর্ডার ও পণ্য সংখ্যা" else "Orders / Items Sold",
                                    value = "${numText(reportData.totalOrders)} / ${numText(reportData.totalItemsSold)}"
                                )
                                MetricRow(
                                    label = if (isBn) "গড় অর্ডার মূল্য" else "Average Order Value",
                                    value = moneyText(reportData.averageOrderValue)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AgeingBucketChip(
                                        label = if (isBn) "নগদ প্রাপ্ত বিক্রয়" else "Paid Sales",
                                        value = moneyText(reportData.cashReceivedSales),
                                        accent = VividGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AgeingBucketChip(
                                        label = if (isBn) "বাকিতে বিক্রয়" else "Baki Sales",
                                        value = moneyText(reportData.bakiCreditSales),
                                        accent = AmberWarning,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                SectionNumbersCheckLine(
                                    check = reportData.salesCheck,
                                    lang = uiLang,
                                    plainBangla = false,
                                    testTag = "check_sales_line"
                                )

                                // Sales by Day Table
                                SectionSubheading(ShopLocale.get("mr_sales_by_day", uiLang))
                                if (reportData.dailySalesRows.isEmpty()) {
                                    SectionEmptyMessage(noSectionDataText)
                                } else {
                                    val dayHeaders = if (showProfit) {
                                        listOf(
                                            ShopLocale.get("mr_col_day", uiLang),
                                            ShopLocale.get("mr_col_orders", uiLang),
                                            ShopLocale.get("mr_col_items", uiLang),
                                            ShopLocale.get("mr_col_sales", uiLang),
                                            ShopLocale.get("col_profit", uiLang)
                                        )
                                    } else {
                                        listOf(
                                            ShopLocale.get("mr_col_day", uiLang),
                                            ShopLocale.get("mr_col_orders", uiLang),
                                            ShopLocale.get("mr_col_items", uiLang),
                                            ShopLocale.get("mr_col_sales", uiLang)
                                        )
                                    }
                                    val dayRows = reportData.dailySalesRows.map { d ->
                                        val base = listOf(
                                            ShopLocale.digits(d.dateLabel, uiLang),
                                            numText(d.orders),
                                            numText(d.itemsSold),
                                            moneyText(d.salesAmount)
                                        )
                                        if (showProfit) base + moneyText(d.profitAmount) else base
                                    }
                                    val dayTotals = buildList {
                                        add(if (isBn) "মোট" else "Total")
                                        add(numText(reportData.totalOrders))
                                        add(numText(reportData.totalItemsSold))
                                        add(moneyText(reportData.netSales))
                                        if (showProfit) add(moneyText(reportData.totalProfit))
                                    }
                                    MonthlyDataTable(
                                        headers = dayHeaders,
                                        rows = dayRows,
                                        totals = dayTotals,
                                        numericColumns = if (showProfit) setOf(1, 2, 3, 4) else setOf(1, 2, 3),
                                        minColumnWidth = 86.dp
                                    )
                                }

                                // Top 5 Products Table
                                SectionSubheading(ShopLocale.get("mr_top_products", uiLang))
                                if (reportData.topProductsByAmount.isEmpty()) {
                                    SectionEmptyMessage(noSectionDataText)
                                } else {
                                    val prodHeaders = if (showProfit) {
                                        listOf("#", ShopLocale.get("col_product", uiLang), ShopLocale.get("col_qty_sold", uiLang), ShopLocale.get("mr_col_sales", uiLang), ShopLocale.get("col_profit", uiLang))
                                    } else {
                                        listOf("#", ShopLocale.get("col_product", uiLang), ShopLocale.get("col_qty_sold", uiLang), ShopLocale.get("mr_col_sales", uiLang))
                                    }
                                    val prodRows = reportData.topProductsByAmount.mapIndexed { idx, p ->
                                        val qty = "${numText(p.quantitySold)} ${ShopLocale.displayUnit(p.unit, uiLang)}"
                                        val base = listOf(numText(idx + 1L), p.productName, qty, moneyText(p.totalSalesAmount))
                                        if (showProfit) base + moneyText(p.totalProfit) else base
                                    }
                                    MonthlyDataTable(
                                        headers = prodHeaders,
                                        rows = prodRows,
                                        numericColumns = if (showProfit) setOf(3, 4) else setOf(3),
                                        minColumnWidth = 84.dp
                                    )
                                }

                                // Sales by Category Table
                                SectionSubheading(ShopLocale.get("mr_sales_by_category", uiLang))
                                if (reportData.salesByCategoryRows.isEmpty()) {
                                    SectionEmptyMessage(noSectionDataText)
                                } else {
                                    val catHeaders = if (showProfit) {
                                        listOf(
                                            ShopLocale.get("col_category", uiLang),
                                            ShopLocale.get("mr_col_items", uiLang),
                                            ShopLocale.get("mr_col_sales", uiLang),
                                            ShopLocale.get("mr_col_share", uiLang),
                                            ShopLocale.get("col_profit", uiLang)
                                        )
                                    } else {
                                        listOf(
                                            ShopLocale.get("col_category", uiLang),
                                            ShopLocale.get("mr_col_items", uiLang),
                                            ShopLocale.get("mr_col_sales", uiLang),
                                            ShopLocale.get("mr_col_share", uiLang)
                                        )
                                    }
                                    val catRows = reportData.salesByCategoryRows.map { c ->
                                        val base = listOf(
                                            ShopLocale.displayCategory(c.category, uiLang),
                                            numText(c.itemsCount),
                                            moneyText(c.salesAmount),
                                            pctText(c.percentage)
                                        )
                                        if (showProfit) base + moneyText(c.profitAmount) else base
                                    }
                                    val catTotals = buildList {
                                        add(if (isBn) "মোট" else "Total")
                                        add(numText(reportData.salesByCategoryTotalItems))
                                        add(moneyText(reportData.salesByCategoryTotalSales))
                                        add(pctText(100.0))
                                        if (showProfit) add(moneyText(reportData.salesByCategoryTotalProfit))
                                    }
                                    MonthlyDataTable(
                                        headers = catHeaders,
                                        rows = catRows,
                                        totals = catTotals,
                                        numericColumns = if (showProfit) setOf(1, 2, 3, 4) else setOf(1, 2, 3),
                                        minColumnWidth = 86.dp
                                    )
                                }

                                // Top 5 Customers Table
                                if (reportData.topCustomersByAmount.isNotEmpty()) {
                                    SectionSubheading(
                                        if (isBn) "শীর্ষ ৫ ক্রেতা" else "Top 5 Customers"
                                    )
                                    val custRows = reportData.topCustomersByAmount.mapIndexed { idx, cust ->
                                        listOf(
                                            numText(idx + 1L),
                                            ShopLocale.displayCustomerName(cust.customerName, uiLang),
                                            moneyText(cust.paidAmount),
                                            moneyText(cust.bakiAmount),
                                            moneyText(cust.totalBought)
                                        )
                                    }
                                    MonthlyDataTable(
                                        headers = listOf(
                                            "#",
                                            ShopLocale.get("col_customer", uiLang),
                                            ShopLocale.get("col_paid", uiLang),
                                            ShopLocale.get("col_due", uiLang),
                                            ShopLocale.get("col_total", uiLang)
                                        ),
                                        rows = custRows,
                                        numericColumns = setOf(2, 3, 4),
                                        minColumnWidth = 82.dp
                                    )
                                }
                            }
                        }
                    }

                    // 3. BAKI (Customers Owe) grouped 0-30, 31-60, 61-90, 90+
                    item(key = "section_baki") {
                        CollapsibleMonthlySection(
                            title = ShopLocale.get("mr_sec_baki", uiLang),
                            keyMetric = moneyText(reportData.bakiClosingReceivable),
                            keyColor = AmberWarning,
                            icon = Icons.Default.People,
                            expanded = expandedSections["baki"] == true,
                            onToggle = { expandedSections["baki"] = !(expandedSections["baki"] ?: false) },
                            isBn = isBn,
                            testTag = "section_baki_card"
                        ) {
                            BakiSectionContent(
                                reportData = reportData,
                                uiLang = uiLang,
                                isBn = isBn,
                                plainBangla = false,
                                noSectionDataText = noSectionDataText,
                                emptyMonthFriendly = emptyMonthFriendly,
                                txt = ::txt,
                                moneyText = ::moneyText,
                                numText = ::numText
                            )
                        }
                    }

                    // 4. SUPPLIERS (Full per-supplier table)
                    item(key = "section_suppliers") {
                        CollapsibleMonthlySection(
                            title = ShopLocale.get("mr_sec_suppliers", uiLang),
                            keyMetric = moneyText(reportData.supplierClosingPayable),
                            keyColor = if (reportData.supplierClosingPayable > 0) AlertRed else TextPrimary,
                            icon = Icons.Default.LocalShipping,
                            expanded = expandedSections["suppliers"] == true,
                            onToggle = { expandedSections["suppliers"] = !(expandedSections["suppliers"] ?: false) },
                            isBn = isBn,
                            testTag = "section_suppliers_card"
                        ) {
                            if (!reportData.supplierHasActivityOrDue) {
                                SectionEmptyMessage(noSectionDataText)
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionNumbersCheckLine(
                                    check = reportData.supplierCheck,
                                    lang = uiLang,
                                    plainBangla = false,
                                    testTag = "check_suppliers_line"
                                )
                            } else {
                                MetricRow(
                                    label = if (isBn) "শুরুতে দেনা" else "Opening Payable",
                                    value = moneyText(reportData.supplierOpeningPayable)
                                )
                                MetricRow(
                                    label = ShopLocale.get("mr_col_credit_purchases", uiLang),
                                    value = moneyText(reportData.supplierCreditPurchasesThisMonth),
                                    valueColor = AmberWarning
                                )
                                MetricRow(
                                    label = ShopLocale.get("mr_col_payments_out", uiLang),
                                    value = moneyText(reportData.supplierPaymentsOutThisMonth),
                                    valueColor = VividGreen
                                )
                                MetricRow(
                                    label = ShopLocale.get("mr_col_returns", uiLang),
                                    value = moneyText(reportData.supplierReturnsThisMonth),
                                    valueColor = CyanAccent
                                )
                                MetricRow(
                                    label = if (isBn) "শেষে দেব" else "Closing Payable",
                                    value = moneyText(reportData.supplierClosingPayable),
                                    valueColor = if (reportData.supplierClosingPayable > 0) AlertRed else TextPrimary,
                                    bold = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                SectionNumbersCheckLine(
                                    check = reportData.supplierCheck,
                                    lang = uiLang,
                                    plainBangla = false,
                                    testTag = "check_suppliers_line"
                                )

                                SectionSubheading(
                                    if (isBn) "সাপ্লায়ার অনুযায়ী তালিকা (${numText(reportData.supplierRowCount)})"
                                    else "Per-Supplier Statement (${numText(reportData.supplierRowCount)})"
                                )
                                if (reportData.supplierRows.isEmpty()) {
                                    SectionEmptyMessage(noSectionDataText)
                                } else {
                                    val supRows = reportData.supplierRows.map { s ->
                                        listOf(
                                            s.supplierName,
                                            moneyText(s.opening),
                                            moneyText(s.creditPurchases),
                                            moneyText(s.paymentsOut),
                                            moneyText(s.returns),
                                            moneyText(s.closing)
                                        )
                                    }
                                    val supTotals = listOf(
                                        if (isBn) "মোট (${numText(reportData.supplierRowCount)})" else "Total (${numText(reportData.supplierRowCount)})",
                                        moneyText(reportData.supplierOpeningPayable),
                                        moneyText(reportData.supplierCreditPurchasesThisMonth),
                                        moneyText(reportData.supplierPaymentsOutThisMonth),
                                        moneyText(reportData.supplierReturnsThisMonth),
                                        moneyText(reportData.supplierClosingPayable)
                                    )
                                    MonthlyDataTable(
                                        headers = listOf(
                                            ShopLocale.get("col_supplier", uiLang),
                                            ShopLocale.get("mr_col_opening", uiLang),
                                            ShopLocale.get("mr_col_credit_purchases", uiLang),
                                            ShopLocale.get("mr_col_payments_out", uiLang),
                                            ShopLocale.get("mr_col_returns", uiLang),
                                            ShopLocale.get("mr_col_closing", uiLang)
                                        ),
                                        rows = supRows,
                                        totals = supTotals,
                                        numericColumns = setOf(1, 2, 3, 4, 5),
                                        minColumnWidth = 88.dp
                                    )
                                }
                            }
                        }
                    }

                    // 5. STOCK (Value at cost, item count, opening N/A unless computable, Low Stock, Slow Sellers 30d)
                    item(key = "section_stock") {
                        CollapsibleMonthlySection(
                            title = ShopLocale.get("mr_sec_stock", uiLang),
                            keyMetric = if (showProfit) moneyText(reportData.closingStockValue) else if (isBn) "${numText(reportData.stockItemCount)} টি" else "${numText(reportData.stockItemCount)} items",
                            keyColor = CyanAccent,
                            icon = Icons.Default.Inventory2,
                            expanded = expandedSections["stock"] == true,
                            onToggle = { expandedSections["stock"] = !(expandedSections["stock"] ?: false) },
                            isBn = isBn,
                            testTag = "section_stock_card"
                        ) {
                            val openingStockDisplay = reportData.openingStockValueComputed?.let { moneyText(it) }
                                ?: ShopLocale.get("mr_not_available", uiLang)
                            MetricRow(
                                label = if (isBn) "প্রারম্ভিক স্টক মূল্য" else "Opening Stock Value",
                                value = openingStockDisplay,
                                valueColor = if (reportData.openingStockValueComputed == null) TextMuted else TextPrimary
                            )
                            if (showProfit) {
                                MetricRow(
                                    label = if (isBn) "বর্তমান স্টক মূল্য (ক্রয়মূল্যে)" else "Stock Value (at Cost)",
                                    value = moneyText(reportData.closingStockValue),
                                    valueColor = CyanAccent,
                                    bold = true
                                )
                            }
                            MetricRow(
                                label = ShopLocale.get("mr_stock_item_count", uiLang),
                                value = numText(reportData.stockItemCount)
                            )
                            MetricRow(
                                label = ShopLocale.get("mr_total_units", uiLang),
                                value = numText(reportData.totalStockUnits)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            SectionNumbersCheckLine(
                                check = reportData.stockCheck,
                                lang = uiLang,
                                plainBangla = false,
                                testTag = "check_stock_line"
                            )

                            SectionSubheading(
                                if (isBn) "কম স্টক পণ্য (${numText(reportData.lowStockCount)})"
                                else "Low-Stock Items (${numText(reportData.lowStockCount)})"
                            )
                            if (reportData.lowStockItems.isEmpty()) {
                                SectionEmptyMessage(if (isBn) "সব পণ্যের স্টক স্বাভাবিক আছে" else "No low-stock items")
                            } else {
                                val headers = if (showProfit) {
                                    listOf(
                                        ShopLocale.get("col_product", uiLang),
                                        ShopLocale.get("mr_col_stock", uiLang),
                                        ShopLocale.get("mr_col_limit", uiLang),
                                        ShopLocale.get("mr_col_value_cost", uiLang)
                                    )
                                } else {
                                    listOf(
                                        ShopLocale.get("col_product", uiLang),
                                        ShopLocale.get("mr_col_stock", uiLang),
                                        ShopLocale.get("mr_col_limit", uiLang)
                                    )
                                }
                                val rows = reportData.lowStockItems.map { item ->
                                    val unitStr = ShopLocale.displayUnit(item.unit, uiLang)
                                    val base = listOf(
                                        item.productName,
                                        "${numText(item.stock)} $unitStr",
                                        "${numText(item.lowStockLimit)} $unitStr"
                                    )
                                    if (showProfit) base + moneyText(item.stockValueAtCost) else base
                                }
                                MonthlyDataTable(
                                    headers = headers,
                                    rows = rows,
                                    numericColumns = if (showProfit) setOf(3) else emptySet(),
                                    minColumnWidth = 92.dp
                                )
                            }

                            SectionSubheading(
                                if (isBn) "৩০ দিনে বিক্রি হয়নি (${numText(reportData.slowStockCount)})"
                                else "Slow Sellers — No Sale in 30 Days (${numText(reportData.slowStockCount)})"
                            )
                            if (reportData.slowStock30DayItems.isEmpty()) {
                                SectionEmptyMessage(if (isBn) "গত ৩০ দিনে সব স্টক পণ্যই বিক্রি হয়েছে" else "All in-stock items sold within 30 days")
                            } else {
                                val slowHeaders = if (showProfit) {
                                    listOf(
                                        ShopLocale.get("col_product", uiLang),
                                        ShopLocale.get("mr_col_stock", uiLang),
                                        if (isBn) "একক" else "Unit",
                                        ShopLocale.get("mr_col_value_cost", uiLang)
                                    )
                                } else {
                                    listOf(
                                        ShopLocale.get("col_product", uiLang),
                                        ShopLocale.get("mr_col_stock", uiLang),
                                        if (isBn) "একক" else "Unit"
                                    )
                                }
                                val slowRows = reportData.slowStock30DayItems.map { item ->
                                    val unitStr = ShopLocale.displayUnit(item.unit, uiLang)
                                    val base = listOf(item.productName, "${numText(item.stock)} $unitStr", unitStr)
                                    if (showProfit) base + moneyText(item.moneyStuck) else base
                                }
                                val slowTotals = if (showProfit) {
                                    listOf(
                                        if (isBn) "মোট আটকে আছে" else "Total Stuck",
                                        "—",
                                        "—",
                                        moneyText(reportData.totalSlowStockMoneyStuck)
                                    )
                                } else null
                                MonthlyDataTable(
                                    headers = slowHeaders,
                                    rows = slowRows,
                                    totals = slowTotals,
                                    numericColumns = if (showProfit) setOf(3) else emptySet(),
                                    minColumnWidth = 90.dp
                                )
                            }
                        }
                    }

                    // 6. PURCHASES (Bought, Returns, Net + Table)
                    item(key = "section_purchases") {
                        CollapsibleMonthlySection(
                            title = ShopLocale.get("mr_sec_purchases", uiLang),
                            keyMetric = moneyText(reportData.netPurchases),
                            keyColor = CyanAccent,
                            icon = Icons.Default.ShoppingCart,
                            expanded = expandedSections["purchases"] == true,
                            onToggle = { expandedSections["purchases"] = !(expandedSections["purchases"] ?: false) },
                            isBn = isBn,
                            testTag = "section_purchases_card"
                        ) {
                            if (!reportData.purchaseHasActivity) {
                                SectionEmptyMessage(noSectionDataText)
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionNumbersCheckLine(
                                    check = reportData.purchasesCheck,
                                    lang = uiLang,
                                    plainBangla = false,
                                    testTag = "check_purchases_line"
                                )
                            } else {
                                MetricRow(
                                    label = ShopLocale.get("mr_total_bought", uiLang),
                                    value = moneyText(reportData.totalBought)
                                )
                                MetricRow(
                                    label = ShopLocale.get("mr_purchase_returns", uiLang),
                                    value = moneyText(reportData.purchaseReturnsTotal),
                                    valueColor = AmberWarning
                                )
                                MetricRow(
                                    label = ShopLocale.get("mr_net_purchases", uiLang),
                                    value = moneyText(reportData.netPurchases),
                                    valueColor = CyanAccent,
                                    bold = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                SectionNumbersCheckLine(
                                    check = reportData.purchasesCheck,
                                    lang = uiLang,
                                    plainBangla = false,
                                    testTag = "check_purchases_line"
                                )

                                SectionSubheading(
                                    if (isBn) "ক্রয় ও ফেরতের তালিকা" else "Purchases & Returns Ledger"
                                )
                                val purRows = buildList {
                                    reportData.purchaseRows.forEach { p ->
                                        add(
                                            listOf(
                                                ShopLocale.get("entry_purchase", uiLang),
                                                p.supplierName,
                                                ShopLocale.formatDateOnly(p.dateMillis, uiLang),
                                                moneyText(p.totalAmount),
                                                moneyText(p.paidAmount),
                                                moneyText(p.creditAmount)
                                            )
                                        )
                                    }
                                    reportData.returnRows.forEach { r ->
                                        add(
                                            listOf(
                                                ShopLocale.get("entry_purchase_return", uiLang),
                                                r.supplierName,
                                                ShopLocale.formatDateOnly(r.dateMillis, uiLang),
                                                moneyText(-r.amount),
                                                "—",
                                                "—"
                                            )
                                        )
                                    }
                                }
                                MonthlyDataTable(
                                    headers = listOf(
                                        ShopLocale.get("col_type", uiLang),
                                        ShopLocale.get("col_supplier", uiLang),
                                        ShopLocale.get("col_date", uiLang),
                                        ShopLocale.get("col_total", uiLang),
                                        ShopLocale.get("col_paid", uiLang),
                                        ShopLocale.get("col_due", uiLang)
                                    ),
                                    rows = purRows,
                                    totals = listOf(
                                        ShopLocale.get("mr_net_purchases", uiLang),
                                        "—",
                                        "—",
                                        moneyText(reportData.netPurchases),
                                        moneyText(reportData.supplierPaidAtPurchaseThisMonth),
                                        moneyText(reportData.supplierCreditPurchasesThisMonth)
                                    ),
                                    numericColumns = setOf(3, 4, 5),
                                    minColumnWidth = 86.dp
                                )
                            }
                        }
                    }

                    // 7. EXPENSES & OTHER INCOME (Both by Category)
                    item(key = "section_expenses_income") {
                        CollapsibleMonthlySection(
                            title = ShopLocale.get("mr_sec_expenses_income", uiLang),
                            keyMetric = moneyText(reportData.totalExpenses),
                            keyColor = AlertRed,
                            icon = Icons.Default.ReceiptLong,
                            expanded = expandedSections["expenses_income"] == true,
                            onToggle = {
                                expandedSections["expenses_income"] = !(expandedSections["expenses_income"] ?: false)
                            },
                            isBn = isBn,
                            testTag = "section_expenses_income_card"
                        ) {
                            if (reportData.expensesByCategory.isEmpty() && reportData.incomesByCategory.isEmpty()) {
                                SectionEmptyMessage(noSectionDataText)
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionNumbersCheckLine(
                                    check = reportData.expensesIncomeCheck,
                                    lang = uiLang,
                                    plainBangla = false,
                                    testTag = "check_expenses_income_line"
                                )
                            } else {
                                MetricRow(
                                    label = if (isBn) "মোট খরচ" else "Total Expenses",
                                    value = moneyText(reportData.totalExpenses),
                                    valueColor = AlertRed
                                )
                                MetricRow(
                                    label = ShopLocale.get("mr_other_income", uiLang),
                                    value = moneyText(reportData.otherIncome),
                                    valueColor = VividGreen
                                )
                                MetricRow(
                                    label = ShopLocale.get("mr_net_income_expense", uiLang),
                                    value = moneyText(reportData.netOtherIncomeMinusExpenses),
                                    valueColor = if (reportData.netOtherIncomeMinusExpenses >= 0) VividGreen else AlertRed,
                                    bold = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                SectionNumbersCheckLine(
                                    check = reportData.expensesIncomeCheck,
                                    lang = uiLang,
                                    plainBangla = false,
                                    testTag = "check_expenses_income_line"
                                )

                                SectionSubheading(ShopLocale.get("mr_expenses_by_cat", uiLang))
                                if (reportData.expensesByCategory.isEmpty()) {
                                    SectionEmptyMessage(if (isBn) "এই মাসে কোনো খরচ নেই" else "No expenses this month")
                                } else {
                                    val expRows = reportData.expensesByCategory.map { c ->
                                        listOf(
                                            ShopLocale.displayCategory(c.category, uiLang),
                                            numText(c.count.toLong()),
                                            pctText(c.percentage),
                                            moneyText(c.amount)
                                        )
                                    }
                                    MonthlyDataTable(
                                        headers = listOf(
                                            ShopLocale.get("col_category", uiLang),
                                            ShopLocale.get("mr_col_count", uiLang),
                                            ShopLocale.get("mr_col_share", uiLang),
                                            ShopLocale.get("col_amount", uiLang)
                                        ),
                                        rows = expRows,
                                        totals = listOf(
                                            if (isBn) "মোট খরচ" else "Total Expenses",
                                            numText(reportData.totalExpenseCount),
                                            pctText(100.0),
                                            moneyText(reportData.totalExpenses)
                                        ),
                                        numericColumns = setOf(1, 2, 3),
                                        minColumnWidth = 88.dp
                                    )
                                }

                                SectionSubheading(ShopLocale.get("mr_income_by_cat", uiLang))
                                if (reportData.incomesByCategory.isEmpty()) {
                                    SectionEmptyMessage(if (isBn) "এই মাসে কোনো অন্যান্য আয় নেই" else "No other income this month")
                                } else {
                                    val incRows = reportData.incomesByCategory.map { c ->
                                        listOf(
                                            ShopLocale.displayCategory(c.category, uiLang),
                                            numText(c.count.toLong()),
                                            pctText(c.percentage),
                                            moneyText(c.amount)
                                        )
                                    }
                                    MonthlyDataTable(
                                        headers = listOf(
                                            ShopLocale.get("col_category", uiLang),
                                            ShopLocale.get("mr_col_count", uiLang),
                                            ShopLocale.get("mr_col_share", uiLang),
                                            ShopLocale.get("col_amount", uiLang)
                                        ),
                                        rows = incRows,
                                        totals = listOf(
                                            ShopLocale.get("mr_other_income", uiLang),
                                            numText(reportData.totalOtherIncomeCount),
                                            pctText(100.0),
                                            moneyText(reportData.otherIncome)
                                        ),
                                        numericColumns = setOf(1, 2, 3),
                                        minColumnWidth = 88.dp
                                    )
                                }
                            }
                        }
                    }

                    // 8. CASH & BANK (Full per-account table)
                    item(key = "section_cash") {
                        CollapsibleMonthlySection(
                            title = ShopLocale.get("mr_sec_cash", uiLang),
                            keyMetric = moneyText(reportData.cashClosingBalance),
                            keyColor = VividGreen,
                            icon = Icons.Default.AccountBalanceWallet,
                            expanded = expandedSections["cash"] == true,
                            onToggle = { expandedSections["cash"] = !(expandedSections["cash"] ?: false) },
                            isBn = isBn,
                            testTag = "section_cash_card"
                        ) {
                            if (reportData.accountCashRows.isEmpty()) {
                                SectionEmptyMessage(noSectionDataText)
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionNumbersCheckLine(
                                    check = reportData.cashCheck,
                                    lang = uiLang,
                                    plainBangla = false,
                                    testTag = "check_cash_line"
                                )
                            } else {
                                MetricRow(
                                    label = ShopLocale.get("mr_col_opening", uiLang),
                                    value = moneyText(reportData.cashOpeningTotal)
                                )
                                MetricRow(
                                    label = ShopLocale.get("money_in_label", uiLang),
                                    value = moneyText(reportData.cashInTotal),
                                    valueColor = VividGreen
                                )
                                MetricRow(
                                    label = ShopLocale.get("money_out_label", uiLang),
                                    value = moneyText(reportData.cashOutTotal),
                                    valueColor = AlertRed
                                )
                                MetricRow(
                                    label = ShopLocale.get("mr_col_closing", uiLang),
                                    value = moneyText(reportData.cashClosingBalance),
                                    valueColor = VividGreen,
                                    bold = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                SectionNumbersCheckLine(
                                    check = reportData.cashCheck,
                                    lang = uiLang,
                                    plainBangla = false,
                                    testTag = "check_cash_line"
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                val cashRows = reportData.accountCashRows.map { acc ->
                                    listOf(
                                        ShopLocale.displayAccountName(acc.accountName, acc.accountId, uiLang),
                                        moneyText(acc.opening),
                                        moneyText(acc.moneyIn),
                                        moneyText(acc.moneyOut),
                                        moneyText(acc.closing)
                                    )
                                }
                                MonthlyDataTable(
                                    headers = listOf(
                                        ShopLocale.get("col_account", uiLang),
                                        ShopLocale.get("mr_col_opening", uiLang),
                                        ShopLocale.get("col_money_in", uiLang),
                                        ShopLocale.get("col_money_out", uiLang),
                                        ShopLocale.get("mr_col_closing", uiLang)
                                    ),
                                    rows = cashRows,
                                    totals = listOf(
                                        if (isBn) "মোট" else "Total",
                                        moneyText(reportData.cashOpeningTotal),
                                        moneyText(reportData.cashInTotal),
                                        moneyText(reportData.cashOutTotal),
                                        moneyText(reportData.cashClosingBalance)
                                    ),
                                    numericColumns = setOf(1, 2, 3, 4),
                                    minColumnWidth = 90.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportLanguageModal && !isRestrictedMode) {
        Dialog(
            onDismissRequest = { showExportLanguageModal = false },
            properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
        ) {
            ConfigureEdgeToEdgeDialogWindow()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkBackground,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDialogWindowPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                var chosenLang by remember(currentLanguage) { mutableStateOf(currentLanguage) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = VividGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "রিপোর্টের ভাষা বাছাই করুন" else "Choose Report Language",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            softWrap = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { chosenLang = AppLanguage.EN }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = chosenLang == AppLanguage.EN,
                            onClick = { chosenLang = AppLanguage.EN },
                            colors = RadioButtonDefaults.colors(selectedColor = VividGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "ইংরেজি (সর্বজনীন হিসাবের জন্য)" else "English (Universal accounting)",
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            maxLines = 2,
                            softWrap = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { chosenLang = AppLanguage.BN }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = chosenLang == AppLanguage.BN,
                            onClick = { chosenLang = AppLanguage.BN },
                            colors = RadioButtonDefaults.colors(selectedColor = VividGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "বাংলা (Bengali Unicode)",
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            maxLines = 2,
                            softWrap = true
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showExportLanguageModal = false },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(
                                text = if (isBn) "বাতিল" else "Cancel",
                                color = TextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                showExportLanguageModal = false
                                runExport(selectedExportFormat, chosenLang)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VividGreen),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            val exportLabel = if (isBn) {
                                if (selectedExportFormat == "PDF") "PDF ডাউনলোড" else "CSV ডাউনলোড"
                            } else {
                                "Export $selectedExportFormat"
                            }
                            Text(
                                text = exportLabel,
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
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

@Composable
private fun BakiSectionContent(
    reportData: com.example.domain.MonthlyReportData,
    uiLang: AppLanguage,
    isBn: Boolean,
    plainBangla: Boolean,
    noSectionDataText: String,
    emptyMonthFriendly: String,
    txt: (String, String) -> String,
    moneyText: (Double) -> String,
    numText: (Long) -> String
) {
    if (!reportData.bakiHasActivityOrDue) {
        SectionEmptyMessage(if (reportData.isMonthEmpty) emptyMonthFriendly else noSectionDataText)
        Spacer(modifier = Modifier.height(8.dp))
        SectionNumbersCheckLine(
            check = reportData.bakiCheck,
            lang = uiLang,
            plainBangla = plainBangla,
            testTag = "check_baki_line"
        )
    } else {
        MetricRow(
            label = txt(if (isBn) "শুরুতে বাকি" else "Opening Receivable", "শুরুতে বাকি"),
            value = moneyText(reportData.bakiOpeningReceivable)
        )
        MetricRow(
            label = txt(if (isBn) "এই মাসে নতুন বাকি" else "New Baki This Month", "নতুন বাকি"),
            value = moneyText(reportData.bakiNewCreditGiven),
            valueColor = AmberWarning
        )
        MetricRow(
            label = txt(if (isBn) "এই মাসে আদায়" else "Payments Received", "আদায়"),
            value = moneyText(reportData.bakiCollectedThisMonth),
            valueColor = VividGreen
        )
        MetricRow(
            label = txt(if (isBn) "শেষে বাকি আছে" else "Closing Receivable", "শেষে বাকি আছে"),
            value = moneyText(reportData.bakiClosingReceivable),
            valueColor = AmberWarning,
            bold = true
        )

        Spacer(modifier = Modifier.height(8.dp))
        SectionNumbersCheckLine(
            check = reportData.bakiCheck,
            lang = uiLang,
            plainBangla = plainBangla,
            testTag = "check_baki_line"
        )

        SectionSubheading(txt(if (isBn) "বকেয়ার মেয়াদ ভাগ" else "Ageing Summary", "বকেয়ার মেয়াদ ভাগ"))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgeingBucketChip(
                    label = txt(ShopLocale.get("mr_bucket_0_30", uiLang), "০-৩০ দিন"),
                    value = moneyText(reportData.bakiAgeing.bucket0To30),
                    accent = VividGreen,
                    modifier = Modifier.weight(1f)
                )
                AgeingBucketChip(
                    label = txt(ShopLocale.get("mr_bucket_31_60", uiLang), "৩১-৬০ দিন"),
                    value = moneyText(reportData.bakiAgeing.bucket31To60),
                    accent = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgeingBucketChip(
                    label = txt(ShopLocale.get("mr_bucket_61_90", uiLang), "৬১-৯০ দিন"),
                    value = moneyText(reportData.bakiAgeing.bucket61To90),
                    accent = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                AgeingBucketChip(
                    label = txt(ShopLocale.get("mr_bucket_90_plus", uiLang), "৯০+ দিন"),
                    value = moneyText(reportData.bakiAgeing.bucket90Plus),
                    accent = AlertRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SectionSubheading(
            txt(
                if (isBn) "বাকি থাকা ক্রেতার তালিকা (${numText(reportData.debtorCount)} জন)"
                else "Customers Owing (${numText(reportData.debtorCount)})",
                "বাকি থাকা ক্রেতার তালিকা"
            )
        )
        if (reportData.debtorCount == 0L) {
            SectionEmptyMessage(txt(if (isBn) "মাস শেষে কোনো বাকি নেই" else "No customers owe baki at month end", "মাস শেষে কোনো বাকি নেই"))
        } else {
            reportData.bakiAgeingGroups.forEach { group ->
                if (group.debtors.isNotEmpty()) {
                    val bucketLabel = when (group.bucketKey) {
                        "0-30" -> txt(ShopLocale.get("mr_bucket_0_30", uiLang), "০-৩০ দিন")
                        "31-60" -> txt(ShopLocale.get("mr_bucket_31_60", uiLang), "৩১-৬০ দিন")
                        "61-90" -> txt(ShopLocale.get("mr_bucket_61_90", uiLang), "৬১-৯০ দিন")
                        else -> txt(ShopLocale.get("mr_bucket_90_plus", uiLang), "৯০+ দিন")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$bucketLabel (${numText(group.count)})",
                        color = when (group.bucketKey) {
                            "0-30" -> VividGreen
                            "31-60", "61-90" -> AmberWarning
                            else -> AlertRed
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val rows = group.debtors.map { d ->
                        val phoneOrDash = if (d.customerPhone.isBlank()) "—" else txt(ShopLocale.digits(d.customerPhone, uiLang), "—")
                        if (plainBangla) {
                            listOf(
                                txt(d.customerName, "ক্রেতা"),
                                "${numText(d.daysOverdue)} দিন",
                                moneyText(d.amountOwed)
                            )
                        } else {
                            listOf(
                                d.customerName,
                                phoneOrDash,
                                "${numText(d.daysOverdue)}",
                                moneyText(d.amountOwed)
                            )
                        }
                    }
                    val headers = if (plainBangla) {
                        listOf("ক্রেতা", "বকেয়ার দিন", "বাকি টাকা")
                    } else {
                        listOf(
                            ShopLocale.get("col_customer", uiLang),
                            if (isBn) "ফোন" else "Phone",
                            ShopLocale.get("mr_col_days_overdue", uiLang),
                            ShopLocale.get("col_amount", uiLang)
                        )
                    }
                    MonthlyDataTable(
                        headers = headers,
                        rows = rows,
                        numericColumns = if (plainBangla) setOf(1, 2) else setOf(2, 3),
                        minColumnWidth = if (plainBangla) 92.dp else 84.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyDataTable(
    headers: List<String>,
    rows: List<List<String>>,
    totals: List<String>? = null,
    numericColumns: Set<Int> = emptySet(),
    minColumnWidth: Dp = 88.dp
) {
    val scrollState = rememberScrollState()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x0DFFFFFF),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Column {
                // Table Header
                Row(
                    modifier = Modifier
                        .background(Color(0xFF132920))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headers.forEachIndexed { colIdx, head ->
                        val isNum = colIdx in numericColumns
                        Text(
                            text = head,
                            color = VividGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = if (isNum) TextAlign.End else TextAlign.Start,
                            maxLines = 2,
                            softWrap = true,
                            modifier = Modifier
                                .widthIn(min = if (colIdx == 0 && headers.size <= 4) 115.dp else minColumnWidth, max = 180.dp)
                                .padding(horizontal = 6.dp)
                        )
                    }
                }
                HorizontalDivider(color = Color(0x3322C55E))

                // Table Rows
                rows.forEachIndexed { rIdx, row ->
                    val bgColor = if (rIdx % 2 == 1) Color(0x08FFFFFF) else Color.Transparent
                    Row(
                        modifier = Modifier
                            .background(bgColor)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        headers.indices.forEach { colIdx ->
                            val isNum = colIdx in numericColumns
                            val cellVal = row.getOrElse(colIdx) { "" }
                            Text(
                                text = cellVal,
                                color = if (colIdx == headers.lastIndex && isNum) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (colIdx == 0 || (colIdx == headers.lastIndex && isNum)) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = if (isNum) TextAlign.End else TextAlign.Start,
                                maxLines = 2,
                                softWrap = true,
                                modifier = Modifier
                                    .widthIn(min = if (colIdx == 0 && headers.size <= 4) 115.dp else minColumnWidth, max = 180.dp)
                                    .padding(horizontal = 6.dp)
                            )
                        }
                    }
                    if (rIdx < rows.lastIndex) {
                        HorizontalDivider(color = Color(0x14FFFFFF))
                    }
                }

                // Totals Row
                if (totals != null) {
                    HorizontalDivider(color = Color(0x3322C55E))
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF10221A))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        headers.indices.forEach { colIdx ->
                            val isNum = colIdx in numericColumns
                            Text(
                                text = totals.getOrElse(colIdx) { "" },
                                color = VividGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = if (isNum) TextAlign.End else TextAlign.Start,
                                maxLines = 2,
                                softWrap = true,
                                modifier = Modifier
                                    .widthIn(min = if (colIdx == 0 && headers.size <= 4) 115.dp else minColumnWidth, max = 180.dp)
                                    .padding(horizontal = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionNumbersCheckLine(
    check: MonthlyReconciliationCheck,
    lang: AppLanguage,
    plainBangla: Boolean,
    testTag: String
) {
    val lineText = ReportDataBuilders.formatMonthlyCheckLine(check, lang, plainBangla = plainBangla)
    val bgColor = when {
        !check.isAvailable -> Color(0x18FFFFFF)
        check.matches -> VividGreen.copy(alpha = 0.10f)
        else -> AlertRed.copy(alpha = 0.16f)
    }
    val borderColor = when {
        !check.isAvailable -> Color(0x33FFFFFF)
        check.matches -> VividGreen.copy(alpha = 0.35f)
        else -> AlertRed.copy(alpha = 0.55f)
    }
    val tintColor = when {
        !check.isAvailable -> TextSecondary
        check.matches -> VividGreen
        else -> AmberWarning
    }
    val icon = when {
        !check.isAvailable -> Icons.Default.Info
        check.matches -> Icons.Default.CheckCircle
        else -> Icons.Default.Warning
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = lineText,
                color = if (!check.matches && check.isAvailable) AmberWarning else TextPrimary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                fontWeight = if (!check.matches) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class SummaryKpi(
    val label: String,
    val value: String,
    val color: Color
)

@Composable
private fun SummaryKpiGrid(
    items: List<SummaryKpi>,
    isLargeText: Boolean
) {
    val rows = remember(items) { items.chunked(2) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { kpi ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x1AFFFFFF),
                        border = BorderStroke(1.dp, Color(0x1FFFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                text = kpi.label,
                                color = TextSecondary,
                                fontSize = if (isLargeText) 13.sp else 11.5.sp,
                                lineHeight = 15.sp,
                                maxLines = 2,
                                softWrap = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = kpi.value,
                                color = kpi.color,
                                fontSize = if (isLargeText) 17.sp else 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                softWrap = true
                            )
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CollapsibleMonthlySection(
    title: String,
    keyMetric: String,
    keyColor: Color,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    isBn: Boolean,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        testTag = testTag,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = keyColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, keyColor.copy(alpha = 0.30f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = keyColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = keyMetric,
                    color = keyColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isBn) {
                        if (expanded) "বন্ধ করুন" else "খুলুন"
                    } else {
                        if (expanded) "Collapse" else "Expand"
                    },
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(color = Color(0x1FFFFFFF))
                    Spacer(modifier = Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (bold) TextPrimary else TextSecondary,
            fontSize = if (bold) 13.5.sp else 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            softWrap = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = if (bold) 14.sp else 13.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SectionSubheading(text: String) {
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = text,
        color = VividGreen,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        softWrap = true
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun SectionEmptyMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            softWrap = true
        )
    }
}

@Composable
private fun AgeingBucketChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                softWrap = true,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                color = accent,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun LocalizedMonthlyExportBar(
    isBn: Boolean,
    isRestrictedMode: Boolean,
    onDownloadPdf: () -> Unit,
    onPrint: () -> Unit,
    onExportCsv: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonthlyExportChip(
            label = when {
                isRestrictedMode -> ShopLocale.get("plain_export_pdf", AppLanguage.BN)
                isBn -> "PDF ডাউনলোড"
                else -> "Download PDF"
            },
            icon = Icons.Default.Download,
            testTag = "btn_download_pdf",
            onClick = onDownloadPdf
        )
        MonthlyExportChip(
            label = when {
                isRestrictedMode -> ShopLocale.get("plain_export_print", AppLanguage.BN)
                isBn -> "প্রিন্ট"
                else -> "Print"
            },
            icon = Icons.Default.Print,
            testTag = "btn_print",
            onClick = onPrint
        )
        MonthlyExportChip(
            label = when {
                isRestrictedMode -> ShopLocale.get("plain_export_excel", AppLanguage.BN)
                isBn -> "Excel/CSV"
                else -> "Export Excel/CSV"
            },
            icon = Icons.Default.TableChart,
            testTag = "btn_export_csv",
            onClick = onExportCsv
        )
        MonthlyExportChip(
            label = when {
                isRestrictedMode -> ShopLocale.get("plain_export_share", AppLanguage.BN)
                isBn -> "শেয়ার"
                else -> "Share"
            },
            icon = Icons.Default.Share,
            testTag = "btn_share",
            onClick = onShare
        )
    }
}

@Composable
private fun MonthlyExportChip(
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = Color(0x1FFFFFFF),
        border = BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VividGreen,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
