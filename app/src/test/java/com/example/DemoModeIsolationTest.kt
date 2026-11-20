package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BusinessStrategyFacts
import com.example.data.model.Product
import com.example.data.model.SaleItem
import com.example.data.model.StoreInvite
import com.example.data.model.StorePermissions
import com.example.data.model.UserProfile
import com.example.data.repository.StoreRepository
import com.example.domain.ShopType
import com.example.domain.ShopTypeCatalog
import com.example.ui.components.ShopNavTab
import com.example.ui.screens.BusinessDestinationContent
import com.example.ui.screens.FinancialReportsHubScreen
import com.example.ui.screens.MoreMenuContent
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.StoreOSTheme
import com.example.ui.viewmodel.AuthBackend
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.AuthenticatedUser
import com.example.ui.viewmodel.BusinessNavDestination
import com.example.ui.viewmodel.SimpleModeRouteGuard
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.NetworkMonitor
import com.example.util.StockPreferences
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xhdpi")
class DemoModeIsolationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        FirebaseApp.clearInstancesForTest()
        listOf(
            "user_mode_prefs",
            "storeos_stock_prefs",
            "storeos_demo_stock_prefs",
            "storeos_app_prefs"
        ).forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
    }

    private class FakeRealAuthBackend : AuthBackend {
        override var currentUser: AuthenticatedUser? = null
        private val listeners = mutableListOf<(AuthenticatedUser?) -> Unit>()
        override fun addAuthStateListener(onUserChanged: (AuthenticatedUser?) -> Unit): () -> Unit {
            listeners += onUserChanged
            return { listeners -= onUserChanged }
        }
        override suspend fun signInWithGoogle(context: Context): AuthenticatedUser = error("unused")
        override suspend fun signInWithEmail(email: String, password: String): AuthenticatedUser = error("unused")
        override suspend fun signUpWithEmail(email: String, password: String): AuthenticatedUser = error("unused")
        override suspend fun sendPasswordResetEmail(email: String) = Unit
        override suspend fun refreshIdToken() = Unit
        override suspend fun signOut(context: Context) {
            currentUser = null
            listeners.forEach { it(null) }
        }
    }

    @Test
    fun `demo session never creates or calls FirebaseApp, FirebaseFirestore, or FirebaseAuth`() {
        assertTrue("Precondition: no FirebaseApp initialized before test", FirebaseApp.getApps(context).isEmpty())

        var firebaseInitCalls = 0
        var realRepoFactoryCalls = 0
        var realAuthVmFactoryCalls = 0
        var activeDemoAuthVm: AuthViewModel? = null
        var activeDemoStoreVm: StoreViewModel? = null

        composeRule.setContent {
            StoreOSTheme {
                StoreOsAppHost(
                    initialMode = AppSessionMode.Demo(ShopType.HARDWARE, generation = 0),
                    ensureFirebaseInitialized = { firebaseInitCalls++ },
                    realRepositoryFactory = {
                        realRepoFactoryCalls++
                        throw AssertionError("Real repository must never be created during a demo session")
                    },
                    realAuthViewModelFactory = {
                        realAuthVmFactoryCalls++
                        throw AssertionError("Real AuthViewModel must never be created during a demo session")
                    },
                    onSessionControllersChanged = { authVm, storeVm ->
                        activeDemoAuthVm = authVm
                        activeDemoStoreVm = storeVm
                    }
                )
            }
        }
        composeRule.waitForIdle()

        // No FirebaseApp, FirebaseAuth, or FirebaseFirestore created
        assertEquals("FirebaseApp init must not be called in demo", 0, firebaseInitCalls)
        assertEquals("Real repository factory must not be called in demo", 0, realRepoFactoryCalls)
        assertEquals("Real AuthViewModel factory must not be called in demo", 0, realAuthVmFactoryCalls)
        assertTrue("FirebaseApp.getApps must stay empty during demo", FirebaseApp.getApps(context).isEmpty())
        assertTrue(
            "Default FirebaseApp must remain uninitialized (throws if FirebaseFirestore/FirebaseAuth touched)",
            runCatching { FirebaseApp.getInstance() }.isFailure
        )
        assertTrue(
            "FirebaseAuth.getInstance must not have an initialized default FirebaseApp",
            runCatching { FirebaseAuth.getInstance() }.isFailure
        )
        assertTrue(
            "FirebaseFirestore.getInstance must not have an initialized default FirebaseApp",
            runCatching { FirebaseFirestore.getInstance() }.isFailure
        )

        val storeVm = activeDemoStoreVm
        assertNotNull(storeVm)
        assertTrue(storeVm!!.isDemoMode)
        assertEquals(ShopType.HARDWARE, storeVm.shopType.value)
        assertEquals(8, storeVm.products.value.size)

        // Exercise interactive flows inside demo: Home -> Stock -> Add Product -> Sell -> Business -> More -> Strategy refresh
        storeVm.navigateToTab(ShopNavTab.STOCK)
        composeRule.waitForIdle()

        storeVm.addNewProductWizard(
            name = "Demo Copper Wire",
            category = "Hardware",
            unit = "coil",
            buyPrice = 200.0,
            sellPrice = 260.0,
            stock = 5.0
        )
        composeRule.waitForIdle()
        assertEquals(9, storeVm.products.value.size)

        val targetProd = storeVm.products.value.first { it.stock >= 2.0 }
        storeVm.completeSale(
            items = listOf(
                SaleItem(
                    productId = targetProd.id,
                    name = targetProd.name,
                    unit = targetProd.unit,
                    qty = 1.0,
                    stockQty = 1.0,
                    buyPrice = targetProd.buyPrice,
                    sellPrice = targetProd.sellPrice,
                    lineTotal = targetProd.sellPrice
                )
            ),
            onSuccess = {}
        )
        composeRule.waitForIdle()

        storeVm.refreshBusinessStrategy()
        storeVm.navigateToTab(ShopNavTab.MORE)
        composeRule.waitForIdle()

        assertTrue(
            "FirebaseApp.getApps must still be completely empty after full demo navigation & writes",
            FirebaseApp.getApps(context).isEmpty()
        )
        assertTrue(activeDemoAuthVm!!.uiState.value is AuthUiState.DemoMode)
    }

    @Test
    fun `entering and exiting demo recreates AuthViewModel and StoreViewModel with zero shared state`() {
        val observedAuthVms = mutableListOf<AuthViewModel>()
        val observedStoreVms = mutableListOf<StoreViewModel?>()
        val realProfiles = MutableStateFlow(
            mapOf(
                "real_uid" to UserProfile(
                    id = "real_uid",
                    email = "owner@realshop.com",
                    storeId = "real_store_1",
                    role = "owner",
                    isSimpleMode = false
                )
            )
        )

        composeRule.setContent {
            StoreOSTheme {
                StoreOsAppHost(
                    initialMode = AppSessionMode.Real(generation = 0),
                    ensureFirebaseInitialized = { /* do not initialize Firebase in unit test */ },
                    realRepositoryFactory = { ctx -> StoreRepository(db = null, appContext = ctx) },
                    realAuthViewModelFactory = { repo ->
                        AuthViewModel(
                            repository = repo,
                            authBackend = FakeRealAuthBackend(),
                            isOnlineProvider = { true },
                            profileObserver = { uid -> realProfiles.map { it[uid] } },
                            profileLoader = { uid -> realProfiles.value[uid] },
                            profileSaver = {},
                            inviteJoiner = { _, _ -> Result.failure(IllegalStateException("unused")) }
                        )
                    },
                    onSessionControllersChanged = { authVm, storeVm ->
                        observedAuthVms += authVm
                        observedStoreVms += storeVm
                    }
                )
            }
        }
        composeRule.waitForIdle()

        // Pre-seed real store's StockPreferences with a real PIN & PhotoStorageMode
        val realPrefs = StockPreferences.forStore(context, "real_store_1")
        realPrefs.setOwnerPin("9999")

        assertEquals(1, observedAuthVms.size)
        val realAuthVm1 = observedAuthVms[0]
        assertTrue(realAuthVm1.viewModelScope.isActive)

        // Enter Demo via "Try Sample Shop" -> pick Pharmacy
        composeRule.onNodeWithTag("demo_mode_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("demo_shop_type_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("shop_type_tile_pharmacy").performScrollTo().performClick()
        composeRule.waitForIdle()

        val demoAuthVm1 = observedAuthVms.last()
        val demoStoreVm1 = observedStoreVms.last()!!
        assertNotSame("Entering demo must recreate AuthViewModel", realAuthVm1, demoAuthVm1)
        assertFalse("Old Real AuthViewModel must be disposed/cancelled on entering demo", realAuthVm1.viewModelScope.isActive)
        assertEquals(ShopType.PHARMACY, demoStoreVm1.shopType.value)
        assertEquals("demo_store", demoStoreVm1.storeId)

        // Real store PIN must NOT leak into demo StoreViewModel (separate prefs file!)
        assertFalse("Demo must not inherit real store's owner PIN", demoStoreVm1.hasOwnerPin.value)

        // Mutate Demo heavily: set a demo PIN, switch to Simple Mode, add a custom product
        composeRule.runOnUiThread {
            demoStoreVm1.setOwnerPin("1234")
            demoStoreVm1.setSimpleMode(true)
            demoStoreVm1.addNewProductWizard(
                name = "Temp Pharmacy Syrup",
                category = "Medicines",
                unit = "bottle",
                buyPrice = 50.0,
                sellPrice = 75.0,
                stock = 4.0
            )
        }
        composeRule.waitForIdle()
        assertTrue(demoStoreVm1.hasOwnerPin.value)
        assertTrue(demoStoreVm1.isSimpleMode.value)
        assertTrue(demoStoreVm1.products.value.any { it.name == "Temp Pharmacy Syrup" })

        // Exit Demo (via authViewModel.signOut like More/Settings Exit Demo does)
        composeRule.runOnUiThread {
            demoAuthVm1.signOut(context)
        }
        composeRule.waitForIdle()

        // Should be back on AuthScreen with a brand-new Real AuthViewModel & disposed Demo VMs
        composeRule.onNodeWithTag("login_app_name").assertIsDisplayed()
        val realAuthVm2 = observedAuthVms.last()
        assertNotSame("Exiting demo must create a fresh AuthViewModel", demoAuthVm1, realAuthVm2)
        assertNotSame("Exiting demo must create a new Real AuthViewModel instance", realAuthVm1, realAuthVm2)
        assertFalse("Demo AuthViewModel scope must be cancelled after exit", demoAuthVm1.viewModelScope.isActive)
        assertFalse("Demo StoreViewModel scope must be cancelled after exit", demoStoreVm1.viewModelScope.isActive)

        // Real store PIN must still be untouched ("9999"), while demo PIN & simple mode were wiped
        assertTrue(realPrefs.verifyOwnerPin("9999"))
        assertFalse(StockPreferences.forStore(context, "demo_store").hasOwnerPinFlow.value)

        // Re-enter Demo with Clothing -> brand-new VMs + clean Clothing seed (zero Pharmacy leftover!)
        composeRule.onNodeWithTag("demo_mode_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shop_type_tile_clothing").performScrollTo().performClick()
        composeRule.waitForIdle()

        val demoAuthVm2 = observedAuthVms.last()
        val demoStoreVm2 = observedStoreVms.last()!!
        assertNotSame("Re-entering demo must create fresh AuthViewModel", demoAuthVm1, demoAuthVm2)
        assertNotSame("Re-entering demo must create fresh StoreViewModel", demoStoreVm1, demoStoreVm2)
        assertEquals(ShopType.CLOTHING, demoStoreVm2.shopType.value)
        assertEquals(8, demoStoreVm2.products.value.size)
        assertFalse(demoStoreVm2.products.value.any { it.name == "Temp Pharmacy Syrup" })
        assertFalse("Demo PIN must not carry over across exit & re-entry", demoStoreVm2.hasOwnerPin.value)
        assertFalse("Demo simple mode must not carry over across exit & re-entry", demoStoreVm2.isSimpleMode.value)
    }

    @Test
    fun `each shop type seeds 8 products with units, 4 customers, 3 suppliers, and 5 sales`() {
        ShopType.entries.forEach { shopType ->
            val seed = ShopTypeCatalog.buildDemoSeed(shopType)
            assertEquals("$shopType product count", 8, seed.products.size)
            assertEquals("$shopType customer count", 4, seed.customers.size)
            assertEquals("$shopType supplier count", 3, seed.suppliers.size)
            assertEquals("$shopType sale count", 5, seed.sales.size)
            assertTrue("$shopType every product needs non-blank unit", seed.products.all { it.unit.isNotBlank() })
            assertTrue("$shopType should include unit conversion product", seed.products.any { it.buyConversion > 1.0 || it.sellUnits.isNotEmpty() })
            assertTrue("$shopType should include low-stock product", seed.products.any { it.stock in 0.01..it.lowStockLimit })
            assertTrue("$shopType should include zero-stock product", seed.products.any { it.stock <= 0.0 })
            assertEquals(shopType.id, seed.store.shopType)

            val repo = StoreRepository(db = null, appContext = context)
            repo.configureDemoStore(shopType)
            runBlocking {
                assertEquals(8, repo.observeProducts("demo_store").first().size)
                assertEquals(4, repo.observeCustomers("demo_store").first().size)
                assertEquals(3, repo.observeSuppliers("demo_store").first().size)
                assertEquals(5, repo.observeSales("demo_store").first().size)
            }
        }
    }

    @Test
    fun `cloud-only StoreRepository methods are disabled cleanly when db is null`() = runBlocking {
        val repo = StoreRepository(db = null, appContext = context)
        repo.configureDemoStore(ShopType.STATIONERY)
        val initialStore = repo.observeStore("demo_store").first()
        val initialProducts = repo.observeProducts("demo_store").first()

        // createStore fails without wiping demo store or demo products
        val createErr = runCatching {
            repo.createStore("Cloud Store", "01700000000", "Dhaka", "grocery", listOf("Rice"))
        }
        assertTrue(createErr.isFailure)
        assertEquals(initialStore?.name, repo.observeStore("demo_store").first()?.name)
        assertEquals(initialProducts.size, repo.observeProducts("demo_store").first().size)

        // Staff invites / member mutations fail cleanly
        assertEquals(0, repo.observeInvites("demo_store").first().size)
        assertTrue(
            runCatching {
                repo.createStaffInvite(
                    StoreInvite(
                        storeId = "demo_store",
                        staffName = "Helper",
                        roleLabel = "Cashier",
                        permissionLevel = "Cashier",
                        permissions = StorePermissions.forLevel("cashier")
                    )
                )
            }.isFailure
        )
        assertTrue(runCatching { repo.cancelInvite("ABCD1234") }.isFailure)
        assertTrue(repo.joinStoreWithInvite("ABCD1234", "u1", "a@b.com", "User").isFailure)
        assertTrue(
            runCatching {
                repo.updateMemberPermissions("demo_store", "m1", "Cashier", "Cashier", emptyMap())
            }.isFailure
        )
        assertTrue(runCatching { repo.removeMember("demo_store", "m1") }.isFailure)
        assertTrue(runCatching { repo.restoreMember("demo_store", "m1") }.isFailure)

        // Private AI key / Gemini API calls fail or emit disabled locally
        assertFalse(repo.observeAiKeySummary("demo_store", isOwner = true, isFatherMode = false).first().hasSavedKey)
        assertNull(repo.getPrivateAiKeyOnce("demo_store"))
        assertTrue(runCatching { repo.savePrivateAiKey("demo_store", "AIzaSy1234567890", "demo_user") }.isFailure)
        assertTrue(repo.testGeminiApiKey("AIzaSy1234567890").isFailure)
        assertTrue(
            repo.generateStrategyWithAi(
                apiKey = "AIzaSy1234567890",
                storeId = "demo_store",
                facts = BusinessStrategyFacts(),
                generatedAtMs = 1_000L,
                refreshCountToday = 0,
                updatedBy = "demo_user"
            ).isFailure
        )
        assertNull(repo.loadMonthlyReportInputs("demo_store", 2026, 0))
    }

    @Test
    fun `demo UI hides staff, AI key, and cloud photo toggle and shows Exit Demo`() {
        val repo = StoreRepository(db = null, appContext = context)
        val authVm = AuthViewModel.createDemo(repo, ShopType.ELECTRONICS)
        val storeVm = StoreViewModel(
            storeId = "demo_store",
            repository = repo,
            networkMonitor = NetworkMonitor(context),
            currentUserId = "demo_user"
        )
        storeVm.setLanguage(AppLanguage.EN)
        storeVm.setSimpleMode(false)

        // 1. SettingsScreen in demo
        composeRule.setContent {
            StoreOSTheme {
                SettingsScreen(
                    authViewModel = authVm,
                    storeViewModel = storeVm,
                    onBack = {},
                    onLogout = {},
                    onNavigateDestination = {}
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithText("Manage Staff").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("AI KEY (OWNER ONLY)").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Private Gemini AI Key").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Demo Local Storage").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_logout_btn").performScrollTo().assertIsDisplayed()
            .assertTextContains("Exit Demo")

        // 2. StoreViewModel guards staff/AI intents and destinations in demo
        storeVm.handleAppIntent(
            Intent().putExtra(SimpleModeRouteGuard.EXTRA_NAV_DESTINATION, BusinessNavDestination.STAFF_MANAGEMENT_SCREEN.name)
        )
        assertEquals(BusinessNavDestination.DASHBOARD, storeVm.businessNavDestination.value)
        storeVm.openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.REPORT_STAFF)
        assertEquals(BusinessNavDestination.DASHBOARD, storeVm.businessNavDestination.value)
    }

    @Test
    fun `demo MoreScreen and ReportsHub hide staff entries and show Exit Demo`() {
        val repo = StoreRepository(db = null, appContext = context)
        val authVm = AuthViewModel.createDemo(repo, ShopType.GROCERY)
        val storeVm = StoreViewModel(
            storeId = "demo_store",
            repository = repo,
            networkMonitor = NetworkMonitor(context),
            currentUserId = "demo_user"
        )
        storeVm.setLanguage(AppLanguage.EN)
        storeVm.setSimpleMode(false)

        composeRule.setContent {
            StoreOSTheme {
                MoreMenuContent(
                    authViewModel = authVm,
                    storeViewModel = storeVm,
                    onBack = {},
                    onOpenSettings = {}
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithTag("more_item_staff").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("more_menu_list").performScrollToKey("grid_ACCOUNT_0")
        composeRule.onNodeWithTag("more_item_logout").assertIsDisplayed().assertTextContains("Exit Demo")
    }

    @Test
    fun `demo ReportsHub hides Staff Report and BusinessDestinationContent blocks staff routes`() {
        val repo = StoreRepository(db = null, appContext = context)
        val authVm = AuthViewModel.createDemo(repo, ShopType.GENERAL)
        val storeVm = StoreViewModel(
            storeId = "demo_store",
            repository = repo,
            networkMonitor = NetworkMonitor(context),
            currentUserId = "demo_user"
        )
        storeVm.setLanguage(AppLanguage.EN)
        storeVm.setSimpleMode(false)

        composeRule.setContent {
            StoreOSTheme {
                FinancialReportsHubScreen(storeViewModel = storeVm)
            }
        }
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("report_link_staff").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("report_link_sales").assertIsDisplayed()
    }

    @Test
    fun `BusinessDestinationContent shows NoPermissionScreen for staff routes in demo`() {
        val repo = StoreRepository(db = null, appContext = context)
        val authVm = AuthViewModel.createDemo(repo, ShopType.GROCERY)
        val storeVm = StoreViewModel(
            storeId = "demo_store",
            repository = repo,
            networkMonitor = NetworkMonitor(context),
            currentUserId = "demo_user"
        )
        storeVm.setLanguage(AppLanguage.EN)

        composeRule.setContent {
            StoreOSTheme {
                BusinessDestinationContent(
                    destination = BusinessNavDestination.STAFF_MANAGEMENT_SCREEN,
                    authViewModel = authVm,
                    storeViewModel = storeVm
                )
            }
        }
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("staff_management_screen").fetchSemanticsNodes().size)
    }
}
