package com.example

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Account
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.OtherIncome
import com.example.data.model.Payment
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.Store
import com.example.data.model.Supplier
import com.example.data.model.SupplierPayment
import com.example.data.repository.StoreRepository
import com.example.domain.CalculationEngine
import com.example.ui.screens.MainShopScreen
import com.example.ui.screens.MonthlyReportScreen
import com.example.ui.theme.StoreOSTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.NetworkMonitor
import com.example.util.ReportDataBuilders
import com.example.util.ReportExportUtil
import com.example.util.ShopLocale
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class MonthlyReportComprehensiveTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("storeos_stock_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("storeos_app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun ts(year: Int, monthZeroBased: Int, day: Int, hour: Int = 12): Timestamp {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Timestamp(Date(cal.timeInMillis))
    }

    private data class RichMonthFixture(
        val year: Int,
        val month: Int,
        val store: Store,
        val accounts: List<Account>,
        val products: List<Product>,
        val customers: List<Customer>,
        val suppliers: List<Supplier>,
        val sales: List<Sale>,
        val payments: List<Payment>,
        val purchases: List<Purchase>,
        val purchaseReturns: List<PurchaseReturn>,
        val supplierPayments: List<SupplierPayment>,
        val expenses: List<Expense>,
        val otherIncomes: List<OtherIncome>
    )

    private fun buildRichMonthFixture(): RichMonthFixture {
        // Use a completed historical month: January 2026 (year = 2026, month = 0)
        // Plus pre-month baki invoices at various past dates so ageing covers 0-30, 31-60, 61-90, 90+ relative to Jan 31, 2026
        val year = 2026
        val month = Calendar.JANUARY // 0

        val store = Store(
            id = "test_store",
            name = "Bismillah Store",
            phone = "01711000000",
            address = "Dhaka"
        )

        val cashAcc = Account(id = "cash_in_hand", name = "Cash in Hand", type = "cash", openingBalance = 5000.0)
        val bkashAcc = Account(id = "bkash_wallet", name = "bKash Merchant", type = "mobile_banking", openingBalance = 2000.0)
        val accounts = listOf(cashAcc, bkashAcc)

        val pRice = Product(
            id = "p_rice",
            name = "Miniket Rice",
            nameBn = "মিনিকেট চাল",
            category = "Grocery",
            unit = "kg",
            buyPrice = 60.0,
            sellPrice = 75.0,
            stock = 4.0, // Low stock (<= 10.0)
            lowStockLimit = 10.0
        )
        val pOil = Product(
            id = "p_oil",
            name = "Soybean Oil",
            nameBn = "সয়াবিন তেল",
            category = "Edible Oil",
            unit = "litre",
            buyPrice = 150.0,
            sellPrice = 180.0,
            stock = 20.0,
            lowStockLimit = 5.0
        )
        val pSlow = Product(
            id = "p_spice",
            name = "Cardamom Pack",
            nameBn = "এলাচ প্যাকেট",
            category = "Spices",
            unit = "pcs",
            buyPrice = 200.0,
            sellPrice = 260.0,
            stock = 12.0,
            lowStockLimit = 2.0
        )
        val products = listOf(pRice, pOil, pSlow)

        // Old baki sales before Jan 2026 for ageing buckets (relative to Jan 31, 2026):
        // - Cust 2: Dec 20, 2025 (~42 days -> 31-60)
        // - Cust 3: Nov 20, 2025 (~72 days -> 61-90)
        // - Cust 4: Oct 01, 2025 (~122 days -> 90+)
        // - Cust 1: Jan 15, 2026 (~16 days -> 0-30), also receives a payment in Jan 2026!
        val cust1 = Customer(id = "c1", name = "Rahim Mia", mobile = "01711111111", currentBalance = 250.0)
        val cust2 = Customer(id = "c2", name = "Karim Uddin", mobile = "01722222222", currentBalance = 400.0)
        val cust3 = Customer(id = "c3", name = "Jamal Hossain", mobile = "01733333333", currentBalance = 600.0)
        val cust4 = Customer(id = "c4", name = "Salam Chowdhury", mobile = "01744444444", currentBalance = 800.0)
        val customers = listOf(cust1, cust2, cust3, cust4)

        val saleOldC2 = Sale(
            id = "s_old_2",
            invoiceNo = "INV-OLD-2",
            customerId = cust2.id,
            customerName = cust2.name,
            customerPhone = cust2.mobile,
            items = listOf(CalculationEngine.buildSaleItem(pOil, 2.0)), // 360 sell, +40? let's force exact due 400
            subtotal = 400.0,
            grandTotal = 400.0,
            profit = 60.0,
            totalItems = 2.0,
            paymentType = "baki",
            paidAmount = 0.0,
            dueAmount = 400.0,
            accountId = cashAcc.id,
            createdAt = ts(2025, Calendar.DECEMBER, 20)
        )
        val saleOldC3 = Sale(
            id = "s_old_3",
            invoiceNo = "INV-OLD-3",
            customerId = cust3.id,
            customerName = cust3.name,
            customerPhone = cust3.mobile,
            items = listOf(CalculationEngine.buildSaleItem(pRice, 8.0)),
            subtotal = 600.0,
            grandTotal = 600.0,
            profit = 120.0,
            totalItems = 8.0,
            paymentType = "baki",
            paidAmount = 0.0,
            dueAmount = 600.0,
            accountId = cashAcc.id,
            createdAt = ts(2025, Calendar.NOVEMBER, 20)
        )
        val saleOldC4 = Sale(
            id = "s_old_4",
            invoiceNo = "INV-OLD-4",
            customerId = cust4.id,
            customerName = cust4.name,
            customerPhone = cust4.mobile,
            items = listOf(CalculationEngine.buildSaleItem(pRice, 10.0)),
            subtotal = 800.0,
            grandTotal = 800.0,
            profit = 200.0,
            totalItems = 10.0,
            paymentType = "baki",
            paidAmount = 0.0,
            dueAmount = 800.0,
            accountId = cashAcc.id,
            createdAt = ts(2025, Calendar.OCTOBER, 1)
        )

        // Jan 2026 Sales:
        // 1) Jan 05: Cash sale of 5 kg Rice -> 5*75 = 375 (cost 300, profit 75) into Cash
        val itemJanRice = CalculationEngine.buildSaleItem(pRice, 5.0)
        val saleJanCash = Sale(
            id = "s_jan_1",
            invoiceNo = "INV-JAN-1",
            customerId = "",
            customerName = "Cash Sale",
            items = listOf(itemJanRice),
            subtotal = itemJanRice.lineTotal,
            grandTotal = itemJanRice.lineTotal,
            profit = itemJanRice.profit,
            totalItems = 5.0,
            paymentType = "cash",
            paidAmount = itemJanRice.lineTotal,
            dueAmount = 0.0,
            accountId = cashAcc.id,
            createdAt = ts(year, month, 5)
        )
        // 2) Jan 15: Baki (part-paid) sale to Rahim (cust1) of 4 L Oil -> 4*180 = 720 (cost 600, profit 120), paid 320 to bKash, baki 400
        val itemJanOil = CalculationEngine.buildSaleItem(pOil, 4.0)
        val saleJanBaki = Sale(
            id = "s_jan_2",
            invoiceNo = "INV-JAN-2",
            customerId = cust1.id,
            customerName = cust1.name,
            customerPhone = cust1.mobile,
            items = listOf(itemJanOil),
            subtotal = itemJanOil.lineTotal,
            grandTotal = itemJanOil.lineTotal,
            profit = itemJanOil.profit,
            totalItems = 4.0,
            paymentType = "baki",
            paidAmount = 320.0,
            dueAmount = 400.0,
            accountId = bkashAcc.id,
            createdAt = ts(year, month, 15)
        )
        // Customer payment in Jan 2026 from Rahim: 150 into Cash -> closing baki for cust1 = 400 - 150 = 250!
        val paymentJan = Payment(
            id = "pay_jan_1",
            customerId = cust1.id,
            customerName = cust1.name,
            amount = 150.0,
            accountId = cashAcc.id,
            paymentDate = ts(year, month, 20).toDate().time,
            createdAt = ts(year, month, 20)
        )

        // Supplier setup: Supplier 1 "Alam Traders" -> Jan 03 Credit purchase 2000 (paid 500, due 1500),
        // Jan 12 Supplier Payment 600 from bKash, Jan 18 Purchase Return 200.
        // Closing supplier payable = 0 + 1500 - 600 - 200 = 700.
        val sup1 = Supplier(id = "sup1", name = "Alam Traders", mobile = "01811111111", balance = 700.0)
        val suppliers = listOf(sup1)

        val purchaseItem = CalculationEngine.buildPurchaseItem(pOil, 10.0, 200.0)
        val purchaseJan = Purchase(
            id = "pur_jan_1",
            purchaseNo = "PUR-001",
            supplierId = sup1.id,
            supplierName = sup1.name,
            items = listOf(purchaseItem),
            totalAmount = 2000.0,
            paidAmount = 500.0,
            dueAmount = 1500.0,
            paymentType = "credit",
            accountId = cashAcc.id,
            createdAt = ts(year, month, 3)
        )
        val supPaymentJan = SupplierPayment(
            id = "sp_jan_1",
            supplierId = sup1.id,
            supplierName = sup1.name,
            amount = 600.0,
            accountId = bkashAcc.id,
            paymentDate = ts(year, month, 12).toDate().time,
            createdAt = ts(year, month, 12)
        )
        val returnJan = PurchaseReturn(
            id = "ret_jan_1",
            returnNo = "RET-001",
            supplierId = sup1.id,
            supplierName = sup1.name,
            amount = 200.0,
            reason = "Damaged bottle",
            createdAt = ts(year, month, 18)
        )

        // Expenses in Jan 2026: 120 Rent + 80 Transport = 200 (from Cash)
        val expRent = Expense(
            id = "exp_1",
            category = "Rent",
            amount = 120.0,
            note = "Monthly stall fee",
            accountId = cashAcc.id,
            date = ts(year, month, 8).toDate().time,
            createdAt = ts(year, month, 8)
        )
        val expTransport = Expense(
            id = "exp_2",
            category = "Transport",
            amount = 80.0,
            note = "Van delivery",
            accountId = cashAcc.id,
            date = ts(year, month, 16).toDate().time,
            createdAt = ts(year, month, 16)
        )

        // Other income in Jan 2026: 90 Commission (into Cash)
        val incCommission = OtherIncome(
            id = "inc_1",
            category = "Commission",
            amount = 90.0,
            note = "bKash bonus",
            accountId = cashAcc.id,
            date = ts(year, month, 22).toDate().time,
            createdAt = ts(year, month, 22)
        )

        return RichMonthFixture(
            year = year,
            month = month,
            store = store,
            accounts = accounts,
            products = products,
            customers = customers,
            suppliers = suppliers,
            sales = listOf(saleOldC2, saleOldC3, saleOldC4, saleJanCash, saleJanBaki),
            payments = listOf(paymentJan),
            purchases = listOf(purchaseJan),
            purchaseReturns = listOf(returnJan),
            supplierPayments = listOf(supPaymentJan),
            expenses = listOf(expRent, expTransport),
            otherIncomes = listOf(incCommission)
        )
    }

    private fun computeFixtureReport(
        fixture: RichMonthFixture,
        lang: AppLanguage = AppLanguage.EN,
        extraCashOffset: Double = 0.0
    ) = CalculationEngine.calculateMonthlyReportData(
        year = fixture.year,
        month = fixture.month,
        allSales = fixture.sales,
        products = fixture.products,
        customers = fixture.customers,
        suppliers = fixture.suppliers.let { list ->
            if (extraCashOffset == 0.0) list else list
        },
        allPayments = fixture.payments,
        lang = lang,
        allPurchases = fixture.purchases,
        allPurchaseReturns = fixture.purchaseReturns,
        allSupplierPayments = fixture.supplierPayments,
        allExpenses = fixture.expenses,
        allOtherIncomes = fixture.otherIncomes,
        accounts = fixture.accounts
    )

    @Test
    fun richMonth_matchesHomeHistoryBusiness_andAllReconciliationChecksPass() {
        val f = buildRichMonthFixture()
        val report = computeFixtureReport(f, AppLanguage.EN)

        val (startMs, endMs) = StoreRepository(null, null).monthBounds(f.year, f.month)
        val janSales = f.sales.filter { (it.createdAt?.toDate()?.time ?: 0L) in startMs..endMs }
        val janPurchases = f.purchases.filter { (it.createdAt?.toDate()?.time ?: 0L) in startMs..endMs }
        val janReturns = f.purchaseReturns.filter { (it.createdAt?.toDate()?.time ?: 0L) in startMs..endMs }
        val janExpenses = f.expenses.filter { it.date in startMs..endMs }
        val janIncomes = f.otherIncomes.filter { it.date in startMs..endMs }

        // 1. Parity with History (calculatePeriodSummary)
        val historySummary = CalculationEngine.calculatePeriodSummary(janSales, purchasesTotal = janPurchases.sumOf { it.totalAmount })
        assertEquals(historySummary.totalSold, report.netSales, 0.01)
        assertEquals(historySummary.totalProfit, report.totalProfit, 0.01)
        assertEquals(historySummary.totalOrders, report.totalOrders)
        assertEquals(historySummary.totalItemsSold, report.totalItemsSold, 0.001)

        // 2. Parity with Business P&L (calculateProfitAndLoss)
        val pnl = CalculationEngine.calculateProfitAndLoss(
            sales = janSales,
            purchases = janPurchases,
            returns = janReturns,
            expenses = janExpenses,
            otherIncomes = janIncomes,
            isStockBased = true
        )
        assertEquals(pnl.costOfGoodsSold, report.costOfGoodsSold, 0.01)
        assertEquals(pnl.grossProfit, report.grossProfit, 0.01)
        assertEquals(pnl.expenses, report.totalExpenses, 0.01)
        assertEquals(pnl.otherIncome, report.otherIncome, 0.01)
        assertEquals(pnl.netProfit, report.netProfit, 0.01)

        // 3. Parity with Home Stock Summary & Business Category Sales
        val stockSummary = CalculationEngine.calculateStockSummary(f.products)
        assertEquals(stockSummary.stockValueAtCost, report.closingStockValue, 0.01)
        assertEquals(stockSummary.totalItems, report.stockItemCount)
        assertEquals(1, report.lowStockItems.size)
        assertEquals("p_rice", report.lowStockItems.first().productId)

        // Opening stock is NOT guessed (null & check isAvailable = false)
        assertNull(report.openingStockValueComputed)
        assertFalse(report.openingStockAvailable)
        assertFalse(report.stockCheck.isAvailable)
        val stockLineEn = ReportDataBuilders.formatMonthlyCheckLine(report.stockCheck, AppLanguage.EN)
        val stockLineBn = ReportDataBuilders.formatMonthlyCheckLine(report.stockCheck, AppLanguage.BN)
        assertTrue(stockLineEn.contains("Not available", ignoreCase = true))
        assertFalse("Stock check EN must not duplicate 'Not available (Not available)': $stockLineEn", stockLineEn.contains("Not available (Not available)"))
        assertTrue("Stock check EN should explain opening stock is not tracked: $stockLineEn", stockLineEn.contains("Opening stock is not tracked"))
        assertTrue(stockLineBn.contains("পাওয়া যাচ্ছে না"))
        assertFalse("Stock check BN must not duplicate 'পাওয়া যাচ্ছে না (পাওয়া যাচ্ছে না)': $stockLineBn", stockLineBn.contains("পাওয়া যাচ্ছে না (পাওয়া যাচ্ছে না)"))
        assertTrue("Stock check BN should explain opening stock is not tracked: $stockLineBn", stockLineBn.contains("শুরুর স্টকের হিসাব সংরক্ষিত নেই"))

        // Slow sellers (30+ days idle): p_spice was never sold -> shows up
        assertTrue(report.slowStock30DayItems.any { it.productId == "p_spice" })

        val catMap = CalculationEngine.calculateSalesByCategory(janSales, f.products)
        assertEquals(catMap.size, report.salesByCategoryRows.size)
        catMap.forEach { (cat, amt) ->
            val row = report.salesByCategoryRows.first { it.category == cat }
            assertEquals(amt, row.salesAmount, 0.01)
        }
        assertEquals(1095.0, report.salesByCategoryTotalSales, 0.01)
        assertEquals(9.0, report.salesByCategoryTotalItems, 0.001)
        assertEquals(195.0, report.salesByCategoryTotalProfit, 0.01)

        // 4. Baki ageing 0-30, 31-60, 61-90, 90+
        assertEquals(1800.0, report.bakiOpeningReceivable, 0.01) // 400+600+800
        assertEquals(400.0, report.bakiNewCreditGiven, 0.01)
        assertEquals(150.0, report.bakiCollectedThisMonth, 0.01)
        assertEquals(2050.0, report.bakiClosingReceivable, 0.01)
        assertEquals(250.0, report.bakiAgeing.bucket0To30, 0.01)
        assertEquals(400.0, report.bakiAgeing.bucket31To60, 0.01)
        assertEquals(600.0, report.bakiAgeing.bucket61To90, 0.01)
        assertEquals(800.0, report.bakiAgeing.bucket90Plus, 0.01)
        assertEquals(4, report.debtorCount)
        assertEquals(4, report.debtorRowsGrouped.size)
        assertEquals(4, report.bakiAgeingGroups.size)
        assertEquals(listOf("0-30", "31-60", "61-90", "90+"), report.bakiAgeingGroups.map { it.bucketKey })

        // 5. Suppliers & Purchases
        assertEquals(0.0, report.supplierOpeningPayable, 0.01)
        assertEquals(2000.0, report.totalBought, 0.01)
        assertEquals(1500.0, report.supplierCreditPurchasesThisMonth, 0.01)
        assertEquals(600.0, report.supplierPaymentsOutThisMonth, 0.01)
        assertEquals(200.0, report.purchaseReturnsTotal, 0.01)
        assertEquals(1800.0, report.netPurchases, 0.01)
        assertEquals(700.0, report.supplierClosingPayable, 0.01)
        assertEquals(1, report.supplierRows.size)
        assertEquals(1, report.supplierRowCount)
        assertEquals(1, report.lowStockCount)
        assertEquals(1, report.slowStockCount)
        assertEquals(2, report.totalExpenseCount)
        assertEquals(1, report.totalOtherIncomeCount)
        assertEquals(-110.0, report.netOtherIncomeMinusExpenses, 0.01)

        // 6. Account Cash & Bank Parity with Business Account Balances and Home MoneyNow
        // Opening: Cash 5000, bKash 2000 => 7000
        // Money In: Cash (+375 sale +150 baki pay +90 income = +615), bKash (+320 sale = +320) => Total +935
        // Money Out: Cash (500 purchase + 120 exp + 80 exp = 700), bKash (600 supPay) => Total 1300
        // Closing: Cash 4915, bKash 1720 => Total 6635
        assertEquals(7000.0, report.cashOpeningTotal, 0.01)
        assertEquals(935.0, report.cashInTotal, 0.01)
        assertEquals(1300.0, report.cashOutTotal, 0.01)
        assertEquals(6635.0, report.cashClosingBalance, 0.01)

        val accountBalances = CalculationEngine.calculateAccountBalances(
            accounts = f.accounts,
            sales = f.sales,
            paymentsIn = f.payments,
            purchases = f.purchases,
            supplierPayments = f.supplierPayments,
            expenses = f.expenses,
            otherIncomes = f.otherIncomes,
            adjustments = emptyList()
        )
        assertEquals(accountBalances.values.sum(), report.cashClosingBalance, 0.01)
        val moneyNow = CalculationEngine.calculateMoneyNow(
            products = f.products,
            customers = f.customers,
            suppliers = f.suppliers,
            allSales = f.sales,
            allPayments = f.payments,
            totalCashAndBankBalance = accountBalances.values.sum()
        )
        assertEquals(moneyNow.stockValueAtCost, report.closingStockValue, 0.01)
        assertEquals(moneyNow.bakiCustomersOwe, report.bakiClosingReceivable, 0.01)
        assertEquals(moneyNow.debtToSuppliers, report.supplierClosingPayable, 0.01)
        assertEquals(moneyNow.totalCashAndBankBalance, report.cashClosingBalance, 0.01)

        // 7. All computable numbers-check lines match cleanly with 0 diff
        listOf(
            report.summaryCheck,
            report.summaryCheckNoProfit,
            report.salesCheck,
            report.bakiCheck,
            report.supplierCheck,
            report.purchasesCheck,
            report.expensesIncomeCheck,
            report.cashCheck
        ).forEach { chk ->
            assertTrue("Expected check ${chk.key} to match, diff=${chk.difference}", chk.matches)
            assertEquals(0.0, chk.difference, 0.01)
        }
    }

    @Test
    fun forcedMismatch_inNumbersCheck_flagsExactDifferenceWarning() {
        val f = buildRichMonthFixture()
        // Corrupt Alam Traders stored closing balance by +150.0 without a matching transaction
        val brokenSuppliers = f.suppliers.map { it.copy(balance = it.balance + 150.0) }
        val report = CalculationEngine.calculateMonthlyReportData(
            year = f.year,
            month = f.month,
            allSales = f.sales,
            products = f.products,
            customers = f.customers,
            suppliers = brokenSuppliers,
            allPayments = f.payments,
            lang = AppLanguage.EN,
            allPurchases = f.purchases,
            allPurchaseReturns = f.purchaseReturns,
            allSupplierPayments = f.supplierPayments,
            allExpenses = f.expenses,
            allOtherIncomes = f.otherIncomes,
            accounts = f.accounts
        )

        assertFalse(report.supplierCheck.matches)
        assertEquals(150.0, report.supplierCheck.difference, 0.01)
        val warningEn = ReportDataBuilders.formatMonthlyCheckLine(report.supplierCheck, AppLanguage.EN)
        assertTrue("Warning string should mention mismatch and +150: $warningEn", warningEn.contains("150"))
        assertTrue(warningEn.contains("Warning", ignoreCase = true))

        val warningBn = ReportDataBuilders.formatMonthlyCheckLine(report.supplierCheck, AppLanguage.BN)
        assertTrue("Bangla warning should use Bengali digits ১৫০: $warningBn", warningBn.contains("১৫০"))
        assertTrue(warningBn.contains("সতর্কতা"))

        // Also test Sales paid + baki split mismatch (grandTotal=720, paid=300, due=395 => split diff +25)
        val brokenSales = f.sales.map { s ->
            if (s.id == "s_jan_2") s.copy(paidAmount = 300.0, dueAmount = 395.0) else s
        }
        val brokenSalesReport = CalculationEngine.calculateMonthlyReportData(
            year = f.year,
            month = f.month,
            allSales = brokenSales,
            products = f.products,
            customers = f.customers,
            suppliers = f.suppliers,
            allPayments = f.payments,
            lang = AppLanguage.EN,
            allPurchases = f.purchases,
            allPurchaseReturns = f.purchaseReturns,
            allSupplierPayments = f.supplierPayments,
            allExpenses = f.expenses,
            allOtherIncomes = f.otherIncomes,
            accounts = f.accounts
        )
        assertFalse(brokenSalesReport.salesCheck.matches)
        assertEquals(25.0, brokenSalesReport.salesCheck.difference, 0.01)
        assertEquals(1070.0, brokenSalesReport.salesCheck.expectedClosing, 0.01)
        assertEquals(1095.0, brokenSalesReport.salesCheck.actualClosing, 0.01)
    }

    @Test
    fun pdfAndCsvExports_inBanglaAndEnglish_fitLandscapeAndMatchScreenDigitsAndNumbers() {
        val f = buildRichMonthFixture()
        // Embedded Bangla font must resolve
        assertNotNull(ResourcesCompat.getFont(context, R.font.noto_sans_bengali))

        // ENGLISH EXPORTS
        val enReport = computeFixtureReport(f, AppLanguage.EN)
        val enTable = ReportDataBuilders.buildMonthlyReportTable(
            reportData = enReport,
            year = f.year,
            month = f.month,
            store = f.store,
            canSeeProfit = true,
            lang = AppLanguage.EN,
            includeSupplierDebt = true,
            isRestrictedMode = false
        )
        assertTrue("Detailed multi-column Monthly Report PDF should use Landscape", enTable.preferLandscape)
        assertTrue(enTable.sections.any { it.title.contains("Summary") })
        assertTrue(enTable.sections.any { it.title.contains("Sales by Day") })
        assertTrue(enTable.sections.any { it.title.contains("Top 5 Products") })
        assertTrue(enTable.sections.any { it.title.contains("Top 5 Customers") })
        assertTrue(enTable.sections.any { it.title.contains("Sales by Category") })
        assertTrue(enTable.sections.any { it.title.contains("Baki") })
        assertTrue(enTable.sections.any { it.title.contains("Suppliers") })
        assertTrue(enTable.sections.any { it.title.contains("Low Stock") })
        assertTrue(enTable.sections.any { it.title.contains("Slow Sellers") })
        assertTrue(enTable.sections.any { it.title.contains("Purchases") })
        assertTrue(enTable.sections.any { it.title.contains("Expenses") })
        assertTrue(enTable.sections.any { it.title.contains("Other Income") })
        assertTrue(enTable.sections.any { it.title.contains("Cash & Bank") })

        val enPdf: File = ReportExportUtil.generatePdfReport(context, enTable, "MonthlyReport_EN_Test", AppLanguage.EN)
        assertTrue(enPdf.exists() && enPdf.length() > 500)
        val enPdfText = enPdf.readText(Charsets.ISO_8859_1)
        assertTrue(enPdfText.startsWith("%PDF-1.4"))
        // Landscape A4 MediaBox (842 x 595)
        assertTrue("PDF should declare Landscape MediaBox 842x595", enPdfText.contains("0 0 842 595"))

        val enCsv: File = ReportExportUtil.generateCsvReport(context, enTable, "MonthlyReport_EN_Test", AppLanguage.EN)
        val enCsvText = enCsv.readText(Charsets.UTF_8)
        assertTrue(enCsvText.contains("Bismillah Store"))
        assertTrue(enCsvText.contains("Alam Traders"))
        assertTrue(enCsvText.contains("Not available"))
        assertTrue("EN CSV should match sales 1095", enCsvText.contains("1095"))
        assertTrue("EN CSV should match gross profit 195", enCsvText.contains("195"))
        assertTrue("EN CSV should match net profit 85", enCsvText.contains("85"))
        assertTrue("EN CSV should match baki closing 2050", enCsvText.contains("2050"))
        assertTrue("EN CSV should match supplier closing 700", enCsvText.contains("700"))
        assertTrue("EN CSV should match net purchases 1800", enCsvText.contains("1800"))
        assertTrue("EN CSV should match expenses 200", enCsvText.contains("200"))
        assertTrue("EN CSV should match other income 90", enCsvText.contains("90"))
        assertTrue("EN CSV should match total cash 6635", enCsvText.contains("6635"))
        assertTrue("EN CSV should include slow seller total stuck", enCsvText.contains("Total Stuck"))
        assertTrue("EN CSV should include check line", enCsvText.contains("Numbers check"))

        // Verify FileProvider URI resolution and export actions (Download, Print, Share) do not fail
        val pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", enPdf)
        val csvUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", enCsv)
        assertNotNull(pdfUri)
        assertNotNull(csvUri)
        ReportExportUtil.downloadReportFile(context, enCsv, AppLanguage.EN)
        ReportExportUtil.shareReportFile(context, enPdf, "application/pdf", "Monthly Report", AppLanguage.EN)
        ReportExportUtil.printReportFile(context, enPdf, "Monthly Report", AppLanguage.EN, isLandscape = enTable.preferLandscape)

        // BENGALI EXPORTS
        val bnReport = computeFixtureReport(f, AppLanguage.BN)
        val bnTable = ReportDataBuilders.buildMonthlyReportTable(
            reportData = bnReport,
            year = f.year,
            month = f.month,
            store = f.store,
            canSeeProfit = true,
            lang = AppLanguage.BN,
            includeSupplierDebt = true,
            isRestrictedMode = false
        )
        val bnPdf: File = ReportExportUtil.generatePdfReport(context, bnTable, "MonthlyReport_BN_Test", AppLanguage.BN)
        assertTrue(bnPdf.exists() && bnPdf.length() > 500)
        assertTrue(bnPdf.readText(Charsets.ISO_8859_1).contains("0 0 842 595"))

        val bnCsv: File = ReportExportUtil.generateCsvReport(context, bnTable, "MonthlyReport_BN_Test", AppLanguage.BN)
        val bnCsvText = bnCsv.readText(Charsets.UTF_8)
        assertTrue("BN CSV should format sales in Bengali digits ১০৯৫", bnCsvText.contains("১০৯৫"))
        assertTrue("BN CSV should format gross profit ১৯৫", bnCsvText.contains("১৯৫"))
        assertTrue("BN CSV should format net profit ৮৫", bnCsvText.contains("৮৫"))
        assertTrue("BN CSV should format baki closing ২০৫০", bnCsvText.contains("২০৫০"))
        assertTrue("BN CSV should format supplier closing ৭০০", bnCsvText.contains("৭০০"))
        assertTrue("BN CSV should format net purchases ১৮০০", bnCsvText.contains("১৮০০"))
        assertTrue("BN CSV should format expenses ২০০", bnCsvText.contains("২০০"))
        assertTrue("BN CSV should format other income ৯০", bnCsvText.contains("৯০"))
        assertTrue("BN CSV should format cash balance in Bengali digits ৬৬৩৫", bnCsvText.contains("৬৬৩৫"))
        assertTrue("BN CSV should show পাওয়া যাচ্ছে না for opening stock", bnCsvText.contains("পাওয়া যাচ্ছে না"))
        assertTrue("BN CSV should include check line", bnCsvText.contains("হিসাব মিল"))

        // EMPTY MONTH EXPORTS (EN & BN): friendly message, zero empty table headers/cells
        val emptyReportEn = CalculationEngine.calculateMonthlyReportData(
            year = 2020,
            month = Calendar.JANUARY,
            allSales = emptyList(),
            products = emptyList(),
            customers = emptyList(),
            suppliers = emptyList(),
            allPayments = emptyList(),
            lang = AppLanguage.EN,
            allPurchases = emptyList(),
            allPurchaseReturns = emptyList(),
            allSupplierPayments = emptyList(),
            allExpenses = emptyList(),
            allOtherIncomes = emptyList(),
            accounts = emptyList()
        )
        assertTrue(emptyReportEn.isMonthEmpty)
        val emptyTableEn = ReportDataBuilders.buildMonthlyReportTable(
            reportData = emptyReportEn,
            year = 2020,
            month = Calendar.JANUARY,
            store = f.store,
            canSeeProfit = true,
            lang = AppLanguage.EN,
            includeSupplierDebt = true,
            isRestrictedMode = false
        )
        assertTrue(emptyTableEn.summaryMetrics.isEmpty())
        assertEquals(1, emptyTableEn.sections.size)
        assertTrue(emptyTableEn.sections.first().headers.isEmpty())
        assertTrue(emptyTableEn.sections.first().rows.isEmpty())
        val emptyCsvEn = ReportExportUtil.generateCsvReport(context, emptyTableEn, "Empty_EN", AppLanguage.EN).readText(Charsets.UTF_8)
        assertTrue(emptyCsvEn.contains("No activity in this month"))
        assertTrue(emptyCsvEn.contains("no sales, purchases, payments, expenses or income recorded"))
        assertFalse("Empty-month CSV must not emit empty comma cells", emptyCsvEn.contains(",,"))
        val emptyPdfEn = ReportExportUtil.generatePdfReport(context, emptyTableEn, "Empty_EN", AppLanguage.EN)
        assertTrue(emptyPdfEn.exists() && emptyPdfEn.length() > 300)

        val emptyTableBn = ReportDataBuilders.buildMonthlyReportTable(
            reportData = emptyReportEn,
            year = 2020,
            month = Calendar.JANUARY,
            store = f.store,
            canSeeProfit = true,
            lang = AppLanguage.BN,
            includeSupplierDebt = false,
            isRestrictedMode = true
        )
        val emptyCsvBn = ReportExportUtil.generateCsvReport(context, emptyTableBn, "Empty_BN", AppLanguage.BN).readText(Charsets.UTF_8)
        assertTrue(emptyCsvBn.contains("কোনো লেনদেন নেই"))
        assertFalse("Restricted empty-month BN CSV must have zero English", Regex("[A-Za-z]+").containsMatchIn(emptyCsvBn))
    }

    @Test
    fun restrictedExport_fatherAndSimpleModes_haveZeroEnglishWordsAndRespectProfitToggle() {
        val f = buildRichMonthFixture()
        val bnReport = computeFixtureReport(f, AppLanguage.BN)

        val noProfitTable = ReportDataBuilders.buildMonthlyReportTable(
            reportData = bnReport,
            year = f.year,
            month = f.month,
            store = f.store,
            canSeeProfit = false,
            lang = AppLanguage.BN,
            includeSupplierDebt = false,
            isRestrictedMode = true
        )
        assertEquals(3, noProfitTable.sections.size)
        val csvNoProfit = ReportExportUtil.generateCsvReport(context, noProfitTable, "Restricted_NoProfit", AppLanguage.BN)
            .readText(Charsets.UTF_8)
        assertFalse("Restricted CSV must hide profit label when setting is off", csvNoProfit.contains("মোট লাভ"))
        assertFalse("Restricted CSV must hide net profit label when setting is off", csvNoProfit.contains("নিট লাভ"))
        assertFalse("Restricted CSV Summary check must NOT leak gross profit ১৯৫", csvNoProfit.contains("১৯৫"))
        assertFalse("Restricted CSV Summary check must NOT leak net profit ৮৫", csvNoProfit.contains("৮৫"))
        assertFalse("Restricted CSV must hide Suppliers", csvNoProfit.contains("সাপ্লায়ার"))
        assertFalse("Restricted CSV must contain ZERO ASCII English words", Regex("[A-Za-z]+").containsMatchIn(csvNoProfit))
        val pdfNoProfit = ReportExportUtil.generatePdfReport(context, noProfitTable, "Restricted_NoProfit", AppLanguage.BN)
        assertTrue(pdfNoProfit.exists() && pdfNoProfit.length() > 500)

        val withProfitTable = ReportDataBuilders.buildMonthlyReportTable(
            reportData = bnReport,
            year = f.year,
            month = f.month,
            store = f.store,
            canSeeProfit = true,
            lang = AppLanguage.BN,
            includeSupplierDebt = false,
            isRestrictedMode = true
        )
        val csvWithProfit = ReportExportUtil.generateCsvReport(context, withProfitTable, "Restricted_WithProfit", AppLanguage.BN)
            .readText(Charsets.UTF_8)
        assertTrue("Restricted CSV should show profit when setting is enabled", csvWithProfit.contains("মোট লাভ"))
        assertTrue("Restricted CSV should show net profit when setting is enabled", csvWithProfit.contains("নিট লাভ"))
        val pdfWithProfit = ReportExportUtil.generatePdfReport(context, withProfitTable, "Restricted_WithProfit", AppLanguage.BN)
        assertTrue(pdfWithProfit.exists() && pdfWithProfit.length() > 500)
    }

    @Test
    fun dateRangeLoader_filtersOutOfMonthFutureRecords() {
        val f = buildRichMonthFixture()
        val futureSale = Sale(
            id = "s_future",
            invoiceNo = "INV-MAR-99",
            customerName = "Future Buyer",
            grandTotal = 9999.0,
            paidAmount = 9999.0,
            paymentType = "cash",
            createdAt = ts(2026, Calendar.MARCH, 10)
        )
        val repo = StoreRepository(db = null, appContext = context)
        val inputs = repo.filterMonthlyInputsByDateRange(
            year = 2026,
            month = Calendar.JANUARY,
            allSales = f.sales + futureSale,
            allPurchases = f.purchases,
            allPurchaseReturns = f.purchaseReturns,
            allPayments = f.payments,
            allSupplierPayments = f.supplierPayments,
            allExpenses = f.expenses,
            allOtherIncomes = f.otherIncomes,
            allAdjustments = emptyList(),
            nowMillis = ts(2026, Calendar.FEBRUARY, 5).toDate().time
        )
        assertFalse("Out-of-range future March sale must be excluded for Jan 2026 range", inputs.sales.any { it.id == "s_future" })
        assertTrue(inputs.sales.any { it.id == "s_jan_1" })
    }

    @Test
    fun ui_detailedMode_displaysAll8SectionsTablesCheckLines_andEmptyMonthMessage() {
        val repository = StoreRepository(db = null, appContext = context)
        val storeViewModel = StoreViewModel(
            storeId = "demo_store",
            repository = repository,
            networkMonitor = NetworkMonitor(context),
            currentUserId = "demo_user"
        )
        storeViewModel.setLanguage(AppLanguage.EN)
        storeViewModel.setSimpleMode(false)

        composeRule.setContent {
            StoreOSTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalAppLanguage provides AppLanguage.EN) {
                    MonthlyReportScreen(storeViewModel = storeViewModel)
                }
            }
        }
        composeRule.waitForIdle()

        // Summary card & its numbers-check line are displayed (scroll inside LazyColumn if viewport is short)
        composeRule.onNodeWithTag("section_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("monthly_report_list").performScrollToNode(hasTestTag("check_summary_line"))
        composeRule.onNodeWithTag("check_summary_line").assertIsDisplayed()

        // Tap each of the 7 collapsible cards -> expands to full tables + per-section check line
        val sectionsAndChecks = listOf(
            Triple("section_sales", "section_sales_card", "check_sales_line"),
            Triple("section_baki", "section_baki_card", "check_baki_line"),
            Triple("section_suppliers", "section_suppliers_card", "check_suppliers_line"),
            Triple("section_stock", "section_stock_card", "check_stock_line"),
            Triple("section_purchases", "section_purchases_card", "check_purchases_line"),
            Triple("section_expenses_income", "section_expenses_income_card", "check_expenses_income_line"),
            Triple("section_cash", "section_cash_card", "check_cash_line")
        )
        sectionsAndChecks.forEach { (itemKey, cardTag, checkTag) ->
            composeRule.onNodeWithTag("monthly_report_list").performScrollToKey(itemKey)
            composeRule.onNodeWithTag(cardTag).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("monthly_report_list").performScrollToNode(hasTestTag(checkTag))
            composeRule.onNodeWithTag(checkTag).assertIsDisplayed()
        }

        // Switch to an empty month (Jan 2020) -> ONLY friendly empty-month message shown, no empty section tables
        storeViewModel.setMonthlyReportMonth(2020, Calendar.JANUARY)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("monthly_report_list").performScrollToKey("empty_month_banner")
        composeRule.onNodeWithTag("empty_month_banner").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("section_summary_card").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("section_sales_card").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("section_baki_card").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("section_cash_card").fetchSemanticsNodes().size)
    }

    @Test
    fun ui_fatherMode_showsOnlySummaryBakiLowStock_plainBanglaZeroEnglish_andHidesProfit() {
        val repository = StoreRepository(db = null, appContext = context)
        val storeViewModel = StoreViewModel(
            storeId = "demo_store",
            repository = repository,
            networkMonitor = NetworkMonitor(context),
            currentUserId = "demo_user"
        )
        storeViewModel.setFatherModeForMember(memberId = "demo_user", enabled = true)
        storeViewModel.setShowProfitInSimpleMode(false)

        composeRule.setContent {
            StoreOSTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalAppLanguage provides AppLanguage.BN) {
                    MonthlyReportScreen(storeViewModel = storeViewModel)
                }
            }
        }
        composeRule.waitForIdle()

        // Summary visible first; collect texts across whole screen
        composeRule.onNodeWithTag("section_summary_card").assertIsDisplayed()
        val collectedTexts = mutableSetOf<String>()
        collectedTexts.addAll(collectAllSemanticTexts())

        // Scroll to Baki & Low Stock and verify they exist while Detailed-only sections do NOT exist
        composeRule.onNodeWithTag("monthly_report_list").performScrollToKey("section_baki")
        composeRule.onNodeWithTag("section_baki_card").assertIsDisplayed()
        composeRule.onNodeWithTag("section_baki_card").performClick()
        composeRule.waitForIdle()
        collectedTexts.addAll(collectAllSemanticTexts())

        composeRule.onNodeWithTag("monthly_report_list").performScrollToKey("section_low_stock")
        composeRule.onNodeWithTag("section_low_stock_card").assertIsDisplayed()
        composeRule.onNodeWithTag("section_low_stock_card").performClick()
        composeRule.waitForIdle()
        collectedTexts.addAll(collectAllSemanticTexts())

        assertEquals(0, composeRule.onAllNodesWithTag("section_sales_card").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("section_suppliers_card").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("section_purchases_card").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("section_expenses_income_card").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("section_cash_card").fetchSemanticsNodes().size)

        // Profit must be hidden in Father Mode when showProfitInSimpleMode is false
        assertFalse(collectedTexts.any { it == "মোট লাভ" || it == "নিট লাভ" })

        val offendingEnglish = collectedTexts.filter { Regex("[A-Za-z]+").containsMatchIn(it) }
        assertTrue(
            "Father mode Monthly Report must contain zero English words (text or contentDescription), found: $offendingEnglish",
            offendingEnglish.isEmpty()
        )
    }

    @Test
    fun ui_simpleMode_homeEntry_showsSummaryBakiLowStock_andRespectsProfitToggle() {
        val repository = StoreRepository(db = null, appContext = context)
        val authViewModel = AuthViewModel(repository)
        val storeViewModel = StoreViewModel(
            storeId = "demo_store",
            repository = repository,
            networkMonitor = NetworkMonitor(context),
            currentUserId = "demo_user"
        )
        storeViewModel.setLanguage(AppLanguage.BN)
        storeViewModel.setSimpleMode(true)
        storeViewModel.setShowProfitInSimpleMode(false)

        composeRule.setContent {
            StoreOSTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalAppLanguage provides AppLanguage.BN) {
                    MainShopScreen(authViewModel = authViewModel, storeViewModel = storeViewModel)
                }
            }
        }
        composeRule.waitForIdle()

        // Open Monthly Report directly from SimpleHomeLayout
        composeRule.onNodeWithTag("home_dashboard_list").performScrollToKey("simple_todays_accounts_card")
        composeRule.onNodeWithTag("simple_monthly_report_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("monthly_report_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("section_summary_card").assertIsDisplayed()
        // Profit hidden when toggle is OFF
        assertEquals(0, composeRule.onAllNodesWithText("নিট লাভ").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("monthly_report_list").performScrollToKey("section_baki")
        composeRule.onNodeWithTag("section_baki_card").assertIsDisplayed()

        composeRule.onNodeWithTag("monthly_report_list").performScrollToKey("section_low_stock")
        composeRule.onNodeWithTag("section_low_stock_card").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("section_suppliers_card").fetchSemanticsNodes().size)

        // Turn Simple-mode profit setting ON -> scroll back to summary & profit appears
        storeViewModel.setShowProfitInSimpleMode(true)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("monthly_report_list").performScrollToKey("section_summary")
        composeRule.onNodeWithText("নিট লাভ").assertIsDisplayed()
    }

    private fun collectAllSemanticTexts(): List<String> {
        val out = mutableListOf<String>()
        fun walk(node: androidx.compose.ui.semantics.SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.Text)?.forEach { s ->
                val v = s.text.trim()
                if (v.isNotEmpty()) out.add(v)
            }
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { desc ->
                val v = desc.trim()
                if (v.isNotEmpty()) out.add(v)
            }
            node.children.forEach { child -> walk(child) }
        }
        walk(composeRule.onRoot().fetchSemanticsNode())
        return out
    }
}
