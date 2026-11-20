package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.StrategyActionTarget
import com.example.ui.components.BackupPinDialog
import com.example.ui.components.BackupPinDialogMode
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.FatherLockInfoDialog
import com.example.ui.components.FloatingGlassPillBottomBar
import com.example.ui.components.GlassCard
import com.example.ui.components.LeftSideGlassMenu
import com.example.ui.components.NoPermissionScreen
import com.example.ui.components.ShopNavTab
import com.example.ui.components.VividButton
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BusinessNavDestination
import com.example.ui.viewmodel.SimpleModeRouteGuard
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import com.example.util.SyncStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class HomeQuickDialog {
    NONE,
    NOTIFICATIONS,
    PAYMENT_IN,
    PAYMENT_OUT,
    PURCHASE,
    EXPENSE,
    ADD_PARTY,
    CONFIRM_DETAILED_MODE,
    FATHER_LOCK_INFO
}

@Composable
fun MainShopScreen(
    authViewModel: AuthViewModel,
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTab by storeViewModel.currentTab.collectAsState()
    val showSettings by storeViewModel.showSettings.collectAsState()
    var activeDialog by remember { mutableStateOf(HomeQuickDialog.NONE) }
    var quickRestockProduct by remember { mutableStateOf<Product?>(null) }

    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val syncStatus by storeViewModel.syncStatus.collectAsState()
    val isPrivacyHidden by storeViewModel.isPrivacyHidden.collectAsState()
    val lowStockProducts by storeViewModel.lowStockProducts.collectAsState()
    val bakiOverview by storeViewModel.bakiOverview.collectAsState()

    val isRemoved by storeViewModel.isRemoved.collectAsState()
    val canSell by storeViewModel.canSellFlow.collectAsState()
    val canViewStock by storeViewModel.canViewStockFlow.collectAsState()
    val canSeeHistory by storeViewModel.canSeeHistoryFlow.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val canViewReports by storeViewModel.canViewReportsFlow.collectAsState()
    val canAddEdit by storeViewModel.canAddEditProductsFlow.collectAsState()
    val canAccessBaki by storeViewModel.canAccessCustomersAndBakiFlow.collectAsState()
    val canReceivePayment by storeViewModel.canReceivePaymentFlow.collectAsState()
    val canAccessSuppliers by storeViewModel.canAccessSuppliersAndPurchasesFlow.collectAsState()
    val canPaySupplier by storeViewModel.canPaySupplierFlow.collectAsState()
    val canAccessCashAndBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()
    val canAccessExpenses by storeViewModel.canAccessExpensesAndIncomeFlow.collectAsState()

    var showAddWizard by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    val isBusy by storeViewModel.isBusy.collectAsState()
    val errorMessage by storeViewModel.errorMessage.collectAsState()
    val statusMessage by storeViewModel.statusMessage.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val isOwner by storeViewModel.isOwner.collectAsState()
    val currentMember by storeViewModel.currentMember.collectAsState()
    val hasOwnerPin by storeViewModel.hasOwnerPin.collectAsState()
    val pinLockoutUntilMs by storeViewModel.pinLockoutUntilMs.collectAsState()
    var showFatherExitPinDialog by remember { mutableStateOf(false) }
    var showDetailedExitPinDialog by remember { mutableStateOf(false) }
    var showSimpleMonthlyReport by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentTab, isSimpleMode, isFatherMode) {
        storeViewModel.setActiveSubNavTab(currentTab)
        if (!isSimpleMode || isFatherMode || currentTab != ShopNavTab.HOME) {
            showSimpleMonthlyReport = false
        }
    }

    LaunchedEffect(isSimpleMode, isFatherMode, activeDialog) {
        val mappedModal = when (activeDialog) {
            HomeQuickDialog.NONE -> SimpleModeRouteGuard.GuardedModal.NONE
            HomeQuickDialog.NOTIFICATIONS -> SimpleModeRouteGuard.GuardedModal.NOTIFICATIONS
            HomeQuickDialog.PAYMENT_IN -> SimpleModeRouteGuard.GuardedModal.PAYMENT_IN
            HomeQuickDialog.PAYMENT_OUT -> SimpleModeRouteGuard.GuardedModal.PAYMENT_OUT
            HomeQuickDialog.PURCHASE -> SimpleModeRouteGuard.GuardedModal.PURCHASE
            HomeQuickDialog.EXPENSE -> SimpleModeRouteGuard.GuardedModal.EXPENSE
            HomeQuickDialog.ADD_PARTY -> SimpleModeRouteGuard.GuardedModal.ADD_PARTY
            HomeQuickDialog.CONFIRM_DETAILED_MODE -> SimpleModeRouteGuard.GuardedModal.CONFIRM_DETAILED_MODE
            HomeQuickDialog.FATHER_LOCK_INFO -> SimpleModeRouteGuard.GuardedModal.FATHER_LOCK_INFO
        }
        if (!SimpleModeRouteGuard.isModalAllowed(mappedModal, isSimpleMode, isFatherMode)) {
            activeDialog = HomeQuickDialog.NONE
        }
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Long
            )
            storeViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(errorMessage, showAddWizard, editingProduct) {
        val err = errorMessage
        if (!err.isNullOrBlank() && !showAddWizard && editingProduct == null) {
            snackbarHostState.showSnackbar(
                message = err,
                duration = SnackbarDuration.Long
            )
            storeViewModel.clearErrorMessage()
        }
    }

    val moreDest by storeViewModel.moreNavDestination.collectAsState()
    BackHandler(
        enabled = !showSettings && (
            (SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode, isFatherMode) && currentTab != ShopNavTab.HOME) ||
                (currentTab == ShopNavTab.MORE && moreDest == BusinessNavDestination.DASHBOARD)
            )
    ) {
        storeViewModel.handleBackNavigation()
    }

    if (isRemoved) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
                .displayCutoutPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "অ্যাক্সেস প্রত্যাহার করা হয়েছে" else "Access Revoked",
                        color = AlertRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "আপনি আর এই দোকানে অ্যাক্সেস করতে পারবেন না। দয়া করে দোকানের মালিকের সাথে যোগাযোগ করুন।"
                        else "You no longer have access to this store. Please contact the store owner.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    VividButton(
                        text = if (currentLanguage == AppLanguage.BN) "লগ আউট করুন" else "Sign Out",
                        onClick = { authViewModel.signOut(context) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        return
    }

    if (showSettings && !isFatherMode && !isSimpleMode) {
        SettingsScreen(
            authViewModel = authViewModel,
            storeViewModel = storeViewModel,
            onBack = { storeViewModel.closeSettings() },
            onLogout = {
                storeViewModel.closeSettings()
                authViewModel.signOut(context)
            },
            onNavigateDestination = { dest ->
                storeViewModel.closeSettings()
                storeViewModel.openSubDestination(ShopNavTab.MORE, dest)
            },
            modifier = modifier
        )
        return
    }

    if (showSimpleMonthlyReport && isSimpleMode && !isFatherMode) {
        MonthlyReportScreen(
            storeViewModel = storeViewModel,
            onBackOverride = { showSimpleMonthlyReport = false },
            modifier = modifier
        )
        return
    }

    val alertCount = (if (canViewStock) lowStockProducts.size else 0) +
        (if (canAccessBaki) bakiOverview.debtorCount.toInt() else 0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070907))
            .drawBehind {
                // Top-left ambient emerald glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VividGreen.copy(alpha = 0.22f),
                            VividGreen.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.06f, size.height * 0.04f),
                        radius = size.width * 0.72f
                    )
                )
                // Soft lower emerald glow when on Home Simple mode
                if (currentTab == ShopNavTab.HOME && isSimpleMode) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                VividGreen.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.65f, size.height * 0.50f),
                            radius = size.width * 0.68f
                        )
                    )
                }
            }
    ) {
        val isWideScreen = maxWidth >= 600.dp
        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen && !isFatherMode) {
                LeftSideGlassMenu(
                    storeName = store?.name?.ifBlank { "StoreOS" } ?: "StoreOS",
                    currentTab = currentTab,
                    onTabSelected = { target -> storeViewModel.navigateToTab(target) },
                    onOpenSettings = if (isSimpleMode) null else { { storeViewModel.openSettings() } },
                    canSell = canSell,
                    canViewStock = canViewStock,
                    canSeeHistory = canSeeHistory,
                    canViewReports = canViewReports,
                    canAccessBaki = canAccessBaki,
                    isSimpleMode = isSimpleMode,
                    isFatherMode = false
                )
            }

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
                topBar = {
                    ShopTopBar(
                        storeName = store?.name?.ifBlank { "StoreOS" } ?: "StoreOS",
                        syncStatus = syncStatus,
                        currentLanguage = currentLanguage,
                        isSimpleMode = isSimpleMode,
                        isFatherMode = isFatherMode,
                        showFatherHomeButton = isFatherMode && currentTab != ShopNavTab.HOME,
                        onFatherGoHome = {
                            storeViewModel.resetBusinessToRoot()
                            storeViewModel.resetMoreToRoot()
                            storeViewModel.navigateToTab(ShopNavTab.HOME)
                        },
                        isPrivacyHidden = isPrivacyHidden,
                        alertCount = alertCount,
                        isMoreSelected = currentTab == ShopNavTab.MORE,
                        onTogglePrivacy = { storeViewModel.togglePrivacyHidden() },
                        onToggleLanguage = { storeViewModel.toggleLanguage() },
                        onOpenNotifications = { if (!isFatherMode) activeDialog = HomeQuickDialog.NOTIFICATIONS },
                        onOpenFatherLockInfo = { activeDialog = HomeQuickDialog.FATHER_LOCK_INFO },
                        onOpenMore = {
                            if (isFatherMode || isSimpleMode) return@ShopTopBar
                            if (currentTab == ShopNavTab.MORE) {
                                storeViewModel.resetMoreToRoot()
                            } else {
                                storeViewModel.navigateToTab(ShopNavTab.MORE)
                            }
                        },
                        onOpenSettings = { storeViewModel.openSettings() },
                        onRequestSwitchToDetailed = {
                            if (!isFatherMode) {
                                showDetailedExitPinDialog = true
                            }
                        }
                    )
                },
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = if (isWideScreen || isFatherMode) 16.dp else 20.dp
                        )
                    ) { data ->
                        val toastShape = RoundedCornerShape(16.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(toastShape)
                                .background(Color(0xF01C221D))
                                .border(1.dp, VividGreen.copy(alpha = 0.45f), toastShape)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = data.visuals.message,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            val actionLabel = data.visuals.actionLabel
                            if (!actionLabel.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = actionLabel,
                                    color = VividGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { data.performAction() }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!isWideScreen && !isFatherMode) {
                        FloatingGlassPillBottomBar(
                            currentTab = currentTab,
                            onTabSelected = { target -> storeViewModel.navigateToTab(target) },
                            canSell = canSell,
                            canViewStock = canViewStock,
                            canSeeHistory = canSeeHistory,
                            canViewReports = canViewReports,
                            canAccessBaki = canAccessBaki,
                            isSimpleMode = isSimpleMode,
                            isFatherMode = false
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding()
                ) {
                when (currentTab) {
                    ShopNavTab.HOME -> HomeDashboardContent(
                        storeViewModel = storeViewModel,
                        onNavigateToSell = { if (canSell) storeViewModel.navigateToTab(ShopNavTab.SELL) },
                        onNavigateToStock = { if (canViewStock) storeViewModel.navigateToTab(ShopNavTab.STOCK) },
                        onNavigateToHistory = {
                            if (SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode, isFatherMode)) {
                                storeViewModel.navigateToTab(ShopNavTab.HOME)
                            } else if (canSeeHistory) {
                                storeViewModel.navigateToTab(ShopNavTab.HISTORY)
                            } else if (canViewReports) {
                                storeViewModel.openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.DASHBOARD)
                            }
                        },
                        onOpenSaleDetail = { sale ->
                            if (!SimpleModeRouteGuard.isSimpleRestricted(isSimpleMode, isFatherMode) && canSeeHistory) {
                                storeViewModel.selectSaleDetail(sale)
                                storeViewModel.navigateToTab(ShopNavTab.HISTORY)
                            }
                        },
                        onOpenBaki = { storeViewModel.openBakiFromAlert() },
                        onOpenCashAndBank = {
                            if (canAccessCashAndBank && !isFatherMode && !isSimpleMode) {
                                storeViewModel.openSubDestination(ShopNavTab.MORE, BusinessNavDestination.CASH_BANK_SCREEN)
                            }
                        },
                        onOpenSuppliers = {
                            if (canAccessSuppliers && !isFatherMode && !isSimpleMode) {
                                val tab = if (canViewReports) ShopNavTab.BUSINESS else ShopNavTab.MORE
                                storeViewModel.openSubDestination(tab, BusinessNavDestination.SUPPLIERS_SCREEN)
                            }
                        },
                        onOpenLowStock = { storeViewModel.openLowStockFromAlert() },
                        onOpenCashflowDetail = {
                            if (!isFatherMode && !isSimpleMode) {
                                if (canViewReports) {
                                    storeViewModel.openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.REPORT_DAY_BOOK)
                                } else if (canAccessCashAndBank) {
                                    storeViewModel.openSubDestination(ShopNavTab.MORE, BusinessNavDestination.CASH_BANK_SCREEN)
                                }
                            }
                        },
                        onRestockProduct = { prod ->
                            if (canAddEdit && !isFatherMode) quickRestockProduct = prod
                        },
                        onRequestSwitchToDetailed = {
                            if (!isFatherMode) {
                                showDetailedExitPinDialog = true
                            }
                        },
                        onQuickPaymentIn = { if (!isFatherMode && (canReceivePayment || canAccessBaki)) activeDialog = HomeQuickDialog.PAYMENT_IN },
                        onQuickPaymentOut = { if (!isFatherMode && !isSimpleMode) activeDialog = HomeQuickDialog.PAYMENT_OUT },
                        onQuickPurchase = { if (!isFatherMode && !isSimpleMode) activeDialog = HomeQuickDialog.PURCHASE },
                        onQuickExpense = { if (!isFatherMode && !isSimpleMode) activeDialog = HomeQuickDialog.EXPENSE },
                        onQuickAddParty = { if (!isFatherMode && !isSimpleMode) activeDialog = HomeQuickDialog.ADD_PARTY },
                        onAddProduct = { if (canAddEdit && !isFatherMode) showAddWizard = true },
                        onOpenReports = {
                            if (isFatherMode) {
                                storeViewModel.openSubDestination(ShopNavTab.BUSINESS, BusinessNavDestination.MONTHLY_REPORT)
                            } else if (isSimpleMode) {
                                showSimpleMonthlyReport = true
                            } else if (canViewReports) {
                                storeViewModel.openSubDestination(ShopNavTab.MORE, BusinessNavDestination.FINANCIAL_REPORTS_HUB)
                            }
                        }
                    )
                    ShopNavTab.STOCK -> if (isFatherMode || (!canViewStock && isSimpleMode)) {
                        LaunchedEffect(Unit) { storeViewModel.navigateToTab(ShopNavTab.HOME) }
                    } else if (canViewStock) {
                        StockScreen(storeViewModel = storeViewModel)
                    } else {
                        NoPermissionScreen(
                            onBack = { storeViewModel.navigateToTab(ShopNavTab.HOME) },
                            currentLanguage = currentLanguage
                        )
                    }
                    ShopNavTab.SELL -> if (canSell) {
                        SellScreen(
                            storeViewModel = storeViewModel,
                            onBack = { storeViewModel.navigateToTab(ShopNavTab.HOME) }
                        )
                    } else if (isFatherMode || isSimpleMode) {
                        LaunchedEffect(Unit) { storeViewModel.navigateToTab(ShopNavTab.HOME) }
                    } else {
                        NoPermissionScreen(
                            onBack = { storeViewModel.navigateToTab(ShopNavTab.HOME) },
                            currentLanguage = currentLanguage
                        )
                    }
                    ShopNavTab.BAKI -> if (isFatherMode || !canAccessBaki) {
                        LaunchedEffect(Unit) { storeViewModel.navigateToTab(ShopNavTab.HOME) }
                    } else {
                        BakiMainScreen(
                            storeViewModel = storeViewModel,
                            initialTab = 0,
                            onReturnHome = { storeViewModel.navigateToTab(ShopNavTab.HOME) }
                        )
                    }
                    ShopNavTab.HISTORY -> if (isFatherMode || isSimpleMode) {
                        LaunchedEffect(Unit) { storeViewModel.navigateToTab(ShopNavTab.HOME) }
                    } else if (canSeeHistory) {
                        HistoryScreen(
                            storeViewModel = storeViewModel,
                            onBack = { storeViewModel.navigateToTab(ShopNavTab.HOME) },
                            onNavigateToStock = { storeViewModel.navigateToTab(ShopNavTab.STOCK) }
                        )
                    } else {
                        NoPermissionScreen(
                            onBack = { storeViewModel.navigateToTab(ShopNavTab.HOME) },
                            currentLanguage = currentLanguage
                        )
                    }
                    ShopNavTab.BUSINESS -> if (isSimpleMode && !isFatherMode) {
                        LaunchedEffect(Unit) {
                            storeViewModel.resetBusinessToRoot()
                            storeViewModel.navigateToTab(ShopNavTab.HOME)
                        }
                    } else if (isFatherMode || canViewReports) {
                        BusinessScreen(
                            authViewModel = authViewModel,
                            storeViewModel = storeViewModel,
                            onReturnHome = {
                                storeViewModel.resetBusinessToRoot()
                                storeViewModel.navigateToTab(ShopNavTab.HOME)
                            }
                        )
                    } else {
                        NoPermissionScreen(
                            onBack = { storeViewModel.navigateToTab(ShopNavTab.HOME) },
                            currentLanguage = currentLanguage
                        )
                    }
                    ShopNavTab.MORE -> if (isFatherMode || isSimpleMode) {
                        LaunchedEffect(Unit) {
                            storeViewModel.resetMoreToRoot()
                            storeViewModel.navigateToTab(ShopNavTab.HOME)
                        }
                    } else {
                        MoreScreen(
                            authViewModel = authViewModel,
                            storeViewModel = storeViewModel,
                            onBack = { storeViewModel.navigateToTab(ShopNavTab.HOME) },
                            onOpenSettings = { storeViewModel.openSettings() }
                        )
                    }
                }
            }
        }
        }
    }

    // Dialogs & Quick Actions
    when (activeDialog) {
        HomeQuickDialog.NOTIFICATIONS -> HomeNotificationsDialog(
            lowStockProducts = lowStockProducts,
            bakiOverview = bakiOverview,
            syncStatus = syncStatus,
            currentLanguage = currentLanguage,
            canViewStock = canViewStock,
            canAccessBaki = canAccessBaki,
            onRestockProduct = { prod ->
                if (canAddEdit) quickRestockProduct = prod
                else storeViewModel.openLowStockFromAlert()
            },
            onOpenLowStock = { storeViewModel.openLowStockFromAlert() },
            onOpenBaki = { storeViewModel.openBakiFromAlert() },
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.PAYMENT_IN -> HomePaymentInDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.PAYMENT_OUT -> HomePaymentOutDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.PURCHASE -> HomeRecordPurchaseDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.EXPENSE -> AddExpenseDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { activeDialog = HomeQuickDialog.NONE },
            onConfirm = { amount, category, date, accId, accName, note, receiptUri ->
                storeViewModel.recordExpense(
                    amount = amount,
                    category = category,
                    date = date,
                    accountId = accId,
                    accountName = accName,
                    note = note,
                    receiptUri = receiptUri,
                    onSuccess = { activeDialog = HomeQuickDialog.NONE }
                )
            }
        )
        HomeQuickDialog.ADD_PARTY -> HomeAddPartyDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            canAddCustomer = canAccessBaki,
            canAddSupplier = canAccessSuppliers,
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.CONFIRM_DETAILED_MODE -> AlertDialog(
            onDismissRequest = { activeDialog = HomeQuickDialog.NONE },
            properties = edgeToEdgeDialogProperties(),
            modifier = Modifier.safeDialogWindowPadding(),
            containerColor = Color(0xFF1A1E1A),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                ConfigureEdgeToEdgeDialogWindow()
                Text(
                    text = ShopLocale.get("switch_to_detailed_title", currentLanguage),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = ShopLocale.get("switch_to_detailed_desc", currentLanguage)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        storeViewModel.setSimpleMode(false)
                        activeDialog = HomeQuickDialog.NONE
                    }
                ) {
                    Text(ShopLocale.get("switch_btn", currentLanguage), color = VividGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDialog = HomeQuickDialog.NONE }) {
                    Text(ShopLocale.get("stay_simple_btn", currentLanguage), color = TextSecondary)
                }
            }
        )
        HomeQuickDialog.FATHER_LOCK_INFO -> FatherLockInfoDialog(
            currentLanguage = currentLanguage,
            isOwner = isOwner,
            onTryDetailedMode = {
                activeDialog = HomeQuickDialog.NONE
                storeViewModel.setSimpleMode(false)
            },
            onTurnOffFatherMode = {
                activeDialog = HomeQuickDialog.NONE
                showFatherExitPinDialog = true
            },
            onDismiss = { activeDialog = HomeQuickDialog.NONE }
        )
        HomeQuickDialog.NONE -> {}
    }

    if (showDetailedExitPinDialog && !isFatherMode) {
        BackupPinDialog(
            mode = if (hasOwnerPin) BackupPinDialogMode.ENTER_PIN else BackupPinDialogMode.SET_PIN,
            currentLanguage = currentLanguage,
            customDescription = if (hasOwnerPin) {
                ShopLocale.get("simple_exit_pin_desc", currentLanguage)
            } else {
                ShopLocale.get("simple_exit_set_pin_first", currentLanguage)
            },
            lockoutUntilMs = pinLockoutUntilMs,
            getRemainingLockoutSeconds = { storeViewModel.getRemainingPinLockoutSeconds() },
            onSetPin = { pin, done ->
                storeViewModel.setOwnerPinAndExitSimpleMode(pin) {
                    done()
                    showDetailedExitPinDialog = false
                }
            },
            onVerifyPin = { _, _ -> },
            onVerifyPinOutcome = { pin, onOutcome ->
                val outcome = storeViewModel.verifyOwnerPinAndExitSimpleMode(pin)
                onOutcome(outcome)
                if (outcome.success) {
                    showDetailedExitPinDialog = false
                }
            },
            onChangePin = { _, _, _ -> },
            onDismiss = { showDetailedExitPinDialog = false }
        )
    }

    if (showFatherExitPinDialog && isOwner) {
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
                        onSuccess = { showFatherExitPinDialog = false }
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
                            onSuccess = { showFatherExitPinDialog = false }
                        )
                    }
                }
            },
            onChangePin = { _, _, _ -> },
            onDismiss = { showFatherExitPinDialog = false }
        )
    }

    if (quickRestockProduct != null) {
        QuickRestockDialog(
            product = quickRestockProduct!!,
            isFatherMode = isFatherMode,
            onDismiss = { quickRestockProduct = null },
            onConfirm = { amount ->
                storeViewModel.addQuickStock(quickRestockProduct!!.id, amount)
                quickRestockProduct = null
            }
        )
    }

    if (showAddWizard) {
        val shopType by storeViewModel.shopType.collectAsState()
        val availableCategories by storeViewModel.availableProductCategories.collectAsState()
        val unitPresets by storeViewModel.unitPresets.collectAsState()
        val photoStorageMode by storeViewModel.photoStorageMode.collectAsState()
        AddProductWizardDialog(
            currentLanguage = currentLanguage,
            isBusy = isBusy,
            canSeeBuyingPrice = canSeeProfit,
            isSimpleMode = isSimpleMode,
            isFatherMode = isFatherMode,
            hasOwnerPin = hasOwnerPin,
            onVerifyOwnerPin = { pin, onResult -> storeViewModel.verifyOwnerPin(pin, onResult) },
            onSetOwnerPin = { pin, onDone -> storeViewModel.setOwnerPin(pin, onDone) },
            shopType = shopType,
            availableCategories = availableCategories,
            unitPresets = unitPresets,
            photoStorageMode = photoStorageMode,
            externalError = errorMessage,
            onDismiss = {
                storeViewModel.clearErrorMessage()
                showAddWizard = false
            },
            onConfirm = { name, nameBn, category, unit, buy, sell, stock, lowLimit, photo, barcode, wholesale, mrp, location, partyRate, buyUnit, buyConversion, sellUnits ->
                storeViewModel.addNewProductWizard(
                    name = name,
                    nameBn = nameBn,
                    category = category,
                    unit = unit,
                    buyPrice = buy,
                    sellPrice = sell,
                    stock = stock,
                    lowStockLimit = lowLimit,
                    photoUri = photo,
                    barcode = barcode,
                    wholesalePrice = wholesale,
                    mrp = mrp,
                    itemLocation = location,
                    partyRate = partyRate,
                    buyUnit = buyUnit,
                    buyConversion = buyConversion,
                    sellUnits = sellUnits,
                    onSuccess = { showAddWizard = false }
                )
            }
        )
    }

    if (editingProduct != null) {
        val shopType by storeViewModel.shopType.collectAsState()
        val availableCategories by storeViewModel.availableProductCategories.collectAsState()
        val unitPresets by storeViewModel.unitPresets.collectAsState()
        val photoStorageMode by storeViewModel.photoStorageMode.collectAsState()
        AddProductWizardDialog(
            productToEdit = editingProduct,
            currentLanguage = currentLanguage,
            isBusy = isBusy,
            canSeeBuyingPrice = canSeeProfit,
            isSimpleMode = isSimpleMode,
            isFatherMode = isFatherMode,
            hasOwnerPin = hasOwnerPin,
            onVerifyOwnerPin = { pin, onResult -> storeViewModel.verifyOwnerPin(pin, onResult) },
            onSetOwnerPin = { pin, onDone -> storeViewModel.setOwnerPin(pin, onDone) },
            shopType = shopType,
            availableCategories = availableCategories,
            unitPresets = unitPresets,
            photoStorageMode = photoStorageMode,
            externalError = errorMessage,
            onDismiss = {
                storeViewModel.clearErrorMessage()
                editingProduct = null
            },
            onConfirm = { name, nameBn, category, unit, buy, sell, stock, lowLimit, photo, barcode, wholesale, mrp, location, partyRate, buyUnit, buyConversion, sellUnits ->
                storeViewModel.updateProductWizard(
                    productId = editingProduct!!.id,
                    name = name,
                    nameBn = nameBn,
                    category = category,
                    unit = unit,
                    buyPrice = buy,
                    sellPrice = sell,
                    stock = stock,
                    lowStockLimit = lowLimit,
                    photoUri = photo,
                    barcode = barcode,
                    wholesalePrice = wholesale,
                    mrp = mrp,
                    itemLocation = location,
                    partyRate = partyRate,
                    buyUnit = buyUnit,
                    buyConversion = buyConversion,
                    sellUnits = sellUnits,
                    onSuccess = { editingProduct = null }
                )
            },
            onDelete = if (isOwner && !isFatherMode && !isSimpleMode) {
                {
                    val target = editingProduct
                    if (target != null) {
                        storeViewModel.deleteProduct(target.id) {
                            editingProduct = null
                        }
                    }
                }
            } else null
        )
    }
}

@Composable
fun ShopTopBar(
    storeName: String,
    syncStatus: SyncStatus,
    currentLanguage: AppLanguage,
    isSimpleMode: Boolean,
    isFatherMode: Boolean = false,
    showFatherHomeButton: Boolean = false,
    onFatherGoHome: () -> Unit = {},
    isPrivacyHidden: Boolean,
    alertCount: Int,
    isMoreSelected: Boolean,
    onTogglePrivacy: () -> Unit,
    onToggleLanguage: () -> Unit = {},
    onOpenNotifications: () -> Unit,
    onOpenFatherLockInfo: () -> Unit = {},
    onOpenMore: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestSwitchToDetailed: () -> Unit = {}
) {
    val dateLabel = remember(currentLanguage) {
        ShopLocale.formatWeekdayDate(System.currentTimeMillis(), currentLanguage)
    }
    val greetingLabel = remember(currentLanguage) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentLanguage == AppLanguage.BN) {
            when {
                hour < 12 -> "শুভ সকাল"
                hour < 17 -> "শুভ দুপুর"
                else -> "শুভ সন্ধ্যা"
            }
        } else {
            when {
                hour < 12 -> "Good morning"
                hour < 17 -> "Good afternoon"
                else -> "Good evening"
            }
        }
    }

    val syncDotColor = when (syncStatus) {
        SyncStatus.ONLINE -> VividGreen
        SyncStatus.SYNCING -> Color(0xFFFFB300)
        SyncStatus.OFFLINE -> AlertRed
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .displayCutoutPadding()
    ) {
        var collapseOverflow by remember(storeName, maxWidth, isFatherMode) { mutableStateOf(false) }
        var overflowExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isFatherMode && showFatherHomeButton) {
                Surface(
                    onClick = onFatherGoHome,
                    shape = RoundedCornerShape(16.dp),
                    color = VividGreen.copy(alpha = 0.20f),
                    border = BorderStroke(1.2.dp, VividGreen),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("father_top_home_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "হোম",
                            tint = VividGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "হোম",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0x36FFFFFF)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF1F301B), Color(0xFF131912))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = storeName,
                            tint = VividGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (isSimpleMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = storeName,
                                color = TextPrimary,
                                fontSize = 15.5.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                softWrap = true,
                                onTextLayout = { result ->
                                    if (!isSimpleMode && !collapseOverflow && (result.lineCount > 1 || result.hasVisualOverflow)) {
                                        collapseOverflow = true
                                    }
                                },
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(syncDotColor)
                            )
                        }
                        Text(
                            text = dateLabel,
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 2,
                            softWrap = true
                        )
                    } else {
                        Text(
                            text = greetingLabel,
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 2,
                            softWrap = true
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = storeName,
                                color = TextPrimary,
                                fontSize = 15.5.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                softWrap = true,
                                onTextLayout = { result ->
                                    if (!collapseOverflow && (result.lineCount > 1 || result.hasVisualOverflow)) {
                                        collapseOverflow = true
                                    }
                                },
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(syncDotColor)
                            )
                        }
                    }
                }
            }
            }

            // Top-right controls: eye, bell, and either language+detailed link or grid+gear
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isSimpleMode && !isFatherMode) 4.dp else 6.dp)
            ) {
                if (isFatherMode) {
                    val amberAccent = Color(0xFFFFB300)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0x334D3800),
                        border = BorderStroke(1.dp, amberAccent.copy(alpha = 0.72f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onOpenFatherLockInfo)
                            .testTag("top_bar_father_mode_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = ShopLocale.get("father_mode_chip", currentLanguage),
                                tint = amberAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = ShopLocale.get("father_mode_chip", currentLanguage),
                                color = amberAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                softWrap = true
                            )
                        }
                    }
                }
                TopBarCircleIconButton(
                    icon = if (isPrivacyHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isPrivacyHidden) "Show amounts" else "Hide amounts",
                    isActive = isPrivacyHidden,
                    onClick = onTogglePrivacy,
                    testTag = "top_bar_privacy_btn"
                )
                if (!isFatherMode) {
                    TopBarCircleIconButton(
                        icon = Icons.Default.NotificationsNone,
                        contentDescription = "Alerts",
                        hasBadge = alertCount > 0,
                        onClick = onOpenNotifications,
                        testTag = "top_bar_notifications_btn"
                    )
                    if (isSimpleMode) {
                        Surface(
                            onClick = onToggleLanguage,
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x22FFFFFF),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("top_bar_language_chip")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "বাংলা" else "EN",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Surface(
                            onClick = onRequestSwitchToDetailed,
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x22FFFFFF),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("top_bar_detailed_mode_link")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ShopLocale.get("go_detailed_mode_link", currentLanguage),
                                    color = VividGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else if (collapseOverflow) {
                        Box {
                            TopBarCircleIconButton(
                                icon = Icons.Default.MoreVert,
                                contentDescription = if (currentLanguage == AppLanguage.BN) "আরও অপশন" else "More options",
                                isActive = isMoreSelected || overflowExpanded,
                                onClick = { overflowExpanded = true },
                                testTag = "top_bar_overflow_btn"
                            )
                            DropdownMenu(
                                expanded = overflowExpanded,
                                onDismissRequest = { overflowExpanded = false },
                                modifier = Modifier.background(DarkBackground)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) "আরও মেনু" else "More",
                                            color = TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = null,
                                            tint = if (isMoreSelected) VividGreen else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        onOpenMore()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = ShopLocale.get("settings_title", currentLanguage),
                                            color = TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = TextPrimary
                                        )
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        onOpenSettings()
                                    }
                                )
                            }
                        }
                    } else {
                        TopBarCircleIconButton(
                            icon = Icons.Default.GridView,
                            contentDescription = "More",
                            isActive = isMoreSelected,
                            onClick = onOpenMore,
                            testTag = "nav_more"
                        )
                        TopBarCircleIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings",
                            onClick = onOpenSettings,
                            testTag = "top_bar_settings_btn"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    hasBadge: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isActive) VividGreen.copy(alpha = 0.7f) else Color(0x2EFFFFFF)
        ),
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isActive) {
                            listOf(Color(0x385DD62C), Color(0x1C5DD62C))
                        } else {
                            listOf(Color(0xFF262A26), Color(0xFF161816))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) VividGreen else TextPrimary,
                modifier = Modifier.size(17.dp)
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(VividGreen)
                )
            }
        }
    }
}

@Composable
fun HomeDashboardContent(
    storeViewModel: StoreViewModel,
    onNavigateToSell: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenSaleDetail: (com.example.data.model.Sale) -> Unit,
    onOpenBaki: () -> Unit,
    onOpenCashAndBank: () -> Unit,
    onOpenSuppliers: () -> Unit,
    onOpenLowStock: () -> Unit,
    onOpenCashflowDetail: () -> Unit,
    onRestockProduct: (Product) -> Unit,
    onRequestSwitchToDetailed: () -> Unit,
    onQuickPaymentIn: () -> Unit,
    onQuickPaymentOut: () -> Unit,
    onQuickPurchase: () -> Unit,
    onQuickExpense: () -> Unit,
    onQuickAddParty: () -> Unit,
    onAddProduct: () -> Unit = {},
    onOpenReports: () -> Unit = {}
) {
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val financials by storeViewModel.financials.collectAsState()
    val stockSummary by storeViewModel.stockSummary.collectAsState()
    val homeInsights by storeViewModel.homeInsights.collectAsState()
    val bakiOverview by storeViewModel.bakiOverview.collectAsState()
    val totalBalance by storeViewModel.totalBalance.collectAsState()
    val moneyNow by storeViewModel.moneyNow.collectAsState()
    val lowStockProducts by storeViewModel.lowStockProducts.collectAsState()
    val sevenDayCashflow by storeViewModel.sevenDayCashflow.collectAsState()
    val recentSales by storeViewModel.recentSales.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val showProfitInSimpleMode by storeViewModel.showProfitInSimpleMode.collectAsState()
    val isOwner by storeViewModel.isOwner.collectAsState()
    val isPrivacyHidden by storeViewModel.isPrivacyHidden.collectAsState()
    val businessStrategy by storeViewModel.businessStrategy.collectAsState()
    val isStrategyRefreshing by storeViewModel.isStrategyRefreshing.collectAsState()
    val strategyRefreshesRemaining by storeViewModel.strategyRefreshesRemainingToday.collectAsState()

    val canSell by storeViewModel.canSellFlow.collectAsState()
    val canAddEdit by storeViewModel.canAddEditProductsFlow.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val canViewStock by storeViewModel.canViewStockFlow.collectAsState()
    val canSeeHistory by storeViewModel.canSeeHistoryFlow.collectAsState()
    val canViewReports by storeViewModel.canViewReportsFlow.collectAsState()
    val canAccessBaki by storeViewModel.canAccessCustomersAndBakiFlow.collectAsState()
    val canReceivePayment by storeViewModel.canReceivePaymentFlow.collectAsState()
    val canAccessCashAndBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()
    val canAccessSuppliers by storeViewModel.canAccessSuppliersAndPurchasesFlow.collectAsState()
    val canPaySupplier by storeViewModel.canPaySupplierFlow.collectAsState()
    val canAccessExpenses by storeViewModel.canAccessExpensesAndIncomeFlow.collectAsState()

    val canSeeSalesSummary = canSeeHistory || canViewReports
    val canViewStrategy = canViewReports
    val canRefreshStrategy = isOwner && !isFatherMode && !storeViewModel.isDemoMode

    val handleStrategyAction: (StrategyActionTarget) -> Unit = { target ->
        when (target) {
            StrategyActionTarget.SELL -> if (canSell) onNavigateToSell() else onOpenReports()
            StrategyActionTarget.STOCK -> if (canViewStock) onNavigateToStock() else onOpenReports()
            StrategyActionTarget.LOW_STOCK -> if (canViewStock) onOpenLowStock() else onOpenReports()
            StrategyActionTarget.BAKI -> if (canAccessBaki || canReceivePayment) onOpenBaki() else onOpenReports()
            StrategyActionTarget.REPORTS -> if (canViewReports) onOpenReports()
        }
    }

    val accounts by storeViewModel.accounts.collectAsState()
    val accountBalances by storeViewModel.accountBalances.collectAsState()
    val cashInHand = remember(accounts, accountBalances, totalBalance) {
        val cashAcc = accounts.find { !it.isArchived && (it.id == "cash_in_hand" || it.isDefault) }
        if (cashAcc != null) {
            accountBalances[cashAcc.id] ?: cashAcc.openingBalance
        } else {
            totalBalance
        }
    }

    if (isFatherMode) {
        FatherModeHomeLayout(
            financials = financials,
            cashInHand = cashInHand,
            bakiOverview = bakiOverview,
            lowStockProducts = lowStockProducts,
            isPrivacyHidden = isPrivacyHidden,
            onNavigateToSell = onNavigateToSell,
            onOpenBaki = onOpenBaki,
            onOpenLowStock = onOpenLowStock,
            onOpenMonthlyReport = onOpenReports,
            businessStrategy = businessStrategy
        )
    } else if (isSimpleMode) {
        SimpleHomeLayout(
            financials = financials,
            homeInsights = homeInsights,
            cashInHand = cashInHand,
            bakiOverview = bakiOverview,
            lowStockProducts = lowStockProducts,
            currentLanguage = currentLanguage,
            isPrivacyHidden = isPrivacyHidden,
            canSell = canSell,
            canViewStock = canViewStock,
            canAddEditProducts = canAddEdit,
            canAccessBaki = canAccessBaki,
            canReceivePayment = canReceivePayment,
            canSeeProfit = canSeeProfit,
            canSeeSalesSummary = canSeeSalesSummary,
            onNavigateToSell = onNavigateToSell,
            onQuickPaymentIn = onQuickPaymentIn,
            onAddProduct = onAddProduct,
            onNavigateToBaki = onOpenBaki,
            onOpenLowStock = onOpenLowStock,
            onNavigateToSalesSummary = onOpenReports,
            onOpenMonthlyReport = onOpenReports,
            businessStrategy = businessStrategy,
            canViewStrategy = canViewStrategy,
            showProfitInSimpleMode = showProfitInSimpleMode
        )
    } else {
        DetailedHomeLayout(
            financials = financials,
            homeInsights = homeInsights,
            bakiOverview = bakiOverview,
            totalBalance = totalBalance,
            moneyNow = moneyNow,
            lowStockProducts = lowStockProducts,
            sevenDayCashflow = sevenDayCashflow,
            recentSales = recentSales,
            currentLanguage = currentLanguage,
            isPrivacyHidden = isPrivacyHidden,
            canSeeSalesSummary = canSeeSalesSummary,
            canSeeProfit = canSeeProfit,
            canAccessBaki = canAccessBaki,
            canReceivePayment = canReceivePayment,
            canAccessCashAndBank = canAccessCashAndBank,
            canAccessSuppliers = canAccessSuppliers,
            canPaySupplier = canPaySupplier,
            canViewStock = canViewStock,
            canAddEditProducts = canAddEdit,
            canViewReports = canViewReports,
            canAccessExpenses = canAccessExpenses,
            canSeeHistory = canSeeHistory,
            canSell = canSell,
            onNavigateToSell = onNavigateToSell,
            onAddProduct = onAddProduct,
            onOpenSalesSummary = onNavigateToHistory,
            onOpenBaki = onOpenBaki,
            onOpenCashAndBank = onOpenCashAndBank,
            onOpenSuppliers = onOpenSuppliers,
            onOpenLowStock = onOpenLowStock,
            onRestockProduct = onRestockProduct,
            onOpenCashflowDetail = onOpenCashflowDetail,
            onOpenHistory = onNavigateToHistory,
            onOpenSaleDetail = onOpenSaleDetail,
            onQuickPaymentIn = onQuickPaymentIn,
            onQuickPaymentOut = onQuickPaymentOut,
            onQuickPurchase = onQuickPurchase,
            onQuickExpense = onQuickExpense,
            onQuickAddParty = onQuickAddParty,
            businessStrategy = businessStrategy,
            canViewStrategy = canViewStrategy,
            canRefreshStrategy = canRefreshStrategy,
            isStrategyRefreshing = isStrategyRefreshing,
            strategyRefreshesRemaining = strategyRefreshesRemaining,
            onRefreshStrategy = { storeViewModel.refreshBusinessStrategy() },
            onStrategyAction = handleStrategyAction
        )
    }
}
