package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Account
import com.example.data.model.AccountStatementEntry
import com.example.data.model.AccountStatementReport
import com.example.data.model.Expense
import com.example.data.model.OtherIncome
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.ExportActionBar
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.PlainEmptyState
import com.example.ui.components.VividButton
import com.example.ui.components.getAccountIcon
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.BusinessNavDestination
import com.example.ui.viewmodel.HistoryPeriod
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ReportExportUtil
import com.example.util.ReportTableData
import com.example.util.ShopLocale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Unified Cash & Bank screen (Accounts, Expenses, Other Income in tabs).
 * Preserves existing balances, calculations, dialogs, permissions, and Simple Mode behavior.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CashAndBankScreen(
    storeViewModel: StoreViewModel,
    initialTab: Int = 0, // 0 = Accounts, 1 = Expenses, 2 = Other Income
    backButtonTag: String = "cash_bank_back_btn",
    modifier: Modifier = Modifier
) {
    val accounts by storeViewModel.accounts.collectAsState()
    val balances by storeViewModel.accountBalances.collectAsState()
    val totalBalance by storeViewModel.totalBalance.collectAsState()
    val expenses by storeViewModel.expenses.collectAsState()
    val otherIncomes by storeViewModel.otherIncomes.collectAsState()
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val isOwner by storeViewModel.isOwner.collectAsState()
    val canCashBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()
    val canExpenses by storeViewModel.canAccessExpensesAndIncomeFlow.collectAsState()

    val showAccountsTab = canCashBank && !isSimpleMode
    val showExpensesTab = canExpenses
    val showIncomeTab = canExpenses

    val availableTabs = remember(showAccountsTab, showExpensesTab, showIncomeTab) {
        buildList {
            if (showAccountsTab) add(0)
            if (showExpensesTab) add(1)
            if (showIncomeTab) add(2)
        }
    }

    var selectedTab by rememberSaveable(initialTab, availableTabs) {
        val preferred = initialTab.coerceIn(0, 2)
        mutableIntStateOf(
            when {
                preferred in availableTabs -> preferred
                availableTabs.isNotEmpty() -> availableTabs.first()
                else -> 0
            }
        )
    }

    var showNewAccountDialog by remember { mutableStateOf(false) }
    var adjustTargetAccount by remember { mutableStateOf<Pair<Account, Double>?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var selectedExpenseForDetails by remember { mutableStateOf<Expense?>(null) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var selectedIncomeForDetails by remember { mutableStateOf<OtherIncome?>(null) }
    var expenseSearchQuery by remember { mutableStateOf("") }
    var incomeSearchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val activeAccounts = remember(accounts) {
        accounts.filter { !it.isArchived }
    }
    val activeExpenses = remember(expenses) {
        expenses.filter { !it.isDeleted }
    }
    val activeIncomes = remember(otherIncomes) {
        otherIncomes.filter { !it.isDeleted }
    }

    val totalActiveExpenses = remember(activeExpenses) {
        activeExpenses.sumOf { it.amount }
    }
    val totalActiveIncome = remember(activeIncomes) {
        activeIncomes.sumOf { it.amount }
    }

    val filteredExpenses = remember(activeExpenses, expenseSearchQuery) {
        val q = expenseSearchQuery.trim().lowercase()
        val base = activeExpenses.sortedByDescending { it.date }
        if (q.isBlank()) base
        else base.filter {
            it.category.lowercase().contains(q) ||
                it.note.lowercase().contains(q) ||
                it.accountName.lowercase().contains(q) ||
                it.amount.toString().contains(q)
        }
    }
    val filteredIncomes = remember(activeIncomes, incomeSearchQuery) {
        val q = incomeSearchQuery.trim().lowercase()
        val base = activeIncomes.sortedByDescending { it.date }
        if (q.isBlank()) base
        else base.filter {
            it.category.lowercase().contains(q) ||
                it.note.lowercase().contains(q) ||
                it.accountName.lowercase().contains(q) ||
                it.amount.toString().contains(q)
        }
    }

    val groupedExpenses = remember(filteredExpenses, currentLanguage) {
        filteredExpenses.groupBy { ShopLocale.formatWeekdayDate(it.date, currentLanguage) }
    }
    val groupedIncomes = remember(filteredIncomes, currentLanguage) {
        filteredIncomes.groupBy { ShopLocale.formatWeekdayDate(it.date, currentLanguage) }
    }

    BackHandler {
        storeViewModel.navigateBackInBusiness()
    }

    val coralExpense = Color(0xFFE56B6F)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070907))
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.25f, size.height * 0.14f),
                        radius = size.width * 0.78f
                    )
                )
            }
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .testTag("cash_and_bank_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("cash_bank_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Centered Top Bar with Back Button
            item(key = "cash_bank_top_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { storeViewModel.navigateBackInBusiness() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(backButtonTag)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "ক্যাশ ও ব্যাংক" else "Cash & Bank",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
                // Glowing Hero Balance Card
                item {
                    val heroShape = RoundedCornerShape(22.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 16.dp,
                                shape = heroShape,
                                ambientColor = VividGreen.copy(alpha = 0.32f),
                                spotColor = VividGreen.copy(alpha = 0.45f)
                            )
                            .testTag("overall_balance_card"),
                        shape = heroShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(VividGreen.copy(alpha = 0.90f), VividGreen.copy(alpha = 0.38f))
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
                                .padding(horizontal = 18.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "সর্বমোট স্থিতি" else "TOTAL BALANCE",
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = ShopLocale.formatMoney(totalBalance, currentLanguage),
                                color = if (totalBalance >= 0) VividGreen else AlertRed,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (canExpenses) {
                                Spacer(modifier = Modifier.height(14.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        onClick = { selectedTab = 1 },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0x384B1B1B),
                                        border = BorderStroke(1.dp, coralExpense.copy(alpha = 0.28f)),
                                        modifier = Modifier.testTag("hero_pill_expenses")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = coralExpense,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "${if (currentLanguage == AppLanguage.BN) "খরচ" else "Expenses"} ${ShopLocale.formatMoney(totalActiveExpenses, currentLanguage)}",
                                                color = coralExpense,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = { selectedTab = 2 },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0x28263226),
                                        border = BorderStroke(1.dp, Color(0x24FFFFFF)),
                                        modifier = Modifier.testTag("hero_pill_income")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropUp,
                                                contentDescription = null,
                                                tint = TextPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "${if (currentLanguage == AppLanguage.BN) "অন্যান্য আয়" else "Other income"} ${ShopLocale.formatMoney(totalActiveIncome, currentLanguage)}",
                                                color = TextPrimary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
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

                // Multi-segment account balance proportion bar (when Accounts visible)
                if (showAccountsTab && activeAccounts.isNotEmpty()) {
                    item {
                        val positiveSlices = remember(activeAccounts, balances) {
                            activeAccounts.mapNotNull { acc ->
                                val bal = balances[acc.id] ?: acc.openingBalance
                                if (bal > 0.0) bal else null
                            }
                        }
                        val positiveTotal = positiveSlices.sum()
                        val segmentColors = listOf(
                            Color(0xFF427527),
                            VividGreen,
                            Color(0xFFA6F27E),
                            Color(0xFF2FA858),
                            Color(0xFF7CEF4F)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x1EFFFFFF))
                                .testTag("account_proportion_bar"),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (positiveTotal > 0.0) {
                                positiveSlices.forEachIndexed { idx, value ->
                                    val frac = (value / positiveTotal).toFloat().coerceAtLeast(0.05f)
                                    Box(
                                        modifier = Modifier
                                            .weight(frac)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(segmentColors[idx % segmentColors.size])
                                    )
                                }
                            }
                        }
                    }
                }

                // Capsule Tab Switcher: Accounts (N) | Expenses (N) | Other Income
                if (availableTabs.isNotEmpty() && !(isSimpleMode && availableTabs.size == 1 && !canExpenses)) {
                    item {
                        val trackShape = RoundedCornerShape(22.dp)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = trackShape,
                            color = Color(0xEB151815),
                            border = BorderStroke(1.dp, Color(0x28FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showAccountsTab) {
                                    CashTabPill(
                                        label = "${if (currentLanguage == AppLanguage.BN) "অ্যাকাউন্ট" else "Accounts"} (${activeAccounts.size})",
                                        isSelected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        testTag = "cash_tab_accounts",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (showExpensesTab) {
                                    CashTabPill(
                                        label = "${if (currentLanguage == AppLanguage.BN) "খরচ" else "Expenses"} (${activeExpenses.size})",
                                        isSelected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        testTag = "cash_tab_expenses",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (showIncomeTab) {
                                    CashTabPill(
                                        label = if (currentLanguage == AppLanguage.BN) "অন্যান্য আয়" else "Other Income",
                                        isSelected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        testTag = "cash_tab_income",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Tab Body
                when {
                    selectedTab == 0 && showAccountsTab -> {
                        items(activeAccounts, key = { it.id }) { acc ->
                            val accBalance = balances[acc.id] ?: acc.openingBalance
                            AccountRowCard(
                                account = acc,
                                balance = accBalance,
                                currentLanguage = currentLanguage,
                                showAdjustButton = isOwner && !isSimpleMode,
                                onAdjustClick = { adjustTargetAccount = acc to accBalance },
                                onClick = {
                                    storeViewModel.selectAccount(acc)
                                    storeViewModel.navigateInBusiness(BusinessNavDestination.ACCOUNT_DETAILS_SCREEN)
                                }
                            )
                        }
                    }

                    selectedTab == 1 && showExpensesTab -> {
                        item(key = "expenses_search_bar") {
                            CashPillSearchBar(
                                query = expenseSearchQuery,
                                onQueryChange = { expenseSearchQuery = it },
                                placeholder = if (currentLanguage == AppLanguage.BN) {
                                    "ক্যাটাগরি, নোট বা অ্যাকাউন্ট খুঁজুন..."
                                } else {
                                    "Search expenses by category, note or account..."
                                },
                                isFocused = isSearchFocused,
                                onFocusChanged = { isSearchFocused = it },
                                testTag = "expenses_search_input"
                            )
                        }

                        if (filteredExpenses.isEmpty()) {
                            item(key = "expenses_empty_state") {
                                PlainEmptyState(
                                    message = if (expenseSearchQuery.isNotBlank()) {
                                        "No expenses match your search"
                                    } else {
                                        "No expenses recorded yet. Tap '+ Add Expense' below to record one."
                                    }
                                )
                            }
                        } else {
                            groupedExpenses.forEach { (dayHeader, dayExpenses) ->
                                item(key = "expense_day_hdr_$dayHeader") {
                                    val dayTotal = dayExpenses.sumOf { it.amount }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = dayHeader,
                                            color = TextSecondary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "-${ShopLocale.formatMoney(dayTotal, currentLanguage)}",
                                            color = AlertRed,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                items(dayExpenses, key = { it.id }) { expense ->
                                    ExpenseRowCard(
                                        expense = expense,
                                        currentLanguage = currentLanguage,
                                        onClick = { selectedExpenseForDetails = expense }
                                    )
                                }
                            }
                        }
                    }

                    selectedTab == 2 && showIncomeTab -> {
                        item(key = "incomes_search_bar") {
                            CashPillSearchBar(
                                query = incomeSearchQuery,
                                onQueryChange = { incomeSearchQuery = it },
                                placeholder = if (currentLanguage == AppLanguage.BN) {
                                    "আয়ের উৎস, নোট বা অ্যাকাউন্ট খুঁজুন..."
                                } else {
                                    "Search income by category, note or account..."
                                },
                                isFocused = isSearchFocused,
                                onFocusChanged = { isSearchFocused = it },
                                testTag = "incomes_search_input"
                            )
                        }

                        if (filteredIncomes.isEmpty()) {
                            item(key = "incomes_empty_state") {
                                PlainEmptyState(
                                    message = if (incomeSearchQuery.isNotBlank()) {
                                        "No income entries match your search"
                                    } else {
                                        "No other income recorded yet. Tap '+ Add Income' below to record one."
                                    }
                                )
                            }
                        } else {
                            groupedIncomes.forEach { (dayHeader, dayIncomes) ->
                                item(key = "income_day_hdr_$dayHeader") {
                                    val dayTotal = dayIncomes.sumOf { it.amount }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = dayHeader,
                                            color = TextSecondary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "+${ShopLocale.formatMoney(dayTotal, currentLanguage)}",
                                            color = VividGreen,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                items(dayIncomes, key = { it.id }) { income ->
                                    OtherIncomeRowCard(
                                        income = income,
                                        currentLanguage = currentLanguage,
                                        onClick = { selectedIncomeForDetails = income }
                                    )
                                }
                            }
                        }
                    }
                }
        }

        // Bottom-right Glowing Vivid Green Pill FAB matching Image 2 & StockScreen
        val fabConfig = when {
            selectedTab == 0 && showAccountsTab -> Triple(
                if (currentLanguage == AppLanguage.BN) "নতুন অ্যাকাউন্ট" else "New Account",
                "btn_new_account"
            ) { showNewAccountDialog = true }
            selectedTab == 1 && showExpensesTab -> Triple(
                if (currentLanguage == AppLanguage.BN) "নতুন খরচ" else "Add Expense",
                "btn_add_expense"
            ) { showAddExpenseDialog = true }
            selectedTab == 2 && showIncomeTab -> Triple(
                if (currentLanguage == AppLanguage.BN) "নতুন আয়" else "Add Income",
                "btn_add_income"
            ) { showAddIncomeDialog = true }
            else -> null
        }

        if (fabConfig != null) {
            val (fabLabel, fabTag, onFabClick) = fabConfig
            val fabShape = RoundedCornerShape(28.dp)
            Surface(
                onClick = onFabClick,
                shape = fabShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color(0x88FFFFFF)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 10.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = fabShape,
                        spotColor = VividGreen,
                        ambientColor = VividGreen
                    )
                    .height(48.dp)
                    .testTag(fabTag)
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF7CEF4F), VividGreen)
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF0A1406),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = fabLabel,
                        color = Color(0xFF0A1406),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }

    if (showNewAccountDialog) {
        NewAccountDialog(
            accounts = accounts,
            currentLanguage = currentLanguage,
            onDismiss = { showNewAccountDialog = false },
            onConfirm = { name, type, accNumber, openingBal ->
                storeViewModel.createAccount(
                    name = name,
                    type = type,
                    accountNumber = accNumber,
                    openingBalance = openingBal,
                    onSuccess = { showNewAccountDialog = false }
                )
            }
        )
    }

    if (adjustTargetAccount != null) {
        val (targetAcc, targetBal) = adjustTargetAccount!!
        AdjustBalanceDialog(
            account = targetAcc,
            currentBalance = targetBal,
            currentLanguage = currentLanguage,
            onDismiss = { adjustTargetAccount = null },
            onConfirm = { newBalance, note ->
                storeViewModel.adjustAccountBalance(
                    account = targetAcc,
                    newBalance = newBalance,
                    note = note,
                    onSuccess = { adjustTargetAccount = null }
                )
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { amount, category, date, accId, accName, note, receiptUri ->
                storeViewModel.recordExpense(
                    amount = amount,
                    category = category,
                    date = date,
                    accountId = accId,
                    accountName = accName,
                    note = note,
                    receiptUri = receiptUri,
                    onSuccess = { showAddExpenseDialog = false }
                )
            }
        )
    }

    if (selectedExpenseForDetails != null) {
        ExpenseDetailsDialog(
            expense = selectedExpenseForDetails!!,
            currentLanguage = currentLanguage,
            onDismiss = { selectedExpenseForDetails = null },
            onDelete = { deleteNote ->
                storeViewModel.deleteExpense(
                    expense = selectedExpenseForDetails!!,
                    note = deleteNote,
                    onSuccess = { selectedExpenseForDetails = null }
                )
            }
        )
    }

    if (showAddIncomeDialog) {
        AddOtherIncomeDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { showAddIncomeDialog = false },
            onConfirm = { amount, category, date, accId, accName, note ->
                storeViewModel.recordOtherIncome(
                    amount = amount,
                    category = category,
                    date = date,
                    accountId = accId,
                    accountName = accName,
                    note = note,
                    onSuccess = { showAddIncomeDialog = false }
                )
            }
        )
    }

    if (selectedIncomeForDetails != null) {
        OtherIncomeDetailsDialog(
            income = selectedIncomeForDetails!!,
            currentLanguage = currentLanguage,
            onDismiss = { selectedIncomeForDetails = null },
            onDelete = { deleteNote ->
                storeViewModel.deleteOtherIncome(
                    income = selectedIncomeForDetails!!,
                    note = deleteNote,
                    onSuccess = { selectedIncomeForDetails = null }
                )
            }
        )
    }
}

@Composable
private fun CashTabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(18.dp)
    Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .testTag(testTag),
        shape = pillShape,
        color = if (isSelected) Color(0xFF282D28) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, Color(0x38FFFFFF)) else null
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CashPillSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    testTag: String
) {
    val searchShape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(searchShape)
            .background(if (isFocused) Color(0xD91A1E1A) else Color(0xBF151815))
            .border(
                width = 1.dp,
                color = if (isFocused) VividGreen.copy(alpha = 0.72f) else Color(0x28FFFFFF),
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
                tint = if (isFocused) VividGreen else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = TextMuted,
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
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.5.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { onFocusChanged(it.isFocused) }
                        .testTag(testTag)
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
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

@Composable
fun AccountRowCard(
    account: Account,
    balance: Double,
    currentLanguage: AppLanguage,
    showAdjustButton: Boolean = false,
    onAdjustClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    val accentColor = remember(account.name, account.type) {
        val n = account.name.lowercase()
        when {
            n.contains("bkash") -> Color(0xFFE91E63)
            n.contains("nagad") -> Color(0xFFFF8F00)
            n.contains("rocket") -> Color(0xFF9C27B0)
            else -> VividGreen
        }
    }
    val iconVector = remember(account.name, account.type) {
        val n = account.name.lowercase()
        if (n.contains("bkash") || n.contains("nagad") || n.contains("rocket")) {
            Icons.AutoMirrored.Filled.Send
        } else {
            getAccountIcon(account.type)
        }
    }

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
            .testTag("account_card_${account.id}"),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0x28FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Ring Icon matching Image 2
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF151915))
                    .border(1.8.dp, accentColor.copy(alpha = 0.85f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = account.name,
                    tint = accentColor,
                    modifier = Modifier.size(23.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = account.name,
                        color = TextPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = " · ${account.type}",
                        color = TextSecondary,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Real account number or default badge only
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (account.accountNumber.isNotBlank()) {
                        Text(
                            text = "ID · ${account.accountNumber}",
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (account.isDefault || account.id == "cash_in_hand") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VividGreen.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "DEFAULT",
                                color = VividGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ShopLocale.formatMoney(balance, currentLanguage),
                        color = if (balance >= 0) VividGreen else AlertRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (showAdjustButton && onAdjustClick != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = onAdjustClick,
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF232723),
                            border = BorderStroke(1.dp, Color(0x34FFFFFF)),
                            modifier = Modifier.testTag("btn_adjust_balance_${account.id}")
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "ঠিক করুন" else "Adjust Balance",
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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

/**
 * Step 3: New Account Dialog
 * Choose Wallet or Bank, name, account ID, and current balance (opening balance).
 * Name required. Duplicate name blocked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAccountDialog(
    accounts: List<Account>,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, accNumber: String, openingBal: Double) -> Unit
) {
    var selectedType by remember { mutableStateOf("Bank") } // Bank or Wallet
    var name by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var openingBalanceText by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = DarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "নতুন অ্যাকাউন্ট যোগ করুন" else "Add New Account",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Type selector: Bank or Wallet
                Text(
                    text = "Account Type",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Bank", "Wallet").forEach { typeOption ->
                        val isSelected = selectedType == typeOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0x335DD62C) else GlassSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) VividGreen else GlassBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedType = typeOption }
                                .padding(vertical = 10.dp)
                                .testTag("type_option_$typeOption"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (typeOption == "Bank") Icons.Default.AccountBalance else Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isSelected) VividGreen else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = typeOption,
                                    color = if (isSelected) VividGreen else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                GlassTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localError = null
                    },
                    label = if (selectedType == "Bank") "Bank Name (e.g. Dutch Bangla Bank)" else "Wallet Name (e.g. bKash / Nagad)",
                    placeholder = if (selectedType == "Bank") "Dutch Bangla Bank" else "bKash Merchant",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "input_account_name"
                )

                Spacer(modifier = Modifier.height(10.dp))

                GlassTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = "Account / Phone Number (Optional)",
                    placeholder = "e.g. 151.110.1234 or 01711...",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "input_account_number"
                )

                Spacer(modifier = Modifier.height(10.dp))

                GlassTextField(
                    value = openingBalanceText,
                    onValueChange = { openingBalanceText = it },
                    label = "Current / Opening Balance (৳)",
                    placeholder = "0.00",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    testTag = "input_opening_balance"
                )

                if (localError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localError ?: "",
                        color = AlertRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassOutlinedButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        testTag = "dialog_cancel_account"
                    )
                    VividButton(
                        text = "Create Account",
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isBlank()) {
                                localError = "Account name is required"
                                return@VividButton
                            }
                            if (accounts.any { !it.isArchived && it.name.trim().equals(trimmedName, ignoreCase = true) }) {
                                localError = "An account with this name already exists"
                                return@VividButton
                            }
                            val bal = openingBalanceText.toDoubleOrNull() ?: 0.0
                            onConfirm(trimmedName, selectedType, accountNumber.trim(), bal)
                        },
                        modifier = Modifier.weight(1.3f),
                        testTag = "dialog_confirm_account"
                    )
                }
            }
        }
    }
}

/**
 * Step 4: Account details and statement
 * Current balance, period filter, statement list (newest first) of every money-in/out line
 * with date, type (Sale, Payment In, Purchase, Payment Out, Expense, Other Income, Adjustment),
 * who or what it was for, amount in or out, and running balance.
 * Export buttons (PDF, Print, CSV, Share).
 * Step 5: Adjust Balance button opens AdjustBalanceDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val account = storeViewModel.selectedAccount.collectAsState().value
    val statement = storeViewModel.selectedAccountStatement.collectAsState().value
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val period by storeViewModel.historyPeriod.collectAsState()
    val customRange by storeViewModel.historyCustomDateRange.collectAsState()
    val store by storeViewModel.store.collectAsState()

    val context = LocalContext.current
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    val isOwner by storeViewModel.isOwner.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    BackHandler {
        storeViewModel.navigateBackInBusiness()
    }

    if (account == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            PlainEmptyState(message = "No account selected")
        }
        return
    }

    val currentBalance = statement?.closingBalance ?: account.openingBalance

    if (showDatePicker) {
        com.example.ui.screens.CustomDateRangePickerDialog(
            initialRange = customRange,
            currentLanguage = currentLanguage,
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                storeViewModel.setHistoryCustomDateRange(start, end)
                showDatePicker = false
            }
        )
    }

    fun buildStatementExportData(): ReportTableData {
        val isBn = currentLanguage == AppLanguage.BN
        val rows = (statement?.entries ?: emptyList()).map { entry ->
            val dateStr = ShopLocale.formatDateLong(entry.timestamp, currentLanguage)
            val inStr = if (entry.moneyIn > 0.0) "+${ShopLocale.formatMoneyFixed2(entry.moneyIn, currentLanguage)}" else "-"
            val outStr = if (entry.moneyOut > 0.0) "-${ShopLocale.formatMoneyFixed2(entry.moneyOut, currentLanguage)}" else "-"
            val balStr = ShopLocale.formatMoneyFixed2(entry.runningBalance, currentLanguage)
            listOf(dateStr, entry.type, entry.partyOrCategory, entry.note, inStr, outStr, balStr)
        }
        val rawNumbers = (statement?.entries ?: emptyList()).map { entry ->
            listOf<Double?>(
                null,
                null,
                null,
                null,
                if (entry.moneyIn > 0.0) entry.moneyIn else null,
                if (entry.moneyOut > 0.0) entry.moneyOut else null,
                entry.runningBalance
            )
        }
        val totalIn = statement?.totalMoneyIn ?: 0.0
        val totalOut = statement?.totalMoneyOut ?: 0.0
        val totals = listOf(
            if (isBn) "মোট" else "Totals",
            "",
            "",
            "",
            "+${ShopLocale.formatMoneyFixed2(totalIn, currentLanguage)}",
            "-${ShopLocale.formatMoneyFixed2(totalOut, currentLanguage)}",
            ShopLocale.format("closing_prefix", currentLanguage, ShopLocale.formatMoneyFixed2(currentBalance, currentLanguage))
        )
        val rawTotals = listOf<Double?>(null, null, null, null, totalIn, totalOut, currentBalance)

        val openingBal = statement?.openingBalance ?: account.openingBalance
        val summaryMetrics = listOf(
            com.example.util.ReportSummaryMetric(
                label = if (isBn) "পূর্বের জের" else "Opening Balance",
                value = ShopLocale.formatMoneyFixed2(openingBal, currentLanguage)
            ),
            com.example.util.ReportSummaryMetric(
                label = if (isBn) "টাকা এসেছে (+)" else "Money In (+)",
                value = ShopLocale.formatMoneyFixed2(totalIn, currentLanguage)
            ),
            com.example.util.ReportSummaryMetric(
                label = if (isBn) "টাকা গেছে (-)" else "Money Out (-)",
                value = ShopLocale.formatMoneyFixed2(totalOut, currentLanguage)
            ),
            com.example.util.ReportSummaryMetric(
                label = if (isBn) "শেষে আছে" else "Closing Balance",
                value = ShopLocale.formatMoneyFixed2(currentBalance, currentLanguage)
            )
        )

        val accDisplay = ShopLocale.displayAccountName(account.name, account.id, currentLanguage)
        return ReportTableData(
            title = ShopLocale.format("rep_account_stmt_title", currentLanguage, accDisplay),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", currentLanguage),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = if (isBn) period.labelBn else period.labelEn,
            generatedAt = ShopLocale.formatDateLong(System.currentTimeMillis(), currentLanguage),
            headers = if (isBn) listOf("তারিখ", "ধরন", "নাম / ক্যাটেগরি", "নোট", "টাকা এসেছে", "টাকা গেছে", "ব্যালেন্স")
            else listOf("Date", "Type", "Party / Category", "Note", "Money In", "Money Out", "Balance"),
            rows = rows,
            numericColumns = setOf(4, 5, 6),
            rawNumbers = rawNumbers,
            totals = totals,
            rawTotals = rawTotals,
            notes = "${ShopLocale.get("account_colon", currentLanguage)} $accDisplay (${ShopLocale.displayAccountType(account.type, currentLanguage)}) • ${ShopLocale.get("opening_balance", currentLanguage)}: ${ShopLocale.formatMoneyFixed2(openingBal, currentLanguage)} • ${ShopLocale.format("closing_prefix", currentLanguage, ShopLocale.formatMoneyFixed2(currentBalance, currentLanguage))}",
            summaryMetrics = summaryMetrics
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item(key = "account_details_header") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.testTag("account_details_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                append(account.type)
                                if (account.accountNumber.isNotBlank()) {
                                    append(" • ")
                                    append(account.accountNumber)
                                }
                            },
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }

                    // Action buttons: Adjust & Archive (Owner only, adjust hidden in Simple Mode)
                    if (isOwner) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!isSimpleMode) {
                                GlassOutlinedButton(
                                    text = "Adjust",
                                    onClick = { showAdjustDialog = true },
                                    icon = Icons.Default.Edit,
                                    testTag = "btn_adjust_balance"
                                )
                            }

                            if (!account.isDefault && account.id != "cash_in_hand") {
                                IconButton(
                                    onClick = {
                                        if (abs(currentBalance) > 0.001) {
                                            Toast.makeText(
                                                context,
                                                ShopLocale.format("archive_balance_zero_err", currentLanguage, ShopLocale.formatMoneyFixed2(currentBalance, currentLanguage)),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            showArchiveConfirm = true
                                        }
                                    },
                                    modifier = Modifier.testTag("btn_archive_account")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = "Archive Account",
                                        tint = if (abs(currentBalance) <= 0.001) TextSecondary else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Balance Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("account_statement_balance_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current Balance",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x265DD62C)
                        ) {
                            Text(
                                text = account.type,
                                color = VividGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = ShopLocale.formatMoney(currentBalance, currentLanguage),
                        color = if (currentBalance >= 0) VividGreen else AlertRed,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Period summary row: Money In vs Money Out
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Period In (+)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = ShopLocale.formatMoney(statement?.totalMoneyIn ?: 0.0, currentLanguage),
                                color = VividGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Period Out (-)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = ShopLocale.formatMoney(statement?.totalMoneyOut ?: 0.0, currentLanguage),
                                color = AlertRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Period Filter
        item {
            com.example.ui.screens.HistoryPeriodTabs(
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

        // Export Actions (PDF, Print, CSV, Share)
        item {
            ExportActionBar(
                onDownloadPdf = {
                    val file = ReportExportUtil.generatePdfReport(
                        context,
                        buildStatementExportData(),
                        "Statement_${account.name}_${System.currentTimeMillis()}",
                        currentLanguage
                    )
                    ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                },
                onPrint = {
                    val file = ReportExportUtil.generatePdfReport(
                        context,
                        buildStatementExportData(),
                        "Statement_${account.name}_${System.currentTimeMillis()}",
                        currentLanguage
                    )
                    ReportExportUtil.printReportFile(context, file, "AccountStatement", currentLanguage)
                },
                onExportCsv = {
                    val file = ReportExportUtil.generateCsvReport(
                        context,
                        buildStatementExportData(),
                        "Statement_${account.name}_${System.currentTimeMillis()}",
                        currentLanguage
                    )
                    ReportExportUtil.downloadReportFile(context, file, currentLanguage)
                },
                onShare = {
                    val file = ReportExportUtil.generatePdfReport(
                        context,
                        buildStatementExportData(),
                        "Statement_${account.name}_${System.currentTimeMillis()}",
                        currentLanguage
                    )
                    ReportExportUtil.shareReportFile(
                        context,
                        file,
                        "application/pdf",
                        "Statement for ${account.name}",
                        currentLanguage
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Statement Timeline Items
        val entries = statement?.entries ?: emptyList()
        if (entries.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlainEmptyState(message = ShopLocale.get("no_tx_account_period", currentLanguage))
                }
            }
        } else {
            item {
                Text(
                    text = "Transactions (${entries.size})",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(entries, key = { it.id }) { entry ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StatementEntryRow(
                        entry = entry,
                        currentLanguage = currentLanguage
                    )
                }
            }
        }
    }

    if (showAdjustDialog) {
        AdjustBalanceDialog(
            account = account,
            currentBalance = currentBalance,
            currentLanguage = currentLanguage,
            onDismiss = { showAdjustDialog = false },
            onConfirm = { newBal, note ->
                storeViewModel.adjustAccountBalance(
                    account = account,
                    newBalance = newBal,
                    note = note,
                    onSuccess = { showAdjustDialog = false }
                )
            }
        )
    }

    if (showArchiveConfirm) {
        BasicAlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
        ) {
            ConfigureEdgeToEdgeDialogWindow()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDialogWindowPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = DarkBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Archive Account?",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Are you sure you want to archive '${account.name}'? It will no longer appear in account pickers.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            text = "Cancel",
                            onClick = { showArchiveConfirm = false },
                            modifier = Modifier.weight(1f)
                        )
                        VividButton(
                            text = "Archive",
                            onClick = {
                                storeViewModel.archiveAccount(
                                    account = account,
                                    onSuccess = {
                                        showArchiveConfirm = false
                                        storeViewModel.navigateBackInBusiness()
                                    }
                                )
                            },
                            containerColor = AlertRed,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatementEntryRow(
    entry: AccountStatementEntry,
    currentLanguage: AppLanguage
) {
    val isMoneyIn = entry.moneyIn > 0.0

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        testTag = "statement_entry_${entry.id}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isMoneyIn) Color(0x265DD62C) else Color(0x26FF5252)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMoneyIn) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (isMoneyIn) VividGreen else AlertRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isMoneyIn) Color(0x335DD62C) else Color(0x33FF5252)
                        ) {
                            Text(
                                text = entry.type,
                                color = if (isMoneyIn) VividGreen else AlertRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        if (entry.partyOrCategory.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = entry.partyOrCategory,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (entry.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.note,
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ShopLocale.formatDateShort(entry.timestamp, currentLanguage),
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val amt = if (isMoneyIn) entry.moneyIn else entry.moneyOut
                Text(
                    text = "${if (isMoneyIn) "+" else "-"}${ShopLocale.formatMoneyFixed2(amt, currentLanguage)}",
                    color = if (isMoneyIn) VividGreen else AlertRed,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = ShopLocale.format("balance_short", currentLanguage, ShopLocale.formatMoneyFixed2(entry.runningBalance, currentLanguage)),
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Step 5: Adjust Balance Dialog
 * Enter real current balance. App calculates difference (up or down).
 * Note is required.
 * Confirmation step shows old balance, new balance, and difference before saving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustBalanceDialog(
    account: Account,
    currentBalance: Double,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (newBalance: Double, note: String) -> Unit
) {
    var newBalanceText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val newBalanceParsed = newBalanceText.toDoubleOrNull()
    val difference = if (newBalanceParsed != null) newBalanceParsed - currentBalance else 0.0

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = DarkBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                if (!isConfirmStep) {
                    Text(
                        text = "Adjust Balance: ${account.name}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter actual physical balance. An adjustment entry will be saved.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Current recorded balance
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassSurface)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recorded Balance:",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = ShopLocale.formatMoney(currentBalance, currentLanguage),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassTextField(
                        value = newBalanceText,
                        onValueChange = {
                            newBalanceText = it
                            localError = null
                        },
                        label = "Real Current Balance (৳)",
                        placeholder = "e.g. 5000",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "input_real_balance"
                    )

                    if (newBalanceParsed != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isUp = difference >= 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Adjustment Difference:",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${ShopLocale.formatSignedMoney(difference, currentLanguage)} (${if (isUp) ShopLocale.get("money_in_label", currentLanguage) else ShopLocale.get("money_out_label", currentLanguage)})",
                                color = if (isUp) VividGreen else AlertRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassTextField(
                        value = note,
                        onValueChange = {
                            note = it
                            localError = null
                        },
                        label = "Adjustment Reason / Note (Required)",
                        placeholder = "e.g. Physical count reconciliation, bank fee correction",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "input_adjust_note"
                    )

                    if (localError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = localError ?: "",
                            color = AlertRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                        VividButton(
                            text = "Review",
                            onClick = {
                                if (newBalanceParsed == null) {
                                    localError = "Please enter a valid real balance"
                                    return@VividButton
                                }
                                if (note.trim().isBlank()) {
                                    localError = "A note explaining the adjustment reason is required"
                                    return@VividButton
                                }
                                if (abs(difference) < 0.0001) {
                                    localError = "New balance is identical to the current balance"
                                    return@VividButton
                                }
                                isConfirmStep = true
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "btn_review_adjustment"
                        )
                    }
                } else {
                    // Confirmation Step
                    Text(
                        text = "Confirm Balance Adjustment",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please verify the adjustment details. Balances are never silently modified.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Old Balance:", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = ShopLocale.formatMoneyFixed2(currentBalance, currentLanguage),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (currentLanguage == AppLanguage.BN) "নতুন ব্যালেন্স:" else "New Balance:", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = ShopLocale.formatMoneyFixed2(newBalanceParsed ?: 0.0, currentLanguage),
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (currentLanguage == AppLanguage.BN) "পার্থক্য:" else "Difference:", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = ShopLocale.formatSignedMoney(difference, currentLanguage),
                                color = if (difference >= 0) VividGreen else AlertRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Reason / Note:", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = note.trim(),
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            text = "Back",
                            onClick = { isConfirmStep = false },
                            modifier = Modifier.weight(1f)
                        )
                        VividButton(
                            text = "Confirm & Save",
                            onClick = {
                                onConfirm(newBalanceParsed ?: currentBalance, note.trim())
                            },
                            modifier = Modifier.weight(1.3f),
                            testTag = "btn_confirm_adjustment"
                        )
                    }
                }
            }
        }
    }
}
