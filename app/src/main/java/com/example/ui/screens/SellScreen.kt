package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompletedSaleReceipt
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.domain.CalculationEngine
import com.example.domain.CustomerBalanceSummary
import com.example.ui.components.AccountPickerField
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.DigitalReceiptBottomSheet
import com.example.ui.components.edgeToEdgeDialogProperties
import com.example.ui.components.safeDialogWindowPadding
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.components.HeroGlowGlassCard
import com.example.ui.components.PlainEmptyState
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.SlideToConfirmSale
import com.example.ui.components.VividButton
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ProductImageUtil
import com.example.util.ShopLocale
import coil.compose.AsyncImage
import coil.request.ImageRequest

private val AlertAmber = Color(0xFFFFB300)

private data class CartLineEntry(
    val product: Product,
    val qty: Double,
    val selectedUnit: String,
    val discount: Double = 0.0
)

@Composable
fun SellScreen(
    storeViewModel: StoreViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val products by storeViewModel.products.collectAsState()
    val sales by storeViewModel.sales.collectAsState()
    val availableCategories by storeViewModel.availableProductCategories.collectAsState()
    val customerSummaries by storeViewModel.allCustomerSummaries.collectAsState()
    val accounts by storeViewModel.accounts.collectAsState()
    val lastUsedAccountId by storeViewModel.lastUsedAccountId.collectAsState()
    val receiptImageStyle by storeViewModel.receiptImageStyle.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val isBusy by storeViewModel.isBusy.collectAsState()
    val errorMessage by storeViewModel.errorMessage.collectAsState()

    var isCheckoutOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        if (isCheckoutOpen) {
            isCheckoutOpen = false
        } else {
            onBack()
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    val cartLines = remember { mutableStateListOf<CartLineEntry>() }

    var paymentType by rememberSaveable { mutableStateOf("cash") } // "cash" or "baki"
    var attachedCustomer by remember { mutableStateOf<Customer?>(null) }
    var invoiceDiscountText by rememberSaveable { mutableStateOf("") }
    var saleNoteText by rememberSaveable { mutableStateOf("") }

    val activeAccounts = remember(accounts) { accounts.filter { !it.isArchived } }
    var selectedAccId by remember { mutableStateOf(lastUsedAccountId) }
    var selectedAccName by remember { mutableStateOf("Cash in Hand") }

    LaunchedEffect(activeAccounts, lastUsedAccountId) {
        val acc = activeAccounts.find { it.id == selectedAccId }
            ?: activeAccounts.find { it.id == lastUsedAccountId }
            ?: activeAccounts.find { it.isDefault || it.id == "cash_in_hand" }
            ?: activeAccounts.firstOrNull()
        if (acc != null) {
            selectedAccId = acc.id
            selectedAccName = acc.name
        }
    }

    var showScanner by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var customerPickerRequireBaki by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var editingQtyLineIndex by remember { mutableStateOf<Int?>(null) }
    var completedReceipt by remember { mutableStateOf<CompletedSaleReceipt?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var sliderResetTick by remember { mutableStateOf(0) }
    var fatherPartialPaidAmount by remember { mutableStateOf<Double?>(null) }
    var fatherClampNoticeTick by remember { mutableStateOf(0) }
    var saleAttemptId by rememberSaveable { mutableStateOf(java.util.UUID.randomUUID().toString()) }

    // Generate a fresh idempotency ID whenever the cart contents change (retries on same cart reuse the same ID)
    LaunchedEffect(cartLines.toList()) {
        saleAttemptId = java.util.UUID.randomUUID().toString()
        if (cartLines.isEmpty() && isCheckoutOpen) {
            isCheckoutOpen = false
        }
    }

    // Build SaleItems via CalculationEngine so preview & checkout stay 100% consistent
    val builtSaleItems = remember(cartLines.toList()) {
        cartLines.map { line ->
            CalculationEngine.buildSaleItem(
                product = line.product,
                qty = line.qty,
                unit = line.selectedUnit,
                requestedDiscount = line.discount
            )
        }
    }
    val subtotal = remember(builtSaleItems) {
        CalculationEngine.roundMoney(
            builtSaleItems.sumOf { CalculationEngine.calculateLineGrossTotal(it.sellPrice, it.qty) }
        )
    }
    val itemDiscountSum = remember(builtSaleItems) {
        CalculationEngine.roundMoney(builtSaleItems.sumOf { it.discount })
    }
    val maxInvoiceDiscount = remember(builtSaleItems) {
        CalculationEngine.calculateMaxInvoiceDiscount(builtSaleItems)
    }
    val rawParsedDiscount = remember(invoiceDiscountText) {
        ShopLocale.toAsciiDigits(invoiceDiscountText).toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }
    val isDiscountOverProfit = remember(builtSaleItems, rawParsedDiscount) {
        CalculationEngine.isSaleDiscountOverProfit(builtSaleItems, rawParsedDiscount)
    }
    val discountLimitMessage = remember(isFatherMode, currentLanguage, canSeeProfit, maxInvoiceDiscount) {
        val lang = if (isFatherMode) AppLanguage.BN else currentLanguage
        if (canSeeProfit) {
            if (lang == AppLanguage.BN) {
                "লাভের (${ShopLocale.formatMoney(maxInvoiceDiscount, AppLanguage.BN)}) চেয়ে বেশি ছাড় দেওয়া যাবে না"
            } else {
                "Cannot give more discount than profit (${ShopLocale.formatMoney(maxInvoiceDiscount, AppLanguage.EN)})"
            }
        } else {
            if (lang == AppLanguage.BN) {
                "অনুমোদিত সীমার চেয়ে বেশি ছাড় দেওয়া যাবে না"
            } else {
                "Cannot give more discount than allowed limit"
            }
        }
    }
    val invoiceDiscount = remember(rawParsedDiscount, maxInvoiceDiscount) {
        CalculationEngine.roundMoney(rawParsedDiscount.coerceAtMost(maxInvoiceDiscount))
    }
    val totalDiscount = CalculationEngine.roundMoney((itemDiscountSum + invoiceDiscount).coerceIn(0.0, subtotal))
    val grandTotal = CalculationEngine.roundMoney((subtotal - totalDiscount).coerceAtLeast(0.0))

    val filteredCatalog = remember(products, searchQuery, selectedCategory) {
        val q = searchQuery.trim()
        products
            .filter { prod ->
                (selectedCategory == "ALL" || prod.category.equals(selectedCategory, ignoreCase = true)) &&
                    (q.isBlank() || prod.name.contains(q, ignoreCase = true) || prod.nameBn.contains(q, ignoreCase = true) || prod.barcode.contains(q, ignoreCase = true))
            }
            .sortedByDescending { it.stock > 0.0 }
    }

    val cartQtyByProduct = remember(cartLines.toList()) {
        val map = mutableMapOf<String, Double>()
        for (line in cartLines) {
            val factor = CalculationEngine.sellFactorForUnit(line.product, line.selectedUnit)
            val stockEq = CalculationEngine.sellQtyToStockQty(line.qty, factor)
            map[line.product.id] = (map[line.product.id] ?: 0.0) + stockEq
        }
        map
    }

    fun totalStockInCartForProduct(productId: String, excludingIndex: Int = -1): Double {
        var total = 0.0
        cartLines.forEachIndexed { idx, entry ->
            if (idx != excludingIndex && entry.product.id == productId) {
                val factor = CalculationEngine.sellFactorForUnit(entry.product, entry.selectedUnit)
                total += CalculationEngine.sellQtyToStockQty(entry.qty, factor)
            }
        }
        return total
    }

    fun addProductToCart(product: Product) {
        if (product.stock <= 0.0) return
        localError = null
        val defaultUnit = CalculationEngine.effectiveStockUnit(product)
        val idx = cartLines.indexOfFirst { it.product.id == product.id && it.selectedUnit == defaultUnit }
        val otherUsedStock = totalStockInCartForProduct(product.id, excludingIndex = idx)
        if (idx >= 0) {
            val current = cartLines[idx]
            val factor = CalculationEngine.sellFactorForUnit(product, current.selectedUnit)
            val nextQty = CalculationEngine.roundStock(current.qty + 1.0)
            val neededStock = otherUsedStock + CalculationEngine.sellQtyToStockQty(nextQty, factor)
            if (neededStock <= product.stock + 0.0001) {
                cartLines[idx] = current.copy(qty = nextQty)
            } else {
                localError = if (currentLanguage == AppLanguage.BN) "পর্যাপ্ত স্টক নেই" else "Not enough stock for ${product.name}"
            }
        } else {
            val remaining = (product.stock - otherUsedStock).coerceAtLeast(0.0)
            val initialQty = if (remaining >= 1.0) 1.0 else CalculationEngine.roundStock(remaining)
            if (initialQty > 0.0) {
                cartLines.add(CartLineEntry(product = product, qty = initialQty, selectedUnit = defaultUnit))
            } else {
                localError = if (currentLanguage == AppLanguage.BN) "পর্যাপ্ত স্টক নেই" else "Not enough stock for ${product.name}"
            }
        }
    }

    fun submitSale(paidNowOverride: Double? = null) {
        if (isBusy) return
        if (isDiscountOverProfit) {
            localError = discountLimitMessage
            sliderResetTick++
            return
        }
        val hasRemainingBaki = if (paidNowOverride != null) {
            CalculationEngine.roundMoney((grandTotal - paidNowOverride.coerceIn(0.0, grandTotal)).coerceAtLeast(0.0)) > 0.001
        } else {
            paymentType == "baki"
        }
        if (hasRemainingBaki && attachedCustomer == null) {
            if (isFatherMode) {
                customerPickerRequireBaki = true
                showCustomerPicker = true
                return
            }
            localError = if (currentLanguage == AppLanguage.BN) {
                "বাকি বিক্রির জন্য ক্রেতা নির্বাচন করুন"
            } else {
                "Please select a customer for Baki sale"
            }
            sliderResetTick++
            return
        }
        localError = null
        val cust = attachedCustomer
        storeViewModel.completeSale(
            items = builtSaleItems,
            invoiceDiscount = invoiceDiscount,
            paymentType = paymentType,
            customerId = cust?.id.orEmpty(),
            customerName = cust?.name?.ifBlank { "Cash Sale" } ?: "Cash Sale",
            customerPhone = cust?.mobile.orEmpty(),
            notes = saleNoteText,
            accountId = selectedAccId,
            accountName = selectedAccName,
            paidNowAmount = paidNowOverride,
            clientSaleId = saleAttemptId
        ) { receipt ->
            cartLines.clear()
            invoiceDiscountText = ""
            saleNoteText = ""
            attachedCustomer = null
            paymentType = "cash"
            fatherPartialPaidAmount = null
            saleAttemptId = java.util.UUID.randomUUID().toString()
            isCheckoutOpen = false
            sliderResetTick++
            completedReceipt = receipt
        }
    }

    if (isCheckoutOpen && cartLines.isNotEmpty()) {
        if (isFatherMode) {
            FatherSellStep3Content(
                builtSaleItems = builtSaleItems,
                subtotal = subtotal,
                totalDiscount = totalDiscount,
                grandTotal = grandTotal,
                paymentType = paymentType,
                attachedCustomer = attachedCustomer,
                partialPaidAmount = fatherPartialPaidAmount,
                clampNoticeTick = fatherClampNoticeTick,
                displayError = localError ?: errorMessage,
                isBusy = isBusy,
                onApplyDiscount = { disc ->
                    invoiceDiscountText = if (disc % 1.0 == 0.0) disc.toLong().toString() else disc.toString()
                    val newGrandTotal = CalculationEngine.roundMoney((subtotal - (itemDiscountSum + disc).coerceIn(0.0, subtotal)).coerceAtLeast(0.0))
                    val existingPaid = fatherPartialPaidAmount
                    if (existingPaid != null && existingPaid > newGrandTotal + 0.001) {
                        fatherPartialPaidAmount = newGrandTotal
                        fatherClampNoticeTick++
                    }
                    localError = null
                },
                onClearDiscount = {
                    invoiceDiscountText = ""
                    localError = null
                },
                onApplyPartialPaid = { paid ->
                    fatherPartialPaidAmount = CalculationEngine.roundMoney(paid.coerceIn(0.0, grandTotal))
                    localError = null
                },
                onClearPartialPaid = {
                    fatherPartialPaidAmount = null
                    localError = null
                },
                onPaymentTypeChange = { nextType ->
                    paymentType = nextType
                    localError = null
                },
                onOpenCustomerPicker = { requireForBaki ->
                    customerPickerRequireBaki = requireForBaki
                    showCustomerPicker = true
                },
                onConfirmSale = { paidNow ->
                    submitSale(paidNowOverride = paidNow)
                },
                onClose = { isCheckoutOpen = false },
                canSeeProfit = canSeeProfit,
                modifier = modifier
            )
        } else {
            val nextInvoicePreview = remember(sales.size) {
                CalculationEngine.formatInvoiceNumber((sales.size + 1).toLong())
            }
            CheckoutContent(
            currentLanguage = currentLanguage,
            nextInvoiceNo = nextInvoicePreview,
            cartLines = cartLines,
            builtSaleItems = builtSaleItems,
            subtotal = subtotal,
            totalDiscount = totalDiscount,
            grandTotal = grandTotal,
            invoiceDiscountText = invoiceDiscountText,
            onInvoiceDiscountChange = {
                invoiceDiscountText = it
                localError = null
                storeViewModel.clearErrorMessage()
            },
            isDiscountOverProfit = isDiscountOverProfit,
            discountLimitMessage = discountLimitMessage,
            paymentType = paymentType,
            onPaymentTypeChange = {
                paymentType = it
                localError = null
            },
            attachedCustomer = attachedCustomer,
            customerSummaries = customerSummaries,
            onSelectCustomer = { cust ->
                attachedCustomer = cust
                localError = null
            },
            onOpenCustomerPicker = { showCustomerPicker = true },
            selectedAccId = selectedAccId,
            selectedAccName = selectedAccName,
            onAccountSelected = { acc ->
                selectedAccId = acc.id
                selectedAccName = acc.name
            },
            storeViewModel = storeViewModel,
            saleNoteText = saleNoteText,
            onSaleNoteChange = { saleNoteText = it },
            onDecreaseQty = { idx ->
                val line = cartLines[idx]
                val next = CalculationEngine.roundStock(line.qty - 1.0)
                if (next > 0.0001) {
                    cartLines[idx] = line.copy(qty = next)
                } else {
                    cartLines.removeAt(idx)
                }
                localError = null
            },
            onIncreaseQty = { idx ->
                val line = cartLines[idx]
                val factor = CalculationEngine.sellFactorForUnit(line.product, line.selectedUnit)
                val otherUsed = totalStockInCartForProduct(line.product.id, excludingIndex = idx)
                val next = CalculationEngine.roundStock(line.qty + 1.0)
                val neededStock = otherUsed + CalculationEngine.sellQtyToStockQty(next, factor)
                if (neededStock <= line.product.stock + 0.0001) {
                    cartLines[idx] = line.copy(qty = next)
                    localError = null
                } else {
                    localError = if (currentLanguage == AppLanguage.BN) "পর্যাপ্ত স্টক নেই" else "Max available stock reached"
                }
            },
            onEditQtyClick = { idx -> editingQtyLineIndex = idx },
            onRemoveLine = { idx ->
                cartLines.removeAt(idx)
                localError = null
            },
            onChangeUnit = { idx, newUnit ->
                val line = cartLines[idx]
                val oldFactor = CalculationEngine.sellFactorForUnit(line.product, line.selectedUnit)
                val newFactor = CalculationEngine.sellFactorForUnit(line.product, newUnit)
                val stockQty = CalculationEngine.sellQtyToStockQty(line.qty, oldFactor)
                val convertedQty = CalculationEngine.roundStock(stockQty * newFactor).coerceAtLeast(0.001)
                val otherUsed = totalStockInCartForProduct(line.product.id, excludingIndex = idx)
                val maxInNewUnit = CalculationEngine.maxSellableQtyInUnit(
                    (line.product.stock - otherUsed).coerceAtLeast(0.0),
                    newFactor
                )
                val safeQty = convertedQty.coerceAtMost(maxInNewUnit).coerceAtLeast(0.001)
                cartLines[idx] = line.copy(selectedUnit = newUnit, qty = safeQty)
                localError = null
            },
            displayError = localError ?: errorMessage,
            isBusy = isBusy,
            isFatherMode = isFatherMode,
            sliderResetKey = sliderResetTick to errorMessage,
            onConfirmSale = { submitSale() },
            onClose = { isCheckoutOpen = false },
            modifier = modifier
        )
        }
    } else {
        // Screen 3: Sell Product Grid
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = if (cartLines.isNotEmpty()) 112.dp else 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                item(key = "sell_header", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isFatherMode) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("sell_back_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = ShopLocale.get("back", currentLanguage),
                                        tint = TextPrimary
                                    )
                                }
                            }
                            Text(
                                text = if (isFatherMode) "বিক্রি করুন" else ShopLocale.get("sell_title", currentLanguage),
                                color = TextPrimary,
                                fontSize = if (isFatherMode) 24.sp else 21.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        if (cartLines.isNotEmpty()) {
                            Surface(
                                onClick = { isCheckoutOpen = true },
                                shape = RoundedCornerShape(50),
                                color = VividGreen.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.55f)),
                                modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = VividGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = ShopLocale.digits(cartLines.size.toString(), currentLanguage),
                                        color = VividGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Search + Conditional Barcode Scan (Hidden in Simple Mode; no dead mic button)
                item(key = "sell_search", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = if (currentLanguage == AppLanguage.BN) {
                                if (isSimpleMode) "পণ্য খুঁজুন..." else "পণ্য খুঁজুন বা বারকোড..."
                            } else {
                                if (isSimpleMode) "Search product..." else "Search or scan..."
                            },
                            leadingIcon = Icons.Default.Search,
                            modifier = Modifier.weight(1f),
                            testTag = "sell_search_input"
                        )
                        if (!isSimpleMode) {
                            Surface(
                                onClick = { showScanner = true },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xEC1B201B),
                                border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .size(54.dp)
                                    .testTag("sell_barcode_scan_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = if (currentLanguage == AppLanguage.BN) "বারকোড স্ক্যান" else "Scan barcode",
                                        tint = VividGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // Category Icon Tiles
                item(key = "sell_categories", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SellCategoryTile(
                            label = if (currentLanguage == AppLanguage.BN) "সব" else "All",
                            icon = Icons.Default.GridView,
                            selected = selectedCategory == "ALL",
                            onClick = { selectedCategory = "ALL" }
                        )
                        availableCategories.forEach { cat ->
                            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                            SellCategoryTile(
                                label = ShopLocale.displayCategory(cat, currentLanguage),
                                icon = getCategoryIcon(cat),
                                selected = isSelected,
                                onClick = { selectedCategory = if (isSelected) "ALL" else cat }
                            )
                        }
                    }
                }

                if (!localError.isNullOrBlank()) {
                    item(key = "sell_error", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = localError!!,
                            color = AlertRed,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // 2-Column Product Grid
                if (filteredCatalog.isEmpty()) {
                    item(key = "sell_empty", span = { GridItemSpan(maxLineSpan) }) {
                        PlainEmptyState(
                            message = if (currentLanguage == AppLanguage.BN) "কোনো পণ্য পাওয়া যায়নি" else "No products match your search"
                        )
                    }
                } else {
                    items(filteredCatalog, key = { it.id }) { prod ->
                        val inCartStock = cartQtyByProduct[prod.id] ?: 0.0
                        val cartCountLines = cartLines.filter { it.product.id == prod.id }.sumOf { it.qty }
                        SellProductGridCard(
                            product = prod,
                            inCartQty = cartCountLines,
                            isHighlighted = inCartStock > 0.0,
                            currentLanguage = currentLanguage,
                            isFatherMode = isFatherMode,
                            onAdd = { addProductToCart(prod) }
                        )
                    }
                }
            }

            // Floating Green Cart Pill above Bottom Nav Bar
            if (cartLines.isNotEmpty()) {
                val itemCountText = if (currentLanguage == AppLanguage.BN) {
                    "${ShopLocale.digits(cartLines.size.toString(), currentLanguage)}টি আইটেম"
                } else {
                    if (cartLines.size == 1) "1 item" else "${cartLines.size} items"
                }
                val checkoutLabel = if (currentLanguage == AppLanguage.BN) "চেকআউট" else "Checkout"

                Surface(
                    onClick = { isCheckoutOpen = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(29.dp),
                            spotColor = VividGreen,
                            ambientColor = VividGreen
                        )
                        .testTag("sell_floating_checkout_bar"),
                    shape = RoundedCornerShape(29.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0x66FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF84FA4E), VividGreen, Color(0xFF47AF20))
                                )
                            )
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color(0xFF0A1406),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = itemCountText,
                                color = Color(0xFF0A1406),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = ShopLocale.formatMoney(grandTotal, currentLanguage),
                            color = Color(0xFF0A1406),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = checkoutLabel,
                                color = Color(0xFF0A1406),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = checkoutLabel,
                                tint = Color(0xFF0A1406),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Decimal Qty Editor Dialog (supports fractional kg / litre / gram input)
    val editIdx = editingQtyLineIndex
    if (editIdx != null && editIdx in cartLines.indices) {
        val targetLine = cartLines[editIdx]
        val factor = CalculationEngine.sellFactorForUnit(targetLine.product, targetLine.selectedUnit)
        val otherUsed = totalStockInCartForProduct(targetLine.product.id, excludingIndex = editIdx)
        val maxQtyInUnit = CalculationEngine.maxSellableQtyInUnit(
            (targetLine.product.stock - otherUsed).coerceAtLeast(0.0),
            factor
        )
        DecimalQtyDialog(
            productName = if (currentLanguage == AppLanguage.BN && targetLine.product.nameBn.isNotBlank()) targetLine.product.nameBn else targetLine.product.name,
            unitLabel = ShopLocale.displayUnit(targetLine.selectedUnit, currentLanguage),
            rawUnit = targetLine.selectedUnit,
            currentQty = targetLine.qty,
            maxQty = maxQtyInUnit,
            currentLanguage = currentLanguage,
            onDismiss = { editingQtyLineIndex = null },
            onConfirm = { newQty ->
                cartLines[editIdx] = targetLine.copy(qty = CalculationEngine.roundStock(newQty))
                editingQtyLineIndex = null
                localError = null
            }
        )
    }

    if (showScanner) {
        QrScannerDialog(
            currentLanguage = currentLanguage,
            onQrScanned = { scanned ->
                showScanner = false
                val match = products.firstOrNull {
                    it.barcode.equals(scanned.trim(), ignoreCase = true) ||
                        it.id == scanned.trim()
                }
                if (match != null && match.stock > 0.0) {
                    addProductToCart(match)
                } else {
                    searchQuery = scanned.trim()
                }
            },
            onDismiss = { showScanner = false }
        )
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customerSummaries = customerSummaries,
            currentLanguage = currentLanguage,
            canAddCustomer = true,
            titleOverride = if (isFatherMode && customerPickerRequireBaki) "বাকি রাখতে কার নাম?" else null,
            showCashSaleOption = !(isFatherMode && customerPickerRequireBaki),
            isFatherMode = isFatherMode,
            onDismiss = {
                showCustomerPicker = false
                customerPickerRequireBaki = false
            },
            onSelectCustomer = { cust ->
                attachedCustomer = cust
                showCustomerPicker = false
                customerPickerRequireBaki = false
                localError = null
            },
            onSelectCashSale = {
                attachedCustomer = null
                paymentType = "cash"
                showCustomerPicker = false
                customerPickerRequireBaki = false
            },
            onAddNewCustomer = {
                showCustomerPicker = false
                showAddCustomerDialog = true
            }
        )
    }

    if (showAddCustomerDialog) {
        AddEditCustomerDialog(
            customerToEdit = null,
            existingCustomers = customerSummaries.map { it.customer },
            currentLanguage = currentLanguage,
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, mobile, address, notes ->
                storeViewModel.addCustomer(name, mobile, address, notes) { createdId ->
                    attachedCustomer = Customer(id = createdId, name = name, mobile = mobile, address = address, notes = notes)
                    showAddCustomerDialog = false
                }
            }
        )
    }

    if (completedReceipt != null) {
        DigitalReceiptBottomSheet(
            receipt = completedReceipt!!,
            onDismiss = { completedReceipt = null },
            onStartNewSale = { completedReceipt = null },
            receiptImageStyle = receiptImageStyle
        )
    }
}

@Composable
private fun SellCategoryTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            width = if (selected) 1.25.dp else 1.dp,
            color = if (selected) VividGreen else Color(0x28FFFFFF)
        ),
        modifier = Modifier
            .defaultMinSize(minWidth = 76.dp)
            .height(64.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (selected) {
                            listOf(Color(0xE81C2E16), Color(0xEE10170E))
                        } else {
                            listOf(Color(0xE8222622), Color(0xEE141714))
                        }
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) VividGreen else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (selected) VividGreen else TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SellProductGridCard(
    product: Product,
    inCartQty: Double,
    isHighlighted: Boolean,
    currentLanguage: AppLanguage,
    isFatherMode: Boolean,
    onAdd: () -> Unit
) {
    val isOutOfStock = product.stock <= 0.0
    val isLowStock = !isOutOfStock && product.stock <= product.lowStockLimit
    val displayName = if (currentLanguage == AppLanguage.BN && product.nameBn.isNotBlank()) product.nameBn else product.name
    val shape = RoundedCornerShape(22.dp)
    val context = LocalContext.current
    val resolvedPhotoModel = remember(product.photoUri, product.storeId, product.id, context) {
        ProductImageUtil.toCoilImageModel(
            context = context,
            photoUri = product.photoUri,
            storeId = product.storeId,
            productId = product.id
        )
    }
    var photoLoadFailed by remember(product.photoUri, product.storeId, product.id, resolvedPhotoModel) {
        mutableStateOf(false)
    }
    val hasProductPhoto = resolvedPhotoModel != null && !photoLoadFailed

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHighlighted) {
                    Modifier.shadow(
                        elevation = 14.dp,
                        shape = shape,
                        spotColor = VividGreen.copy(alpha = 0.4f),
                        ambientColor = VividGreen.copy(alpha = 0.3f)
                    )
                } else Modifier
            )
            .clip(shape)
            .clipToBounds()
            .clickable(enabled = !isOutOfStock, onClick = onAdd)
            .alpha(if (isOutOfStock) 0.62f else 1f)
            .testTag("sell_catalog_item_${product.id}"),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            width = if (isHighlighted) 1.35.dp else 1.dp,
            brush = Brush.verticalGradient(
                colors = if (isHighlighted) {
                    listOf(VividGreen.copy(alpha = 0.9f), VividGreen.copy(alpha = 0.35f))
                } else {
                    listOf(Color(0x3BFFFFFF), Color(0x10FFFFFF))
                }
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isHighlighted) {
                            listOf(Color(0xF21B2916), Color(0xF511160F))
                        } else {
                            listOf(Color(0xF0232723), Color(0xF2151815))
                        }
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (hasProductPhoto) {
                    val imageModel: Any = remember(resolvedPhotoModel, context) {
                        if (resolvedPhotoModel is java.io.File) {
                            val cacheKey = "${resolvedPhotoModel.absolutePath}_${resolvedPhotoModel.lastModified()}"
                            ImageRequest.Builder(context)
                                .data(resolvedPhotoModel)
                                .memoryCacheKey(cacheKey)
                                .diskCacheKey(cacheKey)
                                .build()
                        } else {
                            resolvedPhotoModel!!
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Color(0xFF0E120E))
                    ) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = displayName,
                            contentScale = ContentScale.Crop,
                            onError = { photoLoadFailed = true },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("sell_product_full_photo_${product.id}")
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .align(Alignment.BottomCenter)
                                .background(Color(0x26FFFFFF))
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = if (hasProductPhoto) 10.dp else 12.dp,
                            bottom = 12.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!hasProductPhoto) {
                        // Centered glowing orb / category icon fallback
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(74.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x55122010))
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isOutOfStock) Color(0x33FFFFFF) else VividGreen.copy(alpha = 0.72f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                ProductThumbnail(
                                    photoUri = product.photoUri,
                                    category = product.category,
                                    contentDescription = displayName,
                                    iconSize = 30.dp,
                                    cornerRadius = 33.dp,
                                    productId = product.id,
                                    storeId = product.storeId,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                    }

                // Product Name
                Text(
                    text = displayName,
                    color = TextPrimary,
                    fontSize = if (isFatherMode) 15.5.sp else 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                    modifier = Modifier.height(34.dp)
                )

                // Sell Price
                Text(
                    text = ShopLocale.formatMoney(product.sellPrice, currentLanguage),
                    color = if (isOutOfStock) TextSecondary else VividGreen,
                    fontSize = if (isFatherMode) 18.sp else 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )

                // Stock dot + count + circular Add icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isOutOfStock -> AlertRed
                                        isLowStock -> AlertAmber
                                        else -> VividGreen
                                    }
                                )
                        )
                        Text(
                            text = "${ShopLocale.formatNumber(product.stock, currentLanguage)} ${ShopLocale.displayUnit(product.unit, currentLanguage)}",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!isOutOfStock) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x28FFFFFF),
                            border = BorderStroke(1.dp, Color(0x30FFFFFF)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = if (currentLanguage == AppLanguage.BN) "যোগ করুন" else "Add",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                }
            }

            // Top-right badge (xN in cart or Low stock pill)
            if (inCartQty > 0.0 && !isOutOfStock) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xE0141714),
                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "×${ShopLocale.formatNumber(inCartQty, currentLanguage)}",
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else if (isLowStock) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (hasProductPhoto) Color(0xDC1B1608) else Color(0x33FFB300),
                    border = BorderStroke(1.dp, AlertAmber.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "কম" else "Low",
                        color = AlertAmber,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Out of Stock diagonal banner
            if (isOutOfStock) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 28.dp, y = 16.dp)
                        .graphicsLayer { rotationZ = 42f }
                        .background(AlertRed.copy(alpha = 0.85f))
                        .padding(horizontal = 28.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "স্টক শেষ" else "OUT OF STOCK",
                        color = Color.White,
                        fontSize = 9.5.sp,
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
private fun CheckoutContent(
    currentLanguage: AppLanguage,
    nextInvoiceNo: String,
    cartLines: List<CartLineEntry>,
    builtSaleItems: List<com.example.data.model.SaleItem>,
    subtotal: Double,
    totalDiscount: Double,
    grandTotal: Double,
    invoiceDiscountText: String,
    onInvoiceDiscountChange: (String) -> Unit,
    isDiscountOverProfit: Boolean,
    discountLimitMessage: String,
    paymentType: String,
    onPaymentTypeChange: (String) -> Unit,
    attachedCustomer: Customer?,
    customerSummaries: List<CustomerBalanceSummary>,
    onSelectCustomer: (Customer?) -> Unit,
    onOpenCustomerPicker: () -> Unit,
    selectedAccId: String,
    selectedAccName: String,
    onAccountSelected: (com.example.data.model.Account) -> Unit,
    storeViewModel: StoreViewModel,
    saleNoteText: String,
    onSaleNoteChange: (String) -> Unit,
    onDecreaseQty: (Int) -> Unit,
    onIncreaseQty: (Int) -> Unit,
    onEditQtyClick: (Int) -> Unit,
    onRemoveLine: (Int) -> Unit,
    onChangeUnit: (Int, String) -> Unit,
    displayError: String?,
    isBusy: Boolean,
    isFatherMode: Boolean,
    sliderResetKey: Any?,
    onConfirmSale: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayLabel = remember(currentLanguage) {
        ShopLocale.formatDateOnly(System.currentTimeMillis(), currentLanguage)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: X Checkout + INV-xxxx • Date pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sell_checkout_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = ShopLocale.get("close", currentLanguage),
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "চেকআউট" else "Checkout",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xE81C201C),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Text(
                        text = "$nextInvoiceNo • $todayLabel",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Hero TOTAL Card
        item {
            HeroGlowGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "সর্বমোট" else "TOTAL",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = ShopLocale.formatMoney(grandTotal, currentLanguage),
                        color = VividGreen,
                        fontSize = if (isFatherMode) 40.sp else 36.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false
                    )
                    HorizontalDivider(color = Color(0x28FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                    val subLabel = ShopLocale.get("subtotal", currentLanguage)
                    val discLabel = ShopLocale.get("discount", currentLanguage)
                    Text(
                        text = if (totalDiscount > 0.0) {
                            "$subLabel ${ShopLocale.formatMoneyFixed2(subtotal, currentLanguage)} — $discLabel ${ShopLocale.formatMoneyFixed2(totalDiscount, currentLanguage)}"
                        } else {
                            "$subLabel ${ShopLocale.formatMoneyFixed2(subtotal, currentLanguage)}"
                        },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Cart Items Glass Card (with - qty + pill, tap for decimals, kg/gram unit toggle per line)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    cartLines.forEachIndexed { index, line ->
                        val builtItem = builtSaleItems.getOrNull(index)
                            ?: CalculationEngine.buildSaleItem(line.product, line.qty, line.selectedUnit, line.discount)
                        val units = remember(line.product) {
                            CalculationEngine.availableSellUnits(line.product).map { it.first }
                        }
                        val itemName = if (currentLanguage == AppLanguage.BN && line.product.nameBn.isNotBlank()) {
                            line.product.nameBn
                        } else {
                            line.product.name
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = itemName,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        softWrap = true
                                    )
                                    Text(
                                        text = "${ShopLocale.formatMoneyFixed2(builtItem.sellPrice, currentLanguage)} / ${ShopLocale.displayUnit(line.selectedUnit, currentLanguage)}",
                                        color = TextSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 2,
                                        softWrap = true
                                    )
                                }

                                // Compact - qty + pill with fixed minimum width (tap qty for decimal input)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF1F241F),
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier.defaultMinSize(minWidth = 88.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .clickable { onDecreaseQty(index) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (line.qty <= 1.0) Icons.Default.DeleteOutline else Icons.Default.Remove,
                                                contentDescription = "Decrease",
                                                tint = if (line.qty <= 1.0) AlertRed else TextPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = "×${ShopLocale.formatNumber(line.qty, currentLanguage)}",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onEditQtyClick(index) }
                                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .clickable { onIncreaseQty(index) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Increase",
                                                tint = VividGreen,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = ShopLocale.formatMoney(builtItem.lineTotal, currentLanguage),
                                    color = VividGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.defaultMinSize(minWidth = 60.dp)
                                )
                            }

                            // Per-line Unit Toggle Chips (e.g. kg / gram) + Decimal hint
                            if (units.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    units.forEach { u ->
                                        val active = u.equals(line.selectedUnit, ignoreCase = true)
                                        Surface(
                                            onClick = { onChangeUnit(index, u) },
                                            shape = RoundedCornerShape(50),
                                            color = if (active) VividGreen.copy(alpha = 0.22f) else Color(0x18FFFFFF),
                                            border = BorderStroke(1.dp, if (active) VividGreen else GlassBorder)
                                        ) {
                                            Text(
                                                text = ShopLocale.displayUnit(u, currentLanguage),
                                                color = if (active) VividGreen else TextSecondary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = { onEditQtyClick(index) },
                                        shape = RoundedCornerShape(50),
                                        color = Color(0x14FFFFFF),
                                        border = BorderStroke(1.dp, GlassBorder)
                                    ) {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) "দশমিক পরিমাণ" else "Edit decimal qty",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            } else if (line.selectedUnit.lowercase() in listOf("kg", "litre", "ltr", "meter")) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 48.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        onClick = { onEditQtyClick(index) },
                                        shape = RoundedCornerShape(50),
                                        color = Color(0x14FFFFFF),
                                        border = BorderStroke(1.dp, GlassBorder)
                                    ) {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.BN) "দশমিক পরিমাণ লিখুন" else "Tap qty for decimals",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (index < cartLines.lastIndex) {
                            HorizontalDivider(color = Color(0x1CFFFFFF))
                        }
                    }
                }
            }
        }

        // Discount Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassTextField(
                        value = invoiceDiscountText,
                        onValueChange = onInvoiceDiscountChange,
                        label = if (currentLanguage == AppLanguage.BN) "ছাড় / ডিসকাউন্ট (৳)" else "Discount (৳)",
                        isError = isDiscountOverProfit,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "sell_discount_input"
                    )
                    Text(
                        text = discountLimitMessage,
                        color = if (isDiscountOverProfit) AlertRed else TextMuted,
                        fontSize = 11.5.sp,
                        fontWeight = if (isDiscountOverProfit) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .then(if (isDiscountOverProfit) Modifier.testTag("sell_discount_error_text") else Modifier)
                    )
                }
            }
        }

        // Paid in Full | Baki (Credit) Pill Switcher
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xEB151815),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "cash" to (if (currentLanguage == AppLanguage.BN) "নগদ পরিশোধ" else "Paid in Full"),
                        "baki" to (if (currentLanguage == AppLanguage.BN) "বাকি (ক্রেডিট)" else "Baki (Credit)")
                    ).forEach { (type, label) ->
                        val active = paymentType == type
                        Surface(
                            onClick = { onPaymentTypeChange(type) },
                            shape = RoundedCornerShape(24.dp),
                            color = if (active) VividGreen else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 46.dp)
                                .testTag("sell_payment_$type")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Text(
                                    text = label,
                                    color = if (active) Color(0xFF0A1406) else TextPrimary,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    softWrap = true
                                )
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Customer Cards (Cash Sale + Real Customers + Contact Picker)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isCashSelected = attachedCustomer == null
                CustomerQuickSquareCard(
                    initials = "C",
                    label = if (currentLanguage == AppLanguage.BN) "নগদ বিক্রি" else "Cash Sale",
                    selected = isCashSelected,
                    highlightAmber = paymentType == "baki" && isCashSelected,
                    onClick = { onSelectCustomer(null) }
                )

                customerSummaries.take(8).forEach { summary ->
                    val cust = summary.customer
                    val selected = attachedCustomer?.id == cust.id
                    val initials = cust.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    CustomerQuickSquareCard(
                        initials = initials,
                        label = cust.name,
                        selected = selected,
                        highlightAmber = false,
                        onClick = { onSelectCustomer(cust) }
                    )
                }

                // + Contact / More Customers Card
                CustomerQuickSquareCard(
                    initials = "+",
                    label = if (currentLanguage == AppLanguage.BN) "+ ক্রেতা" else "+ Contact",
                    selected = false,
                    highlightAmber = false,
                    onClick = onOpenCustomerPicker,
                    testTag = "sell_attach_customer_btn"
                )
            }
        }

        // Cash Account Picker (for Paid in Full)
        if (paymentType == "cash") {
            item {
                AccountPickerField(
                    selectedAccountId = selectedAccId,
                    selectedAccountName = selectedAccName,
                    onAccountSelected = onAccountSelected,
                    storeViewModel = storeViewModel
                )
            }
        }

        // Notes Field
        item {
            GlassTextField(
                value = saleNoteText,
                onValueChange = onSaleNoteChange,
                label = if (currentLanguage == AppLanguage.BN) "নোট (ঐচ্ছিক)" else "Notes",
                testTag = "sell_note_input"
            )
        }

        // Error banner if any
        val effectiveDisplayError = displayError ?: if (isDiscountOverProfit) discountLimitMessage else null
        if (!effectiveDisplayError.isNullOrBlank()) {
            item {
                Text(
                    text = effectiveDisplayError,
                    color = AlertRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("sell_checkout_error_text")
                )
            }
        }

        // Slide to Confirm Sale (or big VividButton in Father Mode)
        item {
            if (isFatherMode) {
                VividButton(
                    text = if (currentLanguage == AppLanguage.BN) "বিক্রি সম্পন্ন করুন" else "Complete Sale",
                    enabled = !isBusy && !isDiscountOverProfit,
                    isLoading = isBusy,
                    onClick = onConfirmSale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    fontSize = 17.sp,
                    testTag = "complete_sale_button"
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SlideToConfirmSale(
                        onConfirmed = onConfirmSale,
                        enabled = !isBusy && !isDiscountOverProfit,
                        text = if (isBusy) {
                            if (currentLanguage == AppLanguage.BN) "সংরক্ষণ হচ্ছে..." else "Saving sale..."
                        } else {
                            if (currentLanguage == AppLanguage.BN) "বিক্রি নিশ্চিত করতে স্লাইড করুন" else "Slide to confirm sale"
                        },
                        resetKey = sliderResetKey
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerQuickSquareCard(
    initials: String,
    label: String,
    selected: Boolean,
    highlightAmber: Boolean,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val shape = RoundedCornerShape(18.dp)
    val borderColor = when {
        selected -> VividGreen
        highlightAmber -> AlertAmber
        else -> Color(0x28FFFFFF)
    }
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(if (selected) 1.35.dp else 1.dp, borderColor),
        modifier = Modifier
            .width(86.dp)
            .height(92.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (selected) {
                            listOf(Color(0xEE1B2D16), Color(0xF210180E))
                        } else {
                            listOf(Color(0xF0222622), Color(0xF2151815))
                        }
                    )
                )
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x28FFFFFF))
                    .border(1.dp, if (selected) VividGreen else Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = if (selected) VividGreen else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = label,
                color = if (selected) VividGreen else TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun DecimalQtyDialog(
    productName: String,
    unitLabel: String,
    rawUnit: String,
    currentQty: Double,
    maxQty: Double,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var qtyInput by remember {
        mutableStateOf(ShopLocale.formatNumber(currentQty, AppLanguage.EN))
    }
    var err by remember { mutableStateOf<String?>(null) }

    val quickPresets = remember(rawUnit) {
        when (rawUnit.lowercase()) {
            "kg", "litre", "ltr", "meter" -> listOf(0.25, 0.5, 0.75, 1.0, 1.5, 2.0)
            "gram", "gm", "ml" -> listOf(100.0, 250.0, 500.0, 750.0, 1000.0)
            else -> listOf(1.0, 2.0, 5.0, 10.0)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(),
        modifier = Modifier
            .safeDialogWindowPadding()
            .imePadding(),
        containerColor = Color(0xFF1A1E1A),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            ConfigureEdgeToEdgeDialogWindow()
            Text(
                text = "$productName ($unitLabel)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassTextField(
                    value = qtyInput,
                    onValueChange = {
                        qtyInput = it
                        err = null
                    },
                    label = if (currentLanguage == AppLanguage.BN) "পরিমাণ ($unitLabel)" else "Quantity ($unitLabel)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Text(
                    text = if (currentLanguage == AppLanguage.BN) {
                        "সর্বোচ্চ: ${ShopLocale.formatNumber(maxQty, currentLanguage)} $unitLabel"
                    } else {
                        "Available: ${ShopLocale.formatNumber(maxQty, currentLanguage)} $unitLabel"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.filter { it <= maxQty + 0.0001 }.forEach { preset ->
                        Surface(
                            onClick = {
                                qtyInput = ShopLocale.formatNumber(preset, AppLanguage.EN)
                                err = null
                            },
                            shape = RoundedCornerShape(50),
                            color = Color(0x20FFFFFF),
                            border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = ShopLocale.formatNumber(preset, currentLanguage),
                                color = VividGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                if (err != null) {
                    Text(err!!, color = AlertRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = ShopLocale.toAsciiDigits(qtyInput).toDoubleOrNull()
                    when {
                        parsed == null || parsed <= 0.0 -> {
                            err = if (currentLanguage == AppLanguage.BN) "সঠিক পরিমাণ লিখুন" else "Enter a valid quantity > 0"
                        }
                        parsed > maxQty + 0.0001 -> {
                            err = if (currentLanguage == AppLanguage.BN) "পর্যাপ্ত স্টক নেই" else "Exceeds available stock"
                        }
                        else -> onConfirm(parsed)
                    }
                }
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "নিশ্চিত" else "Apply",
                    color = VividGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(ShopLocale.get("cancel", currentLanguage), color = TextSecondary)
            }
        }
    )
}
