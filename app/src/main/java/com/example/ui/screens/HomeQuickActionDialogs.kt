package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.data.model.PurchaseItem
import com.example.data.model.Supplier
import com.example.domain.BakiOverview
import com.example.domain.CalculationEngine
import com.example.ui.components.AccountPickerField
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.SearchGlassField
import com.example.ui.components.VividButton
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ShopLocale
import com.example.util.SyncStatus

@Composable
fun HomeNotificationsDialog(
    lowStockProducts: List<Product>,
    bakiOverview: BakiOverview,
    syncStatus: SyncStatus,
    currentLanguage: AppLanguage,
    canViewStock: Boolean,
    canAccessBaki: Boolean,
    onRestockProduct: (Product) -> Unit,
    onOpenLowStock: () -> Unit,
    onOpenBaki: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = com.example.ui.components.edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.example.ui.components.ConfigureEdgeToEdgeDialogWindow()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("home_notifications_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = DarkBackground,
            border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF192219), Color(0xFF101210))
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = VividGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "দোকানের সতর্কতা" else "Store Alerts",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real Cloud Sync status banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x16FFFFFF),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (syncIcon, syncColor, syncLabel) = when (syncStatus) {
                            SyncStatus.ONLINE -> Triple(
                                Icons.Default.CloudDone,
                                VividGreen,
                                if (currentLanguage == AppLanguage.BN) "ক্লাউড সিঙ্ক সক্রিয়" else "Cloud Sync Active"
                            )
                            SyncStatus.SYNCING -> Triple(
                                Icons.Default.Sync,
                                Color(0xFFFFB300),
                                if (currentLanguage == AppLanguage.BN) "সিঙ্ক হচ্ছে..." else "Syncing changes..."
                            )
                            SyncStatus.OFFLINE -> Triple(
                                Icons.Default.CloudOff,
                                AlertRed,
                                if (currentLanguage == AppLanguage.BN) "অফলাইন ক্যাশ সক্রিয়" else "Offline Cache Active"
                            )
                        }
                        Icon(syncIcon, contentDescription = null, tint = syncColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = syncLabel,
                            color = syncColor,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (canViewStock) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "স্বল্প মজুদ (${lowStockProducts.size})" else "Low Stock (${lowStockProducts.size})",
                                    color = Color(0xFFFFB300),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                if (lowStockProducts.isNotEmpty()) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) "সব দেখুন" else "View all",
                                        color = VividGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            onDismiss()
                                            onOpenLowStock()
                                        }
                                    )
                                }
                            }
                        }
                        if (lowStockProducts.isEmpty()) {
                            item {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "সব পণ্যের মজুদ পর্যাপ্ত আছে" else "All products are well-stocked.",
                                    color = TextMuted,
                                    fontSize = 12.5.sp
                                )
                            }
                        } else {
                            items(lowStockProducts.take(4), key = { "alert_low_${it.id}" }) { prod ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x14FFFFFF),
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            onRestockProduct(prod)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WarningAmber,
                                                contentDescription = null,
                                                tint = if (prod.stock <= 0.0) AlertRed else Color(0xFFFFB300),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = prod.name,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${ShopLocale.formatNumber(prod.stock, currentLanguage)} ${ShopLocale.displayUnit(prod.unit, currentLanguage)}",
                                            color = if (prod.stock <= 0.0) AlertRed else Color(0xFFFFB300),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (canAccessBaki) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "বকেয়া বাকি (${bakiOverview.debtorCount})" else "Baki Dues (${bakiOverview.debtorCount})",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                if (bakiOverview.debtors.isNotEmpty()) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN) "বাকি খাতা" else "Open Baki",
                                        color = VividGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            onDismiss()
                                            onOpenBaki()
                                        }
                                    )
                                }
                            }
                        }
                        if (bakiOverview.debtors.isEmpty()) {
                            item {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "কোনো বকেয়া নেই" else "No pending customer dues.",
                                    color = TextMuted,
                                    fontSize = 12.5.sp
                                )
                            }
                        } else {
                            items(bakiOverview.debtors.take(4), key = { "alert_baki_${it.customer.id}" }) { debtor ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x14FFFFFF),
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            onOpenBaki()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = debtor.customer.name,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = ShopLocale.formatMoney(debtor.currentBalance, currentLanguage),
                                            color = VividGreen,
                                            fontSize = 12.5.sp,
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
        }
    }
}

@Composable
fun HomePaymentInDialog(
    storeViewModel: StoreViewModel,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val bakiOverview by storeViewModel.bakiOverview.collectAsState()
    var search by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<Pair<Customer, Double>?>(null) }

    if (selectedCustomer != null) {
        val (cust, balance) = selectedCustomer!!
        ReceivePaymentDialog(
            customer = cust,
            currentBalance = balance,
            currentLanguage = currentLanguage,
            onDismiss = { selectedCustomer = null },
            onConfirm = { amount, note, date ->
                storeViewModel.recordCustomerPayment(
                    customer = cust,
                    amount = amount,
                    note = note,
                    paymentDate = date,
                    onSuccess = {
                        selectedCustomer = null
                        onDismiss()
                    }
                )
            }
        )
        return
    }

    val filtered = remember(bakiOverview.debtors, search) {
        val q = search.trim()
        if (q.isEmpty()) bakiOverview.debtors
        else bakiOverview.debtors.filter {
            it.customer.name.contains(q, ignoreCase = true) || it.customer.mobile.contains(q, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = com.example.ui.components.edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.example.ui.components.ConfigureEdgeToEdgeDialogWindow()
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "বাকি আদায় (Payment In)" else "Payment In",
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
                SearchGlassField(
                    query = search,
                    onQueryChange = { search = it },
                    label = if (currentLanguage == AppLanguage.BN) "ক্রেতা খুঁজুন" else "Search due customer",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "কোনো বকেয়া ক্রেতা পাওয়া যায়নি" else "No customers with pending Baki balance.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filtered.forEach { summary ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x18FFFFFF),
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCustomer = summary.customer to summary.currentBalance }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = summary.customer.name,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (summary.customer.mobile.isNotBlank()) {
                                            Text(
                                                text = summary.customer.mobile,
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Text(
                                        text = ShopLocale.formatMoney(summary.currentBalance, currentLanguage),
                                        color = VividGreen,
                                        fontSize = 14.sp,
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePaymentOutDialog(
    storeViewModel: StoreViewModel,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val suppliers by storeViewModel.suppliers.collectAsState()
    val accounts by storeViewModel.accounts.collectAsState()
    val lastUsedAccountId by storeViewModel.lastUsedAccountId.collectAsState()
    val canCashBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()

    var selectedSupplier by remember(suppliers) { mutableStateOf(suppliers.firstOrNull { it.balance > 0.0 } ?: suppliers.firstOrNull()) }
    var supplierDropdown by remember { mutableStateOf(false) }
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
        properties = com.example.ui.components.edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.example.ui.components.ConfigureEdgeToEdgeDialogWindow()
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        text = if (currentLanguage == AppLanguage.BN) "মহাজন পেমেন্ট (Payment Out)" else "Payment Out",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (suppliers.isEmpty()) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "আগে একজন মহাজন যোগ করুন (Add Party)" else "No suppliers yet. Use Add Party to create a supplier first.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassOutlinedButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                } else {
                    ExposedDropdownMenuBox(
                        expanded = supplierDropdown,
                        onExpandedChange = { supplierDropdown = !supplierDropdown }
                    ) {
                        GlassTextField(
                            value = selectedSupplier?.let { "${it.name} (${ShopLocale.formatMoney(it.balance, currentLanguage)})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = if (currentLanguage == AppLanguage.BN) "মহাজন নির্বাচন করুন" else "Supplier",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = supplierDropdown,
                            onDismissRequest = { supplierDropdown = false },
                            modifier = Modifier.background(DarkBackground)
                        ) {
                            suppliers.forEach { sup ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${sup.name} • Due: ${ShopLocale.formatMoney(sup.balance, currentLanguage)}",
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        selectedSupplier = sup
                                        supplierDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GlassTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            errorMsg = null
                        },
                        label = if (currentLanguage == AppLanguage.BN) "পরিশোধের পরিমাণ (৳)" else "Payment Amount (৳)",
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if ((selectedSupplier?.balance ?: 0.0) > 0.0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val bal = selectedSupplier!!.balance
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x205DD62C),
                                border = BorderStroke(1.dp, VividGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        amountText = bal.toInt().toString()
                                        errorMsg = null
                                    }
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "সম্পূর্ণ (${ShopLocale.formatMoney(bal, currentLanguage)})"
                                    else "Full (${ShopLocale.formatMoney(bal, currentLanguage)})",
                                    color = VividGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
                                    maxLines = 1
                                )
                            }
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
                        placeholder = "e.g. Partial settlement",
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
                            val sup = selectedSupplier
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (sup == null) {
                                errorMsg = "Select a supplier"
                                return@VividButton
                            }
                            if (amt <= 0.0) {
                                errorMsg = "Enter a valid amount > 0"
                                return@VividButton
                            }
                            storeViewModel.recordSupplierPayment(
                                supplier = sup,
                                amount = amt,
                                accountId = selectedAccId,
                                accountName = selectedAccName,
                                note = noteText,
                                date = System.currentTimeMillis(),
                                onSuccess = onDismiss
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRecordPurchaseDialog(
    storeViewModel: StoreViewModel,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val suppliers by storeViewModel.suppliers.collectAsState()
    val products by storeViewModel.products.collectAsState()
    val accounts by storeViewModel.accounts.collectAsState()
    val lastUsedAccountId by storeViewModel.lastUsedAccountId.collectAsState()
    val canCashBank by storeViewModel.canAccessCashAndBankFlow.collectAsState()

    var selectedSupplier by remember(suppliers) { mutableStateOf(suppliers.firstOrNull()) }
    var supplierDropdown by remember { mutableStateOf(false) }

    var selectedProduct by remember(products) { mutableStateOf(products.firstOrNull()) }
    var productDropdown by remember { mutableStateOf(false) }
    var selectedBuyUnit by remember(selectedProduct) {
        mutableStateOf(selectedProduct?.let { CalculationEngine.effectiveBuyUnit(it) } ?: "pcs")
    }
    var conversionText by remember(selectedProduct) {
        mutableStateOf(
            selectedProduct?.let {
                ShopLocale.formatNumber(CalculationEngine.effectiveBuyConversion(it), AppLanguage.EN)
            } ?: "1"
        )
    }
    var qtyText by remember { mutableStateOf("1") }
    var buyPriceText by remember(selectedProduct) {
        mutableStateOf(
            selectedProduct?.let { prod ->
                val conv = CalculationEngine.effectiveBuyConversion(prod)
                val suggested = CalculationEngine.buyPriceInBuyUnit(prod.buyPrice, conv)
                if (suggested > 0.0) ShopLocale.formatNumber(suggested, AppLanguage.EN) else ""
            } ?: ""
        )
    }

    val cartItems = remember { mutableStateListOf<PurchaseItem>() }
    val totalAmount = cartItems.sumOf { it.lineTotal }

    var paidAmountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val defaultAcc = accounts.find { it.id == lastUsedAccountId }
        ?: accounts.find { it.isDefault || it.id == "cash_in_hand" }
        ?: accounts.firstOrNull()
    var selectedAccId by remember { mutableStateOf(defaultAcc?.id ?: "cash_in_hand") }
    var selectedAccName by remember { mutableStateOf(defaultAcc?.name ?: "Cash in Hand") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = com.example.ui.components.edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.example.ui.components.ConfigureEdgeToEdgeDialogWindow()
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = VividGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "নতুন ক্রয় (Purchase)" else "Record Purchase",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (suppliers.isEmpty() || products.isEmpty()) {
                    Text(
                        text = if (suppliers.isEmpty()) "Add a supplier first using Add Party." else "Add a product first in Stock.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassOutlinedButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                } else {
                    ExposedDropdownMenuBox(
                        expanded = supplierDropdown,
                        onExpandedChange = { supplierDropdown = !supplierDropdown }
                    ) {
                        GlassTextField(
                            value = selectedSupplier?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = if (currentLanguage == AppLanguage.BN) "মহাজন" else "Supplier",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = supplierDropdown,
                            onDismissRequest = { supplierDropdown = false },
                            modifier = Modifier.background(DarkBackground)
                        ) {
                            suppliers.forEach { sup ->
                                DropdownMenuItem(
                                    text = { Text(sup.name, color = TextPrimary, maxLines = 1, softWrap = false) },
                                    onClick = {
                                        selectedSupplier = sup
                                        supplierDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ExposedDropdownMenuBox(
                        expanded = productDropdown,
                        onExpandedChange = { productDropdown = !productDropdown }
                    ) {
                        GlassTextField(
                            value = selectedProduct?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = if (currentLanguage == AppLanguage.BN) "পণ্য" else "Product",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = productDropdown,
                            onDismissRequest = { productDropdown = false },
                            modifier = Modifier.background(DarkBackground)
                        ) {
                            products.forEach { prod ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${prod.name} (${ShopLocale.formatNumber(prod.stock, currentLanguage)} ${ShopLocale.displayUnit(prod.unit, currentLanguage)})",
                                            color = TextPrimary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        selectedProduct = prod
                                        val effBuyUnit = CalculationEngine.effectiveBuyUnit(prod)
                                        val effConv = CalculationEngine.effectiveBuyConversion(prod)
                                        selectedBuyUnit = effBuyUnit
                                        conversionText = ShopLocale.formatNumber(effConv, AppLanguage.EN)
                                        val suggestedPrice = CalculationEngine.buyPriceInBuyUnit(prod.buyPrice, effConv)
                                        buyPriceText = if (suggestedPrice > 0.0) ShopLocale.formatNumber(suggestedPrice, AppLanguage.EN) else ""
                                        productDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    val activeProduct = selectedProduct
                    if (activeProduct != null) {
                        val stockUnit = CalculationEngine.effectiveStockUnit(activeProduct)
                        val isDifferentBuyUnit = !selectedBuyUnit.equals(stockUnit, ignoreCase = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ShopLocale.get("buy_unit_label", currentLanguage),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalculationEngine.SUPPORTED_UNITS.forEach { u ->
                                val isSelected = selectedBuyUnit.equals(u, ignoreCase = true)
                                Surface(
                                    onClick = {
                                        selectedBuyUnit = u
                                        if (u.equals(stockUnit, ignoreCase = true)) {
                                            conversionText = "1"
                                            buyPriceText = if (activeProduct.buyPrice > 0.0) {
                                                ShopLocale.formatNumber(activeProduct.buyPrice, AppLanguage.EN)
                                            } else ""
                                        } else if (u.equals(CalculationEngine.effectiveBuyUnit(activeProduct), ignoreCase = true)) {
                                            val effConv = CalculationEngine.effectiveBuyConversion(activeProduct)
                                            conversionText = ShopLocale.formatNumber(effConv, AppLanguage.EN)
                                            val suggested = CalculationEngine.buyPriceInBuyUnit(activeProduct.buyPrice, effConv)
                                            if (suggested > 0.0) {
                                                buyPriceText = ShopLocale.formatNumber(suggested, AppLanguage.EN)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) VividGreen.copy(alpha = 0.22f) else Color(0x16FFFFFF),
                                    border = BorderStroke(1.dp, if (isSelected) VividGreen else GlassBorder)
                                ) {
                                    Text(
                                        text = ShopLocale.displayUnit(u, currentLanguage),
                                        color = if (isSelected) VividGreen else TextSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        if (isDifferentBuyUnit) {
                            Spacer(modifier = Modifier.height(8.dp))
                            GlassTextField(
                                value = conversionText,
                                onValueChange = {
                                    conversionText = it
                                    val newConv = it.trim().toDoubleOrNull()
                                    if (newConv != null && newConv > 0.0 && activeProduct.buyPrice > 0.0) {
                                        val suggested = CalculationEngine.buyPriceInBuyUnit(activeProduct.buyPrice, newConv)
                                        buyPriceText = ShopLocale.formatNumber(suggested, AppLanguage.EN)
                                    }
                                },
                                label = "1 ${ShopLocale.displayUnit(selectedBuyUnit, currentLanguage)} = ? ${ShopLocale.displayUnit(stockUnit, currentLanguage)}",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val buyUnitLabel = ShopLocale.displayUnit(selectedBuyUnit, currentLanguage)
                        GlassTextField(
                            value = qtyText,
                            onValueChange = { qtyText = it },
                            label = "Qty ($buyUnitLabel)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.9f)
                        )
                        GlassTextField(
                            value = buyPriceText,
                            onValueChange = { buyPriceText = it },
                            label = "Price/$buyUnitLabel",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = VividGreen,
                            modifier = Modifier
                                .height(52.dp)
                                .clickable {
                                    val prod = selectedProduct ?: return@clickable
                                    val q = qtyText.trim().toDoubleOrNull() ?: 0.0
                                    val bp = buyPriceText.trim().toDoubleOrNull() ?: 0.0
                                    val stockUnit = CalculationEngine.effectiveStockUnit(prod)
                                    val conv = if (selectedBuyUnit.equals(stockUnit, ignoreCase = true)) {
                                        1.0
                                    } else {
                                        conversionText.trim().toDoubleOrNull() ?: 0.0
                                    }
                                    if (q <= 0.0 || bp < 0.0) {
                                        errorMsg = "Enter valid qty > 0"
                                        return@clickable
                                    }
                                    if (conv <= 0.0) {
                                        errorMsg = "Enter valid conversion > 0"
                                        return@clickable
                                    }
                                    errorMsg = null
                                    cartItems.add(
                                        CalculationEngine.buildPurchaseItem(
                                            product = prod,
                                            buyQty = q,
                                            buyUnitPrice = bp,
                                            buyUnit = selectedBuyUnit,
                                            conversion = conv
                                        )
                                    )
                                    val newTotal = CalculationEngine.roundMoney(cartItems.sumOf { it.lineTotal })
                                    paidAmountText = ShopLocale.formatNumber(newTotal, AppLanguage.EN)
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Item", tint = DarkBackground)
                            }
                        }
                    }

                    if (activeProduct != null) {
                        val previewQ = qtyText.trim().toDoubleOrNull() ?: 0.0
                        val previewBp = buyPriceText.trim().toDoubleOrNull() ?: 0.0
                        val stockUnit = CalculationEngine.effectiveStockUnit(activeProduct)
                        val previewConv = if (selectedBuyUnit.equals(stockUnit, ignoreCase = true)) {
                            1.0
                        } else {
                            conversionText.trim().toDoubleOrNull() ?: CalculationEngine.effectiveBuyConversion(activeProduct)
                        }
                        if (previewQ > 0.0 && previewConv > 0.0) {
                            val stockAdded = CalculationEngine.buyQtyToStockQty(previewQ, previewConv)
                            val unitCost = CalculationEngine.costPerStockUnit(previewBp, previewConv)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "+${ShopLocale.formatNumber(stockAdded, currentLanguage)} ${ShopLocale.displayUnit(stockUnit, currentLanguage)} • ${ShopLocale.formatMoney(unitCost, currentLanguage)}/${ShopLocale.displayUnit(stockUnit, currentLanguage)}",
                                color = VividGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (cartItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x16FFFFFF),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                cartItems.forEachIndexed { idx, item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.name} × ${ShopLocale.formatNumber(item.qty, currentLanguage)} ${ShopLocale.displayUnit(item.unit, currentLanguage)}",
                                            color = TextPrimary,
                                            fontSize = 12.5.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = ShopLocale.formatMoney(item.lineTotal, currentLanguage),
                                            color = VividGreen,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        IconButton(
                                            onClick = {
                                                cartItems.removeAt(idx)
                                                val updatedTotal = CalculationEngine.roundMoney(cartItems.sumOf { it.lineTotal })
                                                paidAmountText = if (updatedTotal > 0.0) ShopLocale.formatNumber(updatedTotal, AppLanguage.EN) else ""
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = AlertRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Purchase", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                    Text(ShopLocale.formatMoney(totalAmount, currentLanguage), color = VividGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    GlassTextField(
                        value = paidAmountText,
                        onValueChange = { paidAmountText = it },
                        label = if (currentLanguage == AppLanguage.BN) "নগদ পরিশোধ (৳)" else "Paid Now (৳) — rest goes to Credit",
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (canCashBank) {
                        Spacer(modifier = Modifier.height(10.dp))
                        AccountPickerField(
                            selectedAccountId = selectedAccId,
                            selectedAccountName = selectedAccName,
                            onAccountSelected = { acc ->
                                selectedAccId = acc.id
                                selectedAccName = acc.name
                            },
                            storeViewModel = storeViewModel,
                            label = "Paid From Account"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    GlassTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = "Note (Optional)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(errorMsg!!, color = AlertRed, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    VividButton(
                        text = if (currentLanguage == AppLanguage.BN) "ক্রয় সংরক্ষণ করুন" else "Save Purchase",
                        onClick = {
                            val sup = selectedSupplier
                            if (sup == null) {
                                errorMsg = "Select a supplier"
                                return@VividButton
                            }
                            if (cartItems.isEmpty()) {
                                errorMsg = "Tap + to add at least one item"
                                return@VividButton
                            }
                            val paid = (paidAmountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, totalAmount)
                            storeViewModel.recordPurchase(
                                supplier = sup,
                                items = cartItems.toList(),
                                paidAmount = paid,
                                note = noteText,
                                accountId = selectedAccId,
                                accountName = selectedAccName,
                                onSuccess = { onDismiss() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun HomeAddPartyDialog(
    storeViewModel: StoreViewModel,
    currentLanguage: AppLanguage,
    canAddCustomer: Boolean,
    canAddSupplier: Boolean,
    initialIsSupplier: Boolean? = null,
    onDismiss: () -> Unit
) {
    var isSupplier by remember(initialIsSupplier, canAddCustomer, canAddSupplier) {
        mutableStateOf(
            when {
                initialIsSupplier == true && canAddSupplier -> true
                initialIsSupplier == false && canAddCustomer -> false
                else -> !canAddCustomer && canAddSupplier
            }
        )
    }
    var nameInput by remember { mutableStateOf("") }
    var mobileInput by remember { mutableStateOf("") }
    var companyOrAddressInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = com.example.ui.components.edgeToEdgeDialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.example.ui.components.ConfigureEdgeToEdgeDialogWindow()
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = VividGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "নতুন পার্টি যোগ" else "Add Party",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                if (canAddCustomer && canAddSupplier) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isSupplier) Color(0x335DD62C) else Color(0x18FFFFFF),
                            border = BorderStroke(1.dp, if (!isSupplier) VividGreen else GlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isSupplier = false }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "ক্রেতা (Customer)" else "Customer",
                                    color = if (!isSupplier) VividGreen else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSupplier) Color(0x335DD62C) else Color(0x18FFFFFF),
                            border = BorderStroke(1.dp, if (isSupplier) VividGreen else GlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isSupplier = true }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.BN) "মহাজন (Supplier)" else "Supplier",
                                    color = if (isSupplier) VividGreen else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                GlassTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        errorMsg = null
                    },
                    label = if (isSupplier) "Supplier Name *" else "Customer Name *",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassTextField(
                    value = mobileInput,
                    onValueChange = { mobileInput = it },
                    label = "Mobile Number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassTextField(
                    value = companyOrAddressInput,
                    onValueChange = { companyOrAddressInput = it },
                    label = if (isSupplier) "Company Name (Optional)" else "Address (Optional)",
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMsg!!, color = AlertRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                VividButton(
                    text = if (isSupplier) "Save Supplier" else "Save Customer",
                    onClick = {
                        val trimmed = nameInput.trim()
                        if (trimmed.isBlank()) {
                            errorMsg = "Name is required"
                            return@VividButton
                        }
                        if (isSupplier) {
                            storeViewModel.addSupplier(
                                name = trimmed,
                                mobile = mobileInput,
                                company = companyOrAddressInput,
                                address = "",
                                onSuccess = { onDismiss() }
                            )
                        } else {
                            storeViewModel.addCustomer(
                                name = trimmed,
                                mobile = mobileInput,
                                address = companyOrAddressInput,
                                notes = "",
                                onSuccess = { onDismiss() }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
