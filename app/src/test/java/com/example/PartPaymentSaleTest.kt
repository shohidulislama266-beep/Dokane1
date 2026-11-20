package com.example

import com.example.data.model.Account
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.repository.StoreRepository
import com.example.domain.CalculationEngine
import com.example.domain.LedgerEntryType
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class PartPaymentSaleTest {

    private fun newDemoRepo(): StoreRepository = StoreRepository(db = null, appContext = null)

    private fun cashBalance(
        accounts: List<Account>,
        sales: List<Sale>,
        payments: List<com.example.data.model.Payment>
    ): Double {
        return CalculationEngine.calculateAccountBalances(
            accounts = accounts,
            sales = sales,
            paymentsIn = payments,
            purchases = emptyList(),
            supplierPayments = emptyList(),
            expenses = emptyList(),
            otherIncomes = emptyList(),
            adjustments = emptyList()
        )["cash_in_hand"] ?: 0.0
    }

    @Test
    fun fullCashSale_updatesStockCashAccountAndReports() = runBlocking {
        val repo = newDemoRepo()
        val productsBefore = repo.observeProducts("demo_store").first()
        val prod = productsBefore.first { it.stock >= 5.0 }
        val salesBefore = repo.observeSales("demo_store").first()
        val paymentsBefore = repo.observePayments("demo_store").first()
        val accounts = repo.observeAccounts("demo_store").first()
        val cashBefore = cashBalance(accounts, salesBefore, paymentsBefore)

        val item = CalculationEngine.buildSaleItem(product = prod, qty = 2.0)
        val expectedTotal = CalculationEngine.roundMoney(item.sellPrice * 2.0)

        val invoiceNo = repo.executeSaleCheckout(
            storeId = "demo_store",
            items = listOf(item),
            invoiceDiscount = 0.0,
            paymentType = "cash",
            accountId = "cash_in_hand",
            accountName = "Cash in Hand",
            paidNowAmount = expectedTotal,
            clientSaleId = "test-full-cash-1"
        )

        val productsAfter = repo.observeProducts("demo_store").first()
        val salesAfter = repo.observeSales("demo_store").first()
        val paymentsAfter = repo.observePayments("demo_store").first()
        val savedSale = salesAfter.firstOrNull { it.invoiceNo == invoiceNo }

        assertNotNull(savedSale)
        assertEquals("cash", savedSale!!.paymentType)
        assertEquals(expectedTotal, savedSale.grandTotal, 0.01)
        assertEquals(expectedTotal, savedSale.paidAmount, 0.01)
        assertEquals(0.0, savedSale.dueAmount, 0.01)
        assertEquals("cash_in_hand", savedSale.accountId)
        assertFalse(CalculationEngine.isPartPaidSale(savedSale))

        // No extra payment row created
        assertEquals(paymentsBefore.size, paymentsAfter.size)

        // Stock deducted exactly once
        val updatedProd = productsAfter.first { it.id == prod.id }
        assertEquals(prod.stock - 2.0, updatedProd.stock, 0.001)

        // Cash account increased by full sale total
        val cashAfter = cashBalance(accounts, salesAfter, paymentsAfter)
        assertEquals(cashBefore + expectedTotal, cashAfter, 0.01)
    }

    @Test
    fun fullBakiSale_updatesStockCustomerBalanceAndReports() = runBlocking {
        val repo = newDemoRepo()
        val prod = repo.observeProducts("demo_store").first().first { it.stock >= 5.0 }
        val customer = repo.observeCustomers("demo_store").first().first()
        val salesBefore = repo.observeSales("demo_store").first()
        val paymentsBefore = repo.observePayments("demo_store").first()
        val accounts = repo.observeAccounts("demo_store").first()
        val cashBefore = cashBalance(accounts, salesBefore, paymentsBefore)

        val item = CalculationEngine.buildSaleItem(product = prod, qty = 1.0)
        val expectedTotal = CalculationEngine.roundMoney(item.sellPrice)

        val invoiceNo = repo.executeSaleCheckout(
            storeId = "demo_store",
            items = listOf(item),
            invoiceDiscount = 0.0,
            paymentType = "baki",
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.mobile,
            accountId = "cash_in_hand",
            accountName = "Cash in Hand",
            paidNowAmount = 0.0,
            clientSaleId = "test-full-baki-1"
        )

        val salesAfter = repo.observeSales("demo_store").first()
        val paymentsAfter = repo.observePayments("demo_store").first()
        val customerAfter = repo.observeCustomers("demo_store").first().first { it.id == customer.id }
        val savedSale = salesAfter.first { it.invoiceNo == invoiceNo }

        assertEquals("baki", savedSale.paymentType)
        assertEquals(0.0, savedSale.paidAmount, 0.01)
        assertEquals(expectedTotal, savedSale.dueAmount, 0.01)
        assertEquals("", savedSale.accountId)
        assertFalse(CalculationEngine.isPartPaidSale(savedSale))
        assertEquals(paymentsBefore.size, paymentsAfter.size)
        assertEquals(customer.currentBalance + expectedTotal, customerAfter.currentBalance, 0.01)

        // Cash account unchanged
        val cashAfter = cashBalance(accounts, salesAfter, paymentsAfter)
        assertEquals(cashBefore, cashAfter, 0.01)
    }

    @Test
    fun partPaymentSale_isSingleLine_linksToRightInvoice_andReconcilesMonthlyReport() = runBlocking {
        val repo = newDemoRepo()
        val prod = repo.observeProducts("demo_store").first().first { it.stock >= 5.0 }
        val customer = repo.observeCustomers("demo_store").first().first()
        val salesBefore = repo.observeSales("demo_store").first()
        val paymentsBefore = repo.observePayments("demo_store").first()
        val accounts = repo.observeAccounts("demo_store").first()
        val cashBefore = cashBalance(accounts, salesBefore, paymentsBefore)

        val item = CalculationEngine.buildSaleItem(product = prod, qty = 2.0)
        val total = CalculationEngine.roundMoney(item.sellPrice * 2.0)
        val paidNow = CalculationEngine.roundMoney(total * 0.4)
        val expectedDue = CalculationEngine.roundMoney(total - paidNow)

        val invoiceNo = repo.executeSaleCheckout(
            storeId = "demo_store",
            items = listOf(item),
            invoiceDiscount = 0.0,
            paymentType = "cash", // even if started in cash mode, remaining > 0 becomes baki
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.mobile,
            accountId = "cash_in_hand",
            accountName = "Cash in Hand",
            paidNowAmount = paidNow,
            clientSaleId = "test-part-pay-1"
        )

        val salesAfter = repo.observeSales("demo_store").first()
        val paymentsAfter = repo.observePayments("demo_store").first()
        val customerAfter = repo.observeCustomers("demo_store").first().first { it.id == customer.id }
        val savedSale = salesAfter.first { it.invoiceNo == invoiceNo }

        // 1. Stored on the sale itself; no separate payment row
        assertEquals("baki", savedSale.paymentType)
        assertEquals(total, savedSale.grandTotal, 0.01)
        assertEquals(paidNow, savedSale.paidAmount, 0.01)
        assertEquals(expectedDue, savedSale.dueAmount, 0.01)
        assertEquals("cash_in_hand", savedSale.accountId)
        assertTrue(CalculationEngine.isPartPaidSale(savedSale))
        assertEquals(paymentsBefore.size, paymentsAfter.size)

        // 2. Customer balance increased only by remaining due
        assertEquals(customer.currentBalance + expectedDue, customerAfter.currentBalance, 0.01)

        // 3. Cash account increased only by paidNow
        val cashAfter = cashBalance(accounts, salesAfter, paymentsAfter)
        assertEquals(cashBefore + paidNow, cashAfter, 0.01)

        // 4. Customer ledger shows ONE entry for this sale with total, paid, and due
        val ledger = CalculationEngine.calculateCustomerLedger(customerAfter, salesAfter, paymentsAfter)
        val matchingEntries = ledger.filter { it.referenceNo == invoiceNo }
        assertEquals(1, matchingEntries.size)
        val entry = matchingEntries.first()
        assertEquals(LedgerEntryType.BAKI_SALE, entry.type)
        assertEquals(expectedDue, entry.amount, 0.01)
        assertEquals(total, entry.totalAmount, 0.01)
        assertEquals(paidNow, entry.paidAmount, 0.01)

        // 5. Right invoice + Monthly Report reconciliation:
        // Build an isolated dataset with an OLDER unpaid invoice (45 days ago, legacy format without paidAmount/dueAmount)
        // and a NEW part-paid invoice this month. Verify the old invoice stays 100% in 31-60d bucket (NOT paid down by new part payment)
        // AND Monthly Report cashCheck / bakiCheck reconcile cleanly.
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val nowMs = System.currentTimeMillis()
        val fortyFiveDaysAgoMs = nowMs - 45L * 24L * 60L * 60L * 1000L

        val testCustomer = Customer(id = "cust_1", storeId = "s1", name = "Karim", currentBalance = 700.0)
        val testAccount = Account(id = "cash_in_hand", storeId = "s1", name = "Cash in Hand", openingBalance = 1000.0, isDefault = true)
        val testProduct = Product(id = "p1", storeId = "s1", name = "Rice", buyPrice = 50.0, sellPrice = 100.0, stock = 10.0)

        val oldItem = CalculationEngine.buildSaleItem(product = testProduct, qty = 4.0)
        val newItem = CalculationEngine.buildSaleItem(product = testProduct, qty = 5.0)

        val olderLegacyBakiSale = Sale(
            id = "sale_old",
            storeId = "s1",
            invoiceNo = "INV-0001",
            items = listOf(oldItem),
            totalItems = 4.0,
            subtotal = 400.0,
            grandTotal = 400.0,
            profit = 200.0,
            paymentType = "baki",
            paidAmount = 0.0, // legacy sale with defaulted 0.0/0.0
            dueAmount = 0.0,
            customerId = "cust_1",
            customerName = "Karim",
            createdAt = Timestamp(Date(fortyFiveDaysAgoMs))
        )
        val newPartPaidSale = Sale(
            id = "sale_new",
            storeId = "s1",
            invoiceNo = "INV-0002",
            items = listOf(newItem),
            totalItems = 5.0,
            subtotal = 500.0,
            grandTotal = 500.0,
            profit = 250.0,
            paymentType = "baki",
            paidAmount = 200.0,
            dueAmount = 300.0,
            accountId = "cash_in_hand",
            accountName = "Cash in Hand",
            customerId = "cust_1",
            customerName = "Karim",
            createdAt = Timestamp(Date(nowMs))
        )

        val report = CalculationEngine.calculateMonthlyReportData(
            year = year,
            month = month,
            allSales = listOf(olderLegacyBakiSale, newPartPaidSale),
            products = listOf(testProduct),
            customers = listOf(testCustomer),
            suppliers = emptyList(),
            allPayments = emptyList(),
            accounts = listOf(testAccount)
        )

        // Paid portion (200) counts as cash received; only due portion (300) counts as credit sold
        assertEquals(200.0, report.cashReceivedSales, 0.01)
        assertEquals(300.0, report.bakiCreditSales, 0.01)
        assertEquals(500.0, report.totalSales, 0.01)

        // Cash Opening = 1000, Cash In = 200, Cash Closing = 1200, check matches
        assertEquals(1000.0, report.cashOpeningTotal, 0.01)
        assertEquals(1200.0, report.cashClosingBalance, 0.01)
        assertTrue("Cash reconciliation must match", report.cashCheck.matches)
        assertEquals(0.0, report.cashCheck.difference, 0.01)

        // Baki Opening = 400 (old legacy sale), New Credit = 300, Baki Closing = 700, check matches
        assertEquals(400.0, report.bakiOpeningReceivable, 0.01)
        assertEquals(300.0, report.bakiNewCreditGiven, 0.01)
        assertEquals(700.0, report.bakiClosingReceivable, 0.01)
        assertTrue("Baki reconciliation must match", report.bakiCheck.matches)
        assertEquals(0.0, report.bakiCheck.difference, 0.01)

        // Old 400 invoice was NOT touched by new sale's 200 part-payment:
        // ageing 31-60 days must still be 400.0 and 0-30 days must be 300.0
        assertEquals(400.0, report.bakiAgeing.bucket31To60, 0.01)
        assertEquals(300.0, report.bakiAgeing.bucket0To30, 0.01)
    }

    @Test
    fun failedSale_keepsStockSalesAndCustomerBalanceUnchanged() = runBlocking {
        val repo = newDemoRepo()
        val productsBefore = repo.observeProducts("demo_store").first()
        val prod1 = productsBefore[0]
        val prod2 = productsBefore[1]
        val customerBefore = repo.observeCustomers("demo_store").first().first()
        val salesBefore = repo.observeSales("demo_store").first()

        // Case A: Second item exceeds stock -> first item's stock must NOT be deducted either (atomic)
        val validItem = CalculationEngine.buildSaleItem(product = prod1, qty = 1.0)
        val overStockItem = CalculationEngine.buildSaleItem(product = prod2, qty = prod2.stock + 50.0)

        var threwStockError = false
        try {
            repo.executeSaleCheckout(
                storeId = "demo_store",
                items = listOf(validItem, overStockItem),
                paymentType = "baki",
                customerId = customerBefore.id,
                customerName = customerBefore.name,
                paidNowAmount = 50.0,
                clientSaleId = "test-fail-stock-1"
            )
        } catch (e: Exception) {
            threwStockError = true
        }
        assertTrue("Expected insufficient stock failure", threwStockError)

        // Verify nothing changed
        val productsAfterA = repo.observeProducts("demo_store").first()
        assertEquals(prod1.stock, productsAfterA.first { it.id == prod1.id }.stock, 0.001)
        assertEquals(prod2.stock, productsAfterA.first { it.id == prod2.id }.stock, 0.001)
        assertEquals(salesBefore.size, repo.observeSales("demo_store").first().size)
        assertEquals(
            customerBefore.currentBalance,
            repo.observeCustomers("demo_store").first().first { it.id == customerBefore.id }.currentBalance,
            0.01
        )

        // Case B: Part payment with remaining baki > 0 but NO customer selected -> must fail cleanly
        var threwCustomerError = false
        try {
            repo.executeSaleCheckout(
                storeId = "demo_store",
                items = listOf(validItem),
                paymentType = "cash",
                customerId = "",
                customerName = "Cash Sale",
                paidNowAmount = 10.0, // less than validItem total
                clientSaleId = "test-fail-customer-1"
            )
        } catch (e: Exception) {
            threwCustomerError = true
        }
        assertTrue("Expected missing customer failure for remaining baki", threwCustomerError)
        assertEquals(prod1.stock, repo.observeProducts("demo_store").first().first { it.id == prod1.id }.stock, 0.001)
        assertEquals(salesBefore.size, repo.observeSales("demo_store").first().size)
    }

    @Test
    fun doubleTapOrRetryWithSameClientSaleId_createsOnlyOneSaleAndDeductsStockOnce() = runBlocking {
        val repo = newDemoRepo()
        val prod = repo.observeProducts("demo_store").first().first { it.stock >= 5.0 }
        val customer = repo.observeCustomers("demo_store").first().first()
        val salesBefore = repo.observeSales("demo_store").first()
        val paymentsBefore = repo.observePayments("demo_store").first()

        val item = CalculationEngine.buildSaleItem(product = prod, qty = 2.0)
        val total = CalculationEngine.roundMoney(item.sellPrice * 2.0)
        val paidNow = CalculationEngine.roundMoney(total * 0.5)
        val expectedDue = CalculationEngine.roundMoney(total - paidNow)
        val attemptId = "unique-attempt-id-999"

        // First call succeeds
        val invoice1 = repo.executeSaleCheckout(
            storeId = "demo_store",
            items = listOf(item),
            paymentType = "baki",
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.mobile,
            paidNowAmount = paidNow,
            clientSaleId = attemptId
        )

        // Second call (double tap / retry with same clientSaleId) must return same invoice without duplicating
        val invoice2 = repo.executeSaleCheckout(
            storeId = "demo_store",
            items = listOf(item),
            paymentType = "baki",
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.mobile,
            paidNowAmount = paidNow,
            clientSaleId = attemptId
        )

        assertEquals(invoice1, invoice2)
        val salesAfter = repo.observeSales("demo_store").first()
        assertEquals(salesBefore.size + 1, salesAfter.size)
        assertEquals(paymentsBefore.size, repo.observePayments("demo_store").first().size)

        // Stock deducted only once (2.0, not 4.0)
        val updatedProd = repo.observeProducts("demo_store").first().first { it.id == prod.id }
        assertEquals(prod.stock - 2.0, updatedProd.stock, 0.001)

        // Customer balance increased only once
        val updatedCustomer = repo.observeCustomers("demo_store").first().first { it.id == customer.id }
        assertEquals(customer.currentBalance + expectedDue, updatedCustomer.currentBalance, 0.01)
    }
}
