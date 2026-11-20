package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BackupPinDialog
import com.example.ui.components.BackupPinDialogMode
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.PlainEmptyState
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BusinessNavDestination
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import com.example.util.SyncStatus

private enum class MoreGroup(val titleEn: String, val titleBn: String) {
    MONEY("MONEY", "অর্থ ও লেনদেন"),
    PEOPLE("PEOPLE", "ব্যক্তি ও পক্ষ"),
    STOCK_BUYING("STOCK & BUYING", "মজুদ ও ক্রয়"),
    TOOLS("TOOLS", "টুলস ও রিপোর্ট"),
    ACCOUNT("ACCOUNT", "অ্যাকাউন্ট")
}

private sealed interface MoreTileAction {
    data class Destination(val dest: BusinessNavDestination) : MoreTileAction
    data object OpenPaymentIn : MoreTileAction
    data object OpenPaymentOut : MoreTileAction
    data object OpenRecordPurchase : MoreTileAction
    data object OpenAddParty : MoreTileAction
    data object OpenProductBin : MoreTileAction
    data object OpenSettings : MoreTileAction
    data object ExitFatherMode : MoreTileAction
    data object LogOut : MoreTileAction
}

private enum class MoreQuickModal {
    NONE,
    PAYMENT_IN,
    PAYMENT_OUT,
    RECORD_PURCHASE,
    ADD_PARTY,
    PRODUCT_BIN
}

private data class MoreMenuEntry(
    val id: String,
    val group: MoreGroup,
    val titleEn: String,
    val titleBn: String,
    val keywords: String,
    val icon: ImageVector,
    val action: MoreTileAction,
    val testTag: String,
    val allowed: Boolean
)

@Composable
fun MoreScreen(
    authViewModel: AuthViewModel,
    storeViewModel: StoreViewModel,
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val destination by storeViewModel.moreNavDestination.collectAsState()

    BackHandler(enabled = destination != BusinessNavDestination.DASHBOARD) {
        storeViewModel.navigateBackInBusiness()
    }

    if (destination == BusinessNavDestination.DASHBOARD) {
        MoreMenuContent(
            authViewModel = authViewModel,
            storeViewModel = storeViewModel,
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            modifier = modifier
        )
    } else {
        BusinessDestinationContent(
            destination = destination,
            authViewModel = authViewModel,
            storeViewModel = storeViewModel,
            onReturnHome = {
                storeViewModel.resetMoreToRoot()
                onBack?.invoke()
            },
            modifier = modifier
        )
    }
}

@Composable
fun MoreMenuContent(
    authViewModel: AuthViewModel,
    storeViewModel: StoreViewModel,
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val syncStatus by storeViewModel.syncStatus.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val isOwner by storeViewModel.isOwner.collectAsState()
    val currentMember by storeViewModel.currentMember.collectAsState()
    val canReports by storeViewModel.canViewReportsFlow.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val canBaki by storeViewModel.canAccessCustomersAndBakiFlow.collectAsState()
    val canReceivePayment by storeViewModel.canReceivePaymentFlow.collectAsState()
    val canSuppliers by storeViewModel.canAccessSuppliersAndPurchasesFlow.collectAsState()
    val canPaySupplier by storeViewModel.canPaySupplierFlow.collectAsState()
    val canStock by storeViewModel.canViewStockFlow.collectAsState()
    val canHistory by storeViewModel.canSeeHistoryFlow.collectAsState()
    val canCashBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()
    val canExpenses by storeViewModel.canAccessExpensesAndIncomeFlow.collectAsState()
    val canNotes by storeViewModel.canAccessNotesFlow.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val isDemoMode = storeViewModel.isDemoMode
    val hasOwnerPin by storeViewModel.hasOwnerPin.collectAsState()
    val productBin by storeViewModel.productBin.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var activeModal by remember { mutableStateOf(MoreQuickModal.NONE) }
    var showExitFatherPinDialog by remember { mutableStateOf(false) }

    val primaryItems = remember(
        isOwner,
        canReports,
        canSeeProfit,
        canBaki,
        canReceivePayment,
        canSuppliers,
        canPaySupplier,
        canStock,
        canCashBank,
        canExpenses,
        canNotes,
        isSimpleMode,
        isFatherMode,
        isDemoMode,
        onOpenSettings
    ) {
        listOf(
            // MONEY
            MoreMenuEntry(
                id = "cash_bank",
                group = MoreGroup.MONEY,
                titleEn = "Cash & Bank",
                titleBn = "ক্যাশ ও ব্যাংক",
                keywords = "cash bank wallet balance accounts transfer bkash nagad",
                icon = Icons.Outlined.AccountBalanceWallet,
                action = MoreTileAction.Destination(BusinessNavDestination.CASH_BANK_SCREEN),
                testTag = "more_item_cash_bank",
                allowed = canCashBank && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "payment_in",
                group = MoreGroup.MONEY,
                titleEn = "Payment In",
                titleBn = "টাকা আদায়",
                keywords = "payment in receive baki customer due collect",
                icon = Icons.AutoMirrored.Filled.CallReceived,
                action = MoreTileAction.OpenPaymentIn,
                testTag = "more_item_payment_in",
                allowed = canReceivePayment
            ),
            MoreMenuEntry(
                id = "payment_out",
                group = MoreGroup.MONEY,
                titleEn = "Payment Out",
                titleBn = "টাকা পরিশোধ",
                keywords = "payment out pay supplier vendor due",
                icon = Icons.AutoMirrored.Filled.CallMade,
                action = MoreTileAction.OpenPaymentOut,
                testTag = "more_item_payment_out",
                allowed = canPaySupplier && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "expenses",
                group = MoreGroup.MONEY,
                titleEn = "Expenses",
                titleBn = "দৈনিক খরচ",
                keywords = "expense expenses cost rent salary bill",
                icon = Icons.Outlined.Percent,
                action = MoreTileAction.Destination(BusinessNavDestination.EXPENSES_SCREEN),
                testTag = "more_item_expenses",
                allowed = canExpenses
            ),
            MoreMenuEntry(
                id = "other_income",
                group = MoreGroup.MONEY,
                titleEn = "Other Income",
                titleBn = "অন্যান্য আয়",
                keywords = "income other income commission cashback",
                icon = Icons.Outlined.AttachMoney,
                action = MoreTileAction.Destination(BusinessNavDestination.OTHER_INCOMES_SCREEN),
                testTag = "more_item_other_income",
                allowed = canExpenses
            ),

            // PEOPLE
            MoreMenuEntry(
                id = "add_party",
                group = MoreGroup.PEOPLE,
                titleEn = "Add Party",
                titleBn = "পার্টি যোগ",
                keywords = "party parties add customer supplier contact",
                icon = Icons.Outlined.PersonAdd,
                action = MoreTileAction.OpenAddParty,
                testTag = "more_item_add_party",
                allowed = !isFatherMode && !isSimpleMode && (canBaki || canSuppliers)
            ),
            MoreMenuEntry(
                id = "customers_baki",
                group = MoreGroup.PEOPLE,
                titleEn = "Customer Baki",
                titleBn = "ক্রেতা ও বাকি",
                keywords = "customer customers baki due debtors payment",
                icon = Icons.Outlined.People,
                action = MoreTileAction.Destination(BusinessNavDestination.BAKI_SCREEN),
                testTag = "more_item_baki",
                allowed = canBaki
            ),
            MoreMenuEntry(
                id = "suppliers",
                group = MoreGroup.PEOPLE,
                titleEn = "Suppliers",
                titleBn = "সরবরাহকারী",
                keywords = "supplier suppliers vendor due",
                icon = Icons.Outlined.Storefront,
                action = MoreTileAction.Destination(BusinessNavDestination.SUPPLIERS_SCREEN),
                testTag = "more_item_suppliers",
                allowed = canSuppliers && !isFatherMode && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "staff_permissions",
                group = MoreGroup.PEOPLE,
                titleEn = "Staff",
                titleBn = "কর্মচারী",
                keywords = "staff team member permissions role invite",
                icon = Icons.Outlined.Badge,
                action = MoreTileAction.Destination(BusinessNavDestination.STAFF_MANAGEMENT_SCREEN),
                testTag = "more_item_staff",
                allowed = isOwner && !isFatherMode && !isSimpleMode && !isDemoMode
            ),

            // STOCK & BUYING
            MoreMenuEntry(
                id = "purchases",
                group = MoreGroup.STOCK_BUYING,
                titleEn = "Purchases",
                titleBn = "ক্রয় তালিকা",
                keywords = "purchase purchases buy stock report",
                icon = Icons.Outlined.ShoppingCart,
                action = MoreTileAction.Destination(BusinessNavDestination.REPORT_PURCHASES),
                testTag = "more_item_purchases",
                allowed = canReports && canSuppliers && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "record_purchase",
                group = MoreGroup.STOCK_BUYING,
                titleEn = "Buy Stock",
                titleBn = "স্টক ক্রয়",
                keywords = "record purchase restock buy supplier",
                icon = Icons.Outlined.ShoppingBag,
                action = MoreTileAction.OpenRecordPurchase,
                testTag = "more_item_record_purchase",
                allowed = canSuppliers && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "categories",
                group = MoreGroup.STOCK_BUYING,
                titleEn = "Categories",
                titleBn = "ক্যাটাগরি",
                keywords = "category categories manage",
                icon = Icons.Outlined.Category,
                action = MoreTileAction.Destination(BusinessNavDestination.MANAGE_CATEGORIES_SCREEN),
                testTag = "more_item_categories",
                allowed = canExpenses && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "product_bin",
                group = MoreGroup.STOCK_BUYING,
                titleEn = "Bin",
                titleBn = "বিন",
                keywords = "bin trash recycle deleted restore product",
                icon = Icons.Outlined.DeleteOutline,
                action = MoreTileAction.OpenProductBin,
                testTag = "more_item_bin",
                allowed = canStock && !isFatherMode && !isSimpleMode
            ),

            // TOOLS
            MoreMenuEntry(
                id = "notes",
                group = MoreGroup.TOOLS,
                titleEn = "Notebook",
                titleBn = "নোটবুক",
                keywords = "note notes notebook reminder todo",
                icon = Icons.Outlined.Description,
                action = MoreTileAction.Destination(BusinessNavDestination.NOTES_SCREEN),
                testTag = "more_item_notes",
                allowed = canNotes
            ),
            MoreMenuEntry(
                id = "greeting_cards",
                group = MoreGroup.TOOLS,
                titleEn = "Greeting Cards",
                titleBn = "শুভেচ্ছা কার্ড",
                keywords = "greeting cards business card visiting card বিজনেস কার্ড গ্রিটিং কার্ড",
                icon = Icons.Outlined.Style,
                action = MoreTileAction.Destination(BusinessNavDestination.GREETING_CARDS_SCREEN),
                testTag = "more_item_greeting_cards",
                allowed = !isFatherMode
            ),
            MoreMenuEntry(
                id = "financial_reports",
                group = MoreGroup.TOOLS,
                titleEn = "Reports",
                titleBn = "রিপোর্ট",
                keywords = "financial reports report statement pdf csv daybook profit loss",
                icon = Icons.Outlined.Assessment,
                action = MoreTileAction.Destination(BusinessNavDestination.FINANCIAL_REPORTS_HUB),
                testTag = "more_item_financial_reports",
                allowed = canReports && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "monthly_report",
                group = MoreGroup.TOOLS,
                titleEn = "Monthly Report",
                titleBn = "মাসিক রিপোর্ট",
                keywords = "monthly report summary month",
                icon = Icons.Outlined.CalendarMonth,
                action = MoreTileAction.Destination(BusinessNavDestination.MONTHLY_REPORT),
                testTag = "more_item_monthly_report",
                allowed = canReports && canSeeProfit && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "data_health",
                group = MoreGroup.TOOLS,
                titleEn = "Data Health",
                titleBn = "তথ্য যাচাই",
                keywords = "data health check verify integrity",
                icon = Icons.Outlined.HealthAndSafety,
                action = MoreTileAction.Destination(BusinessNavDestination.DATA_HEALTH_CHECK),
                testTag = "more_item_data_health",
                allowed = isOwner && !isSimpleMode && !isFatherMode
            ),
            MoreMenuEntry(
                id = "feature_checklist",
                group = MoreGroup.TOOLS,
                titleEn = "Checklist",
                titleBn = "চেকলিস্ট",
                keywords = "checklist test verification sync",
                icon = Icons.Outlined.CheckCircle,
                action = MoreTileAction.Destination(BusinessNavDestination.TEST_CHECKLIST),
                testTag = "more_item_test_checklist",
                allowed = isOwner && !isFatherMode && !isSimpleMode
            ),

            // ACCOUNT
            MoreMenuEntry(
                id = "exit_father_mode",
                group = MoreGroup.ACCOUNT,
                titleEn = "Turn Off Lock",
                titleBn = "লক বন্ধ",
                keywords = "father mode lock turn off owner pin",
                icon = Icons.Outlined.LockOpen,
                action = MoreTileAction.ExitFatherMode,
                testTag = "more_item_exit_father_mode",
                allowed = isFatherMode && isOwner
            ),
            MoreMenuEntry(
                id = "settings",
                group = MoreGroup.ACCOUNT,
                titleEn = "Settings",
                titleBn = "সেটিংস",
                keywords = "settings preferences mode language privacy",
                icon = Icons.Outlined.Settings,
                action = MoreTileAction.OpenSettings,
                testTag = "more_item_settings",
                allowed = onOpenSettings != null && !isFatherMode
            ),
            MoreMenuEntry(
                id = "logout",
                group = MoreGroup.ACCOUNT,
                titleEn = if (isDemoMode) "Exit Demo" else "Log Out",
                titleBn = if (isDemoMode) "ডেমো বন্ধ করুন" else "লগ আউট",
                keywords = if (isDemoMode) "exit demo leave sample shop" else "logout sign out exit account",
                icon = Icons.AutoMirrored.Filled.Logout,
                action = MoreTileAction.LogOut,
                testTag = "more_item_logout",
                allowed = true
            )
        ).filter { it.allowed }
    }

    val subReportItems = remember(
        canReports,
        canHistory,
        canSeeProfit,
        canExpenses,
        canBaki,
        canSuppliers,
        canStock,
        isSimpleMode,
        isFatherMode,
        isDemoMode
    ) {
        listOf(
            MoreMenuEntry(
                id = "sub_sales_report",
                group = MoreGroup.TOOLS,
                titleEn = "Sales Report",
                titleBn = "বিক্রয় রিপোর্ট",
                keywords = "sales report invoice",
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                action = MoreTileAction.Destination(BusinessNavDestination.REPORT_SALES),
                testTag = "more_search_report_sales",
                allowed = canReports && canHistory && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "sub_day_book",
                group = MoreGroup.TOOLS,
                titleEn = "Day Book",
                titleBn = "ডে বুক",
                keywords = "day book daybook ledger",
                icon = Icons.Outlined.Book,
                action = MoreTileAction.Destination(BusinessNavDestination.REPORT_DAY_BOOK),
                testTag = "more_search_report_daybook",
                allowed = canReports && !isSimpleMode && !isFatherMode
            ),
            MoreMenuEntry(
                id = "sub_pnl",
                group = MoreGroup.TOOLS,
                titleEn = "Profit & Loss",
                titleBn = "লাভ ও ক্ষতি",
                keywords = "profit loss pnl net",
                icon = Icons.Outlined.AccountBalance,
                action = MoreTileAction.Destination(BusinessNavDestination.REPORT_PROFIT_LOSS),
                testTag = "more_search_report_pnl",
                allowed = canReports && canSeeProfit && (!isSimpleMode || isFatherMode)
            ),
            MoreMenuEntry(
                id = "sub_expense_report",
                group = MoreGroup.TOOLS,
                titleEn = "Expense Report",
                titleBn = "খরচ রিপোর্ট",
                keywords = "expense report",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                action = MoreTileAction.Destination(BusinessNavDestination.REPORT_EXPENSES),
                testTag = "more_search_report_expenses",
                allowed = canReports && canExpenses && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "sub_income_report",
                group = MoreGroup.TOOLS,
                titleEn = "Income Report",
                titleBn = "আয় রিপোর্ট",
                keywords = "other income report",
                icon = Icons.Outlined.AttachMoney,
                action = MoreTileAction.Destination(BusinessNavDestination.REPORT_INCOMES),
                testTag = "more_search_report_income",
                allowed = canReports && canExpenses && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "sub_customer_stmt",
                group = MoreGroup.TOOLS,
                titleEn = "Customer Statement",
                titleBn = "ক্রেতা স্টেটমেন্ট",
                keywords = "customer statement ledger",
                icon = Icons.Outlined.People,
                action = MoreTileAction.Destination(BusinessNavDestination.REPORT_CUSTOMER_STATEMENT),
                testTag = "more_search_report_customer_stmt",
                allowed = canReports && canBaki && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "sub_supplier_stmt",
                group = MoreGroup.TOOLS,
                titleEn = "Supplier Statement",
                titleBn = "মহাজন স্টেটমেন্ট",
                keywords = "supplier statement ledger",
                icon = Icons.Outlined.ShoppingCart,
                action = MoreTileAction.Destination(BusinessNavDestination.REPORT_SUPPLIER_STATEMENT),
                testTag = "more_search_report_supplier_stmt",
                allowed = canReports && canSuppliers && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "sub_inventory_report",
                group = MoreGroup.TOOLS,
                titleEn = "Inventory Report",
                titleBn = "মজুদ রিপোর্ট",
                keywords = "inventory stock report valuation",
                icon = Icons.Outlined.Inventory2,
                action = MoreTileAction.Destination(BusinessNavDestination.REPORT_INVENTORY),
                testTag = "more_search_report_inventory",
                allowed = canReports && canStock && !isSimpleMode
            ),
            MoreMenuEntry(
                id = "sub_staff_report",
                group = MoreGroup.TOOLS,
                titleEn = "Staff Report",
                titleBn = "কর্মচারী রিপোর্ট",
                keywords = "staff performance report",
                icon = Icons.Outlined.Badge,
                action = MoreTileAction.Destination(BusinessNavDestination.REPORT_STAFF),
                testTag = "more_search_report_staff",
                allowed = canReports && !isFatherMode && !isSimpleMode && !isDemoMode
            )
        ).filter { it.allowed }
    }

    val query = searchQuery.trim().lowercase()
    val filteredPrimary = remember(primaryItems, query) {
        if (query.isBlank()) {
            primaryItems
        } else {
            primaryItems.filter { item ->
                item.titleEn.lowercase().contains(query) ||
                    item.titleBn.lowercase().contains(query) ||
                    item.keywords.contains(query)
            }
        }
    }
    val filteredReports = remember(subReportItems, query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            subReportItems.filter { item ->
                item.titleEn.lowercase().contains(query) ||
                    item.titleBn.lowercase().contains(query) ||
                    item.keywords.contains(query)
            }
        }
    }

    val groupedPrimary = remember(filteredPrimary) {
        MoreGroup.values().mapNotNull { group ->
            val items = filteredPrimary.filter { it.group == group }
            if (items.isEmpty()) null else group to items
        }
    }

    fun handleAction(action: MoreTileAction) {
        when (action) {
            is MoreTileAction.Destination -> storeViewModel.navigateInBusiness(action.dest)
            MoreTileAction.OpenPaymentIn -> activeModal = MoreQuickModal.PAYMENT_IN
            MoreTileAction.OpenPaymentOut -> activeModal = MoreQuickModal.PAYMENT_OUT
            MoreTileAction.OpenRecordPurchase -> activeModal = MoreQuickModal.RECORD_PURCHASE
            MoreTileAction.OpenAddParty -> activeModal = MoreQuickModal.ADD_PARTY
            MoreTileAction.OpenProductBin -> activeModal = MoreQuickModal.PRODUCT_BIN
            MoreTileAction.OpenSettings -> onOpenSettings?.invoke()
            MoreTileAction.ExitFatherMode -> showExitFatherPinDialog = true
            MoreTileAction.LogOut -> authViewModel.signOut(context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.45f, size.height * 0.14f),
                        radius = size.width * 0.85f
                    )
                )
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columnCount = if (maxWidth < 420.dp) 3 else 4

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("more_menu_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Compact Top Header: Back arrow + More
                item(key = "more_top_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("more_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (currentLanguage == AppLanguage.BN) "ফিরে যান" else "Back",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "আরও" else "More",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                    // Glowing Store Card (collapses during active search so results sit immediately below search)
                    if (query.isBlank()) {
                        item(key = "more_store_card") {
                            val roleLabel = if (isOwner) {
                                ShopLocale.get("owner_badge", currentLanguage)
                            } else {
                                currentMember?.roleLabel?.ifBlank {
                                    if (currentLanguage == AppLanguage.BN) "কর্মী" else "Staff"
                                } ?: if (currentLanguage == AppLanguage.BN) "কর্মী" else "Staff"
                            }
                            val metaLine = listOfNotNull(
                                store?.address?.takeIf { it.isNotBlank() },
                                store?.phone?.takeIf { it.isNotBlank() }
                            ).joinToString(" · ").ifBlank {
                                val modeName = if (isSimpleMode) {
                                    ShopLocale.get("simple_mode", currentLanguage)
                                } else {
                                    ShopLocale.get("detailed_mode", currentLanguage)
                                }
                                "$roleLabel · $modeName"
                            }

                            MoreStoreHeroCard(
                                storeName = store?.name?.ifBlank { "StoreOS" } ?: "StoreOS",
                                subtitle = metaLine,
                                syncStatus = syncStatus,
                                isDemoMode = authUiState is AuthUiState.DemoMode,
                                currentLanguage = currentLanguage
                            )
                        }
                    }

                    // Compact Dark Glass Search Field
                    item(key = "more_search") {
                        MoreSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            currentLanguage = currentLanguage
                        )
                    }

                    if (groupedPrimary.isEmpty() && filteredReports.isEmpty()) {
                        item(key = "more_empty") {
                            PlainEmptyState(
                                message = if (currentLanguage == AppLanguage.BN) {
                                    "কোনো ফলাফল পাওয়া যায়নি"
                                } else {
                                    "No matching tools found"
                                }
                            )
                        }
                    } else {
                        groupedPrimary.forEach { (group, groupItems) ->
                            item(key = "header_${group.name}") {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) group.titleBn else group.titleEn,
                                    color = TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                                )
                            }

                            val rows = groupItems.chunked(columnCount)
                            items(
                                count = rows.size,
                                key = { idx -> "grid_${group.name}_$idx" }
                            ) { idx ->
                                val rowItems = rows[idx]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { entry ->
                                        MoreGridTile(
                                            label = if (currentLanguage == AppLanguage.BN) entry.titleBn else entry.titleEn,
                                            icon = entry.icon,
                                            testTag = entry.testTag,
                                            onClick = { handleAction(entry.action) },
                                            isFatherMode = isFatherMode,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(columnCount - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        if (filteredReports.isNotEmpty()) {
                            item(key = "header_search_reports") {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "রিপোর্টসমূহ" else "REPORTS",
                                    color = TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                                )
                            }

                            val reportRows = filteredReports.chunked(columnCount)
                            items(
                                count = reportRows.size,
                                key = { idx -> "grid_reports_$idx" }
                            ) { idx ->
                                val rowItems = reportRows[idx]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { entry ->
                                        MoreGridTile(
                                            label = if (currentLanguage == AppLanguage.BN) entry.titleBn else entry.titleEn,
                                            icon = entry.icon,
                                            testTag = entry.testTag,
                                            onClick = { handleAction(entry.action) },
                                            isFatherMode = isFatherMode,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(columnCount - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

    if (showExitFatherPinDialog && isOwner) {
        val targetMemberId = currentMember?.userId?.ifBlank { null } ?: store?.ownerId ?: ""
        BackupPinDialog(
            mode = if (hasOwnerPin) BackupPinDialogMode.ENTER_PIN else BackupPinDialogMode.SET_PIN,
            currentLanguage = currentLanguage,
            customDescription = ShopLocale.get("father_mode_pin_toggle_desc", currentLanguage),
            onSetPin = { pin, _ ->
                storeViewModel.setOwnerPin(pin) {
                    storeViewModel.setFatherModeForMember(
                        memberId = targetMemberId,
                        enabled = false,
                        memberDisplayName = currentMember?.displayName ?: "",
                        memberEmail = currentMember?.email ?: "",
                        onSuccess = { showExitFatherPinDialog = false }
                    )
                }
            },
            onVerifyPin = { pin, onResult ->
                storeViewModel.verifyOwnerPin(pin) { ok ->
                    onResult(ok)
                    if (ok) {
                        storeViewModel.setFatherModeForMember(
                            memberId = targetMemberId,
                            enabled = false,
                            memberDisplayName = currentMember?.displayName ?: "",
                            memberEmail = currentMember?.email ?: "",
                            onSuccess = { showExitFatherPinDialog = false }
                        )
                    }
                }
            },
            onChangePin = { _, _, _ -> },
            onDismiss = { showExitFatherPinDialog = false }
        )
    }

    when (activeModal) {
        MoreQuickModal.PAYMENT_IN -> HomePaymentInDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { activeModal = MoreQuickModal.NONE }
        )
        MoreQuickModal.PAYMENT_OUT -> HomePaymentOutDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { activeModal = MoreQuickModal.NONE }
        )
        MoreQuickModal.RECORD_PURCHASE -> HomeRecordPurchaseDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { activeModal = MoreQuickModal.NONE }
        )
        MoreQuickModal.ADD_PARTY -> HomeAddPartyDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            canAddCustomer = canBaki,
            canAddSupplier = canSuppliers,
            onDismiss = { activeModal = MoreQuickModal.NONE }
        )
        MoreQuickModal.PRODUCT_BIN -> MoreProductBinDialog(
            products = productBin,
            isOwner = isOwner,
            currentLanguage = currentLanguage,
            onRestore = { storeViewModel.restoreProductFromBin(it.id) },
            onEmptyBin = { storeViewModel.emptyProductBin() },
            onDismiss = { activeModal = MoreQuickModal.NONE }
        )
        MoreQuickModal.NONE -> {}
    }
}

@Composable
private fun MoreProductBinDialog(
    products: List<com.example.data.model.Product>,
    isOwner: Boolean,
    currentLanguage: AppLanguage,
    onRestore: (com.example.data.model.Product) -> Unit,
    onEmptyBin: () -> Unit,
    onDismiss: () -> Unit
) {
    val isBn = currentLanguage == AppLanguage.BN
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF141814),
            border = BorderStroke(1.dp, Color(0x38FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("more_product_bin_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "পণ্য বিন (${ShopLocale.formatNumber(products.size.toLong(), currentLanguage)})" else "Product Bin (${products.size})",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isOwner && products.isNotEmpty()) {
                        Surface(
                            onClick = onEmptyBin,
                            shape = RoundedCornerShape(14.dp),
                            color = AlertRed.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("more_empty_bin_btn")
                        ) {
                            Text(
                                text = if (isBn) "সব মুছুন" else "Empty Bin",
                                color = AlertRed,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (products.isEmpty()) {
                    PlainEmptyState(
                        message = if (isBn) "রিসাইকেল বিন খালি" else "Recycle Bin is empty"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 100.dp, max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products.size, key = { products[it].id }) { idx ->
                            val item = products[idx]
                            val unitLabel = ShopLocale.displayUnit(item.unit, currentLanguage)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1D221D),
                                border = BorderStroke(1.dp, Color(0x28FFFFFF))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = if (isBn && item.nameBn.isNotBlank()) item.nameBn else item.name,
                                            color = TextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${ShopLocale.formatNumber(item.stock, currentLanguage)} $unitLabel · ${ShopLocale.formatMoney(item.sellPrice, currentLanguage)}",
                                            color = TextSecondary,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        onClick = { onRestore(item) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = VividGreen.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.55f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.RestoreFromTrash,
                                                contentDescription = null,
                                                tint = VividGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (isBn) "ফেরত নিন" else "Restore",
                                                color = VividGreen,
                                                fontSize = 11.5.sp,
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

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF242924),
                    border = BorderStroke(1.dp, Color(0x30FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isBn) "বন্ধ করুন" else "Close",
                            color = TextPrimary,
                            fontSize = 13.sp,
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

@Composable
private fun MoreStoreHeroCard(
    storeName: String,
    subtitle: String,
    syncStatus: SyncStatus,
    isDemoMode: Boolean,
    currentLanguage: AppLanguage
) {
    val cardShape = RoundedCornerShape(22.dp)
    val (dotColor, statusText) = when {
        isDemoMode -> VividGreen to if (currentLanguage == AppLanguage.BN) {
            "ডেমো স্টোর · লোকাল প্রিভিউ"
        } else {
            "Demo mode · Local preview"
        }
        syncStatus == SyncStatus.ONLINE -> VividGreen to if (currentLanguage == AppLanguage.BN) {
            "ক্লাউড সিঙ্ক · সংযুক্ত"
        } else {
            "Cloud sync · Connected"
        }
        syncStatus == SyncStatus.SYNCING -> Color(0xFFF3B23E) to if (currentLanguage == AppLanguage.BN) {
            "ক্লাউড সিঙ্ক · সিঙ্ক হচ্ছে..."
        } else {
            "Cloud sync · Syncing..."
        }
        else -> AlertRed to if (currentLanguage == AppLanguage.BN) {
            "অফলাইন · লোকাল ক্যাশ"
        } else {
            "Offline · Local cache"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = cardShape,
                spotColor = VividGreen.copy(alpha = 0.45f),
                ambientColor = VividGreen.copy(alpha = 0.25f)
            )
            .clip(cardShape)
            .testTag("more_store_card"),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    VividGreen.copy(alpha = 0.88f),
                    VividGreen.copy(alpha = 0.34f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E3716),
                            Color(0xFF131B11),
                            Color(0xFF0E110E)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Circular dark-glass emblem with crisp green swirl
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF2B4A22), Color(0xFF162115))
                        )
                    )
                    .border(1.dp, Color(0x40FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(30.dp)) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension * 0.42f
                    drawCircle(
                        color = VividGreen.copy(alpha = 0.20f),
                        radius = r
                    )
                    for (angle in listOf(0f, 90f, 180f, 270f)) {
                        drawArc(
                            color = VividGreen,
                            startAngle = angle,
                            sweepAngle = 58f,
                            useCenter = false,
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    drawCircle(
                        color = Color(0xFFB5F59A),
                        radius = size.minDimension * 0.13f,
                        center = c
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = storeName,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = statusText,
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    currentLanguage: AppLanguage
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isFocused) Color(0xD91A1E1A) else Color(0xBF151815),
        border = BorderStroke(
            width = 1.dp,
            color = if (isFocused) VividGreen.copy(alpha = 0.72f) else Color(0x28FFFFFF)
        ),
        modifier = Modifier
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
                modifier = Modifier.size(18.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) {
                            "খরচ, নোট, রিপোর্ট খুঁজুন..."
                        } else {
                            "Search Cash, Expenses, Reports..."
                        },
                        color = TextSecondary,
                        fontSize = 13.sp,
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
                        fontSize = 13.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .testTag("more_search_input")
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("more_search_clear")
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

@Composable
private fun MoreGridTile(
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFatherMode: Boolean = false
) {
    val tileShape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = if (isFatherMode) 104.dp else 88.dp)
            .clip(tileShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = tileShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x46FFFFFF),
                    Color(0x16FFFFFF)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF282B28),
                            Color(0xFF141614)
                        )
                    )
                )
                .padding(horizontal = 6.dp, vertical = if (isFatherMode) 14.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = VividGreen,
                modifier = Modifier.size(if (isFatherMode) 28.dp else 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = if (isFatherMode) 13.5.sp else 11.sp,
                lineHeight = if (isFatherMode) 16.sp else 13.5.sp,
                fontWeight = if (isFatherMode) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
