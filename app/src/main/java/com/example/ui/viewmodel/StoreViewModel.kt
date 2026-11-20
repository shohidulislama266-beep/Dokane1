package com.example.ui.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Account
import com.example.data.model.AccountAdjustment
import com.example.data.model.AccountStatementReport
import com.example.data.model.AiKeyUiSummary
import com.example.data.model.BackupImportMode
import com.example.data.model.BackupImportPreview
import com.example.data.model.BackupImportSummary
import com.example.data.model.BusinessCardProfileSeed
import com.example.data.model.BusinessStrategyDefaults
import com.example.data.model.CompletedSaleReceipt
import com.example.data.model.CsvExportTarget
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.HealthFixLog
import com.example.data.model.OtherIncome
import com.example.data.model.Payment
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseItem
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SavedBusinessStrategy
import com.example.data.model.Store
import com.example.data.model.StoreCategory
import com.example.data.model.StoreInvite
import com.example.data.model.StoreMember
import com.example.data.model.StoreNote
import com.example.data.model.StorePermissions
import com.example.data.model.Supplier
import com.example.data.model.SupplierPayment
import com.example.data.model.UserProfile
import com.example.data.repository.StoreRepository
import com.example.domain.BakiOverview
import com.example.domain.BusinessStrategyBanglaBuilder
import com.example.domain.BusinessStrategyEngine
import com.example.domain.CalculationEngine
import com.example.domain.CategorySalesStat
import com.example.domain.CustomerBalanceSummary
import com.example.domain.CustomerLedgerEntry
import com.example.domain.CustomerStatementReport
import com.example.domain.DailySalesPoint
import com.example.domain.DayBookEntry
import com.example.domain.HealthProblem
import com.example.domain.HomeInsights
import com.example.domain.MoneyNowSummary
import com.example.domain.MonthlyReportData
import com.example.domain.PeriodFinancialSummary
import com.example.domain.ProductSoldStat
import com.example.domain.ProfitAndLossStatement
import com.example.domain.SevenDayCashflowSummary
import com.example.domain.ShopType
import com.example.domain.ShopTypeCatalog
import com.example.domain.ShopUnitPreset
import com.example.domain.StaffReportSummary
import com.example.domain.StockSummary
import com.example.domain.StoreFinancials
import com.example.domain.SupplierStatementReport
import com.example.ui.components.ShopNavTab
import com.example.util.AppLanguage
import com.example.util.BusinessCardLocalStore
import com.example.util.FirestoreErrorHandler
import com.example.util.NetworkMonitor
import com.example.util.PhotoStorageMode
import com.example.util.ReceiptImageStyle
import com.example.util.ReportExportUtil
import com.example.util.ReportTableData
import com.example.util.ShopLocale
import com.example.util.StockPreferences
import com.example.util.StoreBackupCodec
import com.example.util.SyncStatus
import com.google.firebase.Timestamp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BusinessNavDestination {
    DASHBOARD,
    FINANCIAL_REPORTS_HUB,
    BAKI_SCREEN,
    SUPPLIERS_SCREEN,
    LOW_STOCK_SCREEN,
    SLOW_SELLERS_SCREEN,
    REPORT_SALES,
    REPORT_PURCHASES,
    REPORT_DAY_BOOK,
    REPORT_PROFIT_LOSS,
    REPORT_CUSTOMER_STATEMENT,
    REPORT_SUPPLIER_STATEMENT,
    REPORT_INVENTORY,
    MONTHLY_REPORT,
    DATA_HEALTH_CHECK,
    TEST_CHECKLIST,
    CASH_BANK_SCREEN,
    ACCOUNT_DETAILS_SCREEN,
    EXPENSES_SCREEN,
    OTHER_INCOMES_SCREEN,
    MANAGE_CATEGORIES_SCREEN,
    NOTES_SCREEN,
    REPORT_EXPENSES,
    REPORT_INCOMES,
    STAFF_MANAGEMENT_SCREEN,
    STAFF_MEMBER_DETAILS_SCREEN,
    REPORT_STAFF,
    GREETING_CARDS_SCREEN
}

enum class BusinessSubScreen {
    NONE,
    CUSTOMER_DETAILS
}

enum class HistoryPeriod(val labelEn: String, val labelBn: String) {
    TODAY("Today", "আজ"),
    WEEK("Week", "সপ্তাহ"),
    MONTH("Month", "মাস"),
    YEAR("Year", "বছর"),
    ALL("All", "সব"),
    CUSTOM("Custom", "তারিখ")
}

enum class HistoryFilter(val labelEn: String, val labelBn: String) {
    ALL("All", "সব"),
    SALES_ONLY("Sales", "বিক্রি"),
    PURCHASES_ONLY("Purchases", "ক্রয়")
}

enum class HistoryViewMode {
    TRANSACTIONS,
    PRODUCTS_SOLD
}

sealed class HistoryTimelineItem(open val id: String, open val timestamp: Long) {
    data class SaleItemRow(val sale: Sale) :
        HistoryTimelineItem(sale.id, sale.createdAt?.toDate()?.time ?: 0L)

    data class PurchaseItemRow(val purchase: Purchase) :
        HistoryTimelineItem(purchase.id, purchase.createdAt?.toDate()?.time ?: 0L)

    data class PaymentItemRow(val payment: Payment) :
        HistoryTimelineItem(
            payment.id,
            if (payment.paymentDate > 0L) payment.paymentDate else (payment.createdAt?.toDate()?.time ?: 0L)
        )

    data class ExpenseItemRow(val expense: Expense) :
        HistoryTimelineItem(
            expense.id,
            if (expense.date > 0L) expense.date else (expense.createdAt?.toDate()?.time ?: 0L)
        )

    data class OtherIncomeItemRow(val income: OtherIncome) :
        HistoryTimelineItem(
            income.id,
            if (income.date > 0L) income.date else (income.createdAt?.toDate()?.time ?: 0L)
        )

    data class AdjustmentItemRow(val adjustment: AccountAdjustment) :
        HistoryTimelineItem(adjustment.id, adjustment.createdAt?.toDate()?.time ?: 0L)
}

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModel(
    val storeId: String,
    private val repository: StoreRepository,
    private val networkMonitor: NetworkMonitor,
    val currentUserId: String = repository.currentUid ?: "demo_user"
) : ViewModel() {

    val isDemoMode: Boolean get() = storeId == "demo_store"

    private val stockPrefs: StockPreferences? = repository.appContext?.let { StockPreferences.forStore(it, storeId) }

    val currentLanguage: StateFlow<AppLanguage> = repository.currentLanguage

    val store: StateFlow<Store?> = repository.observeStore(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val members: StateFlow<List<StoreMember>> = repository.observeMembers(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentMember: StateFlow<StoreMember?> = repository.observeMyMember(storeId, currentUserId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val currentUserProfile: StateFlow<UserProfile?> =
        if (storeId == "demo_store") {
            flowOf(null)
        } else {
            repository.observeUserProfile(currentUserId)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isOwner: StateFlow<Boolean> = combine(store, currentMember) { st, mem ->
        if (storeId == "demo_store") {
            true
        } else if (st != null && st.ownerId.isNotBlank()) {
            st.ownerId == currentUserId || mem?.role.equals("owner", ignoreCase = true)
        } else {
            mem?.role.equals("owner", ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, storeId == "demo_store")

    val invites: StateFlow<List<StoreInvite>> = isOwner
        .flatMapLatest { owner ->
            if (owner) repository.observeInvites(storeId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isRemoved: StateFlow<Boolean> = combine(isOwner, currentMember) { owner, mem ->
        !owner && mem?.status.equals("Removed", ignoreCase = true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isFatherMode: StateFlow<Boolean> = currentMember.map { mem ->
        mem?.permissions?.get(StorePermissions.FATHER_MODE) == true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _localSimpleMode = MutableStateFlow(repository.getCachedUserSimpleMode(currentUserId))
    private val _isSimpleMode = MutableStateFlow(_localSimpleMode.value)
    val isSimpleMode: StateFlow<Boolean> = _isSimpleMode.asStateFlow()

    val shopType: StateFlow<ShopType?> = store.map { st ->
        ShopType.fromId(st?.shopType)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val unitPresets: StateFlow<List<ShopUnitPreset>> = shopType.map { stType ->
        if (stType != null) ShopTypeCatalog.unitPresets(stType) else emptyList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val photoStorageMode: StateFlow<PhotoStorageMode> =
        stockPrefs?.photoStorageMode ?: MutableStateFlow(PhotoStorageMode.SYNC).asStateFlow()

    val receiptImageStyle: StateFlow<ReceiptImageStyle> =
        stockPrefs?.receiptImageStyle ?: MutableStateFlow(ReceiptImageStyle.LIGHT).asStateFlow()

    val hasOwnerPin: StateFlow<Boolean> =
        stockPrefs?.hasOwnerPinFlow ?: MutableStateFlow(false).asStateFlow()

    val showProfitInSimpleMode: StateFlow<Boolean> =
        stockPrefs?.showProfitInSimpleModeFlow ?: MutableStateFlow(false).asStateFlow()

    val pinLockoutUntilMs: StateFlow<Long> =
        stockPrefs?.pinLockoutUntilMsFlow ?: MutableStateFlow(0L).asStateFlow()

    val lastUsedAccountId: StateFlow<String> =
        stockPrefs?.lastUsedAccountIdFlow ?: MutableStateFlow("cash_in_hand").asStateFlow()

    private val _isPrivacyHidden = MutableStateFlow(false)
    val isPrivacyHidden: StateFlow<Boolean> = _isPrivacyHidden.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = combine(
        networkMonitor.isOnline,
        _isBusy
    ) { online, busy ->
        when {
            storeId == "demo_store" -> SyncStatus.ONLINE
            !online -> SyncStatus.OFFLINE
            busy -> SyncStatus.SYNCING
            else -> SyncStatus.ONLINE
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SyncStatus.ONLINE)

    // Collections
    val products: StateFlow<List<Product>> = repository.observeProducts(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val productBin: StateFlow<List<Product>> = repository.observeProductBin(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sales: StateFlow<List<Sale>> = repository.observeSales(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customers: StateFlow<List<Customer>> = repository.observeCustomers(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val payments: StateFlow<List<Payment>> = repository.observePayments(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.observeSuppliers(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val purchases: StateFlow<List<Purchase>> = repository.observePurchases(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val purchaseReturns: StateFlow<List<PurchaseReturn>> = repository.observePurchaseReturns(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val supplierPayments: StateFlow<List<SupplierPayment>> = repository.observeSupplierPayments(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val accounts: StateFlow<List<Account>> = repository.observeAccounts(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val adjustments: StateFlow<List<AccountAdjustment>> = repository.observeAdjustments(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val expenses: StateFlow<List<Expense>> = repository.observeExpenses(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val otherIncomes: StateFlow<List<OtherIncome>> = repository.observeOtherIncomes(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<StoreCategory>> = repository.observeCategories(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notes: StateFlow<List<StoreNote>> = repository.observeNotes(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val healthFixLogs: StateFlow<List<HealthFixLog>> = repository.observeHealthFixLogs(storeId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val availableProductCategories: StateFlow<List<String>> = combine(
        store,
        categories,
        products
    ) { st, cats, prods ->
        val ordered = linkedSetOf<String>()
        st?.productCategories.orEmpty()
            .filter { it.isNotBlank() }
            .forEach { ordered.add(it.trim()) }
        cats.filter { !it.isArchived && it.type.equals("PRODUCT", ignoreCase = true) && it.name.isNotBlank() }
            .forEach { ordered.add(it.name.trim()) }
        prods.filter { it.category.isNotBlank() }
            .forEach { ordered.add(it.category.trim()) }
        if (ordered.isEmpty()) ordered.add("General")
        ordered.toList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf("General"))

    // Permission Flows
    private fun permissionFlow(key: String, defaultIfOwner: Boolean = true): StateFlow<Boolean> =
        combine(isOwner, currentMember) { owner, mem ->
            if (owner) {
                true
            } else {
                val map = mem?.permissions
                if (map == null || map.isEmpty()) defaultIfOwner else (map[key] ?: false)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, storeId == "demo_store")

    val canSellFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.SELL)
    val canViewStockFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.VIEW_STOCK)
    val canAddEditProductsFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.ADD_EDIT_PRODUCTS)
    val canSeeBuyingPriceAndProfitFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.SEE_BUYING_PRICE_AND_PROFIT)
    val canSeeHistoryFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.HISTORY)
    val canAccessCustomersAndBakiFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.CUSTOMERS_AND_BAKI)
    val canReceivePaymentFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.RECEIVE_PAYMENT)
    val canAccessSuppliersAndPurchasesFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.SUPPLIERS_AND_PURCHASES)
    val canPaySupplierFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.PAY_SUPPLIER)
    val canViewReportsFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.REPORTS)
    val canAccessCashAndBankFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.CASH_AND_BANK)
    val canAccessExpensesAndIncomeFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.EXPENSES_AND_OTHER_INCOME)
    val canAccessNotesFlow: StateFlow<Boolean> = permissionFlow(StorePermissions.NOTES)

    // Account balances & Overview financials
    val accountBalances: StateFlow<Map<String, Double>> = combine(
        listOf<Flow<Any>>(
            accounts,
            sales,
            payments,
            purchases,
            supplierPayments,
            expenses,
            otherIncomes,
            adjustments
        )
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        CalculationEngine.calculateAccountBalances(
            accounts = arr[0] as List<Account>,
            sales = arr[1] as List<Sale>,
            paymentsIn = arr[2] as List<Payment>,
            purchases = arr[3] as List<Purchase>,
            supplierPayments = arr[4] as List<SupplierPayment>,
            expenses = arr[5] as List<Expense>,
            otherIncomes = arr[6] as List<OtherIncome>,
            adjustments = arr[7] as List<AccountAdjustment>
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val totalBalance: StateFlow<Double> = combine(accounts, accountBalances) { accs, bals ->
        CalculationEngine.calculateTotalBalance(accs, bals)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val financials: StateFlow<StoreFinancials> = combine(products, sales, totalBalance) { prods, sls, totalBal ->
        val base = CalculationEngine.calculateStoreFinancials(prods, sls)
        base.copy(cashBalance = totalBal)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StoreFinancials(0.0, 0.0, 0L, 0.0, 0.0, 0.0))

    val stockSummary: StateFlow<StockSummary> = products.map { prods ->
        CalculationEngine.calculateStockSummary(prods)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StockSummary(0L, 0L, 0L, 0.0))

    val recentSales: StateFlow<List<Sale>> = sales.map { it.take(15) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val lowStockProducts: StateFlow<List<Product>> = products.map { prods ->
        prods.filter { it.stock <= it.lowStockLimit }.sortedBy { it.stock }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val slowSellingProducts: StateFlow<List<Product>> = combine(products, sales) { prods, sls ->
        CalculationEngine.calculateSlowSellingProducts(prods, sls)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bakiOverview: StateFlow<BakiOverview> = combine(customers, sales, payments) { custs, sls, pays ->
        CalculationEngine.calculateBakiOverview(custs, sls, pays)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BakiOverview(0.0, 0L, emptyList()))

    val allCustomerSummaries: StateFlow<List<CustomerBalanceSummary>> = combine(
        customers,
        sales,
        payments
    ) { custs, sls, pays ->
        custs.map { c -> CalculationEngine.calculateCustomerSummary(c, sls, pays) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomer: StateFlow<Customer?> = combine(_selectedCustomerId, customers) { id, custs ->
        if (id.isNullOrBlank()) null else custs.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val selectedCustomerSummary: StateFlow<CustomerBalanceSummary?> = combine(
        selectedCustomer,
        sales,
        payments
    ) { cust, sls, pays ->
        cust?.let { CalculationEngine.calculateCustomerSummary(it, sls, pays) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val selectedCustomerLedger: StateFlow<List<CustomerLedgerEntry>> = combine(
        selectedCustomer,
        sales,
        payments
    ) { cust, sls, pays ->
        cust?.let { CalculationEngine.calculateCustomerLedger(it, sls, pays) } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val homeInsights: StateFlow<HomeInsights> = sales.map { sls ->
        CalculationEngine.calculateHomeInsights(sls)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeInsights(0.0, null, emptyList()))

    val sevenDayCashflow: StateFlow<SevenDayCashflowSummary> = combine(
        listOf<Flow<Any>>(sales, payments, otherIncomes, purchases, supplierPayments, expenses, currentLanguage)
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        CalculationEngine.calculateSevenDayCashflow(
            sales = arr[0] as List<Sale>,
            paymentsIn = arr[1] as List<Payment>,
            otherIncomes = arr[2] as List<OtherIncome>,
            purchases = arr[3] as List<Purchase>,
            supplierPayments = arr[4] as List<SupplierPayment>,
            expenses = arr[5] as List<Expense>,
            lang = arr[6] as AppLanguage
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SevenDayCashflowSummary(emptyList(), 0.0, 0.0))

    // History state
    private val _historyPeriod = MutableStateFlow(HistoryPeriod.TODAY)
    val historyPeriod: StateFlow<HistoryPeriod> = _historyPeriod.asStateFlow()

    private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    private val _historyViewMode = MutableStateFlow(HistoryViewMode.TRANSACTIONS)
    val historyViewMode: StateFlow<HistoryViewMode> = _historyViewMode.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _historyCustomDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val historyCustomDateRange: StateFlow<Pair<Long, Long>?> = _historyCustomDateRange.asStateFlow()

    private val _productsSoldIsGrid = MutableStateFlow(false)
    val productsSoldIsGrid: StateFlow<Boolean> = _productsSoldIsGrid.asStateFlow()

    private val _selectedSaleDetail = MutableStateFlow<Sale?>(null)
    val selectedSaleDetail: StateFlow<Sale?> = _selectedSaleDetail.asStateFlow()

    private val _stockFilterLowOnlyTrigger = MutableStateFlow(false)
    val stockFilterLowOnlyTrigger: StateFlow<Boolean> = _stockFilterLowOnlyTrigger.asStateFlow()

    private val historyFilteredSales: StateFlow<List<Sale>> = combine(
        sales,
        _historyPeriod,
        _historyCustomDateRange
    ) { sls, period, customRange ->
        CalculationEngine.filterSalesByPeriod(sls, period, customRange)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val historyFilteredPurchases: StateFlow<List<Purchase>> = combine(
        purchases,
        _historyPeriod,
        _historyCustomDateRange
    ) { purs, period, customRange ->
        CalculationEngine.filterPurchasesByPeriod(purs, period, customRange)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val historySummary: StateFlow<PeriodFinancialSummary> = combine(
        historyFilteredSales,
        historyFilteredPurchases
    ) { sls, purs ->
        CalculationEngine.calculatePeriodSummary(sls, purs.sumOf { it.totalAmount })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PeriodFinancialSummary(0.0, 0.0, 0.0, 0L, 0.0))

    val historyProductsSold: StateFlow<List<ProductSoldStat>> = combine(
        historyFilteredSales,
        _historySearchQuery
    ) { sls, query ->
        val stats = CalculationEngine.calculateProductsSoldStats(sls)
        val q = query.trim()
        if (q.isBlank()) stats
        else stats.filter { it.productName.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val historyTimelineItems: StateFlow<List<HistoryTimelineItem>> = combine(
        listOf<Flow<Any?>>(
            historyFilteredSales,
            historyFilteredPurchases,
            payments,
            expenses,
            otherIncomes,
            adjustments,
            _historyPeriod,
            _historyCustomDateRange,
            _historyFilter,
            _historySearchQuery
        )
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val sls = arr[0] as List<Sale>
        @Suppress("UNCHECKED_CAST")
        val purs = arr[1] as List<Purchase>
        @Suppress("UNCHECKED_CAST")
        val pays = arr[2] as List<Payment>
        @Suppress("UNCHECKED_CAST")
        val exps = arr[3] as List<Expense>
        @Suppress("UNCHECKED_CAST")
        val incs = arr[4] as List<OtherIncome>
        @Suppress("UNCHECKED_CAST")
        val adjs = arr[5] as List<AccountAdjustment>
        val period = arr[6] as HistoryPeriod
        @Suppress("UNCHECKED_CAST")
        val customRange = arr[7] as Pair<Long, Long>?
        val filter = arr[8] as HistoryFilter
        val query = (arr[9] as String).trim()

        val items = mutableListOf<HistoryTimelineItem>()
        if (filter == HistoryFilter.ALL || filter == HistoryFilter.SALES_ONLY) {
            sls.forEach { items.add(HistoryTimelineItem.SaleItemRow(it)) }
        }
        if (filter == HistoryFilter.ALL || filter == HistoryFilter.PURCHASES_ONLY) {
            purs.forEach { items.add(HistoryTimelineItem.PurchaseItemRow(it)) }
        }
        if (filter == HistoryFilter.ALL) {
            CalculationEngine.filterPaymentsByPeriod(pays, period, customRange)
                .forEach { items.add(HistoryTimelineItem.PaymentItemRow(it)) }
            CalculationEngine.filterExpensesByPeriod(exps, period, customRange)
                .filter { !it.isDeleted }
                .forEach { items.add(HistoryTimelineItem.ExpenseItemRow(it)) }
            CalculationEngine.filterOtherIncomesByPeriod(incs, period, customRange)
                .filter { !it.isDeleted }
                .forEach { items.add(HistoryTimelineItem.OtherIncomeItemRow(it)) }
            CalculationEngine.filterAdjustmentsByPeriod(adjs, period, customRange)
                .forEach { items.add(HistoryTimelineItem.AdjustmentItemRow(it)) }
        }

        val filtered = if (query.isBlank()) {
            items
        } else {
            items.filter { row ->
                when (row) {
                    is HistoryTimelineItem.SaleItemRow ->
                        row.sale.invoiceNo.contains(query, ignoreCase = true) ||
                            row.sale.customerName.contains(query, ignoreCase = true) ||
                            row.sale.items.any { it.name.contains(query, ignoreCase = true) }
                    is HistoryTimelineItem.PurchaseItemRow ->
                        row.purchase.purchaseNo.contains(query, ignoreCase = true) ||
                            row.purchase.supplierName.contains(query, ignoreCase = true) ||
                            row.purchase.items.any { it.name.contains(query, ignoreCase = true) }
                    is HistoryTimelineItem.PaymentItemRow ->
                        row.payment.customerName.contains(query, ignoreCase = true) ||
                            row.payment.paymentNo.contains(query, ignoreCase = true) ||
                            row.payment.note.contains(query, ignoreCase = true)
                    is HistoryTimelineItem.ExpenseItemRow ->
                        row.expense.category.contains(query, ignoreCase = true) ||
                            row.expense.note.contains(query, ignoreCase = true)
                    is HistoryTimelineItem.OtherIncomeItemRow ->
                        row.income.category.contains(query, ignoreCase = true) ||
                            row.income.note.contains(query, ignoreCase = true)
                    is HistoryTimelineItem.AdjustmentItemRow ->
                        row.adjustment.accountName.contains(query, ignoreCase = true) ||
                            row.adjustment.note.contains(query, ignoreCase = true)
                }
            }
        }
        filtered.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private data class SimpleGuardSnapshot(
        val isSimpleMode: Boolean,
        val isFatherMode: Boolean,
        val canSell: Boolean,
        val canViewStock: Boolean,
        val canAccessBaki: Boolean
    )

    // Central tab & modal navigation state (enforced through SimpleModeRouteGuard)
    private val _currentTab = MutableStateFlow(ShopNavTab.HOME)
    val currentTab: StateFlow<ShopNavTab> = _currentTab.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    // Navigation stacks for Business vs More tabs
    private val _activeSubNavTab = MutableStateFlow(ShopNavTab.BUSINESS)
    private val _businessBackStack = MutableStateFlow(listOf(BusinessNavDestination.DASHBOARD))
    private val _moreBackStack = MutableStateFlow(listOf(BusinessNavDestination.DASHBOARD))

    private val _businessNavDestination = MutableStateFlow(BusinessNavDestination.DASHBOARD)
    val businessNavDestination: StateFlow<BusinessNavDestination> = _businessNavDestination.asStateFlow()

    private val _moreNavDestination = MutableStateFlow(BusinessNavDestination.DASHBOARD)
    val moreNavDestination: StateFlow<BusinessNavDestination> = _moreNavDestination.asStateFlow()

    private fun updateBusinessStack(newStack: List<BusinessNavDestination>) {
        val normalized = newStack.ifEmpty { listOf(BusinessNavDestination.DASHBOARD) }
        _businessBackStack.value = normalized
        _businessNavDestination.value = normalized.last()
    }

    private fun updateMoreStack(newStack: List<BusinessNavDestination>) {
        val normalized = newStack.ifEmpty { listOf(BusinessNavDestination.DASHBOARD) }
        _moreBackStack.value = normalized
        _moreNavDestination.value = normalized.last()
    }

    private val _businessSubScreen = MutableStateFlow(BusinessSubScreen.NONE)
    val businessSubScreen: StateFlow<BusinessSubScreen> = _businessSubScreen.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _localSimpleMode,
                currentUserProfile,
                isFatherMode
            ) { local, profile, father ->
                if (father) true else (profile?.isSimpleMode ?: local)
            }.collect { resolvedSimple ->
                _isSimpleMode.value = resolvedSimple
            }
        }
        viewModelScope.launch {
            combine(
                isSimpleMode,
                isFatherMode,
                canSellFlow,
                canViewStockFlow,
                canAccessCustomersAndBakiFlow
            ) { simple, father, canSell, canViewStock, canAccessBaki ->
                SimpleGuardSnapshot(simple, father, canSell, canViewStock, canAccessBaki)
            }.collect { snap ->
                if (snap.isFatherMode && _showSettings.value) {
                    _showSettings.value = false
                }
                if (SimpleModeRouteGuard.isSimpleRestricted(snap.isSimpleMode, snap.isFatherMode)) {
                    updateBusinessStack(
                        SimpleModeRouteGuard.sanitizeBackStack(
                            _businessBackStack.value,
                            snap.isSimpleMode,
                            snap.isFatherMode
                        )
                    )
                    updateMoreStack(
                        SimpleModeRouteGuard.sanitizeBackStack(
                            _moreBackStack.value,
                            snap.isSimpleMode,
                            snap.isFatherMode
                        )
                    )
                    if (_showSettings.value) {
                        _showSettings.value = false
                    }
                    val resolved = SimpleModeRouteGuard.resolveTab(
                        requested = _currentTab.value,
                        isSimpleMode = snap.isSimpleMode,
                        isFatherMode = snap.isFatherMode,
                        canSell = snap.canSell,
                        canViewStock = snap.canViewStock,
                        canAccessBaki = snap.canAccessBaki
                    )
                    if (_currentTab.value != resolved) {
                        _businessSubScreen.value = BusinessSubScreen.NONE
                        _selectedCustomerId.value = null
                        _currentTab.value = resolved
                    }
                }
            }
        }
    }

    // Business analytics period & state
    private val _businessPeriod = MutableStateFlow(HistoryPeriod.MONTH)
    val businessPeriod: StateFlow<HistoryPeriod> = _businessPeriod.asStateFlow()

    private val _businessCustomDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val businessCustomDateRange: StateFlow<Pair<Long, Long>?> = _businessCustomDateRange.asStateFlow()

    val businessFilteredSales: StateFlow<List<Sale>> = combine(
        sales,
        _businessPeriod,
        _businessCustomDateRange
    ) { sls, period, custom ->
        CalculationEngine.filterSalesByPeriod(sls, period, custom)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val businessFilteredPurchases: StateFlow<List<Purchase>> = combine(
        purchases,
        _businessPeriod,
        _businessCustomDateRange
    ) { purs, period, custom ->
        CalculationEngine.filterPurchasesByPeriod(purs, period, custom)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val businessFilteredReturns: StateFlow<List<PurchaseReturn>> = combine(
        purchaseReturns,
        _businessPeriod,
        _businessCustomDateRange
    ) { rets, period, custom ->
        CalculationEngine.filterReturnsByPeriod(rets, period, custom)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val businessFilteredExpenses: StateFlow<List<Expense>> = combine(
        expenses,
        _businessPeriod,
        _businessCustomDateRange
    ) { exps, period, custom ->
        CalculationEngine.filterExpensesByPeriod(exps, period, custom).filter { !it.isDeleted }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val businessFilteredOtherIncomes: StateFlow<List<OtherIncome>> = combine(
        otherIncomes,
        _businessPeriod,
        _businessCustomDateRange
    ) { incs, period, custom ->
        CalculationEngine.filterOtherIncomesByPeriod(incs, period, custom).filter { !it.isDeleted }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val businessFinancials: StateFlow<PeriodFinancialSummary> = combine(
        businessFilteredSales,
        businessFilteredPurchases
    ) { sls, purs ->
        CalculationEngine.calculatePeriodSummary(sls, purs.sumOf { it.totalAmount })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PeriodFinancialSummary(0.0, 0.0, 0.0, 0L, 0.0))

    val moneyNow: StateFlow<MoneyNowSummary> = combine(
        listOf<Flow<Any>>(products, customers, suppliers, sales, payments, totalBalance)
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        CalculationEngine.calculateMoneyNow(
            products = arr[0] as List<Product>,
            customers = arr[1] as List<Customer>,
            suppliers = arr[2] as List<Supplier>,
            allSales = arr[3] as List<Sale>,
            allPayments = arr[4] as List<Payment>,
            totalCashAndBankBalance = arr[5] as Double
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MoneyNowSummary(0.0, 0.0, 0.0, 0L, 0L, 0.0))

    val salesChartPoints: StateFlow<List<DailySalesPoint>> = combine(
        businessFilteredSales,
        _businessPeriod,
        _businessCustomDateRange,
        currentLanguage
    ) { sls, period, custom, lang ->
        CalculationEngine.calculateDailySalesChart(sls, period, custom, lang)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val topSellingProducts: StateFlow<List<ProductSoldStat>> = businessFilteredSales.map { sls ->
        CalculationEngine.calculateTopSellingProducts(sls, limit = 5)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val salesByCategory: StateFlow<List<CategorySalesStat>> = combine(
        businessFilteredSales,
        products
    ) { sls, prods ->
        CalculationEngine.calculateSalesByCategory(sls, prods)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dayBookEntries: StateFlow<List<DayBookEntry>> = combine(
        listOf<Flow<Any?>>(
            businessFilteredSales,
            businessFilteredPurchases,
            payments,
            businessFilteredReturns,
            supplierPayments,
            businessFilteredExpenses,
            businessFilteredOtherIncomes,
            adjustments,
            _businessPeriod,
            _businessCustomDateRange
        )
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val period = arr[8] as HistoryPeriod
        @Suppress("UNCHECKED_CAST")
        val custom = arr[9] as Pair<Long, Long>?
        CalculationEngine.calculateDayBook(
            sales = arr[0] as List<Sale>,
            purchases = arr[1] as List<Purchase>,
            paymentsIn = CalculationEngine.filterPaymentsByPeriod(arr[2] as List<Payment>, period, custom),
            returns = arr[3] as List<PurchaseReturn>,
            supplierPayments = CalculationEngine.filterSupplierPaymentsByPeriod(arr[4] as List<SupplierPayment>, period, custom),
            expenses = arr[5] as List<Expense>,
            otherIncomes = arr[6] as List<OtherIncome>,
            adjustments = CalculationEngine.filterAdjustmentsByPeriod(arr[7] as List<AccountAdjustment>, period, custom)
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isPnlStockBased = MutableStateFlow(true)
    val isPnlStockBased: StateFlow<Boolean> = _isPnlStockBased.asStateFlow()

    val pnlStatement: StateFlow<ProfitAndLossStatement> = combine(
        listOf<Flow<Any>>(
            businessFilteredSales,
            businessFilteredPurchases,
            businessFilteredReturns,
            businessFilteredExpenses,
            businessFilteredOtherIncomes,
            _isPnlStockBased
        )
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        CalculationEngine.calculateProfitAndLoss(
            sales = arr[0] as List<Sale>,
            purchases = arr[1] as List<Purchase>,
            returns = arr[2] as List<PurchaseReturn>,
            expenses = arr[3] as List<Expense>,
            otherIncomes = arr[4] as List<OtherIncome>,
            isStockBased = arr[5] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfitAndLossStatement(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, true, emptyMap()))

    // Statements & Details
    private val _selectedStatementCustomerId = MutableStateFlow<String?>(null)
    val selectedStatementCustomer: StateFlow<Customer?> = combine(
        _selectedStatementCustomerId,
        customers
    ) { id, list ->
        if (id.isNullOrBlank()) null else list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val customerStatementReport: StateFlow<CustomerStatementReport?> = combine(
        selectedStatementCustomer,
        sales,
        payments,
        _businessPeriod,
        _businessCustomDateRange
    ) { cust, sls, pays, period, custom ->
        cust?.let {
            CalculationEngine.calculateCustomerStatement(it, sls, pays, period, custom)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _selectedStatementSupplierId = MutableStateFlow<String?>(null)
    val selectedStatementSupplier: StateFlow<Supplier?> = combine(
        _selectedStatementSupplierId,
        suppliers
    ) { id, list ->
        if (id.isNullOrBlank()) null else list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val supplierStatementReport: StateFlow<SupplierStatementReport?> = combine(
        selectedStatementSupplier,
        purchases,
        purchaseReturns,
        _businessPeriod,
        _businessCustomDateRange
    ) { sup, purs, rets, period, custom ->
        sup?.let {
            CalculationEngine.calculateSupplierStatement(it, purs, rets, period, custom)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccount: StateFlow<Account?> = combine(_selectedAccountId, accounts) { id, list ->
        if (id.isNullOrBlank()) null else list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val selectedAccountStatement: StateFlow<AccountStatementReport?> = combine(
        listOf<Flow<Any?>>(
            selectedAccount,
            sales,
            payments,
            purchases,
            supplierPayments,
            expenses,
            otherIncomes,
            adjustments,
            _historyPeriod,
            _historyCustomDateRange
        )
    ) { arr ->
        val acc = arr[0] as Account? ?: return@combine null
        @Suppress("UNCHECKED_CAST")
        CalculationEngine.calculateAccountStatement(
            account = acc,
            allSales = arr[1] as List<Sale>,
            allPaymentsIn = arr[2] as List<Payment>,
            allPurchases = arr[3] as List<Purchase>,
            allSupplierPayments = arr[4] as List<SupplierPayment>,
            allExpenses = arr[5] as List<Expense>,
            allOtherIncomes = arr[6] as List<OtherIncome>,
            allAdjustments = arr[7] as List<AccountAdjustment>,
            period = arr[8] as HistoryPeriod,
            customRange = arr[9] as Pair<Long, Long>?
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Staff selection
    private val _selectedStaffMemberId = MutableStateFlow<String?>(null)
    val selectedStaffMember: StateFlow<StoreMember?> = combine(_selectedStaffMemberId, members) { id, list ->
        if (id.isNullOrBlank()) null else list.firstOrNull { it.userId == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val staffReport: StateFlow<StaffReportSummary> = combine(
        listOf<Flow<Any?>>(
            members,
            store,
            sales,
            payments,
            _businessPeriod,
            _businessCustomDateRange,
            currentLanguage
        )
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val mems = arr[0] as List<StoreMember>
        val st = arr[1] as Store?
        @Suppress("UNCHECKED_CAST")
        val sls = arr[2] as List<Sale>
        @Suppress("UNCHECKED_CAST")
        val pays = arr[3] as List<Payment>
        val period = arr[4] as HistoryPeriod
        @Suppress("UNCHECKED_CAST")
        val custom = arr[5] as Pair<Long, Long>?
        val lang = arr[6] as AppLanguage
        val ownerUid = st?.ownerId?.ifBlank { currentUserId } ?: currentUserId
        CalculationEngine.calculateStaffReport(mems, ownerUid, sls, pays, period, custom, lang)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StaffReportSummary(emptyList(), 0L, 0.0, 0.0, 0.0, 0.0))

    // Monthly Report
    private val initialCal = Calendar.getInstance()
    private val _monthlyReportYear = MutableStateFlow(initialCal.get(Calendar.YEAR))
    val monthlyReportYear: StateFlow<Int> = _monthlyReportYear.asStateFlow()

    private val _monthlyReportMonth = MutableStateFlow(initialCal.get(Calendar.MONTH))
    val monthlyReportMonth: StateFlow<Int> = _monthlyReportMonth.asStateFlow()

    private val _isMonthlyReportLoading = MutableStateFlow(false)
    val isMonthlyReportLoading: StateFlow<Boolean> = _isMonthlyReportLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthlyRangeInputsRemote: Flow<StoreRepository.MonthlyReportInputs?> = combine(
        _monthlyReportYear,
        _monthlyReportMonth
    ) { y, m -> y to m }.flatMapLatest { (y, m) ->
        flow {
            val nowCal = Calendar.getInstance()
            val isCurrentMonth = (y == nowCal.get(Calendar.YEAR) && m == nowCal.get(Calendar.MONTH))
            if (storeId == "demo_store" || isCurrentMonth) {
                emit(null)
            } else {
                _isMonthlyReportLoading.value = true
                val loaded = try {
                    repository.loadMonthlyReportInputs(storeId, y, m)
                } finally {
                    _isMonthlyReportLoading.value = false
                }
                emit(loaded)
            }
        }
    }

    val monthlyReportData: StateFlow<MonthlyReportData> = combine(
        listOf<Flow<Any?>>(
            _monthlyReportYear,
            _monthlyReportMonth,
            monthlyRangeInputsRemote,
            sales,
            products,
            customers,
            suppliers,
            payments,
            currentLanguage,
            purchases,
            purchaseReturns,
            supplierPayments,
            expenses,
            otherIncomes,
            accounts,
            adjustments,
            isSimpleMode,
            isFatherMode
        )
    ) { arr ->
        val y = arr[0] as Int
        val m = arr[1] as Int
        val remoteInputs = arr[2] as StoreRepository.MonthlyReportInputs?
        @Suppress("UNCHECKED_CAST")
        val allSales = arr[3] as List<Sale>
        @Suppress("UNCHECKED_CAST")
        val prods = arr[4] as List<Product>
        @Suppress("UNCHECKED_CAST")
        val custs = arr[5] as List<Customer>
        @Suppress("UNCHECKED_CAST")
        val sups = arr[6] as List<Supplier>
        @Suppress("UNCHECKED_CAST")
        val pays = arr[7] as List<Payment>
        val rawLang = arr[8] as AppLanguage
        @Suppress("UNCHECKED_CAST")
        val purs = arr[9] as List<Purchase>
        @Suppress("UNCHECKED_CAST")
        val rets = arr[10] as List<PurchaseReturn>
        @Suppress("UNCHECKED_CAST")
        val supPays = arr[11] as List<SupplierPayment>
        @Suppress("UNCHECKED_CAST")
        val exps = arr[12] as List<Expense>
        @Suppress("UNCHECKED_CAST")
        val incs = arr[13] as List<OtherIncome>
        @Suppress("UNCHECKED_CAST")
        val accs = arr[14] as List<Account>
        @Suppress("UNCHECKED_CAST")
        val adjs = arr[15] as List<AccountAdjustment>
        val simpleOn = arr[16] as Boolean
        val fatherOn = arr[17] as Boolean
        val effectiveLang = if (simpleOn || fatherOn) AppLanguage.BN else rawLang

        val inputs = if (remoteInputs != null && remoteInputs.year == y && remoteInputs.month == m) {
            remoteInputs
        } else {
            repository.filterMonthlyInputsByDateRange(
                year = y,
                month = m,
                allSales = allSales,
                allPurchases = purs,
                allPurchaseReturns = rets,
                allPayments = pays,
                allSupplierPayments = supPays,
                allExpenses = exps,
                allOtherIncomes = incs,
                allAdjustments = adjs
            )
        }

        CalculationEngine.calculateMonthlyReportData(
            year = y,
            month = m,
            allSales = inputs.sales,
            products = prods,
            customers = custs,
            suppliers = sups,
            allPayments = inputs.payments,
            lang = effectiveLang,
            allPurchases = inputs.purchases,
            allPurchaseReturns = inputs.purchaseReturns,
            allSupplierPayments = inputs.supplierPayments,
            allExpenses = inputs.expenses,
            allOtherIncomes = inputs.otherIncomes,
            accounts = accs,
            allAdjustments = inputs.adjustments
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            MonthlyReportData(
                year = initialCal.get(Calendar.YEAR),
                month = initialCal.get(Calendar.MONTH),
                monthLabel = "",
                totalSales = 0.0,
                totalProfit = 0.0,
                totalOrders = 0L,
                totalItemsSold = 0.0,
                stockValueAtCost = 0.0,
                bakiCustomersOwe = 0.0,
                debtToSuppliers = 0.0,
                customerStats = emptyList(),
                itemsSold = emptyList()
            )
        )

    // Data Health Check
    private val _healthProblems = MutableStateFlow<List<HealthProblem>>(emptyList())
    val healthProblems: StateFlow<List<HealthProblem>> = _healthProblems.asStateFlow()

    private val _isScanningHealth = MutableStateFlow(false)
    val isScanningHealth: StateFlow<Boolean> = _isScanningHealth.asStateFlow()

    private val _lastHealthScanMillis = MutableStateFlow(0L)
    val lastHealthScanMillis: StateFlow<Long> = _lastHealthScanMillis.asStateFlow()

    // Business Strategy & AI Key
    private val savedBusinessStrategy: StateFlow<SavedBusinessStrategy?> =
        repository.observeSavedBusinessStrategy(storeId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val businessStrategy: StateFlow<SavedBusinessStrategy> = combine(
        listOf<Flow<Any?>>(savedBusinessStrategy, products, sales, customers, payments)
    ) { arr ->
        val saved = arr[0] as SavedBusinessStrategy?
        val todayKey = currentDateKey()
        if (saved != null && saved.diagnosis.isNotBlank() && saved.dateKey == todayKey) {
            saved
        } else {
            @Suppress("UNCHECKED_CAST")
            val facts = BusinessStrategyEngine.calculateBusinessStrategyFacts(
                products = arr[1] as List<Product>,
                sales = arr[2] as List<Sale>,
                customers = arr[3] as List<Customer>,
                payments = arr[4] as List<Payment>
            )
            BusinessStrategyBanglaBuilder.buildFixedStrategy(
                storeId = storeId,
                facts = facts,
                refreshCountToday = if (saved?.dateKey == todayKey) saved.refreshCountToday else 0,
                updatedBy = currentUserId
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SavedBusinessStrategy())

    private val _isStrategyRefreshing = MutableStateFlow(false)
    val isStrategyRefreshing: StateFlow<Boolean> = _isStrategyRefreshing.asStateFlow()

    val strategyRefreshesRemainingToday: StateFlow<Int> = savedBusinessStrategy.map { saved ->
        val max = BusinessStrategyDefaults.THRESHOLDS.maxAiRefreshesPerDay
        val used = if (saved?.dateKey == currentDateKey()) saved.refreshCountToday else 0
        (max - used).coerceAtLeast(0)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        BusinessStrategyDefaults.THRESHOLDS.maxAiRefreshesPerDay
    )

    val aiKeySummary: StateFlow<AiKeyUiSummary> = combine(isOwner, isFatherMode) { owner, father ->
        owner to father
    }.flatMapLatest { (owner, father) ->
        repository.observeAiKeySummary(storeId, owner, father)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AiKeyUiSummary())

    val businessCardProfileSeed: StateFlow<BusinessCardProfileSeed> = combine(
        store,
        currentMember
    ) { st, mem ->
        val ctx = repository.appContext
        val savedLogo = if (ctx != null) {
            BusinessCardLocalStore.loadSavedCards(ctx)
                .firstOrNull { it.draft.logoUri.isNotBlank() }
                ?.draft?.logoUri
                .orEmpty()
        } else ""
        BusinessCardProfileSeed(
            shopName = st?.name.orEmpty(),
            ownerName = mem?.displayName.orEmpty(),
            phone = st?.phone.orEmpty(),
            address = st?.address.orEmpty(),
            email = mem?.email.orEmpty(),
            logoUri = savedLogo
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BusinessCardProfileSeed())

    // Simple utilities & preferences
    fun togglePrivacyHidden() {
        _isPrivacyHidden.value = !_isPrivacyHidden.value
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setLanguage(lang: AppLanguage) {
        repository.setLanguage(lang)
    }

    fun toggleLanguage() {
        repository.toggleLanguage()
    }

    fun setSimpleMode(enabled: Boolean) {
        val targetEnabled = if (isFatherMode.value) true else enabled
        _localSimpleMode.value = targetEnabled
        _isSimpleMode.value = targetEnabled
        if (targetEnabled && !isFatherMode.value) {
            forceSimpleHome()
        }
        viewModelScope.launch {
            runCatching { repository.updateUserSimpleMode(currentUserId, targetEnabled) }
        }
    }

    fun setShowProfitInSimpleMode(enabled: Boolean) {
        stockPrefs?.setShowProfitInSimpleMode(enabled)
    }

    fun setPhotoStorageMode(mode: PhotoStorageMode) {
        stockPrefs?.setPhotoStorageMode(mode)
    }

    fun setReceiptImageStyle(style: ReceiptImageStyle) {
        stockPrefs?.setReceiptImageStyle(style)
    }

    fun setLastUsedAccountId(accountId: String) {
        stockPrefs?.setLastUsedAccountId(accountId)
    }

    fun setOwnerPin(pin: String, onDone: () -> Unit = {}) {
        try {
            stockPrefs?.setOwnerPin(pin)
            onDone()
        } catch (e: Exception) {
            _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                e,
                currentLanguage.value,
                fallbackEn = "Invalid PIN. Please enter a 4-6 digit PIN.",
                fallbackBn = "সঠিক ৪ থেকে ৬ সংখ্যার পিন দিন।"
            )
        }
    }

    fun setOwnerPinAndExitSimpleMode(pin: String, onDone: () -> Unit = {}) {
        setOwnerPin(pin) {
            setSimpleMode(false)
            onDone()
        }
    }

    fun verifyOwnerPin(pin: String, onResult: (Boolean) -> Unit) {
        onResult(stockPrefs?.verifyOwnerPin(pin) ?: false)
    }

    fun getRemainingPinLockoutSeconds(): Int =
        stockPrefs?.getRemainingPinLockoutSeconds() ?: 0

    fun verifyOwnerPinWithLockout(pin: String): StockPreferences.PinVerifyOutcome =
        stockPrefs?.verifyOwnerPinWithLockout(pin)
            ?: StockPreferences.PinVerifyOutcome(
                success = false,
                isLocked = false,
                remainingLockSeconds = 0,
                failedAttempts = 0
            )

    fun verifyOwnerPinAndExitSimpleMode(pin: String): StockPreferences.PinVerifyOutcome {
        val outcome = verifyOwnerPinWithLockout(pin)
        if (outcome.success) {
            setSimpleMode(false)
        }
        return outcome
    }

    fun changeOwnerPin(oldPin: String, newPin: String, onResult: (Boolean) -> Unit) {
        onResult(stockPrefs?.changeOwnerPin(oldPin, newPin) ?: false)
    }

    // Central Route Guard & Navigation helpers
    private fun forceSimpleHome() {
        updateBusinessStack(listOf(BusinessNavDestination.DASHBOARD))
        updateMoreStack(listOf(BusinessNavDestination.DASHBOARD))
        _businessSubScreen.value = BusinessSubScreen.NONE
        _selectedCustomerId.value = null
        _showSettings.value = false
        _currentTab.value = ShopNavTab.HOME
    }

    fun navigateToTab(tab: ShopNavTab) {
        if (SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode.value, isFatherMode.value)) {
            val resolved = SimpleModeRouteGuard.resolveTab(
                requested = tab,
                isSimpleMode = true,
                isFatherMode = false,
                canSell = canSellFlow.value,
                canViewStock = canViewStockFlow.value,
                canAccessBaki = canAccessCustomersAndBakiFlow.value
            )
            if (resolved != tab) {
                forceSimpleHome()
                return
            }
            updateBusinessStack(listOf(BusinessNavDestination.DASHBOARD))
            updateMoreStack(listOf(BusinessNavDestination.DASHBOARD))
            _showSettings.value = false
            _currentTab.value = resolved
            return
        }
        if (tab == _currentTab.value) {
            when (tab) {
                ShopNavTab.MORE -> resetMoreToRoot()
                ShopNavTab.BUSINESS -> resetBusinessToRoot()
                else -> {}
            }
        }
        setActiveSubNavTab(tab)
        _currentTab.value = tab
    }

    fun openSubDestination(targetTab: ShopNavTab, dest: BusinessNavDestination) {
        if (SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode.value, isFatherMode.value)) {
            if (!SimpleModeRouteGuard.isDestinationAllowedInSimpleMode(dest)) {
                forceSimpleHome()
                return
            }
        }
        if (isDemoMode && isDemoRestrictedDestination(dest)) {
            return
        }
        if (targetTab == ShopNavTab.MORE) {
            resetMoreToRoot()
        } else {
            resetBusinessToRoot()
        }
        setActiveSubNavTab(targetTab)
        navigateInBusiness(dest)
        _currentTab.value = targetTab
    }

    fun openSettings() {
        if (isFatherMode.value || SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode.value, isFatherMode.value)) {
            forceSimpleHome()
            return
        }
        _showSettings.value = true
    }

    fun closeSettings() {
        _showSettings.value = false
    }

    fun openLowStockFromAlert() {
        if (SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode.value, isFatherMode.value)) {
            if (canViewStockFlow.value) {
                setStockFilterLowOnlyTrigger(true)
                navigateToTab(ShopNavTab.STOCK)
            } else {
                forceSimpleHome()
            }
        } else if (isFatherMode.value) {
            openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.LOW_STOCK_SCREEN)
        } else if (canViewStockFlow.value && canViewReportsFlow.value) {
            openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.LOW_STOCK_SCREEN)
        } else if (canViewStockFlow.value) {
            navigateToTab(ShopNavTab.STOCK)
        }
    }

    fun openBakiFromAlert() {
        if (SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode.value, isFatherMode.value)) {
            if (canAccessCustomersAndBakiFlow.value) {
                closeCustomerDetails()
                navigateToTab(ShopNavTab.BAKI)
            } else {
                forceSimpleHome()
            }
        } else if (isFatherMode.value) {
            openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.BAKI_SCREEN)
        } else if (canAccessCustomersAndBakiFlow.value) {
            val tab = if (canViewReportsFlow.value) ShopNavTab.BUSINESS else ShopNavTab.MORE
            openSubDestination(tab, BusinessNavDestination.BAKI_SCREEN)
        }
    }

    fun handleBackNavigation(): Boolean {
        if (_showSettings.value) {
            _showSettings.value = false
            if (SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode.value, isFatherMode.value)) {
                forceSimpleHome()
            }
            return true
        }
        if (SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode.value, isFatherMode.value)) {
            if (_businessSubScreen.value != BusinessSubScreen.NONE) {
                closeCustomerDetails()
                return true
            }
            if (_currentTab.value != ShopNavTab.HOME ||
                _businessBackStack.value.size > 1 ||
                _moreBackStack.value.size > 1 ||
                _businessBackStack.value.lastOrNull() != BusinessNavDestination.DASHBOARD ||
                _moreBackStack.value.lastOrNull() != BusinessNavDestination.DASHBOARD
            ) {
                forceSimpleHome()
                return true
            }
            return false
        }
        if (_currentTab.value == ShopNavTab.MORE &&
            _moreBackStack.value.size <= 1 &&
            _businessSubScreen.value == BusinessSubScreen.NONE
        ) {
            _currentTab.value = ShopNavTab.HOME
            return true
        }
        return false
    }

    fun handleAppIntent(intent: Intent?) {
        val action = SimpleModeRouteGuard.parseAppIntent(intent) ?: return
        val simpleRestricted = SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode.value, isFatherMode.value)
        when (action) {
            is SimpleModeRouteGuard.GuardedIntentAction.Tab -> {
                if (simpleRestricted && action.tab == ShopNavTab.STOCK && action.lowStockFilter) {
                    openLowStockFromAlert()
                } else {
                    navigateToTab(action.tab)
                    if (action.lowStockFilter && _currentTab.value == ShopNavTab.STOCK) {
                        setStockFilterLowOnlyTrigger(true)
                    }
                }
            }
            is SimpleModeRouteGuard.GuardedIntentAction.Destination -> {
                if (simpleRestricted) {
                    forceSimpleHome()
                } else if (isDemoMode && isDemoRestrictedDestination(action.destination)) {
                    resetBusinessToRoot()
                    resetMoreToRoot()
                    _currentTab.value = ShopNavTab.HOME
                } else if (isFatherMode.value) {
                    if (action.destination == BusinessNavDestination.BAKI_SCREEN ||
                        action.destination == BusinessNavDestination.LOW_STOCK_SCREEN ||
                        action.destination == BusinessNavDestination.MONTHLY_REPORT
                    ) {
                        openSubDestination(ShopNavTab.BUSINESS, action.destination)
                    } else {
                        _currentTab.value = ShopNavTab.HOME
                    }
                } else {
                    val targetTab = if (canViewReportsFlow.value) ShopNavTab.BUSINESS else ShopNavTab.MORE
                    openSubDestination(targetTab, action.destination)
                }
            }
            is SimpleModeRouteGuard.GuardedIntentAction.Modal -> {
                if (simpleRestricted &&
                    !SimpleModeRouteGuard.isModalAllowed(action.modal, isSimpleMode = true, isFatherMode = false)
                ) {
                    forceSimpleHome()
                } else if (isDemoMode && action.modal == SimpleModeRouteGuard.GuardedModal.AI_KEY) {
                    return
                } else if (action.modal == SimpleModeRouteGuard.GuardedModal.SETTINGS ||
                    action.modal == SimpleModeRouteGuard.GuardedModal.AI_KEY ||
                    action.modal == SimpleModeRouteGuard.GuardedModal.BACKUP
                ) {
                    openSettings()
                }
            }
        }
    }

    private fun isDemoRestrictedDestination(dest: BusinessNavDestination): Boolean {
        return dest == BusinessNavDestination.STAFF_MANAGEMENT_SCREEN ||
            dest == BusinessNavDestination.STAFF_MEMBER_DETAILS_SCREEN ||
            dest == BusinessNavDestination.REPORT_STAFF
    }

    fun setActiveSubNavTab(tab: ShopNavTab) {
        if (SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode.value, isFatherMode.value)) {
            if (tab == ShopNavTab.BUSINESS || tab == ShopNavTab.MORE || tab == ShopNavTab.HISTORY) {
                forceSimpleHome()
            }
            return
        }
        if (tab == ShopNavTab.BUSINESS || tab == ShopNavTab.MORE) {
            _activeSubNavTab.value = tab
        }
    }

    fun navigateInBusiness(dest: BusinessNavDestination) {
        if (SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode.value, isFatherMode.value)) {
            if (!SimpleModeRouteGuard.isDestinationAllowedInSimpleMode(dest)) {
                forceSimpleHome()
                return
            }
        }
        if (isDemoMode && isDemoRestrictedDestination(dest)) {
            return
        }
        val isMore = _activeSubNavTab.value == ShopNavTab.MORE
        val current = if (isMore) _moreBackStack.value else _businessBackStack.value
        if (current.lastOrNull() != dest) {
            if (isMore) updateMoreStack(current + dest) else updateBusinessStack(current + dest)
        }
        _businessSubScreen.value = BusinessSubScreen.NONE
    }

    fun navigateBackInBusiness() {
        if (SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode.value, isFatherMode.value)) {
            if (_businessSubScreen.value != BusinessSubScreen.NONE) {
                _businessSubScreen.value = BusinessSubScreen.NONE
                _selectedCustomerId.value = null
                return
            }
            forceSimpleHome()
            return
        }
        if (_businessSubScreen.value != BusinessSubScreen.NONE) {
            _businessSubScreen.value = BusinessSubScreen.NONE
            _selectedCustomerId.value = null
            return
        }
        val isMore = _activeSubNavTab.value == ShopNavTab.MORE
        val current = if (isMore) _moreBackStack.value else _businessBackStack.value
        val next = if (current.size > 1) current.dropLast(1) else listOf(BusinessNavDestination.DASHBOARD)
        if (isMore) updateMoreStack(next) else updateBusinessStack(next)
    }

    fun resetBusinessToRoot() {
        updateBusinessStack(listOf(BusinessNavDestination.DASHBOARD))
        _businessSubScreen.value = BusinessSubScreen.NONE
        _selectedCustomerId.value = null
    }

    fun resetMoreToRoot() {
        updateMoreStack(listOf(BusinessNavDestination.DASHBOARD))
        _businessSubScreen.value = BusinessSubScreen.NONE
        _selectedCustomerId.value = null
    }

    fun openCustomerDetails(customer: Customer) {
        _selectedCustomerId.value = customer.id
        _businessSubScreen.value = BusinessSubScreen.CUSTOMER_DETAILS
    }

    fun closeCustomerDetails() {
        _selectedCustomerId.value = null
        _businessSubScreen.value = BusinessSubScreen.NONE
    }

    // Period & filters
    fun setHistoryPeriod(period: HistoryPeriod) {
        _historyPeriod.value = period
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    fun setHistoryViewMode(mode: HistoryViewMode) {
        _historyViewMode.value = mode
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun setHistoryCustomDateRange(startMillis: Long, endMillis: Long) {
        _historyCustomDateRange.value = minOf(startMillis, endMillis) to maxOf(startMillis, endMillis)
        _historyPeriod.value = HistoryPeriod.CUSTOM
    }

    fun toggleProductsSoldGrid() {
        _productsSoldIsGrid.value = !_productsSoldIsGrid.value
    }

    fun selectSaleDetail(sale: Sale?) {
        _selectedSaleDetail.value = sale
    }

    fun setStockFilterLowOnlyTrigger(enabled: Boolean) {
        _stockFilterLowOnlyTrigger.value = enabled
    }

    fun setBusinessPeriod(period: HistoryPeriod) {
        _businessPeriod.value = period
    }

    fun setBusinessCustomDateRange(startMillis: Long, endMillis: Long) {
        _businessCustomDateRange.value = minOf(startMillis, endMillis) to maxOf(startMillis, endMillis)
        _businessPeriod.value = HistoryPeriod.CUSTOM
    }

    fun togglePnlStockBased() {
        _isPnlStockBased.value = !_isPnlStockBased.value
    }

    fun selectStatementCustomer(customer: Customer?) {
        _selectedStatementCustomerId.value = customer?.id
    }

    fun selectStatementSupplier(supplier: Supplier?) {
        _selectedStatementSupplierId.value = supplier?.id
    }

    fun selectAccount(account: Account?) {
        _selectedAccountId.value = account?.id
    }

    fun selectStaffMember(member: StoreMember?) {
        _selectedStaffMemberId.value = member?.userId
    }

    fun setMonthlyReportMonth(year: Int, month: Int) {
        _monthlyReportYear.value = year
        _monthlyReportMonth.value = month
    }

    // Store profile & Shop Type
    fun updateStoreShopType(
        newType: ShopType,
        addSuggestedCategories: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        launchBusy(onSuccess) {
            repository.updateStoreShopType(
                storeId = storeId,
                shopType = newType,
                addSuggestedCategories = addSuggestedCategories,
                existingCategoriesFallback = store.value?.productCategories.orEmpty()
            )
            _statusMessage.value = ShopLocale.get("shop_type_updated_msg", currentLanguage.value)
        }
    }

    // Products & Stock
    fun addNewProductWizard(
        name: String,
        nameBn: String = "",
        category: String = "General",
        unit: String = "pcs",
        buyPrice: Double = 0.0,
        sellPrice: Double = 0.0,
        stock: Double = 0.0,
        lowStockLimit: Double = 5.0,
        photoUri: String = "",
        barcode: String = "",
        wholesalePrice: Double = 0.0,
        mrp: Double = 0.0,
        itemLocation: String = "",
        partyRate: Double = 0.0,
        buyUnit: String = "",
        buyConversion: Double = 1.0,
        sellUnits: Map<String, Double> = emptyMap(),
        onSuccess: () -> Unit = {}
    ) {
        val product = Product(
            storeId = storeId,
            name = name.trim(),
            nameBn = nameBn.trim(),
            barcode = barcode.trim(),
            category = category.trim().ifBlank { "General" },
            unit = unit.trim().ifBlank { "pcs" },
            buyUnit = buyUnit.trim(),
            buyConversion = buyConversion,
            sellUnits = sellUnits,
            buyPrice = buyPrice,
            sellPrice = sellPrice,
            wholesalePrice = wholesalePrice,
            mrp = mrp,
            partyRate = partyRate,
            itemLocation = itemLocation.trim(),
            stock = stock,
            lowStockLimit = lowStockLimit,
            photoUri = photoUri
        )
        launchBusy(onSuccess) {
            repository.addProduct(storeId, product, photoStorageMode.value)
        }
    }

    fun updateProductWizard(
        productId: String,
        name: String,
        nameBn: String = "",
        category: String = "General",
        unit: String = "pcs",
        buyPrice: Double = 0.0,
        sellPrice: Double = 0.0,
        stock: Double = 0.0,
        lowStockLimit: Double = 5.0,
        photoUri: String = "",
        barcode: String = "",
        wholesalePrice: Double = 0.0,
        mrp: Double = 0.0,
        itemLocation: String = "",
        partyRate: Double = 0.0,
        buyUnit: String = "",
        buyConversion: Double = 1.0,
        sellUnits: Map<String, Double> = emptyMap(),
        onSuccess: () -> Unit = {}
    ) {
        val existing = products.value.firstOrNull { it.id == productId }
        val updated = (existing ?: Product(id = productId, storeId = storeId)).copy(
            id = productId,
            storeId = storeId,
            name = name.trim(),
            nameBn = nameBn.trim(),
            barcode = barcode.trim(),
            category = category.trim().ifBlank { "General" },
            unit = unit.trim().ifBlank { "pcs" },
            buyUnit = buyUnit.trim(),
            buyConversion = buyConversion,
            sellUnits = sellUnits,
            buyPrice = buyPrice,
            sellPrice = sellPrice,
            wholesalePrice = wholesalePrice,
            mrp = mrp,
            partyRate = partyRate,
            itemLocation = itemLocation.trim(),
            stock = stock,
            lowStockLimit = lowStockLimit,
            photoUri = photoUri
        )
        launchBusy(onSuccess) {
            repository.updateProduct(
                storeId = storeId,
                product = updated,
                previousPhotoUri = existing?.photoUri.orEmpty(),
                photoStorageMode = photoStorageMode.value
            )
        }
    }

    fun addQuickStock(productId: String, additionalStock: Double, onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.addStock(storeId, productId, additionalStock)
            _statusMessage.value = ShopLocale.format("stock_added_msg", currentLanguage.value, ShopLocale.formatNumber(additionalStock, currentLanguage.value))
        }
    }

    fun deleteProduct(productId: String, onSuccess: () -> Unit = {}) {
        val product = products.value.firstOrNull { it.id == productId }
        launchBusy(onSuccess) {
            if (product != null) {
                repository.moveProductToBin(storeId, product, currentUserId)
            } else {
                repository.deleteProduct(storeId, productId)
            }
        }
    }

    fun restoreProductFromBin(productId: String, onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.restoreProductFromBin(storeId, productId)
        }
    }

    fun emptyProductBin(onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.emptyProductBin(storeId)
        }
    }

    fun convertExistingProductPhotos(targetMode: PhotoStorageMode, onDone: (Int, Int, Int) -> Unit = { _, _, _ -> }) {
        viewModelScope.launch {
            _isBusy.value = true
            clearErrorMessage()
            try {
                val res = repository.convertExistingProductPhotos(storeId, targetMode, products.value)
                stockPrefs?.setPhotoStorageMode(targetMode)
                onDone(res.convertedCount, res.skippedCount, res.failedCount)
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    currentLanguage.value,
                    fallbackEn = "Photo conversion failed. Please try again.",
                    fallbackBn = "ছবি রূপান্তর করা যায়নি। আবার চেষ্টা করুন।"
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    // Checkout / Sell (preserves existing calculation, stock, invoice, and Baki logic)
    private var saleCheckoutInFlight = false

    fun completeSale(
        items: List<SaleItem>,
        invoiceDiscount: Double = 0.0,
        paymentType: String = "cash",
        customerId: String = "",
        customerName: String = "Cash Sale",
        customerPhone: String = "",
        notes: String = "",
        accountId: String = "cash_in_hand",
        accountName: String = "Cash in Hand",
        paidNowAmount: Double? = null,
        clientSaleId: String = "",
        onSuccess: (CompletedSaleReceipt) -> Unit
    ) {
        if (saleCheckoutInFlight) return
        saleCheckoutInFlight = true

        val normalizedItems = items.map { item ->
            val stockQ = CalculationEngine.saleItemStockQty(item)
            val boundD = CalculationEngine.calculateBoundedDiscount(item.sellPrice, item.qty, item.discount)
            val netTotal = CalculationEngine.calculateLineNetTotal(item.sellPrice, item.qty, boundD)
            val profit = CalculationEngine.calculateLineProfit(item.sellPrice, item.buyPrice, item.qty, boundD)
            item.copy(stockQty = stockQ, discount = boundD, lineTotal = netTotal, profit = profit)
        }
        val subtotal = CalculationEngine.roundMoney(
            normalizedItems.sumOf { CalculationEngine.calculateLineGrossTotal(it.sellPrice, it.qty) }
        )
        val itemDiscounts = CalculationEngine.roundMoney(
            normalizedItems.sumOf { it.discount }
        )
        val safeInvoiceDiscount = CalculationEngine.roundMoney(invoiceDiscount.coerceAtLeast(0.0))
        if (CalculationEngine.isSaleDiscountOverProfit(normalizedItems, safeInvoiceDiscount)) {
            val lang = if (isFatherMode.value) AppLanguage.BN else currentLanguage.value
            val canSeeProfit = canSeeBuyingPriceAndProfitFlow.value
            val maxAllowedDiscount = CalculationEngine.calculateMaxInvoiceDiscount(normalizedItems)
            _errorMessage.value = if (canSeeProfit) {
                if (lang == AppLanguage.BN) {
                    "লাভের (${ShopLocale.formatMoney(maxAllowedDiscount, AppLanguage.BN)}) চেয়ে বেশি ছাড় দেওয়া যাবে না"
                } else {
                    "Cannot give more discount than profit (${ShopLocale.formatMoney(maxAllowedDiscount, AppLanguage.EN)})"
                }
            } else {
                if (lang == AppLanguage.BN) {
                    "অনুমোদিত সীমার চেয়ে বেশি ছাড় দেওয়া যাবে না"
                } else {
                    "Cannot give more discount than allowed limit"
                }
            }
            saleCheckoutInFlight = false
            return
        }
        val totalDiscount = CalculationEngine.roundMoney((itemDiscounts + safeInvoiceDiscount).coerceIn(0.0, subtotal))
        val grandTotal = CalculationEngine.roundMoney((subtotal - totalDiscount).coerceAtLeast(0.0))
        val totalItems = CalculationEngine.roundStock(normalizedItems.sumOf { CalculationEngine.saleItemStockQty(it) })

        val effectivePaid: Double
        val effectiveDue: Double
        val effectivePaymentType: String
        if (paidNowAmount != null) {
            effectivePaid = CalculationEngine.roundMoney(paidNowAmount.coerceIn(0.0, grandTotal))
            effectiveDue = CalculationEngine.roundMoney((grandTotal - effectivePaid).coerceAtLeast(0.0))
            effectivePaymentType = if (effectiveDue > 0.001) "baki" else "cash"
        } else {
            val isBaki = paymentType.equals("baki", ignoreCase = true)
            effectivePaid = if (isBaki) 0.0 else grandTotal
            effectiveDue = if (isBaki) grandTotal else 0.0
            effectivePaymentType = if (isBaki) "baki" else "cash"
        }

        viewModelScope.launch {
            _isBusy.value = true
            clearErrorMessage()
            try {
                val invoiceNo = repository.executeSaleCheckout(
                    storeId = storeId,
                    items = normalizedItems,
                    invoiceDiscount = safeInvoiceDiscount,
                    paymentType = effectivePaymentType,
                    customerId = customerId,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    notes = notes,
                    accountId = accountId,
                    accountName = accountName,
                    paidNowAmount = paidNowAmount,
                    clientSaleId = clientSaleId
                )
                if (effectivePaid > 0.001 && accountId.isNotBlank()) {
                    stockPrefs?.setLastUsedAccountId(accountId)
                }
                val st = store.value
                val logoUri = repository.appContext?.let { ctx ->
                    BusinessCardLocalStore.loadSavedCards(ctx)
                        .firstOrNull { it.draft.logoUri.isNotBlank() }
                        ?.draft?.logoUri
                        .orEmpty()
                }.orEmpty()
                val receipt = CompletedSaleReceipt(
                    invoiceNo = invoiceNo,
                    storeName = st?.name.orEmpty(),
                    storePhone = st?.phone.orEmpty(),
                    storeAddress = st?.address.orEmpty(),
                    items = normalizedItems,
                    totalItems = totalItems,
                    subtotal = subtotal,
                    discount = totalDiscount,
                    grandTotal = grandTotal,
                    paymentType = effectivePaymentType,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    notes = notes.trim(),
                    timestamp = System.currentTimeMillis(),
                    storeLogoUri = logoUri,
                    paidAmount = effectivePaid,
                    dueAmount = effectiveDue
                )
                onSuccess(receipt)
            } catch (e: Exception) {
                val lang = if (isFatherMode.value) AppLanguage.BN else currentLanguage.value
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    lang,
                    fallbackEn = ShopLocale.get("err_checkout_failed", lang),
                    fallbackBn = "বিক্রি সংরক্ষণ হয়নি, আবার চেষ্টা করুন"
                )
            } finally {
                saleCheckoutInFlight = false
                _isBusy.value = false
            }
        }
    }

    // Customers & Payments
    fun addCustomer(
        name: String,
        mobile: String = "",
        address: String = "",
        notes: String = "",
        onSuccess: (String) -> Unit = {}
    ) {
        val cust = Customer(
            storeId = storeId,
            name = name.trim(),
            mobile = mobile.trim(),
            address = address.trim(),
            notes = notes.trim()
        )
        viewModelScope.launch {
            _isBusy.value = true
            clearErrorMessage()
            try {
                val id = repository.addCustomer(storeId, cust)
                onSuccess(id)
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    currentLanguage.value,
                    fallbackEn = "Failed to add customer. Please try again.",
                    fallbackBn = "ক্রেতা যোগ করা যায়নি। আবার চেষ্টা করুন।"
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun updateCustomer(customer: Customer, onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.updateCustomer(storeId, customer)
        }
    }

    fun deleteCustomer(customer: Customer, onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.deleteCustomer(storeId, customer.id)
        }
    }

    fun recordCustomerPayment(
        customer: Customer,
        amount: Double,
        note: String = "",
        paymentDate: Long = System.currentTimeMillis(),
        accountId: String = lastUsedAccountId.value,
        accountName: String = accounts.value.firstOrNull { it.id == lastUsedAccountId.value }?.name ?: "Cash in Hand",
        onSuccess: () -> Unit = {}
    ) {
        launchBusy(onSuccess) {
            repository.recordCustomerPaymentAtomic(
                storeId = storeId,
                customerId = customer.id,
                customerName = customer.name,
                customerMobile = customer.mobile,
                amount = amount,
                note = note,
                paymentDate = paymentDate,
                accountId = accountId,
                accountName = accountName
            )
            stockPrefs?.setLastUsedAccountId(accountId)
        }
    }

    // Suppliers & Purchases
    fun addSupplier(
        name: String,
        mobile: String = "",
        company: String = "",
        address: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val sup = Supplier(
            storeId = storeId,
            name = name.trim(),
            mobile = mobile.trim(),
            company = company.trim(),
            address = address.trim()
        )
        launchBusy(onSuccess) {
            repository.addSupplier(storeId, sup)
        }
    }

    fun updateSupplier(supplier: Supplier, onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.updateSupplier(storeId, supplier)
        }
    }

    fun deleteSupplier(supplier: Supplier, onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.deleteSupplier(storeId, supplier.id)
        }
    }

    fun recordPurchase(
        supplier: Supplier,
        items: List<PurchaseItem>,
        paidAmount: Double = 0.0,
        note: String = "",
        accountId: String = "cash_in_hand",
        accountName: String = "Cash in Hand",
        onSuccess: () -> Unit = {}
    ) {
        val total = CalculationEngine.roundMoney(items.sumOf { it.lineTotal })
        val boundedPaid = paidAmount.coerceIn(0.0, total)
        val due = CalculationEngine.roundMoney((total - boundedPaid).coerceAtLeast(0.0))
        val paymentType = if (due > 0.001) "credit" else "paid"
        val purchase = Purchase(
            storeId = storeId,
            supplierId = supplier.id,
            supplierName = supplier.name,
            items = items,
            totalItems = CalculationEngine.roundStock(items.sumOf { it.stockQty }),
            totalAmount = total,
            paidAmount = boundedPaid,
            dueAmount = due,
            paymentType = paymentType,
            accountId = accountId,
            accountName = accountName,
            note = note.trim(),
            createdBy = currentUserId
        )
        launchBusy(onSuccess) {
            repository.recordPurchase(storeId, purchase)
            if (boundedPaid > 0.0) stockPrefs?.setLastUsedAccountId(accountId)
        }
    }

    fun recordPurchaseReturn(
        supplier: Supplier,
        amount: Double,
        reason: String,
        onSuccess: () -> Unit = {}
    ) {
        val ret = PurchaseReturn(
            storeId = storeId,
            supplierId = supplier.id,
            supplierName = supplier.name,
            amount = CalculationEngine.roundMoney(amount.coerceAtLeast(0.0)),
            reason = reason.trim()
        )
        launchBusy(onSuccess) {
            repository.recordPurchaseReturn(storeId, ret)
        }
    }

    fun recordSupplierPayment(
        supplier: Supplier,
        amount: Double,
        accountId: String = "cash_in_hand",
        accountName: String = "Cash in Hand",
        note: String = "",
        date: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit = {}
    ) {
        val payment = SupplierPayment(
            storeId = storeId,
            supplierId = supplier.id,
            supplierName = supplier.name,
            amount = CalculationEngine.roundMoney(amount),
            accountId = accountId,
            accountName = accountName,
            note = note.trim(),
            paymentDate = date,
            paidBy = currentUserId
        )
        launchBusy(onSuccess) {
            repository.recordSupplierPayment(payment)
            stockPrefs?.setLastUsedAccountId(accountId)
        }
    }

    // Accounts, Expenses, Other Income
    fun createAccount(
        name: String,
        type: String,
        accountNumber: String = "",
        openingBalance: Double = 0.0,
        onSuccess: () -> Unit = {}
    ) {
        val account = Account(
            storeId = storeId,
            name = name.trim(),
            type = type.trim(),
            accountNumber = accountNumber.trim(),
            openingBalance = CalculationEngine.roundMoney(openingBalance),
            isDefault = false
        )
        launchBusy(onSuccess) {
            repository.createAccount(account)
        }
    }

    fun archiveAccount(account: Account, onSuccess: () -> Unit = {}) {
        val bal = accountBalances.value[account.id] ?: account.openingBalance
        launchBusy(onSuccess) {
            repository.archiveAccount(storeId, account.id, bal)
        }
    }

    fun adjustAccountBalance(
        account: Account,
        newBalance: Double,
        note: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val oldBalance = accountBalances.value[account.id] ?: account.openingBalance
        val diff = CalculationEngine.roundMoney(newBalance - oldBalance)
        val adjustment = AccountAdjustment(
            storeId = storeId,
            accountId = account.id,
            accountName = account.name,
            oldBalance = oldBalance,
            newBalance = newBalance,
            difference = diff,
            note = note.trim(),
            adjustedBy = currentUserId
        )
        launchBusy(onSuccess) {
            repository.recordAccountAdjustment(adjustment)
        }
    }

    fun recordExpense(
        amount: Double,
        category: String,
        date: Long,
        accountId: String,
        accountName: String,
        note: String = "",
        receiptUri: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val expense = Expense(
            storeId = storeId,
            amount = CalculationEngine.roundMoney(amount),
            category = category.trim(),
            date = date,
            accountId = accountId,
            accountName = accountName,
            note = note.trim(),
            receiptUri = receiptUri,
            createdBy = currentUserId
        )
        launchBusy(onSuccess) {
            repository.recordExpense(expense)
            stockPrefs?.setLastUsedAccountId(accountId)
        }
    }

    fun deleteExpense(expense: Expense, note: String = "", onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.deleteExpense(storeId, expense.id, note)
        }
    }

    fun recordOtherIncome(
        amount: Double,
        category: String,
        date: Long,
        accountId: String,
        accountName: String,
        note: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val income = OtherIncome(
            storeId = storeId,
            amount = CalculationEngine.roundMoney(amount),
            category = category.trim(),
            date = date,
            accountId = accountId,
            accountName = accountName,
            note = note.trim(),
            createdBy = currentUserId
        )
        launchBusy(onSuccess) {
            repository.recordOtherIncome(income)
            stockPrefs?.setLastUsedAccountId(accountId)
        }
    }

    fun deleteOtherIncome(income: OtherIncome, note: String = "", onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.deleteOtherIncome(storeId, income.id, note)
        }
    }

    // Categories & Notes
    fun createCategory(name: String, type: String, onSuccess: () -> Unit = {}) {
        val cat = StoreCategory(
            storeId = storeId,
            name = name.trim(),
            type = type.trim()
        )
        launchBusy(onSuccess) {
            repository.createCategory(cat)
        }
    }

    fun updateCategory(category: StoreCategory, newName: String, onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.updateCategory(storeId, category.id, newName.trim())
        }
    }

    fun archiveCategory(category: StoreCategory, onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.archiveCategory(storeId, category.id)
        }
    }

    fun saveNote(
        id: String = "",
        title: String,
        text: String,
        date: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit = {}
    ) {
        val note = StoreNote(
            id = id,
            storeId = storeId,
            title = title.trim(),
            text = text.trim(),
            date = date,
            createdBy = currentUserId
        )
        launchBusy(onSuccess) {
            repository.saveNote(note)
        }
    }

    fun deleteNote(noteId: String, onSuccess: () -> Unit = {}) {
        launchBusy(onSuccess) {
            repository.deleteNote(storeId, noteId)
        }
    }

    // Staff & Permissions
    private fun showDemoUnavailableError() {
        _errorMessage.value = if (currentLanguage.value == AppLanguage.BN) {
            "ডেমো মোডে এই সুবিধাটি চালু নেই।"
        } else {
            "This feature is unavailable in demo mode."
        }
    }

    fun createStaffInvite(
        staffName: String,
        roleLabel: String,
        permissionLevel: String,
        permissions: Map<String, Boolean>,
        onSuccess: (StoreInvite) -> Unit = {}
    ) {
        if (isDemoMode) {
            showDemoUnavailableError()
            return
        }
        val cleanName = staffName.trim()
        if (cleanName.isBlank()) {
            _errorMessage.value = ShopLocale.get("staff_name_required", currentLanguage.value)
            return
        }
        val invite = StoreInvite(
            storeId = storeId,
            storeName = store.value?.name.orEmpty(),
            staffName = cleanName,
            roleLabel = roleLabel,
            permissionLevel = permissionLevel,
            permissions = permissions,
            createdByUid = currentUserId,
            createdByName = currentMember.value?.displayName ?: "Owner"
        )
        viewModelScope.launch {
            _isBusy.value = true
            clearErrorMessage()
            try {
                val generatedCode = repository.createStaffInvite(invite)
                onSuccess(invite.copy(id = generatedCode, code = generatedCode))
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    currentLanguage.value,
                    fallbackEn = "Could not create staff invite. Please try again.",
                    fallbackBn = "স্টাফ আমন্ত্রণ তৈরি করা যায়নি। আবার চেষ্টা করুন।"
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun cancelInvite(code: String) {
        if (isDemoMode) {
            showDemoUnavailableError()
            return
        }
        launchBusy {
            repository.cancelInvite(code)
        }
    }

    fun updateMemberPermissions(
        memberId: String,
        roleLabel: String,
        permissionLevel: String,
        permissions: Map<String, Boolean>,
        onSuccess: () -> Unit = {}
    ) {
        if (isDemoMode) {
            showDemoUnavailableError()
            return
        }
        launchBusy(onSuccess) {
            repository.updateMemberPermissions(
                storeId = storeId,
                memberId = memberId,
                roleLabel = roleLabel,
                permissionLevel = permissionLevel,
                permissions = permissions
            )
        }
    }

    fun removeMember(memberId: String, memberName: String = "", onSuccess: () -> Unit = {}) {
        if (isDemoMode) {
            showDemoUnavailableError()
            return
        }
        launchBusy(onSuccess) {
            repository.removeMember(storeId, memberId)
            _statusMessage.value = ShopLocale.format("staff_removed_msg", currentLanguage.value, memberName)
        }
    }

    fun restoreMember(memberId: String, memberName: String = "", onSuccess: () -> Unit = {}) {
        if (isDemoMode) {
            showDemoUnavailableError()
            return
        }
        launchBusy(onSuccess) {
            repository.restoreMember(storeId, memberId)
            _statusMessage.value = ShopLocale.format("staff_restored_msg", currentLanguage.value, memberName)
        }
    }

    fun setFatherModeForMember(
        memberId: String,
        enabled: Boolean,
        memberDisplayName: String = "Owner",
        memberEmail: String = "",
        onSuccess: () -> Unit = {}
    ) {
        launchBusy(onSuccess) {
            repository.setMemberFatherMode(
                storeId = storeId,
                memberId = memberId,
                enabled = enabled,
                fallbackName = memberDisplayName,
                fallbackEmail = memberEmail,
                isOwnerTarget = isOwner.value
            )
            if (enabled) {
                _localSimpleMode.value = true
            }
        }
    }

    // Data Health Check
    fun scanDataHealth() {
        viewModelScope.launch {
            _isScanningHealth.value = true
            clearErrorMessage()
            try {
                val results = withContext(Dispatchers.Default) {
                    CalculationEngine.scanDataHealth(
                        store = store.value,
                        products = products.value,
                        sales = sales.value,
                        purchases = purchases.value,
                        customers = customers.value,
                        suppliers = suppliers.value,
                        payments = payments.value,
                        accounts = accounts.value,
                        accountBalances = accountBalances.value,
                        expenses = expenses.value,
                        otherIncomes = otherIncomes.value,
                        supplierPayments = supplierPayments.value,
                        adjustments = adjustments.value,
                        lang = currentLanguage.value
                    )
                }
                _healthProblems.value = results
                _lastHealthScanMillis.value = System.currentTimeMillis()
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    currentLanguage.value,
                    fallbackEn = "Health scan failed. Please try again.",
                    fallbackBn = "ডেটা যাচাই সম্পন্ন করা যায়নি। আবার চেষ্টা করুন।"
                )
            } finally {
                _isScanningHealth.value = false
            }
        }
    }

    fun applyHealthFix(
        problem: HealthProblem,
        ownerUid: String = currentUserId,
        onSuccess: () -> Unit = {}
    ) {
        launchBusy {
            val fixed = repository.applyHealthFix(storeId, problem, ownerUid)
            if (fixed) {
                _healthProblems.value = _healthProblems.value.filterNot { it.id == problem.id }
                onSuccess()
            } else {
                _errorMessage.value = if (currentLanguage.value == AppLanguage.BN) {
                    "সমাধানটি সংরক্ষণ করা যায়নি।"
                } else {
                    "Fix could not be applied."
                }
            }
        }
    }

    // Business Strategy & AI Key
    fun refreshBusinessStrategy() {
        viewModelScope.launch {
            _isStrategyRefreshing.value = true
            clearErrorMessage()
            try {
                val todayKey = currentDateKey()
                val existing = savedBusinessStrategy.value
                val usedToday = if (existing?.dateKey == todayKey) existing.refreshCountToday else 0
                val facts = BusinessStrategyEngine.calculateBusinessStrategyFacts(
                    products = products.value,
                    sales = sales.value,
                    customers = customers.value,
                    payments = payments.value
                )
                val strategy = if (isDemoMode) {
                    BusinessStrategyBanglaBuilder.buildFixedStrategy(
                        storeId = storeId,
                        facts = facts,
                        refreshCountToday = usedToday,
                        updatedBy = currentUserId
                    )
                } else {
                    val apiKey = runCatching { repository.getPrivateAiKeyOnce(storeId) }.getOrNull()
                    if (!apiKey.isNullOrBlank() && strategyRefreshesRemainingToday.value > 0) {
                        val aiRes = repository.generateStrategyWithAi(
                            apiKey = apiKey,
                            storeId = storeId,
                            facts = facts,
                            generatedAtMs = System.currentTimeMillis(),
                            refreshCountToday = usedToday + 1,
                            updatedBy = currentUserId
                        )
                        aiRes.getOrElse {
                            BusinessStrategyBanglaBuilder.buildFixedStrategy(
                                storeId = storeId,
                                facts = facts,
                                refreshCountToday = usedToday,
                                updatedBy = currentUserId
                            )
                        }
                    } else {
                        BusinessStrategyBanglaBuilder.buildFixedStrategy(
                            storeId = storeId,
                            facts = facts,
                            refreshCountToday = usedToday,
                            updatedBy = currentUserId
                        )
                    }
                }
                repository.saveBusinessStrategy(storeId, strategy, currentUserId)
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    currentLanguage.value,
                    fallbackEn = "Could not refresh advice right now.",
                    fallbackBn = "এই মুহূর্তে পরামর্শ রিফ্রেশ করা যায়নি।"
                )
            } finally {
                _isStrategyRefreshing.value = false
            }
        }
    }

    fun testAndSavePrivateAiKey(rawKey: String, onSuccess: () -> Unit = {}) {
        if (isDemoMode) {
            showDemoUnavailableError()
            return
        }
        launchBusy(onSuccess) {
            val testRes = repository.testGeminiApiKey(rawKey)
            if (testRes.isFailure) {
                throw testRes.exceptionOrNull() ?: IllegalStateException("API key validation failed")
            }
            repository.savePrivateAiKey(storeId, rawKey, currentUserId)
            _statusMessage.value = ShopLocale.get("ai_key_saved_msg", currentLanguage.value)
        }
    }

    fun removePrivateAiKey(onSuccess: () -> Unit = {}) {
        if (isDemoMode) {
            showDemoUnavailableError()
            return
        }
        launchBusy(onSuccess) {
            repository.removePrivateAiKey(storeId)
            _statusMessage.value = ShopLocale.get("ai_key_removed_msg", currentLanguage.value)
        }
    }

    // Backup JSON Export & Import + CSV Export
    fun exportStoreBackupToUri(
        destinationUri: Uri,
        onSuccess: () -> Unit = {}
    ) {
        val ctx = repository.appContext ?: return
        launchBusy(onSuccess) {
            val payload = repository.exportStoreBackup(
                storeId = storeId,
                photoStorageMode = photoStorageMode.value,
                isSimpleMode = isSimpleMode.value,
                actingUserId = currentUserId
            )
            val json = StoreBackupCodec.encodeToJson(payload)
            withContext(Dispatchers.IO) {
                ctx.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("Could not open destination file")
            }
            _statusMessage.value = ShopLocale.get("backup_export_success", currentLanguage.value)
        }
    }

    fun previewBackupImportFromUri(
        sourceUri: Uri,
        onPreviewReady: (String, BackupImportPreview) -> Unit
    ) {
        val ctx = repository.appContext ?: return
        viewModelScope.launch {
            _isBusy.value = true
            clearErrorMessage()
            try {
                val lang = currentLanguage.value
                val rawJson = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Could not read selected file")
                }
                val payload = StoreBackupCodec.decodeAndValidateJson(rawJson, lang)
                val targetEmpty = repository.isStoreEmptyForImport(storeId)
                val preview = BackupImportPreview(
                    payload = payload,
                    version = payload.version,
                    sourceStoreName = payload.store.name,
                    shopType = payload.shopType,
                    exportedAtMillis = payload.exportedAtMillis,
                    targetStoreIsEmpty = targetEmpty,
                    productCategoriesCount = payload.productCategories.size,
                    categoryRecordsCount = payload.categoryRecords.size,
                    productsCount = payload.products.size,
                    customersCount = payload.customers.size,
                    suppliersCount = payload.suppliers.size,
                    salesCount = payload.sales.size,
                    purchasesCount = payload.purchases.size,
                    purchaseReturnsCount = payload.purchaseReturns.size,
                    paymentsCount = payload.payments.size,
                    supplierPaymentsCount = payload.supplierPayments.size,
                    accountsCount = payload.accounts.size,
                    accountAdjustmentsCount = payload.accountAdjustments.size,
                    expensesCount = payload.expenses.size,
                    incomeCount = payload.otherIncomes.size,
                    notesCount = payload.notes.size
                )
                onPreviewReady(rawJson, preview)
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    currentLanguage.value,
                    fallbackEn = "Invalid backup file.",
                    fallbackBn = "ব্যাকআপ ফাইলটি সঠিক নয়।"
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun confirmBackupImport(
        rawJson: String,
        mode: BackupImportMode,
        onSuccess: (BackupImportSummary) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            clearErrorMessage()
            try {
                val summary = repository.importStoreBackup(
                    storeId = storeId,
                    rawJson = rawJson,
                    mode = mode,
                    photoStorageMode = photoStorageMode.value,
                    isSimpleMode = isSimpleMode.value,
                    actingUserId = currentUserId
                )
                onSuccess(summary)
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    currentLanguage.value,
                    fallbackEn = "Backup import failed. Please try again.",
                    fallbackBn = "ব্যাকআপ ইমপোর্ট ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun exportCsvTarget(
        target: CsvExportTarget,
        onReadyFile: (File) -> Unit
    ) {
        val ctx = repository.appContext ?: return
        val lang = currentLanguage.value
        val st = store.value
        val nowStr = ShopLocale.formatDateShort(System.currentTimeMillis(), lang)
        viewModelScope.launch {
            _isBusy.value = true
            clearErrorMessage()
            try {
                val table = withContext(Dispatchers.Default) {
                    when (target) {
                        CsvExportTarget.PRODUCTS -> ReportTableData(
                            title = ShopLocale.get(target.labelKey, lang),
                            storeName = st?.name.orEmpty(),
                            storePhone = st?.phone.orEmpty(),
                            storeAddress = st?.address.orEmpty(),
                            periodLabel = ShopLocale.get("all", lang),
                            generatedAt = nowStr,
                            headers = listOf("Name", "Category", "Unit", "Buy Price", "Sell Price", "Stock", "Barcode"),
                            rows = products.value.map {
                                listOf(
                                    it.name,
                                    it.category,
                                    it.unit,
                                    it.buyPrice.toString(),
                                    it.sellPrice.toString(),
                                    it.stock.toString(),
                                    it.barcode
                                )
                            },
                            numericColumns = setOf(3, 4, 5)
                        )
                        CsvExportTarget.CUSTOMERS -> ReportTableData(
                            title = ShopLocale.get(target.labelKey, lang),
                            storeName = st?.name.orEmpty(),
                            storePhone = st?.phone.orEmpty(),
                            storeAddress = st?.address.orEmpty(),
                            periodLabel = ShopLocale.get("all", lang),
                            generatedAt = nowStr,
                            headers = listOf("Name", "Mobile", "Address", "Balance"),
                            rows = allCustomerSummaries.value.map {
                                listOf(
                                    it.customer.name,
                                    it.customer.mobile,
                                    it.customer.address,
                                    CalculationEngine.roundMoney(it.currentBalance).toString()
                                )
                            },
                            numericColumns = setOf(3)
                        )
                        CsvExportTarget.SUPPLIERS -> ReportTableData(
                            title = ShopLocale.get(target.labelKey, lang),
                            storeName = st?.name.orEmpty(),
                            storePhone = st?.phone.orEmpty(),
                            storeAddress = st?.address.orEmpty(),
                            periodLabel = ShopLocale.get("all", lang),
                            generatedAt = nowStr,
                            headers = listOf("Name", "Company", "Mobile", "Balance"),
                            rows = suppliers.value.map {
                                listOf(
                                    it.name,
                                    it.company,
                                    it.mobile,
                                    CalculationEngine.roundMoney(it.balance).toString()
                                )
                            },
                            numericColumns = setOf(3)
                        )
                        CsvExportTarget.SALES -> ReportTableData(
                            title = ShopLocale.get(target.labelKey, lang),
                            storeName = st?.name.orEmpty(),
                            storePhone = st?.phone.orEmpty(),
                            storeAddress = st?.address.orEmpty(),
                            periodLabel = ShopLocale.get("all", lang),
                            generatedAt = nowStr,
                            headers = listOf("Invoice", "Date", "Customer", "Payment", "Subtotal", "Discount", "Grand Total"),
                            rows = sales.value.map {
                                listOf(
                                    it.invoiceNo,
                                    ShopLocale.formatDateShort(it.createdAt?.toDate()?.time ?: 0L, lang),
                                    it.customerName,
                                    it.paymentType,
                                    CalculationEngine.roundMoney(it.subtotal).toString(),
                                    CalculationEngine.roundMoney(it.discount).toString(),
                                    CalculationEngine.roundMoney(it.grandTotal).toString()
                                )
                            },
                            numericColumns = setOf(4, 5, 6)
                        )
                    }
                }
                val file = withContext(Dispatchers.IO) {
                    ReportExportUtil.generateCsvReport(ctx, table, "storeos_${target.slug}", lang)
                }
                onReadyFile(file)
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    lang,
                    fallbackEn = "CSV export failed. Please try again.",
                    fallbackBn = "CSV এক্সপোর্ট ব্যর্থ হয়েছে। আবার চেষ্টা করুন।"
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun currentDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun launchBusy(
        onSuccess: () -> Unit = {},
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            clearErrorMessage()
            try {
                block()
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = FirestoreErrorHandler.toUserFriendlyMessage(
                    e,
                    currentLanguage.value,
                    fallbackEn = "Operation failed. Please try again.",
                    fallbackBn = "কাজটি সম্পন্ন করা যায়নি। আবার চেষ্টা করুন।"
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    internal fun dispose() {
        onCleared()
        viewModelScope.cancel()
    }
}
