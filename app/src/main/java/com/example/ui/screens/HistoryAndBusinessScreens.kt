package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AccountAdjustment
import com.example.data.model.CompletedSaleReceipt
import com.example.data.model.Expense
import com.example.domain.CalculationEngine
import com.example.data.model.OtherIncome
import com.example.data.model.Payment
import com.example.data.model.Purchase
import com.example.data.model.Sale
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.GlassCard
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.HistoryFilter
import com.example.ui.viewmodel.HistoryPeriod
import com.example.ui.viewmodel.HistoryTimelineItem
import com.example.ui.viewmodel.HistoryViewMode
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ComingSoonScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("coming_soon_${title.lowercase()}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0x265DD62C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VividGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x335DD62C)
                ) {
                    Text(
                        text = "COMING SOON",
                        color = VividGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private val HistoryAmber = Color(0xFFF3B23E)

@Composable
fun HistoryScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onNavigateToStock: (String?) -> Unit = {}
) {
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val period by storeViewModel.historyPeriod.collectAsState()
    val filter by storeViewModel.historyFilter.collectAsState()
    val viewMode by storeViewModel.historyViewMode.collectAsState()
    val searchQuery by storeViewModel.historySearchQuery.collectAsState()
    val summary by storeViewModel.historySummary.collectAsState()
    val timelineItems by storeViewModel.historyTimelineItems.collectAsState()
    val productsSold by storeViewModel.historyProductsSold.collectAsState()
    val isGrid by storeViewModel.productsSoldIsGrid.collectAsState()
    val selectedSale by storeViewModel.selectedSaleDetail.collectAsState()
    val customRange by storeViewModel.historyCustomDateRange.collectAsState()
    val members by storeViewModel.members.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val isPrivacyHidden by storeViewModel.isPrivacyHidden.collectAsState()

    LaunchedEffect(isFatherMode) {
        if (isFatherMode && period != HistoryPeriod.TODAY) {
            storeViewModel.setHistoryPeriod(HistoryPeriod.TODAY)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedPurchase by remember { mutableStateOf<Purchase?>(null) }

    // If a sale is selected, show the full read-only Sale Details Screen
    if (selectedSale != null) {
        SaleDetailsScreen(
            sale = selectedSale!!,
            currentLanguage = currentLanguage,
            members = members,
            ownerUid = store?.ownerId ?: "",
            storeName = store?.name?.ifBlank { "StoreOS" } ?: "StoreOS",
            canSeeProfit = canSeeProfit,
            onBack = { storeViewModel.selectSaleDetail(null) }
        )
        return
    }

    if (onBack != null) {
        BackHandler(enabled = selectedPurchase == null) {
            onBack()
        }
    }

    if (selectedPurchase != null) {
        PurchaseDetailsDialog(
            purchase = selectedPurchase!!,
            currentLanguage = currentLanguage,
            onDismiss = { selectedPurchase = null }
        )
    }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            initialRange = customRange,
            currentLanguage = currentLanguage,
            onDismiss = { showDatePicker = false },
            onConfirm = { startMillis, endMillis ->
                storeViewModel.setHistoryCustomDateRange(startMillis, endMillis)
                showDatePicker = false
            }
        )
    }

    // Group timeline items by day
    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayKey = remember { dayFormat.format(Date()) }
    val yesterdayKey = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        dayFormat.format(cal.time)
    }

    val groupedItems = remember(timelineItems) {
        val map = linkedMapOf<String, MutableList<HistoryTimelineItem>>()
        for (item in timelineItems) {
            val date = Date(item.timestamp)
            val key = dayFormat.format(date)
            map.getOrPut(key) { mutableListOf() }.add(item)
        }
        map
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.50f, size.height * 0.18f),
                        radius = size.width * 0.85f
                    )
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("history_screen_list"),
            contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact Header: Back arrow + "History" + Calendar icon button
            item(key = "history_top_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("history_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isFatherMode) {
                                ShopLocale.get("father_todays_history_title", currentLanguage)
                            } else if (currentLanguage == AppLanguage.BN) "ইতিহাস" else "History",
                            color = TextPrimary,
                            fontSize = if (isFatherMode) 23.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!isFatherMode) {
                        // Rounded-square dark glass calendar button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(
                                    onClickLabel = if (currentLanguage == AppLanguage.BN) "তারিখ নির্বাচন করুন" else "Select Date Range",
                                    onClick = { showDatePicker = true }
                                )
                                .testTag("history_calendar_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (period == HistoryPeriod.CUSTOM) VividGreen.copy(alpha = 0.85f) else Color(0x34FFFFFF)
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = if (period == HistoryPeriod.CUSTOM) {
                                                    listOf(Color(0xFF1F3619), Color(0xFF131D11))
                                                } else {
                                                    listOf(Color(0xFF282C28), Color(0xFF161816))
                                                }
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = if (currentLanguage == AppLanguage.BN) "তারিখ নির্বাচন" else "Select custom date range",
                                        tint = if (period == HistoryPeriod.CUSTOM) VividGreen else TextPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
                // 1. Period tabs (or locked Today badge in Father mode)
                item(key = "history_period_tabs") {
                    if (isFatherMode) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1F3619),
                            border = BorderStroke(1.2.dp, VividGreen.copy(alpha = 0.85f)),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .testTag("history_today_locked_pill")
                        ) {
                            Text(
                                text = ShopLocale.get("reports_today_only_hint", currentLanguage),
                                color = VividGreen,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        HistoryPeriodTabs(
                            currentPeriod = period,
                            customRange = customRange,
                            currentLanguage = currentLanguage,
                            onSelectPeriod = { selected ->
                                if (selected == HistoryPeriod.CUSTOM) {
                                    showDatePicker = true
                                } else {
                                    storeViewModel.setHistoryPeriod(selected)
                                }
                            }
                        )
                    }
                }

                // 2. Bought / Sold / Profit Hero Card
                item(key = "history_summary_hero") {
                    HistorySummaryCard(
                        summary = summary,
                        currentLanguage = currentLanguage,
                        canSeeProfit = canSeeProfit,
                        isPrivacyHidden = isPrivacyHidden,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 3. Search Bar
                item(key = "history_search_bar") {
                    HistorySearchBar(
                        query = searchQuery,
                        onQueryChange = { storeViewModel.setHistorySearchQuery(it) },
                        currentLanguage = currentLanguage,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 4. Transactions / Products Sold (N) Segmented Tabs
                item(key = "history_mode_switcher") {
                    HistoryViewModeSwitcher(
                        currentMode = viewMode,
                        productsSoldCount = productsSold.size,
                        currentLanguage = currentLanguage,
                        isGrid = isGrid,
                        onSelectMode = { storeViewModel.setHistoryViewMode(it) },
                        onToggleGrid = { storeViewModel.toggleProductsSoldGrid() },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 5. All / Sales Only / Purchases Only Filter Chips + Grid Toggle (when Products Sold is active)
                item(key = "history_filter_chips") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HistoryFilterChips(
                            currentFilter = filter,
                            currentLanguage = currentLanguage,
                            includePurchases = !isFatherMode,
                            onSelectFilter = { storeViewModel.setHistoryFilter(it) },
                            modifier = Modifier.weight(1f)
                        )
                        if (viewMode == HistoryViewMode.PRODUCTS_SOLD) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xD9191C19))
                                    .border(
                                        width = 1.dp,
                                        color = if (isGrid) VividGreen.copy(alpha = 0.72f) else Color(0x2EFFFFFF),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(
                                        onClickLabel = if (isGrid) "Switch to List View" else "Switch to Grid View",
                                        onClick = { storeViewModel.toggleProductsSoldGrid() }
                                    )
                                    .testTag("toggle_products_grid"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = if (isGrid) "Switch to List View" else "Switch to Grid View",
                                    tint = if (isGrid) VividGreen else TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 6. Content Section: Day-grouped Transactions OR Products Sold
                if (viewMode == HistoryViewMode.TRANSACTIONS) {
                    if (timelineItems.isEmpty()) {
                        item(key = "history_empty_transactions") {
                            val emptyTitle = when {
                                searchQuery.isNotBlank() ->
                                    if (currentLanguage == AppLanguage.BN) "কোনো ফলাফল পাওয়া যায়নি" else "No matching transactions"
                                filter == HistoryFilter.PURCHASES_ONLY ->
                                    if (currentLanguage == AppLanguage.BN) "এই সময়ে কোনো ক্রয় নেই" else "No purchases in this period"
                                else ->
                                    if (currentLanguage == AppLanguage.BN) "এই সময়ে কোনো লেনদেন নেই" else "No transactions recorded in this period"
                            }
                            val emptySub = when {
                                searchQuery.isNotBlank() ->
                                    if (currentLanguage == AppLanguage.BN) "অন্য ইনভয়েস, গ্রাহক বা পণ্যের নাম দিয়ে চেষ্টা করুন।" else "Try searching by another invoice, customer, supplier, or product."
                                filter == HistoryFilter.PURCHASES_ONLY ->
                                    if (currentLanguage == AppLanguage.BN) "নতুন ক্রয় যোগ করলে এখানে দেখা যাবে।" else "Recorded purchases for this period will appear here."
                                else ->
                                    if (currentLanguage == AppLanguage.BN) "নতুন বিক্রয়, ক্রয় বা বাকি আদায় করলে এখানে দেখা যাবে।" else "Sales, purchases, and payments will appear here."
                            }
                            HistoryEmptyStateCard(
                                title = emptyTitle,
                                subtitle = emptySub,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        groupedItems.forEach { (dateKey, itemsInDay) ->
                            item(key = "header_$dateKey") {
                                val parsed = remember(dateKey) {
                                    try { dayFormat.parse(dateKey) } catch (_: Exception) { null }
                                }
                                val formattedDatePart = remember(parsed, dateKey, currentLanguage) {
                                    parsed?.let { ShopLocale.formatDateOnly(it.time, currentLanguage) } ?: dateKey
                                }
                                val headerLabel = when (dateKey) {
                                    todayKey -> if (currentLanguage == AppLanguage.BN) {
                                        "আজকে · $formattedDatePart"
                                    } else {
                                        "Today · $formattedDatePart"
                                    }
                                    yesterdayKey -> if (currentLanguage == AppLanguage.BN) {
                                        "গতকাল · $formattedDatePart"
                                    } else {
                                        "Yesterday · $formattedDatePart"
                                    }
                                    else -> parsed?.let { ShopLocale.formatWeekdayDate(it.time, currentLanguage) } ?: dateKey
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 0.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = headerLabel,
                                        color = TextSecondary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) {
                                            "${ShopLocale.formatNumber(itemsInDay.size.toLong(), currentLanguage)}টি"
                                        } else {
                                            "${itemsInDay.size} ${if (itemsInDay.size == 1) "item" else "items"}"
                                        },
                                        color = TextMuted,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            items(itemsInDay, key = { it.id }) { item ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    when (item) {
                                        is HistoryTimelineItem.SaleItemRow -> {
                                            HistorySaleRow(
                                                sale = item.sale,
                                                currentLanguage = currentLanguage,
                                                members = members,
                                                ownerUid = store?.ownerId ?: "",
                                                onClick = { storeViewModel.selectSaleDetail(item.sale) }
                                            )
                                        }
                                        is HistoryTimelineItem.PurchaseItemRow -> {
                                            HistoryPurchaseRow(
                                                purchase = item.purchase,
                                                currentLanguage = currentLanguage,
                                                onClick = { selectedPurchase = item.purchase }
                                            )
                                        }
                                        is HistoryTimelineItem.PaymentItemRow -> {
                                            HistoryPaymentRow(
                                                payment = item.payment,
                                                currentLanguage = currentLanguage
                                            )
                                        }
                                        is HistoryTimelineItem.ExpenseItemRow -> {
                                            HistoryExpenseRow(
                                                expense = item.expense,
                                                currentLanguage = currentLanguage
                                            )
                                        }
                                        is HistoryTimelineItem.OtherIncomeItemRow -> {
                                            HistoryOtherIncomeRow(
                                                income = item.income,
                                                currentLanguage = currentLanguage
                                            )
                                        }
                                        is HistoryTimelineItem.AdjustmentItemRow -> {
                                            HistoryAdjustmentRow(
                                                adjustment = item.adjustment,
                                                currentLanguage = currentLanguage
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    historyProductsSoldLazyItems(
                        productsSold = productsSold,
                        isGrid = isGrid,
                        searchQuery = searchQuery,
                        currentLanguage = currentLanguage,
                        canSeeProfit = canSeeProfit,
                        onProductClick = { stat -> onNavigateToStock(stat.productName) }
                    )
            }
        }
    }
}

@Composable
private fun HistoryEmptyStateCard(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HistorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    currentLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isFocused) Color(0xD91A1E1A) else Color(0xBF151815),
        border = BorderStroke(
            width = 1.dp,
            color = if (isFocused) VividGreen.copy(alpha = 0.72f) else Color(0x28FFFFFF)
        ),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = if (currentLanguage == AppLanguage.BN) "খুঁজুন" else "Search",
                tint = TextSecondary,
                modifier = Modifier.size(19.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) {
                            "ইনভয়েস, গ্রাহক বা পণ্য খুঁজুন..."
                        } else {
                            "Search invoice, customer, product..."
                        },
                        color = TextSecondary,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    cursorBrush = SolidColor(VividGreen),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 13.5.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .testTag("history_search_input")
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("history_search_clear")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (currentLanguage == AppLanguage.BN) "মুছুন" else "Clear search",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Period tabs: Today, Week, Month, Year, All, Custom Date
 */
@Composable
fun HistoryPeriodTabs(
    currentPeriod: HistoryPeriod,
    customRange: Pair<Long, Long>?,
    currentLanguage: AppLanguage,
    onSelectPeriod: (HistoryPeriod) -> Unit,
    includeCustomDate: Boolean = true
) {
    val periods = remember(includeCustomDate) {
        if (includeCustomDate) {
            HistoryPeriod.values().toList()
        } else {
            HistoryPeriod.values().filter { it != HistoryPeriod.CUSTOM }
        }
    }
    val capsuleShape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = capsuleShape,
        color = Color(0xEB151815),
        border = BorderStroke(1.dp, Color(0x28FFFFFF))
    ) {
        val showRangeText = currentPeriod == HistoryPeriod.CUSTOM && customRange != null
        val fontScale = LocalDensity.current.fontScale

        val rowContent: @Composable (HistoryPeriod, Modifier, Boolean) -> Unit = { p, itemMod, isScrollable ->
            val isSelected = currentPeriod == p
            val label = when {
                p == HistoryPeriod.CUSTOM && customRange != null -> {
                    val s = ShopLocale.formatDayMonth(customRange.first, currentLanguage)
                    val e = ShopLocale.formatDayMonth(customRange.second, currentLanguage)
                    "$s–$e"
                }
                p == HistoryPeriod.CUSTOM -> {
                    if (currentLanguage == AppLanguage.BN) "তারিখ" else "Custom"
                }
                else -> {
                    if (currentLanguage == AppLanguage.BN) p.labelBn else p.labelEn
                }
            }
            val pillShape = RoundedCornerShape(20.dp)
            Surface(
                modifier = itemMod
                    .clip(pillShape)
                    .clickable { onSelectPeriod(p) }
                    .testTag("period_tab_${p.name.lowercase()}"),
                shape = pillShape,
                color = if (isSelected) Color(0xFF1F3619) else Color.Transparent,
                border = if (isSelected) BorderStroke(1.2.dp, VividGreen.copy(alpha = 0.85f)) else null
            ) {
                Box(
                    modifier = Modifier.padding(
                        horizontal = if (isScrollable) 12.dp else 4.dp,
                        vertical = 7.dp
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val useScroll = showRangeText || maxWidth < 320.dp || fontScale > 1.15f
            if (useScroll) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(periods.size) { index ->
                        rowContent(periods[index], Modifier, true)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    periods.forEach { p ->
                        val weightValue = when (p) {
                            HistoryPeriod.TODAY -> 1.05f
                            HistoryPeriod.WEEK -> 0.98f
                            HistoryPeriod.MONTH -> 1.12f
                            HistoryPeriod.YEAR -> 0.88f
                            HistoryPeriod.ALL -> 0.72f
                            HistoryPeriod.CUSTOM -> 1.25f
                        }
                        rowContent(p, Modifier.weight(weightValue), false)
                    }
                }
            }
        }
    }
}

/**
 * Filter chips: All, Sales Only, Purchases Only
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryFilterChips(
    currentFilter: HistoryFilter,
    currentLanguage: AppLanguage,
    onSelectFilter: (HistoryFilter) -> Unit,
    modifier: Modifier = Modifier,
    includePurchases: Boolean = true
) {
    val filters = remember(includePurchases) {
        if (includePurchases) HistoryFilter.values().toList()
        else HistoryFilter.values().filter { it != HistoryFilter.PURCHASES_ONLY }
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { f ->
            val isSelected = currentFilter == f
            val label = if (currentLanguage == AppLanguage.BN) f.labelBn else f.labelEn

            Surface(
                modifier = Modifier
                    .defaultMinSize(minHeight = 38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectFilter(f) }
                    .testTag("filter_chip_${f.name.lowercase()}"),
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) Color(0xFF1F231F) else Color(0xCC151815),
                border = BorderStroke(
                    width = if (isSelected) 1.2.dp else 1.dp,
                    color = if (isSelected) Color(0xCCFFFFFF) else Color(0x28FFFFFF)
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Glowing Bought / Sold / Profit Summary Hero Card
 */
@Composable
fun HistorySummaryCard(
    summary: com.example.domain.PeriodFinancialSummary,
    currentLanguage: AppLanguage,
    canSeeProfit: Boolean = true,
    isPrivacyHidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    val boughtText = formatHomeMoney(summary.totalBought, currentLanguage, isPrivacyHidden)
    val soldText = formatHomeMoney(summary.totalSold, currentLanguage, isPrivacyHidden)
    val thirdValueText = if (canSeeProfit) {
        formatHomeMoney(summary.totalProfit, currentLanguage, isPrivacyHidden)
    } else {
        ShopLocale.formatNumber(summary.totalItemsSold, currentLanguage)
    }

    val maxLen = maxOf(boughtText.length, soldText.length, thirdValueText.length)
    val valueFontSize = when {
        maxLen <= 6 -> 20.sp
        maxLen <= 8 -> 17.sp
        maxLen <= 10 -> 14.5.sp
        else -> 12.5.sp
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = cardShape,
                spotColor = VividGreen.copy(alpha = 0.45f),
                ambientColor = VividGreen.copy(alpha = 0.28f)
            )
            .clip(cardShape)
            .testTag("history_summary_card"),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    VividGreen.copy(alpha = 0.88f),
                    VividGreen.copy(alpha = 0.36f)
                )
            )
        )
    ) {
        Box(
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
        ) {
            // Subtle decorative translucent wave curve in background
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                val w = size.width
                val h = size.height
                val waveStroke = Path().apply {
                    moveTo(0f, h * 0.78f)
                    cubicTo(w * 0.16f, h * 0.52f, w * 0.28f, h * 0.88f, w * 0.44f, h * 0.68f)
                    cubicTo(w * 0.58f, h * 0.50f, w * 0.70f, h * 0.76f, w * 0.82f, h * 0.62f)
                    cubicTo(w * 0.90f, h * 0.52f, w * 0.95f, h * 0.40f, w, h * 0.46f)
                }
                val waveFill = Path().apply {
                    addPath(waveStroke)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = waveFill,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        startY = h * 0.40f,
                        endY = h
                    )
                )
                drawPath(
                    path = waveStroke,
                    color = Color(0x26FFFFFF),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // BOUGHT column (Warm Amber)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "ক্রয়" else "BOUGHT",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = boughtText,
                        color = HistoryAmber,
                        fontSize = valueFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }

                // SOLD column (Crisp White)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "বিক্রয়" else "SOLD",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = soldText,
                        color = TextPrimary,
                        fontSize = valueFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }

                // PROFIT (or ITEMS SOLD if profit is masked) column (Vivid Green)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (canSeeProfit) {
                            if (currentLanguage == AppLanguage.BN) "লাভ" else "PROFIT"
                        } else {
                            if (currentLanguage == AppLanguage.BN) "পণ্য" else "ITEMS"
                        },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = thirdValueText,
                        color = VividGreen,
                        fontSize = valueFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Sub-navigation: Transactions vs Products Sold (N)
 */
@Composable
fun HistoryViewModeSwitcher(
    currentMode: HistoryViewMode,
    productsSoldCount: Int = 0,
    currentLanguage: AppLanguage,
    isGrid: Boolean,
    onSelectMode: (HistoryViewMode) -> Unit,
    onToggleGrid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackShape = RoundedCornerShape(22.dp)
    val pillShape = RoundedCornerShape(18.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = trackShape,
        color = Color(0xEB151815),
        border = BorderStroke(1.dp, Color(0x28FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isTx = currentMode == HistoryViewMode.TRANSACTIONS
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(pillShape)
                    .clickable { onSelectMode(HistoryViewMode.TRANSACTIONS) }
                    .testTag("history_mode_transactions"),
                color = if (isTx) Color(0xFF282D28) else Color.Transparent,
                shape = pillShape,
                border = if (isTx) BorderStroke(1.dp, Color(0x38FFFFFF)) else null
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "লেনদেন" else "Transactions",
                        color = if (isTx) TextPrimary else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isTx) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            val isProducts = currentMode == HistoryViewMode.PRODUCTS_SOLD
            val countText = ShopLocale.formatNumber(productsSoldCount.toLong(), currentLanguage)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(pillShape)
                    .clickable { onSelectMode(HistoryViewMode.PRODUCTS_SOLD) }
                    .testTag("history_mode_products_sold"),
                color = if (isProducts) Color(0xFF282D28) else Color.Transparent,
                shape = pillShape,
                border = if (isProducts) BorderStroke(1.dp, Color(0x38FFFFFF)) else null
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) {
                            "বিক্রিত পণ্য ($countText)"
                        } else {
                            "Products Sold ($countText)"
                        },
                        color = if (isProducts) TextPrimary else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isProducts) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTransactionGlassCard(
    initialLetter: String,
    primaryTitle: String,
    secondaryLine: String,
    tertiaryLine: String,
    amountText: String,
    amountColor: Color,
    badgeText: String,
    badgeColor: Color,
    testTag: String,
    onClick: (() -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(18.dp)
    var cardMod = Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xEB222522), Color(0xEC141614))
            )
        )
        .testTag(testTag)

    if (onClick != null) {
        cardMod = cardMod.clickable(onClick = onClick)
    }

    Surface(
        modifier = cardMod,
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0x28FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Circular Initial Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
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
                    text = initialLetter,
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            // Middle 3-line Real Data Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = primaryTitle,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = secondaryLine,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tertiaryLine,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right Amount + Outlined Pill Badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = amountText,
                    color = amountColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.65f))
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

private fun initialCharFrom(value: String, fallback: String): String {
    val clean = value.trim()
    val first = clean.firstOrNull { it.isLetterOrDigit() }
    return first?.uppercaseChar()?.toString() ?: fallback
}

/**
 * Payment In row in History feed (does not count as sales or profit)
 */
@Composable
fun HistoryPaymentRow(
    payment: Payment,
    currentLanguage: AppLanguage
) {
    val formattedTime = remember(payment.paymentDate, payment.createdAt, currentLanguage) {
        val ts = if (payment.paymentDate > 0L) payment.paymentDate else (payment.createdAt?.toDate()?.time ?: 0L)
        if (ts > 0L) ShopLocale.formatTimeOnly(ts, currentLanguage) else "--:--"
    }
    val party = payment.customerName.ifBlank { payment.paymentNo }
    val detailLine = buildString {
        if (payment.note.isNotBlank()) {
            append(payment.note)
            append(" · ")
        } else if (payment.accountName.isNotBlank()) {
            append(ShopLocale.displayAccountName(payment.accountName, payment.accountId, currentLanguage))
            append(" · ")
        }
        append(formattedTime)
    }

    HistoryTransactionGlassCard(
        initialLetter = initialCharFrom(party, "P"),
        primaryTitle = party,
        secondaryLine = "${if (currentLanguage == AppLanguage.BN) "আদায়" else "Payment In"} · ${payment.paymentNo}",
        tertiaryLine = detailLine,
        amountText = "+ " + ShopLocale.formatMoney(payment.amount, currentLanguage),
        amountColor = VividGreen,
        badgeText = if (currentLanguage == AppLanguage.BN) "আদায়" else "Payment In",
        badgeColor = VividGreen,
        testTag = "payment_row_${payment.paymentNo}"
    )
}

@Composable
fun HistoryExpenseRow(
    expense: Expense,
    currentLanguage: AppLanguage
) {
    val formattedTime = remember(expense.date, expense.createdAt, currentLanguage) {
        val ts = if (expense.date > 0L) expense.date else (expense.createdAt?.toDate()?.time ?: 0L)
        if (ts > 0L) ShopLocale.formatTimeOnly(ts, currentLanguage) else "--:--"
    }
    val title = if (expense.category.isNotBlank()) {
        ShopLocale.displayCategory(expense.category, currentLanguage)
    } else {
        if (currentLanguage == AppLanguage.BN) "খরচ" else "Expense"
    }
    val detail = if (expense.note.isNotBlank()) "${expense.note} · $formattedTime" else formattedTime

    HistoryTransactionGlassCard(
        initialLetter = initialCharFrom(title, "E"),
        primaryTitle = title,
        secondaryLine = if (expense.accountName.isNotBlank()) {
            ShopLocale.displayAccountName(expense.accountName, expense.accountId, currentLanguage)
        } else {
            if (currentLanguage == AppLanguage.BN) "খরচ" else "Expense"
        },
        tertiaryLine = detail,
        amountText = "- " + ShopLocale.formatMoney(expense.amount, currentLanguage),
        amountColor = AlertRed,
        badgeText = if (currentLanguage == AppLanguage.BN) "খরচ" else "Expense",
        badgeColor = AlertRed,
        testTag = "expense_row_${expense.id}"
    )
}

@Composable
fun HistoryOtherIncomeRow(
    income: OtherIncome,
    currentLanguage: AppLanguage
) {
    val formattedTime = remember(income.date, income.createdAt, currentLanguage) {
        val ts = if (income.date > 0L) income.date else (income.createdAt?.toDate()?.time ?: 0L)
        if (ts > 0L) ShopLocale.formatTimeOnly(ts, currentLanguage) else "--:--"
    }
    val title = if (income.category.isNotBlank()) {
        ShopLocale.displayCategory(income.category, currentLanguage)
    } else {
        if (currentLanguage == AppLanguage.BN) "অন্যান্য আয়" else "Other Income"
    }
    val detail = if (income.note.isNotBlank()) "${income.note} · $formattedTime" else formattedTime

    HistoryTransactionGlassCard(
        initialLetter = initialCharFrom(title, "I"),
        primaryTitle = title,
        secondaryLine = if (income.accountName.isNotBlank()) {
            ShopLocale.displayAccountName(income.accountName, income.accountId, currentLanguage)
        } else {
            if (currentLanguage == AppLanguage.BN) "আয়" else "Other Income"
        },
        tertiaryLine = detail,
        amountText = "+ " + ShopLocale.formatMoney(income.amount, currentLanguage),
        amountColor = VividGreen,
        badgeText = if (currentLanguage == AppLanguage.BN) "অন্যান্য আয়" else "Income",
        badgeColor = VividGreen,
        testTag = "other_income_row_${income.id}"
    )
}

@Composable
fun HistoryAdjustmentRow(
    adjustment: AccountAdjustment,
    currentLanguage: AppLanguage
) {
    val formattedTime = remember(adjustment.createdAt, currentLanguage) {
        val ts = adjustment.createdAt?.toDate()?.time ?: 0L
        if (ts > 0L) ShopLocale.formatTimeOnly(ts, currentLanguage) else "--:--"
    }
    val isPositive = adjustment.difference >= 0.0
    val accent = if (isPositive) VividGreen else AlertRed

    HistoryTransactionGlassCard(
        initialLetter = initialCharFrom(adjustment.accountName, "A"),
        primaryTitle = if (adjustment.accountName.isNotBlank()) {
            ShopLocale.displayAccountName(adjustment.accountName, adjustment.accountId, currentLanguage)
        } else {
            if (currentLanguage == AppLanguage.BN) "অ্যাকাউন্ট" else "Account"
        },
        secondaryLine = if (currentLanguage == AppLanguage.BN) "ব্যালেন্স ঠিক করা" else "Account Adjustment",
        tertiaryLine = if (adjustment.note.isNotBlank()) "${adjustment.note} · $formattedTime" else formattedTime,
        amountText = (if (isPositive) "+ " else "- ") + ShopLocale.formatMoney(kotlin.math.abs(adjustment.difference), currentLanguage),
        amountColor = accent,
        badgeText = if (currentLanguage == AppLanguage.BN) "হিসাব ঠিক" else "Adjustment",
        badgeColor = accent,
        testTag = "adj_row_${adjustment.id}"
    )
}

/**
 * Sale row in History feed
 */
@Composable
fun HistorySaleRow(
    sale: Sale,
    currentLanguage: AppLanguage,
    members: List<com.example.data.model.StoreMember> = emptyList(),
    ownerUid: String = "",
    onClick: () -> Unit
) {
    val formattedTime = remember(sale.createdAt, currentLanguage) {
        sale.createdAt?.toDate()?.let { ShopLocale.formatTimeOnly(it.time, currentLanguage) } ?: "--:--"
    }
    val isBaki = sale.paymentType == "baki"
    val isPartPaid = CalculationEngine.isPartPaidSale(sale)
    val paidAmt = CalculationEngine.salePaidAmount(sale)
    val dueAmt = CalculationEngine.saleDueAmount(sale)
    val staffMember = remember(sale.soldBy, members) { members.find { it.userId == sale.soldBy } }
    val creatorName = staffMember?.displayName
        ?: if (sale.soldBy == ownerUid && ownerUid.isNotBlank()) null
        else if (sale.soldBy.isNotBlank()) sale.soldBy else null

    val displayCustomer = sale.customerName.ifBlank {
        if (currentLanguage == AppLanguage.BN) "নগদ বিক্রয়" else "Cash Sale"
    }
    val secondary = buildString {
        append(sale.invoiceNo)
        append(" · ")
        append(displayCustomer)
        if (!creatorName.isNullOrBlank()) {
            append(" · ")
            append(creatorName)
        }
    }
    val itemsLabel = if (currentLanguage == AppLanguage.BN) {
        "${ShopLocale.formatNumber(sale.totalItems, currentLanguage)}টি পণ্য · $formattedTime"
    } else {
        val isSingle = kotlin.math.abs(sale.totalItems - 1.0) < 1e-6
        "${ShopLocale.formatNumber(sale.totalItems, currentLanguage)} ${if (isSingle) "item" else "items"} · $formattedTime"
    }
    val tertiaryText = if (isPartPaid) {
        val partDetail = if (currentLanguage == AppLanguage.BN) {
            "দিয়েছে ${ShopLocale.formatMoney(paidAmt, currentLanguage)} · বাকি ${ShopLocale.formatMoney(dueAmt, currentLanguage)}"
        } else {
            "Paid ${ShopLocale.formatMoney(paidAmt, currentLanguage)} · Due ${ShopLocale.formatMoney(dueAmt, currentLanguage)}"
        }
        "$itemsLabel · $partDetail"
    } else {
        itemsLabel
    }

    HistoryTransactionGlassCard(
        initialLetter = initialCharFrom(displayCustomer, "S"),
        primaryTitle = displayCustomer,
        secondaryLine = secondary,
        tertiaryLine = tertiaryText,
        amountText = ShopLocale.formatMoney(sale.grandTotal, currentLanguage),
        amountColor = TextPrimary,
        badgeText = when {
            isPartPaid -> if (currentLanguage == AppLanguage.BN) "আংশিক বাকি" else "Part Paid"
            isBaki -> if (currentLanguage == AppLanguage.BN) "বাকি" else "Baki"
            else -> if (currentLanguage == AppLanguage.BN) "পরিশোধিত" else "Paid"
        },
        badgeColor = if (isBaki) HistoryAmber else VividGreen,
        testTag = "sale_row_${sale.invoiceNo}",
        onClick = onClick
    )
}

/**
 * Purchase row in History feed (strictly from real Purchase records)
 */
@Composable
fun HistoryPurchaseRow(
    purchase: Purchase,
    currentLanguage: AppLanguage,
    onClick: () -> Unit
) {
    val formattedTime = remember(purchase.createdAt, currentLanguage) {
        purchase.createdAt?.toDate()?.let { ShopLocale.formatTimeOnly(it.time, currentLanguage) } ?: "--:--"
    }
    val supplierTitle = purchase.supplierName.ifBlank { purchase.purchaseNo }
    val secondary = if (currentLanguage == AppLanguage.BN) {
        "ক্রয় · ${purchase.purchaseNo}"
    } else {
        "Purchase · ${purchase.purchaseNo}"
    }
    val itemsSummary = remember(purchase.items, purchase.totalItems, formattedTime, currentLanguage) {
        val first = purchase.items.firstOrNull()
        when {
            first != null -> {
                val extra = if (purchase.items.size > 1) " +${purchase.items.size - 1}" else ""
                "${first.name} ×${ShopLocale.formatNumber(first.qty, currentLanguage)} ${ShopLocale.displayUnit(first.unit, currentLanguage)}$extra · $formattedTime"
            }
            purchase.totalItems > 0.0 -> {
                if (currentLanguage == AppLanguage.BN) {
                    "${ShopLocale.formatNumber(purchase.totalItems, currentLanguage)}টি পণ্য · $formattedTime"
                } else {
                    val isSingle = kotlin.math.abs(purchase.totalItems - 1.0) < 1e-6
                    "${ShopLocale.formatNumber(purchase.totalItems, currentLanguage)} ${if (isSingle) "item" else "items"} · $formattedTime"
                }
            }
            else -> formattedTime
        }
    }

    HistoryTransactionGlassCard(
        initialLetter = initialCharFrom(supplierTitle, "P"),
        primaryTitle = supplierTitle,
        secondaryLine = secondary,
        tertiaryLine = itemsSummary,
        amountText = ShopLocale.formatMoney(purchase.totalAmount, currentLanguage),
        amountColor = HistoryAmber,
        badgeText = if (currentLanguage == AppLanguage.BN) "ক্রয়" else "Purchase",
        badgeColor = HistoryAmber,
        testTag = "purchase_row_${purchase.purchaseNo}",
        onClick = onClick
    )
}

@Composable
internal fun PurchaseDetailsDialog(
    purchase: Purchase,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val formattedDate = remember(purchase.createdAt, currentLanguage) {
        purchase.createdAt?.toDate()?.let { ShopLocale.formatDateLong(it.time, currentLanguage) } ?: "—"
    }
    val isCredit = purchase.dueAmount > 0.01

    Dialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("purchase_details_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = purchase.purchaseNo,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Text(
                            text = formattedDate,
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HistoryAmber.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, HistoryAmber.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = if (isCredit) {
                                if (currentLanguage == AppLanguage.BN) "বাকি ক্রয়" else "Credit"
                            } else {
                                if (currentLanguage == AppLanguage.BN) "পরিশোধিত" else "Paid"
                            },
                            color = HistoryAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }

                HorizontalDivider(color = Color(0x1FFFFFFF))

                if (purchase.supplierName.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "সাপ্লায়ার" else "Supplier",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = purchase.supplierName,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (purchase.items.isNotEmpty()) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "পণ্যসমূহ" else "Items",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    purchase.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${ShopLocale.formatNumber(item.qty, currentLanguage)} ${ShopLocale.displayUnit(item.unit, currentLanguage)} × ${ShopLocale.formatMoney(item.buyPrice, currentLanguage)}",
                                    color = TextMuted,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ShopLocale.formatMoney(item.lineTotal, currentLanguage),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0x1FFFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "মোট" else "Total",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = ShopLocale.formatMoney(purchase.totalAmount, currentLanguage),
                        color = HistoryAmber,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "পরিশোধ" else "Paid",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = ShopLocale.formatMoney(purchase.paidAmount, currentLanguage),
                        color = VividGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (purchase.dueAmount > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "বাকি" else "Due",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = ShopLocale.formatMoney(purchase.dueAmount, currentLanguage),
                            color = AlertRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (purchase.note.isNotBlank()) {
                    Text(
                        text = purchase.note,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "বন্ধ করুন" else "Close",
                            color = VividGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.historyProductsSoldLazyItems(
    productsSold: List<com.example.domain.ProductSoldStat>,
    isGrid: Boolean,
    searchQuery: String,
    currentLanguage: AppLanguage,
    canSeeProfit: Boolean,
    onProductClick: (com.example.domain.ProductSoldStat) -> Unit
) {
    if (productsSold.isEmpty()) {
        item(key = "history_empty_products_sold") {
            val title = if (searchQuery.isNotBlank()) {
                if (currentLanguage == AppLanguage.BN) "কোনো পণ্য পাওয়া যায়নি" else "No matching products sold"
            } else {
                if (currentLanguage == AppLanguage.BN) "এই সময়ে কোনো পণ্য বিক্রয় হয়নি" else "No products sold in this period"
            }
            HistoryEmptyStateCard(
                title = title,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        return
    }

    if (isGrid) {
        val rows = productsSold.chunked(2)
        items(
            items = rows,
            key = { pair -> "grid_" + pair.joinToString("_") { it.productId.ifBlank { it.productName } } }
        ) { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { stat ->
                    ProductSoldGridTile(
                        stat = stat,
                        currentLanguage = currentLanguage,
                        canSeeProfit = canSeeProfit,
                        onClick = { onProductClick(stat) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    } else {
        items(
            items = productsSold,
            key = { "prod_" + it.productId.ifBlank { it.productName } }
        ) { stat ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ProductSoldListCard(
                    stat = stat,
                    currentLanguage = currentLanguage,
                    canSeeProfit = canSeeProfit,
                    onClick = { onProductClick(stat) }
                )
            }
        }
    }
}

@Composable
private fun ProductSoldGridTile(
    stat: com.example.domain.ProductSoldStat,
    currentLanguage: AppLanguage,
    canSeeProfit: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xEB222522), Color(0xEC141614))
                )
            )
            .clickable(onClick = onClick)
            .testTag("product_sold_grid_${stat.productId}"),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0x28FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stat.productName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${ShopLocale.formatNumber(stat.quantitySold, currentLanguage)} ${ShopLocale.displayUnit(stat.unit, currentLanguage)}",
                    color = VividGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = ShopLocale.formatMoney(stat.totalSalesAmount, currentLanguage),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            if (canSeeProfit) {
                Text(
                    text = (if (currentLanguage == AppLanguage.BN) "লাভ: " else "Profit: ") +
                        ShopLocale.formatMoney(stat.totalProfit, currentLanguage),
                    color = TextMuted,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProductSoldListCard(
    stat: com.example.domain.ProductSoldStat,
    currentLanguage: AppLanguage,
    canSeeProfit: Boolean,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xEB222522), Color(0xEC141614))
                )
            )
            .clickable(onClick = onClick)
            .testTag("product_sold_list_${stat.productId}"),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0x28FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
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
                    text = initialCharFrom(stat.productName, "P"),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.productName,
                    color = TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (canSeeProfit) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = (if (currentLanguage == AppLanguage.BN) "মোট লাভ: " else "Profit: ") +
                            ShopLocale.formatMoney(stat.totalProfit, currentLanguage),
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${ShopLocale.formatNumber(stat.quantitySold, currentLanguage)} ${ShopLocale.displayUnit(stat.unit, currentLanguage)}",
                    color = VividGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = ShopLocale.formatMoney(stat.totalSalesAmount, currentLanguage),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Step 6: Sale details screen (tap a row):
 * invoice number, date and time, customer, every item with quantity, price and line total,
 * discount, total, Paid or Baki, notes, and who made the sale. Read-only.
 */
@Composable
fun SaleDetailsScreen(
    sale: Sale,
    currentLanguage: AppLanguage,
    members: List<com.example.data.model.StoreMember> = emptyList(),
    ownerUid: String = "",
    storeName: String = "StoreOS",
    canSeeProfit: Boolean = true,
    onBack: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onBack)

    val formattedDateTime = remember(sale.createdAt, currentLanguage) {
        sale.createdAt?.toDate()?.let { ShopLocale.formatDateLong(it.time, currentLanguage) }
            ?: if (currentLanguage == AppLanguage.BN) "এইমাত্র" else "Just now"
    }
    val isBaki = sale.paymentType == "baki"

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Navigation Bar
        item(key = "sale_detail_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x1AFFFFFF), CircleShape)
                        .testTag("sale_detail_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = sale.invoiceNo,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        text = formattedDateTime,
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
            // Customer & Status Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "ক্রেতার নাম" else "Customer",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = sale.customerName.ifBlank {
                                        if (currentLanguage == AppLanguage.BN) "নগদ বিক্রয়" else "Cash Sale"
                                    },
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (sale.customerPhone.isNotBlank()) {
                                    Text(
                                        text = sale.customerPhone,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBaki) Color(0x33FFA000) else Color(0x265DD62C)
                            ) {
                                Text(
                                    text = if (isBaki) {
                                        if (currentLanguage == AppLanguage.BN) "বাকি" else "Baki (Credit)"
                                    } else {
                                        if (currentLanguage == AppLanguage.BN) "নগদ পরিশোধ" else "Paid (Cash)"
                                    },
                                    color = if (isBaki) Color(0xFFFFB300) else VividGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (sale.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0x14FFFFFF))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (if (currentLanguage == AppLanguage.BN) "নোট: " else "Notes: ") + sale.notes,
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val staffMember = remember(sale.soldBy, members) { members.find { it.userId == sale.soldBy } }
                        val creatorName = staffMember?.displayName ?: if (sale.soldBy == ownerUid && ownerUid.isNotBlank()) {
                            if (currentLanguage == AppLanguage.BN) "মালিক" else "Owner"
                        } else {
                            sale.soldBy.ifBlank { if (currentLanguage == AppLanguage.BN) "দোকান কর্মী" else "Store Staff" }
                        }
                        Text(
                            text = (if (currentLanguage == AppLanguage.BN) "তৈরি করেছেন: " else "Made by: ") + creatorName,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // Items Purchased Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) {
                                "পণ্য তালিকা (${ShopLocale.formatNumber(sale.items.size.toLong(), currentLanguage)})"
                            } else {
                                "Items Sold (${ShopLocale.formatNumber(sale.items.size.toLong(), currentLanguage)})"
                            },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        sale.items.forEachIndexed { idx, item ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = Color(0x0FFFFFFF)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 3
                                    )
                                    Text(
                                        text = "${ShopLocale.formatNumber(item.qty, currentLanguage)} ${ShopLocale.displayUnit(item.unit, currentLanguage)} × " + ShopLocale.formatMoney(item.sellPrice, currentLanguage),
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    if (item.discount > 0.0) {
                                        Text(
                                            text = (if (currentLanguage == AppLanguage.BN) "ছাড়: " else "Disc: -") +
                                                    ShopLocale.formatMoney(item.discount, currentLanguage),
                                            color = Color(0xFFFFB300),
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Text(
                                    text = ShopLocale.formatMoney(item.lineTotal, currentLanguage),
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Financial Breakdown Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "সাবটোটাল" else "Subtotal",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Text(
                                text = ShopLocale.formatMoney(sale.subtotal, currentLanguage),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }

                        if (sale.discount > 0.0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "মোট ছাড়" else "Total Discount",
                                    color = Color(0xFFFFB300),
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "- " + ShopLocale.formatMoney(sale.discount, currentLanguage),
                                    color = Color(0xFFFFB300),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0x1AFFFFFF))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "সর্বমোট (Total)" else "Grand Total",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = ShopLocale.formatMoney(sale.grandTotal, currentLanguage),
                                color = VividGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Share & Copy Receipt Actions
            item {
                val context = LocalContext.current
                val clipboardManager = LocalClipboardManager.current
                val completedReceipt = remember(sale, storeName) {
                    CompletedSaleReceipt(
                        invoiceNo = sale.invoiceNo,
                        storeName = storeName,
                        items = sale.items,
                        totalItems = sale.totalItems,
                        subtotal = sale.subtotal,
                        discount = sale.discount,
                        grandTotal = sale.grandTotal,
                        paymentType = sale.paymentType,
                        customerName = sale.customerName,
                        customerPhone = sale.customerPhone,
                        notes = sale.notes,
                        timestamp = sale.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                        paidAmount = CalculationEngine.salePaidAmount(sale),
                        dueAmount = CalculationEngine.saleDueAmount(sale)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.material3.Button(
                        onClick = {
                            val shareText = completedReceipt.formatTextReceipt()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val chooserTitle = if (currentLanguage == AppLanguage.BN) "রসিদ পাঠান" else "Share Receipt Via"
                            val shareIntent = Intent.createChooser(sendIntent, chooserTitle)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "রসিদ পাঠান" else "Share Receipt",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(completedReceipt.formatTextReceipt()))
                            Toast.makeText(context, if (currentLanguage == AppLanguage.BN) "রসিদ কপি করা হয়েছে" else "Receipt copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF444444)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "কপি স্লিপ" else "Copy Slip",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
        }
    }
}

/**
 * Custom scrollable calendar date range picker dialog for Step 2
 */
@Composable
fun CustomDateRangePickerDialog(
    initialRange: Pair<Long, Long>?,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit
) {
    var calendarMonth by remember {
        val cal = Calendar.getInstance()
        mutableStateOf(cal)
    }

    var selectedStartMillis by remember {
        mutableStateOf<Long?>(initialRange?.first)
    }
    var selectedEndMillis by remember {
        mutableStateOf<Long?>(initialRange?.second)
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                // Header with chosen range
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "তারিখ পরিসর নির্বাচন করুন" else "Select Date Range",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val startText = selectedStartMillis?.let {
                        ShopLocale.formatDayMonth(it, currentLanguage)
                    } ?: if (currentLanguage == AppLanguage.BN) "শুরুর দিন" else "Start Day"
                    val endText = selectedEndMillis?.let {
                        ShopLocale.formatDayMonth(it, currentLanguage)
                    } ?: if (currentLanguage == AppLanguage.BN) "শেষ দিন" else "End Day"
                    Text(
                        text = "$startText  ➔  $endText",
                        color = VividGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = calendarMonth.timeInMillis
                            add(Calendar.MONTH, -1)
                        }
                        calendarMonth = newCal
                    }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = if (currentLanguage == AppLanguage.BN) "আগের মাস" else "Previous Month",
                            tint = TextPrimary
                        )
                    }

                    Text(
                        text = ShopLocale.formatMonthYear(calendarMonth.timeInMillis, currentLanguage),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    IconButton(onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = calendarMonth.timeInMillis
                            add(Calendar.MONTH, 1)
                        }
                        calendarMonth = newCal
                    }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = if (currentLanguage == AppLanguage.BN) "পরের মাস" else "Next Month",
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Days of week header
                val dayNames = if (currentLanguage == AppLanguage.BN) {
                    listOf("র", "সো", "ম", "বু", "বৃ", "শু", "শ")
                } else {
                    listOf("S", "M", "T", "W", "T", "F", "S")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    dayNames.forEach { d ->
                        Text(
                            text = d,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Calendar days grid
                val calForGrid = Calendar.getInstance().apply {
                    timeInMillis = calendarMonth.timeInMillis
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val daysInMonth = calForGrid.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayOfWeek = calForGrid.get(Calendar.DAY_OF_WEEK) - 1 // 0-based for Sunday

                val totalCells = firstDayOfWeek + daysInMonth
                val totalRows = (totalCells + 6) / 7

                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - firstDayOfWeek + 1

                            if (dayNumber in 1..daysInMonth) {
                                val thisDayCal = Calendar.getInstance().apply {
                                    timeInMillis = calendarMonth.timeInMillis
                                    set(Calendar.DAY_OF_MONTH, dayNumber)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val dayMillis = thisDayCal.timeInMillis
                                val isStart = selectedStartMillis != null && dayMillis == selectedStartMillis
                                val isEnd = selectedEndMillis != null && dayMillis == selectedEndMillis
                                val inRange = selectedStartMillis != null && selectedEndMillis != null &&
                                        dayMillis in selectedStartMillis!!..selectedEndMillis!!

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isStart || isEnd -> VividGreen
                                                inRange -> Color(0x335DD62C)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            if (selectedStartMillis == null || (selectedStartMillis != null && selectedEndMillis != null)) {
                                                selectedStartMillis = dayMillis
                                                selectedEndMillis = null
                                            } else {
                                                if (dayMillis >= selectedStartMillis!!) {
                                                    selectedEndMillis = dayMillis
                                                } else {
                                                    selectedEndMillis = selectedStartMillis
                                                    selectedStartMillis = dayMillis
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ShopLocale.formatNumber(dayNumber.toLong(), currentLanguage),
                                        color = if (isStart || isEnd) DarkBackground else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isStart || isEnd || inRange) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Cancel & Apply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "বাতিল" else "Cancel",
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = selectedStartMillis != null) {
                                val start = selectedStartMillis ?: return@clickable
                                val end = selectedEndMillis ?: start
                                val endOfDay = Calendar.getInstance().apply {
                                    timeInMillis = end
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }.timeInMillis
                                onConfirm(start, endOfDay)
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedStartMillis != null) VividGreen else Color(0x33FFFFFF)
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "প্রয়োগ করুন" else "Apply",
                            color = if (selectedStartMillis != null) DarkBackground else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaleReceiptCard(
    sale: Sale,
    currentLanguage: AppLanguage
) {
    val formattedDate = remember(sale.createdAt, currentLanguage) {
        sale.createdAt?.toDate()?.let { ShopLocale.formatDateShort(it.time, currentLanguage) }
            ?: if (currentLanguage == AppLanguage.BN) "এইমাত্র" else "Just now"
    }

    val isBaki = sale.paymentType == "baki"

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        testTag = "sale_card_${sale.id}"
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isBaki) Color(0x33FFA000) else Color(0x265DD62C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBaki) Icons.Default.CreditCard else Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = if (isBaki) Color(0xFFFFB300) else VividGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = sale.invoiceNo,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = formattedDate,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBaki) Color(0x33FFA000) else Color(0x265DD62C)
                ) {
                    Text(
                        text = if (isBaki) {
                            if (currentLanguage == AppLanguage.BN) "বাকি" else "BAKI (CREDIT)"
                        } else {
                            if (currentLanguage == AppLanguage.BN) "নগদ" else "CASH"
                        },
                        color = if (isBaki) Color(0xFFFFB300) else VividGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Name & Items count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val itemsSuffix = if (currentLanguage == AppLanguage.BN) "টি পণ্য" else "items"
                Text(
                    text = "${sale.customerName} • ${ShopLocale.formatNumber(sale.totalItems, currentLanguage)} $itemsSuffix",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = ShopLocale.formatMoney(sale.grandTotal, currentLanguage),
                    color = VividGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }
    }
}
