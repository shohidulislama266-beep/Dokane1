package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.PlainEmptyState
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.QuantityProgressRing
import com.example.ui.components.VividButton
import com.example.ui.components.edgeToEdgeDialogProperties
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

private val AlertAmber = Color(0xFFFFB300)

@Composable
fun StockScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val products by storeViewModel.products.collectAsState()
    val productBin by storeViewModel.productBin.collectAsState()
    val stockSummary by storeViewModel.stockSummary.collectAsState()
    val availableCategories by storeViewModel.availableProductCategories.collectAsState()
    val unitPresets by storeViewModel.unitPresets.collectAsState()
    val shopType by storeViewModel.shopType.collectAsState()
    val photoStorageMode by storeViewModel.photoStorageMode.collectAsState()
    val canAddEdit by storeViewModel.canAddEditProductsFlow.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val isSimpleMode by storeViewModel.isSimpleMode.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val showCostAndProfit = canSeeProfit && (!isSimpleMode || isFatherMode)
    val hasOwnerPin by storeViewModel.hasOwnerPin.collectAsState()
    val isOwner by storeViewModel.isOwner.collectAsState()
    val isBusy by storeViewModel.isBusy.collectAsState()
    val errorMessage by storeViewModel.errorMessage.collectAsState()
    val isPrivacyHidden by storeViewModel.isPrivacyHidden.collectAsState()
    val lowOnlyTrigger by storeViewModel.stockFilterLowOnlyTrigger.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var showLowOnly by rememberSaveable { mutableStateOf(false) }
    var showBinView by rememberSaveable { mutableStateOf(false) }
    var isGridView by rememberSaveable { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var restockTarget by remember { mutableStateOf<Product?>(null) }
    var confirmEmptyBin by remember { mutableStateOf(false) }

    LaunchedEffect(lowOnlyTrigger) {
        if (lowOnlyTrigger) {
            showLowOnly = true
            storeViewModel.setStockFilterLowOnlyTrigger(false)
        }
    }

    LaunchedEffect(isSimpleMode) {
        if (isSimpleMode) {
            showBinView = false
            selectedCategory = "ALL"
        }
    }

    val totalUnitsCount = remember(products) {
        products.sumOf { it.stock.coerceAtLeast(0.0) }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory, showLowOnly) {
        val q = searchQuery.trim()
        products.filter { prod ->
            val matchCat = selectedCategory == "ALL" || prod.category.equals(selectedCategory, ignoreCase = true)
            val matchLow = !showLowOnly || prod.stock <= prod.lowStockLimit
            val matchQuery = q.isBlank() ||
                prod.name.contains(q, ignoreCase = true) ||
                prod.nameBn.contains(q, ignoreCase = true) ||
                prod.barcode.contains(q, ignoreCase = true) ||
                prod.itemLocation.contains(q, ignoreCase = true) ||
                prod.category.contains(q, ignoreCase = true)
            matchCat && matchLow && matchQuery
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        val warnCount = stockSummary.lowStockCount + stockSummary.outOfStockCount
        val lowCount = products.count { it.stock <= it.lowStockLimit }

        @Composable
        fun HeaderSection() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showBinView) {
                        if (currentLanguage == AppLanguage.BN) {
                            "রিসাইকেল বিন (${ShopLocale.digits(productBin.size.toString(), currentLanguage)})"
                        } else {
                            "Recycle Bin (${productBin.size})"
                        }
                    } else {
                        ShopLocale.get("nav_stock", currentLanguage)
                    },
                    color = TextPrimary,
                    fontSize = if (isFatherMode) 24.sp else 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )

                if ((isOwner || canAddEdit) && !isFatherMode && !isSimpleMode) {
                    Surface(
                        onClick = { showBinView = !showBinView },
                        shape = RoundedCornerShape(50),
                        color = if (showBinView) VividGreen.copy(alpha = 0.2f) else Color(0x1EFFFFFF),
                        border = BorderStroke(1.dp, if (showBinView) VividGreen else GlassBorder),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .testTag("stock_bin_toggle_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = if (showBinView) VividGreen else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (showBinView) {
                                    if (currentLanguage == AppLanguage.BN) "স্টক" else "All Stock"
                                } else {
                                    if (currentLanguage == AppLanguage.BN) {
                                        "বিন (${ShopLocale.digits(productBin.size.toString(), currentLanguage)})"
                                    } else {
                                        "Bin (${productBin.size})"
                                    }
                                },
                                color = if (showBinView) VividGreen else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        @Composable
        fun KpiSection() {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StockKpiGlassCard(
                    title = if (currentLanguage == AppLanguage.BN) "পণ্য" else "ITEMS",
                    value = ShopLocale.digits(stockSummary.totalItems.toString(), currentLanguage),
                    subtitle = if (currentLanguage == AppLanguage.BN) {
                        "${ShopLocale.formatNumber(totalUnitsCount, currentLanguage)} ইউনিট"
                    } else {
                        "${ShopLocale.formatNumber(totalUnitsCount, currentLanguage)} units"
                    },
                    accentColor = TextPrimary,
                    glowColor = null,
                    onClick = {
                        selectedCategory = "ALL"
                        showLowOnly = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                StockKpiGlassCard(
                    title = if (currentLanguage == AppLanguage.BN) "কম স্টক" else "LOW STOCK",
                    value = ShopLocale.digits(warnCount.toString(), currentLanguage),
                    subtitle = if (currentLanguage == AppLanguage.BN) "সতর্কতা" else "alert",
                    accentColor = if (warnCount > 0L) AlertAmber else VividGreen,
                    glowColor = if (warnCount > 0L) AlertAmber else null,
                    onClick = { showLowOnly = !showLowOnly },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                if (showCostAndProfit) {
                    StockKpiGlassCard(
                        title = if (currentLanguage == AppLanguage.BN) "স্টক মূল্য" else "STOCK VALUE",
                        value = if (isPrivacyHidden) "৳••••"
                        else ShopLocale.formatMoney(stockSummary.stockValueAtCost, currentLanguage),
                        subtitle = if (currentLanguage == AppLanguage.BN) "ক্রয় মূল্যে" else "at cost",
                        accentColor = VividGreen,
                        glowColor = VividGreen,
                        onClick = null,
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        @Composable
        fun SearchSection() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = if (currentLanguage == AppLanguage.BN) {
                        if (isSimpleMode) "পণ্য খুঁজুন..." else "নাম, বারকোড, তাক খুঁজুন..."
                    } else {
                        if (isSimpleMode) "Search product..." else "Search name, barcode, shelf..."
                    },
                    leadingIcon = Icons.Default.Search,
                    modifier = Modifier.weight(1f),
                    testTag = "stock_search_input"
                )
                if (!isSimpleMode) {
                    Surface(
                        onClick = { showBarcodeScanner = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xEC1B201B),
                        border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("stock_barcode_scan_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = if (currentLanguage == AppLanguage.BN) "বারকোড স্ক্যান" else "Scan Barcode",
                                tint = VividGreen
                            )
                        }
                    }
                }
                Surface(
                    onClick = { isGridView = !isGridView },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xEC1B201B),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("stock_view_mode_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (currentLanguage == AppLanguage.BN) "ভিউ পরিবর্তন" else "Toggle grid view",
                            tint = TextPrimary
                        )
                    }
                }
            }
        }

        @Composable
        fun FilterSection() {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StockFilterChip(
                    label = if (currentLanguage == AppLanguage.BN) {
                        "সব (${ShopLocale.digits(products.size.toString(), currentLanguage)})"
                    } else {
                        "All (${products.size})"
                    },
                    selected = selectedCategory == "ALL" && !showLowOnly,
                    onClick = {
                        selectedCategory = "ALL"
                        showLowOnly = false
                    },
                    testTag = "stock_filter_all"
                )
                StockFilterChip(
                    label = if (currentLanguage == AppLanguage.BN) {
                        "কম স্টক ${ShopLocale.digits(lowCount.toString(), currentLanguage)}"
                    } else {
                        "Low Stock $lowCount"
                    },
                    selected = showLowOnly,
                    onClick = { showLowOnly = !showLowOnly },
                    accentColor = AlertAmber,
                    testTag = "stock_filter_low"
                )
                if (!isSimpleMode) {
                    availableCategories.forEach { cat ->
                        StockFilterChip(
                            label = ShopLocale.displayCategory(cat, currentLanguage),
                            selected = selectedCategory.equals(cat, ignoreCase = true),
                            onClick = {
                                selectedCategory = if (selectedCategory.equals(cat, ignoreCase = true)) "ALL" else cat
                            },
                            testTag = "stock_filter_cat_$cat"
                        )
                    }
                }
            }
        }

        @Composable
        fun EmptyStateBlock() {
            PlainEmptyState(
                message = if (products.isEmpty()) {
                    if (currentLanguage == AppLanguage.BN) "কোনো পণ্য যোগ করা হয়নি। নিচের + বোতামে ট্যাপ করুন।"
                    else "No products yet. Tap + Add Product to get started."
                } else {
                    if (currentLanguage == AppLanguage.BN) "এই ফিল্টারে কোনো পণ্য পাওয়া যায়নি"
                    else "No products match your filter"
                }
            )
        }

        if (!showBinView) {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = if (canAddEdit) 100.dp else 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "stock_header", span = { GridItemSpan(maxLineSpan) }) { HeaderSection() }
                    item(key = "stock_kpi", span = { GridItemSpan(maxLineSpan) }) { KpiSection() }
                    item(key = "stock_search", span = { GridItemSpan(maxLineSpan) }) { SearchSection() }
                    item(key = "stock_filters", span = { GridItemSpan(maxLineSpan) }) { FilterSection() }

                    if (filteredProducts.isEmpty()) {
                        item(key = "stock_empty", span = { GridItemSpan(maxLineSpan) }) { EmptyStateBlock() }
                    } else {
                        items(filteredProducts, key = { it.id }) { product ->
                            StockGridProductCard(
                                product = product,
                                currentLanguage = currentLanguage,
                                canSeeProfit = showCostAndProfit,
                                isPrivacyHidden = isPrivacyHidden,
                                canAddEdit = canAddEdit,
                                onEditClick = {
                                    if (canAddEdit) editingProduct = product else restockTarget = product
                                },
                                onQuickRestock = { restockTarget = product }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = if (canAddEdit) 100.dp else 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "stock_header") { HeaderSection() }
                    item(key = "stock_kpi") { KpiSection() }
                    item(key = "stock_search") { SearchSection() }
                    item(key = "stock_filters") { FilterSection() }

                    if (filteredProducts.isEmpty()) {
                        item(key = "stock_empty") { EmptyStateBlock() }
                    } else {
                        items(filteredProducts, key = { it.id }) { product ->
                            StockProductCard(
                                product = product,
                                currentLanguage = currentLanguage,
                                canSeeProfit = showCostAndProfit,
                                isPrivacyHidden = isPrivacyHidden,
                                canAddEdit = canAddEdit,
                                isFatherMode = isFatherMode,
                                onEditClick = {
                                    if (canAddEdit) editingProduct = product else restockTarget = product
                                },
                                onQuickRestock = { restockTarget = product }
                            )
                        }
                    }
                }
            }
        } else {
            // Recycle Bin View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "bin_header") { HeaderSection() }
                if (productBin.isNotEmpty() && isOwner) {
                    item(key = "bin_empty_btn") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            GlassOutlinedButton(
                                text = if (currentLanguage == AppLanguage.BN) "বিন খালি করুন" else "Empty Bin",
                                icon = Icons.Default.DeleteOutline,
                                onClick = { confirmEmptyBin = true },
                                testTag = "empty_product_bin_btn"
                            )
                        }
                    }
                }
                if (productBin.isEmpty()) {
                    item(key = "bin_empty_state") {
                        PlainEmptyState(
                            message = if (currentLanguage == AppLanguage.BN) "রিসাইকেল বিন খালি" else "Recycle Bin is empty"
                        )
                    }
                } else {
                    items(productBin, key = { it.id }) { deletedProd ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.BN && deletedProd.nameBn.isNotBlank()) deletedProd.nameBn else deletedProd.name,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${ShopLocale.formatNumber(deletedProd.stock, currentLanguage)} ${ShopLocale.displayUnit(deletedProd.unit, currentLanguage)}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                VividButton(
                                    text = if (currentLanguage == AppLanguage.BN) "ফিরিয়ে আনুন" else "Restore",
                                    icon = Icons.Default.RestoreFromTrash,
                                    onClick = { storeViewModel.restoreProductFromBin(deletedProd.id) },
                                    testTag = "restore_bin_${deletedProd.id}"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Glowing Pill Floating + Add Product Button above bottom bar
        if (canAddEdit && !showBinView) {
            val shape = RoundedCornerShape(28.dp)
            Surface(
                onClick = { showAddDialog = true },
                shape = shape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color(0x66FFFFFF)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 14.dp)
                    .height(if (isFatherMode) 56.dp else 50.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = shape,
                        spotColor = VividGreen,
                        ambientColor = VividGreen
                    )
                    .testTag("add_product_fab")
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF83FA4E), VividGreen, Color(0xFF47AF20))
                            )
                        )
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF0A1406),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (currentLanguage == AppLanguage.BN) "পণ্য যোগ" else "Add Product",
                        color = Color(0xFF0A1406),
                        fontSize = if (isFatherMode) 16.sp else 14.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }

    if (showBarcodeScanner) {
        QrScannerDialog(
            currentLanguage = currentLanguage,
            onQrScanned = { code ->
                showBarcodeScanner = false
                searchQuery = code.trim()
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    if (restockTarget != null) {
        QuickRestockDialog(
            product = restockTarget!!,
            isFatherMode = isFatherMode,
            onDismiss = { restockTarget = null },
            onConfirm = { amt ->
                storeViewModel.addQuickStock(restockTarget!!.id, amt) {
                    restockTarget = null
                }
            }
        )
    }

    if (showAddDialog) {
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
                showAddDialog = false
            },
            onConfirm = { name, nameBn, cat, unit, buy, sell, stock, low, photo, barcode, wholesale, mrp, loc, partyRate, buyUnit, buyConv, sellUnits ->
                storeViewModel.addNewProductWizard(
                    name = name,
                    nameBn = nameBn,
                    category = cat,
                    unit = unit,
                    buyPrice = buy,
                    sellPrice = sell,
                    stock = stock,
                    lowStockLimit = low,
                    photoUri = photo,
                    barcode = barcode,
                    wholesalePrice = wholesale,
                    mrp = mrp,
                    itemLocation = loc,
                    partyRate = partyRate,
                    buyUnit = buyUnit,
                    buyConversion = buyConv,
                    sellUnits = sellUnits,
                    onSuccess = { showAddDialog = false }
                )
            }
        )
    }

    if (editingProduct != null) {
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
            onConfirm = { name, nameBn, cat, unit, buy, sell, stock, low, photo, barcode, wholesale, mrp, loc, partyRate, buyUnit, buyConv, sellUnits ->
                storeViewModel.updateProductWizard(
                    productId = editingProduct!!.id,
                    name = name,
                    nameBn = nameBn,
                    category = cat,
                    unit = unit,
                    buyPrice = buy,
                    sellPrice = sell,
                    stock = stock,
                    lowStockLimit = low,
                    photoUri = photo,
                    barcode = barcode,
                    wholesalePrice = wholesale,
                    mrp = mrp,
                    itemLocation = loc,
                    partyRate = partyRate,
                    buyUnit = buyUnit,
                    buyConversion = buyConv,
                    sellUnits = sellUnits,
                    onSuccess = { editingProduct = null }
                )
            },
            onDelete = if ((isOwner || isFatherMode) && !isSimpleMode) {
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

    if (confirmEmptyBin) {
        AlertDialog(
            onDismissRequest = { confirmEmptyBin = false },
            properties = edgeToEdgeDialogProperties(),
            modifier = Modifier.safeDialogWindowPadding(),
            containerColor = DarkBackground,
            title = {
                ConfigureEdgeToEdgeDialogWindow()
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "বিন সম্পূর্ণ খালি করবেন?" else "Empty Recycle Bin?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "বিনে থাকা সব পণ্য স্থায়ীভাবে মুছে যাবে।"
                    else "All items in Recycle Bin will be permanently deleted.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmEmptyBin = false
                        storeViewModel.emptyProductBin()
                    }
                ) {
                    Text(if (currentLanguage == AppLanguage.BN) "মুছে ফেলুন" else "Empty", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmptyBin = false }) {
                    Text(ShopLocale.get("cancel", currentLanguage), color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StockKpiGlassCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    glowColor: Color?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val bgColors = when (glowColor) {
        AlertAmber -> listOf(Color(0xF02B2212), Color(0xF418130B))
        VividGreen -> listOf(Color(0xF0182914), Color(0xF40E150C))
        else -> listOf(Color(0xF0232723), Color(0xF2151815))
    }
    val borderBrush = if (glowColor != null) {
        Brush.verticalGradient(listOf(glowColor.copy(alpha = 0.85f), glowColor.copy(alpha = 0.3f)))
    } else {
        Brush.verticalGradient(listOf(Color(0x3BFFFFFF), Color(0x10FFFFFF)))
    }

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 98.dp)
            .then(
                if (glowColor != null) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = shape,
                        spotColor = glowColor.copy(alpha = 0.35f),
                        ambientColor = glowColor.copy(alpha = 0.25f)
                    )
                } else Modifier
            )
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(bgColors))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true
            )
            Text(
                text = value,
                color = accentColor,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 12.5.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true
            )
        }
    }
}

@Composable
private fun StockFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = VividGreen,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) accentColor.copy(alpha = 0.22f) else Color(0x1CFFFFFF),
        border = BorderStroke(1.dp, if (selected) accentColor else GlassBorder),
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) accentColor else TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun StockProductCard(
    product: Product,
    currentLanguage: AppLanguage,
    canSeeProfit: Boolean,
    isPrivacyHidden: Boolean,
    canAddEdit: Boolean,
    isFatherMode: Boolean,
    onEditClick: () -> Unit,
    onQuickRestock: () -> Unit
) {
    val isOut = product.stock <= 0.0
    val isLow = !isOut && product.stock <= product.lowStockLimit
    val displayName = if (currentLanguage == AppLanguage.BN && product.nameBn.isNotBlank()) product.nameBn else product.name

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEditClick,
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        testTag = "stock_product_card_${product.id}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuantityProgressRing(
                quantity = product.stock,
                lowStockLimit = product.lowStockLimit,
                size = if (isFatherMode) 60.dp else 54.dp,
                unitLabel = product.unit
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayName,
                    color = TextPrimary,
                    fontSize = if (isFatherMode) 17.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isOut) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AlertRed.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BN) "শেষ" else "Out",
                                color = AlertRed,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isLow) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AlertAmber.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, AlertAmber.copy(alpha = 0.55f))
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
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0x1EFFFFFF),
                            border = BorderStroke(1.dp, Color(0x28FFFFFF))
                        ) {
                            Text(
                                text = ShopLocale.displayCategory(product.category, currentLanguage),
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                val sellLabel = if (currentLanguage == AppLanguage.BN) "বিক্রয়" else "Sell"
                val buyLabel = if (currentLanguage == AppLanguage.BN) "ক্রয়" else "Buy"
                val priceLine = if (canSeeProfit && product.buyPrice > 0.0 && !isPrivacyHidden) {
                    "$sellLabel ${ShopLocale.formatMoney(product.sellPrice, currentLanguage)} • $buyLabel ${ShopLocale.formatMoney(product.buyPrice, currentLanguage)}"
                } else {
                    "$sellLabel ${ShopLocale.formatMoney(product.sellPrice, currentLanguage)}"
                }
                Text(
                    text = priceLine,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (canAddEdit) {
                Surface(
                    onClick = onQuickRestock,
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF1A2A15),
                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("quick_restock_btn_${product.id}")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "+ স্টক" else "+ Stock",
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

@Composable
private fun StockGridProductCard(
    product: Product,
    currentLanguage: AppLanguage,
    canSeeProfit: Boolean,
    isPrivacyHidden: Boolean,
    canAddEdit: Boolean,
    onEditClick: () -> Unit,
    onQuickRestock: () -> Unit
) {
    val displayName = if (currentLanguage == AppLanguage.BN && product.nameBn.isNotBlank()) product.nameBn else product.name
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEditClick,
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(12.dp),
        testTag = "stock_product_card_${product.id}"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuantityProgressRing(
                quantity = product.stock,
                lowStockLimit = product.lowStockLimit,
                size = 52.dp,
                unitLabel = product.unit
            )
            Text(
                text = displayName,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = ShopLocale.formatMoney(product.sellPrice, currentLanguage),
                color = VividGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            if (canSeeProfit && product.buyPrice > 0.0 && !isPrivacyHidden) {
                Text(
                    text = "${if (currentLanguage == AppLanguage.BN) "ক্রয়" else "Buy"} ${ShopLocale.formatMoney(product.buyPrice, currentLanguage)}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
            if (canAddEdit) {
                Surface(
                    onClick = onQuickRestock,
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF1A2A15),
                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_restock_btn_${product.id}")
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.BN) "+ স্টক" else "+ Stock",
                            color = VividGreen,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRestockDialog(
    product: Product,
    isFatherMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val currentLanguage = com.example.util.LocalAppLanguage.current
    var amountText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties()
    ) {
        ConfigureEdgeToEdgeDialogWindow()
        val shape = RoundedCornerShape(24.dp)
        Surface(
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .safeDialogWindowPadding()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xF71B201B), Color(0xFA111411))))
                .testTag("quick_restock_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (currentLanguage == AppLanguage.BN) "দ্রুত স্টক যোগ করুন" else "Quick Restock",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (currentLanguage == AppLanguage.BN && product.nameBn.isNotBlank()) product.nameBn else product.name,
                    color = VividGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (currentLanguage == AppLanguage.BN) {
                        "বর্তমান মজুদ: ${ShopLocale.formatNumber(product.stock, currentLanguage)} ${ShopLocale.displayUnit(product.unit, currentLanguage)}"
                    } else {
                        "Current stock: ${ShopLocale.formatNumber(product.stock, currentLanguage)} ${ShopLocale.displayUnit(product.unit, currentLanguage)}"
                    },
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )

                GlassTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMsg = null
                    },
                    label = if (currentLanguage == AppLanguage.BN) {
                        "যোগ করার পরিমাণ (${ShopLocale.displayUnit(product.unit, currentLanguage)})"
                    } else {
                        "Quantity to Add (${ShopLocale.displayUnit(product.unit, currentLanguage)})"
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMsg != null,
                    errorMessage = errorMsg,
                    testTag = "quick_restock_amount_input"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassOutlinedButton(
                        text = ShopLocale.get("cancel", currentLanguage),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_restock_cancel_btn"
                    )
                    VividButton(
                        text = ShopLocale.get("add_stock_btn", currentLanguage),
                        onClick = {
                            val amt = ShopLocale.toAsciiDigits(amountText).toDoubleOrNull() ?: 0.0
                            if (amt <= 0.0) {
                                errorMsg = ShopLocale.get("err_add_stock_positive", currentLanguage)
                            } else {
                                onConfirm(amt)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_restock_confirm_btn"
                    )
                }
            }
        }
    }
}
