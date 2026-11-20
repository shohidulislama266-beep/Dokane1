package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Product
import com.example.data.repository.StoreRepository
import com.example.domain.BakiOverview
import com.example.domain.HomeInsights
import com.example.domain.StoreFinancials
import com.example.ui.components.ShopNavTab
import com.example.ui.screens.AddProductWizardDialog
import com.example.ui.screens.MainShopScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimpleHomeLayout
import com.example.ui.screens.StockScreen
import com.example.ui.theme.StoreOSTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BusinessNavDestination
import com.example.ui.viewmodel.SimpleModeRouteGuard
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.NetworkMonitor
import com.example.util.StockPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SimpleModeSecurityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun clearPrefs() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("storeos_stock_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("storeos_app_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun createViewModels(): Pair<AuthViewModel, StoreViewModel> {
        val repository = StoreRepository(db = null, appContext = context)
        val authViewModel = AuthViewModel(repository)
        val storeViewModel = StoreViewModel(
            storeId = "demo_store",
            repository = repository,
            networkMonitor = NetworkMonitor(context),
            currentUserId = "demo_user"
        )
        return authViewModel to storeViewModel
    }

    @Test
    fun `route guard blocks every hidden BusinessNavDestination and restricted tab in Simple Mode`() {
        val (_, storeViewModel) = createViewModels()
        storeViewModel.setSimpleMode(true)
        assertTrue(storeViewModel.isSimpleMode.value)

        // Blocked top-level tabs
        listOf(ShopNavTab.BUSINESS, ShopNavTab.MORE, ShopNavTab.HISTORY).forEach { tab ->
            storeViewModel.navigateToTab(tab)
            assertEquals("Blocked tab $tab should land on HOME", ShopNavTab.HOME, storeViewModel.currentTab.value)
        }

        // Allowed Simple Mode tabs still work
        listOf(ShopNavTab.SELL, ShopNavTab.BAKI, ShopNavTab.STOCK, ShopNavTab.HOME).forEach { tab ->
            storeViewModel.navigateToTab(tab)
            assertEquals("Allowed tab $tab should navigate", tab, storeViewModel.currentTab.value)
        }

        // Settings blocked in Simple Mode
        storeViewModel.openSettings()
        assertFalse(storeViewModel.showSettings.value)
        assertEquals(ShopNavTab.HOME, storeViewModel.currentTab.value)

        // Every BusinessNavDestination (Staff, Categories, Cash & Bank, Reports, Monthly Report, Backup, Data Health, etc.) is blocked
        BusinessNavDestination.entries.forEach { dest ->
            storeViewModel.openSubDestination(ShopNavTab.BUSINESS, dest)
            assertEquals("openSubDestination(BUSINESS, $dest) must return HOME", ShopNavTab.HOME, storeViewModel.currentTab.value)
            assertEquals(BusinessNavDestination.DASHBOARD, storeViewModel.businessNavDestination.value)

            storeViewModel.openSubDestination(ShopNavTab.MORE, dest)
            assertEquals("openSubDestination(MORE, $dest) must return HOME", ShopNavTab.HOME, storeViewModel.currentTab.value)
            assertEquals(BusinessNavDestination.DASHBOARD, storeViewModel.moreNavDestination.value)

            storeViewModel.setActiveSubNavTab(ShopNavTab.BUSINESS)
            storeViewModel.navigateInBusiness(dest)
            assertEquals(BusinessNavDestination.DASHBOARD, storeViewModel.businessNavDestination.value)
            assertEquals(ShopNavTab.HOME, storeViewModel.currentTab.value)
        }
    }

    @Test
    fun `notification intents and deep links to hidden screens are blocked in Simple Mode`() {
        val (_, storeViewModel) = createViewModels()
        storeViewModel.setSimpleMode(true)

        val blockedIntents = listOf(
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_OPEN_SETTINGS, true),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_OPEN_AI_KEY, true),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_OPEN_BACKUP, true),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_NAV_TAB, ShopNavTab.BUSINESS.name),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_NAV_TAB, ShopNavTab.MORE.name),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_NAV_TAB, ShopNavTab.HISTORY.name),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_NAV_DESTINATION, BusinessNavDestination.STAFF_MANAGEMENT_SCREEN.name),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_NAV_DESTINATION, BusinessNavDestination.MANAGE_CATEGORIES_SCREEN.name),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_NAV_DESTINATION, BusinessNavDestination.CASH_BANK_SCREEN.name),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_NAV_DESTINATION, BusinessNavDestination.FINANCIAL_REPORTS_HUB.name),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_NAV_DESTINATION, BusinessNavDestination.MONTHLY_REPORT.name),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_NAV_DESTINATION, BusinessNavDestination.DATA_HEALTH_CHECK.name),
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_NAV_DESTINATION, BusinessNavDestination.TEST_CHECKLIST.name),
            Intent(Intent.ACTION_VIEW, Uri.parse("storeos://app/staff")),
            Intent(Intent.ACTION_VIEW, Uri.parse("storeos://app/categories")),
            Intent(Intent.ACTION_VIEW, Uri.parse("storeos://app/cash-bank")),
            Intent(Intent.ACTION_VIEW, Uri.parse("storeos://app/reports")),
            Intent(Intent.ACTION_VIEW, Uri.parse("storeos://app/monthly-report")),
            Intent(Intent.ACTION_VIEW, Uri.parse("storeos://app/ai-key")),
            Intent(Intent.ACTION_VIEW, Uri.parse("storeos://app/backup")),
            Intent(Intent.ACTION_VIEW, Uri.parse("storeos://app/data-health")),
            Intent(Intent.ACTION_VIEW, Uri.parse("storeos://app/settings")),
            Intent(Intent.ACTION_VIEW, Uri.parse("storeos://app/business"))
        )

        blockedIntents.forEachIndexed { idx, intent ->
            storeViewModel.navigateToTab(ShopNavTab.STOCK)
            storeViewModel.handleAppIntent(intent)
            assertEquals("Blocked intent #$idx must return to HOME", ShopNavTab.HOME, storeViewModel.currentTab.value)
            assertFalse("Blocked intent #$idx must not open Settings", storeViewModel.showSettings.value)
            assertEquals(BusinessNavDestination.DASHBOARD, storeViewModel.businessNavDestination.value)
            assertEquals(BusinessNavDestination.DASHBOARD, storeViewModel.moreNavDestination.value)
        }

        // Allowed notification taps go to Simple Mode tabs directly
        storeViewModel.handleAppIntent(Intent().putExtra(SimpleModeRouteGuard.EXTRA_OPEN_LOW_STOCK, true))
        assertEquals(ShopNavTab.STOCK, storeViewModel.currentTab.value)
        assertTrue(storeViewModel.stockFilterLowOnlyTrigger.value)

        storeViewModel.handleAppIntent(Intent().putExtra(SimpleModeRouteGuard.EXTRA_OPEN_BAKI, true))
        assertEquals(ShopNavTab.BAKI, storeViewModel.currentTab.value)
    }

    @Test
    fun `back navigation and switching to Simple Mode sanitizes stale back stacks`() {
        val (_, storeViewModel) = createViewModels()

        // Start in Detailed Mode and push hidden Business/More screens
        storeViewModel.setSimpleMode(false)
        storeViewModel.openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.FINANCIAL_REPORTS_HUB)
        storeViewModel.navigateInBusiness(BusinessNavDestination.MONTHLY_REPORT)
        assertEquals(ShopNavTab.BUSINESS, storeViewModel.currentTab.value)
        assertEquals(BusinessNavDestination.MONTHLY_REPORT, storeViewModel.businessNavDestination.value)

        // Switching into Simple Mode clears stale stack & lands on HOME
        storeViewModel.setSimpleMode(true)
        assertEquals(ShopNavTab.HOME, storeViewModel.currentTab.value)
        assertEquals(BusinessNavDestination.DASHBOARD, storeViewModel.businessNavDestination.value)

        // Navigate to STOCK in Simple Mode, then press back -> lands on HOME
        storeViewModel.navigateToTab(ShopNavTab.STOCK)
        assertEquals(ShopNavTab.STOCK, storeViewModel.currentTab.value)
        assertTrue(storeViewModel.handleBackNavigation())
        assertEquals(ShopNavTab.HOME, storeViewModel.currentTab.value)

        // Calling navigateBackInBusiness in Simple Mode never reveals a hidden screen
        storeViewModel.navigateToTab(ShopNavTab.BAKI)
        storeViewModel.navigateBackInBusiness()
        assertEquals(ShopNavTab.HOME, storeViewModel.currentTab.value)
        assertEquals(BusinessNavDestination.DASHBOARD, storeViewModel.businessNavDestination.value)
    }

    @Test
    fun `five wrong owner PIN attempts trigger 30 second lockout and unlock after expiry`() {
        var nowMs = 1_000_000L
        val prefs = StockPreferences(context) { nowMs }
        prefs.setOwnerPin("2580")

        repeat(4) { attempt ->
            val res = prefs.verifyOwnerPinWithLockout("0000")
            assertFalse(res.success)
            assertFalse("Attempt ${attempt + 1} should not lock yet", res.isLocked)
            assertEquals(attempt + 1, res.failedAttempts)
        }

        // 5th wrong attempt triggers 30-second lock
        val lockedRes = prefs.verifyOwnerPinWithLockout("0000")
        assertFalse(lockedRes.success)
        assertTrue(lockedRes.isLocked)
        assertEquals(30, lockedRes.remainingLockSeconds)

        // Even the right PIN is blocked during lockout
        nowMs += 10_000L
        val midLock = prefs.verifyOwnerPinWithLockout("2580")
        assertFalse(midLock.success)
        assertTrue(midLock.isLocked)
        assertEquals(20, midLock.remainingLockSeconds)

        // After 30s passes, lockout clears and right PIN succeeds
        nowMs += 20_000L
        assertEquals(0, prefs.getRemainingPinLockoutSeconds())
        val unlocked = prefs.verifyOwnerPinWithLockout("2580")
        assertTrue(unlocked.success)
        assertFalse(unlocked.isLocked)
    }

    @Test
    fun `leaving Simple Mode without PIN prompts setup in Bengali then switches after saving`() {
        val (authViewModel, storeViewModel) = createViewModels()
        storeViewModel.setLanguage(AppLanguage.BN)
        storeViewModel.setSimpleMode(true)

        composeRule.setContent {
            StoreOSTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalAppLanguage provides AppLanguage.BN) {
                    MainShopScreen(authViewModel = authViewModel, storeViewModel = storeViewModel)
                }
            }
        }
        composeRule.waitForIdle()

        // Tap "বিস্তারিত মোডে যান"
        composeRule.onNodeWithTag("top_bar_detailed_mode_link").performClick()
        composeRule.waitForIdle()

        // Must show "আগে মালিকের পিন সেট করুন" and SET_PIN fields
        composeRule.onNodeWithTag("backup_pin_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("আগে মালিকের পিন সেট করুন").assertIsDisplayed()

        // Save new PIN -> exits Simple Mode automatically
        composeRule.onNodeWithTag("backup_pin_input").performTextInput("4321")
        composeRule.onNodeWithTag("backup_pin_confirm_input").performTextInput("4321")
        composeRule.onNodeWithTag("backup_pin_confirm_btn").performClick()
        composeRule.waitForIdle()

        assertTrue(storeViewModel.hasOwnerPin.value)
        assertFalse("Should switch to Detailed Mode after PIN setup", storeViewModel.isSimpleMode.value)
    }

    @Test
    fun `leaving Simple Mode with PIN locks after 5 wrong attempts and shows Bengali lockout message`() {
        val (authViewModel, storeViewModel) = createViewModels()
        storeViewModel.setOwnerPin("5678")
        storeViewModel.setSimpleMode(true)

        composeRule.setContent {
            StoreOSTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalAppLanguage provides AppLanguage.BN) {
                    MainShopScreen(authViewModel = authViewModel, storeViewModel = storeViewModel)
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("top_bar_detailed_mode_link").performClick()
        composeRule.waitForIdle()

        // Enter wrong PIN 5 times
        repeat(5) {
            composeRule.onNodeWithTag("backup_pin_input").performTextInput("1111")
            composeRule.onNodeWithTag("backup_pin_confirm_btn").performClick()
            composeRule.waitForIdle()
            if (it < 4) {
                assertTrue("Must remain in Simple Mode after failed PIN", storeViewModel.isSimpleMode.value)
                // Clear previous 4 digits using replacement by dismissing & reopening or verifying directly
                composeRule.onNodeWithTag("backup_pin_cancel_btn").performClick()
                composeRule.waitForIdle()
                composeRule.onNodeWithTag("top_bar_detailed_mode_link").performClick()
                composeRule.waitForIdle()
            }
        }

        // Locked: Bengali error message visible and confirm button disabled
        composeRule.onNodeWithTag("backup_pin_error_text").assertIsDisplayed()
        composeRule.onNodeWithText("৫ বার ভুল পিন দেওয়া হয়েছে", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("backup_pin_confirm_btn").assertIsNotEnabled()
        assertTrue(storeViewModel.isSimpleMode.value)
    }

    @Test
    fun `notifications dialog taps route only to Stock or Baki in Simple Mode`() {
        val (authViewModel, storeViewModel) = createViewModels()
        storeViewModel.setSimpleMode(true)

        composeRule.setContent {
            StoreOSTheme {
                MainShopScreen(authViewModel = authViewModel, storeViewModel = storeViewModel)
            }
        }
        composeRule.waitForIdle()

        // Open Bell alerts and tap "Open Baki" / "বাকি খাতা"
        composeRule.onNodeWithTag("top_bar_notifications_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_notifications_dialog").assertIsDisplayed()

        composeRule.onNodeWithText("Open Baki").performClick()
        composeRule.waitForIdle()

        assertEquals(ShopNavTab.BAKI, storeViewModel.currentTab.value)
        assertEquals(BusinessNavDestination.DASHBOARD, storeViewModel.businessNavDestination.value)
    }

    @Test
    fun `Simple Mode top bar has working language chip`() {
        val (authViewModel, storeViewModel) = createViewModels()
        storeViewModel.setLanguage(AppLanguage.EN)
        storeViewModel.setSimpleMode(true)

        composeRule.setContent {
            StoreOSTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalAppLanguage provides storeViewModel.currentLanguage.value) {
                    MainShopScreen(authViewModel = authViewModel, storeViewModel = storeViewModel)
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals(AppLanguage.EN, storeViewModel.currentLanguage.value)
        composeRule.onNodeWithTag("top_bar_language_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("top_bar_language_chip").performClick()
        composeRule.waitForIdle()

        assertEquals(AppLanguage.BN, storeViewModel.currentLanguage.value)
    }

    @Test
    fun `showProfitInSimpleMode defaults off and hides profit on Simple Home while Detailed Settings toggles it`() {
        val (authViewModel, storeViewModel) = createViewModels()
        assertFalse("showProfitInSimpleMode must default to false", storeViewModel.showProfitInSimpleMode.value)

        composeRule.setContent {
            StoreOSTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalAppLanguage provides AppLanguage.BN) {
                    SimpleHomeLayout(
                        financials = StoreFinancials(
                            todaySales = 1200.0,
                            todayProfit = 350.0,
                            todayOrdersCount = 4L,
                            todayItemsSold = 12.0,
                            stockValueAtCost = 5000.0,
                            cashBalance = 2000.0
                        ),
                        homeInsights = HomeInsights(0.0, null, emptyList()),
                        cashInHand = 2000.0,
                        bakiOverview = BakiOverview(0.0, 0L, emptyList()),
                        lowStockProducts = emptyList(),
                        currentLanguage = AppLanguage.BN,
                        isPrivacyHidden = false,
                        canSell = true,
                        canViewStock = true,
                        canAddEditProducts = true,
                        canAccessBaki = true,
                        canReceivePayment = true,
                        canSeeProfit = true,
                        canSeeSalesSummary = true,
                        onNavigateToSell = {},
                        onQuickPaymentIn = {},
                        onAddProduct = {},
                        onNavigateToBaki = {},
                        onOpenLowStock = {},
                        showProfitInSimpleMode = false
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToKey("simple_todays_accounts_card")
        composeRule.waitForIdle()

        // Cash in hand shown, Today's Profit hidden when default false
        composeRule.onNodeWithTag("simple_cash_in_hand_value").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("simple_todays_profit_value").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("আজকের লাভ").fetchSemanticsNodes().size)
    }

    @Test
    fun `showProfitInSimpleMode when enabled displays Today's Profit on Simple Home`() {
        composeRule.setContent {
            StoreOSTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalAppLanguage provides AppLanguage.BN) {
                    SimpleHomeLayout(
                        financials = StoreFinancials(
                            todaySales = 1200.0,
                            todayProfit = 350.0,
                            todayOrdersCount = 4L,
                            todayItemsSold = 12.0,
                            stockValueAtCost = 5000.0,
                            cashBalance = 2000.0
                        ),
                        homeInsights = HomeInsights(0.0, null, emptyList()),
                        cashInHand = 2000.0,
                        bakiOverview = BakiOverview(0.0, 0L, emptyList()),
                        lowStockProducts = emptyList(),
                        currentLanguage = AppLanguage.BN,
                        isPrivacyHidden = false,
                        canSell = true,
                        canViewStock = true,
                        canAddEditProducts = true,
                        canAccessBaki = true,
                        canReceivePayment = true,
                        canSeeProfit = true,
                        canSeeSalesSummary = true,
                        onNavigateToSell = {},
                        onQuickPaymentIn = {},
                        onAddProduct = {},
                        onNavigateToBaki = {},
                        onOpenLowStock = {},
                        showProfitInSimpleMode = true
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToKey("simple_todays_accounts_card")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("simple_cash_in_hand_value").assertIsDisplayed()
        composeRule.onNodeWithTag("simple_todays_profit_value").assertIsDisplayed()
        composeRule.onNodeWithText("আজকের লাভ").assertIsDisplayed()
    }

    @Test
    fun `Detailed Mode Settings shows Simple Mode Profit toggle and updates preference`() {
        val (authViewModel, storeViewModel) = createViewModels()
        storeViewModel.setLanguage(AppLanguage.BN)
        storeViewModel.setSimpleMode(false)

        composeRule.setContent {
            StoreOSTheme {
                SettingsScreen(
                    authViewModel = authViewModel,
                    storeViewModel = storeViewModel,
                    onBack = {},
                    onLogout = {},
                    onNavigateDestination = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_simple_mode_profit_switch").performScrollTo()
        composeRule.onNodeWithText("সহজ মোডে লাভ দেখান").assertIsDisplayed()
        assertFalse(storeViewModel.showProfitInSimpleMode.value)

        composeRule.onNodeWithTag("settings_simple_mode_profit_switch").performClick()
        composeRule.waitForIdle()
        assertTrue(storeViewModel.showProfitInSimpleMode.value)
    }

    @Test
    fun `Simple Mode hides stock value at cost and buy prices on StockScreen and AddProductWizardDialog`() {
        val (_, storeViewModel) = createViewModels()
        storeViewModel.setLanguage(AppLanguage.EN)
        storeViewModel.setSimpleMode(true)

        composeRule.setContent {
            StoreOSTheme {
                StockScreen(storeViewModel = storeViewModel)
            }
        }
        composeRule.waitForIdle()

        // STOCK VALUE KPI card and "Buy" price on cards must not appear in Simple Mode
        assertEquals(0, composeRule.onAllNodesWithText("STOCK VALUE").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Buy", substring = true).fetchSemanticsNodes().size)

        // Switching to Detailed Mode restores STOCK VALUE KPI
        composeRule.runOnUiThread { storeViewModel.setSimpleMode(false) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("STOCK VALUE").assertIsDisplayed()
    }

    @Test
    fun `AddProductWizardDialog in Simple Mode hides buy price and profit card and saves with preserved cost`() {
        var savedBuyPrice = -1.0
        var savedSellPrice = -1.0
        val existingProduct = Product(
            id = "p1",
            name = "Sugar",
            unit = "kg",
            buyPrice = 95.0,
            sellPrice = 120.0,
            stock = 10.0,
            lowStockLimit = 5.0
        )

        composeRule.setContent {
            StoreOSTheme {
                AddProductWizardDialog(
                    productToEdit = existingProduct,
                    currentLanguage = AppLanguage.EN,
                    isBusy = false,
                    canSeeBuyingPrice = true,
                    isSimpleMode = true,
                    isFatherMode = false,
                    onDismiss = {},
                    onConfirm = { _, _, _, _, buy, sell, _, _, _, _, _, _, _, _, _, _, _ ->
                        savedBuyPrice = buy
                        savedSellPrice = sell
                    }
                )
            }
        }
        composeRule.waitForIdle()

        // Step 1 -> Step 2 (Pricing)
        composeRule.onNodeWithTag("wizard_next_btn").performClick()
        composeRule.waitForIdle()

        // Buy price input and profit hero card must be hidden in Simple Mode
        assertEquals(0, composeRule.onAllNodesWithTag("wizard_buy_price").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("wizard_profit_hero_card").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("wizard_sell_price").assertIsDisplayed()

        // Step 2 -> Step 3 (Review) -> Save works without requiring hidden buy price and preserves 95.0
        composeRule.onNodeWithTag("wizard_next_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_save_product_btn").performClick()
        composeRule.waitForIdle()

        assertEquals(95.0, savedBuyPrice, 0.001)
        assertEquals(120.0, savedSellPrice, 0.001)
    }
}
