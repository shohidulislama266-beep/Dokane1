package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Product
import com.example.data.repository.StoreRepository
import com.example.domain.CalculationEngine
import com.example.domain.ShopType
import com.example.ui.screens.FatherSellStep3Content
import com.example.ui.screens.SellScreen
import com.example.ui.theme.StoreOSTheme
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.NetworkMonitor
import com.example.util.PhotoStorageMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xhdpi")
class SaleProfitDiscountCapTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calculationEngine_capsDiscountAtPreDiscountProfit() {
        val product = Product(
            id = "p_tea",
            storeId = "demo_store",
            name = "Tea Pack",
            buyPrice = 130.0,
            sellPrice = 160.0,
            stock = 10.0,
            unit = "pcs"
        )
        val oneItem = listOf(CalculationEngine.buildSaleItem(product = product, qty = 1.0))
        assertEquals(30.0, CalculationEngine.calculateSaleMaxProfitDiscount(oneItem), 0.001)
        assertEquals(30.0, CalculationEngine.calculateMaxInvoiceDiscount(oneItem), 0.001)
        assertFalse(CalculationEngine.isSaleDiscountOverProfit(oneItem, 0.0))
        assertFalse(CalculationEngine.isSaleDiscountOverProfit(oneItem, 30.0))
        assertTrue(CalculationEngine.isSaleDiscountOverProfit(oneItem, 30.01))
        assertTrue(CalculationEngine.isSaleDiscountOverProfit(oneItem, 31.0))

        val twoQty = listOf(CalculationEngine.buildSaleItem(product = product, qty = 2.0))
        assertEquals(60.0, CalculationEngine.calculateSaleMaxProfitDiscount(twoQty), 0.001)
        assertFalse(CalculationEngine.isSaleDiscountOverProfit(twoQty, 60.0))
        assertTrue(CalculationEngine.isSaleDiscountOverProfit(twoQty, 61.0))

        // Zero or negative profit: 0 discount allowed, any > 0 discount blocked
        val lossProduct = product.copy(buyPrice = 130.0, sellPrice = 120.0)
        val lossItems = listOf(CalculationEngine.buildSaleItem(product = lossProduct, qty = 1.0))
        assertEquals(0.0, CalculationEngine.calculateSaleMaxProfitDiscount(lossItems), 0.001)
        assertEquals(0.0, CalculationEngine.calculateMaxInvoiceDiscount(lossItems), 0.001)
        assertFalse(CalculationEngine.isSaleDiscountOverProfit(lossItems, 0.0))
        assertTrue(CalculationEngine.isSaleDiscountOverProfit(lossItems, 1.0))
    }

    @Test
    fun repositoryAndViewModel_blockDiscountAboveProfit_andAllowUpToProfit() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = StoreRepository(db = null, appContext = context).apply {
            configureDemoStore(ShopType.GROCERY)
        }
        val prodId = repo.addProduct(
            "demo_store",
            Product(
                name = "Test Oil",
                category = "Groceries",
                buyPrice = 130.0,
                sellPrice = 160.0,
                stock = 5.0,
                unit = "ltr"
            ),
            PhotoStorageMode.SYNC
        ).productId
        val prod = repo.observeProducts("demo_store").first().first { it.id == prodId }
        val item = CalculationEngine.buildSaleItem(product = prod, qty = 1.0)
        val salesBeforeCount = repo.observeSales("demo_store").first().size

        // Discount 31 (> 30 profit) must be rejected without touching stock or sales
        var threwOverProfit = false
        try {
            repo.executeSaleCheckout(
                storeId = "demo_store",
                items = listOf(item),
                invoiceDiscount = 31.0,
                paymentType = "cash",
                clientSaleId = "over-profit-attempt-1"
            )
        } catch (e: IllegalArgumentException) {
            threwOverProfit = true
        }
        assertTrue("Repository must reject discount > profit", threwOverProfit)
        assertEquals(salesBeforeCount, repo.observeSales("demo_store").first().size)
        assertEquals(5.0, repo.observeProducts("demo_store").first().first { it.id == prodId }.stock, 0.001)

        // StoreViewModel.completeSale must also block discount > profit and set errorMessage
        val vm = StoreViewModel(
            storeId = "demo_store",
            repository = repo,
            networkMonitor = NetworkMonitor(context),
            currentUserId = "demo_user"
        )
        try {
            var succeeded = false
            vm.completeSale(
                items = listOf(item),
                invoiceDiscount = 31.0,
                paymentType = "cash",
                clientSaleId = "vm-over-profit-attempt"
            ) {
                succeeded = true
            }
            assertFalse("ViewModel must block discount > profit", succeeded)
            assertNotNull(vm.errorMessage.value)
            assertEquals(salesBeforeCount, repo.observeSales("demo_store").first().size)

            // Discount == 30 (exact profit) must succeed with 0 profit and 130 grandTotal
            val invoiceNo = repo.executeSaleCheckout(
                storeId = "demo_store",
                items = listOf(item),
                invoiceDiscount = 30.0,
                paymentType = "cash",
                clientSaleId = "exact-profit-attempt-1"
            )
            val savedSale = repo.observeSales("demo_store").first().first { it.invoiceNo == invoiceNo }
            assertEquals(130.0, savedSale.grandTotal, 0.01)
            assertEquals(30.0, savedSale.discount, 0.01)
            assertEquals(0.0, savedSale.profit, 0.01)
            assertEquals(4.0, repo.observeProducts("demo_store").first().first { it.id == prodId }.stock, 0.001)
        } finally {
            vm.dispose()
        }
    }

    @Test
    fun sellScreen_blocksDiscountExceedingProfit_andClearsWhenWithinProfit() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = StoreRepository(db = null, appContext = context).apply {
            configureDemoStore(ShopType.GROCERY)
        }
        val prodId = runBlocking {
            repo.addProduct(
                "demo_store",
                Product(
                    name = "AAA Cap Product",
                    category = "Groceries",
                    buyPrice = 130.0,
                    sellPrice = 160.0,
                    stock = 10.0,
                    unit = "pcs"
                ),
                PhotoStorageMode.SYNC
            ).productId
        }
        val storeVm = StoreViewModel(
            storeId = "demo_store",
            repository = repo,
            networkMonitor = NetworkMonitor(context),
            currentUserId = "demo_user"
        )
        storeVm.setLanguage(AppLanguage.EN)
        storeVm.setSimpleMode(false)

        try {
            composeRule.setContent {
                StoreOSTheme {
                    Box(modifier = Modifier.size(width = 400.dp, height = 860.dp)) {
                        SellScreen(storeViewModel = storeVm, onBack = {})
                    }
                }
            }
            composeRule.waitForIdle()

            // Add AAA Cap Product (buy=130, sell=160 -> profit=30) to cart and open Checkout
            composeRule.onNodeWithTag("sell_catalog_item_$prodId").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("sell_floating_checkout_bar").performClick()
            composeRule.waitForIdle()

            // Type discount = 31 (> 30 profit)
            composeRule.onNodeWithTag("sell_discount_input").performScrollTo()
            composeRule.onNodeWithTag("sell_discount_input").performTextReplacement("31")
            composeRule.waitForIdle()

            // Error banner must appear right under discount input and before slider
            composeRule.onNodeWithTag("sell_discount_error_text").assertIsDisplayed()
            composeRule.onNodeWithTag("sell_checkout_error_text").performScrollTo()
            composeRule.onNodeWithTag("sell_checkout_error_text").assertIsDisplayed()

            // Lower discount to 30 (== profit): errors must clear cleanly
            composeRule.onNodeWithTag("sell_discount_input").performScrollTo()
            composeRule.onNodeWithTag("sell_discount_input").performTextReplacement("30")
            composeRule.waitForIdle()

            assertEquals(0, composeRule.onAllNodesWithTag("sell_discount_error_text").fetchSemanticsNodes().size)
            assertEquals(0, composeRule.onAllNodesWithTag("sell_checkout_error_text").fetchSemanticsNodes().size)
        } finally {
            storeVm.dispose()
        }
    }

    @Test
    fun fatherModeNumpad_blocksDiscountExceedingProfit_andAllowsUpToProfit() {
        val product = Product(
            id = "p_father",
            storeId = "demo_store",
            name = "ধানের বীজ",
            buyPrice = 130.0,
            sellPrice = 160.0,
            stock = 10.0,
            unit = "pcs"
        )
        val builtItems = listOf(CalculationEngine.buildSaleItem(product = product, qty = 1.0))
        var appliedDiscount = 0.0

        composeRule.setContent {
            StoreOSTheme {
                Box(modifier = Modifier.size(width = 400.dp, height = 860.dp)) {
                    FatherSellStep3Content(
                        builtSaleItems = builtItems,
                        subtotal = 160.0,
                        totalDiscount = 0.0,
                        grandTotal = 160.0,
                        paymentType = "cash",
                        attachedCustomer = null,
                        partialPaidAmount = null,
                        displayError = null,
                        isBusy = false,
                        onApplyDiscount = { appliedDiscount = it },
                        onClearDiscount = { appliedDiscount = 0.0 },
                        onApplyPartialPaid = {},
                        onClearPartialPaid = {},
                        onPaymentTypeChange = {},
                        onOpenCustomerPicker = {},
                        onConfirmSale = {},
                        onClose = {},
                        canSeeProfit = true
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("father_btn_give_discount").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("father_numpad_dialog").assertIsDisplayed()

        // Tap 3 then 1 -> 31 exceeds 30 profit -> error shown and blocked
        composeRule.onNodeWithTag("numpad_key_3").performClick()
        composeRule.onNodeWithTag("numpad_key_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("father_numpad_error_text").assertIsDisplayed()

        // Tap 0 instead -> 30 equals profit -> confirm succeeds with 30.0
        composeRule.onNodeWithTag("numpad_key_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("father_numpad_confirm_btn").performClick()
        composeRule.waitForIdle()

        assertEquals(30.0, appliedDiscount, 0.001)
    }

    @Test
    fun fatherStep3_disablesConfirmButtonIfExistingDiscountExceedsProfit() {
        val product = Product(
            id = "p_father_guard",
            storeId = "demo_store",
            name = "ধানের বীজ",
            buyPrice = 130.0,
            sellPrice = 160.0,
            stock = 10.0,
            unit = "pcs"
        )
        val builtItems = listOf(CalculationEngine.buildSaleItem(product = product, qty = 1.0))

        composeRule.setContent {
            StoreOSTheme {
                Box(modifier = Modifier.size(width = 400.dp, height = 860.dp)) {
                    FatherSellStep3Content(
                        builtSaleItems = builtItems,
                        subtotal = 160.0,
                        totalDiscount = 35.0,
                        grandTotal = 125.0,
                        paymentType = "cash",
                        attachedCustomer = null,
                        partialPaidAmount = null,
                        displayError = null,
                        isBusy = false,
                        onApplyDiscount = {},
                        onClearDiscount = {},
                        onApplyPartialPaid = {},
                        onClearPartialPaid = {},
                        onPaymentTypeChange = {},
                        onOpenCustomerPicker = {},
                        onConfirmSale = {},
                        onClose = {},
                        canSeeProfit = true
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("father_step3_error_text").assertIsDisplayed()
        composeRule.onNodeWithTag("complete_sale_button").assertIsNotEnabled()
    }
}
