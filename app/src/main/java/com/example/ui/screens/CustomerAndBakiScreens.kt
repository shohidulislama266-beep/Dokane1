package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Customer
import com.example.data.model.Supplier
import com.example.domain.CalculationEngine
import com.example.domain.CustomerBalanceSummary
import com.example.domain.CustomerLedgerEntry
import com.example.domain.LedgerEntryType
import com.example.ui.components.AccountPickerField
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.FatherConfirmActionDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.SearchGlassField
import com.example.ui.components.VividButton
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.BusinessNavDestination
import com.example.ui.viewmodel.BusinessSubScreen
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Parties Screen placed inside the Business / More flow.
 * Allows switching between Customers and Suppliers tabs, inspecting customer ledger or
 * supplier statement, recording Receive / Pay transactions directly, and adding parties.
 */
@Composable
fun BakiMainScreen(
    storeViewModel: StoreViewModel,
    initialTab: Int = 0, // 0 = Customers, 1 = Suppliers
    backButtonTag: String = "baki_back_btn",
    onReturnHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    if (isFatherMode) {
        FatherBakiReadOnlyScreen(
            storeViewModel = storeViewModel,
            onBack = {
                if (onReturnHome != null) onReturnHome()
                else storeViewModel.navigateBackInBusiness()
            },
            modifier = modifier
        )
        return
    }

    val subScreen by storeViewModel.businessSubScreen.collectAsState()
    val selectedCustomer by storeViewModel.selectedCustomer.collectAsState()

    LaunchedEffect(initialTab) {
        if (initialTab == 1 && subScreen == BusinessSubScreen.CUSTOMER_DETAILS) {
            storeViewModel.closeCustomerDetails()
        }
    }

    when {
        subScreen == BusinessSubScreen.CUSTOMER_DETAILS && selectedCustomer != null -> {
            CustomerDetailsScreen(
                storeViewModel = storeViewModel,
                customer = selectedCustomer!!,
                onBack = { storeViewModel.closeCustomerDetails() }
            )
        }
        else -> {
            BakiHubScreen(
                storeViewModel = storeViewModel,
                initialTab = initialTab,
                backButtonTag = backButtonTag,
                onReturnHome = onReturnHome,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun FatherBakiReadOnlyScreen(
    storeViewModel: StoreViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bakiOverview by storeViewModel.bakiOverview.collectAsState()
    val isPrivacyHidden by storeViewModel.isPrivacyHidden.collectAsState()
    val amberAccent = Color(0xFFFFC107)

    val owingCustomers = remember(bakiOverview) {
        bakiOverview.debtors
            .filter { it.currentBalance > 0.001 }
            .sortedByDescending { it.currentBalance }
    }

    BackHandler { onBack() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070907))
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .testTag("father_baki_readonly_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "father_baki_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "কার কাছে পাব",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "মোট পাব: ${formatHomeMoney(bakiOverview.totalBakiOwed, AppLanguage.BN, isPrivacyHidden)}",
                    color = VividGreen,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        }

        if (owingCustomers.isEmpty()) {
            item(key = "father_baki_empty") {
                Box(
                    modifier = Modifier
                        .fillParentMaxHeight(0.6f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "আজ এখনো কোনো বাকি নেই",
                        color = Color(0xFFD5DCD5),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            items(owingCustomers, key = { it.customer.id }) { summary ->
                    val phone = summary.customer.mobile.trim()
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF141814),
                        border = BorderStroke(1.2.dp, Color(0x52FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 68.dp)
                            .testTag("father_baki_row_${summary.customer.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Text(
                                    text = summary.customer.name.ifBlank { "কাস্টমার" },
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatHomeMoney(summary.currentBalance, AppLanguage.BN, isPrivacyHidden),
                                    color = amberAccent,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            if (phone.isNotBlank()) {
                                Surface(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                        runCatching { context.startActivity(dialIntent) }
                                    },
                                    shape = CircleShape,
                                    color = VividGreen,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .testTag("father_baki_list_call_${summary.customer.id}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "${summary.customer.name} কল করুন",
                                            tint = Color(0xFF061204),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}

/**
 * Unified Parties screen (Customers & Suppliers tabs) matching Image 3.
 */
@Composable
fun BakiHubScreen(
    storeViewModel: StoreViewModel,
    initialTab: Int = 0,
    backButtonTag: String = "baki_back_btn",
    onReturnHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val bakiOverview by storeViewModel.bakiOverview.collectAsState()
    val allCustomerSummaries by storeViewModel.allCustomerSummaries.collectAsState()
    val suppliers by storeViewModel.suppliers.collectAsState()
    val moneyNow by storeViewModel.moneyNow.collectAsState()
    val canBaki by storeViewModel.canAccessCustomersAndBakiFlow.collectAsState()
    val canReceivePayment by storeViewModel.canReceivePaymentFlow.collectAsState()
    val canSuppliers by storeViewModel.canAccessSuppliersAndPurchasesFlow.collectAsState()
    val canPaySupplier by storeViewModel.canPaySupplierFlow.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()

    val showSuppliersSection = canSuppliers && !isFatherMode && !isSimpleMode

    if (isSimpleMode && !canBaki) {
        LaunchedEffect(Unit) {
            onReturnHome?.invoke() ?: storeViewModel.navigateBackInBusiness()
        }
        return
    }

    val availableTabs = remember(canBaki, showSuppliersSection) {
        buildList {
            if (canBaki) add(0)
            if (showSuppliersSection) add(1)
        }
    }

    var activeTab by rememberSaveable(initialTab, availableTabs) {
        val preferred = initialTab.coerceIn(0, 1)
        mutableIntStateOf(
            when {
                preferred in availableTabs -> preferred
                availableTabs.isNotEmpty() -> availableTabs.first()
                else -> 0
            }
        )
    }

    LaunchedEffect(showSuppliersSection) {
        if (!showSuppliersSection && activeTab == 1) {
            activeTab = 0
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var customerToReceive by remember { mutableStateOf<CustomerBalanceSummary?>(null) }
    var supplierToPay by remember { mutableStateOf<Supplier?>(null) }

    val sortedCustomers = remember(allCustomerSummaries) {
        allCustomerSummaries.sortedWith(
            compareByDescending<CustomerBalanceSummary> { it.currentBalance }
                .thenBy { it.customer.name.lowercase() }
        )
    }

    val filteredCustomers = remember(sortedCustomers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) sortedCustomers
        else sortedCustomers.filter {
            it.customer.name.contains(q, ignoreCase = true) ||
                it.customer.mobile.contains(q, ignoreCase = true)
        }
    }

    val sortedSuppliers = remember(suppliers) {
        suppliers.sortedWith(
            compareByDescending<Supplier> { it.balance }
                .thenBy { it.name.lowercase() }
        )
    }

    val filteredSuppliers = remember(sortedSuppliers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) sortedSuppliers
        else sortedSuppliers.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.mobile.contains(q, ignoreCase = true) ||
                it.company.contains(q, ignoreCase = true)
        }
    }

    val suppliersWithDebtCount = remember(suppliers) {
        suppliers.count { it.balance > 0.001 }
    }

    val amberGive = Color(0xFFFFB300)

    BackHandler {
        if (onReturnHome != null) onReturnHome() else storeViewModel.navigateBackInBusiness()
    }

    if (showAddPartyDialog) {
        HomeAddPartyDialog(
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            canAddCustomer = canBaki,
            canAddSupplier = showSuppliersSection,
            initialIsSupplier = (activeTab == 1 && showSuppliersSection),
            onDismiss = { showAddPartyDialog = false }
        )
    }

    if (customerToReceive != null) {
        val target = customerToReceive!!
        ReceivePaymentDialog(
            customer = target.customer,
            currentBalance = target.currentBalance,
            currentLanguage = currentLanguage,
            isFatherMode = isFatherMode,
            onDismiss = { customerToReceive = null },
            onConfirm = { amount, note, date ->
                storeViewModel.recordCustomerPayment(
                    customer = target.customer,
                    amount = amount,
                    note = note,
                    paymentDate = date,
                    onSuccess = { customerToReceive = null }
                )
            }
        )
    }

    if (supplierToPay != null) {
        SingleSupplierPayDialog(
            supplier = supplierToPay!!,
            storeViewModel = storeViewModel,
            currentLanguage = currentLanguage,
            onDismiss = { supplierToPay = null }
        )
    }

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
                        center = Offset(size.width * 0.26f, size.height * 0.14f),
                        radius = size.width * 0.78f
                    )
                )
            }
            .testTag("baki_hub_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("parties_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Centered Top Bar with Back Arrow
            item(key = "baki_top_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (onReturnHome != null) onReturnHome() else storeViewModel.navigateBackInBusiness() },
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
                        text = if (isFatherMode || isSimpleMode) {
                            ShopLocale.get("customers_baki", currentLanguage)
                        } else if (currentLanguage == AppLanguage.BN) "পার্টি ও হিসাব" else "Parties",
                        color = TextPrimary,
                        fontSize = if (isFatherMode) 23.sp else 20.sp,
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
                // Glowing Split Hero Card: TO RECEIVE | TO GIVE (TO RECEIVE only in Father mode)
                item {
                    val heroShape = RoundedCornerShape(22.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 16.dp,
                                shape = heroShape,
                                ambientColor = VividGreen.copy(alpha = 0.30f),
                                spotColor = VividGreen.copy(alpha = 0.42f)
                            )
                            .testTag("parties_summary_card"),
                        shape = heroShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(VividGreen.copy(alpha = 0.90f), VividGreen.copy(alpha = 0.36f))
                            )
                        )
                    ) {
                        Row(
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
                                .padding(horizontal = 14.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = canBaki) { activeTab = 0 },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "পাব" else "TO RECEIVE",
                                    color = TextSecondary,
                                    fontSize = if (isFatherMode) 13.sp else 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.1.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = if (canBaki) {
                                        ShopLocale.formatMoney(bakiOverview.totalBakiOwed, currentLanguage)
                                    } else {
                                        "—"
                                    },
                                    color = VividGreen,
                                    fontSize = if (isFatherMode) 28.sp else 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (canBaki) {
                                        if (currentLanguage == AppLanguage.BN) {
                                            "${bakiOverview.debtorCount} জন কাস্টমার"
                                        } else {
                                            "${bakiOverview.debtorCount} ${if (bakiOverview.debtorCount == 1L) "customer" else "customers"}"
                                        }
                                    } else {
                                        ""
                                    },
                                    color = VividGreen.copy(alpha = 0.85f),
                                    fontSize = if (isFatherMode) 13.sp else 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            if (showSuppliersSection) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(54.dp)
                                        .background(Color(0x28FFFFFF))
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = true) { activeTab = 1 },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) "দেব" else "TO GIVE",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.1.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = ShopLocale.formatMoney(moneyNow.debtToSuppliers, currentLanguage),
                                        color = amberGive,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) {
                                            "$suppliersWithDebtCount জন মহাজন"
                                        } else {
                                            "$suppliersWithDebtCount ${if (suppliersWithDebtCount == 1) "supplier" else "suppliers"}"
                                        },
                                        color = amberGive.copy(alpha = 0.90f),
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

                // Capsule 2-Tab Switcher (hidden if only Customers tab exists in Father mode)
                if (availableTabs.size > 1) {
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
                                if (canBaki) {
                                    PartiesTabPill(
                                        label = "${if (currentLanguage == AppLanguage.BN) "কাস্টমার" else "Customers"} (${allCustomerSummaries.size})",
                                        isSelected = activeTab == 0,
                                        onClick = { activeTab = 0 },
                                        testTag = "parties_tab_customers",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (showSuppliersSection) {
                                    PartiesTabPill(
                                        label = "${if (currentLanguage == AppLanguage.BN) "মহাজন" else "Suppliers"} (${suppliers.size})",
                                        isSelected = activeTab == 1,
                                        onClick = { activeTab = 1 },
                                        testTag = "parties_tab_suppliers",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Pill Search Bar (No unbuilt category filter pills!)
                item {
                    val searchShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(searchShape)
                            .background(if (isSearchFocused) Color(0xD91A1E1A) else Color(0xBF151815))
                            .border(
                                width = 1.dp,
                                color = if (isSearchFocused) VividGreen.copy(alpha = 0.72f) else Color(0x28FFFFFF),
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
                                tint = if (isSearchFocused) VividGreen else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) {
                                            "নাম বা মোবাইল নম্বর খুঁজুন..."
                                        } else {
                                            "Search name or phone..."
                                        },
                                        color = TextMuted,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    cursorBrush = SolidColor(VividGreen),
                                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.5.sp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isSearchFocused = it.isFocused }
                                        .testTag("baki_search_input")
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
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

                // Active Tab Feed
                if (activeTab == 0 && canBaki) {
                    if (filteredCustomers.isEmpty()) {
                        item(key = "parties_customers_empty") {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) {
                                            "No matching customers found"
                                        } else if (currentLanguage == AppLanguage.BN) {
                                            "কোনো কাস্টমার পাওয়া যায়নি"
                                        } else {
                                            "No customers added yet"
                                        },
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    if (!isSimpleMode) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) {
                                                "নতুন পার্টি যোগ করতে নিচের '+ Add Party' চাপুন।"
                                            } else {
                                                "Tap '+ Add Party' below to add a customer."
                                            },
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredCustomers, key = { it.customer.id.ifBlank { it.customer.name } }) { summary ->
                            CustomerRowCard(
                                summary = summary,
                                currentLanguage = currentLanguage,
                                isFatherMode = isFatherMode,
                                onReceiveClick = if (summary.currentBalance > 0.001 && canReceivePayment) {
                                    { customerToReceive = summary }
                                } else null,
                                onClick = { storeViewModel.openCustomerDetails(summary.customer) }
                            )
                        }
                    }
                } else if (activeTab == 1 && showSuppliersSection) {
                    if (filteredSuppliers.isEmpty()) {
                        item(key = "parties_suppliers_empty") {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) {
                                            "No matching suppliers found"
                                        } else if (currentLanguage == AppLanguage.BN) {
                                            "কোনো মহাজন পাওয়া যায়নি"
                                        } else {
                                            "No suppliers added yet"
                                        },
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) {
                                            "নতুন মহাজন যোগ করতে নিচের '+ Add Party' চাপুন।"
                                        } else {
                                            "Tap '+ Add Party' below to add a supplier."
                                        },
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredSuppliers, key = { it.id.ifBlank { it.name } }) { supplier ->
                            SupplierPartyRowCard(
                                supplier = supplier,
                                currentLanguage = currentLanguage,
                                onPayClick = if (supplier.balance > 0.001 && canPaySupplier) {
                                    { supplierToPay = supplier }
                                } else null,
                                onClick = {
                                    storeViewModel.selectStatementSupplier(supplier)
                                    storeViewModel.navigateInBusiness(BusinessNavDestination.REPORT_SUPPLIER_STATEMENT)
                                }
                            )
                        }
                    }
            }
        }

        // Bottom-right Glowing Vivid Green "+ Add Party" Pill FAB (hidden in Simple Mode & Father Mode)
        if (!isSimpleMode && !isFatherMode && (canBaki || canSuppliers)) {
            val fabShape = RoundedCornerShape(28.dp)
            Surface(
                onClick = { showAddPartyDialog = true },
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
                    .testTag("btn_add_party")
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
                        text = if (currentLanguage == AppLanguage.BN) "নতুন পার্টি" else "Add Party",
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
}

@Composable
private fun PartiesTabPill(
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Dark glass Customer party row matching Image 3 (Avatar, Name, Phone, Due/Settled + Receive pill).
 */
@Composable
fun CustomerRowCard(
    summary: CustomerBalanceSummary,
    currentLanguage: AppLanguage,
    isFatherMode: Boolean = false,
    onReceiveClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val cust = summary.customer
    val hasOwed = summary.currentBalance > 0.001
    val cardShape = RoundedCornerShape(18.dp)
    val initial = remember(cust.name) {
        cust.name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "C"
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
            .testTag("customer_card_${cust.id.ifBlank { cust.name }}"),
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
                    text = initial,
                    color = TextPrimary,
                    fontSize = if (isFatherMode) 19.sp else 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = cust.name,
                    color = TextPrimary,
                    fontSize = if (isFatherMode) 17.sp else 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cust.mobile.ifBlank {
                        if (currentLanguage == AppLanguage.BN) "মোবাইল নেই" else "No phone"
                    },
                    color = TextSecondary,
                    fontSize = if (isFatherMode) 14.sp else 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = ShopLocale.formatMoney(summary.currentBalance, currentLanguage),
                    color = if (hasOwed) VividGreen else TextPrimary,
                    fontSize = if (isFatherMode) 19.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                if (hasOwed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "বাকি" else "Due",
                            color = TextSecondary,
                            fontSize = if (isFatherMode) 13.sp else 11.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (onReceiveClick != null) {
                            Surface(
                                onClick = onReceiveClick,
                                shape = RoundedCornerShape(14.dp),
                                color = VividGreen,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = if (isFatherMode) 42.dp else 28.dp)
                                    .testTag("btn_receive_customer_${cust.id}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) "গ্রহণ" else "Receive",
                                        color = Color(0xFF0A1406),
                                        fontSize = if (isFatherMode) 13.5.sp else 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(
                                            horizontal = if (isFatherMode) 14.dp else 12.dp,
                                            vertical = if (isFatherMode) 8.dp else 4.dp
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x225DD62C),
                        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "পরিশোধিত" else "Settled",
                            color = VividGreen,
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
}

/**
 * Dark glass Supplier party row matching Image 3.
 */
@Composable
private fun SupplierPartyRowCard(
    supplier: Supplier,
    currentLanguage: AppLanguage,
    onPayClick: (() -> Unit)?,
    onClick: () -> Unit
) {
    val hasOwed = supplier.balance > 0.001
    val amberGive = Color(0xFFFFB300)
    val cardShape = RoundedCornerShape(18.dp)
    val initial = remember(supplier.name) {
        supplier.name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "S"
    }
    val subtitle = remember(supplier.mobile, supplier.company, currentLanguage) {
        listOfNotNull(
            supplier.mobile.takeIf { it.isNotBlank() },
            supplier.company.takeIf { it.isNotBlank() }
        ).joinToString(" · ").ifBlank {
            if (currentLanguage == AppLanguage.BN) "মোবাইল নেই" else "No phone"
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
            .testTag("supplier_item_${supplier.id}"),
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
                    text = initial,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = supplier.name,
                    color = TextPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = ShopLocale.formatMoney(supplier.balance, currentLanguage),
                    color = if (hasOwed) amberGive else TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                if (hasOwed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "পাবে" else "Owed",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (onPayClick != null) {
                            Surface(
                                onClick = onPayClick,
                                shape = RoundedCornerShape(14.dp),
                                color = amberGive,
                                modifier = Modifier.testTag("btn_pay_supplier_${supplier.id}")
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "পরিশোধ" else "Pay",
                                    color = Color(0xFF141004),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x225DD62C),
                        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "পরিশোধিত" else "Settled",
                            color = VividGreen,
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
}

@Composable
private fun SingleSupplierPayDialog(
    supplier: Supplier,
    storeViewModel: StoreViewModel,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val accounts by storeViewModel.accounts.collectAsState()
    val lastUsedAccountId by storeViewModel.lastUsedAccountId.collectAsState()
    val canCashBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()

    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val defaultAcc = accounts.find { it.id == lastUsedAccountId }
        ?: accounts.find { it.isDefault || it.id == "cash_in_hand" }
        ?: accounts.firstOrNull()
    var selectedAccId by remember { mutableStateOf(defaultAcc?.id ?: "cash_in_hand") }
    var selectedAccName by remember { mutableStateOf(defaultAcc?.name ?: "Cash in Hand") }

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
                .testTag("single_supplier_pay_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "মহাজন পরিশোধ (Payment Out)" else "Pay Supplier (Payment Out)",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${supplier.name} · ${if (currentLanguage == AppLanguage.BN) "দেব" else "Owed"}: ${ShopLocale.formatMoney(supplier.balance, currentLanguage)}",
                    color = Color(0xFFFFB300),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                GlassTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMsg = null
                    },
                    label = if (currentLanguage == AppLanguage.BN) "পরিশোধের পরিমাণ (৳)" else "Payment Amount (৳)",
                    placeholder = "0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "supplier_pay_amount_input"
                )

                if (supplier.balance > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x205DD62C),
                        border = BorderStroke(1.dp, VividGreen),
                        modifier = Modifier.clickable {
                            amountText = supplier.balance.toInt().toString()
                            errorMsg = null
                        }
                    ) {
                        Text(
                            text = "Full (${ShopLocale.formatMoney(supplier.balance, currentLanguage)})",
                            color = VividGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (canCashBank) {
                    AccountPickerField(
                        selectedAccountId = selectedAccId,
                        selectedAccountName = selectedAccName,
                        onAccountSelected = { acc ->
                            selectedAccId = acc.id
                            selectedAccName = acc.name
                        },
                        storeViewModel = storeViewModel,
                        label = if (currentLanguage == AppLanguage.BN) "যে অ্যাকাউন্ট থেকে দেওয়া হবে" else "Paid From Account"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                GlassTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = if (currentLanguage == AppLanguage.BN) "নোট (ঐচ্ছিক)" else "Note (Optional)",
                    placeholder = "e.g. Cash paid to supplier",
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg!!, color = AlertRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                VividButton(
                    text = if (currentLanguage == AppLanguage.BN) "পেমেন্ট নিশ্চিত করুন" else "Confirm Payment Out",
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            errorMsg = "Enter a valid amount > 0"
                            return@VividButton
                        }
                        storeViewModel.recordSupplierPayment(
                            supplier = supplier,
                            amount = amt,
                            accountId = selectedAccId,
                            accountName = selectedAccName,
                            note = noteText,
                            date = System.currentTimeMillis(),
                            onSuccess = onDismiss
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "confirm_supplier_pay_btn"
                )
            }
        }
    }
}

/**
 * Step 5: Customer details screen.
 * Shows customer name, mobile, address, current balance (big, green when zero, amber when owed),
 * total purchases, last purchase date, and a ledger (newest first) with every Baki sale and payment,
 * plus running balance after each line!
 */
@Composable
fun CustomerDetailsScreen(
    storeViewModel: StoreViewModel,
    customer: Customer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val summary by storeViewModel.selectedCustomerSummary.collectAsState()
    val ledger by storeViewModel.selectedCustomerLedger.collectAsState()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCannotDeleteMessage by remember { mutableStateOf(false) }

    val activeSummary = summary ?: CustomerBalanceSummary(
        customer = customer,
        totalBakiSales = 0.0,
        totalPayments = 0.0,
        currentBalance = customer.currentBalance,
        totalPurchases = 0.0,
        lastPurchaseDate = null
    )

    BackHandler { onBack() }

    if (showPaymentDialog) {
        ReceivePaymentDialog(
            customer = activeSummary.customer,
            currentBalance = activeSummary.currentBalance,
            currentLanguage = currentLanguage,
            isFatherMode = isFatherMode,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, note, dateMillis ->
                storeViewModel.recordCustomerPayment(
                    customer = activeSummary.customer,
                    amount = amount,
                    note = note,
                    paymentDate = dateMillis
                ) {
                    showPaymentDialog = false
                }
            }
        )
    }

    if (showReminderDialog) {
        ShareReminderDialog(
            customerName = activeSummary.customer.name,
            customerPhone = activeSummary.customer.mobile,
            amountOwed = activeSummary.currentBalance,
            storeName = store?.name ?: "StoreOS",
            storePhone = store?.phone ?: "",
            currentLanguage = currentLanguage,
            onDismiss = { showReminderDialog = false }
        )
    }

    if (showEditDialog) {
        val existingCustomers by storeViewModel.customers.collectAsState()
        AddEditCustomerDialog(
            customerToEdit = activeSummary.customer,
            existingCustomers = existingCustomers,
            currentLanguage = currentLanguage,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, mobile, address, notes ->
                storeViewModel.updateCustomer(
                    activeSummary.customer.copy(
                        name = name,
                        mobile = mobile,
                        address = address,
                        notes = notes
                    )
                ) {
                    showEditDialog = false
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            modifier = Modifier.safeDialogWindowPadding(),
            properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = true),
            title = {
                ConfigureEdgeToEdgeDialogWindow()
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "কাস্টমার মুছে ফেলবেন?" else "Delete Customer?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN)
                            "'${activeSummary.customer.name}' কে স্থায়ীভাবে মুছে ফেলতে চান?"
                        else "Are you sure you want to delete customer '${activeSummary.customer.name}'?",
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        storeViewModel.deleteCustomer(activeSummary.customer) {
                            onBack()
                        }
                    }
                ) {
                    Text(if (currentLanguage == AppLanguage.BN) "মুছে ফেলুন" else "Delete", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(if (currentLanguage == AppLanguage.BN) "বাতিল" else "Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    if (showCannotDeleteMessage) {
        AlertDialog(
            onDismissRequest = { showCannotDeleteMessage = false },
            modifier = Modifier.safeDialogWindowPadding(),
            properties = edgeToEdgeDialogProperties(usePlatformDefaultWidth = true),
            title = {
                ConfigureEdgeToEdgeDialogWindow()
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "মুছে ফেলা যাবে না" else "Cannot Delete",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN)
                            "এই কাস্টমারের পূর্বের বিক্রয় বা বাকি ব্যালেন্স রয়েছে। শুধুমাত্র ব্যালেন্স শূন্য এবং কোনো বিক্রয় না থাকলে মুছে ফেলা সম্ভব।"
                        else "This customer has sales or an outstanding balance. You can only delete a customer when their balance is zero and they have no sales.",
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCannotDeleteMessage = false }) {
                    Text("OK", color = VividGreen, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .testTag("customer_details_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        item(key = "customer_details_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x20FFFFFF))
                        .testTag("customer_details_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeSummary.customer.name,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeSummary.customer.mobile.ifBlank { "No phone" },
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                val canBaki by storeViewModel.canAccessCustomersAndBakiFlow.collectAsState()
                val canReceivePayment by storeViewModel.canReceivePaymentFlow.collectAsState()
                val isCashSale = activeSummary.customer.name.equals("Cash Sale", ignoreCase = true)
                if (!isCashSale && canBaki) {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x20FFFFFF))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Customer", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    if (!isFatherMode && !isSimpleMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (CalculationEngine.canDeleteCustomer(activeSummary)) {
                                    showDeleteConfirmDialog = true
                                } else {
                                    showCannotDeleteMessage = true
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x20FFFFFF))
                                .testTag("customer_delete_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
            // Big Balance Card (Green when zero, amber when owed)
            item {
                val hasOwed = activeSummary.currentBalance > 0.001
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_balance_card")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "বর্তমান বকেয়া বাকি" else "Current Balance Owed",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (hasOwed) Color(0x33FFA000) else Color(0x265DD62C)
                            ) {
                                Text(
                                    text = if (hasOwed) (if (currentLanguage == AppLanguage.BN) "বাকি আছে" else "Due")
                                    else (if (currentLanguage == AppLanguage.BN) "পরিশোধিত" else "All Clear"),
                                    color = if (hasOwed) Color(0xFFFFB300) else VividGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ShopLocale.formatMoney(activeSummary.currentBalance, currentLanguage),
                            color = if (hasOwed) Color(0xFFFFB300) else VividGreen,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0x1FFFFFFF))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Total purchases & last purchase date (Step 8)
                        val lastDateStr = activeSummary.lastPurchaseDate?.let {
                            ShopLocale.formatDateOnly(it, currentLanguage)
                        } ?: (if (currentLanguage == AppLanguage.BN) "নেই" else "None")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "মোট ক্রয়" else "Total Purchases",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = ShopLocale.formatMoney(activeSummary.totalPurchases, currentLanguage),
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "সর্বশেষ ক্রয়" else "Last Purchase",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = lastDateStr,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons: Receive Payment, Call, Share Reminder, Edit
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val canReceivePaymentAction by storeViewModel.canReceivePaymentFlow.collectAsState()
                    if (canReceivePaymentAction) {
                        VividButton(
                            text = if (currentLanguage == AppLanguage.BN) "আদায় নিন" else "Receive Payment",
                            onClick = { showPaymentDialog = true },
                            icon = Icons.Default.CallReceived,
                            enabled = activeSummary.currentBalance > 0.001,
                            modifier = Modifier.weight(1.3f),
                            testTag = "receive_payment_btn"
                        )
                    }

                    GlassOutlinedButton(
                        text = if (currentLanguage == AppLanguage.BN) "কল" else "Call",
                        onClick = {
                            if (activeSummary.customer.mobile.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${activeSummary.customer.mobile}"))
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "No mobile number provided", Toast.LENGTH_SHORT).show()
                            }
                        },
                        icon = Icons.Default.Call,
                        modifier = Modifier.weight(0.9f),
                        testTag = "call_customer_btn"
                    )

                    GlassOutlinedButton(
                        text = if (currentLanguage == AppLanguage.BN) "রিমাইন্ডার" else "Reminder",
                        onClick = { showReminderDialog = true },
                        icon = Icons.Default.Share,
                        enabled = activeSummary.currentBalance > 0.001,
                        modifier = Modifier.weight(1f),
                        testTag = "share_reminder_btn"
                    )
                }
            }

            // Ledger Section Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = VividGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "খতিয়ান বিবরণ (লেজার)" else "Ledger History",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = if (currentLanguage == AppLanguage.BN)
                            "${ShopLocale.formatNumber(ledger.size, currentLanguage)}টি এন্ট্রি"
                        else
                            "${ShopLocale.formatNumber(ledger.size, currentLanguage)} " + if (ledger.size == 1) "entry" else "entries",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            if (ledger.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "এই কাস্টমারের কোনো বাকির খতিয়ান নেই।"
                            else "No ledger entries yet. Credit sales and payments will be listed here.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                items(ledger, key = { it.id }) { entry ->
                    LedgerEntryCard(entry = entry, currentLanguage = currentLanguage)
                }
            }
    }
}

/**
 * Single ledger entry displaying Baki sale or Payment In, with running balance after each line.
 */
@Composable
fun LedgerEntryCard(
    entry: CustomerLedgerEntry,
    currentLanguage: AppLanguage
) {
    val isSale = entry.type == LedgerEntryType.BAKI_SALE
    val formattedDate = remember(entry.timestamp, currentLanguage) {
        if (entry.timestamp > 0L) ShopLocale.formatDateLong(entry.timestamp, currentLanguage)
        else if (currentLanguage == AppLanguage.BN) "সাম্প্রতিক" else "Recently"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        testTag = "ledger_row_${entry.referenceNo}"
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isSale) Color(0x33FFA000) else Color(0x265DD62C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSale) Icons.Default.Receipt else Icons.Default.Payment,
                            contentDescription = null,
                            tint = if (isSale) Color(0xFFFFB300) else VividGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.referenceNo,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSale) Color(0x33FFA000) else Color(0x265DD62C)
                            ) {
                                Text(
                                    text = when {
                                        isSale && entry.paidAmount > 0.001 -> if (currentLanguage == AppLanguage.BN) "আংশিক বাকি" else "Part Paid"
                                        isSale -> if (currentLanguage == AppLanguage.BN) "বাকি বিক্রয়" else "Baki Sale"
                                        else -> if (currentLanguage == AppLanguage.BN) "আদায়" else "Payment In"
                                    },
                                    color = if (isSale) Color(0xFFFFB300) else VividGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = formattedDate,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // Amount
                Text(
                    text = (if (isSale) "- " else "+ ") + ShopLocale.formatMoney(entry.amount, currentLanguage),
                    color = if (isSale) Color(0xFFFFB300) else VividGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }

            if (isSale && entry.paidAmount > 0.001) {
                Spacer(modifier = Modifier.height(6.dp))
                val totalVal = if (entry.totalAmount > 0.0) entry.totalAmount else entry.amount + entry.paidAmount
                val breakdownText = if (currentLanguage == AppLanguage.BN) {
                    "মোট ${ShopLocale.formatMoney(totalVal, currentLanguage)} · দিয়েছে ${ShopLocale.formatMoney(entry.paidAmount, currentLanguage)} · বাকি ${ShopLocale.formatMoney(entry.amount, currentLanguage)}"
                } else {
                    "Total ${ShopLocale.formatMoney(totalVal, currentLanguage)} · Paid ${ShopLocale.formatMoney(entry.paidAmount, currentLanguage)} · Due ${ShopLocale.formatMoney(entry.amount, currentLanguage)}"
                }
                Text(
                    text = breakdownText,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            if (entry.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${entry.note}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0x14FFFFFF))
            Spacer(modifier = Modifier.height(4.dp))

            // Running balance after this transaction line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "অবশিষ্ট বাকি ব্যালেন্স:" else "Running Balance:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = ShopLocale.formatMoney(entry.runningBalance, currentLanguage),
                    color = if (entry.runningBalance > 0.0) Color(0xFFFFB300) else VividGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Step 6: Receive Payment (Payment In) Dialog.
 * Amount, date, note, method is Cash for now.
 * Blocks an amount above customer's current balance with a clear message.
 */
@Composable
fun ReceivePaymentDialog(
    customer: Customer,
    currentBalance: Double,
    currentLanguage: AppLanguage,
    isFatherMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Long) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingPaymentConfirm by remember { mutableStateOf<Triple<Double, String, Long>?>(null) }

    if (pendingPaymentConfirm != null) {
        val (amt, noteStr, dateMs) = pendingPaymentConfirm!!
        FatherConfirmActionDialog(
            title = ShopLocale.get("father_confirm_payment_title", currentLanguage),
            body = "${customer.name} · ${ShopLocale.formatMoney(amt, currentLanguage)}",
            confirmLabel = ShopLocale.get("confirm", currentLanguage),
            currentLanguage = currentLanguage,
            onConfirm = {
                pendingPaymentConfirm = null
                onConfirm(amt, noteStr, dateMs)
            },
            onDismiss = { pendingPaymentConfirm = null }
        )
    } else {
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "বাকি আদায় (Payment In)" else "Receive Payment (Payment In)",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${customer.name} • ${if (currentLanguage == AppLanguage.BN) "বাকি" else "Due"}: ${ShopLocale.formatMoney(currentBalance, currentLanguage)}",
                    color = Color(0xFFFFB300),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(14.dp))

                GlassTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        val parsed = it.toDoubleOrNull() ?: 0.0
                        errorMessage = if (parsed > (currentBalance + 0.001)) {
                            val balStr = ShopLocale.formatMoney(currentBalance, currentLanguage)
                            if (currentLanguage == AppLanguage.BN) "আদায়ের পরিমাণ বর্তমান বাকি $balStr এর বেশি হতে পারবে না"
                            else "Amount cannot exceed outstanding balance of $balStr"
                        } else null
                    },
                    label = if (currentLanguage == AppLanguage.BN) "আদায়ের পরিমাণ (৳)" else "Payment Amount (৳)",
                    placeholder = ShopLocale.digits("0", currentLanguage),
                    leadingIcon = Icons.Default.Payment,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    testTag = "receive_payment_amount_input"
                )

                // Quick presets: Full due and Half due
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x205DD62C),
                        border = BorderStroke(1.dp, VividGreen),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                amountInput = currentBalance.toInt().toString()
                                errorMessage = null
                            }
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "সম্পূর্ণ (${ShopLocale.formatMoney(currentBalance, currentLanguage)})"
                            else "Full (${ShopLocale.formatMoney(currentBalance, currentLanguage)})",
                            color = VividGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp),
                            maxLines = 1
                        )
                    }

                    if (currentBalance >= 20.0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x20FFFFFF),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    amountInput = (currentBalance / 2.0).toInt().toString()
                                    errorMessage = null
                                }
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "অর্ধেক (${ShopLocale.formatMoney(currentBalance / 2.0, currentLanguage)})"
                                else "Half (${ShopLocale.formatMoney(currentBalance / 2.0, currentLanguage)})",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = if (currentLanguage == AppLanguage.BN) "নোট (ঐচ্ছিক)" else "Note (Optional)",
                    placeholder = "e.g. Cash received by owner",
                    leadingIcon = Icons.Default.Note,
                    testTag = "receive_payment_note_input"
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "পেমেন্ট মাধ্যম:" else "Payment Method:",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x335DD62C)
                    ) {
                        Text(
                            text = "Cash",
                            color = VividGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = AlertRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val parsedAmt = amountInput.toDoubleOrNull() ?: 0.0
                val isValid = parsedAmt > 0.0 && parsedAmt <= (currentBalance + 0.001)

                VividButton(
                    text = if (currentLanguage == AppLanguage.BN) "আদায় নিশ্চিত করুন" else "Confirm Payment",
                    onClick = {
                        if (isValid) {
                            val nowMs = System.currentTimeMillis()
                            if (isFatherMode) {
                                pendingPaymentConfirm = Triple(parsedAmt, noteInput, nowMs)
                            } else {
                                onConfirm(parsedAmt, noteInput, nowMs)
                            }
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "confirm_receive_payment_btn"
                )
            }
        }
        }
    }
}

/**
 * Step 7: Share Reminder Dialog.
 * Builds polite reminder message in Bangla or English with language switch.
 * Shares through the phone's standard share sheet.
 */
@Composable
fun ShareReminderDialog(
    customerName: String,
    customerPhone: String,
    amountOwed: Double,
    storeName: String,
    storePhone: String,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isBangla by remember { mutableStateOf(currentLanguage == AppLanguage.BN) }

    val formattedAmount = ShopLocale.formatMoney(amountOwed, if (isBangla) AppLanguage.BN else AppLanguage.EN)

    val reminderMessage = remember(isBangla, customerName, amountOwed, storeName, storePhone) {
        if (isBangla) {
            "প্রিয় $customerName,\n" +
                    "$storeName ${if (storePhone.isNotBlank()) "($storePhone) " else ""}থেকে বিনীতভাবে জানানো যাচ্ছে যে আপনার বকেয়া বাকি রয়েছে $formattedAmount। " +
                    "অনুগ্রহ করে সুবিধাজনক সময়ে পরিশোধ করবেন।\nধন্যবাদ!"
        } else {
            "Dear $customerName,\n" +
                    "This is a polite reminder from $storeName ${if (storePhone.isNotBlank()) "($storePhone) " else ""}" +
                    "regarding your outstanding due balance of $formattedAmount. " +
                    "Kindly settle this at your earliest convenience.\nThank you!"
        }
    }

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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBangla) "বাকি রিমাইন্ডার শেয়ার" else "Share Payment Reminder",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Language switch inside dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isBangla) Color(0x335DD62C) else Color(0x20FFFFFF),
                        border = BorderStroke(1.dp, if (!isBangla) VividGreen else GlassBorder),
                        modifier = Modifier.clickable { isBangla = false }
                    ) {
                        Text(
                            text = "English",
                            color = if (!isBangla) VividGreen else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBangla) Color(0x335DD62C) else Color(0x20FFFFFF),
                        border = BorderStroke(1.dp, if (isBangla) VividGreen else GlassBorder),
                        modifier = Modifier.clickable { isBangla = true }
                    ) {
                        Text(
                            text = "বাংলা",
                            color = if (isBangla) VividGreen else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Message Preview Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33202020),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = reminderMessage,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassOutlinedButton(
                        text = if (isBangla) "কপি করুন" else "Copy Text",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Baki Reminder", reminderMessage)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, if (isBangla) "মেসেজ কপি করা হয়েছে" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "copy_reminder_btn"
                    )

                    VividButton(
                        text = if (isBangla) "শেয়ার করুন" else "Share via...",
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, reminderMessage)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Reminder via"))
                        },
                        icon = Icons.Default.Share,
                        modifier = Modifier.weight(1.2f),
                        testTag = "send_share_reminder_btn"
                    )
                }
            }
        }
    }
}

/**
 * Step 2: Add Customer and Edit Customer screen/dialog.
 * Name is required. Warns if mobile already exists.
 * On phone app: adds "Pick from phone contacts" (asks permission first).
 */
@Composable
fun AddEditCustomerDialog(
    customerToEdit: Customer? = null,
    existingCustomers: List<Customer> = emptyList(),
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (name: String, mobile: String, address: String, notes: String) -> Unit
) {
    var nameInput by remember { mutableStateOf(customerToEdit?.name ?: "") }
    var mobileInput by remember { mutableStateOf(customerToEdit?.mobile ?: "") }
    var addressInput by remember { mutableStateOf(customerToEdit?.address ?: "") }
    var notesInput by remember { mutableStateOf(customerToEdit?.notes ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    val isEditing = customerToEdit != null

    // Check for duplicate mobile number
    val cleanMobile = remember(mobileInput) { mobileInput.trim().replace(" ", "").replace("-", "") }
    val duplicateMobileExists = remember(cleanMobile, existingCustomers, customerToEdit) {
        if (cleanMobile.length >= 6) {
            existingCustomers.any {
                it.id != customerToEdit?.id &&
                    it.mobile.trim().replace(" ", "").replace("-", "") == cleanMobile
            }
        } else false
    }

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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) (if (currentLanguage == AppLanguage.BN) "ক্রেতা ঠিক করুন" else "Edit Customer")
                        else (if (currentLanguage == AppLanguage.BN) "নতুন কাস্টমার যোগ করুন" else "Add New Customer"),
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        nameError = if (it.trim().isBlank()) "Name is required" else null
                    },
                    label = if (currentLanguage == AppLanguage.BN) "কাস্টমারের নাম *" else "Customer Name *",
                    placeholder = "e.g. Rahim Ahmed",
                    leadingIcon = Icons.Default.Person,
                    testTag = "customer_name_input"
                )

                if (nameError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(nameError ?: "", color = AlertRed, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassTextField(
                    value = mobileInput,
                    onValueChange = { mobileInput = it },
                    label = if (currentLanguage == AppLanguage.BN) "মোবাইল নম্বর (ঐচ্ছিক)" else "Mobile Number (Optional)",
                    placeholder = "017...",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    testTag = "customer_mobile_input"
                )

                if (duplicateMobileExists) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "সতর্কতা: এই মোবাইল নম্বরে ইতিমধ্যে একজন কাস্টমার রয়েছে"
                            else "Warning: Another customer already has this mobile number",
                            color = Color(0xFFFFB300),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    label = if (currentLanguage == AppLanguage.BN) "ঠিকানা (ঐচ্ছিক)" else "Address (Optional)",
                    placeholder = "e.g. Mirpur, Dhaka",
                    leadingIcon = Icons.Default.LocationOn,
                    testTag = "customer_address_input"
                )

                Spacer(modifier = Modifier.height(10.dp))

                GlassTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = if (currentLanguage == AppLanguage.BN) "নোট (ঐচ্ছিক)" else "Notes (Optional)",
                    placeholder = "Special notes about this customer...",
                    leadingIcon = Icons.Default.Note,
                    testTag = "customer_notes_input"
                )

                Spacer(modifier = Modifier.height(16.dp))

                VividButton(
                    text = if (isEditing) (if (currentLanguage == AppLanguage.BN) "সংরক্ষণ করুন" else "Update Customer")
                    else (if (currentLanguage == AppLanguage.BN) "যোগ করুন" else "Save Customer"),
                    onClick = {
                        if (nameInput.trim().isBlank()) {
                            nameError = "Customer name is required"
                            return@VividButton
                        }
                        onConfirm(nameInput.trim(), mobileInput.trim(), addressInput.trim(), notesInput.trim())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "confirm_save_customer_btn"
                )
            }
        }
    }
}

/**
 * Step 3: Checkout customer selector dialog.
 * Search by name or mobile, "+ Add new customer" inside picker, Cash Sale option.
 */
@Composable
fun CustomerPickerDialog(
    customerSummaries: List<CustomerBalanceSummary>,
    currentLanguage: AppLanguage,
    canAddCustomer: Boolean = true,
    titleOverride: String? = null,
    showCashSaleOption: Boolean = true,
    isFatherMode: Boolean = false,
    onDismiss: () -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onSelectCashSale: () -> Unit,
    onAddNewCustomer: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(customerSummaries, searchQuery) {
        if (searchQuery.isBlank()) customerSummaries
        else customerSummaries.filter {
            it.customer.name.contains(searchQuery, ignoreCase = true) ||
                    it.customer.mobile.contains(searchQuery)
        }
    }

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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titleOverride ?: if (isFatherMode || currentLanguage == AppLanguage.BN) "কাস্টমার নির্বাচন করুন" else "Select Customer",
                        color = TextPrimary,
                        fontSize = if (isFatherMode) 21.sp else 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SearchGlassField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    label = if (isFatherMode || currentLanguage == AppLanguage.BN) "নাম বা মোবাইল লিখুন..." else "Search by name or mobile...",
                    leadingIcon = Icons.Default.Search,
                    testTag = "picker_search_input"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons: Cash Sale & Add New Customer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showCashSaleOption) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x20FFFFFF),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSelectCashSale()
                                    onDismiss()
                                }
                                .testTag("picker_cash_sale_option")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFatherMode || currentLanguage == AppLanguage.BN) "নগদ বিক্রি" else "Cash Sale",
                                    color = TextPrimary,
                                    fontSize = if (isFatherMode) 16.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (canAddCustomer) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x335DD62C),
                            border = BorderStroke(1.dp, VividGreen),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = if (isFatherMode) 52.dp else 40.dp)
                                .clickable {
                                    onDismiss()
                                    onAddNewCustomer()
                                }
                                .testTag("picker_add_new_customer_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = VividGreen, modifier = Modifier.size(if (isFatherMode) 22.dp else 16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFatherMode) "নতুন ক্রেতা" else if (currentLanguage == AppLanguage.BN) "+ নতুন ক্রেতা" else "+ New Customer",
                                    color = VividGreen,
                                    fontSize = if (isFatherMode) 19.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 100.dp, max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.customer.id.ifBlank { it.customer.name } }) { summary ->
                        val cust = summary.customer
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x261E1E1E),
                            border = BorderStroke(1.dp, Color(0x1FFFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = if (isFatherMode) 60.dp else 48.dp)
                                .clickable {
                                    onSelectCustomer(cust)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cust.name,
                                        color = TextPrimary,
                                        fontSize = if (isFatherMode) 20.sp else 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (cust.mobile.isNotBlank()) {
                                        Text(
                                            text = cust.mobile,
                                            color = TextMuted,
                                            fontSize = if (isFatherMode) 15.sp else 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (summary.currentBalance > 0.0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = ShopLocale.formatMoney(summary.currentBalance, if (isFatherMode) AppLanguage.BN else currentLanguage),
                                            color = Color(0xFFFFB300),
                                            fontSize = if (isFatherMode) 18.sp else 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isFatherMode || currentLanguage == AppLanguage.BN) "বাকি" else "Due",
                                            color = Color(0xFFFFB300),
                                            fontSize = if (isFatherMode) 14.sp else 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
